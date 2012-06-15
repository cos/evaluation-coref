package LBJ2.io;

import java.io.*;


/**
  * This class allows an application to create an input stream in which the
  * bytes read are supplied by the contents of a string.
  *
  * <p>
  * Only strings that contain only printable, ASCII characters will be
  * handled correctly by this class.  If the string contains any other
  * characters, the behavior of this class is undefined.
  *
  * @author Nick Rizzolo
 **/
public class StringInputStream extends InputStream
{
  /** The string from which bytes are read. */
  protected String buffer;

  /** The index of the next character to read from the input stream buffer. */
  protected int position;

  /** The number of valid characters in the input stream buffer. */
  protected int size;


  /**
    * Creates a <code>StringInputStream</code> to read data from the specified
    * string.
    *
    * @param s  The underlying input buffer.
   **/
  public StringInputStream(String s)
  {
    buffer = s;
    position = 0;
    size = s.length();
  }


  /**
    * Reads the next byte of data from this input stream.  The value of the
    * next <code>char</code> in the string is returned as an <code>int</code>
    * in the range <code>0</code> to <code>255</code>.  If no
    * <code>char</code> is available because the end of the string has been
    * reached, the value <code>-1</code> is returned.
    *
    * <p>
    * The <code>read</code> method of <code>StringInputStream</code> will not
    * block.  It simply converts the next character available to an
    * <code>int</code> and returns that <code>int</code>.
    *
    * @return The next byte of data, or <code>-1</code> if the end of the
    *         stream is reached.
   **/
  public int read()
  {
    return (position < size) ? (int) buffer.charAt(position++) : -1;
  }


  /**
    * Reads up to <code>len char</code>s from this input stream into an array
    * of bytes.
    *
    * <p>
    * The <code>read</code> method of <code>StringInputStream</code> cannot
    * block.  It simply casts the characters in this input stream's buffer
    * into <code>byte</code>s, storing them in the byte array argument.
    *
    * @param b    The buffer into which the data is read.
    * @param off  The first index in <code>b</code> in which bytes should be
    *             stored.
    * @param len  The maximum number of bytes read.
    * @return     The total number of bytes read into the buffer, or
    *             <code>-1</code> if there is no more data because the end of
    *             the stream has been reached.
   **/
  public int read(byte b[], int off, int len)
  {
    if (position >= size) return -1;
    if (len < 1) return 0;

    if (position + len > size) len = size - position;
    int result = len;
    for (; len > 0; --len) b[off++] = (byte) buffer.charAt(position++);
    return result;
  }


  /**
    * Skips <code>n</code> bytes of input from this input stream.  Fewer bytes
    * might be skipped if the end of the input stream is reached.
    *
    * @param n  The number of bytes to be skipped.
    * @return   The actual number of bytes skipped.
   **/
  public long skip(long n)
  {
    if (n <= 0) return 0;
    if (n > size - position) n = size - position;
    position += n;
    return n;
  }


  /**
    * Returns the number of bytes that can be read from the input stream
    * without blocking.
    *
    * @return The value of <code>size - position</code>, which is the number
    *         of bytes remaining to be read from the input buffer.
   **/
  public int available() { return size - position; }


  /**
    * Resets the input stream to begin reading from the first character of
    * this input stream's underlying buffer.
   **/
  public synchronized void reset() { position = 0; }
}

