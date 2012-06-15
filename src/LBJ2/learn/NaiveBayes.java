package LBJ2.learn;

import java.math.*;
import java.util.*;
import java.io.*;
import LBJ2.classify.*;


/**
  * Naive Bayes is a multi-class learner that uses prediction value counts and
  * feature counts given a particular prediction value to select the most
  * likely prediction value.  More precisely, a score <i>s<sub>v</sub></i> for
  * a given prediction value <i>v</i> is computed such that
  * <i>e<sup>s<sub>v</sub></sup></i> is proportional to
  *
  * <blockquote>
  *   <i>P(v) Prod<sub>f</sub> P(f|v)</i>
  * </blockquote>
  *
  * where <i>Prod</i> is a multiplication quantifier over <i>f</i>, and
  * <i>f</i> stands for a feature.  The value corresponding to the highest
  * score is selected as the prediction.  Feature values that were never
  * observed given a particular prediction value during training are smoothed
  * with a configurable constant that defaults to <i>e<sup>-15</sup></i>.
  *
  * <p> This {@link Learner} learns a <code>discrete</code> classifier from
  * other <code>discrete</code> classifiers.  <i>Features coming from
  * <code>real</code> classifiers are ignored</i>.  It is also assumed that a
  * single discrete label feature will be produced in association with each
  * example object.  A feature taking one of the values observed in that label
  * feature will be produced by the learned classifier.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.NaiveBayes.Parameters Parameters} as input.  The
  * documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.NaiveBayes.Parameters Parameters} class indicates the
  * default value of the parameter when using the latter type of constructor.
  *
  * @see    NaiveBayesVector
  * @author Nick Rizzolo
 **/
public class NaiveBayes extends Learner
{
  /**
    * The default conditional feature probability is
    * <i>e<sup><code>defaultSmoothing</code></sup></i>.
   **/
  public static final int defaultSmoothing = -15;


  /**
    * The exponential of this number is used as the conditional probability of
    * a feature that was never observed during training; default
    * {@link #defaultSmoothing}.
   **/
  protected double smoothing;
  /** One {@link NaiveBayesVector} for each observed prediction value. */
  protected LinkedHashMap network;


  /** Default constructor. */
  public NaiveBayes() { this(""); }

  /**
    * Initializes the smoothing constant.
    *
    * @param smooth The exponential of this number is used as the conditional
    *               probability of a feature that was never observed during
    *               training.
   **/
  public NaiveBayes(double smooth) { this("", smooth); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link NaiveBayes.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public NaiveBayes(Parameters p) { this("", p); }

  /**
    * Initializes the name of the classifier.
    *
    * @param n  The classifier's name.
   **/
  public NaiveBayes(String n) { this(n, defaultSmoothing); }

  /**
    * Initializes the name and smoothing constant.
    *
    * @param name   The classifier's name.
    * @param smooth The exponential of this number is used as the conditional
    *               probability of a feature that was never observed during
    *               training.
   **/
  public NaiveBayes(String name, double smooth)
  {
    super(name);
    network = new LinkedHashMap();
    smoothing = smooth;
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link NaiveBayes.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public NaiveBayes(String n, Parameters p)
  {
    super(n);
    network = new LinkedHashMap();
    smoothing = p.smoothing;
  }


  /**
    * Sets the smoothing parameter to the specified value.
    *
    * @param s  The new value for the smoothing parameter.
   **/
  public void setSmoothing(double s) { smoothing = s; }


  /**
    * Sets the labeler.
    *
    * @param l  A labeling classifier.
   **/
  public void setLabeler(Classifier l)
  {
    if (!l.getOutputType().equals("discrete"))
    {
      System.err.println(
          "LBJ WARNING: NaiveBayes will only work with a label classifier "
          + "that returns discrete.");
      System.err.println(
          "             The given label classifier, " + l.getClass().getName()
          + ", returns " + l.getOutputType() + ".");
    }

    super.setLabeler(l);
  }


  /**
    * Trains the learning algorithm given an object as an example.
    *
    * @param example  An example of the desired learned classifier's behavior.
   **/
  public void learn(Object example)
  {
    String labelValue =
      ((DiscreteFeature) labeler.classify(example).firstFeature()).getValue();
    NaiveBayesVector labelVector = (NaiveBayesVector) network.get(labelValue);

    if (labelVector == null)
    {
      labelVector = new NaiveBayesVector();
      network.put(labelValue, labelVector);
    }

    labelVector.scaledAdd(extractor.classify(example));
  }


  /** Clears the network. */
  public void forget() { network.clear(); }


  /**
    * The scores in the returned {@link ScoreSet} are the posterior
    * probabilities of each possible label given the example.
    *
    * @param example  The object to make decisions about.
    * @return         A set of scores indicating the degree to which each
    *                 possible discrete classification value is associated
    *                 with the given example object.
   **/
  public ScoreSet scores(Object example)
  {
    ScoreSet s = new ScoreSet();

    for (Iterator I = network.entrySet().iterator(); I.hasNext(); )
    {
      Map.Entry e = (Map.Entry) I.next();
      NaiveBayesVector vector = (NaiveBayesVector) e.getValue();
      double score = vector.dot(extractor.classify(example));
      s.put((String) e.getKey(), score);
    }

    Score[] original = s.toArray();
    ScoreSet result = new ScoreSet();

    // This code would clearly run quicker if you computed each exp(score)
    // ahead of time, and divided them each by their sum.  However, each score
    // is likely to be a very negative number, so exp(score) may not be
    // numerically stable.  Subtracting two scores, however, hopefully leaves
    // you with a "less negative" number, so exp applied to the subtraction
    // hopefully behaves better.

    for (int i = 0; i < original.length; ++i)
    {
      double score = 1;

      for (int j = 0; j < original.length; ++j)
      {
        if (i == j) continue;
        score += Math.exp(original[j].score - original[i].score);
      }

      result.put(original[i].value, 1 / score);
    }

    return result;
  }


  /**
    * Prediction value counts and feature counts given a particular prediction
    * value are used to select the most likely prediction value.
    *
    * @param example  The example object.
    * @return         A single discrete feature, set to the most likely value.
   **/
  public FeatureVector classify(Object example)
  {
    double bestScore = -Double.MAX_VALUE;
    String bestValue = null;

    for (Iterator I = network.entrySet().iterator(); I.hasNext(); )
    {
      Map.Entry e = (Map.Entry) I.next();
      NaiveBayesVector vector = (NaiveBayesVector) e.getValue();
      double score = vector.dot(extractor.classify(example));

      if (score > bestScore)
      {
        bestValue = (String) e.getKey();
        bestScore = score;
      }
    }

    if (bestValue == null) return new FeatureVector();
    return
      new FeatureVector(
        new DiscreteFeature(containingPackage, name, bestValue,
                            valueIndexOf(bestValue),
                            (short) allowableValues().length));
  }


  /**
    * Writes the algorithm's internal representation as text.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out)
  {
    Map.Entry[] entries =
      (Map.Entry[]) network.entrySet().toArray(new Map.Entry[0]);
    Arrays.sort(entries,
                new Comparator<Map.Entry>()
                {
                  public int compare(Map.Entry e1, Map.Entry e2)
                  {
                    return ((String) e1.getKey()).compareTo((String) e2.getKey());
                  }
                });

    for (int i = 0; i < entries.length; ++i)
    {
      out.println("label: " + entries[i].getKey());
      ((NaiveBayesVector) entries[i].getValue()).write(out);
    }

    out.println("End of NaiveBayes");
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone()
  {
    NaiveBayes clone = null;

    try { clone = (NaiveBayes) super.clone(); }
    catch (Exception e)
    {
      System.err.println("Error cloning NaiveBayes: " + e);
      System.exit(1);
    }

    clone.network = new LinkedHashMap();
    for (Iterator I = network.entrySet().iterator(); I.hasNext(); )
    {
      Map.Entry e = (Map.Entry) I.next();
      clone.network.put(e.getKey(),
                        ((NaiveBayesVector) e.getValue()).clone());
    }

    return clone;
  }


  /**
    * Simply a container for all of {@link NaiveBayes}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends LBJ2.learn.Parameters
  {
    /**
      * The exponential of this number is used as the conditional probability
      * of a feature that was never observed during training; default
      * {@link NaiveBayes#defaultSmoothing}.
     **/
    public double smoothing;


    /** Sets all the default values. */
    public Parameters()
    {
      smoothing = defaultSmoothing;
    }
  }


  /**
    * A <code>Count</code> object stores an integer and a <code>double</code>
    * intended to hold the natural logarithm of the integer.  The object also
    * contains a <code>boolean</code> flag that is set when the
    * <code>double</code> actually does hold the log of the integer.
    *
    * @author Nick Rizzolo
   **/
  protected static class Count implements Cloneable, Serializable
  {
    /** The integer. */
    protected int count;
    /** The natural logartihm of the integer is sometimes stored here. */
    protected transient double logCount;
    /** A flag that is set iff {@link #logCount} is not up to date. */
    protected transient boolean updateLog;


    /** Sets the count to 0. */
    public Count()
    {
      count = 0;
      logCount = 0;
      updateLog = true;
    }


    /** Returns the integer count. */
    public int getCount() { return count; }


    /** Increments the count, but does not update the log. */
    public void increment()
    {
      ++count;
      updateLog = true;
    }


    /** Returns the log after updating it. */
    public double getLog()
    {
      if (updateLog)
      {
        logCount = Math.log(count);
        updateLog = false;
      }

      return logCount;
    }


    /**
      * The string representation of a <code>Count</code> object is simply the
      * integer count.
     **/
    public String toString() { return "" + count; }


    /**
      * This method returns a shallow clone.
      *
      * @return A shallow clone.
     **/
    public Object clone()
    {
      Object clone = null;
      try { clone = super.clone(); }
      catch (Exception e)
      {
        System.err.println("Error cloning " + getClass().getName() + ":");
        e.printStackTrace();
        System.exit(1);
      }

      return clone;
    }


    /**
      * Special handling during deserialization to ensure that
      * {@link #updateLog} is set to <code>true</code>.
      *
      * @param in The stream to deserialize from.
     **/
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
      in.defaultReadObject();
      updateLog = true;
    }
  }


  /**
    * Keeps track of all the counts associated with a given label.
    * Features are associated with {@link NaiveBayes.Count}s.  Those not
    * appearing in this vector are assumed to have a count of 0.  The
    * invocation of either of the <code>scaledAdd</code> methods increments
    * the prior count for the label.
    *
    * <p> {@link RealFeature}s are ignored by this vector - no counts will be
    * associated with them.
    *
    * @author Nick Rizzolo
   **/
  protected class NaiveBayesVector extends SparseWeightVector
  {
    /**
      * The prior count is the number of times either <code>scaledAdd</code>
      * method has been called.
     **/
    protected Count priorCount;


    /** Simply instantiates {@link SparseWeightVector#weights}. */
    public NaiveBayesVector() { this(new HashMap()); }

    /**
      * Simply initializes {@link SparseWeightVector#weights}.
      *
      * @param w  A map of weights.
     **/
    public NaiveBayesVector(HashMap w)
    {
      super(w);
      priorCount = new Count();
    }


    /**
      * Returns the prior count of the prediction value associated with this
      * vector.
     **/
    public Count getPrior() { return priorCount; }


    /**
      * Produces an iterator that accesses the data in this vector associated
      * with the features in the given vector.
      *
      * @param example  A vector of features extracted from an example object.
      * @return         A weight iterator.
     **/
    public WeightIterator weightIterator(FeatureVector example)
    {
      return new NaiveBayesIterator(example);
    }


    /**
      * Takes the dot product of this vector with the given vector, using the
      * hard coded smoothing weight.
      *
      * @param example  A vector of features extracted from an example object.
      * @return         The computed dot product.
     **/
    public double dot(FeatureVector example)
    {
      return dot(example, priorCount.getLog() + smoothing);
    }


    /**
      * Takes the dot product of this vector with the given vector,
      * using the specified default weight when encountering a feature that is
      * not yet present in this vector.  Here, weights are taken as
      * <i>log(feature count / prior count)</i>.  The output of this method is
      * related to the empirical probability of the example <i>e</i> as
      * follows: <br><br>
      *
      * <i>exp(dot(e)) / (sum of all labels' prior counts)) =</i><br>
      * <i>P(e's label && e)</i>
      *
      * @param example  A vector of features extracted from an example object.
      * @param defaultW The default weight.
      * @return         The computed dot product.
     **/
    public double dot(FeatureVector example, double defaultW)
    {
      WeightIterator I = weightIterator(example);
      double sum = (1 - I.totalFeatures()) * priorCount.getLog();

      while (I.hasNext())
      {
        I.next();
        Double dw = I.getWeight();
        sum += dw == null ? defaultW : dw.doubleValue();
      }

      return sum;
    }


    /**
      * This method is similar to the implementation in
      * {@link SparseWeightVector} except that the <code>factor</code>
      * and <code>defaultW</code> arguments are ignored and
      * {@link NaiveBayes.NaiveBayesVector.NaiveBayesIterator#incrementCount()}
      * is called instead of
      * {@link SparseWeightVector.WeightIterator#setWeight(double)}.
      *
      * @param example  A vector of features extracted from an example object.
      * @param factor   The scaling factor.
      * @param defaultW The default weight.
     **/
    public void scaledAdd(FeatureVector example, double factor,
                          double defaultW)
    {
      WeightIterator I = weightIterator(example);
      priorCount.increment();

      while (I.hasNext())
      {
        I.next();
        ((NaiveBayesIterator) I).incrementCount();
      }
    }


    /**
      * Converts this <code>NaiveBayesVector</code> into a
      * <code>String</code>.
      *
      * @return A <code>String</code> holding a textual representation of this
      *         vector.
     **/
    public String toString()
    {
      Map.Entry[] entries =
        (Map.Entry[]) weights.entrySet().toArray(new Map.Entry[0]);
      Arrays.sort(entries,
                  new Comparator()
                  {
                    public int compare(Object o1, Object o2)
                    {
                      Map.Entry e1 = (Map.Entry) o1;
                      Map.Entry e2 = (Map.Entry) o2;
                      return ((Feature) e1.getKey()).compareTo(e2.getKey());
                    }
                  });

      int i, biggest = "priorCount".length();
      for (i = 0; i < entries.length; ++i)
      {
        String key = entries[i].getKey().toString();
        if (key.length() > biggest) biggest = key.length();
      }

      if (biggest % 2 == 0) biggest += 2;
      else ++biggest;

      StringBuffer result = new StringBuffer();
      result.append("priorCount");
      for (i = "priorCount".length(); i < biggest; ++i) result.append(" ");
      result.append(priorCount + "\n");

      for (i = 0; i < entries.length; ++i)
      {
        String key = entries[i].getKey().toString();
        result.append(key);
        for (int j = 0; key.length() + j < biggest; ++j) result.append(" ");

        Object weight = entries[i].getValue();
        if (weight instanceof Count) result.append(weight + "\n");
        else
        {
          Count[] w = (Count[]) weight;
          result.append(w[0] + "\n");
          for (int j = 1; j < w.length; ++j)
          {
            for (int k = 0; k < biggest; ++k) result.append(" ");
            result.append(w[j] + "\n");
          }
        }
      }

      result.append("\n");
      return result.toString();
    }


    /**
      * Returns a copy of this <code>NaiveBayesVector</code> in which the
      * {@link SparseWeightVector#weights} variable has been cloned deeply.
      *
      * @return A copy of this <code>NaiveBayesVector</code>.
     **/
    public Object clone()
    {
      NaiveBayesVector clone = new NaiveBayesVector();
      clone.priorCount = (Count) priorCount.clone();

      for (Iterator I = weights.entrySet().iterator(); I.hasNext(); )
      {
        Map.Entry e = (Map.Entry) I.next();
        Object value = e.getValue();
        if (value instanceof Count)
          clone.weights.put(e.getKey(), ((Count) value).clone());
        else clone.weights.put(e.getKey(), ((Count[]) value).clone());
      }

      return clone;
    }


    /**
      * This class simultaneously iterates over the features in a given vector
      * and their corresponding weights from a
      * {@link NaiveBayes.NaiveBayesVector}.  Member methods give read and
      * write access to the weight that an object of this class currently
      * points to, and changes are reflected in the
      * {@link NaiveBayes.NaiveBayesVector}.  This iterator automatically
      * skips {@link RealFeature}s.
      *
      * @author Nick Rizzolo
     **/
    protected class NaiveBayesIterator extends WeightIterator
    {
      /**
        * Iterates through the features corresponding to the weights to be
        * iterated over for the current composite child.
       **/
      protected ListIterator I;
      /** The feature corresponding to the current weight. */
      protected DiscreteFeature currentFeature;
      /** The array of weights, if applicable. */
      protected Count[] countArray;


      /**
        * This constructor selects a slice of weights from the
        * {@link NaiveBayes.NaiveBayesVector} representing all those weights
        * corresponding to features in the given vector.
        *
        * @param example  A vector of features extracted from an example
        *                 object.
       **/
      public NaiveBayesIterator(FeatureVector example)
      {
        super(example);
        I = vector.listIterator();
        countArray = null;
        skipReals();
      }


      /**
        * Repositions this iterator to point just before the next weight
        * corresponding to a discrete feature.  If there is no next discrete
        * feature, this iterator will either continue to point to the same
        * weight as before or run off the end of the vector.
       **/
      protected void skipReals()
      {
        if (!I.hasNext()) return;

        Feature f = null;
        do { f = (Feature) I.next(); }
        while (!(f instanceof DiscreteFeature) && I.hasNext());

        if (f instanceof DiscreteFeature) I.previous();
      }


      /**
        * Determines whether a call to {@link #next()} will redirect this
        * iterator to point to a different weight.
        *
        * @return <code>true</code> iff there are more weights in the
        *         iteration.
       **/
      public boolean hasNext() { return I.hasNext(); }


      /**
        * Repositions this iterator to point to the next weight in the
        * iteration.  If there is no next weight, this iterator will continue
        * to point to the same weight as before.
       **/
      public void next()
      {
        if (!hasNext()) return;

        currentFeature = (DiscreteFeature) I.next();
        if (currentFeature.totalValues() >= 2)
        {
          if (currentFeature.fromArray() && arrayIndex++ == -1
              || !currentFeature.fromArray())
          {
            DiscreteFeature key =
              new DiscreteFeature(currentFeature.getPackage(),
                                  currentFeature.getIdentifier(), "");
            countArray = (Count[]) weights.get(key);
          }
        }
        else
        {
          arrayIndex = -1;
          countArray = null;
        }

        skipReals();
      }


      /** Restart the iteration. */
      public void reset()
      {
        I = vector.listIterator();
        countArray = null;
        arrayIndex = -1;
      }


      /**
        * Returns the feature corresponding to the current weight.
        *
        * @return The feature corresponding to the current weight.
       **/
      public Feature getCurrentFeature() { return currentFeature; }


      /**
        * Convenience method for determining the current feature's strength in
        * the given example.  The strength of a discrete feature is always 1,
        * except when that feature is known to be boolean and has a value of
        * <code>false</code>, in which case the strength is 0.
        *
        * @return The strength of the current feature in the given example.
       **/
      public double getCurrentFeatureStrength()
      {
        if (currentFeature.totalValues() == 2)
          return currentFeature.getValueIndex();
        return 1;
      }


      /**
        * Returns the count associated with the current feature's value.
        *
        * @return The count associated with the current feature's value, or
        *         <code>null</code> if the location in the weight vector
        *         pointed to by this iterator is empty.
       **/
      public Integer getCount()
      {
        if (currentFeature.totalValues() >= 2)
        {
          if (countArray != null)
          {
            if (currentFeature.fromArray())
              return
                new Integer(
                  countArray[arrayIndex * currentFeature.totalValues()
                             + currentFeature.getValueIndex()]
                  .getCount());
            return
              new Integer(countArray[currentFeature.getValueIndex()]
                          .getCount());
          }
        }
        else
        {
          Count c = (Count) weights.get(currentFeature);
          if (c == null) return null;
          return new Integer(c.getCount());
        }

        return null;
      }


      /**
        * Returns the double precision value pointed to by this iterator.
        *
        * @return The double precision value pointed to by this iterator, or
        *         <code>null</code> if the location in the weight vector
        *         pointed to by this iterator is empty.
       **/
      public Double getWeight()
      {
        if (currentFeature.totalValues() >= 2)
        {
          if (countArray != null)
          {
            if (currentFeature.fromArray())
              return
                new Double(
                  countArray[arrayIndex * currentFeature.totalValues()
                             + currentFeature.getValueIndex()]
                  .getLog());
            return
              new Double(countArray[currentFeature.getValueIndex()].getLog());
          }
        }
        else
        {
          Count c = (Count) weights.get(currentFeature);
          if (c == null) return null;
          return new Double(c.getLog());
        }

        return null;
      }


      /**
        * This method is overridden to do nothing; use
        * {@link #incrementCount()} instead.
        *
        * @param w  Unused.
       **/
      public void setWeight(double w) { }


      /**
        * Increments the count associated with the value of the current
        * feature.
       **/
      public void incrementCount()
      {
        if (currentFeature.totalValues() >= 2)
        {
          if (currentFeature.fromArray())
          {
            DiscreteArrayFeature daf = (DiscreteArrayFeature) currentFeature;
            if (countArray == null)
            {
              countArray =
                new Count[daf.getArrayLength() * daf.totalValues()];
              for (int i = 0; i < countArray.length; ++i)
                countArray[i] = new Count();

              DiscreteFeature key =
                new DiscreteFeature(daf.getPackage(), daf.getIdentifier(),
                                    "");
              key.intern();
              weights.put(key, countArray);
            }

            countArray[arrayIndex * daf.totalValues() + daf.getValueIndex()]
              .increment();
          }
          else
          {
            if (countArray == null)
            {
              countArray = new Count[currentFeature.totalValues()];
              for (int i = 0; i < countArray.length; ++i)
                countArray[i] = new Count();

              DiscreteFeature key =
                new DiscreteFeature(currentFeature.getPackage(),
                                    currentFeature.getIdentifier(), "");
              key.intern();
              weights.put(key, countArray);
            }

            countArray[currentFeature.getValueIndex()].increment();
          }
        }
        else
        {
          Count c = (Count) weights.get(currentFeature);

          if (c == null)
          {
            c = new Count();
            currentFeature.intern();
            weights.put(currentFeature, c);
          }

          c.increment();
        }
      }
    }
  }
}

