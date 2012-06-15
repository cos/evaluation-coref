package LBJ2.nlp.coref;

import java.util.*;
import LBJ2.classify.*;
import LBJ2.learn.*;
import LBJ2.infer.*;


/**
  * Attempts to merge clusters produced by a coreference classifier via a
  * beam search.  This inference algorithm is specifically designed to work
  * with coreference classifiers that take pairs of
  * <code>LBJ2.nlp.coref.Document.Mention</code>s in an array as input and
  * coherence classifiers that take pairs of
  * <code>LBJ2.nlp.coref.Document</code>s in an array as input and will not
  * work with any other classifiers.
 **/
public class ClusterMerge extends Inference
{
  /** The classifier being reranked. */
  private DataCoref classifier;
  /** A coherence classifier to do the reranking. */
  private Learner cohere;
  /** The "head" document, more conveniently accessible. */
  private Document headDocument;
  /**
    * All coreference scores are multiplied by this weight, and all coherence
    * scores are multiplied by 1 minus this weight.
   **/
  private double alpha;


  /**
    * Don't use this constructor, because it doesn't set values for
    * {@link #classifier} or {@link #cohere}.
   **/
  public ClusterMerge() { this(null, null, .5); }

  /**
    * Initializes member variables, but not the head object.
    *
    * @param c  The classifier being reranked.
    * @param cc A coherence classifier to do the re-ranking.
    * @param a  A value for {@link #alpha}.
   **/
  public ClusterMerge(DataCoref c, Learner cc, double a)
  {
    this(null, c, cc, a);
  }

  /**
    * Initializes member variables including the head object.
    *
    * @param h  The head object.
    * @param c  The classifier being reranked.
    * @param cc A coherence classifier to do the re-ranking.
    * @param a  A value for {@link #alpha}.
   **/
  public ClusterMerge(Object h, DataCoref c, Learner cc, double a)
  {
    super(h);
    classifier = c;
    cohere = cc;
    alpha = a;
  }


  /** Performs the cluster merging inference. */
  protected void infer() throws Exception
  {

    instantiateVariables();
    Normalizer norm = getNormalizer(cohere);

  }


  /**
    * Creates the first order variables involved in this inference problem.
   **/
  private void instantiateVariables()
  {
    Normalizer norm = getNormalizer(classifier);

    for (int i = 0; i < headDocument.sentences(); ++i)
      for (int j = 0; j < headDocument.mentionsInSentence(i); ++j)
      {
        for (int k = 0; k < i; ++k)
          for (int l = 0; l < headDocument.mentionsInSentence(k); ++l)
          {
            FirstOrderVariable v =
              new FirstOrderVariable(
                  classifier,
                  headDocument.getMentionPair(headDocument.getMention(k, l),
                                              headDocument.getMention(i, j)));
            variables.put(v, v);
          }

        for (int l = 0; l < j; ++l)
        {
          FirstOrderVariable v =
            new FirstOrderVariable(
                classifier,
                headDocument.getMentionPair(headDocument.getMention(i, l),
                                            headDocument.getMention(i, j)));
          variables.put(v, v);
        }
      }
  }


  /**
    * Retrieves the value of the specified variable as identified by the
    * classifier and the object that produce that variable.
    *
    * @param c  The classifier producing the variable.
    * @param o  The object from which the variable is produced.
    * @return   The current value of the requested variable.
   **/
  public String valueOf(Learner c, Object o) throws Exception
  {
    infer();
    return getVariable(new FirstOrderVariable(c, o)).getValue();
  }
}

