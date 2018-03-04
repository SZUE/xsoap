/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Recorder.java,v 1.5 2004/05/06 18:18:44 aslom Exp $
 */

package org.gjt.xpp.impl.format;

import java.io.*;
import java.util.Enumeration;

import org.gjt.xpp.*;

/**
 * Implementatin of Recorder that is simply writing XML to output sink.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class Recorder implements XmlRecorder {
  protected String[] declaredPrefixes;
  protected String[] declaredNs;
  protected Writer out;
  
  public Recorder() {
  }
  
  public Writer getOutput()
  {
    return out;
  }
  
  public void setOutput(Writer out)
  {
    this.out = out;
  }
  
  public void write(Object o)
    throws IOException, XmlPullParserException
  {
    // order of check *is* important
    if(o instanceof XmlNode) {
	  writeNode((XmlNode)o);
    } else if(o instanceof XmlStartTag) {
	  writeStartTag((XmlStartTag)o);
    } else if(o instanceof XmlEndTag) {
	  writeEndTag((XmlEndTag)o);
    } else if(o instanceof XmlWritable) {
	  ((XmlWritable)o).writeXml(out);
    } else {
	  if(o != null) {
		writeContent(o.toString());
	  } else {
		//silenty ignore null values
	  }
	}
  }
  
  public void writeXml(XmlWritable w)
	throws IOException, XmlPullParserException
  {
	w.writeXml(out);
  }
  
  public void writeContent(String s)
	throws IOException, XmlPullParserException
  {
	if(s == null) {
	  throw new XmlPullParserException(
		"null string can not be written as XML element content");
	}
	writeEscaped(s, false);
  }
  
  public void writeEndTag(XmlEndTag etag)
	throws IOException, XmlPullParserException
  {
	if(etag.getRawName() == null) {
	  throw new XmlPullParserException(
		"raw name of XML element can not be null");
	}
	out.write("</");
	out.write(etag.getRawName());
	out.write('>');
  }
  
  public void writeStartTag(XmlStartTag stag)
	throws IOException, XmlPullParserException
  {
	writeStartTagStart(stag);
	out.write('>');
  }
  
  public void writeStartTagStart(XmlStartTag stag)
	throws IOException, XmlPullParserException
  {
	if(stag.getRawName() == null) {
	  throw new XmlPullParserException(
		"raw name of XML element can not be null");
	}
	out.write('<');
	out.write(stag.getRawName());
	// print all attributes
	for (int i = 0; i < stag.getAttributeCount(); i++)
	{
	  if(stag.isAttributeNamespaceDeclaration(i) == false) {
		out.write(' ');
		out.write(stag.getAttributeRawName(i));
		out.write("='");
		writeEscaped(stag.getAttributeValue(i), true);
		out.write('\'');
	  }
	}
  }
  
  
  public void writeStartTagAsEndTag(XmlStartTag stag)
	throws IOException, XmlPullParserException
  {
	if(stag.getRawName() == null) {
	  throw new XmlPullParserException(
		"raw name of XML end tag can not be null");
	}
	out.write("</");
	out.write(stag.getRawName());
	out.write('>');
  }
  
  public void writeNode(XmlNode node)
	throws IOException, XmlPullParserException
  {
	writeStartTagStart(node);
	
	// print namespace declarations - if any
	int len = node.getDeclaredNamespaceLength();
	if(len > 0) {
	  if(declaredNs == null || len > declaredNs.length) {
		int newLen = len + 10;
		String[] newDeclaredPrefixes = new String[newLen];
		String[] newDeclaredNs = new String[newLen];
		int oldLen = declaredNs != null ? declaredNs.length : 0;
		for(int i = 0; i < oldLen; ++i) {
		  newDeclaredPrefixes[i] = declaredPrefixes[i];
		  newDeclaredNs[i] = declaredNs[i];
		}
		declaredPrefixes = newDeclaredPrefixes;
		declaredNs = newDeclaredNs;
	  }
	  node.readDeclaredPrefixes(declaredPrefixes, 0, len);
	  node.readDeclaredNamespaceUris(declaredNs, 0, len);
	  for (int i = 0; i < len; i++)
	  {
		out.write(" xmlns:");
		out.write(declaredPrefixes[i]);
		out.write("='");
		writeEscaped(declaredNs[i], true);
		out.write('\'');
	  }
	}
	
	// write default namespace declaration if necessary ...
	//    XmlNode parent = node.getParent();
	String defaultUri = node.getDefaultNamespaceUri();
	//    if(defaultUri != null
//	   && (parent == null
//			 || ! defaultUri.equals(parent.getDefaultNamespaceUri())
//		  )
//	  )
	if(defaultUri != null)
	{
	  XmlNode parent = node.getParentNode();
	  if((parent != null
			&& ! defaultUri.equals(parent.getDefaultNamespaceUri()))
		 || (parent == null && ! "".equals(defaultUri))
		)
	  {
		out.write(" xmlns='");
		writeEscaped(defaultUri, true);
		out.write('\'');
	  }
	}
	
//	out.write('>');
	
	// for each children - print it
	//Enumeration enum = node.children();
	boolean hadChildren = false;
	//while (enum.hasMoreElements())
	int childCount = node.getChildrenCount();
	for(int i = 0; i < childCount; ++i)
	{
	  Object o = node.getChildAt(i);//enum.nextElement();
	  // do not output null or empty string "" nodes
	  if(o == null
//		 || (o instanceof String && o.toString().length() == 0)
		)
	  {
		continue;
	  }
	  if(!hadChildren) {
		hadChildren = true;
		out.write('>');
	  }
	  write(o);
	}
	if(hadChildren) {
	  writeStartTagAsEndTag(node);
	} else {
	  out.write("/>");
	}
  }
  
  protected void writeEscaped(String s, boolean escapeApostAttrib)
	throws IOException, XmlPullParserException
  {
	int pos = 0;
	int i = 0;
	int len = s.length();
	for (; i < len; i++) {
	  char ch = s.charAt(i);
	  switch (ch) {
		case '<':
		  if(i > pos) out.write(s, pos, i - pos);
		  pos = i + 1;
		  out.write("&lt;");
		  break;
		case '\r':
		  if(i > pos) out.write(s, pos, i - pos);
		  pos = i + 1;
		  out.write("&#xD;");
		  break;
		case '&':
		  if(i > pos) out.write(s, pos, i - pos);
		  pos = i + 1;
		  out.write("&amp;");
		  break;
		  // preserve (#x20, #xD, #xA, #x9) ptherwise normalized to #x20
		  // as described in XML1.0 3.3.3 Attribute-Value Normalization
		case '\t':
		  if(escapeApostAttrib) {
			if(i > pos) out.write(s, pos, i - pos);
			pos = i + 1;
			out.write("&#x9;");
		  }
		  break;
		case '\n':
		  if(escapeApostAttrib) {
			if(i > pos) out.write(s, pos, i - pos);
			pos = i + 1;
			out.write("&#xA;");
		  }
		  break;
		  // this recorder writes attributes enclosed in apostrophes
		  // thereofre apostrophe in attrib value must be escaped
		case '\'':
		  if(escapeApostAttrib) {
			if(i > pos) out.write(s, pos, i - pos);
			pos = i + 1;
			out.write("&apos;");
		  }
		  break;
	  }
	}
	if(pos == 0) {
	  out.write(s);
	} else {
	  out.write(s, pos, i - pos);
	}
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


