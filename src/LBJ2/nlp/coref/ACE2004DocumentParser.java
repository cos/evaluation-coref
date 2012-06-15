package LBJ2.nlp.coref;

import java.util.*;
import java.io.*;
import LBJ2.parse.*;
import org.xml.sax.*;
import javax.xml.parsers.*;


/**
  * Starting from a list of <code>.apf.xml</code> filenames, this parser
  * creates {@link Document} objects representing the documents in the files
  * in that list.  It is assumed that a <code>.apf.xml</code> file's
  * corresponding <code>.sgm</code> file is in the same directory.
 **/
public class ACE2004DocumentParser extends LineByLine
{
  /** An instance of the handler used to parse <code>.apf.xml</code> files. */
  protected APFHandler handler = new APFHandler();
  /**
    * {@link #readLine()} gets its data from here instead of from a file when
    * the <code>String[]</code> constructor is used.
   **/
  protected String[] filenames;
  /** The next index to read from {@link #filenames}. */
  protected int index;
  /** A root directory within which the named files can be found. */
  protected String rootDir;


  /**
    * Initializing constructor.
    *
    * @param file The name of a file containing <code>.apf.xml</code> file
    *             names, one per line.
   **/
  public ACE2004DocumentParser(String file)
  {
    super(file);
    setRoot(file);
  }

  /**
    * Alternate constructor.
    *
    * @param f  The names of all files to read in an array.
   **/
  public ACE2004DocumentParser(String[] f) { this(f, "."); }

  /**
    * Alternate constructor.
    *
    * @param f    The names of all files to read in an array.
    * @param root The root directory within which the named files can be
    *             found.
   **/
  public ACE2004DocumentParser(String[] f, String root)
  {
    filenames = f;
    setRoot(root + File.separator);
  }


  /** Sets the value of {@link #rootDir}. */
  public void setRoot(String root)
  {
    int lastSlash = root.lastIndexOf(File.separatorChar);
    while (lastSlash > 0 && root.charAt(lastSlash - 1) == File.separatorChar) --lastSlash;
    if (lastSlash != -1) rootDir = root.substring(0, lastSlash);
    else rootDir = ".";
  }


  /**
    * Diverts the reading of a line to the reading of elements of
    * {@link #filenames} when {@link #filenames} is non-<code>null</code>.
    *
    * @return The next filename or <code>null</code> if there are no more.
   **/
  protected String readLine()
  {
    String result = null;

    do
    {
      if (filenames != null)
      {
        if (index < filenames.length) result = filenames[index++];
        else result = null;
      }
      else result = super.readLine();
    } while (result != null && (result.equals("") || result.startsWith("#")));

    return result;
  }


  /** Returns the next {@link Document} object. */
  public Object next()
  {
    String apfFile = readLine();
    while (apfFile != null && apfFile.equals("")) apfFile = readLine();
    if (apfFile == null) return null;
    String path = apfFile.substring(0, apfFile.lastIndexOf(File.separatorChar));
    apfFile = rootDir + File.separator + apfFile;

    /*
    assert apfFile.endsWith(".apf.xml")
           : "'" + apfFile
             + "' does not have the expected .apf.xml extension.";
             */

    try
    {
      SAXParserFactory.newInstance().newSAXParser()
        .parse(new File(apfFile), handler);
    }
    catch (SAXParseException e)
    {
      System.err.println(apfFile + ":");
      System.err.println(e);
    }
    catch (Throwable t)
    {
      t.printStackTrace();
      System.exit(1);
    }

    String sgmFile = apfFile.substring(0, apfFile.length() - 7) + "sgm";
    return parseSGM(sgmFile, path, handler);
  }


  /**
    * Reads a <code>.sgm</code> file.  The name of the document is retrieved
    * from in between the <code>&lt;DOCNO&gt;&lt;/DOCNO&gt;</code> tags, and
    * the full text of the document is retrieved from in between the
    * <code>&lt;TEXT&gt;&lt;/TEXT&gt;</code> tags.
    *
    * @param file     The name of the <code>.sgm</code> file.
    * @param path     The path within the ACE distribution where the file can
    *                 be found.
    * @param handler  The object that handled the parsing of the
    *                 <code>.apf.xml</code> file.
    * @return An initialized <code>Document</code> object.
   **/
  public Document parseSGM(String file, String path, APFHandler handler)
  {
    LineByLine in =
      new LineByLine(file) { public Object next() { return readLine(); } };
    int offset = 0;
    String text = "";
    boolean foundTextStart = false;
    boolean foundTextEnd = false;
    boolean inTag = false;

    for (String line = (String) in.next(); line != null && !foundTextEnd;
         line = (String) in.next())
    {
      for (int i = 0; i < line.length() && !foundTextEnd; ++i)
      {
        if (!inTag && line.charAt(i) == '<')
        {
          if (!foundTextStart && line.substring(i).startsWith("<TEXT>"))
          {
            foundTextStart = true;
            i += 5;
            continue;
          }

          if (foundTextStart && line.substring(i).startsWith("</TEXT>"))
          {
            foundTextEnd = true;
            continue;
          }

          inTag = true;
        }

        if (!inTag)
        {
          text += line.substring(i, i + 1);
          if (!foundTextStart) ++offset;
        }

        if (inTag && line.charAt(i) == '>') inTag = false;
      }

      if (!inTag && !foundTextEnd)
      {
        text += "\n";
        if (!foundTextStart) ++offset;
      }
    }

    return
      new Document(handler.getDocumentID(), path, text, offset,
                   handler.getMentions());
  }


  public void reset()
  {
    if (filenames == null) super.reset();
    else index = 0;
  }


  /**
    * An instance of this class handles the tags and data encountered by an
    * XML parser parsing a <code>.apf.xml</code> file by collecting the
    * mentions encoded in that data.
   **/
  protected static class APFHandler extends org.xml.sax.helpers.DefaultHandler
  {
    /** Contains the mentions parsed out of XML file. */
    protected LinkedList<Document.MentionData> list;
    /** The ID of the document. */
    protected String documentID;
    /** The ID of the current entity. */
    protected String entityID;
    /** The type of the current entity. */
    protected String entityType;
    /** The ID of the current mention. */
    protected String mentionID;
    /** The type of the current mention. */
    protected String type;
    /** The current mention's head, as found in the XML file. */
    protected String head;
    /** The starting index of the current mention's head. */
    protected int headStart;
    /** The ending index of the current mention's head. */
    protected int headEnd;
    /** The current mention's extent, as found in the XML file. */
    protected String extent;
    /** The starting index of the current mention's extent. */
    protected int extentStart;
    /** The ending index of the current mention's extent. */
    protected int extentEnd;
    /** Whether we're currently parsing the current mention's head. */
    protected boolean parsingHead;
    /** Whether we're currently parsing the current mention's extent. */
    protected boolean parsingExtent;
    /** Whether we're currently inside a character sequence. */
    protected boolean inCharseq;


    /** Default constructor. */
    public APFHandler() { }


    /** Retrieves the mentions collected during parsing. */
    public LinkedList<Document.MentionData> getMentions() { return list; }
    /** Retrieves the document ID. */
    public String getDocumentID() { return documentID; }


    /** Simply instantiate the list of mentions. */
    public void startDocument() throws SAXException
    {
      list = new LinkedList<Document.MentionData>();
    }


    /** Handles each XML tag as it opens. */
    public void startElement(String namespaceURI, String localName,
                             String qualifiedName, Attributes attributes)
      throws SAXException
    {
      String elementName = localName.equals("") ? qualifiedName : localName;

      if (elementName.equals("document"))
        documentID = attributes.getValue("DOCID");
      else if (elementName.equals("entity"))
      {
        entityID = attributes.getValue("ID");
        entityType = attributes.getValue("TYPE");
      }
      else if (elementName.equals("entity_mention"))
      {
        mentionID = attributes.getValue("ID");
        type = attributes.getValue("TYPE");
      }
      else if (elementName.equals("head"))
      {
        parsingHead = true;
        head = "";
      }
      else if (elementName.equals("extent"))
      {
        parsingExtent = true;
        extent = "";
      }
      else if (elementName.equals("charseq"))
      {
        inCharseq = true;

        if (parsingHead)
        {
          headStart = Integer.parseInt(attributes.getValue("START"));
          headEnd = Integer.parseInt(attributes.getValue("END"));
        }
        else if (parsingExtent)
        {
          extentStart = Integer.parseInt(attributes.getValue("START"));
          extentEnd = Integer.parseInt(attributes.getValue("END"));
        }
      }
    }


    /** Handles each XML tag as it closes. */
    public void endElement(String namespaceURI, String localName,
                           String qualifiedName)
      throws SAXException
    {
      String elementName = localName.equals("") ? qualifiedName : localName;

      if (elementName.equals("charseq")) inCharseq = false;
      else if (elementName.equals("extent")) parsingExtent = false;
      else if (elementName.equals("head")) parsingHead = false;
      else if (elementName.equals("entity_mention"))
      {
        list.add(
            new Document.MentionData(entityID, entityType, mentionID, type,
                                     head, headStart, headEnd, extent,
                                     extentStart, extentEnd));
        head = "";
        extent = "";
      }
    }


    /** Handles the character data that appears in between XML tags.*/
    public void characters(char buf[], int off, int len) throws SAXException
    {
      if (inCharseq)
      {
        if (parsingHead) head += new String(buf, off, len);
        else if (parsingExtent) extent += new String(buf, off, len);
      }
    }


    /** Unused. */
    public void endDocument() throws SAXException
    {
    }
  }
}

