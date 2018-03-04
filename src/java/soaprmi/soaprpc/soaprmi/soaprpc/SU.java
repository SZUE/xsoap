/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SU.java,v 1.6 2004/05/06 18:18:48 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlPullParserFactory;
import org.gjt.xpp.XmlRecorder;
import soaprmi.RemoteException;
import soaprmi.soap.SerializeContext;
import soaprmi.soap.SerializeException;
import soaprmi.soap.SoapStyle;
import soaprmi.util.Util;

/**
 * SOAP Utility methods.
 *
 * @version $Revision: 1.6 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class SU {
    private static final XmlPullParserFactory factory;
    
    static {
        factory = Util.getPullParserFactory();
//        try {
//            factory = XmlPullParserFactory.newInstance();
//        } catch (XmlPullParserException e) {
//            throw new RuntimeException("could not load XPP2 facotry"+e, e);
//        }
    }
    
    // SOAP RPC specific...
    
    public static void writeEnvelopeStart(SerializeContext sctx) throws SerializeException, IOException {
        Writer writer = sctx.getWriter();
        writer.write('<');
        SoapStyle style = sctx.getSoapStyle();
        writer.write(style.SOAP_ENV_NS_PREFIX);
        writer.write(":Envelope");
        sctx.writeNamespaces();
        writer.write(" ");
        writer.write(style.SOAP_ENV_NS_PREFIX);
        writer.write(":encodingStyle='");
        writer.write(style.SOAP_ENC_NS);
        writer.write("'");
        writer.write(">\n");
    }
    
    public static void writeEnvelopeEnd(SerializeContext sctx) throws SerializeException, IOException {
        Writer writer = sctx.getWriter();
        writer.write("</");
        SoapStyle style = sctx.getSoapStyle();
        writer.write(style.SOAP_ENV_NS_PREFIX);
        writer.write(":Envelope>\n");
    }
    
    public static void writeBodyStart(SerializeContext sctx) throws SerializeException, IOException {
        Writer writer = sctx.getWriter();
        writer.write('<');
        SoapStyle style = sctx.getSoapStyle();
        writer.write(style.SOAP_ENV_NS_PREFIX);
        writer.write(":Body>\n");
    }
    
    public static void writeBodyEnd(SerializeContext sctx) throws SerializeException, IOException {
        Writer writer = sctx.getWriter();
        writer.write("</");
        SoapStyle style = sctx.getSoapStyle();
        writer.write(style.SOAP_ENV_NS_PREFIX);
        writer.write(":Body>\n");
    }
    
    public static void writeFaultStart(SerializeContext sctx) throws SerializeException, IOException {
        Writer writer = sctx.getWriter();
        writer.write('<');
        SoapStyle style = sctx.getSoapStyle();
        writer.write(style.SOAP_ENV_NS_PREFIX);
        writer.write(":Fault>\n");
    }
    
    public static void writeFaultEnd(SerializeContext sctx) throws SerializeException, IOException {
        Writer writer = sctx.getWriter();
        writer.write("</");
        SoapStyle style = sctx.getSoapStyle();
        writer.write(style.SOAP_ENV_NS_PREFIX);
        writer.write(":Fault>\n");
    }
    
    public static void writeFaultCode(SerializeContext sctx,
                                      String uri, String faultcode)
        throws SerializeException, IOException
    {
        //if(faultcode == null) throw new IllegalArgumentException
        Writer writer = sctx.getWriter();
        writer.write("<faultcode>");
        SoapStyle style = sctx.getSoapStyle();
        writer.write(uri == null || uri.equals(style.SOAP_ENV_NS) ?
                         style.SOAP_ENV_NS_PREFIX : uri);
        writer.write(':');
        Util.writeXMLEscapedString(writer, faultcode);
        writer.write("</faultcode>\n");
    }
    
    public static void writeFaultString(SerializeContext sctx, String faultstring)
        throws SerializeException, IOException
    {
        Writer writer = sctx.getWriter();
        writer.write("<faultstring>");
        Util.writeXMLEscapedString(writer, faultstring);
        writer.write("</faultstring>\n");
    }
    
    
    private static void writeWsdlFault(SerializeContext sctx, XmlNode wsdlFault)
        throws SerializeException, IOException
    {
        try {
            Writer out = sctx.getWriter();
            XmlRecorder recorder = factory.newRecorder();
            recorder.setOutput(out);
            recorder.writeNode(wsdlFault);
            out.flush();
        } catch (XmlPullParserException e) {
            throw new SerializeException("could not serialize fault xml node "+wsdlFault, e);
        }
    }
    
    // XSOAP format: <detail><stackTrace>...
    // AXIS compatible exception packaging ... children of detail:
    // first child is Fault defined in WSDL (if any ...)
    // second child:
    // <ns4:exceptionName xmlns:ns4="http://xml.apache.org/axis/">org.gridforum.ogsi.TargetInvalidFaultType</ns4:exceptionName>
    // third child:
    // <ns5:stackTrace xmlns:ns5="http://xml.apache.org/axis/">...
    
    private static void writeFaultDetail(SerializeContext sctx,
                                         XmlNode wsdlFault,
                                         String exceptionName,
                                         String stackTrace)
        throws SerializeException, IOException
    {
        Writer writer = sctx.getWriter();
        //writer.write("<detail><stackTrace>");
        //Util.writeXMLEscapedString(writer, s);
        //writer.write("</stackTrace></detail>\n");
        writer.write("<detail>\n");
        
        if(wsdlFault != null) {
            writeWsdlFault(sctx, wsdlFault);
        }
        
        writer.write("<n:exceptionName xmlns:n=\"http://xml.apache.org/axis/\">");
        Util.writeXMLEscapedString(writer, exceptionName);
        writer.write("</n:exceptionName>\n");
        
        writer.write("<n:stackTrace xmlns:n=\"http://xml.apache.org/axis/\">");
        Util.writeXMLEscapedString(writer, stackTrace);
        writer.write("</n:stackTrace>\n");
        
        writer.write("</detail>\n");
    }
    
    public static void writeFault(SerializeContext sctx,
                                  String uri,
                                  String faultcode,
                                  Throwable detail)
        throws SerializeException, IOException
    {
        //assert detail != null;
        writeEnvelopeStart(sctx);
        writeBodyStart(sctx);
        writeFaultStart(sctx);
        
        
        writeFaultCode(sctx, uri, faultcode);
        
        if(detail != null) {
            //TODO: writeDetail - specialized serializer for Throwable objects instead of passing XmlNode
            if(detail.getMessage() != null) {
                writeFaultString(sctx, detail.getClass().getName()
                                     +": "+detail.getMessage());
            } else {
                writeFaultString(sctx, detail.toString());
            }
            String exceptionName = detail.getClass().getName();
            StringWriter sw = new StringWriter();
            detail.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString(); //+"\n";
            XmlNode wsdlFault = null;
            if(detail instanceof RemoteException) {
                wsdlFault = ((RemoteException)detail).getWsdlFault();
            }
            writeFaultDetail(sctx, wsdlFault, exceptionName, stackTrace);
        }
        
        
        writeFaultEnd(sctx);
        writeBodyEnd(sctx);
        writeEnvelopeEnd(sctx);
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


