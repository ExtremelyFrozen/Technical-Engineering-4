@echo off
cd /d "%~dp0"
call gradlew.bat runData
call gradlew.bat build
pause
