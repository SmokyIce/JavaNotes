package com.newt.java48.w03.day3;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * W03 Day 3 · DCL（双重检查锁）为什么必须加 volatile
 *
 *  D1  不加 volatile 的 DCL，50 轮 × 6 线程压力测试，统计「拿到半初始化对象」的次数
 *  D2  加 volatile 的 DCL，同样压力，统计
 *  D3  把「JIT 允许的重排顺序」手工写出来：先发布引用、后执行构造 —— 看另一个线程拿到什么
 *  D4  用 Unsafe.allocateInstance 直接拿到一个「没走构造」的对象，看它的字段是默认值
 *
 * 运行：./run.sh day3.DclSingleton        （约 15 秒）
 */
public class DclSingleton {

    // ---------------------------------------------------------------
    // 被测的两个版本
    // ---------------------------------------------------------------

    /** D1：经典错误写法 —— 少一个 volatile。 */
    private static Singleton plain;

    /** D2：正确写法。 */
    private static volatile Singleton safe;

    private static final Object LOCK = new Object();

    static Singleton getPlain() {
        if (plain == null) {                    // 第一次检查（无锁，为了性能）
            synchronized (LOCK) {
                if (plain == null) {            // 第二次检查（锁内，防重复创建）
                    plain = new Singleton("plain");
                }
            }
        }
        return plain;
    }

    static Singleton getSafe() {
        if (safe == null) {
            synchronized (LOCK) {
                if (safe == null) {
                    safe = new Singleton("safe");
                }
            }
        }
        return safe;
    }

    // ---------------------------------------------------------------
    // 被创建的对象：构造里 sleep，故意把「引用发布」与「字段写完」的窗口拉大
    // ---------------------------------------------------------------

    static class Singleton {
        int a;
        String b;
        long c;

        Singleton(String tag) {
            this(tag, true);
        }

        /** init=false 时不跑构造体，字段保持默认值 —— 等价于「对象已分配、构造还没跑」。 */
        Singleton(String tag, boolean init) {
            if (init) init(tag);
        }

        void init(String tag) {
            a = 1;                 // 字段 1
            sleep(2);              // 放大窗口：如果引用在这之前就被发布，别人会看到 a=0
            b = tag;               // 字段 2
            c = 99L;               // 字段 3
        }

        /** 构造体没跑完：b 还是 null，或 c 还是 0。a 可能已经被赋值了，所以不能只看 a。 */
        boolean incomplete() {
            return b == null || c != 99L;
        }

        @Override
        public String toString() {
            return "Singleton{a=" + a + ", b=" + b + ", c=" + c + "}";
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---------------------------------------------------------------
    // main
    // ---------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        System.out.println("JDK " + System.getProperty("java.version")
                + " / " + Runtime.getRuntime().availableProcessors() + " 核 / "
                + System.getProperty("os.arch"));

        sectionD1D2();
        sectionD3();
        sectionD4();
    }

    private static void sectionD1D2() throws Exception {
        System.out.println("\n========== D1/D2 · 压力测试：不加 volatile vs 加 volatile ==========");
        final int ROUNDS = 50, THREADS = 6;

        long badPlain = stress(false, ROUNDS, THREADS);
        System.out.printf("  [D1 无 volatile] %d 轮 × %d 线程（每轮抢 30 ms）：拿到构造没跑完的对象 %d 次%n",
                ROUNDS, THREADS, badPlain);

        long badSafe = stress(true, ROUNDS, THREADS);
        System.out.printf("  [D2 有 volatile] %d 轮 × %d 线程（每轮抢 30 ms）：拿到构造没跑完的对象 %d 次%n",
                ROUNDS, THREADS, badSafe);

        System.out.println("  → 注意这个 0 不能当成「不加 volatile 也没事」。");
        System.out.println("    x86 是 TSO：store-store 不重排，HotSpot 在 x86 后端也不会把 `putfield instance`");
        System.out.println("    搬到构造体前面 —— 所以这个 bug 在 x86 开发机上「测不出来」，这正是它最坑的地方。");
        System.out.println("    JMM 允许它发生（D3 手工写出那个顺序），换 ARM / 换 JIT 版本就炸。");
        System.out.println("    判断标准只能是 JMM，不是「我跑了 50 轮没事」。");
    }

    /**
     * 每轮：清空单例 → 同时放行 N 个线程抢着调 getInstance() 共 30 ms →
     * 任何一次拿到 non-null 但构造没跑完的对象就记一次。
     * 用「时间窗口」而不是「固定次数」：构造里有 2 ms sleep，固定 3000 次可能在窗口外就跑完了。
     */
    private static long stress(boolean vol, int rounds, int threads) throws Exception {
        long bad = 0;
        for (int r = 0; r < rounds; r++) {
            if (vol) safe = null; else plain = null;
            CountDownLatch go = new CountDownLatch(1);
            AtomicLong badInRound = new AtomicLong();
            Thread[] ts = new Thread[threads];
            for (int i = 0; i < threads; i++) {
                ts[i] = new Thread(() -> {
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    long deadline = System.nanoTime() + 30_000_000L;   // 30 ms
                    while (System.nanoTime() < deadline) {
                        Singleton s = vol ? getSafe() : getPlain();
                        if (s != null && s.incomplete()) {
                            badInRound.incrementAndGet();
                        }
                    }
                }, (vol ? "D2-" : "D1-") + i);
            }
            for (Thread t : ts) t.start();
            go.countDown();
            for (Thread t : ts) t.join();
            bad += badInRound.get();
        }
        return bad;
    }

    // ---------------------------------------------------------------
    // D3：把重排后的执行顺序手工写出来
    // ---------------------------------------------------------------

    private static volatile Singleton published;

    private static void sectionD3() throws Exception {
        System.out.println("\n========== D3 · 把「重排后」的执行顺序手工写出来 ==========");
        System.out.println("  正常顺序：分配 → 执行构造（a=1,b=..,c=99）→ 把引用写进字段");
        System.out.println("  重排顺序：分配 → 把引用写进字段 → 执行构造");
        System.out.println("  下面这段就是把重排顺序显式写出来，看读线程会拿到什么：\n");

        published = null;
        CountDownLatch readerGo = new CountDownLatch(1);
        final String[] seenByReader = new String[1];
        final boolean[] halfSeen = new boolean[1];

        Thread reader = new Thread(() -> {
            try {
                readerGo.await();
            } catch (InterruptedException e) {
                return;
            }
            long deadline = System.nanoTime() + 1_000_000_000L;
            while (System.nanoTime() < deadline) {
                Singleton s = published;
                if (s != null) {
                    // 先把三个字段各抓一次快照，再判断 —— 否则 toString 抓到的是半截，
                    // 后面调用 incomplete() 时构造可能已经跑完了，两句话会对不上（第一版就是这样）
                    int av = s.a; String bv = s.b; long cv = s.c;
                    seenByReader[0] = "Singleton{a=" + av + ", b=" + bv + ", c=" + cv + "}";
                    halfSeen[0] = (bv == null || cv != 99L);
                    return;
                }
            }
        }, "D3-reader");
        reader.start();
        readerGo.countDown();

        // ① 先分配（构造体不跑，字段全是默认值）
        Singleton s = new Singleton("reordered", false);
        // ② 先发布引用 —— 就是 javac/JIT 被允许做的那个重排
        published = s;
        // ③ 再执行构造体
        s.init("reordered");

        reader.join();
        System.out.println("  读线程在「引用已发布、构造还没跑」的窗口里抓到了：");
        System.out.println("    " + seenByReader[0]);
        System.out.println("    incomplete（b==null || c!=99）= " + halfSeen[0]);
        System.out.println("  → 这就是 DCL 少写 volatile 时可能递给别人的对象：引用不为 null，构造只跑了一半。");
        System.out.println("    注意 a=1 已经赋上了、b 还是 null —— 半初始化不是「全是默认值」，是「停在中途」。");
        System.out.println("    调用方拿到它之后调 b.length() 就是 NPE —— 堆栈指向调用方，跟构造毫无关系，");
        System.out.println("    所以这类 bug 的排查方向天然是错的（会去查 b 为什么是 null，而不是查单例怎么发出去的）。");
        sleep(5);
    }

    // ---------------------------------------------------------------
    // D4：Unsafe.allocateInstance —— 不跑构造也能造出对象
    // ---------------------------------------------------------------

    private static void sectionD4() {
        System.out.println("\n========== D4 · 用 Unsafe.allocateInstance 直接看「没跑构造的对象」 ==========");
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            Singleton ghost = (Singleton) unsafeClass
                    .getMethod("allocateInstance", Class.class)
                    .invoke(unsafe, Singleton.class);
            System.out.println("  allocateInstance 拿到的对象：" + ghost);
            System.out.println("  incomplete = " + ghost.incomplete());
            System.out.println("  → 这就是「分配了但没构造」的真实内存状态，不是比喻：字段全是零值/ null。");
            System.out.println("    也说明 JVM 的 new 在字节码里是一步（new + invokespecial），在内存里是两步。");
        } catch (Throwable e) {
            System.out.println("  Unsafe 取不到（" + e.getClass().getSimpleName() + "），跳过这一段。");
            System.out.println("  说明：JDK 17 上 sun.misc.Unsafe 在 jdk.unsupported 模块里，");
            System.out.println("  反射 theUnsafe 需要该模块 open；D3 段已经用纯 Java 复现了同样的状态。");
        }
        System.out.println("\n结论：DCL 的 volatile 不是为了「让 instance 可见」这么模糊的一句话，");
        System.out.println("      而是为了禁止 `instance = new Singleton()` 内部「发布引用」与「初始化字段」的重排。");
    }
}
