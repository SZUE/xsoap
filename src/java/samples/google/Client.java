/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Client.java,v 1.5 2003/04/06 00:04:04 aslom Exp $
 */

package google;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.FileWriter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

//import org.gjt.xpp.XmlNode;
//import org.gjt.xpp.XmlPullParser;
//import org.gjt.xpp.XmlPullParserFactory;

import soaprmi.Remote;
import soaprmi.RemoteException;
import soaprmi.mapping.*;

public class Client {

    public static void main(String[] args) throws Exception {

        if(args.length < 1){
            System.out.println("usage:  run.sh google.Client \"word list\"");
            //System.out.println("hello");
            //System.currentTimeMillis();
            System.exit(0);

        }
        XmlJavaMapping mapping =
            soaprmi.soap.Soap.getDefault().getMapping();
        // disable XSOAP auto mapping
        mapping.setDefaultStructNsPrefix(null);

        fixNames(mapping);


        // get reference ot service - from comand line URL
        String location = "http://api.google.com:80/search/beta2";
        GoogleSearch gs = (GoogleSearch)
            soaprmi.soaprpc.SoapServices.getDefault().createStartpoint(
            location,  // service location
            new Class[]{GoogleSearch.class}, // remote service interface
            "urn:GoogleSearch", // endpoint name
            soaprmi.soap.SoapStyle.IBMSOAP,
            "" // SOAPAction
        );

        // put google key here. (see: http://www.google.com/apis/ )
        String key = getGoogleKey();
        String q = args[0];
        int start = 0;
        int maxResults = 2;
        boolean filter = true;
        String restrict = "";
        boolean safeSearch = false;
        String lr = "";
        String ie = "latin1";
        String oe = "latin1";


        System.out.println("contacting gooogle service "+gs);
        GoogleSearchResult res = gs.doGoogleSearch(key,q,start,maxResults,filter,
                                                   restrict,safeSearch,lr,ie,oe);
        System.out.println("call returned");
        ResultElement[] rea = res.getResultElements();
        for( int i = 0; i < rea.length; i++){
            System.out.println("-----------------------------------------------");
            System.out.println("   hit: " + rea[i].getSnippet() );
            System.out.println("   URL: " + rea[i].getURL() );
        }
        System.out.println("\n done.");
    }

    public static String getGoogleKey() {
        // try to load key from file
        String key = null;
        //key = "--------------------------------"; //you can hardcode Google key value here
        if(key == null) {
            try {
                Properties props = new Properties();
                String propertyFile = "src/java/samples/google/google_key.txt";
                try {
                    props.load(new FileInputStream(propertyFile));
                } catch(IOException ex) {
                    // try again with new loacation ...
                    propertyFile = "google_key.txt";
                    props.load(new FileInputStream(propertyFile));
                }
                key = props.getProperty("google_key");
                if(key == null) {
                    throw new RuntimeException("property file "+propertyFile+
                                                   " must cotain google_key");
                }
            } catch(IOException ioe) {
                throw new RuntimeException("could not load google key from property file" + ioe);
            }
        }
        return key;
    }

    public static void fixNames(XmlJavaMapping javaMapping) throws XmlMapException {
        XmlJavaPortTypeMap portMap =
            javaMapping.queryPortType(GoogleSearch.class);

        // extracxt mapping for operation
        XmlJavaOperationMap oMap = portMap.queryMethodRequest("doGoogleSearch");

        // get in maessage and change part names
        XmlJavaMessageMap requestMsg = oMap.getRequest();
        XmlJavaPartMap[] reqParts = requestMsg.getParts();
        reqParts[0].setPartName("key");
        reqParts[1].setPartName("q");
        reqParts[2].setPartName("start");
        reqParts[3].setPartName("maxResults");
        reqParts[4].setPartName("filter");
        reqParts[5].setPartName("restrict");
        reqParts[6].setPartName("safeSearch");
        reqParts[7].setPartName("lr");
        reqParts[8].setPartName("ie");
        reqParts[9].setPartName("oe");

        // map the structures for the return objects

        javaMapping.mapStruct(
            "urn:GoogleSearch" /*NAMESPACE URI*/,
            "GoogleSearchResult" /* NAMESPACE LOCAL*/,
            GoogleSearchResult.class /* Java Bean class */
        );

        javaMapping.mapStruct(
            "urn:GoogleSearch" /*NAMESPACE URI*/,
            "DirectoryCategory" /* NAMESPACE LOCAL*/,
            DirectoryCategory.class /* Java Bean class */
        );

        javaMapping.mapStruct(
            "urn:GoogleSearch" /*NAMESPACE URI*/,
            "ResultElement" /* NAMESPACE LOCAL*/,
            ResultElement.class /* Java Bean class */
        );


        // get out message and change name of output
        // XmlJavaMessageMap responseMsg = oMap.getResponse();
        // XmlJavaPartMap[] resParts = responseMsg.getParts();
        // resParts[0].setPartName("result");


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

