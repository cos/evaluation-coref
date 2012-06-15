package LBJ2;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.*;
import LBJ2.IR.*;
import LBJ2.io.*;
import LBJ2.infer.*;


/**
  * This pass generates Java code from an AST, but does not perform any
  * training.
  *
  * @author Nick Rizzolo
 **/
public class TranslateToJava extends Pass
{
  /** The commented message appearing at the top of all generated files. */
  public static final String disclaimer =
    "// Modifying this comment will cause the next execution of LBJ2 to "
    + "overwrite this file.";
  /**
    * This array contains string descriptions of methods that don't need to be
    * overridden when generating code for a learner.
   **/
  private static final String[] noOverride =
  {
    "native public int hashCode()",
    "public LBJ2.classify.FeatureVector classify(java.lang.Object a0)",
    "public LBJ2.classify.FeatureVector[] classify(java.lang.Object[] a0)",
    "public boolean equals(java.lang.Object a0)",
    "public double realValue(java.lang.Object a0)",
    "public double[] realValueArray(java.lang.Object a0)",
    "public java.lang.Object clone()",
    "public java.lang.String discreteValue(java.lang.Object a0)",
    "public java.lang.String getInputType()",
    "public java.lang.String getOutputType()",
    "public java.lang.String toString()",
    "public java.lang.String[] allowableValues()",
    "public java.lang.String[] discreteValueArray(java.lang.Object a0)",
    "public java.util.LinkedList getCompositeChildren()",
    "public short valueIndexOf(java.lang.String a0)",
    "public void binaryWrite(java.lang.String a0)",
    "public void binaryWrite(java.lang.String a0, java.lang.String a1)",
    "public void learn(java.lang.Object a0)",
    "public void learn(java.lang.Object[] a0)",
    "public void save()"
  };
  /**
    * The prefix of the name of the temporary variable in which a constraint's
    * computed value should be stored.  This variable is only used when
    * {@link #constraintMode} is unset.
   **/
  private static final String constraintResult = "LBJ2$constraint$result$";


  /** Used for collecting the string representation of a method body. */
  private StringBuffer methodBody;
  /** The indent level when collecting the method body. */
  private int indent;
  /** Lets AST children know about the node they are contained in. */
  private CodeGenerator currentCG;
  /**
    * Lets {@link VariableDeclaration}s know if they are contained in the
    * initialization portion of the header of a <code>for</code> loop.
   **/
  private boolean forInit;
  /**
    * Filenames that have been generated during the processing of one
    * statement.
   **/
  private HashSet files;
  /**
    * When this flag is set, code generated for constraint expressions will
    * create {@link LBJ2.infer.Constraint} objects rather than computing the
    * value of the constraint expression.
   **/
  private boolean constraintMode;
  /**
    * This variable is appended to the {@link #constraintResult} variable to
    * form the name of a new temporary variable.
   **/
  private int constraintResultNumber;
  /** The current constraint result variable name. */
  private String constraintResultName;
  /**
    * Lets AST children know the index that a given quantification variable
    * occupies in an {@link EqualityArgumentReplacer}'s vector; the keys of
    * the map are names of quantification variables, and the values are
    * <code>Integer</code>s.
   **/
  private HashMap quantificationVariables;
  /**
    * Lets AST children know the index that a given context variable occupies
    * in an {@link EqualityArgumentReplacer}'s vector; the keys of the map are
    * names of context variables, and the values are <code>Integer</code>s.
   **/
  private HashMap contextVariables;
  /**
    * Lets AST nodes know how deeply nested inside
    * {@link QuantifiedConstraintExpression}s they are.
   **/
  private int quantifierNesting;


  /**
    * Associates an AST with this pass.
    *
    * @param ast  The AST to associate with this pass.
   **/
  public TranslateToJava(AST ast)
  {
    super(ast);
    methodBody = new StringBuffer();
    files = new HashSet();
  }


  /**
    * Uses the current value of {@link #indent} to append the appropriate
    * number of spaces to {@link #methodBody}.
   **/
  private void appendIndent()
  {
    for (int i = 0; i < indent; ++i) methodBody.append("  ");
  }


  /**
    * Sets the indentation level.
    *
    * @param i  The new indentation level.
   **/
  public void setIndent(int i) { indent = i; }


  /**
    * Gives access to the {@link #methodBody} member variable so that this
    * pass can be invoked selectively on some subset of a method body.
    *
    * @return The contents of {@link #methodBody} in a <code>String</code>.
   **/
  public String getMethodBody() { return methodBody.toString(); }


  /**
    * Create a <code>PrintStream</code> that writes to a Java file
    * corresponding to the specified {@link CodeGenerator}.
    *
    * @param node The code producing node.
    * @return     The stream, or <code>null</code> if it couldn't be created.
   **/
  public static PrintStream open(CodeGenerator node)
  {
    return open(node.getName() + ".java");
  }


  /**
    * Create a <code>PrintStream</code> that writes to the specified file.
    *
    * @param name The name of the file to open.
    * @return     The stream, or <code>null</code> if it couldn't be created.
   **/
  public static PrintStream open(String name)
  {
    if (Main.generatedSourceDirectory != null)
    {
      name = Main.generatedSourceDirectory + File.separator + name;

      String[] directories = name.split("\\" + File.separator + "+");
      File directory = new File(directories[0]);

      for (int i = 1; i < directories.length - 1; ++i)
      {
        directory = new File(directory + File.separator + directories[i]);

        if (!directory.exists() && !directory.mkdir())
        {
          System.err.println("Can't create directory '" + directory + "'.");
          return null;
        }
      }
    }
    else if (Main.sourceDirectory != null)
      name = Main.sourceDirectory + File.separator + name;

    Main.fileNames.add(name);

    PrintStream out = null;

    try { out = new PrintStream(new FileOutputStream(name)); }
    catch (Exception e)
    {
      System.err.println("Can't open '" + name + "' for output: " + e);
    }

    return out;
  }


  /**
    * Generate the code that overrides certain methods of
    * {@link LBJ2.learn.Learner} to check types and call themselves on the
    * unique instance; also declares other methods and fields of the
    * classifier's implementation.  The explicitly methods overridden are:
    * <ul>
    *   <li> <code>getInputType()</code> </li>
    *   <li> <code>getOutputType()</code> </li>
    *   <li> <code>allowableValues()</code> </li>
    *   <li> <code>learn(Object)</code> </li>
    *   <li> <code>learn(Object[])</code> </li>
    *   <li> <code>classify(Object)</code> </li>
    *   <li> <code>classify(Object[])</code> </li>
    * </ul>
    *
    * In addition, any methods defined by any subclass of
    * {@link LBJ2.learn.Learner} down to the super class of this learner are
    * overridden to call the super class's implementation on the unique
    * instance.
    *
    * @param out    The stream to write to.
    * @param lce    The {@link LearningClassifierExpression} representing the
    *               learner.
    * @param load   Set to <code>true</code> if the generated classifier
    *               should load its representation from disk either on-demand
    *               or in its no-argument constructor if the classifier caches
    *               its values or not, respectively.
   **/
  public static void generateLearnerBody(PrintStream out,
                                         LearningClassifierExpression lce,
                                         boolean load)
  {
    String lceName = lce.name.toString();
    String field = null;
    boolean cachedInMap = false;

    if (lce.cacheIn != null)
    {
      field = lce.cacheIn.toString();
      cachedInMap = field.equals(ClassifierAssignment.mapCache);
      if (cachedInMap)
        out.println("  private static final WeakHashMap __valueCache "
                    + "= new WeakHashMap();\n");
    }

    HashSet invoked = (HashSet) SemanticAnalysis.invokedGraph.get(lceName);

    if (invoked != null && invoked.size() > 0)
    {
      for (Iterator I = invoked.iterator(); I.hasNext(); )
      {
        String name = (String) I.next();
        String nameNoDots = name.replace('.', '$');
        out.println("  private static final " + name + " __" + nameNoDots
                    + " = new " + name + "();");
      }

      out.println();
    }

    out.println("  public static Parser getParser() { return " + lce.parser
                + "; }\n");

    out.println("  public static TestingMetric getTestingMetric() { return "
                + lce.testingMetric + "; }\n");

    out.println("  private static ThreadLocal cache = new ThreadLocal(){ };");
    out.println("  private static ThreadLocal exampleCache = "
                + "new ThreadLocal(){ };");

    if (!lce.attributeString.equals("") && lce.learnerParameterBlock == null)
      out.println("  private static final String attributeString = \""
                  + lce.attributeString + "\";");

    out.println("\n  private boolean isClone;\n");

    out.println("  public " + lceName + "()");
    out.println("  {");
    String fqName = AST.globalSymbolTable.getPackage();
    if (fqName.length() > 0) fqName += ".";
    fqName += lceName;

    out.println("    super(\"" + fqName + "\");");
    out.println("    isClone = true;");

    if (load && field == null)
    {
      out.println("    if (instance == null)");
      out.println("      instance = (" + lceName + ") "
                  + "Classifier.binaryRead(lcFilePath, \"" + lceName
                  + "\");");
    }

    out.println("  }\n");

    if (field != null)
    {
      out.println("  private void loadInstance()");
      out.println("  {");

      if (load)
      {
        out.println("    if (instance == null)");
        out.println("      instance = (" + lceName + ") "
                    + "Classifier.binaryRead(lcFilePath, \"" + lceName
                    + "\");");
      }

      out.println("  }\n");
    }

    out.println("  private " + lceName + "(boolean b)");
    out.println("  {");
    out.print("    super(");

    if (lce.learnerParameterBlock == null)
    {
      if (lce.learnerConstructor.arguments.size() > 0)
      {
        out.print(lce.learnerConstructor.arguments);

        if (!lce.attributeString.equals(""))
        {
          out.print(", ");
        }
      }

      if (!lce.attributeString.equals("")) out.print("attributeString");
    }
    else out.print("new Parameters()");

    out.println(");");
    out.println("    containingPackage = \""
                + AST.globalSymbolTable.getPackage() + "\";");
    out.println("    name = \"" + lceName + "\";");
    if (lce.labeler != null)
      out.println("    setLabeler(new " + lce.labeler.name + "());");
    out.println("    setExtractor(new " + lce.extractor.name + "());");
    out.println("    isClone = false;");
    out.println("  }\n");

    LBJ2.IR.Type input = lce.argument.getType();
    int line = lce.line + 1;

    typeReturningMethods(out, input, lce.returnType);

    out.println("\n  public void learn(Object example)");
    out.println("  {");
    out.println("    if (isClone)");
    out.println("    {");

    if (load && field != null) out.println("      loadInstance();");

    out.println("      instance.learn(example);");
    out.println("      return;");
    out.println("    }\n");

    out.println("    Classifier saveExtractor = extractor;");
    out.println("    Classifier saveLabeler = labeler;\n");

    out.println("    if (!(example instanceof " + input + "))");
    out.println("    {");
    out.println("      if (example instanceof FeatureVector)");
    out.println("      {");
    out.println("        if (!(extractor instanceof FeatureVectorReturner))");
    out.println("          setExtractor(new FeatureVectorReturner());");
    out.println("        if (!(labeler instanceof LabelVectorReturner))");
    out.println("          setLabeler(new LabelVectorReturner());");
    out.println("      }");
    out.println("      else");
    out.println("      {");
    out.println("        String type = example == null ? \"null\" : "
                + "example.getClass().getName();");
    out.println("        System.err.println(\"Classifier '" + lceName + "("
                + input + ")' defined on line " + line + " of "
                + Main.sourceFilename
                + " received '\" + type + \"' as input.\");");
    out.println("        new Exception().printStackTrace();");
    out.println("        System.exit(1);");
    out.println("      }");
    out.println("    }\n");

    out.println("    super.learn(example);\n");

    out.println("    if (saveExtractor != extractor) "
                + "setExtractor(saveExtractor);");
    out.println("    if (saveLabeler != labeler) setLabeler(saveLabeler);");
    out.println("  }\n");

    out.println("  public void learn(Object[] examples)");
    out.println("  {");
    out.println("    if (isClone)");
    out.println("    {");

    if (load && field != null) out.println("      loadInstance();");

    out.println("      instance.learn(examples);");
    out.println("      return;");
    out.println("    }\n");

    out.println("    Classifier saveExtractor = extractor;");
    out.println("    Classifier saveLabeler = labeler;\n");

    out.println("    if (!(examples instanceof " + input + "[]))");
    out.println("    {");
    out.println("      if (examples instanceof FeatureVector[])");
    out.println("      {");
    out.println("        if (!(extractor instanceof FeatureVectorReturner))");
    out.println("          setExtractor(new FeatureVectorReturner());");
    out.println("        if (!(labeler instanceof LabelVectorReturner))");
    out.println("          setLabeler(new LabelVectorReturner());");
    out.println("      }");
    out.println("      else");
    out.println("      {");
    out.println("        String type = examples == null ? \"null\" : "
                + "examples.getClass().getName();");
    out.println("        System.err.println(\"Classifier '" + lceName + "("
                + input + ")' defined on line " + line + " of "
                + Main.sourceFilename
                + " received '\" + type + \"' as input.\");");
    out.println("        new Exception().printStackTrace();");
    out.println("        System.exit(1);");
    out.println("      }");
    out.println("    }\n");

    out.println("    super.learn(examples);\n");

    out.println("    if (saveExtractor != extractor) "
                + "setExtractor(saveExtractor);");
    out.println("    if (saveLabeler != labeler) setLabeler(saveLabeler);");
    out.println("  }\n");

    out.println("  public FeatureVector classify(Object __example)");
    out.println("  {");

    if (field == null)
      out.println("    if (isClone) return instance.classify(__example);\n");

    out.println("    Classifier __saveExtractor = extractor;\n");

    out.print("    if (!(");
    if (field != null) out.print("isClone || ");
    out.println("__example instanceof " + input + "))");
    out.println("    {");
    out.println("      if (__example instanceof FeatureVector)");
    out.println("      {");
    out.println("        if (!(extractor instanceof FeatureVectorReturner))");
    out.println("          setExtractor(new FeatureVectorReturner());");
    out.println("      }");
    out.println("      else");
    out.println("      {");
    out.println("        String type = __example == null ? \"null\" : "
                + "__example.getClass().getName();");
    out.println("        System.err.println(\"Classifier '" + lceName + "("
                + input + ")' defined on line " + line + " of "
                + Main.sourceFilename
                + " received '\" + type + \"' as input.\");");
    out.println("        new Exception().printStackTrace();");
    out.println("        System.exit(1);");
    out.println("      }");
    out.println("    }\n");

    out.println("__classify:");
    out.println("    {");
    out.println("      if (__example == " + lceName
                + ".exampleCache.get()) break __classify;");
    out.println("      " + lceName + ".exampleCache.set(__example);\n");

    boolean discrete = false;
    boolean array = false;
    String returnSubType = null;
    String returnType = null;
    String methodName = null;

    if (lce.evaluation == null)
    {
      if (field != null)
      {
        discrete = lce.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
                   || lce.returnType.type == ClassifierReturnType.DISCRETE;
        array = lce.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
                || lce.returnType.type == ClassifierReturnType.REAL_ARRAY;

        String indent = "      ";

        if (!cachedInMap)
        {
          out.println(indent + lce.argument + " = null;");
          out.println(indent + "if (__example instanceof " + input + ")");
          out.println(indent + "{");
          out.println(indent + "  " + lce.argument.getName() + " = (" + input
                      + ") __example;");
          indent += "  ";
        }

        if (array)
        {
          out.print(indent);
          if (discrete) out.print("String");
          else out.print("double");
          out.print("[] __cachedValue = ");

          if (cachedInMap)
          {
            out.print("(");
            if (discrete) out.print("String");
            else out.print("double");
            out.println("[]) " + lceName + ".__valueCache.get(__example);");
          }
          else out.println(field + ";");

          out.println("\n" + indent + "if (__cachedValue != null)");
          out.println(indent + "{");
          out.println(indent + "  " + lceName
                      + ".cache.set(new FeatureVector());");
          out.println(indent + "  for (int __i = 0; __i < "
                      + "__cachedValue.__length; ++__i)");
          out.print(indent + "    ((FeatureVector) " + lceName
                    + ".cache.get()).addFeature(new ");

          if (discrete) out.print("Discrete");
          else out.print("Real");

          out.print(
              "ArrayFeature(containingPackage, name, __cachedValue[__i], ");

          if (discrete)
            out.print("valueIndexOf(__cachedValue[__i]), "
                      + "(short) allowableValues().length, ");

          out.println("__i, __cachedValue.__length));");

          out.println(indent + "  break __classify;");
          out.println(indent + "}");
        }
        else
        {
          if (cachedInMap && !discrete)
          {
            out.print(indent + "Double __dValue = (Double) " + lceName
                      + ".__valueCache.get(__example);");
          }

          out.print(indent);
          if (discrete) out.print("String");
          else out.print("double");
          out.print(" __cachedValue = ");

          if (cachedInMap)
          {
            if (discrete)
              out.println("(String) " + lceName
                          + ".__valueCache.get(__example);");
            else
              out.println("__dValue == null ? Double.NaN : "
                          + "__dValue.doubleValue();");
          }
          else out.println(field + ";");

          out.print(indent + "if (");
          if (!discrete) out.print("Double.doubleToLongBits(");
          out.print("__cachedValue");
          if (!discrete) out.print(")");
          out.print(" != ");
          if (!discrete) out.print("Double.doubleToLongBits(Double.NaN)");
          else out.print("null");
          out.println(")");
          out.println(indent + "{");
          out.println(indent + "  " + lceName
                      + ".cache.set(new FeatureVector());");
          out.print(indent + "  ((FeatureVector) " + lceName
                    + ".cache.get()).addFeature(new ");
          if (discrete) out.print("Discrete");
          else out.print("Real");
          out.print(
              "Feature(this.containingPackage, this.name, __cachedValue");
          if (discrete)
            out.print(", valueIndexOf(__cachedValue), "
                      + "(short) allowableValues().length");
          out.println("));");

          out.println(indent + "  break __classify;");
          out.println(indent + "}");
        }

        if (cachedInMap) out.println();
        else out.println("      }\n");

        out.println("      if (isClone)");
        out.println("      {");
        if (load) out.println("        loadInstance();");
        out.println("        " + lceName + ".exampleCache.set(null);");
        out.println("        instance.classify(__example);");
        out.println("        break __classify;");
        out.println("      }\n");
      }

      out.println("      " + lceName
                  + ".cache.set(super.classify(__example));");

      if (lce.checkDiscreteValues)
      {
        out.println("\n      for (java.util.Iterator __I = ((FeatureVector) "
                    + lceName + ".cache.get()).iterator(); __I.hasNext(); )");
        out.println("      {");
        out.println("        DiscreteFeature __df = (DiscreteFeature) "
                    + "__I.next();");
        out.println("        if (__df.getValueIndex() == -1)");
        out.println("        {");
        out.println("          System.err.println(\"Classifier " + lceName
                    + " defined on line " + line + " of "
                    + Main.sourceFilename + " tried to produce a feature "
                    + "with value '\" + __df.getValue() + \"' which is not "
                    + "allowable.\");");
        out.println("          System.exit(1);");
        out.println("        }");
        out.println("      }\n");
      }

      if (field != null)
      {
        if (cachedInMap)
        {
          out.print("      " + lceName + ".__valueCache.put(__example, ");
          if (!array && !discrete) out.print("new Double(");
        }
        else
        {
          out.println("      if (__example instanceof " + input + ")");
          out.print("        " + field + " = ");
        }

        if (array)
        {
          out.print("((FeatureVector) " + lceName + ".cache.get()).");
          if (discrete) out.print("discrete");
          else out.print("real");
          out.print("ValueArray()");
        }
        else
        {
          out.print("((");
          if (discrete) out.print("Discrete");
          else out.print("Real");
          out.print("Feature) ((FeatureVector) " + lceName
                    + ".cache.get()).firstFeature()).getValue()");
        }

        if (cachedInMap)
        {
          if (!array && !discrete) out.print(")");
          out.print(")");
        }

        out.println(";");
      }
    }
    else
    {
      discrete = lce.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
                 || lce.returnType.type == ClassifierReturnType.DISCRETE;
      array = lce.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
              || lce.returnType.type == ClassifierReturnType.REAL_ARRAY;

      returnSubType = discrete ? "String" : "double";
      returnType = returnSubType + (array ? "[]" : "");
      methodName = (discrete ? "discrete" : "real") + "Value"
                   + (array ? "Array" : "");

      out.println("      " + returnType + " __v = " + methodName
                  + "(__example);");
      out.println("      " + lceName + ".cache.set(new FeatureVector());");

      if (array)
      {
        out.println("      for (int __i = 0; __i < __v.length; ++__i)");
        out.print("        ((FeatureVector) " + lceName
                  + ".cache.get()).addFeature(new ");

        if (discrete) out.print("Discrete");
        else out.print("Real");

        out.print("ArrayFeature(containingPackage, name, __v[__i], ");

        if (discrete)
          out.print("valueIndexOf(__v[__i]), "
                    + "(short) allowableValues().length, ");

        out.println("__i, __v.length));");
      }
      else
      {
        out.print("      ((FeatureVector) " + lceName
                  + ".cache.get()).addFeature(new ");

        if (discrete) out.print("Discrete");
        else out.print("Real");

        out.print("ArrayFeature(containingPackage, name, __v");

        if (discrete)
          out.print(", valueIndexOf(__v), (short) allowableValues().length");

        out.println("));");
      }
    }

    out.println("    }\n");

    out.println("    if (__saveExtractor != this.extractor) "
                + "setExtractor(__saveExtractor);");
    out.println("    return (FeatureVector) " + lceName + ".cache.get();");
    out.println("  }\n");

    out.println("  public FeatureVector[] classify(Object[] examples)");
    out.println("  {");
    out.println("    if (isClone)");

    if (load && field != null)
    {
      out.println("    {");
      out.println("      loadInstance();");
    }

    out.println("      return instance.classify(examples);");

    if (load && field != null) out.println("    }\n");
    else out.println();

    out.println("    Classifier saveExtractor = extractor;\n");

    out.println("    if (!(examples instanceof " + input + "[]))");
    out.println("    {");
    out.println("      if (examples instanceof FeatureVector[])");
    out.println("      {");
    out.println("        if (!(extractor instanceof FeatureVectorReturner))");
    out.println("          setExtractor(new FeatureVectorReturner());");
    out.println("      }");
    out.println("      else");
    out.println("      {");
    out.println("        String type = examples == null ? \"null\" : "
                + "examples.getClass().getName();");
    out.println("        System.err.println(\"Classifier '" + lceName + "("
                + input + ")' defined on line " + line + " of "
                + Main.sourceFilename
                + " received '\" + type + \"' as input.\");");
    out.println("        new Exception().printStackTrace();");
    out.println("        System.exit(1);");
    out.println("      }");
    out.println("    }\n");

    out.println("    FeatureVector[] result = super.classify(examples);");

    if (lce.checkDiscreteValues)
    {
      out.println("    for (int i = 0; i < result.length; ++i)");
      out.println("      for (java.util.Iterator I = result[i].iterator(); "
                  + "I.hasNext(); )");
      out.println("      {");
      out.println("        DiscreteFeature df = (DiscreteFeature) I.next();");
      out.println("        if (df.getValueIndex() == -1)");
      out.println("        {");
      out.println("          System.err.println(\"Classifier " + lceName
                  + " defined on line " + line + " of " + Main.sourceFilename
                  + " tried to produce a feature with value '\" + "
                  + "df.getValue() + \"' which is not allowable.\");");
      out.println("          System.exit(1);");
      out.println("        }");
      out.println("      }\n");
    }

    out.println("    if (saveExtractor != extractor) "
                + "setExtractor(saveExtractor);");
    out.println("    return result;");
    out.println("  }\n");

    if (lce.evaluation == null)
    {
      generateValue(out, lce.name, lce.returnType, field, lce.argument);
      out.println();
    }
    else
    {
      TranslateToJava translator = new TranslateToJava(null);
      translator.setRoot(lce.evaluation);
      translator.run();

      out.println("  public " + returnType + " " + methodName
                  + "(Object __example)");
      out.println("  {");

      if (!cachedInMap)
        out.println("    " + lce.argument + " = (" + input + ") __example;");

      if (field != null)
      {
        String rCompute = "      if (isClone)\n";

        if (load)
        {
          rCompute += "      {\n";
          rCompute += "        loadInstance();\n";
        }

        rCompute += "        return instance." + methodName
                    + "(__example);\n";

        if (load) rCompute += "      }\n\n";

        rCompute += "      " + (cachedInMap ? "__cachedValue" : field) + " = "
                    + translator.getMethodBody() + ";\n";

        if (cachedInMap)
        {
          rCompute += "      " + lceName + ".__valueCache.put(__example, ";
          if (!array && !discrete) rCompute += "new Double(";
          rCompute += "__cachedValue";
          if (!array && !discrete) rCompute += ")";
          rCompute += ");";
        }

        returnCachedValue(out, field, array, discrete, lceName, "__example",
                          null, rCompute, "    ");
      }
      else out.println("    return " + translator.getMethodBody() + ";");

      out.println("  }\n");
    }

    generateHashingMethods(out, lceName);

    Class lceClass =
      AST.globalSymbolTable.classForName(lce.learnerName.toString());
    if (lceClass == null)
    {
      reportError(lce.line, "Could not locate class for learner '"
                            + lce.learnerName + "'.");
      return;
    }

    Method[] methods = lceClass.getMethods();
    for (int i = 0; i < methods.length; ++i)
    {
      int modifiers = methods[i].getModifiers();
      if (Modifier.isFinal(modifiers) || Modifier.isPrivate(modifiers)
          || Modifier.isProtected(modifiers) || Modifier.isStatic(modifiers))
        continue;

      Class returned = methods[i].getReturnType();
      String name = methods[i].getName();
      Class[] parameters = methods[i].getParameterTypes();

      String sig =
        signature(methods[i], modifiers, returned, name, parameters);
      if (Arrays.binarySearch(noOverride, sig) >= 0) continue;

      out.println("\n  " + sig);
      out.println("  {");
      out.println("    if (isClone)");

      if (load && field != null || returned.equals(void.class))
        out.println("    {");
      if (load && field != null) out.println("      loadInstance();");

      out.print("      ");
      if (!returned.equals(void.class)) out.print("return ");
      out.print("instance." + name + "(");

      if (parameters.length > 0)
      {
        out.print("a0");
        for (int j = 1; j < parameters.length; ++j) out.print(", a" + j);
      }

      out.println(");");

      if (returned.equals(void.class)) out.println("      return;");
      if (load && field != null || returned.equals(void.class))
        out.println("    }\n");

      out.print("    ");
      if (!returned.equals(void.class)) out.print("return ");
      out.print("super." + name + "(");

      if (parameters.length > 0)
      {
        out.print("a0");
        for (int j = 1; j < parameters.length; ++j) out.print(", a" + j);
      }

      out.println(");");
      out.println("  }");
    }

    if (lce.learnerParameterBlock != null)
    {
      TranslateToJava translator = new TranslateToJava(null);
      translator.setRoot(lce.learnerParameterBlock);
      translator.setIndent(3);
      translator.run();

      out.println("\n");
      out.println("  public static class Parameters extends "
                  + lce.learnerName + ".Parameters");
      out.println("  {");
      out.println("    public Parameters()");
      out.println(translator.getMethodBody());
      out.println("  }");
    }
  }


  /**
    * This method generates a string signature of the given method.  The
    * arguments other than <code>m</code> are supplied as arguments for
    * efficiency reasons, since this method is only called by one other
    * method.
    *
    * @see #generateLearnerBody(PrintStream,LearningClassifierExpression,boolean)
    * @param m          The method object.
    * @param modifiers  The integer representation of the method's modifiers.
    * @param returned   The return type of the method.
    * @param name       The name of the method.
    * @param parameters The parameter types of the method.
    * @return           A string description of the method suitable for
    *                   comparison with the elements of the
    *                   {@link #noOverride} array.
   **/
  public static String signature(Method m, int modifiers, Class returned,
                                 String name, Class[] parameters)
  {
    Class[] thrown = m.getExceptionTypes();

    String result = "";
    if (Modifier.isAbstract(modifiers)) result += "abstract ";
    if (Modifier.isFinal(modifiers)) result += "final ";
    if (Modifier.isNative(modifiers)) result += "native ";
    if (Modifier.isPrivate(modifiers)) result += "private ";
    if (Modifier.isProtected(modifiers)) result += "protected ";
    if (Modifier.isPublic(modifiers)) result += "public ";
    if (Modifier.isStatic(modifiers)) result += "static ";
    if (Modifier.isStrict(modifiers)) result += "strictfp ";
    if (Modifier.isSynchronized(modifiers)) result += "synchronized ";

    result += makeTypeReadable(returned.getName()) + " " + name + "(";
    if (parameters.length > 0)
    {
      result += makeTypeReadable(parameters[0].getName()) + " a0";
      for (int j = 1; j < parameters.length; ++j)
        result += ", " + makeTypeReadable(parameters[j].getName()) + " a" + j;
    }

    result += ")";
    if (thrown.length > 0)
    {
      result += " throws " + thrown[0].getName();
      for (int j = 1; j < thrown.length; ++j)
        result += ", " + thrown[j].getName();
    }

    return result;
  }


  /**
    * The value returned by the <code>Class.getName()</code> method is not
    * recognizable as a type by <code>javac</code> if the given class is an
    * array; this method produces a representation that is recognizable by
    * <code>javac</code>.
    *
    * @param name The name of a class as produced by
    *             <code>Class.getName()</code>.
    * @return     A string representation of the class recognizable by
    *             <code>javac</code>.
   **/
  public static String makeTypeReadable(String name)
  {
    if (name.charAt(0) != '[') return name;

    while (name.charAt(0) == '[') name = name.substring(1) + "[]";

    switch (name.charAt(0))
    {
      case 'B': return "boolean" + name.substring(1);
      case 'C': return "char" + name.substring(1);
      case 'D': return "double" + name.substring(1);
      case 'F': return "float" + name.substring(1);
      case 'I': return "int" + name.substring(1);
      case 'J': return "long" + name.substring(1);
      case 'L':
        int colon = name.indexOf(';');
        return name.substring(1, colon) + name.substring(colon + 1);
      case 'S': return "short" + name.substring(1);
      case 'Z': return "boolean" + name.substring(1);
    }

    assert false : "Unrecognized type string: " + name;
    return null;
  }


  /**
    * Generate code that overrides the methods of
    * {@link LBJ2.classify.Classifier} that return type information.  The
    * methods overridden are:
    * <ul>
    *   <li> <code>getInputType()</code> </li>
    *   <li> <code>getOutputType()</code> </li>
    *   <li> <code>allowableValues()</code> </li>
    * </ul>
    *
    * @param out    The stream to write to.
    * @param input  The input type of the classifier whose code this is.
    * @param output The return type of the classifier whose code this is.
   **/
  public static void typeReturningMethods(PrintStream out,
                                          LBJ2.IR.Type input,
                                          ClassifierReturnType output)
  {
    out.println("  public String getInputType() { return \""
                + input.typeClass().getName() + "\"; }");
    out.println("  public String getOutputType() { return \""
                + output.getTypeName() + "\"; }");

    if (output.values.size() > 0)
    {
      ConstantList values = output.values;
      out.println("\n  public String[] allowableValues()");
      out.println("  {");

      boolean isBoolean = false;
      if (output.values.size() == 2)
      {
        ASTNodeIterator I = values.iterator();
        String v1 = I.next().toString();
        String v2 = I.next().toString();
        if ((v1.equals("false") || v1.equals("\"false\""))
            && (v2.equals("true") || v2.equals("\"true\"")))
        {
          isBoolean = true;
          out.println("    return DiscreteFeature.BooleanValues;");
        }
      }

      if (!isBoolean)
      {
        ASTNodeIterator I = values.iterator();
        String v = I.next().toString();
        if (v.charAt(0) != '"') v = "\"" + v + "\"";
        out.print("    return new String[]{" + v);
        while (I.hasNext())
        {
          v = I.next().toString();
          if (v.charAt(0) != '"') v = "\"" + v + "\"";
          out.print(", " + v);
        }
        out.println("};");
      }

      out.println("  }");
    }
  }


  /**
    * Generates code that overrides the
    * {@link LBJ2.classify.Classifier#classify(Object[])} method so that it
    * checks the types of its arguments.
    *
    * @param out    The stream to write to.
    * @param name   The name of the classifier whose code this is.
    * @param input  The input type of the classifier whose code this is.
    * @param line   The line number on which this classifier is defined.
   **/
  public static void typeCheckClassifyArray(PrintStream out, String name,
                                            LBJ2.IR.Type input, int line)
  {
    out.println("  public FeatureVector[] classify(Object[] examples)");
    out.println("  {");
    out.println("    for (int i = 0; i < examples.length; ++i)");
    out.println("      if (!(examples[i] instanceof " + input + "))");
    out.println("      {");
    out.println("        System.err.println(\"Classifier '" + name + "("
                + input + ")' defined on line " + line + " of "
                + Main.sourceFilename + " received '\" + "
                + "examples[i].getClass().getName() + \"' as input.\");");
    out.println("        new Exception().printStackTrace();");
    out.println("        System.exit(1);");
    out.println("      }\n");

    out.println("    return super.classify(examples);");
    out.println("  }");
  }


  /**
    * Generates code that checks the appropriate cache (either a field access
    * in the case of <code>cachedin</code> or a hash map in the case of
    * <code>cached</code>) for a value, fills in that value as specified if
    * the cache didn't have it, and returns the value.
    *
    * @param out      The stream to write to.
    * @param field    Generated code representing a field access of the field
    *                 in which values are cached.  If this parameter is set to
    *                 {@link ClassifierAssignment#mapCache}, it means the
    *                 generated classifier will cache its value in a hash map.
    * @param array    <code>true</code> iff the classifier returns an array of
    *                 features.
    * @param discrete <code>true</code> iff the classifier is discrete.
    * @param name     The name of this classifier.
    * @param example  Generated code that access the example object passed as
    *                 a parameter to the method containing this generated
    *                 code.
    * @param compute  Generated code that computes the value to be returned
    *                 when it isn't found in the cache.  Use this parameter
    *                 when the result of the computation can simply be stored
    *                 in the cache, and set <code>rCompute</code> to
    *                 <code>null</code>.
    * @param rCompute Generated code that computes the value to be returned
    *                 when it isn't found in the cache.  Use this parameter to
    *                 specify an alternative computation, and set
    *                 <code>compute</code> to <code>null</code>.
    * @param indent   How far statements generated by this method should be
    *                 indented.
   **/
  public static void returnCachedValue(
      PrintStream out, String field, boolean array, boolean discrete,
      String name, String example, String compute, String rCompute,
      String indent)
  {
    boolean cachedInMap = field.equals(ClassifierAssignment.mapCache);
    if (cachedInMap && !array && !discrete)
      out.println(indent + "Double __dValue = (Double) " + name
                  + ".__valueCache.get(" + example + ");");

    out.print(indent);
    if (discrete) out.print("String");
    else out.print("double");
    if (array) out.print("[]");
    out.print(" __cachedValue = ");

    if (cachedInMap)
    {
      if (!array && !discrete)
        out.println("__dValue == null ? Double.NaN : "
                    + "__dValue.doubleValue();");
      else
      {
        out.print("(");
        if (discrete) out.print("String");
        else out.print("double");
        if (array) out.print("[]");
        out.println(") " + name + ".__valueCache.get(" + example + ");");
      }
    }
    else out.println(field + ";");

    out.print(indent + "if (");
    if (!array && !discrete) out.print("Double.doubleToLongBits(");
    out.print("__cachedValue");
    if (!array && !discrete) out.print(")");
    out.print(" == ");
    if (!array && !discrete) out.print("Double.doubleToLongBits(Double.NaN)");
    else out.print("null");
    out.println(")");

    if (compute != null)
    {
      if (cachedInMap) out.println(indent + "{");
      out.println(indent + "  " + (cachedInMap ? "__cachedValue" : field)
                  + " = " + compute + ";");

      if (cachedInMap)
      {
        out.print(indent + "  " + name + ".__valueCache.put(" + example
                  + ", ");
        if (!array && !discrete) out.print("new Double(");
        out.print("__cachedValue");
        if (!array && !discrete) out.print(")");
        out.println(");");
        out.println(indent + "}\n");
      }
    }
    else
    {
      out.println(indent + "{");
      out.println(rCompute);
      out.println(indent + "}\n");
    }

    out.println(indent + "return " + (cachedInMap ? "__cachedValue" : field)
                + ";");
  }


  /**
    * Generates the appropriate Value method which directly produces the
    * resulting feature value(s) when the return feature type is discrete,
    * real, or an array of one of those.  The generated implementation simply
    * calls {@link LBJ2.classify.Classifier#classify(Object)} and unwraps the
    * resulting {@link LBJ2.classify.FeatureVector}.
    *
    * @see LBJ2.classify.Classifier#discreteValue(Object)
    * @see LBJ2.classify.Classifier#realValue(Object)
    * @see LBJ2.classify.Classifier#discreteValueArray(Object)
    * @see LBJ2.classify.Classifier#realValueArray(Object)
    * @param out    The stream to write to.
    * @param name   The name of the classifier.
    * @param output The return type of the classifier whose code this is.
    * @param field  Generated code representing a field access of the field in
    *               which values are cached.  If this parameter is set to
    *               {@link ClassifierAssignment#mapCache}, it means the
    *               generated classifier will cache its value in a hash map.
    *               If set to <code>null</code>, it means its value will not
    *               be cached.
    * @param arg    The input argument of the classifier.
   **/
  public static void generateValue(PrintStream out, Name name,
                                   ClassifierReturnType output, String field,
                                   Argument arg)
  {
    if (output.type != ClassifierReturnType.DISCRETE
        && output.type != ClassifierReturnType.REAL
        && output.type != ClassifierReturnType.DISCRETE_ARRAY
        && output.type != ClassifierReturnType.REAL_ARRAY)
      return;

    boolean discrete =
      output.type == ClassifierReturnType.DISCRETE
      || output.type == ClassifierReturnType.DISCRETE_ARRAY;
    boolean array =
      output.type == ClassifierReturnType.DISCRETE_ARRAY
      || output.type == ClassifierReturnType.REAL_ARRAY;
    boolean cachedInMap = field != null
                          && field.equals(ClassifierAssignment.mapCache);

    String returnSubType = discrete ? "String" : "double";
    String returnType = returnSubType + (array ? "[]" : "");
    String methodName = (discrete ? "discrete" : "real") + "Value"
                        + (array ? "Array" : "");

    out.println("  public " + returnType + " " + methodName
                + "(Object __example)");
    out.println("  {");

    if (field != null)
    {
      if (!cachedInMap)
        out.println("    " + arg + " = (" + arg.getType() + ") __example;");

      String rCompute = "      classify(__example);";

      if (cachedInMap)
      {
        if (!array && !discrete)
          rCompute += "\n      __dValue = (Double) " + name
                      + ".__valueCache.get(__example);";
        rCompute += "\n      __cachedValue = ";

        if (!array && !discrete)
          rCompute += "__dValue == null ? Double.NaN : "
                      + "__dValue.doubleValue();";
        else
        {
          rCompute += "(";
          if (discrete) rCompute += "String";
          else rCompute += "double";
          if (array) rCompute += "[]";
          rCompute += ") " + name + ".__valueCache.get(__example);";
        }
      }

      returnCachedValue(out, field, array, discrete, name.toString(),
                        "__example", null, rCompute, "    ");

      out.println("  }");
      return;
    }

    String featureType = discrete ? "Discrete" : "Real";
    String defaultValue = discrete ? "\"\"" : "0";

    if (array)
      out.println("    return classify(__example)." + methodName + "();");
    else
    {
      out.println("    " + featureType + "Feature f = (" + featureType
                  + "Feature) classify(__example).firstFeature();");
      out.println("    return f == null ? " + defaultValue
                  + " : f.getValue();");
    }

    out.println("  }");
  }


  /**
    * Generates the <code>equals(Object)</code> method, which evaluates to
    * <code>true</code> whenever the two objects are of the same type.  This
    * method should not be called when generating code for a
    * {@link InferenceDeclaration}.
    *
    * @param out  The stream to write to.
    * @param name The name of the node whose <code>equals(Object)</code>
    *             method is being generated.
   **/
  public static void generateHashingMethods(PrintStream out, String name)
  {
    out.println("  public int hashCode() { return \"" + name
                + "\".hashCode(); }");
    out.println("  public boolean equals(Object o) { return o instanceof "
                + name + "; }");
  }


  /**
    * Generates the code appearing at the beginning of, for example, many
    * classifiers' {@link LBJ2.classify.Classifier#classify(Object)} methods
    * that checks to see if that input <code>Object</code> has the appropriate
    * type.
    *
    * @param out          The stream to write to.
    * @param name         The name of the {@link CodeGenerator} whose input is
    *                     being checked.
    * @param type         The type of {@link CodeGenerator} whose input is
    *                     being checked (capitalized).
    * @param input        The correct input type of the {@link CodeGenerator}.
    * @param line         The line number on which the {@link CodeGenerator}
    *                     appears.
    * @param exampleName  The name of the example variable.
    * @param indent       The level of indentation at which the code should be
    *                     printed.
   **/
  public static void generateTypeChecking(PrintStream out, String name,
                                          String type, String input, int line,
                                          String exampleName, int indent)
  {
    String i = "";
    for (int j = 0; j < indent; ++j) i += "  ";

    out.println(i + "if (!(" + exampleName + " instanceof " + input + "))");
    out.println(i + "{");
    out.println(i + "  String type = " + exampleName
                + " == null ? \"null\" : " + exampleName
                + ".getClass().getName();");
    out.println(i + "  System.err.println(\"" + type + " '" + name + "("
                + input + ")' defined on line " + line + " of "
                + Main.sourceFilename + " received '\" "
                + "+ type + \"' as input.\");");
    out.println(i + "  new Exception().printStackTrace();");
    out.println(i + "  System.exit(1);");
    out.println(i + "}\n");
  }


  /**
    * Compress the textual representation of an {@link ASTNode}, convert to
    * ASCII hexadecimal, and write the result to the specified stream.
    *
    * @param buffer The text representation to be written.
    * @param out    The stream to write to.
   **/
  public static void compressAndPrint(StringBuffer buffer, PrintStream out)
  {
    PrintStream converter = null;
    ByteArrayOutputStream converted = new ByteArrayOutputStream();
    try
    {
      converter =
        new PrintStream(new GZIPOutputStream(new HexOutputStream(converted)));
    }
    catch (Exception e)
    {
      System.err.println("Could not create converter stream.");
      System.exit(1);
    }

    converter.print(buffer.toString());
    converter.close();

    try { converted.writeTo(out); }
    catch (Exception e)
    {
      System.err.println("Could not write the converted stream.");
      System.exit(1);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ast  The node to process.
   **/
  public void run(AST ast)
  {
    if (!RevisionAnalysis.noChanges)
    {
      quantificationVariables = new HashMap();
      contextVariables = new HashMap();
      runOnChildren(ast);
    }
  }


  /**
    * Code is only generated for a {@link ClassifierName} when it is the only
    * {@link ClassifierExpression} on the right hand side of the arrow (and
    * there really shouldn't be a reason that a programmer would want to write
    * such a declaration, but if he does, it will work).
    *
    * @param cn The node to process.
   **/
  public void run(ClassifierName cn)
  {
    if (cn.name == cn.referent
        || !RevisionAnalysis.revisionStatus.get(cn.name.toString())
            .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + cn.name);

    PrintStream out = open(cn);
    if (out == null) return;

    out.println(disclaimer);
    out.println("// " + cn.shallow() + "\n");

    ast.symbolTable.generateHeader(out);

    String field = null;
    boolean cachedInMap = false;
    if (cn.cacheIn != null)
    {
      field = cn.cacheIn.toString();
      cachedInMap = field.equals(ClassifierAssignment.mapCache);
      if (cachedInMap) out.println("import java.util.WeakHashMap;");
    }

    out.println("\n");
    if (cn.comment != null) out.println(cn.comment);

    out.println("public class " + cn.name + " extends Classifier");
    out.println("{");

    if (cachedInMap)
      out.println("  private static final WeakHashMap __valueCache "
                  + "= new WeakHashMap();");

    String referentNoDots = cn.referent.toString().replace('.', '$');
    out.println("  private static final " + cn.referent + " __"
                + referentNoDots + " = new " + cn.referent + "();\n");

    String fqName = ast.symbolTable.getPackage();
    if (fqName.length() > 0) fqName += ".";
    fqName += cn.name;
    out.println("  public " + cn.name + "() { super(\"" + fqName
                + "\"); }\n");

    LBJ2.IR.Type input = cn.argument.getType();
    typeReturningMethods(out, input, cn.returnType);
    boolean discrete =
      cn.returnType.type == ClassifierReturnType.DISCRETE
      || cn.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
      || cn.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR;
    boolean array =
      cn.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
      || cn.returnType.type == ClassifierReturnType.REAL_ARRAY;
    boolean generator =
      cn.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
      || cn.returnType.type == ClassifierReturnType.REAL_GENERATOR
      || cn.returnType.type == ClassifierReturnType.MIXED_GENERATOR;

    String methodName = null;
    if (!generator)
    {
      String returnType = null;

      if (cn.returnType.type == ClassifierReturnType.DISCRETE)
      {
        returnType = "String";
        methodName = "discreteValue";
      }
      else if (cn.returnType.type == ClassifierReturnType.REAL)
      {
        returnType = "double";
        methodName = "realValue";
      }
      else if (cn.returnType.type == ClassifierReturnType.DISCRETE_ARRAY)
      {
        returnType = "String[]";
        methodName = "discreteValueArray";
      }
      else
      {
        returnType = "double[]";
        methodName = "realValueArray";
      }

      out.println("\n  public " + returnType + " " + methodName
                  + "(Object example)");
      out.println("  {");
      String compute = "__" + referentNoDots + "." + methodName + "(example)";

      if (field == null) out.println("    return " + compute + ";");
      else
      {
        if (!field.equals(ClassifierAssignment.mapCache))
          out.println("    " + cn.argument + " = (" + cn.argument.getType()
                      + ") example;");
        returnCachedValue(out, field, array, discrete, cn.name.toString(),
                          "example", compute, null, "    ");
      }

      out.println("  }");
    }

    out.println("\n  public FeatureVector classify(Object example)");
    out.println("  {");

    if (field == null)
      out.println("    return __" + referentNoDots + ".classify(example);");
    else
    {
      out.println("    FeatureVector result = new FeatureVector();");
      out.print("    ");
      if (discrete) out.print("String");
      else out.print("double");

      if (array)
      {
        out.println("[] __cachedValue = " + methodName + "(example);");
        out.println("    for (int __i = 0; __i < "
                    + "__cachedValue.__length; ++__i)");
        out.print("      result.addFeature(new ");

        if (discrete) out.print("Discrete");
        else out.print("Real");

        out.print(
            "ArrayFeature(containingPackage, name, __cachedValue[__i], ");

        if (discrete)
          out.print("valueIndexOf(__cachedValue[__i]), "
                    + "(short) allowableValues().length, ");

        out.println("__i, __cachedValue.__length));");
      }
      else
      {
        out.println(" __cachedValue = " + methodName + "(example);");
        out.print("    result.addFeature(new ");
        if (discrete) out.print("Discrete");
        else out.print("Real");
        out.print("Feature(this.containingPackage, this.name, __cachedValue");
        if (discrete)
          out.print(", valueIndexOf(__cachedValue), "
                    + "(short) allowableValues().length");
        out.println("));");
      }

      out.println("    return result;");
    }

    out.println("  }\n");

    typeCheckClassifyArray(out, cn.name.toString(), input, cn.line + 1);
    out.println();
    generateHashingMethods(out, cn.name.toString());

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param cc The node to process.
   **/
  public void run(CodedClassifier cc)
  {
    String fileName = cc.name + ".java";
    if (fileName.startsWith(Main.sourceFileBase + "$")) files.add(fileName);

    if (!RevisionAnalysis.revisionStatus.get(cc.name.toString())
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + cc.name);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.print("// ");
    compressAndPrint(cc.shallow(), out);
    out.println("\n");

    ast.symbolTable.generateHeader(out);

    String field = null;
    boolean cachedInMap = false;
    if (cc.cacheIn != null)
    {
      field = cc.cacheIn.toString();
      cachedInMap = field.equals(ClassifierAssignment.mapCache);
      if (cachedInMap) out.println("import java.util.WeakHashMap;");
    }

    out.println("\n");
    if (cc.comment != null) out.println(cc.comment);

    out.println("public class " + cc.name + " extends Classifier");
    out.println("{");

    if (cachedInMap)
      out.println("  private static final WeakHashMap __valueCache "
                  + "= new WeakHashMap();");

    HashSet invoked =
      (HashSet) SemanticAnalysis.invokedGraph.get(cc.name.toString());
    if (invoked != null && invoked.size() > 0)
    {
      for (Iterator I = invoked.iterator(); I.hasNext(); )
      {
        String name = (String) I.next();
        String nameNoDots = name.replace('.', '$');
        out.println("  private static final " + name + " __" + nameNoDots
                    + " = new " + name + "();");
      }

      out.println();
    }

    out.println("  private static ThreadLocal cache = new ThreadLocal(){ };");
    out.println("  private static ThreadLocal exampleCache = "
                + "new ThreadLocal(){ };\n");

    String fqName = ast.symbolTable.getPackage();
    if (fqName.length() > 0) fqName += ".";
    fqName += cc.name;
    out.println("  public " + cc.name + "() { super(\"" + fqName
                + "\"); }\n");

    LBJ2.IR.Type input = cc.argument.getType();
    typeReturningMethods(out, input, cc.returnType);

    indent = 2;
    forInit = false;
    methodBody.delete(0, methodBody.length());
    currentCG = cc;
    for (ASTNodeIterator I = cc.body.iterator(); I.hasNext(); )
    {
      I.next().runPass(this);
      methodBody.append("\n");
    }

    if (cc.returnType.type == ClassifierReturnType.DISCRETE
        || cc.returnType.type == ClassifierReturnType.REAL)
    {
      boolean discrete = cc.returnType.type == ClassifierReturnType.DISCRETE;

      if (discrete && cc.returnType.values.size() > 0)
      {
        out.println("\n  private String _discreteValue(" + cc.argument + ")");
        out.println("  {");
        out.print(methodBody);
        out.println("  }\n");

        out.println("  public String discreteValue(Object example)");
        out.println("  {");
        generateTypeChecking(out, cc.name.toString(), "Classifier",
                             input.toString(), cc.line + 1, "example", 2);

        if (cc.cacheIn == null)
        {
          out.println("    String result = _discreteValue((" + input + ") "
                      + "example);\n");

          out.println("    if (valueIndexOf(result) == -1)");
          out.println("    {");
          out.println("      System.err.println(\"Classifier '" + cc.name
                      + "' defined on line " + cc.line + " of "
                      + Main.sourceFilename + " produced '\" + result + \"' "
                      + "as a feature value, which is not allowable.\");");
          out.println("      System.exit(1);");
          out.println("    }\n");

          out.println("    return result;");
        }
        else
        {
          out.println("    " + cc.argument + " = (" + input + ") example;\n");

          String variable = cachedInMap ? "__cachedValue" : field;
          String rCompute = "      " + variable + " = _discreteValue("
                            + cc.argument.getName() + ");\n";
          rCompute += "      if (valueIndexOf(" + variable + ") == -1)";
          rCompute += "      {\n";
          rCompute += "        System.err.println(\"Classifier '" + cc.name
                      + "' defined on line " + cc.line + " of "
                      + Main.sourceFilename + " produced '\" + " + variable
                      + " + \"' as a feature value, which is not "
                      + "allowable.\");\n";
          rCompute += "        System.exit(1);\n";
          rCompute += "      }";

          if (cachedInMap)
            rCompute += "\n\n    " + cc.name
                        + ".__valueCache.put(example, __cachedValue);";

          returnCachedValue(out, field, false, true, cc.name.toString(),
                            "example", null, rCompute, "    ");

          out.println("    return __cachedValue;");
        }

        out.println("  }");
      }
      else
      {
        out.print("\n  ");
        if (cc.cacheIn != null) out.print("private");
        else out.print("public");
        out.print(" " + (discrete ? "String" : "double") + " ");
        if (cc.cacheIn != null) out.print("_");
        out.println((discrete ? "discrete" : "real")
                    + "Value(Object __example)");
        out.println("  {");
        generateTypeChecking(out, cc.name.toString(), "Classifier",
                             input.toString(), cc.line + 1, "__example", 2);

        out.println("    " + cc.argument + " = (" + input + ") __example;");
        out.print(methodBody);
        out.println("  }");

        if (cc.cacheIn != null)
        {
          out.println("\n  public " + (discrete ? "String" : "double") + " "
                      + (discrete ? "discrete" : "real")
                      + "Value(Object example)");
          out.println("  {");
          if (!cachedInMap)
            out.println("    " + cc.argument + " = (" + input
                        + ") example;\n");

          String compute =
            "_" + (discrete ? "discrete" : "real") + "Value(example)";
          returnCachedValue(out, field, false, discrete, cc.name.toString(),
                            "example", compute, null, "    ");

          out.println("  }");
        }
      }

      out.println("\n  public FeatureVector classify(Object example)");
      out.println("  {");
      out.println("    if (example == exampleCache.get()) "
                  + "return (FeatureVector) cache.get();");

      if (discrete)
      {
        if (cc.returnType.values.size() > 0)
        {
          out.println("    String value = discreteValue(example);");
          out.println("    cache.set(new FeatureVector(new "
                      + "DiscreteFeature(containingPackage, name, value, "
                      + "valueIndexOf(value), (short) "
                      + cc.returnType.values.size() + ")));");
        }
        else
          out.println("    cache.set(new FeatureVector(new "
                      + "DiscreteFeature(containingPackage, name, "
                      + "discreteValue(example))));");
      }
      else
        out.println("    cache.set(new FeatureVector(new "
                    + "RealFeature(containingPackage, name, "
                    + "realValue(example))));");

      out.println("    exampleCache.set(example);");
      out.println("    return (FeatureVector) cache.get();");
      out.println("  }");
    }
    else
    {
      if (cc.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
          || cc.returnType.type == ClassifierReturnType.REAL_ARRAY)
      {
        out.println();
        generateValue(out, cc.name, cc.returnType, field, cc.argument);
      }

      out.println("\n  public FeatureVector classify(Object __example)");
      out.println("  {");
      generateTypeChecking(out, cc.name.toString(), "Classifier",
                           input.toString(), cc.line + 1, "__example", 2);

      out.println("    if (__example == " + cc.name
                  + ".exampleCache.get()) return (FeatureVector) " + cc.name
                  + ".cache.get();\n");

      out.println("    " + cc.argument + " = (" + input + ") __example;");
      boolean discrete =
        cc.returnType.type == ClassifierReturnType.DISCRETE_ARRAY;

      if (field != null)
      {
        out.print("    ");
        if (discrete) out.print("String");
        else out.print("double");
        out.print("[] __cachedValue = ");

        if (cachedInMap)
        {
          out.print("(");
          if (discrete) out.print("String");
          else out.print("double");
          out.println("[]) " + cc.name + ".__valueCache.get(__example);");
        }
        else out.println(field + ";");

        out.println("    if (__cachedValue != null)");
        out.println("    {");
        out.println("      " + cc.name + ".exampleCache.set(__example);");
        out.println("      " + cc.name + ".cache.set(new FeatureVector());");
        out.println("      for (int i = 0; i < __cachedValue.length; ++i)");
        out.print("        ((FeatureVector) " + cc.name
                  + ".cache.get()).addFeature(new ");

        if (discrete) out.print("Discrete");
        else out.print("Real");

        out.print("ArrayFeature(containingPackage, name, __cachedValue[i], ");

        if (discrete)
          out.print("valueIndexOf(__cachedValue[i]), "
                    + "(short) allowableValues().length, ");

        out.println("i, __cachedValue.length));");

        out.println("      return (FeatureVector) " + cc.name
                    + ".cache.get();");
        out.println("    }\n");
      }

      out.println("    FeatureVector __result = new FeatureVector();");
      if (cc.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
          || cc.returnType.type == ClassifierReturnType.REAL_ARRAY)
        out.println("    int __featureIndex = 0;");
      if (cc.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
          || cc.returnType.type == ClassifierReturnType.REAL_GENERATOR)
        out.println("    String __id;");
      if (cc.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
          || cc.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR)
        out.println("    String __value;");

      out.println("\n" + methodBody);

      if (cc.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
          || cc.returnType.type == ClassifierReturnType.REAL_ARRAY)
      {
        out.println("    for (java.util.Iterator __I = __result.iterator(); "
                    + "__I.hasNext(); )");
        out.println("      ((Feature) __I.next())"
                    + ".setArrayLength(__featureIndex);\n");

        if (field != null)
        {
          out.print("    " + (cachedInMap ? "__cachedValue" : field)
                    + " = __result.");
          if (discrete) out.print("discrete");
          else out.print("real");
          out.println("ValueArray();\n");

          if (cachedInMap)
            out.println("    " + cc.name
                        + ".__valueCache.put(__example, __cachedValue);");
        }
      }

      out.println("    " + cc.name + ".exampleCache.set(__example);");
      out.println("    " + cc.name + ".cache.set(__result);\n");

      out.println("    return __result;");
      out.println("  }");
    }

    out.println();
    typeCheckClassifyArray(out, cc.name.toString(), input, cc.line + 1);
    out.println();
    generateHashingMethods(out, cc.name.toString());

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param cg The node to process.
   **/
  public void run(CompositeGenerator cg)
  {
    String fileName = cg.name + ".java";
    if (fileName.startsWith(Main.sourceFileBase + "$"))
    {
      files.add(fileName);
      runOnChildren(cg);
    }
    else
    {
      files.clear();

      runOnChildren(cg);

      final String prefix = Main.sourceFileBase + "$" + cg.name;
      File[] leftOvers =
        new File(System.getProperty("user.dir")).listFiles(
          new FilenameFilter()
          {
            public boolean accept(File directory, String name)
            {
              int i = name.lastIndexOf('.');
              if (i == -1) return false;
              String javaFile = name.substring(0, i) + ".java";
              return name.startsWith(prefix) && !files.contains(javaFile);
            }
          });

      for (int i = 0; i < leftOvers.length; ++i)
        if (leftOvers[i].exists() && !leftOvers[i].delete())
          reportError(0, "Could not delete '" + leftOvers[i].getName()
                         + "'.");
    }

    if (!RevisionAnalysis.revisionStatus.get(cg.name.toString())
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + cg.name);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.print("// ");
    compressAndPrint(cg.shallow(), out);
    out.println("\n");

    ast.symbolTable.generateHeader(out);

    out.println("\n");
    if (cg.comment != null) out.println(cg.comment);

    out.println("public class " + cg.name + " extends Classifier");
    out.println("{");

    for (ClassifierExpressionList.ClassifierExpressionListIterator I =
           cg.components.listIterator();
         I.hasNext(); )
    {
      String name = I.nextItem().name.toString();
      String nameNoDots = name.replace('.', '$');
      out.println("  private static final " + name + " __" + nameNoDots
                  + " = new " + name + "();");
    }

    out.println("\n  private static ThreadLocal cache = new ThreadLocal(){ };");
    out.println("  private static ThreadLocal exampleCache = "
                + "new ThreadLocal(){ };\n");

    String fqName = ast.symbolTable.getPackage();
    if (fqName.length() > 0) fqName += ".";
    fqName += cg.name;
    out.println("  public " + cg.name + "() { super(\"" + fqName
                + "\"); }\n");

    LBJ2.IR.Type input = cg.argument.getType();
    typeReturningMethods(out, input, cg.returnType);

    out.println("\n  public FeatureVector classify(Object example)");
    out.println("  {");
    generateTypeChecking(out, cg.name.toString(), "Classifier",
                         input.toString(), cg.line + 1, "example", 2);

    out.println("    if (example == exampleCache.get()) "
                + "return (FeatureVector) cache.get();\n");

    out.println("    FeatureVector result = new FeatureVector();");

    for (ClassifierExpressionList.ClassifierExpressionListIterator I =
           cg.components.listIterator();
         I.hasNext(); )
    {
      String nameNoDots = ("__" + I.nextItem().name).replace('.', '$');
      out.println("    result.addFeatures(" + nameNoDots
                  + ".classify(example));");
    }

    out.println("\n    exampleCache.set(example);");
    out.println("    cache.set(result);\n");

    out.println("    return result;");
    out.println("  }\n");

    typeCheckClassifyArray(out, cg.name.toString(), input, cg.line + 1);
    out.println();
    generateHashingMethods(out, cg.name.toString());

    out.println("\n  public java.util.LinkedList getCompositeChildren()");
    out.println("  {");
    out.println("    java.util.LinkedList result = new "
                + "java.util.LinkedList();");

    for (ClassifierExpressionList.ClassifierExpressionListIterator I =
           cg.components.listIterator();
         I.hasNext(); )
    {
      String nameNoDots = ("__" + I.nextItem().name).replace('.', '$');
      out.println("    result.add(" + nameNoDots + ");");
    }

    out.println("    return result;");
    out.println("  }");

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param ii The node to process.
   **/
  public void run(InferenceInvocation ii)
  {
    if (!RevisionAnalysis.revisionStatus.get(ii.name.toString())
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + ii.name);

    PrintStream out = open(ii);
    if (out == null) return;

    out.println(disclaimer);
    out.println("// " + ii.shallow() + "\n");

    ast.symbolTable.generateHeader(out);

    String field = null;
    boolean cachedInMap = false;
    if (ii.cacheIn != null)
    {
      field = ii.cacheIn.toString();
      cachedInMap = field.equals(ClassifierAssignment.mapCache);
      if (cachedInMap) out.println("import java.util.WeakHashMap;");
    }

    out.println("\n");
    if (ii.comment != null) out.println(ii.comment);

    out.println("public class " + ii.name + " extends Classifier");
    out.println("{");

    if (cachedInMap)
      out.println("  private static final WeakHashMap __valueCache "
                  + "= new WeakHashMap();");

    out.println("  private static final " + ii.classifier + " __"
                + ii.classifier + " = new " + ii.classifier + "();\n");

    String fqName = ast.symbolTable.getPackage();
    if (fqName.length() > 0) fqName += ".";
    fqName += ii.name;
    out.println("  public " + ii.name + "() { super(\"" + fqName
                + "\"); }\n");

    ClassifierType iiType = (ClassifierType) ii.classifier.typeCache;
    LBJ2.IR.Type input = iiType.getInput();
    typeReturningMethods(out, input, iiType.getOutput());

    InferenceType inferenceType = (InferenceType) ii.inference.typeCache;

    String fqInferenceName = ii.inference.toString();
    if (ast.symbolTable.containsKey(ii.inference)
        && !ast.symbolTable.getPackage().equals(""))
      fqInferenceName = ast.symbolTable.getPackage() + "." + fqInferenceName;

    out.println("\n  public String discreteValue(Object __example)");
    out.println("  {");
    generateTypeChecking(out, ii.name.toString(), "Classifier",
                         input.toString(), ii.line + 1, "__example", 2);

    String in = "    ";

    if (field == null)
    {
      out.println(in + inferenceType.getHeadType() + " __head = "
                  + ii.inference + ".findHead((" + input + ") __example);");
      out.println(in + ii.inference + " __inference = (" + ii.inference
                  + ") InferenceManager.get(\"" + fqInferenceName
                  + "\", __head);\n");

      out.println(in + "if (__inference == null)");
      out.println(in + "{");
      out.println(in + "  __inference = new " + ii.inference + "(__head);");
      out.println(in + "  InferenceManager.put(__inference);");
      out.println(in + "}\n");

      out.println(in + "String __result = null;\n");

      out.println(in + "try { __result = __inference.valueOf(__"
                  + ii.classifier + ", __example); }");
      out.println(in + "catch (Exception __e)");
      out.println(in + "{");
      out.println(in + "  System.err.println(\"Fatal error while evaluating "
                  + "classifier " + ii.name + ": \" + __e);");
      out.println(in + "  __e.printStackTrace();");
      out.println(in + "  System.exit(1);");
      out.println(in + "}");

      out.println("\n    return __result;");
    }
    else
    {
      if (!cachedInMap)
        out.println(in + ii.argument + " = (" + input + ") __example;\n");

      in += "  ";
      String rCompute =
        in + inferenceType.getHeadType() + " __head = " + ii.inference
        + ".findHead((" + input + ") __example);\n";
      rCompute += in + ii.inference + " __inference = (" + ii.inference
                  + ") InferenceManager.get(\"" + fqInferenceName
                  + "\", __head);\n\n";

      rCompute += in + "if (__inference == null)\n";
      rCompute += in + "{\n";
      rCompute += in + "  __inference = new " + ii.inference + "(__head);\n";
      rCompute += in + "  InferenceManager.put(__inference);\n";
      rCompute += in + "}\n\n";

      rCompute += in + "try { " + (cachedInMap ? "__cachedValue" : field)
                  + " = __inference.valueOf(__" + ii.classifier
                  + ", __example); }\n";
      rCompute += in + "catch (Exception __e)\n";
      rCompute += in + "{\n";
      rCompute += in + "  System.err.println(\"Fatal error while evaluating "
                  + "classifier " + ii.name + ": \" + __e);\n";
      rCompute += in + "  __e.printStackTrace();\n";
      rCompute += in + "  System.exit(1);\n";
      rCompute += in + "}";

      if (cachedInMap)
        rCompute += "\n\n" + in + ii.name
                    + ".__valueCache.put(__example, __cachedValue);";

      returnCachedValue(out, field, false, true, ii.name.toString(),
                        "__example", null, rCompute, "    ");
    }

    out.println("  }\n");

    out.println("  public FeatureVector classify(Object example)");
    out.println("  {");
    out.println("    String value = discreteValue(example);");
    out.println("    return new FeatureVector(new "
                + "DiscreteFeature(containingPackage, name, value, "
                + "valueIndexOf(value), (short) "
                + iiType.getOutput().values.size() + "));");
    out.println("  }\n");

    typeCheckClassifyArray(out, ii.name.toString(), input, ii.line + 1);
    out.println();
    generateHashingMethods(out, ii.name.toString());

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param lce  The node to process.
   **/
  public void run(LearningClassifierExpression lce)
  {
    String fileName = lce.name + ".java";

    if (fileName.startsWith(Main.sourceFileBase + "$"))
    {
      files.add(fileName);
      runOnChildren(lce);
    }
    else
    {
      files.clear();

      runOnChildren(lce);

      final String prefix = Main.sourceFileBase + "$" + lce.name;
      File[] leftOvers =
        new File(System.getProperty("user.dir")).listFiles(
          new FilenameFilter()
          {
            public boolean accept(File directory, String name)
            {
              int i = name.lastIndexOf('.');
              if (i == -1) return false;
              String javaFile = name.substring(0, i) + ".java";
              return name.startsWith(prefix) && !files.contains(javaFile);
            }
          });

      for (int i = 0; i < leftOvers.length; ++i)
        if (leftOvers[i].exists() && !leftOvers[i].delete())
          reportError(0, "Could not delete '" + leftOvers[i].getName()
                         + "'.");
    }

    if (lce.parser != null
          && RevisionAnalysis.revisionStatus.get(lce.name.toString())
             .equals(RevisionAnalysis.UNAFFECTED)
        || lce.parser == null
           && !RevisionAnalysis.revisionStatus.get(lce.name.toString())
               .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + lce.name);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.println("// \n");

    ast.symbolTable.generateHeader(out);

    if (lce.cacheIn != null)
    {
      String field = lce.cacheIn.toString();
      boolean cachedInMap = field.equals(ClassifierAssignment.mapCache);
      if (cachedInMap) out.println("import java.util.WeakHashMap;");
    }

    out.println("\n");
    if (lce.comment != null) out.println(lce.comment);

    out.println("public class " + lce.name + " extends " + lce.learnerName);
    out.println("{");
    out.println("  public static boolean isTraining = true;");
    out.println("  private static java.net.URL lcFilePath = " + lce.name
                + ".class.getResource(\"" + lce.name + ".lc\");");

    out.println("  private static " + lce.name + " instance = new " + lce.name
                + "(false);");
    out.println("  public static " + lce.name + " getInstance()");
    out.println("  {");
    out.println("    return instance;");
    out.println("  }\n");

    out.println("  public void save()");
    out.println("  {");
    out.println("    System.err.println(\"" + lce.name
                + ".save() was called while its source code is at an "
                + "intermediate state.  Re-run the LBJ compiler.\");");
    out.println("    System.exit(1);");
    out.println("  }\n");

    generateLearnerBody(out, lce, false);

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(Conjunction c)
  {
    String fileName = c.name + ".java";
    if (fileName.startsWith(Main.sourceFileBase + "$"))
    {
      files.add(fileName);
      runOnChildren(c);
    }
    else
    {
      files.clear();

      runOnChildren(c);

      final String prefix = Main.sourceFileBase + "$" + c.name;
      File[] leftOvers =
        new File(System.getProperty("user.dir")).listFiles(
          new FilenameFilter()
          {
            public boolean accept(File directory, String name)
            {
              int i = name.lastIndexOf('.');
              if (i == -1) return false;
              String javaFile = name.substring(0, i) + ".java";
              return name.startsWith(prefix) && !files.contains(javaFile);
            }
          });

      for (int i = 0; i < leftOvers.length; ++i)
        if (leftOvers[i].exists() && !leftOvers[i].delete())
          reportError(0, "Could not delete '" + leftOvers[i].getName()
                         + "'.");
    }

    if (!RevisionAnalysis.revisionStatus.get(c.name.toString())
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + c.name);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.println("// " + c.shallow() + "\n");

    ast.symbolTable.generateHeader(out);

    String field = null;
    boolean cachedInMap = false;
    if (c.cacheIn != null)
    {
      field = c.cacheIn.toString();
      cachedInMap = field.equals(ClassifierAssignment.mapCache);
      if (cachedInMap) out.println("import java.util.WeakHashMap;");
    }

    out.println("\n");
    if (c.comment != null) out.println(c.comment);

    out.println("public class " + c.name + " extends Classifier");
    out.println("{");

    if (cachedInMap)
      out.println("  private static final WeakHashMap __valueCache "
                  + "= new WeakHashMap();");

    out.println("  private static final " + c.left.name + " left = new "
                + c.left.name + "();");
    out.println("  private static final " + c.right.name + " right = new "
                + c.right.name + "();\n");

    out.println("  private static ThreadLocal cache = new ThreadLocal(){ };");
    out.println("  private static ThreadLocal exampleCache = "
                + "new ThreadLocal(){ };\n");

    String fqName = ast.symbolTable.getPackage();
    if (fqName.length() > 0) fqName += ".";
    fqName += c.name;
    out.println("  public " + c.name + "() { super(\"" + fqName + "\"); }\n");

    LBJ2.IR.Type input = c.argument.getType();
    typeReturningMethods(out, input, c.returnType);

    out.println("\n  public FeatureVector classify(Object __example)");
    out.println("  {");
    generateTypeChecking(out, c.name.toString(), "Classifier",
                         input.toString(), c.line + 1, "__example", 2);

    out.println("    if (__example == " + c.name
                + ".exampleCache.get()) return (FeatureVector) "
                + c.name + ".cache.get();\n");

    boolean discrete = false;
    boolean array = false;
    if (field != null)
    {
      discrete = c.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
                 || c.returnType.type == ClassifierReturnType.DISCRETE;
      array = c.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
              || c.returnType.type == ClassifierReturnType.REAL_ARRAY;

      if (!cachedInMap)
        out.println("    " + c.argument + " = (" + input + ") __example;");

      if (array)
      {
        out.print("    ");
        if (discrete) out.print("String");
        else out.print("double");
        out.print("[] __cachedValue = ");

        if (cachedInMap)
        {
          out.print("(");
          if (discrete) out.print("String");
          else out.print("double");
          out.println("[]) " + c.name + ".__valueCache.get(__example);");
        }
        else out.println(field + ";");

        out.println("    if (__cachedValue != null)");
        out.println("    {");
        out.println("      " + c.name + ".exampleCache.set(__example);");
        out.println("      " + c.name + ".cache.set(new FeatureVector());");
        out.println("      for (int i = 0; i < __cachedValue.length; ++i)");
        out.print("        ((FeatureVector) " + c.name
                  + ".cache.get()).addFeature(new ");

        if (discrete) out.print("Discrete");
        else out.print("Real");

        out.print("ArrayFeature(containingPackage, name, __cachedValue[i], ");

        if (discrete)
          out.print("valueIndexOf(__cachedValue[i]), "
                    + "(short) allowableValues().length, ");

        out.println("i, __cachedValue.length));");

        out.println("      return (FeatureVector) " + c.name
                    + ".cache.get();");
        out.println("    }\n");
      }
      else
      {
        if (cachedInMap && !discrete)
          out.println("    Double __dValue = (Double) " + c.name
                      + ".__valueCache.get(__example);");

        out.print("    ");
        if (discrete) out.print("String");
        else out.print("double");
        out.print(" __cachedValue = ");

        if (cachedInMap)
        {
          if (!discrete)
            out.println("__dValue == null ? Double.NaN : "
                        + "__dValue.doubleValue();");
          else
          {
            out.print("(");
            if (discrete) out.print("String");
            else out.print("double");
            out.println(") " + c.name + ".__valueCache.get(__example);");
          }
        }
        else out.println(field + ";");

        out.print("    if (");
        if (!discrete) out.print("Double.doubleToLongBits(");
        out.print("__cachedValue");
        if (!discrete) out.print(")");
        out.print(" != ");
        if (!discrete) out.print("Double.doubleToLongBits(Double.NaN)");
        else out.print("null");
        out.println(")");
        out.println("    {");
        out.println("      " + c.name + ".exampleCache.set(__example);");
        out.println("      " + c.name + ".cache.set(new FeatureVector());");
        out.print("      ((FeatureVector) " + c.name
                  + ".cache.get()).addFeature(new ");
        if (discrete) out.print("Discrete");
        else out.print("Real");
        out.print("Feature(containingPackage, name, __cachedValue");
        if (discrete)
          out.print(", valueIndexOf(__cachedValue), "
                    + "(short) allowableValues().length");
        out.println("));");

        out.println("      return (FeatureVector) " + c.name
                    + ".cache.get();");
        out.println("    }\n");
      }
    }

    int leftType = c.left.returnType.type;
    int rightType = c.right.returnType.type;
    boolean sameType = leftType == rightType;
    boolean leftIsGenerator =
      leftType == ClassifierReturnType.DISCRETE_GENERATOR
      || rightType == ClassifierReturnType.REAL_GENERATOR;
    boolean eitherIsMixed =
      leftType == ClassifierReturnType.MIXED_GENERATOR
      || rightType == ClassifierReturnType.MIXED_GENERATOR;
    boolean bothMulti =
      leftType != ClassifierReturnType.DISCRETE
      && leftType != ClassifierReturnType.REAL
      && rightType != ClassifierReturnType.DISCRETE
      && rightType != ClassifierReturnType.REAL;

    out.println("    FeatureVector leftVector = left.classify(__example);");
    out.println("    FeatureVector rightVector = right.classify(__example);");
    out.println("    " + c.name + ".cache.set(new FeatureVector());");
    out.println("    for (java.util.Iterator I = leftVector.iterator(); "
                + "I.hasNext(); )");
    out.println("    {");
    out.println("      Feature lf = (Feature) I.next();");
    out.println("      for (java.util.Iterator J = rightVector.iterator(); "
                + "J.hasNext(); )");
    out.println("      {");
    out.println("        Feature rf = (Feature) J.next();");
    if (eitherIsMixed || leftIsGenerator && sameType)
      out.println("        if (lf.equals(rf)) continue;");
    out.println("        ((FeatureVector) " + c.name
                + ".cache.get()).addFeature(lf.conjunction(rf, this));");
    out.println("      }");
    out.println("    }\n");

    if (field != null)
    {
      if (cachedInMap)
      {
        out.print("    " + c.name + ".__valueCache.put(__example, ");
        if (!array && !discrete) out.print("new Double(");
      }
      else out.print("    " + field + " = ");

      if (array)
      {
        out.print("((FeatureVector) " + c.name + ".cache.get()).");
        if (discrete) out.print("discrete");
        else out.print("real");
        out.print("ValueArray()");
      }
      else
      {
        out.print("((");
        if (discrete) out.print("Discrete");
        else out.print("Real");
        out.print("Feature) ((FeatureVector) " + c.name
                  + ".cache.get()).firstFeature()).getValue()");
      }

      if (cachedInMap)
      {
        if (!array && !discrete) out.print(")");
        out.print(")");
      }

      out.println(";");
    }

    if (bothMulti)
      out.println("    ((FeatureVector) " + c.name
                  + ".cache.get()).sort();\n");

    out.println("    " + c.name + ".exampleCache.set(__example);");
    out.println("    return (FeatureVector) " + c.name + ".cache.get();");
    out.println("  }\n");

    typeCheckClassifyArray(out, c.name.toString(), input, c.line + 1);
    out.println();
    generateHashingMethods(out, c.name.toString());
    out.println();
    generateValue(out, c.name, c.returnType, field, c.argument);

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param cd The node to process.
   **/
  public void run(ConstraintDeclaration cd)
  {
    String cdName = cd.name.toString();
    String fileName = cdName + ".java";

    if (!RevisionAnalysis.revisionStatus.get(cdName)
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + cdName);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.print("// ");
    compressAndPrint(cd.shallow(), out);
    out.println("\n");

    ast.symbolTable.generateHeader(out);

    out.println("\n");
    if (cd.comment != null) out.println(cd.comment);

    out.println("public class " + cdName
                + " extends ParameterizedConstraint");
    out.println("{");

    HashSet invoked = (HashSet) SemanticAnalysis.invokedGraph.get(cdName);
    if (invoked != null && invoked.size() > 0)
    {
      for (Iterator I = invoked.iterator(); I.hasNext(); )
      {
        String name = (String) I.next();
        String nameNoDots = name.replace('.', '$');
        out.println("  private static final " + name + " __" + nameNoDots
                    + " = new " + name + "();");
      }

      out.println();
    }

    String fqName = ast.symbolTable.getPackage();
    if (fqName.length() > 0) fqName += ".";
    fqName += cdName;
    out.println("  public " + cdName + "() { super(\"" + fqName + "\"); }\n");

    LBJ2.IR.Type input = cd.argument.getType();
    out.println("  public String getInputType() { return \""
                + input.typeClass().getName() + "\"; }\n");

    indent = 2;
    forInit = false;
    constraintMode = false;
    methodBody.delete(0, methodBody.length());
    currentCG = cd;
    for (ASTNodeIterator I = cd.body.iterator(); I.hasNext(); )
    {
      I.next().runPass(this);
      methodBody.append("\n");
    }

    out.println("  public String discreteValue(Object __example)");
    out.println("  {");
    generateTypeChecking(out, cdName, "Constraint", input.toString(),
                         cd.line + 1, "__example", 2);

    out.println("    " + cd.argument + " = (" + input + ") __example;\n");

    out.println(methodBody);

    out.println("    return \"true\";");
    out.println("  }");

    out.println();
    typeCheckClassifyArray(out, cdName, input, cd.line + 1);
    out.println();
    generateHashingMethods(out, cdName);

    indent = 2;
    forInit = false;
    constraintMode = true;
    methodBody.delete(0, methodBody.length());
    for (ASTNodeIterator I = cd.body.iterator(); I.hasNext(); )
    {
      I.next().runPass(this);
      methodBody.append("\n");
    }

    out.println("\n  public FirstOrderConstraint makeConstraint(Object "
                + "__example)");
    out.println("  {");
    generateTypeChecking(out, cdName, "Constraint", input.toString(),
                         cd.line + 1, "__example", 2);

    out.println("    " + cd.argument + " = (" + input + ") __example;");
    out.println("    FirstOrderConstraint __result = new "
                + "FirstOrderConstant(true);\n");

    out.println(methodBody);

    out.println("    return __result;");
    out.println("  }");

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param in The node to process.
   **/
  public void run(InferenceDeclaration in)
  {
    in.constraint.runPass(this);

    String inName = in.name.toString();
    String fileName = inName + ".java";

    if (!RevisionAnalysis.revisionStatus.get(inName)
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + inName);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.print("// ");
    compressAndPrint(in.shallow(), out);
    out.println("\n");

    ast.symbolTable.generateHeader(out);
    out.println("import java.util.*;\n\n");

    currentCG = in;
    String defaultNormalizer = "new IdentityNormalizer()";

    if (in.comment != null) out.println(in.comment);

    out.println("public class " + inName + " extends " + in.algorithm.name);
    out.println("{");

    if (in.containsTypeSpecificNormalizer())
    {
      out.println("  private static final HashMap normalizers = new "
                  + "HashMap();");
      out.println("  static");
      out.println("  {");
      for (int i = 0; i < in.normalizerDeclarations.length; ++i)
      {
        if (in.normalizerDeclarations[i].learner != null)
          out.println("    normalizers.put(new "
                      + in.normalizerDeclarations[i].learner + "(), "
                      + in.normalizerDeclarations[i].normalizer + ");");
        else
          defaultNormalizer =
            in.normalizerDeclarations[i].normalizer.toString();
      }

      out.println("  }\n");
    }
    else
      for (int i = 0; i < in.normalizerDeclarations.length; ++i)
        defaultNormalizer =
          in.normalizerDeclarations[i].normalizer.toString();

    indent = 1;
    forInit = false;
    methodBody.delete(0, methodBody.length());
    for (int i = 0; i < in.headFinders.length; ++i)
      in.headFinders[i].runPass(this);
    out.println(methodBody);

    out.println("  public " + inName + "() { }");
    out.println("  public " + inName + "(" + in.head.getType() + " head)");
    out.println("  {");
    out.print("    super(head");
    if (in.algorithm.arguments.size() > 0)
      out.print(", " + in.algorithm.arguments);
    out.println(");");
    out.println("    constraint = new " + in.constraint.name
                + "().makeConstraint(head);");
    out.println("  }\n");

    out.println("  public String getHeadType() { return \""
                + in.head.getType().typeClass().getName() + "\"; }");
    out.println("  public String[] getHeadFinderTypes()");
    out.println("  {");
    out.print("    return new String[]{ \""
              + in.headFinders[0].argument.getType().typeClass().getName()
              + "\"");
    for (int i = 1; i < in.headFinders.length; ++i)
      out.print(", \""
                + in.headFinders[i].argument.getType().typeClass().getName()
                + "\"");
    out.println(" };");
    out.println("  }\n");

    out.println("  public Normalizer getNormalizer(Learner c)");
    out.println("  {");

    if (in.containsTypeSpecificNormalizer())
    {
      out.println("    Normalizer result = (Normalizer) normalizers.get(c);");
      out.println("    if (result == null)");
      out.println("      result = " + defaultNormalizer + ";");
      out.println("    return result;");
    }
    else out.println("    return " + defaultNormalizer + ";");

    out.println("  }");

    out.println("}\n");
    out.close();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param h  The node to process.
   **/
  public void run(InferenceDeclaration.HeadFinder h)
  {
    appendIndent();
    methodBody.append("public static ");
    methodBody.append(((InferenceDeclaration) currentCG).head.getType());
    methodBody.append(" findHead(" + h.argument + ")\n");
    ++indent;
    h.body.runPass(this);
    --indent;
    methodBody.append("\n\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param b  The node to process.
   **/
  public void run(Block b)
  {
    --indent;
    appendIndent();
    methodBody.append("{\n");

    ++indent;
    runOnChildren(b);
    methodBody.append("\n");
    --indent;

    appendIndent();
    methodBody.append("}");
    ++indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(StatementList l)
  {
    ASTNodeIterator I = l.iterator();
    if (!I.hasNext()) return;

    if (I.hasNext()) I.next().runPass(this);
    while (I.hasNext())
    {
      methodBody.append("\n");
      I.next().runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(AssertStatement s)
  {
    appendIndent();
    methodBody.append("assert ");
    s.condition.runPass(this);

    if (s.message != null)
    {
      methodBody.append(" : ");
      s.message.runPass(this);
    }

    methodBody.append(";");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(BreakStatement s)
  {
    appendIndent();
    methodBody.append("break");

    if (s.label != null)
    {
      methodBody.append(" ");
      methodBody.append(s.label);
    }

    methodBody.append(";");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ContinueStatement s)
  {
    appendIndent();
    methodBody.append("continue");

    if (s.label != null)
    {
      methodBody.append(" ");
      methodBody.append(s.label);
    }

    methodBody.append(";");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ExpressionStatement s)
  {
    if (s.expression instanceof ConstraintStatementExpression)
      s.expression.runPass(this);
    else
    {
      appendIndent();
      s.expression.runPass(this);
      methodBody.append(";");
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ForStatement s)
  {
    appendIndent();
    methodBody.append("for (");

    if (s.initializers != null)
    {
      s.initializers.runPass(this);
      methodBody.append("; ");
    }
    else if (s.initializer != null)
    {
      forInit = true;
      s.initializer.runPass(this);
      methodBody.append(" ");
      forInit = false;
    }
    else methodBody.append("; ");

    if (s.condition != null) s.condition.runPass(this);
    methodBody.append("; ");
    if (s.updaters != null) s.updaters.runPass(this);
    methodBody.append(")\n");
    ++indent;
    s.body.runPass(this);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(IfStatement s)
  {
    appendIndent();
    methodBody.append("if (");
    s.condition.runPass(this);
    methodBody.append(")\n");
    ++indent;
    s.thenClause.runPass(this);
    --indent;

    if (s.elseClause != null)
    {
      methodBody.append("\n");
      appendIndent();
      methodBody.append("else\n");
      ++indent;
      s.elseClause.runPass(this);
      --indent;
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(LabeledStatement s)
  {
    appendIndent();
    methodBody.append(s.label + ": ");
    s.statement.runPass(this);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ReturnStatement s)
  {
    appendIndent();

    if (currentCG instanceof CodedClassifier
        && ((CodedClassifier) currentCG).returnType.type
           == ClassifierReturnType.DISCRETE)
    {
      methodBody.append("return \"\" + (");
      s.expression.runPass(this);
      methodBody.append(")");
    }
    else
    {
      methodBody.append("return ");
      s.expression.runPass(this);
    }

    methodBody.append(";");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(SenseStatement s)
  {
    CodedClassifier currentCC = (CodedClassifier) currentCG;

    if (s.value instanceof MethodInvocation)
    {
      MethodInvocation m = (MethodInvocation) s.value;
      if (m.isClassifierInvocation)
      {
        ClassifierReturnType invokedType =
          ((ClassifierType) m.name.typeCache).getOutput();
        int t = invokedType.type;

        if ((currentCC.returnType.type
               == ClassifierReturnType.DISCRETE_GENERATOR
             || currentCC.returnType.type
                == ClassifierReturnType.REAL_GENERATOR)
            && (t == ClassifierReturnType.DISCRETE_GENERATOR
                || t == ClassifierReturnType.REAL_GENERATOR
                || t == ClassifierReturnType.DISCRETE_ARRAY
                || t == ClassifierReturnType.REAL_ARRAY))
        {
          appendIndent();
          methodBody.append("__id = this.name + (");
          s.name.runPass(this);
          methodBody.append(");\n");

          appendIndent();
          methodBody.append("{\n");
          ++indent;

          appendIndent();
          methodBody.append("FeatureVector __temp = ");
          s.value.runPass(this);
          methodBody.append(";\n");

          appendIndent();
          methodBody.append(
              "for (java.util.Iterator __I = __temp.iterator(); "
              + "__I.hasNext(); )\n");
          appendIndent();
          methodBody.append("{\n");
          ++indent;

          boolean isArray = t == ClassifierReturnType.DISCRETE_ARRAY
                            || t == ClassifierReturnType.REAL_ARRAY;

          if (currentCC.returnType.values.size() > 0
              && !currentCC.returnType.values.equals(invokedType.values))
          {
            String featureClass = "Discrete";
            if (isArray) featureClass += "Array";
            featureClass += "Feature";

            appendIndent();
            methodBody.append(featureClass);
            methodBody.append(" __f = (" + featureClass + ") __I.next();\n");

            appendIndent();
            methodBody.append("String __sensedPackage = __f.getPackage();\n");
            appendIndent();
            methodBody.append("String __sensedId = __f.getIdentifier();\n");
            appendIndent();
            methodBody.append("if (this.containingPackage != "
                              + "__sensedPackage)\n");
            ++indent;
            appendIndent();
            methodBody.append("__sensedId = __sensedPackage + \":\" + "
                              + "__sensedId;\n");
            --indent;

            appendIndent();
            methodBody.append("__result.addFeature(new ");
            methodBody.append(featureClass);
            methodBody.append("(this.containingPackage, __id + __sensedId, "
                              + "__f.getValue(), "
                              + "valueIndexOf(__f.getValue()), (short) ");
            methodBody.append(currentCC.returnType.values.size());
            if (isArray)
              methodBody.append(", __f.getArrayIndex(), "
                                + "__f.getArrayLength()");
            methodBody.append("));\n");
          }
          else
          {
            boolean isDiscrete = t == ClassifierReturnType.DISCRETE_GENERATOR
                                 || t == ClassifierReturnType.DISCRETE_ARRAY;
            String featureClass = isDiscrete ? "Discrete" : "Real";
            if (isArray) featureClass += "Array";
            featureClass += "Feature";

            appendIndent();
            methodBody.append(featureClass);
            methodBody.append(" __f = (");
            methodBody.append(featureClass);
            methodBody.append(") __I.next();\n");

            appendIndent();
            methodBody.append("String __sensedPackage = __f.getPackage();\n");
            appendIndent();
            methodBody.append("String __sensedId = __f.getIdentifier();\n");
            appendIndent();
            methodBody.append("if (this.containingPackage != "
                              + "__sensedPackage)\n");
            ++indent;
            appendIndent();
            methodBody.append("__sensedId = __sensedPackage + \":\" + "
                              + "__sensedId;\n");
            --indent;

            appendIndent();
            methodBody.append("__result.addFeature(new ");
            methodBody.append(featureClass);
            methodBody.append("(this.containingPackage, __id + __sensedId, "
                              + "__f.getValue()");
            if (isArray)
              methodBody.append(", __f.getArrayIndex(), "
                                + "__f.getArrayLength()");
            methodBody.append("));\n");
          }

          --indent;
          appendIndent();
          methodBody.append("}\n");

          --indent;
          appendIndent();
          methodBody.append("}");
          return;
        }
        else if ((currentCC.returnType.type
                    == ClassifierReturnType.DISCRETE_ARRAY
                  || currentCC.returnType.type
                     == ClassifierReturnType.REAL_ARRAY)
                 && (t == ClassifierReturnType.DISCRETE_ARRAY
                     || t == ClassifierReturnType.REAL_ARRAY))
        {
          appendIndent();
          methodBody.append("{\n");
          ++indent;

          appendIndent();
          methodBody.append("FeatureVector __temp = ");
          s.value.runPass(this);
          methodBody.append(";\n");

          appendIndent();
          methodBody.append(
              "for (java.util.Iterator __I = __temp.iterator(); "
              + "__I.hasNext(); )\n");
          appendIndent();
          methodBody.append("{\n");
          ++indent;

          if (currentCC.returnType.values.size() > 0
              && !currentCC.returnType.values.equals(invokedType.values))
          {
            String featureClass = "DiscreteArrayFeature";

            appendIndent();
            methodBody.append(featureClass);
            methodBody.append(" __f = (");
            methodBody.append(featureClass);
            methodBody.append(") __I.next();\n");

            appendIndent();
            methodBody.append("__result.addFeature(new ");
            methodBody.append(featureClass);
            methodBody.append("(this.containingPackage, this.name, "
                              + "__f.getValue(), "
                              + "valueIndexOf(__f.getValue()), (short) ");
            methodBody.append(currentCC.returnType.values.size());
            methodBody.append(", __featureIndex++, 0));\n");
          }
          else
          {
            boolean isDiscrete = t == ClassifierReturnType.DISCRETE_ARRAY;
            String featureClass = isDiscrete ? "Discrete" : "Real";
            featureClass += "ArrayFeature";

            appendIndent();
            methodBody.append(featureClass);
            methodBody.append(" __f = (");
            methodBody.append(featureClass);
            methodBody.append(") __I.next();\n");

            appendIndent();
            methodBody.append("__result.addFeature(new ");
            methodBody.append(featureClass);
            methodBody.append("(this.containingPackage, this.name, "
                              + "__f.getValue(), __featureIndex++, 0");
            methodBody.append("));\n");
          }

          --indent;
          appendIndent();
          methodBody.append("}\n");

          --indent;
          appendIndent();
          methodBody.append("}");
          return;
        }
      }
    }

    if (currentCC.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
        || currentCC.returnType.type == ClassifierReturnType.REAL_GENERATOR)
    {
      appendIndent();
      methodBody.append("__id = this.name + (");
      s.name.runPass(this);
      methodBody.append(");\n");
    }

    if (currentCC.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
        || currentCC.returnType.type == ClassifierReturnType.DISCRETE_ARRAY)
    {
      appendIndent();
      methodBody.append("__value = \"\" + (");
      s.value.runPass(this);
      methodBody.append(");\n");
    }

    appendIndent();
    methodBody.append("__result.addFeature(new ");
    if (s.name == null)
    {
      if (currentCC.returnType.type == ClassifierReturnType.DISCRETE_ARRAY)
        methodBody.append("DiscreteArray");
      else methodBody.append("RealArray");
      methodBody.append("Feature(this.containingPackage, this.name, ");
      if (currentCC.returnType.type == ClassifierReturnType.DISCRETE_ARRAY)
      {
        methodBody.append("__value");
        if (currentCC.returnType.values.size() > 0)
          methodBody.append(", valueIndexOf(__value), (short) "
                            + currentCC.returnType.values.size());
      }
      else s.value.runPass(this);
      methodBody.append(", __featureIndex++, 0");
    }
    else
    {
      if (currentCC.returnType.type == ClassifierReturnType.REAL_GENERATOR)
        methodBody.append("Real");
      else methodBody.append("Discrete");
      methodBody.append("Feature(this.containingPackage, __id, ");
      if (currentCC.returnType.type == ClassifierReturnType.REAL_GENERATOR)
        s.value.runPass(this);
      else
      {
        methodBody.append("__value");
        if (currentCC.returnType.values.size() > 0)
          methodBody.append(", valueIndexOf(__value), (short) "
                            + currentCC.returnType.values.size());
      }
    }

    methodBody.append("));");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(SwitchStatement s)
  {
    appendIndent();
    methodBody.append("switch (");
    s.expression.runPass(this);
    methodBody.append(")\n");
    s.block.runPass(this);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(SynchronizedStatement s)
  {
    appendIndent();
    methodBody.append("synchronized (");
    s.data.runPass(this);
    methodBody.append(")\n");
    ++indent;
    s.block.runPass(this);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ThrowStatement s)
  {
    appendIndent();
    methodBody.append("throw ");
    s.exception.runPass(this);
    methodBody.append(";\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(TryStatement s)
  {
    appendIndent();
    methodBody.append("try\n");
    ++indent;
    s.block.runPass(this);
    --indent;
    s.catchList.runPass(this);
    if (s.finallyBlock != null)
    {
      appendIndent();
      methodBody.append("finally\n");
      s.finallyBlock.runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(VariableDeclaration s)
  {
    if (!forInit) appendIndent();
    if (s.isFinal) methodBody.append("final ");
    s.type.runPass(this);

    ASTNodeIterator N = s.names.iterator();
    methodBody.append(" " + N.next());
    ExpressionList.ExpressionListIterator I = s.initializers.listIterator();
    Expression i = I.nextItem();
    if (i != null)
    {
      methodBody.append(" = ");
      i.runPass(this);
    }

    while (N.hasNext())
    {
      methodBody.append(", " + N.next());
      i = I.nextItem();
      if (i != null)
      {
        methodBody.append(" = ");
        i.runPass(this);
      }
    }

    methodBody.append(";");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(WhileStatement s)
  {
    appendIndent();
    methodBody.append("while (");
    s.condition.runPass(this);
    methodBody.append(")\n");
    ++indent;
    s.body.runPass(this);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(DoStatement s)
  {
    appendIndent();
    methodBody.append("do\n");
    ++indent;
    s.body.runPass(this);
    --indent;

    appendIndent();
    methodBody.append("while (");
    s.condition.runPass(this);
    methodBody.append(");\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(SwitchGroupList l)
  {
    ASTNodeIterator I = l.iterator();
    if (!I.hasNext()) return;

    I.next().runPass(this);
    while (I.hasNext())
    {
      methodBody.append("\n");
      I.next().runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param g  The node to process.
   **/
  public void run(SwitchGroup g)
  {
    appendIndent();
    g.labels.runPass(this);
    methodBody.append("\n");
    ++indent;
    g.statements.runPass(this);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(SwitchLabelList l)
  {
    ASTNodeIterator I = l.iterator();
    if (!I.hasNext()) return;

    I.next().runPass(this);
    while (I.hasNext())
    {
      methodBody.append(" ");
      I.next().runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(SwitchLabel l)
  {
    methodBody.append("case ");
    l.value.runPass(this);
    methodBody.append(":");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(CatchList l)
  {
    ASTNodeIterator I = l.iterator();
    if (!I.hasNext()) return;

    I.next().runPass(this);
    while (I.hasNext())
    {
      methodBody.append("\n");
      I.next().runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(CatchClause c)
  {
    appendIndent();
    methodBody.append("catch (");
    c.argument.runPass(this);
    methodBody.append(")\n");
    ++indent;
    c.block.runPass(this);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param a  The node to process.
   **/
  public void run(Argument a)
  {
    if (a.getFinal()) methodBody.append("final ");
    a.getType().runPass(this);
    methodBody.append(" " + a.getName());
  }


  /**
    * This method generates the code for a new temporary variable used when
    * translating constraints.
    *
    * @param name The name of the temporary variable.
   **/
  private void constraintTemporary(String name)
  {
    appendIndent();
    if (constraintMode) methodBody.append("FirstOrderConstraint ");
    else methodBody.append("boolean ");
    methodBody.append(name);
    if (constraintMode) methodBody.append(" = null;\n");
    else methodBody.append(";\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ConstraintStatementExpression e)
  {
    constraintResultNumber = 0;
    appendIndent();
    methodBody.append("{\n");
    ++indent;

    if (constraintMode && e.constraint.containsQuantifiedVariable())
    {
      StringBuffer buffer = new StringBuffer();
      int i = 0;
      HashSet referenced = e.constraint.getVariableTypes();
      for (Iterator I = referenced.iterator(); I.hasNext(); )
      {
        Argument a = (Argument) I.next();
        LBJ2.IR.Type t = a.getType();
        if (t.quantifierArgumentType) continue;

        for (int j = 0; j < indent; ++j) buffer.append("  ");
        buffer.append("LBJ$constraint$context[");
        buffer.append(i);
        buffer.append("] = ");
        if (t instanceof PrimitiveType)
        {
          String primitiveTypeName = null;
          if (((PrimitiveType) t).type == PrimitiveType.INT)
            primitiveTypeName = "Integer";
          else
          {
            primitiveTypeName = t.toString();
            primitiveTypeName =
              Character.toUpperCase(primitiveTypeName.charAt(0))
              + primitiveTypeName.substring(1);
          }

          buffer.append("new ");
          buffer.append(primitiveTypeName);
          buffer.append("(");
        }

        buffer.append(a.getName());
        if (t instanceof PrimitiveType) buffer.append(")");
        buffer.append(";\n");

        contextVariables.put(a.getName(), new Integer(i++));
      }

      appendIndent();
      methodBody.append("Object[] LBJ$constraint$context = new Object[");
      methodBody.append(i);
      methodBody.append("];\n");
      methodBody.append(buffer);
    }

    String childResultName = constraintResult + constraintResultNumber;
    constraintResultName = childResultName;
    constraintTemporary(childResultName);
    quantifierNesting = 0;

    e.constraint.runPass(this);

    appendIndent();
    if (constraintMode)
    {
      methodBody.append("__result = new FirstOrderConjunction(__result, ");
      methodBody.append(childResultName);
      methodBody.append(");\n");
    }
    else
    {
      methodBody.append("if (!");
      methodBody.append(childResultName);
      methodBody.append(") return \"false\";\n");
    }

    --indent;
    appendIndent();
    methodBody.append("}");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(BinaryConstraintExpression e)
  {
    String myResultName = constraintResultName;
    String leftResultName = constraintResult + ++constraintResultNumber;

    appendIndent();
    methodBody.append("{\n");
    ++indent;

    constraintTemporary(leftResultName);
    constraintResultName = leftResultName;
    e.left.runPass(this);

    if (constraintMode
        || e.operation.operation == Operator.DOUBLE_IMPLICATION)
    {
      String rightResultName = constraintResult + ++constraintResultNumber;
      constraintTemporary(rightResultName);
      constraintResultName = rightResultName;
      e.right.runPass(this);

      appendIndent();
      methodBody.append(myResultName);

      if (constraintMode)
      {
        methodBody.append(" = new FirstOrder");
        if (e.operation.operation == Operator.LOGICAL_CONJUNCTION)
          methodBody.append("Conjunction");
        else if (e.operation.operation == Operator.LOGICAL_DISJUNCTION)
          methodBody.append("Disjunction");
        else if (e.operation.operation == Operator.IMPLICATION)
          methodBody.append("Implication");
        else methodBody.append("DoubleImplication");

        methodBody.append("(");
        methodBody.append(leftResultName);
        methodBody.append(", ");
        methodBody.append(rightResultName);
        methodBody.append(");\n");
      }
      else
      {
        methodBody.append(" = ");
        methodBody.append(leftResultName);
        methodBody.append(" == ");
        methodBody.append(rightResultName);
        methodBody.append(";\n");
      }
    }
    else
    {
      appendIndent();
      methodBody.append("if (");
      if (e.operation.operation == Operator.LOGICAL_DISJUNCTION)
        methodBody.append("!");
      methodBody.append(leftResultName);
      methodBody.append(")\n");
      ++indent;

      constraintResultName = myResultName;
      e.right.runPass(this);

      --indent;
      appendIndent();
      methodBody.append("else ");
      methodBody.append(myResultName);
      methodBody.append(" = ");
      methodBody.append(e.operation.operation == Operator.LOGICAL_DISJUNCTION
                        || e.operation.operation == Operator.IMPLICATION);
      methodBody.append(";\n");
    }

    --indent;
    appendIndent();
    methodBody.append("}\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(NegatedConstraintExpression e)
  {
    String myResultName = constraintResultName;
    String childResultName = constraintResult + ++constraintResultNumber;

    appendIndent();
    methodBody.append("{\n");
    ++indent;

    constraintTemporary(childResultName);
    constraintResultName = childResultName;
    e.constraint.runPass(this);

    appendIndent();
    methodBody.append(myResultName);
    methodBody.append(" = ");
    if (constraintMode) methodBody.append("new FirstOrderNegation(");
    else methodBody.append("!");
    methodBody.append(childResultName);
    if (constraintMode) methodBody.append(")");
    methodBody.append(";\n");

    --indent;
    appendIndent();
    methodBody.append("}\n");
  }


  /**
    * Generates the code necessary at the top of a replacer method
    * implementation to declare the variables that will be used in the method.
    *
    * @param expression The expression to be evaluted in the replacer method.
   **/
  private void generateReplacerMethodEnvironment(Expression expression)
  {
    for (Iterator I = expression.getVariableTypes().iterator(); I.hasNext(); )
    {
      Argument a = (Argument) I.next();
      LBJ2.IR.Type type = a.getType();
      String primitiveTypeName = null;
      if (type instanceof PrimitiveType)
      {
        if (((PrimitiveType) type).type == PrimitiveType.INT)
          primitiveTypeName = "Integer";
        else
        {
          primitiveTypeName = type.toString();
          primitiveTypeName =
            Character.toUpperCase(primitiveTypeName.charAt(0))
            + primitiveTypeName.substring(1);
        }
      }

      appendIndent();
      a.runPass(this);
      methodBody.append(" = (");
      if (primitiveTypeName == null) type.runPass(this);
      else methodBody.append("(" + primitiveTypeName);
      methodBody.append(") ");

      if (type.quantifierArgumentType)
      {
        methodBody.append("quantificationVariables.get(");
        methodBody.append(
            ((Integer) quantificationVariables.get(a.getName()))
            .intValue());
        methodBody.append(")");
      }
      else
      {
        methodBody.append("context[");
        methodBody.append(
            ((Integer) contextVariables.get(a.getName())).intValue());
        methodBody.append("]");
      }

      if (primitiveTypeName != null)
        methodBody.append(")." + type + "Value()");
      methodBody.append(";\n");
    }
  }


  /**
    * Translates an expression from a quantified
    * {@link ConstraintEqualityExpression} into the appropriate method of an
    * {@link EqualityArgumentReplacer}.
    *
    * @param right              Indicates if <code>expression</code> comes
    *                           from the right hand side of the equality.
    * @param expression         The expression.
    * @param isDiscreteLearner  This flag is set if <code>expression</code>
    *                           represents a variable.
   **/
  private void generateEARMethod(boolean right, Expression expression,
                                 boolean isDiscreteLearner)
  {
    appendIndent();
    methodBody.append("public ");
    methodBody.append(isDiscreteLearner ? "Object" : "String");
    methodBody.append(" get");
    methodBody.append(right ? "Right" : "Left");
    methodBody.append(isDiscreteLearner ? "Object" : "Value");
    methodBody.append("()\n");

    appendIndent();
    methodBody.append("{\n");
    ++indent;

    generateReplacerMethodEnvironment(expression);

    appendIndent();
    methodBody.append("return ");
    if (isDiscreteLearner)
      ((MethodInvocation) expression).arguments.runPass(this);
    else
    {
      methodBody.append("\"\" + (");
      expression.runPass(this);
      methodBody.append(")");
    }

    methodBody.append(";\n");

    --indent;
    appendIndent();
    methodBody.append("}\n");
  }


  /**
    * Translates an unquantified expression not representing a first order
    * variable from a {@link ConstraintEqualityExpression} into an argument of
    * a {@link FirstOrderEquality}.
    *
    * @param left               This flag is set if <code>expression</code>
    *                           came from the left hand side of the equality.
    * @param expression         The expression.
   **/
  private void generateNotVariable(boolean left, Expression expression)
  {
    if (left) methodBody.append("(");
    methodBody.append("\"\" + (");
    expression.runPass(this);
    methodBody.append(")");
    if (left) methodBody.append(")");
  }


  /**
    * Translates an expression representing a first order variable from a
    * {@link ConstraintEqualityExpression} into an argument of a
    * {@link FirstOrderEquality}.
    *
    * @param expression         The expression.
    * @param isQuantified       This flag is set if <code>expression</code>
    *                           contains a quantified variable.
   **/
  private void generateVariable(Expression expression, boolean isQuantified)
  {
    MethodInvocation method = (MethodInvocation) expression;
    methodBody.append("new FirstOrderVariable(");
    methodBody.append(("__" + method.name).replace('.', '$'));

    if (isQuantified) methodBody.append(", null)");
    else
    {
      methodBody.append(", ");
      method.arguments.runPass(this);
      methodBody.append(")");
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ConstraintEqualityExpression e)
  {
    String myResultName = constraintResultName;

    boolean leftIsDiscreteLearner = e.leftIsDiscreteLearner;
    boolean rightIsDiscreteLearner = e.rightIsDiscreteLearner;
    boolean leftIsQuantified = e.leftIsQuantified;
    boolean rightIsQuantified = e.rightIsQuantified;
    Expression left = e.left;
    Expression right = e.right;

    if (!leftIsDiscreteLearner && rightIsDiscreteLearner)
    {
      leftIsDiscreteLearner = true;
      rightIsDiscreteLearner = false;

      leftIsQuantified ^= rightIsQuantified;
      rightIsQuantified ^= leftIsQuantified;
      leftIsQuantified ^= rightIsQuantified;

      Expression temp = left;
      left = right;
      right = temp;
    }

    if (!(constraintMode && (leftIsQuantified || rightIsQuantified)))
    {
      appendIndent();
      methodBody.append(myResultName);
      methodBody.append(" = ");

      if (constraintMode)
      {
        methodBody.append("new FirstOrder");

        if (leftIsDiscreteLearner)
        {
          if (rightIsDiscreteLearner)
            methodBody.append("EqualityWithVariable");
          else methodBody.append("EqualityWithValue");
          methodBody.append("(");

          methodBody.append(e.operation.operation
                            == Operator.CONSTRAINT_EQUAL);
          methodBody.append(", ");
          generateVariable(left, false);
          methodBody.append(", ");
          if (rightIsDiscreteLearner) generateVariable(right, false);
          else generateNotVariable(false, right);

          methodBody.append(");\n");
          return;
        }

        methodBody.append("Constant(");
      }

      if (e.operation.operation == Operator.CONSTRAINT_NOT_EQUAL)
        methodBody.append("!");
      generateNotVariable(true, left);
      methodBody.append(".equals(");
      generateNotVariable(false, right);
      methodBody.append(")");

      if (constraintMode) methodBody.append(")");
      methodBody.append(";\n");
      return;
    }

    appendIndent();
    methodBody.append("{\n");
    ++indent;

    appendIndent();
    methodBody.append("EqualityArgumentReplacer LBJ$EAR =\n");
    ++indent;

    appendIndent();
    methodBody.append("new EqualityArgumentReplacer(LBJ$constraint$context");
    if (!(leftIsQuantified && rightIsQuantified))
    {
      methodBody.append(", ");
      methodBody.append(leftIsQuantified);
    }

    methodBody.append(")\n");
    appendIndent();
    methodBody.append("{\n");
    ++indent;

    if (leftIsQuantified)
      generateEARMethod(false, left, leftIsDiscreteLearner);

    if (rightIsQuantified)
    {
      if (leftIsQuantified) methodBody.append("\n");
      generateEARMethod(true, right, rightIsDiscreteLearner);
    }

    --indent;
    appendIndent();
    methodBody.append("};\n");
    --indent;

    appendIndent();
    methodBody.append(myResultName);
    methodBody.append(" = new FirstOrderEquality");
    if (leftIsDiscreteLearner)
    {
      if (rightIsDiscreteLearner) methodBody.append("WithVariable");
      else methodBody.append("WithValue");
    }
    else methodBody.append("TwoValues");

    methodBody.append("(");
    methodBody.append(e.operation.operation == Operator.CONSTRAINT_EQUAL);
    methodBody.append(", ");
    if (leftIsDiscreteLearner) generateVariable(left, leftIsQuantified);
    else if (leftIsQuantified) methodBody.append("null");
    else generateNotVariable(true, left);
    methodBody.append(", ");
    if (rightIsDiscreteLearner) generateVariable(right, rightIsQuantified);
    else if (rightIsQuantified) methodBody.append("null");
    else generateNotVariable(false, right);
    methodBody.append(", LBJ$EAR);\n");

    --indent;
    appendIndent();
    methodBody.append("}\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ConstraintInvocation e)
  {
    String myResultName = constraintResultName;

    if (!(constraintMode && e.invocationIsQuantified))
    {
      appendIndent();
      methodBody.append(myResultName);
      methodBody.append((" = __" + e.invocation.name).replace('.', '$'));
      if (constraintMode) methodBody.append(".makeConstraint(");
      else methodBody.append(".discreteValue(");
      e.invocation.arguments.runPass(this);
      methodBody.append(")");
      if (!constraintMode) methodBody.append(".equals(\"true\")");
      methodBody.append(";\n");
      return;
    }

    appendIndent();
    methodBody.append("{\n");
    ++indent;

    appendIndent();
    methodBody.append("InvocationArgumentReplacer LBJ$IAR =\n");
    ++indent;

    appendIndent();
    methodBody.append(
        "new InvocationArgumentReplacer(LBJ$constraint$context)\n");
    appendIndent();
    methodBody.append("{\n");
    ++indent;

    appendIndent();
    methodBody.append("public Object compute()\n");
    appendIndent();
    methodBody.append("{\n");
    ++indent;

    Expression argument = e.invocation.arguments.listIterator().nextItem();
    generateReplacerMethodEnvironment(argument);

    appendIndent();
    methodBody.append("return ");
    argument.runPass(this);
    methodBody.append(";\n");

    --indent;
    appendIndent();
    methodBody.append("}\n");

    --indent;
    appendIndent();
    methodBody.append("};\n");
    --indent;

    appendIndent();
    methodBody.append(myResultName);
    methodBody.append(" = new QuantifiedConstraintInvocation(");
    methodBody.append(("__" + e.invocation.name).replace('.', '$'));
    methodBody.append(", LBJ$IAR);\n");

    --indent;
    appendIndent();
    methodBody.append("}\n");
  }


  /**
    * {@link UniversalQuantifierExpression}s and
    * {@link ExistentialQuantifierExpression}s generate their code through
    * this method.
    *
    * @param e  The node to process.
   **/
  private void generateSimpleQuantifier(QuantifiedConstraintExpression e)
  {
    boolean universal = e instanceof UniversalQuantifierExpression;
    String myResultName = constraintResultName;

    if (!constraintMode)
    {
      String inductionVariable = "__I" + quantifierNesting;

      appendIndent();
      methodBody.append("{\n");
      ++indent;

      appendIndent();
      methodBody.append(myResultName);
      methodBody.append(" = ");
      methodBody.append(universal);
      methodBody.append(";\n");

      appendIndent();
      methodBody.append("for (java.util.Iterator ");
      methodBody.append(inductionVariable);
      methodBody.append(" = (");
      e.collection.runPass(this);
      methodBody.append(").iterator(); ");
      methodBody.append(inductionVariable);
      methodBody.append(".hasNext() && ");
      if (!universal) methodBody.append("!");
      methodBody.append(myResultName);
      methodBody.append("; )\n");

      appendIndent();
      methodBody.append("{\n");
      ++indent;

      appendIndent();
      e.argument.runPass(this);
      methodBody.append(" = (");
      e.argument.getType().runPass(this);
      methodBody.append(") ");
      methodBody.append(inductionVariable);
      methodBody.append(".next();\n");

      ++quantifierNesting;
      e.constraint.runPass(this);
      --quantifierNesting;

      --indent;
      appendIndent();
      methodBody.append("}\n");

      --indent;
      appendIndent();
      methodBody.append("}\n");
      return;
    }

    appendIndent();
    methodBody.append("{\n");
    ++indent;

    String childResultName = constraintResult + ++constraintResultNumber;
    constraintTemporary(childResultName);
    constraintResultName = childResultName;

    quantificationVariables.put(e.argument.getName(),
                                new Integer(quantifierNesting++));
    e.constraint.runPass(this);
    --quantifierNesting;

    if (!e.collectionIsQuantified)
    {
      appendIndent();
      methodBody.append(myResultName);
      methodBody.append(" = new ");
      if (universal) methodBody.append("Universal");
      else methodBody.append("Existential");
      methodBody.append("Quantifier(\"");
      methodBody.append(e.argument.getName());
      methodBody.append("\", ");
      e.collection.runPass(this);
      methodBody.append(", ");
      methodBody.append(childResultName);
      methodBody.append(");\n");
    }
    else
    {
      appendIndent();
      methodBody.append("QuantifierArgumentReplacer LBJ$QAR =\n");
      ++indent;

      appendIndent();
      methodBody.append(
          "new QuantifierArgumentReplacer(LBJ$constraint$context)\n");
      appendIndent();
      methodBody.append("{\n");
      ++indent;

      appendIndent();
      methodBody.append("public java.util.Collection getCollection()\n");
      appendIndent();
      methodBody.append("{\n");
      ++indent;

      generateReplacerMethodEnvironment(e.collection);

      appendIndent();
      methodBody.append("return ");
      e.collection.runPass(this);
      methodBody.append(";\n");

      --indent;
      appendIndent();
      methodBody.append("}\n");

      --indent;
      appendIndent();
      methodBody.append("};\n");
      --indent;

      appendIndent();
      methodBody.append(myResultName);
      methodBody.append(" = new ");
      if (universal) methodBody.append("Universal");
      else methodBody.append("Existential");
      methodBody.append("Quantifier(\"");
      methodBody.append(e.argument.getName());
      methodBody.append("\", null, ");
      methodBody.append(childResultName);
      methodBody.append(", LBJ$QAR);\n");
    }

    --indent;
    appendIndent();
    methodBody.append("}\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(UniversalQuantifierExpression e)
  {
    generateSimpleQuantifier(e);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ExistentialQuantifierExpression e)
  {
    generateSimpleQuantifier(e);
  }


  /**
    * {@link AtLeastQuantifierExpression}s and
    * {@link AtMostQuantifierExpression}s generate their code through this
    * method.
    *
    * @param e  The node to process.
   **/
  public void generateBoundedQuantifier(QuantifiedConstraintExpression e)
  {
    boolean atleast = e instanceof AtLeastQuantifierExpression;
    AtLeastQuantifierExpression ale = null;
    AtMostQuantifierExpression ame = null;
    if (atleast) ale = (AtLeastQuantifierExpression) e;
    else ame = (AtMostQuantifierExpression) e;

    String myResultName = constraintResultName;
    String childResultName = constraintResult + ++constraintResultNumber;

    if (!constraintMode)
    {
      appendIndent();
      methodBody.append("{\n");
      ++indent;

      String m = "LBJ$m$" + quantifierNesting;
      String bound = "LBJ$bound$" + quantifierNesting;

      appendIndent();
      methodBody.append("int ");
      methodBody.append(m);
      methodBody.append(" = 0;\n");
      appendIndent();
      methodBody.append("int ");
      methodBody.append(bound);
      methodBody.append(" = ");
      if (atleast) ale.lowerBound.runPass(this);
      else ame.upperBound.runPass(this);
      methodBody.append(";\n");

      String inductionVariable = "__I" + quantifierNesting;

      appendIndent();
      methodBody.append("for (java.util.Iterator ");
      methodBody.append(inductionVariable);
      methodBody.append(" = (");
      e.collection.runPass(this);
      methodBody.append(").iterator(); ");
      methodBody.append(inductionVariable);
      methodBody.append(".hasNext() && ");
      methodBody.append(m);
      if (atleast) methodBody.append(" < ");
      else methodBody.append(" <= ");
      methodBody.append(bound);
      methodBody.append("; )\n");

      appendIndent();
      methodBody.append("{\n");
      ++indent;

      appendIndent();
      e.argument.runPass(this);
      methodBody.append(" = (");
      e.argument.getType().runPass(this);
      methodBody.append(") ");
      methodBody.append(inductionVariable);
      methodBody.append(".next();\n");

      constraintTemporary(childResultName);
      constraintResultName = childResultName;
      ++quantifierNesting;
      e.constraint.runPass(this);
      --quantifierNesting;

      appendIndent();
      methodBody.append("if (");
      methodBody.append(childResultName);
      methodBody.append(") ++");
      methodBody.append(m);
      methodBody.append(";\n");

      --indent;
      appendIndent();
      methodBody.append("}\n");

      appendIndent();
      methodBody.append(myResultName);
      methodBody.append(" = ");
      methodBody.append(m);
      if (atleast) methodBody.append(" >= ");
      else methodBody.append(" <= ");
      methodBody.append(bound);
      methodBody.append(";\n");

      --indent;
      appendIndent();
      methodBody.append("}\n");
      return;
    }

    appendIndent();
    methodBody.append("{\n");
    ++indent;

    constraintTemporary(childResultName);
    constraintResultName = childResultName;
    quantificationVariables.put(e.argument.getName(),
                                new Integer(quantifierNesting++));
    e.constraint.runPass(this);
    --quantifierNesting;

    if (!(e.collectionIsQuantified
          || atleast && ale.lowerBoundIsQuantified
          || !atleast && ame.upperBoundIsQuantified))
    {
      appendIndent();
      methodBody.append(myResultName);
      methodBody.append(" = new ");
      if (atleast) methodBody.append("AtLeast");
      else methodBody.append("AtMost");
      methodBody.append("Quantifier(\"");
      methodBody.append(e.argument.getName());
      methodBody.append("\", ");
      e.collection.runPass(this);
      methodBody.append(", ");
      methodBody.append(childResultName);
      methodBody.append(", ");
      if (atleast) methodBody.append(ale.lowerBound);
      else methodBody.append(ame.upperBound);
      methodBody.append(");\n");
    }
    else
    {
      appendIndent();
      methodBody.append("QuantifierArgumentReplacer LBJ$QAR =\n");
      ++indent;

      appendIndent();
      methodBody.append(
          "new QuantifierArgumentReplacer(LBJ$constraint$context");
      if (!(e.collectionIsQuantified
            && (atleast && ale.lowerBoundIsQuantified
                || !atleast && ame.upperBoundIsQuantified)))
      {
        methodBody.append(", ");
        methodBody.append(e.collectionIsQuantified);
      }

      methodBody.append(")\n");

      appendIndent();
      methodBody.append("{\n");
      ++indent;

      if (e.collectionIsQuantified)
      {
        appendIndent();
        methodBody.append("public Collection getCollection()\n");
        appendIndent();
        methodBody.append("{\n");
        ++indent;

        generateReplacerMethodEnvironment(e.collection);

        appendIndent();
        methodBody.append("return ");
        e.collection.runPass(this);
        methodBody.append(";\n");

        --indent;
        appendIndent();
        methodBody.append("}\n");
      }

      if (atleast && ale.lowerBoundIsQuantified
          || !atleast && ame.upperBoundIsQuantified)
      {
        if (e.collectionIsQuantified) methodBody.append("\n");
        appendIndent();
        methodBody.append("public int getBound()\n");
        appendIndent();
        methodBody.append("{\n");
        ++indent;

        if (atleast) generateReplacerMethodEnvironment(ale.lowerBound);
        else generateReplacerMethodEnvironment(ame.upperBound);

        appendIndent();
        methodBody.append("return ");
        if (atleast) ale.lowerBound.runPass(this);
        else ame.upperBound.runPass(this);
        methodBody.append(";\n");

        --indent;
        appendIndent();
        methodBody.append("}\n");
      }

      --indent;
      appendIndent();
      methodBody.append("};\n");
      --indent;

      appendIndent();
      methodBody.append(myResultName);
      methodBody.append(" = new ");
      if (atleast) methodBody.append("AtLeast");
      else methodBody.append("AtMost");
      methodBody.append("Quantifier(\"");
      methodBody.append(e.argument.getName());
      methodBody.append("\", ");
      if (e.collectionIsQuantified) methodBody.append("null");
      else e.collection.runPass(this);
      methodBody.append(", ");
      methodBody.append(childResultName);
      methodBody.append(", ");
      if (atleast && ale.lowerBoundIsQuantified
          || !atleast && ame.upperBoundIsQuantified)
        methodBody.append("0");
      else if (atleast) ale.lowerBound.runPass(this);
      else ame.upperBound.runPass(this);
      methodBody.append(", LBJ$QAR);\n");
    }

    --indent;
    appendIndent();
    methodBody.append("}\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(AtLeastQuantifierExpression e)
  {
    generateBoundedQuantifier(e);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(AtMostQuantifierExpression e)
  {
    generateBoundedQuantifier(e);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(ExpressionList l)
  {
    ASTNodeIterator I = l.iterator();
    if (!I.hasNext()) return;

    I.next().runPass(this);
    while (I.hasNext())
    {
      methodBody.append(", ");
      I.next().runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ArrayCreationExpression e)
  {
    if (e.parenthesized) methodBody.append("(");

    methodBody.append("new ");
    e.elementType.runPass(this);

    int d = 0;
    for (ASTNodeIterator I = e.sizes.iterator(); I.hasNext(); ++d)
    {
      methodBody.append("[");
      I.next().runPass(this);
      methodBody.append("]");
    }

    for (; d < e.dimensions; ++d) methodBody.append("[]");

    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ArrayInitializer e)
  {
    if (e.parenthesized) methodBody.append("(");
    methodBody.append("{ ");
    e.values.runPass(this);
    methodBody.append(" }");
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(CastExpression e)
  {
    if (e.parenthesized) methodBody.append("(");
    methodBody.append("(");
    e.type.runPass(this);
    methodBody.append(") ");
    e.expression.runPass(this);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(Conditional e)
  {
    if (e.parenthesized) methodBody.append("(");
    e.condition.runPass(this);
    methodBody.append(" ? ");
    e.thenClause.runPass(this);
    methodBody.append(" : ");
    e.elseClause.runPass(this);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(Constant e)
  {
    if (e.parenthesized) methodBody.append("(");
    methodBody.append(e.value);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(InstanceofExpression e)
  {
    if (e.parenthesized) methodBody.append("(");
    e.left.runPass(this);
    methodBody.append(" instanceof ");
    e.right.runPass(this);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(Assignment e)
  {
    if (e.parenthesized) methodBody.append("(");
    e.left.runPass(this);
    methodBody.append(" " + e.operation + " ");
    e.right.runPass(this);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(IncrementExpression e)
  {
    if (e.parenthesized) methodBody.append("(");
    runOnChildren(e);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(InstanceCreationExpression e)
  {
    if (e.parenthesized) methodBody.append("(");

    if (e.parentObject != null)
    {
      e.parentObject.runPass(this);
      methodBody.append(".");
    }

    methodBody.append("new ");
    e.name.runPass(this);
    methodBody.append("(");
    e.arguments.runPass(this);
    methodBody.append(")");

    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(MethodInvocation e)
  {
    if (e.parenthesized) methodBody.append("(");

    if (e.isClassifierInvocation)
    {
      methodBody.append(("__" + e.name).replace('.', '$') + ".");

      ClassifierType invokedType = (ClassifierType) e.name.typeCache;
      int t = invokedType.getOutput().type;
      if (t == ClassifierReturnType.DISCRETE)
        methodBody.append("discreteValue(");
      else if (t == ClassifierReturnType.REAL)
        methodBody.append("realValue(");
      else if (!e.isSensedValue)
      {
        if (t == ClassifierReturnType.DISCRETE_ARRAY)
          methodBody.append("discreteValueArray(");
        else if (t == ClassifierReturnType.REAL_ARRAY)
          methodBody.append("realValueArray(");
      }
      else methodBody.append("classify(");

      if (invokedType.getInput() instanceof ArrayType)
        methodBody.append("(Object) ");
      e.arguments.runPass(this);
      methodBody.append(")");
    }
    else
    {
      if (e.parentObject != null)
      {
        e.parentObject.runPass(this);
        methodBody.append(".");
      }

      e.name.runPass(this);
      methodBody.append("(");
      e.arguments.runPass(this);
      methodBody.append(")");
    }

    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param be The node to process.
   **/
  public void run(BinaryExpression be)
  {
    if (be.parenthesized) methodBody.append("(");
    be.left.runPass(this);
    methodBody.append(" " + be.operation + " ");
    be.right.runPass(this);
    if (be.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(UnaryExpression e)
  {
    if (e.parenthesized) methodBody.append("(");
    runOnChildren(e);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(FieldAccess e)
  {
    if (e.parenthesized) methodBody.append("(");
    e.object.runPass(this);
    methodBody.append("." + e.name);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(SubscriptVariable e)
  {
    if (e.parenthesized) methodBody.append("(");
    e.array.runPass(this);
    methodBody.append("[");
    e.subscript.runPass(this);
    methodBody.append("]");
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param n  The node to process.
   **/
  public void run(Name n)
  {
    if (n.parenthesized) methodBody.append("(");
    methodBody.append(n);
    if (n.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param t  The node to process.
   **/
  public void run(ArrayType t)
  {
    t.type.runPass(this);
    methodBody.append("[]");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param t  The node to process.
   **/
  public void run(PrimitiveType t) { methodBody.append(t); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param t  The node to process.
   **/
  public void run(ReferenceType t) { methodBody.append(t); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param o  The node to process.
   **/
  public void run(Operator o) { methodBody.append(o); }
}

