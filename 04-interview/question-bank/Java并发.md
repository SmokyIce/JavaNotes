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

### 5. happens-before 到底是什么？它不是「时间上先发生」，那它保证的是什么？

**我的回答（自己的话）：**

> happens-before 是 JMM 给「可见性 + 有序性」定的**偏序关系**，不是时间先后。
> `A happens-before B` 的意思是：**A 的结果对 B 可见，而且 A 不会被重排到 B 后面**。
> 它说的不是「A 先执行」—— 只要不影响结果，JVM 想怎么重排都行（as-if-serial）。
> 所以两个没有 HB 关系的操作之间的顺序是**不确定的**，这才是并发 bug 的来源。
>
> 八条规则里我实测了四条，每条都配一个「去掉就错」的 Demo（`w03-concurrency-basics/day3`）：
>
> | 规则 | 实验 | 实测 |
> | :--- | :--- | :--- |
> | **程序次序** | Dekker litmus：T1 `x=1; r1=y`，T2 `y=1; r2=x` | 10 万轮里 **(0,0) 出现 16239~40629 次（16%~40%）** —— 两个读都发生在对方写之前 |
> | **monitor** | 写线程锁外写 `data`、锁内写 `flag`；读线程在 `synchronized` 里自旋 | 看见 `flag` 后读到 `data=42`，**且没被 JIT 优化掉** |
> | **volatile** | 同上的发布/订阅，`flag` 换 `volatile` | 80 ms 后立刻看见；对照组（普通 `flag`）自旋 **15 亿次也没看见** |
> | **start / join** | `start()` 前写 `777`；不 `join` 直接读结果 | 前者 100% 读到 777；不 join 时 500 轮里 **151 轮读到 0**（抢跑）；join 后 500/500 正确 |
>
> 第一行最反直觉：**「先写后读」这个顺序在内存看来可以被换过来**。
> 程序次序规则只对**单线程内部**负责（as-if-serial），跨线程什么都不保证。
> x86 只禁止 LoadLoad / LoadStore / StoreStore 重排，**唯独放行 StoreLoad** —— 这正是 (0,0) 的来源。
>
> 第二行证明了一件常被忽略的事：**`synchronized` 不只管互斥，还管可见性**。
> 同一个自旋循环，套上 `synchronized` 就能看见普通字段的更新，脱掉就永远看不见。
>
> volatile 的实现细节（`day3-bytecode.txt`）：
> 两个 DCL 字段的字节码**指令序列完全一样**，差别只在 flags ——
> `0x000a`（PRIVATE|STATIC）vs `0x004a`（多一个 `0x0040` = `ACC_VOLATILE`）。
> 成本在机器码：`set()` 比 `lazySet()` 慢 **5.8 倍**（0.775 ns vs 0.133 ns/次，6 轮取最小值），
> 差的就是 x86 上那条 `lock addl $0x0,-0x40(%rsp)`（StoreLoad 屏障）。
> **volatile 慢在屏障，不慢在指令条数。**

**追问预判：**
- happens-before 有时间含义吗？（没有。它是偏序，不是时钟。两个线程之间没有 HB 边时，
  谁先谁后完全不确定；有 HB 边也不代表先执行，只代表结果可见 + 不许重排）
- 那 `A happens-before B`，B 一定在 A 之后执行吗？（不一定。只要重排不改变单线程的可见结果，
  JVM 就可以重排；HB 约束的是「能观察到什么」，不是「执行顺序」）
- 为什么面试官爱问「volatile 和 synchronized 的区别」？（一句话：volatile 只管可见性 + 有序性，
  不管原子性；synchronized 三个都管。所以 `volatile` 救不了 `count++`）
- `volatile` 读为什么几乎不加指令？（x86 的 TSO 天然不允许 LoadLoad / LoadStore 重排，
  读侧只要编译器别把读提到前面就行；写侧才需要 `lock` 前缀把 store buffer 刷出去）
- `final` 字段也有 HB 保证吗？（有，而且是特殊的：JLS 17.5 规定构造结束前对 final 字段的写，
  对「正确发布」的对象一定可见 —— 这也是不可变对象天然线程安全的原因之一。
  但前提是构造里不能把 `this` 泄露出去）
- 那 DCL 单例为什么不能只靠 final？（见下一题，问题出在「引用」先于「构造」被发布，不在字段上）

**项目里的实际案例：**

> 场景：配置热更新。本地缓存放 `Map<String,String>`，另有一个 `volatile boolean reloaded` 做标志，
> 拉取线程先覆盖 Map 再置标志，业务线程读标志决定要不要重读。
> **为什么 Map 不用 ConcurrentHashMap？** 因为它是「整份替换」而不是「并发改」——
> 写线程换完引用再 `volatile` 置位，读线程看到标志时新 Map 一定已经可见。
> 这其实就是一次标准的安全发布，和 DCL 是同一套道理。
> 如果当初把标志写成普通 `boolean`，就会出现「标志没变、其实数据已经换了」这种查不出的怪事。

---

### 6. 双重检查锁（DCL）为什么必须加 `volatile`？不加到底会发生什么？

**我的回答（自己的话）：**

> `instance = new Singleton()` 在字节码里是三条：`new`（分配）、`invokespecial`（执行构造）、
> `putstatic`（把引用写进字段）。JMM 允许最后一步和中间一步**换序** ——
> 也就是「先把引用挂出去，再跑构造」。这时候第二个线程进来，第一次判空发现 `instance != null`，
> 直接返回一个**构造还没跑完**的对象。
>
> 关键是这个对象不是「全是默认值」，而是**停在中途**。实测抓到的现场：
>
> ```text
> Singleton{a=1, b=null, c=0}
> ```
>
> `a` 已经赋上了、`b` 还是 null。调用方拿到它调 `b.length()` 就是 NPE，
> 而且堆栈指向调用方，跟构造毫无关系 —— **排查方向天然是错的**。
>
> volatile 在这里的作用不是「让 instance 可见」这么模糊，而是**禁止 `putstatic` 被提到 `invokespecial` 前面**
> （StoreStore 屏障）。
>
> 最坑的一点：**这个 bug 在 x86 开发机上测不出来。**
> 实测 50 轮 × 6 线程 × 每轮抢 30 ms，不加 volatile 那版拿到半初始化对象 **0 次**。
> 原因是 x86 是 TSO，store-store 不重排，HotSpot 的 x86 后端也不会把 `putstatic` 搬到构造前面。
> 换了 ARM 就会真的炸。
> **所以判断依据只能是 JMM 的规则，不是「我跑了 50 轮没事」。**
>
> 为了能讲清「重排之后长什么样」，我把重排顺序**手工写出来**（分配 → 发布引用 → 跑构造），
> 读线程立刻抓到了上面那个半初始化对象；
> 另外用 `Unsafe.allocateInstance` 直接造了一个不跑构造的对象，字段全是零值 / null，
> 证明「分配」和「初始化」在内存里确实是两步。

**追问预判：**
- 那 DCL 到底怎么写才对？（`private static volatile Xxx instance;`，两次判空都保留。
  或者干脆不用它：静态内部类 holder、枚举单例、饿汉）
- 静态内部类 holder 为什么不需要 volatile？（靠 `<clinit>` —— JLS 规定类初始化由 JVM 加锁且只执行一次，
  多线程同时触发也只有一条线程跑 `<clinit>`，其余阻塞到它结束，天然带 HB 边。
  **比 DCL 简单，也是我实际项目里更常用的写法**）
- 两次判空分别防什么？（外层防「已经创建好还去抢锁」的性能开销；内层防「两个线程同时通过外层判空、排队拿锁后各造一个」）
- 只加锁不判空行不行？（行，就是每次 getInstance 都要拿锁，单例是热点路径，没必要）
- `volatile` 在这里是禁止重排还是保证可见性？（都要：StoreStore 屏障禁止发布提前；
  volatile 读保证别拿到线程本地缓存的旧引用。一句话：**它给「引用发布」这道关加了栅栏**）
- 单例的字段还要不要 `final`？（要。final 字段有自己的发布保证（JLS 17.5），
  双重保险；不过最稳的还是别把半成品发布出去）
- 除了单例，还有什么地方会踩「不安全发布」？（`this` 逃逸：构造里把 `this` 注册到 listener、
  丢进线程池、放进静态集合 —— 都是同一个病：**对象还没造完，别人已经能用了**）

**项目里的实际案例：**

> 场景：配置中心 SDK 的客户端单例。第一版是 DCL 没加 `volatile`，代码在 x86 开发机和 x86 服务器上都跑得好好的；
> 后来换到 ARM 容器，偶发 `NullPointerException`，栈指向 `config.get("key")` 里对内部 map 的调用，
> 查了两天没人想到是单例 —— 因为 NPE 的位置离「创建」太远。
> 最后改成静态内部类 holder，问题消失。
> **教训：靠「DCL + volatile」这种需要读者懂内存模型的写法，不如换成一看就不会错的 holder。**

### 7. AQS 的核心是什么？为什么入队要 CAS 两次、`park` 之后还要再检查一次？

**我的回答（自己的话）：**

> 三件东西：**一个 `volatile int state`、一条 CLH 双向队列、`LockSupport.park/unpark`。**
>
> `state` 的语义不归 AQS 管，由子类定义 —— `ReentrantLock` 当重入次数、`Semaphore` 当许可数、
> `CountDownLatch` 当倒计数。所以一套框架能撑起整个 `java.util.concurrent`。
>
> `lock()` 的形状是「快路径 + 慢路径」：
>
> ```text
> tryAcquire 成功 → 直接返回
> 失败 → enq() 入队 → 循环：
>          前驱是 head 且 tryAcquire 成功 → setHead（把自己变成哨兵）→ 返回
>          否则自旋几轮 → 还不行就 park
> ```
>
> **为什么两次 CAS**：第一次是队列还不存在时造哨兵 `head`（惰性初始化），第二次是把 `tail` 从旧尾换成自己。
> 两个状态转移各自必须原子；想合并成一个 CAS，就得「同时原子地改 `head` 和 `tail`」，做不到。
> 我手写了一遍对拍：200 万次加锁里**建哨兵 1 次、抢尾 87742 次** —— 一个是「整个进程一次」，
> 一个是「每次排队一次」，量级差得很直观。
>
> **为什么 `park` 之后要重查**：因为 `park` 返回 ≠ 拿到锁。可能被 `unpark` 了但锁已被别人抢走，
> 也可能被中断。实测 175075 次挂起里有 **87500 次「醒来没抢到」** —— 最常见的是
> 「刚 `unlock` 转头又 `lock`」的线程插队（在压测循环里它每轮都干这事）。
> 所以 `park` 必须包在 `for(;;)` 里，醒来第一件事是重试 `tryAcquire`。
> 不重查的后果不是「慢」，是**带着「我以为我有锁」进临界区**，互斥直接失效。
>
> 补一个细节：`unpark` 有**许可**语义 —— 先 `unpark` 后 `park` 不会丢信号，之后的 `park` 立刻返回。
> 所以 AQS 不用像 `wait/notify` 那样担心「叫早了」。

**可能的追问：**

- `state` 为什么用 `volatile int` 而不是 `AtomicInteger`？（热路径上想要一段「读 + CAS」在一条指令序列里，
  不想多一次字段解引用；AQS 内部用 `Unsafe.compareAndSwapInt` 直接改字段。我用 `AtomicIntegerFieldUpdater`
  是为了避开 `Unsafe`，语义一样）
- `tryAcquire` 为什么在 `state == 0` 时还要 CAS，不是直接赋值？（可能有多个线程同时看到 0，
  只有一个 CAS 能赢，其余必须失败 —— 这是整个互斥的地基）
- `unlock()` 里「清 `owner`」和「`state = 0`」这两行的顺序能反吗？（不能。先清 `owner` 再置 0；
  反过来的话，别人可能看到 `state == 0` 抢到锁，然后被这行 `owner = null` 把归属擦掉）
- 公平锁怎么实现的？（`hasQueuedPredecessors()`：队列里有人排队就不许走快路径，直接去队尾）
- 为什么 `tryLock()` 可以不排队？（它走的就是快路径，抢不到立刻返回 `false`，不 park ——
  代价是不加退避会**活锁**，W03 Day 2 实测 5 次里 4 次跑不完）
- `CountDownLatch` 怎么用同一套 AQS？（`state` 初始化为计数，`countDown` 就是 `release(1)`，
  减到 0 时唤醒所有 `shared` 模式的等待者 —— 所以它用的是 AQS 的**共享模式**，不是独占模式）
- 手写版比 `ReentrantLock` 慢多少？为什么？（1 ns 临界区下慢 5.8 倍；把临界区拉到 0.5 µs 只剩 1.13 倍。
  差的那部分 ≈ `park 次数 × 4.2 µs` —— **慢的不是队列，是挂起**。所以 `ReentrantLock` 要拼命自旋）

**项目里的实际案例：**

> 场景：异步任务平台的「并发闸门」。第一版自己拿 `synchronized` + `wait/notifyAll` 实现限流排队，
> 结果是每次有人释放许可，`notifyAll` 把几十个等待线程一起叫醒，抢不到又睡回去 ——
> 队列一长 CPU 就抖（无效唤醒实测能到有效唤醒的几百倍）。
> 后来换成 `Semaphore`（AQS 共享模式的现成实现），一行代码解决。
> **教训：`wait/notifyAll` 是「全员广播」，只要等待者超过两三个，就不该再用它做排队。**

---

### 8. `wait/notify` 有哪三个「必须」？`Condition` 又解决了什么？

**我的回答（自己的话）：**

> **必须一：写在 `synchronized` 里。** 否则 `IllegalMonitorStateException: current thread is not owner`。
> 原因不是语法洁癖 —— `wait` 要做的三件事（释放锁 → 挂起 → 唤醒后重新抢锁）全都围绕 monitor，
> 不在锁里就没有「可释放的东西」。
>
> **必须二：包在 `while` 里，不是 `if`。** 被唤醒只代表「你有机会了」，不代表条件成立。
> 实测：2 个消费者等货，生产者放 **1** 件货然后 `notifyAll()` —— 两个人都被叫醒，货只有一件。
> `if` 版那个空手而归（取到 `null`）**1 次**，`while` 版 **0 次**，而且是「醒来重查、接着睡」。
> 真正的虚假唤醒来自 JVM/OS，没法主动复现，但后果和这个场景完全一样 —— **醒了，条件还是不成立**。
>
> **必须三：多个条件时必须 `notifyAll`。** `notify()` 一次只叫**一个**，而且是「任意一个」。
> 实测（确定性的那种）：一次放 **2** 件货、只喊一声 → 取走 **1 件**、剩 **1 件**、
> 还有 **1 个消费者永久 `WAITING`**。**货还在，人在睡** —— 这就是死锁的雏形。
> 换 `notifyAll()` 就是 2 件全取走、0 个人在等。
> 另外 `notify` 不是信号量：没人在等时喊一嗓子就没了，之后的 `wait(200)` 会等满 200 ms 才超时。
>
> 再补两条常考的：`wait()` 会**释放锁**（旁观者排队 0 ms，而持锁者 `sleep(500)` 时旁观者要等 414 ms）；
> 被 `interrupt()` 打断时 JVM **先清中断标志再抛异常**，所以捕获后要自己 `interrupt()` 一次。
>
> **`Condition` 解决的就是「必须三」**：`synchronized` 只有一个等待集合，只能全员广播；
> `Condition` 能从一个锁上 `newCondition()` 出多个队列，`signal()` 只叫自己那个。
> 实测容量 1 的有界队列、4 生产者 4 消费者：无效唤醒 `Condition` **11 次**，`notifyAll` **7666 次**。

**可能的追问：**

- 为什么 `notify()` 叫醒的那个线程条件可能不成立？（你叫的是「任意一个」，它等的可能是另一个条件；
  或者多个线程等同一个条件，但资源只有一个）
- `wait(0)` 是等 0 毫秒吗？（不是，`0` 表示**永久等待**，和 `wait()` 等价）
- `wait()` 被唤醒后，是直接继续执行 `wait()` 后面那行吗？（不是。它必须**重新竞争锁**，
  拿到锁才从 `wait()` 返回 —— 所以「被唤醒」到「真的继续跑」之间还有一段排队）
- `Condition.await()` 和 `Object.wait()` 什么关系？（语义一一对应；区别是 `Condition` 可以分组，
  还多了 `awaitNanos` / `awaitUninterruptibly` / `signalAll` 这些）
- 为什么 AQS 用 `park` 不用 `wait`？（`park` 不需要持锁、以线程为单位、有许可语义不会丢信号；
  `wait` 必须持锁、以对象为单位、信号会丢。实现一个通用框架，`park` 干净得多）
- 生产者消费者还能怎么写？（`BlockingQueue` —— 内部就是 `ReentrantLock` + 两个 `Condition`，
  即上面这套东西的工程化封装。**面试让手写就用 `while + await/signal`，工程里直接用队列**）

**项目里的实际案例：**

> 场景：任务平台的重试队列。第一版用 `synchronized` + `if (!hasTask) wait();` 拿任务，
> 上线后偶发「明明队列里有任务，worker 却全都在睡」—— 就是 `if` 的问题：被叫醒后没重查条件，
> 或者醒来时任务已经被别人抢走，它拿着 `null` 继续往下跑，看起来像「任务丢了」。
> 改成 `while` 之后消失。
> **教训：`wait` 的 `if` 是自己给自己埋雷，编译器不会拦，测试也很难覆盖 —— 只能靠写的时候守住。**

---

### 9. 线程池提交一个任务，是先开线程还是先入队？把七个参数的联动说一遍。

**我的回答（自己的话）：**

> **核心线程没满 → 直接开线程；核心线程满了 → 先入队；队列满了 → 才开非核心线程；
> 两边都满 → 拒绝。** 顺序是「核心 → 队列 → 非核心 → 拒绝」。
>
> 大多数人会答成「先开到 `maximumPoolSize`，再排队」—— 正好反了。
>
> 实测（`w04-threadpool-completablefuture/day1`，core=2 max=4 queue=2，连提 10 个任务，
> 每个任务都堵在同一道门上，所以状态能稳住，提交一个拍一张快照）：
>
> ```text
>   序号       1     2     3     4     5     6     7     8     9    10
>   线程数     1     2     2     2     3     4     4     4     4     4
>   队列       0     0     1     2     2     2     2     2     2     2
>   结果      ok    ok    ok    ok    ok    ok   REJ   REJ   REJ   REJ
> ```
>
> `#3 #4` 只让队列长了，线程数一动不动；`#5` 是分水岭 —— 队列刚满（2/2），线程数才从 2 跳到 3。
> 最终执行 6 个、拒绝 4 个，峰值线程正好等于 max。
>
> 源码也是这三步（`ThreadPoolExecutor.execute()`）：
>
> ```java
> if (workerCountOf(c) < corePoolSize) { if (addWorker(command, true)) return; }   // 1 核心
> if (isRunning(c) && workQueue.offer(command)) { ... }                            // 2 入队
> else if (!addWorker(command, false)) reject(command);                            // 3 非核心，再失败就拒绝
> ```
>
> 类文档原话：`a new thread will be created to handle the request **only if the queue is full**`。
>
> **为什么这个顺序值得背**：它决定了 `maximumPoolSize` 在什么条件下才生效。
> 队列无界 → 永远不满 → **永远走不到第 3 步**，`max` 就是摆设。
> 实测 core=2 max=4 提交 10 个：峰值线程**恒为 2**，8 个任务全在队列里躺着。
> 所以 `newFixedThreadPool(n)` 的 n 就是真实并发上限，`maximumPoolSize` 形同虚设。
>
> 七个参数的联动，一句话一个：
>
> | 参数 | 管什么 | 坑 |
> | :--- | :--- | :--- |
> | `corePoolSize` | 常驻线程，默认不回收 | 不是「最小线程」，是「先填满的这一档」 |
> | `maximumPoolSize` | 线程上限 | 只在**队列满**之后才起作用 |
> | `keepAliveTime` + `unit` | 空闲回收 | 只管非核心；核心要 `allowCoreThreadTimeOut(true)` |
> | `workQueue` | 排队策略 | **它决定整个池的性格** |
> | `threadFactory` | 怎么造线程 | 默认名 `pool-3-thread-1`，堆栈里什么都看不出来 |
> | `handler` | 满了怎么办 | 默认 Abort（抛异常）；`Discard*` 静默丢，基本不能用 |
> | `runState`（隐含） | 池子生命周期 | `shutdown` 之后所有 `execute` 直接走拒绝 |

**可能的追问：**

- `keepAliveTime` 对核心线程生效吗？（默认不生效。实测 core=2 max=4 queue=1：4 个任务堵住时 `poolSize=3`，
  任务结束后 +400 ms 掉回 2 就停住了；开 `allowCoreThreadTimeOut(true)` 之后才归 0）
- 四种拒绝策略分别什么行为？（Abort 抛 `RejectedExecutionException`；CallerRuns 在**提交线程**里跑，
  实测 `#3#5` 跑在 `main` 上，是天然背压；Discard 静默丢；DiscardOldest 丢队头并**重试**，
  实测只跑了 `#1` 和 `#5` —— 它连丢了 `#2#3#4`，不是只丢一个）
- `newCachedThreadPool` 是什么形态？（core=0、max=`Integer.MAX_VALUE`、`SynchronousQueue`。
  队列容量 0 → 永远「满」→ 每个任务都得现开一个线程接。实测 500 个 60 ms 的任务 → 峰值 500 线程）
- `corePoolSize = 0` 会怎样？（第一个任务会**先进队**，再借一个线程来消化 ——
  `execute()` 第 2 步里的 `else if (workerCountOf(recheck) == 0) addWorker(null, false)`）
- 队列满了、线程也到 max 了，新任务去哪？（走 `handler`。注意 `shutdown` 之后也一样，
  不管队列空不空，`execute` 一律拒绝）
- `SynchronousQueue` 和 `LinkedBlockingQueue` 怎么选？（前者「不存任务、直接交接」，适合任务短、并发高；
  后者能缓冲，但要**指定容量**，否则就是 OOM 蓄水池）

**项目里的实际案例：**

> 场景：任务平台的下发线程池。第一版是 `Executors.newFixedThreadPool(16)`，上线半年没事。
> 大促那天上游推送量翻了 20 倍、下游接口变慢，任务开始在队列里堆 —— 堆到 300 多万个任务、
> 堆内存打满，Full GC 之后直接 OOM，整个应用重启。
> 复盘：`newFixedThreadPool` 的队列是 `LinkedBlockingQueue`（无界），**它永远不拒绝，只会一直堆**，
> 所以「拒绝数」这个指标一直是 0，监控上什么都看不出来。
> 改成 `new ThreadPoolExecutor(16, 32, 60s, new ArrayBlockingQueue<>(2000), namedFactory, AbortPolicy)`，
> 再把「拒绝次数」接到告警上 —— 之后大促会拒绝、会降级，但不会 OOM。
> **教训：无界队列的问题不是「会 OOM」，是「OOM 之前一切正常」。**

---

### 10. `Executors` 的四个工厂方法为什么被阿里手册禁用？线程数到底怎么定？

**我的回答（自己的话）：**

> 禁用理由只有一条：**那四个方法把「队列」和「线程数上限」配成了危险组合，而这两个才是决定生死的参数。**
>
> | 工厂 | 实际配置 | 危险在哪（实测） |
> | :--- | :--- | :--- |
> | `newFixedThreadPool(n)` | core=max=n，`LinkedBlockingQueue` 无界 | 队列无上界：`-Xmx64m` 下每个任务 256 KB，**第 176 个任务**就 OOM，之前拒绝数一直是 0 |
> | `newSingleThreadExecutor()` | 同上，n=1 | 一个慢任务堵死全部：前面 `sleep(500)`，后面 3 个「瞬间完成」的任务等了 **513 ms** |
> | `newCachedThreadPool()` | core=0，max=`Integer.MAX_VALUE`，`SynchronousQueue` | 任务一多线程暴涨：500 个 60 ms 的任务 → **峰值 500 线程** |
> | `newScheduledThreadPool(n)` | `DelayedWorkQueue`（无界） | 同上，排队任务无上界；单线程调度器一个任务卡住全部 |
>
> 对照实验：同样的 64 MB 堆，换成 `ArrayBlockingQueue(50)` + 默认 `AbortPolicy`，
> **拒绝 99948 次、队列稳定在 50、堆稳稳的**。所以问题不在「任务多」，在「没有上界、没人拒绝」。
>
> 正确姿势是自己 `new ThreadPoolExecutor(...)`，四个数想清楚：
>
> 1. **队列必须有界** —— 队列容量就是「允许多少任务在等」，这个数必须是你愿意接受的最大积压；
> 2. **拒绝策略必须出声** —— Abort + 上层兜底，或 CallerRuns 背压；`Discard*` 基本不能用；
> 3. **线程数**：CPU 密集 `N+1`、IO 密集 `N × (1 + 等待/计算)` —— 但这是起点，最终靠压测；
> 4. **线程命名 + 指标** —— 默认名 `pool-3-thread-1` 的 `3` 是全局池序号，跟业务无关；
>    还要把 `getActiveCount` / `getQueue().size()` / 拒绝次数上报，没有指标就没法判断池子够不够。
>
> 另外两个每天都在坑人的点：
>
> - **`submit` 吞异常**：`execute` 里抛的异常走 `UncaughtExceptionHandler`，控制台能看到堆栈，
>   而且线程死了会被替换（实测线程名 `thread-1` → `thread-2`）；`submit` 把异常塞进 `Future`，
>   **没人 `get` 就等于没发生**，一行日志都没有。所以「任务提交了没生效」第一步查 Future 有没有人 get。
> - **线程池里的 `ThreadLocal` 会串味**：线程复用，`ThreadLocal` 挂在线程上。
>   实测单线程池里请求 B 读到了请求 A 的 `用户A的登录信息`，`remove()` 之后才是 `null`。
>   租户 ID / 用户身份 / `TraceId` 必须 `try { ... } finally { remove(); }`。

**可能的追问：**

- 线程池怎么优雅关闭？（`shutdown()` 跑完队列再退出 —— 实测 5 个排队任务全部跑完；
  `shutdownNow()` 返回未执行的 4 个、并**尽力**中断正在跑的 1 个。
  标准姿势：`shutdown()` → `awaitTermination(30s)` → 还没完再 `shutdownNow()`）
- `shutdownNow` 一定能把任务停下来吗？（不一定。它只是 `interrupt()`，任务里不检查中断标志、
  不响应 `InterruptedException` 的照样跑完 —— 所以「中断」是协作式的）
- IO 密集为什么是 `2N`？（`N × (1 + W/C)`，W/C 是等待时间/计算时间。纯 IO 时 W/C ≈ 1 → 2N。
  公式只是起点：真实比值只能压测，压测时看「拒绝率 + 队列长度 + P99」三条线）
- 线程池满了但业务不能丢任务怎么办？（两条路：① `CallerRunsPolicy` 背压，让上游变慢；
  ② 落库/落 MQ 再做异步补偿。前者简单，后者可靠 —— 但要接受「拒绝是常态」这个事实）
- 一个应用应该有几个线程池？（按「业务隔离 + 快慢隔离」拆，别所有任务共用一个池：
  一个慢任务能把整个池占满。同名同参数的池也要拆开，否则一个抖全部抖）
- 为什么 `newFixedThreadPool` 的 `maximumPoolSize` 没用？（因为队列无界，`workQueue.offer` 永远成功，
  代码走不到第 3 步 —— `execute()` 里 `else if (!addWorker(command, false))` 这条分支永不执行）

**项目里的实际案例：**

> 场景：导出功能。第一版直接把导出任务丢进 `newCachedThreadPool()`，平时很快。
> 有一次运营一次性点了 200 个导出，每个都要拉 50 万行 → 200 个线程同时抢数据库连接，
> 连接池瞬间打满，**整个应用的所有接口开始超时**（不只是导出）。
> 复盘：cached 池的 max 是 `Integer.MAX_VALUE`，等于「没有上限」，它不会保护任何下游。
> 改成独立的导出线程池（core=2 max=4 有界队列 20，满了直接给用户提示「当前排队过多，请稍后再试」），
> 并且和业务线程池**物理隔离** —— 之后导出再慢，也不会把主业务拖死。
> **教训：线程池的第一职责不是「跑得快」，是「把故障关在笼子里」。**

### 11. `CompletableFuture` 不传线程池用的是什么？为什么这算事故隐患？

**我的回答（自己的话）：**

> 用的是 `ForkJoinPool.commonPool()`，源码里叫 `ASYNC_POOL`：
>
> ```java
> private static final Executor ASYNC_POOL = USE_COMMON_POOL ?
>     ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();
> ```
>
> 它的关键属性有三个，每一个都是坑：
>
> 1. **线程数 = `max(1, 核数 - 1)`**（`ForkJoinPool.java:2564`）。实测 12 核机器 → 并行度 **11**；
> 2. **全 JVM 唯一** —— 所有 `parallelStream()`、所有没传 executor 的 `supplyAsync` 共用它；
> 3. **它不区分「计算任务」和「阻塞任务」** —— 阻塞住一个线程，就少一个 worker。
>
> 阻塞的代价实测（14 个 `sleep(300)` 任务，并行度 11）：
>
> | 提交到哪 | 耗时 | 现象 |
> | :--- | ---: | :--- |
> | `commonPool` | **623 ms** | 分 2 批跑，多出来的 3 个只能等 |
> | 自建 `newFixedThreadPool(14)` | **308 ms** | 14 个线程同时睡 |
>
> 所以真正的风险不是「慢一点」，是**污染范围**：你在订单服务里 `supplyAsync` 调一个 2 秒的 RPC，
> 占掉的就是这 11 个坑之一；占满之后，**同一 JVM 里跟它毫无关系的 `parallelStream` 一起卡住**。
> 这种故障最难查 —— 报错的接口和闯祸的接口根本没有调用关系。
>
> 规矩：**只要任务里有 IO（查库 / RPC / 文件 / 锁等待），就必须传自己的 executor**，
> 而且要用 `ThreadFactory` 命名（实测 `order-1` / `stock-1`，`jstack` 里一眼能看出是谁）。

**可能的追问：**

- `commonPool` 的线程能不能调大？（能，`-Djava.util.concurrent.ForkJoinPool.common.parallelism=N`，
  但这是在**全局**加线程 —— 治的是「并行度不够」，治不了「阻塞型任务不该进公共池」。
  正确的解法永远是给阻塞任务一个自己的池）
- 核数只有 1 的机器会怎样？（`max(1, 1-1)` = 1，但 `USE_COMMON_POOL` 的判断是
  `getCommonPoolParallelism() > 1`，为 false 时退化成 `ThreadPerTaskExecutor` ——
  一个任务一个 `new Thread`，更糟）
- `parallelStream` 和 `supplyAsync` 真的抢同一个池吗？（是。都走 `ForkJoinPool.commonPool()`。
  所以「我在业务里用一下异步，结果报表的并行流变慢了」是完全可能的）
- 那 `thenApplyAsync` 呢？（不传 executor 时**同样**回到 `commonPool` —— 实测线程名
  `ForkJoinPool.commonPool-worker-1`。这是第二次伏击：你以为换了线程，其实只是换到公共池）
- `thenApply` 不带 Async 就一定在异步线程吗？（不一定。实测上游已完成时，它在**调用者线程**里跑完 ——
  我这里是 `main`。所以「写在异步链里」和「跑在异步线程上」是两件事）

**项目里的实际案例：**

> 场景：商品详情页要并行拉「价格 / 库存 / 促销」三个服务，图省事直接 `supplyAsync` 三次 + `allOf`。
> 平时 80 ms 返回，大促时 P99 飙到 3 秒，而且**同机器的定时任务（用 `parallelStream` 算日报）也跟着卡**。
> 复盘：三个 RPC 都是阻塞调用，全塞进了 `commonPool`（8 核机器只有 7 个 worker），
> QPS 一上来 7 个 worker 全堵在等 RPC 上，后面排队的任务只能干等；
> 更要命的是日报的 `parallelStream` 抢的是同一个池，于是「详情页慢」传导成「日报跑不完」。
> 改成专用的 `price-pool`（core=8 max=32 有界队列 200 + Abort），两件事同时消失。
> **教训：`commonPool` 是给「纯 CPU 的短任务」准备的，任何会阻塞的东西都不属于它。**

---

### 12. `thenApply` 和 `thenCompose` 有什么区别？用错了为什么不报错？

**我的回答（自己的话）：**

> **`thenApply` 的函数返回「值」，`thenCompose` 的函数返回「另一个异步」。**
> 差别就写在签名里：
>
> ```java
> thenApply<U>   (Function<? super T, ? extends U>                   fn)
> thenCompose<U> (Function<? super T, ? extends CompletionStage<U>>  fn)
> ```
>
> `thenCompose` 的参数类型里多了 `CompletionStage` 那一层，于是 `U` 的推导停在「值」上，外面那层被拍平。
>
> **为什么用错了不报错**：`thenApply(user -> fetchOrders(user))` 里 `fetchOrders` 返回
> `CompletableFuture<String>`，`U` 顺理成章被推成 `CompletableFuture<String>`，
> 结果类型成了 `CompletableFuture<CompletableFuture<String>>` —— **这是合法的泛型嵌套，编译器没有理由拦你**。
>
> 实测（`ChainShape` A 段）：
>
> | 写法 | `join()` 的值 | `join()` 的类型 |
> | :--- | :--- | :--- |
> | `thenApply` | `CompletableFuture@<hash>[Completed normally]` | `CompletableFuture` |
> | `thenCompose` | `用户:42 的订单共 3 笔` | `String` |
>
> 也就是说：**它能编译、能跑、不抛异常，只是「值」变成了「装着值的盒子」**，
> 然后这个盒子一路往下传，最后在日志里打印出 `CompletableFuture@1b6d3586` ——
> 线上看到这种东西，八成就是套错了。
>
> 什么时候才会被编译器抓住？只有你显式声明了类型：
>
> ```text
> error: incompatible types: inference variable U has incompatible bounds
>     equality constraints: String
>     lower bounds: CompletableFuture<String>
> ```
>
> 顺带一条同源的坑：**不带 `Async` 的 `thenApply` 不一定在异步线程上跑**。
> 它的规则是「在完成上游的那个线程上执行」，而上游**早已完成**时，「那个线程」就是
> **调用 `thenApply` 的线程**（实测在 `main` 里跑完）。要真异步，必须 `thenApplyAsync` + 自己的池。

**可能的追问：**

- 那 `map` / `flatMap` 跟这是一回事吗？（是同一个形状。`Stream.map` 返回 `R`、`flatMap` 返回
  `Stream<R>`；`Optional.map` 返回 `U`、`flatMap` 返回 `Optional<U>`。
  **凡是「容器套容器」的 API 都有这一对**，认形状比背名字有用）
- `thenCombine` / `allOf` / `anyOf` 分别什么时候用？（两路→`thenCombine`；N 路都要→`allOf`
  （注意返回 `CF<Void>`，结果要自己 `join` 收）；N 路谁先回来算谁→`anyOf`（类型擦成 `Object`）。
  实测：两路各 400 ms 的 `thenCombine` 是 **411 ms**，串行写法 **816 ms**）
- 串行链和并行编排差多少？（实测三次各 200 ms 的 `thenCompose` 链 **631 ms**；
  同样三件事改成三路并行 + `allOf` **215 ms**。**有依赖才串，没依赖就并**）
- `allOf` 之后为什么 `join()` 是 `null`？（签名是 `CompletableFuture<Void>`，
  它只表达「都完成了」，不携带结果。要结果就把 `List<CompletableFuture<T>>` 留着，再 `map(join)`）
- 那用 `thenCompose` 串起来会不会阻塞？（不会，`thenCompose` 只是「等上一个完成再接下一个」，
  不占线程。真正会占线程的是拿 `.join()` 的返回值在中间插一脚 —— 那就退化成阻塞等待了）

**项目里的实际案例：**

> 场景：下单流程写成「校验 → 扣库存 → 发券 → 通知」，四步都返回 `CompletableFuture`，
> 有人用 `thenApply` 一路接下去，测试全过、日志正常，就是最后拿到的对象
> `toString()` 是 `CompletableFuture@6d06d69c`。
> 排查花了半天：因为中间每一步都「成功」了，没有任何异常，
> 只是每隔一层就多套一个盒子，最后 `.get()` 拿到的是盒子不是数据。
> 换成 `thenCompose` 之后一次 `join()` 到位。
> 后来在 `Code Review` 清单里加了一条硬规矩：**看到 `thenApply` 的 lambda 里有 `Async` 后缀的方法调用，
> 必须确认返回类型** —— 因为那几乎一定是 `thenCompose` 该出现的地方。
