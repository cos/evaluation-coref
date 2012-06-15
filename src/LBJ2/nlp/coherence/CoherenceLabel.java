// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D2BC1BE0040341500DF59736B902676364EB04C05571D4AA597E5721FFE44CA72933BB3D23407D26CF98C84831E6A62E60640B8ECC40FAAD863DE8023C847817CA9AEC9E2A7B52E0F9C05E85CA09E323EFA2A47547F36FB3B168E5000000

package LBJ2.nlp.coherence;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.learn.*;
import LBJ2.nlp.coref.*;
import LBJ2.parse.*;
import java.util.*;


public class CoherenceLabel extends Classifier
{
  private static ThreadLocal cache = new ThreadLocal(){ };
  private static ThreadLocal exampleCache = new ThreadLocal(){ };

  public CoherenceLabel() { super("LBJ2.nlp.coherence.CoherenceLabel"); }

  public String getInputType() { return "[LLBJ2.nlp.coref.Document;"; }
  public String getOutputType() { return "discrete"; }

  public String[] allowableValues()
  {
    return DiscreteFeature.BooleanValues;
  }

  private String _discreteValue(Document[] pair)
  {
    return "" + (pair[0].getMoreCoherent());
  }

  public String discreteValue(Object example)
  {
    if (!(example instanceof Document[]))
    {
      String type = example == null ? "null" : example.getClass().getName();
      System.err.println("Classifier 'CoherenceLabel(Document[])' defined on line 9 of cohere.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    String result = _discreteValue((Document[]) example);

    if (valueIndexOf(result) == -1)
    {
      System.err.println("Classifier 'CoherenceLabel' defined on line 8 of cohere.lbj produced '" + result + "' as a feature value, which is not allowable.");
      System.exit(1);
    }

    return result;
  }

  public FeatureVector classify(Object example)
  {
    if (example == exampleCache.get()) return (FeatureVector) cache.get();
    String value = discreteValue(example);
    cache.set(new FeatureVector(new DiscreteFeature(containingPackage, name, value, valueIndexOf(value), (short) 2)));
    exampleCache.set(example);
    return (FeatureVector) cache.get();
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Document[]))
      {
        System.err.println("Classifier 'CoherenceLabel(Document[])' defined on line 9 of cohere.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "CoherenceLabel".hashCode(); }
  public boolean equals(Object o) { return o instanceof CoherenceLabel; }
}

