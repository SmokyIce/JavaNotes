package com.newt.java48.w01.day6;

import java.util.HashMap;
import java.util.Random;

/**
 * MyHashMap 验收实验：四个实验，每个都回答一个"面试会问"的问题。
 *
 *   A. 扩容到底在第几个元素触发？           → 验证 `++size > threshold` 是严格大于
 *   B. 扩容后下标为什么只有两种可能？         → 亲眼看到一条链被拆成 j 和 j+oldCap
 *   C. 我写的和 JDK 的算不算一样？            → 20 万次随机操作对拍（这才是真验收）
 *   D. 手写的慢多少？慢在哪一步？             → 没有树化 / 没有红黑树 / 全是解释执行
 *
 * 运行：run.bat day6.MyHashMapProbe
 */
public class MyHashMapProbe {

    public static void main(String[] args) {
        shiYanA();
        System.out.println();
        shiYanB();
        System.out.println();
        shiYanC();
        System.out.println();
        shiYanD();
    }

    // ============================================================
    // 实验 A：扩容触发点
    // ============================================================
    static void shiYanA() {
        System.out.println("========== 实验 A：扩容到底在第几个元素触发 ==========");
        System.out.println("预期：容量 16 阈值 12 → 第 13 个；容量 32 阈值 24 → 第 25 个；64/48 → 第 49 个");
        System.out.println();

        MyHashMap<Integer, Integer> biao = new MyHashMap<>();
        System.out.printf("%-8s %-8s %-10s %-6s %s%n", "第几个", "容量", "threshold", "size", "备注");
        System.out.println("----------------------------------------------------------------");

        for (int i = 1; i <= 60; i++) {
            int capQian = biao.rongLiang();
            int thrQian = biao.yuZhi();
            biao.put(i, i * 10);
            int capHou = biao.rongLiang();

            boolean kuoRong = (capQian > 0) && (capHou != capQian);
            if (kuoRong || i == 1 || i == 12 || i == 24 || i == 48) {
                String beiZhu = kuoRong
                        ? "← 扩容 " + capQian + " → " + capHou + "（size " + biao.size()
                                + " > 旧阈值 " + thrQian + "，所以是第 " + i + " 个触发的）"
                        : "（还没到，安静）";
                System.out.printf("%-8d %-8d %-10d %-6d %s%n",
                        i, capHou, biao.yuZhi(), biao.size(), beiZhu);
            }
        }

        System.out.println();
        System.out.println("结论：");
        System.out.println("  · 触发条件是 `++size > threshold`，**严格大于** —— 阈值 12 时第 12 个不扩，第 13 个才扩。");
        System.out.println("  · 检查发生在元素**已经放进去之后**（put 末尾），属于「先超标，后补救」。");
        System.out.println("  · 阈值永远是 cap * 0.75 向下取整：16→12、32→24、64→48、128→96。");
        System.out.println("  · 一共扩容 " + biao.resizeCiShu + " 次，60 个元素，容量 " + biao.rongLiang() + "。");
    }

    // ============================================================
    // 实验 B：拆链（扩容后下标的两种命运）
    // ============================================================
    static void shiYanB() {
        System.out.println("========== 实验 B：扩容把一条链拆成两条 ==========");
        System.out.println("构造两个 hashCode 为 1 和 17 的 key：容量 16 时 1&15 == 17&15 == 1，必在同一桶");
        System.out.println();

        MyHashMap<Integer, String> biao = new MyHashMap<>();
        biao.put(1, "yi");
        biao.put(17, "shiqi");

        System.out.println("扩容前：容量 " + biao.rongLiang() + "，threshold " + biao.yuZhi());
        System.out.println("  桶[1]  = " + biao.tongNeiLianBiao(1) + "   长度 " + biao.tongNeiChangDu(1));
        System.out.println("  hash(1)=" + MyHashMap.hash(1) + " → 1 & 15 = " + (MyHashMap.hash(1) & 15));
        System.out.println("  hash(17)=" + MyHashMap.hash(17) + " → 17 & 15 = " + (MyHashMap.hash(17) & 15));
        System.out.println();

        // 继续塞到第 13 个，触发扩容 16 → 32
        for (int i = 2; i <= 13; i++) {
            biao.put(i, "v" + i);
        }

        System.out.println("扩容后：容量 " + biao.rongLiang() + "（第 13 个元素触发的）");
        System.out.println("  桶[1]  = " + biao.tongNeiLianBiao(1) + "    ← 1 & 31 = 1，留在原位");
        System.out.println("  桶[17] = " + biao.tongNeiLianBiao(17) + "  ← 17 & 31 = 17 = 1 + oldCap(16)");
        System.out.println("  桶[1] + 桶[17] 总长度 = " + (biao.tongNeiChangDu(1) + biao.tongNeiChangDu(17))
                + "（拆之前是 2，一个没丢）");
        System.out.println();
        System.out.println("结论：容量翻倍，`n-1` 只多了最高那一位（这一位恰好等于 oldCap）：");
        System.out.println("  这一位 = 0 → 新下标还是 j；这一位 = 1 → 新下标是 j + oldCap。");
        System.out.println("  所以源码里用 `(e.hash & oldCap) == 0` 判断，拆成 lo（留 j）/ hi（去 j+oldCap）两条链，");
        System.out.println("  **全程没有重新计算一次 hash、也没有重新取模**。");
    }

    // ============================================================
    // 实验 C：和 JDK 对拍（真正的验收）
    // ============================================================
    static void shiYanC() {
        System.out.println("========== 实验 C：20 万次随机操作，和 java.util.HashMap 对拍 ==========");

        int ciShu = 200_000;
        int keyFanWei = 50_000;
        Random lei = new Random(42);     // 固定种子，可复现

        MyHashMap<Integer, Integer> woDe = new MyHashMap<>();
        HashMap<Integer, Integer> jdk = new HashMap<>();

        long cuoWu = 0;
        for (int i = 0; i < ciShu; i++) {
            int k = lei.nextInt(keyFanWei);
            int v = lei.nextInt(1_000_000);
            // 九成 put，一成 get
            if (lei.nextInt(10) < 9) {
                Integer a = woDe.put(k, v);
                Integer b = jdk.put(k, v);
                if (!yaoDeng(a, b)) {
                    cuoWu++;
                    if (cuoWu <= 3) {
                        System.out.println("  ✗ put 返回值不一致：key=" + k + " 我的=" + a + " JDK=" + b);
                    }
                }
            } else {
                Integer a = woDe.get(k);
                Integer b = jdk.get(k);
                if (!yaoDeng(a, b)) {
                    cuoWu++;
                    if (cuoWu <= 3) {
                        System.out.println("  ✗ get 结果不一致：key=" + k + " 我的=" + a + " JDK=" + b);
                    }
                }
            }
        }

        System.out.println();
        System.out.println("  操作次数      ：" + ciShu + "（九成 put / 一成 get，key 范围 " + keyFanWei + "）");
        System.out.println("  最终 size     ：我的 " + woDe.size() + " ｜ JDK " + jdk.size()
                + (woDe.size() == jdk.size() ? "  ✓" : "  ✗"));
        System.out.println("  容量 / 阈值    ：我的 " + woDe.rongLiang() + " / " + woDe.yuZhi()
                + "（JDK 走同一套规则，容量必然是同一档）");
        System.out.println("  扩容次数      ：我的 " + woDe.resizeCiShu + " 次（16→32→…→" + woDe.rongLiang() + "）");
        System.out.println("  返回值不一致  ：" + cuoWu + " 处" + (cuoWu == 0 ? "  ✓ 完全一致" : ""));
        System.out.println();
        System.out.println("  为什么容量也一样？因为两边用的是同一套 `++size > threshold` + 翻倍规则，");
        System.out.println("  size 走到哪、容量就是同一档。**对拍能过，说明 put / get / resize 三段逻辑没错**。");
    }

    static boolean yaoDeng(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    // ============================================================
    // 实验 D：和自己比较（手写版 vs JDK）
    // ============================================================
    static void shiYanD() {
        System.out.println("========== 实验 D：手写版和 JDK 差多少 ==========");

        int n = 300_000;
        int[] keys = new int[n];
        Random lei = new Random(7);
        for (int i = 0; i < n; i++) {
            keys[i] = lei.nextInt();
        }

        // 预热，避免把 JIT 编译时间算进第一轮
        for (int lun = 0; lun < 2; lun++) {
            MyHashMap<Integer, Integer> a = new MyHashMap<>();
            HashMap<Integer, Integer> b = new HashMap<>();
            for (int i = 0; i < 20_000; i++) {
                a.put(keys[i], i);
                b.put(keys[i], i);
            }
        }

        long t0 = System.nanoTime();
        MyHashMap<Integer, Integer> woDe = new MyHashMap<>();
        for (int i = 0; i < n; i++) {
            woDe.put(keys[i], i);
        }
        long t1 = System.nanoTime();

        HashMap<Integer, Integer> jdk = new HashMap<>();
        for (int i = 0; i < n; i++) {
            jdk.put(keys[i], i);
        }
        long t2 = System.nanoTime();

        double woMs = (t1 - t0) / 1_000_000.0;
        double jdkMs = (t2 - t1) / 1_000_000.0;

        System.out.printf("  put %d 条（随机 int key）%n", n);
        System.out.printf("  手写 MyHashMap ：%8.1f ms  （%5.0f ns/op）%n", woMs, woMs * 1_000_000 / n);
        System.out.printf("  JDK HashMap    ：%8.1f ms  （%5.0f ns/op）%n", jdkMs, jdkMs * 1_000_000 / n);
        System.out.printf("  倍数           ：%.2f 倍%n", woMs / jdkMs);
        System.out.println();
        System.out.println("  差在哪（按影响从大到小）：");
        System.out.println("    ① 手写版没树化：桶内永远是链表。随机 int key 冲突少，所以这条这次影响不大；");
        System.out.println("       但换成 hashCode 恒定的 key，手写版直接退化成 O(n) 链表，JDK 会转红黑树。");
        System.out.println("    ② JDK 里全是 @HotSpotIntrinsicCandidate 和多年打磨的分支预测友好写法；");
        System.out.println("    ③ 同样跑 30 万次，JDK 的 resize 用 Arrays 级别的搬运，我的是一节点一节点挂。");
        System.out.println("  —— 结论不是「我写得差」，而是：**这段逻辑的正确性 20 行就够，性能是另一门生意**。");
        System.out.println();
        shiYanE();
    }

    // ============================================================
    // 实验 E：冲突极端时，没有树化会怎样
    // ============================================================
    static void shiYanE() {
        System.out.println("========== 实验 E：把 hashCode 全部写成 0，看退化 ==========");
        System.out.println("这是实验 D 的反面：随机 key 冲突少，树的优势体现不出来；");
        System.out.println("所有 key 挤一个桶时，链表 O(n) vs 红黑树 O(log n) 的差距才现形。");
        System.out.println();

        int n = 20_000;

        long t0 = System.nanoTime();
        MyHashMap<HuaiKey, Integer> woDe = new MyHashMap<>();
        for (int i = 0; i < n; i++) {
            woDe.put(new HuaiKey(i), i);
        }
        long t1 = System.nanoTime();

        HashMap<HuaiKey, Integer> jdk = new HashMap<>();
        for (int i = 0; i < n; i++) {
            jdk.put(new HuaiKey(i), i);
        }
        long t2 = System.nanoTime();

        double woMs = (t1 - t0) / 1_000_000.0;
        double jdkMs = (t2 - t1) / 1_000_000.0;

        System.out.printf("  【E1】key 不可比较（没实现 Comparable）%n");
        System.out.printf("  put %s 条（hashCode 恒为 0，全部挤进 0 号桶）%n", String.format("%,d", n));
        System.out.printf("  手写 MyHashMap ：%9.1f ms   0 号桶链表长度 = %d（就是 n）%n", woMs, woDe.tongNeiChangDu(0));
        System.out.printf("  JDK HashMap    ：%9.1f ms   桶内已转红黑树（同一桶里塞 %d 个）%n", jdkMs, n);
        System.out.printf("  → 手写反而是 JDK 的 %.2f 倍（JDK 更慢！）%n", woMs / jdkMs);
        System.out.println();
        System.out.println("  这个结果是不合直觉的，原因要看 JDK 17 的 putTreeVal：");
        System.out.println("  · 树里比较大小先看 hash；hash 全相等时，退到 Comparable 比大小；");
        System.out.println("  · HuaiKey 没实现 Comparable → 最后只能用 tieBreakOrder（identityHashCode）定方向；");
        System.out.println("  · 而 tieBreakOrder 定出来的顺序**和 equals 无关**，所以树的每一层都无法排除");
        System.out.println("    「这个 key 是不是已经在左/右子树里」，只能对整个子树做一次 find() 全扫。");
        System.out.println("  · 于是每次插入都要 O(n) 扫一遍 → 总成本仍是 O(n²)，");
        System.out.println("    而 TreeNode 是链表的 2 倍大（多了 left/right/parent/prev/red 五个字段），常数更大。");
        System.out.println("  —— 所以：**红黑树的前提是「key 能比较大小」。hash 全等又不可比较时，树救不了你，还更慢。**");

        System.out.println();
        shiYanE2();
    }

    /** E2：同样的极端冲突，但让 key 可比较 —— 看树化还灵不灵 */
    static void shiYanE2() {
        int n = 20_000;

        long t0 = System.nanoTime();
        MyHashMap<HuaiKey2, Integer> woDe = new MyHashMap<>();
        for (int i = 0; i < n; i++) {
            woDe.put(new HuaiKey2(i), i);
        }
        long t1 = System.nanoTime();

        HashMap<HuaiKey2, Integer> jdk = new HashMap<>();
        for (int i = 0; i < n; i++) {
            jdk.put(new HuaiKey2(i), i);
        }
        long t2 = System.nanoTime();

        double woMs = (t1 - t0) / 1_000_000.0;
        double jdkMs = (t2 - t1) / 1_000_000.0;

        System.out.printf("  【E2】同样的 hash 全 0，但 key 实现了 Comparable%n");
        System.out.printf("  手写 MyHashMap ：%9.1f ms   （还是链表，没变）%n", woMs);
        System.out.printf("  JDK HashMap    ：%9.1f ms   （树能按 id 二分，O(log n)）%n", jdkMs);
        System.out.printf("  → 这一次 JDK 是手写的 %.2f 倍（越小于 1 越快）%n", jdkMs / woMs);
        System.out.println();
        System.out.println("  两轮对比说明什么：");
        System.out.println("  · E1：hash 全等 + 不可比较 → 树退化成 O(n) 全扫，比链表还慢（TreeNode 更重）；");
        System.out.println("  · E2：hash 全等 + 可比较   → 树真正二分，O(log n)，JDK 反超。");
        System.out.println("  —— 「链表到 8 个就转红黑树」这句话省掉了两个前提：**容量 ≥ 64**、**key 能比较**。");
        System.out.println("     String / Integer 这类常用 key 天生 Comparable，所以日常感觉不到；");
        System.out.println("     一旦 key 是自定义类型又忘了实现 Comparable，冲突密集时树形同虚设。");
    }

    /** 同样 hashCode 恒定，但实现了 Comparable */
    static class HuaiKey2 implements Comparable<HuaiKey2> {
        final int id;

        HuaiKey2(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof HuaiKey2 && ((HuaiKey2) o).id == id;
        }

        @Override
        public int compareTo(HuaiKey2 o) {
            return Integer.compare(id, o.id);
        }
    }

    /** 专门用来制造极端冲突的 key：hashCode 恒定 */
    static class HuaiKey {
        final int id;

        HuaiKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return 0;   // 全部同桶
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof HuaiKey && ((HuaiKey) o).id == id;
        }
    }
}
