/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: ArraySerializer.java,v 1.12 2003/04/06 00:04:17 aslom Exp $
 */

package soaprmi.soapenc;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.mapping.XmlMapException;
import soaprmi.soap.EncodingStyle;
import soaprmi.soap.SerializeContext;
import soaprmi.soap.SerializeException;
import soaprmi.soap.Serializer;
import soaprmi.soap.Soap;
import soaprmi.soap.SoapStyle;
import soaprmi.util.Check;
import soaprmi.util.Util;

/**
 * Implementation of standard SOAP encoding array serializer.
 *
 * @version $Revision: 1.12 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class ArraySerializer implements Serializer {

    public void writeObject(
        SerializeContext sctx,
        EncodingStyle enc,
        Object o,
        String name,
        Class baseClass,
        String id)
        throws SerializeException, XmlMapException, IOException
    {
        //try {
        SoapStyle style = sctx.getSoapStyle();
        if(Check.ON) Check.assertion(o != null);
        Class klass = o.getClass();
        if(Check.ON) Check.assertion(klass.isPrimitive() == false);
        if(Check.ON) Check.assertion(klass.isArray());
        if(Check.ON) Check.assertion(klass != String.class);
        //if(Check.ON) Check.assert(map.getSimpleType() == false);
        //write encodingStyle if necessary
        int arrLen = Array.getLength(o);

        //XmlJavaTypeMap map = sctx.queryTypeMap(klass);
        // it should return SOAP-ENC:Array (or derived type?)
        String stagName =  sctx.writeStartStartTag(
            name,
            style.SOAP_ENC_NS,//map.getUri(),
            "Array",
            true, //!klass.equals(baseClass),
            id != null ? id : sctx.addId(o)
        );

        Class kompType = klass.getComponentType();
        XmlJavaTypeMap kompMap = null;
        if(kompType.isInterface()) {
            //uri = defaultStructNsPrefix;
            //localName = kompType.getClass();
            try {
                kompMap = sctx.queryTypeMap(kompType);
            } catch(XmlMapException ex) {
                XmlJavaMapping mapping = sctx.getMapping();
                if(mapping == null) {
                    mapping = Soap.getDefault().getMapping();
                }
                kompMap = mapping.autoMapArrayComponentInterface(kompType);
            }
        } else {
            kompMap = sctx.queryTypeMap(kompType);
        }
        String uri = kompMap.getUri();
        String localName = kompMap.getLocalName();
        if(uri == null) {
            throw new IllegalArgumentException(
                "array component type uri can not be null");
        }
        if(localName == null) {
            throw new IllegalArgumentException(
                "array component type localName can not be null");
        }
        //sctx.writeXmlnsLevel(uri);

        Writer writer = sctx.getWriter();
        writer.write(" xmlns:n");
        if(sctx.structLevel() > 0)
            writer.write(Integer.toString(sctx.structLevel()));
        writer.write("='");
        writer.write(uri);
        writer.write('\'');

        writer.write(' ');

        //expected output for arrayType attribute "SOAP-ENC:arrayType='xsd:ur-type[4]'"
        writer.write(style.SOAP_ENC_NS_PREFIX);
        writer.write(":arrayType='n");
        if(sctx.structLevel() > 0) {
            writer.write(Integer.toString(sctx.structLevel()));
        }
        writer.write(':');
        writer.write(localName);
        writer.write('[');
        writer.write(Integer.toString(arrLen));
        writer.write("]'>\n");

        sctx.enterStruct();

        if(arrLen > 0) {
            //fast writing of arrays of primitive types + String + wrappers ....
            if(kompType == String.class) {
                String[] as = (String[]) o;
                for(int i = 0; i < arrLen; ++i) {
                    //writer.write(as[i]);
                    String s = as[i];
                    if(s != null) {
                        writer.write("<s>");
                        Util.writeXMLEscapedString(writer, s);
                        writer.write("</s>\n");
                    } else {
                        writer.write("<s ");
                        sctx.writeXsiNull();
                        writer.write("/>\n");
                    }
                }
                //writer.write(as[arrLen - 1]);
                //Util.writeXMLEscapedString(writer, as[arrLen - 1]);

            } else if(kompType.isPrimitive()) {
                if(kompType == Double.TYPE) {
                    double[] ad = (double[]) o;
                    writer.write("<d>");
                    for(int i = 0; i < arrLen - 1; ++i) {
                        writer.write(Double.toString(ad[i]));
                        writer.write("</d>\n<d>");
                    }
                    writer.write(Double.toString(ad[arrLen - 1]));
                    writer.write("</d>\n");
                } else if(kompType == Float.TYPE) {
                    float[] a = (float[]) o;
                    writer.write("<f>");
                    for(int i = 0; i < arrLen - 1; ++i) {
                        writer.write(Float.toString(a[i]));
                        writer.write("</f>\n<f>");
                    }
                    writer.write(Float.toString(a[arrLen - 1]));
                    writer.write("</f>\n");
                } else if(kompType == Integer.TYPE) {
                    int[] a = (int[]) o;
                    writer.write("<i>");
                    for(int i = 0; i < arrLen - 1; ++i) {
                        writer.write(Integer.toString(a[i]));
                        writer.write("</i>\n<i>");
                    }
                    writer.write(Integer.toString(a[arrLen - 1]));
                    writer.write("</i>\n");
                } else if(kompType == Long.TYPE) {
                    long[] a = (long[]) o;
                    writer.write("<l>");
                    for(int i = 0; i < arrLen - 1; ++i) {
                        writer.write(Long.toString(a[i]));
                        writer.write("</l>\n<l>");
                    }
                    writer.write(Long.toString(a[arrLen - 1]));
                    writer.write("</l>\n");
                } else if(kompType == Character.TYPE) {
                    char[] a = (char[]) o;
                    writer.write("<u>");
                    for(int i = 0; i < arrLen - 1; ++i) {
                        writer.write(Integer.toString((int)a[i]));
                        writer.write("</u>\n<u>");
                    }
                    writer.write(Integer.toString((int)a[arrLen - 1]));
                    writer.write("</u>\n");
                } else {
                    throw new SerializeException(
                        "unsupported array of primitive type "+kompType);
                }
            } else {
                for(int i = 0; i < arrLen; ++i) {
                    Object value = Array.get(o, i);
                    // make sure that null is not skipped regardless of style
                    if(value != null) {
                        enc.writeObject(sctx, value, "o", kompType);
                    } else {
                        sctx.writeXsiNull("n");
                    }
                }
            }
        }

        sctx.leaveStruct();

        sctx.writeEndTag(stagName);
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

