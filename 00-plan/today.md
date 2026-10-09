---
title: 今天做什么
date: 2026-10-09
weekday: 周五
milestone: M0 · 收尾
task: W01 任务 #1 — HashMap 结构与 put 全流程
status: 已完成
---

# 今天 · 2026-10-09（周五）· 已完成

> 断档 17 天后重启成功。60 分钟时间盒跑完，产出了可运行代码 + 笔记。

## 完成情况

| 产出 | 位置 |
| :--- | :--- |
| 代码 | `02-code/stage1-basics/w01-collections/.../day2/HashMapProbe.java`（6 个实验） |
| 实测快照 | `.../w01-collections/day2-result.txt` |
| 笔记 | `01-notes/stage1-basics/W01-Java集合.md` Day 2 小节 + 6 个核心问题（留空自测） |
| 工程改动 | `run.bat` / `run.sh` 支持带包名跑；README 补 Day 2 清单 |

**额外解决的两个问题**（都进了笔记）

1. `MyArrayList.size` 找不到初始化 —— 成员变量由编译器补默认值，JVM 分配对象时内存归零。
2. JDK 9+ 反射读 `HashMap.table` 被模块系统拦截 —— 用 `--add-opens` + 代码内降级探测解决。

---

# 明天排期（2026-10-10 周六）

**M0 任务 #2 + #4**：手写 `MyHashMap`（降级版：只写 put / get / resize 三段）
+ 把今天的题目整理进 `04-interview/question-bank/Java基础.md`。

> 优先做 MyHashMap 的 `put` + `resize`，这两段是理解"为什么扩容只拆两条链"的最快路径。
