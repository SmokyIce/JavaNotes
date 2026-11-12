#!/usr/bin/env bash
# ============================================================
#  W04 · 线程池与 CompletableFuture · 一键编译 + 运行
#  用法：./run.sh                  -> 跑 PoolBasics（day1）
#        ./run.sh PoolFlow         -> 短名，默认在 day1 包里找
#        ./run.sh day1.PoolTraps   -> 带包名，跨 day 跑
#        ./run.sh day1.PoolTraps oom       -> OOM 实测（无界队列，建议配 -Xmx64m）
#        ./run.sh day1.PoolTraps oom-fixed -> 有界队列对照
# ============================================================
set -e
cd "$(dirname "$0")"

mkdir -p out
find src/main/java -name "*.java" > sources.txt

echo "[1/2] javac 编译中..."
javac -encoding UTF-8 -d out @sources.txt

TARGET="${1:-PoolBasics}"
shift 2>/dev/null || true
case "$TARGET" in
  *.*) FULL="com.newt.java48.w04.$TARGET" ;;
  *)   FULL="com.newt.java48.w04.day1.$TARGET" ;;
esac

echo "[2/2] java $FULL $*"
java -Dfile.encoding=UTF-8 -cp out "$FULL" "$@"
