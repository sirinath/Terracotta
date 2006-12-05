@echo off

rem rem
rem COPYRIGHT NOTICE
rem
rem All content copyright Terracotta, Inc. 2005-2006.  All rights reserved.  Any
rem copying, re-distribution, or sale is expressly prohibited.  Violators
rem will be prosecuted to the maximum extent permissible under applicable law.
rem


SETLOCAL
IF "x%TC_INSTALL_DIR%"=="x" SET TC_INSTALL_DIR=%~p0..\..
SET DIRNAME=%~p0
CD %DIRNAME%

CALL "%TC_INSTALL_DIR%\libexec\tc-functions.bat" tc_install_dir "%TC_INSTALL_DIR%" TRUE
CALL "%TC_INSTALL_DIR%\libexec\tc-functions.bat" tc_classpath "classes" FALSE
CALL "%TC_INSTALL_DIR%\libexec\tc-functions.bat" tc_java_opts ""
CALL "%TC_INSTALL_DIR%\libexec\tc-functions.bat" tc_config "tc-config.xml"
echo -
echo -
SET /P SCRN_NAME=ENTER A SCREEN NAME FOR THIS INSTANCE: 
CALL "%TC_INSTALL_DIR%\libexec\tc-functions.bat" run_dso_java -classpath "'%TC_CLASSPATH%'" "%D_TC_CONFIG%" "%TC_ALL_JAVA_OPTS%" demo.consolechat.Chatter %SCRN_NAME% "kalai*"
ENDLOCAL
