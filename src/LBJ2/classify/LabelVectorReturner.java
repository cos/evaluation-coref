package LBJ2.classify;


/**
  * This classifier expects a <code>FeatureVector</code> as input, and it
  * returns the contents of its <code>labels</code> list in a new
  * <code>FeatureVector</code> as output.
  *
  * @author Nick Rizzolo
 **/
public class LabelVectorReturner extends Classifier
{
  /** Default constructor. */
  public LabelVectorReturner() { super("LabelVectorReturner"); }


  /**
    * This method makes one or more decisions about a single object, returning
    * those decisions as <code>Feature</code>s in a vector.
    *
    * @param o  The object to make decisions about.
    * @return   A vector of <code>Feature</code>s about the input object.
   **/
  public FeatureVector classify(Object o)
  {
    FeatureVector vector = null;

    try { vector = (FeatureVector) o; }
    catch (ClassCastException e)
    {
      System.err.println("LBJ2 ERROR: LabelVectorReturner received a '"
                         + o.getClass().getName() + "' as input.");
      System.exit(1);
    }

    FeatureVector result = new FeatureVector();
    result.features.addAll(vector.labels);
    return result;
  }


  /**
    * Returns a string describing the input type of this classifier.
    *
    * @return A string describing the input type of this classifier.
   **/
  public String getInputType() { return "LBJ2.classify.FeatureVector"; }


  /**
    * Returns a string describing the output type of this classifier.
    *
    * @return A string describing the output type of this classifier.
   **/
  public String getOutputType() { return "mixed%"; }


  /** Simply returns the string <code>"LabelVectorReturner"</code>. */
  public String toString() { return "LabelVectorReturner"; }
}

