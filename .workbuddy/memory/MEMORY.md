# 项目长期记忆 · E:\Notes

## 这是什么

用户的 **Java 进阶学习工程**（每天 1 小时，8K → 25K），用 NoteSync 同步到 GitHub，iPad 上读。

**进度制：里程碑制（2026-10-09 起），不是周次制。**

| 文件 | 作用 |
| :--- | :--- |
| `00-plan/MILESTONES.md` | **唯一进度来源**。M0~M6，不设周数 |
| `00-plan/today.md` | 每日时间盒；开工先看，收工覆盖，末尾写「下一个时间盒」 |
| `00-plan/java-48week-plan.md` | 降级为知识点字典 |
| `00-plan/PROGRESS.md` | 降级，里程碑封板时顺手勾 |

**三条规则**：① 60 分钟产出「知识点/代码/笔记」任意两样即算数，「没打开 IDE」才算失败；
② **取消补课**，断档后从当天该做的那一小步接着走；③ 只细化当前和下一个里程碑。

## 当前状态

**M0 已封板（10-27）；M1（Java 地基）进行中。**

- W01 集合 ✅（`MyHashMap` 对拍 20 万次 0 处不一致）
- W02 泛型/异常/Optional/Lambda/Stream/收集器 ✅ **已封板（11-03）**
- **W03 并发进行中**：Day 1 线程基础（11-04）、Day 2 锁的是谁（11-05）、
  Day 3 volatile + happens-before + DCL（11-06）均已完成
- **下一步：W03 Day 4 = `Lock` / AQS / `wait-notify`**（手写 30 行简易 AQS + `while` 防虚假唤醒），
  之后 W03 封板 → W04 线程池
- 题库：`Java基础.md` 24 题、`Java并发.md` 6 题（M1 只要 8 道，已超额）

## 目录职责

| 目录 | 放什么 |
| :--- | :--- |
| `00-plan/` | 计划、里程碑、进度、能力矩阵 |
| `01-notes/` | 每阶段一目录，每周一份 `WNN-主题.md`（文件名保留 WNN，不对应考核） |
| `02-code/` | 每周一个可运行工程 `stageX/wNN-topic/`；跨周实验放 `lab/` |
| `03-project/` | 长期项目「异步任务管理平台」（迭代制） |
| `04-interview/` | `question-bank/` 题库、项目难题、模拟面试、简历 |
| `05-resources/` | `cheatsheets/`、`templates/` |

## 工程约定

- 目录小写 ASCII `wNN-topic`；包名 `com.newt.java48.wNN`；**Maven 布局**（`src/main/java`），
  即使暂时不需要依赖 —— W05 补 `pom.xml` 即可，不挪文件
- **本机没有 Maven/Gradle**：W01~W04 用纯 `javac` + `run.bat` / `run.sh`（第 2 个参数透传给程序）
- JDK 目录有 1.8 / 17 / 23 / 25，**默认 JDK 17**
- 中文输出：编译 `-encoding UTF-8`，运行 `-Dfile.encoding=UTF-8`
- 笔记骨架：`一、本周知识点 / 二、核心问题 / 三、代码·实验 / 四、踩坑与 Bug 复盘 / 五、面试会怎么问 / 六、一句话总结 / 每日打卡`
- **「核心问题」「一句话总结」默认留空给学员**，答案写文末「参考要点」；只有学员说「补上落后内容」时才写成口述版
- 每天最小标准：① 一个知识点 ② 一点代码 ③ 一份自己的笔记

## 环境注意

- Bash 工具默认 PATH 可能损坏：先 `export PATH="/c/Program Files/Java/jdk-17/bin:$PATH"` 之类
- PowerShell 工具不回显 stdout，要输出就写临时文件再 Read
- 读 JDK 源码：`jar xf ".../jdk-17/lib/src.zip" java.base/java/util/HashSet.java`（前缀必须是 `java.base/`）
- javac 中文报错在 Git Bash 下是 GBK 乱码，靠 `文件:行号:` 定位
- **`-XX:+PrintAssembly` 本机能跑**，但只有十六进制、没有助记符；`f0 83 44 24 c0 00` = `lock addl $0x0,-0x40(%rsp)`

## 讲课最值钱的实测结论（细节在各周笔记里）

- **集合**：「链表 8 个转红黑树」缺两个前提 —— 容量 ≥ 64 且 key 可比较；key 不可比较时 JDK 比手写链表慢 6.7 倍。
- **泛型**：擦除擦实参、留声明，实参藏在子类 `Signature`；CCE 抛在 `get` 不在 `add`。
- **异常**：`new RuntimeException(e)` 是切栈不是丢栈；**`finally` 里的 `return` 才是真的把异常变没**。
- **Stream**：`Predicate` 的 `or().and()` 是左结合 `(a||b)&&c`，**不报错只少数据**；
  `toMap` 走 `HashMap.merge`（value 不许 null、key 允许）；「不可变」≠「不许 null」；
  `reversed()` 反整条链，`nullsLast` 会被翻成 `nullsFirst`。
- **并发（W03 三天）**：
  ① `count++` 四条字节码，窗口在 `iadd`→`putfield`；`incVolatile` 与 `incPlain` 指令**完全相同**，
     差别只在 `ACC_VOLATILE` 标志 —— volatile 加的是内存语义，不是指令条数。
  ② 「本地跑不出来」有物理解释：JIT 预热后单线程几十微秒，唤醒 10 线程要上百微秒，**没重叠就没有竞态**。
     复现两要素 = 工作量够长 + 同时起跑。
  ③ 三种写法三把锁（this / Class / 括号里的对象），证据用 `Thread.holdsLock` + **排队时长**（别用总耗时）。
     **`static synchronized` 保护不了实例字段** = `@Transactional`/`@Async` 失效的同一个病根。
  ④ **`synchronized(new Object())` 比真锁快 7 倍且丢 284 万** ——「加了锁反而更快」几乎一定是锁没生效。
  ⑤ 死锁：`findDeadlockedThreads()` 发现 / `jstack` 确认（成对 `waiting to lock` + 行号 + `Found N deadlock`）/
     全局定序破（2000 轮 1 ms）；**`tryLock` 不加退避会活锁（4/5）**，死锁确定、活锁概率。
  ⑥ 偏向锁：JDK 17 默认关（JEP 374），JDK 25 参数已删。别背 JDK 8 的四段升级。
  ⑦ **happens-before 是偏序不是时钟**：Dekker litmus 10 万轮 `(0,0)` 占 16%~40%（x86 只放行 StoreLoad）；
     `synchronized` 也管可见性；volatile 发布/订阅 80 ms 后 5/5 看见，普通标志 15 亿次看不见。
  ⑧ **DCL 的坑是「引用先于构造发布」**，抓到的是 `{a=1,b=null,c=0}`（停在半路，不是全默认值）；
     **x86 上永远测不出来**（TSO 不重排 store-store），判断只能靠 JMM。

## 工作方式上的硬教训（每条都踩过两次以上）

- **断言先于实测**：已连续五天抓到自己「先写结论再验证」（`sorted` 屏障、`Predicate` 优先级、
  `monitorenter`、活锁必然性、hsdis 缺失）。**写进代码注释/笔记的「结论」，先跑一遍。**
- **数字接近 0 一律当测量事故**：JIT 会消掉「没人看」的写（普通字段写 0.019 ns/次）。
  微基准要多轮取最小值，且用方法调用（`set` vs `lazySet`）防止消除。
- **自旋上限必须远大于等待时间**（这条踩了两次：3 亿 ≈ 200 ms 撞上 200 ms 等待；5 亿 ≈ 170 ms 撞上 200 ms）。
- **可见性试验三件套**：线程握手（确认已在自旋）+ 循环体里**不许有方法调用**（`nanoTime` 会挡住 JIT 提升）
  + 让目标线程先被 JIT 编译完（写线程先睡 80~200 ms）。
- **同步工具会改变时序**：`CyclicBarrier` 每轮同步会把两个线程错开（分布一边倒），
  要测重排得用自旋 gate，并且加一个「真的重叠了吗」的探针（如 `(1,1)` 计数）。
- **并发 demo 各段会互相污染**：不要复用上一段已死锁的锁对象；每段自带干净状态。
- **`jstack` 抓现场**：一条命令内「起进程 → 等 → 读程序打印的 `ProcessHandle.current().pid()` → jstack」；
  **Git Bash 的 `$!` 是 MSYS pid，不是 Windows 原生 pid**。线程名只用 ASCII。
- 收集多个编译错误要按 javac 阶段拆文件（attribution 先跑，flow 会被整段跳过）。

## 断档与恢复的口径

已出现两次 17 天断档（09-22→10-09、10-10→10-27）。学员回来后只说「把落后的内容补充上」，
不解释原因。正确做法：**按 MILESTONES 现状直接续做下一步 + 同步所有过期文件**，
不问「这些天怎么了」，不安排补课。

## 学员偏好

- 技术细节导向，不要冗长叙述；喜欢「结论 + 实测数字 + 反直觉点」
- 「带我完成第 N 天」= 当天时间盒 + 可运行代码 + 笔记回填，而不是讲一段道理
- 喜欢**反直觉但可验证**的结论；对「我本地跑不出来」这类问题接受「物理原因」解释
