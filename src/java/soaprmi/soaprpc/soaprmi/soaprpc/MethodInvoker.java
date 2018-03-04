/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: MethodInvoker.java,v 1.19 2004/05/06 18:18:48 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import org.gjt.xpp.XmlEndTag;
import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlPullParserFactory;
import org.gjt.xpp.XmlStartTag;
import soaprmi.RemoteException;
import soaprmi.ServerException;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlJavaMessageMap;
import soaprmi.mapping.XmlJavaOperationMap;
import soaprmi.mapping.XmlJavaPartMap;
import soaprmi.mapping.XmlJavaPortTypeMap;
import soaprmi.port.Binding;
import soaprmi.port.Endpoint;
import soaprmi.port.Port;
import soaprmi.port.PortType;
import soaprmi.port.SoapBinding;
import soaprmi.server.RemoteRef;
import soaprmi.server.RemoteRefConverter;
import soaprmi.server.RemoteRefSerializer;
import soaprmi.soap.DeserializeException;
import soaprmi.soap.EncodingStyle;
import soaprmi.soap.SerializeException;
import soaprmi.soap.Serializer;
import soaprmi.soap.Soap;
import soaprmi.soap.SoapDeserializeContext;
import soaprmi.soap.SoapException;
import soaprmi.soap.SoapSerializeContext;
import soaprmi.soap.SoapStyle;
import soaprmi.soapenc.SoapEnc;
import soaprmi.util.Check;
import soaprmi.util.Util;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;


/**
 * All that is necessary to execute SOAP RPC call to specified remote method.
 *
 * @version $Revision: 1.19 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class MethodInvoker
{
    private Logger l = Logger.getLogger();
    private final static Logger TRACE_SENDING =
        SoapServices.TRACE_INVOKE_OUT;
    private final static Logger TRACE_RECEIVING =
        SoapServices.TRACE_INVOKE_IN;
    
    private static     XmlPullParserFactory factory;
    
    static {
        factory = Util.getPullParserFactory();
//        try {
//          factory = XmlPullParserFactory.newInstance();
//        } catch(Exception ex) {
//          throw new RuntimeException("could nto create XPP factory to be used in method invoker", ex);
//        }
    }
    
    private SoapSerializeContext sctx;
    private SoapDeserializeContext dctx;
    
    private String soapAction;
    private String methodNs;
    private String methodRequestName;
    private String[] parameterNames;
    
    //private Method method;
    private Class[] parameterTypes;
    private Class returnType;
    
    private Writer traceWriter;
    private Writer traceOrigWriter;
    private Reader traceOrigReader;
    
    protected MethodInvoker () throws RemoteException {
        sctx = Soap.getDefault().createSerializeContext();
        sctx.setDefaultEncodingStyle(SoapEnc.getDefault());
        sctx.setSoapStyle(SoapStyle.getDefaultSoapStyle());
        try {
            dctx = Soap.getDefault().createDeserializeContext();
            dctx.setDefaultEncodingStyle(SoapEnc.getDefault());
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
        } catch(SoapException ex) {
            throw new RemoteException("can't create call deserialzier", ex);
        }
    }
    
    //  public Method getMethod() {
    //    return method;
    //  }
    
    public String getMethodRequestName() {
        return methodRequestName;
    }
    
    public String getSoapAction() {
        return soapAction;
    }
    
    public void setMapping(XmlJavaMapping mapping) {
        sctx.setMapping(mapping);
        dctx.setMapping(mapping);
    }
    
    public static MethodInvoker makeMethodInvoker(
        Port port,
        Endpoint epoint,
        Method m,
        XmlJavaMapping mapping
    ) throws RemoteException
    {
        // get what port type it is ...
        PortType portType = port.getPortType();
        String portTypeName = portType.getName();
        String portTypeUri = portType.getUri();
        
        Class iface = m.getDeclaringClass();
        //
        XmlJavaPortTypeMap portTypeMap;
        XmlJavaOperationMap opMap;
        try {
            portTypeMap = mapping.queryPortType(iface);
            opMap = portTypeMap.queryMethodRequest(m.getName());
        } catch(soaprmi.mapping.XmlMapException ex) {
            throw new RemoteException("can't determine xml type for java type "
                                          +iface, ex);
        }
        
        Check.assertion(opMap != null);
        
        XmlJavaMessageMap request = opMap.getRequest();
        
        //String methodRequestName =  m.getName();
        String methodRequestName =  request.getMessageName();
        XmlJavaPartMap[] requestParts = request.getParts();
        
        Class[] parameterTypes = new Class[requestParts.length]; //m.getParameterTypes();
        String[] parameterNames = new String[parameterTypes.length];
        
        if(parameterTypes.length > 0) {
            
            for(int i = 0; i < requestParts.length; ++i) {
                try {
                    parameterTypes[i] = requestParts[i].javaClass();
                    parameterNames[i] = requestParts[i].getPartName();
                } catch(soaprmi.mapping.XmlMapException ex) {
                    throw new RemoteException(
                        "can't determine class for java parameter"+i
                            +" in method "+m+" in interface "+iface, ex);
                }
            }
        }
        
        
        XmlJavaMessageMap response = opMap.getResponse();
        XmlJavaPartMap[] responseParts = response.getParts();
        Class returnType;
        if(responseParts.length == 0) {
            returnType = Void.TYPE;
        } else if(responseParts.length == 1){
            try {
                returnType = responseParts[0].javaClass();
            } catch(soaprmi.mapping.XmlMapException ex) {
                throw new RemoteException(
                    "can't determine class for java return type "
                        +" from method "+m+" in interface "+iface, ex);
            }
            
        } else {
            throw new RemoteException("response can at most one part");
        }
        
        String soapAction = "\"\"";
        String methodNs = null;
        Binding binding = epoint.getBinding();
        
        if(binding != null) {
            methodNs = binding.getName();
            if(binding instanceof SoapBinding) {
                SoapBinding sb = (SoapBinding) binding;
                if(sb.getSoapAction() != null) {
                    soapAction =
                        "\""+sb.getSoapAction()  +"\"";
                }
                //+ "#" + mi.methodRequestName
                //TODO retrieve and use SoapStyle
            } else {
                soapAction =
                    "\""+ methodNs + "#" + methodRequestName +"\"";
            }
            
        }
        
        return makeMethodInvoker(
            methodNs,
            returnType,
            methodRequestName,
            parameterTypes,
            parameterNames,
            soapAction,
            mapping);
        
    }
    
    public static MethodInvoker makeMethodInvoker(
        String methodNs,
        Class returnType,
        String methodRequestName,
        Class[] parameterTypes,
        String[] parameterNames,
        String soapAction,
        XmlJavaMapping mapping
    ) throws RemoteException {
        
        MethodInvoker mi = new MethodInvoker();
        mi.methodNs = methodNs;
        if(returnType == null) {
            throw new  RemoteException(
                "return type can not be null (use Void.TYPE to have void return instead)");
        }
        mi.returnType = returnType;
        mi.methodRequestName = methodRequestName;
        mi.parameterTypes = parameterTypes;
        mi.parameterNames = parameterNames;
        mi.soapAction = soapAction;
        mi.setMapping(mapping);
        return mi;
    }
    
    
    public Object invoke(Object[] params, Writer writer, Reader reader)
        throws Throwable
    {
        sendRequest(params, writer);
        return receiveResponse(reader);
    }
    
    public void sendRequest(Object[] params, Writer writer_)
        throws IOException, RemoteException, SerializeException
    {
        setWriter(writer_);
        if(params == null)
            new IllegalArgumentException("cant invoke on null parameters");
        
        if((params == null && parameterTypes.length != 0)
               || (params != null &&  parameterTypes.length != params.length))
            new SoapException("wrong number of parameters");
        // --- Request
        
        if(Log.ON && l.isFinestEnabled()) {
            //l.finest("TRACE invoke prepares request");
            l.finest("invoking remote method "+getMethodRequestName());
            for(int i = 0; params != null && i < params.length; ++i) {
                l.finest("method "+getMethodRequestName()+" params["+i+"]="+params[i]);
            }
        }
        // retrieve defaults (soapAction, soapStyle, encodingStyle)
        
        SU.writeEnvelopeStart(sctx);
        SU.writeBodyStart(sctx);
        
        // write method sigs
        Writer writer = sctx.getWriter();
        if(methodNs != null && methodNs.length() > 0) {
            writer.write("<m:");
            writer.write(methodRequestName);
            writer.write(" xmlns:m='");
            writer.write(methodNs);
            writer.write("'>\n");
        } else {
            writer.write('<');
            writer.write(methodRequestName);
            writer.write(">\n");
        }
        
        sctx.enterStruct();
        //...
        
        if(params != null) {
            for(int i = 0; i < params.length; ++i) {
                Object o = params[i];
                sctx.writeObject(o, parameterNames[i], parameterTypes[i]);
            }
        }
        sctx.leaveStruct();
        
        if(methodNs != null && methodNs.length() > 0) {
            writer.write("</m:");
        } else {
            writer.write("</");
        }
        writer.write(methodRequestName);
        writer.write(">\n");
        
        // when closing serializer do marshal
        // any remainig parameters (in multi-ref soap style)
        sctx.done();
        
        SU.writeBodyEnd(sctx);
        SU.writeEnvelopeEnd(sctx);
        
        // one side finished -- now server do munching
        sctx.close();
        
        
        if(Log.ON && TRACE_SENDING.isFinestEnabled()) {
            String s = traceWriter.toString();
            TRACE_SENDING.finest("TRACE sending len="+s.length()
                                     +":---\n"+s+"---\n");
            traceOrigWriter.write(s);
            traceOrigWriter.flush();
            //traceOrigWriter.close(); //ALEK
        }
    }
    
    public Object receiveResponse(Reader reader_)
        throws IOException, RemoteException, SoapException, XmlPullParserException
    {
        setReader(reader_);
        if(Log.ON && TRACE_RECEIVING.isFinestEnabled()) {
            TRACE_RECEIVING.finest("TRACE reading response to buffer");
            char[] readBuf = new char[512];
            StringWriter sink = new StringWriter();
            int i;
            while((i = traceOrigReader.read(readBuf)) > 0) {
                //System.err.println("received:"+new String(readBuf, 0, i));
                sink.write(readBuf, 0 ,i);
            }
            //traceOrigReader.close(); //ALEK
            String s = sink.toString();
            TRACE_RECEIVING.finest("TRACE received: len="+s.length()+"\n"
                                       +"---\n"+s+"---\n");
            StringReader stringReader = new StringReader(s);
            dctx.setReader(stringReader);
        }
        
        // read envelope
        XmlPullParser pp = dctx.getPullParser();
        if(pp.next() != XmlPullParser.START_TAG)
            throw new RemoteException("envelope start tag expected"+pp.getPosDesc());
        XmlStartTag stag = dctx.getStartTag();
        pp.readStartTag(stag);
        if("Envelope".equals(stag.getLocalName()) == false)
            throw new RemoteException("Envelope start tag expected not "
                                          +stag.getLocalName()+pp.getPosDesc());
        
        //TODO  magic with guessing SOAP-ENV namespace here!
        // style.SOAP_ENV_NS = stag.getUri()
        // or mapping.createAlias(SOAP_ENV_NS, stag.getUri());
        if(pp.next() != XmlPullParser.START_TAG)
            throw new RemoteException(
                "envelope child start tag expected"+pp.getPosDesc());
        pp.readStartTag(stag);
        if("Header".equals(stag.getLocalName())) {
            //throw new RemoteException("headers are not supported"+pp.getPosDesc());
            
            // skip headers...
            byte type = pp.next();
            while(type != XmlPullParser.END_TAG) {
                if(type == XmlPullParser.START_TAG) {
                    pp.skipNode();
                }
                type = pp.next();
            }
            if(pp.next() != XmlPullParser.START_TAG)
                throw new RemoteException("body start tag expected"+pp.getPosDesc());
            pp.readStartTag(stag);
        }
        if("Body".equals(stag.getLocalName()) == false)
            throw new RemoteException("Body start tag expected not "
                                          +stag.getLocalName()+pp.getPosDesc());
        
        // process embedded RPC call from Body
        
        if(pp.next() != XmlPullParser.START_TAG)
            throw new RemoteException("method start tag expected"+pp.getPosDesc());
        pp.readStartTag(stag);
        
        ServerException ex = null;
        Object[] result = new Object[]{null};
        if("Fault".equals(stag.getLocalName())) {
            // unwrap Fault element
            String faultcodeUri = null;
            String faultcodeLocalName = null;
            String faultcode = "UNDETERMINED";
            String faultstring = "unknown fault - remote method execution failed";
            String detail = null;
            XmlNode wsdlFault = null;
            
            while(true) {
                byte type = pp.next();
                if(type == XmlPullParser.END_TAG)
                    break;
                pp.readStartTag(stag);
                if("faultcode".equals(stag.getLocalName())) {
                    if(pp.next() != XmlPullParser.CONTENT)
                        throw new RemoteException("content expected"+pp.getPosDesc());
                    faultcode = pp.readContent();
                    faultcodeUri = pp.getQNameUri(faultcode);
                    faultcodeLocalName = pp.getQNameLocal(faultcode);
                    if(pp.next() != XmlPullParser.END_TAG)
                        throw new RemoteException("end tag expected"+pp.getPosDesc());
                    
                } else if("faultstring".equals(stag.getLocalName())) {
                    int eventType = pp.next();
                    if(eventType != XmlPullParser.CONTENT) {
                        faultstring = "";
                        if(eventType != XmlPullParser.END_TAG)
                            throw new RemoteException("end tag expected"+pp.getPosDesc());
                    }
                    else {
                        faultstring = pp.readContent();
                        if(pp.next() != XmlPullParser.END_TAG)
                            throw new RemoteException("end tag expected"+pp.getPosDesc());
                    }
                } else if("detail".equals(stag.getLocalName())) {
                    //try to deserialize detail to get more exception details
                    type = pp.next();
                    boolean firstChild = true;
                    while(type != XmlPullParser.END_TAG) {
                        if(type == XmlPullParser.START_TAG) {
                            pp.readStartTag(stag);
                            if("message".equals(stag.getLocalName())
                                   || "exceptionName".equals(stag.getLocalName()) )
                            {
                                if(pp.next() != XmlPullParser.CONTENT)
                                    throw new RemoteException("content expected"+pp.getPosDesc());
                                if(detail == null) detail = "";
                                detail += pp.readContent() + "\n\n";
                                pp.next();
                            } else if("stackTrace".equals(stag.getLocalName())
                                          || "stacktrace".equals(stag.getLocalName())) //GLUE preferred
                            {
                                if(pp.next() != XmlPullParser.CONTENT)
                                    throw new RemoteException("content expected"+pp.getPosDesc());
                                if(detail == null) detail = "";
                                detail += pp.readContent() + "\n\n";
                                pp.next();
                            } else {
                                if(firstChild) {
                                    wsdlFault = factory.newNode(pp);
                                } else {
                                    pp.skipNode();
                                }
                            }
                        }
                        //} else {
                        if(type == XmlPullParser.CONTENT && detail == null) {
                            detail = pp.readContent();
                        }
                        firstChild = false;
                        type = pp.next();
                    }
                } else {
                    // skip any other unknown possible extensions of Fault
                    pp.skipNode();
                }
            }
            //System.err.println("detail="+detail);
            ex = new ServerException(faultcodeUri, faultcodeLocalName, faultstring,
                                     faultcode + ": " + faultstring,
                                         ((detail != null) ? detail : ""),
                                     wsdlFault
                                    );
        } else {
            // retrieve result
            if(returnType != Void.TYPE) {
                if(pp.next() != XmlPullParser.START_TAG)
                    throw new DeserializeException("expected start tag"+pp.getPosDesc());
                pp.readStartTag(stag);
                //TODO: stag.getValue(style.SOAP_ENV_NS, "encodingStyle") ....
                String href = stag.getAttributeValueFromRawName("href");
                if(href != null) {
                    //throw new DeserializeException("href not allowed in root response element");
                    String hid = href.substring(1); // skip # int '#id1'
                    if(dctx.hasId(hid)) {
                        result[0] = dctx.getIdValue(hid);
                    } else {
                        dctx.addArrayFixup(result, 0, returnType, hid);
                    }
                    if(pp.next() != XmlPullParser.END_TAG) //skip END_TAG
                        throw new DeserializeException(
                            "expected immediate end tag for href element"+pp.getPosDesc());
                } else {
                    try {
                        //result[0] = dctx.readObject(returnType);
                        result[0] =
                            dctx.getDefaultEncodingStyle().readObject(dctx, returnType, pp, stag);
                    } catch(XmlPullParserException xpe) {
                        throw new DeserializeException(
                            "xml parsing exception when deserializing return value", xpe);
                    }
                    
                }
            }
            int type = pp.next();
            
            if(returnType == Void.TYPE) {
                // skip possibly emty content
                if(type == XmlPullParser.CONTENT) {
                    if(!pp.isWhitespaceContent())
                        throw new DeserializeException(
                            "call with no return value can not have text content"
                                +pp.getPosDesc());
                    type = pp.next();
                }
            }
            if(type != XmlPullParser.END_TAG)
                throw new RemoteException("method end tag expected"+pp.getPosDesc());
        }
        
        int type = dctx.done(); // read any remining serialized xml
        
        if(type != XmlPullParser.END_TAG)  // Body out
            throw new RemoteException("end tag expected"+pp.getPosDesc());
        if(pp.next() != XmlPullParser.END_TAG)  // Envelope out
            throw new RemoteException("end tag expected"+pp.getPosDesc());
        XmlEndTag etag = dctx.getEndTag();
        pp.readEndTag(etag);
        if("Envelope".equals(etag.getLocalName()) == false)
            throw new RemoteException("Envelope end tag expected not "
                                          +etag.getLocalName()+pp.getPosDesc());
        
        dctx.close();
        
        if(ex != null) {
            if(Log.ON) l.finest("method "+getMethodRequestName()+" ex="+ex);
            throw ex;
        }
        
        if(Log.ON) l.finest("method "+getMethodRequestName()+" result="+result[0]);
        return result[0];
    }
    
    protected void setWriter(Writer writer_) throws RemoteException {
        try {
            if(Log.ON && TRACE_SENDING.isFinestEnabled()) {
                traceOrigWriter = writer_;
                writer_ = traceWriter = new StringWriter();
            }
            sctx.setWriter(writer_);
        } catch(SoapException ex) {
            throw new RemoteException("can't set call writer", ex);
        }
    }
    
    protected void setReader(Reader reader_)  throws RemoteException {
        try {
            if(Log.ON && TRACE_RECEIVING.isFinestEnabled()) {
                traceOrigReader = reader_;
            } else {
                dctx.setReader(reader_);
            }
        } catch(SoapException ex) {
            throw new RemoteException("can't set call reader", ex);
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

