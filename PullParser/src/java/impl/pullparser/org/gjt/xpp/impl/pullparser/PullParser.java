/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: PullParser.java,v 1.13 2004/05/06 18:18:45 aslom Exp $
 */

package org.gjt.xpp.impl.pullparser;

import java.io.*;
import java.util.Hashtable;
import java.util.Stack;

import org.gjt.xpp.XmlEndTag;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserBufferControl;
import org.gjt.xpp.XmlPullParserEventPosition;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlStartTag;
import org.gjt.xpp.XmlNode;

// some code sharing
import org.gjt.xpp.impl.tag.Attribute;
import org.gjt.xpp.impl.tag.PullParserRuntimeException;

import org.gjt.xpp.impl.tokenizer.Tokenizer;


/**
 * XML Pull Parser (XPP) allows to pull  XML events from input stream.
 *
 * Advantages:<ul>
 * <li>very simple pull interface
 *     - ideal for deserializing XML objects (like SOAP)
 * <li>simple and efficient thin wrapper around Tokenizer class
 *    - when compared with using Tokenizer directly adds
 *      about 10%  for big documents,
 *      maximum 50% more processing time for small documents
 * <li>lightweight memory model - minimized memory allocation:
 *    element content and attributes are only read on explicit
 *       method calls,
 *    both StartTag and EndTag can be reused during parsing
 * <li>small - total compiled size around 20K
 * <li>by default supports namespaces parsing
 *    (can be switched off)
 * <li>support for mixed content can be explicitly disabled
 * </ul>
 *
 * Limitations: <ul>
 * <li>this is beta version - may have still bugs :-)
 * <li>does not parse DTD (recognizes only predefined entities)
 * </ul>
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class PullParser
    implements XmlPullParser, XmlPullParserBufferControl, XmlPullParserEventPosition
{
    /** signal logical end of xml document */
    //public final static byte END_DOCUMENT = 1;
    /** start tag was just read */
    //public final static byte START_TAG = 2;
    /** end tag was just read */
    //public final static byte END_TAG = 3;
    /** element content was just read */
    //public final static byte CONTENT = 4;

    /**
     * Create instance of pull parser.
     */
    public PullParser() {
        //if(USE_NAMECACHE) {
        //  nameCache = new NameCache();
        //}
        resetState();
    }

    /**
     * Reset parser and set new input.
     */
    public void setInput(Reader reader) {
        resetState();
        eventEnd = eventStart = 0;
        tokenizer.setInput(reader);
    }

    /**
     * Reset parser and set new input.
     */
    public void setInput(char[] buf) {
        resetState();
        eventEnd = eventStart =  0;
        tokenizer.setInput(buf, 0, buf.length);
    }

    public void setInput(char[] buf, int off, int len)
        throws XmlPullParserException
    {
        //throw new XmlPullParserException("not implemented");
        resetState();
        eventEnd = eventStart = off;
        tokenizer.setInput(buf, off, len);
    }

    /**
     * Reset parser state so it can be used to parse new
     */
    public void reset() {
        tokenizer.reset();
        resetState();
    }

    public boolean isAllowedMixedContent()
    {
        return tokenizer.isAllowedMixedContent();
    }

    /**
     * Allow for mixed element content.
     * Enabled by default.
     * When disbaled element must containt either text
     * or other elements.
     */
    public void setAllowedMixedContent(boolean enable)
    {
        tokenizer.setAllowedMixedContent(enable);
    }


    public boolean isNamespaceAware() { return supportNs; }

    /**
     * Set support of namespaces. Disabled by default.
     */
    public void setNamespaceAware(boolean awareness)
        throws XmlPullParserException
    {
        if(elStackDepth > 0 || seenRootElement) {
            throw new XmlPullParserException(
                "namespace support can only be set when not parsing");
        }
        supportNs = awareness;
    }

    public boolean isNamespaceAttributesReporting()
    {
        return reportNsAttribs;
    }

    /**
     * Make parser to report xmlns* attributes. Disabled by default.
     * Only meaningful when namespaces are enabled (when namespaces
     * are disabled all attributes are always reported).
     */
    public void setNamespaceAttributesReporting(boolean enable)
    {
        reportNsAttribs = enable;
    }

    public String getNamespaceUri()
    {
        if(state != START_TAG && state != END_TAG) {
            throw new PullParserRuntimeException(
                "no end or start tag available to read"+getPosDesc());
        }
        ElementContent el = elStack[getDepth() - 1];
        return el.uri;
    }

    public String getLocalName()
    {
        if(state != START_TAG && state != END_TAG) {
            throw new PullParserRuntimeException(
                "no end or start tag available to read"+getPosDesc());
        }
        ElementContent el = elStack[getDepth() - 1];
        return el.localName;
    }

    public String getPrefix()
    {
        if(state != START_TAG && state != END_TAG) {
            throw new PullParserRuntimeException(
                "no end or start tag available to read"+getPosDesc());
        }
        ElementContent el = elStack[getDepth() - 1];
        return el.prefix;
    }

    public String getRawName()
    {
        if(state != START_TAG && state != END_TAG) {
            throw new PullParserRuntimeException(
                "no end or start tag available to read"+getPosDesc());
        }
        ElementContent el = elStack[getDepth() - 1];
        return el.qName;
    }

    public String getQNameLocal(String qName) {
        int i = qName.lastIndexOf(':');
        return qName.substring(i + 1);
    }

    public String getQNameUri(String qName)
        throws XmlPullParserException
    {
        if(elStackDepth == 0)
            throw new XmlPullParserException(
                "parsing must be started to get uri from qname");
        int i = qName.lastIndexOf(':');
        if(i > 0) {
            String prefix = qName.substring(0, i);
            return (String) prefix2Ns.get(prefix);
        } else {
            return elStack[elStackDepth-1].defaultNs;
        }
    }

    public int getDepth()
    {
        return
            (state != END_TAG) ? elStackDepth : elStackDepth + 1;
    }

    public int getNamespacesLength(int depth)
    {
        if(depth <= 0)
            throw new IllegalArgumentException(
                "element depth must be bigger than zero");
        int maxDepth = getDepth();
        if(depth > maxDepth) {
            throw new IllegalArgumentException(
                "the depth "+depth+" that was passed for length of namespaces "+
                    " can not be bigger than current depth of "+maxDepth);
        }
        ElementContent el = elStack[ depth - 1 ];
        return el.prefixesEnd;
    }

    /**
     * Return namespace prefixes for element at depth
     */
    public void readNamespacesPrefixes(int depth, String[] prefixes, int off, int len)
        throws XmlPullParserException
    {
        if(depth <= 0)
            throw new XmlPullParserException(
                "element depth must be bigger than zero");
        int maxDepth = getDepth();
        if(depth > maxDepth) {
            throw new XmlPullParserException(
                "passed prefixes array of length "+depth+
                    " can not be bigger than current depth of "+maxDepth);
        }
        if(len == 0) {
            return;
        }
        ElementContent el = elStack[ depth - 1 ];
        if(el.prefixesEnd == 0) {
            return;
        }
        if(len > el.prefixesEnd) {
            throw new XmlPullParserException(
                "number of prefixes to copy "+len
                    +" is bigger than available "+el.prefixesEnd);
        }
        System.arraycopy(el.prefixes, 0, prefixes, off, len);
    }

    /**
     * Return namespace URIs for element at depth
     */
    public void readNamespacesUris(int depth, String[] uris, int off, int len)
        throws XmlPullParserException
    {
        if(depth <= 0)
            throw new XmlPullParserException(
                "element depth must be bigger than zero");
        int maxDepth = getDepth();
        if(depth > maxDepth) {
            throw new XmlPullParserException(
                "passed namespace URIs array of length "+depth+
                    " can not be bigger than current depth of "+maxDepth);
        }
        if(len == 0) {
            return;
        }
        ElementContent el = elStack[ depth - 1];
        if(el.prefixesEnd == 0) {
            return;
        }
        if(len > el.prefixesEnd) {
            throw new XmlPullParserException(
                "number of namespace URIs to copy "+len
                    +" is bigger than available "+el.prefixesEnd);
        }
        System.arraycopy(el.namespaceURIs, 0, uris, off, len);
    }

    /**
     * Return string describing current position of parser in input stream.
     */
    public String getPosDesc() {
        String desc;
        switch(state) {
            case START_TAG:
                desc = "START_TAG";
                break;
            case CONTENT:
                desc = "CONTENT";
                break;
            case END_TAG:
                desc = "END_TAG";
                break;
            case END_DOCUMENT:
                desc = "END_DOCUMENT";
                break;
            default:
                desc = "UNKNONW_EVENT ("+state+")";
        }
        return tokenizer.getPosDesc()+" (parser state "+desc+")";
    }

    public int getLineNumber() { return tokenizer.getLineNumber(); }

    public int getColumnNumber() { return tokenizer.getColumnNumber(); }

    /**
     * This is key method - it reads more from input stream
     * and returns next event type
     * (such as START_TAG, END_TAG, CONTENT).
     * or END_DOCUMENT if no more input.
     *
     * <p>This is simple automata (in pseudo-code):
     * <pre>
     * byte next() {
     *    while(state != END_DOCUMENT) {
     *      token = tokenizer.next();  // get next XML token
     *      switch(token) {
     *
     *      case Tokenizer.END_DOCUMENT:
     *        return state = END_DOCUMENT
     *
     *      case Tokenizer.CONTENT:
     *        // check if content allowed - only inside element
     *        return state = CONTENT
     *
     *     case Tokenizer.ETAG_NAME:
     *        // popup element from stack - compare if matched start and end tag
     *        // if namespaces supported restore namespaces prefix mappings
     *        return state = END_TAG;
     *
     *      case Tokenizer.STAG_NAME:
     *        // create new  element push it on stack
     *        // process attributes (including namespaces)
     *        // set emptyElement = true; if empty element
     *        // check atribute uniqueness (including nmespacese prefixes)
     *        return state = START_TAG;
     *
     *      }
     *    }
     * }
     * </pre>
     *
     * <p>Actual parsing is more complex especilly for start tag due to
     *   dealing with attributes reported separately from tokenizer and
     *   declaring namespace prefixes and uris.
     *
     *
     *
     */

    public byte next() throws XmlPullParserException, IOException {

        // update event start to current tokenizer position
        eventEnd = eventStart = tokenizer.pos;

        // MOTIVATION: save some memory by not reporting "" content
        //    if(emptyElementContent) {
        //      emptyElementContent = false;
        //      elContent = "";
        //      return state = CONTENT;
        //    }

        if(emptyElement) {
            --elStackDepth;  //TODO
            if(elStackDepth == 0)
                seenRootElement = true;
            emptyElement = false;
            return state = END_TAG;
        }
        Attribute ap = null;
        ElementContent el = null;
        //try {

        do {
            String s;
            token = tokenizer.next();
            //System.err.println("got token="+token);

            switch(token) {

                case Tokenizer.END_DOCUMENT:
                    // update event start to current tokenizer position
                    state = END_DOCUMENT;
                    if(elStackDepth > 0) {
                        throw new XmlPullParserException(
                            "expected element end tag '"
                                +elStack[elStackDepth-1].qName+"' not end of document"
                                +getPosDesc(), getLineNumber(), getColumnNumber());
                    }
                    // SAX endDocument
                    eventEnd = tokenizer.pos;
                    return state;

                case Tokenizer.CONTENT:
                    state = CONTENT;
                    if(elStackDepth > 0) {
                        elContent = null;
                        //SAX characters(...)
                        eventEnd = tokenizer.posEnd;
                        return state;
                    } else if(tokenizer.seenContent) {
                        throw new XmlPullParserException(
                            "only whitespace content allowed outside root element"
                                +getPosDesc(), getLineNumber(), getColumnNumber());
                    }
                    // else ignored --> allow whitespace content outside of root element
                    eventEnd = eventStart = tokenizer.pos;
                    break;

                case Tokenizer.ETAG_NAME:
                    // include leading </ in tag
                    eventStart = tokenizer.posStart - 2;
                    state = END_TAG;
                    if(seenRootElement)
                        throw new XmlPullParserException(
                            "no markup allowed outside root element"+getPosDesc(),
                            getLineNumber(), getColumnNumber());
                    //scache.convert(tokenizer.buf, tokenizer.posStart, tokenizer.posEnd, false); //FIXME
                    --elStackDepth;
                    if(elStackDepth == 0)
                        seenRootElement = true;
                    if(elStackDepth < 0)
                        throw new XmlPullParserException(
                            "end tag without start stag"+getPosDesc(), getLineNumber(), getColumnNumber());

                    el = elStack[elStackDepth];

                    //        if(!USE_QNAMEBUF) {
                    //          //if(USE_NAMECACHE) {
                    //          //  s =  nameCache.lookup(
                    //          //    tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                    //          //} else {
                    //if(!USE_CSC) {
                    s =  new String( //HOT 30%
                        tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                    //                    } else {
                    //                        s = stringConverter.convert(
                    //                            tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                    //                    }
                    //}
                    //                    if((!USE_CSC && !s.equals(elStack[elStackDepth].qName))
                    //                       || (USE_CSC && s != elStack[elStackDepth].qName))
                    if(!s.equals(elStack[elStackDepth].qName))
                        throw new XmlPullParserException(
                            "end tag name should be "+elStack[elStackDepth].qName
                                +" not "+s+getPosDesc(),
                            getLineNumber(), getColumnNumber());
                    //        } else {
                    //          int len = tokenizer.posEnd - tokenizer.posStart;
                    //          for (int i = 0; i < len; i++)
                    //          {
                    //            if(el.qNameBuf[i] != tokenizer.buf[i + tokenizer.posStart]) {
                    //              s =  new String(
                    //                tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                    //              throw new XmlPullParserException(
                    //                "end tag name should be "+elStack[elStackDepth].qName
                    //                  +" not "+s+getPosDesc(),
                    //                getLineNumber(), getColumnNumber());
                    //            }
                    //          }
                    //        }
                    // restore declared namespaces
                    if(supportNs && el.prefixes != null) {
                        //NOTE: it is in REVERSE order!
                        for(int i = el.prefixesEnd - 1; i >= 0; --i) {
                            //SAX2 endPrefixMapping(el.prefixes[i]);
                            //System.err.println("el="+el);
                            if( el.prefixPrevNs[i] != null) {
                                prefix2Ns.put( el.prefixes[i], el.prefixPrevNs[i] );
                            } else {
                                prefix2Ns.remove( el.prefixes[i] );
                            }
                        }
                        //el.prefixesEnd = 0; it will make impossible to retrieve readNamespaces...
                        //if(elStackDepth > 0 && !el.defaultNs.equals(elStack[elStackDepth-1].defaultNs)
                        //SAX2 endPrefixMapping("", el.defaultNs) ???
                    }
                    //SAX2 endElement(el.uri, el.localName, el.qName)
                    eventEnd = tokenizer.pos;
                    return state;

                case Tokenizer.STAG_NAME:
                    // include leading < in tag
                    eventStart = tokenizer.posStart - 1;
                    state = START_TAG;
                    if(seenRootElement)
                        throw new XmlPullParserException(
                            "no markup allowed outside root element"+getPosDesc(), getLineNumber(), getColumnNumber());
                    //beforeAtts = true;
                    //emptyElementContent =
                    emptyElement = false;
                    if(elStackDepth >= elStackSize) {
                        ensureCapacity(elStackDepth + 1);
                    }
                    el = elStack[elStackDepth];
                    el.prefixesEnd = 0;
                    attrPosEnd = 0;
                    el.defaultNs = null;
                    //if(USE_NAMECACHE) {
                    //  el.qName =  nameCache.lookup(
                    //    tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                    //} else {

                    //                    if(!USE_CSC) {
                    el.qName = new String(
                        tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                    //                    } else {
                    //                        el.qName  = stringConverter.convert(
                    //                            tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                    //                    }

                    //}

                    //        if(USE_QNAMEBUF) {
                    //          int len = tokenizer.posEnd - tokenizer.posStart;
                    //          if(el.qNameBuf == null || len > el.qNameBuf.length) {
                    //            el.qNameBuf = new char[len < 8 ? 8 : len + 8];
                    //          }
                    //          //System.arraycopy (tokenizer.buf, tokenizer.posStart, el.qNameBuf, 0, len);
                    //          for(int i = 0; i < len; ++i)  {
                    //            el.qNameBuf[i] = tokenizer.buf[ i + tokenizer.posStart ];
                    //          }
                    //        }

                    if(supportNs && tokenizer.posNsColon >= 0) {
                        if(tokenizer.nsColonCount > 1)
                            throw new XmlPullParserException(
                                "only one colon allowed in prefixed element name"
                                    +getPosDesc(), getLineNumber(), getColumnNumber());
                        el.prefix =
                            el.qName.substring(0, tokenizer.posNsColon - tokenizer.posStart);
                        el.localName =
                            el.qName.substring(tokenizer.posNsColon - tokenizer.posStart + 1);
                    } else {
                        el.prefix = null;
                        el.localName = el.qName;
                    }
                    el.uri = null;

                    ++elStackDepth;
                    break;

                case Tokenizer.ATTR_NAME:
                    if(attrPosEnd >= attrPosSize) ensureAttribs(attrPosEnd + 1);
                    ap = attrPos[attrPosEnd];

                    //if(USE_NAMECACHE) {
                    //  ap.qName =  nameCache.lookup(
                    //    tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                    //} else {
                    //ap.qName = new String(
                    //tokenizer.buf,tokenizer.posStart,tokenizer.posEnd-tokenizer.posStart);
                    //                    if(!USE_CSC) {
                    ap.qName = new String(
                        tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                    //                    } else {
                    //                        ap.qName  = stringConverter.convert(
                    //                            tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                    //                    }


                    //}
                    ap.uri = null;
                    if(supportNs && tokenizer.posNsColon >= 0) {
                        if(tokenizer.nsColonCount > 1)
                            throw new XmlPullParserException(
                                "only one colon allowed in prefixed attribute name"
                                    +getPosDesc(), getLineNumber(), getColumnNumber());
                        //ap.prefix = ap.qName.substring(
                        //    0, tokenizer.posNsColon - tokenizer.posStart);

                        //                        if(!USE_CSC) {
                        ap.prefix = ap.qName.substring(
                            0, tokenizer.posNsColon - tokenizer.posStart);
                        //                        } else {
                        //                            ap.prefix  = stringConverter.convert(
                        //                                tokenizer.buf, tokenizer.posStart, tokenizer.posNsColon - tokenizer.posStart);
                        //                        }


                        if(tokenizer.posEnd == tokenizer.posNsColon) {
                            throw new XmlPullParserException(
                                "xmlns: is not allowed to declare default namespace,"+
                                    " use xmlns instead"
                                    +getPosDesc(), getLineNumber(), getColumnNumber());
                        }
                        //ap.localName =
                        //    ap.qName.substring(tokenizer.posNsColon - tokenizer.posStart + 1);

                        //                        if(!USE_CSC) {
                        ap.localName =
                            ap.qName.substring(tokenizer.posNsColon - tokenizer.posStart + 1);
                        //                        } else {
                        //                            ap.localName  = stringConverter.convert(
                        //                                tokenizer.buf, tokenizer.posNsColon + 1,
                        //                                tokenizer.posEnd - tokenizer.posNsColon - 1);
                        //                        }

                    } else {
                        ap.prefix = null;
                        ap.localName = ap.qName;
                    }
                    break;

                case Tokenizer.ATTR_CONTENT:

                    if(tokenizer.parsedContent) {
                        //if(USE_NAMECACHE) {
                        //  ap.value =  nameCache.lookup(
                        //    tokenizer.pc, tokenizer.pcStart, tokenizer.pcEnd -tokenizer.pcStart);
                        //} else {
                        ap.value = new String(
                            tokenizer.pc, tokenizer.pcStart, tokenizer.pcEnd -tokenizer.pcStart);
                        //}
                    } else {
                        //if(USE_NAMECACHE) {
                        //  ap.value =  nameCache.lookup(
                        //    tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                        //} else {
                        ap.value = new String(
                            tokenizer.buf,tokenizer.posStart,tokenizer.posEnd-tokenizer.posStart);
                        //}
                    }
                    if(supportNs) {
                        //                        if((!USE_CSC && "xmlns".equals(ap.prefix))
                        //                          || (USE_CSC && "xmlns" == ap.prefix)) {
                        if("xmlns".equals(ap.prefix)) {
                            // add new NS prefix
                            if(el.prefixesEnd >= el.prefixesSize) {
                                el.ensureCapacity(el.prefixesEnd);
                            }
                            if(ap.value.length() == 0) {
                                // http://www.w3.org/TR/1999/REC-xml-names-19990114/#ns-decl
                                throw new XmlPullParserException(
                                    "the declared xmlns namespace for '"+ap.localName+"' name "
                                        +" may not be empty"
                                        +getPosDesc(), getLineNumber(), getColumnNumber());
                            }
                            el.prefixes[el.prefixesEnd] = ap.localName;
                            el.namespaceURIs[el.prefixesEnd] = ap.value;
                            el.prefixPrevNs[el.prefixesEnd] =
                                (String) prefix2Ns.get(ap.localName);
                            if(CHECK_ATTRIB_UNIQ) {
                                //NOTE: O(n2) complexity but n is very small...
                                //System.err.println("checking xmlns name="+ap.localName);
                                for(int i = 0; i < el.prefixesEnd; ++i) {
                                    //                                    if((!USE_CSC && ap.localName.equals(el.prefixes[i]))
                                    //                                      || (USE_CSC && ap.localName == el.prefixes[i])) {
                                    if(ap.localName.equals(el.prefixes[i])) {
                                        throw new XmlPullParserException(
                                            "duplicate xmlns declaration name '"+ap.localName+"'"
                                                +getPosDesc(), getLineNumber(), getColumnNumber());
                                    }
                                }
                            }
                            ++el.prefixesEnd;
                            //System.err.println("adding prefix="+att.localName+" ns="+att.value);
                            prefix2Ns.put(ap.localName, ap.value);
                            //SAX2 startPrefixMapping(ap.localName, ap.value)
                            if(reportNsAttribs) {
                                ap.xmlnsAttrib = true;
                                ++attrPosEnd;
                            }
                            //                        } else if((!USE_CSC && "xmlns".equals(ap.qName))
                            //                                 || (USE_CSC && "xmlns" == ap.qName)) {
                        } else if("xmlns".equals(ap.qName)) {
                            if(el.defaultNs != null)
                                throw new XmlPullParserException(
                                    "default namespace was alredy declared by xmlns attribute"
                                        +getPosDesc(), getLineNumber(), getColumnNumber());
                            //System.err.println("adding default ns="+att.value);
                            el.defaultNs = ap.value;
                            if(reportNsAttribs) {
                                ap.xmlnsAttrib = true;
                                ++attrPosEnd;
                            }
                        } else {
                            //SAX2 if namespace-prefixes always add
                            ap.xmlnsAttrib = false;
                            ++attrPosEnd; // new attribute is added to list
                        }
                    } else { // ! supportNs
                        if(CHECK_ATTRIB_UNIQ) {
                            //NOTE: O(n2) complexity but n is small...
                            for(int i = 0; i < attrPosEnd; ++i) {
                                //                                if((!USE_CSC && ap.qName.equals(attrPos[i].qName))
                                //                                   ||(USE_CSC && ap.qName == attrPos[i].qName)) {
                                if(ap.qName.equals(attrPos[i].qName)) {
                                    throw new XmlPullParserException(
                                        "duplicate attribute name '"+ap.qName+"'"
                                            +getPosDesc(), getLineNumber(), getColumnNumber());
                                }
                            }
                        }
                        ap.xmlnsAttrib = false;
                        ++attrPosEnd; // new attribute is added to list
                    }
                    break;

                case Tokenizer.EMPTY_ELEMENT:
                    //emptyElementContent =
                    emptyElement = true;
                    break;

                case Tokenizer.STAG_END:
                    if(supportNs) {
                        if(el.defaultNs == null) {
                            if(elStackDepth > 1) {
                                el.defaultNs = elStack[elStackDepth-2].defaultNs;
                            } else {
                                el.defaultNs = "";
                            }
                        } // else //SAX2 startPrefixMapping("", el.defaultNs) ???
                        //          if(el.uri == null) {
                        //            el.uri = el.defaultNs;
                        //          }

                        //System.err.println("el default ns="+el.defaultNs);
                        if(el.prefix != null) {
                            el.uri = (String) prefix2Ns.get(el.prefix);
                            if(el.uri  == null)
                                throw new XmlPullParserException(
                                    "no namespace for prefix '"+el.prefix+"'"
                                        +getPosDesc(), getLineNumber(), getColumnNumber());
                            // assert(stag.uri != null);
                        } else {
                            //stag.uri = "";
                            el.uri = el.defaultNs;
                            //System.err.println("setting el default uri="+stag.uri);
                            el.localName = el.qName;
                        }

                        for(int j = 0; j < attrPosEnd ; ++j) {
                            ap = attrPos[j];
                            if(! ap.xmlnsAttrib && ap.uri == null && ap.prefix != null) {
                                ap.uri = (String) prefix2Ns.get(ap.prefix);
                                if(ap.uri  == null)
                                    throw new XmlPullParserException(
                                        "no namespace for prefix "+ap.prefix+getPosDesc(),
                                        getLineNumber(), getColumnNumber());
                            }
                        }
                        if(CHECK_ATTRIB_UNIQ) {
                            //when namesapces: only now can check attribute uniquenes
                            //NOTE: O(n2) complexity but n is small...
                            for(int j = 1; j < attrPosEnd ; ++j) {
                                ap = attrPos[j];
                                for(int i = 0; i < j; ++i) {
                                    if(! ap.xmlnsAttrib && ! attrPos[i].xmlnsAttrib
                                       //                                       && ((!USE_CSC && ap.localName.equals(attrPos[i].localName))
                                       //                                          || (USE_CSC && ap.localName == attrPos[i].localName))
                                       && (ap.localName.equals(attrPos[i].localName))
                                       && ( (ap.uri != null && ap.uri.equals(attrPos[i].uri))
                                               || (ap.uri == null && attrPos[i].uri == null))
                                      ) {
                                        throw new XmlPullParserException(
                                            "duplicate attribute name '"+ap.qName+"'"
                                                +((ap.uri != null) ?
                                                      " (with namespace '"+ap.uri+"')" : "")
                                                +" and "
                                                +((ap.uri != null) ?
                                                      " (with namespace '"+ap.uri+"')" : "")
                                                +getPosDesc(), getLineNumber(), getColumnNumber());
                                    }
                                }
                            }
                        }

                    } else { // no namespaces
                        el.prefix = null;
                        el.localName = el.qName;
                        el.uri = "";
                    }
                    //SAX2 startElement(el.uri, el.localName, el.qName, el)
                    eventEnd = tokenizer.pos;
                    return state;

                case Tokenizer.DOCTYPE:
                    throw new XmlPullParserException(
                        "<![DOCTYPE declarations not supported"
                            +getPosDesc(), getLineNumber(), getColumnNumber());

                default:
                    throw new XmlPullParserException("unknown token "+token
                                                         +getPosDesc(),
                                                     getLineNumber(), getColumnNumber());
            }
        } while(token != Tokenizer.STAG_END);
        //} catch(TokenizerException ex) {
        //  throw new XmlPullParserException("tokenizer exception", ex);
        //}
        throw new XmlPullParserException(
            "invalid state of tokenizer token="+token);
    }

    public byte getEventType()
    {
        return state;
    }


    /**
     * Return true if just read CONTENT contained only white spaces.
     */
    public boolean isWhitespaceContent() throws XmlPullParserException {
        if(state != CONTENT)
            throw new XmlPullParserException("no content available to read"
                                            +getPosDesc(), getLineNumber(), getColumnNumber());
        return tokenizer.seenContent == false;
    }

    public int getContentLength() throws XmlPullParserException {
        if(state != CONTENT)
            throw new XmlPullParserException("no content available"
                                                 +getPosDesc(), getLineNumber(), getColumnNumber());
        return tokenizer.parsedContent ?
            (tokenizer.pcEnd - tokenizer.pcStart) :
            (tokenizer.posEnd - tokenizer.posStart);
    }

    /**
     * Return String that contains just read CONTENT.
     */
    public String readContent() throws XmlPullParserException {
        if(state != CONTENT)
            throw new XmlPullParserException("no content available to read"
                                                 +getPosDesc(), getLineNumber(), getColumnNumber());
        if(elContent == null) {
            if(tokenizer.parsedContent) {
                //      int pcLen = tokenizer.pcEnd - tokenizer.pcStart;
                //      if(USE_NAMECACHE && pcLen < 8) {
                //        elContent = nameCache.lookup(
                //          tokenizer.pc, tokenizer.pcStart, pcLen);
                //        //scache.convert(tokenizer.pc, tokenizer.posStart, tokenizer.posEnd, true);
                //      } else {
                elContent = new String(
                    tokenizer.pc, tokenizer.pcStart, tokenizer.pcEnd - tokenizer.pcStart);
                //      }
            } else {
                //System.out.println("posStart="+tokenizer.posStart+" posEnd="+tokenizer.posEnd);
                //      int bufLen = tokenizer.posEnd - tokenizer.posStart;
                //      if(USE_NAMECACHE && bufLen < 8) {
                //        elContent = nameCache.lookup(
                //          tokenizer.buf, tokenizer.posStart, bufLen);
                //        //scache.convert(tokenizer.buf, tokenizer.posStart, tokenizer.posEnd, true);
                //      } else {
                //elContent = new String(
                //    tokenizer.buf, tokenizer.posStart, tokenizer.posEnd - tokenizer.posStart);
                int len = tokenizer.posEnd - tokenizer.posStart;
                //                if(!USE_CSC ||  (USE_CSC && len > 3)) {
                elContent = new String(
                    tokenizer.buf, tokenizer.posStart, len);
                //                } else {
                //                    elContent  = stringConverter.convert(
                //                        tokenizer.buf, tokenizer.posStart, len);
                //                }


                //      }
            }
        }
        return elContent;
    }

    /**
     * Read value of just read END_TAG into passed as argument EndTag.
     */
    public void readEndTag(XmlEndTag etag) throws XmlPullParserException
    {
        if(state != END_TAG)
            throw new XmlPullParserException(
                "no end tag available to read"+getPosDesc(), getLineNumber(), getColumnNumber());
        //etag.qName = elStack[elStackDepth].qName;
        //etag.uri = elStack[elStackDepth].uri;
        //etag.localName = elStack[elStackDepth].localName;
        ElementContent el = elStack[elStackDepth];

        etag.resetEndTag();

        etag.modifyTag(el.uri,
                       el.localName,
                       el.qName);
    }

    /**
     * Read value of just read START_TAG into passed as argument StartTag.
     */
    public void readStartTag(XmlStartTag stag)
        throws XmlPullParserException
    {
        if(state != START_TAG)
            throw new XmlPullParserException(
                "no start tag available to read"+getPosDesc(), getLineNumber(), getColumnNumber());
        //if(beforeAtts == false)
        //  throw new XmlPullParserException(
        //    "start tag was already read"+getPosDesc(), getLineNumber(), getColumnNumber());

        stag.resetStartTag();

        ElementContent el = elStack[elStackDepth - 1];
        //stag.qName = el.qName;
        //stag.uri = el.uri;
        //stag.localName = el.localName;
        stag.modifyTag(el.uri,
                       el.localName,
                       el.qName);

        // process atttributes
        stag.ensureAttributesCapacity(attrPosEnd);

        //stag.attEnd = attrPosEnd;
        for(int i = 0; i < attrPosEnd; ++i) {
            Attribute ap = attrPos[i];

            // place for next attribute value
            //Attribute att = stag.attArr[i];
            //att.qName =  ap.qName;
            //att.localName = ap.localName;
            //att.value = ap.value;
            //att.uri = ap.uri;
            stag.addAttribute(
                ap.uri,
                ap.localName,
                ap.qName,
                ap.value,
                ap.xmlnsAttrib);

        }
    }

    public void readNodeWithoutChildren(XmlNode node)
        throws XmlPullParserException
    {
        readStartTag(node);
        if(supportNs) {
            ElementContent el = elStack[elStackDepth-1];
            node.setDefaultNamespaceUri(el.defaultNs);
            //System.err.println("adding my namespace size="+el.prefixesEnd);
            //    for(int i = 0; i < el.prefixesEnd; i++) {
            //    System.err.println("adding"+
            //      " el.prefixes["+i+"] = "+el.prefixes[i]+
            //      " el.namespaceURIs["+i+"] = "+el.namespaceURIs[i]);
            //    }
            node.addDeclaredNamespaces(
                el.prefixes, 0, el.prefixesEnd, el.namespaceURIs);
        }
        //       (state != END_TAG) ? elStackDepth : elStackDepth + 1;
    }

    public byte readNode(XmlNode node)
        throws XmlPullParserException, IOException
    {
        readNodeWithoutChildren(node);
        ElementContent el = elStack[elStackDepth-1];
        int level = elStackDepth;
        //byte type = PullParser.END_TAG;
        //Stack stack = new Stack();
        //stack.push(node);
        while(true) {
            byte state = next();
            switch(state) {
                case XmlPullParser.START_TAG:
                    XmlNode child = node.newNode();
                    readNodeWithoutChildren(child);
                    //System.err.println("read "+child);
                    //        readStartTag(child);
                    //        if(supportNs) {
                    //          child.setDefaultNamespaceUri(el.defaultNs);
                    //          //System.err.println("adding namespace size="+el.prefixesEnd);
                    //          child.addDeclaredNamespaces(
                    //            el.prefixes, 0, el.prefixesEnd, el.namespaceURIs);
                    //        }
                    node.appendChild(child);
                    el = elStack[elStackDepth-1];
                    el.node = node;
                    node = child;
                    break;
                case XmlPullParser.CONTENT:
                    // skip element content when it is empty string "" ...
                    //if(eventEnd > eventStart) {
                    node.appendChild(readContent());
                    //}
                    break;
                case XmlPullParser.END_TAG:
                    if(elStackDepth >= level) {
                        node = elStack[elStackDepth].node;
                    } else {
                        return state;
                    }
                    break;
            }
        }
    }

    /**
     * If parser has just read start tag it allows to skip whoole
     * subtree contined in this element. Returns when encounters
     * end tag matching the start tag.
     */
    public byte skipNode() throws XmlPullParserException, IOException {
        if(state != START_TAG) {
            throw new XmlPullParserException(
                "start tag must be read before skiping subtree"+getPosDesc(),
                getLineNumber(), getColumnNumber());
        }
        int level = 1;
        byte type = XmlPullParser.END_TAG;
        while(level > 0) {
            type = next();
            switch(type) {
                case XmlPullParser.START_TAG:
                    ++level;
                    break;
                case XmlPullParser.END_TAG:
                    --level;
                    break;
            }
        }
        return type;
    }


    // low level API: interface to parser internal data - good for perf

    public int getHardLimit()
    {
        return tokenizer.getHardLimit();
    }

    public void setHardLimit(int value) throws XmlPullParserException {
        //try {
        tokenizer.setHardLimit(value);
        //} catch(TokenizerException ex) {
        //  throw new XmlPullParserException("could not set hard limit to "+value, ex);
        //}
    }

    public int getSoftLimit()
    {
        return tokenizer.getSoftLimit();
    }

    public void setSoftLimit(int value) throws XmlPullParserException {
        //try {
        tokenizer.setSoftLimit(value);
        //} catch(TokenizerException ex) {
        //  throw new XmlPullParserException("could not set soft limit to "+value, ex);
        //}
    }

    public int getBufferShrinkOffset()
    {
        return tokenizer.getBufferShrinkOffset();
    }

    public void setBufferShrinkable(boolean shrinkable) throws XmlPullParserException
    {
        tokenizer.setBufferShrinkable(shrinkable);
    }

    public boolean isBufferShrinkable()
    {
        return tokenizer.isBufferShrinkable();
    }


    public int getEventStart()
    {
        return eventStart;
    }

    // eventStart = tokenizer.pos, parsing..., eventEnd = tokenizer.pos }
    public int getEventEnd()
    {
        return eventEnd;
    }

    // equivalent to tokenizer.buf ALWAYS (never tokenizer.pc)
    public char[] getEventBuffer()
    {
        return tokenizer.buf;
    }


    // ====== utility methods
    /**
     * Make sure that we have enough space to keep element stack if passed size.
     */
    protected void ensureCapacity(int size) {
        int newSize = 2 * size;
        if(newSize == 0)
            newSize = 8; // = lucky 7 + 1 //25
        //if(elStackSize < newSize) {
        if(elStackSize < size) {
            if(TRACE_SIZING) {
                System.err.println("elStack "+elStackSize+" ==> "+newSize);
            }
            ElementContent[] newStack = new ElementContent[newSize];
            if(elStack != null) {
                System.arraycopy(elStack, 0, newStack, 0, elStackSize);
            }
            for(int i = elStackSize; i < newSize; ++i) {
                newStack[i] = new ElementContent();
            }
            elStack = newStack;
            elStackSize = newSize;
        }
    }

    /**
     * Make sure that in attributes temporary array is enough space.
     */
    protected  void ensureAttribs(int size) {
        int newSize = 2 * size;
        if(newSize == 0)
            newSize = 8; // = lucky 7 + 1 //25
        //if(attrPosSize < newSize) {
        if(attrPosSize < size) {
            if(TRACE_SIZING) {
                System.err.println("attrPos "+attrPosSize+" ==> "+newSize);
            }
            Attribute[] newAttrPos = new Attribute[newSize];
            if(attrPos != null) {
                System.arraycopy(attrPos, 0, newAttrPos, 0, attrPosSize);
            }
            for(int i = attrPosSize; i < newSize; ++i) {
                newAttrPos[i] = new Attribute(); //inner classes are weird  :-)
            }
            attrPos = newAttrPos;
            attrPosSize = newSize;
        }
    }

    protected void resetState() {
        tokenizer.paramNotifyDoctype = true;
        token = state = -1;
        eventStart = -1;
        elStackDepth = 0;
        prefix2Ns.clear();
        // 4. NC: Prefix Declared
        prefix2Ns.put("xml", "http://www.w3.org/XML/1998/namespace");
        //beforeAtts = false;
        //emptyElementContent =
        emptyElement = false;
        seenRootElement = false;
        //scache = new StringCache();
    }


    // ===== internals
    private final static boolean TRACE_SIZING = false;

    protected final static boolean USE_QNAMEBUF = false;

    //protected final static boolean USE_CSC = false;

    //protected final static boolean USE_NAMECACHE = true;
    //NameCache nameCache;

    /**
     * Should attribute uniqueness be checked for attributes
     * as in specified XML and NS specifications?
     */
    protected final static boolean CHECK_ATTRIB_UNIQ = true;
    //private boolean beforeAtts;

    /** Have we read empty element? */
    protected boolean emptyElement;

    ///** just to make that empty element content must be emitted */
    //protected boolean emptyElementContent;

    /** Have we seen root element */
    protected boolean seenRootElement;
    /** Content of current element if in CONTENT state */
    protected String elContent;

    /** XML tokenizer that is doing actual tokenizning of input stream. */
    protected Tokenizer tokenizer = new Tokenizer();
    /** start position of current event in tokenizer biffer */
    protected int eventStart;
    /** end position of current event in tokenizer biffer */
    protected int eventEnd;
    /** what is current event type as returned from next()? */
    protected byte state;
    /** what is current token returned from tokeizer */
    protected byte token;

    //private StringCache scache = new StringCache(); //FIXME

    // mapping namespace prefixes to uri
    /** should parser support namespaces? */
    protected boolean supportNs;

    /** should parser report namespace xmlns* attributes ? */
    protected boolean reportNsAttribs;

    /** mapping of names prefixes to uris */
    protected Hashtable prefix2Ns = new Hashtable();

    /*
     private String stagValue;
     private String stagPC;
     private int stagEnd;
     private int stagPCStart;
     private int stagPCEnd;
     private int stagValueStart;
     private int stagValueEnd;
     private int stagPosNsColon;
     //private int stagNsColonCount;
     */
    /** index for last attribute in attrPos array */
    protected int attrPosEnd;
    /** size of attrPos array */
    protected int attrPosSize;

    /** temporary array of current attributes */
    protected Attribute attrPos[];

    // for validating element pairing and string namespace context
    /** how many elements are on elStack */
    protected int elStackDepth;
    /** size of elStack array */
    protected int elStackSize;
    /** temprary array to keep ElementContent stack */
    protected ElementContent[] elStack;

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

