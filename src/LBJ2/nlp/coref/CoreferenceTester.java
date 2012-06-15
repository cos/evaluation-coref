package LBJ2.nlp.coref;

import java.util.*;
import LBJ2.classify.Classifier;
import LBJ2.util.TableFormat;
import LBJ2.util.ClassUtils;


/**
  * Use this program to test a coreference classifier with the
  * <i>B<sup>3</sup></i> evaluation metric.
  *
  * <h4>Usage</h4>
  * <blockquote><pre>
  *   java LBJ2.nlp.coref.CoreferenceTester &lt;classifier&gt; \
  *                                         &lt;test data&gt; \
  *                                         &lt;error analysis&gt; \
  *                                         &lt;merge singleton pronouns&gt; \
  *                                         [&lt;verbosity=0&gt;]
  * </pre></blockquote>
  * or
  * <blockquote><pre>
  *   java LBJ2.nlp.coref.CoreferenceTester &lt;coref classifier&gt; \
  *                                         &lt;coherence classifier&gt; \
  *                                         &lt;test data&gt; \
  *                                         [&lt;verbosity=0&gt;]
  * </pre></blockquote>
  *
  * <h4>Input</h4>
  * <p> <code>&lt;classifier&gt;</code> is the fully qualified class name of
  * the classifier to test.  <code>&lt;test data&gt;</code> is the name of a
  * file containing test data.  <code>&lt;error analysis&gt;</code> specifies
  * whether to perform error analysis, and it may be either be
  * <code>true</code> or <code>false</code>.  The higher the integer supplied
  * for <code>&lt;verbosity&gt;</code>, the more information will be output to
  * <code>STDOUT</code>.
  *
  * <h4>Output</h4>
  * <p> Precision, recall, and <i>F<sub>1</sub></i> are sent to
  * <code>STDOUT</code> as computed by <i>B<sup>3</sup></i>.
 **/
public class CoreferenceTester
{
  public static final String[] conditionedColumnLabels =
    { "Type", "Precision", "Recall", "F1", "Count" };
  public static final String[] conditionedRowLabels = new String[5];
  public static final String[] unconditionedRowLabels =
    { "Precision", "Recall", "F1" };
  public static final String[] coherenceRowLabels =
    { "Precision", "Recall", "F1", "Cohere Accuracy" };
  public static final int[] sigDigits = { 3, 3, 3, 0 };

  static
  {
    System.arraycopy(Document.mentionTypes, 0, conditionedRowLabels, 0, 4);
    conditionedRowLabels[4] = "Total";
  }


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
    String corefClassifierName = null;
    String coherenceClassifierName = null;
    String testFile = null;
    double threshold = 0;
    boolean errorAnalysis = false;
    boolean mergePronouns = false;
    int verbosity = 0;

    try
    {
      corefClassifierName = args[0];
      int i = 4;

      if (args.length == 3
          || args.length == 4
             && !(args[3].equals("true") || args[3].equals("false")))
      {
        coherenceClassifierName = args[1];
        testFile = args[2];
        i = 3;
      }
      else
      {
        testFile = args[1];
        errorAnalysis = args[2].equals("true");
        if (!errorAnalysis && !args[2].equals("false")) throw new Exception();
        mergePronouns = args[3].equals("true");
        if (!mergePronouns && !args[3].equals("false")) throw new Exception();
      }

      if (args.length > i) verbosity = Integer.parseInt(args[i]);
      if (args.length > 5) throw new Exception();
    }
    catch (Exception e)
    {
      System.err.println(
  "usage: java LBJ2.nlp.coref.CoreferenceTester <classifier> <test data> \\\n"
+ "                                             <error analysis> \\\n"
+ "                                             <merge singleton pronouns> \\\n"
+ "                                             [<verbosity=0>]\n"
+ "       or\n"
+ "       java LBJ2.nlp.coref.CoreferenceTester <coref classifier> \\\n"
+ "                                             <coherence classifier> \\\n"
+ "                                             <test data> \\\n"
+ "                                             [<verbosity=0>]");
      System.exit(1);
    }

    Classifier corefClassifier =
      ClassUtils.getClassifier(corefClassifierName);
    String[] output = null;

    if (coherenceClassifierName != null)
    {
      Classifier coherenceClassifier =
        ClassUtils.getClassifier(coherenceClassifierName);
      double[] results =
        test(corefClassifier, coherenceClassifier, testFile, verbosity);
      double[][] table = new double[][]{ (double[]) results.clone() };

      for (int j = 0; j < table[0].length; ++j)
        table[0][j] *= 100;
      output =
        TableFormat.tableFormat(
            null, coherenceRowLabels, TableFormat.transpose(table),
            sigDigits);
    }
    else if (errorAnalysis)
    {
      double[][] table =
        addOverall(
            testByType(corefClassifier, testFile, mergePronouns, verbosity));
      output =
        TableFormat.tableFormat(conditionedColumnLabels, conditionedRowLabels,
                                table, sigDigits, new int[]{ 0, 4 });
    }
    else
    {
      double[] results =
        test(corefClassifier, testFile, mergePronouns, verbosity);
      double[][] table = new double[][]{ (double[]) results.clone() };
      for (int i = 0; i < table[0].length; ++i) table[0][i] *= 100;
      output =
        TableFormat.tableFormat(
            null, unconditionedRowLabels, TableFormat.transpose(table),
            sigDigits);
    }

    for (int i = 0; i < output.length; ++i)
      System.out.println(output[i]);
  }


  /**
    * Tests the specified classifier on the specified testing data.
    *
    * @param classifier The specified classifier.
    * @param testFile   The name of the file containing testing data file
    *                   names.
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall, and
    *         <i>F<sub>1</sub></i> for all the documents named in the given
    *         file, in that order, in an array.
   **/
  public static double[] test(Classifier classifier, String testFile,
                              int verbosity)
  {
    return test(classifier, new ACE2004DocumentParser(testFile), verbosity);
  }


  /**
    * Tests the specified classifier on the specified testing data.
    *
    * @param classifier The specified classifier.
    * @param testFile   The name of the file containing testing data file
    *                   names.
    * @param merge      Setting to <code>true</code> enables a post-processing
    *                   step which greedily merges singleton pronoun clusters
    *                   with other clusters that improve overall
    *                   <i>F<sub>1</sub></i>.  <b>Note that this is
    *                   cheating.</b>
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall, and
    *         <i>F<sub>1</sub></i> for all the documents named in the given
    *         file, in that order, in an array.
   **/
  public static double[] test(Classifier classifier, String testFile,
                              boolean merge, int verbosity)
  {
    return
      test(classifier, new ACE2004DocumentParser(testFile), merge, verbosity);
  }


  /**
    * Tests the specified classifier on the specified testing data.
    *
    * @param classifier The specified classifier.
    * @param testFiles  The names of the files containing testing data.
    * @param root       The root directory in which to look for the files.
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall, and
    *         <i>F<sub>1</sub></i> for all the given documents, in that order,
    *         in an array.
   **/
  public static double[] test(Classifier classifier, String[] testFiles,
                              String root, int verbosity)
  {
    return
      test(classifier, new ACE2004DocumentParser(testFiles, root), verbosity);
  }


  /**
    * Tests the specified classifier on the specified testing data.
    *
    * @param classifier The specified classifier.
    * @param testFiles  The names of the files containing testing data.
    * @param root       The root directory in which to look for the files.
    * @param merge      Setting to <code>true</code> enables a post-processing
    *                   step which greedily merges singleton pronoun clusters
    *                   with other clusters that improve overall
    *                   <i>F<sub>1</sub></i>.  <b>Note that this is
    *                   cheating.</b>
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall, and
    *         <i>F<sub>1</sub></i> for all the given documents, in that order,
    *         in an array.
   **/
  public static double[] test(Classifier classifier, String[] testFiles,
                              String root, boolean merge, int verbosity)
  {
    return
      test(classifier, new ACE2004DocumentParser(testFiles, root), merge,
           verbosity);
  }


  /**
    * Tests the specified classifier on the specified testing data.
    *
    * @param classifier The specified classifier.
    * @param parser     A document parser.
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall, and
    *         <i>F<sub>1</sub></i> for all the documents from the given
    *         parser, in that order, in an array.
   **/
  public static double[] test(Classifier classifier,
                              ACE2004DocumentParser parser, int verbosity)
  {
    return test(classifier, parser, false, verbosity);
  }


  /**
    * Tests the specified classifier on the specified testing data.
    *
    * @param classifier The specified classifier.
    * @param parser     A document parser.
    * @param merge      Setting to <code>true</code> enables a post-processing
    *                   step which greedily merges singleton pronoun clusters
    *                   with other clusters that improve overall
    *                   <i>F<sub>1</sub></i>.  <b>Note that this is
    *                   cheating.</b>
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall, and
    *         <i>F<sub>1</sub></i> for all the documents from the given
    *         parser, in that order, in an array.
   **/
  public static double[] test(Classifier classifier,
                              ACE2004DocumentParser parser, boolean merge,
                              int verbosity)
  {
    double precision = 0;
    double recall = 0;
    int mentions = 0;
    int index = 1;

    for (Document d = (Document) parser.next(); d != null;
         d = (Document) parser.next(), ++index)
    {
      if (verbosity > 0) System.out.print(index + ": ");
      double[] results =
        new CoreferenceTester(verbosity).test(classifier, d, merge);
      precision += results[0];
      recall += results[1];
      mentions += d.totalMentions();

      if (verbosity > 1)
      {
        double p = precision;
        double r = recall;
        double F1 = 0;

        if (mentions > 0)
        {
          p /= (double) mentions;
          r /= (double) mentions;
          if (p + r != 0) F1 = F1(p, r);
        }

        System.out.println("  cumulative: " + p + ", " + r + ": " + F1);
      }
    }

    double F1 = 0;

    if (mentions > 0)
    {
      precision /= (double) mentions;
      recall /= (double) mentions;
      if (precision + recall != 0)
        F1 = F1(precision, recall);
    }

    return new double[]{ precision, recall, F1 };
  }


  /**
    * Tests the specified coref classifier on the specified testing data,
    * merges pronouns greedily, then tests the coherence classifier on
    * document pairs consisting of the coref classifier's original labels and
    * the labels after merging pronouns.
    *
    * @param coref      The coreference classifier.
    * @param cohere     The coherence classifier.
    * @param testFile   The name of the file containing testing data file
    *                   names.
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall, and
    *         <i>F<sub>1</sub></i> for all the documents from the given
    *         parser after merging and the accuracy of the coherence
    *         classifier in selecting the merged version, in that order, in an
    *         array.
   **/
  public static double[] test(Classifier coref, Classifier cohere,
                              String testFile, int verbosity)
  {
    ACE2004DocumentParser parser1 = new ACE2004DocumentParser(testFile);
    ACE2004DocumentParser parser2 = new ACE2004DocumentParser(testFile);
    double precision = 0;
    double recall = 0;
    int mentions = 0;
    int documents = 0;
    int cohereCorrect = 0;
    int index = 1;

    for (Document d = (Document) parser1.next(); d != null;
         d = (Document) parser1.next(), ++index)
    {
      if (verbosity > 0) System.out.print(index + ": ");
      double[] results =
        new CoreferenceTester(verbosity).test(coref, d, true);
      precision += results[0];
      recall += results[1];
      mentions += d.totalMentions();

      if (verbosity > 1)
      {
        double p = precision;
        double r = recall;
        double F1 = 0;

        if (mentions > 0)
        {
          p /= (double) mentions;
          r /= (double) mentions;
          if (p + r != 0) F1 = F1(p, r);
        }

        System.out.println("  cumulative: " + p + ", " + r + ": " + F1);
      }

      if (results[2] > 0.5)
      {
        Document unmerged = (Document) parser2.next();
        new CoreferenceTester(0).test(coref, unmerged);
        Random randomizer = new Random(unmerged.getName().hashCode());
        Document[] pair = null;
        boolean label = randomizer.nextBoolean();
        if (label) pair = new Document[]{ unmerged, d };
        else pair = new Document[]{ d, unmerged };
        if (("" + label).equals(cohere.discreteValue(pair))) ++cohereCorrect;
        ++documents;
      }
    }

    double cohereAccuracy = cohereCorrect / (double) documents;
    double F1 = 0;

    if (mentions > 0)
    {
      precision /= (double) mentions;
      recall /= (double) mentions;
      if (precision + recall != 0) F1 = F1(precision, recall);
    }

    return new double[]{ precision, recall, F1, cohereAccuracy };
  }


  /**
    * Tests the specified classifier on the specified testing data,
    * conditioned by mention type.
    *
    * @param classifier The specified classifier.
    * @param testFile   The name of the file containing testing data file
    *                   names.
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall,
    *         <i>F<sub>1</sub></i>, and counts for all the documents from the
    *         given parser, in that order, in an array, conditioned by mention
    *         type.  "Conditioned on mention type" means the returned array is
    *         two-dimensional.  The first dimension specifies precision,
    *         recall, <i>F<sub>1</sub></i>, or mention counts, and the second
    *         specifies the value of the statistic for mentions of type
    *         <code>NAM</code>, <code>NOM</code>, <code>PRO</code>, and
    *         <code>PRE</code>.
   **/
  public static double[][] testByType(Classifier classifier, String testFile,
                                      int verbosity)
  {
    return
      testByType(classifier, new ACE2004DocumentParser(testFile), false,
                 verbosity);
  }


  /**
    * Tests the specified classifier on the specified testing data,
    * conditioned by mention type.
    *
    * @param classifier The specified classifier.
    * @param testFile   The name of the file containing testing data file
    *                   names.
    * @param merge      Setting to <code>true</code> enables a post-processing
    *                   step which greedily merges singleton pronoun clusters
    *                   with other clusters that improve overall
    *                   <i>F<sub>1</sub></i>.  <b>Note that this is
    *                   cheating.</b>
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall,
    *         <i>F<sub>1</sub></i>, and counts for all the documents from the
    *         given parser, in that order, in an array, conditioned by mention
    *         type.  "Conditioned on mention type" means the returned array is
    *         two-dimensional.  The first dimension specifies precision,
    *         recall, <i>F<sub>1</sub></i>, or mention counts, and the second
    *         specifies the value of the statistic for mentions of type
    *         <code>NAM</code>, <code>NOM</code>, <code>PRO</code>, and
    *         <code>PRE</code>.
   **/
  public static double[][] testByType(Classifier classifier, String testFile,
                                      boolean merge, int verbosity)
  {
    return
      testByType(classifier, new ACE2004DocumentParser(testFile), merge,
                 verbosity);
  }


  /**
    * Tests the specified classifier on the specified testing data,
    * conditioned by mention type.
    *
    * @param classifier The specified classifier.
    * @param testFiles  The names of the files containing testing data.
    * @param root       The root directory in which to look for the files.
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall,
    *         <i>F<sub>1</sub></i>, and counts for all the documents from the
    *         given parser, in that order, in an array, conditioned by mention
    *         type.  "Conditioned on mention type" means the returned array is
    *         two-dimensional.  The first dimension specifies precision,
    *         recall, <i>F<sub>1</sub></i>, or mention counts, and the second
    *         specifies the value of the statistic for mentions of type
    *         <code>NAM</code>, <code>NOM</code>, <code>PRO</code>, and
    *         <code>PRE</code>.
   **/
  public static double[][] testByType(Classifier classifier,
                                      String[] testFiles, String root,
                                      int verbosity)
  {
    return
      testByType(classifier, new ACE2004DocumentParser(testFiles, root),
                 verbosity);
  }


  /**
    * Tests the specified classifier on the specified testing data,
    * conditioned by mention type.
    *
    * @param classifier The specified classifier.
    * @param testFiles  The names of the files containing testing data.
    * @param root       The root directory in which to look for the files.
    * @param merge      Setting to <code>true</code> enables a post-processing
    *                   step which greedily merges singleton pronoun clusters
    *                   with other clusters that improve overall
    *                   <i>F<sub>1</sub></i>.  <b>Note that this is
    *                   cheating.</b>
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall,
    *         <i>F<sub>1</sub></i>, and counts for all the documents from the
    *         given parser, in that order, in an array, conditioned by mention
    *         type.  "Conditioned on mention type" means the returned array is
    *         two-dimensional.  The first dimension specifies precision,
    *         recall, <i>F<sub>1</sub></i>, or mention counts, and the second
    *         specifies the value of the statistic for mentions of type
    *         <code>NAM</code>, <code>NOM</code>, <code>PRO</code>, and
    *         <code>PRE</code>.
   **/
  public static double[][] testByType(Classifier classifier,
                                      String[] testFiles, String root,
                                      boolean merge, int verbosity)
  {
    return
      testByType(classifier, new ACE2004DocumentParser(testFiles, root),
                 merge, verbosity);
  }


  /**
    * Tests the specified classifier on the specified testing data,
    * conditioned by mention type.
    *
    * @param classifier The specified classifier.
    * @param parser     A document parser.
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall,
    *         <i>F<sub>1</sub></i>, and counts for all the documents from the
    *         given parser, in that order, in an array, conditioned by mention
    *         type.  "Conditioned on mention type" means the returned array is
    *         two-dimensional.  The first dimension specifies precision,
    *         recall, <i>F<sub>1</sub></i>, or mention counts, and the second
    *         specifies the value of the statistic for mentions of type
    *         <code>NAM</code>, <code>NOM</code>, <code>PRO</code>, and
    *         <code>PRE</code>.
   **/
  public static double[][] testByType(Classifier classifier,
                                      ACE2004DocumentParser parser,
                                      int verbosity)
  {
    return testByType(classifier, parser, false, verbosity);
  }


  /**
    * Tests the specified classifier on the specified testing data,
    * conditioned by mention type.
    *
    * @param classifier The specified classifier.
    * @param parser     A document parser.
    * @param merge      Setting to <code>true</code> enables a post-processing
    *                   step which greedily merges singleton pronoun clusters
    *                   with other clusters that improve overall
    *                   <i>F<sub>1</sub></i>.  <b>Note that this is
    *                   cheating.</b>
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return The total <i>B<sup>3</sup></i> precision, recall,
    *         <i>F<sub>1</sub></i>, and counts for all the documents from the
    *         given parser, in that order, in an array, conditioned by mention
    *         type.  "Conditioned on mention type" means the returned array is
    *         two-dimensional.  The first dimension specifies precision,
    *         recall, <i>F<sub>1</sub></i>, or mention counts, and the second
    *         specifies the value of the statistic for mentions of type
    *         <code>NAM</code>, <code>NOM</code>, <code>PRO</code>, and
    *         <code>PRE</code>.
   **/
  public static double[][] testByType(Classifier classifier,
                                      ACE2004DocumentParser parser,
                                      boolean merge, int verbosity)
  {
    double[] precision = new double[4];
    double[] recall = new double[4];
    double[] counts = new double[4];
    int mentions = 0;
    int index = 1;

    for (Document d = (Document) parser.next(); d != null;
         d = (Document) parser.next(), ++index)
    {
      if (verbosity > 0) System.out.print(index + ": ");
      double[][] results =
        new CoreferenceTester(verbosity).testByType(classifier, d, merge);

      for (int i = 0; i < 4; ++i)
      {
        precision[i] += results[0][i];
        recall[i] += results[1][i];
        counts[i] += results[2][i];
      }

      mentions += d.totalMentions();

      if (verbosity > 1)
      {
        double[] p = (double[]) precision.clone();
        double[] r = (double[]) recall.clone();
        double[] F1 = new double[4];

        for (int i = 0; i < 4; ++i)
          if (counts[i] > 0)
          {
            p[i] /= counts[i];
            r[i] /= counts[i];
            if (p[i] + r[i] != 0) F1[i] = F1(p[i], r[i]);
          }

        System.out.println("  cumulative:");
        for (int i = 0; i < 4; ++i)
          System.out.println("    " + Document.mentionTypes[i] + ": "
                             + p[i] + ", " + r[i] + ": " + F1[i]);
      }
    }

    double[] F1 = new double[4];

    for (int i = 0; i < 4; ++i)
      if (counts[i] > 0)
      {
        precision[i] /= counts[i];
        recall[i] /= counts[i];
        if (precision[i] + recall[i] != 0)
          F1[i] = F1(precision[i], recall[i]);
      }

    return new double[][]{ precision, recall, F1, counts };
  }


  /**
    * Takes the results from a call to a <code>testByType()</code> method,
    * transposes them, multiplies the precision and recall values by 100, and
    * adds a row for overall results.
    *
    * @param results  The results from a call to a <code>testByType()</code>
    *                 method.
    * @return Those same results, after transposition and addition of an
    *         overall performance row.
   **/
  public static double[][] addOverall(double[][] results)
  {
    results = TableFormat.transpose(results);
    double[][] table =
      new double[][]{ results[0], results[1], results[2], results[3],
                      new double[4] };

    for (int i = 0; i < table.length - 1; ++i)
    {
      for (int j = 0; j < 3; ++j)
        table[i][j] *= 100;
      table[table.length - 1][3] += table[i][3];
    }

    for (int j = 0; j < 2; ++j)
    {
      for (int i = 0; i < table.length - 1; ++i)
        table[table.length - 1][j] += table[i][j] * table[i][3];
      table[table.length - 1][j] /= table[table.length - 1][3];
    }

    table[table.length - 1][2] =
      F1(table[table.length - 1][0], table[table.length - 1][1]);

    return table;
  }


  /**
    * Caches the clusters produced for mentions by
    * {@link #getCluster(Document,int,Document.Mention)}.
   **/
  protected Vector<Vector<HashSet<Document.Mention>>> clusterCache;
  /** Indicates how much output should be sent to <code>STDOUT</code>. */
  protected int verbosity;


  /** Simply sets {@link #verbosity} to 0. */
  public CoreferenceTester() { this(0); }

  /**
    * Initializing constructor.
    *
    * @param v  A value for {@link #verbosity}.
   **/
  public CoreferenceTester(int v) { verbosity = v; }


  /**
    * (Re)initializes the {@link #clusterCache}.
    *
    * @param c  The number of columns in the cache.
   **/
  protected void initializeClusterCache(int c)
  {
    Vector<HashSet<Document.Mention>> row =
      new Vector<HashSet<Document.Mention>>();
    row.setSize(c);
    clusterCache = new Vector<Vector<HashSet<Document.Mention>>>();
    clusterCache.add(row);
    row = new Vector<HashSet<Document.Mention>>();
    row.setSize(c);
    clusterCache.add(row);
  }


  /**
    * Returns the <i>B<sup>3</sup> F<sub>1</sub></i> of the specified
    * classifier on the specified document.
    *
    * @param classifier The specified classifier.
    * @param document   The document to test.
    * @return The <i>B<sup>3</sup> F<sub>1</sub></i> of the specified
    *         classifier on the specified document.
   **/
  public double getF1(Classifier classifier, Document document)
  {
    double[] results = test(classifier, document);
    double F1 = 0;
    if (results[0] + results[1] != 0)
      F1 = F1(results[0], results[1]) / document.totalMentions();
    return F1;
  }


  /**
    * Returns the <i>B<sup>3</sup> F<sub>1</sub></i> of the first specified
    * document relative to the second specified document.
    *
    * @param predicted  The document to test.
    * @param labeled    The document to test against.
    * @return The <i>B<sup>3</sup> F<sub>1</sub></i> of the first specified
    *         document relative to the second specified document.
   **/
  public double getF1(Document predicted, Document labeled)
  {
    double[] results = test(predicted, labeled);
    int mentions = labeled.totalMentions();
    results[0] /= mentions;
    results[1] /= mentions;
    double F1 = 0;
    if (results[0] + results[1] != 0) F1 = F1(results[0], results[1]);
    return F1;
  }


  /**
    * Tests the specified classifier on a single labeled testing document.
    *
    * @param classifier The specified classifier.
    * @param document   The document to test.
    * @return The <b>cumulative</b> <i>B<sup>3</sup></i> precision and recall
    *         for the mentions in a given document, in that order, in an
    *         array.  Cumulative means that these values must be divided by
    *         the number of mentions in the document in order to get the
    *         precision and recall for the document.
   **/
  public double[] test(Classifier classifier, Document document)
  {
    return test(classifier, document, false);
  }


  /**
    * Tests the specified classifier on a single labeled testing document.
    *
    * @param classifier The specified classifier.
    * @param labeled    The document to test.
    * @param merge      Setting to <code>true</code> enables a post-processing
    *                   step which greedily merges singleton pronoun clusters
    *                   with other clusters that improve overall
    *                   <i>F<sub>1</sub></i>.  <b>Note that this is
    *                   cheating.</b>
    * @return The <b>cumulative</b> <i>B<sup>3</sup></i> precision and recall
    *         for the mentions in a given document, in that order, in an
    *         array.  Cumulative means that these values must be divided by
    *         the number of mentions in the document in order to get the
    *         precision and recall for the document.
   **/
  public double[] test(Classifier classifier, Document labeled, boolean merge)
  {
    Document predicted = new Document(labeled);
    predicted.fillInPredictions(classifier, verbosity);
    if (verbosity > 2)
      System.out.print(labeled.getName() + ", " + classifier.name);
    return test(predicted, labeled, merge);
  }


  /**
    * Tests the specified classifier on a single labeled testing document.
    *
    * @param predicted  A document whose entity IDs are predictions from a
    *                   classifier.
    * @param labeled    The same document with labels in its entity IDs.
    * @return The <b>cumulative</b> <i>B<sup>3</sup></i> precision and recall
    *         for the mentions in a given document, in that order, in an
    *         array.  Cumulative means that these values must be divided by
    *         the number of mentions in the document in order to get the
    *         precision and recall for the document.
   **/
  public double[] test(Document predicted, Document labeled)
  {
    return test(predicted, labeled, false);
  }


  /**
    * Tests the specified classifier on a single labeled testing document.
    *
    * @param predicted  A document whose entity IDs are predictions from a
    *                   classifier.
    * @param labeled    The same document with labels in its entity IDs.
    * @param merge      Setting to <code>true</code> enables a post-processing
    *                   step which greedily merges singleton pronoun clusters
    *                   with other clusters that improve overall
    *                   <i>F<sub>1</sub></i>.  <b>Note that this is
    *                   cheating.</b>
    * @return The <b>cumulative</b> <i>B<sup>3</sup></i> precision and recall
    *         for the mentions in a given document, in that order, in an
    *         array.  Cumulative means that these values must be divided by
    *         the number of mentions in the document in order to get the
    *         precision and recall for the document.
   **/
  public double[] test(Document predicted, Document labeled, boolean merge)
  {
    if (verbosity > 0)
      System.out.println(labeled.getName() + ": " + new Date());

    double precision = 0;
    double recall = 0;
    int I = 0;
    boolean merging = false;

    if (verbosity > 2)
    {
      for (int i = 0; i < predicted.sentences(); ++i)
        for (int j = 0; j < predicted.mentionsInSentence(i); ++j)
        {
          Document.Mention m = predicted.getMention(i, j);
          System.out.print(m.getEntityID() + ", ");
        }

      System.out.println();
    }

    if (merge)
    {
      int[] counts = mergeSingletonPronouns(predicted, labeled, verbosity);
      merging = counts[0] > 0;

      if (!merging)
      {
        if (verbosity > 0)
          System.out.println("  no merging");
      }
    }
    else initializeClusterCache(labeled.totalMentions());

    for (int i = 0; i < labeled.sentences(); ++i)
      for (int j = 0; j < labeled.mentionsInSentence(i); ++j, ++I)
      {
        Document.Mention m = labeled.getMention(i, j);
        HashSet<Document.Mention> trueCluster = getCluster(labeled, 1, m);
        HashSet<Document.Mention> predictedCluster =
          getCluster(predicted, 0, predicted.getMention(i, j));
        int overlap = clusterOverlap(predictedCluster, trueCluster);
        precision += overlap / (double) predictedCluster.size();
        recall += overlap / (double) trueCluster.size();

        if (verbosity > 2)
        {
          System.out.println("  " + I + ": " + m.getMentionID());
          System.out.println(
              "    " + clusterToString("predicted", predictedCluster));
          System.out.println("    " + clusterToString("true", trueCluster));
          System.out.println("    overlap: " + overlap);
        }
      }

    if (verbosity > 0)
    {
      double p = precision;
      double r = recall;
      double F1 = 0;
      double mentions = labeled.totalMentions();

      if (mentions > 0)
      {
        p /= mentions;
        r /= mentions;
        if (p + r != 0) F1 = F1(p, r);
      }

      System.out.println("  document: " + p + ", " + r + ": " + F1);
    }

    return new double[]{ precision, recall, merging ? 1 : 0 };
  }


  /**
    * Tests the specified classifier on a single labeled testing document,
    * conditioned by mention type.
    *
    * @param classifier The specified classifier.
    * @param document   The document to test.
    * @return The <b>cumulative</b> <i>B<sup>3</sup></i> precision, recall,
    *         and counts for the mentions in a given document, in that order,
    *         in an array, conditioned on mention type.  "Cumulative" means
    *         that these values must be divided by the number of mentions in
    *         the document in order to get the precision and recall for the
    *         document.  "Conditioned on mention type" means the returned
    *         array is two-dimensional.  The first dimension specifies
    *         precision, recall, or mention counts, and the second specifies
    *         the value of the statistic for mentions of type
    *         <code>NAM</code>, the second for <code>NOM</code>, the third for
    *         <code>PRO</code>, and the last for <code>PRE</code>.
   **/
  public double[][] testByType(Classifier classifier, Document document)
  {
    return testByType(classifier, document, false);
  }


  /**
    * Tests the specified classifier on a single labeled testing document,
    * conditioned by mention type.
    *
    * @param classifier The specified classifier.
    * @param labeled    The document to test.
    * @param merge      Setting to <code>true</code> enables a post-processing
    *                   step which greedily merges singleton pronoun clusters
    *                   with other clusters that improve overall
    *                   <i>F<sub>1</sub></i>.  <b>Note that this is
    *                   cheating.</b>
    * @return The <b>cumulative</b> <i>B<sup>3</sup></i> precision, recall,
    *         and counts for the mentions in a given document, in that order,
    *         in an array, conditioned on mention type.  "Cumulative" means
    *         that these values must be divided by the number of mentions in
    *         the document in order to get the precision and recall for the
    *         document.  "Conditioned on mention type" means the returned
    *         array is two-dimensional.  The first dimension specifies
    *         precision, recall, or mention counts, and the second specifies
    *         the value of the statistic for mentions of type
    *         <code>NAM</code>, the second for <code>NOM</code>, the third for
    *         <code>PRO</code>, and the last for <code>PRE</code>.
   **/
  public double[][] testByType(Classifier classifier, Document labeled,
                               boolean merge)
  {
    Document predicted = new Document(labeled);
    predicted.fillInPredictions(classifier, verbosity);
    if (verbosity > 2)
      System.out.print(labeled.getName() + ", " + classifier.name);
    return testByType(predicted, labeled, merge);
  }


  /**
    * Tests the specified classifier on a single labeled testing document,
    * conditioned by mention type.
    *
    * @param predicted  A document whose entity IDs are predictions from a
    *                   classifier.
    * @param labeled    The same document with labels in its entity IDs.
    * @return The <b>cumulative</b> <i>B<sup>3</sup></i> precision, recall,
    *         and counts for the mentions in a given document, in that order,
    *         in an array, conditioned on mention type.  "Cumulative" means
    *         that these values must be divided by the number of mentions in
    *         the document in order to get the precision and recall for the
    *         document.  "Conditioned on mention type" means the returned
    *         array is two-dimensional.  The first dimension specifies
    *         precision, recall, or mention counts, and the second specifies
    *         the value of the statistic for mentions of type
    *         <code>NAM</code>, the second for <code>NOM</code>, the third for
    *         <code>PRO</code>, and the last for <code>PRE</code>.
   **/
  public double[][] testByType(Document predicted, Document labeled)
  {
    return testByType(predicted, labeled, false);
  }


  /**
    * Tests the specified classifier on a single labeled testing document,
    * conditioned by mention type.
    *
    * @param predicted  A document whose entity IDs are predictions from a
    *                   classifier.
    * @param labeled    The same document with labels in its entity IDs.
    * @param merge      Setting to <code>true</code> enables a post-processing
    *                   step which greedily merges singleton pronoun clusters
    *                   with other clusters that improve overall
    *                   <i>F<sub>1</sub></i>.  <b>Note that this is
    *                   cheating.</b>
    * @return The <b>cumulative</b> <i>B<sup>3</sup></i> precision, recall,
    *         and counts for the mentions in a given document, in that order,
    *         in an array, conditioned on mention type.  "Cumulative" means
    *         that these values must be divided by the number of mentions in
    *         the document in order to get the precision and recall for the
    *         document.  "Conditioned on mention type" means the returned
    *         array is two-dimensional.  The first dimension specifies
    *         precision, recall, or mention counts, and the second specifies
    *         the value of the statistic for mentions of type
    *         <code>NAM</code>, the second for <code>NOM</code>, the third for
    *         <code>PRO</code>, and the last for <code>PRE</code>.
   **/
  public double[][] testByType(Document predicted, Document labeled,
                               boolean merge)
  {
    if (verbosity > 0)
      System.out.println(labeled.getName() + ": " + new Date());

    double[] precision = new double[4];
    double[] recall = new double[4];
    double[] counts = new double[4];
    int I = 0;

    if (verbosity > 2)
    {
      for (int i = 0; i < predicted.sentences(); ++i)
        for (int j = 0; j < predicted.mentionsInSentence(i); ++j)
        {
          Document.Mention m = predicted.getMention(i, j);
          System.out.print(m.getEntityID() + ", ");
        }

      System.out.println();
    }

    if (merge) mergeSingletonPronouns(predicted, labeled, verbosity);
    else initializeClusterCache(labeled.totalMentions());

    for (int i = 0; i < labeled.sentences(); ++i)
      for (int j = 0; j < labeled.mentionsInSentence(i); ++j, ++I)
      {
        Document.Mention m = labeled.getMention(i, j);
        int index = 0;
        while (!m.getType().equals(Document.mentionTypes[index]))
          ++index;

        HashSet<Document.Mention> trueCluster = getCluster(labeled, 1, m);
        HashSet<Document.Mention> predictedCluster =
          getCluster(predicted, 0, predicted.getMention(i, j));
        int overlap = clusterOverlap(predictedCluster, trueCluster);

        precision[index] += overlap / (double) predictedCluster.size();
        recall[index] += overlap / (double) trueCluster.size();
        ++counts[index];

        if (verbosity > 2)
        {
          System.out.println("  " + I + ": " + m.getMentionID());
          System.out.println(
              "    " + clusterToString("predicted", predictedCluster));
          System.out.println("    " + clusterToString("true", trueCluster));
          System.out.println("    overlap: " + overlap);
        }
      }

    if (verbosity > 1)
    {
      double[] p = (double[]) precision.clone();
      double[] r = (double[]) recall.clone();
      double[] F1 = new double[4];

      for (int i = 0; i < 4; ++i)
        if (counts[i] > 0)
        {
          p[i] /= counts[i];
          r[i] /= counts[i];
          if (p[i] + r[i] != 0) F1[i] = F1(p[i], r[i]);
        }

      double[][] table = addOverall(new double[][]{ p, r, F1, counts });
      String[] output =
        TableFormat.tableFormat(conditionedColumnLabels, conditionedRowLabels,
                                table, sigDigits, new int[]{ 0, 4 });
      for (int i = 0; i < output.length; ++i)
        System.out.println(output[i]);
    }

    return new double[][]{ precision, recall, counts };
  }


  /**
    * This method implements a post-processing step which greedily merges
    * singleton pronoun clusters with other clusters that improve overall
    * <i>F<sub>1</sub></i>.  <b>Note that this is cheating.</b>  The acutal
    * merging is accomplished simply by setting the
    * {@link Document.Mention#entityID} fields.
    *
    * @param predicted  A document whose entity IDs are predictions from a
    *                   classifier.
    * @param labeled    The same document with labels in its entity IDs.
    * @param verbosity  The level of verbosity for output sent to
    *                   <code>STDOUT</code>.
    * @return An array containing a count of the number of singleton pronouns
    *         which were successfully merged (i.e., merging them resulted in a
    *         performance improvement) followed by a count of the number of
    *         singleton pronouns which could not be merged.
   **/
  public int[] mergeSingletonPronouns(Document predicted, Document labeled,
                                      int verbosity)
  {
    double[] results = test(predicted, labeled);
    double bestF1 = F1(results[0], results[1]);
    int[] result = new int[2];

    if (verbosity > 1) System.out.println("--------------------");

    for (int i = 0; i < labeled.sentences(); ++i)
      for (int j = 0; j < labeled.mentionsInSentence(i); ++j)
      {
        Document.Mention m = predicted.getMention(i, j);
        if (!m.getType().equals("PRO")) continue;

        if (verbosity > 1)
          System.out.print("mention " + m.getIndexInDocument() + ": ");

        HashSet<Document.Mention> predCluster = getCluster(predicted, 0, m);
        HashSet<Document.Mention> goldCluster =
          getCluster(labeled, 1, labeled.getMention(i, j));

        if (predCluster.size() != 1 || goldCluster.size() == 1)
        {
          if (verbosity > 1)
          {
            if (predCluster.size() != 1)
              System.out.println("not predicted a singleton");
            if (goldCluster.size() == 1)
              System.out.println("really a singleton");
          }

          continue;
        }

        int mi = m.getSentenceIndex();
        int mj = m.getIndexInSentence();
        String originalLabel = m.getEntityID();
        Document.Mention bestMention = null;

        for (Document.Mention g : goldCluster)
        {
          int gi = g.getSentenceIndex();
          int gj = g.getIndexInSentence();
          if (gi == mi && gj == mj) continue;

          m.setEntityID(predicted.getMention(gi, gj).getEntityID());
          results = new CoreferenceTester(0).test(predicted, labeled);
          double currentF1 = F1(results[0], results[1]);

          if (currentF1 > bestF1)
          {
            bestF1 = currentF1;
            bestMention = g;
          }
        }

        if (bestMention == null)
        {
          m.setEntityID(originalLabel);
          ++result[1];
          if (verbosity > 1) System.out.println("bad singleton");
        }
        else
        {
          int bi = bestMention.getSentenceIndex();
          int bj = bestMention.getIndexInSentence();
          m.setEntityID(predicted.getMention(bi, bj).getEntityID());
          ++result[0];
          HashSet<Document.Mention> cluster =
            clusterCache.get(0).get(bestMention.getIndexInDocument());
          cluster.add(m);
          clusterCache.get(0).set(m.getIndexInDocument(), cluster);

          if (verbosity > 1)
          {
            for (Document.Mention k : cluster)
              System.out.print(k.getIndexInDocument() + ", ");
            System.out.println();
          }
        }
      }

    return result;
  }


  /**
    * This method creates mention graphs based on the links predicted by a
    * given classifier for a given document.  The first graph (in the first
    * element of the first dimension of the result) is the mention graph
    * predicted by the classifier.  The second is the true mention graph.
    *
    * @param d          The document to create graphs for.
    * @param classifier The classifier that predicts links between mentions.
    * @return The two graphs (which are two-dimensional arrays) in an array.
   **
  protected boolean[][][] makeGraphs(Document d, Classifier classifier)
  {
    clusterCache = new HashSet[2][d.totalMentions()];
    boolean[][][] result =
      new boolean[2][d.totalMentions()][d.totalMentions()];

    for (Iterator I = d.iterator(); I.hasNext(); )
    {
      Mention m1 = (Mention) I.next();
      int i1 = m1.getIndexInDocument();

      for (Iterator J = d.iterator(); J.hasNext(); )
      {
        Mention m2 = (Mention) J.next();
        int i2 = m2.getIndexInDocument();

        if (i1 == i2)
        {
          result[0][i1][i1] = result[1][i1][i1] = true;
          continue;
        }

        result[0][i1][i2] = result[0][i2][i1] =
          classifier.discreteValue(d.getMentionPair(m1, m2)).equals("true");
        result[1][i1][i2] = result[1][i2][i1] =
          m1.getEntityID().equals(m2.getEntityID());
      }
    }

    return result;
  }
  */


  /**
    * An alternative to {@link #makeGraphs(Document,Classifier)} which fills
    * in the {@link Mention#predictedEntityID} field of the mentions in the
    * document instead of building a graph.
    *
    * @param d          The document in which to fill in predictions.
    * @param classifier The classifier that predicts links between mentions.
   **
  protected void fillInPredictions(Document d, Classifier classifier)
  {
    clusterCache = new HashSet[2][d.totalMentions()];
    int nextEntityID = 0;
    String message = null;
    if (verbosity > 2) message = d.getName() + ", " + classifier.name + ": ";

    for (Iterator I = d.iterator(); I.hasNext(); )
    {
      Mention current = (Mention) I.next();
      boolean assigned = false;

      for (Iterator J = d.iterator(); J.hasNext() && !assigned; )
      {
        Mention before = (Mention) J.next();
        if (before == current) break;

        if (classifier.discreteValue(d.getMentionPair(before, current))
            .equals("true"))
        {
          current.predictedEntityID = before.predictedEntityID;
          assigned = true;
        }
      }

      if (!assigned) current.predictedEntityID = "e" + nextEntityID++;
      if (verbosity > 2) message += current.predictedEntityID + ", ";
    }

    if (verbosity > 2) System.out.println(message);
  }
  */


  /**
    * This method computes the set of mentions linked to a given mention
    * through transitive coreference closure in a given document.
    *
    * @param graph  A graph representation of coreference.
    * @param i      The index of the graph to look in.
    * @param m      The index of the mention whose cluster is requested.
    * @return The cluster for the given mention in the specified graph.
   **
  protected HashSet getCluster(boolean[][][] graph, int i, int m)
  {
    if (clusterCache[i][m] != null) return clusterCache[i][m];
    clusterCache[i][m] = new HashSet();

    boolean[] inQueue = new boolean[graph[i].length];
    LinkedList queue = new LinkedList();
    queue.add(new Integer(m));
    inQueue[m] = true;

    while (queue.size() > 0)
    {
      Integer Index = (Integer) queue.removeFirst();
      int index = Index.intValue();

      clusterCache[i][m].add(Index);
      clusterCache[i][index] = clusterCache[i][m];

      for (int j = 0; j < graph[i][index].length; ++j)
        if (graph[i][index][j] && !inQueue[j])
        {
          queue.add(new Integer(j));
          inQueue[j] = true;
        }
    }

    return clusterCache[i][m];
  }
  */


  /**
    * An alternative to <code>getCluster(boolean[][][],int,int)</code> which
    * returns an entirely equivalent set of mention indexes based on the
    * {@link Document.Mention#entityID} field of the mentions in the document.
    *
    * @param d  The document in which the cluster is found.
    * @param l  Index into the cluster cache; 0 for the predicted cluster, or
    *           1 for the labeled cluster.
    * @param m  The mention whose cluster is requested.
   **/
  protected HashSet<Document.Mention> getCluster(Document d, int l,
                                                 Document.Mention m)
  {
    int i = m.getIndexInDocument();
    HashSet<Document.Mention> cluster = clusterCache.get(l).get(i);
    if (cluster != null) return cluster;

    cluster = d.getCluster(m);

    for (Document.Mention mention : cluster)
    {
      i = mention.getIndexInDocument();
      clusterCache.get(l).set(i, cluster);
    }

    return cluster;
  }


  /**
    * Computes the size of the overlap between two clusters.
    *
    * @param c1 The first cluster.
    * @param c2 The second cluster.
    * @return The size of the overlap between the two clusters.
   **/
  protected int clusterOverlap(HashSet<Document.Mention> c1,
                               HashSet<Document.Mention> c2)
  {
    if (c2.size() < c1.size())
    {
      HashSet<Document.Mention> t = c1;
      c1 = c2;
      c2 = t;
    }

    int result = 0;
    for (Iterator I = c1.iterator(); I.hasNext(); )
      if (c2.contains(I.next())) ++result;
    return result;
  }


  /**
    * Creates a textual representation of a cluster.
    *
    * @param name     A name for the cluster.
    * @param cluster  The set of mentions representing the cluster.
    * @return A textual representation of a cluster.
   **/
  protected String clusterToString(String name,
                                   HashSet<Document.Mention> cluster)
  {
    String result = name + " cluster:";
    Document.Mention[] mentions = cluster.toArray(new Document.Mention[0]);
    Arrays.sort(mentions);
    if (mentions.length > 0) result += " " + mentions[0];
    for (int i = 1; i < mentions.length; ++i) result += ", " + mentions[i];
    return result;
  }
}

