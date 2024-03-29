package LBJ2.nlp.seg;

import LBJ2.parse.*;
import LBJ2.nlp.*;


/**
  * This parser takes the {@link LBJ2.nlp.Word}s in the representation created
  * by another {@link LBJ2.parse.Parser} and creates a new representation
  * consisting of {@link Token}s.  The input parser is actually expected to
  * return a {@link LBJ2.parse.LinkedVector} populated by
  * {@link LBJ2.nlp.Word}s with each call to {@link LBJ2.parse.Parser#next()}.
  * The {@link Token}s returned by calls to this class's {@link #next()}
  * method are also contained in {@link LBJ2.parse.LinkedVector}s representing
  * sentences which are accessible via the
  * {@link LBJ2.parse.LinkedVector#parent} field.
  *
  * @author Nick Rizzolo
 **/
public class PlainToTokenParser implements Parser
{
  /**
    * A parser creating a representation consisting of {@link LBJ2.nlp.Word}s.
   **/
  protected Parser parser;
  /** The next token to return. */
  protected Token next;


  /**
    * The only constructor.
    *
    * @param p  A parser creating a representation consisting of
    *           {@link LBJ2.nlp.Word}s.
   **/
  public PlainToTokenParser(Parser p) { parser = p; }


  /**
    * This method returns {@link Token}s until the input is exhausted, at
    * which point it returns <code>null</code>.
   **/
  public Object next()
  {
    while (next == null)
    {
      LinkedVector words = (LinkedVector) parser.next();
      if (words == null) return null;
      Word w = (Word) words.get(0);
      Token t = new Token(w, null, null);

      for (w = (Word) w.next; w != null; w = (Word) w.next)
      {
        t.next = new Token(w, t, null);
        t = (Token) t.next;
      }

      LinkedVector tokens = new LinkedVector(t);
      next = (Token) tokens.get(0);
    }

    Token result = next;
    next = (Token) next.next;
    return result;
  }


  /** Sets this parser back to the beginning of the raw data. */
  public void reset()
  {
    parser.reset();
    next = null;
  }
}

