package LBJ2.nlp.coref;

import java.util.*;
import java.io.*;
import LBJ2.classify.*;
import LBJ2.parse.*;

/**
 * This class implements a dummy inference variable returning mention pair
 * scores that have been precomputed by an external coreference classifier.
 **/
public class DataCoref extends LBJ2.learn.Learner {
	/**
	 * A score set with scores of 0 for variables representing identity
	 * coreference.
	 **/
	private static final ScoreSet zeroScore = new ScoreSet(new String[] {
			"false", "true" }, new double[] { 0, 0 });
	/** The name of the file from which this learner reads its data. */
	private static final String scoresDirectory = 
		"data" + File.separator+ "data" + File.separator+ "ace_tides_multling_train" + File.separator + "NoConstraints";
		//"data/data/ace_tides_multling_train/NoConstraints";
	// "/home/binhle2/workspaces/coref/coref/data/ace_tides_multling_train/NoConstraints";
	/**
	 * The threshold adjustment, as if supplied to
	 * <code>LinearThresholdUnit.setThreshold(double)</code>.
	 **/
	private static double thresholdAdjustment = -5.5;
	/** The name of the currently loaded scores file. */
	private static String currentScoresFile = new String();
	/**
	 * A cache of all <code>ScoreSet</code>s this learner is capable of
	 * returning.
	 **/
	private static HashMap<String, ScoreSet> scores = new HashMap<String, ScoreSet>();

	/**
	 * Determines if <code>DataCoref</code> currently has the predictions for
	 * the specified document cached.
	 * 
	 * @param name
	 *            The name of the document.
	 * @return <code>true</code> iff the predictions for the specified document
	 *         are currently cached.
	 **/
	public static boolean isCached(String name) {
		return (name + ".scores").equals(currentScoresFile);
	}

	/**
	 * Reads this classifier's internal representation into memory.
	 * 
	 * @param file
	 *            The name of the file to read it from.
	 * @return A map storing the scores for every possible example object.
	 **/
	private static void readData(String file) {
		if (file.equals(currentScoresFile))
			return;
		currentScoresFile = file;
		final String[] dirs = { "dev", "test", "train" };

		for (int i = 0; i < 3; ++i) {
			file = scoresDirectory + File.separator + dirs[i] + File.separator + currentScoresFile;
			if (new File(file).exists())
				break;
		}

		scores = new HashMap<String, ScoreSet>();
		Parser parser = new LineByLine(file) {
			public Object next() {
				return readLine();
			}
		};

		for (String line = (String) parser.next(); line != null; line = (String) parser
				.next()) {
			if (line.matches("^\\s*$"))
				continue;
			String[] fields = line.split("\\s+");
			double score = Double.parseDouble(fields[1]);
			String[] ids = fields[0].split("\\|");
			scores.put(fields[0], new ScoreSet(
					new String[] { "false", "true" }, new double[] { -score,
							score }));
			scores.put(ids[0] + "|" + ids[2] + "|" + ids[1], new ScoreSet(
					new String[] { "false", "true" }, new double[] { -score,
							score }));
		}
	}

	/** Constructor. */
	public DataCoref() {
		super("DataCoref");
	}

	/**
	 * Sets a new threshold adjustment.
	 * 
	 * @param t
	 *            This value will be added to every positive prediction score,
	 *            and subtracted from every negative prediction score.
	 **/
	public void setThreshold(double t) {
		thresholdAdjustment = t;
	}

	/** Returns the input type of this classifier. */
	public String getInputType() {
		return "[LLBJ2.nlp.coref.Document$Mention;";
	}

	/** Returns the feature output type of this classifier. */
	public String getOutputType() {
		return "discrete";
	}

	/**
	 * Returns the values that a feature returned by this classifier is allowed
	 * to take. This classifier is Boolean, and its features are either
	 * <code>"false"</code> or <code>"true"</code>.
	 **/
	public String[] allowableValues() {
		return DiscreteFeature.BooleanValues;
	}

	/** Does nothing. */
	public void learn(Object example) {
	}

	/** Does nothing. */
	public void forget() {
	}

	/** Does nothing. */
	public void write(PrintStream out) {
	}

	/**
	 * Looks up the scores for the given example in {@link #scores}.
	 * 
	 * @param example
	 *            The example for which scores are desired.
	 * @return The score set for the given example.
	 **/
	public ScoreSet scores(Object example) {
		Document.Mention[] pair = (Document.Mention[]) example;
		String docName = pair[0].getDocument().getName();
		readData(docName + ".scores");

		if (pair[0].getMentionID().equals(pair[1].getMentionID()))
			return zeroScore;
		String key = pair[0].getDocument().getName() + "|"
				+ pair[0].getMentionID() + "|" + pair[1].getMentionID();

		if (!scores.containsKey(key)) {
			System.err.println("ERROR: No entry for mention pair '" + key + "'");
			System.exit(1);
		}

		ScoreSet result = (ScoreSet) scores.get(key).clone();
		result.getScore("true").score -= thresholdAdjustment;
		result.getScore("false").score += thresholdAdjustment;
		return result;
	}

	/**
	 * Returns the value with the highest score as computed by
	 * {@link #scores(Object)}.
	 * 
	 * @param example
	 *            The example to classify.
	 * @return The classification for the given example.
	 **/
	public String discreteValue(Object example) {
		return scores(example).highScoreValue();
	}

	/**
	 * Looks up the score set for the given example and then returns a feature
	 * whose value had the highest score in the score set wrapped in a feature
	 * vector.
	 * 
	 * @param example
	 *            The example for which a classification is desired.
	 * @return A vector containing a single feature whose value is set as
	 *         described above.
	 **/
	public FeatureVector classify(Object example) {
		return new FeatureVector(new DiscreteFeature(containingPackage, name,
				discreteValue(example)));
	}

	/** Returns a hash code for this object. */
	public int hashCode() {
		return name.hashCode();
	}

	/**
	 * Two <code>DataCoref</code> classifiers are equivalent when their run-time
	 * types are the same. Implementing this method is necessary to make
	 * inference over this classifier work.
	 * 
	 * @param o
	 *            The object with which to compare this object.
	 * @return <code>true</code> iff the argument object is an instance of class
	 *         <code>TrainingDataCoref</code>.
	 **/
	public boolean equals(Object o) {
		return o instanceof DataCoref;
	}
}
