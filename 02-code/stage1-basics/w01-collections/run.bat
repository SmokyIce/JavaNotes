@echo off
rem ============================================================
rem  W01 · Java 集合 · 一键编译 + 运行
rem  用法：run.bat                    -> 跑 ArrayListVsLinkedList
rem        run.bat MyArrayList        -> 跑手写动态数组的扩容现场
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

echo [2/2] java com.newt.java48.w01.day1.%TARGET%
echo.
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w01.day1.%TARGET%
echo.
pause
