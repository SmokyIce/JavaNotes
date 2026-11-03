# 自动化记忆 · 每日编码任务

## 2026-10-28（首次执行）
- 任务口径：先查历史会话/文件找出未完成项并补完，再推进当天计划（`00-plan/today.md` 的时间盒）。
- 本次补完 4 项遗留：W02 空工程目录、W01 Day 1 验收清单未勾、W02 笔记空模板、题库泛型 +3。
- 推进 M1 Day 1：类型擦除 + PECS，产出 `w02-generics-stream/` 工程（2 个 Demo + 1 个故意报错文件 + 实测快照）。
- 同步文件：`W02` 笔记、`today.md`、`MILESTONES.md`（M1 进展表）。
- 下次执行建议起点：`00-plan/today.md` 的「下一个时间盒」= W02 Day 2 异常体系；
  每次开工先扫 `01-notes` 里 `- [ ]` 未勾项与 `02-code` 下空目录（`.gitkeep` 单独存在即为未开工）。
- 环境：JDK 17 在 `C:\Program Files\Java\jdk-17`；Bash 需先 export PATH；PowerShell 不回显 stdout。

## 2026-10-29（第二次执行）
- 补完 3 项遗留：① `ErasureProof` B2 段抛 `NoSuchMethodException`（`getMethod` 只找 public，改 `getDeclaredMethod`），
  C~F 四段此前从未跑过；② `compile-errors/GenericLimits2.java`（通配符 6 个错）；③ `day1/WildcardCapture.java` 捕获正例。
- 推进 M1 Day 2：异常体系，`day2/` 两个 Demo 共 10 段 + `day2-result.txt`；题库 15 → 17。
- 同步：`W02` 笔记（Day 1 遗留 ✅ + 第 8 小节 + Day 2 整节 + 参考要点 Q4/Q5）、`today.md`、`MILESTONES.md`、工程 `README.md`。
- 下次起点：`00-plan/today.md` 的「下一个时间盒」= W02 Day 3 Optional（三反模式 + map/flatMap 链）。
- 新增经验（写进 MEMORY.md）：`getMethod` 只认 public；抓 `capture of ?` 要 `-Xdiags:verbose`；
  `compile-errors/` 下注释必须英文（javac 回显源码行会让中文变 GBK 乱码）。
- 口径提醒：笔记「一句话总结」「核心问题」仍按约定留空给学员，只把答案写进文末「参考要点」。

## 2026-10-30（第三次执行）
- 开工先做**回归验证**：清空 `out/` 全量重编 + 跑全部 8 个入口（day1×3 / day2×3 / day3×2），无回归。
  这一步值得固化成每天第一步 —— 比逐条找 TODO 更快发现「中断的代码」。
- 补完 1 项遗留：Day 2 §9「异常开销没量」→ `day2/ExceptionCost.java`，两遍 JVM 参数对照
  （447 ns → 31 ns，15 倍）。Day 2 另一条遗留（`Error` / `catch(Throwable)`）按计划留给 W13，未动。
- 推进 M1 Day 3：Optional，`day3/` 两个 Demo 共 11 段 + `day3-result.txt`；题库 17 → 18。
- 同步：`W02` 笔记（Optional 勾选 + Day 3 整节 + Day 2 §10 + 踩坑 4 条 + 面试问题 5 条 + Q6 + 打卡）、
  `today.md`、`MILESTONES.md`、工程 `README.md`（目录树 + Day 3 清单）。
- 下次起点：`00-plan/today.md` 的「下一个时间盒」= W02 Day 4 Lambda。
- 新增经验（已写进 MEMORY.md）：手动编译要刷新 `sources.txt`；微基准必须多轮取最小值（单轮差 6 倍）；
  TWR 里 catch 子类不够，资源 `close()` 的 IOException 会提升到语句上。

## 2026-11-02（第四次执行）
- 开工回归：全量重编 + 跑 7 个入口，全部 exit=0，无回归，没有中断的代码。
  扫 `01-notes` 未勾项与 `02-code` 空目录，确认无孤儿待办（剩余未勾项都是未解锁里程碑）。
- 推进 M1 Day 4：Lambda。新建 `day4/FunctionalShape`（六段）+ `day4/LambdaCapture`（六段），
  `compile-errors/LambdaLimits{,2,3}`（8 条报错原文），产出 `day4-result.txt` + `day4-bytecode.txt`。
  题库 18 → 20（Q19 捕获/匿名类对比，Q20 装箱逃逸）。
- 同步：`W02` 笔记（知识点勾 Lambda + Day 4 整节 + 踩坑 3 条 + 面试 8 条 + 参考要点 Q7/Q8 + 打卡）、
  `today.md`、`MILESTONES.md`、工程 `README.md`。
- 下次起点：**W02 Day 5 Stream**（惰性求值 / 短路 / 一个流只能用一次 / flatMap vs map），
  时间盒已写进 `00-plan/today.md`。
- 新增经验（已写进 MEMORY.md）：**javac 分阶段** —— attribution 报错会让 FLOW 分析整个跳过，
  `effectively final` 错误不出现，故意收集多个编译错误要按阶段拆文件；
  `javap` 输出里的中文字符串常量是 GBK 乱码（不影响读方法句柄）；
  `Function.identity()` 无入参推不出 `T`，要写 `Function.<X>identity()`。

## 2026-11-02（第五次执行 · 同日续做 Day 5）
- 开工回归：清空 `out/` 重编 + 跑 9 个入口 + `ExceptionCost` 两遍参数，全 exit=0，无中断代码；
  扫 `01-notes` 未勾项与 `02-code` 空目录，确认只剩 Day 6 与未解锁里程碑。
- 补完 Day 4 遗留 1 项：`day5/FuncInterfaceRule`（反射数 4 个 abstract vs 编译器算 1 个，JLS 9.8）。
- 推进 M1 Day 5：Stream。新建 `day5/StreamLazy`（惰性/短路/屏障/peek）、`day5/StreamOnce`
  （流一次性/map vs flatMap/parallel/何时不用）、`day5-result.txt`；题库 20 → 22（Q21/Q22）。
- 三个实测结论已进笔记与 README：`sorted()` 在上游带 `SORTED` 特征时是空操作（`SortedOps.java:136`）；
  `count()` 会跳过整条管道连 `peek` 一起（0 条日志）；parallel 小数据量反向慢 2~5 倍。
- 同步：`W02` 笔记（Day 5 整节 8 小节 + 踩坑 4 条 + 面试 8 条 + Q9/Q10 + 打卡）、
  `today.md`、`MILESTONES.md`、工程 `README.md`（Day 5 清单 20 项）。
- 下次起点：**W02 Day 6 = `List<User>` 过滤→排序→分组→统计**（`groupingBy` 三参 + 下游收集器、
  `toMap` 重复 key、`teeing`、`Collectors.toList()` vs `Stream.toList()` 可变性）。
- 经验：**「XX 一定很贵」也要 fact-check** —— 今天的 `sorted` 屏障假设就是被实测推翻后翻源码才说清的；
  微基准第二次栽在「活太轻被 JIT 消掉」，数字接近 0 一律视为测量事故。

## 2026-11-03（第六次执行 · W02 收尾封板）
- 开工回归：清空 `out/` 重编 18 文件 + 跑全部 15 个入口（day1×3 / day2×2 / day3×2 / day4×2 / day5×3 / day6×3），
  全 exit=0，无中断代码。扫 `01-notes` 未勾项：只剩 W02「过滤/排序/分组/统计」+ 未解锁里程碑；
  `w03-concurrency-basics/` 只有 `.gitkeep`（明天的开工对象，按口径不算遗留、不动）。
- 推进 M1 Day 6：新建 `day6/` 五个文件（`User` / `UserData` / `UserReport` / `GroupStats` / `CollectorTraps`）
  + `day6-result.txt`（208 行）。题库 22 → 24（Q23 groupingBy / Q24 toMap）。
- 同步：W02 笔记（Day 6 整节 8 小节 + 踩坑 7 条 + 面试 11 条 + 核心问题 Q11 + 参考要点 Q11 + 打卡 + 状态 🟩）、
  `today.md`（下一个时间盒 = W03 Day 1 线程基础）、`MILESTONES.md`（进展表 + W02 封板 + 五条结论）、
  工程 `README.md`（目录树 + Day 6 清单 31 行）。
- **W02 已封板**，六天全部完成。下次起点：**W03 Day 1 = 线程基础**（`w03-concurrency-basics/` 需从 `.gitkeep` 建工程）。
- 经验（已写进 MEMORY.md）：`Predicate` 的 `or().and()` 是左结合 `(a||b)&&c`；
  `toMap` 走 `HashMap.merge` 而非 `put`（value 不许 null、key 允许 null）；
  「不可变」≠「不许 null」（`Stream.toList()` 放行、`toUnmodifiableList()` 拒绝）；
  **方法引用的拆箱 NPE 没有 message，lambda 有**。
- 自查警告：本次先后三次「先写结论后验证」被自己抓回（Predicate 优先级、`Stream.toList()` 的 null 政策、
  `averagingInt` 的 message），**都是靠实跑输出对不上才发现**。写注释里的「结论」前先跑一遍。
