/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Base64Handler.java,v 1.6 2003/04/06 00:04:17 aslom Exp $
 */

package soaprmi.soapenc;

import java.io.IOException;
import java.io.Writer;

import java.util.*;
import java.text.*;

import org.gjt.xpp.*;

//import org.globus.util.Base64;

import soaprmi.mapping.XmlMapException;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.util.Check;
import soaprmi.util.Util;
import soaprmi.util.base64.Base64;

import soaprmi.soap.*;

/**
 * Implementation BASE64 for hadling byte arrays in SOAP.
 *
 * @version $Revision: 1.6 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class Base64Handler implements Deserializer, Serializer {

    public Base64Handler() {
    }

    public Object readObject(
        DeserializeContext dctx,
        EncodingStyle enc,
        Class expectedClass,
        XmlJavaTypeMap map,
        XmlPullParser pp,
        XmlStartTag stag
    )
        throws DeserializeException, XmlPullParserException, IOException
    {
        if(pp.next() == XmlPullParser.END_TAG) {
          // empty array
            return new byte[]{}; //TODO make into singleton ...
        }

        if(pp.getEventType() != XmlPullParser.CONTENT) {
            throw new DeserializeException("expected element content"
                                               +pp.getPosDesc());
        }
        String value = pp.readContent();
        byte[] byteArr;
        try {
            //byteArr = Base64.decode(value.toCharArray());
            byteArr = Base64.decode(value.toCharArray());
        } catch(Exception ex) {
            throw new DeserializeException(
                "can't parse BAS64 value '"+value+"'"
                    +pp.getPosDesc(), ex);
        }
        if(pp.next() != XmlPullParser.END_TAG)
            throw new DeserializeException("expected end tag"+pp.getPosDesc());
        return byteArr;
    }

    public void writeObject(
        SerializeContext sctx,
        EncodingStyle enc,
        Object o,
        String name,
        Class baseClass,
        String id)
        throws SerializeException, XmlMapException, IOException
    {
        Writer out = sctx.getWriter();
        //String tagName = sctx.writeStartTag(name, Soap.SOAP_ENC_NS, "base64", sctx.structLevel() == 0, id);
        String tagName = sctx.writeStartTag(name, Soap.XSD_NS_CURR, "base64Binary", sctx.structLevel() == 0, id);
//        out.write('<');
//        out.write(name);
//        SoapStyle style = sctx.getSoapStyle();
//
//        //if(style.XSI_TYPED) sctx.writeXsdType("base64");
//        if(style.XSI_TYPED || sctx.structLevel() == 0) sctx.writeXsiType(Soap.SOAP_ENC_NS, "base64");
//        out.write('>');
        //String s=ISO_8601_UTC.format((Date)o);
        byte[] byteArr = (byte[]) o;
        char[] charArr = Base64.encode(byteArr);
        String s = new String(charArr);
        out.write(s);
        sctx.writeEndTag(tagName);
//        out.write("</");
//        out.write(name);
//        out.write(">\n");
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


