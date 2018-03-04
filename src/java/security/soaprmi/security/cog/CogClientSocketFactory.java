/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: CogClientSocketFactory.java,v 1.10 2003/04/06 00:04:08 aslom Exp $
 */

package soaprmi.security.cog;

import java.io.IOException;
import java.net.Socket;
import org.globus.security.GlobusProxy;
import org.globus.security.GlobusProxyException;
import org.globus.security.SSLClientConnection;
import soaprmi.RemoteException;
import soaprmi.security.SoaprmiSecurityContext;
import soaprmi.server.ConnectionContext;
import soaprmi.server.SoaprmiClientSocketFactory;
import soaprmi.util.logging.Logger;


/**
 *
 *
 * @version $Revision: 1.10 $ * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class CogClientSocketFactory implements SoaprmiClientSocketFactory {
    protected static Logger logger = Logger.getLogger();
    protected boolean delegationEnabled;
    protected boolean delegation;
    protected boolean limitedDelegation;

    // package level access
    CogClientSocketFactory(boolean delegationEnabled,
                           boolean doDelegation,
                           boolean doLimitedDelegation)
    {
        this.delegationEnabled = delegationEnabled;
        this.delegation = doDelegation;
        this.limitedDelegation = doLimitedDelegation;
    }



    /**
     *
     */
    public Socket connect(String host, int port) throws IOException, RemoteException {
        SSLClientConnection sc;
        logger.fine("trying connect to host="+host+" port="+port
                        +" delegationEnabled="+delegationEnabled+" delegation="+delegation
                        +" limitedDelegation="+limitedDelegation);
        try {
            // try to use proxy that is in current thread connection context
            GlobusProxy proxy = CogSoaprmiUtil.getOutgoingGlobusProxyFromConnectionContext();

            if(proxy != null) {
                logger.fine("using per Globus proxy from thread connection context "+proxy);
                sc = new SSLClientConnection(proxy);
            } else {
                sc = new SSLClientConnection();  //use *global* default proxy
            }
        } catch(GlobusProxyException ex) {
            throw new RemoteException("could not connect to "+host+":"+port, ex);
        }
        //sc.enableEncryption(true);
        //logger.fine("connecting using CoG to "+host+":"+port+" ...");
        Socket s = sc.connect(host, port);

        sc.enableEncryption(true);
        if(delegationEnabled) {
            logger.fine("Going to do delegation="+delegation+" limitedDelegation="+limitedDelegation);
            sc.doDelegation(delegation,limitedDelegation);
        }
        return s;
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



