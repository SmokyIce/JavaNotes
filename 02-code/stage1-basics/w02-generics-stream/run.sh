#!/usr/bin/env bash
# ============================================================
#  W02 · 泛型 / 异常 / Optional / Stream · 一键编译 + 运行
#  用法：./run.sh                  -> 跑 ErasureProof（day1）
#        ./run.sh PecsDemo         -> 短名，默认在 day1 包里找
#        ./run.sh day2.XxxDemo     -> 带包名，跨 day 跑
# ============================================================
set -e
cd "$(dirname "$0")"

mkdir -p out
find src/main/java -name "*.java" > sources.txt

echo "[1/2] javac 编译中..."
javac -encoding UTF-8 -d out @sources.txt

TARGET="${1:-ErasureProof}"
case "$TARGET" in
  *.*) FULL="com.newt.java48.w02.$TARGET" ;;
  *)   FULL="com.newt.java48.w02.day1.$TARGET" ;;
esac

echo "[2/2] java $FULL"
java -Dfile.encoding=UTF-8 -cp out "$FULL"
