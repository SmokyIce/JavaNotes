package com.newt.java48.w03.day1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

/**
 * Day 1 · 竞态复现：为什么 <code>count++</code> 会丢（B / C 段）
 *
 * <pre>
 * B0  单线程基线        先证明「循环次数没写错」，2 万 × 10 一定等于 20 万
 * B1  每线程 2 万次      10 线程 × 10 轮 —— 注意它<b>不是每轮都丢</b>，原因在输出里
 * B2  每线程 50 万次     同一份代码只是加量 —— 这才能稳定复现
 * C1  volatile          能不能救 count++？   （不能，volatile 只管可见性）
 * C2  synchronized      能不能救？           （能；方法级 / 同步块两种写法各跑 5 轮）
 * C3  AtomicInteger     能不能救？           （能，CAS 循环）
 * C4  可见性对照         普通 boolean vs volatile boolean，自旋等标志位
 * </pre>
 *
 * <p>这一节的结论比 API 重要：<b>count++ 不是一步，是三步</b>（读 → 加 → 写）。
 * 丢更新发生在「两个线程都读到了同一个旧值」这个窗口里。
 *
 * <p>运行：{@code run.bat day1.RaceCondition}
 */
public class RaceCondition {

    private static final int THREADS = 10;
    private static final int ROUNDS = 10;
    private static final int SMALL = 20_000;      // 2 万
    private static final int BIG = 500_000;       // 50 万

    // ---- B / C 各自的计数器 ----
    private int plain;
    private volatile int vol;
    private int sync;
    private final AtomicInteger atomic = new AtomicInteger();

    // ---- C4 用 ----
    private boolean plainFlag;
    private volatile boolean volatileFlag;

    public static void main(String[] args) throws Exception {
        RaceCondition demo = new RaceCondition();
        demo.sectionB0();
        demo.sectionB1();
        demo.sectionB2();
        demo.sectionC1();
        demo.sectionC2();
        demo.sectionC3();
        demo.sectionC4();
        demo.summary();
    }

    // ================================================================== B0
    private void sectionB0() {
        System.out.println("========== B0 · 单线程基线：先证明循环次数没写错 ==========");
        int expected = THREADS * SMALL;
        for (int r = 1; r <= 3; r++) {
            int n = 0;
            for (int i = 0; i < expected; i++) {
                n++;
            }
            System.out.printf("  第 %d 轮：%d  %s%n", r, n, n == expected ? "✓ 刚好 200000" : "✗ 循环本身就有问题");
        }
        System.out.println("  → 基线干净。等一下少掉的那些，只能是并发造成的。");
        System.out.println();
    }

    // ================================================================== B1
    private void sectionB1() throws InterruptedException {
        System.out.println("========== B1 · 普通 int，每线程 2 万次（计划表里的原始数量）==========");
        runCase("B1", SMALL, ROUNDS, () -> plain = 0, this::incPlain, () -> plain);
        System.out.println("  看仔细：前几轮丢得很难看，后面几轮一次都没丢。");
        System.out.println("  不是代码变安全了 —— 是 JIT 把自增编译成机器码之后，2 万次只要几十微秒，");
        System.out.println("  10 个线程还没全被唤醒，第一个已经跑完了，压根没重叠。");
        System.out.println("  → 这就是「我本地跑了十遍都是对的」的物理原因。要复现，得加量。");
        System.out.println();
    }

    // ================================================================== B2
    private void sectionB2() throws InterruptedException {
        System.out.println("========== B2 · 同一份代码，每线程 50 万次 ==========");
        runCase("B2", BIG, ROUNDS, () -> plain = 0, this::incPlain, () -> plain);
        System.out.println("  单次循环够长，10 个线程必然重叠 —— 于是每一轮都丢。");
        System.out.println("  丢掉的量级（几百万分之几到十分之几）取决于调度，但「一定丢」是可以预期的。");
        System.out.println();
    }

    // ================================================================== C1
    private void sectionC1() throws InterruptedException {
        System.out.println("========== C1 · volatile 能不能救 count++？==========");
        runCase("C1", BIG, ROUNDS, () -> vol = 0, this::incVolatile, () -> vol);
        System.out.println("  volatile 保证「读到的是最新值」，但保证不了「读和写之间没人插队」。");
        System.out.println("  三步还是三步，只是每一步都去主存取而已 —— 该丢还是丢。");
        System.out.println();
    }

    // ================================================================== C2
    private void sectionC2() throws InterruptedException {
        System.out.println("========== C2 · synchronized 能不能救？（方法级 / 同步块，各 5 轮）==========");
        runCase("C2-方法级", BIG, 5, () -> sync = 0, this::incSync, () -> sync);
        runCase("C2-同步块", BIG, 5, () -> sync = 0, this::incSyncBlock, () -> sync);
        System.out.println("  能救。代价：每次自增都要抢一次锁（这里锁的是 this，换个人拿别的锁就失效）。");
        System.out.println("  注意 synchronized 顺手把可见性也解决了 —— 解锁前会把改动刷回主存。");
        System.out.println("  两种写法语义一样，但字节码不一样：");
        System.out.println("    方法级 -> 方法 flags 里多一个 ACC_SYNCHRONIZED，一条指令都没多；");
        System.out.println("    同步块 -> 多出 monitorenter / monitorexit（见 day1-bytecode.txt）。");
        System.out.println();
    }

    // ================================================================== C3
    private void sectionC3() throws InterruptedException {
        System.out.println("========== C3 · AtomicInteger 能不能救？==========");
        runCase("C3", BIG, ROUNDS, () -> atomic.set(0), () -> atomic.incrementAndGet(), atomic::get);
        System.out.println("  能救。incrementAndGet 是一条 CAS 循环：读旧值 → 算新值 → 比对再写，失败就重来。");
        System.out.println("  换来的好处只覆盖「这一个变量」，多个字段要一起原子，还是得靠锁。");
        System.out.println();
    }

    // ================================================================== C4
    private void sectionC4() throws InterruptedException {
        System.out.println("========== C4 · 可见性：volatile 真正管的那件事 ==========");
        System.out.println("  一个线程死等标志位，主线程确认它已经在转，再等 50ms 才置 true。");
        System.out.println("  先普通 boolean，再 volatile。自旋上限 20 亿次，防止真的挂死；两次都用守护线程收尾。");
        runFlagTest("普通 boolean", false);
        runFlagTest("volatile boolean", true);
        System.out.println("  说明：可见性问题在 x86 上本来就不容易复现 —— 没复现 ≠ 安全。");
        System.out.println("        「永远看不到」的物理来源通常是 JIT 把读操作提到循环外（编译期就把 field 读成了寄存器）。");
        System.out.println();
    }

    private void runFlagTest(String label, boolean useVolatile) throws InterruptedException {
        final long limit = 2_000_000_000L;
        final long[] spins = {0};
        final boolean[] hitByFlag = {false};
        CountDownLatch inLoop = new CountDownLatch(1);

        if (useVolatile) {
            volatileFlag = false;
        } else {
            plainFlag = false;
        }

        Thread reader = new Thread(() -> {
            long n = 0;
            inLoop.countDown();          // 告诉主线程「我马上进循环」
            if (useVolatile) {
                while (!volatileFlag && n < limit) {
                    n++;
                }
                hitByFlag[0] = n < limit;
            } else {
                while (!plainFlag && n < limit) {
                    n++;
                }
                hitByFlag[0] = n < limit;
            }
            spins[0] = n;
        }, "reader-" + (useVolatile ? "volatile" : "plain"));
        reader.setDaemon(true);

        long start = System.nanoTime();
        reader.start();
        inLoop.await();
        Thread.sleep(50);                // 确保它真的已经在自旋，而不是还没被调度
        if (useVolatile) {
            volatileFlag = true;
        } else {
            plainFlag = true;
        }
        reader.join(5000);
        long ms = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("  [%s] 自旋 %,d 次，%s，耗时 %d ms，线程还活着=%s%n",
                label, spins[0],
                hitByFlag[0] ? "在循环里看见了新值 ✓" : "✗ 一直到撞上 20 亿次上限都没看见",
                ms, reader.isAlive());
    }

    // ================================================================== 收尾
    private void summary() {
        System.out.println("========== 收尾 ==========");
        System.out.println("同一件事的三种说法，说的是同一件事：");
        System.out.println("  1. count++ 是 read-modify-write，三步之间随时可能被切走；");
        System.out.println("  2. 「原子性」和「可见性」是两个独立问题：volatile 只解决后者，synchronized 两个都解决；");
        System.out.println("  3. AtomicInteger 用 CAS 只解决「这一个变量」的原子性，多字段还是一个整体动作时仍要锁。");
    }

    // ================================================================== 被计数的四个方法
    // 单独拆成方法，是为了 javap -c 能直接看出差异（见 day1-bytecode.txt）

    private void incPlain() {
        plain++;
    }

    private void incVolatile() {
        vol++;
    }

    private synchronized void incSync() {
        sync++;
    }

    private void incSyncBlock() {
        synchronized (this) {
            sync++;
        }
    }

    // ================================================================== 压测骨架
    /**
     * 跑 rounds 轮，每轮 THREADS 个线程各做 perThread 次 step，打印每轮结果。
     */
    private void runCase(String tag, int perThread, int rounds, Runnable reset,
                         Runnable step, IntSupplier reader) throws InterruptedException {
        int expected = THREADS * perThread;
        int fail = 0;
        int min = Integer.MAX_VALUE;
        int max = 0;
        long start = System.nanoTime();

        for (int r = 1; r <= rounds; r++) {
            reset.run();
            Runnable body = () -> {
                for (int i = 0; i < perThread; i++) {
                    step.run();
                }
            };
            oneRound(body);
            int got = reader.getAsInt();
            if (got != expected) {
                fail++;
            }
            min = Math.min(min, got);
            max = Math.max(max, got);
            System.out.printf("  第 %2d 轮：%8d / %d  %s%n", r, got, expected,
                    got == expected ? "✓ 一个不少" : "✗ 丢了 " + (expected - got));
        }

        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("  [%s] 小结：%d/%d 轮丢更新，最低 %d（丢 %d，%.3f%%），最高 %d，总耗时 %d ms%n",
                tag, fail, rounds, min, expected - min, (expected - min) * 100.0 / expected, max, ms);
        System.out.println("  （这个耗时含 JIT 预热，别当基准数字用）");
    }

    /**
     * 开 THREADS 个线程，全部卡在 gate 上，然后一起放行。
     *
     * <p>不加 latch 会怎样：线程是一个个 start 的，第一个可能已经跑完了第十个才起来，
     * 撞不上就没有竞态 —— 这也是「本地复现不了」的第二个原因（第一个是 JIT 让循环太快）。
     */
    private void oneRound(Runnable perThreadTask) throws InterruptedException {
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                try {
                    gate.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    done.countDown();
                    return;
                }
                try {
                    perThreadTask.run();
                } finally {
                    done.countDown();
                }
            }, "w" + i).start();
        }
        gate.countDown();
        done.await();
    }
}
