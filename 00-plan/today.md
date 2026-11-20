---
title: 今天做什么
date: 2026-11-20
weekday: 周五
milestone: M2 · Spring 内核
task: W05 Day 1 — Spring IOC / DI（手写装配 vs 容器装配）
status: 已完成
---

# 今天 · 2026-11-20（周五）· 已完成

> M1 封板，进 M2。第一件事不是讲「控制反转」，是**先写一遍没有 Spring 的版本**，
> 再把它交给容器 —— 「Spring 帮你管理什么」这个问题，只有靠 diff 才回答得了。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w04（6 文件）+ 跑六个入口 | ✅ 全 exit=0，行数 45/57/35/40/43/80，与 11-19 一致 —— 无回归、无中断代码 |
| `01-notes` 里的未勾项 | W05 剩 3 格（生命周期 / 作用域 / Spring 帮你管理什么）+ W06 起的未解锁内容；**没有该做没做的** |
| `02-code` 空目录 | `w05-` 起的 `.gitkeep`（今天开工的就是它）+ `lab/` 下 7 个实验目录（都是 M2 及以后） |
| 11-19 留的两条待办 | ① `pom.xml` → 按计划推到 W05 收尾（本机没 Maven，写了也验证不了）；② 手工 jar 清单 → 今天落地成 `lib/` 六个 jar |

**结论：没有需要补的中断代码。** 今天的起点就是 W05 Day 1。

## 二、今天的正题：一个新工程 + 三个程序

代码在 `02-code/stage1-basics/w05-spring-ioc-di/`（从 `.gitkeep` 建起）：

| 文件 | 内容 |
| :--- | :--- |
| `lib/*.jar` | Spring 5.3.39 六个 jar（core / beans / context / expression / aop / **spring-jcl**） |
| `run.sh` / `run.bat` | `javac -cp "lib/*"` + `java -cp "out;lib/*"`（Windows 分隔符是 `;`） |
| `day1/ManualWiring.java` | A 三个入口各自 `new`（3 份仓储）/ B 集中装配（1 份）/ C 数源码算改动点 |
| `day1/SpringWiring.java` | A 谁+何时 new / B 单例 / C 装配行数 / D 换实现 / E `@Lazy` / F prototype |
| `day1/ContainerQuestions.java` | Q1 不写注解 / Q2 注解对 `new` 无效 / Q3 循环依赖 / Q4 单例池 |
| `day1/app/` | 同一套业务类：Controller / Job / Test / Service / Repository / Calculator |
| `day1/cycle/` | FieldA⇄FieldB（字段注入）、CtorA⇄CtorB（构造器注入） |
| `support/Trace.java`、`support/Src.java` | 打点脚手架（谁 new 的、数源码 new 了几次） |
| `day1-result.txt` | 三段实验完整实测（355 行） |
| `day1-spring-source.txt` | 6 段 Spring 源码，行号能和栈对撞 |

## 三、今天挖到的五个点

1. **「换实现要改几处」是能数出来的：3 处 → 0 处。**
   A 段三个入口各自 `new`：`new JdbcOrderRepository` 3 次、`new PriceCalculator` 3 次；
   Spring 版入口代码里全是 **0**。唯一的 `new` 落在 `MemoryConfig` 的 `@Bean` 里（1 次）。
   **变的是「一个装配点」，不是「零个 new」。**

2. **手写版的代价不是「代码丑」，是三个连接池。**
   A 段造出 **3 个** `JdbcOrderRepository`（哈希三个都不同）；B 段收进 `main` 之后是 **1 个**。
   这一条比「改 3 处」值钱 —— 它会体现在连接数和缓存命中率上。

3. **「谁 new 的、什么时候 new 的」都能从栈上读出来。**
   手写版直接调用者是 `ManualWiring.webEntry:28`；Spring 版是 `BeanUtils.instantiateClass:213`，
   再往上命中 `createBeanInstance` / `preInstantiateSingletons:955` / `getSingleton:234`。
   时间线：第一个 Bean `+312 ms`、最后一个 `+338 ms`、「容器创建完成」`+354 ms` ——
   **全在 refresh 内**，而且那 312 ms 是类加载 + 扫包，不是 `new`。

4. **两个行号说明造对象有两条路：1222 与 1232。**
   无参构造走 `instantiateBean`（1232）；有构造器参数走 `autowireConstructor`（1222）。
   同一份输出里两种都出现了 —— 这就是「单构造器不用写 `@Autowired`」的机制来源。

5. **循环依赖的差别就是源码三行的顺序。**
   `createBeanInstance`(582) → `addSingletonFactory`(613) → `populateBean`(619)。
   字段注入：先造空壳、登记、再灌 → 能过；构造器注入：参数先于对象 → 必炸
   （`BeanCurrentlyInCreationException`）。

## 四、今天踩的坑（四个）

- **自指统计**：`Src` 第一版把针脚 `"new JdbcOrderRepository("` 当字符串写在被数的文件里，
  于是 `SpringWiring`（0 个 new）数出 3。改成 `countNew(src, "JdbcOrderRepository")`，
  针脚在 `Src` 内部拼 —— **针脚必须藏在被数文件之外**。
- **`GenericApplicationContext` 只能 `refresh()` 一次**：先 `new ...(AppConfig.class)` 再
  `register + refresh` → `IllegalStateException: does not support multiple refresh attempts`。
- **`getBeanDefinitionNames()` + `getBean(name)` 会把 `@Lazy` 提前造出来**：
  第一版 A 段这么写，LazyBean 在枚举名单时就构造了，E 段「refresh 时没造」直接不成立。
  改成读 `getBeanDefinition(name).getBeanClassName()`。
- **`getSingleton:234` 不是查三级缓存那一支**：234 行是 `singletonFactory.getObject()`
  （真正开始造对象），查三级缓存的是 180 行起的另一个重载。同名重载，看栈必须对行号。

## 五、环境备忘

- 跑今天：`./run.sh ManualWiring`（< 1 s）/ `./run.sh SpringWiring`（约 0.4 s）/ `./run.sh ContainerQuestions`（约 0.5 s）
- **Spring 5.x 用自带 `spring-jcl`，不是 `commons-logging`**；照老教程下前者会多一个 jar（也能跑）
- 第一次跑 Spring 有约 300 ms 的类加载开销，别把它算进「容器启动慢」
- `@Configuration` 会被 CGLIB 代理（输出里 `AppConfig$$EnhancerBySpringCGLIB$$...`），CGLIB 在 `spring-core` 里，不用额外 jar

---

# 下一个时间盒（60 分钟）

**W05 Day 2：Bean 生命周期** —— 代码放同一个工程 `day1/` 旁边，新建 `day2/`

> 昨天证明了「对象是容器造的」，今天要回答「造完之后又对它做了什么」。
> 一句话剧透：**一定是 `构造 → 属性注入 → BeanPostProcessor 前 → 初始化 → BeanPostProcessor 后` 这个顺序**，
> 但顺序背下来没用 —— 要靠四组打点把它**打出来**。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 1：手写 3 处 / Spring 0 处、Bean 在 refresh 里造、字段注入为什么能解环 | 口头过 |
| 20 分钟 | 一个 Bean 上同时挂 `@PostConstruct` / `InitializingBean.afterPropertiesSet` / 自定义 `initMethod` / `@PreDestroy` / `DisposableBean` / `destroyMethod`，**六个回调打时间戳** | `day2/LifecycleOrder.java` |
| 20 分钟 | 两个 `BeanPostProcessor`：一个 `postProcessBeforeInitialization` 一个 `After`，打印时把 beanName 和「当前是第几次」都带上 —— 看它对**每个** Bean 都跑一遍 | `day2/PostProcessorTrace.java` |
| 5 分钟 | 三个追问：`@PostConstruct` 是谁调的（哪个后处理器）/ `prototype` 的销毁回调会不会被调 / `BeanPostProcessor` 自己会不会被后处理 | 输出段落 |
| 5 分钟 | 输出 `day2-result.txt` + 笔记 Day 2 小节 + 勾掉「Bean 生命周期」 | 笔记 + 快照 |

> 优先做第二个 20 分钟。**「初始化回调四种写法的执行顺序」是面试高频，
> 而「`BeanPostProcessor` 对所有 Bean 都跑一遍」是理解 AOP 的入口** ——
> 代理就是在 `postProcessAfterInitialization` 里被换掉的。
