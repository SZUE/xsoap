/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Tag.java,v 1.6 2004/05/06 18:18:45 aslom Exp $
 */

package org.gjt.xpp.impl.tag;

import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlTag;

/**
 * Encapsulate XML ETag
 *
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class Tag implements XmlTag {
    private String uri = "";
    private String localName;
    private String qName;

    // only package level access -- all others use factory
    protected Tag() {
    }

    public void resetTag() {
        uri = "";
        localName = qName = null;
    }

    /** Get endtag tag uri (meaningful only if namespaces enabled) */
    public String getNamespaceUri() { return uri; }

    //public void setNamespaceURI(String value) { uri = value; }

    /**
     * Get start tag localName if namespaces enabled
     * or just qName (see below) if namespaces diabled.
     */
    public String getLocalName() { return localName; }

    //public void setLocalName(String value) { localName = value; }

    public String getPrefix() {
        if(qName != null) {
            int pos = qName.indexOf(':');
            if(pos != -1) {
                return qName.substring(0, pos);
            }
        }
        return null;
    }

    /**
     * Return end tag name as it is in document (qName).
     */
    public String getRawName() { return qName; }

    //public void setRawName(String value) { qName = value; };


    /** this constructor is modeled after SAX2 startTag */
    public void modifyTag(String namespaceURI,
                          String localName,
                          String rawName)
        throws XmlPullParserException {
        if(rawName == null) {
            throw new XmlPullParserException(
                "tag raw name must be not null");
        }
        this.uri = namespaceURI != null ? namespaceURI : "";
        this.localName = localName != null ? localName : rawName ;
        this.qName = rawName;
    }


    /**
     * Print into StringBuffer element name
     */
    protected void printFields(StringBuffer buf) {
        //if(uri != null)
        //  buf.append(" uri='" + uri + "'");
        //if(localName != null)
        //  buf.append(" localName='" + localName + "'");
        buf.append(" '" + qName + "'");
        if(uri != null && !"".equals(uri))
            buf.append("('" + uri +"','" + localName + "') ");
    }

    public int hashCode() {
        int c = (localName != null) ? c = localName.hashCode() : 0;
        if(uri != null) c = c ^ uri.hashCode();
        return c;
    }

    public boolean equals(Object o) {
        if(o == null) return false;
        if(! (o instanceof Tag) ) return false;
        Tag t = (Tag) o;
        return (uri == t.uri || (uri != null && uri.equals(t.uri)))
            && (localName == t.localName || (localName != null && localName.equals(t.localName)));
    }
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


