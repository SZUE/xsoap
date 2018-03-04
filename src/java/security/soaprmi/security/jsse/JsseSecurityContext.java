/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: JsseSecurityContext.java,v 1.8 2003/04/06 00:04:09 aslom Exp $
 */

package soaprmi.security.jsse;

import java.io.IOException;

import java.security.GeneralSecurityException;

import org.globus.security.GlobusProxyException;

import soaprmi.RemoteException;
import soaprmi.ServerException;

import soaprmi.server.SoaprmiClientSocketFactory;
import soaprmi.server.SoaprmiServerSocketFactory;
import soaprmi.security.SoaprmiSecurityContext;
import soaprmi.util.logging.Logger;

/**
 * Security context that is based on JSSE but uses CoG to get globus proxy.
 *
 * @version $Revision: 1.8 $
 * @author Aleksander A. Slominski [http://www.extreme.indiana.edu/~aslom/]
 */
public class JsseSecurityContext implements SoaprmiSecurityContext {
    protected static Logger l = Logger.getLogger();
    private static JsseSecurityContext instance = new JsseSecurityContext();

    protected JsseSecurityContext()
    {
    }

    /**
     *
     */
    public static JsseSecurityContext getDefault() {
        return instance;
    }

    public SoaprmiClientSocketFactory newClientSocketFactory()
        throws ServerException
    {
        try {
            return new JsseClientSocketFactory();
        } catch(RemoteException ex) {
            throw new ServerException("could not create CoG client socket factory: "+ex, ex);
        } catch(IOException ex) {
            throw new ServerException("could not create CoG client socket factory: "+ex, ex);
        } catch(GeneralSecurityException ex) {
            throw new ServerException("could not create CoG client socket factory: "+ex, ex);
        } catch(GlobusProxyException ex) {
            throw new ServerException("could not create CoG client socket factory: "+ex, ex);
        }
    }

    public SoaprmiServerSocketFactory newServerSocketFactory(int port)
        throws ServerException
    {
        try {
            return new JsseServerSocketFactory(port);
            //        } catch(GlobusProxyException ex) {
            //            throw new ServerException("could not create CoG server socket factory", ex);
        } catch(IOException ex) {
            throw new ServerException("could not create CoG server socket factory", ex);
        } catch(GeneralSecurityException ex) {
            throw new ServerException("could not create CoG server socket factory", ex);
        } catch(GlobusProxyException ex) {
            throw new ServerException("could not create CoG server socket factory", ex);
        }
    }

    public boolean supportsDelegation() {
        return false;
    }

    public void setDelegation(boolean delegationEnabled,
                              boolean doDelegation,
                              boolean doLimitedDelegation)
        throws RemoteException
    {
        throw new RemoteException("delegation is not supported in this JSSE security provider");
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


