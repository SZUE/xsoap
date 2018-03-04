/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HttpSocketSoapInvocationHandler.java,v 1.11 2003/11/15 00:34:24 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.gjt.xpp.XmlPullParserException;
import soaprmi.RemoteException;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.port.Endpoint;
import soaprmi.port.Port;
import soaprmi.server.SoaprmiClientSocketFactory;
import soaprmi.soap.SoapException;
import soaprmi.util.logging.Logger;

/**
 * Remote reference to SOAP service described in port.
 *
 * @version $Revision: 1.11 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class HttpSocketSoapInvocationHandler extends SoapDynamicStub {
    private static Logger logger = Logger.getLogger();
    private int timeout = 8 * 60 * 1000;// [ms] // 0; //2 * 60 * 1000;
    //private int timeout = HttpSocketSoapInvoker.getDefaultTimeout();
    protected SoaprmiClientSocketFactory connectionFactory;
    protected String httpProxyHost = null;
    protected int httpProxyPort = -1;
    protected String location;
    protected String host;
    protected int port;
    protected String path;

    public HttpSocketSoapInvocationHandler(SoaprmiClientSocketFactory connectionFactory_,
                                           Port port_,
                                           Endpoint epoint_,
                                           XmlJavaMapping mapping_,
                                           Class[] interfaces_)
        //throws RemoteException
    {
        super(port_, epoint_, mapping_, interfaces_);
        this.connectionFactory = connectionFactory_;

        //URL url = new URL(epoint.getLocation());
        // some parsing otherwise URL("https://") will choke:
        //   java.net.MalformedURLException: unknown protocol: https
        location = epoint.getLocation();
        String s = location;
        if(s.startsWith("http://")) {
            s = s.substring("http://".length());
        } else if(s.startsWith("https://")) {
            s = s.substring("https://".length());
        } else {
            throw new RuntimeException("expected http:// or https://");
        }
        int posSlash = s.indexOf('/');
        if(posSlash != -1) {
            path = s.substring(posSlash);
            s = s.substring(0, posSlash);
        }
        int posColon = s.indexOf(':');
        if(posColon != -1) {
            host = s.substring(0, posColon);
            s = s.substring(posColon +1);
            try {
                port = Integer.parseInt(s);
            } catch(Exception e) {
                throw new RuntimeException("Invalid port naumber '"+s+"' in "+location);
            }
        } else {
            host =s;
            port = location.startsWith("https://") ?
                HttpUtils.HTTPS_DEFAULT_PORT : HttpUtils.HTTP_DEFAULT_PORT;
        }


    }

    public int getTimeout() { return timeout; }
    public void setTimeout(int value) { timeout = value; }

    public Object invokeTransport(Object proxy, Method m, Object[] params)
        throws Throwable {
        try {

            MethodInvoker mi = newCall(m);

            Map requestHeaders = new HashMap();

            requestHeaders.put("SOAPAction", mi.getSoapAction());

            //set cookie
            String sCookie = epoint.getCookie();
            if (!"".equals(sCookie) && sCookie != null)
            {
                requestHeaders.put("Cookie", sCookie);
            }

            StringWriter sw = new StringWriter();
            Writer writer = new BufferedWriter(sw);
            mi.sendRequest(params, writer);
            writer.close();

            String requestContent = sw.toString();

            HttpUtils.HttpResult hr = HttpUtils.post(
                connectionFactory,
                host,
                port,
                path,
                requestContent,
                requestHeaders,
                "text/xml; charset=utf-8",
                //NOTE putting "" around utf-8 is crashing Tomcat 3.2.1 ...
                timeout,
                httpProxyHost,
                httpProxyPort,
                location
            );

            sCookie = (String) hr.getResponseHeaders().get("Set-Cookie");
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

            Reader reader = hr.getReader();


            Object result =  mi.receiveResponse(reader);

            returnCallToPool(mi, m);

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

