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
