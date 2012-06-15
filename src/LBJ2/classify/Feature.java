package LBJ2.classify;

import java.io.*;


/**
  * Objects of this class represent the value of a <code>Classifier</code>'s
  * decision.
  *
  * @author Nick Rizzolo
 **/
public abstract class Feature implements Cloneable, Comparable, Serializable
{
  /**
    * The Java <code>package</code> containing the classifier that produced
    * this feature.
   **/
  protected String containingPackage;
  /**
    * The <code>identifier</code> string distinguishes this
    * <code>Feature</code> from other <code>Feature</code>s.
   **/
  protected String identifier;


  /**
    * Initializing constructor.
    *
    * @param p  The package containing the classifier that produced this
    *           feature.
    * @param i  The new feature's identifier.
   **/
  public Feature(String p, String i)
  {
    containingPackage = p;
    identifier = i;
  }


  /** Retrieves this feature's package. */
  public String getPackage() { return containingPackage; }


  /** Retrieves this feature's identifier. */
  public String getIdentifier() { return identifier; }


  /**
    * Gives a string representation of the value of this feature.
    *
    * @return A string representation of the value of this feature.
   **/
  public abstract String getStringValue();


  /**
    * Determines whether the argument feature has equivalent package and
    * identifier.
    *
    * @param f  The feature with which to compare this feature.
    * @return   <code>true</code> iff <code>f</code> has identical containing
    *           package and equivalent identifier.
   **/
  public boolean nameEquals(Feature f)
  {
    assert !(f.containingPackage.equals(containingPackage)
             && f.containingPackage != containingPackage)
         : "Features \"" + f.identifier + "\" and \"" + identifier
           + " have equivalent package strings in different objects.";
    return f.containingPackage == containingPackage
           && f.identifier.equals(identifier);
  }


  /**
    * Determines whether or not the parameter is equivalent to the string
    * representation of the value of this feature.
    *
    * @param v  The string to compare against.
    * @return   <code>true</code> iff the parameter is equivalent to the
    *           string representation of the value of this feature.
   **/
  public abstract boolean valueEquals(String v);


  /**
    * Makes sure that the identifier is <code>intern()</code>ed.  Note that
    * the containing package need not be <code>intern()</code>ed since it's
    * always initialized with literals.
   **/
  public void intern() { identifier = identifier.intern(); }


  /**
    * Determines if this feature comes from an array.
    *
    * @return <code>true</code> iff this feature comes from an array.
   **/
  public boolean fromArray() { return false; }


  /**
    * Returns the total number of values this feature might possibly be set
    * to.
    *
    * @return Some integer greater than 1 iff this feature is a discrete
    *         feature with a specified value list, and 0 otherwise.
   **/
  public int totalValues() { return 0; }


  /**
    * If this feature is an array feature, call this method to set its array
    * length; otherwise, this method has no effect.
    *
    * @param l  The new length.
   **/
  public void setArrayLength(int l) { }


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  public abstract Feature conjunction(Feature f, Classifier c);


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  protected abstract Feature conjunctWith(DiscreteFeature f, Classifier c);


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  protected abstract Feature conjunctWith(DiscreteArrayFeature f,
                                          Classifier c);


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  protected abstract Feature conjunctWith(RealFeature f, Classifier c);


  /**
    * Create a feature representing the conjunction of this feature with the
    * given argument feature.
    *
    * @param f  The feature to conjunct with.
    * @param c  The classifier producing the resulting feature.
    * @return   A feature representing the conjunction of this feature and
    *           <code>f</code>.
   **/
  protected abstract Feature conjunctWith(RealArrayFeature f, Classifier c);


  /**
    * The hash code of a <code>Feature</code> is the hash code of its package
    * plus the hash code of its identifier.
    *
    * @return The hash code of this <code>Feature</code>.
   **/
  public int hashCode()
  {
    return containingPackage.hashCode() + identifier.hashCode();
  }


  /**
    * Two <code>Feature</code>s are equal when their packages and identifiers
    * are equivalent.
    *
    * @return <code>true</code> iff the argument is an equivalent
    *         <code>Feature</code>.
   **/
  public boolean equals(Object o)
  {
    assert !(o instanceof Feature)
           || !(((Feature) o).containingPackage.equals(containingPackage)
                && ((Feature) o).containingPackage != containingPackage)
         : "Features \"" + o + "\" and \"" + this
           + " have equivalent package strings in different objects.";
    assert (getClass() == o.getClass())
           == (getClass().getName().equals(o.getClass().getName()))
         : "getClass() doesn't behave as expected.";

    if (getClass() != o.getClass()) return false;
    Feature f = (Feature) o;
    return f.containingPackage == containingPackage
           && f.identifier.equals(identifier);
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
    if (o.getClass() != getClass())
    {
      boolean thisDiscrete = getClass().getName().indexOf("Discrete") != -1;
      boolean thatDiscrete = o.getClass().getName().indexOf("Discrete") != -1;
      boolean thisArray = getClass().getName().indexOf("Array") != -1;
      if (thisDiscrete && !thatDiscrete) return -1;
      if (!thisDiscrete && thatDiscrete) return 1;
      if (thisArray) return -1;
      return 1;
    }

    Feature f = (Feature) o;
    int packageComparison = containingPackage.compareTo(f.containingPackage);
    if (packageComparison != 0) return packageComparison;
    return identifier.compareTo(f.identifier);
  }


  /** Returns a shallow clone of this <code>Feature</code>. */
  public Object clone()
  {
    Object result = null;

    try { result = super.clone(); }
    catch (Exception e)
    {
      System.err.println("Can't clone feature '" + this + "':");
      e.printStackTrace();
    }

    return result;
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
    containingPackage = containingPackage.intern();
    identifier = identifier.intern();
  }
}

