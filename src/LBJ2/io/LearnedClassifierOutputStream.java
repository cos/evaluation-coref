package LBJ2.io;

import java.io.*;


/**
  * This stream assumes it will be sent characters that are not <code>"</code>
  * (double quote), rearranging those characters by adding spacing, double
  * quotes, addition symbols, and commas so that the result is a
  * <code>String</code> expression list suitable for inclusion in a Java
  * source file.  It was created for use by the LBJ2 compiler when generating
  * the source file that represents a trained classifier.
  *
  * @see HexInputStream
  * @author Nick Rizzolo
 **/
public class LearnedClassifierOutputStream extends OutputStream
{
  /**
    * The <code>PrintStream</code> to which converted output should be sent.
   **/
  private PrintStream out;
  /** Internal buffer for data not yet written. */
  private StringBuffer buffer;
  /** Keeps track of how many lines have been written by this stream. */
  private int lines;


  /**
    * Initializes this stream with a <code>PrintStream</code>.
    *
    * @param o  The <code>PrintStream</code> to which converted output should
    *           be sent.
   **/
  public LearnedClassifierOutputStream(PrintStream o)
  {
    out = o;
    buffer = new StringBuffer();
    lines = 0;
  }


  /**
    * All writes go through this method which interacts with the buffer,
    * deciding what bytes to write when.
   **/
  private void writeIfFull()
  {
    while (buffer.length() >= 70)
    {
      String line = buffer.substring(0, 70);
      buffer.delete(0, 70);

      char op = '+';
      if (++lines == 1) op = ' ';
      else if (lines % 936 == 1) op = ',';

      out.println("    " + op + " \"" + line + "\"");
    }
  }


  /**
    * Writes the specified byte to this output stream.  The general contract
    * for <code>write</code> is that one byte is written to the output stream.
    * The byte to be written is the eight low-order bits of the argument
    * <code>b</code>.  The 24 high-order bits of <code>b</code> are ignored.
    *
    * @param b  The byte to be written.
   **/
  public void write(int b) throws IOException
  {
    buffer.append((char) b);
    writeIfFull();
  }


  /**
    * Writes <code>b.length</code> bytes from the specified byte array to this
    * output stream.  The general contract for <code>write(b)</code> is that
    * it should have exactly the same effect as the call <code>write(b, 0,
    * b.length)</code>.
    *
    * @param b  The bytes to be written.
   **/
  public void write(byte[] b) throws IOException
  {
    for (int i = 0; i < b.length; ++i) buffer.append((char) b[i]);
    writeIfFull();
  }


  /**
    * Writes <code>len</code> bytes from the specified byte array starting at
    * offset <code>off</code> to this output stream.  The general contract for
    * <code>write(b, off, len)</code> is that some of the bytes in the array
    * <code>b</code> are written to the output stream in order; element
    * <code>b[off]</code> is the first byte written and
    * <code>b[off+len-1]</code> is the last byte written by this operation.
    * <br><br>
    *
    * If <code>b</code> is <code>null</code>, a
    * <code>NullPointerException</code> is thrown. <br><br>
    *
    * If <code>off</code> is negative, or <code>len</code> is negative, or
    * <code>off+len</code> is greater than the length of the array
    * <code>b</code>, then an <code>IndexOutOfBoundsException</code> is
    * thrown.
    *
    * @param b    A buffer containing the bytes to be written.
    * @param off  The offset of the first byte to be written.
    * @param len  The amount of bytes to be written.
   **/
  public void write(byte[] b, int off, int len) throws IOException
  {
    for (int i = off; i < off + len; ++i) buffer.append((char) b[i]);
    writeIfFull();
  }


  /**
    * Flushes this output stream and forces any buffered output bytes to be
    * written out.  The general contract of <code>flush</code> is that calling
    * it is an indication that, if any bytes previously written have been
    * buffered by the implementation of the output stream, such bytes should
    * immediately be written to their intended destination.
   **/
  public void flush() throws IOException
  {
    writeIfFull();

    if (buffer.length() > 0)
    {
      char op = '+';
      if (++lines == 1) op = ' ';
      else if (lines % 936 == 1) op = ',';

      out.println("    " + op + " \"" + buffer + "\"");

      buffer.delete(0, buffer.length());
    }

    out.flush();
  }


  /**
    * Closes this output stream and releases any system resources associated
    * with this stream.  The general contract of <code>close</code> is that it
    * closes the output stream.  A closed stream cannot perform output
    * operations and cannot be reopened.
   **/
  public void close() throws IOException { flush(); }
}

