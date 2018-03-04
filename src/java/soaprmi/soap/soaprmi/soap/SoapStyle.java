/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: SoapStyle.java,v 1.7 2003/04/06 00:04:15 aslom Exp $
 */

package soaprmi.soap;

/**
 * Define attributes for different SOAP envelope styles.
 *
 * @version $Revision: 1.7 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */



public class SoapStyle {
    /**
     * IBM/Apache SOAP compatible serialization
     */
    public final static SoapStyle APACHESOAP =
        new SoapStyle(true, true, true, true, true);
    public final static SoapStyle IBMSOAP =
        new SoapStyle(true, false, true, true, true);
    /**
     *  Microsoft SOAP compatible
     */
    public final static SoapStyle MSSOAP =
        new SoapStyle(false, true, true, false, true);
    /**
     *  SOAP serialzation as in examples of SOAP 1.1 spec
     */
    public final static SoapStyle SOAP11 = APACHESOAP;
    /**
     *  SOAP serialization best suited for emebedding into XML documents
     */
    public final static SoapStyle DOCUMENT =
        new SoapStyle(true, false, true, false, false);
    //public final static SoapStyle SOAP10 = MSSOAP;


    private static SoapStyle defaultSoapStyle = APACHESOAP;

    public final String XSD_NS = Soap.XSD_NS_CURR;
    public final String XSI_NS = Soap.XSI_NS_CURR;

    public final String SOAP_ENC_NS = Soap.SOAP_ENC_NS;
    public final String SOAP_ENC_NS_PREFIX = Soap.SOAP_ENC_NS_PREFIX;
    public final String SOAP_ENV_NS = Soap.SOAP_ENV_NS;
    public final String SOAP_ENV_NS_PREFIX = Soap.SOAP_ENV_NS_PREFIX;

    /**
     * Make deep serialization (all references are expanded and serialzied).
     */
    public final boolean DEEP_SER;


    /**
     * Use multi-ref (id and href) to serialize graph.
     */
    public final boolean MULTI_REF;

    /**
     * Include namespaces for serialized root element.
     */
    public final boolean USE_NS;

    /**
     * Add xsi:type for each serialized element.
     */
    public final boolean XSI_TYPED;

    /**
     * Serialzie object that are null?
     */
    public final boolean SERIALIZE_NULL;

    protected SoapStyle(
        boolean deepSer,
        boolean multiRef,
        boolean useNs,
        boolean xsiTyped,
        boolean serializeNull)
    {
        DEEP_SER = deepSer;
        MULTI_REF = multiRef;
        USE_NS = useNs;
        XSI_TYPED = xsiTyped;
        SERIALIZE_NULL = serializeNull;
    }

    public static SoapStyle getDefaultSoapStyle() {
        return defaultSoapStyle;
    }

    public static void setDefaultSoapStyle(SoapStyle style) {
        defaultSoapStyle = style;
    }

    public static SoapStyle guess(
        boolean hadSoapAction,
        boolean hadMethodNs,
        boolean hadXsiType,
        boolean hadTrailingBody)
    {
        return SOAP11;
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

