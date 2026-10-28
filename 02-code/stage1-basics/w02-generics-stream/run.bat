@echo off
rem ============================================================
rem  W02 · 泛型 / 异常 / Optional / Stream · 一键编译 + 运行
rem  用法：run.bat                 -> 跑 ErasureProof（day1）
rem        run.bat PecsDemo        -> 短名，默认在 day1 包里找
rem        run.bat day2.XxxDemo    -> 带包名，跨 day 跑
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
if "%TARGET%"=="" set TARGET=ErasureProof

rem 不含点号就当短名，默认加到 day1 包下；含点号则视为完整类名
echo %TARGET% | find "." >nul
if errorlevel 1 (
  set FULL=com.newt.java48.w02.day1.%TARGET%
) else (
  set FULL=com.newt.java48.w02.%TARGET%
)

echo [2/2] java %FULL%
echo.
java -Dfile.encoding=UTF-8 -cp out %FULL%
echo.
pause
