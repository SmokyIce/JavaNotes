@echo off
rem ============================================================
rem  W03 · Java 并发基础 · 一键编译 + 运行
rem  用法：run.bat                 -> 跑 StartThread（day1）
rem        run.bat RaceCondition   -> 短名，默认在 day1 包里找
rem        run.bat day1.XxxDemo    -> 带包名，跨 day 跑
rem        run.bat day2.Deadlock hold  -> 第 2 个参数原样传给程序（Deadlock 的 hold 模式）
rem  环境：JDK 17（JAVA_HOME 指向 jdk-17 时直接可用）
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
if "%TARGET%"=="" set TARGET=StartThread

rem 不含点号就当短名，默认加到 day1 包下；含点号则视为完整类名
echo %TARGET% | find "." >nul
if errorlevel 1 (
  set FULL=com.newt.java48.w03.day1.%TARGET%
) else (
  set FULL=com.newt.java48.w03.%TARGET%
)

echo [2/2] java %FULL% %2
echo.
rem %2 起是传给程序自己的参数（例如 Deadlock 的 hold）
if "%2"=="" (
  java -Dfile.encoding=UTF-8 -cp out %FULL%
) else (
  java -Dfile.encoding=UTF-8 -cp out %FULL% %2
)
echo.
pause
