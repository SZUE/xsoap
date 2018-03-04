/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: BeanAccessor.java,v 1.5 2003/04/06 00:04:24 aslom Exp $
 */

package soaprmi.struct;

import java.beans.*;
import java.lang.reflect.*;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

//import soaprmi.util.Check;

/**
 * Make bean properties available through for struct acccessors.
 *
 * @version $Revision: 1.5 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class BeanAccessor implements StructAccessor {

  public BeanAccessor() {
  }

  public BeanAccessor(Class klass) throws StructException {
    //if(Log.ON) l = Log.getLogger(getClass());
    this.klass = klass;
    //TODO experiment with its performance...//HashMap((4 * descs.length)/3 + 1);
    propertyMapping = new TreeMap();
    // do bean introspection
    try {
      BeanInfo info = Introspector.getBeanInfo(klass);
      PropertyDescriptor[] descs = info.getPropertyDescriptors();
      // fill propertyMapping
      for(int i = 0; i < descs.length; ++i) {
        //TODO check of both read write methods available (read-only properites)?
        BeanAccessorEntry entry = new BeanAccessorEntry(
          descs[i].getPropertyType() , descs[i].getName(),
          descs[i].getReadMethod(), descs[i].getWriteMethod() );
        if(entry.propertyType != Class.class) {
          if(entry.read == null) {
            throw new StructException("bean get method required for property '"
              +entry.name+"' in class: "+klass);
          }
          if(entry.write == null) {
            throw new StructException("bean set method required for property '"
              +entry.name+"' in class: "+klass);
          }
          //if(Log.ON) l.debug("added "+entry);
          propertyMapping.put(descs[i].getName(), entry);
        } else {
          //if(Log.ON) l.debug("skipping class property "+entry);
        }
      }
      Set set = propertyMapping.keySet();
      names = new String[set.size()];
      set.toArray(names);
    } catch (IntrospectionException ex) {
      throw new StructException("can't introspect bean: "+klass, ex);
    }
  }

  public Object newInstance() throws StructException {
    //bean = Class.forName(javaType).instantinate();
    //target = Beans.instantiate(getClass().getClassLoader(), javaType.getName());
    try {
      return klass.newInstance();
    } catch(InstantiationException ex) {
      throw new StructException("class initializer not accessible for "+klass, ex);
    } catch(IllegalAccessException ex) {
      throw new StructException("can not initialize "+klass, ex);
    }
  }

  public boolean primitivesWrapped() {
    return true;
  }

  public StructAccessor makeStructAccessor(Class accessorType)
    throws StructException
  {
    return new BeanAccessor(accessorType);
  }


  public void setValue(Object target, String accessorName, Object value)
    throws StructException
  {
    if(accessorName == null)
      throw new IllegalArgumentException("accessorName can not be null");
    BeanAccessorEntry entry =
      (BeanAccessorEntry) propertyMapping.get(accessorName);
    if(entry == null) {
      throw new StructException("no propery named "+accessorName);
    }
    Object[] writeParams = {value};
    Throwable thrown = null;
    try {
      entry.write.invoke(target, writeParams);
    } catch(InvocationTargetException ex) {
      thrown = ex;
    } catch(IllegalAccessException ex) {
      thrown = ex;
    } catch(IllegalArgumentException ex) {
      thrown = ex;
    }
    if(thrown != null) {
      throw new StructException("can't set property "+accessorName
                                +" to value '"+value+"'"
                                +" of class '"+value.getClass()+"'"
                                +" for struct type "
                                +klass, thrown);
    }
  }

  public Object getValue(Object target, String accessorName)
    throws StructException
  {
    if(accessorName == null)
      throw new IllegalArgumentException("accessorName can not be null");
    BeanAccessorEntry entry =
      (BeanAccessorEntry) propertyMapping.get(accessorName);
    if(entry == null) {
      throw new StructException("no propery named "+accessorName);
    }
    try {
      return entry.read.invoke(target, null);
    } catch(InvocationTargetException ex) {
      throw new StructException("can't get property "
                                  +accessorName+" for type "+klass, ex);
    } catch(IllegalAccessException ex) {
      throw new StructException("can't get property "
                                  +accessorName+" for type "+klass, ex);
    } catch(IllegalArgumentException ex) {
      throw new StructException("can't get property "
                                    +accessorName+" for type "+klass, ex);
    }
  }

  public Class getAccessorType(String accessorName) throws StructException {
    if(accessorName == null)
      throw new IllegalArgumentException("accessorName can not be null");
    BeanAccessorEntry entry =
       (BeanAccessorEntry) propertyMapping.get(accessorName);
    if(entry == null) {
      throw new StructException("no propery named "+accessorName);
    }
    return entry.propertyType;
  }

  public String[] getAccessorNames() {
    return names; //.clone()
  }

  public void mapAccessorToName(String accessorName, String name)
    throws StructException
  {
    //if(Check.ON) Check.assert(name != null);
    if(name == null)
      throw new IllegalArgumentException("name to map can not be null");
    if(accessorName == null)
      throw new IllegalArgumentException("accessorName can not be null");

    if(propertyMapping.get(name) != null) {
      throw new StructException("new name "+name
        +" conflicts with already defined property "+accessorName);
    }
    BeanAccessorEntry entry =
      (BeanAccessorEntry) propertyMapping.get(accessorName);
    if(entry == null) {
      throw new StructException("no propery named "+accessorName);
    }
    propertyMapping.remove(accessorName);
    propertyMapping.put(name, entry);
    for(int i = 0; i < names.length; ++i) {
      if(accessorName.equals(names[i])) {
        names[i] = name;
        return;
      }
    }
    throw new StructException("list of property names incosistent with mapping");
  }



  // -- internal state

  class BeanAccessorEntry {

    public BeanAccessorEntry(Class c, String n, Method r, Method w) {
      propertyType = c;
      name = n;
      read = r;
      write = w;
    }

    public Class propertyType;
    public String name;
    public Method read;
    public Method write;

    public String toString() {
      return "["+getClass() + " property name="+name+" type="+propertyType+"]";
    }
  }

  private Map propertyMapping;
  private String[] names;
  //private Object[] readParams = new Object[1];
  //private Object[] writeParams = new Object[1];
  private Class klass;
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

