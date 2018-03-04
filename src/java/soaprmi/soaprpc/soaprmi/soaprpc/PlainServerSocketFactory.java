/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: PlainServerSocketFactory.java,v 1.12 2004/10/29 07:20:41 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import soaprmi.ServerException;
import soaprmi.server.SoaprmiServerSocketFactory;
import soaprmi.util.logging.Logger;

/**
 * Socket factory that can be used by soaprmi embedded server.
 *
 * @version $Revision: 1.12 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class PlainServerSocketFactory implements SoaprmiServerSocketFactory {
    private static Logger logger = Logger.getLogger();
    public final static String XSOAP_HOST_IP_PROPERTY = "xsoap.host.ip";
    public final static String XSOAP_PORT_RANGE_PROPERTY = "xsoap.port.range";
    
    private static boolean initialized = false;
    private static TcpPortRange portRange;
    protected ServerSocket listenSocket;
    private static InetAddress myIp;
    protected int serverPort;
    
    static {
        initializePortRange();
    }
    
    protected PlainServerSocketFactory(int serverPort) throws IOException {
        this.serverPort = serverPort;
        if(serverPort == 0 && portRange != null) {
            while(true) {
                serverPort = portRange.getFreePort(serverPort);
                try {
                    listenSocket = new ServerSocket( serverPort );
                    portRange.setUsed( serverPort );
                    break;
                } catch(IOException e) {
                }
                ++serverPort;
            }
        } else {
            listenSocket = new ServerSocket( serverPort );
        }
        this.serverPort = listenSocket.getLocalPort();
        //} catch(Exception ex) {
        //    throw new ServerException("can't start web services server", ex);
        //}
        //        synchronized(getClass()) {
        //            if(!initialized) {
        //                init();
        //            }
        //        }
    }
    
    //    private static void init() {
    //        initializePortRange();
    //        initialized = true;
    //    }
    
    public static SoaprmiServerSocketFactory newInstance(int serverPort) throws IOException {
        return new PlainServerSocketFactory(serverPort);
    }
    
    public int getServerPort() {
        return serverPort;
    }
    
    public static synchronized void initializePortRange()
    {
        String portRangeProp = null;
        try {
            portRangeProp = (String) AccessController.doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            return System.getProperty(XSOAP_PORT_RANGE_PROPERTY);
                        }
                    });
        } catch(AccessControlException ace) {
            logger.severe("could not read system property "+XSOAP_PORT_RANGE_PROPERTY, ace);
        }
        
        if(portRangeProp != null) {
            try {
                portRange = new TcpPortRange(portRangeProp);
                logger.config(XSOAP_PORT_RANGE_PROPERTY+"="+portRangeProp);
            } catch(Exception e) {
                logger.severe("user specified -D"+XSOAP_PORT_RANGE_PROPERTY+" is not valid", e);
            }
        } else {
            logger.config("no "+XSOAP_PORT_RANGE_PROPERTY+" property");
        }
    }
    
    private static synchronized InetAddress getMyIp() throws java.net.UnknownHostException
    {
        if(myIp != null) {
            return myIp;
        }
        myIp = InetAddress.getLocalHost();
        //String overrideIp = System.getProperty(XSOAP_HOST_IP_PROPERTY);
        
        String overrideIp = null;
        try {
            overrideIp = (String) AccessController.doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            return System.getProperty(XSOAP_HOST_IP_PROPERTY);
                        }
                    });
        } catch(AccessControlException ace) {
            
            logger.severe("could not read system property "+XSOAP_HOST_IP_PROPERTY , ace);
            
        }
        
        
        if(overrideIp != null) {
            // parse into 4 bytes
            try {
                /*              byte b[] = new byte[4];
                 String s = overrideIp;
                 int i = s.indexOf('.');
                 String s0 = s.substring(0, i);
                 b[0] = (Byte) Integer.parseInt(s0);
                 int j = s.indexOf('.', i);
                 String s1 = s.substring(i, j);
                 b[1] = Byte.parseByte(s1);
                 i = j;
                 j = s.indexOf('.', i);
                 String s2 = s.substring(i, j);
                 b[2] = Byte.parseByte(s2);
                 String s3 = s.substring(j+1);
                 b[3] = Byte.parseByte(s3);
                 //myIp =  InetAddress.getByAddress(b); //only JDK 1.4
                 */
                myIp = InetAddress.getByName(overrideIp);
                logger.config(XSOAP_HOST_IP_PROPERTY+"="+myIp);
            } catch(Exception e) {
                logger.severe("user specified -D"+XSOAP_HOST_IP_PROPERTY+" is not IP address", e);
            }
        } else {
            logger.config("no "+XSOAP_HOST_IP_PROPERTY+" property");
        }
        return myIp;
    }
    
    
    public String getServerLocation() throws ServerException {
        try {
            //InetAddress addr = InetAddress.getLocalHost();
            //InetAddress addr = listenSocket.getInetAddress(); //wont work returns "0.0.0.0"
            InetAddress addr = getMyIp();
            return "http://" + addr.getHostAddress() + ':' + getServerPort();
        } catch(Exception ex) {
            throw new ServerException(
                "cant determine internet address of HTTP SOAP embedded web server", ex);
        }
    }
    
    public Socket accept() throws IOException {
        return accept(null);
    }
    
    public Socket accept(Map connectionProps) throws IOException {
        return listenSocket.accept();
    }
    
    public void shutdown() throws IOException {
        listenSocket.close();
        listenSocket = null;
    }
    
    
    //
    //    public String getLocation() throws ServerException {
    //      return socketFactory.getLocation();
    //        if(listenSocket == null)
    //            throw new ServerException("can't get location - server was not started");
    //        //return "http://"+listenSocket.getInetAddress().getHostName()
    //        //                                     + ":" + listenSocket.getLocalPort();
    //        try {
    //            InetAddress addr = InetAddress.getLocalHost();
    //            return "http://" + addr.getHostAddress() + ':' + getServerPort();
    //        } catch(Exception ex) {
    //            throw new ServerException(
    //                "cant determine internet address of current host", ex);
    //        }
    //    }
    
    /**
     * Accept new socket that can be used to read SOAP/HTTP request.
     */
    
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


