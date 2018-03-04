/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HttpSocketSoapInvoker.java,v 1.11 2003/04/06 00:04:20 aslom Exp $
 */

package soaprmi.soaprpc;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.port.Endpoint;
import soaprmi.port.Port;
import soaprmi.server.SoaprmiClientSocketFactory;
import soaprmi.util.logging.Logger;

/**
 * Remote reference to SOAP service described in port.
 *
 * @version $Revision: 1.11 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class HttpSocketSoapInvoker extends SoapInvokerImpl {
    public final static String XSOAP_RPC_INVOKE_TIMEOUT_MS_PROPERTY
        = "xsoap.rpc.invoke.timeout.ms";

    private static Logger logger = Logger.getLogger();
    private static SoapInvoker instance = new HttpSocketSoapInvoker();
    //private int defaultTimeout = 2 * 60 * 1000; // 2 minutes
    private static int defaultTimeout = 4 * 60 * 1000; // 4 minutes

    static {
        //String overrideTimeout = System.getProperty(XSOAP_RPC_INVOKE_TIMEOUT_MS_PROPERTY);

        String overrideTimeout = null;
        try {
            overrideTimeout = (String) AccessController.doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            return System.getProperty(XSOAP_RPC_INVOKE_TIMEOUT_MS_PROPERTY);
                        }
                    });
        } catch(AccessControlException ace) {

            logger.severe(
                "could not read system property "+XSOAP_RPC_INVOKE_TIMEOUT_MS_PROPERTY , ace);

        }

        if(overrideTimeout != null) {
            logger.config(XSOAP_RPC_INVOKE_TIMEOUT_MS_PROPERTY+"="+overrideTimeout);
            try {
                //defaultTimeout = Integer.parseInt(overrideTimeout);
                setDefaultTimeout(Integer.parseInt(overrideTimeout));
            } catch(Exception e) {
                logger.severe("user specified -D"+XSOAP_RPC_INVOKE_TIMEOUT_MS_PROPERTY
                                  +" is not number", e);
            }
        } else {
            logger.config("property "+XSOAP_RPC_INVOKE_TIMEOUT_MS_PROPERTY
                              +" is not specified");
        }
    }

    private SoaprmiClientSocketFactory connectionFactory =
        PlainClientSocketFactory.getDefault();


    protected HttpSocketSoapInvoker() {
    }

    protected HttpSocketSoapInvoker(SoaprmiClientSocketFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public static SoapInvoker getDefault()
    {
        return instance;
    }

    public static SoapInvoker newInstance(SoaprmiClientSocketFactory connectionFactory)
    {
        return new HttpSocketSoapInvoker(connectionFactory);
    }

    public SoapInvocationHandler createSoapDynamicStub(
        Port port,
        Endpoint epoint,
        XmlJavaMapping mapping,
        Class[] interfaces
    )
    {
        HttpSocketSoapInvocationHandler transportInvoker =
            new HttpSocketSoapInvocationHandler(
            connectionFactory, port, epoint, mapping, interfaces
        );
        transportInvoker.setTimeout(getDefaultTimeout());
        return transportInvoker;
    }

    public static int getDefaultTimeout() { return defaultTimeout; }
    public static void setDefaultTimeout(int value) {
        logger.config("setting default timeout to "+value+" in ms");
        defaultTimeout = value;
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


