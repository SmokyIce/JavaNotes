---
title: 今天做什么
date: 2026-11-23
weekday: 周一
milestone: M2 · Spring 内核
task: W05 Day 3 — Bean 作用域（四条判据 + request 报错 + proxyMode + 自定义 Scope）
status: 已完成
---

# 今天 · 2026-11-23（周一）· 已完成

> 前两个时间盒回答的是「造几个」和「造完做什么」，今天回答第三个：
> **同一份定义，能不能造出「每个请求一个」「每个线程一个」这种生命周期的对象。**
> 一句话剧透：`singleton` / `prototype` 是写死的两个 `if`，其余全是查表 —— 表里没有就报错。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w05（37 文件 / 45 class）+ 跑五个入口 | ✅ 全 exit=0，行数 61 / 129 / 156 / 56 / 77，无回归、无中断代码 |
| `01-notes` 里的未勾项 | W05 只剩「Bean 作用域」（= 今天）与「Spring 到底在帮你管理什么」（留 Day 6 收尾）；W06 起未解锁 |
| `02-code` 空目录 | `w05-` 已开工；`w06-` 起仍是 `.gitkeep`（M2 后续，不是遗留） |
| Day 2 留的两条待办 | ① `pom.xml` → 仍推到 W05 收尾（本机没 Maven）；② 三个追问 → 已在 Day 2 答完 |
| 与上一个时间盒的关系 | 今天上午刚做完 Day 2（同一台机器、同一份 `today.md`），本次是**同一天的第二个 60 分钟**，不涉及补课 |

**结论：没有需要补的中断代码。** 起点就是 W05 Day 3。

## 二、今天的正题：两个程序 + 一个探针

代码在 `02-code/stage1-basics/w05-spring-ioc-di/day3/`：

| 文件 | 内容 |
| :--- | :--- |
| `ScopeMatrix.java` | A `singleton` vs `prototype` 三条判据 / B `thread` 作用域（含「覆盖 singleton 会被拒」）/ C 四作用域判据矩阵 |
| `WebScopeTrap.java` | A `request` 报错原文与栈 / B `session` 同款 / C `proxyMode` 三组对照 / D 自定义 `Scope` |
| `ScopeProbe.java` | 探针 Bean：构造数 / `@PreDestroy` 数 / `destroy` 数，按角色分开记 |

产出的两个文件：`day3-result.txt`（154 行）、`day3-spring-source.txt`（7 段源码，行号与栈对撞）。

## 三、今天挖到的五个点

1. **作用域 = 「造几个」+「谁管埋」+「什么时候解析」。**
   前两个由 `scope` 决定，第三个由 `proxyMode` 决定。四条判据一次列清：
   `singleton` 两次同一个 / 进池 / 销毁 1+1；`prototype` 两次不同 / 不进池 / 销毁 0+0；
   `request` `session` 在非 Web 容器里根本取不到。

2. **`singleton` / `prototype` 不在作用域表里。**
   `registerScope("singleton", …)` → `IllegalArgumentException: Cannot replace existing scopes
   'singleton' and 'prototype'`（`AbstractBeanFactory:1045`）；`getRegisteredScopeNames()` 只有自己登记的 `[thread]`。
   它们写在 `doGetBean` 的两个 `if` 里，第三个 `else` 才查表。

3. **`request` 的报错是「查表查不到」，而且 `refresh()` 不报、`getBean` 才报。**
   `No Scope registered for scope name 'request'`（`doGetBean:369`）；
   启动不报是因为它不是单例，`preInstantiateSingletons` 的循环压根不碰它。

4. **`proxyMode` 不是「每次新对象」，是「把解析推迟到调用时」。**
   单例里注入原型：不加代理三次 `id()` 全是 `1`（只造 1 个）；加了 `TARGET_CLASS` 是 `2 / 3 / 4`（造 3 个）。
   实现是注册期把一个定义拆两个（`scopedTarget.requestProxy` + 外层的 `ScopedProxyFactoryBean`）。
   拿 `request` 试就露出真相：`getBean` 不抛了，第一次 `id()` 抛同一个 ISE。

5. **自定义 `Scope` 证明作用域是插件；`SimpleThreadScope` 证明「埋」也得插件自己干。**
   `every-two` 作用域：4 次 `getBean` → `A A B B`，轮换时销毁 1 次，`close()` 后仍是 1 次
   （容器 `close()` 只销毁单例）。Spring 自带的 `SimpleThreadScope` 连销毁回调都不支持，直接打 WARN。

## 四、今天踩的坑（五个，四个是「先说后验」）

- **`@Scope` 注解和 `Scope` 接口同名。** 同一个文件里 `import …annotation.Scope` 之后
  `implements Scope` 直接编译失败（「对 `Scope` 的引用不明确」）。接口写全限定名才干净。
- **`SimpleThreadScope.SCOPE_NAME` 根本不存在。** 我按 `ConfigurableBeanFactory.SCOPE_SINGLETON`
  的习惯想当然 —— 第三次栽在「按上一个类的 API 猜这一个类」。
- **行号又错了一次：`AbstractBeanFactory:1942` 是方法声明，`!isPrototype()` 在 1944。**
  Day 2 刚在 154 vs 157 上栽过，同一个病。
- **第一版写「拆出来的两个定义 class 都为空」**，实跑发现外层是 `ScopedProxyFactoryBean`；
  只有真身（`@Bean` 定义）才没有 class、只有 `factoryMethod`。
- **文案写「容器 `close()` 时会通知 scope」**，实测销毁计数不变 —— `close()` 只销毁单例。

## 五、环境备忘

- 跑今天：`./run.sh day3.ScopeMatrix`（约 0.5 s）/ `./run.sh day3.WebScopeTrap`（约 0.4 s）
- day3 的入口同样**必须写全包名**（`run.sh` 短名分支只认 `day1`）
- 没有新增 jar：CGLIB 在 `spring-core` 里，`ScopedProxyMode` / `SimpleThreadScope` 都在已有的 jar 内
- `ScopeMatrix` B 段会打两条 JUL WARN（`SimpleThreadScope does not support destruction callbacks`），
  这是**证据不是噪音**，快照里保留原样

---

# 下一个时间盒（60 分钟）

**W05 Day 4：注入方式的取舍 —— `@Autowired` / `@Resource` / 构造器，以及「多个候选」怎么收场**

> 作用域解决的是「给几个」，注入解决的是「给哪个」。同一个接口有两个实现时，
> Spring 不会猜，它会抛 `NoUniqueBeanDefinitionException` —— 今天要把这个错**打出来**，
> 再看 `@Primary` / `@Qualifier` / 集合注入三种收场方式。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 3：作用域三问 + `request` 为什么取不到 + `proxyMode` 推迟了什么 | 口头过 |
| 20 分钟 | 同一个接口两个实现，分别用 `@Autowired`（byType）/ `@Resource`（byName）/ `@Qualifier` / `@Primary` 注入，把四个结果打出来 | `day4/InjectionChoice.java` |
| 15 分钟 | 集合注入：`List<PayChannel>` / `Map<String, PayChannel>`（key = beanName）+ `@Order` 决定顺序 | 同文件 B 段 |
| 10 分钟 | 构造器注入的两个细节：单构造器免注解 / 多构造器必须标一个；`ObjectProvider` 延迟取 | 同文件 C 段 |
| 5 分钟 | 输出 `day4-result.txt` + 笔记 Day 4 小节 + 观察 `AutowiredAnnotationBeanPostProcessor` 的栈（接 Day 2） | 笔记 + 快照 |

> 优先做第三个 20 分钟里的「四个结果」。`@Resource` 按名字、`@Autowired` 按类型 ——
> 这句话谁都会背，但**两种注解在「名字对不上」时的报错完全不同**，那个差异才是面试的落点。
