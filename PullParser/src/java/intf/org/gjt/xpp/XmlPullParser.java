/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: XmlPullParser.java,v 1.8 2004/05/06 18:18:45 aslom Exp $
 */

package org.gjt.xpp;

import java.io.IOException;
import java.io.Reader;

/**
 * Generic interface for simple and quick XML Pull Parser.
 *
 * @see XmlPullParserFactory
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public interface XmlPullParser {
    
    //  public static final XmlPullParserEvent END_DOCUMENT =
    //    XmlPullParserEvent.END_DOCUMENT;
    //
    //  public static final XmlPullParserEvent START_TAG =
    //    XmlPullParserEvent.START_TAG;
    //
    //  /** end tag was just read */
    //  public static final XmlPullParserEvent END_TAG =
    //    XmlPullParserEvent.END_TAG;
    //
    //  /** element content was just read */
    //  public static final XmlPullParserEvent CONTENT =
    //    XmlPullParserEvent.CONTENT;
    
    /** signal logical end of xml document */
    public final static byte END_DOCUMENT = 1;
    /** start tag was just read */
    public final static byte START_TAG = 2;
    /** end tag was just read */
    public final static byte END_TAG = 3;
    /** element content was just read */
    public final static byte CONTENT = 4;
    
    /**
     * Set the input for parser.
     */
    public void setInput(Reader in) throws XmlPullParserException;
    
    /**
     * Set the input for parser.
     */
    public void setInput(char[] buf) throws XmlPullParserException;
    
    /**
     * Set the input for parser.
     */
    public void setInput(char[] buf, int off, int len)
        throws XmlPullParserException;
    
    /**
     * Reset the parser state.
     */
    public void reset() throws XmlPullParserException;
    
    /**
     * Is mixed element context allowed?
     */
    public boolean isAllowedMixedContent();
    
    /**
     * Allow for mixed element content. Disabled by default.
     * When disbaled element must containt either text or other elements.
     *
     * <p><b>NOTE:</b> this is not resetable parameter.
     *
     */
    public void setAllowedMixedContent(boolean enable)
        throws XmlPullParserException;
    
    /**
     * Is parser namespace aware?
     */
    public boolean isNamespaceAware();
    
    /**
     * Indicate that the parser understands XML Namespaces
     *
     * <p><b>NOTE:</b> this is not resetable parameter.
     *
     */
    public void setNamespaceAware(boolean enable)
        throws XmlPullParserException;
    
    /**
     * Is parser going to report namespace attributes (xmlns*) ?
     */
    public boolean isNamespaceAttributesReporting();
    
    /**
     * Make parser to report xmlns* attributes. Disabled by default.
     * Only meaningful when namespaces are enabled (when namespaces
     * are disabled all attributes are always reported).
     */
    public void setNamespaceAttributesReporting(boolean enable)
        throws XmlPullParserException;
    
    /**
     * Returns the namespace URI of the current element
     * Returns null if not applicable
     * (current event must be START_TAG or END_TAG)
     */
    public String getNamespaceUri();
    
    /**
     * Returns the local name of the current element
     * (current event must be START_TAG or END_TAG)
     */
    public String getLocalName();
    
    /**
     * Returns the prefix of the current element
     * or null if elemet has no prefix.
     * (current event must be START_TAG or END_TAG)
     */
    public String getPrefix();
    
    
    /**
     * Returns the raw name (prefix + ':' + localName) of the current element
     * (current event must be START_TAG or END_TAG)
     */
    public String getRawName();
    
    /**
     * Return local part of qname.
     * For example for 'xsi:type' it returns 'type'.
     */
    public String getQNameLocal(String qName)
        throws XmlPullParserException;
    
    /**
     * Return uri part of qname.
     * It is depending on current state of parser to find
     * what namespace uri is mapped from namespace prefix.
     * For example for 'xsi:type' if xsi namespace prefix
     * was declared to 'urn:foo' it will return 'urn:foo'.
     */
    public String getQNameUri(String qName)
        throws XmlPullParserException;
    
    /**
     * Returns the current depth of the element.
     */
    public int getDepth();
    
    
    public int getNamespacesLength(int depth);
    
    /**
     * Return namespace prefixes for element at depth
     */
    public void readNamespacesPrefixes(int depth, String[] prefixes, int off, int len)
        throws XmlPullParserException;
    
    /**
     * Return namespace URIs for element at depth
     */
    public void readNamespacesUris(int depth, String[] uris, int off, int len)
        throws XmlPullParserException;
    
    // --- miscellaneous reporting methods
    
    /**
     *
     */
    public String getPosDesc();
    
    /**
     *
     */
    public int getLineNumber();
    
    /**
     *
     */
    public int getColumnNumber();
    
    
    // --- methods doing real work
    /**
     * Get next parsing event.
     * <p><b>NOTE:</b> empty element (such as &lt;tag/>) will be reported
     *  with just two events: START_TAG, END_TAG - it must be so to preserve
     *   parsing equivalency of empty element to &lt;tag>&lt;/tag>.
     */
    public byte next()
        throws XmlPullParserException, IOException;
    
    /**
     * Returns the type of the current element (START_TAG, END_TAG, CONTENT, etc)
     */
    public byte getEventType()
        throws XmlPullParserException;
    
    /**
     * Check if last CONTENT contained only whitespace characters.
     */
    public boolean isWhitespaceContent() throws XmlPullParserException;
    
    /**
     * Return how big is content.
     *
     * <p><b>NOTE:</b> parser must be on CONTENT event.
     */
    public int getContentLength()
        throws XmlPullParserException;
    
    /**
     * Read current content as Stirng.
     *
     * <p><b>NOTE:</b> parser must be on CONTENT event.
     */
    public String readContent()
        throws XmlPullParserException;
    
    /**
     * Read current end tag.
     *
     * <p><b>NOTE:</b> parser must be on END_TAG event.
     */
    public void readEndTag(XmlEndTag etag)
        throws XmlPullParserException;
    
    /**
     * Read current start tag.
     *
     * <p><b>NOTE:</b> parser must be on START_TAG event.
     */
    public void readStartTag(XmlStartTag stag)
        throws XmlPullParserException;
    
    /**
     * Read node: it calls readStartTag and then if parser is namespaces
     * aware currently declared nemaspeces will be added
     * and defaultNamespace will be set.
     *
     * <p><b>NOTE:</b> parser must be on START_TAG event.
     * and all events will written into node!
     */
    public void readNodeWithoutChildren(XmlNode node)
        throws XmlPullParserException;
    
    /**
     * Read subtree into node: call readNodeWithoutChildren
     * and then parse subtree adding children
     * (values obtained with readXontent or readNodeWithoutChildren).
     *
     * <p><b>NOTE:</b> parser must be on START_TAG event.
     * and all events will written into node!
     *
     */
    public byte readNode(XmlNode node)
        throws XmlPullParserException, IOException;
    
    /**
     * Goes directly to the next sibling
     *
     * <p><b>NOTE:</b> parser must be on START_TAG event.
     * and all intermittent events will be lost!
     *
     */
    public byte skipNode()
        throws XmlPullParserException, IOException;
    
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

