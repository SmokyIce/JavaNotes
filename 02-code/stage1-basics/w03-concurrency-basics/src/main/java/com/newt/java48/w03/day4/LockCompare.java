package com.newt.java48.w03.day4;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * W03 Day 4 · A/B：ReentrantLock 的两个开关
 *
 * <p>A 段：公平 vs 非公平 —— 不比「谁快」，比三个能数的东西：
 * <ul>
 *   <li>吞吐：非公平应该明显快；</li>
 *   <li><b>连续两次拿到锁的次数</b>：刚 unlock 的线程转头又 lock 成功 = 插队。公平模式下这个数应该接近 0；</li>
 *   <li>单线程最长等待：公平模式有上限（排在你前面的线程数 × 临界区），非公平可能被饿很久。</li>
 * </ul>
 *
 * <p>B 段：{@code Condition} 的 await/signal 对照 {@code wait/notifyAll}。
 * 同样是「有界队列 + 生产者消费者」，数的是<b>无效唤醒</b>：
 * 醒来之后发现条件还是不成立，只好再睡回去的次数。
 * <ul>
 *   <li>Condition 版：两个条件队列（非空 / 非满），signal 精确叫一个 → 无效唤醒应该极少；</li>
 *   <li>notifyAll 版：只有一把锁、一个等待集合，每次唤醒全部 4 个消费者 → 3 个白醒。</li>
 * </ul>
 *
 * 运行：./run.sh day4.LockCompare       （约 8 秒）
 */
public class LockCompare {

    public static void main(String[] args) throws Exception {
        System.out.println("JDK " + System.getProperty("java.version")
                + " / " + Runtime.getRuntime().availableProcessors() + " 核");
        sectionA();
        sectionB();
        System.out.println("\n全部段落跑完。");
    }

    // ==================================================================
    // A · 公平 vs 非公平
    // ==================================================================

    static final int A_THREADS = 8;
    static final int A_PER_THREAD = 20_000;

    private static int counter;

    private static void sectionA() throws Exception {
        System.out.println("\n========== A · 公平锁 vs 非公平锁（" + A_THREADS + " 线程 × "
                + A_PER_THREAD + " 次） ==========");

        Result unfair = runFairness(false);
        Result fair = runFairness(true);

        System.out.printf("  %-12s %10s %14s %16s%n", "模式", "耗时", "连续抢到次数", "单线程最长等待");
        System.out.printf("  %-10s %8d ms %12d 次 %12d µs%n", "非公平",
                unfair.ms, unfair.consecutive, unfair.maxWaitNanos / 1000);
        System.out.printf("  %-10s %8d ms %12d 次 %12d µs%n", "公平",
                fair.ms, fair.consecutive, fair.maxWaitNanos / 1000);

        System.out.printf("  → 非公平快 %.1f 倍；连续抢到 %d 次 vs %d 次。%n",
                fair.ms / (double) Math.max(1, unfair.ms), unfair.consecutive, fair.consecutive);
        System.out.println("  → 「连续抢到」= 同一个线程 unlock 之后立刻又 lock 成功。");
        System.out.println("     非公平锁允许它直接插队（barging），公平锁必须先去队尾排队，所以这个数接近 0。");
        System.out.println("  → 注意「最长等待」这一列：公平锁反而更大（" + fair.maxWaitNanos / 1000
                + " µs vs " + unfair.maxWaitNanos / 1000 + " µs），这和「公平锁不会饿死人」的直觉是反的。");
        System.out.println("     原因是临界区只有 1 ns，公平锁等的主要不是临界区，而是「解锁后唤醒下一个人」的交接成本（几 µs）；");
        System.out.println("     排 8 个人就是 8 次交接。非公平锁靠自旋，反而几乎不用走内核。");
        System.out.println("  → 所以公平换来的不是「等得更短」，是「等得有上界、可预测」：");
        System.out.println("     最坏情况 = 排在你前面的人数 × 交接成本；而非公平的理论最坏情况是无穷（可以被一直插队）。");
    }

    static final class Result {
        long ms;
        int consecutive;
        long maxWaitNanos;
    }

    private static Result runFairness(boolean fair) throws Exception {
        ReentrantLock lock = new ReentrantLock(fair);
        final int[] consecutive = new int[A_THREADS];
        final long[] maxWait = new long[A_THREADS];
        final int[] lastOwner = {-1};

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(A_THREADS);
        counter = 0;

        for (int i = 0; i < A_THREADS; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < A_PER_THREAD; j++) {
                        long t0 = System.nanoTime();
                        lock.lock();
                        try {
                            long waited = System.nanoTime() - t0;
                            if (waited > maxWait[id]) {
                                maxWait[id] = waited;
                            }
                            // 上一次持锁的也是我 → 我插队成功了
                            if (lastOwner[0] == id) {
                                consecutive[id]++;
                            }
                            lastOwner[0] = id;
                            counter++;
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "fair-" + i).start();
        }

        long t = System.nanoTime();
        start.countDown();
        done.await();

        Result r = new Result();
        r.ms = (System.nanoTime() - t) / 1_000_000;
        for (int i = 0; i < A_THREADS; i++) {
            r.consecutive += consecutive[i];
            r.maxWaitNanos = Math.max(r.maxWaitNanos, maxWait[i]);
        }
        return r;
    }

    // ==================================================================
    // B · Condition 精确唤醒 vs notifyAll 全员广播
    // ==================================================================

    /** 容量故意设成 1：队列几乎总是「空」或「满」，每次唤醒都是一场争抢，差别才看得见 */
    static final int CAP = 1;
    static final int PRODUCERS = 4;
    static final int CONSUMERS = 4;
    static final int ITEMS = 8_000;

    private static void sectionB() throws Exception {
        System.out.println("\n========== B · Condition 精确唤醒 vs notifyAll 全员广播 ==========");
        System.out.println("  " + PRODUCERS + " 生产者 + " + CONSUMERS + " 消费者，队列容量 " + CAP + "，共 "
                + ITEMS + " 件货");

        QueueResult cond = runWithCondition();
        QueueResult sync = runWithNotifyAll();

        System.out.printf("  %-14s %10s %14s%n", "实现", "耗时", "无效唤醒");
        System.out.printf("  %-12s %8d ms %12d 次%n", "Condition", cond.ms, cond.invalidWakes);
        System.out.printf("  %-12s %8d ms %12d 次%n", "wait/notifyAll", sync.ms, sync.invalidWakes);

        System.out.println("  → 「无效唤醒」= 醒来一看条件还是不成立，只能再睡回去的次数。");
        System.out.printf("     Condition 版 %d 次，notifyAll 版 %d 次，差 %.0f 倍。%n",
                cond.invalidWakes, sync.invalidWakes,
                sync.invalidWakes / (double) Math.max(1, cond.invalidWakes));
        System.out.println("  → 差距来自「唤醒谁」：Condition 有两个等待队列，signal 只叫非空队列里的一个；");
        System.out.println("     synchronized 只有一个等待集合，notifyAll 把消费者和生产者一起叫醒，绝大多数醒了也白醒。");
        System.out.println("  → 这就是 Condition 存在的唯一理由：把「等同一个条件的线程」分组成多个队列。");
    }

    static final class QueueResult {
        long ms;
        long invalidWakes;
    }

    /** Condition 版：两个条件队列，signal 精确叫一个 */
    private static QueueResult runWithCondition() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        Condition notFull = lock.newCondition();
        Condition notEmpty = lock.newCondition();
        Deque<Integer> q = new ArrayDeque<>();
        AtomicLong invalid = new AtomicLong();
        AtomicInteger produced = new AtomicInteger();

        CountDownLatch done = new CountDownLatch(PRODUCERS + CONSUMERS);
        long t = System.nanoTime();

        for (int i = 0; i < PRODUCERS; i++) {
            final int share = shareOf(i);
            new Thread(() -> {
                try {
                    for (int k = 0; k < share; k++) {
                        lock.lock();
                        try {
                            while (q.size() == CAP) {
                                notFull.await();
                                if (q.size() == CAP) {
                                    invalid.incrementAndGet();
                                }
                            }
                            q.addLast(1);
                            if (produced.incrementAndGet() == ITEMS) {
                                notEmpty.signalAll();   // 叫醒所有消费者：没货了，该退出了
                            } else {
                                notEmpty.signal();      // 只叫一个，且它一定能拿到货
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "p-" + i).start();
        }

        for (int i = 0; i < CONSUMERS; i++) {
            new Thread(() -> {
                try {
                    for (;;) {
                        lock.lock();
                        try {
                            while (q.isEmpty() && produced.get() < ITEMS) {
                                notEmpty.await();
                                if (q.isEmpty() && produced.get() < ITEMS) {
                                    invalid.incrementAndGet();
                                }
                            }
                            if (q.isEmpty()) {
                                return;                 // 货已全部生产完且取空
                            }
                            q.removeFirst();
                            notFull.signal();           // 只叫一个生产者
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "c-" + i).start();
        }

        done.await();
        QueueResult r = new QueueResult();
        r.ms = (System.nanoTime() - t) / 1_000_000;
        r.invalidWakes = invalid.get();
        return r;
    }

    /** 对照组：一把 monitor、一个等待集合，notifyAll 全员广播 */
    private static QueueResult runWithNotifyAll() throws Exception {
        final Object mon = new Object();
        Deque<Integer> q = new ArrayDeque<>();
        AtomicLong invalid = new AtomicLong();
        int[] produced = {0};

        CountDownLatch done = new CountDownLatch(PRODUCERS + CONSUMERS);
        long t = System.nanoTime();

        for (int i = 0; i < PRODUCERS; i++) {
            final int share = shareOf(i);
            new Thread(() -> {
                try {
                    for (int k = 0; k < share; k++) {
                        synchronized (mon) {
                            while (q.size() == CAP) {
                                mon.wait();
                                if (q.size() == CAP) {
                                    invalid.incrementAndGet();
                                }
                            }
                            q.addLast(1);
                            produced[0]++;
                            mon.notifyAll();            // 消费者和生产者一起醒
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "pn-" + i).start();
        }

        for (int i = 0; i < CONSUMERS; i++) {
            new Thread(() -> {
                try {
                    for (;;) {
                        synchronized (mon) {
                            while (q.isEmpty() && produced[0] < ITEMS) {
                                mon.wait();
                                if (q.isEmpty() && produced[0] < ITEMS) {
                                    invalid.incrementAndGet();
                                }
                            }
                            if (q.isEmpty()) {
                                return;
                            }
                            q.removeFirst();
                            mon.notifyAll();            // 全员广播，包括其他消费者
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "cn-" + i).start();
        }

        done.await();
        QueueResult r = new QueueResult();
        r.ms = (System.nanoTime() - t) / 1_000_000;
        r.invalidWakes = invalid.get();
        return r;
    }

    /** 每个生产者分到的件数，余数给最后一个 */
    private static int shareOf(int id) {
        int base = ITEMS / PRODUCERS;
        return id == PRODUCERS - 1 ? base + ITEMS % PRODUCERS : base;
    }
}
