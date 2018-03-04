/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Tokenizer.java,v 1.15 2004/05/06 18:18:45 aslom Exp $
 */

package org.gjt.xpp.impl.tokenizer;

import java.io.*;

import org.gjt.xpp.impl.tag.PullParserRuntimeException;

/**
 * Simpe XML Tokenizer (SXT) performs input stream tokenizing.
 *
 * Advantages:<ul>
 * <li>utility class to simplify creation of XML parsers, especially
 *    suited for pull event model but can support also push (SAX2)
 * <li>small footprint: whole tokenizer is in one file
 * <li>minimal memory utilization: does not use memory except for input
 *    and content buffer (that can grow in size)
 * <li>fast: all parsing done in one function (simple automata)
 * <li>supports most of XML 1.0 (except validation and external entities)
 * <li>low level: supports on demand parsing of Characters,
 *    CDSect, Comments, PIs etc.)
 * <li>parsed content: supports providing on demand
 *     parsed content to application (standard entities expanded
 *    all CDATA sections inserted, Comments and PIs removed)
 *    not for attribute values and element content
 * <li>mixed content: allow to dynamically disable mixed content
 * <li>small - total compiled size around 15K
 * </ul>
 *
 * Limitations:<ul>
 * <li>it is just a tokenizer - does not enforce grammar
 * <li>readName() is using Java identifier rules not XML
 * <li>does not parse DOCTYPE declaration (skips everyting in [...])
 * </ul>
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class Tokenizer {
    //enumeration of tokens that can be returned
    public final static byte END_DOCUMENT          = 2;
    public final static byte CONTENT               = 10;
    public final static byte CHARACTERS            = 20;
    public final static byte CDSECT                = 30;
    public final static byte COMMENT               = 40;
    public final static byte DOCTYPE               = 50;
    public final static byte PI                    = 60;
    public final static byte ENTITY_REF            = 70;
    public final static byte CHAR_REF              = 75;
    public final static byte ETAG_NAME             = 110;
    public final static byte EMPTY_ELEMENT         = 111;
    public final static byte STAG_END              = 112;
    public final static byte STAG_NAME             = 120;
    public final static byte ATTR_NAME             = 122;
    public final static byte ATTR_CHARACTERS       = 124;
    public final static byte ATTR_CONTENT          = 127;

    // parameters controlling tokenizer behaviour
    public boolean paramNotifyCharacters;
    public boolean paramNotifyComment;
    public boolean paramNotifyCDSect;
    public boolean paramNotifyDoctype;
    public boolean paramNotifyPI;
    public boolean paramNotifyCharRef;
    public boolean paramNotifyEntityRef;
    public boolean paramNotifyAttValue;

    public char[] buf = new char[BUF_SIZE];
    /** position of next char that will be read from buffer */
    public int pos;
    /**
     * Range [posStart, posEnd) defines part of buf that is content
     * of current token iff parsedContent == false
     */
    public int posStart;
    public int posEnd;
    public int posNsColon;
    public int nsColonCount;
    public boolean seenContent;

    /**
     * This falg decides which buffer will be used to retrieve
     * content for current token. If true use pc and [pcStart, pcEnd)
     * and if false use buf and [posStart, posEnd)
     */
    public boolean parsedContent;
    /**
     * This is buffer for parsed content such as
     * actual valuue of entity
     * ('&amp;lt;' in buf but in pc it is '&lt')
     */
    public char[] pc = new char[BUF_SIZE];
    /**
     * Range [pcStart, pcEnd) defines part of pc that is content
     * of current token iff parsedContent == false
     */
    public int pcStart;
    public int pcEnd;

    public Tokenizer() {
    }

    public void reset() {
        // release buffer that may have been used by setInput(char[])...
        if(!reading) {  // re-allocate if data was taken from input char[]
            //|| ((hardLimit != -1) && (buf.length < hardLimit))
            //) {
            //if(hardLimit != -1) {
            if(softLimit != -1) {
                resize(softLimit);
            } else {
                resize(BUF_SIZE);
            }
        }
        resetState();
    }

    private void resetState() {
        reading = true;
        reader = null;
        bufSize  = buf.length;
        bufStart = bufEnd = 0;
        posEnd = posStart = pos = 0;
        posNsColon = -1;
        state = STATE_INIT;
        prevCh = '\0';
        posCol = posRow = 1;
        //reachedEnd = false;
        pcEnd = pcStart = 0;
        // shrink to default size PC buf -- good for preserving memory!
        if(pc.length > 2 * BUF_SIZE) {
            pc = new char[BUF_SIZE];
        }
        previousState = -1;
        backtracking = false;
        seenContent = false;
        shrinkOffset = 0;
    }

    /** Reset tokenizer state and set new input source */
    public void setInput(Reader r) {
        reset();
        reading = true;
        reader = r;
        bufStart = bufEnd = 0;
    }

    /** Reset tokenizer state and set new input source */
    public void setInput(char[] data) {
        setInput(data, 0 , data.length);
    }

    public void setInput(char[] data, int off, int len)
    {
        resetState();
        reading = false;
        buf = data;
        bufStart = pos = off;
        bufSize = bufEnd = off + len; //buf.length;
        //if(paramPC && pc.length < bufSize) {
        //    pc = new char[bufSize];
        //    if(TRACE_SIZING) for(int i = 0; i < bufSize; ++i) pc[i]='X';
        //}
    }

    /**
     * Set notification of all XML content tokens:
     * Characters, Comment, CDSect, Doctype, PI, EntityRef, CharRef and
     * AttValue (tokens for STag, ETag and Attribute are always sent).
     */
    public void setNotifyAll(boolean enable) {
        paramNotifyCharacters = enable;
        paramNotifyComment = enable;
        paramNotifyCDSect = enable;
        paramNotifyDoctype = enable;
        paramNotifyPI = enable;
        paramNotifyEntityRef = enable;
        paramNotifyCharRef = enable;
        paramNotifyAttValue = enable;
    }

    /**
     * Allow reporting parsed content for element content
     * and attribute content (no need to deal with low level
     * tokens such as in setNotifyAll).
     */
    public void setParseContent(boolean enable) {
        paramPC = enable;
        //if(paramPC && pc.length < bufSize) {
        //    pc = new char[bufSize];
        //}
    }

    public boolean isAllowedMixedContent() {
        return !paramNoMixContent ;
    }

    /**
     * Set support for mixed conetent. If mixed content is
     * disabled tokenizer will do its best to ensure that
     * no element has mixed content model also ignorable whitespaces
     * will not be reported as element content.
     */
    public void setAllowedMixedContent(boolean enable) {
        paramNoMixContent = !enable;
    }

    public int getSoftLimit() {
        return softLimit;
    }

    /**
     * Set soft limit on internal buffer size.
     * That means suggested size that tokznzier will try to keep.
     */
    public void setSoftLimit(int value) throws TokenizerException {
        //if(state != STATE_INIT) {
        //  throw new TokenizerException(
        //    "soft limit can not be changed after parsing started");
        //}
        if(!reading) {
            throw new TokenizerException(
                "hard limit can not be set for char array input"
            );
        }
        if((value != -1) && (hardLimit != -1) && (2 * value > hardLimit)) {
            throw new TokenizerException(
                "soft limit can no tbe bigger than half of hard limit"
                    +"current hard limit "+hardLimit
            );

        }
        softLimit = value;
        if(softLimit != -1) {
            posSafe = softLimit;
        } else if(hardLimit != -1) {
            posSafe = hardLimit / 2;
        } else {
            posSafe = ((loadFactor * bufSize) / 100); //restore default
        }
    }

    public int getHardLimit() {
        return hardLimit;
    }

    /**
     * Set hard limit on internal buffer size.
     * That means that if input (such as element content) is bigger than
     * hard limit size tokenizer will throw
     * XmlTokenizerBufferOverflowException.
     */
    public void setHardLimit(int value) throws TokenizerException {
        if(!reading) {
            throw new TokenizerException(
                "hard limit can not be set for char array input"
            );
        }
        if(state != STATE_INIT && value < hardLimit) {
            throw new TokenizerException(
                "hard limit on buffer size can not be shrunk during parsing"
            );
        }
        if(softLimit == -1 && value != -1) {
            throw new TokenizerException(
                "soft limit must be set to non -1 before setting hard limit"
                    +getPosDesc(), posRow, (posCol-1)
            );
        }
        if((value != -1) && ((2 * softLimit) >= value)) {
            throw new TokenizerException(
                "hard limit must be at least twice the size of soft limit"
                    +"current soft limit "+softLimit+" and hard limit "+value
                    +getPosDesc(), posRow, (posCol-1)
            );

        }
        // resize buffer to new hard limit
        hardLimit = value;
        if(shrinkable && softLimit != -1 && softLimit < bufSize) {
            resize(softLimit);
        }
    }


    public int getBufferShrinkOffset()
    {
        return shrinkOffset;
    }

    public void setBufferShrinkable(boolean shrinkable) throws TokenizerException
    {
        this.shrinkable = shrinkable;
    }

    public boolean isBufferShrinkable()
    {
        return shrinkable;
    }

    //    private static int findFragment(int bufStart, char[] b, int start, int j) {
    //        if(start <= bufStart) return start;
    //        int i = start;
    //        while(--i > bufStart) {
    //            if((j - i) > 55) break;
    //            char c = b[i];
    //            if(c == '<' && (start - i) > 10) break;
    //        }
    //        return i;
    //    }
    //

    private static int findFragment(int bufStart, char[] b, int start, int end) {
        //System.err.println("bufStart="+bufStart+" b="+printable(new String(b, start, end - start))+" start="+start+" end="+end);
        if(start < bufStart) {
            start = bufStart;
            if(start > end) start = end;
            return start;
        }
        if(end - start > 55) {
            start = end - 10; // try to find good location
        }
        int i = start + 1;
        while(--i > bufStart) {
            if((end - i) > 55) break;
            char c = b[i];
            if(c == '<' && (start - i) > 10) break;
        }
        return i;
    }


    /**
     * Return string describing current position of parsers as
     * text 'at line %d (row) and column %d (colum) [seen %s...]'.
     */
    public String getPosDesc() {
        String fragment = null;
        //        if(parsedContent) {
        //            //System.err.println("pcStart="+pcStart+" pcEnd="+pcEnd);
        //            //if(pcStart > 0) fragment = "...";
        //            if(pcStart <= pcEnd) {
        //                int start = findFragment(pc, pcStart, pcEnd);
        //                if(start < pcEnd) {
        //                    fragment = new String(pc, start, pcEnd - start);
        //                    if(start > bufStart) fragment = "..." + fragment;
        //                }
        //            }
        //        } else {
        //System.err.println("posStart="+posStart+" posEnd="+posEnd);
        //if(posStart > 0) fragment = "...";
        if(posStart <= posEnd) {
            int start = findFragment(bufStart, buf, posStart, posEnd);
            //System.err.println("start="+start);
            if(start < posEnd) {
                fragment = new String(buf, start, posEnd - start);
                if(start > bufStart) fragment = "..." + fragment;
            }
            //}
        }
        return " at line "+posRow
            +" and column "+(posCol-1)
            +(fragment != null ? " seen "+printable(fragment)+"..." : "");
    }

    public int getLineNumber() { return posRow; }
    public int getColumnNumber() { return posCol-1; }

    // ============ utility methods

    /**
     * Compare char arraychar after cha
     * - faster than converting to strings
     */
    private static boolean compareCharArr(char[] a, char[] b,
                                          int bStart, int bEnd)
    {
        if(bEnd - bStart != a.length) {
            return false;
        }
        for(int i = bStart, j = 0; i < bEnd; ++i, ++j) {
            if(a[j] != b[i])
                return false;
        }
        return true;
    }

    /** Return printable representation for all specaile chars */

    private static String printableChar(char ch) {
        if(ch == '\n') {
            return "\\n";
        } else if(ch == '\r') {
            return "\\r";
        } else if(ch == '\t') {
            return "\\t";
        } if(ch < 32) {
            return "\\u"+Character.digit(ch, 16)+"";
        }
        return ""+ch;
    }

    private static String printable(char ch) {
        return "'"+printableChar(ch)+"'";
    }

    private static String printable(String s) {
        int iN = s.indexOf('\n');
        int iR = s.indexOf('\r');
        int iT = s.indexOf('\t');
        if((iN != -1) || (iR != -1) || (iT != -1)) {
            StringBuffer buf = new StringBuffer("\"");
            for(int i = 0; i < s.length(); ++i) {
                buf.append(printableChar(s.charAt(i)));
            }
            buf.append("\"");
            s = buf.toString();
        }
        return s;
    }



    // nameStart / name lookup tables based on XML 1.1 http://www.w3.org/TR/2001/WD-xml11-20011213/
    protected static final int LOOKUP_MAX = 0x400;
    protected static final char LOOKUP_MAX_CHAR = (char)LOOKUP_MAX;
    //    protected static int lookupNameStartChar[] = new int[ LOOKUP_MAX_CHAR / 32 ];
    //    protected static int lookupNameChar[] = new int[ LOOKUP_MAX_CHAR / 32 ];
    protected static boolean lookupNameStartChar[] = new boolean[ LOOKUP_MAX ];
    protected static boolean lookupNameChar[] = new boolean[ LOOKUP_MAX ];

    private static final void setName(char ch)
        //{ lookupNameChar[ (int)ch / 32 ] |= (1 << (ch % 32)); }
    { lookupNameChar[ ch ] = true; }
    private static final void setNameStart(char ch)
        //{ lookupNameStartChar[ (int)ch / 32 ] |= (1 << (ch % 32)); setName(ch); }
    { lookupNameStartChar[ ch ] = true; setName(ch); }

    static {
        setNameStart(':');
        for (char ch = 'A'; ch <= 'Z'; ++ch) setNameStart(ch);
        setNameStart('_');
        for (char ch = 'a'; ch <= 'z'; ++ch) setNameStart(ch);
        for (char ch = '\u00c0'; ch <= '\u02FF'; ++ch) setNameStart(ch);
        for (char ch = '\u0370'; ch <= '\u037d'; ++ch) setNameStart(ch);
        for (char ch = '\u037f'; ch < '\u0400'; ++ch) setNameStart(ch);

        setName('-');
        setName('.');
        for (char ch = '0'; ch <= '9'; ++ch) setName(ch);
        setName('\u00b7');
        for (char ch = '\u0300'; ch <= '\u036f'; ++ch) setName(ch);
    }

    //private final static boolean isNameStartChar(char ch) {
    protected boolean isNameStartChar(char ch) {
        return (ch < LOOKUP_MAX_CHAR && lookupNameStartChar[ ch ])
            || (ch >= LOOKUP_MAX_CHAR && ch <= '\u2027')
            || (ch >= '\u202A' &&  ch <= '\u218F')
            || (ch >= '\u2800' &&  ch <= '\uFFEF')
            ;

        //      if(ch < LOOKUP_MAX_CHAR) return lookupNameStartChar[ ch ];
        //      else return ch <= '\u2027'
        //              || (ch >= '\u202A' &&  ch <= '\u218F')
        //              || (ch >= '\u2800' &&  ch <= '\uFFEF')
        //              ;
        //return false;
        //        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == ':'
        //          || (ch >= '0' && ch <= '9');
        //        if(ch < LOOKUP_MAX_CHAR) return (lookupNameStartChar[ (int)ch / 32 ] & (1 << (ch % 32))) != 0;
        //        if(ch <= '\u2027') return true;
        //        //[#x202A-#x218F]
        //        if(ch < '\u202A') return false;
        //        if(ch <= '\u218F') return true;
        //        // added pairts [#x2800-#xD7FF] | [#xE000-#xFDCF] | [#xFDE0-#xFFEF] | [#x10000-#x10FFFF]
        //        if(ch < '\u2800') return false;
        //        if(ch <= '\uFFEF') return true;
        //        return false;


        // else return (supportXml11 && ( (ch < '\u2027') || (ch > '\u2029' && ch < '\u2200') ...
    }

    //private final static boolean isNameChar(char ch) {
    protected boolean isNameChar(char ch) {
        //return isNameStartChar(ch);

        //        if(ch < LOOKUP_MAX_CHAR) return (lookupNameChar[ (int)ch / 32 ] & (1 << (ch % 32))) != 0;

        return (ch < LOOKUP_MAX_CHAR && lookupNameChar[ ch ])
            || (ch >= LOOKUP_MAX_CHAR && ch <= '\u2027')
            || (ch >= '\u202A' &&  ch <= '\u218F')
            || (ch >= '\u2800' &&  ch <= '\uFFEF')
            ;
        //return false;
        //        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == ':'
        //          || (ch >= '0' && ch <= '9');
        //        if(ch < LOOKUP_MAX_CHAR) return (lookupNameStartChar[ (int)ch / 32 ] & (1 << (ch % 32))) != 0;

        //else return
        //  else if(ch <= '\u2027') return true;
        //        //[#x202A-#x218F]
        //        else if(ch < '\u202A') return false;
        //        else if(ch <= '\u218F') return true;
        //        // added pairts [#x2800-#xD7FF] | [#xE000-#xFDCF] | [#xFDE0-#xFFEF] | [#x10000-#x10FFFF]
        //        else if(ch < '\u2800') return false;
        //        else if(ch <= '\uFFEF') return true;
        //else return false;
    }

    /**
     * Determine if ch is whitespace ([3] S)
     */
    protected boolean isS(char ch) {
        return (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t');
        // || (supportXml11 && (ch == '\u0085' || ch == '\u2028');
    }

    /**
     * Read name from input or throw exception ([4] NameChar, [5] Name).
     */
    // TODO: make it fully complaint with XML spec
    private char readName(char ch) throws IOException,
        TokenizerException {
        //int type = Character.getType(ch); //TODO: translate [84]-[89] for Java...
        posNsColon = -1;
        nsColonCount = 0;
        //if(  !Character.isLowerCase(ch)
        //   && !Character.isUpperCase(ch)
        //   && ch != ':'
        //   && ch != '_')
        if(!isNameStartChar(ch)) {
            throw new TokenizerException(
                "expected name start not "+printable(ch)
                    +getPosDesc(), posRow, (posCol-1));
        }
        do {
            ch = more();
            if(ch == ':') {
                posNsColon = pos - 1;
                ++nsColonCount;
            }
        } while( isNameChar(ch));
        //Character.isLowerCase(ch)
        //    || Character.isUpperCase(ch)
        //    || Character.isDigit(ch)
        //    || ch == '.' || ch == '-'
        //    || ch == '_' || ch == ':');
        return ch;
    }



    private char skipS(char ch) throws IOException,
        TokenizerException //XmlTokenizerBufferOverflowException
    {
        while(ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r')
            ch = more();
        return ch;
    }

    private char readS(char ch) throws IOException, TokenizerException {
        if(!isS(ch))
            throw new TokenizerException(
                "expected white space not "+ch+getPosDesc(), posRow, (posCol-1));
        while(ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r')
            ch = more();
        return ch;
    }

    // ========= input buffer management

    /**
     * Get next available character from input.
     * If it is last character set internal flag reachedEnd.
     * If there are no more characters throw EOFException.
     */
    private char more() throws IOException, TokenizerException {
        if(backtracking) {
            backtracking = false;
            //return prevCh = ch;
            ++pos;
            ++posCol;
            if(TRACING) System.err.println(
                    "TRACING BACK XmlTokenizer ch="+printable(prevCh)+" state="+state
                        +" posStart="+posStart+" posEnd="+posEnd
                        +" pcStart="+pcStart+" pcEnd="+pcEnd);
            //prevCh = prevPrevCh;
            return prevCh;
        }
        if(!reading) {
            //if(pos == bufEnd - 1)
            //    reachedEnd = true;
            if(pos >= bufEnd) {
                throw new EOFException("no more data available");
            }
        } else if(hardLimit != -1 && (pos >= hardLimit - 1)) {
            // no more place to grow...
            throw new TokenizerBufferOverflowException(
                "reached hard limit on buffer size"
                    +getPosDesc(), posRow, (posCol-1));
        } else if(pos > bufEnd - 1) {
            if(hardLimit != -1 && bufSize > hardLimit) {
                throw new TokenizerBufferOverflowException(
                    "buffer size should never exceed hard limit"+getPosDesc(),
                    posRow, (posCol-1));
            }
            int spaceLeft = bufSize - bufEnd;
            if(spaceLeft <= readChunkSize) { //expand buffer to accomodate read
                int newSize = 2 * bufSize;
                if(hardLimit == -1) {
                    if(newSize < softLimit) {
                        newSize = 2 * softLimit;
                    }
                    if(newSize < 4*readChunkSize) {
                        newSize = 12 * readChunkSize;
                    }
                } else if(newSize > hardLimit) {  //hardLimit != -1
                    if(bufEnd >= hardLimit) {
                        throw new TokenizerBufferOverflowException(
                            "buffer can not grow beyond hard limit"
                                +getPosDesc(), posRow, (posCol-1));
                    }
                    if(TRACE_SIZING) {
                        System.err.println("limiting new size to hard limit");
                    }
                    newSize = hardLimit;
                }
                if(newSize - bufSize > 0) {
                    resize(newSize);
                }
            }
            int chunkie = readChunkSize;
            // sorry that is only how much we are allowed to read - hard limit..
            if(bufSize - bufEnd < chunkie) {
                chunkie = bufSize -  bufEnd;
            }
            if(reader == null) {
                throw new TokenizerException(
                    "to start parsing setInput() must be called!");
            }
            int bufRead = reader.read(buf, bufEnd, chunkie);
            if(bufRead == -1) {
                //if(reachedEnd) {
                //    throw new TokenizerException(
                //      "can't read more - reached end of stream (state="+state+")"
                //          +getPosDesc());
                //}
                //reachedEnd = true;
                throw new EOFException("no more data available to read");
            } if(bufRead == 0) {
                // TODO: chekc how it will behave when reading rough socket...
                throw new TokenizerBufferOverflowException(
                    "can't read more data in buffer (read() returns 0 chars)"
                        +getPosDesc());
            } else {
                bufEnd += bufRead;
            }
            //} else { //reading && hardLimit != -1
        }
        char ch = buf[pos++];

        // update (row,colum) position so far
        // new lines: "\r\n","\r\n"...  "\n","\n",...   "\r","\r","\r"
        if(ch == '\n' || ch == '\r') {
            if ( prevCh != '\r' || ch != '\n' ) {
                posCol = 2; // always one char ahead
                ++posRow;
            }
            // 2.11 End-of-Line Handling
            //XXX
            //if(NORMALIZE_LINE_BREAKS && ch == '\r') {

            //ch = '\n';
            // make sure that parsed content will have it correct
            //if(paramPC && pcStart == pcEnd && posEnd > posStart) {
            //int len = posEnd - posStart;
            //System.arraycopy(buf, posStart, pc, pcEnd, len);
            //pcEnd += len;
            //}
            //if(paramPC)
            //pc[pcEnd++] = '\n';
            //}
            //System.err.println("ch = "+printable(ch)+" posRow="+posRow);
        } else {
            ++posCol;
        }
        prevPrevCh = prevCh;
        if(TRACING) System.err.println(
                "TRACING XmlTokenizer ch="+printable(ch)+" state="+state
                    +" posStart="+posStart+" posEnd="+posEnd
                    +" pcStart="+pcStart+" pcEnd="+pcEnd
                    + " prevCh="+printable(prevCh));
        return prevCh = ch;
    }

    private void resize(int newSize) {
        if(TRACE_SIZING) {
            System.err.println("resizing "+bufSize+" ==> "+newSize);
        }
        char[] newBuf = new char[newSize];
        int end = bufEnd;
        if(end > newSize) { // case of shriniking
            end = newSize;
            if(state != STATE_INIT) { // should never happen...
                throw new PullParserRuntimeException(
                    "internal buffer may not be shrank during parsing"
                        +getPosDesc());
            }
        }
        System.arraycopy(buf, 0, newBuf, 0, end);
        buf = newBuf;
        bufSize = newSize;
        if(softLimit == -1) {
            posSafe = ((loadFactor * bufSize) / 100);
        }
        if(TRACE_SIZING) for(int i = bufEnd; i < bufSize; ++i) buf[i]='X';
        //if(paramPC && pc.length < bufSize) {
        //    char[] newpc = new char[bufSize];
        //    System.arraycopy(pc, 0, newpc, 0, pcEnd);
        //    pc = newpc;
        //    if(TRACE_SIZING) for(int i = pcEnd; i < bufSize; ++i) pc[i]='X';
        //}
    }


    private void ensurePC() {
        if(paramPC && pcEnd >= pc.length) {
            int newSize = 2 * pcEnd + 1;
            char[] newpc = new char[newSize];
            System.arraycopy(pc, 0, newpc, 0, pc.length);
            pc = newpc;
            if(TRACE_SIZING) for(int i = pcEnd; i < newSize; ++i) pc[i]='X';
        }
    }

    /**
     * Special procedure to undo last read character...
     */
    private char less() {
        //NOTE: trick - we are backtracing one characker....
        --pos;
        --posCol;
        backtracking = true;
        //return buf[pos - 1];
        return prevPrevCh;
    }

    /**
     * Shrink processing buffer size.
     */
    private void shrink(int posCut) {
        //System.err.println("shrink called posCut="+posCut);
        System.arraycopy(buf, posCut, buf, 0, bufEnd - posCut);
        shrinkOffset += posCut;
        bufEnd -= posCut;
        pos -= posCut;
        posStart -= posCut;
        posEnd -= posCut;
        posNsColon -= posCut;
    }

    private void joinPC() {
        if(pcStart == pcEnd) {
            appendPC();
        }
    }

    private void appendPC() {
        if(posEnd > posStart) {
            int len = posEnd - posStart;
            int pcLen = pc.length - pcEnd;
            if(pcLen < len) {
                int newSize = len;
                char[] newpc = new char[newSize];
                System.arraycopy(pc, 0, newpc, 0, pc.length);
                pc = newpc;
                if(TRACE_SIZING) for(int i = pcEnd; i < newSize; ++i) pc[i]='X';
            }
            System.arraycopy(buf, posStart, pc, pcEnd, len);
            pcEnd += len;
        }
    }

    /**
     * Return next recognized toke or END_DOCUMENT if no more input.
     *
     * <p>This is simple automata (in pseudo-code):
     * <pre>
     * byte next() {
     *    while(state != END_DOCUMENT) {
     *      ch = more();  // read character from input
     *      state = func(ch, state); // do transition
     *      if(state is accepting)
     *        return state;  // return token to caller
     *    }
     * }
     * </pre>
     *
     * <p>For speed (and simplicity?) it is using few procedures
     *  such as readName() or isS().
     *
     *
     *
     */
    public byte next() throws TokenizerException, IOException {
        if(state == STATE_FINISHED)
            throw new TokenizerException("attempt to read beyond end of input");

        if(state != STATE_FINISH_CONTENT && state != STATE_FINISH)
        {
            try {
                parsedContent = false;
                LOOP:
                while(true) {
                    //if(reachedEnd) {
                    //}
                    char ch = more();
                    //if(reachedEnd) continue LOOP;

                    // 2.11 End-of-Line Handling: "\r\n" -> "\n"; "\rX" -> "\nX"
                    //XXX
                    if(NORMALIZE_LINE_BREAKS) {
                        if(ch == '\r') {
                            joinPC();
                            //ch = '\n';
                        } else if(prevPrevCh == '\r' && ch == '\n') {
                            continue LOOP; //ask for more chars --> ch = more();
                            // it can not be break as we are not yet in switch(..)
                        }
                    }


                    //System.out.println("prevState="+state);
                    switch(state) {
                        case STATE_INIT:
                            // detect BOM and frop it (Unicode Byte Order Mark)
                            if(ch == '\uFFFE') {
                                throw new TokenizerException(
                                    "first character in input was UNICODE noncharacter (0xFFFE)"+
                                        "- input requires byte swapping");
                            }
                            if(ch == '\uFEFF') {
                                // skipping UNICODE Byte Order Mark (so called BOM)
                                state = STATE_CONTENT_INIT;
                                break;
                            }
                            ; // fall through
                        case STATE_CONTENT_INIT:
                            pcEnd = pcStart = 0; //pos - 1;
                            ; // fall through
                        case STATE_CONTENT_CONTINUED:
                            posEnd = posStart = pos - 1;
                            //seenStartTag = false; // make sure to reset flag
                            state = STATE_CONTENT;
                            ; // fall through
                        case STATE_CONTENT:
                            while(true) {
                                if(ch == '<') {
                                    state = STATE_SEEN_LT;
                                    if(paramNotifyCharacters && posStart != posEnd)
                                        return CHARACTERS;
                                } else if(ch == '&') {
                                    if(paramPC) {
                                        joinPC();
                                    }
                                    if(!seenContent) {
                                        seenContent = true;
                                        if(paramNoMixContent && !mixInElement)
                                            throw new TokenizerException(
                                                "mixed content disallowed outside element"+getPosDesc(),
                                                posRow, (posCol-1));
                                    }
                                    state = STATE_SEEN_AMP;
                                    previousState = STATE_CONTENT_CONTINUED;
                                    posStart = pos - 1;
                                } else {
                                    if(!seenContent && !isS(ch)) {
                                        seenContent = true;
                                        if(paramNoMixContent && !mixInElement)
                                            throw new TokenizerException(
                                                "mixed content disallowed outside element, "
                                                    +"character "+printable(ch)
                                                    +" ("+(int)ch+")"+getPosDesc(), posRow, (posCol-1));
                                    }
                                    posEnd = pos;
                                    if(paramPC &&
                                           ((pcStart != pcEnd)
                                                || (NORMALIZE_LINE_BREAKS && ch == '\r'))
                                      ) {
                                        //NOTE: normalization is done at beginning of LOOP
                                        //but it only will prepare pc buffer that is only now filled
                                        // CONSIDER: worst case when \r is first character!!!!
                                        if(NORMALIZE_LINE_BREAKS && ch == '\r') {
                                            if(pcEnd >= pc.length) ensurePC();
                                            pc[pcEnd++] = '\n';
                                        } else {
                                            if(pcEnd >= pc.length) ensurePC();
                                            pc[pcEnd++] = ch;
                                        }
                                    }
                                    //if(NORMALIZE_LINE_BREAKS && ch == '\r') {
                                    //  throw new IllegalStateException(
                                    //    "end-of-line normalization failed"+getPosDesc());
                                    //}
                                }
                                if(state != STATE_CONTENT) break;
                                ch = more();
                                if(NORMALIZE_LINE_BREAKS) {
                                    if(ch == '\r') {
                                        joinPC();
                                        //ch = '\n';
                                    } else if(prevPrevCh == '\r' && ch == '\n') {
                                        continue LOOP; //ask for more chars --> ch = more();
                                        // it can not be break as we are not yet in switch(..)
                                    }
                                }
                            }
                            break;
                        case STATE_SEEN_LT:
                            if(ch == '!') {
                                state = STATE_SEEN_LT_BANG;
                            } else if(ch == '?') {
                                state = STATE_PI;
                            } else { // it must be STag or ETag
                                boolean prevMixSeenContent = seenContent;
                                boolean prevMixInElement = mixInElement;
                                if(ch == '/') {
                                    state = STATE_SCAN_ETAG_NAME;
                                    mixInElement = false;
                                } else {
                                    state = STATE_SCAN_STAG_NAME;
                                    if(paramNoMixContent && seenContent)
                                        throw new TokenizerException(
                                            "mixed content disallowed"+
                                                " inside element and before start tag"+getPosDesc(),
                                            posRow, (posCol-1));
                                    mixInElement = true;
                                }

                                if(paramPC)
                                {

                                    if( (pcStart != pcEnd || posEnd != posStart
                                             || (state == STATE_SCAN_ETAG_NAME && seenStartTag))
                                       &&
                                           (paramNoMixContent == false
                                                || prevMixSeenContent
                                                || (paramNoMixContent //&& !prevMixInElement
                                                        && state == STATE_SCAN_ETAG_NAME && seenStartTag
                                                   )
                                           )

                                      )
                                    {
                                        parsedContent = (pcEnd != pcStart);
                                        //                 || parsedContent
                                        //                 || (posEnd != posStart)
                                        //                 // condition below allows to report "" content for <tag></tag>
                                        //                 || (paramNoMixContent && state == STATE_SCAN_ETAG_NAME
                                        //                       && seenStartTag
                                        //                       //&& prevMixInElement && prevMixSeenContent)) {
                                        //                       //&& prevMixInElement
                                        //                    )
                                        //              )
                                        //            {
                                        //                System.out.println("returning CONTENT "
                                        //                                      +" posStart="+posStart
                                        //                                      +" posEnd="+posEnd
                                        //                                      +" pcStart="+pcStart
                                        //                                      +" pcEnd="+pcEnd
                                        //                                  );

                                        // MOTIVATION: save memory by not reporting "" content
                                        if(parsedContent || posEnd != posStart) {
                                            return CONTENT;
                                        } else {
                                            break;
                                        }

                                    }
                                }
                            }
                            // gather parsed content - keep what was before comments etc.
                            if(paramPC && state != STATE_SCAN_STAG_NAME
                               && state != STATE_SCAN_ETAG_NAME)
                            {
                                joinPC();
                            }
                            posStart = pos;  // to make PI start content
                            break;
                        case STATE_SEEN_LT_BANG:
                            if(ch == '-') {
                                ch = more();
                                if(ch != '-')
                                    throw new TokenizerException(
                                        "expected - for start of comment <!-- not "+ch+getPosDesc(),
                                        posRow, (posCol-1));
                                state = STATE_COMMENT;
                                posStart = pos;
                            } else if(ch == '[') {
                                ch = more(); if(ch != 'C')
                                    throw new TokenizerException(
                                        "expected <![CDATA"+getPosDesc(),
                                        posRow, (posCol-1));
                                ch = more(); if(ch != 'D')
                                    throw new TokenizerException(
                                        "expected <![CDATA"+getPosDesc(),
                                        posRow, (posCol-1));
                                ch = more(); if(ch != 'A')
                                    throw new TokenizerException(
                                        "expected <![CDATA"+getPosDesc(),
                                        posRow, (posCol-1));
                                ch = more(); if(ch != 'T')
                                    throw new TokenizerException(
                                        "expected <![CDATA"+getPosDesc(),
                                        posRow, (posCol-1));
                                ch = more(); if(ch != 'A')
                                    throw new TokenizerException(
                                        "expected <![CDATA"+getPosDesc(),
                                        posRow, (posCol-1));
                                ch = more(); if(ch != '[')
                                    throw new TokenizerException(
                                        "expected <![CDATA"+getPosDesc(),
                                        posRow, (posCol-1));
                                posStart = pos;
                                if(!seenContent) {
                                    seenContent = true;
                                    if(paramNoMixContent && !mixInElement)
                                        throw new TokenizerException(
                                            "mixed content disallowed outside element"
                                                +getPosDesc(), posRow, (posCol-1));
                                }
                                state = STATE_CDSECT;
                            } else if(ch == 'D') {
                                ch = more(); if(ch != 'O')
                                    throw new TokenizerException(
                                        "expected <![DOCTYPE"+getPosDesc(),
                                        posRow, (posCol-1));
                                ch = more(); if(ch != 'C')
                                    throw new TokenizerException(
                                        "expected <![DOCTYPE"+getPosDesc(),
                                        posRow, (posCol-1));
                                ch = more(); if(ch != 'T')
                                    throw new TokenizerException(
                                        "expected <![DOCTYPE"+getPosDesc(),
                                        posRow, (posCol-1));
                                ch = more(); if(ch != 'Y')
                                    throw new TokenizerException(
                                        "expected <![DOCTYPE"+getPosDesc(),
                                        posRow, (posCol-1));
                                ch = more(); if(ch != 'P')
                                    throw new TokenizerException(
                                        "expected <![DOCTYPE"+getPosDesc(),
                                        posRow, (posCol-1));
                                ch = more(); if(ch != 'E')
                                    throw new TokenizerException(
                                        "expected <![DOCTYPE"+getPosDesc(),
                                        posRow, (posCol-1));
                                posStart = pos;
                                state = STATE_DOCTYPE;
                            } else {
                                throw new TokenizerException(
                                    "unknown markup after <! "+printableChar(ch)+getPosDesc(),
                                    posRow, (posCol-1));
                            }
                            break;

                            // [66]-[68] reference ; 4.1 Character and Entity Reference
                        case STATE_SEEN_AMP:
                            posStart = pos - 2;
                            if(ch == '#') {
                                state = STATE_CHAR_REF;
                                break;
                            }
                            state = STATE_ENTITY_REF;
                            ; // fall through
                        case STATE_ENTITY_REF:
                            if(ch == ';') {
                                state = previousState;
                                posEnd = pos;
                                // 4.6 Predefined Entities
                                if(paramPC) {
                                    int i = posStart + 1;
                                    int j = pos - 1;
                                    int len = j - i;
                                    if(len == 2 && buf[i] == 'l' && buf[i+1] == 't') {
                                        if(pcEnd >= pc.length) ensurePC();
                                        pc[pcEnd++] = '<';
                                    } else if(len == 3 && buf[i] == 'a'
                                              && buf[i+1] == 'm' && buf[i+2] == 'p') {
                                        if(pcEnd >= pc.length) ensurePC();
                                        pc[pcEnd++] = '&';
                                    } else if(len == 2 && buf[i] == 'g' && buf[i+1] == 't') {
                                        if(pcEnd >= pc.length) ensurePC();
                                        pc[pcEnd++] = '>';
                                    } else if(len == 4 && buf[i] == 'a' && buf[i+1] == 'p'
                                              && buf[i+2] == 'o' && buf[i+3] == 's')
                                    {
                                        if(pcEnd >= pc.length) ensurePC();
                                        pc[pcEnd++] = '\'';
                                    } else if(len == 4 && buf[i] == 'q' && buf[i+1] == 'u'
                                              && buf[i+2] == 'o' && buf[i+3] == 't')
                                    {
                                        if(pcEnd >= pc.length) ensurePC();
                                        pc[pcEnd++] = '"';
                                    } else {
                                        String s = new String(buf, i, j - i);
                                        throw new TokenizerException(
                                            "undefined entity "+s+getPosDesc(), posRow, (posCol-1));
                                    }
                                }
                                if(paramNotifyEntityRef)
                                    return ENTITY_REF;
                            }
                            break;
                        case STATE_CHAR_REF:
                            charRefValue = 0;
                            state = STATE_CHAR_REF_DIGITS;
                            if(ch == 'x') {
                                charRefHex = true;
                                break;
                            }
                            charRefHex = false;
                            ; // fall through
                        case STATE_CHAR_REF_DIGITS:
                            if(ch == ';') {
                                if(paramPC) {
                                    if(pcEnd >= pc.length) ensurePC();
                                    pc[pcEnd++] = charRefValue;
                                }
                                state = previousState;
                                posEnd = pos;
                                if(paramNotifyCharRef)
                                    return CHAR_REF;
                            } else if(ch >= '0' && ch <= '9') {
                                if(charRefHex) {
                                    charRefValue = (char)(charRefValue * 16 + (ch - '0'));
                                } else {
                                    charRefValue = (char)(charRefValue * 10 + (ch - '0'));
                                }
                            } else if(charRefHex && ch >= 'A' && ch <= 'F') {
                                charRefValue = (char)(charRefValue * 16 + (ch - 'A' + 10));
                            } else if(charRefHex && ch >= 'a' && ch <= 'f') {
                                charRefValue = (char)(charRefValue * 16 + (ch - 'a' + 10));
                            } else {
                                throw new TokenizerException(
                                    "character reference may not contain "
                                        +printable(ch)+getPosDesc(),
                                    posRow, (posCol-1));
                            }
                            break;

                            // [40]-[44]; 3.1 Start-Tags, End-Tags, and Empty-Element Tags
                        case STATE_SCAN_ETAG_NAME:
                            seenStartTag = false;
                            seenContent = false;
                            posStart = pos - 1;
                            ch = readName(ch);
                            posEnd = pos - 1;
                            ch = skipS(ch);
                            if(ch != '>')
                                throw new TokenizerException(
                                    "expected > for end tag not "
                                        +printable(ch)+getPosDesc(), posRow, (posCol-1));
                            state = STATE_CONTENT_INIT;
                            return ETAG_NAME;

                        case STATE_SCAN_STAG_NAME:
                            // dangerous call!!!!
                            if(shrinkable && reading && pos > 2
                               //&& (bufEnd - pos) <= 64
                               && pos > posSafe
                              ) {
                                shrink(pos - 2);
                            }

                            seenStartTag = true;
                            seenContent = false;
                            ch = less();
                            posStart = pos - 1;
                            ch = readName(ch);
                            posEnd = pos - 1;
                            ch = less();
                            //if(ch != '>')
                            state = STATE_SCAN_ATTR_NAME;
                            pcEnd = pcStart = 0; // to have place for attribute content
                            return STAG_NAME;


                        case STATE_SCAN_STAG_GT:
                            if(ch == '>') {
                                state = STATE_CONTENT_INIT;
                                posStart = pos -1;
                                posEnd = pos;
                                return STAG_END;
                            } else {
                                throw new TokenizerException(
                                    "expected > for end of start tag not "
                                        +printable(ch)+getPosDesc(),
                                    posRow, (posCol-1));
                            }

                        case STATE_SCAN_ATTR_NAME:
                            {
                                pcStart = pcEnd;
                                boolean seenSBeforeAttrName = isS(ch);
                                ch = skipS(ch);
                                if(ch == '/') { // [44] EmptyElemTag
                                    state = STATE_SCAN_STAG_GT;
                                    posStart = pos -1;
                                    posEnd = pos;
                                    mixInElement = false;
                                    seenStartTag = false;
                                    return EMPTY_ELEMENT;
                                } else if(ch == '>') {
                                    state = STATE_CONTENT_INIT;
                                    posStart = pos -1;
                                    posEnd = pos;
                                    return STAG_END;
                                }
                                if(false == seenSBeforeAttrName) {
                                    throw new TokenizerException(
                                        "white space expected before attribute name and not "
                                            +printable(ch)+getPosDesc(),
                                        posRow, (posCol-1));
                                }
                                posStart = pos - 1;
                                ch = readName(ch);
                                posEnd = pos - 1;
                                ch = less();
                                state = STATE_SCAN_ATTR_EQ;
                                return ATTR_NAME;
                            }
                        case STATE_SCAN_ATTR_EQ:
                            ch = skipS(ch);
                            if(ch != '=')
                                throw new TokenizerException(
                                    "expected = after attribute name not "+
                                        printable(ch)+getPosDesc(),
                                    posRow, (posCol-1));
                            state = STATE_SCAN_ATTR_VALUE;
                            break;
                        case STATE_SCAN_ATTR_VALUE: // [10] AttValue
                            ch = skipS(ch);
                            if(ch != '\'' && ch != '"')
                                throw new TokenizerException(
                                    "attribute value must start with double quote"
                                        +" or apostrophe not "
                                        +printable(ch)+getPosDesc(), posRow, (posCol-1));
                            attrMarker = ch;
                            state = STATE_SCAN_ATTR_VALUE_CONTINUE;
                            //make sure that attr value such as \r will be in buffer beginning
                            posEnd = posStart = pos;
                            pcEnd = pcStart; //remove possible attr name form PC buffer
                            break;
                        case STATE_SCAN_ATTR_VALUE_CONTINUE:
                            state = STATE_SCAN_ATTR_VALUE_END;
                            ; // fall through
                        case STATE_SCAN_ATTR_VALUE_END:
                            if(ch == attrMarker) {
                                if(paramPC) {
                                    state = STATE_ATTR_VALUE_CONTENT;
                                } else {
                                    state = STATE_SCAN_ATTR_NAME;
                                }
                                if(paramNotifyAttValue)
                                    return ATTR_CHARACTERS;
                            } else if(ch == '&') {
                                if(paramPC) {
                                    joinPC();
                                }
                                state = STATE_SEEN_AMP;
                                previousState = STATE_SCAN_ATTR_VALUE_CONTINUE;
                                if(paramNotifyAttValue)
                                    return ATTR_CHARACTERS;
                            } else if(ch == '<') {
                                throw new TokenizerException(
                                    "attribute value can not contain "+ch+getPosDesc(),
                                    posRow, (posCol-1));
                            } else {
                                //if(NORMALIZE_LINE_BREAKS && ch == '\r') {
                                //  throw new IllegalStateException(
                                //    "end-of-line normalization failed"+getPosDesc());
                                //}
                                //if(paramPC && pcStart != pcEnd
                                //|| (paramPC && NORMALIZE_LINE_BREAKS && ch == '\r')
                                //) {
                                if(paramPC &&
                                       ((pcStart != pcEnd)
                                            //|| (NORMALIZE_LINE_BREAKS && ch == '\r')
                                            || ch == '\t' || ch == '\n' || ch == '\r')
                                  ) {
                                    //if(NORMALIZE_LINE_BREAKS && ch == '\r') {
                                    //  pc[pcEnd++] = '\n';
                                    //} else {
                                    //  pc[pcEnd++] = ch;
                                    //}

                                    joinPC();

                                    // 3.3.3 Attribute-Value Normalization
                                    if(ch == '\t' || ch == '\n' || ch == '\r') {
                                        if(pcEnd >= pc.length) ensurePC();
                                        pc[pcEnd++] = ' ';
                                    } else {
                                        if(pcEnd >= pc.length) ensurePC();
                                        pc[pcEnd++] = ch;
                                    }
                                } else {
                                    posEnd = pos;
                                }
                            }
                            break;
                        case STATE_ATTR_VALUE_CONTENT:
                            ch = less();
                            // finishPC()
                            parsedContent = (pcEnd != pcStart);
                            state = STATE_SCAN_ATTR_NAME;
                            return ATTR_CONTENT;

                            // [18] - [21] CDSEct; 2.7 CDATA Sections
                        case STATE_CDSECT:
                            if(ch == ']')
                                state = STATE_CDSECT_BRACKET;
                            break;
                        case STATE_CDSECT_BRACKET:
                            if(ch == ']')
                                state = STATE_CDSECT_BRACKET_BRACKET;
                            else
                                state = STATE_CDSECT;
                            break;
                        case STATE_CDSECT_BRACKET_BRACKET:
                            if(ch == '>') {
                                state = STATE_CONTENT_CONTINUED;
                                posEnd = pos - 3;
                                //TODO LOW: avoid memory copy for <m:bar><![CDATA[TEST]]></m:bar>
                                if(paramPC) {
                                    appendPC();
                                }
                                if(paramNotifyCDSect)
                                    return CDSECT;
                            } else {
                                if(ch == ']') {
                                    state = STATE_CDSECT_BRACKET_BRACKET;
                                } else {
                                    state = STATE_CDSECT;
                                }
                            }
                            break;

                            // [15] Comment; 2.5 Comments
                        case STATE_COMMENT:
                            if(ch == '-')
                                state = STATE_COMMENT_DASH;
                            break;
                        case STATE_COMMENT_DASH:
                            if(ch == '-')
                                state = STATE_COMMENT_DASH_DASH;
                            else
                                state = STATE_COMMENT;
                            break;
                        case STATE_COMMENT_DASH_DASH:
                            if(ch == '>') {
                                state = STATE_CONTENT_CONTINUED;
                                posEnd = pos - 3;
                                if(paramNotifyComment)
                                    return COMMENT;
                            } else if(ch == '-') {
                                throw new TokenizerException(
                                    "expected > after -- in coment and not "+ch+getPosDesc(),
                                    posRow, (posCol-1));
                            } else {
                                state = STATE_COMMENT;
                            }
                            break;

                            // [28] doctypedecl; 2.8 Prolog and Document Type Declaration
                        case STATE_DOCTYPE:
                            if(ch == '[')
                                state = STATE_DOCTYPE_BRACKET;
                            else if(ch == '>') {
                                state = STATE_CONTENT_CONTINUED;
                                posEnd = pos - 1;
                                if(paramNotifyDoctype)
                                    return DOCTYPE;
                            }
                            break;
                        case STATE_DOCTYPE_BRACKET:
                            if(ch == ']')
                                state = STATE_DOCTYPE_BRACKET_BRACKET;
                            break;
                        case STATE_DOCTYPE_BRACKET_BRACKET:
                            ch = skipS(ch);
                            if(ch == '>') {
                                state = STATE_CONTENT_CONTINUED;
                                posEnd = pos - 1;
                                if(paramNotifyDoctype)
                                    return DOCTYPE;
                            } else {
                                throw new TokenizerException(
                                    "expected > for DOCTYPE end not "+ch+getPosDesc(),
                                    posRow, (posCol-1));
                            }
                            break;

                            // [16]-[17] PI; 2.6 Processing Instructions
                        case STATE_PI:  //TODO: enforce "XML" as reserved prefix
                            if(ch == '?')
                                state = STATE_PI_END;
                            break;
                        case STATE_PI_END:
                            if(ch == '>') {
                                state = STATE_CONTENT_CONTINUED;
                                posEnd = pos - 2;
                                if(paramNotifyPI)
                                    return PI;
                            }
                            break;
                        default:
                            throw new TokenizerException(
                                "invalid internal state "+state
                                    +getPosDesc(), posRow, (posCol-1));
                    }
                }
            } catch(EOFException ex) {


                if(state != STATE_CONTENT && state != STATE_CONTENT_INIT
                   && state != STATE_CONTENT_CONTINUED) {
                    throw new TokenizerException(
                        "unexpected end of stream (state="+state+")"
                            +getPosDesc());
                }
                state = STATE_FINISH_CONTENT;
                if(paramNotifyCharacters && posEnd > posStart)
                    return CHARACTERS;

            }
        } // if

        if(state == STATE_FINISH_CONTENT) {

            if(state == STATE_CONTENT_INIT
               || state == STATE_CONTENT_CONTINUED) {
                if(state == STATE_CONTENT_INIT) {
                    pcEnd = pcStart = 0; //pos - 1;
                }
                posEnd = posStart = pos - 1;
            }
            state = STATE_FINISH;
            if(paramPC && (pcStart != pcEnd || posEnd != posStart)) {
                //  if(pcEnd == pcStart) {
                //    pcStart = posStart;
                //    pcEnd = posEnd;
                //    pc = buf;
                //  } else {
                //    pc = pc;
                //  }
                parsedContent = (pcEnd != pcStart);
                if(paramNoMixContent == false || seenContent == false) {
                    return CONTENT;
                } else if(parsedContent)
                    throw new TokenizerException(
                        "no element content allowed before end of stream"+getPosDesc());
            }
        }
        if(state == STATE_FINISH) {
            state = STATE_FINISHED;
            if(TRACE_SIZING) {
                System.err.println("finished with bufEnd="+bufEnd+" bufSize="+bufSize
                                       +" softLimit="+softLimit
                                       +" hardLimit="+hardLimit);
            }
            return END_DOCUMENT;
        }
        throw new TokenizerException(
            "unexpected internal tokenizer state "+state
                +getPosDesc(), posRow, (posCol-1));

    }
    // ============ internal parser state


    private final static boolean TRACING = false;
    private final static boolean TRACE_SIZING = false;

    /** Parsed Content reporting */
    private boolean paramPC = true;
    /** Allow mixed content ? */
    private boolean paramNoMixContent = false;
    private boolean mixInElement = false;
    private boolean backtracking  = false;

    //    private final static int BUF_SIZE = 1024; //12 * 1024;
    //    private int readChunkSize = 1024;
    private final static int BUF_SIZE = 12 * 1024;
    private int readChunkSize = 4*1024;
    private int loadFactor = 99; // 99% == 99/100 == 0.99f
    //private int posSafe = BUF_SIZE - 1024; //2*readChunkSize; //
    private int posSafe = (int)((loadFactor * BUF_SIZE) / 100);
    private int softLimit = -1;
    private int hardLimit = -1;
    private int shrinkOffset = 0;
    private boolean shrinkable = true;
    private boolean reading = true;
    private Reader reader;
    private int bufStart;
    /** this is logical end of buffer content */
    private int bufEnd;
    private int bufSize = BUF_SIZE;

    // strictly internal
    private int posCol;
    private int posRow;
    private char prevCh;
    private char prevPrevCh;

    private char attrMarker;
    private char charRefValue;
    private boolean charRefHex;
    //private boolean reachedEnd;
    private boolean seenStartTag;
    /** used to implement simple one level CALL to entity parsing */
    private byte previousState;
    private byte state;

    private final static boolean NORMALIZE_LINE_BREAKS = true;

    // internal tokenizer automata states
    private final static byte STATE_INIT                    = 1;
    private final static byte STATE_FINISH_CONTENT          = 5;
    private final static byte STATE_FINISH                  = 6;
    private final static byte STATE_FINISHED                = 7;
    private final static byte STATE_CONTENT_INIT            = 10;
    private final static byte STATE_CONTENT_CONTINUED       = 11;
    private final static byte STATE_CONTENT                 = 12;
    private final static byte STATE_SEEN_LT                 = 13;
    private final static byte STATE_SEEN_LT_BANG            = 14;
    private final static byte STATE_CDSECT                  = 30;
    private final static byte STATE_CDSECT_BRACKET          = 31;
    private final static byte STATE_CDSECT_BRACKET_BRACKET  = 32;
    private final static byte STATE_COMMENT                 = 40;
    private final static byte STATE_COMMENT_DASH            = 41;
    private final static byte STATE_COMMENT_DASH_DASH       = 42;
    private final static byte STATE_DOCTYPE                 = 50;
    private final static byte STATE_DOCTYPE_BRACKET         = 51;
    private final static byte STATE_DOCTYPE_BRACKET_BRACKET = 52;
    private final static byte STATE_PI                      = 60;
    private final static byte STATE_PI_END                  = 61;
    private final static byte STATE_SEEN_AMP                = 70;
    private final static byte STATE_ENTITY_REF              = 71;
    private final static byte STATE_CHAR_REF                = 75;
    private final static byte STATE_CHAR_REF_DIGITS         = 76;
    private final static byte STATE_SCAN_ETAG_NAME          = 110;
    private final static byte STATE_SCAN_STAG_NAME          = 120;
    private final static byte STATE_SCAN_STAG_GT            = 121;
    private final static byte STATE_SCAN_ATTR_NAME          = 122;
    private final static byte STATE_SCAN_ATTR_EQ            = 123;
    private final static byte STATE_SCAN_ATTR_VALUE         = 124;
    private final static byte STATE_SCAN_ATTR_VALUE_CONTINUE= 125;
    private final static byte STATE_SCAN_ATTR_VALUE_END     = 126;
    private final static byte STATE_ATTR_VALUE_CONTENT      = 127;

}


/*
 * Indiana University Extreme! Lab Software License, Version 1.2
 *
 * Copyright (c) 2002-2004 The Trustees of Indiana University.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1) All redistributions of source code must retain the above
 *    copyright notice, the list of authors in the original source
 *    code, this list of conditions and the disclaimer listed in this
 *    license;
 *
 * 2) All redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the disclaimer
 *    listed in this license in the documentation and/or other
 *    materials provided with the distribution;
 *
 * 3) Any documentation included with all redistributions must include
 *    the following acknowledgement:
 *
 *      "This product includes software developed by the Indiana
 *      University Extreme! Lab.  For further information please visit
 *      http://www.extreme.indiana.edu/"
 *
 *    Alternatively, this acknowledgment may appear in the software
 *    itself, and wherever such third-party acknowledgments normally
 *    appear.
 *
 * 4) The name "Indiana University" or "Indiana University
 *    Extreme! Lab" shall not be used to endorse or promote
 *    products derived from this software without prior written
 *    permission from Indiana University.  For written permission,
 *    please contact http://www.extreme.indiana.edu/.
 *
 * 5) Products derived from this software may not use "Indiana
 *    University" name nor may "Indiana University" appear in their name,
 *    without prior written permission of the Indiana University.
 *
 * Indiana University provides no reassurances that the source code
 * provided does not infringe the patent or any other intellectual
 * property rights of any other entity.  Indiana University disclaims any
 * liability to any recipient for claims brought by any other entity
 * based on infringement of intellectual property rights or otherwise.
 *
 * LICENSEE UNDERSTANDS THAT SOFTWARE IS PROVIDED "AS IS" FOR WHICH
 * NO WARRANTIES AS TO CAPABILITIES OR ACCURACY ARE MADE. INDIANA
 * UNIVERSITY GIVES NO WARRANTIES AND MAKES NO REPRESENTATION THAT
 * SOFTWARE IS FREE OF INFRINGEMENT OF THIRD PARTY PATENT, COPYRIGHT, OR
 * OTHER PROPRIETARY RIGHTS.  INDIANA UNIVERSITY MAKES NO WARRANTIES THAT
 * SOFTWARE IS FREE FROM "BUGS", "VIRUSES", "TROJAN HORSES", "TRAP
 * DOORS", "WORMS", OR OTHER HARMFUL CODE.  LICENSEE ASSUMES THE ENTIRE
 * RISK AS TO THE PERFORMANCE OF SOFTWARE AND/OR ASSOCIATED MATERIALS,
 * AND TO THE PERFORMANCE AND VALIDITY OF INFORMATION GENERATED USING
 * SOFTWARE.
 */


