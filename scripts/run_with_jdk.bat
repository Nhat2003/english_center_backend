@echo off
REM Usage: run_with_jdk.bat "C:\Path\to\jdk21"
SETLOCAL
if "%~1"=="" (
  echo Please pass the JDK home path as the first argument, for example:
  echo   run_with_jdk.bat "C:\Program Files\Java\jdk-21"
  exit /b 1
)
set "JDK_HOME=%~1"
echo Using JDK at: %JDK_HOME%
set "JAVA_HOME=%JDK_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo java version:
java -version
echo javac version:
javac -version
echo Running Maven wrapper...
if exist mvnw.cmd (
  .\mvnw.cmd %*
) else (
  mvn %*
)
ENDLOCAL

