# W03 · Java 并发基础 · 实验工程

> 目标：**不是背 `volatile` 的定义，而是亲手把「丢更新」撞出来，再解释它。**
>
> 本周笔记：[`../../../01-notes/stage1-basics/W03-Java并发基础.md`](../../../01-notes/stage1-basics/W03-Java并发基础.md)

---

## 怎么跑

纯 JDK，无第三方依赖，按 **JDK 17** 编译验证。并发实验请在**真机**上跑，不要放进受限沙箱 —— 单核环境天然不丢更新。

```bat
rem Windows
run.bat                      rem 默认跑 day1.StartThread
run.bat RaceCondition        rem 短名 -> day1 包
run.bat day1.RaceCondition   rem 带点号 -> 完整类名
```

```bash
# Git Bash / macOS / Linux
./run.sh
./run.sh RaceCondition
./run.sh day1.RaceCondition
./run.sh day2.LockTarget
./run.sh day2.Deadlock          # 约 11 秒：D1 死锁 + D2 可重入 + D3 活锁统计
./run.sh day2.Deadlock hold     # 只跑 D1 并挂住 20 秒，留给外部 jstack
./run.sh day3.HappensBefore     # 约 8 秒：happens-before 四条实测 + volatile 代价
./run.sh day3.DclSingleton      # 约 6 秒：DCL 压力测试 + 手工重排 + Unsafe
./run.sh day4.WaitNotify        # 约 5 秒：wait/notify 五个现场 + if vs while
./run.sh day4.MiniAqs           # 约 7 秒：手写 AQS 对拍 200 万次 + 三种锁吞吐对照
./run.sh day4.LockCompare       # 约 3 秒：公平/非公平 + Condition vs notifyAll
```

手动编译：

```bash
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day1.RaceCondition
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day2.LockTarget
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day2.Deadlock
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day3.HappensBefore
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day3.DclSingleton
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day4.WaitNotify
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day4.MiniAqs
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day4.LockCompare
```

> ⚠️ `RaceCondition` 会开 10 个线程做几千万次自增，跑完大约 5 秒；`StartThread` 里的守护线程会故意被 JVM 掐断，属于预期现象。
> `Deadlock` 结尾调 `System.exit(0)` —— D1 的两个死锁线程永远不会醒，不 exit 的话 JVM 不会退出。

## 目录

```text
w03-concurrency-basics/
├── run.bat / run.sh                        一键编译 + 运行
├── day1-result.txt                         Day 1 实测输出（两种起线程 + 竞态 + 可见性）
├── day1-bytecode.txt                       Day 1 字节码证据（iinc 窗口 / ACC_VOLATILE / ACC_SYNCHRONIZED / monitorenter）
├── day2-result.txt                         Day 2 实测输出（锁对象三连 + 死锁检测 + 活锁统计 + 偏向锁三档）
├── day2-deadlock-jstack.txt                Day 2 死锁现场原件（jstack，含 Found one Java-level deadlock）
├── day3-result.txt                         Day 3 实测输出（Dekker / monitor / volatile 发布订阅 / start-join / set vs lazySet）
├── day3-bytecode.txt                       Day 3 证据（ACC_VOLATILE / 两个 DCL 字节码对比 / lock 前缀原文）
├── day4-result.txt                         Day 4 实测输出（wait/notify 五段 + AQS 对拍 + 公平性 + Condition）
├── day4-bytecode.txt                       Day 4 证据（手写 lock 的快路径 / park 调用 / monitorenter 成对出现）
└── src/main/java/com/newt/java48/w03/
    ├── day1/
    │   ├── StartThread.java                A~E：三种起线程方式 / run vs start / 守护线程
    │   └── RaceCondition.java              B0~C4：基线 / 竞态复现 / volatile / synchronized / AtomicInteger / 可见性
    ├── day2/
    │   ├── LockTarget.java                 A~C：实例锁 this / 静态锁 Class / new Object() 等于没锁 / 字符串常量全局锁
    │   └── Deadlock.java                   D1~D3：死锁 + ThreadMXBean 检测 / 可重入 / 活锁与全局定序
    ├── day3/
    │   ├── HappensBefore.java              A~E：Dekker litmus / monitor 传可见性 / volatile 发布订阅 / start-join / 屏障代价
    │   └── DclSingleton.java               D1~D4：压力测试 / 手工重排抓半初始化对象 / Unsafe.allocateInstance
    └── day4/
        ├── WaitNotify.java                 A~E：ISE / if vs while / notify 只叫一个 / 释放锁 / 中断清标志
        ├── MiniAqs.java                    A~D：手写 state+CLH 队列 / 200 万次对拍 / 三种锁吞吐 / CAS 与 park 统计
        └── LockCompare.java                A~B：公平 vs 非公平（插队次数）/ Condition vs notifyAll（无效唤醒）
```

## Day 1 做了什么

### `StartThread` —— 起线程的三种写法，加上两个常见误解

| 段 | 内容 | 一句话结论 |
| :-- | :--- | :--- |
| A | `extends Thread` | 能用，但把任务和线程绑死，Java 单继承 → 不推荐 |
| B | `implements Runnable` / lambda | 任务与线程解耦；`Runnable` 是函数式接口，lambda 直接能塞 |
| C | `Callable<Integer>` + `FutureTask` | 唯一能**带返回值**的方式；`call()` 抛的异常会被包成 `ExecutionException`，原异常只能从 `getCause()` 取 |
| D | `t.run()` vs `t.start()` | `run()` 打印的线程名是 `main` —— 它只是普通方法调用，压根没开线程 |
| E | daemon 线程 | 守护线程不阻止 JVM 退出，`sleep(5s)` 的那句永远不会打印 |

另外撞了一条：**`start()` 第二次调用抛 `IllegalThreadStateException`，而且没有 message** —— 又一个「异常什么都没说」的现场。

### `RaceCondition` —— 一天里最值钱的部分

| 段 | 配置 | 实测结果（JDK 17，10 线程，CountDownLatch 同时起跑） |
| :-- | :--- | :--- |
| B0 | 单线程 20 万次 | 3/3 轮都是 200000 → 循环本身没问题 |
| B1 | `int` 自增，**每线程 2 万次** × 10 轮 | **7/10 轮丢**，最惨 32265（丢 83.9%）；中间第 5~7 轮一次不丢，第 8~10 轮又小额丢（1 万~4 万） |
| B2 | 同一份代码，**每线程 50 万次** × 10 轮 | **10/10 轮丢**，最低 930367 / 5000000 |
| C1 | `volatile int` 自增 | **10/10 轮丢**，最低 1676548（丢 66.5%） |
| C2 | `synchronized`（方法级 5 轮 + 同步块 5 轮） | **0/10 轮丢**，两次都是 10 个 5000000 |
| C3 | `AtomicInteger.incrementAndGet()` | **0/10 轮丢** |
| C4 | 自旋等标志位：普通 `boolean` vs `volatile boolean` | 普通版 20 亿次自旋**始终没看见**（撞上限退出）；volatile 版 1.98 亿次后看见 |

三个必须记住的点：

1. **`count++` 在字节码里是四条指令**（`getfield` / `iconst_1` / `iadd` / `putfield`），
   窗口就在 `iadd` 和 `putfield` 之间 —— 两个线程都读到同一个旧值，就少一次加法。
2. **B1 的后 3 轮一次都没丢，不是代码变安全了。** JIT 把自增编译成机器码之后，2 万次只要几十微秒，
   10 个线程还没全被唤醒第一个就跑完了 —— 这就是「我本地跑十遍都是对的」的物理原因。**要复现，得加量**（B2）。
3. **`volatile` 救不了 `count++`**（10/10 轮都丢），它救的是 C4 那种「一个写、另一个只读」的场景。
   两件事：可见性 ≠ 原子性。

### `day1-bytecode.txt` 里被纠正的一个想当然

我原本以为方法级 `synchronized` 会产生 `monitorenter` / `monitorexit`。`javap` 打脸：

- **方法级**：字节码一条加锁指令都没有，只有方法 flags 里的 `ACC_SYNCHRONIZED`（`0x0022`）；
- **同步块**：才真的出现 `monitorenter` / `monitorexit`，而且带着异常路径（`Exception table` 里两条 `any`）。

字段那边同理：`private volatile int vol` 的 `flags: (0x0042) ACC_PRIVATE, ACC_VOLATILE`，
而 `incVolatile` 的字节码和 `incPlain` **一模一样** —— volatile 不是多一条指令，是给 `getfield`/`putfield` 加了内存语义。

## Day 2 做了什么

> 主题：**`synchronized` 到底锁了谁**。Day 1 只证明了「加锁能救」，Day 2 回答「锁的是哪个对象」，
> 以及「锁错了会怎么样」「锁对了还会死在哪里」。

### `LockTarget` —— 三把锁，三条可打印的证据

证据不是背结论，是两样东西：`Thread.holdsLock(obj)`（直接问 JVM 你现在持有谁的锁），
以及方法入口算出的「排队等了多久」（被挡住时不是 0）。

| 段 | 场景 | 实测 |
| :-- | :--- | :--- |
| A1 | 同一实例 × 2 线程 | 第二个**排队 414 ms**，总 818 ms，`holdsLock(this)=true` |
| A2 | 两个不同实例各 1 线程 | **都排队 0 ms**，总 409 ms → 锁的是实例，不是类 |
| A3 | 先 `synchronized(inst)` 占 400 ms，再调 `inst.work()` | 调用者**排队 340 ms** → 方法锁与 `synchronized(this)` 是同一个 monitor |
| B1 | 两个线程调 `static synchronized` | 第二个**排队 400 ms**；`holdsLock(ClassLock.class)=true`，`holdsLock(实例)=false` |
| B2 | 先 `synchronized(ClassLock.class)` 占 400 ms，再调静态方法 | **排队 351 ms** → 静态方法锁的就是这个 Class 对象 |
| B3 | 一个走静态方法、一个锁实例 | 都 0 ms，总 412 ms → **两把锁，互不影响** |
| C1 | `synchronized (new Object())` × 10 线程 × 50 万 | 丢 **2846644** / 5000000，耗时仅 **46 ms** |
| C2 | 换成共享 `private final Object lock` | 丢 0，耗时 339 ms |
| C3 | 锁字符串常量 `"LOCK"` | 跨类 `==` 为 true，别的类被挡 **354 ms** → 等于拿了一把全局锁 |

三个必须记住的点：

1. **`synchronized(new Object())` 比真锁还快**（46 ms vs 339 ms）—— 快出来的就是没加锁省下的。
   **「加了锁反而变快」几乎一定意味着锁没生效**，这是个很好用的反直觉信号。
2. **静态锁与实例锁是两把锁**（实测并行 412 ms）。`static synchronized` 保护不了实例字段，
   两个方法都在改同一个 `static Map` 却各加各的锁 → 等于没加。这条直接对应
   `@Transactional` 不生效、`@Async` 不同步那一类 bug 的病根。
3. **锁字符串常量 / 包装类型缓存对象 = 全局锁**。锁对象用 `private final Object lock = new Object()`。

### `Deadlock` —— 发现、确认、破解是三件事

| 段 | 内容 | 实测 |
| :-- | :--- | :--- |
| D1 | 交叉拿锁 A→B / B→A | `findDeadlockedThreads()` 检出 2 个 BLOCKED；`jstack` 稳定输出 `Found 1 deadlock` |
| D2 | 同一线程重入同一把锁 | 递归 3 层正常返回；`ReentrantLock` 的 `holdCount` 2 → 1 → 0，多 unlock 抛 `IllegalMonitorStateException` |
| D3a | `tryLock` 拿不到就放掉重试，**不加退避** × 5 次 | **活锁 4/5 次**，重试合计 74 次，线程在跑但没人推进 |
| D3b | 同上 + 随机退避 | 跑完，重试 0 + 1 次 |
| D3c | 全局定序（按 `identityHashCode` 排序后加锁） | 2000 轮 × 2 线程 = 4000，精确 4000，**1 ms，零重试** |

`day2-deadlock-jstack.txt` 是 `jstack` 原件，看三样：
**谁等谁**（`waiting to lock ... which is held by ...` 成对出现）、**业务代码第几行**、
**末尾的 `Found N deadlock`**（没有这行就不是死锁，是等 IO / 等信号）。

两个坑：`isAlive()==true` 不代表在干活；`blockedCount` 不是等待毫秒数。

### 偏向锁：JDK 17 默认关，JDK 25 参数已删

```text
JDK 17: bool UseBiasedLocking = false {product} {default}      ← JEP 374
        -XX:+UseBiasedLocking 只剩一句 "deprecated in version 15.0" 警告
JDK 25: Unrecognized VM option 'UseBiasedLocking'              ← 参数已删除
```

所以面试再讲「无锁 → 偏向 → 轻量 → 重量」这套升级，要补一句「JDK 15 起偏向锁默认关闭」，
别背 JDK 8 的八股。

## Day 3 做了什么

> 主题：**`volatile` 的内存语义**。Day 1 只证明「volatile 救得了标志位」，
> Day 3 要回答「为什么救得了」「happens-before 到底保证了什么」「DCL 不加会怎样」。

### `HappensBefore` —— 四条规则，四组对照

| 段 | 规则 | 实测 |
| :-- | :--- | :--- |
| A | 程序次序（Dekker litmus） | 10 万轮里 `(0,0)` 出现 **16239 / 28465 / 40629 次**（16%~40%）→ 「先写后读」在内存看来能反过来 |
| B | monitor | 锁内自旋 **11 065 533 次**后看到标志，读到锁外写的 `data = 42`；且没被 JIT 优化掉 |
| C1 | 对照组：普通 `boolean` 标志 | **5 轮全部撞满 15 亿次上限，一次没看见** |
| C2 | volatile 标志 | **5/5 看见**，平均自旋 2.97 亿次（≈80 ms，正好是写入时刻） |
| D1 | `start()` 前的写 | 子线程 100% 读到 777 |
| D2 | 不 join，直接读结果 | **151/500 轮读到 0**（抢跑） |
| D3 | `join()` 后读 | 500/500 正确 |
| E | `set()` vs `lazySet()` | 15.51 ms vs 2.66 ms（2000 万次），**5.8 倍** |

> E 段还有一行「普通字段写 0.38 ms / 0.019 ns」，那是**测量事故** ——
> JIT 发现中间值没人看，整个循环只剩最后一次 store。留着这一行是为了记住这个坑。

### `DclSingleton` —— 为什么必须 volatile，以及为什么你在 x86 上测不出来

| 段 | 内容 | 实测 |
| :-- | :--- | :--- |
| D1 | 不加 volatile 的 DCL，50 轮 × 6 线程 × 每轮抢 30 ms | 拿到半初始化对象 **0 次** |
| D2 | 加 volatile，同样压力 | **0 次** |
| D3 | 把重排顺序**手工写出来**（分配 → 发布 → 构造） | 抓到 `Singleton{a=1, b=null, c=0}` —— 构造停在中途 |
| D4 | `Unsafe.allocateInstance` 不跑构造造对象 | `Singleton{a=0, b=null, c=0}` —— 字段全零值 |

**D1/D2 都是 0，这本身就是最重要的结论**：x86 是 TSO，不重排 store-store，
所以 DCL 少写 volatile 在 x86 开发机上「永远测不出来」，换 ARM 才炸。
判断依据只能是 JMM 的规则，不能是「我跑了没复现」。

D3 抓到的 `b=null` 也纠正了一个常见想象：半初始化不是「字段全是默认值」，而是「停在半路的某一刻」，
所以 NPE 会出现在离构造很远的地方。

### `day3-bytecode.txt` 里的两处证据

```text
private static Singleton plain;            flags: (0x000a) ACC_PRIVATE, ACC_STATIC
private static volatile Singleton safe;    flags: (0x004a) ACC_PRIVATE, ACC_STATIC, ACC_VOLATILE
```

两个 `getInstance()` 的字节码**指令序列完全一样** —— 加 volatile 一行都没多，改的是 `putstatic` 的内存语义。

屏障在机器码里（`-XX:+PrintAssembly`，本机只输出十六进制、不带助记符）：

```text
0x000002198f2d1830: 4d8b 5740 | 4d8b 5f50 | f083 4424 | c000 4180 | 3800 ...
```

`f0 83 44 24 c0 00` = `lock addl $0x0,-0x40(%rsp)` —— HotSpot 在 x86 上实现 StoreLoad 屏障的标准做法，
全文出现 21 处。读侧不加指令（TSO 本来就不许 LoadLoad 重排）。

## Day 4 做了什么

> 主题：**`Lock` / AQS / `wait-notify`**。前三天证明了「锁能互斥」「volatile 能传可见性」，
> 但 `wait()` 为什么必须写在 `synchronized` 里、AQS 的 `state` 和那个队列到底怎么转，一直是空的。
> 今天把 AQS 手写了一遍 —— 这两个问题只有写出来才答得干净。

### `WaitNotify` —— 五个现场

| 段 | 内容 | 实测 |
| :-- | :--- | :--- |
| A | `wait()` 不在 `synchronized` 里 | `IllegalMonitorStateException: current thread is not owner` |
| B1 | 2 消费者 + 1 件货 + `notifyAll()`，用 `if` | 空手而归 **1 次** —— 醒了就往下走，货架是空的 |
| B2 | 同样场景换成 `while` | 空手而归 **0 次**，「醒来重查又回去等」1 次 |
| C1 | 放 2 件货只 `notify()` 一次 | 取走 **1 件**，剩 1 件，还有 **1 个消费者停在 WAITING** |
| C2 | 放 2 件货 `notifyAll()` | 取走 2 件，剩 0 件，0 个在等 |
| C3 | 没人在等时 `notify()`，随后 `wait(200)` | 等满 **200 ms** 才超时 —— 信号不存储 |
| D | 持锁者 `sleep(500)` vs `wait(500)` | 旁观者排队 **414 ms** vs **0 ms** |
| E | 中断打断 `wait()` | 抛 `InterruptedException`，捕获后 `isInterrupted()` 已是 **false** |

C 段第一版想用「两个等待者等不同条件 + `notify()` 叫错人」来演示丢信号，
20 轮只丢了 2 次 —— 这个 JVM 大多时候恰好挑中了该醒的那个。**靠概率的结论不算结论**，
改成「2 件货只喊一声」：只要少喊一次，就必然有人醒不过来，100% 可复现。

### `MiniAqs` —— 80 行手写 AQS

`state`（重入计数）+ 双向 CLH 队列 + `LockSupport.park/unpark`，与 AQS 同构。

| 段 | 内容 | 实测 |
| :-- | :--- | :--- |
| A | 10 线程 × 20 万次 `count++`，普通 `int` | 期望 2000000，实际 **2000000，丢 0**，730 ms |
| A' | 同一段代码不加上锁 | 丢 **1551791**（77.6%） |
| B | `lock()` 三次 / `unlock()` 三次 | `state` 1 → 3 → 1 → 0；第 4 次 `unlock()` 抛 ISE |
| C | 三种锁，临界区 ≈1 ns（200 万次） | sync **118 ms** / ReentrantLock **50 ms** / 简易 **687 ms**（5.8 倍） |
| C' | 三种锁，临界区 ≈0.5 µs（5 万次） | sync **84 ms** / ReentrantLock **75 ms** / 简易 **95 ms**（**1.13 倍**） |
| D | 队列开销 | CAS 建哨兵 **1 次**、CAS 抢尾 **87742 次**、park **175075 次**、醒来没抢到 **87500 次** |

**C / C' 这一对是今天最有价值的对照**：临界区从 1 ns 拉到 0.5 µs，5.8 倍的差距缩到 1.13 倍。
差的那部分几乎正好等于 `park 次数 × 4.2 µs`（D 段算出来的实数）。
所以「手写版慢」不是队列写错了，是**这个负载在考「自旋 vs 挂起」的取舍** ——
`ReentrantLock` 敢猛自旋，是因为它知道自旋失败才付挂起的钱；而临界区 1 ns 时，挂起一次就等于输几万次。

回答开头那两个问题：

- **为什么入队要 CAS 两次**：一次建哨兵（队列惰性初始化），一次抢尾。两个状态转移各自必须原子，
  没法合并成一个 CAS —— 合并就意味着要同时原子地改 `head` 和 `tail`。实测就是 `1 次 + 每次入队 1 次`。
- **为什么 park 之后还要再检查**：D 段那 **87500 次「醒来没抢到」**就是答案。最凶的插队者不是新线程，
  而是**刚 `unlock()` 转头又 `lock()` 的那个线程** —— 在压测循环里它每轮都干这事。

### `LockCompare` —— 公平性，和 Condition 存在的理由

| 段 | 指标 | 非公平 | 公平 |
| :-- | :--- | :--- | :--- |
| A | 8 线程 × 2 万次耗时 | **30 ms** | 1228 ms（**41 倍**） |
| A | 连续两次抢到锁 | **154582 次** | 445 次 |
| A | 单线程最长等待 | 387 µs | **582 µs** |

第三行是反直觉的：**公平锁的最长等待反而更大**。因为临界区只有 1 ns，公平锁等的主要不是临界区，
而是「解锁后唤醒下一个人」的交接成本（几 µs），排 8 个人就是 8 次交接。
所以公平换来的不是「等得更短」，是「等得有上界、可预测」；非公平的理论最坏情况是无穷。

| 段 | 实现 | 耗时 | 无效唤醒 |
| :-- | :--- | :--- | :--- |
| B | `Condition`（notFull / notEmpty 两个队列） | 143 ms | **11 次** |
| B | `synchronized` + `notifyAll` | 172 ms | **7666 次** |

容量设成 1（队列几乎总是空或满），每次唤醒都是争抢，差别才看得见：
`Condition` 精确叫一个，`notifyAll` 把 4 个消费者一起叫醒，3 个白醒。
**这就是 `Condition` 存在的唯一理由 —— 把等不同条件的线程分组成多个等待队列。**

### 今天的四个坑

- **`join()` 写在 `synchronized` 里 = 自己卡自己。** C 段第一版在持锁状态下 `join` 消费者线程，
  而消费者要重新拿到这把锁才能退出临界区 —— 直接等到超时。改成先取数、出临界区、再 join。
- **C 段第一版把「放 2 件货」写成了初始状态。** 消费者进临界区时货已经在架上，谁都没 `wait`，
  于是两个人都顺利取走 —— 实验根本没成立。**货必须在消费者睡着之后才放。**
- **`synchronized` 那个 22 ms 是假的。** 一开始 C 段和 C2 用了两套脚手架（一套内联自增、一套包 lambda），
  同一份活测出 22 ms 和 107 ms，还很自信地写了「差 15 倍」。统一脚手架 + 预热之后是 5.8 倍。
  **不同脚手架的数字不能放一张表里比。**
- **`Node.thread` 声明成 `final` 就没法变哨兵。** `setHead` 要把自己变成哨兵（`thread = null`），
  `final` 直接编译不过 —— 真实 AQS 里这个字段也是 `volatile` 而非 `final`。
