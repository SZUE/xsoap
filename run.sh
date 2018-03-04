#!/bin/sh

#
# You can set JAVA_HOME to point ot JDK 1.3 
# or shell will try to deterine java location using which
#

#JAVA_HOME=/l/jdk1.3

# 
# No need to modify anything after this line.
# --------------------------------------------------------------------


if [ -z "$JAVA_HOME" ] ; then
  JAVA=`/usr/bin/which java`
  if [ -z "$JAVA" ] ; then
    echo "Cannot find JAVA. Please set your PATH."
    exit 1
  fi
  JAVA_BIN=`dirname $JAVA`
  JAVA_HOME=$JAVA_BIN/..
else
  JAVA=$JAVA_HOME/bin/java
fi

#echo "JAVA= $JAVA"

#if [ ! "`$JAVA -version 2>&1 | grep "\ 1\.3"`" ]; then 
if [ ! "`$JAVA -version 2>&1 | egrep "\ 1\.[3456789].*"`" ]; then 
    echo Required 1.3 verion of JDK: can not use $JAVA
    echo Current version is:
    $JAVA -version
    exit 1
fi 

#POLICY=-Djava.security.policy=${TOP}/src/tests/java.policy
#JAVA_OPTS="-Djavax.net.debug=ssl"
#JAVA_OPTS=-Debug=true
#JAVA_OPTS="$JAVA_OPTS -Djava.compiler=NONE"
JAVA_OPTS="-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog -Dorg.apache.commons.logging.simplelog.defaultlog=error"

#echo set required LOCALCLASSPATH
LOCALCLASSPATH=`/bin/sh $PWD/classpath.sh run`

MY_JAVA="$JAVA $JAVA_OPTS $JAVA_DEBUG_OPTS -cp $LOCALCLASSPATH"


if [ -z "$1" ] ; then
   echo Please specify test name.
   exit 1
fi

NAME=$1
shift

if [ "$NAME" = "registry" ] ; then
  CMD="$MY_JAVA soaprmi.registry.RegistryImpl $*"
elif [ "$NAME" = "secure_registry" ] ; then
  CMD="$MY_JAVA soaprmi.registry.SecureRegistryImpl $*"
elif [ "$NAME" = "hello_server" ] ; then
  CMD="$MY_JAVA hello.HelloServer $*"
elif [ "$NAME" = "hello_client" ] ; then
  CMD="$MY_JAVA hello.HelloClient $*"
elif [ "$NAME" = "junit" ] ; then
  CMD="$MY_JAVA AllTests $*"
else
  CMD="$MY_JAVA $NAME $*"
fi

echo $CMD
$CMD
