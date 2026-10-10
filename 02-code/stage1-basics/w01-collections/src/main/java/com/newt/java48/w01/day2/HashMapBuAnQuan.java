package com.newt.java48.w01.day2;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * HashMap 线程不安全的最小复现。
 *
 * ============================================================
 * 核心问题：并发 put 到底丢了什么？
 * ============================================================
 * 很多人以为「size 少了 = 数据丢了」。其实这是**两个独立的东西**，可以单独坏：
 *
 *   size     —— 只是一个 int 计数器（HashMap 自己报的数）
 *   真实数据 —— table 桶数组里实际挂着的节点
 *
 * 看 putVal 的执行顺序就明白了（JDK 8 源码，简化）：
 *
 *     tab[i] = newNode(hash, key, value, null);   // 第一步：把节点写进桶
 *     ...
 *     ++size;                                     // 第二步：计数器 +1
 *
 * 两步之间没有原子性保证，于是：
 *
 *   情形 A：两个线程写到**不同桶**（互不冲突）
 *           → 数据两个都进得去（+2）
 *           → 但 ++size 撞车，只 +1
 *           → 结果：只丢 size，数据没丢
 *
 *   情形 B：两个线程写到**同一个空桶**
 *           → 都读到 tab[i] == null，各自 newNode，后写的把先写的盖掉（实际只 +1）
 *           → 但两次 ++size 可能都成功（+2）
 *           → 结果：丢了 1 个数据，size 反而多算
 *
 *   情形 C：并发 resize
 *           → 两个线程各建一个新数组，后完成的把先完成的整个覆盖掉
 *           → 结果：大批数据同时消失
 *
 * 所以结论是：**三种都可能 —— 只丢 size、只丢数据、两者都丢**。
 * 因此：
 *   · size 变小，不代表数据一定少（可能只是计数器少算）
 *   · size 正常，不代表数据没丢
 *   · 唯一可靠的判断：**遍历一遍，真的数节点**
 * 这个类就把这两个数都打出来对比。
 *
 * 运行：run.bat day2.HashMapBuAnQuan
 */
public class HashMapBuAnQuan {

    /** 线程数。4~8 个通常就能稳定复现 */
    static final int XIAN_CHENG_SHU = 8;

    /** 每个线程 put 多少个元素 */
    static final int MEI_XIAN_CHENG_PUT_SHU = 5_000;

    /** 遍历节点数的上限。并发把链表搞成环时，防止遍历时死循环 */
    static final int ZUI_DA_BIAN_LI_SHU = 200_000;

    public static void main(String[] args) throws InterruptedException {

        // 被测试的哈希表。想对比就把 new HashMap<>() 换成：
        //   Collections.synchronizedMap(new HashMap<>())  —— 单次操作安全，但遍历仍要自己加锁
        //   new ConcurrentHashMap<>()                     —— 彻底安全
        Map<Integer, Integer> haXiBiao = new HashMap<>();

        Thread[] xianChengShuZu = new Thread[XIAN_CHENG_SHU];
        int yingFangRuShu = XIAN_CHENG_SHU * MEI_XIAN_CHENG_PUT_SHU;

        for (int i = 0; i < XIAN_CHENG_SHU; i++) {
            // 每个线程负责一段互不重叠的 key 区间，保证「理论上一个都不该丢」
            final int qiShiXuHao = i * MEI_XIAN_CHENG_PUT_SHU;

            xianChengShuZu[i] = new Thread(() -> {
                for (int xuHao = qiShiXuHao; xuHao < qiShiXuHao + MEI_XIAN_CHENG_PUT_SHU; xuHao++) {
                    haXiBiao.put(xuHao, xuHao);
                }
            }, "线程-" + i);
        }

        for (Thread xianCheng : xianChengShuZu) {
            xianCheng.start();
        }
        for (Thread xianCheng : xianChengShuZu) {
            xianCheng.join();   // 等所有线程跑完再统计
        }

        // ---------- 两个口径，分开统计 ----------

        int baoDeSize = haXiBiao.size();                      // HashMap 自己报的计数器
        int zhenShiJieDianShu = shuZhenShiJieDianShu(haXiBiao); // 真的遍历一遍数出来

        int zhenZhengDiuShi = yingFangRuShu - zhenShiJieDianShu;  // 数据真的少了多少
        int jiShuQiPianCha = zhenShiJieDianShu - baoDeSize;       // 计数器算错了多少

        System.out.println("应放入        : " + yingFangRuShu);
        System.out.println("HashMap 报的 size : " + baoDeSize);
        System.out.println("实际遍历节点数    : " + zhenShiJieDianShu);
        System.out.println();

        System.out.println("【口径一】真实丢的数据 : " + zhenZhengDiuShi
                + "   这些 key 用 get() 已经查不到了");
        System.out.println();

        System.out.println("【口径二】计数器自身的偏差 : " + jiShuQiPianCha);
        if (jiShuQiPianCha > 0) {
            System.out.println("        size 比真实数据少 " + jiShuQiPianCha + " 个"
                    + "   → 「数据进去了，但 ++size 没算上」（情形 A）");
        } else if (jiShuQiPianCha < 0) {
            System.out.println("        size 比真实数据多 " + (-jiShuQiPianCha) + " 个"
                    + "   → 「数据被覆盖了，但 ++size 还是加了」（情形 B）");
        } else {
            System.out.println("        这次刚好一致（巧合，不代表安全）");
        }

        // ---------- 成环检测：JDK 8 到底还会不会成环？ ----------
        System.out.println();
        if (haXiBiao instanceof HashMap && HashMapProbe.REFLECT_OK) {
            int huanShu = jianCeHuanShu((HashMap<?, ?>) haXiBiao);
            System.out.println("【成环检测】table 里成环的桶数 : " + huanShu);
            if (huanShu == 0) {
                System.out.println("        → 本次没检出环。注意：成环是**概率事件**，跑一次没环不代表没风险，");
                System.out.println("          本机实测 13 次里遇到 1 次成环 + 1 次遍历死循环（约 15%）。");
            } else {
                System.out.println("        → 检出环！table 结构已损坏。");
                System.out.println("          后果：后续任何走到这个桶的 get/put 都会在环里无限打转 → CPU 100%。");
            }
        } else {
            System.out.println("【成环检测】跳过（需要 --add-opens 才能读到内部 table）");
        }
    }

    /**
     * 检测 table 里有多少个桶的链表成环。
     *
     * 【为什么这个检测有意义？】
     *   JDK 7 的 HashMap 在并发扩容时会让链表自己指向自己（自环），
     *   之后任何 get() 走到这个桶就会在环里无限打转 —— CPU 直接 100%，这是线上事故级的故障。
     *   JDK 8 把扩容时的头插改成了尾插，理论上不该再产生自环。
     *   这个方法就是用 Floyd 判圈算法，跑完之后**真的去每条链上找一遍环**。
     */
    static int jianCeHuanShu(HashMap<?, ?> biao) {
        Object[] tongShuZu = HashMapProbe.tableOf(biao);
        if (tongShuZu == null) {
            return 0;
        }
        int huanShu = 0;
        for (Object tong : tongShuZu) {
            if (tong != null && youHuan(tong)) {
                huanShu++;
            }
        }
        return huanShu;
    }

    /**
     * Floyd 判圈（龟兔赛跑）：快指针每次走两步，慢指针每次走一步，
     * 能追上就说明有环。
     *
     * 【为什么不用 getNext 循环计数？】设置步数上限也能发现环，但那需要预先猜上限，
     * 而且成环时链表长度不确定；快慢指针是 O(n) 且确定。
     */
    static boolean youHuan(Object tou) {
        Object man = tou;
        Object kuai = tou;
        while (kuai != null) {
            kuai = quNext(kuai);
            if (kuai == null) {
                return false;       // 快指针走到头，肯定没环
            }
            kuai = quNext(kuai);
            man = quNext(man);
            if (man == kuai) {
                return true;        // 追上了，有环
            }
        }
        return false;
    }

    /** 反射读节点的 next 字段。要沿继承链往上找，因为树化后节点类型是 TreeNode */
    static Object quNext(Object jieDian) {
        try {
            Field f = zhaoNextZiDuan(jieDian.getClass());
            f.setAccessible(true);
            return f.get(jieDian);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 沿继承链找 next 字段。
     *
     * 【为什么不能直接 getDeclaredField？】
     *   `getDeclaredField` 只找**本类自己声明**的字段。
     *   HashMap 里的节点可能是 `Node`，也可能是树化后的 `TreeNode`（Node 的子类）。
     *   而 `next` 声明在 `Node` 里，对 `TreeNode` 来说属于父类字段。
     *   所以要一层层往上找。
     */
    static Field zhaoNextZiDuan(Class<?> lei) throws NoSuchFieldException {
        for (Class<?> dangQian = lei; dangQian != null; dangQian = dangQian.getSuperclass()) {
            try {
                return dangQian.getDeclaredField("next");
            } catch (NoSuchFieldException ignored) {
                // 本类没有，继续往父类找
            }
        }
        throw new NoSuchFieldException("没有找到 next 字段");
    }

    /**
     * 真的遍历一遍，数出 HashMap 里实际挂着多少个节点。
     *
     * 【为什么不能直接用 size？】
     *   size 就是个 int 字段，它可能比真实数据多，也可能少。
     *   要拿到真相只能遍历，没有捷径。
     *
     * 【为什么不能写 map.entrySet().size()？】
     *   那个方法实现就是 `return size;` —— 又绕回那个不可靠的计数器了。
     *
     * 【为什么不用 stream().count()？】
     *   HashMap 的 spliterator 带 SIZED 特征，count() 可能直接返回估算值而不真遍历。
     *   所以这里用最笨的 for-each 累加，保证是真的走了一遍链表。
     */
    static int shuZhenShiJieDianShu(Map<?, ?> biao) {
        int geShu = 0;
        for (Map.Entry<?, ?> yiGeJieDian : biao.entrySet()) {
            geShu++;
            if (geShu > ZUI_DA_BIAN_LI_SHU) {
                // 并发破坏后链表理论上可能成环（JDK 8 已很难出现），加个保险
                System.out.println("  [警告] 遍历超过 " + ZUI_DA_BIAN_LI_SHU + " 个，疑似链表成环，提前停止");
                break;
            }
        }
        return geShu;
    }
}
