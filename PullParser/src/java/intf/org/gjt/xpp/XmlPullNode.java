/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: XmlPullNode.java,v 1.5 2004/05/06 18:18:45 aslom Exp $
 */

package org.gjt.xpp;

import java.io.*;
import java.util.Enumeration;

import org.gjt.xpp.XmlNode;

/**
 * This class represents pullable XML subtree - children are built on
 * demand.
 *
 * @see org.gjt.xpp.XmlNode
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public interface XmlPullNode extends XmlNode {
  
  public void resetPullNode();
  
  public XmlNode newNode()
    throws XmlPullParserException;

  public XmlPullNode newPullNode(XmlPullParser pp)
    throws XmlPullParserException;
  
  /**
   * Is pull parsing of node finished.
   */
  public boolean isFinished();
  
  /**
   * Get parser that is use to build this node tree
   * and this pull node becomes finished - the caller is responsibile
   * to move pull parser state to the end tag of this node
   * (or parent pull node will be left in unconsistent state!!!!).
   * The returned pull parser position will be before start tag
   *  of next child or before final end tag and the caller
   * should use next() to move parser to start reading children.
   * The node state becomes finished and subsequent call to
   * this method will throw exception until setPullParser() is called.
   * The final effect should be equivalen to calling skipNode()!
   * <p><b>NOTE:</b> this pull node must be in unfinished state
   *  or exception will be thrown
   */
  public  XmlPullParser getPullParser()
	throws IOException, XmlPullParserException;
  
  
  /** Reset pull node to use pull parser. Pull Parser must be on START_TAG */
  public void setPullParser(XmlPullParser pp)
	throws XmlPullParserException;

  public int getChildrenCountSoFar();

  /**
   * This is not recommened method to pull children when node is not
   * finished (use readNextChild() instead) as Enumeration interface
   * does not allow to throw XmlPullParserException
   * so any parsing exeption is wrapped into RuntimeException
   * making code more messy...
   *
   * @see #readNextChild()
   */
  public Enumeration children();
  
  /**
   * This is preferred method to pull children
   * (children() requires .wrapping object Enumeration).
   *
   * @see #children()
   * @return next child (which is String or XmlPullNode) or
   *         null if there is no re children
   */
  public Object readNextChild()
    throws XmlPullParserException, IOException;
      
  /**
   * Read all reminaing children up to end tag.
   */
  public void readChildren()
    throws XmlPullParserException, IOException;
  
  
  public void skipChildren()
    throws XmlPullParserException, IOException;
  
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


