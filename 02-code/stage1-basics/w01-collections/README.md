# W01 · Java 集合 · 实验工程

> 目标：**不只是会用 HashMap，要能解释它。**
>
> 本周笔记：[`../../../01-notes/stage1-basics/W01-Java集合.md`](../../../01-notes/stage1-basics/W01-Java集合.md)

---

## 怎么跑

本周不需要任何第三方依赖，纯 JDK 就能跑（本机已装 JDK 8 / 17 / 23 / 25，本工程按 **JDK 17** 编译验证）。

```bat
rem Windows：双击，或在 cmd 里执行
run.bat
run.bat MyArrayList
run.bat day2.HashMapProbe
```

```bash
# Git Bash / macOS / Linux
./run.sh
./run.sh MyArrayList
./run.sh day2.HashMapProbe
```

> `run.bat` 的规则：**不带点号**的参数当作短名，默认去 `day1` 包里找；
> **带点号**的（如 `day2.HashMapProbe`）当作完整类名，加前缀 `com.newt.java48.w01.`。

如果只想手动来：

```bash
javac -encoding UTF-8 -d out src/main/java/com/newt/java48/w01/day1/*.java
java --add-opens java.base/java.util=ALL-UNNAMED -cp out com.newt.java48.w01.day1.ArrayListVsLinkedList
```

> ⚠️ `--add-opens java.base/java.util=ALL-UNNAMED` 是给 **Day 2 的 HashMapProbe 反射读内部字段**用的。
> 不加的话 Day 1 的两个类照样能跑，但 Day 2 会退化成"按 size 推算容量"的模式。
> 原因见笔记 Day 2 的「踩坑」一节 —— JDK 9+ 模块系统默认禁止反射 `java.util` 内部字段。

> 目录已经按 **Maven 标准布局**（`src/main/java`）建好，等 W05 开始写 Spring 时，
> 直接补一个 `pom.xml` 就能升成 Maven 工程，不用挪文件。

## 目录

```text
w01-collections/
├── run.bat / run.sh                                一键编译 + 运行（支持跨 day 按类名跑）
├── day1-result.txt                                 Day 1 实测输出（本机快照）
├── day2-result.txt                                 Day 2 实测输出（本机快照）
├── day2-buanquan-result.txt                        Day 2 线程不安全复现的实测输出
└── src/main/java/com/newt/java48/w01/
    ├── day1/
    │   ├── ArrayListVsLinkedList.java              7 项操作的真实耗时对比 + 内存占用
    │   └── MyArrayList.java                        手写动态数组，打印扩容现场
    └── day2/
        ├── HashMapProbe.java                       HashMap put 全流程：扰动 / 桶定位 / 冲突 / 扩容
        └── HashMapBuAnQuan.java                    多线程 put 丢数据的最小复现
```

---

## Day 1（ArrayList / LinkedList）实验清单

| # | 要验证的结论 | 命令 | 预期 |
| :-- | :--- | :--- | :--- |
| 1 | 按下标遍历链表是性能事故 | `run.bat` | `for-i indexed walk` 一行，LinkedList 慢 **上千倍** |
| 2 | 随机访问链表慢 | `run.bat` | `get(i) random access`，LinkedList 慢 **上百倍** |
| 3 | 尾部追加链表居然也不快 | `run.bat` | `add(E) append`，LinkedList 慢 **2~4 倍** |
| 4 | 用迭代器遍历时两者差不多 | `run.bat` | `iterator walk + sum`，比值接近 **1x** |
| 5 | 头部插入删除链表才真正赢 | `run.bat` | `add(0,E)` / `remove(0)` 两行，LinkedList 快 **十几~几十倍** |
| 6 | 中间插入链表又输了 | `run.bat` | `add(mid,E)`，因为**先要 O(n) 找到那个位置** |
| 7 | 同样 100 万元素，链表更费内存 | `run.bat` | 约 **5.7 倍**（Node 对象头 + prev/next 两个指针） |
| 8 | 扩容是 1.5 倍，不是 2 倍 | `run.bat MyArrayList` | `10 -> 15 -> 22 -> 33 -> 49 -> 73 -> 109` |

## 读代码的顺序

1. 先跑 `run.bat MyArrayList`，看扩容现场，读 `MyArrayList.grow()` —— 只有十来行，把这十行看懂，
   JDK `ArrayList` 的扩容你就懂了 80%。
2. 再读 `MyArrayList.add()` 和 `remove()`：存入元素、更新个数、搬移元素分开写，
   跟着每一步观察 `size` 和数组内容如何变化。扩容计数由每个实例单独保存。
3. 再跑 `run.bat`，从 `ArrayListVsLinkedList.main()` 选择一个 `compare...()` 实验。
   每个实验依次准备两种集合、传入被测操作、打印每次操作的平均耗时。
4. 重点对照 `sumByIndex()`（反复 `get(i)`）和 `sumByIterator()`（增强 for）。
   最后再读 `compareOperations()`、`measureNanos()` 等计时工具。

`warmUp()` 用于预热，`consumedSum` 用于保存计算结果、降低无用计算被消除的风险。
这些处理不能保证测量精确；部分实验的两种集合规模不同，表中倍率仅供本次观察。

## Day 2（HashMap）实验清单

| # | 要验证的结论 | 命令 | 预期 |
| :-- | :--- | :--- | :--- |
| 1 | key 怎么变成桶下标 | `run.bat day2.HashMapProbe` | "hello" → hashCode `99162322` → 扰动后 `99163451` → 桶下标 **11** |
| 2 | 不做扰动会全挤一个桶 | 同上 | 4 个低位相同的 hash，**不扰动全落在下标 1**，扰动后分散到 0/3/2/5 |
| 3 | 哈希冲突长什么样 | 同上 | `"Aa"` 与 `"BB"` hashCode 都是 **2112**，同桶但 equals 为 false |
| 4 | 什么时候扩容 | 同上 | 16→13 个触发扩到 32；32→25 个扩到 64；64→49 个扩到 128 |
| 5 | 扩容后新下标只有两种可能 | 同上 | 8 个 key 里，要么"不动"，要么"**+16**"（旧容量） |
| 6 | put 覆盖时返回什么 | 同上 | `null` → `100` → `200`；**size 始终是 1** |

> 实验 D 的扩容点是 `size > threshold`（严格大于），
> 所以容量 16、阈值 12 时，第 12 个元素不触发，**第 13 个**才触发。这个边界值面试常问。

## Day 2 补充实验：HashMap 线程不安全复现

| # | 要验证的结论 | 命令 | 预期 |
| :-- | :--- | :--- | :--- |
| 1 | 多线程 put，数据会丢 | `run.bat day2.HashMapBuAnQuan` | 应放入 40000，实际**总会少一两万** |
| 2 | 换成 `ConcurrentHashMap` 就不丢 | 改一行 `new HashMap<>()` → `new ConcurrentHashMap<>()` | 实际 = 40000，丢 **0** |

做法：8 个线程，每个负责**一段互不重叠的 key 区间**，各 put 5000 个。
互不重叠意味着"理论上一个都不该丢"—— 但只要丢了，就一定是 HashMap 自己的问题。

**实测三次（JDK 17.0.12）**

```text
第 1 次   应放入 40000   实际 24979   丢了 15021
第 2 次   应放入 40000   实际 27817   丢了 12183
第 3 次   应放入 40000   实际 19641   丢了 20359
对照组 ConcurrentHashMap  实际 40000   丢了 0
```

> 注意：**丢多少每次都不一样**（30%~50% 之间波动），但一定会丢。
> 这种"结果不稳定"本身就是竞态的指纹 —— 如果它是线程安全的，无论跑多少次都该是 40000。

**为什么只改了 Map 的实现类型就安全了？**
因为 `ConcurrentHashMap` 用 CAS + synchronized 把每一处临界区都保护起来了
（`++size` 走 `addCount` 的 CAS、桶头用 `synchronized` 锁一个节点、扩容允许多线程协作搬运）。
而 `HashMap` 完全没有这些保护，它从头到尾都是为**单线程**设计的。

## 读代码的顺序

**Day 1**

1. 先跑 `run.bat MyArrayList`，看扩容现场，读 `MyArrayList.grow()` —— 只有十来行，把这十行看懂，
   JDK `ArrayList` 的扩容你就懂了 80%。
2. 再读 `MyArrayList.add()` 和 `remove()`：存入元素、更新个数、搬移元素分开写，
   跟着每一步观察 `size` 和数组内容如何变化。扩容计数由每个实例单独保存。
3. 再跑 `run.bat`，从 `ArrayListVsLinkedList.main()` 选择一个 `compare...()` 实验。
   每个实验依次准备两种集合、传入被测操作、打印每次操作的平均耗时。
4. 重点对照 `sumByIndex()`（反复 `get(i)`）和 `sumByIterator()`（增强 for）。
   最后再读 `compareOperations()`、`measureNanos()` 等计时工具。

**Day 2**

1. 从 `HashMapProbe.main()` 往下读六个实验，每个实验对应一个"看不见的过程"。
2. `jdkHash()` 和 `bucketIndex()` 是把源码里的两行抽出来单独写 ——
   对照 JDK 源码 `h = key.hashCode()) ^ (h >>> 16)` 和 `tab[i = (n - 1) & hash]`。
3. `probeReflect()` / `capacityOf()` 是"拿不到内部状态也不要崩"的降级写法，
   值得学的是这个思路，而不是反射本身。

`warmUp()` 用于预热，`consumedSum` 用于保存计算结果、降低无用计算被消除的风险。
这些处理不能保证测量精确；部分实验的两种集合规模不同，表中倍率仅供本次观察。

```text
operation                     scale (AL/LL)    ArrayList   LinkedList     LL/AL
                                                   ns/op        ns/op         x
--------------------------------------------------------------------------------
add(E)   append                     1M / 1M        44.02       161.24     3.66x
get(i)   random access             1M / 20k        42.87      6990.08   163.04x
for-i    indexed walk              1M / 20k         2.97      6710.78  2262.57x
iterator walk + sum                 1M / 1M        10.60         9.83     0.93x
add(0,E) insert head              20k / 20k       995.54        56.28     0.06x
add(mid,E) insert middle          20k / 20k       487.29      7634.38    15.67x
remove(0) remove head             20k / 20k       894.96        24.89     0.03x

ArrayList   extra heap:    4.00 MB   (1 x Object[] with 1M slots)
LinkedList  extra heap:   22.91 MB   (1M x Node objects, each {prev,item,next})
```

> 每次跑数字都会有浮动（GC、JIT、CPU 频率），**要看的是数量级，不是小数点**。
> 自己在自己机器上再跑一遍，对比一下——如果某项和上表差了一个数量级，那是值得追下去的问题。
