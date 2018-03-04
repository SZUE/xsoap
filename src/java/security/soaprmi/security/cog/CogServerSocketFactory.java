/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: CogServerSocketFactory.java,v 1.14 2003/04/06 00:04:08 aslom Exp $
 */

package soaprmi.security.cog;

import iaik.security.ssl.SSLSocket;
import java.io.IOException;
import java.lang.Exception;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import org.globus.net.GSIServerSocketFactory;
import org.globus.security.GlobusProxy;
import org.globus.security.GlobusProxyException;
import org.globus.security.SSLClientConnection;
import org.globus.security.auth.NoAuthorization;
import org.globus.security.gsi.GSIBaseSocket;
import org.globus.security.gsi.IaikGSIBaseSocket;
import soaprmi.ServerException;
import soaprmi.security.SoaprmiSecurityContext;
import soaprmi.server.SoaprmiServerSocketFactory;
import soaprmi.util.logging.Logger;

/**
 * This socket factory uses CoG to produce secure server sockets.
 * We are similat CoG BaseServer class though we only need to access
 * handleDelegation and not need to run whole server,
 * moreover even just extending BaseServer to get access to protected
 * handleDelegation is not work too smooth as crucial delegation fields are private...
 *
 * @version $Revision: 1.14 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

//public class CogServerSocketFactory extends BaseServer implements SoaprmiServerSocketFactory {
public class CogServerSocketFactory implements SoaprmiServerSocketFactory {
    private static Logger logger = Logger.getLogger();
    private int port;
    private boolean delegationEnabled;
    private ServerSocket _server;
    private GlobusProxy credentials;

    // package level access
    CogServerSocketFactory(int port,
                           boolean delegationEnabled)
        throws IOException, GlobusProxyException
    {
        //super(true, port);
        this.port = port;
        this.delegationEnabled = delegationEnabled;

        credentials = GlobusProxy.getDefaultUserProxy();
        _server = GSIServerSocketFactory.getDefault().createServerSocket(port,credentials);
    }

    /**
     *
     */
    public Socket accept() throws IOException
    {
        return accept(null);
    }

    public Socket accept(Map conenctionProps) throws IOException
    {
        Socket socket = null;

        try {
            // based on CoG BaseServer
            //try {
            socket = _server.accept();
            //socket.setSoTimeout(SO_TIMEOUT);
            //socket.getSoTimeout();
            if (org.globus.util.debug.Debug.debugLevel >= 4) {
                SSLClientConnection.enableDebug(socket);
            }
            //} catch(IOException e) {
            //      logger.info("Server died: " + e.getMessage(), e);
            //    break;
            //}


            GSIBaseSocket wrappedSocket = new IaikGSIBaseSocket( (SSLSocket)socket ) {
                // adding missig implementation of those methods in GSIBaseSocket
                public int getSoTimeout() throws SocketException {
                    return sslSocket.getSoTimeout();
                }
                public void setSoTimeout(int timeout) throws SocketException {
                    sslSocket.setSoTimeout(timeout);
                }
                public String toString() { return "GSI socket="+this.socket.toString(); }
            };
            wrappedSocket.setCredentials(credentials);
            wrappedSocket.setTrustedCertificates(credentials.getTrustedCertificates());
            //wrappedSocket.setAuthorization(new SelfAuthorization(credentials));
            wrappedSocket.setAuthorization(NoAuthorization.getInstance());
            //s = wrappedSocket;

            // if(XSoapServerConnectionInterceptorGlobal.socketAccepted(s) == false) close();
            if(delegationEnabled) {
                logger.finer("trying to do delegation for "+wrappedSocket);
                //GlobusProxy proxy = handleDelegation(s);
                //GSIBaseSocket gsiBaseSocket = (GSIBaseSocket) s;
                GlobusProxy proxy = wrappedSocket.handleDelegation();
                logger.fine("got delegated proxy "+proxy+" on socket "+wrappedSocket);
                if(conenctionProps != null) {
                    conenctionProps.put(SoaprmiSecurityContext.DELEGATED_GLOBUS_PROXY_PROP, proxy);
                } else {
                    logger.warning("there was no connectionPropos to store delegated proxy "+proxy
                                  +" for socket "+wrappedSocket);
                }
            } else {
                logger.fine("no delegation enabled for "+wrappedSocket);
            }
            return wrappedSocket;
        } catch(IOException ioe) {
            // make sure to notify client by closing connection
            try { if(socket != null) socket.close(); } catch(Exception ex) {}
            throw ioe;
        }
    }

    //private GlobusProxy handleDelegation(Socket s)

    public int getServerPort()
    {
        return _server.getLocalPort();

    }

    public String getServerLocation() throws ServerException {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return "https://" + addr.getHostAddress() + ':' + getServerPort();
        } catch(Exception ex) {
            throw new ServerException(
                "cant determine internet address of HTTP SOAP embedded web server", ex);
        }
    }

    public void shutdown() throws IOException {
        //super.shutdown();
        _server.close();
    }
    //
    //    // disable CoG BaseServer behaving as server ...
    //    protected void initialize() {}
    //    protected void start() {}
    //    protected void handleConnection(Socket socket){}



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


