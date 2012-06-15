package LBJ2.classify;


/**
  * A real array feature keeps track of its index in the classifier's returned
  * array.
  *
  * @author Nick Rizzolo
 **/
public class RealArrayFeature extends RealFeature
{
  /** The feature's index in the returned array it is contained in. */
  protected int arrayIndex;
  /** The size of the returned array this feature is contained in. */
  protected int arrayLength;


  /**
    * Sets all member variables.
    *
    * @param p  The new real feature's package.
    * @param id The new real feature's identifier.
    * @param v  The new real feature's value.
    * @param i  The index of this feature in the returned array.
    * @param l  The length of the array this feature is contained in.
   **/
  public RealArrayFeature(String p, String id, double v, int i, int l)
  {
    super(p, id, v);
    arrayIndex = i;
    arrayLength = l;
  }


  /**
    * Determines if this feature comes from an array.
    *
    * @return <code>true</code>.
   **/
  public boolean fromArray() { return true; }


  /**
    * If this feature is an array feature, call this method to set its array
    * length; otherwise, this method has no effect.
    *
    * @param l  The new length.
   **/
  public void setArrayLength(int l) { arrayLength = l; }


  /** Returns the array index of this feature. */
  public int getArrayIndex() { return arrayIndex; }


  /** Returns the length of the feature array that this feature comes from. */
  public int getArrayLength() { return arrayLength; }


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  public Feature conjunction(Feature f, Classifier c)
  {
    return f.conjunctWith(this, c);
  }


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  protected Feature conjunctWith(DiscreteFeature f, Classifier c)
  {
    return f.conjunctWith(this, c);
  }


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  protected Feature conjunctWith(DiscreteArrayFeature f, Classifier c)
  {
    return f.conjunctWith(this, c);
  }


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  protected Feature conjunctWith(RealFeature f, Classifier c)
  {
    return f.conjunctWith(this, c);
  }


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  protected Feature conjunctWith(RealArrayFeature f, Classifier c)
  {
    int index = f.getArrayIndex() * arrayLength + arrayIndex;
    int length = f.getArrayLength() * arrayLength;
    return
      new RealArrayFeature(c.containingPackage, c.name, value * f.getValue(),
                           index, length);
  }


  /**
    * The hash code of a <code>RealArrayFeature</code> is the sum of the hash
    * codes of the containing package, the identifier, the value, and the
    * array index.
    *
    * @return The hash code for this <code>Feature</code>.
   **/
  public int hashCode()
  {
    return super.hashCode() + new Integer(arrayIndex).hashCode();
  }


  /**
    * Two <code>RealArrayFeature</code>s are equivalent when their containing
    * packages, identifiers, indices, and values are equivalent.
    *
    * @param o  The object with which to compare this <code>Feature</code>.
    * @return   True iff the parameter is an equivalent <code>Feature</code>.
   **/
  public boolean equals(Object o)
  {
    return super.equals(o) && arrayIndex == ((RealArrayFeature) o).arrayIndex;
  }


  /**
    * Used to sort features via a lexicographic comparison of their packages
    * and identifiers.
    *
    * @param o  An object to compare with.
    * @return   The same thing that a <code>String</code> comparison of the
    *           concatenations of the features' packages and identifiers would
    *           return.
   **/
  public int compareTo(Object o)
  {
    if (!(o instanceof RealArrayFeature)) return -1;
    RealArrayFeature f = (RealArrayFeature) o;
    int c = containingPackage.compareTo(f.containingPackage);
    if (c != 0) return c;
    c = identifier.compareTo(f.identifier);
    if (c != 0) return c;
    c = arrayIndex - f.arrayIndex;
    if (c != 0) return c;
    double difference = value - ((RealFeature) o).value;
    if (difference < 0) return -1;
    if (difference > 0) return 1;
    return 0;
  }


  /**
    * The string representation of a real feature is its identifier followed
    * by its array index surrounded by square brackets and its value
    * surrounded by curly braces.
    *
    * @return The string representation of this <code>Feature</code>.
   **/
  public String toString()
  {
    return containingPackage + ":" + identifier + "[" + arrayIndex + "]("
           + value + ")";
  }
}

