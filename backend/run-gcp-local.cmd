@echo off
setlocal EnableExtensions

cd /d "%~dp0"

if exist ".env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
    if not "%%A"=="" set "%%A=%%B"
  )
)

if "%SERVER_PORT%"=="" set "SERVER_PORT=8081"
if "%SPRING_PROFILES_ACTIVE%"=="" set "SPRING_PROFILES_ACTIVE=gcp"
if "%DB_HOST%"=="" set "DB_HOST=127.0.0.1"
if "%DB_PORT%"=="" set "DB_PORT=3307"
if "%DB_NAME%"=="" set "DB_NAME=merchant_sales"
if "%DB_USERNAME%"=="" set "DB_USERNAME=merchant_app"
if "%DB_INIT_MODE%"=="" set "DB_INIT_MODE=never"
if "%CLOUD_SQL_PROXY_PATH%"=="" set "CLOUD_SQL_PROXY_PATH=cloud-sql-proxy.exe"

echo.
echo [run-gcp-local] Backend port: %SERVER_PORT%
echo [run-gcp-local] DB: %DB_HOST%:%DB_PORT%/%DB_NAME% as %DB_USERNAME%

if not "%CLOUD_SQL_INSTANCE%"=="" (
  powershell -NoProfile -Command "if ((Test-NetConnection 127.0.0.1 -Port %DB_PORT% -WarningAction SilentlyContinue).TcpTestSucceeded) { exit 0 } else { exit 1 }" >nul 2>nul
  if errorlevel 1 (
    echo [run-gcp-local] Starting Cloud SQL Auth Proxy...
    start "Cloud SQL Auth Proxy" /min "%CLOUD_SQL_PROXY_PATH%" "%CLOUD_SQL_INSTANCE%" --port %DB_PORT%
    timeout /t 3 /nobreak >nul
  ) else (
    echo [run-gcp-local] Cloud SQL Auth Proxy already appears to be listening on port %DB_PORT%.
  )
) else (
  echo [run-gcp-local] CLOUD_SQL_INSTANCE is empty. Assuming DB/proxy is already running.
)

powershell -NoProfile -Command "if ((Test-NetConnection 127.0.0.1 -Port %DB_PORT% -WarningAction SilentlyContinue).TcpTestSucceeded) { exit 0 } else { exit 1 }" >nul 2>nul
if errorlevel 1 (
  echo [run-gcp-local] ERROR: DB port %DB_PORT% is not reachable.
  echo [run-gcp-local] Set CLOUD_SQL_INSTANCE in backend\.env or start Cloud SQL Auth Proxy manually.
  exit /b 1
)

echo [run-gcp-local] Starting Spring Boot...
call "%~dp0gradle-java17.cmd" bootRun --args="--server.port=%SERVER_PORT%" --no-problems-report
exit /b %ERRORLEVEL%
