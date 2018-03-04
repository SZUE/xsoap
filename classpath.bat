@echo off
rem
rem This script set classpath
rem 	Usage: classpath [run|build] [set] [queit]
rem By default it sets CLASSPATH to execute direct java invocations (all included)
rem option build prepares LOCALCLASSPATH to use in build.bat
rem option build prepares LOCALCLASSPATH to use in run.bat
rem by using set CLASSPATH also be set by run|build
rem by using queit no echo of set CLASSOATH well be visible
rem

REM set CLASSPATH=classes.cpr;build\samples;build\classes;lib\junit37.jar;lib\servlet.jar
REM set CLASSPATH=%CLASSPATH%;lib\jsse.jar;lib\jcert.jar;lib\jnet.jar;
REM set COG=..\..\..\cog-0.9.12\lib
REM set CLASSPATH=%CLASSPATH%;%COG%\cog.jar;%COG%\iaik_jce_full.jar;%COG%\iaik_ssl.jar;%COG%\cryptix.jar;%COG%\jaas.jar

set LOCALCLASSPATH=
for %%i in (lib\junit\*.jar) do call lib\ant\lcp.bat %%i
for %%i in (lib\servlet_api\*.jar) do call lib\ant\lcp.bat %%i
for %%i in (lib\wsdl\*.jar) do call lib\ant\lcp.bat %%i
for %%i in (lib\jsse\*.jar) do call lib\ant\lcp.bat %%i
for %%i in (lib\cog\*.jar) do call lib\ant\lcp.bat %%i
rem add JDK1.4 JSSE/JCE jars if they are there
if exist %JAVA_HOME%\jre\lib\jsse.jar set LOCALCLASSPATH=%LOCALCLASSPATH%;%JAVA_HOME%\jre\lib\jsse.jar
if exist %JAVA_HOME%\jre\lib\jce.jar set LOCALCLASSPATH=%LOCALCLASSPATH%;%JAVA_HOME%\jre\lib\jce.jar

REM check options on how to set classpath

if "%1" == "build" goto build_classpath
if "%1" == "run" goto run_classpath
if "%1" == "clean" goto clean_classpath

REM otherwise set user classpath

set LOCALCLASSPATH=classes.cpr;build\classes;build\samples;build\tests;%LOCALCLASSPATH%
set CLASSPATH=%LOCALCLASSPATH%

if "%1" == "quiet" goto end

echo %CLASSPATH%


goto end

:clean_classpath
set CLASSPATH=
set LOCALCLASSPATH=

if "%2" == "quiet" goto end

echo set CLASSPATH=%CLASSPATH%
echo set LOCALCLASSPATH=%LOCALCLASSPATH%

goto end

:build_classpath
for %%i in (lib\ant\*.jar) do call lib\ant\lcp.bat %%i
for %%i in (lib\jakarta-regexp\*.jar) do call lib\ant\lcp.bat %%i
if exist %JAVA_HOME%\lib\tools.jar set LOCALCLASSPATH=%LOCALCLASSPATH%;%JAVA_HOME%\lib\tools.jar

goto extra_args

:run_classpath
set LOCALCLASSPATH=build\classes;build\samples;build\tests;%LOCALCLASSPATH%

:extra_args


if not "%2" == "set" goto check_echo

set CLASSPATH=%LOCALCLASSPATH%

:check_echo

if "%2" == "quiet" goto end
if "%3" == "quiet" goto end

echo %LOCALCLASSPATH%

goto end

:end
