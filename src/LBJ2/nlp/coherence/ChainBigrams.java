// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000DA19D4B63C03C068FFA867818C7889467CD7C56DDE016CA791CE25A70314D4D0DA35C650656CEFBF427390D232C06C0F546BED752D327427BBB685C6D9F0F8EBD8E6F90F9AB6AFD350E5DA1E0EC74307F50C72CBAB3043D5F183590F0931BA27D6B52ECED5AA57193D8736EEEC4D5D8AEA6E5DB9E2206DC41D1B445B8368E4656C887C7326436DF0974134D67BE2D29E385F26E43D9D765D24678450C9E9C88C3C0A5F181A15004CA3035B42879D1A3BDBBBD5F4864A55813E1C1A015A4CB10C18AC63D5061494F2474C9A28E5C81D182B5ECA9C86311DF09F54FBF28441211C5A80E657CE24ACAA26E6F55DFFFEB488FA6CFC8F5097AD4DF50B760AB8310BC9F60788E8EE9A200000

package LBJ2.nlp.coherence;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.jni.*;
import LBJ2.learn.*;
import LBJ2.nlp.coref.*;
import LBJ2.parse.*;
import java.util.*;


public class ChainBigrams extends Classifier
{
  private static ThreadLocal cache = new ThreadLocal(){ };
  private static ThreadLocal exampleCache = new ThreadLocal(){ };

  public ChainBigrams() { super("LBJ2.nlp.coherence.ChainBigrams"); }

  public String getInputType() { return "[LLBJ2.nlp.coref.Document;"; }
  public String getOutputType() { return "real%"; }

  public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof Document[]))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'ChainBigrams(Document[])' defined on line 624 of cohere.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == ChainBigrams.exampleCache.get()) return (FeatureVector) ChainBigrams.cache.get();

    Document[] pair = (Document[]) __example;
    FeatureVector __result = new FeatureVector();
    String __id;

    Map counts0 = pair[0].getChainNgramCounts(2);
    Map counts1 = pair[1].getChainNgramCounts(2);
    for (Iterator I = counts0.entrySet().iterator(); I.hasNext(); )
    {
      Map.Entry entry = (Map.Entry) I.next();
      int c0 = ((Integer) entry.getValue()).intValue();
      int c1 = 0;
      if (counts1.containsKey(entry.getKey()))
      {
        c1 = ((Integer) counts1.get(entry.getKey())).intValue();
      }
      __id = this.name + (entry.getKey());
      __result.addFeature(new RealFeature(this.containingPackage, __id, c0 - c1));
    }
    for (Iterator I = counts1.entrySet().iterator(); I.hasNext(); )
    {
      Map.Entry entry = (Map.Entry) I.next();
      if (!counts0.containsKey(entry.getKey()))
      {
        int c1 = ((Integer) counts1.get(entry.getKey())).intValue();
        __id = this.name + (entry.getKey());
        __result.addFeature(new RealFeature(this.containingPackage, __id, -c1));
      }
    }

    ChainBigrams.exampleCache.set(__example);
    ChainBigrams.cache.set(__result);

    return __result;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Document[]))
      {
        System.err.println("Classifier 'ChainBigrams(Document[])' defined on line 624 of cohere.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "ChainBigrams".hashCode(); }
  public boolean equals(Object o) { return o instanceof ChainBigrams; }
}

