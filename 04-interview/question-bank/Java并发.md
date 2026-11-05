# Java并发 · 面试题库

> 线程、锁、volatile、线程池、CompletableFuture、AQS ｜ 对应 W43
> 每道题都要求：**脱稿讲出来**，并给出「我在项目里怎么用的」。

## 题目

### 1. `count++` 为什么线程不安全？用字节码说清楚「窗口」在哪一步。

**我的回答（自己的话）：**

> 因为它不是一条指令，是**四条**。`javap -p -c` 打出来：
>
> ```text
> 0: aload_0
> 1: dup
> 2: getfield   // Field plain:I     ← 读
> 5: iconst_1
> 6: iadd                          ← 加（在操作数栈上，还没写回）
> 7: putfield   // Field plain:I     ← 写
> ```
>
> 窗口就在 `iadd` 和 `putfield` 之间。两个线程都停在 `getfield` 读到 100，各自算出 101，
> 各自写回 101 —— 两次自增只多 1。**没有异常、没有告警、日志也不打**，所以报表数字偏小能挂很久没人发现。
>
> 实测（`w03-concurrency-basics/day1`，10 线程、`CountDownLatch` 同时起跑，跑 10 轮）：
>
> | 写法 | 结果 |
> | :--- | :--- |
> | 普通 `int`，每线程 50 万次 | **10/10 轮丢**，最惨一轮只数到 930367（应到 5000000） |
> | `volatile int`，同样 50 万次 | **10/10 轮丢**，最低 1676548 |
> | `synchronized` | 0/10 轮丢 |
> | `AtomicInteger` | 0/10 轮丢 |
>
> 单线程跑同样的次数，3/3 轮都是精确值 —— 说明循环本身没问题，少的那些只能是并发造成的。
>
> 最容易答错的是第二行：很多人把 `volatile` 当「轻量 synchronized」，但它连 `count++` 都救不了。

**追问预判：**
- 那 `count += 5`、`++count` 呢？（一样不安全，字节码同样是 `getfield` → 常量 → `iadd` → `putfield`，
  换写法不改变「读-改-写」的本质）
- 为什么单线程能跑出精确的 200000？（单线程不存在「另一个人插进来」的时机，顺序执行 = 天然原子）
- `long` / `double` 和 `int` 有区别吗？（有，JLS 17.7：非 volatile 的 64 位 `long`/`double`
  在 32 位 JVM 上允许被拆成两次 32 位写，读到「半个新值」叫 word tearing；`int` 不会）
- 那就把所有字段都加 `volatile`？（不行，见第 2 题；而且 volatile 有内存屏障开销，也保证不了多字段一起变）
- 为什么有时候跑 10 万次一次都不丢？（这是真加分项：2 万次 × 10 线程实测只有 4~7/10 轮丢，
  因为预热后单线程几十微秒就跑完，10 个线程还没全被唤醒 —— **没重叠就没有竞态**。
  加量到 50 万次才稳定 10/10 丢）

**项目里的实际案例：**

> 场景：报表模块用 `static Map<String, Integer> counter` 做本地计数，多线程累加。
> 数字比数据库 `count(*)` 少一截，但没有任何报错，被当成「统计口径差异」查了两天。
> 现在这类计数一律 `LongAdder`（高并发下比 `AtomicInteger` 更好，分段累加减少 CAS 冲突）。

---

### 2. `volatile` 保证什么、不保证什么？为什么它救不了 `count++`，却能救一个标志位？

**我的回答（自己的话）：**

> 一句话：**volatile 保证可见性和有序性，不保证原子性。**
>
> - **可见性**：一个线程写完，另一个线程立刻能读到。底层靠内存屏障（x86 上写是 `lock` 前缀指令，
>   把 store buffer 刷掉）。
> - **有序性**：禁止指令重排（写前 StoreStore、写后 StoreLoad；读后 LoadLoad / LoadStore）。
> - **原子性**：**不管**。`vol++` 依然是「读-加-写」三步，volatile 只是让每一步都去主存取最新值，
>   读和写之间照样能被插队。
>
> 两个方向的实测（同一天做的对照，最能说明边界）：
>
> | 场景 | 结果 |
> | :--- | :--- |
> | `volatile int vol; vol++` × 10 线程 × 50 万次 | **10/10 轮丢**，最低 1676548 / 5000000 |
> | 普通 `boolean` 标志位，另一线程自旋等 | 20 亿次自旋**始终没看见**新值 |
> | `volatile boolean` 同样自旋 | **1.98 亿次后看见**，51 ms 退出 |
>
> 第一行是「不保证原子性」，后两行是「保证可见性」——同一个关键字，两种完全相反的结论，
> 取决于这个变量是「参与计算」还是「只做信号」。
>
> 字节码层面还有个细节值得说：`incVolatile()` 和 `incPlain()` 的指令**一模一样**，
> 差别只在字段标志位 `flags: (0x0042) ACC_PRIVATE, ACC_VOLATILE`。
> 所以 volatile 不是「多编了一条指令」，是给 `getfield` / `putfield` 加了内存语义。
>
> 那个「普通 boolean 永远看不见」的实验还顺带证明了一件事：**JIT 会把循环里的读提到循环外**
> （编译进寄存器），这才是「可见性问题真的会发生」的物理来源，不是玄学。

**追问预判：**
- 那什么场景 `volatile` 就够？（一写多读的状态标志：`running` / `initialized` / 配置热更新；
  以及 DCL 单例的实例字段）
- 为什么双重检查锁（DCL）的 `instance` 必须 `volatile`？（`new` 不是原子的：
  分配内存 → 初始化 → 赋值引用。重排成「先赋值后初始化」，另一个线程会拿到**半初始化对象**。
  volatile 禁止这个重排）
- `volatile` 能替代锁吗？（不能。多字段要一起变、需要「先读后算再写」的组合操作时不行）
- 那 `AtomicInteger` 和 `volatile` 什么关系？（`AtomicInteger` 内部的值就是 `volatile` 的，
  原子性靠 CAS 循环补上 —— **volatile 是 Atomic 的地基，不是替代品**）
- 内存屏障具体是哪四种？（LoadLoad / StoreStore / LoadStore / StoreLoad，
  最重的是 StoreLoad，x86 上只有它需要显式 `mfence` 或 `lock` 前缀）
- `volatile` 数组呢？`volatile int[] arr` 有用吗？（引用可见，**元素不可见**；
  要元素级可见得用 `AtomicIntegerArray`）

**项目里的实际案例：**

> 场景：本地缓存刷新用 `volatile boolean refreshing` 做「同时间只允许一个线程刷新」的开关。
> 这个写法只对了一半 —— 可见性没问题，但「读 + 判断 + 写」不是原子的，两个线程能同时进。
> 正确做法是 `AtomicBoolean.compareAndSet(false, true)`。
> **这正是「volatile 只解决信号、解决不了判断」的典型**，也是我原来最容易搞错的地方。

---

### 3. `synchronized` 到底锁的是谁？修饰实例方法、静态方法、代码块，锁对象分别是什么？

**我的回答（自己的话）：**

> 三种写法，三把不同的锁：
>
> | 写法 | 锁对象 | 实测证据 |
> | :--- | :--- | :--- |
> | `synchronized` 实例方法 | **`this`**（当前实例） | 同一实例两线程：第二个排队 414 ms；两个不同实例：都排队 0 ms |
> | `synchronized` 静态方法 | **`Xxx.class`**（Class 对象） | 静态方法里 `Thread.holdsLock(Xxx.class)==true`，`holdsLock(任意实例)==false` |
> | `synchronized (obj)` | **`obj` 指向的那个对象** | `synchronized(inst)` 持锁 400 ms 时，另一线程调 `inst.work()` 排队 340 ms |
>
> 我用的证据不是背结论，是两条可以直接打印的东西：
> `Thread.holdsLock(obj)`（问 JVM 你现在拿的是谁的锁），
> 以及在方法入口算「从调用到真正进来等了多久」——**被挡住时这个数字不是 0**。
>
> 三个最容易错的推论：
>
> 1. **两个实例的 synchronized 方法互不阻塞**（实测总耗时 409 ms ≈ 一个 400 ms 的量级）。
>    所以「单例里加 synchronized」和「多例里加 synchronized」根本不是一回事。
> 2. **静态锁和实例锁是两把锁**，实测 412 ms 并行。静态同步方法保护不了实例字段 ——
>    这条直接对应 `@Transactional` 拦不住、`@Async` 不生效那一类 bug 的同一个病根。
> 3. **`synchronized (new Object())` 等于没锁**。每次进临界区都是一把新锁，谁也不挡谁：
>    10 线程 × 50 万次实测丢 2846644 / 5000000，而且**比真锁还快**（46 ms vs 339 ms）——
>    快出来的那部分，就是没加锁省下的。
>
> 还有个特别阴的：**锁字符串常量等于拿了一把全局锁**。
> `"LOCK"` 字面量是驻留的，本类的 `"LOCK"` 和另一个类的 `LOCK` 字段 `==` 为 true，
> 实测两边的 `synchronized` 会互相阻塞（排队 354 ms）。
> 锁对象要 `private final Object lock = new Object()` —— 专用、私有、final、不可变。

**追问预判：**
- 那到底该锁什么？（锁「不变的那个东西」：要保护的状态本身，或者一个专用的 `private final` 锁对象。
  锁 `this` 的问题是外部代码也能拿到这个 monitor 跟你抢）
- `synchronized` 能重入吗？怎么实现的？（能。同一线程再进同一把锁，monitor 的持有计数 +1，
  退出 -1，归零才真正释放。实测递归 3 层正常返回；所以「自己等自己」这种死锁不存在）
- 锁升级的过程？（无锁 → 偏向 → 轻量（自旋 CAS）→ 重量（monitor 挂起）。
  但**别背 JDK 8 的八股**：JDK 15 起偏向锁默认关闭（JEP 374），
  实测 JDK 17 `UseBiasedLocking = false {product} {default}`，
  显式 `-XX:+UseBiasedLocking` 只剩一句「deprecated in 15.0」警告；
  JDK 25 直接 `Unrecognized VM option` —— 参数已经删了）
- 那「偏向锁」面试还考吗？（考的是**你知道它没了**。答「JDK 17 起默认关闭，因为撤销偏向的代价
  在大量并发场景下比收益还高」比背「偏向锁把线程 ID 写进 Mark Word」加分得多）
- 方法级和代码块的字节码区别？（方法级没有 `monitorenter`，只有方法 flags `ACC_SYNCHRONIZED`；
  代码块才有指令 + 异常表兜底）

**项目里的实际案例：**

> 场景：一个工具类写了 `public static synchronized void refreshCache()`，
> 同时另一个地方 `synchronized (this)` 改同一份 `static Map` —— 以为是互斥的，其实两把锁，
> 并发下照样丢更新。后来统一成 `private static final Object CACHE_LOCK`，两处都锁它。
> **「锁的是谁」答错，代码看起来完全正常，只有压测才露头。**

---

### 4. 死锁怎么排查？线上只能用 jstack 的时候，你从哪几行看出问题？

**我的回答（自己的话）：**

> 先分成两件事：**怎么发现**、**怎么确认**。
>
> **怎么发现**（程序里）：`ThreadMXBean.findDeadlockedThreads()`。
> 它是 JVM 自己算的锁图环检测，返回死锁线程 id 数组，比人眼看日志靠谱：
>
> ```java
> ThreadMXBean mx = ManagementFactory.getThreadMXBean();
> long[] ids = mx.findDeadlockedThreads();      // null = 没检出
> ThreadInfo[] infos = mx.getThreadInfo(ids, true, true);  // 后两个 true 才带锁信息
> ```
>
> 实测输出（两个线程互相等）：
>
> ```text
> - D1-A-then-B  state=BLOCKED
>     在等 : java.lang.Object @0x5197848c（被 D1-B-then-A 持有）
>     已持有: Object @0x20ad9418（栈深度 0，位置 Deadlock.lambda$sectionD1$0:65）
> - D1-B-then-A  state=BLOCKED
>     在等 : java.lang.Object @0x20ad9418（被 D1-A-then-B 持有）
> ```
>
> 两行对着读就是环：A 等 0x...48c，那把在 B 手里；B 等 0x...418，那把在 A 手里。
>
> **怎么确认**（线上）：`jstack <pid>`，找 `Found one Java-level deadlock:`。
> 原件存在 `w03-concurrency-basics/day2-deadlock-jstack.txt`，关键结构是：
>
> ```text
> Found one Java-level deadlock:
> "D1-A-then-B":
>   waiting to lock monitor 0x...b620 (object 0x...f060, a java.lang.Object),
>   which is held by "D1-B-then-A"
> "D1-B-then-A":
>   waiting to lock monitor 0x...bfc0 (object 0x...f050, a java.lang.Object),
>   which is held by "D1-A-then-B"
> ...
> 	at ...Deadlock.lambda$sectionD1$0(Deadlock.java:65)
> 	- waiting to lock <0x...f060>
> 	- locked <0x...f050>
> Found 1 deadlock.
> ```
>
> 看三样：**谁等谁**（第一段就够）、**业务代码在第几行**（`Deadlock.java:65` 这种）、
> **结尾那行 `Found N deadlock`**（没有这行就说明不是死锁，是别的卡住）。
>
> **几个容易踩的点：**
>
> - **`isAlive() == true` 不等于在干活。** 卡死的线程一直是 BLOCKED，还活着，但不推进。
> - **`blockedCount` 不是等待毫秒数**，是这个线程「进过多少次阻塞」，别拿它算等了多久。
> - **只有一把锁的时候不可能死锁**，死锁至少要两把 + 反向获取。
> - **jstack 抓不到的原因通常是你抓晚了**：进程还在，但你 `jstack` 的时候它已经退出，
>   报 `no such process`。我在 Windows 上还踩了一个：Git Bash 里 `$!` 拿到的是 MSYS 的 pid，
>   跟 Windows 原生 pid 不是同一个数 —— 必须用程序自己打印的 `ProcessHandle.current().pid()`。
>
> **死锁的四个条件**：互斥、持有并等待、不可剥夺、循环等待。破一个就行，
> 工程上最常用的是**破循环等待**：全局定序。
> 实测两个线程按 `identityHashCode` 排序后加锁，2000 轮 × 2 线程 = 4000 次，结果精确 4000、耗时 1 ms、零重试。
>
> **还有一个比死锁更阴的：活锁。** 用 `tryLock(timeout)` 拿不到就放掉重试，看起来是标准解法，
> 但不加退避的话两个线程会踩同一个节拍反复对撞 —— 实测 5 次里 **4 次 2 秒内一个都没跑完**，
> 线程都在跑、CPU 也在烧，就是没人推进。加随机退避后 0~1 次重试就成功。
> **区别**：死锁是确定的（每次必卡），活锁是概率的（有时能自己走出来）——所以它更难排查。

**追问预判：**
- 生产上怎么预防？（① 加锁全局定序；② 用 `tryLock` 带超时，超时就放弃并回滚；
  ③ 缩小锁粒度、不要在锁里调 RPC / 做 IO；④ 关键路径上不做嵌套锁）
- 那 `synchronized` 能设置超时吗？（不能，`synchronized` 只能无限等。
  要超时就得换 `ReentrantLock.tryLock(timeout)`）
- 死锁能被 JVM 自动解开吗？（不能。JDK 没有自动侦测并 kill 的机制，
  只能靠外部监控调用 `findDeadlockedThreads()` 报警，或者人工 dump 线程后重启）
- 线程 dump 里除了 `Found one Java-level deadlock` 还看什么？（看大量 BLOCKED 集中在哪个 monitor、
  看 `at ...` 是否集中在同一个业务方法、看是不是 `waiting on condition`（那是等 IO 或等信号，不是死锁））
- `findDeadlockedThreads` 和 `findMonitorDeadlockedThreads` 区别？（后者只查 synchronized 监视器，
  前者连 `ReentrantLock` 这类 `AbstractOwnableSynchronizer` 一起查，现在用前者）

**项目里的实际案例：**

> 场景：任务调度里两个定时任务，一个先锁任务表再更新用户表，另一个反过来，
> 平时错峰跑没事，某天大促两个撞上，整个调度线程池 8 个线程全 BLOCKED，任务全停。
> 定位方式：`jstack` 抓到的就是上面那个结构，两段 `waiting to lock` 指向对方的 `locked`。
> 修法是统一按表名排序加锁，并给 `tryLock` 加 3 秒超时兜底 —— 超时就放弃本轮、记日志、下一轮再来。
> **教训：锁的获取顺序是一个跨模块的约定，不能各写各的。**

---

### 5.

**我的回答（自己的话）：**

**追问预判：**

**项目里的实际案例：**
