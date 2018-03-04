/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: TestList.java,v 1.5 2003/04/06 00:04:06 aslom Exp $
 */

package interop.intf;

import java.io.Serializable;

/**
 * Sample class to for tests
 *
 * @version $Revision: 1.5 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class TestList extends TestEntry {

  public TestList link;

  public TestList() {
    link = null;
  }

  public TestList(String s) {
    super(s);
  }

  public boolean equals(Object o) {
    if (o == null) {
     return false;
    }
    TestList other;
    try {
      other = (TestList) o;
    } catch (ClassCastException e) {
      return false;
    }
    //recursive equals are too memory expensive: return link.equals(le.link);
    TestList le = this;
    TestList otherStart = other;
    while(le != null) {
      if(equalFields(le, other) == false)
        return false;
      if(le.link == this) {
        return other.link == otherStart;
      }
      le = le.link;
      other = other.link;
      if(other == null) {
        return le == null;
      }
    }
    //if(Check.ON) Check.assert(other != null);
    return false;
  }

  static public TestList newList(int size) {
    TestList prev = null;
    TestList cur = null;
    for (int i = 0; i < size; i++) {
      cur  = new TestList();
      cur.setIntValue(i);
      cur.setLink(prev);
      prev=cur;
    }
    return cur;
  }

  static public double[] newDoubleArray(int size) {
    double[] arr = new double[size];
    for (int i=0; i < arr.length; i++)
      arr[i]= 1.0/(i+1);
    return arr;
  }

  static public TestList[] newArray(int size) {
    TestList[] arr = new TestList[size];
    for (int i=0; i < arr.length; i++) {
      arr[i] = new TestList();
      arr[i].setIntValue(i);
    }
    return arr;
  }

  public void setLink(TestList l) {
    link = l;
  }

  public TestList getLink() {
    return link;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(getClass().getName()+" [head:");
    fieldsToString(buf);
    TestList cur=this;
    TestList prev=null;
    while (cur.link != null && cur.link != this) {
      prev = cur;
      cur = cur.link;
    }
    buf.append(" tail:");
    if(prev != null) {
      prev.fieldsToString(buf);
    } else {
      buf.append(" null");
    }
    buf.append("]");
    return buf.toString();
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

