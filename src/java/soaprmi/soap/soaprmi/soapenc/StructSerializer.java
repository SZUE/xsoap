/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: StructSerializer.java,v 1.5 2003/04/06 00:04:17 aslom Exp $
 */

package soaprmi.soapenc;

import java.io.IOException;

import soaprmi.struct.StructAccessor;
import soaprmi.struct.StructException;
import soaprmi.mapping.XmlJavaStructMap;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.mapping.XmlMapException;
import soaprmi.util.Check;

import soaprmi.soap.*;

/**
 * Implementation of standard SOAP encoding style struct serializer.
 *
 * @version $Revision: 1.5 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class StructSerializer implements Serializer {

  public void writeObject(
    SerializeContext sctx,
    EncodingStyle enc,
    Object o,
    String name,
    Class baseClass,
    String id)
  throws SerializeException, XmlMapException, IOException
  {
    try {
      SoapStyle style = sctx.getSoapStyle();
      if(Check.ON) Check.assertion(o != null);
      if(Check.ON) Check.assertion(baseClass.isPrimitive() == false);
      if(Check.ON) Check.assertion(baseClass.isArray() == false);
      if(Check.ON) Check.assertion(baseClass != String.class);
      //if(Check.ON) Check.assert(map.getSimpleType() == false);
      //write encodingStyle if necessary
      XmlJavaTypeMap map = sctx.queryTypeMap(o.getClass());
      String stagName =  sctx.writeStartTag(
        name,
        map.getUri(),
        map.getLocalName(),
        o.getClass() != baseClass,
        id != null ? id : sctx.addId(o)
      );

      XmlJavaStructMap smap = (XmlJavaStructMap) map;
      StructAccessor sa = smap.structAccessor();
      boolean primitivesWrapped = sa.primitivesWrapped();
      String[] saNames = sa.getAccessorNames();
      int saLen = saNames.length;

      sctx.enterStruct();

      for(int i = 0; i < saLen; ++i) {
        String saName = saNames[i];
        Class saBaseType = sa.getAccessorType(saName);
        if(primitivesWrapped) {
          Object saVal = sa.getValue(o, saName);
          enc.writeObject(sctx, saVal, saName, saBaseType);
        } else {
          throw new IllegalStateException(
            "unwrapped primitives not yet supported");
          /*
          if(saBaseType.isPrimitive()) {
              if(saBaseType == Integer.TYPE) {
                int saVal = sa.getIntValue(o, saName);
                enc.writeInt(sctx, saVal, saName);
              //TODO: all other types... } else {
              }
          } else {
            Object saVal = sa.getValue(o, saName);
            enc.writeObject(sctx, saVal, saName, saBaseType);
          }
          */
        }
      }

      sctx.leaveStruct();

      //TODO: it should also restores encoding style!!!!
      sctx.writeEndTag(stagName);
    } catch(StructException ex) {
      throw new SerializeException("struct access exception", ex);
    }
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

