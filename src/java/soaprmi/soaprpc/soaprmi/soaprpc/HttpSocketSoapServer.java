/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/

/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HttpSocketSoapServer.java,v 1.17 2003/04/06 00:04:20 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.Exception;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import soaprmi.ServerException;
import soaprmi.server.SoaprmiServerSocketFactory;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

//TODO: use thread pool

/**
 * Very simple embeddable web server that is hosting web services.
 *
 * @version $Revision: 1.17 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class HttpSocketSoapServer implements SoapServer, Runnable {
    private static Logger logger = Logger.getLogger();
    protected SoapDispatcher dsptr = new HttpSocketSoapDispatcher(this);
    protected Thread listenThread ;
    //private int port; // = 7777;
    //private String propertiesName = "soaprmi.properties";
    protected boolean running;
    protected boolean shutdown;
    protected static int connectionNo;
    private int defaultSocketTimeout;

    protected SoaprmiServerSocketFactory socketFactory;

    protected HttpSocketSoapServer(SoaprmiServerSocketFactory socketFactory)
    {
        this.socketFactory = socketFactory;
        defaultSocketTimeout = HttpSocketSoapInvoker.getDefaultTimeout();
        // thread is started immediately to avoid JVM exiting when no running threads is around...
        listenThread = new Thread(this, "listen"+getServerPort());
        listenThread.start();
    }


    public static HttpSocketSoapServer newInstance(SoaprmiServerSocketFactory socketFactory) {
        return new HttpSocketSoapServer(socketFactory);
    }

    public int getServerPort() {
        //return port;
        return socketFactory.getServerPort();
    }

    public SoapDispatcher getDispatcher() {
        return dsptr;
    }

    public String getLocation() throws ServerException {
        return socketFactory.getServerLocation();
    }

    public void setDispatcher(SoapDispatcher disp_) {
        dsptr = disp_;
    }

    public void startServer() throws ServerException {
        if(shutdown) {
            throw new ServerException("already shutdown server can not started");
        }
        //init();
        //String name = getClass().getName();
        //name = name.substring(name.lastIndexOf('.') + 1);
        if(Log.ON) logger.config("starts listening on "+ getServerPort()+" :-)");
        running = true;
        if(listenThread == null) {
            listenThread = new Thread(this, "listen"+getServerPort());
            //listenThread.setDaemon(true);
            listenThread.start();
        }
        //
        //        }
        //
    }

    public void stopServer() throws ServerException {
        if(shutdown) {
            throw new ServerException("already shutdown server can not stopped");
        }
        running = false;
        if(listenThread != null) {
            listenThread.interrupt();
        }
        if(Log.ON) logger.config("stops listening on "+ getServerPort()+" :-)");
    }

    public void shutdownServer() throws ServerException {
        try {
            stopServer();
        } catch(Exception e) {
        }
        if(listenThread != null) {
            try {
                socketFactory.shutdown();
            } catch(IOException ioe) {
                if(Log.ON) logger.config("socket shutdown", ioe);
            }
        }
        shutdown = true;
        if(Log.ON) logger.config("server is shutdown");
    }

    // -- more internal functions

    public void run() {
        if(Log.ON) logger.entering();
        while(!shutdown) {
            try {
                while(running) {
                    Map connectionProps = new HashMap();
                    Socket socket = null;


                    try {
                        socket = socketFactory.accept(connectionProps);
                        if(Log.ON) logger.finest("received connection socket="+socket);
                        socket.setSoTimeout(defaultSocketTimeout); // 4 minutes timeout

                        // start proceshsing client data from socket
                        try {
                            //                            if(Log.ON) logger.fine(
                            //                                    "firing new connection socket="+socket
                            //                                        +" connectionProps="+connectionProps);
                            fireConnection(socket, connectionProps);
                        }catch(Exception ex) {
                            logger.fine("exception in processing connection", ex);
                        }

                    } catch(InterruptedIOException se) {
                        //this is fine to ignore this exception here - in this case stopServer()
                        //may have requested stopping but this thread is still running
                        //and is blocked on socket.accept()
                        //so stopServer() calls listenThread.interrupt() that results
                        //in InterruptedIOException that leads to checking of
                        //loop condition in while(running) and as running==false
                        // this thread will cleanly exit :-)

                        //of course the other possibility is that accept() timed out
                        //then running == true and we will just be back to accept() ...

                        // but just to be sure we will close socket to notify client
                        if(socket != null) { try {socket.close();} catch(Exception ex){} }

                    } catch(IOException se) {
                        logger.fine("exception in accepting socket connection", se);
                        // when SSL used it simportant to notify client that connection failed
                        if(socket != null) { try {socket.close();} catch(Exception ex){} }
                        //                    }catch(SocketException se) {
                        //                        l.fine("exception in accepting connection", se);
                        //                        if(s != null) { s.close();}
                        //continue LOOP;
                        //throw se;
                    }

                }
                //UNRESOLVED: seems to be required on Solaris or it goes into some kind
                //  of race condition and throws socket timeout after 200 seconds ...
                //Remains unresolved due to Heisenberg principle applied ot use of debugger :-)
                //  (using local or remote debugging over JDWP behavior could not be reproduced ...)
                //
                try {
                    Thread.currentThread().sleep(10 * 1);
                } catch(InterruptedException ie) {
                }

                // eliminate even remote possibility that this thread will be stopped accidentally..
            } catch(Exception ex) {
                //ex.printStackTrace();  //good for now....
                logger.severe("exception in embedded web server", ex);
            } finally {
            }
        }
        if(Log.ON) logger.exiting();
    }

    public void fireConnection(Socket socket, Map connectionProps) {
        HttpSocketSoapServerConnection wsc = new HttpSocketSoapServerConnection();
        //              connectionProps = new HashMap();
        //              connectionProps_.putAll( connectionProps );
        wsc.setConnectionProps(connectionProps);
        wsc.setSocket(socket);
        wsc.setSoapDispatcher(dsptr);
        //TODO: pool threads
        //String name = getClass().getName();
        //name = name.substring(name.lastIndexOf('.') + 1);
        //System.err.println("creating new thread "+connectionNo);
        new Thread(wsc, "connection"+(++connectionNo)+"on"+getServerPort()).start();
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




