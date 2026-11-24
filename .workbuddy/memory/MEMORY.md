# 项目长期记忆 · E:\Notes

## 这是什么

**Java 进阶学习工程**（每天 60 分钟，8K → 25K），NoteSync 同步 GitHub，iPad 上读。

**进度制：里程碑制，不是周次制。** 唯一进度来源 `00-plan/MILESTONES.md`；`today.md` 每日时间盒
（开工先看、收工覆盖、末尾写「下一个时间盒」）；`java-48week-plan.md` 降级为知识点字典；`PROGRESS.md` 只在封板时勾。

**三条规则**：① 60 分钟产出「知识点 / 代码 / 笔记」任意两样即算数，只有「没打开 IDE」才算失败；
② **取消补课** —— 断档后从当天该做的那一小步接着走；③ 只细化当前与下一个里程碑。

## 当前状态（2026-11-24）

M0 封板 10-27；**M1 封板 11-20**；当前 **M2 · Spring 内核**。

| 周 | 状态 |
| :--- | :--- |
| W01 集合 | ✅ `MyHashMap` 对拍 20 万次 0 处不一致 |
| W02 泛型/异常/Optional/Lambda/Stream/收集器 | ✅ 封板 11-03 |
| W03 并发（4 天） | ✅ 封板 11-11 |
| W04 线程池 / CompletableFuture（2 天） | ✅ 封板 11-19 |
| W05 Spring IOC/DI | 🟨 Day 1（11-20 IOC/DI）+ Day 2（生命周期）+ Day 3（11-23 作用域）+ **Day 4（11-24 注入取舍）完成** |

- **下一步：W05 Day 5 = 收尾封板**（第四格「Spring 到底在帮你管理什么」写成三段：对象图 / 生命周期 / 边界；
  `pom.xml` 结账；题库抽 Q1/Q4/Q8 脱稿；MILESTONES 标 W05 🟩 → 开 W06 AOP）。
- 题库：`Java基础.md` 24、`Java并发.md` 12、`Spring.md` **9**（M2 只要 6 道，已超额）。
- 已知待办：`pom.xml` 推到 W05 收尾（本机没 Maven，写了验证不了）。

## W05 工程（唯一需要 jar 的工程）

- `lib/` **7 个 jar**，Spring **5.3.39**：core / beans / context / expression / aop / **spring-jcl**
  （Spring 5 自带 jcl，**不是** commons-logging）+ **javax.annotation-api-1.3.2**（Day 2 加，见下）
- `run.sh`：`javac -cp "lib/*"`、`java -cp "out;lib/*"`（**Windows 分号，Git Bash 里也别改成冒号**）；
  **短名只认 `day1`，day2 起的入口必须写全包名** `day2.LifecycleOrder`
- `@Configuration` 被 CGLIB 代理（`AppConfig$$EnhancerBySpringCGLIB$$…`），CGLIB 在 `spring-core` 内，无需额外 jar
- **JDK 11+ 移除了 `javax.annotation`** → `@PostConstruct` 编译都过不了，必须补 `javax.annotation-api`；
  **Spring 5 用 `javax.*`，Spring 6 才换 `jakarta.*`**，下错包不报错、只是注解不执行（静默失效）

## 目录职责

`00-plan/` 计划里程碑 ｜ `01-notes/` 每阶段一目录、每周一份 `WNN-主题.md` ｜
`02-code/` 每周一个可运行工程 `stageX/wNN-topic/`，跨周实验放 `lab/` ｜
`03-project/` 长期项目「异步任务管理平台」｜ `04-interview/` 题库 / 难题 / 模拟面试 / 简历 ｜
`05-resources/` cheatsheets / templates

## 工程约定

- 目录小写 ASCII `wNN-topic`；包名 `com.newt.java48.wNN`；**Maven 布局**（`src/main/java`）不挪
- 本机没 Maven/Gradle → 纯 `javac` + `run.bat` / `run.sh`（第 2 个参数透传给程序）
- JDK 有 1.8/17/23/25，**默认 17**；中文输出要 `-encoding UTF-8` + `-Dfile.encoding=UTF-8`
- 笔记骨架：知识点 / 核心问题 / 代码·实验 / 踩坑 / 面试 / 一句话总结 / 打卡
- **「核心问题」「一句话总结」默认留空给学员**，答案写文末「参考要点」；只有学员说「补上落后内容」才写口述版

## 环境注意

- Bash 工具 PATH 可能损坏：先 `export PATH="/c/Program Files/Java/jdk-17/bin:$PATH"`
- PowerShell 不回显 stdout → 写临时文件再 Read
- 读 JDK 源码：`jar xf ".../jdk-17/lib/src.zip" java.base/java/util/HashSet.java`（前缀必须 `java.base/`）
- javac 中文报错在 Git Bash 是 GBK 乱码 → 加 `-J-Duser.language=en -J-Duser.country=US`；`compile-errors/` 注释必须英文
- `-XX:+PrintAssembly` 能跑但只有十六进制：`f0 83 44 24 c0 00` = `lock addl $0x0,-0x40(%rsp)`
- 加了新文件后**必须刷新 `sources.txt`**，否则 `ClassNotFoundException`

## 讲课最值钱的实测结论

**集合**：「链表 8 转红黑树」缺两前提 —— 容量 ≥ 64 且 key 可比较；key 不可比较时 JDK 比手写链表慢 6.7 倍。

**泛型 / 异常**：擦除擦实参留声明，实参藏在子类 `Signature`；CCE 抛在 `get` 不在 `add`；
`new RuntimeException(e)` 是切栈不是丢栈；**`finally` 里的 `return` 才真把异常变没**；异常开销 447 ns → 31 ns。

**Stream**：`Predicate` 的 `or().and()` 是左结合 `(a||b)&&c`（**不报错只少数据**）；
`toMap` 走 `merge`（value 不许 null、key 允许）；「不可变」≠「不许 null」；
`count()` 跳过整条管道连 `peek` 一起；上游带 `SORTED` 时 `sorted()` 是空操作。

**并发（W03）**
- `count++` 四条字节码，窗口在 `iadd`→`putfield`；`incVolatile` 与 `incPlain` 指令**完全相同**，
  差别只在 `ACC_VOLATILE` —— volatile 是内存语义不是指令条数。
- 「本地跑不出来」有物理原因：JIT 预热后单线程几十微秒，唤醒 10 线程上百微秒，**没重叠就没竞态**。
- 三种写法三把锁（this / Class / 括号里的对象），证据用 `holdsLock` + **排队时长**（别用总耗时）；
  **`static synchronized` 保护不了实例字段** = `@Transactional`/`@Async` 失效的同一病根。
- **`synchronized(new Object())` 比真锁快 7 倍且丢 284 万** ——「加锁反而更快」几乎一定是锁没生效。
- 死锁：`findDeadlockedThreads()` 发现 / `jstack` 确认 / 全局定序破；**`tryLock` 不加退避会活锁（4/5）**。
- 偏向锁 JDK 17 默认关（JEP 374），JDK 25 参数已删，别背 JDK 8 四段升级。
- **happens-before 是偏序不是时钟**：Dekker 10 万轮 `(0,0)` 占 16%~40%（x86 只放行 StoreLoad）；
  volatile 发布 80 ms 后 5/5 看见，普通标志 15 亿次看不见。
- **DCL 的坑是「引用先于构造发布」**，抓到 `{a=1,b=null,c=0}`；**x86 上永远测不出来**（TSO 不重排 store-store）。
- `notify` 一次只叫一个：放 2 件货喊一声 → 取走 1、剩 1、1 个消费者永久 WAITING；
  没人在等时空喊就消失；`wait` 释放锁 vs `sleep` 抱锁：0 ms vs 414 ms。
- 手写 AQS 200 万次零丢失：抢尾 87742 / park 175075 / **醒来没抢到 87500** ——「park 返回 ≠ 拿到锁」；
  临界区 1 ns 慢 5.8 倍、0.5 µs 只慢 1.13 倍，差额 ≈ park × 4.2 µs —— **慢的不是队列是挂起**。
- 公平锁慢 41 倍、插队 154582 vs 445，但**最长等待反而更大**（582 vs 387 µs）：换的是**上界**不是更短；
  `Condition` vs `notifyAll` 无效唤醒 11 vs 7666 —— 它存在的唯一理由是「分多个等待队列」。

**线程池（W04 D1）**：任务顺序 **核心 → 队列 → 非核心 → 拒绝**（线程 1→2→2→2→3→4→4、队列 0→0→1→2→2，
JDK 原话 `only if the queue is full`，教科书反着的那个答案就是错的）；**队列决定 max 是否生效**
（无界队列 → 非核心永不开，max=4 峰值恒 2）；1 万任务 `new Thread` 620~796 ms vs 池 12~13 ms；
无界队列不是「会 OOM」是「**OOM 之前一切正常**」（第 176 个任务才炸、拒绝数一直 0）；
**`submit` 吞异常**（`execute` 打印且换线程）；`ThreadLocal` 串味必须 `fixed(1)`。

**CompletableFuture（W04 D2）**：默认 `commonPool`、并行度 = 核数-1（12 核 → 11）、**全 JVM 唯一**；
14×300 ms 阻塞任务公共池 623 ms vs 自建池 308 ms（代价是排队）；`thenApply` 用错**编译通过**只得到 `CF<CF<T>>`；
不带 Async 的 `thenXxx` 跑在「完成上游的线程」上；`allOf` 返回 **null**；**`whenComplete` 吞不掉异常**
（上游失败时原始异常胜出、新异常只进**外层** suppressed=1；`exceptionally` 里抛 → 原始异常彻底丢失）；
`join` 抛 `CompletionException`、`get` 抛 `ExecutionException`；`orTimeout` **不中断**后台任务。

**Spring IOC/DI（W05 D1）**：价值是「**3 个调用点 → 1 个装配点**」（手写 `new X` 3 次 → 入口 0 次，
但 `@Bean` 里仍 `new` 1 次）；手写版真实代价是 **3 个连接池**；「谁 new 的」读栈
（`instantiateClass:213` / `createBeanInstance:1222·1232` / `preInstantiateSingletons:955` /
`getSingleton:234` —— **234 是 `singletonFactory.getObject()`，查三级缓存的是 180 行起另一个重载**）；
Bean 在 `refresh()` 里造完（构造 +312~338 ms、容器 +354 ms，那 300 ms 是类加载扫包）；
单例池 定义 13 / 实例 19，取过 prototype+lazy 后 13 / 20 → **原型造完连账都不记**；
循环依赖差别只有三行顺序（582 造 → 613 暴露 → 619 灌）；**注解不是元数据是注册指令**。

**Spring 生命周期（W05 D2）**：顺序 ①构造 → ②`BPP.before` → ③`@PostConstruct` → ④`afterPropertiesSet`
→ ⑤`initMethod` → ⑥`BPP.after`；销毁 ⑦`@PreDestroy` → ⑧`destroy` → ⑨`destroyMethod`
（`+375.7 / 386.7 / 389.0 / 389.5 / 390.2 / 390.5`，refresh 在 +408.7 返回）；
**`@PostConstruct` 是后处理器调的**（`InitDestroy…:157` ← `applyBPPBefore:440` ← `initializeBean:1796`；
载体 `CommonAnnotationBeanPostProcessor:205`）；属性注入**没有回调**，靠「构造 null / 初始化有值」夹住；
④ 早于 ⑤（`invokeInitMethods:1863` vs `1874`）且重名只跑一次；销毁逆序且**依赖方先死**；
**prototype 销毁 0 次**（`registerDisposableBeanIfNecessary:1942` 的 `!isPrototype()` 就短路，连登记都没有）；
`@Bean` 默认 `INFER_METHOD`，**自动调 `close()`**；BPP 计数表：正常 Bean **2/2**、
同批 BPP 与提前创建的 Bean 全 **0/0**（「先全部 `getBean` 收集、再统一注册」`PostProcessorRegistrationDelegate:257~265`）；
提前创建 → 启动日志 `not eligible for auto-proxying` = **代理套不上 = 事务/异步必然失效**。

**Spring 作用域（W05 D3）**：`singleton` / `prototype` **不在 `scopes` 表里**，是 `doGetBean` 写死的两个 `if`，
第三个 `else` 才查表 —— 覆盖会被拒（`Cannot replace existing scopes`，`registerScope:1045`），
`getRegisteredScopeNames()` 只列出自己登记的；四条判据（两次 `getBean` 同否 / 进池 / 销毁 / 非 Web 容器）：
singleton 是·是·`1+1`·正常，prototype 否·否·`0+0`·正常，request/session 取不到；
`request` 报 `No Scope registered for scope name 'request'`（`doGetBean:369`），**refresh 不报、getBean 才报**
（不是单例 → `preInstantiateSingletons` 不碰）；`@Scope(proxyMode)` 是**把解析推迟到调用时**：
单例注入原型三次 `id()` 从 `1/1/1` 变 `2/3/4`，实现是注册期拆成 `scopedTarget.xxx`（真身带 scope）+
外层 `ScopedProxyFactoryBean` 单例；`request` + 代理后 `getBean` 不抛、首次调用抛同一 ISE
（栈多 `SimpleBeanTargetSource:35`）；自定义作用域的销毁回调交给 `scope.registerDestructionCallback`
（`:1952~1960` else 分支，prototype 在 1944 就短路），`ctx.close()` **只销毁单例**；
`SimpleThreadScope` 就是一个 `NamedThreadLocal`，且**不支持销毁回调只打 WARN**。

**Spring 注入取舍（W05 D4）**：挑人是**四级链**（`DefaultListableBeanFactory:1633-1655`）——
`@Qualifier` 先缩候选 → `@Primary` → `@Priority` → 名字匹配 → `resolveNotUnique`（1369）；
**`@Autowired` 不纯按类型**，多候选时按**字段名**回退（`wechatChannel` 零注解就注入成功），
而 `@Primary` 在名字之前（同样字段名 → `preferred`）；`@Resource` 默认名 = 字段名，
**显式点名时不查类型**（`No bean named 'channel' available`），默认名查不到才退回按类型（同一个 `NoUnique`）；
`List` 按 `@Order`、**`Map` 不排序**（源码 Map 分支无排序，key = bean 名）；
**集合不是兜底**：`required=true` 零候选照样抛，`required=false` 给 **null** 不是空集合；
注入失败死在 **`refresh()`** 不是 `getBean`；`ObjectProvider` 三局面（原型每次新 / `getIfAvailable` 不兜多候选 /
`getIfUnique` 吞成 null）；构造器参数名回退要 `-parameters`（默认只剩 `arg0`）。

## 工作方式上的硬教训（每条都踩过两次以上）

- **断言先于实测**：本月反复栽（`sorted` 屏障、`Predicate` 优先级、`monitorenter`、活锁必然性、
  `bppSecond` 的 1/1、栈行号 154、`SimpleThreadScope.SCOPE_NAME`、`:1942` vs `:1944`）。
  **写进注释/笔记的「结论」，先跑一遍。**
- **别按上一个类的 API 猜这一个类**：`ConfigurableBeanFactory.SCOPE_SINGLETON` 有、`SimpleThreadScope` 就没有常量。
  要写常量名/静态字段，先 `grep` 源码。
- **引行号必须说清「声明 / 语句 / 调用点」**（`1942` 是声明、`1944` 才是那句 `if`；`154` vs `157`）。
- **不同脚手架的数字不能放一张表**（内联自增 vs 包 lambda：22 ms vs 107 ms，结论从 15 倍改成 5.8 倍）；
  统一脚手架 + 开头 `warmup()` 再计时。
- **靠概率的实验不能当结论**（`notify` 叫错人 20 轮只丢 2 次）——要证明「丢了」得构造必然丢的场景。
- **并发实验先让人睡着再发生事件**；`join`/`wait`/IO **不许写在 `synchronized` 里**（自己把自己卡住）。
- **数字接近 0 一律当测量事故**（JIT 会消掉没人看的写，0.019 ns/次）；微基准多轮取最小值 + 方法调用防消除。
- **自旋上限必须远大于等待时间**（踩过两次：3 亿 ≈ 200 ms 撞 200 ms；5 亿 ≈ 170 ms）。
- **可见性试验三件套**：线程握手 + 循环体**不许有方法调用**（`nanoTime` 会挡 JIT 提升）+ 让目标线程先被编译。
- **同步工具会改变时序**（`CyclicBarrier` 把两线程错开）→ 用自旋 gate + 「真的重叠了吗」探针（`(1,1)` 计数）。
- **并发 demo 各段会互相污染**：别复用已死锁的锁对象。
- **`jstack` 抓现场**要在一条命令内「起进程→等→读 `ProcessHandle.current().pid()`→jstack」；
  **Git Bash 的 `$!` 是 MSYS pid**；线程名只用 ASCII。
- 收集多个编译错误要按 javac 阶段拆文件（attribution 报错会让 flow 整段跳过）。
- **`catch (OutOfMemoryError)` 里不能直接 `printf`**（二次 OOM）：先预留 4 MB，catch 到就释放 + `gc()` 再打。
- **用线程池做实验也会被调度骗**：演示线程复用 / `ThreadLocal` 串味必须 `fixed(1)`。
- **自指统计把针脚藏起来**（数源码 `new X(` 时把针脚写在被数文件里 → 0 个 new 数出 3）。
- **`getBeanDefinitionNames()` + `getBean(name)` 会触发构造**（把 `@Lazy`/prototype 提前造掉）→ 读注册表用
  `getBeanDefinition(name).getBeanClassName()`。
- **`GenericApplicationContext` 只能 `refresh` 一次**（`does not support multiple refresh attempts`）。
- **同名重载看栈必须对行号**；**栈帧行号指向调用点不是方法声明行**（154 vs 157）。
- **Spring 栈行号能和 sources jar 对撞**：下 `spring-*-5.3.39-sources.jar` → `jar xf` → 逐行核对。
  「读过源码」和「看过总结」的区别就在这一步。
- **会抖的输出不能当证据**：别粘 `@1b6d3586` 这种身份哈希、别粘 `[Not completed]` 这种状态；
  要打类型用 `getClass()`，要打状态先 `join()` 到底。

## 断档与恢复的口径

已出现三次断档（09-22→10-09、10-10→10-27、11-20→11-23）。学员回来后只说「把落后的补上」，不解释原因。
正确做法：**按 MILESTONES 现状直接续做下一步 + 同步所有过期文件**，不问「这些天怎么了」，不安排补课。

## 学员偏好

- 技术细节导向，不要冗长叙述；喜欢「结论 + 实测数字 + 反直觉点」
- 「带我完成第 N 天」= 当天时间盒 + 可运行代码 + 笔记回填，不是讲一段道理
- 喜欢**反直觉但可验证**的结论；「本地跑不出来」接受「物理原因」解释
- 每天最小标准：① 一个知识点 ② 一点代码 ③ 一份自己的笔记
