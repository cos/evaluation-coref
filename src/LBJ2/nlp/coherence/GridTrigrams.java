// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000DA15D4B44C0301DFB23B701627868D2EDCF839A86117F0A2E569E124D9DE60AB9694255719DFFEE42B965AA8E2884F2946ED7CCB75B4AA9328B6BAF9F1DAEADAAD83CBC6BAE7346CF2B48E496B20EC238778B15E6D77AA38AADED8773783F83C5ECB49539FB7AEC2936E82FAF5861147E222E0F85C9E71A6EC4534FA3CB22304FA003AA5E52CC87AD73D806BCF4C7A1E16BAD1CAA5B085872BAC3F9A0627AD85237CE6F18C3A09A3DC3897127DACD28EDC78B40B5E5A4E550C24460B20E8F4281E62165B1F05582201B03E9A62EAA2232CA9F4AA9E9050B5991EC98112FCC7F993504955BC174B177B4B5C1512C54888D34E3539186C0AF60F98FDE08B827403501C9485B3369DFDAFACFFFBF2E4C3B1E76C188C3635F798DF348EC6F189FBF046CF19C65F200000

package LBJ2.nlp.coherence;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.jni.*;
import LBJ2.learn.*;
import LBJ2.nlp.coref.*;
import LBJ2.parse.*;
import java.util.*;


public class GridTrigrams extends Classifier
{
  private static ThreadLocal cache = new ThreadLocal(){ };
  private static ThreadLocal exampleCache = new ThreadLocal(){ };

  public GridTrigrams() { super("LBJ2.nlp.coherence.GridTrigrams"); }

  public String getInputType() { return "[LLBJ2.nlp.coref.Document;"; }
  public String getOutputType() { return "real%"; }

  public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof Document[]))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'GridTrigrams(Document[])' defined on line 330 of cohere.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == GridTrigrams.exampleCache.get()) return (FeatureVector) GridTrigrams.cache.get();

    Document[] pair = (Document[]) __example;
    FeatureVector __result = new FeatureVector();
    String __id;

    HashMap counts0 = pair[0].getRepresentativeNgramCounts(3);
    HashMap counts1 = new HashMap();
    if (pair[1] != null)
    {
      counts1 = pair[1].getRepresentativeNgramCounts(3);
    }
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

    GridTrigrams.exampleCache.set(__example);
    GridTrigrams.cache.set(__result);

    return __result;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Document[]))
      {
        System.err.println("Classifier 'GridTrigrams(Document[])' defined on line 330 of cohere.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "GridTrigrams".hashCode(); }
  public boolean equals(Object o) { return o instanceof GridTrigrams; }
}

