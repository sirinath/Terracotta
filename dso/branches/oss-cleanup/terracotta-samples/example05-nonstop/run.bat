@echo off
set TC_INSTALL_DIR=%~d0%~p0..\..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

if exist %TC_INSTALL_DIR%\server\bin\setenv.bat (
  call %TC_INSTALL_DIR%\server\bin\setenv.bat
)
if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

set JAVA_HOME="%JAVA_HOME:"=%"

rem Set BigMemory to the base directory of the BigMemory Distribution
@SET WD=%~d0%~p0
@SET BIGMEMORY=%WD%..\..

@rem setup the class path...
CALL "%WD%"..\bin\buildcp.bat
SET BIGMEMORY_CP=%TMP_CP%


%JAVA_HOME%\bin\java.exe -cp %BIGMEMORY_CP% -Xmx200M  -Dcom.tc.productkey.path="%BIGMEMORY%"/terracotta-license.key  com.bigmemory.samples.nonstop.BigMemoryNonStopRejoin
