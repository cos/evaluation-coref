// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000DA15B3B63C0301EFB27D1A07A1C2CEE8D7C2D704C48669047909145171745219384768682DFFED39B2B94A52D2492E5E42D7FC37023B9378977FCF2469F6D531CC6FEADEC374CBF6D67B52FCB852CEC8B0A0EAB087389898BE743B30BD38A48B9EF1715E25734C9566670A271AEAEB07AA1835DF4C955B100B66A06856AA5839D15B44A0BF9313A2DE2FB3A01A6DB631764FAC9E0A429A83AE784858E912A083E59218BFC857E91C6A22885B76A684AA6FC849EE9CC6A3245265E78933325D8256E5106EE5AD6BE9D83F17A4B7C1512D149A418A7AC189C04310D738F19FDB3442F190E8140799267122B9017A6F55DFFFEB4A1F9D0F33E79AC3E6AEF25BF449E2E3B0BC7F10ACD2323BCB200000

package LBJ2.nlp.coherence;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.jni.*;
import LBJ2.learn.*;
import LBJ2.nlp.coref.*;
import LBJ2.parse.*;
import java.util.*;


public class SubjectBigramCounts extends Classifier
{
  private static ThreadLocal cache = new ThreadLocal(){ };
  private static ThreadLocal exampleCache = new ThreadLocal(){ };

  public SubjectBigramCounts() { super("LBJ2.nlp.coherence.SubjectBigramCounts"); }

  public String getInputType() { return "[LLBJ2.nlp.coref.Document;"; }
  public String getOutputType() { return "real%"; }

  public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof Document[]))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'SubjectBigramCounts(Document[])' defined on line 1919 of cohere.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == SubjectBigramCounts.exampleCache.get()) return (FeatureVector) SubjectBigramCounts.cache.get();

    Document[] pair = (Document[]) __example;
    FeatureVector __result = new FeatureVector();
    String __id;

    HashMap counts0 = pair[0].getSubjectNgramCounts(2);
    HashMap counts1 = pair[1].getSubjectNgramCounts(2);
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

    SubjectBigramCounts.exampleCache.set(__example);
    SubjectBigramCounts.cache.set(__result);

    return __result;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Document[]))
      {
        System.err.println("Classifier 'SubjectBigramCounts(Document[])' defined on line 1919 of cohere.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "SubjectBigramCounts".hashCode(); }
  public boolean equals(Object o) { return o instanceof SubjectBigramCounts; }
}

