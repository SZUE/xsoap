/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SoapDynamicSkeleton.java,v 1.4 2003/04/06 00:04:20 aslom Exp $
 */

package soaprmi.soaprpc;

import java.io.*;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.gjt.xpp.XmlStartTag;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;

import soaprmi.*;
import soaprmi.mapping.*;
import soaprmi.port.*;
import soaprmi.soap.*;
import soaprmi.soapenc.SoapEnc;

/**
 * SOAP service wrapper for local object.
 *
 * @version $Revision: 1.4 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class SoapDynamicSkeleton {
  private Map pool = new HashMap(); // Method -> List of Calls
  private XmlJavaMapping mapping;
  private Port port;
  Endpoint epoint;
  private XmlJavaPortTypeMap portTypeMap;
  private Object target;

  public SoapDynamicSkeleton (
    Object target_,
    Port port_,
    XmlJavaPortTypeMap portTypeMap_,
    Endpoint epoint_,
    XmlJavaMapping mapping_
  )
  {
    target = target_;
    port = port_;
    portTypeMap = portTypeMap_;
    epoint = epoint_;
    mapping = mapping_;
  }

  public Object getTarget() { return target; }
  public Port getPort() { return port; }
	
  public boolean dispatch(DeserializeContext dctx, XmlStartTag stag,
    Writer writer)
    throws IOException, DeserializeException, ServerException
  {
    MethodDispatcher md = newMethodDispatcher(stag.getLocalName());
    if(md == null) {
      throw new ServerException("no method named "+stag.getLocalName());
    }
    boolean success = md.dispatch(dctx, stag, writer);
    returnMethodDispatcherToPool(md);
    return success;
  }
  
  protected MethodDispatcher newMethodDispatcher(
    String methodRequestName)
    throws ServerException
  {
    List list = null;
    synchronized(pool) {
      list = (List) pool.get(methodRequestName);
      if(list == null) {
        list = new ArrayList();
        pool.put(methodRequestName, list);
      }
    }
    MethodDispatcher md = null;
    if(list.size() > 0) {
      synchronized(list) {
        if(list.size() > 0) {
          md = (MethodDispatcher) list.remove(list.size() - 1);
        }
      }
    }
    if(md == null) {
      try {
        XmlJavaOperationMap oprtn =
          portTypeMap.queryMethodRequest(methodRequestName);
        if(oprtn == null) return null;
        md = MethodDispatcher.createMethodDispatcher(
          target,
          port,
          oprtn,
          mapping
        );
       //System.err.println("new method dispatcher created md="+md);
      } catch(XmlMapException ex) {
        throw new ServerException("can't obtain mappingfor method "
          +methodRequestName, ex);
      }
    }
    return md;
  }

  protected void returnMethodDispatcherToPool(MethodDispatcher md) {
     List list = null;
     synchronized(pool) {
       list = (List) pool.get(md.getMethodRequestName());
       if(list == null) {
          throw new RuntimeException(
            "can not return to pool something that was not there..."
               +md.getMethodRequestName());
       }
     }
     synchronized(list) {
       list.add(md);
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

