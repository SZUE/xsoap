/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Soap.java,v 1.22 2003/05/18 13:16:06 aslom Exp $
 */

package soaprmi.soap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.gjt.xpp.XmlNode;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlMapException;
import soaprmi.soapenc.HexBinary;

/**
 * Entry point to SOAP related functionality.
 *
 * @version $Revision: 1.22 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class Soap {
    // XML schemas
    public final static String XSI_NS_1999
        = "http://www.w3.org/1999/XMLSchema-instance";
    //= "http://www.w3.org/1999/XMLSchema/instance/";
    public final static String XSD_NS_1999
        = "http://www.w3.org/1999/XMLSchema";
    public final static String XSI_NS_1999_NULL = "null";

    public final static String XSI_NS_2001 =
        "http://www.w3.org/2001/XMLSchema-instance";
    public final static String XSD_NS_2001 =
        "http://www.w3.org/2001/XMLSchema";
    public final static String XSI_NS_2001_NIL
        = "nil";

//    public static final String XSI_NS_CURR = XSI_NS_1999;
//    public static final String XSD_NS_CURR = XSD_NS_1999;
//    public static final String XSI_NS_CURR_NULL = XSI_NS_1999_NULL;
    public static final String XSI_NS_CURR = XSI_NS_2001;
    public static final String XSD_NS_CURR = XSD_NS_2001;
    public static final String XSI_NS_CURR_NULL = XSI_NS_2001_NIL;

    // SOAP envelope
    public final static  String SOAP_ENC_NS
        = "http://schemas.xmlsoap.org/soap/encoding/";
    public final static  String SOAP_ENC_NS_PREFIX = "SOAP-ENC";
    // SOAP encoding
    public final static  String SOAP_ENV_NS
        = "http://schemas.xmlsoap.org/soap/envelope/";
    public final static  String SOAP_ENV_NS_PREFIX = "SOAP-ENV";
    //private final static String SOAP10_ENV = "urn:schemas-xmlsoap-org:soap.v1";

    protected Soap() throws RuntimeException {
        try {
            initMapping();
        } catch(XmlMapException ex) {
            ex.printStackTrace();
            throw new RuntimeException("can't initialize standard mappings: " + ex);
        }
    }

    public static Soap getDefault()  {
        /*
         if(instance == null) {
         synchronized(Soap.class) {
         if(instance == null) {
         instance = new Soap();
         }
         }
         }
         */
        return instance;
    }

    public void initMapping() throws XmlMapException {
        mapping = new XmlJavaMapping();

        // enable SoapRMI auto mapping
        mapping.setDefaultStructNsPrefix(
            "urn:soaprmi-v11:temp-java-xml-type"); //"urn:soaprmi:struct";
        mapping.setDefaultPortTypeNsPrefix(
            "urn:soaprmi-v11:temp-java-port-type");
        mapping.setDefaultArrayComponentInterfaceNsPrefix(
            "urn:soaprmi-v11:temp-java-array-component-interface-type");

        // do mapping for defaul and SOAP_ENC_NS
        for (int i = 1; i <= 2; i++)
        {
            String style = (i == 1) ? SOAP_ENC_NS : null;
            // 1999
            mapping.mapType(style, XSD_NS_1999, "str", String.class); // pyGlobus xsd:str ...
            mapping.mapType(style, XSD_NS_1999, "string", String.class, true);
            mapping.mapType(style, XSD_NS_1999, "boolean", Boolean.TYPE);
            mapping.mapType(style, XSD_NS_1999, "long", Long.TYPE);
            mapping.mapType(style, XSD_NS_1999, "short", Short.TYPE);

            //TODO maybe it should be BigNum????
            //mapping.mapType(SOAP_ENC_NS, XSD_NS, "integer", Integer.TYPE, true, false, false);
            mapping.mapType(style, XSD_NS_1999, "int", Integer.TYPE);
            mapping.mapType(style, XSD_NS_1999, "float", Float.TYPE);
            mapping.mapType(style, XSD_NS_1999, "double", Double.TYPE);

            // 2001
            mapping.mapType(style, XSD_NS_2001, "string", String.class, true);
            mapping.mapType(style, XSD_NS_2001, "boolean", Boolean.TYPE, true);
            mapping.mapType(style, XSD_NS_2001, "long", Long.TYPE, true);
            mapping.mapType(style, XSD_NS_2001, "short", Short.TYPE, true);

            mapping.mapType(style, XSD_NS_2001, "int", Integer.TYPE, true);
            mapping.mapType(SOAP_ENC_NS, XSD_NS_2001, "float", Float.TYPE, true);
            mapping.mapType(SOAP_ENC_NS, XSD_NS_2001, "double", Double.TYPE, true);

            // CURR
            mapping.mapType(style, XSD_NS_CURR, "string", String.class, true);
            mapping.mapType(style, XSD_NS_CURR, "boolean", Boolean.TYPE, true);
            mapping.mapType(style, XSD_NS_CURR, "long", Long.TYPE, true);
            mapping.mapType(style, XSD_NS_CURR, "short", Short.TYPE, true);

            // see http://www.w3.org/TR/2000/CR-xmlschema-2-20001024/#unsignedShort
            mapping.mapType(style, XSD_NS_1999, "unsignedShort", Character.TYPE);
            mapping.mapType(style, XSD_NS_2001, "unsignedShort", Character.TYPE, true);
            mapping.mapType(style, XSD_NS_CURR, "unsignedShort", Character.TYPE, true);

            mapping.mapType(style, XSD_NS_CURR, "int", Integer.TYPE, true);
            mapping.mapType(style, XSD_NS_CURR, "float", Float.TYPE, true);
            mapping.mapType(style, XSD_NS_CURR, "double", Double.TYPE, true);

            mapping.mapType(style, XSD_NS_1999, "decimal", BigDecimal.class);
            mapping.mapType(style, XSD_NS_2001, "decimal", BigDecimal.class, true);
            mapping.mapType(style, XSD_NS_CURR, "decimal", BigDecimal.class, true);

            mapping.mapType(style, XSD_NS_1999, "hex", HexBinary.class);

            // for Spheon http://www.w3.org/1999/XMLSchema localName=hexBinary
            mapping.mapType(style, XSD_NS_1999, "hexBinary", HexBinary.class, true);
            mapping.mapType(style, XSD_NS_2001, "hexBinary", HexBinary.class, true);

            Class byteArrClass = (new byte[]{}).getClass();
            //http://www.w3.org/1999/XMLSchema localName=base64Binary
            mapping.mapType(style, XSD_NS_1999, "base64Binary", byteArrClass, true);
            mapping.mapType(style, XSD_NS_2001, "base64Binary", byteArrClass, true);
            mapping.mapType(style, SOAP_ENC_NS, "base64", byteArrClass, true);

            // for Spheon http://www.w3.org/1999/XMLSchema localName=dateTime
            //mapping.mapType(style, XSD_NS_1999, "dateTime", java.util.Date.class);
            mapping.mapType(style, XSD_NS_1999, "timeInstant", java.util.Date.class, true);
            //http://www.w3.org/2001/XMLSchema localName=dateTime
            mapping.mapType(style, XSD_NS_2001, "dateTime", java.util.Date.class, true);


            // handler for Hashtable (http://xml.apache.org/xml-soap:Map)
            mapping.mapType(style, "http://xml.apache.org/xml-soap", "Map", Hashtable.class);

            //mapping.mapStruct(style, XSD_NS_1999, "ur-type", Object.class);
            mapping.mapType(style, XSD_NS_1999, "ur-type", Object.class);
            // NOTE: order *really* matters do not chnage it unless u know what u are doing !!!
            mapping.mapType(style, XSD_NS_2001, "anyType", XmlNode.class, true);
            mapping.mapType(style, XSD_NS_2001, "anyType", Object.class, true);
        }

        // this is special thing to allow sending array of any objects without mapping
        Object[] arr = new Object[0];
        Class arrClass = arr.getClass();
        mapping.mapStruct(
            null, //encodingStyle,
            SOAP_ENC_NS,
            "Array", //localName,
            arrClass
        );

        //mapping.aliasNamespaces(SOAP_ENC_NS, XSD_NS, "http://www.w3.org/1999/XMLSchema/", false);
    }



    public void registerEncoding(String encodingStyleUri,
                                 EncodingStyle enc)
        throws SoapException
    {
        if(lookupEncoding(encodingStyleUri) != null)
            throw new SoapException(
                "encoding style already registered for uri="+encodingStyleUri);
        encUri2Style.put(encodingStyleUri, enc);
    }

    public EncodingStyle lookupEncoding(String encodingStyleUri) {
        return (EncodingStyle) encUri2Style.get(encodingStyleUri);
    }


    public XmlJavaMapping getMapping() {
        return mapping;
    }

    public void setMapping(XmlJavaMapping mapping_) {
        mapping = mapping_;
    }



    // --- ser/deser pool management

    public SoapDeserializeContext createDeserializeContext()
        throws DeserializeException

    {
        return new SoapDeserializeContextImpl(null);
    }

    public SoapDeserializeContext getDeserializeContext()
        throws DeserializeException
    {
        if( poolDeserList.size() > 0 ) {
            synchronized(poolDeserList) {
                if( poolDeserList.size() > 0 ) {
                    SoapDeserializeContext dctx =  (SoapDeserializeContext)
                        poolDeserList.remove(poolDeserList.size() - 1);
                    return dctx;
                }
            }
        }
        return new SoapDeserializeContextImpl(this);
    }

    public void returnDeserializeContextToPool(DeserializeContext dctx) {
        synchronized(poolDeserList) {
            if(dctx.isClosed() == false) {
                try
                {
                    dctx.close();
                }
                catch (DeserializeException e) {}
                catch (java.io.IOException e) {}
            }

            //as this function can be called multiple times -> avoid duplicates in pool
            for (int i = 0; i < poolDeserList.size(); i++)
            {
                if(poolDeserList.get(i) == dctx) {
                    return;
                }
            }
            poolDeserList.add(dctx);
        }
    }


    public SoapSerializeContext createSerializeContext() {
        return new SoapSerializeContextImpl(null);
    }

    public SoapSerializeContext getSerializeContext() {
        if( poolSerList.size() > 0 ) {
            synchronized(poolSerList) {
                if( poolSerList.size() > 0 ) {
                    SoapSerializeContext sctx =
                        (SoapSerializeContext) poolSerList.remove(poolSerList.size() - 1);
                    return sctx;
                }
            }
        }
        return new SoapSerializeContextImpl(this);
    }

    public void returnSerializeContextToPool(SerializeContext sctx) {
        synchronized(poolSerList) {
            //as this function can be called multiple times -> avoid duplicates in pool
            for (int i = 0; i < poolDeserList.size(); i++)
            {
                if(poolDeserList.get(i) == sctx) {
                    return;
                }
            }
            poolSerList.add(sctx);
        }
    }


    // internal state

    protected Map encUri2Style = new TreeMap();
    protected XmlJavaMapping mapping;
    protected static Soap instance = new Soap();

    // pools of ser/deser
    private List poolSerList = new ArrayList();
    private List poolDeserList = new ArrayList();

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

