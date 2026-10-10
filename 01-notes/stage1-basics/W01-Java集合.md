# W01 · Java 集合

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）  
> 状态：🟨 进行中　｜　开始日期：2026-09-22　｜　完成日期：\____-**-**

> 💡 目标：不只是会用 HashMap，要能解释它。

---

## 一、本周知识点

- [x] ArrayList / LinkedList ← **Day 1（2026-09-22）**
- [x] HashMap 基本结构 ← **Day 2（2026-10-09）**
- [x] HashMap 源码与扩容 ← **Day 2（2026-10-09）**
- [x] HashSet / LinkedHashMap ← **Day 3（2026-10-10）**
- [ ] 集合面试题
- [ ] 手写简易 HashMap

---

## Day 1 · 2026-09-22（周二）ArrayList / LinkedList

> 第一天把「周一学新」和「周二写代码」合并了，因为本周的周一没赶上。  
> 从明天起按原表走：周三 = HashMap 源码与扩容。

### 1. 时间盒（60 分钟，实际用了 75）

| 段       | 时长 | 做什么                                              | 结果                    |
| :------ | -: | :----------------------------------------------- | :-------------------- |
| 复习 / 热身 |  5 | 明确定义：集合解决的是「一组数据怎么存、怎么取」                         | 写下「动态数组 vs 双向链表」两组关键词 |
| 学新（理论）  | 30 | 两种结构的内存布局、扩容公式、`size` vs `length`                | 见下面「知识点」              |
| 写代码     | 30 | 跑 `02-code/stage1-basics/w01-collections/`，看真实数字 | `day1-result.txt`     |
| 输出      | 10 | 填完「核心问题」和「一句话总结」                                 | 本节                    |

### 2. 知识点（用自己的话说一遍，不要抄）

**ArrayList —— 一个会自己长个的数组**

- 底层就是 `Object[] elementData`，内存**连续**。
- 两个容易搞混的东西：
  - `size`：装了几个元素。（是「有几个」）
  - `elementData.length`：这个数组能装几个。（是「能装几个」）
- 构造时**不分配**数组（懒加载）。`new ArrayList<>()` 拿到的是空数组，第一次 `add` 才给 10 个位置。
- 扩容公式就一行：`newCapacity = oldCapacity + (oldCapacity >> 1)`，也就是 **1.5 倍**，然后 `Arrays.copyOf` 全量拷贝。
- 复杂度：按下标取 `get(i)` 是 O(1)；尾部 `add` 摊还是 O(1)；**中间插入 / 删除是 O(n)**，因为要把后面的元素整体搬家。
- 删元素之后**不缩容**——缩容又是一次 O(n) 拷贝，而删完通常还会再加回来。

**LinkedList —— 一堆互相牵着手的 Node**

- 每个元素被包成一个 `Node { prev, item, next }`，内存**不连续**。
- 没有下标。`get(i)` 要一个节点一个节点挪过去，是 O(n)。  
  它有个小优化：`i < size/2` 从头找，否则从尾找——所以平均走 n/4 个节点，**量级还是 O(n)**。
- 头尾增删是 O(1)（前提是你已经站在那个节点上）；按**下标**增删要先 O(n) 找位置。
- 每个元素额外背着一个 Node 对象 —— 这是它费内存的根源。

**把两者放在一起看（这才是今天的重点）**

|                   | ArrayList             | LinkedList                |
| :---------------- | :-------------------- | :------------------------ |
| 内存布局              | 连续数组                  | 离散节点 + 指针                 |
| `get(i)`          | O(1)                  | O(n)                      |
| 尾部 `add`          | 摊还 O(1)（偶尔扩容 O(n)）    | O(1)（但要 new Node）         |
| 中间 `add`/`remove` | O(n) 搬家               | O(n) 找位置（找到后 O(1) 改指针）    |
| 每个元素的额外开销         | 1 个数组槽位（4/8 字节引用）     | 1 个 Node 对象（约 24 字节 + 指针） |
| CPU 缓存友好度         | 好（顺序访问能打满 cache line） | 差（指针追逐，每次都像一次随机内存访问）      |

### 3. 代码 / 实验

- 代码目录：`02-code/stage1-basics/w01-collections/`
- 一键运行：`run.bat`（或 `./run.sh`）
- 本机实测结果原件：`02-code/stage1-basics/w01-collections/day1-result.txt`

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

**三个反直觉的读数，值得单独记一笔**

1. `append` 链表慢了 3.66 倍 —— 说好的「链表插入 O(1)」呢？因为每次 `add` 都要 `new Node`，  
   而且新节点和上一节点不在同一块连续内存里，写指针就可能 miss 缓存。  
   「O(1)」讲的是**指令条数**，不是**墙上时间**。
2. `iterator walk + sum` 两者几乎一样（0.93x）—— LinkedList 的慢**不在遍历**，在 `get(i)`。  
   用增强 for 循环遍历链表是没问题的，`for (int i=0; i<list.size(); i++) list.get(i)` 才是事故。
3. `add(mid,E)` 链表又慢了 15.67 倍 —— 因为它要先 O(n) 走到中间那个位置。  
   **链表插入"快"，前提是"已经拿到了那个节点"**，而这个前提在日常业务代码里几乎不成立。

### 4. 扩容器观察（`run.bat MyArrayList`）

```text
[grow #1]  0 -> 10    (size=0,  need=1)
[grow #2]  10 -> 15   (size=10, need=11)
[grow #3]  15 -> 22   (size=15, need=16)
[grow #4]  22 -> 33   (size=22, need=23)
[grow #5]  33 -> 49   (size=33, need=34)
[grow #6]  49 -> 73   (size=49, need=50)
[grow #7]  73 -> 109  (size=73, need=74)
```

装 100 个元素：**扩容 7 次，累计多拷了 202 次引用，最后多占了 9 个空槽**。  
换成 `new ArrayList<>(100)`：**扩容 0 次**。这就是「顺手写初始容量」的价值。

> 为什么是 1.5 倍而不是 2 倍？  
> 2 倍时 `8 -> 16 -> 32`，每次释放的旧空间（8、16）永远凑不出下一次要的 32，(1+2+4+8) 永远差 1；  
> 1.5 倍时 `10 -> 15 -> 22 -> 33`，前几次释放的空间有机会拼成后面要的大小，内存更容易被复用。  
> 另外 1.5 倍的摊还成本更平稳，不会出现特别长的停顿。

---

## Day 2 · 2026-10-09（周五）HashMap 结构与 put 全流程

> 断档 17 天后重启。按里程碑制的规则，**不补课**，直接接 Day 2。

### 0. 顺带解决的一个疑问：`size` 的初始值在哪？

翻 `MyArrayList` 时发现 `private int size;` 找不到赋值的地方 —— 因为**根本不用赋值**：

- Java 的**成员变量**由编译器补默认值：`int` → `0`，引用类型 → `null`，`boolean` → `false`。
- JVM 分配对象内存时会把整块内存**归零**，这一步在构造函数第一行**之前**。  
  所以构造函数里的 `this.elementData = new Object[0];` 是**覆盖** `null`，不是第一次赋值。
- 对照：**局部变量**必须手动初始化，否则编译报错（`variable a might not have been initialized`）。

> 连带记一条：`new int[10]` 是 `[0,0,...]`，`new Integer[10]` 是 `[null,...]`。  
> 基本类型数组归零成 `0`，引用数组归零成 `null` —— 隐蔽 Bug 的常见来源。

### 1. 知识点

**put 的完整链路（JDK 8）**

```text
put(key, value)
   ↓
① hash(key)          = (h = key.hashCode()) ^ (h >>> 16)     ← 扰动
   ↓
② 定位桶             = (n - 1) & hash                        ← 等价于 hash % n，但前提 n 是 2 的幂
   ↓
③ 桶为空             → 直接放新 Node
   桶不为空           → 比较 hash 和 equals
                        相同 key → 覆盖 value，返回旧值
                        不同 key → 挂到链表尾（尾插，JDK 8；JDK 7 是头插）
   ↓
④ 链表长度 ≥ 8 且容量 ≥ 64 → 树化成红黑树（< 6 退化为链表）
   ↓
⑤ ++size > threshold → resize()，容量翻倍，threshold = capacity * 0.75
```

**三个必须能解释的点**

1. **为什么要扰动 `h ^ (h >>> 16)`？**  
   `(n-1) & hash` 只取 hash 的**低位**（容量 16 时只看低 4 位）。  
   如果一批 key 的 hashCode 高位不同、低位相同，不扰动就会全挤进一个桶，链表退化 O(n)。  
   异或 `h >>> 16` 把高 16 位掺进低 16 位，让低位也带上高位的随机性。成本只有一次移位 + 一次异或。
2. **为什么容量必须是 2 的幂？**  
   因为 `(n-1) & hash` 只有在 n 是 2 的幂时才等价于 `hash % n`。  
   取模指令慢，位运算快 —— 这是"用空间形态换计算速度"。  
   也解释了 `new HashMap<>(17)` 会被 `tableSizeFor` 向上取整成 32。
   1. **扩容后元素的新位置为什么只有两种可能？**  
      容量从 16 变 32，`n-1` 从 `0b1111` 变成 `0b11111`，**只多了一位**。  
      所以 `(新n-1) & hash` 相比旧值，要么不变，要么加上 16（旧容量）。  
      JDK 8 因此不重算 hash，只看"新增的那一位"是 0 还是 1，把一条链拆成 lo / hi 两条 —— 这是相对 JDK 7 的主要性能优化之一。

      **补：JDK 7 为什么不行？**  
      JDK 7 扩容要对每个元素**重新计算下标**，而且插入用**头插法**。  
      头插在并发扩容时会让链表**成环** → `get()` 死循环 → CPU 打满，  
      这就是著名的「HashMap 并发死循环」问题。JDK 8 改成尾插 + 拆链，规避了它。  
      但根本结论不变：**HashMap 天生不支持并发，要并发用 `ConcurrentHashMap`。**
   2. **put 返回值有个隐蔽的坑**  
      `put` 返回被覆盖的旧值，首次插入返回 `null`；  
      但如果**插入的 value 本身就是 `null`**，返回值也是 `null` —— 两种情况无法区分。  
      要"仅当不存在才写入"，应该用 `putIfAbsent` / `computeIfAbsent`：
      ```java
      map.putIfAbsent("a", 99);              // 存在则不动，返回旧值
      map.computeIfAbsent("b", k -> 100);    // 不存在才计算并放入
      ```

**扩容其实有两条触发路径（第二条大多数人不知道）**

| # | 触发条件 | 什么时候发生 |
| :--- | :--- | :--- |
| ① | `++size > threshold`（阈值 = 容量 × 0.75） | 元素总数超了承载能力。容量 16 → 第 **13** 个触发；32 → 第 **25** 个；64 → 第 **49** 个 |
| ② | 桶内链表变长，**且容量 < 64** | 个别桶太挤。此时**不树化，而是扩容** |

第 ② 条的源码依据在 `treeifyBin()` 的第一行：

```java
if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
    resize();          // 容量还没到 64？先扩容，别急着树化
else
    ...树化...
```

`MIN_TREEIFY_CAPACITY = 64`。所以「链表到 8 个就变红黑树」这句话**不完整** ——
容量 < 64 时走的是**扩容**，容量 ≥ 64 才真的树化。
设计意图：桶不够多的时候，冲突多半是「桶太少」造成的，拆桶比树化更划算。

**实测（`HashMapKuoRong.java`，用 hashCode 恒为 0 的 key 制造极端冲突）**

```text
第 8 个   容量 16   阈值 12   size 8
第 9 个   容量 32   ← 扩容 16→32 ｜ 链表变到 9 个，容量 < 64，链表触发（size 9 还没超阈值 12！）
第 10 个  容量 64   ← 扩容 32→64 ｜ 同上（size 10 也没超阈值 24）
第 11 个  容量 64   （已到 64，不再扩容，转为树化）
  ...
第 49 个  容量 128  ← 扩容 64→128 ｜ size 49 > 阈值 48，这回才是 size 触发
```

> **反直觉点**：16 → 32、32 → 64 这两次，在**这个实验里都是「链表触发」的**，
> 根本不是 size 到 13、25 触发的 —— 因为所有 key 挤在同一个桶，链表很快就超了。
> key 分布均匀时，走的才是「第 13 个 / 第 25 个」那条路。
> **两条路最终都调同一个 `resize()`，所以容量永远是 2 的幂。**

**扩容时，桶里的链表怎么分？（实测）**

扩容不重算 hash，只看 `hash & oldCap` 这一位：

| `hash & oldCap` | 去哪 |
| :--- | :--- |
| 0 | 留在原下标（lo 链） |
| 非 0 | 移到 原下标 + oldCap（hi 链） |

所以「桶 0 里的元素扩容后去哪」**不是固定的** —— 要看每个元素自己的 hash 在第 5 位上是 0 还是 1。

**实测（`HashMapChaiLian.java`）**

实验 A：4 个 key 的 hash 取 `0x00 / 0x10 / 0x20 / 0x30`，低 4 位全是 0 → 容量 16 时全挤在桶 0。

```text
扩容到 32 后：
  k0(hash=0x00) → 桶 0    （hash & 16 == 0  → 留）
  k1(hash=0x10) → 桶 16   （hash & 16 == 16 → 走）
  k2(hash=0x20) → 桶 0    （留）
  k3(hash=0x30) → 桶 16   （走）
```

桶 0 被劈成了「桶 0」+「桶 16」两条链。

实验 B：**如果桶里所有元素的 hash 完全相同，就拆不动了** ——

```text
用 hashCode 恒为 0 的 key 塞 11 个：
  第 9 个   容量 16 → 32    仍在桶 0
  第 10 个  容量 32 → 64    仍在桶 0
  第 11 个  容量 64         仍在桶 0
```

容量一路在涨，元素**始终在桶 0**。因为扩容不改变 hash，只是让**更多位**参与下标计算；
hash 高位全是 0 → 新增的位永远是 0 → `(n-1) & 0` 永远是 0。

> **两条结论**
> 1. **不是「第几个元素」决定去哪，是「每个元素自己的 hash」决定去哪。**
>    桶 0 里的第 9 个和第 1 个地位完全一样，各按自己的 hash 归位。
> 2. 同桶元素能被拆开的前提，是它们的 hash 在**新增那一位**上有差异。
>    hash 全相同（比如自己写的 `hashCode` 恒返回常量）时，扩容只会让桶变多，**救不了这个桶**。

**hash 存在哪？为什么扩容不用重算？**

`Node` 里本来就有一个 `hash` 字段，存的是**扰动之后**的值：

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;      // ← 存进去时算过一次，之后一直复用
    final K key;
    V value;
    Node<K,V> next;
}
```

所以扩容拆链是「读现成的」，不是「算」：

```java
do {
    next = e.next;
    if ((e.hash & oldCap) == 0) {     // 读 e.hash 的某一位，一次与运算
        ...挂到 lo 链（留原下标）...
    } else {
        ...挂到 hi 链（+oldCap）...
    }
} while ((e = next) != null);
```

对比 JDK 7：`int i = indexFor(e.hash, newCapacity);` —— 要拿新容量**重新取模**才算得出新下标。

> **"不用重算"省的是"算"（hash + 取模），省不掉"搬"（遍历链表 + 重新串指针）。**
> 拆链本身是 O(n)，所以扩容不便宜。

**同一个 `hash` 字段还有第二个用处：加速 key 比较**

`getNode` / `putVal` 判断「是不是同一个 key」时，先比 hash，再比 equals：

```java
if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
```

先比 hash（一次整数比较，快），hash 相同了才去调 `equals`（可能很慢）。
大多数情况下 hash 就不同，`equals` 直接被短路掉 —— 这是很典型的"用便宜的比较过滤掉绝大多数情况"。

注意：**hash 相同不等于同一个 key**（那就是哈希冲突），所以 `equals` 这一步不能省。

**容量的天花板：2^30，不是 2^32**

直觉上 hash 是 32 位，是不是能扩到 2^32 个桶？**不是。**

```java
static final int MAXIMUM_CAPACITY = 1 << 30;      // 1073741824，约 10.7 亿
```

为什么卡在 2^30：

- 容量必须是 **2 的幂**，所以候选只有 2^30 / 2^31 / 2^32 …
- 桶数组的长度是 `int`，而 `1 << 31` = `Integer.MIN_VALUE`（**负数**），当不了数组长度
- 于是 2 的幂里最大的安全值就是 `1 << 30`

实测（`HashMapKuoRong` 实验 C）：

```text
new HashMap<>(100)                    → 实际容量 128
new HashMap<>(1000)                   → 实际容量 1024
new HashMap<>(1073741823)             → 实际容量 1073741824
new HashMap<>(1073741824)             → 实际容量 1073741824
new HashMap<>(Integer.MAX_VALUE)      → 实际容量 1073741824   ← 被截断到 2^30
```

到达上限后，`resize()` 会把 `threshold` 设成 `Integer.MAX_VALUE`，**从此不再扩容**。

现实约束：2^30 个引用 ≈ 4 GB（压缩指针）～ 8 GB，普通机器根本分配不出来 —— 所以这个上限更多是理论边界。

> 顺带一个细节：容量到 2^30 时 `n-1 = 0b0111...1111`（30 个 1），**只用到 hash 的低 30 位**，
> hash 的最高 2 位永远闲置。这也说明 `h ^ (h >>> 16)` 的扰动设计假设了「容量不会太大」——
> 实际业务里容量基本不会超过 2^16，所以把高 16 位掺进低 16 位就够用了。

### 2. 代码 / 实验

- 新增：`src/main/java/com/newt/java48/w01/day2/HashMapProbe.java`
- 运行：`run.bat day2.HashMapProbe`（脚本已改成支持带包名）
- 实测原件：`day2-result.txt`

**六个实验的关键读数**

```text
实验 A  "hello" → hashCode 99162322 → 扰动后 99163451 → 桶下标 11（容量 16）

实验 B  4 个低位全为 1 的 hash：
          不扰动 → 全部落在下标 1（全撞一起）
          扰动后 → 分别落到 0 / 3 / 2 / 5

实验 C  "Aa" 和 "BB" 的 hashCode 都是 2112 → 同桶，但 equals 为 false → 是两个 key

实验 D  容量 16 → 第 13 个元素扩容到 32（阈值 12）
        容量 32 → 第 25 个元素扩容到 64（阈值 24）
        容量 64 → 第 49 个元素扩容到 128（阈值 48）

实验 E  旧容量 16 → 新容量 32，8 个 key 里：5 个 "+16"，3 个"不动"

实验 F  put("k",100) → null      size 1
        put("k",200) → 100       size 1（覆盖不算新增）
        put("k",null) → 200      get("k") = null
```

### 3. 补充实验：HashMap 线程不安全的最小复现

- 新增：`src/main/java/com/newt/java48/w01/day2/HashMapBuAnQuan.java`
- 运行：`run.bat day2.HashMapBuAnQuan`
- 实测原件：`day2-buanquan-result.txt`

**做法**：8 个线程，每个负责**一段互不重叠的 key 区间**，各 `put` 5000 个 ——  
互不重叠就意味着「理论上一个都不该丢」，丢了就一定是 HashMap 自己的问题。

**实测（JDK 17.0.12，跑三次）**

```text
第 1 次   应放入 40000   size 报 23771   真实节点数 36613   真丢 3387    计数偏差 +12842
第 2 次   应放入 40000   size 报 27481   真实节点数 28869   真丢 11131   计数偏差 +1388
第 3 次   应放入 40000   size 报 24244   真实节点数 22218   真丢 17782   计数偏差 -2026
对照组 ConcurrentHashMap：  实际 40000，真丢 0
```

> **丢多少每次都不一样，但一定会丢** —— 这个「不稳定」本身就是竞态的指纹。  
> 如果它是线程安全的，跑一万次也该是 40000。

**关键区分：`size` 和「真实数据」是两回事**

这是最容易搞混的一点。`size` 只是一个 `int` 计数器，  
它和 table 里真实挂着的节点**没有任何强一致保证**，而且**两个方向都会偏**：

| 口径     | 怎么算               | 说明                      |
| :----- | :---------------- | :---------------------- |
| 真实丢的数据 | 应放入 − 遍历数出的节点数    | 这些 key 用 `get()` 已经查不到了 |
| 计数器偏差  | 遍历数出的节点数 − `size` | 计数器自己算错了多少              |

**为什么会偏？看 `putVal` 的执行顺序**（JDK 8 源码，简化）：

```java
tab[i] = newNode(hash, key, value, null);   // 第一步：把节点写进桶
...
++size;                                     // 第二步：计数器 +1
```

两步之间没有原子性保证，于是：

- **情形 A**：两个线程写**不同桶**（互不冲突）  
  → 数据两个都进得去，但 `++size` 撞车只 +1  
  → 结果：**只丢 size，数据没丢**（偏差为**正**）
- **情形 B**：两个线程写**同一个空桶**  
  → 都读到 `tab[i] == null`，各建节点，后写的把先写的盖掉  
  → 但两次 `++size` 可能都成功  
  → 结果：**数据丢了，size 反而多算**（偏差为**负**）

实测三次里，前两次偏差为正（+12842 / +1388，都是「数据在、计数器没算上」），  
第三次偏差为负（−2026，「数据被覆盖、计数器却还是加了」）—— **两种都出现了，不是理论推演。**

**所以三条结论**

1. `map.size()` 变小，**不代表数据一定少**（可能只是计数器算错）
2. `map.size()` 正常，**不代表数据没丢**（情形 B 会让它虚高）
3. 想判断数据到底丢没丢，**只能遍历数节点**  
   （用 `entrySet()` 走一遍；别用 `entrySet().size()`，那还是返回 `size` 字段）

> 面试加分点：能主动说出「size 和实际数据是两回事，size 甚至可能比真实数据多」，  
> 比只说一句「HashMap 线程不安全」高一个段位 —— 说明你看过 `putVal` 的执行顺序。

**丢在哪？`HashMap.put` 里至少三处竞态**

| #  | 竞态点        | 具体怎么丢                                       |
| :- | :--------- | :------------------------------------------ |
| ①  | `++size`   | 读-改-写三步，不是原子操作。两个线程同时写，会丢掉一次增量              |
| ②  | `resize()` | 两个线程同时扩容，各建一个新数组，后完成的把先完成的整个覆盖掉             |
| ③  | 同桶覆盖       | 两个线程同时 put 到同一个空桶，都读到 `null`，各建一个节点，一个盖掉另一个 |

**为什么换成 `ConcurrentHashMap` 就安全了**

它把上面三处都保护起来了：

- 计数走 `addCount()` 的 **CAS/LongAdder**，不再用裸 `++`
- 桶头用 **`synchronized` 锁单个节点**（锁粒度细到桶）
- 扩容支持**多线程协作搬运**，且有 `sizeCtl` 标记位防止并发重复扩容

而 `HashMap` 从头到尾都是给**单线程**设计的，没有任何一处同步。

> 面试怎么答：「HashMap 线程不安全」不能只答这一句，要能说出**具体丢在哪**（上面三条），  
> 再补一句「JDK 7 还会因为头插法在并发扩容时成环，导致 `get()` 死循环；JDK 8 改成尾插规避了成环，但数据丢失依然存在」。  
> 最后给出方案：**并发场景一律用 `ConcurrentHashMap`**，  
> `Collections.synchronizedMap` 只能保证单次操作原子，遍历仍要自己加锁。

### 4. 踩坑：JDK 9+ 反射读 HashMap 内部字段会被模块系统拦住


第一次运行直接抛：

```text
java.lang.reflect.InaccessibleObjectException: Unable to make field
transient java.util.HashMap$Node[] java.util.HashMap.table accessible:
module java.base does not "opens java.util" to unnamed module @85ede7b
```

**原因**：JDK 9 引入模块系统（JPMS）后，`java.base` 模块默认不对外 `opens` 内部包，  
`setAccessible(true)` 会被拒绝 —— 这是**故意设计的封装保护**，不是 Bug。

**解决**（两条路都留了）：

1. 启动参数显式开一个口子（已写进 `run.bat` / `run.sh`）：
   ```bash
   java --add-opens java.base/java.util=ALL-UNNAMED -cp out ...
   ```
2. 代码里先做一次性探测（`probeReflect()`），拿不到内部状态就退回**用 size 推算容量**  
   （容量 16 装到 12、32 装到 24……），保证没加参数时程序也不崩。

> 顺带理解 `--add-opens` / `--add-exports` 的区别：  
> `--add-opens` 允许**反射读写**（含 `setAccessible`），`--add-exports` 只允许**编译期/运行期直接访问**。  
> 要反射内部字段，必须用 `--add-opens`。

---

## Day 3 · 2026-10-10（周六）HashSet / LinkedHashMap

> `MILESTONES.md` 的任务 #5 本来标着"砍掉 —— 知道是包装 HashMap 即可"。
> 实际做成了完整一节 + 10 个实验，因为这两块**不是两个新容器，而是 HashMap 的两种复用方式**：
> 一个把 value 抽掉变成 Set，一个给节点缝上一条双向链表。
> 这一节看完，Day 2 的 HashMap 才算真懂。

### 1. 先把四个容器摆在一起

| 容器             | 本质                          | 比 HashMap 多出来的东西        | 迭代顺序     | 单元素查找    |
| :------------- | :-------------------------- | :---------------------- | :------- | :------- |
| `HashSet`      | 一个 `HashMap`，元素全挂在 key 上    | 无                       | 无序       | O(1)     |
| `LinkedHashSet` | 一个 `LinkedHashMap`          | 双向链表                    | 插入序      | O(1)     |
| `TreeSet`      | 一个 `TreeMap`                | 红黑树                     | 按 key 排序 | O(log n) |
| `LinkedHashMap` | `extends HashMap`           | `before` / `after` 两个指针 | 插入序 或 访问序 | O(1)     |

**一句话**：Set 家族全是 Map 换皮，差别只在"底层用哪种 Map"。

### 2. HashSet：三个单行方法就是全部秘密

源码（JDK 17，`java.base/java/util/HashSet.java`）：

```java
private transient HashMap<E, Object> map;              // 第 100 行附近：唯一的存储
private static final Object PRESENT = new Object();    // 一个全类共享的哨兵常量

public boolean add(E e)           { return map.put(e, PRESENT) == null; }   // 221 行
public boolean contains(Object o) { return map.containsKey(o); }            // 205 行
public boolean remove(Object o)   { return map.remove(o) == PRESENT; }      // 237 行
public int size()                 { return map.size(); }
```

**实验 A 读数**（反射掏出 `HashSet.map` 字段看内部）

```text
HashSet.map 的真实类型   : java.util.HashMap
value 的运行时类型        : java.lang.Object
不同 value 的个数(按引用)  : 1     ← 3 个元素，只有 1 个 value 对象
value 就是 PRESENT 常量吗 : true
```

**为什么 value 可以全类共享一个对象？**
HashSet 从不暴露 value，所以它不需要"每个元素配一个 value"。
若真给每个元素 `new Object()` 当 value，100 万元素就多背 100 万个对象头（约 16 字节）≈ **16 MB 纯浪费**。
共享一个 `static final` 常量是零成本优化 —— 这就是 `PRESENT` 存在的全部理由。

**实验 B：返回值语义**

```text
add("a") 第一次 -> true    size=1
add("a") 第二次 -> false   size=1     ← 重复元素被吃掉
add("a") 第三次 -> false   size=1
remove("a") 第一次 -> true
remove("a") 第二次 -> false
```

`add` 返回的 boolean，本质就是 `map.put(e, PRESENT) == null`：
`put` 返回 null 说明桶里原本没这个 key → 确实是新增。

> 这里正好是 Day 2 那个坑的**反面教材**：
> `HashMap.put` 返回 null 时你分不清"首次插入"还是"插进去的 value 本身是 null"；
> HashSet 的 value 恒为非 null 的 `PRESENT`，天然没有这个歧义。

**顺手一条源码阅读**：`new HashSet<>(已有集合)` 会先按 `c.size()/0.75f + 1` 预分配容量再 `addAll`
（JDK 17 源码第 120 行），避免构造过程中反复扩容。
被问"`new HashSet<>(list)` 相比逐个 add 有什么好处"时可以答这个。

### 3. 去重：hashCode 和 equals 缺一不可（实验 C）

同一组数据 `(1,2)` 塞两遍，三种重写方式的结果完全不同：

| 重写了什么             | size  | 为什么                                                          |
| :---------------- | :---: | :----------------------------------------------------------- |
| 只有 `hashCode`     | **2** | 两个对象落进同一个桶，但桶内逐个 `equals` 用的是 `Object` 默认实现（地址比较）→ 都不相等 |
| 只有 `equals`       | **2** | `equals` 是 true，但两个对象用**默认 hashCode（地址哈希）**，根本分不到同一个桶       |
| 两个都重写             | **1** | 这才是对的                                                        |

口诀：**hashCode 决定"分到哪个桶"，equals 决定"桶里是不是同一个"。**

**反直觉点（C4）：可变对象当 key 会变成"幽灵"**

```text
放入 (x=1) 后 contains -> true    size=1
把 x 改成 2 后 contains -> false   size=1
但直接遍历还能看见它            -> KeBian{x=2}
```

对象还在集合里（`size` 没变、遍历拿得到），`contains` 却永远 false ——
因为它现在拿着**新的 hashCode** 去别的桶里找了。

**结论：HashSet 的 key 必须是不可变对象**（String / Integer / 枚举 / record）。
把可变对象塞进 Set 或当 Map 的 key，是生产环境里的经典幽灵 Bug。

### 4. 性能：同样的 contains，差了三个数量级（实验 D）

200,000 个元素，随机探测 5,000 次（JDK 17.0.12，实测）：

| 实现                  | 总耗时(ns)        | 每次(ns)      |
| :------------------ | -------------: | ----------: |
| `ArrayList.contains` | 1,879,621,200 | **375,924** |
| `HashSet.contains`   | 1,049,500     | **209.9**   |
| 倍数                  |                | **1791 倍**  |

`List.contains` 是 O(n) 逐个 equals，`HashSet.contains` 是 O(1) 均摊（定位桶 + 桶内少量比较）。
**业务里"判断这个 id 处理过没有"如果写成 `List.contains`，数据量一上来就是灾难。**
这是本节最实用的一条。

### 5. "无序"的准确含义（实验 E）

```text
E1  装 1..12 打印 : [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
```

**陷阱**：看着完全有序。因为 `Integer.hashCode()` 返回自身，
容量 16 时 `n & 15 = n`（n < 16），于是"桶下标 = 数值"，自然从小到大。
**别用小整数验证 HashSet 的有序性，会得出完全错误的结论。**

换成字符串立刻露馅 —— 同样 12 个词、同样插入顺序，**只改容量**：

```text
插入顺序              : delta alpha charlie bravo echo golf hotel india juliet kilo lima mike
new HashSet<>(16)     : bravo golf juliet kilo lima mike alpha delta hotel echo india charlie
new HashSet<>(64)     : mike delta echo india bravo golf kilo alpha hotel lima juliet charlie
```

结论：**无序 ≠ 随机，而是"顺序由 hashCode 和当时的 capacity 共同决定，不受你控制"。**
迭代顺序 = 桶下标升序 + 桶内链表顺序，而桶下标 = `hash & (capacity - 1)`：
capacity 一变大，`&` 取的位数就变了，顺序自然全乱。

想要顺序就用 `LinkedHashSet`（插入序）或 `TreeSet`（排序）。

### 6. 遍历中改集合：fail-fast 和它的例外（实验 F）

```text
F1  增强 for + remove(命中)     -> ConcurrentModificationException
F2  增强 for + remove(不存在)   -> 正常结束，没抛
F3  iterator.remove()           -> 正常结束
F4  removeIf("b"::equals)       -> 正常结束
```

机制：集合内部有个 `modCount`，迭代器创建时记下快照，每次 `next()` 校验它有没有被改。

**反直觉点在 F2**：只有**真的改到了结构**（remove 命中 / 新增 / 扩容）时 `modCount` 才 +1。
所以"遍历中删一个不存在的元素"不抛异常 —— **这不是安全，只是恰好没被发现**。
别把"跑起来没报错"当成"这段代码没问题"。

正确写法：`iterator.remove()`（迭代器自己同步 `modCount`），或 JDK 8 的 `removeIf(...)`。

### 7. LinkedHashMap：先搞清楚它有几种"有序"（实验 G）

实现方式：`extends HashMap`，**额外**用一条双向链表把所有节点串起来
（JDK 17 源码里是 `head` / `tail` 两个字段，节点多了 `before` / `after` 指针）。
迭代时走链表，不是走桶数组。

关键是三个钩子 —— HashMap 在 `put` / `get` / `remove` 收尾处各留了一个**空方法**：

```java
// HashMap 里的空实现（父类定义骨架）
void afterNodeAccess(Node<K,V> p)      { }
void afterNodeInsertion(boolean evict) { }
void afterNodeRemoval(Node<K,V> p)     { }

// LinkedHashMap 重写它们来维护链表（子类只补自己那部分）
void afterNodeRemoval(Node<K,V> e)     { ... unlink ... }              // 第 284 行
void afterNodeInsertion(boolean evict) { ... possibly remove eldest ... } // 第 298 行
void afterNodeAccess(Node<K,V> e)      { ... move node to last ... }   // 第 306 行
```

这是教科书级的**模板方法模式**：HashMap 一行都不用改，就凭空获得了"顺序"能力。

顺序由构造函数第三个参数 `accessOrder` 决定：

```text
插入顺序                : delta alpha charlie bravo echo golf
HashMap                 : bravo golf alpha delta echo charlie      ← 无序
LinkedHashMap(默认)     : delta alpha charlie bravo echo golf      ← 插入序
LinkedHashMap(访问序)   : delta alpha charlie bravo echo golf      ← 还没访问，暂时一样

get("charlie") 之后：
    LinkedHashMap(默认)   : 没动
    LinkedHashMap(访问序) : delta alpha bravo echo golf charlie     ← charlie 被挪到队尾
```

**反直觉点：`accessOrder = true` 时，`get()` 会修改结构**（重排链表 + `modCount++`）。
也就是说这个 `get` 不是只读操作：
① 并发下比普通读更容易出问题；② 读一次就会改变后续的迭代顺序。
而 ② 正是 LRU 的立身之本。

### 8. 用 LinkedHashMap 手写 LRU —— 只要 6 行（实验 H）

```java
int capacity = 3;
Map<String, Integer> lru = new LinkedHashMap<String, Integer>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
        return size() > capacity;      // 超了就淘汰队首（最久未使用）
    }
};
```

实测（容量 3）：

```text
put a, b, c  → [a, b, c]
put d        → 淘汰 a         → [b, c, d]
put e        → 淘汰 b         → [c, d, e]
get("d")     → 顺序变 [c, e, d]     d 变成"最新"
put f        → 淘汰队首 c     → [e, d, f]   ← 刚 get 过的 d 保住了，这就是 LRU 要的行为
```

**两个坑**

1. 判断必须写 `size() > capacity`。写成 `>=` 会**少存一个**：实测声明容量 3，实际只留住 2 个 `[d, e]`。
   原因：`removeEldestEntry` 是在**新元素已经插入之后**才被调用的，此刻 `size` 已经 +1。
2. `removeEldestEntry` 只在**插入新 key** 时被调用（走 `afterNodeInsertion`）。
   覆盖已有 key 走的是 `afterNodeAccess` 分支，不触发淘汰 ——
   但访问序会把它挪到队尾，所以语义上仍然正确。

> 现实提醒：JDK 这个 LRU **非线程安全**，而且每次读都改结构。
> 生产里要并发 + 高性能，一般用 Caffeine（W-TinyLFU），别自己拿它硬撑。

### 9. 两个反直觉的收尾数字

**实验 I：更"重"的 LinkedHashMap，迭代反而快 21 倍**

```text
曾经放入 300,000 个，现在各剩 1,000 个
HashMap        table 长度（容量）: 524288     ← table 只扩容、不缩容
LinkedHashMap  table 长度（容量）: 524288

for-each HashMap          总耗时 14,998,200 ns   （每轮  749,910 ns）
for-each LinkedHashMap    总耗时    707,000 ns   （每轮   35,350 ns）
→ HashMap 是 LinkedHashMap 的 21.2 倍
```

原因在 `HashMap` 的迭代器里：它要**从头扫完整个 table 数组，含所有 null 桶**，
成本 **O(capacity + size)**；LinkedHashMap 只沿链表走，成本 **O(size)**。
删元素不缩容，于是 300,000 删到 1,000 之后，HashMap 每次遍历仍要扫 524,288 个桶。

**所以"LinkedHashMap 每节点多两个指针 → 更重 → 更慢"是错的。**
内存开销和遍历性能是两笔独立的账，别混着算。

**实验 J：那两个指针到底值多少钱**

```text
各放 500,000 个 <Integer,Integer>
HashMap         额外堆 36.55 MB    76.7 字节/元素
LinkedHashMap   额外堆 40.43 MB    84.8 字节/元素
差值                              8.1 字节/元素   ← 理论值 8 字节（2 个压缩指针）
```

**8 字节/元素**。50 万元素多花 3.9 MB，换一条能精确控制顺序的链表 —— 很值。

> 这里做的是**差值**而不是绝对值：key 和 value 都装箱了，两边一样，差值才是干净的。
> `Runtime` 读数含 GC 噪声，只看数量级。

### 10. 这一节直接产出的面试题

- HashSet 怎么实现去重？只重写 `hashCode` 会发生什么？（答"会重复"就完了，要讲清**为什么**：桶内 `equals` 用的是地址比较）
- 为什么 HashSet 的 value 是全类共享的一个常量？不共享会怎样？
- HashSet 是无序的，那我放 1..12 进去为什么打印出来是升序的？（**陷阱题**，考你知不知道 `Integer.hashCode()` 和 `hash & (cap-1)`）
- 遍历 Set 时删元素，什么时候会抛 `ConcurrentModificationException`？（答出"删不存在的元素不抛"是加分项）
- 怎么用 JDK 自带类实现 LRU？（`accessOrder=true` + `removeEldestEntry`；再补一句"生产用 Caffeine"）
- LinkedHashMap 怎么在不改 HashMap 一行代码的前提下维护链表？（三个 `afterNodeXxx` 钩子 → 模板方法模式）
- LinkedHashMap 比 HashMap 重，那它迭代更慢吗？（**反直觉**：更快，因为 HashMap 要扫空桶）

### 11. Day 3 · 核心问题（先自己写）

> 答案在上面各小节里，写完再看文末「参考要点」。

**Q1.** `HashSet` 和 `HashMap` 是什么关系？`add` 的返回值是怎么来的？
>

**Q2.** 只重写 `hashCode` 不重写 `equals`，往 `HashSet` 里放两个"逻辑相同"的对象会怎样？反过来呢？为什么两种失败方式不一样？
>

**Q3.** `HashSet` 里放 1..12，打印出来是升序的，这能说明它有序吗？迭代顺序的真正决定因素是什么？
>

**Q4.** 遍历 `HashSet` 时删元素，为什么删"不存在的元素"不抛异常？这算安全吗？
>

**Q5.** `LinkedHashMap` 的插入序和访问序怎么切？为什么说访问序下 `get()` 不是只读操作？
>

**Q6.** 为什么 LinkedHashMap 每个元素更重、占内存更多，但迭代反而比 HashMap 快？

---

## 二、核心问题（能用自己的话脱稿讲出来，才算学会）

> 先自己写。写不出来的先空着，回头翻上面的知识点，**不要直接抄**。  
> 写完再看文末「参考要点」对答案。

1. `size` 和 `elementData.length` 有什么区别？为什么 `new ArrayList<>(0)` 是合法的？
   > size是已填入的数量，elementData.length是数组扩容到的大小，new0时，默认先不创建数组，懒加载。

> 🔍 补充：`new ArrayList<>(0)` 和 `new ArrayList<>()` 用的**不是同一个空数组常量**：
> 前者是 `EMPTY_ELEMENTDATA`，后者是 `DEFAULTCAPACITY_EMPTY_ELEMENTDATA`。
> 区别落在**第一次 `add` 跳到多少**：`new ArrayList<>()` → 跳到 10；`new ArrayList<>(0)` → 只跳到 1。
> 这是 JDK 区分「你没说容量」和「你明确说了 0」的巧劲 —— 两者都不白占 10 个槽位，但补位策略不同。
2. 为什么 `new ArrayList<>()` 之后马上 `add` 第一个元素，容量直接变成 10，而不是 1？
   > 默认值定为10，是大量经验选择的数字，不会频繁扩容也不会建立太大数组。

> 🔍 补充：机制上靠一个**特殊常量**打标记 —— 空参构造把 `elementData` 指向
> `DEFAULTCAPACITY_EMPTY_ELEMENTDATA`，第一次 `add` 时发现是它，才取 `DEFAULT_CAPACITY = 10`。
> 补一句「为什么正好是 10」：它是「空间浪费」和「扩容次数」之间的经验折中，不是推导出来的。
3. 扩容为什么是 1.5 倍？直接 2 倍行不行？
   > 可以复用之前使用的空间，2倍每次2，4，8使用之前的空间永远差1

> 🔍 补充：答得准。再补第二点理由 —— **摊还成本更平稳**。
> 1.5 倍每次挪动的元素少一点，不会出现某一次 `add` 特别长的停顿。
> 面试里「内存复用率」+「摊还平稳」两个点都说到就够了。
4. `list.add(0, x)` 这一行，ArrayList 内部到底做了什么？为什么它是 O(n)？
   > 将数据放到了第一个，原本在数组中的元素需要依次往后移动1位腾位置。

> 🔍 补充：补两个细节。① 搬移用的是 `System.arraycopy`，底层是**整块内存移动**，
> 不是 Java 层逐个循环，所以常数很小（这就是实测里 20k 次才 900ns/op 的原因）；
> ② 这一步之前还有一次 `ensureCapacity` —— 容量刚好满时会先扩容再搬，
> 所以一次 `add(0,x)` 可能同时触发「扩容 O(n)」+「搬移 O(n)」。
5. 实测里 LinkedList 连 `add(E)` 都跑不过 ArrayList，为什么？「O(1)」错了吗？
   > 创建节点会花点时间

> 🔍 补充：只答到三分之一。三个原因叠加：
> ① 每次 `add` 都要 `new Node`（堆分配 + 可能的 GC 压力）；
> ② 新节点和上一节点内存不连续，写 `next` 指针可能触发 cache miss；
> ③ 数组的连续写入能一次填满整条 cache line，链表做不到同一个数量级的吞吐。
>
> **关键在最后一句**：「O(1)」没错 —— 它描述的是**指令条数随 n 的增长方式**，不描述常数因子。
> 面试里主动说出这句，比背「链表插入是 O(1)」高一个段位。
6. 什么场景下你真的该用 LinkedList？举一个业务里可能出现的例子。
   > 需要头插法，获取头部元素时会快一些。

> 🔍 补充：方向要修正一下。
> · `getFirst()` 确实 O(1)，但 `get(i)` 仍是 O(n)，「获取头部元素快」不等于「链表适合读」；
> · 更要紧的是：**需要两端进出时应该用 `ArrayDeque`**，比 `LinkedList` 更快、更省内存
>   （每个元素不用多背一个 Node 对象）；
> · 真正只剩 `LinkedList` 的场景：用 `ListIterator` 边走边在**当前位置**反复增删（实现 LRU、流式处理）。
> 面试里大方说「业务代码里我基本不用它」，比硬编一个场景更可信。

### Day 2 · HashMap 核心问题

> 答案就在上面的「Day 2 知识点」里，先自己写，写不出来再回头翻。

**Q1.** `HashMap` 的 `hash()` 为什么要写成 `h ^ (h >>> 16)`？不做扰动会怎样？

答：按照后四位哈希，如果相同会造成都放到同一个链中

> 🔍 补充：方向对。公式本身是 `h ^ (h >>> 16)` ——
> `>>>` 是**无符号**右移（不能用 `>>`，负数会补 1，结果就不随机了），
> 把高 16 位挪到低 16 位再做异或，让低位也带上高位的随机性。成本只有一次移位 + 一次异或。
> 另外 `key == null` 时直接返回 0 —— 所以 null key 永远落在 0 号桶。

**Q2.** 为什么 HashMap 的容量必须是 2 的幂？`new HashMap<>(17)` 实际容量是多少？

答：可以用(n-1)\&hash代替%运算。容量为32

> 🔍 补充：再补一个反例，记起来更牢 —— 假设容量 15，`n-1 = 0b1110`，
> 最低位恒为 0，与运算后下标永远是**偶数** → 一半的桶永远用不上。
> 只有 `n-1` 是「低位全 1」（即 n 是 2 的幂），`&` 才等价于 `%`。
> 这也解释了 `new HashMap<>(17)` 为什么会被 `tableSizeFor` 向上取整到 32。

**Q3.** 同一个 key 连续 `put` 三次，`size` 分别是多少？返回值分别是什么？

答：1，1，1，返回值为null，第一次存入的值，第二次存入的值

> 🔍 补充：全对。再记一个坑：`put` 返回 `null` 时**无法区分**「首次插入」和「插入了 null value」——
> 这两种情况返回值都是 null。要「仅当不存在才写入」，得用 `putIfAbsent` / `computeIfAbsent`。

**Q4.** 扩容时为什么不用重新计算每个元素的 hash？新下标为什么只有两种可能？

答：扩容为2倍，相当于把链拆成low和high两条链，旧数据补零无需处理数据，新数据直接插到后面

> 🔍 补充：「拆成 lo/hi 两条链」是对的，但「为什么只有两种可能」还没讲透。
> 容量 16 → 32，`n-1` 从 `0b01111` 变成 `0b11111`，**只多了最高那一位**。
> 于是 `(新n-1) & hash` 相比旧值：那一位是 0 → 下标不变；是 1 → 下标 + 16（旧容量）。
> JDK 8 就是拿 `hash & oldCap` 判这一位，所以**不用重算 hash**，只把一条链拆成两条。

**Q5.** JDK 8 把链表头插改成了尾插，为什么？（提示：想想并发扩容时头插会出什么事）

答：避免扩容时成环。

> 🔍 补充：结论对，补上机制和一个反直觉点。
> · JDK 7 的锅在**头插**：`e.next = newTable[i]` 会把顺序反转，而线程 A 醒来后还在用挂起前
>   **缓存的老 `next`** 往前走，于是节点互相指向成环 → `get()` 死循环、CPU 100%。
> · **反直觉点**：JDK 8 只是把成环从「必然」降成「偶发」，**并没有消除**。
>   本机实测 13 次里仍有 1 次检出环、1 次直接死循环（约 15%）。
>   根因是 `resize()` 里 `table = newTab` 在搬运**之前**执行，两个线程会共享同一个数组读写。

**Q6.** `new int[10]` 和 `new Integer[10]` 里的元素分别是什么？为什么？

答：前者是0，后者是null

> 🔍 补充：对。补上「为什么」—— JVM 分配对象/数组内存时会把整块内存**归零**，
> 而基本类型 0 的二进制就是全 0，引用类型的全 0 表示 null。
> 所以 `new int[10]` 是 `[0,0,...]`，`new Integer[10]` 是 `[null,...]`。
> 连带的坑：`int[]` 里「没赋值」和「赋了 0」分不出来，`Integer[]` 里能。

**Q7.** 多线程同时 `put`，数据具体丢在哪几个点？（实测 8 线程 put 4 万条，丢了 1.2~2 万）

答：size会丢，key也会丢。

> 🔍 补充：太笼统，具体是三个点：
> ① `++size` 是读-改-写三步、非原子；② 并发 `resize()` 互相覆盖；
> ③ 两个线程同时写同一个空桶，后写的盖掉先写的。
>
> 还有个更隐蔽的点：**`size` 和「真实数据」是两个独立的量，两个方向都会偏** ——
> 实测出现过 `size` 比真实节点数**多** 2026 个的情况（数据被覆盖了，计数器却照样加了）。
> 所以「size 变小」不能直接推断「数据变少」，反过来也不成立。

**Q8.** 既然会丢数据，为什么 JDK 不把 HashMap 直接做成线程安全的？

答：为了提高性能，并发如果只是访问，无需线程安全，插入的话一般都是单线程，不用担心线程安全问题。

> 🔍 补充：方向对（性能取舍），但「插入一般都是单线程」这句**不严谨** ——
> 实际业务里多线程写 Map 很常见（本地缓存、并发收集结果、并行流里聚合）。
> 更准确的说法：**单线程场景占绝大多数**，一律加锁会带来无谓开销，
> 所以 JDK 把「要不要安全」的选择权交给使用者，同时给出 `ConcurrentHashMap` 这个更好的答案。
> 反面教材是 `Hashtable`：方法级 `synchronized` 锁整张表，并发度极低，早被淘汰。

---

## 三、代码 / 实验（本周其余部分）

- 代码目录：`02-code/stage1-basics/w01-collections/`
- Day 2 计划：HashMap 基本结构 + 源码与扩容
- Day 3 计划：HashSet / LinkedHashMap → 见上面 Day 3 一节
- Day 5 计划：集合面试题 → `04-interview/question-bank/Java基础.md`
- Day 6 计划：手写简易 HashMap

```text
Day 1（已完成 09-22）  动态数组 vs 双向链表：结构、扩容、真实耗时
Day 2（已完成 10-09）  HashMap put 全流程：hash → 扰动 → 下标 → 处理冲突 → 扩容
Day 3（已完成 10-10）  HashSet / LinkedHashMap：PRESENT 哨兵、去重、无序、LRU
Day 6（待做）          手写 MyHashMap：put / get / resize 三段
```

### Day 1 复习与验收

已有两个可运行实验，核心问题的个人回答仍留空。先读 `MyArrayList` 的  
`add → ensureCapacity → grow`，再读 `remove`；最后看性能对比实验。

- [ ] 能区分元素个数 `size` 和数组容量 `capacity`。
- [ ] 运行 `MyArrayList`，解释 `10 → 15 → 22 → 33 → 49 → 73 → 109`。
- [ ] 能解释删除时为什么搬移元素、清空最后一个槽位，却不缩容。
- [ ] 对比 `sumByIndex` 和 `sumByIterator`，解释链表两种遍历方式的差异。
- [ ] 用自己的话补完「核心问题」，不要把一次运行的耗时倍率背成定律。

### Day 2 任务（待做，60 分钟）

当前仓库还没有 Day 2 实验代码。今天先理解 HashMap 的存取流程，完整手写留到 Day 6。

| 时间    | 任务                                                          | 产出                         |
| :---- | :---------------------------------------------------------- | :------------------------- |
| 10 分钟 | 复述 Day 1：数组按下标取值、扩容、链表查找                                    | 回答上面的复习问题                  |
| 15 分钟 | 理解 key、value、桶和哈希冲突；画两个 key 落入同一个桶的情况                       | 一张结构草图                     |
| 20 分钟 | 阅读当前 JDK 的 `HashMap`：`hash → putVal → resize`，再看 `get` 如何查找 | 一张 put 流程图，标出更新旧值和新增节点两条路径 |
| 10 分钟 | 自己写小例子：新增两个键、覆盖一个键、查询存在和不存在的键                               | 打印返回值与 `size`，先预测再运行       |
| 5 分钟  | 用自己的话总结                                                     | 回答下面三个问题                   |

- [ ] HashMap 如何从 key 找到桶？发生冲突后如何区分不同 key？
- [ ] 同一个 key 再次 `put` 时，返回什么？`size` 是否变化？
- [ ] 为什么需要扩容？扩容时原有元素的位置如何处理？

阅读源码时先追踪普通插入、覆盖和扩容分支；树化分支先标出入口，本次不展开红黑树算法。

---

## 四、踩坑与 Bug 复盘

| 现象                                    | 原因                             | 排查过程                                  | 解决              | 底层原理                                                          |
| :------------------------------------ | :----------------------------- | :------------------------------------ | :-------------- | :------------------------------------------------------------ |
| 第一版基准测试结果全是几毫秒的噪声，比值动不动 0.3x / 40x 乱跳 | 没有 JIT 预热，测到的几乎全是类加载 + 解释执行的时间 | 在 `main` 开头加 `warmUp()`，每个方法先小规模跑 3 轮 | 数字稳定下来了，重复跑量级一致 | HotSpot 最先解释执行，热点方法（默认阈值约 1 万次调用）才编译成机器码，不预热测的是"解释器"而不是"Java" |

| 基准里计算结果没人用，担心 JIT 把整个循环删掉 | 死代码消除（dead code elimination） | 把累加结果写进 `static volatile long sink` | 保留 | `volatile` 写有内存屏障语义，JIT 不能假定它对别人不可见，所以循环删不掉 |  
| 同一段代码两次运行差一倍 | GC、CPU 频率、后台进程 | 看数量级不看小数点 | 只写结论差异在 1~2 个数量级以上的项 | 微基准本质上是概率测量，单次跑出来的数字只代表"这一次" |

---

## 五、面试会怎么问

> 到 Day 5 再集中整理，先记下今天冒出来的两个：

- ArrayList 的扩容机制是怎样的？为什么是 1.5 倍？
- LinkedList 插入删除是 O(1)，那它是不是比 ArrayList 快？（**这是个陷阱题，答"是"就结束了**）

---

## 六、一句话总结

> **ArrayList 用「连续内存」换来随机访问 O(1)，LinkedList 用「额外指针 + 内存碎片」换来「拿着节点就能 O(1) 改结构」——  
> 而日常业务代码几乎永远拿不到那个节点，所以链表连它最擅长的插入都经常输。**
>
> 落地规则：默认 `ArrayList`；需要两端进出用 `ArrayDeque`；要 key 找 value 用 `HashMap`。  
> `LinkedList` 在业务代码里基本没有位置。

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期         | 星期 | 学习内容                                           | 用时 | 产出物                                          |  ✅  |
| :--------- | :- | :--------------------------------------------- | -: | :------------------------------------------- | :-: |
| 2026-09-22 | 一  | ArrayList / LinkedList 底层结构（与周二合并执行）           | 35 | 本笔记「知识点」                                     | [x] |
| 2026-09-22 | 二  | 实测 7 项操作耗时 + 内存占用 + 手写扩容现场                     | 40 | `w01-collections/day1-result.txt`            | [x] |
| 2026-10-09 | 五  | HashMap put 全流程：扰动 / 桶定位 / 冲突 / 扩容（断档 17 天后重启） | 55 | `day2/HashMapProbe.java` + `day2-result.txt` | [x] |
| 2026-10-09 | 五  | HashMap 线程不安全复现（8 线程 put 4 万条，实丢 1.2~2 万） | 25 | `day2/HashMapBuAnQuan.java` + `day2-buanquan-result.txt` | [x] |
| 2026-10-10 | 六  | 补完 W01 核心问题（Day1 六题 + Day2 八题）+ 逐题细节订正 | 45 | 本笔记「核心问题」 | [x] |
| 2026-10-10 | 六  | HashSet / LinkedHashMap：10 个实验（PRESENT / 去重 / 无序 / fail-fast / LRU / 迭代 / 内存） | 50 | `day3/HashSetLinkedHashMapProbe.java` + `day3-result.txt` | [x] |
|            | 三  |                                                |    |                                              |     |
|            | 四  |                                                |    |                                              |     |
|            | 五  |                                                |    |                                              |     |
|            | 六  |                                                |    |                                              |     |
|            | 日  |                                                |    |                                              |     |

> Day 1 合计约 75 分钟（超了 15 分钟）。原因：第一次要把工程骨架和运行脚本搭起来，  
> 这部分只做一次，不重复计入后面的日常开销。

---

## 附：Day 1 参考要点（**先自己写完「核心问题」再往下看**）

1. **`size` vs `length`**：`size` 是元素个数，`length` 是数组容量。两者只在「刚好装满」时相等。  
   `new ArrayList<>(0)` 合法，因为容量 0 只是"现在不预留"，第一次 `add` 会走扩容逻辑跳到 10（或跳到 `minCapacity`）。
2. **为什么第一次是 10**：JDK 把"空数组"当作一个特殊常量 `DEFAULTCAPACITY_EMPTY_ELEMENTDATA`，  
   第一次真正 `add` 时才用 `DEFAULT_CAPACITY = 10` 去 `ensureCapacityInternal`。  
   好处是：`new ArrayList<>()` 这种"可能根本不用"的对象不白占 10 个槽位。
3. **为什么 1.5 倍**：2 倍会造成「上一次释放的空间永远不够下一次用」（8+16 也拼不出 32），  
   内存复用率低；1.5 倍能形成可拼接的余量。同时 1.5 倍摊还出来的单次 `add` 成本更平稳。  
   —— 这是"时间 vs 空间"的折中，答出"复用率"和"摊还平稳"两个点就够了。
4. **`add(0, x)` 做了什么**：先 `ensureCapacity`，再 `System.arraycopy(elementData, 0, elementData, 1, size)`  
   把**所有元素整体右移一格**，最后 `elementData[0] = x`。移动 n 个元素 → O(n)。  
   这也解释了为什么 `remove(0)` 一样慢。
5. **为什么链表 `add(E)` 也慢**：三个原因叠加 ——  
   ① 每次 `add` 都要 `new Node`（对象分配 + 可能的 GC 压力）；  
   ② 新节点分配在堆上，和上一个节点不连续，写 `next` 指针可能触发 cache miss；  
   ③ 数组的连续写入能一次填满一整条 64 字节 cache line，链表做不到同一个数量级的吞吐。  
   **「O(1)」没有错，它描述的是指令条数随 n 的增长方式，不描述常数因子。**  
   面试里能主动说出这一点，比背「链表插入是 O(1)」高一个段位。
6. **什么时候用 LinkedList**：需要频繁在**两端**增删时 —— 但这时 `ArrayDeque` 更快、更省内存，  
   所以标准答案其实是「用 `ArrayDeque`」。真要用 `LinkedList` 的场景通常是：  
   用 `ListIterator` 边走边插删（比如实现 LRU、或流式处理中在当前位置反复改结构）。  
   面试里大方承认「业务代码里我基本不用它」，比硬编一个场景更可信。

---

## 附：Day 3 参考要点（**先自己写完「Day 3 核心问题」再往下看**）

1. **HashSet = HashMap 的 key 视图**。`add(e)` 就是 `map.put(e, PRESENT) == null`，  
   其中 `PRESENT` 是 `private static final Object` —— 一个**全类共享**的哨兵常量  
   （实测：3 个元素的 value 是同一个对象，`== PRESENT` 为 true，按引用去重后只有 1 种 value）。  
   共享的理由：value 永远不被读取，共享能省掉每个元素一个对象头（约 16 字节）。  
   顺带的意义：`HashMap.put` 返回 null 时无法区分「首次插入」和「插入了 null value」，  
   而 HashSet 的 value 恒非 null，所以 `put()==null` 这个判据在这里是干净的。  
   —— 追问「那 remove 怎么判断成功」：`map.remove(o) == PRESENT`，同样是拿哨兵做判据。

2. **两种都 → size = 2；只少一个也是 2，但失败方式不同**：  
   · **只有 `hashCode`**：两个对象能进同一个桶（hashCode 对了），  
     但桶内逐个比较用的是 `equals`，`Object` 默认实现是 `==`（地址比较），两个新对象必然 false → 都留下。  
   · **只有 `equals`**：`equals` 虽然是 true，但默认 `hashCode` 是地址哈希，  
     两个对象分到**不同桶**，Java 在桶这一层就免掉了 `equals` 调用 → 都留下。  
   · 口诀：**hashCode 管分桶，equals 管同桶内是不是同一个**。缺一个都不去重。  
   · 连带考点：**可变对象当 key 会变"幽灵"** —— 改掉参与 hashCode 的字段之后，  
     `contains` 永远 false，但 `size` 不变、遍历还看得见它。所以 key 必须不可变。

3. **不能说明有序，那是巧合**。`Integer.hashCode()` 返回自身，容量 16 时 `hash & 15 == n`，  
   于是「桶下标 = 数值」，看着像排好序。真实规则是：  
   **迭代顺序 = 桶下标升序 + 桶内链表顺序，而桶下标 = `hash & (capacity - 1)`**。  
   元素 hashCode 或 capacity **任何一个变了顺序就变** ——  
   实测同样 12 个字符串、同样插入顺序，容量 16 和容量 64 打出来的顺序完全不同。  
   所以「无序」的准确含义是「顺序由实现细节决定、不受你控制」，**不是随机**。  
   想控制顺序：`LinkedHashSet`（插入序）/ `TreeSet`（排序）/ 自己排序后放 `ArrayList`。

4. **因为它没真的改到结构**。`modCount` 只在结构发生变更（remove 命中 / 新增 / 扩容）时 +1，  
   迭代器每次 `next()` 才校验。删一个不存在的 key，`HashMap.remove` 直接返回 null，  
   `modCount` 不动 → 不抛异常。**这不代表安全，只是恰好没被发现**：  
   一旦那个 key 真的存在，立刻 `ConcurrentModificationException`。  
   正确写法：`iterator.remove()`（迭代器自己同步 `modCount`）或 `removeIf(...)`。

5. **看构造函数第三个参数 `accessOrder`**：`new LinkedHashMap<>(16, 0.75f, true)` 是访问序，  
   默认 `false` 是插入序。访问序下 `get(k)` 会触发 `afterNodeAccess(e)`，把节点**移到链表尾部**，  
   并且 `modCount++` —— 所以它不是只读操作：  
   ① 并发下比普通读更容易出问题；② 读一次就改变后续迭代顺序。  
   而 ② 正好是 LRU 需要的语义：最近被访问的排到最后，队首自然就是最久未使用的。  
   —— 追问「为什么 get 要改结构」：因为 LRU 的"使用"必须被记录下来，否则无法判断谁该淘汰。

6. **因为两者的迭代成本不是一个量级**：  
   `HashMap` 的迭代器要扫完整个 `table` 数组（含所有 null 桶），成本 **O(capacity + size)**；  
   `LinkedHashMap` 只沿双向链表走，成本 **O(size)**。  
   而 `table` 只扩容、**不缩容** —— 实测放 30 万再删到剩 1,000 个后 capacity 仍是 524,288，  
   于是 HashMap 每次遍历要扫 52 万个桶，实测比 LinkedHashMap 慢 **21.2 倍**。  
   代价是内存：每元素多 `before` / `after` 两个指针 = 8 字节（实测差值 8.1 字节/元素）。  
   —— 一句话收尾：**「更重」和「更慢」是两码事。**

