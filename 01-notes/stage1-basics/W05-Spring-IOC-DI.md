# W05 · Spring IOC / DI

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟨 进行中（Day 3 11-23）　｜　开始日期：2026-11-20　｜　完成日期：____-__-__

> 💡 目标理解：Spring 到底在帮你管理什么？

---

## 一、本周知识点

- [x] Bean · IOC · DI
- [x] Bean 生命周期
- [x] Bean 作用域
- [ ] Spring 到底在帮你管理什么

> Day 1 勾第一格：`Bean · IOC · DI`。
> 判断标准不是「背出 IOC 是控制反转、DI 是依赖注入」，而是三条：
> **① 同一套类，手写装配要改 3 处、Spring 版改 0 处（能当场数出来）；**
> **② 能说清对象是「谁 new 的、在 refresh 的哪一步 new 的」（能指到栈上的行号）；**
> **③ 能解释「字段注入的循环依赖为什么能过、构造器注入为什么过不去」（能指到源码顺序）。**
>
> Day 2 勾第二格：`Bean 生命周期`。判断标准同样不是背「构造 → 属性注入 → 初始化 → 销毁」，而是：
> **① 六个回调的先后能靠打点打出来，不是背出来；**
> **② 能说清 `@PostConstruct` 的调用者是哪个后处理器（栈上有行号）；**
> **③ 能解释 prototype 的销毁回调为什么「一次都不会被调」（源码在登记阶段就短路了）。**
>
> 第四格「Spring 到底在帮你管理什么」要等 Day 6 收尾时才勾 —— 现在只有半张答案。
>
> Day 3 勾第三格：`Bean 作用域`。判断标准三条，全部有实测输出：
> **① 能说出「造几个」和「谁管埋」是同一个 `scope` 属性的两个后果（四条判据能当场列出来）；**
> **② 能背出 `No Scope registered for scope name 'request'` 这句，并说清它为什么在 `getBean` 才抛、`refresh` 不抛；**
> **③ 能解释 `proxyMode` 是「把解析推迟到调用时」而不是「每次新对象」—— 并且知道代价是错误从启动期挪到运行期。**

## 二、核心问题（能用自己的话脱稿讲出来，才算学会）

1.
2.
3.

## 三、代码 / 实验

- 代码目录：`02-code/stage1-basics/w05-spring-ioc-di/`
- 本周动手内容：

```text
Day 1（已完成 11-20）  工具链：没有 Maven，手工 lib/ 放 6 个 Spring 5.3.39 jar
                       + ManualWiring：三个入口各自 new（3 份仓储）/ 集中装配（1 份）/ 数源码改动点
                       + SpringWiring：谁 new 的（栈指纹）、什么时候 new 的（时间线）、
                         单例池、换实现（jdbc → memory 零改调用点）、@Lazy、prototype
                       + ContainerQuestions：不写注解的两种失败 / 注解对 new 无效 /
                         循环依赖（字段 vs 构造器）/ 单例池里到底有什么
Day 2（已完成 11-23）  工具链增量：lib/ 多一个 javax.annotation-api-1.3.2.jar（JDK 11+ 已移出 JDK）
                       + LifecycleOrder：六种回调打时间戳（①构造 → ②BPP.before → ③@PostConstruct
                         → ④afterPropertiesSet → ⑤initMethod → ⑥BPP.after →
                         ⑦@PreDestroy → ⑧destroy → ⑨destroyMethod）/ 多 Bean 逆序销毁 /
                         prototype 销毁 0 次 / @Bean 的 destroyMethod 自动推断 close()
                       + PostProcessorTrace：两个 BPP 的计数表（谁 2/2、谁 0/0）、
                         @PostConstruct 的执行栈、同批 BPP 互相看不见、Ordered 生效顺序
                       + support/Stacks：把「这个回调是谁调的」从栈上读出来
Day 3（已完成 11-23）  工具链：无新增 jar（spring-aop 早就在；CGLIB 在 spring-core 里）
                       + ScopeMatrix：singleton / prototype 三条判据（两次 getBean / 单例池 / 销毁回调）/
                         registerScope 拒绝覆盖 singleton / thread 作用域（SimpleThreadScope）/
                         四作用域判据矩阵
                       + WebScopeTrap：request、session 在非 Web 容器的报错原文与栈 /
                         proxyMode 三组对照（原型注入单例 1-1-1 vs 2-3-4、定义拆成两个、报错时机后移）/
                         自定义 Scope（每 2 次 getBean 换实例 + 销毁回调交给 scope）
```

### Day 1 · 2026-11-20（周五）· 「Spring 帮你管理什么」——用 diff 回答，不用定义回答

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 W04 Day 2：默认池是哪个 / `thenApply` 与 `thenCompose` 的返回类型 / `whenComplete` 为什么吞不掉 | 口头过 |
| 15 分钟 | **先写「没有 Spring」的版本**：`OrderService` 里 `new OrderRepository(new DataSource(...))` 三层，再改成构造器传参 —— 数一下「换一个实现要动几处」 | `day1/ManualWiring.java` |
| 20 分钟 | 同一套类交给 `AnnotationConfigApplicationContext`：`@Configuration` + `@Bean` + `@ComponentScan`，打印「Bean 是谁 new 的、什么时候 new 的、是不是同一个」 | `day1/SpringWiring.java` |
| 10 分钟 | 四个问号：不写 `@Component` 会怎样 / 注解对 `new` 出来的对象生效吗 / 循环依赖报不报错 / 单例池里有什么 | `day1/ContainerQuestions.java` |
| 5 分钟 | 输出 `day1-result.txt` + 源码取证 + 本节笔记 + 勾掉「Bean · IOC · DI」 | 笔记 + 快照 |

实测快照：`w05-spring-ioc-di/day1-result.txt`（JDK 17.0.12 / Spring 5.3.39 / 12 核 / Windows 11，355 行）
源码取证：`w05-spring-ioc-di/day1-spring-source.txt`（6 段源码，行号能和栈上的行号对撞）

#### 1. 先算「没有 Spring」这笔账 —— 不是少写代码，是少一个连接池

`ManualWiring` A 段写了三个入口（Web / 定时任务 / 冒烟测试），最直觉的写法就是各自把对象拼起来：

```java
static OrderService webEntry() {
    return new OrderService(new JdbcOrderRepository(), new PriceCalculator());
}
```

三个入口都这么写，功能完全正确 —— 但**每个入口都造了一份仓储**。实测：

| 段 | 造出的仓储实例数 | 三个入口拿到的哈希 |
| :--- | ---: | :--- |
| A 三个入口各自 `new` | **3** | 三个都不一样 |
| B 把装配收进 `main`、入口只收参数 | **1** | 三个相同 |

真正的代价不是「改代码麻烦」，是 **3 个连接池**：每份 `new JdbcOrderRepository()` 都带走一份连接、
一份缓存、一份计数器。A 段那三个不同哈希就是证据，`JdbcOrderRepository.instances()` 直接数到 3。

B 段把装配收到 `main` 里，实例共享了 —— 但责任也全落到人身上：谁先 `new`、只 `new` 一次、
并发下安全吗，全靠记着。再加第四个入口就得回来改。

#### 2. 把「要动几处」数出来，而不是感觉出来

`Src.countNew()` 直接读自己的源码数 `new Xxx(` 出现几次（而不是嘴上说「大概三处」）：

```text
待改的东西                        new JdbcOrderRepository  new PriceCalculator   new OrderService
A 段（每入口各来一份）                   3                        3                     3
B 段（装配集中到 main）                1                        1                     3
```

| 要改什么 | 手写版 | Spring 版 |
| :--- | :--- | :--- |
| 换实现（jdbc → memory） | A 段 **3 处**（B 段 1 处） | 入口代码 **0 处**，配置里 1 个 `@Bean` |
| 加依赖（`OrderService` 多一个参数） | A 段 **3 处**，B 段 **3 处** | `OrderService` 构造器加一个参数，调用点 **0 处** |

Spring 版那张表是这么打出来的（`SpringWiring` C 段）：

```text
SpringWiring.java（三个入口）   0 / 0 / 0      ← 入口一个 new 都没有
AppConfig.java（扫包）          0 / 0 / 0
MemoryConfig.java              0 / 0 / 0
MemoryConfig 里唯一的那行 new = 1 次（决定「配哪个实现」，不是「每个入口各配一次」）
```

**这就是 IOC 的全部价值**：把「谁依赖谁」从**每个调用点**搬到**一个装配点**。
`OrderService` 里那句 `this.repository = repository` 还在，变的是「谁把 repository 递进来」——
从「每个入口自己 `new` 一个」变成「容器按构造器签名找现成的」。

#### 3. 「谁 new 的」—— 栈上有指纹，手写版和 Spring 版完全不是一件事

同一个 `Trace.constructed()` 打点，两种跑法打出来的「直接调用者」：

| 跑法 | 直接调用者 | 还命中了什么 |
| :--- | :--- | :--- |
| 手写 | `ManualWiring.webEntry:28` | —— |
| Spring | `org.springframework.beans.BeanUtils.instantiateClass:213` | `createBeanInstance:1232`、`preInstantiateSingletons:955`、`getSingleton:234` |

**「谁 new 的」这个问题，答案不在代码里，在栈里。** 手写版的栈顶永远是你的业务方法；
Spring 版的栈顶是一串 `BeanUtils` → `createBeanInstance` → `preInstantiateSingletons`。

顺带一个重要细节：`preInstantiateSingletons` 的 955 行、`createBeanInstance` 的 1222/1232 行、
`getSingleton` 的 234 行 —— 这些行号都能在 `day1-spring-source.txt` 里逐句对上。
**能对上，才算读过源码；对不上，说明你背的是别人的总结。**

#### 4. 「什么时候 new 的」—— refresh 期间，不是第一次 getBean 时

时间线（本次快照）：

```text
[   0 ms] SpringWiring 启动（此时一个 Bean 都还没有）
[ 312 ms] 第一个 Bean 开始构造（JdbcOrderRepository）
[ 338 ms] 最后一个入口 Bean 构造完（ReportJob）
[ 354 ms] 容器创建完成：refresh() 返回，单例已经在池子里了
```

**所有构造行的时间戳都在「容器创建完成」之前。** 那 312 ms 里没有一个是 `new` 花的 ——
是类加载、ASM 扫包、解析注解的成本。对象本身几十微秒就造完了。
（时间戳每次跑会有 ±30 ms 抖动，这只是某一台 12 核机器上的一次快照。）

源码里就一行解释（`AnnotationConfigApplicationContext.java:90`）：

```java
public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
    this();
    register(componentClasses);
    refresh();                       // ← 构造器里直接 refresh
}
```

`refresh()` 里的 `preInstantiateSingletons()`（`DefaultListableBeanFactory.java:922`）
遍历所有「非抽象 + singleton + 非 lazy」的定义，一个个 `getBean(beanName)`（955 行）。
三个条件缺一个就不会被预实例化 —— 这正好解释了后面 `@Lazy` 和 `prototype` 的两组现象。

#### 5. 两个行号 1222 与 1232：造对象的两条路

同一份输出里，不同 Bean 的 `createBeanInstance` 行号**不一样**：

| Bean | 栈上行号 | 走的分支 |
| :--- | :--- | :--- |
| `JdbcOrderRepository` / `PriceCalculator` | `createBeanInstance:1232` | `instantiateBean` —— 无参构造 |
| `OrderService` / `OrderController` / `ReportJob` / `OrderApiTest` | `createBeanInstance:1222` | `autowireConstructor` —— 解析构造器参数 |

源码（`AbstractAutowireCapableBeanFactory.java:1218`）：

```java
// Candidate constructors for autowiring?
Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
        mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
    return autowireConstructor(beanName, mbd, ctors, args);      // 1222
}
...
// No special handling: simply use no-arg constructor.
return instantiateBean(beanName, mbd);                           // 1232
```

「只有一个带参构造器时会自动用它」不是约定俗成，是 `determineConstructorsFromBeanPostProcessors`
在单构造器情况下直接命中。**这也是为什么构造器注入不需要写 `@Autowired`。**

#### 6. 单例池：13 个定义、19 个实例、20 个

`Q4` 把三张表都打了：

| 计数 | 值 | 为什么 |
| :--- | ---: | :--- |
| `getBeanDefinitionCount()` | **13** | 9 个我们自己的 + 4 个容器内部处理器 |
| `getSingletonCount()` | **19** | 13 个里 11 个已造（`lazyBean`/`prototypeTask` 没造）+ 8 个「有实例没定义」的 |
| 取过 prototype + lazy 后再数 | 定义 **13** / 单例 **20** | 只多了 `lazyBean`，`prototypeTask` 一个都没进池 |

那 8 个「有实例没定义」的是容器自己 `registerSingleton` 塞进去的：
`environment` / `systemProperties` / `systemEnvironment` / `messageSource` /
`applicationEventMulticaster` / `lifecycleProcessor` / `applicationStartup` /
`ConfigurationClassPostProcessor.importRegistry`。

**13 → 20 只加了 1**，这条最能说明「原型不进单例池」：容器只负责造，不管你后面有几个、什么时候死。

#### 7. 循环依赖：字段注入能解，构造器注入不能 —— 差在源码的三行顺序

`Q3a` 字段注入 A ⇄ B：**过**。构造时两边都是 null（打点打出来了），之后 `b.a == a` 成立。

`Q3b` 构造器注入 A ⇄ B：**不过**，异常链三层：

```text
UnsatisfiedDependencyException: Error creating bean with name 'ctorA': Unsatisfied dependency
  → UnsatisfiedDependencyException: Error creating bean with name 'ctorB': ...
    → BeanCurrentlyInCreationException: Error creating bean with name 'ctorA':
      Requested bean is currently in creation: Is there an unresolvable circular reference?
```

差别就在 `doCreateBean` 里这三行的**先后**（`AbstractAutowireCapableBeanFactory.java`）：

```java
instanceWrapper = createBeanInstance(beanName, mbd, args);   // 582：先把对象造出来
...
addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));  // 613：把半成品提前暴露
...
populateBean(beanName, mbd, instanceWrapper);                // 619：再灌字段
```

- **字段注入**：582 造出空壳 → 613 把空壳登记进三级缓存 → 619 才灌字段。
  B 要 A 的时候，从缓存里拿到的是**空壳的引用**，回头 A 的字段照样被灌满 → 能过。
- **构造器注入**：参数必须在 582 **之前**就位，那时连空壳都还没有 → 无解。

三级缓存的三个 Map（`DefaultSingletonBeanRegistry.java:180`）：
`singletonObjects`（成品）→ `earlySingletonObjects`（早期引用）→ `singletonFactories`（工厂）。
第三级存的是 **`ObjectFactory` 而不是对象**，因为要留一手：真正的 `getEarlyBeanReference`
可能要返回 AOP 代理而不是原对象，代理得等 `BeanPostProcessor` 处理完才知道。
**所以三级缓存不是「多加一层 Map」，是把「造什么」这件事延迟到真的有人要。**

#### 8. 注解不是元数据，是注册指令

`Q1` 两种失败方式（都实测了原文）：

| 场景 | 异常 | 关键 message |
| :--- | :--- | :--- |
| 直接 `getBean(没注册的类)` | `NoSuchBeanDefinitionException` | `No qualifying bean of type '...$MissingDep' available` |
| 被别人依赖 | `UnsatisfiedDependencyException` → 内层 `NoSuchBeanDefinitionException` | 多了 `expected at least 1 bean which qualifies as autowire candidate` |

`Q2` 更狠：同一个类、同样的 `@Autowired`，差别只在**谁 new 的**：

```text
new NotManaged() 之后，@Autowired 字段 = null
交给容器注册后，@Autowired 字段 = jdbc
```

**注解本身什么都不会发生。** 是容器在扫包时把「带注解的类」翻译成 BeanDefinition、
在造完对象后由 `AutowiredAnnotationBeanPostProcessor` 反射灌字段。
`new` 出来的对象走不到这条流水线，注解在它身上只是一行注释。

#### 9. 工具链：没有 Maven 也要能跑（这次多花了 20 分钟）

`lib/` 六个 jar（Spring 5.3.39）。两个必须知道的点：

1. **Spring 5.x 用自带的 `spring-jcl`，不是 `commons-logging`。** 照老教程下 `commons-logging`
   也能跑（两条桥接不冲突），但会多一个 jar。
2. **Windows 下 classpath 分隔符是 `;`**，Git Bash 里也别写成 `:`（`java` 是 Windows 程序）。
   `run.sh` 里就是 `-cp "out;lib/*"`。

`@Configuration` 被 CGLIB 代理（输出里能看到 `AppConfig$$EnhancerBySpringCGLIB$$...`），
CGLIB 是 `spring-core` 里自带的，不需要额外 jar。

### Day 2 · 2026-11-23（周一）· Bean 生命周期：把顺序打出来，而不是背下来

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 1：换实现 3 处 → 0 处、Bean 在 `refresh` 里造、字段注入为什么能解环 | 口头过 |
| 20 分钟 | 一个 Bean 上同时挂 `@PostConstruct` / `afterPropertiesSet` / `initMethod` / `@PreDestroy` / `destroy` / `destroyMethod`，六个回调打时间戳 | `day2/LifecycleOrder.java` |
| 20 分钟 | 两个 `BeanPostProcessor` 对每个 Bean 计数 + 在 `@PostConstruct` 里打印栈 | `day2/PostProcessorTrace.java` |
| 5 分钟 | 三个追问：`@PostConstruct` 谁调的 / prototype 销毁调不调 / 后处理器自己被不被处理 | 输出段落 |
| 5 分钟 | 快照 + 源码取证 + 本节笔记 + 勾掉「Bean 生命周期」 | 笔记 + 快照 |

实测快照：`w05-spring-ioc-di/day2-result.txt`（140 行）
源码取证：`w05-spring-ioc-di/day2-spring-source.txt`（7 段，行号与栈逐一对撞）

#### 1. 九步时间线（一次容器启动 + 一次 close 的完整记录）

| # | 回调 | 实测时间 | 谁写的 |
| -: | :--- | ---: | :--- |
| ① | 构造器 | `+375.737 ms` | 我 |
| — | 属性注入（无回调，只能间接证明） | ① 与 ② 之间 | 容器 |
| ② | `BPP.postProcessBeforeInitialization` | `+386.709 ms` | 容器钩子 |
| ③ | `@PostConstruct` | `+388.960 ms` | 我（注解） |
| ④ | `afterPropertiesSet` | `+389.518 ms` | 我（接口） |
| ⑤ | `initMethod = customInit` | `+390.210 ms` | 我（配置点名） |
| ⑥ | `BPP.postProcessAfterInitialization` | `+390.524 ms` | 容器钩子 |
| — | `refresh()` 返回 | `+408.658 ms` | — |
| ⑦ | `@PreDestroy` | `+411.442 ms` | 我（注解） |
| ⑧ | `DisposableBean.destroy()` | `+411.804 ms` | 我（接口） |
| ⑨ | `destroyMethod = customDestroy` | `+412.041 ms` | 我（配置点名） |

（时间是本次快照，每次跑 ±30 ms 抖动，别抄死数字，抄**顺序**。）

**属性注入没有回调**，只能用两个观测点夹住它：构造器里 `repo = null`，
到了 `@PostConstruct` 里 `repo = jdbc`。中间那次注入是 `populateBean` 干的，
它发生在 `initializeBean` **之前** —— 所以「初始化回调」这个名字是有前提的：
**初始化的是「已经装配好的对象」**。

#### 2. ② 在 ③ 之前 —— `@PostConstruct` 不是容器直接调的

这是今天最值得带走的一条。`@PostConstruct` 的执行栈（`PostProcessorTrace` B 段，原文）：

```text
StackDumpBean.init(StackDumpBean.java:21)
  ← InitDestroyAnnotationBeanPostProcessor$LifecycleElement.invoke:389
  ← LifecycleMetadata.invokeInitMethods:333
  ← InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization:157
  ← AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization:440
  ← AbstractAutowireCapableBeanFactory.initializeBean:1796
  ← doCreateBean:620 → createBean:542
```

读法：**`@PostConstruct` 的执行者是后处理器**，具体是 `CommonAnnotationBeanPostProcessor`
（继承 `InitDestroyAnnotationBeanPostProcessor`；它在 `CommonAnnotationBeanPostProcessor.java:205`
把 `@PostConstruct` 注册成 init 注解）。容器主流程只做一件事：遍历后处理器列表。

两个行号细节，抄的时候别抄错：
- `postProcessBeforeInitialization` **声明在 154 行**，栈上出现的是 **157 行** —— 157 才是
  `metadata.invokeInitMethods(...)` 那句。
- `initializeBean` 里 1796 是 BPP.before，1800 是 `invokeInitMethods` —— 同一个方法里的两行。

#### 3. ④ 为什么一定在 ⑤ 之前 —— 源码就两句 if

`invokeInitMethods:1843` 里顺序固定：

```java
boolean isInitializingBean = (bean instanceof InitializingBean);
if (isInitializingBean && ...) {
    ((InitializingBean) bean).afterPropertiesSet();   // 1863
}
...
if (StringUtils.hasLength(initMethodName) && ...
        && !(isInitializingBean && "afterPropertiesSet".equals(initMethodName))) {
    invokeCustomInitMethod(beanName, bean, mbd);      // 1874
}
```

所以「三种初始化写法都生效时谁先」，答案是**注解（在后处理器里）→ 接口 → 配置点名的**。
注意最后那个 `!(...)` 条件：**接口方法名和 initMethod 重名时不会跑两遍** ——
这正是「写了 `initMethod = "afterPropertiesSet"` 也不会双调」的原因。

#### 4. 销毁：三件套的顺序 + 逆序 + 依赖方先死

销毁的顺序和初始化**对称**（`DisposableBeanAdapter.destroy:194`）：

```text
DestructionAwareBeanPostProcessor.postProcessBeforeDestruction  → ⑦ @PreDestroy
((DisposableBean) bean).destroy()                              → ⑧
invokeCustomDestroyMethod                                      → ⑨
```

多个 Bean 之间是**逆序**，而且有依赖关系时**依赖方先死**（`B` 段实测）：

```text
Beta 构造  →  Alpha 构造            （Beta 先造）
close():  Alpha @PreDestroy  →  Beta @PreDestroy   （Alpha 先死）
```

原因不是「栈式逆序」这一条 —— `DefaultSingletonBeanRegistry` 会先销毁
`dependentBeanMap` 里的下游。否则 Alpha 被销毁时，手里那个 Beta 可能已经不在了。

#### 5. prototype 的销毁回调：不是忘了调，是没登记

`LifecycleOrder` C 段：取两个 prototype、`close()` 之后销毁回调被调 **0 次**。

源码只有一行（`AbstractBeanFactory.registerDisposableBeanIfNecessary:1942`）：

```java
if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
    if (mbd.isSingleton()) { registerDisposableBean(...); }
    else { scope.registerDestructionCallback(...); }
}
```

**prototype 在进 `if` 之前就被短路了**，连「销毁回调」这件事都没登记，
销毁阶段自然找不到它。所以定时器、连接池、`FileChannel` 这些必须自己关 ——
「容器管造不管埋」在 prototype 上是字面意思。

#### 6. `@Bean` 的 `destroyMethod` 默认会**推断** `close()`

`@Bean` 不写 `destroyMethod` 时默认值是 `AbstractBeanDefinition.INFER_METHOD`
（字符串 `"(inferred)"`）：容器自己去找 `close()` 或 `shutdown()` 并调掉。

实测（D 段）：

```text
AutoCloseBean.close()  被调 1 次   （@Bean 没写 destroyMethod → 推断成功）
ManualOffBean.close()  被调 0 次   （@Bean(destroyMethod = "") → 推断关闭）
```

这条的杀伤力在于「静默」：你的类里刚好有个 `public void close()` 用来做别的事，
容器关闭时会替你调一次。要么改名，要么显式写 `destroyMethod = ""`。

#### 7. 后处理器：2/2 与 0/0 的差别就是「出生时间」

`PostProcessorTrace` A 段（两个后处理器，各记一次 before / after）：

| beanName | before | after | 说明 |
| :--- | ---: | ---: | :--- |
| `plainBean` | 2 | 2 | 正常 Bean，两个后处理器各处理一次 |
| `lifecycleRepo` | 2 | 2 | 同上 |
| `bppFirst` | **0** | **0** | 后处理器自己 |
| `bppSecond` | **0** | **0** | 后处理器自己 |
| `earlyDependent` | **0** | **0** | 被 `bppFirst` `@Autowired` 进去，被迫提前创建 |

为什么 `bppSecond` 也是 0/0？我第一版答案是错的（以为它至少能被 `bppFirst` 处理一次）。
源码纠正了我（`PostProcessorRegistrationDelegate:257~265`）：

```java
for (String ppName : orderedPostProcessorNames) {
    BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);  // 先全部造出来
    orderedPostProcessors.add(pp);
}
sortPostProcessors(orderedPostProcessors, beanFactory);
registerBeanPostProcessors(beanFactory, orderedPostProcessors);                   // 再统一注册
```

**同批后处理器是「先全部 `getBean`，再统一注册」** —— 收集的时候谁都没进列表，
所以互相都看不见。而 `PriorityOrdered` 那一批（含 `CommonAnnotationBeanPostProcessor`）
在 253 行就已经注册完了，所以它**看得见**我们的后处理器：证据是 `bppFirst` / `bppSecond`
身上那个 `@PostConstruct` 确实被执行了（输出里两行 `[后处理器自己的 @PostConstruct]`）。

`earlyDependent` 才是真正要警惕的：它被 `@Autowired` 进后处理器，所以比后处理器还早出生，
Spring 直接给了一条 INFO 告警（原文）：

```text
Bean 'earlyDependent' of type [...EarlyDependent] is not eligible for getting processed by
all BeanPostProcessors (for example: not eligible for auto-proxying)
```

**"not eligible for auto-proxying" = 拿不到 AOP 代理 = `@Transactional` / `@Async` 白写。**
这条告警平时被日志级别埋在启动日志里，但它是「我这个类的注解怎么不生效」的一类标准答案。

#### 8. 三个追问的答案（能一句话说完才算会）

1. **`@PostConstruct` 谁调的？** —— `CommonAnnotationBeanPostProcessor`
   （继承 `InitDestroyAnnotationBeanPostProcessor`）在 `postProcessBeforeInitialization` 里调的，
   栈上有 157 / 440 / 1796 三个行号。前提是 classpath 上有 `javax.annotation-api`。
2. **prototype 的销毁回调会不会被调？** —— 不会，一次都不会；`registerDisposableBeanIfNecessary:1942`
   用 `!mbd.isPrototype()` 在登记阶段就短路了。
3. **`BeanPostProcessor` 自己会不会被后处理？** —— 被「更早注册的」处理（`@PostConstruct` 照样跑），
   被「同批和后来的」漏掉（计数器 0/0）。所以给后处理器加 `@Transactional` 基本等于白写。

#### 9. 工具链增量：`javax.annotation-api` 这一个 jar 是必需的

JDK 11 起 `javax.annotation` 已从 JDK 移除，JDK 17 上没有它 —— `@PostConstruct` **编译都过不了**。
`lib/` 因此从 6 个 jar 变成 7 个（多的那个是 `javax.annotation-api-1.3.2.jar`，26 KB）。

两个容易踩的点：
- **Spring 5 用的是 `javax.annotation`，不是 `jakarta.annotation`**（Spring 6 才换）。
  下错包会得到「注解放上去但没生效」的静默失败。
- 这个后处理器是 `AnnotationConfigApplicationContext` **按 classpath 自动注册**的：
  `AnnotationConfigUtils` 检测到 `javax.annotation.Resource` 存在才注册。
  没有 jar 时不会报错，`@PostConstruct` 只是「不执行」—— 静默失效。

### Day 3 · 2026-11-23（周一 · 第二个时间盒）· Bean 作用域：造几个 + 谁管埋

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 2：六回调顺序 + `@PostConstruct` 谁调的 + prototype 为什么不销毁 | 口头过 |
| 20 分钟 | `singleton` / `prototype` 三条判据（两次 `getBean` 是否同一个 / 进不进单例池 / 销毁回调） | `day3/ScopeMatrix.java` A 段 |
| 20 分钟 | `request` / `session` 在非 Web 容器的报错原文 + `@Scope(proxyMode)` 为什么存在 | `day3/WebScopeTrap.java` A~C |
| 5 分钟 | 自定义 `Scope`：每 2 次 `getBean` 换一个实例 | 同文件 D 段 |
| 5 分钟 | 快照 + 源码取证 + 本节笔记 + 勾掉「Bean 作用域」 | 笔记 + 快照 |

实测快照：`w05-spring-ioc-di/day3-result.txt`（154 行）
源码取证：`w05-spring-ioc-di/day3-spring-source.txt`（7 段，行号与栈逐一对撞）

#### 1. 四条判据，一张矩阵说完

| 作用域 | 两次 `getBean` 同一个？ | 进单例池？ | `close()` 触发销毁回调？ | 非 Web 容器 |
| :--- | :--- | :--- | :--- | :--- |
| `singleton` | 是（`singletonProbe#1` 两次） | 是（`containsSingleton = true`） | 是：`@PreDestroy` 1 次 + `destroy` 1 次 | 正常 |
| `prototype` | 否（`#2` / `#3`） | 否 | **否：0 次 / 0 次** | 正常 |
| `request` | 取不到 | 否 | 不会发生 | `IllegalStateException` |
| `session` | 取不到 | 否 | 不会发生 | 同上，只换个名字 |
| `thread`（外挂） | 同线程是、跨线程否 | 否 | 否（`SimpleThreadScope` 直接 WARN 不支持） | 需先 `registerScope` |

两个数字值得记：`refresh()` 刚返回时，`singletonProbe` **已经构造 1 次**、`prototypeProbe` **0 次** ——
这就是 Day 1 那条「Bean 在 `refresh()` 里造完」的又一次复核，只不过这次是从「作用域」这一侧看。

作用域其实是**两个问题合成的一个答案**：

- **造几个**（存哪儿、按什么 key 找）→ `scopes` 里那个 `Scope` 实现决定；
- **谁管埋**（销毁回调谁负责）→ `registerDisposableBeanIfNecessary` 的三个分支决定。

#### 2. `singleton` / `prototype` 根本不在作用域表里

`getBeanFactory().getRegisteredScopeNames()` 打印出来是 `[thread]` —— 只有我手工登记的那个。
再试着登记一个同名覆盖：

```text
registerScope("singleton", new SimpleThreadScope())
  → IllegalArgumentException: Cannot replace existing scopes 'singleton' and 'prototype'
```

源码 `AbstractBeanFactory.registerScope:1041`，抛在 **1045**：

```java
if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
    throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
}
```

原因很简单：它们不是查表查出来的，是 `doGetBean` 里**写死的两个 `if` 分支**
（`mbd.isSingleton()` / `mbd.isPrototype()`），第三个 `else` 才是查表的自定义作用域。
**所以「作用域」在 Spring 里是一张可插拔的表，只有前两个名字是特权。**

#### 3. `request` / `session`：报错在 `getBean`，不在 `refresh`

```text
new AnnotationConfigApplicationContext(WebScopeConfig.class)      ← 成功，不报错
getBeanDefinition("requestProbe").getScope()                      → "request"
getRegisteredScopeNames()                                         → []
getBean("requestProbe") → java.lang.IllegalStateException
                          message = No Scope registered for scope name 'request'
  栈：AbstractBeanFactory.doGetBean:369 ← getBean:214 ← AbstractApplicationContext.getBean:1177
```

三件事要连起来讲：

1. **`refresh()` 为什么不报？** 因为 `preInstantiateSingletons()` 的条件是
   `!isAbstract() && isSingleton() && !isLazyInit()` —— 它不是单例，循环压根不碰它。
2. **报错措辞是「查表查不到」**，不是「这个作用域不能这么用」：
   `doGetBean:367` 查 `scopes`，`369` 发现是 `null` 就抛。
3. **它在哪里才是真的？** 在 `WebApplicationContext` 里，由 spring-web 的 `RequestScope` /
   `SessionScope` 注册进去。所以同一段代码在 Spring Boot Web 里好好的，挪到单测里就炸 ——
   单测里没有 Web 上下文，这才是「非 Web 环境跑 Spring 代码」最常见的第一个坑。

#### 4. `proxyMode` 的两个用途：一个是「每次新的」，一个是「以后再说」

**用途一：单例里注入原型**（`C1` vs `C2`，全部实测）

| 写法 | 三次 `probe.id()` | 原型构造数 |
| :--- | :--- | ---: |
| `@Scope("prototype")` 直接注入 | `1 / 1 / 1`（永远是注入时那一个） | 1 |
| `@Scope(value="prototype", proxyMode=TARGET_CLASS)` | `2 / 3 / 4` | 3 |

单例只在**注入那一刻**问容器要了一次对象，之后手里那个引用不会自己变。
加代理后，注入的是 `ScopeProbe$$EnhancerBySpringCGLIB$$<hash>`，**每次方法调用**先
`SimpleBeanTargetSource.getTarget()` → `beanFactory.getBean("scopedTarget.proxiedProto")`。

**代理是怎么实现的？** 注册阶段把**一个定义拆成两个**（`C2b`，读的是定义表）：

```text
scopedTarget.requestProxy   scope=request      class=-                                       factoryMethod=requestProxy
requestProxy                scope=(singleton)  class=org.springframework.aop.scope.ScopedProxyFactoryBean   factoryMethod=-
```

`ScopedProxyUtils:41` 里那个常量 `TARGET_NAME_PREFIX = "scopedTarget."` 就是命名来源；
外层那个是单例的 `FactoryBean`（产代理），真身挪到 `scopedTarget.xxx` 并保留 `scope=request`。

**用途二：Web 作用域注入单例**（`C3`）—— 这里要看清一件事：
`getBean("requestProxy")` **不抛异常了**，第一次 `id()` 才抛，而且是同一个错：

```text
requestProxy.id() → IllegalStateException: No Scope registered for scope name 'request'
  at AbstractBeanFactory.doGetBean:369
  at AbstractBeanFactory.getBean:209
  at SimpleBeanTargetSource.getTarget:35          ← 代理在这里才去容器要目标
  at CglibAopProxy$DynamicAdvisedInterceptor:691
```

所以 `proxyMode` 的价值是**把「什么时候解析目标实例」从注入时推迟到使用时**，
不是「修好了 `request` 作用域」。代价很明确：**错误从启动期挪到了运行期**。

#### 5. 自定义 `Scope`：容器只依赖其中三个方法

`Scope` 接口五个方法（`Scope.java` 行号：`get` 74 / `remove` 93 / `registerDestructionCallback` 124 /
`resolveContextualObject` 134 / `getConversationId` 152），容器真正用的是前三个。
我实现了一个「第 1、3、5… 次 `get` 换新实例」的怪作用域，`registerScope("every-two", scope)` 装上：

```text
第 1 次 getBean → everyTwoProbe#1
第 2 次 getBean → everyTwoProbe#1
第 3 次 getBean → everyTwoProbe#2
第 4 次 getBean → everyTwoProbe#2
构造 2 个 / scope.get() 被调 4 次 / 轮换时销毁回调 1 次
ctx.close() 之后销毁回调累计 1 次 —— 没有变化
```

两条容易忽略的：

1. **销毁回调不是容器调的**，是登记时**交给 scope** 的 —— 就是 Day 2 那段源码的 `else` 分支
   （`registerDisposableBeanIfNecessary` 声明在 1942，`!mbd.isPrototype()` 在 1944，
   `else` 在 1952~1960）。prototype 连这个 `else` 都进不去（1944 就短路），
   所以「自定义作用域**有**销毁回调、prototype **没有**」的差别就落在这里。
2. **`ctx.close()` 只销毁单例**（`DefaultSingletonBeanRegistry.destroySingletons`），
   自定义作用域里的最后一个实例容器不管。真实现里必须自己收尾，否则就是内存里留着一个没人认领的对象。

**再补一个 Spring 自带的例子**（`ScopeMatrix` B 段）：`SimpleThreadScope` 就是一个
`NamedThreadLocal<Map<String,Object>>` —— main 线程两次同实例、`worker-A` 另造一个（共 2 个）。
而且它的 `registerDestructionCallback` 只打一条 WARN：

```text
警告: SimpleThreadScope does not support destruction callbacks. Consider using RequestScope in a web environment.
```

连 Spring 自己写的作用域都能「只管造不管埋」—— 这反过来证明：**埋不埋是 `Scope` 实现者的事，容器只负责登记。**

#### 6. 一句话记法

> **作用域 = 「造几个」+「谁管埋」+「什么时候解析」。**
> 前两个由定义的 `scope` 决定；第三个由 `proxyMode` 决定。
> 面试被问「`@Scope` 有什么用」，答这三条比背 `singleton`/`prototype` 的定义有用得多。

## 四、踩坑与 Bug 复盘

| 现象 | 原因 | 排查过程 | 解决 | 底层原理 |
| :--- | :--- | :--- | :--- | :--- |
| `SpringWiring` 明明一个 `new` 都没有，计数却打出 3；`AppConfig` 也打出 3 | 统计代码自己把针脚 `"new JdbcOrderRepository("` 当字符串常量写在被数的文件里 | 一眼看出「3」正好等于统计代码里 `Src.count` 调用的次数 → 自指 | 改成 `Src.countNew(src, "JdbcOrderRepository")`，针脚在 `Src` 内部拼 | 自指统计（quine 类问题）必须把针脚藏到被数文件之外 |
| `IllegalStateException: GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once` | 先 `new AnnotationConfigApplicationContext(AppConfig.class)`（内部已 refresh），又 `register()` + `refresh()` | 异常信息直接点名 | `new AnnotationConfigApplicationContext()` → `register(AppConfig.class, 额外类)` → `refresh()` | `GenericApplicationContext` 的 `refreshed` 标志只能翻一次；要动态加定义得在 refresh 之前 register |
| `@Lazy` 那段的结论不成立：`containsSingleton("lazyBean")` 一开始就是 true | A 段为了打印 bean 名单写了 `ctx.getBean(name)`，把 LazyBean 和 PrototypeTask 提前造了 | 输出里 LazyBean 的构造行出现在 A 段的 `getBean(name)` 循环中间 —— 比「容器创建完成」晚，但在 E 段之前 | A 段改读 `getBeanDefinition(name).getBeanClassName()` | 读 **注册表** 不触发构造；`getBean(name)` 会触发。调试「什么时候造」时不能用 `getBean` 枚举 |
| 险些写错：以为栈上的 `getSingleton:234` 是「查三级缓存」 | `DefaultSingletonBeanRegistry` 有两个同名重载 | 去源码数行：234 行是 `singletonFactory.getObject()`，180 行那个重载才是查三级缓存 | 文案改成「234 = 真正开始造对象的入口」 | 同名重载看栈必须对行号，否则结论会反 |
| `bppSecond` 的计数是 0/0，与预期「至少被 `bppFirst` 处理一次」相反 | 同一批（Ordered）后处理器是「先全部 `getBean` 收集，再统一注册」 | 先怀疑自己写错，再去看 `PostProcessorRegistrationDelegate:257~265`，最后被表格数字说服 | 结论改成「同批互相看不见；只有更早阶段（PriorityOrdered）注册的才看得见它」 | 收集与注册是两个循环；「顺序」这件事在 BPP 上要分「阶段」和「批内」两层 |
| 文案里写 `postProcessBeforeInitialization:154`，栈上却是 157 | 154 是方法声明行，157 是 `metadata.invokeInitMethods` 那行 | 把栈打印出来逐行对：389 / 333 / 157 / 440 / 1796 | 文案改成「声明在 154，执行帧在 157」，并说明差在哪 | 栈帧行号指向**调用点**，不是方法签名行 —— 引行号前必须自己跑一次 |
| `@PostConstruct` 编译不过：`程序包 javax.annotation 不存在` | JDK 11 起 `javax.annotation` 已从 JDK 移除 | 一眼确认；该类在 `lib/` 里没有对应的 jar | `lib/` 补 `javax.annotation-api-1.3.2.jar`（Spring 5 用 javax，不是 jakarta） | 没有这个 jar 时，即使换个方式跑起来，`@PostConstruct` 也是**静默不执行**；容器自动注册 `CommonAnnotationBeanPostProcessor` 的前提就是 classpath 上有它 |
| 编译报「对 `Scope` 的引用不明确」 | `@Scope`（注解）与 `Scope`（接口）**同名**，同一个文件里 `import …annotation.Scope` 之后，`implements Scope` 就二义了 | javac 直接把两个候选都列了出来 | `implements org.springframework.beans.factory.config.Scope` 写全限定名 | Spring 里这种「注解与接口同名」不止一处（`@Order`/`Ordered`、`@Scope`/`Scope`）；类里要同时用两个时，注解 import、接口全限定是最省事的写法 |
| `SimpleThreadScope.SCOPE_NAME` 报「找不到符号」 | 这个类**没有** `SCOPE_NAME` 常量；作用域名是字符串 `"thread"` | 去源码里 grep，确认只有 `get/remove/registerDestructionCallback/…` | 写 `registerScope("thread", new SimpleThreadScope())` + `@Scope("thread")` | 又一次「断言先于实测」：我按 `ConfigurableBeanFactory.SCOPE_SINGLETON` 的习惯想当然，实际那个常量在工厂接口上，不在 scope 实现上 |
| 文案写 `AbstractBeanFactory:1942 的 !mbd.isPrototype()` | 1942 是**方法声明**，1944 才是那句 `if` | 用 `awk` 带行号打印源码，逐行对 | 改成「声明 1942、`if` 在 1944」，并把这个差别写进 `day3-spring-source.txt` 的抬头 | 和 Day 2 的 154 vs 157 是同一类错：**行号必须说清是「声明」还是「语句」**，栈帧只给调用点 |
| 第一版写「拆出来的两个定义 class 都是空的」 | 外层 `requestProxy` 的 `class` 是 `ScopedProxyFactoryBean`（显式类），只有真身 `scopedTarget.requestProxy` 是 `@Bean` 定义（只有 `factoryMethod`，没有 `class`） | 定义了 `C2b` 段把 `scope / class / factoryMethod` 三列都打出来，实跑就对上了 | 文案改成「外层有 class、真身只有 factoryMethod」 | `@Bean` 方法产出的 `BeanDefinition` 本来就没有 `beanClass`；读定义表时不能只看 class 一列 |
| 文案写「容器 `close()` 时会通知 scope」 | `close()` 只销毁单例（`destroySingletons`），自定义作用域不在其中 | D 段实测：轮换时销毁计数 1，`close()` 后仍是 1 | 改成「自定义作用域的生命周期由 scope 自己负责，容器不管收尾」 | 作用域是插件，插件里的资源也得插件自己收 —— 这和 prototype「谁取谁负责」是同一条原则 |

## 五、面试会怎么问

- 「Spring 帮你管理什么？」——别背「控制反转」，讲 **3 处 → 0 处**，讲 3 个连接池变成 1 个。
- 「Bean 什么时候创建的？」——**`refresh()` 期间**，`preInstantiateSingletons` 遍历「非抽象 + singleton + 非 lazy」。
  追问：「第一次 `getBean` 的时候？」不，那时已经在池子里了。
- 「单例池是什么数据结构？」——`Map<String, Object>`（`singletonObjects`），
  配合 `earlySingletonObjects` / `singletonFactories` 共三级。
- 「三级缓存为什么要第三级？直接存对象不行吗？」——要留一手给 AOP：暴露的可能是代理而不是原对象。
- 「循环依赖能解决吗？」——字段/setter 注入能（提前暴露半成品），构造器不能（参数先于对象）。
  追问：「Spring Boot 2.6 之后为什么默认禁止了？」——因为「能解」不等于「该写」。
- 「`@Autowired` 和构造器注入选哪个？」——构造器（不可变、能单测、启动就暴露循环依赖）；
  单构造器时根本不用写 `@Autowired`。
- 「不写 `@Component` 会怎样？」——两种异常，`NoSuchBeanDefinitionException` 是根，
  `UnsatisfiedDependencyException` 只是它被包了一层。
- 「`@Lazy` 有什么用？」——把构造推迟到第一次 `getBean`；也能用来打断循环依赖。
- 「`prototype` 的 Bean 谁负责销毁？」——没人，容器不管（所以别在原型里放需要关的资源）。
- 「`new` 出来的对象，`@Autowired` 生效吗？」——不生效，注解要靠容器的后处理器。
- 「`@PostConstruct`、`afterPropertiesSet`、`initMethod` 都写了，执行顺序是什么？」——
  **注解 → 接口 → 配置**；因为注解是后处理器在 `postProcessBeforeInitialization` 里调的，
  后两者在 `invokeInitMethods` 里，接口在前（`AbstractAutowireCapableBeanFactory:1863` vs `1874`）。
- 「`@PostConstruct` 到底是谁调的？」——`CommonAnnotationBeanPostProcessor`（继承
  `InitDestroyAnnotationBeanPostProcessor`）。追问「没有它行不行」→ 不行，JDK 11+ 还得自己加
  `javax.annotation-api`，否则静默不执行。
- 「prototype 的 Bean 销毁回调会被调吗？」——不会，一次都不会。不是忘了调，
  是 `registerDisposableBeanIfNecessary` 用 `!mbd.isPrototype()` 在登记阶段就短路了。
- 「`@Bean` 不写 `destroyMethod` 会怎样？」——默认 `INFER_METHOD`，容器自动找 `close()` / `shutdown()`
  调掉；要关掉写 `destroyMethod = ""`。
- 「两个 Bean 一个有依赖一个被依赖，谁先销毁？」——依赖方先死（`dependentBeanMap` 下游优先），
  不是单纯的栈式逆序。
- 「`BeanPostProcessor` 对哪些 Bean 不生效？」——比它早出生的：同批后处理器、被后处理器
  `@Autowired` 进去的 Bean、配置类。后果是 `is not eligible for auto-proxying`，代理套不上。
- 「为什么说 `BeanPostProcessor` 是 AOP 的入口？」——代理是在 `postProcessAfterInitialization` 里
  把原对象**换掉**的；所以初始化回调里 `this` 还是原对象，注入到别处的才是代理。
- 「Spring 有几种作用域？」——标准六个（`singleton` / `prototype` / `request` / `session` /
  `application` / `websocket`），但**只有前两个是写死在 `doGetBean` 的 `if` 里的**，
  后四个是 spring-web 注册进 `scopes` 表的插件；`registerScope` 想覆盖前两个会直接抛
  `Cannot replace existing scopes`。
- 「`request` 作用域的 Bean 在单测里为什么取不到？」——容器里没这个作用域：
  `No Scope registered for scope name 'request'`（`AbstractBeanFactory:369`）。
  追问「为什么启动不报错」→ 因为它不是单例，`preInstantiateSingletons` 不碰它，**报错推迟到第一次 `getBean`**。
- 「单例里注入一个 prototype，会变成新的吗？」——不会，注入那一刻拿到哪个就一直是哪个（实测三次 `id` 全是 1）。
  要每次新的，加 `proxyMode = TARGET_CLASS`，代理每次方法调用回容器要一次（实测 `2 / 3 / 4`）。
- 「`proxyMode` 到底做了什么？」——注册阶段把定义拆成两个：`scopedTarget.xxx`（真身，带 `scope`）
  和外层 `xxx`（单例的 `ScopedProxyFactoryBean`，产代理）。价值是**把解析推迟到调用时**，
  所以它修的是「作用域不匹配」，不是「作用域不存在」—— `request` 在非 Web 容器里加了代理照样抛，只是抛得晚。
- 「自定义作用域怎么写？」——实现 `Scope` 的五个方法（容器只用 `get` / `remove` /
  `registerDestructionCallback`），`registerScope` 注册，Bean 上 `@Scope("名字")`。
- 「自定义作用域里的 Bean 谁销毁？」——**scope 自己**。容器在登记阶段把 `DisposableBeanAdapter`
  交给 `scope.registerDestructionCallback`，`close()` 只销毁单例。`SimpleThreadScope` 干脆不支持，
  只打一条 WARN —— 这就是「容器管造不管埋」的第三个例子（前两个：prototype、`@Bean` 的 `destroyMethod` 推断）。

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 11-20 | 五 | W05 Day 1：IOC / DI —— 手写装配 vs 容器装配，谁 new、何时 new、循环依赖 | 70 min | `ManualWiring` + `SpringWiring` + `ContainerQuestions` + `day1-result.txt`（355 行）+ 源码取证 | ✅ |
| 11-23 | 一 | W05 Day 2：Bean 生命周期 —— 六回调顺序 / 后处理器 / 三个追问 | 65 min | `LifecycleOrder` + `PostProcessorTrace` + `day2-result.txt`（140 行）+ `day2-spring-source.txt`（7 段） | ✅ |
| 11-23 | 一 | W05 Day 3：Bean 作用域 —— 四条判据 / `request` 报错 / `proxyMode` / 自定义 Scope | 60 min | `ScopeMatrix` + `WebScopeTrap` + `ScopeProbe` + `day3-result.txt`（154 行）+ `day3-spring-source.txt`（7 段） | ✅ |

---

## 参考要点（先自己答，再对照）

**Q1 · Spring 到底帮你管理什么？说人话。**

**它管的是「对象图」，不是「对象」。** 拆成三件事：

1. **谁依赖谁**（DI）——从「每个调用点自己 `new`」搬到「一个装配点按签名匹配」。
2. **造几个、什么时候造**（作用域 + 生命周期）——singleton 在 `refresh` 里预造、`@Lazy` 推迟、
   prototype 每次给新的。
3. **造完做什么**（`BeanPostProcessor` / 初始化回调）——AOP、`@Autowired` 灌字段都挂在这一步。

最硬的证据是这次数的三个数：

| | 手写版 | Spring 版 |
| :--- | ---: | ---: |
| 换实现要改的调用点 | 3 | **0** |
| 加依赖要改的调用点 | 3（A 段）/ 3（B 段） | **0** |
| 三个入口造出的仓储实例 | 3（A 段） | **1** |

第三行比前两行更值钱：手写版不是「写得丑」，是**真的多开了两个连接池**。

---

**Q2 · Bean 是 `refresh()` 里造的，还是第一次 `getBean` 时造的？单例池里到底有什么？**

默认是 **`refresh()` 里**。时间线证据（`SpringWiring` A 段）：第一个 Bean 在 `+312 ms` 构造，
最后一个在 `+338 ms`，而「容器创建完成」在 `+354 ms` —— 全部在 refresh 内。
源码是 `preInstantiateSingletons()`（`DefaultListableBeanFactory.java:922`）里那句 `getBean(beanName)`（955），
条件是 `!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()`。

两个例外正好把这个条件反过来验证了：

| Bean | 为什么不预造 | 实测 |
| :--- | :--- | :--- |
| `@Lazy` 的 `LazyBean` | `isLazyInit()` = true | `containsSingleton` false → `getBean` 后 true |
| `@Scope("prototype")` 的 `PrototypeTask` | `isSingleton()` = false | 取两次造两个（`p1 == p2 : false`），且始终不在池里 |

单例池的三层结构（`DefaultSingletonBeanRegistry.java:180`）：
`singletonObjects`（成品）→ `earlySingletonObjects`（早期引用，给循环依赖用）→
`singletonFactories`（`ObjectFactory`，第三级存的是工厂不是对象，为了 AOP 代理能延迟决定）。

数字：定义 13、实例 19。差的 6 这么来的 —— 13 个定义里 2 个没造（`lazyBean` / `prototypeTask`），
即 11 个已造；另有 8 个「有实例没定义」的容器内部单例（`environment` / `systemProperties` /
`messageSource` / `applicationEventMulticaster` …）；11 + 8 = 19。
更直观的是「取过 prototype + lazy 之后」：定义还是 13、单例只从 19 变 20。
**原型不进池**这一条，一个 +1 就说完了。

---

**Q3 · 循环依赖为什么字段注入能解、构造器注入不能？**

差别只在 `doCreateBean` 里三行的**顺序**：

```java
createBeanInstance(...)   // 582：造对象
addSingletonFactory(...)  // 613：把「半成品」提前登记（只有 singleton + 允许循环时才做）
populateBean(...)         // 619：灌字段
```

- **字段注入**：582 造出空壳（字段全是 null，打点验证过）→ 613 登记 → 619 灌。
  B 需要 A 时从缓存里拿到空壳引用，回头 A 的字段照样灌满 → 环被「时间差」化解。
- **构造器注入**：参数必须在 582 之前准备好，那时连空壳都没有 → 直接
  `BeanCurrentlyInCreationException`。

判据极简：`beforeSingletonCreation` 往 `singletonsCurrentlyInCreation` 这个 Set 里 `add` 失败就抛
（`DefaultSingletonBeanRegistry.java:353`）——「你正在造它，怎么又来要它」。

**面试到这里别停**：能解 ≠ 该写。字段注入解出来的对象在构造完成时是不完整状态，
并发下可能被别人看到半成品；而且它把「依赖」藏起来了，单测必须起容器。
所以 **Spring 官方推荐构造器注入**，代价是「别写出环」——这才是正确答案的落点。

---

**Q4 · Bean 的完整生命周期，从头到尾说一遍。**

一句话骨架：**构造 → 属性注入 → BPP.before → 初始化回调（注解 → 接口 → 配置）→ BPP.after → 使用 → 销毁三件套**。

展开成能对上行号的样子（括号里是 Spring 5.3.39 的实测时间戳，同一台机器跑出来）：

| # | 阶段 | 关键类 / 行号 |
| -: | :--- | :--- |
| ① | 构造（`+375.7 ms`） | `createBeanInstance` → `BeanUtils.instantiateClass` |
| — | 属性注入 | `populateBean`（没有回调，只能用「构造时 null / `@PostConstruct` 里有值」夹出来） |
| ② | `BPP.before`（`+386.7 ms`） | `initializeBean:1796` → `applyBeanPostProcessorsBeforeInitialization:440` |
| ③ | `@PostConstruct`（`+389.0 ms`） | `InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization:157` |
| ④ | `afterPropertiesSet`（`+389.5 ms`） | `invokeInitMethods:1863` |
| ⑤ | `initMethod`（`+390.2 ms`） | `invokeCustomInitMethod:1874` |
| ⑥ | `BPP.after`（`+390.5 ms`） | `applyBeanPostProcessorsAfterInitialization:1805` |
| ⑦⑧⑨ | `@PreDestroy` → `destroy()` → `destroyMethod`（`+411.4~412.0 ms`） | `DisposableBeanAdapter.destroy:194` |

三个必须能接住追问的点：

1. **③ 在 ①之后、②之内** —— `@PostConstruct` 是后处理器调的，不是容器主流程调的。
2. **④ 一定在 ⑤ 之前** —— 接口优先于配置；而且两者重名时只跑一次（源码里那句 `!(...)`）。
3. **销毁是逆序 + 依赖方先死**，因为 `DefaultSingletonBeanRegistry` 先销毁 `dependentBeanMap` 的下游。

---

**Q5 · `BeanPostProcessor` 为什么是理解 AOP 的入口？它有什么「漏掉」的 Bean？**

两个方向都要讲：

**向上（它是怎么工作的）**：`postProcessAfterInitialization` 返回的对象就是最终放进单例池的对象 ——
AOP 在这一步把原对象**换成代理**。所以「初始化回调里的 `this`」和「注入到别人手里的引用」是两个不同的对象，
这是「同类内部方法调用让 `@Transactional` 失效」的根。

**向下（它会漏掉谁）** —— 今天的计数表就是答案：

| Bean | 被处理次数 | 为什么 |
| :--- | :---: | :--- |
| 正常 Bean | 2/2 | 注册完成后创建，两个后处理器都看得见 |
| 同批后处理器 | 0/0 | `PostProcessorRegistrationDelegate:257~265` 先全部 `getBean` 收集、再统一注册 |
| 被后处理器 `@Autowired` 的 Bean | 0/0 | 比后处理器还早出生 → 启动日志里 `is not eligible for auto-proxying` |
| 配置类本身 | 0/0 | 在 `invokeBeanFactoryPostProcessors` 阶段就被实例化了 |

从这张表能推出一条实战判断：**启动日志里只要出现 `is not eligible for getting processed by
all BeanPostProcessors`，那个 Bean 上的 `@Transactional` / `@Async` / `@Cacheable` 就一定是假的**。
不是「可能不生效」，是「一定不生效」—— 代理根本没机会套上去。

---

**Q6 · Bean 的作用域到底在控制什么？`request` 在非 Web 环境为什么取不到？**

**作用域 = 「造几个」+「存哪儿」+「谁管埋」三件事的一个属性。** 不是「生命周期长短」这么含糊的说法。

四条判据（本次全部实测，`day3-result.txt`）：

| 作用域 | 两次 `getBean` 同一个？ | 进单例池？ | `close()` 触发销毁回调？ | 非 Web 容器 |
| :--- | :--- | :--- | :--- | :--- |
| `singleton` | 是 | 是 | 是（`@PreDestroy` 1 + `destroy` 1） | 正常 |
| `prototype` | 否 | 否 | 否（0 / 0） | 正常 |
| `request` / `session` | 取不到 | 否 | 不会发生 | `IllegalStateException` |

关键差别在**它们怎么被解析**：

- `singleton` / `prototype` 是 `doGetBean` 里**写死的两个 `if` 分支**，不查表。
  证据：`registerScope("singleton", …)` 直接抛
  `IllegalArgumentException: Cannot replace existing scopes 'singleton' and 'prototype'`（`AbstractBeanFactory:1045`）。
- 其余作用域都在 `scopes` 这张 `Map<String, Scope>` 里，走到第三个分支才查：
  `Scope scope = this.scopes.get(scopeName); if (scope == null) throw …`（367 / 369）。

**所以「取不到」的真实原因是「表里没有这一项」**，报错原文：

```text
java.lang.IllegalStateException: No Scope registered for scope name 'request'
	at AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:369)
```

三个必须接住追问的点：

1. **为什么启动不报？** 因为它不是单例 —— `preInstantiateSingletons` 的条件
   `!isAbstract() && isSingleton() && !isLazyInit()` 不成立，循环不碰它，错误推迟到第一次 `getBean`。
2. **`request` / `session` 什么时候才是真的？** 在 `WebApplicationContext` 里，由 spring-web 的
   `RequestScope` / `SessionScope` 注册（它们依赖 `RequestContextHolder` 里当前线程的请求）。
3. **实战**：纯单测里写 `@Scope("request")` 的 Bean，一定炸；要让它能跑，得加
   `@WebMvcTest` / `@SpringBootTest(webEnvironment = RANDOM_PORT)` 之类有 Web 上下文的测试。
   这也解释了「本地跑得好、单测一跑就 null」的一类怪问题。

---

**Q7 · `proxyMode` 到底解决什么问题？为什么说它是「把错误推迟」？**

**它解决的是「作用域不匹配」，不是「作用域不存在」。**

场景一（最高频）：单例里注入 prototype。

```text
没有代理：三次 probe.id() → 1 / 1 / 1，原型只造了 1 个
加了 TARGET_CLASS：三次 → 2 / 3 / 4，造了 3 个
```

单例只在注入那一刻向容器要了一次；之后手里那个引用不会自己变。这不是 bug，是「引用」语义的必然。

机制（读定义表能看到，`C2b` 段）：

```text
scopedTarget.requestProxy   scope=request      class=-                                       factoryMethod=requestProxy
requestProxy                scope=(singleton)  class=org.springframework.aop.scope.ScopedProxyFactoryBean
```

`ScopedProxyUtils:41` 的 `TARGET_NAME_PREFIX = "scopedTarget."` 就是命名来源；
外层是单例的 `FactoryBean`（产代理），真身被改名单独注册并保留 `scope`。
代理每个方法先 `SimpleBeanTargetSource.getTarget():35` → `beanFactory.getBean("scopedTarget.xxx")`。

场景二（它真正的价值）：单例需要「每个请求一个对象」。
Web 里这靠代理合法地做到 —— 单例持有代理，每次方法调用去当前请求的 `RequestScope` 里取。

**代价必须说清**：同一个错在非 Web 容器里从「`getBean` 时抛」变成了「第一次调用时抛」：

```text
getBean("requestProxy")   → 成功（拿到代理）
requestProxy.id()         → IllegalStateException: No Scope registered for scope name 'request'
	at doGetBean:369 ← getBean:209 ← SimpleBeanTargetSource.getTarget:35 ← CglibAopProxy$DynamicAdvisedInterceptor:691
```

一句话收口：**`proxyMode` 换的是「解析时机」，不是「解析结果」**。
面试里如果只答「加注解就每次新的」，多半会被追一句「那 `request` 在单测里为什么还是不行」，答案就在这。
