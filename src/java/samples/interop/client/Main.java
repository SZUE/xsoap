/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Main.java,v 1.9 2003/04/06 00:04:05 aslom Exp $
 */

package interop.client;

import java.math.BigDecimal;
import java.util.Hashtable;
import soaprmi.soapenc.HexBinary;

import java.util.Arrays;

import interop.intf.Interop;
import interop.intf.SOAPStruct;

/**
 * Demonstration of SoapRMI client that access other SOAP
 * implementations using our simple dynamic proxy API.
 *
 * @version $Revision: 1.9 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class Main {

    public static void main (String args[]) throws Exception
    {

        // changing invocation transport - this suppports socket timeout and works better with HTTP 1.0
        soaprmi.soaprpc.HttpSocketSoapInvoker hsi =  (soaprmi.soaprpc.HttpSocketSoapInvoker)
            soaprmi.soaprpc.HttpSocketSoapInvoker.getDefault();
        hsi.setDefaultTimeout(1 * 60 * 1000); // 1 minute
        soaprmi.soaprpc.SoapServices.getDefault().setInvoker(hsi);

        System.out.println("Starting interop test...");

        String location = args.length > 0 ? args[0] : null;
        //"http://services.xmethods.net:9090/soap";
        //"http://localhost:1568/";

        Test[] tests = new Test[] {
            new Test("xsoap","SoapRMI/1.2",
                     "http://rainier.extreme.indiana.edu:1568/"),

                new Test("apache","Apache Axis",
                         "http://nagoya.apache.org:5049/axis/servlet/AxisServlet"
                        ),
                new Test("apache","Apache 2.1",
                         "http://nagoya.apache.org:5049/soap/servlet/rpcrouter"
                             //"urn:xmethodsInterop", "",
                             //"http://www.xmethods.com/services"
                        ),

//              new Test("4s4c", "4S4C 1.3",
//                       "http://soap.4s4c.com/ilab/soap.asp"
//                      ),
//              new Test("4s4c", "4S4C 3.0",
//                       "http://soap.4s4c.com/ilab2/soap.asp"
//                      ),
//
//              new Test("ASP.Net", "ASP.Net",
//                       "http://www.mssoapinterop.org/asmx/simple.asmx"
//                      ),
//
//              new Test("CapeConnect", "CapeConnect",
//                       "http://interop.capeclear.com/ccx/soapbuilders-round2"),
//
//
//
//              new Test("Delphi", "Delphi",
//                       "http://soap-server.borland.com/WebServices/Interop/cgi-bin/InteropService.exe/soap/InteropTestPortType"),
//
//              new Test("MS STKV3 Typed", "MS STKV3 Typed",
//                       "http://mssoapinterop.org/stkV3/InteropTyped.wsdl"),
//
//              new Test("MS STKV3 Untyped", "MS STKV3 Untyped",
//                       "http://mssoapinterop.org/stkV3/Interop.wsdl"),
//
//              new Test("IONA XMLBus", "IONA XMLBus",
//                       "http://interop.xmlbus.com:7002/xmlbus/container/InteropTest/BaseService/BasePort/"),

                new Test("Spheon JSOAP", "Spheon JSOAP",
                         "http://213.23.125.181:8081/RPC"),



                //              new Test("soaplite", "SOAP::Lite 0.47",
                //                       "http://services.soaplite.com/interop.cgi"),
                //
                //
                //
                //              new Test("net2_ut", "MS .NET Beta 2 (untyped on wire) ",
                //                       "http://131.107.72.13/test/simple.asmx"
                //                           //"http://soapinterop.org/", "http://soapinterop.org/"
                //                      ),
                //              new Test("net2_t", "MS .NET Beta 2 (typed on wire) ",
                //                       "http://131.107.72.13/test/typed.asmx"
                //                           //"http://soapinterop.org/", "http://soapinterop.org/"
                //                      ),
                //              new Test("mstk_ut","MS SOAP Toolkit 2.0 (untyped on wire)",
                //                       "http://131.107.72.13/stk/InteropService.asp"
                //                      ),
                //              new Test("mstk_t","MS SOAP Toolkit 2.0 (typed on wire)",
                //                       "http://131.107.72.13/stk/InteropTypedService.asp"
                //                      ),
                //              new Test("phalanx","Phalanx Soap",
                //                       "http://www.phalanxsys.com/interop/listener.asp"
                //                      ),
                //              new Test("kafka","Kafka XSLT Interop Service",
                //                       "http://www.vbxml.com/soapworkshop/services/kafka10/services/endpoint.asp?service=ilab"
                //                      ),
                //              new Test("frontier", "Frontier 7.0b26 (Userland)",
                //                       "http://www.soapware.org:80/xmethodsInterop",
                //                       "urn:xmethodsInterop", "/xmethodsInterop"),
                //              new Test("sqldata","SQLData SOAP Server",
                //                       "http://www.soapclient.com/interop/sqldatainterop.wsdl",
                //                       "http://tempuri.org/message/", "/soapinterop"
                //                      ),
                //              new Test("mesa","White Mesa SOAP RPC 1.4",
                //                       "http://services2.xmethods.net:8080/interop",
                //                       "urn:xmethodsInterop", "urn:interopLab#"
                //                      ),
        };
        String nick = null;
        if(location != null && (location.startsWith("http://") == false)) {
            nick = location;
            location = null;
        }
        if(location != null ) {
            testInterop(new Test("test", "Test fromm command line", location,
                                 "http://soapinterop.org/", "urn:action"
                                     //"http://xml.apache.org/xml-soap"
                                )
                       );

        } else {
            boolean found = false;
            for(int i = 0; i < tests.length; ++i)
                try {
                    if(nick == null
                       || (nick != null && nick.equals(tests[i].nick))) {
                        found = true;
                        testInterop(tests[i]);
                    }
                } catch(Exception ex) {
                System.err.println("Test FAILED!!!!!");
                ex.printStackTrace(System.err);
            }
            if(!found) {
                System.out.println("no test matching "+nick+" was found");
            }
        }
        System.out.println("finished.");
    }

    public static boolean testInterop(Test test) throws Exception {
        soaprmi.mapping.XmlJavaMapping mapping =
            soaprmi.soap.Soap.getDefault().getMapping();
        // disable SoapRMI auto mapping
        mapping.setDefaultStructNsPrefix(null);
        // map SOAPStruct into namespace:http://soapinterop.org/ : SOAPStruct
        mapping.mapStruct(null, test.structNs, "SOAPStruct",
                          SOAPStruct.class, null, null, false, false, true);



        Interop server = (Interop)
            soaprmi.soaprpc.SoapServices.getDefault().createStartpoint(
            test.location,
            new Class[]{Interop.class},
            test.namespace,
            //soaprmi.soap.SoapStyle.IBMSOAP,
                soaprmi.soap.SoapStyle.SOAP11,
            test.soapAction
        );

        System.out.println("\n===============================================\nrunning test for ");
        System.out.println("NAME: "+test.name);
        System.out.println("ENDPOINT: "+test.location);
        System.out.println("NAMESPACE: "+test.namespace);
        System.out.println("SOAPACTION: "+test.soapAction);
        //*
        String es = server.echoString("hello");
        check("echoString", "hello".equals(es));

        String[] sa = new String[] { "hello", "goodbye" };
        String[] esa = server.echoStringArray(sa);
        check("echoStringArray", Arrays.equals(sa, esa));

        int ei = server.echoInteger(5);
        check("echoInteger", ei == 5);

        int[] ia = new int[] { -1, 0, 1, 2, 3, 4, 5 };
        int[] eia = server.echoIntegerArray(ia);
        check("echoIntegerArray", Arrays.equals(ia, eia));

        float ef = server.echoFloat(5.5f);
        check("echoFloat", 5.5f, ef);

        float[] fa = new float[] { 5.2f, 6.2f, -1.3f };
        float[] efa = server.echoFloatArray(fa);
        check("echoFloatArray", Arrays.equals(fa, efa));
        //*/


        SOAPStruct struct = new SOAPStruct("Hello", 8, 10.2f);
        SOAPStruct estruct = server.echoStruct(struct);
        check("echoStruct", struct.equals(estruct));



        SOAPStruct[] structArr = new SOAPStruct[] {
            new SOAPStruct("test string", 5, 6.2f),
                new SOAPStruct("another test", 10, 12.4f),
        };
        SOAPStruct[] estructArr = server.echoStructArray(structArr);
        check("echoStructArray", Arrays.equals(structArr, estructArr));

        server.echoVoid();
        check("echoVoid", true);


        byte[] barr = new byte[]{1,2,3};
        byte[] ebarr = server.echoBase64(barr);
        check("echoBase64", Arrays.equals(barr, ebarr));


        HexBinary hexValue = new HexBinary("3344");
        HexBinary hex = server.echoHexBinary(hexValue);
        check("echoHexBinary", hexValue.equals(hex));

        java.util.Date date = new java.util.Date();
        java.util.Date edate = server.echoDate(date);
        check("echoDate", date.getTime() == edate.getTime());

        BigDecimal decimalValue = new BigDecimal("3.14159");
        BigDecimal decimal = server.echoDecimal(decimalValue);
        check("echoDecimal", decimalValue.equals(decimal));

        boolean boolVal = server.echoBoolean(true);
        check("echoBoolean(true)", boolVal == true);

        boolVal = server.echoBoolean(false);
        check("echoBoolean(false)", boolVal == false);


        Hashtable hashtableValue = new Hashtable();
        hashtableValue.put(new Integer(5), "String Value");
        hashtableValue.put("String Key", new java.util.Date());

        Hashtable hashtable = server.echoMap(hashtableValue);
        check("echoMap", hashtableValue.equals(hashtable));

        Hashtable secondHashtableValue = new Hashtable();
        secondHashtableValue.put(">this is the second map", new Boolean(true));
        secondHashtableValue.put("test", new Float(411));

        Hashtable[] hashtableArrValue = new Hashtable[]{hashtableValue, secondHashtableValue};
        Hashtable[] hashtableArr = server.echoMapArray(hashtableArrValue);
        check("echoMapArray", Arrays.equals(hashtableArrValue, hashtableArr));


        float f_inf = server.echoFloat(Float.POSITIVE_INFINITY);
        check("echoINF", Float.POSITIVE_INFINITY, f_inf);

        float f_ninf = server.echoFloat(Float.NEGATIVE_INFINITY);
        check("echo-INF", Float.NEGATIVE_INFINITY, f_ninf);

        float f_nan = server.echoFloat(Float.NaN);
        //check("echoNan", 5.5f, ef);


        SOAPStruct[] structArr2 = new SOAPStruct[] {
            new SOAPStruct(null, -10000, 1),
        };
        SOAPStruct[] estructArr2 = server.echoStructArray(structArr2);
        check("echoStructArray 2", Arrays.equals(structArr2, estructArr2));

        return true;
    }

    public static void check(String name, float f, float ef) {
        if(f == ef)
            check(name, true);
        else
            failed(name, "expected "+f+" and received "+ef);
    }

    public static void check(String name, boolean passed) {
        System.out.println(name + " : " + (passed ? "PASSED" : "FAILED"));
    }

    public static void failed(String name, String reason) {
        System.out.println(name + " : FAILED - reason: "+reason);
    }


    public static class Test {
        public String nick;
        public String name;
        public String location;
        public String soapAction;
        public String namespace;
        public String structNs;

        public Test(String nick_, String name_, String loc_) {
            this(nick_,name_, loc_,
                 "http://soapinterop.org/", "urn:soapinterop");
        }

        public Test(String nick_, String name_, String loc_,
                    String ns_, String action_) {
            this(nick_, name_, loc_, ns_, action_,
                 "http://soapinterop.org/xsd");
        }

        public Test(String nick_, String name_, String loc_,
                    String ns_, String action_,
                    String structNs_)
        {
            this.nick = nick_;
            this.name = name_;
            this.location = loc_;
            this.soapAction = action_;
            this.namespace = ns_;
            this.structNs = structNs_;
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


