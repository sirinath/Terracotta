@echo off

setlocal enabledelayedexpansion

set classpath=

FOR %%F IN (lib/*.jar) DO (
  SET classpath=!classpath!;lib/%%F%
)

java -cp "%classpath%" com.tc.license.generator.gui.MainFrame

endlocal
