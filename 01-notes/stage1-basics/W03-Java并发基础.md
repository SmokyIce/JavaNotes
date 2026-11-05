# W03 · Java 并发基础

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟨 进行中（Day 2 已完成 2026-11-05）　｜　开始日期：2026-11-04　｜　完成日期：____-__-__

> 💡 重点理解：为什么会有线程安全问题？

---

## 一、本周知识点

- [x] Thread · Runnable
- [x] synchronized
- [ ] volatile
- [ ] Lock
- [x] 为什么会有线程安全问题

> Day 1 勾了 `Thread · Runnable` 和「为什么会有线程安全问题」。
> Day 2 勾 `synchronized`：现在能脱稿答「锁的是谁」（this / Class / 锁对象本身）+
> 能说清死锁怎么发现、怎么确认。`volatile` 仍不勾 —— 它救标志位那一面撞过了，
> 但 happens-before 八条、DCL 为什么必须 volatile 还没单独过。
> `Lock` 也不勾：只用了 `ReentrantLock.tryLock` 和可重入计数，AQS 一行没碰。

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
Day 2（已完成 11-05）  锁的是谁：实例方法锁 this / 静态方法锁 Class / new Object() 等于没锁
                       + 可重入 + 死锁（ThreadMXBean 检测 + jstack 现场）+ 活锁与全局定序
                       + 偏向锁在 JDK 17 / 25 的真实状态
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

### Day 2 · 2026-11-05（周四）· `synchronized` 到底锁了谁

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 1：`count++` 的窗口 / 为什么 2 万次复现不了 / volatile 管哪件事 | 口头过 |
| 20 分钟 | **锁对象三连**：实例方法 → `this`；静态方法 → `Class`；`synchronized(new Object())` → 等于没锁 | `day2/LockTarget.java` A~C 段 |
| 15 分钟 | **死锁复现 + 检测**：交叉拿锁、`ThreadMXBean.findDeadlockedThreads()`、`jstack` 抓现场 | `day2/Deadlock.java` D1 段 + `day2-deadlock-jstack.txt` |
| 10 分钟 | 偏向锁现状核对：JDK 17 默认关、JDK 25 参数已删 | 同上 E 段 + 输出片段 |
| 5 分钟 | 输出 `day2-result.txt` + 本节笔记 + 勾掉 `synchronized` | 笔记 + 快照 |

实测快照：`w03-concurrency-basics/day2-result.txt`（116 行）
死锁现场原件：`w03-concurrency-basics/day2-deadlock-jstack.txt`（142 行）

#### 1. 三种写法，三把锁

| 写法 | 锁对象 | 实测证据（同一段代码改的三种形态） |
| :--- | :--- | :--- |
| `synchronized` 实例方法 | **`this`** | 同一实例两线程：第二个「排队 414 ms」；两个不同实例：**都排队 0 ms**（总耗时 409 ms） |
| `synchronized` 静态方法 | **`Xxx.class`** | 方法内 `holdsLock(ClassLock.class)=true`、`holdsLock(任意实例)=false` |
| `synchronized (obj)` | **`obj` 那个对象** | `synchronized(inst)` 持锁 400 ms 时，另一线程调 `inst.work()` **排队 340 ms** |

证明方式不是背结论，而是两条能直接打印的：

- `Thread.holdsLock(obj)` —— 直接问 JVM「你现在拿的是不是这个对象的锁」；
- 在方法**入口处**算 `now - 调用时刻` —— **被挡住时这个数字不是 0**。

第二条比「总耗时」可靠得多。总耗时受线程启动、JIT、调度影响，而得锁排队时间是因果直接的：
A1 段第二个线程排队 414 ms、A2 段两个都 0 ms，一眼就能分清「同一把锁」和「两把锁」。
（第一版我只打印总耗时，被自己质疑「800 ms 也可能只是启动慢」—— 后来改成打印排队时长才站得住。）

#### 2. 锁错对象的四个现场，一个比一个安静

**① 锁 `this` 的问题：外面的人也能抢这把锁。**
`Thread.holdsLock` 在 A3 段证明了 `synchronized` 方法与 `synchronized(this)` 抢的是**同一个 monitor**。
好处是省事，坏处是**任何拿到这个实例引用的代码都能跟你抢**，
而且锁粒度是「整个对象」，两个不相干的方法会互相挡。

**② 静态锁 ≠ 实例锁（这条最值钱）。**
B3 段一个线程走静态方法、一个线程锁实例，**都排队 0 ms，总耗时 412 ms** —— 完全并行。
推论：`static synchronized` 保护不了实例字段，`synchronized` 实例方法也保护不了静态字段。
**两个都在改同一个 `static Map`，各自加锁，等于没加。**

**③ `synchronized (new Object())` 等于没锁。**

```text
C1 锁 new Object()：期望 5000000，实际 2153356，丢 2846644，耗时 46 ms
C2 锁共享 final 字段：期望 5000000，实际 5000000，丢 0，耗时 339 ms
```

注意 C1 那个 **46 ms**：比真锁快 7 倍 —— 快出来的部分就是没加锁省下的。
**「加了 synchronized 反而更快」基本可以断定锁没生效**，这是个很好用的反直觉信号。

**④ 附带撞到的：锁字符串常量等于拿了一把全局锁。**
`"LOCK"` 字面量是驻留的，`LockTarget` 里的 `"LOCK"` 与另一个类 `OtherCode.LOCK` `==` 为 true
（实测打印 true），于是两个**毫不相干的类**互相阻塞：排队 354 ms。
同理危险的有 `Integer.valueOf(1)`（-128~127 走缓存）、`Boolean.TRUE`。

> 结论：锁对象选 `private final Object lock = new Object()` —— 私有、final、只用于加锁。

#### 3. 可重入：为什么「自己等自己」不会死锁

```text
[D2] 第一次进锁，holdsLock=true
[D2] 第二次进同一把锁，照样进去了 → 锁按「线程」记，不按「次」记
[D2] 递归 3 层也回来了
```

monitor 里记的是「持有线程 + 重入计数」，同线程再进就 +1，退出 -1，归零才真释放。
**所以死锁的四个必要条件里没有「线程等自己」这一条 —— 它压根不成立。**

对照 `ReentrantLock`：`holdCount=2` → unlock 一次变 1（锁没放开）→ 再 unlock 变 0。
多 unlock 一次抛 `IllegalMonitorStateException`；而 `synchronized` 的加解锁由 JVM 隐式配对，
**多不出来也少不掉**（这也是它比 Lock 不容易写出锁泄漏的原因）。

#### 4. 死锁：先发现、再确认，两步是两件事

**发现（程序里）**：`ThreadMXBean.findDeadlockedThreads()`，JVM 自己做的锁图环检测。

```text
✓ findDeadlockedThreads() 检出 2 个线程：
  - D1-A-then-B  state=BLOCKED  blockedCount=2
      在等 : java.lang.Object @0x5197848c（被 D1-B-then-A 持有）
      已持有: Object @0x20ad9418（栈深度 0，位置 Deadlock.lambda$sectionD1$0:65）
  - D1-B-then-A  state=BLOCKED  blockedCount=1
      在等 : java.lang.Object @0x20ad9418（被 D1-A-then-B 持有）
      已持有: Object @0x5197848c（栈深度 0，位置 Deadlock.lambda$sectionD1$1:76）
```

两行对着读就是环：A 等 `0x...48c`，那把在 B 手里；B 等 `0x...418`，那把在 A 手里。
`getThreadInfo(ids, true, true)` 的后两个 `true` 必须给，否则 `getLockedMonitors()` 返回空数组。

**确认（线上）**：`jstack <pid>`，找 `Found one Java-level deadlock:`。原件结构：

```text
Found one Java-level deadlock:
"D1-A-then-B":
  waiting to lock monitor 0x...b620 (object 0x...f060, a java.lang.Object),
  which is held by "D1-B-then-A"
"D1-B-then-A":
  waiting to lock monitor 0x...bfc0 (object 0x...f050, a java.lang.Object),
  which is held by "D1-A-then-B"
Java stack information for the threads listed above:
"D1-A-then-B":
	at com.newt.java48.w03.day2.Deadlock.lambda$sectionD1$0(Deadlock.java:65)
	- waiting to lock <0x...f060> (a java.lang.Object)
	- locked <0x...f050> (a java.lang.Object)
Found 1 deadlock.
```

看三样：**谁等谁**（第一段就够）、**业务代码第几行**（`Deadlock.java:65`）、
**结尾 `Found N deadlock`**（没有这行就不是死锁，是等 IO / 等信号）。

三个细节别搞错：

- **`isAlive()==true` 不等于在干活**。卡死线程一直是 BLOCKED，活着但不推进。
- **`blockedCount` 不是等待毫秒数**，是「进过几次阻塞」。
- **只有一把锁不可能死锁**，至少要两把 + 反向获取。

#### 5. 活锁：`tryLock` 不加退避会变成「两个人互相让路」

用 `tryLock(timeout)` 拿不到就放掉重试，看着是标准解法。实测 5 次：

```text
第 1 次：活锁超时，重试 8 + 8，耗时 2133 ms
第 2 次：活锁超时，重试 8 + 8，耗时 2139 ms
第 3 次：活锁超时，重试 8 + 8，耗时 2153 ms
第 4 次：活锁超时，重试 7 + 8，耗时 2152 ms
第 5 次：跑完，重试 6 + 5，耗时 1675 ms
D3a 不加退避 × 5 次（每次限时 2 秒）：活锁 4/5 次，重试合计 74 次
D3b 加随机退避：跑完？true，重试 0 + 1 次
```

**4/5 次一个都没跑完**，两线程都在跑、CPU 也在烧，就是没人推进 —— 这叫**活锁**。
原因：两边都「先拿左手，等 50 ms，再抢右手」，节拍一致 → 每次都在 50 ms 处对撞 → 同时放弃、同时重来。
加随机退避（`50 + rnd(120)` + 失败后 `rnd(100)`）就把节拍打散了，0~1 次重试就过。

**死锁 vs 活锁**：死锁是**确定的**（D1 每次必卡，`Found 1 deadlock` 稳定出现），
活锁是**概率的**（4/5，有时自己走出来）。所以活锁更难排查 —— 没有阻塞栈可看，CPU 还是高的。

> 结论：`tryLock` 的正确用法是 **① 拿不到第二把就把第一把还回去；② 还回去之后要退避，不是立刻重来。**

#### 6. 最干脆的解法：全局定序

```text
D3c 固定顺序：跑完？true，期望 4000，实际 4000，耗时 1 ms，零重试
```

两个线程都按 `identityHashCode` 排序后再加锁 → 加锁顺序一致 → **「循环等待」从一开始就不成立**。
2000 轮 × 2 线程，结果精确 4000，1 ms，零重试。

三招按优先级：**定序（无开销）> 超时放弃（有重试）> 缩小粒度/不在锁里做 IO**。

#### 7. 偏向锁的现状：别背 JDK 8 的八股

```text
$ java -XX:+PrintFlagsFinal -version | grep -i biased        # JDK 17.0.12
     intx BiasedLockingBulkRebiasThreshold  = 20    {product} {default}
     intx BiasedLockingBulkRevokeThreshold  = 40    {product} {default}
     intx BiasedLockingDecayTime            = 25000 {product} {default}
     intx BiasedLockingStartupDelay         = 0     {product} {default}
     bool UseBiasedLocking                  = false {product} {default}   ← 默认关

$ java -XX:+UseBiasedLocking -version                        # JDK 17：能开，但只剩一句警告
Java HotSpot(TM) 64-Bit Server VM warning: Option UseBiasedLocking was deprecated
in version 15.0 and will likely be removed in a future release.

$ jdk-25/bin/java -XX:+UseBiasedLocking -version             # JDK 25：参数已删除
Unrecognized VM option 'UseBiasedLocking'
Error: Could not create the Java Virtual Machine.
```

三档证据：**JDK 17 默认 false（JEP 374）→ 显式开启只剩弃用警告 → JDK 25 直接认不出这个参数。**
所以面试再答「升级过程是无锁→偏向→轻量→重量」时，要补一句「JDK 15 起偏向锁默认关闭，
现在这套升级只剩下轻量→重量」。

#### 8. 遗留

- ⬜ `volatile` 仍不勾：happens-before 八条、DCL 为什么必须 volatile 还没单独过。
- ⬜ `Lock` / AQS 只碰了 `tryLock` 与 `holdCount`，`Condition`、公平锁、`AQS` 的 state/CLH 队列留给 Day 3。
- ⬜ 对象头 Mark Word 的结构（锁标志位、GC 分代年龄）没有实测 —— 要读得靠 `Unsafe` 或 JOL，
  今天没引第三方包，只用了 `PrintFlagsFinal` 这一层证据。
- ⬜ `wait` / `notify` / `interrupt` 还没碰。
- ⬜ Day 1 遗留的「C2 耗时含预热」仍未做成正规基准。

## 四、踩坑与 Bug 复盘

| 现象 | 原因 | 排查过程 | 解决 | 底层原理 |
| :--- | :--- | :--- | :--- | :--- |
| B1 段（每线程 2 万次）跑 10 轮，前 4 轮丢 80%+、中间 3 轮又是整齐的 200000、第 8~10 轮再丢 1 万~4 万，差点当成「锁生效了」 | JIT 预热后 2 万次自增只要几十微秒，而唤醒 10 个线程本身要上百微秒，第一个跑完第九个还没被调度 —— 没重叠就没有竞态 | 看每轮数字的形状（丢 → 不丢 → 又小额丢）；同一份代码三次运行分别丢 4/10、5/10、7/10，连丢几轮都不稳定；总耗时只有 30~50 ms（10 轮！） | 同一份代码加量到每线程 50 万次，立刻 10/10 轮丢 | 竞态能否观测取决于「单次工作量 / 线程调度开销」这个比值；预热会把这个比值推反，让「不丢」变成常态 |
| 写笔记时先写「方法级 synchronized 会编译出 monitorenter / monitorexit」，`javap` 打出来一条都没有 | 方法级同步是 **ACC_SYNCHRONIZED 标志**，JVM 在方法调用入口隐式加解锁；只有同步块才发 `monitorenter` 指令 | 对 `incSync()` 跑 `javap -p -c`，四条指令全是算术，再 `javap -v` 看 flags 才发现 `0x0022 ACC_SYNCHRONIZED` | 补了一个 `incSyncBlock()` 用同步块写法，两种写法各跑 5 轮，字节码并列对比 | 同一个语义有两种实现层次：方法级靠方法标志（无指令），块级靠字节码指令 + 异常表兜底 |
| 可见性实验第一版：普通 `boolean` 只自旋 **0** 次就「看见」了新值，看着像没问题 | 主线程 `start()` 之后直接 `sleep(20)`，20 ms 内 reader 线程还没被调度，等它进循环时标志位早就是 true 了 | 输出里 `自旋 0 次` 自相矛盾 —— 没进循环怎么算「看见」 | 加一个 `CountDownLatch inLoop` 握手：reader 进循环前 `countDown()`，主线程 `await()` 后再等 50 ms 才置位 | 并发实验的**准备动作本身**也会被调度影响，测试脚手架不严，结论全废 |
| 加上握手之后，第二版 `volatile` 那条也「始终没看见」，差点得出「volatile 没用」的荒谬结论 | 自旋上限设成 3 亿次，实测 3 亿次只要 200 ms，而主线程要等 200 ms 才置位 —— reader 撞上限退出时标志位还没写 | 两行输出都是「撞上限」，耗时 201 / 214 ms，跟等待时间几乎相等，明显是「跑完了才被置位」 | 上限提到 20 亿次、等待降到 50 ms，普通版撞上限（384 ms 未看见）、volatile 版 1.98 亿次后看见 | 自旋上限必须远大于等待时间，否则测的是「谁先到终点」，不是可见性 |
| `start()` 调第二次抛 `IllegalThreadStateException`，想打点日志发现 message 是 null | `Thread.start0()` 里就一句 `throw new IllegalThreadStateException()`，没带任何信息 | `e.toString()` 只有类名；`getMessage()` 为 null | 结论化进笔记：这类异常只能靠上下文推断 | 「异常没 message」不一定是 JDK 偷懒，也可能是「它认为调用方本该知道」（同一个线程不能起两次） |
| `RaceCondition` 第一版直接 `for` 循环起 10 个线程，竞态复现率极不稳定 | 线程是一个个 `start()` 的，第一个可能已经跑完第十个才起来，天然错开 | 把「每轮有没有丢」打出来，发现有的轮次整齐 200000 | 全部卡在 `CountDownLatch(1)` 上，`countDown()` 一起放行 | 复现竞态的第二件必需品：**同时起跑**。第一件是「工作量够长」（见上面第一条） |
| Day 2 的 D2 段（可重入）整段一行输出都没有，`join(2000)` 直接超时 | **复用了 D1 的锁对象 A** —— 那时 A 已经被死锁线程永久占着，D2 一进去就 BLOCKED | 输出里 D2 标题在、内容全无，且没有异常栈；反查发现 `synchronized (A)` 的 A 就是 D1 那两把 | 给 D2 换一个全新的 `final Object me = new Object()` | 死锁的线程不会自己松手；**同一份 demo 里段的顺序会互相污染状态**，每段要自带干净的锁 |
| `jstack` 报 `17592: no such process`，但进程明明在 | ① 抓晚了：`hold` 只挂 20 秒，两次工具调用之间的间隔就超了；② 更坑的是 Git Bash 里 `$!` 拿到的是 **MSYS 的 pid**，跟 Windows 原生 pid 不是一个数 | 第二次比对：`$!`=1315，程序自己打印的 `ProcessHandle.current().pid()`=33708 | 单条命令里完成「起进程 → 等 4 秒 → 读程序打印的 PID → jstack」 | Windows 上跨 shell 的 pid 不是同一个 ID 空间；`jstack` 只认原生 pid |
| `tryLock` 那版第一轮跑出「5 秒内跑完？false，重试 18/18」，以为 demo 写错了 | 不是写错，是**活锁**：两个线程节拍完全一致，每次都在「等第二把锁」处对撞，然后同时放弃同时重来 | 重试次数 18 说明循环在疯狂空转而不是卡住；没有 BLOCKED、CPU 还在烧 → 排除死锁 | ① 加随机退避打散节拍；② 单次改成 5 轮统计（4/5 活锁）；③ 保留「不加退避 vs 加退避」的对照 | 活锁的判据是「线程在跑但没推进」，靠 CPU + 无阻塞栈排除死锁；**它和死锁的排查路径完全不同** |
| D3a 第一版写死注释「不加退避一定活锁」，实跑却 3 次就成功了 | 活锁是**概率事件**，不是必然；线程启动有先后，节拍本来就会漂 | 连跑两次：一次 0/5 活锁、一次 3/5 活锁 | 改成「跑 5 次统计比例」并加上 `CountDownLatch` 同时起跑提高对称性 | 同 Day 1 的教训：**偶发不能写成必然**；能证明「会活锁」的只有统计，不是单次输出 |

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
- **`synchronized` 锁的是什么？**（实例方法锁 `this`、静态方法锁 `Class` 对象、代码块锁括号里的对象 ——
  Day 2 用 `Thread.holdsLock` + 排队时长三条都验证过。别再答「对象头 Mark Word」了，那是**存储位置**不是**锁对象**）
- 两个实例的 `synchronized` 方法会互斥吗？（不会，实测并行，总耗时 409 ms ≈ 单个 400 ms）
- `static synchronized` 和 `synchronized` 实例方法能互斥吗？（不能，两把锁，实测 412 ms 并行）
- `synchronized (new Object())` 有什么问题？（每次一把新锁 = 没锁；实测丢 2846644/5000000，而且**比真锁快 7 倍**）
- 锁一个字符串常量会怎样？（`"LOCK"` 是驻留的，等于全局锁，实测跨类互相阻塞 354 ms）
- `synchronized` 可重入吗？怎么实现的？（monitor 持有计数；所以「自己等自己」不会死锁）
- 死锁怎么排查？（先说 `ThreadMXBean.findDeadlockedThreads()` 程序化检测，再说 `jstack` 看
  `Found one Java-level deadlock` 的「waiting to lock / which is held by」成对结构）
- 死锁的四个条件？破了哪个？（互斥 / 持有并等待 / 不可剥夺 / 循环等待；工程上通常破**循环等待** —— 全局定序）
- `tryLock` 能解决死锁吗？（能破「无限等待」，但不加退避会**活锁**；实测 5 次里 4 次 2 秒内没跑完）
- 偏向锁现在还有吗？（JDK 15 起默认关闭，JDK 17 `UseBiasedLocking=false`，JDK 25 参数已删除）

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 2026-11-04 | 三 | Day 1 线程基础：三种起线程方式 + run/start 陷阱 + 竞态复现（2 万 vs 50 万对照）+ volatile/synchronized/Atomic 三条修法 + 可见性对照 + javap 字节码 | 60 min | `StartThread` / `RaceCondition` / `day1-result.txt` / `day1-bytecode.txt` | ✅ |
| 2026-11-05 | 四 | Day 2 锁的是谁：实例锁 this / 静态锁 Class / `new Object()` 没锁 + 可重入 + 死锁（ThreadMXBean + jstack）+ 活锁 4/5 + 全局定序 + 偏向锁三档证据 | 60 min | `LockTarget` / `Deadlock` / `day2-result.txt` / `day2-deadlock-jstack.txt` | ✅ |
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

---

**Q · `synchronized` 锁的是谁？**

三句话答完，每句都能给出实测证据：

1. **实例方法 → `this`**。同一实例两线程互斥（第二个排队 414 ms），两个实例完全并行（都 0 ms）。
   证据：`Thread.holdsLock(this)` 在方法里恒为 true，而 `holdsLock(另一个实例)` 恒为 false。
2. **静态方法 → `Xxx.class`**。与实例锁是两把锁（静态 vs 实例实测 412 ms 并行）。
   证据：静态方法里 `holdsLock(Xxx.class)=true`、`holdsLock(任意实例)=false`；
   先 `synchronized(Xxx.class)` 占住 400 ms，再调静态方法会排队 351 ms。
3. **代码块 → 括号里那个对象**，所以 `synchronized (new Object())` 每次都是一把新锁，
   10 线程 50 万次实测丢 2846644 / 5000000 —— **而且比真锁快 7 倍**（46 ms vs 339 ms）。

补一句区分概念：对象头 Mark Word 是**锁状态存在哪**，锁对象是**锁住谁**，两个问题别混答。

**Q · 死锁怎么排查？**

分两步，**发现**和**确认**是两件事：

- **发现**：`ThreadMXBean.findDeadlockedThreads()`，JVM 自己做锁图环检测，返回死锁线程 id；
  用 `getThreadInfo(ids, true, true)` 拿细节（后两个 `true` 不能省，否则 `getLockedMonitors()` 是空的）。
- **确认**：`jstack <pid>` 找 `Found one Java-level deadlock:`，看三样 ——
  谁等谁（`waiting to lock ... which is held by ...` 成对出现）、业务代码第几行（`Xxx.java:65`）、
  末尾有没有 `Found N deadlock`（没有就不是死锁，是等 IO/等信号）。

三个容易答错的点：`isAlive()==true` 不代表在干活；`blockedCount` 不是毫秒数；
**只有一把锁不可能死锁**，至少两把 + 反向获取。

破法按优先级：**全局定序**（实测 2000 轮零重试、1 ms）> `tryLock` 超时放弃（但有活锁风险，
不加退避实测 5 次里 4 次跑不完）> 缩小粒度 / 不在锁里做 IO。
