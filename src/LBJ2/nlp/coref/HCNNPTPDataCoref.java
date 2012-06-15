package LBJ2.nlp.coref;

import java.util.*;
import java.io.*;
import LBJ2.classify.*;
import LBJ2.parse.*;


/**
  * A hard-coded, best-link decoding of {@link DataCoref}.
 **/
public class HCNNPTPDataCoref extends Classifier
{
  /** A coref classifier that needs to be decoded. */
  private static final DataCoref dataCoref = new DataCoref();
  /**
    * The name of the cluster that each mention in the currently cached
    * document belongs to.
   **/
  private static ThreadLocal<String[]> clusterNames = new ThreadLocal<String[]>();


  /** Constructor. */
  public HCNNPTPDataCoref() { super("HCNNPTPDataCoref"); }


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
    * Returns the post-decoding prediction.
    *
    * @param example  The example to classify.
    * @return The classification for the given example.
   **/
  public String discreteValue(Object example)
  {
    Document.Mention[] pair = (Document.Mention[]) example;
    Document d = pair[0].getDocument();

    if (!DataCoref.isCached(d.getName()))
    {
      clusterNames.set(new String[d.totalMentions()]);
      int nextEntityID = 0;

      for (int i = 0; i < d.sentences(); ++i)
        for (int j = 0; j < d.mentionsInSentence(i); ++j)
        {
          Document.Mention m = d.getMention(i, j);
          double highest = 0; // Not -Double.MAX_VALUE
          Document.Mention best = null;

          for (int k = 0; k <= i; ++k)
            for (int l = 0;
                 k < i && l < d.mentionsInSentence(k) || k == i && l < j;
                 ++l)
            {
              Document.Mention p = d.getMention(k, l);
              double current =
                dataCoref.scores(d.getMentionPair(p, m)).get("true");

              if ((m.getType().equals("PRO") || !p.getType().equals("PRO"))
                  && current > highest)
              {
                highest = current;
                best = p;
              }
            }

          if (best == null)
            clusterNames.get()[m.getIndexInDocument()] = "e" + nextEntityID++;
          else
            clusterNames.get()[m.getIndexInDocument()] =
              clusterNames.get()[best.getIndexInDocument()];
        }
    }

    return "" + clusterNames.get()[pair[0].getIndexInDocument()]
                .equals(clusterNames.get()[pair[1].getIndexInDocument()]);
  }


  /**
    * Returns a feature representing the prediction for the given example
    * wrapped in a feature vector.
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


  /**
    * Two <code>HCNNPTPDataCoref</code> classifiers are equivalent when their
    * run-time types are the same.  Implementing this method is necessary to
    * make inference over this classifier work.
    *
    * @param o  The object with which to compare this object.
    * @return <code>true</code> iff the argument object is an instance of
    *         class <code>TrainingDataCoref</code>.
   **/
  public boolean equals(Object o) { return o instanceof HCNNPTPDataCoref; }
}

