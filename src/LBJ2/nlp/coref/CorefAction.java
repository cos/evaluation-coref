package LBJ2.nlp.coref;

import java.io.PrintStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

import LBJ2.classify.Classifier;
import LBJ2.classify.TestDiscrete;
import LBJ2.learn.Learner;
import LBJ2.nlp.coref.ClusterMerger.Filter;

public class CorefAction {
	private static final long serialVersionUID = -1072536969137444539L;


	
	static Classifier corefClassifier;
	static Learner coherenceClassifier;
	static Filter filter;
	private Document initialDoc; // pre-computed to get around blocking IO
	private ClusterMergerParser parser;
	static Classifier coherenceLabeler;

	static int verbosity;
	static double base;
	static boolean DEBUG;
	static PrintStream out;
	static PrintStream performanceOutput;
	private double totalP = 0;
	private double totalR = 0;
	private int totalMentions = 0;
	private LinkedList<double[]> results = new LinkedList<double[]>();
	static TestDiscrete tester;

	private int indexOfDocument;

	private StatisticsUtility statistics;

	public LinkedList<double[]> getResults() {
		return results;
	}

	public int getTotalMentions() {
		return totalMentions;
	}

	public double getTotalP() {
		return totalP;
	}

	public double getTotalR() {
		return totalR;
	}

	public CorefAction(ClusterMergerParser parser, Document initialDoc, int i) {

		this.parser = parser;
		this.initialDoc = initialDoc;

		this.indexOfDocument = i;
	}

	protected void compute() {
		int counter = 0;
		Document currentPredicted = null;
		double currentF1 = 0;
		double bestScore = -Double.MAX_VALUE, bestP = 0, bestR = 0;

		long startDoc = System.nanoTime();

		boolean firstIteration = true;
		boolean secondParsing = true;
		double firstIterationTime = 0; // to record the IO time

		int iterationCount = 0;
		for (Document d = initialDoc; d != null;) {

			// The branch is taken on every brand new document, not on another
			// partitioning of the same document
			if (currentPredicted == null
					|| currentPredicted.getLabeled() != d.getLabeled()) {

				if (currentPredicted != null) {
					totalP += bestP;
					totalR += bestR;
					totalMentions += currentPredicted.totalMentions();
					bestScore = -Double.MAX_VALUE;
					bestP = bestR = 0;
				}

//				System.out.println("Processing document " + indexOfDocument
//						+ ": " + d.getName() + " on thread " + Thread.currentThread());

				currentPredicted = new Document(d.getLabeled());
				currentPredicted.fillInPredictions(corefClassifier, 0);
				currentF1 = new CoreferenceTester(verbosity).getF1(
						currentPredicted, d.getLabeled());
			}

			Document[] pair = null;
			boolean predictedFirst = new Random(d.getName().hashCode())
					.nextBoolean();
			if (predictedFirst)
				pair = new Document[] { currentPredicted, d };
			else
				pair = new Document[] { d, currentPredicted };

			double score = coherenceClassifier.scores(pair).get(
					"" + !predictedFirst);
			if (base > 0)
				score = 1.0 / (1.0 + Math.pow(base, -score));

			double[] dB3 = new CoreferenceTester(verbosity).test(d, d
					.getLabeled()); // an array of size 3
			double dF1 = 0;
			if (dB3[0] + dB3[1] != 0)
				dF1 = F1(dB3[0], dB3[1]) / d.totalMentions();

			double difference = dF1 - currentF1;
			if (Double.doubleToLongBits(score) != Double
					.doubleToLongBits(Double.NaN))
				results.add(new double[] { score, difference,
						parser.getLastDepth() });

			if (score > bestScore) {
				bestScore = score;
				bestP = dB3[0];
				bestR = dB3[1];
			}

			String label = coherenceLabeler.discreteValue(pair).equals(
					"" + !predictedFirst) ? "higher" : "lower";
			String prediction = score > 0 ? "higher" : "lower";
			tester.reportPrediction(prediction, label);

			if (DEBUG) {
				String number = "" + counter++;
				while (number.length() < 3)
					number = "0" + number;
				number += "." + parser.getLastDepth();
				d.writeHTML("debug/" + number);
				out.println("<li> <a href=\"" + number + "/" + d.getName()
						+ "/all.html\">" + number + "</a>");
			}
			if (firstIteration) {
				firstIteration = false;
				firstIterationTime = (System.nanoTime() - startDoc) / 1000000.00;
				statistics.setTimeForFirstIteration(indexOfDocument,
						firstIterationTime);
			}

			if (secondParsing) {
				secondParsing = false;
				long startSecondParsing = System.nanoTime();
				d = (Document) parser.next();
				statistics.setTimeForSecondParsing(indexOfDocument, (System
						.nanoTime() - startSecondParsing) / 1000000.00);
			} else
				d = (Document) parser.next();
			iterationCount++;

		} // end of the loop

		totalP += bestP;
		totalR += bestR;
		totalMentions += currentPredicted.totalMentions();

		long endDoc = System.nanoTime();
		double timeForThisDoc = (endDoc - startDoc) / 1000000.00;
		statistics.setTimeForDocument(indexOfDocument, timeForThisDoc);
		statistics.setIterationPerDocument(indexOfDocument, iterationCount);
	}

	private static double F1(double p, double r) {
		return 2 * p * r / (p + r);
	}
}
