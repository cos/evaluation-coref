// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000B49CC2E4E2A4D294DAE4B4CC92E45D158292A2D4DA5507ECF2A4D43F94C4A4DC1D079CF4E2DCD4DCB21D3F5021999F971D1BA09BA9A063ABA05DA004D75A5497A09B1D601BA79E9A52EA049F29A4F471D0D4DB4D2C250A17A19B1D688623A96DA05B001001FF6C37000000

package LBJ2.nlp.coref;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.learn.*;
import LBJ2.parse.*;
import java.util.*;


public class CorefLabel extends Classifier
{
  private static ThreadLocal cache = new ThreadLocal(){ };
  private static ThreadLocal exampleCache = new ThreadLocal(){ };

  public CorefLabel() { super("LBJ2.nlp.coref.CorefLabel"); }

  public String getInputType() { return "[LLBJ2.nlp.coref.Document$Mention;"; }
  public String getOutputType() { return "discrete"; }

  public String[] allowableValues()
  {
    return DiscreteFeature.BooleanValues;
  }

  private String _discreteValue(Document.Mention[] m)
  {
    return "" + (m[0].getEntityID().equals(m[1].getEntityID()));
  }

  public String discreteValue(Object example)
  {
    if (!(example instanceof Document.Mention[]))
    {
      String type = example == null ? "null" : example.getClass().getName();
      System.err.println("Classifier 'CorefLabel(Document.Mention[])' defined on line 8 of coref.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    String result = _discreteValue((Document.Mention[]) example);

    if (valueIndexOf(result) == -1)
    {
      System.err.println("Classifier 'CorefLabel' defined on line 7 of coref.lbj produced '" + result + "' as a feature value, which is not allowable.");
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
      if (!(examples[i] instanceof Document.Mention[]))
      {
        System.err.println("Classifier 'CorefLabel(Document.Mention[])' defined on line 8 of coref.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "CorefLabel".hashCode(); }
  public boolean equals(Object o) { return o instanceof CorefLabel; }
}

