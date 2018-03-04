/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SoapDeserializeContextImpl.java,v 1.12 2004/05/06 18:18:47 aslom Exp $
 */

package soaprmi.soap;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import org.gjt.xpp.XmlEndTag;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlPullParserFactory;
import org.gjt.xpp.XmlStartTag;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.mapping.XmlMapException;
import soaprmi.struct.StructAccessor;
import soaprmi.struct.StructException;
import soaprmi.util.Check;
import soaprmi.util.Util;
import soaprmi.util.logging.Logger;

/**
 * Standard implementation for SOAP deserialization context.
 *
 * @version $Revision: 1.12 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class SoapDeserializeContextImpl
    implements SoapDeserializeContext
{

    // package access - only factory can churn out contexts
    SoapDeserializeContextImpl(Soap factory_)
        throws DeserializeException
    {
        factory = factory_;
        try {
            //XmlPullParserFactory xp2f = XmlPullParserFactory.newInstance();
            XmlPullParserFactory xp2f = Util.getPullParserFactory();
            xp2f.setNamespaceAware(true);
            etag = xp2f.newEndTag();
            stag = xp2f.newStartTag();
            cachedParser = xp2f.newPullParser();
            init();
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "could not create serialize context", ex);
        }
    }

    // -- setup

    public EncodingStyle getDefaultEncodingStyle() {
        return enc;
    }

    public void setDefaultEncodingStyle(EncodingStyle enc_) {
        enc = enc_;
    }

    public XmlJavaMapping getMapping() {
        return mapping;
    }

    public void setMapping(XmlJavaMapping mapping_) {
        mapping = mapping_;
        if(mapping == null)
            mapping = Soap.getDefault().getMapping();
    }

    public XmlPullParser getPullParser() {
        return pp;
    }

    public XmlStartTag getStartTag() {
        return stag;
    }

    public XmlEndTag getEndTag() {
        return etag;
    }

    public void setReader(Reader reader_)
        throws SoapException
    {
        if(!closed) {
            throw new SoapException("context must be closed before it can be reused");
        }
        try {
            reset();
            reader = reader_;
            pp.setInput(reader);
        } catch(XmlPullParserException ex) {
            throw new SoapException(
                "reader could not be set for pull parser", ex);

        }
        closed = false;
    }

    public void setPullParser(XmlPullParser parser)
        throws SoapException
    {
        if(!closed) {
            throw new SoapException("context must be closed before it can be reused");
        }
        try {
            reset();
            //reader = reader_;
            reader = null;
            //pp.setInput(reader);
            pp = parser;

            //if(pp.getEventType() != pp.START_TAG) {
            //    throw new SoapException(
            //        "pull parser must be on start tag and not "+pp.getPosDesc());
           // }
        } catch(XmlPullParserException ex) {
            throw new SoapException(
                "reset failed when setting pull parser", ex);

        }
        closed = false;
    }

    // --- work

    void init() throws XmlPullParserException {
        reset();
        enc = null;
        reader = null;
        //pp.reset();
        pp.setAllowedMixedContent(false);
        mapping = Soap.getDefault().getMapping();
        closed = true;
    }

    public byte done() throws DeserializeException, IOException {
        try {
            byte state = pp.next();
            if(state == XmlPullParser.START_TAG) {
                pp.readStartTag(stag);
                return readRoots(pp, stag);
            }
            return state;
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "xml parsing exception when deserializing roots", ex);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() throws DeserializeException, IOException {
        if(closed) {
            throw new DeserializeException("context is already closed");
        }
        closed = true;
        //reader.close();
        fixup();
        try {
            reset();
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "could not close context - reset problem", ex);
        }

        if(factory != null) {
            factory.returnDeserializeContextToPool(this);
        }
    }

    public Object readObject(Class expectedType)
        throws DeserializeException, IOException
    {
        //if(expectedType == String.class)
        //  return readString();
        try {
            if(pp.next() != XmlPullParser.START_TAG)
                throw new DeserializeException("expected start tag"+pp.getPosDesc());
            pp.readStartTag(stag);
            //TODO: stag.getValue(style.SOAP_ENV_NS, "encodingStyle") ....
            return enc.readObject(this, expectedType, pp, stag);
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "xml parsing exception when deserializing", ex);
        }
    }


    public double readDouble() throws DeserializeException, IOException {
        try {
            if(pp.next() != XmlPullParser.START_TAG)
                throw new DeserializeException("expected start tag");
            //we could chose not to read tag - we know element type -
            // ... but it is more consistent :-)
            pp.readStartTag(stag);
            return enc.readDouble(this, pp, stag);
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "xml parsing exception when deserializing", ex);
        }
    }


    public float readFloat() throws DeserializeException, IOException {
        try {
            if(pp.next() != XmlPullParser.START_TAG)
                throw new DeserializeException("expected start tag");
            //we could chose not to read tag - we know element type -
            // ... but it is more consistent :-)
            pp.readStartTag(stag);
            return enc.readFloat(this, pp, stag);
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "xml parsing exception when deserializing", ex);
        }
    }


    public int readInt() throws DeserializeException, IOException {
        try {
            if(pp.next() != XmlPullParser.START_TAG)
                throw new DeserializeException("expected start tag");
            //we could chose not to read tag - we know element type -
            // ... but it is more consistent :-)
            pp.readStartTag(stag);
            return enc.readInt(this, pp, stag);
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "xml parsing exception when deserializing", ex);
        }
    }

    public String readString() throws DeserializeException, IOException {
        try {
            if(pp.next() != XmlPullParser.START_TAG)
                throw new DeserializeException("expected start tag");
            pp.readStartTag(stag);
            return enc.readString(this, pp, stag);
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "xml parsing exception when deserializing", ex);
        }
    }

    // --- utility methods for deserializers


    public XmlJavaTypeMap queryTypeMap(EncodingStyle enc, Class javaClass)
        throws XmlMapException
    {
        return mapping.queryTypeMap(enc.getEncodingStyleUri(), javaClass);
    }

    public XmlJavaTypeMap queryTypeMap(EncodingStyle enc,
                                       String uri,
                                       String localName)
        throws XmlMapException
    {
        return mapping.queryTypeMap(
            enc.getEncodingStyleUri(), uri, localName);
    }

    public Deserializer queryDeserializer(EncodingStyle enc,
                                          XmlJavaTypeMap map)
        throws DeserializeException
    {
        String deserializerName = map.getDeserializerName();
        if(deserializerName != null) {
            // TODO: dynamic loading of serialziers...
            //TODO chek if map.getEncodingStyleUri() is new
            //TODO get  serializer name and o enc.querySerializerByName()
            throw new IllegalStateException(
                "dynamic serializers not supported yet");
        }
        //TODO: make it NICER!!!!!!!!!!!
        try {
            Class klass = map.javaClass();
            try {
                Deserializer deser = enc.queryDeserializer(klass);
                if(deser != null) return deser;
            } catch(SoapException ex) {
            }
            if(klass.isArray()) {
                if( Byte.TYPE.equals( klass.getComponentType() ) ) {
                    return base64Handler;
                } else {
                    return enc.defaultArrayDeserializer();
                }
            }
            //TODO FIXME FIXME much better ot have dynamic registration!!!!
            if(java.util.Date.class.isAssignableFrom(klass)) {
                return dateHandler;   //FIXME FIXME
            }

        } catch(XmlMapException xme) {
        }

        //if(map instanceof XmlJavaStructMap)
        return enc.defaultStructDeserializer();
    }


    public void enterStruct() {
        ++level;
    }

    public void leaveStruct() {
        --level;
        if(level < 0) {
            throw new IllegalStateException("xml nesting level can not be negative");
        }
    }
    public int structLevel() { return level; }

    // --- maintaining multi-ref deserialization

    /**
     */
    public byte readRoots(XmlPullParser pp, XmlStartTag stag)
        throws DeserializeException, IOException
    {
        try {
            while(true) {
                //TODO: do magic and get encoding style from stag...
                enc.readObject(this, null, pp, stag);
                byte state = pp.next();
                //System.err.println("state="+state);
                if(state == XmlPullParser.END_TAG
                   || state == XmlPullParser.END_DOCUMENT)
                {
                    // when roots are read - make sure to run fixup method
                    fixup();
                    return state;
                }
                pp.readStartTag(stag);
            }
        } catch(XmlPullParserException ex) {
            throw new DeserializeException(
                "xml parsing exception when deserializing roots", ex);
        }
    }

    public void setIdValue(String id, Object value)
        throws DeserializeException
    {
        if(hasId(id))
            throw new DeserializeException(
                "id "+id+" already has value set, duplication "+value+pp.getPosDesc());
        id2Value.put(id, value);
    }

    public boolean hasId(String id) {
        return id2Value.containsKey(id);
    }

    /**
     *
     * @throws DeserializeException if there is no element associated with id
     */
    public Object getIdValue(String id) throws DeserializeException {
        if(! hasId(id))
            throw new DeserializeException("no element has id "+id+pp.getPosDesc());
        Object value = id2Value.get(id);
        return value;
    }

    public void addStructFixup(StructAccessor sa, Object target,
                               String accessorName, Class accessorType, String id)
    {
        if(Check.ON) Check.assertion(sa != null);
        if(Check.ON) Check.assertion(target != null);
        if(Check.ON) Check.assertion(accessorName != null);
        if(Check.ON) Check.assertion(id != null);
        if(structFixesEnd >= structFixes.length) {
            int newSize = structFixes.length * 2;
            if(newSize < 16) newSize = 16;
            StructFix[] newSF = new StructFix[newSize];
            System.arraycopy(structFixes, 0, newSF, 0, structFixesEnd);
            for(int i = structFixesEnd; i < newSize; ++i) {
                newSF[i] = new StructFix();
            }
            structFixes = newSF;
        }
        StructFix sf = structFixes[structFixesEnd++];
        sf.sa = sa;
        sf.target = target;
        sf.accessorName =  accessorName;
        sf.accessorType = accessorType;
        sf.id = id;
    }

    public void addArrayFixup(Object /*Array*/ target,
                              int pos, Class komponentType, String id)
    {
        //if(target == null) {
        //  throw new IllegalArgumentException(
        //    "array fiuo atrget can not be null "+target);
        //}
        //if(target.getClass().isArray() == false) {
        //  throw new IllegalArgumentException(
        //    "expected array for fixup not "+target.getClass());
        //}
        if(Check.ON) Check.assertion(target != null);
        if(Check.ON) Check.assertion(target.getClass().isArray());
        if(Check.ON) Check.assertion(pos >= 0);
        if(Check.ON) Check.assertion(komponentType != null);
        if(Check.ON) Check.assertion(id != null);
        if(arrayFixesEnd >= arrayFixes.length) {
            int newSize = arrayFixes.length * 2;
            if(newSize < 16) newSize = 16;
            ArrayFix[] newAF = new ArrayFix[newSize];
            System.arraycopy(arrayFixes, 0, newAF, 0, arrayFixesEnd);
            for(int i = arrayFixesEnd; i < newSize; ++i) {
                newAF[i] = new ArrayFix();
            }
            arrayFixes = newAF;
        }
        ArrayFix af = arrayFixes[arrayFixesEnd++];
        af.target = target;
        af.pos =  pos;
        af.komponentType = komponentType;
        af.id = id;
    }

    private void fixup() throws DeserializeException {
        try {
            for(int i = 0; i < structFixesEnd; ++i) {
                StructFix sf = structFixes[i];
                Object value = getIdValue(sf.id);
                if(Check.ON) Check.assertion(value != null);
                //System.err.println("fixup struct target="+sf.target+" accessor="
                //  +sf.accessorName+" id="+sf.id+" value="+value);
                sf.sa.setValue(sf.target, sf.accessorName,
                               convert(enc, value, sf.accessorType) );
                //TODO it should not be using just any encoding but encoding of registered
            }
            structFixesEnd = 0;

            for(int i = 0; i < arrayFixesEnd; ++i) {
                ArrayFix af = arrayFixes[i];
                Object value = getIdValue(af.id);
                if(Check.ON) Check.assertion(value != null);
                //System.err.println("fixup array target="+af.target+" pos="
                //  +af.pos+" id="+af.id+" value="+value);
                Array.set(af.target, af.pos,
                          convert(enc, value, af.komponentType) );
                //TODO it should not be using just any encoding but encoding of registered

            }
            arrayFixesEnd = 0;

        } catch(StructException ex) {
            throw new DeserializeException(
                "can't set value to multi-ref accessor", ex);
        }
    }

    private Map id2Value = new HashMap();

    private class StructFix {
        StructAccessor sa;
        Object target;
        String accessorName;
        Class accessorType;
        String id;
    };
    private int structFixesEnd;
    private StructFix structFixes[] = new StructFix[0];

    private class ArrayFix {
        Object target;
        int pos;
        Class komponentType;
        String id;
    };
    private int arrayFixesEnd;
    private ArrayFix arrayFixes[] = new ArrayFix[0];


    // --- converters

    public Object convert(EncodingStyle enc, Object source, Class expectedType)
        throws DeserializeException
    {
        if(source == null) return source;
        Class klass = source.getClass();
        if(klass == expectedType) return source;
        //if(Log.ON) l.log(Level.FINEST, "from "+source.getClass()+" to "+expectedType);
        //      if(cnvFrom != null) {
        //          Converter cnv = (Converter) cnvFrom.get(klass);
        Converter cnv = enc.queryConverterFrom(klass);
        if(cnv != null) {
            Object result = cnv.convert(source, expectedType);
            //if(Log.ON) l.log(Level.FINEST, "result="+result);
            if(result != null)
                return result;
        }
        //}
        //      if(cnvTo != null) {
        //          Converter cnv = (Converter) cnvTo.get(expectedType);
        if(expectedType != null) {
            cnv = (Converter) enc.queryConverterTo(expectedType);
            if(cnv != null) {
                Object result = cnv.convert(source, expectedType);
                if(result != null)
                    return result;
                //}
            }
        }
        return source;
    }

    // -- private state

    private void reset() throws XmlPullParserException {
        level = 0;
        nameId = 0;
        pp = cachedParser;
        pp.reset();
        id2Value.clear();
        structFixesEnd = 0;
        arrayFixesEnd = 0;
    }

    private static Logger l = Logger.getLogger();
    private int level;
    private int nameId;
    private EncodingStyle enc;
    private Reader reader;
    private XmlEndTag etag;
    private XmlStartTag stag;
    private XmlPullParser cachedParser;
    private XmlPullParser pp;
    private XmlJavaMapping mapping;
    private Soap factory;
    //private boolean inPool;
    private boolean closed;

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



