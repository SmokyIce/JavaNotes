---
title: 今天做什么
date: 2026-11-23
weekday: 周一
milestone: M2 · Spring 内核
task: W05 Day 2 — Bean 生命周期（六回调 + BeanPostProcessor）
status: 已完成
---

# 今天 · 2026-11-23（周一）· 已完成

> 昨天证明了「对象是容器造的」，今天要回答「造完之后又对它做了什么」。
> 顺序背下来没有用 —— 要靠打点把它**打出来**，再靠栈把「谁调的」钉死。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w05（21 文件 / 24 class）+ 跑三个入口 | ✅ 全 exit=0，行数 59 / 127 / 154，与 11-20 的 355 行快照一致 —— 无回归、无中断代码 |
| `01-notes` 里的未勾项 | W05 剩「Bean 生命周期 / 作用域 / Spring 帮你管理什么」；第二格是今天的任务，另两格未到 |
| `02-code` 空目录 | `w05-` 已开工；`w06-` 起仍是 `.gitkeep`（M2 后续，不是遗留） |
| 11-20 留的两条待办 | ① `pom.xml` → 仍按计划推到 W05 收尾（本机没 Maven，写了验证不了）；② 「Bean 生命周期」 → **今天做完** |
| 11-20 与今天之间断了 3 天（11-21~11-22 周末） | 按里程碑制规则：不补课，直接从「下一个时间盒」接着走 |

**结论：没有需要补的中断代码。** 今天的起点就是 W05 Day 2。

## 二、今天的正题：两个程序 + 一个新 jar

代码在 `02-code/stage1-basics/w05-spring-ioc-di/day2/`（14 个文件）：

| 文件 | 内容 |
| :--- | :--- |
| `LifecycleOrder.java` | A 六回调 + 两个钩子的时间线 / B 逆序销毁 / C prototype / D 推断 `close()` |
| `LifecycleBean.java` | 一个类上挂三种初始化 + 三种销毁，每个回调打时间戳 |
| `LifecycleBpp.java` | 把 ② 与 ⑥ 两个钩子插进序列，只对 `lifecycleBean` 打 |
| `LifecycleConfig.java` | `@Bean(initMethod, destroyMethod)`；后处理器用 `static` 方法声明 |
| `LifecycleRepo.java` | 属性注入的观测点：构造时 null、初始化时有值 |
| `Alpha` / `Beta` | 依赖关系 → 销毁顺序 |
| `PrototypeBean.java` | 销毁回调计数（恒 0） |
| `AutoCloseBean` / `ManualOffBean` | 推断 `close()` 的开关对照 |
| `PostProcessorTrace.java` | A 计数表 / B `@PostConstruct` 栈 / C 三追问 / D `Ordered` |
| `TraceBpp.java` | 会计数的后处理器，自己带 `@PostConstruct` 做对照 |
| `EarlyDependent.java` | 被后处理器 `@Autowired` → 提前创建 → 拿不到代理 |
| `StackDumpBean.java` | 在 `@PostConstruct` 里打印栈 |
| `support/Stacks.java` | 栈过滤脚手架（只留 spring 与 com.newt 的帧） |
| `lib/javax.annotation-api-1.3.2.jar` | 第 7 个 jar，JDK 17 上没有 `@PostConstruct` |

## 三、今天挖到的五个点

1. **六回调的真实顺序（实测时间戳）：**
   `①构造 +375.7 → ②BPP.before +386.7 → ③@PostConstruct +389.0 → ④afterPropertiesSet +389.5
   → ⑤initMethod +390.2 → ⑥BPP.after +390.5 →（refresh 返回 +408.7）→ ⑦@PreDestroy +411.4
   → ⑧destroy +411.8 → ⑨destroyMethod +412.0 ms`。
   **② 在 ③ 之前** —— 这条说明 `@PostConstruct` 不是容器主流程调的。

2. **`@PostConstruct` 的执行者是后处理器，栈上有三个行号。**
   `InitDestroyAnnotationBeanPostProcessor:157` ← `applyBeanPostProcessorsBeforeInitialization:440`
   ← `initializeBean:1796`。具体载体是 `CommonAnnotationBeanPostProcessor:205`（`setInitAnnotationType`）。

3. **属性注入没有回调，只能用两个点夹住。** 构造器里 `repo=null`、`@PostConstruct` 里 `repo=jdbc`。
   「初始化」这个名字有前提：**初始化的是已经装配好的对象**。

4. **prototype 的销毁回调一次都不会被调，而且不是「忘了调」。**
   `AbstractBeanFactory.registerDisposableBeanIfNecessary:1942` 第一行就是 `if (!mbd.isPrototype() && ...)` ——
   登记阶段就短路，销毁阶段自然找不到它。

5. **后处理器只看得见「注册完成之后」出生的 Bean，同批的还互相看不见。**
   计数表：正常 Bean 2/2；`bppFirst` / `bppSecond` / `earlyDependent` 全 0/0。
   原因在 `PostProcessorRegistrationDelegate:257~265`：先全部 `getBean` 收集，再统一注册。
   被提前创建的那个还会收到官方告警 `not eligible for auto-proxying` ——
   **= 拿不到代理 = `@Transactional` / `@Async` 白写**。

## 四、今天踩的坑（三个）

- **`bppSecond` 应该是 1/1？实测 0/0。** 我按「注册完第一个就会处理第二个」理所当然地写了文案，
  被表格数字打回来。真正的机制是「先收集、后注册」，**同批全瞎**。
  教训：**「顺序」在 BPP 上要分「阶段」和「批内」两层**，别拿一条规则套所有情况。
- **栈上 `postProcessBeforeInitialization:154` 对不上。** 154 是方法声明，157 才是
  `metadata.invokeInitMethods`。栈帧行号指向**调用点**。引行号前先跑一遍。
- **`@PostConstruct` 在 JDK 17 上编译不过**（`程序包 javax.annotation 不存在`）。
  JDK 11+ 已把它移出 JDK，必须补 `javax.annotation-api`；Spring 5 用 `javax.*`、Spring 6 才换 `jakarta.*`。
  更阴的是：**没有这个 jar 时不报错，`@PostConstruct` 只是不执行**。

## 五、环境备忘

- 跑今天：`./run.sh day2.LifecycleOrder`（约 0.4 s）/ `./run.sh day2.PostProcessorTrace`（约 0.3 s）
- **day2 的入口必须写全包名**：`run.sh` 的短名分支只认 `day1`
- 时间戳每次跑抖 ±30 ms，文档里的数字是本次快照，改代码后要重新生成 `day2-result.txt` 再抄
- A 段容器启动仍是 ~375 ms（类加载 + 扫包），不是 `new` 慢

---

# 下一个时间盒（60 分钟）

**W05 Day 3：Bean 作用域** —— 代码放 `day3/`

> 前两天回答的是「造几个」和「造完做什么」，今天回答「**同一份定义，能造出几种生命周期的对象**」。
> 一句话剧透：`singleton` 是容器里一个，`prototype` 是每次一个，`request` / `session` 是**每个请求/会话一个**，
> 而它们只有在 Web 容器里才活得下去 —— 在 `AnnotationConfigApplicationContext` 里直接报错。
> 这不是背结论，是要**看到那个错**。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 2：六回调顺序 + `@PostConstruct` 谁调的 + prototype 为什么不销毁 | 口头过 |
| 20 分钟 | 四种作用域的对照表：`getBean` 两次是否同一个 / 是否进单例池 / 销毁回调会不会被调（复用 `PrototypeBean` 的计数思路） | `day3/ScopeMatrix.java` |
| 20 分钟 | `request` / `session` 在非 Web 容器里的报错原文（`No Scope registered for scope name 'request'`）+ `@Scope(proxyMode)` 为什么存在 | `day3/WebScopeTrap.java` |
| 5 分钟 | 自定义作用域：`Scope` 接口三方法 + `registerScope`，实现一个「每 2 次 getBean 换一个实例」的怪作用域 —— 证明作用域是**插件化**的 | 同文件 C 段 |
| 5 分钟 | 输出 `day3-result.txt` + 笔记 Day 3 小节 + 勾掉「Bean 作用域」 | 笔记 + 快照 |

> 优先做第三个 20 分钟。**「`request` 作用域在非 Web 环境报什么错」和「`proxyMode` 解决什么问题」**
> 是面试里区分「用过 Spring」和「读过 Spring」的地方 —— 后者答不上来通常是没踩过那个错。
