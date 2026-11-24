---
title: 今天做什么
date: 2026-11-24
weekday: 周二
milestone: M2 · Spring 内核
task: W05 Day 4 — 注入方式的取舍（判定链四级 + 两种注解的报错 + 集合与零候选）
status: 已完成
---

# 今天 · 2026-11-24（周二）· 已完成

> 前三个时间盒回答「造几个」「造完做什么」「给几个」，今天回答最后一个：
> **同一个接口摆三个实现，容器凭什么挑中某一个 —— 挑不出来时它说什么。**
> 一句话剧透：`@Autowired` 不是纯按类型，`@Resource` 也不是纯按名字；两者的真差别在报错。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w05（40 文件 / 57 class）+ 跑七个入口 | ✅ 全 exit=0（59 / 127 / 154 / 54 / 75 / 51 / 92 行），无回归、无中断代码 |
| `01-notes` 里的未勾项 | W05 只剩「Spring 到底在帮你管理什么」（= W05 收尾那一格）；W06 起未解锁 |
| `02-code` 空目录 | `w06-` 起仍是 `.gitkeep`（M2 后续，不是遗留） |
| 上一个时间盒留的待办 | `pom.xml` 仍推到 W05 收尾（本机没 Maven，写了验不了）；Day 3 的五条踩坑已全部回填 |
| 与上一个时间盒的关系 | 11-23（周一）停在这里，今天接着走同一个时间盒，不涉及补课 |

**结论：没有需要补的中断代码。** 起点就是 W05 Day 4。

## 二、今天的正题：三个程序 + 一个工具类

代码在 `02-code/stage1-basics/w05-spring-ioc-di/day4/`：

| 文件 | 内容 |
| :--- | :--- |
| `InjectionChoice.java` | A 三候选爆炸（报错原文 + 栈）/ B 字段名回退 / C `@Qualifier` / D `@Primary` 压过名字 / E `@Resource` 按名 / F 两种注解名字对不上的报错对照 |
| `CollectionInject.java` | A `List` 按 `@Order`、数组同源 / B `Map` 按注册顺序（key = bean 名）/ C 零候选三段 / D 集合 + `@Qualifier` |
| `CtorInject.java` | A 单构造器免注解 / B 多构造器不标的两种结局 / C 标一个 `@Autowired` / D `ObjectProvider` 三种局面 / E 参数名回退要 `-parameters` |
| `pay/` | `PayChannel` 接口 + 三个 `@Order` 不同的实现（bean 名可预测） |
| `support/Quiet.java` | 压掉 `refresh` 失败的 JUL 噪音（**logger 名是枚举出来的，不是猜的**） |

产出两个文件：`day4-result.txt`（145 行）、`day4-spring-source.txt`（8 段源码，行号与栈对撞）。

## 三、今天挖到的六个点

1. **挑人是一条四级链，顺序写死在源码里。**
   `@Qualifier` 在筛候选那一步就生效 → `@Primary`（1635）→ `@Priority`（1639）→ 名字匹配（1642）→
   都不行才 `resolveNotUnique`（1369）。`@Primary` 在名字**之前**，所以字段名对得上也会被压过。

2. **`@Autowired` 不是纯按类型：第三级回退拿字段名当 bean 名。**
   三个候选、字段名叫 `wechatChannel`、零注解 → 注入成功拿到 `wechat`。教科书只讲前一半。

3. **两种注解的真差别在报错。**
   显式 `@Resource(name="channel")` 对不上 → `No bean named 'channel' available`（**连类型都不查**）；
   默认名对不上 → 退回按类型 → 和 `@Autowired` 一模一样的 `NoUniqueBeanDefinitionException`。

4. **`Map` 注入**不**按 `@Order` 排序。**
   同一个容器：`List` = `wechat → alipay → unionpay`（按 order），`Map` key = `[alipayChannel, wechatChannel, unionPayChannel]`
   （注册顺序）。源码里 Map 分支一行排序都没有。**按顺序跑策略只能用 `List`。**

5. **集合没有特权 —— 「零候选给空集合」是讹传（今天唯一被推翻的常识）。**
   `required=true` 的 `List` 照样抛 `expected at least 1 bean`；`required=false` 给的是 `null`。
   原因：`resolveMultipleBeans` 在候选为空时 `return null`，等于「我没处理」，掉回单值分支照抛。

6. **`ObjectProvider` 把「取」推迟到调用时，三种局面一次收干净。**
   原型两次 `getObject()` = `temp-1 / temp-2`；零候选 `getIfAvailable()` / `getIfUnique()` 都是 null；
   三候选 `getIfUnique()` 给 null 但 `getIfAvailable()` **抛** `NoUnique` —— 名字差一个词，行为差一个异常。

## 四、今天踩的坑（四个）

- **「零候选时集合注入给空集合」我先写成文案才跑**，三段实测全不对。
  `required` 才是决定因素，不是「类型是不是集合」。这是本月第 N 次「断言先于实测」。
- **注入失败死在 `refresh()` 不是 `getBean`。** 第一版把 `try/catch` 包在 `getBean` 上，
  程序直接 `Exception in thread "main"` 退出 —— 那个 bean 不是 lazy、没人依赖它，预实例化循环自己就造了。
- **JUL logger 名猜错。** 按父类 `AbstractApplicationContext` 设 `Level.OFF` 压不掉；
  真名是子类 `AnnotationConfigApplicationContext`（字段是 `LogFactory.getLog(getClass())`）。
  最后靠枚举 `LogManager.getLoggerNames()` 才查清。
- **只打 root cause 会丢掉最有用的那层。** `No default constructor found` 在中间的
  `BeanInstantiationException` 上，只打根因只剩一句 `NoSuchMethodException: …<init>()`。

## 五、环境备忘

- 跑今天：`./run.sh day4.InjectionChoice` / `day4.CollectionInject` / `day4.CtorInject`（各约 0.3~0.6 s）
- 入口同样必须写全包名（`run.sh` 短名分支只认 `day1`）
- 无新增 jar；Day 4 起快照里不再有 JUL 警告（`support/Quiet` 压掉了，Day 2/3 的 WARN 仍保留为证据）

---

# 下一个时间盒（60 分钟）

**W05 Day 5：收尾封板 —— 把「Spring 到底在帮你管理什么」那半张答案补全**

> 四天学了「造什么、造完做什么、造几个、给哪个」。这四件事合起来才是「Spring 管什么」的完整答案。
> 今天不写新 Demo，只做三件事：**把答案写成能脱稿讲的三段**、把 `pom.xml` 这笔账结掉、然后把 W05 封板。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 4：四级判定链 + 两种注解的报错差别 + 集合为什么不是兜底 | 口头过 |
| 25 分钟 | 写第四格答案：**对象图 / 生命周期 / 边界** 三段，每段配一个本周的实测数字 | 笔记「一句话总结」+ 参考要点 Q10 |
| 10 分钟 | `pom.xml` 处置：本机没 Maven → 写出 `pom.xml` 但**不声称验证过**，或明确记为「装 Maven 后再补」 | 决定 + 一行记录 |
| 10 分钟 | 题库回顾：`Spring.md` 9 道题抽 3 道脱稿讲（Q1 / Q4 / Q8） | 口头过 |
| 5 分钟 | `MILESTONES.md` 把 W05 标 🟩、W06 AOP 置为「下一个」，`today.md` 覆盖 | 三份文件同步 |

> 判定「可以封板」的标准（照 M0/M1 的惯例）：四个知识点全勾、四个可运行工程（其实是四天实验同一工程）、
> 题库 9 道全部有「我的回答 + 追问预判」、踩坑表里每条都有「原因 + 证据」。
> 现在的状态：前三条已满足，只差第四格的答案。
