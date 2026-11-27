---
title: 今天做什么
date: 2026-11-27
weekday: 周五
milestone: M2 · Spring 内核
task: W06 Day 2 — 切点与通知的全谱（+ 补 CGLIB）
status: 已完成
---

# 今天 · 2026-11-27（周五）· 已完成

> Day 1 回答了「代理什么时候套上去」，但只用了 `within()` 一个切点、三种通知。
> 今天把剩下的补齐 —— 因为真正难查的不是「切面写错」，是**切点写得太宽 / 太窄**：
> 写宽了给所有方法开事务，写窄了该拦的没拦住，两种都安静得像没问题。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w06（7 文件）+ 跑 Day 1 三个入口 | ✅ 全 exit=0（58 / 72 / 47 行），无回归、无中断代码 |
| `01-notes` 里的未勾项 | 只剩 W06 第三格 `CGLIB`（Day 1 明确写了「留 Day 2」）→ **今天补掉** |
| `02-code` 空目录 | 无；`w07-` / `w08-` 仍只有 `.gitkeep`，属未解锁里程碑，不动 |
| 上一个时间盒留的三个待办 | ① 四种切点对照 ✅；② `@AfterReturning` / `@AfterThrowing` ✅；③ `@Order` ✅ |
| 与上一个时间盒的关系 | 11-26 → 11-27 隔一天（正常节奏），不涉及补课 |

**结论：唯一要补的遗留就是 W06 第三格 `CGLIB`**，已并入今天的第五段实验。

## 二、今天做的五件事

| # | 做什么 | 产出 |
| -: | :--- | :--- |
| 1 | 八种切点各配一个 `@Before`，10 次调用打一张命中矩阵 | `day2/PointcutForms.java` + `PointcutMatrix` |
| 2 | 把「切点写宽」推到极端：宽切点把 `@Configuration` 也拦了 | `day2/WidePointcutTrap.java` |
| 3 | `@AfterReturning` 改返回值 / `@AfterThrowing` 吞异常 / 类型不匹配静默跳过 | `day2/AdviceTypes.java` |
| 4 | 两个切面的 `@Order`，正反各一组 + 不写 `@Order` 的两组 | `day2/AspectOrder.java` |
| 5 | **补 CGLIB**：`Enhancer` / final 类 / `MethodProxy` 耗时 / Objenesis | `day2/CglibProbe.java` |

快照与取证：`day2-result.txt`（288 行，5 个入口全 exit=0）+ `day2-spring-source.txt`（6 段源码，行号与输出对撞）。

## 三、今天挖到的五个点

1. **`within(接口)` 是「写窄了」的标准形态，命中 0 且不报错。**
   `within` 问的是「方法声明在哪个类里」，不是「对象实现了谁」。实测 `within(Shop)` 6 个、`within(Buyer)` 0 个。

2. **`execution` 写宽到 `day2..*`，容器直接起不来。**
   `..` = 「零个或多个子包」，把 `@Configuration` 的 `@Bean` 工厂方法也匹配了。
   异常链 5 层都指向 `BeanCurrentlyInCreationException`，文案说 circular reference，
   但**依赖关系一点没绕，绕的是创建顺序**。线索只有栈里那行 `BrokenCfg$$EnhancerBySpringCGLIB.shop(<generated>)`。
   顺带查到 `@Aspect` 自己反而不受影响 —— `isInfrastructureClass` 用 `isAspect()` 豁免了它。

3. **`@AfterReturning` / `@AfterThrowing` 都改不了结果。**
   实测 `ret.append()` 生效、`ret = new StringBuilder()` 不生效（局部引用 vs 共用对象）；
   异常照抛，源码 `AspectJAfterThrowingAdvice:70` 就是 `throw ex`。
   只有 `@Around` 有 `proceed()`。

4. **类型接不住 = 静默不执行，两处机制完全一样。**
   `returning` 用 `isAssignableValue` 判值、`throwing` 用 `isAssignableFrom` 判类型，
   不匹配就跳过，启动不报错、调用不报错。这是「异常日志有时有有时没有」的常见原因。

5. **`@Order` 数字小的在外层；不写就退化成注册顺序。**
   对调数字进出顺序整体翻转；不写时按注册顺序（用两组反向注册证伪了「按名字排序」）。
   源码 `AspectJPrecedenceComparator:84` 只对「同切面」做特殊处理，跨切面相等直接返回 0。

## 四、今天踩的坑（五个，四个是「先写后验」）

- **宽切点炸容器，第一反应去查循环依赖。** 异常文案带 `circular reference`，
  但五层异常全是「工厂方法失败」。看到 `$$EnhancerBySpringCGLIB.shop(<generated>)` 才反应过来是配置类。
- **`import net.sf.cglib` 一次 16 个「找不到符号」。** Spring 5 把 CGLIB 重打包进了 `spring-core`，
  包名是 `org.springframework.cglib`，`lib/` 里也没有独立 cglib jar。
- **测「final 类」时忘了 `setCallback`，先撞 NPE。** `callbacks is null` 把真正的原因盖住了。
- **`replaceAll` 的替换串里 `$` 是组引用。** 想抹掉 `$$EnhancerBySpringCGLIB$$<hash>` 做去重，
  抛 `Illegal group reference`，要 `Matcher.quoteReplacement`。
- **两处文案写反了，都靠实跑抓回来**：
  ① 「通知抛新异常会把原异常塞进 cause」——那是我自己传的；加了一组不传的对照，
  实测 `cause = null`、`suppressed = 0`，原异常彻底丢失。
  ② 「不写 `@Order` 时按切面名排序」——补了反向注册才排除字母序，确认是注册顺序。

## 五、环境备忘

- 跑今天：`./run.sh PointcutForms` / `WidePointcutTrap` / `AdviceTypes` / `AspectOrder` / `CglibProbe`
  （短名现在会自动在 `day1`/`day2` 里找，脚本按 class 文件是否存在来定位）。
- **无新增 jar**。CGLIB 在 `spring-core` 里，`aspectjweaver` 已在。
- 源码取证：`/tmp/w06src` 下解压了 `spring-aop` 与 `spring-core` 的 `-sources.jar`（各 http=200），
  6 段原文已抄进 `day2-spring-source.txt`。
- 跑之前别忘了 `export PATH="/c/Program Files/Java/jdk-17/bin:$PATH"`。

---

# 下一个时间盒（60 分钟）

**W06 Day 3：手写 mini AOP —— 不用 `@Aspect`，把增强织进去**

> 这是 `java-48week-plan.md` 第 6 周的硬要求。前两天都在用 `@EnableAspectJAutoProxy`，
> 所有魔法都在注解后面。今天把注解拿掉，只用 `ProxyFactory` / `MethodInterceptor` / `Advisor`
> 这三样，看同一个「增强」还能不能织进去 —— 织得进去，就说明 `@Aspect` 只是语法糖；
> 织不进去的地方，才是它真正替我们做的事。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 2：写窄/写宽各什么现象、哪些通知能改结果、`@Order` 谁在外层 | 口头过 |
| 20 分钟 | **`ProxyFactory` 手工织入**：`addAdvice`（`MethodInterceptor`）→ 调一次看计数；换成 `addAdvisor`（`PointcutAdvisor`）看「有条件的增强」 | `day3/MiniAop.java` |
| 15 分钟 | **手写一个 `PointcutAdvisor` + `MethodMatcher`**：自己决定哪些方法被拦，不用 `execution` 表达式 | `day3/OwnAdvisor.java` |
| 10 分钟 | 对照：`ProxyFactory` 的 `setProxyTargetClass(true/false)` 与 `@EnableAspectJAutoProxy(proxyTargetClass)` 是不是一回事 | `day3/FactoryVsAspect.java` |
| 5 分钟 | 快照 + 笔记 + `today.md` 覆盖 | 三份文件 |

> 开工先做：确认 `spring-aop` 里的 `ProxyFactory` / `NameMatchMethodPointcut` / `DefaultPointcutAdvisor`
> 都在（都在 `org.springframework.aop.framework` 与 `org.springframework.aop.support`，不需要新 jar）。
> 注意 `ProxyFactory` 造出来的是**裸代理**，不经过容器 —— 这一点正好能反证 Day 1 的结论：
> 代理不需要容器来造，容器只是「替你决定谁该被包」。
> Day 4 是收尾封板：把四天的结论写成断言（照 W05 的 `BoardCheck` 那套），
> 并把 `@Transactional` 失效的三种形态各自对上一份 AOP 证据。
