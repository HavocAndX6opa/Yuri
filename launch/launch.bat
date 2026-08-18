@echo off
title Yuri Client
cd /d "%~dp0"
set "NATIVES=%~dp0natives\windows"
set "GAMEDIR=%APPDATA%\.minecraft"
set "JAVA=%~dp0jre\bin\java.exe"
if not exist "%JAVA%" (
    echo [Yuri] jre\bin\java.exe not found. Make sure the jre\ folder is included.
    pause
    exit /b 1
)
if not exist "%GAMEDIR%" mkdir "%GAMEDIR%"
echo [Yuri] Using bundled JRE
echo [Yuri] Game dir: %GAMEDIR%
set "JAVA_LIB=%~dp0jre\bin"
if not exist "%JAVA_LIB%" set "JAVA_LIB=%~dp0jre\jre\bin"
"%JAVA%" -Dorg.lwjgl.librarypath="%NATIVES%" -Djava.library.path="%NATIVES%;%JAVA_LIB%" -jar "Yuri.jar" --version Yuri --accessToken 0 --assetsDir assets --assetIndex 1.8 --gameDir "%GAMEDIR%" --userProperties {}
pause
