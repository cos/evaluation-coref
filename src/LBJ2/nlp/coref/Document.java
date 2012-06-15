package LBJ2.nlp.coref;

import java.util.*;
import java.io.*;
import LBJ2.util.TableFormat;
import LBJ2.classify.Classifier;
import LBJ2.nlp.*;
import LBJ2.parse.LinkedVector;


/**
  * Represents a document as a set of mentions partitioned into groups
  * corresponding to the sentences they appeared in.
 **/
public class Document
{
  /** A flag controlling whether debugging messages are generated. */
  public static boolean DEBUG = false;
  /** The four mention types in an array. */
  public static final String[] mentionTypes =
    new String[]{ "NAM", "NOM", "PRO", "PRE" };


  /**
    * Computes <i>F<sub>1</sub></i>.
    *
    * @param p  The precision.
    * @param r  The recall.
    * @return The <i>F<sub>1</sub></i>.
   **/
  private static double F1(double p, double r) { return 2 * p * r / (p + r); }


  /** The name of the document. */
  private String name;
  /** The path within the ACE dataset where the document can be found. */
  private String path;
  /** The mentions in the document arranged by sentence. */
  private Mention[][] mentions;
  /** The full text of the document. */
  private String text;
  /** The full text of the document split into words and sentences. */
  private LinkedVector[] sentences;
  /** The offset within the text at which the content starts. */
  private int offset;
  /** {@link Mention} pairs are cached here. */
  private Mention[][][] mentionPairCache;
  /** Pairs of consecutive {@link Mention} arrays are cached here. */
  private Mention[][][] sentencePairCache;
  /** A label used by coherence classifiers. */
  private boolean moreCoherent;
  /**
    * A link to the document this one was cloned from, which is assumed to be
    * the labeled version from the data.
   **/
  private Document labeled;

  /** Cache for {@link #getNgramCounts(int)}. */
  private Vector<HashMap<String, Integer>> ngramCounts =
    new Vector<HashMap<String, Integer>>();
  /** Cache for {@link #getRepresentativeNgramCounts(int)}. */
  private Vector<HashMap<String, Integer>> representativeNgramCounts =
    new Vector<HashMap<String, Integer>>();
  /** Cache for {@link #getChainNgramCounts(int)}. */
  private Vector<HashMap<String, Integer>> chainNgramCounts =
    new Vector<HashMap<String, Integer>>();
  /** Cache for {@link #headWordClusterCounts(int)}. */
  private HashMap<String, Integer> headWordCounts =
    new HashMap<String, Integer>();
  /** Cache for {@link #getStatistics()}. */
  private Vector<Double> statistics = new Vector<Double>();
  /** Cache for {@link #getAdjacencyCounts()}. */
  private HashMap<String, Integer> adjacencyCounts =
    new HashMap<String, Integer>();
  /** Cache for {@link #getSubjectNgramCounts(int)}. */
  private Vector<HashMap<String, Integer>> subjectNgramCounts =
    new Vector<HashMap<String, Integer>>();


  /**
    * This constructor should only used by the
    * {@link #shuffleSentences(Random)} and {@link #shuffleEntityIds(Random)}
    * methods.
    *
    * @param d  A document to clone.
    * @param I  Whether or not to set the mentions' indexes.
   **/
  private Document(Document d, boolean I)
  {
    name = d.name;
    path = d.path;
    mentions = new Mention[d.mentions.length][];
    text = d.text;
    sentences = new LinkedVector[d.sentences.length];
    for (int i = 0; i < d.sentences.length; ++i)
      sentences[i] = (LinkedVector) d.sentences[i].clone();

    offset = d.offset;
    mentionPairCache =
      new Mention[d.mentionPairCache.length][d.mentionPairCache.length][];

    for (int i = 0; i < mentions.length; ++i)
    {
      mentions[i] = new Mention[d.mentions[i].length];

      for (int j = 0; j < mentions[i].length; ++j)
      {
        mentions[i][j] = new Mention(d.mentions[i][j]);
        if (I)
          mentions[i][j].setIndexes(
              i, j, d.mentions[i][j].getIndexInDocument(),
              d.mentions[i][j].getWordIndexInSentence(),
              d.mentions[i][j].getWordIndexInDocument(), 0);
      }
    }

    buildSentencePairCache();
    labeled = d;
  }

  /**
    * Clones the specified document.
    *
    * @param d  A document to clone.
   **/
  public Document(Document d) { this(d, true); }

  /**
    * Initializing constructor.
    *
    * @param n    The name of the document.
    * @param p    The path within the ACE dataset where the document can be
    *             found.
    * @param t    The full text of the document.
    * @param o    The offset within the input file at which the full text
    *             started.
    * @param list A (flat) list of the mention data in the document.
   **/
  public Document(String n, String p, String t, int o, List<MentionData> list)
  {
    name = n;
    path = p;
    text = t;
    offset = o;
    mentionPairCache = new Mention[list.size()][list.size()][];
    moreCoherent = true;
    Vector<LinkedList<Mention>> temp = new Vector<LinkedList<Mention>>();
    LinkedList<Mention> sentence = new LinkedList<Mention>();
    temp.add(sentence);

    LinkedList<Mention> mentionList = new LinkedList<Mention>();
    for (MentionData data : list) mentionList.add(new Mention(data));
    Collections.sort(mentionList);

    Sentence[] rawSentences = splitSentences(mentionList);
    sentences = new LinkedVector[rawSentences.length];
    for (int i = 0; i < sentences.length; ++i)
      sentences[i] = rawSentences[i].wordSplit();

    if (DEBUG)
    {
      System.out.println("\n" + name + ", " + rawSentences.length
                         + " sentences:");
      for (int i = 0; i < rawSentences.length; ++i)
        System.out.println("'" + rawSentences[i] + "'");
    }

    int i = 0, j = 0, d = 0, wordIndex = 0;

    for (Mention m : mentionList)
    {
      if (DEBUG
          && !(i < sentences.length
               && m.getExtentStart()
                  >= offset + rawSentences[i].start
                     + rawSentences[i].text.length()))
      {
        System.out.println(
            "SAME SENTENCE: mention '" + m.getHead()
            + "' (" + m.getExtentStart() + "-" + m.getExtentEnd()
            + ") < end position of '" + rawSentences[i].text + "': ("
            + (offset + rawSentences[i].start) + ", "
            + (offset + rawSentences[i].start + rawSentences[i].text.length())
            + ")");
      }

      while (i < sentences.length
             && m.getExtentStart()
                >= offset + rawSentences[i].start
                   + rawSentences[i].text.length())
      {
        if (DEBUG)
          System.out.println(
              "NEXT SENTENCE: mention '" + m.getHead()
              + "' (" + m.getExtentStart() + "-" + m.getExtentEnd()
              + ") >= end position of '" + rawSentences[i].text + "': ("
              + (offset + rawSentences[i].start) + ", "
              + (offset + rawSentences[i].start
                 + rawSentences[i].text.length())
              + ")");

        wordIndex += sentences[i++].size();
        j = 0;
        sentence = new LinkedList<Mention>();
        temp.add(sentence);
      }

      int k = 0;

      while (!(((Word) sentences[i].get(k)).start + offset <= m.getHeadStart()
               && ((Word) sentences[i].get(k)).end + offset
                  >= m.getHeadStart()))
      {
        if (DEBUG)
        {
          Word w = (Word) sentences[i].get(k);
          System.out.println("-" + m.getHead() + ":" + m.getHeadStart() + ", "
                             + w.form + ":" + (w.start + offset));
        }

        ++k;
      }

      if (DEBUG)
      {
        Word w = (Word) sentences[i].get(k);
        System.out.println("+" + m.getHead() + ":" + m.getHeadStart() + ", "
                           + w.form + ":" + (w.start + offset));
      }

      m.setIndexes(i, j++, d++, k, wordIndex + k, 0);
      sentence.add(m);
    }

    mentions = new Mention[temp.size()][];

    for (j = 0; j < mentions.length; ++j)
    {
      mentions[j] = temp.get(j).toArray(new Mention[0]);

      if (DEBUG)
      {
        System.out.print("  " + j + ": ");
        for (int k = 0; k < mentions[j].length; ++k)
          System.out.print(mentions[j][k].getEntityID() + "("
                           + mentions[j][k].getType() + "), ");
        System.out.println();
      }
    }

    buildSentencePairCache();
  }


  /** Only used by assertions. */
  private static final String[] codes = { "&AMP;", "&amp;" };
  /** Only used by assertions. */
  private static final String[] characters = { "&", "&" };

  /** Only used by assertions. */
  private static String clean(String text)
  {
    StringBuffer buffer = new StringBuffer(text);
    for (int i = 0; i < codes.length; ++i)
      for (int j = buffer.indexOf(codes[i]); j != -1;
           j = buffer.indexOf(codes[i]))
        buffer.replace(j, j + codes[i].length(), characters[i]);
    return buffer.toString();
  }


  /**
    * Creates and initializes the {@link #sentencePairCache}.  Called only by
    * constructors.
   **/
  private void buildSentencePairCache()
  {
    /*
    sentencePairCache = new Mention[mentions.length - 1][][];
    for (int i = 0; i < sentencePairCache.length; ++i)
      sentencePairCache[i] = new Mention[][]{ mentions[i], mentions[i + 1] };
      */
  }


  /** Clears all the feature extraction related caches. */
  private void clearCaches()
  {
    ngramCounts.clear();
    representativeNgramCounts.clear();
    chainNgramCounts.clear();
    headWordCounts.clear();
    statistics.clear();
    adjacencyCounts.clear();
    subjectNgramCounts.clear();
  }


  /**
    * Chooses one of two sentence splitting methods based on the distribution
    * of capital letters found in the text and applies it.
    *
    * @param list The list of mentions in this document.  This parameter is
    *             only used if there are no (or very few) capital letters in
    *             the document.
    * @return The split sentences.
   **/
  private Sentence[] splitSentences(List<Mention> list)
  {
    int capitals = 0;
    int lowercase = 0;
    String text = this.text.substring(offset);

    for (int i = 0; i < text.length(); ++i)
    {
      if ('A' <= text.charAt(i) && text.charAt(i) <= 'Z')
      {
        if (DEBUG) System.out.print(text.charAt(i));
        ++capitals;
      }
      else if ('a' <= text.charAt(i) && text.charAt(i) <= 'z') ++lowercase;
    }

    if (DEBUG)
      System.out.println("\nc = " + capitals + ", l = " + lowercase);

    if (300 * capitals < lowercase)
    {
      int start = 0;
      int end = 0;
      LinkedList<Sentence> sentences = new LinkedList<Sentence>();
      boolean[] noSplit = new boolean[text.length()];

      for (Mention m : list)
        for (int i = m.getExtentStart(); i <= m.getExtentEnd(); ++i)
          noSplit[i - offset] = true;

      while (start < text.length())
      {
        while (start < text.length()
               && Character.isWhitespace(text.charAt(start)))
          ++start;
        if (start == text.length()) break;
        end = start;

        while (end < text.length()
               && (".!?".indexOf(text.charAt(end)) == -1 || noSplit[end]))
          ++end;

        if (end == text.length()) --end;
        sentences.add(
            new Sentence(text.substring(start, end + 1), start, end));
        start = end + 1;
      }

      return sentences.toArray(new Sentence[sentences.size()]);
    }

    return new SentenceSplitter(text.split("\n")).splitAll();
  }


  /** Sets the value of {@link #moreCoherent}. */
  public void setMoreCoherent(boolean v) { moreCoherent = v; }


  /** Returns the value of {@link #moreCoherent}. */
  public boolean getMoreCoherent() { return moreCoherent; }
  /** Returns the name of the document. */
  public String getName() { return name; }
  /**
    * Returns the path within the ACE distribution where the document can be
    * found.
   **/
  public String getPath() { return path; }
  /** Returns the full text of the document. */
  public String getText() { return text.substring(offset); }
  /** Returns the offset within the text at which the content starts. */
  public int getOffset() { return offset; }
  /** Returns the total number of mentions in the document. */
  public int totalMentions() { return mentionPairCache.length; }
  /** Returns the number of sentences in the document. */
  public int sentences() { return mentions.length; }
  /** Returns the value of {@link #labeled}. */
  public Document getLabeled() { return labeled; }


  /**
    * Returns the number of mentions in the specified sentence.
    *
    * @param i  The index of the specified sentence.
    * @return The number of mentions in sentence <code>i</code>.
   **/
  public int mentionsInSentence(int i)
  {
    if (i < 0 || i >= mentions.length) throw new NoSuchElementException();
    return mentions[i].length;
  }


  /**
    * Returns the specified mention.
    *
    * @param i  The index of the sentence containing the mention.
    * @param j  The index of the mention in sentence <code>i</code>.
    * @return A reference to the specified mention.
   **/
  public Mention getMention(int i, int j)
  {
    if (i < 0 || j < 0 || i >= mentions.length || j >= mentions[i].length)
      throw
        new NoSuchElementException(
            "Either " + i + " >= " + mentions.length + " or " + j + " >= "
            + mentions[i].length);
    return mentions[i][j];
  }


  /**
    * Returns the number of words in the specified sentence.
    *
    * @param i  The index of the specified sentence.
    * @return The number of words in sentence <code>i</code>.
   **/
  public int wordsInSentence(int i)
  {
    if (i < 0 || i >= sentences.length) throw new NoSuchElementException();
    return sentences[i].size();
  }


  /**
    * Returns the specified word.
    *
    * @param i  The index of the sentence containing the word.
    * @param j  The index of the word in sentence <code>i</code>.
    * @return A reference to the specified word.
   **/
  public Word getWord(int i, int j)
  {
    if (i < 0 || j < 0 || i >= sentences.length || j >= sentences[i].size())
      throw new NoSuchElementException();
    return (Word) sentences[i].get(j);
  }


  /**
    * Throws an exception iff the given mention is not from this document.
    *
    * @param m  The mention to check.
   **/
  public void checkMention(Mention m)
  {
    if (m != mentions[m.getSentenceIndex()][m.getIndexInSentence()])
      throw
        new NoSuchElementException(
            "ERROR: Mention '" + m + "' not from document " + name);
  }


  /**
    * Computes the set of mentions in the same coreference cluster as the
    * given mention.
    *
    * @param m  The mention whose cluster will be computed.
    * @return A set of {@link Document.Mention}s representing the cluster.
   **/
  public HashSet<Mention> getCluster(Mention m)
  {
    checkMention(m);
    LinkedHashSet<Mention> result = new LinkedHashSet<Mention>();

    for (int i = 0; i < mentions.length; ++i)
      for (int j = 0; j < mentions[i].length; ++j)
        if (m.getEntityID().equals(mentions[i][j].getEntityID()))
          result.add(mentions[i][j]);

    return result;
  }


  /**
    * Returns a canonical object for the specified consecutive pair of
    * sentences.
    *
    * @param i1 The index of the first sentence in the consecutive pair.
    * @return An array containing both {@link Mention} arrays.  This array
    *         object is guaranteed to be the same array object every time this
    *         method is called with the same argument.
   **/
  public Mention[][] getSentencePair(int i1) { return sentencePairCache[i1]; }


  /**
    * Returns a canonical object for the specified mention pair.
    *
    * @param m1 The first mention in the pair.
    * @param m2 The second mention in the pair.
    * @return An array containing both mentions.  This array object is
    *         guaranteed to be the same array object every time this method is
    *         called with the same arguments.
   **/
  public Mention[] getMentionPair(Mention m1, Mention m2)
  {
    checkMention(m1);
    checkMention(m2);
    int i1 = m1.getIndexInDocument();
    int i2 = m2.getIndexInDocument();

    if (mentionPairCache[i1][i2] == null)
    {
      if (i2 < i1)
      {
        Mention t = m1;
        m1 = m2;
        m2 = t;
      }

      mentionPairCache[i1][i2] = mentionPairCache[i2][i1] =
        new Mention[]{ m1, m2 };
    }

    return mentionPairCache[i1][i2];
  }


  /**
    * Returns a copy of this object whose sentences have been shuffled into a
    * random order.  This method simply passes a random number generator
    * seeded with the current system time to
    * {@link #shuffleSentences(Random)}.
   **/
  public Document shuffleSentences()
  {
    return shuffleSentences(new Random());
  }


  /**
    * Returns a copy of this object whose sentences have been shuffled into a
    * random order.  The provided random number generator is used to do the
    * shuffling.
    *
    * @param randomizer A random number generator.
    * @return A copy of this object whose sentences have been shuffled.
   **/
  public Document shuffleSentences(Random randomizer)
  {
    Document result = new Document(this, false);
    int[][] wordOffsets = new int[mentions.length][];

    for (int i = 0; i < mentions.length; ++i)
    {
      wordOffsets[i] = new int[mentions[i].length];
      for (int j = 0; j < mentions[i].length; ++j)
        wordOffsets[i][j] = mentions[i][j].getWordIndexInSentence();
    }

    int d = 0, words = 0, offset = ((Word) result.sentences[0].get(0)).start;

    for (int i = 0; i < result.mentions.length; ++i)
    {
      int j = i + randomizer.nextInt(result.mentions.length - i);
      LinkedVector s = result.sentences[j];
      result.sentences[j] = result.sentences[i];
      result.sentences[i] = s;
      int delta = offset - ((Word) result.sentences[i].get(0)).start;

      for (int k = 0; k < result.sentences[i].size(); ++k)
      {
        Word w = (Word) result.sentences[i].get(k);
        w.start += delta;
        w.end += delta;
      }

      Mention[] m = result.mentions[j];
      result.mentions[j] = result.mentions[i];
      result.mentions[i] = m;

      int[] a = wordOffsets[j];
      wordOffsets[j] = wordOffsets[i];
      wordOffsets[i] = a;

      for (int k = 0; k < result.mentions[i].length; ++k, ++d)
        result.mentions[i][k].setIndexes(i, k, d, wordOffsets[i][k],
                                         words + wordOffsets[i][k], delta);

      int wordLength = result.sentences[i].size();
      words += wordLength;
      offset += ((Word) result.sentences[i].get(wordLength - 1)).end + 2;
    }

    return result;
  }


  /**
    * Returns a new <code>Document</code> whose sentences and mentions are
    * identical, except that the entity IDs have been redistributed randomly
    * across the mentions.
    *
    * @param randomizer A random number generator.
    * @return A copy of this object in which entity IDs have been
    *         redistributed randomly across the mentions.
   **/
  public Document shuffleEntityIds(Random randomizer)
  {
    HashSet<String> entityIds = new HashSet<String>();
    for (int i = 0; i < mentions.length; ++i)
      for (int j = 0; j < mentions[i].length; ++j)
        entityIds.add(mentions[i][j].getEntityID());
    String[] ids = entityIds.toArray(new String[entityIds.size()]);
    Document result = new Document(this);

    for (int i = 0; i < mentions.length; ++i)
      for (int j = 0; j < mentions[i].length; ++j)
        result.mentions[i][j]
          .setEntityID(ids[randomizer.nextInt(ids.length)]);

    return result;
  }


  /**
    * Overwrites the {@link Mention#entityID} fields of the mentions in the
    * document using predictions from the specified classifier.
    *
    * @param classifier The classifier that predicts links between mentions.
    * @param verbosity  The higher this number, the more messages will be sent
    *                   to <code>STDOUT</code> describing the activity of this
    *                   method.
    * @return The number of coreference clusters (i.e. chains) in the
    *         document.
   **/
  public int fillInPredictions(Classifier classifier, int verbosity)
  {
    int nextEntityID = 0;
    String message = null;
    if (verbosity > 2) message = getName() + ", " + classifier.name + ": ";

    for (int i = 0; i < mentions.length; ++i)
      for (int j = 0; j < mentions[i].length; ++j)
      {
        boolean assigned = false;

        for (int l = j - 1; l >= 0 && !assigned; --l)
          if (classifier.discreteValue(
                getMentionPair(mentions[i][l], mentions[i][j]))
              .equals("true"))
          {
            mentions[i][j].setEntityID(mentions[i][l].getEntityID());
            assigned = true;
          }

        for (int k = i - 1; k >= 0 && !assigned; --k)
          for (int l = mentions[k].length - 1; l >= 0 && !assigned; --l)
          if (classifier.discreteValue(
                getMentionPair(mentions[k][l], mentions[i][j]))
              .equals("true"))
          {
            mentions[i][j].setEntityID(mentions[k][l].getEntityID());
            assigned = true;
          }

        if (!assigned) mentions[i][j].setEntityID("e" + nextEntityID++);
        if (verbosity > 2) message += mentions[i][j].getEntityID() + ", ";
      }

    if (verbosity > 2) System.out.println(message);
    return nextEntityID;
  }


  /**
    * Counts all mention type <i>n</i>-grams in the document and creates a map
    * from the names of those <i>n</i>-grams to their counts.  A mention type
    * <i>n</i>-gram is simply the types of <i>n</i> mentions of the same
    * entity in different, consecutive sentences.  If an entity is not
    * mentioned in a given sentence, we halucinate a single mention of that
    * entity with type <code>"X"</code>.
    *
    * @param n  The number of terms in the <i>n</i>-gram.
    * @return A map from mention type <i>n</i>-gram name to the count for that
    *         <i>n</i>-gram.
   **/
  public HashMap<String, Integer> getNgramCounts(int n)
  {
    if (n < 1) return new HashMap<String, Integer>();

    if (n >= ngramCounts.size()) ngramCounts.setSize(n + 1);
    HashMap<String, Integer> counts = ngramCounts.get(n);
    if (counts != null) return counts;
    counts = new HashMap<String, Integer>();
    ngramCounts.set(n, counts);

    Vector<HashMap<String, HashSet<String>>> grid =
      new Vector<HashMap<String, HashSet<String>>>();
    HashSet<String> ids = new HashSet<String>();

    for (int i = 0; i < mentions.length; ++i)
    {
      HashMap<String, HashSet<String>> row =
        new HashMap<String, HashSet<String>>();
      grid.add(row);

      for (int j = 0; j < mentions[i].length; ++j)
      {
        String id = mentions[i][j].getEntityID();
        ids.add(id);
        HashSet<String> types = row.get(id);

        if (types == null)
        {
          types = new HashSet<String>();
          row.put(id, types);
        }

        types.add(mentions[i][j].getType());
      }
    }

    int N = Math.min(grid.size(), n);
    HashSet<String> empty = new HashSet<String>();
    empty.add("X");

    for (String id : ids)
    {
      Vector<HashSet<String>> column = new Vector<HashSet<String>>();

      for (int i = 0; i < grid.size(); ++i)
      {
        HashSet<String> types = grid.get(i).get(id);
        if (types == null) types = empty;
        column.add(types);
      }

      for (int i = 0; i + N - 1 < grid.size(); ++i)
      {
        int j = 0;
        Vector<java.util.Iterator<String>> stack =
          new Vector<java.util.Iterator<String>>();
        String[] types = new String[N];

        do
        {
          while (stack.size() < N)
            stack.add(column.get(i + stack.size()).iterator());
          for (; j < N; ++j)
            types[j] = stack.get(j).next();
          while (stack.size() > 0 && !stack.lastElement().hasNext())
            stack.removeElementAt(stack.size() - 1);
          j = stack.size() - 1;

          String ngram = types[0];
          for (int k = 1; k < N; ++k) ngram += "-" + types[k];

          Integer count = counts.get(ngram);
          if (count == null) count = 0;
          counts.put(ngram, count + 1);
        } while (stack.size() > 0);
      }
    }

    return counts;
  }


  /**
    * Counts all n-grams of representative mention types in the document and
    * creates a map from the names of those n-grams to their counts.  A
    * representative mention type n-gram is the concatenation of the
    * representative types of n mentions of the same entity in different,
    * consecutive sentences.  If an entity is not mentioned in a given
    * sentence, we halucinate a single mention of that entity with type
    * <code>"X"</code>.
    *
    * @param n  The number of elements in the n-gram.
    *
    * @return A map from mention type n-gram name to the count for that
    *         n-gram.
   **/
  public HashMap<String, Integer> getRepresentativeNgramCounts(int n)
  {
    if (n < 1) return new HashMap<String, Integer>();

    if (n >= representativeNgramCounts.size())
      representativeNgramCounts.setSize(n + 1);
    HashMap<String, Integer> counts = representativeNgramCounts.get(n);
    if (counts != null) return counts;
    counts = new HashMap<String, Integer>();
    representativeNgramCounts.set(n, counts);

    Vector<HashMap<String, String>> grid =
      new Vector<HashMap<String, String>>();
    HashSet<String> ids = new HashSet<String>();

    for (int i = 0; i < mentions.length; ++i)
    {
      HashMap<String, String> row = new HashMap<String, String>();
      grid.add(row);

      for (int j = 0; j < mentions[i].length; ++j)
      {
        String id = mentions[i][j].getEntityID();
        ids.add(id);
        String best = row.get(id);
        int b = 0;
        if (best == null) b = mentionTypes.length;
        while (b < mentionTypes.length && !best.equals(mentionTypes[b])) ++b;
        String current = mentions[i][j].getType();
        int k = 0;
        while (k < mentionTypes.length && !current.equals(mentionTypes[k]))
          ++k;
        if (k < b) row.put(id, current);
      }
    }

    int N = Math.min(grid.size(), n);

    for (String id : ids)
    {
      String[] column = new String[grid.size()];

      for (int i = 0; i < grid.size(); ++i)
      {
        column[i] = grid.get(i).get(id);
        if (column[i] == null) column[i] = "X";
      }

      for (int i = 0; i + N - 1 < grid.size(); ++i)
      {
        String ngram = column[i];
        boolean allX = ngram.equals("X");

        for (int k = 1; k < N; ++k)
        {
          ngram += "-" + column[i + k];
          allX &= column[i + k].equals("X");
        }

        if (!allX)
        {
          Integer count = counts.get(ngram);
          if (count == null) count = 0;
          counts.put(ngram, count + 1);
        }
      }
    }

    /*
    String[] keys = counts.keySet().toArray(new String[counts.size()]);
    Arrays.sort(keys);
    System.err.print(" ");
    for (String key : keys)
      System.err.print(" g" + key + ":" + counts.get(key));
    System.err.println();
    */

    return counts;
  }


  /**
    * Counts the number of times that each possible pairing of mention types
    * occurs adjacent to each other when the two mentions are either part of
    * the same cluster or not.  Adjacency is defined as in
    * {@link Document.Mention#adjacentTo()}.
   **/
  public HashMap<String, Integer> getAdjacencyCounts()
  {
    if (adjacencyCounts.size() > 0) return adjacencyCounts;

    for (int i = 0; i < mentions.length; ++i)
      for (int j = 0; j < mentions[i].length; ++j)
        for (int k = j + 1; k < mentions[i].length; ++k)
          if (mentions[i][j].adjacentTo(mentions[i][k]))
          {
            String prediction =
              mentions[i][j].getEntityID()
              .equals(mentions[i][k].getEntityID()) ? "+" : "-";
            String key =
              mentions[i][j].getType() + "|" + mentions[i][k].getType() + "|"
              + prediction;
            Integer count = adjacencyCounts.get(key);
            if (count == null) count = 0;
            adjacencyCounts.put(key, count + 1);
          }

    return adjacencyCounts;
  }


  /**
    * Returns the mention with the most specific mention type that satisfies
    * {@link Document.Mention#isSubject()} for the specified sentence.
    *
    * @param s  The index of the specified sentence.
    * @return The mention with the most specific mention type that satisfies
    *         {@link Document.Mention#isSubject()} for the specified sentence,
    *         or <code>null</code> if no mention qualifies.
   **/
  public Mention getSubject(int s)
  {
    Mention best = null;
    int b = mentionTypes.length;

    for (int i = 0; i < mentions[s].length; ++i)
      if (mentions[s][i].isSubject())
      {
        int c = 0;
        while (c < mentionTypes.length
               && !mentions[s][i].getType().equals(mentionTypes[c]))
          ++c;

        if (c < b)
        {
          b = c;
          best = mentions[s][i];
        }
      }

    return best;
  }


  /**
    * Counts how often each mention type bigram, formed by looking at the
    * subjects of consecutive sentences, appears either in the same cluster or
    * in different clusters.
    *
    * @param n  The number of terms in the <i>n</i>-gram.
    * @return A map from mention type <i>n</i>-gram name to the count for that
    *         <i>n</i>-gram.
    * @see #getSubject(int)
   **/
  public HashMap<String, Integer> getSubjectNgramCounts(int n)
  {
    if (n < 1) return new HashMap<String, Integer>();

    if (n >= subjectNgramCounts.size()) subjectNgramCounts.setSize(n + 1);
    HashMap<String, Integer> counts = subjectNgramCounts.get(n);
    if (counts != null) return counts;
    counts = new HashMap<String, Integer>();
    subjectNgramCounts.set(n, counts);

    Mention[] subjects = new Mention[mentions.length];
    for (int i = 0; i < mentions.length; ++i) subjects[i] = getSubject(i);
    int N = Math.min(mentions.length, n);

    for (int i = 0; i + N - 1 < mentions.length; ++i)
    {
      int j = 0;
      for (; j < N && subjects[i + j] != null; ++j);

      if (j < N)
      {
        i += j;
        continue;
      }

      String key = subjects[i].getType();
      boolean sameCluster = true;

      for (j = 1; j < N; ++j)
      {
        key += "-" + subjects[i + j].getType();
        sameCluster &=
          subjects[i + j].getEntityID().equals(subjects[i].getEntityID());
      }

      key += "|" + (sameCluster ? "+" : "-");
      Integer count = counts.get(key);
      if (count == null) count = 0;
      counts.put(key, count + 1);
    }

    return counts;
  }


  /**
    * This method returns all the coreference chains in the document in the
    * order of first appearance.
   **/
  public Collection<Vector<Mention>> getAllChains()
  {
    LinkedHashMap<String, Vector<Mention>> chains =
      new LinkedHashMap<String, Vector<Mention>>();

    for (Mention[] sentence : mentions)
      for (Mention m : sentence)
      {
        String id = m.getEntityID();
        Vector<Mention> chain = chains.get(id);

        if (chain == null)
        {
          chain = new Vector<Mention>();
          chains.put(id, chain);
        }

        chain.add(m);
      }

    return chains.values();
  }


  /**
    * Compiles statistics about the way entities and mentions are distributed
    * across the document.  The returned vector contains in the following
    * order: <br>
    * <code>[0]</code> words in the document divided by entities,
    * <code>[1]</code> mean, <code>[2]</code> standard deviation,
    * <code>[3]</code> skewness, and <code>[4]</code> kurtosis of mentions per
    * entity, <code>[5]</code> standard deviation, <code>[6]</code> skewness,
    * and <code>[7]</code> kurtosis of mention head word indexes from the most
    * mentioned entity, <code>[8]</code> standard deviation, <code>[9]</code>
    * skewness, and <code>[10]</code> kurtosis of mention head word indexes
    * from the second most mentioned entity, and <code>[11]</code> standard
    * deviation, <code>[12]</code> skewness, and <code>[13]</code> kurtosis of
    * mention head word indexes from the third most mentioned entity.
    *
    * <p> The statistics from a given entity of the three most mentioned
    * entities will appear only if it was mentioned more than 4 times.
   **/
  public Vector<Double> getStatistics()
  {
    /*
    System.err.println("\n" + name + "(" + moreCoherent + "):");

    for (Mention[] sentence : mentions)
    {
      System.err.print(" ");
      for (Mention m : sentence)
        System.err.print(
            " " + m.getMentionID() + ":" + m.getType() + "(" + m.getEntityID()
            + ")");
      System.err.println();
    }
    */

    if (statistics.size() > 0) return statistics;
    Vector<Vector<Mention>> chains = new Vector<Vector<Mention>>();
    chains.addAll(getAllChains());

    Mention[] last = mentions[mentions.length - 1];
    for (int i = mentions.length - 2; last.length == 0; --i)
      last = mentions[i];
    int totalWords = last[last.length - 1].getWordIndexInDocument() + 1;
    /*
    int totalWords = 0;
    for (int i = 0; i < sentences.length; ++i)
      totalWords += sentences[i].size();
      */
    statistics.add(totalWords / (double) chains.size());

    int i = 0;
    double[] data = new double[chains.size()];
    for (Vector<Mention> chain : chains) data[i++] = chain.size();
    statistics.addAll(moments(data, true));

    Collections.sort(chains,
        new Comparator<Vector<Mention> >()
        {
          public int compare(Vector<Mention> v1, Vector<Mention> v2)
          {
            return v2.size() - v1.size();
          }
        });

    //System.err.print(" ");

    i = 0;
    for (Vector<Mention> chain : chains)
    {
      if (chain.size() < 5) break;
      data = new double[chain.size()];

      /*
      System.err.print(
          " (" + chain.get(0).getEntityID() + "):" + chain.size());
          */

      for (int j = 0; j < data.length; ++j)
        data[j] = chain.get(j).getWordIndexInDocument() / (double) totalWords;
      statistics.addAll(moments(data, false));
      if (++i == 3) break;
    }

    for (i = 8; i < statistics.size(); i += 3)
      for (int j = i - 3;
           j >= 5
           && chains.get((j - 5) / 3).size() == chains.get((j - 2) / 3).size()
           && statistics.get(j + 3) < statistics.get(j);
           j -= 3)
        for (int k = 0; k < 3; ++k)
          Collections.swap(statistics, j + k, j + k + 3);

    /*
    for (i = 0; i < statistics.size(); ++i)
      System.err.print(
          " " + i + ":" + String.format("%.5f", statistics.get(i)));
    System.err.println();
    */

    return statistics;
  }


  /**
    * Returns unbiased estimates of the moments of the distribution whose
    * sample is given as input.
    *
    * @param sample A sample of the distribution.
    * @param m      Whether or not the mean should be returned.
    * @return Unbiased estimates of the sampled distribution's moments.
   **/
  public static LinkedList<Double> moments(double[] sample, boolean m)
  {
    LinkedList<Double> result = new LinkedList<Double>();
    double mean = 0;
    double n = sample.length;
    for (int i = 0; i < n; ++i) mean += sample[i];
    if (n > 0) mean /= n;
    if (m) result.add(mean);

    double sumSquaredDeviations = 0;
    for (int i = 0; i < n; ++i)
      sumSquaredDeviations += (sample[i] - mean) * (sample[i] - mean);

    double variance = sumSquaredDeviations;
    if (n > 0) variance /= n;
    double sampleVariance = sumSquaredDeviations;
    if (n > 1) sampleVariance /= (n - 1);
    double sigma = Math.sqrt(sampleVariance);
    result.add(sigma);

    double G1 = 0;

    if (n > 0)
    {
      double m3 = 0;
      for (int i = 0; i < n; ++i)
        m3 += (sample[i] - mean) * (sample[i] - mean) * (sample[i] - mean);
      m3 /= n;
      double g1 = m3 / Math.pow(variance, 1.5);
      G1 = g1 * Math.sqrt(n * (n - 1)) / (n - 2);
    }

    result.add(G1);
    double G2 = 0;

    if (n > 3 && variance > 0)
    {
      for (int i = 0; i < n; ++i)
        G2 += (sample[i] - mean) * (sample[i] - mean) * (sample[i] - mean)
              * (sample[i] - mean);
      G2 *= (n + 1) * n
            / ((n - 1) * (n - 2) * (n - 3) * sampleVariance * sampleVariance);
      G2 -= 3 * (n - 1) * (n - 1) / ((n - 2) * (n - 3));
    }

    result.add(G2);

    return result;
  }


  /**
    * Looks within each coreference chain to find n-grams of mention types and
    * returns their associated counts.
    *
    * @param n  The number of elements in the n-gram.
    * @return A map from n-gram names to their counts.
   **/
  public Map<String, Integer> getChainNgramCounts(int n)
  {
    if (n >= chainNgramCounts.size()) chainNgramCounts.setSize(n + 1);
    HashMap<String, Integer> result = chainNgramCounts.get(n);
    if (result != null) return result;
    result = new HashMap<String, Integer>();
    chainNgramCounts.set(n, result);

    Collection<Vector<Mention>> chains = getAllChains();

    for (Vector<Mention> chain : chains)
    {
      int N = Math.min(chain.size(), n);

      for (int i = 0; i + N - 1 < chain.size(); ++i)
      {
        String ngram = chain.get(i).getType();
        for (int j = 1; j < N; ++j)
          ngram += "-" + chain.get(i + j).getType();

        Integer count = result.get(ngram);
        if (count == null) count = 0;
        result.put(ngram, count + 1);
      }
    }

    /*
    String[] keys = result.keySet().toArray(new String[result.size()]);
    Arrays.sort(keys);
    System.err.print(" ");
    for (String key : keys)
      System.err.print(" c" + key + ":" + result.get(key));
    System.err.println();
    */

    return result;
  }


  /**
    * Records counts of how many clusters each word in the head of a mention
    * appears in.
   **/
  public HashMap<String, Integer> headWordClusterCounts()
  {
    if (headWordCounts.size() > 0) return headWordCounts;
    Vector<Vector<Mention>> chains = new Vector<Vector<Mention>>();
    chains.addAll(getAllChains());

    for (int i = 0; i < chains.size(); ++i)
    {
      for (Mention m : chains.get(i))
      {
        LinkedVector headWords = new Sentence(m.getHead()).wordSplit();

        for (int j = 0; j < headWords.size(); ++j)
        {
          Word word = (Word) headWords.get(j);

          if (word.capitalized && !headWordCounts.containsKey(word.form))
          {
            int count = 1;

            for (int k = i + 1; k < chains.size(); ++k)
            {
              boolean found = false;

              for (int l = 0; l < chains.get(k).size() && !found; ++l)
              {
                LinkedVector hw =
                  new Sentence(chains.get(k).get(l).getHead()).wordSplit();
                for (int t = 0; t < hw.size() && !found; ++t)
                  found = ((Word) hw.get(t)).form.equals(word.form);
              }

              if (found) ++count;
            }

            headWordCounts.put(word.form, count);
          }
        }
      }
    }

    return headWordCounts;
  }


  /**
    * Returns the string representations of all mentions in the document,
    * separated by spaces.
   **/
  public String mentionsToString()
  {
    String result = "";
    for (Mention[] sentence : mentions)
      for (Mention m : sentence)
        result += " " + m;
    return result.substring(1);
  }


  /**
    * Returns <code>true</code> iff this document's {@link #name} is the same
    * as the argument's.
    *
    * @param o  The object to compare with this <code>Document</code>.
   **/
  public boolean equals(Object o)
  {
    if (!(o instanceof Document)) return false;
    Document d = (Document) o;
    return d.name.equals(name);
  }


  /**
    * Returns the name of the document followed by the number of mentions in
    * each sentence in a comma separated list surrounded by parentheses.
   **/
  public String toString()
  {
    String result = name;
    HashMap<String, Integer> entities = new HashMap<String, Integer>();
    int nextID = 0;

    for (int i = 0; i < mentions.length; ++i)
    {
      result += "\n  ";

      for (int j = 0; j < mentions[i].length; ++j)
      {
        String id = mentions[i][j].getEntityID();
        Integer I = entities.get(id);

        if (I == null)
        {
          I = nextID++;
          entities.put(id, I);
        }

        result += I + ":" + mentions[i][j].getType();
        if (j + 1 < mentions[i].length) result += ", ";
      }
    }

    result += "\n  ";
    HashMap<String, Integer> counts = getNgramCounts(2);
    String[] keys = counts.keySet().toArray(new String[0]);
    Arrays.sort(keys);
    for (String key : keys)
      result += key + ": " + counts.get(key) + ", ";

    return result;
  }


  /**
    * Writes an html representation of this document to the specified
    * directory.
    *
    * @param directory  The directory to write the html files to.
   **/
  public void writeHTML(String directory)
  {
    Collection<Vector<Mention>> chains = getAllChains();

    directory += "/" + name;
    File f = new File(directory);
    f.mkdirs();
    int i = 0;

    for (Vector<Mention> chain : chains)
    {
      writeCluster(directory + "/cluster" + i + ".html", i, chain);
      ++i;
    }

    writeClusters(directory + "/all.html", chains);
  }


  /**
    * Supports the implementation of {@link #writeHTML(String)} by creating
    * the HTML document displaying the mentions in a single cluster.
    *
    * @param file       The name of the file to write to.
    * @param index      The index of this cluster.
    * @param cluster    The cluster of coreferent mentions.
   **/
  private void writeCluster(String file, int index, Vector<Mention> cluster)
  {
    PrintStream out = null;

    try { out = new PrintStream(new FileOutputStream(file)); }
    catch (Exception e)
    {
      System.err.println("Can't open " + file + " for output: " + e);
      System.exit(1);
    }

    int h = 0;
    int[] map = new int[text.length()];

    for (Mention m : cluster)
    {
      boolean allSame = true;
      for (int i = m.getExtentStart(); i <= m.getExtentEnd() && allSame; ++i)
        allSame = map[i] == map[m.getExtentStart()];
      for (int i = m.getExtentStart(); i <= m.getExtentEnd(); ++i)
        if (allSame || map[i] == 0)
          map[i] = hueToRGBInt(h * 360 / cluster.size());
      ++h;
    }

    writeHeader(out);
    out.println("<table cellpadding=10><tr valign=top><td nowrap>"
                + "<font size=+1><b>" + index + "</b></font><br><br>");
    h = 0;

    for (Mention m : cluster)
    {
      out.println(
          "<font color=\"#"
          + intToHex(hueToRGBInt(h * 360 / cluster.size())) + "\">"
          + m.getHead() + "</font><br><br>");
      ++h;
    }

    out.println("</td><td>");
    writeColoredText(out, map);
    out.println("</td></tr></table></body></html>");
  }


  /**
    * Supports the implementation of {@link #writeHTML(String)} by creating
    * the HTML document displaying all the mentions highlighted by cluster.
    *
    * @param file     The name of the file to write to.
    * @param clusters The clusters of coreferent mentions.
   **/
  public void writeClusters(String file, Collection<Vector<Mention>> clusters)
  {
    PrintStream out = null;

    try { out = new PrintStream(new FileOutputStream(file)); }
    catch (Exception e)
    {
      System.err.println("Can't open " + file + " for output: " + e);
      System.exit(1);
    }

    int h = 0;
    int[] map = new int[text.length()];
    int[][] typeCounts = new int[clusters.size()][4];

    for (Vector<Mention> cluster : clusters)
    {
      for (Mention m : cluster)
      {
        boolean allSame = true;
        for (int i = m.getExtentStart(); i <= m.getExtentEnd() && allSame;
             ++i)
          allSame = map[i] == map[m.getExtentStart()];
        for (int i = m.getExtentStart(); i <= m.getExtentEnd(); ++i)
          if (allSame || map[i] == 0)
            map[i] = hueToRGBInt(h * 360 / clusters.size());

        int t = 0;
        while (!m.getType().equals(mentionTypes[t])) ++t;
        ++typeCounts[h][t];
      }

      ++h;
    }

    writeHeader(out);
    out.print("<table cellpadding=10><tr valign=top><td nowrap bgcolor=\"#"
              + intToHex(hueToRGBInt(0))
              + "\"><font size=-2><a href=\"cluster0.html\">cluster 0"
              + "</a><br>" + typeCounts[0][0]);
    for (int i = 1; i < 4; ++i) out.print("/" + typeCounts[0][i]);
    out.println("</font></td>");
    out.println("<td rowspan=" + clusters.size() + ">");
    writeColoredText(out, map);
    out.println("</td><td rowspan=" + clusters.size() + "><pre>");

    Vector<Double> stats = getStatistics();
    out.println("words/ent.: " + stats.remove(0) + "\n");

    Double[][] data = new Double[(stats.size() - 4) / 3 + 1][4];
    for (int i = 0; i < 4; ++i) data[0][i] = stats.get(i);
    for (int i = 1; 3 * i + 1 < stats.size(); ++i)
      for (int j = 0; j < 3; ++j)
        data[i][j + 1] = stats.get(3 * i + j + 1);

    String[] columnLabels = { "Dist.", "mu", "sigma", "skew", "kurt" };
    String[] rowLabels = { "ment./ent.", "largest", "second", "third" };
    String[] table =
      TableFormat.tableFormat(columnLabels, rowLabels, data,
                              new int[]{ 3, 3, 3, 3 }, new int[]{ 0 });
    for (int i = 0; i < table.length; ++i) out.println(table[i]);

    /*
    if (predictedIDs)
    {
      double[][] results =
        new CoreferenceTester(0).testByType(null, this, false);
      double[] F1 = new double[4];

      for (int i = 0; i < 4; ++i)
        if (results[2][i] > 0)
        {
          results[0][i] /= results[2][i];
          results[1][i] /= results[2][i];
          if (results[0][i] + results[1][i] != 0)
            F1[i] = F1(results[0][i], results[1][i]);
        }

      double[][] data2 =
        CoreferenceTester.addOverall(
            new double[][]{ results[0], results[1], F1, results[2] });
      table =
        TableFormat.tableFormat(CoreferenceTester.conditionedColumnLabels,
                                CoreferenceTester.conditionedRowLabels,
                                data2, CoreferenceTester.sigDigits,
                                new int[]{ 0, 4 });

      out.println();
      for (int i = 0; i < table.length; ++i) out.println(table[i]);
    }
    */

    out.println("</pre></td></tr>");

    for (int i = 1; i < clusters.size(); ++i)
    {
      out.print(
          "<tr><td nowrap bgcolor=\"#"
          + intToHex(hueToRGBInt(i * 360 / clusters.size()))
          + "\"><font size=-2><a href=\"cluster" + i + ".html\">cluster " + i
          + "</a><br>" + typeCounts[i][0]);
      for (int j = 1; j < 4; ++j) out.print("/" + typeCounts[i][j]);
      out.println("</font></td></tr>");
    }

    out.println("</table></body></html>");
  }


  /**
    * Supports the implementation of {@link #writeHTML(String)} by writing the
    * header boilerplate HTML.
    *
    * @param out  The stream to write to.
   **/
  private void writeHeader(PrintStream out)
  {
    out.println("<html>");
    out.println("<head>");
    out.println("<title>" + name + "</title>");
    out.println("</head>");
    out.println("<body>");
  }


  /**
    * Supports the implementation of {@link #writeHTML(String)} by writing the
    * {@link #text} of this document highlighted as specified.
    *
    * @param out  The stream to write to.
    * @param map  Indicates, character by character, which colors to use for
    *             highlighting.
   **/
  private void writeColoredText(PrintStream out, int[] map)
  {
    out.println("<pre>");

    for (int i = 0; i < map.length; )
    {
      int start = i;
      while (i < map.length && map[i] == map[start]) ++i;
      if (map[start] != 0)
        out.print("<font color=\"#" + intToHex(map[start]) + "\">");
      out.print(text.substring(start, i));
      if (map[start] != 0) out.print("</font>");
    }

    out.println("</pre>");
  }


  /** Data used by {@link #intToHex(int)} to determine hexidecimal digits. */
  private static final String digits = "0123456789ABCDEF";
  /**
    * Converts an integer into a 6 digit hexidecimal integer, zero-padded from
    * the left.  If the specified integer is greater than 16<sup>6</sup>, only
    * the 6 least significant digits are returned.
    *
    * @param I  The integer to convert.
    * @return The hexidecimal conversion.
   **/
  private static String intToHex(int I)
  {
    String result = "";

    for (int i = 0; i < 6; ++i)
    {
      result = digits.charAt(I % 16) + result;
      I >>= 4;
    }

    return result;
  }


  /**
    * Converts a hue expressed as an angle <i>0 &lt;= hue &lt; 360</i> into an
    * integer whose hexidecimal representation is an RGB color.
    *
    * @param hue  The fully saturated, fully bright color expressed as an
    *             angle in degrees.
    * @return The converted RGB integer.
   **/
  private static int hueToRGBInt(int hue)
  {
    return hsvToRGBInt((hue % 360) / 360.0, 1, 1);
  }


  /** Data supporting {@link #hsvToRGBInt(double,double,double)}. */
  private static final int[][] hsvData =
    new int[][]
    { //  v       p      q      t
      { 65536,      1,     0,   256 },
      {   256,      1, 65536,     0 },
      {   256,  65536,     0,     1 },
      {     1,  65536,   256,     0 },
      {     1,    256,     0, 65536 },
      { 65536,    256,     1,     0 }
    };

  /**
    * Converts a color represented in HSV (three floating point values between
    * 0 and 1 denoting hue, saturation, and value (i.e., brightness)) into an
    * integer whose hexidecimal representation is a 6-digit RGB color.
    *
    * @param h  <i>0 &lt;= hue &lt; 1</i>
    * @param s  <i>0 &lt;= saturation &lt; 1</i>
    * @param v  <i>0 &lt;= value &lt; 1</i> (or "brightness").
    * @return The converted RGB integer.
   */
  private static int hsvToRGBInt(double h, double s, double v)
  {
    int hh = (int) (h * 6);
    double f = h * 6 - hh;
    double p = v * (1 - s);
    double q = v * (1 - f * s);
    double t = v * (1 - (1 - f) * s);
    return hsvData[hh][0] * ((int) (v * 255))
           + hsvData[hh][1] * ((int) (p * 255))
           + hsvData[hh][2] * ((int) (q * 255))
           + hsvData[hh][3] * ((int) (t * 255));
  }


  /**
    * Itermediary containing the raw data partaining to a mention.
   **/
  public static class MentionData implements Cloneable
  {
    /**
      * The ID of the entity, which should be available only if coreferences
      * have been resolved.
     **/
    public String entityID;
    /**
      * The type of the entity, which is <code>"PER"</code>,
      * <code>"ORG"</code>, <code>"LOC"</code>, <code>"GPE"</code>,
      * <code>"FAC"</code>, <code>"VEH"</code>, or <code>"WEA"</code>.
     **/
    public String entityType;
    /** The ID of the mention. */
    public String mentionID;
    /**
      * The type of the mention, which is <code>"NAM"</code>,
      * <code>"NOM"</code>, <code>"PRO"</code>, or <code>"PRE"</code>.
     **/
    public String type;
    /** The head, as recorded in the <code>.apf.xml</code> file. */
    public String head;
    /** The starting character index in the document of the mention's head. */
    public int headStart;
    /** The ending character index in the document of the mention's head. */
    public int headEnd;
    /** The extent, as recorded in the <code>.apf.xml</code> file. */
    public String extent;
    /**
      * The starting character index in the document of the mention's extent.
     **/
    public int extentStart;
    /** The ending character index in the document of the mention's extent. */
    public int extentEnd;


    /**
      * Initializing constructor.
      *
      * @param id The ID of the entity.
      * @param et The entity type.
      * @param mi The ID of the mention.
      * @param t  The type.
      * @param h  The head.
      * @param hs The start index of the head.
      * @param he The end index of the head.
      * @param e  The extent.
      * @param es The start index of the extent.
      * @param ee The end index of the extent.
     **/
    public MentionData(String id, String et, String mi, String t, String h,
                       int hs, int he, String e, int es, int ee)
    {
      entityID = id;
      entityType = et;
      mentionID = mi;
      type = t;
      head = h;
      headStart = hs;
      headEnd = he;
      extent = e;
      extentStart = es;
      extentEnd = ee;
    }


    /** Allows cloning. */
    public Object clone()
    {
      Object clone = null;

      try { clone = super.clone(); }
      catch (Exception e)
      {
        System.err.println("Cloning exception: " + e);
        System.exit(1);
      }

      return clone;
    }
  }


  /**
    * Represents the mention of any entity in the text.
   **/
  public class Mention implements Comparable<Mention>, Cloneable
  {
    /** Vital info about the mention. */
    private MentionData data;
    /** The index of this mention in the document. */
    private int indexInDocument;
    /** The index of the sentence containing this mention in the document. */
    private int sentenceIndex;
    /** The index of this mention within the sentence containing it. */
    private int indexInSentence;
    /**
      * The number of words appearing before this mention's head word in the
      * document.
     **/
    private int wordIndexInDocument;
    /**
      * The number of words appearing before this mention's head word in its
      * sentence.
     **/
    private int wordIndexInSentence;
    /**
      * The number of words in the sentence appearing before the first word of
      * this mention's extent.
     **/
    private int extentWordStart;
    /** The number of words in this mention's extent. */
    private int wordLength;
    /** Indexes may only be set once; this keeps track of if they have. */
    private boolean indexesSet;


    /**
      * Initializing constructor.
      *
      * @param d  The vital information about the mention.
     **/
    public Mention(MentionData d) { data = d; }

    /**
      * Copies the data in the {@link #data} field, but nothing else.
      *
      * @param m  The mention to clone.
     **/
    public Mention(Mention m) { data = (MentionData) m.data.clone(); }


    /**
      * Sets the index variables of the mention.
      *
      * @param i  The new value for {@link #sentenceIndex}.
      * @param j  The new value for {@link #indexInSentence}.
      * @param d  The new value for {@link #indexInDocument}.
      * @param wj The new value for {@link #wordIndexInSentence}.
      * @param wd The new value for {@link #wordIndexInDocument}.
      * @param o  An amount to add to the character indexes.
     **/
    public void setIndexes(int i, int j, int d, int wj, int wd, int o)
    {
      if (indexesSet)
      {
        System.err.println("ERROR: Tried setting indexes of '" + toString()
                           + "' in document " + name + " again.");
        System.exit(1);
      }

      sentenceIndex = i;
      indexInSentence = j;
      indexInDocument = d;
      wordIndexInSentence = wj;
      wordIndexInDocument = wd;
      indexesSet = true;
      data.extentStart += o;
      data.extentEnd += o;
      data.headStart += o;
      data.headEnd += o;

      for (extentWordStart = wj;
           extentWordStart >= 0
           && ((Word) sentences[i].get(extentWordStart)).start + offset
              > data.extentStart;
           --extentWordStart);

      if (extentWordStart == -1
          || ((Word) sentences[i].get(extentWordStart)).start + offset
             != data.extentStart)
      {
        if (extentWordStart == -1)
        {
          System.err.println(
              "Mention extends over the beginning of its sentence.");
          System.err.println("Document " + name + ", sentence "
                             + sentenceIndex + ", mention '" + toString()
                             + "'");
          System.exit(1);
        }

        Word w = (Word) sentences[i].get(extentWordStart);
        String mentionPart = null;

        mentionPart =
          w.form.substring(data.extentStart - w.start - offset,
                           Math.min(w.form.length(),
                                    data.extentEnd + 1 - w.start - offset));

        if (!data.extent.startsWith(mentionPart))
        {
          System.err.println("Word start indexes don't line up.");
          System.err.println("Document " + name + ", sentence "
                             + sentenceIndex + ", mention '" + toString()
                             + "'");
          System.err.println("Mention part of word: '" + mentionPart + "'");
          System.err.println("Extent of mention: '" + data.extent + "'");
          System.exit(1);
        }
      }

      for (wordLength = 1;
           extentWordStart + wordLength - 1 < sentences[i].size()
           && ((Word) sentences[i].get(extentWordStart + wordLength - 1)).end
                + offset
              < data.extentEnd;
           ++wordLength);

      if (extentWordStart + wordLength > sentences[i].size()) --wordLength;
      else if (((Word) sentences[i].get(extentWordStart + wordLength - 1)).end
                 + offset
               != data.extentEnd)
      {
        Word w = (Word) sentences[i].get(extentWordStart + wordLength - 1);
        String mentionPart =
          w.form.substring(Math.max(0, data.extentStart - w.start - offset),
                           data.extentEnd + 1 - w.start - offset);

        if (!data.extent.endsWith(mentionPart))
        {
          System.err.println("Word end indexes don't line up.");
          System.err.println("Document " + name + ", sentence "
                             + sentenceIndex + ", mention '" + toString()
                             + "'");
          System.err.println("Mention part of word: '" + mentionPart + "'");
          System.err.println("Extent of mention: '" + data.extent + "'");
          System.exit(1);
        }
      }
    }


    /** Sets the value of {@link #entityID}. */
    public void setEntityID(String id)
    {
      data.entityID = id;
      clearCaches();
    }


    /** Retrieve the containing document. */
    public Document getDocument() { return Document.this; }
    /** Retrieve the value of {@link MentionData#entityID}. */
    public String getEntityID() { return data.entityID; }
    /** Retrieve the value of {@link MentionData#entityType}. */
    public String getEntityType() { return data.entityType; }
    /** Retrieve the value of {@link MentionData#mentionID}. */
    public String getMentionID() { return data.mentionID; }
    /** Retrieve the value of {@link MentionData#type}. */
    public String getType() { return data.type; }
    /** Retrieve the value of {@link MentionData#head}. */
    public String getHead() { return data.head; }
    /** Retrieve the value of {@link MentionData#headStart}. */
    public int getHeadStart() { return data.headStart; }
    /** Retrieve the value of {@link MentionData#headEnd}. */
    public int getHeadEnd() { return data.headEnd; }
    /** Retrieve the value of {@link MentionData#extent}. */
    public String getExtent() { return data.extent; }
    /** Retrieve the value of {@link MentionData#extentStart}. */
    public int getExtentStart() { return data.extentStart; }
    /** Retrieve the value of {@link MentionData#extentEnd}. */
    public int getExtentEnd() { return data.extentEnd; }
    /** Retrieve the value of {@link #indexInDocument}. */
    public int getIndexInDocument() { return indexInDocument; }
    /** Retrieve the value of {@link #sentenceIndex}. */
    public int getSentenceIndex() { return sentenceIndex; }
    /** Retrieve the value of {@link #indexInSentence}. */
    public int getIndexInSentence() { return indexInSentence; }
    /** Retrieve the value of {@link #wordIndexInDocument}. */
    public int getWordIndexInDocument() { return wordIndexInDocument; }
    /** Retrieve the value of {@link #wordIndexInSentence}. */
    public int getWordIndexInSentence() { return wordIndexInSentence; }
    /** Retrieve the value of {@link #extentWordStart}. */
    public int getExtentWordStart() { return extentWordStart; }
    /** Retrieve the value of {@link #wordLength}. */
    public int getWordLength() { return wordLength; }


    /**
      * Uses a simple heuristic which should have high precision and may have
      * low recall to determine if this mention is the subject of its
      * sentence.
      *
      * @return <code>true</code> if the heuristic determines that this
      *         mention is the subject of its sentence.
     **/
    public boolean isSubject()
    {
      if (extentWordStart == 0) return true;
      int comma = -1;
      Word before = (Word) sentences[sentenceIndex].get(extentWordStart - 1);

      if (before.form.equals(",")) comma = extentWordStart - 1;
      else if ((before.form.equals("a") || before.form.equals("the"))
               && before.previous != null)
      {
        before = (Word) before.previous;
        if (before.form.equals(",")) comma = extentWordStart - 2;
      }

      if (comma == -1) return false;
      for (int i = 0; i < comma; ++i)
        if (((Word) sentences[sentenceIndex].get(i)).form.equals(","))
          return false;
      return true;
    }


    /**
      * Determines whether this mention is adjacent to, contains, or is
      * contained in the argument mention.
      *
      * @param m  The other mention involved.
      * @return <code>true</code> iff this mention is adjacent to, contains,
      *         or is contained in the argument mention.
     **/
    public boolean adjacentTo(Mention m)
    {
      int s1 = extentWordStart;
      int e1 = s1 + wordLength;
      int s2 = m.extentWordStart;
      int e2 = s2 + m.wordLength;

      if (s2 < s1)
      {
        int t = s1;
        s1 = s2;
        s2 = t;

        t = e1;
        e1 = e2;
        e2 = t;
      }

      return s2 <= e1;
    }


    /**
      * Two <code>Mention</code>s are compared by comparing the starts of
      * their extents.
      *
      * @param m  The mention to compare with.
      * @return   An integer less than, equal to, or greater than 0 if
      *           <code>o</code> is not a <code>Mention</code> or has an
      *           extent that starts after this <code>Mention</code>'s, the
      *           two <code>Mention</code>s' extents start at the same offset,
      *           or <code>o</code>'s extent starts before this
      *           <code>Mention</code>'s, respectively.
     **/
    public int compareTo(Mention m)
    {
      if (data.extentStart != m.data.extentStart)
        return data.extentStart - m.data.extentStart;
      if (data.extentEnd != m.data.extentEnd)
        return data.extentEnd - m.data.extentEnd;
      if (data.headStart != m.data.headStart)
        return data.headStart - m.data.headStart;
      if (data.headEnd != m.data.headEnd)
        return data.headEnd - m.data.headEnd;
      return data.mentionID.compareTo(m.data.mentionID);
    }


    /**
      * The string representation of a <code>Mention</code> is its head
      * followed by the the mention ID surrounded by parentheses.
      *
      * @return The string representation of this <code>Mention</code> as
      *         described above.
     **/
    public String toString()
    {
      return data.mentionID + "::" + data.head + "(" + data.extentStart
             + ", " + data.extentEnd + ")";
    }


    public int hashCode()
    {
      return data.headStart + data.headEnd;
    }


    public boolean equals(Object o)
    {
      Mention m = (Mention) o;
      return m.data.headStart == data.headStart
             && m.data.headEnd == data.headEnd
             && m.data.extent.equals(data.extent)
             && m.data.type.equals(data.type);
    }


    /** Allows cloning. */
    public Object clone()
    {
      Object clone = null;

      try { clone = super.clone(); }
      catch (Exception e)
      {
        System.err.println("Cloning exception: " + e);
        System.exit(1);
      }

      return clone;
    }
  }
}

