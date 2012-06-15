package LBJ2.nlp;

import LBJ2.parse.*;


/**
  * Use this class to represent a natural language document.
  * <code>SentenceSplitter</code> and <code>Sentence.wordSplit()</code> are
  * used to represent the text of the document internally as a collection of
  * vectors of words.  As such, the text of the document is assumed plain,
  * i.e. there should not be any mark-up.
  *
  * @author Nick Rizzolo
 **/
public class NLDocument extends LinkedVector
{
  /**
    * This constructor takes the entire text of the document in a String array
    * as input and initializes the representation.
    *
    * @param text The entire text of the document.  Each element of this array
    *             should represent a line of input without any line
    *             termination characters.
   **/
  public NLDocument(String[] text) { this(null, text); }


  /**
    * This constructor takes the entire text of the document in a String array
    * as input and initializes the representation.
    *
    * @param p    The previous child in the parent vector.
    * @param text The entire text of the document.  Each element of this array
    *             should represent a line of input without any line
    *             termination characters.
   **/
  public NLDocument(NLDocument p, String[] text)
  {
    super(p);

    SentenceSplitter splitter = new SentenceSplitter(text);

    Sentence[] rawSentences = splitter.splitAll();
    for (int i = 0; i < rawSentences.length; ++i)
      add(rawSentences[i].wordSplit());
  }
}

