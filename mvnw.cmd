@echo off
setlocal

set "MAVEN_VERSION=3.9.9"
set "BASE_DIR=%~dp0"
set "TOOLS_DIR=%BASE_DIR%.tools"
set "MAVEN_DIR=%TOOLS_DIR%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_DIR%\bin\mvn.cmd"
set "MAVEN_ZIP=%TOOLS_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"

if exist "%MAVEN_BIN%" goto run_maven

echo Maven not found locally. Downloading Apache Maven %MAVEN_VERSION%...
if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ProgressPreference='SilentlyContinue';" ^
  "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%'"
if errorlevel 1 (
  echo Failed to download Maven from %MAVEN_URL%
  exit /b 1
)

echo Extracting Maven...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ProgressPreference='SilentlyContinue';" ^
  "if (Test-Path '%MAVEN_DIR%') { Remove-Item -Path '%MAVEN_DIR%' -Recurse -Force };" ^
  "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%TOOLS_DIR%' -Force"
if errorlevel 1 (
  echo Failed to extract Maven archive
  exit /b 1
)

:run_maven
call "%MAVEN_BIN%" %*
set "MVN_EXIT=%ERRORLEVEL%"
exit /b %MVN_EXIT%
