/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: PackageTest.java,v 1.8 2003/04/06 00:04:26 aslom Exp $
 */

package mapping;

import java.io.*;
import java.lang.reflect.Method;

import junit.framework.*;

//import soaprmi.soapenc.SoapEnc;
import soaprmi.struct.*;
import soaprmi.mapping.*;
import soaprmi.util.logging.Logger;

import interop.intf.EchoPort;
import interop.intf.TestList;

/**
 * Some tests to verify XML-JAVA mapping for SOAP.
 *
 */
public class PackageTest extends TestCase {
    private static Logger l = Logger.getLogger();

    public PackageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(PackageTest.class);
    }

    protected void setUp() {
    }

    public void testPortMapping()
        throws XmlMapException, StructException, NoSuchMethodException
    {

        // lets first set up mapping - in future just unmarshal xml file
        XmlJavaMapping javaMapping = new XmlJavaMapping();
        final String NS = "http://schemas";
        final String LOCAL_NAME = "string";
        javaMapping.mapType(NS, LOCAL_NAME, String.class);

        Class klass = EchoPort.class;
        Method method = klass.getMethod("sendString", new Class[]{String.class});
        XmlJavaOperationMap[] operationsMap = {
            new XmlJavaOperationMap("SendString", "SendStringResponse", method)
        };


        javaMapping.mapPortType(
            "urn:soaprmi-tests",
            "test-port-type",
            EchoPort.class,
            operationsMap,
            false // override
        );

        // now use mappings
        XmlJavaPortTypeMap portMap =
            javaMapping.queryPortType("urn:soaprmi-tests", "test-port-type");
        //System.err.println("java interface type ="+portMap.getJavaType());
        XmlJavaOperationMap oMap = portMap.queryMethodRequest("sendString");
        assertNull(oMap);
        oMap = portMap.queryMethodRequest("SendString");
        assertNotNull(oMap);
        //System.err.println("java method: "+oMap.getJavaName());


        // now go further and mat each operaion arguments
        assertSame(operationsMap[0], oMap);

        XmlJavaMessageMap requestMsg = oMap.getRequest();
        //XmlJavaMessageMap[] getFaults();
        //setFaults(XmlJavaMessageMap[])

        XmlJavaPartMap[] reqParts = requestMsg.getParts();
        Class part1Type = reqParts[0].javaClass();
        assertEquals(String.class, part1Type);
        XmlJavaTypeMap part1XmlType = javaMapping.queryTypeMap(null, part1Type);
        //System.err.println("xml namespace of argument 1: "+part1XmlType.getUri());
        //System.err.println("xml localName of argument 1: "+part1XmlType.getLocalName());
        assertEquals(NS, part1XmlType.getUri());
        assertEquals(LOCAL_NAME, part1XmlType.getLocalName());

        XmlJavaMessageMap responseMsg = oMap.getResponse();
        XmlJavaPartMap[] resParts = responseMsg.getParts();
        assertEquals(0, resParts.length);

        reqParts[0].setPartName("helloString");
        //System.err.println("name of argument 1: "+reqParts[0].getPartName());
    }

    public void testDefaultPortMapping()
        throws XmlMapException, StructException, NoSuchMethodException
    {
        // lets first set up mapping - in future just unmarshal xml file
        XmlJavaMapping javaMapping = new XmlJavaMapping();
        final String NS = "http://schemas";
        final String LOCAL_NAME = "string";
        javaMapping.mapType(NS, LOCAL_NAME, String.class);

        final String DEFAULT_PORT_TYPE_NS = "http://schemas";
        javaMapping.setDefaultPortTypeNsPrefix(DEFAULT_PORT_TYPE_NS);

        XmlJavaPortTypeMap defPortMap =
            javaMapping.queryPortType(EchoPort.class);

        assertNotNull( defPortMap );
        assertTrue("prefix check", defPortMap.getUri().startsWith( DEFAULT_PORT_TYPE_NS ));

        // now use mappings
        XmlJavaPortTypeMap portMap =
            javaMapping.queryPortType(defPortMap.getUri(), defPortMap.getLocalName());

        XmlJavaOperationMap[] ops = portMap.getOperations();
        assertNotNull(ops);
        assertEquals(3, ops.length);
        XmlJavaOperationMap foundSendString = null;
        for (int i = 0; i < ops.length; i++)
        {
            if("sendString".equals(ops[i].getJavaName())) {
                foundSendString = ops[i];
            }
        }
        assertNotNull(foundSendString);

        //System.err.println("java interface type ="+portMap.getJavaType());
        XmlJavaOperationMap oMap = portMap.queryMethodRequest("SendString");
        assertNull(oMap);
        oMap = portMap.queryMethodRequest("sendString");
        assertNotNull(oMap);
        assertSame(foundSendString, oMap);
        //System.err.println("java method: "+oMap.getJavaName());

        XmlJavaMessageMap requestMsg = oMap.getRequest();

        XmlJavaPartMap[] reqParts = requestMsg.getParts();

        String part1TypeName = reqParts[0].getJavaType();
        assertNotNull(part1TypeName);

        Class part1Type = reqParts[0].javaClass();
        assertEquals(String.class, part1Type);
        assertEquals(part1TypeName, part1Type.getName());

        XmlJavaTypeMap part1XmlType = javaMapping.queryTypeMap(null, part1Type);
        //System.err.println("xml namespace of argument 1: "+part1XmlType.getUri());
        //System.err.println("xml localName of argument 1: "+part1XmlType.getLocalName());
        assertEquals(NS, part1XmlType.getUri());
        assertEquals(LOCAL_NAME, part1XmlType.getLocalName());

        XmlJavaMessageMap responseMsg = oMap.getResponse();
        XmlJavaPartMap[] resParts = responseMsg.getParts();
        assertEquals(0, resParts.length);

        reqParts[0].setPartName("helloString");
        //System.err.println("name of argument 1: "+reqParts[0].getPartName());
    }

    public void testStructMapping() throws XmlMapException, StructException {
        //assertEquals();
        XmlJavaMapping mapping = new XmlJavaMapping();

        XmlJavaAccessorMap acc = new XmlJavaAccessorMap();
        acc.setJavaName("link");
        acc.setXmlName("Next");

        // TODO add iint!!!
        XmlJavaAccessorMap[] accessors = new XmlJavaAccessorMap[]{acc};
        mapping.mapStruct(
            null,
            "urn:test",
            "linked-list",
            TestList.class,
            null, //"bean"
            accessors,
            false, // simpleType,
            false, //generated,
            false //override
        );

        XmlJavaTypeMap typeMap = mapping.queryTypeMap(
            //SoapEnc.getDefault().getEncodingStyleUri(),
            null, "urn:test", "linked-list");
        XmlJavaTypeMap typeMap2 = mapping.queryTypeMap(
            //SoapEnc.getDefault().getEncodingStyleUri(),
            null, TestList.class);
        assertEquals(typeMap, typeMap2);

        XmlJavaStructMap llMap = (XmlJavaStructMap) typeMap;
        //XmlJavaStructMap entryMap = (XmlJavaStructMap) mapping.queryTypeMap(
        //  SoapEnc.getDefault().getEncodingStyleUri(), TestEntry.class);

        StructAccessor llSA = llMap.structAccessor();

        TestList next = (TestList) llSA.newInstance();
        Object target = llSA.newInstance();
        llSA.setValue(target, "Next", next);
        Object oNext = llSA.getValue(target, "Next");
        assertEquals(oNext, next);
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



