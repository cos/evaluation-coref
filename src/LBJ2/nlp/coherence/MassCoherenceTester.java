package LBJ2.nlp.coherence;

import java.util.Date;
import LBJ2.classify.*;
import LBJ2.learn.*;
import LBJ2.parse.Parser;
import LBJ2.nlp.coref.*;
import LBJ2.util.TableFormat;
import LBJ2.util.ClassUtils;


/**
  * This class may be used to produce a report of the accuracy of a coherence
  * classifier.
  *
  * <h4>Usage</h4>
  * <blockquote><pre>
  *   java LBJ2.nlp.coherence.MassCoherenceTester \
  *          &lt;labeled data&gt; &lt;coherence classifier&gt; ...
  * </pre></blockquote>
  *
  * <h4>Input</h4>
  * <code>&lt;labeled data&gt;</code> is the name of a file containing names
  * of ACE 2004 documents.  <code>&lt;coherence classifier&gt;</code> is the
  * fully qualified name of the coherence classifier to test.  Multiple such
  * classifiers can be specified.
  *
  * <h4>Output</h4>
  * A table of resutls for each classifier tested in a variety of different
  * settings.
 **/
public class MassCoherenceTester
{
  /** For now, this progam does not understand verbosity. */
  private static final int verbosity = 0;


  private static final String[] columnLabels =
  {
    "Name", "SS20", "SE20", "NCSPO", "NNPTPSPO", "FPLSPO", "P-NCSPO",
    "P-NNPTPSPO", "P-FPLSPO"
  };


  private static final Classifier[] labelers =
    new Classifier[columnLabels.length - 1];
  static
  {
    labelers[0] = new CoherenceLabel();
    for (int i = 1; i < 5; ++i) labelers[i] = labelers[0];
    labelers[5] = new B3Label();
    for (int i = 6; i < 8; ++i) labelers[i] = labelers[5];
  }


  public static void main(String[] args)
  {
    String coherenceName = null;
    String filename = null;

    try
    {
      filename = args[0];
      if (args.length < 2) throw new Exception();
    }
    catch (Exception e)
    {
      System.err.println(
            "usage: java LBJ2.nlp.coherence.MassCoherenceTester \\\n"
          + "              <labeled data> <coherence classifier> ...");
      System.exit(1);
    }

    String total = "" + (args.length - 1);
    Parser[] parsers = getParsers(filename);
    double[][] results = new double[args.length - 1][parsers.length];
    String[] rowLabels = new String[args.length - 1];

    for (int i = 1; i < args.length; ++i)
    {
      Learner cohere = ClassUtils.getLearner(args[i]);
      rowLabels[i - 1] = args[i].substring(args[i].lastIndexOf('.') + 1);
      String current = "" + i;
      while (current.length() < total.length()) current = " " + current;
      System.err.println("Processing classifier " + current + " / "
                         + total + ": " + rowLabels[i - 1] + ", at "
                         + new Date());

      for (int j = 0; j < parsers.length; ++j)
      {
        TestDiscrete tester = new TestDiscrete();

        for (Object e = parsers[j].next(); e != null; e = parsers[j].next())
        {
          String label = labelers[j].discreteValue(e);
          String prediction = cohere.discreteValue(e);
          tester.reportPrediction(prediction, label);

          if (verbosity > 0)
          {
            Document[] pair = (Document[]) e;
            System.err.println(
                pair[0].getName() + ": " + cohere.scores(e).get(label));
            if (verbosity > 1)
              System.err.println(cohere.getExtractor().classify(e));
          }
        }

        results[i - 1][j] = 100 * tester.getOverallStats()[0];
        parsers[j].reset();
      }
    }

    String[] output =
      TableFormat.tableFormat(columnLabels, rowLabels, results);
    for (int i = 0; i < output.length; ++i)
      System.out.println(output[i]);
  }


  private static Parser[] getParsers(String filename)
  {
    Parser[] result = new Parser[columnLabels.length - 1];

    result[0] =
      new DocumentPairParser(
          new ACE2004DocumentParser(filename), 20, false, true);
    result[1] =
      new DocumentPairParser(
          new ACE2004DocumentParser(filename), 20, true, true);

    Classifier coref = new HCBLDataCoref();

    result[2] =
      new DocumentPairParser(
          new ACE2004DocumentParser(filename),
          new PredictedDocumentParser(
            new ACE2004DocumentParser(filename), coref),
          true);

    result[5] =
      new DocumentPairParser(
          new PredictedDocumentParser(
            new ACE2004DocumentParser(filename),
            new PerturbedLabels()),
          new PredictedDocumentParser(
            new ACE2004DocumentParser(filename), coref),
          true);

    coref = new HCNNPTPDataCoref();

    result[3] =
      new DocumentPairParser(
          new ACE2004DocumentParser(filename),
          new PredictedDocumentParser(
            new ACE2004DocumentParser(filename), coref),
          true);

    result[6] =
      new DocumentPairParser(
          new PredictedDocumentParser(
            new ACE2004DocumentParser(filename),
            new PerturbedLabels()),
          new PredictedDocumentParser(
            new ACE2004DocumentParser(filename), coref),
          true);

    //coref = new FPLDataCoref();

    result[4] =
      new DocumentPairParser(
          new ACE2004DocumentParser(filename),
          new PredictedDocumentParser(
            new ACE2004DocumentParser(filename), coref),
          true);

    result[7] =
      new DocumentPairParser(
          new PredictedDocumentParser(
            new ACE2004DocumentParser(filename),
            new PerturbedLabels()),
          new PredictedDocumentParser(
            new ACE2004DocumentParser(filename), coref),
          true);

    return result;
  }
}

