/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HelloServer.java,v 1.19 2003/04/06 00:04:04 aslom Exp $
 * @author Lavanya Ramakrishnan mailto:laramakr@extreme.indiana.edu
 */

package hello;

import java.lang.reflect.InvocationHandler;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import soaprmi.Naming;
import soaprmi.NotBoundException;
import soaprmi.RemoteException;
import soaprmi.mapping.XmlMapException;
import soaprmi.server.SecureUnicastRemoteObject;
import soaprmi.server.UnicastRemoteObject;
import soaprmi.soaprpc.SoapServices;

/**
 * Demonstration of how to get standalone SoapRMI service bootstraped from command line.
 *
 * @version $Revision: 1.19 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class HelloServer
{

    public static void usage(String errMsg) {
        if(errMsg != null) System.err.println(errMsg);
        System.err.print(
            "Usage: \n"
                +"port | URL \tport number on which service should be exported \n"
                +"           \tor URL location of naming service (Naming) where to bind service reference\n"
                +"[-check_delegation]\twill only allow to execute method if delegation was passed in\n"
                +"[-name NAME]\tservice NAME to use (by default HelloService)"
                +"[-secure]\twill export service on secure endpoint using SSL\n"
                +"[-securityprovider PROVIDER]\tspecify security PROVIDER to use (ex. cog, cog_delegation or jsse)\n"
                +"[-chained URL]\tlocation of oter hello service to call\n"
                +"[-chained_securityprovider PROVIDER]\tsecurity provider to use to contact chained service\n"
                //| [rmi:|ldap:]//host:port/service_name]
                //+"[ [[http://]host:]port | ldap://host/... | cn=...  ]\n"
        );
        System.exit(1);
    }

    public static void main(String args[])
        throws RemoteException ,
        MalformedURLException,
        NotBoundException,
        UnknownHostException,
        XmlMapException
    {
        HelloServiceMapping.init();


        //      java.util.Properties props = System.getProperties();
        //      props.list(System.err);

        // process arguments
        boolean secure = false;
        boolean checkDelegation = false;
        String providerName = null;
        String location = null;
        String serviceName = "HelloService";
        String chainedHelloServiceLocation = null;
        String chainedProviderName = null;
        for (int i = 0 ; i < args.length ; ++i)
        {
            String arg = args[i];
            if(arg.startsWith("-")) {
                if("-secure".equals(arg)) {
                    secure = true;
                } else if("-check_delegation".equals(arg)) {
                    checkDelegation = true;
                } else if("-name".equals(arg)) {
                    i++;
                    if (i < args.length) {
                        serviceName = args[i];
                    } else {
                        usage("no NAME providedfor -name");
                    }
                } else if("-chained".equals(arg)) {
                    i++;
                    if (i < args.length) {
                        chainedHelloServiceLocation = args[i];
                    } else {
                        usage("no URL provided for -chained");
                    }
                } else if("-securityprovider".equals(arg)) {
                    secure = true;
                    i++;
                    if (i < args.length) {
                        providerName = args[i];
                    } else {
                        usage("no security PROVIDER specified");
                    }
                } else if("-chained_securityprovider".equals(arg)) {
                    //provider = true;
                    i++;
                    if (i < args.length) {
                        chainedProviderName = args[i];
                    } else {
                        usage("no chained security PROVIDER specified");
                    }
                } else if(arg.startsWith("-h")) {
                    usage(null);
                } else {
                    usage("unsupported option "+arg);
                }
            }
            else {
                if(location != null) {
                    usage("already specified location '"+location+"' can not be set to '"+arg+"'");
                    System.exit(1);
                }
                location = arg;
            }
        }
        if(location == null) location = "localhost";

        HelloService serverImpl = null;
        if(chainedHelloServiceLocation != null) {
            String providerToUse = chainedProviderName == null ? providerName : chainedProviderName;
            boolean useSecurityProvider = (chainedProviderName != null) || secure;
            serverImpl = new HelloAsStubServiceImpl(chainedHelloServiceLocation,
                                                    useSecurityProvider,
                                                    providerToUse);
        } else {
            serverImpl = new HelloServiceImpl(serviceName);
        }

        // add interceptor that will check if globus proxy delegation is used
        if(checkDelegation) {
            try {
                InvocationHandler delegationChecker = (InvocationHandler)
                    Class.forName("hello.SimpleDelegationChecker").newInstance();
                serverImpl = (HelloService)
                    SoapServices.getDefault().wrapService(serverImpl, delegationChecker);
                System.err.println(//HelloServer.class.getName()
                    "Server installed interceptor to check globus proxy delegation: "+delegationChecker);
            } catch(Exception ex) {
                System.err.println("could not load secure service impl");
                ex.printStackTrace();
            }
        }

        // this is exmaple of chaining of interceptors
        // first check delegation (if DelegationChecker class available) and then password
        //          soaprmi.server.BasicAuthWrapper passwordChecker =
        //              new soaprmi.server.BasicAuthWrapper(new String [] {"joe/sesame"});
        //
        //          serverImpl = (HelloService)
        //              SoapServices.getDefault().wrapService(serverImpl, passwordChecker);

        soaprmi.Remote remote = null;
        if( ! Character.isDigit(location.charAt(0)) ) {
            String name = location;
            if(location.indexOf("//") == -1) {
                name = (secure ? "rmis:" : "") + "//" + location;
            }
            if(location.indexOf("/") == -1) {
                name += "/" + serviceName;
            }

            System.out.println("Server exporting service on random socket port");
            if(secure) {
                if (providerName != null){
                    remote = SecureUnicastRemoteObject.exportObject(providerName,  serverImpl);
                } else {
                    remote = SecureUnicastRemoteObject.exportObject(serverImpl);
                }
            } else {
                remote = UnicastRemoteObject.exportObject(serverImpl);
            }

            System.out.println("Server attempting to rebind in the registry to the name "+name);
            Naming.rebind(name, remote);
            System.out.println("Server created and bound in the registry to "+name);

        } else {
            System.out.println("Server attempting to bind to socket port "+location);
            int port = Integer.parseInt(location);
            System.out.println("Secure trying to bind to port "+port);
            if(secure) {
                if (providerName != null){
                    remote = SecureUnicastRemoteObject.exportObject(providerName, serverImpl, port);
                    //remote = SecureUnicastRemoteObject.exportObject(
                    //providername, port, serverImpl, new Class[]{HelloService.class});
                } else{
                    remote = SecureUnicastRemoteObject.exportObject(serverImpl, port);
                }
            } else {

                //              // exporting services on the same TCP port
                //              Services services = SoapServices.newInstance(port);
                //              services.setMapping(soaprmi.soap.Soap.getDefault().getMapping());
                ////            UnicastRemoteObject.exportObject(
                ////                serverImpl, services, services.createGUID(), serverImpl.getClass().getInterfaces());
                ////            UnicastRemoteObject.exportObject(
                ////                serverImpl, services, services.createGUID(), serverImpl.getClass().getInterfaces());
                //              remote = UnicastRemoteObject.exportObject(serverImpl, services);
                //              remote = UnicastRemoteObject.exportObject(serverImpl, services);

                //remote = UnicastRemoteObject.exportObject(serverImpl, port);
                remote = UnicastRemoteObject.exportObject(serverImpl, port);
            }
            String remoteServiceUrl = SoapServices.getDefault().getStartpointLocation(remote);
            System.out.println("Server is available at "+remoteServiceUrl);

            System.out.println("Server waiting for conections...");

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


