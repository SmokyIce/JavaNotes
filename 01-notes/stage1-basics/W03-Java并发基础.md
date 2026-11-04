# W03 · Java 并发基础

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟨 进行中（Day 1 已完成 2026-11-04）　｜　开始日期：2026-11-04　｜　完成日期：____-__-__

> 💡 重点理解：为什么会有线程安全问题？

---

## 一、本周知识点

- [x] Thread · Runnable
- [ ] synchronized
- [ ] volatile
- [ ] Lock
- [x] 为什么会有线程安全问题

> Day 1 只勾了两个：`Thread · Runnable` 和「为什么会有线程安全问题」。
> `synchronized` / `volatile` 今天各撞了一次，但还没到能脱稿讲清锁升级和内存语义的程度，**先不勾**。

## 二、核心问题（能用自己的话脱稿讲出来，才算学会）

1.
2.
3.

## 三、代码 / 实验

- 代码目录：`02-code/stage1-basics/w03-concurrency-basics/`
- 本周动手内容：

```text
Day 1（已完成 11-04）  线程基础：三种起线程方式 + run/start 陷阱 + 竞态复现（10 线程 × 50 万次）
                       + volatile / synchronized / AtomicInteger 三条修法各撞一次 + 可见性对照
```

### Day 1 · 2026-11-04（周三）· 线程基础：把「丢更新」撞出来

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 W02：擦除留下什么 / `finally` 里的 return / `count()` 为什么跳过 `peek` | 口头过 |
| 15 分钟 | 建 `w03-concurrency-basics/` 工程；`Thread` / `Runnable` / `Callable` 三种起线程方式各跑一次 | `day1/StartThread.java` A~C 段 |
| 20 分钟 | **竞态复现**：`count++` × 10 线程跑十轮统计丢几次；再用 `javap -c` 看 `iadd` 与 `putfield` 之间的窗口 | `day1/RaceCondition.java` B 段 + `day1-bytecode.txt` |
| 10 分钟 | 三条修法各撞一次：`volatile`（不能）/ `synchronized`（能）/ `AtomicInteger`（能） | 同上 C 段 |
| 5 分钟 | 输出 `day1-result.txt` + 本节笔记 + 勾掉两个知识点 | 笔记 + 快照 |

实测快照：`w03-concurrency-basics/day1-result.txt`（JDK 17.0.12 / 12 核）
字节码证据：`w03-concurrency-basics/day1-bytecode.txt`

#### 1. 三种起线程方式，差别只在一件事：有没有返回值

| 方式 | 签名 | 能返回结果 | 能抛受检异常 | 备注 |
| :--- | :--- | :--: | :--: | :--- |
| `extends Thread` | `run(): void` | ❌ | ❌ | 任务和线程绑死，Java 单继承 → 不推荐 |
| `implements Runnable` | `run(): void` | ❌ | ❌ | 任务与线程解耦，**lambda 直接能塞**（W02 的函数式接口） |
| `implements Callable<V>` | `call(): V` | ✅ | ✅ | 必须配 `FutureTask` 或线程池，否则没有线程去跑它 |

`Callable` 那条有个坑值得单独记：**`call()` 里抛的异常不会直接冒泡**。实测输出是

```text
抓到 java.util.concurrent.ExecutionException: java.lang.IllegalStateException: call() 里抛的异常会被包进 ExecutionException
真正的异常在 e.getCause() = java.lang.IllegalStateException: ...
```

`future.get()` 永远只抛四类：`InterruptedException` / `ExecutionException` / `CancellationException` / `TimeoutException`。
业务异常被塞进 `ExecutionException` 的 cause 里 —— 写 `catch (ExecutionException e)` 之后**必须再 `e.getCause()`**，
否则日志里只剩一句 `ExecutionException`，跟丢了没区别。

#### 2. `run()` 不是 `start()`：一个在 main 里跑

```java
Thread t = new ByExtends("should-be-worker");
t.run();     // 打印出来线程名是 main，id 是 1
```

`run()` 只是个普通方法调用，**当前线程是谁就在谁身上执行**。演示里那句「线程名=main」就是全部证据，
不需要看源码。同类坑：`start()` 调第二次抛 `IllegalThreadStateException`，**而且这个异常没有 message** ——
又一个「异常什么都没说」的现场（W02 的 `toMap` 空 message NPE 是同一个家族）。

#### 3. `count++` 是四条指令，窗口在 `iadd` 与 `putfield` 之间

`javap -p -c` 打出来的 `incPlain()`：

```text
0: aload_0
1: dup
2: getfield   #267   // Field plain:I      ← 读
5: iconst_1
6: iadd                                  ← 加（在操作数栈上）
7: putfield   #267   // Field plain:I      ← 写
10: return
```

三步：**读 → 加 → 写**。两个线程都停在 `getfield` 拿到同一个旧值 100，各自加 1 得到 101，
各自 `putfield` 写回 101 —— 两次自增，结果只多 1。这就是「丢更新」，一次都不报错。

关键是这一步**不原子**，而这四行字节码在任何一本并发书上都会被跳过。**能指着 `putfield` 说「窗口就在这儿」，
和只会背「count++ 不是原子操作」，是两种水平。**

#### 4. 为什么「本地跑十遍都是对的」：2 万次太快

这是今天最反直觉的实测结果。同一份代码，只改每线程的次数：

| 配置 | 结果 |
| :--- | :--- |
| 每线程 **2 万**次 × 10 线程 × 10 轮 | **7/10 轮丢**，最惨 32265 / 200000（丢 83.9%）；**中间第 5~7 轮一次不丢**，第 8~10 轮又开始丢但只丢 1 万~4 万 |
| 每线程 **50 万**次 × 10 线程 × 10 轮 | **10/10 轮丢**，最低 930367 / 5000000 |

注意第一行的形状：不是「一直丢」也不是「一直不丢」，而是 **丢 → 不丢 → 又丢（丢得少）**。
同一份代码三次运行分别丢 4/10、5/10、7/10 —— **连丢几轮都不稳定**，这本身就是最好的证据：
竞态的可观测性是个概率问题，不是布尔值。

原因是 **JIT 预热改变了时间尺度**：

- 前几轮还在解释执行，2 万次自增要毫秒级，10 个线程有充足时间重叠 → 丢得很难看（83.9%）；
- 预热后 2 万次只要几十微秒，而唤醒 10 个线程本身要上百微秒 —— 第一个线程跑完，第九个还没被调度，
  **压根没重叠**，于是中间几轮「一轮不丢」；
- 再往后调度偶尔又对上（10 次里擦肩 1 次也够丢），于是第 8~10 轮又出现小额丢失（20000 / 9424 / 40000）。

所以复现竞态需要两个条件，缺一不可：

1. **工作量足够长**（50 万次那种量级），单线程跑不完；
2. **同一刻起跑** —— 用 `CountDownLatch(1)` 让所有线程卡在门口，`countDown()` 一起放行。
   直接 `for` 循环 `new Thread().start()` 的话，线程是一个个起来的，天然错开。

> 「跑不出来」不等于「没问题」。反过来也成立：**跑到 100% 丢更新，也不能证明「加锁就一定没 bug」**，
> 只能证明这把锁在这条路上生效了。竞态的可观测性从来不是证据。

#### 5. `volatile` 救不了自增，它救的是「一个写多个读」

| 写法 | 10 轮结果 |
| :--- | :--- |
| `private volatile int vol; vol++` | **10/10 轮丢**，最低 1676548（丢 66.5%） |
| 普通 `boolean` 标志位，另一线程自旋等 | 20 亿次自旋**始终没看见**新值（撞上限退出，384 ms） |
| `volatile boolean` 标志位，同样自旋 | **1.98 亿次后看见**（51 ms 退出） |

同一张表里两个相反结论，正好把 volatile 的边界划清楚：

- **原子性**：`vol++` 仍然是「读-加-写」三步，volatile 只是让每一步都去主存取最新值，
  **读和写之间照样能被插队** → 丢更新照旧（10/10）。
- **可见性**：一个线程写、另一个线程只读标志位，volatile 保证写完了对方能看见 → 普通 `boolean` 那条永远不会退出。

字节码层面更能说明问题：`incVolatile()` 和 `incPlain()` 的指令**一模一样**（`getfield` / `iconst_1` / `iadd` / `putfield`），
差别只在字段的访问标志：

```text
private int plain;              flags: (0x0002) ACC_PRIVATE
private volatile int vol;       flags: (0x0042) ACC_PRIVATE, ACC_VOLATILE
```

**volatile 不是多一条指令，是给 `getfield` / `putfield` 加了内存语义**（读时清 invalidate queue、
写时 store-load 屏障，x86 上主要靠 `lock` 前缀）。所以「volatile 慢一点」这个感觉是对的，
但它慢在内存屏障，不慢在指令条数。

#### 6. `synchronized`：方法级压根没有 `monitorenter`

我以为方法级 `synchronized` 会编译出 `monitorenter` / `monitorexit`。`javap` 打脸了：

```text
private synchronized void incSync();
  flags: (0x0022) ACC_PRIVATE, ACC_SYNCHRONIZED
  Code:
     0: aload_0
     1: dup
     2: getfield   // Field sync:I
     5: iconst_1
     6: iadd
     7: putfield
    10: return
```

**一条加锁指令都没有**，锁是靠方法 flags 里的 `ACC_SYNCHRONIZED` 让 JVM 在方法调用入口自动加解锁的。
只有**同步块**才会出现真指令：

```text
3: monitorenter
...
15: monitorexit
16: goto 24
19: astore_2        ← 异常路径
20: aload_1
21: monitorexit
22: aload_2
23: athrow
```

注意那段 `astore_2` / `athrow`：**同步块里的异常也会先 `monitorexit` 再抛**（对应 Exception table 里两条 `any`），
这就是「synchronized 不会因为异常把锁漏掉」的字节码依据。

两种写法的实测结果都是 0/10 轮丢（各 5 轮，全部 5000000）。语义一样，代价也一样 ——
50 万 × 10 线程 × 5 轮，方法级 1890 ms、同步块 1872 ms（含预热，只作量级参考）。

#### 7. `AtomicInteger` 与它的边界

`incrementAndGet()` 是 CAS 循环：读旧值 → 算新值 → `compareAndSwap` 比对再写，失败就重来。
10/10 轮全部 5000000，且**不需要显式加锁**。

边界记一句：**CAS 保住的是「这一个变量」**。两个字段要一起变（典型：转账扣款+入账、
库存扣减+版本号），单靠 Atomic 系列做不到，还是得锁或 `AtomicStampedReference`。

#### 8. 遗留

- ⬜ `synchronized` / `volatile` 的知识点还没勾 —— 今天只撞了现象，锁升级（偏向 → 轻量 → 重量）、
  happens-before 八条规则、双重检查锁为什么必须 volatile，都要单独一节。
- ⬜ `Lock` / `ReentrantLock` / `AQS` 完全没碰，等 Day 3 之后。
- ⬜ 线程的 `interrupt` / `wait` / `notify` / `join` 语义只用了 `join`，其余留给下一节。
- ⬜ C2 的耗时（1890 / 1872 / 1215 ms）含 JIT 预热，**不能当基准**，要对比得单独跑多轮取最小值。

## 四、踩坑与 Bug 复盘

| 现象 | 原因 | 排查过程 | 解决 | 底层原理 |
| :--- | :--- | :--- | :--- | :--- |
| B1 段（每线程 2 万次）跑 10 轮，前 4 轮丢 80%+、中间 3 轮又是整齐的 200000、第 8~10 轮再丢 1 万~4 万，差点当成「锁生效了」 | JIT 预热后 2 万次自增只要几十微秒，而唤醒 10 个线程本身要上百微秒，第一个跑完第九个还没被调度 —— 没重叠就没有竞态 | 看每轮数字的形状（丢 → 不丢 → 又小额丢）；同一份代码三次运行分别丢 4/10、5/10、7/10，连丢几轮都不稳定；总耗时只有 30~50 ms（10 轮！） | 同一份代码加量到每线程 50 万次，立刻 10/10 轮丢 | 竞态能否观测取决于「单次工作量 / 线程调度开销」这个比值；预热会把这个比值推反，让「不丢」变成常态 |
| 写笔记时先写「方法级 synchronized 会编译出 monitorenter / monitorexit」，`javap` 打出来一条都没有 | 方法级同步是 **ACC_SYNCHRONIZED 标志**，JVM 在方法调用入口隐式加解锁；只有同步块才发 `monitorenter` 指令 | 对 `incSync()` 跑 `javap -p -c`，四条指令全是算术，再 `javap -v` 看 flags 才发现 `0x0022 ACC_SYNCHRONIZED` | 补了一个 `incSyncBlock()` 用同步块写法，两种写法各跑 5 轮，字节码并列对比 | 同一个语义有两种实现层次：方法级靠方法标志（无指令），块级靠字节码指令 + 异常表兜底 |
| 可见性实验第一版：普通 `boolean` 只自旋 **0** 次就「看见」了新值，看着像没问题 | 主线程 `start()` 之后直接 `sleep(20)`，20 ms 内 reader 线程还没被调度，等它进循环时标志位早就是 true 了 | 输出里 `自旋 0 次` 自相矛盾 —— 没进循环怎么算「看见」 | 加一个 `CountDownLatch inLoop` 握手：reader 进循环前 `countDown()`，主线程 `await()` 后再等 50 ms 才置位 | 并发实验的**准备动作本身**也会被调度影响，测试脚手架不严，结论全废 |
| 加上握手之后，第二版 `volatile` 那条也「始终没看见」，差点得出「volatile 没用」的荒谬结论 | 自旋上限设成 3 亿次，实测 3 亿次只要 200 ms，而主线程要等 200 ms 才置位 —— reader 撞上限退出时标志位还没写 | 两行输出都是「撞上限」，耗时 201 / 214 ms，跟等待时间几乎相等，明显是「跑完了才被置位」 | 上限提到 20 亿次、等待降到 50 ms，普通版撞上限（384 ms 未看见）、volatile 版 1.98 亿次后看见 | 自旋上限必须远大于等待时间，否则测的是「谁先到终点」，不是可见性 |
| `start()` 调第二次抛 `IllegalThreadStateException`，想打点日志发现 message 是 null | `Thread.start0()` 里就一句 `throw new IllegalThreadStateException()`，没带任何信息 | `e.toString()` 只有类名；`getMessage()` 为 null | 结论化进笔记：这类异常只能靠上下文推断 | 「异常没 message」不一定是 JDK 偷懒，也可能是「它认为调用方本该知道」（同一个线程不能起两次） |
| `RaceCondition` 第一版直接 `for` 循环起 10 个线程，竞态复现率极不稳定 | 线程是一个个 `start()` 的，第一个可能已经跑完第十个才起来，天然错开 | 把「每轮有没有丢」打出来，发现有的轮次整齐 200000 | 全部卡在 `CountDownLatch(1)` 上，`countDown()` 一起放行 | 复现竞态的第二件必需品：**同时起跑**。第一件是「工作量够长」（见上面第一条） |

---

## 五、面试会怎么问

- 起线程有几种方式？`Runnable` 和 `Callable` 的区别？`call()` 抛的异常去哪了？（`ExecutionException.getCause()`）
- `run()` 和 `start()` 的区别？`start()` 调两次会怎样？（`IllegalThreadStateException`，无 message）
- 为什么 `count++` 线程不安全？**用字节码说一遍**（`getfield` → `iconst_1` → `iadd` → `putfield` 三步）
- `volatile` 保证什么、不保证什么？为什么它救不了 `count++` 却能救标志位？
- 为什么 `volatile` 能禁止重排序？（内存屏障；x86 上写靠 `lock` 前缀，读靠 invalidate）
- `synchronized` 修饰方法与修饰代码块，字节码上有什么区别？（`ACC_SYNCHRONIZED` vs `monitorenter`/`monitorexit`）
- 同步块里抛异常会不会漏锁？（不会，异常表把 `monitorexit` 放在异常路径上）
- `AtomicInteger` 怎么实现的？CAS 的 ABA 问题在计数器场景会不会出现？（不会，只增不减）
- 什么时候 `Atomic` 系列不够用？（多字段要整体原子、需要「先读后算再写」的组合操作）
- 线程池里的线程和 `new Thread()` 有什么本质区别？（复用 + 任务队列，等 W04）
- 守护线程是什么？JVM 什么时候退出？（只等非守护线程）
- `synchronized` 锁的是什么？（对象头 Mark Word；静态方法锁 Class 对象 —— 这条今天没实测，先记着）

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 2026-11-04 | 三 | Day 1 线程基础：三种起线程方式 + run/start 陷阱 + 竞态复现（2 万 vs 50 万对照）+ volatile/synchronized/Atomic 三条修法 + 可见性对照 + javap 字节码 | 60 min | `StartThread` / `RaceCondition` / `day1-result.txt` / `day1-bytecode.txt` | ✅ |
|  | 四 |  |  |  |  |
|  | 五 |  |  |  |  |
|  | 六 |  |  |  |  |
|  | 日 |  |  |  |  |

---

## 参考要点（先自己答，再对照）

**Q · 为什么会有线程安全问题？**

三个必要条件同时成立才会出事（缺一个都安全）：

1. **共享**：多个线程访问同一份数据（局部变量在栈上，天然不共享）；
2. **可变**：这份数据会被改（只读的常量、`final` 字段安全）；
3. **非原子**：修改不是一个不可分割的动作 —— `count++` 是「读-加-写」三步，
   中间任何一个时刻都可能被切走（`RaceCondition` 的 B1/B2 段，10 线程 50 万次，10/10 轮丢）。

三条对应三种修法，别混：

| 缺哪条 | 修法 |
| :--- | :--- |
| 不能共享 | 局部变量 / `ThreadLocal` / 每个线程一份副本 |
| 不能可变 | 不可变对象（`String`、`record`、`final`） |
| 不能非原子 | `synchronized` / `Lock` / `Atomic*`（只保一个变量） |

再补一句容易答漏的：**「非原子」之外还有「不可见」**。线程 A 改了 `flag`，线程 B 可能一直在读自己
寄存器里的旧值（C4 段：普通 `boolean` 自旋 20 亿次也没看见，换成 `volatile` 1.98 亿次就看见了）。
所以严格说是四件事：**共享 + 可变 + 非原子 + 不可见**，前三条讲的是「丢更新」，第四条讲的是「看不见」。
