/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HttpSocketSoapServerConnection.java,v 1.22 2003/09/23 17:45:59 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import soaprmi.ServerException;
import soaprmi.Version;
import soaprmi.server.ConnectionContext;
import soaprmi.server.Services;
import soaprmi.util.Util;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

/**
 * Encapsulate processing of web service in HTTP connection to Dispatcher.
 *
 * @version $Revision: 1.22 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class HttpSocketSoapServerConnection implements Runnable {

    public final static String DEFAULT_CHARSET = "iso-8859-1";
    //    private final static boolean TRACE_SENDING =
    //        SoapServices.TRACE_DISPATCH && SoapServices.TRACE_SENDING;
    //    private final static boolean TRACE_RECEIVING =
    //        SoapServices.TRACE_DISPATCH && SoapServices.TRACE_RECEIVING;
    private final static Logger TRACE_SENDING =
        SoapServices.TRACE_DISPATCH_OUT;
    private final static Logger TRACE_RECEIVING =
        SoapServices.TRACE_DISPATCH_IN;

    private Logger logger = Logger.getLogger();
    protected Map connectionProps;
    protected SoapDispatcher dsptr;
    protected Socket socket;
    protected InputStream inputStream;
    protected OutputStream outputStream;

    protected HttpSocketSoapServerConnection() {
    }

    public void setConnectionProps(Map connectioProps_) {
        connectionProps = connectioProps_;
    }

    public void setSoapDispatcher(SoapDispatcher dsptr_) {
        dsptr = dsptr_;
    }

    public void setSocket(Socket s) {
        socket = s;
    }

    // simple automaton to process incoming requests
    public void run() {
        try {
            ConnectionContext cctx = Services.getConnectionContext();
            cctx.setIncomingSocket(socket);
            cctx.setIncomingProps(connectionProps);
            logger.finest("cctx="+cctx+" socket="+socket+" connectionProps="+connectionProps);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            process(cctx);
        } catch(ServerException ex) {
            //ex.printStackTrace(System.err);
            logger.warning("server exception in HTTP connection, sending HTTP error ", ex);
            error(ex.toString(), ex); //500
        } catch(IOException ex) {
            // this can be safely dropped
            //- no much point in sending back HTTP error code!!!
            logger.warning("IO exception in HTTP connection, closing connection ", ex);
        } catch(Exception ex) {
            //ex.printStackTrace(System.err);
            logger.warning("exception in HTTP connection, sending HTTP error ", ex);
            error(ex.toString(), ex);
        } finally {
            try {
                logger.finest("closing socket");
                if(socket != null) socket.close();
            } catch(IOException ex) {
                // TODO tracing
            }
        }
        logger.finest("connection thread finished");
    }

    private void process(ConnectionContext cctx) throws IOException, ServerException {
        logger.entering();
        boolean keepalive = true;
        while(keepalive) {
            logger.finest("waiting for next request");
            int length = -1;
            DataInputStream lineInput = new DataInputStream(inputStream);
            if(TRACE_RECEIVING.isFinestEnabled()) TRACE_RECEIVING.finest(
                    "TRACE waiting for first line of HTTP request");
            String request = null;
            try {
                // RFC 2616 says be generous about allowing extra \r\n between requests
                do {
                    request =lineInput.readLine();
                } while ((request != null) && (request.length() == 0));
            } catch(java.net.SocketException ex) {
                //skip typical  SocketException: Connection reset by peer:
                //JVM_recv in socket in put stream read
                //ex.printStackTrace();
            }
            if(request==null) {
                //throw new ServerException("Invalid request");
                break; //we are finished - client does not want to send any more requests
            }
            if(Log.ON && TRACE_RECEIVING.isFinestEnabled()) TRACE_RECEIVING.finest(
                    "TRACE receiving first line with request:---\n"+request+"---");
            String method      = null;
            String path        = null;
            String httpversion = null;

            try {
                StringTokenizer tokenizer=new StringTokenizer(request," ");
                if(!tokenizer.hasMoreTokens()) {
                    throw new ServerException("Wrong request: "+request);
                }
                method      = tokenizer.nextToken();
                path        = tokenizer.nextToken();
                httpversion = tokenizer.nextToken();
            } catch(Exception ex) {
                throw new ServerException("Malformed HTTP request: '"+request+"'", ex);
            }
            keepalive  = "HTTP/1.1".equals (httpversion);
            String contentType = null;

            // readHeaders()
            Hashtable headers = new Hashtable();
            while(true) {
                String line=lineInput.readLine();
                if(TRACE_RECEIVING.isFinestEnabled())
                    System.err.println(line);
                if(line == null || "".equals(line)) {
                    break;
                }
                int index = line.indexOf(':');
                if(index == -1)  //TODO: handle multiline MIMEs???
                    continue;
                String name = line.substring(0, index).toLowerCase();
                int len = line.length();
                for(++index;index < len;++index) {
                    if(line.charAt(index) != ' ')
                        break;
                }
                String value=line.substring(index);
                headers.put(name, value);
                if(name.equals("content-length")) {
                    try {
                        length = Integer.parseInt(value);
                    } catch(NumberFormatException ex) {
                        throw new ServerException("could not parse content-length of '"+line+"'");
                    }
                }
                if(name.equals("content-type")) {
                    contentType = value;
                }
                if(name.equals("connection")) {
                    keepalive = value.indexOf ("keep-alive") > -1;
                }
            }

            // --- prepare input buffers for request

            //
            String charset = Util.getContentTypeCharset(contentType);
            logger.finest("got charset="+charset
                              +" from contentType="+contentType);

            Reader reader;

            //if(length==0) length = 100;
            if(length > -1) {
                reader  = new InputStreamReader(
                    new FixedLengthInputStream(inputStream, length),
                    charset != null ? charset : DEFAULT_CHARSET);
            } else {
                reader  = new InputStreamReader(inputStream,
                                                charset != null ? charset : DEFAULT_CHARSET);
            }
            if(length < 0) {
                logger.warning("length="+length);
            }
            if(TRACE_RECEIVING.isFinestEnabled()
               && ("POST".equals(method) || "OST".equals(method)) ) {
                //char[] cbuf = new char[length];
                //int toRead = length;
                //while(toRead > 0) {
                //  int received = reader.read(cbuf, length - toRead, toRead);
                //  toRead -= received;
                //}
                StringWriter sw = new StringWriter(length > 0 ? length : 8*1024);
                char[] cbuf = new char[4*1024];
                while(true) {
                    int received = reader.read(cbuf);
                    if(received <= 0)
                        break;
                    sw.write(cbuf, 0, received);
                }
                String requestBody = sw.toString();
                //System.err.println(
                //"TRACE: "+getClass()+" received request:---\n"+
                TRACE_RECEIVING.finest(" received request:---\n"+requestBody+"---\n");
                reader = new StringReader(requestBody);
            }

            // --- prepare output buffers

            ByteArrayOutputStream baos =
                new ByteArrayOutputStream(length > 0 ? length : 1024);
            Writer writer = new OutputStreamWriter(baos,
                                                   charset != null ? charset : DEFAULT_CHARSET);

            //  -- do processing

            int errCode = 200;
            Map bag = connectionProps; //new Hashtable();
            bag.put("provides.path", path);
            bag.put("provides.http.headers", headers);
            java.net.InetAddress remoteIP = socket.getInetAddress();
            bag.put("provides.remote.ip", remoteIP);
            int remotePort = socket.getPort();
            bag.put("provides.remote.ip.port", new Integer(remotePort));
            logger.finest("dispatching method '"+method+"'");
            if(method.equals("GET")) {
                doGet(reader, writer, bag, true);
            } else if(method.equals("HEAD") ) {
                doGet(reader, writer, bag, false);
            } else if("POST".equals(method) || "OST".equals(method)  ) {
                if("OST".equals(method)) {
                    logger.warning("processing call that required delegation but user did just POST!");
                }
                if(length > 0) {
                    errCode = doPost(reader, writer, bag, length);
                } else {
                    logger.warning(
                        "TRACE: skipping POST request, problem with length="+length);
                }
            } else {
                throw new ServerException("Unsupported method "+method);
            }

            // --- send response
            writer.flush();
            writer.close();
            byte [] bytes = baos.toByteArray();
            int len = bytes.length;
            String content =
                (method.equals("POST") ? "text/xml" : "text/html");
            if(charset != null) {
                content += "; charset=\""+charset+"\"";
            } else {
                content += "; charset=\""+DEFAULT_CHARSET+"\"";
            }

            {
                Map outProps = cctx.getOutgoingProps();
                Map outHeaders  = (Map) outProps.get("outgoing.http.headers");

                String headersOut=getHeaders(errCode, outHeaders, content, len, keepalive);
                if(TRACE_SENDING.isFinestEnabled()) {
                    String body = new String(bytes,
                                             charset != null ? charset : DEFAULT_CHARSET);
                    TRACE_SENDING.finest("TRACE: sending response:"
                                             +"---\n"+headersOut+body+"---\n");
                    TRACE_SENDING.finest("TRACE: sending response headers");
                }
                //BufferedWriter output = new BufferedWriter(
                //  new OutputStreamWriter(outputStream(),"8859_1"));
                //TODO: check if it idesult encoding for HTTP headers????
                outputStream.write(headersOut.getBytes("utf-8"));
            }
            if(method.equals("HEAD") == false) {
                logger.finest("sending response body");
                outputStream.write(bytes);
            }
            outputStream.flush();
            logger.finest("sending response finished");
        }
        logger.exiting();
    }


    public String getHeaders(int errCode,
                             Map outHeaders,
                             String contentType,
                             int contentLen,
                             boolean keepalive)
    {
        StringBuffer buf = new StringBuffer();
        String message = "";
        if(errCode == 200) {
            message = "OK";
        } else if (errCode == 500) {
            message = "Error processing request.";
        }


        buf.append("HTTP/1.0 ").append(errCode)
            .append(" ").append(message).append("\r\n");

        //TODO check that no outHeaders overwrite those ...
        buf.append("Date: "+(new Date()).toGMTString() + "\r\n");
        buf.append("Server: ").append(SoapServices.USER_AGENT).append("\r\n");
        buf.append("Content-Type: ").append(contentType).append("\r\n");
        buf.append("Content-Length: ").append(contentLen).append("\r\n");


        if(keepalive) {
            buf.append("Connection: keep-alive\r\n");
        }

        if(outHeaders != null && outHeaders.size() > 0) {
            Set entries = outHeaders.entrySet();
            for(Iterator iter = entries.iterator(); iter.hasNext(); ) {
                Map.Entry en = (Map.Entry) iter.next();
                String headerName = (String) en.getKey();
                String headerValue = (String) en.getValue();
                buf.append(headerName);
                buf.append(": ");
                buf.append(headerValue);
                buf.append("\r\n");
            }
        }

        buf.append("\r\n");
        return buf.toString();
    }


    public int doPost(Reader reader, Writer writer,
                      Map bag, int length) throws IOException
    {
        if(dsptr.dispatch(reader, writer, bag) == false) {
            //write header with 500 Internal Server error...
            //return 500;
            // HACK: JDK 1.x incorreclty interprets HTTP status code 500
            // as fatal erro and WILL NOT return input stream so SoapRMI
            // will not be able to process SOAP:Fault response...
            return 200;
        }
        return 200;
    }

    //TODO: send nice HTTP errors
    public void error(String msg, Throwable thrw)
    {
        System.err.println(getClass().getName()+" error: "+msg);
        thrw.printStackTrace();
        logger.severe("error "+msg, thrw);

        try {
            byte[] bytes = msg.getBytes();
            int len = bytes.length;
            String headers = getHeaders(500, null, "text/html", len, false);
            outputStream.write(headers.getBytes("utf-8"));
            outputStream.write(bytes);
            outputStream.flush();
        } catch(IOException ex) {
            logger.warning("exception in sending HTTP error "+msg, ex);
        } //UnsupportedEncodingException
    }

    public void doGet(Reader reader, Writer writer,
                      Map bag, boolean body) throws IOException
    {
        //System.err.println("processing get");
        doDefault(writer);
    }

    public void doDefault(Writer writer) throws IOException {
        //res.setContentType("text/html");

        PrintWriter out = new PrintWriter(writer);

        out.println("<html>");
        out.println("<body bgcolor=\"white\">");
        out.println("<head>");
        out.println("<title>" + "SoapRMI "
                        + Version.getImplementationVersion()
                        +"</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>" + "SoapRMI "
                        + Version.getImplementationVersion()
                        + "</h1>");
        out.println("<p>Status: OK");
        out.println("</body>");
        out.println("</html>");
        out.flush();
        //out.close();

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




