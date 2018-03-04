/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: PackageTest.java,v 1.32 2004/05/06 18:18:48 aslom Exp $
 */

package soap;

import interop.intf.TestEntry;
import interop.intf.TestList;
import interop.intf.TestableEntry;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlPullParserFactory;
import org.gjt.xpp.XmlRecorder;
import org.gjt.xpp.XmlStartTag;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.soap.DeserializeContext;
import soaprmi.soap.DeserializeException;
import soaprmi.soap.MarshalException;
import soaprmi.soap.Marshaller;
import soaprmi.soap.SerializeContext;
import soaprmi.soap.Soap;
import soaprmi.soap.SoapException;
import soaprmi.soap.SoapStyle;
import soaprmi.soap.Unmarshaller;
import soaprmi.soap.ValidationException;
import soaprmi.soapenc.HexBinary;
import soaprmi.soapenc.SoapEnc;
import soaprmi.util.Util;
import soaprmi.util.logging.Logger;

/**
 * Some tests to verify SOAP-ENC.
 *
 */
public class PackageTest extends TestCase {
    private static Logger l = Logger.getLogger();
    public final String TEST_STRING =
        "it  <tag> this special &amp; it is a \"string\" ]]>  "
        +"<![[CDATA[[ 'test'";
    
    public static void main (String[] args) {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }
    
    public PackageTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(PackageTest.class);
    }
    
    SerializeContext sctx;
    DeserializeContext dctx;
    
    protected void setUp() throws SoapException {
        sctx = Soap.getDefault().createSerializeContext();
        sctx.setDefaultEncodingStyle(SoapEnc.getDefault());
        //sctx.setSoapStyle(SoapStyle.IBMSOAP); //MSSOAP
        dctx = Soap.getDefault().createDeserializeContext();
        dctx.setDefaultEncodingStyle(SoapEnc.getDefault());
    }
    public void testInt(int value, String name)
        throws SoapException, IOException {
        StringWriter writer = new StringWriter();
        sctx.setWriter(writer);
        sctx.writeStartTag("testMethodInt", "urn:test-ns", "foo", true, null);
        if(name == null) {
            sctx.writeInt(value);
        } else {
            sctx.writeInt(value, name);
        }
        sctx.close();
        String result = writer.toString();
        //TODO if(Log.LEVEL > 2) l.debug("result = "+result);
        //System.err.println("result='\n"+result+"\n'");
        //if(Log.ON) l.log(Level.FINE, "result="+result);
        
        // setup deserializer
        StringReader reader = new StringReader(result);
        
        dctx.setReader(reader);
        
        try {
            XmlPullParser pp = dctx.getPullParser();
            XmlStartTag stag = dctx.getStartTag();
            if(pp.next() != XmlPullParser.START_TAG)
                throw new DeserializeException("expected start tag");
            pp.readStartTag(stag);
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "xml parsing exception when deserializing", ex);
        }
        
        // read back from stream
        int i = dctx.readInt();
        dctx.close();
        assertEquals(value, i);
    }
    
    
    public void testInt() throws SoapException, IOException {
        testInt(2, null);
        testInt(222333, null);
        testInt(1, "number-one");
    }
    
    
    
    public void testFloat(float value, String name)
        throws SoapException, IOException {
        StringWriter writer = new StringWriter();
        sctx.setWriter(writer);
        sctx.writeStartTag("testMethodInt", "urn:test-ns", "foo", true, null);
        if(name == null) {
            sctx.writeFloat(value);
        } else {
            sctx.writeFloat(value, name);
        }
        sctx.close();
        String result = writer.toString();
        //TODO if(Log.LEVEL > 2) l.debug("result = "+result);
        //System.err.println("result='\n"+result+"\n'");
        //if(Log.ON) l.log(Level.FINE, "result="+result);
        
        // setup deserializer
        StringReader reader = new StringReader(result);
        
        dctx.setReader(reader);
        
        try {
            XmlPullParser pp = dctx.getPullParser();
            XmlStartTag stag = dctx.getStartTag();
            if(pp.next() != XmlPullParser.START_TAG)
                throw new DeserializeException("expected start tag");
            pp.readStartTag(stag);
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "xml parsing exception when deserializing", ex);
        }
        
        // read back from stream
        float f = dctx.readFloat();
        dctx.close();
        assertEquals(Float.floatToIntBits(value), Float.floatToIntBits(f));
    }
    
    
    
    public void testFloat() throws SoapException, IOException {
        testFloat(12.22f, null);
        testFloat(222333.3232f, null);
        testFloat(1.0f, "number-one");
        testFloat(0.0f, "zero");
        testFloat(-0.0f, "minus-zero");
        testFloat(-1/0.0f, "minus-inf");
        testFloat(1/0.0f, "inf");
        testFloat(0/0.0f, "NaN");
    }
    
    
    public void testString(String value, String name)
        throws SoapException, IOException {
        StringWriter writer = new StringWriter();
        sctx.setWriter(writer);
        String sname = sctx.writeStartTag(
            "testMethodString", "urn:test-ns", "foo", true, null);
        if(name == null) {
            sctx.writeString(value);
        } else {
            sctx.writeString(value, name);
        }
        sctx.writeEndTag(sname);
        sctx.close();
        String result = writer.toString();
        
        //System.err.println("result='\n"+result+"\n'");
        
        // setup deserializer
        StringReader reader = new StringReader(result);
        
        dctx.setReader(reader);
        
        try {
            XmlPullParser pp = dctx.getPullParser();
            XmlStartTag stag = dctx.getStartTag();
            if(pp.next() != XmlPullParser.START_TAG)
                throw new DeserializeException("expected start tag");
            pp.readStartTag(stag);
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "xml parsing exception when deserializing", ex);
        }
        
        
        // read back from stream
        String s = dctx.readString();
        dctx.close();
        assertEquals(value, s);
    }
    
    public void testString() throws SoapException, IOException {
        testString(null, null);
        testString("Ala ma kota!", null);
        testString("<document><tag ala='1'>"
                       +"embedded document &amp <!CDATA[[ ]]></tag></document>", null);
        testString(TEST_STRING, null);
        testString(TEST_STRING, "document");
        testString(null, "test");
    }
    
    public Object testObject(Object value,
                             Class expectedType, String elName) throws SoapException, IOException {
        return testObject(value, value, expectedType, elName);
    }
    
    public Object testObject(Object value,
                             Class expectedType, String elName,
                             boolean printResult) throws SoapException, IOException {
        return testObject(value, value, expectedType, elName, printResult);
    }
    
    public Object testObject(Object value, Object expectedValue,
                             Class expectedType, String elName) throws SoapException, IOException {
        return testObject(value, expectedValue, expectedType, elName, false);
    }
    
    public Object testObject(Object value, Object expectedValue,
                             Class expectedType, String elName,
                             boolean printResult) throws SoapException, IOException {
        StringWriter writer = new StringWriter();
        sctx.setWriter(writer);
        String sname = sctx.writeStartTag(
            "testMethodString", "urn:test-ns", "foo", true, null);
        if(elName == null) {
            sctx.writeObject(value);
        } else {
            sctx.writeObject(value, elName);
        }
        sctx.done();
        sctx.writeEndTag(sname);
        sctx.close();
        
        String result = writer.toString();
        //TODO if(Log.LEVEL > 2) l.debug("result = "+result);
        
        if(printResult) System.err.println("result='\n"+result+"\n'");
        
        //debug(" result="+result);
        //if(Log.ON) l.log(Level.FINE, "result="+result);
        
        // setup deserializer
        StringReader reader = new StringReader(result);
        
        dctx.setReader(reader);
        
        try {
            XmlPullParser pp = dctx.getPullParser();
            XmlStartTag stag = dctx.getStartTag();
            if(pp.next() != XmlPullParser.START_TAG)
                throw new DeserializeException("expected start tag");
            pp.readStartTag(stag);
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "xml parsing exception when deserializing", ex);
        }
        
        // read back from stream
        Object deserializedObject = dctx.readObject(expectedType);
        //if(Log.ON) l.log(Level.FINE, "got so far o="+o);
        dctx.done();
        dctx.close();
        //if(Log.ON) l.log(Level.FINE, "final o="+o);
        Class klass = (deserializedObject != null) ? deserializedObject.getClass() : expectedType;
        if(klass != null && klass.isArray()) {
            Class kompType = klass.getComponentType();
            if(kompType.isPrimitive() == false) {
                assertTrue( Arrays.equals((Object[])value, (Object[])deserializedObject) );
            } else if(kompType.equals(Double.TYPE)) {
                assertTrue( Arrays.equals((double[])value, (double[])deserializedObject) );
            } else if(kompType.equals(Integer.TYPE)) {
                assertTrue( Arrays.equals((int[])value, (int[])deserializedObject) );
            } else if(kompType.equals(Long.TYPE)) {
                assertTrue( Arrays.equals((long[])value, (long[])deserializedObject) );
            } else if(kompType.equals(Byte.TYPE)) {
                assertEquals(printable((byte[])value), printable((byte[])deserializedObject) );
                assertTrue( Arrays.equals((byte[])value, (byte[])deserializedObject) );
            } else if(kompType.equals(Character.TYPE)) {
                assertEquals(printable((char[])value), printable((char[])deserializedObject) );
                assertTrue( Arrays.equals((char[])value, (char[])deserializedObject) );
            } else {
                fail("no test for array of elements that are of primitive type '"+kompType+"'");
            }
        } else {
            assertEquals(expectedValue, deserializedObject);
        }
        //return result;
        return deserializedObject;
    }
    
    public void testObject(SoapStyle style)
        throws SoapException, IOException {
        sctx.setSoapStyle(style);
        
        
        Object[] arr = new Object[1];
        arr[0] = "test string";
        testObject(arr, arr, Object[].class, "testArr1", false);
        
        arr = null;
        testObject(arr, Object[].class, "testNullArr");
        testObject(arr, Object[].class, null);
        if(style.XSI_TYPED) testObject(arr, null, null);
        
        
        arr = new Object[3];
        arr[0] = new Integer(7);
        arr[1] = null;
        arr[2] = "hello";
        
        testObject(arr, Object[].class, "testArr3");
        testObject(arr, Object[].class, null);
        if(style.XSI_TYPED) testObject(arr, null, null);
        
        Vector v = null;
        testObject(v, Vector.class, "testNullVector", false);
        testObject(v, Vector.class, null);
        if(style.XSI_TYPED) testObject(v, null, null);
        
        v = new Vector();
        for (int i = 0; i < arr.length; i++) {
            v.addElement(arr[i]);
        }
        testObject(v, Vector.class, "testVector3", false);
        testObject(v, Vector.class, null, false);
        // this can not b eused as SoapEnc will not know to expectedType to convertFrom
        //if(style.XSI_TYPED) testObject(v, null, null, true);
        
        TestEntry shared = new TestEntry();
        shared.setStringValue("shared sub object");
        TestList list1 = new TestList();
        list1.setStringValue("list1");
        list1.setObjectValue(shared);
        TestList list2 = new TestList();
        list2.setStringValue("list2");
        //if(style.DEEP_SER == false) list2.setObjectValue(shared);
        if(style.MULTI_REF) list2.setObjectValue(shared);
        list1.setLink(list2);
        //if(style.DEEP_SER == false) list2.setLink(list1); //circular list...
        if(style.MULTI_REF) list2.setLink(list1); //circular list...
        TestList rlist1 = (TestList) testObject(list1, TestList.class, null);
        TestList rlist2 = rlist1.getLink();
        //if(style.DEEP_SER == false)
        if(style.MULTI_REF) {
            assertSame(rlist1.getObjectValue(), rlist2.getObjectValue());
        }
        //if(style.DEEP_SER == false) assertSame(rlist1, rlist2.getLink());
        if(style.MULTI_REF) assertSame(rlist1, rlist2.getLink());
        
        //*
        //TODO java.lang.ClassNotFoundException: int
        //testObject(new Integer(7), Integer.TYPE, null);
        
        testObject(new Integer(7), Integer.class, null);
        testObject("test", String.class, null);
        //testObject("test", null, null);
        testObject("x", new Character('x'), Character.class, null);
        //testObject(new Character('C'), "C", null, null);
        testObject(new Character('C'), "C", String.class, null);
        
        TestEntry sample = new TestEntry();
        TestList ll = TestList.newList(2);
        TestList ll10 = TestList.newList(10);
        
        testObject(sample, TestEntry.class, null);
        if(style.XSI_TYPED) {
            testObject(sample, null, null);
            testObject(ll, null, null);
            testObject(ll10, null, null);
            
        }
        
        // check if embedded byte array work ok
        sample.setBbyteArr("ALEK".getBytes());
        testObject(sample, TestEntry.class, null);
        sample.setBbyteArr2(new byte[]{0,1,-128,127,64,63});
        testObject(sample, TestEntry.class, null);
        sample.setBbyteArr(sample.getBbyteArr2());
        testObject(sample, TestEntry.class, null);
        
        testObject(ll10, TestList.class, null);
        testObject(ll10, TestList.class, "list");
        /*
         PortType portType = new PortType();
         portType.setUri(
         "http://www.extreme.indiana.edu/soap/events/schema.xsd");
         portType.setName("listener");
         Endpoint epoint = new Endpoint();
         epoint.setLocation("http://localhost:7777/servlet/logger");
         //SoapRMIBinding binding = new SoapRMIBinding();
         Binding binding = new Binding();
         binding.setName("extreme.soaprmi");
         //binding.setMethodNs("urn:soaprmi:event-logger-component:logger-port");
         epoint.setBinding(binding);
         Port port = new Port();
         port.setName("logger-port");
         port.setPortType(portType);
         port.setEndpoint(epoint);
        
         testObject(port, port.getClass(), "Port");
         */
        //*/
        
        long[] larr0 = new long[]{};
        long[] larr1 = new long[]{1L};
        long[] larr2 = new long[]{Long.MIN_VALUE, Long.MAX_VALUE};
        testObject(larr0, null, null);
        testObject(larr1, null, null);
        testObject(larr2, null, null);
        
        int[] iarr0 = new int[]{};
        int[] iarr1 = new int[]{1};
        int[] iarr2 = new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE};
        testObject(iarr0, null, null);
        testObject(iarr1, null, null);
        testObject(iarr2, null, null);
        
        //boolean printResult
        testObject(new char[]{}, null, null); //, true);
        testObject(new char[]{'a'}, null, null); //, true);
        testObject(new char[]{'a','b'}, null, null); //, true);
        testObject(new char[]{'a','b','c'}, null, null); //, true);
        testObject(new char[]{Character.MAX_VALUE}, null, null); //, true);
        testObject(new char[]{Character.MIN_VALUE}, null, null); //, true);
        testObject(new char[]{(char)0, (char)1000, (char)65535}, null, null); //, true);
        
        String[] strArr = new String[]{"test","one", "two^&<> <![[CDATA"};
        testObject(strArr, strArr.getClass(), null);
        testObject(strArr, null, null);
        
        double[] darr0 = TestList.newDoubleArray(0);
        double[] darr1 = TestList.newDoubleArray(1);
        double[] darr2 = TestList.newDoubleArray(2);
        double[] darr10 = TestList.newDoubleArray(10);
        TestEntry[] arr0 = TestList.newArray(0);
        TestEntry[] arr1 = TestList.newArray(1);
        TestEntry[] arr2 = TestList.newArray(2);
        TestEntry[] arr10 = TestList.newArray(10);
        
        testObject(darr0, null, null);
        testObject(darr1, null, null);
        testObject(darr2, null, null);
        testObject(darr10, null, null);
        testObject(arr0, null, null);
        testObject(arr1, null, null);
        testObject(arr2, null, null);
        // they can be sent as null expected type as xi:type='soapenc:Array' is enforced!
        testObject(arr10, null, null);
        
        //test sending array of interfaces
        TestableEntry[] ia0 = arr0;
        testObject(ia0, null, null);
        TestableEntry[] ia1 = arr1;
        testObject(ia1, null, null);
        TestableEntry[] ia2 = arr2;
        testObject(ia2, null, null);
        TestableEntry[] ia10 = arr10;
        testObject(ia10, null, null);
        
        // now we make its type _really_ array of intrfaces
        ia0 = new TestableEntry[]{};
        testObject(ia0, null, null);
        ia1 = new TestableEntry[]{arr1[0]};
        testObject(ia1, null, null);
        ia2 = new TestableEntry[]{arr1[0], arr2[1]};
        testObject(ia2, null, null);
        
        //if(style.DEEP_SER == false) {
        if(style.MULTI_REF) {
            TestableEntry[] iaWithMultiRef = new TestableEntry[]{arr1[0], arr1[0]};
            assertSame("identity check", iaWithMultiRef[0], iaWithMultiRef[1]);
            TestableEntry[] iaWithMultiRefResult =
                (TestableEntry[]) testObject(iaWithMultiRef, null, null);
            assertSame("identity check for result",
                       iaWithMultiRefResult[0], iaWithMultiRefResult[1]);
        }
        
        ia10 = new TestableEntry[10];
        for (int i = 0; i < ia10.length; i++) {
            ia10[i] = arr10[i];
        }
        testObject(ia10, null, null);
        
        
        //testObject(arr10, arr10, null, null, true);
        //Vector v = ...
        //List l = ArrayList
        //Collection c = l;
        //testObject(v, v.getClass(), null);
        //testObject(arr2, v, v.getClass(), null);
        //testObject(arr2, l, l.getClass(), null);
        //testObject(v, arr2, null, null);
        //testObject(v, arr2, arr2.getClass(), null);
        //testObject(l, arr2, null, null);
        //testObject(l, arr2, arr2.getClass(), null);
        
        Class byteArrClass = (new byte[]{0}).getClass();
        // byte[] base64
        byte [] b = new byte[]{};
        testObject(b, byteArrClass, null);
        byte [] b0 = new byte[]{0};
        testObject(b0, byteArrClass, null);
        byte [] b01 = new byte[]{0,1};
        testObject(b01, byteArrClass, null);
        byte [] b012 = new byte[]{0,1,2};
        testObject(b012, byteArrClass, null);
        byte [] b0123 = new byte[]{0,1,2,3};
        testObject(b0123, byteArrClass, null);
        byte [] b01234 = new byte[]{0,1,2,3,4};
        testObject(b01234, byteArrClass, null);
        String test = "test&<><![[]]>>&lt;";
        byte [] btest = test.getBytes();
        testObject(btest, btest, byteArrClass, null, false);
        testObject(btest, byteArrClass, null);
        byte [] bfull = new byte[512];
        for(int i = 0; i < 256; ++i) {
            bfull[i] = (byte)i;
            bfull[511-i] = (byte)i;
        }
        testObject(bfull, byteArrClass, null);
        
        
        BigDecimal bigDecimal = new BigDecimal("10.10001");
        testObject(bigDecimal, BigDecimal.class, null);
        bigDecimal = new BigDecimal("0");
        testObject(bigDecimal, BigDecimal.class, null);
        
        HexBinary hex = new HexBinary("AA00");
        testObject(hex, HexBinary.class, null);
        
        
        Hashtable hashtable = new Hashtable();
        testObject(hashtable, Hashtable.class, null);
        
        hashtable.put("test", "foo");
        testObject(hashtable, Hashtable.class, null);
        
        hashtable.put(new Integer(123), new Double(1.2));
        testObject(hashtable, Hashtable.class, null);
        
        //Hashtable h2 = new Hashtable();
        //testObject(hashtable, h2, Hashtable.class, null);
        
        // multiple styles sctx.setSoapStyle(
        // mapping, change names, accessors
        // mapping bean, serializable ....
        // pass String as object...
        // use Notebook { String name, String id, Notebook[] subs, Page[] pages }
        // Page { id, name, byte[] binaryContent, String stringContent, URL href }
        //TODO testArr() array of int, and Objects
    }
    
    public void testObject() throws SoapException, IOException {
        testObject(SoapStyle.APACHESOAP);
        testObject(SoapStyle.SOAP11);
        testObject(SoapStyle.IBMSOAP);
        testObject(SoapStyle.DOCUMENT);
        testObject(SoapStyle.SOAP11);
    }
    
    public Object testMarshal(Object value, SoapStyle style)
        throws MarshalException, ValidationException, XmlPullParserException, IOException {
        StringWriter writer = new StringWriter();
        if(style != null) {
            Marshaller.marshal(value, writer, style);
        } else {
            Marshaller.marshal(value, writer);
        }
        
        String result = writer.toString();
        //debug(" marshalled result=\n"+result);
        l.fine("marshalled result="+result);
        //System.err.println("marshalled result="+result);
        
        
        //XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParserFactory factory = Util.getPullParserFactory();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(result));
        parser.setNamespaceAware(true);
        parser.setAllowedMixedContent(false);
        parser.next(); // move to start tag
        Object o = Unmarshaller.unmarshal(value.getClass(), parser);
        compareExpectedValue(value, o);
        
        // setup deserializer
        StringReader reader = new StringReader(result);
        o = Unmarshaller.unmarshal(value.getClass(), reader);
        compareExpectedValue(value, o);
        return o;
    }
    
    private void compareExpectedValue(Object value, Object o) throws XmlPullParserException {
        if(value != null) {
            if(value.getClass().isArray()) {
                Class klass = value.getClass();
                Class kompType = klass.getComponentType();
                if(kompType.isPrimitive() == false) {
                    assertTrue( Arrays.equals((Object[])value, (Object[])o) );
                } else if(kompType.equals(Double.TYPE)) {
                    assertTrue( Arrays.equals((double[])value, (double[])o) );
                } else if(kompType.equals(Byte.TYPE)) {
                    assertEquals(printable((byte[])value), printable((byte[])o) );
                    assertTrue( Arrays.equals((byte[])value, (byte[])o) );
                } else {
                    fail("no test for primite array type "+kompType);
                }
            } else if(value instanceof XmlNode) {
                assertEquals(serializeXmlToString((XmlNode)value),
                             serializeXmlToString((XmlNode)o));
            }
        } else {
            assertEquals(value, o);
        }
    }
    
    public void testMarshal(SoapStyle style)
        throws MarshalException, ValidationException, XmlPullParserException, IOException {
        /*
         PortType portType = new PortType();
         portType.setUri("http://www.extreme.indiana.edu/soap/events/schema.xsd");
         portType.setName("listener");
         Endpoint epoint = new Endpoint();
         epoint.setLocation("http://localhost:7777/servlet/logger");
         //SoapRMIBinding binding = new SoapRMIBinding();
         Binding binding = new Binding();
         epoint.setBinding(binding);
         Port port = new Port();
         port.setName("logger-port");
         port.setPortType(portType);
         port.setEndpoint(epoint);
        
         testMarshal(portType);
         testMarshal(binding);
         testMarshal(port);
         // previous binding was empty now it has something....
         binding.setName("extreme.soaprmi");
         //binding.setMethodNs("urn:soaprmi:event-logger-component:logger-port");
         testMarshal(binding);
         testMarshal(port);
         */
        
        
        testMarshal("Alek", style);
        testMarshal("", style);
        //NOTE: by design marshaller does not marshal null objects - may be chnaged in future ...
        //testMarshal(null, style);
        
        TestEntry entry = new TestEntry();
        entry.setIntValue(46);
        entry.setStringValue("foo");
        testMarshal(entry, style);
        
        double[] darr0= TestList.newDoubleArray(0);
        double[] darr1= TestList.newDoubleArray(1);
        double[] darr2 = TestList.newDoubleArray(2);
        double[] darr10 = TestList.newDoubleArray(10);
        TestEntry[] arr0= TestList.newArray(0);
        TestEntry[] arr1= TestList.newArray(1);
        TestEntry[] arr2 = TestList.newArray(2);
        TestEntry[] arr10 = TestList.newArray(10);
        
        testMarshal(darr0, style);
        testMarshal(darr1, style);
        testMarshal(darr2, style);
        testMarshal(darr10, style);
        testMarshal(arr0, style);
        testMarshal(arr1, style);
        testMarshal(arr2, style);
        testMarshal(arr10, style);
        
        //test sending array of interfaces
        TestableEntry[] ia0 = arr0;
        testMarshal(ia0, style);
        TestableEntry[] ia1 = arr1;
        testMarshal(ia1, style);
        TestableEntry[] ia2 = arr2;
        testMarshal(ia2, style);
        TestableEntry[] ia10 = arr10;
        testMarshal(ia10, style);
        
        // now we make its type _really_ array of intrfaces
        ia0 = new TestableEntry[]{};
        testMarshal(ia0, style);
        ia1 = new TestableEntry[]{arr1[0]};
        testMarshal(ia1, style);
        ia2 = new TestableEntry[]{arr1[0], arr2[1]};
        testMarshal(ia2, style);
        
        ia10 = new TestableEntry[10];
        for (int i = 0; i < ia10.length; i++) {
            ia10[i] = arr10[i];
        }
        testMarshal(ia10, style);
        
        //        // now for more sophisticated data structures
        //        TestEntry t1 = new TestEntry("a");
        //        TestEntry t2 = new TestEntry("a");
        //        TestEntry[] tArr = new TestEntry[]{t1, t2};
        //        assertEquals(t1, t2);
        //        assertTrue(t1 != t2);
        //        assertEquals(tArr[0], tArr[1]);
        //        assertTrue(tArr[0] != tArr[1]);
        //        TestEntry[] tResultArr = (TestEntry[]) testMarshal(tArr, style);
        //        assertEquals(tResultArr[0], tResultArr[1]);
        //        assertTrue(tResultArr[0] != tResultArr[1]);
        
        // this is to check that objects that are equal have their identity preserved
        String s1 = new String("a");
        String s2 = new String("a");
        String[] sArr = new String[]{s1, s2};
        assertEquals(s1, s2);
        assertTrue(s1 != s2);
        assertEquals(sArr[0], sArr[1]);
        assertTrue(sArr[0] != sArr[1]);
        String[] resultArr = (String[]) testMarshal(sArr, style);
        assertEquals(resultArr[0], resultArr[1]);
        assertTrue(resultArr[0] != resultArr[1]);
        
        
        //this test is to check that array with emoty and null string can be (de)serialized
        
        String[] stringArr = new String[]{s1, null, "", s2};
        String[] resultStringArr = (String[]) testMarshal(stringArr, style);
        assertNull(resultStringArr[1]);
        assertEquals("", resultStringArr[2]);
        
        Class byteArrClass = (new byte[]{0}).getClass();
        // byte[] base64
        byte [] b = new byte[]{};
        testMarshal(b, style);
        byte [] b0 = new byte[]{0};
        testMarshal(b0, style);
        byte [] b01 = new byte[]{0,1};
        testMarshal(b01, style);
        byte [] b012 = new byte[]{0,1,2};
        testMarshal(b01, style);
        byte [] b0123 = new byte[]{0,1,2,3};
        testMarshal(b0123, style);
        byte [] b01234 = new byte[]{0,1,2,3,4};
        testMarshal(b01234, style);
        String test = "test&<><![[]]>>&lt;";
        byte [] btest = test.getBytes();
        testMarshal(btest, style);
        byte [] bfull = new byte[512];
        for(int i = 0; i < 256; ++i) {
            bfull[i] = (byte)i;
            bfull[511-i] = (byte)i;
        }
        testMarshal(bfull, style);
        
        // some test for roundtriping XML
        XmlNode node = converStringToXmlTree("<test/>");
        //String stringified = serialzieXmlToString(node);
        testMarshal(node, style);
        node = converStringToXmlTree("<ns:test xmlns:ns='test-namespace'>some content</ns:test>");
        testMarshal(node, style);
    }
    
    
    public void testMarshal()
        throws MarshalException, ValidationException, XmlPullParserException, IOException {
        testMarshal(null);
        testMarshal(SoapStyle.DOCUMENT);
        testMarshal(SoapStyle.APACHESOAP);
        testMarshal(SoapStyle.IBMSOAP);
        //testMarshal(SoapStyle.MSSOAP);
        //testMarshal(SoapStyle.SOAP11);
    }
    
    
    
    public void testMarshalMultiref()
        throws Exception {
        //register mapping
        soaprmi.mapping.XmlJavaMapping mapping = soaprmi.soap.Soap.getDefault().getMapping();
        mapping.mapStruct("http://tempuri.org/example-namespace/", "foo", FooJavaBean.class);
        
        // create =some data to test
        FooJavaBean testobj = new FooJavaBean();
        testobj.setBar("hello baz");
        
        // set whatever data you care abou
        Object[] objarray = { testobj, testobj };
        assertSame(objarray[0], objarray[1]);
        // check marshaliung and unmarshalling
        StringWriter writer = new StringWriter();
        Marshaller.marshal(objarray, writer); //SOAP11 required for multi-ref
        String result = writer.toString();
        //System.out.println("result="+result);
        StringReader reader = new StringReader(result);
        Object deserializedObj = Unmarshaller.unmarshal(FooJavaBean.class, reader);
        Object[] deserializedObjArr = (Object[]) deserializedObj;
        assertSame(deserializedObjArr[0], deserializedObjArr[1]);
    }
    
    public void testMarshalXmlNode()
        throws Exception {
        XmlNode node0 = converStringToXmlTree("<node/>");
        XmlNode node1 = converStringToXmlTree(
            "<another-node attr='foo'>stuff<more>bar</more></another-node>");
        
        // set whatever data you care abou
        Object[] objarray = { node0, null, node1 };
        String strObjArr = testMarshalXmlNode(objarray);
        XmlNode[] xmlnodearray = { node0, null, node1 };
        testMarshalXmlNode(xmlnodearray);
        
        // and now compare serializations char by char ...
        
        StringWriter writer = new StringWriter();
        Marshaller.marshal(xmlnodearray, writer); //SOAP11 required for multi-ref
        String strXmlArr = writer.toString();
        //System.out.println(getClass()+" XMLNODE result="+strXmlArr);
        
        // when comapring remove type info
        int i = strObjArr.indexOf("]'>"); //SOAP-ENC:arrayType='n:anyType[3]'>
        i += "]'>".length();
        int j = strObjArr.lastIndexOf("</ArrayOfObject>");
        assertTrue(j > i);
        String s1 = strObjArr.substring(i, j);
        
        i = strXmlArr.indexOf("]'>");
        i += "]'>".length();
        j = strXmlArr.lastIndexOf("</ArrayOfXmlNode>");
        assertTrue(j > i);
        String s2 = strXmlArr.substring(i, j);
        
        assertEquals(printable(s1), printable(s2));
    }
    
    public String testMarshalXmlNode(Object[] objarray)
        throws Exception {
        // check marshaliung and unmarshalling
        StringWriter writer = new StringWriter();
        Marshaller.marshal(objarray, writer); //SOAP11 required for multi-ref
        String result = writer.toString();
        //System.out.println(getClass()+" result="+result);
        StringReader reader = new StringReader(result);
        XmlNode[] expectedArrayType = new XmlNode[0];
        Object deserializedObj = Unmarshaller.unmarshal(expectedArrayType.getClass(), reader);
        Object[] deserializedObjArr = (Object[]) deserializedObj;
        
        compareNodes((XmlNode)objarray[0],
                         (XmlNode)deserializedObjArr[0]);
        assertEquals(null,
                     deserializedObjArr[1]);
        //assertEquals(serializeXmlToString((XmlNode)objarray[2]),
        //             serializeXmlToString((XmlNode)deserializedObjArr[2]));
        compareNodes((XmlNode)objarray[2],
                         (XmlNode)deserializedObjArr[2]);
        //compareNodes((XmlNode)objarray[0],
        //                 (XmlNode)deserializedObjArr[2]);
        return result;
    }
    
    private static void compareStartTags(XmlStartTag left, XmlStartTag right)
        throws XmlPullParserException
    {
        assertEquals(left, right);
    }
    
    private static void compareNodes(XmlNode expected, XmlNode actual)
        throws XmlPullParserException
    {
        assertEquals("Expected "+serializeXmlToString(expected)
                         +" but got "+serializeXmlToString(actual),
                     expected, actual);
    }
    
    
    
    public void testMapping()
        throws Exception
    {
        XmlJavaMapping mapping = Soap.getDefault().getMapping();
        XmlJavaTypeMap intMap = mapping.queryTypeMap(null, Integer.TYPE);
        assertEquals(Integer.TYPE, intMap.javaClass());
        intMap = mapping.queryTypeMap(Soap.SOAP_ENC_NS, Integer.TYPE);
        assertEquals(Integer.TYPE, intMap.javaClass());
        XmlJavaTypeMap integerMap = mapping.queryTypeMap(Soap.SOAP_ENC_NS, Integer.class);
        assertEquals(Integer.TYPE, intMap.javaClass());
        // TODO namespace should be XSD ...
        //System.out.println("integerMap="+integerMap);
    }
    
    
    private static XmlNode converStringToXmlTree(String xmlString)
        throws XmlPullParserException {
        try {
            //XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParserFactory factory = Util.getPullParserFactory();
            XmlPullParser pp = factory.newPullParser();
            pp.setInput(new StringReader(xmlString));
            pp.setNamespaceAware(true);
            pp.next();
            XmlNode nodeTree = factory.newNode(pp);
            return nodeTree;
        } catch(IOException ex) {
            throw new XmlPullParserException("could not do conversion string to XML", ex);
        }
    }
    
    private static String serializeXmlToString(XmlNode node)
        throws XmlPullParserException {
        try {
            //XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParserFactory factory = Util.getPullParserFactory();
            XmlRecorder recorder = factory.newRecorder();
            Writer out = new StringWriter();
            recorder.setOutput(out);
            recorder.writeNode(node);
            out.flush();
            return out.toString();
        } catch(IOException ex) {
            throw new XmlPullParserException("could serialize XML to string", ex);
        }
    }
    
    private static final String printable(byte[] arr) {
        return printable(new String(arr, 0));
    }
    
    /** simple utility method -- good for debugging */
    private static final String printable(String s) {
        return printable(s.toCharArray());
    }
    
    private static final String printable(char[] arr) {
        StringBuffer retval = new StringBuffer();
        char ch;
        for (int i = 0; i < arr.length; i++) {
            switch (ch = arr[i]) {
                case 0 :
                    continue;
                case '\b':
                    retval.append("\\b");
                    continue;
                case '\t':
                    retval.append("\\t");
                    continue;
                case '\n':
                    retval.append("\\n");
                    continue;
                case '\f':
                    retval.append("\\f");
                    continue;
                case '\r':
                    retval.append("\\r");
                    continue;
                case '\"':
                    retval.append("\\\"");
                    continue;
                case '\'':
                    retval.append("\\\'");
                    continue;
                case '\\':
                    retval.append("\\\\");
                    continue;
                default:
                    if (ch < 0x20 || ch > 0x7e) {
                        String ss = "0000" + Integer.toString(ch, 16);
                        retval.append("\\u" + ss.substring(ss.length() - 4, ss.length()));
                    } else {
                        retval.append(ch);
                    }
                    continue;
            }
        }
        return retval.toString();
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







