package com.newt.java48.w02.day2;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

/**
 * Day 2 · 异常体系（二）：包装异常到底丢了什么
 *
 * <p>「`throw new RuntimeException(e)` 丢栈」这个说法只对了一半。这一节把它拆开：
 *
 * <pre>
 * A  三种包装写法对比：带 cause / 只带 message / 直接往上抛
 *    量出来的东西：栈深度、栈顶那一帧是谁
 * B  日志怎么写才不丢现场：getMessage() / toString() / printStackTrace 的差距
 * C  addSuppressed 与 cause 的分工：一次要带两个异常时用哪个
 * D  受检 / 非受检的真实分界线：编译器只对「可能抛出的受检异常」说话
 * E  sneakyThrow：用 Day 1 的擦除绕过受检检查（Lombok @SneakyThrows 的原理）
 * F  什么时候该包装，什么时候不该
 * </pre>
 *
 * <p>运行：{@code run.bat day2.ExceptionChainProbe}
 */
public class ExceptionChainProbe {

    public static void main(String[] args) {
        threeWaysToWrap();
        howToLog();
        suppressedVsCause();
        checkedBoundary();
        sneakyThrowDemo();
        whenToWrap();
    }

    // ------------------------------------------------------------------ A
    /** 最底层：真实原因在这里产生。 */
    private static void deepest() {
        throw new IllegalStateException("数据库连接超时");
    }

    private static void middle() {
        deepest();
    }

    // A1 带 cause：信息最全
    private static void wrapWithCause() {
        try {
            middle();
        } catch (Exception e) {
            throw new RuntimeException("查询用户失败", e);
        }
    }

    // A2 只带 message：cause 没了，等于把现场删了
    private static void wrapMessageOnly() {
        try {
            middle();
        } catch (Exception e) {
            throw new RuntimeException("查询用户失败：" + e.getMessage());
        }
    }

    private static void threeWaysToWrap() {
        System.out.println("========== A · 三种包装写法 ==========");

        // A1
        try {
            wrapWithCause();
        } catch (RuntimeException e) {
            StackTraceElement top = e.getStackTrace()[0];
            System.out.println("A1 new RuntimeException(\"查询用户失败\", e)");
            System.out.println("   栈深度 = " + e.getStackTrace().length
                    + "，栈顶 = " + top.getClassName() + "." + top.getMethodName() + ":" + top.getLineNumber());
            System.out.println("   getCause() = " + e.getCause());
            System.out.println("   cause 栈深度 = " + (e.getCause() == null ? "-" : e.getCause().getStackTrace().length)
                    + "，cause 栈顶 = " + (e.getCause() == null ? "-"
                    : e.getCause().getStackTrace()[0].getClassName() + "."
                    + e.getCause().getStackTrace()[0].getMethodName()));
            System.out.println("   => 栈没有被「丢」，只是被「分成了两段」：包装点到 main 在新异常上，");
            System.out.println("      真正的出错点在 cause 里。少打 cause 就等于只看半段栈。");
        }

        // A2
        try {
            wrapMessageOnly();
        } catch (RuntimeException e) {
            System.out.println();
            System.out.println("A2 new RuntimeException(\"查询用户失败：\" + e.getMessage())");
            System.out.println("   栈深度 = " + e.getStackTrace().length + "，getCause() = " + e.getCause());
            System.out.println("   原始异常类型 IllegalStateException 也一起没了，只剩一句话");
            System.out.println("   => 这才是真的丢栈。现场只剩「数据库连接超时」这七个字，");
            System.out.println("      不知道是哪一行、哪个类型、有没有被别的异常包过。");
        }

        // A3 不包装，直接往上抛
        try {
            middle();
        } catch (Exception e) {
            System.out.println();
            System.out.println("A3 不包装，直接 throws 往上抛");
            System.out.println("   栈深度 = " + e.getStackTrace().length + "，栈顶 = " + e.getStackTrace()[0]);
            System.out.println("   => 栈是完整一段，没有 cause、没有断层，排查最省事。");
            System.out.println("      代价是方法签名上多一个 throws，调用方必须知道底层异常类型。");
        }
        System.out.println();
    }

    // ------------------------------------------------------------------ B
    private static String render(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static void howToLog() {
        System.out.println("========== B · 日志写法决定还剩多少现场 ==========");
        Throwable captured;
        try {
            wrapWithCause();
            return;
        } catch (RuntimeException e) {
            captured = e;
        }

        String onlyMessage = "查询失败：" + captured.getMessage();
        String withToString = "查询失败：" + captured;
        String full = render(captured);

        System.out.println("  ① log.error(\"...\" + e.getMessage())   字符数 = " + onlyMessage.length()
                + "，行数 = 1");
        System.out.println("     " + onlyMessage);
        System.out.println("  ② log.error(\"...\" + e)               字符数 = " + withToString.length()
                + "，行数 = 1");
        System.out.println("     " + withToString);
        System.out.println("  ③ log.error(\"...\", e)                字符数 = " + full.length()
                + "，行数 = " + full.split("\n").length);
        for (String line : full.split("\n")) {
            if (line.trim().startsWith("at com.newt") || line.contains("Caused by")) {
                System.out.println("     " + line.trim());
            }
        }
        System.out.println("  => ① 只有一句话；② 多了类名但还在同一行；③ 才是完整现场（含 Caused by）。");
        System.out.println("     slf4j 的 {} 占位符最后一个是异常对象时，会自动打全栈，不需要 e.toString()。");
        System.out.println();
    }

    // ------------------------------------------------------------------ C
    private static void suppressedVsCause() {
        System.out.println("========== C · cause 与 suppressed 的分工 ==========");
        IllegalStateException main = new IllegalStateException("主异常：订单状态校验失败");
        main.addSuppressed(new java.io.IOException("附属 1：写审计日志失败"));
        main.addSuppressed(new IllegalArgumentException("附属 2：MQ 发送失败"));

        System.out.println("  主异常 = " + main);
        System.out.println("  getCause() = " + main.getCause() + "   <- 还是 null");
        System.out.println("  getSuppressed() = " + Arrays.toString(main.getSuppressed()));
        System.out.println("  分工：");
        System.out.println("    cause      = 「因为什么才失败」—— 一对一，能用 initCause 补一次，之后不能再改；");
        System.out.println("    suppressed = 「失败路上还顺手炸了别的」—— 一对多，TWR 关资源就靠它。");
        System.out.println("  顺带一个坑：addSuppressed 传 null 会抛 NullPointerException，传自己会抛 IllegalArgumentException。");
        try {
            main.addSuppressed(main);
        } catch (IllegalArgumentException e) {
            System.out.println("  实测 main.addSuppressed(main) -> IllegalArgumentException: " + e.getMessage());
        }
        System.out.println();
    }

    // ------------------------------------------------------------------ D
    private static void checkedBoundary() {
        System.out.println("========== D · 受检 / 非受检的分界线 ==========");
        System.out.println("  编译器只做一件事：检查「这里有没有可能抛出受检异常，而你没交代」。");
        System.out.println("  · try 里不可能抛 IOException，却写 catch (IOException e) -> 编译错：");
        System.out.println("      error: exception IOException is never thrown in body of corresponding try statement");
        System.out.println("  · catch (RuntimeException e) / catch (Exception e) 永远合法 —— 非受检异常编译器不管。");
        System.out.println("  · 所以「受检」的本质是：Throwable 的子类里，除 Error / RuntimeException 之外的那些，");
        System.out.println("    方法必须显式 throws 或就地 catch。它约束的是「调用方必须做决定」，不是「更严重」。");
        System.out.println();

        // 现场证明 catch (Exception) 合法但 catch (IOException) 不合法（后者见上面注释）
        try {
            int x = 1 / 1;
            System.out.println("  try { int x = 1 / 1; } 里没有受检异常，但 catch (Exception e) 编译通过（x=" + x + "）");
        } catch (Exception e) {
            System.out.println("  不会走到这里");
        }
        System.out.println();
    }

    // ------------------------------------------------------------------ E
    /**
     * 把受检异常「偷偷」抛出去：擦除之后 T 是 RuntimeException，
     * 编译器按 T 检查 throws 子句，于是 IOException 混过了检查。
     */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    /** 注意：这个方法签名上没有任何 throws，但它真的会抛 IOException。 */
    private static void looksInnocent() {
        ExceptionChainProbe.<RuntimeException>sneakyThrow(new java.io.IOException("我是受检异常，但没人声明我"));
    }

    private static void sneakyThrowDemo() {
        System.out.println("========== E · 用擦除绕过受检检查（sneaky throw） ==========");
        try {
            looksInnocent();
        } catch (Exception e) {
            System.out.println("  looksInnocent() 的签名是 void looksInnocent()，一个 throws 都没有");
            System.out.println("  实际抛出了： " + e.getClass().getName() + ": " + e.getMessage());
            System.out.println("  getCause() = " + e.getCause() + "   <- 不是被包装，是原样抛出");
            System.out.println("  原理：方法声明 throws T，T 擦除成 Throwable 的下界检查，而调用处显式指定 T = RuntimeException，");
            System.out.println("        编译器只按 RuntimeException 检查 —— 实参是 IOException 这件事它看不见（Day 1 的擦除）。");
            System.out.println("  这就是 Lombok @SneakyThrows 的实现方式。生产代码慎用：");
            System.out.println("        调用方按签名以为不用处理，实际会在运行期收到没声明过的受检异常。");
        }
        System.out.println();
    }

    // ------------------------------------------------------------------ F
    private static void whenToWrap() {
        System.out.println("========== F · 什么时候包装，什么时候不包装 ==========");
        System.out.println("  该包装（并且一定带上 cause）：");
        System.out.println("    ① 跨层：DAO 的 SQLException 不该漏到 Controller，换成业务语义的异常；");
        System.out.println("    ② 换异常体系：受检 -> 非受检（比如在 lambda / Stream 里没法写 throws）；");
        System.out.println("    ③ 补上下文：把 orderId、userId 这类「排查必需但底层拿不到」的信息塞进 message。");
        System.out.println("  不该包装：");
        System.out.println("    ① 同层、同一个语义，包装只会多一层 Caused by，纯噪音；");
        System.out.println("    ② 只想加日志 —— 打完日志直接 throw e，不要 new 一个新的；");
        System.out.println("    ③ 用 message 替代 cause（A2 那种），现场全丢。");
        System.out.println("  判断标准一句话：包装是为了「给上层一个能处理的抽象」，不是为了「显得统一」。");
        System.out.println();
        System.out.println("========== 收尾 ==========");
        System.out.println("RuntimeException(e) 不丢栈，它把栈切成两段；只传 message 才是真丢栈。");
        System.out.println("finally 里的 return / throw 才是真的「静默吃掉异常」—— 这也是 TWR 存在的理由。");
    }
}
