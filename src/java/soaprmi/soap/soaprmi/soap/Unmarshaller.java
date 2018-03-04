/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Unmarshaller.java,v 1.8 2003/04/06 00:04:15 aslom Exp $
 */

package soaprmi.soap;

import java.io.IOException;
import java.io.Reader;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlStartTag;
import soaprmi.mapping.XmlJavaMapping;
import soaprmi.soapenc.SoapEnc;

/**
 * Utility class to unmarshal object from XML.
 *
 * @version $Revision: 1.8 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */


public class Unmarshaller {

    public static Object unmarshal(Class baseClass, Reader reader)
        throws MarshalException, ValidationException
    {
        return unmarshal(baseClass, reader, Soap.getDefault().getMapping());
    }

    public static Object unmarshal(Class baseClass, Reader reader,
                                   XmlJavaMapping mapping)
        throws MarshalException, ValidationException
    {
        if(baseClass == null)
            throw new ValidationException("can't unmarshal with null class type");
        try {
            //DeserializeContext dctx = new DeserializeContextImpl(null);
            DeserializeContext dctx = Soap.getDefault().getDeserializeContext();
            dctx.setReader(reader);
            dctx.setDefaultEncodingStyle(SoapEnc.getDefault());
            dctx.setMapping(mapping);

            Object o = dctx.readObject(baseClass);
            dctx.done();
            dctx.close();
            Soap.getDefault().returnDeserializeContextToPool(dctx);
            return o;
        } catch(DeserializeException ex) {
            throw new ValidationException("can't unmarshal object", ex);
        } catch(SoapException ex) {
            throw new MarshalException(
                "can't unmarshal object - soap exception", ex);
        } catch(IOException ex) {
            throw new MarshalException(
                "can't unmarshal object - IO problem ", ex);
        }
    }

    public static Object unmarshal(Class baseClass, XmlPullParser parser)
        throws MarshalException, ValidationException
    {
        return unmarshal(baseClass, parser, Soap.getDefault().getMapping());
    }

    public static Object unmarshal(Class baseClass, XmlPullParser parser,
                                   XmlJavaMapping mapping)
        throws MarshalException, ValidationException
    {
        return unmarshal(baseClass, parser, mapping, false);

    }

    public static Object unmarshal(Class baseClass, XmlPullParser parser,
                                   XmlJavaMapping mapping,
                                   boolean deserializePastRoot)
        throws MarshalException, ValidationException
    {
        if(baseClass == null)
            throw new ValidationException("can't unmarshal with null class type");
        try {
            //DeserializeContext dctx = new DeserializeContextImpl(null);
            DeserializeContext dctx = Soap.getDefault().getDeserializeContext();
            if(parser.isNamespaceAware() == false) {
                throw new MarshalException(
                    "parser must have namespaces enabled to unmarshal SOAP/XML content");
            }
            if(parser.isAllowedMixedContent() == true) {
                throw new MarshalException(
                    "parser must have mixed cotnent disabled to unmarshal SOAP/XML content");
            }

            dctx.setPullParser(parser);
            EncodingStyle enc = SoapEnc.getDefault();
            dctx.setDefaultEncodingStyle(enc);
            dctx.setMapping(mapping);

            XmlStartTag stag = dctx.getStartTag();
            parser.readStartTag(stag);
            //Object o = dctx.readObject(baseClass);
            Object o = enc.readObject(dctx, baseClass, parser, stag);
            if(deserializePastRoot) {
                dctx.done();
            }
            dctx.close();
            Soap.getDefault().returnDeserializeContextToPool(dctx);
            return o;
        } catch(DeserializeException ex) {
            throw new ValidationException("can't unmarshal object", ex);
        } catch(XmlPullParserException ex) {
            throw new ValidationException("can't unmarshal object", ex);
        } catch(SoapException ex) {
            throw new MarshalException(
                "can't unmarshal object - soap exception", ex);
        } catch(IOException ex) {
            throw new MarshalException(
                "can't unmarshal object - IO problem ", ex);
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


