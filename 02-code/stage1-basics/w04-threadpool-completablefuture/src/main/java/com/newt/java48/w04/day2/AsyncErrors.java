package com.newt.java48.w04.day2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * W04 Day 2 · A~H：异步链上的异常到底被谁吃了。
 *
 * 三个方法名字很像，作用完全不同，一句话先记住：
 *   exceptionally : 只在失败时执行，返回一个「兜底值」，异常到此为止（能吞）
 *   handle        : 成功失败都执行，看 ex 是否为 null 分支（能吞）
 *   whenComplete  : 成功失败都执行，但「只看不改」—— 它返回的 CF 仍然保持原来的成败（不能吞）
 *
 * 今天要特别量清楚的是第三条：whenComplete 里明明打了日志、甚至「处理了」异常，
 * 但下游 join() 照样抛 —— 因为它只是个观察者。
 */
public class AsyncErrors {

    private static void log(String s) {
        System.out.println("  " + s);
    }

    /** 一个必定失败的异步任务。 */
    private static CompletableFuture<String> failing() {
        return CompletableFuture.supplyAsync(() -> {
            log("上游任务：抛 IllegalStateException");
            throw new IllegalStateException("库存服务 500");
        });
    }

    private static <T> CompletableFuture<T> failingWith(String msg) {
        return CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException(msg);
        });
    }

    public static void main(String[] args) throws Exception {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
        sectionF();
        sectionG();
        sectionH();
        System.out.println();
        System.out.println("  一句话：exceptionally / handle 是「修复」，whenComplete 是「围观」；");
        System.out.println("  要兜底就用前两个，只想记日志就别指望它把异常咽下去。");
    }

    // ---------------------------------------------------------------- A
    private static void sectionA() {
        System.out.println("== A. 上游失败 → 后面的 thenApply 整个被跳过 ==");
        CompletableFuture<String> cf = failing()
                .thenApply(s -> {
                    log("!!! step2 被调用了（不该发生）");
                    return s + "-step2";
                })
                .thenApply(s -> {
                    log("!!! step3 被调用了（不该发生）");
                    return s + "-step3";
                });
        try {
            cf.join();
        } catch (CompletionException e) {
            log("join() 抛 CompletionException，cause = " + e.getCause());
        }
        log("→ 失败之后链上所有「不处理异常」的节点全部跳过，异常沿途复制到最后一站。");
        System.out.println();
    }

    // ---------------------------------------------------------------- B
    private static void sectionB() {
        System.out.println("== B. exceptionally：只在失败时跑，返回值就是兜底结果 ==");
        String v1 = failing()
                .exceptionally(ex -> {
                    log("exceptionally 收到 " + ex.getClass().getSimpleName() + " / " + ex.getMessage());
                    return "兜底：返回缓存里的库存";
                })
                .join();
        log("join() = " + v1 + "   ← 不再抛异常，链变成「成功」");

        String v2 = CompletableFuture.supplyAsync(() -> "正常结果")
                .exceptionally(ex -> {
                    log("!!! exceptionally 被调用了（成功时不该跑）");
                    return "兜底";
                })
                .join();
        log("成功路径下 exceptionally 不执行，join() = " + v2);
        System.out.println();
    }

    // ---------------------------------------------------------------- C
    private static void sectionC() {
        System.out.println("== C. handle：成功失败都进，靠 ex 是否为 null 分支 ==");
        String ok = CompletableFuture.supplyAsync(() -> "正常结果")
                .handle((r, ex) -> {
                    log("成功分支：r = " + r + "，ex = " + ex);
                    return "加工后：" + r;
                })
                .join();
        log("join() = " + ok);

        String bad = failing()
                .handle((r, ex) -> {
                    log("失败分支：r = " + r + "，ex = " + ex.getClass().getSimpleName());
                    return "降级结果";
                })
                .join();
        log("join() = " + bad + "   ← 也把异常吃掉了");
        System.out.println();
    }

    // ---------------------------------------------------------------- D
    private static void sectionD() {
        System.out.println("== D. whenComplete：看着像处理了，其实一个都没吞 ==");
        CompletableFuture<String> cf = failing()
                .whenComplete((r, ex) -> {
                    log("whenComplete 收到 r = " + r + "，ex = " + ex.getClass().getSimpleName());
                    log("我在这里打了日志、也「处理」了 —— 但下面照样抛");
                });
        try {
            cf.join();
        } catch (CompletionException e) {
            log("join() 仍然抛 " + e.getClass().getSimpleName() + "：cause = " + e.getCause());
        }

        // 成功路径：whenComplete 也不改结果
        String v = CompletableFuture.supplyAsync(() -> "A")
                .whenComplete((r, ex) -> log("成功时也会执行，r = " + r + "，ex = " + ex))
                .join();
        log("成功路径 join() = " + v + "（whenComplete 的返回值被忽略，原值继续往下走）");
        log("→ 它的签名是 (T, Throwable) -> void，没有返回值，所以物理上就没法兜底。");
        System.out.println();
    }

    // ---------------------------------------------------------------- E
    private static void sectionE() {
        System.out.println("== E. 在 whenComplete / exceptionally 里自己抛：原始异常会不会被顶掉 ==");
        System.out.println("  -- E1 上游已经失败 + whenComplete 又抛 --");
        try {
            failingWith("原始异常")
                    .whenComplete((r, ex) -> {
                        throw new IllegalStateException("whenComplete 里又抛了一个");
                    })
                    .join();
        } catch (CompletionException e) {
            log("拿到 cause = " + e.getCause().getMessage() + "   ← 原始异常赢了，新抛的没能顶掉它");
            log("外层 CompletionException 的 suppressed = " + e.getSuppressed().length
                    + "，cause 自己的 suppressed = " + e.getCause().getSuppressed().length);
            if (e.getSuppressed().length > 0) {
                log("被挂上来的正是：" + e.getSuppressed()[0].getMessage() + "（只是挂在包装层，不是原因）");
            }
        }

        System.out.println("  -- E2 上游成功 + whenComplete 又抛 --");
        try {
            CompletableFuture.supplyAsync(() -> "正常结果")
                    .whenComplete((r, ex) -> {
                        throw new IllegalStateException("日志逻辑写挂了");
                    })
                    .join();
        } catch (CompletionException e) {
            log("拿到 cause = " + e.getCause().getMessage() + "   ← 这次新异常生效了，把成功变成了失败");
        }
        log("→ 结论：whenComplete 的异常只在「上游本来没事」时才起作用。");
        log("  上游已失败时它只能当配角：JDK 把它 addSuppressed 到外层包装异常上（源码 line 872），");
        log("  真正往外传的还是原始异常 —— 所以「用 whenComplete 兜底」从来兜不住。");

        System.out.println("  -- E3 上游失败 + exceptionally 又抛 --");
        try {
            failingWith("原始异常")
                    .exceptionally(ex -> {
                        throw new IllegalStateException("兜底逻辑也挂了");
                    })
                    .join();
        } catch (CompletionException e) {
            log("拿到 cause = " + e.getCause().getMessage() + "   ← 覆盖了原始异常");
            log("外层 suppressed = " + e.getSuppressed().length
                    + "，cause 自己的 suppressed = " + e.getCause().getSuppressed().length);
            log("→ 原始异常既不在 cause 里，也不在 suppressed 里 —— 彻底丢了。");
            log("  因为 exceptionally 抛异常时走的是 completeThrowable(ex)，直接 new 了一个新的包装。");
        }
        log("→ 兜底代码本身也会失败。要么它自己 try-catch，要么链上再挂一层 exceptionally。");
        System.out.println();
    }

    // ---------------------------------------------------------------- F
    private static void sectionF() throws Exception {
        System.out.println("== F. join() vs get()：包装的异常类型不一样 ==");
        try {
            failing().join();
        } catch (CompletionException e) {
            log("join() → CompletionException（RuntimeException，方法不用 throws）");
        }
        try {
            failing().get();
        } catch (ExecutionException e) {
            log("get()  → ExecutionException（受检异常，必须 throws / catch）");
        }
        try {
            CompletableFuture<String> never = new CompletableFuture<>();
            never.get(100, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log("get(100ms) → TimeoutException：get 支持超时，join 不支持");
        }

        // 嵌套包装：CompletionException 里再套 CompletionException
        try {
            failingWith("内层").exceptionally(ex -> {
                throw new RuntimeException("兜底崩了", ex);
            }).join();
        } catch (CompletionException e) {
            Throwable cur = e;
            StringBuilder sb = new StringBuilder();
            while (cur != null) {
                sb.append(cur.getClass().getSimpleName()).append("(").append(cur.getMessage()).append(")");
                cur = cur.getCause();
                if (cur != null) {
                    sb.append(" <- ");
                }
            }
            log("嵌套后的异常链 = " + sb);
        }
        log("→ 每经过一次异步边界就多包一层，所以打日志直接打 ex.getMessage() 只能看到");
        log("  「java.util.concurrent.CompletionException: ...」这种没有信息量的东西。");

        // 正确姿势：先拆包装再判断类型
        failing().handle((r, ex) -> {
            log("handle 里的 ex 类型 = " + ex.getClass().getName());
            log("  ex.getMessage() = " + ex.getMessage());
            log("  ex.getCause()   = " + ex.getCause());
            log("  拆一层后 = " + unwrap(ex));
            return "x";
        }).join();

        try {
            failing().join();
        } catch (CompletionException ce) {
            Throwable e = ce;      // 用 Throwable 声明，才能演示 instanceof 匹配不到
            log("所以类型判断要往下拆一层，否则永远匹配不到自己的业务异常：");
            log("  e                     instanceof IllegalStateException → "
                    + (e instanceof IllegalStateException));
            log("  e.getCause()          instanceof IllegalStateException → "
                    + (e.getCause() instanceof IllegalStateException));
            log("  但线程池里失败两次就会套两层，只拆一层不够 → unwrap(e) 用 while 循环：");
            log("  unwrap(e)             instanceof IllegalStateException → "
                    + (unwrap(e) instanceof IllegalStateException));
        }
        System.out.println();
    }

    // ---------------------------------------------------------------- G
    private static void sectionG() {
        System.out.println("== G. 最常见的写法错误：exceptionally 的返回值没人接 ==");
        CompletableFuture<String> cf = failing();
        cf.exceptionally(ex -> "兜底");     // ← 返回值被丢掉
        try {
            cf.join();
        } catch (CompletionException e) {
            log("原 CF 仍然异常：" + e.getCause().getMessage());
        }
        log("→ CompletableFuture 是不可变的：每个 then/exceptionally 都返回「新的」CF，");
        log("  老的那个状态一点没变。所有链式 API 都是这个约定，没有例外。");
        System.out.println();
    }

    // ---------------------------------------------------------------- H
    private static void sectionH() throws Exception {
        System.out.println("== H. 超时：orTimeout / completeOnTimeout（JDK 9+）==");
        long t0 = System.nanoTime();
        try {
            CompletableFuture.supplyAsync(() -> {
                sleep(800);
                return "慢";
            }).orTimeout(200, TimeUnit.MILLISECONDS).join();
        } catch (CompletionException e) {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            log("orTimeout(200ms) → 约 " + ms + " ms 后抛 " + e.getCause().getClass().getSimpleName());
        }

        long t1 = System.nanoTime();
        String v = CompletableFuture.supplyAsync(() -> {
            sleep(800);
            return "慢";
        }).completeOnTimeout("超时降级值", 200, TimeUnit.MILLISECONDS).join();
        long ms2 = (System.nanoTime() - t1) / 1_000_000;
        log("completeOnTimeout(200ms) → " + ms2 + " ms 后拿到 \"" + v + "\"（不抛，直接给默认值）");
        log("→ 区别就是「抛」和「给默认值」。给 RPC 调用兜底时，前者要 catch，后者不用。");
        log("→ 注意：超时只是让 CF 提前完成，后台那个慢任务还在跑，它不会被中断。");
        System.out.println();
    }

    /** 拆掉 CompletionException / ExecutionException 的包装，拿到业务异常本体。 */
    private static Throwable unwrap(Throwable t) {
        while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
