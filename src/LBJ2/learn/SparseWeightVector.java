package LBJ2.learn;

import java.util.*;
import java.io.*;
import LBJ2.classify.*;


/**
  * This class is used as a weight vector in sparse learning algorithms.
  * {@link Feature}s are associated with <code>Double</code>s and/or with
  * <code>double[]</code>s representing the weights of the features they
  * produce.  Features not appearing in the vector are assumed to have the
  * {@link #defaultWeight}.
  *
  * @author Nick Rizzolo
 **/
public class SparseWeightVector implements Cloneable, Serializable
{
  /**
    * When a feature appears in an example but not in this vector, it is
    * assumed to have this weight.
   **/
  protected static final double defaultWeight = 0;


  /** The weights in the vector indexed by {@link Feature}. */
  protected HashMap weights;


  /** Simply instantiates {@link #weights}. */
  public SparseWeightVector() { this(new HashMap()); }

  /**
    * Simply initializes {@link #weights}.
    *
    * @param w  A map of weights.
   **/
  public SparseWeightVector(HashMap w) { weights = w; }


  /**
    * Produces an iterator that accesses the data in this vector associated
    * with the features in the argument vector.
    *
    * @param example  A vector of features extracted from an example object.
    * @return         A weight iterator.
   **/
  public WeightIterator weightIterator(FeatureVector example)
  {
    return new WeightIterator(example);
  }


  /**
    * Takes the dot product of this <code>SparseWeightVector</code> with the
    * argument vector, using the hard coded default weight.
    *
    * @param example  A vector of features extracted from an example object.
    * @return         The computed dot product.
   **/
  public double dot(FeatureVector example)
  {
    return dot(example, defaultWeight);
  }


  /**
    * Takes the dot product of this <code>SparseWeightVector</code> with the
    * argument vector, using the specified default weight when one is not yet
    * present in this vector.
    *
    * @param example  A vector of features extracted from an example object.
    * @param defaultW The default weight.
    * @return         The computed dot product.
   **/
  public double dot(FeatureVector example, double defaultW)
  {
    WeightIterator I = weightIterator(example);
    double sum = 0;

    while (I.hasNext())
    {
      I.next();
      Double dw = I.getWeight();
      double w = dw == null ? defaultW : dw.doubleValue();
      sum += w * I.getCurrentFeatureStrength();
    }

    return sum;
  }


  /**
    * Self-modifying vector addition.
    *
    * @param example  A vector of features extracted from an example object.
   **/
  public void scaledAdd(FeatureVector example)
  {
    scaledAdd(example, 1, defaultWeight);
  }


  /**
    * Self-modifying vector addition where the argument vector is first scaled
    * by the given factor.  The default weight is used to initialize new
    * feature weights.
    *
    * @param example  A vector of features extracted from an example object.
    * @param factor   The scaling factor.
   **/
  public void scaledAdd(FeatureVector example, double factor)
  {
    scaledAdd(example, factor, defaultWeight);
  }


  /**
    * Self-modifying vector addition where the argument vector is first scaled
    * by the given factor.
    *
    * @param example  A vector of features extracted from an example object.
    * @param factor   The scaling factor.
    * @param defaultW An initial weight for previously unseen features.
   **/
  public void scaledAdd(FeatureVector example, double factor, double defaultW)
  {
    WeightIterator I = weightIterator(example);

    while (I.hasNext())
    {
      I.next();
      Double dw = I.getWeight();
      double w = dw == null ? defaultW : dw.doubleValue();
      I.setWeight(w + I.getCurrentFeatureStrength() * factor, defaultW);
    }
  }


  /** Empties the weight map. */
  public void clear() { weights.clear(); }


  /**
    * Converts this <code>SparseWeightVector</code> into a
    * <code>String</code>.
    *
    * @return A <code>String</code> holding a textual representation of this
    *         vector.
   **/
  public String toString()
  {
    Map.Entry[] entries =
      (Map.Entry[]) weights.entrySet().toArray(new Map.Entry[weights.size()]);
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

    int i, biggest = 0;
    for (i = 0; i < entries.length; ++i)
    {
      String key = entries[i].getKey().toString();
      if (key.length() > biggest) biggest = key.length();
    }

    if (biggest % 2 == 0) biggest += 2;
    else ++biggest;

    StringBuffer result = new StringBuffer();
    for (i = 0; i < entries.length; ++i)
    {
      String key = entries[i].getKey().toString();
      result.append(key);
      for (int j = 0; key.length() + j < biggest; ++j) result.append(" ");

      Object weight = entries[i].getValue();
      if (weight instanceof Double) result.append(weight + "\n");
      else
      {
        double[] w = (double[]) weight;
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
    * Outputs the contents of this <code>SparseWeightVector</code> into the
    * specified <code>PrintStream</code>.  This method merely writes the value
    * returned by {@link #toString()}.
    *
    * @param out  The stream to output into.
   **/
  public void write(PrintStream out) { out.print(toString()); }


  /**
    * Returns a copy of this <code>SparseWeightVector</code> in which the
    * {@link #weights} variable has been cloned deeply.
    *
    * @return A copy of this <code>SparseWeightVector</code>.
   **/
  public Object clone()
  {
    SparseWeightVector clone = new SparseWeightVector();

    for (Iterator I = weights.entrySet().iterator(); I.hasNext(); )
    {
      Map.Entry e = (Map.Entry) I.next();
      Object value = e.getValue();
      if (value instanceof Double) clone.weights.put(e.getKey(), value);
      else clone.weights.put(e.getKey(), ((double[]) value).clone());
    }

    return clone;
  }


  /**
    * This class simultaneously iterates over the features in the given vector
    * and their corresponding weights from a {@link SparseWeightVector}.
    * Member methods give read and write access to the weight that an object
    * of this class currently points to, and changes are reflected in the
    * {@link SparseWeightVector}.
    *
    * @author Nick Rizzolo
   **/
  protected class WeightIterator
  {
    /** The feature vector to iterate through. */
    protected FeatureVector vector;
    /**
      * Iterates through the features corresponding to the weights to be
      * iterated over for the current composite child.
     **/
    protected Iterator I;
    /** The feature corresponding to the current weight. */
    protected Feature currentFeature;
    /** <code>true</code> iff the current feature is discrete. */
    protected boolean isDiscrete;
    /** The array of weights, if applicable. */
    protected double[] weightArray;
    /** Pointer into <code>weightArray</code>. */
    protected int arrayIndex;


    /**
      * This constructor selects a slice of weights from the
      * {@link SparseWeightVector} representing all those weights
      * corresponding to features in the given vector.
      *
      * @param example  A vector of features extracted from an example object.
     **/
    public WeightIterator(FeatureVector example)
    {
      currentFeature = null;
      vector = example;
      I = vector.iterator();
      weightArray = null;
      arrayIndex = -1;
    }


    /** Returns the total number of features to iterate through. */
    public int totalFeatures() { return vector.size(); }


    /**
      * Determines whether a call to <code>next()</code> will redirect this
      * iterator to point to a different weight.
      *
      * @return <code>true</code> iff there are more weights in the iteration.
     **/
    public boolean hasNext() { return I.hasNext(); }


    /**
      * Repositions this iterator to point to the next weight in the
      * iteration.  If there is no next weight, this iterator will continue to
      * point to the same weight as before.
     **/
    public void next()
    {
      if (!hasNext()) return;

      Feature nextFeature = (Feature) I.next();
      if (currentFeature != null && currentFeature.fromArray()
          && (!nextFeature.fromArray()
              || !currentFeature.nameEquals(nextFeature)))
      {
        arrayIndex = -1;
        weightArray = null;
      }

      currentFeature = nextFeature;
      if (currentFeature.fromArray())
      {
        if (arrayIndex++ == -1)
        {
          isDiscrete = currentFeature instanceof DiscreteFeature;
          if (currentFeature.totalValues() > 0)
          {
            DiscreteFeature key =
              new DiscreteFeature(currentFeature.getPackage(),
                                  currentFeature.getIdentifier(), "");
            weightArray = (double[]) weights.get(key);
          }
          else if (!isDiscrete)
          {
            RealFeature key =
              new RealFeature(currentFeature.getPackage(),
                              currentFeature.getIdentifier(), 0);
            weightArray = (double[]) weights.get(key);
          }
        }
      }
      else
      {
        isDiscrete = currentFeature instanceof DiscreteFeature;
        if (currentFeature.totalValues() > 2)
        {
          DiscreteFeature key =
            new DiscreteFeature(currentFeature.getPackage(),
                                currentFeature.getIdentifier(), "");
          weightArray = (double[]) weights.get(key);
        }
        else weightArray = null;
      }
    }


    /** Restart the iteration. */
    public void reset()
    {
      I = vector.iterator();
      weightArray = null;
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
      * <code>false</code>, in which case the strength is 0.  The strength of
      * a real feature is just its value.
      *
      * @return The strength of the current feature in the given example.
     **/
    public double getCurrentFeatureStrength()
    {
      if (currentFeature.totalValues() == 2)
        return ((DiscreteFeature) currentFeature).getValueIndex();
      if (isDiscrete) return 1;
      return ((RealFeature) currentFeature).getValue();
    }


    /**
      * Returns the double precision value pointed to by this iterator.
      *
      * @return The double precision value pointed to by this iterator, or
      *         <code>null</code> if the location in the weight vector pointed
      *         to by this iterator is empty.
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
              if (currentFeature.totalValues() == 2)
                return new Double(weightArray[arrayIndex]);
              else
              {
                DiscreteFeature df = (DiscreteFeature) currentFeature;
                return new Double(weightArray[arrayIndex * df.totalValues()
                                              + df.getValueIndex()]);
              }
            }
          }
          else if (currentFeature.totalValues() == 2)
          {
            DiscreteFeature key =
              new DiscreteFeature(currentFeature.getPackage(),
                                  currentFeature.getIdentifier(), "");
            return (Double) weights.get(key);
          }
          else if (weightArray != null)
          {
            DiscreteFeature df = (DiscreteFeature) currentFeature;
            return new Double(weightArray[df.getValueIndex()]);
          }
        }
        else return (Double) weights.get(currentFeature);
      }
      else if (currentFeature.fromArray())
      {
        if (weightArray != null) return new Double(weightArray[arrayIndex]);
      }
      else
      {
        RealFeature key =
          new RealFeature(currentFeature.getPackage(),
                          currentFeature.getIdentifier(), 0);
        return (Double) weights.get(key);
      }

      return null;
    }


    /**
      * Modifies the weight vector.  Calling this method with parameter
      * <code>w</code> is equivalent to calling <code>setWeight(w, 0)</code>.
      *
      * @param w  The new value for the weight pointed to by this iterator.
      * @see      #setWeight(double,double)
     **/
    public void setWeight(double w) { setWeight(w, 0); }


    /**
      * Modifies the weight vector.
      *
      * @param w  The new value for the weight pointed to by this iterator.
      * @param d  The default value for other weights incidentally created by
      *           this invocation.
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
                weightArray = new double[daf.getArrayLength()];
                DiscreteFeature key =
                  new DiscreteFeature(daf.getPackage(), daf.getIdentifier(),
                                      "");
                key.intern();
                weights.put(key, weightArray);

                if (d != 0)
                  for (int i = 0; i < weightArray.length; ++i)
                    weightArray[i] = d;
              }

              weightArray[arrayIndex] = w;
            }
            else
            {
              DiscreteArrayFeature daf =
                (DiscreteArrayFeature) currentFeature;
              if (weightArray == null)
              {
                weightArray =
                  new double[daf.getArrayLength() * daf.totalValues()];
                DiscreteFeature key =
                  new DiscreteFeature(daf.getPackage(), daf.getIdentifier(),
                                      "");
                key.intern();
                weights.put(key, weightArray);

                if (d != 0)
                  for (int i = 0; i < weightArray.length; ++i)
                    weightArray[i] = d;
              }

              weightArray[arrayIndex * daf.totalValues()
                          + daf.getValueIndex()] = w;
            }
          }
          else if (currentFeature.totalValues() == 2)
          {
            DiscreteFeature key =
              new DiscreteFeature(currentFeature.getPackage(),
                                  currentFeature.getIdentifier(), "");
            key.intern();
            weights.put(key, new Double(w));
          }
          else
          {
            if (weightArray == null)
            {
              weightArray = new double[currentFeature.totalValues()];
              DiscreteFeature key =
                new DiscreteFeature(currentFeature.getPackage(),
                                    currentFeature.getIdentifier(), "");
              key.intern();
              weights.put(key, weightArray);

              if (d != 0)
                for (int i = 0; i < weightArray.length; ++i)
                  weightArray[i] = d;
            }

            weightArray[((DiscreteFeature) currentFeature).getValueIndex()] =
              w;
          }
        }
        else
        {
          currentFeature.intern();
          weights.put(currentFeature, new Double(w));
        }
      }
      else if (currentFeature.fromArray())
      {
        if (weightArray == null)
        {
          RealArrayFeature raf = (RealArrayFeature) currentFeature;
          weightArray = new double[raf.getArrayLength()];
          RealFeature key =
            new RealFeature(raf.getPackage(), raf.getIdentifier(), 0);
          key.intern();
          weights.put(key, weightArray);

          if (d != 0)
            for (int i = 0; i < weightArray.length; ++i)
              weightArray[i] = d;
        }

        weightArray[arrayIndex] = w;
      }
      else
      {
        RealFeature key =
          new RealFeature(currentFeature.getPackage(),
                          currentFeature.getIdentifier(), 0);
        key.intern();
        weights.put(key, new Double(w));
      }
    }
  }
}

