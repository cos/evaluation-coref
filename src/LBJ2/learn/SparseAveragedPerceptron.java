package LBJ2.learn;

import java.util.*;
import java.io.*;
import LBJ2.classify.*;


/**
  * An approximation to voted Perceptron, in which a weighted average of the
  * weight vectors arrived at during training becomes the weight vector used
  * to make predictions after training.
  *
  * <p> During training, after each example <i>e<sub>i</sub></i> is processed,
  * the weight vector <i>w<sub>i</sub></i> becomes the active weight vector
  * used to make predictions on future training examples.  If a mistake was
  * made on <i>e<sub>i</sub></i>, <i>w<sub>i</sub></i> will be different than
  * <i>w<sub>i - 1</sub></i>.  Otherwise, it will remain unchanged.
  *
  * <p> After training, each distinct weight vector arrived at during training
  * is associated with an integer weight equal to the number of examples whose
  * training made that weight vector active.  A new weight vector
  * <i>w<sup>*</sup></i> is computed by taking the average of all these weight
  * vectors weighted as described.  <i>w<sup>*</sup></i> is used to make all
  * predictions returned to the user through methods such as
  * {@link Classifier#classify(Object)} or
  * {@link Classifier#discreteValue(Object)}.
  *
  * <p> The above description is a useful way to think about the operation of
  * this {@link Learner}.  However, the user should note that this
  * implementation never explicitly stores <i>w<sup>*</sup></i>.  Instead, it
  * is computed efficiently on demand.  Thus, interspersed online training and
  * evaluation is efficient and operates as expected.
  *
  * <p> It is assumed that {@link Learner#labeler} is a single discrete
  * classifier that produces the same feature for every example object and
  * that the values that feature may take are available through the
  * {@link Classifier#allowableValues()} method.  The second value returned
  * from {@link Classifier#allowableValues()} is treated as "positive", and it
  * is assumed there are exactly 2 allowable values.  Assertions will produce
  * error messages if these assumptions do not hold.
  *
  * @author Nick Rizzolo
 **/
public class SparseAveragedPerceptron extends SparsePerceptron
{
  /** Default for {@link LinearThresholdUnit#weightVector}. */
  public static final AveragedWeightVector defaultWeightVector =
    new AveragedWeightVector();

  /** Keeps the extra information necessary to compute the averaged bias. */
  protected double averagedBias;


  /**
    * The learning rate and threshold take default values, while the name of
    * the classifier gets the empty string.
   **/
  public SparseAveragedPerceptron() { this(""); }

  /**
    * Sets the learning rate to the specified value, and the threshold takes
    * the default, while the name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
   **/
  public SparseAveragedPerceptron(double r) { this("", r); }

  /**
    * Sets the learning rate and threshold to the specified values, while the
    * name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
   **/
  public SparseAveragedPerceptron(double r, double t) { this("", r, t); }

  /**
    * Use this constructor to fit a thick separator, where both the positive
    * and negative sides of the hyperplane will be given the specified
    * thickness, while the name of the classifier gets the empty string.
    *
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
    * @param pt The desired thickness.
   **/
  public SparseAveragedPerceptron(double r, double t, double pt)
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
  public SparseAveragedPerceptron(double r, double t, double pt, double nt)
  {
    this("", r, t, pt, nt);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseAveragedPerceptron.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public SparseAveragedPerceptron(Parameters p) { this("", p); }


  /**
    * The learning rate and threshold take default values.
    *
    * @param n  The name of the classifier.
   **/
  public SparseAveragedPerceptron(String n) { this(n, defaultLearningRate); }

  /**
    * Sets the learning rate to the specified value, and the threshold takes
    * the default.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
   **/
  public SparseAveragedPerceptron(String n, double r)
  {
    this(n, r, defaultThreshold);
  }

  /**
    * Sets the learning rate and threshold to the specified values.
    *
    * @param n  The name of the classifier.
    * @param r  The desired learning rate value.
    * @param t  The desired threshold value.
   **/
  public SparseAveragedPerceptron(String n, double r, double t)
  {
    this(n, r, t, defaultThickness);
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
  public SparseAveragedPerceptron(String n, double r, double t, double pt)
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
  public SparseAveragedPerceptron(String n, double r, double t, double pt,
                                  double nt)
  {
    super(n, r, t, pt, nt, null);
    weightVector = (AveragedWeightVector) defaultWeightVector.clone();
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseAveragedPerceptron.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public SparseAveragedPerceptron(String n, Parameters p) { super(n, p); }


  /**
    * The score of the specified object is equal to <code>w * x + bias</code>
    * where <code>*</code> is dot product, <code>w</code> is the weight
    * vector, and <code>x</code> is the feature vector produced by the
    * extractor.
    *
    * @param example  The example object.
    * @return         The result of the dot product plus the bias.
   **/
  public double score(Object example)
  {
    double result =
      weightVector.dot(extractor.classify(example), initialWeight);
    int examples = ((AveragedWeightVector) weightVector).getExamples();
    if (examples > 0)
      result += (examples * bias - averagedBias) / (double) examples;
    return result;
  }


  /**
    * Scales the feature vector produced by the extractor by the learning rate
    * and adds it to the weight vector.
    *
    * @param example  The example object.
   **/
  public void promote(Object example)
  {
    bias += learningRate;
    int examples = ((AveragedWeightVector) weightVector).getExamples();
    averagedBias += examples * learningRate;
    weightVector.scaledAdd(extractor.classify(example), learningRate,
                           initialWeight);
  }


  /**
    * Scales the feature vector produced by the extractor by the learning rate
    * and subtracts it from the weight vector.
    *
    * @param example  The example object.
   **/
  public void demote(Object example)
  {
    bias -= learningRate;
    int examples = ((AveragedWeightVector) weightVector).getExamples();
    averagedBias -= examples * learningRate;
    weightVector.scaledAdd(extractor.classify(example), -learningRate,
                           initialWeight);
  }


  /**
    * This method works just like {@link SparsePerceptron#learn(Object)},
    * except it assumes that {@link #weightVector} references a
    * {@link SparseAveragedPerceptron.AveragedWeightVector} and it notifies
    * that vector when it got an example correct in addition to updating it
    * when it makes a mistake.
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

    AveragedWeightVector awv = (AveragedWeightVector) weightVector;
    double s =
      awv.simpleDot(extractor.classify(example), initialWeight) + bias;
    if (label && s < threshold + positiveThickness) promote(example);
    else if (!label && s >= threshold - negativeThickness) demote(example);
    else ((AveragedWeightVector) weightVector).correctExample();
  }


  /** Resets the weight vector to all zeros. */
  public void forget()
  {
    super.forget();
    averagedBias = 0;
  }


  /**
    * Writes the algorithm's internal representation as text.  In the first
    * line of output, the name of the classifier is printed, followed by
    * {@link SparsePerceptron#learningRate},
    * {@link LinearThresholdUnit#initialWeight},
    * {@link LinearThresholdUnit#threshold},
    * {@link LinearThresholdUnit#positiveThickness},
    * {@link LinearThresholdUnit#negativeThickness},
    * {@link LinearThresholdUnit#bias}, and finally {@link #averagedBias}.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out)
  {
    out.println(name + ": " + learningRate + ", " + initialWeight + ", "
                + threshold + ", " + positiveThickness + ", "
                + negativeThickness + ", " + bias + ", " + averagedBias);
    weightVector.write(out);
  }


  /**
    * Simply a container for all of {@link SparseAveragedPerceptron}'s
    * configurable parameters.  Using instances of this class should make code
    * more readable and constructors less complicated.  Note that if the
    * object referenced by {@link LinearThresholdUnit.Parameters#weightVector}
    * is replaced via an instance of this class, it must be replaced with an
    * {@link SparseAveragedPerceptron.AveragedWeightVector}.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends SparsePerceptron.Parameters
  {
    /** Sets all the default values. */
    public Parameters()
    {
      weightVector = (AveragedWeightVector) defaultWeightVector.clone();
    }
  }


  /**
    * This implementation of a sparse weight vector associates two
    * <code>double</code>s with each {@link Feature}.  The first plays the
    * role of the usual weight vector, and the second accumulates multiples of
    * examples on which mistakes were made to help implement the weighted
    * average.
    *
    * @author Nick Rizzolo
   **/
  public static class AveragedWeightVector extends SparseWeightVector
  {
    /** Counts the total number of training examples this vector has seen. */
    protected int examples;


    /** Simply instantiates {@link SparseWeightVector#weights}. */
    public AveragedWeightVector() { super(new HashMap()); }

    /**
      * Simply initializes {@link SparseWeightVector#weights}.
      *
      * @param w  A map of weights.
     **/
    public AveragedWeightVector(HashMap w) { super(w); }


    /** Increments the {@link #examples} variable. */
    public void correctExample() { ++examples; }
    /** Returns the {@link #examples} variable. */
    public int getExamples() { return examples; }


    /**
      * Produces an iterator that accesses the data in this vector associated
      * with the features in the given vector.
      *
      * @param example  A vector of features extracted from an example object.
      * @return         A weight iterator.
     **/
    public WeightIterator weightIterator(FeatureVector example)
    {
      return new AveragedWeightIterator(example);
    }


    /**
      * Takes the dot product of the regular, non-averaged, Perceptron weight
      * vector with the given vector, using the hard coded default weight.
      *
      * @param example  A vector of features extracted from an example object.
      * @return         The computed dot product.
     **/
    public double simpleDot(FeatureVector example)
    {
      return simpleDot(example, defaultWeight);
    }


    /**
      * Takes the dot product of the regular, non-averaged, Perceptron weight
      * vector with the given vector, using the specified default weight when
      * a feature is not yet present in this vector.
      *
      * @param example  A vector of features extracted from an example object.
      * @param defaultW An initial weight for new features.
      * @return         The computed dot product.
     **/
    public double simpleDot(FeatureVector example, double defaultW)
    {
      WeightIterator I = weightIterator(example);
      double sum = 0;

      while (I.hasNext())
      {
        I.next();
        Double dw = ((AveragedWeightIterator) I).getSimpleWeight();
        double w = dw == null ? defaultW : dw.doubleValue();
        sum += w * I.getCurrentFeatureStrength();
      }

      return sum;
    }


    /**
      * Performs pairwise addition of the feature values in the given vector
      * scaled by the given factor, modifying this weight vector, using the
      * specified default weight when a feature from the given vector is not
      * yet present in this vector.
      *
      * @param example  A vector of features extracted from an example object.
      * @param factor   The scaling factor.
      * @param defaultW An initial weight for new features.
     **/
    public void scaledAdd(FeatureVector example, double factor,
                          double defaultW)
    {
      WeightIterator I = weightIterator(example);

      while (I.hasNext())
      {
        I.next();
        Double dw = ((AveragedWeightIterator) I).getSimpleWeight();
        double w = dw == null ? defaultW : dw.doubleValue();
        I.setWeight(w + I.getCurrentFeatureStrength() * factor, defaultW);
      }

      ++examples;
    }


    /**
      * Returns a copy of this <code>AveragedWeightVector</code> in which the
      * {@link SparseWeightVector#weights} variable has been cloned deeply.
      *
      * @return A copy of this <code>AveragedWeightVector</code>.
     **/
    public Object clone()
    {
      AveragedWeightVector clone = new AveragedWeightVector();

      for (Iterator I = weights.entrySet().iterator(); I.hasNext(); )
      {
        Map.Entry e = (Map.Entry) I.next();
        double[] value = (double[]) e.getValue();
        clone.weights.put(e.getKey(), value.clone());
      }

      return clone;
    }


    /**
      * Converts this <code>AveragedWeightVector</code> into a
      * <code>String</code>.
      *
      * @return A <code>String</code> holding a textual representation of this
      *         vector.
     **/
    public String toString() { return examples + "\n" + super.toString(); }


    /**
      * This class simultaneously iterates over the features in a given vector
      * and their corresponding weights from an
      * {@link SparseAveragedPerceptron.AveragedWeightVector}.  Member methods
      * give read and write access to the weight that an object of this class
      * currently points to, and changes are reflected in the
      * {@link SparseAveragedPerceptron.AveragedWeightVector}.
      *
      * @author Nick Rizzolo
     **/
    protected class AveragedWeightIterator
              extends SparseWeightVector.WeightIterator
    {
      /**
        * This constructor selects a slice of weights from the
        * {@link SparseAveragedPerceptron.AveragedWeightVector} representing
        * all those weights corresponding to features in the given vector.
        *
        * @param example  A vector of features extracted from an example
        *                 object.
       **/
      public AveragedWeightIterator(FeatureVector example) { super(example); }


      /**
        * Returns the double precision value pointed to by this iterator.
        *
        * @return The double precision value pointed to by this iterator, or
        *         <code>null</code> if the location in the weight vector
        *         pointed to by this iterator is empty.
       **/
      public Double getSimpleWeight()
      {
        if (isDiscrete)
        {
          if (currentFeature.totalValues() >= 2)
          {
            if (currentFeature.fromArray())
            {
              if (weightArray != null)
              {
                if (currentFeature.totalValues() == 2)
                  return new Double(weightArray[2 * arrayIndex]);
                else
                {
                  DiscreteFeature df = (DiscreteFeature) currentFeature;
                  return
                    new Double(weightArray[2 * (arrayIndex * df.totalValues()
                                                + df.getValueIndex())]);
                }
              }
            }
            else if (currentFeature.totalValues() == 2)
            {
              DiscreteFeature key =
                new DiscreteFeature(currentFeature.getPackage(),
                                    currentFeature.getIdentifier(), "");
              double[] w = (double[]) weights.get(key);
              if (w != null) return new Double(w[0]);
            }
            else if (weightArray != null)
            {
              DiscreteFeature df = (DiscreteFeature) currentFeature;
              return new Double(weightArray[2 * df.getValueIndex()]);
            }
          }
          else
          {
            double[] w = (double[]) weights.get(currentFeature);
            if (w != null) return new Double(w[0]);
          }
        }
        else if (currentFeature.fromArray())
        {
          if (weightArray != null)
            return new Double(weightArray[2 * arrayIndex]);
        }
        else
        {
          RealFeature key =
            new RealFeature(currentFeature.getPackage(),
                            currentFeature.getIdentifier(), 0);
          double[] w = (double[]) weights.get(key);
          if (w != null) return new Double(w[0]);
        }

        return null;
      }


      /**
        * Returns the weighted average value pointed to by this iterator.
        *
        * @return The weighted average value pointed to by this iterator, or
        *         <code>null</code> if the location in the weight vector
        *         pointed to by this iterator is empty.
       **/
      public Double getWeight()
      {
        if (isDiscrete)
        {
          if (currentFeature.totalValues() >= 2)
          {
            if (currentFeature.fromArray())
            {
              if (weightArray != null)
              {
                int i = 0;

                if (currentFeature.totalValues() == 2) i = 2 * arrayIndex;
                else
                {
                  DiscreteFeature df = (DiscreteFeature) currentFeature;
                  i = 2 * (arrayIndex * df.totalValues()
                           + df.getValueIndex());
                }

                return
                  new Double((examples * weightArray[i] - weightArray[i + 1])
                             / (double) examples);
              }
            }
            else if (currentFeature.totalValues() == 2)
            {
              DiscreteFeature key =
                new DiscreteFeature(currentFeature.getPackage(),
                                    currentFeature.getIdentifier(), "");
              double[] w = (double[]) weights.get(key);
              if (w != null)
                return
                  new Double((examples * w[0] - w[1]) / (double) examples);
            }
            else if (weightArray != null)
            {
              DiscreteFeature df = (DiscreteFeature) currentFeature;
              int i = 2 * df.getValueIndex();
              return
                new Double((examples * weightArray[i] - weightArray[i + 1])
                           / (double) examples);
            }
          }
          else
          {
            double[] w = (double[]) weights.get(currentFeature);
            if (w != null)
              return new Double((examples * w[0] - w[1]) / (double) examples);
          }
        }
        else if (currentFeature.fromArray())
        {
          if (weightArray != null)
          {
            int i = 2 * arrayIndex;
            return
              new Double((examples * weightArray[i] - weightArray[i + 1])
                         / (double) examples);
          }
        }
        else
        {
          RealFeature key =
            new RealFeature(currentFeature.getPackage(),
                            currentFeature.getIdentifier(), 0);
          double[] w = (double[]) weights.get(key);
          if (w != null)
            return new Double((examples * w[0] - w[1]) / (double) examples);
        }

        return null;
      }


      /**
        * Modifies the weight vector.  A call to this method is equivalent to
        * calling <code>setWeight(w, 0)</code>.
        *
        * @see      #setWeight(double,double)
        * @param w  The new value for the weight pointed to by this iterator.
       **/
      public void setWeight(double w) { setWeight(w, defaultWeight); }


      /**
        * Modifies the weight vector.
        *
        * @param w  The new value for the weight pointed to by this iterator.
        * @param d  The default value for other weights incidentally created
        *           by this invocation.
       **/
      public void setWeight(double w, double d)
      {
        if (isDiscrete)
        {
          if (currentFeature.totalValues() >= 2)
          {
            if (currentFeature.fromArray())
            {
              if (currentFeature.totalValues() == 2)
              {
                if (weightArray == null)
                {
                  DiscreteArrayFeature daf =
                    (DiscreteArrayFeature) currentFeature;
                  weightArray = new double[2 * daf.getArrayLength()];
                  DiscreteFeature key =
                    new DiscreteFeature(daf.getPackage(), daf.getIdentifier(),
                                        "");
                  key.intern();
                  weights.put(key, weightArray);

                  if (d != 0)
                    for (int i = 0; i < weightArray.length; i += 2)
                      weightArray[i] = d;
                }

                int i = 2 * arrayIndex;
                double difference = w - weightArray[i];
                weightArray[i] = w;
                weightArray[i + 1] += examples * difference;
              }
              else
              {
                DiscreteArrayFeature daf =
                  (DiscreteArrayFeature) currentFeature;
                if (weightArray == null)
                {
                  weightArray =
                    new double[2 * daf.getArrayLength() * daf.totalValues()];
                  DiscreteFeature key =
                    new DiscreteFeature(daf.getPackage(), daf.getIdentifier(),
                                        "");
                  key.intern();
                  weights.put(key, weightArray);

                  if (d != 0)
                    for (int i = 0; i < weightArray.length; i += 2)
                      weightArray[i] = d;
                }

                int i = 2 * (arrayIndex * daf.totalValues()
                             + daf.getValueIndex());
                double difference = w - weightArray[i];
                weightArray[i] = w;
                weightArray[i + 1] += examples * difference;
              }
            }
            else if (currentFeature.totalValues() == 2)
            {
              DiscreteFeature key =
                new DiscreteFeature(currentFeature.getPackage(),
                                    currentFeature.getIdentifier(), "");
              double[] a = (double[]) weights.get(key);

              if (a == null)
              {
                a = new double[2];
                key.intern();
                weights.put(key, a);
                a[0] = d;
              }

              double difference = w - a[0];
              a[0] = w;
              a[1] += examples * difference;
            }
            else
            {
              if (weightArray == null)
              {
                weightArray = new double[2 * currentFeature.totalValues()];
                DiscreteFeature key =
                  new DiscreteFeature(currentFeature.getPackage(),
                                      currentFeature.getIdentifier(), "");
                key.intern();
                weights.put(key, weightArray);

                if (d != 0)
                  for (int i = 0; i < weightArray.length; i += 2)
                    weightArray[i] = d;
              }

              int i = 2 * ((DiscreteFeature) currentFeature).getValueIndex();
              double difference = w - weightArray[i];
              weightArray[i] = w;
              weightArray[i + 1] += examples * difference;
            }
          }
          else
          {
            double[] a = (double[]) weights.get(currentFeature);

            if (a == null)
            {
              a = new double[2];
              currentFeature.intern();
              weights.put(currentFeature, a);
              a[0] = d;
            }

            double difference = w - a[0];
            a[0] = w;
            a[1] += examples * difference;
          }
        }
        else if (currentFeature.fromArray())
        {
          if (weightArray == null)
          {
            RealArrayFeature raf = (RealArrayFeature) currentFeature;
            weightArray = new double[2 * raf.getArrayLength()];
            RealFeature key =
              new RealFeature(raf.getPackage(), raf.getIdentifier(), 0);
            key.intern();
            weights.put(key, weightArray);

            if (d != 0)
              for (int i = 0; i < weightArray.length; i += 2)
                weightArray[i] = d;
          }

          int i = 2 * arrayIndex;
          double difference = w - weightArray[i];
          weightArray[i] = w;
          weightArray[i + 1] += examples * difference;
        }
        else
        {
          RealFeature key =
            new RealFeature(currentFeature.getPackage(),
                            currentFeature.getIdentifier(), 0);
          double[] a = (double[]) weights.get(key);

          if (a == null)
          {
            a = new double[2];
            key.intern();
            weights.put(key, a);
            a[0] = d;
          }

          double difference = w - a[0];
          a[0] = w;
          a[1] += examples * difference;
        }
      }
    }
  }
}

