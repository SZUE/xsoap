/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: RemoteWrapper.java,v 1.4 2003/04/06 00:04:18 aslom Exp $
 */

package soaprmi.server;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

import soaprmi.Remote;
import soaprmi.port.Port;

import soaprmi.util.logging.Logger;

/**
 * Special chaining InvocationHandler that mimics all interfaces of
 * wrapped object and also creates internal port for remote references
 * only if necessary.
 *
 * @see Services.wrapRemote
 *
 * @version $Revision: 1.4 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class RemoteWrapper implements InvocationHandler {
    
    protected static Method hashCodeMethod;
    protected static Method equalsMethod;
    protected static Method toStringMethod;
    
    protected static Method getSoapRMIPortMethod;
    protected static Method getReference;
    
    private static Logger logger = Logger.getLogger();
    protected Port port;
    //protected Endpoint epoint;
    //protected XmlJavaMapping mapping;
    
    protected InvocationHandler chainedInvoker;
    protected Object unwrappedObject;
    protected boolean implementsRemoteRef;
    protected boolean implementsReferenceable;
    
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
    
    public RemoteWrapper(Object objectToWrap,
                         InvocationHandler invokerToChain)
    {
        if(objectToWrap == null) throw new IllegalArgumentException(
                "object to wrap can not be null");
        if(invokerToChain == null) throw new IllegalArgumentException(
                "chained invoker can not be null");
        unwrappedObject = objectToWrap;
        chainedInvoker = invokerToChain;
        implementsRemoteRef = unwrappedObject instanceof RemoteRef;
        implementsReferenceable = unwrappedObject instanceof Referenceable ;
    }
    
    
    public Object invoke(Object proxy, Method m, Object[] params)
        throws Throwable //RemoteException
    {
        if(implementsRemoteRef == false) {
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
            }
            
        }
        if(implementsReferenceable == false) {
            if (m.equals(getReference)) {
                return new Reference(Port.class.getName(),
                                     new StringRefAddr("soaprmiport", port.toXml()));
            }
        }
        
        // forward ot call to chained invoker if not caugh by now
        return chainedInvoker.invoke(unwrappedObject, m, params);
    }
    
    protected Integer proxyHashCode(Object proxy) {
        return new Integer((port != null) ? port.hashCode() : System.identityHashCode(proxy)
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


