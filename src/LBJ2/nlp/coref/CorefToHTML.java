package LBJ2.nlp.coref;

import java.io.*;
import LBJ2.classify.Classifier;
import LBJ2.util.ClassUtils;


/**
  * A program that writes documents to HTML files with coreference predictions
  * highlighted.
  *
  * <h4>Usage</h4>
  * <blockquote><pre>
  *   java LBJ2.nlp.coref.CorefToHTML &lt;test data&gt; &lt;directory&gt; \
  *                                   [&lt;classifier&gt;]
  * </pre></blockquote>
  *
  * <h4>Input</h4>
  * <p> <code>&lt;test data&gt;</code> is the name of a
  * file containing test data.  <code>&lt;directory&gt;</code> is the name of
  * a directory into which the HTML will be generated.
  * <code>&lt;classifier&gt;</code> is the fully qualified class name of the
  * classifier to test.  If no classifier is specified, gold labels are used
  * instead.
  *
  * <h4>Output</h4>
  * HTML files are created in the specified directory displaying the files in
  * the test data highlighted with coreference labels or predictions.
 **/
public class CorefToHTML
{
  public static void main(String[] args)
  {
    String classifierName = null;
    String testFile = null;
    String directory = null;

    try
    {
      testFile = args[0];
      directory = args[1];
      if (args.length == 3) classifierName = args[2];
      if (args.length > 3) throw new Exception();
    }
    catch (Exception e)
    {
      System.out.println(
        "usage: java LBJ2.nlp.coref.CorefToHTML <test data> <directory> \\\n"
      + "                                       [<classifier>]");
      System.exit(1);
    }

    Classifier classifier = null;

    if (classifierName == null) directory += "/gold";
    else
    {
      directory += "/" + classifierName;
      classifier = ClassUtils.getClassifier(classifierName);
    }

    new File(directory).mkdirs();
    ACE2004DocumentParser parser = new ACE2004DocumentParser(testFile);
    PrintStream out = null;

    try
    {
      out = new PrintStream(new FileOutputStream(directory + "/index.html"));
    }
    catch (Exception e)
    {
      System.err.println(
          "Can't open " + directory + "/index.html for output: " + e);
      System.exit(1);
    }

    String testFileName = testFile;
    int index = testFileName.lastIndexOf('/');
    testFileName = testFileName.substring(index + 1);

    out.println("<html>");
    out.println("<head>");
    out.println("<title>Files from " + testFileName + "</title>");
    out.println("</head>");
    out.println("<body><ul>");

    for (Document d = (Document) parser.next(); d != null;
         d = (Document) parser.next())
    {
      if (classifier != null) d.fillInPredictions(classifier, 0);
      d.writeHTML(directory);
      out.println(
          "<li> <a href=\"" + d.getName() + "/all.html\">" + d.getName());
    }

    out.println("</ul></body></html>");
    out.close();
  }
}

