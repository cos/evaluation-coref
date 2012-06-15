package LBJ2;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import LBJ2.IR.*;
import LBJ2.io.*;


/**
  * To be run after <code>SemanticAnalysis</code>, this pass determines which
  * <code>CodeGenerator</code>s need to have their code generated and which
  * classifiers need to be trained based on the revisions made to the LBJ
  * source file.
  *
  * <p> A hard coded classifier, a constraint, or an inference named
  * <code>foo</code> needs its code regenerated iff at least one of the
  * following is true:
  * <ul>
  *   <li> The file <code>foo.java</code> does not exist.
  *   <li>
  *     Using the comments at the top of <code>foo.java</code>, it is
  *     determined that the code specifying <code>foo</code> has been revised.
  * </ul>
  * If the comments at the top of <code>foo.java</code> do not exist, or if
  * they don't have the expected form, the file will not be overwritten and an
  * error will be generated.
  *
  * <p> All <code>CodeGenerator</code>s are also labeled as either "affected"
  * (by a revision) or "unaffected".  An <code>CodeGenerator</code> named
  * <code>foo</code> is labeled "affected" iff at least one of the following
  * is true:
  * <ul>
  *   <li>
  *     <code>foo</code> is a hard coded classifier, a constraint, or an
  *     inference and either:
  *     <ul>
  *       <li> its code needed to be regenerated as described above or
  *       <li> it invokes another "affected" <code>CodeGenerator</code>.
  *     </ul>
  *   <li>
  *     <code>foo</code> is a learning classifier and at least one of its
  *     label or extractor classifiers is "affected".
  * </ul>
  *
  * <p> A learning classifier named <code>foo</code> needs to have its code
  * regenerated and retrained iff at least one of the following is true:
  * <ul>
  *   <li> The file <code>foo.java</code> does not exist.
  *   <li>
  *     Using the comments at the top of <code>foo.java</code>, it is
  *     determined that the code specifying <code>foo</code> has been revised.
  *   <li> At least one of its label or extractor classifiers is "affected".
  * </ul>
  *
  * @see    LBJ2.SemanticAnalysis
  * @author Nick Rizzolo
 **/
public class RevisionAnalysis extends Pass
{
  /** Constant representing the "unaffected" revision status. */
  public static final Integer UNAFFECTED = new Integer(0);
  /** Constant representing the "affected" revision status. */
  public static final Integer AFFECTED = new Integer(1);
  /** Constant representing the "revised" revision status. */
  public static final Integer REVISED = new Integer(2);


  /**
    * Keeps track of the names of classifiers whose revision status has been
    * resolved.
   **/
  public static HashMap revisionStatus;
  /**
    * Set to <code>true</code> iff no code has changed since the compiler was
    * last run.
   **/
  public static boolean noChanges;


  /**
    * Instantiates a pass that runs on an entire <code>AST</code>.
    *
    * @param ast  The program to run this pass on.
   **/
  public RevisionAnalysis(AST ast)
  {
    super(ast);
    revisionStatus = new HashMap();
  }


  /**
    * This method reads the comments at the top of the file containing the
    * code corresponding to the specified code generating node to determine if
    * the LBJ source describing that code generator has been modified since
    * the LBJ2 compiler was last executed.
    *
    * @param node     The code generating node.
    * @param convert  Whether or not the code is converted to hexadecimal
    *                 compressed format.
    * @return         <code>true</code> iff the associated Java file did not
    *                 exist or it contained the expected comments and those
    *                 comments indicate that a revision has taken place.
   **/
  private boolean codeRevision(CodeGenerator node, boolean convert)
  {
    String name = node.getName();

    if (node instanceof LearningClassifierExpression)
    {
      String lcFilePath = name + ".lc";
      if (Main.classDirectory != null)
        lcFilePath = Main.classDirectory + File.separator + lcFilePath;

      File lcFile = new File(lcFilePath);
      if (!lcFile.exists()) return true;
    }

    name += ".java";
    if (Main.generatedSourceDirectory != null)
      name = Main.generatedSourceDirectory + File.separator + name;

    File javaSource = new File(name);
    if (!javaSource.exists()) return true;

    BufferedReader in = null;
    try { in = new BufferedReader(new FileReader(javaSource)); }
    catch (Exception e)
    {
      System.err.println("Can't open '" + name + "' for input: " + e);
      System.exit(1);
    }

    String line1 = "";
    String line2 = "";
    try
    {
      line1 = in.readLine();
      line2 = in.readLine();
    }
    catch (Exception e)
    {
      System.err.println("Can't read from '" + name + "': " + e);
      System.exit(1);
    }

    try { in.close(); }
    catch (Exception e)
    {
      System.err.println("Can't close file '" + name + "': " + e);
      System.exit(1);
    }

    if (line1 == null || line2 == null || !line2.startsWith("// ")
        || !TranslateToJava.disclaimer.equals(line1))
    {
      reportError(node.getLine(),
          "The file '" + name + "' does not appear to have been generated by "
          + "LBJ2, but LBJ2 needs to overwrite it.  Either remove the file, "
          + "or change the name of the classifier in '" + Main.sourceFilename
          + "'.");
      return false;
    }

    line2 = line2.substring(3);
    String expected = null;

    if (convert)
    {
      StringBuffer buffer = node.shallow();

      PrintStream converter = null;
      ByteArrayOutputStream converted = new ByteArrayOutputStream();
      try
      {
        converter = new PrintStream(
                      new GZIPOutputStream(
                        new HexOutputStream(converted)));
      }
      catch (Exception e)
      {
        System.err.println("Could not create converter stream.");
        System.exit(1);
      }

      converter.print(buffer.toString());
      converter.close();

      expected = converted.toString();
    }
    else expected = node.shallow().toString();

    return !line2.equals(expected);
  }


  /**
    * Recursively propagates the information about which nodes are "affected".
    *
    * @param name The name of an affected node.
   **/
  private void propagateAffected(String name)
  {
    boolean isCompositeGenerator =
      SemanticAnalysis.representationTable.get(name)
      instanceof CompositeGenerator;
    boolean isRevised = revisionStatus.get(name) == REVISED;
    HashSet dependors = (HashSet) SemanticAnalysis.dependorGraph.get(name);

    assert dependors != null : "null entry in dependorGraph for " + name;

    for (Iterator I = dependors.iterator(); I.hasNext(); )
    {
      Object dependor = I.next();

      if (!revisionStatus.containsKey(dependor))
      {
        if (isCompositeGenerator && isRevised
            && SemanticAnalysis.representationTable.get(dependor)
               instanceof LearningClassifierExpression)
          revisionStatus.put(dependor, REVISED);
        else revisionStatus.put(dependor, AFFECTED);
        propagateAffected((String) dependor);
      }
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param list The node to process.
   **/
  public void run(DeclarationList list)
  {
    noChanges = true;
    if (list.size() == 0) return;

    runOnChildren(list);

    noChanges = revisionStatus.size() == 0;
    String[] revised =
      (String[]) revisionStatus.keySet().toArray(new String[0]);
    for (int i = 0; i < revised.length; ++i) propagateAffected(revised[i]);

    for (Iterator I = SemanticAnalysis.dependorGraph.keySet().iterator();
         I.hasNext(); )
    {
      Object name = I.next();
      if (!revisionStatus.containsKey(name))
        revisionStatus.put(name, UNAFFECTED);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cn The node to process.
   **/
  public void run(ClassifierName cn)
  {
    if (cn.referent == cn.name) return;
    if (codeRevision(cn, false))
      revisionStatus.put(cn.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cc The node to process.
   **/
  public void run(CodedClassifier cc)
  {
    if (codeRevision(cc, true))
      revisionStatus.put(cc.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cg The node to process.
   **/
  public void run(CompositeGenerator cg)
  {
    runOnChildren(cg);
    if (codeRevision(cg, true))
      revisionStatus.put(cg.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(Conjunction c)
  {
    runOnChildren(c);
    if (codeRevision(c, false))
      revisionStatus.put(c.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param i  The node to process.
   **/
  public void run(InferenceInvocation i)
  {
    if (codeRevision(i, false))
      revisionStatus.put(i.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param lce  The node to process.
   **/
  public void run(LearningClassifierExpression lce)
  {
    runOnChildren(lce);
    if (codeRevision(lce, true))
      revisionStatus.put(lce.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cd The node to process.
   **/
  public void run(ConstraintDeclaration cd)
  {
    if (codeRevision(cd, true))
      revisionStatus.put(cd.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param id The node to process.
   **/
  public void run(InferenceDeclaration id)
  {
    if (codeRevision(id, true))
      revisionStatus.put(id.name.toString(), REVISED);
    run(id.constraint);
  }
}

