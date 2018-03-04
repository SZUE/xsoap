/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: XmlStartTag.java,v 1.8 2004/05/06 18:18:45 aslom Exp $
 */

package org.gjt.xpp;

/**
 * This class represents abstract functionality necessary to
 * to persist XML Pull Parser events.
 *
 * @see XmlTag
 * @see XmlNode
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public interface XmlStartTag extends XmlTag {

    /**
     * Clear all Tag state to default values.
     */
    public void resetStartTag();

    /**
     * Returns the number of attributes on the current element
     */
    public int getAttributeCount();

    /**
     * Returns the namespace URI of the specified attribute
     *  number index (starts from 0).
     * (meaningful only if namespaces enabled)
     * Returns null if invalid index.
     */
    public String getAttributeNamespaceUri(int index);

    /**
     * Returns the localname of the specified attribute
     * if namespaces enabled or just attribute name if namespaces disabled.
     * Returns null if invalid index.
     */
    public String getAttributeLocalName(int index);

    /**
     * Returns the prefix of the specified attribute
     * Returns null if invalid index or if element has no prefix.
     */
    public String getAttributePrefix(int index);

    /**
     * Returns the raw name of the specified attribute
     * Returns null if invalid index.
     */
    public String getAttributeRawName(int index);

    /**
     * Returns the given attributes value
     * Returns null if invalid index.
     */
    public String getAttributeValue(int index);

    /**
     * Returns the given attributes value
     * Returns null if no attribute with rawName.
     */
    public String getAttributeValueFromRawName(String rawName);

    /**
     * Returns the given attributes value
     */
    public String getAttributeValueFromName(String namespaceUri,
                                            String localName);

    /**
     * Return true if attribute at index is namespace declaration
     * such as xmlns='...' or xmlns:prefix='...'
     */
    public boolean isAttributeNamespaceDeclaration(int index);

    // -- modfiable


    /** parameters modeled after SAX2 attribute approach */
    public void addAttribute(String namespaceUri,
                             String localName,
                             String rawName,
                             String value)
        throws XmlPullParserException;

    /**
     *
     * Parameter isNamespaceDeclaration if true indicates that attribute is related
     *        to namespace management and may be ignored by normal processing
     * <p><b>NOTE:</b> this class has no support for resolving namespaces and
     *       such support may be added later (see XmlNode and namespaces methids)
     */
    public void addAttribute(String namespaceUri,
                             String localName,
                             String rawName,
                             String value,
                             boolean isNamespaceDeclaration)
        throws XmlPullParserException;

    /**
     * Pre-allocate if necessary tag data structure to hold
     * at least minCapacity attributes .
     */
    public void ensureAttributesCapacity(int minCapacity)
        throws XmlPullParserException;

    /** remove all atribute */
    public void removeAttributes()
        throws XmlPullParserException;


    /**
     * This method tries to remove attribute identified by namespace uti and local name.
     * @return true if attribute was removed or false otherwise.
     */
    public boolean removeAttributeByName(String uri, String localName)
                throws XmlPullParserException;

    /**
     * This method tries to remove attribute identified by raw name.
     * @return true if attribute was removed or false otherwise.
     */
    public boolean removeAttributeByRawName(String rawName)
                throws XmlPullParserException;

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

