/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: MethodDispatcher.java,v 1.10 2003/11/15 00:34:24 aslom Exp $
 */

package soaprmi.soaprpc;

import soaprmi.soap.*;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlStartTag;
import soaprmi.RemoteException;
import soaprmi.ServerException;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlJavaMessageMap;
import soaprmi.mapping.XmlJavaOperationMap;
import soaprmi.mapping.XmlJavaPartMap;
import soaprmi.mapping.XmlMapException;
import soaprmi.port.Port;
import soaprmi.soapenc.SoapEnc;
import soaprmi.util.logging.Logger;


/**
 * All that is necessary to execute SOAP RPC call to specified
 *   local method.
 *
 * @version $Revision: 1.10 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class MethodDispatcher
{
    private static Logger l = Logger.getLogger();
    //private final static boolean TRACING
    //    = false || SoapServices.TRACE_DISPATCH;
    private final static Logger TRACING = SoapServices.TRACE_EXECUTION;

    private Object target;
    private XmlJavaMapping mapping;
    private SoapSerializeContext sctx;

    private String methodNs;
    private String methodRequestName;
    private String methodResponseName;


    private Method method;
    private Class[] parameterTypes;
    private Class returnType;
    private Object[] params;
    private String returnName;

    protected MethodDispatcher() {
    }

    public static MethodDispatcher createMethodDispatcher(
        Object target_,
        Port port_,
        XmlJavaOperationMap oprtn_,
        XmlJavaMapping mapping_
    ) throws ServerException
    {
        MethodDispatcher md = new MethodDispatcher();

        md.target = target_;
        md.mapping = mapping_;

        //md.methodNs = methodNs_;
        //md.methodRequestName = oprtn_.getRequestName();
        XmlJavaMessageMap request = oprtn_.getRequest();
        md.methodRequestName = request.getMessageName();
        XmlJavaMessageMap response = oprtn_.getResponse();
        //md.methodResponseName = oprtn_.getResponseName();
        md.methodResponseName = response.getMessageName();

        //ALEK
        XmlJavaPartMap[] requestParts = request.getParts();
        md.parameterTypes = new Class[requestParts.length]; //md.method.getParameterTypes();
        for (int i = 0; i < requestParts.length; i++)
        {
            try {
                md.parameterTypes[i] = requestParts[i].javaClass();
            } catch(XmlMapException ex) {
                throw new ServerException(
                    "could not determine parameter "+(i+1)+" type for "+requestParts[1]
                        +" for "+md.methodRequestName+" in "+port_);
            }
        }
        md.params = new Object[md.parameterTypes.length];

        XmlJavaPartMap[] responseParts = response.getParts();
        if(responseParts.length == 0) {
            md.returnType = Void.TYPE; //md.method.getReturnType();
            md.returnName = "return";
        } else if(responseParts.length == 1) {
            try {
                md.returnType = responseParts[0].javaClass();
                md.returnName = responseParts[0].getPartName();
            } catch(XmlMapException ex) {
                throw new ServerException(
                    "could not determine return class type for "+responseParts[0]);
            }
        } else {
            throw new ServerException("mapped output message has more than one part "+response);
        }

        md.sctx = Soap.getDefault().createSerializeContext();
        md.sctx.setDefaultEncodingStyle(SoapEnc.getDefault());
        md.sctx.setSoapStyle(SoapStyle.getDefaultSoapStyle());
        md.sctx.setMapping(mapping_);

        md.method = oprtn_.javaMethod();

        return md;
    }

    /**
     * This a key method for executing SOAP requests.
     */
    public boolean dispatch(DeserializeContext dctx, XmlStartTag stag,
                            Writer writer)
        throws IOException, DeserializeException
    {
        Exception ex = null;
        Object result = null;
        dctx.setMapping(mapping); //TODO: should it be undo????
        try {
            receiveRequest(dctx, stag);
        } catch(DeserializeException dex) {
            ex = new SoapClientFault(
                "unexpected error in processing incoming request", dex);
        } catch(Error er) {
            ex = new SoapClientFault(
                "unexpected error in processing incoming request", er);
        } catch(Throwable th) {
            ex = new SoapClientFault(
                "unexpected exception in processing incoming request", th);
        } finally {
            // make absolutely sure that context _is_ closed and returned to pool
            // closing context also will fixuo() all dangling refernces
            try {
                if(dctx != null) {
                    dctx.close();
                }
            } catch(DeserializeException dex) {
            }
        }
        if(ex == null) {
            try {
                if(TRACING.isFineEnabled()) {
                    l.fine("TRACE invoking method "+method.getName()+
                               " on target "+target.getClass());
                    if(params.length > 0) {
                        for(int i = 0; i < params.length; ++i) {
                            l.finer(
                                "TRACE params["+i+"]="+params[i]+" "
                                    +(params[i] != null ? params[i].getClass().getName() : "")
                            );
                        }
                    } else {
                        l.finer("TRACE no parameters");
                    }
                }
                result = method.invoke(target, params);
                if(TRACING.isFineEnabled())
                    TRACING.fine("TRACE "+method.getName()+" result='"+result+"'");
            } catch(InvocationTargetException itex) {
                ex = new SoapServerFault(
                    "exception in processing incoming request",
                    itex.getTargetException());
            } catch(Error er) {
                ex = new SoapServerFault(
                    "unexpected error in processing incoming request", er);
            } catch(Throwable th) {
                ex = new SoapServerFault(
                    "unexpected exception in processing incoming request", th);
            }
        }
        return sendResponseOrException(writer, result, ex);
    }

    public String getMethodRequestName() {
        return methodRequestName;
    }

    public void receiveRequest(DeserializeContext dctx, XmlStartTag stag)
        throws IOException, RemoteException,
        DeserializeException, XmlPullParserException
    {
        methodNs = stag.getNamespaceUri();

        XmlPullParser pp = dctx.getPullParser();
        EncodingStyle enc = dctx.getDefaultEncodingStyle();

        dctx.enterStruct();

        //Parameter can be href - resuse Object[] dctx fixup
        for(int i = 0; i < params.length; ++i) {
            //params[i] = dctx.readObject(parameterTypes[i]);
            pp.next();
            pp.readStartTag(stag);
            // this is basically the same code as in ArrayDeserializer...
            // but it may be chnaged to dela with parameter names ?!! :-(
            //enc.readObject(dctx, pp, stag, );
            String href = stag.getAttributeValueFromRawName("href");
            if(href != null) {
                String hid = href.substring(1); // skip # int '#id1'
                if(dctx.hasId(hid)) {
                    //TODO check if assignment is valid???
                    params[i] = dctx.getIdValue(hid);
                } else {
                    dctx.addArrayFixup(params, i, parameterTypes[i], hid);
                }
                if(pp.next() != XmlPullParser.END_TAG) //skip END_TAG
                    throw new DeserializeException(
                        "expected immediate end tag for href element"+pp.getPosDesc());
            } else {
                params[i] =
                    enc.readObject(dctx, parameterTypes[i], pp, stag);
            }
        }

        dctx.leaveStruct();

        int type = pp.next();
        if(params.length == 0) {
            // skip possibly emty content
            if(type == XmlPullParser.CONTENT) {
                if(!pp.isWhitespaceContent())
                    throw new DeserializeException(
                        "calls with no parameters can not have text content"
                            +pp.getPosDesc());
                type = pp.next();
            }
        }
        if(type != XmlPullParser.END_TAG)  // Method call out
            throw new RemoteException("end tag expected"+pp.getPosDesc());

        // close input

        type = dctx.done(); // read any remining serialized xml

        if(type != XmlPullParser.END_TAG)  // Body out
            throw new RemoteException("end tag expected"+pp.getPosDesc());
        if(pp.next() != XmlPullParser.END_TAG)  // Envelope out
            throw new RemoteException("end tag expected"+pp.getPosDesc());

    }

    /**
     * If sending response or fault fails system *MUST* propagate
     * exception upstream so rts will shutdown socket indicating to client
     * that something goes wrong. If sendResponse was buffered it would
     * be possible to send nice fault if sending response failed but
     * still nothing could be done if sending fault throws exception....
     */
    public boolean sendResponseOrException(Writer writer_, Object result,
                                           Exception ex) throws IOException
    {
        boolean success = false;
        try {
            sctx.setWriter(writer_);
            if(ex == null) {
                sendResponse(result);
            } else {
                success = false;
                sendFault(null, ex);
            }
        } catch(SoapException sex) {
            //sex.printStackTrace(); ///refine to have nice server side logging...
            l.severe("exception in sending response", sex);
            throw new IOException("exception in sending response: " + sex);
        } finally {
            try {
                success = false;
                sctx.close();
            } catch(SerializeException sex) {}
        }
        return success;
    }

    protected void sendResponse(Object result)
        throws IOException, SerializeException
    {

        if(TRACING.isFineEnabled()) {
            TRACING.fine("TRACE sending response result='"+result+"'");
        }
        SU.writeEnvelopeStart(sctx);
        SU.writeBodyStart(sctx);

        // write method sigs
        Writer writer = sctx.getWriter();
        //NOTE: unfortunately XML mandates that element <a> has namespace ''
        // therefore it is equalavlent to <a xmlns=''> or <m:a xmlns:m=''>
        // but some SOAP implementation do not treat this as equivalent...
        if(methodNs != null && methodNs.length() > 0) {
            writer.write("<m:");
            writer.write(methodResponseName);
            writer.write(" xmlns:m='");
            writer.write(methodNs);
            writer.write("'>\n");
        } else {
            writer.write('<');
            writer.write(methodResponseName);
            writer.write(">\n");
        }

        if(returnType != Void.TYPE) { //result != null) {
            sctx.enterStruct();
            sctx.writeObject(result, returnName, returnType);
            sctx.leaveStruct();
        }

        if(methodNs != null && methodNs.length() > 0) {
            writer.write("</m:");
            writer.write(methodResponseName);
            writer.write(">\n");
        } else {
            writer.write("</");
            writer.write(methodResponseName);
            writer.write(">\n");
        }

        // when closing serializer do marshal any remainig parameters
        // (in multi-ref soap style)

        sctx.done();

        SU.writeBodyEnd(sctx);
        SU.writeEnvelopeEnd(sctx);

        sctx.close();
    }

    private void sendFault(String faultcode, Exception ex)
        throws IOException, SerializeException
    {
        Throwable stacktrace = ex;
        if(ex instanceof RemoteException) { // unwrap detail
            RemoteException remoteEx = (RemoteException) ex;
            stacktrace = remoteEx.getNested();
            if(stacktrace == null) {
                stacktrace = ex; //detail can be null than undo unwrap
            }
        }
        if(faultcode == null && ex instanceof SoapFault) { // unwrap detail
            faultcode = ((SoapFault)ex).getFaultcode();
        }
        if(TRACING.isFineEnabled()) {
            l.fine("TRACE sending exception detail", stacktrace);
            //detail.printStackTrace();
        }
        SU.writeFault(sctx, null, faultcode, stacktrace);
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

