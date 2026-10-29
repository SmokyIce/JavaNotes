package com.newt.java48.w02.day2;

/**
 * Day 2 · 异常体系（一）：资源关闭与「异常被吃掉」的三种方式
 *
 * <p>重点不是背「受检 / 非受检」，而是三件真能写出 bug 的事：
 *
 * <pre>
 * A  try-with-resources 的关闭顺序：逆序，后开的先关
 * B  close() 自己抛异常时，TWR 把它 addSuppressed 到主异常上 —— 两个都在
 * C  finally 里 return：静默吞掉 try 里抛的异常，连日志都没有
 * D  手写 try/finally 且 close 抛异常：主异常被直接覆盖，连 suppressed 都拿不到
 * E  受检异常在 TWR 里的位置：close 的 throws 也要在签名上交代
 * </pre>
 *
 * <p>运行：{@code run.bat day2.TryWithResourcesProbe}
 */
public class TryWithResourcesProbe {

    public static void main(String[] args) {
        closeOrder();
        closeThrowsSuppressed();
        finallyReturnSwallows();
        finallyThrowMasks();
        checkedInTryWithResources();
        System.out.println("========== 收尾 ==========");
        System.out.println("C 和 D 是同一个病的两种表现：finally 里只要有 return / throw，try 里正在冒泡的异常就没了。");
        System.out.println("TWR 的价值不只是「少写 close」，而是它保证了「主异常一定活下来，close 的异常降级成 suppressed」。");
    }

    // ------------------------------------------------------------------ 资源
    /** 最普通的资源：只记录开关，不抛异常。 */
    static class Res implements AutoCloseable {
        private final String name;

        Res(String name) {
            this.name = name;
            System.out.println("  open  " + name);
        }

        void use() {
            System.out.println("  use   " + name);
        }

        @Override
        public void close() {
            System.out.println("  close " + name);
        }
    }

    /** 关闭时会炸的资源：模拟「连接已经断了，close 才报错」。 */
    static class BadClose implements AutoCloseable {
        @Override
        public void close() {
            throw new IllegalStateException("close 阶段自己炸了");
        }
    }

    // ------------------------------------------------------------------ A
    private static void closeOrder() {
        System.out.println("========== A · 关闭顺序 ==========");
        try (Res a = new Res("A"); Res b = new Res("B")) {
            a.use();
            b.use();
        }
        System.out.println("  结论：close 顺序是 B -> A，即「后开的先关」，和栈一样。");
        System.out.println("  为什么必须逆序：B 可能依赖 A（比如 B 是 A 上的事务），先关 A 会让 B 的 close 拿到坏连接。");
        System.out.println();
    }

    // ------------------------------------------------------------------ B
    private static void closeThrowsSuppressed() {
        System.out.println("========== B · close 抛异常：TWR 用 suppressed 保住两个 ==========");
        try (BadClose bad = new BadClose()) {
            throw new RuntimeException("业务代码里炸的");
        } catch (Exception e) {
            System.out.println("  catch 到的： " + e);
            Throwable[] sup = e.getSuppressed();
            System.out.println("  getSuppressed().length = " + sup.length);
            for (Throwable t : sup) {
                System.out.println("  suppressed: " + t);
            }
            System.out.println("  => 主异常是业务异常，close 的异常降级成 suppressed，两个信息都在。");
        }
        System.out.println("  如果 try 块正常结束、只有 close 抛，那 close 的异常就是主异常（没有东西可压制）。");
        System.out.println();
    }

    // ------------------------------------------------------------------ C
    /** 注意：javac 不拦这个写法，只有 IDE 会提示。 */
    private static int swallow() {
        try {
            throw new IllegalStateException("try 里抛的，本应往上冒泡");
        } finally {
            return 42;      // 这一行把上面的异常直接抹掉了
        }
    }

    /** 覆盖返回值：finally 里的 return 也会盖掉 try 里的 return。 */
    @SuppressWarnings("finally")
    private static int overrideReturn() {
        try {
            return 1;
        } finally {
            return 2;
        }
    }

    private static void finallyReturnSwallows() {
        System.out.println("========== C · finally 里 return：静默吞异常 ==========");
        int v = swallow();
        System.out.println("  swallow() 返回 " + v + "，没有异常、没有日志、没有任何痕迹");
        System.out.println("  try 里那句 throw new IllegalStateException(...) 就这样消失了 ——");
        System.out.println("  不是被 catch 了，是「方法正常返回」把异常帧一起扔了。");
        System.out.println("  overrideReturn() 返回 " + overrideReturn() + "（try 的 return 1 被 finally 的 return 2 覆盖）");
        System.out.println("  javac 对这个写法一句话都不说，IDEA 才会标 'return inside finally block'。");
        System.out.println();
    }

    // ------------------------------------------------------------------ D
    /** 等价于 Java 7 之前手写资源关闭的写法 —— close 抛异常就会盖掉业务异常。 */
    private static void jdk6Style(StringBuilder log) {
        BadClose bad = new BadClose();
        try {
            throw new RuntimeException("业务异常 A");
        } finally {
            log.append("  finally: 准备关闭资源\n");
            bad.close();      // finally 里的异常直接把 A 顶掉
        }
    }

    private static void finallyThrowMasks() {
        System.out.println("========== D · finally 里抛异常：主异常被覆盖，连 suppressed 都没有 ==========");
        StringBuilder log = new StringBuilder();
        try {
            jdk6Style(log);
        } catch (Exception e) {
            System.out.print(log);
            System.out.println("  catch 到的： " + e);
            System.out.println("  getSuppressed().length = " + e.getSuppressed().length + "   <- 业务异常 A 彻底消失");
            System.out.println("  getCause() = " + e.getCause() + "   <- 也没有");
            System.out.println("  => 线上表现：日志里只有「close 阶段自己炸了」，业务异常查不到，白排查半天。");
        }
        System.out.println("  这就是 TWR 被设计出来的直接原因：把「关资源」从 finally 里挪走，");
        System.out.println("  由编译器生成 try/finally + addSuppressed，保证主异常不被覆盖。");
        System.out.println();
    }

    // ------------------------------------------------------------------ E
    /** 资源实现的 close 声明了受检异常，使用处就要交代。 */
    static class CheckedRes implements AutoCloseable {
        @Override
        public void close() throws java.io.IOException {
            throw new java.io.IOException("close 抛受检异常");
        }
    }

    private static void checkedInTryWithResources() {
        System.out.println("========== E · 受检异常在 TWR 里的位置 ==========");
        try (CheckedRes r = new CheckedRes()) {
            System.out.println("  body 正常结束");
        } catch (java.io.IOException e) {
            System.out.println("  catch(IOException) 收到： " + e.getMessage());
            System.out.println("  => close 的 throws 会被「提升」到 try-with-resources 语句上，");
            System.out.println("     写这段代码的人必须处理它，编译器不会替你兜。");
        }
        // 反过来的坑：如果 try 里也抛 IOException，谁当主异常？现场跑一遍，不猜。
        try (CheckedRes r = new CheckedRes()) {
            throw new java.io.IOException("body 里的受检异常");
        } catch (java.io.IOException e) {
            System.out.println("  body 和 close 都抛 IOException 时：");
            System.out.println("    主异常 = " + e.getMessage()
                    + "，suppressed = " + java.util.Arrays.toString(e.getSuppressed()));
            System.out.println("  => 主异常永远是 try 块里的那个，close 的进 suppressed（和 B 段同一条规律）。");
        }
        System.out.println();
    }
}
