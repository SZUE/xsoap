/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Level.java,v 1.5 2003/04/06 00:04:03 aslom Exp $
 */

package soaprmi.util.logging;

import java.util.Map;
import java.util.HashMap;

/**
 * Levels of logging (based on propsed Java Logging API).
 *
 * @version $Revision: 1.5 $ $Date: 2003/04/06 00:04:03 $ (GMT)
 * @author Aleksander Slominski [http://www.extreme.indiana.edu/~aslom]
 */

public class Level implements java.io.Serializable {
  private static Map levels = new HashMap();

  public static final Level OFF =  new Level("OFF", 10000);
  //(highest value)
  public static final Level SEVERE =  new Level("SEVERE", 7777);
  public static final Level WARNING =  new Level("WARNING", 666);
  public static final Level INFO =  new Level("INFO", 55);
  public static final Level CONFIG =  new Level("CONFIG", 44);
  public static final Level FINE =  new Level("FINE", 33);
  public static final Level FINER =  new Level("FINER", 22);
  //(lowest value)
  public static final Level FINEST =  new Level("FINEST", 11);
  public static final Level ALL = new Level("ALL", 0);

  protected Level(java.lang.String name, int value) {
    this.name = name;
    this.value = value;
    levels.put(name, this);
  }

  public int hashCode() {
    return value; //+ name.hasCode()
  }

  public boolean equals(java.lang.Object ox) {
    if (ox == null) {
     return false;
    }
    Level other;
    try {
      other = (Level) ox;
    } catch (ClassCastException e) {
      return false;
    }
    return value == other.intValue();
  }

  public final int intValue() {
    return value;
  }

  public final java.lang.String toString() {
    return name;
  }

  public static Level parse(java.lang.String name) {
    Level l = (Level) levels.get(name);
    if(l == null) throw new IllegalArgumentException
      ("unknown debug level: "+name);
    return l;
  }

  private int value;
  private String name;
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


