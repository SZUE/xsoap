/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: PackageTest.java,v 1.6 2003/04/06 00:04:27 aslom Exp $
 */

package util;

import java.io.IOException;
import java.io.StringWriter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.gjt.xpp.XmlEndTag;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;
import org.gjt.xpp.XmlPullParserFactory;
import org.gjt.xpp.XmlStartTag;
import soaprmi.util.Util;
import soaprmi.util.base64.Base64;

/**
 * Some tests to verify generic util.
 *
 */
public class PackageTest extends TestCase {

    private XmlStartTag stag;
    private XmlEndTag etag;
    private XmlPullParser pp;

    public PackageTest(String name) {
        super(name);
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }

    public static Test suite() {
        TestSuite suite= new TestSuite(
            "Util Tests for XSOAP (SoapRMI)");

        suite.addTest(new TestSuite(PackageTest.class));
        suite.addTest(new TestSuite(DynamicProxyTest.class));
        return suite;
    }

    protected void setUp() throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        stag = factory.newStartTag();
        etag = factory.newEndTag();
        pp = factory.newPullParser();
    }

    protected void tearDown() {
    }

    public void testVersionCheck()
    {
    }



    public void testXmlEscape(String s, String escaped) throws IOException
    {
        StringWriter sw = new StringWriter();
        Util.writeXMLEscapedString(sw, s);
        sw.close();
        String se = sw.toString();
        assertEquals(escaped, se);
    }

    private final String T1_STRING =
        " < &amp; <";
    private final String E1_STRING =
        " &lt; &amp;amp; &lt;";
    private final String T2_STRING =
        "\n \n\r\n ";
    private final String E2_STRING =
        "\n \n&#xD;\n ";
    private final String T3_STRING =
        "\n \n\n \r \n\r \r\n \r\n\r\n\r\r";
    private final String E3_STRING =
        "\n \n\n &#xD; \n&#xD; &#xD;\n "+
        "&#xD;\n&#xD;\n&#xD;&#xD;";
    private final String T7_STRING =
        "it <tag> this &amp;  a \"string\" <a> 'test'";
    private final String E7_STRING =
        "it &lt;tag> this &amp;amp;  a \"string\" &lt;a> 'test'";

    public void testXmlEscape() throws IOException {
        testXmlEscape(T1_STRING, E1_STRING);
        testXmlEscape(T2_STRING, E2_STRING);
        testXmlEscape(T3_STRING, E3_STRING);
        testXmlEscape(T7_STRING, E7_STRING);
    }

    public void testBase64(String test, String encoded) throws IOException {
        byte [] btest = test.getBytes("ASCII");
        //System.out.println("bt="+printable(btest));
        char[] c = Base64.encode(btest);

        //System.out.println("c ="+new String(c));
        //char[] cG = org.globus.util.Base64.encode(btest);
        //System.out.println("cG="+new String(cG));
        //assertEquals("dGXzdCb8PjyhW1tdXT7+Jm30Ow==", new String(c));
        //assertEquals(printable(cG), printable(c));
        //assertEquals("dGVzdCY8PjwhW1tdXT4+Jmx0Ow==", new String(c));
        assertEquals(encoded, new String(c));

        //byte[] d = Base64.decode(c);
        byte[] d = soaprmi.util.base64.Base64.decode(c);
        //byte[] dG = org.globus.util.Base64.decode(cG);
        //assertEquals(printable(d), printable(dG));
        //System.out.println("d ="+printable(d));
        //System.out.println("dG="+printable(dG));


        assertEquals(printable(btest), printable(d));

    }

    public void testBase64() throws IOException {
        //        byte[] d1 = "TEST".getBytes("ASCII"); //new byte[]{1, 2, 3};
        //        char[] c1 = Base64.encode(d1, 0, d1.length);
        //        System.out.println("encoded = "+new String(c1));
        //        assertEquals("VEVTVA==", printable(c1));
        //        c1 = org.globus.util.Base64.encode(d1);
        //        System.out.println("encoded = "+new String(c1));
        //        assertEquals("VEVTVA==", printable(c1));
        //
        //        byte[] r1 = Base64.decode(c1);
        //        assertEquals(printable(d1), printable(r1));

        //import ;
        //String test = ;
        testBase64("tes", "dGVz");
        testBase64("TEST", "VEVTVA==");
        testBase64("test&<><![[]]>>&lt;", "dGVzdCY8PjwhW1tdXT4+Jmx0Ow==");
    }

    public void testXmlRoundtrip(String s)
        throws IOException, XmlPullParserException
    {
        StringWriter sw = new StringWriter();
        Util.writeXMLEscapedString(sw, s);
        sw.close();
        String se = sw.toString();
        String xp = "<t>"+se+"</t>";
        pp.setInput(xp.toCharArray());
        pp.next();
        pp.next();
        String parsed = pp.readContent();
        assertEquals(s, parsed);
    }

    public void testXmlRoundtrip()
        throws IOException, XmlPullParserException
    {
        testXmlRoundtrip(T1_STRING);
        testXmlRoundtrip(T2_STRING);
        testXmlRoundtrip(T3_STRING);
        testXmlRoundtrip(T7_STRING);
    }

    private static final String printable(byte[] arr) {
        return printable(new String(arr, 0));
    }

    /** simple utility method -- good for debugging */
    private static final String printable(String s) {
        return printable(s.toCharArray());
    }

    private static final String printable(char[] arr) {
        StringBuffer retval = new StringBuffer();
        char ch;
        for (int i = 0; i < arr.length; i++) {
            switch (ch = arr[i])
            {
                case 0 :
                    continue;
                case '\b':
                    retval.append("\\b");
                    continue;
                case '\t':
                    retval.append("\\t");
                    continue;
                case '\n':
                    retval.append("\\n");
                    continue;
                case '\f':
                    retval.append("\\f");
                    continue;
                case '\r':
                    retval.append("\\r");
                    continue;
                case '\"':
                    retval.append("\\\"");
                    continue;
                case '\'':
                    retval.append("\\\'");
                    continue;
                case '\\':
                    retval.append("\\\\");
                    continue;
                default:
                    if (ch < 0x20 || ch > 0x7e) {
                        String ss = "0000" + Integer.toString(ch, 16);
                        retval.append("\\u" + ss.substring(ss.length() - 4, ss.length()));
                    } else {
                        retval.append(ch);
                    }
                    continue;
            }
        }
        return retval.toString();
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


