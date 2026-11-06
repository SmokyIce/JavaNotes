# W03 · Java 并发基础

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟨 进行中（Day 3 已完成 2026-11-06）　｜　开始日期：2026-11-04　｜　完成日期：____-__-__

> 💡 重点理解：为什么会有线程安全问题？

---

## 一、本周知识点

- [x] Thread · Runnable
- [x] synchronized
- [x] volatile
- [ ] Lock
- [x] 为什么会有线程安全问题

> Day 1 勾了 `Thread · Runnable` 和「为什么会有线程安全问题」。
> Day 2 勾 `synchronized`：现在能脱稿答「锁的是谁」（this / Class / 锁对象本身）+
> 能说清死锁怎么发现、怎么确认。
> Day 3 勾 `volatile`：happens-before 四条实测规则（程序次序 / monitor / volatile / start-join）+
> DCL 为什么必须 volatile + 手工复现「半初始化对象」。`volatile` 这一格现在敢说「讲得清」了，
> 因为每条规则都有一个「去掉就错」的对照实验，不是一个定义。
> `Lock` 仍不勾：只用了 `ReentrantLock.tryLock` 和可重入计数，AQS 的 state / CLH 队列一行没碰。

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
Day 3（已完成 11-06）  happens-before 四条实测（程序次序 Dekker / monitor / volatile 发布订阅 / start-join）
                       + DCL 为什么必须 volatile（压力测试 0 次 + 手工重排抓到半初始化对象）
                       + Unsafe.allocateInstance + set vs lazySet 的屏障代价
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

### Day 3 · 2026-11-06（周五）· `volatile` 的内存语义 + happens-before + DCL

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 2：三种写法各锁谁 / 为什么 `static synchronized` 救不了实例字段 / 死锁看 jstack 哪三样 | 口头过 |
| 20 分钟 | **happens-before 四条能实测的**（程序次序 / monitor / volatile / start-join），每条一个「去掉就错」的对照 | `day3/HappensBefore.java` A~E 段 |
| 15 分钟 | **DCL 单例**：不加 volatile 能不能复现半初始化对象？50 轮 × 6 线程压力测试 + 手工重排复现 | `day3/DclSingleton.java` |
| 10 分钟 | `javap` 看字段 flags、`PrintAssembly` 看 StoreLoad 屏障落点 | `day3-bytecode.txt` |
| 5 分钟 | 输出 `day3-result.txt` + 本节笔记 + 勾掉 `volatile` | 笔记 + 快照 |

实测快照：`w03-concurrency-basics/day3-result.txt`（86 行）
字节码/屏障证据：`w03-concurrency-basics/day3-bytecode.txt`（170 行）

#### 1. happens-before 不是「时间上先发生」

`A happens-before B` 说的是两件事：**A 的结果对 B 可见**，**A 不会被重排到 B 之后**。
它**不说**「A 先执行」—— 只要不影响单线程的可见结果，JVM 想怎么重排都行（as-if-serial）。
所以两个没有 HB 边的操作，顺序是不确定的。

这条定义之所以重要，是因为它解释了「为什么本地跑不出来」：
没有 HB 边的地方，行为取决于 JIT 和 CPU 当天的心情，不取决于代码的字面顺序。

#### 2. 程序次序规则只对单线程负责：Dekker 实测 16%~40%

```text
线程 1：ax = 1;  ar1 = ay;
线程 2：ay = 1;  ar2 = ax;
```

按「程序次序」每个线程都是**先写后读**，那么 `(ar1, ar2)` 至少有一个该是 1。
10 万轮实测（三次运行）：

| 运行 | (0,0) | (1,1) | 只有 r1=1 | 只有 r2=1 |
| :--- | ---: | ---: | ---: | ---: |
| 第 1 次 | **16239（16.2%）** | 34 | 25279 | 58448 |
| 第 2 次 | 28465（28.5%） | 23 | 32057 | 39455 |
| 第 3 次 | 40629（40.6%） | 22 | 34548 | 24801 |

**(0,0) 的含义**：两个读都发生在对方写生效之前 —— 「先写后读」在内存看来被换成了「先读后写」。
x86 的 TSO 只禁止 LoadLoad / LoadStore / StoreStore，**唯独放行 StoreLoad**，这就是它的来源。
**程序次序规则保证的是「单线程内 as-if-serial」，跨线程什么都没保证。**

顺带注意 `(1,1)` 只有 20~30 次：这才是「两个线程都看见对方」的轮次。
一多半轮次其实是「一个跑完另一个才跑」—— 数量级差四个，说明并发窗口有多窄。

#### 3. monitor 规则：`synchronized` 也管可见性

写线程在**锁外**写普通字段 `data`、在**锁内**写普通字段 `flag`；读线程在 `synchronized` 里自旋等 `flag`。

```text
[B1 有 monitor] 看见标志？true，自旋 11065533 次，读到的 data = 42
```

`data` 不是 volatile，可见性完全是「写线程解锁 → 读线程加锁」这条 HB 边传过来的。
**这条常被忽略：很多人以为 `synchronized` 只负责互斥。**

更硬的证据是它「没被优化掉」：同一个自旋循环套上 `synchronized`，
JIT 不敢把 `flag` 的读提到循环外（monitor 是一道屏障）；脱掉就立刻被提走 —— 见下一段。

#### 4. volatile 规则：看见标志 = 看见标志之前的**所有**写

发布/订阅：写线程先写 `d1`、`d2`，再置 `flag`；读线程自旋等 `flag`，然后读 `d1`/`d2`。
写线程先睡 80 ms（**给读线程的循环留出被 JIT 编译的时间**，这一条是今天的关键）。

| 段 | 标志类型 | 5 轮结果 |
| :--- | :--- | :--- |
| C1 | 普通 `boolean` | **5 轮全部撞满 15 亿次自旋上限，一次也没看见** |
| C2 | `volatile boolean` | **5/5 看见**，平均自旋 2.97 亿次（≈80 ms，正好是写完的时刻） |

只差一个 `volatile`，一个永远看不见、一个 80 ms 后立刻看见。
这就是 DCL 敢靠 volatile 的理论依据：**volatile 写之前的写，对读到该 volatile 的线程全部可见。**

至于「看见标志之后数据是否完整」，x86 上测不出差异（TSO 不重排 store-store）——
这跟 DCL 是同一个故事：**JMM 允许的事，不一定能在 x86 上复现。**

#### 5. start() 与 join() 是两条屏障

| 段 | 做法 | 500 轮实测 |
| :--- | :--- | :--- |
| D1 | `start()` **之前**写 `777`，子线程读 | 100% 读到 777 |
| D2 | 不 join，主线程 `start()` 后立刻读结果 | **151 轮读到 0**（还没算完），349 轮读到结果 |
| D3 | `join()` 之后读 | **500/500 正确**（4951） |

D2 那 151 个 0 不是「线程没跑」，是**你比它先读** —— 抢跑。不是并发 bug，但一样是错的。

#### 6. DCL：重排之后递给别人的，是一个「停在半路」的对象

`instance = new Singleton()` 在字节码里是三条：`new` → `invokespecial`（构造）→ `putstatic`（发布）。
JMM 允许把最后一步提到中间那步之前。压力测试（50 轮 × 6 线程 × 每轮抢 30 ms）：

```text
[D1 无 volatile] 拿到构造没跑完的对象 0 次
[D2 有 volatile] 拿到构造没跑完的对象 0 次
```

**两个 0。** 这个 0 不能读成「不加也没事」，只能读成「x86 测不出来」。

为了看清「重排之后长什么样」，我把重排顺序**手工写出来**（分配 → 发布引用 → 跑构造）：

```text
读线程在「引用已发布、构造还没跑」的窗口里抓到了：
  Singleton{a=1, b=null, c=0}
  incomplete（b==null || c!=99）= true
```

注意 `a=1` 已经赋上了、`b` 还是 null —— **半初始化不是「全是默认值」，是「停在中途」**。
调用方拿到它调 `b.length()` 就是 NPE，堆栈指向调用方，离构造十万八千里。
这类 bug 的排查方向天然是错的：会去查「b 为什么是 null」，而不是查「单例怎么发出去的」。

`Unsafe.allocateInstance` 那段补了另一半证据：不跑构造也能造出对象，字段全是零值 / null
（`Singleton{a=0, b=null, c=0}`）—— 说明 `new` 在字节码里一步，在内存里是两步。

#### 7. volatile 的代价：字节码一字不差，机器码多一条 `lock`

```text
private static Singleton plain;                  flags: (0x000a) ACC_PRIVATE, ACC_STATIC
private static volatile Singleton safe;          flags: (0x004a) ACC_PRIVATE, ACC_STATIC, ACC_VOLATILE
```

`getPlain()` 与 `getSafe()` 的字节码**指令序列完全一样**（`getstatic` → `ifnonnull` → `monitorenter`
→ 再判空 → `putstatic` → `monitorexit`）。加 volatile 一行都没多 ——
它改的是 `putstatic` 的内存语义，不是指令条数。

代价在机器码上（`PrintAssembly`）：

```text
0x000002198f2d1830: 4d8b 5740 | 4d8b 5f50 | f083 4424 | c000 4180 | 3800 ...
```

`f0 83 44 24 c0 00` 解码是 `lock addl $0x0,-0x40(%rsp)` —— HotSpot 在 x86 上实现 StoreLoad 屏障的标准做法。
全文这样的 lock 前缀出现 21 处。读侧不加指令：x86 的 TSO 本来就不许 LoadLoad 重排。

微基准（2000 万次 × 6 轮取最小值）：

| 写法 | 耗时 | ns/次 | 说明 |
| :--- | ---: | ---: | :--- |
| 普通字段写 | 0.38 ms | 0.019 | **测量事故**：JIT 发现中间值没人看，整个循环只剩最后一次 store |
| `AtomicInteger.lazySet()` | 2.66 ms | 0.133 | 有序写，不加 lock 前缀 |
| `AtomicInteger.set()` | 15.51 ms | 0.775 | volatile 写 |

**`set()` 是 `lazySet()` 的 5.8 倍。** 差的不是指令，是那道屏障。

#### 8. 遗留

- ✅ `volatile` 已勾（Day 3 的四条规则 + DCL 都过了一遍）。
- ⬜ `Lock` / AQS：只碰了 `tryLock` 与 `holdCount`，`Condition`、公平锁、AQS 的 state/CLH 队列留 Day 4。
- ⬜ `wait` / `notify` / `interrupt` 仍未碰，排 Day 4。
- ⬜ DCL 的「真复现」需要 ARM 机器（x86 的 TSO 天然挡住 store-store 重排），本机只能手工重排 + JMM 推理。
- ⬜ 微基准不是 JMH：E 段只能看量级，不能说「volatile 写就是 0.775 ns」。
- ⬜ 对象头 Mark Word 仍未实测（要 JOL），偏向锁只到 `PrintFlagsFinal` 这一层。

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
| Dekker 第一版 2 万轮一次 (0,0) 都没出现，差点写成「x86 不会重排」 | 用 `CyclicBarrier` 每轮同步，barrier 释放是有先后的：分布 9468 / 10531 一边倒，说明两个线程根本没重叠 —— 测的是「谁先跑完」，不是重排 | 加了一列 `(1,1)` 计数当「真的重叠了吗」的探针：2 万轮里只有 1 次 | 把 barrier 换成**自旋 gate**（`volatile int` 放行 + `AtomicInteger` 收工），(0,0) 立刻占到 16%~40% | 同步工具本身会改变时序；**测试脚手架的对称性决定了能测出什么**，要有一个「重叠确认」的探针 |
| B2 / C1 第一版「去掉锁也看得见」，结论完全反了 | ① 循环体里放了 `System.nanoTime()` 当超时判据 —— 方法调用挡住 JIT 把字段读提到循环外；② 写线程立刻置位，循环还没被 JIT 编译就已经结束，测的是解释执行 | 输出里自旋只有 1.3 万次就看见了，而 Day 1 同样的实验要 20 亿次 —— 数量级不对 | ① 循环体改成纯自旋、只留计数器；② 写线程先睡 80 ms，让循环先被 JIT 编译完 | 可见性实验里**任何方法调用都会污染被测代码**；必须先让 JIT 编译完，否则永远测不出「提升」 |
| C2（volatile）也「看不见」，一度以为 volatile 失效 | 自旋上限 5 亿次 ≈ 170 ms，比写线程的 200 ms 等待还短 —— 读者先撞上限退出了，写还没发生 | 上限 15 亿次 ≈ 400 ms 后 5/5 都看得见，平均自旋 2.97 亿（≈80 ms，正好是写入时刻） | 上限提到 15 亿，等待降到 80 ms（比值 6 倍） | **自旋上限必须远大于等待时间** —— 这条 Day 1 就记过，今天又踩了一次。同一个坑踩两次，说明它该进「开工前必读」 |
| E 段「普通字段写」测出 0.019 ns/次，比 L1 命中还快 50 倍 | JIT 发现循环里的中间写没人看得见，整条循环只剩最后一次 store —— 循环被消掉了 | 数字小到物理上不可能（0.019 ns ≈ 1/50 个时钟周期） | 不再拿它当基准，改用 `AtomicInteger.set()` vs `lazySet()` 对照（都是方法调用，消不掉） | **数字接近 0 一律先怀疑测量事故**，这是本周第二次（Day 5 的 parallel 也是） |
| D3 段同一段输出里 `toString` 显示 `b=null`，下一行 `incomplete()` 却是 false | `toString` 抓得早、`incomplete()` 调用得晚，中间 2 ms 构造跑完了 —— 两次读不是同一个瞬间 | 两句话自相矛盾，一眼看着像 bug | 先把 `a` / `b` / `c` 各抓一次快照到局部变量，再用快照判断和拼接 | 并发下的「打印」不是原子的；**要断言同一条状态，必须只读一次** |
| 以为本机没装 hsdis，`PrintAssembly` 用不了，准备把屏障证据留成遗留 | 想当然。`-XX:+PrintAssembly` 其实能跑，只是输出只有十六进制、**不带助记符** | 跑一次发现输出了 204 段 Assembly，全文 21 处 `f0` 前缀 | 改成「贴原始十六进制 + 自己按 x86 编码表解码」并标明这是人读的不是工具给的 | `f0 83 44 24 c0 00` = `lock addl $0x0,-0x40(%rsp)`，HotSpot 的 StoreLoad 屏障就长这样 —— **工具不给助记符，也照样能取证** |

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
- **happens-before 到底是什么？**（偏序，不是时钟；说的是「结果可见 + 不许重排」，不是「先执行」。
  八条里能实测的四条：程序次序 / monitor / volatile / start-join —— 每条都有一个「去掉就错」的对照实验）
- 程序次序规则能不能保证跨线程的顺序？（不能。Dekker 实测 10 万轮里 `(0,0)` 出现 16%~40%，
  「先写后读」在内存看来可以被换过来。程序次序只管单线程 as-if-serial）
- `synchronized` 只保证互斥吗？（不是，还保证可见性 —— 写线程解锁 happens-before 读线程加锁。
  实测：锁外写的普通字段，读线程在锁内看到标志后一定能读到新值，去掉锁就 15 亿次自旋也看不见）
- **volatile 在字节码上多了什么？**（一条指令都没多。`getPlain()` 和 `getSafe()` 的字节码完全一样，
  差别只在字段 flags：`0x000a` vs `0x004a`，多出来的 `0x0040` 是 `ACC_VOLATILE`）
- **volatile 写为什么比普通写慢？慢多少？**（x86 上要跟一条 `lock addl $0x0,-0x40(%rsp)` 做 StoreLoad 屏障。
  实测 `set()` 0.775 ns/次 vs `lazySet()` 0.133 ns/次，**5.8 倍**。读侧几乎不加指令）
- **DCL 为什么必须加 volatile？不加会发生什么？**（`putstatic` 可能被提到 `invokespecial` 之前，
  另一个线程会拿到「构造跑一半」的对象 —— 实测抓到 `{a=1, b=null, c=0}`。
  注意它不是「全是默认值」，是「停在中途」，所以 NPE 的位置离构造很远）
- 那在 x86 上不加 volatile 是不是也没事？（**不是**。实测压力跑 50 轮 × 6 线程，失败 0 次 ——
  因为 x86 是 TSO，不重排 store-store。但 JMM 允许，换 ARM 就炸。
  **判断依据只能是 JMM，不是「我跑了没复现」**）
- DCL 之外还有什么单例写法？哪种更稳？（静态内部类 holder / 枚举 / 饿汉。
  holder 靠 `<clinit>` 的类初始化锁，天然带 HB 边且不需要 volatile，**工程上更推荐**）

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 2026-11-04 | 三 | Day 1 线程基础：三种起线程方式 + run/start 陷阱 + 竞态复现（2 万 vs 50 万对照）+ volatile/synchronized/Atomic 三条修法 + 可见性对照 + javap 字节码 | 60 min | `StartThread` / `RaceCondition` / `day1-result.txt` / `day1-bytecode.txt` | ✅ |
| 2026-11-05 | 四 | Day 2 锁的是谁：实例锁 this / 静态锁 Class / `new Object()` 没锁 + 可重入 + 死锁（ThreadMXBean + jstack）+ 活锁 4/5 + 全局定序 + 偏向锁三档证据 | 60 min | `LockTarget` / `Deadlock` / `day2-result.txt` / `day2-deadlock-jstack.txt` | ✅ |
| 2026-11-06 | 五 | Day 3 happens-before：Dekker 测出 (0,0) 16%~40% / monitor 传可见性 / volatile 发布订阅（对照组 15 亿次看不见）/ start-join 抢跑 151/500 + DCL 半初始化对象 `{a=1,b=null,c=0}` + `lock` 前缀取证 + set vs lazySet 5.8 倍 | 60 min | `HappensBefore` / `DclSingleton` / `day3-result.txt` / `day3-bytecode.txt` | ✅ |
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

---

**Q · happens-before 保证的是什么？为什么它不等于「先执行」？**

一句话：**它保证「结果可见」和「不许越过它重排」，不保证「时间上先执行」。**

- `A hb B` → A 的写对 B 可见；并且编译器/CPU 不能把 A 挪到 B 后面。
- 但 A 完全可能被挪到 B **前面**的其他位置，也可能和别的操作交错 —— 只要单线程的可见结果不变（as-if-serial）。
- 两个**没有** hb 边的操作，顺序是完全不确定的 —— 这就是并发 bug 的温床。

四条实测（`day3/HappensBefore.java`，每条都有对照组）：

| 规则 | 怎么证明 |
| :--- | :--- |
| 程序次序 | Dekker litmus，10 万轮 `(0,0)` 占 16%~40% → 「先写后读」在内存看来能反过来 |
| monitor | 锁内自旋能看到锁外写的普通字段；脱掉锁 15 亿次也看不见 |
| volatile | 同样的发布/订阅，`volatile` 版 80 ms 后看见，普通版撞满上限也看不见 |
| start / join | `start()` 前写的 777 必然可见；不 join 抢跑 151/500；join 后 500/500 |

补一句容易答漏的：**`volatile` 修的是「可见性 + 有序性」，不是「原子性」**。
所以它救不了 `count++`（Day 1 实测 10/10 轮丢），但能救标志位与安全发布。

---

**Q · 双重检查锁为什么必须加 `volatile`？**

`instance = new Singleton()` 是三条字节码：`new`（分配）→ `invokespecial`（构造）→ `putstatic`（发布）。
JMM 允许 `putstatic` 被提到 `invokespecial` 前面 —— 于是第二个线程可能拿到**构造只跑了一半**的对象：

```text
Singleton{a=1, b=null, c=0}
```

（这是实测抓到的现场：`a` 已赋值、`b` 还是 null，所以「半初始化」不是「全是默认值」，是「停在中途」。）
后果是调用方 NPE，堆栈指向调用方，跟构造毫无关系 —— 排查方向天然是错的。

`volatile` 的作用：在「发布引用」这一步加 StoreStore 屏障，禁止它被提前。

**最容易被追问的一点：这个 bug 在 x86 上复现不了。**
实测 50 轮 × 6 线程 × 每轮抢 30 ms，不加 volatile 那版失败 **0 次** —— x86 的 TSO 不重排 store-store。
但这**不代表安全**：JMM 允许它发生，ARM / 换 JIT 版本就会炸。
**判断依据只能是 JMM 的规则，不能是「我跑了没复现」。**

工程上更推荐**静态内部类 holder**：靠 `<clinit>` 的类初始化锁，JVM 保证只跑一次且天然带 hb 边，
不需要 volatile，也不会有「两次判空」这种需要读者懂内存模型才能看懂的地方。
