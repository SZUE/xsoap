/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Util.java,v 1.6 2003/04/06 00:04:10 aslom Exp $
 */

package soaprmi.jndi;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Arrays;

import java.util.Hashtable;

import javax.naming.*;
import javax.naming.directory.*;

import soaprmi.Naming;
import soaprmi.Remote;
import soaprmi.RemoteException;

import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

/**
 * The bag for useful staff.
 *
 * @version $Revision: 1.6 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class Util {

  public static DirContext getInitialDirContext(String ldapUrl)
    throws NamingException
  {

    Hashtable env = new Hashtable(11);
    env.put(Context.OBJECT_FACTORIES,
      soaprmi.jndi.SoapRMIPortFactory.class.getName());
    if(ldapUrl != null && ldapUrl.startsWith("ldap://")) {
      env.put(Context.INITIAL_CONTEXT_FACTORY,
              "com.sun.jndi.ldap.LdapCtxFactory");
    }
    if(ldapUrl != null) {
      env.put(Context.PROVIDER_URL, ldapUrl);
    }

    DirContext ctx = new InitialDirContext(env);

    // see to learn mor ehow JNDI environment is resolved
    // http://java.sun.com/products/jndi/tutorial/beyond/env/context.html
    //System.out.println("JNDI env="+ctx.getEnvironment());

    return ctx;
  }


  public static String expandLocation(String loc, int defaultHttpPort)
  {
    //int pos = -1;
    String location = loc;
    if(loc.startsWith("//")) {
      location = "rmi:" + loc;
    }
    //if(loc.startsWith("http://") || loc.startsWith("https://")
    //|| loc.startsWith("ldap://")
    //|| loc.startsWith("rmi://") || loc.startsWith("soaprmi://")
    //) {
      // explicit full  URL - do not modify it
    //} else if((pos = arg.indexOf(':')) != -1) {
    //  url = arg; //Util.DEFAULT_LDAP_URL+","
    //} else {
      // check if location is a [machine:]number - make it into http port
    if(loc.indexOf("://") == -1) {
      try {
        int colonPos = loc.indexOf(':');
        String portPart = loc.substring(colonPos+1);
        int port = Integer.parseInt(portPart);
        String machine = (colonPos != -1) ?
          loc.substring(0, colonPos) : "localhost";
        location = "http://" + machine + ":"+port+"/";
      } catch(NumberFormatException ex) {
      }
    }

    if(loc.startsWith("http://") && defaultHttpPort != -1) {
      // set default port if it is not set
      String s = loc.substring("http://".length());
      int pos = s.indexOf('/');
      String addr = s;
      if(pos != -1) {
         addr = s.substring(0, pos);
      }
      int colonPos = addr.indexOf(':');
      if(colonPos == -1) {
         location = "http://"+addr+":"+defaultHttpPort
              +((pos != - 1) ? s.substring(pos) : "");
      }
    }
    return location;
  }

  public static String makeRMIRef(String loc, String ctx) {
    String rmi = null;
    if(loc.startsWith("rmi://")
      || loc.startsWith("soaprmi://")
      || loc.startsWith("//")
      ) {
      rmi = loc;
    }
    if(ctx != null && (
        ctx.startsWith("rmi://")
        || ctx.startsWith("soaprmi://")
        || ctx.startsWith("//"))
    ) {
      if(ctx.endsWith("/")) {
        ctx = ctx.substring(0, ctx.length() - 1);
      }
      rmi = ctx + "/" + loc;
    }
    return rmi;
  }

  public static Remote lookupRemote(String loc, Remote ref, String ctx,
    Class iface ) throws RemoteException
  {
   if(Log.ON) l.finest("looking up remote for "
      +" loc="+loc+" ctx="+ctx+" iface="+iface+" ref="+ref);
    if(loc == null)
      throw new IllegalArgumentException(
        "lookup location can not be null");

    // http(s) locations are direct remote references - no registry
    if(loc.startsWith("http://") || loc.startsWith("https://")) {
      // avoid creating unnecessary stub instances
      soaprmi.server.Services serv =
        soaprmi.soaprpc.SoapServices.getDefault();
      if(ref != null
        && iface.isInstance(ref)
        && loc.equals(serv.getStartpointLocation(ref))
        ) {
        if(Log.ON) l.finest("reused startpoint loc="+loc
          +" iface="+iface+" ref="+ref);
        return ref;
      }
      if(Log.ON) l.finest("creating startpoint loc="+loc+" iface="+iface);
      return  serv.createStartpoint(loc, new Class[]{iface});
    }
    // do RMI registry lookup
    String rmi = makeRMIRef(loc, ctx);
    if(rmi != null) {
      if(Log.ON) l.finest("looking up rmi="+rmi);
      try {
        return Naming.lookup(rmi);
      /*
      } catch(Exception ex) {
        throw new RemoteException("cant lookup in RMI registry "+rmi+": "
          +ex, ex);
      }
      */
      } catch(java.net.UnknownHostException uhe) {
        throw new RemoteException("cant lookup in RMI registry "+rmi+": "
          +uhe, uhe);
      } catch(java.net.MalformedURLException mue) {
        throw new RemoteException("cant lookup in RMI registry "+rmi+": "
          +mue, mue);
      } catch(soaprmi.NotBoundException nbe) {
        throw new RemoteException("cant lookup in RMI registry "+rmi+": "
          +nbe, nbe);
      } catch(soaprmi.AccessException ae) {
        throw new RemoteException("cant lookup in RMI registry "+rmi+": "
          +ae, ae);
      }
    }

    // ultimate sink - do JNDI lookup
    if(ctx == null && loc.startsWith("ldap://")) {
      //int slashPos = loc.indexOf("/", "ldap://".length()+1);
      //int commaPos = loc.lastIndexOf(",");
      //if(commaPos != -1) {
      //  ctx = loc.substring(0, commaPos);
      //  loc = loc.substring(commaPos+1);
      ctx = loc;
      loc = "";
        if(Log.ON) l.finest("spliting location into"
          +" ctx="+ctx+" loc="+loc);
      //}

    }
    if(loc.length() > 0 && loc.indexOf('=') == -1) {
      //throw new RemoteException("attribute name required like cn=Name");
      loc = "cn="+loc;
    }
    Object bound = null;
    try {
      DirContext dirCtx = getInitialDirContext(ctx);
      bound = dirCtx.lookup(loc);
      dirCtx.close();
    } catch(NamingException ne) {
        throw new RemoteException("cant lookup in JNDI \'"+loc+"\'"
          +" (context "+ctx+"): "+ne, ne);
    }
    if(! (bound instanceof Remote)) {
      throw new RemoteException("object bound to JNDI location "
          +loc+" (context "+ctx+") is not SoapRMI remote reference"
          +" but it is "+bound);
    }
    return (Remote) bound;
  }

  public static void rebindRemote(String loc, Remote ref, String ctx)
    throws RemoteException
  {
     rebindRemote(loc, ref, ctx, null);
  }

  public static void rebindRemote(String loc, Remote ref, String ctx,
    Attributes attrs ) throws RemoteException
  {
    if(Log.ON) l.finest("rebinding remote for "
      +" loc="+loc+" ctx="+ctx+" ref="+ref+" attrs="+attrs);
    if(loc == null)
      throw new IllegalArgumentException(
        "lookup location can not be null");

    // http(s) locations are direct remote references - no registry
    if(loc.startsWith("http://") || loc.startsWith("https://")) {
      throw new RemoteException("HTTP references can not be bound "+loc);
    }

    // do RMI registry bind
    String rmi = makeRMIRef(loc, ctx);
    if(rmi != null) {
      if(Log.ON) l.finest("binding rmi="+rmi);
      try {
        Naming.rebind(rmi, ref);
        return;
      } catch(MalformedURLException mue) {
        throw new RemoteException("cant bind in RMI registry "+rmi+": "
          +mue, mue);
      //} catch(soaprmi.AlreadyBoundException nbe) {
      //  throw new RemoteException("cant bind in RMI registry "+rmi+": "
      //    +nbe, nbe);
      } catch(soaprmi.AccessException ae) {
        throw new RemoteException("cant bind in RMI registry "+rmi+": "
          +ae, ae);
      } catch(UnknownHostException uhe) {
        throw new RemoteException("cant bind in RMI registry "+rmi+": "
          +uhe, uhe);
      }
    }

    // ultimate sink - do JNDI
    if(ctx == null && loc.startsWith("ldap://")) {
      //int commaPos = loc.lastIndexOf(",");
      //if(commaPos != -1) {
      //  ctx = loc.substring(0, commaPos);
      //  loc = loc.substring(commaPos+1);
      ctx = loc;
      loc = "";
        if(Log.ON) l.finest("spliting location into"
          +" ctx="+ctx+" loc="+loc);
      //}
    }
    if(Log.ON) l.finest("rebinding to JNDI loc='"+loc+"'"
         +" ctx="+ctx);
    if(loc.length() > 0 && loc.indexOf('=') == -1) {
      //throw new RemoteException("attribute name required like cn=Name");
      loc = "cn="+loc;
    }

    try {
      DirContext dirCtx = getInitialDirContext(ctx);
      dirCtx.rebind(loc, ref, attrs);
      dirCtx.close();
    } catch(NamingException ne) {
        throw new RemoteException("cant bind in JNDI '"+loc+"'"
          +" (context "+ctx+") remote "+ref+": "+ne, ne);
    }
  }


  public static void unbindRemote(String loc, String ctx)
    throws RemoteException
  {
    throw new UnsupportedOperationException("not implemented yet");
  }

  private static Logger l = Logger.getLogger();
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


