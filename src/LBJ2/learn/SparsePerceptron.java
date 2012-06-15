package LBJ2.learn;

import java.io.*;


/**
  * Simple sparse Perceptron implementation.  It is assumed that
  * {@link Learner#labeler} is a single discrete classifier that produces the
  * same feature for every example object and that the values that feature may
  * take are available through the
  * {@link LBJ2.classify.Classifier#allowableValues()} method.  The second
  * value returned from {@link LBJ2.classify.Classifier#allowableValues()} is
  * treated as "positive", and it is assumed there are exactly 2 allowable
  * values.  Assertions will produce error messages if these assumptions do
  * not hold.
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
  * @author Nick Rizzolo
 **/
public class SparsePerceptron extends LinearThresholdUnit
{
  /** Default value for {@link #learningRate}. */
  public static final double defaultLearningRate = 0.1;


  /**
    * The rate at which weights are updated; default
    * {@link #defaultLearningRate}.
   **/
  protected double learningRate;


  /**
    * The learning rate and threshold take default values, while the name of
    * the classifier gets the empty string.
   **/
  public SparsePerceptron() { this(""); }

  /**
    * Sets the learning rate to the specified value, and the threshold takes
    * the default, while the name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
   **/
  public SparsePerceptron(double r) { this("", r); }

  /**
    * Sets the learning rate and threshold to the specified values, while the
    * name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
   **/
  public SparsePerceptron(double r, double t)
  {
    this("", r, t);
  }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness, while the name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired thickness.
   **/
  public SparsePerceptron(double r, double t, double pt)
  {
    this("", r, t, pt);
  }

  /**
    * Use this constructor to fit a thick separator, where the positive and
    * negative sides of the hyperplane will be given the specified separate
    * thicknesses, while the name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  public SparsePerceptron(double r, double t, double pt, double nt)
  {
    this("", r, t, pt, nt);
  }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}, while the name of the classifier gets the
    * empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An empty sparse weight vector.
   **/
  public SparsePerceptron(double r, double t, double pt, double nt,
                          SparseWeightVector v)
  {
    this("", r, t, pt, nt, v);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparsePerceptron.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public SparsePerceptron(Parameters p) { this("", p); }


  /**
    * The learning rate and threshold take default values.
    *
    * @param n  The name of the classifier.
   **/
  public SparsePerceptron(String n) { this(n, defaultLearningRate); }

  /**
    * Sets the learning rate to the specified value, and the threshold takes
    * the default.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
   **/
  public SparsePerceptron(String n, double r)
  {
    this(n, r, LinearThresholdUnit.defaultThreshold);
  }

  /**
    * Sets the learning rate and threshold to the specified values.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
   **/
  public SparsePerceptron(String n, double r, double t)
  {
    this(n, r, t, LinearThresholdUnit.defaultThickness);
  }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired thickness.
   **/
  public SparsePerceptron(String n, double r, double t, double pt)
  {
    this(n, r, t, pt, pt);
  }

  /**
    * Use this constructor to fit a thick separator, where the positive and
    * negative sides of the hyperplane will be given the specified separate
    * thicknesses.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  public SparsePerceptron(String n, double r, double t, double pt, double nt)
  {
    this(n, r, t, pt, nt,
         (SparseWeightVector)
         LinearThresholdUnit.defaultWeightVector.clone());
  }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An empty sparse weight vector.
   **/
  public SparsePerceptron(String n, double r, double t, double pt, double nt,
                          SparseWeightVector v)
  {
    super(n, t, pt, nt);
    learningRate = r;
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparsePerceptron.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public SparsePerceptron(String n, Parameters p)
  {
    super(n, p);
    learningRate = p.learningRate;
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
    * @param r  The new value for {@link #learningRate}.
   **/
  public void setLearningRate(double r) { learningRate = r; }


  /**
    * Scales the feature vector produced by the extractor by the learning rate
    * and adds it to the weight vector.
    *
    * @param example  The example object.
   **/
  public void promote(Object example)
  {
    weightVector.scaledAdd(extractor.classify(example), learningRate,
                           initialWeight);
    bias += learningRate;
  }


  /**
    * Scales the feature vector produced by the extractor by the learning rate
    * and subtracts it from the weight vector.
    *
    * @param example  The example object.
   **/
  public void demote(Object example)
  {
    weightVector.scaledAdd(extractor.classify(example), -learningRate,
                           initialWeight);
    bias -= learningRate;
  }


  /**
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link #learningRate}, {@link LinearThresholdUnit#initialWeight},
    * {@link LinearThresholdUnit#threshold},
    * {@link LinearThresholdUnit#positiveThickness},
    * {@link LinearThresholdUnit#negativeThickness}, and finally
    * {@link LinearThresholdUnit#bias}.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out)
  {
    out.println(name + ": " + learningRate + ", " + initialWeight + ", "
                + threshold + ", " + positiveThickness + ", "
                + negativeThickness + ", " + bias);
    weightVector.write(out);
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone()
  {
    SparsePerceptron clone = null;

    try { clone = (SparsePerceptron) super.clone(); }
    catch (Exception e)
    {
      System.err.println("Error cloning SparsePerceptron: " + e);
      System.exit(1);
    }

    if (weightVector != null)
      clone.weightVector = (SparseWeightVector) weightVector.clone();
    return clone;
  }


  /**
    * Simply a container for all of {@link SparsePerceptron}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends LinearThresholdUnit.Parameters
  {
    /**
      * The rate at which weights are updated; default
      * {@link #defaultLearningRate}.
     **/
    public double learningRate;


    /** Sets all the default values. */
    public Parameters()
    {
      learningRate = defaultLearningRate;
    }
  }
}

