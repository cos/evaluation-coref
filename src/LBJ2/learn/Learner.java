package LBJ2.learn;

import java.io.*;
import LBJ2.classify.*;


/**
  * Extend this class to create a new {@link Classifier} that learns to mimic
  * one an oracle classifier given a feature extracting classifier and example
  * objects.
  *
  * @author Nick Rizzolo
 **/
public abstract class Learner extends Classifier
{
  /** Stores the classifier used to produce labels. */
  protected Classifier labeler;
  /** Stores the classifiers used to produce features. */
  protected Classifier extractor;


  /**
    * This constructor is used by the LBJ2 compiler; it should never be called
    * by a programmer.
   **/
  protected Learner() { }

  /**
    * Initializes the name.
    *
    * @param n  The name of the classifier.
   **/
  protected Learner(String n) { super(n); }

  /**
    * Constructor for unsupervised learning.
    *
    * @param n  The name of the classifier.
    * @param e  The feature extracting classifier.
   **/
  protected Learner(String n, Classifier e) { this(n, null, e); }

  /**
    * Constructor for supervised learning.
    *
    * @param n  The name of the classifier.
    * @param l  The labeling classifier.
    * @param e  The feature extracting classifier.
   **/
  protected Learner(String n, Classifier l, Classifier e)
  {
    super(n);
    setLabeler(l);
    setExtractor(e);
  }


  /** Returns the labeler. */
  public Classifier getLabeler() { return labeler; }


  /**
    * Sets the labeler.
    *
    * @param l  A labeling classifier.
   **/
  public void setLabeler(Classifier l) { labeler = l; }


  /** Returns the extractor. */
  public Classifier getExtractor() { return extractor; }


  /**
    * Sets the extractor.
    *
    * @param e  A feature extracting classifier.
   **/
  public void setExtractor(Classifier e) { extractor = e; }


  /**
    * Trains the learning algorithm given an object as an example.
    *
    * @param example  An example of the desired learned classifier's behavior.
   **/
  abstract public void learn(Object example);


  /**
    * Trains the learning algorithm given many objects as examples.  This
    * implementation simply calls {@link #learn(Object)} on each of the
    * objects in the input array.  It should be overridden if there is a more
    * efficient implementation.
    *
    * @param examples Examples of the desired learned classifier's behavior.
   **/
  public void learn(Object[] examples)
  {
    for (int i = 0; i < examples.length; ++i) learn(examples[i]);
  }


  /**
    * Overridden by subclasses to perform any required post-processing
    * computations after all training examples have been observed through
    * {@link #learn(Object)} and {@link #learn(Object[])}.  By default this
    * method does nothing.
   **/
  public void doneLearning() { }


  /**
    * Overridden by subclasses to reinitialize the learner to the state it
    * started at before any learning was performed.
   **/
  abstract public void forget();


  /**
    * Overridden by subclasses to store a binary representation of this
    * learner in a pre-defined location.  By default this method does nothing.
   **/
  public void save() { }


  /**
    * Produces a set of scores indicating the degree to which each possible
    * discrete classification value is associated with the given example
    * object.  Learners that return a <code>real</code> feature or more than
    * one feature may implement this method by simply returning
    * <code>null</code>.
    *
    * @param example  The object to make decisions about.
    * @return         A set of scores indicating the degree to which each
    *                 possible discrete classification value is associated
    *                 with the given example object.
   **/
  abstract public ScoreSet scores(Object example);


  /**
    * Writes the algorithm's internal representation as text.
    *
    * @param out  The output stream.
   **/
  abstract public void write(PrintStream out);
}

