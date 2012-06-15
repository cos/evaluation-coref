package LBJ2.learn;

import java.util.*;
import java.io.*;
import LBJ2.classify.*;


/**
  * A <code>MuxLearner</code> uses one of many <code>Learner</code>s indexed
  * by the first feature in an example to produce a classification.  During
  * training, the features produced by the first child classifier of this
  * classifier's composite generator feature extractor are taken to determine
  * which <code>Learner</code>s will learn from the training object.  There
  * must be one <code>Feature</code> produced by the labeler for each
  * <code>Feature</code> produced by the first child classifier.  If this
  * classifier's feature extractor is not a composite generator, the first
  * feature it produces is the only one taken.
  *
  * <p> It is assumed that the <code>Learner</code> being multiplexed expects
  * a single label feature on each training example, and that the feature(s)
  * used to do the multiplexing are <code>DiscreteFeature</code>(s).
  * Furthermore, if this classifier's feature extractor is a composite
  * generator, it must produce the same number of features as this
  * classifier's labeler, and they must correspond to each other in the order
  * produced.
  *
  * <p> This algorithm's user-configurable parameters are stored in member
  * fields of this class.  They may be set via either a constructor that names
  * each parameter explicitly or a constructor that takes an instance of
  * {@link LBJ2.learn.MuxLearner.Parameters Parameters} as input.  The
  * documentation in each member field in this class indicates the default
  * value of the associated parameter when using the former type of
  * constructor.  The documentation of the associated member field in the
  * {@link LBJ2.learn.MuxLearner.Parameters Parameters} class indicates the
  * default value of the parameter when using the latter type of constructor.
  *
  * @author Nick Rizzolo
 **/
public class MuxLearner extends Learner
{
  /** Default for {@link #baseLearner}. */
  public static final Learner defaultBaseLearner = new SparsePerceptron();
  /** Default for {@link #defaultPrediction}. */
  public static final String defaultDefaultPrediction = null;


  /**
    * Instances of this learning algorithm will be multiplexed; default
    * {@link #defaultBaseLearner}.
   **/
  protected Learner baseLearner;
  /** A map from feature values to learners. */
  protected HashMap<String, Learner> network;
  /**
    * This string is returned during testing when the multiplexed
    * <code>Learner</code> doesn't exist; default
    * {@link #defaultDefaultPrediction}.
   **/
  protected String defaultPrediction;
  /**
    * If this classifier's feature extractor is a composite generator, this
    * member variable will reference the first child classifier of that
    * composite generator; otherwise, it is <code>null</code>.
   **/
  protected Classifier select;
  /**
    * If this classifier's feature extractor is a composite generator, this
    * list will contain every child classifier of that composite generator
    * except the first; otherwise, it is <code>null</code>.
   **/
  protected LinkedList compositeChildren;


  /**
    * Instantiates this multiplexed learner with {@link #defaultBaseLearner}.
   **/
  public MuxLearner() { this(""); }

  /**
    * Instantiates this multiplexed learner using the specified base learning
    * algorithm.  This constructor will normally only be called by the
    * compiler.
    *
    * @param base Instances of this learner will be multiplexed.
   **/
  public MuxLearner(Learner base) { this("", base); }

  /**
    * Instantiates this multiplexed learner using the specified base learning
    * algorithm.
    *
    * @param base Instances of this learner will be multiplexed.
    * @param d    This prediction will be returned during testing when the
    *             multiplexed <code>Learner</code> does not exist.
   **/
  public MuxLearner(Learner base, String d) { this("", base, d); }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link MuxLearner.Parameters} object.
    *
    * @param p  The settings of all parameters.
   **/
  public MuxLearner(Parameters p) { this("", p); }

  /**
    * Instantiates this multiplexed learner with the default learning
    * algorithm: <code>SparsePerceptron</code>.
    *
    * @param n  The name of the classifier.
    * @see      SparsePerceptron
   **/
  public MuxLearner(String n)
  {
    this(n, (Learner) defaultBaseLearner.clone());
  }

  /**
    * Instantiates this multiplexed learner using the specified base learning
    * algorithm.
    *
    * @param n    The name of the classifier.
    * @param base Instances of this learner will be multiplexed.
   **/
  public MuxLearner(String n, Learner base)
  {
    this(n, base, defaultDefaultPrediction);
  }

  /**
    * Instantiates this multiplexed learner using the specified base learning
    * algorithm.
    *
    * @param n    The name of the classifier.
    * @param base Instances of this learner will be multiplexed.
    * @param d    This prediction will be returned during testing when the
    *             multiplexed <code>Learner</code> does not exist.
   **/
  public MuxLearner(String n, Learner base, String d)
  {
    super(n);
    setBase(base);
    defaultPrediction = d;
    network = new HashMap<String, Learner>();
  }

  /**
    * Initializing constructor.  Sets all member variables to their associated
    * settings in the {@link MuxLearner.Parameters} object.
    *
    * @param n  The name of the classifier.
    * @param p  The settings of all parameters.
   **/
  public MuxLearner(String n, Parameters p)
  {
    super(n);
    baseLearner = p.baseLearner;
    defaultPrediction = p.defaultPrediction;
    network = new HashMap<String, Learner>();
  }


  /**
    * Sets {@link #baseLearner}.  This method will <i>not</i> have any effect
    * on the learners that already exist in the network.  However, new
    * learners created after this method is executed will be of the same type
    * as the object specified.
    *
    * @param base The new base learning algorithm.
   **/
  public void setBase(Learner base)
  {
    baseLearner = base;
    baseLearner.containingPackage = containingPackage;
    baseLearner.name = name;
    baseLearner.setLabeler(new LabelVectorReturner());
    baseLearner.setExtractor(new FeatureVectorReturner());
  }


  /**
    * Sets the extractor.
    *
    * @param e  A feature extracting classifier.
   **/
  public void setExtractor(Classifier e)
  {
    super.setExtractor(e);

    try
    {
      compositeChildren = extractor.getCompositeChildren();
      select = (Classifier) compositeChildren.removeFirst();
    }
    catch (UnsupportedOperationException ex)
    {
      compositeChildren = null;
      select = null;
    }
  }


  /**
    * The training example is multiplexed to the appropriate
    * <code>Learner</code>(s).
    *
    * @param example  The example object.
   **/
  public void learn(Object example)
  {
    FeatureVector labels = labeler.classify(example);
    FeatureVector selections = null;
    FeatureVector vector = null;

    if (select != null)
    {
      selections = select.classify(example);
      vector = new FeatureVector();
      for (Iterator I = compositeChildren.iterator(); I.hasNext(); )
        vector.addFeatures(((Classifier) I.next()).classify(example));

      assert selections.size() == labels.size()
           : "MuxLearner ERROR: Learner selections and labels have differing "
             + "sizes: " + labels + ", " + selections;
    }
    else
    {
      vector = (FeatureVector) extractor.classify(example).clone();
      selections = new FeatureVector((Feature) vector.features.removeFirst());
      for (int i = 1; i < labels.size(); ++i)
        selections.addFeature((Feature) vector.features.removeFirst());
    }

    Iterator L = labels.iterator();
    for (Iterator I = selections.iterator(); I.hasNext(); )
    {
      vector.addLabel((Feature) L.next());

      DiscreteFeature df = (DiscreteFeature) I.next();
      Learner l = network.get(df.getValue());

      if (l == null)
      {
        l = (Learner) baseLearner.clone();
        network.put(df.getValue(), l);
      }

      l.learn(vector);

      vector = (FeatureVector) vector.clone();
      vector.labels.clear();
    }
  }


  /** Clears the network. */
  public void forget() { network.clear(); }


  /**
    * Produces a set of scores indicating the degree to which each possible
    * discrete classification value is associated with the given example
    * object.  These scores are just the scores produced by the multiplexed
    * <code>Learner</code>'s <code>scores(Object)</code> method.
    *
    * @see   Learner#scores(Object)
    * @param example  The example object.
   **/
  public ScoreSet scores(Object example)
  {
    String selection = null;
    FeatureVector vector = null;

    if (select != null)
    {
      selection =
        ((DiscreteFeature) select.classify(example).firstFeature())
        .getValue();
      vector = new FeatureVector();
      for (Iterator I = compositeChildren.iterator(); I.hasNext(); )
        vector.addFeatures(((Classifier) I.next()).classify(example));
    }
    else
    {
      vector = (FeatureVector) extractor.classify(example).clone();
      selection =
        ((DiscreteFeature) vector.features.removeFirst()).getValue();
    }

    Learner l = network.get(selection);

    if (l == null)
      return
        new ScoreSet(new String[]{ defaultPrediction }, new double[]{ 1 });

    return l.scores(vector);
  }


  /**
    * This method performs the multiplexing and returns the output of the
    * selected <code>Learner</code>.
    *
    * @param example  The example object.
    * @return         The output of the selected <code>Learner</code>.
   **/
  public FeatureVector classify(Object example)
  {
    String selection = null;
    FeatureVector vector = null;

    if (select != null)
    {
      selection =
        ((DiscreteFeature) select.classify(example).firstFeature())
        .getValue();
      vector = new FeatureVector();
      for (Iterator I = compositeChildren.iterator(); I.hasNext(); )
        vector.addFeatures(((Classifier) I.next()).classify(example));
    }
    else
    {
      vector = (FeatureVector) extractor.classify(example).clone();
      selection =
        ((DiscreteFeature) vector.features.removeFirst()).getValue();
    }

    Learner l = network.get(selection);

    if (l == null)
      return
        new FeatureVector(
          new DiscreteFeature(containingPackage, name, defaultPrediction,
                              valueIndexOf(defaultPrediction),
                              (short) allowableValues().length));

    return l.classify(vector);
  }


  /**
    * Writes the algorithm's internal representation as text.
    *
    * @param out  The output stream.
   **/
  public void write(PrintStream out)
  {
    Map.Entry<String, Learner>[] entries =
      network.entrySet().toArray(new Map.Entry[0]);
    Arrays.sort(entries,
                new Comparator<Map.Entry<String, Learner>>()
                {
                  public int compare(Map.Entry<String, Learner> e1, Map.Entry<String, Learner> e2)
                  {
                    return e1.getKey().compareTo(e2.getKey());
                  }
                });

    for (int i = 0; i < entries.length; ++i)
    {
      out.println("select: " + entries[i].getKey());
      ((Learner) entries[i].getValue()).write(out);
    }

    out.println("End of MuxLearner");
  }


  /** Returns a deep clone of this learning algorithm. */
  public Object clone()
  {
    MuxLearner clone = null;
    try { clone = (MuxLearner) super.clone(); }
    catch (Exception e)
    {
      System.err.println("Error cloning MuxLearner: " + e);
      e.printStackTrace();
      System.exit(1);
    }

    clone.baseLearner = (Learner) baseLearner.clone();
    clone.network = new HashMap();
    for (Map.Entry<String, Learner> e : network.entrySet())
    {
      clone.network.put(e.getKey(), (Learner) e.getValue().clone());
    }

    return clone;
  }


  /**
    * Simply a container for all of {@link MuxLearner}'s configurable
    * parameters.  Using instances of this class should make code more
    * readable and constructors less complicated.
    *
    * @author Nick Rizzolo
   **/
  public static class Parameters extends LBJ2.learn.Parameters
  {
    /**
      * Instances of this learning algorithm will be multiplexed; default
      * {@link MuxLearner#defaultBaseLearner}.
     **/
    public Learner baseLearner;
    /**
      * This string is returned during testing when the multiplexed
      * <code>Learner</code> doesn't exist; default
      * {@link MuxLearner#defaultDefaultPrediction}.
     **/
    public String defaultPrediction;


    /** Sets all the default values. */
    public Parameters()
    {
      baseLearner = (Learner) defaultBaseLearner.clone();
      defaultPrediction = defaultDefaultPrediction;
    }
  }
}

