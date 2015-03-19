@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

set SAG_INSTALL_DIR=%~d0%~p0..\..\..
set SAG_INSTALL_DIR="%SAG_INSTALL_DIR:"=%"

if exist %SAG_INSTALL_DIR%\install\bin\setenv.bat (
  call %SAG_INSTALL_DIR%\install\bin\setenv.bat
)

