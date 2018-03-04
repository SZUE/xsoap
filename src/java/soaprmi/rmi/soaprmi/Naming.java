/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Naming.java,v 1.9 2004/05/06 18:18:47 aslom Exp $
 */

package soaprmi;

import java.net.MalformedURLException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import soaprmi.registry.LocateRegistry;
import soaprmi.registry.Registry;
import soaprmi.NotBoundException;
import soaprmi.AccessException;
import soaprmi.AlreadyBoundException;
import soaprmi.RemoteException;


/**
 * API to access naming service for SoapRMI services.
 *
 * @version $Revision: 1.9 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class Naming {
    
    protected Naming() {
    }
    
    public static Remote lookup(String urlToLookup)
        throws NotBoundException, AccessException, MalformedURLException,
        java.net.UnknownHostException, RemoteException
    {
        ParsedName pn = cleanURL(urlToLookup);
        Registry reg = LocateRegistry.getRegistry(pn.host, pn.port, pn.secure);
        if(reg == null) {
            throw new ConnectException(
                "Naming.lookup(): cannot get reference to registry host "+pn.host+" for "
                    + urlToLookup);
        } else {
            // Let the registry stub do the real work.
            Remote ref = reg.lookup(pn.objName);
            return ref;
        }
    }
    
    public static void bind(String urlToBindWith, Remote remoteObject )
        throws AlreadyBoundException,
        MalformedURLException, java.net.UnknownHostException,
        AccessException, RemoteException
    {
        ParsedName pn = cleanURL(urlToBindWith);
        Registry reg = LocateRegistry.getRegistry(pn.host, pn.port, pn.secure);
        if(reg == null) {
            throw new ConnectException
                ("Naming.bind(): Connection refused to host "+pn.host+" for " + urlToBindWith);
        } else {
            reg.bind(pn.objName, remoteObject);
        }
    }
    
    public static void unbind(String urlToUnbind)
        throws NotBoundException, MalformedURLException,
        java.net.UnknownHostException, RemoteException
    {
        ParsedName pn = cleanURL(urlToUnbind);
        Registry reg = LocateRegistry.getRegistry(pn.host, pn.port, pn.secure);
        if(reg == null) {
            throw new ConnectException
                ("Naming.unbind(): Connection refused to host " + pn.host+" for "+urlToUnbind);
        } else {
            reg.unbind(pn.objName);
        }
    }
    
    public static void rebind(String urlToBindWith, Remote remoteObject)
        throws MalformedURLException, java.net.UnknownHostException,
        RemoteException
    {
//      if(remoteObject == null) {
//          throw new RuntimeException("object to bind can not be null");
//      }
        ParsedName pn = cleanURL(urlToBindWith);
        Registry reg = LocateRegistry.getRegistry(pn.host, pn.port, pn.secure);
        if(reg == null) {
            throw new ConnectException
                ("Naming.rebind(): Connection refused to host " + pn.host+" for "+urlToBindWith);
        } else {
            reg.rebind(pn.objName, remoteObject);
        }
    }
    
    public static String[] list(String url)
        throws MalformedURLException, UnknownHostException,
        RemoteException
    {
        ParsedName pn = cleanURL(url);
        Registry reg = LocateRegistry.getRegistry(pn.host, pn.port, pn.secure);
        if(reg == null) {
            throw new ConnectException
                ("Naming.list(): Connection refused to host " + pn.host+" for "+url);
        } else {
            return reg.list();
        }
    }
    
    protected static ParsedName cleanURL(String name)
        throws MalformedURLException, UnknownHostException
    {
        if(name == null) {
            throw new MalformedURLException("URL used for Naming can not be null");
        }
        ParsedName pn = new ParsedName();
        // remove the approved protocol
        if (name.startsWith("rmi:")) {
            name = name.substring("rmi:".length());
        }
        if (name.startsWith("soaprmi:")) {
            name = name.substring("soaprmi:".length());
        }
        if (name.startsWith("rmis:")) {
            name = name.substring("rmis:".length());
            pn.secure = true;
        }
        if (name.startsWith("soaprmi:")) {
            name = name.substring("soaprmi:".length());
            pn.secure = true;
        }
        
        if( name.startsWith("//")) {
            name = name.substring("//".length());
            
            int slash = name.indexOf('/');
            if(slash == -1) {
                throw new MalformedURLException("URL used for Naming must have object name after slash");
            }
            pn.objName = name.substring(slash + 1);
            name = name.substring(0, slash);
            int colon = name.indexOf(':');
            if(colon == -1) {
                pn.host = name;
                pn.port = -1;
            } else {
                pn.host = name.substring(0, colon);
                String s = name.substring(colon + 1);
                try {
                    pn.port = Integer.parseInt(s);
                } catch(NumberFormatException ex) {
                    throw new MalformedURLException("port after colon in "+name+" name must be integer");
                }
            }
        } else {
            //throw new MalformedURLException("URL used for Naming must have //host...");
            try {
                pn.host = InetAddress.getLocalHost().getHostName();
            } catch(java.net.UnknownHostException e) {
                throw new UnknownHostException("could not determine host for Naming "+name);
            }
            pn.port = -1;
            if(name.startsWith("/")) {
                name = name.substring(1);
            }
            pn.objName = name;
        }
        
        
        return pn;
    }
    
    //    private static String getName(URL objurl) {
    //        String name = objurl.getPath();
    //        if(name.length() > 0) //get rid of initial slash
    //      name = name.substring(1);
    //        return name;
    //    }
    
    protected static class ParsedName {
        public boolean secure;
        public String host;
        public int port;
        public String objName;
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


