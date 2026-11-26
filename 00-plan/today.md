---
title: 今天做什么
date: 2026-11-26
weekday: 周四
milestone: M2 · Spring 内核
task: W06 Day 1 — AOP：代理到底是什么时候套上去的
status: 已完成
---

# 今天 · 2026-11-26（周四）· 已完成

> W05 停在「Bean 造完、字段灌完」。今天回答下一个问题：
> **方法被调用的时候，Spring 还能插进来做什么？**
>
> 答案不是「切面编程」，是**容器里放的不是你的对象，是一个代理**。
> 所以今天不写博客式的 `@Aspect` demo，只做一件事：把「谁拿到了代理」这件事量出来。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w05（49 文件 / 107 class）+ 跑全部 11 个入口 | ✅ 全 exit=0，`day5.BoardCheck` **23/23** 通过，无回归、无中断代码 |
| `01-notes` 里的未勾项 | 只剩 W06 的四个知识点（= 今天要动的）+ W07 起未解锁内容 |
| `02-code` 空目录 | 无空目录；`w06-` 起仍是 `.gitkeep`，今天从 `.gitkeep` 建 `w06-spring-aop/` |
| 上一个时间盒留的待办 | ① `w06-spring-aop/` 建工程 ✅；② 确认 `aspectjweaver` 能下 ✅（2,087,074 B）；③ `pom.xml` 验证 ❌（本机仍无 Maven，且 W06 先不写 pom） |
| 与前一个时间盒的关系 | 11-25 → 11-26 隔一天（正常节奏），不涉及补课 |

**结论：没有需要补的中断代码。** 起点就是 W06 Day 1。

## 二、今天做的三件事

| # | 做什么 | 产出 |
| -: | :--- | :--- |
| 1 | **纯 JDK 手写动态代理**，八段（A 不代理 → H 非 public 接口），数 handler 进几次 | `day1/JdkProxy.java` |
| 2 | **同一套类交给 Spring**：代理身份 / 选型 / 自调用，七段 + 六段 | `day1/ProxyIdentity.java`、`day1/SelfInvocation.java` |
| 3 | 快照 + 源码取证（8 段，行号与栈对撞）+ 笔记 + 题库 + 三处同步 | `day1-result.txt`（189 行）、`day1-spring-source.txt`、W06 笔记、`Spring.md` 10 → 12 |

工程从 `.gitkeep` 建起，`lib/` 沿用 W05 的 7 个 jar，**新增 `aspectjweaver-1.9.7.jar`**（2.0 MB）：
`@Aspect` / `@Pointcut` 的注解和切点解析都在它里面，只放 `spring-aop` 会**编译不过**（不是运行才炸）。

## 三、今天挖到的四个点

1. **`this.xxx()` 不需要 Spring 就能证明。** 纯 JDK 代理里，`placeThenCancel()` 内部调 `place()` 和 `cancel()`：
   **业务跑了 3 个方法，handler 只进 1 次**。那两次是 `invokevirtual` 直连真身。
   这一条比任何「代理模式」的定义都有说服力，也是 W07 讲事务失效的地基。

2. **Spring 的 JDK 代理就是你手写的那一句。** `JdkDynamicAopProxy:126`
   `return Proxy.newProxyInstance(determineClassLoader(classLoader), this.proxiedInterfaces, this);`
   第三个参数是 `this`，因为它自己就是 `InvocationHandler`。我们把「计数器」换成了「顾问链」。

3. **`self` 是三层代理，而这件事只能靠 `TargetSource` 分清。**
   `self.getClass() == bean.getClass()` 是 `true`（JDK 代理类按接口列表缓存），完全不能当证据；
   真正的证据是 `self == bean` 为 `false` + `self` 的 `TargetSource` 是
   `ContextAnnotationAutowireCandidateResolver$1` + `self.getTarget() == bean` 为 `true`。
   链路：`@Lazy` 解析代理 → AOP 代理 → `GreetingImpl`。

4. **`final` 方法上的切面是「静默失效」。** `PlainJob.sealed()` 调用前后 `before` 增量 **0**，
   启动日志一个字都不提。这类「配了像没配」比抛异常难查十倍。

## 四、今天踩的坑（三个，都是「先写后验」）

- **「按实现类取 Bean 抛 `BeanNotOfRequiredTypeException`」只对了一半。**
  实测 `getBean(GreetingImpl.class)` 抛的是 `NoSuchBeanDefinitionException`（按类型匹配，连候选都不是），
  只有 `getBean("greeting", GreetingImpl.class)` 才是 `BeanNotOfRequiredTypeException`。
  两个错混为一谈，面试里会被追到崩。
- **`AopUtils` 不在 `aop.framework`，在 `aop.support`。** 凭印象写 import，一次 11 个编译错。
- **字符串里的 `\$` 是非法转义。** 在 shell 里写 `\$` 防变量替换，抄进 Java 源码就成了语法错。

## 五、环境备忘

- 跑今天：`./run.sh JdkProxy` / `./run.sh ProxyIdentity` / `./run.sh SelfInvocation`（各约 0.5 s）。
- 新增 jar：`lib/aspectjweaver-1.9.7.jar`（`curl` 自 repo1.maven.org，http=200）。
- 源码取证：`/tmp` 下解压了 `spring-aop-5.3.39-sources.jar` 与 JDK 的 `src.zip`，
  8 段原文已抄进 `day1-spring-source.txt`，`/tmp` 清了也不影响。
- 跑之前别忘了 `export PATH="/c/Program Files/Java/jdk-17/bin:$PATH"`。

---

# 下一个时间盒（60 分钟）

**W06 Day 2：切点与通知的全谱 —— `execution` 到底怎么匹配，`@AfterThrowing` 能不能吞异常**

> Day 1 只用了 `within()` 一个切点、`@Before/@Around/@After` 三种通知。
> Day 2 把剩下的补齐 —— 因为真正难查的不是「切面写错」，是**切点写得太宽/太窄**：
> 写宽了给所有方法开事务，写窄了该拦的没拦住，两种都安静得像没问题。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 1：代理在哪一步套上（`wrapIfNecessary:328`）/ JDK 与 CGLIB 的分界（`:58`）/ `this` 为什么失效 | 口头过 |
| 20 分钟 | **四种切点写法对照**：`execution` / `within` / `args` / `@annotation`，同一批方法各打一次，看谁命中谁不命中（含 `execution(* com..*.*(..))` 写宽了的后果） | `day2/PointcutForms.java` |
| 15 分钟 | **`@AfterReturning` 改返回值** + **`@AfterThrowing` 吞异常**：分别试「改得动吗」「吞得掉吗」 | `day2/AdviceTypes.java` |
| 10 分钟 | **两个切面的 `@Order`**：同一个方法上两个切面，谁在外层；`@Order` 反过来会怎样 | `day2/AspectOrder.java` |
| 5 分钟 | 快照 + 笔记 + `today.md` 覆盖 | 三份文件 |

> 开工先做：无新增 jar（`aspectjweaver` 已在）。切点表达式里如果要用 `args()` 绑定参数，
> 注意它匹配的是**运行时实参类型**，和 `execution` 的声明类型不是一回事 —— 这一点要先跑再写。
> 下一步 Day 3 是**手写 mini AOP**（`ProxyFactory` + `MethodInterceptor` + `Advisor`，
> 不用 `@Aspect`），那是 `java-48week-plan.md` 第 6 周的硬要求。
