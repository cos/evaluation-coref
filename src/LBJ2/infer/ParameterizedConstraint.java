package LBJ2.infer;

import LBJ2.classify.*;


/**
  * This class represents an LBJ constraint as it appears in a source file.
  * When given its input object, an object of this class generates objects of
  * type <code>FirstOrderConstraint</code>.
  * <code>ParameterizedConstraint</code>s are also Boolean
  * <code>Classifier</code>s.
  *
  * <p> <code>ParameterizedConstraint</code> depends on extending classes to
  * override the <code>discreteValue(Object)</code> method so that it returns
  * <code>"true"</code> or <code>"false"</code> as appropriate.  When this is
  * done, there is no need to override the <code>classify(Object)</code>
  * method.
  *
  * @author Nick Rizzolo
 **/
public abstract class ParameterizedConstraint extends Classifier
{
  /** Default constructor. */
  public ParameterizedConstraint() { }

  /**
    * Initializes the name.
    *
    * @param n  The name of this constraint.
   **/
  public ParameterizedConstraint(String n) { super(n); }


  /**
    * This method makes one or more decisions about a single object, returning
    * those decisions as <code>Feature</code>s in a vector.
    *
    * @param o  The object to make decisions about.
    * @return   A vector of <code>Feature</code>s about the input object.
   **/
  public FeatureVector classify(Object o)
  {
    String value = discreteValue(o);
    short index = (short) (value.equals("true") ? 1 : 0);
    return
      new FeatureVector(
          new DiscreteFeature(containingPackage, name, value, index,
                              (short) 2));
  }


  /**
    * Returns the array of allowable values that a feature returned by this
    * classifier may take.  If the array has length 0, it means either that
    * the feature has discrete type and allowable values were not specified or
    * that the feature has real or mixed type.  The default return value of
    * this method is a 0 length array.<br><br>
    *
    * This method should be overridden by derived classes.
    *
    * @return The allowable values that a feature returned by this classifier
    *         may take.
   **/
  public String[] allowableValues() { return DiscreteFeature.BooleanValues; }


  /**
    * This method builds a first order constraint based on the given input
    * object.
    *
    * @param o  The object to build a constraint with respect to.
    * @return   A first order constraint.
   **/
  public abstract FirstOrderConstraint makeConstraint(Object o);
}

