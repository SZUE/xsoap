/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HelloClient.java,v 1.4 2003/04/06 00:04:07 aslom Exp $
 */

package minimal;

import soaprmi.RemoteException;

/**
 * Simple client that executes remote method on server.
 *
 * @version $Revision: 1.4 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class HelloClient {

    public static void usage(String errMsg) {
        if(errMsg != null) System.err.println(errMsg);
        System.err.print(
            "Usage: location [arguments ...]\n"
                +"location \tit can be URL to connect to specified server \n"
                +"         \t  examples: http://localhost:9876/\n"
                +"arguments\tall remianing arguments will be used as strings\n"
                +"         \t and send to hello service\n"
                +"\nExample: http://locahost:9876 Foo\n"
        );
        System.exit(1);
    }

    public static void main (String args[]) throws RemoteException
    {

        HelloService serviceRef = null;
        String location;

        if(args.length == 0) {//usage("pass at least one argument");
            location = "http://localhost:"+HelloServer.DEFAULT_TCP_PORT;
        } else {
            if(args[0].startsWith("-h")) {
                usage(null);
            }
            location = args[0];
        }

        // create reference ot service
        serviceRef = (HelloService)
            soaprmi.soaprpc.SoapServices.getDefault().createStartpoint(
            location,  // service location
            new Class[]{HelloService.class}, // remote service interface
            "urn:hello:sample", // endpoint name
            soaprmi.soap.SoapStyle.SOAP11,
            "" // SOAPAction
        );


        // for each argument ask service to say hello ...
        int pos = 1;
        do {
            String arg = (pos < args.length ? args[pos++] : "World");
            System.out.println(
                "Client executing remote method sayHello on server with '"+arg+"' argument");
            String greeting = serviceRef.sayHello(arg);
            System.out.println("Server said '"+greeting+"'");
        } while(pos < args.length);

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

