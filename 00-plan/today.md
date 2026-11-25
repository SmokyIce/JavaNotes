---
title: 今天做什么
date: 2026-11-25
weekday: 周三
milestone: M2 · Spring 内核
task: W05 Day 5 — 收尾封板（三段答案 → 23 条断言 + pom.xml 结账）
status: 已完成
---

# 今天 · 2026-11-25（周三）· 已完成

> 前四天回答了四个问题：**造什么**（Day 1）、**造完做什么**（Day 2）、**造几个**（Day 3）、**给哪个**（Day 4）。
> 今天不提问、不写新 Demo，只做一件事：把这四个答案**变成能跑的程序**，然后封板。
>
> 理由：前四天的结论都是「跑一段、用眼睛看输出」。眼睛会漏（Day 4 就把「零候选给空集合」看错了），
> 数字也会过期（换个 Spring 小版本就不一定成立）。所以今天反过来 —— **先写期望值，再让程序说对不上**。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w05（48 文件 / 89 class）+ 跑全部 10 个入口 | ✅ 全 exit=0，行数 59 / 127 / 154 / 54 / 75 / 51 / 92 / 57 / 34 / 40，与 11-24 逐个一致，无回归、无中断代码 |
| `01-notes` 里的未勾项 | 只剩 W05 第四格（= 今天这格）+ W06 起未解锁内容 |
| `02-code` 空目录 | 无空目录；`w06-` 起仍是 `.gitkeep`（下一个里程碑，不是遗留） |
| 上一个时间盒留的待办 | ① 第四格答案 ✅ 今天写；② `pom.xml` ✅ 今天写（但明确标「未验证」）；③ 题库抽讲 Q1/Q4/Q8 ✅ 顺带补了 Q10 |
| 与前一个时间盒的关系 | 11-24 → 11-25 隔一天（正常节奏），不涉及补课 |

**结论：没有需要补的中断代码。** 起点就是 W05 Day 5。

## 二、今天做的三件事

| # | 做什么 | 产出 |
| -: | :--- | :--- |
| 1 | **把答案写成程序**：`day5/BoardCheck`，四段共 23 条断言，FAIL 就 `exit=1` | `BoardCheck.java` + `day5-result.txt`（85 行，**23 / 23 通过**） |
| 2 | **两个新观测点**（下面第三节） | 同上 + `day5-spring-source.txt`（4 段，713 / 332 行号对撞） |
| 3 | **`pom.xml` 结账** + 笔记第四格 + 题库 Q10 + 三处同步 | `pom.xml`、W05 笔记、`Spring.md`（9 → 10 题）、`MILESTONES.md` |

`BoardCheck` 的分段与断言数：S1 对象图 3 / S2 生命周期 4 / S3 作用域 7 / S4 判定链 9。
设计上**故意不复用** day1~day4 的探针类 —— 封板自检不能依赖「被测对象自己写对了」。

## 三、今天挖到的两个点（都是前几天只能间接说的）

1. **属性注入的位置：从「夹住」变「钉死」。**
   Day 2 只有两个观测点（构造时 `repo=null` / `@PostConstruct` 时 `repo=jdbc`），精度只到「中间某处」。
   今天给 `LifeProbe` 加一个 **`@Autowired` 的 setter** 打点（setter 是我写的、不是容器回调），实测：

   ```text
   ctor → inject(setter) → bpp.before → @PostConstruct → afterPropertiesSet → initMethod → bpp.after
   ```

   于是 `populateBean` 落在 `ctor` 与 `bpp.before` 之间，对应 `doCreateBean` 的 582 / 619 / 620 三行。
   **方法论**：「没有回调」不等于「位置说不清」，在自己的代码里插观测点就行。

2. **「两种注解报同一个错」只对了内层。**
   内层确实都是 `NoUnique`，但外层包装不同：`@Autowired` 是 `UnsatisfiedDependencyException`（713 行），
   `@Resource` 是 `BeanCreationException` + `Injection of resource dependencies failed`（332 行）。
   根源是继承关系：`NoUnique` 是 `NoSuchBean` 的子类，两处 `catch` 都接得住，谁接住用谁的壳。
   **这条修正了 Day 4 笔记里「完全同一个错」的说法**，README 和题库 Q9 已同步收窄。

## 四、今天踩的坑（两个）

- **静态计数器跨段污染。** S2 段 `@PostConstruct` 里打出 `repo = jdbc#2`（本以为 `#1`）——
  S1 段已经造过 `jdbc#1`，而 `BUILT` 是静态的没归零。改成每段开工先 `set(0)`。
  和「并发 demo 各段互相污染」是同一类病：**静态状态不归零，后一段就替前一段付账。**
- **复制粘贴的函数签名没改。** `failOf(...)` 从 Day 4 抄过来，方法体 `return Throwable`，
  签名还留着 `AnnotationConfigApplicationContext` → 4 个「不兼容的类型」。
  好消息是这次 javac 一次就把两个方向都指出来了。

## 五、环境备忘

- 跑今天：`./run.sh day5.BoardCheck`（约 0.5 s）。**FAIL 会 `exit=1`**，可以直接当回归用例。
- 无新增 jar；`pom.xml` 已写但**本机没有 Maven，未验证**（`python -c` 只验了 XML 格式合法）。
- 跑之前别忘了 `export PATH="/c/Program Files/Java/jdk-17/bin:$PATH"`。

---

# 下一个时间盒（60 分钟）

**W06 Day 1：AOP —— 代理到底是什么时候套上去的**

> W05 停在「Bean 造完、字段灌完」。W06 接着回答下一个问题：
> **方法被调用时，Spring 还能插进来做什么？** —— 这一块正是 `@Transactional` / `@Async` 失效的总根源。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 W05 封板三段（对象图 / 生命周期 / 边界），限时 3 分钟 | 口头过 |
| 15 分钟 | 手写 JDK 动态代理（`InvocationHandler` + `Proxy.newProxyInstance`），数一次调用进了几次 handler | `w06-spring-aop/day1/JdkProxy.java` |
| 20 分钟 | 同一套类交给 Spring `@Aspect`，打印调用前后的对象身份：**`this` 和容器里那个不是同一个** | `day1/ProxyIdentity.java` |
| 10 分钟 | 追问：为什么 `this.xxx()` 不走代理（连到 Day 2 的 `is not eligible for auto-proxying`） | 输出段落 |
| 5 分钟 | 快照 + 源码取证 + 笔记 + `today.md` 覆盖 | 三份文件 |

> 开工先做：`w06-spring-aop/` 从 `.gitkeep` 建工程（照 w05 的 `run.sh` 抄一份，`lib/` 里已有 `spring-aop`，
> 但 **`aspectjweaver` 是新依赖**，`@Aspect` 需要它 —— 先确认能不能从 repo1.maven.org 下到）。
