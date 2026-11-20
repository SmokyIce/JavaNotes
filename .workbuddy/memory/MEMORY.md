# 项目长期记忆 · E:\Notes

## 这是什么

用户的 **Java 进阶学习工程**（每天 1 小时，8K → 25K），用 NoteSync 同步到 GitHub，iPad 上读。

**进度制：里程碑制（2026-10-09 起），不是周次制。**

| 文件 | 作用 |
| :--- | :--- |
| `00-plan/MILESTONES.md` | **唯一进度来源**。M0~M6，不设周数 |
| `00-plan/today.md` | 每日时间盒；开工先看，收工覆盖，末尾写「下一个时间盒」 |
| `00-plan/java-48week-plan.md` | 降级为知识点字典 |
| `00-plan/PROGRESS.md` | 降级，里程碑封板时顺手勾 |

**三条规则**：① 60 分钟产出「知识点/代码/笔记」任意两样即算数，「没打开 IDE」才算失败；
② **取消补课**，断档后从当天该做的那一小步接着走；③ 只细化当前和下一个里程碑。

## 当前状态

**M0 已封板（10-27）；M1 已封板（11-20）；当前在 M2 · Spring 内核。**

- W01 集合 ✅（`MyHashMap` 对拍 20 万次 0 处不一致）
- W02 泛型/异常/Optional/Lambda/Stream/收集器 ✅ **已封板（11-03）**
- W03 并发 ✅ **已封板（11-11）**：Day 1 线程基础 / Day 2 锁的是谁 /
  Day 3 volatile + happens-before + DCL / Day 4 `Lock` + 手写 AQS + `wait-notify`
- W04 线程池/CF ✅ **已封板（11-19）**：Day 1 线程池七个参数 + 任务四步顺序 + Executors 四个工厂的坑 /
  Day 2 `commonPool` 并行度 = 核数-1 + `thenApply` vs `thenCompose` + 异常链三种写法
- W05 Spring IOC/DI 🟨 **Day 1 已完成（11-20）**：手工装配 vs 容器装配（3 处 → 0 处 / 3 份仓储 → 1 份）、
  谁 new 的（栈指纹）、什么时候 new 的（`refresh` 内）、循环依赖（字段能解 / 构造器不能）
- **下一步：W05 Day 2 = Bean 生命周期**（六个回调打时间戳 + 两个 `BeanPostProcessor`；
  之后 Day 3 作用域，Day 4~ AOP 交给 W06）
- 题库：`Java基础.md` 24 题、`Java并发.md` 12 题、`Spring.md` **3 题**（M1 只要 8 道，已远超）

## W05 工程（唯一需要 jar 的工程）

- `lib/` 六个 jar，Spring **5.3.39**：core / beans / context / expression / aop / **spring-jcl**
  （Spring 5 自带 jcl，**不是** commons-logging；照老教程下后者会多一个 jar，也能跑）
- `run.sh` 的 classpath 是 `out;lib/*`（Windows 分号，Git Bash 里也别改冒号）；`javac -cp "lib/*"`
- `@Configuration` 被 CGLIB 代理（`AppConfig$$EnhancerBySpringCGLIB$$…`），CGLIB 在 `spring-core` 内，无需额外 jar
- `pom.xml` 仍未补（本机没 Maven，写了验证不了）→ 推到 W05 收尾

## 目录职责

| 目录 | 放什么 |
| :--- | :--- |
| `00-plan/` | 计划、里程碑、进度、能力矩阵 |
| `01-notes/` | 每阶段一目录，每周一份 `WNN-主题.md`（文件名保留 WNN，不对应考核） |
| `02-code/` | 每周一个可运行工程 `stageX/wNN-topic/`；跨周实验放 `lab/` |
| `03-project/` | 长期项目「异步任务管理平台」（迭代制） |
| `04-interview/` | `question-bank/` 题库、项目难题、模拟面试、简历 |
| `05-resources/` | `cheatsheets/`、`templates/` |

## 工程约定

- 目录小写 ASCII `wNN-topic`；包名 `com.newt.java48.wNN`；**Maven 布局**（`src/main/java`），
  即使暂时不需要依赖 —— W05 补 `pom.xml` 即可，不挪文件
- **本机没有 Maven/Gradle**：W01~W05 用纯 `javac` + `run.bat` / `run.sh`（第 2 个参数透传给程序）；
  W05 起 classpath 带 `lib/*`，Windows 分隔符是 `;`（Git Bash 里也别改冒号）
- JDK 目录有 1.8 / 17 / 23 / 25，**默认 JDK 17**
- 中文输出：编译 `-encoding UTF-8`，运行 `-Dfile.encoding=UTF-8`
- 笔记骨架：`一、本周知识点 / 二、核心问题 / 三、代码·实验 / 四、踩坑与 Bug 复盘 / 五、面试会怎么问 / 六、一句话总结 / 每日打卡`
- **「核心问题」「一句话总结」默认留空给学员**，答案写文末「参考要点」；只有学员说「补上落后内容」时才写成口述版
- 每天最小标准：① 一个知识点 ② 一点代码 ③ 一份自己的笔记

## 环境注意

- Bash 工具默认 PATH 可能损坏：先 `export PATH="/c/Program Files/Java/jdk-17/bin:$PATH"` 之类
- PowerShell 工具不回显 stdout，要输出就写临时文件再 Read
- 读 JDK 源码：`jar xf ".../jdk-17/lib/src.zip" java.base/java/util/HashSet.java`（前缀必须是 `java.base/`）
- javac 中文报错在 Git Bash 下是 GBK 乱码，靠 `文件:行号:` 定位
- **`-XX:+PrintAssembly` 本机能跑**，但只有十六进制、没有助记符；`f0 83 44 24 c0 00` = `lock addl $0x0,-0x40(%rsp)`

## 讲课最值钱的实测结论（细节在各周笔记里）

- **集合**：「链表 8 个转红黑树」缺两个前提 —— 容量 ≥ 64 且 key 可比较；key 不可比较时 JDK 比手写链表慢 6.7 倍。
- **泛型**：擦除擦实参、留声明，实参藏在子类 `Signature`；CCE 抛在 `get` 不在 `add`。
- **异常**：`new RuntimeException(e)` 是切栈不是丢栈；**`finally` 里的 `return` 才是真的把异常变没**。
- **Stream**：`Predicate` 的 `or().and()` 是左结合 `(a||b)&&c`，**不报错只少数据**；
  `toMap` 走 `HashMap.merge`（value 不许 null、key 允许）；「不可变」≠「不许 null」；
  `reversed()` 反整条链，`nullsLast` 会被翻成 `nullsFirst`。
- **并发（W03 三天）**：
  ① `count++` 四条字节码，窗口在 `iadd`→`putfield`；`incVolatile` 与 `incPlain` 指令**完全相同**，
     差别只在 `ACC_VOLATILE` 标志 —— volatile 加的是内存语义，不是指令条数。
  ② 「本地跑不出来」有物理解释：JIT 预热后单线程几十微秒，唤醒 10 线程要上百微秒，**没重叠就没有竞态**。
     复现两要素 = 工作量够长 + 同时起跑。
  ③ 三种写法三把锁（this / Class / 括号里的对象），证据用 `Thread.holdsLock` + **排队时长**（别用总耗时）。
     **`static synchronized` 保护不了实例字段** = `@Transactional`/`@Async` 失效的同一个病根。
  ④ **`synchronized(new Object())` 比真锁快 7 倍且丢 284 万** ——「加了锁反而更快」几乎一定是锁没生效。
  ⑤ 死锁：`findDeadlockedThreads()` 发现 / `jstack` 确认（成对 `waiting to lock` + 行号 + `Found N deadlock`）/
     全局定序破（2000 轮 1 ms）；**`tryLock` 不加退避会活锁（4/5）**，死锁确定、活锁概率。
  ⑥ 偏向锁：JDK 17 默认关（JEP 374），JDK 25 参数已删。别背 JDK 8 的四段升级。
  ⑦ **happens-before 是偏序不是时钟**：Dekker litmus 10 万轮 `(0,0)` 占 16%~40%（x86 只放行 StoreLoad）；
     `synchronized` 也管可见性；volatile 发布/订阅 80 ms 后 5/5 看见，普通标志 15 亿次看不见。
  ⑧ **DCL 的坑是「引用先于构造发布」**，抓到的是 `{a=1,b=null,c=0}`（停在半路，不是全默认值）；
     **x86 上永远测不出来**（TSO 不重排 store-store），判断只能靠 JMM。
  ⑨ `notify` 一次只叫一个：放 2 件货只喊一声 → 取走 1、剩 1、1 个消费者永久 WAITING
     （「货还在、人在睡」是死锁雏形）；`notify` 也不是信号量，没人在等时空喊就消失，
     之后 `wait(200)` 等满 200 ms。`wait` 释放锁 vs `sleep` 抱锁：旁观者排队 0 ms vs 414 ms。
  ⑩ 手写 AQS（state + CLH + park）200 万次零丢失：建哨兵 CAS 1 次 / 抢尾 87742 / park 175075 /
     **醒来没抢到 87500** —— 这最后一个数就是「park 返回 ≠ 拿到锁」的实证，最凶的插队者
     是「刚 unlock 转头又 lock」的线程。
  ⑪ 手写版 vs 内置：临界区 1 ns 时慢 5.8 倍，拉到 0.5 µs 只慢 1.13 倍；差额 ≈ park × 4.2 µs。
     **慢的不是队列，是挂起** —— 这解释了 `ReentrantLock` 为什么拼命自旋。
  ⑫ 公平锁慢 41 倍、插队 154582 vs 445，但**最长等待反而更大**（582 vs 387 µs）：
     临界区 1 ns 时它等的是「唤醒交接」而不是临界区。公平换来的是**有上界**，不是更短。
     `Condition` vs `notifyAll` 无效唤醒 11 vs 7666 —— Condition 存在的唯一理由就是「分多个等待队列」。

- **线程池（W04 Day 1）**：
  ① 任务顺序 **核心 → 队列 → 非核心 → 拒绝**（实测线程数 1→2→2→2→3→4→4，队列 0→0→1→2→2，
  `#5` 是分水岭 = 队列刚满）；JDK 原话 `only if the queue is full`。教科书反着的那个答案就是错的。
  ② **队列决定 max 是否生效**：无界队列永远不满 → 永不开非核心（实测 max=4、peak 恒 2）。
  ③ 1 万个任务：`new Thread` 620~796 ms vs 池 12~13 ms（几十倍，量级稳、倍数飘），≈62 µs/线程。
  ④ 无界队列不是「会 OOM」，是「**OOM 之前一切正常**」：`-Xmx64m` 下第 176 个任务才炸、拒绝数一直 0；
  有界 50 + Abort → 拒绝 99948 次不 OOM。
  ⑤ **`submit` 吞异常**（不 get 一个字不打，`execute` 打印且线程被替换 `thread-1`→`thread-2`）；
  `ThreadLocal` 在池里串味（必须 `fixed(1)` 才能复现）；`keepAlive` 只管非核心线程。

- **CompletableFuture（W04 Day 2）**：
  ① 不传 executor 就是 `ForkJoinPool.commonPool()`，并行度 = `max(1, 核数-1)`（12 核 → 11），
     **全 JVM 唯一**（和 `parallelStream` 同一个池）。14×300 ms 阻塞任务：公共池 623 ms vs 自建池 308 ms
     —— 阻塞的代价是「排队」（`ceil(14/11)×300`），不是变慢。`thenApplyAsync` 不传池也回 commonPool。
  ② `thenApply` 返回「值」、`thenCompose` 返回「异步」；用错**编译通过**，只得到 `CF<CF<T>>`，
     `join()` 拿到的是 Future 对象（要 join 两次）。和 `map`/`flatMap` 同构。
  ③ 不带 `Async` 的 `thenXxx` 在「完成上游的那个线程」上跑 —— 上游已完成时就是**调用者线程**（实测 main）。
  ④ 编队：combine 两路 400 ms → 411 ms（串行 816）；`allOf` 返回 **null**、耗时=最慢那路；
     `anyOf` 类型擦成 `Object`；三次串联 631 ms vs 三路并行 215 ms。
  ⑤ **`whenComplete` 吞不掉异常**（`BiConsumer` 返回 void，没有出口）。三段实测：上游失败+它抛 →
     **原始异常胜出**，新异常只进**外层** `suppressed`（=1，`uniWhenComplete` 的 `addSuppressed`）；
     上游成功+它抛 → 新异常生效；`exceptionally` 里抛 → **原始异常彻底丢失**（cause 与 suppressed 都没有）。
  ⑥ `join` 抛 `CompletionException`、`get` 抛 `ExecutionException`（且先拆一层）；嵌套越套越深，
     `exceptionally`/`handle` 里拿到的 `ex` 就是 `CompletionException`，业务类型判断必须 `unwrap()`（while）。
  ⑦ `exceptionally` 的返回值必须接 —— CF 不可变；`orTimeout` 只让 CF 提前完成，**不中断**后台任务。

- **Spring IOC/DI（W05 Day 1）**：
  ① **IOC 的价值不是「少写 new」，是「3 个调用点 → 1 个装配点」**：手写版三个入口各 `new` 一次
     （`new JdbcOrderRepository` 3 / `new PriceCalculator` 3），Spring 版入口代码 0；
     但 `@Bean` 里仍然 `new`（实测 1 次）——「一个装配点」≠「零个 new」。
  ② **手写版的真实代价是 3 个连接池**：A 段造出 3 个仓储实例（哈希三个不同），集中装配后 1 个。
     这条比「改 3 处」硬 —— 它会体现在连接数、缓存命中率、计数器上。
  ③ **「谁 new 的」只能在栈上读**：手写版 `直接调用者 = ManualWiring.webEntry:28`；
     Spring 版 `BeanUtils.instantiateClass:213` + `createBeanInstance:1222/1232` + `preInstantiateSingletons:955`
     + `getSingleton:234`。这几个行号**都能在 5.3.39 的 sources jar 里对上**（1222 = autowireConstructor
     构造器注入 / 1232 = instantiateBean 无参构造，同一份输出里两种都出现）。
  ④ **Bean 在 `refresh()` 里就造完了**：构造 `+312~338 ms`，「容器创建完成」`+354 ms`；
     那 312 ms 是类加载 + ASM 扫包，不是 `new`（对象几十微秒）。`preInstantiateSingletons`
     三个条件：非 abstract + singleton + 非 lazy。
  ⑤ **单例池的三个数**：定义 13 / 实例 19（13 里 2 个没造 = 11，另有 8 个「有实例没定义」的容器内部单例）；
     取过 prototype + lazy 之后定义仍 13、单例只 +1 → **原型造完连账都不记**。
  ⑥ **循环依赖的差别只有三行顺序**：`createBeanInstance`(582) → `addSingletonFactory`(613) →
     `populateBean`(619)。字段注入吃到「先造空壳再灌」的时间差（实测构造时字段全 null、之后 `b.a == a`）；
     构造器注入参数先于对象 → `BeanCurrentlyInCreationException`（判据：`singletonsCurrentlyInCreation.add` 失败就抛）。
  ⑦ **注解不是元数据是注册指令**：同一个类 `@Autowired` 字段，`new` 出来是 `null`，注册给容器后是 `jdbc`。
     不写 `@Component` 的两种失败：`NoSuchBeanDefinitionException`（根）/ `UnsatisfiedDependencyException`（被包一层，
     多一句 `expected at least 1 bean which qualifies as autowire candidate`）。

## 工作方式上的硬教训（每条都踩过两次以上）

- **断言先于实测**：已连续五天抓到自己「先写结论再验证」（`sorted` 屏障、`Predicate` 优先级、
  `monitorenter`、活锁必然性、hsdis 缺失）。**写进代码注释/笔记的「结论」，先跑一遍。**
- **不同脚手架的数字不能放一张表比**（11-11 踩）：内联自增 vs 包一层 lambda，同一份活测出
  22 ms 与 107 ms，结论从「差 15 倍」改成 5.8 倍。先跑的那个实现还背了全部 JIT 预热成本 ——
  **统一脚手架 + 开头 `warmup()` 再计时**。
- **靠概率的实验不能当结论**（11-11 踩）：想演示 `notify()` 叫错人，20 轮只丢 2 次
  （JVM 大多时候恰好挑中该醒的那个）。**要证明「丢了」，得构造必然丢的场景。**
- **并发实验先让人睡着再发生事件**（11-11 踩）：把「放 2 件货」写成初始状态，
  消费者进临界区时货已在架上，谁都没 `wait` —— 实验根本没成立。
- **`join()` / `wait()` / IO 不许写在 `synchronized` 里**：11-11 持锁 `join` 消费者，
  而消费者要重新拿锁才能退出临界区 —— 自己把自己卡住（固定等满超时）。
- **数字接近 0 一律当测量事故**：JIT 会消掉「没人看」的写（普通字段写 0.019 ns/次）。
  微基准要多轮取最小值，且用方法调用（`set` vs `lazySet`）防止消除。
- **自旋上限必须远大于等待时间**（这条踩了两次：3 亿 ≈ 200 ms 撞上 200 ms 等待；5 亿 ≈ 170 ms 撞上 200 ms）。
- **可见性试验三件套**：线程握手（确认已在自旋）+ 循环体里**不许有方法调用**（`nanoTime` 会挡住 JIT 提升）
  + 让目标线程先被 JIT 编译完（写线程先睡 80~200 ms）。
- **同步工具会改变时序**：`CyclicBarrier` 每轮同步会把两个线程错开（分布一边倒），
  要测重排得用自旋 gate，并且加一个「真的重叠了吗」的探针（如 `(1,1)` 计数）。
- **并发 demo 各段会互相污染**：不要复用上一段已死锁的锁对象；每段自带干净状态。
- **`jstack` 抓现场**：一条命令内「起进程 → 等 → 读程序打印的 `ProcessHandle.current().pid()` → jstack」；
  **Git Bash 的 `$!` 是 MSYS pid，不是 Windows 原生 pid**。线程名只用 ASCII。
- 收集多个编译错误要按 javac 阶段拆文件（attribution 先跑，flow 会被整段跳过）。
- **`catch (OutOfMemoryError)` 里不能直接 `printf`**：打印本身要分配对象，堆已满 → 二次 OOM，
  控制台只剩 `thrown from the UncaughtExceptionHandler`。先预留 4 MB，catch 到就 `reserve = null; System.gc();` 再打。
- **用线程池做实验也会被调度骗**：想演示「线程复用 / ThreadLocal 串味」，必须 `fixed(1)`，
  否则两个任务落到不同线程，「复用」根本没发生（这个坑和「并发实验先让人睡着」是同一个毛病）。

- **自指统计要把针脚藏起来**（11-20 踩）：数「源码里 `new X(` 出现几次」时，针脚字符串写在被数的文件里，
  统计代码自己就被数进去（SpringWiring 明明 0 个 new，数出 3）。改成在工具类内部拼 `"new " + name + "("`。
- **`getBeanDefinitionNames()` + `getBean(name)` 会触发构造**（11-20 踩）：想枚举 bean 名单就用了 `getBean`，
  结果把 `@Lazy` 和 prototype 提前造了，后面「refresh 时没造」的结论直接不成立。
  读注册表用 `getBeanDefinition(name).getBeanClassName()`。
- **`GenericApplicationContext` 只能 refresh 一次**（11-20 踩）：`new ...(Config.class)` 之后再 `register + refresh`
  → `does not support multiple refresh attempts`。要动态加定义：先 `new AnnotationConfigApplicationContext()`，
  `register(配置类, 额外类)`，再 `refresh()`。
- **同名重载看栈必须对行号**（11-20 踩）：`getSingleton` 有两个重载，栈上的 234 是 `singletonFactory.getObject()`
  （真正开始造对象），查三级缓存的在 180 行起。差 50 多行，看错结论就反了。
- **Spring 的栈行号能和 sources jar 对撞**：下 `spring-beans-5.3.39-sources.jar` → `jar xf` → 行号逐一对上
  （1222/1232/955/234/582/613/619 全部验证过）。「读过源码」和「看过总结」的区别就在这一步。

## 断档与恢复的口径

已出现两次 17 天断档（09-22→10-09、10-10→10-27）。学员回来后只说「把落后的内容补充上」，
不解释原因。正确做法：**按 MILESTONES 现状直接续做下一步 + 同步所有过期文件**，
不问「这些天怎么了」，不安排补课。

## 学员偏好

- 技术细节导向，不要冗长叙述；喜欢「结论 + 实测数字 + 反直觉点」
- 「带我完成第 N 天」= 当天时间盒 + 可运行代码 + 笔记回填，而不是讲一段道理
- 喜欢**反直觉但可验证**的结论；对「我本地跑不出来」这类问题接受「物理原因」解释
- **会抖的输出不能当证据**（11-12 踩 S4 一次，11-19 又踩）：`nested.join().toString()` 里是
  `[Not completed]` 还是 `[Completed normally]` 取决于内层任务跑没跑完。举证「类型」要打 `getClass()`；
  要打状态就先 `join()` 到底。文档里也别粘 `@1b6d3586` 这种身份哈希（每次运行都变）。
- **测异常结构先想清楚测的是哪一层对象**（11-19 踩）：`e.getSuppressed()` 与 `e.getCause().getSuppressed()`
  是两个数，我打了后者得 0，与源码 `addSuppressed` 矛盾了半天，其实新异常挂在外层包装上。
- 收集编译错误要加 `-J-Duser.language=en -J-Duser.country=US`，否则 Git Bash 下全是 GBK 乱码；
  `compile-errors/` 里的注释也必须是英文（javac 会回显源码行）。
