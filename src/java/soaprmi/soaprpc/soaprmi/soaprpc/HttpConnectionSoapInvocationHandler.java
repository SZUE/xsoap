/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HttpConnectionSoapInvocationHandler.java,v 1.9 2003/09/23 17:45:59 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.*;
import java.net.*;

import java.lang.reflect.Method;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import soaprmi.Remote;
import soaprmi.RemoteException;
//import soaprmi.ServerException;
import soaprmi.server.Invoker;
import soaprmi.server.RemoteRef;
import soaprmi.mapping.*;
import soaprmi.soap.*;
import soaprmi.port.*;
import soaprmi.util.*;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

import org.gjt.xpp.XmlPullParserException;

/**
 * Remote reference to SOAP service described in port.
 *
 * @version $Revision: 1.9 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class HttpConnectionSoapInvocationHandler extends SoapDynamicStub{
    private static Logger logger = Logger.getLogger();

    public HttpConnectionSoapInvocationHandler(
        Port port_, Endpoint epoint_,
        XmlJavaMapping mapping_, Class[] interfaces_)
    {
        super(port_, epoint_, mapping_, interfaces_);
    }

    public Object invokeTransport(Object proxy, Method m, Object[] params)
        throws Throwable {
        try {
            //TODO: pool connection
            URL url = new URL(epoint.getLocation());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if(Log.ON) logger.fine( "opening HTTP connection to "+url
                                       +" to execute remote method "+m.getName()
                                       +" port="+port);
            //     HttpURLConnection conn = new HttpURLConnection(url);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            String charset = "utf-8"; //"UTF-8"; "ISO-8859-1"; //"UTF-16";//
            conn.setRequestProperty("Content-Type",
                                    //NOTE putting \"\" around charset is crashing Tomcat 3.2.1
                                    "text/xml; charset="+charset+""
                                   );
            //"text/xml; charset="+charset);
            conn.setRequestProperty("User-Agent", SoapServices.USER_AGENT);
            MethodInvoker mi = newCall(m);

            conn.setRequestProperty("SOAPAction", mi.getSoapAction());
            //"\""+targetURI + "#" + mi.getMethodRequestName() +"\"");
            //set cookie
            String sCookie = epoint.getCookie();
            if (!"".equals(sCookie) && sCookie != null)
            {
                conn.setRequestProperty("Cookie", sCookie);
            }

            //conn.connect();
            OutputStream out = conn.getOutputStream();
            //out.write("hello".getBytes());
            //out.close();
            Writer writer = new BufferedWriter(
                new OutputStreamWriter(out,
                                       charset != null ? charset :
                                           HttpSocketSoapServerConnection.DEFAULT_CHARSET
                                      ));
            //Writer writer = new StringWriter();
            //Reader reader = new StringReader("");
            //Object result = call.invoke(params, writer, reader);

            mi.sendRequest(params, writer);

            writer.close(); // sendRequest does not closes writer on its won

            // it takes care of not reading beyond Content-Length
            InputStream in = conn.getInputStream();

            //get the cookie:
            sCookie = conn.getHeaderField("Set-Cookie");
            epoint.setCookie(sCookie);
            logger.finest("received Set-Cookie sCookie = "+sCookie);
            //            if (!"".equals(sCookie) && sCookie != null)
            //            {
            //                int nIndex = sCookie.indexOf(";");
            //                if (nIndex >= 0)
            //                {
            //                    sCookie = sCookie.substring(0, nIndex);
            //                }
            //                //          System.out.println(sCookie);
            //                logger.finest("received Set-Cookie sCookie = "+sCookie);
            //                epoint.setCookie(sCookie);
            //            }

            String contentType = conn.getContentType();

            charset = Util.getContentTypeCharset(contentType);
            logger.finest("got charset="+charset
                              +" from contentType="+contentType);

            Reader reader = new InputStreamReader(
                in, charset != null ? charset : HttpSocketSoapServerConnection.DEFAULT_CHARSET);

            //BufferedReader br = new BufferedReader(reader);

            Object result =  mi.receiveResponse(reader);

            returnCallToPool(mi, m);

            //String line = br.readLine();
            //System.err.println("line="+line);
            //call.setReader(reader);
            //call.setWriter(writer);
            reader.close(); // --  receiveResponse doe snot closes reader
            //conn.disconnect(); -- only if you want to TRY to shutdown keep-alive conn

            return result;
        } catch(IOException ex) {
            throw new RemoteException("IO Exception", ex);
        } catch(SoapException ex) {
            throw new RemoteException("SOAP exception", ex);
        } catch(XmlPullParserException ex) {
            throw new RemoteException("SOAP exception", ex);
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

