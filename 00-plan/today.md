---
title: 今天做什么
date: 2026-11-03
weekday: 周二
milestone: M1 · Java 地基
task: W02 Day 6 — List<User> 过滤 → 排序 → 分组 → 统计（W02 封板）
status: 已完成
---

# 今天 · 2026-11-03（周二）· 已完成

> W02 的收尾。今天这一节是整周里**最能直接搬进业务代码**的：
> 过滤、排序、分组统计，写完之后项目里那种「三层 for 循环 + 好几个临时 Map」的代码就能删掉了。
> 开工第一步照旧：**回归编译 + 跑全部入口**。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 全量重编（清空 `out/`）+ 跑 12 个入口（day1×3 / day2×2 / day3×2 / day4×2 / day5×3） | ✅ 全部 exit=0，无回归，**没有中断的代码** |
| `01-notes` 里的未勾项 | 只剩 W02 的「过滤 / 排序 / 分组 / 统计」这一条（今天做掉）+ W03 之后未解锁的里程碑，无孤儿待办 |
| `02-code` 下的 `.gitkeep` 空目录 | `w03-concurrency-basics/` 只有 `.gitkeep`，属于**明天**的开工对象，不是遗留 |
| Day 5 留下的两条遗留 | `sorted` 屏障 / `count` 快速路径已在 Day 5 结论化；`Files.lines()` 持句柄那条按计划留给 W08 |

## 二、今天的正题：`List<User>` 报表

代码在 `02-code/stage1-basics/w02-generics-stream/src/main/java/com/newt/java48/w02/day6/`：

| 文件 | 内容 |
| :--- | :--- |
| `User.java` / `UserData.java` | 统一模型 + 一份 15 条的数据。**故意留三个 null**：`name` 1 条、`age` 2 条、`dept` 1 条 |
| `UserReport.java` | A 过滤（Predicate 组合 / null 字段 NPE / 短路终止）+ B 排序（多级 / `reversed` 位置陷阱 / `nullsLast` / TimSort 稳定） |
| `GroupStats.java` | C 分组：`groupingBy` 一参 / 两参 / 三参，七个下游收集器，多级分组，`partitioningBy` 空组，`summarizingInt` |
| `CollectorTraps.java` | D 六个陷阱：`toMap` 重复 key / null value、`groupingBy` null key、`teeing`、三种 `toList` 的可变性与 null 政策 |
| `day6-result.txt` | 三个入口的完整实测输出（205 行） |

> 那三个 null 不是凑数：`name=null` 让 A 段炸一次，`age=null` 让 B 段和 C 段各炸一次，
> `dept=null` 让 D 段炸一次。**每个 API 的 null 政策都留下了实测证据**，不是背来的。

## 三、今天挖到的五个点

1. **`Predicate` 的 `or().and()` 是左结合，不是布尔优先级。**
   想表达「研发 或 (测试且在职)」，写 `isDev.or(isTest).and(active)` 得到 **6 人**，
   写 `isDev.or(isTest.and(active))` 得到 **7 人**。差的是离职的研发王强：
   前者先算出并集再整体 `&& active`，也就是 `(a||b)&&c`。
   **这类错误不报编译错、不抛异常，只是少几行数据** —— 报表类需求最难发现的一种 bug。
   （我第一版注释就写反了，跑出人数对不上才发现。）

2. **`reversed()` 作用在「它左边整条链」上，`nullsLast` 会被一起反转成 `nullsFirst`。**
   同一个需求两种写法，结果完全不同：
   `comparing(dept, nullsLast(...)).thenComparing(salary).reversed()` → 部门降序、null 跑到最前；
   `comparing(dept, nullsLast(...)).thenComparing(salary, reverseOrder())` → 部门升序、只反转薪资。
   这是线上「排序方向和要求反了」的常见来源。

3. **`groupingBy` 的三层重载是三个问题**：怎么分组 / 每组算什么 / 用什么 Map 装。
   第三个重载的 `mapFactory` **夹在中间**，最容易记错位置。实测同一份数据三种 Map 的 key 顺序：
   `HashMap [产品, 研发, 运营, 测试]` / `LinkedHashMap [研发, 产品, 运营, 测试]` /
   `TreeMap [产品, 测试, 研发, 运营]` —— **一个都不能当业务约定**，要稳定必须显式指定。

4. **统计场景里「补个默认值」是错的。** `averagingInt(User::age)` 遇 null 直接 NPE，
   而且**方法引用版的 message 是空的**（换成 `u -> u.age()` 就有 `Cannot invoke "Integer.intValue()"`）——
   拆箱走 MethodHandle 适配层，没有字节码位置可指。
   但修法不能是「null 当 0」：研发组 null 当 0 得 **25.6**，先 filter 掉得 **32.0**，**差 6.4 岁** ——
   因为 0 进了分母，分母从 4 变成 5。平均值 = 总和 / **有效样本数**，这条最容易被「让代码先跑起来」毁掉。

5. **`toMap` 用的是 `merge` 不是 `put`。** 这就解释了两件看起来矛盾的事：
   `HashMap.put(k, null)` 完全没问题，但 `toMap` 的 value 为 null 直接 NPE 且**没有 message**；
   `groupingBy` 遇 null key 抛 `element cannot be mapped to a null key`，而 `toMap` 对同一份数据
   能成功返回 `{null=50000}`。**同样是「按 key 聚合」，null 政策不一样，只能靠记内部实现。**

> 附带一条今天纠正的错误认知：我原以为「`Stream.toList()` 不可变，所以也不许 null」。
> 实测是 **`Stream.toList()` 放行 null、`toUnmodifiableList()` 拒绝 null**，
> 两者返回的却是同一个 `ImmutableCollections$ListN` —— 差别在入口方法
> （`listFromTrustedArrayNullsAllowed` vs `listFromTrustedArray`）。
> **「不可变」和「不许 null」是两件事**，别捆在一起记。

## 四、今天踩的坑

- **先写结论、后写验证，被抓了一次。** A2 段的注释我先按布尔直觉写了「`a.or(b).and(c)` 等价于
  `a || (b && c)`」，跑到人数对不上才发现是 `(a||b)&&c`。补了一个最小用例
  （用 `"x"` 这种「短且命中」的输入才能区分两种语义）才定论。
  —— 这已经是本周第三次「断言先于实测」了（前两次：`sorted` 一定是屏障、`average` 补默认值无害）。
- **`averagingInt` 的空 message 极难定位。** NPE 没有 message、栈顶在 `Collectors` 内部，
  只能靠「我把哪个字段传进去了」反推。以后看到 `Collectors` 内部的裸 NPE，第一反应查包装类型字段。
- **`joining` 把 null 拼成字面量 `"null"`**，不抛错。测试组那行输出 `吴磊/徐丽/null`，
  一眼看不出是脏数据还是真人叫 null。
- **数据设计要留「同薪同部门」的样本。** 第一版数据里研发组薪资全不同，`thenComparing(age)`
  那一级**从来没被执行过**，等于白写。把何静的薪资改成和张伟一样，年龄兜底才真正跑到。

## 五、环境备忘

- 跑 day6：`run.bat day6.UserReport` / `run.bat day6.GroupStats` / `run.bat day6.CollectorTraps`
- 手动编译照旧先 `find src/main/java -name "*.java" > sources.txt`（今天 18 个文件）
- `-Xlint:all` 会报 6 条警告，都是旧文件里**故意示范**的（rawtypes / serial / finally / try），不用管
- 临时验证小用例记得加 `-encoding UTF-8`，否则中文注释直接编译失败（今天又踩一次）

---

# 下一个时间盒（60 分钟）

**W03 Day 1：线程基础 —— `Thread` / `Runnable` / 为什么会有线程安全问题**

W02 封板，进并发。并发是 M1 里唯一「不实测就必然学错」的一块，
所以第一天只做一件事：**把「共享可变状态」这件事用代码撞出来**，不讲 API 清单。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 W02：擦除留下什么 / `finally` 里的 return / `count()` 为什么跳过 `peek` | 口头过，卡住回看 W02 |
| 15 分钟 | 建 `w03-concurrency-basics/` 工程（照 `w02` 的布局 + `run.bat`）；`Thread` / `Runnable` / `Callable` 三种起线程方式，各跑一次 | `day1/StartThread.java` A 段 |
| 20 分钟 | **竞态复现**：`count++` 两万次 × 10 线程，跑十轮统计「有几次没到 20 万」；再用 `javap -c` 看 `iinc` 与 `putfield` 之间的窗口 | `day1/RaceCondition.java` B 段 |
| 10 分钟 | 三条可见性/原子性各撞一次：`volatile` 能不能救 `count++`（不能）、`synchronized` 能不能（能）、`AtomicInteger` 能不能（能） | 同上 C 段 |
| 5 分钟 | 输出 `day1-result.txt` + 笔记 W03 Day 1 小节 + 勾掉「Thread · Runnable」「为什么会有线程安全问题」 | 笔记 + 快照 |

> 优先做第三个 20 分钟。**并发所有后续知识（`volatile` / `synchronized` / `Lock` / 线程池）
> 都是在解释今天这个「没到 20 万」**。如果第三天讲 `volatile` 时还说不清它解决的是可见性还是原子性，
> 那就是今天这一步没落地。
>
> 顺带把 W02 的两条结论带进并发：`parallel()` 共用 `ForkJoinPool.commonPool()`、
> `ArrayList` 并发 `add` 不安全 —— 这两条正好是「共享可变状态」的第一批受害者。
