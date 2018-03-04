/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: ArrayDeserializer.java,v 1.12 2003/04/06 00:04:17 aslom Exp $
 */

package soaprmi.soapenc;

import java.io.IOException;
import java.lang.reflect.Array;
import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlStartTag;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.mapping.XmlMapException;
import soaprmi.soap.DeserializeContext;
import soaprmi.soap.DeserializeException;
import soaprmi.soap.Deserializer;
import soaprmi.soap.EncodingStyle;
import soaprmi.soap.Soap;

/**
 * Implementation of standard SOAP encoding style struct deserializer.
 *
 * @version $Revision: 1.12 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class ArrayDeserializer implements Deserializer {


    public Object readObject(
        DeserializeContext dctx,
        EncodingStyle enc,
        Class expectedClass,
        XmlJavaTypeMap map,
        XmlPullParser pp,
        XmlStartTag stag
    )
        throws DeserializeException, XmlPullParserException, IOException
    {
        /*
         possible cases:
         * in XML:
         - we have correctly formated array with size  [known kompType]
         - we have correctly formated array but without size --> ERROR
         - there is no array mentioned but just list of elements....
         - expected type is array do it synamically  - maybe even primitives...
         - expected type is List - just List(<size if known>) and then adding
         * map.Class subtype of expectedClass (and it must be arrys or list)
         ---> if not create expectedClass if it is subtype of List
         (map.Class MUST BE array or equals excpected Type!!!!)
         - deserilize array of know size
         - if size unknow: deserializeList and convert inot array?
         - private List deserializeList(...)
         ---> Vector, List, something implementing List (ArrayList, ...)


         TODO: * Map (HashMap) and Hashtable (de)serialzier
         */
        //*
        try {
            if(map != null && map.getSimpleType())
                throw new DeserializeException(
                    "struct deserializer can not deserialize simple types");
            String href = stag.getAttributeValueFromRawName("href");
            if(href != null)
                throw new DeserializeException("href not allowed in root elements");

            String arrayType =
                stag.getAttributeValueFromName(Soap.SOAP_ENC_NS, "arrayType");
            if(arrayType == null) {
                throw new DeserializeException("cant get arrayType attribute"
                                                   +stag+pp.getPosDesc());
                //kompType = Object.class;
            }

            int idxBracket = arrayType.indexOf('[');
            if(idxBracket == -1) {
                throw new DeserializeException("array must have size specified"
                                                   +" arrayType='"+arrayType+"' in "+stag+pp.getPosDesc());
            }
            String arrayQname = arrayType.substring(0, idxBracket);
            int idxBracket2 = arrayType.indexOf(']', idxBracket + 1);
            if(idxBracket2 == -1) {
                throw new DeserializeException("array must have size specified"
                                                   +" arrayType='"+arrayType+"' in "+stag+pp.getPosDesc());
            }
            String arraySize = arrayType.substring(idxBracket+1, idxBracket2);
            int arrLen = -1;
            try {
                if(arraySize.length() > 0) {
                    arrLen = Integer.parseInt(arraySize);
                } else {
                    // this is a special case not covered by SOAP 1.1 spec that
                    // allows empty array to be represented as [] instead of [0]
                    // from http://www.w3.org/TR/SOAP/#_Toc478383513 arrayTypeValue/asize defintions
                    // (MOTIVATION: improve interoperability with GLUE)
                    arrLen = 0;
                }
            } catch(NumberFormatException nfe) {
                throw new DeserializeException("array size must be integer"
                                                   +" not '"+arraySize+"' from "+arrayType+" in "
                                                   +stag+pp.getPosDesc());
            }

            String uri = pp.getQNameUri(arrayQname);
            if(uri == null) {
                throw new DeserializeException(
                    "cant determine array element type from '"+arrayQname+"' "
                        +stag+pp.getPosDesc());
            }
            String localName = pp.getQNameLocal(arrayQname);

            XmlJavaTypeMap kompMap = dctx.queryTypeMap(enc, uri, localName);
            Class kompType = kompMap.javaClass();

            //this is to support deserializing of XmlNode[] that is alternative to Object[]
            if(expectedClass != null && kompType == Object.class) {
                Class expectedCompType = expectedClass.getComponentType();
                if(XmlNode.class.equals(expectedCompType)) {
                   // switch array from Object[] to XmlNode[] when it is expected by user
                    kompType = expectedCompType;
                }
            }


            Object o = null;

            // super fast loading of primitve types  + Strings

            if(kompType == String.class) {
                String[] arr = new String[arrLen];

                for(int i = 0; i < arrLen; ++i) {
                    byte state = pp.next();
                    pp.readStartTag(stag);

                    String xs = stag.getAttributeValueFromName(Soap.XSI_NS_CURR,
                                                               Soap.XSI_NS_CURR_NULL);
                    //System.err.println("xs="+xs);
                    if( "1".equals(xs) ) {
                        state = pp.next();
                        arr[i] = null;
                    } else {
                        state = pp.next();
                        if(state == XmlPullParser.CONTENT) {
                            arr[i] = pp.readContent();
                            state = pp.next();
                        } else {
                            arr[i] = "";
                        }
                    }
                    if(state != XmlPullParser.END_TAG) {
                        throw new DeserializeException("expected end tag"+pp.getPosDesc());
                    }
                }
                o = arr;
            } else if(kompType.isPrimitive()) {
                if(kompType == Double.TYPE) {
                    //throw new IllegalStateException(
                    //  "deserializing not implemented for double array");
                    double[] arr = new double[arrLen];
                    for(int i = 0; i < arrLen; ++i) {
                        pp.next(); //pp.readStartTag(stag);
                        pp.next();
                        arr[i] = Double.parseDouble(pp.readContent());
                        pp.next();
                    }
                    o = arr;
                } else if(kompType == Float.TYPE) {
                    //throw new IllegalStateException(
                    //  "deserializing not implemented for double array");
                    float[] arr = new float[arrLen];
                    for(int i = 0; i < arrLen; ++i) {
                        pp.next(); //pp.readStartTag(stag);
                        pp.next();
                        arr[i] = Float.parseFloat(pp.readContent());
                        pp.next();
                    }
                    o = arr;
                } else if(kompType == Integer.TYPE) {
                    //throw new IllegalStateException(
                    //  "deserializing not implemented for double array");
                    int[] arr = new int[arrLen];
                    for(int i = 0; i < arrLen; ++i) {
                        pp.next(); //pp.readStartTag(stag);
                        pp.next();
                        arr[i] = Integer.parseInt(pp.readContent());
                        pp.next();
                    }
                    o = arr;
                } else if(kompType == Long.TYPE) {
                    //throw new IllegalStateException(
                    //  "deserializing not implemented for double array");
                    long[] arr = new long[arrLen];
                    for(int i = 0; i < arrLen; ++i) {
                        pp.next(); //pp.readStartTag(stag);
                        pp.next();
                        arr[i] = Long.parseLong(pp.readContent());
                        pp.next();
                    }
                    o = arr;
                } else if(kompType == Character.TYPE) {
                    //throw new IllegalStateException(
                    //  "deserializing not implemented for double array");
                    String s = null;
                    try {
                        char[] arr = new char[arrLen];
                        for(int i = 0; i < arrLen; ++i) {
                            pp.next(); //pp.readStartTag(stag);
                            pp.next();
                            s = pp.readContent();
                            if(s == null) {
                                throw new DeserializeException(
                                    "expected character content as unsigned short value and not "
                                        +stag+pp.getPosDesc());
                            }
                            int val = Integer.parseInt(s);
                            if(val < 0 || val > 65535) {
                                throw new DeserializeException(
                                    "value of unsignedShort element in array must be "+
                                        "in between 0 and 655535 and not "+val+pp.getPosDesc());
                            }
                            arr[i] = (char) val;
                            pp.next();
                        }
                        o = arr;
                    } catch(NumberFormatException nfe) {
                        throw new DeserializeException("each element in array must be unsignedShort"
                                                           +" and not '"+s+stag+pp.getPosDesc());
                    }
                } else {
                    throw new IllegalStateException("can not yet deserialize array of "+kompType);
                }
            } else {
                o = Array.newInstance(kompType, arrLen);
                for(int i = 0; i < arrLen; ++i) {
                    if(pp.next() != XmlPullParser.START_TAG)
                        throw new DeserializeException(
                            "unexpected end of items for array of length "+arrLen
                                + pp.getPosDesc());
                    pp.readStartTag(stag);
                    //System.err.println("struct deserializer reading stag="+stag);
                    //if(!"".equals(stag.getUri()))
                    //  throw new DeserializeException(
                    //    "namespace typed accessors not allowed inside array"
                    //      + pp.getErrPos());
                    // TODO deal with href
                    href = stag.getAttributeValueFromRawName("href");
                    if(href != null) {
                        String hid = href.substring(1); // skip # int '#id1'
                        if(dctx.hasId(hid)) {
                            Object val = dctx.getIdValue(hid);
                            Array.set(o, i, val);
                        } else {
                            dctx.addArrayFixup(o, i, kompType, hid);
                        }
                        if(pp.next() != XmlPullParser.END_TAG) //skip END_TAG
                            throw new DeserializeException(
                                "expected immediate end tag for href element"+pp.getPosDesc());
                    } else {
                        Object val =  enc.readObject(dctx, kompType, pp, stag);
                        Array.set(o, i, val);
                    }
                }
            }
            //if(id != null) {
            //  dctx.setIdValue(id, o);
            //}


            int state = pp.next();
            //empty array may have two mpty content that we should ignore...
            if(arrLen == 0 && state == XmlPullParser.CONTENT) {
                if(!pp.isWhitespaceContent())
                    throw new DeserializeException(
                        "empty array can not have text content"+pp.getPosDesc());
                state = pp.next();
            }
            if(state != XmlPullParser.END_TAG) {
                throw new DeserializeException(
                    "expected array end tag"+pp.getPosDesc());
            }
            return o;
        } catch(XmlMapException ex) {
            throw new DeserializeException(
                "problem with xml-java mapping"+pp.getPosDesc(), ex);
            //} catch(StructException ex) {
            //  throw new DeserializeException(
            //    "problem with struct accessor"+pp.getErrPos(), ex);
        }
        //*/
        //throw new DeserializeException("not implemented");
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


