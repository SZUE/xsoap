/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: ThrowableRewrapper.java,v 1.5 2003/04/06 00:04:25 aslom Exp $
 */
package soaprmi.util.dynamic_proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import soaprmi.util.logging.Log;
import soaprmi.util.logging.Logger;

/**
 * This is utility class to make easier to deal with situation
 * when API is not using classes that are thrown by implementation
 * hidden behind dynamic proxies - in such case suer will be getting
 * UndeclaredThrowableException.
 *
 * <p>For example if API implementation uses XSOAP
 * for remote objects and is not using  soaprmi.RemoteException then this utility
 * class is able to wrap RemoteExzception into target API chainable exception.
 *
 * @version $Revision: 1.5 $ $Date: 2003/04/06 00:04:25 $ (GMT)
 * @author Aleksander Slominski [http://www.extreme.indiana.edu/~aslom]
 */
public class ThrowableRewrapper {
    // NOTE: tuned for performance to minimize number of mem alloc when invoke() is executed

    /**
     * Returns object implementing the same set of interface as wrapped object
     * but with additional ability to catch all exception of wrapped object
     * and wrap them into one target exception (provided as parameter)
     * and it will take care of UndeclaredThrowableException if wrapped object is
     * dynamic proxy and it generates undeclared exception.
     *
     * <p>Additionally returned proxy will not do double wrapping i.e. in case
     * when thrown exception is the same type or subtyppe of target exception
     * then it will be passed through.
     *
     * @param    objectToWrap        an Object
     * @param    throwableForWrappinga  exception class that must have constructor
     *          taking
     * @param    messageForWrappedExceptiona  String
     *
     * @return   an Object
     */
    public static Object wrap(Object objectToWrap,
                              Class throwableForWrapping,
                              String messageForWrappedException)
    {

        InvocationHandler invokerToChain = new ThrowableRewrapperInvocationHandler(
            objectToWrap, messageForWrappedException, throwableForWrapping);

        Class klass = objectToWrap.getClass();
        Class[] objInterfaces = klass.getInterfaces();

        return Proxy.newProxyInstance(objectToWrap.getClass().getClassLoader(),
                                      objInterfaces,
                                      invokerToChain);

    }

    static class ThrowableRewrapperInvocationHandler implements InvocationHandler {
        private static Logger logger = Logger.getLogger();
        private static Logger heavyTracinbg = Logger.getLogger("trace.dynamic_proxy");
        private Object unwrappedObject;
        private String messageForWrappedException;
        private Class throwableForWrapping;
        private Constructor exceptionConstructorST;
        private Constructor exceptionConstructorTS;
        private Constructor exceptionConstructorT;

        public ThrowableRewrapperInvocationHandler(Object objectToWrap,
                                                   String messageForWrappedException,
                                                   Class throwableForWrapping)
        {
            this.unwrappedObject = objectToWrap;
            if(objectToWrap == null) throw new IllegalArgumentException(
                    "object to wrap can not be null");

            this.messageForWrappedException = messageForWrappedException;
            if(messageForWrappedException == null) throw new IllegalArgumentException(
                    "message for wrapped throwable must benot null");

            this.throwableForWrapping = throwableForWrapping;
            if(throwableForWrapping == null) throw new IllegalArgumentException(
                    "exception for throwing can notbe null");
            if(throwableForWrapping.isAssignableFrom(Throwable.class)) {
                throw new IllegalArgumentException(
                    "provided class "+throwableForWrapping+" must extend Throwable");
            }

            //this.chainedInvoker = invokerToChain;
            try{
                this.exceptionConstructorST = throwableForWrapping.getConstructor(
                    new Class[]{ String.class, Throwable.class });
            } catch (NoSuchMethodException e) {

                try{
                    this.exceptionConstructorTS = throwableForWrapping.getConstructor(
                        new Class[]{ Throwable.class, String.class });
                } catch (NoSuchMethodException e2) {

                    try{
                        this.exceptionConstructorT = throwableForWrapping.getConstructor(
                            new Class[]{ Throwable.class });
                    } catch (NoSuchMethodException e3) {
                        throw new IllegalArgumentException(
                            throwableForWrapping
                                +"must have public constructor with String and Throwable: "+e);
                    }
                }
            }
            // extra check if we *really* can call constructor (it isnotenough to find it!)
            try{
                Exception exampleEx = new Exception();
                //exceptionConstructorST.newInstance(new Object[]{"test message", exampleEx});
                createWrappingThrowable("test message", exampleEx);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "calling constructor for "+throwableForWrapping+" failed: "+e);
            }
        }

        public Object invoke(Object proxy, Method m, Object[] params)
            throws Throwable //RemoteException
        {
            if(Log.ON) logger.entering(params);
            // forward ot call to chained invoker if not caugh by now
            //return chainedInvoker.invoke(unwrappedObject, m, params);
            Throwable t;
            try {
                return m.invoke(unwrappedObject, params);
            } catch(Throwable tt) {
                t = tt;
            }

            // now unwrap all kinds of wrapped exceptions :-)
            while(true) {
                if(t instanceof InvocationTargetException) {
                    //t.printStackTrace();
                    InvocationTargetException ite = (InvocationTargetException) t;
                    t = ite.getTargetException();
                } else if(t instanceof UndeclaredThrowableException) {
                    //t.printStackTrace();
                    UndeclaredThrowableException ute = (UndeclaredThrowableException) t;
                    t = ute.getUndeclaredThrowable();
                } else {
                    break;
                }
            }

            // now finally we gat the most nested exception
            Class tClass = t.getClass();

            // if it is already target exception avoind unnecessary wraping
            if(throwableForWrapping.isAssignableFrom(tClass)) {
                // if actual exceotion is (sub)class of exception to throw then throw it directly!
                // nowwrapping applied :-)
                throw t;
            }

            // we need to wrap it as target exception
            String s;
            if(t.getMessage() != null) {
                s = messageForWrappedException + ": " + t.getMessage();
            } else {
                s = messageForWrappedException;
            }
            Throwable wrappedThrowable = createWrappingThrowable(s, t);
            throw wrappedThrowable;
        }

        private Throwable createWrappingThrowable(String s, Throwable t)
            throws InstantiationException, InvocationTargetException,
            IllegalAccessException, IllegalArgumentException
        {
            Throwable wrappedThrowable;
            if(exceptionConstructorST != null)  {
                wrappedThrowable = (Throwable) exceptionConstructorST.newInstance(new Object[]{s, t});
            } else if(exceptionConstructorTS != null)  {
                wrappedThrowable = (Throwable) exceptionConstructorTS.newInstance(new Object[]{t, s});
            } else if(exceptionConstructorT != null)  {
                wrappedThrowable = (Throwable) exceptionConstructorT.newInstance(new Object[]{t});
            } else {
                throw new IllegalStateException("no exception constructor available");
            }
            return wrappedThrowable;
        }
    }


    //static class  Excpetion
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

