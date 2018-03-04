/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: WSDLUtil.java,v 1.7 2004/06/08 21:38:45 srikrish Exp $
 */

package soaprmi.wsdl;

import soaprmi.RemoteException;
import soaprmi.server.RemoteRef;
import soaprmi.soaprpc.SoapServices;

import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;
import java.util.Map;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPAddress;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Utility to convert an XSOAP reference to a WSDL document
 * @version $Revision: 1.7 $
 * @author Sriram Krishnan <srikrish@extreme.indiana.edu>
 */

public class WSDLUtil {

    /**
     * Converts an XSOAP reference to WSDL. The implicit assumption is that
     * every service contains only one portType. Default mappings will be 
     * used to retrieve the interface responsible for the reference.
     * 
     * @param remote XSOAP Remote Reference
     * @param serviceName Name for the service in the WSDL document
     * @return WSDL Document as a String
     */
    public static String convertRefToWSDL(RemoteRef remote,
                                          String serviceName)
        throws RefWSDLException {

        soaprmi.port.Port port = remote.getSoapRMIPort();

        // assuming default mappings, get the class for the interface
        String portClassName = port.getPortType().getName();

        return convertRefToWSDL(remote, serviceName, portClassName);
    }

    /**
     * Converts an XSOAP reference to WSDL. The implicit assumption is that
     * every service contains only one portType.
     * 
     * @param remote XSOAP Remote Reference
     * @param serviceName Name for the service in the WSDL document
     * @param portClassName The FQN of the interface exported
     * @return WSDL Document as a String
     */
    public static String convertRefToWSDL(RemoteRef remote,
                                          String serviceName,
                                          String portClassName)
        throws RefWSDLException {
        
        soaprmi.port.Port port = remote.getSoapRMIPort();
        return convertRefToWSDL(serviceName,
                                portClassName,
                                port.getPortType().getUri(),
                                port.getEndpoint().getLocation());
    }

    /**
     * Converts an XSOAP reference to WSDL. The implicit assumption is that
     * every service contains only one portType.
     * 
     * @param serviceName Name for the service in the WSDL document
     * @param portClassName The FQN of the interface exported
     * @param namespace the namespace for the WSDL
     * @param location the http(s) location for the XSOAP service
     * @return WSDL Document as a String
     */
    public static String convertRefToWSDL(String serviceName,
                                          String portClassName,
                                          String namespace,
                                          String location) 
        throws RefWSDLException {
        return convertRefToWSDL(serviceName,
                                portClassName,
                                namespace,
                                namespace, // use the same namespace for the interface
                                location);
    }

    /**
     * Converts an XSOAP reference to WSDL. The implicit assumption is that
     * every service contains only one portType.
     * 
     * @param serviceName Name for the service in the WSDL document
     * @param portClassName The FQN of the interface exported
     * @param namespace the namespace for the WSDL
     * @param bindingNamespace the namespace for the binding of the operation
     * @param location the http(s) location for the XSOAP service
     * @return WSDL Document as a String
     */
    public static String convertRefToWSDL(String serviceName,
                                          String portClassName,
                                          String namespace,
                                          String bindingNamespace,
                                          String location) 
        throws RefWSDLException {

        // create a new emitter object
        Emitter emitter = new Emitter();
        
        // set the name of the service
        emitter.setServiceElementName(serviceName);
        
        // set the name of the port
        emitter.setServicePortName(serviceName + "Port");

        // set the portType for the interface
        // using the portClassName as portType name
        emitter.setPortTypeName(portClassName);

        // set the namespaces for the interface, and service
        //emitter.setIntfNamespace(namespace);
        emitter.setImplNamespace(namespace);
        emitter.setTargetService(bindingNamespace);

        // set the end point of the XSOAP reference
        emitter.setLocationUrl(location);

        // set the class for the interface to generate the WSDL
        try {
            emitter.setCls(portClassName);
        } catch (ClassNotFoundException cnfe) {
            throw new RefWSDLException("PortType class " + portClassName + " not found",
                                       cnfe);
        }

        // emit the WSDL and return it
        try { 
            return emitter.emitToString(Emitter.MODE_ALL);
        } catch (IOException ioe) {
            throw new RefWSDLException("IOException while emitting WSDL",
                                       ioe);
        } catch (WSDLException we) {
            throw new RefWSDLException("WSDLException while emitting WSDL",
                                       we);
        } catch (SAXException se) {
            throw new RefWSDLException("SaxException while emitting WSDL",
                                       se);
        } catch (ParserConfigurationException pce) {
            throw new RefWSDLException("ParserConfigurationException while emitting WSDL",
                                       pce);
        }
    }
    
    /**
     * Converts a WSDL to an XSOAP reference. The implicit assumption is that
     * every service contains only one portType. The portType's local name is
     * assumed to be the FQN of the java interface.
     * 
     * @param wsdlString String representation for the WSDL to be converted
     * @return XSOAP Remote reference. Null if one can't be created.
     */
    public static RemoteRef convertWSDLToRef(String wsdlString)
        throws RefWSDLException {
        
        WSDLFactory factory = null;
        WSDLReader reader = null;
        Definition defn = null;

        try {
            // read in the WSDL
            factory = WSDLFactory.newInstance();
            reader = factory.newWSDLReader();
            defn = reader.readWSDL(null,
                                   new InputSource(new StringReader(wsdlString)));

        } catch (WSDLException we) {
            throw new RefWSDLException("WSDLException while parsing WSDL",
                                       we);
        }

        // get the portType(s) associated with this service
        Map portTypeMap = defn.getPortTypes();
        if (portTypeMap.size() < 1)
            return null;
        else {
            PortType portType = 
                (PortType) ((Map.Entry) (portTypeMap.entrySet().toArray()[0])).getValue();

            // Assumption: In the absence of any mappings, portType's local name
            // is the FQN for the java interface of the portType
            String portTypeName = portType.getQName().getLocalPart();
            return convertWSDLToRef(wsdlString, portTypeName);
        }
    }

    /**
     * Converts a WSDL to an XSOAP reference. The implicit assumption is that
     * every service contains only one portType.
     *
     * @param wsdlString String representation for the WSDL to be converted
     * @param portTypeName The FQN for the interface corresponding to the portType
     * @return XSOAP remote reference. Null if one can't be created
     */
    public static RemoteRef convertWSDLToRef(String wsdlString,
                                             String portTypeName)
        throws RefWSDLException {
        
        WSDLFactory factory = null;
        WSDLReader reader = null;
        Definition defn = null;
        
        try {
            // read in the WSDL
            factory = WSDLFactory.newInstance();
            reader = factory.newWSDLReader();
            defn = reader.readWSDL(null,
                                   new InputSource(new StringReader(wsdlString)));
        } catch (WSDLException we) {
            throw new RefWSDLException("WSDLException while parsing WSDL",
                                       we);
        }

        // Get the location of the service
        Map serviceMap = defn.getServices();
        if (serviceMap.size() < 1)
            return null;
        else {
            Service service =
                (Service) ((Map.Entry) (serviceMap.entrySet().toArray()[0])).getValue();
            
            Map portMap = service.getPorts();
            if (portMap.size() < 1)
                return null;
            else {
                Port port =
                    (Port) ((Map.Entry) (portMap.entrySet().toArray()[0])).getValue();
                List extElems = port.getExtensibilityElements();
                if (extElems.size() < 1)
                    return null;
                else {
                    // Assuming first Extensibility Element is SOAPAddress
                    SOAPAddress sAddress = (SOAPAddress) extElems.get(0);
                    String location = sAddress.getLocationURI();
                    
                    // create remote reference from all the information
                    try { 
                        RemoteRef ref = SoapServices.getDefault().
                            createStartpoint(location,
                                             new Class[]{
                                                 Class.forName(portTypeName)});
                        return ref;
                    } catch (RemoteException re) {
                        throw new RefWSDLException
                            ("Can't create startpoint for service at location " 
                             + location 
                             + " using class " 
                             + portTypeName,
                             re);
                    } catch (ClassNotFoundException cnfe) {
                        throw new RefWSDLException("Class " + portTypeName + " not found",
                                                   cnfe);
                    }
                }
            }
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
