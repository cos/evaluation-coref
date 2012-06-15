package LBJ2.learn;

import java.util.*;
import java.io.*;
import LBJ2.classify.*;


/**
  * A <code>SparseNetworkLearner</code> uses multiple
  * {@link LinearThresholdUnit}s to make a multi-class classification.
  * Any {@link LinearThresholdUnit} may be used, so long as it implements its
  * <code>clone()</code> method and a public constructor that takes no
  * arguments.
  *
  * <p> It is assumed that a single discrete label feature will be produced in
  * association with each example object.  A feature taking one of the values
  * observed in that label feature will be produced by the learned classifier.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.SparseNetworkLearner.Parameters Parameters} as input.
  * The documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.SparseNetworkLearner.Parameters Parameters} class
  * indicates the default value of the parameter when using the latter type of
  * constructor.
  *
  * @author Nick Rizzolo
 **/
public class SparseNetworkLearner extends Learner
{
  /** Default for {@link #baseLTU}. */
  public static final LinearThresholdUnit defaultBaseLTU =
    new SparsePerceptron();


  /**
    * The underlying algorithm used to learn each class separately as a binary
    * classifier; default {@link #defaultBaseLTU}.
   **/
  protected LinearThresholdUnit baseLTU;
  /**
    * A map from labels to the linear threshold unit used to learn each label.
   **/
  protected HashMap network;


  /**
    * Instantiates this multi-class learner with the default learning
    * algorithm: {@link #defaultBaseLTU}.
   **/
  public SparseNetworkLearner() { this(""); }

  /**
    * Instantiates this multi-class learner using the specified algorithm to
    * learn each class separately as a binary classifier.  This constructor
    * will normally only be called by the compiler.
    *
    * @param ltu  The linear threshold unit used to learn binary classifiers.
   **/
  public SparseNetworkLearner(LinearThresholdUnit ltu) { this("", ltu); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseNetworkLearner.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public SparseNetworkLearner(Parameters p) { this("", p); }

  /**
    * Instantiates this multi-class learner with the default learning
    * algorithm: {@link #defaultBaseLTU}.
    *
    * @param n  The name of the classifier.
   **/
  public SparseNetworkLearner(String n)
  {
    super(n);
    baseLTU = (LinearThresholdUnit) defaultBaseLTU.clone();
    network = new HashMap();
  }

  /**
    * Instantiates this multi-class learner using the specified algorithm to
    * learn each class separately as a binary classifier.
    *
    * @param n    The name of the classifier.
    * @param ltu  The linear threshold unit used to learn binary classifiers.
   **/
  public SparseNetworkLearner(String n, LinearThresholdUnit ltu)
  {
    super(n);

    if (!ltu.getOutputType().equals("discrete"))
    {
      System.err.println(
          "LBJ WARNING: SparseNetworkLearner will only work with a "
          + "LinearThresholdUnit that returns discrete.");
      System.err.println(
          "             The given LTU, " + ltu.getClass().getName()
          + ", returns " + ltu.getOutputType() + ".");
    }

    setLTU(ltu);
    network = new HashMap();
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link SparseNetworkLearner.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public SparseNetworkLearner(String n, Parameters p)
  {
    super(n);

    if (!p.baseLTU.getOutputType().equals("discrete"))
    {
      System.err.println(
          "LBJ WARNING: SparseNetworkLearner will only work with a "
          + "LinearThresholdUnit that returns discrete.");
      System.err.println(
          "             The given LTU, " + p.baseLTU.getClass().getName()
          + ", returns " + p.baseLTU.getOutputType() + ".");
    }

    setLTU(p.baseLTU);
    network = new HashMap();
  }


  /**
    * Sets the <code>baseLTU</code> variable.  This method will <i>not</i>
    * have any effect on the LTUs that already exist in the network.  However,
    * new LTUs created after this method is executed will be of the same type
    * as the object specified.
    *
    * @param ltu  The new LTU.
   **/
  public void setLTU(LinearThresholdUnit ltu)
  {
    baseLTU = ltu;
    baseLTU.name = name + "$baseLTU";
  }


  /**
    * Sets the labeler.
    *
    * @param l  A labeling classifier.
   **/
  public void setLabeler(Classifier l)
  {
    if (getClass().getName().indexOf("SparseNetworkLearner") != -1
        && !l.getOutputType().equals("discrete"))
    {
      System.err.println(
          "LBJ WARNING: SparseNetworkLearner will only work with a "
          + "label classifier that returns discrete.");
      System.err.println(
          "             The given label classifier, " + l.getClass().getName()
          + ", returns " + l.getOutputType() + ".");
    }

    super.setLabeler(l);

    for (Iterator I = network.entrySet().iterator(); I.hasNext(); )
    {
      Map.Entry entry = (Map.Entry) I.next();
      String labelValue = (String) entry.getKey();
      ((ValueComparer) ((LinearThresholdUnit) entry.getValue()).getLabeler())
        .setLabeler(labeler);
    }
  }


  /**
    * Sets the extractor.
    *
    * @param e  A feature extracting classifier.
   **/
  public void setExtractor(Classifier e)
  {
    super.setExtractor(e);
    baseLTU.setExtractor(e);

    for (Iterator I = network.values().iterator(); I.hasNext(); )
      ((LinearThresholdUnit) I.next()).setExtractor(e);
  }


  /**
    * Each example is treated as a positive example for the linear threshold
    * unit associated with the label's value that is active for the example
    * and as a negative example for all other linear threshold units in the
    * network.
    *
    * @param example  The example object.
   **/
  public void learn(Object example)
  {
    String labelValue =
      ((DiscreteFeature) labeler.classify(example).firstFeature()).getValue();

    if (!network.containsKey(labelValue))
    {
      LinearThresholdUnit ltu = (LinearThresholdUnit) baseLTU.clone();
      ltu.setLabeler(new ValueComparer(labeler, labelValue));
      ltu.setExtractor(extractor);
      network.put(labelValue, ltu);
    }

    for (Iterator I = network.values().iterator(); I.hasNext(); )
      ((LinearThresholdUnit) I.next()).learn(example);
  }


  /** Simply calls <code>doneLearning()</code> on every LTU in the network. */
  public void doneLearning()
  {
    for (Iterator I = network.values().iterator(); I.hasNext(); )
      ((LinearThresholdUnit) I.next()).doneLearning();
  }


  /** Clears the network. */
  public void forget() { network.clear(); }


  /**
    * Produces a set of scores indicating the degree to which each possible
    * discrete classification value is associated with the given example
    * object.  These scores are just the scores of each LTU's positive
    * classification as produced by
    * <code>LinearThresholdUnit.scores(Object)</code>.
    *
    * @see   LinearThresholdUnit#scores(Object)
    * @param example  The example object.
   **/
  public ScoreSet scores(Object example)
  {
    ScoreSet result = new ScoreSet();

    for (Iterator I = network.entrySet().iterator(); I.hasNext(); )
    {
      Map.Entry e = (Map.Entry) I.next();
      LinearThresholdUnit ltu = (LinearThresholdUnit) e.getValue();
      result.put((String) e.getKey(),
                 ltu.score(example) - ltu.getThreshold());
    }

    return result;
  }


  /**
    * This implementation uses a winner-take-all comparison of the outputs
    * from the individual linear threshold units' score methods.
    *
    * @param example  The example object.
    * @return         A single feature with the winning linear threshold
    *                 unit's associated value.
   **/
  public FeatureVector classify(Object example)
  {
    double bestScore = Double.NEGATIVE_INFINITY;
    String bestValue = null;

    for (Iterator I = network.entrySet().iterator(); I.hasNext(); )
    {
      Map.Entry e = (Map.Entry) I.next();
      double score = ((LinearThresholdUnit) e.getValue()).score(example);
      if (score > bestScore)
      {
        bestValue = (String) e.getKey();
        bestScore = score;
      }
    }

    if (bestValue == null) return new FeatureVector();
    return
      new FeatureVector(
        new DiscreteFeature(containingPackage, name, bestValue,
                            valueIndexOf(bestValue),
                            (short) allowableValues().length));
  }


  /**
    * Using this method, the winner-take-all competition is narrowed to
    * involve only those labels contained in the specified list.  The list
    * must contain only <code>String</code>s.
    *
    * @param example    The example object.
    * @param candidates A list of the only labels the example may take.
    * @return           The label chosen by this classifier or
    *                   <code>null</code> if the network did not contain any
    *                   of the specified labels.
   **/
  public String valueOf(Object example, Collection candidates)
  {
    double bestScore = Double.NEGATIVE_INFINITY;
    String bestValue = null;

    Iterator I =
      candidates.size() == 0 ? network.keySet().iterator()
                             : candidates.iterator();

    while (I.hasNext())
    {
      String label = (String) I.next();
      double score = Double.NEGATIVE_INFINITY;
      if (network.containsKey(label))
        score = ((LinearThresholdUnit) network.get(label)).score(example);

      if (score > bestScore)
      {
        bestValue = label;
        bestScore = score;
      }
    }

    return bestValue;
  }


  /**
    * Returns scores for only those labels in the given collection.  If the
    * given collection is empty, scores for all labels will be returned.  If
    * there is no {@link LinearThresholdUnit} associated with a given label
    * from the collection, that label's score in the returned {@link ScoreSet}
    * will be set to <code>Double.NEGATIVE_INFINITY</code>.
    *
    * @param example    The example object.
    * @param candidates A list of the only labels the example may take.
    * @return           Scores for only those labels in
    *                   <code>candidates</code>.
   **/
  public ScoreSet scores(Object example, Collection candidates)
  {
    ScoreSet result = new ScoreSet();
    Iterator I =
      candidates.size() == 0 ? network.keySet().iterator()
                             : candidates.iterator();

    while (I.hasNext())
    {
      String label = (String) I.next();
      double score = Double.NEGATIVE_INFINITY;

      if (network.containsKey(label))
      {
        LinearThresholdUnit ltu = (LinearThresholdUnit) network.get(label);
        score = ltu.score(example) - ltu.getThreshold();
      }

      result.put(label, score);
    }

    return result;
  }


  /**
    * Writes the algorithm's internal representation as text.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out)
  {
    out.println(baseLTU.getClass().getName());
    baseLTU.write(out);

    Map.Entry[] entries =
      (Map.Entry[]) network.entrySet().toArray(new Map.Entry[0]);
    Arrays.sort(entries,
                new Comparator()
                {
                  public int compare(Object o1, Object o2)
                  {
                    Map.Entry e1 = (Map.Entry) o1;
                    Map.Entry e2 = (Map.Entry) o2;
                    return ((String) e1.getKey()).compareTo((String) e2.getKey());
                  }
                });

    for (int i = 0; i < entries.length; ++i)
    {
      out.println("label: " + entries[i].getKey());
      ((LinearThresholdUnit) entries[i].getValue()).write(out);
    }

    out.println("End of SparseNetworkLearner");
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone()
  {
    SparseNetworkLearner clone = null;
    try { clone = (SparseNetworkLearner) super.clone(); }
    catch (Exception e)
    {
      System.err.println("Error cloning SparseNetworkLearner: " + e);
      e.printStackTrace();
      System.exit(1);
    }

    clone.baseLTU = (LinearThresholdUnit) baseLTU.clone();
    clone.network = new HashMap();
    for (Iterator I = network.entrySet().iterator(); I.hasNext(); )
    {
      Map.Entry e = (Map.Entry) I.next();
      clone.network.put(e.getKey(),
                        ((LinearThresholdUnit) e.getValue()).clone());
    }

    return clone;
  }


  /**
    * Simply a container for all of {@link SparseNetworkLearner}'s
    * configurable parameters.  Using instances of this class should make code
    * more readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends LBJ2.learn.Parameters
  {
    /**
      * The underlying algorithm used to learn each class separately as a
      * binary classifier; default
      * {@link SparseNetworkLearner#defaultBaseLTU}.
     **/
    public LinearThresholdUnit baseLTU;


    /** Sets all the default values. */
    public Parameters()
    {
      baseLTU = (LinearThresholdUnit) defaultBaseLTU.clone();
    }
  }
}

