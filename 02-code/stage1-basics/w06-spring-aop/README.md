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
| `spring-core` | 1,490,485 B | 基础设施 + 自带 CGLIB（`@Configuration` 与 `proxyTargetClass=true` 都要它） |
| `spring-beans` | 706,684 B | `@Bean` / `@Lazy` / 后处理器注册 |
| `spring-context` | 1,276,093 B | `AnnotationConfigApplicationContext` / `@EnableAspectJAutoProxy` / `ContextAnnotationAutowireCandidateResolver` |
| `spring-expression` | 294,249 B | SpEL（context 的依赖） |
| `spring-aop` | 385,322 B | `DefaultAopProxyFactory` / `JdkDynamicAopProxy` / `AopContext` / `AbstractAutoProxyCreator` |
| `spring-jcl` | 25,161 B | Spring 5 自带的日志桥（**不是** commons-logging） |
| `javax.annotation-api` | 26,586 B | 沿用 W05：`@PostConstruct` 需要（JDK 11+ 已移出 JDK） |
| `aspectjweaver-1.9.7` | 2,087,074 B | **Day 1 新增**：`@Aspect` / `@Pointcut` / `execution()` 解析全靠它 |

```bash
./run.sh                    # 默认跑 ProxyIdentity
./run.sh JdkProxy           # 短名 → day1.JdkProxy
./run.sh ProxyIdentity
./run.sh SelfInvocation
```

`run.bat` 同款。classpath 是 `out;lib/*`（Windows 分隔符是 `;`，Git Bash 下也别改成 `:`）。

> **新依赖为什么必须单独下**：`spring-aop` 只带 `org.aopalliance` 那套「方法拦截」的骨架，
> `@Aspect` 注解本身在 `aspectjweaver` 里。少了它，`import org.aspectj.lang.annotation.Aspect` 直接编译不过；
> 只放了 `spring-aop` 而没放 `aspectjweaver` 但代码里用了 `@Aspect`，编译期就死，不存在"运行才炸"。

## 二、目录

```text
w06-spring-aop/
├── lib/                       8 个 jar（W05 的 7 个 + aspectjweaver）
├── run.sh / run.bat           javac -cp "lib/*" + java -cp "out;lib/*"
├── day1-result.txt            三段实验的完整实测（189 行）
├── day1-spring-source.txt     源码取证（8 段：选代理 / 那一句 newProxyInstance / invoke / AopContext / @Lazy / wrapIfNecessary / JDK 的 Proxy 缓存）
└── src/main/java/com/newt/java48/w06/day1/
    ├── Greeting.java          业务接口（含三个 outer*，专给自调用实验）
    ├── GreetingImpl.java      this / self / currentProxy 三种写法
    ├── PlainJob.java          没有接口的类 + 一个 final 方法
    ├── TraceAspect.java       唯一的切面（三计数器 + 事件流水 + getThis/getTarget）
    ├── JdkProxy.java          纯 JDK：手写 InvocationHandler，A~H 八段
    ├── ProxyIdentity.java     Spring：S1~S7 七段，代理身份与选型
    └── SelfInvocation.java    Spring：S1~S6 六段，this.xxx() 为什么不走代理
```

## 三、三段实验各自证明了什么

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

## 四、坑（这几条都是今天就踩到的）

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

## 五、待办（W06 继续）

- [x] Day 1 · 代理是什么时候套上去的（JDK 代理 / 代理身份 / 自调用）
- [ ] Day 2 · 切点与通知全谱：`execution` / `within` / `args` / `@annotation` 四种切点，
      `@AfterReturning`（改返回值）/ `@AfterThrowing`（吞异常），以及两个切面的 `@Order`
- [ ] Day 3 · **手写一个 mini AOP**：`ProxyFactory` + `MethodInterceptor` + `Advisor`，
      不用 `@Aspect` 也把增强织进去（`java-48week-plan.md` 第 6 周的硬要求）
- [ ] Day 4 · 收尾封板 + 接到 W07：`@Transactional` 失效的三种形态各有哪一条 AOP 原理兜底
- [ ] 仍然没装 Maven：W05 的 `pom.xml` 未验证，W06 也先不写 `pom.xml`
