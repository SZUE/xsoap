/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: UnicastRemoteObject.java,v 1.12 2003/04/06 00:04:19 aslom Exp $
 */

package soaprmi.server;

import soaprmi.AlreadyBoundException;
import soaprmi.Remote;
import soaprmi.RemoteException;
import soaprmi.port.Port;
import soaprmi.soaprpc.SoapServices;
import soaprmi.util.Check;
import soaprmi.util.logging.Logger;


/**
 * Base class to create SOAP web services with RMI approach.
 *
 * @version $Revision: 1.12 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class UnicastRemoteObject extends RemoteObject {
    private static Logger l = Logger.getLogger();
    //private static SoapServerFactory factory = HttpSocketSoapServerFactory.getDefault();
    private static Services defaultServices = SoapServices.getDefault();

    public UnicastRemoteObject() throws RemoteException {
        exportObject(this);
    }

    public UnicastRemoteObject(int port) throws RemoteException {
        exportObject(this, port);
    }

    public UnicastRemoteObject(String portName) throws RemoteException {
        exportObject(this, portName);
    }


    public UnicastRemoteObject(int port, String portName)
        throws RemoteException
    {
        exportObject(this, port, portName);
    }


    public UnicastRemoteObject(int port, String portName, Class[] ifaces ) throws RemoteException {
        exportObject(this, port, portName, ifaces);
    }


    public UnicastRemoteObject(String portName, Class[] ifaces)
        throws RemoteException {
        exportObject(this, portName, ifaces);
    }

    public UnicastRemoteObject(Class[] ifaces)
        throws RemoteException
    {
        exportObject(this, ifaces);
    }

//
//    public static SoapServerFactory getSoapServerFactory() {
//        return factory;
//    }
//
//    public static void setSoapServerFactory(SoapServerFactory value) {
//        factory = value;
//    }

    public static Services getDefaultServices() throws RemoteException {
        return defaultServices;
    }

    public static void setDefaultServices(Services services) throws RemoteException {
        defaultServices = services;
    }


    public static RemoteRef exportObject(Remote obj, Services services,
                                         String portName, Class[] ifaces) throws RemoteException
    {
        //register object with default dispatcher (from emebedded soap server)
        //if(services == SoapServices.getDefault()) startDefaultServer();
        try {
            Port port = services.createPort(
                portName,
                ifaces,
                obj
            );
            RemoteRef ref = services.createStartpoint(port);
            //for RemoteObject it should be always ref == this ...
            if(Check.ON && RemoteObject.class.isAssignableFrom(obj.getClass()))
                Check.assertion(ref == obj);
            return ref;
        } catch(AlreadyBoundException ex) {
            throw new RemoteException(
                "cant create port for already bound unicast remote object", ex);
            //} catch(NotBoundException ex) {
            //  throw new RemoteException("cant create remote referenece to port", ex);
        }

    }


    public static RemoteRef exportObject(Remote obj, int port,
                                         String portName, Class[] ifaces )
        throws RemoteException
    {
        Services services = SoapServices.newInstance(port);
        // this servie is PRIVATE so it is crucial to use default mappings DIRECTLY!!!
        services.setMapping(soaprmi.soap.Soap.getDefault().getMapping());

//        try {
//        SoapServer server = factory.newSoapServer(port);
//        //server.setPort(port);
//        SoapDispatcher dsptr = server.getDispatcher();
//        services.addDispatcher(dsptr);
//        //if(Log.ON) l.fine(
//        //        "remote object starting embedded web server "+server);
//        server.startServer();
//        if(Log.ON) l.fine(
//                "remote object web server started "+server+" on "+server.getServerPort());
//        } catch(IOException ioe) {
//            throw new ServerException("could not start embedded saerver on port "+port, ioe);
//        }
        RemoteRef ref = exportObject(obj, services, portName, ifaces);

        return ref;
    }

    public static RemoteRef exportObject(Remote obj, int port,
                                         String portName) throws RemoteException
    {
        Class[] ifaces = obj.getClass().getInterfaces();
        return exportObject(obj, port, portName, ifaces);
    }

    public static RemoteRef exportObject(Remote obj, int port) throws RemoteException
    {
        Services services = getDefaultServices();
        Class[] ifaces = obj.getClass().getInterfaces();
        return exportObject(obj, port, services.createGUID(), ifaces);
    }

    public static RemoteRef exportObject(Remote obj, Services services) throws RemoteException
    {
        Class[] ifaces = obj.getClass().getInterfaces();
        return exportObject(obj, services, services.createGUID(), ifaces);
    }

    public static RemoteRef exportObject(Remote obj, String portName,
                                         Class[] ifaces) throws RemoteException
    {
        Services services = getDefaultServices();
        return exportObject(obj, services, portName, ifaces);
    }

    public static RemoteRef exportObject(Remote obj, String portName)
        throws RemoteException
    {
        Class[] ifaces = obj.getClass().getInterfaces();
        return exportObject(obj, portName, ifaces);
    }

    public static RemoteRef exportObject(Remote obj, Class[] ifaces)
        throws RemoteException {
        Services services = getDefaultServices();
        return exportObject(obj, services.createGUID(), ifaces);
    }

    public static RemoteRef exportObject(Remote obj)
        throws RemoteException {
        Services services = getDefaultServices();
        return exportObject(obj, services.createGUID());
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

