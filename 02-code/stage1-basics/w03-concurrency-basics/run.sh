#!/usr/bin/env bash
# ============================================================
#  W03 · Java 并发基础 · 一键编译 + 运行
#  用法：./run.sh                  -> 跑 StartThread（day1）
#        ./run.sh RaceCondition    -> 短名，默认在 day1 包里找
#        ./run.sh day1.XxxDemo     -> 带包名，跨 day 跑
#        ./run.sh day2.Deadlock hold -> 第 2 个参数原样传给程序（Deadlock 的 hold 模式）
# ============================================================
set -e
cd "$(dirname "$0")"

mkdir -p out
find src/main/java -name "*.java" > sources.txt

echo "[1/2] javac 编译中..."
javac -encoding UTF-8 -d out @sources.txt

TARGET="${1:-StartThread}"
shift 2>/dev/null || true
case "$TARGET" in
  *.*) FULL="com.newt.java48.w03.$TARGET" ;;
  *)   FULL="com.newt.java48.w03.day1.$TARGET" ;;
esac

echo "[2/2] java $FULL $*"
java -Dfile.encoding=UTF-8 -cp out "$FULL" "$@"
