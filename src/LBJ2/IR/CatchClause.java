package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a catch clause on a try statement.
  *
  * @author Nick Rizzolo
 **/
public class CatchClause extends ASTNode
{
  /** (&not;&oslash;) The catch's input specification */
  public Argument argument;
  /** (&not;&oslash;) The code to execute when an exception is caught. */
  public Block block;


  /**
    * Full constructor.
    *
    * @param a          The catch's input specification.
    * @param b          The code to execute when an exception is caught.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public CatchClause(Argument a, Block b, int line, int byteOffset)
  {
    super(line, byteOffset);
    argument = a;
    block = b;
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
    ASTNodeIterator I = new ASTNodeIterator(2);
    I.children[0] = argument;
    I.children[1] = block;
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
    return new CatchClause((Argument) argument.clone(), (Block) block.clone(),
                           -1, -1);
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
    buffer.append("catch (");
    argument.write(buffer);
    buffer.append(") ");
    block.write(buffer);
  }
}

