/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: XmlJavaPortTypeMap.java,v 1.10 2003/05/18 13:16:05 aslom Exp $
 */

package soaprmi.mapping;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.TreeMap;

import soaprmi.util.Util;

/**
 * Map por type to java interface.
 *
 * @version $Revision: 1.10 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class XmlJavaPortTypeMap extends XmlMap {
    // --- internal
    protected String javaType;

    // --- internal state

    protected XmlJavaOperationMap [] operations;

    protected Class javaClass;
    protected Map mapJavaMethod2Operation = new HashMap();
    protected Map mapMethodRequest2Operation = new HashMap();
    protected Map mapMethodResponse2Operation = new HashMap();
    //private Map mapJavaMethod2Response = new HashMap();
    //private Map mapResponse2JavaMethod = new HashMap();

    public String toString() {
        StringBuffer buf = new StringBuffer(
            getClass().getName() + "[javaType:"+javaType+",javaClass:"+javaClass);
        if(operations != null) {
            buf.append(",operations=");
            for(int i = 0; i < operations.length; ++i) {
                buf.append(operations[i]);
            }
        }
        buf.append("]");
        return buf.toString();
    }


    public XmlJavaPortTypeMap() {
    }

    public XmlJavaPortTypeMap(String uri, String localName,
                              String javaType)
    {
        super(uri, localName);
        this.javaType = javaType;
    }

    public XmlJavaPortTypeMap(String uri, String localName,
                              Class javaClass)
    {
        super(uri, localName);
        this.javaClass = javaClass;
        this.javaType = javaClass.getName();
    }

    public String getJavaType() { return javaType; }

    public void setJavaType(String javaType) throws XmlMapException {
        valid = false;
        this.javaType = javaType;
    }

    public XmlJavaOperationMap[] getOperations() {
        return operations;
    }

    public void setOperations(XmlJavaOperationMap[] operations)
        throws XmlMapException
    {
        valid = false;
        this.operations = operations;
    }

    // utility methods

    public XmlJavaOperationMap queryOperation(Method m) throws XmlMapException {
        if(!valid) remap();
        return (XmlJavaOperationMap) mapJavaMethod2Operation.get(m);
    }

    public XmlJavaOperationMap queryMethodRequest(String requestName) throws XmlMapException {
        if(!valid) remap();
        return (XmlJavaOperationMap) mapMethodRequest2Operation.get(requestName);
    }

    public Class javaClass() throws XmlMapException{
        if(!valid) remap();
        return javaClass;
    }

    public void setJavaClass(Class value) throws XmlMapException{
        valid = false;
        javaClass = value;
    }

    // --- internal manipuation

    private void remap() throws XmlMapException {
        mapJavaMethod2Operation.clear();
        mapMethodRequest2Operation.clear();
        mapMethodResponse2Operation.clear();

        if(javaClass == null) {
            if(javaType == null) {
                throw new XmlMapException("null interace name can not be mapped");
            }
            try {
                javaClass = Util.loadClass(javaType); //Class.forName(javaType);
            } catch(Exception ex) {
                throw new XmlMapException("can't get java class for type "+javaType, ex);
            }
        } else {
            javaType = javaClass.getName();
        }

        // check that javaType is interface!!!!
        if(!javaClass.isInterface()) {
            throw new XmlMapException("java class "+javaType+" must be interface");
        }

        // map public methods (including inherited from super interfaces)
        Method[] javaMethods = javaClass.getMethods();
        Map javaName2Method = new HashMap();
        final String overloaded = "OVERLOADED";
        // prepare map of javaName -> Method (may be overloaded)
        for( int i = 0; i <javaMethods.length; ++i ) {
            Method m = javaMethods[i];
            String javaName = m.getName();
            if(javaName2Method.get(javaName) != null) {
                if(operations == null) {
                    throw new XmlMapException("mapping overloaded public method not supported,"
                                                  +" method: "+javaName
                                                  +" at java interface: "+javaType);
                }
                javaName2Method.put(javaName, overloaded);
            } else {
                javaName2Method.put(javaName, m);
                if(operations == null) { // use default mapping - map all public methods
                    XmlJavaOperationMap oprtn =
                        new XmlJavaOperationMap(javaName, javaName+"Response", m);
                    mapMethodRequest2Operation.put(javaName, oprtn);
                    mapMethodResponse2Operation.put(javaName+"Response", oprtn);
                    mapJavaMethod2Operation.put(m, oprtn);
                }
            }
        }
        if(operations != null) {
            for(int k = 0; k < operations.length; ++k ) {
                XmlJavaOperationMap oprtn = operations[k];
                XmlJavaMessageMap request = oprtn.getRequest();
                String requestName = request.getMessageName(); //oprtn.getRequestName();
                XmlJavaMessageMap response = oprtn.getResponse();
                String responseName = response.getMessageName(); //oprtn.getResponseName();
                String javaName = oprtn.getJavaName();
                Object val = javaName2Method.get(javaName);
                if(val == overloaded) {
                    throw new XmlMapException("overloaded methods are not supported"
                                                  +" - can't map xml name "+requestName
                                                  +" to overloaded method "+javaName
                                                  +" at java interface: "+javaType);

                }
                Method m = (Method) val;
                if(m == null) {
                    throw new XmlMapException("no java method name "+javaName
                                                  +" declared for interface "+javaType);
                }
                if(mapMethodRequest2Operation.get(requestName) != null) {
                    throw new XmlMapException("method request name "+requestName
                                                  +" already declared for interface "+javaType);
                }
                oprtn.javaMethod(m);
                mapMethodRequest2Operation.put(requestName, oprtn);
                mapMethodResponse2Operation.put(responseName, oprtn);
                mapJavaMethod2Operation.put(m, oprtn);
            }
        } else {
            Iterator it = mapJavaMethod2Operation.values().iterator();
            int size = mapJavaMethod2Operation.values().size();
            operations = new XmlJavaOperationMap[size];
            for (int i = 0; i < size; i++)
            {
                operations[i] = (XmlJavaOperationMap) it.next();
            }

        }
        valid = true;
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

