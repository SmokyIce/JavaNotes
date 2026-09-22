package com.newt.java48.w01.day1;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Day 1 Lab — ArrayList vs LinkedList：用数字说话，不靠背结论。
 *
 * 运行方式见 ../../../README.md
 *
 * 提前把结论写在这里，跑完你会亲眼看到：
 *   1. get(i) 随机访问：ArrayList 快几百倍 —— 数组是一次算地址，链表是一个个节点跳。
 *   2. for-i 按下标遍历链表：慢上千倍 —— 这才是 LinkedList 真正的"性能事故"来源。
 *   3. add(E) 尾部追加：ArrayList 通常不输甚至更快 —— 因为链表每次 add 都要 new Node，
 *      而且节点散落在堆里，CPU 缓存基本命中不了（指针追逐 pointer chasing）。
 *   4. add(0, E) 头部插入：这一项 LinkedList 才真正赢。这是它少数不可替代的场景。
 *   5. 内存：装同样 100 万个元素，LinkedList 要多花一大截，全花在 Node 对象头 + 两个指针上。
 *
 * 结论：生产代码里默认用 ArrayList；需要双端队列用 ArrayDeque；
 *        LinkedList 只在你"已经持有节点位置"且"确实要 O(1) 改结构"时才考虑。
 */
public class ArrayListVsLinkedList {

    /** 顺序类操作的规模 */
    static final int N_SEQ = 1_000_000;
    /** 头部 / 中间插入删除的规模（O(n^2)，故意开小，否则要跑几分钟） */
    static final int N_SHIFT = 20_000;
    /** 链表按下标 get 的规模（O(n^2)） */
    static final int N_GET = 20_000;

    /** 用来吃掉计算结果，防止 JIT 把整个循环当成 dead code 优化掉 */
    static volatile long sink = 0;

    public static void main(String[] args) {
        warmUp();

        System.out.println("=== Day 1 Lab : ArrayList vs LinkedList ===");
        System.out.println("java.version = " + System.getProperty("java.version")
                + "   maxHeap = " + (Runtime.getRuntime().maxMemory() >> 20) + " MB");
        System.out.println("N_SEQ = " + N_SEQ + "   N_SHIFT = " + N_SHIFT + "   N_GET = " + N_GET);
        System.out.println();
        System.out.printf("%-28s %14s %12s %12s %9s%n",
                "operation", "scale (AL/LL)", "ArrayList", "LinkedList", "LL/AL");
        System.out.printf("%-28s %14s %12s %12s %9s%n", "", "", "ns/op", "ns/op", "x");
        line();
        List<Integer> a1 = new ArrayList<>();
        List<Integer> l1 = new LinkedList<>();
        bench("add(E)   append", scale(N_SEQ, N_SEQ),
                () -> append(a1, N_SEQ), N_SEQ,
                () -> append(l1, N_SEQ), N_SEQ);

        List<Integer> a2 = new ArrayList<>(withSeq(N_SEQ));
        List<Integer> l2 = new LinkedList<>(withSeq(N_GET));
        bench("get(i)   random access", scale(N_SEQ, N_GET),
                () -> randomGet(a2, N_SEQ), N_SEQ,
                () -> randomGet(l2, N_GET), N_GET);

        List<Integer> a3 = new ArrayList<>(withSeq(N_SEQ));
        List<Integer> l3 = new LinkedList<>(withSeq(N_GET));
        bench("for-i    indexed walk", scale(N_SEQ, N_GET),
                () -> indexedWalk(a3, N_SEQ), N_SEQ,
                () -> indexedWalk(l3, N_GET), N_GET);

        List<Integer> a4 = new ArrayList<>(withSeq(N_SEQ));
        List<Integer> l4 = new LinkedList<>(withSeq(N_SEQ));
        bench("iterator walk + sum", scale(N_SEQ, N_SEQ),
                () -> iterate(a4, N_SEQ), N_SEQ,
                () -> iterate(l4, N_SEQ), N_SEQ);

        List<Integer> a5 = new ArrayList<>();
        List<Integer> l5 = new LinkedList<>();
        bench("add(0,E) insert head", scale(N_SHIFT, N_SHIFT),
                () -> insertHead(a5, N_SHIFT), N_SHIFT,
                () -> insertHead(l5, N_SHIFT), N_SHIFT);

        List<Integer> a6 = new ArrayList<>();
        List<Integer> l6 = new LinkedList<>();
        bench("add(mid,E) insert middle", scale(N_SHIFT, N_SHIFT),
                () -> insertMiddle(a6, N_SHIFT), N_SHIFT,
                () -> insertMiddle(l6, N_SHIFT), N_SHIFT);

        List<Integer> a7 = new ArrayList<>(withSeq(N_SHIFT));
        List<Integer> l7 = new LinkedList<>(withSeq(N_SHIFT));
        bench("remove(0) remove head", scale(N_SHIFT, N_SHIFT),
                () -> removeHead(a7, N_SHIFT), N_SHIFT,
                () -> removeHead(l7, N_SHIFT), N_SHIFT);

        System.out.println();
        memoryFootprint();
        System.out.println();
        System.out.println("读一遍上面的数字，然后回答：为什么 LinkedList 连 append 都赢不了？");
        System.out.println("hint: new Node() 的成本 + 内存不连续 + CPU cache line (64B) 的命中率");
    }

    // ================= 被测操作 =================

    static void append(List<Integer> list, int n) {
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
    }

    static void randomGet(List<Integer> list, int n) {
        long s = 0;
        for (int i = 0; i < n; i++) {
            int idx = (int) (((i * 2654435761L) >>> 3) % n);
            s += list.get(idx);
        }
        sink = s;
    }

    static void indexedWalk(List<Integer> list, int n) {
        long s = 0;
        for (int i = 0; i < n; i++) {
            s += list.get(i);
        }
        sink = s;
    }

    static void iterate(List<Integer> list, int n) {
        long s = 0;
        for (Integer v : list) {
            s += v;
        }
        sink = s;
    }

    static void insertHead(List<Integer> list, int n) {
        for (int i = 0; i < n; i++) {
            list.add(0, i);
        }
    }

    static void insertMiddle(List<Integer> list, int n) {
        for (int i = 0; i < n; i++) {
            list.add(list.size() >> 1, i);
        }
    }

    static void removeHead(List<Integer> list, int n) {
        for (int i = 0; i < n; i++) {
            list.remove(0);
        }
    }

    // ================= 工具 =================

    static void bench(String name, String scale, Runnable a, long opsA, Runnable b, long opsB) {
        long ta = time(a);
        long tb = time(b);
        double nsa = (double) ta / opsA;
        double nsb = (double) tb / opsB;
        System.out.printf("%-28s %14s %12.2f %12.2f %8.2fx%n", name, scale, nsa, nsb, nsb / nsa);
    }

    static long time(Runnable task) {
        long t0 = System.nanoTime();
        task.run();
        return System.nanoTime() - t0;
    }

    static void warmUp() {
        List<Integer> a = new ArrayList<>();
        List<Integer> l = new LinkedList<>();
        for (int r = 0; r < 3; r++) {
            append(a, 20_000);
            append(l, 20_000);
            randomGet(a, 20_000);
            randomGet(l, 2_000);
            indexedWalk(a, 20_000);
            iterate(l, 20_000);
            insertHead(a, 2_000);
            insertHead(l, 2_000);
            removeHead(a, 2_000);
            removeHead(l, 2_000);
            a.clear();
            l.clear();
        }
    }

    static List<Integer> withSeq(int n) {
        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        return list;
    }

    static String scale(int a, int b) {
        return n(a) + " / " + n(b);
    }

    static String n(int v) {
        if (v >= 1_000_000) {
            return (v / 1_000_000) + "M";
        }
        if (v >= 1_000) {
            return (v / 1_000) + "k";
        }
        return String.valueOf(v);
    }

    static void line() {
        for (int i = 0; i < 80; i++) {
            System.out.print('-');
        }
        System.out.println();
    }

    // ================= 内存占用 =================

    /**
     * 用同一批 Integer 对象喂给两个 List，这样测出来的差值就纯粹是
     * "数组槽位" 和 "Node 对象" 的差别，不掺杂装箱开销。
     */
    static void memoryFootprint() {
        int n = 1_000_000;
        Integer[] boxed = new Integer[n];
        for (int i = 0; i < n; i++) {
            boxed[i] = i;
        }

        gc();
        long base1 = used();
        List<Integer> al = new ArrayList<>(n);
        for (Integer v : boxed) {
            al.add(v);
        }
        gc();
        long alExtra = used() - base1;

        gc();
        long base2 = used();
        List<Integer> ll = new LinkedList<>();
        for (Integer v : boxed) {
            ll.add(v);
        }
        gc();
        long llExtra = used() - base2;

        System.out.println("--- memory footprint (1,000,000 same Integer objects, approx) ---");
        System.out.printf("ArrayList   extra heap: %7.2f MB   (1 x Object[] with 1M slots)%n",
                alExtra / 1024.0 / 1024.0);
        System.out.printf("LinkedList  extra heap: %7.2f MB   (1M x Node objects, each {prev,item,next})%n",
                llExtra / 1024.0 / 1024.0);
        System.out.printf("LinkedList costs about %.1fx more memory%n",
                llExtra == 0 ? 0.0 : (double) llExtra / Math.max(alExtra, 1));
        System.out.println("(keep alive to avoid dead-code elimination: " + (al.size() + ll.size()) + ")");
    }

    static long used() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    static void gc() {
        for (int i = 0; i < 2; i++) {
            System.gc();
            try {
                Thread.sleep(120);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
