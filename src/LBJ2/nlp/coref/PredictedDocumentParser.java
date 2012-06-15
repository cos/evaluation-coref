package LBJ2.nlp.coref;

import LBJ2.classify.Classifier;
import LBJ2.parse.Parser;


/**
  * Given a parser that produces {@link Document}s and a classifier that can
  * predict if a given pair of mentions from that document are coreferent,
  * this parser will produce new {@link Document} objects whose mentions'
  * {@link Mention#entityID} fields are consistent with the predictions of the
  * classifier.  The classifier is evaluated by taking each mention in turn
  * and looking for the closest previous mention that the classifier predicts
  * is coreferent.
 **/
public class PredictedDocumentParser implements Parser
{
  /** The parser that produces {@link Document}s for the classifier. */
  protected Parser parser;
  /** The classifier that relabels the document. */
  protected Classifier classifier;


  /**
    * Initializing constructor.
    *
    * @param p  The parser that produces {@link Document}s for the classifier.
    * @param c  The classifier that relabels the document.
   **/
  public PredictedDocumentParser(Parser p, Classifier c)
  {
    parser = p;
    classifier = c;
  }


  /**
    * Returns the next {@link Document} from {@link #parser}, relabeled
    * according to {@link #classifier} using the algorithm described above.
    *
    * @return The next document or <code>null</code> if there aren't any more.
   **/
  public Object next()
  {
    Document d = (Document) parser.next();
    if (d == null) return null;
    Document p = new Document(d);
    p.fillInPredictions(classifier, 0);
    return p;
  }


  /** Starts parsing over again. */
  public void reset() { parser.reset(); }
}

