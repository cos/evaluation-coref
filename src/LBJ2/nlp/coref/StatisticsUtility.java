package LBJ2.nlp.coref;

public class StatisticsUtility {

	private int numberOfDocs;    // the maximum number of documents in the corpus 
	private double[] execTimePerDoc; 
	private double[] timeFirstParsing;
	private double[] timeSecondParsing;
	private double[] timeFirstIteration;
	private int[] iterationsPerDocument;
	
	public StatisticsUtility(int numberOfDocuments) {
		numberOfDocs = numberOfDocuments;
		execTimePerDoc = new double[numberOfDocs];
		timeFirstParsing = new double[numberOfDocs];
		timeSecondParsing = new double[numberOfDocs];
		timeFirstIteration = new double[numberOfDocs];
		iterationsPerDocument = new int[numberOfDocs];
	}

	public void setTimeForDocument(int indexOfDocument, double timeForThisDoc) {
		execTimePerDoc[indexOfDocument] = timeForThisDoc;
	}
	
	public double averageExecTimePerDoc(){
		double totalTime = 0.0;
		for (int i = 0; i < numberOfDocs; ++i){
			totalTime += execTimePerDoc[i];
		}
		return totalTime / numberOfDocs;
	}

	public void setTimeForFirstParsing(int indexDoc, double time) {
		timeFirstParsing[indexDoc] = time;
	}

	public void setTimeForSecondParsing(int indexOfDocument, double d) {
		timeSecondParsing[indexOfDocument] = d;
	}

	public void setTimeForFirstIteration(int indexOfDocument,
			double firstIterationTime) {
		timeFirstIteration[indexOfDocument] = firstIterationTime;
	}
	
	public void printStatistics() {
		System.out.printf("The average time to process each document: %.2f\n", averageExecTimePerDoc());
		
		double maxTime = 0.0;
		double minTime = 10000000000.00; //
		int maxIndex = 0, minIndex = 0;
		double totalFirstParsing = 0.0;
		for (int i = 0; i < numberOfDocs; i++) {
			System.out.println("document " + i + "========");
			
			double timePerDoc = (execTimePerDoc[i] + timeFirstParsing[i]);
			System.out.printf("total execution time per doc: %.2f\n", timePerDoc);
			System.out.printf("timeFirstParsing: %.2f\n", timeFirstParsing[i]);
			System.out.printf("timeSeconParsing: %.2f\n", timeSecondParsing[i]);
			System.out.printf("timeFirstIteration: %.2f\n", timeFirstIteration[i]);
			System.out.println("iterations per Document: " + iterationsPerDocument[i]);
			totalFirstParsing += timeFirstParsing[i];
			
			if (timePerDoc > maxTime) {
				maxTime = timePerDoc;
				maxIndex = i;
			}
			
			if (timePerDoc < minTime) {
				minTime = timePerDoc;
				minIndex = i;
			}
		}
		
		System.out.printf("Total first parsing time: %.2f\n", totalFirstParsing);
		System.out.printf("Document %d has MIN exec time of %.2f\n", minIndex, minTime);
		System.out.printf("Document %d has MAX exec time of %.2f\n", maxIndex, maxTime);
	}

	public void setIterationPerDocument(int indexOfDocument, int iterationCount) {
		iterationsPerDocument[indexOfDocument] = iterationCount;
	}
	
//	System.err.printf("The time for doing firstIteration  %d: %.2f ms\n", indexOfDocument, firstIterationTime);
//	System.err.printf("Time parser %d doing firstIteration: %.2f%%\n", indexOfDocument, firstIterationTime * 100.0 / timeForThisDoc);
	
}
