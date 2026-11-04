package com.newt.java48.w03.day1;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Day 1 · 三种起线程的方式（A 段）
 *
 * <pre>
 * A  extends Thread            最老的一种，任务和线程绑死，Java 只能单继承，所以不推荐
 * B  implements Runnable       任务与线程解耦，lambda 可以直接当 Runnable（W02 的函数式接口）
 * C  implements Callable&lt;V&gt;    有返回值、能抛受检异常，必须配 FutureTask 或线程池
 * D  run() vs start()          同一个方法，一个在 main 线程里跑，一个真的开新线程
 * E  daemon                    守护线程拦不住 JVM 退出
 * </pre>
 *
 * <p>运行：{@code run.bat}（默认就是本类）
 */
public class StartThread {

    public static void main(String[] args) throws Exception {
        System.out.println("main 线程：" + Thread.currentThread()
                + "，priority=" + Thread.currentThread().getPriority()
                + "，daemon=" + Thread.currentThread().isDaemon());
        System.out.println("可用核数：" + Runtime.getRuntime().availableProcessors());
        System.out.println();

        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
    }

    // ================================================================== A
    private static void sectionA() throws InterruptedException {
        System.out.println("========== A · 方式一：extends Thread ==========");
        Thread t = new ByExtends("worker-extends");
        t.start();
        System.out.println("  [main] start() 之后，我看到的 state=" + t.getState());
        t.join();
        System.out.println("  [main] join 回来，state=" + t.getState() + "（执行完就是 TERMINATED）");
        System.out.println();
    }

    static class ByExtends extends Thread {
        ByExtends(String name) {
            super(name);
        }

        @Override
        public void run() {
            System.out.println("  [extends] 线程名=" + Thread.currentThread().getName()
                    + "，id=" + Thread.currentThread().getId()
                    + "，state=" + Thread.currentThread().getState());
        }
    }

    // ================================================================== B
    private static void sectionB() throws InterruptedException {
        System.out.println("========== B · 方式二：implements Runnable ==========");
        Thread t = new Thread(new ByRunnable(), "worker-runnable");
        t.start();
        t.join();

        // W02 的 lambda：Runnable 只有一个抽象方法 run()，所以 lambda 天然就是它
        Thread lambda = new Thread(() ->
                System.out.println("  [lambda] 线程名=" + Thread.currentThread().getName()
                        + "（Runnable 是函数式接口，所以 lambda 能直接塞进 Thread）"),
                "worker-lambda");
        lambda.start();
        lambda.join();
        System.out.println();
    }

    static class ByRunnable implements Runnable {
        @Override
        public void run() {
            System.out.println("  [runnable] 线程名=" + Thread.currentThread().getName()
                    + "，id=" + Thread.currentThread().getId());
        }
    }

    // ================================================================== C
    private static void sectionC() throws Exception {
        System.out.println("========== C · 方式三：Callable<V>（有返回值）==========");
        System.out.println("  Runnable.run() 返回 void、不能抛受检异常；Callable.call() 两样都行。");

        Callable<Integer> task = () -> {
            int sum = 0;
            for (int i = 1; i <= 100; i++) {
                sum += i;
            }
            System.out.println("  [callable] 在 " + Thread.currentThread().getName() + " 上算完 1..100");
            return sum;
        };

        FutureTask<Integer> future = new FutureTask<>(task);
        Thread t = new Thread(future, "worker-callable");
        t.start();

        System.out.println("  [main] get() 之前 isDone=" + future.isDone() + "，此刻去 get() 会阻塞");
        int result = future.get();          // 阻塞直到 call() 返回
        System.out.println("  [main] get() 回来：" + result + "，isDone=" + future.isDone());

        // 有返回值 => 结果可以跨线程传回来，这是 Runnable 做不到的
        Callable<Integer> boom = () -> {
            throw new IllegalStateException("call() 里抛的异常会被包进 ExecutionException");
        };
        FutureTask<Integer> failed = new FutureTask<>(boom);
        new Thread(failed, "worker-boom").start();
        try {
            failed.get();
        } catch (ExecutionException e) {
            System.out.println("  [main] 抓到 " + e + "，真正的异常在 e.getCause() = " + e.getCause());
            System.out.println("  → 注意：原始异常不会直接冒泡出来，get() 统一包成 ExecutionException");
        }
        System.out.println();
    }

    // ================================================================== D
    private static void sectionD() throws InterruptedException {
        System.out.println("========== D · run() 和 start() 不是一回事 ==========");
        Thread t = new ByExtends("should-be-worker");
        System.out.println("  直接调 t.run()：");
        t.run();
        System.out.println("  ↑ 打印的是 main —— run() 只是普通方法调用，压根没开新线程");

        Thread again = new Thread(() ->
                System.out.println("  [twice] 我在 " + Thread.currentThread().getName()), "worker-twice");
        again.start();
        try {
            again.start();
        } catch (IllegalThreadStateException e) {
            System.out.println("  第二次 start()：" + e + "（而且没有 message —— 别指望它告诉你为什么）");
        }
        again.join();
        System.out.println();
    }

    // ================================================================== E
    private static void sectionE() throws InterruptedException {
        System.out.println("========== E · 守护线程拦不住 JVM 退出 ==========");
        Thread d = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            System.out.println("  [daemon] 5 秒到了 —— 你看到这行，说明 JVM 一直在等它");
        }, "worker-daemon");
        d.setDaemon(true);
        d.start();
        System.out.println("  [main] worker-daemon 的 daemon=" + d.isDaemon() + "，main 立刻结束、不等它");
        System.out.println("  → 正常情况下，上面那句「5 秒到了」永远不会出现：JVM 只等非守护线程");
        System.out.println();
    }
}
