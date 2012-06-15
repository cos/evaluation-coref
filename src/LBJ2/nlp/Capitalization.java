// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B88000000000000000D509DCA038030148F556A78A82EF05A7C64B79E344FC14758580152985B45C77F6C86B2DE56166776EB18D68D6D68C1D4DA45692383330DC74CD46FCE4A2E7947CD9E8FED99603AF190ACC13185B30B0CAB6451E491494BE63120D676013B79721064951EC882A5D278A0A705A402D4931F4BD271A5290AA0D4F47203FF15EA8C3056C2A734F0EE60BB3B2FC30B6CD14BBBEB421EFEE31B4588F5BE258B4ED296B40E435C5CF6BEFCB18A95813FB10FCEFDD64E2100000

package LBJ2.nlp;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.jni.*;
import LBJ2.learn.*;
import LBJ2.parse.*;


/**
  * This class implements a classifier that takes a {@link Word} as input and
  * generates Boolean features representing the capitalizations of the words
  * in a [-2, +2] window around the input word.  The generated features
  * consist of the capitalization (as read from {@link Word#capitalized}) of
  * the input word as well as the capitalizations of the two words before the
  * input word in the sentence and the capitalizations of the two words after
  * the input word in the sentence.  If any of those words do not exist, the
  * corresponding feature isn't generated.
  *
  * <p> This class's implementation was automatically generated by the LBJ
  * compiler.
  *
  * @author Nick Rizzolo
 **/
public class Capitalization extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public Capitalization() { super("LBJ2.nlp.Capitalization"); }

  public String getInputType() { return "LBJ2.nlp.Word"; }
  public String getOutputType() { return "discrete%"; }

  public String[] allowableValues()
  {
    return DiscreteFeature.BooleanValues;
  }

  public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof Word))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'Capitalization(Word)' defined on line 45 of CommonFeatures.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == Capitalization.exampleCache) return Capitalization.cache;

    Word word = (Word) __example;
    FeatureVector __result = new FeatureVector();
    String __id;
    String __value;

    int i;
    Word w = word, last = word;
    for (i = 0; i <= 2 && last != null; ++i)
    {
      last = (Word) last.next;
    }
    for (i = 0; i > -2 && w.previous != null; --i)
    {
      w = (Word) w.previous;
    }
    for (; w != last; w = (Word) w.next)
    {
      __id = this.name + (i++);
      __value = "" + (w.capitalized);
      __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value, valueIndexOf(__value), (short) 2));
    }

    Capitalization.exampleCache = __example;
    Capitalization.cache = __result;

    return __result;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Word))
      {
        System.err.println("Classifier 'Capitalization(Word)' defined on line 45 of CommonFeatures.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "Capitalization".hashCode(); }
  public boolean equals(Object o) { return o instanceof Capitalization; }
}

