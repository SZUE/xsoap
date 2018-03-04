/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: StartTag.java,v 1.14 2004/05/06 18:18:45 aslom Exp $
 */

package org.gjt.xpp.impl.tag;

import java.util.Hashtable;

import org.gjt.xpp.XmlStartTag;

import org.gjt.xpp.impl.tag.Attribute;

/**
 * Encapsulate XML STag and EmptyElement
 *
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class StartTag extends Tag implements XmlStartTag {
    // experimental flag to allow check to approaches to handling attribs
    //private final static boolean MAPPING = false;
    private final static boolean TRACE_SIZING = false;

    private int attEnd;
    private int attSize;
    private Attribute[] attArr;


    //private Hashtable mapQName;
    //private Hashtable mapUri;

    public StartTag()
    {
        //    if(MAPPING) {
        //      mapQName = new Hashtable();
        //      mapUri = new Hashtable();
        //    }
    }

    /**
     * Reinitialize start tag content to none
     */
    public void resetStartTag()
    {
        super.resetTag();
        //Reset intenrla valriables but do not clear previous values
        // (it is more efficient)
        attEnd = 0;
        //    if(MAPPING) {
        //      mapQName.clear();
        //      mapUri.clear();
        //    }
    }

    /**
     * Return number of attributes.
     */
    public int getAttributeCount() { return attEnd; }

    /**
     * Get uri of attribute number index (starts from 0).
     * (meaningful only if namespaces enabled)
     */
    public String getAttributeNamespaceUri(int index) {
        if (index >= 0 && index < attEnd) {
            return attArr[index].uri;
        } else {
            return null;
        }
    }

    /**
     * Get localName of attribute number index (starts from 0)
     * if namespaces enabled or just attribute name if namespaces disabled.
     */
    public String getAttributeLocalName(int index) {
        if (index >= 0 && index < attEnd) {
            return attArr[index].localName;
        } else {
            return null;
        }
    }

    public String getAttributePrefix(int index)
    {
        if (index >= 0 && index < attEnd) {
            String s = attArr[index].qName;
            if(s != null) {
                int pos = s.indexOf(':');
                if(pos != -1) {
                    return s.substring(0, pos);
                }
            }
        }
        return null;
    }

    /** Return qName of atrribute number index (starts from 0) */
    public String getAttributeRawName(int index) {
        if (index >= 0 && index < attEnd) {
            return attArr[index].qName;
        } else {
            return null;
        }
    }

    //  /** always return "CDATA" */
    //  public String getAttributeType(int index) {
    //    return "CDATA";
    //  }


    /** Return value of attribute number index. */
    public String getAttributeValue(int index) {
        if (index >= 0 && index < attEnd) {
            return attArr[index].value;
        } else {
            return null;
        }
    }

    public boolean isAttributeNamespaceDeclaration(int index)
    {
        return attArr[index].xmlnsAttrib;
    }

    //  /** always return "CDATA" */
    //  public String getAttributeType(String qName) {
    //    return "CDATA";
    //  }
    //
    //  /** always return "CDATA" */
    //  public String getAttributeType(String uri, String localName) {
    //    return "CDATA";
    //  }

    /**
     * Return value of attribute named (uri, localName) or null
     * of no such attribute found.
     * (meaningful only if namespaces enabled)
     */
    public String getAttributeValueFromName(String uri, String localName) {
        //    if(MAPPING) {
        //      Hashtable mapLocal = (Hashtable) mapUri.get(uri);
        //      return (String) mapLocal.get(localName);
        //    } else {
        for(int i = 0; i < attEnd; ++i) {
            if((uri != null && uri.equals(attArr[i].uri) || (uri == null && attArr[i].uri == null))
               && localName.equals(attArr[i].localName))
            {
                return attArr[i].value;
            }
        }
        return null;
        //    }
    }

    /**
     * Return value of attribute named qName or null
     * of no such attribute found.
     */
    public String getAttributeValueFromRawName(String qName) {
        //    if(MAPPING) {
        //      return (String) mapQName.get(qName);
        //    } else {
        for(int i = 0; i < attEnd; ++i) {
            if(qName.equals(attArr[i].qName))
            {
                return attArr[i].value;
            }
        }
        return null;
        //    }
    }


    /** parameters modeled after SAX2 attribute approach */
    public void addAttribute(String namespaceUri,
                             String localName,
                             String rawName,
                             String value)
    {
        addAttribute(namespaceUri, localName, rawName, value, false);
    }

    public void addAttribute(String namespaceUri,
                             String localName,
                             String rawName,
                             String value,
                             boolean isNamespaceDeclaration)
    {
        if(attEnd >= attSize) ensureAttributesCapacity(2 * attEnd + 1);
        // assert namespaceUri != null && localName != null && rawName != null
        Attribute att = attArr[attEnd++];
        att.uri = namespaceUri;
        att.localName = localName;
        att.qName = rawName;
        att.value = value;
        //    if(MAPPING) {
        //      mapQName.put(att.qName, att.value);
        //      Hashtable mapLocal = (Hashtable) mapUri.get(att.uri);
        //      if(mapLocal == null) {
        //        mapLocal = new Hashtable();
        //        mapUri.put(att.uri, mapLocal);
        //      }
        //      mapLocal.put(att.localName, att.value);
        //    }
    }

    /**
     * Make sure that there is enough space to keep size attributes.
     */
    public void ensureAttributesCapacity(int minCapacity)
    {
        int newSize = minCapacity;
        //    if(newSize < 8)
        //      newSize = 8; // = lucky 7 + 1 //25
        if(attSize < newSize) {
            if(TRACE_SIZING) {
                System.err.println("stag attributes "+attEnd+" ==> "+newSize);
            }
            Attribute[] newAttArr = new Attribute[newSize];
            if(attArr != null)
                System.arraycopy(attArr, 0, newAttArr, 0, attEnd);
            for(int i = attEnd; i < newSize; ++i) {
                newAttArr[i] = new Attribute();
            }
            attArr = newAttArr;
            attSize = newSize;
        }
    }


    public boolean removeAttributeByRawName(String rawName) {
        for(int i = 0; i < attEnd; ++i) {
            if(rawName.equals(attArr[i].qName))
            {
                removeAttributeAtPos(i);
                return true;
            }
        }
        return false;
    }


    public boolean removeAttributeByName(String uri, String localName) {
        for(int i = 0; i < attEnd; ++i) {
            if((uri != null && uri.equals(attArr[i].uri) || (uri == null && attArr[i].uri == null))
               && localName.equals(attArr[i].localName))
            {
                //              int count =  attEnd - i - 1;
                //              if(count > 0) {
                //                  Attribute attr = attArr[i];
                //                  // delete by moving over attributes
                //                  System.arraycopy(attArr, i + 1, attArr, i, count);
                //                  attArr[attEnd - 1] = attr;
                //              }
                //              --attEnd;
                removeAttributeAtPos(i);
                return true;
            }
        }
        return false;
    }

    private void removeAttributeAtPos(int i) {
        int count =  attEnd - i - 1;
        if(count > 0) {
            Attribute attr = attArr[i];
            // delete by moving over attributes
            System.arraycopy(attArr, i + 1, attArr, i, count);
            // and copying removed attrinute to the end for later reuse
            attArr[attEnd - 1] = attr;
        }
        --attEnd;
    }

    /** remove all atribute */
    public void removeAttributes() {
        attEnd = 0;
    }

    protected void printFields(StringBuffer buf) {
        super.printFields(buf);
        if(attEnd > 0) {
            buf.append(" attArr=[ ");
            for(int i = 0; i < attEnd; ++i) {
                buf.append(attArr[i]+" ");
            }
            buf.append(" ]");
        }
    }

    /**
     * Return string representation of start tag including name
     * and list of attributes.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("StartTag={");
        printFields(buf);
        buf.append(" }");
        return buf.toString();
    }

    public boolean equals(Object o) {
        if(o == null) return false;
        if(! (o instanceof StartTag) ) return false;
        StartTag t = (StartTag) o;
        if(attEnd != t.attEnd) return false;
        for (int i = 0; i < attEnd; i++) {
            if(! attArr[i].equals(t.attArr[i])) {
               return false;
            }
        }
        return super.equals(o);
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



