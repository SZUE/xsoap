/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: XmlJavaOperationMap.java,v 1.9 2003/05/18 13:16:05 aslom Exp $
 */

package soaprmi.mapping;

import java.lang.reflect.Method;
import soaprmi.util.ParamNameExtractor;

/**
 * Map port operation names (request and response) to Java method.
 *
 * @version $Revision: 1.9 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class XmlJavaOperationMap {
    // -- internal state

    //private String requestName;
    //private String responseName;
    private String javaName;
    private Method javaMethod;
    private XmlJavaMessageMap request;
    private XmlJavaMessageMap response;
    private XmlJavaMessageMap[] faults;

    public String toString() {
        StringBuffer buf = new StringBuffer(
            getClass().getName() + "[javaName:"+javaName+",javaMethod:"+javaMethod);
        buf.append(",request="+request);
        buf.append(",response="+request);
        for(int i = 0; faults != null && i < faults.length; ++i) {
            buf.append(",faults["+i+"]="+faults[i]);
        }
        buf.append("]");
        return buf.toString();
    }


    public XmlJavaOperationMap() {
    }

    public XmlJavaOperationMap(String requestName, Method m)
        throws XmlMapException
    {
        this(requestName, null, m);
    }

    public XmlJavaOperationMap(String requestName, String responseName, Method m)
        throws XmlMapException
    {
        //      this.requestName = requestName;
        //      this.responseName = responseName;
        javaMethod(m, requestName, responseName);
    }

    public void setRequest(XmlJavaMessageMap request)
    {
        this.request = request;
    }

    public XmlJavaMessageMap getRequest()
    {
        return request;
    }

    public void setResponse(XmlJavaMessageMap response)
    {
        this.response = response;
    }

    public XmlJavaMessageMap getResponse()
    {
        return response;
    }

    public void setFaults(XmlJavaMessageMap[] faults)
    {
        this.faults = faults;
    }

    public XmlJavaMessageMap[] getFaults()
    {
        return faults;
    }

    public String getJavaName() { return javaName; }

    public void setJavaName(String javaName) { this.javaName = javaName; }

    //public void setRequestName(String requestName) { this.requestName = requestName; }

    //public void setResponseName(String responseName) { this.responseName = responseName; }

    //public String getRequestName() { return requestName; }

    //  public String getResponseName() {
    //    // NOTE: it is using SOAP RPC naming convention
    //    if(responseName == null && requestName != null)
    //      responseName = requestName + "Response";
    //    return responseName;
    //  }


    public Method javaMethod() { return javaMethod; }

    public void javaMethod(Method m) throws XmlMapException
    {
        javaMethod(m, m.getName(), m.getName()+"Response");
    }

    public void javaMethod(Method m, String requestName, String responseName)
        throws XmlMapException
    {
        this.javaName = m.getName();
        // TODO map real exceptions
        this.faults = new XmlJavaMessageMap[0];
        javaMethod = m;
        XmlJavaMessageMap newRequest = createRequest(m, requestName);
        if(request != null) {
            compareMessagePartTypes("request", request, newRequest);
        } else {
            this.request = newRequest;
        }
        if(responseName == null) responseName = requestName + "Response";
        XmlJavaMessageMap newResponse = createResponse(m, responseName);
        if(response != null) {
            compareMessagePartTypes("response", response, newResponse);
        } else {
            this.response = newResponse;
        }
    }

    public static void compareMessagePartTypes(String desc,
                                               XmlJavaMessageMap oldMessage,
                                               XmlJavaMessageMap newMessage)
        throws XmlMapException
    {

        XmlJavaPartMap[] oldParts = oldMessage.getParts();
        XmlJavaPartMap[] newParts = newMessage.getParts();
        if(oldParts.length != newParts.length) {
            throw new XmlMapException(
                "in "+desc+" number of must be the same in old "+oldMessage+" and new "+newMessage);
        }
        for (int i = 0; i < oldParts.length; i++)
        {
            Class oldType = oldParts[i].javaClass();
            Class newType = newParts[i].javaClass();
            if(oldType != newType) {
                throw new XmlMapException(
                    "in "+desc+" types of must be the same "+oldType+" and "+newType
                        +" in old "+oldMessage+" and new "+newMessage);
            }
        }
    }

    public static XmlJavaMessageMap createRequest(Method m, String requestName)
        throws XmlMapException
    {
        XmlJavaMessageMap messageMap = new XmlJavaMessageMap();
        messageMap.setMessageName(requestName);
        Class[] parameterTypes = m.getParameterTypes();
        String[] parameterNames = ParamNameExtractor.getParameterNamesFromDebugInfo(m);
        if(parameterNames == null) { //magic failed -- use brute force ...
            parameterNames = new String[parameterTypes.length];
            for(int i = 0; i < parameterTypes.length; ++i) {
                //parameterNames[i] = (i > 0) ? "p" + (i + 1) : requestName;
                parameterNames[i] = "p" + (i + 1);
            }
        }
        XmlJavaPartMap[] parts = new XmlJavaPartMap[parameterTypes.length];
        if(parameterTypes.length > 0) {
            for(int i = 0; i < parameterTypes.length; ++i) {
                parts[i] = new XmlJavaPartMap();
                parts[i].setPartName(parameterNames[i]);
                parts[i].javaClass(parameterTypes[i]);
                //mapping.q;
            }
        }
        messageMap.setParts(parts);
        //Check.assertion(parts.length == parameterTypes.length);
        return messageMap;
    }

    public static XmlJavaMessageMap createResponse(Method m, String responseName)
        throws XmlMapException
    {
        XmlJavaMessageMap messageMap = new XmlJavaMessageMap();
        messageMap.setMessageName(responseName);
        Class returnType = m.getReturnType();
        XmlJavaPartMap[] parts;
        if(returnType != Void.TYPE) {
            parts = new XmlJavaPartMap[1];
            parts[0] = new XmlJavaPartMap();
            parts[0].setPartName("return");
            parts[0].javaClass(returnType);
        } else {
            parts = new XmlJavaPartMap[0];
        }
        messageMap.setParts(parts);
        //Check.assertion(parts.length < 2);
        return messageMap;
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

