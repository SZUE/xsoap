/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: HashtableHandler.java,v 1.4 2003/04/06 00:04:17 aslom Exp $
 */

package soaprmi.soapenc;

import java.io.IOException;
import java.io.Writer;

import java.util.Enumeration;
import java.util.Hashtable;
//import java.text.*;

import org.gjt.xpp.*;

import soaprmi.mapping.XmlMapException;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.util.Check;
import soaprmi.util.Util;

import soaprmi.soap.*;

/**
 * Implementation of handler Hashtable that uses Apache SOAP encoding.
 * (http://xml.apache.org/xml-soap:Map)
 *
 * @version $Revision: 1.4 $
 * @author Aleksander A. Slominski
 */

public class HashtableHandler implements Deserializer, Serializer {

    public HashtableHandler() {
    }

    public Object readObject(DeserializeContext dctx,
			     EncodingStyle enc,
			     Class expectedClass,
			     XmlJavaTypeMap map,
			     XmlPullParser pp,
			     XmlStartTag stag)
        throws DeserializeException, XmlPullParserException, IOException
    {
	//String id = stag.getAttributeValueFromRawName("id");
        Hashtable hashtable = new Hashtable();
	//if(id != null) {
	//    dctx.setIdValue(id, hashtable);
	//}
	while(true) {
	    int state = pp.next();
	    if(state == XmlPullParser.CONTENT) {
		if(!pp.isWhitespaceContent())
		    throw new DeserializeException(
			"empty struct can not have text content"+pp.getPosDesc());
		state = pp.next();
	    }
	    if(state == XmlPullParser.END_TAG)
		break;
	    pp.readStartTag(stag);
	    if("item".equals(stag.getLocalName()) == false) {
		throw new DeserializeException(
		    "map elements must have name 'item' not '"+stag.getLocalName()+"'"
			+pp.getPosDesc());
	    }
	    if(!"".equals(stag.getNamespaceUri()))
		throw new DeserializeException(
		    "map item is not allowed to have namespace"+ pp.getPosDesc());
	    // process item
	    Object key = null;
	    Object value = null;
	    boolean keyInitialized = false;
	    boolean valueInitialized = false;
	    while(true) {
		state = pp.next();
		if(state == XmlPullParser.CONTENT) {
		    if(!pp.isWhitespaceContent())
			throw new DeserializeException(
			    "empty struct can not have text content"+pp.getPosDesc());
		    state = pp.next();
		}
		if(state == XmlPullParser.END_TAG)
		    break;
		pp.readStartTag(stag);
		String localName = stag.getLocalName();
		if(!"".equals(stag.getNamespaceUri()))
		    throw new DeserializeException(
			"map item '"+localName+"' is not allowed to have namespace"+ pp.getPosDesc());

		String href = stag.getAttributeValueFromRawName("href");
		if(href != null) {
		    throw new DeserializeException(
			"multiref is not currently supported inside map "+ pp.getPosDesc());
		}

		if("key".equals(localName)) {
		    if(keyInitialized) {
			throw new DeserializeException(
			    "map key in item can not be duplicated"+pp.getPosDesc());
		    }
		    key =  enc.readObject(dctx, Object.class, pp, stag);
		    keyInitialized = true;
		} else if("value".equals(localName)) {
		    if(valueInitialized) {
			throw new DeserializeException(
			    "map value in item can not be duplicated"+pp.getPosDesc());
		    }
		    value =  enc.readObject(dctx, Object.class, pp, stag);
		    valueInitialized = true;
		} else {
		    throw new DeserializeException(
			"map item can ony have 'key' or 'value' not '"+localName+"'"
			    +pp.getPosDesc());
		}
	    }
	    if(!keyInitialized) {
		throw new DeserializeException("missing item key"+pp.getPosDesc());
	    }
	    if(!valueInitialized) {
		throw new DeserializeException("missing item key"+pp.getPosDesc());
	    }

	    hashtable.put(key, value);
	}

	return hashtable;
    }

    public void writeObject(SerializeContext sctx,
			    EncodingStyle enc,
			    Object o,
			    String name,
			    Class baseClass,
			    String id)
	throws SerializeException, XmlMapException, IOException
    {
	Writer out = sctx.getWriter();
	out.write('<');
	out.write(name);
	SoapStyle style = sctx.getSoapStyle();
	if(style.XSI_TYPED) {
	    sctx.writeXsiType("http://xml.apache.org/xml-soap", "Map");
	}
	out.write(">\n");

	Hashtable hashtable = (Hashtable)o;

	for (Enumeration e = hashtable.keys(); e.hasMoreElements(); )
	{
	    Object key = e.nextElement();
	    Object value = hashtable.get(key);

	    out.write("<item>\n");
	    if(value != null) {
		enc.writeObject(sctx, key, "key", Object.class);
	    } else {
		sctx.writeXsiNull("i");
	    }
	    if(value != null) {
		enc.writeObject(sctx, value, "value", Object.class);
	    } else {
		sctx.writeXsiNull("i");
	    }

	    out.write("</item>\n");
	}

	out.write("</");
	out.write(name);
	out.write(">\n");
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


