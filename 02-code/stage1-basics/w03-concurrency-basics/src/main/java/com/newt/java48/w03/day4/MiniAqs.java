package com.newt.java48.w03.day4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * W03 Day 4 · 手写 80 行简易 AQS：state + CLH 队列 + park/unpark
 *
 * <p>这里回答两个「背了模板也讲不清」的问题：
 *
 * <p><b>一、为什么入队要 CAS 两次？</b>
 * <pre>
 *   第一次：队列还不存在时，CAS 建出哨兵 head（惰性初始化）
 *   第二次：CAS 把 tail 从旧尾换成自己（真正入队）
 * </pre>
 * 两次都不能省，因为它们各自对应一个「要么完全成功、要么完全没发生」的状态转移：
 * 哨兵建重了就有两个头，tail 抢输了就会覆盖掉别人的入队。实测次数在 D 段。
 *
 * <p><b>二、为什么 park 之后还要再检查一次？</b>
 * 因为 <i>park 返回 ≠ 拿到锁</i>。锁可能已经被别人抢走了（本实现有快路径，是非公平的），
 * 也可能是中断/虚假唤醒把线程叫起来的。所以 park 必须包在 for(;;) 里，醒来第一件事是重试 tryAcquire。
 * D 段会把「醒了但没抢到、又睡回去」的次数打出来 —— 大于 0 就说明重查不是多余的。
 *
 * <p>顺带一个容易忽略的点：<b>unpark 有「许可」语义</b>。先 unpark 后 park 不会丢信号，
 * 后到的 park 会立刻返回。所以这里不用像 wait/notify 那样担心「叫早了」。
 *
 * 运行：./run.sh day4.MiniAqs       （约 7 秒）
 */
public class MiniAqs {

    // ==================================================================
    // 一、简易 AQS 本体
    // ==================================================================

    /** CLH 队列节点：双向链表，thread 为 null 的是哨兵（head） */
    static final class Node {
        volatile Node prev;
        volatile Node next;
        volatile Thread thread;

        Node(Thread thread) {
            this.thread = thread;
        }
    }

    static final class MiniLock {

        /** 用 CAS 改 volatile 字段 —— AQS 内部是 Unsafe，这里换成公开 API，语义一样 */
        private static final AtomicIntegerFieldUpdater<MiniLock> STATE =
                AtomicIntegerFieldUpdater.newUpdater(MiniLock.class, "state");

        /** 挂起前去碰运气的次数。AQS 的 shouldParkAfterFailedAcquire 第一次返回 false，也多给一次机会 */
        private static final int SPIN = 4;

        /** 0 = 没人持有；>0 = 重入次数。AQS 里这玩意就叫 state */
        private volatile int state;
        private volatile Thread owner;
        private final AtomicReference<Node> head = new AtomicReference<>();
        private final AtomicReference<Node> tail = new AtomicReference<>();

        // ---- 只用于统计，不参与逻辑 ----
        final AtomicInteger casHeadCount = new AtomicInteger();
        final AtomicInteger casTailCount = new AtomicInteger();
        final AtomicInteger parkCount = new AtomicInteger();
        final AtomicInteger wakeButLoseCount = new AtomicInteger();

        // --------------------------------------------------------------
        // 抢锁：快路径 → 入队 → 自旋 → 挂起
        // --------------------------------------------------------------
        void lock() {
            if (tryAcquire(1)) {
                return;                              // 快路径：没人持有直接拿下
            }

            Node node = enq();                       // 慢路径：先入队
            boolean woke = false;
            for (;;) {
                Node p = node.prev;
                // 只有「前驱是 head」的节点才有资格去抢 —— 这是队列纪律
                if (p == head.get() && tryAcquire(1)) {
                    setHead(node);                   // 我成了新哨兵，把自己从队列里摘出来
                    return;
                }
                if (p == head.get()) {
                    for (int i = 0; i < SPIN; i++) {
                        if (tryAcquire(1)) {
                            setHead(node);
                            return;
                        }
                        Thread.onSpinWait();         // 挂起要走内核，比转几圈贵几个数量级
                    }
                }
                if (woke) {
                    // 上一轮 park 返回了，这一轮还是没抢到 → 醒来 ≠ 拿到锁
                    wakeButLoseCount.incrementAndGet();
                }
                parkCount.incrementAndGet();
                LockSupport.park(this);
                woke = true;                         // park 返回后必须回到循环顶部重试
            }
        }

        boolean tryAcquire(int arg) {
            int c = state;
            if (c == 0) {
                if (STATE.compareAndSet(this, 0, arg)) {
                    owner = Thread.currentThread();
                    return true;
                }
                return false;                        // CAS 输了：有人同一瞬间抢走了
            }
            if (owner == Thread.currentThread()) {
                state = c + arg;                     // 重入：自己在临界区里，不需要 CAS
                return true;
            }
            return false;
        }

        void unlock() {
            if (owner != Thread.currentThread()) {
                throw new IllegalMonitorStateException("current thread is not owner");
            }
            if (state > 1) {
                state = state - 1;                   // 重入只减计数，不真放锁
                return;
            }
            // 顺序不能反：先清 owner，再把 state 置 0。
            // 反过来的话，别人可能看到 state==0 抢到锁，然后被我们这行 owner=null 把归属擦掉。
            owner = null;
            state = 0;

            Node h = head.get();
            if (h != null) {
                Node s = h.next;
                if (s != null && s.thread != null) {
                    LockSupport.unpark(s.thread);    // 只叫醒队首，靠「释放者→下一个」接力传递
                }
            }
        }

        int state() {
            return state;
        }

        // --------------------------------------------------------------
        // 入队：这里就是「两次 CAS」
        // --------------------------------------------------------------
        private Node enq() {
            Node node = new Node(Thread.currentThread());
            for (;;) {
                Node t = tail.get();
                if (t == null) {
                    // 第一次 CAS：队列还不存在，先造一个哨兵当 head，再把 tail 指过去
                    if (head.compareAndSet(null, new Node(null))) {
                        casHeadCount.incrementAndGet();
                        tail.compareAndSet(null, head.get());
                    }
                    // CAS 输了说明别人建好了，下一轮 t 就不是 null
                } else {
                    node.prev = t;                   // 先挂前驱，再抢尾
                    if (tail.compareAndSet(t, node)) {
                        casTailCount.incrementAndGet();
                        t.next = node;               // 抢到尾之后才补前驱的 next
                        return node;
                    }
                    // 抢尾输了：有人排在我前面，重来
                }
            }
        }

        /** 自己成为哨兵：从队列里摘出来，thread 域不再需要 */
        private void setHead(Node node) {
            head.set(node);
            node.prev = null;
            node.thread = null;
        }
    }

    // ==================================================================
    // 二、压测脚手架（三种锁共用，保证可比）
    // ==================================================================

    static final int THREADS = 10;

    /** 被保护的计数器：故意用最普通的 int，不加任何修饰 */
    private static int counter;

    /** 临界区里的活 */
    interface Op {
        int apply(int v);
    }

    /** 轻活：一条 iadd，约 1 ns */
    private static final Op INC = v -> v + 1;

    /** 重活的工作量 */
    private static final int WORK = 1500;

    /** 重活：WORK 次乘加，约 0.5~1 µs */
    private static final Op HEAVY = v -> {
        int s = v;
        for (int i = 0; i < WORK; i++) {
            s = s * 31 + i;
        }
        return s;
    };

    public static void main(String[] args) throws Exception {
        System.out.println("JDK " + System.getProperty("java.version")
                + " / " + Runtime.getRuntime().availableProcessors() + " 核");

        sectionA();
        sectionB();
        warmup();          // 不做这一步，先跑的那个实现会背下全部 JIT 预热成本
        sectionC();
        sectionD();
        System.out.println("\n全部段落跑完。");
    }

    // ==================================================================
    // A · 正确性对拍：简易 AQS 保护下的 count++ 一次都不能丢
    // ==================================================================

    private static void sectionA() throws Exception {
        System.out.println("\n========== A · 对拍：10 线程 200 万次自增，一次都不能丢 ==========");
        final int per = 200_000;
        final int expected = THREADS * per;

        MiniLock lock = new MiniLock();
        counter = 0;
        long t = System.nanoTime();
        runWith(lock, per);
        long ms = (System.nanoTime() - t) / 1_000_000;
        System.out.printf("  用简易 AQS 上锁：期望 %d，实际 %d，丢 %d，耗时 %d ms%n",
                expected, counter, expected - counter, ms);

        counter = 0;
        long t0 = System.nanoTime();
        runWith(null, per);
        long ms0 = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("  对照（不锁）  ：期望 %d，实际 %d，丢 %d，耗时 %d ms%n",
                expected, counter, expected - counter, ms0);
        System.out.println("  → lock()/unlock() 前后各有内存屏障，所以临界区里的普通 int 也是安全的。");
    }

    /** @param lock null 表示不上锁 */
    private static void runWith(MiniLock lock, int perThread) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < perThread; j++) {
                        if (lock != null) {
                            lock.lock();
                            try {
                                counter++;
                            } finally {
                                lock.unlock();
                            }
                        } else {
                            counter++;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "a-" + i).start();
        }
        start.countDown();
        done.await();
    }

    // ==================================================================
    // B · 重入：state 就是重入计数
    // ==================================================================

    private static void sectionB() {
        System.out.println("\n========== B · 重入：state 就是重入次数 ==========");

        MiniLock lock = new MiniLock();
        lock.lock();
        System.out.println("  lock() 一次            → state = " + lock.state());
        lock.lock();
        lock.lock();
        System.out.println("  再 lock() 两次         → state = " + lock.state() + "（同一个线程，计数累加）");
        lock.unlock();
        lock.unlock();
        System.out.println("  unlock() 两次          → state = " + lock.state() + "（还剩一层，锁没真放）");
        lock.unlock();
        System.out.println("  unlock() 第三次        → state = " + lock.state() + "（这时才真的放锁）");

        try {
            lock.unlock();
        } catch (IllegalMonitorStateException e) {
            System.out.println("  再多 unlock() 一次     → IllegalMonitorStateException: " + e.getMessage());
        }

        System.out.println("  → 这就是为什么必须 try/finally：重入计数少减一次，锁就永远放不掉。");
    }

    // ==================================================================
    // C · 三种锁 × 两种临界区长度
    // ==================================================================

    private static void sectionC() throws Exception {
        System.out.println("\n========== C · 三种锁，两种临界区长度（同一套脚手架） ==========");

        int perLight = 200_000;      // 10 × 20 万 = 200 万次
        int perHeavy = 5_000;        // 10 × 5 千 = 5 万次（每次都干 1500 次乘加）

        long sLight = bestOf(() -> benchSync(INC, perLight));
        long rLight = bestOf(() -> benchReentrant(INC, perLight));
        long mLight = bestOf(() -> benchMini(INC, perLight));

        long sHeavy = bestOf(() -> benchSync(HEAVY, perHeavy));
        long rHeavy = bestOf(() -> benchReentrant(HEAVY, perHeavy));
        long mHeavy = bestOf(() -> benchMini(HEAVY, perHeavy));

        System.out.printf("  %-24s %10s %14s %12s %12s%n",
                "临界区", "synchronized", "ReentrantLock", "简易 AQS", "简易÷内置");
        System.out.printf("  %-22s %8d ms %12d ms %10d ms %10.1f 倍%n",
                "≈1 ns（200 万次）", sLight, rLight, mLight, mLight / (double) Math.max(1, sLight));
        System.out.printf("  %-20s %8d ms %12d ms %10d ms %10.2f 倍%n",
                "≈0.5µs（5 万次）", sHeavy, rHeavy, mHeavy, mHeavy / (double) Math.max(1, sHeavy));

        System.out.println("  → 临界区一拉长，5~6 倍缩到 1 倍附近：park 的代价是固定的几微秒，");
        System.out.println("     临界区越长摊得越薄；而临界区只有 1 ns 时，谁少挂起一次谁赢 —— 这正是 ReentrantLock 猛自旋的原因。");
        System.out.println("  → 所以「手写版慢 5 倍多」不是队列写错了，是这个负载在考「自旋 vs 挂起」的取舍。");
        System.out.println("  → 数字只用于横向比较（三轮取最小），不是 JMH 基准。");
    }

    private static void warmup() throws Exception {
        for (int i = 0; i < 3; i++) {
            benchSync(INC, 50_000);
            benchReentrant(INC, 50_000);
            benchMini(INC, 50_000);
        }
    }

    interface Bench {
        long run() throws Exception;
    }

    /** 三轮取最小，防止 JIT 预热与单次抖动污染结论 */
    private static long bestOf(Bench b) throws Exception {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            best = Math.min(best, b.run());
        }
        return best;
    }

    interface Ctx {
        void run(Op op);
    }

    private static long benchSync(Op op, int perThread) throws Exception {
        final Object mon = new Object();
        return hammer(perThread, body -> {
            synchronized (mon) {
                counter = body.apply(counter);
            }
        }, op);
    }

    private static long benchReentrant(Op op, int perThread) throws Exception {
        ReentrantLock lock = new ReentrantLock();
        return hammer(perThread, body -> {
            lock.lock();
            try {
                counter = body.apply(counter);
            } finally {
                lock.unlock();
            }
        }, op);
    }

    private static long benchMini(Op op, int perThread) throws Exception {
        MiniLock lock = new MiniLock();
        return hammer(perThread, body -> {
            lock.lock();
            try {
                counter = body.apply(counter);
            } finally {
                lock.unlock();
            }
        }, op);
    }

    /** 统一脚手架：10 线程同时起跑，每线程 perThread 次，返回 ms */
    private static long hammer(int perThread, Ctx ctx, Op op) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        counter = 0;
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < perThread; j++) {
                        ctx.run(op);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "h-" + i).start();
        }
        long t = System.nanoTime();
        start.countDown();
        done.await();
        return (System.nanoTime() - t) / 1_000_000;
    }

    // ==================================================================
    // D · 两个 CAS、park 的次数
    // ==================================================================

    private static void sectionD() throws Exception {
        System.out.println("\n========== D · 队列的真实开销：两次 CAS 与 park ==========");

        final int per = 200_000;
        final long total = (long) THREADS * per;

        MiniLock lock = new MiniLock();
        counter = 0;
        long t = System.nanoTime();
        runWith(lock, per);
        long ms = (System.nanoTime() - t) / 1_000_000;

        System.out.printf("  本轮耗时 %d ms%n", ms);
        System.out.printf("  CAS 建哨兵（head）%d 次 —— 队列惰性初始化，整个进程只建一次%n", lock.casHeadCount.get());
        System.out.printf("  CAS 抢尾（tail）  %d 次 —— 平均每 %.0f 次加锁 1 次%n",
                lock.casTailCount.get(), total / (double) Math.max(1, lock.casTailCount.get()));
        System.out.printf("  park 挂起         %d 次 —— 平均每 %.0f 次加锁挂起一次%n",
                lock.parkCount.get(), total / (double) Math.max(1, lock.parkCount.get()));
        System.out.printf("  醒来但没抢到      %d 次%n", lock.wakeButLoseCount.get());
        if (lock.parkCount.get() > 0) {
            System.out.printf("  → 总耗时 ÷ 挂起次数 = %.1f µs/次：这就是「挂起比自旋贵几个数量级」的实数%n",
                    ms * 1000.0 / lock.parkCount.get());
        }
        System.out.println("  → 入队两次 CAS 的必要性：哨兵 1 次 + 每次真正入队 1 次。");
        System.out.println("     两个状态转移各自必须原子，合并成一个 CAS 做不到（要同时改 head 和 tail）。");
        System.out.println("  → 「醒来没抢到」> 0，就是 park 之后必须重试的实测证据：这些线程被 unpark 了，");
        System.out.println("     但锁已经被别人拿走 —— 最常见的是「刚 unlock 的那个线程转头又 lock」，");
        System.out.println("     在压测循环里它就是最凶的插队者。不重查就会带着「我以为我有锁」继续跑，临界区直接失效。");
    }
}
