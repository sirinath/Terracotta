@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

rem --------------------------------------------------------------------
rem - stop.bat 908{1,2}
rem --------------------------------------------------------------------

setlocal
cd %~d0%~p0..
set SANDBOX=%CD%
set SANDBOX="%SANDBOX:"=%"

set TC_INSTALL_DIR=%SANDBOX%\..\..\..

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

set JAVA_HOME="%JAVA_HOME:"=%"

if not defined CATALINA_HOME (
   echo CATALINA_HOME must be set to a Tomcat6.0 installation.
   exit /b 1
)
set CATALINA_HOME="%CATALINA_HOME:"=%"

if not exist %CATALINA_HOME% (
   echo CATALINA_HOME %CATALINA_HOME% does not exist.
   exit /b 1
)
set CATALINA_BASE=%SANDBOX%\tomcat6.0\%1

set CLASSPATH=%CLASSPATH:"=%
set CATALINA_HOME=%CATALINA_HOME:"=%
set CATALINA_BASE=%CATALINA_BASE:"=%
set JAVA_HOME=%JAVA_HOME:"=%
call "%CATALINA_HOME%\bin\shutdown.bat"
exit /b %ERRORLEVEL%.
endlocal
