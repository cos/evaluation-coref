// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000DA19D4B63C03C068FFA867818C788948DD6F17AE678036DBC6CE25A70314D4D0DA35C650656CEFBF427390D232C06C0F546BED752D327427BBB685C6D9F0F61D7B1DDE31E367D4FB7A0CBA53C1C9F860EEB08F487177086ABE307A21E1272655EAD6B4C9DCB45BE227A1F6CCDD99ABA15D5DCBA73D540CA992A3698A617C0D9CAC811F8FA4C86CAF12F8268ADE6D5A52D70BE5CC96A3BF4AA58CE09A083D391197814BE30343A00885706A6940F2B3476B777BBE90D84BA036C383412A49873083059D6AB0C2829E99E883550DB813A3056BC95391D622AF12FB8E7F509824228B411CDAE8D5849555CCDEBAAFFFD7901F5D8F91FB02F4B9AFB06FC0471720693FD081F73EA8AA200000

package LBJ2.nlp.coherence;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.jni.*;
import LBJ2.learn.*;
import LBJ2.nlp.coref.*;
import LBJ2.parse.*;
import java.util.*;


public class ChainTrigrams extends Classifier
{
  private static ThreadLocal cache = new ThreadLocal(){ };
  private static ThreadLocal exampleCache = new ThreadLocal(){ };

  public ChainTrigrams() { super("LBJ2.nlp.coherence.ChainTrigrams"); }

  public String getInputType() { return "[LLBJ2.nlp.coref.Document;"; }
  public String getOutputType() { return "real%"; }

  public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof Document[]))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'ChainTrigrams(Document[])' defined on line 548 of cohere.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == ChainTrigrams.exampleCache.get()) return (FeatureVector) ChainTrigrams.cache.get();

    Document[] pair = (Document[]) __example;
    FeatureVector __result = new FeatureVector();
    String __id;

    Map counts0 = pair[0].getChainNgramCounts(3);
    Map counts1 = pair[1].getChainNgramCounts(3);
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

    ChainTrigrams.exampleCache.set(__example);
    ChainTrigrams.cache.set(__result);

    return __result;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Document[]))
      {
        System.err.println("Classifier 'ChainTrigrams(Document[])' defined on line 548 of cohere.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "ChainTrigrams".hashCode(); }
  public boolean equals(Object o) { return o instanceof ChainTrigrams; }
}

