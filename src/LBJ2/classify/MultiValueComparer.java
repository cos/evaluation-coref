package LBJ2.classify;

import java.util.Iterator;


/**
  * This classifier applies another classifier to the example object and
  * returns a Boolean feature (with value "true" or "false") indicating
  * whether a given feature value appeared in the output of the classifier.
  * This behavior differs from that of {@link ValueComparer} because it does
  * not assume that the given classifier will return only a single feature.
  *
  * @author Nick Rizzolo
 **/
public class MultiValueComparer extends ValueComparer
{
  /**
    * Constructor.
    *
    * @param c  The classifier whose value will be compared.
    * @param v  The value to compare with.
   **/
  public MultiValueComparer(Classifier c, String v) { super(c, v); }


  /**
    * Returns a Boolean feature (with value "true" or "false") indicating
    * whether the output of {@link ValueComparer#labeler} applied to the
    * argument object contained the feature value referenced by
    * {@link ValueComparer#value}.
    *
    * @param o  The object to make decisions about.
    * @return   A feature vector containing the feature described above.
   **/
  public FeatureVector classify(Object o)
  {
    boolean prediction = false;

    for (Iterator I = labeler.classify(o).iterator();
         I.hasNext() && !prediction; )
      prediction = ((DiscreteFeature) I.next()).getValue().equals(value);

    short p = prediction ? (short) 1 : (short) 0;
    return
      new FeatureVector(
          new DiscreteFeature("", "MultiValueComparer",
                              DiscreteFeature.BooleanValues[p], p,
                              (short) 2));
  }


  /**
    * The <code>String</code> representation of a <code>ValueComparer</code>
    * has the form <code>"ValueComparer(</code><i>child</i><code>)</code>,
    * where <i>child</i> is the <code>String</code> representation of the
    * classifier whose value is being compared.
    *
    * @return A string of the form described above.
   **/
  public String toString()
  {
    return "MultiValueComparer(" + labeler + ")";
  }
}

