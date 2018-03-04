/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: XmlPullParserFactory.java,v 1.9 2004/05/06 18:18:45 aslom Exp $
 */

package org.gjt.xpp;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * This class is used to create implementations of  XML Pull Parser.
 * Based on JAXP ideas but tailored to work in J2ME environments
 * (no access to system properties or file system).
 *
 * @see XmlPullParser
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class XmlPullParserFactory
{
    private static final boolean DEBUG = false;

    public static final String DEFAULT_PROPERTY_NAME =
        "org.gjt.xpp.XmlPullParserFactory";

    //private static Class MY_CLASS;
    private static Object MY_REF = new XmlPullParserFactory();
    private static final String DEFAULT_FULL_IMPL_FACTORY_CLASS_NAME =
        "org.gjt.xpp.impl.PullParserFactoryFullImpl";
    private static final String DEFAULT_SMALL_IMPL_FACTORY_CLASS_NAME =
        "org.gjt.xpp.impl.PullParserFactorySmallImpl";
    private static final String DEFAULT_RESOURCE_NAME =
        "/META-INF/services/" + DEFAULT_PROPERTY_NAME;
    private static String foundFactoryClassName = null;
    private boolean namespaceAware;

    /**
     * Proteted constructor to be called by factory implementations.
     */
    protected XmlPullParserFactory()
    {
    }

    /**
     * Get a new instance of a PullParserFactory used to create XPP.
     */
    public static XmlPullParserFactory newInstance()
        throws XmlPullParserException
    {
        return newInstance(null, null);
    }

    /**
     * Get a new instance of a PullParserFactory from given class name.
     *
     * @param factoryClassName use specified factory class if not null
     */
    public static XmlPullParserFactory newInstance(String factoryClassName)
        throws XmlPullParserException
    {
        return newInstance( null, factoryClassName );
    }

    /**
     * Get a new instance of a PullParserFactory used to create XPP.
     * <p><b>NOTE:</b> passing classLoaderCtx is not very useful in ME
     *    but can be useful in container environment where
     *    multiple class loaders are used
     *    (it is using Class as ClassLoader is not in ME profile).
     *
     * @param classLoaderCtx if not null it is used to find
     *    default factory and to create instance
     */
    public static XmlPullParserFactory newInstance(Class classLoaderCtx)
        throws XmlPullParserException
    {
        return newInstance( classLoaderCtx, null );
    }

    private final static String PREFIX = "DEBUG XPP2 factory: ";

    private static void debug(String msg) {
        if(!DEBUG)
            throw new RuntimeException(
                "only when DEBUG enabled can print messages");
        System.err.println(PREFIX+msg);
    }

    /**
     * Get instance of XML pull parser factiry.
     *
     * <p><b>NOTE:</b>  this allows to use elegantly -D system properties or
     *    similar configuratin in ME environments..
     *
     * @param classLoaderCtx if null Class.forName will be used instead
     *    - simple way to use class loaders and still have ME compatibility!
     * @param hint with name of parser factory to use -
     *   it is a hint and is ignored if factory is not available.
     */
    private static XmlPullParserFactory newInstance(Class classLoaderCtx,
                                                    String factoryClassName)
        throws XmlPullParserException
    {

        // if user hinted factory then try to use it ...


        XmlPullParserFactory factoryImpl = null;
        if(factoryClassName != null) {
            try {
                //*
                Class clazz = null;
                if(classLoaderCtx != null) {
                    clazz = classLoaderCtx.forName(factoryClassName);
                } else {
                    clazz = Class.forName(factoryClassName);
                }
                factoryImpl = (XmlPullParserFactory) clazz.newInstance();
                //foundFactoryClassName = factoryClassName;
                //*/
                if(DEBUG) debug("loaded "+clazz);
            } catch  (Exception ex) {
                if(DEBUG) debug("failed to load "+factoryClassName);
                if(DEBUG) ex.printStackTrace();
            }
        }

        // if could not load then proceed with pre-configured
        if(factoryImpl == null) {

            // default factory is unknown - try to find it!
            if(foundFactoryClassName == null) {
                findFactoryClassName( classLoaderCtx );
            }

            if(foundFactoryClassName != null) {
                try {

                    Class clazz = null;
                    if(classLoaderCtx != null) {
                        clazz = classLoaderCtx.forName(foundFactoryClassName);
                    } else {
                        clazz = Class.forName(foundFactoryClassName);
                    }
                    factoryImpl = (XmlPullParserFactory) clazz.newInstance();
                    if(DEBUG) debug("loaded pre-configured "+clazz);
                } catch  (Exception ex) {
                    if(DEBUG) debug("failed to use pre-configured "
                                        +foundFactoryClassName);
                    if(DEBUG) ex.printStackTrace();
                }
            }
        }

        // still could not load then proceed with default
        if(factoryImpl == null) {
            try {
                Class clazz = null;
                factoryClassName = DEFAULT_FULL_IMPL_FACTORY_CLASS_NAME;
                // give one more chance for small implementation
                if(classLoaderCtx != null) {
                    clazz = classLoaderCtx.forName(factoryClassName);
                } else {
                    clazz = Class.forName(factoryClassName);
                }
                factoryImpl = (XmlPullParserFactory) clazz.newInstance();

                if(DEBUG) debug("using default full implementation "
                                    +factoryImpl);

                // make it as pre-configured default
                foundFactoryClassName = factoryClassName;

            } catch( Exception ex2) {
                try {
                    Class clazz = null;
                    factoryClassName = DEFAULT_SMALL_IMPL_FACTORY_CLASS_NAME;
                    // give one more chance for small implementation
                    if(classLoaderCtx != null) {
                        clazz = classLoaderCtx.forName(factoryClassName);
                    } else {
                        clazz = Class.forName(factoryClassName);
                    }
                    factoryImpl = (XmlPullParserFactory) clazz.newInstance();

                    if(DEBUG) debug("no factory was found instead "+
                                        "using small default impl "+factoryImpl);

                    // now it is pre-configured default
                    foundFactoryClassName = factoryClassName;

                } catch( Exception ex3) {
                    throw new XmlPullParserException(
                        "could not load any factory class "+
                            "(even small or full default implementation)", ex3);
                }
            }
        }

        // return what was found..
        if(factoryImpl == null) throw new RuntimeException(
                "XPP2: internal parser factory error");
        return factoryImpl;
    }

    // --- private utility methods

    private static void findFactoryClassName( Class classLoaderCtx )
    {
        if(foundFactoryClassName != null) //return; // foundFactoryClassName;
            throw new RuntimeException("internal XPP2 initialization error");

        InputStream is = null;
        try {

            if(classLoaderCtx != null) {
                if(DEBUG) debug(
                        "trying to load "+DEFAULT_RESOURCE_NAME+
                            " from "+classLoaderCtx);
                is = classLoaderCtx.getResourceAsStream( DEFAULT_RESOURCE_NAME );
            }

            if(is == null) {
                Class klass = MY_REF.getClass(); //XmlPullParserFactory.getClass();
                if(DEBUG) debug(
                        "opening "+DEFAULT_RESOURCE_NAME+
                            " (class context "+klass+")");
                is = klass.getResourceAsStream( DEFAULT_RESOURCE_NAME );
            }

            if( is != null ) {

                foundFactoryClassName = readLine( is );

                if( DEBUG ) debug(
                        "foundFactoryClassName=" + foundFactoryClassName );

                //if( foundFactoryClassName != null
                //   && !  "".equals( foundFactoryClassName ) ) {
                //  return foundFactoryClassName;
                //}
            }
        } catch( Exception ex ) {
            if( DEBUG ) ex.printStackTrace();
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(Exception ex) {
                }
            }
        }
        //return foundFactoryClassName;
    }

    private static String readLine(InputStream input) throws IOException
    {
        StringBuffer sb = new StringBuffer();

        while (true) {
            int ch = input.read();
            if (ch < 0) {
                break;
            } else if (ch == '\n') {
                break;
            }
            sb.append((char) ch);
        }

        // strip end-of-line \r\n if necessary
        int n = sb.length();
        if ((n > 0) && (sb.charAt(n - 1) == '\r')) {
            sb.setLength(n - 1);
        }

        return (sb.toString());
    }

    /**
     * Specifies that the parser produced by this factory will provide
     * support for XML namespaces.
     * By default the value of this is set to false.
     *
     * @param awareness true if the parser produced by this code
     *    will provide support for XML namespaces;  false otherwise.
     */
    public void setNamespaceAware(boolean awareness)
        throws XmlPullParserException
    {
        namespaceAware = awareness;
    }

    /**
     * Indicates whether or not the factory is configured to produce
     * parsers which are namespace aware.
     *
     * @return  true if the factory is configured to produce parsers
     *    which are namespace aware; false otherwise.
     */
    public boolean isNamespaceAware()
    {
        return namespaceAware;
    }

    /**
     * Create new XML pull parser.
     */
    public XmlPullParser newPullParser() throws XmlPullParserException {
        throw new XmlPullParserException("newPullParser() not implemented");
    }

    /**
     * Create new end tag.
     */
    public XmlEndTag newEndTag() throws XmlPullParserException {
        throw new XmlPullParserException("newEndTag() not implemented");
    }

    /**
     * Return new XML node.
     */
    public XmlNode newNode() throws XmlPullParserException {
        throw new XmlPullParserException("newNode() not implemented");
    }

    /**
     * Return new XML node that is represeting tree from current pull parser start tag.
     */
    public XmlNode newNode(XmlPullParser pp)
        throws XmlPullParserException, IOException
    {
        XmlNode node = newNode();
        pp.readNode(node);
        return node;
    }

    /**
     * Return new XML pull node that is represeting tree from current pull parser start tag.
     */
    public XmlPullNode newPullNode(XmlPullParser pp)
        throws XmlPullParserException
    {
        throw new XmlPullParserException("newPullNode() not implemented");
    }

    /**
     * Return new XML start tag.
     */
    public XmlStartTag newStartTag() throws XmlPullParserException {
        throw new XmlPullParserException("newStartTag() not implemented");
    }

    /**
     * Return new XML formatter.
     */
    public XmlFormatter newFormatter() throws XmlPullParserException {
        throw new XmlPullParserException("newFormatter() not implemented");
    }


    /**
     * Return new XML recorder.
     */
    public XmlRecorder newRecorder() throws XmlPullParserException {
        throw new XmlPullParserException("newRecorder() not implemented");
    }

    // some utility methods as requested by users


    /**
     * Read XmlNode from input - essentially it is utility function that
     * will create instance of pull parser, feed input inpt it and
     * return new node tree parsed form the input.
     * If closeAtEnd is true clos() will be called on reader
     *
     */
    public XmlNode readNode(Reader reader, boolean closeAtEnd)
        throws XmlPullParserException, IOException
    {
        XmlPullParser pp = newPullParser();
        pp.setInput(reader);
        byte event = pp.next();
        XmlNode doc = newNode();
        if(event == XmlPullParser.START_TAG) {
            pp.readNode(doc);
        } else {
            if(event != XmlPullParser.END_DOCUMENT) {
                throw new XmlPullParserException(
                    "coul dnot read node tree from input, unexpected parser state"+pp.getPosDesc());
            }
        }
        if(closeAtEnd) {
            reader.close();
        }
        return doc;
    }

    /**
     * Equivalent to calling readNode(reader, false);
     */
    public XmlNode readNode(Reader reader)
        throws XmlPullParserException, IOException
    {
        return readNode(reader, false);
    }

    public void writeNode(XmlNode node, Writer writer, boolean closeAtEnd)
        throws XmlPullParserException, IOException
    {
        XmlRecorder recorder = newRecorder();
        recorder.setOutput(writer);
        recorder.writeNode(node);
        if(closeAtEnd) {
            writer.close();
        }
    }

    /**
     * Equivalent to calling writeNode(node, writer, false);
     */
    public void writeNode(XmlNode node, Writer writer)
        throws XmlPullParserException, IOException
    {
        writeNode(node, writer, false);
    }

}


/*
 * Indiana University Extreme! Lab Software License, Version 1.2
 *
 * Copyright (c) 2002-2004 The Trustees of Indiana University.
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

