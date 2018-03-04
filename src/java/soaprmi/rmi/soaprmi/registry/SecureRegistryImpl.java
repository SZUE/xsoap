/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SecureRegistryImpl.java,v 1.3 2003/04/06 00:04:12 aslom Exp $
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
import soaprmi.soaprpc.SoapServices;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

//TODO: persistent registry implementation with lease renewal


/**
 * Note that PortRegistry does not need to know about interfaces as
 * it is storing only Port-s but to remote objects it is implementing Registry.
 */
public class SecureRegistryImpl {
    
    
    public static void main(String[] args) throws Exception {
        System.err.println("secure registry starting up");
        //PortRegistry serverImpl = new RegistryImpl();
        int port = LocateRegistry.DEFAULT_SECURE_REGISTRY_PORT;
        if(args.length > 0)
            port = Integer.parseInt(args[0]);
        Remote remote = LocateRegistry.createSecureRegistry(port);
        // wait until die...
        int myPort = SoapServices.getDefault().getStartpointLocationPort(remote);
        System.err.println("secure registry waiting for conenction on "+myPort+" port");
        
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


