package LBJ2.nlp.coref;

import java.util.*;
import LBJ2.classify.*;
import LBJ2.learn.*;
import LBJ2.infer.*;


/**
  * An inference algorithm that re-ranks a <i>k</i>-best list of best-link
  * coref results based on coherence scores.  This inference algorithm is
  * specifically designed to work with coreference classifiers that take pairs
  * of <code>LBJ2.nlp.coref.Document.Mention</code>s in an array as input and
  * coherence classifiers that take pairs of
  * <code>LBJ2.nlp.coref.Document</code>s in an array as input and will not
  * work with any other classifiers.
 **/
public class Rerank extends Inference
{
  /** The classifier being reranked. */
  protected DataCoref classifier;
  /** A coherence classifier to do the reranking. */
  protected Learner cohere;
  /** The size of the <i>k</i>-best list of coref results to rerank. */
  protected int K;
  /**
    * All coreference scores are multiplied by this weight, and all coherence
    * scores are multiplied by 1 minus this weight.
   **/
  protected double alpha;
  /** The "head" document, more conveniently accessible. */
  protected Document headDocument;
  /** All mentions in the document. */
  protected Document.Mention[] mentions;
  /**
    * The value at location <i>(i, j)</i> in this array stores the score of
    * the best-link coref classifier when mention <i>i</i> chooses mention
    * <i>j</i> as its "best link".
   **/
  protected ScoreEntry[][] scores;


  /**
    * Don't use this constructor, because it doesn't set values for {@link #K}
    * or {@link #classifier}.
   **/
  public Rerank() { this(null, null, 1); }

  /**
    * Initializes member variables, but not the head object.
    *
    * @param c  The classifier being reranked.
    * @param cc A coherence classifier to do the re-ranking.
    * @param k  The size of the <i>k</i>-best list of coref results to rerank.
   **/
  public Rerank(DataCoref c, Learner cc, int k) { this(c, cc, k, .5); }

  /**
    * Initializes member variables, but not the head object.
    *
    * @param c  The classifier being reranked.
    * @param cc A coherence classifier to do the re-ranking.
    * @param k  The size of the <i>k</i>-best list of coref results to rerank.
    * @param a  The value for {@link #alpha}.
   **/
  public Rerank(DataCoref c, Learner cc, int k, double a)
  {
    this(null, c, cc, k, a);
  }

  /**
    * Initializes member variables.
    *
    * @param h  The head object.
    * @param c  The classifier being reranked.
    * @param cc A coherence classifier to do the re-ranking.
    * @param k  The size of the <i>k</i>-best list of coref results to rerank.
   **/
  public Rerank(Object h, DataCoref c, Learner cc, int k)
  {
    this(h, c, cc, k, .5);
  }

  /**
    * Initializes member variables.
    *
    * @param h  The head object.
    * @param c  The classifier being reranked.
    * @param cc A coherence classifier to do the re-ranking.
    * @param k  The size of the <i>k</i>-best list of coref results to rerank.
    * @param a  The value for {@link #alpha}.
   **/
  public Rerank(Object h, DataCoref c, Learner cc, int k, double a)
  {
    super(h);
    headDocument = (Document) h;
    classifier = c;
    cohere = cc;
    K = k;
    alpha = a;
  }


  /** Performs the re-ranking inference. */
  protected void infer() throws Exception
  {
    if (scores != null) return;
    System.out.println(headDocument.getName() + "  "
                       + headDocument.totalMentions() + " mentions");
    instantiateVariables();
    Normalizer norm = getNormalizer(cohere);

    PredictedDocument[] kBest = new PredictedDocument[K];
    LinkedList<PredictedDocument> kBestDifferent =
      new LinkedList<PredictedDocument>();
    PredictedDocument.NextBests[] nextBests =
      new PredictedDocument.NextBests[K];
    kBest[0] = new PredictedDocument(new int[mentions.length]);
    /*
    kBest[0].coherenceScore =
      (1 - alpha)
      * norm.normalize(
          cohere.scores(new Document[]{ kBest[0].document, new Document() }))
        .get("true");
        */
    kBest[0].bCubed();
    kBestDifferent.add(kBest[0]);
    kBest[0].original = true;

    HashSet<String> seen = new HashSet<String>();
    String identifier = kBest[0].toString();
    seen.add(identifier);

    for (int i = 1; i < K; ++i)
    {
      nextBests[i - 1] = kBest[i - 1].getNextBests();
      int bestJ = -1;
      double bestScore = -Double.MAX_VALUE;

      for (int j = 0; j < i; ++j)
      {
        double current = nextBests[j].nextScore();

        if (current > bestScore)
        {
          bestScore = current;
          bestJ = j;
        }
      }

      if (bestJ == -1)
      {
        PredictedDocument[] temp = new PredictedDocument[i];
        System.arraycopy(kBest, 0, temp, 0, i);
        kBest = temp;
        break;
      }

      kBest[i] = nextBests[bestJ].next();

      identifier = kBest[i].toString();
      if (seen.contains(identifier)) continue;
      seen.add(identifier);
      kBestDifferent.add(kBest[i]);

      /*
      kBest[i].coherenceScore =
        (1 - alpha)
        * norm.normalize(
            cohere.scores(
              new Document[]{ kBest[i].document, new Document() }))
          .get("true");
          */
      kBest[i].bCubed();
    }

    Collections.sort(kBestDifferent);
    kBestDifferent.getFirst().setVariables();

    for (PredictedDocument doc : kBestDifferent)
    {
      if (doc.original) System.out.print("* ");
      else System.out.print("  ");
      System.out.println(doc.scoresToString());
    }

    System.out.println();
  }


  /**
    * Creates the first order variables involved in this inference problem as
    * well as initializing the {@link #mentions} and {@link #scores} member
    * variables.
   **/
  protected void instantiateVariables()
  {
    Normalizer norm = getNormalizer(classifier);
    double normZero =
      alpha
      * norm.normalize(new ScoreSet(new Score[]{ new Score("", 0) })).get("");
    /*
    mentions =
      (Document.Mention[]) headDocument.toArray(new Document.Mention[0]);
      */
    scores = new ScoreEntry[mentions.length][];

    for (int i = 0; i < mentions.length; ++i)
    {
      scores[i] = new ScoreEntry[i + 1];

      for (int j = 0; j < i; ++j)
      {
        FirstOrderVariable v =
          new FirstOrderVariable(
              classifier,
              headDocument.getMentionPair(mentions[j], mentions[i]));
        variables.put(v, v);
        scores[i][j] =
          new ScoreEntry(j,
                         alpha * norm.normalize(v.getScores()).get("true"));
      }

      scores[i][i] = new ScoreEntry(-1, normZero);
      Arrays.sort(scores[i]);
    }
  }


  /**
    * Retrieves the value of the specified variable as identified by the
    * classifier and the object that produce that variable.
    *
    * @param c  The classifier producing the variable.
    * @param o  The object from which the variable is produced.
    * @return   The current value of the requested variable.
   **/
  public String valueOf(Learner c, Object o) throws Exception
  {
    infer();
    return getVariable(new FirstOrderVariable(c, o)).getValue();
  }


  /**
    * Simply a wrapper for a ({@link Document#Mention}<code>[]</code>, score)
    * pair.  An instance of this class can also represent the case where a
    * mention does not have an antecedent.  In this case, {@link #index} will
    * be set to -1.
   **/
  protected class ScoreEntry implements Comparable
  {
    /**
      * The index of the earlier occurring mention in {@link Rerank#mentions};
      * the other mention in the pair is determined externally to this class.
     **/
    public int index;
    /** The score associated with the mention pair. */
    public double score;


    /**
      * Initializing constructor.
      *
      * @param i  A value for {@link #index}.
      * @param s  A value for {@link #score}.
     **/
    public ScoreEntry(int i, double s)
    {
      index = i;
      score = s;
    }


    /**
      * This method insures that instances of this class will be sorted in
      * decreasing order of their {@link #score}.
      *
      * @param o  An object to compare with.
      * @return   A value less than 0 if this score is greater than the
      *           input's, 0 if they are equal, and a value greater than 0
      *           otherwise.
     **/
    public int compareTo(Object o)
    {
      if (!(o instanceof ScoreEntry)) return -1;
      ScoreEntry e = (ScoreEntry) o;
      return new Double(e.score).compareTo(new Double(score));
    }


    /**
      * The string representation of a score entry is the mention ID of the
      * mention pointed to by it and the associated score separated by a comma
      * and surrounded by parentheses.
     **/
    public String toString()
    {
      String id = "none";
      //if (index != -1) id = mentions[index].mentionID;
      return "(" + id + ", " + score + ")";
    }
  }


  /**
    * Represents a copy of the "head" document with the entity IDs relabeled
    * to correspond to a prediction from the <i>k</i>-best list of the
    * coreference classifier.
   **/
  protected class PredictedDocument implements Comparable<PredictedDocument>
  {
    /**
      * Initialized with the same mentions as the "head" document, but with
      * modified entity IDs.
     **/
    public Document document;
    /**
      * The indexes into the {@link Rerank#scores} matrix that instantiate
      * this document.
     **/
    protected int[] indexes;
    /**
      * The score of this document when evaluated by the
      * <code>DataCoref</code> classifier.
     **/
    protected double corefScore;
    /** The score of this document when evaluated by <code>Cohere</code>. */
    protected double coherenceScore;
    /**
      * The <i>B<sup>3</sup></i> scores (precision, recall, and
      * <i>F<sub>1</sub></i> of this prediction.
     **/
    protected double[] bCubedScores;
    /**
      * The indexes of {@link #indexes} resorted into the order in which they
      * should be incremented to produce next-best predictions.
     **/
    protected int[][] nextI;
    /**
      * Whether or not this document is the original solution of the
      * coreference classifier before reranking.
     **/
    public boolean original;


    /**
      * Initializing constructor.
      *
      * @param indexes  The indexes into the {@link Rerank#scores} matrix that
      *                 represent the decisions made in this predicted
      *                 document.
     **/
    public PredictedDocument(int[] indexes)
    {
      double corefS = 0;
      for (int i = 0; i < indexes.length; ++i)
        corefS += scores[i][indexes[i]].score;
      initialize(indexes, corefS);
    }

    /**
      * Initializing constructor.
      *
      * @param indexes  The indexes into the {@link Rerank#scores} matrix that
      *                 represent the decisions made in this predicted
      *                 document.
      * @param corefS   The value for {@link #corefScore}.
     **/
    public PredictedDocument(int[] indexes, double corefS)
    {
      initialize(indexes, corefS);
    }


    /**
      * Initializes this instance of this class.
      *
      * @param indexes  The indexes into the {@link Rerank#scores} matrix that
      *                 represent the decisions made in this predicted
      *                 document.
      * @param corefS   The value for {@link #corefScore}.
     **/
    protected void initialize(final int[] indexes, double corefS)
    {
      this.indexes = indexes;
      corefScore = corefS;

      Vector<Document.Mention> newMentions = new Vector<Document.Mention>();
      int entityID = 0;

      for (int i = 0; i < indexes.length; ++i)
      {
        Document.Mention newMention =
          mentions[i].getDocument().new Mention(mentions[i]);
        newMentions.add(newMention);
        /*
        if (scores[i][indexes[i]].index == -1)
          newMention.entityID = "e" + entityID++;
        else
          newMention.entityID =
            ((Mention) newMentions.get(scores[i][indexes[i]].index)).entityID;
            */
      }

      /*
      document =
        new Document(headDocument.getName(), headDocument.getPath(),
                     headDocument.getText(), headDocument.getOffset(),
                     newMentions);
                     */

      nextI = new int[indexes.length][1];
      for (int i = 0; i < indexes.length; ++i) nextI[i][0] = i;
      final ScoreEntry[][] entries = scores;

      Arrays.sort(nextI,
          new Comparator<int[]>()
          {
            Double difference(int[] i)
            {
              double result = Double.MAX_VALUE;
              if (indexes[i[0]] + 1 < entries[i[0]].length)
                result =
                  entries[i[0]][indexes[i[0]]].score
                  - entries[i[0]][indexes[i[0]] + 1].score;
              return new Double(result);
            }

            public int compare(int[] i1, int[] i2)
            {
              return difference(i1).compareTo(difference(i2));
            }
          });
    }


    /**
      * Sets the first order variables of this inference problem to reflect
      * the predictions in this document.
     **/
    public void setVariables()
    {
      for (int i = 1; i < mentions.length; ++i)
        for (int j = 0; j < i; ++j)
        {
          Document.Mention[] p =
            headDocument.getMentionPair(mentions[j], mentions[i]);
          FirstOrderVariable v =
            getVariable(new FirstOrderVariable(classifier, p));
          v.setValue("" + (scores[i][indexes[i]].index == j));
        }
    }


    /** Sets the value of {@link #bCubedScores}. */
    public void bCubed()
    {
      int i = 0;

      /*
      for (Iterator I = document.iterator(); I.hasNext(); ++i)
      {
        Mention m = (Mention) I.next();
        mentions[i].predictedEntityID = m.entityID;
      }

      double totalMentions = headDocument.totalMentions();
      double[] results =
        new CoreferenceTester().test(new PredictedCoref(), headDocument);
      bCubedScores = new double[3];
      bCubedScores[0] = results[0];
      bCubedScores[1] = results[1];

      if (totalMentions > 0)
      {
        bCubedScores[0] /= totalMentions;
        bCubedScores[1] /= totalMentions;
        if (bCubedScores[0] + bCubedScores[1] != 0)
          bCubedScores[2] = 2 * bCubedScores[0] * bCubedScores[1]
                            / (bCubedScores[0] + bCubedScores[1]);
      }
      */
    }


    /**
      * Returns a string containing, in this order, the sum of the scores from
      * <code>DataCoref</code>, the score from <code>Cohere</code>, and the
      * <i>B<sup>3</sup></i> precision, recall, and <i>F<sub>1</sub></i>.
     **/
    public String scoresToString()
    {
      return corefScore + "\t" + coherenceScore + "\t" + bCubedScores[0]
             + "\t" + bCubedScores[1] + "\t" + bCubedScores[2];
    }


    /**
      * The string representation of a predicted document is the array of
      * links from each mention to the previous mention it links with followed
      * by string representations of all the mentions in the corresponding
      * document.
     **/
    public String toString()
    {
      Vector entities = new Vector();

      /*
      for (Iterator I = document.iterator(); I.hasNext(); )
      {
        Mention m = (Mention) I.next();
        int index = Integer.parseInt(m.entityID.substring(1));
        LinkedList mentionList = null;

        if (index < entities.size())
          mentionList = (LinkedList) entities.get(index);
        else
        {
          mentionList = new LinkedList();
          entities.add(mentionList);
        }

        mentionList.add(new Integer(m.getMentionIndex()));
      }
      */

      String result = "";

      for (int i = 0; i < entities.size(); ++i)
      {
        if (i != 0) result += " ";
        result += "(";
        LinkedList mentionList = (LinkedList) entities.get(i);
        Iterator I = mentionList.iterator();
        if (I.hasNext()) result += I.next();
        while (I.hasNext()) result += ", " + I.next();
        result += ")";
      }

      return result;
    }


    /**
      * Implemented so that sorting a collection of these objects will put
      * them in decreasing order of the sum of {@link #corefScore} and
      * {@link #coherenceScore}.
      *
      * @param d  A document to compare with.
      * @return   A value less than 0 if this score is greater than the
      *           input's, 0 if they are equal, and a value greater than 0
      *           otherwise.
     **/
    public int compareTo(PredictedDocument d)
    {
      return
        new Double(d.corefScore + d.coherenceScore)
        .compareTo(new Double(corefScore + coherenceScore));
    }


    /**
      * Returns an iterator of sorts for iterating through the next best
      * solutions after this one.
     **/
    public NextBests getNextBests() { return new NextBests(); }


    /**
      * An iterator of sorts for iterating through the next best solutions
      * after this one.
     **/
    protected class NextBests
    {
      /**
        * Index into the {@link LBJ2.nlp.coref.Rerank.PredictedDocument#nextI}
        * array.
       **/
      protected int index;


      /**
        * Returns <code>true</code> iff there is another index that can be
        * incremented.
       **/
      public boolean hasNext()
      {
        return index < nextI.length
               && indexes[nextI[index][0]] + 1
                  < scores[nextI[index][0]].length;
      }


      /**
        * Returns the score of the next best document prediction in this
        * iteration.
        *
        * @return The score of the next best document prediction in this
        *         iteration, or <code>-Double.MAX_VALUE</code> if there are no
        *         more document predictions in this iteration.
       **/
      public double nextScore()
      {
        if (!hasNext()) return -Double.MAX_VALUE;
        return
          corefScore
          - scores[nextI[index][0]][indexes[nextI[index][0]]].score
          + scores[nextI[index][0]][indexes[nextI[index][0]] + 1].score;
      }


      /**
        * Create and return the next best document prediction in this
        * iteration.
        *
        * @return The next best document prediction in this iteration, or
        *         <code>null</code> if there are no more document predictions
        *         in this iteration.
       **/
      public PredictedDocument next()
      {
        if (!hasNext()) return null;
        int[] newIndexes = (int[]) indexes.clone();
        ++newIndexes[nextI[index][0]];
        PredictedDocument result =
          new PredictedDocument(newIndexes, nextScore());
        ++index;
        return result;
      }
    }
  }
}

