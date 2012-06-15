package LBJ2.nlp.coref;

import java.util.*;
import LBJ2.classify.*;


/**
  * Takes the labels present in a given document and perturbs them slightly.
  * For each mention in a given document, if the decision is made to change
  * that mention's ID, it can either be given a new ID not previously found in
  * the document, or it can be randomly assigned the same ID as some mention
  * that came before it.  The decision to make a change is governed by
  * {@link #changeProbability}, while the decision of how to change is
  * governed by {@link #newIDProbability}.
 **/
public class PerturbedLabels extends Classifier
{
  /** The probability that a mention's ID will be changed. */
  private static double changeProbability = .1;
  /**
    * The probability that a mention will be assigned a new ID, given that we
    * have decided to change it at all.
   **/
  private static double newIDProbability = .35;


  /**
    * Sets the values of {@link #changeProbability} and
    * {@link #newIDProbability}.
    *
    * @param c  The new value for {@link #changeProbability}.
    * @param n  The new value for {@link #newIDProbability}.
   **/
  public static void setParameters(double c, double n)
  {
    changeProbability = c;
    newIDProbability = n;
  }


  /** Remembers the predictions this classifier makes for a given document. */
  protected WeakHashMap<Document, String[]> predictionMap;


  /** Sets the classifier's name to the default. */
  public PerturbedLabels()
  {
    super("LBJ2.nlp.coref.PerturbedLabels");
    predictionMap = new WeakHashMap<Document, String[]>();
  }


  /** Returns the input type of this classifier. */
  public String getInputType() { return "[LLBJ2.nlp.coref.Document$Mention;"; }
  /** Returns the feature output type of this classifier. */
  public String getOutputType() { return "discrete"; }
  /**
    * Returns the values that a feature returned by this classifier is allowed
    * to take.  This classifier is Boolean, and its features are either
    * <code>"false"</code> or <code>"true"</code>.
   **/
  public String[] allowableValues() { return DiscreteFeature.BooleanValues; }


  /**
    * Specifically designed to classify {@link Mention}<code>[]</code>s.
    *
    * @param ex The example object to classify.
    * @return The prediction of this classifier.
   **/
  public String discreteValue(Object ex)
  {
    Document.Mention[] pair = (Document.Mention[]) ex;
    Document d = pair[0].getDocument();
    String[] predictions = predictionMap.get(d);

    if (predictions == null)
    {
      predictions = new String[d.totalMentions()];
      predictionMap.put(d, predictions);

      for (int i = 0; i < d.sentences(); ++i)
        for (int j = 0; j < d.mentionsInSentence(i); ++j)
        {
          Document.Mention m = d.getMention(i, j);
          predictions[m.getIndexInDocument()] = m.getEntityID();
        }

      Random r = new Random(d.getName().hashCode());
      int nextID = 0;

      for (int i = 0; i < predictions.length; ++i)
      {
        if (r.nextDouble() >= changeProbability) continue;

        if (r.nextDouble() < newIDProbability)
        {
          predictions[i] = "e" + nextID++;
          continue;
        }

        if (i == 0) continue;
        predictions[i] = predictions[r.nextInt(i)];
      }
    }

    return "" + predictions[pair[0].getIndexInDocument()]
                .equals(predictions[pair[1].getIndexInDocument()]);
  }


  /**
    * Specifically designed to classify {@link Mention}<code>[]</code>s.
    *
    * @param example  The example object to classify.
    * @return A vector containing the single feature representing the
    *         prediction of this classifier.
   **/
  public FeatureVector classify(Object example)
  {
    return
      new FeatureVector(
          new DiscreteFeature(containingPackage, name,
                              discreteValue(example)));
  }
}

