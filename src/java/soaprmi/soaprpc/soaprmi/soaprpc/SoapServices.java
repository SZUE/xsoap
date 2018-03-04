/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SoapServices.java,v 1.13 2003/04/06 00:04:21 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.IOException;
import soaprmi.RemoteException;
import soaprmi.ServerException;
import soaprmi.port.Endpoint;
import soaprmi.port.Port;
import soaprmi.server.RemoteRef;
import soaprmi.server.Services;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

/**
 * Define entry points to SOAP web services.
 *
 * @version $Revision: 1.13 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class SoapServices extends Services {
    private static Logger logger = Logger.getLogger();
    private static SoapServices instance = new SoapServices();
    // by default creation of embedded server is enabled (use -1 to disable)
    protected int port = 0;

    //public final static String SOAPRMI_VERSION = "1.2";
    public final static String USER_AGENT = "SoapRMI/"+soaprmi.Version.getImplementationVersion();
//    public final static boolean TRACE_DISPATCH = true && Log.ON;
//    public final static boolean TRACE_INVOKE = true && Log.ON;
//    public final static boolean TRACE_SENDING = true && Log.ON;
//    public final static boolean TRACE_RECEIVING = true && Log.ON;

    final static Logger TRACE_EXECUTION = Logger.getLogger("soaprmi.trace.execution");
    final static Logger TRACE_DISPATCH_IN = Logger.getLogger("soaprmi.trace.dispatch.in");
    final static Logger TRACE_DISPATCH_OUT = Logger.getLogger("soaprmi.trace.dispatch.out");
    final static Logger TRACE_INVOKE_IN = Logger.getLogger("soaprmi.trace.invoke.in");
    final static Logger TRACE_INVOKE_OUT = Logger.getLogger("soaprmi.trace.invoke.out");

    protected SoapServices()
    {
        setInvoker(HttpSocketSoapInvoker.getDefault());
    }

    protected SoapServices(int port)
    {
        this();
        this.port = port;
    }


    public static SoapServices getDefault() { return instance; }

    /**
     * Create new instance of service that will not have
     * default embedded server and dispatcher.
     */
    public static SoapServices newInstance() { return newInstance(-1); }

    /**
     * Create new istance of service that by default if no other dispatcher set
     * will start embedded server on port and use it as default dispatcher
     * uless port is -1. Using 0 as port will allow automaic selection of
     * available free TCP port.
     */
    public static SoapServices newInstance(int port) {
        return new SoapServices(port);
    }

    public RemoteRef createStartpoint(Port port)
        throws RemoteException
    {
        if(port == null) {
            throw new RemoteException("can not create startpoint to null port");
        }
        Endpoint epoint = port.getEndpoint();
        if(epoint == null) {
            throw new RemoteException("port can not have null endpoint");
        }
        String location = epoint.getLocation();
        if(port == null) {
            throw new RemoteException("port endpoint can not be null");
        }
        if(location.startsWith("http://")) {
            return super.createStartpoint(port);
        } else if(location.startsWith("https://")) {
            try {
                return SecureSoapServices.getDefault().createStartpoint(port);
            } catch(Exception ex) {
                throw new RemoteException("could not create secure startpoint to "+location, ex);
            }
        } else {
            throw new RemoteException(
                "only http:// or https:// are supported port endpoint locations and not "+location);
        }
    }

    public void addDefaultDispatcher() throws IOException, ServerException {
        synchronized(this) {
            if(dispatchersSize() > 0) return;
            if(port < 0) {
                throw new ServerException("default embedded server disabled "+
                                              "- no dispatcher available for "+getClass());
            }
            SoapServerFactory factory = HttpSocketSoapServerFactory.getDefault();
            SoapServer server = factory.newSoapServer(port); //new SoapEmbeddedServer();
            //if(Log.ON) l.log(Level.FINE, "starting embedded web server");
            //server.setPort(0);
            server.startServer();
            if(Log.ON) logger.fine(
                    "default embedded web server started on port "+server.getServerPort());

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

