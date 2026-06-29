@echo off
setlocal
cd /d "%~dp0backend"
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=aipaper_review
set DB_USERNAME=root
set DB_PASSWORD=root
if not exist "%~dp0tmp\runlogs" mkdir "%~dp0tmp\runlogs"
"C:\tmp\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run > "%~dp0tmp\runlogs\backend.out.log" 2> "%~dp0tmp\runlogs\backend.err.log"
