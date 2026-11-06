package com.newt.java48.w03.day3;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * W03 Day 3 · happens-before 五条现场
 *
 *  A 程序次序规则 —— 只对「单线程内部」负责。用 Dekker litmus 测跨线程能不能看见重排（负结果也是结果）
 *  B monitor 规则  —— 解锁 happens-before 后续加锁。所以 synchronized 不只管互斥，还管可见性
 *  C volatile 规则 —— 读线程看到 volatile 写之后，写线程「写之前的所有写」都可见（发布/订阅）
 *  D 启动/终止规则 —— start() 之前的写对新线程可见；join() 之后的读能看到线程内所有写
 *  E volatile 写的时间代价 —— set() vs lazySet()，差的就是那道 StoreLoad 屏障
 *
 * 运行：./run.sh day3.HappensBefore        （约 20 秒）
 */
public class HappensBefore {

    public static void main(String[] args) throws Exception {
        System.out.println("JDK " + System.getProperty("java.version")
                + " / " + Runtime.getRuntime().availableProcessors() + " 核 / "
                + System.getProperty("os.arch"));
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
        System.out.println("\n全部段落跑完。");
    }

    // ==================================================================
    // A · 程序次序规则：单线程内 as-if-serial 说了算，跨线程谁都不保证
    // ==================================================================

    private static int ax, ay;
    private static int ar1, ar2;
    /** 放行门：主线程改它，两个 worker 自旋等 —— 比 CyclicBarrier 更接近「同时起跑」。 */
    private static volatile int gate;
    private static final AtomicInteger doneCount = new AtomicInteger();

    /**
     * Dekker litmus：
     *   线程 1： ax = 1;  ar1 = ay;
     *   线程 2： ay = 1;  ar2 = ax;
     * 「程序次序」告诉我们每个线程都是先写后读，所以 (ar1, ar2) 至少有一个应该是 1。
     * 出现 (0,0)：两个读都发生在对方写生效之前 —— 说明「先写后读」在内存看来可以被换序。
     */
    private static void sectionA() throws Exception {
        System.out.println("\n========== A · 程序次序规则：先写后读，内存看来能不能反过来 ==========");
        final int ROUNDS = 100_000;

        Thread w1 = new Thread(() -> {
            for (int i = 1; i <= ROUNDS; i++) {
                while (gate != i) { }        // 自旋等放行
                ax = 1;
                ar1 = ay;
                doneCount.incrementAndGet();
            }
        }, "A-writer1");
        Thread w2 = new Thread(() -> {
            for (int i = 1; i <= ROUNDS; i++) {
                while (gate != i) { }
                ay = 1;
                ar2 = ax;
                doneCount.incrementAndGet();
            }
        }, "A-writer2");
        w1.start();
        w2.start();

        int both0 = 0, only1 = 0, only2 = 0, both1 = 0;
        for (int i = 1; i <= ROUNDS; i++) {
            ax = 0; ay = 0; ar1 = -1; ar2 = -1;
            doneCount.set(0);
            gate = i;
            while (doneCount.get() < 2) { }
            if (ar1 == 0 && ar2 == 0) both0++;
            else if (ar1 == 1 && ar2 == 1) both1++;
            else if (ar1 == 1) only1++;
            else only2++;
        }

        System.out.printf("  %d 轮：(r1,r2)=(0,0) → %d 次 / (1,1) → %d 次 / 只有 r1=1 → %d 次 / 只有 r2=1 → %d 次%n",
                ROUNDS, both0, both1, only1, only2);
        System.out.println("  (1,1) 的含义：两个线程都看见了对方的写 —— 这才是「真的重叠执行」的轮次。");
        System.out.println("  (0,0) 的含义：两个读都发生在对方写之前 —— 只有在写进 store buffer、读直接走时才会出现。");
        if (both0 == 0) {
            System.out.println("  → 本机（x86-64 / HotSpot 17）10 万轮一次都没测出 (0,0)。");
            System.out.println("    这不是「JMM 不允许」——JMM 明确允许（x86 只禁止 LoadLoad/LoadStore/StoreStore，");
            System.out.println("    唯独放行 StoreLoad）。测不出来是因为 store buffer 刷得太快、两个线程的窗口对不齐。");
            System.out.println("    这条负结果本身就是结论：**并发 bug 测不出来 ≠ 不存在，判断依据只能是 JMM，不是跑通。**");
        } else {
            System.out.println("  → 测到了 (0,0)：`ar1 = ay` 被提到了 `ax = 1` 之前。程序次序只约束单线程内部。");
        }
        System.out.println("    对照：(1,1) 与 (0,0) 的关系正是「可见性」这个词的全部含义。");
        w1.join(); w2.join();
    }

    // ==================================================================
    // B · monitor 规则：解锁 happens-before 后续对同一把锁的加锁
    // ==================================================================

    private static final Object MONITOR = new Object();
    private static int bData;
    private static boolean bFlag;

    private static void sectionB() throws Exception {
        System.out.println("\n========== B · monitor 规则：synchronized 不只管互斥，还管可见性 ==========");

        // 关键：写线程先睡 200 ms。这 200 ms 让读线程的自旋循环被 JIT 编译完 ——
        // 只有编译过的循环才可能把「读字段」提到循环外。不睡，测的只是解释执行。
        Thread w = new Thread(() -> {
            sleep(200);
            bData = 42;                              // 锁外的普通写
            synchronized (MONITOR) {
                bFlag = true;                        // 锁内写标志
            }
        }, "B-writer");
        w.start();

        long spin = 0;
        boolean saw = false;
        while (spin < 200_000_000L) {
            synchronized (MONITOR) {
                if (bFlag) { saw = true; break; }
            }
            spin++;
        }
        w.join();
        System.out.println("  [B1 有 monitor] 看见标志？" + saw + "，自旋 " + spin
                + " 次，读到的 data = " + bData);
        System.out.println("    → data 不是 volatile，可见性是「写线程解锁 → 读线程加锁」这条 HB 边传过来的。");
        System.out.println("      更硬的证据是它「没被优化掉」：同样的自旋循环放进 synchronized 里，");
        System.out.println("      JIT 不敢把 bFlag 的读提到循环外（monitor 是一道屏障）；见 C1 的对照。");
        System.out.println("    （第一版我在循环体里放了 System.nanoTime() 当超时判据 —— 方法调用挡住了 JIT 提升，");
        System.out.println("      测出 true 还以为是结论。改成纯自旋 + 写线程先睡 80 ms，C1 才复现出失败。）");
    }

    // ==================================================================
    // C · volatile 规则：写之前的「所有」写，对读到它的线程可见
    // ==================================================================

    private static int cD1, cD2;
    private static boolean cReady;
    private static volatile boolean cReadyV;

    private static void sectionC() throws Exception {
        System.out.println("\n========== C · volatile 规则：发布 / 订阅 ==========");
        final int ROUNDS = 5;

        int seenPlain = 0, tornPlain = 0;
        for (int r = 0; r < ROUNDS; r++) {
            cD1 = 0; cD2 = 0; cReady = false;
            Thread w = new Thread(() -> {
                sleep(80);                         // 给读线程的循环留出被 JIT 编译的时间（自旋上限必须远大于它）
                cD1 = 1;
                cD2 = 2;
                cReady = true;                     // 普通写，没有任何屏障
            }, "C-w-plain");
            w.start();
            long spin = 0;
            boolean ok = false;
            while (spin < 1_500_000_000L) {
                if (cReady) { ok = true; break; }
                spin++;
            }
            if (ok) {
                seenPlain++;
                if (cD1 != 1 || cD2 != 2) tornPlain++;
            }
            w.join();
            System.out.printf("    第 %d 轮：看见？%s，自旋 %d 次%n", r + 1, ok, spin);
        }
        System.out.printf("  [C1 普通标志] %d 轮：看见标志 %d 轮，其中「标志到了但数据没到」%d 轮%n",
                ROUNDS, seenPlain, tornPlain);

        int seenVol = 0, tornVol = 0;
        long volSpinSum = 0;
        for (int r = 0; r < ROUNDS; r++) {
            cD1 = 0; cD2 = 0; cReadyV = false;
            Thread w = new Thread(() -> {
                sleep(80);
                cD1 = 1;
                cD2 = 2;
                cReadyV = true;                    // volatile 写：之前的写全部先落地
            }, "C-w-vol");
            w.start();
            long spin = 0;
            boolean ok = false;
            while (spin < 1_500_000_000L) {
                if (cReadyV) { ok = true; break; }
                spin++;
            }
            if (ok) {
                seenVol++;
                volSpinSum += spin;
                if (cD1 != 1 || cD2 != 2) tornVol++;
            }
            w.join();
        }
        System.out.printf("  [C2 volatile 标志] %d 轮：看见标志 %d 轮，其中数据不完整 %d 轮，平均自旋 %d 次%n",
                ROUNDS, seenVol, tornVol, seenVol == 0 ? -1 : volSpinSum / seenVol);
        System.out.println("  → C1 与 C2 只差一个 volatile：前者撞满上限永远看不见，后者 80 ms 后立刻看见。");
        System.out.println("    「看见之后数据是否完整」在 x86 上测不出差异（TSO 不重排 store-store），");
        System.out.println("    但 volatile 的 HB 保证的是两件事一起：标志可见 + 标志之前的写也可见。");
        System.out.println("    这也是 DCL 敢靠 volatile 的理论依据 —— 换 ARM 就会真的出现「标志到了、数据没到」。");
    }

    // ==================================================================
    // D · 线程启动 / 终止（join）规则
    // ==================================================================

    private static int dBeforeStart;
    private static int dResult;

    private static void sectionD() throws Exception {
        System.out.println("\n========== D · 启动与终止规则：start() 与 join() 是两条屏障 ==========");

        dBeforeStart = 0;
        final int[] d1Seen = new int[1];
        dBeforeStart = 777;                        // 在 start() 之前写
        Thread t1 = new Thread(() -> d1Seen[0] = dBeforeStart, "D-start");
        t1.start();
        t1.join();
        System.out.println("  [D1 start 前写] 子线程读到 " + d1Seen[0] + "（期望 777）");

        final int ROUNDS = 500;
        int zero = 0;
        for (int i = 0; i < ROUNDS; i++) {
            dResult = 0;
            Thread t = new Thread(() -> {
                int s = 0;
                for (int k = 0; k < 100; k++) s += k;   // 制造一点延迟
                dResult = s + 1;                        // 4950 + 1
            }, "D-nora");
            t.start();
            if (dResult == 0) zero++;                   // 不等它，直接读
        }
        System.out.printf("  [D2 不 join] %d 轮：读到 0（还没算完）%d 轮，读到结果 %d 轮%n",
                ROUNDS, zero, ROUNDS - zero);

        int okD3 = 0;
        for (int i = 0; i < ROUNDS; i++) {
            dResult = 0;
            Thread t = new Thread(() -> {
                int s = 0;
                for (int k = 0; k < 100; k++) s += k;
                dResult = s + 1;
            }, "D-join");
            t.start();
            t.join();
            if (dResult == 4951) okD3++;
        }
        System.out.printf("  [D3 有 join] %d 轮：拿到完整结果 %d 轮（期望 4951）%n", ROUNDS, okD3);
        System.out.println("  → start() 让「之前的写」对新线程可见；join() 让「线程内的写」对调用者可见。");
        System.out.println("    D2 读到 0 不是「线程没跑」，是「你比它先读」—— 抢跑，不是并发 bug，但一样错。");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================================================================
    // E · volatile 写的时间代价：set() vs lazySet()
    // ==================================================================

    private static int ePlain;

    private static void sectionE() {
        System.out.println("\n========== E · volatile 写到底贵在哪（6 轮取最小值，丢第 1 轮预热） ==========");
        final int N = 20_000_000;
        AtomicInteger ai = new AtomicInteger();

        long minPlain = Long.MAX_VALUE, minSet = Long.MAX_VALUE, minLazy = Long.MAX_VALUE;
        for (int r = 0; r < 7; r++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < N; i++) ePlain = i;
            long t1 = System.nanoTime();
            for (int i = 0; i < N; i++) ai.lazySet(i);      // 有序写：x86 上就是普通 store
            long t2 = System.nanoTime();
            for (int i = 0; i < N; i++) ai.set(i);          // volatile 写：StoreLoad 屏障
            long t3 = System.nanoTime();
            if (r > 0) {
                minPlain = Math.min(minPlain, t1 - t0);
                minLazy = Math.min(minLazy, t2 - t1);
                minSet = Math.min(minSet, t3 - t2);
            }
        }
        System.out.printf("  普通字段写  %7.2f ms（%.3f ns/次）← 被 JIT 消掉了，只留最后一次 store%n",
                minPlain / 1e6, minPlain / (double) N);
        System.out.printf("  lazySet()   %7.2f ms（%.3f ns/次）← 有序写，不加 lock 前缀%n",
                minLazy / 1e6, minLazy / (double) N);
        System.out.printf("  set()       %7.2f ms（%.3f ns/次）← volatile 写%n",
                minSet / 1e6, minSet / (double) N);
        System.out.printf("  → set() 是 lazySet() 的 %.1f 倍。差的不是指令条数（字节码都是 putfield），%n",
                minSet / (double) minLazy);
        System.out.println("    是 x86 上跟在 volatile 写后面那条 `lock addl $0,(%rsp)`（StoreLoad 屏障）。");
        System.out.println("    普通写那行的 0.0x ns 是**测量事故**：JIT 发现中间值没人看，整个循环只剩最后一次写。");
        System.out.println("    「数字接近 0 就是没测到」，这条今天又踩了一次。");
    }
}
