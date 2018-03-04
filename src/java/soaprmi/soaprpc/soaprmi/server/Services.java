/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Services.java,v 1.15 2003/04/06 00:04:19 aslom Exp $
 */

package soaprmi.server;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import javax.naming.Referenceable;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import java.net.Socket;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;


import soaprmi.AlreadyBoundException;
import soaprmi.NotBoundException;
import soaprmi.Remote;
import soaprmi.RemoteException;
import soaprmi.ServerException;
import soaprmi.port.*;

import soaprmi.soap.EncodingStyle;
import soaprmi.soap.SoapException;
import soaprmi.soap.Serializer;
import soaprmi.soapenc.SoapEnc;

//import soaprmi.port.SoapRMIPort;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlJavaPortTypeMap;
import soaprmi.mapping.XmlMapException;
import soaprmi.soap.Soap;
import soaprmi.soap.SoapStyle;
import soaprmi.soap.MarshalException;
import soaprmi.soap.Unmarshaller;
import soaprmi.soap.ValidationException;
import soaprmi.util.logging.Logger;

/**
 * Define entry points to web services.
 *
 * @version $Revision: 1.15 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class Services {
    private static Logger l = Logger.getLogger();

    // -- internal
    private XmlJavaMapping mapping;
    private List dispatchers = new ArrayList();
    private List invokers = new ArrayList();
    private List ports = new ArrayList();
    //private Map name2Port = new HashMap();
    //private static Services instance = new Services();

    protected static class ConnectionContextImpl implements ConnectionContext {
        Socket incomingSocket;
        Map incomingConnectionProps;
        Map outgoingConnectionProps;


        public Socket getIncomingSocket() throws ServerException
        {
            return incomingSocket;
        }

        public void setIncomingSocket(Socket socket) throws ServerException
        {
            this.incomingSocket = socket;
        }

        public Map getIncomingProps() throws ServerException
        {
            return incomingConnectionProps;
        }

        public void setIncomingProps(Map connectionProps_) throws ServerException
        {
            //try {
            // make copy of map passed in
            incomingConnectionProps = connectionProps_;
            //} catch ( CloneNotSupportedException ex ) {
            //  throw new ServerException("could not set incoming connection props: "+ex, ex);
            //}
        }

        public void setOutgoingProps(Map connectionProps_) throws ServerException
        {
            outgoingConnectionProps = connectionProps_;
        }

        public Map getOutgoingProps() throws ServerException
        {
            if(outgoingConnectionProps == null) {
                outgoingConnectionProps = new HashMap();
            }
            return outgoingConnectionProps;
        }


    }

    protected static class ConnectionContextManager extends ThreadLocal {
        public Object initialValue() {
            return new ConnectionContextImpl();
        }

        public ConnectionContext getConnectionContext() {
            return (ConnectionContext) super.get();
        }
    }

    protected static
        ConnectionContextManager connectionContextManager = new ConnectionContextManager();

    protected Services() {
        mapping = new XmlJavaMapping();
        mapping.connectTo(Soap.getDefault().getMapping());
        try {
            EncodingStyle enc = SoapEnc.getDefault();
            synchronized(enc) {
                if(enc.queryInterfaceSerializer(RemoteRef.class) == null) {
                    Serializer remoteRefSerializer = new RemoteRefSerializer();
                    enc.registerInterfaceEncodingHandler(
                        RemoteRef.class, remoteRefSerializer, null);
                    enc.registerConverterFrom(Port.class, new RemoteRefConverter());
                }
            }
        } catch(SoapException ex) {
            l.severe("could not add remote ref mapping :"+ex, ex);
            throw new RuntimeException("could not add remote ref mapping :"+ex);
        }
    }

    //** this is shared byu _all_ services!!! */
    public static ConnectionContext getConnectionContext() {
        return connectionContextManager.getConnectionContext();
    }

    //public static Services getDefault() { return instance; }

    public Remote wrapService(Object objectToWrap,
                              InvocationHandler invokerToChain)
    {
        if(objectToWrap == null) throw new IllegalArgumentException(
                "object to wrap can not be null");
        if(invokerToChain == null) throw new IllegalArgumentException(
                "chained invoker can not be null");

        // discover all interfaces
        Class klass = objectToWrap.getClass();
        Class[] objInterfaces = klass.getInterfaces();
        // add RemoteRef and Referenceable if not on the list
        boolean implementsRemoteRef = objectToWrap instanceof RemoteRef;
        boolean implementsReferenceable = objectToWrap instanceof Referenceable ;

        Class[] interfaces = objInterfaces;
        int len = objInterfaces.length;
        // create proxy and return to the user as wrapped object
        if(!implementsRemoteRef || !implementsReferenceable) {
            if(!implementsRemoteRef || !implementsReferenceable) {
                interfaces = new Class[len + 2];
                interfaces[ len ] = RemoteRef.class;
                interfaces[ len + 1 ] = Referenceable.class;
            } else if(!implementsRemoteRef) {
                interfaces = new Class[len + 1];
                interfaces[ len ] = RemoteRef.class;
            } else if(!implementsReferenceable) {
                interfaces = new Class[len + 1];
                interfaces[ len ] = Referenceable.class;
            }
            System.arraycopy(objInterfaces, 0, interfaces, 0, len);
        }

        InvocationHandler remoteWrapper = new RemoteWrapper(
            objectToWrap,
            invokerToChain
        );

        return (RemoteRef) Proxy.newProxyInstance(objectToWrap.getClass().getClassLoader(),
                                                  interfaces,
                                                  remoteWrapper);

    }

    public XmlJavaMapping getMapping() {
        return mapping;
    }

    public void setMapping(XmlJavaMapping value) {
        mapping = value;
        if(mapping == null)
            mapping = Soap.getDefault().getMapping();
    }

    public Port[] getPorts() {
        return (Port[]) ports.toArray();
    }

    public Port createPort(
        String name,     // portName
        Class iface,  // portType
        Object impl   // implementaion of port type
    ) throws ServerException, AlreadyBoundException
    {
        return createPort(name, new Class[]{iface}, impl);
    }


    /**
     * this is a hook method used by susclasses to register
     * default dsipatcher if class user did not set any dispatchers
     */
    public void addDefaultDispatcher() throws IOException, RemoteException {
        throw new ServerException("at least one dispatcher must be available");

    }

    public Port createPort(
        String name,     // portName
        Class interfaces[],  // portType
        Object impl   // implementaion of port type
    ) throws ServerException, AlreadyBoundException
    {
        if(impl == null)  throw new IllegalArgumentException(
                "object implementing port can not be null");
        if(interfaces == null || interfaces.length == 0)
            throw new IllegalArgumentException(
                "port must have at least one interface");
        int countInterfaces = 0;
        //if(interfaces.length > 1)
        for (int i = 0; i < interfaces.length; i++)
        {
            if(interfaces[i] != Remote.class
               &&  interfaces[i] != RemoteRef.class
               &&  interfaces[i] != Referenceable.class
              )
            {
                ++countInterfaces;
            }
        }
        if(countInterfaces == 0)
            throw new IllegalArgumentException(
                "at least one non XSOAP interface must be implemented");
        if(countInterfaces > 1)
            throw new IllegalArgumentException(
                "multiple port interfaces are not supported in this version");
        //if(name2Port.get(name) != null) throw new AlreadyBoundException(
        //  "there is already port bound to name "+name);
        if(dispatchers.isEmpty()) {
            try {
                addDefaultDispatcher();
            } catch(IOException ioe) {
                throw new ServerException("could not start default dispatcher", ioe);
            } catch(RemoteException ex) {
                throw new ServerException("could not start default dispatcher", ex);
            }
        }
        try {
            Port port = new Port();
            port.setName(name);
            XmlJavaPortTypeMap portMap = mapping.queryPortType(interfaces[0]);
            PortType portType = new PortType();
            portType.setUri(portMap.getUri());
            portType.setName(portMap.getLocalName());
            port.setPortType(portType);
            // TODO: create endpoint with each dispatcher and add endpoints to port
            for (Iterator iter = dispatchers.iterator(); iter.hasNext(); ) {
                Dispatcher dsptr = (Dispatcher) iter.next();
                Endpoint epoint  = dsptr.createEndpoint(port, impl, mapping);
                port.setEndpoint(epoint);
            }
            //*
            //port.setName("FOO");
            //port.getEndpoint().getBinding().setName("FOO");
            //*/

            if(RemoteObject.class.isAssignableFrom(impl.getClass()) )
                    ((RemoteObject)impl).port = port;
            ports.add(port);
            return port;
        } catch(Exception ex) {
            throw new ServerException("can't create port", ex);
        }
    }

    //** @return null if port is not found */
    //public Port port(String name) {
    //  return (Port) name2Port.get(name);
    //}

    protected Object findLocalEndpoint(Port port) {
        // check dispatchers if theres is a local implementation
        for (Iterator iter = dispatchers.iterator(); iter.hasNext(); ) {
            Dispatcher dsptr = (Dispatcher) iter.next();
            Object impl = dsptr.findObject(port);
            if(impl != null) return impl;
        }
        return null;
    }

    public RemoteRef createStartpoint(Port port)
        throws RemoteException
    {
        Object obj = findLocalEndpoint(port);
        if(obj != null && RemoteRef.class.isAssignableFrom(obj.getClass())) {
            //((RemoteStub)obj).setSoapRMIPort(port);
            RemoteRef ref = (RemoteRef)obj;
            Port stubPort = ref.getSoapRMIPort();
            if(stubPort != null && stubPort.equals(port))
                return ref;
        }
        //no local endpoint found - create stub

        //check all invokers
        //for each epoint in port.getEndpoints() {
        Endpoint epoint = port.getEndpoint();
        for (Iterator iter = invokers.iterator(); iter.hasNext(); ) {
            Invoker invkr = (Invoker) iter.next();
            RemoteRef ref = invkr.createStartpoint(
                port,
                epoint,
                mapping
            );
            if(ref != null)
                return ref;
        }
        //}
        throw new RemoteException("cant get remote reference to port "+port);
    }

    public RemoteRef createStartpoint(String serviceUrl,
                                      Class[] interfaces)
        throws RemoteException
    {
        return createStartpoint(serviceUrl, interfaces, null);
    }

    public RemoteRef createStartpoint(String serviceUrl,
                                      Class[] interfaces, String endpointBindingName)
        throws RemoteException
    {
        return createStartpoint(serviceUrl, interfaces,
                                endpointBindingName, null, null);
    }

    public RemoteRef createStartpoint(String serviceUrl,
                                      Class[] interfaces, String endpointBindingName,
                                      SoapStyle style, String soapAction)
        throws RemoteException
    {
        if(interfaces.length > 1)
            throw new IllegalArgumentException(
                "multiple port interfaces are not supported in this version");
        Port port = new Port();
        port.setName("");
        XmlJavaPortTypeMap portMap;
        try {
            portMap = mapping.queryPortType(interfaces[0]);
        } catch(soaprmi.mapping.XmlMapException ex) {
            throw new RemoteException("can't determine xml type for java type "
                                          +interfaces[0], ex);
        }
        PortType portType = new PortType();
        portType.setUri(portMap.getUri());
        portType.setName(portMap.getLocalName());
        port.setPortType(portType);
        Endpoint epoint  = new Endpoint();
        epoint.setLocation(serviceUrl);
        port.setEndpoint(epoint);
        if(endpointBindingName != null) {
            Binding binding = new SoapBinding();
            if(style != null || soapAction != null) {
                SoapBinding sb = new SoapBinding();
                binding = sb;
                sb.setSoapAction(soapAction);
                //sb.setSoapStyle(style);
            }
            epoint.setBinding(binding);
            binding.setName(endpointBindingName);
        }
        return createStartpoint(port);
    }

    public RemoteRef createStartpoint(Port port, Class[] interfaces)
        throws RemoteException
    {
        if(interfaces.length > 1)
            throw new IllegalArgumentException(
                "multiple port interfaces are not supported in this version");
        PortType portType = port.getPortType();
        Class javaInterface;
        try {
            XmlJavaPortTypeMap portTypeMap =
                mapping.queryPortType(portType.getUri(), portType.getName());
            javaInterface = portTypeMap.javaClass();
        } catch(XmlMapException ex) {
            throw new RemoteException("could not find java class for interface "
                                          +" uri="+portType.getUri() +" name="+ portType.getName(),
                                      ex);
        }
        // extra validation
        if(! javaInterface.equals(interfaces[0])) {
            throw new RemoteException(
                "passed interface "+interfaces[0]+" must be mapped to passed "
                    +" port type of port "+port);
        }
        return createStartpoint(port);
    }


    public RemoteRef createStartpointFromXml(String portXml)
        throws RemoteException
    {
        StringReader sr = new StringReader(portXml);
        Port port;
        try {
            port = (Port) Unmarshaller.unmarshal(Port.class, sr);
        } catch(MarshalException ex) {
            throw new RemoteException(
                "can't unmarshal port from xml: '"+portXml+"'", ex);
        } catch(ValidationException ex) {
            throw new RemoteException(
                "validation error when unmarshaling port from xml: '"
                    +portXml+"'", ex);
        }
        return createStartpoint(port);
    }

    public String getStartpointLocation(Remote ref) throws RemoteException{
        if(ref == null)
            throw new IllegalArgumentException(
                "cant get location of null remote object");
        if(! (ref instanceof RemoteRef)) {
            throw new RemoteException("Object is not XSOAP remote refernce "+ref);
        }
        Port port = ((RemoteRef)ref).getSoapRMIPort();
        if(port == null) {
            throw new RemoteException("remote object was not exported");
        }
        Endpoint epoint = port.getEndpoint();
        return epoint.getLocation();
    }

    public int getStartpointLocationPort(Remote ref) throws RemoteException{
        String loc = getStartpointLocation(ref);
        int slashSlash = loc.indexOf("//");
        int colon = loc.indexOf(":", slashSlash+1);
        int slash = loc.indexOf("/", colon+1);
        if(slashSlash == -1 || colon == -1 || slash == -1) {
            throw new RemoteException("cant get port - cant parse"
                                          +" location '"+loc+"' for remote refernce "+ref);
        }
        String s = loc.substring(colon+1, slash);
        try {
            return Integer.parseInt(s);
        } catch(NumberFormatException ex) {
            throw new RemoteException("cant get port - cant get number for"
                                          +" port '"+s+"' from "
                                          +" location '"+loc+"' for remote refernce "+ref);
        }
    }

    public String getStartpointXml(Remote ref) throws RemoteException {
        if(ref == null)
            throw new IllegalArgumentException(
                "cant get location of null remote object");
        if(! (ref instanceof RemoteRef)) {
            throw new RemoteException("Object is not SoapRMI remote refernce "
                                          +ref);
        }
        Port port = ((RemoteRef)ref).getSoapRMIPort();
        if(port == null) {
            throw new RemoteException("remote object was not exported");
        }
        return port.toXml();
    }


    public void setStartpointLocation(Remote ref, String loc)
        throws RemoteException
    {
        if(ref == null)
            throw new IllegalArgumentException(
                "cant get location of null remote object");
        if(! (ref instanceof RemoteRef)) {
            throw new RemoteException("Object is not SoapRMI remote refernce "
                                          +ref);
        }
        Port port = ((RemoteRef)ref).getSoapRMIPort();
        if(port == null) {
            throw new RemoteException("remote object was not exported");
        }
        Endpoint epoint = port.getEndpoint();
        epoint.setLocation(loc);
    }


    public RemoteRef loadStartpoint(Class klass,
                                    String portLocation,
                                    Class[] interfaces)
        throws RemoteException, IOException
    {
        //Port port = Port.loadPort(klass, portLocation);
        //return createStartpoint(port, interfaces);
        throw new UnsupportedOperationException("not implemented yet");
    }

    public void saveStartpoint(Remote remote, Writer writer)
        throws RemoteException, IOException
    {
        //RemoteRef ref = (RemoteRef) remote;
        //Port port = ref.getSoapRMIPort();
        //port.savePort(writer);
        throw new UnsupportedOperationException("not implemented yet");
    }


    public int dispatchersSize() {
        return dispatchers.size();
    }

    public void addDispatcher(Dispatcher dsptr) {
        if(dsptr == null) throw new IllegalArgumentException(
                "dispatcher can not be null");
        dispatchers.add(dsptr);
    }

    public void addInvoker(Invoker invkr) {
        if(invkr == null) throw new IllegalArgumentException(
                "invoker can not be null");
        invokers.add(invkr);
    }

    public void setDispatcher(Dispatcher dsptr) {
        if(dsptr == null) throw new IllegalArgumentException(
                "dispatcher can not be null");
        dispatchers.clear();
        dispatchers.add(dsptr);
    }

    public void setInvoker(Invoker invkr) {
        if(invkr == null) throw new IllegalArgumentException("invoker can not be null");
        invokers.clear();
        invokers.add(invkr);
    }

    public static Random GUIDrandom = new Random();
    private static int GUIDcounter;

    /**
     * Generate "pesudo" unique identifier - current algorithm is
     * is concatinating: time in milliseconds, random integer and
     * incremented counter; in futire it shoiuld use real UUID...
     */
    public String createGUID() {
        //one-liner was too simple - will return duplicates when called too fast...
        //return "GUID_"+System.currentTimeMillis();
        StringBuffer buf = new StringBuffer("GUID_");
        buf.append(System.currentTimeMillis());
        buf.append('_');
        buf.append(Math.abs(GUIDrandom.nextInt()));
        buf.append('_');
        // this operation should be syunchronized to *guarantee*
        //  uniqueness but it is too much hassle and we have random above...
        buf.append(++GUIDcounter);
        return buf.toString();
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



