#!/usr/bin/env bash
# ============================================================
#  W06 · Spring AOP · 一键编译 + 运行
#  用法：./run.sh                     -> 跑 ProxyIdentity
#        ./run.sh PointcutForms       -> 短名，先找 day1 再找 day2
#        ./run.sh day2.AspectOrder    -> 全名
#
#  依赖：lib/ 里 8 个 jar
#    Spring 5.3.39 六个（core/beans/context/expression/aop/jcl）
#    + javax.annotation-api（JDK 11+ 没有 javax.annotation）
#    + aspectjweaver-1.9.7（Day 1 新增：@Aspect 注解与切点解析都靠它，
#      没有它 @Aspect 编译不过；只留 spring-aop 会 NoClassDefFoundError）
#  注意：CGLIB 不需要额外的 jar —— Spring 5 已把 CGLIB 重打包进 spring-core，
#        包名是 org.springframework.cglib，不是 net.sf.cglib
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
  *)
    # 短名：按 class 文件实际存在的位置解析，免得每加一个 dayN 就改一次脚本
    FULL=""
    for d in day1 day2 day3 day4 day5; do
      if [ -f "out/com/newt/java48/w06/$d/$TARGET.class" ]; then
        FULL="com.newt.java48.w06.$d.$TARGET"
        break
      fi
    done
    if [ -z "$FULL" ]; then
      echo "找不到 $TARGET（day1..day5 都没有这个 class）" >&2
      exit 1
    fi
    ;;
esac

echo "[2/2] java $FULL $*"
java -Dfile.encoding=UTF-8 -cp "out;lib/*" "$FULL" "$@"
