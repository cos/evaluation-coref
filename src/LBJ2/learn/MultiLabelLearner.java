package LBJ2.learn;

import java.util.*;
import LBJ2.classify.*;


/**
  * A simple implementation of a learner that learns from examples with
  * multiple labels and is capable of predicting multiple labels on new
  * examples.  A separate {@link LinearThresholdUnit} is learned independently
  * to predict whether each label is appropriate for a given example.  Any
  * {@link LinearThresholdUnit} may be used, so long as it implements its
  * <code>clone()</code> method and a public constructor that takes no
  * arguments.  During testing, the {@link #classify(Object)} method returns a
  * separate feature for each {@link LinearThresholdUnit} whose score on the
  * example object exceeds the threshold.
  *
  * @author Nick Rizzolo
 **/
public class MultiLabelLearner extends SparseNetworkLearner
{
  /**
    * Instantiates this multi-label learner with the default learning
    * algorithm: {@link SparsePerceptron}.
   **/
  public MultiLabelLearner() { this(""); }

  /**
    * Instantiates this multi-label learner using the specified algorithm to
    * learn each class separately as a binary classifier.  This constructor
    * will normally only be called by the compiler.
    *
    * @param ltu  The linear threshold unit used to learn binary classifiers.
   **/
  public MultiLabelLearner(LinearThresholdUnit ltu) { this("", ltu); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link MultiLabelLearner.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public MultiLabelLearner(Parameters p) { this("", p); }

  /**
    * Instantiates this multi-label learner with the default learning
    * algorithm: {@link SparsePerceptron}.
    *
    * @param n  The name of the classifier.
   **/
  public MultiLabelLearner(String n) { super(n); }

  /**
    * Instantiates this multi-label learner using the specified algorithm to
    * learn each class separately as a binary classifier.
    *
    * @param n    The name of the classifier.
    * @param ltu  The linear threshold unit used to learn binary classifiers.
   **/
  public MultiLabelLearner(String n, LinearThresholdUnit ltu)
  {
    super(n, ltu);
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link MultiLabelLearner.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public MultiLabelLearner(String n, Parameters p)
  {
    super(n, p);
  }


  /** This learner's output type is <code>"discrete%"</code>. */
  public String getOutputType() { return "discrete%"; }


  /**
    * Each example is treated as a positive example for each linear threshold
    * unit associated with a feature produced by the labeler, and as a
    * negative example for all other linear threshold units in the network.
    *
    * @param example  The example object.
   **/
  public void learn(Object example)
  {
    FeatureVector labels = labeler.classify(example);

    for (Iterator I = labels.iterator(); I.hasNext(); )
    {
      String labelValue = ((DiscreteFeature) I.next()).getValue();

      if (!network.containsKey(labelValue))
      {
        LinearThresholdUnit ltu = (LinearThresholdUnit) baseLTU.clone();
        ltu.setLabeler(new MultiValueComparer(labeler, labelValue));
        ltu.setExtractor(extractor);
        network.put(labelValue, ltu);
      }
    }

    for (Iterator I = network.values().iterator(); I.hasNext(); )
      ((LinearThresholdUnit) I.next()).learn(example);
  }


  /**
    * Returns a separate feature for each {@link LinearThresholdUnit} whose
    * score on the example object exceeds the threshold.
    *
    * @param example  The example object.
    * @return         A vector containing the features described above.
   **/
  public FeatureVector classify(Object example)
  {
    Score[] s = scores(example).toArray();
    FeatureVector result = new FeatureVector();

    for (int i = 0; i < s.length; ++i)
      if (s[i].score >= 0)
        result.addFeature(
          new DiscreteFeature(containingPackage, name, s[i].value,
                              valueIndexOf(s[i].value),
                              (short) allowableValues().length));

    return result;
  }


  /**
    * Simply a container for all of {@link MultiLabelLearner}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends SparseNetworkLearner.Parameters
  {
  }
}

