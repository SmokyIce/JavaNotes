# Spring · 面试题库

> IOC、AOP、事务、代理、循环依赖、Bean 生命周期 ｜ 对应 W44
> 每道题都要求：**脱稿讲出来**，并给出「我在项目里怎么用的」。
>
> W05 Day 1 补前三题（IOC/DI 的价值 / Bean 何时创建 / 循环依赖）。
> W05 Day 2 补 Q4 / Q5（生命周期顺序 / `BeanPostProcessor` 漏掉谁）。
> W05 Day 3 补 Q6 / Q7（作用域四种判据 / `proxyMode` 是把错误推迟）。
> W05 Day 4 补 Q8 / Q9（多候选的判定链 / `@Resource` 与 `@Autowired` 的报错差别）。
> W05 Day 5 补 Q10（启动期注入失败的异常链怎么读）+ 修正 Q9 里「完全同一个错」的说法（内层一样、外层不同）。
> W06 Day 1 补 Q11 / Q12（代理什么时候套上去 + JDK 与 CGLIB 怎么选 / `this.xxx()` 为什么让事务失效）。
> 「项目案例」是按常见场景写的**讲稿模板**，细节和数字换成自己项目里的真实版本再背。

## 题目

### 1. IOC / DI 到底帮你管理什么？别说「控制反转」，说人话。

**我的回答（自己的话）：**

它管的是**对象图**，不是单个对象。拆成三件：

1. **谁依赖谁**（DI）——把「谁依赖谁」从**每个调用点**搬到**一个装配点**；
2. **造几个、什么时候造**（作用域 + 生命周期）；
3. **造完做什么**（`BeanPostProcessor` / 初始化回调 —— `@Autowired` 灌字段、AOP 织入都挂这一步）。

最硬的证据是「同一套类，手写装配 vs 容器装配」的 diff（W05 Day 1 数出来的）：

| | 手写版 | Spring 版 |
| :--- | ---: | ---: |
| 换实现（jdbc → memory）要改的调用点 | **3** | **0** |
| 加依赖（`OrderService` 多一个参数）要改的调用点 | **3** | **0** |
| 三个入口造出的仓储实例 | **3**（= 3 个连接池） | **1** |

第三行比前两行值钱：手写版不是「写得丑」，是**真的多开了两个连接池**。

**追问预判：**

- 「那 `new` 就不用了？」——业务代码里不用，但**装配点仍然要 `new`**：`@Bean` 方法里就是
  `return new MemoryOrderRepository()`（实测计数 = 1）。变的是「一个装配点」，不是「零个 new」。
- 「DI 有几种方式？」——构造器 / setter / 字段。优先构造器：不可变、能单测、启动就暴露循环依赖。
- 「单构造器为什么不用写 `@Autowired`？」——`determineConstructorsFromBeanPostProcessors`
  在单构造器时直接命中，走 `createBeanInstance:1222` 的 `autowireConstructor` 分支（栈上能看出来）。
- 「注解是怎么生效的？」——注解本身什么都不做。容器扫包时把带注解的类翻译成 BeanDefinition，
  再由后处理器反射灌字段。证据：`new` 出来的对象上 `@Autowired` 字段是 `null`，注册给容器后才有值。
- 「不写 `@Component` 会怎样？」——`NoSuchBeanDefinitionException`
  （`No qualifying bean of type 'X' available`）；被别人依赖时外面包一层
  `UnsatisfiedDependencyException`，message 多一句
  `expected at least 1 bean which qualifies as autowire candidate`。

**项目里的实际案例：**

> 讲稿模板：三个入口（Web Controller / 定时任务 / 冒烟测试）各自 `new` 一遍仓储，
> 上线后表现为「缓存命中率莫名很低」「连接数比预期多两倍」——
> 因为每个入口各有一份缓存和连接池，计数器各算各的。改成容器管理后连接数回到 1 份，命中率立刻正常。
> 落点：装配散在各处不只是代码风格问题，**它会体现在连接数和缓存命中率上**。

---

### 2. Bean 是 `refresh()` 时创建的，还是第一次 `getBean` 时？单例池里有什么？

**我的回答（自己的话）：**

默认在 **`refresh()` 里**。`preInstantiateSingletons()`（`DefaultListableBeanFactory:922`）
遍历所有「非抽象 + singleton + 非 lazy」的定义，逐个 `getBean`（955 行）。
实测时间线：第一个 Bean `+312 ms` 构造、最后一个 `+338 ms`，「容器创建完成」`+354 ms` ——
全在 refresh 内；而且那 312 ms 主要是类加载和扫包，不是 `new` 对象。

两个例外正好把条件反证了：

| Bean | 为什么不预造 | 实测 |
| :--- | :--- | :--- |
| `@Lazy` | `isLazyInit()` = true | `containsSingleton` false → `getBean` 后 true |
| `@Scope("prototype")` | `isSingleton()` = false | 取两次造两个（`p1 == p2 : false`），始终不在池里 |

单例池是 `Map<beanName, instance>`（`singletonObjects`），配合 `earlySingletonObjects`（早期引用）
和 `singletonFactories`（`ObjectFactory`）共三级。实测：定义 13、实例 19；
取过 prototype + lazy 之后定义仍 13、单例只从 19 变 20 —— **原型造完连账都不记**。

**追问预判：**

- 「`getBean` 一个没注册的类型会怎样？」——`NoSuchBeanDefinitionException`，不会「顺手造一个」。
- 「单例池线程安全吗？」——`singletonObjects` 是 `ConcurrentHashMap`；
  创建过程用 `synchronized (this.singletonObjects)` 兜住（`getSingleton(beanName, allowEarlyReference)`）。
- 「`prototype` 谁负责销毁？」——没人。容器只管造，`@PreDestroy` 也不调 —— 原型里别放要关的资源。
- 「`refresh()` 为什么慢？」——本次 354 ms，其中 312 ms 发生在任何 Bean 构造之前：
  类加载 + ASM 扫包 + 解析注解。启动优化常从「缩小扫包范围」下手。
- 「`@Lazy` 除了省启动时间还能干嘛？」——打断循环依赖（把一条边改成延迟注入）。

**项目里的实际案例：**

> 讲稿模板：一个 `@Lazy` 的报表服务本地正常，上线后第一次点报表卡 8 秒。
> 原因是 `@Lazy` 把「建连接池 + 预热」推到了第一次调用，正好砸在用户请求上。
> 落点：**`@Lazy` 不是白拿的性能，是把成本从启动挪到首请求** —— 要么配预热，要么别用。

---

### 3. 循环依赖：为什么字段注入能解、构造器注入不能？

**我的回答（自己的话）：**

差别只在 `doCreateBean` 里三行的**顺序**（`AbstractAutowireCapableBeanFactory`）：

```java
createBeanInstance(...)   // 582：先把对象造出来
addSingletonFactory(...)  // 613：把「半成品」提前登记（仅 singleton + 允许循环时）
populateBean(...)         // 619：再灌字段
```

- 字段注入：582 造出空壳（实测构造时字段全是 `null`）→ 613 登记 → 619 灌。
  B 要 A 时从缓存里拿到**空壳引用**，回头 A 的字段照样被灌满 → 环被时间差化解。
- 构造器注入：参数必须在 582 **之前**就位，那时连空壳都没有 → 直接
  `BeanCurrentlyInCreationException: Requested bean is currently in creation:
  Is there an unresolvable circular reference?`

判据极简：`beforeSingletonCreation` 往 `singletonsCurrentlyInCreation` 里 `add` 失败就抛
（`DefaultSingletonBeanRegistry:353`）。

**追问预判：**

- 「三级缓存为什么要第三级？直接存对象不行吗？」——要留一手给 AOP：
  提前暴露的可能是**代理**而不是原对象，而代理得等 `BeanPostProcessor` 才知道。
  第三级存 `ObjectFactory`，把「造什么」延迟到真有人要。
- 「Spring Boot 2.6 之后为什么默认禁止循环依赖？」——因为「能解」不等于「该写」。
  字段注入解出来的对象在初始化完成前是不完整状态，并发下可能被别人看到半成品。
- 「那怎么改？」——抽第三个类、`@Lazy` 打断一条边、或改成事件 / 回调。
- 「构造器注入就完全没法处理环了？」——是，只能改设计。这也是它被推荐的原因之一：
  **启动时就把设计问题暴露出来**，而不是留到线上跑出半成品。

**项目里的实际案例：**

> 讲稿模板：`OrderService` ⇄ `CouponService` 互相字段注入，一直没报错；
> 某次把其中一个改成构造器注入，应用直接起不来，报 `BeanCurrentlyInCreationException`。
> 当时以为是「Spring 版本升级」，其实是这次改动让环从「能糊过去」变成「糊不过去」。
> 最后抽出 `CouponCalculator` 把边打断。
> 落点：**字段注入能跑，不代表设计没问题；构造器注入是帮你早点发现它。**

### 4. Bean 的生命周期，从造出来到销毁，完整说一遍。

**我的回答（自己的话）：**

一句话骨架：**构造 → 属性注入 → `BPP.before` → 初始化（注解 → 接口 → 配置）→ `BPP.after` → 使用 → 销毁三件套**。

展开成能对上行号的版本（Spring 5.3.39，括号里是同一次运行的实测时间戳）：

| # | 阶段 | 关键位置 |
| -: | :--- | :--- |
| ① | 构造（`+375.7 ms`） | `createBeanInstance` → `BeanUtils.instantiateClass` |
| — | 属性注入 | `populateBean`（**没有回调**，只能用「构造时 null / `@PostConstruct` 里有值」夹出来） |
| ② | `BPP.before`（`+386.7 ms`） | `initializeBean:1796` → `applyBeanPostProcessorsBeforeInitialization:440` |
| ③ | `@PostConstruct`（`+389.0 ms`） | `InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization:157` |
| ④ | `afterPropertiesSet`（`+389.5 ms`） | `invokeInitMethods:1863` |
| ⑤ | `initMethod`（`+390.2 ms`） | `invokeCustomInitMethod:1874` |
| ⑥ | `BPP.after`（`+390.5 ms`） | `applyBeanPostProcessorsAfterInitialization:1805` |
| ⑦⑧⑨ | `@PreDestroy` → `destroy()` → `destroyMethod` | `DisposableBeanAdapter.destroy:194` |

三句要能脱口而出：

1. **`@PostConstruct` 是后处理器调起来的**，不是容器主流程 —— 所以它发生在 ② 里面。
2. **接口优先于配置**：`afterPropertiesSet` 一定在 `initMethod` 之前；两者重名时只跑一次。
3. **销毁是逆序 + 依赖方先死**（`dependentBeanMap` 下游优先），不是单纯栈式逆序。

**追问预判：**

- 「`@PostConstruct` 是谁调的？没有它行不行？」——`CommonAnnotationBeanPostProcessor`
  （继承 `InitDestroyAnnotationBeanPostProcessor`）。它靠 classpath 上有没有 `javax.annotation-api`
  决定注册与否：JDK 11+ 已把 `javax.annotation` 移出 JDK，没有这个 jar 时**不报错、只是不执行**。
- 「prototype 的销毁回调会被调吗？」——一次都不会。不是忘了调，
  是 `AbstractBeanFactory.registerDisposableBeanIfNecessary:1942` 用 `!mbd.isPrototype()` 在登记阶段就短路了。
- 「`@Bean` 不写 `destroyMethod` 会怎样？」——默认 `INFER_METHOD`，容器自动找 `close()` / `shutdown()`
  调掉；要关掉写 `destroyMethod = ""`。实测：一个被调 1 次、一个 0 次。
- 「初始化回调里 `this` 是代理吗？」——不是。代理是 ⑥ 之后才换进去的，
  所以「同类内部方法调用让 `@Transactional` 失效」是必然的。

**项目里的实际案例：**

> 讲稿模板：某个 `CacheWarmer` 在 `@PostConstruct` 里去查数据库，本地起服务时快、
> 上测试环境偶尔超时，导致应用启动失败。一开始以为是数据库慢，
> 后来在 `@PostConstruct` 里打印栈才确认它跑在「容器初始化」阶段 —— 整个应用的启动都被它拖住。
> 改成实现 `ApplicationRunner`（容器 refresh 完成后才跑）就解决了。
> 落点：**`@PostConstruct` 属于「Bean 自己的初始化」，不要在里面做跨系统的重活。**

---

### 5. `BeanPostProcessor` 对哪些 Bean 不生效？为什么说它是理解 AOP 的入口？

**我的回答（自己的话）：**

先讲它怎么工作：`postProcessAfterInitialization` 返回的对象就是最终放进单例池的对象 ——
AOP 在这一步把原对象**换成代理**。所以「初始化回调里的 `this`」和「注入到别人手里的引用」
是两个不同的对象，这是同类内部调用让 `@Transactional` 失效的根。

再讲它会漏掉谁（实测计数表，两个后处理器，正常 Bean 是 2/2）：

| Bean | 次数 | 原因 |
| :--- | :---: | :--- |
| 正常 Bean | 2/2 | 注册完成后创建，两个后处理器都看得见 |
| 同批后处理器 | **0/0** | `PostProcessorRegistrationDelegate:257~265`：先全部 `getBean` 收集，再统一注册 |
| 被后处理器 `@Autowired` 的 Bean | **0/0** | 比后处理器还早出生，拿不到全部后处理 |
| 配置类本身 | **0/0** | 在 `invokeBeanFactoryPostProcessors` 阶段就被实例化了 |

**判断口径（这条最值钱）**：启动日志里出现
`is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)`
时，那个 Bean 上的 `@Transactional` / `@Async` / `@Cacheable` **一定不生效** ——
不是「可能」，是代理根本没机会套上去。

**追问预判：**

- 「同一批后处理器为什么互相看不见？」——因为注册分两步：先把这一批全部 `getBean` 造好放进局部 List，
  排序后再统一注册。收集的时候谁都没进列表。
- 「那谁看得见后处理器自己？」——更早阶段注册的。`CommonAnnotationBeanPostProcessor` 是
  `PriorityOrdered`，在 253 行就注册完了，所以后处理器身上的 `@PostConstruct` 照样执行（实测两行都打出来了）。
- 「`@Order` 影响什么？」——只影响后处理器的调用顺序（先 before 后 after 各按同一顺序遍历），
  不影响 Bean 的创建顺序。
- 「怎么避免提前创建？」——后处理器 / 监听器里别 `@Autowired` 普通业务 Bean；
  确实需要就 `ObjectProvider` 延迟取。

**项目里的实际案例：**

> 讲稿模板：一个自定义 `HandlerInterceptor` 里 `@Autowired` 了 `OrderService`，
> 启动日志有那条 `not eligible`，但没人看。后来 `OrderService` 上加了 `@Transactional`，
> 线上发现事务不回滚 —— 排查半天，最后是启动日志里那句告警救了命。
> 改成 `ObjectProvider<OrderService>` 后恢复。
> 落点：**启动日志里那条 `not eligible` 不是噪音，是「这个 Bean 没有代理」的官方通告。**

---

### 6. Spring 的作用域有几种？`request` 作用域在非 Web 环境为什么取不到？

**我的回答（自己的话）：**

**作用域不是「生命周期长短」，是三个问题的一个答案：造几个 / 存哪儿 / 谁管埋。**

四条判据（W05 Day 3 全部实测）：

| 作用域 | 两次 `getBean` 同一个？ | 进单例池？ | `close()` 触发销毁回调？ | 非 Web 容器 |
| :--- | :--- | :--- | :--- | :--- |
| `singleton` | 是 | 是 | 是（`@PreDestroy` 1 + `destroy` 1） | 正常 |
| `prototype` | 否 | 否 | 否（0 / 0） | 正常 |
| `request` / `session` | 取不到 | 否 | 不会发生 | `IllegalStateException` |

关键在**解析路径不一样**：

- `singleton` / `prototype` 是 `doGetBean` 里**写死的两个 `if` 分支**，不查表。证据：想覆盖直接报错
  `IllegalArgumentException: Cannot replace existing scopes 'singleton' and 'prototype'`（`AbstractBeanFactory:1045`）。
- 其余作用域都在 `scopes` 这张 `Map<String, Scope>` 里，走到第三个分支才查表；
  查不到就抛（`AbstractBeanFactory:367 / 369`）：

```text
java.lang.IllegalStateException: No Scope registered for scope name 'request'
```

**为什么启动不报错？** 因为它不是单例，`preInstantiateSingletons` 的条件
（`!isAbstract() && isSingleton() && !isLazyInit()`）不成立，循环不碰它 ——
**错误推迟到第一次 `getBean`**。

**追问预判：**

- 「那 Web 里为什么就行？」——spring-web 的 `RequestScope` / `SessionScope` 在
  `WebApplicationContext` 初始化时注册进去，它们从 `RequestContextHolder` 里取当前线程绑定的请求。
- 「单测里怎么才不炸？」——要么有 Web 上下文（`@WebMvcTest` / `@SpringBootTest(webEnvironment=…)`），
  要么别用 `request` 作用域，改成方法参数传。
- 「`prototype` 谁销毁？」——没人。`registerDisposableBeanIfNecessary` 第一句
  `!mbd.isPrototype() && ...` 就短路了，连登记都没有。
- 「自定义作用域怎么写？」——实现 `Scope` 五个方法（容器只用 `get` / `remove` /
  `registerDestructionCallback`），`registerScope` 注册，Bean 上 `@Scope("名字")`。

**项目里的实际案例：**

> 讲稿模板：一个导出服务用 `@Scope("request")` 存当前登录人，线上没问题；
> 后来加了一个定时任务也想复用它，单测一跑就 `No Scope registered for scope name 'request'`。
> 排查发现单测没有 Web 上下文。改法两种：单测加 `@SpringBootTest(webEnvironment = RANDOM_PORT)`，
> 或者把「当前登录人」从 Bean 里挪出去，改成方法参数（更干净）。
> 落点：**看见 `No Scope registered`，先问「这个容器有没有 Web 上下文」，不要怀疑注解写错了。**

---

### 7. `@Scope(proxyMode)` 解决什么问题？为什么说它是「把错误推迟」而不是「修好错误」？

**我的回答（自己的话）：**

它解决的是**作用域不匹配**，不是**作用域不存在**。

**场景一（最高频）：单例里注入 prototype。** 单例只在注入那一刻向容器要了一次，
之后那个引用不会自己变 —— 实测三次 `id()` 全是同一个，原型只造了 1 个。
加 `proxyMode = TARGET_CLASS` 后，注入的是代理，**每次方法调用**回容器要一次：`2 / 3 / 4`，造了 3 个。

**它的实现是「注册期把一个定义拆成两个」**（读定义表能直接看到）：

```text
scopedTarget.requestProxy   scope=request      class=-                                       factoryMethod=requestProxy
requestProxy                scope=(singleton)  class=org.springframework.aop.scope.ScopedProxyFactoryBean
```

外层是单例的 `FactoryBean`（产代理），真身被改名 `scopedTarget.` 前缀单独注册、保留原 `scope`。
代理每次方法调用先 `SimpleBeanTargetSource.getTarget():35` → `beanFactory.getBean("scopedTarget.xxx")`。

**场景二（它真正的价值）**：单例需要「每个请求一个对象」。Web 里靠代理合法做到 ——
单例持有代理，每次调用去当前请求的 `RequestScope` 里取。

**为什么说它是「把错误推迟」**：同一个 `request` Bean，加了代理之后
`getBean` 不抛了，**第一次调用方法才抛**，而且是同一个异常：

```text
getBean("requestProxy") → 成功（拿到代理）
requestProxy.id()       → IllegalStateException: No Scope registered for scope name 'request'
	at doGetBean:369 ← getBean:209 ← SimpleBeanTargetSource.getTarget:35 ← CglibAopProxy$DynamicAdvisedInterceptor:691
```

**追问预判：**

- 「`TARGET_CLASS` 和 `INTERFACES` 怎么选？」——有接口用 `INTERFACES`（JDK 代理，不用生成子类），
  没接口只能 `TARGET_CLASS`（CGLIB）。被代理的类不能是 `final`、方法不能是 `final`。
- 「代理会不会有性能开销？」——每次方法调用多一次 `getBean` 查找 + 一次代理分派；
  高频路径上别把代理当普通字段用。
- 「prototype 注入单例的另一种修法？」——`ObjectProvider<T>` / `@Lookup` /
  每次从 `ApplicationContext.getBean()` 取。`ObjectProvider` 最干净（不引入代理，也不依赖容器）。
- 「`@Scope` 注解和 `Scope` 接口混了怎么办？」——两者同名，同一个文件里 `implements` 要用全限定名
  `org.springframework.beans.factory.config.Scope`，否则编译报「对 Scope 的引用不明确」。

**项目里的实际案例：**

> 讲稿模板：一个 `TokenService`（单例）里 `@Autowired` 了 `@Scope("prototype")` 的
> `Signer`（每次要有独立的状态）。压测发现并发下签名串错乱 —— 因为单例里那个 `Signer`
> 从启动到停机都是同一个，状态被多线程共享。改成 `proxyMode = TARGET_CLASS`（或 `ObjectProvider`）后正常。
> 落点：**「单例里注入原型」不会报错，只会静默变成「其实是单例」——这是最典型的作用域陷阱。**

---

### 8. 同一个接口有三个实现，`@Autowired` 怎么挑？挑不出来报什么错？怎么收场？

**我的回答（自己的话）：**

它按一条**四级链**挑，顺序是写死的（`DefaultListableBeanFactory:1633-1655`）：

```text
按类型筛出候选（@Qualifier 在这一步就把范围缩好）
  → ① @Primary 有且只有一个   → 用它
  → ② @Priority 最大的        → 用它
  → ③ 名字匹配：字段名 / 参数名 == bean 名（或别名）
  → ④ 都不行 → resolveNotUnique → NoUniqueBeanDefinitionException
```

**关键的第一个反直觉点：`@Autowired` 不是纯按类型。** 类型匹配到多个之后，它还有一级「按名字」回退，
对字段来说这个名字就是字段名。三个候选（`alipayChannel` / `wechatChannel` / `unionPayChannel`）：

```text
字段名 channel       → NoUniqueBeanDefinitionException:
                        expected single matching bean but found 3: alipayChannel,wechatChannel,unionPayChannel
字段名 wechatChannel → 注入成功，拿到 wechat        ← 第 ③ 级回退，一个注解都没多写
```

**第二个反直觉点：`@Primary` 在名字之前。** 同样字段名 `wechatChannel`，容器里加一个 `@Primary`
的 bean，注入的就变成 `preferred` 了 —— 名字对得上也不管用。所以「按字段名回退」是最后一级，不是第二级。

**第三个点：这个异常死在 `refresh()`，不是 `getBean`。** 那个 bean 没标 `@Lazy`、也没人依赖它，
`preInstantiateSingletons` 自己就把它造了：

```text
at DependencyDescriptor.resolveNotUnique:220
at DefaultListableBeanFactory.doResolveDependency:1369
at AutowiredAnnotationBeanPostProcessor$AutowiredFieldElement.resolveFieldValue:710
at AutowiredAnnotationBeanPostProcessor.postProcessProperties:408
at AbstractAutowireCapableBeanFactory.populateBean:1431
at AbstractAutowireCapableBeanFactory.doCreateBean:619
```

**收场方式按场景选：**

| 场景 | 写法 |
| :--- | :--- |
| 有一个明确的默认实现 | `@Primary` |
| 只有一个调用点要特殊实现 | `@Qualifier("beanName")` |
| 想按名字、且不想被类型匹配干扰 | `@Resource(name = "beanName")` |
| 全都要（策略/插件式） | `List<T>`（按 `@Order`）或 `Map<String,T>`（key = bean 名） |

**追问预判：**

- 「`@Qualifier` 和 `@Primary` 谁优先？」——`@Qualifier` 更早：它在 `findAutowireCandidates` 那一步
  就把候选集缩掉了，`@Primary` 是候选集**里面**再挑。
- 「`@Qualifier` 点名一个不存在的会怎样？」——`NoSuchBeanDefinitionException: expected at least 1 bean
  which qualifies as autowire candidate`，message 里会打印 `@Qualifier("xxx")` 原文。
- 「`@Priority` 和 `@Order` 一样吗？」——不一样。`@Priority`（`javax.annotation.Priority`）参与注入的
  候选取舍；`@Order` 只管集合排序和 AOP/后处理器的执行顺序，**不参与**「多候选挑一个」。
  实测：三个候选 order 分别是 1/2/3，没有 `@Primary` 时照样 `NoUniqueBeanDefinitionException` ——
  如果 `@Order` 能救，就不会抛。（另：源码 1713 行，两个候选 `@Priority` 相同也会抛 `NoUnique`，
  说「Multiple beans found with the same priority」；这条只读过源码，没实测。）
- 「为什么 `@Bean` 方法返回接口类型时 bean 名是方法名？」——`@Bean` 的默认名字就是方法名，
  所以 `@Bean PayChannel preferredChannel()` 的名字是 `preferredChannel`，而不是类名小写。

**项目里的实际案例：**

> 讲稿模板：支付渠道从 1 个扩到 3 个之后，`OrderService` 里那个 `@Autowired PayChannel payChannel`
> 第二天上线直接 `NoUniqueBeanDefinitionException`，本地却没事 —— 因为本地只配了沙箱的那一个实现。
> 修法是给「默认渠道」加 `@Primary`，需要指定渠道的调用点改成 `@Qualifier` 或 `Map<String, PayChannel>`
> 按名字取。落点：**多实现这件事一旦上线才出现，说明环境之间的 bean 集合不一致，
> 这类问题必须在启动期炸，而不是等到某笔订单进来才炸。**

---

### 9. `@Resource` 和 `@Autowired` 的区别，除了「名字 vs 类型」还有什么？

**我的回答（自己的话）：**

真正的差别在**名字对不上时报什么错** —— 这一步才说明两者的机制不同。

```text
@Resource(name = "channel")（不存在）
  → NoSuchBeanDefinitionException: No bean named 'channel' available
    ↑ 它根本没查类型：容器里明明有 3 个 PayChannel，也照样报「没有这个名字」

@Resource 字段名 channel（不存在，即默认名）
  → NoUniqueBeanDefinitionException: expected single matching bean but found 3: ...
    ↑ 退回按类型，内层和 @Autowired 踩的是同一个 NoUnique（但外层包装不同，见 Q10）
```

**源码一句话**（`CommonAnnotationBeanPostProcessor.autowireResource:546`）：

```java
if (this.fallbackToDefaultTypeMatch && element.isDefaultName && !factory.containsBean(name)) {
    resource = beanFactory.resolveDependency(descriptor, ...);   // 退回按类型
}
else {
    resource = beanFactory.resolveBeanByName(name, descriptor);  // 硬按名字
}
```

而 `isDefaultName` 在 `:649` 算出：没写 `name` 属性 → 默认名 = **字段名**（`member.getName()`）。
所以「`@Resource` 默认按字段名」这句话对，但它只是「默认名」，查不到还会退回按类型。

**再补两个经常被一起问的边界：**

```text
@Autowired（required=true）List<T>，容器里零候选 → 照样抛，不是空集合
@Autowired(required=false)                      → 给 null，不是空集合
List 的顺序 = @Order；Map 的顺序 = bean 定义注册顺序，key = bean 名
```

**追问预判：**

- 「`@Resource` 支持构造器参数吗？」——不支持，它只能标在字段和 setter 上；
  `@Autowired` 可以标构造器、方法、参数。
- 「`@Resource` 能不写 name 又指定类型吗？」——`@Resource(type = Xxx.class)` 可以，
  但 name 非空时 type 只做校验，最终还是按名字取。
- 「`@Autowired` 为什么能标在构造器上而 `@Resource` 不能？」——因为两者是不同的后处理器：
  `AutowiredAnnotationBeanPostProcessor` 实现了 `SmartInstantiationAwareBeanPostProcessor`
  （能决定用哪个构造器），`CommonAnnotationBeanPostProcessor` 只做属性/方法注入。
- 「`@Inject` 呢？」——JSR-330，等价于 `@Autowired`（按类型），需要 `javax.inject` 依赖，
  且没有 `required` 属性。

**项目里的实际案例：**

> 讲稿模板：一个 `@Resource(name = "smsSender")` 的字段，重构时把 bean 改名成 `aliyunSmsSender`，
> 启动直接 `No bean named 'smsSender' available` —— 一眼就能定位，因为报错里带着名字。
> 同一个改动如果是 `@Autowired`，报的会是 `No qualifying bean of type 'SmsSender'`，
> 你得自己去想「以前是按哪个名字进来的」。落点：**显式点名的写法可读性和可排查性都更好，
> 代价是它绕过了「按类型 + 多候选」那套机制，注入失败不会被 `@Primary` 救回来。**

---

### 10. 启动报 `UnsatisfiedDependencyException` / `BeanCreationException`，从异常链能读出什么？

**我的回答（自己的话）：**

**异常链是分层写信息的：外层说「谁在灌字段的时候炸的」，中间层说「哪个 bean」，内层说「为什么」。**
读反了就会像早期排查那样，只看到「找不到一个构造器」却不知道该去改哪。

实测三种写法（同一个容器、三个 `PayChannel` 候选，`day5-result.txt` S4 段）：

```text
@Autowired 多候选
  UnsatisfiedDependencyException
    ← NoUniqueBeanDefinitionException: expected single matching bean but found 3: ...

@Resource(name = "channel")（不存在）
  BeanCreationException: Injection of resource dependencies failed
    ← NoSuchBeanDefinitionException: No bean named 'channel' available

@Resource 默认名（字段名 channel，不存在）
  BeanCreationException: Injection of resource dependencies failed
    ← NoUniqueBeanDefinitionException: ...
```

**为什么外层不一样？** 因为两个注解由不同的后处理器负责，各自 `catch` 完换了个壳：

```java
// AutowiredAnnotationBeanPostProcessor.resolveFieldValue:712-713
catch (BeansException ex) {
    throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(field), ex);
}
// CommonAnnotationBeanPostProcessor.postProcessProperties:331-332
catch (Throwable ex) {
    throw new BeanCreationException(beanName, "Injection of resource dependencies failed", ex);
}
```

而 `NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException extends BeansException`
（`javap` 一眼可查），所以两处 `catch` 都接得住它，谁接住就用谁的壳。

**所以看报错的口诀是：**

```text
UnsatisfiedDependencyException  → @Autowired 那条路（构造器 / 字段 / 方法参数）
BeanCreationException + "Injection of resource dependencies failed" → @Resource 那条路
内层的 NoUniqueBeanDefinitionException + 候选名清单 → 挑不出来，去加 @Primary / @Qualifier / 收成 List
内层的 NoSuchBeanDefinitionException + "No bean named" → 名字写错了，不是类型问题
```

**追问预判：**

- 「`@Autowired` 的注入失败为什么会死在 `refresh()` 而不是 `getBean`？」——因为没被依赖的普通单例，
  `preInstantiateSingletons` 自己就把它造了；栈底是 `refresh:591`，不是 `getBean`。
- 「那什么时候才死在 `getBean`？」——它是 `@Lazy`、或者 `prototype`、或者根本不在预实例化的三个条件里
  （`!isAbstract() && isSingleton() && !isLazyInit()`）。
- 「还有一种报错你见过吗？」——`is not eligible for getting processed by all BeanPostProcessors
  (for example: not eligible for auto-proxying)`。这条不是异常，是启动日志的 INFO，
  但它意味着**代理没套上**，那个类上的 `@Transactional` / `@Async` 一定是假的。
- 「怎么在不启动整个 Spring 的情况下确认是哪个 bean？」——异常 message 里就带 beanName
  （`Error creating bean with name 'injectionChoice.NeedsOne'`），外层那句比栈更省事。

**项目里的实际案例：**

> 讲稿模板：一次灰度环境启动失败，日志只有一行 `UnsatisfiedDependencyException`，
> 运维直接回滚了。真正原因在第三层：`expected single matching bean but found 2`，
> 灰度环境多加载了一个 mock 实现。落点：**这类问题必须在启动期炸（这次确实炸了），
> 但排查时要顺着异常链一层层读，别只搜第一行类名** —— 第一行只告诉你「谁在灌字段」，
> 真正的原因在最后一层。

---

> W05 封板：Q1~Q10 十道题全部有「我的回答 + 追问预判」，对应 `BoardCheck` 的 23 条断言。

### 11. Spring AOP 的代理是什么时候套上去的？JDK 代理和 CGLIB 怎么选？

**我的回答（自己的话）：**

**AOP 没改我的类，它改的是「容器里放谁」。** 我写的 `GreetingImpl` 一个字节没变，
容器在 `BeanPostProcessor.postProcessAfterInitialization` 阶段调
`AbstractAutoProxyCreator.wrapIfNecessary:328`：这个 Bean 有顾问就
`createProxy(..., new SingletonTargetSource(bean))` 返回代理（`:344-347`），没有就原样 `return bean`（`:351`）。

从那以后，**谁 `getBean`、谁被注入，拿到的都是代理；拿到真身的人没有切面。**

实测三条（`w06-spring-aop/day1-result.txt`）：

| | 结果 |
| :--- | :--- |
| 手工 `new` 的实例调方法 | `before=0`（切面完全不认识它） |
| 容器里那个对象 | `jdk.proxy2.$Proxy22`，实现 `Greeting` + `SpringProxy` + `Advised` + `DecoratingProxy` |
| `@Before` 里的 `getThis()` vs `getTarget()` | 代理 vs 真身，`==` 为 `false` |

选型在 `DefaultAopProxyFactory.createAopProxy:58`：

```java
if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
    // 目标本身是接口 / 已是代理 / lambda → 仍 JDK；否则 ObjenesisCglibAopProxy
}
else {
    return new JdkDynamicAopProxy(config);
}
```

翻译：**显式要求 CGLIB，或者目标没有接口 → CGLIB；否则 JDK。**
我实测的三种情况：有接口默认 → `jdk.proxy2.$Proxy22`；`proxyTargetClass=true` →
`GreetingImpl$$EnhancerBySpringCGLIB$$<hash>`（父类就是 `GreetingImpl`）；没有接口 → 自动 CGLIB。

两者的本质差别只有一句：**JDK 生成「实现同接口的兄弟」，CGLIB 生成「覆写方法的子类」**。
所以：

- JDK：`(GreetingImpl) proxy` 直接 `ClassCastException`；按实现类取 Bean 会失败；
- CGLIB：`final` 类 / `final` 方法**静默不拦**（我那个 `final sealed()` 调用前后 `before` 增量是 `0`，
  启动日志一个字都不提）；`private` 构造器也不行。

**追问预判：**

- 「Spring 默认用 JDK，Spring Boot 为什么改成 CGLIB？」——`proxyTargetClass=true`。
  为的是「按实现类注入」还能用：JDK 代理只实现接口，`@Autowired GreetingImpl` 会拿不到。
- 「按实现类取 Bean 报什么错？」——两个不同的错，别混：
  `getBean(GreetingImpl.class)` → `NoSuchBeanDefinitionException`（按类型匹配，Bean 定义声明的类型是接口）；
  `getBean("greeting", GreetingImpl.class)` → `BeanNotOfRequiredTypeException`（名字命中，实例类型不对）。
- 「怎么判断一个对象是不是 AOP 代理？」——`AopUtils.isAopProxy`，判据是
  `instanceof SpringProxy && (Proxy.isProxyClass || 类名含 $$)`（`AopUtils:68-70`），
  **不是看类名猜**。自己手写的 `$Proxy0` 没实现 `SpringProxy`，所以不会被误认。
- 「代理对象的 `toString()` 会触发切面吗？」——会。`Object` 三个方法也被转发进 handler
  （手写版实测 +1/+1/+1），所以 debug 打日志也可能触发懒加载或多一次 SQL。

**项目里的实际案例：**

> 讲稿模板：一个 `OrderService` 加了 `@Transactional`，日志里事务日志就是不出现。
> 排查顺序：① `AopUtils.isAopProxy(bean)` 是不是 false；② 启动日志有没有
> `not eligible for auto-proxying`；③ 是不是 `final` 方法 / `final` 类；
> ④ 最后才是「是不是 `this` 调的」。前三步都在查「代理有没有套上」，第四步才查「有没有拿到代理」。

---

### 12. `this.xxx()` 为什么让 `@Transactional` / `@Async` 失效？三种修法各有什么代价？

**我的回答（自己的话）：**

**因为代理只活在「别人拿到的那根引用」上，而 `this` 是真身。**
类内部写 `this.hi()`，编译成 `invokevirtual GreetingImpl.hi`，直接落在真身上，`InvocationHandler` 根本没被调用。

我用纯 JDK（不引 Spring）就能证明：一个代理对象调 `placeThenCancel()`，
里面再调 `place()` 和 `cancel()` —— **业务真跑了 3 个方法，handler 只进 1 次**。

交给 Spring 之后，三个数字（`SelfInvocation`，`day1-result.txt`）：

| 写法 | 被拦的方法 | before |
| :--- | :--- | ---: |
| `this.hi()` | `[Greeting.outer]` | **1** |
| `self.hi()`（`@Lazy` 注入自己） | `[Greeting.outerViaSelf, Greeting.hi]` | **2** |
| `AopContext.currentProxy().hi()` | `[Greeting.outerViaAopContext, Greeting.hi]` | **2** |

三种写法业务返回一模一样，差别只在「有没有回到代理」。

**三种修法，代价不一样：**

| 修法 | 做法 | 代价 |
| :--- | :--- | :--- |
| 注入自己 | `@Autowired @Lazy Greeting self;` → `self.hi()` | 这是**三层**代理：`@Lazy` 解析代理 → AOP 代理 → 真身（`self==bean` 为 false，`TargetSource` 是 `ContextAnnotationAutowireCandidateResolver$1`）；不加 `@Lazy` 就是循环依赖，构造器注入直接 `BeanCurrentlyInCreationException` |
| `AopContext` | `((Greeting) AopContext.currentProxy()).hi()` | 必须 `@EnableAspectJAutoProxy(exposeProxy = true)`，否则运行时 `IllegalStateException: Cannot find current proxy…`；而且 `AopContext` 是 `NamedThreadLocal`（`AopContext:50`），**必须在同一线程** —— 线程池 / `@Async` 里必炸 |
| 拆 Bean | 把被调的方法挪到另一个 Bean，注入进来 | 最干净，但要改结构。很多时候这才是正确答案 |

**追问预判：**

- 「为什么 `self` 是三层？不能直接注入吗？」——不加 `@Lazy`，`GreetingImpl` 依赖 `GreetingImpl`，
  创建到一半就要一个完整的自己 → 循环依赖。加了 `@Lazy`，注入的是一个「调用时才回容器解析」的代理，
  而它解析出来的正是 AOP 代理，所以中间多一层。
- 「`currentProxy()` 为什么不能跨线程？」——ThreadLocal 的本质就是不共享。所以异步方法里做 AOP 自调用，
  只能靠注入 `self`，不能靠 `AopContext`。
- 「`@Transactional` 失效还有别的原因吗？」——有，而且都指向同一件事「代理没套上或没拿到」：
  ① `final` 方法 / `final` 类（CGLIB 覆写不了）；② 方法不是 `public`（Spring 5 的 JDK 代理只拦 public）；
  ③ Bean 被提前创建，启动日志 `not eligible for auto-proxying`；④ 异常被自己 `catch` 了（这是事务边界问题，不是代理问题）。
- 「那 `@Async` 呢？」——完全同构：它也是一个 `MethodInterceptor`，失效原因逐条对应，
  只是「没生效」的表现从「没回滚」变成「还是同步执行」。

**项目里的实际案例：**

> 讲稿模板：一段「下单成功后发券」的逻辑写在 `OrderService.create()` 里，
> 直接调 `this.grantCoupon()`（那个方法上有 `@Transactional(propagation = REQUIRES_NEW)`）。
> 结果主事务回滚时，券也跟着回滚了 —— 因为 `REQUIRES_NEW` 根本没生效，自调用没经过代理。
> 最后是把发券拆到 `CouponService`。落点：**「传播级别不生效」这种问题，
> 先看调用是不是经过代理，别先去怀疑数据库。**

---

### 13. 切点写错会怎样？`within` / `execution` / `args` 三者的差别是什么？

**我的回答（自己的话）：**

**写错了不会报错，这才是它难查的原因。** 切点由两个轴组成：在哪（类路径）和什么方法（名字/参数/注解）。
两个方向各有各的安静法。

**写窄 —— `within` 写成接口，命中 0。** `within` 问的是「这个方法**声明**在哪个类里」，
不是「这个对象实现了谁」。接口只是对外的写法清单，不是代码定义的位置。
实测同一批方法，`within(Shop)` 命中 **6** 个，`within(Buyer)`（接口）命中 **0** 个。

**写宽 —— 多拦了不想拦的类。** `within(com…shop..*)` 从 6 个变 **8** 个，
同包里另一个 `Cart` 也被收进来了。用包路径图省事，代价就是整个包连坐。

**再写宽一点，容器直接起不来。** `execution(* com…day2..*.*(..))` 里的 `..` 是
「零个或多个子包」，把 `@Configuration` 类的 `@Bean` 工厂方法也匹配了 ——
配置类为了 `@Bean` 返回单例，本身是 CGLIB 代理。于是：

```text
造 shop → 命中宽切点 → 通知要 wideAspect → wideAspect 正在造
→ BeanCurrentlyInCreationException
```

报错文案写 `Is there an unresolvable circular reference?`，但依赖关系一点都没绕，
绕的是**创建顺序**。真正的线索只有栈里那行 `BrokenCfg$$EnhancerBySpringCGLIB.shop(<generated>)`。
（`@Aspect` 自己反而没事，被 `AnnotationAwareAspectJAutoProxyCreator:110` 的 `isAspect` 豁免了。）

**`execution` 和 `args` 的分歧。** 同一个 `Shop#buy(Object)`：

```text
buy((Object)"x") 命中 execution buy(Object) = true   ← 看声明类型
buy((Object)"x") 命中 args(String)          = true   ← 看运行时实参
```

`args(String)` 还会命中 `Warehouse#stock(String)` —— **它只认参数，不认方法属于谁**。
而 `buy(String,int)` 明明 String 开头却不命中，因为 `args(String)` 要求「恰好一个参数」。
代价在源码里：带 `args()` 的切点 `isRuntime():353` 返回 true，每次调用重算，没有缓存。

**收尾一句：** 切点是对着**目标类**算的（`AspectJExpressionPointcut:455` 的
`getMostSpecificMethod(method, targetClass)`），跟用 JDK 还是 CGLIB 无关；
所以同一个表达式换代理方式结果一样，但 JDK 代理下「接口里没有的方法」连调用都发不出去。

**面试官可能追问：**

- 「`within(接口)` 真的一个都拦不到？」——是。而且不报错、不告警，日志里什么都没有。
- 「怎么快速判断切点写窄了？」——临时换成 `within(目标类)` 对比命中数。
- 「切点写宽了有什么代价？」——轻则多写脏数据（审计日志、写操作），重则容器起不来。
- 「`args()` 有性能问题吗？」——有，它让切点变成运行时切点；能用 `execution` 就别用 `args`。

**项目里的实际案例：**

> 讲稿模板：给「订单相关」加了操作日志切面，切点写的是
> `within(com.xxx.order..*)`。上线后发现库存服务的日志也混进了订单表 —— 因为库存的
> `StockService` 被放在 `order` 包下面。落点：**切点用包路径是「省事」，代价是边界不可控；
> 要么写到类，要么用自定义注解标记目标方法。**

---

### 14. `@AfterReturning` 能改返回值吗？`@AfterThrowing` 能吞异常吗？

**我的回答（自己的话）：**

**都不能。只有 `@Around` 有 `proceed()`。**

不是写法习惯问题，是物理上不可能：这两个通知的方法签名都是 `void`，
一个 void 方法拿什么去替换 `proceed()` 的返回值，又拿什么去「取消」一个已经抛出的异常。

**`@AfterReturning` 改不动。** 返回值只是被当参数传给通知方法（`AspectJAfterReturningAdvice:64-68`），
没有回写通道。实测：

```text
ret.append("-touched")              调用方拿到 hello-touched     ← 生效
ret = new StringBuilder("REPLACED") 调用方拿到 hello-touched     ← 不生效
```

所以「`@AfterReturning` 能改返回值」这句话，**只对可变对象成立** ——
改的是共用对象的内部（副作用），不是返回值本身。对 `String` / `int` 这种换值就等于没改。

**`@AfterThrowing` 吞不掉。** 源码 `AspectJAfterThrowingAdvice:62-72` 就是个 try/catch：

```java
try { return mi.proceed(); }
catch (Throwable ex) {
    if (shouldInvokeOnThrowing(ex)) { invokeAdviceMethod(getJoinPointMatch(), null, ex); }
    throw ex;                    // :70 —— 异常必须继续抛
}
```

实测调用方照样收到 `IllegalArgumentException`，通知只是「看到了」。

**两种「配了不执行」的静默失效**，机制完全一样 —— 类型接不住就跳过：

- `returning` 声明 `String`、方法返回 `int` → `isAssignableValue(String, 100)` 为 false → 不执行；
- `throwing` 声明 `IllegalStateException`、抛的是 `IllegalArgumentException` → `isAssignableFrom` 为 false → 不执行。

两种都**启动不报错、调用不报错**。所以「异常日志有时有有时没有」，第一件事是看
`throwing` 写的是不是 `Throwable`。

**一条反直觉的：** 在 `@AfterThrowing` 里抛新异常，会顶掉 `:70` 那行 `throw ex`，
而且**框架不会帮你保留原异常** —— 实测 `cause = null`、`suppressed = 0`，原异常彻底丢失。
想留证据必须自己写 `throw new RuntimeException("…", ex)`。
（对比 `CompletableFuture.whenComplete`，那里框架至少塞进了 suppressed。）

**唯一能改、能吞的是 `@Around`**，因为它拿到 `ProceedingJoinPoint`，可以决定调不调、
改返回值、catch 掉。但吞掉业务异常通常是坏主意：事务、监控、调用方全都看不到失败。

**面试官可能追问：**

- 「那顺序呢？」——`@Around 进入 → @Before → @AfterReturning → @After → @Around 拿到返回值 → @Around finally`。
  `@After` 就是 `proceed()` 外面那层 `finally`。
- 「`@AfterReturning` 想改返回值，正确做法是什么？」——用 `@Around` 改 `proceed()` 的返回值，
  或者干脆在业务方法里返回一个可变的结果对象。
- 「`returning` 写 `Object` 有什么好处？」——类注释写明：声明成 `Object` 时**永远**执行，不会因类型被跳过。

**项目里的实际案例：**

> 讲稿模板：做过一个「统一响应包装」的切面，一开始用 `@AfterReturning` 想把返回值塞进
> `Result<T>`，写完发现一点效果没有、也不报错。落点：**`@AfterReturning` 是「看一眼」，
> 要改结果就得用 `@Around`；同理，想吞异常也只有 `@Around` 一条路。**
