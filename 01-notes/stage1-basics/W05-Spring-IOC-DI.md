# W05 · Spring IOC / DI

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟨 进行中（Day 1 11-20）　｜　开始日期：2026-11-20　｜　完成日期：____-__-__

> 💡 目标理解：Spring 到底在帮你管理什么？

---

## 一、本周知识点

- [x] Bean · IOC · DI
- [ ] Bean 生命周期
- [ ] Bean 作用域
- [ ] Spring 到底在帮你管理什么

> Day 1 勾第一格：`Bean · IOC · DI`。
> 判断标准不是「背出 IOC 是控制反转、DI 是依赖注入」，而是三条：
> **① 同一套类，手写装配要改 3 处、Spring 版改 0 处（能当场数出来）；**
> **② 能说清对象是「谁 new 的、在 refresh 的哪一步 new 的」（能指到栈上的行号）；**
> **③ 能解释「字段注入的循环依赖为什么能过、构造器注入为什么过不去」（能指到源码顺序）。**
>
> 第四格「Spring 到底在帮你管理什么」要等 Day 6 收尾时才勾 —— 现在只有半张答案。
> 第二、三格（生命周期、作用域）留到 Day 2 / Day 3，今天只做了 singleton / prototype 的对照。

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

## 四、踩坑与 Bug 复盘

| 现象 | 原因 | 排查过程 | 解决 | 底层原理 |
| :--- | :--- | :--- | :--- | :--- |
| `SpringWiring` 明明一个 `new` 都没有，计数却打出 3；`AppConfig` 也打出 3 | 统计代码自己把针脚 `"new JdbcOrderRepository("` 当字符串常量写在被数的文件里 | 一眼看出「3」正好等于统计代码里 `Src.count` 调用的次数 → 自指 | 改成 `Src.countNew(src, "JdbcOrderRepository")`，针脚在 `Src` 内部拼 | 自指统计（quine 类问题）必须把针脚藏到被数文件之外 |
| `IllegalStateException: GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once` | 先 `new AnnotationConfigApplicationContext(AppConfig.class)`（内部已 refresh），又 `register()` + `refresh()` | 异常信息直接点名 | `new AnnotationConfigApplicationContext()` → `register(AppConfig.class, 额外类)` → `refresh()` | `GenericApplicationContext` 的 `refreshed` 标志只能翻一次；要动态加定义得在 refresh 之前 register |
| `@Lazy` 那段的结论不成立：`containsSingleton("lazyBean")` 一开始就是 true | A 段为了打印 bean 名单写了 `ctx.getBean(name)`，把 LazyBean 和 PrototypeTask 提前造了 | 输出里 LazyBean 的构造行出现在 A 段的 `getBean(name)` 循环中间 —— 比「容器创建完成」晚，但在 E 段之前 | A 段改读 `getBeanDefinition(name).getBeanClassName()` | 读 **注册表** 不触发构造；`getBean(name)` 会触发。调试「什么时候造」时不能用 `getBean` 枚举 |
| 险些写错：以为栈上的 `getSingleton:234` 是「查三级缓存」 | `DefaultSingletonBeanRegistry` 有两个同名重载 | 去源码数行：234 行是 `singletonFactory.getObject()`，180 行那个重载才是查三级缓存 | 文案改成「234 = 真正开始造对象的入口」 | 同名重载看栈必须对行号，否则结论会反 |

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

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 11-20 | 五 | W05 Day 1：IOC / DI —— 手写装配 vs 容器装配，谁 new、何时 new、循环依赖 | 70 min | `ManualWiring` + `SpringWiring` + `ContainerQuestions` + `day1-result.txt`（355 行）+ 源码取证 | ✅ |

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
