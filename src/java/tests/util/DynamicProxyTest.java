/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/

/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 * $Id: DynamicProxyTest.java,v 1.3 2003/04/06 00:04:27 aslom Exp $
 */

package util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import soaprmi.util.dynamic_proxy.ThrowableRewrapper;
import soaprmi.util.logging.Logger;

/**
 * Utility function tests
 *
 */
public class DynamicProxyTest extends TestCase {
    private static Logger l = Logger.getLogger();

    public static class FooException extends Exception {
        private Throwable detail;
        public FooException(String msg, Throwable t) {
            super(msg);
            detail = t;
        }
        Throwable getDetail() {
            return detail;
        }
    }

    public static class FooDerivedException extends FooException {
        public FooDerivedException(String msg, Throwable t) {
            super(msg, t);
        }
    }

    public static interface FooInterface {
        public void foo() throws FooException;
    }

    static class FooClass implements FooInterface {
        public void foo() throws FooException {
            //throw new RuntimeException("test");
            throw new FooException("example exception", null);
        }
    }

    /**
     * This invocation handler intentionally violates dynamic proxy contract
     * and throws undecallared exception (for testing purposes only).
     */
    static class FooInvocationHandler implements InvocationHandler {
        private Throwable throwableToUse;
        public FooInvocationHandler() {
        }
        public FooInvocationHandler(Throwable throwableToUse) {
            this.throwableToUse = throwableToUse;
        }
        public Object invoke(Object proxy, Method m, Object[] params)
            throws Throwable //RemoteException
        {
            if(throwableToUse != null) {
                throw throwableToUse;
            } else {
                throw new BarException("undeclared exception");
            }
        }
    }

    static class BarException extends Exception {
        public BarException(String msg) {
            super(msg);
        }
    }


    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
        System.exit(0);
    }

    public DynamicProxyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DynamicProxyTest.class);
    }

    protected void setUp() throws Exception {
    }

    public void testExampleException() throws Exception {
        FooClass example = new FooClass();
        try {
            example.foo();
        } catch(FooException e) {
            return;
        }
        fail("excepted exception");
    }

    public void testExampleExceptionThroughProxy() throws Exception {

        FooClass example = new FooClass();
        FooInterface wrapper = (FooInterface)
            ThrowableRewrapper.wrap(example,
                                    FooException.class,
                                    "wrapped exception");
        //TODO: make sure to generate UndeclaredThrowable
        try {
            example.foo();
        } catch(FooException e) {
            return;
        }
        fail("excepted exception");
    }


    public void testThrowingUndeclared() throws Exception
    {
        InvocationHandler invoker = new FooInvocationHandler();

        FooInterface example = (FooInterface) Proxy.newProxyInstance(
            invoker.getClass().getClassLoader(),
            new Class[] {FooInterface.class},
            invoker);

        try {
            example.foo();
        } catch(UndeclaredThrowableException e) {
            Throwable ut = e.getUndeclaredThrowable();
            assertTrue(ut instanceof BarException);
            return;
        }
        fail("excepted undelared throwable exception");
    }

    public void testExceptionUnwrapping() throws Exception
    {
        InvocationHandler invoker = new FooInvocationHandler();

        FooInterface unwrapped = (FooInterface) Proxy.newProxyInstance(
            invoker.getClass().getClassLoader(),
            new Class[] {FooInterface.class},
            invoker);

        FooInterface example = (FooInterface) ThrowableRewrapper.wrap(
            unwrapped,
            FooException.class,
            "could not execute");

        try {
            example.foo();
        } catch(FooException e) {
            return;
        }
        fail("excepted exception");
    }

    public void testPassingThroughOfExceptions() throws Exception
    {
        InvocationHandler invoker = new FooInvocationHandler(new FooException("foo-test", null));

        FooInterface unwrapped = (FooInterface) Proxy.newProxyInstance(
            invoker.getClass().getClassLoader(),
            new Class[] {FooInterface.class},
            invoker);

        try {
            unwrapped.foo();
        } catch(FooException e) {
            assertEquals("foo-test", e.getMessage());
        }

        FooInterface example = (FooInterface) ThrowableRewrapper.wrap(
            unwrapped,
            FooException.class,
            "generated exception");

        try {
            example.foo();
        } catch(FooException e) {
            //e.printStackTrace();
            assertEquals("foo-test", e.getMessage());
            return;
        }
        fail("excepted exception");
    }

    public void testPassingThroughOfDerivedExceptions() throws Exception
    {
        InvocationHandler invoker = new FooInvocationHandler(
            new FooDerivedException("foo-test", null));

        FooInterface unwrapped = (FooInterface) Proxy.newProxyInstance(
            invoker.getClass().getClassLoader(),
            new Class[] {FooInterface.class},
            invoker);

        try {
            unwrapped.foo();
        } catch(FooDerivedException e) {
            assertEquals("foo-test", e.getMessage());
        }

        FooInterface example = (FooInterface) ThrowableRewrapper.wrap(
            unwrapped,
            FooException.class,
            "generated exception");

        try {
            example.foo();
        } catch(FooDerivedException e) {
            //e.printStackTrace();
            assertEquals("foo-test", e.getMessage());
            return;
        }
        fail("excepted to catch derived exception");
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

