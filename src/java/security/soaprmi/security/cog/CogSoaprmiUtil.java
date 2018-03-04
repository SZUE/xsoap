/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: CogSoaprmiUtil.java,v 1.5 2003/04/06 00:04:08 aslom Exp $
 */

package soaprmi.security.cog;

import java.util.HashMap;
import org.globus.security.GlobusProxy;
import soaprmi.ServerException;
import soaprmi.security.SoaprmiSecurityContext;
import soaprmi.server.ConnectionContext;

/**
 * Usesful utilites for CoG and Soaprmi ...
 *
 * @version $Revision: 1.5 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class CogSoaprmiUtil {

    /**
     * Extract incoming Globus proxy from current thread context.
     * Return null if no globus proxy is attached to current context.
     *
     * <p>This functiononly check incoming properties on connection context.
     */
    public static GlobusProxy getIncomingGlobusProxyFromConnectionContext() throws ServerException
    {
        ConnectionContext xsoapCtx=soaprmi.soaprpc.SoapServices.getDefault().getConnectionContext();
        GlobusProxy proxy = null;
        if(xsoapCtx.getIncomingProps() != null) {
            proxy = (GlobusProxy)
                xsoapCtx.getIncomingProps().get(SoaprmiSecurityContext.DELEGATED_GLOBUS_PROXY_PROP);
        }
        return proxy;
    }

    /**
     * Overide delegated proxy that was obtained from incoming connection,
     * this should be never used (but who knows ...)!
     * <p>NOTE: it returns overrided previous value.
     */
    public static GlobusProxy setIncomingGlobusProxyFromConnectionContext(GlobusProxy proxy)
        throws ServerException
    {
        ConnectionContext xsoapCtx=soaprmi.soaprpc.SoapServices.getDefault().getConnectionContext();
        if(xsoapCtx.getIncomingProps() == null) {
            xsoapCtx.setIncomingProps(new HashMap());
        }
        Object o = xsoapCtx.getIncomingProps().put(SoaprmiSecurityContext.DELEGATED_GLOBUS_PROXY_PROP, proxy);
        return (GlobusProxy) o;
    }

    /**
     * Extract GlobusProxy from current thread cotnext.
     * Return null if no globus proxy is attached to current context.
     *
     * <p>Essentially check to see if there is a proxy set in outgoing properties
     * of current connection context, if notne then try to see if there is
     * proxy in incoming properties (a delegated proxy from remote call).
     * If still none then return null.
     */
    public static GlobusProxy getOutgoingGlobusProxyFromConnectionContext() throws ServerException
    {
        ConnectionContext xsoapCtx=soaprmi.soaprpc.SoapServices.getDefault().getConnectionContext();
        GlobusProxy proxy = null;
        if(xsoapCtx.getOutgoingProps() != null) {
            proxy = (GlobusProxy)
                xsoapCtx.getOutgoingProps().get(SoaprmiSecurityContext.DELEGATED_GLOBUS_PROXY_PROP);
            //ctx.getOutgoingProps().put(SoaprmiSecurityContext.DELEGATED_GLOBUS_PROXY_PROP, proxy);
        }

        if(proxy == null && xsoapCtx.getIncomingProps() != null) {
            proxy = (GlobusProxy)
                xsoapCtx.getIncomingProps().get(SoaprmiSecurityContext.DELEGATED_GLOBUS_PROXY_PROP);
        }
        return proxy;
    }


    /**
     * Set globus proxy to use for delegation for outgoing connections.
     * <p>NOTE: it returns overrided previous value.
     */
    public static GlobusProxy setOutgoingGlobusProxyInConnectionContext(GlobusProxy proxy)
        throws ServerException
    {
        ConnectionContext xsoapCtx=soaprmi.soaprpc.SoapServices.getDefault().getConnectionContext();
        if(xsoapCtx.getIncomingProps() == null) {
            xsoapCtx.setIncomingProps(new HashMap());
        }
        Object o = xsoapCtx.getIncomingProps().put(SoaprmiSecurityContext.DELEGATED_GLOBUS_PROXY_PROP, proxy);
        return (GlobusProxy) o;
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


