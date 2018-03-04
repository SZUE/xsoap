/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Port.java,v 1.5 2003/05/18 13:16:05 aslom Exp $
 */

package soaprmi.port;

import java.io.StringWriter;

import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlMapException;
import soaprmi.util.logging.Logger;

/**
 * Port describes access point to remote object.
 *
 * @version $Revision: 1.5 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class Port implements Cloneable {
    private static Logger l = Logger.getLogger();

    static {
        try {
            defaultMappings();
        } catch(XmlMapException ex) {
            l.severe("could initialize soap rmi port mappings", ex);
        }
    }

    public final static String NAMESPACE =
        "http://www.extreme.indiana.edu/soap/rmi/port/v10/";

    private static void defaultMappings() throws XmlMapException {
        // initialize default mapings for SOAP remote-ref
        soaprmi.mapping.XmlJavaMapping mapping =
            soaprmi.soap.Soap.getDefault().getMapping();
        // map SOAPStruct into namespace:http://soapinterop.org/ : SOAPStruct
        mapping.mapStruct(NAMESPACE,
                          "port", Port.class);
        mapping.mapStruct(NAMESPACE,
                          "binding", Binding.class);
        mapping.mapStruct(NAMESPACE,
                          "port-type", PortType.class);
        mapping.mapStruct(NAMESPACE,
                          "endpoint", Endpoint.class);
    }


    public String getName() {
        return name;
    }

    public void setName(String name_) {
        name = name_;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String name_) {
        userName = name_;
    }

    //public String getUri() {
    //  return uri;
    //}

    //public void setUri(String uri_) {
    //  uri = uri_;
    //}


    // TODO: should be PortType[] portTypes
    public PortType getPortType() {
        return portType;
    }

    public void setPortType(PortType portType_) {
        portType = portType_;
    }

    // TODO: should be Endpoint[] endpoints
    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint_) {
        if(endpoint != null)
            throw new IllegalArgumentException("port already has endpoint");
        endpoint = endpoint_;
    }

    // TODO: get/setMapping XmlMap[]
    //public void setMapping(XmlMap[] map) {
    //}

    //public XmlMap[] getMapping() {
    //  return new XmlMap("FIXME test", "only");
    //}
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        Port other;
        try {
            other = (Port) o;
        } catch (ClassCastException e) {
            return false;
        }
        //NOTE: port name is irrelevant for equality tests
        return //(PortUtil.equalsRef(name, other.getName()))
            (PortUtil.equalsRef(portType, other.getPortType()))
            && (PortUtil.equalsRef(endpoint, other.getEndpoint()));
    }

    public Object clone() {
        Port copyPort = null;
        try {
            copyPort = (Port) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError(e.toString());
        }
        copyPort.portType = (PortType) portType.clone();
        copyPort.endpoint = (Endpoint) endpoint.clone();
        return copyPort;
    }

    public String toString() {
        return "Port" + "["
            +"name="+name
            +",portType="+portType
            +",endpoint="+endpoint
            +"]";
    }

    public int hashCode() {
        if(portType == null || endpoint == null) {
            throw new RuntimeException(
                "trying to get hashCode() for incomplete Port");
        }
        return portType.hashCode() ^ endpoint.hashCode();
    }

    public String toXml() {
        try {
            StringWriter sw = new StringWriter();
            soaprmi.soap.Marshaller.marshal(this, sw);
            sw.close();
            return sw.toString();
        } catch(Exception ex) {
            throw new IllegalArgumentException(
                "can not port "+this+" marshal into XML: "+ex);
        }
    }

    /*
     public static Port loadPort(Reader reder)
     throws IOException, UnmarshalException
     {
     throw new UnsupportedOperationException("not implemented yet");
     }

     public static Port loadPort(Class klass, String portLocation)
     throws IOException
     {
     Reader reader = Util.readerFor(klass, portLocation);
     return loadPort(reader)
     }

     public void savePort(Writer writer)
     throws IOException, MarshalException
     {
     soaprmi.soap.Marshaller.marshal(port, writer);
     }
     */

    private String uri;
    private String name;
    private String userName = "";
    private PortType portType;
    private Endpoint endpoint;
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

