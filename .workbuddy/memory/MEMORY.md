# 项目长期记忆 · E:\Notes

## 这是什么

用户的 **Java 进阶学习工程**（每天 1 小时，8K → 25K），仓库用 NoteSync 自动同步到 GitHub，iPad 上读。

**进度制已于 2026-10-09 变更：周次制 → 里程碑制。**

| 文件 | 作用 |
| :--- | :--- |
| `00-plan/MILESTONES.md` | **唯一进度来源**。M0~M6 七个里程碑，不设周数、不写死日期 |
| `00-plan/today.md` | 每日时间盒，开工先看、收工覆盖 |
| `00-plan/java-48week-plan.md` | 降级为「知识点字典」，当参考查，不看周次 |
| `00-plan/PROGRESS.md` | 降级，里程碑封板时顺手勾对应周次行即可 |
| `00-plan/STAGES.md` | 阶段主题 + 能力自评矩阵 |

**里程碑制三条核心规则（务必遵守）**

1. 每天 60 分钟，产出「知识点 / 代码 / 笔记」**任意两样**即算数；"只干了 30 分钟"不算失败，"没打开 IDE"才算
2. **取消补课**：断档后从当天该做的那一小步接着走，绝不回头补漏（补课是周次制的病根）
3. 只细化当前和下一个里程碑，更远的等走到跟前再排

**为什么改**：2026-09-22 跑完 W01 Day 1 后停了 17 天（学员"忙别的去了"）。
周次制会产生"欠账感"——计划说第 3 周学并发、实际第 5 周才学到，于是每天都在还账，最后不看计划表。

**当前状态**：**M0 已封板（2026-10-27），M1（Java 地基）进行中。**

- Day 1（09-22）ArrayList / LinkedList ✅
- Day 2（10-09）HashMap put 全流程 + 线程不安全复现 ✅
- Day 3（10-10）**HashSet / LinkedHashMap 完整一节 + 10 个实验** ✅
  （MILESTONES 里原本标着"砍掉"，学员主动点名要教程 → 已订正为保留）
- Day 4（10-27）**手写 MyHashMap（`day6/` 包，put/get/resize）+ 与 JDK 20 万次对拍 0 处不一致** ✅
- Day 4（10-27）题库扩到 **12 题**、题题有「我的回答 + 追问预判 + 项目案例草稿」；全周复述封板 ✅
- **M1 Day 1（10-28）`w02-generics-stream/` 工程建好，类型擦除 + PECS 两个 Demo + 6 类编译期报错原文** ✅
- **M1 Day 2（10-29）异常体系**（`day2/` 10 段实验）+ 补完 Day 1 三个遗留
  （修 `ErasureProof` B2 段 `NoSuchMethodException`、`GenericLimits2` 6 个通配符报错、`WildcardCapture` 捕获正例）✅
  题库同天 15 → **17 题**（异常 2 道）
- **M1 Day 3（10-30）Optional**（`day3/` 11 段实验：三反模式 + 五陷阱 + 一条链）
  + 补 Day 2 遗留「异常开销」（`ExceptionCost`，447 ns → 关栈 31 ns，15 倍）✅
  题库 17 → **18 题**（Optional 1 道）
- **M1 Day 4（11-02）Lambda**（`day4/` 12 段实验：五形态 + Predicate 短路 + andThen/compose +
  装箱逃逸 + 自定义函数式接口 + 方法引用五连 + 值捕获 + 循环变量 + this 归属 + 实例复用 + invokedynamic）
  + `compile-errors/LambdaLimits{,2,3}` 8 条报错原文 + `day4-bytecode.txt`（javap 三段证据）✅
  题库 18 → **20 题**（Lambda 2 道）
- **M1 Day 5（11-02）Stream**（`day5/` 三个 Demo：惰性求值打印顺序 / 短路探测计数 / 屏障 /
  `peek` 与 `count` / 流只能用一次 / map vs flatMap / parallel 代价 / 函数式接口抽象方法计数）
  + `day5-result.txt`；顺带补完 Day 4 遗留 ✅
  题库 20 → **22 题**（Stream 2 道）

**下一步**：W02 Day 6 = `List<User>` 过滤→排序→分组→统计（`groupingBy` 三参 + 下游收集器、
`toMap` 重复 key、`teeing`、`Collectors.toList()` vs `Stream.toList()` 可变性），
时间盒在 `00-plan/today.md`。做完 W02 封板 → W03 并发。

**W02 最值钱的三条（讲课直接用）**

1. 擦除擦的是**实参**、留下的是**声明**；实参藏在子类的 `Signature` 属性里
   （`StringBox.class.getGenericSuperclass()` → `Box<java.lang.String>`）。
   Spring `ParameterizedTypeReference` / Jackson `TypeReference` 的匿名子类就是为这一招而生。
2. **CCE 抛在 `get` 那一行，不是 `add` 那一行**（编译器在 get 后插 checkcast）。
3. **`new RuntimeException(e)` 不丢栈，是把栈切成两段**（实测包装栈深度 3 / cause 5）；
   真丢栈是只传 message（`getCause() = null`）。**`finally` 里的 `return` 才是真的把异常变没**
   （方法返回 42，异常无影无踪，javac 不报错）—— 这就是 TWR 与 `addSuppressed` 存在的理由。
4. **「lambda 捕获值拷贝」不用背 `effectively final`，看合成方法就懂**：
   `javap -p` 里 `lambda$sectionB$1(int, int)`，第二个形参就是被捕获的局部变量；
   捕获 `this` 的合成方法**不是 static**（所以字段能改），`int[] box` 能"绕过"是因为拷的是引用。
   这条与擦除、sneakyThrow 同属「编译器骗得过类型系统，骗不过内存布局」。

**踩过的工具坑**：`Class.getMethod()` 只找 public，包私有方法要用 `getDeclaredMethod`（且不含继承）；
抓 `capture of ?` 细节要加 `-Xdiags:verbose`；`compile-errors/` 下的注释必须写英文，
否则 javac 回显源码行时中文变 GBK 乱码；
手动编译前必须刷新 `find src/main/java -name "*.java" > sources.txt`，
否则新建的类报 `ClassNotFoundException`（`run.bat` 每次都会重扫，手动走容易漏）；
**微基准只测一轮会差 6 倍**（44 ns vs 295 ns，JIT 分层编译），必须多轮 × 大样本取最小值。
**javac 分阶段**：attribution（类型/重载/目标类型）先跑，FLOW 分析后跑；attribution 一报错，
flow 就整个跳过 —— `effectively final` 这类错误会**凭空消失**。故意收集多个编译错误时，
attr 错和 flow 错要放不同文件（`LambdaLimits` / `LambdaLimits2` / `LambdaLimits3` 就是这么来的）。
`javap` 不受 `-Dfile.encoding` 影响，常量池里的中文字符串在快照里是 GBK 乱码，不影响读方法句柄。
`Function.identity()` 无入参推不出 `T`，放进 `Map<String,Function<X,?>>` 要写 `Function.<X>identity()`。
**Stream 三条**：① `sorted()` 不一定是屏障 —— 上游 spliterator 带 `SORTED` 特征（`IntStream.rangeClosed` 就有）
时 `SortedOps.opWrapSink` 直接 `return sink`，是空操作（实测探测 1 个 vs 换无序源 5 个）；
② JDK 9+ `count()` 在 `SIZED` 流上走快速路径，**整条管道连 `peek` 一起跳过**（日志 0 条），
所以 `peek` 只能调试；③ 演示「顺序」的 parallel 实验要 `forEachOrdered` 对照，
且小数据量（1k×30 轮）parallel 反而慢 2~5 倍。
**微基准重复踩坑**：活太轻会被 JIT 向量化/常量折叠（5M 平方和读出 1 ms vs 0 ms），
要挑「算不出答案」的重活 + 两档数据量，否则数字接近 0 就是测量事故。

**W01 最值钱的一条实测结论（以后讲集合必用）**：
「链表到 8 个就转红黑树」缺两个前提 —— **容量 ≥ 64** 且 **key 能比较大小**。
hash 全等 + key 不可比较时，`putTreeVal` 每次插入都要对子树 `find()` 全扫，
实测 2 万条 JDK 2521ms vs 手写链表 374ms（JDK 慢 6.7 倍）；给 key 加 `Comparable` 后 JDK 44ms（快 12 倍）。

**断档与恢复的口径**：已两次出现 17 天断档（09-22→10-09、10-10→10-27）。
学员回来后只说一句"把落后的内容补充上"，不解释原因；此时正确做法是
**按 MILESTONES 现状直接续做下一步并同步所有过期文件**，不要问"这些天怎么了"，也不要安排补课。


## 目录职责（不要混）

| 目录 | 放什么 |
| :--- | :--- |
| `00-plan/` | 计划、里程碑、进度、能力矩阵（只读参考 + 维护进度） |
| `01-notes/` | 学习笔记，每阶段一目录，**每周一份 Markdown**，文件名 `WNN-主题.md`（文件名仍保留 WNN，只是不再对应周次考核） |
| `02-code/` | 练习代码，每周一个独立可运行工程 `stageX/wNN-topic/`；跨周实验放 `02-code/lab/` |
| `03-project/` | 长期项目「异步任务管理平台」（迭代制，不再绑 W8） |
| `04-interview/` | 八股题库、项目难题、模拟面试、简历 |
| `05-resources/` | 速查表 `cheatsheets/` 与模板 `templates/` |

## 工程约定（产代码时必须遵守）

- 目录名小写 ASCII：`wNN-{主题英文}`；Java 包名：`com.newt.java48.wNN`
- 代码按 **Maven 标准布局** 建（`src/main/java`），即使当周不需要依赖 —— 到 W05 写 Spring 时直接补 `pom.xml` 即可，不挪文件
- **本机没有 Maven / Gradle**，W01~W04 用纯 `javac` + 一键脚本 `run.bat` / `run.sh`
- 本机 JDK：`C:\Program Files\Java\` 下有 jdk1.8.0_441 / jdk-17 / jdk-23 / jdk-25（`JAVA_HOME=jdk-25`）。**默认用 JDK 17 编译运行**
- Java 源码里有中文输出时：编译加 `-encoding UTF-8`，运行加 `-Dfile.encoding=UTF-8`，否则 Windows 控制台会乱码
- 笔记必须保持模板骨架：`一、本周知识点 / 二、核心问题 / 三、代码·实验 / 四、踩坑与 Bug 复盘 / 五、面试会怎么问 / 六、一句话总结 / 每日打卡`
- **「核心问题」和「一句话总结」默认留空给学员自己填**，答案放文末「参考要点」，不要直接写进正文
  - 例外：学员明确说「补上 / 补充落后内容」时，直接写成「答：… + 🔍 补充」的口述版（Day 2 / Day 3 已是这个格式），
    并在文末保留参考要点，别删
- 每天最小完成标准：① 学一个知识点　② 写一点代码　③ 留下一份自己的笔记

## 环境注意

- Bash 工具默认 PATH 损坏（`ls: command not found`）。用命令前先 `export PATH="/usr/bin:/bin:/c/Windows/System32"`，或改用 PowerShell
- PowerShell 工具不返回 stdout，需要看输出时把结果写进临时文件再 Read
- **核对 JDK 源码**：`jar xf "/c/Program Files/Java/jdk-17/lib/src.zip" java.base/java/util/HashSet.java`
  —— zip 条目前缀是 `java.base/`，写错前缀会静默失败
- javac 的中文报错在 Git Bash 下是 GBK 乱码，靠 `文件:行号:` 定位，别读文案

## 学员偏好

- 技术细节导向，不要冗长叙述；喜欢「结论 + 实测数字 + 反直觉点」这种组织方式
- 「带我完成第 N 天」= 要按计划表给出当天的时间盒内容 + 可运行代码 + 笔记回填，而不是只讲一段道理
