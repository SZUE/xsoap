/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: DeserializeContext.java,v 1.7 2003/04/06 00:04:15 aslom Exp $
 */

package soaprmi.soap;

import java.io.IOException;
import java.io.Reader;
import org.gjt.xpp.XmlEndTag;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlStartTag;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.mapping.XmlMapException;
import soaprmi.struct.StructAccessor;

/**
 * Define set of operations that must be supported for SOAP deserialization.
 *
 * @version $Revision: 1.7 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public interface DeserializeContext {

    // --- setup

    public EncodingStyle getDefaultEncodingStyle();
    public void setDefaultEncodingStyle(EncodingStyle estyle);

    public XmlJavaMapping getMapping();
    public void setMapping(XmlJavaMapping mapping);

    public void setReader(Reader reader) throws SoapException;
    public void setPullParser(XmlPullParser parser) throws SoapException;
    public XmlPullParser getPullParser();
    public XmlStartTag getStartTag();
    public XmlEndTag getEndTag();

    // --- real work

    //public void init() throws SoapException;

    public Object readObject(Class baseClass) throws DeserializeException, IOException;

    public double readDouble() throws DeserializeException, IOException;

    public float readFloat() throws DeserializeException, IOException;

    public int readInt() throws DeserializeException, IOException;

    public String readString() throws DeserializeException, IOException;


    public byte done() throws DeserializeException, IOException;

    public boolean isClosed();

    public void close() throws DeserializeException, IOException;

    // --- utility methods


    public XmlJavaTypeMap queryTypeMap(EncodingStyle enc, Class javaClass)
        throws XmlMapException;

    public XmlJavaTypeMap queryTypeMap(EncodingStyle enc, String uri, String localName)
        throws XmlMapException;

    public Deserializer queryDeserializer(EncodingStyle enc, XmlJavaTypeMap map)
        throws DeserializeException;

    public void enterStruct();
    public void leaveStruct();
    public int structLevel();

    // --- multi-ref deserialization

    public void setIdValue(String id, Object value) throws DeserializeException;
    public boolean hasId(String id);
    public Object getIdValue(String id) throws DeserializeException;
    public void addStructFixup(StructAccessor sa, Object target,
                               String accessorName, Class accessorType, String id);
    public void addArrayFixup(Object /*Array*/ target,
                              int pos, Class komponentType, String id);
    // --- converters

    //  public void registerConverterFrom(Class expectedType, Converter cnv);
    //
    //  public void registerConverterTo(Class expectedType, Converter cnv);

    public Object convert(EncodingStyle enc, Object source, Class expectedType)
        throws DeserializeException;


    //void setInPool(boolean inPool);
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

