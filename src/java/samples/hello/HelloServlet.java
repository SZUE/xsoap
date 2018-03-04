/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HelloServlet.java,v 1.9 2003/04/06 00:04:04 aslom Exp $
 */

package hello;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Hashtable;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import soaprmi.AlreadyBoundException;
import soaprmi.RemoteException;
import soaprmi.port.Port;
import soaprmi.server.Services;
import soaprmi.soaprpc.SoapDispatcher;
import soaprmi.soaprpc.SoapServices;
import soaprmi.soaprpc.SoapServletDispatcher;

/**
 * Example Hello web service running in servlet container.
 *
 * @version $Revision: 1.9 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class HelloServlet extends HttpServlet {
    public final static boolean DEBUG = true;
    /* Actual implementationof event receiver embedded in servlet. */
    private static HelloService helloService;
    /* Private SOAP dispatcher - only for this servlet. */
    private static SoapDispatcher dsptr;
    /* what is remote reference port for this service */
    private static Port port;

    public HelloServlet() {
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /** Write information about this component - such as XML description */
    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        Writer out = res.getWriter();
        res.setContentType("text/html");
        out.write("<html><head><title>SOAP RMI Hello Sample</title></head>");
        out.write("<body bgcolor='white'><h1>This is SOAP RMI Hello Sample!</h1>");
    }

    /** Execute SOAP RPC call thatis HTTP POST dispatching it helloService */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws  ServletException, IOException
    {
        if(dsptr == null) initSoap(req);
        if(DEBUG) System.err.println(getClass()+" entering doPost()");

        Reader reader = null;
        try {
            reader = req.getReader();
        } catch(IOException ioe) {
            System.err.println(getClass()+" got "+ioe);
            ioe.printStackTrace();
            throw ioe;
        }
        res.setContentType("text/xml");

        // nice ot see what is going on - in production DEBUG should be false
        if(DEBUG) {
            System.err.println(getClass()+" new request");
            int length = req.getContentLength();
            char[] cbuf = new char[length];
            int toRead = length;
            while(toRead > 0) {
                int received = reader.read(cbuf, length - toRead, toRead);
                toRead -= received;
            }
            System.err.println(getClass()+" request ="+new String(cbuf));

            BufferedReader sreader = new BufferedReader(
                new StringReader(new String(cbuf)));
            reader = sreader;
        }
        if(DEBUG) System.err.println(getClass()+" getting writer");

        Writer rwriter = res.getWriter();
        Writer writer = rwriter;
        if(DEBUG) {
            StringWriter swriter = new StringWriter();
            writer = swriter;
        }

        if(DEBUG) System.err.println(getClass()+" dispatching");
        Hashtable bag = new Hashtable();
        String path = req.getServletPath() + (req.getPathInfo() != null ? req.getPathInfo() : "");
        if(DEBUG) System.err.println(" setting bag provides.path="+path);
        bag.put("provides.path", path);
        dsptr.dispatch(reader, writer, bag);
        if(DEBUG) System.err.println(getClass()+" leaving doPost()");

        if(DEBUG) {
            String s = writer.toString();
            System.err.println(getClass()+" response = "+s);
            rwriter.write(s);
        }
        rwriter.close();
    }



    private void initSoap(HttpServletRequest req)
        throws ServletException, IOException
    {
        try {
            // how to figure out that it may be https:// ???
            String location = "http://"
                +req.getServerName()+":"+req.getServerPort()
                +req.getServletPath();


            // create dispatcher that will take care of executing RPC
            dsptr = new SoapServletDispatcher(location);

            Services services = SoapServices.newInstance();
            services.addDispatcher(dsptr);

            // create communication ecpoint (port)
            helloService = new HelloServiceImpl("Indiana");
            port = services.createPort(
                "hello-service", //HelloService.SERVICE_NAME,     // portName
                HelloService.class,  // portType
                helloService         // implementation of port type
            );

        } catch(RemoteException ex) {
            throw new ServletException("can't initialize SOAP port", ex);
        } catch(AlreadyBoundException ex) {
            throw new ServletException("can't create SOAP port", ex);
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

