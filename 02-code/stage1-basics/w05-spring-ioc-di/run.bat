@echo off
chcp 65001 >nul
rem ============================================================
rem  W05 . Spring IOC / DI . compile + run
rem  usage: run.bat                  -> SpringWiring
rem         run.bat ManualWiring     -> day1.ManualWiring
rem ============================================================
setlocal
cd /d "%~dp0"

if exist out rmdir /s /q out
mkdir out

dir /s /b src\main\java\*.java > sources.txt

echo [1/2] javac ...
javac -encoding UTF-8 -cp "lib/*" -d out @sources.txt
if errorlevel 1 exit /b 1

set TARGET=%1
if "%TARGET%"=="" set TARGET=SpringWiring
echo %TARGET% | findstr /c:"." >nul
if errorlevel 1 (set FULL=com.newt.java48.w05.day1.%TARGET%) else (set FULL=com.newt.java48.w05.%TARGET%)

shift
echo [2/2] java %FULL%
java -Dfile.encoding=UTF-8 -cp "out;lib/*" %FULL% %1 %2 %3 %4
endlocal
