@echo off
set "NODE_HOME=%~dp0tools\node-portable\node-v24.15.0-win-x64"
set "PATH=%NODE_HOME%;%PATH%"
"%NODE_HOME%\node.exe" "%~dp0scripts\seedFurnitureCatalog.js"
