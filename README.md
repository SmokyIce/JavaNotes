# 我的笔记

这个仓库由 NoteSync 自动同步到 GitHub，iPad 上可读。

- 所有笔记放在 `E:\Notes` 下
- 保存即自动同步，无需手动操作

# Java 48 周进阶 · 学习工程

> 每天 1 小时，48 周从 8K 走到 25K。
> 目标：**能独立做模块 → 能解释原理 → 能解决复杂问题 → 能做技术方案 → 能通过中高级 Java 面试。**

原始计划见 [`00-plan/java-48week-plan.md`](00-plan/java-48week-plan.md)，交互看板见 [`00-plan/java-48week-roadmap.html`](00-plan/java-48week-roadmap.html)。

---

## 目录结构

```text
.
├── 00-plan/         计划、进度、能力矩阵（只读参考 + 进度维护）
├── 01-notes/        学习笔记：每个阶段一个目录，每周一份 Markdown
├── 02-code/         练习代码：每周一个可独立运行的工程 + lab 复现实验
├── 03-project/      长期项目：异步任务管理平台（第 8 周起持续迭代）
├── 04-interview/    面试冲刺：八股题库、项目难题、模拟面试、简历
└── 05-resources/    速查表与模板
```

## 四个阶段

| 阶段                | 周次    | 薪资目标     | 主题                                          | 代码目录                        | 笔记目录                         |
| :------------------ | :------ | :----------- | :-------------------------------------------- | :------------------------------ | :------------------------------- |
| 第一阶段 · 打基础   | W01–W12 | 8K → 15K     | Java → Spring → MySQL → Redis 四件套打透      | `02-code/stage1-basics/`        | `01-notes/stage1-basics/`        |
| 第二阶段 · 进阶提升 | W13–W24 | 15K → 20K    | JVM + Redis 进阶 + MQ + 并发 + Linux + Docker | `02-code/stage2-advanced/`      | `01-notes/stage2-advanced/`      |
| 第三阶段 · 系统设计 | W25–W40 | 20K → 25K    | 微服务 + 高并发 + 性能优化 + 系统设计         | `02-code/stage3-system-design/` | `01-notes/stage3-system-design/` |
| 第四阶段 · 面试冲刺 | W41–W48 | 能力 → Offer | 整理能力 + 面试 + 项目包装 + 查漏补缺         | `02-code/stage4-interview/`     | `01-notes/stage4-interview/`     |

## 每天怎么用这套目录

| 星期 | 节奏                                | 落到哪个目录                      |
| :--- | :---------------------------------- | :-------------------------------- |
| 周一 | 复习 10 + 学新 35 + 总结 15         | 笔记「知识点」+「一句话总结」     |
| 周二 | 复习 10 + 写代码 40 + 记录 10       | `02-code/<stage>/wNN-*/`          |
| 周三 | 复习 10 + 源码原理 35 + 画图 15     | 笔记「核心问题」+ 调用链路图      |
| 周四 | 复习 10 + 做项目 45 + 记录 5        | `03-project/`                     |
| 周五 | 复习 15 + 面试题 30 + 口述 15       | `04-interview/question-bank/`     |
| 周六 | 连续做项目 60，不看视频             | `03-project/`                     |
| 周日 | 回顾 20 + 重做错题 20 + 整理卡片 20 | 笔记「每日打卡」+ `05-resources/` |

**每天的最小完成标准：① 学一个知识点　② 写一点代码　③ 留下一份自己的笔记。**

## 长期项目

`03-project/` 里是贯穿全程的**异步任务管理平台**：创建 / 执行 / 进度 / 失败 / 重试 / 取消 / 日志 / 恢复。
第 8 周起步，第 2 阶段加 Redis + MQ + Docker，第 3 阶段拆微服务。

## 常用入口

- 进度总表：[00-plan/PROGRESS.md](00-plan/PROGRESS.md)
- 阶段目标与能力矩阵：[00-plan/STAGES.md](00-plan/STAGES.md)
- 每周笔记模板：[05-resources/templates/周笔记模板.md](05-resources/templates/周笔记模板.md)
- Bug 复盘模板：[05-resources/templates/Bug复盘模板.md](05-resources/templates/Bug复盘模板.md)
