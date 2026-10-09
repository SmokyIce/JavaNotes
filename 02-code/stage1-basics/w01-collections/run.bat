@echo off
rem ============================================================
rem  W01 · Java 集合 · 一键编译 + 运行
rem  用法：run.bat                       -> 跑 ArrayListVsLinkedList（day1）
rem        run.bat MyArrayList           -> 短名，默认在 day1 包里找
rem        run.bat day2.HashMapProbe     -> 带包名，跨 day 跑
rem ============================================================
cd /d "%~dp0"

if not exist out mkdir out
dir /s /b src\main\java\*.java > sources.txt

echo [1/2] javac 编译中...
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
  echo 编译失败，看上面的报错。
  exit /b 1
)

set TARGET=%1
if "%TARGET%"=="" set TARGET=ArrayListVsLinkedList

rem 不含点号就当短名，默认加到 day1 包下；含点号则视为完整类名
echo %TARGET% | find "." >nul
if errorlevel 1 (
  set FULL=com.newt.java48.w01.day1.%TARGET%
) else (
  set FULL=com.newt.java48.w01.%TARGET%
)

echo [2/2] java %FULL%
echo.
java -Dfile.encoding=UTF-8 --add-opens java.base/java.util=ALL-UNNAMED -cp out %FULL%
echo.
pause
