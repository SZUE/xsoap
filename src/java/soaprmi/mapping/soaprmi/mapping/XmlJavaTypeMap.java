/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: XmlJavaTypeMap.java,v 1.8 2003/04/06 00:04:10 aslom Exp $
 */

package soaprmi.mapping;

import java.lang.reflect.*;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

import soaprmi.util.Util;

import soaprmi.struct.Struct;
import soaprmi.struct.StructAccessor;
import soaprmi.struct.StructException;


public class XmlJavaTypeMap extends XmlMap {
    // --- internal state
    protected String javaType;
    protected String deserializerName;
    protected String serializerName;
    protected String encodingStyle;
    protected boolean simpleType;

    protected boolean generated;

    // -- cached
    protected Class javaClass;

    public XmlJavaTypeMap() {
    }

    public XmlJavaTypeMap(String uri, String localName, String javaType) {
        super(uri, localName);
        this.javaType = javaType;
    }


    public XmlJavaTypeMap(String uri, String localName, Class javaClass) {
        super(uri, localName);
        this.javaClass = javaClass;
        this.javaType = javaClass.getClass().getName();
        this.valid = true;
        this.simpleType = true; //do not care about internals
    }

    //public XmlJavaTypeMap(boolean generated_) {
    //  generated = generated_;
    //}

    public String getJavaType() { return javaType; }

    public void setJavaType(String javaType) throws XmlMapException {
        valid = false;
        this.javaType = javaType;
    }

    public String getEncodingStyle() { return encodingStyle; }

    public void setEncodingStyle(String encodingStyle) throws XmlMapException {
        this.encodingStyle = encodingStyle;
    }

    public String getDeserializerName() { return deserializerName; }

    public void setDeserializerName(String deserializerName_)
        throws XmlMapException
    {
        deserializerName = deserializerName_;
    }

    public String getSerializerName() { return serializerName; }

    public void setSerializerName(String serializerName_)
        throws XmlMapException
    {
        serializerName = serializerName_;
    }

    // good for String and byte[]
    public boolean getSimpleType() throws XmlMapException{
        return simpleType;
    }

    public void setSimpleType(boolean simpleType_) throws XmlMapException{
        simpleType = simpleType_;
    }

    // utility methods

    public boolean wasGenerated() throws XmlMapException{
        return generated;
    }

    public void makeGenerated(boolean generated_) throws XmlMapException{
        generated = generated_;
    }


    public Class javaClass() throws XmlMapException{
        if(!valid && javaClass == null) remap();     //TODO ALEK FIXME
        return javaClass;
    }

    public void javaClass(Class value) throws XmlMapException{
        javaClass = value;
        javaType = javaClass.getName();
        if(!simpleType) remap();     //TODO ALEK FIXME
    }


    // --- internal manipuation

    protected void remap() throws XmlMapException {
        if(javaClass == null) {
            if(javaType == null) {
                throw new XmlMapException("null struct name can not be mapped");
            }
            try {
                javaClass = Util.loadClass(javaType);
            } catch(Exception ex) {
                throw new XmlMapException("no java class for type "+javaType, ex);
            }
        } else {
            javaType = javaClass.getName();
        }
        //if(javaClass.isInterface()) {
        //    throw new XmlMapException("java struct "+javaType+" can not be interface");
        //}

        valid = true;
        return;
    }

    public String toString() {
        return getClass().getName() + "["
            + "uri:"+uri
            +",localName:"+localName
            +",javaType:"+javaType
            +"]";
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


