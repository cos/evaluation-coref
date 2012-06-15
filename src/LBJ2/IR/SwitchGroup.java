package LBJ2.IR;

import LBJ2.Pass;


/**
  * Represents a list of statements labeled by one or more
  * <code>SwitchLabel</code>s.
  *
  * @author Nick Rizzolo
 **/
public class SwitchGroup extends ASTNode
{
  /** (&not;&oslash;) The list of labels labeling this group. */
  public SwitchLabelList labels;
  /** (&not;&oslash;) The list of statements in the group. */
  public StatementList statements;


  /**
    * Full constructor.  Line and byte offset information are taken from the
    * labels.
    *
    * @param l  The list of labels.
    * @param s  The list of statements.
   **/
  public SwitchGroup(SwitchLabelList l, StatementList s)
  {
    super(l.line, l.byteOffset);
    labels = l;
    statements = s;
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
    I.children[0] = labels;
    I.children[1] = statements;
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
    return new SwitchGroup((SwitchLabelList) labels.clone(),
                           (StatementList) statements.clone());
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
    labels.write(buffer);
    buffer.append(" ");
    statements.write(buffer);
  }
}

