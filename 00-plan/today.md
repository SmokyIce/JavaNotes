---
title: 今天做什么
date: 2026-10-30
weekday: 周五
milestone: M1 · Java 地基
task: W02 Day 3 — Optional；顺带把 Day 2 遗留的「异常开销」量掉
status: 已完成
---

# 今天 · 2026-10-30（周五）· 已完成

> 开工先做了一件不在计划里但必须先做的事：**回归编译 + 跑一遍 Day 1 / Day 2**。
> 前两天的代码都通过（`javac 17.0.12`，`TryWithResourcesProbe` 各段输出正常），
> 所以今天的遗留清单只剩一条：Day 2 §9 那个「异常开销没量」。

## 一、先补的遗留

| 遗留 | 状态 | 处理 |
| :--- | :--- | :--- |
| Day 2 §9：异常性能只说了「`fillInStackTrace` 贵」，没量 | ✅ 已补 | 新建 `day2/ExceptionCost.java`，同一个类跑两遍（默认 / `-XX:-StackTraceInThrowable`），5 轮 × 10 万次取最小值 |
| Day 2 §9：`Error` / `catch(Throwable)` 没展开 | ⏭ 保留 | 等 W13 JVM 内存一起做，不提前补 |
| Day 1 / Day 2 代码可运行性 | ✅ 已验证 | 全量重编 + 抽跑 `TryWithResourcesProbe`，无回归 |

## 二、今天的正题：Optional

代码在 `02-code/stage1-basics/w02-generics-stream/src/main/java/com/newt/java48/w02/day3/`：

| 文件 | 内容 |
| :--- | :--- |
| `OptionalAntiPatterns.java` | 五段：`isPresent()+get()` / `orElse` 白算一次 / 当字段·参数·集合元素 / 五个静默陷阱 / 判定表 |
| `OptionalChain.java` | 六段：null 检查对比 / `map` vs `flatMap` / `filter` / `orElseGet` vs `orElseThrow` / 消费侧 / 优惠券算价 |
| `day3-result.txt` | 两段实测快照（JDK 17.0.12） |

## 三、今天挖到的四个点

1. **Optional 让报错信息「退化」，不是进化。**
   实测 `empty.get()` → `NoSuchElementException: No value present`，没有变量名、没有业务上下文、
   没说是哪一层；而同一场景的 NPE 有 helpful message：`Cannot invoke "String.length()" because "<parameter1>" is null`。
   所以正确姿势不是「包起来再拆」，而是把缺失沿链传递、链尾处理一次。
   判断标准很土但有效：**`orElse` 出现多次，就说明你在用它做 null 检查。**

2. **`orElse(compute())` 的 bug 是「结果正确 + 偶尔慢」。**
   模拟一次 20 ms 远程查询：有值时 `orElse` 仍调 **1 次 / 36.2 ms**，`orElseGet` **0 次 / 0.3 ms**。
   空值时两者一样，所以问题只在「有值」那条路径上 —— 命中缓存却多打一次 DB，业务无感。
   但也不必一刀切：`orElse(new ArrayList<>())` 这种廉价对象被逃逸分析消掉，基本无害。

3. **Optional 当字段会让整个对象不可序列化。**
   只加一行 `Optional<String> port`，`ObjectOutputStream.writeObject` 就抛
   `NotSerializableException: java.util.Optional`（它没实现 `Serializable`）。改成可空字段 + 出口包一层，133 字节正常。
   这条把「Optional 是给返回值用的」从风格问题变成了**工程约束**。

4. **`Optional` 只管「有没有」，不管「合不合法」。**
   券 500 减订单 200：少了 `filter(c -> c.off() <= amount)` 就**算出 −300**。
   `Optional` 的四个环节里，`filter` 是唯一负责「值合法性」的，最容易被漏。

顺带一个反直觉对照：`map(s -> null)` 静默变 `empty`，`flatMap(s -> null)` 直接 NPE。
两者「都合理」—— `map` 的契约是函数可能返回 null，`flatMap` 的契约是你必须还一个 Optional。

## 四、环境备忘

- 编译前记得刷新清单：`find src/main/java -name "*.java" > sources.txt`
  —— 今天新建 `ExceptionCost` 后忘了刷新，`java` 直接报 `ClassNotFoundException`
- 跑 day3：`run.bat day3.OptionalAntiPatterns` / `run.bat day3.OptionalChain`
- 异常开销要跑两遍：
  `java -Dfile.encoding=UTF-8 -XX:-StackTraceInThrowable -cp out com.newt.java48.w02.day2.ExceptionCost`
- **微基准必须多轮取最小值**：第一版每用例只测一轮，同一段代码读出 44 ns 和 295 ns

---

# 下一个时间盒（60 分钟）

**W02 Day 4：Lambda**，重点不是语法，而是「编译器替你生成了什么」：

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述今天：`orElse` 为什么白算 / Optional 为什么不能做字段 | 口头过，卡住回看笔记 Day 3 |
| 20 分钟 | 函数式接口四种形态：`Function` / `Consumer` / `Supplier` / `Predicate`，各配一个「业务里长什么样」的例子 | `day4/FunctionalShape.java` |
| 20 分钟 | 方法引用四种写法（静态 / 实例 / `对象::实例` / `构造`）+ 变量捕获与 effectively final：为什么 lambda 里改不了外面的局部变量 | `day4/LambdaCapture.java` |
| 10 分钟 | 输出：`lambda` 在字节码里是一次 `invokedynamic`，不是匿名内部类 —— 用 `javap -c` 看一眼 | 笔记 Day 4 小节 |

> 优先做第二个 20 分钟。「lambda 里改不了外部局部变量」几乎所有教程都只说「规定如此」，
> 真实原因是**捕获的是值拷贝**，跟 Day 1 的擦除、Day 2 的 sneakyThrow 是同一类问题 ——
> 编译器骗得过类型系统，骗不过运行时的内存布局。
