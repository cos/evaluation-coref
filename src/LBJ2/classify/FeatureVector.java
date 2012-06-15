package LBJ2.classify;

import java.util.*;
import java.io.*;


/**
  * Objects of this class are returned by classifiers that have been applied
  * to an object.
  *
  * @author Nick Rizzolo
 **/
public class FeatureVector implements Serializable
{
  /** Stores non-label features. */
  public LinkedList features;
  /** Stores labels. */
  public LinkedList labels;
  /** With this variable, the user can weight the entire vector. */
  public double weight;


  /** Simply instantiates the member variables. */
  public FeatureVector()
  {
    features = new LinkedList();
    labels = new LinkedList();
    weight = 1;
  }

  /**
    * Creates the vector and adds the given feature to it.
    *
    * @param f  A <code>Feature</code> to start this vector off with.
   **/
  public FeatureVector(Feature f)
  {
    features = new LinkedList();
    labels = new LinkedList();
    addFeature(f);
  }


  /**
    * Creates the vector and adds all the given features to it.
    *
    * @param l  A list of features to add to the vector.
   **/
  public FeatureVector(LinkedList l)
  {
    features = new LinkedList();
    labels = new LinkedList();
    features.addAll(l);
  }


  /**
    * The size of this vector is defined as the size of <code>features</code>
    * plus the size of <code>labels</code>.
    *
    * @return The size of this vector.
   **/
  public int size() { return features.size() + labels.size(); }


  /**
    * Removes all elements from both <code>features</code> and
    * <code>labels</code>.
   **/
  public void clear()
  {
    features.clear();
    labels.clear();
  }


  /** Sorts both of the feature lists. */
  public void sort()
  {
    Collections.sort(features);
    Collections.sort(labels);
  }


  /**
    * Adds a feature to the vector.
    *
    * @param f  The features to be added.
   **/
  public void addFeature(Feature f) { features.add(f); }


  /**
    * Adds all the features in another vector to this vector.
    *
    * @param v  The vector whose features are to be added.
   **/
  public void addFeatures(FeatureVector v)
  {
    features.addAll(v.features);
  }


  /**
    * Adds a label to the vector.
    *
    * @param l  The label to be added.
   **/
  public void addLabel(Feature l) { labels.add(l); }


  /**
    * Adds all the features in another vector (but not the labels in that
    * vector) to the labels of this vector.
    *
    * @param v  The vector whose features will become this vector's labels.
   **/
  public void addLabels(FeatureVector v) { labels.addAll(v.features); }


  /**
    * Determines whether this vector has any labels.
    *
    * @return <code>true</code> iff this vector has at least one label.
   **/
  public boolean isLabeled() { return labels.size() > 0; }


  /**
    * Returns all the values of the features in this vector (not labels)
    * arranged in a <code>String</code> array.
    *
    * @return An array of <code>String</code>s with all the feature values
    *         from this vector, or <code>null</code> if there are any
    *         <code>RealFeature</code>s in this vector.
   **/
  public String[] discreteValueArray()
  {
    String[] result = new String[features.size()];

    try
    {
      int i = 0;
      for (Iterator I = features.iterator(); I.hasNext(); ++i)
        result[i] = ((DiscreteFeature) I.next()).value;
    }
    catch (ClassCastException e) { return null; }

    return result;
  }


  /**
    * Returns all the values of the features in this vector (not labels)
    * arranged in a <code>double</code> array.
    *
    * @return An array of <code>double</code>s with all the feature values
    *         from this vector, or <code>null</code> if there are any
    *         <code>DiscreteFeature</code>s in this vector.
   **/
  public double[] realValueArray()
  {
    double[] result = new double[features.size()];

    try
    {
      int i = 0;
      for (Iterator I = features.iterator(); I.hasNext(); ++i)
        result[i] = ((RealFeature) I.next()).value;
    }
    catch (ClassCastException e) { return null; }

    return result;
  }


  /**
    * Returns the first feature in <code>features</code>.
    *
    * @return The first feature, or <code>null</code> if there aren't any.
   **/
  public Feature firstFeature()
  {
    return features.size() > 0 ? (Feature) features.iterator().next() : null;
  }


  /**
    * Returns the first feature in <code>labels</code>.
    *
    * @return The first label, or <code>null</code> if there aren't any.
   **/
  public Feature firstLabel()
  {
    return features.size() > 0 ? (Feature) labels.iterator().next() : null;
  }


  /**
    * Allows the user to iterate through the feature list.
    *
    * @return An iterator for <code>features</code>.
   **/
  public Iterator iterator() { return features.iterator(); }


  /**
    * Allows the user to iterate through the label list.
    *
    * @return An iterator for <code>labels</code>.
   **/
  public Iterator labelIterator() { return labels.iterator(); }


  /**
    * Allows the user to iterate through the feature list.
    *
    * @return An iterator for <code>features</code>.
   **/
  public ListIterator listIterator() { return features.listIterator(); }


  /**
    * Allows the user to iterate through the label list.
    *
    * @return An iterator for <code>labels</code>.
   **/
  public ListIterator labelListIterator() { return labels.listIterator(); }


  /**
   * Returns the square of the magnitude of the feature vector
   * 
   * @return The square of the magnitude of the feature vector
   */
  public double L2NormSquared() {
	  double sum = 0;
	  for (Iterator I = features.iterator(); I.hasNext(); ) {
		  Feature cur = (Feature)I.next();
		  double val;
		  if (cur.totalValues() == 2)
			  val = ((DiscreteFeature) cur).getValueIndex();
		  else if (cur instanceof DiscreteFeature)
			  val = 1;
		  else
			  val = ((RealFeature) cur).getValue();
		  
		  sum += val * val;
	   }
	   return sum;
   }
  

  /**
   * Two <code>FeatureVector</code>s are equivalent if they contain the same
   * features, as defined by <code>Feature</code> equivalence.
   *
   * @param o  The object to compare with this <code>FeatureVector</code> for
   *           equality.
   * @return   True iff <code>o</code> is a <code>FeatureVector</code>
   *           equivalent with this vector as defined above.
  **/
  public boolean equals(Object o) {
    if (!(o instanceof FeatureVector)) return false;

    LinkedList v1 = (LinkedList) features.clone();
    LinkedList v2 = (LinkedList) ((FeatureVector) o).features.clone();

    Collections.sort(v1);
    Collections.sort(v2);

    Iterator i1 = v1.iterator();
    Iterator i2 = v2.iterator();

    while (i1.hasNext() && i2.hasNext()) {
    	Feature f1 = (Feature) i1.next();
    	Feature f2 = (Feature) i2.next();

    	if (!f1.equals(f2)) return false;
    }

    if (!i1.hasNext() && !i2.hasNext()) return true;
    return false;
  }


  /**
   * Take dot product of two feature vectors
   * 
   * @param vector	The feature vector to take the dot product with
   * @return 	The dot product of the invoking feature vector and
   * 			the feature vector passed as parameter
   */
  public double dot(FeatureVector vector) {
	  LinkedList v1 = (LinkedList) features.clone();
	  LinkedList v2 = (LinkedList) vector.features.clone();

	  Collections.sort(v1);
	  Collections.sort(v2);

	  Iterator i1 = v1.iterator();
	  Iterator i2 = v2.iterator();

	  double res = 0;

	  if (!i1.hasNext() || !i2.hasNext()) return res;

	  Feature f1 = (Feature) i1.next();
	  Feature f2 = (Feature) i2.next();

	  boolean flag = true;
	  while (flag) {  
		  try {
			  if (f1.equals(f2)) {
				  double val = 0;
				  if (f1.totalValues() == 2)
					  val = ((DiscreteFeature) f1).getValueIndex() * ((DiscreteFeature) f2).getValueIndex();
				  else if (f1 instanceof DiscreteFeature)
					  val = 1;
				  else
					  val = ((RealFeature) f1).getValue() * ((RealFeature) f2).getValue();
				  res += val;

				  f1 = (Feature) i1.next();
				  f2 = (Feature) i2.next();
			  } else if (f1.compareTo(f2) < 0)
				  f1 = (Feature) i1.next();
			  else
				  f2 = (Feature) i2.next();
		  } catch (NoSuchElementException e) {
			  flag = false;
		  }
	  }

	  return res;
  }


  /**
    * Two <code>FeatureVector</code>s have equal value if they contain the
    * same number of <code>Feature</code>s and if the values of those
    * <code>Feature</code>s are pair-wise equivalent according to
    * <code>Feature</code>'s <code>valueEquals(String)</code> method.
    *
    * @param vector The vector with which to test equivalence.
    * @return       <code>true</code> iff the two vectors are "value
    *               equivalent" as defined above.
   **/
  public boolean valueEquals(FeatureVector vector)
  {
    if (size() != vector.size()) return false;
    Iterator I = iterator(), J = vector.iterator();
    while (I.hasNext())
      if (!((Feature) I.next())
           .valueEquals(((Feature) J.next()).getStringValue()))
        return false;
    return true;
  }


  /**
    * Returns a string representation of this <code>FeatureVector</code>.  A
    * comma separated list of labels appears first, surrounded by square
    * brackets.  Then follows a comma separated list of features.
    *
    * @return A string representation of this <code>FeatureVector</code>.
   **/
  public String toString()
  {
    String result = "[";
    Iterator I;
    if (labels.size() > 0)
    {
      I = labels.iterator();
      result += I.next();
      while (I.hasNext()) result += ", " + I.next();
    }

    result += "]";
    if (features.size() > 0)
    {
      I = features.iterator();
      result += " " + I.next();
      while (I.hasNext()) result += ", " + I.next();
    }

    return result;
  }


  /**
    * Returns a shallow clone of this vector; the lists are cloned, but their
    * elements aren't.
   **/
  public Object clone()
  {
    FeatureVector clone = new FeatureVector();
    clone.features = (LinkedList) features.clone();
    clone.labels = (LinkedList) labels.clone();
    clone.weight = weight;
    return clone;
  }
}

