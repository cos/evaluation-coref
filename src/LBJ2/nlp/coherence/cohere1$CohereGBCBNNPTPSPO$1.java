// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000B2A4D4CC155584ECFC84D2A4534517603DEEE4ECE4E7E71012101C10EFA268A1E29F9C5A9B9A97521D1BA050989954A9A063ABA0EE549992E4999E54989B5CA3A0EC9198999707E1004D407C0725000000

package LBJ2.nlp.coherence;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.learn.*;
import LBJ2.nlp.coref.*;
import LBJ2.parse.*;
import java.util.*;


public class cohere1$CohereGBCBNNPTPSPO$1 extends Classifier
{
  private static final GridBigrams __GridBigrams = new GridBigrams();
  private static final ChainBigrams __ChainBigrams = new ChainBigrams();

  private static ThreadLocal cache = new ThreadLocal(){ };
  private static ThreadLocal exampleCache = new ThreadLocal(){ };

  public cohere1$CohereGBCBNNPTPSPO$1() { super("LBJ2.nlp.coherence.cohere1$CohereGBCBNNPTPSPO$1"); }

  public String getInputType() { return "[LLBJ2.nlp.coref.Document;"; }
  public String getOutputType() { return "real%"; }

  public FeatureVector classify(Object example)
  {
    if (!(example instanceof Document[]))
    {
      String type = example == null ? "null" : example.getClass().getName();
      System.err.println("Classifier 'cohere1$CohereGBCBNNPTPSPO$1(Document[])' defined on line 13 of cohere1.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (example == exampleCache.get()) return (FeatureVector) cache.get();

    FeatureVector result = new FeatureVector();
    result.addFeatures(__GridBigrams.classify(example));
    result.addFeatures(__ChainBigrams.classify(example));

    exampleCache.set(example);
    cache.set(result);

    return result;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Document[]))
      {
        System.err.println("Classifier 'cohere1$CohereGBCBNNPTPSPO$1(Document[])' defined on line 13 of cohere1.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "cohere1$CohereGBCBNNPTPSPO$1".hashCode(); }
  public boolean equals(Object o) { return o instanceof cohere1$CohereGBCBNNPTPSPO$1; }

  public java.util.LinkedList getCompositeChildren()
  {
    java.util.LinkedList result = new java.util.LinkedList();
    result.add(__GridBigrams);
    result.add(__ChainBigrams);
    return result;
  }
}

