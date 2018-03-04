/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: RegistryImpl.java,v 1.5 2003/04/06 00:04:12 aslom Exp $
 */

package soaprmi.registry;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import soaprmi.Remote;
import soaprmi.RemoteException;
import soaprmi.*;
import soaprmi.port.Port;
import soaprmi.server.Dispatcher;
import soaprmi.server.UnicastRemoteObject;
//import soaprmi.util.*;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

//TODO: persistent registry implementation with lease renewal


/**
 * Note that PortRegistry does not need to know about interfaces as
 * it is storing only Port-s but to remote objects it is implementing Registry.
 */
public class RegistryImpl implements PortRegistry {
    
    public RegistryImpl() throws RemoteException {
    }
    
    public Port lookup(String name)
        throws RemoteException //, NotBoundException,  AccessException
    {
        //if(ports.containsKey(name) == false)
        Port port = (Port) ports.get(name);
        if(port == null)
            throw new RemoteException("no port bound to name "+name);
        return port;
    }
    
    public void bind(String name, Port port)
        throws RemoteException//, AlreadyBoundException, AccessException
    {
        //throw new RemoteException("not implemented yet");
        //port.setName(name); //reqted by madhu
        port.setUserName(name); //reqted by madhu
        if(ports.containsKey(name))
            throw new RemoteException("port "+port+" already bound to name "+name);
        ports.put(name, port);
    }
    
    
    public void unbind(String name)
        throws RemoteException//, NotBoundException, AccessException
    {
        if(ports.containsKey(name) == false)
            throw new RemoteException("no port bound to name "+name);
        ports.remove(name);
    }
    
    public void rebind(String name, Port port)
        throws RemoteException//, AccessException
    {
        //port.setName(name); //reqted by madhu
        port.setUserName(name); //reqted by madhu
        ports.put(name, port);
    }
    
    public String[] list()
        throws RemoteException//,  AccessException
    {
        Set names = ports.keySet();
        String[] list = new String[names.size()];
        names.toArray(list);
        return list;
    }
    
    
    private Logger l = Logger.getLogger();
    private Map ports = new HashMap();
    
    public static void main(String[] args) throws Exception {
        System.err.println("registry starting up");
        //PortRegistry serverImpl = new RegistryImpl();
        int port = LocateRegistry.DEFAULT_REGISTRY_PORT;
        if(args.length > 0)
            port = Integer.parseInt(args[0]);
        LocateRegistry.createRegistry(port);
        // wait until die...
        System.err.println("registry waiting for conenction");
        /*
         while(true) {
         try {
         Thread.currentThread().sleep(1000);
         } catch(Exception e) {
         e.printStackTrace();
         }
         }
         */
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


