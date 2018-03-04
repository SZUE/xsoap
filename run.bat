@echo off

REM $Id: run.bat,v 1.3 2003/03/03 07:24:28 aslom Exp $
REM
REM REQUIRED: Please make sure that JAVA_HOME points to JDK1.3 
REM   and make sure that JAVA_HOME\lib contains tools.jar !!!!
REM 

REM JAVA_HOME can be set manualy ..
REM set JAVA_HOME=c:\jdk13

if "%JAVA_HOME%" == "" goto javahomeerror



REM 
REM No need to modify anything after this line.
REM --------------------------------------------------------------------

echo JAVA_HOME=%JAVA_HOME%

set OLDCLASSPATH=%CLASSPATH%
call classpath.bat run quiet

REM set POLICY=-Djava.security.policy=D:\java\Janus\src\resman\janus\archive\java.policy
REM set JAVA_OPTS=-DDebug
REM set JAVA_OPTS=-Dlog.names=
REM set JAVA_OPTS="%JAVA_OPTS%"

set NAME=%1
shift

set OLDJAVA=%JAVA%
set JAVA=%JAVA_HOME%\bin\java %JAVA_OPTS% %JAVA_DEBUG_OPTS% -cp %LOCALCLASSPATH% 

if "%NAME%" == "registry" (
   set CMD=%JAVA% soaprmi.registry.RegistryImpl %JAVA_ARGS% %1 %2 %3 %4 %5 %6 %7 %8 %9
) else if "%NAME%" == "secure_registry" (
   set CMD=%JAVA% soaprmi.registry.SecureRegistryImpl %JAVA_ARGS% %1 %2 %3 %4 %5 %6 %7 %8 %9
) else if "%NAME%" == "hello_server" (
   set CMD=%JAVA% hello.HelloServer %JAVA_ARGS% %1 %2 %3 %4 %5 %6 %7 %8 %9
) else if "%NAME%" == "hello_client" (
   set CMD=%JAVA% hello.HelloClient %JAVA_ARGS% %1 %2 %3 %4 %5 %6 %7 %8 %9
) else if "%NAME%" == "junit" (
   set CMD=%JAVA% AllTests %JAVA_ARGS% %1 %2 %3 %4 %5 %6 %7 %8 %9
) else if "%NAME%" == "interop_client" (
   set CMD=%JAVA% interop.Client %JAVA_ARGS% %1 %2 %3 %4 %5 %6 %7 %8 %9
) else (
  set CMD=%JAVA% %NAME% %JAVA_ARGS% %1 %2 %3 %4 %5 %6 %7 %8 %9
)

rem clean up

set CLASSPATH=%OLDCLASSPATH%
set JAVA=%OLDJAVA%
set OLDCLASSPATH=
set OLDJAVA=

rem exute it

echo %CMD%
%CMD%

goto end

REM -----------ERROR-------------

:javahomeerror
echo "ERROR: JAVA_HOME not found in your environment."
echo "Please, set the JAVA_HOME variable in your environment to match the"
echo "location of the Java Virtual Machine you want to use."


:end





