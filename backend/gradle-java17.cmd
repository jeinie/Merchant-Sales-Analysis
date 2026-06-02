@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
if "%SPRING_PROFILES_ACTIVE%"=="" set "SPRING_PROFILES_ACTIVE=gcp"
gradle %*
exit /b %ERRORLEVEL%
