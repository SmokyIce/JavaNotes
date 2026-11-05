package com.newt.java48.w03.day2;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Day 2 · D：死锁的复现、检测与破解
 *
 * <p>D1 交叉拿锁 → 真死锁；用 {@link ThreadMXBean#findDeadlockedThreads()} 程序化检测
 * （这是线上监控真正用的那一招，不是靠人眼看日志）。
 *
 * <p>D2 同一个线程重复拿同一把锁 —— 不死锁，因为 synchronized 可重入。
 * 「可重入」不是语法糖，它就是为了避免「自己把自己锁死」。
 *
 * <p>D3 用 tryLock 超时把死锁破掉：拿不到第二把就主动放掉第一把，重来。
 *
 * <p>用法：{@code java ... Deadlock hold} —— 只跑 D1 然后挂住 20 秒，
 * 方便在外部执行 {@code jstack <pid>} 抓「Found one Java-level deadlock」原件。
 */
public class Deadlock {

    static final Object A = new Object();
    static final Object B = new Object();

    /** D1 的两个线程要能被 join，所以存下来 */
    static Thread t1;
    static Thread t2;

    public static void main(String[] args) throws Exception {
        boolean hold = args.length > 0 && "hold".equals(args[0]);
        System.out.println("PID = " + ProcessHandle.current().pid()
                + "（需要抓现场就趁它活着：jstack " + ProcessHandle.current().pid() + "）");

        sectionD1(hold);
        if (hold) {
            System.out.println("\n【hold 模式】线程已卡死，保持 20 秒供外部 jstack 抓取…");
            Thread.sleep(20_000);
            System.out.println("【hold 模式】结束");
            System.exit(0);
        }

        sectionD2();
        sectionD3();
        System.exit(0);   // D1 的两个线程永远醒不来，不 exit 的话 JVM 不会退
    }

    // ==================================================================
    // D1 · 交叉拿锁 → 死锁 + 程序化检测
    // ==================================================================
    static void sectionD1(boolean hold) throws Exception {
        System.out.println();
        System.out.println("========== D1 · 交叉拿锁：A 等 B、B 等 A ==========");

        t1 = new Thread(() -> {
            synchronized (A) {
                System.out.println("  [t1] 拿到 A");
                sleep(100);                 // 把这个窗口留出来，t2 才有机会拿到 B
                System.out.println("  [t1] 想要 B …");
                synchronized (B) {
                    System.out.println("  [t1] 拿到 B（这行在死锁时永远不会打印）");
                }
            }
        }, "D1-A-then-B");

        t2 = new Thread(() -> {
            synchronized (B) {
                System.out.println("  [t2] 拿到 B");
                sleep(100);
                System.out.println("  [t2] 想要 A …");
                synchronized (A) {
                    System.out.println("  [t2] 拿到 A（这行在死锁时永远不会打印）");
                }
            }
        }, "D1-B-then-A");

        t1.start();
        t2.start();
        Thread.sleep(1_000);                // 给它们足够时间卡死

        System.out.println("  t1.isAlive=" + t1.isAlive() + " t2.isAlive=" + t2.isAlive()
                + "（还活着不等于在干活，可能只是卡住了）");

        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        System.out.println("  isSynchronizerUsageSupported = " + mx.isSynchronizerUsageSupported());

        long[] ids = mx.findDeadlockedThreads();
        if (ids == null) {
            System.out.println("  ✗ findDeadlockedThreads() 返回 null —— 没检出死锁（理论上不该发生）");
            return;
        }
        System.out.println("  ✓ findDeadlockedThreads() 检出 " + ids.length + " 个线程：");

        ThreadInfo[] infos = mx.getThreadInfo(ids, true, true);
        for (ThreadInfo info : infos) {
            System.out.printf("    - %s  state=%s  blockedCount=%d%n",
                    info.getThreadName(), info.getThreadState(), info.getBlockedCount());
            LockInfo waiting = info.getLockInfo();
            if (waiting != null) {
                System.out.printf("        在等 : %s @%s（被 %s 持有）%n",
                        waiting.getClassName(), hex(waiting.getIdentityHashCode()),
                        info.getLockOwnerName());
            }
            MonitorInfo[] held = info.getLockedMonitors();
            if (held != null && held.length > 0) {
                for (MonitorInfo m : held) {
                    // MonitorInfo 只多两个东西：在哪个栈深度、哪一帧拿的锁
                    StackTraceElement frame = m.getLockedStackFrame();
                    System.out.printf("        已持有: %s @%s（栈深度 %d，位置 %s）%n",
                            shortName(m.getClassName()), hex(m.getIdentityHashCode()),
                            m.getLockedStackDepth(),
                            frame == null ? "-" : shortName(frame.getClassName()) + "." + frame.getMethodName()
                                    + ":" + frame.getLineNumber());
                }
            }
        }
        System.out.println("  → 两个线程各持一把、各等一把，形成环 —— 这就是死锁的「循环等待」");
        System.out.println("  → 注意 blockedCount 只统计「这次进入阻塞」的次数，不是等了多少毫秒，");
        System.out.println("     等得久不久要看 jstack 里的线程栈 + 采样时间差");

        if (!hold) {
            System.out.println("  （两个线程会一直卡着，main 后面调 System.exit 收尾）");
        }
    }

    // ==================================================================
    // D2 · 同一线程重复拿同一把锁：可重入，不死锁
    // ==================================================================
    static void sectionD2() throws Exception {
        System.out.println();
        System.out.println("========== D2 · 可重入：自己锁自己不会死 ==========");

        // ⚠ 必须用新的锁对象：D1 的 A / B 已经被两个死锁线程永久占着，
        //    这里若继续 synchronized (A)，本段会跟着卡死（第一版就这么翻车的）
        final Object me = new Object();

        Thread t = new Thread(() -> {
            synchronized (me) {
                System.out.println("  [D2] 第一次进锁，holdsLock=" + Thread.holdsLock(me));
                synchronized (me) {
                    System.out.println("  [D2] 第二次进同一把锁，照样进去了 → 锁按「线程」记，不按「次」记");
                    nested(me, 3);
                    System.out.println("  [D2] 递归 3 层也回来了");
                }
            }
        }, "D2-reentrant");
        t.start();
        t.join(2_000);
        System.out.println("  → 若 synchronized 不可重入，上面第二行就会把自己锁死；");
        System.out.println("     所以「死锁四个必要条件」里没有『线程等自己』这一条 —— 它压根不成立");

        System.out.println("  → 对照：ReentrantLock 也可重入，但必须 lock 几次 unlock 几次");
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        lock.lock();
        System.out.println("     holdCount=" + lock.getHoldCount() + "（拿两次记 2）");
        lock.unlock();
        System.out.println("     unlock 一次后 holdCount=" + lock.getHoldCount() + "，锁还没真放开");
        lock.unlock();
        System.out.println("     unlock 两次后 holdCount=" + lock.getHoldCount() + "，别的线程这时才进得来");
        try {
            lock.unlock();
        } catch (IllegalMonitorStateException e) {
            System.out.println("     再多 unlock 一次 → " + e.getClass().getSimpleName()
                    + "（而 synchronized 的加解锁是 JVM 隐式配对的，多不出来）");
        }
    }

    static void nested(Object lock, int depth) {
        if (depth == 0) {
            return;
        }
        synchronized (lock) {
            nested(lock, depth - 1);
        }
    }

    // ==================================================================
    // D3 · 用 tryLock 破局：无退避会活锁，加退避才解得开；固定顺序更干脆
    // ==================================================================
    static void sectionD3() throws Exception {
        System.out.println();
        System.out.println("========== D3 · 破解：tryLock 拿不到就放掉，别硬等 ==========");

        int stuck = 0;
        int retrySum = 0;
        for (int i = 0; i < 5; i++) {
            int[] r = new int[2];
            long t0 = System.nanoTime();
            boolean ok = runTryLock(false, r, 2_000);
            if (!ok) {
                stuck++;
            }
            retrySum += r[0] + r[1];
            System.out.printf("      第 %d 次：%s，重试 %d + %d，耗时 %d ms%n",
                    i + 1, ok ? "跑完" : "活锁超时", r[0], r[1], ms(t0));
        }
        System.out.printf("D3a 不加退避 × 5 次（每次限时 2 秒）：活锁 %d/5 次，重试合计 %d 次%n", stuck, retrySum);

        int[] r2 = new int[2];
        boolean ok2 = runTryLock(true, r2, 5_000);
        System.out.printf("D3b 加随机退避：跑完？%s，重试 %d + %d 次%n", ok2, r2[0], r2[1]);

        System.out.println("  → 关键有两层：① 拿不到第二把必须把第一把还回去；");
        System.out.println("     ② 还回去之后要「错开时间」再来，否则两个线程踩同一个节拍反复对撞 ——");
        System.out.println("        那叫活锁（livelock）：线程都在跑、CPU 也在烧，就是没人推进。");
        System.out.println("        注意它和死锁的区别：死锁是确定的（D1 每次必卡），活锁是概率的（D3a 有时能自己走出来）");

        sectionD3c();
    }

    /** 两个线程分别按 左→右 / 右→左 的顺序抢，deadlineMs 内跑完返回 true */
    static boolean runTryLock(boolean backoff, int[] retries, long deadlineMs) throws Exception {
        ReentrantLock left = new ReentrantLock();
        ReentrantLock right = new ReentrantLock();
        CountDownLatch done = new CountDownLatch(2);
        CountDownLatch gate = new CountDownLatch(1);   // 同时起跑：活锁要靠对称才复现得出来
        java.util.concurrent.atomic.AtomicBoolean run = new java.util.concurrent.atomic.AtomicBoolean(true);

        Thread x = worker("D3-left-first", left, right, retries, 0, done, backoff, run, gate);
        Thread y = worker("D3-right-first", right, left, retries, 1, done, backoff, run, gate);
        x.start();
        y.start();
        gate.countDown();
        boolean ok = done.await(deadlineMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        run.set(false);              // 活锁的那两个线程得有人叫停，否则会一直烧 CPU 到 JVM 退出
        x.join(1_000);
        y.join(1_000);
        return ok;
    }

    static Thread worker(String name, ReentrantLock first, ReentrantLock second,
                         int[] retries, int idx, CountDownLatch done, boolean backoff,
                         java.util.concurrent.atomic.AtomicBoolean run, CountDownLatch gate) {
        return new Thread(() -> {
            java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
            try {
                gate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            while (run.get()) {
                boolean gotFirst = false;
                try {
                    if (!first.tryLock(200, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        retries[idx]++;
                        continue;
                    }
                    gotFirst = true;
                    sleep(backoff ? 50 + rnd.nextInt(120) : 50);   // 有退避就随机错开，没退避就死板 50 ms
                    if (second.tryLock(200, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        try {
                            done.countDown();
                            return;
                        } finally {
                            second.unlock();
                        }
                    }
                    retries[idx]++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    if (gotFirst) {
                        first.unlock();      // ★ 拿不到第二把，第一把必须还回去，否则就是 D1
                    }
                }
                if (backoff) {
                    sleep(rnd.nextInt(100));  // 让出这一段，避免两个线程同频对撞
                }
            }
        }, name);
    }

    /** D3c：固定顺序（按 identityHashCode 定序）—— 一次性破坏「循环等待」，不需要重试 */
    static void sectionD3c() throws Exception {
        final Object p = new Object();
        final Object q = new Object();
        final int[] sum = {0};
        final int rounds = 2_000;

        Runnable job = () -> {
            for (int i = 0; i < rounds; i++) {
                Object first = pick(p, q, true);
                Object second = pick(p, q, false);
                synchronized (first) {
                    synchronized (second) {
                        sum[0]++;
                    }
                }
            }
        };

        Thread a = new Thread(job, "D3c-A");
        Thread b = new Thread(job, "D3c-B");
        long t = System.nanoTime();
        a.start();
        b.start();
        a.join(5_000);
        b.join(5_000);
        boolean ok = !a.isAlive() && !b.isAlive();
        System.out.printf("D3c 固定顺序：跑完？%s，期望 %d，实际 %d，耗时 %d ms，零重试%n",
                ok, rounds * 2, sum[0], (System.nanoTime() - t) / 1_000_000);
        System.out.println("  → 全局定序（这里是按 identityHashCode）让两个线程的加锁顺序一致，");
        System.out.println("     「循环等待」这个条件从一开始就不成立 —— 这是生产代码里最常用的解法");
    }

    static Object pick(Object x, Object y, boolean first) {
        boolean xFirst = System.identityHashCode(x) <= System.identityHashCode(y);
        return (first == xFirst) ? x : y;
    }

    // ------------------------------------------------------------------
    static long ms(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    static String hex(int h) {
        return "0x" + Integer.toHexString(h);
    }

    static String shortName(String fqcn) {
        if (fqcn == null) {
            return "?";
        }
        int i = fqcn.lastIndexOf('.');
        return i < 0 ? fqcn : fqcn.substring(i + 1);
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
