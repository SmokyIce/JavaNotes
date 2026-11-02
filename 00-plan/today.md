---
title: 今天做什么
date: 2026-11-02
weekday: 周一
milestone: M1 · Java 地基
task: W02 Day 5 — Stream（惰性求值 + 短路 + 屏障 + 流只能用一次）
status: 已完成
---

# 今天 · 2026-11-02（周一）· 已完成

> 上午跑完 Day 4，接着把 Day 5 也做掉：Stream 是 W02 里唯一「写业务代码天天用、
> 但没人说得清它什么时候执行」的一块，正好一次讲透。
> 开工第一步照旧：**回归编译 + 跑全部入口**。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 全量重编（清空 `out/`）+ 跑 9 个入口（day1×3 / day2×2 / day3×2 / day4×2，`ExceptionCost` 单独两遍参数） | ✅ 全部 exit=0，无回归 |
| `01-notes` 里的未勾项 | 只剩 W02 的 Day 6 练习 + W03 之后未解锁的里程碑，无孤儿待办 |
| `02-code` 下的 `.gitkeep` 空目录 | 全是 W03–W48 尚未解锁的，不动 |
| Day 4 留下的那条 `@FunctionalInterface` 遗留 | ✅ 今天补完，见 `day5/FuncInterfaceRule` |

## 二、今天的正题：Stream

代码在 `02-code/stage1-basics/w02-generics-stream/src/main/java/com/newt/java48/w02/day5/`：

| 文件 | 内容 |
| :--- | :--- |
| `StreamLazy.java` | 四段：惰性求值（打印顺序）/ 短路（无限流 + 探测计数）/ 屏障（sorted、distinct + 100 万条计时）/ `peek` 与 `count` |
| `StreamOnce.java` | 四段：流只能用一次（四种场景）/ `map` vs `flatMap` / parallel 的顺序与代价 / 何时不用 Stream |
| `FuncInterfaceRule.java` | 四段：抽象方法怎么数（反射 4 个 vs 编译器 1 个）—— 补 Day 4 遗留 |
| `day5-result.txt` | 三个入口的完整实测输出 |

## 三、今天挖到的四个点

1. **「中间操作 / 终止操作」不是知识点，是排查工具。**
   不接终止操作时 `filter` 一行都不执行（日志 0 条）；接上之后是
   `filter a → map a → filter b → map b` —— **一个元素走完全链才轮到下一个**，
   不是「先全部 filter 再全部 map」。判断方法不用背表：**看返回值是不是 `Stream`**。
   线上看到「filter 没生效」，按 ① 缺终止操作 → ② 被屏障挡住 → ③ `count()` 走快速路径 三步走。

2. **`sorted` 不一定吃掉全部 —— 上游已有序时它是空操作。**
   本来想演示「屏障毁掉短路」，结果 `rangeClosed(1,5).sorted().findFirst()` 只探测了 **1 个**，
   跟不加 `sorted` 一样。翻源码才明白：`IntStream.rangeClosed` 的 spliterator 自带 `SORTED` 特征
   （`Streams.java:119`），而 `SortedOps.opWrapSink` 第一句就是
   `if (StreamOpFlag.SORTED.isKnown(flags) && isNaturalSort) return sink;`（`SortedOps.java:136`）——
   节点直接被摘掉。换成 `IntStream.of(5,3,1,4,2)` 立刻吃满 5 个。
   **「`sorted` 一定贵」是错的，结论取决于上游特征。**

3. **`count()` 会把整条管道（连 `peek`）一起跳过。**
   `Stream.of("a","b","c").peek(log).count()` 里 `peek` 执行了 **0 次**；
   前面加一个 `filter(x -> true)` 立刻变 **3 次**，而 `count()` 结果都是 3。
   原因：JDK 9+ 的 `count()` 在源是 `SIZED`、中间没有改数量的操作时能直接算出个数。
   结论很硬：**`peek` 只能调试，绝不能放业务逻辑** —— 它会静默不执行，连日志都不留。

4. **`parallel` 不是「更快」，是「用核数换单次耗时」。**
   12 核实测：2000 万求和 20 → 6 ms（3.3 倍，被内存带宽卡住，拿不到 12 倍）；
   1000 个元素 × 30 轮计算 21 → **49 µs（反向慢 2~5 倍）**；200 万 × 30 轮 34 → 5 ms。
   小数据量下拆任务 + 合并结果的开销比干活本身还大。
   另外 `parallel().forEach` 实测输出 `[7,8,6,10,11,9,3,1,5,4,2,0]` —— 不保序，要顺序必须
   `forEachOrdered`。

> 附带一条工程结论：**`Stream` 不能出现在字段和返回值里**。它是方法内的临时游标，
> 第二次终止操作就抛 `stream has already been operated upon or closed`。
> 要复用就给 `Supplier<Stream<T>>`。

## 四、今天踩的坑

- **又栽在微基准上（第二次）。** 5M 个数平方和读出「串行 1 ms / parallel 0 ms」——
  活太轻，JIT 把整个 `sum` 向量化甚至常量折叠了，测的是「循环被消掉」的时间。
  换成「算不出答案」的重活（每元素 30 轮乘加）+ 20M/2M 两档才得到有意义的三组数字。
  这条和 Day 2 的 6 倍抖动是同一类错误：**数字接近 0 或两档差距离谱，先怀疑 JIT**。
- **以为 `sorted` 一定全量，被实测打脸**，顺手查了 `Streams.java` / `SortedOps.java` 才把原因说清。
  这条提醒自己：**「我知道 XXX 很贵」也是一个需要 fact-check 的断言。**
- `peek` 日志一条都没打印，第一反应是「流没执行 / 代码没编译进去」，
  实际是 `count()` 走了快速路径。加个 `filter(x -> true)` 就现原形。

## 五、环境备忘

- 跑 day5：`run.bat day5.StreamLazy` / `run.bat day5.StreamOnce` / `run.bat day5.FuncInterfaceRule`
- 想看 JDK 源码：`jar xf "C:/Program Files/Java/jdk-17/lib/src.zip" java.base/java/util/stream/SortedOps.java`
- 手动编译仍然要先 `find src/main/java -name "*.java" > sources.txt`（今天又差点漏，新类会 `ClassNotFoundException`）

---

# 下一个时间盒（60 分钟）

**W02 Day 6：`List<User>` 过滤 → 排序 → 分组 → 统计** —— W02 的收尾练习，
也是**最能直接搬进业务代码**的一次：

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述今天：中间/终止怎么分 / 屏障什么时候不成立 / 流为什么只能用一次 | 口头过，卡住回看 Day 5 |
| 15 分钟 | 过滤 + 多级排序（`Comparator.comparing().thenComparing()`，含 null 值排最后） | `day6/UserReport.java` A / B 段 |
| 20 分钟 | 分组：`groupingBy` 一参 / 两参 / 三参（下游收集器），`counting` / `summingInt` / `averagingDouble` / `mapping` | 同上 C 段 |
| 10 分钟 | `toMap` 的重复 key 会抛 `IllegalStateException`（撞一次），`teeing` 一次算两个聚合 | 同上 D 段 |
| 5 分钟 | 输出：`day6-result.txt` + 笔记 Day 6 小节 + 勾掉「过滤 / 排序 / 分组 / 统计」 | 笔记 + 快照 |

> 优先做第三个 20 分钟。`groupingBy` 的三参重载（`classifier, mapFactory, downstream`）
> 和下游收集器是业务里 90% 的「分组统计」需求 —— 会写这一条，就能把一大坨 for 循环删掉。
> 顺手把 `Collectors.toList()`（可变）和 JDK 16 的 `Stream.toList()`（不可变）撞一次
> `UnsupportedOperationException`，这两个混用过的项目不少。
