@echo off
setlocal
cd /d "%~dp0frontend"
set VITE_API_BASE_URL=http://localhost:8080/api
if not exist "%~dp0tmp\runlogs" mkdir "%~dp0tmp\runlogs"
D:\nodejs\npm.cmd run dev > "%~dp0tmp\runlogs\frontend.out.log" 2> "%~dp0tmp\runlogs\frontend.err.log"
