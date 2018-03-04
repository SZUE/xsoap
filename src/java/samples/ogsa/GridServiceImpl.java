/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: GridServiceImpl.java,v 1.7 2003/04/06 00:04:07 aslom Exp $
 */

package ogsa;

import java.io.IOException;
import java.io.StringReader;

import soaprmi.Naming;
import soaprmi.Remote;
import soaprmi.RemoteException;

import soaprmi.server.UnicastRemoteObject;
import soaprmi.server.SecureUnicastRemoteObject;

import soaprmi.util.logging.Logger;

import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlPullParserFactory;

public class GridServiceImpl implements GridService  {
    private Logger logger = Logger.getLogger();

    public final static String GSL_NS = "http://schemas.gridforum.org/gridService";
    public final static String GSL_QUERY_ELEMENT = "queryByServiceDataName";
    public final static String NS = "http://www.extreme.indiana.edu/xgws/20020623/";

    public void  Destroy() throws RemoteException
    {
        throw new RemoteException("this service can not be destroyed");
    }

    public XmlNode FindServiceData(XmlNode xmlAsTree) throws RemoteException
    {
        // do some work on inpu
        String ns = xmlAsTree.getNamespaceUri();
        String name = xmlAsTree.getLocalName();
        if(!GSL_NS.equals(ns)) {
            throw new RemoteException("unknow namespace of query "+ns);
        }
        if(!GSL_QUERY_ELEMENT.equals(name)) {
            throw new RemoteException("unknow type of query for GSL: "+name);
        }
        try {
            //extract query namespace and local name from qname
            String qname = xmlAsTree.getAttributeValueFromRawName("qname");
            if(qname == null) {
                throw new RemoteException(
                    "qname attribute is required for GSL query element "+GSL_QUERY_ELEMENT);
            }
            String queryUri = xmlAsTree.getQNameUri(qname);
            String queryName = xmlAsTree.getQNameLocal(qname);

            logger.fine("got query qname uri="+queryUri+" name="+queryName);
            if(GSL_NS.equals(queryUri)) {
                //TODO: check queryNamer and support required OGSA serviceData elements

                throw new RemoteException("not yet supported OGSA serviceData "+queryName);
            } else if(NS.equals(queryUri)) {
                if("myServiceData".equals(queryName)) {
                    return converStringToXmlTree(
                        "<ns:description xmlns:ns='"+NS+"'>"
                            +"This server is based on XSOAP</ns:description>");
                } else {
                    throw new RemoteException("not yet supported private serviceData "+queryName);
                }
            } else {
                throw new RemoteException("unsupported query namespace "+queryUri);
            }
        } catch(XmlPullParserException ex) {
            throw new RemoteException("XML syntax error:"+ex, ex);
        }
    }

    public static XmlNode converStringToXmlTree(String xmlString)
        throws XmlPullParserException
    {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser pp = factory.newPullParser();
            pp.setInput(new StringReader(xmlString));
            pp.setNamespaceAware(true);
            pp.next();
            XmlNode nodeTree = factory.newNode(pp);
            return nodeTree;
        } catch(IOException ex) {
            throw new XmlPullParserException("could not do conversion", ex);
        }
    }

    public static void main(String args[])  {
        // if you want ot execute automatic test chnage this falg to true and recompile
        final boolean AUTOMATIC_TEST = false;
        try {
            if(AUTOMATIC_TEST) args = new String[] {"2998"};

            GridServiceImpl serviceImpl = new GridServiceImpl();
            boolean secure = false;

            String whereToPublish = args[0];
            if(whereToPublish == null) whereToPublish = "localhost";

            Remote remote = null;
            if( ! Character.isDigit(whereToPublish.charAt(0)) ) {
                // this whereToPublish point to RMI registry or JNDI provider (such as LDAP)
                String name = whereToPublish;
                if(whereToPublish.indexOf("//") == -1) {
                    name = (secure ? "rmis:" : "") + "//" + whereToPublish;
                }
                if(whereToPublish.indexOf("/") == -1) {
                    name += "/OgsaService";
                }

                System.out.println("Server exporting service on random socket port");
                if(secure) {
                    remote = SecureUnicastRemoteObject.exportObject("cog",  serviceImpl);

                } else {
                    remote = UnicastRemoteObject.exportObject(serviceImpl);
                }

                System.out.println("Server attempting to rebind in the registry to the name "+name);
                Naming.rebind(name, remote);
                System.out.println("Server created and bound in the registry to "+name);

            } else { //this whereToPublish is TCP port number
                System.out.println("Server attempting to bind to socket port "+whereToPublish);
                int port = Integer.parseInt(whereToPublish);
                if(secure) {
                    remote = SecureUnicastRemoteObject.exportObject("cog", serviceImpl, port);
                } else {
                    remote = UnicastRemoteObject.exportObject(serviceImpl, port);
                }
                System.out.println("Server is available at "+remote);


            }
            System.out.println("Server waiting for conections...");

            if(AUTOMATIC_TEST) {
                args[0] = "http://localhost:"+args[0];
                Client.main(args);
                System.exit(0);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(1);
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


