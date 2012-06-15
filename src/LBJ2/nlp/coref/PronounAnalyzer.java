package LBJ2.nlp.coref;

import java.util.*;
import LBJ2.classify.Classifier;
import LBJ2.util.TableFormat;
import LBJ2.util.ClassUtils;


/**
  * This program compiles some statistics about the pronouns in a dataset.
  *
  * <h4>Usage</h4>
  * <blockquote><pre>
  *   java LBJ2.nlp.coref.PronounAnalyzer &lt;classifier&gt; \
  *                                       &lt;test data&gt; \
  *                                       [&lt;verbosity=0&gt;]
  * </pre></blockquote>
  *
  * <h4>Input</h4>
  * <p> <code>&lt;classifier&gt;</code> is the fully qualified class name of
  * the classifier to test.  <code>&lt;test data&gt;</code> is the name of a
  * file containing test data.  The higher the integer supplied for
  * <code>&lt;verbosity&gt;</code>, the more information will be output to
  * <code>STDOUT</code>.
  *
  * <h4>Output</h4>
  * A table with various counts related to how pronouns are clustered to other
  * mentions.
 **/
public class PronounAnalyzer extends CoreferenceTester
{
  private static final String[] rowLabels =
    new String[]{ "pred", "bestMerge", "gold" };
  private static final String[] columnLabels =
    new String[]{ "Cluster", "Sing-good", "Sing-bad", "NAM", "NOM", "PRO",
                  "PRE", "Total" };
  private static final int[] sigDigits = new int[7];


  /**
    * Computes <i>F<sub>1</sub></i>.
    *
    * @param p  The precision.
    * @param r  The recall.
    * @return The <i>F<sub>1</sub></i>.
   **/
  private static double F1(double p, double r) { return 2 * p * r / (p + r); }


  public static void main(String[] args)
  {
    String classifierName = null;
    String testFile = null;
    double threshold = 0;
    int verbosity = 0;

    try
    {
      classifierName = args[0];
      testFile = args[1];
      if (args.length > 2) verbosity = Integer.parseInt(args[2]);
      if (args.length > 3) throw new Exception();
    }
    catch (Exception e)
    {
      System.err.println(
  "usage: java LBJ2.nlp.coref.PronounAnalyzer <classifier> <test data> \\\n"
+ "                                           [<verbosity=0>]");
      System.exit(1);
    }

    Classifier classifier = ClassUtils.getClassifier(classifierName);

    double[][] results =
      analyzePronouns(classifier, new ACE2004DocumentParser(testFile),
                      verbosity);
    results = TableFormat.transpose(results);
    double[][] table = new double[results.length + 1][];
    System.arraycopy(results, 0, table, 0, results.length);
    table[results.length] = new double[rowLabels.length];
    table = TableFormat.transpose(table);
    for (int i = 0; i < rowLabels.length; ++i)
      for (int j = 0; j < results.length; ++j)
        table[i][results.length] += table[i][j];

    String[] output =
      TableFormat.tableFormat(columnLabels, rowLabels, table, sigDigits,
                              new int[]{ 0 });
    for (int i = 0; i < output.length; ++i)
      System.out.println(output[i]);
  }


  /**
    * Tests the specified classifier on the specified testing data.
    *
    * @param classifier The specified classifier.
    * @param parser     A document parser.
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total pronoun counts over the data set, as described in
    *         {@link #analyzePronouns(Classifier,Document)}.
   **/
  public static double[][] analyzePronouns(Classifier classifier,
                                           ACE2004DocumentParser parser,
                                           int verbosity)
  {
    int index = 1;
    double[][] result = new double[rowLabels.length][columnLabels.length - 2];

    for (Document d = (Document) parser.next(); d != null;
         d = (Document) parser.next(), ++index)
    {
      if (verbosity > 0) System.out.print(index + ": ");
      double[][] results =
        new PronounAnalyzer(verbosity).analyzePronouns(classifier, d);
      for (int i = 0; i < result.length; ++i)
        for (int j = 0; j < result[0].length; ++j)
          result[i][j] += results[i][j];
    }

    return result;
  }


  /** Simply sets {@link #verbosity} to 0. */
  public PronounAnalyzer() { this(0); }

  /**
    * Initializing constructor.
    *
    * @param v  A value for {@link #verbosity}.
   **/
  public PronounAnalyzer(int v) { super(v); }


  /**
    * Computes statistics about pronoun usage both in the gold labels and in
    * the predictions of a classifier.  In particular, we look to see how
    * often pronouns occur as singletons and in clusters containing mentions
    * of other types.  The first dimension of the returned array selects
    * either the prediction stats (index 0) or the gold stats (index 1).  The
    * second dimension contains counts for the following scenarios starting
    * from index 0: singleton, cluster contains another mention of type
    * <code>NAM</code>, <code>NOM</code>, <code>PRO</code>, or
    * <code>PRE</code>.
    *
    * @param classifier The classifier whose predictions will be merged.
    * @param document   The document which the classifier is classifying.
   **/
  public double[][] analyzePronouns(Classifier classifier, Document document)
  {
    if (verbosity > 0)
      System.out.println(document.getName() + ": " + new Date());

    double[][] result = new double[rowLabels.length][columnLabels.length - 2];
    initializeClusterCache(document.totalMentions());
    Document[] documents = { new Document(document), document };
    documents[0].fillInPredictions(classifier, verbosity);

    for (int i = 0; i < documents[0].sentences(); ++i)
      for (int j = 0; j < documents[0].mentionsInSentence(i); ++j)
      {
        Document.Mention[] m =
          { documents[0].getMention(i, j), documents[1].getMention(i, j) };
        if (!m[0].getType().equals("PRO")) continue;
        if (verbosity > 1)
          System.out.print("mention " + m[0].getIndexInDocument() + ": ");

        for (int k = 0; k < 2; ++k)
        {
          HashSet<Document.Mention> cluster =
            getCluster(documents[k], k, m[k]);

          if (cluster.size() == 1)
          {
            if (k == 0)
            {
              cluster = getCluster(documents[1], 1, m[1]);

              if (cluster.size() == 1)
              {
                ++result[0][0];
                if (verbosity > 1) System.out.println("really a singleton");
              }
              else
              {
                ++result[0][1];
                if (verbosity > 1) System.out.println("not a singleton");
              }
            }
            else ++result[2][0];
          }
          else
          {
            boolean[] types = new boolean[6];

            for (Document.Mention n : cluster)
            {
              if (m[k] == n) continue;

              if (n.getType().equals("NAM")) types[2] = true;
              else if (n.getType().equals("NOM")) types[3] = true;
              else if (n.getType().equals("PRO")) types[4] = true;
              else if (n.getType().equals("PRE")) types[5] = true;
              else
              {
                System.err.println("ERROR: unrecognized mention type: '"
                                   + n.getType() + "'");
                System.exit(1);
              }
            }

            for (int t = 2; t < 6; ++t)
              if (types[t]) ++result[2 * k][t];

            if (verbosity > 1 && k == 0)
              System.out.println("not predicted a singleton");
          }
        }
      }

    int[] c = mergeSingletonPronouns(documents[0], documents[1], verbosity);
    result[1][0] = c[0];
    result[1][1] = c[1];

    for (int i = 0; i < documents[0].sentences(); ++i)
      for (int j = 0; j < documents[0].mentionsInSentence(i); ++j)
      {
        Document.Mention m = documents[0].getMention(i, j);
        if (!m.getType().equals("PRO")) continue;
        HashSet<Document.Mention> cluster = getCluster(documents[0], 0, m);

        if (cluster.size() > 1)
        {
          boolean[] types = new boolean[6];

          for (Document.Mention n : cluster)
          {
            if (m == n) continue;

            if (n.getType().equals("NAM")) types[2] = true;
            else if (n.getType().equals("NOM")) types[3] = true;
            else if (n.getType().equals("PRO")) types[4] = true;
            else if (n.getType().equals("PRE")) types[5] = true;
            else
            {
              System.err.println("ERROR: unrecognized mention type: '"
                                 + n.getType() + "'");
              System.exit(1);
            }
          }

          for (int t = 2; t < 6; ++t)
            if (types[t]) ++result[1][t];
        }
      }

    return result;
  }
}

