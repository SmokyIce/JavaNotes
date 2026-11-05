---
title: 今天做什么
date: 2026-11-05
weekday: 周四
milestone: M1 · Java 地基
task: W03 Day 2 — synchronized 到底锁了谁
status: 已完成
---

# 今天 · 2026-11-05（周四）· 已完成

> 昨天证明了「加锁能救丢更新」，但没回答「锁的是哪个对象」。
> 今天只做这一件事：**把锁对象钉死**，再顺着它把死锁撞出来。
> 开工第一步照旧：**回归编译 + 跑全部入口**。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 清空 `out/` 全量重编 w03（4 文件）+ 跑 day1 两个入口 | ✅ 全部 exit=0（`StartThread` 全段正常、`RaceCondition` 106 行），**没有中断的代码** |
| `01-notes` 里的未勾项 | W03 只剩 `synchronized` / `volatile` / `Lock` 三个；其余全是未解锁里程碑，无孤儿待办 |
| `02-code` 空目录 | 无 |
| Day 1 遗留「C2 耗时含预热」 | 仍未做成正规基准，按计划留到 W04 线程池那节一起做 |

## 二、今天的正题：锁对象

代码在 `02-code/stage1-basics/w03-concurrency-basics/src/main/java/com/newt/java48/w03/day2/`：

| 文件 | 内容 |
| :--- | :--- |
| `LockTarget.java` | A 实例方法锁 `this`（A1 同实例 / A2 两实例 / A3 与 `synchronized(this)` 对撞）/ B 静态方法锁 `Class`（B1 / B2 / B3 与实例锁并行）/ C `new Object()` 等于没锁 + 字符串常量全局锁 |
| `Deadlock.java` | D1 交叉拿锁 + `ThreadMXBean.findDeadlockedThreads()` 检测 / D2 可重入（含 `ReentrantLock` 的 holdCount 对照）/ D3 活锁 4/5 + 随机退避 + 全局定序 |
| `day2-result.txt` | 全部实测（116 行，含 E 段偏向锁三档证据） |
| `day2-deadlock-jstack.txt` | `jstack` 现场原件（142 行，`Found one Java-level deadlock`） |

## 三、今天挖到的六个点

1. **三种写法，三把锁，靠两条东西证明 —— 不是靠背。**
   `Thread.holdsLock(obj)` 直接问 JVM 你拿的是谁的锁；方法入口算「排队等了多久」。
   实测：同实例第二个线程排队 **414 ms**，两个不同实例**都 0 ms**（总 409 ms）；
   先 `synchronized(inst)` 占住 400 ms 再调 `inst.work()`，调用者排队 **340 ms** ——
   方法锁与 `synchronized(this)` 抢的是同一个 monitor。
   **排队时长比总耗时可靠**：总耗时会被线程启动、JIT、调度污染，排队时长是因果直接的。

2. **`static synchronized` 保护不了实例字段。**
   静态方法锁 `ClassLock.class`、实例锁 `this`，实测两路并行（都 0 ms，总 412 ms）。
   两个方法都在改同一个 `static Map` 却各加各的锁 —— 代码看着有锁，实际没锁。
   **这就是 `@Transactional` 不生效、`@Async` 不同步那一类 bug 的病根**，W07 会再撞一次。

3. **`synchronized (new Object())` 不仅没锁，还比真锁快 7 倍。**
   10 线程 × 50 万次：锁新对象丢 **2846644**、耗时 **46 ms**；锁共享 `final` 字段丢 0、耗时 **339 ms**。
   **「加了锁反而更快」几乎一定意味着锁没生效** —— 省下来的正是加锁的开销。
   附带撞到：锁字符串常量 = 拿了一把全局锁（跨类 `==` 为 true，别的类被挡 354 ms）。

4. **死锁的「发现 / 确认 / 破解」是三件事，别混。**
   发现靠 `findDeadlockedThreads()`（JVM 自己算锁图环）；确认靠 `jstack` 看
   `waiting to lock … which is held by …` **成对**出现 + 业务行号 + 末尾 `Found N deadlock`；
   破解靠**全局定序**（按 `identityHashCode` 排序，2000 轮 × 2 线程 = 4000，1 ms，零重试）。
   两个易错：`isAlive()==true` 不代表在干活；`blockedCount` 不是毫秒数。

5. **`tryLock` 不加退避会变成活锁 —— 5 次里 4 次跑不完。**
   两个线程节拍一致（都「先左手、等 50 ms、再右手」），每次都在同一处对撞、同时放弃同时重来。
   线程在跑、CPU 在烧、没有 BLOCKED 栈 —— 死锁是**确定的**，活锁是**概率的**，所以活锁更难查。
   加随机退避后 0~1 次重试就过。

6. **偏向锁：JDK 17 默认关，JDK 25 参数已删。**
   `UseBiasedLocking = false {product} {default}`（JEP 374）；显式 `-XX:+UseBiasedLocking`
   只剩一句「deprecated in version 15.0」；JDK 25 直接 `Unrecognized VM option`。
   以后讲锁升级必须补这句，别背 JDK 8 的八股。

## 四、今天踩的坑

- **demo 里各段会互相污染状态。** D2（可重入）第一版直接复用 D1 的锁对象 `A`，而那时 `A` 已被
  死锁线程永久占着 —— 整段一行输出都没有，`join(2000)` 悄悄超时，看着像「代码没跑」。
  修法：每段自带干净的锁对象。**死锁线程不会自己松手。**
- **`jstack` 报 `no such process` 两次。** 第一次是抓晚了（`hold` 只挂 20 秒，两次工具调用之间的间隔就超了）；
  第二次更坑：Git Bash 里 `$!` 拿到的是 **MSYS 的 pid**（1315），跟 Windows 原生 pid（33708）不是一个数。
  最后固定成「用程序自己打印的 `ProcessHandle.current().pid()`」。
- **又是「断言先于实测」。** D3a 的注释我先写死「不加退避一定活锁」，实跑第一次 3 次重试就成功了。
  改成跑 5 次统计比例（4/5）+ 加 `CountDownLatch` 同时起跑提高对称性，结论才站得住。
  这已经是连续第四天（`sorted` 屏障 → `Predicate` 优先级 → `monitorenter` → 今天这个）。

## 五、环境备忘

- 跑 day2：`run.sh day2.LockTarget` / `run.sh day2.Deadlock`（约 11 秒）/ `run.sh day2.Deadlock hold`
- `run.sh` / `run.bat` 今天补了「第 2 个参数透传给程序」，`hold` 模式才能从脚本跑
- 抓 jstack 一条命令搞定（避免两次调用之间的等待窗口）：
  `java ... Deadlock hold & sleep 4; jstack <程序打印的 PID> > stack.txt`
- `Deadlock` 结尾必须 `System.exit(0)`：D1 的两个线程永远醒不来，不 exit JVM 不退出
- `day2-result.txt` 是**一条命令整体生成**的（含 E 段的 flag 输出），不是手抄

---

# 下一个时间盒（60 分钟）

**W03 Day 3：`volatile` 的内存语义 + happens-before + DCL 为什么必须 volatile**

Day 1 已经撞过「volatile 救不了 `count++`，但救得了标志位」，但只到现象层。
今天要把它讲到能解释**为什么**，顺手把 W02 遗留的 DCL 单例补掉。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 2：三种写法各锁谁 / 为什么 `static synchronized` 救不了实例字段 / 死锁看 jstack 哪三样 | 口头过，卡住回看 Day 2 |
| 20 分钟 | **happens-before 八条**里挑四条能实测的（程序次序 / monitor / volatile / 线程启动与 join），每条写一个「去掉就错」的最小 Demo | `day3/HappensBefore.java` A~D 段 |
| 15 分钟 | **DCL 单例**：不加 volatile 的版本能不能复现半初始化对象？用 `Unsafe` 或「构造里 sleep + 字段赋值顺序」放大窗口，跑 100 轮统计 | `day3/DclSingleton.java` + 统计输出 |
| 10 分钟 | 用 `javap` 看 volatile 字段读写的实际屏障落点（x86 上写靠 `lock` 前缀、读基本无额外指令） | `day3-bytecode.txt` |
| 5 分钟 | 输出 `day3-result.txt` + 笔记 Day 3 小节 + 勾掉 `volatile` | 笔记 + 快照 |

> 优先做第二个 20 分钟。**DCL 是「背了十年还在答错」的题**：多数人只会说「volatile 禁止重排」，
> 说不清「重排成什么样、另一个线程会拿到什么」，今天的放大实验就是补这一刀。
>
> 顺带说明：`Lock` / AQS / `wait-notify` 排到 Day 4，`Lock` 那个勾也留到那天。
