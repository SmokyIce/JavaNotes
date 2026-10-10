package com.newt.java48.w01.day2;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * 扩容到底什么时候触发？实测两个触发条件。
 *
 * ============================================================
 * 触发条件一：size 超过阈值（大家都会答的）
 * ============================================================
 *   if (++size > threshold)
 *       resize();
 *
 *   threshold = capacity * loadFactor(0.75)
 *
 *   容量 16 → 阈值 12 → 放入第 13 个元素时扩容到 32
 *   容量 32 → 阈值 24 → 放入第 25 个元素时扩容到 64
 *   容量 64 → 阈值 48 → 放入第 49 个元素时扩容到 128
 *
 *   注意两点：
 *     · 是「严格大于」，所以是第 13 个触发，不是第 12 个
 *     · 检查发生在元素**已经放进去之后**（put 的末尾），属于「先超标，后补救」
 *
 * ============================================================
 * 触发条件二：桶里链表太长（大多数人不知道的）
 * ============================================================
 *   直觉是「链表到 8 个就变红黑树」。但这句话**不完整**。
 *   看 JDK 源码 treeifyBin() 的第一行：
 *
 *       if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
 *           resize();          // ← 容量还没到 64？先扩容，别急着树化
 *       else
 *           ...树化...
 *
 *   MIN_TREEIFY_CAPACITY = 64。也就是说：
 *     · 容量 < 64 时，链表变长**不树化，而是扩容**（把桶拆细，冲突自然就散了）
 *     · 容量 ≥ 64 时，才真正把链表转成红黑树
 *
 *   这条规则很聪明：桶少的时候，冲突多是因为「桶不够」，扩容比树化更划算。
 *
 * ============================================================
 * 这个实验怎么做的
 * ============================================================
 *   用一个 hashCode 恒为 0 的 key，让所有元素都挤进同一个桶。
 *   于是「桶内链表长度」就等于 size，两个触发条件会先后出现：
 *
 *     第 9 个  → 链表变到 9 个，容量 16 < 64 → 扩容 16 → 32（链表触发）
 *     第 10 个 → 链表继续变长，容量 32 < 64 → 扩容 32 → 64（链表触发）
 *     第 11 个 → 容量已到 64，不再扩容，转为树化
 *     第 49 个 → size 49 > 阈值 48 → 扩容 64 → 128（size 触发）
 *
 * 运行：run.bat day2.HashMapKuoRong
 */
public class HashMapKuoRong {

    /**
     * 一个 hashCode 恒为 0 的 key。
     * 所有实例的 hash 都相同 → 全部落进 0 号桶 → 制造极端冲突。
     */
    static class ChongTuKey {
        final int xuHao;

        ChongTuKey(int xuHao) {
            this.xuHao = xuHao;
        }

        /** 故意全部返回 0 —— 这是实验的关键 */
        @Override
        public int hashCode() {
            return 0;
        }

        /** hashCode 相同不算同一个 key，还要靠 equals 区分 */
        @Override
        public boolean equals(Object o) {
            return o instanceof ChongTuKey && ((ChongTuKey) o).xuHao == xuHao;
        }

        @Override
        public String toString() {
            return "k" + xuHao;
        }
    }

    /** 读当前容量（桶的个数）。读不到返回 -1 */
    static int rongLiang(HashMap<?, ?> biao) {
        if (!HashMapProbe.REFLECT_OK) {
            return -1;
        }
        Object[] tongShuZu = HashMapProbe.tableOf(biao);
        return (tongShuZu == null) ? 0 : tongShuZu.length;
    }

    static int yuZhi(HashMap<?, ?> biao) {
        return HashMapProbe.REFLECT_OK ? HashMapProbe.thresholdOf(biao) : -1;
    }

    public static void main(String[] args) {
        System.out.println("=== 扩容触发条件实测 ===");
        System.out.println("key 的 hashCode 恒为 0 → 所有元素都挤进同一个桶（桶内链表长度 == size）");
        System.out.println();

        HashMap<ChongTuKey, Integer> biao = new HashMap<>();

        System.out.printf("%-7s %-7s %-11s %-6s %s%n",
                "第几个", "容量", "threshold", "size", "备注");
        System.out.println("--------------------------------------------------------------------------");

        for (int i = 1; i <= 50; i++) {
            int rongLiangQian = rongLiang(biao);
            int yuZhiQian = yuZhi(biao);

            biao.put(new ChongTuKey(i), i);

            int rongLiangHou = rongLiang(biao);
            int sizeHou = biao.size();

            String beiZhu = "";
            if (rongLiangHou != rongLiangQian && rongLiangQian > 0) {
                // 判断这次是被谁触发的
                if (sizeHou > yuZhiQian) {
                    beiZhu = "← 扩容 " + rongLiangQian + " → " + rongLiangHou
                            + " ｜ size " + sizeHou + " > 阈值 " + yuZhiQian + "，size 触发";
                } else {
                    beiZhu = "← 扩容 " + rongLiangQian + " → " + rongLiangHou
                            + " ｜ 桶内链表太长，容量 < 64，链表触发（不树化）";
                }
            }

            // 只打印有意义的行：前 12 个 + 后 3 个 + 所有扩容点
            boolean yaoDaYin = (i <= 12) || (i >= 48) || !beiZhu.isEmpty();
            if (yaoDaYin) {
                System.out.printf("%-7d %-7d %-11d %-6d %s%n",
                        i, rongLiangHou, yuZhi(biao), sizeHou, beiZhu);
            } else if (i == 13) {
                System.out.println("  ...  （第 13 ~ 47 个：容量 64、阈值 48，既不扩容也不树化，安静）");
            }
        }

        System.out.println();
        System.out.println("两个触发条件的区别：");
        System.out.println("  · size 触发：元素总数超了容量承载能力 → 整表翻倍（16→32→64→128…）");
        System.out.println("  · 链表触发：个别桶太挤，但容量还不到 64 → 也翻倍（可避免过早树化）");
        System.out.println("  两条路走的都是同一个 resize()，所以容量永远是 2 的幂。");

        System.out.println();
        shiYanC();
    }

    /** 反射读 HashMap 的静态常量 */
    static int duChangLiang(String ming) {
        try {
            Field f = HashMap.class.getDeclaredField(ming);
            f.setAccessible(true);
            return f.getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- 实验 C：容量的天花板 ----------

    static void shiYanC() {
        System.out.println("========== 实验 C：容量的天花板在哪 ==========");
        System.out.println("  HashMap.MAXIMUM_CAPACITY = " + duChangLiang("MAXIMUM_CAPACITY")
                + "   （就是 1 << 30 = " + (1 << 30) + "，约 10.7 亿）");
        System.out.println();

        // 构造时 threshold 里存的就是「向上取整到 2 的幂」之后的容量，且不会真的分配数组（懒加载）
        int[] shiRuDe = {100, 1000, 1 << 20, (1 << 30) - 1, 1 << 30, Integer.MAX_VALUE};
        System.out.println("  传入的初始容量     →  实际容量（threshold 里存的值）");
        for (int shiRu : shiRuDe) {
            HashMap<Object, Object> biao = new HashMap<>(shiRu);
            System.out.printf("  %-18d →  %d%n", shiRu, yuZhi(biao));
        }
        System.out.println();

        System.out.println("  为什么不是 2^32？也不是 2^31 - 1？");
        System.out.println("    · 容量必须是 2 的幂，候选只有 2^30 / 2^31 / 2^32 …");
        System.out.println("    · 桶数组的长度是 int，而 1 << 31 = Integer.MIN_VALUE（负数），当不了长度");
        System.out.println("    · 所以 2 的幂里最大的安全值就是 1 << 30 = 1073741824");
        System.out.println("    · 到顶之后 resize() 会把 threshold 设成 Integer.MAX_VALUE，从此不再扩容");
        System.out.println();
        System.out.println("  还有个现实约束：2^30 个引用 ≈ 4 GB（压缩指针）～ 8 GB，普通机器分配不出来。");
        System.out.println();
        System.out.println("  顺带一个细节：容量到 2^30 时，n-1 = 0b0111...1111（30 个 1），");
        System.out.println("                只用到 hash 的低 30 位 —— hash 的最高 2 位永远闲置。");
    }
}
