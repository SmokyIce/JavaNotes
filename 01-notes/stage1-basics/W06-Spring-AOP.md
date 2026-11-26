# W06 · Spring AOP

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟨 进行中（Day 1 完成 11-26）　｜　开始日期：2026-11-26　｜　完成日期：____-__-__

> 💡 ⚠️ 这周不要只看视频，自己写一个简单 AOP Demo。把 this.xxx() 和 self.xxx() 彻底搞明白。

---

## 一、本周知识点

- [x] AOP · 切面 · 代理对象
- [x] JDK 动态代理
- [ ] CGLIB
- [x] this.xxx() 与 self.xxx()

> Day 1 勾三格，判断标准都不是「会写 `@Aspect`」：
>
> **第一格 `AOP · 切面 · 代理对象`**：能说出「AOP 不改你的类，它让容器在注册期把真身换成代理」，
> 并且能指出这个替换发生在 `AbstractAutoProxyCreator.wrapIfNecessary:328`（后处理器的 `postProcessAfterInitialization` 阶段）。
> 检验方式：手工 `new` 出来的同一套类，切面计数必须是 **0**。
>
> **第二格 `JDK 动态代理`**：能手写 `InvocationHandler` + `Proxy.newProxyInstance`，
> 并说清「一次调用 = 一次 handler」「嵌套调用不额外进 handler」「类是按接口列表缓存的」。
> 还要能对上 Spring 的实现：`JdkDynamicAopProxy:126` 就是同一句 `newProxyInstance`。
>
> **第四格 `this.xxx() 与 self.xxx()`**：三个数字能当场背 —— `this` **1** 次、`self` **2** 次、`currentProxy()` **2** 次，
> 并且能解释 `self` 为什么是三层（`@Lazy` 解析代理 → AOP 代理 → 真身）。
>
> **第三格 `CGLIB` 没勾**：Day 1 只证明了「什么时候选它」（`DefaultAopProxyFactory:58`）和
> 「final 方法静默不被拦」，还没讲 `Enhancer` 怎么生成子类、`MethodProxy` 与反射谁快。
> 留 Day 2。

## 二、核心问题（能用自己的话脱稿讲出来，才算学会）

1.
2.
3.

## 三、代码 / 实验

- 代码目录：`02-code/stage1-basics/w06-spring-aop/`
- 本周动手内容：

```text
Day 1（已完成 11-26）  工具链：从 .gitkeep 建工程；lib/ 沿用 W05 的 7 个 jar
                       + 新增 aspectjweaver-1.9.7.jar（2.0 MB，@Aspect / @Pointcut 靠它，
                         只放 spring-aop 会编译不过 —— 网上 curl repo1.maven.org 现下）
                       + JdkProxy：纯 JDK，不引 Spring。A 不代理 / B 一次代理 / C 嵌套调用 /
                         D 代理套代理 / E 类复用 / F Object 三方法 / G 强转 / H 非 public 接口的包 —— 八段
                       + ProxyIdentity：S1 手工 new 不进切面 / S2 容器里是什么类 /
                         S2b 按实现类取 Bean 的两个错 / S3 getThis 与 getTarget /
                         S4 proxyTargetClass=true / S5 无接口自动 CGLIB + final 方法静默失效 /
                         S6 切面自己不代理 / S7 通知顺序
                       + SelfInvocation：S1 this / S2 self / S3 currentProxy / S4 self 三层探针 /
                         S5 没开 exposeProxy 的报错原文 / S6 数字对照
```

### Day 1 · 2026-11-26（周四）· 「代理是什么时候套上去的」

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 W05 封板三段（对象图 / 生命周期 / 边界），限时 3 分钟 | 口头过 |
| 15 分钟 | 手写 JDK 动态代理，数一次调用进几次 handler | `day1/JdkProxy.java` |
| 20 分钟 | 同一套类交给 Spring `@Aspect`，打印 `this` 与容器里那个对象的身份 | `day1/ProxyIdentity.java` |
| 10 分钟 | 追问 `this.xxx()`，三种修法各跑一遍 | `day1/SelfInvocation.java` |
| 5 分钟 | 快照 + 源码取证 + 本节笔记 + `today.md` 覆盖 | 三份文件 |

实测快照：`w06-spring-aop/day1-result.txt`（JDK 17.0.12 / Spring 5.3.39 / aspectjweaver 1.9.7 / Windows 11，189 行）

#### 1. 先给结论：AOP 到底做了什么

一句话：**它没有改你的类，它改的是「容器里放谁」。**

```text
你的类 GreetingImpl  ←── 原封不动，一个字节都没变
        ↑ 被包进 SingletonTargetSource
代理 $Proxy22 / GreetingImpl$$EnhancerBySpringCGLIB$$…   ←── 容器里放的是这个
        ↑ 谁注入、谁 getBean，拿到的都是它
调用方
```

所以「切面生效」的条件只有一条：**调用方手里拿的是代理**。
W05 Day 2 见过的那句 `not eligible for auto-proxying`、和今天 `this.xxx()` 的失效，是同一件事的两面。

#### 2. `JdkProxy`：把地基拆到只剩一句话

```java
Proxy.newProxyInstance(loader, new Class<?>[]{Shop.class}, handler)
```

八段实测（数字都在 `day1-result.txt`）：

| 段 | 做的事 | handler 进入次数 | 附带观测 |
| :- | :--- | ---: | :--- |
| A | 直接调 `impl.place()` | **0** | 业务照样跑 1 次 |
| B | 代理调一次 | **1** | 类名 `jdk.proxy1.$Proxy0`；`proxy instanceof Shop` = true |
| C | 代理里再调自己的另一个方法 | **1** | 业务真的执行了 3 个方法体 |
| D | 代理套代理 | **2** | 内外层是同一个类，各转一次 |
| E | 同接口再生成两个 | — | `p1` 与 `p2` 同类；多实现一个接口 → `$Proxy1` |
| F | `hashCode` / `toString` / `equals` | +1 / +1 / +1 | 三个**都**进 handler |
| G | 强转成 `ShopImpl` | — | `ClassCastException: class jdk.proxy1.$Proxy0 cannot be cast to …ShopImpl` |
| H | 接口不是 public | — | 代理类跑到 `com.newt.java48.w06.day1.$Proxy4`（接口所在包） |

**C 段是整周最重要的一次观测。** `placeThenCancel` 内部调 `place()` 和 `cancel()`：

```text
handler 进入次数 = 1
handler 看到的调用链 = [Shop.placeThenCancel]
业务里 place 执行次数 = 1   cancel 执行次数 = 1
```

业务真跑了 3 个方法，handler 只进 1 次。原因很朴素：那两次调用编译成 `invokevirtual ShopImpl.place`，
**直接落在真身上，跟代理对象没有任何关系**。这就是 `this.xxx()` 不走代理的全部物理内容 ——
不需要 Spring，也不需要「事务」「切面」这些词。

**E 段拆掉一个误解。** `$Proxy0` 只生成一次，按「接口列表」缓存（JDK `Proxy:309` 的 `proxyCache`，
427/438 行 `proxyCache.sub(intfs)`）。所谓「动态代理」，动态的是**生成时机**，不是每次都生成新类；
`newProxyInstance` 每次做的只是换一个 handler。

**F 段是个埋伏。** `toString()` 也走 handler —— debug 时随手打一行日志，可能就触发了一次切面、
甚至触发了一次懒加载。这条在排查「为什么多次莫名其妙的 SQL」时很值钱。

#### 3. Spring 的 JDK 代理 = 你刚写的那一句

`JdkDynamicAopProxy:122-127`：

```java
public Object getProxy(@Nullable ClassLoader classLoader) {
    ...
    return Proxy.newProxyInstance(determineClassLoader(classLoader), this.proxiedInterfaces, this);
}
```

第三个参数 `this` —— 因为 `JdkDynamicAopProxy` 自己就实现了 `InvocationHandler`。
我们写的 `CountingHandler` 就是它的替身：Spring 只是把「计数器」换成了「顾问链」：

```java
// JdkDynamicAopProxy.invoke
List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass); // :225
...
retVal = invocation.proceed();                                                                    // :241
```

#### 4. 什么时候用 JDK、什么时候用 CGLIB

`DefaultAopProxyFactory.createAopProxy:56-72`，第 58 行是全部答案：

```java
if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
    // 目标本身是接口 / 已是代理 / lambda → 仍走 JDK；否则 ObjenesisCglibAopProxy
}
else {
    return new JdkDynamicAopProxy(config);
}
```

翻译成人话：

| 条件 | 结果 | 实测 |
| :--- | :--- | :--- |
| 有接口 + 默认配置 | JDK | `jdk.proxy2.$Proxy22`，`isJdkDynamicProxy=true` |
| `proxyTargetClass = true` | CGLIB | `GreetingImpl$$EnhancerBySpringCGLIB$$<hash>`，父类就是 `GreetingImpl` |
| **没有接口** | 自动 CGLIB | `PlainJob$$EnhancerBySpringCGLIB$$<hash>`（不用你配） |

CGLIB 的代价当场就看到了：`PlainJob` 里那个 `final sealed()` 调用前后 `before` 增量是 **0** ——
生成子类才能覆写方法，`final` 覆写不了，于是切面**完全不执行，启动日志一个字都不提**。

#### 5. 「代理」和「真身」的身份差

```text
类名                    = jdk.proxy2.$Proxy22
isAopProxy              = true
实现的接口               = [Greeting, SpringProxy, Advised, DecoratingProxy]
@Before 收到的 this      = jdk.proxy2.$Proxy22
@Before 收到的 target    = GreetingImpl
this == target          = false
this == 容器里那个 bean   = true
```

后面那两个接口不是摆设：`AopUtils.isJdkDynamicProxy` 的判据是
`object instanceof SpringProxy && Proxy.isProxyClass(...)`（`AopUtils:81-83`），
**不是「看类名像不像代理」**。所以自己手写的 `$Proxy0` 不会被认成 AOP 代理。

**S2b 是一道很容易答错的题 ——两种取法，两个不同的错：**

```text
getBean(GreetingImpl.class)             → NoSuchBeanDefinitionException: No qualifying bean of type 'GreetingImpl' available
getBean("greeting", GreetingImpl.class) → BeanNotOfRequiredTypeException: Bean named 'greeting' is expected to be of type
                                          'GreetingImpl' but was actually of type 'jdk.proxy2.$Proxy22'
```

按**类型**取，连候选都算不上 —— Bean 定义里声明的类型是接口 `Greeting`，`GreetingImpl` 压根不匹配；
按**名字 + 类型**取，名字先命中，再拿实例对类型，才对不上。
这也是 Spring Boot 2.x 把 `proxyTargetClass` 默认改成 `true` 的原因之一：让「按实现类注入」还能用。

#### 6. `this.xxx()`：三种写法，三个数字

| 写法 | 被拦的方法（实测 `@Before` 清单） | before |
| :--- | :--- | ---: |
| `this.hi()` | `[Greeting.outer]` | **1** |
| `self.hi()`（`@Lazy` 注入自己） | `[Greeting.outerViaSelf, Greeting.hi]` | **2** |
| `AopContext.currentProxy().hi()` | `[Greeting.outerViaAopContext, Greeting.hi]` | **2** |

三个方法业务返回一模一样（`outer->hi S1` / `outerViaSelf->hi S2` / …），差别只在「有没有回到代理」。

`self` 那一格最容易讲错，所以专门加了 `TargetSource` 探针：

```text
self 类名                  = jdk.proxy2.$Proxy22      ← 和 bean 同一个类，不能当证据
self == 容器 bean          = false
self 的 TargetSource 类    = ContextAnnotationAutowireCandidateResolver$1
self 的 target == bean     = true
```

即真实链路是 **`@Lazy` 解析代理 → AOP 代理 → `GreetingImpl` 真身**，三层。
那个 `$1` 就是 `buildLazyResolutionProxy:83` 的匿名 `TargetSource`，它的 `getTarget()`
每次调用都回容器 `doResolveDependency` 一次（`:93-95`），所以拿到的是 AOP 代理而不是真身。

`@Lazy` 在这里不是为了性能，是为了**打断「自己注入自己」的循环依赖** ——
不加它，构造器注入会直接 `BeanCurrentlyInCreationException`。

S5 打出的是 `AopContext.currentProxy()` 的原文：

```text
IllegalStateException: Cannot find current proxy: Set 'exposeProxy' property on Advised
to 'true' to make it available, and ensure that AopContext.currentProxy() is invoked
in the same thread as the AOP invocation context.
```

最后半句是重点：`AopContext` 就是一个 `NamedThreadLocal`（`AopContext:50`）。
所以**线程池 / `@Async` 里 `currentProxy()` 必然拿不到** —— 这条留到 W08 讲异步时回来看。

#### 7. 通知顺序

```text
@Around 进入 hi
@Before Greeting.hi(..)
@After hi
@Around 返回 hi
```

只有 `@Around` 能包住别人（它有 `proceed()`）；`@After` 在 `@Around` 返回**之前**就跑完了 ——
它就是 `proceed()` 外面那层 `try/finally` 里的 `finally`。所以「`@After` 在 `@Around` 之后执行」是错的。

#### 8. 这一周还差的

- 切点表达式的四种写法（`execution` / `within` / `args` / `@annotation`）还没系统过一遍；
- `@AfterReturning` 改返回值、`@AfterThrowing` 吞异常，还没试；
- 两个切面之间的 `@Order`，还没试；
- **手写 mini AOP**（`ProxyFactory` + `MethodInterceptor` + `Advisor`，不用 `@Aspect`）——
  这是 `java-48week-plan.md` 第 6 周的硬要求，放 Day 3。

## 四、踩坑与 Bug 复盘

| 现象 | 原因 | 排查过程 | 解决 | 底层原理 |
| :--- | :--- | :--- | :--- | :--- |
| 文案写「按实现类取 Bean 抛 `BeanNotOfRequiredTypeException`」，实跑抛 `NoSuchBeanDefinitionException` | 按**类型**找时，Bean 定义声明的类型是接口 `Greeting`，`GreetingImpl` 连候选都不是 | 两种取法都跑了一遍：`getBean(Class)` 与 `getBean(name, Class)` | 结论拆成两个错，写清「按类型 vs 按名字 + 类型」的差别 | 类型匹配走 Bean 定义声明的类型；名字命中才轮到「实例 instanceof 校验」 |
| 编译报 11 个「找不到符号: 类 AopUtils」 | `AopUtils` 在 `org.springframework.aop.support`，不在 `…aop.framework` | javac 一次全列出来了 | 改 import | `spring-aop` 的包划分是 `framework`（代理实现）/ `support`（工具类）/ `interceptor`（拦截器），`Advised` 和 `AopUtils` 不在同一个包 |
| `Advised.getTargetSource().getTarget()` 报 `unreported exception Exception` | `TargetSource.getTarget()` 声明了 `throws Exception` | 报错行直接指到 `.getTarget()` | `main` 加 `throws Exception` | 接口签名带受检异常，调用方必须表态；这是 `TargetSource` 支持「每次取新对象」的代价 |
| 用 `self.getClass() == bean.getClass()` 论证「self 是另一层代理」，实测 `true`，结论站不住 | JDK 代理类按接口列表缓存，两个不同实例完全可能共用一个 `$Proxy` | 换成 `self == bean`（false）+ 打印 `getTargetSource().getClass()`（`…Resolver$1`）+ `getTarget() == bean`（true） | 结论改成「三层」，证据换成 `TargetSource` 类型 | 「同类」不等于「同实例」，更不等于「同一层」；证明层级要看**包装关系**，不能看类名 |
| 以为顺序是 `before → 目标 → after → around 退出` | 想当然按「通知列表」的顺序推 | 给 `@After` 也补了一条事件流水，再跑一遍 | 改成 `@Around 进入 → @Before → 目标 → @After → @Around 返回` | `@After` 在 `proceed()` 的 `finally` 里执行，位置在「方法之后、Around 返回之前」 |
| 以为「代理类名固定是 `jdk.proxy1.$ProxyN`」，差点把它写进结论 | 代理类放在哪个包，取决于**接口的可见性**：public → `jdk.proxy1` 动态模块；package-private → 接口所在的包 | 补了 H 段：同一个 handler 换成 package-private 接口，类名从 `jdk.proxy1.$Proxy0` 变成 `com.newt.java48.w06.day1.$Proxy4` | 结论改成「包名由接口可见性决定，不是固定值」 | 非 public 接口只有在同包内可见，代理类必须放进那个包才能 `implements` 它；老 JDK 上 `com.sun.proxy.$Proxy0` 的由来也是这条 |
| `System.out.println("…com.sun.proxy.\$Proxy0…")` 编译报「非法的转义字符」 | Java 字符串里 `$` 不需要转义，`\$` 是非法转义 | javac 直接指到那一行 | 去掉反斜杠 | 在 shell / markdown 里写 `\$` 是为了防止变量替换，但这段是 **Java 源码**，照抄过去就成语法错 —— 跨语言的转义规则不能顺手带过去 |

## 五、面试会怎么问

- 「AOP 是什么？」——别背「面向切面编程」，讲 **容器里放的是代理不是真身**，再补一句
  「替换发生在 `AbstractAutoProxyCreator.wrapIfNecessary:328`，后处理器的 `postProcessAfterInitialization` 阶段」。
- 「JDK 动态代理和 CGLIB 的区别？」——JDK：实现同样接口、靠 `InvocationHandler` 转发、**不是**目标的子类；
  CGLIB：生成子类覆写方法、所以不能代理 final 类 / final 方法。选谁看 `DefaultAopProxyFactory:58`。
- 「为什么 Spring 默认用 JDK？」——因为「面向接口」是它推荐的用法；而且 JDK 代理不需要生成字节码。
  追问：「Spring Boot 为什么改成 CGLIB？」——`proxyTargetClass=true`，为的是「按实现类注入」还能用。
- 「`final` 方法上的注解为什么不生效？」——CGLIB 覆写不了，切面静默不执行，**没有任何警告**。
- 「`this.xxx()` 为什么事务失效？」——`this` 是真身，`invokevirtual` 直连，代理根本没参与。
  追问：「怎么修？」——注入自己（`@Lazy`，注意三层代理）、`AopContext.currentProxy()`（要 `exposeProxy=true`、
  同线程）、或者干脆把方法拆到另一个 Bean。
- 「`AopContext` 为什么在线程池里会失败？」——它是 `NamedThreadLocal`，不跨线程传。
- 「`@Around` 和 `@Before` 谁能改返回值？」——只有 `@Around`（`proceed()` 的返回值你能换）。
- 「代理对象的 `toString()` 会触发切面吗？」——会，`Object` 的三个方法也被转发进 handler。
- 「怎么判断一个对象是不是 AOP 代理？」——`AopUtils.isAopProxy`，判据是 `instanceof SpringProxy` +
  `Proxy.isProxyClass` 或类名含 `$$`，**不是看类名猜**。

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 11-26 | 四 | W06 Day 1：代理什么时候套上去 —— 手写 JDK 代理 / 代理身份与选型 / `this` 与 `self` | 65 min | `JdkProxy` + `ProxyIdentity` + `SelfInvocation` + `day1-result.txt`（189 行）+ `day1-spring-source.txt`（8 段） | ✅ |

---

## 参考要点（先自己答，再对照）

**Q1 · AOP 到底做了什么？为什么说它「没有改你的类」？**

它改的是**容器里放谁**，不是你的字节码。

1. 你的 `GreetingImpl` 一个字节没变，`new` 出来的对象也没有任何增强（实测 `before=0`）；
2. 容器在 `BeanPostProcessor.postProcessAfterInitialization` 阶段调
   `wrapIfNecessary`（`AbstractAutoProxyCreator:328`）：有顾问就 `createProxy(..., new SingletonTargetSource(bean))`
   返回代理，没有就原样 `return bean`（`:351`）；
3. 从此「谁 `getBean` / 谁被注入」，拿到的都是代理。**拿到代理的人有切面，拿到真身的人没有。**

由此可以推出今天全部三个现象：

- `this.xxx()` 失效 —— `this` 就是真身，`invokevirtual` 直连，代理没参与（JdkProxy C 段：业务跑 3 个方法、handler 只进 1 次）；
- `new` 出来的对象不吃切面 —— 它没走过容器，没人问过「要不要包代理」；
- W05 那句 `not eligible for auto-proxying` —— 那个 Bean 出生太早，等后处理器注册好时它已经定型了。

**Q2 · JDK 动态代理和 CGLIB，怎么选、各自怕什么？**

| | JDK 动态代理 | CGLIB |
| :--- | :--- | :--- |
| 前提 | 目标**实现接口** | 目标**能被继承**（非 final 类、非 final 方法、构造器可见） |
| 机制 | `Proxy.newProxyInstance` 生成实现同接口的类，方法转发给 `InvocationHandler` | 生成子类覆写方法 |
| 与目标的关系 | 兄弟（只共享接口）→ `(GreetingImpl) proxy` 会 CCE | 父子 → `proxy instanceof GreetingImpl` 为 true |
| 怕什么 | 目标没接口（这时 Spring 自动换 CGLIB）；按实现类注入会失败 | `final` 类 / `final` 方法静默失效；`private` 构造器不行 |

选谁看 `DefaultAopProxyFactory.createAopProxy:58`：`isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces()`
→ 进 CGLIB 分支，否则 JDK。Spring 默认 `proxyTargetClass=false`，Spring Boot 2.x 改成 `true`。

**三个实测数字**：`$Proxy22`（JDK）vs `$$EnhancerBySpringCGLIB$$…`（CGLIB）；
`PlainJob` 无接口时自动 CGLIB；`final sealed()` 前后 `before` 增量 **0**。

**Q3 · `this.xxx()` 为什么让切面失效？三种修法各有什么代价？**

根因只有一句：**代理只活在「别人拿到的那根引用」上**。类内部写 `this.hi()`，编译成
`invokevirtual GreetingImpl.hi`，直接落在真身上 —— `JdkProxy` C 段已经证明过了
（业务跑 3 个方法，handler 只进 1 次）。

| 修法 | 做法 | 代价 |
| :--- | :--- | :--- |
| 注入自己 | `@Autowired @Lazy Greeting self;` 然后 `self.hi()` | 三层代理（`@Lazy` 解析代理 → AOP 代理 → 真身）；不加 `@Lazy` 是循环依赖 |
| `AopContext` | `((Greeting) AopContext.currentProxy()).hi()` | 必须 `@EnableAspectJAutoProxy(exposeProxy = true)`；**必须同线程**（`NamedThreadLocal`），线程池里必炸 |
| 拆 Bean | 把被调的方法挪到另一个 Bean，注入进来 | 最干净，但会改结构（很多时候这才是正确答案） |

实测：`this` **1** 次、`self` **2** 次、`currentProxy()` **2** 次。

这条不只对 AOP 成立 —— `@Transactional`、`@Async`、`@Cacheable` 全是同一个病根：
**它们都靠代理开闭，而 `this` 不经过代理。**
