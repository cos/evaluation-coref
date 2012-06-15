package LBJ2.learn;

import LBJ2.classify.*;


/**
  * A <code>LinearThresholdUnit</code> is a {@link Learner} for binary
  * classification in which a score is computed as a linear function a
  * <i>weight vector</i> and the input example, and the decision is made by
  * comparing the score to some threshold quantity.  Deriving a linear
  * threshold algorithm from this class gives the programmer more flexible
  * access to the score it computes as well as its promotion and demotion
  * methods (if it's on-line).
  *
  * <p> On-line, mistake driven algorithms derived from this class need only
  * override the {@link #promote(Object)}, and {@link #demote(Object)}
  * methods, assuming the score returned by the {@link #score(Object)} method
  * need only be compared with {@link #threshold} to make a prediction.
  * Otherwise, the {@link #classify(Object)} method also needs to be
  * overridden.  If the algorithm is not mistake driven, the
  * {@link #learn(Object)} method needs to be overridden as well.
  *
  * <p> It is assumed that {@link Learner#labeler} is a single discrete
  * classifier that produces the same feature for every example object and
  * that the values that feature may take are available through the
  * {@link Classifier#allowableValues()} method.  The first value returned
  * from {@link Classifier#allowableValues()} is treated as "negative", and it
  * is assumed there are exactly 2 allowable values.  Assertions will produce
  * error messages if these assumptions do not hold.
  *
  * <p> Fitting a "thick separator" instead of just a hyperplane is also
  * supported through this class.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.LinearThresholdUnit.Parameters Parameters} as input.
  * The documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.LinearThresholdUnit.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Nick Rizzolo
 **/
public abstract class LinearThresholdUnit extends Learner
{
  /** Default for {@link #initialWeight}. */
  public static final double defaultInitialWeight = 0;
  /** Default for {@link #threshold}. */
  public static final double defaultThreshold = 0;
  /** Default for {@link #positiveThickness}. */
  public static final double defaultThickness = 0;
  /** Default for {@link #weightVector}. */
  public static final SparseWeightVector defaultWeightVector =
    new SparseWeightVector();


  /** The LTU's weight vector; default is an empty vector. */
  protected SparseWeightVector weightVector;
  /**
    * The weight associated with a feature when first added to the vector;
    * default {@link #defaultInitialWeight}.
   **/
  protected double initialWeight;
  /**
    * The score is compared against this value to make predictions; default
    * {@link LinearThresholdUnit#defaultThreshold}.
   **/
  protected double threshold;
  /**
    * The bias is stored here rather than as an element of the weight vector.
   **/
  protected double bias;
  /**
    * The thickness of the hyperplane on the positive side; default
    * {@link #defaultThickness}.
   **/
  protected double positiveThickness;
  /**
    * The thickness of the hyperplane on the negative side; default equal to
    * {@link #positiveThickness}.
   **/
  protected double negativeThickness;
  /** The label producing classifier's allowable values. */
  protected String[] allowableValues;


  /**
    * Default constructor.  Sets the threshold, positive thickness, and
    * negative thickness to their default values.
    *
    * @param n  The name of the classifier.
   **/
  protected LinearThresholdUnit(String n) { this(n, defaultThreshold); }

  /**
    * Initializing constructor.  Sets the threshold to the specified value,
    * while the positive and negative thicknesses get their defaults.
    *
    * @param n  The name of the classifier.
    * @param t  The desired value for the threshold.
   **/
  protected LinearThresholdUnit(String n, double t)
  {
    this(n, t, defaultThickness);
  }

  /**
    * Initializing constructor.  Sets the threshold and positive thickness to
    * the specified values, and the negative thickness is set to the same
    * value as the positive thickness.
    *
    * @param n  The name of the classifier.
    * @param t  The desired value for the threshold.
    * @param pt The desired thickness.
   **/
  protected LinearThresholdUnit(String n, double t, double pt)
  {
    this(n, t, pt, pt);
  }

  /**
    * Initializing constructor.  Sets the threshold, positive thickness, and
    * negative thickness to the specified values.
    *
    * @param n  The name of the classifier.
    * @param t  The desired value for the threshold.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  protected LinearThresholdUnit(String n, double t, double pt, double nt)
  {
    this(n, t, pt, nt, (SparseWeightVector) defaultWeightVector.clone());
  }

  /**
    * Initializing constructor.  Sets the threshold, positive thickness, and
    * negative thickness to the specified values.
    *
    * @param n  The name of the classifier.
    * @param t  The desired value for the threshold.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An initial weight vector.
   **/
  protected LinearThresholdUnit(String n, double t, double pt, double nt,
                                SparseWeightVector v)
  {
    super(n);
    weightVector = v;
    initialWeight = defaultInitialWeight;
    threshold = t;
    bias = defaultInitialWeight;
    positiveThickness = pt;
    negativeThickness = nt;
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link LinearThresholdUnit.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  protected LinearThresholdUnit(String n, Parameters p)
  {
    super(n);
    weightVector = p.weightVector;
    initialWeight = p.initialWeight;
    threshold = p.threshold;
    bias = p.initialWeight;
    positiveThickness = p.thickness + p.positiveThickness;
    negativeThickness = p.thickness + p.negativeThickness;
  }


  /**
    * Sets the labels list.
    *
    * @param l  A new label producing classifier.
   **/
  public void setLabeler(Classifier l)
  {
    if (!(l == null || l.allowableValues().length == 2))
    {
      System.err.println(
          "Error: " + name
          + ": An LTU must be given a single binary label classifier.");
      System.exit(1);
    }

    super.setLabeler(l);
    allowableValues = l == null ? null : l.allowableValues();
  }


  /**
    * Returns the current value of the {@link #initialWeight} variable.
    *
    * @return The value of the {@link #initialWeight} variable.
   **/
  public double getInitialWeight() { return initialWeight; }


  /**
    * Sets the {@link #initialWeight} member variable to the specified value.
    *
    * @param w  The new value for {@link #initialWeight}.
   **/
  public void setInitialWeight(double w) { initialWeight = w; }


  /**
    * Returns the current value of the {@link #threshold} variable.
    *
    * @return The value of the {@link #threshold} variable.
   **/
  public double getThreshold() { return threshold; }


  /**
    * Sets the {@link #threshold} member variable to the specified value.
    *
    * @param t  The new value for {@link #threshold}.
   **/
  public void setThreshold(double t) { threshold = t; }


  /**
    * Returns the current value of the {@link #positiveThickness} variable.
    *
    * @return The value of the {@link #positiveThickness} variable.
   **/
  public double getPositiveThickness() { return positiveThickness; }


  /**
    * Sets the {@link #positiveThickness} member variable to the specified
    * value.
    *
    * @param t  The new value for {@link #positiveThickness}.
   **/
  public void setPositiveThickness(double t)
  {
    positiveThickness = t;
  }


  /**
    * Returns the current value of the {@link #negativeThickness} variable.
    *
    * @return The value of the {@link #negativeThickness} variable.
   **/
  public double getNegativeThickness() { return negativeThickness; }


  /**
    * Sets the {@link #negativeThickness} member variable to the specified
    * value.
    *
    * @param t  The new value for {@link #negativeThickness}.
   **/
  public void setNegativeThickness(double t) { negativeThickness = t; }


  /**
    * Sets the {@link #positiveThickness} and {@link #negativeThickness}
    * member variables to the specified value.
    *
    * @param t  The new thickness value.
   **/
  public void setThickness(double t)
  {
    positiveThickness = negativeThickness = t;
  }


  /**
    * Returns the array of allowable values that a feature returned by this
    * classifier may take.
    *
    * @return If a labeler has not yet been established for this LTU,
    *         <code>new String[]{ "*", "*" }</code> is returned, which
    *         indicates to the compiler that classifiers derived from this
    *         learner will return features that take one of two values that
    *         are specified in the source code.  Otherwise, the allowable
    *         values of the labeler are returned.
   **/
  public String[] allowableValues()
  {
    if (allowableValues == null) return new String[]{ "*", "*" };
    return allowableValues;
  }


  /**
    * The default training algorithm for a linear threshold unit consists of
    * evaluating the example object with the {@link #score(Object)} method and
    * {@link #threshold}, checking the result of evaluation against the label,
    * and, if they are different, promoting when the label is positive or
    * demoting when the label is negative.
    *
    * <p> This method does not call {@link #classify(Object)}; it calls
    * {@link #score(Object)} directly.
    *
    * @param example  The example object.
   **/
  public void learn(Object example)
  {
    Feature l = labeler.classify(example).firstFeature();
    assert l != null
      : "An LTU's label classifier must always produce the same feature.";
    assert l instanceof DiscreteFeature
      : "An LTU's label classifier must always produce a single discrete "
        + "feature.";

    DiscreteFeature labelFeature = (DiscreteFeature) l;
    assert labelFeature.valueEquals(allowableValues[0])
           || labelFeature.valueEquals(allowableValues[1])
      : "Example has unallowed label value.";

    boolean label = labelFeature.getValueIndex() == 1
                    || labelFeature.getValueIndex() == -1
                       && labelFeature.valueEquals(allowableValues[1]);

    double s = score(example);
    if (label && s < threshold + positiveThickness) promote(example);
    if (!label && s >= threshold - negativeThickness) demote(example);
  }


  /**
    * An LTU returns two scores; one for the negative classification and one
    * for the positive classification.  By default, the score for the positive
    * classification is the result of {@link #score(Object)} minus the
    * {@link #threshold}, and the score for the negative classification is the
    * opposite of the positive classification's score.
    *
    * @param example  The object to make decisions about.
    * @return         Two scores as described above.
   **/
  public ScoreSet scores(Object example)
  {
    double s = score(example) - threshold;
    ScoreSet result = new ScoreSet();
    result.put(allowableValues[0], -s);
    result.put(allowableValues[1], s);
    return result;
  }


  /**
    * The default evaluation method simply computes the score for the example
    * and returns a {@link DiscreteFeature} set to either the second value
    * from the label classifier's array of allowable values if the score is
    * greater than or equal to {@link #threshold} or the first otherwise.
    *
    * @param example  The example to be evaluated.
    * @return         The computed feature (in a vector).
   **/
  public FeatureVector classify(Object example)
  {
    short index = score(example) >= threshold ? (short) 1 : (short) 0;
    return
      new FeatureVector(
        new DiscreteFeature(containingPackage, name, allowableValues[index],
                            index, (short) 2));
  }


  /**
    * Computes the score for the specified example vector which will be
    * thresholded to make the binary classification.
    *
    * @param example  The example object.
    * @return         The score for the given example vector.
   **/
  public double score(Object example)
  {
    return weightVector.dot(extractor.classify(example), initialWeight)
           + bias;
  }


  /**
    * Resets the weight vector to associate the default weight with all
    * features.
   **/
  public void forget()
  {
    weightVector.clear();
    bias = initialWeight;
  }


  /**
    * If the <code>LinearThresholdUnit</code> is mistake driven, this method
    * should be overridden and used to update the internal representation when
    * a mistake is made on a positive example.
    *
    * @param example  The example object.
   **/
  public abstract void promote(Object example);


  /**
    * If the <code>LinearThresholdUnit</code> is mistake driven, this method
    * should be overridden and used to update the internal representation when
    * a mistake is made on a negative example.
    *
    * @param example  The example object.
   **/
  public abstract void demote(Object example);


  /**
    * Simply a container for all of {@link LinearThresholdUnit}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends LBJ2.learn.Parameters
  {
    /** The LTU's weight vector; default is an empty vector. */
    public SparseWeightVector weightVector;
    /**
      * The weight associated with a feature when first added to the vector;
      * default {@link LinearThresholdUnit#defaultInitialWeight}.
     **/
    public double initialWeight;
    /**
      * The score is compared against this value to make predictions; default
      * {@link LinearThresholdUnit#defaultThreshold}.
     **/
    public double threshold;
    /**
      * This thickness will be added to both {@link #positiveThickness} and
      * {@link #negativeThickness}; default
      * {@link LinearThresholdUnit#defaultThickness}.
     **/
    public double thickness;
    /** The thickness of the hyperplane on the positive side; default 0. */
    public double positiveThickness;
    /** The thickness of the hyperplane on the negative side; default 0. */
    public double negativeThickness;


    /** Sets all the default values. */
    public Parameters()
    {
      weightVector = (SparseWeightVector) defaultWeightVector.clone();
      initialWeight = defaultInitialWeight;
      threshold = defaultThreshold;
      thickness = defaultThickness;
    }
  }
}

