@echo off
title Metamodel Coevolution Tool v1.0.0
echo ============================================
echo   Metamodel Coevolution Tool v1.0.0
echo ============================================

:: Start Flask API
echo [1/2] Starting Flask API...
start "Flask API" cmd /k "cd /d %~dp0..\python\api && python app.py"
timeout /t 3 /nobreak >nul

:: Start GUI
echo [2/2] Starting GUI...
java -Dapi.url=http://localhost:5000 -jar "%~dp0..\release\coevolution-gui.jar"

echo [OK] Done.
pause