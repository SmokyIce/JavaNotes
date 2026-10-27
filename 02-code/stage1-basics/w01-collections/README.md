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
├── day2-kuorong-result.txt                         Day 2 扩容触发条件的实测输出
├── day2-chailian-result.txt                        Day 2 扩容拆链的实测输出
├── day3-result.txt                                 Day 3 HashSet / LinkedHashMap 的实测输出
├── day6-result.txt                                 Day 6 手写 MyHashMap 的实测输出（含与 JDK 对拍）
└── src/main/java/com/newt/java48/w01/
    ├── day1/
    │   ├── ArrayListVsLinkedList.java              7 项操作的真实耗时对比 + 内存占用
    │   └── MyArrayList.java                        手写动态数组，打印扩容现场
    ├── day2/
    │   ├── HashMapProbe.java                       HashMap put 全流程：扰动 / 桶定位 / 冲突 / 扩容
    │   ├── HashMapBuAnQuan.java                    多线程 put 丢数据的最小复现 + 成环检测
    │   ├── HashMapKuoRong.java                     扩容的两条触发路径（size / 链表太长）
    │   └── HashMapChaiLian.java                    扩容时桶内链表怎么拆（按 hash & oldCap）
    ├── day3/
    │   └── HashSetLinkedHashMapProbe.java          HashSet 哨兵 value / 去重 / 无序 / LRU / 迭代 / 内存
    └── day6/
        ├── MyHashMap.java                          手写 HashMap：put / get / resize 三段核心
        └── MyHashMapProbe.java                     5 个实验：扩容触发点 / 拆链 / 与 JDK 对拍 / 性能 / 树化退化
```

---

## Day 1（ArrayList / LinkedList）实验清单

| #  | 要验证的结论            | 命令                    | 预期                                                  |
| :- | :---------------- | :-------------------- | :-------------------------------------------------- |
| 1  | 按下标遍历链表是性能事故      | `run.bat`             | `for-i indexed walk` 一行，LinkedList 慢 **上千倍**        |
| 2  | 随机访问链表慢           | `run.bat`             | `get(i) random access`，LinkedList 慢 **上百倍**         |
| 3  | 尾部追加链表居然也不快       | `run.bat`             | `add(E) append`，LinkedList 慢 **2~4 倍**              |
| 4  | 用迭代器遍历时两者差不多      | `run.bat`             | `iterator walk + sum`，比值接近 **1x**                   |
| 5  | 头部插入删除链表才真正赢      | `run.bat`             | `add(0,E)` / `remove(0)` 两行，LinkedList 快 **十几~几十倍** |
| 6  | 中间插入链表又输了         | `run.bat`             | `add(mid,E)`，因为**先要 O(n) 找到那个位置**                   |
| 7  | 同样 100 万元素，链表更费内存 | `run.bat`             | 约 **5.7 倍**（Node 对象头 + prev/next 两个指针）              |
| 8  | 扩容是 1.5 倍，不是 2 倍  | `run.bat MyArrayList` | `10 -> 15 -> 22 -> 33 -> 49 -> 73 -> 109`           |

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

| #  | 要验证的结论       | 命令                          | 预期                                                          |
| :- | :----------- | :-------------------------- | :---------------------------------------------------------- |
| 1  | key 怎么变成桶下标  | `run.bat day2.HashMapProbe` | "hello" → hashCode `99162322` → 扰动后 `99163451` → 桶下标 **11** |
| 2  | 不做扰动会全挤一个桶   | 同上                          | 4 个低位相同的 hash，**不扰动全落在下标 1**，扰动后分散到 0/3/2/5                 |
| 3  | 哈希冲突长什么样     | 同上                          | `"Aa"` 与 `"BB"` hashCode 都是 **2112**，同桶但 equals 为 false     |
| 4  | 什么时候扩容       | 同上                          | 16→13 个触发扩到 32；32→25 个扩到 64；64→49 个扩到 128                   |
| 5  | 扩容后新下标只有两种可能 | 同上                          | 8 个 key 里，要么"不动"，要么"**+16**"（旧容量）                           |
| 6  | put 覆盖时返回什么  | 同上                          | `null` → `100` → `200`；**size 始终是 1**                       |

> 实验 D 的扩容点是 `size > threshold`（严格大于），  
> 所以容量 16、阈值 12 时，第 12 个元素不触发，**第 13 个**才触发。这个边界值面试常问。

## Day 2 补充实验 1：扩容的两条触发路径

| #  | 要验证的结论         | 命令                            | 预期                                                             |
| :- | :------------- | :---------------------------- | :------------------------------------------------------------- |
| 1  | `size` 超阈值触发扩容 | `run.bat day2.HashMapKuoRong` | 容量 64 → 第 **49** 个元素时扩到 128（阈值 48）                             |
| 2  | **链表太长也会触发扩容** | 同上                            | 第 **9** 个 → 16→32；第 **10** 个 → 32→64（此时 size 才 9、10，**远没超阈值**） |
| 3 | **桶 0 的元素扩容后去哪** | `run.bat day2.HashMapChaiLian` | 按 `hash & oldCap` 分：一半留桶 0、一半去桶 16；**hash 全相同时一个都不拆** |

用一个 `hashCode` 恒为 0 的 key，让所有元素挤进同一个桶，于是"桶内链表长度 == size"，  
两个触发条件就会先后出现。

```text
第 9 个   容量 32   ← 扩容 16→32 ｜ 链表到 9 个，容量 < 64，链表触发
第 10 个  容量 64   ← 扩容 32→64 ｜ 同上
第 11 个  容量 64   （已到 64，转为树化）
第 49 个  容量 128  ← 扩容 64→128 ｜ size 49 > 阈值 48，才是 size 触发
```

> 第二条的源码依据是 `treeifyBin()` 的第一行：  
> `if (tab.length < MIN_TREEIFY_CAPACITY) resize();` —— `MIN_TREEIFY_CAPACITY = 64`。  
> 所以「链表到 8 个就变红黑树」**不完整**：容量 < 64 时走扩容，容量 ≥ 64 才树化。

## Day 2 补充实验 2：HashMap 线程不安全复现

| #  | 要验证的结论                     | 命令                                                  | 预期                                                                 |
| :- | :------------------------- | :-------------------------------------------------- | :----------------------------------------------------------------- |
| 1  | 多线程 put，数据会丢               | `run.bat day2.HashMapBuAnQuan`                      | 应放入 40000，真实节点数**总会少几千到一两万**                                       |
| 2  | **`size` 和真实数据是两回事**       | 同上                                                  | `size` 可能**比**真实节点数少，也可能**比它多**（三次实测出现 +12842 / +1388 / **−2026**） |
| 3  | 换成 `ConcurrentHashMap` 就不丢 | 改一行 `new HashMap<>()` → `new ConcurrentHashMap<>()` | 实际 = 40000，丢 **0**                                                 |
| 4  | **JDK 8 并没有彻底修好成环**        | 同上（程序内置环检测）                                         | 约 **15%** 概率踩到：检出环、或直接死循环超时、或树化 `ClassCastException`               |

做法：8 个线程，每个负责**一段互不重叠的 key 区间**，各 put 5000 个。  
互不重叠意味着"理论上一个都不该丢"—— 但只要丢了，就一定是 HashMap 自己的问题。

程序会打印**两个口径**：`size`（HashMap 自己报的）和真实节点数（遍历 `entrySet` 数出来的）。

**实测三次（JDK 17.0.12）**

```text
第 1 次   size 报 23771   真实节点数 36613   真丢 3387    计数偏差 +12842
第 2 次   size 报 27481   真实节点数 28869   真丢 11131   计数偏差 +1388
第 3 次   size 报 24244   真实节点数 22218   真丢 17782   计数偏差 -2026
对照组 ConcurrentHashMap：实际 40000，真丢 0
```

> 注意两点：
>
> 1. **丢多少每次都不一样**，但一定会丢 —— 这种"不稳定"本身就是竞态的指纹。
> 2. **计数器偏差有正有负** —— 正数表示「数据进去了但 `++size` 没算上」，  
>    负数表示「数据被覆盖了但 `++size` 还是加了」。第三次的 −2026 就是后者。  
>    所以 `size` 变小 ≠ 数据变少，`size` 正常 ≠ 数据没丢。

**程序还会检测成环**（Floyd 快慢指针扫 table 的每条链）。跑 13 次的统计：

```text
11 次    无环，但数据照样丢一万多
 1 次    检出 2 个桶成环
 1 次    直接死循环，只能超时杀掉进程
```

> 常见说法「JDK 8 改成尾插就修好了成环」**只对了一半**：  
> JDK 8 修掉的是 JDK 7 那个「必然发生」的自环（头插反转 + 缓存 next），  
> 但 `resize()` 里 `table = newTab` 是在搬运**之前**执行的 ——  
> 两个线程同时扩容时，A 的目标数组会成为 B 的源数组，同一条链被一边写一边读，照样能拼出环。  
> 准确说法：**JDK 8 把成环从「必然」降成了「偶发」，没有消除。**

**最严重的第三种崩法**：桶内链表到 8 个要树化时，另一线程刚好在同一个桶 `put`，  
会抛 `ClassCastException: HashMap$Node cannot be cast to HashMap$TreeNode`。  
实测最狠的一次 8 个线程全崩，40000 条只进去 1542 条。

**为什么只改了 Map 的实现类型就安全了？**  
因为 `ConcurrentHashMap` 用 CAS + synchronized 把每一处临界区都保护起来了  
（`++size` 走 `addCount` 的 CAS、桶头用 `synchronized` 锁一个节点、扩容允许多线程协作搬运）。  
而 `HashMap` 完全没有这些保护，它从头到尾都是为**单线程**设计的。

## Day 3（HashSet / LinkedHashMap）实验清单

| #  | 要验证的结论                                    | 命令                                       | 预期                                                                                                       |
| :- | :---------------------------------------- | :--------------------------------------- | :------------------------------------------------------------------------------------------------------- |
| 1  | HashSet 内部就是一个 HashMap，value 全类共享一个常量     | `run.bat day3.HashSetLinkedHashMapProbe` | `HashSet.map` 类型是 `java.util.HashMap`；3 个元素**只有 1 种 value**（按引用去重），且 `== PRESENT` 为 true                 |
| 2  | `add` 的返回值 = `put() == null`              | 同上                                       | 第一次 `true`，第二次 `false`，`size` 恒为 1                                                                       |
| 3  | 去重靠 hashCode + equals，**缺一不可**            | 同上                                       | 只重写 hashCode → size=**2**；只重写 equals → size=**2**；都重写 → size=**1**                                       |
| 4  | 可变对象当 key 会变"幽灵"                          | 同上                                       | 改字段后 `contains` 变 `false`，但 `size` 还是 1、遍历拿得到它                                                           |
| 5  | 同样的 contains，HashSet 快三个数量级               | 同上                                       | 20 万元素 / 5000 次探测：ArrayList ~**37.6 万 ns/次**，HashSet ~**210 ns/次**，差 **1791 倍**                          |
| 6  | HashSet 的"无序"= 由 hashCode + capacity 共同决定 | 同上                                       | 装 1..12 打印是**升序（假象）**；同样 12 个字符串，容量 16 和 64 顺序**完全不同**                                                   |
| 7  | fail-fast 有个"没被发现"的例外                     | 同上                                       | 遍历中 remove 命中 → 抛 `ConcurrentModificationException`；remove **不存在**的元素 → **不抛**                           |
| 8  | LinkedHashMap 的顺序由 `accessOrder` 决定       | 同上                                       | 默认 = 插入序；`(16,0.75f,true)` 后 `get("charlie")` 会把它挪到**队尾**                                                |
| 9  | LRU 只要 6 行，但 `>` 写成 `>=` 会少存一个            | 同上                                       | 容量 3：`put(d)` 淘汰 a、`put(e)` 淘汰 b；`>=` 版本只留住 **2** 个                                                      |
| 10 | 更重的 LinkedHashMap 迭代**更快**                | 同上                                       | 30 万删到 1 千后 capacity 仍是 **524288**：HashMap 每轮 **749,910 ns** vs LinkedHashMap **35,350 ns**，慢 **21.2 倍** |
| 11 | 那两个 `before`/`after` 指针值多少钱               | 同上                                       | 50 万元素：HashMap **76.7 B/元素**，LinkedHashMap **84.8 B/元素**，差 **8.1 B**（理论 8 B）                             |

**本节最值钱的三个反直觉点**

1. **小整数骗人**：HashSet 装 1..12 打印出来是升序，因为 `Integer.hashCode()` 返回自身、  
   容量 16 时 `n & 15 == n`。换字符串立刻乱 —— 「无序」不等于「随机」。
2. **删"不存在的元素"不抛异常**：`modCount` 只在真的改到结构时才 +1。  
   没报错 ≠ 安全，那个元素一旦存在立刻抛。
3. **更重的 LinkedHashMap 迭代反而快 21 倍**：HashMap 的迭代器要扫完整个 `table`（含 null 桶），  
   成本 O(capacity + size)；LinkedHashMap 只走链表，O(size)。而 `table` 只扩容不缩容。  
   —— 内存开销和遍历性能是两笔独立的账。

> 实验 1 需要反射读 `HashSet.map`，依赖 `run.bat` 里已经带上的  
> `--add-opens java.base/java.util=ALL-UNNAMED`。忘了带也不会崩，只是会打 `reflect = BLOCKED`。

## Day 6（手写 MyHashMap）实验清单

> 只写 `put` / `get` / `resize` 三段，代码 200 行，注释占一半。
> 目标是**能自己从零写出来**，不是复刻 HashMap 的 2000 行。

| #  | 要验证的结论                       | 命令                        | 预期                                                             |
| :- | :--------------------------- | :------------------------ | :------------------------------------------------------------- |
| 1  | 扩容触发是**严格大于**阈值             | `run.bat day6.MyHashMapProbe` | 容量 16/阈值 12：第 12 个不扩，**第 13 个**才扩到 32；25 → 64；49 → 128        |
| 2  | 扩容后下标只有两种命运                | 同上                        | hashCode 1 和 17 在容量 16 时同桶（`&15` 都是 1）；扩容到 32 后分别落到桶 **1** 和桶 **17**（= 1 + oldCap） |
| 3  | 手写的和 JDK 结果**完全一致**         | 同上                        | 20 万次随机 put/get：size 都是 **48609**、容量 **65536/49152**、返回值不一致 **0 处**   |
| 4  | 随机 key 下，手写版甚至略快            | 同上                        | 30 万次 put：手写 **46 ms** vs JDK **77 ms**（0.60 倍），**别当结论，见 5、6**     |
| 5  | hash 全等 + key **不可比较** → JDK 更慢 | 同上                        | 2 万条全挤一个桶：手写 **374 ms** vs JDK **2521 ms**（JDK 慢 6.7 倍）          |
| 6  | hash 全等 + key **可比较** → JDK 反超 | 同上                        | 同样 2 万条：手写 **539 ms** vs JDK **44 ms**（JDK 快 12 倍）              |

**这一节最值钱的三个点**

1. **`resize` 没有重算过一次 hash**：容量翻倍时 `n-1` 只多一位（这一位恰好等于 `oldCap`），
   所以用 `(e.hash & oldCap) == 0` 拆成 lo（留在 `j`）/ hi（去 `j + oldCap`）两条链。
   实测 hashCode 1 和 17：扩容前同在桶 1，扩容后一个留在 1、一个去 17，**一个都没丢**。
2. **「链表到 8 个就转红黑树」少了两个前提**：容量必须 ≥ 64，而且 **key 要能比较大小**。
   E1/E2 对照就是证据：自定义 key 忘了实现 `Comparable` 时，`putTreeVal` 里
   `if (!searched) { ... ch.find(h, k, kc) ... }` 只能对子树做一次全扫，
   每次插入 O(n)，`TreeNode` 又比链表节点重（多 `parent/left/right/prev/red`），结果**比链表还慢**。
3. **微基准的单次数字什么都不能证明**：同一份代码，E1 说「JDK 慢 6.7 倍」、E2 说「JDK 快 12 倍」，
   差别只在于 key 有没有实现 `Comparable`。看数量级、看前提，不看小数点。

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

**Day 3**

1. 先跑 `run.bat day3.HashSetLinkedHashMapProbe`，看实验 A 的三行读数 ——  
   `HashSet.map` 的类型、3 个元素只有 1 种 value、`== PRESENT` 为 true。  
   这三行看懂了，HashSet 就没有秘密了。
2. 实验 C 的三个类（`ZhiXieHashCode` / `ZhiXieEquals` / `DouXie`）是本节的核心：  
   故意"只重写一个"，用 `size=2` 证明两个方法缺一不可，而且**失败方式不同**。
3. 实验 H 的 LRU 用了一个匿名子类 + 一个 `removeEldestEntry`，  
   这是"模板方法模式"最便宜的示例，值得逐行读一遍。
4. 实验 I 的 `tableLength()` 会打印 capacity，用来解释"为什么删到只剩 1000 个还那么慢"。

**Day 6**

1. 先读 `MyHashMap` 的字段：`table` / `size` / `threshold` 三个 + `loadFactor`，
   对照 JDK —— 你会发现 JDK 也差不多就这几个，其余全是辅助。
2. 再读 `put`：四步（懒加载 → 定位 → 冲突/覆盖 → 检查扩容），
   注意覆盖已有 key 时**直接 return，不检查扩容**。
3. 最后读 `resize`，只看 lo/hi 那段 `do-while`。看懂 `(e.hash & oldCap) == 0` 这一行，
   你就把 JDK 里最长的那段 `resize` 拿下了。
4. 写完再回头看 `MyHashMapProbe` 的实验 C —— 对拍才是真正的验收，自己说"我觉得对"不算。


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


```

> 每次跑数字都会有浮动（GC、JIT、CPU 频率），**要看的是数量级，不是小数点**。
> 自己在自己机器上再跑一遍，对比一下——如果某项和上表差了一个数量级，那是值得追下去的问题。
```
