---
title: 今天做什么
date: 2026-11-06
weekday: 周五
milestone: M1 · Java 地基
task: W03 Day 3 — volatile 的内存语义 + happens-before + DCL
status: 已完成
---

# 今天 · 2026-11-06（周五）· 已完成

> 昨天证明了「三种写法三把锁」，但 `volatile` 那一格还没勾 —— 只知道它「救得了标志位」，
> 说不出「为什么救得了」。
> 今天把 happens-before 讲到能实测：**每条规则配一个「去掉就错」的对照**。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w03（11 文件）+ 跑 day1/day2 四个入口 | ✅ 全部 exit=0（`StartThread` 30 行、`RaceCondition` 106 行、`LockTarget` 40 行、`Deadlock` 48 行），**没有中断的代码** |
| `01-notes` 里的未勾项 | W03 只剩 `volatile` / `Lock` 两个；其余 W04 以后都是未解锁里程碑，无孤儿待办 |
| `02-code` 空目录 | 只有未开工的 `.gitkeep`（W04 起），按口径不算遗留 |
| Day 1 那条「C2 耗时含预热」 | 仍未做成正规基准，继续留到 W04 线程池那节 |

**结论：今天没有需要补的中断代码。**（补漏是这套流程的第一步，今天空手而归本身就是好消息）

## 二、今天的正题：happens-before 与 DCL

代码在 `02-code/stage1-basics/w03-concurrency-basics/src/main/java/com/newt/java48/w03/day3/`：

| 文件 | 内容 |
| :--- | :--- |
| `HappensBefore.java` | A 程序次序（Dekker litmus，自旋 gate 同步）/ B monitor 传可见性 / C volatile 发布订阅（普通 vs volatile 对照）/ D start-join / E `set()` vs `lazySet()` 的屏障代价 |
| `DclSingleton.java` | D1 无 volatile 压力测试 / D2 有 volatile 对照 / D3 手工写出重排顺序 / D4 `Unsafe.allocateInstance` |
| `day3-result.txt` | 全部实测（86 行） |
| `day3-bytecode.txt` | 字段 flags、两个 DCL 的字节码对比、`lock` 前缀原文（170 行） |

## 三、今天挖到的七个点

1. **happens-before 是偏序，不是时钟。**
   `A hb B` 说的是「A 的结果对 B 可见 + A 不许被挪到 B 之后」，**不说**「A 先执行」。
   所以两个没有 HB 边的操作，顺序完全不确定 —— 这才是「本地跑不出来」的物理原因。

2. **Dekker 实测：`(0,0)` 占 16%~40%。**
   T1 `x=1; r1=y`、T2 `y=1; r2=x`，10 万轮三次运行分别出现 16239 / 28465 / 40629 次。
   **「先写后读」在内存看来能被换序** —— x86 的 TSO 只禁止 LoadLoad / LoadStore / StoreStore，
   唯独放行 StoreLoad。程序次序规则只对单线程负责（as-if-serial）。

3. **`synchronized` 也管可见性，不只管互斥。**
   锁外写的普通字段 `data`，读线程在锁内看到 `flag` 后读到 `42`，且**没被 JIT 优化掉**。
   同一个自旋循环脱掉锁 → 15 亿次也看不见（C1 5 轮全部撞满上限）。
   **monitor 是一道屏障，JIT 不敢把字段读提到循环外。**

4. **volatile 发布/订阅：80 ms 后 5/5 看见，普通标志 5 轮 0 次。**
   差别只在「看见标志之后能读到什么」—— volatile 写之前的写全部可见。
   这就是 DCL 敢靠 volatile 的理论依据。

5. **DCL 递给别人的是「停在半路」的对象，不是「全是默认值」。**
   手工写出重排顺序后抓到 `Singleton{a=1, b=null, c=0}` —— `a` 已赋值、`b` 还是 null。
   后果是 NPE 出现在离构造十万八千里的地方，**排查方向天然是错的**。
   `Unsafe.allocateInstance` 补了另一半：不跑构造也能造对象，字段全零值。

6. **这个 bug 在 x86 上测不出来 —— 而这正是它最坑的地方。**
   50 轮 × 6 线程 × 每轮抢 30 ms，不加 volatile 那版失败 **0 次**。
   x86 是 TSO，不重排 store-store。但 JMM 允许，换 ARM 就炸。
   **判断依据只能是 JMM，不能是「我跑了 50 轮没事」。**

7. **volatile 的成本：字节码一字不差，机器码多一条 `lock`。**
   `plain` flags `0x000a` / `safe` flags `0x004a`（多 `0x0040` = `ACC_VOLATILE`），
   两个 `getInstance()` 的指令序列完全一样。
   成本在 `lock addl $0x0,-0x40(%rsp)`，实测 `set()` 比 `lazySet()` 慢 **5.8 倍**（0.775 vs 0.133 ns/次）。

## 四、今天踩的坑（六个，比结论更值钱）

- **Dekker 第一版 2 万轮 (0,0) 零次。** 用 `CyclicBarrier` 每轮同步，释放有先后 ——
  分布 9468 / 10531 一边倒，说明两个线程根本没重叠，测的是「谁先跑完」。
  换成**自旋 gate**（`volatile int` 放行 + `AtomicInteger` 收工）后立刻 16%~40%。
  **同步工具本身会改变时序**，还得加一个「真的重叠了吗」的探针（`(1,1)` 计数）。
- **B2/C1 第一版「去掉锁也看得见」。** 循环体里放了 `System.nanoTime()` 当超时判据 ——
  方法调用挡住 JIT 提升；而且写线程立刻置位，循环还没编译就已经结束。
  改成纯自旋 + 写线程先睡 80 ms 才复现。
- **C2（volatile）也「看不见」。** 自旋上限 5 亿次 ≈ 170 ms，比 200 ms 的等待还短 ——
  读者先撞上限退出了。**这条 Day 1 就记过，今天又踩一次**，该进「开工前必读」。
- **E 段普通写 0.019 ns/次。** JIT 把整条循环消了，只剩最后一次 store。
  数字小到物理上不可能 —— 本周第二次栽在「接近 0 就是没测到」。
- **D3 同一段输出自相矛盾**（`toString` 显示 `b=null`，`incomplete()` 却是 false）：
  两次读不是同一个瞬间。改成先抓快照再判断。**并发下的「打印」不是原子的。**
- **以为没装 hsdis。** 想当然。`PrintAssembly` 能跑，只是不带助记符、只给十六进制。
  改成「贴原始字节 + 自己解码」并标明来源 —— 工具不给助记符也照样能取证。

## 五、环境备忘

- 跑今天：`./run.sh day3.HappensBefore`（约 8 秒）/ `./run.sh day3.DclSingleton`（约 6 秒）
- `PrintAssembly` 用法（不需要额外插件，但只有十六进制）：
  `java -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly -cp out <类名>`
- 快照与字节码文件都是**一条命令整体生成**的，不是手抄

---

# 下一个时间盒（60 分钟）

**W03 Day 4：`Lock` / AQS / `wait-notify`** —— 把 W03 剩下的两个坑一次填掉

`ReentrantLock` 已经用过（`tryLock`、`holdCount`），但 `AQS` 的 state / CLH 队列一行没碰；
`wait` / `notify` 更是完全没写。这两块是同一条线上的东西：**AQS 就是 `wait-notify` 的工程化版本**。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 3：happens-before 四条各怎么证明 / DCL 为什么在 x86 上测不出来 | 口头过 |
| 15 分钟 | `wait` / `notify`：为什么必须写在 `synchronized` 里、`if` 与 `while` 的区别（虚假唤醒）、`notify` 与 `notifyAll` 丢信号 | `day4/WaitNotify.java` |
| 20 分钟 | **手写一个简易 AQS**：`state` + `CLH` 队列 + `tryAcquire`/`tryRelease`，用 `LockSupport.park` 阻塞；再跑 10 线程 × 50 万次对拍 | `day4/MiniAqs.java` |
| 10 分钟 | `ReentrantLock` 公平 vs 非公平的实测差异（吞吐 + 排队方差）、`Condition` 的 `await/signal` 对照 `wait/notify` | `day4/LockCompare.java` + 统计 |
| 5 分钟 | 输出 `day4-result.txt` + 笔记 Day 4 小节 + 勾掉 `Lock` | 笔记 + 快照 |

> 优先做第二个 20 分钟。**AQS 是「背了模板也讲不清」的典型**：多数人能背 `state` + 双向队列，
> 但说不清「为什么入队要 CAS 两次」「为什么 `park` 之后还要再检查一次」。
> 手写一遍 30 行的版本，这两问自然就有答案了。
>
> 顺带说明：`Condition` 如果时间不够可以砍到 Day 5，但 `wait/notify` 的 `while` 循环不能砍 ——
> 虚假唤醒是线上面试题的高频追问，也是生产者-消费者写错的头号原因。
