/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: XmlNode.java,v 1.6 2004/05/06 18:18:45 aslom Exp $
 */

package org.gjt.xpp;

import java.io.*;
import java.util.Enumeration;

/**
 * This class represents XML subtree.
 * XmlNode is extension of XmlStartTag adding support for children
 * (some of them may be also XmlNode so we get recursively built tree)
 * and namespaces declarations associated with this node.
 * When node has no children and namespaces declared it is
 * essentially equivalent to XmlStartTag.
 *
 * <p>When XmlNode user does not need namespaces then namespace related
 * methods may be ignored. However to use namespaces it is required to:<ul>
 * <li>call setDefaultNamespaceUri() to associate default namespace
 *    in which this node is declared
 * <li>and to declare additional namespaces (as of xmlns:prefix="...")
 *    with calling first removeNamespaces() and then addNamespaces()
 * </ul>
 *
 * @see XmlStartTag
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public interface XmlNode extends XmlStartTag {
    
    /**
     * Clear all Tag state to default values.
     */
    public void resetNode();
    
    /** context sensitive factory method to create the same type of node */
    public XmlNode newNode() throws XmlPullParserException;
    
    public XmlNode newNode(String amespaceUri, String localName)  throws XmlPullParserException;
        
    public XmlNode getParentNode();
    
    public void setParentNode(XmlNode parent) throws XmlPullParserException;
    
    //  public Object getFirstChild()
    //    throws XmlPullParserException;
    
    //  public Object getNextSibling()
    //    throws XmlPullParserException;
    
    public Enumeration children();
    
    /** it may need to reconsruct whole subtree to get count ... */
    public int getChildrenCount();
    
    public Object getChildAt(int pos);
    
    // ---------------- modifiable
    
    public void appendChild(Object child) throws XmlPullParserException;
    
    public void insertChildAt(int pos, Object child) throws XmlPullParserException;
    
    public void removeChildAt(int pos) throws XmlPullParserException;
    
    public void replaceChildAt(int pos, Object child) throws XmlPullParserException;
    
    public void ensureChildrenCapacity(int minCapacity) throws XmlPullParserException;
    
    /**
     * Removes all children - every child that was
     * implementing XmlNode will have set parent to null.
     */
    public void removeChildren() throws XmlPullParserException;
    
    // ---------------- namespace related methods -- pain...
    
    //NOTE: this is initialized based on "namespace" xmlns attributes...
    
    /**
     * Return local part of qname.
     * For example for 'xsi:type' it returns 'type'.
     */
    public String getQNameLocal(String qName) throws XmlPullParserException;
    
    /**
     * Return uri part of qname.
     * The return value is dependent on declared namespaces in this
     * node and possible when looking for value in parent node.
     * For example for 'xsi:type' if xsi namespace prefix
     * was declared to 'http://foo' it will return 'http://foo'.
     */
    public String getQNameUri(String qName) throws XmlPullParserException;
    
    /** return namespace for prefix searching node tree upward. */
    public String prefix2Namespace(String prefix) throws XmlPullParserException;
    
    /** return prefix for namesapce searching node tree upward. */
    public String namespace2Prefix(String namespaceUri) throws XmlPullParserException;
    
    /** Namesapce URI associated with default namesapce prefix (xmlns='....') */
    public String getDefaultNamespaceUri();
    
    /** Set default namesapce URI (xmlns='....') */
    public void setDefaultNamespaceUri(String defaultNamespaceUri) throws XmlPullParserException;
    
    public int getDeclaredNamespaceLength();
    
    public void readDeclaredNamespaceUris(String[] uris, int off, int len)
        throws XmlPullParserException;
    
    public void readDeclaredPrefixes(String[] prefixes, int off, int len)
        throws XmlPullParserException;
    
    public void ensureDeclaredNamespacesCapacity(int minCapacity) throws XmlPullParserException;
    
    public void addNamespaceDeclaration(
        String prefix, String namespaceUri)
        throws XmlPullParserException;
    
    /**
     * <b>NOTE:</b> node SHOULD NOT keep references to passed arrays!
     */
    public void addDeclaredNamespaces(
        String[] prefix, int off, int len, String[] namespaceUri)
        throws XmlPullParserException;
    
    public void removeDeclaredNamespaces() throws XmlPullParserException;
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

