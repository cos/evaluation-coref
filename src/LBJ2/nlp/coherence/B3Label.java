package LBJ2.nlp.coherence;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.jni.*;
import LBJ2.learn.*;
import LBJ2.nlp.coref.*;
import LBJ2.parse.*;
import java.util.*;


/**
  * This classifier picks the document in the pair with the higher
  * <i>B<sup>3</sup> F<sub>1</sub></i> score, returning <code>true</code> iff
  * it picked the first of the pair.  Of course, for this to work, both
  * <code>Document</code> objects must be labeled.
 **/
public class B3Label extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;
  private int verbosity;

  public B3Label() { this(0); }

  /**
    * Initializing constructor.
    *
    * @param v  Value for {@link #verbosity}.
   **/
  public B3Label(int v)
  {
    super("LBJ2.nlp.coherence.B3Label");
    verbosity = v;
  }

  public String getInputType() { return "[LLBJ2.nlp.coref.Document;"; }
  public String getOutputType() { return "discrete"; }

  public String[] allowableValues()
  {
    return DiscreteFeature.BooleanValues;
  }

  private String _discreteValue(Document[] pair)
  {
    int mentions = pair[0].totalMentions();
    double[] pr = new CoreferenceTester(verbosity - 1)
                  .test(pair[0], pair[0].getLabeled());

    if (verbosity > 0 && mentions > 0)
    {
      pr[0] /= mentions;
      pr[1] /= mentions;
    }

    double F1_1 = 0;
    if (pr[0] + pr[1] != 0) F1_1 = 2 * pr[0] * pr[1] / (pr[0] + pr[1]);
    pr = new CoreferenceTester(verbosity - 1)
         .test(pair[1], pair[1].getLabeled());

    if (verbosity > 0 && mentions > 0)
    {
      pr[0] /= mentions;
      pr[1] /= mentions;
    }

    double F1_2 = 0;
    if (pr[0] + pr[1] != 0) F1_2 = 2 * pr[0] * pr[1] / (pr[0] + pr[1]);
    String result = "" + (F1_1 > F1_2);

    if (verbosity > 0)
    {
      if (pair[1].getMoreCoherent())
      {
        double t = F1_1;
        F1_1 = F1_2;
        F1_2 = t;
      }

      System.out.println("  [" + F1_1 + ", " + F1_2 + "]");
    }

    return result;
  }

  public String discreteValue(Object example)
  {
    if (!(example instanceof Document[]))
    {
      String type = example == null ? "null" : example.getClass().getName();
      System.err.println("Classifier 'B3Label(Document[])' defined on line 177 of cohere.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    String result = _discreteValue((Document[]) example);

    if (valueIndexOf(result) == -1)
    {
      System.err.println("Classifier 'B3Label' defined on line 176 of cohere.lbj produced '" + result + "' as a feature value, which is not allowable.");
      System.exit(1);
    }

    return result;
  }

  public FeatureVector classify(Object example)
  {
    if (example == exampleCache) return cache;
    String value = discreteValue(example);
    cache = new FeatureVector(new DiscreteFeature(containingPackage, name, value, valueIndexOf(value), (short) 2));
    exampleCache = example;
    return cache;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Document[]))
      {
        System.err.println("Classifier 'B3Label(Document[])' defined on line 177 of cohere.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "B3Label".hashCode(); }
  public boolean equals(Object o) { return o instanceof B3Label; }
}

