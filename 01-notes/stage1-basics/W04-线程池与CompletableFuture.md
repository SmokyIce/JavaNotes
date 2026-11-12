# W04 · 线程池与 CompletableFuture

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟨 进行中（Day 1 完成，2026-11-12）　｜　开始日期：2026-11-12　｜　完成日期：____-__-__

> 💡 周六小程序：任务提交 → 线程池 → 异步执行 → 结果返回。给后面学 @Async 打基础。

---

## 一、本周知识点

- [x] Executor
- [x] ThreadPoolExecutor
- [x] 线程池参数
- [ ] CompletableFuture
- [ ] 异步结果返回练习

> Day 1 勾了前三格：`Executor` / `ThreadPoolExecutor` / 线程池参数。
> 判断标准不是「背得出七个参数的名字」，而是**能画出任务在「核心 → 队列 → 非核心 → 拒绝」四步里的走向**，
> 并且能解释「为什么无界队列会让 `maximumPoolSize` 变成摆设」。
> 这两条现在有实测表（S1~S5）和 JDK 源码原文（`execute()` 三步注释）两重证据。

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

## 四、踩坑与 Bug 复盘

| 现象 | 原因 | 排查过程 | 解决 | 底层原理 |
| :--- | :--- | :--- | :--- | :--- |
| OOM 实测第一版控制台只有一句 `OutOfMemoryError thrown from the UncaughtExceptionHandler`，连「第几个任务」都没打出来 | `catch (OutOfMemoryError e)` 里直接 `printf` —— **打印本身也要分配对象**，堆已经满了，格式化字符串又炸一次 | 预期能看到「提交到第 N 个任务」，实际一行数据都没有，反而是 UncaughtExceptionHandler 里的二次 OOM | 先预留 `byte[4MB]`，catch 到就 `reserve = null; System.gc();`，再打印 | OOM 之后整个 JVM 都「不能分配」，连报错都要先腾地方。**处理 OOM 的代码必须不依赖分配** |
| `ThreadLocal` 串味实验第一版两个请求都读到 `null`，看着像「没有残留」 | 用了 `newFixedThreadPool(2)`，两个 `submit` 落到**不同线程**上 —— 复用的前提根本没成立 | 把线程名打出来才看见 `pool-5-thread-1` 和 `pool-5-thread-2` | 改成 `newFixedThreadPool(1)`，并 `get()` 串行执行 | 线程池的「复用」是概率性的（线程数 > 1 时）；**要证明复用，必须先把并发降到 1** |
| `PoolFlow` 的 S4（core=0）第一格「线程数 1 / 队列 1」，换一次运行变成「线程数 1 / 队列 0」 | `execute()` 入队之后还会 `recheck` 一次（`workerCountOf == 0` 就补一个线程），补线程和「工作线程从队列里取任务」是两条并发路径 | 同一份代码连跑三次，这一格三种结果 | 结论只写「先进队、再借线程」，并在输出里显式声明「这一格会抖」 | 快照类实验只能保证「不变的那部分」是结论；**会抖的格子要标注，不能当证据** |
| 以为 `DiscardOldestPolicy` 只丢一个（队列里最老的那一个） | 它丢完会**重试**，重试还可能失败，于是接着丢 —— `#3` 挤掉 `#2`，`#4` 挤掉 `#3`，`#5` 挤掉 `#4` | 实测只跑了 `#1` 和 `#5`，跟「只丢一个」的预期对不上，回头翻 JDK 文档 | 按实测改成「连丢多个」，并把文档原话抄进 `day1-jdk-source.txt` | 拒绝策略是**循环**不是单次：`retried (which can fail again, causing this to be repeated)` |
| 一开始想用 `newFixedThreadPool(2)` 的返回值调 `getQueue()`，编译不过 | `ExecutorService` 接口上没有 `getQueue()`，那是 `ThreadPoolExecutor` 的方法 | `javac` 报「找不到符号」 | 强转 `(ThreadPoolExecutor) p` | `Executors` 返回的确实是 `ThreadPoolExecutor`，但换成 `ForkJoinPool`（`newWorkStealingPool`）就炸 —— **强转能过是工厂方法的实现细节，不是契约** |

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

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 2026-11-12 | 三 | Day 1 线程池：1 万个任务 `new Thread` 620 ms vs 池 13 ms（47.7 倍）/ 任务四步走 S1~S5（核心→队列→非核心→拒绝）/ 无界队列让 max 失效 / keepAlive 只收非核心 / 四种拒绝策略 / `submit` 吞异常 / `ThreadLocal` 串味 / OOM 第 176 个任务 / shutdown vs shutdownNow | 60 min | `PoolBasics` / `PoolFlow` / `PoolTraps` / `day1-result.txt` / `day1-jdk-source.txt` | ✅ |
|  | 四 |  |  |  |  |
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
