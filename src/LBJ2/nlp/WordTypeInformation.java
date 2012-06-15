// Modifying this comment will cause the next execution of LBJ2 to overwrite this file.
// F1B880000000000000005715D4F4380401DFB23D6263092013E1DD2626AE5C4C872D87E516ADE095779CEE6DAAD0FFDD95825821DB09CCB73FE367829C5161D3E1732593C40CBDD363750F66C69F2FD53E3AED81BF12D391D150C0E0CF9816592C1184B70210D1C097B4520A4A3F7A1400B81222E1FA5001CA278B185C2AB5995E0A7FA49085E292667B39EAD498BD923D8F5E50433179B384B5793465B5CF423B773875A96BE5781C868DA3B3906E9511224C5EA680C4AF24FE6C8249A14A25B6953979A8E70B4E5F072A420CF04B52FE6C8C3B1DF48ED3ADE1DEF40C7AAAAB70450B2E4A075DC41AEDAFD54178B1451227215B8C16DB396561C68919B7DAB64BB69E03A39EB062FED7455CCE034D998AA54FF15C8AE2BA66369578FF5AB60C1A67803F26E0B4EFF17B39AD2A7EBC11FD57B3355F88A1A440FE6E71AA26A74119200000

package LBJ2.nlp;

import LBJ2.classify.*;
import LBJ2.infer.*;
import LBJ2.jni.*;
import LBJ2.learn.*;
import LBJ2.parse.*;


/**
  * This class implements a classifier that takes a {@link Word} as input and
  * generates Boolean features representing interesting information about the
  * forms of the words in a [-2, +2] window around the input word.  The
  * generated features include one that indicates if the entire input word
  * consists only of capital letters, one that indicates if the entire input
  * word consists only of digits, and one that indicates if the entire input
  * word consists only of non-letters.  The same features are also produced
  * for the two words before and after the input word.  If any of those words
  * do not exist, the corresponding features aren't generated.
  *
  * <p> This class's implementation was automatically generated by the LBJ
  * compiler.
  *
  * @author Nick Rizzolo
 **/
public class WordTypeInformation extends Classifier
{
  private static FeatureVector cache;
  private static Object exampleCache;

  public WordTypeInformation() { super("LBJ2.nlp.WordTypeInformation"); }

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
      System.err.println("Classifier 'WordTypeInformation(Word)' defined on line 71 of CommonFeatures.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    if (__example == WordTypeInformation.exampleCache) return WordTypeInformation.cache;

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
    for (; w != last; w = (Word) w.next, ++i)
    {
      boolean allCapitalized = true, allDigits = true, allNonLetters = true;
      for (int j = 0; j < w.form.length(); ++j)
      {
        allCapitalized &= Character.isUpperCase(w.form.charAt(j));
        allDigits &= Character.isDigit(w.form.charAt(j));
        allNonLetters &= !Character.isLetter(w.form.charAt(j));
      }
      __id = this.name + ("c" + i);
      __value = "" + (allCapitalized);
      __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value, valueIndexOf(__value), (short) 2));
      __id = this.name + ("d" + i);
      __value = "" + (allDigits);
      __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value, valueIndexOf(__value), (short) 2));
      __id = this.name + ("p" + i);
      __value = "" + (allNonLetters);
      __result.addFeature(new DiscreteFeature(this.containingPackage, __id, __value, valueIndexOf(__value), (short) 2));
    }

    WordTypeInformation.exampleCache = __example;
    WordTypeInformation.cache = __result;

    return __result;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i)
      if (!(examples[i] instanceof Word))
      {
        System.err.println("Classifier 'WordTypeInformation(Word)' defined on line 71 of CommonFeatures.lbj received '" + examples[i].getClass().getName() + "' as input.");
        new Exception().printStackTrace();
        System.exit(1);
      }

    return super.classify(examples);
  }

  public int hashCode() { return "WordTypeInformation".hashCode(); }
  public boolean equals(Object o) { return o instanceof WordTypeInformation; }
}

