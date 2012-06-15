package LBJ2.learn;

import java.io.*;
import LBJ2.classify.*;


/**
  * Gradient descent is a batch learning algorithm for function approximation
  * in which the learner tries to follow the gradient of the error function to
  * the solution of minimal error.  This implementation is a stochastic
  * approximation to gradient descent in which the approximated function is
  * assumed to have linear form.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.StochasticGradientDescent.Parameters Parameters} as
  * input.  The documentation in each member field in this class indicates the
  * default value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.StochasticGradientDescent.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Nick Rizzolo
 **/
public class StochasticGradientDescent extends Learner
{
  /** Default value for {@link #learningRate}. */
  public static final double defaultLearningRate = 0.1;
  /** Default for {@link #weightVector}. */
  public static final SparseWeightVector defaultWeightVector =
    new SparseWeightVector();


  /** The hypothesis vector; default {@link #defaultWeightVector}. */
  protected SparseWeightVector weightVector;
  /**
    * The bias is stored here rather than as an element of the weight vector.
   **/
  protected double bias;
  /**
    * The rate at which weights are updated; default
    * {@link #defaultLearningRate}.
   **/
  protected double learningRate;


  /**
    * The learning rate takes the default value, while the name of the
    * classifier gets the empty string.
   **/
  public StochasticGradientDescent() { this(""); }

  /**
    * Sets the learning rate to the specified value, while the name of the
    * classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
   **/
  public StochasticGradientDescent(double r) { this("", r); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link StochasticGradientDescent.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public StochasticGradientDescent(Parameters p) { this("", p); }

  /**
    * The learning rate takes the default value.
    *
    * @param n  The name of the classifier.
   **/
  public StochasticGradientDescent(String n) { this(n, defaultLearningRate); }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
   **/
  public StochasticGradientDescent(String n, double r)
  {
    super(n);
    weightVector = (SparseWeightVector) defaultWeightVector.clone();
    learningRate = r;
    bias = 0;
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link StochasticGradientDescent.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public StochasticGradientDescent(String n, Parameters p)
  {
    super(n);
    weightVector = p.weightVector;
    learningRate = p.learningRate;
    bias = 0;
  }


  /**
    * Returns the current value of the {@link #learningRate} variable.
    *
    * @return The value of the {@link #learningRate} variable.
   **/
  public double getLearningRate() { return learningRate; }


  /**
    * Sets the {@link #learningRate} member variable to the specified
    * value.
    *
    * @param t  The new value for {@link #learningRate}.
   **/
  public void setLearningRate(double t) { learningRate = t; }


  /** Resets the weight vector to all zeros. */
  public void forget()
  {
    weightVector.clear();
    bias = 0;
  }


  /**
    * Returns a string describing the output feature type of this classifier.
    *
    * @return <code>"real"</code>
   **/
  public String getOutputType() { return "real"; }


  /**
    * Trains the learning algorithm given an object as an example.
    *
    * @param example  An example of the desired learned classifier's behavior.
   **/
  public void learn(Object example)
  {
    Feature l = labeler.classify(example).firstFeature();
    assert l != null
      : "The label classifier for stochastic gradient descent must always "
        + "produce the same feature.";
    assert l instanceof RealFeature
      : "The label classifier for stochastic gradient descent must always "
        + "produce a single real feature.";

    RealFeature labelFeature = (RealFeature) l;
    double multiplier =
      learningRate * (labelFeature.getValue()
                      - weightVector.dot(extractor.classify(example)) - bias);
    weightVector.scaledAdd(extractor.classify(example), multiplier);
    bias += multiplier;
  }


  /**
    * Since this algorithm returns a real feature, it does not return scores.
    *
    * @param example  The object to make decisions about.
    * @return         <code>null</code>
   **/
  public ScoreSet scores(Object example) { return null; }


  /**
    * Simply computes the dot product of the weight vector and the feature
    * vector extracted from the example object.
    *
    * @param example  The example to be evaluated.
    * @return         The computed feature (in a vector).
   **/
  public FeatureVector classify(Object example)
  {
    return
      new FeatureVector(
        new RealFeature(containingPackage, name,
                        weightVector.dot(extractor.classify(example))
                        + bias));
  }


  /**
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link #learningRate} and {@link #bias}.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out)
  {
    out.println(name + ": " + learningRate + ", " + bias);
    weightVector.write(out);
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone()
  {
    StochasticGradientDescent clone = null;

    try { clone = (StochasticGradientDescent) super.clone(); }
    catch (Exception e)
    {
      System.err.println("Error cloning StochasticGradientDescent: " + e);
      System.exit(1);
    }

    clone.weightVector = (SparseWeightVector) weightVector.clone();
    return clone;
  }


  /**
    * Simply a container for all of {@link StochasticGradientDescent}'s
    * configurable parameters.  Using instances of this class should make code
    * more readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends LBJ2.learn.Parameters
  {
    /**
      * The hypothesis vector; default
      * {@link StochasticGradientDescent#defaultWeightVector}.
     **/
    public SparseWeightVector weightVector;
    /**
      * The rate at which weights are updated; default
      * {@link #defaultLearningRate}.
     **/
    public double learningRate;


    /** Sets all the default values. */
    public Parameters()
    {
      weightVector = (SparseWeightVector) defaultWeightVector.clone();
      learningRate = defaultLearningRate;
    }
  }
}

