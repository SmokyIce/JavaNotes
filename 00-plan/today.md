---
title: 今天做什么
date: 2026-11-11
weekday: 周三
milestone: M1 · Java 地基
task: W03 Day 4 — Lock / AQS / wait-notify（W03 封板）
status: 已完成
---

# 今天 · 2026-11-11（周三）· 已完成

> 昨天之前已经证明了两件事：锁能互斥（Day 2）、volatile 能传可见性（Day 3）。
> 但 `wait()` 为什么必须写在 `synchronized` 里、AQS 的 `state` 和那条队到底怎么转，
> 一直是「背得出名词、说不出过程」。
> 今天把 AQS 手写了一遍 —— **这两个问题只有自己写出来才答得干净。**

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w03（6 文件）+ 跑 day1/day2/day3 六个入口 | ✅ 全部 exit=0，输出行数与 11-06 一致（31 / 106 / 40 / 48 / 47 / 32），**没有回归、没有中断的代码** |
| `01-notes` 里的未勾项 | W03 只剩 `Lock` 一个 —— 正好是今天要做的事；其余是未解锁里程碑 |
| `02-code` 空目录 | 只有 `w04-` 起的 `.gitkeep`（未开工），按口径不算遗留 |
| Day 1 那条「C2 耗时含预热」 | 仍未做成正规基准。今天顺手把它变成了正经对照（`warmup()` + 三轮取最小），**这条可以销了** |

**结论：没有需要补的中断代码。** 五个知识点里最后一个是 `Lock`，今天补上就封板。

## 二、今天的正题：三个程序

代码在 `02-code/stage1-basics/w03-concurrency-basics/src/main/java/com/newt/java48/w03/day4/`：

| 文件 | 内容 |
| :--- | :--- |
| `WaitNotify.java` | A `wait()` 不在锁里（ISE 原文）/ B `if` vs `while` / C `notify` 只叫一个 / D 释放锁对照 / E 中断清标志 |
| `MiniAqs.java` | A 200 万次对拍 / B 重入 `state` / C 两种临界区长度的三锁对照 / D 两次 CAS 与 park 统计 |
| `LockCompare.java` | A 公平 vs 非公平（插队次数 + 最长等待）/ B `Condition` vs `notifyAll`（无效唤醒） |
| `day4-result.txt` | 三个程序的全部实测（129 行，一条命令整体生成） |
| `day4-bytecode.txt` | 三种「锁」在字节码层的差别（75 行） |

## 三、今天挖到的五个点

1. **`notify()` 一次只叫醒一个，而「货还在、人在睡」就是死锁的雏形。**
   放 2 件货只喊一声 → 取走 1 件、剩 1 件、1 个消费者永久 `WAITING`。
   换 `notifyAll()` 就是 2/0/0。这也解释了为什么工程里默认 `notifyAll`：
   多叫醒几个只是空转，少叫醒一个是永久卡死。

2. **`notify` 不是信号量 —— 没人在等时它直接消失。**
   空喊一嗓子之后 `wait(200)` 会等满 200 ms 才超时。
   所以「先改条件再 `wait`」一定丢；条件检查和 `wait` 必须在同一把锁里。

3. **`wait()` 释放锁，`sleep()` 抱着锁睡：0 ms vs 414 ms。**
   同是「停 500 ms」，一个把钥匙放桌上，一个锁着门睡。
   **「在 `synchronized` 里 `sleep`」是并发性能的头号杀手。**

4. **`park` 返回 ≠ 拿到锁 —— 87500 次「醒来没抢到」就是证据。**
   手写 AQS 跑 200 万次加锁：挂起 175075 次，其中 87500 次醒来发现锁已经被抢走。
   最凶的插队者不是新线程，而是**刚 `unlock` 转头又 `lock` 的那个消费线程** ——
   压测循环里它每轮都干这事。所以 `park` 必须包在 `for(;;)` 里，醒来第一件事是重试。

5. **手写版慢 5.8 倍？把临界区拉长就只剩 1.13 倍。**
   同一套脚手架，临界区 1 ns 时：500 万级对比是 118 / 50 / 687 ms；
   临界区 0.5 µs 时：84 / 75 / 95 ms。
   差额 ≈ `park 次数 × 4.2 µs` —— **慢的不是队列，是挂起**。
   这就是 `ReentrantLock` 敢拼命自旋的原因：临界区只有 1 ns 时，挂起一次等于输几万次。

## 四、今天踩的坑（五个，比结论更值钱）

- **`join()` 写在 `synchronized` 里 = 自己卡自己。** 消费者要重新拿到 `mon` 才能退出临界区，
  而我持着锁等它结束 —— 每段固定等满 1 秒超时。改成「取数 → 出临界区 → 再 join」。
- **C 段第一版把「放 2 件货」写成了初始状态。** 消费者进临界区时货已经在架上，谁都没 `wait`，
  `notify` 一次也「取走 2 件」—— 实验根本没成立。**必须先让人睡着，再发生事件。**
- **想靠概率证明 `notify` 丢信号，20 轮只丢了 2 次。** 这个 JVM 大多时候恰好挑中该醒的那个。
  换成「2 件货只喊一声」的确定性写法，才 100% 复现。**靠概率的实验不能当结论。**
- **`synchronized` 那个 22 ms 是假的。** C 段内联自增、C2 段包了 lambda，两套脚手架，
  同一份活测出 22 与 107 ms，还很自信地写了「差 15 倍」。统一脚手架 + `warmup()` 之后是 5.8 倍。
- **`Node.thread` 声明成 `final` 就没法变哨兵。** `setHead()` 要把自己变成哨兵（`thread = null`），
  编译直接不过；真实 AQS 里这个字段也是 `volatile`。

## 五、环境备忘

- 跑今天：`./run.sh day4.WaitNotify`（约 5 s）/ `day4.MiniAqs`（约 7 s）/ `day4.LockCompare`（约 3 s）
- 三个程序的输出都由一条命令整体写进 `day4-result.txt`，不是手抄
- 中途踩过 `ClassNotFoundException`：加了新的 `day4/` 之后 **`sources.txt` 是旧的**，
  必须先 `find src/main/java -name "*.java" > sources.txt` 再 `javac`

---

# 下一个时间盒（60 分钟）

**W04 Day 1：线程池** —— 从 `w04-threadpool-completablefuture/.gitkeep` 建工程

W03 到这里封板：`Thread` / `synchronized` / `volatile` / `Lock` / 「为什么会有线程安全问题」五个全勾，
`Java并发.md` 8 道题（M1 只要 8 道，**刚好达标**）。

线程池是 W03 的自然下一步：前面所有实验都是「手动 new Thread」，而工程里没人这么干。
今天要回答的是「`new Thread` 到底错在哪」和「`ThreadPoolExecutor` 那七个参数各自负责什么」。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 4：为什么入队两次 CAS / 为什么 `park` 后要重查 / `notify` 为什么默认用 `notifyAll` | 口头过 |
| 15 分钟 | 建工程 + `new Thread` vs 线程池：跑 1 万个任务，比创建开销与内存；`ThreadPoolExecutor` 七个参数各改一次看行为 | `day1/PoolBasics.java` |
| 20 分钟 | **任务在四步里怎么走**：核心线程 → 队列 → 非核心 → 拒绝。把 `corePoolSize` / `maximumPoolSize` / 队列容量三者的关系跑成一张表 | `day1/PoolFlow.java` |
| 10 分钟 | `Executors` 的四个工厂方法为什么被阿里手册禁用：`newFixedThreadPool` 的 `LinkedBlockingQueue` 无界 → OOM 实测 | `day1/PoolTraps.java` |
| 5 分钟 | 输出 `day1-result.txt` + 笔记 W04 Day 1 小节 + 勾掉「线程池」 | 笔记 + 快照 |

> 优先做第二个 20 分钟。**「任务先入队还是先开线程」是最高频的追问**，
> 而绝大多数人答成「先开线程到上限再排队」—— 正好反了：
> 核心线程满了**先入队**，队满了才开非核心，两边都满了才拒绝。
> 这个顺序不看代码是想不到的，跑一遍 `PoolFlow` 就再也忘不掉。
