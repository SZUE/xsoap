/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SecureSoapServices.java,v 1.12 2003/04/06 00:04:20 aslom Exp $
 */

package soaprmi.soaprpc;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.StringTokenizer;
import soaprmi.RemoteException;
import soaprmi.ServerException;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.port.Endpoint;
import soaprmi.port.Port;
import soaprmi.server.RemoteRef;
import soaprmi.server.Services;
import soaprmi.util.Util;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

/**
 * Define entry points to decure SOAP web services.
 *
 * @version $Revision: 1.12 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public abstract class SecureSoapServices extends Services
{
    private static Logger logger = Logger.getLogger();
    private static String defaultSecurityProviders = "cog" ;
    private static SecureSoapServices instance;

    public final static String SOAPRMI_SECURITY_PROVIDERS = "soaprmi.security.providers";

    static {
        try {
            //String providers = System.getProperty(SOAPRMI_SECURITY_PROVIDERS);

            String providers = null;
            try {
                providers = (String) AccessController.doPrivileged(new PrivilegedAction() {
                            public Object run() {
                                return System.getProperty(SOAPRMI_SECURITY_PROVIDERS);
                            }
                        });
            } catch(AccessControlException ace) {

                logger.severe("could not read system property "+SOAPRMI_SECURITY_PROVIDERS , ace);

            }

            if(providers != null) {
                defaultSecurityProviders = providers;
                if(Log.ON) logger.config("default security providers set to "+providers);
            }
        } catch(Exception ex) {
        }
    }

    protected SecureSoapServices()
    {
        // secure soap services has "superset" of SoapServices mappings
        XmlJavaMapping mapping = getMapping();
        mapping.connectTo(SoapServices.getDefault().getMapping());

        //        String providers = null;
        //        try {
        //            providers = System.getProperty(SOAPRMI_SECURITY_PROVIDERS);
        //            setDefault(providers);
        //        } catch(Exception ex) {
        //            if(Log.ON) l.config("could not find any security provider"
        //                                    +(providers != null ? " for "+providers : ""), ex);
        //        }
    }

    /**
     * Set default secure soap services to use.
     * @param securityProviders comma separated list of security providers to try.
     *          Security providers will be tested in list order and first that was
     *          successfully loaded will be used.
     *          To successfully load secure service of NAME following class must be on CLASSPATH:
     *          soaprmi.security.NAME.SecureSoapServeices and it must extend this
     *          SecureSoapServices, be non abstract (implement all abstract medod) and
     *          needs to have public static synchronized SecureSoapServices getDefault() method.
     *          If null passed than hardcoded default will be used (ie. CoG).
     */
    public static synchronized void setDefault(String securityProviders)
        throws RemoteException
    {
        String [] providers = null;
        if(securityProviders == null) {
            securityProviders = defaultSecurityProviders;
        }
        try {
            StringTokenizer stok = new StringTokenizer(securityProviders, ",:;");
            int count = stok.countTokens();
            providers = new String[count];
            for (int i = 0; i < providers.length; i++)
            {
                providers[i] = stok.nextToken();
            }
        } catch(Exception e) {
            throw new ServerException(
                "could not determie list of security providers from "+securityProviders);
        }
        Exception e = null;
        StringBuffer list = new StringBuffer();
        instance = null;
        for(int i = 0; instance == null && i < providers.length; ++i) {
            try {
                if(i > 0) list.append(",");
                list.append(providers[i]);
                instance = getDefault(providers[i]);
            } catch(Exception ex) {
                e = ex;
            }
        }
        if(instance == null) {
            throw new ServerException("could not set default security services to "
                                          +securityProviders+" tried: "+list, e);
        }
        defaultSecurityProviders = securityProviders;
    }

    public static synchronized SecureSoapServices getDefault()
        throws RemoteException
    {
        if(instance == null) {
            setDefault(null);
        }
        return instance;
    }

    public static synchronized SecureSoapServices newInstance(int port)
        throws RemoteException
    {
        if(instance == null) {
            setDefault(null);
        }
        //return instance.newInstance(port); //wont work as it is static method resolving...
        try {
            Class klass = instance.getClass();
            java.lang.reflect.Method  m = klass.getMethod(
                "newInstance", new Class[]{Integer.TYPE});
            SecureSoapServices secServ = (SecureSoapServices) m.invoke(
                null, new Object[]{new Integer(port)});
            return secServ;
        } catch(Exception ex) {
            throw new ServerException("could not get new instance of soap securitty services for '"
                                          +instance.getClass()+"' ", ex);
        }
        //      SecureSoapServices s3 = null;
        //      StringBuffer list = new StringBuffer();
        //      Exception e = null;
        //      for(int i = 0; s3 == null && i < defaultSecurityProviders.length; ++i) {
        //          try {
        //              if(i > 0) list.append(", ");
        //              list.append(defaultSecurityProviders[i]);
        //              s3 = newInstance(defaultSecurityProviders[i], port);
        //          } catch(Exception ex) {
        //              e = ex;
        //          }
        //      }
        //      if(s3 == null) {
        //          throw new ServerException("no security services can be created, tried: "+list, e);
        //      }
        //      return s3;
    }


    public static SecureSoapServices getDefault(String securityServicesName)
        throws RemoteException
    {
        if(securityServicesName == null) {
            return getDefault();
        }

        String klassName = "soaprmi.security."+securityServicesName+".SecureSoapServices";
        try {

            //ClassLoader cl = Thread.currentThread().getContextClassLoader();
            //Class klass = cl != null ? cl.loadClass(klassName) : Class.forName(klassName);
            Class klass = Util.loadClass(klassName);

            java.lang.reflect.Method  m = klass.getMethod("getDefault", null);
            SecureSoapServices secServ = (SecureSoapServices) m.invoke(null, null);
            return secServ;
        } catch(Exception ex) {
            throw new ServerException("could not get default soap security services for '"
                                          +securityServicesName+"' as "+klassName+" : "+ex, ex);
        }
    }

    public static SecureSoapServices newInstance(String securityServicesName, int port)
        throws RemoteException
    {
        if(securityServicesName == null) return newInstance(port);
        try {
            Class klass =
                Class.forName("soaprmi.security."+securityServicesName+".SecureSoapServices");
            java.lang.reflect.Method  m = klass.getMethod(
                "newInstance", new Class[]{Integer.TYPE});
            SecureSoapServices secServ = (SecureSoapServices) m.invoke(
                null, new Object[]{new Integer(port)});
            return secServ;
        } catch(Exception ex) {
            throw new ServerException("could not get new instance of soap securitty services for '"
                                          +securityServicesName+"' ", ex);
        }
    }

    public static SecureSoapServices newInstance(String securityServicesName)
        throws RemoteException
    {
        return newInstance(securityServicesName, 0);
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
        if(location.startsWith("https://")) {
            return super.createStartpoint(port);
        } else {
            throw new RemoteException(
                "only https:// is supported for secure port endpoint locations and not "+location);
        }
    }

    public abstract boolean supportsDelegation();

    public abstract void setDelegation(boolean delegationEnabled,
                                       boolean doDelegation,
                                       boolean doLimitedDelegation)
        throws RemoteException;

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

