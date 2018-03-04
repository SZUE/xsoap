/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: JsseServerSocketFactory.java,v 1.13 2003/04/06 00:04:09 aslom Exp $
 */

package soaprmi.security.jsse;

import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Map;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import org.globus.security.GlobusProxy;
import org.globus.security.GlobusProxyException;
import org.globus.security.TrustedCertificates;
import soaprmi.ServerException;
import soaprmi.server.SoaprmiServerSocketFactory;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;


//TODO: some code to allow overrding default keystore and trust
/**
 * Very simple wrapper around JSSE server socket factory.
 *
 * @version $Revision: 1.13 $
 * @author Aleksander A. Slominski
 * @author Lavanya Ramakrishnan [mailto: laramakr@extreme.indiana.edu]
 */

public class JsseServerSocketFactory implements SoaprmiServerSocketFactory {
    protected static Logger logger = Logger.getLogger();
    protected int port;
    protected SSLServerSocket serverSocket;
    protected static KeyManagerFactory kmf;
    protected static TrustManagerFactory tmf;
    protected static KeyStore ks;
    protected static KeyStore trustks;

    JsseServerSocketFactory(int port)
        throws IOException, GlobusProxyException, GeneralSecurityException
    {
        this.port = port;
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        kmf= KeyManagerFactory.getInstance("SunX509");
        ks = KeyStore.getInstance("JKS","SUN");
        trustks = KeyStore.getInstance("JKS","SUN");
        tmf =  TrustManagerFactory.getInstance("SunX509");
        setupServerSecurityContext();
    }

    public static String guessProxyPath() throws IOException {
        ReadProperties paths = new ReadProperties();
        // check to see if there is a delegated proxy; if not we'll have to use
        // the default (if it exists)
        //String proxy_path = System.getProperty( "X509_USER_PROXY" );
        final String X509_USER_PROXY = "X509_USER_PROXY";
        String proxyPath = null;
        try {
            proxyPath = (String) AccessController.doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            return System.getProperty(X509_USER_PROXY);
                        }
                    });
        } catch(AccessControlException ace) {

            logger.severe( "could not read system property "+X509_USER_PROXY , ace);
        }

        if ( proxyPath == null ) {
            proxyPath = paths.getProxyPath();
        }
        return proxyPath;
    }

    private void setupServerSecurityContext()
        throws GeneralSecurityException, IOException, GlobusProxyException
    {
        SSLContext ctx = SSLContext.getInstance("SSL");
        char[] passPhrase = "xsoap.jsse".toCharArray();



        GlobusProxy gP = GlobusProxy.load( guessProxyPath() );

        java.security.cert.Certificate [] certs = gP.getCertificateChain();
        ks.load(null,null);

        PrivateKey key = gP.getPrivateKey();
        ks.setKeyEntry("test", key, passPhrase,certs);

        kmf.init(ks, passPhrase);

        /* The Trust Manager is loaded to decide what certificates can be trusted from the client side */
        //java.security.cert.X509Certificate [] trustCerts = new java.security.cert.X509Certificate[1];

        trustks.load(null,null);
        //trustCerts[0] = CertUtil.loadCert(paths.getCaCertPath());
        //trustks.setCertificateEntry("globus",trustCerts[0]);
        //        String[] caCerts = paths.getCaCertPath();
        //        for (int i = 0; i < caCerts.length; i++)
        //        {
        //            String caCert = caCerts[i];
        //            java.security.cert.X509Certificate trustcert = CertUtil.loadCert(caCert);
        //            String alias = "globus"+i;
        //            trustks.setCertificateEntry(alias,trustcert);
        //            if(Log.ON) l.finest("setting JSSE server trusted cert named "+alias+" to "+caCert);
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

        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        SSLServerSocketFactory sslSrvFact = ctx.getServerSocketFactory();

        if(Log.ON) logger.fine("creating JSSE server socket at "+port);

        serverSocket =(SSLServerSocket)sslSrvFact.createServerSocket(port);
        serverSocket.setNeedClientAuth(true);

    }
    /**
     *
     */
    public Socket accept() throws IOException
    {
        return accept(null);
    }

    public Socket accept(Map connectionProps) throws IOException
    {

        SSLSocket s = (SSLSocket)serverSocket.accept();

        //TODO: theoretically we could globus delegation here...
        //if(delegationEnabled) {
        //    handleDelegation(s);
        //}
        return s;
    }

    public void shutdown() throws IOException
    {
        serverSocket.close();
    }

    public int getServerPort()
    {
        return serverSocket.getLocalPort();
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


