package LBJ2;


/**
  * This class holds configuration parameters determined by the
  * <code>configure</code> script.
  *
  * <p> <b>LBJ2/Configuration.java.  Generated from Configuration.java.in by configure.</b>
  *
  * @author Nick Rizzolo
 **/
public class Configuration
{
  /** The name of the Java compiler. */
  public static String javac = "javac";
  /** The name of the JVM executable. */
  public static String java = "java";
  /** Contains the version number of this software. */
  public static String packageVersion = "2.1.5";
  /** Whether GLPK is supported. */
  public static boolean GLPKLinked = "no" == "yes";
  /** Whether Xpress-MP is supported. */
  public static boolean XpressMPLinked = "no" == "yes";
  /** Whether WEKA is supported. */
  public static boolean WekaLinked = "no" == "yes";
  /** LBJ's web site. */
  public static String webSite = "http://l2r.cs.uiuc.edu/~cogcomp";
}

