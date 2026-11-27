# W06 · Spring AOP

> M2 的第二周。W05 停在「Bean 造完、字段灌完」，W06 接着回答下一个问题：
> **方法被调用的时候，Spring 还能插进来做什么？**
>
> 答案只有一句话：**它给你的不是你的类，是一个代理**。
> 所以这一周真正要搞清的不是 `@Aspect` 怎么写（半小时就会），而是
> **「代理在什么时候套上去的、谁拿到了代理、谁没拿到」** —— 因为 `@Transactional` / `@Async` 失效的全是这一类。

## 一、怎么跑（本机没有 Maven）

`lib/` 里 8 个 jar，Spring **5.3.39**：

| jar | 大小 | 作用 |
| :--- | ---: | :--- |
| `spring-core` | 1,490,485 B | 基础设施 + **自带 CGLIB**（`@Configuration`、`proxyTargetClass=true`、Day 2 手写 `Enhancer` 都靠它） |
| `spring-beans` | 706,684 B | `@Bean` / `@Lazy` / 后处理器注册 |
| `spring-context` | 1,276,093 B | `AnnotationConfigApplicationContext` / `@EnableAspectJAutoProxy` / `ContextAnnotationAutowireCandidateResolver` |
| `spring-expression` | 294,249 B | SpEL（context 的依赖） |
| `spring-aop` | 385,322 B | `DefaultAopProxyFactory` / `JdkDynamicAopProxy` / `AopContext` / `AbstractAutoProxyCreator` / `AspectJAfter*Advice` |
| `spring-jcl` | 25,161 B | Spring 5 自带的日志桥（**不是** commons-logging） |
| `javax.annotation-api` | 26,586 B | 沿用 W05：`@PostConstruct` 需要（JDK 11+ 已移出 JDK） |
| `aspectjweaver-1.9.7` | 2,087,074 B | **Day 1 新增**：`@Aspect` / `@Pointcut` / `execution()` 解析全靠它 |

> **Day 2 没有新增 jar** —— 因为 CGLIB 已经被 Spring 重打包进 `spring-core`。
> 所以 import 写的是 `org.springframework.cglib.proxy.Enhancer`，
> **不是**网上到处抄的 `net.sf.cglib.proxy.Enhancer`（写后者会一次报 16 个「找不到符号」）。
> `lib/` 里也没有独立的 `cglib-nodep.jar`，不需要下。

```bash
./run.sh                    # 默认跑 ProxyIdentity
./run.sh JdkProxy           # 短名 → 自动在 day1 / day2 里找
./run.sh PointcutForms      # 短名 → day2.PointcutForms
./run.sh day2.AspectOrder   # 全名
```

`run.bat` 同款。classpath 是 `out;lib/*`（Windows 分隔符是 `;`，Git Bash 下也别改成 `:`）。
短名解析是 Day 2 改的：按 `out/…/dayN/<名字>.class` 是否存在来定位，加新的一天不用再改脚本。

> **新依赖为什么必须单独下**：`spring-aop` 只带 `org.aopalliance` 那套「方法拦截」的骨架，
> `@Aspect` 注解本身在 `aspectjweaver` 里。少了它，`import org.aspectj.lang.annotation.Aspect` 直接编译不过；
> 只放了 `spring-aop` 而没放 `aspectjweaver` 但代码里用了 `@Aspect`，编译期就死，不存在"运行才炸"。

## 二、目录

```text
w06-spring-aop/
├── lib/                       8 个 jar（W05 的 7 个 + aspectjweaver；Day 2 没加新的）
├── run.sh / run.bat           javac -cp "lib/*" + java -cp "out;lib/*"，短名自动找 dayN
├── day1-result.txt            三段实验的完整实测（189 行）
├── day1-spring-source.txt     源码取证（8 段：选代理 / 那一句 newProxyInstance / invoke / AopContext / @Lazy / wrapIfNecessary / JDK 的 Proxy 缓存）
├── day2-result.txt            五段实验的完整实测（288 行，5 个入口全 exit=0）
├── day2-spring-source.txt     源码取证（6 段：AfterReturning / AfterThrowing / @Aspect 豁免 / @Order 比较器 / Objenesis / 切点缓存）
└── src/main/java/com/newt/java48/w06/
    ├── day1/
    │   ├── Greeting.java          业务接口（含三个 outer*，专给自调用实验）
    │   ├── GreetingImpl.java      this / self / currentProxy 三种写法
    │   ├── PlainJob.java          没有接口的类 + 一个 final 方法
    │   ├── TraceAspect.java       唯一的切面（三计数器 + 事件流水 + getThis/getTarget）
    │   ├── JdkProxy.java          纯 JDK：手写 InvocationHandler，A~H 八段
    │   ├── ProxyIdentity.java     Spring：S1~S7 七段，代理身份与选型
    │   └── SelfInvocation.java    Spring：S1~S6 六段，this.xxx() 为什么不走代理
    └── day2/
        ├── shop/                  靶子包：Buyer（接口）/ Shop（6 个方法）/ Cart（同包另一个类）/ Traced（注解）
        ├── stock/Warehouse.java   另一个子包 + 没有接口 → 自动 CGLIB
        ├── PointcutMatrix.java    8 种切点各一个 @Before，命中结果记成矩阵
        ├── PointcutForms.java     S1~S6：命中矩阵 / 写窄 / 写宽 / args vs execution / 目标类不是代理类
        ├── WidePointcutTrap.java  反面教材：宽切点把 @Configuration 自己的 @Bean 方法拦了，容器起不来
        ├── AdviceTypes.java       S1~S7：@AfterReturning 改得动吗 / @AfterThrowing 吞得掉吗
        ├── AspectOrder.java       S1~S4：@Order 谁在外层 / 不写 @Order 时按什么排
        └── CglibProbe.java        A~E：Enhancer 生成的子类 / final 类 / MethodProxy 耗时 / 构造器几次
```

## 三、Day 1 的三段实验

### 1. `JdkProxy` —— 把 AOP 的地基拆到只剩一句话

不引入 Spring 的任何东西，只用 `java.lang.reflect`：

```java
Proxy.newProxyInstance(loader, new Class<?>[]{Shop.class}, handler)
```

八段的数字：

| 段 | 做的事 | 结果 |
| :- | :--- | :--- |
| A | 直接调 `impl.place()` | handler 进 **0** 次，业务跑 1 次 |
| B | 代理调一次 | handler 进 **1** 次；类名 `jdk.proxy1.$Proxy0` |
| C | 代理里再调自己的另一个方法 | handler 仍 **1** 次，业务跑了 3 个方法 |
| D | 代理套代理 | **2** 次，两层各一次 |
| E | 同样接口再生成 | `p1` 与 `p2` **同一个类**；多实现一个 `Serializable` → 换成 `$Proxy1` |
| F | `hashCode/toString/equals` | 三个**都进** handler |
| G | 强转成实现类 | `ClassCastException` |
| H | 接口改成 package-private | 代理类名变成 `com.newt.java48.w06.day1.$Proxy4`（接口所在的包） |

C 段是整周最重要的一次观测：`placeThenCancel` 内部调 `place` 与 `cancel`，
**业务真的执行了 3 个方法，handler 只进 1 次**。原因是那两次是 `invokevirtual` 直连 impl，
与代理对象完全无关 —— 这就是 `this.xxx()` 不走代理的物理原因，不需要任何 Spring 知识就能证明。

E 段顺手拆掉一个误解：`$Proxy0` 是按「接口列表」缓存的（JDK `Proxy:309` 的 `proxyCache`，
427/438 行 `proxyCache.sub(intfs)`），**动态的是生成时机，不是每次调用都生成新类**。

### 2. `ProxyIdentity` —— 容器里那个对象到底是谁

| 段 | 问题 | 实测 |
| :- | :--- | :--- |
| S1 | 手工 `new` 的实例进切面吗 | 不进，`before=0` |
| S2 | 容器里放的是什么 | `jdk.proxy2.$Proxy22`，`isJdkDynamicProxy=true` |
| S2b | 按实现类取这个 Bean | 按类型 `NoSuchBeanDefinition`；按名字 `BeanNotOfRequiredType` |
| S3 | 一次调用各通知几次 | before/around/after = **1/1/1**；`getThis()` 是代理、`getTarget()` 是真身 |
| S4 | `proxyTargetClass=true` | `GreetingImpl$$EnhancerBySpringCGLIB$$…`，父类就是 `GreetingImpl` |
| S5 | 没有接口的类 | 自动 CGLIB；`run()` 被拦，`final sealed()` **静默不拦** |
| S6 | 切面 Bean 自己被代理吗 | 不，`isAopProxy=false` |
| S7 | 通知顺序 | `@Around 进入 → @Before → 目标 → @After → @Around 返回` |

S2 里那个 `getInterfaces()` 打印值得背下来：代理除了 `Greeting` 还实现了
`SpringProxy` / `Advised` / `DecoratingProxy` —— 前两个正是 `AopUtils.isJdkDynamicProxy`
的判据（`instanceof SpringProxy && Proxy.isProxyClass`），**不是看类名像不像**。
所以自己手写的 `$Proxy0` 不会被认成 AOP 代理。

S2b 是两个**不同的错**，这一点一开始被我写反了（见「踩坑」第 1 条）：

```text
getBean(GreetingImpl.class)            → NoSuchBeanDefinitionException  （按类型匹配，候选都不是）
getBean("greeting", GreetingImpl.class) → BeanNotOfRequiredTypeException （名字命中，实例类型不对）
```

S5 里 `sealed()` 那次调用前后 `before` 增量是 **0** —— CGLIB 靠生成子类覆写方法，
final 方法覆写不了，于是切面**完全不执行，启动日志一个字都不提**。这是"配了像没配"的典型。

### 3. `SelfInvocation` —— 三种写法，三个数字

| 写法 | 被拦的方法 | before |
| :--- | :--- | ---: |
| `this.hi()` | `[Greeting.outer]` | **1** |
| `self.hi()`（`@Lazy` 注入自己） | `[Greeting.outerViaSelf, Greeting.hi]` | **2** |
| `AopContext.currentProxy().hi()` | `[Greeting.outerViaAopContext, Greeting.hi]` | **2** |

业务结果三行完全一样（`outer->hi S1` / …），差别只在「有没有回到代理」。

`self` 那一格最容易讲错，所以 S4 专门加了 `TargetSource` 探针：

```text
self 类名                    = jdk.proxy2.$Proxy22      ← 和 bean 同一个类，不能当证据
self == 容器 bean            = false
self 的 TargetSource 类      = ContextAnnotationAutowireCandidateResolver$1
self 的 target == bean       = true
```

即真实链路是 **`@Lazy` 解析代理 → AOP 代理 → `GreetingImpl` 真身** 三层。
`$1` 就是 `buildLazyResolutionProxy:83` 那个匿名 `TargetSource`，它的 `getTarget()`
每次调用都回容器 `doResolveDependency` 一次（`:93-95`），所以拿到的是 AOP 代理而不是真身。

S5 打出的是 `AopContext.currentProxy()` 的原文：

```text
IllegalStateException: Cannot find current proxy: Set 'exposeProxy' property on Advised
to 'true' to make it available, and ensure that AopContext.currentProxy() is invoked
in the same thread as the AOP invocation context.
```

注意最后半句「same thread」：`AopContext` 就是一个 `NamedThreadLocal`（`AopContext:50`），
所以 `@Async` / 线程池里 `currentProxy()` 必然拿不到 —— 这条留到 W08 讲异步时回来看。

## 四、Day 2 的五段实验

Day 1 只用了 `within()` 一个切点、`@Before/@Around/@After` 三种通知。Day 2 把剩下的补齐 ——
因为真正难查的不是「切面写错」，是**切点写宽了/写窄了**、以及**通知的类型对不上**：
两种都安静得像没问题。

### 4. `PointcutForms` —— 八种切点，十次调用，一张矩阵

调用脚本固定 10 次：`buy(String)` / `buy((Object)"x")` / `buy(String,int)` / `count()` /
`tagged()` / `plain()` / `Cart#add` / `Cart#size` / `Warehouse#stock` / `toString()`。
八个 `@Before` 各记一份，结果是：

| 切点 | 命中 | 命中了谁 |
| :--- | ---: | :--- |
| `execution(* Shop.buy(String))` | 1 | 只有 `Shop#buy(String)` |
| `execution(* Shop.buy(Object))` | 1 | 只有 `Shop#buy(Object)` |
| `within(Shop)` | **6** | Shop 自己的 6 个方法，`Cart` / `Warehouse` 一个没有 |
| `within(shop..*)` | **8** | 多出 `Cart#add(String)` / `Cart#size()` |
| `within(Buyer)`（写成接口） | **0** | 一个都没拦到 |
| `args(String)` | 4 | 含 `Shop#buy(Object)` 与 `Warehouse#stock(String)` |
| `args(String,..)` | 5 | 再多个 `Shop#buy(String,int)` |
| `@annotation(Traced)` | 1 | 只有 `Shop#tagged()` |

四条能直接拿去面试的：

1. **`within(接口)` = 一条都不生效，而且不报错。** `within` 问的是「这个方法声明在哪个类里」，
   不是「这个对象实现了谁」。接口是对外的写法清单，不是代码定义的位置。
2. **`args(String)` 会命中声明成 `Object` 的方法**（`buy(Object)` 传了 `String`），
   也会命中**别的类**的同类方法（`Warehouse#stock(String)`）—— 它只看参数，不看方法属于谁。
   反过来 `buy(String,int)` 明明是 String 开头也不命中：参数个数要对上。
3. **`execution` 认声明类型、`args` 认运行时实参**，`buy(Object)` 那一行两边结论正好相反。
4. **切点是对着「目标类」算的，跟代理方式无关。** 换成默认 JDK 代理后
   `jdk.proxy2.$Proxy19` 里根本没有 `Shop` 这个名字，`within(Shop)` 照样命中
   （源码 `AspectJExpressionPointcut:455` 的 `getMostSpecificMethod(method, targetClass)`）。

S6 顺带挖到「事务失效的第二条路」：JDK 代理下 `Shop#plain()` 不在 `Buyer` 接口里，
代理类压根没这个方法 —— 强转 `(Shop) proxy` 直接 CCE，只能绕到真身上调，
两条路都不进切面。**方法没写在接口里，`@Transactional` 一样白加。**

### 5. `WidePointcutTrap` —— 切点写宽了，容器起不来

这一条本来是 `PointcutForms` 的一个格子，跑出来直接把容器炸了，所以单独拆出来当反面教材。

`execution(* com.newt.java48.w06.day2..*.*(..))` 里的 `..` 是「**零个或多个**子包」，
所以连 `day2` 包里的 `@Configuration` 类一起匹配。于是：

```text
container 调 shop()（走的是 BrokenCfg 的 CGLIB 代理）
  → 命中宽切点
    → 通知要 wideAspect 这个 Bean
      → wideAspect 正在创建中
        → BeanCurrentlyInCreationException: Requested bean is currently in creation
```

异常链是 5 层，从外到内：`BeanCreationException(shop)` → `BeanInstantiationException(Shop)`
→ `BeanCreationException(wideAspect)` → `BeanInstantiationException(WideAspect)`
→ `BeanCurrentlyInCreationException`。

**真正的线索只有一行栈**（报错文案说的是 circular reference，会把注意力引向依赖注入，
但这里的依赖关系一点都没绕）：

```text
at WidePointcutTrap$BrokenCfg$$EnhancerBySpringCGLIB$$<hash>.shop(<generated>)
at WidePointcutTrap$BrokenCfg$$EnhancerBySpringCGLIB$$<hash>.wideAspect(<generated>)
```

修法：把 `execution` 收到业务包 `com.newt.java48.w06.day2.shop..*` —— 配置类在 `day2`、
不在 `day2.shop`，天然出局，容器正常起来。

顺带查到一件事：**`@Aspect` 的 Bean 不会被自己的切点套住**，因为它被
`isInfrastructureClass` 当成基础设施豁免了（`AnnotationAwareAspectJAutoProxyCreator:110-111`）；
`@Configuration` 没有这层豁免，所以炸的是配置类。

### 6. `AdviceTypes` —— 改得动吗、吞得掉吗

| 段 | 试什么 | 实测 |
| :- | :--- | :--- |
| S1 | `@AfterReturning` 里 `ret = new StringBuilder("REPLACED")` | 调用方拿到 `hello-touched`：**换引用无效，改对象内部有效** |
| S2 | `returning` 声明 `String`，方法返回 `int` | 通知**从来没执行过**，启动不报错、调用不报错 |
| S3 | `@AfterThrowing` 想吞异常 | 通知收到 `IllegalArgumentException`，调用方**照样收到** |
| S4 | `throwing` 声明 `IllegalStateException`，抛的是 `IllegalArgumentException` | 静默跳过；换成 `IllegalStateException` 才被调用 |
| S5 | 通知里抛新异常 | 新异常顶掉原异常；**框架不会帮你挂 cause 也不会挂 suppressed** |
| S6 | `@Around` + try/catch | 唯一能吞的写法，调用方什么都不知道 |
| S7 | 四种通知的顺序 | `@Around 进入 → @Before → @AfterReturning → @After → @Around 拿到返回值 → @Around finally` |

S1 的坑值得单独说：**「能改返回值」是假象**。`ret` 是局部参数，重新赋值只改了那个引用；
但 `ret` 指向的对象是共用的，`append` 会生效。所以有人总结「`@AfterReturning` 能改返回值」——
那只对**可变对象**成立，对 `String` / `int` 这种换值就等于没改。

S5 的两组对照最能说明问题：第一组能看到 `cause`，是因为代码里写了
`new RuntimeException("advice-thrown", ex)`；第二组不传 `ex`，原异常**彻底丢失**，`suppressed` 实测 0。
对比 W04 的 `whenComplete`，那里框架至少把它塞进了 `suppressed`。

源码上的依据很直白 —— `AspectJAfterThrowingAdvice.invoke:62-72` 就是个 try/catch，
catch 里先判类型、再调通知、然后 **`:70 throw ex`**：异常从来没有被「取消」的机会。

### 7. `AspectOrder` —— 谁在外层

| 段 | 配置 | 进入顺序 | 退出顺序 |
| :- | :--- | :--- | :--- |
| S1 | `alpha @Order(1)` + `beta @Order(2)` | `[alpha, beta]` | `[beta, alpha]` |
| S2 | 只把两个数字对调（注册顺序不变） | `[beta, alpha]` | `[alpha, beta]` |
| S3 | 都不写 `@Order`，先注册 `zzz` 后 `aaa` | `[zzz, aaa]` | `[aaa, zzz]` |
| S3b | 都不写 `@Order`，先注册 `aaa` 后 `zzz` | `[aaa, zzz]` | `[zzz, aaa]` |

- **`@Order` 数字小的在洋葱外层**：先进后出。
- **对调数字，进出顺序整体翻转**，说明决定权只在 `@Order`，跟注册顺序无关。
- **都不写时跟着注册顺序走**。S3 与 S3b 的顺序随注册顺序翻转，而字母序 `aaa < zzz`
  只能解释其中一组，所以可以排除「按名字排」。
  源码依据：`AspectJPrecedenceComparator:84` 只在「order 相等 **且** 同一切面」时特殊处理，
  跨切面相等直接返回 0，靠 `List.sort` 的稳定性保住原序。
- 顺带记住：**`@Before` 与 `@After` 的顺序天生相反**。想让日志切面包住事务切面，就把日志的 order 写小。

### 8. `CglibProbe` —— CGLIB 生成的到底是什么

| 段 | 问题 | 实测 |
| :- | :--- | :--- |
| A | 生成的类长什么样 | `Calc$$EnhancerByCGLIB$$<hash>`，父类就是 `Calc`，只多实现 `Factory` |
| A2 | 谁进了拦截器 | `add` 1 次；`sealedAdd`（final）**0 次**；`callSecret` 1 次（里面的 private 不算） |
| B | final 类 | `IllegalArgumentException: Cannot subclass final class java.lang.String` |
| C | Spring 里同一件事 | 切面正常生效；`final sealedAdd()` 前后 `before` 增量 **0** |
| D | 三条调用路径 | 直接 0.4 ns / `Method.invoke` 20.6 ns（52x）/ `MethodProxy.invoke` 7.5 ns（19x）/ `invokeSuper` 16.5 ns（42x）/ 走一遍代理 24.1 ns（61x） |
| E | 构造器被调几次 | Spring 的 CGLIB Bean：**1 次** |

E 段是这一节最有用的：直接 `Enhancer.create()` 会跑一次构造器（那个代理实例本身要 `new` 出来），
那 Spring 为什么不重复初始化？**Objenesis**。`ObjenesisCglibAopProxy:57` 只
`enhancer.createClass()` 生成类，`:62` 用 `objenesis.newInstance()` 直接分配对象、绕过构造器；
`:70-84` 才是兜底路径 —— 万一 Objenesis 不灵，退回 `ctor.newInstance()`，
那时字段会初始化两次、构造器里的副作用执行两遍。

D 段读数字的规矩：直接调用 0.4 ns 已经贴着 JIT 下限（方法体只有一次加法、循环被展开内联），
「倍」是相对这个下限算的；真到业务里方法体一变长，这些倍数会被一起摊薄。

## 五、坑

### Day 1

1. **「按实现类取 Bean 抛 `BeanNotOfRequiredTypeException`」只对了一半。**
   先写文案后跑，实测按**类型**取抛的是 `NoSuchBeanDefinitionException`（连候选都算不上，
   因为 Bean 定义声明的类型是接口）；只有按**名字 + 类型**取才是 `BeanNotOfRequiredTypeException`。
   两个错混为一谈，面试里会被追问到崩。

2. **`AopUtils` 不在 `aop.framework` 里。**
   在 `org.springframework.aop.support.AopUtils`。凭印象写 import，一次编译 11 个错。

3. **`Advised.getTargetSource().getTarget()` 抛受检 `Exception`。**
   `main` 得写 `throws Exception`，否则 `unreported exception`。

4. **代理类名不能证明「层级」。**
   一开始用 `self.getClass() == bean.getClass()` 论证"双层代理"，实测 `true` ——
   但 JDK 代理类是按接口组合缓存的，两个不同实例完全可能共用一个 `$Proxy`。
   换成 `self == bean` 与 `TargetSource` 类名才分得清。

5. **`@After` 在 `@Around` 返回之前就跑完了。**
   原以为顺序是 `before → 目标 → after → around 退出`，实测 `@After` 落在 `@Around 返回` 之前 ——
   它就是 `proceed()` 外面那层 `try/finally` 里的 `finally`。

### Day 2

6. **`execution(* …day2..*.*(..))` 把容器自己的配置类拦了。**
   第一次跑直接 `BeanCurrentlyInCreationException`，异常文案说 `circular reference`，
   但依赖关系一点都没绕 —— 绕的是「创建顺序」。真正的线索是栈里那行
   `BrokenCfg$$EnhancerBySpringCGLIB.shop(<generated>)`：`@Configuration` 类本身就是 CGLIB 代理。
   拆成 `WidePointcutTrap` 单独取证。

7. **`import net.sf.cglib.proxy.Enhancer` 报 16 个「找不到符号」。**
   Spring 5 已把 CGLIB 重打包进 `spring-core`，包名是 `org.springframework.cglib`。
   `lib/` 里也没有独立的 cglib jar，不需要额外下载。

8. **`Enhancer.create()` 不给 callback 会先撞 NPE。**
   测「final 类不能代理」时忘了 `setCallback`，抛的是
   `NullPointerException: Cannot read the array length because "callbacks" is null`，
   把真正的原因（`IllegalArgumentException: Cannot subclass final class`）盖住了。

9. **`replaceAll` 的替换串里 `$` 要 `quoteReplacement`。**
   想把类名里的 `$$EnhancerBySpringCGLIB$$<hash>` 抹成固定串来去重，直接写
   `"$$EnhancerBySpringCGLIB$$<hash>"` 被当成组引用，抛 `IllegalArgumentException: Illegal group reference`。

10. **`@AfterThrowing` 里抛的新异常不会自动带上原异常。**
    先写的是「原异常被塞进 cause」——那是我自己在代码里传的。加了一组不传 `ex` 的对照，
    实测 `cause = null`、`suppressed = 0`，原异常彻底丢失。**断言先于实测，又一次。**

11. **「都不写 `@Order` 时按切面名排序」是编的。**
    第一版 S3 只注册了 `zzz → aaa`，看到 `zzz` 在前就以为是名字序（其实也符合「注册顺序」）。
    补了 S3b（`aaa → zzz`）才排除掉字母序，确认是**注册顺序**。

## 六、待办（W06 继续）

- [x] Day 1 · 代理是什么时候套上去的（JDK 代理 / 代理身份 / 自调用）
- [x] Day 2 · 切点与通知全谱：八种切点写法 + `@AfterReturning` / `@AfterThrowing` / `@Order`，
      外加补掉 Day 1 留的 CGLIB（`Enhancer` / `MethodProxy` / Objenesis）
- [ ] Day 3 · **手写一个 mini AOP**：`ProxyFactory` + `MethodInterceptor` + `Advisor`，
      不用 `@Aspect` 也把增强织进去（`java-48week-plan.md` 第 6 周的硬要求）
- [ ] Day 4 · 收尾封板 + 接到 W07：`@Transactional` 失效的三种形态各有哪一条 AOP 原理兜底
- [ ] 仍然没装 Maven：W05 的 `pom.xml` 未验证，W06 也先不写 `pom.xml`
