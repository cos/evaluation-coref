package LBJ2.nlp;

import LBJ2.parse.*;


/**
  * Use this parser to return <code>Word</code> objects given file names of
  * POS bracket form files to parse.  These files are expected to have one
  * sentence per line, and the format of each line is as follows: <br><br>
  *
  * <code>(pos1 spelling1) (pos2 spelling2) ... (posN spellingN)</code>
  * <br><br>
  *
  * It is also expected that there will be exactly one space between a part of
  * speech and the corresponding spelling and between a closing parenthesis
  * and an opening parenthesis.
  *
  * @deprecated As of LBJ release 2.0.4, the functionality of this class has
  *             been superceded by the {@link LBJ2.parse.ChildrenFromVectors}
  *             parser used in conjunction with {@link POSBracketToVector}.
  * @author     Nick Rizzolo
 **/
public class POSBracketToWord extends POSBracketToVector
{
  /**
    * The next word to return, or <code>null</code> if we need a new sentence.
   **/
  private Word currentWord;


  /**
    * Adds the given file name to the queue.
    *
    * @param file The file name to add to the queue.
   **/
  public POSBracketToWord(String file) { super(file); }


  /**
    * Retrieves the next sentence from the files being parsed.
    *
    * @return A <code>LinkedVector</code> representation of the next sentence.
   **/
  public Object next()
  {
    if (currentWord == null)
    {
      LinkedVector vector = (LinkedVector) super.next();
      if (vector != null) currentWord = (Word) vector.get(0);
    }

    Word result = currentWord;
    if (currentWord != null) currentWord = (Word) currentWord.next;
    return result;
  }
}

