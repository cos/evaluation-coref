package LBJ2.learn;

import java.io.*;
import java.util.*;
import LBJ2.classify.*;


/**
  * The Binary MIRA learning algorithm implementation.  This algorithm
  * operates very similarly to {@link SparsePerceptron} with a thick
  * separator, except the learning rate is a function of each training
  * example's margin.  When the weight vector has made a mistake, the full
  * {@link SparsePerceptron#learningRate} will be used.  When the weight
  * vector did not make a mistake, {@link SparsePerceptron#learningRate} is
  * multiplied by the following value before the update takes place.
  *
  * <p>
  * <blockquote>
  * <i>(beta/2 - y(w*x)) / ||x||<sup>2</sup></i>
  * </blockquote>
  *
  * <p> In the expression above, <i>w</i> is the weight vector, <i>y</i>
  * represents the label of the example vector <i>x</i>, <i>*</i> stands for
  * inner product, and <i>beta</i> is a user supplied parameter.  If this
  * expression turns out to be non-positive (i.e., if <i>y(w*x) >=
  * beta/2</i>), no update is made for that example.
  *
  * <p> It is assumed that {@link Learner#labeler} is a single discrete
  * classifier that produces the same feature for every example object and
  * that the values that feature may take are available through the
  * {@link Classifier#allowableValues()} method.  The second value returned
  * from {@link Classifier#allowableValues()} is treated as "positive", and it
  * is assumed there are exactly 2 allowable values.  Assertions will produce
  * error messages if these assumptions do not hold.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.SparsePerceptron.Parameters Parameters} as input.
  * The documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.SparsePerceptron.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Arindam Saha
 **/
public class BinaryMIRA extends SparsePerceptron {
	/**
    * Used to decide if two values are nearly equal to each other.
    * @see #nearlyEqualTo(double,double)
   **/
	public static final double TOLERANCE = 0.000000001;
  /** Default value for {@link #beta}. */
  public static final double defaultBeta = 2;


	/**
	 * The user supplied learning algorithm parameter; default
   * {@link #defaultBeta}. The learning rate changes as a function of
   * <code>beta</code>.
	 */
	protected double beta;
  /**
    * A copy of the original learning rate value is stored here so that
    * {@link SparsePerceptron#learningRate} can be modified without losing it.
   **/
  protected double saveLearningRate;


	/**
	 * The learning rate and beta take default values while the name of
	 * the classifier takes the empty string.
	 */
	public BinaryMIRA() { this(""); }

	/**
	 * Sets the learning rate to the specified value, and beta to the
	 * default, while the name of the classifier takes the empty string.
	 *
	 * @param r The desired learning rate value.
	 */
	public BinaryMIRA(double r) { this("", r); }

	/**
	 * Sets the learning rate and beta to the specified values, while
	 * the name of the classifier takes the empty string.
	 *
	 * @param r The desired learning rate value.
	 * @param B the desired beta value.
	 */
	public BinaryMIRA(double r, double B) { this("", r, B); }

	/**
	 * Sets the learning rate, beta and the weight vector to the specified
	 * values.
	 *
	 * @param r The desired learning rate.
	 * @param B The desired beta value.
	 * @param v The desired weight vector.
	 */
	public BinaryMIRA(double r, double B, SparseWeightVector v) {
    this("", r, B, v);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link BinaryMIRA.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public BinaryMIRA(Parameters p) { this("", p); }


	/**
	 * Sets the name of the classifier to the specified value, while the
	 * learning rate and beta take default values.
	 *
	 * @param n The name of the classifier.
	 */
	public BinaryMIRA(String n) {
    this(n, SparsePerceptron.defaultLearningRate);
  }

	/**
	 * Sets the name of the classifier and learning rate to the specified
	 * values, while beta takes the default value.
	 *
	 * @param n The name of the classifier.
	 * @param r The desired learning rate.
	 */
	public BinaryMIRA(String n, double r) { this(n, r, defaultBeta); }

	/**
	 * Sets the name of the classifier, the learning rate and beta to
	 * the specified values.
	 *
	 * @param n The name of the classifier.
	 * @param r The desired learning rate.
	 * @param B The desired beta value.
	 */
	public BinaryMIRA(String n, double r, double B) {
    this(n, r, B,
         (SparseWeightVector)
         LinearThresholdUnit.defaultWeightVector.clone());
  }

	/**
	 * Sets the name of the classifier, the learning rate, beta and the
   * weight vector to the specified values.  Use this constructor to specify
   * an alternative subclass of {@link SparseWeightVector}.
	 *
	 * @param n The name of the classifier.
	 * @param r The desired learning rate.
	 * @param B The desired beta value.
	 * @param v The desired weight vector.
	 */
	public BinaryMIRA(String n, double r, double B, SparseWeightVector v) {
		super(n, r, LinearThresholdUnit.defaultThreshold,
          LinearThresholdUnit.defaultThickness,
          LinearThresholdUnit.defaultThickness, v);
		beta = B;
    saveLearningRate = r;
	}

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link BinaryMIRA.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public BinaryMIRA(String n, Parameters p) {
    super(n, p);
    beta = p.beta;
    saveLearningRate = learningRate;
  }


	/**
	  * Returns the current value of the {@link #beta} member variable.
	  *
	  * @return The value of the {@link #beta} variable.
	 **/
	public double getBeta() { return beta; }


	/**
	  * Sets the {@link #beta} member variable to the specified value.
	  *
    * @param B The new value for {@link #beta}.
	 **/
	public void setBeta(double B) { beta = B; }


	/**
	 * Evaluates the example object with the
   * {@link LinearThresholdUnit#score(Object)}, updates the
	 * learning rate, and finally updates
   * {@link LinearThresholdUnit#weightVector} as described above.
   *
	 * @param example The example object
	 */
	public void learn(Object example) {
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
	    updateLearningRate(example, s, label);

	    if (!nearlyEqualTo(learningRate, 0.0)) {
	    	if (label) promote(example);
	    	else demote(example);
	    }
	}


	/**
	 * Determines if <code>a</code> is nearly equal to <code>b</code> based on
	 * the value of the <code>TOLERANCE</code> member variable.
	 *
	 * @param a The first value
	 * @param b The second value
	 * @return 	True if they are nearly equal, false otherwise.
	 */
	private static boolean nearlyEqualTo(double a, double b) {
		return -TOLERANCE < a - b && a - b < TOLERANCE;
	}


	/**
	 * Updates the {@link #learningRate} member variable.
	 *
	 * @param example The example object
	 * @param s       The score for the example object
	 * @param label   The label
	 */
	private void updateLearningRate(Object example, double s, boolean label) {
		double labelVal = label? 1: -1;

		double x = (beta / 2 - labelVal * s)
               / (extractor.classify(example).L2NormSquared() + 1);

		if (x < 0) learningRate = 0;
		else if (x < 1 || nearlyEqualTo(x, 1.0)) learningRate = x;
		else learningRate = 1;
    learningRate *= saveLearningRate;
	}


	/**
	  * Writes the algorithm's internal representation as text.  In the first
	  * line of output, the name of the classifier is printed, followed by
	  * {@link SparsePerceptron#learningRate}, {@link #beta},
    * {@link LinearThresholdUnit#initialWeight},
    * {@link LinearThresholdUnit#threshold},
	  * {@link LinearThresholdUnit#positiveThickness},
    * {@link LinearThresholdUnit#negativeThickness},
	  * and finally {@link LinearThresholdUnit#bias}.
	  *
	  * @param out  The output stream.
	 **/
	public void write(PrintStream out) {
		out.println(name + ": " + learningRate + ", " + beta + ", "
                + initialWeight + ", " + threshold + ", " + positiveThickness
                + ", " + negativeThickness + ", " + bias);
		weightVector.write(out);
	}


  /**
    * Simply a container for all of {@link BinaryMIRA}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends SparsePerceptron.Parameters {
    /**
     * The user supplied learning algorithm parameter; default
     * {@link #defaultBeta}. The learning rate changes as a function of
     * <code>beta</code>.
     */
    public double beta;


    /** Sets all the default values. */
    public Parameters() {
      beta = defaultBeta;
    }
  }
}



