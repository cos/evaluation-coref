package LBJ2.learn;

import java.io.*;


/**
  * Simple sparse Winnow implementation.  It is assumed that
  * {@link Learner#labeler} is a single discrete classifier whose returned
  * feature values are available through the
  * {@link LBJ2.classify.Classifier#allowableValues()} method.  The second
  * value returned from {@link LBJ2.classify.Classifier#allowableValues()} is
  * treated as "positive", and it is assumed there are exactly 2 allowable
  * values.  Assertions will produce error messages if these assumptions do
  * not hold.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.SparseWinnow.Parameters Parameters} as input.  The
  * documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.SparseWinnow.Parameters Parameters} class indicates the
  * default value of the parameter when using the latter type of constructor.
  *
  * @author Nick Rizzolo
 **/
public class SparseWinnow extends LinearThresholdUnit
{
  /** Default for {@link #alpha}. */
  public static final double defaultAlpha = 2;
  /** Default for {@link LinearThresholdUnit#threshold}. */
  public static final double defaultThreshold = 16;
  /** Default for {@link LinearThresholdUnit#initialWeight}. */
  public static final double defaultInitialWeight = 1;


  /**
    * The rate at which weights are promoted; default {@link #defaultAlpha}.
   **/
  protected double alpha;
  /**
    * The rate at which weights are demoted; default equal to <code>1 /</code>
    * {@link #alpha}.
   **/
  protected double beta;


  /**
    * {@link #alpha}, {@link #beta}, and {@link LinearThresholdUnit#threshold}
    * take default values, while the name of the classifier gets the empty
    * string.
   **/
  public SparseWinnow() { this(""); }

  /**
    * Sets {@link #alpha} to the specified value, {@link #beta} to 1 /
    * {@link #alpha}, and the {@link LinearThresholdUnit#threshold} takes the
    * default, while the name of the classifier gets the empty string.
    *
    * @param a  The desired value of the promotion parameter.
   **/
  public SparseWinnow(double a) { this("", a); }

  /**
    * Sets {@link #alpha} and {@link #beta} to the specified values, and the
    * {@link LinearThresholdUnit#threshold} takes the default, while the name
    * of the classifier gets the empty string.
    *
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
   **/
  public SparseWinnow(double a, double b) { this("", a, b); }

  /**
    * Sets {@link #alpha}, {@link #beta}, and
    * {@link LinearThresholdUnit#threshold} to the specified values, while the
    * name of the classifier gets the empty string.
    *
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
   **/
  public SparseWinnow(double a, double b, double t)
  {
    this("", a, b, t);
  }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness, while the name of the classifier gets the empty string.
    *
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
   **/
  public SparseWinnow(double a, double b, double t, double pt)
  {
    this("", a, b, t, pt);
  }

  /**
    * Use this constructor to fit a thick separator, where the positive and
    * negative sides of the hyperplane will be given the specified separate
    * thicknesses, while the name of the classifier gets the empty string.
    *
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  public SparseWinnow(double a, double b, double t, double pt, double nt)
  {
    this("", a, b, t, pt, nt);
  }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}, while the name of the classifier gets the
    * empty string.
    *
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An empty sparse weight vector.
   **/
  public SparseWinnow(double a, double b, double t, double pt, double nt,
                      SparseWeightVector v)
  {
    this("", a, b, t, pt, nt, v);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseWinnow.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public SparseWinnow(Parameters p) { this("", p); }


  /**
    * {@link #alpha}, {@link #beta}, and {@link LinearThresholdUnit#threshold}
    * take default values.
    *
    * @param n  The name of the classifier.
   **/
  public SparseWinnow(String n) { this(n, defaultAlpha); }

  /**
    * Sets {@link #alpha} to the specified value, {@link #beta} to 1 /
    * {@link #alpha}, and the {@link LinearThresholdUnit#threshold} takes the
    * default.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
   **/
  public SparseWinnow(String n, double a) { this(n, a, 1 / a); }

  /**
    * Sets {@link #alpha} and {@link #beta} to the specified values, and the
    * {@link LinearThresholdUnit#threshold} takes the default.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
   **/
  public SparseWinnow(String n, double a, double b)
  {
    this(n, a, b, defaultThreshold);
  }

  /**
    * Sets {@link #alpha}, {@link #beta}, and
    * {@link LinearThresholdUnit#threshold} to the specified values.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
   **/
  public SparseWinnow(String n, double a, double b, double t)
  {
    this(n, a, b, t, LinearThresholdUnit.defaultThickness);
  }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
   **/
  public SparseWinnow(String n, double a, double b, double t, double pt)
  {
    this(n, a, b, t, pt, pt);
  }

  /**
    * Use this constructor to fit a thick separator, where the positive and
    * negative sides of the hyperplane will be given the specified separate
    * thicknesses.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
   **/
  public SparseWinnow(String n, double a, double b, double t, double pt,
                      double nt)
  {
    this(n, a, b, t, pt, nt,
         (SparseWeightVector)
         LinearThresholdUnit.defaultWeightVector.clone());
  }

  /**
    * Use this constructor to specify an alternative subclass of
    * {@link SparseWeightVector}.
    *
    * @param n  The name of the classifier.
    * @param a  The desired value of the promotion parameter.
    * @param b  The desired value of the demotion parameter.
    * @param t  The desired threshold value.
    * @param pt The desired positive thickness.
    * @param nt The desired negative thickness.
    * @param v  An empty sparse weight vector.
   **/
  public SparseWinnow(String n, double a, double b, double t, double pt,
                      double nt, SparseWeightVector v)
  {
    super(n, t, pt, nt);
    alpha = a;
    beta = b;
    threshold = defaultThreshold;
    bias = initialWeight = defaultInitialWeight;
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseWinnow.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public SparseWinnow(String n, Parameters p)
  {
    super(n, p);
    alpha = p.alpha;
    beta = p.beta;
  }


  /**
    * Returns the current value of the {@link #alpha} variable.
    *
    * @return The value of the {@link #alpha} variable.
   **/
  public double getAlpha() { return alpha; }


  /**
    * Sets the {@link #alpha} member variable to the specified value.
    *
    * @param t  The new value for {@link #alpha}.
   **/
  public void setAlpha(double t) { alpha = t; }


  /**
    * Returns the current value of the {@link #beta} variable.
    *
    * @return The value of the {@link #beta} variable.
   **/
  public double getBeta() { return beta; }


  /**
    * Sets the {@link #beta} member variable to the specified value.
    *
    * @param t  The new value for {@link #beta}.
   **/
  public void setBeta(double t) { beta = t; }


  /**
    * Promotion is simply <code>w_i *= alpha<sup>x_i</sup></code>.
    *
    * @param example  The example object.
   **/
  public void promote(Object example) { update(example, alpha); }


  /**
    * Demotion is simply <code>w_i *= beta<sup>x_i</sup></code>.
    *
    * @param example  The example object.
   **/
  public void demote(Object example) { update(example, beta); }


  /**
    * This method performs an update <code>w_i *= base<sup>x_i</sup></code>,
    * initalizing weights in the weight vector as needed.
    *
    * @param example  The example object.
    * @param base     As described above.
   **/
  public void update(Object example, double base)
  {
    SparseWeightVector.WeightIterator wI =
      weightVector.weightIterator(extractor.classify(example));

    while (wI.hasNext())
    {
      wI.next();
      Double dw = wI.getWeight();
      double w = dw == null ? initialWeight : dw.doubleValue();
      double s = wI.getCurrentFeatureStrength();

      double multiplier = base;
      if (s == 0) multiplier = 1;
      else if (s != 1) multiplier = Math.pow(base, s);

      wI.setWeight(w * multiplier, initialWeight);
    }

    bias *= base;
  }


  /**
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link #alpha}, {@link #beta},
    * {@link LinearThresholdUnit#initialWeight},
    * {@link LinearThresholdUnit#threshold},
    * {@link LinearThresholdUnit#positiveThickness},
    * {@link LinearThresholdUnit#negativeThickness}, and finally
    * {@link LinearThresholdUnit#bias}.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out)
  {
    out.println(name + ": " + alpha + ", " + beta + ", " + initialWeight
                + ", " + threshold + ", " + positiveThickness + ", "
                + negativeThickness + ", " + bias);
    weightVector.write(out);
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone()
  {
    SparseWinnow clone = null;

    try { clone = (SparseWinnow) super.clone(); }
    catch (Exception e)
    {
      System.err.println("Error cloning SparseWinnow: " + e);
      e.printStackTrace();
      System.exit(1);
    }

    if (weightVector != null)
      clone.weightVector = (SparseWeightVector) weightVector.clone();
    return clone;
  }


  /**
    * Simply a container for all of {@link SparseWinnow}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends LinearThresholdUnit.Parameters
  {
    /**
      * The rate at which weights are promoted; default
      * {@link SparseWinnow#defaultAlpha}.
     **/
    public double alpha;
    /**
      * The rate at which weights are demoted; default equal to <code>1
      * /</code> {@link #alpha}.
     **/
    public double beta;


    /** Sets all the default values. */
    public Parameters()
    {
      alpha = defaultAlpha;
      beta = 1 / defaultAlpha;
      threshold = defaultThreshold;
      initialWeight = defaultInitialWeight;
    }
  }
}

