# 项目长期记忆 · E:\Notes

## 这是什么

用户的 **Java 48 周进阶学习工程**（每天 1 小时，8K → 25K），仓库用 NoteSync 自动同步到 GitHub，iPad 上读。
计划文件：`00-plan/java-48week-plan.md`（原始计划）、`00-plan/PROGRESS.md`（进度总表）、`00-plan/STAGES.md`（阶段目标）。

## 目录职责（不要混）

| 目录 | 放什么 |
| :--- | :--- |
| `00-plan/` | 计划、进度、能力矩阵（只读参考 + 维护进度） |
| `01-notes/` | 学习笔记，每阶段一目录，**每周一份 Markdown**，文件名 `WNN-主题.md` |
| `02-code/` | 练习代码，每周一个独立可运行工程 `stageX/wNN-topic/`；跨周实验放 `02-code/lab/` |
| `03-project/` | 长期项目「异步任务管理平台」（W8 起迭代） |
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

## 学员偏好

- 技术细节导向，不要冗长叙述；喜欢「结论 + 实测数字 + 反直觉点」这种组织方式
- 「带我完成第 N 天」= 要按计划表给出当天的时间盒内容 + 可运行代码 + 笔记回填，而不是只讲一段道理
