#!/usr/bin/env bash
# ============================================================
#  W05 · Spring IOC / DI · 一键编译 + 运行
#  用法：./run.sh                     -> 跑 SpringWiring
#        ./run.sh ManualWiring        -> 短名，默认在 day1 包里找
#        ./run.sh day1.ContainerQuestions
#
#  注意：本机没有 Maven，依赖是手工放在 lib/ 的 7 个 jar：
#    spring-core / spring-beans / spring-context / spring-expression / spring-aop / spring-jcl
#    + javax.annotation-api（Day 2 加：JDK 11+ 已把 @PostConstruct 移出 JDK）
#  版本 5.3.39（最后一个支持 JDK 8 的 5.x 线；JDK 17 上跑没问题）
#  换 JDK 版本不用动这里；换 Spring 版本要同时换 lib/ 里的 6 个 spring-* jar。
# ============================================================
set -e
cd "$(dirname "$0")"

mkdir -p out
find src/main/java -name "*.java" > sources.txt

echo "[1/2] javac 编译中（lib/* 已上 classpath）..."
javac -encoding UTF-8 -cp "lib/*" -d out @sources.txt

TARGET="${1:-SpringWiring}"
shift 2>/dev/null || true
case "$TARGET" in
  *.*) FULL="com.newt.java48.w05.$TARGET" ;;
  *)   FULL="com.newt.java48.w05.day1.$TARGET" ;;
esac

echo "[2/2] java $FULL $*"
java -Dfile.encoding=UTF-8 -cp "out;lib/*" "$FULL" "$@"
