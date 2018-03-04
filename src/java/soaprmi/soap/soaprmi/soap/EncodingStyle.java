/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: EncodingStyle.java,v 1.6 2003/04/06 00:04:15 aslom Exp $
 */

package soaprmi.soap;

import java.io.IOException;

import org.gjt.xpp.XmlStartTag;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;

/**
 * Interface to use SOAP encoding style.
 *
 * @version $Revision: 1.6 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public interface EncodingStyle {

    public Deserializer queryDeserializer(Class javaType)
        throws SoapException;

    public Serializer querySerializer(Class javaType)
        throws SoapException;

    public void registerClassEncodingHandler(
	Class javaType,
	Serializer ser,
	Deserializer deser) throws SoapException;

    public Deserializer queryInterfaceDeserializer(Class javaInterface)
        throws SoapException;

    public Serializer queryInterfaceSerializer(Class javaInterface)
	throws SoapException;

    public void registerInterfaceEncodingHandler(
	Class javaType,
	Serializer ser,
	Deserializer deser) throws SoapException;

    public Converter  queryConverterFrom(Class expectedType);

    public Converter  queryConverterTo(Class expectedType);

    public void registerConverterFrom(Class expectedType, Converter cnv);

    public void registerConverterTo(Class expectedType, Converter cnv);

    String getEncodingStyleUri();

    Serializer defaultArraySerializer();

    Serializer defaultStructSerializer();

    Deserializer defaultArrayDeserializer();

    Deserializer defaultStructDeserializer();

    // --- reading

    public Object readObject(DeserializeContext dctx,
			     Class baseClass,
			     XmlPullParser pp, XmlStartTag stag
			    )
	throws DeserializeException, XmlPullParserException, IOException;

    public boolean readBoolean(DeserializeContext dctx,
			       XmlPullParser pp, XmlStartTag stag)
	throws DeserializeException, XmlPullParserException, IOException;

    public long readLong(DeserializeContext dctx,
			 XmlPullParser pp, XmlStartTag stag)
	throws DeserializeException, XmlPullParserException, IOException;

    public short readShort(DeserializeContext dctx,
			   XmlPullParser pp, XmlStartTag stag)
	throws DeserializeException, XmlPullParserException, IOException;

    public int readInt(DeserializeContext dctx,
		       XmlPullParser pp, XmlStartTag stag)
	throws DeserializeException, XmlPullParserException, IOException;

    public float readFloat(DeserializeContext dctx,
			   XmlPullParser pp, XmlStartTag stag)
	throws DeserializeException, XmlPullParserException, IOException;

    public double readDouble(DeserializeContext dctx,
			     XmlPullParser pp, XmlStartTag stag)
	throws DeserializeException, XmlPullParserException, IOException;


    public String readString(DeserializeContext dctx,
			     XmlPullParser pp, XmlStartTag stag)
	throws DeserializeException, XmlPullParserException, IOException;


    // -- writing

    //public void writeRef(
    //  Serializer typeSer,
    //  SerializeContext sctx,
    //  Object value,
    //  String name,
    //  String href)  throws SerializeException, IOException;

    public void writeObject(
	SerializeContext sctx,
	Object o,
	String name,
	Class baseType) throws SerializeException, IOException;

    public void writeDouble(
	SerializeContext sctx,
	double d,
	String name,
	boolean forceXsiType) throws SerializeException, IOException;

    public void writeFloat(
	SerializeContext sctx,
	float f,
	String name,
	boolean forceXsiType) throws SerializeException, IOException;


    public void writeInt(
	SerializeContext sctx,
	int i,
	String name,
	boolean forceXsiType) throws SerializeException, IOException;


    public void writeString(
	SerializeContext sctx,
	String s,
	String name,
	boolean forceXsiType) throws SerializeException, IOException;


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

