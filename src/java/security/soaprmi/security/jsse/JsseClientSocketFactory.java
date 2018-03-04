/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: JsseClientSocketFactory.java,v 1.12 2003/04/06 00:04:09 aslom Exp $
 */

package soaprmi.security.jsse;

import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.globus.security.GlobusProxy;
import org.globus.security.GlobusProxyException;
import org.globus.security.TrustedCertificates;
import soaprmi.RemoteException;
import soaprmi.ServerException;
import soaprmi.security.cog.CogSoaprmiUtil;
import soaprmi.server.SoaprmiClientSocketFactory;
import soaprmi.util.logging.Logger;


/**
 * Very simple wrapper to access standard JSSE client sockets.
 *
 * @version $Revision: 1.12 $
 * @author Aleksander A. Slominski
 * @author Lavanya Ramakrishnan [mailto: laramakr@extreme.indiana.edu]
 */


public class JsseClientSocketFactory implements SoaprmiClientSocketFactory {
    protected static Logger logger = Logger.getLogger();
    protected static SSLSocketFactory sslFact;
    protected static KeyManagerFactory kmf;
    protected static TrustManagerFactory tmf;
    protected static KeyStore ks;
    protected static KeyStore trustks;

    // package level access
    JsseClientSocketFactory()
        throws GeneralSecurityException, IOException, GlobusProxyException, RemoteException
    {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        kmf= KeyManagerFactory.getInstance("SunX509");
        tmf =  TrustManagerFactory.getInstance("SunX509");
        ks = KeyStore.getInstance("JKS","SUN");
        trustks = KeyStore.getInstance("JKS","SUN");
        setupClientSecurityContext();
    }


    private void setupClientSecurityContext()
        throws GeneralSecurityException, IOException, GlobusProxyException, ServerException
    {
        SSLContext sslCtx;
        char[] passphrase = "xsoap.jsse".toCharArray();
        sslCtx = SSLContext.getInstance("SSL");


        GlobusProxy proxy = CogSoaprmiUtil.getOutgoingGlobusProxyFromConnectionContext();

        if(proxy != null) {
            logger.fine("using per Globus proxy from thread conenction context "+proxy);
        } else {

            //ReadProperties paths = new ReadProperties();
            // the default (if it exists)

            //String proxyPath = System.getProperty( "X509_USER_PROXY" );
            //if ( proxyPath == null )
                //proxyPath = paths.getProxyPath();
            ////GlobusProxy gP = GlobusProxy.load( proxy_path );
            //proxy = GlobusProxy.load( proxyPath );
            String proxyPath = JsseServerSocketFactory.guessProxyPath();
            proxy = GlobusProxy.load( proxyPath );
            logger.fine("loaded global default Globus proxy "+proxy+" from "+proxyPath);
        }

        java.security.cert.X509Certificate [] certs = proxy.getCertificateChain();

        ks.load(null,null);

        // The Keystore needs the public- private key pair stored. So we get the Private Key
        //  and load an entry into the keystore for the chain certificates */
        PrivateKey key = proxy.getPrivateKey();

        ks.setKeyEntry("test", key, passphrase,certs);
        kmf.init(ks, passphrase);

        /* The Trust Manager is loaded to decide what certificates can be trusted from the server side */
        trustks.load(null,null);

        //        String[] caCerts = paths.getCaCertPath();
        //        for (int i = 0; i < caCerts.length; i++)
        //        {
        //            String caCert = caCerts[i];
        //            java.security.cert.X509Certificate trustcert = CertUtil.loadCert(caCert);
        //            String alias = "globus"+i;
        //            trustks.setCertificateEntry(alias,trustcert);
        //            l.finest("setting JSSE client trusted cert named "+alias+" to "+caCert);
        //        }

        java.security.cert.X509Certificate [] trustCerts
            = TrustedCertificates.getDefaultTrustedCertificates().getCertificates();
        for (int i = 0; i < trustCerts.length; i++)
        {
            java.security.cert.X509Certificate trustcert = trustCerts[i];
            String alias = "globus"+i;
            trustks.setCertificateEntry(alias,trustcert);
            logger.finest("setting JSSE client trusted cert named "+alias+" to "+trustcert);
        }

        tmf.init(trustks);
        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        sslFact = sslCtx.getSocketFactory();
    }


    /**
     *
     */
    public Socket connect(String host, int port) throws IOException {

        //try {
        //  SSLSocketFactory sslFact = (SSLSocketFactory)SSLSocketFactory.getDefault();
        logger.fine("Connecting JSSE/CoG to "+host+":"+port+" ...");
        SSLSocket s = (SSLSocket) sslFact.createSocket(host, port);
        // TODO: theoretically globus delegation could be done here
        //if(delegationEnabled) {
        //    System.out.println("Going to do delegation");
        //    sc.doDelegation(delegation,limitedDelegation);
        //}
        return s;
        //}
        //catch (Exception e){
        //    throw new ServerException("Problem with initialising Security Context", ex);
        //}

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


