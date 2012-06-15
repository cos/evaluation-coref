package LBJ2.nlp.coref;

import java.util.*;
import LBJ2.classify.Classifier;
import LBJ2.util.ClassUtils;


/**
  * This program is intended to test a coreference classifier that uses a
  * {@link DataCoref} classifier in some way with the <i>B<sup>3</sup></i>
  * evaluation metric.
  *
  * <h4>Usage</h4>
  * <blockquote><pre>
  *   java LBJ2.nlp.coref.DataCorefTester &lt;classifier&gt; \
  *                                       &lt;test data&gt; \
  *                                       &lt;threshold&gt; \
  *                                       [&lt;verbosity=0&gt;]
  * </pre></blockquote>
  *
  * <h4>Input</h4>
  * <p> <code>&lt;classifier&gt;</code> is the fully qualified class name of
  * the classifier to test.  <code>&lt;test data&gt;</code> is the name of a
  * file containing test data.  <code>&lt;threshold&gt;</code> is used to set
  * the threshold adjustment of the classifier.  The higher the integer
  * supplied for <code>&lt;verbosity&gt;</code>, the more information will be
  * output to <code>STDOUT</code>.
  *
  * <h4>Output</h4>
  * <p> Precision, recall, and <i>F<sub>1</sub></i> are sent to
  * <code>STDOUT</code> as computed by <i>B<sup>3</sup></i>.
 **/
public class DataCorefTester
{
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
      threshold = Double.parseDouble(args[2]);
      if (args.length > 3) verbosity = Integer.parseInt(args[3]);
      if (args.length > 4) throw new Exception();
    }
    catch (Exception e)
    {
      System.err.println(
  "usage: java LBJ2.nlp.coref.DataCorefTester <classifier> <test data> \\\n"
+ "                                           <threshold> [<verbosity=0>]");
      System.exit(1);
    }

    Class classifierClass = null;
    DataCoref classifier =
      (DataCoref) ClassUtils.getClassifier(classifierName);
    classifier.setThreshold(threshold);

    double[] results =
      CoreferenceTester.test(classifier, testFile, verbosity);

    System.out.println("precision: " + results[0]);
    System.out.println("recall:    " + results[1]);
    System.out.println("F1:        " + results[2]);
  }
}

