package LBJ2.learn;

import LBJ2.classify.Classifier;
import LBJ2.learn.Learner;
import LBJ2.parse.Parser;


/**
  * <code>TestingMetric</code> is an interface through which the user may
  * implement their own testing method for use by LBJ's internal
  * cross validation algorithm.
  *
  * @author Dan Muriello
 **/
public interface TestingMetric
{
  /**
    * <code>test</code> is the function which LBJ's cross validation method
    * will call in order to test an example.
    *
    * @param classifier The classifier whose accuracy is being measured.
    * @param oracle     A classifier that returns the label of each example.
    * @param parser     A parser to supply the example objects.
   **/
  double test(Classifier classifier, Classifier oracle, Parser parser); 
}

