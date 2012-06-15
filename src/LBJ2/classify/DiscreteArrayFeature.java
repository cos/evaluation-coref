package LBJ2.classify;


/**
  * A discrete array feature keeps track of its index in the classifier's
  * returned array as well as the total number of features in that array.
  *
  * @author Nick Rizzolo
 **/
public class DiscreteArrayFeature extends DiscreteFeature
{
  /** The feature's index in the returned array it is contained in. */
  protected int arrayIndex;
  /** The size of the returned array this feature is contained in. */
  protected int arrayLength;


  /**
    * Sets the identifier, value, array index, and size of the containing
    * array.  The value index and total allowable values, having not been
    * specified, default to -1 and 0 respectively.
    *
    * @param p  The new discrete feature's package.
    * @param id The new discrete feature's identifier.
    * @param v  The new discrete feature's value.
    * @param i  The index of this feature in the returned array.
    * @param l  The length of the array this feature is contained in.
   **/
  public DiscreteArrayFeature(String p, String id, String v, int i, int l)
  {
    this(p, id, v, (short) -1, (short) 0, i, l);
  }

  /**
    * Sets all member variables.
    *
    * @param p  The new discrete feature's package.
    * @param id The new discrete feature's identifier.
    * @param v  The new discrete feature's value.
    * @param vi The index corresponding to the value.
    * @param t  The total allowable values for this feature.
    * @param i  The index of this feature in the returned array.
    * @param l  The length of the array this feature is contained in.
   **/
  public DiscreteArrayFeature(String p, String id, String v, short vi,
                              short t, int i, int l)
  {
    super(p, id, v, vi, t);
    arrayIndex = i;
    arrayLength = l;
  }


  /** Returns the array index of this feature. */
  public int getArrayIndex() { return arrayIndex; }


  /** Returns the length of the feature array that this feature comes from. */
  public int getArrayLength() { return arrayLength; }


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
    String[] allowable = c.allowableValues();
    String result = f.getValue() + "&" + value;
    int index = f.getArrayIndex() * arrayLength + arrayIndex;
    int length = f.getArrayLength() * arrayLength;
    if (allowable.length == 0)
      return
        new DiscreteArrayFeature(c.containingPackage, c.name, result, index,
                                 length);
    short v = c.valueIndexOf(result);
    return
      new DiscreteArrayFeature(c.containingPackage, c.name, result, v,
                               (short) allowable.length, index, length);
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
    String id = c.name + "_" + arrayIndex + "_" + value;
    double result = f.getValue();
    return new RealFeature(c.containingPackage, id, result);
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
    String id = c.name + "_" + arrayIndex + "_" + value;
    return
      new RealArrayFeature(c.containingPackage, id, f.getValue(),
                           f.getArrayIndex(), f.getArrayLength());
  }


  /**
    * The hash code of a <code>DiscreteArrayFeature</code> is the sum of the
    * hash codes of the containing package, the identifier, the value and the
    * array index.
    *
    * @return The hash code of this <code>Feature</code>.
   **/
  public int hashCode()
  {
    return super.hashCode() + new Integer(arrayIndex).hashCode();
  }


  /**
    * Two <code>DiscreteArrayFeature</code>s are equivalent when their
    * containing packages, identifiers, indices, and values are equivalent.
    *
    * @param o  The object with which to compare this <code>Feature</code>.
    * @return   True iff the parameter is an equivalent <code>Feature</code>.
   **/
  public boolean equals(Object o)
  {
    return
      super.equals(o) && arrayIndex == ((DiscreteArrayFeature) o).arrayIndex;
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
    if (!(o instanceof DiscreteArrayFeature)) return -1;
    DiscreteArrayFeature f = (DiscreteArrayFeature) o;
    int c = containingPackage.compareTo(f.containingPackage);
    if (c != 0) return c;
    c = identifier.compareTo(f.identifier);
    if (c != 0) return c;
    c = arrayIndex - f.arrayIndex;
    if (c != 0) return c;
    return value.compareTo(f.value);
  }


  /**
    * The string representation of a discrete feature is its identifier
    * followed by its array index surrounded by square brackets and its value
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

