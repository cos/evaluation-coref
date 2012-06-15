package LBJ2.nlp.coref;

import LBJ2.classify.*;
import LBJ2.parse.*;


/**
  * This class implements a classifier which takes the predictions of another
  * coreference classifier and then merges the singleton pronouns.
 **/
public class NNPTPMergedPronouns extends MergedPronouns
{
  /** Constructor. */
  public NNPTPMergedPronouns()
  {
    super("NNPTPMergedPronouns", new HCNNPTPDataCoref());
  }


  /**
    * Two {@link MergedPronouns} classifiers are equivalent when their
    * run-time types are the same.  Implementing this method is necessary to
    * make inference over this classifier work.
    *
    * @param o  The object with which to compare this object.
    * @return <code>true</code> iff the argument object is an instance of
    *         class <code>NNPTPMergedPronouns</code>.
   **/
  public boolean equals(Object o)
  {
    return o instanceof NNPTPMergedPronouns;
  }
}

