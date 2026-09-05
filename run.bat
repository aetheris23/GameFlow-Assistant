@echo off
REM GameFlow Assistant (Kotlin JVM build + run) for Windows.
REM Requires a JDK 17+ and kotlinc. Set KOTLINC to the kotlinc.exe path if not on PATH.
setlocal

if "%KOTLINC%"=="" set KOTLINC=kotlinc
set JARS=lib\sqlite-jdbc-3.45.1.0.jar;lib\slf4j-api-2.0.13.jar

set MODE=%1
if "%MODE%"=="" set MODE=gui

if not exist out\GameFlow (goto compile)
if "%MODE%"=="--demo" goto compile
if "%MODE%"=="--headless" goto compile
if "%MODE%"=="smoke" goto compile
goto run

:compile
rd /s /q out
mkdir out
echo Compiling...
%KOTLINC% -classpath "%JARS%" -d out src\main\kotlin\**\*.kt
if errorlevel 1 goto :eof
if exist src\test (
  mkdir out\test
  %KOTLINC% -classpath "out;%JARS%" -d out\test src\test\*.kt
)
xcopy /e /y /q src\main\resources\* out\ >nul 2>&1

:run
if "%MODE%"=="smoke" goto smoke
if "%MODE%"=="--headless" goto headless
if "%MODE%"=="--demo" goto demo
java -cp "out;%JARS%" GameFlow.App
goto :eof

:demo
java -cp "out;%JARS%" GameFlow.App --demo
goto :eof

:headless
java -Djava.awt.headless=true -cp "out;%JARS%" GameFlow.App --demo --headless
goto :eof

:smoke
java -cp "out\test;out;%JARS%" GameFlow.SmokeTest
goto :eof