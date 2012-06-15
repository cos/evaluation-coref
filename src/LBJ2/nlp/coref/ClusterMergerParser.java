package LBJ2.nlp.coref;

import java.util.*;
import LBJ2.classify.Classifier;
import LBJ2.parse.Parser;


/**
  * Implements a beam search over the document renditions returned by an
  * instance of {@link ClusterMerger}.
 **/
public class ClusterMergerParser implements Parser
{
  /** A file containing the names of files to be parsed. */
  private String filename;
  /** A parser returning labeled documents. */
  private Parser parser;
  /** The document are currently merging clusters over. */
  private Document currentDocument;
  /**
    * A coreference classifier that takes a pair of {@link Mention}s as input.
   **/
  private Classifier coref;
  /** The maximum number of document renditions in the beam. */
  private int beamWidth;
  /** The maximum number of mergings allowed to create a new rendition. */
  protected int maxDepth;
  /** The beam. */
  protected LinkedList<ClusterMerger> beam;
  /**
    * When all documents in the {@link #beam} have been processed, this
    * becomes the new beam.
   **/
  private LinkedList<ClusterMerger> nextLevel;
  /** This comparator puts the "best" documents first in the beam. */
  private Comparator<ClusterMerger> key;
  /**
    * Remembers the depth of the most recent document returned by
    * {@link #next()}.
   **/
  private int lastDepth;
  /** Filters the cluster merging process. */
  private ClusterMerger.Filter filter;


  /**
    * Use of this constructor implies that document renditions will be ranked
    * by gold <i>B<sup>3</sup></i> score.
    *
    * @param file The name of the input file.
    * @param c    A coreference classifier.
    * @param b    Value for {@link #beamWidth}.
    * @param d    Value for {@link #maxDepth}.
   **/
  public ClusterMergerParser(String file, Classifier c, int b, int d)
  {
    this(file, c, b, d, new GoldKey(), new ClusterMerger().new Filter());
  }

  /**
    * Use of this constructor implies that document renditions will be ranked
    * by gold <i>B<sup>3</sup></i> score.
    *
    * @param file The name of the input file.
    * @param c    A coreference classifier.
    * @param b    Value for {@link #beamWidth}.
    * @param d    Value for {@link #maxDepth}.
    * @param f    A filter used during the search.
   **/
  public ClusterMergerParser(String file, Classifier c, int b, int d,
                             ClusterMerger.Filter f)
  {
    this(file, c, b, d, new GoldKey(), f);
  }

  /**
    * Use of this constructor implies that document renditions will be ranked
    * by coherence score.
    *
    * @param file   The name of the input file.
    * @param coref  A coreference classifier.
    * @param b      Value for {@link #beamWidth}.
    * @param d      Value for {@link #maxDepth}.
    * @param cohere The coherence classifier whose scores will be used to rank
    *               document renditions.
   **/
  public ClusterMergerParser(String file, Classifier coref, int b, int d,
                             Classifier cohere)
  {
    this(file, coref, b, d, new CoherenceKey(cohere),
         new ClusterMerger().new Filter());
  }

  /**
    * Use of this constructor implies that document renditions will be ranked
    * by coherence score.
    *
    * @param file   The name of the input file.
    * @param coref  A coreference classifier.
    * @param b      Value for {@link #beamWidth}.
    * @param d      Value for {@link #maxDepth}.
    * @param cohere The coherence classifier whose scores will be used to rank
    *               document renditions.
    * @param f      A filter used during the search.
   **/
  public ClusterMergerParser(String file, Classifier coref, int b, int d,
                             Classifier cohere, ClusterMerger.Filter f)
  {
    this(file, coref, b, d, new CoherenceKey(cohere), f);
  }

  /**
    * Initializing constructor.
    *
    * @param file The name of the input file.
    * @param c    A coreference classifier.
    * @param b    Value for {@link #beamWidth}.
    * @param d    Value for {@link #maxDepth}.
    * @param k    Value for {@link #key}; probably an instance of
    *             {@link ClusterMergerParser.GoldKey} or
    *             {@link ClusterMergerParser.CoherenceKey}.
    * @param f    A filter used during the search.
   **/
  protected ClusterMergerParser(String file, Classifier c, int b, int d,
                                Comparator<ClusterMerger> k,
                                ClusterMerger.Filter f)
  {
    filename = file;
    parser = new ACE2004DocumentParser(filename);
    beam = new LinkedList<ClusterMerger>();
    nextLevel = new LinkedList<ClusterMerger>();
    coref = c;
    beamWidth = b;
    maxDepth = d;
    key = k;
    filter = f;
  }


  /**
    * Return the next document rendition.
    *
    * @return The next document rendition.
   **/
  public Object next()
  {
    if (beam.size() == 0)
    {
      if (nextLevel.size() > 0)
      {
        beam = nextLevel;
        nextLevel = new LinkedList<ClusterMerger>();
      }
      else
      {
        currentDocument = (Document) parser.next();
        if (currentDocument == null) return null;
        beam.add(new ClusterMerger(currentDocument, coref, filter));
      }
    }

    ClusterMerger result = updateBeam();
    lastDepth = result.getDepth();
    return result.getDocument();
  }


  /** Returns the value of {@link #lastDepth}. */
  public int getLastDepth() { return lastDepth; }


  /**
    * Pop the first document in the beam, expand on it, resort, trim, and
    * return the popped document.
    *
    * @return The popped document from the top of the beam.
   **/
  protected ClusterMerger updateBeam()
  {
    ClusterMerger top = beam.removeFirst();
    if (top.getDepth() >= maxDepth) return top;

    for (ClusterMerger c = top.next(); c != null; c = top.next())
    {
      if (nextLevel.size() == 0 || key.compare(c, nextLevel.getFirst()) < 0)
        nextLevel.addFirst(c);
      else
      {
        ListIterator<ClusterMerger> I =
          nextLevel.listIterator(nextLevel.size());
        for (ClusterMerger p = I.previous(); key.compare(c, p) < 0;
             p = (ClusterMerger) I.previous());
        I.next();
        I.add(c);
      }

      if (nextLevel.size() > beamWidth) nextLevel.removeLast();
    }

    return top;
  }


  /** Sets this parser back to the beginning of the raw data. */
  public void reset()
  {
    parser.reset();
    beam = new LinkedList<ClusterMerger>();
    nextLevel = new LinkedList<ClusterMerger>();
  }


  /**
    * Simply a comparator that orders documents with better
    * <i>B<sup>3</sup></i> scores first.
   **/
  private static class GoldKey implements Comparator<ClusterMerger>
  {
    public int compare(ClusterMerger c1, ClusterMerger c2)
    {
      return c2.compareTo(c1);
    }
  }


  /**
    * A comparator that orders documents with higher coherence score first.
   **/
  private static class CoherenceKey implements Comparator<ClusterMerger>
  {
    /**
      * A coherence classifier which takes a pair of documents as input and
      * returns <code>true</code> iff the first document is more coherent.
     **/
    private Classifier cohere;


    /**
      * Initializing constructor.
      *
      * @param c  Value for {@link #cohere}.
     **/
    public CoherenceKey(Classifier c) { cohere = c; }


    public int compare(ClusterMerger c1, ClusterMerger c2)
    {
      if (cohere.discreteValue(
            new Document[]{ c1.getDocument(), c2.getDocument() })
          .equals("true"))
        return -1;
      return 1;
    }
  }
}

