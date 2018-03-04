/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SoapEnc.java,v 1.17 2003/11/06 03:10:11 aslom Exp $
 */

package soaprmi.soapenc;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlStartTag;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.mapping.XmlMapException;
import soaprmi.port.Port;
import soaprmi.soap.Converter;
import soaprmi.soap.DeserializeContext;
import soaprmi.soap.DeserializeException;
import soaprmi.soap.Deserializer;
import soaprmi.soap.EncodingStyle;
import soaprmi.soap.SerializeContext;
import soaprmi.soap.SerializeException;
import soaprmi.soap.Serializer;
import soaprmi.soap.Soap;
import soaprmi.soap.SoapException;
import soaprmi.soap.SoapStyle;
import soaprmi.util.Check;
import soaprmi.util.Util;

/**
 * Implementaion of standard SOAP 1.1 encoding style.
 *
 * @version $Revision: 1.17 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class SoapEnc implements EncodingStyle {
    protected final static String URI =
        "http://schemas.xmlsoap.org/soap/encoding/";
    private static SoapEnc instance;
    private Serializer defaultArraySerializer;
    private Serializer defaultStructSerializer;
    private Deserializer defaultArrayDeserializer;
    private Deserializer defaultStructDeserializer;
    private Hashtable deserializers = new Hashtable();
    private Hashtable serializers = new Hashtable();
    private Vector interfaceTypes = new Vector();
    private Vector interfaceDeserializers = new Vector();
    private Vector interfaceSerializers = new Vector();
    private Map cnvTo = new HashMap();
    private Map cnvFrom = new HashMap();

    private static Base64Handler base64Handler = new Base64Handler();
    private static XmlNodeHandler xmlNodeHandler;

    static {
        try {

            // register SOAP ENC style

            Soap.getDefault().registerEncoding(
                SoapEnc.getDefault().getEncodingStyleUri(),
                SoapEnc.getDefault());


            // handler for Decimal
            BigDecimalHandler decimalHandler = new BigDecimalHandler();
            SoapEnc.getDefault().registerClassEncodingHandler(
                BigDecimal.class,
                decimalHandler,
                decimalHandler);
            //Serializer ser = SoapEnc.getDefault().querySerializer(BigDecimal.class);


            // handler for xsd:hexBinary
            HexBinaryHandler hexBinaryHandler = new HexBinaryHandler();
            SoapEnc.getDefault().registerClassEncodingHandler(
                HexBinary.class,
                hexBinaryHandler,
                hexBinaryHandler);

            // handler for Hashtable (http://xml.apache.org/xml-soap:Map)
            HashtableHandler hashtableHandler = new HashtableHandler();
            SoapEnc.getDefault().registerClassEncodingHandler(
                Hashtable.class,
                hashtableHandler,
                hashtableHandler);

            // register Vector magic conversion to arrays

            SoapEnc.getDefault().registerClassEncodingHandler(
                Vector.class,
                new VectorSerializer(),
                null);


            //new VectorDeserializer());
            SoapEnc.getDefault().registerConverterTo(Vector.class,
                                                     new VectorConverter());


            // XML direct representation as XML tree node handler
            // deserilization is tricky and is depending on inernal stuff -- se below ...
            xmlNodeHandler = new XmlNodeHandler();
            SoapEnc.getDefault().registerInterfaceEncodingHandler(
                XmlNode.class,
                xmlNodeHandler,
                null);





        } catch(SoapException ex) {
            System.err.println(
                "unexpected exception during registration of SoapEnc : "+ex);
            ex.printStackTrace();
        }
    }

    protected SoapEnc() {
        defaultArraySerializer = new ArraySerializer();
        defaultStructSerializer = new StructSerializer();
        defaultArrayDeserializer = new ArrayDeserializer();
        defaultStructDeserializer = new StructDeserializer();

        // add default serializers and deserializers...
    }


    public static SoapEnc getDefault() {
        if(instance == null) {
            synchronized(Soap.class) {
                if(instance == null) {
                    instance = new SoapEnc();
                }
            }
        }
        return instance;
    }

    public Serializer querySerializer(
        Class javaType) throws SoapEncException
    {
        Serializer ser = (Serializer) serializers.get(javaType);
        if(ser != null) return ser;
        return queryInterfaceSerializer(javaType);
    }

    public Deserializer queryDeserializer(
        Class javaType) throws SoapEncException
    {
        Deserializer deser = (Deserializer) deserializers.get(javaType);
        if(deser != null) return deser;
        return queryInterfaceDeserializer(javaType);
    }

    public void registerClassEncodingHandler(
        Class javaType,
        Serializer ser,
        Deserializer deser) throws SoapEncException
    {
        if(javaType.isInterface() == true) {
            throw new SoapEncException(
                "expected class not interface "+javaType.getName()
                    +" when registering class (de)serializer");
        }
        if(deser != null && queryDeserializer(javaType) != null) {
            throw new  SoapEncException(
                "deserializer for "+javaType+" is already registered");
        }
        if(ser != null && querySerializer(javaType) != null) {
            throw new  SoapEncException(
                "serializer for "+javaType+" is already registered");
        }
        if(deser != null) {
            deserializers.put(javaType, deser);
        }
        if(ser != null) {
            serializers.put(javaType, ser);
        }
    }

    protected int queryInterfacePos(
        Class javaInterface) throws SoapEncException
    {
        for (int i = 0; i < interfaceTypes.size(); i++)
        {
            Class c = (Class) interfaceTypes.elementAt(i);
            //System.err.println("query "+javaInterface+" to be of "+c);
            if(c.isAssignableFrom(javaInterface)) {
                return i;
            }
        }
        return -1;
    }

    public Deserializer queryInterfaceDeserializer(
        Class javaInterface) throws SoapEncException
    {
        int pos = queryInterfacePos(javaInterface);
        if(pos == -1) return null;
        return (Deserializer) interfaceDeserializers.elementAt(pos);
    }

    public Serializer queryInterfaceSerializer(
        Class javaInterface) throws SoapEncException
    {
        int pos = queryInterfacePos(javaInterface);
        if(pos == -1) return null;
        return (Serializer) interfaceSerializers.elementAt(pos);
    }

    public void registerInterfaceEncodingHandler(
        Class javaInterface,
        Serializer ser,
        Deserializer deser) throws SoapException
    {
        if(javaInterface.isInterface() == false) {
            throw new SoapEncException(
                "expected interface not class "+javaInterface.getName()
                    +" when registering interface (de)serializer");
        }
        int pos = queryInterfacePos(javaInterface);
        if(ser == null && deser == null) {
            throw new IllegalArgumentException(
                "both serializer and deserializer can not be null");
        }
        if(deser != null && pos != -1
           && interfaceDeserializers.elementAt(pos) != null)
        {
            throw new  SoapException(
                "deserializer for interface "+javaInterface.getName()
                    +" is already registered");
        }
        if(ser != null && pos != -1
           && interfaceSerializers.elementAt(pos) != null)
        {
            throw new  SoapException(
                "serializer for interface "+javaInterface.getName()
                    +" is already registered");
        }
        if(pos != -1) {
            if(deser != null) interfaceDeserializers.setElementAt(deser, pos);
            if(ser != null) interfaceSerializers.setElementAt(ser, pos);
        } else {
            interfaceTypes.addElement(javaInterface);
            interfaceDeserializers.addElement(deser);
            interfaceSerializers.addElement(ser);
        }
    }

    public Converter  queryConverterFrom(Class expectedType)
    {
        return (Converter) cnvFrom.get(expectedType);
    }

    public Converter  queryConverterTo(Class expectedType)
    {
        return (Converter) cnvTo.get(expectedType);
    }

    public void registerConverterFrom(Class expectedType, Converter cnv)
    {
        cnvFrom.put(expectedType, cnv);
    }

    public void registerConverterTo(Class expectedType, Converter cnv)
    {
        cnvTo.put(expectedType, cnv);
    }


    public String getEncodingStyleUri() {
        return URI;
    }

    public Serializer defaultArraySerializer() {
        return defaultArraySerializer;
    }

    public Serializer defaultStructSerializer() {
        return defaultStructSerializer;
    }

    public Deserializer defaultArrayDeserializer() {
        return defaultArrayDeserializer;
    }

    public Deserializer defaultStructDeserializer() {
        return defaultStructDeserializer;
    }


    // --- reading

    public Object readObject(DeserializeContext dctx,
                             Class expectedType,
                             XmlPullParser pp, XmlStartTag stag
                            )
        throws DeserializeException, XmlPullParserException, IOException
    {
        try {
            String id = stag.getAttributeValueFromRawName("id");

            if( "1".equals(stag.getAttributeValueFromName(Soap.XSI_NS_CURR, Soap.XSI_NS_CURR_NULL)) )
            {
                if(pp.next() != XmlPullParser.END_TAG) //skip END_TAG
                    throw new DeserializeException(
                        "expected immediate end tag for xsi:null element"+stag+pp.getPosDesc());
                if(id != null) {
                    dctx.setIdValue(id, null);
                }
                return null;
            }
            // special shortcut to handle literla XML
            //if(XmlNode.class.equals(expectedType)) {
            if(expectedType != null && XmlNode.class.isAssignableFrom(expectedType)) {
                XmlNode result
                    = (XmlNode) xmlNodeHandler.readObject(dctx, this, expectedType, null, pp, stag);
                if(id != null) {
                    dctx.setIdValue(id, result);
                }

                return result;
            }

            XmlJavaTypeMap typemap = null;
            Class klass = expectedType;
            boolean isArray = false;
            if(expectedType != null) {
                if(expectedType.isArray()) {
                    isArray = true;
                }
            }


            String xsiType = stag.getAttributeValueFromName(Soap.XSI_NS_CURR, "type");
            // guessing game...
            if(xsiType == null) {
                String foundNs = null;
                for(int i = 0; i < stag.getAttributeCount(); ++i) {
                    if( "type".equals(stag.getAttributeLocalName(i)) ) {
                        String ns = stag.getAttributeNamespaceUri(i);
                        if(Soap.XSI_NS_2001.equals(ns) || Soap.XSI_NS_1999.equals(ns)) {
                            xsiType = stag.getAttributeValue(i);
                        } else {
                            throw new DeserializeException(
                                "unknnow namespace '"
                                    +ns+"' for type attribute on element "+stag+pp.getPosDesc());

                        }
                        if(foundNs != null) {
                            throw new DeserializeException(
                                "can not resolve duplicate type attribute for '"
                                    +foundNs+"' and '"+ns+"'"+pp.getPosDesc());
                        }
                        foundNs = ns;
                    }
                }
            }
            if(xsiType != null) {
                String uri = pp.getQNameUri(xsiType);
                if(uri == null)
                    throw new DeserializeException(
                        "undeclared namespace prefix for xsi:type "+xsiType+pp.getPosDesc());
                String localName = pp.getQNameLocal(xsiType);
                typemap = dctx.queryTypeMap(this, uri, localName);
                klass = typemap.javaClass();
                if(expectedType == null) expectedType = klass;
                //System.err.println("TRACE xsi:type="+xsiType+" expectedType="+expectedType);
            } else if(!"".equals(xsiType = stag.getNamespaceUri())) {
                typemap = dctx.queryTypeMap(this, xsiType, stag.getLocalName());
                klass = typemap.javaClass();
                if(expectedType == null) expectedType = klass;
            } else if(isArray == false) {
                if(expectedType == null) {
                    throw new DeserializeException(
                        "expected class type must be specified to"
                            +" deserialize object without explicit type information "+stag
                            +pp.getPosDesc());
                }
                if(soaprmi.Remote.class.isAssignableFrom( expectedType )){
                    typemap = dctx.queryTypeMap(this, Port.class);
                    klass = expectedType;
                } else {
                    //if(!XmlNode.class.equals(expectedType)) {
                    typemap = dctx.queryTypeMap(this, expectedType);
                    klass = typemap.javaClass();
                }
                //}
            }

            if(klass != null && XmlNode.class.isAssignableFrom(klass)) {
                XmlNode result
                    = (XmlNode) xmlNodeHandler.readObject(dctx, this, expectedType, null, pp, stag);
                if(id != null) {
                    dctx.setIdValue(id, result);
                }
                return result;
            }


            if(klass == Object.class) {
                throw new DeserializeException("cannot deserialize"
                                                   +" generic object without explicit type information "+stag
                                                   +pp.getPosDesc());
            }
            //            if(klass == String.class) {
            //                return readString(dctx, pp, stag);
            //                // TODO use faster HashMap --> register BooleanDeserializer , ...
            //                // klasses below here are final!!
            //      } else
            Object result = null;
            if(klass == Boolean.TYPE || klass == Boolean.class) {
                boolean b = readBoolean(dctx, pp, stag);
                result = new Boolean(b);
            } else if(klass == Long.TYPE || klass == Long.class) {
                long l = readLong(dctx, pp, stag);
                result = new Long(l);
            } else if(klass == Short.TYPE || klass == Short.class) {
                short s = readShort(dctx, pp, stag);
                result = new Short(s);
            } else if(klass == Integer.TYPE || klass == Integer.class) {
                int i = readInt(dctx, pp, stag);
                result = new Integer(i);
            } else if(klass == Float.TYPE || klass == Float.class) {
                float f = readFloat(dctx, pp, stag);
                result = new Float(f);
            } else if(klass == Double.TYPE || klass == Double.class) {
                double d = readDouble(dctx, pp, stag);
                result = new Double(d);
            } else if(expectedType == Character.TYPE || expectedType == Character.class) {
                // this is special case to deserialize char that is encoded as xsd:string ...
                String s = readString(dctx, pp, stag);
                if(s == null) {
                    throw new DeserializeException(
                        "when deserializating into Character string can not be null"
                            +pp.getPosDesc());
                }
                if(s.length() != 1) {
                    throw new DeserializeException(
                        "when deserializating into Character string must have length of 1"
                            +pp.getPosDesc());
                }
                result = new Character(s.charAt(0));
            } else if(klass == String.class) {
                result = readString(dctx, pp, stag);
            } else if(klass.isPrimitive()) {
                throw new IllegalStateException("unsuppported primitive type "
                                                    +expectedType);
            }
            //if(klass == String.class) { // it can not be char - it had a chance above
            //    return readString(dctx, pp, stag);
            //}
            if(result == null) {
                Deserializer deser;
                if(isArray) {
                    if( Byte.TYPE.equals( klass.getComponentType() ) ) {
                        deser = base64Handler;
                    } else {
                        deser = defaultArrayDeserializer;
                    }
                } else {
                    deser = dctx.queryDeserializer(this, typemap);
                }

                result = deser.readObject(dctx, this, expectedType, typemap, pp, stag);

                if(result != null) {//&& expectedType != null)
                    result =  dctx.convert(this, result, expectedType);
                }
            }

            if(id != null) {
                dctx.setIdValue(id, result);
            }
            return result;

        } catch(XmlMapException ex) {
            throw new DeserializeException(
                "xml mapping problem when reading object "+pp.getPosDesc(), ex);
        }
    }

    public boolean readBoolean(DeserializeContext dctx,
                               XmlPullParser pp, XmlStartTag stag)
        throws DeserializeException, XmlPullParserException, IOException
    {
        try {
            if(pp.next() != XmlPullParser.CONTENT)
                throw new DeserializeException("expected content"+pp.getPosDesc());
            String s = pp.readContent();
            boolean b = s.charAt(0) == '1'
                || s.charAt(0) == 't'; //TOOD check against xsd spec
            if(pp.next() != XmlPullParser.END_TAG)
                throw new DeserializeException("expected end tag"+pp.getPosDesc());
            return b;
        } catch(NumberFormatException ex) {
            throw new DeserializeException("can't parse booleans value"
                                               +pp.getPosDesc(), ex);
        }
    }


    public long readLong(DeserializeContext dctx,
                         XmlPullParser pp, XmlStartTag stag)
        throws DeserializeException, XmlPullParserException, IOException
    {
        try {
            if(pp.next() != XmlPullParser.CONTENT)
                throw new DeserializeException("expected content"+pp.getPosDesc());
            long l = Long.parseLong(pp.readContent());
            if(pp.next() != XmlPullParser.END_TAG)
                throw new DeserializeException("expected end tag"+pp.getPosDesc());
            return l;
        } catch(NumberFormatException ex) {
            throw new DeserializeException("can't parse long value"
                                               +pp.getPosDesc(), ex);
        }
    }


    public short readShort(DeserializeContext dctx,
                           XmlPullParser pp, XmlStartTag stag)
        throws DeserializeException, XmlPullParserException, IOException
    {
        try {
            if(pp.next() != XmlPullParser.CONTENT)
                throw new DeserializeException("expected content"+pp.getPosDesc());
            short s = Short.parseShort(pp.readContent());
            if(pp.next() != XmlPullParser.END_TAG)
                throw new DeserializeException("expected end tag"+pp.getPosDesc());
            return s;
        } catch(NumberFormatException ex) {
            throw new DeserializeException("can't parse short value"
                                               +pp.getPosDesc(), ex);
        }
    }


    public int readInt(DeserializeContext dctx,
                       XmlPullParser pp, XmlStartTag stag)
        throws DeserializeException, XmlPullParserException, IOException
    {
        try {
            if(pp.next() != XmlPullParser.CONTENT) {
                throw new DeserializeException("expected content"+pp.getPosDesc());
            }
            // needs to call trim() as otherwise "17 " will lead to exception ... go figure!
            int i = Integer.parseInt(pp.readContent().trim());
            if(pp.next() != XmlPullParser.END_TAG)
                throw new DeserializeException("expected end tag"+pp.getPosDesc());
            return i;
        } catch(NumberFormatException ex) {
            throw new DeserializeException("can't parse int value"+pp.getPosDesc(), ex);
        }
    }


    public float readFloat(DeserializeContext dctx,
                           XmlPullParser pp, XmlStartTag stag)
        throws DeserializeException, XmlPullParserException, IOException
    {
        if(pp.next() != XmlPullParser.CONTENT)
            throw new DeserializeException("expected float number"
                                               +pp.getPosDesc());
        String value = pp.readContent();
        float f;
        try {
            f = Float.parseFloat(value);
        } catch(NumberFormatException ex) {
            if(value.equals("INF") || value.toLowerCase().equals("infinity")) {
                f = Float.POSITIVE_INFINITY;
            } else if (value.equals("-INF")
                       || value.toLowerCase().equals("-infinity")) {
                f = Float.NEGATIVE_INFINITY;
            } else if (value.equals("NaN")) {
                f = Float.NaN;
            } else {
                throw new DeserializeException(
                    "can't parse float value '"+value+"'"
                        +pp.getPosDesc(), ex);
            }
        }
        if(pp.next() != XmlPullParser.END_TAG)
            throw new DeserializeException("expected end tag"+pp.getPosDesc());
        return f;
    }


    public double readDouble(DeserializeContext dctx,
                             XmlPullParser pp, XmlStartTag stag)
        throws DeserializeException, XmlPullParserException, IOException
    {

        if(pp.next() != XmlPullParser.CONTENT)
            throw new DeserializeException("expected float number"
                                               +pp.getPosDesc());
        String value = pp.readContent();
        double d;
        try {
            d = Double.parseDouble(pp.readContent());
        } catch(NumberFormatException ex) {
            if(value.equals("INF") || value.toLowerCase().equals("infinity")) {
                d = Double.POSITIVE_INFINITY;
            } else if (value.equals("-INF")
                       || value.toLowerCase().equals("-infinity")) {
                d = Double.NEGATIVE_INFINITY;
            } else if (value.equals("NaN")) {
                d = Double.NaN;
            } else {
                throw new DeserializeException(
                    "can't parse double value '"+value+"'"
                        +pp.getPosDesc(), ex);
            }
        }
        if(pp.next() != XmlPullParser.END_TAG)
            throw new DeserializeException("expected end tag"+pp.getPosDesc());
        return d;
    }

    public String readString(DeserializeContext dctx,
                             XmlPullParser pp, XmlStartTag stag)
        throws DeserializeException, XmlPullParserException, IOException
    {
        // TODO: more robust would be to walk attribute list
        // hunting for http://../schema and null ...
        String xs = stag.getAttributeValueFromName(Soap.XSI_NS_CURR, Soap.XSI_NS_CURR_NULL);
        //System.err.println("xs="+xs);
        if( "1".equals(xs) ) {
            if(pp.next() != XmlPullParser.END_TAG)
                throw new DeserializeException(
                    "expected end tag"+pp.getPosDesc());
            return null;
        }
        String tagName = stag.getLocalName();
        byte state = pp.next();
        String s;
        if(state == XmlPullParser.CONTENT) {
            s = pp.readContent();
            state = pp.next();
        } else {
            s = "";
        }
        if(state != XmlPullParser.END_TAG)
            throw new DeserializeException("expected end tag"+pp.getPosDesc());
        return s;
    }

    // -- writing

    public void writeObject(
        SerializeContext sctx,
        Object o,
        String name,
        Class baseType) throws SerializeException, IOException
    {
        try {
            SoapStyle style = sctx.getSoapStyle();
            // 1. deal with null values
            if(o==null) {
                if(style.SERIALIZE_NULL == false && sctx.structLevel() > 0) {
                    //if(sctx.structLevel() == 0)
                    //    throw new SerializeException(
                    //"when null elements are not serialized root element can not be null");
                    return;
                } else if(style.XSI_TYPED) {
                    if(baseType != null && !baseType.isInterface() && !baseType.isArray()) {
                        XmlJavaTypeMap map = sctx.queryTypeMap(baseType);
                        sctx.writeXsiNull(name, map.getUri(), map.getLocalName());
                    } else {
                        //throw new SerializeException(
                        // "baseType must be specified to determine type of null object");
                        sctx.writeXsiNull(name);
                    }
                } else {
                    sctx.writeXsiNull(name);
                }
                return;
            }
            // 2. allow for dynamic discovery of object type
            Class klass = o.getClass();
            if(baseType == null) {
                baseType = klass;
            }
            if(Check.ON) Check.assertion(baseType != null);
            boolean forceXsiType = style.XSI_TYPED || klass != baseType;

            // 3. serialize primitive types, String and wrapped primitives
            if(klass == String.class) {
                writeString(sctx, o.toString(), name, forceXsiType);
                return;
            } else if(klass.isPrimitive()) {
                if(klass == Integer.TYPE) {
                    writeInt(sctx, ((Integer)o).intValue(), name, forceXsiType);
                } else {
                    throw new IllegalStateException("temp. unsupported type "+klass);
                }
                return;
            } else {
                Package pkg = klass.getPackage();
                String pkgName = pkg != null ? pkg.getName() : null; //TODO: profile it!
                if("java.lang".equals(pkgName)) {
                    if(klass == Boolean.class) {
                        writeBoolean(sctx, ((Boolean)o).booleanValue(), name, forceXsiType);
                    } else if(klass == Long.class) {
                        writeLong(sctx, ((Long)o).longValue(), name, forceXsiType);
                    } else if(klass == Short.class) {
                        writeShort(sctx, ((Short)o).shortValue(), name, forceXsiType);
                    } else if(klass == Integer.class) {
                        writeInt(sctx, ((Integer)o).intValue(), name, forceXsiType);
                    } else if(klass == Float.class) {
                        writeFloat(sctx, ((Float)o).floatValue(), name, forceXsiType);
                    } else if(klass == Double.class) {
                        writeDouble(sctx, ((Double)o).doubleValue(), name, forceXsiType);
                    } else if(klass == Character.class) {
                        writeString(sctx, o.toString(), name, forceXsiType);
                    } else {
                        throw new IllegalStateException("temp. unsupported type "+klass);
                    }
                    return;
                }
            }

            // 4. delegate serialization to specialized serializers
            //XmlJavaTypeMap map = sctx.queryTypeMap(klass);
            Serializer ser = sctx.querySerializer(this, klass);
            if(Check.ON) Check.assertion(ser != null);
            if(style.DEEP_SER || sctx.structLevel() == 0 /*|| ser.simpleValue()*/) {
                if(style.MULTI_REF) {
                    String id = sctx.getId(o);
                    if(id != null) {
                        // was it already serialized? then just write href
                        sctx.writeRef(name, id);
                    } else {
                        // otherwise serialize in-place
                        id = sctx.addId(o);
                        ser.writeObject(sctx, this, o, name, baseType, id);
                    }
                } else {
                    ser.writeObject(sctx, this, o, name, baseType, null);
                }
            } else {
                String href = sctx.addRef(ser, this, o, name, baseType);
                sctx.writeRef(name, href);
            }

        } catch (XmlMapException ex) {
            throw new SerializeException(
                "can't serialize object - mapping problem", ex);
        }
    }

    public void writeBoolean(
        SerializeContext sctx,
        boolean b,
        String name,
        boolean forceXsiType) throws SerializeException, IOException
    {
        Writer out = sctx.getWriter();
        out.write('<');
        out.write(name);
        if(forceXsiType) sctx.writeXsdType("boolean");
        out.write('>');
        out.write(b ? "1" : "0");
        out.write("</");
        out.write(name);
        out.write(">\n");
    }


    public void writeLong(
        SerializeContext sctx,
        long l,
        String name,
        boolean forceXsiType) throws SerializeException, IOException
    {
        Writer out = sctx.getWriter();
        out.write('<');
        out.write(name);
        if(forceXsiType) sctx.writeXsdType("long");
        out.write('>');
        out.write(Long.toString(l));
        out.write("</");
        out.write(name);
        out.write(">\n");
    }


    public void writeShort(
        SerializeContext sctx,
        short s,
        String name,
        boolean forceXsiType) throws SerializeException, IOException
    {
        Writer out = sctx.getWriter();
        out.write('<');
        out.write(name);
        if(forceXsiType) sctx.writeXsdType("short");
        out.write('>');
        out.write(Short.toString(s));
        out.write("</");
        out.write(name);
        out.write(">\n");
    }

    public void writeInt(
        SerializeContext sctx,
        int i,
        String name,
        boolean forceXsiType) throws SerializeException, IOException
    {
        Writer out = sctx.getWriter();
        out.write('<');
        out.write(name);
        if(forceXsiType) sctx.writeXsdType("int");
        out.write('>');
        out.write(Integer.toString(i));
        out.write("</");
        out.write(name);
        out.write(">\n");
    }


    public void writeFloat(
        SerializeContext sctx,
        float f,
        String name,
        boolean forceXsiType) throws SerializeException, IOException
    {
        Writer out = sctx.getWriter();
        out.write('<');
        out.write(name);
        if(forceXsiType) sctx.writeXsdType("float");
        out.write('>');
        // a bit inefficient but IEEE compliant :-)
        if(f == Float.POSITIVE_INFINITY) {
            out.write("INF");
        } else if(f == Float.NEGATIVE_INFINITY) {
            out.write("-INF");
        } else {
            out.write(Float.toString(f));
        }
        out.write("</");
        out.write(name);
        out.write(">\n");
    }

    public void writeDouble(
        SerializeContext sctx,
        double d,
        String name,
        boolean forceXsiType) throws SerializeException, IOException
    {
        Writer out = sctx.getWriter();
        out.write('<');
        out.write(name);
        if(forceXsiType) sctx.writeXsdType("double");
        out.write('>');
        if(d == Double.POSITIVE_INFINITY) {
            out.write("INF");
        } else if(d == Double.NEGATIVE_INFINITY) {
            out.write("-INF");
        } else {
            out.write(Double.toString(d));
        }
        out.write("</");
        out.write(name);
        out.write(">\n");
    }

    public void writeString(
        SerializeContext sctx,
        String s,
        String name,
        boolean forceXsiType) throws SerializeException, IOException
    {
        //if(Check.ON) Check.assert(s != null);
        if(s == null) {
            if(sctx.getSoapStyle().SERIALIZE_NULL == false) {
                return;
            }
            Writer out = sctx.getWriter();
            out.write('<');
            out.write(name);
            if(forceXsiType) sctx.writeXsdType("string");
            sctx.writeXsiNull();
            out.write("/>\n");
            return;
        } else {
            Writer out = sctx.getWriter();
            out.write('<');
            out.write(name);
            if(forceXsiType) sctx.writeXsdType("string");
            out.write('>');
            //out.write(s);
            Util.writeXMLEscapedString(out, s);
            out.write("</");
            out.write(name);
            out.write(">\n");
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


