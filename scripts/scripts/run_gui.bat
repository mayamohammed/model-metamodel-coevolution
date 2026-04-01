@echo off
title Metamodel Coevolution GUI v1.0.0
echo ================================================
echo   Metamodel Coevolution Tool v1.0.0
echo ================================================

:: JavaFX path - adapter si necessaire
set JFX_LIB=C:\javafx-sdk-21\javafx-sdk-21.0.2\lib

:: Verifier JavaFX
if not exist "%JFX_LIB%\javafx.controls.jar" (
    echo [ERROR] JavaFX non trouve: %JFX_LIB%
    echo Telecharger depuis: https://gluonhq.com/products/javafx/
    pause
    exit /b 1
)

:: Projet root
set ROOT=%~dp0..

:: Classpath
set CP=%ROOT%\java\gui\target\coevolution-gui-1.0.0.jar
set CP=%CP%;%ROOT%\java\predictor\target\coevolution-predictor-1.0.0.jar
set CP=%CP%;%ROOT%\java\migrator\target\coevolution-migrator-1.0.0.jar
set CP=%CP%;%ROOT%\java\core\target\coevolution-core-1.0.0.jar

:: Demarrer Flask API
echo [1/2] Demarrage Flask API...
start "Flask API" cmd /k "cd /d %ROOT%\python\api && python app.py"
timeout /t 3 /nobreak >nul

:: Lancer GUI
echo [2/2] Lancement GUI JavaFX...
java ^
  --module-path "%JFX_LIB%" ^
  --add-modules javafx.controls,javafx.fxml ^
  -Dapi.url=http://localhost:5000 ^
  -cp "%CP%" ^
  com.coevolution.gui.MainApp

pause