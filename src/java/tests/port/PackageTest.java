/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: PackageTest.java,v 1.6 2003/04/06 00:04:26 aslom Exp $
 */

package port;

import java.io.*;
import java.util.Arrays;

import junit.framework.*;

import org.gjt.xpp.*;

//import soaprmi.port.*;
//import soaprmi.util.*;
import soaprmi.util.logging.Logger;

import soaprmi.port.*;
import soaprmi.soap.*;
import soaprmi.soapenc.SoapEnc;

import interop.intf.EchoPort;
import interop.intf.TestList;
import interop.intf.TestEntry;

/**
 * Some tests to verify SOAP-ENC.
 *
 */
public class PackageTest extends TestCase {
    private static Logger logger = Logger.getLogger();
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
        sctx.setSoapStyle(SoapStyle.IBMSOAP); //MSSOAP
        dctx = Soap.getDefault().createDeserializeContext();
        dctx.setDefaultEncodingStyle(SoapEnc.getDefault());
    }

    public Object testPortTransport(Object value,
                                    Class expectedType,
                                    String elName)
        throws SoapException, IOException
    {
        return testPortTransport(value, value, expectedType, elName);
    }

    public Object testPortTransport(Object value,
                                    Object expectedValue,
                                    Class expectedType,
                                    String elName)
        throws SoapException, IOException
    {
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

        //System.err.println("result='\n"+result+"\n'");

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
        Object o = dctx.readObject(expectedType);
        //if(Log.ON) l.log(Level.FINE, "got so far o="+o);
        dctx.done();
        dctx.close();
        //if(Log.ON) l.log(Level.FINE, "final o="+o);
        Class klass = (o != null) ? o.getClass() : expectedType;
        if(klass != null && klass.isArray()) {
            Class kompType = klass.getComponentType();
            if(kompType.isPrimitive() == false) {
                assertTrue( Arrays.equals((Object[])value, (Object[])o) );
            } else if(kompType.equals(Double.TYPE)) {
                assertTrue( Arrays.equals((double[])value, (double[])o) );
            } else {
                fail("no test for primite array type "+kompType);
            }
        } else {
            assertEquals(expectedValue, o);
        }
        return o;
    }

    public void testPortTransport(SoapStyle style)
        throws SoapException, IOException
    {
        sctx.setSoapStyle(style);

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
        //testPortTransport(port, port.getClass(), "Port");

        port.setName("logger-port");
        port.setPortType(portType);

        //testPortTransport(port, port.getClass(), "Port");
        port.setEndpoint(epoint);

        testPortTransport(port, port.getClass(), "Port");
    }

    public void testPortTransport() throws SoapException, IOException {
        testPortTransport(SoapStyle.SOAP11);
        testPortTransport(SoapStyle.IBMSOAP);
        testPortTransport(SoapStyle.DOCUMENT);
        testPortTransport(SoapStyle.SOAP11);
    }

    public void testPortMarshal(Object value)
        throws MarshalException, ValidationException
    {
        StringWriter writer = new StringWriter();
        Marshaller.marshal(value, writer);

        String result = writer.toString();
        //debug(" marshalled result=\n"+result);
        logger.fine("marshalled result="+result);
        //System.err.println("marshalled result="+result);

        // setup deserializer
        StringReader reader = new StringReader(result);
        Object o = Unmarshaller.unmarshal(value.getClass(), reader);
        if(value != null && value.getClass().isArray()) {
            Class klass = value.getClass();
            Class kompType = klass.getComponentType();
            if(kompType.isPrimitive() == false) {
                assertTrue( Arrays.equals((Object[])value, (Object[])o) );
            } else if(kompType.equals(Double.TYPE)) {
                assertTrue( Arrays.equals((double[])value, (double[])o) );
            } else {
                fail("no test for primite array type "+kompType);
            }
        } else {
            assertEquals(value, o);
        }
    }

    public void testPortMarshal()
        throws MarshalException, ValidationException
    {
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

        testPortMarshal(portType);
        testPortMarshal(binding);
        testPortMarshal(port);
        // previous binding was empty now it has something....
        binding.setName("extreme.soaprmi");
        //binding.setMethodNs("urn:soaprmi:event-logger-component:logger-port");
        testPortMarshal(binding);
        testPortMarshal(port);

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



