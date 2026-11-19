# W04 · 线程池与 CompletableFuture

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟩 已完成（Day 1 11-12，Day 2 11-19）　｜　开始日期：2026-11-12　｜　完成日期：2026-11-19

> 💡 周六小程序：任务提交 → 线程池 → 异步执行 → 结果返回。给后面学 @Async 打基础。

---

## 一、本周知识点

- [x] Executor
- [x] ThreadPoolExecutor
- [x] 线程池参数
- [x] CompletableFuture
- [x] 异步结果返回练习

> Day 1 勾了前三格：`Executor` / `ThreadPoolExecutor` / 线程池参数。
> 判断标准不是「背得出七个参数的名字」，而是**能画出任务在「核心 → 队列 → 非核心 → 拒绝」四步里的走向**，
> 并且能解释「为什么无界队列会让 `maximumPoolSize` 变成摆设」。
> 这两条现在有实测表（S1~S5）和 JDK 源码原文（`execute()` 三步注释）两重证据。
>
> Day 2 勾后两格：`CompletableFuture` / 异步结果返回练习。
> 判断标准同样不是「背得出几个 thenXxx」，而是三条：**默认线程池是谁（以及为什么这是坑）**、
> **`thenApply` 与 `thenCompose` 的返回类型差在哪（要能当场写出两种类型）**、
> **异常在链上被谁吃掉（`whenComplete` 为什么吞不掉）**。

## 二、核心问题（能用自己的话脱稿讲出来，才算学会）

1.
2.
3.

## 三、代码 / 实验

- 代码目录：`02-code/stage1-basics/w04-threadpool-completablefuture/`
- 本周动手内容：

```text
Day 1（已完成 11-12）  线程池：1 万个任务 new Thread vs 池（620 ms vs 13 ms）
                       + 任务四步走（核心 → 队列 → 非核心 → 拒绝）跑成 S1~S5 五张表
                       + 队列决定一切（无界 / core=max / SynchronousQueue 三种形态）
                       + keepAliveTime 与 allowCoreThreadTimeOut、四种拒绝策略
                       + Executors 四个工厂的坑：无界队列 OOM（第 176 个任务）/ 单线程堵死
                         / cached 线程暴涨 500 / submit 吞异常 / ThreadLocal 串味
                       + shutdown vs shutdownNow
Day 2（已完成 11-19）  CompletableFuture：默认池是 commonPool（并行度 = 核数-1，实测 11 个线程）
                       + 阻塞任务进公共池 = 排队（14×300ms：公共池 623ms vs 自建池 308ms）
                       + thenApply vs thenCompose 的嵌套类型（CF<CF<String>>，join 两次）
                       + thenCombine / allOf / anyOf 三种编队（411ms vs 816ms 串行）
                       + 异常链 exceptionally / handle / whenComplete 三者的吞与不吞
                       + whenComplete 抛异常时原始异常仍胜出（挂 suppressed，不是覆盖）
                       + join vs get 的包装差异 + unwrap 的必要性
                       + orTimeout / completeOnTimeout
```

### Day 1 · 2026-11-12（周三）· 线程池：任务先入队，还是先开线程

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 W03 Day 4：为什么入队要 CAS 两次 / 为什么 `park` 后要重查 / `notify` 为什么默认 `notifyAll` | 口头过 |
| 15 分钟 | 建 `w04-threadpool-completablefuture/` 工程；1 万个任务对照 `new Thread` 与线程池；七个参数各改一次 | `day1/PoolBasics.java` A~E 段 |
| 20 分钟 | **任务在四步里怎么走**：核心线程 → 队列 → 非核心 → 拒绝，拍成 S1~S5 五张快照表 | `day1/PoolFlow.java` |
| 10 分钟 | `Executors` 四个工厂为什么被阿里手册禁用：无界队列 OOM 实测 + 有界队列对照 | `day1/PoolTraps.java` T1~T6 |
| 5 分钟 | 输出 `day1-result.txt` + 源码取证 + 本节笔记 + 勾掉前三格 | 笔记 + 快照 |

实测快照：`w04-threadpool-completablefuture/day1-result.txt`（JDK 17 / 12 核 / Windows 11，164 行）
源码取证：`w04-threadpool-completablefuture/day1-jdk-source.txt`（`ThreadPoolExecutor` 的 execute 三步 + 排队三条规则）

#### 1. `new Thread` 到底错在哪（先量化，再讲道理）

1 万个任务，每个任务做 1000 次加法：

| 方式 | 耗时 | 折算 |
| :--- | ---: | :--- |
| 每个任务 `new Thread` + `join` | **620 ms** | ≈ 62 µs / 线程 |
| 固定 8 线程池 | **13 ms** | ≈ 1.3 µs / 任务 |

**47.7 倍，全部差在「创建 / 销毁」上，不在「干活」上。** 任务本身只做 1000 次加法（纳秒级），
而创建一个线程要几十微秒 —— **线程比任务贵三个数量级**。这就是线程池存在的全部理由，
不是「方便」，是「创建线程这件事本身太贵」。

> 这个倍数会飘：同一台机器连跑三次是 **620 / 681 / 796 ms（47.7 倍 / 56.8 倍 / 66.3 倍）**，
> 线程池那边一直稳定在 12~13 ms。所以「创建线程很贵」这个量级是稳的，具体倍数别当常数背。

顺带撞到两件事：

- 默认线程名长这样：`pool-3-thread-1`。那个 `3` 是**全局线程池序号**（前面已经开过两个池子），
  跟业务毫无关系；后面那个 `1/2` 只是线程编号轮转。从堆栈里根本看不出这是哪个业务的哪批任务 ——
  所以线上必须自己写 `ThreadFactory`，命名成 `order-task-1` 这种。
- 一万个线程在这台机器上能开起来（620 ms），**但不代表安全**：栈是「预留」不是「提交」，
  真正危险的是 native 内存和上下文切换。别把「我本机能开一万个」当成结论。

#### 2. 七个参数，一句话一个

| 参数 | 管什么 | 容易答错的地方 |
| :--- | :--- | :--- |
| `corePoolSize` | 常驻线程数，**默认不收** | 不是「最小线程数」，是「不到 max 之前先填满的这一档」 |
| `maximumPoolSize` | 线程数上限 | **只在队列满了之后才起作用**（无界队列下永远是摆设） |
| `keepAliveTime` + `unit` | 空闲多久回收 | 只管**非核心**线程；核心线程要靠 `allowCoreThreadTimeOut(true)` |
| `workQueue` | 排队策略 | **它决定了整个池的性格**（见第 4 小节） |
| `threadFactory` | 怎么造线程 | 命名、是否 daemon、优先级、`UncaughtExceptionHandler` 都在这 |
| `handler` | 满了怎么办 | 四种策略，默认 Abort（抛异常） |
| （隐含）`runState` | 池子的生命周期 | `shutdown` / `shutdownNow` 之后所有 `execute` 都直接走拒绝 |

#### 3. 今天的主菜：任务先入队，还是先开线程

**教科书答案，也是大多数人答错的那个：核心线程满 → 先入队 → 队满才开非核心 → 都满才拒绝。**

默认回答是「先开到最大线程数，再排队」—— 正好反了。实测（core=2 max=4 queue=2，连提 10 个，
每个任务都堵在同一道门上，所以状态能稳住，提交一个拍一张快照）：

```text
  序号       1     2     3     4     5     6     7     8     9    10
  线程数     1     2     2     2     3     4     4     4     4     4
  队列       0     0     1     2     2     2     2     2     2     2
  结果      ok    ok    ok    ok    ok    ok   REJ   REJ   REJ   REJ
```

读法：

- `#1 #2` → 建核心线程（线程数 1 → 2）；
- `#3 #4` → **进队列**（队列 1 → 2），线程数**没有变**；
- `#5` → 队列刚满（2/2），才开第 3 个线程；`#6` 开到第 4 个（= max）；
- `#7` 起 → 线程到顶、队列满 → 拒绝。最终执行 6 个、拒绝 4 个。

JDK 类文档原话（`day1-jdk-source.txt` 第一节）：

> `Else if fewer than maximumPoolSize threads are running, a new thread will be created to handle the request`
> **`only if the queue is full`.**

`execute()` 的三步注释和代码是完全对应的（第三节）：先 `addWorker(command, true)`（核心）、
再 `workQueue.offer`（入队）、`offer` 失败才 `addWorker(command, false)`（非核心）、
再失败 `reject`。

#### 4. 队列决定了整个池的性格

| 场景 | core | max | queue | 执行 / 拒绝 | 峰值线程 | 结论 |
| :--- | ---: | ---: | :--- | :--- | ---: | :--- |
| S1 标准 | 2 | 4 | `ArrayBlockingQueue(2)` | 6 / 4 | 4 | 四步全走一遍，标准形态 |
| S2 无界 | 2 | 4 | `LinkedBlockingQueue()` | 10 / 0 | **2** | **max 一次都没生效** |
| S3 core=max | 2 | 2 | `ArrayBlockingQueue(2)` | 4 / 6 | 2 | 没有非核心阶段，队满直接拒绝 |
| S4 core=0 | 0 | 4 | `ArrayBlockingQueue(2)` | 6 / 4 | 4 | 第一个任务先进队，再借线程 |
| S5 不排队 | 2 | 4 | `SynchronousQueue` | 4 / 6 | 4 | 队列永远「满」，直接开线程 |

四条：

1. **S2 是最重要的一行：`max=4` 完全没用。** 因为队列无界 → 永远不满 → 永远走不到「开非核心」那一步。
   所以 `newFixedThreadPool(n)` 的 n 就是真实并发上限；`maximumPoolSize` 在这个工厂里形同虚设。
2. **S3 说明 `core == max` 时没有「非核心」这一档**：队列一满就直接拒绝。
3. **S5 是 `newCachedThreadPool` 的形态**：`SynchronousQueue` 容量 0，来一个任务就得有人立刻接，
   没人接就开线程 —— 它的 `max` 是 `Integer.MAX_VALUE`，所以理论上永不拒绝，直到开不出线程。
4. **S4（core=0）是 JDK 的补丁行为**：`execute()` 第 2 步里 `else if (workerCountOf(recheck) == 0) addWorker(null, false)` ——
   因为入队之后又检查了一次「万一一个线程都没有呢」。所以它的第一格是「线程 1 / 队列 1」，
   而且这一格会随调度抖动。

**所以真实工程里的结论是：先选队列，再定线程数。** 队列选无界，线程数就没有上界意义上的意义了，
只有「消费速度」；队列选有界，就必须配套一个会出声的拒绝策略。

#### 5. 线程什么时候死：keepAliveTime 只管非核心

同一把尺子量出来（core=2 max=4 queue=1 keepAlive=300ms）：

```text
4 个任务堵住时: poolSize=3 queue=1     ← 第 4 个任务开出了非核心线程（这条本身就是「队满才开」的证据）
  +  0 ms  poolSize=3
  +150 ms  poolSize=3
  +400 ms  poolSize=2                  ← 非核心线程被回收
  +800 ms  poolSize=2                  ← 核心线程不动

开 allowCoreThreadTimeOut(true) 之后：
  刚跑完: poolSize=2
  +500 ms: poolSize=0                  ← 全死光
```

**默认配置下核心线程是「常驻」的**，哪怕一个任务都没有它也占着。想让闲时彻底释放，
必须显式 `allowCoreThreadTimeOut(true)` —— 代价是下一次来任务要重新付 62 µs 的建线程钱。

#### 6. 四种拒绝策略：谁出声，谁静默

core=1 max=1 queue=1，连提 5 个任务（`#1` 在跑、`#2` 在队列、`#3#4#5` 走策略）：

| 策略 | 实测日志 | 结果 |
| :--- | :--- | :--- |
| `AbortPolicy`（默认） | `抛异常#3 抛异常#4 抛异常#5 跑#1 跑#2` | 抛 `RejectedExecutionException`，能被 catch 到 |
| `CallerRunsPolicy` | `跑#3@main 跑#1 跑#5@main 跑#2 跑#4` | 一个都没丢，但 `#3#5` 在 **main 线程**里跑 |
| `DiscardPolicy` | `跑#1 跑#2` | `#3#4#5` 静默消失，**无异常无日志** |
| `DiscardOldestPolicy` | `跑#1 跑#5` | `#2#3#4` 被后来的任务逐个挤掉 |

三条：

- **`CallerRunsPolicy` 是唯一的「背压」**：提交线程被自己的任务拖住，天然限速。
  代价是提交线程可能被卡很久（在 Web 场景里就是 Tomcat 的工作线程）。
- **`DiscardPolicy` 最危险，因为它不出声。** Abort 至少能让调用方感知，Discard 只能靠对账发现丢单。
- **`DiscardOldestPolicy` 会连丢多个**：`#3` 进来挤掉 `#2`，`#4` 挤掉 `#3`，`#5` 挤掉 `#4`。
  JDK 文档那句 `retried (which can fail again, causing this to be repeated)` 就是这个。

#### 7. `submit` 吞异常 —— 生产上最常见的一类「任务没生效」

```text
[execute] 任务里抛异常 → 控制台直接打印堆栈，且线程死了被替换：
          Exception in thread "pool-4-thread-1" java.lang.IllegalStateException: ...
          下一个任务的线程名 = pool-4-thread-2   ← 旧线程已经死了，这是新补的

[submit ] 同样的异常，不调用 get() → 一个字都不打印
[submit ] get() 才拿到：ExecutionException(cause=IllegalStateException: ...)
```

机制：`execute` 的异常一路抛到 `runWorker` 之外，走线程的 `UncaughtExceptionHandler`（默认打印）；
`submit` 把它包进 `FutureTask`，异常存在 `Future` 里等人 `get`。

**「任务提交了但没生效」的排查第一步永远是：Future 有没有人 get。**
工程里用 `submit` 又不 get，等于把异常扔进黑洞；要么改成 `execute`，要么统一加
`CompletableFuture.whenComplete` / 外面包一层兜底日志。

#### 8. 线程池里的 `ThreadLocal` 会串味

```text
请求A[pool-5-thread-1] 设置后读到: 用户A的登录信息
请求B[pool-5-thread-1] 读到        : 用户A的登录信息  ← 上一个请求的残留
请求B[pool-5-thread-1] remove 之后 : null
```

线程是复用的，`ThreadLocal` 挂在线程上（`ThreadLocalMap` 是 `Thread` 的字段），
不 `remove()` 就一路活到下一个任务。所以「用完必须 remove」不是洁癖，是正确性要求 ——
租户 ID、用户身份、`TraceId` 这类上下文必须写成 `try { ... } finally { CTX.remove(); }`。

> 这个实验第一版写成 `newFixedThreadPool(2)`，两个请求落到不同线程上，第二个读到 `null`，
> 「残留」根本没发生。**用线程池做实验，也会被调度骗** —— 想演示「复用」，先保证真的是同一个线程。

#### 9. OOM 实测：无界队列是「最后一秒才崩」

`-Xmx64m`，每个任务自带 256 KB 数组（跟着 `Runnable` 一起进队列）：

| 队列 | 结果 |
| :--- | :--- |
| `LinkedBlockingQueue`（无界） | 提交到**第 176 个**任务时 `OutOfMemoryError`；成功入池 175、队列积压 173、**拒绝 0** |
| `ArrayBlockingQueue(50)` + Abort | 入池 52、**拒绝 99948**、队列稳定在 50、堆稳稳的 |

**无界队列的问题不是「会 OOM」，是「OOM 之前一切正常」。** 拒绝数 0、监控全绿、QPS 正常，
直到堆撑不住那一刻整体垮掉。这就是阿里手册那条「线程池不允许使用 `Executors` 去创建」的实测现场。

有界队列的代价是「调用方必须处理拒绝」（降级 / 重试 / 返回 429），但那是**必须做的功课**，不是可选项。

#### 10. 关线程池：`shutdown` 收尾，`shutdownNow` 掀桌

| 方法 | 实测 |
| :--- | :--- |
| `shutdown()` | 5 个排队任务**全部跑完** → 完成 5 个 |
| `shutdownNow()` | 返回**未执行 4 个**、跑完 0 个、正在跑的 1 个**被中断**（`InterruptedException`） |

两个都是「不再接受新任务」，区别在队列：`shutdown` 把队列跑完，`shutdownNow` 把队列**倒出来还给你**。
`shutdownNow` 的中断是「尽力而为」—— 任务里不检查中断标志、不响应 `InterruptedException` 的，
照样跑完。所以**优雅停机要自己实现**：`shutdown()` → `awaitTermination(30s)` → 还没完再 `shutdownNow()`。

#### 11. 那线上参数到底怎么定

一句话版本：

- **CPU 密集** → `N + 1`；**IO 密集** → `2N` 起步，等待越久越大（`N × (1 + 等待时间/计算时间)`）；
- 但**先定队列、再定线程数**：队列必须**有界**，`handler` 必须**出声**（Abort + 上层兜底，或 CallerRuns 背压）；
- 线程池必须**自己 `new`**、自己命名、自己暴露指标（`getActiveCount` / `getQueue().size()` / 拒绝次数），
  并且做成**全局复用**的 Bean，别每个请求 `new` 一个池子。

`2N` 这种公式只是起点，真正的数只能压测出来 —— 因为「等待时间/计算时间」这个比值只能实测。

### Day 2 · 2026-11-19（周四）· CompletableFuture：跑完之后怎么接

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 1：任务四步顺序 / 为什么无界队列让 max 失效 / `submit` 与 `execute` 的异常差别 | 口头过 |
| 15 分钟 | `supplyAsync` / `runAsync`：**默认线程池是谁**、几个线程、阻塞任务塞进去会怎样 | `day2/AsyncBasics.java` |
| 20 分钟 | **`thenApply` vs `thenCompose`**：把类型打出来；`thenCombine` / `allOf` / `anyOf` | `day2/ChainShape.java` |
| 10 分钟 | 异常链三种写法：`exceptionally` / `handle` / `whenComplete` 各自能不能吞 | `day2/AsyncErrors.java` |
| 5 分钟 | 输出 `day2-result.txt` + 源码取证 + 本节笔记 + 勾掉后两格 | 笔记 + 快照 |

实测快照：`w04-threadpool-completablefuture/day2-result.txt`（215 行）
源码取证：`w04-threadpool-completablefuture/day2-jdk-source.txt`（`uniWhenComplete` / `reportJoin` / `commonPool` 并行度）
编译错误原文：`compile-errors/ChainTypeErrors.java`（3 条 `incompatible types`）

> 一句话分工：**线程池解决「在哪跑」，`CompletableFuture` 解决「跑完之后怎么接」**。
> Day 1 留在那儿的两条伏笔今天都用上了 —— `submit` 吞异常（换成 CF 一样吞）、
> 线程复用导致 `ThreadLocal` 串味（换成 CF 的默认池一样串，而且范围更大：全 JVM）。

#### 1. 不传 executor，任务落在 `ForkJoinPool.commonPool()` 上

```java
CompletableFuture.supplyAsync(() -> "x");   // 没有第二个参数
```

跑出来是什么：

| 观测点 | 实测（12 核机器） |
| :--- | :--- |
| `Runtime.getRuntime().availableProcessors()` | 12 |
| `ForkJoinPool.commonPool().getParallelism()` | **11** = `max(1, 核数 - 1)` |
| 刚启动时 `getPoolSize()` | 0（按需创建，不是一开始就 11 个） |
| `supplyAsync` 里的线程名 | `ForkJoinPool.commonPool-worker-1` |
| 30 个任务用到几个不同线程 | **11 个**（worker-1 ~ worker-11） |

源码对齐（`ForkJoinPool.java:2564`）：

```java
private ForkJoinPool(byte forCommonPoolOnly) {
    int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
```

`CompletableFuture.java:430-438` 则说明了默认池是谁：

```java
private static final Executor ASYNC_POOL = USE_COMMON_POOL ?
    ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();
```

**为什么要强调「-1」**：留一个核给调用者（主线程）本身，否则提交者自己都抢不到 CPU。
这个设计对「短小的 CPU 任务」是对的，对「阻塞的 IO 任务」是灾难 —— 见下一节。

#### 2. 阻塞任务进公共池：不是变慢，是排队

14 个任务（并行度 11 + 3），每个 `sleep(300)`：

| 提交到哪 | 耗时 | 解释 |
| :--- | ---: | :--- |
| `commonPool`（并行度 11） | **623 ms** | 分 2 批 × 300 ms —— 多出来的 3 个只能等 |
| 自建 `newFixedThreadPool(14)` | **308 ms** | 14 个线程同时睡，一批跑完 |

这就是那句「不是慢一点，是排队」的实证。而真正的生产事故是**污染范围**：

- `commonPool` 是**全 JVM 唯一**的，所有 `parallelStream()`、所有没传 executor 的 `supplyAsync` 共用它；
- 你在订单服务里 `supplyAsync` 调一个 2 秒的 RPC，占掉的就是这 11 个坑之一；
- 占满之后，**同 JVM 里跟它毫无关系的 `parallelStream` 也会卡住** —— 它们抢的是同一个池。

所以规矩很简单：**只要任务里有 IO（查库、RPC、文件、锁等待），就必须传自己的 executor。**
Spring 的 `@Async` 必须配 `TaskExecutor` 是同一个道理，只是它默认还会退化成 `SimpleAsyncTaskExecutor`（一个任务一个线程，更糟）。

#### 3. 传自己的池：线程名是唯一的排查线索

```java
.supplyAsync(this::loadOrder, orderPool)        // order-1
.thenApplyAsync(this::checkStock, stockPool);   // stock-1
```

实测两个阶段分别落在 `order-1` 和 `stock-1` —— 池子隔离 + 名字可读。
默认的 `pool-3-thread-1` 在 `jstack` 里什么信息都没有，线上排查时等于瞎的。

#### 4. `thenApply` vs `thenCompose`：差别只在签名里的那一层

```java
thenApply<U>   (Function<? super T, ? extends U>                   fn)
thenCompose<U> (Function<? super T, ? extends CompletionStage<U>>  fn)
```

参数里多了 `CompletionStage` 那一层，导致 **`U` 的推导方向完全不同**。
业务是「查用户 → 查他的订单」，第二阶段本身是异步的：

```java
CompletableFuture<CompletableFuture<String>> nested =
        CompletableFuture.supplyAsync(() -> "用户:42")
                         .thenApply(user -> fetchOrders(user));   // fetchOrders 返回 CF
```

实测三条：

| 写法 | `join()` 拿到什么 | 类型 |
| :--- | :--- | :--- |
| `thenApply` | `java.util.concurrent.CompletableFuture@<hash>[Completed normally]` | `CompletableFuture`，**还要再 `join()` 一次** |
| `thenCompose` | `用户:42 的订单共 3 笔` | `String`，一次到位 |

这不是「风格问题」，是**类型问题**，而且**编译器不拦**（`ChainTypeErrors.java` 里我故意赋值给 `CF<String>` 才拦下来）：

```text
error: incompatible types: inference variable U has incompatible bounds
    equality constraints: String
    lower bounds: CompletableFuture<String>
```

> 注意这条报错**不是**「cannot be converted」，而是「U 的边界冲突」——
> 目标类型把 `U` 钉死成 `String`，lambda 体却给出 `CF<String>`，两边打架。
> 所以真实代码里最常见的形状是：**它编译通过了**，因为你没写中间变量的类型，
> 然后 `join()` 打印出一个 `CompletableFuture@1b6d3586`，日志里全是这个鬼东西。

同一件事在 W02 叫 `map` vs `flatMap`；在 `Optional` 里叫 `map` vs `flatMap`；
在这里叫 `thenApply` vs `thenCompose`。**凡是「容器套容器」的 API 都有这一对。**

#### 5. `thenApply` 不带 Async：可能根本不在异步线程上

同一个上游 `supplyAsync(..., biz)`，三种接法：

| 写法 | 实测线程 | 说明 |
| :--- | :--- | :--- |
| `thenApply` | `main` | 上游已经完成 → **就在调用者线程里同步跑完** |
| `thenApplyAsync` | `ForkJoinPool.commonPool-worker-1` | 又回到公共池（默认池的第二次伏击） |
| `thenApplyAsync(..., biz)` | `biz-2` | 指定池子 |

「不带 Async 的 `thenXxx` 在完成上游的那个线程上执行」这句话是对的，但要补一句：
**上游早已完成时，「完成上游的那个线程」就是调用 `thenApply` 的线程**（这里是 `main`）。
所以「我明明写了异步，怎么做完一看耗时全在 main 里」不是玄学，是这一条。

#### 6. 三种编队：combine / allOf / anyOf

| API | 实测 | 要点 |
| :--- | :--- | :--- |
| `thenCombine` 两路各 400 ms | **411 ms**（串行写法 816 ms） | 两路是并行的，不是相加 |
| `allOf` 三个任务 300/200/100 ms | **306 ms**，`join()` 返回 `null` | 等最慢的；返回 `CF<Void>`，**结果要自己 `join` 收** |
| `anyOf` 300 ms 与 50 ms 两路 | **58 ms**，值类型 `Object` | 只等最快的；**类型信息被擦成 `Object`** |
| 三次 `thenCompose`（有依赖） | **631 ms** | 有依赖，必须串 |
| 三路并行 + `allOf`（无依赖） | **215 ms** | 没依赖就别串 |

`allOf` 的用法是固定的两段式：

```java
CompletableFuture.allOf(f1, f2, f3).join();      // 1. 等全部完成（返回值是 null）
List<R> rs = List.of(f1, f2, f3).stream().map(CompletableFuture::join).toList();  // 2. 再收
```

第二段的 `join()` 不会阻塞 —— 因为第一段已经保证它们全完成了。
**顺手能记住的一条**：`anyOf` 返回 `CF<Object>`，想拿回原类型只能强转，泛型在这里救不了你。

#### 7. 异常链：谁能吞，谁吞不掉

先看失败怎么传播：上游抛异常之后，链上所有「不处理异常」的节点**全部被跳过**（实测两段 `thenApply` 一行日志都没打），
异常沿途复制到最后一站，`join()` 抛 `CompletionException`，`getCause()` 才是真身。

三个方法的分工：

| 方法 | 什么时候执行 | 能不能把失败变成功 | 实测证据 |
| :--- | :--- | :--- | :--- |
| `exceptionally` | **只在失败时** | 能（返回兜底值） | 成功路径下不执行 |
| `handle` | 成功失败都执行 | 能（`ex == null` 分支） | `r=null, ex=CompletionException` |
| `whenComplete` | 成功失败都执行 | **不能** | 打了日志，`join()` 照样抛 |

`whenComplete` 为什么吞不掉？因为它签名的返回值是 `void` —— **没有出口**。
源码 `uniWhenComplete`（`CompletableFuture.java:848-877`）的 catch 块：

```java
} catch (Throwable ex) {
    if (x == null)
        x = ex;                 // 上游成功 → 新异常成为结果（成功变失败）
    else if (x != ex)
        x.addSuppressed(ex);    // 上游已失败 → 只能挂 suppressed
}
completeThrowable(x, r);
```

三段实测正好把这三种情况钉死：

| 场景 | 结果 |
| :--- | :--- |
| E1 上游失败 + `whenComplete` 又抛 | cause 仍是**原始异常**（原始赢），外层 `suppressed = 1`，挂上来的正是新抛的那个 |
| E2 上游成功 + `whenComplete` 又抛 | cause 是**新异常** —— 把成功变成了失败 |
| E3 上游失败 + `exceptionally` 又抛 | cause 是**新异常**，原始异常既不在 cause 也不在 suppressed → **彻底丢了** |

> 所以「用 `whenComplete` 兜底」从来兜不住；而 `exceptionally` 里自己的代码一旦抛异常，
> 会把原始异常**覆盖掉且不留痕** —— 这就是线上「只看到兜底 NPE，看不到真正的业务异常」的成因。

#### 8. `join` / `get` 与「异常越套越深」

| 调用 | 抛什么 | 备注 |
| :--- | :--- | :--- |
| `join()` | `CompletionException` | 非受检，方法不用 `throws` |
| `get()` | `ExecutionException` | 受检，必须处理；内部还会先拆一层 |
| `get(100, MS)` | `TimeoutException` | `join` 没有超时版本 |

嵌套链路里包装层数会累加。实测这条链：

```text
CompletionException(RuntimeException: 兜底崩了)
  <- RuntimeException(兜底崩了)
    <- CompletionException(IllegalStateException: 内层)
      <- IllegalStateException(内层)
```

两个后果：

1. **`exceptionally` / `handle` 拿到的 `ex` 是 `CompletionException`**，不是你的业务异常 ——
   实测 `ex.getMessage()` 是 `java.lang.IllegalStateException: 库存服务 500`（带类名的拼装串），
   `ex.getCause()` 才是业务异常本体。所以打日志直接打 `ex.getMessage()` 只会得到一堆没信息量的包装。
2. **`instanceof` 判断要拆到底**：实测 `e instanceof IllegalStateException → false`，
   `e.getCause() instanceof → true`，但失败两次之后一层 `getCause()` 又不够了 —— 必须用 `while` 循环的 `unwrap()`：

```java
static Throwable unwrap(Throwable t) {
    while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null)
        t = t.getCause();
    return t;
}
```

#### 9. 两个高频写法错误

**① `exceptionally` 的返回值没人接。**

```java
CompletableFuture<String> cf = failing();
cf.exceptionally(ex -> "兜底");   // ← 返回值被丢掉
cf.join();                        // 仍然抛：库存服务 500
```

`CompletableFuture` 是**不可变**的：每个 `thenXxx` / `exceptionally` 都返回**新的** CF，
老的那个状态一点没变。这跟 `String` / `Stream` 是同一个约定，但 CF 的「不可变」更容易忘，
因为它的方法名看起来像「设置处理器」。

**② 超时只是让 CF 提前完成，后台任务还在跑。**

| 写法 | 实测 |
| :--- | :--- |
| `orTimeout(200ms)` | 约 **212 ms** 抛 `TimeoutException` |
| `completeOnTimeout("降级值", 200ms)` | 约 **211 ms** 返回降级值，不抛 |

两者都**不会中断**那个 800 ms 的后台任务 —— 它照样跑完，只是没人要它的结果了。
要真正停下来，得把 `Future` 拿回来 `cancel(true)`，而且任务本身得响应中断。

## 四、踩坑与 Bug 复盘

| 现象 | 原因 | 排查过程 | 解决 | 底层原理 |
| :--- | :--- | :--- | :--- | :--- |
| OOM 实测第一版控制台只有一句 `OutOfMemoryError thrown from the UncaughtExceptionHandler`，连「第几个任务」都没打出来 | `catch (OutOfMemoryError e)` 里直接 `printf` —— **打印本身也要分配对象**，堆已经满了，格式化字符串又炸一次 | 预期能看到「提交到第 N 个任务」，实际一行数据都没有，反而是 UncaughtExceptionHandler 里的二次 OOM | 先预留 `byte[4MB]`，catch 到就 `reserve = null; System.gc();`，再打印 | OOM 之后整个 JVM 都「不能分配」，连报错都要先腾地方。**处理 OOM 的代码必须不依赖分配** |
| `ThreadLocal` 串味实验第一版两个请求都读到 `null`，看着像「没有残留」 | 用了 `newFixedThreadPool(2)`，两个 `submit` 落到**不同线程**上 —— 复用的前提根本没成立 | 把线程名打出来才看见 `pool-5-thread-1` 和 `pool-5-thread-2` | 改成 `newFixedThreadPool(1)`，并 `get()` 串行执行 | 线程池的「复用」是概率性的（线程数 > 1 时）；**要证明复用，必须先把并发降到 1** |
| `PoolFlow` 的 S4（core=0）第一格「线程数 1 / 队列 1」，换一次运行变成「线程数 1 / 队列 0」 | `execute()` 入队之后还会 `recheck` 一次（`workerCountOf == 0` 就补一个线程），补线程和「工作线程从队列里取任务」是两条并发路径 | 同一份代码连跑三次，这一格三种结果 | 结论只写「先进队、再借线程」，并在输出里显式声明「这一格会抖」 | 快照类实验只能保证「不变的那部分」是结论；**会抖的格子要标注，不能当证据** |
| 以为 `DiscardOldestPolicy` 只丢一个（队列里最老的那一个） | 它丢完会**重试**，重试还可能失败，于是接着丢 —— `#3` 挤掉 `#2`，`#4` 挤掉 `#3`，`#5` 挤掉 `#4` | 实测只跑了 `#1` 和 `#5`，跟「只丢一个」的预期对不上，回头翻 JDK 文档 | 按实测改成「连丢多个」，并把文档原话抄进 `day1-jdk-source.txt` | 拒绝策略是**循环**不是单次：`retried (which can fail again, causing this to be repeated)` |
| 一开始想用 `newFixedThreadPool(2)` 的返回值调 `getQueue()`，编译不过 | `ExecutorService` 接口上没有 `getQueue()`，那是 `ThreadPoolExecutor` 的方法 | `javac` 报「找不到符号」 | 强转 `(ThreadPoolExecutor) p` | `Executors` 返回的确实是 `ThreadPoolExecutor`，但换成 `ForkJoinPool`（`newWorkStealingPool`）就炸 —— **强转能过是工厂方法的实现细节，不是契约** |
| Day 2：先写注释「`whenComplete` 里抛异常会覆盖原始异常」，实测是**原始异常胜出** | 源码 `uniWhenComplete` 的 catch 把新异常 `addSuppressed` 到包装层，而 `encodeThrowable(x, r)` 在 `x` 就是 `r` 里那个实例时直接返回 `r` | 输出里 cause 仍是「原始异常」，跟注释打架 | 按实测改文案，并把 `suppressed` 一起打出来定位 | action 异常只在「上游本来没事」时才有话语权 |
| Day 2：查 `suppressed` 打印了 `e.getCause().getSuppressed()`，得到 0，一度以为「新异常被丢弃」 | suppressed 挂在**外层 `CompletionException`** 上，不在 cause 上 | 源码明明有 `addSuppressed`，实测却是 0 —— 两边矛盾，说明是测量位置错了 | 同时打两层：外层 = 1，cause 自己 = 0 | **测「异常结构」时，先想清楚你测的是哪一层对象**；`e` 和 `e.getCause()` 是两个东西 |
| Day 2：`e instanceof IllegalStateException` 编译报「不可转换的类型」 | `e` 声明成 `CompletionException`，和 `IllegalStateException` 没有继承关系，编译期就被判定不可能 | 想演示「匹配不到」，结果连编译都过不去 | 用 `Throwable e = ce;` 声明，`instanceof` 才合法 | `instanceof` 的可达性检查在编译期做；演示运行时行为要先用宽类型接住 |
| Day 2：`nested.join()` 有时打印 `[Completed normally]`，有时打印 `[Not completed]` | 内层 CF 是 `supplyAsync` 提交的，打印那一刻它可能还没跑完 —— `toString` 里的状态本身就是一个竞态 | 重跑三次看到两种状态，改笔记时才发现「证据会抖」 | 先 `join().join()` 把内层等到完成，再打印外层 | **和 Day 1 的 S4 同一类错误：会抖的格子不能当证据。** 要举证「类型是 Future」，打 `getClass()` 而不是 `toString()` |

---

## 五、面试会怎么问

- **线程池的核心线程数、最大线程数、队列满了之后的顺序是什么？**（核心 → 队列 → 非核心 → 拒绝。
  答「先开到最大再排队」是错的，JDK 文档原话是 `only if the queue is full`）
- 为什么 `newFixedThreadPool` 的 `maximumPoolSize` 没用？（队列是 `LinkedBlockingQueue` 无界，永远不满）
- `newCachedThreadPool` 为什么可能把机器打爆？（`SynchronousQueue` + `max = Integer.MAX_VALUE`，
  一个任务一个线程；实测 500 个 60 ms 的任务 → 峰值 500 线程）
- `newSingleThreadExecutor` 有什么坑？（单线程 + 无界队列：一个慢任务堵死全部，实测 3 个瞬间完成的任务等了 513 ms）
- 线程池的七个参数分别是什么？`keepAliveTime` 对核心线程生效吗？（默认不生效，要 `allowCoreThreadTimeOut(true)`）
- 四种拒绝策略的区别？`CallerRunsPolicy` 为什么能当背压？（提交线程自己跑，天然限速；实测 `#3#5` 在 `main` 里跑）
- `submit` 和 `execute` 有什么区别？（`submit` 包成 `Future`，**异常被吞**，不 `get` 等于没发生；
  `execute` 走 `UncaughtExceptionHandler`，会打印堆栈且线程被替换）
- 线程池里的 `ThreadLocal` 会有什么问题？（线程复用 → 串味，实测读到上一个人的登录信息，必须 `finally remove()`）
- 线程池怎么优雅关闭？（`shutdown` → `awaitTermination` → 超时再 `shutdownNow`；
  前者跑完队列，后者把队列倒出来并尽力中断）
- 无界队列的风险是什么？（不是「会 OOM」，是「OOM 前一切正常」；实测第 176 个任务才炸，拒绝数一直是 0）
- 线程数怎么定？（CPU 密集 `N+1`、IO 密集 `N × (1 + W/C)`；公式只是起点，
  **先定有界队列 + 出声的拒绝策略，再压测定数**）
- 线程池怎么做监控？（`getActiveCount` / `getQueue().size()` / 自定义 `handler` 计拒绝次数，
  再上报到指标系统；这些数才是「池子够不够」的唯一依据）
- `CompletableFuture.supplyAsync` 不传线程池，用的是哪个池？几个线程？（`ForkJoinPool.commonPool()`，
  并行度 `max(1, 核数-1)`，实测 12 核 → 11 个 worker；**全 JVM 共享**）
- 为什么阿里手册要求 `@Async` / `supplyAsync` 必须指定线程池？（阻塞任务占满公共池，
  连无关的 `parallelStream` 一起卡住；实测 14 个 300 ms 任务：公共池 623 ms vs 自建池 308 ms）
- `thenApply` 和 `thenCompose` 有什么区别？（前者 `T -> R`，后者 `T -> CF<R>` 会拍平；
  用错的后果是 `CF<CF<String>>`，**编译不报错**，`join()` 打印出一个 `CompletableFuture@1b6d3586`）
- 不带 `Async` 的 `thenApply` 在哪个线程跑？（完成上游的那个线程；上游已完成时就是**调用者线程**，
  实测在 `main` 里跑完）
- `exceptionally` / `handle` / `whenComplete` 三者区别？（前两个能给兜底值、能吞异常；
  后者签名返回 `void`，**吞不掉**，上游已失败时它的异常只能挂 `suppressed`）
- `whenComplete` 里抛异常会覆盖原始异常吗？（不会。实测 cause 仍是原始异常，新异常只进 suppressed；
  但 `exceptionally` 里抛会覆盖，**且原始异常彻底丢失**）
- `join` 和 `get` 的区别？（`CompletionException` vs 受检的 `ExecutionException`；
  `get` 支持超时；嵌套链路里都会越套越深，要写 `unwrap()`）
- 为什么 `exceptionally` 写了却没生效？（忘了接返回值 —— CF 不可变，每个 `thenXxx` 都返回新对象）
- `orTimeout` 会中断任务吗？（不会。只是让 CF 提前完成，后台任务继续跑完）

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 2026-11-12 | 三 | Day 1 线程池：1 万个任务 `new Thread` 620 ms vs 池 13 ms（47.7 倍）/ 任务四步走 S1~S5（核心→队列→非核心→拒绝）/ 无界队列让 max 失效 / keepAlive 只收非核心 / 四种拒绝策略 / `submit` 吞异常 / `ThreadLocal` 串味 / OOM 第 176 个任务 / shutdown vs shutdownNow | 60 min | `PoolBasics` / `PoolFlow` / `PoolTraps` / `day1-result.txt` / `day1-jdk-source.txt` | ✅ |
| 2026-11-19 | 四 | Day 2 CompletableFuture：默认池 commonPool（并行度 11，30 任务只用 11 线程）/ 阻塞任务排队（623 ms vs 308 ms）/ `thenApply` vs `thenCompose` 的 `CF<CF<String>>` / combine 411 ms vs 串行 816 ms / allOf 返回 null / 异常三种写法 + `whenComplete` 吞不掉 / suppressed 位置 / unwrap / orTimeout | 60 min | `AsyncBasics` / `ChainShape` / `AsyncErrors` / `day2-result.txt` / `day2-jdk-source.txt` / `compile-errors/ChainTypeErrors.java` | ✅ |
|  | 五 |  |  |  |  |
|  | 六 |  |  |  |  |
|  | 日 |  |  |  |  |

---

## 参考要点（先自己答，再对照）

**Q1 · 线程池提交一个任务，是先开线程还是先入队？**

**核心线程没满 → 直接开线程；核心线程满了 → 先入队；队列满了 → 才开非核心线程；两边都满 → 拒绝。**
顺序是「核心 → 队列 → 非核心 → 拒绝」。

证据有两层：

1. **实测**（`PoolFlow` S1，core=2 max=4 queue=2，连提 10 个）：
   线程数 `1 → 2 → 2 → 2 → 3 → 4`，队列 `0 → 0 → 1 → 2 → 2`。
   第 3、4 个任务只让队列长了，线程数一动不动 —— 这就是「先入队」。
2. **源码**（`execute()` 三步）：
   `if (workerCountOf(c) < corePoolSize) addWorker(command, true)` →
   `else if (workQueue.offer(command))` → `else if (!addWorker(command, false)) reject(command)`。
   类文档里那句 `a new thread will be created ... only if the queue is full` 是同一件事的文字版。

**为什么大家会答错**：直觉上「线程越多越快」，所以以为会先把线程开满。但线程池的设计目标不是「最快」，
是「稳定」—— 队列是缓冲，非核心线程是应急，两者顺序反了就等于把队列作废了。

追问「那无界队列呢」：无界队列永远不满，所以**永远走不到开非核心那一步**，`maximumPoolSize` 是摆设
（实测 S2：max=4，峰值线程恒为 2）。这是 `newFixedThreadPool` 的真实行为。

---

**Q2 · 线程池参数怎么配？为什么阿里手册禁用 `Executors`？**

**禁用理由只有一条：那四个工厂方法把「队列」和「线程数上限」配成了危险组合，而这两个才是真正决定生死的参数。**

| 工厂 | 实际配置 | 危险在哪 |
| :--- | :--- | :--- |
| `newFixedThreadPool(n)` | core=max=n，`LinkedBlockingQueue`（无界） | 队列无上界 → 堆积到 OOM（实测 `-Xmx64m` 下第 176 个任务炸，之前拒绝数一直是 0） |
| `newSingleThreadExecutor()` | 同上，n=1 | 一个慢任务堵死全部（实测 3 个瞬间完成的任务等了 513 ms） |
| `newCachedThreadPool()` | core=0，max=`Integer.MAX_VALUE`，`SynchronousQueue` | 任务一多线程暴涨（实测 500 任务 → 500 线程），打满 CPU / 开不出线程 |
| `newScheduledThreadPool(n)` | `DelayedWorkQueue`（无界） | 同上：排队任务无上界；且单线程调度器一个任务卡住全部 |

正确姿势是**自己 `new ThreadPoolExecutor(...)`**，把四个数想清楚：

1. **队列必须有界**（`ArrayBlockingQueue` 或指定容量的 `LinkedBlockingQueue`）——
   队列就是「允许多少任务在等」，这个数必须是你能接受的最大积压量；
2. **拒绝策略必须出声**（Abort + 上层兜底，或 CallerRuns 背压），`Discard*` 基本不能用；
3. **线程数**：CPU 密集 `N+1`、IO 密集 `N × (1 + 等待/计算)`，但只是起点，最终靠压测；
4. **线程命名 + 指标**：`ThreadFactory` 命名（默认的 `pool-3-thread-1` 什么都看不出来），
   并把 `getActiveCount` / `getQueue().size()` / 拒绝次数上报 —— 没有指标就没法判断池子够不够。

一句话收尾：**参数配错的表现不是「慢」，是「平时一切正常，峰值直接崩」**，
所以队列有界 + 拒绝出声 + 有指标，三件事缺一不可。

---

**Q3 · `thenApply` 和 `thenCompose` 的区别，为什么用错了不报错？**

**`thenApply` 的函数返回「值」，`thenCompose` 的函数返回「另一个异步」。**
区别在签名里：`thenCompose` 的参数是 `Function<? super T, ? extends CompletionStage<U>>`，
参数类型里多了 `CompletionStage` 这一层，正是它让 `U` 的推导停在「值」上，把外面那层拍平。

用错了**为什么不报错**：因为你写的是 `thenApply(user -> fetchOrders(user))`，
`fetchOrders` 返回 `CompletableFuture<String>`，于是 `U` 顺理成章被推成 `CompletableFuture<String>`，
结果类型成为 `CompletableFuture<CompletableFuture<String>>` —— **这是合法的泛型嵌套**。

只有在三种情况下才会露馅：

1. 你显式声明了中间变量 `CompletableFuture<String> cf = ...` →
   `incompatible types: inference variable U has incompatible bounds / lower bounds: CompletableFuture<String>`；
2. 你在 `join()` 之后继续调下游方法 → `cannot find symbol: method length()`；
3. 运行时打印 → `java.util.concurrent.CompletableFuture@<hash>[Completed normally]`。

实测（`ChainShape` A 段）：

| 写法 | `join()` 的值 | `join()` 的类型 |
| :--- | :--- | :--- |
| `thenApply` | `CompletableFuture@<hash>[Completed normally]` | `java.util.concurrent.CompletableFuture` |
| `thenCompose` | `用户:42 的订单共 3 笔` | `java.lang.String` |

**判断口诀**：看 `join()` 的返回类型 —— 拿到 `CompletableFuture` 就是套错了。
这和 W02 的 `map` / `flatMap`、`Optional` 的 `map` / `flatMap` 是同一个形状：
**容器套容器的 API，永远有「会不会拍平」这一对。**

---

**Q4 · `whenComplete` 为什么兜不住异常？`exceptionally` 里抛异常又会怎样？**

**因为 `whenComplete` 的 action 是 `BiConsumer`，返回值 `void` —— 它没有「返回值」这个出口。**
`exceptionally` / `handle` 的函数都返回 `T`，那个返回值就是新的结果（可以把失败变成功）；
`whenComplete` 只能「看」，结果由上游决定。

源码 `uniWhenComplete`（`CompletableFuture.java:848-877`）的 catch 块把这件事写得很直白：

```java
} catch (Throwable ex) {
    if (x == null) x = ex;              // 上游成功 → 新异常成为结果
    else if (x != ex) x.addSuppressed(ex);  // 上游已失败 → 只能挂 suppressed
}
completeThrowable(x, r);
```

三种场景的实测（`AsyncErrors` E 段）：

| 场景 | cause | suppressed |
| :--- | :--- | :--- |
| 上游失败 + `whenComplete` 抛 | **原始异常** | 外层 1（= 新抛的那个），cause 自己 0 |
| 上游成功 + `whenComplete` 抛 | 新异常 | —— （把成功变成了失败） |
| 上游失败 + `exceptionally` 抛 | **新异常** | 0 / 0，**原始异常彻底丢失** |

最后一行是最凶的：兜底代码自己挂了，原始业务异常**既不在 cause 也不在 suppressed**。
所以写 `exceptionally` 的正确姿势是**兜底逻辑自己 try-catch**，或者在链上再挂一层 `exceptionally`，
否则线上只能看到「兜底 NPE」，看不到真正的故障原因。

顺带一条：`exceptionally` / `handle` 拿到的 `ex` 是 **`CompletionException`**（不是业务异常），
`ex.getMessage()` 是 `java.lang.IllegalStateException: xxx` 这种拼装串，
`ex.getCause()` 才是本体；嵌套两层之后一层 `getCause()` 也不够，要 `while` 循环的 `unwrap()`。
