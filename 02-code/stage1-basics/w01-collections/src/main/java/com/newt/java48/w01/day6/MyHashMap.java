package com.newt.java48.w01.day6;

/**
 * 手写 MyHashMap —— 降级版：只实现 put / get / resize 三段核心。
 *
 * ============================================================
 * 为什么只写这三段
 * ============================================================
 *   HashMap 有 2000 多行，但真正决定"它为什么长这样"的只有三件事：
 *
 *     1. put     —— 一次 put 走完全部路径：扰动 → 定位桶 → 处理冲突 → 检查扩容
 *     2. get     —— 验证"桶 + 链表"的两级查找，以及 equals 在第二级才登场
 *     3. resize  —— 为什么扩容只拆两条链（这是 W01 最容易背错的一段）
 *
 *   红黑树（treeifyBin）、fail-fast（modCount）、null key 的特殊处理，全部不写。
 *   理由是：树化是"冲突极端时的补丁"，不影响主流程；modCount 属于迭代器章节。
 *
 * ============================================================
 * 和 JDK 的差异（故意保留的差异）
 * ============================================================
 *   · 没有树化：桶内永远是链表。故意让一个 hashCode 恒定的 key 塞满一个桶，
 *     就能看到"JDK 会树化、我的会退化成 O(n)"的差别。
 *   · 没有 modCount：迭代器在这里不存在，所以不需要 fail-fast。
 *   · 没有 keySet/entrySet/remove/containsKey：不是本周目标。
 *
 * ============================================================
 * 三段里的三个关键结论（写完代码才对得上）
 * ============================================================
 *   ① 扩容触发是 `++size > threshold`，**严格大于** —— 容量 16、阈值 12，
 *      第 13 个元素放进去之后才触发。不是第 12 个。
 *   ② 扩容后下标只有两种可能：`hash & (newCap-1)` 比旧值**只多看了最高一位**，
 *      这一位是 0 → 原地不动；是 1 → 去掉 `j + oldCap`。
 *      所以 JDK 用 lo / hi 两条链代替"重新计算每个元素的 hash"。
 *   ③ put 的返回值和 HashMap 一样：返回被覆盖的旧值，首次插入返回 null
 *      —— 所以「返回 null」无法区分首次插入和存了 null 值（要 putIfAbsent）。
 *
 * 运行：run.bat day6.MyHashMapProbe
 */
public class MyHashMap<K, V> {

    // ---------------- 常量：三个数字直接抄 JDK ----------------

    /** 默认容量 16。1 << 4 这样写是为了强调"必须是 2 的幂" */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

    /** 容量上限 2^30。为什么不是 2^31：int 数组长度不能是负数 */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /** 负载因子 0.75：空间和扩容次数之间的经验折中 */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    // ---------------- 节点：一个单向链表节点 ----------------

    /**
     * 和 JDK 一样，节点里**存了 hash**。
     * 为什么不每次重新算？因为 hashCode() 可能是用户重写的重方法，
     * 桶内逐个比对时先比 int 型的 hash 比调用 equals 便宜得多。
     */
    static class Node<K, V> {
        final int hash;      // 扰动之后的 hash（不是 key.hashCode()）
        final K key;
        V value;
        Node<K, V> next;

        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    // ---------------- 状态：整个 HashMap 就这三个字段（+ loadFactor） ----------------

    /** 桶数组。懒加载：第一次 put 才创建，new HashMap() 时不占内存 */
    Node<K, V>[] table;

    /** 已存元素个数（不是容量！容量是 table.length） */
    int size;

    /** 扩容阈值 = capacity * loadFactor。size 超过它才扩容 */
    int threshold;

    final float loadFactor;

    // ---------------- 实验用的计数器（生产代码里不会有） ----------------

    /** 一共扩容了几次 */
    int resizeCiShu;

    /** 最近一次扩容前后容量，给 Probe 打印用 */
    int shangCiCapQian;
    int shangCiCapHou;

    public MyHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }

    public MyHashMap(int chuShiRongLiang) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        // 注意：这里只是把"向上取整到 2 的幂"的结果暂存在 threshold 里，
        // 数组本身仍然是懒加载的。JDK 的构造函数也这么干。
        this.threshold = tableSizeFor(chuShiRongLiang);
    }

    // ---------------- 工具方法 ----------------

    /**
     * 扰动函数：h ^ (h >>> 16)。
     *
     * 为什么需要？桶下标是 `hash & (n-1)`，n 一般是 16，n-1 只有低 4 位有效。
     * 也就是说 hashCode 的**高 28 位全部被丢弃**。
     * 如果一批 key 的 hashCode 恰好"高位不同、低位相同"（很常见，比如 id 乘同一个系数），
     * 它们会全部挤进同一个桶 → 链表退化 → O(n)。
     *
     * 做法是把高 16 位无符号右移下来，和低 16 位异或，让低位也带上高位的随机性。
     * 成本只有一次移位 + 一次异或，这是 JDK 里性价比最高的几行之一。
     *
     * 注意 `>>>` 是**无符号**右移。写成 `>>` 的话，负数高位补 1，
     * 结果会把低 16 位污染成一片 1，等于没扰动。
     */
    static int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * 把任意容量向上取整到 2 的幂。`new MyHashMap(17)` → 32。
     *
     * 手法：cap-1 之后把最高位的 1 一路"摊"到所有低位（|= 右移 1/2/4/8/16），
     * 最后 +1 就进位成 2 的幂。
     *
     * 为什么先减 1？因为容量本身已经是 2 的幂时要原样返回。
     * 不减 1 的话 16 → 0b10000 → 摊成 0b11111 → +1 → 32，翻了一倍。
     */
    static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    public int size() {
        return size;
    }

    public int rongLiang() {
        return (table == null) ? 0 : table.length;
    }

    public int yuZhi() {
        return threshold;
    }

    // ============================================================
    // 第一段：put
    // ============================================================

    /**
     * put 全流程，四步：
     *
     *   step 1  表还没建 → 先建（懒加载，容量 16 / 阈值 12）
     *   step 2  算 hash、算下标，桶是空的 → 直接 new 一个节点，第 3 步
     *   step 3  桶非空 → 沿链找：
     *             · 找到了（hash 相同 && equals 相同）→ 覆盖 value，**直接返回老值**
     *             · 走到链尾都没有 → 尾插一个新节点
     *   step 4  ++size > threshold → 扩容（注意是"放进去之后"才检查）
     *
     * 两个容易答错的细节：
     *   · 判断相等是 `p.hash == h && (p.key == key || key.equals(p.key))` ——
     *     先用 `==` 比引用（同一个对象连 equals 都不用调），再退到 equals。
     *   · 覆盖已有 key **不会**触发扩容检查（size 没变，直接 return 了）。
     *
     * @return 被覆盖的旧 value；首次插入返回 null
     */
    public V put(K key, V value) {
        // step 1：懒加载
        if (table == null) {
            resize();
        }

        // step 2：定位
        int h = hash(key);
        int i = (table.length - 1) & h;

        if (table[i] == null) {
            // 空桶：一步到位
            table[i] = new Node<>(h, key, value, null);
        } else {
            // step 3：桶内查找 / 尾插
            Node<K, V> p = table[i];
            while (true) {
                if (p.hash == h && (p.key == key || (key != null && key.equals(p.key)))) {
                    V old = p.value;
                    p.value = value;   // 覆盖，size 不变，不检查扩容
                    return old;
                }
                if (p.next == null) {
                    p.next = new Node<>(h, key, value, null);   // JDK 8 起是尾插
                    break;
                }
                p = p.next;
            }
        }

        // step 4：放进去之后才检查，所以是"先超标，后补救"
        if (++size > threshold) {
            resize();
        }
        return null;
    }

    // ============================================================
    // 第二段：get
    // ============================================================

    /**
     * get 只有两次比较，缺一不可：
     *
     *   第一级 `hash & (n-1)`  —— 用 hash 直接跳到桶，**这一步不用 equals**
     *   第二级 桶内沿链比对   —— 这时才用 equals
     *
     * 这就是"只重写 hashCode 不去重 / 只重写 equals 不去重"两种失败方式的根源：
     *   · 只重写 hashCode：能进同一个桶，但第二级用 Object 的 == → 全留下
     *   · 只重写 equals：第一级就分到不同桶了，equals 根本没机会被调用 → 全留下
     */
    public V get(Object key) {
        if (table == null) {
            return null;   // 一次都没 put 过
        }
        int h = hash(key);
        Node<K, V> p = table[(table.length - 1) & h];
        while (p != null) {
            if (p.hash == h && (p.key == key || (key != null && key.equals(p.key)))) {
                return p.value;
            }
            p = p.next;
        }
        return null;
    }

    // ============================================================
    // 第三段：resize —— 整周最值钱的一段
    // ============================================================

    /**
     * 扩容：容量翻倍，然后把老元素搬到新表里。
     *
     * 关键问题：搬的时候要不要重新算每个元素的 hash 和下标？
     * 答案：**不需要**。因为容量翻倍时，`n-1` 只多了最高那一位：
     *
     *     旧 n = 16 → n-1 = 0000 1111   （看低 4 位）
     *     新 n = 32 → n-1 = 0001 1111   （看低 5 位）
     *                        ↑ 只多了这一位（恰好等于 oldCap = 16）
     *
     * 于是对同一个 hash：
     *     这一位是 0 → 新下标 == 旧下标 j
     *     这一位是 1 → 新下标 == j + oldCap
     *
     * 所以源码里用 `(e.hash & oldCap) == 0` 做判断，把一条旧链拆成 lo / hi 两条，
     * 分别挂到 newTab[j] 和 newTab[j + oldCap]，**全程没有一次重新取模**。
     *
     * 顺带解释"为什么 JDK 8 不会成环"：JDK 7 是头插法（每次把节点插到新表链表头部），
     * 并发扩容时两个线程可能互相把对方刚挂好的节点又插回去，形成环 → get 死循环。
     * JDK 8 改成 tail 插（lo/hi 两条链保持原相对顺序），环就没了。
     * 但注意：**丢数据依然存在**，改尾插只解决了成环，没解决并发正确性。
     */
    @SuppressWarnings("unchecked")
    final Node<K, V>[] resize() {
        Node<K, V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap;
        int newThr;

        if (oldCap > 0) {
            // ---- 正常扩容：翻倍 ----
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;   // 到顶了，从此只冲突不扩容
                return oldTab;
            }
            newCap = oldCap << 1;
            newThr = oldThr << 1;                // 阈值也翻倍：12 → 24，24 → 48
        } else {
            // ---- 首次建表 ----
            // 注意这里简化了：JDK 还要区分"构造函数给了初始容量"的情况
            newCap = (oldThr > 0) ? oldThr : DEFAULT_INITIAL_CAPACITY;
            newThr = (int) (newCap * loadFactor);
        }

        threshold = newThr;
        Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
        table = newTab;

        // ---- 搬数据 ----
        if (oldTab != null) {
            for (int j = 0; j < oldCap; j++) {
                Node<K, V> e = oldTab[j];
                if (e == null) {
                    continue;               // 空桶直接跳过（这就是"遍历慢"的来源之一）
                }
                oldTab[j] = null;           // 断开旧表引用，帮 GC

                if (e.next == null) {
                    // 只有一个节点：直接按新容量重算一次下标，不用拆链
                    newTab[e.hash & (newCap - 1)] = e;
                } else {
                    // 多个节点：拆成 lo / hi 两条链
                    Node<K, V> loHead = null, loTail = null;
                    Node<K, V> hiHead = null, hiTail = null;
                    Node<K, V> next;
                    do {
                        next = e.next;
                        if ((e.hash & oldCap) == 0) {
                            // 新增的那一位是 0 → 留在原位 j
                            if (loTail == null) {
                                loHead = e;
                            } else {
                                loTail.next = e;
                            }
                            loTail = e;
                        } else {
                            // 新增的那一位是 1 → 挪到 j + oldCap
                            if (hiTail == null) {
                                hiHead = e;
                            } else {
                                hiTail.next = e;
                            }
                            hiTail = e;
                        }
                        e = next;
                    } while (e != null);

                    if (loTail != null) {
                        loTail.next = null;     // 断尾，否则可能挂着旧链残留
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }

        // ---- 实验用计数 ----
        resizeCiShu++;
        shangCiCapQian = oldCap;
        shangCiCapHou = newCap;

        return newTab;
    }

    // ---------------- 调试辅助：看某个桶里有几个节点 ----------------

    public int tongNeiChangDu(int xiaBiao) {
        if (table == null || xiaBiao < 0 || xiaBiao >= table.length) {
            return 0;
        }
        int n = 0;
        for (Node<K, V> p = table[xiaBiao]; p != null; p = p.next) {
            n++;
        }
        return n;
    }

    /** 调试用：把某个桶里的链表打出来 */
    public String tongNeiLianBiao(int xiaBiao) {
        if (table == null || xiaBiao < 0 || xiaBiao >= table.length) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (Node<K, V> p = table[xiaBiao]; p != null; p = p.next) {
            sb.append(p).append(" -> ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 4);
        }
        return sb.append("]").toString();
    }
}
