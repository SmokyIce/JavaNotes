#!/usr/bin/env bash
# ============================================================
#  W06 · Spring AOP · 一键编译 + 运行
#  用法：./run.sh                     -> 跑 ProxyIdentity
#        ./run.sh JdkProxy            -> 短名，默认在 day1 包里找
#        ./run.sh day1.SelfInvocation
#
#  依赖：lib/ 里 8 个 jar
#    Spring 5.3.39 六个（core/beans/context/expression/aop/jcl）
#    + javax.annotation-api（JDK 11+ 没有 javax.annotation）
#    + aspectjweaver-1.9.7（Day 1 新增：@Aspect 注解与切点解析都靠它，
#      没有它 @Aspect 编译不过；只留 spring-aop 会 NoClassDefFoundError）
# ============================================================
set -e
cd "$(dirname "$0")"

mkdir -p out
find src/main/java -name "*.java" > sources.txt

echo "[1/2] javac 编译中（lib/* 已上 classpath）..."
javac -encoding UTF-8 -cp "lib/*" -d out @sources.txt

TARGET="${1:-ProxyIdentity}"
shift 2>/dev/null || true
case "$TARGET" in
  *.*) FULL="com.newt.java48.w06.$TARGET" ;;
  *)   FULL="com.newt.java48.w06.day1.$TARGET" ;;
esac

echo "[2/2] java $FULL $*"
java -Dfile.encoding=UTF-8 -cp "out;lib/*" "$FULL" "$@"
