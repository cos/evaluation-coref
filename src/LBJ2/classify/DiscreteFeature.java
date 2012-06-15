package LBJ2.classify;

import java.io.*;


/**
  * A discrete feature takes on one value from a set of discontinuous values.
  * The set of values that a given <code>DiscreteFeature</code> may take is
  * defined in the <code>Classifier</code> that produced the feature.
  *
  * @author Nick Rizzolo
 **/
public class DiscreteFeature extends Feature
{
  /** Convient access to a common allowable value set. */
  public static final String[] BooleanValues = { "false", "true" };


  /** The discrete value is represented as a string. */
  protected String value;
  /** Index into the set of allowable values corresponding to this value. */
  protected short valueIndex;
  /** The total number of allowable values for this feature. */
  protected short totalValues;


  /**
    * Sets both the identifier and the value.  The value index and total
    * allowable values, having not been specified, default to -1 and 0
    * respectively.
    *
    * @param p  The new discrete feature's package.
    * @param i  The new discrete feature's identifier.
    * @param v  The new discrete feature's value.
   **/
  public DiscreteFeature(String p, String i, String v)
  {
    this(p, i, v, (short) -1, (short) 0);
  }

  /**
    * Sets the identifier, value, value index, and total allowable values.
    *
    * @param p  The new discrete feature's package.
    * @param i  The new discrete feature's identifier.
    * @param v  The new discrete feature's value.
    * @param vi The index corresponding to the value.
    * @param t  The total allowable values for this feature.
   **/
  public DiscreteFeature(String p, String i, String v, short vi, short t)
  {
    super(p, i);
    value = v;
    valueIndex = vi;
    totalValues = t;
  }


  /**
    * Gives a string representation of the value of this feature.
    *
    * @return A string representation of the value of this feature.
   **/
  public String getStringValue() { return value; }


  /** Returns the value of this discrete feature. */
  public String getValue() { return value; }


  /**
    * Determines whether or not the parameter is equivalent to the string
    * representation of the value of this feature.
    *
    * @param v  The string to compare against.
    * @return   <code>true</code> iff the parameter is equivalent to the
    *           string representation of the value of this feature.
   **/
  public boolean valueEquals(String v) { return v.equals(value); }


  /**
    * Make sure that contained <code>String</code>s are
    * <code>intern()</code>ed.
   **/
  public void intern()
  {
    super.intern();
    value = value.intern();
  }


  /**
    * Returns the index of this feature's value in the allowable values array.
   **/
  public short getValueIndex() { return valueIndex; }


  /**
    * Returns the total number of values this feature might possibly be set
    * to.
    *
    * @return Some integer greater than 1 iff this feature is a discrete
    *         feature with a specified value list, and 0 otherwise.
   **/
  public short getTotalValues() { return totalValues; }


  /**
    * Returns the total number of values this feature might possibly be set
    * to.
    *
    * @return Some integer greater than 1 iff this feature is a discrete
    *         feature with a specified value list, and 0 otherwise.
   **/
  public int totalValues() { return totalValues; }


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
    String[] allowable = c.allowableValues();
    String result = f.getValue() + "&" + value;
    if (allowable.length == 0)
      return new DiscreteFeature(c.containingPackage, c.name, result);
    short v = c.valueIndexOf(result);
    return
      new DiscreteFeature(c.containingPackage, c.name, result, v,
                          (short) allowable.length);
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
    if (allowable.length == 0)
      return
        new DiscreteArrayFeature(c.containingPackage, c.name, result,
                                 f.getArrayIndex(), f.getArrayLength());
    short v = c.valueIndexOf(result);
    return
      new DiscreteArrayFeature(c.containingPackage, c.name, result, v,
                               (short) allowable.length, f.getArrayIndex(),
                               f.getArrayLength());
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
    return
      new RealFeature(c.containingPackage, c.name + "_" + value,
                      f.getValue());
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
    return
      new RealArrayFeature(c.containingPackage, c.name + "_" + value,
                           f.getValue(), f.getArrayIndex(),
                           f.getArrayLength());
  }


  /**
    * The hash code of a <code>DiscreteFeature</code> is the sum of the hash
    * codes of its containing package, identifier, and value.
    *
    * @return The hash code of this <code>Feature</code>.
   **/
  public int hashCode() { return super.hashCode() + value.hashCode(); }


  /**
    * Two <code>DiscreteFeature</code>s are equivalent when their containing
    * packages, identifiers, and values are equivalent.
    *
    * @param o  The object with which to compare this <code>Feature</code>.
    * @return   <code>true</code> iff the parameter is an equivalent
    *           <code>Feature</code>.
   **/
  public boolean equals(Object o)
  {
    return super.equals(o) && value.equals(((DiscreteFeature) o).value);
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
    int superComparison = super.compareTo(o);
    if (superComparison != 0) return superComparison;
    assert o instanceof DiscreteFeature
         : "Strangeness in DiscreteFeature.compareTo(Object)";
    return value.compareTo(((DiscreteFeature) o).value);
  }


  /**
    * The string representation of a discrete feature is its identifier
    * followed by its value surrounded by curly braces.
    *
    * @return The string representation of this <code>Feature</code>.
   **/
  public String toString()
  {
    return containingPackage + ":" + identifier + "(" + value + ")";
  }


  /**
    * Special handling during deserialization to ensure that
    * <code>Strings</code> are <code>intern()</code>ed.
    *
    * @param in The stream to deserialize from.
   **/
  private void readObject(java.io.ObjectInputStream in)
          throws IOException, ClassNotFoundException
  {
    in.defaultReadObject();
    value = value.intern();
  }
}

