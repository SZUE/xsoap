/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SerializeContext.java,v 1.5 2003/04/06 00:04:15 aslom Exp $
 */

package soaprmi.soap;

import java.io.IOException;
import java.io.Writer;

import soaprmi.mapping.XmlMapException;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlJavaTypeMap;


/**
 * Define set of operations that must be supported for SOAP serialization.
 *
 * @version $Revision: 1.5 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public interface SerializeContext {

    // -- setup

    EncodingStyle getDefaultEncodingStyle();
    void setDefaultEncodingStyle(EncodingStyle enc);

    XmlJavaMapping getMapping();
    void setMapping(XmlJavaMapping mapping);

    Writer getWriter();
    void setWriter(Writer writer) throws SerializeException;

    SoapStyle getSoapStyle();
    void setSoapStyle(SoapStyle style);


    // --- actions

    public void writeObject(
        Object o,
        String name,
        Class baseType) throws SerializeException, IOException;

    public void writeObject(
        Object o,
        String name) throws SerializeException, IOException;

    public void writeObject(Object o) throws SerializeException, IOException;

    public void writeFloat(
        float f,
        String name) throws SerializeException, IOException;

    public void writeFloat(float f) throws SerializeException, IOException;

    public void writeInt(
        int i,
        String name) throws SerializeException, IOException;

    public void writeInt(int i) throws SerializeException, IOException;


    public void writeString(
        String s,
        String name) throws SerializeException, IOException;

    public void writeString(String s) throws SerializeException, IOException;

    void done() throws SerializeException, IOException;

    void close() throws SerializeException, IOException;


    // --- utility methods for serializers

    public XmlJavaTypeMap queryTypeMap(Class javaClass)
        throws SerializeException, XmlMapException;
    public Serializer querySerializer(EncodingStyle enc, Class klass)
        throws SerializeException, IOException;

    // --- support pre-defined namespace declarations

    public void writeXsiNull(String name, String uri, String localName)
        throws SerializeException, IOException;
    public void writeXsiNull(String name)
        throws SerializeException, IOException;
    void writeXsiNull() throws SerializeException, IOException;
    void writeXsiType(String uri, String localName)
        throws SerializeException, IOException;
    void writeXsdType(String localName)
        throws SerializeException, IOException;
    void writeNamespaces() throws SerializeException, IOException;
    //void writeXmlnsXsd() throws SerializeException, IOException;
    //void writeXmlnsXsi() throws SerializeException, IOException;
    void writeXmlns(String prefix, String uri)
        throws SerializeException, IOException;
    void writeXmlnsLevel(String uri) throws SerializeException, IOException;

    // --- support SOAP type of serialization
    public void writeRef(
        String name,
        String href)  throws SerializeException, IOException;
    public String writeStartStartTag(String name, String uri, String localName,
                                     boolean forceXsiType, String id) throws SerializeException, IOException;
    public String writeStartTag(String name, String uri, String localName,
                                boolean forceXsiType, String id) throws SerializeException, IOException;
    public void writeEndTag(String name) throws SerializeException, IOException;

    // -- support for logical nesting of serialized objects

    void enterStruct();
    void leaveStruct();
    int structLevel();

    // -- support for multi-ref serialization

    public String addRef(
        Serializer typeSer,
        EncodingStyle typeEnc,
        Object typeValue,
        String name,
        Class baseClass);

    /**
     * Add object to id-table and return assigned id.
     * This function can be call multiple times and always returns the same id.
     * @param obj must be not null
     * @return id assigned to object
     */
    public String addId(Object obj);

    /**
     * Return id for object or null if object has no id.
     */
    public String getId(Object value);

    /**
     * Assign to object new id.
     * @param obj must be not null
     * @param id assigned to object
     * @return previous id or null if object had no id previously
     */
    public String setObjectId(Object obj, String id);

}

/*
 * Indiana University Extreme! Lab Software License, Version 1.2
 *
 * Copyright (C) 2002 The Trustees of Indiana University.
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

