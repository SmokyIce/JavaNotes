@echo off
rem ============================================================
rem  W04 · 线程池与 CompletableFuture · 一键编译 + 运行
rem  用法：run.bat                 -> 跑 PoolBasics（day1）
rem        run.bat PoolFlow        -> 短名，默认在 day1 包里找
rem        run.bat day1.PoolTraps  -> 带包名，跨 day 跑
rem        run.bat day1.PoolTraps oom        -> OOM 实测（无界队列，配 -Xmx64m）
rem        run.bat day1.PoolTraps oom-fixed  -> 有界队列对照
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
if "%TARGET%"=="" set TARGET=PoolBasics

rem 不含点号就当短名，默认加到 day1 包下；含点号则视为完整类名
echo %TARGET% | find "." >nul
if errorlevel 1 (
  set FULL=com.newt.java48.w04.day1.%TARGET%
) else (
  set FULL=com.newt.java48.w04.%TARGET%
)

echo [2/2] java %FULL% %2
echo.
if "%2"=="" (
  java -Dfile.encoding=UTF-8 -cp out %FULL%
) else (
  java -Dfile.encoding=UTF-8 -Xmx64m -cp out %FULL% %2
)
echo.
pause
