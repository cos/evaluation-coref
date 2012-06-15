// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000DA15B4B44C0301EFB23B701627868DA74F171F186117F0A0E569E124D9DE60AB9694255719DFFEE42B965AA8E2884F2946EB7CC7F5D29A6E80E6CAE7EB0D5B55B17875D65DF68C8F5690D92D650C9560FE07BACDAFE557055BDB1FE6E07E178BC9792B62FF04D9527CC15E5FB0D228EC564C1E1B83DF24DC99A68E578754608E510645BCB4891F4BF6A11C69F98F43C3C65B3855B610B0F46597E351C4E4B1B46E8DDE329741257A9703F24EA59B50DB9F07906BCB49CBE08588C0650C1F9403CD42CA63E1AA05402616C35D4C555464853F9453D31A06B233C931324E99FE337A082BA693E863EEE86B83A248B8011B78C7A6230D814FD0E31FBD10715E806A028390B676C2BFB5F59FFF7F5C98763CFC830197C6AEF21BF780D9DE303F7F100A914D9C4F200000

package LBJ2.nlp.coherence;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.jni.*;
import LBJ2.learn.*;
import LBJ2.nlp.coref.*;
import LBJ2.parse.*;
import java.util.*;


public class GridBigrams extends Classifier
{
  private static ThreadLocal cache = new ThreadLocal(){ };
  private static ThreadLocal exampleCache = new ThreadLocal(){ };

  public GridBigrams() { super("LBJ2.nlp.coherence.GridBigrams"); }

  public String getInputType() { return "[LLBJ2.nlp.coref.Document;"; }
  public String getOutputType() { return "real%"; }

  public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof Document[]))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'GridBigrams(Document[])' defined on line 111 of cohere.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == GridBigrams.exampleCache.get()) return (FeatureVector) GridBigrams.cache.get();

    Document[] pair = (Document[]) __example;
    FeatureVector __result = new FeatureVector();
    String __id;

    HashMap counts0 = pair[0].getRepresentativeNgramCounts(2);
    HashMap counts1 = new HashMap();
    if (pair[1] != null)
    {
      counts1 = pair[1].getRepresentativeNgramCounts(2);
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

    GridBigrams.exampleCache.set(__example);
    GridBigrams.cache.set(__result);

    return __result;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Document[]))
      {
        System.err.println("Classifier 'GridBigrams(Document[])' defined on line 111 of cohere.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "GridBigrams".hashCode(); }
  public boolean equals(Object o) { return o instanceof GridBigrams; }
}

