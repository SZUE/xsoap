/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SecurityContextBasedSoapServices.java,v 1.8 2003/04/06 00:04:08 aslom Exp $
 */

package soaprmi.security;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.*;

import soaprmi.RemoteException;
import soaprmi.ServerException;
import soaprmi.soaprpc.HttpSocketSoapInvoker;
import soaprmi.soaprpc.HttpSocketSoapServer;
import soaprmi.soaprpc.SecureSoapServices;
import soaprmi.soaprpc.SoapDispatcher;
import soaprmi.soaprpc.SoapServer;
import soaprmi.server.SoaprmiServerSocketFactory;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

/**
 * Define entry points to decure SOAP web services.
 *
 * @version $Revision: 1.8 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class SecurityContextBasedSoapServices extends SecureSoapServices {
    private static Logger logger = Logger.getLogger();
    protected boolean delegationEnabled;
    protected boolean delegation;
    protected boolean limitedDelegation;

    protected int port = -1; // by default starting embedded server is disabled
    protected SoaprmiSecurityContext secCtx;

    protected SecurityContextBasedSoapServices(SoaprmiSecurityContext secCtx)
        throws RemoteException
    {
        this.secCtx = secCtx;
        setInvoker(HttpSocketSoapInvoker.newInstance(secCtx.newClientSocketFactory()));
        if(Log.ON) logger.fine("created DEFAULT security context "+secCtx.getClass());

    }

    protected SecurityContextBasedSoapServices(SoaprmiSecurityContext secCtx, int port)
        throws RemoteException
    {
        this(secCtx);
        this.port = port;
        if(Log.ON) logger.fine("created security contenxt for port "+port);
    }

    public static SecureSoapServices newInstance(SoaprmiSecurityContext secCtx)
        throws RemoteException
    {
        return new SecurityContextBasedSoapServices(secCtx);
    }

    public static SecureSoapServices newInstance(SoaprmiSecurityContext secCtx,
                                                 int port)
        throws RemoteException
    {
        return new SecurityContextBasedSoapServices(secCtx, port);
    }

    public boolean supportsDelegation() {
        return secCtx.supportsDelegation();
    }


    public void setDelegation(boolean delegationEnabled,
                              boolean doDelegation,
                              boolean doLimitedDelegation)
        throws RemoteException
    {
        this.delegationEnabled = delegationEnabled;
        this.delegation = doDelegation;
        this.limitedDelegation = doLimitedDelegation;
        secCtx.setDelegation(delegationEnabled, doDelegation, doLimitedDelegation);
    }

    public void addDefaultDispatcher() throws IOException, RemoteException {
        synchronized(this) {
            if(dispatchersSize() > 0) return;
            if(port < 0) {
                throw new ServerException("default embedded server disabled "+
                                              "- no dispatcher available for "+getClass());
            }
            //SoapServerFactory factory = HttpSocketSoapServerFactory.getDefault();

            //SoapServer server = factory.newSoapServer(port); //new SoapEmbeddedServer();

            SoaprmiServerSocketFactory socketFact =   secCtx.newServerSocketFactory(port);
            SoapServer server = HttpSocketSoapServer.newInstance(socketFact);
            int serverPort = socketFact.getServerPort();

            if(Log.ON) logger.fine("starting embedded web server on provided port "+port
                                       +" server socket factory port "+serverPort);
            //server.setPort(0);
            server.startServer();
            if(Log.ON) logger.fine(
                    "default secure embedded web server "+getClass()+
                        " started on port "+server.getServerPort());

            SoapDispatcher dsptr = server.getDispatcher();

            addDispatcher(dsptr);

            //started = true;
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


