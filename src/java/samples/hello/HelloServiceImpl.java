/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HelloServiceImpl.java,v 1.10 2003/11/24 14:38:50 aslom Exp $
 */

package hello;


import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import soaprmi.RemoteException;
import soaprmi.server.ConnectionContext;

/**
 * Demonstration of how to get standalone SOAP RMI service bootstraped from command line.
 *
 * @version $Revision: 1.10 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class HelloServiceImpl
    implements HelloService
{
    private String serviceName;
    
    public HelloServiceImpl(String serviceName) throws RemoteException {
        this.serviceName = serviceName;
    }
    
    /**
     * This is service implementation.
     */
    public String sayHello(String name) throws RemoteException {
        // some magic to retireve conenction socket
        Socket incomingSocket =
            soaprmi.soaprpc.SoapServices.getDefault().getConnectionContext().getIncomingSocket();
        //System.err.println(Thread.currentThread()+" "+incomingSocket);
        //try {Thread.currentThread().sleep(30*1000L);} catch (InterruptedException ex) {};
        //incomingSocket =
        //    soaprmi.soaprpc.SoapServices.getDefault().getConnectionContext().getIncomingSocket();
        //System.err.println(Thread.currentThread()+" "+incomingSocket);
        
        ConnectionContext ctx =
            soaprmi.soaprpc.SoapServices.getDefault().getConnectionContext();
        
        Map incomingProps = ctx.getIncomingProps();
        //ctx.setIncomingProps(...)
        
        String msg = "";
        if(incomingSocket != null) {
            msg += incomingSocket;
        }
        if(incomingProps != null) {
            Map inHttpHeaders = (Map) incomingProps.get("provides.http.headers");
            
            String cookie = (String)inHttpHeaders.get("cookie");
            
            if(cookie == null) {
                cookie = "XSOAP-SERVICE-SET-COOKIE="+System.currentTimeMillis();
                msg = " setting cookie="+cookie;
            } else {
                msg = " got cookie="+cookie;
            }
            Map outgoingProps = ctx.getOutgoingProps();
            Map outHttpHeaders = (Map) outgoingProps.get("outgoing.http.headers");
            if(outHttpHeaders == null) {
                outHttpHeaders = new HashMap();
                outgoingProps.put("outgoing.http.headers", outHttpHeaders);
            }
            outHttpHeaders.put("Set-Cookie", cookie);
        }
        return "Hello from "+serviceName+" to "+name
            +" (connection received from "+msg+")!";
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

