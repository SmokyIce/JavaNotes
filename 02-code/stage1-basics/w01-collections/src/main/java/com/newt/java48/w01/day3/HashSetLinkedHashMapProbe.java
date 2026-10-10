package com.newt.java48.w01.day3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Day 3：HashSet / LinkedHashMap 的现场观测。
 *
 * ============================================================
 * 这个类到底在干什么？
 * ============================================================
 * 面经里这两句几乎是标准答案：
 *
 *   "HashSet 底层就是 HashMap，value 是一个常量 PRESENT"
 *   "LinkedHashMap 继承 HashMap，多了一条双向链表维护顺序"
 *
 * 但背下来的句子经不起追问：
 *
 *   · "value 是常量" —— 你怎么证明？常量共享有什么好处？
 *   · "靠 hashCode 和 equals 去重" —— 只重写其中一个会怎样？（两种失败方式不一样）
 *   · "无序" —— 那为什么我装 1..12 进去，打印出来正好是 1..12？
 *   · "LinkedHashMap 有序" —— 哪种序？插入序和访问序怎么切？
 *   · 它多背了两个指针，所以更慢？—— 不一定，迭代反而**更快**。
 *
 * 这个类的作用就是把上面每一条都变成可以亲眼看的数字。
 *
 * ============================================================
 * 阅读顺序建议
 * ============================================================
 * main() → A → B → C → D → E → F → G → H → I → J。
 * 每个实验对应的结论都写在方法头的注释里，先猜结果再往下看。
 *
 * 运行：run.bat day3.HashSetLinkedHashMapProbe
 *      （反射读 HashSet.map / HashMap.table 需要 --add-opens，脚本里已经带了；
 *        忘了带也不会崩，只是实验 A/I/J 会退化成降级模式）
 */
public class HashSetLinkedHashMapProbe {

    /** 保存计算结果，降低被 JIT 当作无用计算消除的风险（Day 1 就这么干的）。 */
    static volatile long consumedSum = 0;

    /** 反射能不能用，启动时探测一次。 */
    static final boolean REFLECT_OK = probeReflect();

    public static void main(String[] args) {
        System.out.println("=== Day 3 Lab : HashSet / LinkedHashMap ===");
        System.out.println("java.version = " + System.getProperty("java.version")
                + "   maxHeap = " + (Runtime.getRuntime().maxMemory() >> 20) + " MB"
                + "   reflect = " + (REFLECT_OK ? "OK" : "BLOCKED(降级模式)"));
        System.out.println();

        shiYanA_jiuJingShiShenMe();
        shiYanB_addFanHuiZhi();
        shiYanC_quChongKaoShenMe();
        shiYanD_containsFuZaDu();
        shiYanE_wuXuShiZhenXiang();
        shiYanF_failFast();
        shiYanG_sanZhongShunXu();
        shiYanH_shouXieLRU();
        shiYanI_dieDaiSuDu();
        shiYanJ_neiCunKaiXiao();

        System.out.println();
        System.out.println("=== 全部实验结束（keep alive: " + consumedSum + "） ===");
    }

    // ==================================================================
    // 实验 A：HashSet 到底是什么？
    //
    // 【结论】HashSet 里没有任何"自己的存储结构"，它就是 HashMap 的一层壳：
    //         元素 = key，value = 一个全类共享的常量 Object（源码里叫 PRESENT）。
    //
    // 【怎么证明】反射掏出 HashSet.map 字段，看三件事：
    //   ① 它的运行时类型是不是 java.util.HashMap
    //   ② 所有元素的 value 是不是**同一个对象**（用 == 比，不是 equals）
    //   ③ 这个对象是不是 HashSet.PRESENT 本身
    //
    // 【为什么 value 要共享一个常量，而不是每个元素 new 一个？】
    //   算笔账：100 万个元素，如果每个都 new Object() 当 value，
    //   JVM 里就是一个对象头 12~16 字节 + 对齐 → 白吃 16MB 堆。
    //   而 values 里的值你永远不会去读（HashSet 不暴露 value），
    //   所以完全可以用同一个对象顶替 —— 这是纯粹的省内存技巧。
    // ==================================================================
    static void shiYanA_jiuJingShiShenMe() {
        System.out.println("---- 实验 A：反射看 HashSet 的内部 ----");

        Set<String> set = new HashSet<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");

        Object mapObject = reflectGet(set, HashSet.class, "map");
        if (mapObject == null) {
            System.out.println("  反射被拦住了，跳过本实验（检查 run.bat 的 --add-opens 参数）");
            System.out.println();
            return;
        }

        System.out.println("  HashSet.map 的真实类型   : " + mapObject.getClass().getName());

        Map<?, ?> inner = (Map<?, ?>) mapObject;

        // 用一个「按引用比较」的 Set 收集所有 value，个数是 1 就说明共享同一个对象
        Set<Object> distinctValues = Collections.newSetFromMap(new IdentityHashMap<>());
        distinctValues.addAll(inner.values());

        Object oneValue = inner.values().iterator().next();
        Object presentConst = reflectGet(null, HashSet.class, "PRESENT");

        System.out.println("  HashSet 元素个数          : " + set.size()
                + "   （内部 map.size() = " + inner.size() + "）");
        System.out.println("  value 的运行时类型        : " + oneValue.getClass().getName());
        System.out.println("  value 的 identityHashCode : " + System.identityHashCode(oneValue));
        System.out.println("  不同 value 的个数(按引用)  : " + distinctValues.size() + "  ← 3 个元素只有 1 种 value");
        System.out.println("  value 就是 PRESENT 常量吗 : " + (oneValue == presentConst));

        // 顺带看一遍挂进去的 key 和 value 长什么样
        System.out.println("  map 里真实存的键值对      : ");
        for (Map.Entry<?, ?> e : inner.entrySet()) {
            System.out.println("      key=" + e.getKey() + "   value=" + e.getValue()
                    + "  (value.hashCode=" + e.getValue().hashCode() + ")");
        }

        // 再看一眼 JDK 里那几个只有一行的方法 —— 这就是 HashSet 的全部秘密
        System.out.println();
        System.out.println("  源码里这几个方法都是单行：");
        System.out.println("      add(e)      -> map.put(e, PRESENT) == null");
        System.out.println("      contains(o) -> map.containsKey(o)");
        System.out.println("      remove(o)   -> map.remove(o) == PRESENT");
        System.out.println("      size()      -> map.size()");
        System.out.println();
    }

    // ==================================================================
    // 实验 B：add 的返回值从哪来？
    //
    // 【结论】HashSet.add 返回 boolean，实现是 `map.put(e, PRESENT) == null`：
    //         put 返回 null = 桶里原本没有这个 key = 真的是新增。
    //
    // 【注意这里的 value 不是 null】所以这个判据是成立的 ——
    //   换成 HashMap 自己就没这么好命：put 返回 null 时你分不清
    //   "首次插入" 还是 "插进去的 value 本身是 null"（Day 2 记过这个坑）。
    //   HashSet 因为有 PRESENT 这个哨兵常量，天然避开了歧义。
    // ==================================================================
    static void shiYanB_addFanHuiZhi() {
        System.out.println("---- 实验 B：add / remove 的返回值语义 ----");

        Set<String> set = new HashSet<>();
        System.out.println("  add(\"a\")     -> " + set.add("a") + "     size=" + set.size());
        System.out.println("  add(\"a\") 再来 -> " + set.add("a") + "    size=" + set.size() + "   ← 重复元素被吃掉");
        System.out.println("  add(\"a\") 三来 -> " + set.add("a") + "    size=" + set.size());
        System.out.println("  remove(\"a\")  -> " + set.remove("a") + "     size=" + set.size());
        System.out.println("  remove(\"a\") 再来 -> " + set.remove("a") + "    size=" + set.size());
        System.out.println("  remove(\"不存在\") -> " + set.remove("zzz") + "    size=" + set.size());
        System.out.println();
    }

    // ==================================================================
    // 实验 C：去重到底靠什么？
    //
    // 【结论】同时靠 hashCode 和 equals，**缺一不可**，而且两种缺法的失败方式不同：
    //         · 只写 hashCode：能落到同一个桶，但桶内逐个 equals 用的是 Object 默认的
    //           「比地址」，两个新对象必然 false → 两个都进去，size=2
    //         · 只写 equals：equals 是 true，但默认 hashCode 是地址哈希，
    //           两个对象分到**不同桶**，Java 在桶层面就没机会比 equals → size=2
    //
    // 口诀：**hashCode 决定"分到哪个桶"，equals 决定"桶里是不是同一个"**。
    //
    // 【反直觉点 · C3】可变对象当 key 会变成"幽灵"：
    //         放进去之后改了参与 hashCode 计算的字段，对象还在集合里（size 不变），
    //         但 contains 查不到了 —— 因为它现在拿着新的 hashCode 去别的桶里找。
    //         所以 HashSet 的 key 必须是**不可变对象**（String / Integer 这种）。
    // ==================================================================
    static class ZuHe {
        final int a;
        final int b;

        ZuHe(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return "(" + a + "," + b + ")";
        }
    }

    /** 只重写 hashCode，不重写 equals。 */
    static final class ZhiXieHashCode extends ZuHe {
        ZhiXieHashCode(int a, int b) {
            super(a, b);
        }

        @Override
        public int hashCode() {
            return a * 31 + b;
        }
    }

    /** 只重写 equals，不重写 hashCode。 */
    static final class ZhiXieEquals extends ZuHe {
        ZhiXieEquals(int a, int b) {
            super(a, b);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ZhiXieEquals)) {
                return false;
            }
            ZhiXieEquals other = (ZhiXieEquals) o;
            return a == other.a && b == other.b;
        }
    }

    /** 两个都重写 —— 这才是正确的写法。 */
    static final class DouXie extends ZuHe {
        DouXie(int a, int b) {
            super(a, b);
        }

        @Override
        public int hashCode() {
            return a * 31 + b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DouXie)) {
                return false;
            }
            DouXie other = (DouXie) o;
            return a == other.a && b == other.b;
        }
    }

    static void shiYanC_quChongKaoShenMe() {
        System.out.println("---- 实验 C：去重，是 hashCode 和 equals 一起干的 ----");

        Set<ZhiXieHashCode> c1 = new HashSet<>();
        c1.add(new ZhiXieHashCode(1, 2));
        c1.add(new ZhiXieHashCode(1, 2));
        System.out.println("  C1 只重写 hashCode，塞两个 (1,2)   -> size = " + c1.size()
                + "   ← 期望 1，实际 2：桶对了，但桶内 equals 用的是地址比较");

        Set<ZhiXieEquals> c2 = new HashSet<>();
        c2.add(new ZhiXieEquals(1, 2));
        c2.add(new ZhiXieEquals(1, 2));
        System.out.println("  C2 只重写 equals，塞两个 (1,2)     -> size = " + c2.size()
                + "   ← 期望 1，实际 2：equals 对了，但两个对象根本不在一个桶里");

        Set<DouXie> c3 = new HashSet<>();
        c3.add(new DouXie(1, 2));
        c3.add(new DouXie(1, 2));
        System.out.println("  C3 两个都重写，塞两个 (1,2)        -> size = " + c3.size()
                + "   ← 这才是对的");

        // C4：可变对象当 key —— 放进去之后改字段
        Set<KeBian> c4 = new HashSet<>();
        KeBian key = new KeBian(1);
        c4.add(key);
        boolean beforeContains = c4.contains(key);
        int beforeSize = c4.size();
        key.x = 2;                                  // 改了参与 hashCode 的字段
        boolean afterContains = c4.contains(key);
        int afterSize = c4.size();

        System.out.println();
        System.out.println("  C4 可变对象当 key：");
        System.out.println("      放入 (x=1) 后 contains -> " + beforeContains + "    size=" + beforeSize);
        System.out.println("      把 x 改成 2 后 contains -> " + afterContains + "   size=" + afterSize
                + "   ← 对象还在里面，却再也查不到了");
        System.out.println("      直接遍历能看见它吗     -> ");
        for (KeBian k : c4) {
            System.out.println("          " + k + "  ← 它在集合里，只是被「弄丢」了");
        }
        System.out.println();
    }

    /** 故意写歪的类：字段可变、hashCode 依赖字段。 */
    static final class KeBian {
        int x;

        KeBian(int x) {
            this.x = x;
        }

        @Override
        public int hashCode() {
            return x;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof KeBian && ((KeBian) o).x == x;
        }

        @Override
        public String toString() {
            return "KeBian{x=" + x + "}";
        }
    }

    // ==================================================================
    // 实验 D：contains 的复杂度 —— HashSet 的看家本领
    //
    // 【结论】ArrayList.contains 是 O(n) 逐个 equals；HashSet.contains 是
    //         O(1) 均摊（hashCode 定位桶 + 桶内少量比较）。
    //         元素越多，差距越大，而且差距随规模**线性增长**。
    //
    // 这个实验的意义不在于"Set 比 List 快"（这是常识），
    // 而在于体会：**同样的 contains 调用，代价可以差三个数量级。**
    // 业务里"判断某个 id 是否处理过"这类代码，写成 List 就是慢性自杀。
    // ==================================================================
    static void shiYanD_containsFuZaDu() {
        System.out.println("---- 实验 D：contains 的复杂度对比 ----");

        int n = 200_000;
        List<Integer> list = new ArrayList<>(n);
        Set<Integer> set = new HashSet<>(n);
        for (int i = 0; i < n; i++) {
            list.add(i);
            set.add(i);
        }

        int probes = 5_000;
        long[] probeTargets = new long[probes];
        for (int i = 0; i < probes; i++) {
            probeTargets[i] = ((i * 2654435761L) >>> 3) % n;
        }

        // 预热
        for (int round = 0; round < 3; round++) {
            containsLoop(list, probeTargets);
            containsLoop(set, probeTargets);
        }

        long listNanos = measureNanos(() -> containsLoop(list, probeTargets));
        long setNanos = measureNanos(() -> containsLoop(set, probeTargets));

        System.out.printf("  元素规模 %d，随机探测 %d 次%n", n, probes);
        System.out.printf("  %-24s %14s %14s%n", "实现", "总耗时(ns)", "每次(ns)");
        System.out.printf("  %-24s %14d %14.2f%n", "ArrayList.contains", listNanos, listNanos / (double) probes);
        System.out.printf("  %-24s %14d %14.2f%n", "HashSet.contains", setNanos, setNanos / (double) probes);
        System.out.printf("  ArrayList / HashSet = %.1f 倍%n", listNanos / (double) Math.max(setNanos, 1));
        System.out.println("  注：O(1) 和 O(n) 的差距，直观表现就是**随 n 线性拉开**。");
        System.out.println();
    }

    static void containsLoop(Object c, long[] targets) {
        long hit = 0;
        if (c instanceof List) {
            List<?> list = (List<?>) c;
            for (long t : targets) {
                if (list.contains((int) t)) {
                    hit++;
                }
            }
        } else {
            Set<?> set = (Set<?>) c;
            for (long t : targets) {
                if (set.contains((int) t)) {
                    hit++;
                }
            }
        }
        consumedSum = hit;
    }

    // ==================================================================
    // 实验 E："无序"到底是什么意思？
    //
    // 【结论】HashSet 的迭代顺序 = 桶下标从小到大、桶内链表顺序。
    //         桶下标 = hash & (capacity-1)，所以顺序由**两件事**决定：
    //         ① 元素的 hashCode   ② 当时的 capacity
    //         任何一个变了，顺序就变。这叫"无序"，不叫"随机"。
    //
    // 【陷阱】装 1..12 进去，打印出来正好是 1..12 —— 看起来超有序！
    //         因为 Integer.hashCode() 就是它本身，容量 16 时 n & 15 = n（n<16），
    //         于是"桶下标 = 数值"，自然排好。
    //         **别被小整数骗了，换字符串立刻露馅。**
    //
    // 【证明它不稳定】同样的 12 个字符串，装进两个容量不同的 HashSet，
    //         迭代顺序就不一样（因为 & 15 和 & 63 取的是不同的低位）。
    // ==================================================================
    static void shiYanE_wuXuShiZhenXiang() {
        System.out.println("---- 实验 E：\"无序\"到底有多无序 ----");

        Set<Integer> ints = new HashSet<>();
        for (int i = 1; i <= 12; i++) {
            ints.add(i);
        }
        System.out.println("  E1 装 1..12 打印     : " + ints);
        System.out.println("      ↑ 看着挺有序？假象。Integer.hashCode()=本身，容量 16 时 n&15=n。");

        List<String> words = new ArrayList<>();
        words.add("delta");
        words.add("alpha");
        words.add("charlie");
        words.add("bravo");
        words.add("echo");
        words.add("golf");
        words.add("hotel");
        words.add("india");
        words.add("juliet");
        words.add("kilo");
        words.add("lima");
        words.add("mike");

        Set<String> w16 = new HashSet<>(16);
        Set<String> w64 = new HashSet<>(64);
        w16.addAll(words);
        w64.addAll(words);

        System.out.println();
        System.out.println("  E2 同样的 12 个字符串，插入顺序 = " + words);
        System.out.println("     装进 new HashSet<>(16) : " + w16);
        System.out.println("     装进 new HashSet<>(64) : " + w64);
        System.out.println("     同样元素、同样插入顺序，容量不同 → 顺序不同。这就是'无序'。");
        System.out.println("     想要顺序：LinkedHashSet（插入序）或 TreeSet（排序）。");
        System.out.println();
    }

    // ==================================================================
    // 实验 F：遍历时改集合会炸 —— fail-fast
    //
    // 【结论】集合内部有个 modCount 计数，迭代器创建时记下快照，
    //         每次 next() 都校验 modCount 有没有变，变了就抛
    //         ConcurrentModificationException。
    //
    // 【反直觉点】只有**真的改到了结构**（remove 命中 / 新增）时 modCount 才 +1。
    //         所以"遍历中调用 remove 一个不存在的元素"不会抛异常 ——
    //         这不是安全，只是恰好没被发现。别把"没报错"当"没问题"。
    //
    // 【正确写法】iterator.remove()（迭代器自己同步更新 modCount）、
    //         或 removeIf(...)。JDK 8 之后 removeIf 基本是最优解。
    // ==================================================================
    static void shiYanF_failFast() {
        System.out.println("---- 实验 F：遍历中修改集合 ----");

        Set<String> set = new HashSet<>();
        set.add("a");
        set.add("b");
        set.add("c");

        // F1：增强 for 里 remove 命中的元素
        try {
            for (String s : set) {
                if ("b".equals(s)) {
                    set.remove(s);
                }
            }
            System.out.println("  F1 增强 for + remove(命中)     -> 没抛异常？（不应该）");
        } catch (ConcurrentModificationException e) {
            System.out.println("  F1 增强 for + remove(命中)     -> ConcurrentModificationException ✓");
        }

        // F2：遍历中 remove 一个**不存在**的元素
        Set<String> set2 = new HashSet<>(set);
        try {
            for (String s : set2) {
                set2.remove("zzz-not-exists");
            }
            System.out.println("  F2 增强 for + remove(不存在)   -> 正常结束，没抛 "
                    + "  ← 因为 modCount 根本没变，不是它安全");
        } catch (ConcurrentModificationException e) {
            System.out.println("  F2 增强 for + remove(不存在)   -> 抛了异常（与预期不符）");
        }

        // F3：iterator.remove() 才是对的
        Set<String> set3 = new HashSet<>();
        set3.add("a");
        set3.add("b");
        set3.add("c");
        Iterator<String> it = set3.iterator();
        while (it.hasNext()) {
            if ("b".equals(it.next())) {
                it.remove();
            }
        }
        System.out.println("  F3 iterator.remove()           -> 正常结束，剩下 " + set3);

        // F4：removeIf 一行搞定
        Set<String> set4 = new HashSet<>();
        set4.add("a");
        set4.add("b");
        set4.add("c");
        set4.removeIf("b"::equals);
        System.out.println("  F4 removeIf(\"b\"::equals)      -> 正常结束，剩下 " + set4);
        System.out.println();
    }

    // ==================================================================
    // 实验 G：LinkedHashMap 的"有序"有几种？
    //
    // 【结论】LinkedHashMap 继承 HashMap，额外用一条双向链表把所有节点串起来，
    //         所以它迭代时走的是链表，不是桶数组。顺序由 accessOrder 决定：
    //           accessOrder = false（默认）→ 插入序
    //           accessOrder = true          → 访问序（get 也会把节点移到队尾）
    //
    // 【怎么实现的】HashMap 在 put/get/remove 的收尾处留了三个空钩子：
    //           afterNodeAccess / afterNodeInsertion / afterNodeRemoval
    //         LinkedHashMap 重写它们来维护链表 —— 典型的**模板方法模式**。
    //         HashMap 负责"存得对"，子类负责"额外做点事"，谁也不用改谁。
    //
    // 【反直觉点】accessOrder=true 时，get() 会**修改结构**（链表重排 + modCount++）。
    //         也就是说这个 get 不是只读操作，多线程下更容易出问题；
    //         而且"读一下"就会改变后续的迭代顺序 —— 这是 LRU 的立身之本。
    // ==================================================================
    static void shiYanG_sanZhongShunXu() {
        System.out.println("---- 实验 G：LinkedHashMap 的插入序 / 访问序 ----");

        // 故意用一个不太"自然有序"的插入顺序，方便看出差异
        String[] keys = {"delta", "alpha", "charlie", "bravo", "echo", "golf"};

        Map<String, Integer> hashMap = new HashMap<>();
        Map<String, Integer> linkedInsertOrder = new LinkedHashMap<>();
        Map<String, Integer> linkedAccessOrder = new LinkedHashMap<>(16, 0.75f, true);

        for (int i = 0; i < keys.length; i++) {
            hashMap.put(keys[i], i);
            linkedInsertOrder.put(keys[i], i);
            linkedAccessOrder.put(keys[i], i);
        }

        System.out.println("  插入顺序                : " + String.join(" ", keys));
        System.out.println("  HashMap                 : " + hashMap.keySet());
        System.out.println("  LinkedHashMap(默认)     : " + linkedInsertOrder.keySet());
        System.out.println("  LinkedHashMap(访问序)   : " + linkedAccessOrder.keySet() + "   ← 还没访问，暂时一样");

        // 访问序的那个，get 一下中间那个 key
        linkedAccessOrder.get("charlie");
        System.out.println();
        System.out.println("  get(\"charlie\") 之后：");
        System.out.println("      LinkedHashMap(默认)   : " + linkedInsertOrder.keySet() + "   ← 没动");
        System.out.println("      LinkedHashMap(访问序) : " + linkedAccessOrder.keySet() + "   ← charlie 被挪到队尾");

        // 再 get 两次，看看顺序怎么滚
        linkedAccessOrder.get("delta");
        linkedAccessOrder.get("alpha");
        System.out.println("  再 get(\"delta\")、get(\"alpha\") 之后：");
        System.out.println("      LinkedHashMap(访问序) : " + linkedAccessOrder.keySet());
        System.out.println("      ↑ 队首 = 最久没被访问的，队尾 = 最近访问的。这就是 LRU 的原料。");

        // 顺带看一眼 LinkedHashSet：就是 LinkedHashMap 的壳，顺序是插入序
        Set<String> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.addAll(java.util.Arrays.asList(keys));
        System.out.println();
        System.out.println("  LinkedHashSet           : " + linkedHashSet + "   ← 保持插入序");
        System.out.println("  TreeSet（对照，会排序） : " + new TreeSet<>(linkedHashSet));
        System.out.println();
    }

    // ==================================================================
    // 实验 H：用 LinkedHashMap 手写 LRU —— 只要 6 行
    //
    // 【结论】只要三样东西：accessOrder=true + 重写 removeEldestEntry + 一个容量。
    //         这就是 JDK 自带的、最经典的 LRU 实现，面试常问"怎么实现 LRU"，
    //         标准答案之一就是这个匿名内部类。
    //
    // 【两个坑】
    //   ① 判断写 `size() > cap`，不是 `>=`。写成 >= 会少存一个
    //      （第 3 个元素一进来就触发淘汰，永远只保留 cap-1 个）。
    //   ② removeEldestEntry 只在**插入新 key** 时被调用（afterNodeInsertion）。
    //      覆盖已有 key 走的是 afterNodeAccess 分支，不触发淘汰 —— 但访问序会把它
    //      挪到队尾，所以语义上仍然正确。
    // ==================================================================
    static void shiYanH_shouXieLRU() {
        System.out.println("---- 实验 H：6 行代码的 LRU ----");

        final int capacity = 3;
        Map<String, Integer> lru = new LinkedHashMap<String, Integer>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                boolean tooBig = size() > capacity;
                if (tooBig) {
                    System.out.println("        淘汰最久未使用的：" + eldest.getKey()
                            + "（removeEldestEntry 调用时新元素已插入，所以此刻 size = " + size() + "）");
                }
                return tooBig;
            }
        };

        System.out.println("  容量 " + capacity + "，依次 put a..e：");
        for (String k : new String[]{"a", "b", "c", "d", "e"}) {
            lru.put(k, 1);
            System.out.println("      put(" + k + ") 后 -> " + lru.keySet() + "   size=" + lru.size());
        }

        System.out.println();
        System.out.println("  演示「访问改变淘汰顺序」：");
        System.out.println("      get(\"d\") 让 d 变成最新 -> " + (lru.get("d") != null ? "命中" : "未命中")
                + "，顺序 " + lru.keySet());
        lru.put("f", 1);
        System.out.println("      put(f) 后 -> " + lru.keySet()
                + "   ← 淘汰的是队首 c，刚 get 过的 d 保住了（这就是 LRU 想要的行为）");

        System.out.println();
        System.out.println("  坑演示：把 `>` 写成 `>=`");
        Map<String, Integer> wrong = new LinkedHashMap<String, Integer>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return size() >= capacity;      // 错！多淘汰一个
            }
        };
        for (String k : new String[]{"a", "b", "c", "d", "e"}) {
            wrong.put(k, 1);
        }
        System.out.println("      容量声明 3，实际只留住了 " + wrong.size() + " 个：" + wrong.keySet());
        System.out.println();
    }

    // ==================================================================
    // 实验 I：迭代速度 —— 更重的 LinkedHashMap 反而更快？
    //
    // 【结论】迭代 LinkedHashMap 比迭代 HashMap 快得多，而且数据越稀疏差得越多。
    //         原因：HashMap 的迭代器要**从头扫完整个 table 数组**，包括所有空桶，
    //              成本是 O(capacity + size)；LinkedHashMap 只走链表，成本是 O(size)。
    //
    //         table 只扩容、不缩容。所以删掉大量元素后，HashMap 的 capacity
    //         还停在原来的大小，遍历时白白扫几十万个 null。
    //
    // 【反直觉点】"LinkedHashMap 每个节点多两个指针，所以更重" —— 对，
    //         但它**遍历更快**。内存开销和遍历性能是两笔独立的账，别混着算。
    // ==================================================================
    static void shiYanI_dieDaiSuDu() {
        System.out.println("---- 实验 I：迭代速度对比（HashMap vs LinkedHashMap） ----");

        int total = 300_000;
        int keep = 1_000;

        Map<Integer, Integer> hashMap = new HashMap<>();
        Map<Integer, Integer> linkedMap = new LinkedHashMap<>();
        for (int i = 0; i < total; i++) {
            hashMap.put(i, i);
            linkedMap.put(i, i);
        }
        // 删掉绝大部分，制造"size 很小但 capacity 很大"的稀疏场景
        for (int i = keep; i < total; i++) {
            hashMap.remove(i);
            linkedMap.remove(i);
        }

        for (int round = 0; round < 3; round++) {
            sumKeys(hashMap);
            sumKeys(linkedMap);
        }

        final int rounds = 20;
        long hashNanos = measureNanos(() -> {
            for (int r = 0; r < rounds; r++) {
                sumKeys(hashMap);
            }
        });
        long linkedNanos = measureNanos(() -> {
            for (int r = 0; r < rounds; r++) {
                sumKeys(linkedMap);
            }
        });

        System.out.printf("  曾经放入 %d 个，现在各剩 %d 个%n", total, hashMap.size());
        System.out.printf("  HashMap       table 长度（容量）: %d%n", tableLength(hashMap));
        System.out.printf("  LinkedHashMap table 长度（容量）: %d%n", tableLength(linkedMap));
        System.out.printf("  %-26s %16s %16s%n", "遍历方式", "总耗时(ns)", "每次(ns)");
        System.out.printf("  %-26s %16d %16.0f%n", "for-each HashMap", hashNanos, hashNanos / (double) rounds);
        System.out.printf("  %-26s %16d %16.0f%n", "for-each LinkedHashMap", linkedNanos, linkedNanos / (double) rounds);
        System.out.printf("  HashMap / LinkedHashMap = %.1f 倍%n", hashNanos / (double) Math.max(linkedNanos, 1));
        System.out.println("  原因：HashMap 要扫完 " + tableLength(hashMap) + " 个桶（绝大多数是 null），"
                + "LinkedHashMap 只走 " + linkedMap.size() + " 个链节点。");
        System.out.println();
    }

    static void sumKeys(Map<Integer, Integer> map) {
        long sum = 0;
        for (Integer k : map.keySet()) {
            sum += k;
        }
        consumedSum = sum;
    }

    // ==================================================================
    // 实验 J：内存开销 —— 那"多出来的两个指针"值多少钱？
    //
    // 【结论】LinkedHashMap.Entry 在 HashMap.Node 的基础上多了 before / after
    //         两个引用。开了指针压缩（默认 64 位 JVM、堆 < 32G）时是 8 字节/元素。
    //
    //         这里用 Runtime 的堆占用量粗略估算 —— 它包含 GC 噪声，
    //         所以**只看数量级和理论值是否对得上**，别纠结小数点。
    // ==================================================================
    static void shiYanJ_neiCunKaiXiao() {
        System.out.println("---- 实验 J：LinkedHashMap 多花多少内存 ----");

        int n = 500_000;
        long hashBytes = measureExtraHeap(() -> {
            Map<Integer, Integer> m = new HashMap<>();
            for (int i = 0; i < n; i++) {
                m.put(i, i);
            }
            return m;
        });
        long linkedBytes = measureExtraHeap(() -> {
            Map<Integer, Integer> m = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) {
                m.put(i, i);
            }
            return m;
        });

        System.out.printf("  各放 %d 个 <Integer,Integer>%n", n);
        System.out.printf("  %-18s %14s %16s%n", "实现", "额外堆(MB)", "字节/元素");
        System.out.printf("  %-18s %14.2f %16.1f%n", "HashMap", hashBytes / 1024.0 / 1024.0, hashBytes / (double) n);
        System.out.printf("  %-18s %14.2f %16.1f%n", "LinkedHashMap", linkedBytes / 1024.0 / 1024.0, linkedBytes / (double) n);
        System.out.printf("  %-18s %14.2f %16.1f%n", "差值", (linkedBytes - hashBytes) / 1024.0 / 1024.0,
                (linkedBytes - hashBytes) / (double) n);
        System.out.println("  理论差值 = 2 个引用 × 4 字节（压缩指针）= 8 字节/元素");
        System.out.println("  → 差值 = " + (linkedBytes - hashBytes) / (double) n + " 字节/元素，量级对得上");
        System.out.println("  注：Runtime 读数含 GC / 装箱对象的噪声，只看数量级。key 和 value 都装箱了，"
                + "两边一样，所以差值才是干净的（这就是为什么做'差值'而不是绝对值）。");
        System.out.println();
    }

    static long measureExtraHeap(java.util.function.Supplier<Object> supplier) {
        requestGarbageCollection();
        long before = usedHeapBytes();
        Object kept = supplier.get();
        requestGarbageCollection();
        long after = usedHeapBytes();
        // 确保对象不会被优化掉
        consumedSum += System.identityHashCode(kept);
        return after - before;
    }

    static long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    static void requestGarbageCollection() {
        for (int i = 0; i < 2; i++) {
            System.gc();
            try {
                Thread.sleep(120);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ==================================================================
    // 工具方法
    // ==================================================================

    static long measureNanos(Runnable task) {
        long start = System.nanoTime();
        task.run();
        return System.nanoTime() - start;
    }

    /** 反射读字段；拿不到就返回 null（降级）。 */
    static Object reflectGet(Object target, Class<?> declaring, String name) {
        try {
            Field f = declaring.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 反射拿 HashMap.table 的长度 —— 也就是容量。 */
    static int tableLength(Map<?, ?> map) {
        Object table = reflectGet(map, HashMap.class, "table");
        return table instanceof Object[] ? ((Object[]) table).length : -1;
    }

    static boolean probeReflect() {
        try {
            Field f = HashMap.class.getDeclaredField("table");
            f.setAccessible(true);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
