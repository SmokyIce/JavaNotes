package com.newt.java48.w04.day1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * W04 Day 1 · A~E：new Thread 到底错在哪，以及七个参数各管什么。
 *
 * A 一万个任务：new Thread vs 固定线程池（耗时 + 峰值线程数）
 * B 默认线程名长什么样，为什么线上必须自己命名
 * C keepAliveTime / allowCoreThreadTimeOut：非核心线程什么时候死
 * D 四种拒绝策略，谁抛异常、谁静默丢、谁把活干在提交线程里
 * E shutdown vs shutdownNow：队列里的任务跑不跑、正在跑的中不中断
 */
public class PoolBasics {

    /** 每个任务的活：1000 次加法。够小，主要测「起线程」的开销，不是测算力。 */
    private static volatile long SINK;

    private static void tinyWork() {
        long s = 0;
        for (int i = 0; i < 1000; i++) {
            s += i * 31L;
        }
        SINK = s;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== 环境 ==");
        System.out.printf("CPU %d 核 / JVM 最大堆 %d MB / %s%n",
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().maxMemory() / 1024 / 1024,
                System.getProperty("os.name"));
        System.out.println();

        secA();
        secB();
        secC();
        secD();
        secE();
    }

    // ---------------------------------------------------------------- A

    private static void runNewThread(int n) throws InterruptedException {
        List<Thread> ts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Thread t = new Thread(PoolBasics::tinyWork);
            t.start();
            ts.add(t);
        }
        for (Thread t : ts) {
            t.join();
        }
    }

    private static void runPool(int n) throws InterruptedException {
        ExecutorService p = Executors.newFixedThreadPool(8);
        for (int i = 0; i < n; i++) {
            p.execute(PoolBasics::tinyWork);
        }
        p.shutdown();
        p.awaitTermination(60, TimeUnit.SECONDS);
    }

    private static void secA() throws Exception {
        System.out.println("== A. 一万个任务：new Thread 每个任务一个线程， vs 固定 8 线程池 ==");

        // 预热：先各跑 200 个，避免第一轮背 JIT 编译成本
        runNewThread(200);
        runPool(200);
        Thread.sleep(300);

        long t1 = System.nanoTime();
        runNewThread(10_000);
        long d1 = (System.nanoTime() - t1) / 1_000_000;

        long t2 = System.nanoTime();
        runPool(10_000);
        long d2 = (System.nanoTime() - t2) / 1_000_000;

        System.out.printf("  逐个 new Thread + join : %6d ms%n", d1);
        System.out.printf("  固定 8 线程池           : %6d ms%n", d2);
        if (d2 > 0) {
            System.out.printf("  → 差 %.1f 倍，全部差在「创建 / 销毁」上，不在「干活」上%n", d1 * 1.0 / d2);
        }
        System.out.println();
    }

    // ---------------------------------------------------------------- B

    private static void secB() throws Exception {
        System.out.println("== B. 线程名：默认名什么都看不出来，线上必须自己命名 ==");

        ExecutorService p1 = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 4; i++) {
            p1.execute(() -> System.out.println("  默认名 → " + Thread.currentThread().getName()));
        }
        p1.shutdown();
        p1.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  注：默认名里的 pool-3 是**全局线程池序号**（前面 A 段开过两个），跟业务毫无关系；");
        System.out.println("      后面那个 1/2 只是线程编号轮转 —— 从堆栈里根本看不出这是哪个业务的哪批任务。");

        AtomicInteger seq = new AtomicInteger(1);
        ThreadFactory named = r -> {
            Thread t = new Thread(r, "order-task-" + seq.getAndIncrement());
            t.setDaemon(false);
            return t;
        };
        ExecutorService p2 = Executors.newFixedThreadPool(2, named);
        for (int i = 0; i < 4; i++) {
            p2.execute(() -> System.out.println("  自命名 → " + Thread.currentThread().getName()));
        }
        p2.shutdown();
        p2.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println();
    }

    // ---------------------------------------------------------------- C

    private static void secC() throws Exception {
        System.out.println("== C. keepAliveTime：只有「非核心线程」会被回收 ==");

        ThreadPoolExecutor tpe = new ThreadPoolExecutor(
                2, 4, 300, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        System.out.printf("  配置 core=2 max=4 queue=1 keepAlive=300ms%n");

        // 4 个任务同时堵住 4 个线程 → 队列满（1）+ 非核心被拉起来
        CountDownLatch gate = new CountDownLatch(1);
        for (int i = 0; i < 4; i++) {
            tpe.execute(() -> {
                try {
                    gate.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        System.out.printf("  4 个任务堵住时: poolSize=%d queue=%d%n",
                tpe.getPoolSize(), tpe.getQueue().size());
        gate.countDown();

        long t0 = System.nanoTime();
        for (int ms : new int[]{0, 150, 400, 800}) {
            long wait = ms - (System.nanoTime() - t0) / 1_000_000;
            if (wait > 0) {
                Thread.sleep(wait);
            }
            System.out.printf("    +%3d ms  poolSize=%d%n", ms, tpe.getPoolSize());
        }
        tpe.shutdown();
        tpe.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("  —— 再开 allowCoreThreadTimeOut(true)，核心线程也会死 ——");
        ThreadPoolExecutor tpe2 = new ThreadPoolExecutor(
                2, 4, 300, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10));
        tpe2.allowCoreThreadTimeOut(true);
        tpe2.execute(PoolBasics::tinyWork);
        tpe2.execute(PoolBasics::tinyWork);
        Thread.sleep(100);
        System.out.printf("    刚跑完 2 个任务: poolSize=%d%n", tpe2.getPoolSize());
        Thread.sleep(500);
        System.out.printf("    再等 500 ms    : poolSize=%d  ← 全死光，线程池空了%n", tpe2.getPoolSize());
        tpe2.shutdown();
        tpe2.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println();
    }

    // ---------------------------------------------------------------- D

    private static void handlerDemo(String name, RejectedExecutionHandler h) throws Exception {
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), h);
        List<String> log = Collections.synchronizedList(new ArrayList<>());
        for (int i = 1; i <= 5; i++) {
            final int id = i;
            try {
                tpe.execute(() -> {
                    log.add("跑#" + id + "@" + Thread.currentThread().getName());
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (RejectedExecutionException e) {
                log.add("抛异常#" + id);
            }
        }
        tpe.shutdown();
        tpe.awaitTermination(5, TimeUnit.SECONDS);
        System.out.printf("  %-22s %s%n", name + ":", log);
    }

    private static void secD() throws Exception {
        System.out.println("== D. 四种拒绝策略：core=1 max=1 queue=1，连提 5 个任务 ==");
        handlerDemo("AbortPolicy(默认)", new ThreadPoolExecutor.AbortPolicy());
        handlerDemo("CallerRunsPolicy", new ThreadPoolExecutor.CallerRunsPolicy());
        handlerDemo("DiscardPolicy", new ThreadPoolExecutor.DiscardPolicy());
        handlerDemo("DiscardOldestPolicy", new ThreadPoolExecutor.DiscardOldestPolicy());
        System.out.println("  逐个看这 5 个任务的下场：");
        System.out.println("    Abort（默认）  → #3#4#5 抛 RejectedExecutionException，被我 catch 住了，所以线程池还能继续干活");
        System.out.println("    CallerRuns    → 一个都没丢，但 #3#5 是在 **main 线程**里跑的：提交线程被自己的任务拖住 = 天然背压");
        System.out.println("    Discard       → 只跑了 #1#2，#3#4#5 静默消失，**没有异常、没有日志**，最危险");
        System.out.println("    DiscardOldest → 队列里最老的 #2 被挤掉，接着 #3#4 也被后来的挤掉，最后只剩 #1 和 #5");
        System.out.println();
    }

    // ---------------------------------------------------------------- E

    private static void secE() throws Exception {
        System.out.println("== E. 关线程池：shutdown 收尾，shutdownNow 掀桌 ==");

        // E1 shutdown
        ThreadPoolExecutor t1 = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
        AtomicInteger done = new AtomicInteger();
        for (int i = 1; i <= 5; i++) {
            t1.execute(() -> {
                try {
                    Thread.sleep(50);
                    done.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        t1.shutdown();
        t1.awaitTermination(5, TimeUnit.SECONDS);
        System.out.printf("  shutdown()    : 5 个排队任务全部跑完 → 完成 %d 个%n", done.get());

        // E2 shutdownNow
        ThreadPoolExecutor t2 = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
        AtomicInteger done2 = new AtomicInteger();
        AtomicInteger interrupted = new AtomicInteger();
        for (int i = 1; i <= 5; i++) {
            t2.execute(() -> {
                try {
                    Thread.sleep(50);
                    done2.incrementAndGet();
                } catch (InterruptedException e) {
                    interrupted.incrementAndGet();
                }
            });
        }
        Thread.sleep(20);
        List<Runnable> neverRun = t2.shutdownNow();
        t2.awaitTermination(5, TimeUnit.SECONDS);
        System.out.printf("  shutdownNow() : 返回未执行 %d 个，跑完 %d 个，被中断 %d 个%n",
                neverRun.size(), done2.get(), interrupted.get());
        System.out.println();
    }
}
