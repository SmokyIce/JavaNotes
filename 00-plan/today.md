---
title: 今天做什么
date: 2026-11-19
weekday: 周四
milestone: M1 · Java 地基
task: W04 Day 2 — CompletableFuture（跑完之后怎么接）
status: 已完成
---

# 今天 · 2026-11-19（周四）· 已完成

> Day 1 回答的是「任务在哪跑」，今天回答「跑完之后怎么接」。
> 三个问题：不传线程池它到底用哪个池、`thenApply` 和 `thenCompose` 差在哪、
> 以及异步链上的异常到底被谁吃了。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w04（6 文件）+ 跑 day1 三个入口 | ✅ 全部正常：`PoolBasics` 尾部 E 段完整、`PoolFlow` S1~S5 结论段完整、`PoolTraps` T5 串味复现 —— 无回归、无中断代码 |
| `01-notes` 里的未勾项 | 只剩 W04 的 `CompletableFuture` / `异步结果返回练习`（= 今天这两格），以及 W05 起的未解锁内容 |
| `02-code` 空目录 | 只剩 `w05-` 起的 `.gitkeep`（未开工），按口径不算遗留 |
| Day 1 的两条待办 | ① `submit` 吞异常 → 今天 C 段换 CF 复现，销账；② `ThreadLocal` 串味 → 今天升级成「默认池全 JVM 共享」，销账 |

**结论：没有需要补的中断代码。** 上一轮（11-12）的产出完整，今天直接接着做 Day 2。

## 二、今天的正题：三个程序

代码在 `02-code/stage1-basics/w04-threadpool-completablefuture/src/main/java/com/newt/java48/w04/day2/`：

| 文件 | 内容 |
| :--- | :--- |
| `AsyncBasics.java` | A 默认池是谁（并行度 11）/ B 阻塞任务进公共池 = 排队 / C `runAsync` vs `supplyAsync` / D 自建池隔离 / E 三种现成 CF |
| `ChainShape.java` | A `thenApply` vs `thenCompose` 的类型 / B 三种 `thenApply` 的线程 / C `thenCombine` / D `allOf` / E `anyOf` / F 串 vs 并 |
| `AsyncErrors.java` | A 失败后节点全跳过 / B `exceptionally` / C `handle` / D `whenComplete` / E 三者互相抛 / F `join` vs `get` + `unwrap` / G 返回值没人接 / H 超时 |
| `day2-result.txt` | 全部实测（215 行，含编译错误原文） |
| `day2-jdk-source.txt` | `uniWhenComplete` / `reportJoin` / `commonPool` 并行度原文 |
| `compile-errors/ChainTypeErrors.java` | 3 条 `incompatible types` 原文（不参与编译） |

## 三、今天挖到的五个点

1. **不传 executor 就是 `ForkJoinPool.commonPool()`，并行度 = 核数 - 1。**
   12 核机器 → **11**；30 个任务只用到 11 个 worker 线程。而且它**全 JVM 唯一** ——
   所有 `parallelStream()` 共用。这条决定了「阻塞任务必须自带线程池」。

2. **阻塞任务的代价是「排队」，不是「变慢」。**
   14 个 300 ms 任务：公共池 **623 ms**（≈ `ceil(14/11) × 300`，分两批），自建 `fixed(14)` **308 ms**。
   更阴的是污染范围：业务里的 `supplyAsync` 能把同 JVM 无关的 `parallelStream` 一起卡住。

3. **`thenApply` / `thenCompose` 用错了不报错，只是悄悄套一层盒子。**
   `thenApply(u -> fetchOrders(u))` 得到 `CF<CF<String>>`，`join()` 打印
   `CompletableFuture@<hash>[Completed normally]` —— 要 join 两次。`thenCompose` 一次到位。
   和 W02 的 `map` / `flatMap` 同构。

4. **`whenComplete` 物理上吞不掉异常。**
   它的 action 是 `BiConsumer`，返回值 `void` —— 没有出口。实测：上游失败 + `whenComplete` 抛，
   cause 仍是**原始异常**，新异常只挂在**外层包装**的 `suppressed`（= 1）上。
   而 `exceptionally` 里抛会覆盖原始异常，且原始异常**彻底丢失**（cause 和 suppressed 里都没有）。

5. **不带 `Async` 的 `thenApply` 可能跑在 `main` 里。**
   规则是「在完成上游的那个线程上执行」，上游早已完成时，「那个线程」就是调用者线程 —— 实测就是 `main`。
   所以「写在异步链里」≠「跑在异步线程上」。

## 四、今天踩的坑（五个）

- **先写注释、后被实测打脸**：「`whenComplete` 抛异常会覆盖原始异常」写反了，实际是原始异常胜出。
  这是本月第六次「断言先于实测」，好在输出对不上时立刻回头查了源码。
- **查 `suppressed` 查错了对象**：先打 `e.getCause().getSuppressed()` 得 0，和源码 `addSuppressed` 矛盾；
  同时打两层才发现新异常挂在**外层 `CompletionException`** 上（1），cause 自己才是 0。
- **`e instanceof IllegalStateException` 编译不过**：`e` 声明成 `CompletionException`，两者无继承关系，
  `instanceof` 在编译期被判为不可能 —— 要演示「匹配不到」得先用 `Throwable` 接住。
- **`Runtime.availableProcessors()` 不是静态方法**：要 `Runtime.getRuntime().availableProcessors()`。
- **`run.sh` 的短名只认 day1**：跑 Day 2 必须写 `./run.sh day2.AsyncBasics`，否则 `ClassNotFoundException`。

## 五、环境备忘

- 跑今天：`./run.sh day2.AsyncBasics`（约 1.5 s）/ `day2.ChainShape`（约 2.5 s）/ `day2.AsyncErrors`（约 2 s）
- 编译错误取证：`javac -J-Duser.language=en -J-Duser.country=US -encoding UTF-8 -d /tmp/ce compile-errors/ChainTypeErrors.java`
  —— 加英文 locale 才看得懂报错，Git Bash 默认输出是 GBK 乱码
- `commonPool` 想调并行度：`-Djava.util.concurrent.ForkJoinPool.common.parallelism=N`（全局生效，慎用）

---

# 下一个时间盒（60 分钟）

**W05 Day 1：Spring IOC / DI** —— 代码放 `02-code/stage1-basics/w05-spring-ioc-di/`

> ⚠️ 动手前先解决工具链：**本机没有 Maven/Gradle**。两条路选一条，
> 建议先走 ①，因为它不引入新工具、20 分钟内能跑起来：
> ① 手工下载 5 个 jar（`spring-core` / `spring-beans` / `spring-context` / `spring-expression` / `commons-logging`）
> 放 `lib/`，`run.sh` 里 `-cp "out;lib/*"`；② 装 Maven，补 `pom.xml`（W05 收尾时做，不挪文件）。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 2：默认池是哪个 / `thenApply` 与 `thenCompose` 的返回类型 / `whenComplete` 为什么吞不掉 | 口头过 |
| 15 分钟 | **先写「没有 Spring」的版本**：`OrderService` 里 `new OrderRepository(new JdbcTemplate(...))` 三层，再改成构造器传参 —— 数一下「换一个实现要动几处」 | `day1/ManualWiring.java` |
| 20 分钟 | 同一套类交给 `AnnotationConfigApplicationContext`：`@Configuration` + `@Bean` + `@ComponentScan`，打印「Bean 是谁 new 的、什么时候 new 的、是不是同一个」 | `day1/SpringWiring.java` |
| 10 分钟 | 三个问号：`getBean` 两次是不是同一个对象（单例）/ 不写 `@Component` 会怎样（NoSuchBeanDefinition）/ 循环依赖会不会报错 | 输出段落 |
| 5 分钟 | 输出 `day1-result.txt` + 笔记 W05 Day 1 小节 + 勾掉「Bean · IOC · DI」 | 笔记 + 快照 |

> 优先做第二个 20 分钟。「Spring 到底帮你管理什么」这个问题，
> **必须靠「手写版 vs Spring 版」的 diff 才讲得清** —— 手写版里 `new` 散在三个地方，
> Spring 版里只有一个 `@Bean` 方法，这个对比比任何定义都管用。
