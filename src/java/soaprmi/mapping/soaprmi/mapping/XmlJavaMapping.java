/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: XmlJavaMapping.java,v 1.17 2003/05/18 13:16:05 aslom Exp $
 */

package soaprmi.mapping;

import java.util.HashMap;
import java.util.Map;
import soaprmi.util.Util;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

/**
 * Keep track of all XML - Java mappings.
 *
 * @version $Revision: 1.17 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class XmlJavaMapping
{
    private static final boolean DETAILED_TRACE = Log.ON && false;

    public XmlJavaMapping() {
    }

    public void setDefaultStructType(String defaultStructType) {
        this.defaultStructType = defaultStructType;
    }

    public void setDefaultStructNsPrefix(String value) {
        this.defaultStructNsPrefix = value;
    }

    public void setDefaultPortTypeNsPrefix(String value) {
        logger.fine("called. value = " + value);
        this.defaultPortTypeNsPrefix = value;
    }

    public void setDefaultArrayComponentInterfaceNsPrefix(String value) {
        logger.entering(value);
        this.defaultArrayComponentInterfaceNsPrefix = value;
    }

    public void connectTo(XmlJavaMapping sink_) {
        if(sink != sink_) {
            this.sink = sink_;
            if(DETAILED_TRACE) logger.finest("new sink="+sink);
        } else {
            this.sink = null;
            logger.warning("disabled SELF looped sink="+sink);
        }
    }

    public void aliasNamespaces(String encodingStyle,
                                String uri, String otherUri, boolean force)
        throws XmlMapException
    {
        Map uriMap = uriMap(encodingStyle);
        if(!force) {
            Map otherLocalMap = (Map) uriMap.get(otherUri);
            if(otherLocalMap != null) {
                throw new XmlMapException(
                    otherUri+" must not be defined to allow aliasing from "+uri
                );
            }
        }
        Map localMap = (Map) uriMap.get(uri);
        if(localMap == null) {
            throw new XmlMapException(
                uri+" must be already mapped to something to allow to be aliased"
                    +" to other namespace "+otherUri
                    +" (encodingStyle="+encodingStyle+")");
        }
        uriMap.put(otherUri, localMap);
    }


    //public void setJavaPortTypeMapping(XmlJavaPortTypeMap[] portTypeMap)
    // throws XmlMapException {
    //  throw new XmlMapException("ports mapping not implemented");
    //}

    //public void setJavaTypeMapping(XmlJavaTypeMap[] structMap)
    // throws XmlMapException {
    //  valid = false;
    //  this.typeMap = typeMap;
    //}

    //TODO getJavaTypeMap, getJavaPortTypeMap


    // --- ports / operations mappings

    public XmlJavaPortTypeMap mapPortType(String uri, String localName,
                                          Class javaClass)
        throws XmlMapException
    {
        return mapPortType(uri, localName, javaClass, false);
    }
    public XmlJavaPortTypeMap mapPortType(String uri, String localName, Class javaClass,
                                          boolean override)
        throws XmlMapException
    {
        return mapPortType(uri, localName, javaClass, null, override);
    }

    public XmlJavaPortTypeMap mapPortType(String uri, String localName,
                                          Class javaClass, XmlJavaOperationMap[] operationsMap,
                                          boolean override)
        throws XmlMapException
    {
        if(javaClass.isInterface() == false)
            throw new XmlMapException(
                javaClass+" must be java interface for port type "
                    +" uri="+uri+" localName="+localName);
        XmlJavaPortTypeMap portMap = new XmlJavaPortTypeMap();
        portMap.setUri(uri);
        portMap.setLocalName(localName);
        portMap.setJavaClass(javaClass);
        if (operationsMap != null) {
            portMap.setOperations(operationsMap);
        }
        return mapPortType(portMap, override);
    }

    public XmlJavaPortTypeMap mapPortType(XmlJavaPortTypeMap portMap,
                                          boolean override) throws XmlMapException
    {
        String uri = portMap.getUri();
        if(uri == null)
            throw new IllegalArgumentException("Port type uri can not be null");
        Map local2PortMap = (Map) xml2PortMap.get(uri);
        if(local2PortMap == null) {
            local2PortMap = new HashMap();
            xml2PortMap.put(uri, local2PortMap);
        }
        String localName = portMap.getLocalName();
        if(!override && local2PortMap.get(localName) != null)
            throw new XmlMapException("portType map uri="+uri+" localName="+localName
                                          +" already exist for port type map"
                                          +portMap);
        local2PortMap.put(localName, portMap);
        Class javaInterface = portMap.javaClass();
        if(!override && java2PortMap.get(javaInterface)  != null)
            throw new XmlMapException("portType map javaType="+portMap.getJavaType()
                                          +" already exist for port type map"
                                          +portMap);
        java2PortMap.put(javaInterface, portMap);
        return portMap;
    }

    /**
     * Query for mapping for QName (uri, localName)
     * <p>Note: throws exception if there is no mapping.
     */
    public XmlJavaPortTypeMap queryPortType(String uri, String localName)
        throws XmlMapException
    {
        XmlJavaPortTypeMap portMap = null;
        Map local2PortMap = (Map) xml2PortMap.get(uri);
        if(local2PortMap != null) {
            portMap =  (XmlJavaPortTypeMap) local2PortMap.get(localName);
        }
        if(portMap == null) {
            if(sink != null) {
                return sink.queryPortType(uri, localName);
            }

            if((defaultPortTypeNsPrefix != null) && defaultPortTypeNsPrefix.equals(uri)) {
                Class javaInterface;
                try {
                    //ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    //javaInterface = cl != null ? cl.loadClass(localName) : Class.forName(localName);
                    javaInterface = Util.loadClass(localName);
                } catch(Exception ex) {
                    throw new XmlMapException(
                        "can't load java class "+localName
                            +" for automatic port type mapping for "
                            +" uri="+uri+" localName="+localName);
                }
                return mapPortType(uri, localName,
                                   javaInterface, null,
                                   true //override - to avoid race condition
                                  );
            }
            throw new XmlMapException(
                "no port type maping for "+uri+":"+localName);
        }
        //if(Check.ON) Check.assert(portMap != null);
        return portMap;
    }

    public XmlJavaPortTypeMap queryPortType(Class javaInterface)
        throws XmlMapException
    {
        XmlJavaPortTypeMap portMap =
            (XmlJavaPortTypeMap) java2PortMap.get(javaInterface);
        if(portMap == null) {
            if(sink != null)
                return sink.queryPortType(javaInterface);
            //throw new XmlMapException("no port type maping for "+javaInterface);
            String uri = defaultPortTypeNsPrefix;
            String localName = javaInterface.getName();
            return mapPortType(uri, localName,
                               javaInterface, null,
                               true //override
                              );
        }
        //if(Check.ON) Check.assert(portMap != null);
        return portMap;
    }

    //Method operationInput(uri, localName, requestName)
    //String operationOutput(uri, localName, Method)
    // Method mapXmlObjectMethod(String uri, String methodName);

    // --- type mapping

    /**
     * Query for mapping for given java class that is associated
     * with encodingStyle (null can be passed to indicate default noencoding).
     * <p>Note: throws exception if there is no mapping.
     */
    public XmlJavaTypeMap queryTypeMap(String encodingStyle,
                                       Class javaClass)
        throws XmlMapException
    {
        if(HEAVY_TRACING) {
            logger.finest("called javaClass="+javaClass.getName()
                         +" encodingStyle="+encodingStyle);
        }
        XmlJavaTypeMap sa = typeMap(encodingStyle, javaClass);
        //if(!valid) remap();


        if(sa == null) {
            if(sink != null)
                sa = sink.queryTypeMap(encodingStyle, javaClass);
        }

        if(sa == null && defaultStructNsPrefix != null) {

            sa = autoMapJavaClass(javaClass);

        }
        if(sa == null) {
            throw new XmlMapException("no mapping for encodingStyle="
                                          +encodingStyle
                                          +" javaClass="+javaClass);
        }
        if(HEAVY_TRACING) {
            logger.finest("query javaClass="+javaClass.getName()
                         +" encodingStyle="+encodingStyle
                         +" returned "+sa);
        }
        return sa;
    }

    private XmlJavaTypeMap autoMapJavaClass(Class javaClass) throws XmlMapException {
        return autoMapJavaClass(javaClass, false);
    }

    private XmlJavaTypeMap autoMapJavaClass(Class javaClass, boolean simpleType)
        throws XmlMapException
    {
        if(javaClass.isArray()) {
            throw new XmlMapException(
                "array type "+javaClass.getName()+" can not be auto mapped to structs...");
            //return queryTypemap(SOAP_ENC_NS, SOAP_ENC_NS, "Array"
        }
        if(javaClass.isPrimitive()) {
            throw new XmlMapException(
                "primitive type "+javaClass+" can not be auto mapped to structs...");
            //return queryTypemap(SOAP_ENC_NS, SOAP_ENC_NS, "Array"
        }

        if(javaClass.isInterface()) {
            throw new XmlMapException(
                "interface "+javaClass.getName()+" can not be auto mapped to structs...");
            //return queryTypemap(SOAP_ENC_NS, SOAP_ENC_NS, "Array"
        }
        if(defaultStructNsPrefix == null) {
            throw new XmlMapException("default struct ns prefix must be not null for automapping");
        }

        //Package pkg = javaClass.getPackage();
        //String packageName = pkg != null ? pkg.getName() : null; //TODO: profile it!
        //String klassName = javaClass.getName();
        //klassName = klassName.substring(klassName.lastIndexOf('.')+1);
        //String uri = defaultStructNsPrefix
        //  + (packageName != null ? ':' + packageName : "");
        // create automatic mapping - good for java RMI system
        //TODO if javaClass.isArray()

        //XmlJavaTypeMap sa = typeMap(encodingStyle, javaClass);
        String uri = defaultStructNsPrefix;
        String klassName = javaClass.getName();
        XmlJavaTypeMap sa = mapStruct(null, uri, klassName, javaClass,
                                      defaultStructType,
                                      null,
                                      simpleType, // simpleType
                                      true,  // generated
                                      true //override
                                     );
        return sa;
    }


    public XmlJavaTypeMap autoMapArrayComponentInterface(Class kompType) throws XmlMapException {
        if(kompType.isInterface() == false) {
            throw new XmlMapException(
                "only interface can be auto mapped for array element not "+kompType);
            //return queryTypemap(SOAP_ENC_NS, SOAP_ENC_NS, "Array"
        }
        if(defaultArrayComponentInterfaceNsPrefix == null) {
            throw new XmlMapException(
                "default array component interface ns prefix must be not null for automapping");
        }
        String uri = defaultArrayComponentInterfaceNsPrefix;
        String klassName = kompType.getName();
        return new XmlJavaTypeMap(uri, klassName, kompType);
    }

    public XmlJavaTypeMap queryTypeMap(String encodingStyle,
                                       String uri, String localName)
        throws XmlMapException
    {
        if(HEAVY_TRACING) {
            logger.finest("called localName="+localName+" uri="+uri
                         +" encodingStyle="+encodingStyle);
        }
        if(uri == null)
            throw new IllegalArgumentException("uri can not be null");
        //if(!valid) remap();
        XmlJavaTypeMap sa = typeMap(encodingStyle, uri, localName);

        if(sa == null) {
            if(sink != null)
                sa = sink.queryTypeMap(encodingStyle, uri, localName);
        }

        // restore automatic mapping of java classes
        if(sa == null) {
            if(defaultStructNsPrefix != null && uri.startsWith(defaultStructNsPrefix)) {
                //String packageName = null;
                //if(uri.length() > defaultStructNsPrefix.length())
                //  packageName = uri.substring(defaultStructNsPrefix.length()+1);
                String klassName = localName;
                Class javaClass;
                try {
                    javaClass = Class.forName(
                        //(packageName != null ? packageName+'.' : "")+
                        klassName);
                } catch(ClassNotFoundException ex) {
                    throw new XmlMapException(
                        "can't create automatic struct mapping for "
                            +" uri="+uri+" localName="+localName);
                }
                // created automatic mapping - good for java RMI system
                //TODO if javaClass.isArray()
                sa = mapStruct(null, uri, klassName, javaClass,
                               defaultStructType, null, false, true,
                               true //override
                              );
            }
            if(defaultArrayComponentInterfaceNsPrefix != null
               && uri.startsWith(defaultArrayComponentInterfaceNsPrefix)) {
                String klassName = localName;
                Class javaClass;
                try {
                    javaClass = Class.forName(
                        //(packageName != null ? packageName+'.' : "")+
                        klassName);
                } catch(ClassNotFoundException ex) {
                    throw new XmlMapException(
                        "can't create automatic array java component mapping for "
                            +" uri="+uri+" localName="+localName);
                }
                // created automatic mapping - good for java RMI system
                //TODO if javaClass.isArray()
                sa = new XmlJavaTypeMap(uri, klassName, javaClass);
            }
        }
        if(sa == null) {
            throw new XmlMapException("no mapping for encodingStyle="+encodingStyle
                                          +" uri="+uri+" localName="+localName);
        }
        if(HEAVY_TRACING) {
            logger.finest("query localName="+localName+" uri="+uri
                         +" encodingStyle="+encodingStyle
                         +" returned "+sa);
        }
        return sa;
    }

    public XmlJavaTypeMap mapType(
        String uri,
        String localName,
        Class javaClass)
        throws XmlMapException
    {
        return mapType(null, uri, localName, javaClass);
    }


    public XmlJavaTypeMap mapType(
        String uri,
        String localName,
        Class javaClass,
        boolean override)
        throws XmlMapException
    {
        return mapType(null, uri, localName, javaClass,
                       true, false, override);

    }

    public XmlJavaTypeMap mapType(
        String encodingStyle,
        String uri,
        String localName,
        Class javaClass)
        throws XmlMapException
    {
        return mapType(encodingStyle, uri, localName, javaClass,
                       true, false, false);
    }

    public XmlJavaTypeMap mapType(
        String encodingStyle,
        String uri,
        String localName,
        Class javaClass,
        boolean override)
        throws XmlMapException
    {
        return mapType(encodingStyle, uri, localName, javaClass,
                       true, false, override);
    }

    public XmlJavaTypeMap mapType(
        String encodingStyle,
        String uri,
        String localName,
        Class javaClass,
        boolean simpleType,
        boolean generated,
        boolean override)
        throws XmlMapException
    {
        XmlJavaTypeMap typemap = new XmlJavaTypeMap();
        typemap.setEncodingStyle(encodingStyle);
        typemap.setUri(uri);
        typemap.setLocalName(localName);
        typemap.setSimpleType(simpleType);
        typemap.makeGenerated(generated);
        typemap.javaClass(javaClass); //javaClass.getName());
        return mapType(typemap, override);
    }


    public XmlJavaTypeMap mapType(XmlJavaTypeMap typemap, boolean override)
        throws XmlMapException
    {
        if(typemap.getUri() == null)
            throw new XmlMapException("struct uri can't be null");
        if(typemap.getLocalName() == null)
            throw new XmlMapException("struct localName can't be null");
        if(typemap.javaClass() == null)
            throw new XmlMapException("struct javaClass can't be null");


        Map localMap = localMap(typemap.getEncodingStyle(), typemap.getUri());
        Map javaMap =  javaMap(typemap.getEncodingStyle());

        if(!override) {
            Object o;
            if((o = javaMap.get( typemap.javaClass() )) != null) {
                throw new XmlMapException("type javaClass="+typemap.javaClass()
                                              +" is already mapped to "+o);
            }
            if((o = localMap.get( typemap.getLocalName() )) != null) {
                throw new XmlMapException("type uri="+typemap.getUri()
                                              +" localName="+typemap.getLocalName()
                                              +"is already mapped to "+o);
            }
        }

        if(DETAILED_TRACE) {
            logger.finest("mapping "+typemap.javaClass().getName()
                         +" encodingStyle="+typemap.getEncodingStyle()
                         +" to "+typemap);
        }
        javaMap.put(typemap.javaClass(), typemap);

        if(DETAILED_TRACE) {
            logger.finest("mapping "+typemap.getUri()+":"+typemap.getLocalName()
                         +" encodingStyle="+typemap.getEncodingStyle()
                         +" to "+typemap);
        }
        localMap.put(typemap.getLocalName(), typemap);

        return typemap;
    }


    public XmlJavaStructMap mapStruct(
        String uri,
        String localName,
        Class javaClass)
        throws XmlMapException
    {
        return mapStruct(null, uri, localName,
                         javaClass, null, null, false, false, false);
    }

    public XmlJavaStructMap mapStruct(
        String encodingStyle,
        String uri,
        String localName,
        Class javaClass)
        throws XmlMapException
    {
        return mapStruct(encodingStyle, uri, localName,
                         javaClass, null, null, false, false, false);
    }



    public XmlJavaStructMap mapStruct(
        String encodingStyle,
        String uri,
        String localName,
        Class javaClass,
        String structType,
        XmlJavaAccessorMap[] accessorsMap,
        boolean simpleType,
        boolean generated,
        boolean override)
        throws XmlMapException
    {
        XmlJavaStructMap struct = new XmlJavaStructMap();
        struct.setEncodingStyle(encodingStyle);
        struct.setUri(uri);
        struct.setLocalName(localName);
        //struct.setJavaType(javaClass.getName());
        struct.javaClass(javaClass);
        struct.setStructType(structType);
        struct.setAccessors(accessorsMap);
        struct.setSimpleType(simpleType);
        struct.makeGenerated(generated);
        return (XmlJavaStructMap) mapStruct(struct, override);
    }

    public XmlJavaStructMap mapStruct(XmlJavaStructMap struct, boolean override)
        throws XmlMapException
    {
        return (XmlJavaStructMap) mapType(struct, override);
    }

    /*
     public XmlJavaStructMap mapStruct(XmlJavaStructMap struct, boolean override)
     throws XmlMapException
     {
     if(struct.getUri() == null)
     throw new XmlMapException("struct uri can't be null");
     if(struct.getLocalName() == null)
     throw new XmlMapException("struct localName can't be null");
     if(struct.javaClass() == null)
     throw new XmlMapException("struct javaClass can't be null");


     Map localMap = localMap(struct.getEncodingStyle(), struct.getUri());
     Map javaMap =  javaMap(struct.getEncodingStyle());

     if(!override) {
     Object o;
     if((o = javaMap.get( struct.javaClass() )) != null) {
     throw new XmlMapException("javaClass="+struct.javaClass()
     +"is already mapped to "+o);
     }
     if((o = localMap.get( struct.getLocalName() )) != null) {
     throw new XmlMapException("uri="+struct.getUri()
     +" localName="+struct.getLocalName()
     +"is already mapped to "+o);
     }
     }

     javaMap.put(struct.javaClass(), struct);
     localMap.put(struct.getLocalName(), struct);

     return struct;
     }
     */

    // -- utillity methods


    protected Map javaMap(String encodingStyle)
        throws XmlMapException
    {
        if(encodingStyle != null) {
            Map javaMap = (Map) java2Struct.get(encodingStyle);
            if(javaMap == null) {
                javaMap = new HashMap();
                java2Struct.put(encodingStyle, javaMap);
            }
            return javaMap;
        } else {
            return defaultJava2Struct;
        }
    }

    protected XmlJavaTypeMap typeMap(String encodingStyle, Class javaClass)
        throws XmlMapException
    {
        XmlJavaTypeMap sa;
        if(encodingStyle != null) {
            sa =  (XmlJavaTypeMap) javaMap(encodingStyle).get(javaClass);
            if(sa == null) {
                sa = (XmlJavaTypeMap) defaultJava2Struct.get(javaClass);
            }
        } else {
            sa = (XmlJavaTypeMap) defaultJava2Struct.get(javaClass);
        }
        if(HEAVY_TRACING) {
            logger.finest("query javaClass="+javaClass.getName()
                         +" encodingStyle="+encodingStyle
                         +" returned "+sa);
        }
        return sa;
    }


    protected Map uriMap(String encodingStyle)
        throws XmlMapException
    {
        if(encodingStyle != null) {
            Map uriMap = (Map) xml2Struct.get(encodingStyle);
            if(uriMap == null) {
                uriMap = new HashMap();
                xml2Struct.put(encodingStyle, uriMap);
            }
            return uriMap;
        } else {
            return defaultXml2Struct;
        }
    }

    protected Map localMap(String encodingStyle, String uri)
        throws XmlMapException
    {

        Map uriMap = uriMap(encodingStyle);
        Map localMap = (Map) uriMap.get(uri);
        if(localMap == null) {
            localMap = new HashMap();
            uriMap.put(uri, localMap);
        }
        return localMap;
        /*
         if(encodingStyle != null) {
         Map uriMap = (Map) xml2Struct.get(encodingStyle);
         if(uriMap == null) {
         uriMap = new HashMap();
         xml2Struct.put(encodingStyle, uriMap);
         }
         Map localMap = (Map) uriMap.get(uri);
         if(localMap == null) {
         localMap = new HashMap();
         uriMap.put(uri, localMap);
         }
         return localMap;
         } else {
         Map localMap = (Map) defaultXml2Struct.get(uri);
         if(localMap == null) {
         localMap = new HashMap();
         defaultXml2Struct.put(uri, localMap);
         }
         return localMap;
         }
         */
    }

    protected XmlJavaTypeMap typeMap(String encodingStyle,
                                     String uri, String localName)
        throws XmlMapException
    {
        if(encodingStyle != null) {
            Map localMap = localMap(encodingStyle, uri);
            XmlJavaTypeMap sa = (XmlJavaTypeMap) localMap.get(localName);
            if(sa == null) {
                localMap = localMap(null, uri);
                sa = (XmlJavaTypeMap) localMap.get(localName);
            }
            return sa;
        } else {
            Map localMap = localMap(null, uri);
            return (XmlJavaTypeMap) localMap.get(localName);
        }
    }


    protected synchronized void remap() throws XmlMapException {
        valid = true;
        // TODO
        throw new XmlMapException("remap not implemented");
    }

    // --- internal

    private final static boolean HEAVY_TRACING = false;
    private boolean valid = false;
    private String defaultStructNsPrefix = null;
    private String defaultPortTypeNsPrefix = null;
    private String defaultArrayComponentInterfaceNsPrefix = null;
    private String defaultStructType = "bean";
    private XmlJavaTypeMap[] typeMap;
    private XmlJavaMapping sink;

    // --- internal mapping

    private Map xml2Struct = new HashMap(); // encodingStyle -> uri -> localName -> XmlJavaStructMap
    private Map java2Struct = new HashMap(); // rncodingStyle -> class -> XmlJavaStructMap

    private Map defaultXml2Struct = new HashMap();
    private Map defaultJava2Struct = new HashMap();

    private Map xml2PortMap = new HashMap();
    private Map java2PortMap = new HashMap();

    private static Logger logger = Logger.getLogger();
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


