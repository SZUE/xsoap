/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "SOAP" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 2000, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */


package soaprmi.soaprpc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import soaprmi.RemoteException;
import soaprmi.Version;
import soaprmi.server.SoaprmiClientSocketFactory;
import soaprmi.util.Util;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

/**
 * Based on Apache SOAP - a bunch of utility stuff for doing HTTP things.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Wouter Cloetens (wcloeten@raleigh.ibm.com)
 */
public class HttpUtils {
    private static Logger logger = Logger.getLogger();
    private static Logger verboseTracer = Logger.getLogger("soaprmi.verbose.trace.invoker");

    // HTTP header field names.
    public static final String HEADER_POST = "POST";
    public static final String HEADER_HOST = "Host";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    //public static final String HEADER_SOAP_ACTION = "SOAPAction";
    //public static final String HEADER_AUTHORIZATION = "Authorization";


    // HTTP header field values.
    public static final String HEADERVAL_DEFAULT_CHARSET = "iso-8859-1";
    //public static final String HEADERVAL_CHARSET_UTF8 = "utf-8";
    //public static final String HEADERVAL_CONTENT_TYPE = "text/xml";

    private static final String HTTP_VERSION = "1.0";
    public static final int    HTTP_DEFAULT_PORT = 80;
    public static final int    HTTPS_DEFAULT_PORT = 443;


    public static class HttpResult {
        private Reader reader;
        private Map responseHeaders;

        public HttpResult(Reader r, Map m) {
            this.reader = r;
            this.responseHeaders = m;
        }

        public Reader getReader() {
            return reader;
        }

        public Map getResponseHeaders() {
            return responseHeaders;
        }
    }

    //    /**
    //     * Utility function to determine port number from URL object.
    //     *
    //     * @param url URL object from which to determine port number
    //     * @return port number
    //     */
    //    private static int getPort(URL url) throws IOException {
    //        int port = url.getPort();
    //        if (port < 0)  // No port given, use HTTP or HTTPS default
    //            if (url.getProtocol().equalsIgnoreCase("HTTPS"))
    //                port = HTTPS_DEFAULT_PORT;
    //            else
    //                port = HTTP_DEFAULT_PORT;
    //        return port;
    //    }

    /**
     * POST something to the given URL. The headers are put in as
     * HTTP headers, the content length is calculated and the content
     * byte array is sent as the POST content.
     *
     * @param url the url to post to
     * @param request the message
     * @param timeout the amount of time, in ms, to block on reading data
     * @param httpProxyHost the HTTP proxy host or null if no proxy
     * @param httpProxyPort the HTTP proxy port, if the proxy host is not null
     */
    public static HttpResult post(SoaprmiClientSocketFactory socketFactory,
                              String host,
                              int port,
                              String path,
                              String requestContent,
                              Map requestHeaders,
                              String requestContentType,
                              int timeout,
                              String httpProxyHost,
                              int httpProxyPort,
                              String location)
        throws IOException, RemoteException
    {
        if(Log.ON) logger.finest("sending request to host "+host+":"+port
                                     +" path="+path
                                     +" requestContentType="+requestContentType
                                     +" timeout="+timeout
                                     +" httpProxy="+httpProxyHost+":"+httpProxyPort);

        String charset = Util.getContentTypeCharset(requestContentType);
        byte[] request = requestContent.getBytes(
            charset != null ? charset : HEADERVAL_DEFAULT_CHARSET
        );
        int requestContentLen = request.length;

        /* Construct the HTTP header. */
        StringBuffer headerbuf = new StringBuffer();

        //String path = url.getFile();
        if(path == null || path.length() == 0) path = "/";
        headerbuf.append(HEADER_POST).append(' ')
            .append(httpProxyHost == null ? path : location)
            .append(" HTTP/").append(HTTP_VERSION).append("\r\n")
            .append(HEADER_HOST).append(": ").append(host)
            //.append(':').append(port)
            .append("\r\n")
            .append("User-Agent: SoapRMI/").append(Version.getImplementationVersion())
            .append("\r\n")
            .append(HEADER_CONTENT_TYPE).append(": ")
            .append(requestContentType).append("\r\n")
            .append(HEADER_CONTENT_LENGTH).append(": ")
            .append(requestContentLen).append("\r\n");
        for (Iterator i = requestHeaders.keySet().iterator(); i.hasNext(); ) {
            Object key = i.next();
            headerbuf.append(key).append(": ")
                .append(requestHeaders.get(key)).append("\r\n");
        }
        headerbuf.append("Connection: Close").append("\r\n");

        headerbuf.append("\r\n");

        //try {                    // DEBUG - remove me
        /* Send the request. */
        //BufferedOutputStream bOutStream = new BufferedOutputStream(outStream);

        byte[] headers = headerbuf.toString().getBytes(HEADERVAL_DEFAULT_CHARSET);

        //System.err.println("connecting to  "+url);
        //int port = getPort(url);
        //String host = url.getHost();
        //System.err.println("connecting to  "+url+" host="+host+" port="+port);
        Socket s = null;
        OutputStream outStream = null;
        InputStream inStream = null;

        // This is clearly hacking over java.net quirks...
        // event if socket is correctly created i can still get
        // get some exceptions corresponsindg to low-level
        // socket signals (only on solaris though)
        // ex:java.io.IOException: Transport endpoint is not connected
        //   at java.net.SocketOutputStream.socketWrite(Native Method)
        //   at java.net.SocketOutputStream.write(SocketOutputStream.java:62)
        //   at soaprmi.util.HTTPUtils.post(HTTPUtils.java:233)
        // whih is line with first call to  outStream.write(...)

        int tryCount = 3;

        while(tryCount-- > 0) {
            try {
                s = //buildSocket(url, port, httpProxyHost, httpProxyPort);
                    //new Socket(host, port);
                    socketFactory.connect(host, port);

                s.setTcpNoDelay(true);

                outStream = s.getOutputStream();
                inStream = s.getInputStream();


                if (timeout > 0)  // Should be redundant but not every JVM likes this
                    s.setSoTimeout(timeout);

                if(Log.ON)
                    logger.finest("sending headers:---\n"+new String(headers)+"---");
                outStream.write(headers);

                break;

            } catch(IOException ioe) {
                if (tryCount == 0
                    ||  ioe instanceof UnknownHostException
                    ||  ioe instanceof InterruptedIOException) {
                    throw ioe;
                }
                if(s != null) {
                    try {
                        s.close();
                    } catch(Exception e) {
                    }
                }
                continue;
            }
        }


        if(Log.ON) logger.finest("sending request body len="+requestContentLen
                                     +" content:---\n"+requestContent+"---");
        outStream.write(request);

        //request.writeTo(bOutStream);
        //bOutStream.write('\r'); bOutStream.write('\n');
        //bOutStream.write('\r'); bOutStream.write('\n');
        //bOutStream.flush();
        outStream.flush();
        //s.shutdownOutput();

        if(Log.ON) logger.finest("reading response");

        BufferedReader in = null;
        BufferedInputStream bInStream = new BufferedInputStream(inStream);
        /* Read the response status line. */
        int statusCode = 0;
        String statusString = null;
        StringBuffer linebuf = new StringBuffer();
        int b = 0;
        while(true) {
            b = bInStream.read();
            if(Log.ON) verboseTracer.finest("reading first line byte: "+b
                                                +" character:`"+(char)b+"'"
                                                + " line so far "+linebuf);
            if(b == -1) {
                if(linebuf.length() > 0) {
                    if(Log.ON) logger.severe(
                            "unexpected end of input when reading input line so far '"+linebuf+"'");
                    throw new IOException(
                        "unexpected end of input when reading input line so far '"+linebuf+"'");
                } else {
                    if(Log.ON) logger.severe(
                            "connection to "+s.getInetAddress()+" timed out");
                    throw new IOException(
                            "connection to "+s.getInetAddress()+" timed out");
                }
            }
            if (b != '\n') {
                if( b != '\r') {
                    linebuf.append((char)b);
                }
            } else {
                if(linebuf.length() > 0 ) { // got enough in line?
                    break;
                }
            }
        }
        String line = linebuf.toString();
        if(Log.ON) logger.finest("received first line:`"+line+"'");
        try {
            StringTokenizer st = new StringTokenizer(line);
            st.nextToken(); // ignore version part
            statusCode = Integer.parseInt (st.nextToken());
            StringBuffer sb = new StringBuffer();
            while (st.hasMoreTokens()) {
                sb.append (st.nextToken());
                if (st.hasMoreTokens()) {
                    sb.append(" ");
                }
            }
            statusString = sb.toString();
        }
        catch (Exception e) {
            throw new IOException(
                "error parsing HTTP status line \"" + line + "\": " + e);
        }

        // Read the entire response (following the status line)
        // into a byte array.

        if(Log.ON) logger.finest("converting rest of response into byte array");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte buf[] = new byte[4096];
        int len;
        while (true){
            try {
                len = bInStream.read(buf);
            } catch(EOFException eof) {
                break;
            }
            if (len < 0) {
                break;
            }
            //TODO
            if(Log.ON) verboseTracer.finest("read:---\n"+new String(buf, 0, len)+"---");
            os.write(buf, 0, len);
        }
        byte [] bytes = os.toByteArray();
        if(Log.ON) verboseTracer.finest("input array:---"+new String(bytes, 0)+"---");

        //if(Log.ON) l.finest("reading headers from array");
        Hashtable respHeaders = new Hashtable();
        int respContentLength = -1;
        String respContentType = null;
        StringBuffer namebuf = new StringBuffer();
        StringBuffer valuebuf = new StringBuffer();
        boolean parsingName = true;
        int offset;
        for (offset = 0; offset < bytes.length; offset++) {
            if (bytes[offset] == '\n') {
                if (namebuf.length() == 0)
                    break;
                String name = namebuf.toString();
                String value = valuebuf.toString();
                if (name.equalsIgnoreCase(HEADER_CONTENT_LENGTH))
                    respContentLength = Integer.parseInt(value);
                else if (name.equalsIgnoreCase(HEADER_CONTENT_TYPE))
                    respContentType = value;
                else
                    respHeaders.put(name, value);
                namebuf = new StringBuffer();
                valuebuf = new StringBuffer();
                parsingName = true;
            }
            else if (bytes[offset] != '\r') {
                if (parsingName) {
                    if (bytes[offset] == ':') {
                        parsingName = false;
                        offset++;
                    }
                    else
                        namebuf.append((char)bytes[offset]);
                }
                else
                    valuebuf.append((char)bytes[offset]);
            }
        }

        if(Log.ON) logger.finest("reading body from array");
        InputStream is = new ByteArrayInputStream(bytes);
        is.skip(offset + 1);
        if (respContentLength < 0)
            respContentLength = bytes.length - offset - 1;

        //response = new TransportMessage(is, respContentLength,
        //                                    respContentType, ctx, respHeaders);

        byte[] responseBytes = new byte[respContentLength];
        is.read(responseBytes);

        charset = Util.getContentTypeCharset(respContentType);
        String response = new String(responseBytes,
                                     charset != null ? charset : HEADERVAL_DEFAULT_CHARSET
                                    );
        if(Log.ON) verboseTracer.finest("received response:---\n"+response+"---");


        /* All done here! */
        //bOutStream.close();
        outStream.close();
        bInStream.close();
        //inStream.close();
        //s.shutdownInput();
        s.close();

        return new HttpResult(new StringReader(response), respHeaders);
        //} catch (IOException ioe) { ioe.printStackTrace(); throw ioe; }  // DEBUG - remove me
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


