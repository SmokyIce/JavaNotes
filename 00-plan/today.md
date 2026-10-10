---
title: 今天做什么
date: 2026-10-10
weekday: 周六
milestone: M0 · 收尾
task: W01 任务 #5 — HashSet / LinkedHashMap
status: 已完成
---

# 今天 · 2026-10-10（周六）· 已完成

> 原本 MILESTONES 把任务 #5 标成"砍掉"，实际做成了完整一节 + 10 个实验。
> 理由：HashSet / LinkedHashMap 不是两个新容器，而是 **HashMap 的两种复用方式** ——
> 跳过它们，Day 2 的 HashMap 就一直是悬着的。

## 完成情况

| 产出 | 位置 |
| :--- | :--- |
| 代码 | `02-code/stage1-basics/w01-collections/.../day3/HashSetLinkedHashMapProbe.java`（实验 A–J） |
| 实测快照 | `.../w01-collections/day3-result.txt` |
| 笔记 | `01-notes/stage1-basics/W01-Java集合.md` 「Day 3」整节（10 个小节 + 6 道自测题） |
| 题库 | `04-interview/question-bank/Java基础.md` 新增第 7~10 题 |
| 工程改动 | `README.md` 加 Day 3 实验清单 + 读码顺序；`MILESTONES.md` 任务 #5 状态订正 |

## 今天挖出来的三个反直觉点

1. **小整数骗人**：HashSet 装 1..12 打印是升序 —— 因为 `Integer.hashCode()` 返回自身、
   容量 16 时 `n & 15 == n`。换字符串立刻乱，容量 16 与 64 的顺序也完全不同。
2. **删"不存在的元素"不抛 CME**：`modCount` 只在真的改到结构时才 +1。没报错 ≠ 安全。
3. **更重的 LinkedHashMap 迭代快 21.2 倍**：HashMap 要扫完整个 table（含 null 桶），
   成本 O(capacity + size)；删元素不缩容 → 30 万删到 1 千后 capacity 仍是 524288。

其他实测数字：`contains` 差 1791 倍（37.6 万 ns/次 vs 210 ns/次）；
去重漏写任一个都是 size=2；LRU 容量判断写成 `>=` 只留 2 个；每元素多 8.1 字节。

---

# 明天排期（2026-10-11 周日）

**M0 剩余两项**：

1. 任务 #2 —— 手写 `MyHashMap`（降级版：只写 `put` / `get` / `resize` 三段）
2. 任务 #4 —— 集合面试题整理（题库已经有 10 道，补上"我的回答"）
3. 任务 #6 —— W01 全周复述，然后封板进 M1

> 优先做 MyHashMap 的 `put` + `resize`，这两段是理解"为什么扩容只拆两条链"的最快路径。
> 另外 Day 3 的 6 道自测题还空着，可以顺手填掉。
