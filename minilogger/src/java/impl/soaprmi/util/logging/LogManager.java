/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: LogManager.java,v 1.11 2003/04/06 00:04:04 aslom Exp $
 */

package soaprmi.util.logging;

import java.lang.ref.WeakReference;
import java.util.*;
//import java.util.Map.Entry;

// LoggerTest: check set level functionality
// "",com,com.wombat,com.womab.nose,com.wombat.head

/**
 * Class that manages loggers
 *
 * @version $Revision: 1.11 $ $Date: 2003/04/06 00:04:04 $ (GMT)
 * @author Aleksander Slominski [http://www.extreme.indiana.edu/~aslom]
 */

public class LogManager {
    private static final boolean DEBUG = false;
    private static LogManager instance = new LogManager();
    private SortedMap loggers = new TreeMap(); // loggers: name -> Logger (name, level)


    LogManager() {
    }

    public static LogManager getLogManager() {
        return instance;
    }

    public boolean addLogger(Logger l) {
        String name = l.getName();
        synchronized(loggers) {
            loggers.put(name, new WeakReference(l));
        }
        Level level = l.getLevel();
        if(DEBUG) System.err.println("LogManager.addLogger() name='"+name+"' level="+level);
        //System.err.println("LOGGER addLogger l="+l);
        return true; //TODO why?
    }

    public Level getLevel(java.lang.String name) {
        //obtain from levels guessing by taking off dots
        //Object o;
        Level level = Level.OFF;  // this is default level if no loggers available ...
        String origName = name;
        while(name != null){
            if(DEBUG) System.err.println("LogManager.getLevel() trying name='"+name+"'");
            WeakReference wr;
            synchronized(loggers) {
                wr =  (WeakReference) loggers.get(name);
            }
            if(wr != null) {
                if(DEBUG) System.err.println("LogManager.getLevel() found wr='"+wr+"'");
                Logger logger;
                if((logger = (Logger) wr.get()) != null) {
                    level = logger.getLevel();
                    if(DEBUG) System.err.println(
                            "LogManager.getLevel()="+level+" for name="+origName
                                +" stripped name="+name);
                    break; //return level;
                }
            }
            int pos;
            if((pos = name.lastIndexOf('.')) > 0) {
                name=name.substring(0, pos);
            } else if(name.length() > 0) {
                name = "";
            } else {
                name = null;
            }
        }
        if(DEBUG && name == null) System.err.println(
                "LogManager.getLevel()="+level+" DEFAULT for name='"+origName+"'");
        return level;
    }

    public Logger getLogger(java.lang.String name) {
        return getLogger(name, null);
    }

    Logger getLogger(java.lang.String name, Level newLevel) {
        WeakReference wr;
        synchronized(loggers) {
            wr = (WeakReference) loggers.get(name);
        }
        Logger logger = null;
        if(wr != null) {
            logger = (Logger) wr.get();
        }
        if(logger == null) {
            logger = new Logger(name, null);
            if(newLevel == null) {
                Level level = getLevel(name);
                logger.setLevel(level);
            }
            addLogger(logger);
        }
        if(newLevel != null) {
            logger.setLevel(newLevel);
        }
        return logger;
    }

    public Enumeration getLoggerNames() {
        Enumeration enum;
        synchronized(loggers) {
            enum = Collections.enumeration(loggers.keySet());
        }
        return enum;
    }


    /**
     * Set a log level for a given set of loggers.
     * Subsequently the target loggers will only log messages whose
     * types are greater than or equal to the given level.
     * The level value Level.OFF can be used to turn off logging.
     * The given log level applies to the named Logger (if it exists), and
     * on any other named Loggers below that name in the naming hierarchy.
     * The name and level are recorded, and will be applied to any
     * new Loggers that are later created matching the given name.
     */
    public void setLevel(String name, Level level) {
        // it will create a logger if not existing as well
        Logger l = getLogger(name);
        //l.setLevel(level);
        synchronized(loggers) {

            SortedMap tail = loggers.tailMap(name);
            // traverse map & set levels until you hit next category...
            Set entries = tail.entrySet();

            for(Iterator i = entries.iterator(); i.hasNext(); ) {

                //CodeWarrior compiler hates it and crashes with strange errors!!!
                //Map.Entry me = (Map.Entry) i;
                Map.Entry me = (Map.Entry) i.next();

                String keyName = (String) me.getKey();
                if(!keyName.startsWith(name))
                    break;
                WeakReference wr = (WeakReference) me.getValue();
                if((l = (Logger) wr.get()) != null) {
                    l.setLevel(level);
                }

            }
        }
    }

    // print all loggers -- good for debugging
    public String toString() {
        StringBuffer buf = new StringBuffer(getClass().getName()+"={ ");
        SortedMap tail = loggers.tailMap("");
        // traverse map & set levels until you hit next category...
        Set entries = tail.entrySet();
        Logger l = null;
        for(Iterator i = entries.iterator(); i.hasNext(); ) {

            Map.Entry me = (Map.Entry) i.next();

            String keyName = (String) me.getKey();
            WeakReference wr = (WeakReference) me.getValue();
            Level level = null;
            if((l = (Logger) wr.get()) != null) {
                level = l.getLevel();
                buf.append(keyName+":"+level);
                if(i.hasNext()) buf.append(",");
            }
        }
        buf.append(" }");
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




