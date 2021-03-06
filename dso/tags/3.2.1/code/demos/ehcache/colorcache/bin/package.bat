@echo off

if not defined JAVA_HOME (
  echo JAVA_HOME is not defined
  exit /b 1
)

setlocal

set JAVA_HOME="%JAVA_HOME:"=%"
set root=%~d0%~p0..
set root="%root:"=%"
set jetty1=%root%\jetty6.1\9081\webapps
set jetty2=%root%\jetty6.1\9082\webapps
cd %root%
set tc_install_dir=..\..\..

mkdir target\classes 2> NUL

for %%f in (..\..\..\ehcache\ehcache-core*.jar) do (
  set ehcache_core=%%f
)

if not exist %ehcache_core% (
  echo Couldn't find ehcache-core jar. Do you have a full kit?
  exit /b 1
)

set classpath=target\classes;%tc_install_dir%\lib\servlet-api-2.5-6.1.8.jar;%ehcache_core%

for %%f in (src\main\webapp\WEB-INF\lib\*.jar) do (
  set classpath=%classpath%;%%f
)

%JAVA_HOME%\bin\javac -d target\classes -sourcepath src\main\java -cp %classpath% src\main\java\org\terracotta\*.java

if not %errorlevel% == 0 ( 
  echo Failed to compile demo. Do you have a full kit with Ehcache core?
  exit /b 1
)

xcopy /e /y /q src\main\webapp target 1> NUL
mkdir target\WEB-INF\classes 2> NUL
xcopy /e /y /q target\classes target\WEB-INF\classes 1> NUL
mkdir target\WEB-INF\lib 2> NUL

rem packaging ehcache-terracotta
xcopy /y /q %tc_install_dir%\ehcache\ehcache-terracotta*.jar target\WEB-INF\lib 1> NUL
if not %errorlevel% == 0  (
  echo Couldn't package ehcache-terracotta. Do you have a complete kit?
  exit /b 1
)

rem packaging ehcache-core
xcopy /y /q %tc_install_dir%\ehcache\ehcache-core*.jar target\WEB-INF\lib 1> NUL
if not %errorlevel% == 0  (
  echo Couldn't package ehcache-core. Do you have a complete kit?
  exit /b 1
)

rem packaging slf4j-api
xcopy /y /q %tc_install_dir%\ehcache\slf4j-api*.jar target\WEB-INF\lib 1> NUL
if not %errorlevel% == 0  (
  echo Couldn't package slf4j-api. Do you have a complete kit?
  exit /b 1
)

rem packaging slf4j-jdk14
xcopy /y /q %tc_install_dir%\ehcache\slf4j-jdk14*.jar target\WEB-INF\lib 1> NUL
if not %errorlevel% == 0  (
  echo Couldn't package slf4j-jdk14. Do you have a complete kit?
  exit /b 1
)

rem create WAR
set warname=ColorCache.war
cd target
%JAVA_HOME%\bin\jar cf %warname% *
if %errorlevel% == 0 (
  echo %warname% has been created successfully. Deploying...
  xcopy /y /q %warname% %jetty1% 1> NUL
  xcopy /y /q %warname% %jetty2% 1> NUL
  echo Done.
) else (
  echo Error packaging %warname%
)

endlocal
