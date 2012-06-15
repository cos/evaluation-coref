package LBJ2.nlp.coref;

import LBJ2.classify.*;
import LBJ2.parse.*;


/**
  * This class implements a classifier which takes the predictions of another
  * coreference classifier and then merges the singleton pronouns.
 **/
public abstract class MergedPronouns extends Classifier
{
  /**
    * A classifier to start with whose singleton pronouns will then be merged.
   **/
  protected Classifier coref;
  /**
    * Caches the current document, which will contain this classifier's
    * predictions.
   **/
  protected Document document;


  /**
    * Initializing constructor.
    *
    * @param n  The name of the classifier.
    * @param c  The coref classifier.
   **/
  public MergedPronouns(String n, Classifier c)
  {
    super(n);
    coref = c;
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
    * Returns the value after merging pronouns.
    *
    * @param example  The example to classify.
    * @return The classification for the given example.
   **/
  public String discreteValue(Object example)
  {
    Document.Mention[] pair = (Document.Mention[]) example;
    Document labeled = pair[0].getDocument();

    if (document == null || !document.getName().equals(labeled.getName()))
    {
      document = new Document(labeled);
      document.fillInPredictions(coref, 0);
      new CoreferenceTester(0).mergeSingletonPronouns(document, labeled, 0);
    }

    int i0 = pair[0].getSentenceIndex();
    int j0 = pair[0].getIndexInSentence();
    int i1 = pair[1].getSentenceIndex();
    int j1 = pair[1].getIndexInSentence();
    return "" + document.getMention(i0, j0).getEntityID()
                .equals(document.getMention(i1, j1).getEntityID());
  }


  /**
    * Looks up the score set for the given example and then returns a feature
    * whose value had the highest score in the score set wrapped in a feature
    * vector.
    *
    * @param example  The example for which a classification is desired.
    * @return A vector containing a single feature whose value is set as
    *         described above.
   **/
  public FeatureVector classify(Object example)
  {
    return
      new FeatureVector(
          new DiscreteFeature(containingPackage, name,
                              discreteValue(example)));
  }


  /** Returns a hash code for this object. */
  public int hashCode() { return name.hashCode(); }
}

