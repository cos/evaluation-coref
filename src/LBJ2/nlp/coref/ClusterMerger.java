package LBJ2.nlp.coref;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import extra166y.Ops.Generator;
import extra166y.Ops.IntToObject;
import extra166y.Ops.Procedure;
import extra166y.ParallelArray;

import LBJ2.classify.Classifier;
import LBJ2.classify.TestDiscrete;
import LBJ2.learn.Learner;
import LBJ2.nlp.coherence.B3Label;
import LBJ2.nlp.coref.Document.Mention;
import LBJ2.util.ClassUtils;

/**
 * Use this program to determine the extent of the correlation between the
 * performances of a coreference and a coherence classifier on perturbations of
 * a coreference labeling as produced by merging clusters.
 * 
 * <h4>Usage</h4> <blockquote>
 * 
 * <pre>
 *   java LBJ2.nlp.coref.ClusterMerger &lt;coreference classifier&gt; \
 *                                     &lt;coherence classifier&gt; \
 *                                     &lt;test data&gt; \
 *                                     &lt;base&gt; \
 *                                     [&lt;verbosity=0&gt;]
 * </pre>
 * 
 * </blockquote>
 * 
 * <h4>Input</h4>
 * <p>
 * <code>&lt;coreference classifier&gt;</code> is the fully qualified class name
 * of the classifier involved in the correlation, which will also produce the
 * initial clustering for the search procedure.
 * <code>&lt;coherence classifier&gt;</code> is the fully qualified class name
 * of the other classifier involved in the correlation. <code>&lt;test
 * data&gt;</code> is the name of a file containing test data. The higher the
 * integer supplied for <code>&lt;verbosity&gt;</code>, the more information
 * will be output to <code>STDOUT</code>.
 * 
 * <h4>Output</h4> GNU plot commands to create a correlation graph.
 **/
public class ClusterMerger implements Comparable<ClusterMerger> {
	/**
	 * Computes <i>F<sub>1</sub></i>.
	 * 
	 * @param p
	 *            The precision.
	 * @param r
	 *            The recall.
	 * @return The <i>F<sub>1</sub></i>.
	 **/
	private static double F1(double p, double r) {
		return 2 * p * r / (p + r);
	}

	private static final Learner coref = new DataCoref();

	/** The document whose clusters will be merged. */
	private Document predicted;
	/** The document whose clusters have been merged. */
	private Document mergedDocument;
	/** All predicted cluster names. */
	private String[] clusterNames;
	/** The sizes of the current clusters. */
	private int[] clusterSizes;
	/** Index of the first cluster in the next pair of clusters to be merged. */
	private int i;
	/**
	 * Index of the second cluster in the next pair of clusters to be merged.
	 **/
	private int j;
	/**
	 * Keeps track of how deep the bredth first search is. This value will be
	 * the same as the number of <code>0</code> entries in the
	 * {@link #clusterSizes} array.
	 **/
	private int depth;
	/**
	 * Remembers the <i>B<sup>3</sup></i> score of the document represented by
	 * this object.
	 **/
	private Double corefScore;
	/** Filters out certain cluster pairs from being considered for merging. */
	private Filter filter;

	/** Does nothing. */
	public ClusterMerger() {
	}

	/**
	 * Initializes the search procedure for a given document.
	 * 
	 * @param d
	 *            The document whose clusters will be merged.
	 * @param c
	 *            A coreference classifier that takes pairs of {@link Mention}s
	 *            as input.
	 **/
	public ClusterMerger(Document d, Classifier c) {
		this(d, c, null);
	}

	/**
	 * Initializes the search procedure for a given document with a specified
	 * filter.
	 * 
	 * @param d
	 *            The document whose clusters will be merged.
	 * @param c
	 *            A coreference classifier that takes pairs of {@link Mention}s
	 *            as input.
	 * @param f
	 *            A filter used during the search.
	 **/
	public ClusterMerger(Document d, Classifier c, Filter f) {
		if (f == null)
			filter = new Filter();
		else
			filter = f.clone(this);
		predicted = new Document(d);
		clusterNames = new String[predicted.fillInPredictions(c, 0)];
		for (int i = 0; i < clusterNames.length; ++i)
			clusterNames[i] = "e" + i;

		clusterSizes = new int[clusterNames.length];
		for (int i = 0; i < predicted.sentences(); ++i)
			for (int j = 0; j < predicted.mentionsInSentence(i); ++j) {
				Document.Mention m = predicted.getMention(i, j);
				int cluster = Integer.parseInt(m.getEntityID().substring(1));
				++clusterSizes[cluster];
			}

		depth = i = 0;
		j = 0;
		increment();
	}

	/** Returns the value of {@link #depth}. */
	public int getDepth() {
		return depth;
	}

	/**
	 * Return the next cluster merging or <code>null</code> if there are no
	 * more.
	 **/
	public ClusterMerger next() {
		if (clusterNames == null || j >= clusterNames.length)
			return null;
		ClusterMerger result = new ClusterMerger();
		result.predicted = predicted;
		result.clusterNames = clusterNames.clone();
		result.clusterNames[j] = result.clusterNames[i];
		result.clusterSizes = clusterSizes.clone();
		result.clusterSizes[i] += result.clusterSizes[j];
		result.clusterSizes[j] = 0;
		result.i = i;
		result.j = j;
		result.filter = filter.clone(result);
		result.increment();
		result.depth = depth + 1;
		increment();
		return result;
	}

	/**
	 * Sets the {@link #i} and {@link #j} variables to point to the next cluster
	 * pair to be merged together.
	 **/
	private void increment() {
		do {
			if (++j == clusterNames.length) {
				for (++i; i < clusterNames.length && clusterSizes[i] == 0; ++i)
					;
				j = i + 1;
			}
		} while (j < clusterNames.length && (clusterSizes[j] == 0 || filter.reject()));
	}

	/**
	 * Returns the document represented by this object in which some clusters
	 * have been merged.
	 **/
	public Document getDocument() {
		if (mergedDocument != null)
			return mergedDocument;
		mergedDocument = new Document(predicted.getLabeled());

		for (int i = 0; i < predicted.sentences(); ++i)
			for (int j = 0; j < predicted.mentionsInSentence(i); ++j) {
				Document.Mention mi = predicted.getMention(i, j);
				Document.Mention mj = mergedDocument.getMention(i, j);

				if (!mi.equals(mj)) {
					System.err.println("mi = {'" + mi.getHead() + "', '" + mi.getExtent() + "'}");
					System.err.println("mj = {'" + mj.getHead() + "', '" + mj.getExtent() + "'}");
					System.exit(1);
				}

				mj.setEntityID(clusterNames[Integer.parseInt(mi.getEntityID().substring(1))]);
			}

		return mergedDocument;
	}

	/**
	 * Retrieves the <i>B<sup>3</sup></i> <i>F<sub>1</sub></i> score of the
	 * document represented by this object.
	 **/
	public Double getCorefScore() {
		if (corefScore == null) {
			Document d = getDocument();
			corefScore = new CoreferenceTester(0).getF1(d, d.getLabeled());
		}

		return corefScore;
	}

	/**
	 * Will sort instances of this class in increasing order of
	 * <i>B<sup>3</sup></i> <i>F<sub>1</sub></i> score.
	 **/
	public int compareTo(ClusterMerger c) {
		return getCorefScore().compareTo(c.getCorefScore());
	}

	private static final boolean DEBUG = false;

	/** Produces the labels that the coherence classifier is judged against. */
	private static final Classifier coherenceLabeler = new B3Label();

	/**
	 * The percentage by which the x-axis range will be expanded in the output
	 * graph.
	 **/
	private static final double xrangePercentage = .04;
	/**
	 * The percentage by which the y-axis range will be expanded in the output
	 * graph.
	 **/
	private static final double yrangePercentage = .05;
	/** Horizontal positioning of labels on the graph. */
	private static final double labelXPercentage = .72;
	/** Vertical positioning of the overall correlation label on the graph. */
	private static final double overallLabelYPercentage = .918;
	/** Vertical positioning of the overall correlation label on the graph. */
	private static final double pointsLabelYPercentage = .878;

	private static CorefAction[] leafTasks;

	/**
	 * Computes the correlation coefficients for the given data.
	 * 
	 * @param pairs
	 *            The data arranged as an array of pairs, the first element of
	 *            each pair coming from one variable to be correlated, and the
	 *            second element of each pair coming from the other.
	 * @return The correlation coefficient <i>R</i> as well as the coefficients
	 *         <i>a</i> and <i>b</i> such that the line <i>ax + b</i> is the
	 *         best fit for the data, in the order mentioned, in an array.
	 **/
	private static double[] correlate(double[][] pairs) {
		return correlate(pairs, 0, pairs.length);
	}

	/**
	 * Computes the correlation coefficients for the given data.
	 * 
	 * @param pairs
	 *            The data arranged as an array of pairs, the first element of
	 *            each pair coming from one variable to be correlated, and the
	 *            second element of each pair coming from the other.
	 * @param start
	 *            The index of the first pair in the correlation.
	 * @param end
	 *            One more than the index of the last pair in the correlation.
	 * @return The correlation coefficient <i>R</i> as well as the coefficients
	 *         <i>a</i> and <i>b</i> such that the line <i>ax + b</i> is the
	 *         best fit for the data, in the order mentioned, in an array.
	 **/
	private static double[] correlate(double[][] pairs, int start, int end) {
		double N = end - start;
		if (N == 1)
			return new double[] { 1, 0, pairs[start][1] };
		double sumX = 0, sumY = 0, sumX2 = 0, sumY2 = 0, sumXY = 0;

		for (int i = start; i < end; ++i) {
			sumX += pairs[i][0];
			sumY += pairs[i][1];
			sumX2 += pairs[i][0] * pairs[i][0];
			sumY2 += pairs[i][1] * pairs[i][1];
			sumXY += pairs[i][0] * pairs[i][1];
		}

		double R = (sumXY - sumX * sumY / N) / Math.sqrt((sumX2 - sumX * sumX / N) * (sumY2 - sumY * sumY / N));
		double a = (sumXY - sumX * sumY / N) / (sumX2 - sumX * sumX / N);
		double b = (sumY - a * sumX) / N;
		return new double[] { R, a, b };
	}

	public static void main(String[] args) {
		// UPCRC2 repo now working
		long start = System.nanoTime();

		String corefClassifierName = null;
		String coherenceClassifierName = null;
		double base = 0;
		String testDataFile = null;
		int beamWidth1 = 0;
		int maxDepth1 = 0;
		String filterSpecifier = null;
		double filterArgument = 0;
		int verbosity1 = 0;

		int posOfAvailableProcessors = args.length - 1;
		int availableProcessors = Integer.parseInt(args[posOfAvailableProcessors]);
		// int availableProcessors = Runtime.getRuntime().availableProcessors();

		
		try {
			corefClassifierName = args[0];
			coherenceClassifierName = args[1];
			base = Double.parseDouble(args[2]);
			testDataFile = args[3];
			System.out.println("Test data file: " + testDataFile);
			beamWidth1 = Integer.parseInt(args[4]);
			maxDepth1 = Integer.parseInt(args[5]);
			filterSpecifier = args[6];
			filterArgument = Double.parseDouble(args[7]);
			// if (args.length == 9)
			verbosity1 = Integer.parseInt(args[8]);

			// if (args.length > 9)
			// throw new Exception();

		} catch (Exception e) {
			System.err.println("usage: java LBJ2.nlp.coref.ClusterMerger \\\n"
					+ "              <coreference classifier> <coherence classifier> <base>\\\n"
					+ "              <test data> <beam width> <max depth> <filter specifier> \\\n"
					+ "              <filter argument> [<verbosity=0>]");
			System.exit(1);
		}
		
		final int beamWidth = beamWidth1;
		final int maxDepth = maxDepth1;
		final int verbosity = verbosity1;

		final Classifier corefClassifier = ClassUtils.getClassifier(corefClassifierName);
		final Learner coherenceClassifier = ClassUtils.getLearner(coherenceClassifierName);
		final ClusterMerger.Filter filter = ClusterMerger.getUnattachedFilter(filterSpecifier, filterArgument);

		PrintStream out = null;

		final TestDiscrete tester = new TestDiscrete();

		if (DEBUG) {
			new File("debug").mkdirs();

			try {
				out = new PrintStream(new FileOutputStream("debug/index.html"));
			} catch (Exception e) {
				System.err.println("Can't open debug/index.html for output: " + e);
				System.exit(1);
			}

			out.println("<html>");
			out.println("<head>");
			out.println("<title>Document renditions</title>");
			out.println("</head>");
			out.println("<body><ul>");
		}

		String shortName = coherenceClassifierName.substring(coherenceClassifierName.lastIndexOf('.') + 7);
		String performanceFileName = "analysis/correlation/clusterMerge/" + shortName + "/" + shortName + "."
				+ beamWidth + "." + maxDepth + "." + filterArgument + ".performance";
		PrintStream performanceOutput = null;

		try {
			performanceOutput = new PrintStream(new FileOutputStream(performanceFileName));
			System.out.println("Performance file name: " + performanceFileName + "\n");
		} catch (Exception e) {
			System.err.println("Error opening " + performanceFileName + " for output: " + e);

			System.exit(1);
		}
		

		int numberOfParsers = countFilesToParse(testDataFile); // usually the
		// number of
		// files to
		// process
		final String[] testDataFiles = splitTestDataFile(testDataFile, numberOfParsers);

		long startFJ = System.nanoTime();

		ParallelArray<CorefAction> leafTasks = ParallelArray.createUsingHandoff(new CorefAction[numberOfParsers],
				ParallelArray.defaultExecutor());

		// TODO this assumes that there is one parser/document; however, one
		// parser might process more than one single document
		final StatisticsUtility statistics = new StatisticsUtility(testDataFiles.length);
		
		CorefAction.corefClassifier = corefClassifier;
		CorefAction.coherenceClassifier = coherenceClassifier;
		CorefAction.filter = filter;
		CorefAction.coherenceLabeler = coherenceLabeler;
		CorefAction.tester = tester;
		CorefAction.verbosity = verbosity;
		CorefAction.base = base;
		CorefAction.DEBUG = false;
		CorefAction.out = out;
		CorefAction.performanceOutput = performanceOutput;
		
		leafTasks.replaceWithMappedIndexSeq(new IntToObject<CorefAction>() {
			@Override
			public CorefAction op(int i) {
				ClusterMergerParser parser = new ClusterMergerParser(testDataFiles[i], corefClassifier, beamWidth,
						maxDepth, coherenceClassifier, filter);

				// the initial document is pre-computed for each parser to get
				// around the IO cost in FJ tasks
				long startFirstIO = System.nanoTime();
				Document initialDoc = (Document) parser.next();
				statistics.setTimeForFirstParsing(i, (System.nanoTime() - startFirstIO) / 1000000.0);

				return new CorefAction(parser, initialDoc, i);
			}
		});

		// CorefRootAction[] secondLevelTasks = new
		// CorefRootAction[availableProcessors];

		// distributeLeafTasks(secondLevelTasks, leafTasks,
		// availableProcessors);
		// CorefRootAction rootTask = new CorefRootAction(secondLevelTasks,
		// "1stRoot");

		// ForkJoinPool fjp = new ForkJoinPool(availableProcessors);
		// fjp.invoke(rootTask);
		// System.out.println("Steal count: " + fjp.getStealCount());

		leafTasks.apply(new Procedure<CorefAction>() {
			@Override
			public void op(CorefAction b) {
				b.compute();
			}
		});

		// Add up the totals from multiple tasks
		final double totalP[] = new double[1]; totalP[0] = 0;
		final double totalR[] = new double[1]; totalR[0] = 0;
		final int totalMentions[] = new int[1]; totalMentions[0] = 0;
		final LinkedList<double[]> results = new LinkedList<double[]>();

//		final CorefAction[] leafTasksA = leafTasks.getArray();
		
		leafTasks.applySeq(new Procedure<CorefAction>() {
			@Override
			public void op(CorefAction b) {
			totalP[0] += b.getTotalP();
			totalR[0] += b.getTotalR();
			totalMentions[0] += b.getTotalMentions();
			results.addAll(b.getResults());
		}});

		double F1 = F1(totalP[0], totalR[0]) / totalMentions[0];
		if (DEBUG) {
			out.println("</ul></body></html>");
			out.close();
		}
		double forkJoinTime = (System.nanoTime() - startFJ) / 1000000.00;

		long startPlotting = System.nanoTime();
		// plotGraph(performanceOutput, tester, F1, results,
		// coherenceClassifierName, corefClassifierName, base);
		double plotTime = (System.nanoTime() - startPlotting) / 1000000.00;
		double totalTime = (System.nanoTime() - start) / 1000000.00;
		System.out.printf("Total execution time: %.2f ms \n", totalTime);
		System.out.printf("\t Fork-join time: %.2f ms \n", forkJoinTime);
		System.out.printf("\t Generate plot date time: %.2f ms \n", plotTime);

		 statistics.printStatistics();
		cleanUp(testDataFiles);
	}

	private static int countFilesToParse(String fileName) {
		int count = 0;
		try {
			File file = new File(fileName);
			if (file.exists()) {
				FileReader fr = new FileReader(file);
				LineNumberReader ln = new LineNumberReader(fr);
				while (ln.readLine() != null) {
					count++;
				}
				ln.close();
			} else {
				throw new IOException("File " + "\"" + fileName + "\"" + " does not exists!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return count;
	}

	/*
	 * private static void distributeLeafTasks(CorefRootAction[]
	 * secondLevelTasks, CorefAction[] leafTasks, int availableProcessors) { int
	 * chunkSize = leafTasks.length / availableProcessors;
	 * 
	 * for (int i = 0; i < availableProcessors - 1; ++i) { CorefAction[] chunk =
	 * new CorefAction[chunkSize]; System.arraycopy(leafTasks, i * chunkSize,
	 * chunk, 0, chunkSize); secondLevelTasks[i] = new CorefRootAction(chunk,
	 * "2ndLevelRoot" + i); }
	 * 
	 * int remainder = leafTasks.length - (availableProcessors - 1) chunkSize;
	 * CorefAction[] lastChunk = new CorefAction[remainder];
	 * System.arraycopy(leafTasks, (availableProcessors - 1) * chunkSize,
	 * lastChunk, 0, remainder); secondLevelTasks[availableProcessors - 1] = new
	 * CorefRootAction( lastChunk, "2ndLevelRoot" + (availableProcessors - 1));
	 * 
	 * }
	 */

	/** Delete temporary files generated */
	private static void cleanUp(String fileNames[]) {
		for (String fileName : fileNames) {
			File f = new File(fileName);

			if (!f.exists())
				throw new IllegalArgumentException("Delete: no such file or directory: " + fileName);

			if (!f.canWrite())
				throw new IllegalArgumentException("Delete: write protected: " + fileName);

			if (f.isDirectory()) {
				String[] files = f.list();
				if (files.length > 0)
					throw new IllegalArgumentException("Delete: directory not empty: " + fileName);
			}

			boolean success = f.delete();

			if (!success)
				throw new IllegalArgumentException("Delete: deletion failed");
		}
	}

	private static void plotGraph(PrintStream performanceOutput, TestDiscrete tester, double F1,
			LinkedList<double[]> results, String coherenceClassifierName, String corefClassifierName, double base) {

		performanceOutput.println("B^3 of highest scorers: " + F1 + "\n");
		tester.printPerformance(System.err);
		performanceOutput.close();

		Collections.sort(results, new Comparator<double[]>() {
			public int compare(double[] d1, double[] d2) {
				return new Double(d1[2]).compareTo(d2[2]);
			}
		});

		double[][] arrayResults = results.toArray(new double[0][]);
		int[] levelEnds = new int[(int) arrayResults[arrayResults.length - 1][2] + 1];

		int counter = 0;

		for (int i = 1; i < arrayResults.length; ++i)
			if (arrayResults[i][2] > arrayResults[i - 1][2])
				levelEnds[counter++] = i;
		levelEnds[counter] = arrayResults.length;

		double[][] coefficients = new double[levelEnds.length + 1][];
		coefficients[0] = correlate(arrayResults, 0, levelEnds[0]);
		for (int i = 1; i < levelEnds.length; ++i)
			coefficients[i] = correlate(arrayResults, levelEnds[i - 1] + 1, levelEnds[i]);
		coefficients[levelEnds.length] = correlate(arrayResults, 0, arrayResults.length);

		double lowX = Double.MAX_VALUE, lowY = Double.MAX_VALUE;
		double highX = -Double.MAX_VALUE, highY = -Double.MAX_VALUE;

		for (double[] entry : arrayResults) {
			lowX = Math.min(lowX, entry[0]);
			highX = Math.max(highX, entry[0]);
			lowY = Math.min(lowY, entry[1]);
			highY = Math.max(highY, entry[1]);
		}

		if (highX - lowX == 0) {
			lowX -= .5;
			highX += .5;
		}

		if (highY - lowY == 0) {
			lowY -= .5;
			highY += .5;
		}

		double xRange = highX - lowX;
		double yRange = highY - lowY;
		lowX -= xrangePercentage * xRange;
		highX += xrangePercentage * xRange;
		lowY -= yrangePercentage * yRange;
		highY += yrangePercentage * yRange;
		xRange += 2 * xrangePercentage * xRange;
		yRange += 2 * yrangePercentage * yRange;

		double labelX = lowX + labelXPercentage * xRange;
		double overallLabelY = lowY + overallLabelYPercentage * yRange;
		double pointsLabelY = lowY + pointsLabelYPercentage * yRange;
		String coherenceScoreDescription = "unnormalized";
		if (base > 0)
			coherenceScoreDescription = "sigmoid_{" + base + "}";

		int dot = coherenceClassifierName.lastIndexOf('.');
		coherenceClassifierName = coherenceClassifierName.substring(dot + 1);
		dot = corefClassifierName.lastIndexOf('.');
		corefClassifierName = corefClassifierName.substring(dot + 1);

		System.out.println("set term postscript enhanced");
		System.out.println("set xtic auto");
		System.out.println("set ytic auto");
		System.out.println("set key top right");
		System.out.println("set title \"" + coherenceClassifierName + " Coherence vs. " + corefClassifierName
				+ " Coreference, Cluster Merging Search\"");
		System.out.println("set xlabel \"Coherence score (" + coherenceScoreDescription + ")\"");
		System.out.println("set ylabel \"Coreference B^3 score\"");

		counter = 0;

		for (int i = 0; i < levelEnds.length; ++i) {
			System.out.print("set label 1 \"R = ");
			if (Double.doubleToLongBits(coefficients[i][0]) == Double.doubleToLongBits(Double.NaN))
				System.out.print("NaN");
			else
				System.out.print(String.format("%.5f", coefficients[i][0]));
			System.out.println("\" at " + labelX + ", " + overallLabelY + " left front");
			int totalPoints = levelEnds[i];
			if (i > 0)
				totalPoints -= levelEnds[i - 1];
			System.out.println("set label 2 \"Total points: " + totalPoints + "\" at " + labelX + ", " + pointsLabelY
					+ " left front");
			System.out.println("plot [" + lowX + ":" + highX + "] [" + lowY + ":" + highY + "] " + coefficients[i][1]
					+ " * x + " + coefficients[i][2] + " title 'best fit', '-' using 1:2 title 'depth " + i + "'");

			for (; counter < arrayResults.length && arrayResults[counter][2] == i; ++counter)
				System.out.println(arrayResults[counter][0] + " " + arrayResults[counter][1]);
			System.out.println("e");
		}

		counter = levelEnds.length;
		System.out.print("set label 1 \"R = ");
		if (Double.doubleToLongBits(coefficients[counter][0]) == Double.doubleToLongBits(Double.NaN))
			System.out.print("NaN");
		else
			System.out.print(String.format("%.5f", coefficients[counter][0]));
		System.out.println("\" at " + labelX + ", " + overallLabelY + " left front");
		System.out.println("set label 2 \"Total points: " + arrayResults.length + "\" at " + labelX + ", "
				+ pointsLabelY + " left front");
		System.out.println("plot [" + lowX + ":" + highX + "] [" + lowY + ":" + highY + "] " + coefficients[counter][1]
				+ " * x + " + coefficients[counter][2] + " title 'best fit', '-' using 1:2 title 'all'");

		for (int i = 0; i < arrayResults.length; ++i)
			System.out.println(arrayResults[i][0] + " " + arrayResults[i][1]);
		System.out.println("e");

		System.out.println("DONE.");

	}

	private static String[] splitTestDataFile(String testDataFile, int numberOfParsers) {

		String[] fileNames = new String[numberOfParsers];
		String[] lines = readLinesFromFile(testDataFile);
		int[] numberOfLines = new int[numberOfParsers];
		int totalLines = lines.length;
		System.out.println("Total number of files = " + totalLines + "\n");

		for (int i = 0; i < numberOfParsers - 1; i++) {
			numberOfLines[i] = totalLines / numberOfParsers;
		}
		numberOfLines[numberOfParsers - 1] = totalLines - (totalLines / numberOfParsers) * (numberOfParsers - 1);

		int dotIndex = testDataFile.indexOf('.');
		int lineCount = 0;
		for (int i = 0; i < numberOfParsers; i++) {
			String fileName = testDataFile.substring(0, dotIndex) + "_" + i + ".dev";
			fileNames[i] = fileName;

			try {
				PrintWriter out = new PrintWriter(new FileWriter(fileName));

				System.out.println("Number of files in split test data file " + i + ": " + numberOfLines[i]);
				for (int k = 0; k < numberOfLines[i]; k++) {
					out.println(lines[lineCount++]);
				}

				out.close();
			} catch (IOException e) {
				System.err.println("Can not write to file " + fileName);
			}
		}

		return fileNames;
	}

	private static String[] readLinesFromFile(String fileName) {
		int count = 0;

		try {
			File file = new File(fileName);

			FileReader fr = new FileReader(file);
			LineNumberReader ln = new LineNumberReader(fr);

			while (ln.readLine() != null) {
				count++;
			}
			ln.close();
		} catch (IOException e) {
			System.err.println("Error reading " + fileName);
		}

		String[] lines = new String[count];
		try {
			File file = new File(fileName);

			FileReader fileReader = new FileReader(file);
			LineNumberReader ln = new LineNumberReader(fileReader);

			for (int i = 0; i < count; i++) {
				lines[i] = ln.readLine();
			}

			ln.close();
		} catch (IOException e) {
			System.err.println("Error reading " + fileName);
		}

		return lines;
	}

	/**
	 * Evaluates the specified document's coreference labels as well as the
	 * specified coherence classifier's ability to distinguish it from the gold
	 * labeled version and returns the results in an array.
	 * 
	 * @param document
	 *            The document to be evaluated.
	 * @param depth
	 *            The level in the search at which the document was produced.
	 * @param cohere
	 *            The coherence classifier.
	 * @param base
	 *            The base of the exponentiation in the sigmoid function applied
	 *            to the coherence classifier's output. Setting this parameter
	 *            to 0 results in no sigmoid function being applied.
	 * @return Coherence and coreference performance followed by the value of
	 *         <code>depth</code>, in that order, in an array.
	 **/
	private static double[] getResults(Document document, int depth, Learner cohere, double base) {
		Document[] pair = null;
		if (new Random(document.getName().hashCode()).nextBoolean())
			pair = new Document[] { document.getLabeled(), document };
		else
			pair = new Document[] { document, document.getLabeled() };
		String label = coherenceLabeler.discreteValue(pair);
		double score = cohere.scores(pair).get(label);
		if (base > 0)
			score = 1.0 / (1.0 + Math.pow(base, -score));

		double F1 = new CoreferenceTester(0).getF1(document, document.getLabeled());
		return new double[] { score, F1, depth };
	}

	/**
	 * This method returns a new {@link ClusterMerger.Filter} object that is
	 * "unattached" in the sense that it does not belong to any
	 * <code>ClusterMerger</code> object referenced anywhere else in the code.
	 * Of course, it is attached to some <code>ClusterMerger</code> object, but
	 * that object does not contain anything useful and should not be
	 * referenced. Thus, the object returned by this method should only be used
	 * as input to the various constructors in this package.
	 * 
	 * @param specifier
	 *            Can be <code>"none"</code> to create a
	 *            {@link ClusterMerger.Filter}, <code>"size"</code> to create a
	 *            {@link ClusterMerger.SizeFilter}, <code>"b3D"</code> to create
	 *            a {@link ClusterMerger.B3DistanceFilter}, or
	 *            <code>"clusterD"</code> to create a
	 *            {@link ClusterMerger.ClusterDistanceFilter}.
	 * @param argument
	 *            Passed to the constructors of
	 *            {@link ClusterMerger.B3DistanceFilter} and
	 *            {@link ClusterMerger.ClusterDistanceFilter}, and ignored by
	 *            other filters.
	 **/
	public static Filter getUnattachedFilter(String specifier, double argument) {
		Filter filter = null;

		if (specifier.equals("none"))
			filter = new ClusterMerger().new Filter();
		else if (specifier.equals("size"))
			filter = new ClusterMerger().new SizeFilter();
		else if (specifier.equals("b3D"))
			filter = new ClusterMerger().new B3DistanceFilter(argument);
		else if (specifier.equals("clusterD"))
			filter = new ClusterMerger().new ClusterDistanceFilter(argument);
		else {
			System.err.println("Unrecognized filter specifier: '" + specifier + "'");
			System.exit(1);
		}

		return filter;
	}

	public class Filter {
		protected boolean reject() {
			return false;
		}

		protected Filter clone(ClusterMerger merger) {
			return merger.new Filter();
		}
	}

	public class SizeFilter extends Filter {
		protected boolean reject() {
			return clusterSizes[i] > 2 || clusterSizes[j] > 2;
		}

		protected Filter clone(ClusterMerger merger) {
			return merger.new SizeFilter();
		}
	}

	public class B3DistanceFilter extends Filter {
		private double threshold;

		public B3DistanceFilter(double t) {
			threshold = t;
		}

		protected boolean reject() {
			return new CoreferenceTester().getF1(predicted, getDocument()) < threshold;
		}

		protected Filter clone(ClusterMerger merger) {
			return merger.new B3DistanceFilter(threshold);
		}
	}

	public class ClusterDistanceFilter extends Filter {
		private double threshold;

		public ClusterDistanceFilter(double t) {
			threshold = t;
		}

		protected boolean reject() {
			Vector<Vector<Document.Mention>> clusters = new Vector<Vector<Document.Mention>>();
			clusters.addAll(getDocument().getAllChains());
			double highest = -Double.MAX_VALUE;
			int I = i, J = j;
			for (int k = J - 1; k >= 0; --k)
				if (clusterSizes[k] == 0)
					--J;
			for (int k = I - 1; k >= 0; --k)
				if (clusterSizes[k] == 0)
					--I;

			for (Document.Mention mi : clusters.get(I))
				for (Document.Mention mj : clusters.get(J))
					highest = Math.max(highest, coref.scores(new Document.Mention[] { mi, mj }).get("true"));

			return highest < threshold;
		}

		protected Filter clone(ClusterMerger merger) {
			return merger.new ClusterDistanceFilter(threshold);
		}
	}
}
