package LBJ2.jni;

import LBJ2.infer.ILPSolver;
import java.util.*;


/**
  * Native interface to the GNU Linear Programming Kit (GLPK), designed for
  * version 4.14.  In order to use this class, GLPK 4.14 must be installed
  * such that the following statement would work in a <code>C</code> source
  * file: <br><br>
  *
  *   <code>#include &lt;glpk.h&gt;</code> <br><br>
  *
  * and such that that <code>C</code> file could be compiled and linked with
  * <code>-lglpk</code> on the command line.  Furthermore, the
  * <code>GLPKHook</code> library distributed with LBJ must be accessible in a
  * location where the JVM will search for it.
  *
  * <p> An object of this class represents a single <i>integer</i> linear
  * programming problem.  All columns added are assumed to represent Boolean
  * variables (i.e., integer variables that may take either the value 0 or the
  * value 1).
 **/
public class GLPKHook implements ILPSolver
{
  static { System.loadLibrary("LBJGLPKHook"); }


  /** Indicates whether this problem instance has been solved already. */
  private boolean solved;
  /** A <code>C</code> pointer to the <code>C</code> problem structure. */
  private int problemPointer;
  /** Whether or not to generate Gomory cuts. */
  private boolean generateCuts;
  /**
    * Diagnostic messages are written to this file when an optimal solution
    * cannot be found.
   **/
  private String debugFileName;
  /**
    * <code>true</code> if the ILP problem should be printed when invoking
    * {@link #solve()}.
   **/
  private boolean printProblem;
  /**
    * If the appropriate methods are called below, this list will contain all
    * the constraints that have been added to this solver.
   **/
  private LinkedList constraints;
  /**
    * If the appropriate methods are called below, this string will represent
    * the objective function.
   **/
  private String objectiveFunction;
  /** Represents the number of variables in the optimization problem. */
  private int objectiveCoefficients;
  /**
    * If the appropriate methods are called below, this variable will be set
    * iff this solver is solving a maximization.
   **/
  private boolean maximize;


  /**
    * Creates the problem object with a call to <code>createProblem()</code>.
    *
    * @see #createProblem()
   **/
  public GLPKHook() { this(""); }

  /**
    * Creates the problem object with a call to <code>createProblem()</code>.
    *
    * @param g  Whether or not to generate cuts.
    * @see #createProblem()
   **/
  public GLPKHook(boolean g) { this("", g); }

  /**
    * Creates the problem object with a call to <code>createProblem()</code>.
    *
    * @param g  Whether or not to generate cuts.
    * @param p  Set <code>true</code> to cause a textual representation of the
    *           problem to be printed to <code>STDOUT</code> when invoking the
    *           {@link #solve()} method before the solution is carried out.
    * @see #createProblem()
   **/
  public GLPKHook(boolean g, boolean p) { this("", g, p); }

  /**
    * Creates the problem object with a call to <code>createProblem()</code>.
    *
    * @param s  A file name for the "debug" file, in which diagnostic messages
    *           are written when an optimal solution cannot be found.
    * @see      #createProblem()
   **/
  public GLPKHook(String s) { this(s, false); }

  /**
    * Creates the problem object with a call to <code>createProblem()</code>.
    *
    * @param s  A file name for the "debug" file, in which diagnostic messages
    *           are written when an optimal solution cannot be found.
    * @param g  Whether or not to generate cuts.
    * @see      #createProblem()
   **/
  public GLPKHook(String s, boolean g) { this(s, g, false); }

  /**
    * Creates the problem object with a call to <code>createProblem()</code>.
    *
    * @param s  A file name for the "debug" file, in which diagnostic messages
    *           are written when an optimal solution cannot be found.
    * @param g  Whether or not to generate cuts.
    * @param p  Set <code>true</code> to cause a textual representation of the
    *           problem to be printed to <code>STDOUT</code> when invoking the
    *           {@link #solve()} method before the solution is carried out.
    * @see      #createProblem()
   **/
  public GLPKHook(String s, boolean g, boolean p)
  {
    if (!LBJ2.Configuration.GLPKLinked)
    {
      System.err.println(
          "LBJ ERROR: LBJ has not been configured correctly to invoke GLPK "
          + "inference.  Install GLPK 4.14 or better and re-configure.");
      System.exit(1);
    }

    debugFileName = s;
    printProblem = p;
    generateCuts = g;
    reset();
  }


  /**
    * This method clears the all constraints and variables out of the ILP
    * solver's problem representation, bringing the <code>ILPSolver</code>
    * instance back to the state it was in when first constructed.
   **/
  public void reset()
  {
    constraints = new LinkedList();
    objectiveFunction = "";
    if (problemPointer != 0) deleteProblem();
    problemPointer = createProblem();
    solved = false;
  }


  /**
    * Tests whether the problem represented by this <code>ILPSolver</code>
    * instance has been solved already.
   **/
  public boolean isSolved() { return solved; }


  /**
    * Deletes the problem object with a call to <code>deleteProblem()</code>.
    *
    * @see #deleteProblem()
   **/
  protected void finalize() throws Throwable { deleteProblem(); }


  /**
    * Instantiates the problem object in the <code>C</code> world.
    *
    * @return A pointer to the structure created.
   **/
  protected native int createProblem();


  /** Frees the problem object's memory in the <code>C</code> world. */
  protected native void deleteProblem();


  /**
    * Sets the direction of the objective function.
    *
    * @param d  <code>true</code> if the objective function is to be
    *           maximized.
   **/
  public void setMaximize(boolean d)
  {
    maximize = d;
    if (d) setMaximize();
    else setMinimize();
  }


  /** Sets the direction of the objective function to maximization. */
  protected native void setMaximize();


  /** Sets the direction of the objective function to minimization. */
  protected native void setMinimize();


  /**
    * Adds a general, multi-valued discrete variable, which is implemented as
    * a set of Boolean variables, one per value of the discrete variable, with
    * exactly one of those variables set <code>true</code> at any given time.
    * Since GLPK does not handle these sets of Boolean variables specially,
    * this method simply calls {@link #addBooleanVariable(double)} repeatedly
    * and then adds an equality constraint.
    *
    * @param c  The objective function coefficients for the new Boolean
    *           variables.
    * @return   The indexes of the newly created variables.
   **/
  public int[] addDiscreteVariable(double[] c)
  {
    int[] result = new int[c.length];
    for (int i = 0; i < c.length; ++i) result[i] = addBooleanVariable(c[i]);

    double[] coefficients = new double[c.length];
    Arrays.fill(coefficients, 1);
    addEqualityConstraint(result, coefficients, 1);
    return result;
  }


  /**
    * Adds a general, multi-valued discrete variable, which is implemented as
    * a set of Boolean variables, one per value of the discrete variable, with
    * exactly one of those variables set <code>true</code> at any given time.
    * Since GLPK does not handle these sets of Boolean variables specially,
    * this method simply calls {@link #addBooleanVariable(double)} repeatedly
    * and then adds an equality constraint.
    *
    * @param c  An array of {@link LBJ2.classify.Score}s containing the
    *           objective function coefficients for the new Boolean variables.
    * @return   The indexes of the newly created variables.
   **/
  public int[] addDiscreteVariable(LBJ2.classify.Score[] c)
  {
    int[] result = new int[c.length];
    for (int i = 0; i < c.length; ++i)
      result[i] = addBooleanVariable(c[i].score);

    double[] coefficients = new double[c.length];
    Arrays.fill(coefficients, 1);
    addEqualityConstraint(result, coefficients, 1);
    return result;
  }


  /**
    * Adds a new Boolean variable (an integer variable constrained to take
    * either the value 0 or the value 1) with the specified coefficient in the
    * objective function to the problem.
    *
    * @param c  The objective function coefficient for the new Boolean
    *           variable.
   **/
  public int addBooleanVariable(double c)
  {
    if (printProblem)
    {
      objectiveFunction += " ";
      if (c >= 0) objectiveFunction += "+";
      objectiveFunction += c + " x_" + objectiveCoefficients;
    }

    addObjectiveCoefficient(c);
    return objectiveCoefficients++;
  }


  /**
    * Adds a new Boolean variable (an integer variable constrained to take
    * either the value 0 or the value 1) with the specified coefficient in the
    * objective function to the problem.
    *
    * @param c  The objective function coefficient for the new Boolean
    *           variable.
   **/
  protected native void addObjectiveCoefficient(double c);


  /**
    * Adds a new fixed constraint to the problem.  The two array arguments
    * must be the same length, as their elements correspond to each other.
    * Variables whose coefficients are zero need not be mentioned.  Variables
    * that are mentioned must have previously been added via
    * {@link #addBooleanVariable(double)} or
    * {@link #addDiscreteVariable(double[])}.  The resulting constraint has
    * the form:
    * <blockquote> <code>x<sub>i</sub> * a = b</code> </blockquote>
    * where <code>x<sub>i</sub></code> represents the inference variables
    * whose indexes are contained in the array <code>i</code> and
    * <code>*</code> represents dot product.
    *
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The new constraint will enforce equality with this constant.
   **/
  public void addEqualityConstraint(int[] i, double[] a, double b)
  {
    if (printProblem)
    {
      String constraint = "";

      for (int j = 0; j < i.length; ++j)
      {
        constraint += " ";
        if (a[j] >= 0) constraint += "+";
        constraint += a[j] + " x_" + i[j];
      }

      constraint += " = " + b;
      constraints.add(constraint);
    }

    addFixedConstraint(i, a, b);
  }


  /**
    * Adds a new fixed constraint to the problem.  The two array arguments
    * must be the same length, as their elements correspond to each other.
    * Variables whose coefficients are zero need not be mentioned.  Variables
    * that are mentioned must have previously been added via
    * {@link #addBooleanVariable(double)} or
    * {@link #addDiscreteVariable(double[])}.  The resulting constraint has
    * the form:
    * <blockquote> <code>x<sub>i</sub> * a = b</code> </blockquote>
    * where <code>x<sub>i</sub></code> represents the inference variables
    * whose indexes are contained in the array <code>i</code> and
    * <code>*</code> represents dot product.
    *
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The new constraint will enforce equality with this constant.
   **/
  protected native void addFixedConstraint(int[] i, double[] a, double b);


  /**
    * Adds a new lower bounded constraint to the problem.  The two array
    * arguments must be the same length, as their elements correspond to each
    * other.  Variables whose coefficients are zero need not be mentioned.
    * Variables that are mentioned must have previously been added via
    * {@link #addBooleanVariable(double)} or
    * {@link #addDiscreteVariable(double[])}.  The resulting constraint has
    * the form:
    * <blockquote> <code>x<sub>i</sub> * a &gt;= b</code> </blockquote>
    * where <code>x<sub>i</sub></code> represents the inference variables
    * whose indexes are contained in the array <code>i</code> and
    * <code>*</code> represents dot product.
    *
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The lower bound for the new constraint.
   **/
  public void addGreaterThanConstraint(int[] i, double[] a, double b)
  {
    if (printProblem)
    {
      String constraint = "";

      for (int j = 0; j < i.length; ++j)
      {
        constraint += " ";
        if (a[j] >= 0) constraint += "+";
        constraint += a[j] + " x_" + i[j];
      }

      constraint += " >= " + b;
      constraints.add(constraint);
    }

    addLowerBoundedConstraint(i, a, b);
  }


  /**
    * Adds a new lower bounded constraint to the problem.  The two array
    * arguments must be the same length, as their elements correspond to each
    * other.  Variables whose coefficients are zero need not be mentioned.
    * Variables that are mentioned must have previously been added via
    * {@link #addBooleanVariable(double)} or
    * {@link #addDiscreteVariable(double[])}.  The resulting constraint has
    * the form:
    * <blockquote> <code>x<sub>i</sub> * a &gt;= b</code> </blockquote>
    * where <code>x<sub>i</sub></code> represents the inference variables
    * whose indexes are contained in the array <code>i</code> and
    * <code>*</code> represents dot product.
    *
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The lower bound for the new constraint.
   **/
  protected native void addLowerBoundedConstraint(int[] i, double[] a,
                                                  double b);


  /**
    * Adds a new upper bounded constraint to the problem.  The two array
    * arguments must be the same length, as their elements correspond to each
    * other.  Variables whose coefficients are zero need not be mentioned.
    * Variables that are mentioned must have previously been added via
    * {@link #addBooleanVariable(double)} or
    * {@link #addDiscreteVariable(double[])}.  The resulting constraint has
    * the form:
    * <blockquote> <code>x<sub>i</sub> * a &lt;= b</code> </blockquote>
    * where <code>x<sub>i</sub></code> represents the inference variables
    * whose indexes are contained in the array <code>i</code> and
    * <code>*</code> represents dot product.
    *
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The upper bound for the new constraint.
   **/
  public void addLessThanConstraint(int[] i, double[] a, double b)
  {
    if (printProblem)
    {
      String constraint = "";

      for (int j = 0; j < i.length; ++j)
      {
        constraint += " ";
        if (a[j] >= 0) constraint += "+";
        constraint += a[j] + " x_" + i[j];
      }

      constraint += " <= " + b;
      constraints.add(constraint);
    }

    addUpperBoundedConstraint(i, a, b);
  }


  /**
    * Adds a new upper bounded constraint to the problem.  The two array
    * arguments must be the same length, as their elements correspond to each
    * other.  Variables whose coefficients are zero need not be mentioned.
    * Variables that are mentioned must have previously been added via
    * {@link #addBooleanVariable(double)} or
    * {@link #addDiscreteVariable(double[])}.  The resulting constraint has
    * the form:
    * <blockquote> <code>x<sub>i</sub> * a &lt;= b</code> </blockquote>
    * where <code>x<sub>i</sub></code> represents the inference variables
    * whose indexes are contained in the array <code>i</code> and
    * <code>*</code> represents dot product.
    *
    * @param i  The indexes of the variables with non-zero coefficients.
    * @param a  The coefficients of the variables with the given indexes.
    * @param b  The upper bound for the new constraint.
   **/
  protected native void addUpperBoundedConstraint(int[] i, double[] a,
                                                  double b);


  /**
    * Simply calls {@link #nativeSolve()}, saving the result in
    * {@link #solved}.
   **/
  public boolean solve() throws Exception
  {
    if (printProblem)
    {
      StringBuffer buffer = new StringBuffer();
      write(buffer);
      System.out.print(buffer);
    }

    return (solved = nativeSolve());
  }


  /**
    * Invokes the <code>lpx_intopt</code> GLPK library function to solve the
    * integer linear program.  To produce diagnostic messages regarding a
    * return value of <code>false</code> from this method, use the constructor
    * of this class that takes a <code>String</code> argument.
    *
    * @return <code>true</code> iff an optimal integer solution was found and
    *         no problems were encountered while running the algorithms.
   **/
  protected native boolean nativeSolve();


  /**
    * When the problem has been solved, use this method to retrieve the value
    * of any Boolean inference variable.  The result of this method is
    * undefined when the problem has not yet been solved.
    *
    * @param index  The index of the variable whose value is requested.
    * @return       The value of the variable.
   **/
  public boolean getBooleanValue(int index)
  {
    return columnPrimalValueOf(index) > 0.5;
  }


  /**
    * Returns the value of the specified variable in the primal solution.
    *
    * @param i  The index of the variable whose value is to be returned.
    * @return   The value of the specified variable in the primal solution.
   **/
  public native double columnPrimalValueOf(int i);


  /**
    * Writes the optimization problem that this solver represents into the
    * specified buffer, assuming the appropriate member methods were called to
    * remember this information.
    *
    * @param buffer The buffer to write in.
   **/
  public void write(StringBuffer buffer)
  {
    if (maximize) buffer.append("max");
    else buffer.append("min");
    buffer.append(objectiveFunction);
    buffer.append("\n");

    for (Iterator I = constraints.iterator(); I.hasNext(); )
    {
      buffer.append(I.next());
      buffer.append("\n");
    }
  }
}

