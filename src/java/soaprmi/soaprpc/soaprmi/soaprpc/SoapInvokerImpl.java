/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SoapInvokerImpl.java,v 1.5 2003/04/06 00:04:21 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.*;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import javax.naming.Referenceable;

import soaprmi.Remote;
import soaprmi.RemoteException;
import soaprmi.server.Invoker;
import soaprmi.server.RemoteRef;
import soaprmi.mapping.*;
import soaprmi.soap.*;
import soaprmi.port.*;

/**
 * Remote reference to SOAP service described in port.
 *
 * @version $Revision: 1.5 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public abstract class SoapInvokerImpl implements SoapInvoker {
    
    protected SoapInvokerImpl() {
    }
    
    //public static SoapInvoker getDefault() { return instance; }
    
    
    public abstract SoapInvocationHandler createSoapDynamicStub(
        Port port,
        Endpoint epoint,
        XmlJavaMapping mapping,
        Class[] interfaces
    );
    
    /**
     * Return created startpoint to SOAP web service
     */
    public RemoteRef createStartpoint(
        Port port,
        Endpoint epoint,
        XmlJavaMapping defaultMapping
    ) throws RemoteException
    {
        XmlJavaMapping mapping = defaultMapping;
        /*
         if(port.getXmlMap() != null) {
         //TOD make deep clone of portMapping
         mapping = new XmlJavaMapping();
         mapping.setXmlMap(port.getXmlMap);
         mapping.connectTo(defaultMapping);
         }
         */
        
        
        Class javaInterface = null;
        PortType portType = port.getPortType();
        XmlJavaPortTypeMap portTypeMap = null;
        try {
            portTypeMap =
                mapping.queryPortType(portType.getUri(), portType.getName());
        } catch(XmlMapException ex) {
            throw new RemoteException("could not find java class for interface "
                                          +" uri="+portType.getUri() +" name="+ portType.getName(),
                                      ex);
            //throw new RemoteException("can't get port xml java mapping", ex);
        }
        try {
            
            //if(portTypeMap == null) {
            //
            //}
            
            // TODO: try to load generated stub and if succedded return it
            // use extra information from SoapRMIBinding
            // Class.forName(port.getName()+"_SoapRMIStub")
            
            // otherwise create dynamic stub
            javaInterface = portTypeMap.javaClass();
            
            Class[] interfaces = new Class[]{RemoteRef.class,
                    Referenceable.class, javaInterface};
            
            
            SoapInvocationHandler dynaStub = createSoapDynamicStub(
                port,
                epoint,
                mapping,
                interfaces
            );
            
            return (RemoteRef) Proxy.newProxyInstance(getClass().getClassLoader(),
                                                      interfaces,
                                                      dynaStub);
            
            //return null;
        } catch(Exception ex) {
            throw new RemoteException("cant create dynamic stub for port", ex);
        }
    }
    
    
    //private static SoapInvoker instance = new SoapInvokerImpl();
    
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

