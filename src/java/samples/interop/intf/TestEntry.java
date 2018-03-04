/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: TestEntry.java,v 1.7 2003/04/06 00:04:06 aslom Exp $
 */

package interop.intf;

import java.io.Serializable;

/**
 * Class that has all kind of data types for tests.
 *
 * @version $Revision: 1.7 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class TestEntry implements TestableEntry, Serializable {

    public boolean      bboolean;
    public long         llong;
    public short        sshort;
    public int          iint;
    public float        ffloat;
    public char         cchar;  ///not supported in Apache SOAP?
    public double       ddouble;
    public String       sstring;
    public Object       oobject;
    public byte[]       bbyteArr;
    public byte[]       bbyteArr2;

    ////public Boolean B = new Boolean("true");
    protected String       protected_string = "protected";
    private String       private_string = "private";
    transient String       transient_string = "transient";

    public TestEntry() {
        bboolean = false;
        sshort = 1;
        iint = 0;
        llong = 3;
        ffloat = 4;
        ddouble = 5;
        //cchar = 's';
        sstring = "Hello World!";
    }

    public TestEntry(String s) {
        this();
        sstring = s;
    }

    public TestEntry(int i) {
        this();
        iint = i;
    }

    public int getIntValue() {
        return iint;
    }

    public void setIntValue(int inValue) {
        iint = inValue;
    }

    public String getStringValue() {
        return sstring;
    }

    public void setStringValue(String s) {
        sstring = s;
    }

    public short getShortValue() {
        return sshort;
    }

    public void setShortValue(short s) {
        sshort = s;
    }


    public long getLongValue() {
        return llong;
    }

    public void setLongValue(long l) {
        llong = l;
    }

    public float getFloatValue() {
        return ffloat;
    }

    public void setFloatValue(float f) {
        ffloat = f;
    }

    public double getDoubleValue() {
        return ddouble;
    }

    public void setDoubleValue(double d) {
        ddouble = d;
    }

    public char getCharValue() {
        return cchar;
    }

    public void setCharValue(char c) {
        cchar = c;
    }

    public Object getObjectValue() {
        return oobject;
    }

    public void setObjectValue(Object o) {
        oobject = o;
    }

    public void setBbyteArr(byte[] bbyteArr)
    {
        this.bbyteArr = bbyteArr;
    }

    public byte[] getBbyteArr()
    {
        return bbyteArr;
    }

    public void setBbyteArr2(byte[] bbyteArr2)
    {
        this.bbyteArr2 = bbyteArr2;
    }

    public byte[] getBbyteArr2()
    {
        return bbyteArr2;
    }

    public int hashCode() {
        int i = 0;
        if(sstring != null) i ^= sstring.hashCode();
        if(oobject != null) i ^= oobject.hashCode();
        if(bboolean) i++;
        i ^= llong;
        i ^= llong >> 32;
        i ^= sshort;
        i ^= iint;
        i ^= Float.floatToRawIntBits(ffloat);
        i ^= Double.doubleToRawLongBits(ddouble);;
        return i;
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        TestEntry other;
        try {
            other = (TestEntry) o;
        } catch (ClassCastException e) {
            return false;
        }
        return equalFields(this, other);
    }

    protected static boolean equalFields(TestEntry le, TestEntry other) {
        if(equalsRef(other.sstring, le.sstring)
           && equalsRef(other.oobject, le.oobject)
           && other.bboolean == le.bboolean
           && other.llong == le.llong
           && other.sshort == le.sshort
           && other.iint == le.iint
           && other.ffloat == le.ffloat
           //&& other.cchar == le.cchar
           && other.ddouble == le.ddouble
           && equalsByteArr(other.bbyteArr, le.bbyteArr)
           && equalsByteArr(other.bbyteArr2, le.bbyteArr2)
          )
        {
            return true;
        }
        return false;
    }

    protected static final boolean equalsRef(Object s1, Object s2) {
        return ((s1 != null && s1.equals(s2)) || (s1 == null && s1 == s2));
    }


    protected static final boolean equalsByteArr(byte[] b1, byte[] b2) {
        if(b1 == null || b2 == null) {
            return b1 == b2;
        }
        //assert b1 != null && b2 != null
        if(b1.length != b2.length) {
            return false;
        }
        for (int i = 0; i < b1.length; i++)
        {
            if(b1[i] != b2[i]) {
                return false;
            }
        }
        return true;
    }


    protected void fieldsToString(StringBuffer buf) {
        buf.append("fields"
                       //+" bboolean="+bboolean
                       //+" llong="+llong
                       //+" sshort"+sshort
                       +" iint="+iint
                       //+" float="+ffloat
                       ////  +" cchar="+cchar  ///not supported in Apache SOAP?
                       //+" ddouble="+ddouble
                       +" sstring="+sstring
                       +" ooobject="+oobject
                       //"link; //no ecursion
                  );
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(getClass().getName()+" {");
        fieldsToString(buf);
        buf.append("}");
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


