# Spring · 面试题库

> IOC、AOP、事务、代理、循环依赖、Bean 生命周期 ｜ 对应 W44
> 每道题都要求：**脱稿讲出来**，并给出「我在项目里怎么用的」。
>
> W05 Day 1 补前三题（IOC/DI 的价值 / Bean 何时创建 / 循环依赖）。
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
