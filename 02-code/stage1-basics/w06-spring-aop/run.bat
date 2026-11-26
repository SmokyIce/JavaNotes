@echo off
chcp 65001 > nul
cd /d "%~dp0"

if not exist out mkdir out
dir /s /b src\main\java\*.java > sources.txt

echo [1/2] javac ...
javac -encoding UTF-8 -cp "lib/*" -d out @sources.txt
if errorlevel 1 goto :eof

set TARGET=%1
if "%TARGET%"=="" set TARGET=ProxyIdentity
echo %TARGET% | findstr /c:"." > nul
if errorlevel 1 (
  set FULL=com.newt.java48.w06.day1.%TARGET%
) else (
  set FULL=com.newt.java48.w06.%TARGET%
)

echo [2/2] java %FULL%
java -Dfile.encoding=UTF-8 -cp "out;lib/*" %FULL%
