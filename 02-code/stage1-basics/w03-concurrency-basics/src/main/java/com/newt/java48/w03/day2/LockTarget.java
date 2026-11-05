package com.newt.java48.w03.day2;

import java.util.concurrent.CountDownLatch;
import java.util.function.IntSupplier;

/**
 * Day 2 · A/B/C：synchronized 到底锁的是谁
 *
 * <p>A 段：实例方法 → 锁 this（同一个实例互斥，两个实例互不影响）
 * <p>B 段：静态方法 → 锁 Class 对象（与实例锁互不影响）
 * <p>C 段：synchronized (new Object()) → 每次都换一把新锁，等于没锁
 *
 * <p>证据不是我说它锁 this，而是两条能打印的东西：
 * <ul>
 *   <li>{@link Thread#holdsLock(Object)} —— 线程当前是否持有某个对象的锁，直接问 JVM；</li>
 *   <li>方法入口处算「排队等了多久」—— 被别的锁挡住时这个数字不是 0。</li>
 * </ul>
 */
public class LockTarget {

    /** 每个临界区故意占住锁的时间，单位 ms */
    static final long HOLD_MS = 400;

    public static void main(String[] args) throws Exception {
        sectionA();
        sectionB();
        sectionC();
    }

    // ==================================================================
    // A · 实例方法 synchronized 锁的是 this
    // ==================================================================
    static class InstanceLock {
        private int count;

        /**
         * 注意参数里的 callNanos：调用方在调之前打一个点，
         * 方法进来第一件事算差值 —— 差值就是「在锁上排队」的时间。
         */
        synchronized void work(String who, long callNanos) {
            long waited = (System.nanoTime() - callNanos) / 1_000_000;
            count++;
            System.out.printf("    [%s] 排队 %3d ms 才进方法；holdsLock(this)=%s，holdsLock(另一个实例)=%s%n",
                    who, waited, Thread.holdsLock(this), Thread.holdsLock(OTHER));
            sleep(HOLD_MS);
        }

        synchronized int count() {
            return count;
        }
    }

    /** 只为在 A 段里回答「那把锁是不是也锁住了别的实例」 */
    static final InstanceLock OTHER = new InstanceLock();

    static void sectionA() throws Exception {
        System.out.println("========== A · 实例方法 synchronized 锁的是 this ==========");

        // A1：同一个实例，两个线程
        System.out.println("A1 同一实例 × 2 线程（应该串行：第二个排队约 400 ms）");
        InstanceLock one = new InstanceLock();
        runPair(() -> one.work("A1-线程1", System.nanoTime()),
                () -> one.work("A1-线程2", System.nanoTime()));
        System.out.println("    → count = " + one.count() + "（两次自增都在，锁生效）");

        // A2：两个不同实例，各一个线程
        System.out.println("A2 两个不同实例 × 各 1 线程（应该并行：都排队 0 ms）");
        InstanceLock s1 = new InstanceLock();
        InstanceLock s2 = new InstanceLock();
        runPair(() -> s1.work("A2-实例1", System.nanoTime()),
                () -> s2.work("A2-实例2", System.nanoTime()));

        // A3：用同步块锁住同一个实例，再去调同步方法 —— 如果锁的是 this，必然被挡住
        System.out.println("A3 先 synchronized(inst) 持锁 400 ms，再调 inst.work()（应被挡住约 350 ms）");
        InstanceLock inst = new InstanceLock();
        Thread holder = new Thread(() -> {
            synchronized (inst) {
                System.out.println("    [A3-持锁者] 已持有 inst 的锁，占 400 ms");
                sleep(HOLD_MS);
            }
        }, "A3-holder");
        holder.start();
        sleep(50);                       // 让 holder 先进去，否则谁先拿锁说不准
        long t = System.nanoTime();
        Thread waiter = new Thread(() -> inst.work("A3-调用者", System.nanoTime()), "A3-waiter");
        waiter.start();
        waiter.join();
        holder.join();
        System.out.printf("    → 从发起到方法返回共 %d ms = 排队 + 400 ms 自身占用%n", ms(t));
        System.out.println("    → 结论：synchronized 方法 与 synchronized(this) 抢的是同一个 monitor");
    }

    // ==================================================================
    // B · 静态方法 synchronized 锁的是 Class 对象
    // ==================================================================
    static class ClassLock {
        static int count;

        static synchronized void swork(String who, long callNanos) {
            long waited = (System.nanoTime() - callNanos) / 1_000_000;
            count++;
            System.out.printf("    [%s] 排队 %3d ms 才进方法；holdsLock(ClassLock.class)=%s，holdsLock(某个实例)=%s%n",
                    who, waited, Thread.holdsLock(ClassLock.class), Thread.holdsLock(INSTANCE));
            sleep(HOLD_MS);
        }
    }

    /** 与 ClassLock 伴生的一个实例，用来证明「静态锁管不到实例」 */
    static final ClassLock INSTANCE = new ClassLock();

    static void sectionB() throws Exception {
        System.out.println();
        System.out.println("========== B · 静态方法 synchronized 锁的是 Class 对象 ==========");

        // B1：两个线程调静态方法 —— 连实例都没有，照样互斥
        System.out.println("B1 两个线程调 ClassLock.swork()（同一把 Class 锁，第二个排队约 400 ms）");
        runPair(() -> ClassLock.swork("B1-线程1", System.nanoTime()),
                () -> ClassLock.swork("B1-线程2", System.nanoTime()));
        System.out.println("    → 静态 count = " + ClassLock.count);

        // B2：同步块锁 ClassLock.class，再去调静态方法
        System.out.println("B2 先 synchronized(ClassLock.class) 持锁 400 ms，再调 swork()（应被挡住）");
        Thread holder = new Thread(() -> {
            synchronized (ClassLock.class) {
                System.out.println("    [B2-持锁者] 已持有 ClassLock.class 的锁，占 400 ms");
                sleep(HOLD_MS);
            }
        }, "B2-holder");
        holder.start();
        sleep(50);
        Thread waiter = new Thread(() -> ClassLock.swork("B2-调用者", System.nanoTime()), "B2-waiter");
        waiter.start();
        waiter.join();
        holder.join();
        System.out.println("    → 结论：静态方法 与 synchronized(Xxx.class) 抢的是同一个 monitor");

        // B3：静态方法 vs 实例上的锁 —— 两把不同的锁，不该互斥
        System.out.println("B3 一个线程走静态方法、一个线程锁住某个实例（应该并行）");
        runPair(() -> ClassLock.swork("B3-静态", System.nanoTime()),
                () -> lockInstance(INSTANCE, "B3-实例", System.nanoTime()));
        System.out.println("    → 上面那个总耗时如果到 800 ms 上下，才说明两把是同一把锁");
        System.out.println("    → 结论：Class 锁 与 this 锁 是两把锁，静态同步方法保护不了实例字段");
    }

    /** 在某个实例上拿锁 —— 语义上等价于调用它的实例同步方法 */
    static void lockInstance(Object target, String who, long startNanos) {
        synchronized (target) {
            System.out.printf("    [%s] 排队 %3d ms；holdsLock(实例)=%s，holdsLock(ClassLock.class)=%s%n",
                    who, (System.nanoTime() - startNanos) / 1_000_000,
                    Thread.holdsLock(target), Thread.holdsLock(ClassLock.class));
            sleep(HOLD_MS);
        }
    }

    // ==================================================================
    // C · synchronized (new Object()) 等于没锁
    // ==================================================================
    static class BadCounter {
        private int value;

        /** ✗ 每次进方法 new 一个新对象 —— 十把钥匙开十把锁，谁也不挡谁 */
        void inc() {
            synchronized (new Object()) {
                value++;
            }
        }

        int value() {
            return value;
        }
    }

    static class GoodCounter {
        /** ✓ 锁对象必须是「所有线程共享的同一个 final 字段」 */
        private final Object lock = new Object();
        private int value;

        void inc() {
            synchronized (lock) {
                value++;
            }
        }

        int value() {
            return value;
        }
    }

    static final int THREADS = 10;
    static final int PER_THREAD = 500_000;
    static final int EXPECTED = THREADS * PER_THREAD;

    static void sectionC() throws Exception {
        System.out.println();
        System.out.println("========== C · synchronized (new Object()) 等于没锁 ==========");

        BadCounter bad = new BadCounter();
        long t1 = hammer(bad::inc);
        System.out.printf("C1 锁 new Object()：期望 %d，实际 %d，丢 %d，耗时 %d ms%n",
                EXPECTED, bad.value(), EXPECTED - bad.value(), t1);

        GoodCounter good = new GoodCounter();
        long t2 = hammer(good::inc);
        System.out.printf("C2 锁共享 final 字段：期望 %d，实际 %d，丢 %d，耗时 %d ms%n",
                EXPECTED, good.value(), EXPECTED - good.value(), t2);

        // C3：字符串常量是驻留的 —— 拿 "LOCK" 当锁，等于拿了一把全局锁
        String mine = "LOCK";
        String others = OtherCode.LOCK;
        System.out.printf("C3 本类的 \"LOCK\" 与 OtherCode.LOCK 是同一个对象吗？%s（是则同一把锁）%n", mine == others);
        Thread holder = new Thread(() -> {
            synchronized (mine) {
                System.out.println("    [C3-我的类] 持有本类字符串常量的锁 400 ms");
                sleep(HOLD_MS);
            }
        }, "C3-own");
        holder.start();
        sleep(50);
        long t = System.nanoTime();
        Thread waiter = new Thread(() -> {
            synchronized (others) {
                System.out.printf("    [C3-别的类] 排队 %d ms 才进来 —— 被一个不相干的类挡住了%n",
                        (System.nanoTime() - t) / 1_000_000);
            }
        }, "C3-other");
        waiter.start();
        waiter.join();
        holder.join();
        System.out.println("    → 结论：锁字符串常量等于锁公共资源，别人写 synchronized(\"LOCK\") 就会和你互相阻塞");
    }

    /** 另一个类里的字符串常量，与 LockTarget 里的 "LOCK" 是同一个对象 */
    static class OtherCode {
        static final String LOCK = "LOCK";
    }

    /** 十线程同时起跑各跑 PER_THREAD 次，返回耗时 ms */
    static long hammer(Runnable body) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < PER_THREAD; j++) {
                        body.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "c-" + i).start();
        }
        long t = System.nanoTime();
        start.countDown();
        done.await();
        return ms(t);
    }

    // ------------------------------------------------------------------
    static void runPair(Runnable a, Runnable b) throws Exception {
        Thread ta = new Thread(a, "pair-a");
        Thread tb = new Thread(b, "pair-b");
        long t = System.nanoTime();
        ta.start();
        tb.start();
        ta.join();
        tb.join();
        System.out.printf("    → 总耗时 %d ms%n", ms(t));
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static long ms(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
