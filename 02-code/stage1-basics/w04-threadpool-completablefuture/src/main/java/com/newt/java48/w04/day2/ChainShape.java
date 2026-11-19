package com.newt.java48.w04.day2;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * W04 Day 2 · A~F：thenApply 与 thenCompose 的形状差别。
 *
 * 这两者的区别和 W02 的 map / flatMap 完全同构：
 *   thenApply   : T -> R              结果 T 变成 R
 *   thenCompose : T -> CF<R>          结果拍平，不留嵌套
 *
 * 关键在于：<b>thenApply 里返回一个 CompletableFuture 是合法的</b>，
 * 编译器不报错、不警告，于是你会得到一个 CompletableFuture<CompletableFuture<String>>。
 * 它一直编译通过、一直到 join() 那一刻才暴露 —— 因为 join() 返回的是一个 Future 对象，
 * 打印出来是 java.util.concurrent.CompletableFuture@1b6d3586，而不是你要的字符串。
 *
 * 本文件的核心手法：把类型用「声明的类型 + 运行时 getClass」双向钉死，不靠嘴说。
 */
public class ChainShape {

    private static void log(String s) {
        System.out.println("  " + s);
    }

    /**
     * 这个方法签名本身就是证据：能把它传进去，说明前者真的是 CF&lt;CF&lt;String&gt;&gt;。
     */
    private static void expectNested(CompletableFuture<CompletableFuture<String>> f) {
        log("   （能赋给 CompletableFuture<CompletableFuture<String>> 形参 → 编译期类型就是嵌套的）");
    }

    public static void main(String[] args) throws Exception {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
        sectionF();
        System.out.println();
        System.out.println("  一句话：返回值是「值」用 thenApply，返回值是「另一个异步」用 thenCompose；");
        System.out.println("  分不清的时候看 join() 的返回类型 —— 拿到 CompletableFuture 就是套错了。");
    }

    // ---------------------------------------------------------------- A
    private static void sectionA() {
        System.out.println("== A. 同一个业务：查用户 → 查他的订单，两种写法两种类型 ==");

        // 错误写法：thenApply 里返回 CompletableFuture
        CompletableFuture<CompletableFuture<String>> nested = CompletableFuture
                .supplyAsync(() -> "用户:42")
                .thenApply(user -> fetchOrders(user));       // 返回的是 CF，于是套了一层

        log("thenApply 的静态类型 = CompletableFuture<CompletableFuture<String>>");
        log("nested.join() 的类型 = " + nested.join().getClass().getName() + "   ← 不是 String");
        // 先把内层 join 到底再打印：否则 toString 里是 [Not completed] 还是 [Completed normally]
        // 取决于内层任务跑没跑完 —— 那是另一个竞态，不能拿来当证据。
        String value = nested.join().join();
        log("nested.join().join() = " + value + "   ← 要 join 两次才拿到值");
        log("此时再看 nested.join() = " + nested.join() + "（外层已完成，打印才稳定）");
        expectNested(nested);

        // 正确写法：thenCompose 拍平
        CompletableFuture<String> flat = CompletableFuture
                .supplyAsync(() -> "用户:42")
                .thenCompose(ChainShape::fetchOrders);

        log("thenCompose 的静态类型 = CompletableFuture<String>");
        log("flat.join()          = " + flat.join());
        log("flat.join() 的类型    = " + flat.join().getClass().getName() + "   ← 一次 join 到位");
        System.out.println();
    }

    private static CompletableFuture<String> fetchOrders(String user) {
        return CompletableFuture.supplyAsync(() -> user + " 的订单共 3 笔");
    }

    // ---------------------------------------------------------------- B
    private static void sectionB() {
        System.out.println("== B. thenApply / thenApplyAsync / thenApply(executor)：在哪一层跑 ==");
        AtomicInteger n = new AtomicInteger();
        ExecutorService biz = Executors.newFixedThreadPool(2,
                r -> new Thread(r, "biz-" + n.incrementAndGet()));

        CompletableFuture<String> base = CompletableFuture.supplyAsync(() -> {
            log("上游 supplyAsync        线程 = " + tn());
            return "A";
        }, biz);

        String same = base.thenApply(s -> {
            log("thenApply（同步）        线程 = " + tn() + "  ← 上游已完成，于是直接在调用线程原地执行");
            return s + "B";
        }).join();

        String other = base.thenApplyAsync(s -> {
            log("thenApplyAsync（默认池）  线程 = " + tn() + "  ← 又回到 commonPool");
            return s + "C";
        }).join();

        String owned = base.thenApplyAsync(s -> {
            log("thenApplyAsync(biz)     线程 = " + tn() + "  ← 指定池子");
            return s + "D";
        }, biz).join();

        log("三个结果 = " + same + " / " + other + " / " + owned);
        biz.shutdown();
        try {
            biz.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log("→ 规则：不带 Async 的 thenXxx 在「完成上游的那个线程」上跑 —— 上游早已完成时，");
        log("  它就在调用的那个线程（这里是 main）上同步跑完，跟异步一点关系都没有。");
        System.out.println();
    }

    // ---------------------------------------------------------------- C
    private static void sectionC() {
        System.out.println("== C. thenCombine：两路并行，再合并（不是串行相加）==");
        long t0 = System.nanoTime();
        CompletableFuture<String> user = CompletableFuture.supplyAsync(() -> {
            sleep(400);
            return "用户A";
        });
        CompletableFuture<Integer> points = CompletableFuture.supplyAsync(() -> {
            sleep(400);
            return 1280;
        });
        String merged = user.thenCombine(points, (u, p) -> u + " 有 " + p + " 积分").join();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        log("两路各 400 ms，合并结果 = " + merged);
        log("总耗时 = " + ms + " ms  → 两路是并行的，不是 800 ms");

        // 串行写法对照
        long t1 = System.nanoTime();
        String a = CompletableFuture.supplyAsync(() -> {
            sleep(400);
            return "用户A";
        }).join();
        Integer b = CompletableFuture.supplyAsync(() -> {
            sleep(400);
            return 1280;
        }).join();
        long ms2 = (System.nanoTime() - t1) / 1_000_000;
        log("若是「先 join 再提交下一个」= " + ms2 + " ms   （" + a + " / " + b + "）← 串行相加");
        System.out.println();
    }

    // ---------------------------------------------------------------- D
    private static void sectionD() {
        System.out.println("== D. allOf：返回的是 CompletableFuture<Void>，结果得自己收 ==");
        long t0 = System.nanoTime();
        List<CompletableFuture<String>> tasks = List.of(
                CompletableFuture.supplyAsync(() -> { sleep(300); return "订单"; }),
                CompletableFuture.supplyAsync(() -> { sleep(200); return "库存"; }),
                CompletableFuture.supplyAsync(() -> { sleep(100); return "优惠券"; }));

        CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
        Object v = all.join();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        log("三个任务 300/200/100 ms，allOf().join() 返回值 = " + v + "   （Void，没有结果）");
        log("总耗时 = " + ms + " ms ≈ 最慢的那个（300 ms），不是三者之和");
        List<String> results = tasks.stream().map(CompletableFuture::join).toList();
        log("收集结果 = " + results + "   ← join 在这里不会阻塞，因为 allOf 已经保证全完成了");

        // 结果收集的简写
        String joined = String.join("+", tasks.stream().map(CompletableFuture::join).toList());
        log("拼起来 = " + joined);
        System.out.println();
    }

    // ---------------------------------------------------------------- E
    private static void sectionE() {
        System.out.println("== E. anyOf：谁先回来用谁，返回类型被擦成 Object ==");
        long t0 = System.nanoTime();
        CompletableFuture<Object> any = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> { sleep(300); return "慢接口"; }),
                CompletableFuture.supplyAsync(() -> { sleep(50); return "快接口"; }));
        Object got = any.join();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        log("anyOf 的静态类型 = CompletableFuture<Object>，值 = " + got + "（" + got.getClass().getSimpleName() + "）");
        log("耗时 = " + ms + " ms → 只等最快的那个");
        log("→ 代价：类型信息丢了。要拿回 String 得自己强转，泛型在这里救不了你。");
        System.out.println();
    }

    // ---------------------------------------------------------------- F
    private static void sectionF() {
        System.out.println("== F. 串联三次 vs 并行三路：同一个业务的两种编队 ==");
        long t0 = System.nanoTime();
        String serial = CompletableFuture.supplyAsync(() -> step("下单", 200))
                .thenCompose(a -> CompletableFuture.supplyAsync(() -> step(a + "→扣库存", 200)))
                .thenCompose(b -> CompletableFuture.supplyAsync(() -> step(b + "→发券", 200)))
                .join();
        long ms1 = (System.nanoTime() - t0) / 1_000_000;
        log("三次 thenCompose（有依赖，必须串） = " + ms1 + " ms   " + serial);

        long t1 = System.nanoTime();
        CompletableFuture<String> x = CompletableFuture.supplyAsync(() -> step("下单", 200));
        CompletableFuture<String> y = CompletableFuture.supplyAsync(() -> step("扣库存", 200));
        CompletableFuture<String> z = CompletableFuture.supplyAsync(() -> step("发券", 200));
        String par = CompletableFuture.allOf(x, y, z).thenApply(v -> x.join() + "/" + y.join() + "/" + z.join()).join();
        long ms2 = (System.nanoTime() - t1) / 1_000_000;
        log("三路并行（无依赖，可以并）       = " + ms2 + " ms   " + par);
        log("→ 有依赖才串，没依赖就并。把「能并的」写成 thenCompose 链是最常见的性能自残。");
        System.out.println();
    }

    private static String step(String what, long ms) {
        sleep(ms);
        return what;
    }

    private static String tn() {
        return Thread.currentThread().getName();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
