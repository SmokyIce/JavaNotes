#!/usr/bin/env bash
# W01 · Java 集合 · 一键编译 + 运行（Git Bash / WSL / macOS / Linux）
# 用法： ./run.sh                  -> 跑 ArrayListVsLinkedList
#        ./run.sh MyArrayList      -> 跑手写动态数组的扩容现场
set -euo pipefail

cd "$(dirname "$0")"

mkdir -p out
find src/main/java -name '*.java' > sources.txt

echo "[1/2] javac 编译中..."
javac -encoding UTF-8 -d out @sources.txt

TARGET="${1:-ArrayListVsLinkedList}"
echo "[2/2] java com.newt.java48.w01.day1.$TARGET"
echo
java -Dfile.encoding=UTF-8 -cp out "com.newt.java48.w01.day1.$TARGET"
