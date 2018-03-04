/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: InteropImpl.java,v 1.8 2003/04/06 00:04:06 aslom Exp $
 */

package interop.impl;

import java.math.BigDecimal;
import soaprmi.soapenc.HexBinary;
import java.util.Hashtable;

import soaprmi.RemoteException;
import soaprmi.mapping.*;

import interop.intf.Interop;
import interop.intf.InteropEx;
import interop.intf.SOAPStruct;
import interop.intf.SOAPLinkedList;

/**
 * Implementation interoperability test service.
 * FOr more details see: http://www.whitemesa.com/interop/proposal2.html
 *
 * @version $Revision: 1.8 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class InteropImpl implements InteropEx {
    public InteropImpl() {}
    public String echoString(String inputString) throws RemoteException {
        return inputString;
    }
    public String[] echoStringArray(String[] inputStringArray) throws RemoteException {
        return inputStringArray;
    }
    public int echoInteger(int inputInteger) throws RemoteException {
        return inputInteger;
    }
    public int[] echoIntegerArray(int[] inputIntegerArray) throws RemoteException {
        return inputIntegerArray;
    }
    public float echoFloat(float inputFloat) throws RemoteException {
        return inputFloat;
    }
    public float[] echoFloatArray(float[] inputFloatArray) throws RemoteException {
        return inputFloatArray;
    }
    public SOAPStruct echoStruct( SOAPStruct inputStruct) throws RemoteException {
        return inputStruct;
    }
    public SOAPStruct[] echoStructArray (SOAPStruct[] inputStructArray) throws RemoteException {
        return inputStructArray;
    }
    public void echoVoid() throws RemoteException {
    }
    public byte[] echoBase64(byte[] inputBase64) throws soaprmi.RemoteException
    {
        return inputBase64;
    }
    public HexBinary echoHexBinary(HexBinary hexBinary) throws soaprmi.RemoteException
    {
        return hexBinary;
    }
    public java.util.Date echoDate(java.util.Date inputDate)  throws soaprmi.RemoteException
    {
        return inputDate;
    }
    public BigDecimal echoDecimal(BigDecimal decimal) throws soaprmi.RemoteException {
        return decimal;
    }
    public boolean echoBoolean(boolean inputBoolean) throws soaprmi.RemoteException
    {
        return inputBoolean;
    }
    public Hashtable echoMap(Hashtable hashtable) throws soaprmi.RemoteException
    {
        return hashtable;
    }
    public Hashtable[] echoMapArray(Hashtable[] hashtableArr) throws soaprmi.RemoteException
    {
        return hashtableArr;
    }


    public SOAPLinkedList echoLinkedList(SOAPLinkedList  linkedList) throws RemoteException {
        return linkedList;
    }

    public static void mapParams(XmlJavaPortTypeMap portMap,
                                 String operationName,
                                 String paramName)
        throws XmlMapException
    {
        mapParams(portMap, operationName, paramName, "return");
    }

    public static void mapParams(XmlJavaPortTypeMap portMap,
                                 String operationName,
                                 String paramName,
                                 String returnName
                                ) throws XmlMapException
    {
        // extraxt mapping for operation
        XmlJavaOperationMap oMap = portMap.queryMethodRequest(operationName);

        // get in maessage and change part names
        XmlJavaMessageMap requestMsg = oMap.getRequest();
        XmlJavaPartMap[] reqParts = requestMsg.getParts();
        reqParts[0].setPartName(paramName);

        // get out message and change name of output
        XmlJavaMessageMap responseMsg = oMap.getResponse();
        XmlJavaPartMap[] resParts = responseMsg.getParts();
        resParts[0].setPartName(returnName);
    }

    public static void establishMappings(XmlJavaMapping javaMapping, Class iface) throws XmlMapException {
        XmlJavaPortTypeMap portMap =
            javaMapping.queryPortType(iface);


        mapParams(portMap, "echoString", "inputString");
        mapParams(portMap, "echoInteger", "inputInteger");
        mapParams(portMap, "echoIntegerArray", "echoIntegerArray");
        mapParams(portMap, "echoFloat", "inputFloat");
        mapParams(portMap, "echoFloatArray", "inputFloatArray");
        mapParams(portMap, "echoStruct", "inputStruct");
        mapParams(portMap, "echoStructArray", "inputStructArray");
        //mapParams(portMap, "echoVoid", null);
        mapParams(portMap, "echoBase64", "inputBase64");
        mapParams(portMap, "echoHexBinary", "inputHexBinary");
        mapParams(portMap, "echoDate", "inputDate");
        mapParams(portMap, "echoDecimal", "inputDecimal");
        mapParams(portMap, "echoBoolean", "inputBoolean");
        mapParams(portMap, "echoMap", "input");
        mapParams(portMap, "echoMapArray", "input");

    }

    /**
     * Start SoapRMI service for interoperability testing.
     */
    public static void main(String args[]) throws Exception {
        soaprmi.mapping.XmlJavaMapping mapping =
            soaprmi.soap.Soap.getDefault().getMapping();
        // disable XSOAP auto mapping
        mapping.setDefaultStructNsPrefix(null);
        establishMappings(mapping, Interop.class);
        establishMappings(mapping, InteropEx.class);

        // map the structures for complex objects
        // map SOAPStruct into namespace:http://soapinterop.org/ : SOAPStruct
        //mapping.mapStruct(null, "http://xml.apache.org/xml-soap", "SOAPStruct",
        //  SOAPStruct.class, null, null, false, false, true);
        //javaMapping.mapStruct(null, "http://soapinterop.org/xsd", "SOAPStruct",
        //                    SOAPStruct.class, null, null, false, false, true);
        mapping.mapStruct("http://soapinterop.org/xsd", "SOAPStruct",
                              SOAPStruct.class);
        mapping.queryTypeMap(null, SOAPStruct.class);
        mapping.queryTypeMap(null, "http://soapinterop.org/xsd", "SOAPStruct");

        // this mapping is required ot process Apache SOAP 2.x request incorrectly using ArrayOfSOAPStruct
        // ... <inputStructArray xmlns:ns2="http://soapinterop.org/xsd" xsi:type="ns2:ArrayOfSOAPStruct"
        //       xmlns:ns3="http://schemas.xmlsoap.org/soap/encoding/" ns3:arrayType="ns2:SOAPStruct[5]">
        SOAPStruct[] arrOfSoapStruct = new SOAPStruct[0];
        mapping.mapType(null, "http://soapinterop.org/xsd", "ArrayOfSOAPStruct",
                        arrOfSoapStruct.getClass(),
                        false /*simple type*/, // this is required or array deserializer will fail
                        false /*generated*/,
                        false /*override*/);


        // map SOAPLinkedList into namesapce:http://soapinterop.org/ : SOAPStruct
        int portNumber = 1568; // default TCP port number for this service
        if(args.length > 0) {
            try {
                portNumber = Integer.parseInt(args[0]);
            } catch(NumberFormatException ex) {
                System.out.println("could not parse port number "+args[0]
                                       +" will use default "+portNumber+": "+ex);
            }
        }
        System.out.println("Starting interop server on port "+portNumber);
        InteropEx serviceImpl = new InteropImpl();
        // endpoint namespace is http://soapinterop.org/
        final String SERVICE_NAME = "http://soapinterop.org/";
        soaprmi.server.UnicastRemoteObject.exportObject(serviceImpl, portNumber, SERVICE_NAME);
        System.out.println("service exported...");
        //self test
        System.out.println("self test...");
        interop.client.Main.testInterop(new interop.client.Main.Test(
                                            "xsoap","SoapRMI/1.2",
                                            "http://localhost:1568/")
                                       );

        System.out.println("waiting for connections on "+portNumber+"...");
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


