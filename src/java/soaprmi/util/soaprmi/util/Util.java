/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Util.java,v 1.8 2004/05/06 18:18:48 aslom Exp $
 */

package soaprmi.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlPullParserFactory;
import soaprmi.util.logging.Logger;

/**
 * The bag for useful staff.
 *
 * @version $Revision: 1.8 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class Util {
    private static Logger logger = Logger.getLogger();
    private static Object lockFactory = new Object();
    private static XmlPullParserFactory factory;
    
    public static final boolean equalsRef(Object s1, Object s2) {
        return ((s1 != null && s1.equals(s2)) || (s1 == null && s1 == s2));
    }
    
    public static final boolean equalsArrays(Object s1, Object s2) {
        boolean result = false;
        if(s1.getClass().isArray()) {
            result = Arrays.equals((double[])s1, (double[])s2);
        } else {
            result = equalsRef(s1, s2);
        }
        return result;
    }
    
    public static void setPullParserFactory(XmlPullParserFactory userFactory) {
        synchronized(lockFactory) {
            factory = userFactory;
        }
    }
    
    public static XmlPullParserFactory getPullParserFactory() {
        synchronized(lockFactory) {
            if(factory == null) {
                try {
                    factory = XmlPullParserFactory.newInstance();
                } catch (XmlPullParserException e) {
                    throw new RuntimeException("could not load XPP2 factory"+e, e);
                }
            }
            return factory;
        }
    }
    
    private static ClassLoader classLoader;
    
    public static void setClassloader(ClassLoader classLoaderToUse) throws Exception {
        classLoader = classLoaderToUse;
    }
    
    public static Class loadClass(String klassName) throws Exception {
        //ClassLoader cl = Thread.currentThread().getContextClassLoader(); //Class.forName(klassName);
        
        ClassLoader cl = null;
        if(classLoader == null) {
            try {
                cl = (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
                            public Object run() {
                                return Thread.currentThread().getContextClassLoader();
                            }
                        });
            } catch(AccessControlException ace) {
                
                logger.severe("could not get context class loader", ace);
                
            }
        } else {
            cl= classLoader;
        }
        
        Class klass = cl != null ? cl.loadClass(klassName) : Class.forName(klassName);
        return klass;
    }
    
    public static final void writeXMLEscapedString(Writer out, String s)
        throws IOException
    {
        //TODO Faster should be CDATA for s.length() > 1000??? indexOf("]]>")...
        //TODO can CDATA handle unnormalized new lines (\r)???
        if(s == null) {
            throw new IllegalArgumentException("can't serialize null string value");
        }
        // optimized for most common case when escaping is unnecessary or minimal
        int ltPos = s.indexOf('<');
        int breakPos = -2;
        int ampPos = -2;
        if( ltPos != -1
               || ((breakPos = s.indexOf('\r')) != -1 )
               || ((ampPos = s.indexOf('&')) != -1 )
          )
        {
            int i = 0;
            int len = s.length();
            if(breakPos == -2) breakPos = s.indexOf('\r');
            if(ampPos == -2) ampPos = s.indexOf('&');
            if(ltPos == -1) ltPos = len;
            if(breakPos == -1) breakPos = len;
            if(ampPos == -1) ampPos = len;
            //System.err.println("len="+len+" s='"+s+"'");
            while(ltPos < len || breakPos < len || ampPos < len) {
                //System.err.println("i="+i+" ltPos="+ltPos+" breakPos="+breakPos
                //  +" ampPos="+ampPos);
                if(ltPos < breakPos && ltPos < ampPos) {
                    out.write(s.substring(i, ltPos));
                    out.write("&lt;");
                    i = ltPos + 1;
                    ltPos = s.indexOf('<', i);
                    if(ltPos == -1) ltPos = len;
                } else if(breakPos < ampPos) {
                    out.write(s.substring(i, breakPos));
                    out.write("&#xD;");
                    i = breakPos + 1;
                    breakPos = s.indexOf('\r', i);
                    if(breakPos == -1) breakPos = len;
                } else {
                    out.write(s.substring(i, ampPos));
                    out.write("&amp;");
                    i = ampPos + 1;
                    ampPos = s.indexOf('&', i);
                    if(ampPos == -1) ampPos = len;
                }
            }
            //System.err.println("final i="+i);
            if(i < len)
                out.write(s.substring(i));
        } else {
            out.write(s);
        }
    }
    
    
    public static final void readXMLEscapedString(Writer out,
                                                  String s)
        throws IOException
    {
        
        if(s == null) {
            throw new IllegalArgumentException("can't deserialize null string value");
        }
        
        int ltPos = s.indexOf("&lt;");
        int breakPos = s.indexOf("&#xD;");
        int ampPos = s.indexOf("&amp;");
        
        if( (ltPos != -1) ||
               (breakPos != -1 ) ||
               (ampPos != -1 ))
        {
            int i = 0;
            int len = s.length();
            if(ltPos == -1) ltPos = len;
            if(breakPos == -1) breakPos = len;
            if(ampPos == -1) ampPos = len;
            
            //System.err.println("len="+len+" s='"+s+"'");
            
            while(ltPos < len || breakPos < len || ampPos < len) {
                //System.err.println("i="+i+" ltPos="+ltPos+" breakPos="+breakPos
                //         +" ampPos="+ampPos);
                if(ltPos < breakPos && ltPos < ampPos) {
                    out.write(s.substring(i, ltPos));
                    out.write('<');
                    i = ltPos + 4;
                    ltPos = s.indexOf("&lt;", i);
                    if(ltPos == -1) ltPos = len;
                } else if(breakPos < ampPos) {
                    out.write(s.substring(i, breakPos));
                    out.write('\r');
                    i = breakPos + 5;
                    breakPos = s.indexOf("&#xD;", i);
                    if(breakPos == -1) breakPos = len;
                } else {
                    out.write(s.substring(i, ampPos));
                    out.write('&');
                    i = ampPos + 5;
                    ampPos = s.indexOf("&amp;", i);
                    if(ampPos == -1) ampPos = len;
                }
            }
            //System.err.println("final i="+i);
            if(i < len)
                out.write(s.substring(i));
        } else {
            out.write(s);
        }
        
        out.flush();
        //System.err.println("final out="+out.toString());
    }
    
    public static String getContentTypeCharset(String contentType) {
        return getContentTypeCharset(contentType, null);
    }
    
    public static String getContentTypeCharset(String contentType,
                                               String defaultCharset)
    {
        String charset = null;
        if(contentType != null) {
            int ndx = contentType.indexOf("charset=");
            logger.finest("ndx="+ndx
                              +" from contentType="+contentType);
            if(ndx != -1) {
                ndx += "charset=".length();
                char c = contentType.charAt(ndx);
                if(c == '\'' || c == '\"') {
                    int ndx2 = contentType.indexOf(c, ndx+1);
                    if(ndx2 == -1) {
                        //throw new RemoteException(
                        //  "content type header '"+contentType+"'"
                        //    +" has malformed charset");
                        logger.warning("could not get charset"
                                           +" from contentType="+contentType);
                        return null;
                    }
                    charset = contentType.substring(ndx+1, ndx2);
                } else {
                    charset = contentType.substring(ndx);
                }
                return charset;
            }
        }
        return defaultCharset;
    }
    
    /**
     * Try to get input stream to load from location
     * by checking class loader, files & URLs.
     */
    public static InputStream inputStreamFor(Class klass, String location)
        throws IOException
    {
        InputStream in = null;
        try {
            try {
                in = new FileInputStream( location );
            } catch(IOException ex) {
                in = klass.getResourceAsStream( location );
                if(in  == null && location != null && location.startsWith("/")) {
                    in = klass.getResourceAsStream("/" +  location );
                }
            }
            if(in == null) {
                URL url = new URL(location);
                in = url.openStream();
            }
            //} catch(java.net.MalformedURLException ex) {
            //  l.config("can't load data from "+location, ex);
            //  throw new IOException("can't load "+location+":"+ex);
        } catch(IOException ex) {
            logger.config("can't load data from "+location, ex);
            throw new IOException("can't load "+location+":"+ex);
        }
        return in;
        //throw new UnsupportedOperationException("not implemented yet");
    }
    
    public static Reader readerFor(Class klass, String location)
        throws IOException
    {
        return new InputStreamReader(inputStreamFor(klass, location));
    }
    
    public static String loadAsString(Class klass, String location)
        throws IOException
    {
        //InputStream in = inputStreamFor(klass, name);
        //Reader reader = new InputStreamReader(
        //  new BufferedInputStream(in)
        //);
        Reader reader = new BufferedReader(readerFor(klass, location));
        StringWriter sink = new StringWriter();
        int i;
        while((i = reader.read()) != -1) {
            sink.write((char)i);
        }
        //while((i = reader.read(readBuf)) > 0) {
        //  sink.write(readBuf, 0 ,i);
        //}
        return sink.toString();
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

