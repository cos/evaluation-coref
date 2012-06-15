package LBJ2.learn;

import LBJ2.classify.*;


/**
  * The softmax normalization function replaces each score with the fraction
  * of its exponential out of the sum of all scores' exponentials.  In other
  * words, each score <code>x<sub>i</sub></code> is replaced by
  * <code>exp(x<sub>i</sub>) / sum<sub>j</sub> exp(x<sub>j</sub>)</code>.
  *
  * @author Nick Rizzolo
 **/
public class Softmax extends Normalizer
{
  /**
    * Normalizes the given <code>ScoreSet</code>; its scores are modified in
    * place before it is returned.
    *
    * @param scores The set of scores to normalize.
    * @return       The normalized set of scores.
   **/
  public ScoreSet normalize(ScoreSet scores)
  {
    Score[] array = scores.toArray();
    double sum = 0;
    for (int i = 0; i < array.length; ++i)
    {
      array[i].score = Math.exp(array[i].score);
      sum += array[i].score;
    }

    for (int i = 0; i < array.length; ++i) array[i].score /= sum;
    return scores;
  }
}

