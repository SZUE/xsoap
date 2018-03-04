/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HelloSpeedTest.java,v 1.4 2003/04/06 00:04:04 aslom Exp $
 */

package hello;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

import soaprmi.Naming;
import soaprmi.RemoteException;
import soaprmi.NotBoundException;
import soaprmi.registry.LocateRegistry;

import soaprmi.server.Services;
import soaprmi.soaprpc.HttpConnectionSoapServices;

/**
 * Simple speed test that executes remote method on server multiple times.
 *
 * @version $Revision: 1.4 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class HelloSpeedTest {

    public static void usage(String errMsg) {
        if(errMsg != null) System.err.println(errMsg);
        System.err.print(
            "Usage: location [count [length [JDK]  ] ]\n"
                +"location\tit can be URL to connect to specified server \n"
                +"      \t examples: http://localhost:3332/ or http://loccalhost/soaprmi/servlet/hello\n"
                +"       \tor just name under which to lookup service in Naming \n"
                +"      \t examples: [rmi:|ldap:]//host:port/cn=name,...]\n"
                +"COUNT\thow many times to call service\n"
                +"LENGTH\thow many characters to send to sayHello service\n"
                +"JDK\tif this option is provided that will uses JDK HttpURLConnection class\n"
                +"\tthat typically supports keep alive so is fater than staright socket open/close\n"
        );
        System.exit(1);
    }

    public static void main (String args[])
        throws RemoteException, NotBoundException,
        MalformedURLException, UnknownHostException
    {

        HelloService serverRef = null;
        String location;
        if(args.length == 0) {//usage("pass at least one argument");
            location = "localhost";
        } else {
            location = args[0];
        }
        int count = 500;
        if(args.length > 1) {
            count = Integer.parseInt(args[1]);
        }
        int len = 1;
        if(args.length > 2) {
            len = Integer.parseInt(args[2]);
        }
        Services services = soaprmi.soaprpc.SoapServices.getDefault();
        if(args.length > 3) {
            services = soaprmi.soaprpc.HttpConnectionSoapServices.getDefault();
        }

        if(! location.startsWith("http") ) {

            String name = location;
            if(location.indexOf("//") == -1) {
                //name = (secure ? "rmis:" : "") + "//" + location;
                name = "//" + location;
            }
            if(location.indexOf("/") == -1) {
                name += "/HelloService";
            }
            System.out.println("Client attempting to lookup in the registry to the name "+name);
            serverRef = (HelloService) Naming.lookup(name);
        } else {
            serverRef = (HelloService)
                services.createStartpoint(
                location,  // service location
                new Class[]{HelloService.class}, // remote service interface
                "urn:hello:sample" // SoapAction
            );

        }

       StringBuffer buf = new StringBuffer(len);
        final char[] ABACUS = { 'a', 'b', 'r', 'a', 'c', 'a', 'a', 'b', 'r', 'a' };
        for(int i  = 0; i < len; ++i) {
            buf.append(ABACUS[i % ABACUS.length]);
        }
        String abacus = buf.toString();

        long start = System.currentTimeMillis();
        String greeting;
        for (int i=0; i < count; ++i) {
            greeting = serverRef.sayHello(abacus);
        }
        long end = System.currentTimeMillis();
        System.out.println("Time for " + count + " requests (ms):" +
            (end-start) + ". Tps:" + count / ((end-start)/1000));


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

