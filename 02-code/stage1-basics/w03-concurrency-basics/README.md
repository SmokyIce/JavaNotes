# W03 · Java 并发基础 · 实验工程

> 目标：**不是背 `volatile` 的定义，而是亲手把「丢更新」撞出来，再解释它。**
>
> 本周笔记：[`../../../01-notes/stage1-basics/W03-Java并发基础.md`](../../../01-notes/stage1-basics/W03-Java并发基础.md)

---

## 怎么跑

纯 JDK，无第三方依赖，按 **JDK 17** 编译验证。并发实验请在**真机**上跑，不要放进受限沙箱 —— 单核环境天然不丢更新。

```bat
rem Windows
run.bat                      rem 默认跑 day1.StartThread
run.bat RaceCondition        rem 短名 -> day1 包
run.bat day1.RaceCondition   rem 带点号 -> 完整类名
```

```bash
# Git Bash / macOS / Linux
./run.sh
./run.sh RaceCondition
./run.sh day1.RaceCondition
```

手动编译：

```bash
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w03.day1.RaceCondition
```

> ⚠️ `RaceCondition` 会开 10 个线程做几千万次自增，跑完大约 5 秒；`StartThread` 里的守护线程会故意被 JVM 掐断，属于预期现象。

## 目录

```text
w03-concurrency-basics/
├── run.bat / run.sh                        一键编译 + 运行
├── day1-result.txt                         Day 1 实测输出（两种起线程 + 竞态 + 可见性）
├── day1-bytecode.txt                       Day 1 字节码证据（iinc 窗口 / ACC_VOLATILE / ACC_SYNCHRONIZED / monitorenter）
└── src/main/java/com/newt/java48/w03/
    └── day1/
        ├── StartThread.java                A~E：三种起线程方式 / run vs start / 守护线程
        └── RaceCondition.java              B0~C4：基线 / 竞态复现 / volatile / synchronized / AtomicInteger / 可见性
```

## Day 1 做了什么

### `StartThread` —— 起线程的三种写法，加上两个常见误解

| 段 | 内容 | 一句话结论 |
| :-- | :--- | :--- |
| A | `extends Thread` | 能用，但把任务和线程绑死，Java 单继承 → 不推荐 |
| B | `implements Runnable` / lambda | 任务与线程解耦；`Runnable` 是函数式接口，lambda 直接能塞 |
| C | `Callable<Integer>` + `FutureTask` | 唯一能**带返回值**的方式；`call()` 抛的异常会被包成 `ExecutionException`，原异常只能从 `getCause()` 取 |
| D | `t.run()` vs `t.start()` | `run()` 打印的线程名是 `main` —— 它只是普通方法调用，压根没开线程 |
| E | daemon 线程 | 守护线程不阻止 JVM 退出，`sleep(5s)` 的那句永远不会打印 |

另外撞了一条：**`start()` 第二次调用抛 `IllegalThreadStateException`，而且没有 message** —— 又一个「异常什么都没说」的现场。

### `RaceCondition` —— 一天里最值钱的部分

| 段 | 配置 | 实测结果（JDK 17，10 线程，CountDownLatch 同时起跑） |
| :-- | :--- | :--- |
| B0 | 单线程 20 万次 | 3/3 轮都是 200000 → 循环本身没问题 |
| B1 | `int` 自增，**每线程 2 万次** × 10 轮 | **7/10 轮丢**，最惨 32265（丢 83.9%）；中间第 5~7 轮一次不丢，第 8~10 轮又小额丢（1 万~4 万） |
| B2 | 同一份代码，**每线程 50 万次** × 10 轮 | **10/10 轮丢**，最低 930367 / 5000000 |
| C1 | `volatile int` 自增 | **10/10 轮丢**，最低 1676548（丢 66.5%） |
| C2 | `synchronized`（方法级 5 轮 + 同步块 5 轮） | **0/10 轮丢**，两次都是 10 个 5000000 |
| C3 | `AtomicInteger.incrementAndGet()` | **0/10 轮丢** |
| C4 | 自旋等标志位：普通 `boolean` vs `volatile boolean` | 普通版 20 亿次自旋**始终没看见**（撞上限退出）；volatile 版 1.98 亿次后看见 |

三个必须记住的点：

1. **`count++` 在字节码里是四条指令**（`getfield` / `iconst_1` / `iadd` / `putfield`），
   窗口就在 `iadd` 和 `putfield` 之间 —— 两个线程都读到同一个旧值，就少一次加法。
2. **B1 的后 3 轮一次都没丢，不是代码变安全了。** JIT 把自增编译成机器码之后，2 万次只要几十微秒，
   10 个线程还没全被唤醒第一个就跑完了 —— 这就是「我本地跑十遍都是对的」的物理原因。**要复现，得加量**（B2）。
3. **`volatile` 救不了 `count++`**（10/10 轮都丢），它救的是 C4 那种「一个写、另一个只读」的场景。
   两件事：可见性 ≠ 原子性。

### `day1-bytecode.txt` 里被纠正的一个想当然

我原本以为方法级 `synchronized` 会产生 `monitorenter` / `monitorexit`。`javap` 打脸：

- **方法级**：字节码一条加锁指令都没有，只有方法 flags 里的 `ACC_SYNCHRONIZED`（`0x0022`）；
- **同步块**：才真的出现 `monitorenter` / `monitorexit`，而且带着异常路径（`Exception table` 里两条 `any`）。

字段那边同理：`private volatile int vol` 的 `flags: (0x0042) ACC_PRIVATE, ACC_VOLATILE`，
而 `incVolatile` 的字节码和 `incPlain` **一模一样** —— volatile 不是多一条指令，是给 `getfield`/`putfield` 加了内存语义。
