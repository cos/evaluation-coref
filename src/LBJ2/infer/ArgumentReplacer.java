package LBJ2.infer;

import java.util.Vector;


/**
  * Anonymous inner classes extending this class are instantiated by the code
  * generated by the LBJ compiler when creating
  * <code>FirstOrderConstraint</code> representations.  The methods of this
  * class are used to compute new values for the arguments of quantified
  * constraint expressions.
  *
  * @see    LBJ2.infer.FirstOrderConstraint
  * @author Nick Rizzolo
 **/
abstract public class ArgumentReplacer
{
  /**
    * The settings of non-quantification variables in context at the equality
    * in question.
   **/
  protected Object[] context;
  /**
    * The settings of quantification variables in context at the equality in
    * question.
   **/
  protected Vector quantificationVariables;


  /**
    * Initializing constructor.
    *
    * @param c  The context of the corresponding quantified constraint
    *           expression, except for quantification variables.
   **/
  public ArgumentReplacer(Object[] c) { context = c; }


  /**
    * Provides the settings of quantification variables.
    *
    * @param q  The settings of quantification variables.
   **/
  public void setQuantificationVariables(Vector q)
  {
    quantificationVariables = q;
  }
}

