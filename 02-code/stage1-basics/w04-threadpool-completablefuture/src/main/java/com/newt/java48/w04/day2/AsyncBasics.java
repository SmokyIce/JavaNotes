package com.newt.java48.w04.day2;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * W04 Day 2 · A~E：CompletableFuture 到底在哪个线程上跑。
 *
 * 核心问题只有一个：<b>不传 executor 的 supplyAsync，用的是哪个池？</b>
 * 答案是 ForkJoinPool.commonPool() —— 全局唯一、JVM 里所有 parallelStream 共用、
 * 而且它的线程数 = CPU 核数 - 1。
 *
 * 于是有两个后果，今天都要量出来：
 *   1. 你在业务代码里 supplyAsync 一个阻塞任务（查库、调 RPC），占的是全 JVM 的公共池；
 *   2. 池子只有 N-1 个线程，第 N 个阻塞任务只能排队 —— 这不是「慢一点」，是「排队」。
 *
 * 对照实验：同样一批阻塞任务丢进 newFixedThreadPool(n)，耗时差一个量级。
 */
public class AsyncBasics {

    private static void log(String s) {
        System.out.println("  " + s);
    }

    private static String tn() {
        return Thread.currentThread().getName();
    }

    public static void main(String[] args) throws Exception {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
        System.out.println();
        System.out.println("  一句话：不传 executor 的 *Async 方法，默认都在 commonPool 上跑；");
        System.out.println("  commonPool 的线程数 = 核数 - 1，且全 JVM 共享 —— 所以阻塞任务必须自带线程池。");
    }

    // ---------------------------------------------------------------- A
    private static void sectionA() throws Exception {
        System.out.println("== A. 不传 executor，任务落在哪个线程上？==");
        ForkJoinPool common = ForkJoinPool.commonPool();
        log("Runtime.getRuntime().availableProcessors() = " + Runtime.getRuntime().availableProcessors());
        log("ForkJoinPool.commonPool().getParallelism() = " + common.getParallelism());
        log("ForkJoinPool.commonPool().getPoolSize()     = " + common.getPoolSize()
                + "   （池子按需创建，刚启动时可能还是 0）");

        String name = CompletableFuture.supplyAsync(AsyncBasics::tn).join();
        log("supplyAsync 里看到的线程名 = " + name);

        // 30 个任务，看总共用到了几个不同的线程
        Set<String> used = ConcurrentHashMap.newKeySet();
        CompletableFuture<?>[] all = IntStream.range(0, 30)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    used.add(tn());
                    sleep(20);
                }))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(all).join();
        log("30 个任务用到的不同线程数 = " + used.size() + "   → " + used.stream().sorted().toList());
        log("→ 名字都是 ForkJoinPool.commonPool-worker-N，说明没有新建线程池，全在一个公共池里。");
        System.out.println();
    }

    // ---------------------------------------------------------------- B
    private static void sectionB() throws Exception {
        System.out.println("== B. 阻塞任务塞进 commonPool：不是变慢，是排队 ==");
        int n = ForkJoinPool.commonPool().getParallelism() + 3;
        long block = 300;

        long t0 = System.nanoTime();
        CompletableFuture<?>[] blocking = IntStream.range(0, n)
                .mapToObj(i -> CompletableFuture.runAsync(() -> sleep(block)))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(blocking).join();
        long commonMs = (System.nanoTime() - t0) / 1_000_000;

        ExecutorService owned = Executors.newFixedThreadPool(n, namedFactory("biz-"));
        long t1 = System.nanoTime();
        CompletableFuture<?>[] blocking2 = IntStream.range(0, n)
                .mapToObj(i -> CompletableFuture.runAsync(() -> sleep(block), owned))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(blocking2).join();
        long ownedMs = (System.nanoTime() - t1) / 1_000_000;
        owned.shutdown();
        owned.awaitTermination(2, TimeUnit.SECONDS);

        log(n + " 个任务，每个阻塞 " + block + " ms");
        log("commonPool（并行度 " + ForkJoinPool.commonPool().getParallelism() + "）耗时 = " + commonMs
                + " ms  ≈ 分 " + (int) Math.ceil((double) n / ForkJoinPool.commonPool().getParallelism())
                + " 批 × " + block + " ms");
        log("自建 fixed(" + n + ")                    耗时 = " + ownedMs + " ms");
        log("→ 公共池只有那么几个线程，多出来的任务只能排队等，不是被并行执行。");
        log("→ 生产事故的典型形状：业务里 supplyAsync 调 RPC，把 commonPool 占满，");
        log("  同 JVM 里无关的 parallelStream 全部卡住 —— 它们抢的是同一个池。");
        System.out.println();
    }

    // ---------------------------------------------------------------- C
    private static void sectionC() throws Exception {
        System.out.println("== C. runAsync vs supplyAsync：一个没有返回，一个异常也照样躺着 ==");
        CompletableFuture<Void> run = CompletableFuture.runAsync(() -> log("runAsync 跑完了，返回 CompletableFuture<Void>"));
        run.join();
        log("run.join() 的返回值 = " + run.join() + "   （Void 就是 null）");

        CompletableFuture<Integer> supply = CompletableFuture.supplyAsync(() -> 40 + 2);
        log("supplyAsync 结果 = " + supply.join() + "，类型 CompletableFuture<Integer>");

        CompletableFuture<Integer> boom = CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("下游服务 500");
        });
        // 注意：异常不会自己冒出来，必须有人 join/get
        Thread.sleep(100);
        log("提交了一个一定抛异常的任务，100 ms 后控制台什么都没有 —— 异常躺在 CF 里");
        try {
            boom.join();
        } catch (Exception e) {
            log("join() 才抛：" + e.getClass().getSimpleName() + " / cause = " + e.getCause());
        }
        log("→ 这就是 Day 1 T4「submit 吞异常」的同一件事，只是换了个壳。");
        System.out.println();
    }

    // ---------------------------------------------------------------- D
    private static void sectionD() throws Exception {
        System.out.println("== D. 传自己的 executor：线程名能读懂，池子能隔离 ==");
        AtomicInteger seq = new AtomicInteger();
        ExecutorService order = Executors.newFixedThreadPool(4, namedFactory("order-"));
        ExecutorService stock = Executors.newFixedThreadPool(4, namedFactory("stock-"));

        CompletableFuture<String> f = CompletableFuture
                .supplyAsync(() -> {
                    log("查订单，线程 = " + tn());
                    return "ORDER-9527";
                }, order)
                .thenApplyAsync(o -> {
                    log("查库存，线程 = " + tn() + "（换了池子）");
                    return o + " / 有货";
                }, stock);

        log("结果 = " + f.join());
        order.shutdown();
        stock.shutdown();
        order.awaitTermination(2, TimeUnit.SECONDS);
        stock.awaitTermination(2, TimeUnit.SECONDS);
        log("→ 线上排查「这个异步任务跑在哪」全靠线程名，默认的 pool-N-thread-M 什么也看不出来。");
        System.out.println();
    }

    // ---------------------------------------------------------------- E
    private static void sectionE() throws Exception {
        System.out.println("== E. 不一定非得异步起：三种现成的 CompletableFuture ==");
        CompletableFuture<String> done = CompletableFuture.completedFuture("缓存命中");
        log("completedFuture.join()      = " + done.join()
                + "   isDone=" + done.isDone() + "（不占线程，直接就是完成态）");

        CompletableFuture<String> failed = CompletableFuture.failedFuture(new RuntimeException("熔断中"));
        log("failedFuture.isCompletedExceptionally() = " + failed.isCompletedExceptionally());

        CompletableFuture<String> manual = new CompletableFuture<>();
        Thread t = new Thread(() -> {
            sleep(150);
            manual.complete("回调线程塞进来的结果");
        }, "callback-thread");
        t.start();
        log("手动 complete 之前 isDone = false，join 会阻塞……");
        log("join() = " + manual.join() + "（由 " + t.getName() + " 完成）");

        CompletableFuture<String> late = new CompletableFuture<>();
        boolean won = late.complete("第一次");
        boolean again = late.complete("第二次");
        log("complete 两次：第一次 " + won + "，第二次 " + again + "，值仍是 " + late.join()
                + "（CAS 语义，只有一个赢家）");
        System.out.println();
    }

    // ---------------------------------------------------------------- 工具
    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, prefix + n.incrementAndGet());
            t.setDaemon(false);
            return t;
        };
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
