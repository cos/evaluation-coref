package LBJ2.nlp.coref;

import java.util.*;
import java.io.*;
import LBJ2.parse.LineByLine;


/**
  * Reads the <code>.pred.apf.xml</code> files that have the same base
  * filename as the usual <code>.apf.xml</code> files from the specified root
  * folder.  The same input files that {@link ACE2004DocumentParser} take as
  * input are accepted by this parser.  <code>.pred.apf.xml</code> files have
  * the same format as <code>.apf.xml</code> files, so this class simply
  * serves to redirect the input from the files named in the input file to the
  * corresponding <code>.pred.apf.xml</code> files.
 **/
public class PrePredictedDocumentParser extends LineByLine
{
  /**
    * Possible subdirectories of the prefix in which a prediction file may
    * reside.
   **/
  protected static String[] subdirectories = { "train", "dev", "test" };


  /** This parser reads prediction labeled documents. */
  protected ACE2004DocumentParser parser;


  /**
    * Initializing constructor.
    *
    * @param root The root direcotry in which to look for prediction files.
    * @param file The name of a file containing <code>.apf.xml</code> file
    *             names, one per line.
   **/
  public PrePredictedDocumentParser(String root, String file)
  {
    super(file);
    LinkedList<String> files = new LinkedList<String>();

    for (String name = readLine(); name != null; name = readLine())
      if (!name.equals(""))
      {
        int lastSlash = name.lastIndexOf(File.separatorChar);
        name = name.substring(lastSlash + 1);
        name = name.substring(0, name.length() - 8) + ".pred.apf.xml";
        File f = new File(root + File.separator + subdirectories[0] + File.separator + name);
        int i = 1;
        for (; i < subdirectories.length && !f.exists(); ++i)
          f = new File(root + File.separator + subdirectories[i] + File.separator + name);
        --i;

        if (!f.exists())
        {
          System.out.println("Can't find " + name);
          System.exit(1);
        }

        files.add(f.getAbsolutePath());
      }

    parser = new ACE2004DocumentParser(files.toArray(new String[0]), "");
  }


  /** The next pre-predicted document. */
  public Object next() { return parser.next(); }


  /** Starts parsing over again. */
  public void reset() { parser.reset(); }
}

