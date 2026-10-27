---
title: 今天做什么
date: 2026-10-27
weekday: 周二
milestone: M0 · 收尾 → 已封板
task: W01 任务 #2 / #4 / #6 — MyHashMap + 题库 + 全周复述
status: 已完成
---

# 今天 · 2026-10-27（周二）· 已完成

> M0 的最后三件事一次收掉。距上次 10-10 隔了 17 天，按里程碑制**不补课**，
> 直接从「手写 MyHashMap」接着走 —— 今天做完，W01 封板，进 M1。

## 完成情况

| 产出 | 位置 |
| :--- | :--- |
| 代码 | `02-code/stage1-basics/w01-collections/.../day6/MyHashMap.java`（put / get / resize 三段） |
| 实验 | `.../day6/MyHashMapProbe.java`（5 个实验：扩容触发点 / 拆链 / 20 万次对拍 / 性能 / 树化退化） |
| 实测快照 | `.../w01-collections/day6-result.txt` |
| 笔记 | `01-notes/stage1-basics/W01-Java集合.md` 新增「Day 4」整节 + 补完 Day 3 六道核心问题 + 封板版一句话总结 |
| 题库 | `04-interview/question-bank/Java基础.md` 12 题全部补完（我的回答 + 追问预判 + 项目案例草稿） |
| 进度 | `MILESTONES.md` M0 ✅ 封板、M1 🟨 进行中；`PROGRESS.md` W01 勾掉 |

## 今天挖出来的三个反直觉点

1. **手写版比 JDK 快 0.6 倍**（30 万 put：46 ms vs 77 ms）——
   别当结论，随机 int key 在替树化打掩护。
2. **hash 全等的 key，JDK 的 HashMap 比手写链表慢 6.7 倍**（2 万条：2521 ms vs 374 ms）。
   读 `putTreeVal` 才明白：hash 比不出来、key 又没实现 `Comparable` 时，只能用 `tieBreakOrder`
   硬定方向，于是每次插入都要对子树做一次 `find()` 全扫，而 `TreeNode` 又更重。
3. **给同一个 key 加上 `implements Comparable`，JDK 从 2521 ms 变 44 ms（快 12 倍）** ——
   「链表到 8 个就转红黑树」这句话，省掉了「容量 ≥ 64」和「key 能比较」两个前提。

其他实测：扩容在 13 / 25 / 49 个触发；hashCode 1 和 17 扩容前同桶、扩容后分别落桶 1 和 17；
20 万次随机 put/get 与 JDK 对拍 **0 处不一致**（size 都是 48609，容量 65536）。

---

# 下一个时间盒（60 分钟）

**M1 · Java 地基，第一步：`W02-泛型-异常-Stream.md` 的泛型部分**

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 W01：HashMap put 四步 + 扩容为什么只拆两条链 | 口头过一遍，卡住就回看 Day 4 |
| 25 分钟 | 类型擦除到底擦掉了什么：`List<String>` 和 `List<Integer>` 为什么运行时是同一个类 | 笔记「知识点」一节 |
| 15 分钟 | 写 `w02-generics-stream/` 第一个类：用反射证明擦除（`getGenericSuperclass` 拿不到） | 可运行代码 |
| 10 分钟 | 输出：一句话讲清「擦除 → 为什么不能 `new T[]` → 为什么要 `Class<T>` 参数」 | 笔记「核心问题」 |

> 优先做「擦除的证据」那 15 分钟。看一百遍"泛型是编译期的"，不如自己用反射打出
> `List<String>.getClass() == List<Integer>.getClass()` 这一行。
