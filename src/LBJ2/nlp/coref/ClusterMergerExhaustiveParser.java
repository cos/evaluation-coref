package LBJ2.nlp.coref;

import java.util.*;
import LBJ2.classify.Classifier;


/**
  * Returns all the document renditions resulting from merging clusters
  * starting from the rendition produced by a coreference classifier.
 **/
public class ClusterMergerExhaustiveParser extends ClusterMergerParser
{
  /**
    * Initializing constructor.
    *
    * @param file A file containing the names of files to be parsed.
    * @param c    A value for {@link #coref}.
    * @param d    A value for {@link #maxDepth}.
   **/
  public ClusterMergerExhaustiveParser(String file, Classifier c, int d)
  {
    this(file, c, d, new ClusterMerger().new Filter());
  }

  /**
    * Initializing constructor.
    *
    * @param file A file containing the names of files to be parsed.
    * @param c    A value for {@link #coref}.
    * @param d    A value for {@link #maxDepth}.
    * @param f    A filter used during the search.
   **/
  public ClusterMergerExhaustiveParser(String file, Classifier c, int d,
                                       ClusterMerger.Filter f)
  {
    super(file, c, 0, d, (Comparator<ClusterMerger>) null, f);
  }


  /**
    * Pop the first document in the beam, expand on it, resort, trim, and
    * return the popped document.
    *
    * @return The popped document from the top of the beam.
   **/
  protected ClusterMerger updateBeam()
  {
    for (boolean process = false; !process; )
    {
      ClusterMerger top = beam.getFirst();
      process = top.getDepth() >= maxDepth;

      if (!process)
      {
        ClusterMerger merger = top.next();
        if (merger != null) beam.addFirst(merger);
        else process = true;
      }
    }

    return beam.removeFirst();
  }
}

