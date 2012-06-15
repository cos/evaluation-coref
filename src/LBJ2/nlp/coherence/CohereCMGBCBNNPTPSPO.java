// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// 

package LBJ2.nlp.coherence;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.learn.*;
import LBJ2.nlp.coref.*;
import LBJ2.parse.*;
import java.io.File;
import java.util.*;


/************************************
 * Training on beam search examples *
 ************************************/
public class CohereCMGBCBNNPTPSPO extends SparseAveragedPerceptron
{
  public static boolean isTraining = true;
  private static java.net.URL lcFilePath = CohereCMGBCBNNPTPSPO.class.getResource("CohereCMGBCBNNPTPSPO.lc");
  private static CohereCMGBCBNNPTPSPO instance = new CohereCMGBCBNNPTPSPO(false);
  public static CohereCMGBCBNNPTPSPO getInstance()
  {
    return instance;
  }

  public void save()
  {
    System.err.println("CohereCMGBCBNNPTPSPO.save() was called while its source code is at an intermediate state.  Re-run the LBJ compiler.");
    System.exit(1);
  }

  public static Parser getParser() { return new ClusterMergerCoherenceParser(new ClusterMergerParser("data" + File.separatorChar + "HLT2007.train", new HCNNPTPDataCoref(), 100, 4, new CohereGBCBNNPTPSPO(), ClusterMerger.getUnattachedFilter("clusterD", -5)), new HCNNPTPDataCoref()); }

  public static TestingMetric getTestingMetric() { return null; }

  private static ThreadLocal cache = new ThreadLocal(){ };
  private static ThreadLocal exampleCache = new ThreadLocal(){ };

  private boolean isClone;

  public CohereCMGBCBNNPTPSPO()
  {
    super("LBJ2.nlp.coherence.CohereCMGBCBNNPTPSPO");
    isClone = true;
  }

  private CohereCMGBCBNNPTPSPO(boolean b)
  {
    super(.1, 0, 6);
    containingPackage = "LBJ2.nlp.coherence";
    name = "CohereCMGBCBNNPTPSPO";
    setLabeler(new B3Label());
    setExtractor(new cohere1$CohereCMGBCBNNPTPSPO$1());
    isClone = false;
  }

  public String getInputType() { return "[LLBJ2.nlp.coref.Document;"; }
  public String getOutputType() { return "discrete"; }

  public String[] allowableValues()
  {
    return DiscreteFeature.BooleanValues;
  }

  public void learn(Object example)
  {
    if (isClone)
    {
      instance.learn(example);
      return;
    }

    Classifier saveExtractor = extractor;
    Classifier saveLabeler = labeler;

    if (!(example instanceof Document[]))
    {
      if (example instanceof FeatureVector)
      {
        if (!(extractor instanceof FeatureVectorReturner))
          setExtractor(new FeatureVectorReturner());
        if (!(labeler instanceof LabelVectorReturner))
          setLabeler(new LabelVectorReturner());
      }
      else
      {
        String type = example == null ? "null" : example.getClass().getName();
        System.err.println("Classifier 'CohereCMGBCBNNPTPSPO(Document[])' defined on line 13 of cohere1.lbj received '" + type + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }
    }

    super.learn(example);

    if (saveExtractor != extractor) setExtractor(saveExtractor);
    if (saveLabeler != labeler) setLabeler(saveLabeler);
  }

  public void learn(Object[] examples)
  {
    if (isClone)
    {
      instance.learn(examples);
      return;
    }

    Classifier saveExtractor = extractor;
    Classifier saveLabeler = labeler;

    if (!(examples instanceof Document[][]))
    {
      if (examples instanceof FeatureVector[])
      {
        if (!(extractor instanceof FeatureVectorReturner))
          setExtractor(new FeatureVectorReturner());
        if (!(labeler instanceof LabelVectorReturner))
          setLabeler(new LabelVectorReturner());
      }
      else
      {
        String type = examples == null ? "null" : examples.getClass().getName();
        System.err.println("Classifier 'CohereCMGBCBNNPTPSPO(Document[])' defined on line 13 of cohere1.lbj received '" + type + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }
    }

    super.learn(examples);

    if (saveExtractor != extractor) setExtractor(saveExtractor);
    if (saveLabeler != labeler) setLabeler(saveLabeler);
  }

  public FeatureVector classify(Object __example)
  {
    if (isClone) return instance.classify(__example);

    Classifier __saveExtractor = extractor;

    if (!(__example instanceof Document[]))
    {
      if (__example instanceof FeatureVector)
      {
        if (!(extractor instanceof FeatureVectorReturner))
          setExtractor(new FeatureVectorReturner());
      }
      else
      {
        String type = __example == null ? "null" : __example.getClass().getName();
        System.err.println("Classifier 'CohereCMGBCBNNPTPSPO(Document[])' defined on line 13 of cohere1.lbj received '" + type + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }
    }

__classify:
    {
      if (__example == CohereCMGBCBNNPTPSPO.exampleCache.get()) break __classify;
      CohereCMGBCBNNPTPSPO.exampleCache.set(__example);

      CohereCMGBCBNNPTPSPO.cache.set(super.classify(__example));
    }

    if (__saveExtractor != this.extractor) setExtractor(__saveExtractor);
    return (FeatureVector) CohereCMGBCBNNPTPSPO.cache.get();
  }

  public FeatureVector[] classify(Object[] examples)
  {
    if (isClone)
      return instance.classify(examples);

    Classifier saveExtractor = extractor;

    if (!(examples instanceof Document[][]))
    {
      if (examples instanceof FeatureVector[])
      {
        if (!(extractor instanceof FeatureVectorReturner))
          setExtractor(new FeatureVectorReturner());
      }
      else
      {
        String type = examples == null ? "null" : examples.getClass().getName();
        System.err.println("Classifier 'CohereCMGBCBNNPTPSPO(Document[])' defined on line 13 of cohere1.lbj received '" + type + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }
    }

    FeatureVector[] result = super.classify(examples);
    if (saveExtractor != extractor) setExtractor(saveExtractor);
    return result;
  }

  public String discreteValue(Object __example)
  {
    DiscreteFeature f = (DiscreteFeature) classify(__example).firstFeature();
    return f == null ? "" : f.getValue();
  }

  public int hashCode() { return "CohereCMGBCBNNPTPSPO".hashCode(); }
  public boolean equals(Object o) { return o instanceof CohereCMGBCBNNPTPSPO; }

  public void write(java.io.PrintStream a0)
  {
    if (isClone)
    {
      instance.write(a0);
      return;
    }

    super.write(a0);
  }

  public double score(java.lang.Object a0)
  {
    if (isClone)
      return instance.score(a0);
    return super.score(a0);
  }

  public void promote(java.lang.Object a0)
  {
    if (isClone)
    {
      instance.promote(a0);
      return;
    }

    super.promote(a0);
  }

  public void demote(java.lang.Object a0)
  {
    if (isClone)
    {
      instance.demote(a0);
      return;
    }

    super.demote(a0);
  }

  public void forget()
  {
    if (isClone)
    {
      instance.forget();
      return;
    }

    super.forget();
  }

  public double getLearningRate()
  {
    if (isClone)
      return instance.getLearningRate();
    return super.getLearningRate();
  }

  public void setLearningRate(double a0)
  {
    if (isClone)
    {
      instance.setLearningRate(a0);
      return;
    }

    super.setLearningRate(a0);
  }

  public void setThreshold(double a0)
  {
    if (isClone)
    {
      instance.setThreshold(a0);
      return;
    }

    super.setThreshold(a0);
  }

  public void setLabeler(LBJ2.classify.Classifier a0)
  {
    if (isClone)
    {
      instance.setLabeler(a0);
      return;
    }

    super.setLabeler(a0);
  }

  public double getInitialWeight()
  {
    if (isClone)
      return instance.getInitialWeight();
    return super.getInitialWeight();
  }

  public void setInitialWeight(double a0)
  {
    if (isClone)
    {
      instance.setInitialWeight(a0);
      return;
    }

    super.setInitialWeight(a0);
  }

  public double getThreshold()
  {
    if (isClone)
      return instance.getThreshold();
    return super.getThreshold();
  }

  public double getPositiveThickness()
  {
    if (isClone)
      return instance.getPositiveThickness();
    return super.getPositiveThickness();
  }

  public void setPositiveThickness(double a0)
  {
    if (isClone)
    {
      instance.setPositiveThickness(a0);
      return;
    }

    super.setPositiveThickness(a0);
  }

  public double getNegativeThickness()
  {
    if (isClone)
      return instance.getNegativeThickness();
    return super.getNegativeThickness();
  }

  public void setNegativeThickness(double a0)
  {
    if (isClone)
    {
      instance.setNegativeThickness(a0);
      return;
    }

    super.setNegativeThickness(a0);
  }

  public void setThickness(double a0)
  {
    if (isClone)
    {
      instance.setThickness(a0);
      return;
    }

    super.setThickness(a0);
  }

  public LBJ2.classify.ScoreSet scores(java.lang.Object a0)
  {
    if (isClone)
      return instance.scores(a0);
    return super.scores(a0);
  }

  public void setExtractor(LBJ2.classify.Classifier a0)
  {
    if (isClone)
    {
      instance.setExtractor(a0);
      return;
    }

    super.setExtractor(a0);
  }

  public LBJ2.classify.Classifier getLabeler()
  {
    if (isClone)
      return instance.getLabeler();
    return super.getLabeler();
  }

  public LBJ2.classify.Classifier getExtractor()
  {
    if (isClone)
      return instance.getExtractor();
    return super.getExtractor();
  }

  public void doneLearning()
  {
    if (isClone)
    {
      instance.doneLearning();
      return;
    }

    super.doneLearning();
  }
}

