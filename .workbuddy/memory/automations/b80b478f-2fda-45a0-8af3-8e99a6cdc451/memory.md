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

## 2026-11-04（第七次执行 · 开 W03 并发）
- 开工回归：清空 `out/` 重编 w02（18 文件）+ 跑全部 16 个入口，全 exit=0，无中断代码；
  扫 `01-notes` 未勾项：W02 已全勾，剩余都是未解锁里程碑；`w03-concurrency-basics/` 只有 `.gitkeep` → 今天从零建工程（已删 .gitkeep）。
- 推进 W03 Day 1：新建 `day1/StartThread`（A~E：三种起线程 / run vs start / daemon）
  + `day1/RaceCondition`（B0 基线 / B1 2 万 / B2 50 万 / C1 volatile / C2 方法级+同步块各 5 轮 /
  C3 Atomic / C4 可见性对照）+ `run.bat` / `run.sh` / `README.md` + `day1-result.txt`（147 行）+ `day1-bytecode.txt`（88 行）。
- 题库 `Java并发.md` 0 → 2 题（Q1 字节码窗口，Q2 volatile 边界）。
- 同步：W03 笔记（Day 1 整节 8 小节 + 踩坑 6 条 + 面试 12 条 + 参考要点 + 打卡）、`today.md`、`MILESTONES.md`、工程 `README.md`。
- 下次起点：**W03 Day 2 = `synchronized` 锁的是谁**（this / Class / 临时对象 + 死锁 + jstack + ThreadMXBean + JDK 17 偏向锁现状）。
- 经验：**并发实验的脚手架比被测代码更容易错** —— 没有线程握手会测出「自旋 0 次就看见了」；
  自旋上限必须远大于等待时间，否则测的是「谁先到终点」；复现竞态两要素 = 工作量够长 + CountDownLatch 同时起跑。
  另外「方法级 synchronized 有 monitorenter」这个想当然又被 javap 打脸（只有 ACC_SYNCHRONIZED）——
  连续第三天栽在「断言先于实测」。

## 2026-11-05（第八次执行 · W03 Day 2）
- 开工回归：清空 `out/` 重编 w03（4 文件）+ 跑 day1 两个入口，全 exit=0，无中断代码；
  `01-notes` 未勾项只剩 W03 的 `synchronized`/`volatile`/`Lock`，其余是未解锁里程碑；`02-code` 无空目录 → **无遗留需补**。
- 推进 W03 Day 2：新建 `day2/LockTarget`（A 实例锁 this / B 静态锁 Class / C new Object() 等于没锁 + 字符串常量）
  + `day2/Deadlock`（D1 交叉死锁 + ThreadMXBean / D2 可重入 / D3a 活锁 5 次统计 / D3b 随机退避 / D3c 全局定序）
  + `day2-result.txt`（116 行）+ `day2-deadlock-jstack.txt`（142 行，含 Found one Java-level deadlock）。
- 题库 `Java并发.md` 2 → 4 题；`cheatsheets/JVM排查命令.md` 补「死锁」小节；`run.sh`/`run.bat` 支持第 2 参数透传。
- 同步：W03 笔记（Day 2 整节 8 小节 + 踩坑 5 条 + 参考要点两条 + 打卡 + 勾 `synchronized`）、`today.md`、`MILESTONES.md`、工程 `README.md`。
- 下次起点：**W03 Day 3 = `volatile` 内存语义 + happens-before + DCL**（`Lock`/AQS/`wait-notify` 排 Day 4）。
- 新增经验（已写进 MEMORY.md）：并发 demo 各段会互相污染（复用已死锁的锁对象 → 整段静默无输出）；
  **Git Bash 的 `$!` 是 MSYS pid，jstack 必须用 `ProcessHandle.current().pid()`**，且要在一条命令内完成抓取；
  线程名带 `→` 会让 Windows 下 jstack 输出乱码 → 用纯 ASCII；
  `tryLock` 不加退避 = 活锁（实测 4/5），"会活锁"只能靠多次统计证明，不能由单次输出断言。

## 2026-11-06（第九次执行 · W03 Day 3）
- 开工回归：清空 `out/` 重编 w03（11 文件 / 14 class）+ 跑 day1/day2 四入口，全 exit=0，**无中断代码**；
  `01-notes` 未勾项只剩 `volatile`/`Lock`，`02-code` 无空目录 → 无遗留需补。
- 推进 W03 Day 3：新建 `day3/HappensBefore`（A~E：Dekker litmus / monitor / volatile 发布订阅 /
  start-join / set vs lazySet）+ `day3/DclSingleton`（D1~D4：压力测试 / 手工重排 / Unsafe）
  + `day3-result.txt`（86 行）+ `day3-bytecode.txt`（170 行，含 `lock addl` 十六进制原文）。
- 题库 `Java并发.md` 4 → 6 题；笔记 W03（Day 3 整节 8 小节 + 踩坑 6 条 + 面试 9 条 + 参考要点 2 题 + 打卡 + 勾 `volatile`）、
  `today.md`、`MILESTONES.md`、工程 `README.md` 全部同步。顺手压缩了超限的 `MEMORY.md`（17.5 KB → ~7 KB）。
- 下次起点：**W03 Day 4 = `Lock` / AQS / `wait-notify`**（手写 30 行简易 AQS + `while` 防虚假唤醒 +
  公平/非公平实测 + `Condition` 对照），之后 W03 封板 → W04 线程池。
- 新增经验（已写进 MEMORY.md）：`CyclicBarrier` 每轮同步会把线程错开（要用自旋 gate + `(1,1)` 重叠探针）；
  可见性实验的循环体里**不许有方法调用**（`nanoTime` 会挡住 JIT 提升），且要让目标循环先被 JIT 编译完；
  **自旋上限必须远大于等待时间**（本月第二次踩）；并发下 `toString` 与后续读不是同一瞬间，要抓快照；
  `-XX:+PrintAssembly` 本机能跑但只输出十六进制，`f0 83 44 24 c0 00` = `lock addl $0x0,-0x40(%rsp)`。

## 2026-11-11（第十次执行 · W03 Day 4，W03 封板）
- 开工回归：清空 `out/` 重编 w03（6 文件）+ 跑 day1/day2/day3 六入口，全 exit=0、行数与 11-06 一致，
  **无中断代码**；`01-notes` 未勾项只剩 `Lock`（= 今天的任务），`02-code` 无待开工空目录 → 无遗留需补。
- 推进 W03 Day 4：新建 `day4/WaitNotify`（49 行输出）、`day4/MiniAqs`（38 行）、`day4/LockCompare`（27 行）
  + `day4-result.txt`（129 行）+ `day4-bytecode.txt`（75 行，javap 取证三种锁的字节码差别）。
- 题库 `Java并发.md` 6 → 8（Q7 AQS 两问 / Q8 wait-notify 三个必须 + Condition）。
- 同步：W03 笔记（Day 4 十小节 + 踩坑 5 条 + 面试 14 条 + 参考要点 2 题 + 打卡 + 勾 `Lock` + 状态 🟩 封板）、
  `README.md`（目录树 + Day 4 整节 + 四个坑）、`MILESTONES.md`（进展表 + W03 封板 + 下一时间盒）、
  `today.md`（覆盖为 11-11，含下一时间盒 W04 Day 1 线程池）。
- **W03 已封板**，五个知识点全勾。下次起点：**W04 Day 1 = 线程池**（从 `.gitkeep` 建工程）。
- 本次自查抓回 4 处「先说后验」：① C 段先写的「notify 叫错人丢信号」20 轮只丢 2 次 → 换成确定性构造；
  ② 快照里 `synchronized` 22 ms 与 C2 段 107 ms 打架 → 查出两套脚手架，统一后 5.8 倍；
  ③ 公平锁「最长等待」实测反而更大，文案原本写的是教科书结论 → 按数据改写成「换的是上界不是更短」；
  ④ 结论文案写「十几倍」，实测只有 5~6 倍 → 改回实测值。
- 新增经验（已写进 MEMORY.md）：不同脚手架数字不能同表比 + 必须 `warmup()`；靠概率的实验不算结论；
  并发实验先让人睡着再发生事件；`join`/`wait`/IO 不许写在 `synchronized` 里。
- 环境提醒：加了新文件后 `sources.txt` 是旧的 → `ClassNotFoundException`，手动编译必须先刷新清单。

## 2026-11-12（第十一次执行 · W04 Day 1 线程池）
- 开工回归：清空 `out/` 重编 w03（6 文件）+ 跑 day1/day2/day3 六个入口，全 exit=0、行数与 11-06 逐个一致
  （31/106/40/48/47/32），**无中断代码**；`01-notes` 未勾项只剩 W04 起，`02-code` 只剩 `w04-` 起的 `.gitkeep` → 无遗留需补。
- 推进 W04 Day 1：从 `.gitkeep` 建 `w04-threadpool-completablefuture/`（run.sh / run.bat / README），
  新建 `day1/PoolBasics`（A~E）、`day1/PoolFlow`（S1~S5 快照表）、`day1/PoolTraps`（T1~T6）
  + `day1-result.txt`（164 行）+ `day1-jdk-source.txt`（ThreadPoolExecutor execute 三步 + 类文档原文）。
- 题库 `Java并发.md` 8 → 10（Q9 任务四步顺序 / Q10 Executors 禁用 + 线程数）。
- 同步：W04 笔记（Day 1 十一小节 + 踩坑 5 + 面试 12 + 参考要点 2 题 + 打卡 + 勾前三格）、
  `today.md`（覆盖为 11-12，下一时间盒 W04 Day 2）、`MILESTONES.md`（M1 进展表 + 三条结论 + 下一步）。
- 下次起点：**W04 Day 2 = CompletableFuture**（`supplyAsync` 默认 `commonPool` / `thenApply` vs `thenCompose` /
  异常链 `exceptionally`/`handle`/`whenComplete`）。
- 本次踩坑（已进 MEMORY.md）：`catch(OOM)` 里直接 printf 会二次 OOM（先预留内存再打印）；
  `ThreadLocal` 串味实验必须 `fixed(1)`（`fixed(2)` 两个请求落到不同线程，残留根本没发生）；
  `DiscardOldestPolicy` 连丢多个；`PoolFlow` 的 core=0 那格会抖 → 输出里标注；`ExecutorService` 无 `getQueue()`。

## 2026-11-19（第十二次执行 · W04 Day 2，W04 封板）
- 开工回归：清空 `out/` 重编 w04（6 文件）+ 跑 day1 三入口，全 exit=0、无回归、**无中断代码**；
  `01-notes` 未勾项只剩 W04 的两格（= 今天的任务），`02-code` 只剩 `w05-` 起的 `.gitkeep` → 无遗留需补。
- 推进 W04 Day 2：新建 `day2/AsyncBasics`（A~E）、`day2/ChainShape`（A~F）、`day2/AsyncErrors`（A~H）
  + `compile-errors/ChainTypeErrors.java`（3 条 `incompatible types`）+ `day2-result.txt`（215 行）
  + `day2-jdk-source.txt`（`uniWhenComplete` / `reportJoin` / `commonPool` 并行度 / `ASYNC_POOL`）。
- 题库 `Java并发.md` 10 → 12（Q11 默认池与污染 / Q12 thenApply vs thenCompose）。
- 同步：W04 笔记（Day 2 九小节 + 踩坑 4 + 面试 9 + 参考要点 Q3/Q4 + 打卡 + 后两格打勾 + 🟩）、
  `README.md`（目录树 + Day 2 三节 + 五个坑）、`today.md`（11-19 + 下一时间盒）、
  `MILESTONES.md`（进展表 + W04 封板 + 三条结论 + 下一时间盒）。
- **W04 已封板**。下次起点：**W05 Day 1 = Spring IOC/DI**；`w05-spring-ioc-di/` 需从 `.gitkeep` 建工程，
  且**本机无 Maven** → 先手工下 `spring-core`/`beans`/`context`/`expression`/`commons-logging` 到 `lib/`。
- 本次自查抓回 4 处（都靠实跑输出对不上发现）：① 「whenComplete 抛异常会覆盖原始异常」写反了，
  实测原始异常胜出、新异常只进外层 suppressed；② 查 suppressed 查错层（`e.getCause()` 而非 `e`）；
  ③ `nested.join()` 的 toString 会抖（`[Not completed]`/`[Completed normally]`）→ 改成先 `join().join()`，
  并把文档里硬编码的 `@1218025c` 全换成 `@<hash>`；④ `e instanceof ISE` 编译不过（`e` 是 CompletionException）。
- 环境：`run.sh` 短名只认 day1，跑 day2 必须写 `day2.AsyncBasics`；
  编译错误取证加 `-J-Duser.language=en -J-Duser.country=US` 才不是 GBK 乱码。

## 2026-11-20（第十三次执行 · M1 封板，开 M2 / W05 Day 1）
- 开工回归：清空 `out/` 重编 w04（6 文件）+ 跑六个入口，全 exit=0、行数 45/57/35/40/43/80 与 11-19 一致，
  **无中断代码**；`01-notes` 未勾项只剩 W05 剩余 3 格 + W06 起未解锁内容，`02-code` 只剩 `w05-` 起的 `.gitkeep` → 无遗留需补。
- **M1 判定封板**：4 份笔记 + 4 个可运行工程 + 题库 24 + 12 = 36 道（要求 8 道），全部超额 → MILESTONES 改 M1 ✅ / M2 🟨。
- 推进 W05 Day 1：`w05-spring-ioc-di/` 从 `.gitkeep` 建工程（21 个 java / 24 个 class）。
  工具链：curl 从 repo1.maven.org 下 Spring 5.3.39 六个 jar 到 `lib/`（**用 spring-jcl，不是 commons-logging**），
  `run.sh` 用 `-cp "out;lib/*"`（Windows 分号）。三个入口：`ManualWiring` / `SpringWiring` / `ContainerQuestions`。
- 产出：`day1-result.txt`（355 行）+ `day1-spring-source.txt`（6 段源码，行号与栈逐一对撞：
  1222 autowireConstructor / 1232 instantiateBean / 955 getBean / 234 singletonFactory.getObject() /
  582-613-619 doCreateBean 三步）。
- 同步：W05 笔记（Day 1 九小节 + 踩坑 4 + 面试 10 + 参考要点 Q1~Q3 + 打卡 + 勾第一格）、
  `Spring.md` 0 → 3 题、`today.md`（11-20 + 下一时间盒 W05 Day 2）、`MILESTONES.md`（M1 封板 + 进展行 + 下一步）、
  工程 `README.md`。
- 下次起点：**W05 Day 2 = Bean 生命周期**（六种 init/destroy 回调打时间戳 + 两个 `BeanPostProcessor`；
  三个追问：`@PostConstruct` 谁调的 / prototype 销毁回调 / BPP 自己被不被后处理）。
- 本次踩坑（已进 MEMORY.md）：① 自指统计（针脚字符串写在被数文件里 → 0 个 new 数出 3）；
  ② `GenericApplicationContext` 只能 refresh 一次；③ `getBeanDefinitionNames()` + `getBean(name)` 会把 `@Lazy` 提前造，
  要让「refresh 时没造」成立必须读 `getBeanDefinition().getBeanClassName()`；
  ④ 栈上 `getSingleton:234` 是 `singletonFactory.getObject()`，不是查三级缓存那个重载。
- 口径提醒：时间戳每次跑抖 ±30 ms，文档里的 `+312/+338/+354 ms` 是当次快照，改代码后重新生成快照再抄数字。

## 2026-11-23（第十四次执行 · W05 Day 2 Bean 生命周期）
- 开工回归：清空 `out/` 全量重编 w05（21 文件 / 24 class）+ 跑 day1 三入口，全 exit=0、行数 59/127/154，
  与 11-20 的 355 行快照一致，**无中断代码**；`01-notes` 未勾项只剩 W05 后两格 + 未解锁里程碑，
  `02-code` 只剩 `w06-` 起的 `.gitkeep` → 无遗留需补。11-20→11-23 断 3 天（周末），按口径不补课。
- 推进 W05 Day 2：新建 `day2/` 14 个文件 + `support/Stacks` + `lib/javax.annotation-api-1.3.2.jar`（第 7 个 jar）。
  `LifecycleOrder`（六回调时间线 / 逆序销毁 / prototype / 推断 close）+ `PostProcessorTrace`（计数表 / 栈 / 三追问 / Ordered）。
  产出 `day2-result.txt`（140 行）+ `day2-spring-source.txt`（7 段源码）。
- 题库 `Spring.md` 3 → 5（Q4 生命周期 / Q5 BPP 漏掉谁）；同步笔记 Day 2 整节、README、`today.md`、`MILESTONES.md`。
- 下次起点：**W05 Day 3 = Bean 作用域**（四作用域对照 / `request` 在非 Web 容器的报错原文 /
  `@Scope(proxyMode)` 为什么存在 / 自定义 `Scope` 插件）。时间盒已写进 `today.md`。
- 本次自查抓回 2 处「断言先于实测」：① `bppSecond` 写成 1/1，实测 0/0（同批「先收集再注册」）；
  ② 栈行号写 154，实际 157（声明行 vs 调用行）。**引行号/写顺序前必须自己跑一次。**
- 环境新增：**JDK 11+ 没有 `javax.annotation`** → `@PostConstruct` 编译不过，必须补
  `javax.annotation-api`（Spring 5 用 javax，Spring 6 才换 jakarta）；没这个 jar 时注解**静默不执行**。
- 维护：`MEMORY.md` 19.2 KB → 13.6 KB（W02 细节只留「一句话 + 关键数字」）；
  注意 bash heredoc 里的反引号会被命令替换，写记忆文件时用 Edit 或转义。

## 2026-11-23（第十五次执行 · 同日第二个时间盒 · W05 Day 3 Bean 作用域）
- 开工回归：清空 `out/` 全量重编 w05（37 文件 / 45 class）+ 跑五个入口（day1×3 / day2×2），
  exit=0、行数 61/129/156/56/77，**无中断代码**；`01-notes` 未勾项只剩「Bean 作用域」（今日）+
  「Spring 帮你管理什么」（留 W05 收尾），`02-code` 只剩 `w06-` 起的 `.gitkeep` → 无遗留需补。
- 注意：本次与第十四次是**同一天**（上午 Day 2、09:46 Day 3），不涉及补课；`today.md` 由「下一个时间盒」直接兑现。
- 推进 W05 Day 3：新建 `day3/ScopeProbe`（三计数探针）+ `day3/ScopeMatrix`（A 三判据 / B thread 作用域 /
  C 四作用域矩阵）+ `day3/WebScopeTrap`（A request / B session / C proxyMode 三组 / D 自定义 Scope）。
  产出 `day3-result.txt`（154 行）+ `day3-spring-source.txt`（7 段源码，另下了 spring-aop sources jar）。
- 题库 `Spring.md` 5 → 7（Q6 作用域四判据 / Q7 proxyMode 是把错误推迟）；同步笔记 Day 3 整节（6 小节）+
  踩坑 5 行 + 面试 6 条 + 参考要点 Q6/Q7、README（目录树 + 第 6 节 + 两坑 + 待办）、`today.md`、`MILESTONES.md`。
- 下次起点：**W05 Day 4 = 注入方式的取舍**（`@Autowired` byType / `@Resource` byName / `@Qualifier` /
  `@Primary` / 集合注入 `List`·`Map` + `@Order` / 构造器注入两细节 + `AutowiredAnnotationBeanPostProcessor` 的栈）。
- 本次踩坑（已进 MEMORY.md）：`@Scope` 注解与 `Scope` 接口同名（implements 要全限定名）；
  `SimpleThreadScope` **没有** `SCOPE_NAME` 常量；`:1942` 是声明、`:1944` 才是 `!isPrototype()`；
  自定义作用域的定义表「外层有 class、真身只有 factoryMethod」；`ctx.close()` 只销毁单例，不管自定义作用域。
- 内容自查抓回 3 处：① 「两个定义 class 都为空」错（外层是 `ScopedProxyFactoryBean`）；
  ② 「close() 会通知 scope」错；③ `registerScope` 行号先写 1046（`}`）后改 1045（真 throw）。
  留档：`ScopeMatrix` B 段那两条 JUL WARN 是**证据**，快照里必须保留。

## 2026-11-24（第十六次执行 · W05 Day 4 注入方式的取舍）
- 开工回归：清空 `out/` 重编 w05（40 文件 / 57 class）+ 跑 7 个入口，全 exit=0，**无中断代码**；
  `01-notes` 未勾项只剩 W05 第四格（= W05 收尾）与未解锁里程碑，`02-code` 只剩 `w06-` 起的 `.gitkeep` → 无遗留需补。
- 推进 W05 Day 4：新建 `day4/InjectionChoice`（A~F）、`day4/CollectionInject`（A~D）、`day4/CtorInject`（A~E）、
  `day4/pay/`（接口 + 三个 `@Order` 不同的实现）、`support/Quiet`（压掉 refresh 失败的 JUL 噪音）。
  产出 `day4-result.txt`（145 行）+ `day4-spring-source.txt`（8 段源码，行号与栈对撞：1635/1639/1642/1369/1493-1500/546）。
- 题库 `Spring.md` 7 → 9（Q8 多候选判定链 / Q9 两种注解的报错差别）；同步 W05 笔记 Day 4 整节（8 小节）+
  踩坑 4 条 + 面试 8 条 + 参考要点 Q8/Q9 + 打卡、README（§7 + 坑 10~13 + 待办）、`today.md`、`MILESTONES.md`。
- 收工回归：重编 48 文件 / 89 class，跑 10 个入口全 exit=0（day4 三段 57 / 34 / 40 行）。
- 下次起点：**W05 Day 5 = 收尾封板**（第四格答案三段 + `pom.xml` 结账 + 题库抽讲 + 标 🟩 并开 W06 AOP）。
- 本次自查抓回 2 处：① 「零候选的集合注入给空集合」是先写文案后跑 → 实测 `required=true` 照样抛、
  `required=false` 给 null（`resolveMultipleBeans` 空候选 `return null` 后掉回单值分支）；
  ② 「注入失败在 getBean 时抛」→ 实际死在 `refresh()`（没被依赖的普通单例由 `preInstantiateSingletons` 造）。
- 新环境坑：**JUL logger 名是子类** `AnnotationConfigApplicationContext`（字段是 `LogFactory.getLog(getClass())`），
  按父类名设 `Level.OFF` 压不掉 → 以后要关某个日志，先枚举 `LogManager.getLoggerNames()` 查真名，别猜。
- 口径提醒：Day 4 快照里**没有** JUL 警告（Quiet 压掉了）；Day 2/3 的 WARN 是证据，保持原样别去压。

## 2026-11-25（第十七次执行 · W05 Day 5 收尾封板）
- 开工回归：清空 `out/` 重编 w05（48 文件 / 89 class）+ 跑 10 个入口，全 exit=0，
  行数 59/127/154/54/75/51/92/57/34/40 与 11-24 逐个一致，**无中断代码**；
  `01-notes` 未勾项只剩 W05 第四格（= 今日任务）+ W06 起未解锁，`02-code` 无空目录 → 无遗留需补。
  顺手补掉一个陈旧遗留：`PROGRESS.md` 里 W02–W05 都已封板却没勾（W01 之外全漏），一并勾上、计数改 5/48。
- 推进 W05 Day 5：新建 `day5/BoardCheck`（**把四天口述结论写成 23 条断言**，FAIL 即 exit=1；
  S1 对象图 3 / S2 生命周期 4 / S3 作用域 7 / S4 判定链 9；实测 23/23 通过）。
  产出 `day5-result.txt`（85 行）+ `day5-spring-source.txt`（4 段，713 vs 332 行号对撞）+ `pom.xml`（未验证）。
- 两个新结论：① `@Autowired` setter 打点把 `populateBean` 钉死在 `ctor` 与 `bpp.before` 之间；
  ② 两种注解「内层一样、外层不同」（713 `UnsatisfiedDependencyException` vs 332 `BeanCreationException`），
  并据此**修正 Day 4「完全同一个错」的说法**（笔记/README/题库 Q9 三处同步）。
- 题库 `Spring.md` 9 → 10（Q10 专门讲启动期异常链怎么读）；同步 W05 笔记（Day 5 整节 6 小节 +
  一句话总结 + 参考要点 Q10 + 踩坑 +3 → 19 条 + 面试 +4 + 打卡 🟩）、README（§8 + 坑 14/15 + 待办）、
  `today.md`（11-25 + 下一时间盒）、`MILESTONES.md`（进展表 + 封板段 + 下一时间盒）。
- 收工回归：重编 49 文件 / 107 class，11 个入口全 exit=0（day5 85 行）。
- 下次起点：**W06 Day 1 = AOP**（手写 JDK 代理 → `@Aspect` → `this.xxx()` 不走代理）。
  **注意 `aspectjweaver` 是 `lib/` 里没有的新依赖**，开工先确认能下到。
- 本次踩坑（已进 MEMORY.md）：静态计数器跨段污染（`JdbcRepo.BUILT` 没归零）；`failOf` 复制粘贴后返回类型没改。
- 口径提醒：`pom.xml` 只能当「声明」不能当「证据」，笔记里已用 ⚠️ 标明未验证 + 写出验证步骤。
