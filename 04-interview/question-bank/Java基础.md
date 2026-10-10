# Java基础 · 面试题库

> 集合、泛型、异常、Stream、String、反射 ｜ 对应 W43
> 每道题都要求：**脱稿讲出来**，并给出「我在项目里怎么用的」。

## 题目

### 1. HashMap 的 hash() 为什么要写成 h ^ (h >>> 16)？

**我的回答（自己的话）：**

**追问预判：**
- 不做扰动会怎样？（一批 hashCode 高位不同、低位相同的 key 会全挤一个桶，链表退化 O(n)）
- 为什么容量必须是 2 的幂？（`(n-1) & hash` 才等价于取模，位运算比取模快）
- `new HashMap<>(17)` 容量是多少？（32，`tableSizeFor` 向上取整到 2 的幂）

**项目里的实际案例：**

---

### 2. HashMap 的扩容机制？为什么扩容后元素位置只有两种可能？

**我的回答（自己的话）：**

**追问预判：**
- 什么时候扩容？（`++size > threshold`，**严格大于**；容量 16 阈值 12，第 13 个才触发）
- 为什么不用重算 hash？（`n-1` 只多了一位，结果要么不变要么 +oldCap）
- JDK 8 相比 JDK 7 的优化点？（尾插 + lo/hi 拆链，避免重算；JDK 7 头插在并发扩容下会成环）
- 负载因子 0.75 怎么来的？（时间和空间的折中，泊松分布下桶内元素 ≥8 的概率约千万分之六）

**项目里的实际案例：**

---

### 3. Java 成员变量和局部变量的默认值有什么区别？

**我的回答（自己的话）：**

**追问预判：**
- 为什么局部变量必须手动初始化？（编译期能确定使用路径，强制初始化杜绝未定义行为；成员变量由 JVM 归零保证）
- 字段默认值是在什么时候写入的？（分配内存后、执行构造函数前，JVM 把整块内存归零）
- `new int[10]` 和 `new Integer[10]` 里的元素分别是什么？（`[0,0,...]` vs `[null,...]`）

**项目里的实际案例：**

---

### 4. JDK 9+ 反射读 java.util 内部字段为什么会被拒绝？

**我的回答（自己的话）：**

**追问预判：**
- 报什么错？（`InaccessibleObjectException: module java.base does not "opens java.util"`）
- 怎么解决？（启动参数 `--add-opens java.base/java.util=ALL-UNNAMED`）
- `--add-opens` 和 `--add-exports` 区别？（opens = 允许反射读写含 setAccessible；exports = 只允许直接访问）

**项目里的实际案例：**

---

### 5. HashMap 为什么线程不安全？具体丢在哪几个点？

**我的回答（自己的话）：**

**追问预判：**
- 具体丢在哪？（① `++size` 读-改-写非原子 ② 并发 `resize` 互相覆盖 ③ 同桶覆盖，两个线程都读到 `null` 各建一个节点）
- JDK 7 和 JDK 8 的问题一样吗？（JDK 7 头插法并发扩容会**成环** → `get()` 死循环 CPU 打满；JDK 8 改尾插 + lo/hi 拆链，规避成环，但**数据丢失依然存在**）
- 怎么解决？（并发一律 `ConcurrentHashMap`；`Collections.synchronizedMap` 只能保证单次操作原子，遍历仍要自己加锁）
- `ConcurrentHashMap` 凭什么安全？（`addCount` 用 CAS/LongAdder；桶头用 `synchronized` 锁单个节点，锁粒度细到桶；扩容支持多线程协作搬运，`sizeCtl` 防重复扩容）
- `size` 变小是不是就等于数据丢了？（**不是**。`size` 只是 `int` 计数器，和真实节点数没有强一致保证，**两个方向都会偏**：
  `putVal` 里 `tab[i] = newNode()` 和 `++size` 是两步 —— 写不同桶时数据都进去了但 `++size` 撞车 → **size 偏小**；
  写同一空桶时数据被盖掉但两次 `++size` 都成功 → **size 偏大**。
  实测 8 线程 4 万条：偏差出现 +12842 / +1388 / **−2026** 三种。想判断数据丢没丢，只能遍历 `entrySet()` 数节点）

**项目里的实际案例：**

---

### 6. 既然会丢数据，为什么 JDK 不把 HashMap 直接做成线程安全的？

**我的回答（自己的话）：**

**追问预判：**
- 取舍是什么？（单线程场景占绝大多数，加锁会带来无谓的同步开销；把"要不要安全"交给使用者选择）
- 那有哪些替代？（`ConcurrentHashMap` / `Collections.synchronizedMap` / `Hashtable`）
- `Hashtable` 为什么被淘汰了？（方法级 `synchronized`，锁整个表，并发度太低；且不允许 null key/value）

**项目里的实际案例：**

---

### 7. HashSet 是怎么实现去重的？只重写 hashCode 不重写 equals 会怎样？

**我的回答（自己的话）：**

**追问预判：**
- HashSet 和 HashMap 什么关系？（就一个 `HashMap<E,Object>`，元素全在 key 上）
- 那 value 是什么？（`private static final Object PRESENT = new Object()`，**全类共享一个常量**；
  实测 3 个元素的 value 是同一个对象，按引用去重后只有 1 种。共享是因为 value 永不暴露，能省掉每元素一个对象头）
- 只重写 `hashCode`？（能进同一个桶，但桶内比较用 `equals`，`Object` 默认是 `==` 地址比较 → 两个都留下，size=2）
- 只重写 `equals`？（`equals` 是 true，但默认 `hashCode` 是地址哈希 → **分到不同桶**，桶层面就免掉了 equals → size=2）
- 口诀？（**hashCode 管分桶，equals 管同桶内是不是同一个**）
- `add` 的返回值怎么来的？（`map.put(e, PRESENT) == null`）
- 顺带一个坑？（**可变对象当 key 会变幽灵**：改掉参与 hashCode 的字段后 `contains` 永远 false，
  但 `size` 不变、遍历还看得见它。所以 key 必须不可变）

**项目里的实际案例：**

---

### 8. HashSet 是无序的，那为什么我放 1..12 进去打印出来是升序？

**我的回答（自己的话）：**

**追问预判：**
- 这是巧合还是机制？（**巧合**。`Integer.hashCode()` 返回自身，容量 16 时 `hash & 15 == n`，
  于是"桶下标 = 数值"，看着像排好序）
- 真正的规则是什么？（迭代顺序 = 桶下标升序 + 桶内链表顺序，桶下标 = `hash & (capacity - 1)`）
- 怎么证明它不稳定？（同样 12 个字符串、同样插入顺序，`new HashSet<>(16)` 和 `new HashSet<>(64)`
  打印出的顺序**完全不同** —— capacity 变了，`&` 取的位数就变了）
- 「无序」准确的含义？（不是随机，而是"顺序由 hashCode 和 capacity 共同决定，不受你控制"）
- 想控制顺序怎么办？（`LinkedHashSet` 插入序 / `TreeSet` 排序）

**项目里的实际案例：**

---

### 9. 怎么用 JDK 自带类实现 LRU？为什么 get() 会改变结构？

**我的回答（自己的话）：**

**追问预判：**
- 怎么实现？（`new LinkedHashMap<>(16, 0.75f, **true**)` 开启访问序 + 重写 `removeEldestEntry` 返回 `size() > capacity`，
  一共 6 行）
- `accessOrder` 默认是什么？（`false` = 插入序；`true` = 访问序，`get` 会把节点移到链表尾部）
- 为什么说访问序下 `get()` 不是只读操作？（它触发 `afterNodeAccess` 重排链表并 `modCount++` ——
  ① 并发下更容易出问题 ② 读一次就改变后续迭代顺序。而 ② 正是 LRU 需要的语义）
- 常见写错的地方？（判断写成 `>=` 就**少存一个**：`removeEldestEntry` 是在新元素已插入**之后**才调用的）
- 覆盖已有 key 会触发淘汰吗？（不会，走的是 `afterNodeAccess` 分支；但访问序会把它挪到队尾，语义上仍然对）
- LinkedHashMap 怎么做到不改 HashMap 一行代码？（三个 `afterNodeXxx` 空钩子被重写 → **模板方法模式**）
- 这个 LRU 能上生产吗？（不能，非线程安全且每次读都改结构；生产用 **Caffeine**（W-TinyLFU））

**项目里的实际案例：**

---

### 10. LinkedHashMap 每个元素多两个指针、更占内存，它迭代会更慢吗？

**我的回答（自己的话）：**

**追问预判：**
- 谁更慢？（**HashMap 更慢**，实测差 **21.2 倍**）
- 为什么？（HashMap 的迭代器要扫完整个 `table` 数组，**含所有 null 桶**，成本 **O(capacity + size)**；
  LinkedHashMap 只沿双向链表走，成本 **O(size)**）
- 为什么 capacity 会那么大？（`table` **只扩容、不缩容**。实测放入 30 万再删到剩 1000 个，
  capacity 仍是 **524288**，每次遍历要白扫 52 万个桶）
- 那它多花的代价是什么？（每元素多 `before`/`after` 两个引用 = **8 字节**（压缩指针），
  实测差值 **8.1 字节/元素**，50 万元素多 3.9 MB）
- 一句话收尾？（**「更重」和「更慢」是两码事**，内存开销和遍历性能是两笔独立的账）

**项目里的实际案例：**

---
