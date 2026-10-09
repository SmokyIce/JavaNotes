#!/usr/bin/env bash
# W01 · Java 集合 · 一键编译 + 运行（Git Bash / WSL / macOS / Linux）
# 用法： ./run.sh                       -> 跑 ArrayListVsLinkedList（day1）
#        ./run.sh MyArrayList           -> 短名，默认在 day1 包里找
#        ./run.sh day2.HashMapProbe     -> 带包名，跨 day 跑
set -euo pipefail

cd "$(dirname "$0")"

mkdir -p out
find src/main/java -name '*.java' > sources.txt

echo "[1/2] javac 编译中..."
javac -encoding UTF-8 -d out @sources.txt

TARGET="${1:-ArrayListVsLinkedList}"

# 不含点号就当短名，默认加到 day1 包下；含点号则视为完整类名
case "$TARGET" in
  *.*) FULL="com.newt.java48.w01.$TARGET" ;;
  *)   FULL="com.newt.java48.w01.day1.$TARGET" ;;
esac

echo "[2/2] java $FULL"
echo
java -Dfile.encoding=UTF-8 --add-opens java.base/java.util=ALL-UNNAMED -cp out "$FULL"
