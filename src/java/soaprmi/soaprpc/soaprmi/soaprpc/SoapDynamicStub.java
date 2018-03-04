/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SoapDynamicStub.java,v 1.5 2003/04/06 00:04:20 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.*;
import java.net.*;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;

import javax.naming.Reference;
import javax.naming.StringRefAddr;

import soaprmi.RemoteException;
import soaprmi.soap.*;
import soaprmi.port.*;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.server.RemoteRef;
//import soaprmi.util.*;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;


/**
 * Remote reference to SOAP service described in port.
 *
 * @version $Revision: 1.5 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public abstract class SoapDynamicStub implements SoapInvocationHandler {
  // preloaded Method objects for the methods in java.lang.Object
  private static Method hashCodeMethod;
  private static Method equalsMethod;
  private static Method toStringMethod;

  private static Method getSoapRMIPortMethod;
  private static Method getReference;

  static {
    try {
      getSoapRMIPortMethod =
        soaprmi.server.RemoteRef.class.getMethod("getSoapRMIPort", null);
      getReference =
        javax.naming.Referenceable.class.getMethod("getReference", null);
      //setSoapRMIPortMethod =
      //  soaprmi.server.RemoteStub.class.getMethod("setSoapRMIPort", null);
      hashCodeMethod = Object.class.getMethod("hashCode", null);
      equalsMethod =
        Object.class.getMethod("equals", new Class[] { Object.class });
      toStringMethod = Object.class.getMethod("toString", null);
    } catch (NoSuchMethodException e) {
      throw new NoSuchMethodError(e.getMessage());
    }
  }

  public SoapDynamicStub(Port port_, Endpoint epoint_,
    XmlJavaMapping mapping_, Class[] interfaces_)
  {
    port = port_;
    if(port == null)
      throw new IllegalArgumentException(
        "Dynamic stub can not be created for null port");
    epoint = epoint_;
    mapping = mapping_;
    Binding binding = epoint.getBinding();
    if(binding != null) {
      targetURI = binding.getName();
    } else {
      targetURI = epoint.getLocation();
    }
  }


  public abstract Object invokeTransport(Object proxy, Method m,
    Object[] params) throws Throwable; //RemoteException //

  public Object invoke(Object proxy, Method m, Object[] params)
    throws Throwable //RemoteException
  {

    Class declaringClass = m.getDeclaringClass();
    if (declaringClass == Object.class) {
      if (m.equals(hashCodeMethod)) {
        return proxyHashCode(proxy);
      } else if (m.equals(equalsMethod)) {
        return proxyEquals(proxy, params[0]);
      } else if (m.equals(toStringMethod)) {
        return proxyToString(proxy);
      } else {
        throw new InternalError(
          "unexpected Object method dispatched: " + m);
       }
    } else if (m.equals(getSoapRMIPortMethod)) {
      return port;
    } else if (m.equals(getReference)) {
      return new Reference(Port.class.getName(),
        new StringRefAddr("soaprmiport", port.toXml()));
    } //else if (m.equals(setSoapRMIPortMethetod)) {
    //  port = (Port) params[0];
    //  return null;
    //}
    try {
    return // invokeSocketHttp(proxy, m, params);
      invokeTransport(proxy, m, params);
    } catch(InvocationTargetException ex) {
        throw ex.getTargetException();
    }
  }

  protected MethodInvoker newCall(Method m)
    throws SoapException, RemoteException
  {
     List list = null;
     synchronized(pool) {
       list = (List) pool.get(m);
       if(list == null) {
         list = new ArrayList();
         pool.put(m, list);
       }
     }
     MethodInvoker mi = null;
     if(list.size() > 0) {
       synchronized(list) {
         if(list.size() > 0) {
           mi = (MethodInvoker) list.remove(list.size() - 1 );
         }
       }
     }
     if(mi == null) {
       mi = MethodInvoker.makeMethodInvoker(port, epoint, m, mapping);
       //System.err.println("new method invoker created mi="+mi);
     }
     return mi;
  }

  protected void returnCallToPool(MethodInvoker mi, Method m) {
     List list = null;
     synchronized(pool) {
       list = (List) pool.get(m);
       if(list == null) {
          throw new RuntimeException(
            "can not return to pool something that was not there..."
               +mi.getMethodRequestName());
       }
     }
     synchronized(list) {
       list.add(mi);
     }
  }

  protected Integer proxyHashCode(Object proxy) {
    return new Integer(
      (port != null) ? port.hashCode() : System.identityHashCode(proxy)
    );
  }

  protected Boolean proxyEquals(Object proxy, Object other) {
    //return (proxy == other ? Boolean.TRUE : Boolean.FALSE);
    if (other == proxy) {
      return Boolean.TRUE;
    }
    if (other == null) {
     return Boolean.FALSE;
    }
    RemoteRef otherRef;
    try {
      otherRef = (RemoteRef) other;
    } catch (ClassCastException e) {
      return Boolean.FALSE;
    }

    Port otherPort = otherRef.getSoapRMIPort();
    return port != null ?
      ((port.equals(otherPort)) ? Boolean.TRUE : Boolean.FALSE) : Boolean.FALSE;
  }

  protected String proxyToString(Object proxy) {
    return proxy.getClass().getName() + '@' +
           Integer.toHexString(proxy.hashCode())
           +" to "+port;
  }
  // --- private

  private Logger l = Logger.getLogger();
  protected String targetURI;
  //protected String soapAction;
  protected Port port;
  protected Endpoint epoint;
  protected XmlJavaMapping mapping;
  //private URL url;
  private Map pool = new HashMap(); // Method -> List of Calls
  //private Class[] interfaces;
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

