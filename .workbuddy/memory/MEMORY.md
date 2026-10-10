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

**当前状态**：M0（收尾）进行中 = 把 W01 做完封板。

- Day 1（09-22）ArrayList / LinkedList ✅
- Day 2（10-09）HashMap put 全流程 + 线程不安全复现 ✅
- Day 3（10-10）**HashSet / LinkedHashMap 完整一节 + 10 个实验** ✅
  （MILESTONES 里原本标着"砍掉"，学员主动点名要教程 → 已订正为保留）
- 剩余：手写 MyHashMap（降级版 put/get/resize）、集合面试题补"我的回答"、全周复述封板

**待办 #4「集合面试题」已部分完成**：`04-interview/question-bank/Java基础.md` 已有 10 道题（含追问预判），
但「我的回答」栏全空着，等学员自己填。


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
- **「核心问题」和「一句话总结」留空给学员自己填**，答案放文末「参考要点」，不要直接写进正文
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
