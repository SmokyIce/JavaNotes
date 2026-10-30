# Java基础 · 面试题库

> 集合、泛型、异常、Stream、String、反射 ｜ 对应 W43
> 每道题都要求：**脱稿讲出来**，并给出「我在项目里怎么用的」。

> ⚠️ 两个使用说明
>
> 1. 「我的回答」是**口述版骨架**（4~6 句，够讲 90 秒），照着讲得顺就改成自己的语气。
> 2. 「项目里的实际案例」是**可挂靠的场景草稿**，不是你真做过的事。
>    面试前逐条替换成你亲手做过的细节（哪个接口、多少数据量、什么现象、怎么改的），
>    否则被追问两层就露。宁可删掉，也不要背一个假案例。

## 题目

### 1. HashMap 的 hash() 为什么要写成 h ^ (h >>> 16)？

**我的回答（自己的话）：**

> 桶下标是 `hash & (n-1)`，容量 16 时 `n-1` 只有低 4 位有效，也就是 hashCode 的**高 28 位全被丢掉**。
> 如果一批 key 的 hashCode 高位不同、低位相同，它们会全挤进同一个桶，链表退化、查找变 O(n)。
> `h ^ (h >>> 16)` 就是把高 16 位无符号右移下来，跟低 16 位异或，让低位也带上高位的随机性。
> 成本只有一次移位加一次异或，是 JDK 里性价比最高的几行之一。另外 `key == null` 直接返回 0，
> 所以 null key 永远落在 0 号桶。

**追问预判：**
- 不做扰动会怎样？（一批 hashCode 高位不同、低位相同的 key 会全挤一个桶，链表退化 O(n)）
- 为什么容量必须是 2 的幂？（`(n-1) & hash` 才等价于取模，位运算比取模快）
- `new HashMap<>(17)` 容量是多少？（32，`tableSizeFor` 向上取整到 2 的幂）
- 为什么用 `>>>` 不用 `>>`？（无符号右移，负数不补 1；写成 `>>` 会把低位污染成一片 1，等于没扰动）

**项目里的实际案例：**

> 场景：本地缓存 key 是「机器号 << 20 | 序列号」拼出来的 Long，低位长期不变。
> 这种 key 不扰动就会集中撞桶。我在 W01 实验里用 4 个低位相同的 hash 复现过：
> 不扰动全落在桶 1，扰动后分散到 0 / 3 / 2 / 5。

---

### 2. HashMap 的扩容机制？为什么扩容后元素位置只有两种可能？

**我的回答（自己的话）：**

> 触发是 `++size > threshold`，**严格大于** —— 容量 16、阈值 12，第 13 个放进去之后才扩，不是第 12 个。
> 扩容容量和阈值都翻倍。搬运时**不用重算 hash**：容量翻倍，`n-1` 只多了最高一位，
> 而这一位恰好等于 `oldCap`。这一位是 0 → 新下标还是 `j`；是 1 → 新下标是 `j + oldCap`。
> 所以源码用 `(e.hash & oldCap) == 0` 把一条链拆成 lo / hi 两条，分别挂到 `j` 和 `j + oldCap`。
> JDK 7 是头插、JDK 8 改尾插，但那只是让并发扩容"必然成环"变成"偶发成环"，数据丢失一直都在。

**追问预判：**
- 什么时候扩容？（`++size > threshold`，**严格大于**；容量 16 阈值 12，第 13 个才触发）
- 为什么不用重算 hash？（`n-1` 只多了一位，结果要么不变要么 +oldCap）
- JDK 8 相比 JDK 7 的优化点？（尾插 + lo/hi 拆链，避免重算；JDK 7 头插在并发扩容下会成环）
- 负载因子 0.75 怎么来的？（时间和空间的折中，泊松分布下桶内元素 ≥8 的概率约千万分之六）
- 还有什么会触发扩容？（桶内链表太长且**容量 < 64** 时，`treeifyBin` 第一行直接 `resize()`，先扩容不树化）

**项目里的实际案例：**

> 场景：批量导入任务时已知条数（比如 5 万），按 `new HashMap<>((int)(50000/0.75f) + 1)` 预分配。
> 省掉的是 13 次扩容搬运（16 → 65536，实测 W01 手写版 20 万次 put 一共扩 13 次）。
> 「已知规模」是唯一值得预估容量的场景，其他情况别猜。

---

### 3. Java 成员变量和局部变量的默认值有什么区别？

**我的回答（自己的话）：**

> 成员变量有默认值，局部变量没有 —— 局部变量不初始化就直接用，**编译期**就报错，不是运行期。
> 原因在内存模型：对象分配内存后、执行构造函数前，JVM 会把整块内存**归零**，
> 所以引用是 null、数字是 0、boolean 是 false；而局部变量在栈帧里，JVM 不愿意为它做归零
> （方法调用太频繁），改成要求编译器强制初始化，把问题拦在使用之前。
> 一个相关的坑：`new int[10]` 元素是 0，`new Integer[10]` 元素是 null —— 因为基本类型 0 的二进制就是全 0，
> 而引用的全 0 表示 null。

**追问预判：**
- 为什么局部变量必须手动初始化？（编译期能确定使用路径，强制初始化杜绝未定义行为；成员变量由 JVM 归零保证）
- 字段默认值是在什么时候写入的？（分配内存后、执行构造函数前，JVM 把整块内存归零）
- `new int[10]` 和 `new Integer[10]` 里的元素分别是什么？（`[0,0,...]` vs `[null,...]`）
- 那 `int[]` 和 `Integer[]` 能区分"没赋值"和"赋了 0"吗？（`Integer[]` 能，`int[]` 不能）

**项目里的实际案例：**

> 场景：DTO 里用 `Integer` 而不是 `int` 表示金额 / 数量，就是为了区分
> 「用户没传」和「传了 0」—— 这在更新接口里直接决定"要不要覆盖这一列"。
> 反过来如果用了 `int`，前端漏传会被当成 0 存进去，这类事故很难事后发现。

---

### 4. JDK 9+ 反射读 java.util 内部字段为什么会被拒绝？

**我的回答（自己的话）：**

> JDK 9 引入模块系统，`java.base` 是命名模块，它对别的模块默认只**导出**包，
> 不**开放**包。`exports` 允许你在编译期和运行期访问 public 成员，
> 但 `setAccessible(true)` 需要的是 `opens`（开放，包含反射读写私有成员）。
> 所以读 `HashMap.table` 会抛 `InaccessibleObjectException`。
> 解决是加启动参数 `--add-opens java.base/java.util=ALL-UNNAMED`，
> 只对当前 unnamed module（也就是 classpath 上的代码）开放这一个包。

**追问预判：**
- 报什么错？（`InaccessibleObjectException: module java.base does not "opens java.util"`）
- 怎么解决？（启动参数 `--add-opens java.base/java.util=ALL-UNNAMED`）
- `--add-opens` 和 `--add-exports` 区别？（opens = 允许反射读写含 setAccessible；exports = 只允许直接访问）
- 生产代码能这么干吗？（不能，依赖 JDK 实现细节，一个小版本升级就可能失效；只适合本地实验）

**项目里的实际案例：**

> 场景：就是 W01 的实验 —— `HashMapProbe` 用反射读 `table` 和 `threshold`，
> 打印真实的 capacity。不加参数程序不崩，只是降级成"按 size 推算容量"。
> 这个"拿不到内部状态也要能跑"的降级写法，比反射本身更值得学。

---

### 5. HashMap 为什么线程不安全？具体丢在哪几个点？

**我的回答（自己的话）：**

> 三个点：① `++size` 是读-改-写三步，非原子；② 并发 `resize()` 互相覆盖 ——
> 两个线程各自建了新表，最后一个 `table = newTab` 生效，另一个线程搬进去的节点整体丢失；
> ③ 两个线程同时写同一个空桶，都读到 null，各建一个节点，后写的盖掉先写的。
> 更要紧的是：`size` 和"真实节点数"是两个独立的量，**两个方向都会偏** ——
> 写不同桶时数据都进去了但 `++size` 撞车（size 偏小），写同一空桶时数据被盖掉但两次 `++size` 都成功（size 偏大）。
> 本机 8 线程 4 万条实测出现过 +12842 / +1388 / −2026 三种偏差，真丢 1.2~1.8 万条。
> 所以判断丢没丢只能遍历 `entrySet()` 数节点，`size` 完全不可信。

**追问预判：**
- 具体丢在哪？（① `++size` 读-改-写非原子 ② 并发 `resize` 互相覆盖 ③ 同桶覆盖，两个线程都读到 `null` 各建一个节点）
- JDK 7 和 JDK 8 的问题一样吗？（JDK 7 头插法并发扩容会**成环** → `get()` 死循环 CPU 打满；JDK 8 改尾插 + lo/hi 拆链，规避"必然成环"，但**偶发成环和数据丢失依然存在**）
- 怎么解决？（并发一律 `ConcurrentHashMap`；`Collections.synchronizedMap` 只能保证单次操作原子，遍历仍要自己加锁）
- `ConcurrentHashMap` 凭什么安全？（`addCount` 用 CAS/LongAdder；桶头用 `synchronized` 锁单个节点，锁粒度细到桶；扩容支持多线程协作搬运，`sizeCtl` 防重复扩容）
- `size` 变小是不是就等于数据丢了？（**不是**。`size` 只是 `int` 计数器，和真实节点数没有强一致保证，**两个方向都会偏**：
  `putVal` 里 `tab[i] = newNode()` 和 `++size` 是两步 —— 写不同桶时数据都进去了但 `++size` 撞车 → **size 偏小**；
  写同一空桶时数据被盖掉但两次 `++size` 都成功 → **size 偏大**。
  实测 8 线程 4 万条：偏差出现 +12842 / +1388 / **−2026** 三种。想判断数据丢没丢，只能遍历 `entrySet()` 数节点）

**项目里的实际案例：**

> 场景：统计类代码里用 `static Map<String, Integer> counter` 做本地计数，
> 多线程累加。这种写法丢数据是"静默"的 —— 报表数字偏小但没人报错。
> 改法：累加用 `ConcurrentHashMap.computeIfAbsent(...).incrementAndGet()` 或 `LongAdder`。
> 我在 W01 里复现过一次：8 线程 4 万条丢了 1.7 万，而 `size` 报 24244 看着还挺合理。

---

### 6. 既然会丢数据，为什么 JDK 不把 HashMap 直接做成线程安全的？

**我的回答（自己的话）：**

> 因为**单线程场景占绝大多数**，而加锁的代价是每一次 `get`/`put` 都要付内存屏障和 CAS。
> JDK 的做法是把"要不要安全"的选择权交给使用者：要安全就用 `ConcurrentHashMap`，
> 它锁的粒度是桶，不是整张表，并发度高得多。
> 反面教材是 `Hashtable` —— 方法级 `synchronized` 锁整张表，一个线程 `get` 时别人连读都不行，
> 而且它还不允许 null key/value。所以 `Hashtable` 早就被淘汰了，
> 现在没人会为了"线程安全"去选它。

**追问预判：**
- 取舍是什么？（单线程场景占绝大多数，加锁会带来无谓的同步开销；把"要不要安全"交给使用者选择）
- 那有哪些替代？（`ConcurrentHashMap` / `Collections.synchronizedMap` / `Hashtable`）
- `Hashtable` 为什么被淘汰了？（方法级 `synchronized`，锁整个表，并发度太低；且不允许 null key/value）
- `Collections.synchronizedMap` 就够了吗？（只保证单次操作原子；`for` 循环 / 迭代器仍要自己 `synchronized (map)`）

**项目里的实际案例：**

> 场景：接口层的本地缓存 Map，一开始用 `HashMap` 因为"单线程访问"，
> 后来加了 `@Async` 异步刷新，就变成多线程写了 —— 这类"后来才变成并发"的改造最常见。
> 判断标准很简单：**这个 Map 有没有可能被两个线程碰到**（包括异步、定时任务、并行流），有就换 `ConcurrentHashMap`。

---

### 7. HashSet 是怎么实现去重的？只重写 hashCode 不重写 equals 会怎样？

**我的回答（自己的话）：**

> `HashSet` 内部就一个 `HashMap<E, Object>`，元素全当 key，value 是全类共享的常量 `PRESENT`。
> `add(e)` 就是 `map.put(e, PRESENT) == null` —— 返回 null 说明之前不存在，算新增成功。
> 去重是两个关卡配合的结果：第一关 `hashCode` 决定进哪个桶，第二关 `equals` 决定桶内是不是同一个。
> **只重写 `hashCode`**：两个对象能进同一个桶，但桶内比的是 `equals`，`Object` 默认是地址比较 → 都留下，size=2。
> **只重写 `equals`**：`equals` 明明 true，但默认 `hashCode` 是地址哈希 → 分到不同桶，
> `equals` 根本没机会被调用 → 也都留下，size=2。两种失败方式不一样，但结果一样。
> 所以口诀是：**hashCode 管分桶，equals 管同桶内是不是同一个**。

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

> 场景：任务去重 —— 同一批任务按 `(bizType, bizId)` 去重，`HashSet<TaskKey>`。
> 如果 `TaskKey` 忘了重写 `equals`，去重完全失效（size 翻倍），而且**不报错**，
> 表现是"任务被重复执行"。这种 bug 只能靠单测兜：`assertEquals(1, set.size())`。
> 更狠的一版：`TaskKey` 有 `setStatus()`，放进 Set 之后再改 status，
> 如果 status 参与了 hashCode，这个元素就"消失"了 —— `contains` 返回 false 但 `size` 不变。

---

### 8. HashSet 是无序的，那为什么我放 1..12 进去打印出来是升序？

**我的回答（自己的话）：**

> 那是**巧合**，不是有序。`Integer.hashCode()` 返回自身，容量 16 时 `hash & 15 == n`，
> 于是"桶下标 = 数值"，迭代顺序正好等于数值顺序。
> 真正的规则是：**迭代顺序 = 桶下标升序 + 桶内链表顺序**，桶下标 = `hash & (capacity - 1)`。
> 所以 hashCode 或 capacity 任何一个变了，顺序就变 —— 同样 12 个字符串、同样插入顺序，
> `new HashSet<>(16)` 和 `new HashSet<>(64)` 打出来的顺序完全不同。
> 「无序」的准确含义是**顺序由实现细节决定、不受你控制**，不是随机。

**追问预判：**
- 这是巧合还是机制？（**巧合**。`Integer.hashCode()` 返回自身，容量 16 时 `hash & 15 == n`，
  于是"桶下标 = 数值"，看着像排好序）
- 真正的规则是什么？（迭代顺序 = 桶下标升序 + 桶内链表顺序，桶下标 = `hash & (capacity - 1)`）
- 怎么证明它不稳定？（同样 12 个字符串、同样插入顺序，`new HashSet<>(16)` 和 `new HashSet<>(64)`
  打印出的顺序**完全不同** —— capacity 变了，`&` 取的位数就变了）
- 「无序」准确的含义？（不是随机，而是"顺序由 hashCode 和 capacity 共同决定，不受你控制"）
- 想控制顺序怎么办？（`LinkedHashSet` 插入序 / `TreeSet` 排序）

**项目里的实际案例：**

> 场景：接口返回 `HashSet<String>` 的标签列表，前端拿到的顺序每次重启都不同，
> 被当成 bug 提过来。改成 `LinkedHashSet` 后稳定。
> 教训：**只要输出会被人看到或对比（接口响应、导出文件、缓存 key 拼接、幂等签名），就不要用无序集合。**

---

### 9. 怎么用 JDK 自带类实现 LRU？为什么 get() 会改变结构？

**我的回答（自己的话）：**

> `new LinkedHashMap<>(16, 0.75f, true)` 开启访问序，再重写 `removeEldestEntry` 返回 `size() > capacity`，
> 一共 6 行。第三个参数 `accessOrder` 默认 `false` 是插入序，`true` 是访问序。
> 访问序下 `get(k)` 会触发 `afterNodeAccess`，把节点**挪到双向链表尾部**，并且 `modCount++` ——
> 所以它不是只读操作：读一次就改变后续迭代顺序，并发下也更容易出问题。
> 而"读一次改一次顺序"正好是 LRU 需要的语义：最近被访问的排到最后，队首自然就是最久未使用的。
> 那个 `removeEldestEntry` 是在**新元素已经插入之后**才被调用的，所以判断写 `>`，
> 写成 `>=` 就会少存一个。这个 LRU 只能当玩具，生产用 Caffeine。

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

> 场景：本地缓存「字典表 / 配置项」，读多写少、允许丢。
> 手写 LRU 的诱惑很大，但真正的坑是**并发**：访问序 `get` 会改结构，必须整张表加锁，
> 一加锁就又成了串行。所以结论是直接上 Caffeine：
> `Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(10, MINUTES).build()`，
> 读不加锁、淘汰策略也比 LRU 好（W-TinyLFU 能挡住偶发批量扫描把热数据冲掉）。

---

### 10. LinkedHashMap 每个元素多两个指针、更占内存，它迭代会更慢吗？

**我的回答（自己的话）：**

> 不会，**HashMap 更慢**，实测差 21.2 倍。因为两者的迭代成本不是一个量级：
> HashMap 的迭代器要扫完整个 `table` 数组，**包括所有 null 桶**，成本是 O(capacity + size)；
> LinkedHashMap 只沿双向链表走，成本 O(size)。
> 而 `table` **只扩容、不缩容** —— 我实测放入 30 万再删到剩 1000 个，capacity 还是 524288，
> 于是 HashMap 每次遍历要白扫 52 万个桶。
> 代价是内存：每个元素多 `before`/`after` 两个引用，理论 8 字节，实测 8.1 字节/元素。
> 所以结论是：**「更重」和「更慢」是两笔独立的账**，内存花得多不必然性能差。

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

> 场景：一个 Map 当"临时结果集"用了半个月，元素从 30 万删到几千，
> 之后每次遍历都莫名变慢 —— 就是"只扩容不缩容"。
> 处理办法：要么重新 `new` 一个（数据量小，成本低），要么一开始就别用同一个实例当长期容器。
> 另一个反例：如果这个 Map 要反复全量遍历，`LinkedHashMap` 反而更划算 —— 多 3.9 MB 换 21 倍遍历速度。

---

### 11. 手写 HashMap 时，resize 里 `(e.hash & oldCap) == 0` 在判什么？

**我的回答（自己的话）：**

> 判的是"新增的那一位"。容量翻倍，`n-1` 只多了最高一位，而这一位恰好等于 `oldCap`
> （16 → 32 时 `oldCap = 16 = 0b10000`）。
> 这一位是 0，新下标还是 `j`；是 1，就挪到 `j + oldCap`。
> 所以只要把一条旧链拆成 lo / hi 两条，分别挂到 `newTab[j]` 和 `newTab[j + oldCap]`，
> **一次 hash 都不用重算**。我实测 hashCode 1 和 17 在容量 16 时同在桶 1，
> 扩容到 32 后分别落到桶 1 和桶 17，一个都没丢。

**追问预判：**
- 为什么不能直接 `hash % newCap`？（能算对，但要重新取模 N 次，而拆链只需要一次 `& oldCap`）
- 前提是什么？（容量必须是 2 的幂，否则 `n-1` 多出来的不止一位）
- 拆链要注意什么？（两条链的尾巴都要 `next = null`，否则会挂着旧链残留，出现重复元素）
- 怎么验证自己写对了？（和 `java.util.HashMap` 逐次对拍：返回值、size、get 结果全比，20 万次 0 处不一致才算过）

**项目里的实际案例：**

> 场景：这就是 W01 的手写实验（`day6/MyHashMap.java`，只写 put/get/resize 三段）。
> 对拍时第一版漏了 `loTail.next = null`，`get` 结果先对不上 —— 说明**验收靠对拍，不靠"我觉得对"**。

---

### 12. 「链表到 8 个就转红黑树」这句话有什么问题？

**我的回答（自己的话）：**

> 缺两个前提：**容量 ≥ 64**，以及 **key 能比较大小**。
> 容量小于 64 时，`treeifyBin` 第一行就直接 `resize()` 了 —— 桶少的时候冲突多是因为桶不够，
> 先扩容比树化划算。
> 更隐蔽的是第二个：每次插入都要判断"这个 key 在不在"，需要比较大小。hash 全相等时退到
> `Comparable`，key 没实现就只能用 `tieBreakOrder`（identityHashCode）硬定方向，
> 这个顺序和 `equals` 无关，所以树的每一层都无法排除，只能对整个子树做一次 `find()` 全扫。
> 结果每次插入 O(n)，而 `TreeNode` 还比链表节点重（多 parent/left/right/prev/red），**比链表更慢**。
> 我实测 2 万条 hash 全等的 key：JDK 2521 ms，手写链表 374 ms；
> 给同一个 key 加上 `Comparable` 之后，JDK 变 44 ms，反超 12 倍。

**追问预判：**
- 什么时候树化？（链表到 8 个 **且** 容量 ≥ 64；容量 < 64 时先 `resize`）
- 什么时候退回去？（扩容拆树后某一边 ≤ 6 个，`untreeify` 变回链表）
- 为什么阈值是 8 和 6？（8 是泊松分布下桶内 ≥8 的概率低到千万分之六；6 和 8 留个缓冲，防止反复树化/退化）
- 树化一定更快吗？（**不一定**，前提是 key 可比较；否则全扫，还更重）
- 自定义对象当 key 要注意什么？（**不可变** + `hashCode`/`equals` 同源；冲突密集场景再实现 `Comparable`）

**项目里的实际案例：**

> 场景：自定义对象当 Map key 时，除了不可变和两个方法同源，还要考虑 hashCode 的分撒性。
> 见过最典型的反例是「用两个业务字段拼字符串当 hashCode」（`(a + "-" + b).hashCode()`），
> 前缀相同的一批 key 低位高度相似，冲突率远高于预期。
> 这类场景要么换成 `Objects.hash`，要么老老实实让 IDE 生成。

---

### 13. 类型擦除到底擦掉了什么？运行时还能不能拿到泛型实参？

**我的回答（自己的话）：**

> 擦掉的是「使用处填的实参」，留下的是「声明」。
> `new ArrayList<String>()` 和 `new ArrayList<Integer>()` 的 `getClass()` 是同一个对象，
> 连 `List` 都不剩，只剩 `java.util.ArrayList` —— 所以在运行时判断不了它是 `List<String>`。
> 但声明处的类型变量还在：`Box.class.getTypeParameters()` 能读出 `T`，
> `Box.get` 的泛型返回类型也是 `T`。
> 实参唯一能捞回来的地方是**子类继承时写下的那段签名**，它被写进 class 文件的 `Signature` 属性，
> `StringBox.class.getGenericSuperclass()` 能拿到 `Box<java.lang.String>`。
> 代价是类型检查从「运行时每个元素」搬到了「编译期每个表达式」——
> 错误不在 `add` 时报，而在 `get` 后面编译器插的 `checkcast` 那一刻才报。
> 顺带一个副作用：实现泛型接口时，编译器会补一个桥方法（`bridge=true, synthetic=true`）对齐擦除后的签名。

**追问预判：**
- 现场证明一下擦除？（`getClass() ==` 判 true，或者 `getMethod("add", String.class)` 抛 `NoSuchMethodException`）
- 那 `Signature` 属性里到底存了什么？（泛型父类/接口的实参、泛型字段和方法的类型变量）
- 反射有两个视图是什么意思？（`getParameterTypes()` 是擦除后的 `Object`，`toGenericString()` 能还原成 `add(E)`）
- 桥方法是干嘛的？（接口方法擦除后是 `compare(Object,Object)`，实现类写的是 `compare(String,String)`，编译器补一个转发 + checkcast）
- 桥方法会带来什么坑？（遍历 `getDeclaredMethods()` 会多出一个，框架必须按 `isBridge()` 跳过）

**项目里的实际案例：**

> 场景：线上出现 `Integer cannot be cast to String`，栈顶落在某个看起来毫无问题的 `map.get(key)` 上。
> 排查发现是上游用原始类型 `Map` 塞了数字，下游按 `Map<String,String>` 取。
> 结论：CCE 的位置是「按类型读出来」那一行，不是塞进去那一行 —— 修 `get` 没用，要往上追入口。

---

### 14. 为什么不能 `new T[]`？那框架是怎么创建泛型数组、拿到泛型类型的？

**我的回答（自己的话）：**

> 因为数组在运行时必须知道自己的组件类型 —— `ArrayStoreException` 就是靠这个检查的。
> 而擦除后的 `T` 什么都不剩，编译期无法把这个信息写进 `newarray` 指令，所以 `new T[10]` 直接
> `generic array creation` 报错。
> `(T[]) new Object[n]` 只是骗过编译器，数组的真实类型还是 `[Ljava.lang.Object;`，
> 一旦赋给 `String[]` 立刻 ClassCastException。
> JDK 自己的兜底是把组件类型显式传进来：`Array.newInstance(type, n)`；
> 框架拿泛型类型也是同一个思路 —— 让调用方继承一个写了实参的类（`TypeReference<T>` 的匿名子类），
> 再从 `getGenericSuperclass()` 里把实参读出来。
> 说白了：**擦除把「类型」变成了「必须由调用方显式告知的参数」**，`Class<T>` / `TypeReference` 都是在补这一课。

**追问预判：**
- 那 `new ArrayList[10]` 为什么可以？（原始类型的数组，没有实参要检查；但用它就是 unchecked，容易混装）
- 为什么不允许 `new List<String>[10]`？（数组协变 + 擦除，会让「绕过编译器塞错类型」成为可能，破坏类型安全）
- `Array.newInstance` 返回的是真数组吗？（是，组件类型就是传进去的那个 Class）
- `TypeReference` 的原理？（匿名子类把实参写进 `Signature`，再 `getGenericSuperclass()` 取 `ParameterizedType`）

**项目里的实际案例：**

> 场景：用 Jackson 反序列化一个统一响应体 `Result<List<User>>`，直接
> `objectMapper.readValue(json, Result.class)` 拿到的是 `Result`，里面的 data 变成
> `List<LinkedHashMap>`，取字段时 CCE。改成 `new TypeReference<Result<List<User>>>(){}` 就好了。
> 本质就是匿名子类 + `getGenericSuperclass()`，和 `Array.newInstance` 是同一类解法。

---

### 15. PECS 是什么？`Collections.copy` 的签名为什么用两个不同的通配符？

**我的回答（自己的话）：**

> PECS = Producer Extends, Consumer Super：参数是生产者（你从里面读）就用 `? extends T`，
> 是消费者（你往里面写）就用 `? super T`。
> `? extends Number` 能安全读成 `Number`，但写不进去 —— 编译器不知道容器实际是 `List<Integer>`
> 还是 `List<Double>`，只允许 `add(null)`。
> `? super Integer` 能安全写 `Integer`，因为容器至少能装 `Integer`；但读出来只能当 `Object`。
> `Collections.copy(List<? super T> dest, List<? extends T> src)` 正好一写一读：
> `dest` 是消费者用 `super`，`src` 是生产者用 `extends`，`T` 由两边共同推断。
> 如果两边都写成 `<T>`，`List<Object>` 和 `List<String>` 就没法一起传 —— 泛型是不变的。

**追问预判：**
- 泛型为什么不变？数组协变不是挺方便吗？（数组协变把检查推到运行时，泛型用不变性把错误挡在编译期）
- `List<? extends Number>` 为什么能 `add(null)`？（`null` 是唯一对所有子类型都合法的值）
- `?` 和 `<T>` 有什么区别？（`?` 是「一个确定但我不知道的类型」，不能声明新变量；`<T>` 是给方法/类引入一个可命名的类型参数）
- 通配符捕获是什么？（`capture of ?` 报错时，用一个 `<T> helper(List<T>)` 把它捕获成具名类型）
- 什么时候干脆用 `<T>`？（进和出是同一种类型、需要在方法体里声明该类型的变量时）

**项目里的实际案例：**

> 场景：写一个批量搬运的公共方法 `moveAll(List<? super T> dest, List<? extends T> src)`，
> 上层既能 `List<Object>` 收、也能 `List<User>` 收。一开始签名写成两个 `List<T>`，
> 调用方传 `List<Object>` 和 `List<User>` 直接编译不过，改成 PECS 后一次通过。

---

### 16. `finally` 里写 `return` 或 `throw` 会发生什么？TWR 为什么需要 `addSuppressed`？

**我的回答（自己的话）：**

> `finally` 里 `return`：方法走「正常返回」，`try` 里正在冒泡的异常被**直接丢弃** ——
> 不 catch、不打日志、不进 suppressed，调用方只看到一个正常返回值。我实测过
> `swallow()` 里 throw 了 `IllegalStateException`，方法返回 `42`，什么痕迹都没留。
> 而且 `finally` 的 `return` 还会盖掉 `try` 的 `return` 值。javac 对这个写法一句话都不说，
> 只有 IDE 会警告。
>
> `finally` 里 `throw`：新异常成为最终抛出的那个，`try` 的原始异常被**覆盖**，
> 既不是 cause 也不是 suppressed —— 实测 `getSuppressed().length = 0`、`getCause() = null`，
> 原始异常彻底消失。线上表现是日志里只有「close 炸了」，业务异常查不到。
>
> 所以 TWR 才需要 suppressed：Java 7 之前关资源写在 `finally` 里，close 一抛就覆盖主异常。
> TWR 由编译器生成「关资源 + 保留主异常」的代码，而保留不能靠 cause（close 失败不是业务失败的原因，
> 用 cause 会把因果说反），也不能靠重新抛（一次只能抛一个）。`suppressed` 就是为此加的语义：
> 主异常照常抛，附带损害挂在它身上，两个都能进日志。

**追问预判：**
- 关闭顺序为什么是逆序？（后面的资源可能依赖前面的，先关被依赖的会让 close 拿到坏连接）
- body 和 close 都抛，谁是主异常？（body 的；实测 `getSuppressed()` 里是 close 的那个）
- body 正常、只有 close 抛呢？（close 的就是主异常，没有东西可压制）
- `finally` 里为什么连 `close()` 都不该写？（close 自己就可能抛，等于把风险留在 `finally`；
  正确做法是让资源实现 `AutoCloseable`，交给 TWR）
- `cause` 和 `suppressed` 有什么区别？（cause 一对一，`initCause` 只能补一次；suppressed 一对多，可反复 add）

**项目里的实际案例：**

> 场景：老代码里 `finally { conn.close(); }` 包在业务逻辑外面，某次数据库主从切换，
> `close()` 抛了 `SQLException`，把真正的「订单状态机校验失败」覆盖掉了，
> 日志里只有 close 的错，查了两小时。后来统一改成 try-with-resources，
> 同一个故障再出现时，日志里同时有主异常和 `Suppressed: java.sql.SQLException`。

---

### 17. `throw new RuntimeException(e)` 会丢栈吗？什么样的写法才是真的丢栈？

**我的回答（自己的话）：**

> 这句话不准确。`new RuntimeException(e)` 的栈**没丢**，是断成了两段：
> 新异常的栈从包装那一行开始往上，真正的出错点在 cause 里。
> 我实测过：包装异常栈深度 3、栈顶是 `wrapWithCause:50`（包装那一行）；
> cause 是 `IllegalStateException`、栈深度 5、栈顶是真正出事的 `deepest()`。
> 只要日志把 cause 打出来（`log.error("...", e)`），两段栈都能看到，还多了一句业务语义。
>
> 真正丢栈的是这两类：
> ① `new RuntimeException(e.getMessage())` —— `getCause()` 直接是 `null`，异常类型、栈、嵌套关系全没了；
> ② `log.error("... " + e.getMessage())` —— 异常对象根本没进日志。同一个异常，
> `getMessage()` 只有 11 个字符 1 行，`printStackTrace` 是 634 个字符 9 行还带 `Caused by`。
>
> 所以判断标准不是「要不要包装」，而是「包装时有没有把 cause 传下去、打日志时有没有把异常对象传下去」。
> 该包装的场合：跨层（DAO 的 SQLException 不该漏到 Controller）、受检转非受检（lambda 里写不了 throws）、
> 补上下文（把 orderId 塞进 message）。不该包装：同层同语义（只多一层 Caused by 纯噪音）、
> 只想加日志（打完直接 `throw e`）。

**追问预判：**
- 那为什么很多人说它丢栈？（看的是新异常的栈顶，没往下翻 cause；或者日志只打了一半）
- 怎么在包装时同时保留两个独立异常？（`addSuppressed`，TWR 就是这么干的）
- 受检异常的边界在哪？能不能绕过？（编译器只检查「可能抛出的受检异常」；
  `sneakyThrow` 用泛型擦除可以绕过，Lombok `@SneakyThrows` 就是这个原理）
- `catch (Throwable)` 为什么危险？（会把 `OutOfMemoryError`、`StackOverflowError` 一起吞掉，
  JVM 已经进入不确定状态，继续跑反而更糟）
- 异常的性能成本在哪？（`fillInStackTrace` 要遍历栈帧。W02 实测栈深 1 约 447 ns/次，
  用 `-XX:-StackTraceInThrowable` 关掉采集后 31 ns，差 15 倍；多 5 层栈再多 63 ns。
  高频路径上不要用异常做流程控制，但「该抛就抛」，一次几百纳秒比查不出问题便宜）

**项目里的实际案例：**

> 场景：一个「订单导出」接口线上一半失败，日志只有一行
> `导出失败：导出失败：null` —— 典型的 `catch (Exception e) { throw new RuntimeException("导出失败：" + e.getMessage()); }`，
> 而且外面还套了一层同样的包装。改成 `throw new BizException("导出失败，orderNo=" + orderNo, e)` 后，
> 下一次故障直接从 `Caused by` 里看到 `SocketTimeoutException: Read timed out`，定位时间从小时级到分钟级。

---

### 18. `Optional` 能消灭 NPE 吗？`orElse` 和 `orElseGet` 有什么区别？

**我的回答（自己的话）：**

> 消灭不了。`Optional` 只是把「可能没有值」写进**返回类型**，逼调用方表态，它自己照样能炸：
> `get()` 抛 `NoSuchElementException`、`orElse(null)` 当场把契约破坏掉、当字段用还能被序列化绕过。
> 它真正值钱的是链式：`map` / `flatMap` / `filter` 把「缺失」一路短路到链尾，只在一处兜底。
>
> `orElse` 和 `orElseGet` 的差别是 **eager 还是 lazy**：`orElse` 的副参数在进 `orElse` 之前就求值完了，
> 所以命中缓存时也白算一次。我实测过：模拟一次 20 ms 的远程查询，有值时 `orElse` 还是调了 **1 次、36 ms**，
> `orElseGet` 是 **0 次、0.3 ms**。结果完全正确，只是慢，所以这类 bug 极难发现。
> 判断标准一条：参数里有 IO / 远程调用 / 加锁 / 大对象，就必须 `orElseGet`。
>
> 还有两个常被问到的坑：`Optional` 不能做字段 —— 它没实现 `Serializable`，
> 一个 `Optional` 字段就让整个 DTO 去序列化时抛 `NotSerializableException`；
> `map` 里返回 `null` 会**静默**变成 `empty`，而 `flatMap` 里返回 `null` 直接 NPE。

**追问预判：**
- `Optional` 该不该做方法参数？（不该：调用方每个地方都要 `Optional.ofNullable(x)` 包一遍，
  而且照样能传 `null`，签名给人的安全感是假的）
- `map` 和 `flatMap` 怎么选？（被调方法**自己返回 Optional** 就用 `flatMap`，返回普通值用 `map`。
  写错不是编译错，是多一层 `Optional<Optional<T>>`，到链尾 `orElse` 时才露馅）
- `Optional` 能提升性能吗？（不能，还多一次对象分配；`OptionalInt` 这类特化只是为了少装箱。
  它的价值是可读性 + 强制表态）
- `filter` 在链里干什么？（把「有值但无效」也归到 `empty`。业务例子里：
  订单 200 元用 500 元券，不 `filter(c -> c.off() <= amount)` 会算出 **−300**）
- 为什么 `empty.get()` 的报错比 NPE 还难查？（只有一句 `No value present`，
  没有变量名和上下文；而 JDK 14+ 的 NPE 有 helpful message，会说清谁为 null）

**项目里的实际案例：**

> 场景：商品详情接口从缓存取价格，`return cache.get(id).orElse(loadFromDb(id));`，
> 缓存命中率 95%，逻辑上看没问题。压测时发现 DB 的 QPS 是缓存未命中率的 5 倍，
> 一直找不到原因 —— 因为 `orElse` 的副参数**每次**都求值。
> 改成 `orElseGet(() -> loadFromDb(id))` 后 DB QPS 立刻掉到原来的 1/20 以下。
> 这个 bug 的特征值得记：**结果永远正确、只在监控上表现为「DB 压力莫名偏高」**。

---

