/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: PackageTest.java,v 1.5 2003/11/15 00:34:24 aslom Exp $
 */

package soaprpc;

import interop.intf.EchoException;
import interop.intf.EchoPort;
import interop.intf.TestEntry;
import interop.intf.TestList;
import java.io.IOException;
import java.io.StringReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.gjt.xpp.XmlNode;
import soaprmi.AlreadyBoundException;
import soaprmi.NotBoundException;
import soaprmi.RemoteException;
import soaprmi.ServerException;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlMapException;
import soaprmi.port.Port;
import soaprmi.server.Services;
import soaprmi.soap.MarshalException;
import soaprmi.soap.SoapException;
import soaprmi.soap.Unmarshaller;
import soaprmi.soap.ValidationException;
import soaprmi.soaprpc.HttpSocketSoapServerFactory;
import soaprmi.soaprpc.SoapDispatcher;
import soaprmi.soaprpc.SoapServer;
import soaprmi.soaprpc.SoapServices;
import soaprmi.util.logging.Logger;

/**
 * Some tests to implementation of SOAP services.
 *
 */
public class PackageTest extends TestCase {
    private static Logger l = Logger.getLogger();
    private String testPortXml;
    private SoapServer server;

    public static void main (String[] args) {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }

    public PackageTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(PackageTest.class);
    }

    protected void setUp() throws SoapException, XmlMapException,
        AlreadyBoundException, NotBoundException, RemoteException,
        IOException
    {
        server = HttpSocketSoapServerFactory.getDefault().newSoapServer(0);
        l.fine("starting embedded web server "+server);
        server.startServer();
        l.fine("embedded web server started "+server);

        SoapDispatcher dsptr = server.getDispatcher();

        Services services = SoapServices.newInstance(); //NOTE: it is a new object!
        services.addDispatcher(dsptr);

        // map port type to Java interface that implements it
        XmlJavaMapping mapping = services.getMapping();
        mapping.mapPortType(
            "urn:soaprmi:echo","echo-port-type",
            EchoPort.class, null, false
        );

        // create port
        String portName = "echo-port";
        EchoPort echoPortImpl = new EchoPortImpl();
        EchoPort echoPortImpl2 = new EchoPortImpl();

        // wire all together
        Port port = services.createPort(
            portName,     // portName
            EchoPort.class,  // portType
            echoPortImpl   // implementaion of port type
        );

        Port port2 = services.createPort(
            portName+"2",     // portName
            EchoPort.class,  // portType
            echoPortImpl2   // implementaion of port type
        );

        // publish port to registry or maybe better whole component ...
        // registryImpl.bindPort("hello", port);

        l.finer("registered port "+port);
        testPortXml = port.toXml();
        l.finest("port description in xml:---\n"+
                     testPortXml + "---\n");

        // check some basic assumptions

        // references to local objects *are* local objects
        Object ref = services.createStartpoint(port);
        assertEquals(echoPortImpl, ref);
        // even after port is deserialized from XML
        StringReader sr = new StringReader(testPortXml);
        Port portCopy = (Port) Unmarshaller.unmarshal(Port.class, sr);
        Object refCopy = services.createStartpoint(portCopy);
        assertEquals(echoPortImpl, refCopy);
    }

    //  public void tearDown() {

    protected void tearDown() throws ServerException {
        l.fine("stopping embedded web server "+server);
        server.stopServer();
        l.fine("embedded web server stopped "+server);
    }

    public void testEcho(EchoPort ref, Object value)
        throws EchoException, RemoteException
    {
        Object o = ref.echo(value);
        assertEquals(value, o);
    }

    public void testServices()
        throws MarshalException, ValidationException, XmlMapException,
        NotBoundException, RemoteException, EchoException
    {
        l.fine("starting test");
        StringReader sr = new StringReader(testPortXml);
        //System.err.println("will user portXml="+testPortXml);
        Port port = (Port) Unmarshaller.unmarshal(Port.class, sr);

        Services services = SoapServices.newInstance(); //NOTE: it is a new object!
        XmlJavaMapping mapping = services.getMapping();

        // map port type to Java interface that implements it
        mapping.mapPortType(
            "urn:soaprmi:echo","echo-port-type",
            EchoPort.class, null, false
        );
        EchoPort ref = (EchoPort) services.createStartpoint(port);
        testEcho(ref, "Test string");

        testEcho(ref, "Alek &<> &#### <tag> <<< <!CDATA[[ ]]>]]>");
        //TODO: testEcho(ref, null);
        testEcho(ref, port);
        //TODO: testEcho(ref, new Integer(1));
        ref.echo("Bar");  // remote object will remeber lat value echoed
        String s = (String) ref.lastEcho();
        assertEquals("Bar", s);

        try {
            testEcho(ref, EchoPortImpl.MAGIC_FAIL_STRING);
            fail("excepted exception for special argument");
        } catch(RemoteException ex) {
            XmlNode fault = ex.getWsdlFault();
            assertNotNull(fault);
            assertEquals("urn:test", fault.getNamespaceUri());
            assertEquals("fault", fault.getLocalName());
        }

        
        // transport port through echo and make sure it is stil the same...
        Port port2 = (Port) ref.echo(port);
        assertEquals(port, port2);


        EchoPort ref2 = (EchoPort) services.createStartpoint(port2);
        ref2.echo("Foo");
        ref.echo("Bar");  // remote object will remeber lat value echoed
        s = (String) ref2.lastEcho();
        assertEquals("Bar", s);
        testEcho(ref2, "Alek");
        //TODO: testEcho(ref2, null);

        //testEcho(ref2, port);
        //testEcho(ref2, port2);
        //testEcho(ref, port2);


        TestEntry sample = new TestEntry();
        testEcho(ref, sample);
        TestList ll = TestList.newList(2);
        testEcho(ref, ll);

        /* TODO
         // make sure that references are preserved (not passed by value)
         EchoPort ref3 = (EchoPort) ref.echo(ref);
         ref3.echo("Foo");
         ref.echo("Bar3");  // remote object will remeber lat value echoed
         String s = (String) ref3.lastEcho();
         assertEquals("Bar3", s);
         */

        //TODO: test primitive types
        //TODO: test automatic (un)wrapping of primitive types
        //TODO: test multiple arguments - concat

        //TODO: test passing faults and unwrapping exceptions
        //TODO: test multi intefrace ports

        //TODO: test mapping of ports operations
        //TODO: test mapping of structs and accessors


        l.fine("test finished");
    }

    public void testGUID()
    {
        Services services = SoapServices.newInstance();
        // test that generating GUIDs really fast does not create duplicates
        for(int i = 1; i < 100; ++i) {
            String g1 = services.createGUID();
            String g2 = services.createGUID();
            assertTrue("generated GUIDS should be unique", ! g1.equals(g2));
        }
        //TODO: run test in two parallel threads (when have spare time...)
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

