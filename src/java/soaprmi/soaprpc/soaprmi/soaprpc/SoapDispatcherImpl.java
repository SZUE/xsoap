/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*- */ //------100-columns-wide------>|
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SoapDispatcherImpl.java,v 1.13 2003/11/15 00:34:24 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.*;

import org.gjt.xpp.XmlStartTag;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;

import soaprmi.*;
import soaprmi.port.*;
import soaprmi.mapping.*;
import soaprmi.server.RemoteRef;
import soaprmi.server.RemoteRefConverter;
import soaprmi.server.RemoteRefSerializer;
import soaprmi.soap.*;
import soaprmi.soapenc.SoapEnc;
import soaprmi.util.logging.Logger;

/**
 * Implementation of SOAP services.
 *
 * @version $Revision: 1.13 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public abstract class SoapDispatcherImpl implements SoapDispatcher {

    public SoapDispatcherImpl() {
        //dispMapping = new XmlJavaMapping();
        //dispMapping.connectTo(Soap.getDefault().getMapping());
    }

    public Endpoint createEndpoint(Port port,
                                   Object impl,
                                   XmlJavaMapping defaultMapping
                                  ) throws ServerException, AlreadyBoundException
    {
        if(port == null)
            throw new ServerException("can not create endpoint for null port");
        //if(port.getName() == null)
        //  throw new ServerException("port name can not be null "+port);
        if(impl == null)
            throw new ServerException(
                "object implementing port "+port+" can not be null");
        String epointLoc = getLocation() + "/" + port.getName();
        if(loc2Skel.get(epointLoc) != null)
            throw new AlreadyBoundException(
                "endpoint location "+epointLoc+" is already bound in local server");
        PortType portType = port.getPortType();
        XmlJavaPortTypeMap portTypeMap = null;

        try {
            portTypeMap =
                defaultMapping.queryPortType(
                portType.getUri(), portType.getName());
        } catch(XmlMapException ex) {
            throw new ServerException(
                "could not find java class for interface "
                    +" uri="+portType.getUri() +" name="+ portType.getName());
        }

        Endpoint epoint = new Endpoint();
        epoint.setLocation(epointLoc);
        Binding binding = new Binding();
        epoint.setBinding(binding);
        binding.setName(port.getName());
        //binding.setMethodNs(...) //frowm where tjkae this value????
        //loc2Object.put(epointLoc, impl);
        //loc2Port.put(epointLoc, port);
        SoapDynamicSkeleton skel = new SoapDynamicSkeleton(
            impl, port, portTypeMap, epoint, defaultMapping
        );
        //defaultSkel = skel;
        loc2Skel.put(epointLoc, skel);
        l.fine("added for loc="+epointLoc+" skeleton="+skel+" disptr="+this);
        //portName2Loc.put(port.getName(), epointLoc);
        //bindingName2Loc.put(binding.getName(), epointLoc);
        return epoint;
    }

    public void removeEndpoint(Endpoint epoint)
        throws ServerException, NotBoundException
    {
        String epointLoc = epoint.getLocation();
        SoapDynamicSkeleton skel = (SoapDynamicSkeleton) loc2Skel.get(epointLoc);
        if(skel == null)
            throw new NotBoundException(
                "location "+epointLoc+" not bound, cant remove endpoint "+epoint);
        Port port = skel.getPort();
        loc2Skel.remove(epointLoc);
        //Binding binding = epoint.getBinding();
        //portName2Loc.remove(port.getName());
        //bindingName2Loc.remove(binding.getName());
    }


    /**
     * Unpacks the SOAP packet from reader, assembles a call, executes it and
     * returns the serialized SOAP packet in the writer stream.
     */
    public boolean dispatch(Reader reader, Writer writer, Map bag)
        throws IOException
    {
        boolean success = false;

        Exception faultEx = null;
        try {
            DeserializeContext dctx = Soap.getDefault().getDeserializeContext();

            //TODO: dctx should have been in separate pool as it is modifying pool member
            //dctx.registerConverterFrom(Port.class, new RemoteRefConverter());
            EncodingStyle enc = SoapEnc.getDefault();
            synchronized(enc) {
                if(enc.queryInterfaceSerializer(RemoteRef.class) == null) {
                    Serializer remoteRefSerializer = new RemoteRefSerializer();
                    enc.registerInterfaceEncodingHandler(
                        RemoteRef.class, remoteRefSerializer, null);
                    enc.registerConverterFrom(Port.class, new RemoteRefConverter());
                }
            }
            dctx.setReader(reader);
            dctx.setDefaultEncodingStyle(SoapEnc.getDefault());
            XmlPullParser pp = dctx.getPullParser();
            pp.setAllowedMixedContent(false);
            if(pp.next() != XmlPullParser.START_TAG)
                throw new ServerException("envelope start tag expected"+pp.getPosDesc());
            XmlStartTag stag = dctx.getStartTag();
            pp.readStartTag(stag);
            if("Envelope".equals(stag.getLocalName()) == false)
                throw new ServerException("envelope start tag expected not "
                                              +stag.getLocalName()+pp.getPosDesc());
            if(Soap.SOAP_ENV_NS.equals(stag.getNamespaceUri()) == false) {
                throw new SoapVersionMismatchFault(
                    "unknonw envelope namespace: "+stag.getNamespaceUri()
                        +stag.getLocalName()+pp.getPosDesc());
            }

            //TODO  magic with guessing SOAP-ENV namespace here!
            // style.SOAP_ENV_NS = stag.getUri()
            // or mapping.createAlias(SOAP_ENV_NS, stag.getUri());
            if(pp.next() != XmlPullParser.START_TAG)
                throw new ServerException("envelope child start tag expected"+pp.getPosDesc());
            pp.readStartTag(stag);
            if("Header".equals(stag.getLocalName())) {

                //throw new ServerException("headers are not supported"+pp.getPosDesc());

                // skip headers...
                byte type = pp.next();
                while(type != XmlPullParser.END_TAG) {
                    if(type == XmlPullParser.START_TAG) {
                        //TODO check for must understand  and frow SOAPMustUnderstandFault
                        pp.readStartTag(stag);
                        String mustUnderstand = stag.getAttributeValueFromName(
                            Soap.SOAP_ENV_NS, "mustUnderstand");
                        String actor = stag.getAttributeValueFromName(
                            Soap.SOAP_ENV_NS, "actor");
                        if(actor == null ||
                           actor.equals("http://schemas.xmlsoap.org/soap/actor/next")) {
                            if("1".equals(mustUnderstand)) {
                                throw new SoapMustUnderstandFault(
                                    "does not understand header "+stag+pp.getPosDesc());
                            }
                        }
                        pp.skipNode();
                    }
                    type = pp.next();
                }
                if(pp.next() != XmlPullParser.START_TAG)
                    throw new ServerException("body start tag expected"+pp.getPosDesc());
                pp.readStartTag(stag);

            }
            if("Body".equals(stag.getLocalName()) == false)
                throw new ServerException("Body start tag expected not "
                                              +stag.getLocalName()+pp.getPosDesc());

            // process embedded RPC call from Body

            if(pp.next() != XmlPullParser.START_TAG)  // Method call in
                throw new ServerException("start tag expected"+pp.getPosDesc());

            pp.readStartTag(stag);

            success = dispatchToService(dctx, stag, writer, bag);

        } catch(SoapException ex) {
            faultEx = new SoapClientFault("can't process incoming message", ex);
        } catch(XmlPullParserException ex) {
            faultEx = new SoapClientFault("can't parse incoming message", ex);
        } catch(RemoteException ex) {
            faultEx = ex;
        }

        if(faultEx != null) {
            success = false;
            SoapSerializeContext sctx =
                Soap.getDefault().getSerializeContext();
            sctx.setDefaultEncodingStyle(SoapEnc.getDefault());
            sctx.setSoapStyle(SoapStyle.getDefaultSoapStyle()); //TODO guess style
            try {
                sctx.setWriter(writer);
                Throwable detail = faultEx;
                if(faultEx instanceof RemoteException) { // unwrap detail
                    RemoteException remoteEx = (RemoteException) faultEx;
                    detail = remoteEx.getNested();
                    if(detail == null) {
                        detail = faultEx; //detail can be null than undo unwrap
                    }
                }
                String faultcode = "Client";
                if(faultEx instanceof SoapFault) { // unwrap detail
                    faultcode = ((SoapFault)faultEx).getFaultcode();
                }
                SU.writeFault(sctx, null, faultcode, detail);
            } catch(SerializeException sex) {
                l.severe("exception when serializing fault response - "
                             +"should never happen!", sex);
                //ex.printStackTrace();
            } finally {
                try {
                    if(sctx != null) {
                        sctx.close();
                    }
                } catch(SerializeException ex) {
                }
            }
        }
        return success;
    }

    public boolean dispatchToService(DeserializeContext dctx,
                                     XmlStartTag stag,
                                     Writer writer,
                                     Map bag)
        throws IOException, DeserializeException, ServerException
    {
        String methodNs = stag.getNamespaceUri();

        String path = (String) bag.get("provides.path");
        if(path == null) path = "";
        String loc = getLocation() + path;
        SoapDynamicSkeleton skel = (SoapDynamicSkeleton) loc2Skel.get(loc);
        //TODO if(skel == null) loc = (String) bindingName2Loc.get(methodNs);
        if(skel == null) {
            if(loc2Skel.size() == 0)
                throw new ServerException("no remote object registered");
            if(loc2Skel.size() > 1)
                throw new ServerException(
                    "more than one remote object registered - could not choose default");
            Iterator iter = loc2Skel.values().iterator();
            skel = (SoapDynamicSkeleton) iter.next(); //defaultSkel;
        }
        // try {
        // XSoapServerConnectionInterceptorGlobal.acceptCall(methodNs, providesPort, ...)
        // } catch(NonAuthorizedUserException ex) {
        //
        //
        return skel.dispatch(dctx, stag, writer);
    }

    public String getLocation() throws ServerException {
        return location;
    }

    //public void setLocation(String location_) {
    //  location = location_;
    //}

    public Object findObject(Port port) {
        //Binding binding = port.getEndpoint().getBinding();
        //String bindingName = binding.getName();
        //String loc = (String) bindingName2Loc.get(bindingName);
        String loc = port.getEndpoint().getLocation();
        if(loc == null) return null;
        SoapDynamicSkeleton skel = (SoapDynamicSkeleton) loc2Skel.get(loc);
        if(skel == null) return null;
        return skel.getTarget();
    }

    /*
     public Port findPort(String portName) {
     String loc = (String) portName2Loc.get(portName);
     if(loc == null) return null;
     SoapDynamicSkeleton skel = (SoapDynamicSkeleton) loc2Skel.get(loc);
     if(skel == null) return null;
     return skel.getPort();
     }
     */

    //private final static boolean TRACING = true;
    private Map loc2Skel = new HashMap();
    //private Map portName2Loc = new HashMap();
    //private Map bindingName2Loc = new HashMap();
    //private SoapDynamicSkeleton defaultSkel;

    //private static final SoapDispatcher instance = new SoapDispatcher();
    protected String location;
    protected Map skels = new HashMap();
    private Logger l = Logger.getLogger();
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

