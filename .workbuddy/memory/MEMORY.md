# 项目长期记忆 · E:\Notes

## 这是什么

**Java 进阶学习工程**（每天 60 分钟，8K → 25K），NoteSync 同步 GitHub，iPad 上读。

**进度制：里程碑制，不是周次制。** 唯一进度来源 `00-plan/MILESTONES.md`；`today.md` 每日时间盒（开工先看、收工覆盖、末尾写「下一个时间盒」）；`java-48week-plan.md` 降级为知识点字典；`PROGRESS.md` 只在封板时勾。

**三条规则**：① 60 分钟产出「知识点 / 代码 / 笔记」任意两样即算数，只有「没打开 IDE」才算失败；② **取消补课** —— 断档后从当天该做的那一小步接着走；③ 只细化当前与下一个里程碑。

## 当前状态（2026-11-27）

M0 封板 10-27；**M1 封板 11-20**；M2 · Spring 内核进行中：**W05 封板 11-25**，**W06 AOP 进行中**。

| 周 | 状态 |
| :--- | :--- |
| W01 集合 | ✅ `MyHashMap` 对拍 20 万次 0 处不一致 |
| W02 泛型/异常/Optional/Lambda/Stream/收集器 | ✅ 封板 11-03 |
| W03 并发（4 天） | ✅ 封板 11-11 |
| W04 线程池 / CompletableFuture | ✅ 封板 11-19 |
| W05 Spring IOC/DI（5 天） | ✅ 封板 11-25 |
| W06 Spring AOP | 🟨 D2 完成 11-27（D1 代理套在 `wrapIfNecessary:328`、分界 `DefaultAopProxyFactory:58`、`this` 1 / `self` 2；**四个知识点全勾**） |

- **下一步：W06 Day 3 = 手写 mini AOP**（`ProxyFactory` + `MethodInterceptor` + `Advisor`，不用 `@Aspect`）；
  Day 4 收尾封板（照 W05 `BoardCheck` 把结论写成断言）。
- 题库：`Java基础.md` 24、`Java并发.md` 12、`Spring.md` **14**（M2 只要 6 道，已超额）。
- 已知待办：w05 的 `pom.xml` 已写但**本机没 Maven、未验证**；装 Maven 后要和 `run.sh day5.BoardCheck` 对撞。

## 目录职责

`00-plan/` 计划里程碑 ｜ `01-notes/stageX-basics/WNN-主题.md` 每周一份笔记 ｜ `02-code/stageX-.../wNN-topic/` 每周一个可运行工程，跨周实验放 `lab/` ｜ `03-project/` 长期项目 ｜ `04-interview/` 题库 / 难题 / 模拟面试 / 简历 ｜ `05-resources/` cheatsheets / templates

## 工程约定与环境

- 目录小写 ASCII `wNN-topic`；包名 `com.newt.java48.wNN`；**Maven 布局**（`src/main/java`）不挪
- 本机没 Maven/Gradle → 纯 `javac` + `run.bat` / `run.sh`；`run.sh` 的 `-cp` 用 **Windows 分号**（Git Bash 里也别改冒号）；短名只认 `day1`，day2 起写全包名
- JDK 有 1.8/17/23/25，**默认 17**；中文输出要 `-encoding UTF-8` + `-Dfile.encoding=UTF-8`
- Bash 工具 PATH 可能损坏：先 `export PATH="/c/Program Files/Java/jdk-17/bin:$PATH"`
- PowerShell 不回显 stdout → 写临时文件再 Read；读 JDK 源码 `jar xf src.zip java.base/java/util/X.java`（前缀必须 `java.base/`）
- javac 中文报错在 Git Bash 是 GBK 乱码 → 加 `-J-Duser.language=en -J-Duser.country=US`；`compile-errors/` 注释必须英文
- 加了新文件后**必须刷新 `sources.txt`**，否则 `ClassNotFoundException`
- 笔记骨架：知识点 / 核心问题 / 代码·实验 / 踩坑 / 面试 / 一句话总结 / 打卡。**「核心问题」「一句话总结」默认留空给学员**，答案写文末「参考要点」

## 依赖 / jar 备忘

- w05、w06 的 `lib/`：Spring **5.3.39** 的 core / beans / context / expression / aop / **spring-jcl**（Spring 5 自带 jcl，**不是** commons-logging）+ `javax.annotation-api-1.3.2`（JDK 11+ 已移除 `javax.annotation`，`@PostConstruct` 编译不过）
- **Spring 5 用 `javax.*`，Spring 6 才换 `jakarta.*`**，下错包不报错、只是注解静默不执行
- `@Aspect` 需要 **`aspectjweaver`**（1.9.x），放在 `lib/`，与 spring-aop 一起上 classpath
- `@Configuration` 被 CGLIB 代理（`$$EnhancerBySpringCGLIB$$…`），CGLIB 在 `spring-core` 内，无需额外 jar
- **手写 CGLIB 的包名是 `org.springframework.cglib`**（Spring 5 重打包），不是 `net.sf.cglib`；`lib/` 里没有独立 cglib jar

## 讲课最值钱的实测结论

**集合**：「链表 8 转红黑树」缺两前提 —— 容量 ≥ 64 且 key 可比较；key 不可比较时 JDK 比手写链表慢 6.7 倍。

**泛型 / 异常**：擦除擦实参留声明，实参藏在子类 `Signature`；CCE 抛在 `get` 不在 `add`；`new RuntimeException(e)` 是切栈不是丢栈；**`finally` 里的 `return` 才真把异常变没**；异常开销 447 ns → 31 ns。

**Stream**：`Predicate` 的 `or().and()` 是左结合 `(a||b)&&c`（**不报错只少数据**）；`toMap` 走 `merge`（value 不许 null、key 允许）；「不可变」≠「不许 null」；`count()` 跳过整条管道连 `peek` 一起；上游带 `SORTED` 时 `sorted()` 是空操作。

**并发（W03）**：`count++` 四条字节码，窗口在 `iadd`→`putfield`；volatile 与普通自增**指令完全相同**，差别只在 `ACC_VOLATILE`；三种写法三把锁（this / Class / 括号里的对象），证据用 `holdsLock` + **排队时长**；**`synchronized(new Object())` 比真锁快 7 倍且丢 284 万** ——「加锁反而更快」几乎一定是锁没生效；`static synchronized` 保护不了实例字段（`@Transactional`/`@Async` 失效的同一病根）；`tryLock` 不加退避会活锁；**DCL 的坑是「引用先于构造发布」**，x86 上永远测不出来；`notify` 一次只叫一个（2 件货喊一声 → 1 人永久 WAITING）；手写 AQS：**「park 返回 ≠ 拿到锁」**（200 万次里 87500 次醒来没抢到），慢的不是队列是挂起（≈4.2 µs/次）；公平锁换的是**上界**不是更短；`Condition` 存在的唯一理由是「分多个等待队列」。

**线程池（W04 D1）**：任务顺序 **核心 → 队列 → 非核心 → 拒绝**（线程 1→2→2→2→3→4→4、队列 0→0→1→2→2，JDK 原话 `only if the queue is full`）；**队列决定 max 是否生效**（无界队列 → 非核心永不开）；无界队列不是「会 OOM」是「**OOM 之前一切正常**」（第 176 个任务才炸）；**`submit` 吞异常**（`execute` 打印且换线程）；`ThreadLocal` 串味必须 `fixed(1)`。

**CompletableFuture（W04 D2）**：默认 `commonPool`、并行度 = 核数-1、**全 JVM 唯一**（14×300 ms：623 ms vs 自建池 308 ms）；`thenApply` 用错**编译通过**只得到 `CF<CF<T>>`；不带 Async 的 `thenXxx` 跑在「完成上游的线程」上；`allOf` 返回 **null**；**`whenComplete` 吞不掉异常**（上游失败时原始异常胜出，新异常只进外层 suppressed；`exceptionally` 里抛 → 原始异常彻底丢失）；`join` 抛 `CompletionException`、`get` 抛 `ExecutionException`；`orTimeout` **不中断**后台任务。

**Spring IOC/DI（W05 D1）**：价值是「**3 个调用点 → 1 个装配点**」；手写版真实代价是 **3 个连接池**；「谁 new 的」读栈（`instantiateClass:213` / `preInstantiateSingletons:955` / `getSingleton:234` 是 `singletonFactory.getObject()`）；Bean 在 `refresh()` 里造完；单例池 定义 13 / 实例 19，**原型造完连账都不记**；循环依赖差别只有三行顺序（582 造 → 613 暴露 → 619 灌）；**注解不是元数据是注册指令**。

**Spring AOP（W06）**：代理在 `wrapIfNecessary:328` 套上，选型一句 `isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces()`；
`this.xxx()` 用纯 JDK 代理就能证（业务跑 3 方法、handler 只进 1 次）；`self` 是三层，证据只能用 `TargetSource`（类名会戳穿）。
**`within(接口)` 命中 0 且不报错**（`within` 问「方法声明在哪个类」）；写宽到 `execution(* …pkg..*.*(..))` 会连
`@Configuration` 的 `@Bean` 方法都拦 —— `..` 是「**零个或多个**子包」—— 容器 `BeanCurrentlyInCreationException` 起不来，
线索是栈里 `<Cfg>$$EnhancerBySpringCGLIB.shop(<generated>)`；`@Aspect` 反而不受影响（`isInfrastructureClass` 用 `isAspect` 豁免）。
`args()` 看运行时实参、`execution` 看声明类型，同一个 `buy(Object)` 两边结论相反；`args` 还会把切点变成运行时切点（无缓存）。
`@AfterReturning` 换引用无效、改对象有效；`@AfterThrowing` 吞不掉（`:70 throw ex` 是源码里明写的）；
`returning`/`throwing` 类型接不住就**静默跳过**；通知抛新异常顶掉原异常且 `cause=null`·`suppressed=0`（框架不管挂）。
`@Order` 数字小的在外层，不写时按**注册顺序**（`AspectJPrecedenceComparator:84` 跨切面相等返回 0，靠排序稳定性保原序）。
CGLIB 父类就是目标类；final 类 `Cannot subclass final class`；MethodProxy 7.5 ns vs 反射 20.6 ns vs 直接 0.4 ns；
**Spring CGLIB Bean 构造器只跑 1 次**（`ObjenesisCglibAopProxy:62` 绕过构造器，`:70-84` 才是兜底）。

**Spring 生命周期（W05 D2）**：①构造 → ②`BPP.before` → ③`@PostConstruct` → ④`afterPropertiesSet` → ⑤`initMethod` → ⑥`BPP.after`；销毁 ⑦`@PreDestroy` → ⑧`destroy` → ⑨`destroyMethod`；**`@PostConstruct` 是后处理器调的**；属性注入**没有回调**；④ 早于 ⑤ 且重名只跑一次；销毁逆序且**依赖方先死**；**prototype 销毁 0 次**（`!isPrototype()` 在登记阶段就短路）；`@Bean` 默认 `INFER_METHOD` **自动调 `close()`**；BPP 计数表：正常 Bean **2/2**、同批 BPP 与提前创建的 Bean **0/0**；提前创建 → `not eligible for auto-proxying` = **代理套不上 = 事务/异步必然失效**。

**Spring 作用域（W05 D3）**：`singleton` / `prototype` **不在 `scopes` 表里**，是 `doGetBean` 写死的两个 `if`（覆盖会被拒 `Cannot replace existing scopes`）；四条判据：singleton 是·是·`1+1`·正常，prototype 否·否·`0+0`·正常；`request` 报 `No Scope registered for name 'request'`，**refresh 不报、getBean 才报**；`@Scope(proxyMode)` 是**把解析推迟到调用时**（单例注入原型 `1/1/1` → `2/3/4`，定义拆成 `scopedTarget.xxx` + `ScopedProxyFactoryBean` 单例）；自定义作用域的销毁回调交给 `scope.registerDestructionCallback`，`ctx.close()` **只销毁单例**；`SimpleThreadScope` 就是一个 `NamedThreadLocal` 且**不支持销毁回调只打 WARN**。

**Spring 注入取舍（W05 D4）**：挑人是**四级链** —— `@Qualifier` 缩候选 → `@Primary` → `@Priority` → 名字匹配 → `resolveNotUnique`；**`@Autowired` 不纯按类型**，多候选按**字段名**回退，而 `@Primary` 在名字之前；`@Resource` 默认名 = 字段名，**显式点名时不查类型**（`No bean named 'x' available`），默认名查不到才退回按类型；`List` 按 `@Order`、**`Map` 不排序**（key = bean 名）；**集合不是兜底**：`required=true` 零候选照样抛、`required=false` 给 **null**；注入失败死在 **`refresh()`**；`ObjectProvider` 三局面；构造器参数名回退要 `-parameters`。

**Spring 封板自检（W05 D5）**：`day5.BoardCheck` 把四天结论写成 **23 条断言**（FAIL 即 `exit=1`，实测 23/23），**断言化**让「结论」变成「回归用例」。两条新结论：① 属性注入的位置能**钉死**（`@Autowired` setter 打点：`ctor → inject → bpp.before → @PostConstruct`，`populateBean` 在 582/619/620 之间）；② 「两种注解报同一个错」只对了内层 —— `@Autowired` 外层 `UnsatisfiedDependencyException`（713 行），`@Resource` 外层 `BeanCreationException` + `Injection of resource dependencies failed`（332 行）。

## 工作方式上的硬教训（每条都踩过两次以上）

- **断言先于实测**：反复栽（`sorted` 屏障、`Predicate` 优先级、`monitorenter`、`bppSecond` 的 1/1、栈行号 154 vs 157）。写进注释/笔记的「结论」先跑一遍。
- **别按上一个类的 API 猜这一个类**（`SimpleThreadScope` 就没有 `SCOPE_NAME` 常量）；要写常量名先 `grep` 源码。
- **引行号必须说清「声明 / 语句 / 调用点」**；栈帧行号指向**调用点**。
- **不同脚手架的数字不能放一张表**；统一脚手架 + 开头 `warmup()` 再计时。
- **靠概率的实验不能当结论**（`notify` 叫错人 20 轮只丢 2 次）—— 要构造必然发生的场景。
- **并发实验先让人睡着再发生事件**；`join`/`wait`/IO **不许写在 `synchronized` 里**。
- **数字接近 0 一律当测量事故**（JIT 会消掉没人看的写）；微基准多轮取最小值 + 方法调用防消除。
- **自旋上限必须远大于等待时间**；可见性试验三件套：线程握手 + 循环体不许有方法调用 + 让目标线程先被编译。
- **并发 demo 各段会互相污染**（别复用已死锁的锁对象）；**静态计数器不归零，后一段就替前一段付账**，分段实验开工先清零。
- **复制粘贴的函数签名最容易留下**（方法体返回 `Throwable`、签名还是上下文类型 → 4 个编译错）。
- **`jstack` 要在一条命令内**「起进程→等→读 `ProcessHandle.current().pid()`→jstack」；Git Bash 的 `$!` 是 MSYS pid；线程名只用 ASCII。
- 收集多个编译错误要**按 javac 阶段拆文件**（attribution 报错会让 flow 整段跳过）。
- **`catch (OutOfMemoryError)` 里不能直接 `printf`**（二次 OOM）：先预留内存，catch 到就释放 + `gc()` 再打。
- **自指统计会把针脚藏起来**（数源码 `new X(` 时别把针脚写在被数文件里）。
- **`getBeanDefinitionNames()` + `getBean(name)` 会触发构造**（把 `@Lazy`/prototype 提前造掉）→ 读注册表用 `getBeanDefinition(name).getBeanClassName()`。
- **`GenericApplicationContext` 只能 `refresh` 一次**。
- **Spring 栈行号能和 sources jar 对撞**：下 `spring-*-5.3.39-sources.jar` → `jar xf` → 逐行核对。「读过源码」和「看过总结」的区别就在这一步。
- **「同类」≠「同实例」≠「同一层」**：JDK 代理类按接口列表缓存，两个不同代理会共用一个 `$Proxy`；
  证明包装层级要看 `TargetSource` / `getTarget()`，不能看类名。
- **会抖的输出不能当证据**：别粘身份哈希、别粘 `[Not completed]` 这类状态；要打类型用 `getClass()`。
- **JUL logger 名是子类**（`LogFactory.getLog(getClass())`），按父类名设 `Level.OFF` 压不掉 → 先枚举 `LoggerNames` 查真名。

## 断档与恢复的口径

已出现三次断档（09-22→10-09、10-10→10-27、11-20→11-23）＋两次周末空档。学员回来后只说「把落后的补上」，不解释原因。正确做法：**按 MILESTONES 现状直接续做下一步 + 同步所有过期文件**，不问「这些天怎么了」，不安排补课。

## 学员偏好

- 技术细节导向，不要冗长叙述；喜欢「结论 + 实测数字 + 反直觉点」
- 「带我完成第 N 天」= 当天时间盒 + 可运行代码 + 笔记回填，不是讲一段道理
- 喜欢**反直觉但可验证**的结论；「本地跑不出来」接受「物理原因」解释
