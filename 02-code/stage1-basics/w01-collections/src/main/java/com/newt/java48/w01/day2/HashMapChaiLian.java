package com.newt.java48.w01.day2;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * 扩容时，桶里的链表到底怎么分？
 *
 * ============================================================
 * 一句话答案
 * ============================================================
 * 看 `hash & oldCap` 这一位：
 *   是 0 → 留在原下标（lo 链，桶 0 还是桶 0）
 *   是 1 → 移到 原下标 + oldCap（hi 链，桶 0 → 桶 16）
 *
 * 所以「桶 0 里的元素扩容后去哪」**不是固定的**，
 * 要看每个元素自己的 hash 在那一位上是 0 还是 1。
 *
 * ============================================================
 * 但有个重要前提：得先「hash 不同」才谈得上拆
 * ============================================================
 * 如果一批 key 的 hash **完全相同**（比如自己写的 hashCode 恒返回同一个值），
 * 那 `hash & oldCap` 对它们来说结果必然一样 —— 要么全留、要么全走，**根本不会拆**。
 *
 * 而且还有个更极端的：hashCode 恒为 0 时，扰动后 hash 还是 0，
 * 高位全是 0 → 新增的位永远是 0 → **不管扩到多大，它永远落桶 0**。
 *
 * ============================================================
 * 两个实验
 * ============================================================
 *   实验 A：hash 低 4 位相同、第 5 位不同 → 扩容后**拆成两条链**
 *   实验 B：hash 完全相同（全为 0）  → 扩容后**还是挤在同一个桶**
 *
 * 运行：run.bat day2.HashMapChaiLian
 */
public class HashMapChaiLian {

    /**
     * 可以指定 hashCode 的 key，用来精确控制它落哪个桶。
     * 注意 hashCode 都故意选得很小（< 65536），这样扰动 `h ^ (h >>> 16)` 之后值不变，
     * 便于手算和对照。
     */
    static class KeZhiKey {
        final int haXiZhi;
        final String mingZi;

        KeZhiKey(int haXiZhi, String mingZi) {
            this.haXiZhi = haXiZhi;
            this.mingZi = mingZi;
        }

        @Override
        public int hashCode() {
            return haXiZhi;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof KeZhiKey && ((KeZhiKey) o).haXiZhi == haXiZhi;
        }

        @Override
        public String toString() {
            return mingZi + "(hash=0x" + Integer.toHexString(haXiZhi) + ")";
        }
    }

    /** hashCode 恒为 0 的 key —— 所有实例永远挤在同一个桶 */
    static class HengLingKey {
        final int xuHao;

        HengLingKey(int xuHao) {
            this.xuHao = xuHao;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof HengLingKey && ((HengLingKey) o).xuHao == xuHao;
        }
    }

    // ---------- 反射小工具 ----------

    /** 沿继承链找字段（兼容 Node 和 TreeNode） */
    static Object duZiDuan(Object duiXiang, String ziDuanMing) {
        try {
            for (Class<?> lei = duiXiang.getClass(); lei != null; lei = lei.getSuperclass()) {
                try {
                    Field f = lei.getDeclaredField(ziDuanMing);
                    f.setAccessible(true);
                    return f.get(duiXiang);
                } catch (NoSuchFieldException ignored) {
                    // 本类没有，往父类找
                }
            }
            throw new NoSuchFieldException(ziDuanMing);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /** 真的去 table 里翻，返回这个 key 实际挂在哪一号桶；找不到返回 -1 */
    static int zhaoSuoZaiTong(HashMap<?, ?> biao, Object muBiao) {
        Object[] tongShuZu = HashMapProbe.tableOf(biao);
        if (tongShuZu == null) {
            return -1;
        }
        for (int i = 0; i < tongShuZu.length; i++) {
            Object jieDian = tongShuZu[i];
            while (jieDian != null) {
                if (duZiDuan(jieDian, "key") == muBiao) {
                    return i;
                }
                jieDian = duZiDuan(jieDian, "next");
            }
        }
        return -1;
    }

    static int rongLiang(HashMap<?, ?> biao) {
        Object[] t = HashMapProbe.tableOf(biao);
        return (t == null) ? 0 : t.length;
    }

    // ---------- 实验 A ----------

    static void shiYanA() {
        System.out.println("========== 实验 A：hash 低 4 位相同、第 5 位不同 → 会拆 ==========");
        System.out.println("4 个 key 的 hash 选成 0x00 / 0x10 / 0x20 / 0x30（低 4 位全是 0）");
        System.out.println();

        HashMap<Object, Integer> biao = new HashMap<>();
        KeZhiKey k0 = new KeZhiKey(0x00, "k0");
        KeZhiKey k1 = new KeZhiKey(0x10, "k1");
        KeZhiKey k2 = new KeZhiKey(0x20, "k2");
        KeZhiKey k3 = new KeZhiKey(0x30, "k3");

        biao.put(k0, 0);
        biao.put(k1, 1);
        biao.put(k2, 2);
        biao.put(k3, 3);

        System.out.println("-- 容量 " + rongLiang(biao) + "（n-1 = 0b1111，只看低 4 位）--");
        for (KeZhiKey k : new KeZhiKey[]{k0, k1, k2, k3}) {
            System.out.println("   " + k + "  →  桶 " + zhaoSuoZaiTong(biao, k));
        }
        System.out.println("   4 个 key 全挤在桶 0（因为低 4 位都是 0）");
        System.out.println();

        // 再塞 9 个普通 key，凑到 13 个 → 触发 size 扩容
        // 用 1~9 做 key（hash 就是自身，落桶 1~9），避开桶 0，免得干扰观察
        for (int i = 1; i <= 9; i++) {
            biao.put(i, i);
        }
        System.out.println("-- 再塞 9 个普通 key，size = " + biao.size() + " → 触发扩容 --");
        System.out.println("  扩容后容量 = " + rongLiang(biao) + "（n-1 = 0b11111，第 5 位开始参与）");
        System.out.println();

        int[] qiWang = new int[]{0, 16, 0, 16};
        int geShu = 0;
        for (KeZhiKey k : new KeZhiKey[]{k0, k1, k2, k3}) {
            int tong = zhaoSuoZaiTong(biao, k);
            int naYiWei = k.haXiZhi & 0x10;
            String panDuan = (naYiWei == 0) ? "hash & 16 == 0  → 留" : "hash & 16 == 16 → 走";
            String duiCuo = (tong == qiWang[geShu++]) ? "" : "   ← 和预期不符!";
            System.out.printf("   %-18s → 桶 %-3d （%s）%s%n", k.toString(), tong, panDuan, duiCuo);
        }
        System.out.println();
        System.out.println("   桶 0 被拆成了两条链：k0 / k2 留在桶 0，k1 / k3 搬到桶 16。");
        System.out.println("   搬家的规则就是把原来那条链按「第 5 位是 0 还是 1」劈成两半。");
        System.out.println();
    }

    // ---------- 实验 B ----------

    static void shiYanB() {
        System.out.println("========== 实验 B：hash 完全相同 → 不会拆 ==========");
        System.out.println("用 hashCode 恒为 0 的 key，塞 11 个：");
        System.out.println();

        HashMap<HengLingKey, Integer> biao = new HashMap<>();

        for (int i = 1; i <= 11; i++) {
            int qian = rongLiang(biao);
            HengLingKey zheGeKey = new HengLingKey(i);
            biao.put(zheGeKey, i);
            int hou = rongLiang(biao);

            String beiZhu = "";
            if (hou != qian && qian > 0) {
                beiZhu = "   ← 扩容 " + qian + " → " + hou;
            }
            System.out.printf("   第 %-3d 个：容量 %-4d  size %-3d  它落在桶 %d%s%n",
                    i, hou, biao.size(), zhaoSuoZaiTong(biao, zheGeKey), beiZhu);
        }
        System.out.println();
        System.out.println("   注意：容量一路 16 → 32 → 64 在涨，但元素**始终在桶 0**，从没拆开过。");
        System.out.println();
        System.out.println("   原因：扩容不改变元素的 hash，只是让**更多位**参与下标计算。");
        System.out.println("        hashCode 恒为 0 → 扰动后 hash 还是 0 → 高位全是 0 →");
        System.out.println("        新增的位永远是 0 → (n-1) & 0 永远是 0 → 永远落桶 0。");
        System.out.println();
    }

    public static void main(String[] args) {
        shiYanA();
        shiYanB();
    }
}
