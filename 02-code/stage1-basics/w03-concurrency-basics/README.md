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
```

手动编译：

```bash
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day1.RaceCondition
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day2.LockTarget
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day2.Deadlock
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
└── src/main/java/com/newt/java48/w03/
    ├── day1/
    │   ├── StartThread.java                A~E：三种起线程方式 / run vs start / 守护线程
    │   └── RaceCondition.java              B0~C4：基线 / 竞态复现 / volatile / synchronized / AtomicInteger / 可见性
    └── day2/
        ├── LockTarget.java                 A~C：实例锁 this / 静态锁 Class / new Object() 等于没锁 / 字符串常量全局锁
        └── Deadlock.java                   D1~D3：死锁 + ThreadMXBean 检测 / 可重入 / 活锁与全局定序
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
