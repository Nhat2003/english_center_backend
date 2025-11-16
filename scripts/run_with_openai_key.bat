@echo off
:: run_with_openai_key.bat - set OPENAI_API_KEY for current session and run app
if "%1"=="" (
  echo Usage: run_with_openai_key.bat YOUR_OPENAI_KEY
  exit /b 1
)
set OPENAI_API_KEY=%1
echo Running backend with OPENAI_API_KEY set for this session.
cd /d "%~dp0.."
call mvnw.cmd spring-boot:run

