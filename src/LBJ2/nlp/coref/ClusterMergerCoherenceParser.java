package LBJ2.nlp.coref;

import java.util.*;
import LBJ2.classify.Classifier;
import LBJ2.parse.Parser;


/**
  * Returns the same objects as the {@link ClusterMergerParser} sent to its
  * constructor after wrapping them in a document pair with either a gold
  * labeled document or a document labeled with a specified coreference
  * classifier.
 **/
public class ClusterMergerCoherenceParser implements Parser
{
  /** A parser returning <code>Document</code>s with merged clusters. */
  private ClusterMergerParser parser;
  /** The original document whose clusters are being merged. */
  private Document currentDocument;
  /** Used to produce an alternative to the gold labeled rendition. */
  private Classifier coref;
  /** For shuffled pair order. */
  private Random random;


  /**
    * Every produced document pair will contain a gold labeled rendition of
    * the document.
    *
    * @param p  A value for {@link #parser}.
   **/
  public ClusterMergerCoherenceParser(ClusterMergerParser p) { parser = p; }

  /**
    * Every produced document pair will contain a rendition of the document
    * labeled by the specified coreference classifier.
    *
    * @param p  A value for {@link #parser}.
    * @param c  A value for {@link #coref}.
   **/
  public ClusterMergerCoherenceParser(ClusterMergerParser p, Classifier c)
  {
    parser = p;
    coref = c;
  }


  /**
    * Return a document pair containing the next document rendition and its
    * corresponding gold document.
    *
    * @return A document pair containing the next document rendition and its
    *         corresponding gold document.
   **/
  public Object next()
  {
    Document next = (Document) parser.next();
    if (next == null) return null;

    Document original = next.getLabeled();

    if (currentDocument != original)
    {
      if (coref != null)
      {
        currentDocument = new Document(original);
        currentDocument.fillInPredictions(coref, 0);
      }
      else currentDocument = original;

      random = new Random(currentDocument.getName().hashCode());
    }

    if (random.nextBoolean()) return new Document[]{ currentDocument, next };
    return new Document[]{ next, currentDocument };
  }


  /** Sets this parser back to the beginning of the raw data. */
  public void reset() { parser.reset(); }
}

