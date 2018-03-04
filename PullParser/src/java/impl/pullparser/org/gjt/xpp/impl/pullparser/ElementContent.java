/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: ElementContent.java,v 1.5 2004/05/06 18:18:45 aslom Exp $
 */

package org.gjt.xpp.impl.pullparser;

import org.gjt.xpp.XmlNode;

/**
 * Utility class to keep information about XML element such as name etc.
 * and if namespaces enabled also list of declared prefixes and
 * previous values of prefixes namespace uri
 * (to restore them in ns hashtbale)
 */
public class ElementContent {
  private final static boolean TRACE_SIZING = false;

  String qName;
  //char qNameBuf[];
  String uri;
  String localName;
  String prefix;

  String defaultNs;

  int prefixesEnd;
  int prefixesSize;
  String[] prefixes;
  String[] namespaceURIs;
  String[] prefixPrevNs;

  // just to optimize readNode
  XmlNode node;

  ElementContent() {
  }

  void ensureCapacity(int size) {
    int newSize = 2 * size;
    if(newSize == 0)
      newSize = 8; // = lucky 7 + 1 //25
    if(prefixesSize < newSize) {
      if(TRACE_SIZING) {
	    System.err.println("prefixesEnd "+prefixesEnd+" ==> "+newSize);
      }
      String[] newPrefixes = new String[newSize];
      String[] newNamespaceURIs = new String[newSize];
      String[] newPrefixPrevNs = new String[newSize];
      if(prefixes != null) {
        System.arraycopy(
          prefixes, 0, newPrefixes, 0, prefixesEnd);
        System.arraycopy(
          namespaceURIs, 0, newNamespaceURIs, 0, prefixesEnd);
        System.arraycopy(
          prefixPrevNs, 0, newPrefixPrevNs, 0, prefixesEnd);
      }
      prefixes = newPrefixes;
      namespaceURIs = newNamespaceURIs;
      prefixPrevNs = newPrefixPrevNs;
      prefixesSize = newSize;
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

