# W06 · Spring AOP

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟨 进行中（Day 2 完成 11-27）　｜　开始日期：2026-11-26　｜　完成日期：____-__-__

> 💡 ⚠️ 这周不要只看视频，自己写一个简单 AOP Demo。把 this.xxx() 和 self.xxx() 彻底搞明白。

---

## 一、本周知识点

- [x] AOP · 切面 · 代理对象
- [x] JDK 动态代理
- [x] CGLIB
- [x] this.xxx() 与 self.xxx()

> Day 1 勾三格，Day 2 补上第三格 `CGLIB`。判断标准都不是「会写 `@Aspect`」：
>
> **第一格 `AOP · 切面 · 代理对象`**：能说出「AOP 不改你的类，它让容器在注册期把真身换成代理」，
> 并且能指出这个替换发生在 `AbstractAutoProxyCreator.wrapIfNecessary:328`（后处理器的 `postProcessAfterInitialization` 阶段）。
> 检验方式：手工 `new` 出来的同一套类，切面计数必须是 **0**。
>
> **第二格 `JDK 动态代理`**：能手写 `InvocationHandler` + `Proxy.newProxyInstance`，
> 并说清「一次调用 = 一次 handler」「嵌套调用不额外进 handler」「类是按接口列表缓存的」。
> 还要能对上 Spring 的实现：`JdkDynamicAopProxy:126` 就是同一句 `newProxyInstance`。
>
> **第三格 `CGLIB`**（Day 2 补）：能手写 `Enhancer` 生成子类，说清「它是目标的子类而不是兄弟」
> （实测父类就是 `Calc`、`proxy instanceof Calc` 为 true），并答出三条边界 ——
> final 类（`Cannot subclass final class`）、final 方法（拦不到）、private 方法（看不见），
> 三种都**不报错**。还要能解释「Spring 为什么不重复执行构造器」= Objenesis。
>
> **第四格 `this.xxx() 与 self.xxx()`**：三个数字能当场背 —— `this` **1** 次、`self` **2** 次、`currentProxy()` **2** 次，
> 并且能解释 `self` 为什么是三层（`@Lazy` 解析代理 → AOP 代理 → 真身）。

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

Day 2（已完成 11-27）  无新增 jar（CGLIB 已在 spring-core 里，包名 org.springframework.cglib）
                       + PointcutForms：八种切点各一个 @Before，10 次调用打一张命中矩阵；
                         S2 写窄（within 接口=0）/ S3 写宽（整包多拦 Cart）/
                         S4 args 看实参 vs execution 看声明 / S5 匹配的是目标类不是代理类 /
                         S6 JDK 代理下「接口里没有的方法」连调用都发不出去
                       + WidePointcutTrap：反面教材。宽切点把 @Configuration 的 @Bean 方法也拦了，
                         容器 BeanCurrentlyInCreationException 起不来；S2 是收到业务包的修法
                       + AdviceTypes：S1 换引用无效但改对象有效 / S2 returning 类型不匹配静默不执行 /
                         S3 @AfterThrowing 吞不掉 / S4 throwing 类型是硬过滤 /
                         S5 通知抛新异常（带 cause vs 不带）/ S6 只有 @Around 能吞 / S7 四通知顺序
                       + AspectOrder：@Order 数字小的在外层；对调即翻转；都不写时按注册顺序
                       + CglibProbe：A 手写 Enhancer / A2 谁进拦截器 / B final 类报错原文 /
                         C Spring 侧对照 / D MethodProxy vs 反射耗时 / E 构造器只跑一次（Objenesis）
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

### Day 2 · 2026-11-27（周五）· 「切点写宽了写窄了，通知吞不吞得掉」

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 1：`wrapIfNecessary:328` / `DefaultAopProxyFactory:58` / `this` 为什么失效 | 口头过 |
| 20 分钟 | 八种切点写法对照，同一批方法各打一次 | `day2/PointcutForms.java` |
| 15 分钟 | `@AfterReturning` 改返回值 + `@AfterThrowing` 吞异常，各试两次 | `day2/AdviceTypes.java` |
| 10 分钟 | 两个切面的 `@Order`，正反各一组 | `day2/AspectOrder.java` |
| 5 分钟 | 补 Day 1 留的 CGLIB（`Enhancer` / `MethodProxy` / Objenesis） | `day2/CglibProbe.java` |

实测快照：`w06-spring-aop/day2-result.txt`（288 行，5 个入口全 exit=0）+ `day2-spring-source.txt`（6 段源码）

#### 1. 先给结论：切点错在哪两个方向

`execution` 只是切点的一半。完整的写法由两个轴组成：

```text
在哪（类路径）         within(Shop) / within(shop..*) / execution(* com..*.*(..))
什么方法（谓词）       方法名 / 参数 / 注解 / args()
```

两个方向都会出事，而且都**不报错**：

| 写错了 | 现象 | 实测 |
| :--- | :--- | :--- |
| 写窄（`within(Buyer)` 接口） | 一条都不生效，安静 | 命中 **0** |
| 写宽（`within(shop..*)`） | 同包的 `Cart` 一起被拦 | 6 → **8** 个 |
| 写宽到 `com..day2..*` | 连 `@Configuration` 都被拦，容器**起不来** | `BeanCurrentlyInCreationException` |

#### 2. 八种切点的一张矩阵

调用脚本固定 10 次：`buy(String)` / `buy((Object)"x")` / `buy(String,int)` / `count()` /
`tagged()` / `plain()` / `Cart#add` / `Cart#size` / `Warehouse#stock` / `toString()`。

| 切点 | 命中数 | 命中了谁 |
| :--- | ---: | :--- |
| `execution(* Shop.buy(String))` | 1 | `Shop#buy(String)` |
| `execution(* Shop.buy(Object))` | 1 | `Shop#buy(Object)` |
| `within(Shop)` | 6 | Shop 的 6 个方法（含不在接口里的 `plain()`） |
| `within(shop..*)` | 8 | 多出 `Cart#add` / `Cart#size` |
| `within(Buyer)` 接口 | **0** | 空 |
| `args(String)` | 4 | 含 `Shop#buy(Object)`、`Warehouse#stock` |
| `args(String,..)` | 5 | 再多个 `Shop#buy(String,int)` |
| `@annotation(Traced)` | 1 | `Shop#tagged()` |

`within` 的语义只要记一句：**它问的是「这个方法声明在哪个类里」**，不是「这个对象实现了谁」。
接口是对外的写法清单，不是代码定义的位置 —— 所以 `within(接口)` 恒为 0，且没有任何提示。

#### 3. `args` 与 `execution` 的分歧点

```text
Shop#buy(Object) 的声明参数类型 = java.lang.Object
buy((Object)"x") 命中 execution buy(Object) = true
buy((Object)"x") 命中 args(String)          = true
```

同一个方法，两个表达式给出相反判断：

- `execution` 看**方法签名上的类型**（`Object`）；
- `args` 看**这一枪打出去的实参类型**（`String`）。

`args(String)` 的命中清单还暴露另一件事：它把 `Warehouse#stock(String)` 也收了 ——
**`args` 只认参数，不认方法属于谁**。反过来，`buy(String,int)` 明明是 String 开头却不命中，
因为 `args(String)` 要求「恰好一个参数」。

代价也在源码里：带 `args()` 的切点会被判成「运行时切点」（`AspectJExpressionPointcut.isRuntime:353`
返回 `mayNeedDynamicTest()`），每次调用都要再算一遍；`execution` / `within` / `@annotation`
这种静态切点算一次就进 `shadowMatchCache`（`:130`，key = `Method`）。
**能写 `execution` 就别写 `args`。**

#### 4. 切点是对着「目标类」算的

换成默认 JDK 代理后，容器里是 `jdk.proxy2.$Proxy19`，里面根本没有 `Shop` 这个名字 ——
`within(Shop)` 照样命中。依据是 `AspectJExpressionPointcut:455`
`AopUtils.getMostSpecificMethod(method, targetClass)`：匹配的输入是**目标类上的那个方法**。

推论有两条，都实测过：

- 同一个表达式，JDK 代理和 CGLIB 代理的匹配结果完全一样；
- 但 JDK 代理下「接口里没有的方法」**连调用都发不出去**：`(Shop) proxy` 直接 CCE，
  只能绕到真身上调 —— 两条路都不进切面。**这就是「加了 `@Transactional` 却没事务」的第二条路：方法没写在接口里。**

#### 5. 切点写宽了，容器起不来

`execution(* com.newt.java48.w06.day2..*.*(..))` 里的 `..` 是「**零个或多个**子包」，
所以连 `day2` 包里的 `@Configuration` 类一起匹配。因果链四步：

```text
container 调 shop()（走的是 BrokenCfg 的 CGLIB 代理）
  → 命中宽切点 → 通知要 wideAspect 这个 Bean
    → wideAspect 正在创建中 → BeanCurrentlyInCreationException
```

异常链 5 层，报错文案写的是 `Is there an unresolvable circular reference?` ——
**但这里的依赖关系一点都没绕，绕的是「创建顺序」**。真正的线索只有一行栈：

```text
at WidePointcutTrap$BrokenCfg$$EnhancerBySpringCGLIB$$<hash>.shop(<generated>)
at WidePointcutTrap$BrokenCfg$$EnhancerBySpringCGLIB$$<hash>.wideAspect(<generated>)
```

为什么炸的是配置类而不是切面自己？`AnnotationAwareAspectJAutoProxyCreator:110-111` 里
`isInfrastructureClass` 多认了一个条件 —— `isAspect(beanClass)` 为 true 就当基础设施、不走代理。
`@Configuration` 没有这层豁免。

修法：把 `execution` 收到业务包 `…day2.shop..*`，配置类在 `day2` 不在 `day2.shop`，天然出局。

#### 6. 改得动吗、吞得掉吗

一句话：**只有 `@Around` 有 `proceed()`，只有它能改返回值、能吞异常。**
其余四种通知都是「路过看一眼」，而且类型接不住就**静默跳过**。

```text
S1  @AfterReturning 里 ret = new StringBuilder("REPLACED")
    调用方拿到 hello-touched        ← 换引用无效，改对象内部有效
S2  returning 声明 String，方法返回 int
    通知从来没执行过                ← 启动不报错、调用不报错
S3  @AfterThrowing 想吞异常
    调用方照样收到 IllegalArgumentException
S4  throwing 声明 IllegalStateException，抛 IllegalArgumentException
    静默跳过
S5  通知里 throw new RuntimeException("advice-bare")
    cause = null、suppressed = 0    ← 原异常彻底丢失
S6  @Around + try/catch
    调用方什么都不知道              ← 唯一能吞的写法
S7  顺序：@Around 进入 → @Before → @AfterReturning → @After → @Around 拿到返回值 → @Around finally
```

S1 那条最容易被说成「`@AfterReturning` 能改返回值」——**只对可变对象成立**。
`ret` 是局部参数，重新赋值只改了那个引用；`append` 之所以生效，是因为对象是共用的。

源码依据：
`AspectJAfterReturningAdvice:64-68` 把返回值当参数传给 void 方法，没有回写通道；
`:79` 的 `shouldInvokeOnReturnValueOf` 决定要不要执行 —— `returning=String` 而返回值是 `100` 时，
`:100` 的 `isAssignableValue(String, 100)` 为 false，于是整条通知不执行。
`AspectJAfterThrowingAdvice:62-72` 更直白：catch 里判类型、调通知、然后 `:70 throw ex`。

S5 是这轮最值得记的一条：**别指望框架帮你保留原异常**。第一组能看到 `cause`，
是因为代码里写了 `new RuntimeException("advice-thrown", ex)`；第二组不传，`cause` 就是 null、
`suppressed` 是 0。对比 W04 的 `whenComplete`，那里框架至少塞进了 `suppressed`。

#### 7. `@Order`：谁在外层

| 配置 | 进入 | 退出 |
| :--- | :--- | :--- |
| `alpha @Order(1)` + `beta @Order(2)` | `[alpha, beta]` | `[beta, alpha]` |
| 只对调两个数字（注册顺序不变） | `[beta, alpha]` | `[alpha, beta]` |
| 都不写，先注册 `zzz` 后 `aaa` | `[zzz, aaa]` | `[aaa, zzz]` |
| 都不写，先注册 `aaa` 后 `zzz` | `[aaa, zzz]` | `[zzz, aaa]` |

- `@Order` 数字小的在**外层**（先进后出）；
- 对调数字，进出顺序整体翻转 → 决定权只在 `@Order`，跟注册顺序无关；
- 都不写时跟着**注册顺序**走。最后两行是专门加的第二组：`zzz` 先注册就先进，
  `aaa` 先注册就先进 —— 字母序 `aaa < zzz` 只能解释其中一组，所以可以排除「按名字排」。

源码：`AspectJPrecedenceComparator:84` 只在「order 相等 **且** 同一切面」时才特殊处理；
跨切面相等直接返回 0，靠 `List.sort` 的稳定性保住原序。

**`@Before` 与 `@After` 的顺序天生相反** —— 想让日志切面包住事务切面，就把日志的 order 写小。

#### 8. CGLIB：生成的到底是什么

| 问题 | 实测 |
| :--- | :--- |
| 生成的类 | `Calc$$EnhancerByCGLIB$$<hash>`，**父类就是 `Calc`**，只多实现 `Factory` |
| `proxy instanceof Calc` | `true`（和 JDK 代理「是兄弟」正好相反） |
| 谁进拦截器 | `add` 1 次；`sealedAdd`（final）**0 次**；`callSecret` 1 次（里面的 private 不算） |
| final 类 | `IllegalArgumentException: Cannot subclass final class java.lang.String` |
| Spring 侧 | `final` 方法前后 `before` 增量 **0** |
| 构造器次数 | Spring 的 CGLIB Bean：**1 次** |
| 三条调用路径 | 直接 0.4 ns / `Method.invoke` 20.6 ns / `MethodProxy.invoke` 7.5 ns / `invokeSuper` 16.5 ns / 走一遍代理 24.1 ns |

**构造器只跑一次是 Objenesis 的功劳**：`ObjenesisCglibAopProxy:57` 只 `createClass()` 生成类，
`:62` 用 `objenesis.newInstance()` 直接分配对象、绕过构造器；`:70-84` 是兜底路径 ——
万一 Objenesis 不灵就退回 `ctor.newInstance()`，那时字段初始化两次、构造器副作用执行两遍。

`MethodProxy` 比 `Method.invoke` 快，是因为它生成的是「直接调父类方法的字节码」，
绕开反射的类型检查；但再快也快不过直接调用。读这组数字要记住：
直接调用 0.4 ns 已经贴着 JIT 下限（方法体只有一次加法、循环被展开内联），
倍数相对这个下限算，业务里方法体一长就全被摊薄（W03 Day 4 手写 AQS 时同一条）。

#### 9. 这一周还差的

- **手写 mini AOP**（`ProxyFactory` + `MethodInterceptor` + `Advisor`，不用 `@Aspect`）——
  这是 `java-48week-plan.md` 第 6 周的硬要求，放 Day 3；
- W07 的接口：`@Transactional` 失效的三种形态（`this` 调用 / 方法不在接口里 / `final` 方法），
  Day 2 已经把三条路都各留了一份实测。

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
| Day 2：`execution(* …day2..*.*(..))` 一跑，容器直接 `BeanCurrentlyInCreationException` | `..` 是「零个或多个子包」，把 `@Configuration` 类自己的 `@Bean` 工厂方法也匹配进去了 | 先怀疑是循环依赖，看异常链 5 层都是「工厂方法失败」；再看栈里 `BrokenCfg$$EnhancerBySpringCGLIB.shop(<generated>)` 才反应过来配置类本身是代理 | 拆成 `WidePointcutTrap` 单独取证；修法是把范围收到业务包 `…day2.shop..*` | `@Configuration` 为了 `@Bean` 返回单例会被 CGLIB 增强，所以它的工厂方法也在切点射程内；`@Aspect` 则因为 `isInfrastructureClass` 豁免而不被代理 |
| Day 2：`import net.sf.cglib.proxy.Enhancer` 一次 16 个「找不到符号」 | Spring 5 把 CGLIB 重打包进 `spring-core`，包名是 `org.springframework.cglib` | 报错集中在三行 import；`lib/` 里也确实没有 cglib 的独立 jar | 改 import，不额外下 jar | 网上教程大多还写 `net.sf.cglib`，那是 Spring 3 时代或独立使用 CGLIB 的写法 |
| Day 2：测「final 类不能代理」时抛的是 NPE 而不是 `IllegalArgumentException` | `Enhancer` 没设 callback，`create()` 先撞上 `callbacks is null` | NPE 文案是 `Cannot read the array length because "callbacks" is null`，和「final」毫无关系 | 补 `setCallback(NoOp.INSTANCE)`，才拿到 `Cannot subclass final class java.lang.String` | 异常的**出现顺序**也会骗人：前面的校验先炸，把真正的病因盖住 |
| Day 2：想抹掉类名里的 `$$<hash>` 做去重，抛 `IllegalArgumentException: Illegal group reference` | `replaceAll` 的**替换串**里 `$` 是组引用 | 异常直接指到 `replaceAll` 那一行 | 用 `Matcher.quoteReplacement(...)` 包一层 | `replaceAll` 的 pattern 要转义 `$`，replacement 也要 —— 两头规则不同，只处理一头照样炸 |
| Day 2：文案写「通知里抛新异常，原异常被塞进 cause」 | 那是我自己在代码里 `new RuntimeException("advice-thrown", ex)` 传的，不是框架行为 | 加了一组不传 `ex` 的对照，实测 `cause = null`、`suppressed = 0` | 结论改成「原异常彻底丢失」，并注明对比 W04 `whenComplete` 的 suppressed | `AspectJAfterThrowingAdvice:70` 只是 `throw ex`，通知抛的新异常直接把它顶掉，没有任何挂载动作 |
| Day 2：文案写「都不写 `@Order` 时按切面名排序」 | 第一版只注册了 `zzz → aaa`，看到 `zzz` 在前就以为符合「字母序」 | 补了反向注册（`aaa → zzz`），顺序跟着翻 → 排除字母序 | 结论改成「按注册顺序」，并对上 `AspectJPrecedenceComparator:84` 的相等返回 0 | 单一组数据往往同时符合两种解释；**要证伪一个假设，先设计一组能让两个假设给出不同结果的实验** |

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
- 「`execution` 和 `within` 什么区别？」——`execution` 匹配「方法签名」（含返回值、类名、方法名、参数），
  `within` 只匹配「方法声明在哪个类/包里」。
  追问：「`within(接口)` 行不行？」——不行，命中 0，而且不报错。实测 6 个方法一个都没拦到。
- 「`args()` 和 `execution` 的参数匹配有什么不同？」——`args` 看**运行时实参类型**，
  所以声明成 `Object` 的方法传 `String` 也会命中；`execution` 看**声明类型**。
  追问：「`args` 有什么代价？」——带 `args()` 的切点变成运行时切点（`isRuntime()`），每次调用重算，没有缓存。
- 「切点写宽了会怎样？」——轻则多拦别的类（`within(shop..*)` 里 6 个变 8 个，同包的 `Cart` 也进来了），
  重则把 `@Configuration` 的 `@Bean` 方法一起拦掉，**容器直接起不来**（`BeanCurrentlyInCreationException`）。
- 「`@AfterReturning` 能改返回值吗？」——不能。返回值只是被当参数传给一个 `void` 方法，没有回写通道。
  唯一「看起来能改」的情况是改了**可变对象**的内部（`StringBuilder.append`），那是副作用不是改返回值。
- 「`@AfterReturning` 配了但没执行，可能是什么原因？」——`returning` 声明的类型接不住返回值，
  源码 `shouldInvokeOnReturnValueOf` 直接跳过，**不报错**。写 `Object` 就一定执行。
- 「`@AfterThrowing` 能吞掉异常吗？」——不能。源码 `AspectJAfterThrowingAdvice:62-72` 就是
  try/catch + `throw ex`。要吞只能 `@Around` 里 catch 住 `proceed()`。
  追问：「那在通知里抛新异常呢？」——顶掉原异常，而且**框架不会帮你挂 cause**，原异常彻底丢失。
- 「两个切面谁先执行？」——看 `@Order`，数字小的在外层（先进后出）。
  追问：「都不写呢？」——同为 `LOWEST_PRECEDENCE`，顺序退化成「切面 Bean 的注册顺序」，
  因为比较器返回 0、排序稳定性保住原序。**别赌这个。**
- 「CGLIB 为什么不能代理 final 类 / final 方法？」——它就是生成子类覆写方法，
  final 类不能继承、final 方法不能覆写。报错原文 `Cannot subclass final class`；
  final 方法更糟，**没有任何报错**。
- 「Spring 用 CGLIB 时，目标对象的构造器会执行两次吗？」——不会。`ObjenesisCglibAopProxy`
  用 Objenesis 直接分配代理实例、不跑构造器（`:62`）；只有 Objenesis 失败才退回 `ctor.newInstance()`（`:70-84`）。
- 「`MethodProxy` 比反射快多少？」——本机实测 7.5 ns vs 20.6 ns（约 2.7 倍），
  但都远慢于直接调用（0.4 ns）。差别来自它生成的是直接调父类方法的字节码，省掉反射的类型检查。

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 11-26 | 四 | W06 Day 1：代理什么时候套上去 —— 手写 JDK 代理 / 代理身份与选型 / `this` 与 `self` | 65 min | `JdkProxy` + `ProxyIdentity` + `SelfInvocation` + `day1-result.txt`（189 行）+ `day1-spring-source.txt`（8 段） | ✅ |
| 11-27 | 五 | W06 Day 2：切点与通知全谱 —— 八种切点矩阵 / 写宽炸容器 / `@AfterReturning` 与 `@AfterThrowing` / `@Order` / CGLIB 补课 | 70 min | `PointcutForms` + `WidePointcutTrap` + `AdviceTypes` + `AspectOrder` + `CglibProbe` + `day2-result.txt`（288 行）+ `day2-spring-source.txt`（6 段） | ✅ |

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

**Q4 · 切点写错的两个方向，各自什么现象、怎么自查？**

切点由「在哪」和「什么方法」两个轴组成，两个方向都会静默出错。

**写窄**（最典型：`within(接口)`）：

- 现象：一条通知都不执行，日志干净得像没问题；
- 原因：`within` 问的是「方法**声明**在哪个类里」，接口只是对外的写法清单；
- 实测：`within(Buyer)` 命中 **0**，而 `within(Shop)` 命中 **6**；
- 自查：把切点临时换成 `within(目标类)` 或 `execution(* 目标类.*(..))`，如果有命中，
  说明原表达式写窄了。

**写宽**：

- 现象 A：多拦了不想拦的类。`within(shop..*)` 从 6 个变 **8** 个，同包的 `Cart` 也进来了；
- 现象 B（更凶）：拦到了容器自己。`execution(* com…day2..*.*(..))` 把 `@Configuration` 类的
  `@Bean` 工厂方法也匹配了 —— 因为 `..` 是「零个或多个子包」，而配置类为了返回单例本身是 CGLIB 代理。
  于是「造 shop → 要 wideAspect → wideAspect 正在造」→ `BeanCurrentlyInCreationException`，**容器起不来**；
- 报错会骗人：文案写的是 `Is there an unresolvable circular reference?`，但依赖关系一点都没绕；
  真正的线索是栈里那行 `配置类$$EnhancerBySpringCGLIB.shop(<generated>)`；
- 自查：看异常链里每一层的「失败的是哪个 Bean」，出现 `@Configuration` 类名或 `@Bean` 方法名就是它。

顺带一条省事的经验：`args()` 会让切点变成**运行时切点**（`isRuntime()` 为 true），每次调用重算；
`execution` / `within` / `@annotation` 是静态切点，命中结果进 `shadowMatchCache`，算一次就复用。

**Q5 · 五种通知各自的能力边界，为什么？**

先记一句：**只有 `@Around` 有 `proceed()`。**

| 通知 | 能改返回值 | 能吞异常 | 为什么 |
| :--- | :--: | :--: | :--- |
| `@Before` | ✗ | ✗ | 在调用前执行，还没返回值、也还没有异常 |
| `@AfterReturning` | ✗ | ✗ | 返回值只被当参数传给一个 `void` 方法，没有回写通道 |
| `@AfterThrowing` | ✗ | ✗ | 源码就是 `catch (Throwable ex) { …; throw ex; }`，异常必须继续抛 |
| `@After` | ✗ | ✗ | 相当于 `finally`，语义上就不该改变结果 |
| `@Around` | ✓ | ✓ | 它拿到 `ProceedingJoinPoint`，可以决定调不调、改返回值、catch 掉 |

两个最容易错的点：

1. **`@AfterReturning` 看起来能改，是因为对象可变。** `ret.append(...)` 生效、
   `ret = new StringBuilder(...)` 不生效 —— 前者改的是共用对象的内部，后者只改了局部引用。
   所以对 `String` / `int` 这种「换值」就等于没改。
2. **类型接不住就静默跳过。** `returning` 用 `isAssignableValue(声明类型, 实际值)`、
   `throwing` 用 `isAssignableFrom` 过滤，不匹配时**既不报错也不执行**。
   所以「异常日志有时有有时没有」，第一件事就是看 `throwing` 写的是不是 `Throwable`。

再补一条反直觉的：在 `@AfterThrowing` 里抛新异常时，**原异常会彻底丢失**
（实测 `cause = null`、`suppressed = 0`）。想留证据必须自己写
`throw new RuntimeException("…", ex)` —— 框架不会帮你挂。
