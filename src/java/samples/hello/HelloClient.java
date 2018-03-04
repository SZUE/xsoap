/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HelloClient.java,v 1.19 2003/09/23 17:45:59 aslom Exp $
 */

package hello;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import soaprmi.Naming;
import soaprmi.NotBoundException;
import soaprmi.RemoteException;
import soaprmi.mapping.XmlMapException;
import soaprmi.server.RemoteRef;
import soaprmi.soaprpc.HttpSocketSoapInvoker;
import soaprmi.soaprpc.PlainClientSocketFactory;
import soaprmi.soaprpc.SecureSoapServices;
/**
 * Simple client that executes remote method on server.
 *
 * @version $Revision: 1.19 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class HelloClient {

    public static void usage(String errMsg) {
        if(errMsg != null) System.err.println(errMsg);
        System.err.print(
            "Usage: location [-securityprovider name] [arguments ...]\n"
                +"location \tit can be URL to connect to specified server \n"
                +"         \t  examples: http://localhost:3332/ or http://loccalhost/soaprmi/servlet/hello\n"
                +"         \tor just name under which to lookup service in Naming \n"
                +"         \t  examples: [rmi:|ldap:]//host:port/cn=name,...]\n"
                +"arguments\tall remaining arguments will be used as strings\n"
                +"         \t and send to hello service\n"
        );
        System.exit(1);
    }

    public static void main (String args[])
        throws RemoteException, NotBoundException,
        MalformedURLException, UnknownHostException,
        XmlMapException
    {
        //HttpSocketSoapInvoker hsi =  (HttpSocketSoapInvoker) HttpSocketSoapInvoker.getDefault();
        //soaprmi.soaprpc.SoapServices.getDefault().setInvoker(hsi);

        soaprmi.soap.SoapStyle.setDefaultSoapStyle(soaprmi.soap.SoapStyle.SOAP11);
        HelloServiceMapping.init();
        // example of how to change default socket invoker
        //soaprmi.soaprpc.SoapInvoker invoker =  new soaprmi.soaprpc.HttpConnectionSoapInvoker();
        //soaprmi.soaprpc.SoapServices.getDefault().setInvoker(invoker);

        String location;
        if(args.length == 0) {//usage("pass at least one argument");
            location = "localhost";
        } else {
            if(args[0].startsWith("-h")) {
                usage(null);
            }
            location = args[0];
        }

        String securityProviderName = null;
        if (args.length > 1){
            if("-securityprovider".equals(args[1])) {
                if (args.length > 2 ) {
                    securityProviderName = args[2];
                } else {
                    usage("security provider name expected");
                }
            }
        }

        // NOTE: that location allows both "http" and "https"
        HelloService serverRef = findService(location,
                                             securityProviderName != null,
                                             securityProviderName);

        // play with cookies
        boolean clientSetCookie = false;
        if(clientSetCookie) {
            String myCookie= "XSOAP-CLIENT-COOKIE="+System.currentTimeMillis();
            ((RemoteRef)serverRef).getSoapRMIPort().getEndpoint().setCookie(myCookie);
        }

        int pos = 1;
        if(securityProviderName != null) pos = 3;
        do {
            String arg = (pos < args.length ? args[pos++] : "World");
            System.out.println(
                "Client executing remote method sayHello on server with '"+arg+"' argument");
            String greeting = serverRef.sayHello(arg);

            String cookie = ((RemoteRef)serverRef).getSoapRMIPort().getEndpoint().getCookie();
            System.out.println("Server said '"+greeting+"' "+(cookie != null? "cookie="+cookie:""));
        } while(pos < args.length);

    }

    public static HelloService findService(String location,
                                           boolean secure,
                                           String securityProvidername)
        throws UnknownHostException, MalformedURLException, RemoteException, NotBoundException
    {
        HelloService serverRef = null;
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
            if (secure) { //securityProvidername != null) {
                System.out.println("using security provider: "+securityProvidername);
                SecureSoapServices secureServices = SecureSoapServices.getDefault();
                if(securityProvidername != null) {
                    secureServices = SecureSoapServices.getDefault(securityProvidername);
                }
                serverRef = (HelloService) secureServices.createStartpoint(
                    location,  // service location
                    new Class[]{HelloService.class}, // remote service interface
                    "urn:hello:sample", // endpoint name
                    soaprmi.soap.SoapStyle.SOAP11,
                    "" // SOAPAction
                );
            } else {
                serverRef = (HelloService)
                    soaprmi.soaprpc.SoapServices.getDefault().createStartpoint(
                    location,  // service location
                    new Class[]{HelloService.class}, // remote service interface
                    "urn:hello:sample", // endpoint name
                    soaprmi.soap.SoapStyle.SOAP11,
                    "" // SOAPAction
                );
            }
        }
        return serverRef;
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

