@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
gradle %*
exit /b %ERRORLEVEL%
