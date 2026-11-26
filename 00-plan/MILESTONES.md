---
title: Java 进阶 · 里程碑计划
subtitle: 不追周次，只追产出 · 8K → 25K
tags:
  - java
  - 学习计划
  - 里程碑
created: 2026-10-09
supersedes: java-48week-plan.md
daily: 60 分钟
---

# Java 进阶 · 里程碑计划

> **为什么改掉周次制**
>
> 原 48 周计划是「周次驱动」——计划表说你第 3 周该学并发，你第 5 周才学到，
> 于是每天都在「欠账」。断档一次，账就永远还不完，最后干脆不看计划表了。
>
> 真实情况：2026-09-22 跑了 Day 1，然后停了 17 天（忙别的）。
> **这不是落后几周，是周次制本身不适合你的节奏。**
>
> 改成「里程碑制」：
>
> - **不设第几周，只设"要做完什么"。**
> - 做完一个里程碑，才解锁下一个。做完就是做完，没有「晚了」。
> - 进度只有一种表达：**产出物拿到了没有**。

---

## 一、唯一的日常标准

每天 60 分钟，只要产出下面三样里的**任意两样**，今天就算数：

1. **一个知识点**（能用自己话讲出来，不是看完）
2. **一段代码**（能跑，哪怕 20 行）
3. **一份笔记回填**（写进当周笔记的对应小节）

> 三样是理想；两样是及格。**"今天只干了 30 分钟" 不是失败，"今天没打开 IDE" 才是。**

**断档规则（重要）**：允许断，但不允许"补"。断了一天，第二天从**当天该做的那一小步**接着走，
不去补昨天漏的。补课是周次制的病根，取消它。

---

## 二、里程碑总览

七个里程碑，从下往上解锁。**M1 是你现在唯一要关心的事。**

| 里程碑 | 一句话目标 | 解锁条件（硬标准） | 状态 |
| :--- | :--- | :--- | :--: |
| **M0 · 收尾** | 把 W01 剩余做完，找回手感 | 见下方「M0 待办」全部勾掉 | ✅ 已封板 2026-10-27 |
| **M1 · Java 地基** | 集合 / 泛型 / 并发 / 线程池，能讲原理 | 4 份笔记 + 4 个可运行工程 + 8 道面试题进题库 | ✅ 已封板 2026-11-20 |
| **M2 · Spring 内核** | 事务 / AOP / 异步，能解释失效场景 | 4 份笔记 + 4 个可运行 Demo + 6 道面试题 | 🟨 进行中 |
| **M3 · 数据层** | MySQL 索引 / EXPLAIN / 锁 + Redis 基础 | 3 份笔记 + 10 条 EXPLAIN 案例 + Redis Demo | ⬜ |
| **M4 · 线上能力** | JVM 排查 / MQ / Linux / Docker | JVM 排查实验报告 + MQ 任务系统 + Docker 化跑通 | ⬜ |
| **M5 · 系统设计** | 微服务 / 高并发 / 性能优化 | 项目微服务化 + 架构图 + 技术文档 | ⬜ |
| **M6 · 面试冲刺** | 项目包装 + 题库 + 简历 | 5 大难题 + 全题库脱稿 + 简历终稿 | ⬜ |

> 原来的 48 周 → 现在**不折算周数**。按你每天 60 分钟、每周实际能跑 4–5 天的节奏，
> M0 一周内、M1 一个月上下、M2 一个月上下，大致是这个量级。
> **但不要在计划表上写死日期** —— 写了就又变成周次制了。

---

## 三、M0 · 收尾（当前唯一焦点）

目标：**把 W01 从「Day 1 完成」推到「W01 可以封板」**，用最小成本。

W01 原来的 7 天任务里，有性价比高低之分。重新排序如下：

| # | 任务 | 原计划位置 | 现在怎么办 | 产出 |
| :-- | :--- | :--- | :--- | :-- |
| 1 | HashMap 结构 + put 全流程（含源码） | Day 2 / Day 3 | ✅ 已完成 10-09 | 笔记「知识点」新增一节 + 小实验 |
| 2 | 手写 MyHashMap | Day 6 | ✅ 已完成 10-27（`put/get/resize` 三段 + 与 JDK 20 万次对拍） | `02-code/.../day6/MyHashMap.java` + `day6-result.txt` |
| 3 | HashMap 扩容观察实验 | Day 3 | ✅ 合并进 #1 | 打印扩容现场 |
| 4 | 集合面试题整理 | Day 5 | ✅ 已完成 10-27，共 12 题，题题有「我的回答 + 追问预判 + 项目案例」 | `04-interview/question-bank/Java基础.md` |
| 5 | HashSet / LinkedHashMap | Day 4 | ✅ 已完成 10-10（完整一节 + 10 个实验） | 笔记「Day 3」一节 + `day3-result.txt` |
| 6 | 全周总结 / 复述 | Day 7 | ✅ 已完成 10-27 | 笔记「六、一句话总结」封板版 + 打卡补完 |

**M0 完成定义**：笔记 W01 的「本周知识点」6 个勾全勾上、「每日打卡」7 行有内容、
`Java基础.md` 至少 3 道集合题写完、`MyHashMap` 能跑出结果。做到这些，W01 封板，直接进 M1。

> ✅ **2026-10-27 封板**：6 个知识点全勾、打卡 8 行、题库 12 题全部写完、`MyHashMap` 对拍 20 万次
> 0 处不一致。这一版比原定义多做两件事：题库从 3 道加到 12 道；手写版额外做了「树化退化」对照实验
> （hash 全等且 key 不可比较时，JDK 反而比手写链表慢 6.7 倍）。
>
> 断档记录：09-22 → 10-09（17 天）、10-10 → 10-27（17 天）。两次都按里程碑制「不补课、接着走」
> 处理，没有产生欠账感 —— 这正是改制的目的。

---

## 四、M1 · Java 地基（W02–W04 内容）

| 主题 | 笔记 | 代码 | 面试题库 |
| :--- | :--- | :--- | :--- |
| 泛型 · 异常 · Optional · Stream | `W02-泛型-异常-Stream.md` | `w02-generics-stream/` | Java基础 +3 |
| Java 并发基础 | `W03-Java并发基础.md` | `w03-concurrency-basics/` | Java并发 +8（✅ 已 8 道） |
| 线程池 · CompletableFuture | `W04-线程池与CompletableFuture.md` | `w04-threadpool-completablefuture/` | Java并发 +2（✅ 已 12 道） |

**M1 解锁标准**：三份笔记各自「核心问题」有个人回答 + 三个工程能一条命令跑起来 + 题库 8 道题有内容。

> 📌 **M1 进展**
>
> | 时间 | 做了什么 | 产出 |
> | :--- | :--- | :--- |
> | 2026-10-28 | **W02 Day 1 · 类型擦除 + PECS**（补掉 W02 空目录 + W01 遗留清单） | `w02-generics-stream/` 工程 + `day1/` 两个 Demo + `compile-errors/` + `day1-result.txt`；笔记 Day 1 整节；`Java基础.md` 12 → **15 题** |
> | 2026-10-29 | **W02 Day 2 · 异常体系** + Day 1 三个遗留 | `day2/` 两个 Demo（10 段实验）+ `day1/WildcardCapture` + `compile-errors/GenericLimits2`（6 个错）+ `day2-result.txt`；修掉 `ErasureProof` B2 段的 `NoSuchMethodException`；笔记 Day 2 整节；`Java基础.md` 15 → **17 题** |
> | 2026-10-30 | **W02 Day 3 · Optional** + Day 2 遗留「异常开销」 | `day3/` 两个 Demo（11 段实验）+ `day2/ExceptionCost`（两遍 JVM 参数对照）+ `day3-result.txt`；笔记 Day 3 整节 + Day 2 §10；`Java基础.md` 17 → **18 题** |
> | 2026-11-02 | **W02 Day 4 · Lambda** | `day4/` 两个 Demo（12 段实验）+ `compile-errors/LambdaLimits{,2,3}`（8 条报错原文，拆三个文件）+ `day4-result.txt` + `day4-bytecode.txt`（javap 三段证据）；笔记 Day 4 整节 + 参考要点 Q7/Q8；`Java基础.md` 18 → **20 题** |
> | 2026-11-02 | **W02 Day 5 · Stream** + Day 4 遗留 | `day5/` 三个 Demo（惰性 / 短路 / 屏障 / peek 坑 / 流一次性 / map vs flatMap / parallel）+ `day5-result.txt`；笔记 Day 5 整节（8 小节）+ 踩坑 4 条 + 参考要点 Q9/Q10；`Java基础.md` 20 → **22 题** |
> | 2026-11-03 | **W02 Day 6 · `List<User>` 报表**，W02 封板 | `day6/` 三个 Demo + `User` / `UserData`（15 条含 3 种 null）+ `day6-result.txt`；笔记 Day 6 整节（8 小节）+ 踩坑 7 条 + 参考要点 Q11；`Java基础.md` 22 → **24 题** |
> | 2026-11-04 | **W03 Day 1 · 线程基础**（`w03-concurrency-basics/` 从 `.gitkeep` 建工程） | `day1/StartThread`（三种起线程 + run/start + daemon）+ `day1/RaceCondition`（基线 / 2 万 vs 50 万对照 / volatile / synchronized 方法级与同步块 / Atomic / 可见性对照）+ `day1-result.txt`（147 行）+ `day1-bytecode.txt`（6 段 javap 证据）；笔记 Day 1 整节（8 小节）+ 踩坑 6 条 + 参考要点「为什么会有线程安全问题」；`Java并发.md` 0 → **2 题** |
> | 2026-11-05 | **W03 Day 2 · `synchronized` 锁的是谁** | `day2/LockTarget`（实例锁 this / 静态锁 Class / `new Object()` 等于没锁 / 字符串常量全局锁）+ `day2/Deadlock`（交叉死锁 + `ThreadMXBean` 检测 / 可重入 / 活锁 4/5 + 随机退避 + 全局定序）+ `day2-result.txt`（116 行）+ `day2-deadlock-jstack.txt`（jstack 原件 142 行）；笔记 Day 2 整节（8 小节）+ 踩坑 5 条 + 参考要点「锁的是谁」「死锁怎么排查」；`Java并发.md` 2 → **4 题**；`cheatsheets/JVM排查命令.md` 补「死锁」小节 |
> | 2026-11-06 | **W03 Day 3 · `volatile` 内存语义 + happens-before + DCL** | `day3/HappensBefore`（Dekker litmus 测出 `(0,0)` 16%~40% / monitor 传可见性 / volatile 发布订阅 5/5 vs 对照组 0/5 / start-join 抢跑 151/500 / `set` vs `lazySet` 5.8 倍）+ `day3/DclSingleton`（50 轮 × 6 线程压力 0 次 + 手工重排抓到 `{a=1,b=null,c=0}` + `Unsafe.allocateInstance`）+ `day3-result.txt`（86 行）+ `day3-bytecode.txt`（170 行，含 `lock addl` 原文）；笔记 Day 3 整节（8 小节）+ 踩坑 6 条 + 面试 9 条 + 参考要点 2 题；`Java并发.md` 4 → **6 题** |
> | 2026-11-11 | **W03 Day 4 · `Lock` / AQS / `wait-notify`**，W03 封板 | `day4/WaitNotify`（ISE / `if` vs `while` / `notify` 只叫一个 → 剩 1 件货 + 1 人 WAITING / `wait` 释放锁 0 ms vs `sleep` 414 ms / 中断清标志）+ `day4/MiniAqs`（**手写 state + CLH 队列 + park**，200 万次对拍零丢失；两次 CAS 与 park 重查的实测 1 / 87742 / 175075 / 87500；临界区 1 ns 慢 5.8 倍 → 0.5 µs 只慢 1.13 倍）+ `day4/LockCompare`（公平锁 41 倍、插队 154582 vs 445、最长等待反而更大；`Condition` 无效唤醒 11 vs 7666）+ `day4-result.txt`（129 行）+ `day4-bytecode.txt`（75 行）；笔记 Day 4 整节（10 小节）+ 踩坑 5 条 + 面试 14 条 + 参考要点 2 题；`Java并发.md` 6 → **8 题** |
> | 2026-11-12 | **W04 Day 1 · 线程池**（`w04-threadpool-completablefuture/` 从 `.gitkeep` 建工程） | `day1/PoolBasics`（A~E：1 万个任务 `new Thread` 620 ms vs 池 13 ms = 47.7 倍 / 线程命名 / keepAlive 只收非核心 3→2 / 四种拒绝策略 / shutdown 5 个跑完 vs shutdownNow 倒出 4 个）+ `day1/PoolFlow`（**S1~S5 五张快照表**：任务顺序「核心 → 队列 → 非核心 → 拒绝」；无界队列下 max 恒为 2 是摆设；core=max / core=0 / SynchronousQueue 三种形态）+ `day1/PoolTraps`（T1~T6：队列峰值 33714 / 单线程堵死 513 ms / cached 峰值 500 线程 / `submit` 吞异常而 `execute` 打印且换线程 / `ThreadLocal` 串味 / **OOM 第 176 个任务** vs 有界队列拒绝 99948 次不 OOM）+ `day1-result.txt`（164 行）+ `day1-jdk-source.txt`（execute 三步 + 排队三条规则原文）；笔记 Day 1 整节（11 小节）+ 踩坑 5 条 + 面试 12 条 + 参考要点 2 题；`Java并发.md` 8 → **10 题** |
> | 2026-11-19 | **W04 Day 2 · CompletableFuture**，W04 封板 | `day2/AsyncBasics`（A 默认池 `commonPool` 并行度 11 = 核数-1，30 任务只用 11 个 worker / B 14 个 300 ms 阻塞任务：公共池 **623 ms** vs 自建池 **308 ms** / C `runAsync` 返回 null、异常静默 / D 自建池 `order-1` / `stock-1` 隔离 / E `completedFuture` / `failedFuture` / 手动 complete 的 CAS 只赢一次）+ `day2/ChainShape`（A `thenApply` 给出 `CF<CF<String>>`，`join()` 打印出 `CompletableFuture@<hash>`，要 join 两次；B 不带 Async 的 `thenApply` 在 **main** 里跑完；C combine 411 ms vs 串行 816 ms；D `allOf` 返回 null、306 ms；E `anyOf` 58 ms、类型擦成 `Object`；F 三次串联 631 ms vs 三路并行 215 ms）+ `day2/AsyncErrors`（A 失败后两段 thenApply 全跳过 / B `exceptionally` 只在失败时跑 / C `handle` 两路都进 / D `whenComplete` 打断点也照样抛 / **E1 上游失败 + whenComplete 抛 → 原始异常胜出、新异常只进外层 suppressed=1**；E2 上游成功 + whenComplete 抛 → 成功变失败；E3 `exceptionally` 抛 → 覆盖且原始异常彻底丢失 / F `join` vs `get` 包装不同 + 双层嵌套 + `unwrap` / G 返回值没人接 → 原 CF 仍失败 / H `orTimeout` 212 ms vs `completeOnTimeout` 211 ms）+ `compile-errors/ChainTypeErrors`（3 条 `incompatible types` 原文）+ `day2-result.txt`（215 行）+ `day2-jdk-source.txt`（`uniWhenComplete` / `reportJoin` / `commonPool` 并行度原文）；笔记 Day 2 整节（9 小节）+ 踩坑 4 条 + 面试 9 条 + 参考要点 Q3/Q4；`Java并发.md` 10 → **12 题** |
> | 2026-11-20 | **W05 Day 1 · Spring IOC / DI**（`w05-spring-ioc-di/` 从 `.gitkeep` 建工程，M2 开工） | 手工 `lib/` 放 Spring 5.3.39 六个 jar（本机无 Maven）；`day1/ManualWiring`（A 三入口各自 `new` → **3 份仓储**、B 集中装配 → 1 份、C 数源码：换实现 A 段 3 处 / B 段 1 处，加依赖 3 处）+ `day1/SpringWiring`（A 构造发生在 `+312~338 ms`、`refresh` 返回 `+354 ms`；栈上 `BeanUtils.instantiateClass:213` + `createBeanInstance:1222/1232` + `preInstantiateSingletons:955`；B `s1 == s2`；C 入口代码 `new X(` = **0**；D jdbc→memory 零改调用点；E `@Lazy` `containsSingleton` false→true；F prototype `p1 == p2 : false`、不进池）+ `day1/ContainerQuestions`（Q1 两种 `NoSuchBeanDefinitionException`；Q2 `@Autowired` 在 `new` 出来是 null、注册后是 jdbc；Q3a 字段注入解环成功 `b.a == a`、Q3b 构造器注入三层异常到 `BeanCurrentlyInCreationException`；Q4 定义 13 / 单例 19，取过 prototype+lazy 后 13/20）+ `day1-result.txt`（355 行）+ `day1-spring-source.txt`（6 段源码，行号与栈对撞）；笔记 W05 Day 1 整节（9 小节）+ 踩坑 4 条 + 面试 10 条 + 参考要点 Q1/Q2/Q3；`Spring.md` 0 → **3 题** |
> | 2026-11-23 | **W05 Day 2 · Bean 生命周期** | `day2/LifecycleOrder`（A 六回调时间线：①构造 `+375.7` → ②BPP.before `+386.7` → ③`@PostConstruct` `+389.0` → ④`afterPropertiesSet` `+389.5` → ⑤`initMethod` `+390.2` → ⑥BPP.after `+390.5` →⑦`@PreDestroy` →⑧`destroy` →⑨`destroyMethod`；属性注入用「构造时 repo=null / 初始化时 repo=jdbc」夹住；B 销毁**逆序 + 依赖方先死**；C prototype 销毁回调 **0 次**，源头 `AbstractBeanFactory:1942` 的 `!mbd.isPrototype()`；D `@Bean` 默认推断 `close()`：1 次 vs `destroyMethod=""` 的 0 次）+ `day2/PostProcessorTrace`（A 计数表：正常 Bean **2/2**、`bppFirst`/`bppSecond`/`earlyDependent` **0/0**；B `@PostConstruct` 栈 `InitDestroyAnnotationBeanPostProcessor:157` ← `applyBPPBefore:440` ← `initializeBean:1796`；C 三追问；D `Ordered` 序列 `[first.before, second.before, first.after, second.after]`）+ 新增 `support/Stacks` + `lib/javax.annotation-api-1.3.2.jar`；`day2-result.txt`（140 行）+ `day2-spring-source.txt`（7 段源码）；笔记 W05 Day 2 整节（9 小节）+ 踩坑 3 条 + 面试 8 条 + 参考要点 Q4/Q5；`Spring.md` 3 → **5 题** |
> | 2026-11-23 | **W05 Day 3 · Bean 作用域** | `day3/ScopeMatrix`（A `singleton` vs `prototype` 三条判据：`refresh` 后构造数 `1 / 0`、两次 `getBean` 的 `==` 为 `true / false`、`containsSingleton` `true / false`、`close()` 后销毁 `1+1 vs 0+0`；B `registerScope("singleton", …)` → `Cannot replace existing scopes`，`getRegisteredScopeNames()` 只有 `[thread]`，`SimpleThreadScope` 每线程一个实例并打 WARN「不支持销毁回调」；C 四作用域判据矩阵）+ `day3/WebScopeTrap`（A `request` → `IllegalStateException: No Scope registered for scope name 'request'`，`doGetBean:369`，**refresh 不报错、getBean 才报**；B `session` 同款；C `proxyMode` 三组：无代理三次 `id()` 全 `1`（造 1 个）、`TARGET_CLASS` 后 `2/3/4`（造 3 个）、定义被拆成 `scopedTarget.requestProxy`（`scope=request`）+ `requestProxy`（`ScopedProxyFactoryBean` 单例）、`request` + 代理后 `getBean` 不抛而首次调用抛（栈多一帧 `SimpleBeanTargetSource:35`）；D 自定义 `Scope`「每 2 次换实例」→ `A A B B`、轮换销毁 1 次、`close()` 后仍 1 次）+ `day3/ScopeProbe`；`day3-result.txt`（154 行）+ `day3-spring-source.txt`（7 段源码）；笔记 W05 Day 3 整节（6 小节）+ 踩坑 5 条 + 面试 6 条 + 参考要点 Q6/Q7；`Spring.md` 5 → **7 题** |
> | 2026-11-24 | **W05 Day 4 · 注入方式的取舍** | `day4/InjectionChoice`（A 三个候选 + 字段名对不上 → `NoUniqueBeanDefinitionException: expected single matching bean but found 3: alipayChannel,wechatChannel,unionPayChannel`，**死在 `refresh()` 里**（栈：`resolveNotUnique:220` ← `doResolveDependency:1369` ← `AutowiredFieldElement.resolveFieldValue:710` ← `postProcessProperties:408` ← `populateBean:1431` ← `doCreateBean:619`）；B **字段名回退**：字段名 `wechatChannel` + 零注解 → 注入到 `wechat`；C `@Qualifier("alipayChannel")` → `alipay`，点名不存在 → `expected at least 1 bean which qualifies as autowire candidate`；D 字段名依旧 `wechatChannel` + `@Primary` → 注入 `preferred`（**Primary 在名字之前**）；E `@Resource` 默认名 = 字段名 → `unionpay`，`name="alipayChannel"` + 字段名 `anything` → `alipay`；F1 显式名对不上 → **`No bean named 'channel' available`**（不查类型）；F2 默认名对不上 → 退回按类型 → 同一个 `NoUnique`） + `day4/CollectionInject`（A `List` / 数组 = `wechat → alipay → unionpay` 按 `@Order`；B `Map` key = bean 名、顺序 = 注册顺序 `[alipayChannel, wechatChannel, unionPayChannel]`，源码 1493-1500 无一行排序；C 零候选三段：`required=true` 的 `List` **照样抛** `expected at least 1 bean` / `required=false` 给 **null**（不是空集合）/ 单值同款异常；D 集合 + `@Qualifier` → 缩到 1 个再全收） + `day4/CtorInject`（A 单构造器免注解 → `log`；B1 两个构造器无无参 → `BeanInstantiationException: No default constructor found`，B2 有无参则**静默**走无参；C 标了 `@Autowired` 的那个赢；D1 原型 `getObject()` 两次 = `temp-1 / temp-2`，D2 零候选 `getIfAvailable`/`getIfUnique` 都是 null，D3 三候选 `getIfUnique()` null 但 `getIfAvailable()` **抛** `NoUnique`；E 反射读到的参数名 = `arg0`（`isNamePresent=false`）→ 参数名回退失效）+ `support/Quiet`；`day4-result.txt`（145 行）+ `day4-spring-source.txt`（8 段源码）；笔记 W05 Day 4 整节（8 小节）+ 踩坑 4 条 + 面试 8 条 + 参考要点 Q8/Q9；`Spring.md` 7 → **9 题** |
> | 2026-11-25 | **W05 Day 5 · 收尾封板** | `day5/BoardCheck`（**把四天的口述结论写成 23 条断言**，FAIL 即 `exit=1`：S1 对象图 3 条 `3 份 → 1 份`、S2 生命周期 4 条（七步顺序 `[ctor, inject(setter), bpp.before, @PostConstruct, afterPropertiesSet, initMethod, bpp.after]` / 销毁三件套 / prototype 造 2 销 0）、S3 作用域 7 条（refresh 后 `1 / 0`、`==` 的 `true / false`、进池 `T/F`、销毁 `1+1 / 0`）、S4 判定链 9 条（NoUnique 原文含三候选名 / 字段名回退 `wechat` / `@Primary` 压名字 `preferred` / `@Qualifier` `alipay` / 两种报错 / 两种外层包装）；实测 **23/23 通过**）+ `day5-result.txt`（85 行）+ `day5-spring-source.txt`（4 段，713 vs 332 行号对撞）+ `pom.xml`（**未用 mvn 验证**：本机无 Maven，只验了 XML 合法）；笔记 W05 第四格答案三段（对象图 / 生命周期 / 边界）+ Day 5 整节（6 小节）+ 一句话总结 + 参考要点 Q10 + 踩坑 3 条 + 面试 4 条 + 打卡；`Spring.md` 9 → **10 题**（Q10 异常链怎么读），**并修正 Q9「完全同一个错」→「内层一样、外层不同」** |
> | 2026-11-26 | **W06 Day 1 · AOP：代理什么时候套上去**（`w06-spring-aop/` 从 `.gitkeep` 建工程，`lib/` 新增 **`aspectjweaver-1.9.7`**） | `day1/JdkProxy`（**纯 JDK 不引 Spring**，八段：A 不代理 handler 0 次 / B 一次调用 1 次、类名 `jdk.proxy1.$Proxy0` / C **嵌套调用仍 1 次**，业务跑了 3 个方法 / D 代理套代理 2 次 / E 同接口同类、多实现一个接口换 `$Proxy1` / F `hashCode`·`toString`·`equals` **三个都进** handler / G 强转 **CCE** / H package-private 接口 → 代理类落到接口所在包 `com.newt…day1.$Proxy4`）+ `day1/ProxyIdentity`（S1 手工 `new` 的实例 `before=0` / S2 `jdk.proxy2.$Proxy22`、实现 `SpringProxy`+`Advised`+`DecoratingProxy` / **S2b 两个错**：按类型 `NoSuchBeanDefinition`、按名字+类型 `BeanNotOfRequiredType` / S3 before·around·after **1/1/1**、`getThis()==getTarget()` 为 false / S4 `$$EnhancerBySpringCGLIB$$`、父类就是 `GreetingImpl` / S5 无接口自动 CGLIB + **`final sealed()` 增量 0**（静默失效）/ S6 切面自己 `isAopProxy=false` / S7 顺序 `@Around 进入 → @Before → @After → @Around 返回`）+ `day1/SelfInvocation`（S1 `this` **1** 次 / S2 `self` **2** 次 / S3 `currentProxy()` **2** 次 / S4 **三层探针**：`self==bean` false、`TargetSource=ContextAnnotationAutowireCandidateResolver$1`、`getTarget()==bean` true / S5 没开 `exposeProxy` 的 ISE 原文）+ 源码取证 8 段（`DefaultAopProxyFactory:58` / `JdkDynamicAopProxy:126` 同一句 `newProxyInstance` / `:225·239·241` 取链与 proceed / `AopContext:50` NamedThreadLocal / `AopUtils:68-70` 判据 / `buildLazyResolutionProxy:83·93` / `wrapIfNecessary:328·341·351` / JDK `Proxy:309·427` 类缓存）；`day1-result.txt`（189 行）+ `day1-spring-source.txt`（8 段）；笔记 W06 Day 1 整节（8 小节）+ 踩坑 6 条 + 面试 9 条 + 参考要点 Q1~Q3；`Spring.md` 10 → **12 题** |
>
> **W02 已封板（2026-11-03）**，六天全部完成。**W03 已封板（2026-11-11）**，四天全部完成。
> **W04 已封板（2026-11-19）**，两天全部完成 —— 五个知识点全勾。
> **M1 已封板（2026-11-20）**：M1 要的「4 份笔记 + 4 个可运行工程 + 8 道面试题」全部超额
> —— W01~W04 四份笔记、四个一条命令能跑起来的工程、题库 24（Java基础）+ 12（Java并发）＝ **36 道**。
> 当前在 **M2 · Spring 内核**：**W05 已封板（2026-11-25）**，五个时间盒全部完成
> （IOC/DI → 生命周期 → 作用域 → 注入取舍 → 封板自检），四个知识点全勾，
> 工程从 `.gitkeep` 建起、`lib/` 手工放 7 个 jar（Day 2 多了 `javax.annotation-api`），
> 外加 `pom.xml` 一份（**未用 mvn 验证**）。
> **W06 · AOP 已开工（2026-11-26，Day 1 完成）**：代理在 `wrapIfNecessary:328` 那一层被套上去，
> JDK 与 CGLIB 的分界在 `DefaultAopProxyFactory:58`，`this.xxx()` 的失效在纯 JDK 代理里就能证明。
>
> W04 Day 2 最值钱的三条结论（全部有实测数字）：
>
> 1. **不传 executor 的 `supplyAsync` 落在 `ForkJoinPool.commonPool()`，并行度 = 核数-1**（12 核 → 11），
>    且**全 JVM 共享**。14 个 300 ms 阻塞任务：公共池 623 ms（分两批）、自建池 308 ms ——
>    这是「为什么 `@Async` 必须指定线程池」最硬的证据。
> 2. **`thenApply` 与 `thenCompose` 的差别是静态类型，但编译器不拦**：前者 `U` 会被推成
>    `CompletableFuture<String>`，结果 `CF<CF<String>>`，`join()` 打印 `CompletableFuture@<hash>`。
>    和 W02 的 `map` / `flatMap` 同构。
> 3. **`whenComplete` 物理上吞不掉异常**（`BiConsumer` 无返回值）：上游已失败时它抛的异常只进
>    外层 `suppressed`（实测 = 1），原始异常胜出；而 `exceptionally` 里抛异常会**覆盖并使原始异常彻底丢失**。
>
> W04 Day 1 最值钱的三条结论（全部有实测数字）：
>
> 1. **任务顺序是「核心 → 队列 → 非核心 → 拒绝」**，不是「先开满线程再排队」。
>    实测 core=2 max=4 queue=2 提 10 个：线程数 `1→2→2→2→3→4→4`，队列 `0→0→1→2→2`。
>    JDK 类文档原话 `only if the queue is full`。这个顺序不看代码想不出来，也是最高频的追问。
> 2. **队列决定了 `maximumPoolSize` 是否生效**：无界队列 → 永远不满 → 永远走不到「开非核心」，
>    实测 max=4 峰值线程恒为 2。所以 `newFixedThreadPool(n)` 的 n 就是真实并发上限。
> 3. **无界队列的问题不是「会 OOM」，是「OOM 之前一切正常」**：`-Xmx64m` 下第 176 个任务才炸，
>    之前拒绝数一直是 0；换有界 50 + Abort 则拒绝 99948 次、堆稳稳的。
>
> W02 六天里最值钱的五条结论（讲课时直接能用，全部有实测数字）：
>
> 1. 擦除擦的是**实参**、留下的是**声明**；实参藏在子类 `Signature` 里（`getGenericSuperclass()`）。
> 2. **CCE 抛在 `get` 那一行，不是 `add` 那一行**；`finally` 里的 `return` 才是真的把异常变没。
> 3. **`new RuntimeException(e)` 不丢栈，是把栈切成两段**（实测 3 层 vs 5 层）；只传 message 才真丢。
> 4. **`count()` 会跳过整条管道连 `peek` 一起**（日志 0 条）；`sorted()` 在上游带 `SORTED` 特征时是空操作。
> 5. **`Predicate` 的 `or().and()` 是左结合 `(a||b)&&c`**，按布尔优先级写会静默少数据（实测 6 人 vs 7 人）。

---

## 五、M2 · Spring 内核（W05–W08 内容）

四份笔记 + 四个 Demo，其中 **W07 事务、W08 异步是全局最重点**——
因为你工作里正好踩过 `@Transactional` 和 `@Async` 失效的坑，这两个是"真实问题驱动"的学习。

`02-code/lab/` 下已经建好了对应的实验目录，直接用：

- `01-transaction-self-invocation` — `this.xxx()` 为什么让事务失效
- `02-transaction-propagation` — `REQUIRED` vs `REQUIRES_NEW`
- `03-async-transaction-boundary` — 事务为什么不会跨线程继承

> **M2 的验收方式很特别**：不看笔记写得多漂亮，看你能不能**当场写一个 Demo 让事务失效**，
> 再讲清楚怎么修。这是面试里最能体现深度的一类回答。

---

## 六、M3–M6（先不细化）

里程碑制的原则是**只细化当前和下一个，再远的等走到跟前再说**。
M3 之后的内容原封不动沿用 `java-48week-plan.md` 里的知识点清单即可，
到时候按同样的"保留 / 降级 / 砍掉"逻辑过一遍。

---

## 七、和原文件的关系

| 文件 | 作用 | 现在怎么用 |
| :--- | :--- | :--- |
| `MILESTONES.md`（本文件） | **唯一进度来源** | 每天看这个 |
| `PROGRESS.md` | 原 48 周勾选表 | **降级为参考**，不再要求勾齐；W01 封板时勾 W01 一行 |
| `java-48week-plan.md` | 知识点清单 | **当字典查**，不看周次 |
| `STAGES.md` | 阶段能力矩阵 | 保留，每完成一个里程碑自评一次 |

---

## 八、下一步

**M0 已封板（2026-10-27）、M1 已封板（2026-11-20），当前焦点是 M2 · Spring 内核。**

**W02 Day 1 已完成（2026-10-28）**：`w02-generics-stream/` 工程建好，`ErasureProof` / `PecsDemo`
两个 Demo + `compile-errors/GenericLimits.java` 四类报错原文跑通，笔记 Day 1 整节写完。

**W02 Day 2 已完成（2026-10-29）**：异常体系。`day2/` 两个 Demo 共 10 段实验
（TWR 逆序关闭与 suppressed / `finally` 里 `return` 吞异常 / `finally` 里 `throw` 覆盖主异常 /
包装是切栈而非丢栈 / cause 与 suppressed 分工 / 受检边界 / sneakyThrow）。
顺带补掉 Day 1 三个遗留：修 `ErasureProof` B2 段的 `NoSuchMethodException`、
`compile-errors/GenericLimits2.java`（6 个通配符报错）、`day1/WildcardCapture.java`（捕获正例 + JDK 的 raw 选择）。

**W02 Day 3 已完成（2026-10-30）**：Optional。`day3/` 两个 Demo 共 11 段实验
（`isPresent()+get()` 的报错退化 / `orElse` eager 求值白打远程 / Optional 当字段不可序列化 /
当参数签名骗人 / 五个静默陷阱 / `map` vs `flatMap` / `filter` 挡掉负价 / 优惠券算价）。
顺带把 Day 2 最后一个遗留量掉了：异常开销 447 ns → 关掉栈采集 31 ns（15 倍），
`day2/ExceptionCost.java`，跑两遍只改一个 JVM 参数。

**W02 Day 4 已完成（2026-11-02）**：Lambda。`day4/` 两个 Demo 共 12 段实验
（五种形状 / `Predicate` 短路组合 / `andThen` vs `compose` / 装箱逃逸对照 / 自定义函数式接口 /
lambda 缺目标类型 / 方法引用五连 / 值捕获 / 循环变量 / `this` 归属 / 实例复用 / `invokedynamic`）。
核心结论：`javap -p` 里 `lambda$sectionB$1(int, int)` 直接证明**捕获 = 把变量当实参传进静态方法**，
`effectively final` 由此不再需要背；装箱代价实测不逃逸 **2 倍**、逃逸 **36 倍**。
`compile-errors/LambdaLimits{,2,3}` 收 8 条报错原文 —— 必须拆三个文件，否则 attribution 阶段的报错
会让 FLOW 分析整个跳过，`effectively final` 那条根本不出现。

**W03 Day 1 已完成（2026-11-04）**：线程基础。`w03-concurrency-basics/` 工程从零建起，
`day1/StartThread`（三种起线程 + `run()` vs `start()` + daemon）+ `day1/RaceCondition`
（B0 基线 / B1 每线程 2 万 / B2 50 万 / C1 volatile / C2 synchronized 方法级与同步块 / C3 Atomic / C4 可见性对照），
产出 `day1-result.txt`（147 行）+ `day1-bytecode.txt`（6 段 `javap` 证据）。
三条核心结论：① `count++` 是 `getfield`/`iconst_1`/`iadd`/`putfield` 四条指令，窗口在 `iadd` 与 `putfield` 之间；
② 每线程 2 万次时三次运行只丢 4~7/10 轮（JIT 预热后单线程几十微秒跑完，10 个线程没重叠），
加量到 50 万次才 10/10 轮稳定复现；③ 方法级 `synchronized` **没有** `monitorenter`，
只有 `ACC_SYNCHRONIZED` 标志（原本以为有，被 `javap` 打脸），同步块才发指令并带异常路径。

**W03 Day 2 已完成（2026-11-05）**：`synchronized` 到底锁了谁。
`day2/LockTarget`（A 实例方法锁 `this`：同实例第二个排队 414 ms / 两实例都 0 ms / 与 `synchronized(this)` 抢同一 monitor，
排队 340 ms；B 静态方法锁 `Class`：排队 400 ms、`holdsLock(ClassLock.class)=true`、与实例锁并行 412 ms；
C `synchronized(new Object())` 丢 2846644/5000000 且耗时仅 46 ms vs 真锁 339 ms，外加字符串常量的跨类全局锁）
+ `day2/Deadlock`（D1 交叉死锁 + `findDeadlockedThreads()`；D2 可重入持计数；D3a 活锁 4/5、D3b 随机退避、
D3c 全局定序 4000/4000 零重试）。产出 `day2-result.txt`（116 行）+ `day2-deadlock-jstack.txt`（jstack 原件 142 行）。

四条核心结论：① 三种写法三把锁，证据用 `Thread.holdsLock()` + 「排队时长」，**总耗时会骗人**；
② `static synchronized` 保护不了实例字段（并行实测 412 ms）—— `@Transactional` 失效那一类 bug 的病根；
③ **「加了锁反而变快」几乎一定是锁没生效**（`new Object()` 46 ms vs 真锁 339 ms）；
④ 死锁的发现（`ThreadMXBean`）/ 确认（`jstack` 三样）/ 破解（全局定序）是三件事；活锁是概率的，死锁是确定的。

**W03 Day 4 已完成（2026-11-11）· W03 封板**：`Lock` / AQS / `wait-notify`。
`day4/WaitNotify`（A `wait()` 不在锁里 → `IllegalMonitorStateException: current thread is not owner`；
B 2 消费者 + 1 件货 + `notifyAll`：`if` 版空手而归 1 次 vs `while` 版 0 次；
C 放 2 件货只 `notify()` 一次 → 取走 1 件、剩 1 件、1 个消费者永久 `WAITING`（对比 `notifyAll` 2/0/0），
外加「没人在等时 `notify` 直接丢弃」（`wait(200)` 等满 200 ms）；
D `wait` 释放锁：旁观者排队 0 ms vs 持锁 `sleep` 414 ms；E 中断时 JVM 先清标志再抛异常）
+ `day4/MiniAqs`（**手写 `state` + CLH 双向队列 + `LockSupport.park/unpark`**：
200 万次对拍零丢失、不锁对照丢 1551791；`state` 1→3→1→0 加重入 ISE；
两次 CAS 实测「建哨兵 1 次 / 抢尾 87742 次」；175075 次挂起里 **87500 次醒来没抢到**；
临界区 1 ns 时比 `synchronized` 慢 5.8 倍，拉到 0.5 µs 只慢 1.13 倍，差额 ≈ `park × 4.2 µs`）
+ `day4/LockCompare`（公平锁慢 41 倍、插队 154582 次 vs 445 次，但**最长等待反而更大** 582 µs vs 387 µs；
`Condition` vs `notifyAll` 无效唤醒 11 次 vs 7666 次）。
产出 `day4-result.txt`（129 行）+ `day4-bytecode.txt`（75 行）。

三条核心结论：① **`park` 返回 ≠ 拿到锁**，87500 次「醒来没抢到」就是「必须重查」的实测证据，
最凶的插队者是「刚 `unlock` 转头又 `lock`」的线程；② 手写 AQS 慢的不是队列是**挂起**
（4.2 µs/次），临界区一变长差距就没了；③ 公平锁换来的不是「等得更短」而是「**等得有上界**」。

**W04 Day 1 已完成（2026-11-12）· 线程池**：`day1/PoolBasics`（A 1 万个任务：逐个 `new Thread` + `join`
620 ms vs 固定 8 线程池 13 ms＝**47.7 倍**，折合 62 µs / 线程；B 默认名 `pool-3-thread-1` 的 `3` 是全局池序号；
C `keepAlive` 只管非核心：堵住时 `poolSize=3`，+400 ms 回落到 2，`allowCoreThreadTimeOut(true)` 才归 0；
D 四种拒绝策略：Abort 抛 `#3#4#5` / CallerRuns 让 `#3#5` 在 `main` 里跑 / Discard 只跑 `#1#2` /
DiscardOldest 只跑 `#1#5`（连丢 `#2#3#4`）；E `shutdown` 5 个全跑完 vs `shutdownNow` 倒出 4 个 + 中断 1 个）
+ `day1/PoolFlow`（**S1~S5 五张快照表**，每提交一个拍一张 `poolSize` / `queueSize` / 是否被拒；
S1 线程数 `1→2→2→2→3→4→4`、队列 `0→0→1→2→2`；S2 无界队列峰值线程恒为 2 → **max 是摆设**；
S3 core=max 没有非核心阶段；S4 core=0 先进队再借线程；S5 `SynchronousQueue` 等于 `cached` 形态）
+ `day1/PoolTraps`（T1 队列峰值 33714；T2 单线程池堵死 513 ms；T3 cached 峰值 500 线程；
T4 **`execute` 打印堆栈且线程被替换 `thread-1`→`thread-2`，`submit` 不 get 一个字都不打**；
T5 `ThreadLocal` 串味读到上一个人的登录信息；T6 `-Xmx64m` 下**第 176 个任务 OOM** / 有界 50 拒绝 99948 次不 OOM）。
产出 `day1-result.txt`（164 行）+ `day1-jdk-source.txt`（`execute()` 三步注释 + 类文档「排队三条规则」原文）。

三条核心结论：① **顺序是「核心 → 队列 → 非核心 → 拒绝」**，JDK 原话 `only if the queue is full`；
② **队列决定 `maximumPoolSize` 是否生效** —— 无界队列永远不满，永远走不到「开非核心」；
③ **无界队列的问题不是「会 OOM」，是「OOM 之前一切正常」**（拒绝数一直是 0）。

**W04 Day 2 已完成（2026-11-19）· CompletableFuture**：`day2/AsyncBasics`（A 默认池 `commonPool`
并行度 11，30 个任务只用到 11 个 worker；B 14 个 300 ms 阻塞任务：公共池 623 ms / 自建池 308 ms；
C 异常静默直到 `join`；D 自建池线程名 `order-1` / `stock-1`；E 三种现成 CF + complete 的 CAS）
+ `day2/ChainShape`（A `thenApply` → `CF<CF<String>>` 实测 vs `thenCompose` → `String`；
B 不带 Async 的 `thenApply` 在 `main` 里跑；C combine 411 ms vs 串行 816 ms；D `allOf` 306 ms、返回 null；
E `anyOf` 58 ms、类型 `Object`；F 三次串联 631 ms vs 三路并行 215 ms）
+ `day2/AsyncErrors`（A 失败后后续节点全跳过；B/C/D 三种写法的吞与不吞；
E 三段对照 **whenComplete 抛异常时原始异常胜出（suppressed=1）/ exceptionally 抛异常时原始异常丢失**；
F `join` vs `get` + 双层包装 + `unwrap`；G 返回值没人接；H `orTimeout` 212 ms / `completeOnTimeout` 211 ms）。
产出 `day2-result.txt`（215 行）+ `day2-jdk-source.txt`（`uniWhenComplete` 844-877 / `reportJoin` /
`ForkJoinPool` 并行度 2564）+ `compile-errors/ChainTypeErrors.java`（3 条报错原文）。

三条核心结论：① **默认池是 `commonPool`，并行度 = 核数-1，全 JVM 共享** —— 阻塞任务必须自带线程池；
② **`thenApply` 用错不报错**，只是悄悄给你 `CF<CF<T>>`，`join()` 拿到一个 Future 对象；
③ **`whenComplete` 是围观不是修复**（`void` 无出口），上游失败时它的异常只能挂在 suppressed 上。

**W05 Day 1 已完成（2026-11-20）· M2 开工**：Spring IOC / DI。
`day1/ManualWiring`（A 三个入口各自 `new` → **3 份仓储 / 三个不同哈希**；B 装配收进 `main` → 1 份；
C 数源码：换实现 A 段 3 处、B 段 1 处，加依赖 3 处）
+ `day1/SpringWiring`（A 构造 `+312~338 ms`、`refresh` 返回 `+354 ms`，栈上
`BeanUtils.instantiateClass:213` / `createBeanInstance:1222·1232` / `preInstantiateSingletons:955`；
B `s1 == s2`；C 入口代码 `new X(` = **0**，`MemoryConfig` 里唯一 1 个 `new`；
D jdbc→memory 零改调用点；E `@Lazy` `containsSingleton` false→true；F prototype `p1 == p2 : false`）
+ `day1/ContainerQuestions`（Q1 两种 `NoSuchBeanDefinitionException`；Q2 `@Autowired` 在 `new` 出来是 null、
注册后是 jdbc；Q3a 字段注入解环 `b.a == a`、Q3b 构造器注入三层异常到 `BeanCurrentlyInCreationException`；
Q4 定义 13 / 单例 19，取过 prototype + lazy 后 13 / 20）。
产出 `day1-result.txt`（355 行）+ `day1-spring-source.txt`（6 段源码，行号与栈对撞）。

三条核心结论：① **IOC 的价值是「3 个调用点 → 1 个装配点」，不是「少写 new」**
（`@Bean` 里仍然 `new`，实测计数 1）；② **Bean 在 `refresh()` 里就造完了**，
时间戳全部早于「容器创建完成」，那 300 多毫秒是类加载 + 扫包而不是 `new`；
③ **循环依赖的差别只有源码三行的顺序**（582 造对象 → 613 提前暴露 → 619 灌字段），
字段注入吃到「时间差」，构造器注入连空壳都没有。

**W05 Day 2 已完成（2026-11-23）· Bean 生命周期**：`day2/LifecycleOrder`（A 六回调时间线，
`+375.7` 构造 → `+390.5` BPP.after，`refresh()` 在 `+408.7` 返回；属性注入没有回调，靠
「构造时 `repo=null` / 初始化时 `repo=jdbc`」夹住；B 销毁逆序且**依赖方先死**；C prototype 销毁
**0 次**；D `@Bean` 默认推断 `close()`：1 次 vs `destroyMethod=""` 的 0 次）
+ `day2/PostProcessorTrace`（A 计数表：正常 Bean **2/2**、两个后处理器自己与 `earlyDependent` **0/0**；
B `@PostConstruct` 执行栈 `:157` ← `:440` ← `initializeBean:1796`；C 三个追问；D `Ordered` 序列）。
产出 `day2-result.txt`（140 行）+ `day2-spring-source.txt`（7 段源码，行号与栈对撞）。

三条核心结论：① **`@PostConstruct` 是后处理器调起来的**（不是容器主流程），所以它落在
`BPP.before` 的遍历里；② **prototype 的销毁回调不是「忘了调」，是登记阶段就被
`!mbd.isPrototype()` 短路了**；③ **后处理器只看得见「注册完成之后」出生的 Bean，
同批还互相看不见**（先全部 `getBean` 收集再统一注册）—— 被提前创建的那个会收到
`not eligible for auto-proxying`，即代理套不上，`@Transactional` / `@Async` 必然失效。

**W05 Day 3 已完成（2026-11-23）· Bean 作用域**：`day3/ScopeMatrix`（A 三条判据：`refresh` 后构造数
`1 / 0`、两次 `getBean` 的 `==` 为 `true / false`、`containsSingleton` `true / false`、`close()` 后
`@PreDestroy`+`destroy` 为 `1+1` vs `0+0`；B `registerScope("singleton", …)` →
`Cannot replace existing scopes 'singleton' and 'prototype'`，`getRegisteredScopeNames()` 只有 `[thread]`，
`SimpleThreadScope` 每线程一个实例并打 WARN「不支持销毁回调」；C 四作用域判据矩阵）
+ `day3/WebScopeTrap`（A `request` → `IllegalStateException: No Scope registered for scope name 'request'`
（`doGetBean:369`），**`refresh()` 不报、`getBean` 才报**；B `session` 同款；C `proxyMode`：
无代理三次 `id()` 全 `1`（造 1 个）vs `TARGET_CLASS` 后 `2/3/4`（造 3 个），定义被拆成
`scopedTarget.requestProxy`（`scope=request`）+ `requestProxy`（`ScopedProxyFactoryBean` 单例），
`request` + 代理后 `getBean` 不抛而首次调用抛（多一帧 `SimpleBeanTargetSource:35`）；
D 自定义 `Scope`「每 2 次换实例」→ `A A B B`、轮换销毁 1 次、`close()` 后仍 1 次）
+ `day3/ScopeProbe`。产出 `day3-result.txt`（154 行）+ `day3-spring-source.txt`（7 段源码，行号与栈对撞）。

三条核心结论：① **`singleton` / `prototype` 不在作用域表里**，是 `doGetBean` 写死的两个 `if`
（覆盖会被拒：`Cannot replace existing scopes`），第三个 `else` 才查表 ——
「取不到」的真实原因是「表里没有这一项」；② **`request` 的错推迟到 `getBean`**，
因为它不是单例，`preInstantiateSingletons` 不碰它；③ **`proxyMode` 换的是「解析时机」不是「解析结果」**：
代理每次调用回容器要一次（原型注入单例从 `1/1/1` 变 `2/3/4`），但 `request` 在非 Web 容器里
照样抛同一个 ISE，只是从 `getBean` 挪到了第一次方法调用 —— 顺带记住「自定义作用域的销毁回调交给
`scope.registerDestructionCallback`，`ctx.close()` 只销毁单例」。

**W05 Day 4 已完成（2026-11-24）· 注入方式的取舍**：`day4/InjectionChoice`（A 三候选 + 字段名对不上 →
`NoUniqueBeanDefinitionException: expected single matching bean but found 3`，死在 `refresh()` 而不是 `getBean`；
B **字段名回退**：字段名 `wechatChannel` + 零注解 → 注入 `wechat`；C `@Qualifier` → `alipay`；
D 字段名不变 + `@Primary` → `preferred`；E `@Resource` 默认名 = 字段名；F1 显式名对不上 →
`No bean named 'channel' available`（不查类型）/ F2 默认名对不上 → 退回按类型 → 同一个 `NoUnique`）
+ `day4/CollectionInject`（`List` 按 `@Order`、`Map` 按注册顺序且 key = bean 名、零候选三段：
`required=true` 照样抛 / `required=false` 给 null / 单值同款）+ `day4/CtorInject`（单构造器免注解、
多构造器不标的两种结局、`ObjectProvider` 三局面、参数名回退要 `-parameters`）+ `support/Quiet`。
产出 `day4-result.txt`（145 行）+ `day4-spring-source.txt`（8 段源码）。

三条核心结论：① **挑人是四级链**：`@Qualifier` 先缩范围 → `@Primary` → `@Priority` → 名字匹配，
所以 `@Primary` 能让「字段名对得上」失效，反过来不成立；② **`@Autowired` 不是纯按类型**，
多候选时拿字段名当 bean 名回退；`@Resource` 默认名也是字段名，但**显式点名时不查类型** ——
这就是两种注解报错不同的原因；③ **集合不是兜底**：零候选 + `required=true` 照样抛，
`required=false` 给 `null`；`Map` 不像 `List` 那样按 `@Order` 排序（源码 Map 分支无排序）。

**W05 Day 5 已完成（2026-11-25）· 收尾封板**：`day5/BoardCheck`（**把四天口述过的结论写成 23 条断言**，
任一条不成立就 `exit=1`，可以当回归用例跑：S1 对象图 3 条 / S2 生命周期 4 条 / S3 作用域 7 条 / S4 判定链 9 条；
实测 **23 / 23 通过**）+ `day5-result.txt`（85 行）+ `day5-spring-source.txt`（4 段，713 vs 332 行号对撞）
+ `pom.xml`（`spring-context` 一个坐标带 5 个 spring-*、`javax.annotation-api` 显式写；**未用 mvn 验证**）。
笔记第四格答案三段 + 一句话总结 + 参考要点 Q10；`Spring.md` 9 → **10 题**。

三条核心结论：① **属性注入的位置可以说死，不只夹住** —— 给 Bean 加一个 `@Autowired` 的 setter 打点
（setter 是自己的代码、不是容器回调），实测顺序 `ctor → inject(setter) → bpp.before → @PostConstruct → …`，
`populateBean` 就落在 `ctor` 与 `bpp.before` 之间（对应 `doCreateBean` 的 582 / 619 / 620）；
② **「两种注解报同一个错」只对了内层** —— `@Autowired` 外层是 `UnsatisfiedDependencyException`
（`resolveFieldValue:713` 的 `catch (BeansException)`），`@Resource` 外层是 `BeanCreationException`
+ `Injection of resource dependencies failed`（`postProcessProperties:332` 的 `catch (Throwable)`）；
根源是 `NoUnique extends NoSuchBean extends BeansException`，两处 `catch` 都接得住，谁接住用谁的壳；
**内层讲「为什么」，外层讲「谁在灌字段的时候炸的」**；
③ **「讲得出」和「断言得住」是两件事** —— 前者靠理解，后者靠把期望值写下来；
封板要求后者，因为换个 JDK / Spring 小版本时，只有断言会告诉你哪个数字过期了。

**W06 Day 1 已完成（2026-11-26）· AOP：代理什么时候套上去**：`day1/JdkProxy`（纯 JDK，八段：
A 不代理 `handler` 0 次 / B 一次调用 1 次 / **C 嵌套调用仍 1 次，而业务真跑了 3 个方法** / D 代理套代理 2 次 /
E 同接口同类、每 `newProxyInstance` 只换 handler / F `Object` 三方法也进 handler / G 强转 CCE /
H 非 public 接口 → 代理类落到接口所在包）
+ `day1/ProxyIdentity`（S1 手工 `new` 不进切面 / S2 `jdk.proxy2.$Proxy22` 且实现 `SpringProxy` /
**S2b 按实现类取 Bean 是「两个不同的错」**：按类型 `NoSuchBeanDefinition`、按名字+类型 `BeanNotOfRequiredType` /
S3 `getThis()` 是代理、`getTarget()` 是真身 / S4 `proxyTargetClass=true` → CGLIB 子类 /
**S5 `final` 方法上行切面静默失效**（`before` 增量 0）/ S6 切面自己不被代理 /
S7 顺序 `@Around 进入 → @Before → @After → @Around 返回`）
+ `day1/SelfInvocation`（`this` **1** 次 / `self` **2** 次 / `currentProxy()` **2** 次；
**S4 用 `TargetSource` 证明 `self` 是三层**：`@Lazy` 解析代理 → AOP 代理 → 真身）。
产出 `day1-result.txt`（189 行）+ `day1-spring-source.txt`（8 段源码，行号与输出对撞）。

三条核心结论：① **AOP 改的是「容器里放谁」**，替换发生在
`AbstractAutoProxyCreator.wrapIfNecessary:328`（后处理器的 `postProcessAfterInitialization` 阶段），
所以 `new` 出来的对象 `before=0`；② **选型只看一句**
`isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces()`（`DefaultAopProxyFactory:58`）——
无接口自动 CGLIB，而 CGLIB 覆写不了 `final`，于是 **`final` 上的切面静默不执行、日志一个字不提**；
③ **`this.xxx()` 失效不需要 Spring 就能证明**（纯 JDK 代理里业务跑 3 个方法、handler 只进 1 次），
三种修法的代价分别是「三层代理」「`exposeProxy` + 同线程」「改结构」。

下一个时间盒（60 分钟）：**W06 Day 2 · 切点与通知的全谱** ——
① 四种切点对照（`execution` / `within` / `args` / `@annotation`），重点看「写宽了」和「写窄了」各是什么表现；
② `@AfterReturning` 改返回值、`@AfterThrowing` 吞异常，分别试「改得动吗」「吞得掉吗」；
③ 两个切面的 `@Order`：谁在外层；
④ Day 3 是 `java-48week-plan.md` 第 6 周的硬要求 —— **手写 mini AOP**（`ProxyFactory` + `MethodInterceptor` + `Advisor`，不用 `@Aspect`）。
时间盒见 `00-plan/today.md`。
