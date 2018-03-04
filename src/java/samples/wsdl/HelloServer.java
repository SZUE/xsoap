/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HelloServer.java,v 1.2 2003/11/21 05:31:17 srikrish Exp $
 * @author Lavanya Ramakrishnan mailto:laramakr@extreme.indiana.edu
 */

package wsdl;


import soaprmi.Remote;
import soaprmi.server.RemoteRef;
import soaprmi.server.UnicastRemoteObject;
import soaprmi.wsdl.WSDLUtil;

import java.io.PrintWriter;
import java.io.FileOutputStream;

/**
 * Demonstration of how to get standalone SoapRMI service bootstraped from command line.
 *
 * @version $Revision: 1.2 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 * @author <a href="http://www.extreme.indiana.edu/~srikrish/">Sriram Krishnan</a>
 */
public class HelloServer
{
    public static final int DEFAULT_TCP_PORT = 9876;

    public static void usage(String errMsg)
    {
        if(errMsg != null) System.err.println(errMsg);
        System.err.print(
            "Usage: \n"
                +"[port] \tport number on which service should be exported (by default 9876)\n"
                +"\nExample: 9876\n"
        );
        System.exit(1);
    }

    public static void main(String args[]) throws Exception
    {

        // process arguments
        String location = null;
        for (int i = 0 ; i < args.length ; ++i)
        {
            String arg = args[i];
            if(location != null) {
                usage("already specified location '"+location+"' can not be set to '"+arg+"'");
                System.exit(1);
            }
            location = arg;
        }
        if(location == null) location = ""+DEFAULT_TCP_PORT;

        HelloService serverImpl = new HelloServiceImpl();

        System.out.println("Server attempting to bind to socket TCP port " + 
                           location);
        int port = Integer.parseInt(location);

        System.out.println("Server trying to bind to port " +
                           port);
        RemoteRef remote = UnicastRemoteObject.exportObject(serverImpl, port);

        System.out.println("Server is available at " + 
                           remote);

        System.out.println("Testing XSOAP Reference to WSDL conversion");
        String wsdlRef = WSDLUtil.convertRefToWSDL(remote,
                                                   "HelloService");
        System.out.println(wsdlRef);

        System.out.println("Testing WSDL to XSOAP Reference conversion");
        remote = WSDLUtil.convertWSDLToRef(wsdlRef);

        System.out.println("Invoking method using XSOAP reference " + 
                           remote);
        System.out.println(((HelloService) remote).sayHello("Hello from server"));

        System.out.println("WSDL Tests complete");
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

