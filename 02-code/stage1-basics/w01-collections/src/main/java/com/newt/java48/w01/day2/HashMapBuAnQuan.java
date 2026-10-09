package com.newt.java48.w01.day2;

import java.util.HashMap;
import java.util.Map;

/**
 * HashMap 线程不安全的最小复现。
 *
 * 【现象】
 *   多个线程各自往同一个 HashMap 里 put 互不重叠的 key，
 *   全部 put 完之后，size 应该等于 put 总次数 —— 实际总会**少一些**。
 *
 * 【原因】HashMap.put 里至少有三处竞态：
 *   ① ++size     —— 读-改-写三步，不是原子操作，两个线程同时写会丢掉一次增量
 *   ② resize     —— 两个线程同时扩容，各自建新数组，后完成的把先完成的覆盖掉
 *   ③ 同桶覆盖   —— 两个线程同时 put 到同一个空桶，都读到 null，
 *                   各建一个节点，一个把另一个盖掉
 *
 * 【对照】把 HashMap 换成 ConcurrentHashMap，丢掉的个数就是 0（可自行验证）。
 *
 * 运行：run.bat day2.HashMapBuAnQuan
 */
public class HashMapBuAnQuan {

    /** 线程数。4~8 个通常就能稳定复现 */
    static final int XIAN_CHENG_SHU = 8;

    /** 每个线程 put 多少个元素 */
    static final int MEI_XIAN_CHENG_PUT_SHU = 5_000;

    public static void main(String[] args) throws InterruptedException {

        // 被测试的哈希表。想对比就把 new HashMap<>() 换成：
        //   Collections.synchronizedMap(new HashMap<>())  —— 单次操作安全，但遍历仍要自己加锁
        //   new ConcurrentHashMap<>()                     —— 彻底安全
        Map<Integer, Integer> haXiBiao = new HashMap<>();

        Thread[] xianChengShuZu = new Thread[XIAN_CHENG_SHU];
        int putZongShu = XIAN_CHENG_SHU * MEI_XIAN_CHENG_PUT_SHU;

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

        int shiJiGeShu = haXiBiao.size();
        int diuShiGeShu = putZongShu - shiJiGeShu;

        System.out.println("应放入 : " + putZongShu);
        System.out.println("实际有 : " + shiJiGeShu);
        System.out.println("丢掉了 : " + diuShiGeShu
                + (diuShiGeShu > 0 ? "   <-- 这就是线程不安全" : "   这次没丢，重跑一次"));
    }
}
