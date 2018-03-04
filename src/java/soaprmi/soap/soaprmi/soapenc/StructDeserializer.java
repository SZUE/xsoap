/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: StructDeserializer.java,v 1.8 2003/04/06 00:04:17 aslom Exp $
 */

package soaprmi.soapenc;

import java.io.IOException;

import org.gjt.xpp.XmlStartTag;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;

import soaprmi.struct.StructAccessor;
import soaprmi.struct.StructException;
import soaprmi.mapping.XmlJavaStructMap;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.mapping.XmlMapException;
import soaprmi.util.Check;

import soaprmi.soap.*;

/**
 * Implementation of standard SOAP encoding style struct deserializer.
 *
 * @version $Revision: 1.8 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class StructDeserializer implements Deserializer {


    public Object readObject(
        DeserializeContext dctx,
        EncodingStyle enc,
        Class baseType,
        XmlJavaTypeMap map,
        XmlPullParser pp,
        XmlStartTag stag
    )
        throws DeserializeException, XmlPullParserException, IOException
    {
        try {
            if(map.getSimpleType()) {
                throw new DeserializeException(
                    "specialized serializer is required for "+map.getJavaType()+
                        " - struct deserializer can not deserialize simple types"+pp.getPosDesc());
            }
            if(map instanceof XmlJavaStructMap == false) {
                throw new DeserializeException(
                    "to deserialize must be struct - java type "+map.javaClass());
            }
            String href = stag.getAttributeValueFromRawName("href");
            if(href != null)
                throw new DeserializeException("href not allowed in root elements");
            //String id = stag.getAttributeValueFromRawName("id");
            StructAccessor sa = ((XmlJavaStructMap)map).structAccessor();
            Object o = sa.newInstance();
            //if(id != null) {
            //  dctx.setIdValue(id, o);
            //}
            while(true) {
                int state = pp.next();
                if(state == XmlPullParser.CONTENT) {
                    if(!pp.isWhitespaceContent())
                        throw new DeserializeException(
                            "empty struct can not have text content"+pp.getPosDesc());
                    state = pp.next();
                }
                if(state == XmlPullParser.END_TAG)
                    break;
                pp.readStartTag(stag);
                //System.err.println("struct deserializer reading stag="+stag);
                if(!"".equals(stag.getNamespaceUri()))
                    throw new DeserializeException(
                        "namespace typed accessors not allowed inside struct"
                            + pp.getPosDesc());
                String name = stag.getLocalName();
                //TODO: encUri SOAP_ENV encodingStyle
                Class saClass = sa.getAccessorType(name);
                if(saClass.isPrimitive() || saClass == String.class) {
                    if(sa.primitivesWrapped()) {
                        Object value = enc.readObject(dctx, saClass, pp, stag);
                        sa.setValue(o, name, value);
                    } else {
                        throw new IllegalStateException("unwrapped primitives unsupported yet");
                    }
                } else {
                    // TODO deal with href
                    href = stag.getAttributeValueFromRawName("href");
                    if(href != null) {
                        String hid = href.substring(1); // skip # int '#id1'
                        if(dctx.hasId(hid)) {
                            Object val = dctx.getIdValue(hid);
                            sa.setValue(o, name, val);
                        } else {
                            dctx.addStructFixup(sa, o, name, saClass, hid);
                        }
                        if(pp.next() != XmlPullParser.END_TAG) //skip END_TAG
                            throw new DeserializeException(
                                "expected immediate end tag for href element"+pp.getPosDesc());
                    } else {
                        Object val =  enc.readObject(dctx, saClass, pp, stag);
                        sa.setValue(o, name, val);
                    }
                }
            }
            return o;
        } catch(XmlMapException ex) {
            throw new DeserializeException(
                "problem with xml-java mapping"+pp.getPosDesc(), ex);
        } catch(StructException ex) {
            throw new DeserializeException(
                "problem with struct accessor"+pp.getPosDesc(), ex);
        }
    }
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

