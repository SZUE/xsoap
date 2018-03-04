/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: PackageTest.java,v 1.6 2003/04/06 00:04:26 aslom Exp $
 */

package rmi;

import interop.intf.EchoException;
import interop.intf.EchoPort;
import java.io.IOException;
import java.io.StringWriter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import soaprmi.AccessException;
import soaprmi.AlreadyBoundException;
import soaprmi.IncompatibleVersionException;
import soaprmi.NotBoundException;
import soaprmi.RemoteException;
import soaprmi.ServerException;
import soaprmi.mapping.XmlMapException;
import soaprmi.port.Port;
import soaprmi.registry.LocateRegistry;
import soaprmi.registry.Registry;
import soaprmi.server.RemoteRef;
import soaprmi.server.UnicastRemoteObject;
import soaprmi.soap.MarshalException;
import soaprmi.soap.Marshaller;
import soaprmi.soap.SoapException;
import soaprmi.soap.ValidationException;
import soaprmi.soaprpc.SoapServices;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;
import soaprpc.EchoPortImpl;

/**
 * Some tests to implementation of SOAP RMI API.
 *
 */
public class PackageTest extends TestCase {
    private static Logger logger = Logger.getLogger();
    private static final int TEST_REGISTRY_PORT = 5647;
    private Registry registry;

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

    protected void setUp() throws SoapException, XmlMapException,
        AlreadyBoundException, NotBoundException, RemoteException,
        AccessException, IOException
    {
        if(Log.ON) logger.fine("creating registry");
        registry = LocateRegistry.createRegistry(TEST_REGISTRY_PORT);
        Port registryPort = ((RemoteRef)registry).getSoapRMIPort();
        if(Log.ON) logger.fine("registry port="+registryPort);

        EchoPort echoPortImpl = new EchoPortImpl();

        SoapServices.getDefault().getMapping().mapPortType(
            "urn:soaprmi:echo","echo-port-type",
            EchoPort.class, null, false
        );

        RemoteRef ref = UnicastRemoteObject.exportObject(echoPortImpl);

        if(Log.ON) logger.finer("echo remote ref="+ref);

        Port echoPort = ref.getSoapRMIPort();
        StringWriter sw =  new StringWriter();
        Marshaller.marshal(echoPort, sw);
        sw.close();
        String testPortXml = sw.toString();
        if(Log.ON) logger.finest("echo remote ref port:---\n"
                                     +testPortXml+"---\n");

        if(Log.ON) logger.fine("binding echo remote ref in registry");
        registry.bind("echo", ref);

        Exception ex = null;
        try {
            registry.bind("echo", ref);
        } catch(RemoteException rex) {
            ex = rex;
        }
        assertNotNull(ex);
        assertEquals(ServerException.class, ex.getClass());
        registry.rebind("echo", ref);

    }

    //  public void tearDown() {

    protected void tearDown() throws ServerException {
    }

    public void testEcho(EchoPort ref, Object value)
        throws EchoException, RemoteException
    {
        Object o = ref.echo(value);
        assertEquals(value, o);
    }

    public void testAPI()
        throws MarshalException, ValidationException, XmlMapException,
        AccessException, NotBoundException, RemoteException, EchoException
    {
        if(Log.ON) logger.fine("starting test");

        registry = LocateRegistry.getRegistry(TEST_REGISTRY_PORT);

        EchoPort ref = (EchoPort) registry.lookup("echo");
        // unfortunately - this is local object reference (good for performance)
        testEcho(ref, "alek");

        assertEquals(ref.hashCode(),
                         ((RemoteRef)ref).getSoapRMIPort().hashCode());


        if(Log.ON) logger.fine("test finished");

        //Package pkg =  Package.getPackage("soaprmi");
        //String specTitle = pkg.getSpecificationTitle();

        //        soaprmi.Remote remote = null;
        //        Package pkg =  Package.getPackage("soaprmi");
        //        assertNotNull(pkg);
        //        System.out.println("Package name:\t" + pkg.getName());
        //        System.out.println("Spec title:\t" + pkg.getSpecificationTitle());
        //        System.out.println("Spec vendor:\t" + pkg.getSpecificationVendor());
        //        System.out.println("Spec version:\t" + pkg.getSpecificationVersion());
        //
        //        System.out.println("Impl title:\t" + pkg.getImplementationTitle());
        //        System.out.println("Impl vendor:\t" + pkg.getImplementationVendor());
        //        System.out.println("Impl version:\t" + pkg.getImplementationVersion());
        //
        //
        //        String specTitle = pkg.getSpecificationTitle();
        //        assertNotNull(specTitle);
        //        String specVersion = pkg.getSpecificationVersion();

        soaprmi.Version.require("1.0");
        soaprmi.Version.require("1.2");
        soaprmi.Version.require("1.2.1");
        soaprmi.Version.require("1.2.10");
        soaprmi.Version.require("1.2.20");

        // compatibility checks
        try {
            soaprmi.Version.require("0.1");
            fail("should fail when version is too early");
        } catch(IncompatibleVersionException ex) {}

        try {
            soaprmi.Version.require("1.2.200");
            fail("should fail when current version is too old");
        } catch(IncompatibleVersionException ex) {}

        try {
            soaprmi.Version.require("1.10");
            fail("should fail when current minor version is smaller than expected");
        } catch(IncompatibleVersionException ex) {}

        try {
            soaprmi.Version.require("2.0");
            fail("should fail when major version is newer than expected");
        } catch(IncompatibleVersionException ex) {}

        //format checks
        try {
            soaprmi.Version.require("1");
            fail("should fail when only major version is specified");
        } catch(IncompatibleVersionException ex) {}

        try {
            soaprmi.Version.require("1.-2");
            fail("should fail when version is negative");
        } catch(IncompatibleVersionException ex) {}

        try {
            soaprmi.Version.require("1,dfdfdf");
            fail("should fail when version is not formatted as N.M");
        } catch(IncompatibleVersionException ex) {}
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

