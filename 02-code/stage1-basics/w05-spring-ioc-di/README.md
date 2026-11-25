# W05 · Spring IOC / DI

> M2 · Spring 内核的第一周，回答一个最基础也最容易被跳过的问题：
> **Spring 到底在帮你管理什么？**
>
> 不是"帮你 new 对象"这么简单 —— 是**把「谁依赖谁」从代码里搬到配置里**，
> 于是换实现不动业务、加依赖不动入口。

## 一、怎么跑（本机没有 Maven）

依赖是手工放在 `lib/` 的 7 个 jar，Spring 版本 **5.3.39**：

| jar | 大小 | 作用 |
| :--- | ---: | :--- |
| `spring-core` | 1,490,485 B | 基础设施 + 自带 CGLIB（`@Configuration` 要被它代理） |
| `spring-beans` | 706,684 B | `BeanFactory` / 三级缓存 / `doCreateBean` / `initializeBean` |
| `spring-context` | 1,276,093 B | `AnnotationConfigApplicationContext` / `@ComponentScan` / `CommonAnnotationBeanPostProcessor` |
| `spring-expression` | 294,249 B | SpEL（本次没直接用到，但 context 依赖它） |
| `spring-aop` | 385,322 B | context 的编译期依赖 |
| `spring-jcl` | 25,161 B | Spring 5 自带的日志桥（**不是** commons-logging） |
| `javax.annotation-api` | 26,586 B | Day 2 新增：`@PostConstruct` / `@PreDestroy`（JDK 11+ 已移出 JDK） |

```bash
./run.sh                      # 默认跑 SpringWiring
./run.sh ManualWiring         # 短名 → day1.ManualWiring
./run.sh day1.ContainerQuestions
./run.sh day2.LifecycleOrder  # 带包名 → day2 的入口必须写全
./run.sh day2.PostProcessorTrace
./run.sh day3.ScopeMatrix
./run.sh day3.WebScopeTrap
./run.sh day4.InjectionChoice
./run.sh day4.CollectionInject
./run.sh day4.CtorInject
./run.sh day5.BoardCheck      # 封板自检：23 条断言，FAIL 时 exit=1
```

`run.bat` 同款。`run.sh` 里 classpath 是 `out;lib/*`（Windows 分隔符是 `;`，Git Bash 下也别改成 `:`）。

> `pom.xml` 已于 Day 5 补上，**但没有验证过**（本机没有 Maven）—— 它是一个「声明」，不是「证据」。
> 装好 Maven 后第一件事是拿它和 `run.sh` 做一次对撞：两条路都要打出 `23 / 23 条通过`、exit=0，
> 对不上说明 pom 有问题。`lib/` 的 7 个 jar 里，`spring-context` 一个坐标就传递带进另外 5 个 spring-*，
> `javax.annotation-api` 必须显式写（它不是 Spring 的传递依赖）。

## 二、目录

```text
w05-spring-ioc-di/
├── lib/                         7 个 jar，见上表
├── pom.xml                      Day 5 补：声明式依赖（**未用 mvn 验证过**，见上）
├── run.sh / run.bat             javac -cp "lib/*" + java -cp "out;lib/*"
├── day1-result.txt              三段实验的完整实测（355 行）
├── day1-spring-source.txt       Spring 源码取证（行号能和栈对上）
├── day2-result.txt              Day 2 两段实验的实测（140 行）
├── day2-spring-source.txt       Day 2 源码取证（7 段：initializeBean / 销毁登记 / BPP 注册…）
├── day3-result.txt              Day 3 两段实验的实测（154 行）
├── day3-spring-source.txt       Day 3 源码取证（7 段：doGetBean 作用域分支 / registerScope / SimpleThreadScope / ScopedProxyUtils…）
├── day4-result.txt              Day 4 三段实验的实测（145 行）
├── day4-spring-source.txt       Day 4 源码取证（8 段：doResolveDependency / determineAutowireCandidate / Map 不排序 / @Resource 回退条件…）
├── day5-result.txt              Day 5 封板自检的实测（85 行，23 条断言）
├── day5-spring-source.txt       Day 5 源码取证：713 vs 332（两种注解外层包装为什么不同）
└── src/main/java/com/newt/java48/w05/
    ├── support/
    │   ├── Trace.java           「谁 new 的、什么时候 new 的」打点
    │   ├── Src.java             数源码里 new Xxx( 出现几次
    │   ├── Stacks.java          「这个回调是谁调的」——从栈上读
    │   └── Quiet.java           压掉 refresh 失败的 JUL 噪音（logger 名是枚举出来的）
    ├── day1/
    │   ├── ManualWiring.java    A 三处各自 new / B 集中装配 / C 量化改动点
    │   ├── SpringWiring.java    A 谁+何时 / B 单例 / C 装配行数 / D 换实现 / E @Lazy / F prototype
    │   ├── ContainerQuestions.java  Q1 不写注解 / Q2 注解对 new 无效 / Q3 循环依赖 / Q4 单例池
    │   ├── AppConfig.java       @Configuration + @ComponentScan
    │   ├── MemoryConfig.java    「换实现」版配置：排除 jdbc，@Bean 出 memory
    │   ├── app/                 同一套业务类（Controller / Job / Test / Service / Repo / Calc）
    │   └── cycle/               FieldA⇄FieldB（字段注入）、CtorA⇄CtorB（构造器注入）
    ├── day2/
        ├── LifecycleOrder.java  A 六回调时间线 / B 逆序销毁 / C prototype / D 推断 close()
        ├── LifecycleBean.java   ①构造 ③@PostConstruct ④afterPropertiesSet ⑤⑥⑦⑧⑨ 全挂在身上
        ├── LifecycleBpp.java    插进 ② 与 ⑥ 两个钩子
        ├── LifecycleConfig.java @Bean(initMethod, destroyMethod) + static 声明后处理器
        ├── LifecycleRepo.java   属性注入的观测点（构造时 null / 初始化时有值）
        ├── Alpha.java / Beta.java            依赖与被依赖 → 销毁顺序
        ├── PrototypeBean.java   prototype 的销毁回调计数（恒 0）
        ├── AutoCloseBean.java   有 close() 但没声明 destroyMethod → 被推断调用
        ├── ManualOffBean.java   destroyMethod = "" → 关掉推断
        ├── PostProcessorTrace.java A 计数表 / B @PostConstruct 栈 / C 三追问 / D Ordered
        ├── TraceBpp.java        会计数的后处理器（自己带 @PostConstruct 做对照）
        ├── EarlyDependent.java  被后处理器 @Autowired → 提前创建 → 拿不到代理
        ├── PlainBean.java       普通 Bean，2/2 的样本
        └── StackDumpBean.java   在 @PostConstruct 里打印栈
    ├── day3/
        ├── ScopeMatrix.java     A singleton vs prototype 三条判据 / B thread 作用域 / C 判据矩阵
        ├── WebScopeTrap.java    A request 报错原文 / B session / C proxyMode / D 自定义 Scope
        └── ScopeProbe.java      探针 Bean：构造数 / @PreDestroy / destroy 三个计数
    └── day4/
        ├── InjectionChoice.java A 三候选爆炸 + 栈 / B 字段名回退 / C @Qualifier / D @Primary 压过名字
        │                        / E @Resource 按名 / F 两种注解名字对不上的报错对照
        ├── CollectionInject.java A List 按 @Order / B Map 按注册顺序 / C 零候选三段 / D 集合 + @Qualifier
        ├── CtorInject.java      A 单构造器免注解 / B 多构造器不标（炸 or 静默走无参）/ C 标一个 /
        │                        D ObjectProvider 三种局面 / E 参数名回退要 -parameters
        └── pay/
            ├── PayChannel.java      接口（单抽象方法，方便写 lambda）
            ├── AlipayChannel.java   @Order(2)，bean 名 alipayChannel
            ├── WechatChannel.java   @Order(1)，bean 名 wechatChannel
            └── UnionPayChannel.java @Order(3)，bean 名 unionPayChannel
    └── day5/
        └── BoardCheck.java      封板自检：S1 对象图 / S2 生命周期 / S3 作用域 / S4 判定链 / S5 封板表
                                 （一个文件自包含，不依赖 day1~day4 的探针；FAIL 时 exit=1）
```

> `support/Quiet.java` 是 Day 4 加的：Day 4 大量「故意让容器启动失败」的实验，
> Spring 每条都会打一遍完整 JUL 警告，和自己打印的异常链完全重复。它只关掉
> `AnnotationConfigApplicationContext` 这一个 logger（**名字是枚举 `LogManager` 查出来的**，不是猜的 ——
> 按父类名设 `Level.OFF` 压不掉，因为字段是 `LogFactory.getLog(getClass())`）。
>
> `day5` 的 `BoardCheck` 刻意**不**复用 day1~day4 的探针类：封板自检要能独立回答「结论还站不站得住」，
> 复用探针会把「探针自己写错了」这个变量带进来。代价是重复了几个小类，换来一个能当回归用例跑的文件。

## 三、五段实验各自证明了什么

### 1. `ManualWiring` —— 没有 Spring 时，"装配"这笔账记在谁头上

| 段 | 现象 | 数字 |
| :--- | :--- | :--- |
| A 三个入口各自 `new` | 功能全对，但造出**三份仓储** | 仓储实例 = **3**，哈希三个都不同 |
| B 装配收进 `main` | 实例共享了 | 仓储实例 = **1**，哈希三个相同 |
| C 数源码 | 换实现 = A 段 3 处 / B 段 1 处；加依赖 = A 段 3 处 / B 段 3 处 | 见 `day1-result.txt` |

关键不是"3 处代码改动很麻烦"，是 **3 个连接池**：每处 `new` 都带走一份缓存、一份连接、一份计数。
A 段那三个不同哈希就是证据。

### 2. `SpringWiring` —— 同一套类，装配代码 0 行

| 段 | 结论 | 证据 |
| :--- | :--- | :--- |
| A 谁 / 何时 new | `refresh()` 期间就 new 完了 | 构造行 `t=+312~338 ms`，全在「容器创建完成 `+354 ms`」之前 |
| A 调用者 | 不是我们，是 `BeanUtils.instantiateClass` | 栈上同时命中 `createBeanInstance` / `preInstantiateSingletons` / `getSingleton` |
| B 单例 | 两次 `getBean` 同一对象 | `s1 == s2 : true`，哈希相同 |
| C 装配行数 | 入口代码里 `new X(` = **0** | 数源码；`MemoryConfig` 里唯一那 1 个 `new` 是"选哪个实现" |
| D 换实现 | 三个入口一行没改 | 仓储从 `jdbc` 变 `memory`，`OrderService` 构造器打 `repo=memory` |
| E `@Lazy` | 推迟到第一次 `getBean` | `containsSingleton("lazyBean")` false → true |
| F `prototype` | 每次给新的，且不进单例池 | `p1 == p2 : false`，累计实例 2，`containsSingleton("prototypeTask") = false` |

C 段那张表是全周最该记住的东西：

```text
SpringWiring.java（三个入口）   0 / 0 / 0      ← 入口一个 new 都没有
AppConfig.java（扫包）          0 / 0 / 0
MemoryConfig.java              0 / 0 / 0      ← 唯一那行 new 是 new MemoryOrderRepository()
```

### 3. `ContainerQuestions` —— 四个不试一次就说不准的问题

| 问题 | 实测 |
| :--- | :--- |
| Q1 不写 `@Component` | 直接要：`NoSuchBeanDefinitionException: No qualifying bean of type '...$MissingDep' available`；被别人依赖：`UnsatisfiedDependencyException` → 内层同一个 `NoSuchBeanDefinitionException`（带 `expected at least 1 bean which qualifies as autowire candidate`） |
| Q2 注解对 `new` 出来的对象 | `new NotManaged()` 后 `@Autowired` 字段 = **null**；`ctx.register(NotManaged.class)` 后 = **jdbc** |
| Q3a 循环依赖（字段注入） | **能过**。构造时两边都是 null，之后 `b.a == a` |
| Q3b 循环依赖（构造器注入） | **过不去**。`UnsatisfiedDependencyException(ctorA)` → `(ctorB)` → `BeanCurrentlyInCreationException: Requested bean is currently in creation: Is there an unresolvable circular reference?` |
| Q4 单例池 | 定义 13 / 单例 19；「有定义没实例」= `lazyBean`、`prototypeTask`；「有实例没定义」= 8 个容器自己的东西（`environment` / `systemProperties` / `messageSource` …）。取过 prototype + lazy 后：定义仍 13、单例 20（只多了 lazyBean） |

Q4 那个 **19 → 20** 是原型作用域最干净的证据：原型造完连账都不记。

### 4. `LifecycleOrder` —— 六个回调的顺序，打出来而不是背出来

| 段 | 结论 | 证据（实测） |
| :--- | :--- | :--- |
| A 六回调 | ①构造 → ②BPP.before → ③`@PostConstruct` → ④`afterPropertiesSet` → ⑤`initMethod` → ⑥BPP.after | `+375.7 / 386.7 / 389.0 / 389.5 / 390.2 / 390.5 ms`，`refresh()` 在 `+408.7 ms` 才返回 |
| A 属性注入 | 没有回调，只能用两个点夹住 | 构造器里 `repo=null`，`@PostConstruct` 里 `repo=jdbc` |
| B 销毁 | 逆序 + **依赖方先死** | `Beta 构造 → Alpha 构造`；`close()` 时 `Alpha @PreDestroy → Beta @PreDestroy` |
| C prototype | 销毁回调被调 **0 次** | 取两个实例（`p1 == p2 : false`），`close()` 后计数仍是 0 |
| D 推断 | `@Bean` 默认推断 `close()` | `AutoCloseBean.close()` 1 次 vs `ManualOffBean.close()` 0 次（`destroyMethod = ""`） |

### 5. `PostProcessorTrace` —— 后处理器对谁生效、对谁不生效

| 段 | 结论 | 证据（实测） |
| :--- | :--- | :--- |
| A 计数表 | 正常 Bean **2/2**，后处理器自己与提前创建的 Bean **0/0** | `plainBean` / `lifecycleRepo` 2/2；`bppFirst` / `bppSecond` / `earlyDependent` 0/0 |
| A 同一批 | 同批后处理器**互相看不见** | `PostProcessorRegistrationDelegate:257~265`：先全部 `getBean` 收集，再统一注册 |
| B 栈 | `@PostConstruct` 是后处理器调的 | `InitDestroyAnnotationBeanPostProcessor:157` ← `applyBeanPostProcessorsBeforeInitialization:440` ← `initializeBean:1796` |
| C 提前创建 | 拿不到代理，启动日志有官方告警 | `is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)` |
| D 排序 | `Ordered` 决定调用顺序 | `plainBean` 上的序列 `[first.before, second.before, first.after, second.after]` |

反过来也验证了：后处理器**自己**的 `@PostConstruct` 照样执行（两行都打出来了）——
因为 `CommonAnnotationBeanPostProcessor` 是 `PriorityOrdered`，比我们早注册。

### 6. `ScopeMatrix` / `WebScopeTrap` —— 作用域是两个问题：造几个 + 谁管埋

| 段 | 结论 | 证据（实测） |
| :--- | :--- | :--- |
| A | singleton 造 1 个进池；prototype 每次一个不进池 | `refresh()` 后构造数 `1 / 0`；两次 `getBean` 的 `==` 是 `true / false`；`containsSingleton` 是 `true / false` |
| A 销毁 | 只有 singleton 被销毁 | `close()` 后 `@PreDestroy` 与 `destroy` 各 `1/1`（singleton）vs `0/0`（prototype） |
| B | `singleton` / `prototype` 不在作用域表里 | `registerScope("singleton", …)` → `IllegalArgumentException: Cannot replace existing scopes 'singleton' and 'prototype'`；`getRegisteredScopeNames()` 只列出自己登记的 `[thread]` |
| B | 作用域 = 一份按 key 找实例的存储 | `SimpleThreadScope` 就是 `NamedThreadLocal<Map>`：main 两次同实例，`worker-A` 另造一个（共 2 个）；它还直接 WARN「不支持销毁回调」 |
| C1/C2 | `proxyMode` 前：单例永远抱着第一个原型 | 三次 `id()` 全是 `1`，构造只有 1 个；加 `TARGET_CLASS` 后 `2 / 3 / 4`，构造 3 个 |
| C2b | 代理 = 注册期把一个定义拆成两个 | `scopedTarget.requestProxy`（`scope=request`）+ `requestProxy`（`class=ScopedProxyFactoryBean`，单例） |
| C3 | `proxyMode` 只是把解析推迟，不修错 | `getBean` 不抛；第一次 `id()` 抛同一个 ISE，栈上多一帧 `SimpleBeanTargetSource.getTarget:35` |
| D | 自定义作用域是插件 | `registerScope("every-two", scope)` + `@Scope("every-two")`：4 次 `getBean` 得到 `A A B B`，轮换时销毁回调 1 次，`close()` 后仍是 1 次 |

三条能直接拿去面试的：

1. **`request` / `session` 在非 Web 容器的报错是「查表查不到」**：
   `IllegalStateException: No Scope registered for scope name 'request'`（`AbstractBeanFactory:369`），
   而且 **`refresh()` 不报错、`getBean` 才报** —— 因为它不是单例，预实例化循环根本不碰它。
2. **`proxyMode` 的价值是「推迟解析」，不是「每次新对象」**：代理每个方法先
   `SimpleBeanTargetSource.getTarget()` → `beanFactory.getBean(scopedTarget.xxx)`；
   代价是把启动期的错误挪到了运行期（C3 段）。
3. **自定义作用域的销毁回调交给 scope 自己**（`AbstractBeanFactory:1952~1960` 的 else 分支），
   而 `ctx.close()` 只销毁单例 —— 所以作用域实现者必须自己收尾。

### 7. `InjectionChoice` / `CollectionInject` / `CtorInject` —— 「给哪个」是一条四级链

| 段 | 结论 | 证据（实测） |
| :--- | :--- | :--- |
| A | 三候选 + 字段名对不上 → 抛 | `NoUniqueBeanDefinitionException: expected single matching bean but found 3: alipayChannel,wechatChannel,unionPayChannel` |
| A 栈 | 它死在 `refresh()`，抛点在后处理器 | `resolveNotUnique:220 ← doResolveDependency:1369 ← AutowiredFieldElement.resolveFieldValue:710 ← postProcessProperties:408 ← populateBean:1431 ← doCreateBean:619` |
| B | `@Autowired` **不纯按类型**：多候选时按字段名回退 | 字段名 `wechatChannel`、零注解 → 注入到 `wechat` |
| C | `@Qualifier` 在「筛候选」那一步就生效 | 点名 `alipayChannel` → `alipay`；点名不存在 → `NoSuchBeanDefinitionException: expected at least 1 bean which qualifies as autowire candidate` |
| D | `@Primary` 压过「字段名回退」 | 字段名仍是 `wechatChannel`，注入到 `preferred` |
| E | `@Resource` 按名字（默认名 = 字段名） | 字段名 `unionPayChannel` → `unionpay`；`name="alipayChannel"` + 字段名 `anything` → `alipay` |
| F1 | 显式名对不上 → 不查类型 | `No bean named 'channel' available`（容器里明明有 3 个 `PayChannel`） |
| F2 | 默认名对不上 → 退回按类型 | 同一个 `NoUniqueBeanDefinitionException`（和 `@Autowired` 一模一样） |
| 集合 A | `List` / 数组按 `@Order` | `wechat → alipay → unionpay`（注册顺序是 alipay 打头） |
| 集合 B | `Map` **不排序**，key = bean 名 | `[alipayChannel, wechatChannel, unionPayChannel]` = 注册顺序；源码 1493-1500 无一行排序 |
| 集合 C | 集合没有特权 | `required=true` 零候选 → `expected at least 1 bean`；`required=false` → `null`（不是空集合） |
| 构造器 A/B | 单构造器免注解；多构造器不标看运气 | 无无参构造器 → `BeanInstantiationException: No default constructor found`；有则**静默**走无参 |
| 构造器 C | 标了 `@Autowired` 的那个赢 | 走 `ctor(Notifier, String)` |
| `ObjectProvider` | 把解析推迟到调用时 | 原型两次 `getObject()` = `temp-1 / temp-2`；零候选 `getIfAvailable()` = null；三候选 `getIfUnique()` = null 但 `getIfAvailable()` 抛 `NoUnique` |
| 参数名回退 | 要 `-parameters`（或 `-g`） | 反射读到 `arg0`、`isNamePresent=false` → `NoUnique`；字段名回退不需要 |

四条能直接拿去面试的：

1. **判定链的顺序是 Primary → Priority → 名字**（`DefaultListableBeanFactory:1635/1639/1642`）——
   所以 `@Primary` 能让「字段名对得上」失效，而反过来不成立。
2. **`@Autowired` 的第三级回退是「字段名当 bean 名」**，这是它「其实也按名字」的那一半。
3. **两种注解真正的差别在报错**：显式名对不上是 `No bean named`（不查类型），
   默认名对不上则退回按类型、和 `@Autowired` 同一个 `NoUnique`。
4. **集合注入不是兜底**：零候选 + `required=true` 照样抛；`required=false` 给 `null`。
   「收成 List 就天然为空」是讹传，实测推翻。

### 8. `BoardCheck` —— 封板自检：把四天口述过的结论变成 23 条断言

前七天每个论点都是「跑一段、用眼睛看输出」。今天反过来：**先写期望值，再让程序说对不上**。
理由很直接 —— 眼睛会漏，断言不会；而且下次换 JDK 或 Spring 小版本，`./run.sh day5.BoardCheck`
一条命令就知道哪些结论还站得住。

| 段 | 断言的东西 | 条数 |
| :--- | :--- | ---: |
| S1 对象图 | 手写 3 份 / 容器 1 份 / 三个入口同一引用 | 3 |
| S2 生命周期 | 初始化七步顺序（含注入点）、销毁三件套顺序、prototype 造 2 个销 0 个 | 4 |
| S3 作用域 | refresh 后 `1 / 0`、`==` 的 `true / false`、进不进池、销毁 `1+1 / 0` | 7 |
| S4 边界 | NoUnique 原文含三个候选名、字段名回退、`@Primary` 压名字、`@Qualifier`、两种报错、两种外层包装 | 9 |

**今天新增的两个观测点**（前两天只能间接说的事，今天钉死了）：

1. **属性注入的位置不再是「夹住」而是「钉死」。** 给 `LifeProbe` 加一个 `@Autowired` 的 **setter**，
   在 setter 里打点（setter 是我写的方法，不是容器回调）。实测顺序：

   ```text
   ctor → inject(setter) → bpp.before → @PostConstruct → afterPropertiesSet → initMethod → bpp.after
   ```

   `populateBean` 就落在 `ctor` 与 `bpp.before` 之间 —— Day 2 那句「初始化的是**已经装配好的**对象」
   到这里才有直接证据，不再只是「构造时 null / 初始化时有值」两头夹。

2. **「两种注解报同一个错」只对了内层。** Day 4 我写的是「默认名对不上 → 同一个 `NoUnique`」，
   内层确实一样，但**外层包装不同**：

   | 写法 | 外层 | 内层 |
   | :--- | :--- | :--- |
   | `@Autowired` 多候选 | `UnsatisfiedDependencyException` | `NoUniqueBeanDefinitionException` |
   | `@Resource(name="channel")` 不存在 | `BeanCreationException` | `NoSuchBeanDefinitionException` |
   | `@Resource` 默认名不存在 | `BeanCreationException` | `NoUniqueBeanDefinitionException` |

   原因是异常继承：`NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException extends BeansException`。
   于是两处 `catch` 都接得住它，各自换个壳子扔出去：

   ```text
   AutowiredAnnotationBeanPostProcessor.resolveFieldValue:712-713
       catch (BeansException ex) → new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(field), ex)
   CommonAnnotationBeanPostProcessor.postProcessProperties:331-332
       catch (Throwable ex)     → new BeanCreationException(beanName, "Injection of resource dependencies failed", ex)
   ```

   面试里这一层能加分：**内层讲「为什么」，外层讲「谁在灌字段的时候炸的」**。

## 四、十五个坑

1. **`Src.count` 把自己数进去了。** 第一版把针脚 `"new JdbcOrderRepository("` 写在统计代码里当字符串常量，
   于是 `SpringWiring`（明明 0 个 new）数出 3、`AppConfig` 也数出 3。改成 `countNew(src, "JdbcOrderRepository")`
   在 `Src` 内部拼针脚才干净 —— **自指统计必须把针脚藏到被数文件之外**。
2. **`GenericApplicationContext` 只能 `refresh()` 一次。** 先 `new AnnotationConfigApplicationContext(AppConfig.class)`
   再 `register(...)` + `refresh()` → `IllegalStateException: GenericApplicationContext does not support multiple refresh attempts`。
   正确姿势：`new AnnotationConfigApplicationContext()` → `register(配置类, 额外类)` → `refresh()`。
3. **`getBeanDefinitionNames()` + `getBean(name)` 会把 `@Lazy` 和 prototype 提前造出来。**
   第一版 SpringWiring 的 A 段就是这么把 LazyBean 在枚举名单时造掉的，直接导致 E 段"refresh 时没造"这个结论不成立。
   改成读 `getBeanDefinition(name).getBeanClassName()` —— 读注册表不触发构造。
4. **栈上的 `getSingleton:234` 不是查三级缓存那一支。** 它是 `getSingleton(beanName, ObjectFactory)` 里的
   `singletonFactory.getObject()`，即"真正开始造对象"的那句。查三级缓存的是另一个重载（180 行起）。
   同名两个重载，行号差 50 多行，看栈时别对错。
5. **`bppSecond` 的计数是 0/0，不是 1/1。** 我第一版文案写的是「至少能被 `bppFirst` 处理一次」，
   实测打脸。原因：同一批后处理器是「先全部 `getBean` 收集到局部 List，再统一注册」，
   收集的时候谁都没进列表。**「顺序」在 BPP 上要分「阶段」和「批内」两层看。**
6. **`@PostConstruct` 编译不过**（`程序包 javax.annotation 不存在`）。JDK 11+ 已把它移出 JDK，
   `lib/` 必须补 `javax.annotation-api`。注意 Spring 5 用 `javax.*`，Spring 6 才换 `jakarta.*`；
   下错包不会报错，只会「注解不执行」——静默失效最难查。
7. **栈帧行号指向调用点，不是方法声明行。** `postProcessBeforeInitialization` 声明在 154，
   栈上是 157（`metadata.invokeInitMethods` 那句）。**引行号前先跑一次**，别照抄博客。
8. **`@Scope` 注解和 `Scope` 接口同名。** 同一个文件里既 `import …annotation.Scope`（注解）
   又要 `implements Scope`（接口）→ 编译报「对 Scope 的引用不明确」。接口写全限定名是唯一干净解法。
9. **`SimpleThreadScope` 没有 `SCOPE_NAME` 常量。** 我按 `ConfigurableBeanFactory` 的习惯写了
   `SimpleThreadScope.SCOPE_NAME`，编译不过 —— 它的作用域名在配置里写死字符串 `"thread"`。
   同类的坑：`registerDisposableBeanIfNecessary` 声明在 1942，但 `!isPrototype()` 那句在 1944，
   引行号必须说清「声明 / 语句」。
10. **「零候选的集合注入给空集合」是讹传。** 我第一版文案就这么写的，实跑三段全对不上：
   `required=true` 的 `List` 照样抛 `expected at least 1 bean`，`required=false` 给的是 `null`。
   源码：Collection / Map 分支 `if (matchingBeans.isEmpty()) return null;` → 掉回单值分支 → 抛。
11. **注入失败不是死在 `getBean`，是死在 `refresh`。** 第一版把 `try/catch` 包在 `ctx.getBean()` 上，
   程序直接 `Exception in thread "main"` 退出 —— 因为那个 bean 不是 lazy、也没人依赖它，
   `preInstantiateSingletons` 自己就把它造了。改法：`failOf(...)` 只包 `refresh()`。
12. **JUL logger 名是子类，不是父类。** 想压掉 `refresh` 失败的警告，按 `AbstractApplicationContext`
   设 `Level.OFF` 无效 —— 字段是 `LogFactory.getLog(getClass())`，真名是
   `org.springframework.context.annotation.AnnotationConfigApplicationContext`（枚举 `LogManager` 才查到）。
13. **只打印 root cause 会丢掉最有用的那层。** `No default constructor found` 在中间的
   `BeanInstantiationException` 上，只打根因只剩一句 `NoSuchMethodException: …<init>()`。
   异常链的信息分布是「中间层最说明问题」。
14. **静态计数器跨段污染。** `BoardCheck` 的 S2 第一次跑时，`@PostConstruct` 里打出的
   `repo = jdbc#2` —— 因为 S1 的容器已经造过 `jdbc#1`，而 `BUILT` 是静态的、没 reset。
   数字本身没错（确实是新容器里第 2 个），但会让读的人以为「同一个容器造了两个」。
   修法：每段开工前 `JdbcRepo.BUILT.set(0)`。**和「并发 demo 各段互相污染」是同一类病**：
   静态状态不归零，后一段就在替前一段付账。
15. **复制粘贴函数签名没改。** `failOf(...)` 从 Day 4 抄过来，方法体返回 `Throwable`，
   签名还是 `AnnotationConfigApplicationContext` → 4 个「不兼容的类型」。
   同一个文件里既能造容器又要返回异常，签名一定要重看过再用。

## 五、待办（W05 已封板）

- [x] Bean 生命周期（Day 2 完成：六回调 + 后处理器 + 三个追问）
- [x] Bean 作用域（Day 3 完成：四条判据 + `request` 报错原文 + `proxyMode` + 自定义 Scope）
- [x] `@Autowired` vs `@Resource` vs 构造器注入的取舍（Day 4 完成：判定链四级 + 两种注解报错对照 + 集合/零候选）
- [x] W05 收尾：`Spring 到底在帮你管理什么`（Day 5 完成：三段答案 + 23 条断言的自检程序）
- [x] 补 `pom.xml`（Day 5 写出，**未验证**：本机没 Maven）
- [ ] **装 Maven 后**：`mvn -q clean compile` + `mvn -q exec:java` 与 `run.sh` 对撞一次，
      两路都必须是 `23 / 23 条通过`。在此之前 pom 只能当「声明」不能当「证据」。
