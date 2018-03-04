/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: LocateRegistry.java,v 1.5 2003/04/06 00:04:12 aslom Exp $
 */


package soaprmi.registry;

import java.io.*;
import java.net.InetAddress;
import soaprmi.UnknownHostException;
import soaprmi.RemoteException;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.port.*;
import soaprmi.server.Services;
import soaprmi.soap.*;
import soaprmi.soaprpc.SoapDispatcher;
//import soaprmi.soaprpc.SoapEmbeddedServer;
//import soaprmi.soaprpc.SoapEmbeddedServerFactory;
import soaprmi.soaprpc.SoapServer;
import soaprmi.soaprpc.SoapServerFactory;
import soaprmi.soaprpc.SoapServices;
import soaprmi.soaprpc.SecureSoapServices;
//import soaprmi.util.*;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

/**
 * @version $Revision: 1.5 $ $Author: aslom $
 * $Date: 2003/04/06 00:04:12 $ (GMT)
 */

public final class LocateRegistry {
    //protected static SoapServerFactory factory = new SoapEmbeddedServerFactory();
    private static Logger l = Logger.getLogger();
    
    public static final int DEFAULT_REGISTRY_PORT = 2999;
    public static final int DEFAULT_SECURE_REGISTRY_PORT = 2997;
    
    static {
        try {
            SoapServices.getDefault().getMapping().mapPortType(
                "urn:soaprmi-v11:services","registry-type",
                Registry.class, null, false
            );
        } catch(Exception e) {
            e.printStackTrace(); //TODO better error handling
        }
    }
    
    public static Registry getRegistry() throws RemoteException {
        Registry r = null;
        try {
            r = getRegistry(InetAddress.getLocalHost().getHostName(),
                            DEFAULT_REGISTRY_PORT);
        } catch(java.net.UnknownHostException e) {
            throw new RemoteException("can't locate registry", e);
        }
        return r;
    }
    
    public static Registry getRegistry(int port) throws RemoteException {
        Registry r = null;
        try {
            r = getRegistry(InetAddress.getLocalHost().getHostName(),
                            port);
        } catch(java.net.UnknownHostException e) {
            throw new RemoteException("can't locate registry", e);
        }
        return r;
    }
    
    public static Registry getRegistry(String host, int port)
        throws RemoteException
    {
        if(port == -1)
            port = DEFAULT_REGISTRY_PORT;
        String registryUrl = "http://"+host+":"+port+"/registry-port";
        return getRegistry(registryUrl, SoapServices.getDefault());
    }
    
    public static Registry getSecureRegistry(String host, int port)
        throws RemoteException
    {
        if(port == -1)
            port = DEFAULT_SECURE_REGISTRY_PORT;
        String registryUrl = "https://"+host+":"+port+"/registry-port";
        return getRegistry(registryUrl , SecureSoapServices.getDefault());
        
    }
    
    public static Registry getRegistry(String host, int port, boolean secure)
        throws RemoteException
    {
        if(secure) {
            return getSecureRegistry(host, port);
        } else {
            return getRegistry(host, port);
        }
    }

    public static Registry getRegistry(String registryUrl, Services services)
        throws RemoteException
    {
        
        try {
            //reg = new RegistryStub(host, port);
            Port portRef = new Port();
            portRef.setName("registry-port");
            PortType portType = new PortType();
            portType.setName("registry-type");
            portType.setUri("urn:soaprmi-v11:services");
            portRef.setPortType(portType);
            Endpoint epoint = new Endpoint();
            epoint.setLocation(registryUrl);
            portRef.setEndpoint(epoint);
            Binding binding = new Binding();
            binding.setName(portRef.getName());
            epoint.setBinding(binding);
            
            /*This will clreate on the wire registry reference like that:
             <Port>
             <name>registry-port</name>
             <portType id='id4'>
             <name>registry-port-type</name>
             <uri>urn:soaprmi-v11:services</uri>
             </portType>
             <endpoint id='id2'>
             <location>http://192.168.1.8:2999/registry-port</location>
             <binding id='id3'>
             <name>registry-port</name>
             </binding>
             </endpoint>
             
             </Port>
             */
            
            Registry reg = (Registry) services.createStartpoint(portRef);
            
            return reg;
        } catch(Exception e) {
            throw new RemoteException("can't locate registry", e);
        }
    }
    
    public static Registry createRegistry(int port) throws RemoteException {
        Services services = SoapServices.newInstance(port); //NOTE: it is a new object!
        return createRegistry(port, services);
    }
    
    public static Registry createSecureRegistry(int port) throws RemoteException {
        Services services = SecureSoapServices.newInstance(port); //NOTE: it is a new object!
        return createRegistry(port, services);
    }
    
    public static Registry createRegistry(int port, Services services) throws RemoteException {
        
        try {
            // map port type to Java interface that implements it
            XmlJavaMapping mapping = services.getMapping();
            mapping.mapPortType(
                "urn:soaprmi-v11:services","registry-port-type",
                PortRegistry.class, null, false
            );
            
            // create registry
            PortRegistry registryImpl = new RegistryImpl();
            
            // wire all together
            Port portRegistry = services.createPort(
                "registry-port",     // portName
                PortRegistry.class,  // portType
                registryImpl   // implementaion of port type
            );
            
            Port portRef = (Port) portRegistry.clone();
            PortType portType = portRef.getPortType();
            portType.setName("registry-type"); //switch from "registry-port-type"
            
            Registry reg =
                (Registry) SoapServices.getDefault().createStartpoint(portRef);
            
            return reg;
            
        } catch (Exception e) {
            //e.printStackTrace();
            throw new RemoteException("can't create registry on port "+port, e);
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

