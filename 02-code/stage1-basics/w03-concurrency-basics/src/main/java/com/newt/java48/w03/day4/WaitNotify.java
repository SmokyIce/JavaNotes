package com.newt.java48.w03.day4;

/**
 * W03 Day 4 · A ~ E：wait / notify 的三个「必须」和两个「其实」
 *
 * <p>A 段：wait() 不在 synchronized 里 → IllegalMonitorStateException（拿到真实的异常消息）
 * <p>B 段：「被唤醒」≠「条件成立」—— if 与 while 的差别，用 2 消费者 + notifyAll 精确复现
 * <p>C 段：notify() 一次只叫醒一个 —— 放下 2 件货只喊一声，剩下的人和货一起卡住
 * <p>D 段：wait() 会释放锁，sleep() 不会 —— 排队时长从几百 ms 掉到 0
 * <p>E 段：被中断时抛 InterruptedException，同时也把中断标志清掉了
 *
 * <p>关于「虚假唤醒」要说清一件事：真正的虚假唤醒（spurious wakeup）来自 JVM/OS，
 * 没有参数能让它必然发生，所以**不能靠「我等到了就能复现」来证明**。
 * 但它的后果和「notifyAll 多叫了一个人」「被中断」完全一样：醒了，条件还是不成立。
 * 本篇用的是后两种 —— 能 100% 复现的那种。
 *
 * 运行：./run.sh day4.WaitNotify       （约 5 秒）
 */
public class WaitNotify {

    public static void main(String[] args) throws Exception {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
        System.out.println("\n全部段落跑完。");
    }

    // ==================================================================
    // A · wait() 必须持有 monitor
    // ==================================================================

    private static void sectionA() {
        System.out.println("========== A · wait() 不写在 synchronized 里会怎样 ==========");

        Object any = new Object();
        try {
            any.wait();
            System.out.println("  ✗ 居然没抛异常？");
        } catch (IllegalMonitorStateException e) {
            System.out.println("  wait()   → IllegalMonitorStateException: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("  被中断（不该发生）");
        }

        try {
            any.notify();
            System.out.println("  ✗ 居然没抛异常？");
        } catch (IllegalMonitorStateException e) {
            System.out.println("  notify() → IllegalMonitorStateException: " + e.getMessage());
        }

        System.out.println("  → 两个方法都要先「拥有这个对象的 monitor」，也就是必须写在 synchronized(any) 里。");
        System.out.println("  → 原因不是语法洁癖：wait 要做的三件事（释放锁 → 挂起 → 被唤醒后重新抢锁）");
        System.out.println("     全都得围绕 monitor 完成，没有 monitor 就没有「要释放的东西」。");
    }

    // ==================================================================
    // B · 「被唤醒」≠「条件成立」：if 与 while
    // ==================================================================

    /** 只有一个位置的货架，用来看「两个人被叫醒、只有一件货」的后果。 */
    static final class Shelf {
        private Integer item;
        private boolean closed;
        int emptyTake;      // 条件不成立却继续往下走（取到了 null）的次数
        int guardRecheck;   // while 版本里「醒来发现没货、又回去等」的次数

        /** ✗ if 版本：只检查一次 */
        Integer takeWithIf(String who) throws InterruptedException {
            synchronized (this) {
                if (item == null) {
                    wait();
                }
                Integer got = item;
                item = null;
                if (got == null) {
                    emptyTake++;
                    System.out.printf("    [%s] ✗ 醒了就往下走，货架是空的 —— 这就是 if 的漏洞%n", who);
                } else {
                    System.out.printf("    [%s] ✓ 取到 %d%n", who, got);
                }
                return got;
            }
        }

        /** ✓ while 版本：被唤醒后重新验证条件 */
        Integer takeWithWhile(String who) throws InterruptedException {
            synchronized (this) {
                while (item == null && !closed) {
                    wait();
                    if (item == null && !closed) {
                        guardRecheck++;
                        System.out.printf("    [%s] 醒来后重查：还是空的 → 接着 wait()%n", who);
                    }
                }
                if (item == null) {
                    System.out.printf("    [%s] 货架已关闭，空闲退出%n", who);
                    return null;
                }
                Integer got = item;
                item = null;
                System.out.printf("    [%s] ✓ 取到 %d%n", who, got);
                return got;
            }
        }

        void put(int v) {
            synchronized (this) {
                item = v;
                System.out.printf("    [生产者] 放入 %d，notifyAll() 把所有等待者都叫醒%n", v);
                notifyAll();
            }
        }

        void close() {
            synchronized (this) {
                closed = true;
                notifyAll();
            }
        }
    }

    private static void sectionB() throws Exception {
        System.out.println();
        System.out.println("========== B · 被唤醒 ≠ 条件成立：if 只查一次，while 会重查 ==========");

        // ---- B1：if 版本 ----
        System.out.println("B1 两个消费者 wait() + 生产者放 1 件货 + notifyAll()（用 if）");
        Shelf s1 = new Shelf();
        Thread c1 = new Thread(() -> {
            try {
                s1.takeWithIf("消费者1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "B1-c1");
        Thread c2 = new Thread(() -> {
            try {
                s1.takeWithIf("消费者2");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "B1-c2");
        c1.start();
        c2.start();
        sleep(200);                       // 让他们都进到 wait()
        s1.put(7);
        c1.join(2000);
        c2.join(2000);
        System.out.printf("    → 空手而归（取到 null）%d 次%n", s1.emptyTake);
        System.out.println("    → 这就是无数生产者-消费者写错的地方：notifyAll 叫醒了 2 个人，货只有 1 件");

        // ---- B2：while 版本 ----
        System.out.println();
        System.out.println("B2 同样场景，把 if 换成 while");
        Shelf s2 = new Shelf();
        Thread d1 = new Thread(() -> {
            try {
                s2.takeWithWhile("消费者1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "B2-c1");
        Thread d2 = new Thread(() -> {
            try {
                s2.takeWithWhile("消费者2");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "B2-c2");
        d1.start();
        d2.start();
        sleep(200);
        s2.put(7);
        sleep(300);
        s2.close();
        d1.join(2000);
        d2.join(2000);
        System.out.printf("    → 空手而归 %d 次；「醒来重查又回去等」%d 次%n", s2.emptyTake, s2.guardRecheck);
        System.out.println("    → 结论：wait() 必须包在 while(条件不成立) 里。被叫醒只是「有机会了」，不是「条件成立了」。");
        System.out.println("    → 真正的虚假唤醒由 JVM/OS 发起，无法主动触发；但后果与上面这个场景一模一样。");
    }

    // ==================================================================
    // C · notify() 一次只叫醒一个
    // ==================================================================

    /**
     * 货架 + 两个消费者，两者等的都是同一个条件（有货）。
     *
     * <p>生产者一次放下 2 件货，然后<b>只喊一声</b>：
     * notify() 只会叫醒其中一个，被叫醒的拿走 1 件，另一个人和另一件货一起卡在那里。
     * notifyAll() 则两个都被叫醒，醒来各自重查条件，2 件货都被取走。
     *
     * <p>这个实验是确定的（不靠概率）：只要生产者少喊了一声，就必然有人醒不过来。
     */
    private static void sectionC() throws Exception {
        System.out.println();
        System.out.println("========== C · notify() 一次只叫醒一个 ==========");

        int[] r1 = runTwoItems(false);
        System.out.printf("C1 放 2 件货 + notify()    一次 → 取走 %d 件，剩 %d 件，还有 %d 个消费者停在 WAITING%n",
                r1[0], r1[1], r1[2]);
        int[] r2 = runTwoItems(true);
        System.out.printf("C2 放 2 件货 + notifyAll() 一次 → 取走 %d 件，剩 %d 件，还有 %d 个消费者停在 WAITING%n",
                r2[0], r2[1], r2[2]);

        System.out.println("  → notify() 唤醒的是「任意一个」等待者，一次就一个。");
        System.out.println("  → 放了几件货、变更了几种条件，就得喊几声 —— 而真实业务里这个「几声」根本算不准，");
        System.out.println("     所以默认用 notifyAll()：多叫醒几个人只是多几次空转，少叫醒一个是永久的卡死。");

        // C3：信号不存储 —— 没人在等的时候 notify 出去，就彻底没了
        Object mon = new Object();
        synchronized (mon) {
            mon.notify();                     // 此刻没有任何等待者
        }
        long t = System.nanoTime();
        synchronized (mon) {
            mon.wait(200);                    // 期望「刚才那个 notify 还留着」→ 会等满 200 ms
        }
        System.out.printf("C3 没人在等时 notify() 一次，随后的 wait(200) 等了 %d ms 才超时返回%n",
                (System.nanoTime() - t) / 1_000_000);
        System.out.println("  → monitor 不是信号量，notify 不是一个会累加的计数。它只是「当场叫醒」这个动作。");
        System.out.println("  → 所以「先改条件再 wait」一定会丢 —— 条件与 wait 必须在同一把锁里检查。");
    }

    /** @return {取走件数, 剩余件数, 仍在 WAITING 的消费者数} */
    private static int[] runTwoItems(boolean useNotifyAll) throws Exception {
        final Object mon = new Object();
        final int[] items = {0};              // 初始为空，等消费者都睡下再放货
        final int[] taken = {0};
        final boolean[] closed = {false};

        Runnable consumer = () -> {
            synchronized (mon) {
                try {
                    while (items[0] == 0 && !closed[0]) {
                        mon.wait();
                    }
                    if (items[0] > 0) {
                        items[0]--;
                        taken[0]++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        Thread a = new Thread(consumer, "C-consumer1");
        Thread b = new Thread(consumer, "C-consumer2");
        a.start();
        b.start();
        sleep(150);                           // 两人都进入 wait()

        synchronized (mon) {
            items[0] = 2;                     // 关键：货必须在消费者睡着之后才放下
            if (useNotifyAll) {
                mon.notifyAll();
            } else {
                mon.notify();
            }
        }
        sleep(300);                           // 给被叫醒的人时间干活

        int waiting = 0;
        int got;
        int left;
        // 注意：join 必须放在离开 synchronized 之后 —— 消费者要重新拿到 mon 才能退出临界区，
        // 持锁 join 等于自己把自己卡住（第一版就写错过，见 README 踩坑）。
        synchronized (mon) {
            if (a.getState() == Thread.State.WAITING) waiting++;
            if (b.getState() == Thread.State.WAITING) waiting++;
            left = items[0];
            got = taken[0];
            closed[0] = true;
            mon.notifyAll();                  // 收尾，让剩下的线程退出
        }
        a.join(1000);
        b.join(1000);
        return new int[]{got, left, waiting};
    }

    // ==================================================================
    // D · wait() 释放锁，sleep() 不释放
    // ==================================================================

    private static void sectionD() throws Exception {
        System.out.println();
        System.out.println("========== D · wait() 会释放锁，sleep() 不会 ==========");

        long sleptWait = measureQueueTime(true);
        long waitedWait = measureQueueTime(false);
        System.out.printf("  持锁者 sleep(500): 另一个人排队 %3d ms 才进临界区%n", sleptWait);
        System.out.printf("  持锁者 wait(500) : 另一个人排队 %3d ms 才进临界区%n", waitedWait);
        System.out.println("  → 同是「停 500 ms」，一个把门锁着睡，一个把钥匙放桌上。");
        System.out.println("  → 所以「在 synchronized 里 sleep」是并发性能的头号杀手；要让出锁，只能靠 wait。");
    }

    /** @param useSleep true=sleep 500ms，false=wait 500ms；返回旁观者进临界区前的排队时长 */
    private static long measureQueueTime(boolean useSleep) throws Exception {
        final Object lock = new Object();
        Thread holder = new Thread(() -> {
            synchronized (lock) {
                try {
                    if (useSleep) {
                        Thread.sleep(500);
                    } else {
                        lock.wait(500);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "D-holder");
        holder.start();
        sleep(80);                            // 确保 holder 先进去

        final long[] waited = {0};
        Thread other = new Thread(() -> {
            long t = System.nanoTime();
            synchronized (lock) {
                waited[0] = (System.nanoTime() - t) / 1_000_000;
            }
        }, "D-other");
        other.start();
        other.join();
        holder.join();
        return waited[0];
    }

    // ==================================================================
    // E · 中断会打断 wait，并且把中断标志清掉
    // ==================================================================

    private static void sectionE() throws Exception {
        System.out.println();
        System.out.println("========== E · 中断打断 wait()：抛异常的同时清掉中断标志 ==========");

        Object lock = new Object();
        final boolean[] sawInterrupt = {false};
        final boolean[] stillInterrupted = {false};

        Thread t = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait();                    // 无超时，永久等待
                } catch (InterruptedException e) {
                    sawInterrupt[0] = true;
                    stillInterrupted[0] = Thread.currentThread().isInterrupted();
                }
            }
        }, "E-waiter");

        t.start();
        sleep(150);
        System.out.println("  E1 打断前 isInterrupted          = " + t.isInterrupted());
        t.interrupt();
        t.join(2000);
        System.out.println("  E2 抛了 InterruptedException     = " + sawInterrupt[0]);
        System.out.println("  E3 捕获后 isInterrupted 还是 true = " + stillInterrupted[0]);
        System.out.println("  → 抛 InterruptedException 时 JVM 会先清标志，所以捕获后必须自己再 interrupt() 一次，");
        System.out.println("     否则上层永远不知道「有人想让我停」。");
        System.out.println("  → 另外 wait(0) 是「永久等待」，不是「等 0 毫秒」—— 超时参数 0 表示无限。");
    }

    // ------------------------------------------------------------------
    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
