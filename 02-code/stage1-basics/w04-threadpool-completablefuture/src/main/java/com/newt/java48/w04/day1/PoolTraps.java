package com.newt.java48.w04.day1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * W04 Day 1 · Executors 四个工厂方法为什么被阿里手册点名禁用。
 *
 * T1 newFixedThreadPool：无界 LinkedBlockingQueue → 队列就是 OOM 的蓄水池
 * T2 newSingleThreadExecutor：一个慢任务堵死全部（它也是无界队列 + 1 个线程）
 * T3 newCachedThreadPool：maximumPoolSize = Integer.MAX_VALUE，任务一多线程就爆
 * T4 submit 吞异常 / execute 不吞 —— 工程里最常见的一类「任务悄悄没跑」
 * T5 线程池复用线程 → ThreadLocal 会串味
 * T6 OOM 实测（要用 -Xmx64m 单独跑，见 README）
 */
public class PoolTraps {

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].startsWith("oom")) {
            oom(args[0]);
            return;
        }
        System.out.printf("== 环境：JVM 最大堆 %d MB ==%n%n", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        t1();
        t2();
        t3();
        t4();
        t5();
    }

    // ---------------------------------------------------------------- T1

    private static void t1() throws Exception {
        System.out.println("== T1. newFixedThreadPool(2) 提交 5 万个空任务 ==");
        ExecutorService p = Executors.newFixedThreadPool(2);
        int peakQueue = 0;
        long t0 = System.nanoTime();
        for (int i = 1; i <= 50_000; i++) {
            p.execute(() -> {
            });
            if (i % 2_000 == 0) {
                // getQueue() 只有在强转成 ThreadPoolExecutor 时才有 —— newFixedThreadPool 返回的就是它
                peakQueue = Math.max(peakQueue, ((ThreadPoolExecutor) p).getQueue().size());
            }
        }
        p.shutdown();
        p.awaitTermination(30, TimeUnit.SECONDS);
        System.out.printf("  队列峰值 %d 个任务，总耗时 %d ms，线程数始终 = 2%n",
                peakQueue, (System.nanoTime() - t0) / 1_000_000);
        System.out.println("  → 这个队列没有上界（LinkedBlockingQueue 默认容量 Integer.MAX_VALUE）。");
        System.out.println("    消费端一慢，任务全堆在队列里，堆就这么被吃完了 —— 而且 max 参数完全没参与。");
        System.out.println();
    }

    // ---------------------------------------------------------------- T2

    private static void t2() throws Exception {
        System.out.println("== T2. newSingleThreadExecutor：一个慢任务堵死后面所有人 ==");
        ExecutorService p = Executors.newSingleThreadExecutor();
        p.execute(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        long t0 = System.nanoTime();
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            fs.add(p.submit(() -> {
            }));
        }
        for (Future<?> f : fs) {
            f.get();
        }
        long cost = (System.nanoTime() - t0) / 1_000_000;
        p.shutdown();
        p.awaitTermination(5, TimeUnit.SECONDS);
        System.out.printf("  3 个「瞬间就能跑完」的任务，从提交到全部完成花了 %d ms%n", cost);
        System.out.println("  → 它们没在等自己，在等前面那个 sleep(500)。单线程池没有并发，只有排队。");
        System.out.println();
    }

    // ---------------------------------------------------------------- T3

    private static void t3() throws Exception {
        System.out.println("== T3. newCachedThreadPool：线程数没有上限 ==");
        ExecutorService raw = Executors.newCachedThreadPool();
        ThreadPoolExecutor p = (ThreadPoolExecutor) raw;
        int peak = 0;
        for (int i = 0; i < 500; i++) {
            p.execute(() -> {
                try {
                    Thread.sleep(60);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            peak = Math.max(peak, p.getPoolSize());
        }
        System.out.printf("  500 个 60 ms 的任务 → 峰值线程数 %d（maximumPoolSize=%d）%n",
                peak, p.getMaximumPoolSize());
        p.shutdown();
        p.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("  → 每个任务一个线程，因为 SynchronousQueue 不存任务，只能现开一个线程来接。");
        System.out.println("    60 秒 keepAlive 之内这些线程全活着。低峰期看不出来，活动一来就 thread 暴涨 / CPU 打满。");
        System.out.println();
    }

    // ---------------------------------------------------------------- T4

    private static void t4() throws Exception {
        System.out.println("== T4. submit 吞异常，execute 不吞 —— 这条最容易在生产上踩 ==");

        ThreadPoolExecutor peek = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10));
        AtomicInteger boom = new AtomicInteger();

        System.out.println("  [execute] 任务里直接抛异常 → 异常被打印到控制台（下面那行就是）");
        peek.execute(() -> {
            throw new IllegalStateException("execute 里的异常：炸了 " + boom.incrementAndGet());
        });
        Thread.sleep(200);
        peek.execute(() -> System.out.println("  [execute] 下一个任务的线程名 = "
                + Thread.currentThread().getName() + " ← 旧线程已经死了，这是新补的"));

        Thread.sleep(200);
        System.out.println("  [submit ] 同样的异常，不调用 get() → 控制台什么都不会打印");
        Future<?> f = peek.submit(() -> {
            throw new IllegalStateException("submit 里的异常：也炸了");
        });
        Thread.sleep(300);
        System.out.println("  [submit ] 上面这行之后没有任何堆栈 —— 异常被存在 Future 里了");
        try {
            f.get();
        } catch (ExecutionException e) {
            System.out.println("  [submit ] get() 才拿到：ExecutionException(cause="
                    + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage() + ")");
        }
        peek.shutdown();
        peek.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  → 线程池里「任务没跑 / 跑了没生效」，第一步先查 Future 有没有人 get。");
        System.out.println();
    }

    // ---------------------------------------------------------------- T5

    private static final ThreadLocal<String> CTX = new ThreadLocal<>();

    private static void t5() throws Exception {
        System.out.println("== T5. 线程池复用线程 → ThreadLocal 串味 ==");
        // 必须是「同一个线程」才能演示串味 —— 第一版用了 fixed(2)，两个请求落到不同线程上，
        // 第二个读到 null，「残留」根本没发生。这就是「线程池里的实验也会被调度骗」。
        ExecutorService p = Executors.newFixedThreadPool(1);

        // 第一个「请求」：线程池无理由不清理 ThreadLocal
        p.submit(() -> {
            CTX.set("用户A的登录信息");
            System.out.println("  请求A[" + Thread.currentThread().getName() + "] 设置后读到: " + CTX.get());
            // 故意不 remove()
        }).get();

        // 第二个「请求」，正常情况下应该什么都读不到
        p.submit(() -> {
            System.out.println("  请求B[" + Thread.currentThread().getName() + "] 读到        : "
                    + CTX.get() + "  ← 上一个请求的残留");
            CTX.remove();
            System.out.println("  请求B[" + Thread.currentThread().getName() + "] remove 之后 : " + CTX.get());
        }).get();

        p.shutdown();
        p.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  → 线程是复用的，ThreadLocal 是挂在线程上的，不 remove 就跟着线程活到下一次任务。");
        System.out.println("    所以「用完必须 remove」不是洁癖，是正确性要求（尤其是租户 ID / 用户身份这类上下文）。");
        System.out.println();
    }

    // ---------------------------------------------------------------- T6

    private static void oom(String mode) throws Exception {
        System.out.printf("== T6. OOM 实测（mode=%s，最大堆 %d MB）==%n",
                mode, Runtime.getRuntime().maxMemory() / 1024 / 1024);

        boolean bounded = mode.equals("oom-fixed");
        ThreadPoolExecutor p = bounded
                ? new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(50))
                : (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

        CountDownLatch gate = new CountDownLatch(1);
        int submitted = 0;
        int rejected = 0;
        Throwable boom = null;
        // 预留一块内存：OOM 之后连「打印一句话」都要分配对象，不先还回去，报错都报不出来
        // （第一版就是在这里栽的：控制台只有一句 UncaughtExceptionHandler 里的 OOM，连第几个都没记住）
        byte[] reserve = new byte[4 * 1024 * 1024];

        try {
            for (int i = 1; i <= 100_000; i++) {
                byte[] payload = new byte[256 * 1024];   // 每个任务自带 256 KB，跟着 Runnable 一起进队列
                final int id = i;
                try {
                    p.execute(() -> {
                        try {
                            gate.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        if (payload.length == 0 || id == 0) {
                            System.out.print("");
                        }
                    });
                    submitted++;
                } catch (java.util.concurrent.RejectedExecutionException e) {
                    rejected++;
                }
            }
        } catch (Throwable t) {
            reserve = null;
            boom = t;
            System.gc();
        }

        if (boom != null) {
            System.out.printf("  提交到第 %d 个任务时炸了: %s%n", submitted + 1, boom.getClass().getSimpleName());
        }
        System.out.printf("  结果：成功入池 %d 个 / 被拒绝 %d 个 / 队列里还压着 %d 个%n",
                submitted, rejected, p.getQueue().size());
        gate.countDown();
        p.shutdownNow();
        p.awaitTermination(5, TimeUnit.SECONDS);

        if (bounded) {
            System.out.println("  → 队列有界 + 默认 AbortPolicy：拒绝得很干脆，堆稳稳的。");
            System.out.println("    代价是调用方要处理拒绝（降级 / 重试 / 返回 429），这是「必须做的功课」。");
        } else {
            System.out.println("  → 无界队列：没有人拒绝，直到堆先撑不住。前面「一切正常」，最后一步全崩。");
            System.out.println("    这就是阿里手册那条「线程池不允许使用 Executors 去创建」的实测现场。");
        }
        System.exit(0);
    }
}
