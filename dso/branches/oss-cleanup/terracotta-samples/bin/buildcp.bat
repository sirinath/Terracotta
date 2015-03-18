@echo off

SET QEB=%BIGMEMORY%\code-samples\bin

SET TMP_CP=.

dir /b "%BIGMEMORY%\code-samples\lib\*.jar" > temp.tmp
FOR /F %%I IN (temp.tmp) DO CALL "%QEB%\addpath.bat" "%BIGMEMORY%\code-samples\lib\%%I"

dir /b "%BIGMEMORY%\apis\ehcache\lib\*.jar" > temp.tmp
FOR /F %%I IN (temp.tmp) DO CALL "%QEB%\addpath.bat" "%BIGMEMORY%\apis\ehcache\lib\%%I"

dir /b "%BIGMEMORY%\apis\toolkit\lib\*.jar" > temp.tmp
FOR /F %%I IN (temp.tmp) DO CALL "%QEB%\addpath.bat" "%BIGMEMORY%\apis\toolkit\lib\%%I"

DEL temp.tmp
