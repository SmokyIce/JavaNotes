package com.newt.java48.w02.day2;

/**
 * Day 2 · 遗留补完：异常到底贵在哪
 *
 * <p>Day 2 只说了「{@code fillInStackTrace} 贵」，没量。这一节量出来，顺带当第一个 JVM 参数实验：
 * {@code -XX:-StackTraceInThrowable} 能关掉栈采集，关掉之后 throw 便宜多少。
 *
 * <pre>
 * A  new 但不抛（栈深 1）—— 成本发生在构造函数的 fillInStackTrace 里
 * B  throw + catch（栈深 1）—— 与 A 的差值就是「抛」这个动作本身
 * C  throw + catch（栈深 6）—— 抓栈成本随栈深增长
 * D  反复抛同一个缓存异常 —— 栈只采集一次
 * </pre>
 *
 * <p>测量方式：每个用例跑 {@link #ROUNDS} 轮、每轮 {@link #N} 次，**取最小值**。
 * 单次测一组数字会被 JIT 编译层级（解释执行 → C1 → C2）带偏，同一段代码
 * 在一次测量里能读出 44 ns，下一次 295 ns。取最小值是微基准的标准做法：
 * 最小值代表「代码已经在 C2 下跑稳」时的真实成本。
 *
 * <p>运行（两遍，对比看）：
 * <pre>
 * java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w02.day2.ExceptionCost
 * java -Dfile.encoding=UTF-8 -XX:-StackTraceInThrowable -cp out com.newt.java48.w02.day2.ExceptionCost
 * </pre>
 */
public class ExceptionCost {

    private static final int N = 100_000;
    private static final int WARMUP = 100_000;
    private static final int ROUNDS = 5;
    private static int sink = 0;

    static class BizException extends RuntimeException {
        BizException() {
            super("业务异常");
        }
    }

    @FunctionalInterface
    interface Probe {
        long run(int n);
    }

    /** 每个用例返回「最好那一轮」的耗时（ns/次）。 */
    private static double best(Probe probe) {
        long min = Long.MAX_VALUE;
        for (int r = 0; r < ROUNDS; r++) {
            min = Math.min(min, probe.run(N));
        }
        return min / (double) N;
    }

    public static void main(String[] args) {
        System.out.println("========== 异常开销实测 ==========");
        System.out.println("JVM       = " + System.getProperty("java.version"));
        System.out.printf("测量方式   = 每个用例 %d 轮 × %d 次，取最小值（预热 %d）%n", ROUNDS, N, WARMUP);
        System.out.println("栈深 1 帧数 = " + framesOf(() -> {
            throw new BizException();
        }));
        System.out.println("栈深 6 帧数 = " + framesOf(() -> {
            try {
                depth6();
            } catch (BizException e) {
                throw e;
            }
        }) + "   （为 0 = 栈采集被 -XX:-StackTraceInThrowable 关掉了）");

        // 预热：让所有方法都进 C2
        for (int i = 0; i < 3; i++) {
            newShallow(WARMUP);
            throwShallow(WARMUP);
            throwDeep(WARMUP);
            throwCached(WARMUP);
        }

        double a = best(ExceptionCost::newShallow);
        double b = best(ExceptionCost::throwShallow);
        double c = best(ExceptionCost::throwDeep);
        double d = best(ExceptionCost::throwCached);

        row("A new 不抛（栈深 1）", a);
        row("B throw+catch（栈深 1）", b);
        row("C throw+catch（栈深 6）", c);
        row("D throw 缓存异常", d);

        System.out.printf("%nB - A = %+6.0f ns/次  -> 「抛 + 捕」这个动作本身（应接近 0）%n", b - a);
        System.out.printf("C - B = %+6.0f ns/次  -> 多走 5 层栈的抓栈成本（关掉采集后应归零）%n", c - b);
        System.out.println("D 恒为 0：整个「抛常量异常 + catch」被 JIT 消掉了 —— 缓存异常不但查不到现场，"
                + "连成本数字都不具参考性");
        System.out.println();
        System.out.println("两遍对比 A / B / C 三行 ——");
        System.out.println("  默认：A、B 都在 400~470 ns 量级，C 再贵 30 ns 左右（多 5 层栈）");
        System.out.println("  关掉采集：三行一起掉到 30~40 ns，A / B / C 之间没有差别，约 15 倍差距");
        System.out.println("  所以「异常慢」慢在 fillInStackTrace 遍历调用栈，不在跳转。");
        System.out.println("  生产别关这个参数 —— 排障靠的就是那份栈。");
        System.out.println("sink=" + sink);
    }

    // ---------------------------------------------------------------- 栈深 6 的调用链
    private static void depth6() { depth5(); }
    private static void depth5() { depth4(); }
    private static void depth4() { depth3(); }
    private static void depth3() { depth2(); }
    private static void depth2() { depth1(); }
    private static void depth1() { throw new BizException(); }

    private static int framesOf(Runnable r) {
        try {
            r.run();
        } catch (BizException e) {
            return e.getStackTrace().length;
        }
        return -1;
    }

    private static long newShallow(int n) {
        long t = System.nanoTime();
        for (int i = 0; i < n; i++) {
            BizException e = new BizException();
            sink += e.getMessage().length();
        }
        return System.nanoTime() - t;
    }

    private static long throwShallow(int n) {
        long t = System.nanoTime();
        for (int i = 0; i < n; i++) {
            try {
                throw new BizException();
            } catch (BizException e) {
                sink += e.getMessage().length();
            }
        }
        return System.nanoTime() - t;
    }

    private static long throwDeep(int n) {
        long t = System.nanoTime();
        for (int i = 0; i < n; i++) {
            try {
                depth6();
            } catch (BizException e) {
                sink += e.getMessage().length();
            }
        }
        return System.nanoTime() - t;
    }

    private static long throwCached(int n) {
        BizException cached = new BizException();
        long t = System.nanoTime();
        for (int i = 0; i < n; i++) {
            try {
                throw cached;
            } catch (BizException e) {
                sink += 1;
            }
        }
        return System.nanoTime() - t;
    }

    private static void row(String name, double nsPerOp) {
        System.out.printf("%-24s 单次 %7.1f ns%n", name, nsPerOp);
    }
}
