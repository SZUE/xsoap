/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/

/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 * $Id: WsdlTest.java,v 1.7 2003/11/24 23:47:27 srikrish Exp $
 */

package wsdl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import soaprmi.util.dynamic_proxy.ThrowableRewrapper;
import soaprmi.util.logging.Logger;
import soaprmi.wsdl.WSDLUtil;
import soaprmi.server.RemoteRef;
import soaprmi.server.UnicastRemoteObject;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

/**
 * WSDL handling unit tests
 *
 */
public class WsdlTest extends TestCase {
    private static Logger logger = Logger.getLogger();

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
        System.exit(0);
    }

    public WsdlTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WsdlTest.class);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }
            
    public void testExample() throws Exception {

        // instantiate the test service
        if (Log.ON) logger.fine("Instantiating new service");
        TestService service = new TestServiceImpl();
        
        // export the service as a remote object
        if (Log.ON) logger.fine("Exporting service as remote object");
        RemoteRef remote1 = UnicastRemoteObject.exportObject(service);
        if (Log.ON) logger.fine("Remote reference: " + remote1);

        // invoke a method on the service
        if (Log.ON) logger.fine("Invoking the echoString method on the service");
        String retVal1 = 
            ((TestService) remote1).echoString("Test");
        if (Log.ON) logger.fine("Return value: " + retVal1);

        // convert the XSOAP reference to WSDL
        if (Log.ON) logger.fine("Converting reference to WSDL");
        String wsdlRef = WSDLUtil.convertRefToWSDL(remote1,
                                                   "TestService");
        if (Log.ON) logger.fine(wsdlRef);

        // convert WSDL back to XSOAP reference
        if (Log.ON) logger.fine("Converting WSDL to XSOAP reference");
        RemoteRef remote2 = WSDLUtil.convertWSDLToRef(wsdlRef);
        if (Log.ON) logger.fine("Remote reference: " + remote2);

        // make sure the hashCodes for the remote references are the same
        assertEquals("Hashcodes of the remote references are different",
                     remote1.hashCode(),
                     remote2.hashCode());

        // invoke the method with the same parameters
        if (Log.ON) logger.fine("Invoking the echoString method again");
        String retVal2 = 
            ((TestService) remote2).echoString("Test");
        if (Log.ON) logger.fine("Return value: " + retVal2);
        
        // make sure the return values are the same
        assertEquals("Return value different for regenerated reference", 
                     retVal1, 
                     retVal2);
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

