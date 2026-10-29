---
title: 今天做什么
date: 2026-10-29
weekday: 周四
milestone: M1 · Java 地基
task: W02 Day 2 — 异常体系；顺带把 Day 1 的三个遗留补掉
status: 已完成
---

# 今天 · 2026-10-29（周四）· 已完成

> 开工前的「遗留清扫」比昨天多了一项：不是笔记漏勾，而是**代码崩了**。
> Day 1 的 `ErasureProof` 在 B2 段抛异常，后面四段实验根本没跑过，快照里还留着那个异常栈。

## 一、先补的遗留

| 遗留 | 状态 | 处理 |
| :--- | :--- | :--- |
| `ErasureProof` B 段末尾抛 `NoSuchMethodException`，C~F 四段没跑 | ✅ 已修 | `Box.set` 是包私有，`getMethod()` 只找 public；改 `Box.class.getDeclaredMethod(...)`，并补打「StringBox 自己声明的方法数 = 0」。快照重跑，六段全通 |
| Day 1 遗留：PECS 的「编译不过」只有注释，没有报错原文 | ✅ 已补 | 新建 `compile-errors/GenericLimits2.java`，一次编出 **6 个错**（不变性 / extends 写 / super 读 / List<?> 写 / 通配符重载 / `new ?`） |
| Day 1 遗留：`capture of ?` 只见过报错文案，没有正例 | ✅ 已补 | 新建 `day1/WildcardCapture.java` 四段：报错原文 → helper 捕获正例 → JDK 17 的 raw 选择 → raw 的代价 |

## 二、今天的正题：异常体系

代码在 `02-code/stage1-basics/w02-generics-stream/src/main/java/com/newt/java48/w02/day2/`：

| 文件 | 内容 |
| :--- | :--- |
| `TryWithResourcesProbe.java` | 5 段：逆序关闭 / close 抛异常进 suppressed / `finally` 里 `return` / `finally` 里 `throw` / 受检 close 的提升 |
| `ExceptionChainProbe.java` | 6 段：三种包装对比 / 三种日志写法 / cause vs suppressed / 受检边界 / sneakyThrow / 何时包装 |
| `day2-result.txt` | 两段实测快照（JDK 17.0.12） |

## 三、今天挖到的四个点

1. **「`RuntimeException(e)` 丢栈」只对一半，栈是断成两截，不是丢了。**
   实测：包装异常栈深度 3、栈顶在包装那一行；cause 栈深度 5、栈顶在真正出事的 `deepest()`。
   真丢栈的是 `new RuntimeException(e.getMessage())` —— `getCause()` 直接 `null`。
   同一个异常三种日志写的体积：`getMessage()` 11 字 1 行 / `+e` 39 字 1 行 / `printStackTrace` **634 字 9 行**。

2. **`finally` 里的 `return` 才是真的把异常变没。**
   `swallow()` 里 throw 了 `IllegalStateException`，方法返回 `42`，没有异常、没有日志、没有 suppressed，
   javac 一句话都不说。`finally` 里 `throw` 则是另一个病：主异常被覆盖，`getSuppressed().length = 0`。
   这两个病合起来，就是 TWR 存在的理由 —— 它的价值不是「少写 close」，是保证主异常活下来。

3. **`cause` 与 `suppressed` 是两种语义，不能互换。**
   cause 是「因为什么才失败」（一对一，`initCause` 只能补一次）；
   suppressed 是「失败路上还顺手炸了别的」（一对多）。TWR 关资源走 suppressed，
   因为 close 的异常不是失败原因，是附带损害。顺带实测：`addSuppressed(自己)` 抛
   `IllegalArgumentException: Self-suppression not permitted`。

4. **Day 1 的擦除在异常体系里有个意外用途：sneaky throw。**
   `static <T extends Throwable> void sneakyThrow(Throwable t) throws T` —— 调用处指定 `T = RuntimeException`，
   编译器就只按 RuntimeException 检查，传进去一个 `IOException` 它看不见。实测 `void looksInnocent()`
   签名上一个 `throws` 都没有，运行期真抛 `java.io.IOException`，而且 `getCause() = null`（原样抛，不是包装）。
   Lombok `@SneakyThrows` 就是这个原理。

## 四、环境备忘

- 编译：`find src/main/java -name "*.java" > sources.txt && javac -encoding UTF-8 -d out @sources.txt`
- 跑 day2：`java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w02.day2.TryWithResourcesProbe`
  （或 `run.bat day2.TryWithResourcesProbe`，带点号的走完整类名）
- 抓捕获的报错原文要加 `-Xdiags:verbose`，否则只有一句 `argument mismatch`，看不到 CAP#1 / CAP#2
- `compile-errors/` 下的注释一律写英文：javac 会回显出错的源码行，中文在那里会变 GBK 乱码

---

# 下一个时间盒（60 分钟）

**W02 Day 3：Optional**，重点不是 API 清单，而是「三个反模式 + 一条边界」：

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述今天：`finally` 里 return/throw 各自怎么吞异常 / 包装是切栈不是丢栈 | 口头过，卡住回看笔记 Day 2 |
| 25 分钟 | 三个反模式：`isPresent()+get()` 当 null 检查、`orElse(compute())` 白算一次、把 Optional 当字段/参数 | `day3/OptionalAntiPatterns.java` |
| 15 分钟 | 正例：`map/flatMap/filter/orElseGet/orElseThrow` 串成一条链；`flatMap` 与 `map` 的差别现场证 | `day3/OptionalChain.java` |
| 10 分钟 | 输出：一句话讲清「Optional 是给返回值用的，不是给字段用的」 | 笔记「核心问题」 |

> 优先做第二个 25 分钟。`orElse(compute())` 那种「结果对但白算一次」的 bug，只有实测打印才知道
> 它真的每次都算 —— 而且算的那一次可能是个远程调用。
