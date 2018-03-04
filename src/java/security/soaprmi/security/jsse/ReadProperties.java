/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: ReadProperties.java,v 1.5 2003/04/06 00:04:09 aslom Exp $
 */

package soaprmi.security.jsse;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
//import sun.security.krb5.internal.crypto.e;
import soaprmi.util.logging.Logger;

/**
 * Simple class to read cog.properties
 *
 * @author Lavanya Ramakrishnan [mailto: laramakr@extreme.indiana.edu]
 * @author Aleksander A. Slominski [http://www.extreme.indiana.edu/~aslom/]
 */
public class ReadProperties{
    private static Logger logger = Logger.getLogger();

    private static String usercertpath;
    private static String proxypath;
    private static String[] cacertpath;

    public ReadProperties() throws IOException
    {
        //String userhome = System.getProperty("user.home");

        final String USER_HOME = "user.home";
        String userHome = null;
        try {
            userHome = (String) AccessController.doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            return System.getProperty(USER_HOME);
                        }
                    });
        } catch(AccessControlException ace) {
            logger.severe("could not read system property "+USER_HOME, ace);
            throw new IOException( "could not read system property "+USER_HOME+" no access: "+ace);
        }


        String file = userHome +"/.globus/cog.properties";
        String key, value;

        Properties p = new Properties();
        p.load(new FileInputStream(file));

        Enumeration enum = p.keys();

        while(enum.hasMoreElements()) {
            key   = enum.nextElement().toString();
            value = (String)p.get(key);
            if (key.equals("usercert")){
                usercertpath = value;
            }
            else if (key.equals("proxy")){
                proxypath=value;
            }
            else if (key.equals("cacert")){
                cacertpath = parseList(value);
            }
        }

    }

    private String[] parseList(String list) {
        Vector v = new Vector();
        int start = 0;
        int end = -1;
        while(true) {
            end = list.indexOf(',', start);
            if(end == -1) break;
            v.addElement(list.substring(start, end));
            start = end + 1;
        }
        v.addElement(list.substring(start));


        String[] ss = new String[v.size()];
        for (int i = 0; i < ss.length; i++)
        {
            ss[i] = (String) v.elementAt(i);
        }
        return ss;
    }
    public String getUserCertPath(){
        return usercertpath;

    }

    public String getProxyPath(){
        return proxypath;
    }

    public String[] getCaCertPath(){
        return cacertpath;
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


