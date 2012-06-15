package LBJ2.IR;

import LBJ2.Pass;


/**
  * An expression statement is a statement composed only of a single
  * expression, as opposed to a statement involving control flow.
  *
  * @author Nick Rizzolo
 **/
public class ExpressionStatement extends Statement
{
  /** (&not;&oslash;) The expression being used as a statement. */
  public StatementExpression expression;


  /**
    * Initializing constructor.  Line and byte offset information are taken
    * from the expression's representation.
    *
    * @param e  The expression being used as a statement.
   **/
  public ExpressionStatement(StatementExpression e)
  {
    super(e.line, e.byteOffset);
    expression = e;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator()
  {
    ASTNodeIterator I = new ASTNodeIterator(1);
    I.children[0] = expression;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone()
  {
    return new ExpressionStatement((StatementExpression) expression.clone());
  }


  /**
    * Ensures that the correct <code>run()</code> method is called for this
    * type of node.
    *
    * @param pass The pass whose <code>run()</code> method should be called.
   **/
  public void runPass(Pass pass) { pass.run(this); }


  /**
    * Writes a string representation of this <code>ASTNode</code> to the
    * specified buffer.  The representation written is parsable by the LBJ2
    * compiler, but not very readable.
    *
    * @param buffer The buffer to write to.
   **/
  public void write(StringBuffer buffer)
  {
    expression.write(buffer);
    buffer.append(";");
  }
}

