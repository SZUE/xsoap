/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SoapSerializeContextImpl.java,v 1.15 2003/04/06 00:04:15 aslom Exp $
 */

package soaprmi.soap;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.mapping.XmlMapException;
import soaprmi.util.serp.IdentityMap;

/**
 * Standard implementation serialization context.
 *
 * @version $Revision: 1.15 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class SoapSerializeContextImpl implements SoapSerializeContext {

    public SoapSerializeContextImpl(Soap factory_) {
        factory = factory_;
        init();
    }

    // -- setup

    public EncodingStyle getDefaultEncodingStyle() {
        return enc;
    }

    public void setDefaultEncodingStyle(EncodingStyle enc_) {
        enc = enc_;
    }

    public XmlJavaMapping getMapping()  {
        return (mapping == Soap.getDefault().getMapping()) ? null : mapping;
    }

    public void setMapping(XmlJavaMapping mapping_) {
        mapping = mapping_;
        if(mapping == null) {// || mapping == Soap.getDefault().getMapping() )
            mapping = Soap.getDefault().getMapping();
        }
        //else
        //mapping.connectTo(Soap.getDefault().getMapping());
    }


    public Writer getWriter() {
        return writer;
    }

    public void setWriter(Writer writer_) throws SerializeException {
        if(!closed) {
            throw new SerializeException(
                "context must be closed before it can be reused");
        }
        reset();
        closed = false;
        writer = writer_;
    }

    public SoapStyle getSoapStyle() {
        return style;
    }

    public void setSoapStyle(SoapStyle style_) {
        style = style_;
    }

    // --- actions

    public void writeObject(
        Object o,
        String name,
        Class baseType) throws SerializeException, IOException
    {
        // use enc to delegate work
        enc.writeObject(this, o, name, baseType);
    }

    public void writeObject(
        Object o,
        String name) throws SerializeException, IOException
    {
        enc.writeObject(this, o, name, (o != null) ? o.getClass() : null);
    }

    public void writeObject(Object o) throws SerializeException, IOException {
        enc.writeObject(this, o, "o"+(++nameId), (o!=null) ? o.getClass() : null);
    }

    public void writeFloat(
        float f,
        String name) throws SerializeException, IOException
    {
        enc.writeFloat(this, f, name, style.XSI_TYPED);
    }

    public void writeFloat(float f) throws SerializeException, IOException
    {
        enc.writeFloat(this, f, "f"+(++nameId), style.XSI_TYPED);
    }

    public void writeInt(
        int i,
        String name) throws SerializeException, IOException
    {
        enc.writeInt(this, i, name, style.XSI_TYPED);
    }

    public void writeInt(int i) throws SerializeException, IOException
    {
        enc.writeInt(this, i, "i"+(++nameId), style.XSI_TYPED);
    }

    public void writeString(
        String s,
        String name) throws SerializeException, IOException
    {
        enc.writeString(this, s, name, style.XSI_TYPED);
    }

    public void writeString(String s) throws SerializeException, IOException {
        enc.writeString(this, s, "s"+(++nameId), style.XSI_TYPED);
    }


    // --- namespace handling

    public void writeXsiNull(String name, String uri, String localName)
        throws  SerializeException, IOException
    {
        writeStartStartTag(name, uri, localName, false, null);
        writeXsiNull();
        writer.write("/>\n");
    }

    public void writeXsiNull(String name)
        throws SerializeException, IOException
    {
        writeXsiNull(name, null, null);
    }

    public void writeXsiNull() throws IOException {
        if(Soap.XSI_NS_1999 == Soap.XSI_NS_CURR) {
            writer.write(" xsi:null='1'");
        } else if(Soap.XSI_NS_2001 == Soap.XSI_NS_CURR) {
            writer.write(" xsi:nil='1'");
        } else {
            throw new IllegalStateException("unsupporte namespace "+Soap.XSI_NS_CURR);
        }
        //if(!declaredNamespaces) writeXmlnsXsi();
        if(!declaredNamespaces) writeNamespaces();
    }

    public void writeXsiType(String uri, String localName) throws IOException {
        if(uri == null)
            throw new IllegalArgumentException("uri can not be null");
        if(localName == null)
            throw new IllegalArgumentException("localName can not be null");
        writer.write(" xsi:type='");
        writer.write("ns");
        if(level > 0)
            writer.write(Integer.toString(level));
        writer.write(':');
        writer.write(localName);
        writer.write('\'');
        //TODO write xmlsn _if_ necessary and add to ns scope
        writeXmlnsLevel(uri);
        //if(!declaredNamespaces) writeXmlnsXsi();
        if(!declaredNamespaces) writeNamespaces();
    }

    public void writeXsdType(String localName) throws IOException {
        if(localName == null)
            throw new IllegalArgumentException("localName can not be null");
        writer.write(" xsi:type='xsd:");
        writer.write(localName);
        writer.write('\'');
        //if(!declaredNamespaces) writeXmlnsXsd();
        if(!declaredNamespaces) writeNamespaces();
    }

    /**
     * Write namespaces that are required - should be called on top level element.
     * If was already called will not write anything.
     */
    public void writeNamespaces() throws IOException {
        if(declaredNamespaces) return;
        writeXmlns(style.SOAP_ENV_NS_PREFIX, style.SOAP_ENV_NS);
        writeXmlns(style.SOAP_ENC_NS_PREFIX, style.SOAP_ENC_NS);
        writeXmlnsXsi();
        writeXmlnsXsd();
        declaredNamespaces = true;
    }

    public void writeXmlnsXsd() throws IOException {
        writer.write(" xmlns:xsd='");
        writer.write(style.XSD_NS);
        writer.write('\'');
    }

    public void writeXmlnsXsi() throws IOException {
        writer.write(" xmlns:xsi='");
        writer.write(style.XSI_NS);
        writer.write('\'');
    }

    public void writeXmlns(String prefix, String uri)
        throws IOException
    {
        writer.write(" xmlns:");
        writer.write(prefix);
        writer.write("='");
        writer.write(uri);
        writer.write('\'');
    }
    public void writeXmlnsLevel(String uri)
        throws IOException
    {
        writer.write(" xmlns:ns");
        if(level > 0)
            writer.write(Integer.toString(level));
        writer.write("='");
        writer.write(uri);
        writer.write('\'');
    }

    // --- support for SOAP specific functionality

    // generic SOAP encoding writing

    public void writeRef(
        String name,
        String href)  throws SerializeException, IOException
    {
        if(href == null) throw new IllegalStateException("href can not be null");
        writer.write('<');
        writer.write(name);
        //SoapStyle style = sctx.getSoapStyle();
        //out.write(' ');
        //out.write(style.SOAP_ENC_NS_PREFIX);
        writer.write(" href='#");
        writer.write(href);
        writer.write('\'');
        //if(style.XSI_TYPED) { -- seems not necessarry
        // sctx.writeXsiType(uri, localName);
        //}
        writer.write("/>\n");
    }


    public String writeStartStartTag(
        String name,
        String uri,
        String localName,
        boolean forceXsiType,
        String id)
        throws SerializeException, IOException
    {
        writer.write('<');
        if(name != null) {
            writer.write(name);
        } else {
            if(level != 0)
                throw new SerializeException("only root elements can have null name");
            writer.write("ns");
            if(level > 0)
                writer.write(Integer.toString(level));
            writer.write(':');
            if(localName == null)
                throw new SerializeException("only root elements can have null name");
            writer.write(localName);

            name = "ns"+((level > 0)?Integer.toString(level):"")
                +':'+localName;
        }
        if(id != null) {
            writer.write(" id='");
            writer.write(id);
            writer.write('\'');
        }
        writeNamespaces();
        if((style.XSI_TYPED && uri != null && localName != null) || forceXsiType) {
            writeXsiType(uri, localName);
        }  else if(uri != null) {
            writeXmlnsLevel(uri);
        }
        //writer.write(">\n");
        return name;
    }

    public String writeStartTag(
        String name,
        String uri, String localName,
        boolean forceXsiType,
        String id)
        throws SerializeException, IOException
    {
        String ename = writeStartStartTag(name, uri, localName, forceXsiType, id);
        writer.write(">\n");
        return ename;
    }


    public void writeEndTag(String name) throws IOException
    {
        writer.write("</");
        writer.write(name);
        writer.write(">\n");
    }


    // --- SOAPRPC specific???

    // --- general


    public void init() {
        reset();
        closed = true;
        enc = null;
        writer = null;
        mapping = Soap.getDefault().getMapping();
    }

    public void done() throws IOException, SerializeException {
        // serialize all remianing references from queue
        serializeRefIds();
    }

    public void close() throws SerializeException, IOException {
        if(closed) {
            throw new SerializeException("context is already closed");
        }
        //writer.close();
        if(writer != null) {
            writer.flush();
        }
        closed = true;
        reset();
        if(factory != null) {
            factory.returnSerializeContextToPool(this);
        }
    }


    // --- utility methods for serializers

    public XmlJavaTypeMap queryTypeMap(Class javaClass)
        throws SerializeException, XmlMapException
    {
        return mapping.queryTypeMap(enc.getEncodingStyleUri(), javaClass);
    }

    //public Serializer querySerializer(EncodingStyle enc, XmlJavaTypeMap map)
    public Serializer querySerializer(EncodingStyle enc, Class klass)
        throws SerializeException, IOException
    {
        //String serializerName = map.getSerializerName();
        //if(serializerName != null) {
        // TODO: dynamic loading of serialziers...
        //TODO chek if map.getEncodingStyleUri() is new
        //TODO get  serializer name and o enc.querySerializerByName()
        //throw new IllegalStateException(
        //  "dynamic serializers not supported yet");
        //}

        //if(map instanceof XmlJavaStructMap)

        //    if(soaprmi.server.RemoteRef.class.isAssignableFrom(klass))
        //      return remoteRefSerializer;
        //TODO FIXME FIXME much better ot have dynamic registration!!!!

        try {
            Serializer ser = enc.querySerializer(klass);
            if(ser != null) return ser;
        } catch(SoapException ex) {
            ex.printStackTrace();
        }


        if(java.util.Date.class.isAssignableFrom(klass)) {
            return dateHandler;
        }

        //  throw new SerializeException("expected RemoteRef not "+o.getClass());
        if(klass.isArray()) {
            if( Byte.TYPE.equals( klass.getComponentType() ) ) {
                return base64Handler;
            } else {
                return enc.defaultArraySerializer();
            }
        }

        return enc.defaultStructSerializer();
    }

    // --- keeping up with nesting

    public void enterStruct() { ++level; }

    public void leaveStruct() {
        --level;
        if(level < 0) {
            throw new IllegalStateException("xml nesting level can not be negative");
        }
    }

    public int structLevel() { return level; }

    //public void setInPool(boolean inPool_) {
    //  inPool = inPool_;
    //}

    // --- mult-ref support

    public String addRef(
        Serializer typeSer,
        EncodingStyle typeEnc,
        Object typeValue,
        String name,
        Class baseClass)
    {
        // if we have laready ref id - do not duplicate
        String id = getId(typeValue);
        if(id != null) {
            return id;
        }
        if(queueEnd >= queue.length) {
            int newSize = queue.length * 2;
            if(newSize < 16) newSize = 16;
            RefId[] newQueue = new RefId[newSize];
            System.arraycopy(queue, 0, newQueue, 0, queueEnd);
            for(int i = queueEnd; i < newSize; ++i) {
                newQueue[i] = new RefId();
            }
            queue = newQueue;
        }
        RefId ref = queue[queueEnd++];
        ref.ser = typeSer;
        ref.enc = typeEnc;
        ref.value = typeValue;
        //ref.typemap = typemap;
        ref.baseClass = baseClass;
        ref.name = name;
        ref.id = addId(typeValue);
        return ref.id;
    }

    //private Map mapValueToId = new HashMap();
    private Map mapValueToId = new IdentityMap();

    private void idMapClear() {
        mapValueToId.clear();
    }

    private String idMapGet(Object value) {
        return (String) mapValueToId.get(value);
    }

    private void idMapPut(Object value, String id) {
        mapValueToId.put(value, id);
    }

    public String addId(Object value) {
        if(value == null)
            throw new IllegalArgumentException("null objects can not have id added...");
        String id = idMapGet(value);
        if(id == null) {
            id = "id" + (++lastId);
            idMapPut(value, id) ; //mapValueToId.put(value, id);
        }
        return id;
    }

    public String getId(Object value) {
        if(value == null)
            throw new IllegalArgumentException("null objects can not have id...");
        return idMapGet(value);
    }

    public String setObjectId(Object value, String id) {
        if(value == null)
            throw new IllegalArgumentException("null objects can not have id set...");
        if(id == null)
            throw new IllegalArgumentException("object id can not be null...");
        String oldId = idMapGet(value);
        idMapPut(value, id) ; //mapValueToId.put(value, id);
        return oldId;
    }

    private void serializeRefIds() throws IOException, SerializeException {
        RefId ref = null;
        try {
            while(queueStart < queueEnd) {
                ref = queue[queueStart++];
                ref.ser.writeObject(
                    this, ref.enc, ref.value, /*ref.name*/null, ref.baseClass, ref.id);
            }
        } catch(XmlMapException ex) {
            throw new SerializeException("can't write referenced object " +
                                             (ref!=null ? ref.value : ref), ex);
        }
    }

    private RefId[] queue = new RefId[0];
    private int queueStart;
    private int queueEnd;
    private int lastId;
    private class RefId {
        Serializer ser;
        EncodingStyle enc;
        Object value;
        String name;
        //XmlJavaTypeMap typemap;
        Class baseClass;
        String id;
    }



    // -- private state

    private void reset() {
        level = 0;
        nameId = 0;
        declaredNamespaces = false;
        // multiref support
        queueStart = 0;
        queueEnd = 0;
        lastId = 0;
        idMapClear();
    }

    private int level;
    private int nameId;
    private EncodingStyle enc;
    private SoapStyle style = SoapStyle.getDefaultSoapStyle();
    private Writer writer;
    private XmlJavaMapping mapping;
    private Soap factory;
    //private boolean inPool;
    private boolean closed;

    // TODO nicer would be more general pushNs(prefix, uri) etc,
    // TODO this will do for now but later...
    private boolean declaredNamespaces;
    //  private static final Serializer remoteRefSerializer
    //    = new soaprmi.server.RemoteRefSerializer();

    private soaprmi.soapenc.DateHandler dateHandler = new soaprmi.soapenc.DateHandler();
    private soaprmi.soapenc.Base64Handler base64Handler = new soaprmi.soapenc.Base64Handler();
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


