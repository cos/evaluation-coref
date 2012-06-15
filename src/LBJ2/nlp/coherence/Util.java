package LBJ2.nlp.coherence;

import java.util.*;


/**
  * Contains utility methods used by the classifiers in this package.
 **/
public class Util
{
  /**
    * Determines what ranges the two given real values fall in, considers both
    * sets of ranges to be feature vectors with appropriately named features,
    * then subtracts the second vector from the first.
   **/
  public static LinkedList rankingRangeFeatures(double range, Double real1,
                                                Double real2)
  {
    HashMap<String, Integer> features = new HashMap<String, Integer>();

    if (real1 != null)
    {
      increment(features, name1(range, real1), 1);
      increment(features, name2(range, real1), 1);
    }

    if (real2 != null)
    {
      increment(features, name1(range, real2), -1);
      increment(features, name2(range, real2), -1);
    }

    LinkedList result = new LinkedList();

    for (Map.Entry<String, Integer> entry : features.entrySet())
    {
      result.add(entry.getKey());
      result.add(entry.getValue());
    }

    return result;
  }


  private static String name1(double range, double real)
  {
    return "" + (Math.floor(real / range) * range);
  }


  private static String name2(double range, double real)
  {
    return "" + ((Math.floor(real / range + .5) - .5) * range);
  }


  private static void increment(HashMap<String, Integer> features,
                                String name, int delta)
  {
    Integer I = features.get(name);
    if (I == null) I = 0;
    features.put(name, I + delta);
  }
}

