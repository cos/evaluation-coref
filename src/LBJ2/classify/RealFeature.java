package LBJ2.classify;


/**
  * A real feature takes on any value representable by a <code>double</code>.
  *
  * @author Nick Rizzolo
 **/
public class RealFeature extends Feature
{
  /** The real value is represented as a double. */
  protected double value;


  /**
    * Sets both the identifier and the value.
    *
    * @param p  The new real feature's package.
    * @param i  The new <code>RealFeature</code>'s identifier.
    * @param v  The new <code>RealFeature</code>'s value.
   **/
  public RealFeature(String p, String i, double v)
  {
    super(p, i);
    value = v;
  }


  /** Returns the value of the <code>value</code> member variable. */
  public double getValue() { return value; }


  /**
    * Gives a string representation of the value of this feature.
    *
    * @return A string representation of the value of this feature.
   **/
  public String getStringValue() { return "" + value; }


  /**
    * Determines whether or not the parameter is equivalent to the string
    * representation of the value of this feature.
    *
    * @param v  The string to compare against.
    * @return   <code>true</code> iff the parameter is equivalent to the
    *           string representation of the value of this feature.
   **/
  public boolean valueEquals(String v) { return v.equals("" + value); }


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
    return new RealFeature(c.containingPackage, c.name, value * f.getValue());
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
      new RealArrayFeature(c.containingPackage, c.name, value * f.getValue(),
                           f.getArrayIndex(), f.getArrayLength());
  }


  /**
    * The hash code of a <code>RealFeature</code> is the sum of the hash codes
    * of the containing package, the identifier, and the value.
    *
    * @return The hash code for this <code>Feature</code>.
   **/
  public int hashCode()
  {
    return super.hashCode() + new Double(value).hashCode();
  }


  /**
    * Two <code>RealFeature</code>s are equivalent when their containing
    * packages and identifiers are equivalent and their values are equal.
    *
    * @param o  The object with which to compare this <code>Feature</code>.
    * @return   True iff the parameter is an equivalent <code>Feature</code>.
   **/
  public boolean equals(Object o)
  {
    return super.equals(o) && value == ((RealFeature) o).value;
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
    double difference = value - ((RealFeature) o).value;
    if (difference < 0) return -1;
    if (difference > 0) return 1;
    return 0;
  }


  /**
    * The string representation of a real feature is its identifier followed
    * by its value surrounded by parentheses.
    *
    * @return The string representation of this <code>Feature</code>.
   **/
  public String toString()
  {
    return containingPackage + ":" + identifier + "(" + value + ")";
  }
}

