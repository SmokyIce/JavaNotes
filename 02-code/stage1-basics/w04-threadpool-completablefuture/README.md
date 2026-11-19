# W04 · 线程池与 CompletableFuture · 实验工程

> 目标：**回答「`new Thread` 到底错在哪」，以及「`ThreadPoolExecutor` 那七个参数各自负责什么」；
> 再回答「异步跑完之后怎么接」—— `CompletableFuture` 的默认线程池、类型嵌套、异常链。**
>
> Day 1 补的是「W03 全靠手动 `new Thread`」这个落差；Day 2 补的是「跑完就 join」这个落差。
>
> 本周笔记：[`../../../01-notes/stage1-basics/W04-线程池与CompletableFuture.md`](../../../01-notes/stage1-basics/W04-线程池与CompletableFuture.md)

---

## 怎么跑

纯 JDK，无第三方依赖，按 **JDK 17** 编译验证。

```bash
# Git Bash / macOS / Linux
./run.sh                        # 默认跑 PoolBasics（day1）
./run.sh PoolFlow               # 短名 -> day1 包
./run.sh day1.PoolFlow          # 带点号 -> 按完整路径解析
./run.sh day2.AsyncBasics       # Day 2 三个入口都要带 day2. 前缀
./run.sh day2.ChainShape
./run.sh day2.AsyncErrors
```

```bat
rem Windows
run.bat
run.bat PoolFlow
run.bat day1.PoolTraps
run.bat day2.AsyncBasics
run.bat day2.ChainShape
run.bat day2.AsyncErrors
```

OOM 实测必须单独给堆，两条命令都跑一遍才有对照：

```bash
java -Xmx64m -Dfile.encoding=UTF-8 -cp out com.newt.java48.w04.day1.PoolTraps oom        # 无界队列
java -Xmx64m -Dfile.encoding=UTF-8 -cp out com.newt.java48.w04.day1.PoolTraps oom-fixed  # 有界队列 50
```

手动编译：

```bash
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w04.day1.PoolBasics
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w04.day1.PoolFlow
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w04.day1.PoolTraps
```

> ⚠️ `PoolBasics` A 段会真的起 1 万个线程（约 620 ms，退出前全部 join 完），`PoolTraps` T3 会起 500 个线程。
> 内存吃得不多（栈是预留，不是提交），但**别在 CI 的 512 MB 容器里跑**。
> `PoolTraps` 带参数时结尾 `System.exit(0)` —— OOM 之后线程池里还有一堆堵在门上的任务，不 exit 退不干净。

## 目录

```text
w04-threadpool-completablefuture/
├── run.bat / run.sh                        一键编译 + 运行（短名默认在 day1 包里找）
├── day1-result.txt                         Day 1 全部实测（164 行）
├── day1-jdk-source.txt                     Day 1 源码取证（ThreadPoolExecutor 的 3 步 + 排队三条规则）
├── day2-result.txt                         Day 2 全部实测（215 行，含编译错误原文）
├── day2-jdk-source.txt                     Day 2 源码取证（uniWhenComplete / reportJoin / commonPool 并行度）
├── compile-errors/
│   └── ChainTypeErrors.java                故意写错：thenApply 套错 / thenCompose 反着用（不参与编译）
└── src/main/java/com/newt/java48/w04/
    ├── day1/
    │   ├── PoolBasics.java                 A~E：1 万个任务对照 / 线程命名 / keepAlive / 四种拒绝策略 / 关闭
    │   ├── PoolFlow.java                   S1~S5：任务在「核心 → 队列 → 非核心 → 拒绝」四步里怎么走
    │   └── PoolTraps.java                  T1~T6：Executors 四个工厂的坑
    └── day2/
        ├── AsyncBasics.java                A~E：默认池是谁 / 阻塞任务排队 / runAsync vs supplyAsync / 自建池 / 三种现成 CF
        ├── ChainShape.java                 A~F：thenApply vs thenCompose 的类型 / Async 变体 / combine / allOf / anyOf / 串并对比
        └── AsyncErrors.java                A~H：异常链三种写法 / suppressed 挂哪层 / join vs get / unwrap / 超时
```

> `compile-errors/` 不在 `src/main/java` 下，`run.sh` 的 `find` 不会把它捞进去。
> 要单独编它看报错（注释用英文，否则 javac 回显源码行会在 Git Bash 里变 GBK 乱码）：
>
> ```bash
> javac -J-Duser.language=en -J-Duser.country=US -encoding UTF-8 -d /tmp/ce compile-errors/ChainTypeErrors.java
> ```

## Day 1 做了什么

> 主题：**线程池**。要回答三个问题：`new Thread` 错在哪、七个参数各管什么、
> 「任务先入队还是先开线程」这个最高频的追问。

### `PoolBasics` —— 先把「为什么要线程池」量化

| 段 | 内容 | 实测 |
| :-- | :--- | :--- |
| A | 1 万个任务，每个任务 1000 次加法：逐个 `new Thread` + `join` vs 固定 8 线程池 | **620 ms vs 13 ms（47.7 倍）**；连跑三次是 620 / 681 / 796 ms —— 量级稳，倍数会飘 |
| B | 默认线程名 vs 自定义 `ThreadFactory` | 默认 `pool-3-thread-1/2`（编号还是全局的）；自命名 `order-task-1/2` |
| C | core=2 max=4 queue=1 keepAlive=300ms，4 个任务堵住 | 堵住时 `poolSize=3`（队列满才开第 3 个）；+400 ms 回落到 2；`allowCoreThreadTimeOut(true)` 后归 0 |
| D | 四种拒绝策略，core=1 max=1 queue=1 连提 5 个 | Abort 抛 3 个异常 / CallerRuns 一个不丢但 `#3#5` 在 main 里跑 / Discard 只跑 `#1#2` / DiscardOldest 只跑 `#1#5` |
| E | `shutdown()` vs `shutdownNow()` | 前者 5 个全跑完；后者返回未执行 4 个、正在跑的 1 个被中断 |

三个必须记住的点：

1. **47.7 倍全部差在「创建/销毁」上。** 每个 `new Thread` 在 1 万个量级下约 **62 µs**（620 ms / 10000），
   而任务本身只做 1000 次加法（纳秒级）。**线程比任务贵三个数量级** —— 这就是线程池存在的全部理由。
2. **C 段那个 `poolSize=3` 本身就是一条证据**：core=2、queue=1，提交第 4 个任务时线程数才变成 3 ——
   说明第 3 个任务进的是队列，不是新线程。
3. **`DiscardPolicy` 最危险，因为它不出声。** Abort 抛异常你能 catch 到，CallerRuns 至少把活干了，
   Discard 是「任务消失了、日志没有、监控没有」，只能靠对账发现。

### `PoolFlow` —— 今天的主菜：先入队，还是先开线程？

每个任务都堵在同一道门上（`gate.await()`），所以线程不会释放、状态不会变；
**提交一个就拍一张快照**（`getPoolSize()` / `getQueue().size()` / 是否被拒），最后拼成表。

S1（core=2 max=4 queue=2，连提 10 个）拍出来的就是这个：

```text
  序号       1     2     3     4     5     6     7     8     9    10
  线程数     1     2     2     2     3     4     4     4     4     4
  队列       0     0     1     2     2     2     2     2     2     2
  结果      ok    ok    ok    ok    ok    ok   REJ   REJ   REJ   REJ
```

读法：**1→2 建核心线程 → 3、4 进队列 → 5、6 队满才开非核心 → 7 起拒绝。**
`#5` 是分水岭：那一刻队列刚满（2/2），线程数才从 2 跳到 3。

五个场景一张表：

| 场景 | core | max | queue | 提交 | 执行 | 拒绝 | 峰值线程 | 结束剩余 |
| :--- | ---: | ---: | :--- | ---: | ---: | ---: | ---: | ---: |
| S1 标准 | 2 | 4 | Array(2) | 10 | 6 | 4 | 4 | 2 |
| S2 无界 | 2 | 4 | Linked(无界) | 10 | 10 | 0 | **2** | 8 |
| S3 无核（core=max） | 2 | 2 | Array(2) | 10 | 4 | 6 | 2 | 2 |
| S4 core=0 | 0 | 4 | Array(2) | 10 | 6 | 4 | 4 | 2 |
| S5 不排队 | 2 | 4 | SynchronousQueue | 10 | 4 | 6 | 4 | 0 |

四条结论：

1. **顺序是「核心 → 队列 → 非核心 → 拒绝」**，不是「先开满线程再排队」。
   JDK 类文档原话：`a new thread will be created ... only if the queue is full`
   （见 `day1-jdk-source.txt`）。这个顺序不看代码是想不出来的。
2. **S2 里 max=4 一次都没生效**（峰值线程恒为 2）。所以 `newFixedThreadPool(n)` 的 n 就是真实并发上限，
   `maximumPoolSize` 形同虚设 —— 因为它的队列是无界的，永远「不满」。
3. **S3（core=max）没有非核心阶段**：队列一满直接拒绝，线程数不会再多一个。
4. **S5（SynchronousQueue 容量 0）队列永远「满」**：任务一个个直接开线程，开到 max 就拒绝。
   这就是 `newCachedThreadPool` 的形态（它的 max 是 `Integer.MAX_VALUE`，所以理论上永远不拒绝，直到开不出线程）。

> S4（core=0）是特例：任务**先进队**，再借一个线程来消化（`execute()` 第 2 步里的
> `else if (workerCountOf(recheck) == 0) addWorker(null, false)`）。
> 所以它第一格是「线程 1 / 队列 1」，而且这一个格子会随调度抖动，看结论就好。

### `PoolTraps` —— Executors 四个工厂方法为什么被阿里手册点名

| 段 | 内容 | 实测 |
| :-- | :--- | :--- |
| T1 | `newFixedThreadPool(2)` 提交 5 万个空任务 | 队列峰值 **33714**，总耗时 20 ms，线程数始终 2 |
| T2 | `newSingleThreadExecutor`，前面一个 `sleep(500)` | 后面 3 个「瞬间完成」的任务等了 **513 ms** |
| T3 | `newCachedThreadPool`，500 个 60 ms 的任务 | 峰值线程 **500**（`maximumPoolSize=2147483647`） |
| T4 | `execute` vs `submit` 抛异常 | `execute` 打印堆栈且线程被替换（`thread-1` → `thread-2`）；`submit` 不 get **一个字都不打印** |
| T5 | 线程池复用线程 + `ThreadLocal` | 请求 B 读到请求 A 的 `"用户A的登录信息"`；`remove()` 后为 `null` |
| T6 | OOM（`-Xmx64m`，每个任务 256 KB） | 无界队列：第 176 个任务炸，入池 175 / 队列积压 173；有界 50：拒绝 99948 次，**堆稳稳的** |

三个必须记住的点：

1. **无界队列的 OOM 是「最后一秒才崩」**。前面一切正常、拒绝数为 0，直到堆扛不住 ——
   这就是阿里手册那条「线程池不允许使用 Executors 去创建」的实测现场。
   有界队列 + `AbortPolicy` 的代价是「调用方必须处理拒绝」，但那是**必须做的功课**，不是可选项。
2. **`submit` 吞异常是静默的**。`execute` 里抛的异常走线程的 `UncaughtExceptionHandler`，控制台能看到；
   `submit` 把它塞进 `Future`，没人 `get` 就等于没发生。**「任务没生效」的排查第一步永远是：Future 有没有人 get。**
3. **线程池里的 `ThreadLocal` 会串味**（实测读到上一个人的值）。线程是复用的，`ThreadLocal` 挂在线程上，
   不 `remove()` 就一路活到下一个任务 —— 租户 ID / 用户身份这类上下文必须 `try/finally remove()`。

> T5 这个实验第一版写成 `newFixedThreadPool(2)`，两个请求落到不同线程上，第二个读到 `null`，
> 「残留」根本没发生。**用线程池做实验，也会被调度骗** —— 想演示「复用」，就得先保证真的是同一个线程。

## Day 1 踩的坑

- **OOM 之后连报错都报不出来。** 第一版 `catch (OutOfMemoryError e)` 里直接 `printf`，
  控制台只剩一句 `OutOfMemoryError thrown from the UncaughtExceptionHandler` —— 打印本身也要分配对象。
  改成先预留 4 MB、catch 到就 `reserve = null; System.gc();` 再打印，才拿到「第 176 个任务」这个数字。
- **`ThreadLocal` 串味实验必须单线程池**（见上）。
- **`newFixedThreadPool(2)` 的返回值其实可以直接强转 `ThreadPoolExecutor`** —— `getQueue()` 只有它才有，
  但强转是有前提的（`Executors` 返回的就是 `ThreadPoolExecutor`），换成 `ForkJoinPool` 就炸。
- **`DiscardOldestPolicy` 会连丢多个。** 实测只跑了 `#1` 和 `#5`：`#3` 进来时挤掉 `#2`，
  `#4` 进来时挤掉 `#3`，`#5` 进来时挤掉 `#4` —— JDK 文档里那句
  `retried (which can fail again, causing this to be repeated)` 说的就是这个。

---

## Day 2 做了什么

> 主题：**CompletableFuture**。线程池解决「在哪跑」，它解决「跑完之后怎么接」。
>
> Day 1 留了两个伏笔，今天正好收：`submit` 吞异常（CF 一样吞）、线程复用导致 `ThreadLocal` 串味
> （换成 CF 的默认池范围更大 —— 全 JVM）。

### `AsyncBasics` —— 不传 executor，任务落在谁身上

| 段 | 内容 | 实测 |
| :-- | :--- | :--- |
| A | 默认池是谁、几个线程 | `ForkJoinPool.commonPool()`，并行度 **11**（12 核 → `max(1, 核数-1)`）；30 个任务只用到 **11 个** worker；刚启动 `getPoolSize()=0` |
| B | 14 个 300 ms 阻塞任务 | 公共池 **623 ms**（分 2 批）vs 自建 `fixed(14)` **308 ms** |
| C | `runAsync` vs `supplyAsync` | 前者返回 `null`（`Void`）；后者提交一个必抛异常的任务，**100 ms 后控制台一个字都没有**，`join()` 才抛 |
| D | 自建两个池 | `order-1` / `stock-1`，池子隔离 + 线程名可读 |
| E | 三种现成的 CF | `completedFuture`（不占线程）/ `failedFuture` / 手动 `complete`；`complete` 两次只有第一次返回 `true` |

三个必须记住的点：

1. **`commonPool` 是「核数 - 1」，而且全 JVM 只有一个。** 所有 `parallelStream()`、
   所有没传 executor 的 `*Async` 共用它 —— 阻塞任务占满之后，**跟它毫无关系的并行流一起卡住**。
2. **阻塞的代价是「排队」不是「变慢」**：623 ms ≈ `ceil(14/11) × 300`。
   所以生产上「只要任务里有 IO，就必须传自己的 executor」。
3. **`thenApplyAsync` 不传池子也回 `commonPool`**（D 段的对照组就是它）——
   默认池的伏击有两次：一次在起点，一次在每个忘了传池的中间节点。

### `ChainShape` —— 把类型打出来

| 段 | 内容 | 实测 |
| :-- | :--- | :--- |
| A | `thenApply` 里返回 CF | 得到 `CF<CF<String>>`，`join()` 打印 `CompletableFuture@<hash>[Completed normally]`，**要 join 两次**；`thenCompose` 一次到位 |
| B | 三种 `thenApply` | 不带 Async 的跑在 **`main`**（上游已完成）；`thenApplyAsync` 在 `commonPool-worker-1`；指定池在 `biz-2` |
| C | `thenCombine` | 两路各 400 ms → **411 ms**；串行写法 **816 ms** |
| D | `allOf` | 300/200/100 ms → **306 ms**，`join()` 返回 **`null`**，结果要自己 `map(join)` 收 |
| E | `anyOf` | 300/50 ms → **58 ms**，类型 `Object`（泛型救不了） |
| F | 串 vs 并 | 三次 `thenCompose` **631 ms**；三路并行 + `allOf` **215 ms** |

一句话：**`thenApply` 返回「值」、`thenCompose` 返回「异步」，用错了不报错，只是悄悄套一层盒子。**
这和 W02 的 `map` / `flatMap` 是同一个形状 —— 容器套容器的 API 永远有这一对。

### `AsyncErrors` —— 异常到底被谁吃了

| 段 | 内容 | 实测 |
| :-- | :--- | :--- |
| A | 上游失败后 | 后面两段 `thenApply` **一行日志都没打**，异常复制到最后一站 |
| B | `exceptionally` | 只在失败时跑；成功路径下不执行 |
| C | `handle` | 成功 `ex=null`、失败 `r=null, ex=CompletionException`，两条都能吞 |
| D | `whenComplete` | 打了日志、也「处理」了，`join()` **照样抛** |
| E | 三者互相抛异常 | 见下表 |
| F | `join` vs `get` | `CompletionException` vs 受检的 `ExecutionException`；`get` 支持超时 |
| G | `exceptionally` 返回值没人接 | 原 CF **仍然异常**（CF 不可变） |
| H | `orTimeout` / `completeOnTimeout` | 212 ms 抛 / 211 ms 返回降级值；**后台任务不会被中断** |

E 段三种场景（今天最值钱的一条，因为直觉全错）：

| 场景 | cause | suppressed |
| :--- | :--- | :--- |
| 上游失败 + `whenComplete` 抛 | **原始异常**（原始赢） | 外层 **1**，cause 自己 0 |
| 上游成功 + `whenComplete` 抛 | **新异常**（成功变失败） | — |
| 上游失败 + `exceptionally` 抛 | **新异常** | 0 / 0 → **原始异常彻底丢失** |

源码依据：`uniWhenComplete` 的 catch 是 `x.addSuppressed(ex)`，随后 `completeThrowable(x, r)`
在 `x` 就是 `r` 里那个实例时直接返回 `r`（`day2-jdk-source.txt` 第 4 节）。

## Day 2 踩的坑

- **先写注释「whenComplete 抛异常会覆盖原始异常」，实测是原始异常胜出。** 这条是典型的「断言先于实测」，
  跑完才发现写反了 —— 顺带把 `suppressed` 打出来，才看清新异常挂在**外层包装**上而不是 cause 上。
- **查 suppressed 查错了对象。** 第一次打 `e.getCause().getSuppressed()` 得到 0，和源码的 `addSuppressed`
  矛盾；改成同时打两层（外层 1 / cause 0）才对上。**测异常结构先想清楚测的是哪一层对象。**
- **`e instanceof IllegalStateException` 编译不过。** `e` 声明成 `CompletionException`，
  两个类没有继承关系，`instanceof` 在编译期就被判为不可能 —— 想演示「匹配不到」，得先用 `Throwable` 接住。
- **`Runtime.availableProcessors()` 不是静态方法**（第一次编译就撞上），要 `Runtime.getRuntime().availableProcessors()`。
- **`nested.join()` 的 `toString` 会抖。** 内层 CF 可能还没跑完，于是同一份代码有时打印
  `[...Completed normally]`、有时 `[Not completed]`。改成先 `join().join()` 再把外层打印出来才稳定 ——
  「会抖的格子不能当证据」这条 Day 1 已经踩过（S4），今天又踩一次。
- **跑 `day2.*` 必须带包名前缀。** `run.sh` 的短名规则只认 `day1`，写 `./run.sh AsyncBasics` 会 `ClassNotFoundException`。
