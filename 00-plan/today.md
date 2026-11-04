---
title: 今天做什么
date: 2026-11-04
weekday: 周三
milestone: M1 · Java 地基
task: W03 Day 1 — 线程基础：把「丢更新」撞出来
status: 已完成
---

# 今天 · 2026-11-04（周三）· 已完成

> 进并发。并发是 M1 里唯一「不实测就必然学错」的一块，
> 所以今天只做一件事：**把共享可变状态用代码撞出来**，不讲 API 清单。
> 开工第一步照旧：**回归编译 + 跑全部入口**。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w02（18 文件）+ 跑 16 个入口 | ✅ 全部 exit=0，无回归，**没有中断的代码** |
| `01-notes` 里的未勾项 | W02 已全部勾完；W03 之后全是未解锁里程碑，无孤儿待办 |
| w02 里剩下的「遗留」三条 | `toMap` 四参 `mapFactory`、`concurrentGroupingBy`、`Files.lines()` 持句柄 —— 分别等 W03 / W04 / W08，今天不动 |
| `w03-concurrency-basics/` | 只有 `.gitkeep`，**今天从零建工程**（`.gitkeep` 已删） |

## 二、今天的正题：线程基础

代码在 `02-code/stage1-basics/w03-concurrency-basics/src/main/java/com/newt/java48/w03/day1/`：

| 文件 | 内容 |
| :--- | :--- |
| `StartThread.java` | A~E：`extends Thread` / `Runnable` + lambda / `Callable` + `FutureTask` / `run()` vs `start()` / daemon |
| `RaceCondition.java` | B0 基线 / B1 每线程 2 万（复现不稳）/ B2 50 万（稳定复现）/ C1 volatile / C2 synchronized（方法级 + 同步块各 5 轮）/ C3 `AtomicInteger` / C4 可见性对照 |
| `day1-result.txt` | 两个入口的完整实测输出（147 行） |
| `day1-bytecode.txt` | `javap` 六段证据：字段 flags / `incPlain` 四指令 / `incVolatile` 同指令 / `incSync` 只有 `ACC_SYNCHRONIZED` / `incSyncBlock` 才有 `monitorenter` + 异常路径 |

## 三、今天挖到的五个点

1. **`count++` 是四条字节码，窗口在 `iadd` 与 `putfield` 之间。**
   `getfield` → `iconst_1` → `iadd` → `putfield`。两个线程都停在 `getfield` 读到 100，
   各自算出 101、各自写回 —— 两次自增只多 1。
   **能指着 `putfield` 说「窗口在这儿」，和只会背「count++ 不是原子操作」是两种水平。**

2. **「本地跑十遍都是对的」有物理解释，不是玄学。**
   同一份代码，每线程 2 万次 × 10 线程 × 10 轮，今天三次运行分别丢 **4/10、5/10、7/10** ——
   连丢几轮都不稳定；某一轮里的形状还是「前 4 轮丢 80%+ → 中间 3 轮整齐 200000 → 后面又小额丢」，
   总耗时只有 30~50 ms。
   原因：JIT 预热后 2 万次只要几十微秒，而唤醒 10 个线程要上百微秒，**第一个跑完第九个还没被调度**。
   加量到每线程 50 万次，立刻 **10/10 轮丢**（最低 930367 / 5000000）。
   → 复现竞态两个必需品：**工作量够长** + **同时起跑**（`CountDownLatch(1)`）。

3. **`volatile` 救不了 `count++`，但真的能救标志位 —— 同一天两个相反的实验。**
   `volatile int vol; vol++` 10/10 轮丢（最低 1676548）；
   而自旋等标志位：普通 `boolean` **20 亿次自旋始终没看见**，`volatile boolean` **1.98 亿次后看见**（51 ms）。
   字节码上 `incVolatile()` 和 `incPlain()` 指令**一模一样**，差别只在字段 `flags: (0x0042) ACC_PRIVATE, ACC_VOLATILE`
   —— **volatile 不是多一条指令，是给 `getfield`/`putfield` 加内存语义。**

4. **方法级 `synchronized` 压根没有 `monitorenter`。**（今天被 `javap` 打脸的一条）
   方法级：`flags: (0x0022) ACC_PRIVATE, ACC_SYNCHRONIZED`，四条指令全是算术，**一条加锁指令都没有**；
   同步块：才真的出现 `monitorenter` / `monitorexit`，而且带 `Exception table` 两条 `any` ——
   这就是「同步块里抛异常也不会漏锁」的字节码依据。
   两种写法实测都 0/10 轮丢。

5. **`Callable` 的异常不会直接冒泡。** `future.get()` 抛的是
   `ExecutionException: java.lang.IllegalStateException: ...`，业务异常躺在 `getCause()` 里。
   只 `catch (ExecutionException e)` 不打 cause，日志里就只剩一句包装异常 —— 跟丢了没区别。
   顺带：`start()` 第二次调用抛 `IllegalThreadStateException`，**message 是 null**。

## 四、今天踩的坑

- **实验脚手架比被测代码更容易错。** 可见性实验第一版：主线程 `start()` 后直接 `sleep(20)` 就置位，
  结果 reader 输出「自旋 0 次就看见了」—— 自相矛盾（没进循环怎么叫看见）。
  加 `CountDownLatch` 握手（reader 进循环前 `countDown()`，主线程 `await()` 后再等 50 ms）才干净。
- **自旋上限必须远大于等待时间，否则测的是「谁先到终点」。** 第二版上限设 3 亿次，
  实测 3 亿次只要 200 ms，跟等待时间几乎相等 → 连 `volatile` 那条也「没看见」，
  差一点得出「volatile 没用」的荒谬结论。改成上限 20 亿 / 等待 50 ms 才对上。
- **写结论之前先跑一遍。** 字节码文件的标题我先写成「incSync 多出 monitorenter / monitorexit」，
  `javap` 打出来一条都没有。这已经是连续第三天栽在「断言先于实测」上（W02：`sorted` 屏障、
  `Predicate` 优先级、`toList` 的 null 政策）。低成本修法：**注释里的每个「所以」都配一条可复现命令**。

## 五、环境备忘

- 跑 day1：`run.bat`（默认 `StartThread`）/ `run.bat RaceCondition`
- `RaceCondition` 全跑约 5 秒（C2 的 synchronized 最慢，5 轮 1890 ms）
- 手动编译照旧先 `find src/main/java -name "*.java" > sources.txt`
- C4 那两行「自旋 N 次」的数字每台机器都不同，**只对比「看见 / 没看见」，别对比次数**

---

# 下一个时间盒（60 分钟）

**W03 Day 2：`synchronized` 到底锁了谁**

今天已经证明「加锁能救」，但没回答「锁的是哪个对象」。明天只做这件事。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 1：`count++` 的窗口在哪条指令之间 / 为什么 2 万次复现不了 / volatile 管哪件事 | 口头过，卡住回看 Day 1 |
| 20 分钟 | **锁对象三连**：① 实例方法锁 `this`，两个实例互不影响；② 静态方法锁 `Class`，与实例锁互不影响；③ `synchronized(new Object())` 等于没锁 | `day2/LockTarget.java` A~C 段 |
| 15 分钟 | **死锁复现**：两个线程交叉拿两把锁，统计「几秒内没跑完」；用 `jstack` 抓一次现场，再用 `ThreadMXBean.findDeadlockedThreads()` 程序化检测 | `day2/Deadlock.java` D 段 + `jstack` 快照 |
| 10 分钟 | 锁升级的现状核对：JDK 17 里偏向锁已默认关闭（JEP 374），用 `-XX:+PrintFlagsFinal` 查 `UseBiasedLocking` 实际值，别背 JDK 8 的八股 | 同上 E 段 + 输出片段 |
| 5 分钟 | 输出 `day2-result.txt` + 笔记 W03 Day 2 小节 + 勾掉 `synchronized` | 笔记 + 快照 |

> 优先做第二个 20 分钟。**「锁的是谁」答错，后面 `@Transactional` 失效、`@Async` 不生效、
> 单例里的 `synchronized` 拦不住两个实例，全是同一个病根**（W07 会再撞一次）。
>
> 死锁那 15 分钟也别省：`jstack` 抓到的那段 `Found one Java-level deadlock` 是线上排查的原件，
> 值得存进 `05-resources/cheatsheets/`。
