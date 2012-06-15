package LBJ2;

import java.util.*;
import LBJ2.IR.*;
import LBJ2.classify.Classifier;
import LBJ2.learn.*;
import LBJ2.infer.*;
import LBJ2.parse.Parser;


/**
  * The <code>SemanticAnalysis</code> pass builds useful tables, computes
  * classifier types and other useful information, and generally checks that
  * things appear only where they are expected.  More specifically, the
  * following data is arranged:
  *
  * <table cellspacing=8>
  *   <tr valign=top>
  *     <td align=right>A1</td>
  *     <td>
  *       The global symbol table is built.  It stores information about
  *       classifier, constraint, and inference declarations as well as
  *       symbols local to method bodies.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A2</td>
  *     <td>
  *       The classifier representation table is built.  It stores references
  *       to internal representations of source code implementing classifiers
  *       indexed by the classifiers' names.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A3</td>
  *     <td>
  *       Names for every {@link ClassifierExpression} are computed.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A4</td>
  *     <td>
  *       Type information is computed for classifiers,
  *       {@link InstanceCreationExpression}s creating outer classes, and
  *       {@link Name}s known to refer to classifiers, the latter two only to
  *       support the semantic checks performed over the various classifier
  *       specification syntaxes.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A5</td>
  *     <td>
  *       Method invocations that are actually classifier invocations are
  *       marked as such.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A6</td>
  *     <td>
  *       If a <code>sense</code> statement with a single argument appears in
  *       a generator, the argument expression is moved from the
  *       {@link SenseStatement#value value} to the
  *       {@link SenseStatement#name name} variable in the
  *       {@link SenseStatement} object, and the
  *       {@link SenseStatement#value value} variable gets a new
  *       {@link Constant} representing <code>"true"</code> if the generator
  *       is discrete and <code>"1"</code> if the generator is real.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A7</td>
  *     <td>
  *       If there are any <code>for</code>, <code>if</code>,
  *       <code>while</code>, or <code>do</code> statements that contain a
  *       single statement in their body, that statement is wrapped in a
  *       {@link Block}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A8</td>
  *     <td>
  *       The dependor graph, linking the names of {@link CodeGenerator}s with
  *       the names of other {@link CodeGenerator}s that depend on them, is
  *       built for use by {@link RevisionAnalysis}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A9</td>
  *     <td>
  *       The invoked graph, linking the names of {@link CodeGenerator}s with
  *       the names of other {@link CodeGenerator}s that are invoked by them,
  *       is built for use by {@link TranslateToJava}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A10</td>
  *     <td>
  *       If a method of the unique instance of a learning classifier is
  *       invoked using the learning classifier's name,
  *       <code>.getInstance()</code> is inserted.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A11</td>
  *     <td>
  *       If a {@link LearningClassifierExpression} does not have a
  *       <code>with</code> clause, the default learning algorithm is
  *       substituted.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A12</td>
  *     <td>
  *       Flags are set in each {@link ConstraintEqualityExpression}
  *       indicating if its subexpressions are learner invocations.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A13</td>
  *     <td>
  *       {@link Name}s and every {@link ASTNode} that represents a new local
  *       scope gets a link to the symbol table representing its scope.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A14</td>
  *     <td>
  *       {@link Argument} types in arguments of quantifier expressions are
  *       marked as such.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A15</td>
  *     <td>
  *       Quantified {@link ConstraintEqualityExpression}s,
  *       {@link ConstraintInvocation}s, and
  *       {@link QuantifiedConstraintExpression}s are marked as such.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A16</td>
  *     <td>
  *       If a {@link InferenceDeclaration} does not have a <code>with</code>
  *       clause, the default inference algorithm is substituted.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A17</td>
  *     <td>
  *       When a {@link ClassifierName} is not alone on the right hand side of
  *       a {@link ClassifierAssignment}, its {@link ClassifierName#name name}
  *       is set equal to its {@link ClassifierName#referent referent}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A18</td>
  *     <td>
  *       The {@link ClassifierExpression#cacheIn} member variable is set when
  *       the containing {@link ClassifierAssignment} had a
  *       <code>cached</code> or <code>cachedin</code> modifier.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A19</td>
  *     <td>
  *       The {@link ClassifierExpression#comment} field of each top level
  *       classifier expression is set to the comment of the containing
  *       {@link ClassifierAssignment}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A20</td>
  *     <td>
  *       When a <code>with<code> clause is specified with an
  *       {@link InstanceCreationExpression} as an argument,
  *       {@link LearningClassifierExpression#learnerName} is set to the name
  *       of the class instantiated.
  *     </td>
  *   </tr>
  * </table>
  *
  * <p> And the following conditions are checked for:
  *
  * <table cellspacing=8>
  *   <tr valign=top>
  *     <td align=right>B1</td>
  *     <td>No named classifier is defined more than once.</td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B2</td>
  *     <td>
  *       Classifier and constraint invocations can only contain a single
  *       argument.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B3</td>
  *     <td>
  *       The output type of every classifier expression is checked for
  *       appropriateness in its context.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B4</td>
  *     <td>
  *       The input type of a {@link ClassifierName} is checked for
  *       appropriateness in its context.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B5</td>
  *     <td>
  *       The {@link InstanceCreationExpression} in the <code>from</code>
  *       clause of a {@link LearningClassifierExpression} instantiates a
  *       {@link Parser}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B6</td>
  *     <td>
  *       The {@link InstanceCreationExpression} in the <code>with</code>
  *       clause of a {@link LearningClassifierExpression} instantiates a
  *       {@link Learner}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B7</td>
  *     <td>
  *       The {@link Learner} specified in a
  *       {@link LearningClassifierExpression} must have input type
  *       assignable from the learning classifier expression's input type.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B8</td>
  *     <td>
  *       Classifiers with feature type <code>discrete</code>,
  *       <code>real</code>, or arrays of those may be invoked as if they were
  *       methods in any context.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B9</td>
  *     <td>
  *       Any classifier other than one of feature return type
  *       <code>mixed%</code> may be invoked as a method when that invocation
  *       is the value argument of a <code>sense</code> statement inside a
  *       generator of the same basic type (<code>discrete</code> or
  *       <code>real</code>).  Generators may not be invoked in any other
  *       context.  Array producing classifiers may also be invoked as the
  *       only argument of a <code>sense</code> statement inside another array
  *       producing classifier of the same basic type.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B10</td>
  *     <td>
  *       <code>sense</code> statements may only appear in classifiers that
  *       are generators or that return arrays.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B11</td>
  *     <td>
  *       The <i>expression : expression</i> form of the <code>sense</code>
  *       statement may only appear in a generator.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B12</td>
  *     <td>
  *       <code>return</code> statements may not appear in classifiers that
  *       are generators or that return arrays or in constraints.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B13</td>
  *     <td>
  *       Every {@link ReferenceType} must successfully locate the Java
  *       <code>Class</code> object for the type it refers to.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B14</td>
  *     <td>
  *       The only "mixed" classifier return type is <code>mixed%</code>.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B15</td>
  *     <td>
  *       A {@link CodedClassifier} may not be declared as
  *       <code>mixed%</code>.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B16</td>
  *     <td>
  *       There can be no more than one <code>with</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B17</td>
  *     <td>
  *       There can be no more than one <code>from</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B18</td>
  *     <td>
  *       There must be exactly one <code>using</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B19</td>
  *     <td>
  *       Constraint statements may only appear in constraint declarations.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B20</td>
  *     <td>
  *       Constraint declarations must contain at least one constraint
  *       statement.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B21</td>
  *     <td> Names in classifier expressions must refer to classifiers. </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B22</td>
  *     <td>
  *       The name to the left of the parentheses in an
  *       {@link InferenceInvocation} must refer to an inference.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B23</td>
  *     <td>
  *       The name inside the parentheses of an
  *       {@link InferenceInvocation} must refer to a discrete learner.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B24</td>
  *     <td>
  *       The input type of the classifier inside the parentheses of an
  *       {@link InferenceInvocation} is checked for appropriateness in its
  *       context.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B25</td>
  *     <td>
  *       The inference of an {@link InferenceInvocation} must contain an
  *       {@link LBJ2.IR.InferenceDeclaration.HeadFinder} whose input type is
  *       the same as the input type of the {@link InferenceInvocation}'s
  *       argument learner.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B26</td>
  *     <td>
  *       Only constraints can be invoked with the <code>&#64;</code> operator
  *       in a constraint statement.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B27</td>
  *     <td>
  *       The left hand side of the <code>normalizedby</code> operator must be
  *       the name of a {@link Learner}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B28</td>
  *     <td>
  *       The right hand side of the <code>normalizedby</code> operator must
  *       instantiate a {@link LBJ2.learn.Normalizer}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B29</td>
  *     <td>
  *       An {@link InferenceDeclaration} must contain at least one head
  *       finder method.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B30</td>
  *     <td>
  *       An {@link InferenceDeclaration} must contain exactly one
  *       <code>subjectto</code> clause.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B31</td>
  *     <td>
  *       An {@link InferenceDeclaration} may contain no more than one
  *       <code>with</code> clause.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B32</td>
  *     <td>
  *       The {@link InstanceCreationExpression} in the <code>with</code>
  *       clause of an {@link InferenceDeclaration} instantiates a
  *       {@link LBJ2.infer.Inference}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B33</td>
  *     <td>
  *       An inference may not be invoked anywhere other than classifier
  *       expression context.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B34</td>
  *     <td>
  *       Constraint expressions are only allowed to appear as part of their
  *       own separate expression statement.  (The only other place that the
  *       parser will allow them is in the head of a <code>for</code> loop.)
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B35</td>
  *     <td>
  *       The value supplied before the <code>rounds</code> keyword in a
  *       {@link LearningClassifierExpression}'s <code>from</code> clause must
  *       be an integer.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B36</td>
  *     <td>
  *       The <code>cachedin</code> keyword can be used to cache the value(s)
  *       produced by classifiers returning either a single feature or an
  *       array of features in a member variable of a user's class.  The
  *       values of features produced by generators cannot be cached in this
  *       way.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B37</td>
  *     <td>
  *       There can be no more than one <code>evaluate</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B38</td>
  *     <td>
  *       In the body of a coded classifier, a method invocation with no
  *       parent object is assumed to be a classifier invocation.  As such,
  *       that classifier's definition must be accessible in one form or
  *       another.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B39</td>
  *     <td>
  *       LBJ must be properly <code>configure</code>d to use the selected
  *       inference algorithm.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B40</td>
  *     <td>
  *       The value supplied after the <code>cval</code> keyword in a
  *       {@link LearningClassifierExpression} must be an integer.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B41</td>
  *     <td>
  *       The value supplied after <code>preExtract</code> must be a Boolean.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B42</td>
  *     <td>
  *       The value supplied after <code>progressOutput</code> must be an
  *       integer.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B43</td>
  *     <td>
  *       The value supplied after the <code>alpha</code> keyword in a
  *       {@link LearningClassifierExpression} must be a double.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B44</td>
  *     <td>
  *       The input to any classifier must have either type
  *       {@link ReferenceType} or type {@link ArrayType}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B45</td>
  *     <td>
  *       The <code>alpha</code> keyword should not be used if the
  *       <code>cval</code> keyword is not being used.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B46</td>
  *     <td>
  *       The <code>testingMetric</code> keyword should not be used if the
  *       <code>cval</code> keyword is not being used.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B47</td>
  *     <td>
  *       There can be no more than one <code>cval</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B48</td>
  *     <td>
  *       There can be no more than one <code>testingMetric</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B49</td>
  *     <td>
  *       There can be no more than one <code>alpha</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  * </table>
  *
  * @see    LBJ2.RevisionAnalysis
  * @see    LBJ2.parse.Parser
  * @see    LBJ2.learn.Learner
  * @see    LBJ2.learn.Normalizer
  * @see    LBJ2.infer.Inference
  * @author Nick Rizzolo
 **/
public class SemanticAnalysis extends Pass
{
  /**
    * The keys of this map are the names of {@link CodeGenerator}s; the values
    * are <code>HashSet</code>s of names of other locally defined
    * {@link CodeGenerator}s that depend on the {@link CodeGenerator} named by
    * the associated key.  The dependor graph has an entry for every
    * {@link CodeGenerator} in the source.
   **/
  public static HashMap dependorGraph;
  /**
    * The keys of this map are the names of {@link CodeGenerator}s; the values
    * are <code>HashSet</code>s of names of other (not necessarily locally
    * defined) {@link CodeGenerator}s that are invoked within the
    * {@link CodeGenerator} named by the associated key.  The invoked graph
    * does not necessarily have an entry for every {@link CodeGenerator} in
    * the source.
   **/
  public static HashMap invokedGraph;
  /**
    * The keys of this map are the names of {@link Classifier}s; the values
    * are {@link ASTNode}s representing the source code implementations of the
    * associated {@link Classifier}s.  This table has an entry for every
    * {@link Classifier} in the source.
   **/
  public static HashMap representationTable;


  /**
    * Adds an edge from dependency to dependor in the {@link #dependorGraph}.
    * If the dependor is <code>null</code>, no new list item is added, but the
    * <code>HashSet</code> associated with the dependency is still created if
    * it didn't already exist.
    *
    * @param dependency The name of the node depended on.
    * @param dependor   The name of the node doing the depending.
   **/
  private static void addDependor(String dependency, String dependor)
  {
    HashSet dependors = (HashSet) dependorGraph.get(dependency);

    if (dependors == null)
    {
      dependors = new HashSet();
      dependorGraph.put(dependency, dependors);
    }

    if (dependor != null) dependors.add(dependor);
  }


  /**
    * Use this method to determine if one {@link CodeGenerator} depends on
    * another either directly or indirectly.
    *
    * @param c1 One {@link CodeGenerator}.
    * @param c2 The other {@link CodeGenerator}.
    * @return   <code>true</code> iff <code>c1</code> depends on
    *           <code>c2</code>.
   **/
  public static boolean isDependentOn(String c1, String c2)
  {
    LinkedList queue = new LinkedList();
    queue.add(c2);

    HashSet visited = new HashSet();

    while (queue.size() > 0)
    {
      String c = (String) queue.removeFirst();
      if (c.equals(c1)) return true;

      visited.add(c);
      for (Iterator I = ((HashSet) dependorGraph.get(c)).iterator();
           I.hasNext(); )
      {
        c = (String) I.next();
        if (!visited.contains(c)) queue.add(c);
      }
    }

    return false;
  }


  /**
    * Adds an edge from invoker to invokee in the {@link #invokedGraph}.
    *
    * @param invoker  The name of the node doing the invoking.
    * @param invokee  The name of the invoked node.
   **/
  private static void addInvokee(String invoker, String invokee)
  {
    HashSet invokees = (HashSet) invokedGraph.get(invoker);

    if (invokees == null)
    {
      invokees = new HashSet();
      invokedGraph.put(invoker, invokees);
    }

    invokees.add(invokee);
  }


  /**
    * Prints the contents of {@link #dependorGraph} to <code>STDOUT</code> in
    * a readable form.
   **/
  public static void printDependorGraph() { printGraph(dependorGraph); }


  /**
    * Prints the contents of {@link #dependorGraph} to <code>STDOUT</code> in
    * a readable form.
   **/
  public static void printInvokedGraph() { printGraph(invokedGraph); }


  /**
    * Prints the contents of the specified graph to <code>STDOUT</code> in a
    * readable form.
    *
    * @param graph  The graph to print as a map of collections.
   **/
  private static void printGraph(HashMap graph)
  {
    String[] keys = (String[]) graph.keySet().toArray(new String[0]);
    Arrays.sort(keys);
    for (int i = 0; i < keys.length; ++i)
    {
      System.out.print(keys[i] + " ->");
      String[] edges =
        (String[]) ((Collection) graph.get(keys[i])).toArray(new String[0]);
      for (int j = 0; j < edges.length; ++j)
        System.out.print(" " + edges[j]);
      System.out.println();
    }
  }


  /**
    * Calls the <code>Class#isAssignableFrom(Class)</code> method after making
    * sure that both classes involved aren't null.  The assumption made when
    * calling this method is that if either argument class is
    * <code>null</code>, an error has already been generated with respect to
    * it.
    *
    * @param c1 Class 1.
    * @param c2 Class 2.
    * @return   <code>true</code> iff either class is null or c1 is assignable
    *           from c2.
   **/
  private static boolean isAssignableFrom(Class c1, Class c2)
  {
    return c1 == null || c2 == null || c1.isAssignableFrom(c2);
  }


  /**
    * Called when analyzing the feature types for use by a WEKA classifier.
    * Writes the necessary attribute information from a
    * <code>ClassifierReturnType</code> to <code>lce.attributeString</code>.
    *
    * <p> <code>lce.attributeString</code> takes the form of a colon-separated
    * list of attribute specifications, each of which are formated in the
    * following way:
    * "<code>type</code>_<code>name</code>(_<code>value-list</code>)".
    *
    * <p> <code>value-list</code> takes the same format as it would in an lbj
    * source file.  i.e. <code>{"value1","value2",...}</code>
    *
    * <p> <code>type</code> can take the values <code>str</code> (string
    * attributes), <code>nom</code> (nominal attributes), or <code>num</code>
    * (numerical attributes).
    *
    * <p> The first attribute in this string is, by convention, considered to
    * be the class attribute.
   **/
  public void wekaIze(int line, ClassifierReturnType RT, Name name)
  {
    String typeName = RT.getTypeName();
    if (!typeName.equals("discrete") && !typeName.equals("real"))
      reportError(line, "Classifiers with return type " + typeName
                        + " are not usable with WEKA learning algorithms");

    // String attribute case
    if (typeName.equals("discrete"))
    {
      if (RT.values.size() == 0)
      {
        lceInQuestion.attributeString += "str_" + name.toString() + ":";
      }
      // Nominal attribute case
      else
      {
        Constant[] constantList = RT.values.toArray();
        String valueList = "";

        for (int i = 0; i < constantList.length; ++i)
        {
          String value = constantList[i].value;

          if (value.length() > 1 && value.charAt(0) == '"'
              && value.charAt(value.length() - 1) == '"')
            value = value.substring(1, value.length() - 1);

          valueList += value + ",";
        }

        // at this point valueList should look like:
        // "value1,value2,...,valueN"

        lceInQuestion.attributeString +=
          "nom_" + name + "_" + valueList + ":";
      }
    }
    // Numerical attribute case
    else
    {
      lceInQuestion.attributeString += "num_" + name + ":";
    }
  }


  /**
    * Lets AST children know about the code producing node they are contained
    * in.
   **/
  private CodeGenerator currentCG;
  /**
    * Lets AST children know the return type of the
    * {@link ClassifierAssignment} they are contained in.
   **/
  private ClassifierReturnType currentRT;
  /**
    * Used when analyzing constraint declarations to determine if a constraint
    * statement appears within them.
   **/
  private boolean containsConstraintStatement;
  /** Lets all nodes know what symbol table represents their scope. */
  private SymbolTable currentSymbolTable;
  /**
    * Lets AST nodes know how deeply nested inside
    * {@link QuantifiedConstraintExpression}s they are.
   **/
  private int quantifierNesting;
  /**
    * A flag which indicates whether or not the compiler is in the process of
    * gathering attribute information for a WEKA learning algorithm.
   **/
  private boolean attributeAnalysis = false;
  /**
    * A reference to the <code>LearningClassifierExpression</code> which is
    * currently under analysis.
   **/
  private LearningClassifierExpression lceInQuestion;


  /**
    * Instantiates a pass that runs on an entire {@link AST}.
    *
    * @param ast  The program to run this pass on.
   **/
  public SemanticAnalysis(AST ast) { super(ast); }


  /**
    * Creates a new anonymous classifier name.
    *
    * @param lastName The last part of the classifier's name as determined by
    *                 its parent's name.
    * @return The created name.
   **/
  public Name anonymousClassifier(String lastName)  // A3
  {
    int index = lastName.indexOf('$');
    if (lastName.indexOf('$', index + 1) >= 0) return new Name(lastName);
    return new Name(Main.sourceFileBase + "$" + lastName);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ast The node to process.
   **/
  public void run(AST ast)
  {
    currentSymbolTable = ast.symbolTable;

    if (ast.symbolTable.importedSize() == 0) // A1
    {
      ast.symbolTable.addImported("LBJ2.classify.*");
      ast.symbolTable.addImported("LBJ2.learn.*");
      ast.symbolTable.addImported("LBJ2.parse.*");
      ast.symbolTable.addImported("LBJ2.infer.*");
      if (LBJ2.Configuration.GLPKLinked)
        ast.symbolTable.addImported("LBJ2.jni.*");
    }

    dependorGraph = new HashMap();
    invokedGraph = new HashMap();
    representationTable = new HashMap();
    quantifierNesting = 0;

    runOnChildren(ast);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param decl The node to process.
   **/
  public void run(PackageDeclaration decl)
  {
    ast.symbolTable.setPackage(decl.name.toString()); // A1
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param decl The node to process.
   **/
  public void run(ImportDeclaration decl)
  {
    ast.symbolTable.addImported(decl.name.toString());  // A1
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param list The node to process.
   **/
  public void run(DeclarationList list)
  {
    if (list.size() == 0) return;

    for (DeclarationList.DeclarationListIterator I = list.listIterator();
         I.hasNext(); )
    {
      Declaration d = I.nextItem();
      if (ast.symbolTable.containsKey(d.name))  // B1
        reportError(d.line, "A declaration named '" + d.name
                            + "' already exists.");
      ast.symbolTable.put(d.name, d.getType()); // A1
    }

    currentCG = null;
    runOnChildren(list);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ca The node to process.
   **/
  public void run(ClassifierAssignment ca)
  {
    LBJ2.IR.Type inputType = ca.argument.getType();
    if (!(inputType instanceof ReferenceType  // B44
          || inputType instanceof ArrayType))
      reportError(ca.line,
          "The input to a classifier must be a single object reference.");

    ca.expression.name = (Name) ca.name.clone();  // A3

    ca.expression.returnType = (ClassifierReturnType) ca.returnType.clone();
      // B3
    ca.expression.argument = (Argument) ca.argument.clone();  // A4

    if (ca.cacheIn != null)
    {
      // B36
      if (ca.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
          || ca.returnType.type == ClassifierReturnType.REAL_GENERATOR
          || ca.returnType.type == ClassifierReturnType.MIXED_GENERATOR)
        reportError(ca.line,
          "Generators' outputs cannot be cached in a member variable.");

      ca.expression.setCacheIn(ca.cacheIn); // A18
    }

    currentRT = (ClassifierReturnType) ca.returnType.clone(); // A4
    ca.returnType.runPass(this);
    ca.name.runPass(this);
    ca.expression.runPass(this);
    ca.expression.returnType = (ClassifierReturnType) ca.returnType.clone();

    ca.expression.comment = ca.comment; // A19
    representationTable.put(ca.name.toString(), ca.expression); // A2
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cce  The node to process.
   **/
  public void run(ClassifierCastExpression cce)
  {
    if (!cce.castType.isContainableIn(cce.returnType))  // B3
      reportError(cce.line,
          "Found classifier expression of return type '" + cce.castType
          + "' when '" + cce.returnType + "' was expected.");

    cce.expression.name = (Name) cce.name.clone();  // A3

    cce.expression.returnType = (ClassifierReturnType) cce.castType.clone();
      // B3
    cce.expression.argument = (Argument) cce.argument.clone();  // A4

    ClassifierReturnType saveRT = currentRT;
    currentRT = (ClassifierReturnType) cce.castType.clone();  // A4
    boolean saveAttributeAnalysis = attributeAnalysis;
    attributeAnalysis = false;

    runOnChildren(cce);

    attributeAnalysis = saveAttributeAnalysis;
    currentRT = saveRT;

    representationTable.put(cce.name.toString(), cce);  // A2
    cce.returnType = (ClassifierReturnType) cce.castType.clone();

    if (attributeAnalysis) wekaIze(cce.line, cce.returnType, cce.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cn The node to process.
   **/
  public void run(ClassifierName cn)
  {
    if (cn.name.toString().startsWith(Main.sourceFileBase + "$")) // A17
      cn.name = cn.referent;
    else addDependor(cn.name.toString(), null); // A8

    LBJ2.IR.Type t = ast.symbolTable.get(cn);
    if (!(t instanceof ClassifierType)) // B21
    {
      reportError(cn.line, "'" + cn + "' is not known to be a classifier.");
      cn.returnType = null;
      return;
    }

    ClassifierType type = (ClassifierType) t;

    LBJ2.IR.Type input = type.getInput();
    if (!isAssignableFrom(input.typeClass(),
                          cn.argument.getType().typeClass()))  // B4
      reportError(cn.line,
          "Classifier '" + cn + "' has input type '" + input + "' when '"
          + cn.argument.getType() + "' was expected.");

    ClassifierReturnType output = type.getOutput();
    if (!output.isContainableIn(cn.returnType)) // B3
      reportError(cn.line,
          "Classifier '" + cn + "' has return type '" + output + "' when '"
          + cn.returnType + "' was expected.");
    else cn.returnType = output;  // A4

    if (attributeAnalysis) wekaIze(cn.line, cn.returnType, cn.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cc The node to process.
   **/
  public void run(CodedClassifier cc)
  {
    addDependor(cc.name.toString(), null);  // A8

    cc.returnType = (ClassifierReturnType) currentRT.clone(); // A4

    if (cc.returnType.type == ClassifierReturnType.MIXED_GENERATOR) // B15
      reportError(cc.line,
          "A coded classifier may not have return type 'mixed%'.");

    // A13
    currentSymbolTable = cc.symbolTable = cc.body.symbolTable =
      new SymbolTable(currentSymbolTable);

    CodeGenerator saveCG = currentCG;
    currentCG = cc;
    run(cc.argument); // A1
    runOnChildren(cc);
    currentCG = saveCG;

    representationTable.put(cc.name.toString(), cc);  // A2
    currentSymbolTable = currentSymbolTable.getParent();

    if (attributeAnalysis) wekaIze(cc.line, cc.returnType, cc.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cg The node to process.
   **/
  public void run(CompositeGenerator cg)
  {
    addDependor(cg.name.toString(), null);  // A8

    int i = 0;
    for (ClassifierExpressionList.ClassifierExpressionListIterator I =
           cg.components.listIterator();
         I.hasNext(); )
    {
      ClassifierExpression e = I.nextItem();

      e.name = anonymousClassifier(cg.name + "$" + i++);  // A3
      e.returnType =
        new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR); // B3
      e.argument = (Argument) cg.argument.clone();  // A4

      e.runPass(this);

      addDependor(e.name.toString(), cg.name.toString()); // A8
    }

    String cgReturnType = null;
    ConstantList values = null;
    for (ClassifierExpressionList.ClassifierExpressionListIterator I =
           cg.components.listIterator();
         I.hasNext(); ) // A4
    {
      ClassifierExpression component = I.nextItem();

      if (component.returnType == null) return;
      String componentReturnType = component.returnType.toString();
      if (cgReturnType == null)
      {
        cgReturnType = componentReturnType;
        values = component.returnType.values;
      }
      else
      {
        if (cgReturnType.startsWith("discrete")
              && !componentReturnType.startsWith("discrete")
            || cgReturnType.startsWith("real")
               && !componentReturnType.startsWith("real"))
          cgReturnType = "mixed";
        if (values.size() > 0 && !values.equals(component.returnType.values))
          values = new ConstantList();
      }
    }

    assert cgReturnType != null : "Empty component list";

    // A4
    ClassifierReturnType output = null;
    if (cgReturnType.startsWith("discrete"))
      output =
        new ClassifierReturnType(ClassifierReturnType.DISCRETE_GENERATOR,
                                 values);
    else if (cgReturnType.startsWith("real"))
      output = new ClassifierReturnType(ClassifierReturnType.REAL_GENERATOR);
    else
      output = new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR);

    if (!output.isContainableIn(cg.returnType)) // B3
      reportError(cg.line,
          "Found a classifier expression of return type '" + output
          + "' when '" + cg.returnType + "' was expected.");
    else cg.returnType = output;

    representationTable.put(cg.name.toString(), cg);  // A2
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(Conjunction c)
  {
    addDependor(c.name.toString(), null); // A8

    c.left.name = anonymousClassifier(c.name + "$0"); // A3
    c.left.returnType =
      new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR); // B3
    c.left.argument = (Argument) c.argument.clone();  // A4

    c.right.name = anonymousClassifier(c.name + "$1");  // A3
    c.right.returnType =
      new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR); // B3
    c.right.argument = (Argument) c.argument.clone(); // A4

    boolean saveAttributeAnalysis = attributeAnalysis;
    attributeAnalysis = false;

    runOnChildren(c);

    attributeAnalysis = saveAttributeAnalysis;

    if (c.left.returnType == null || c.right.returnType == null) return;

    addDependor(c.left.name.toString(), c.name.toString()); // A8
    addDependor(c.right.name.toString(), c.name.toString());  // A8

    // A4
    LBJ2.IR.Type inputType = c.right.argument.getType();
    Class inputRight = inputType.typeClass();
    LBJ2.IR.Type leftType = c.left.argument.getType();
    Class inputLeft = leftType.typeClass();
    if (!isAssignableFrom(inputLeft, inputRight)) inputType = leftType;

    c.argument =
      new Argument(inputType, c.argument.getName(), c.argument.getFinal());

    ConstantList valuesLeft = c.left.returnType.values;
    ConstantList valuesRight = c.right.returnType.values;
    ConstantList values = new ConstantList();
    if (valuesLeft.size() > 0 && valuesRight.size() > 0)
      for (ConstantList.ConstantListIterator I = valuesLeft.listIterator();
           I.hasNext(); )
      {
        Constant valueLeft = I.nextItem();
        for (ConstantList.ConstantListIterator J = valuesRight.listIterator();
             J.hasNext(); )
          values.add(new Constant(valueLeft.noQuotes() + "&"
                                  + J.nextItem().noQuotes()));
      }

    int rt1 = c.left.returnType.type;
    int rt2 = c.right.returnType.type;
    if (rt2 < rt1)
    {
      int temp = rt1;
      rt1 = rt2;
      rt2 = temp;
    }

    ClassifierReturnType output = null;
    switch (10 * rt1 + rt2)
    {
      case 0:
        output =
          new ClassifierReturnType(ClassifierReturnType.DISCRETE, values);
        break;

      case 11:
        output = new ClassifierReturnType(ClassifierReturnType.REAL);
        break;

      case 3: case 33:
        output =
          new ClassifierReturnType(ClassifierReturnType.DISCRETE_ARRAY,
                                   values);
        break;

      case 14: case 44:
        output = new ClassifierReturnType(ClassifierReturnType.REAL_ARRAY);
        break;

      case 6: case 36: case 66:
        output =
          new ClassifierReturnType(ClassifierReturnType.DISCRETE_GENERATOR,
                                   values);
        break;

      case 1: case 4: case 7: case 13: case 16: case 17: case 34: case 37:
      case 46: case 47: case 67: case 77:
        output =
          new ClassifierReturnType(ClassifierReturnType.REAL_GENERATOR);
        break;

      case 8: case 18: case 38: case 48: case 68: case 78: case 88:
        output =
          new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR);
        break;
    }

    assert output != null
           : "Unexpected conjunction types: "
             + ClassifierReturnType.typeName(rt1) + ", "
             + ClassifierReturnType.typeName(rt2);

    if (!output.isContainableIn(c.returnType))  // B3
      reportError(c.line,
          "Found a classifier expression of return type '" + output
          + "' when '" + c.returnType + "' was expected.");
    else c.returnType = output;

    representationTable.put(c.name.toString(), c);  // A2

    if (attributeAnalysis) wekaIze(c.line, c.returnType, c.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ii  The node to process.
   **/
  public void run(InferenceInvocation ii)
  {
    // A8
    addDependor(ii.name.toString(), null);
    addDependor(ii.inference.toString(), ii.name.toString());
    addDependor(ii.classifier.toString(), ii.name.toString());

    runOnChildren(ii);

    if (!(ii.inference.typeCache instanceof InferenceType))  // B22
    {
      reportError(ii.inference.line,
          "'" + ii.inference + "' is not known to be a inference.");
      return;
    }

    if (!(ii.classifier.typeCache instanceof ClassifierType)) // B23
    {
      reportError(ii.classifier.line,
          "'" + ii.classifier + "' is not known to be a learner.");
      return;
    }

    ClassifierType argumentType = (ClassifierType) ii.classifier.typeCache;
    ClassifierReturnType output = argumentType.getOutput();
    if (output.type != ClassifierReturnType.DISCRETE
        || !argumentType.isLearner()) // B23
      reportError(ii.classifier.line,
          "'" + ii.classifier + "' is not a discrete learner.");

    LBJ2.IR.Type input = argumentType.getInput();
    if (!isAssignableFrom(input.typeClass(),
                          ii.argument.getType().typeClass()))  // B24
      reportError(ii.line,
          "Classifier '" + ii + "' has input type '" + input + "' when '"
          + ii.argument.getType() + "' was expected.");

    if (!output.isContainableIn(ii.returnType)) // B3
      reportError(ii.line,
          "Classifier '" + ii + "' has return type '" + output + "' when '"
          + ii.returnType + "' was expected.");
    else ii.returnType = output; // A4

    InferenceType type = (InferenceType) ii.inference.typeCache;
    boolean found = false;
    for (int i = 0; i < type.getFindersLength() && !found; ++i)
      found = type.getFinderType(i).equals(input);

    if (!found) // B25
      reportError(ii.line,
          "Inference '" + ii.inference + "' does not contain a head finder "
          + " method for class '" + input + "'.");

    representationTable.put(ii.name.toString(), ii);  // A2

    if (attributeAnalysis) wekaIze(ii.line, ii.returnType, ii.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param lce  The node to process.
   **/
  public void run(LearningClassifierExpression lce)
  {
    addDependor(lce.name.toString(), null); // A8

    int i = 0;
    if (lce.labeler != null)
    {
      lce.labeler.name = anonymousClassifier(lce.name + "$" + i++); // A3
      lce.labeler.returnType =
        new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR); // B3
      lce.labeler.argument = (Argument) lce.argument.clone(); // A4
    }

    if (lce.usingClauses != 1)  // B18
    {
      reportError(lce.line,
          "A learning classifier expression must contain exactly one 'using' "
          + "clause.");
      return;
    }

    if (lce.fromClauses > 1)  // B17
    {
      reportError(lce.line,
          "A learning classifier expression can have no more than one 'from' "
          + "clause.");
      return;
    }

    if (lce.withClauses > 1) // B16
    {
      reportError(lce.line,
          "A learning classifier expression can have no more than one 'with' "
          + "clause.");
      return;
    }

    if (lce.evaluateClauses > 1)  // B37
    {
      reportError(lce.line,
          "A learning classifier expression can have no more than one "
          + "'evaluate' clause.");
    }

    if (lce.cvalClauses == 0)
    {
      if (lce.alphaClauses > 0) // B45
      {
        reportError(lce.line,
            "The alpha keyword is meaningful only if the cval keyword is "
            + "also being used, and should not be used otherwise.");
      }

      if (lce.testingMetric != null)  // B46
      {
        reportError(lce.line,
            "The testingMetric keyword is meaningful only if the cval "
            + "keyword is also being used, and should not be used "
            + "otherwise.");
      }
    }

    if (lce.cvalClauses > 1)  // B47
    {
      reportError(lce.line,
          "A learning classifier expression can have no more than one 'cval'"
          + " clause.");
    }

    if (lce.testingMetricClauses > 1) // B48
    {
      reportError(lce.line,
          "A learning classifier expression can have no more than one "
          + "'testingMetric' clause.");
    }

    if (lce.alphaClauses > 1) // B49
    {
      reportError(lce.line,
          "A learning classifier expression can have no more than one 'alpha'"
          + " clause.");
    }

    lce.extractor.name = anonymousClassifier(lce.name + "$" + i); // A3
    lce.extractor.returnType =
      new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR); // B3
    lce.extractor.argument = (Argument) lce.argument.clone(); // A4

    if (lce.learnerName == null)
    {
      if (lce.learnerConstructor == null)  // A11
      {
        if (lce.returnType.toString().charAt(0) == 'd')
          lce.learnerConstructor =
            LearningClassifierExpression.defaultDiscreteLearner;
        else
          lce.learnerConstructor =
            LearningClassifierExpression.defaultRealLearner;
        lce.learnerConstructor.runPass(this);
      }

      lce.learnerName = lce.learnerConstructor.name;  // A20
    }

    lce.learnerName.runPass(this);

    if (lce.evaluation != null && lce.evaluation instanceof MethodInvocation)
      ((MethodInvocation) lce.evaluation).isEvaluateArgument = true;

    currentCG = lce;
    boolean weka = false;
    boolean saveAttributeAnalysis = attributeAnalysis;
    LearningClassifierExpression temp = null;

    Class learnerClass =
      ast.globalSymbolTable.classForName(lce.learnerName);
    weka = learnerClass != null
           && (learnerClass.getName().equals("LBJ2.learn.WekaWrapper")
               || learnerClass.getName().equals("WekaWrapper"));
    if (weka && !LBJ2.Configuration.WekaLinked)
      reportError(lce.learnerName.line,
          "LBJ has not been configured properly to use the WEKA library.  "
          + "See the Users's Manual for more details.");

    if (weka)
    {
      attributeAnalysis = true;

      // Identify which learning classifier expression we are gathering
      // feature information for.
      temp = lceInQuestion;
      lceInQuestion = lce;
    }

    runOnChildren(lce);

    if (weka)
    {
      attributeAnalysis = saveAttributeAnalysis;
      lceInQuestion = temp;
    }

    currentCG = null;

    if (lce.rounds != null)
    {
      try { int rounds = Integer.parseInt(lce.rounds.value); }
      catch (Exception e) // B35
      {
        reportError(lce.line,
            "The value supplied before 'rounds' must be an integer.");
      }
    }

    if (lce.K != null)
    {
      try { int k = Integer.parseInt(lce.K.value); }
      catch (Exception e) // B40
      {
        reportError(lce.line,
            "The value supplied after 'cval' must be an integer.");
      }
    }

    if (lce.alpha != null)
    {
      try { double a = Double.parseDouble(lce.alpha.value); }
      catch (Exception e) // B43
      {
        reportError(lce.line,
            "The value supplied after 'alpha' must be an double.");
      }
    }

    if (lce.preExtract != null)
    {
      try
      {
        boolean p = Boolean.valueOf(lce.preExtract.value).booleanValue();
      }
      catch (Exception e) // B41
      {
        reportError(lce.line,
            "The value supplied after 'preExtract' must be a boolean.");
      }
    }

    if (lce.progressOutput != null)
    {
      try { int p = Integer.parseInt(lce.progressOutput.value); }
      catch (Exception e) // B42
      {
        reportError(lce.line,
            "The value supplied after 'progressOutput' must be an integer.");
      }
    }

    if (lce.labeler != null)
      addDependor(lce.labeler.name.toString(), lce.name.toString());  // A8
    addDependor(lce.extractor.name.toString(), lce.name.toString());  // A8

    if (lce.parser != null) // B5
    {
      if (!(lce.parser.typeCache instanceof ReferenceType))
        reportError(lce.parser.line,
            "The 'from' clause of a learning classifier expression must "
            + "instantiate a LBJ2.parse.Parser.");
      else
      {
        Class iceClass = lce.parser.typeCache.typeClass();
        if (!isAssignableFrom(Parser.class, iceClass))
          reportError(lce.parser.line,
              "The 'from' clause of a learning classifier expression must "
              + "instantiate a LBJ2.parse.Parser.");
      }
    }

    LBJ2.IR.Type input = lce.argument.getType();
    Class inputClass = input.typeClass();
    ClassifierReturnType output = null;

    if (!(lce.learnerName.typeCache instanceof ClassifierType)
        || !((ClassifierType) lce.learnerName.typeCache).isLearner()) // B6
    {
      System.out.println(lce.learnerName.typeCache);
      reportError(lce.learnerName.line,
          "The 'with' clause of a learning classifier expression must "
          + "instantiate a LBJ2.learn.Learner.");
    }
    else
    {
      Class iceClass = AST.globalSymbolTable.classForName(lce.learnerName);

      if (iceClass != null)
      {
        if (!isAssignableFrom(Learner.class, iceClass)) // B6
          reportError(lce.learnerName.line,
              "The 'with' clause of a learning classifier expression must "
              + "instantiate a LBJ2.learn.Learner.");
        else  // A4
        {
          ClassifierType learnerType =
            (ClassifierType) lce.learnerName.typeCache;
          LBJ2.IR.Type learnerInputType = learnerType.getInput();
          if (!isAssignableFrom(learnerInputType.typeClass(), inputClass))
            reportError(lce.learnerName.line,  // B7
                "A learning classifier with input type '" + input
                + "' cannot use a Learner with input type '"
                + learnerInputType + "'.");

          output = learnerType.getOutput();
        }
      }
    }

    if (output != null && !output.isContainableIn(lce.returnType))  // B3
    {
      if (output.toString().charAt(0) != 'd'
          || lce.returnType.toString().charAt(0) != 'd')
        reportError(lce.line,
            "Learner " + lce.learnerName + " returns '" + output
            + "' which conflicts with the declared return type '"
            + lce.returnType + "'.");
      else
      {
        lce.checkDiscreteValues = true;
        reportWarning(lce.line,
            "Learner " + lce.learnerName + " returns '" + output
            + "' which may conflict with the declared return type '"
            + lce.returnType + "'.  A run-time error will be reported if a "
            + "conflict is detected.");
      }
    }
    else lce.returnType = output;

    if (output != null && lce.labeler != null
        && !lce.labeler.returnType.isContainableIn(output))
    { // B3
      if (output.toString().charAt(0) == 'd'
          && lce.labeler.returnType.toString().charAt(0) == 'd')
        reportWarning(lce.line,
            "The labeler for learner " + lce.name + " may return more labels "
            + "than the learner is designed to deal with.  A run-time error "
            + "will be reported if a conflict is detected.");
      else
        reportWarning(lce.line,
            "The labeler for learner " + lce.name + " may return labels that "
            + "the learner is not designed to deal with.  A run-time error "
            + "will be reported if a conflict is detected.");
    }

    representationTable.put(lce.name.toString(), lce);  // A2

    if (attributeAnalysis) wekaIze(lce.line, lce.returnType, lce.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param b  The node to process.
   **/
  public void run(Block b)
  {
    boolean needLocalTable = b.symbolTable == null;
    if (needLocalTable)
      currentSymbolTable = b.symbolTable =
        new SymbolTable(currentSymbolTable);  // A13

    runOnChildren(b);

    if (needLocalTable) currentSymbolTable = currentSymbolTable.getParent();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param m  The node to process.
   **/
  public void run(MethodInvocation m)
  {
    runOnChildren(m);

    if (m.name.typeCache instanceof ClassifierType && m.parentObject == null)
    {
      if (m.arguments.size() != 1)  // B2
        reportError(m.line, "Classifiers can only take a single argument.");
      else
      {
        ClassifierReturnType returnType =
          ((ClassifierType) m.name.typeCache).getOutput();
        m.isClassifierInvocation = true;  // A5 B8

        if (m.isSensedValue // B9
            && !returnType
                .isContainableIn(((CodedClassifier) currentCG).returnType))
          reportError(m.line,
              "Classifier " + currentCG.getName() + " with return type '"
              + ((CodedClassifier) currentCG).returnType
              + "' cannot sense classifier " + m.name + " with return type '"
              + returnType + "'.");
        else
          if (!m.isSensedValue
              && (returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
                  || returnType.type == ClassifierReturnType.REAL_GENERATOR
                  || returnType.type == ClassifierReturnType.MIXED_GENERATOR))
            reportError(m.line,
                "Feature generators may only be invoked as the value "
                + "argument of a sense statement in another generator.");
        else if (currentCG != null) // A9
          addInvokee(currentCG.getName(), m.name.toString());
      }
    }
    else if (m.name.typeCache instanceof InferenceType
             && m.parentObject == null) // B33
      reportError(m.line,
          "Inferences may only be invoked to create a new classifier in "
          + "classifier expression context.");
    else if (m.parentObject == null && m.name.name.length == 1
             && !m.isEvaluateArgument) // B38
      reportError(m.line, "Unrecognized classifier name: '" + m.name + "'");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ice  The node to process.
   **/
  public void run(InstanceCreationExpression ice)
  {
    runOnChildren(ice);

    if (ice.parentObject == null)  // A4
    {
      ice.typeCache = new ReferenceType(ice.name);
      ice.typeCache.runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param n  The node to process.
   **/
  public void run(Name n)
  {
    n.symbolTable = currentSymbolTable; // A13

    n.typeCache = n.symbolTable.get(n); // A4

    if (currentCG == null) return;

    if (n.typeCache instanceof ClassifierType)
    {
      if (ast.symbolTable.containsKey(n)) // A8
        addDependor(n.toString(), currentCG.getName());
    }
    else if (n.length() > 1 && ast.symbolTable.containsKey(n.name[0])
             && !n.name[1].equals("isTraining"))
    {
      addDependor(n.name[0], currentCG.getName());  // A8

      // A10
      LBJ2.IR.Type t = ast.symbolTable.get(n.name[0]);
      if (t instanceof ClassifierType && !n.name[1].equals("getInstance"))
        n.name[0] = "new " + n.name[0] + "()";
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ForStatement s)
  {
    if (s.initializers != null) // B34
      for (ASTNodeIterator I = s.initializers.iterator(); I.hasNext(); )
      {
        ASTNode statementExpression = I.next();
        if (statementExpression instanceof ConstraintStatementExpression)
          reportError(statementExpression.line,
              "Constraint expressions are only allowed to appear as part of "
              + "their own separate expression statement.");
      }

    if (s.updaters != null) // B34
      for (ASTNodeIterator I = s.updaters.iterator(); I.hasNext(); )
      {
        ASTNode statementExpression = I.next();
        if (statementExpression instanceof ConstraintStatementExpression)
          reportError(statementExpression.line,
              "Constraint expressions are only allowed to appear as part of "
              + "their own separate expression statement.");
      }

    if (!(s.body instanceof Block)) // A7
      s.body = new Block(new StatementList(s.body));

    currentSymbolTable = s.symbolTable = s.body.symbolTable =
      new SymbolTable(currentSymbolTable);  // A13

    runOnChildren(s);

    currentSymbolTable = currentSymbolTable.getParent();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(IfStatement s)
  {
    if (!(s.thenClause instanceof Block)) // A7
      s.thenClause = new Block(new StatementList(s.thenClause));
    if (s.elseClause != null && !(s.elseClause instanceof Block)) // A7
      s.elseClause = new Block(new StatementList(s.elseClause));
    runOnChildren(s);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ReturnStatement s)
  {
    if (currentCG instanceof ConstraintDeclaration
        || currentCG instanceof CodedClassifier
           && ((CodedClassifier) currentCG).returnType.type
              != ClassifierReturnType.DISCRETE
           && ((CodedClassifier) currentCG).returnType.type
              != ClassifierReturnType.REAL) // B12
      reportError(s.line,
          "return statements may only appear in classifers of type discrete "
          + "or real, not in an array returner, a generator, or a "
          + "constraint.");

    runOnChildren(s);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(SenseStatement s)
  {
    if (!(currentCG instanceof CodedClassifier)
        || ((CodedClassifier) currentCG).returnType.type
           == ClassifierReturnType.DISCRETE
        || ((CodedClassifier) currentCG).returnType.type
           == ClassifierReturnType.REAL)  // B10
    {
      reportError(s.line,
          "sense statements may only appear in an array returning classifier "
          + "or a generator.");
      return;
    }

    CodedClassifier currentCC = (CodedClassifier) currentCG;
    if (s.name != null) // B11
    {
      if (currentCC.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
          || currentCC.returnType.type == ClassifierReturnType.REAL_ARRAY)
        reportError(s.line,
            "The names of features need not be sensed in an array returning "
            + "classifier.  (Use sense <expression>; instead of sense "
            + "<expression> : <expression>;)");
    }
    else if (currentCC.returnType.type
             == ClassifierReturnType.DISCRETE_GENERATOR)  // A6
    {
      s.name = s.value;
      s.value = new Constant("true");
    }
    else if (currentCC.returnType.type == ClassifierReturnType.REAL_GENERATOR)
    { // A6
      s.name = s.value;
      s.value = new Constant("1");
    }

    s.value.senseValueChild();  // B9

    runOnChildren(s);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(WhileStatement s)
  {
    if (!(s.body instanceof Block)) // A7
      s.body = new Block(new StatementList(s.body));
    runOnChildren(s);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(DoStatement s)
  {
    if (!(s.body instanceof Block)) // A7
      s.body = new Block(new StatementList(s.body));
    runOnChildren(s);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param v  The node to process.
   **/
  public void run(VariableDeclaration v)
  {
    for (NameList.NameListIterator I = v.names.listIterator(); I.hasNext(); )
      currentSymbolTable.put(I.nextItem(), v.type); // A1
    runOnChildren(v);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param a  The node to process.
   **/
  public void run(Argument a)
  {
    currentSymbolTable.put(a);  // A1
    runOnChildren(a);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param t  The node to process.
   **/
  public void run(ReferenceType t)
  {
    runOnChildren(t);
    if (t.typeClass() == null)  // B13
      reportError(t.line, "Cannot locate class '" + t + "'.");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param t  The node to process.
   **/
  public void run(ClassifierReturnType t)
  {
    // B14
    if (t.type == ClassifierReturnType.MIXED)
      reportError(t.line,
                  "There is no such type as mixed.  (There is only mixed%.)");
    else if (t.type == ClassifierReturnType.MIXED_ARRAY)
      reportError(t.line,
          "There is no such type as mixed[].  (There is only mixed%.)");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(ConstraintDeclaration c)
  {
    addDependor(c.getName(), null); // A8

    currentSymbolTable = c.symbolTable = c.body.symbolTable
      = new SymbolTable(currentSymbolTable);  // A13
    c.argument.runPass(this);

    containsConstraintStatement = false;
    CodeGenerator saveCG = currentCG;
    currentCG = c;
    c.body.runPass(this);
    currentCG = saveCG;

    currentSymbolTable = currentSymbolTable.getParent();

    if (!containsConstraintStatement) // B20
      reportWarning(c.line,
          "Constraint '" + c.name
          + "' does not contain any constraint statements.");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ConstraintStatementExpression e)
  {
    if (!(currentCG instanceof ConstraintDeclaration))  // B19
    {
      reportError(e.line,
          "Constraint statements may only appear in constraint "
          + "declarations.");
      return;
    }

    containsConstraintStatement = true;
    runOnChildren(e);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param q  The node to process.
   **/
  public void run(UniversalQuantifierExpression q)
  {
    q.argument.getType().quantifierArgumentType = true; // A14
    // A13
    currentSymbolTable = q.symbolTable = new SymbolTable(currentSymbolTable);

    ++quantifierNesting;
    runOnChildren(q);
    --quantifierNesting;

    currentSymbolTable = currentSymbolTable.getParent();

    // A15
    q.collectionIsQuantified =
      quantifierNesting > 0 && q.collection.containsQuantifiedVariable();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param q  The node to process.
   **/
  public void run(ExistentialQuantifierExpression q)
  {
    q.argument.getType().quantifierArgumentType = true; // A14
    // A13
    currentSymbolTable = q.symbolTable = new SymbolTable(currentSymbolTable);

    ++quantifierNesting;
    runOnChildren(q);
    --quantifierNesting;

    currentSymbolTable = currentSymbolTable.getParent();

    // A15
    q.collectionIsQuantified =
      quantifierNesting > 0 && q.collection.containsQuantifiedVariable();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param q  The node to process.
   **/
  public void run(AtLeastQuantifierExpression q)
  {
    q.argument.getType().quantifierArgumentType = true; // A14
    // A13
    currentSymbolTable = q.symbolTable = new SymbolTable(currentSymbolTable);

    ++quantifierNesting;
    runOnChildren(q);
    --quantifierNesting;

    currentSymbolTable = currentSymbolTable.getParent();

    // A15
    if (quantifierNesting > 0)
    {
      q.collectionIsQuantified = q.collection.containsQuantifiedVariable();
      q.lowerBoundIsQuantified = q.lowerBound.containsQuantifiedVariable();
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param q  The node to process.
   **/
  public void run(AtMostQuantifierExpression q)
  {
    q.argument.getType().quantifierArgumentType = true; // A14
    // A13
    currentSymbolTable = q.symbolTable = new SymbolTable(currentSymbolTable);

    ++quantifierNesting;
    runOnChildren(q);
    --quantifierNesting;

    currentSymbolTable = currentSymbolTable.getParent();

    // A15
    if (quantifierNesting > 0)
    {
      q.collectionIsQuantified = q.collection.containsQuantifiedVariable();
      q.upperBoundIsQuantified = q.upperBound.containsQuantifiedVariable();
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param i  The node to process.
   **/
  public void run(ConstraintInvocation i)
  {
    if (i.invocation.arguments.size() != 1)  // B2
      reportError(i.line, "Constraints can only take a single argument.");

    runOnChildren(i); // A9

    if (!(i.invocation.name.typeCache instanceof ConstraintType)) // B26
      reportError(i.line,
          "Only constraints can be invoked with the '@' operator.");

    // A15
    i.invocationIsQuantified =
      quantifierNesting > 0 && i.invocation.containsQuantifiedVariable();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ConstraintEqualityExpression e)
  {
    runOnChildren(e);

    e.leftIsDiscreteLearner = e.rightIsDiscreteLearner = false;
    if (e.left instanceof MethodInvocation)
    {
      MethodInvocation m = (MethodInvocation) e.left;
      // A12
      e.leftIsDiscreteLearner = m.name.typeCache instanceof ClassifierType;
      if (e.leftIsDiscreteLearner)
      {
        ClassifierType type = (ClassifierType) m.name.typeCache;
        e.leftIsDiscreteLearner =
          type.getOutput().type == ClassifierReturnType.DISCRETE
          && type.isLearner();
      }
    }

    if (e.right instanceof MethodInvocation)
    {
      MethodInvocation m = (MethodInvocation) e.right;
      // A12
      e.rightIsDiscreteLearner = m.name.typeCache instanceof ClassifierType;
      if (e.rightIsDiscreteLearner)
      {
        ClassifierType type = (ClassifierType) m.name.typeCache;
        e.rightIsDiscreteLearner =
          type.getOutput().type == ClassifierReturnType.DISCRETE
          && type.isLearner();
      }
    }

    // A15
    if (quantifierNesting > 0)
    {
      e.leftIsQuantified = e.left.containsQuantifiedVariable();
      e.rightIsQuantified = e.right.containsQuantifiedVariable();
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param d  The node to process.
   **/
  public void run(InferenceDeclaration d)
  {
    addDependor(d.getName(), null); // A8

    if (d.headFinders.length == 0)  // B29
      reportError(d.line,
          "An inference with no head finder methods can never be applied to "
          + "a learner.");

    if (d.subjecttoClauses != 1)  // B30
      reportError(d.line,
          "Every inference must contain exactly one 'subjectto' clause "
          + "specifying a constraint. " + d.subjecttoClauses);

    if (d.withClauses > 1)  // B31
      reportError(d.line,
          "An inference may contain no more than one 'with' clause "
          + "specifying an inference algorithm.");

    currentCG = d;

    d.name.runPass(this);

    for (int i = 0; i < d.headFinders.length; ++i)
    {
      d.headFinders[i].symbolTable = d.headFinders[i].body.symbolTable =
        currentSymbolTable = new SymbolTable(currentSymbolTable); // A13
      d.headFinders[i].runPass(this);
      currentSymbolTable = currentSymbolTable.getParent();
    }

    for (int i = 0; i < d.normalizerDeclarations.length; ++i)
      d.normalizerDeclarations[i].runPass(this);

    d.constraint.runPass(this);
    if (d.algorithm != null) d.algorithm.runPass(this);

    currentCG = null;

    if (d.algorithm != null)
    {
      Class iceClass = d.algorithm.typeCache.typeClass();
      if (!isAssignableFrom(Inference.class, iceClass)) // B32
        reportError(d.algorithm.line,
            "The 'with' clause of an inference must instantiate an "
            + "LBJ2.infer.Inference.");

      if (iceClass != null && iceClass.equals(ILPInference.class))
      {
        Expression[] arguments = d.algorithm.arguments.toArray();

        if (arguments[0] instanceof InstanceCreationExpression) // B39
        {
          InstanceCreationExpression ice =
            (InstanceCreationExpression) arguments[0];

          if ((ice.name.toString().equals("GLPKHook")
               || ice.name.toString().equals("LBJ2.infer.GLPKHook"))
              && !LBJ2.Configuration.GLPKLinked)
            reportError(ice.line,
                "LBJ has not been configured properly to use the GLPK "
                + "library.  See the Users's Manual for more details.");

          if ((ice.name.toString().equals("XpressMPHook")
               || ice.name.toString().equals("LBJ2.infer.XpressMPHook"))
              && !LBJ2.Configuration.XpressMPLinked)
            reportError(ice.line,
                "LBJ has not been configured properly to use the Xpress-MP "
                + "library.  See the Users's Manual for more details.");
        }
      }
    }
    else
      d.algorithm = InferenceDeclaration.defaultInferenceConstructor; // A16
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param n  The node to process.
   **/
  public void run(InferenceDeclaration.NormalizerDeclaration n)
  {
    runOnChildren(n);

    if (n.learner != null
        && !(n.learner.typeCache instanceof ClassifierType
             && ((ClassifierType) n.learner.typeCache).isLearner()))  // B27
      reportError(n.line,
          "The left hand side of the 'normalizedby' operator must be the "
          + "name of a LBJ2.learn.Learner.");

    if (!(n.normalizer.typeCache instanceof ReferenceType)
        || !isAssignableFrom(Normalizer.class,
                ((ReferenceType) n.normalizer.typeCache).typeClass()))  // B28
      reportError(n.line,
          "The right hand side of the 'normalizedby' operator must "
          + "instantiate a LBJ2.learn.Normalizer.");
  }
}

