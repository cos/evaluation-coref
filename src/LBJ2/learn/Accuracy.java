package LBJ2.learn;

import java.util.*;
import java.io.*;
import LBJ2.classify.*;
import LBJ2.learn.Learner;
import LBJ2.parse.*;


/**
  * This is the cross validation testing metric which LBJ defaults to when
  * none is specified.  It simply counts the number of correct predictions and
  * returns that number divided by the total number of examples which it used
  * for testing.
  *
  * @author Dan Muriello
 **/
public class Accuracy implements TestingMetric
{
  /** Standard constructor, takes no arguments and does nothing. */
  public Accuracy() {}


  /**
    * The <code>test</code> method is what LBJ calls during its testing stage.
    *
    * @param classifier The classifier whose accuracy is being measured.
    * @param oracle     A classifier that returns the label of each example.
    * @param parser     A parser to supply the example objects.
   **/
  public double test(Classifier classifier, Classifier oracle, Parser parser)
  {
    TestDiscrete tester =
      TestDiscrete.testDiscrete(classifier, oracle, parser);
    return tester.getOverallStats()[0];
  }
}

