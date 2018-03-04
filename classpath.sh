#!/bin/sh
#
# This script sets required LOCALCLASSPATH and by default CLASSPATH 
# if no arguments.Otherwise use "set" option to set CLASSPATH
# and use "quiet" option to suppress prinitng of messages
# It must be run by source its content to modify current environment
#    . classpath.sh [build|run] [set] [quiet]
#

#export CLASSPATH=build/samples:build/classes:lib/junit/junit37.jar:lib/servlet_api/servlet.jar:
#lib/cog/cog-0_9_12.jar:lib/cog/iaik_ssl.jar:lib/jsse/jsse.jar:lib/cog/cryptix.jar:
#lib/jsse/jcert.jar:lib/cog/iaik_jce_full.jar:lib/jsse/jnet.jar 

LOCALCLASSPATH=.
LOCALCLASSPATH=`echo lib/junit/*.jar | tr ' ' ':'`:$LOCALCLASSPATH
LOCALCLASSPATH=`echo lib/servlet_api/*.jar | tr ' ' ':'`:$LOCALCLASSPATH
if [ ! "`echo lib/cog/*.jar`" = "lib/cog/*.jar" ] ; then
    LOCALCLASSPATH=`echo lib/cog/*.jar | tr ' ' ':'`:$LOCALCLASSPATH
fi
if [ ! "`echo lib/jakarta-regexp/*.jar`" = "lib/jakarta-regexp/*.jar" ] ; then
    LOCALCLASSPATH=`echo lib/jakarta-regexp/*.jar | tr ' ' ':'`:$LOCALCLASSPATH
fi
if [ ! "`echo lib/wsdl/*.jar`" = "lib/wsdl/*.jar" ] ; then
    LOCALCLASSPATH=`echo lib/wsdl/*.jar | tr ' ' ':'`:$LOCALCLASSPATH
fi
if [ ! "`echo lib/jsse/*.jar`" = "lib/jsse/*.jar" ] ; then
    LOCALCLASSPATH=`echo lib/jsse/*.jar | tr ' ' ':'`:$LOCALCLASSPATH
fi

if [ -f $JAVA_HOME/jre/lib/jsse.jar ] ; then
    LOCALCLASSPATH=$JAVA_HOME/jre/lib/jsse.jar:$LOCALCLASSPATH
    LOCALCLASSPATH=$JAVA_HOME/jre/lib/jce.jar:$LOCALCLASSPATH
fi

if [ "$1" = "build" ] ; then 
    LOCALCLASSPATH=`echo lib/ant/*.jar | tr ' ' ':'`:$LOCALCLASSPATH
    LOCALCLASSPATH=$JAVA_HOME/lib/tools.jar:$LOCALCLASSPATH
    if [ "$2" = "set" ] ; then
        CLASSPATH=$LOCALCLASSPATH
        if [ ! "$3" = "quiet" ] ; then
            echo $LOCALCLASSPATH
        fi
    elif [ ! "$2" = "quiet" ] ; then
        echo $LOCALCLASSPATH
    fi
else 
    LOCALCLASSPATH=build/samples:build/classes:build/tests:$LOCALCLASSPATH
    if [ "$1" = "run" ] ; then
        if [ "$2" = "set" ] ; then
            CLASSPATH=$LOCALCLASSPATH
            if [ ! "$3" = "quiet" ] ; then
                echo $LOCALCLASSPATH
            fi
        elif [ ! "$2" = "quiet" ] ; then
            echo $LOCALCLASSPATH
        fi
    else 
        CLASSPATH=$LOCALCLASSPATH
        if [ ! "$1" = "quiet" ] ; then
            echo $CLASSPATH
        fi
    fi
fi
export CLASSPATH

