package com.newt.java48.w04.day1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * W04 Day 1 · 全周最值钱的一个程序：任务到底先入队还是先开线程。
 *
 * 教科书答案（也是大多数人答错的那个）：核心线程满 → **先入队** → 队满才开非核心 → 都满才拒绝。
 *
 * 默认回答是「先开到最大线程数，再排队」—— 正好反了。
 * 这个顺序不看代码想不出来，跑一遍就再也忘不掉。
 *
 * 做法：每个任务都堵在同一道门上（gate），所以线程不会释放、状态不会变，
 * 提交一个就拍一张快照（poolSize / queueSize / 是否被拒），最后拼成一张表。
 */
public class PoolFlow {

    private record Row(int seq, int poolSize, int queueSize, boolean rejected) {
    }

    private record Result(String name, int core, int max, String queueDesc, int submit,
                          int executed, int rejected, int peakPool, int restQueue, List<Row> rows) {
    }

    private static Result run(String name, int core, int max, BlockingQueue<Runnable> q, int submit)
            throws InterruptedException {
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(core, max, 60, TimeUnit.SECONDS, q);
        CountDownLatch gate = new CountDownLatch(1);
        List<Row> rows = new ArrayList<>();

        for (int i = 1; i <= submit; i++) {
            try {
                tpe.execute(() -> {
                    try {
                        gate.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                rows.add(new Row(i, tpe.getPoolSize(), tpe.getQueue().size(), false));
            } catch (RejectedExecutionException e) {
                rows.add(new Row(i, tpe.getPoolSize(), tpe.getQueue().size(), true));
            }
        }

        int peak = tpe.getPoolSize();
        int restQueue = tpe.getQueue().size();
        int executed = submit - (int) rows.stream().filter(Row::rejected).count();

        gate.countDown();
        tpe.shutdown();
        tpe.awaitTermination(5, TimeUnit.SECONDS);

        return new Result(name, core, max, describe(q),
                submit, executed, submit - executed, peak, restQueue, rows);
    }

    private static String describe(BlockingQueue<Runnable> q) {
        if (q instanceof ArrayBlockingQueue<?> ab) {
            return "Array(" + (ab.remainingCapacity() + ab.size()) + ")";
        }
        if (q instanceof LinkedBlockingQueue<?> lb) {
            return lb.remainingCapacity() == Integer.MAX_VALUE
                    ? "Linked(无界)" : "Linked(" + (lb.remainingCapacity() + lb.size()) + ")";
        }
        return q.getClass().getSimpleName();
    }

    private static void print(Result r) {
        System.out.printf("%n【%s】core=%d max=%d queue=%s，连提 %d 个任务%n",
                r.name(), r.core(), r.max(), r.queueDesc(), r.submit());
        StringBuilder head = new StringBuilder("  序号  ");
        StringBuilder ps = new StringBuilder("  线程数");
        StringBuilder qs = new StringBuilder("  队列  ");
        StringBuilder st = new StringBuilder("  结果  ");
        for (Row row : r.rows()) {
            head.append(String.format("%6d", row.seq()));
            ps.append(String.format("%6d", row.poolSize()));
            qs.append(String.format("%6d", row.queueSize()));
            st.append(String.format("%6s", row.rejected() ? "REJ" : "ok"));
        }
        System.out.println(head);
        System.out.println(ps);
        System.out.println(qs);
        System.out.println(st);
        System.out.printf("  → 实际执行 %d 个 / 拒绝 %d 个 / 峰值线程 %d / 结束时队列里还剩 %d 个%n",
                r.executed(), r.rejected(), r.peakPool(), r.restQueue());
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== 任务在四步里怎么走：核心线程 → 队列 → 非核心线程 → 拒绝 ==");
        System.out.println("（每个任务都堵在同一道门上，所以状态能稳住；提交一个拍一张快照）");

        List<Result> all = new ArrayList<>();

        // S1 标准配置：最能看出四步顺序
        all.add(run("S1 标准", 2, 4, new ArrayBlockingQueue<>(2), 10));
        // S2 无界队列：max 变成摆设
        all.add(run("S2 无界", 2, 4, new LinkedBlockingQueue<>(), 10));
        // S3 core == max：没有非核心阶段
        all.add(run("S3 无核", 2, 2, new ArrayBlockingQueue<>(2), 10));
        // S4 core == 0：第一个任务会先进队，再借一个线程来消化
        all.add(run("S4 core0", 0, 4, new ArrayBlockingQueue<>(2), 10));
        // S5 SynchronousQueue：根本不排队
        all.add(run("S5 不排", 2, 4, new SynchronousQueue<>(), 10));

        all.forEach(PoolFlow::print);

        System.out.println();
        System.out.println("== 一张表看完 ==");
        System.out.println("  场景       core  max  queue             提交  执行  拒绝  峰值  剩余");
        for (Result r : all) {
            System.out.printf("  %-9s %4d %4d  %-15s %4d %5d %5d %5d %5d%n",
                    r.name(), r.core(), r.max(), r.queueDesc(), r.submit(),
                    r.executed(), r.rejected(), r.peakPool(), r.restQueue());
        }

        System.out.println();
        System.out.println("== 四条结论 ==");
        System.out.println("  1. S1 的线程数是这样长的：1→2（核心满）→2,2（任务进队列）→3→4（队满才开非核心）→拒绝。");
        System.out.println("     顺序是「核心 → 队列 → 非核心 → 拒绝」，不是「先开满线程再排队」。");
        System.out.println("  2. S2 用无界队列，max=4 一次都没生效（峰值线程恒为 2）。");
        System.out.println("     所以 newFixedThreadPool(n) 的 n 就是真实并发上限，max 参数形同虚设。");
        System.out.println("  3. S3 core=max，没有非核心阶段：队列一满就直接拒绝，线程数不会再多一个。");
        System.out.println("  4. S5 用 SynchronousQueue（容量 0），队列永远「满」，于是任务一个个直接开线程，");
        System.out.println("     开到 max 就拒绝 —— 这就是 newCachedThreadPool 的形态（它的 max 是 Integer.MAX_VALUE）。");
        System.out.println();
        System.out.println("  S4 那个 core=0 是特例：任务先进队，再借一个线程来消化（JDK 的补丁行为），");
        System.out.println("  所以它的第一格线程数可能是 1、队列可能是 0，取决于工作线程抢得有多快 —— 看结论就好。");
    }
}
