---
title: 今天做什么
date: 2026-11-12
weekday: 周三
milestone: M1 · Java 地基
task: W04 Day 1 — 线程池（任务先入队，还是先开线程）
status: 已完成
---

# 今天 · 2026-11-12（周三）· 已完成

> W03 之前所有实验都是「手动 `new Thread`」，而工程里没人这么干。
> 今天补上这个落差，要回答三件事：`new Thread` 到底错在哪、七个参数各自管什么、
> 以及那个最高频的追问 —— **任务先入队，还是先开线程？**

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w03（6 文件）+ 跑 day1/day2/day3 六个入口 | ✅ 全部 exit=0，行数 31 / 106 / 40 / 48 / 47 / 32，与 11-06 **逐个一致**，无回归、无中断代码 |
| `01-notes` 里的未勾项 | 只剩 W04 起的条目（今天是第一天）；W02/W03 已全勾 |
| `02-code` 空目录 | 只剩 `w04-` 起的 `.gitkeep`（未开工），按口径不算遗留 |
| Day 4 那条「手写 AQS 慢 5.8 倍是脚手架问题」 | 已在 11-11 用统一 `warmup()` 解决，销账 |

**结论：没有需要补的中断代码。** 今天从 `.gitkeep` 起一个全新工程。

## 二、今天的正题：三个程序

代码在 `02-code/stage1-basics/w04-threadpool-completablefuture/src/main/java/com/newt/java48/w04/day1/`：

| 文件 | 内容 |
| :--- | :--- |
| `PoolBasics.java` | A 1 万个任务对照 / B 线程命名 / C keepAlive 与核心线程 / D 四种拒绝策略 / E shutdown 两种 |
| `PoolFlow.java` | **S1~S5 五张快照表**：核心 → 队列 → 非核心 → 拒绝，一步一步拍下来 |
| `PoolTraps.java` | T1~T6：无界队列 / 单线程堵死 / cached 暴涨 / `submit` 吞异常 / `ThreadLocal` 串味 / OOM |
| `day1-result.txt` | 全部实测（164 行，五条命令整体重定向生成） |
| `day1-jdk-source.txt` | `ThreadPoolExecutor.execute()` 三步注释 + 类文档「排队三条规则」原文 |

## 三、今天挖到的五个点

1. **顺序是「核心 → 队列 → 非核心 → 拒绝」，不是「先开满再排队」。**
   core=2 max=4 queue=2 提 10 个任务，线程数 `1→2→2→2→3→4→4`，队列 `0→0→1→2→2`：
   第 3、4 个任务只让队列长了，线程数一动不动；**第 5 个才是分水岭**（队列刚满）。
   JDK 类文档原话 `only if the queue is full`。

2. **队列决定了 `maximumPoolSize` 到底有没有用。**
   换 `LinkedBlockingQueue`（无界）之后，max=4 一次都没生效 —— 峰值线程恒为 2，8 个任务全在队列里躺。
   所以 `newFixedThreadPool(n)` 的 n 就是真实并发上限。**先选队列，再定线程数。**

3. **线程比任务贵三个数量级：几十微秒 vs 纳秒。**
   1 万个「1000 次加法」的任务：逐个 `new Thread` + `join` 620 ms，固定 8 线程池 13 ms，**47.7 倍**
   （连跑三次 620 / 681 / 796 ms —— 量级稳，倍数会飘）。全部差在创建/销毁上 ——
   这就是线程池存在的全部理由，不是「方便」。

4. **无界队列的问题不是「会 OOM」，是「OOM 之前一切正常」。**
   `-Xmx64m`、每个任务 256 KB：第 **176** 个任务才炸，之前拒绝数一直是 0、队列积压 173。
   换 `ArrayBlockingQueue(50)` + Abort：拒绝 99948 次，堆稳稳的。**拒绝要出声，不出声就只能靠对账。**

5. **`submit` 吞异常是静默的，`execute` 不是。**
   `execute` 抛异常 → 控制台打印堆栈，且线程死了被替换（`thread-1` → `thread-2`）；
   `submit` 不 `get` → **一个字都不打**，异常躺在 `Future` 里。
   「任务提交了没生效」的排查第一步永远是：Future 有没有人 get。

## 四、今天踩的坑（五个）

- **OOM 之后连报错都报不出来。** 第一版 `catch (OutOfMemoryError e)` 里直接 `printf`，
  控制台只剩 `OutOfMemoryError thrown from the UncaughtExceptionHandler` —— 打印本身也要分配对象。
  改成先预留 4 MB、catch 到就 `reserve = null; System.gc();` 再打印，才拿到「第 176 个」这个数字。
- **`ThreadLocal` 串味实验第一版用了 `fixed(2)`**，两个请求落到不同线程上，第二个读到 `null`，
  「残留」根本没发生。改 `fixed(1)` 才复现。**用线程池做实验，也会被调度骗。**
- **`DiscardOldestPolicy` 不是只丢一个。** 实测只跑了 `#1` 和 `#5` —— 它会「丢队头 + 重试」，
  `#3` 挤掉 `#2`、`#4` 挤掉 `#3`、`#5` 挤掉 `#4`。JDK 文档那句 `retried (which can fail again...)` 说的就是这个。
- **`PoolFlow` 的 S4（core=0）第一格会抖。** 连跑三次三种结果（线程 1/队列 1、线程 1/队列 0）——
  入队之后还有一次 `recheck` 会补线程，和「工作线程取任务」是两条并发路径。**会抖的格子不能当证据**，输出里已标注。
- **`ExecutorService` 上没有 `getQueue()`。** 想采样队列长度必须强转 `ThreadPoolExecutor`；
  `Executors` 返回的确实是它，但换成 `newWorkStealingPool`（`ForkJoinPool`）就炸 —— 强转能过是工厂方法的实现细节，不是契约。

## 五、环境备忘

- 跑今天：`./run.sh PoolFlow`（约 1 s）/ `PoolBasics`（约 1.5 s）/ `PoolTraps`（约 2 s）
- OOM 实测必须单独给堆：`java -Xmx64m -cp out ...PoolTraps oom` / `oom-fixed`，两条都要跑才有对照
- 一万个线程在这台机器上能开（620 ms），但栈是**预留**不是提交 —— 别把「我本机能开」当结论

---

# 下一个时间盒（60 分钟）

**W04 Day 2：CompletableFuture** —— 代码接着放 `w04-threadpool-completablefuture/day2/`

线程池解决的是「在哪跑」，`CompletableFuture` 解决的是「跑完之后怎么接」。
今天已经埋了两个伏笔，明天正好接上：`submit` 的异常躺在 `Future` 里没人 get，
以及「线程池会复用线程 → `ThreadLocal` 串味」（这就是后面 `@Async` 丢上下文的同一个病根）。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 1：任务四步顺序 / 为什么无界队列让 max 失效 / `submit` 与 `execute` 的异常差别 | 口头过 |
| 15 分钟 | `supplyAsync` / `runAsync`：**默认线程池是谁**（`ForkJoinPool.commonPool()`，几个线程？），阻塞任务塞进去会怎样 | `day2/AsyncBasics.java` |
| 20 分钟 | **`thenApply` vs `thenCompose`**：嵌套 `CompletableFuture<CompletableFuture<T>>` 长什么样，`thenCombine` / `allOf` 怎么等所有 | `day2/ChainShape.java` |
| 10 分钟 | 异常链三种写法：`exceptionally` / `handle` / `whenComplete`，各自在哪一层生效、能不能吞掉异常 | `day2/AsyncErrors.java` |
| 5 分钟 | 输出 `day2-result.txt` + 笔记 W04 Day 2 小节 + 勾掉「CompletableFuture」 | 笔记 + 快照 |

> 优先做第二个 20 分钟。`thenApply` / `thenCompose` 的区别**必须靠「把类型打出来」才讲得清** ——
> 前者返回 `CompletableFuture<CompletableFuture<String>>`，后者返回 `CompletableFuture<String>`，
> 而嵌套的那个在编译期是合法的，所以永远不会报错，只会在 `join()` 时拿到一个 Future 对象。
> 这跟 W02 的 `map` vs `flatMap` 是同一个形状，可以顺手串起来。
