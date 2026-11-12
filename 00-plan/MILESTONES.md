---
title: Java 进阶 · 里程碑计划
subtitle: 不追周次，只追产出 · 8K → 25K
tags:
  - java
  - 学习计划
  - 里程碑
created: 2026-10-09
supersedes: java-48week-plan.md
daily: 60 分钟
---

# Java 进阶 · 里程碑计划

> **为什么改掉周次制**
>
> 原 48 周计划是「周次驱动」——计划表说你第 3 周该学并发，你第 5 周才学到，
> 于是每天都在「欠账」。断档一次，账就永远还不完，最后干脆不看计划表了。
>
> 真实情况：2026-09-22 跑了 Day 1，然后停了 17 天（忙别的）。
> **这不是落后几周，是周次制本身不适合你的节奏。**
>
> 改成「里程碑制」：
>
> - **不设第几周，只设"要做完什么"。**
> - 做完一个里程碑，才解锁下一个。做完就是做完，没有「晚了」。
> - 进度只有一种表达：**产出物拿到了没有**。

---

## 一、唯一的日常标准

每天 60 分钟，只要产出下面三样里的**任意两样**，今天就算数：

1. **一个知识点**（能用自己话讲出来，不是看完）
2. **一段代码**（能跑，哪怕 20 行）
3. **一份笔记回填**（写进当周笔记的对应小节）

> 三样是理想；两样是及格。**"今天只干了 30 分钟" 不是失败，"今天没打开 IDE" 才是。**

**断档规则（重要）**：允许断，但不允许"补"。断了一天，第二天从**当天该做的那一小步**接着走，
不去补昨天漏的。补课是周次制的病根，取消它。

---

## 二、里程碑总览

七个里程碑，从下往上解锁。**M1 是你现在唯一要关心的事。**

| 里程碑 | 一句话目标 | 解锁条件（硬标准） | 状态 |
| :--- | :--- | :--- | :--: |
| **M0 · 收尾** | 把 W01 剩余做完，找回手感 | 见下方「M0 待办」全部勾掉 | ✅ 已封板 2026-10-27 |
| **M1 · Java 地基** | 集合 / 泛型 / 并发 / 线程池，能讲原理 | 4 份笔记 + 4 个可运行工程 + 8 道面试题进题库 | 🟨 进行中 |
| **M2 · Spring 内核** | 事务 / AOP / 异步，能解释失效场景 | 4 份笔记 + 4 个可运行 Demo + 6 道面试题 | ⬜ |
| **M3 · 数据层** | MySQL 索引 / EXPLAIN / 锁 + Redis 基础 | 3 份笔记 + 10 条 EXPLAIN 案例 + Redis Demo | ⬜ |
| **M4 · 线上能力** | JVM 排查 / MQ / Linux / Docker | JVM 排查实验报告 + MQ 任务系统 + Docker 化跑通 | ⬜ |
| **M5 · 系统设计** | 微服务 / 高并发 / 性能优化 | 项目微服务化 + 架构图 + 技术文档 | ⬜ |
| **M6 · 面试冲刺** | 项目包装 + 题库 + 简历 | 5 大难题 + 全题库脱稿 + 简历终稿 | ⬜ |

> 原来的 48 周 → 现在**不折算周数**。按你每天 60 分钟、每周实际能跑 4–5 天的节奏，
> M0 一周内、M1 一个月上下、M2 一个月上下，大致是这个量级。
> **但不要在计划表上写死日期** —— 写了就又变成周次制了。

---

## 三、M0 · 收尾（当前唯一焦点）

目标：**把 W01 从「Day 1 完成」推到「W01 可以封板」**，用最小成本。

W01 原来的 7 天任务里，有性价比高低之分。重新排序如下：

| # | 任务 | 原计划位置 | 现在怎么办 | 产出 |
| :-- | :--- | :--- | :--- | :-- |
| 1 | HashMap 结构 + put 全流程（含源码） | Day 2 / Day 3 | ✅ 已完成 10-09 | 笔记「知识点」新增一节 + 小实验 |
| 2 | 手写 MyHashMap | Day 6 | ✅ 已完成 10-27（`put/get/resize` 三段 + 与 JDK 20 万次对拍） | `02-code/.../day6/MyHashMap.java` + `day6-result.txt` |
| 3 | HashMap 扩容观察实验 | Day 3 | ✅ 合并进 #1 | 打印扩容现场 |
| 4 | 集合面试题整理 | Day 5 | ✅ 已完成 10-27，共 12 题，题题有「我的回答 + 追问预判 + 项目案例」 | `04-interview/question-bank/Java基础.md` |
| 5 | HashSet / LinkedHashMap | Day 4 | ✅ 已完成 10-10（完整一节 + 10 个实验） | 笔记「Day 3」一节 + `day3-result.txt` |
| 6 | 全周总结 / 复述 | Day 7 | ✅ 已完成 10-27 | 笔记「六、一句话总结」封板版 + 打卡补完 |

**M0 完成定义**：笔记 W01 的「本周知识点」6 个勾全勾上、「每日打卡」7 行有内容、
`Java基础.md` 至少 3 道集合题写完、`MyHashMap` 能跑出结果。做到这些，W01 封板，直接进 M1。

> ✅ **2026-10-27 封板**：6 个知识点全勾、打卡 8 行、题库 12 题全部写完、`MyHashMap` 对拍 20 万次
> 0 处不一致。这一版比原定义多做两件事：题库从 3 道加到 12 道；手写版额外做了「树化退化」对照实验
> （hash 全等且 key 不可比较时，JDK 反而比手写链表慢 6.7 倍）。
>
> 断档记录：09-22 → 10-09（17 天）、10-10 → 10-27（17 天）。两次都按里程碑制「不补课、接着走」
> 处理，没有产生欠账感 —— 这正是改制的目的。

---

## 四、M1 · Java 地基（W02–W04 内容）

| 主题 | 笔记 | 代码 | 面试题库 |
| :--- | :--- | :--- | :--- |
| 泛型 · 异常 · Optional · Stream | `W02-泛型-异常-Stream.md` | `w02-generics-stream/` | Java基础 +3 |
| Java 并发基础 | `W03-Java并发基础.md` | `w03-concurrency-basics/` | Java并发 +8（✅ 已 8 道） |
| 线程池 · CompletableFuture | `W04-线程池与CompletableFuture.md` | `w04-threadpool-completablefuture/` | Java并发 +2 |

**M1 解锁标准**：三份笔记各自「核心问题」有个人回答 + 三个工程能一条命令跑起来 + 题库 8 道题有内容。

> 📌 **M1 进展**
>
> | 时间 | 做了什么 | 产出 |
> | :--- | :--- | :--- |
> | 2026-10-28 | **W02 Day 1 · 类型擦除 + PECS**（补掉 W02 空目录 + W01 遗留清单） | `w02-generics-stream/` 工程 + `day1/` 两个 Demo + `compile-errors/` + `day1-result.txt`；笔记 Day 1 整节；`Java基础.md` 12 → **15 题** |
> | 2026-10-29 | **W02 Day 2 · 异常体系** + Day 1 三个遗留 | `day2/` 两个 Demo（10 段实验）+ `day1/WildcardCapture` + `compile-errors/GenericLimits2`（6 个错）+ `day2-result.txt`；修掉 `ErasureProof` B2 段的 `NoSuchMethodException`；笔记 Day 2 整节；`Java基础.md` 15 → **17 题** |
> | 2026-10-30 | **W02 Day 3 · Optional** + Day 2 遗留「异常开销」 | `day3/` 两个 Demo（11 段实验）+ `day2/ExceptionCost`（两遍 JVM 参数对照）+ `day3-result.txt`；笔记 Day 3 整节 + Day 2 §10；`Java基础.md` 17 → **18 题** |
> | 2026-11-02 | **W02 Day 4 · Lambda** | `day4/` 两个 Demo（12 段实验）+ `compile-errors/LambdaLimits{,2,3}`（8 条报错原文，拆三个文件）+ `day4-result.txt` + `day4-bytecode.txt`（javap 三段证据）；笔记 Day 4 整节 + 参考要点 Q7/Q8；`Java基础.md` 18 → **20 题** |
> | 2026-11-02 | **W02 Day 5 · Stream** + Day 4 遗留 | `day5/` 三个 Demo（惰性 / 短路 / 屏障 / peek 坑 / 流一次性 / map vs flatMap / parallel）+ `day5-result.txt`；笔记 Day 5 整节（8 小节）+ 踩坑 4 条 + 参考要点 Q9/Q10；`Java基础.md` 20 → **22 题** |
> | 2026-11-03 | **W02 Day 6 · `List<User>` 报表**，W02 封板 | `day6/` 三个 Demo + `User` / `UserData`（15 条含 3 种 null）+ `day6-result.txt`；笔记 Day 6 整节（8 小节）+ 踩坑 7 条 + 参考要点 Q11；`Java基础.md` 22 → **24 题** |
> | 2026-11-04 | **W03 Day 1 · 线程基础**（`w03-concurrency-basics/` 从 `.gitkeep` 建工程） | `day1/StartThread`（三种起线程 + run/start + daemon）+ `day1/RaceCondition`（基线 / 2 万 vs 50 万对照 / volatile / synchronized 方法级与同步块 / Atomic / 可见性对照）+ `day1-result.txt`（147 行）+ `day1-bytecode.txt`（6 段 javap 证据）；笔记 Day 1 整节（8 小节）+ 踩坑 6 条 + 参考要点「为什么会有线程安全问题」；`Java并发.md` 0 → **2 题** |
> | 2026-11-05 | **W03 Day 2 · `synchronized` 锁的是谁** | `day2/LockTarget`（实例锁 this / 静态锁 Class / `new Object()` 等于没锁 / 字符串常量全局锁）+ `day2/Deadlock`（交叉死锁 + `ThreadMXBean` 检测 / 可重入 / 活锁 4/5 + 随机退避 + 全局定序）+ `day2-result.txt`（116 行）+ `day2-deadlock-jstack.txt`（jstack 原件 142 行）；笔记 Day 2 整节（8 小节）+ 踩坑 5 条 + 参考要点「锁的是谁」「死锁怎么排查」；`Java并发.md` 2 → **4 题**；`cheatsheets/JVM排查命令.md` 补「死锁」小节 |
> | 2026-11-06 | **W03 Day 3 · `volatile` 内存语义 + happens-before + DCL** | `day3/HappensBefore`（Dekker litmus 测出 `(0,0)` 16%~40% / monitor 传可见性 / volatile 发布订阅 5/5 vs 对照组 0/5 / start-join 抢跑 151/500 / `set` vs `lazySet` 5.8 倍）+ `day3/DclSingleton`（50 轮 × 6 线程压力 0 次 + 手工重排抓到 `{a=1,b=null,c=0}` + `Unsafe.allocateInstance`）+ `day3-result.txt`（86 行）+ `day3-bytecode.txt`（170 行，含 `lock addl` 原文）；笔记 Day 3 整节（8 小节）+ 踩坑 6 条 + 面试 9 条 + 参考要点 2 题；`Java并发.md` 4 → **6 题** |
> | 2026-11-11 | **W03 Day 4 · `Lock` / AQS / `wait-notify`**，W03 封板 | `day4/WaitNotify`（ISE / `if` vs `while` / `notify` 只叫一个 → 剩 1 件货 + 1 人 WAITING / `wait` 释放锁 0 ms vs `sleep` 414 ms / 中断清标志）+ `day4/MiniAqs`（**手写 state + CLH 队列 + park**，200 万次对拍零丢失；两次 CAS 与 park 重查的实测 1 / 87742 / 175075 / 87500；临界区 1 ns 慢 5.8 倍 → 0.5 µs 只慢 1.13 倍）+ `day4/LockCompare`（公平锁 41 倍、插队 154582 vs 445、最长等待反而更大；`Condition` 无效唤醒 11 vs 7666）+ `day4-result.txt`（129 行）+ `day4-bytecode.txt`（75 行）；笔记 Day 4 整节（10 小节）+ 踩坑 5 条 + 面试 14 条 + 参考要点 2 题；`Java并发.md` 6 → **8 题** |
> | 2026-11-12 | **W04 Day 1 · 线程池**（`w04-threadpool-completablefuture/` 从 `.gitkeep` 建工程） | `day1/PoolBasics`（A~E：1 万个任务 `new Thread` 620 ms vs 池 13 ms = 47.7 倍 / 线程命名 / keepAlive 只收非核心 3→2 / 四种拒绝策略 / shutdown 5 个跑完 vs shutdownNow 倒出 4 个）+ `day1/PoolFlow`（**S1~S5 五张快照表**：任务顺序「核心 → 队列 → 非核心 → 拒绝」；无界队列下 max 恒为 2 是摆设；core=max / core=0 / SynchronousQueue 三种形态）+ `day1/PoolTraps`（T1~T6：队列峰值 33714 / 单线程堵死 513 ms / cached 峰值 500 线程 / `submit` 吞异常而 `execute` 打印且换线程 / `ThreadLocal` 串味 / **OOM 第 176 个任务** vs 有界队列拒绝 99948 次不 OOM）+ `day1-result.txt`（164 行）+ `day1-jdk-source.txt`（execute 三步 + 排队三条规则原文）；笔记 Day 1 整节（11 小节）+ 踩坑 5 条 + 面试 12 条 + 参考要点 2 题；`Java并发.md` 8 → **10 题** |
>
> **W02 已封板（2026-11-03）**，六天全部完成。**W03 已封板（2026-11-11）**，四天全部完成。
> 题库 M1 只要 8 道，现在 24（Java基础）+ 10（Java并发）＝ **34 道，M1 题量已超标**。
> 当前在 **M1**，W04 已开（线程池 Day 1 完成），**下一步 W04 Day 2 = CompletableFuture**
> （`supplyAsync` / `thenApply` vs `thenCompose` / `thenCombine` / `allOf` / 异常与 `handle` / 默认用哪个线程池）。
>
> W04 Day 1 最值钱的三条结论（全部有实测数字）：
>
> 1. **任务顺序是「核心 → 队列 → 非核心 → 拒绝」**，不是「先开满线程再排队」。
>    实测 core=2 max=4 queue=2 提 10 个：线程数 `1→2→2→2→3→4→4`，队列 `0→0→1→2→2`。
>    JDK 类文档原话 `only if the queue is full`。这个顺序不看代码想不出来，也是最高频的追问。
> 2. **队列决定了 `maximumPoolSize` 是否生效**：无界队列 → 永远不满 → 永远走不到「开非核心」，
>    实测 max=4 峰值线程恒为 2。所以 `newFixedThreadPool(n)` 的 n 就是真实并发上限。
> 3. **无界队列的问题不是「会 OOM」，是「OOM 之前一切正常」**：`-Xmx64m` 下第 176 个任务才炸，
>    之前拒绝数一直是 0；换有界 50 + Abort 则拒绝 99948 次、堆稳稳的。
>
> W02 六天里最值钱的五条结论（讲课时直接能用，全部有实测数字）：
>
> 1. 擦除擦的是**实参**、留下的是**声明**；实参藏在子类 `Signature` 里（`getGenericSuperclass()`）。
> 2. **CCE 抛在 `get` 那一行，不是 `add` 那一行**；`finally` 里的 `return` 才是真的把异常变没。
> 3. **`new RuntimeException(e)` 不丢栈，是把栈切成两段**（实测 3 层 vs 5 层）；只传 message 才真丢。
> 4. **`count()` 会跳过整条管道连 `peek` 一起**（日志 0 条）；`sorted()` 在上游带 `SORTED` 特征时是空操作。
> 5. **`Predicate` 的 `or().and()` 是左结合 `(a||b)&&c`**，按布尔优先级写会静默少数据（实测 6 人 vs 7 人）。

---

## 五、M2 · Spring 内核（W05–W08 内容）

四份笔记 + 四个 Demo，其中 **W07 事务、W08 异步是全局最重点**——
因为你工作里正好踩过 `@Transactional` 和 `@Async` 失效的坑，这两个是"真实问题驱动"的学习。

`02-code/lab/` 下已经建好了对应的实验目录，直接用：

- `01-transaction-self-invocation` — `this.xxx()` 为什么让事务失效
- `02-transaction-propagation` — `REQUIRED` vs `REQUIRES_NEW`
- `03-async-transaction-boundary` — 事务为什么不会跨线程继承

> **M2 的验收方式很特别**：不看笔记写得多漂亮，看你能不能**当场写一个 Demo 让事务失效**，
> 再讲清楚怎么修。这是面试里最能体现深度的一类回答。

---

## 六、M3–M6（先不细化）

里程碑制的原则是**只细化当前和下一个，再远的等走到跟前再说**。
M3 之后的内容原封不动沿用 `java-48week-plan.md` 里的知识点清单即可，
到时候按同样的"保留 / 降级 / 砍掉"逻辑过一遍。

---

## 七、和原文件的关系

| 文件 | 作用 | 现在怎么用 |
| :--- | :--- | :--- |
| `MILESTONES.md`（本文件） | **唯一进度来源** | 每天看这个 |
| `PROGRESS.md` | 原 48 周勾选表 | **降级为参考**，不再要求勾齐；W01 封板时勾 W01 一行 |
| `java-48week-plan.md` | 知识点清单 | **当字典查**，不看周次 |
| `STAGES.md` | 阶段能力矩阵 | 保留，每完成一个里程碑自评一次 |

---

## 八、下一步

**M0 已封板（2026-10-27），当前焦点是 M1 · Java 地基。**

**W02 Day 1 已完成（2026-10-28）**：`w02-generics-stream/` 工程建好，`ErasureProof` / `PecsDemo`
两个 Demo + `compile-errors/GenericLimits.java` 四类报错原文跑通，笔记 Day 1 整节写完。

**W02 Day 2 已完成（2026-10-29）**：异常体系。`day2/` 两个 Demo 共 10 段实验
（TWR 逆序关闭与 suppressed / `finally` 里 `return` 吞异常 / `finally` 里 `throw` 覆盖主异常 /
包装是切栈而非丢栈 / cause 与 suppressed 分工 / 受检边界 / sneakyThrow）。
顺带补掉 Day 1 三个遗留：修 `ErasureProof` B2 段的 `NoSuchMethodException`、
`compile-errors/GenericLimits2.java`（6 个通配符报错）、`day1/WildcardCapture.java`（捕获正例 + JDK 的 raw 选择）。

**W02 Day 3 已完成（2026-10-30）**：Optional。`day3/` 两个 Demo 共 11 段实验
（`isPresent()+get()` 的报错退化 / `orElse` eager 求值白打远程 / Optional 当字段不可序列化 /
当参数签名骗人 / 五个静默陷阱 / `map` vs `flatMap` / `filter` 挡掉负价 / 优惠券算价）。
顺带把 Day 2 最后一个遗留量掉了：异常开销 447 ns → 关掉栈采集 31 ns（15 倍），
`day2/ExceptionCost.java`，跑两遍只改一个 JVM 参数。

**W02 Day 4 已完成（2026-11-02）**：Lambda。`day4/` 两个 Demo 共 12 段实验
（五种形状 / `Predicate` 短路组合 / `andThen` vs `compose` / 装箱逃逸对照 / 自定义函数式接口 /
lambda 缺目标类型 / 方法引用五连 / 值捕获 / 循环变量 / `this` 归属 / 实例复用 / `invokedynamic`）。
核心结论：`javap -p` 里 `lambda$sectionB$1(int, int)` 直接证明**捕获 = 把变量当实参传进静态方法**，
`effectively final` 由此不再需要背；装箱代价实测不逃逸 **2 倍**、逃逸 **36 倍**。
`compile-errors/LambdaLimits{,2,3}` 收 8 条报错原文 —— 必须拆三个文件，否则 attribution 阶段的报错
会让 FLOW 分析整个跳过，`effectively final` 那条根本不出现。

**W03 Day 1 已完成（2026-11-04）**：线程基础。`w03-concurrency-basics/` 工程从零建起，
`day1/StartThread`（三种起线程 + `run()` vs `start()` + daemon）+ `day1/RaceCondition`
（B0 基线 / B1 每线程 2 万 / B2 50 万 / C1 volatile / C2 synchronized 方法级与同步块 / C3 Atomic / C4 可见性对照），
产出 `day1-result.txt`（147 行）+ `day1-bytecode.txt`（6 段 `javap` 证据）。
三条核心结论：① `count++` 是 `getfield`/`iconst_1`/`iadd`/`putfield` 四条指令，窗口在 `iadd` 与 `putfield` 之间；
② 每线程 2 万次时三次运行只丢 4~7/10 轮（JIT 预热后单线程几十微秒跑完，10 个线程没重叠），
加量到 50 万次才 10/10 轮稳定复现；③ 方法级 `synchronized` **没有** `monitorenter`，
只有 `ACC_SYNCHRONIZED` 标志（原本以为有，被 `javap` 打脸），同步块才发指令并带异常路径。

**W03 Day 2 已完成（2026-11-05）**：`synchronized` 到底锁了谁。
`day2/LockTarget`（A 实例方法锁 `this`：同实例第二个排队 414 ms / 两实例都 0 ms / 与 `synchronized(this)` 抢同一 monitor，
排队 340 ms；B 静态方法锁 `Class`：排队 400 ms、`holdsLock(ClassLock.class)=true`、与实例锁并行 412 ms；
C `synchronized(new Object())` 丢 2846644/5000000 且耗时仅 46 ms vs 真锁 339 ms，外加字符串常量的跨类全局锁）
+ `day2/Deadlock`（D1 交叉死锁 + `findDeadlockedThreads()`；D2 可重入持计数；D3a 活锁 4/5、D3b 随机退避、
D3c 全局定序 4000/4000 零重试）。产出 `day2-result.txt`（116 行）+ `day2-deadlock-jstack.txt`（jstack 原件 142 行）。

四条核心结论：① 三种写法三把锁，证据用 `Thread.holdsLock()` + 「排队时长」，**总耗时会骗人**；
② `static synchronized` 保护不了实例字段（并行实测 412 ms）—— `@Transactional` 失效那一类 bug 的病根；
③ **「加了锁反而变快」几乎一定是锁没生效**（`new Object()` 46 ms vs 真锁 339 ms）；
④ 死锁的发现（`ThreadMXBean`）/ 确认（`jstack` 三样）/ 破解（全局定序）是三件事；活锁是概率的，死锁是确定的。

**W03 Day 4 已完成（2026-11-11）· W03 封板**：`Lock` / AQS / `wait-notify`。
`day4/WaitNotify`（A `wait()` 不在锁里 → `IllegalMonitorStateException: current thread is not owner`；
B 2 消费者 + 1 件货 + `notifyAll`：`if` 版空手而归 1 次 vs `while` 版 0 次；
C 放 2 件货只 `notify()` 一次 → 取走 1 件、剩 1 件、1 个消费者永久 `WAITING`（对比 `notifyAll` 2/0/0），
外加「没人在等时 `notify` 直接丢弃」（`wait(200)` 等满 200 ms）；
D `wait` 释放锁：旁观者排队 0 ms vs 持锁 `sleep` 414 ms；E 中断时 JVM 先清标志再抛异常）
+ `day4/MiniAqs`（**手写 `state` + CLH 双向队列 + `LockSupport.park/unpark`**：
200 万次对拍零丢失、不锁对照丢 1551791；`state` 1→3→1→0 加重入 ISE；
两次 CAS 实测「建哨兵 1 次 / 抢尾 87742 次」；175075 次挂起里 **87500 次醒来没抢到**；
临界区 1 ns 时比 `synchronized` 慢 5.8 倍，拉到 0.5 µs 只慢 1.13 倍，差额 ≈ `park × 4.2 µs`）
+ `day4/LockCompare`（公平锁慢 41 倍、插队 154582 次 vs 445 次，但**最长等待反而更大** 582 µs vs 387 µs；
`Condition` vs `notifyAll` 无效唤醒 11 次 vs 7666 次）。
产出 `day4-result.txt`（129 行）+ `day4-bytecode.txt`（75 行）。

三条核心结论：① **`park` 返回 ≠ 拿到锁**，87500 次「醒来没抢到」就是「必须重查」的实测证据，
最凶的插队者是「刚 `unlock` 转头又 `lock`」的线程；② 手写 AQS 慢的不是队列是**挂起**
（4.2 µs/次），临界区一变长差距就没了；③ 公平锁换来的不是「等得更短」而是「**等得有上界**」。

**W04 Day 1 已完成（2026-11-12）· 线程池**：`day1/PoolBasics`（A 1 万个任务：逐个 `new Thread` + `join`
620 ms vs 固定 8 线程池 13 ms＝**47.7 倍**，折合 62 µs / 线程；B 默认名 `pool-3-thread-1` 的 `3` 是全局池序号；
C `keepAlive` 只管非核心：堵住时 `poolSize=3`，+400 ms 回落到 2，`allowCoreThreadTimeOut(true)` 才归 0；
D 四种拒绝策略：Abort 抛 `#3#4#5` / CallerRuns 让 `#3#5` 在 `main` 里跑 / Discard 只跑 `#1#2` /
DiscardOldest 只跑 `#1#5`（连丢 `#2#3#4`）；E `shutdown` 5 个全跑完 vs `shutdownNow` 倒出 4 个 + 中断 1 个）
+ `day1/PoolFlow`（**S1~S5 五张快照表**，每提交一个拍一张 `poolSize` / `queueSize` / 是否被拒；
S1 线程数 `1→2→2→2→3→4→4`、队列 `0→0→1→2→2`；S2 无界队列峰值线程恒为 2 → **max 是摆设**；
S3 core=max 没有非核心阶段；S4 core=0 先进队再借线程；S5 `SynchronousQueue` 等于 `cached` 形态）
+ `day1/PoolTraps`（T1 队列峰值 33714；T2 单线程池堵死 513 ms；T3 cached 峰值 500 线程；
T4 **`execute` 打印堆栈且线程被替换 `thread-1`→`thread-2`，`submit` 不 get 一个字都不打**；
T5 `ThreadLocal` 串味读到上一个人的登录信息；T6 `-Xmx64m` 下**第 176 个任务 OOM** / 有界 50 拒绝 99948 次不 OOM）。
产出 `day1-result.txt`（164 行）+ `day1-jdk-source.txt`（`execute()` 三步注释 + 类文档「排队三条规则」原文）。

三条核心结论：① **顺序是「核心 → 队列 → 非核心 → 拒绝」**，JDK 原话 `only if the queue is full`；
② **队列决定 `maximumPoolSize` 是否生效** —— 无界队列永远不满，永远走不到「开非核心」；
③ **无界队列的问题不是「会 OOM」，是「OOM 之前一切正常」**（拒绝数一直是 0）。

下一个时间盒（60 分钟）：**W04 Day 2 · CompletableFuture** ——
① `supplyAsync` 默认用哪个线程池（`ForkJoinPool.commonPool()`）以及为什么工程里必须显式传自己的池；
② `thenApply` vs `thenCompose`（嵌套 `CompletableFuture` 的坑）、`thenCombine` / `allOf` 等所有；
③ 异常链：`exceptionally` / `handle` / `whenComplete` 各自在哪一层生效；
④ 输出 `w04/day2-result.txt` + 勾掉「CompletableFuture」。
时间盒见 `00-plan/today.md`（每天开工前先打开它，收工时覆盖）。
