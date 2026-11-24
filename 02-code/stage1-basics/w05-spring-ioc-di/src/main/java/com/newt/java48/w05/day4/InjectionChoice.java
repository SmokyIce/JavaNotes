package com.newt.java48.w05.day4;

import com.newt.java48.w05.day4.pay.AlipayChannel;
import com.newt.java48.w05.day4.pay.PayChannel;
import com.newt.java48.w05.day4.pay.UnionPayChannel;
import com.newt.java48.w05.day4.pay.WechatChannel;
import com.newt.java48.w05.support.Quiet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.Resource;

/**
 * W05 Day 4 · 第一段：同一个接口三个实现，「给哪个」是怎么定的。
 *
 * <p>六个对照，全部围绕一条判定链（{@code DefaultListableBeanFactory#determineAutowireCandidate}）：
 * <pre>
 *   按类型筛出候选  →  @Qualifier 缩范围（在筛的时候就已经生效）
 *                  →  @Primary 独苗？
 *                  →  @Priority 最大？
 *                  →  字段名 == bean 名？
 *                  →  还不行就 NoUniqueBeanDefinitionException
 * </pre>
 *
 * <p>今天的落点不是「@Autowired 按类型、@Resource 按名字」这句背书，而是两件更硬的事：
 * <ol>
 *   <li>{@code @Autowired} <b>不是纯按类型</b> —— 类型有多候选时它会退化成「字段名当 bean 名」；</li>
 *   <li>两种注解「名字对不上」时报的错<b>根本不是同一个</b>：一个说「找到 3 个，无法取舍」，
 *       一个说「没有叫这个名字的 bean」—— 后者连类型都没查。</li>
 * </ol>
 */
public class InjectionChoice {

    public static void main(String[] args) {
        Quiet.springRefreshWarning();
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
        sectionF();
    }

    // ------------------------------------------------------------------ 被测 bean

    /** A 段：字段名 {@code channel} 对不上任何 bean 名。 */
    static class NeedsOne {
        @Autowired
        PayChannel channel;
    }

    /** B / D 段：字段名正好是 {@code wechatChannel}，而容器里真的有个同名 bean。 */
    static class NameMatch {
        @Autowired
        PayChannel wechatChannel;
    }

    /** C 段：用 @Qualifier 点名。 */
    static class ByQualifier {
        @Autowired
        @Qualifier("alipayChannel")
        PayChannel channel;
    }

    /** C 段的反例：点名一个不存在的限定符。 */
    static class BadQualifier {
        @Autowired
        @Qualifier("noSuchChannel")
        PayChannel channel;
    }

    /** E 段：{@code @Resource} 默认 name = 字段名。 */
    static class ByResourceName {
        @Resource
        PayChannel unionPayChannel;
    }

    /** E 段：{@code @Resource(name=...)} 显式点名，字段名随便起。 */
    static class ByResourceAttr {
        @Resource(name = "alipayChannel")
        PayChannel anything;
    }

    /** F 段：{@code @Resource} 显式点了一个不存在的名字。 */
    static class ResourceWrongName {
        @Resource(name = "channel")
        PayChannel channel;
    }

    /** F 段：{@code @Resource} 用默认名（字段名），而字段名也对不上。 */
    static class ResourceDefaultName {
        @Resource
        PayChannel channel;
    }

    @Configuration
    static class PrimaryConfig {
        @Bean
        @Primary
        PayChannel preferredChannel() {
            return () -> "preferred";
        }
    }

    // ------------------------------------------------------------------ A

    /**
     * A：三个候选、字段名也不是任何 bean 名 → 直接炸。
     *
     * <p>顺带取一次栈：这个异常是在<b>后处理器</b>里抛的，接 Day 2 的
     * {@code AutowiredAnnotationBeanPostProcessor}。
     */
    private static void sectionA() {
        System.out.println("=== A. @Autowired 遇到三个候选：报错原文 ===");
        System.out.println("  （注意：它死在 refresh() 里 —— 因为后面没有别的 bean 依赖它，");
        System.out.println("   它自己在 preInstantiateSingletons 的循环里就被造了，等不到 getBean）");

        Throwable e = failOf(AlipayChannel.class, WechatChannel.class, UnionPayChannel.class, NeedsOne.class);
        if (e == null) {
            System.out.println("  ✗ 居然注入成功了 —— 说明前面的假设是错的");
            return;
        }
        System.out.println("  字段名 channel 对不上任何 bean，且没有 @Primary / @Qualifier：");
        showChain(e);
        System.out.println();
        System.out.println("  栈上真正抛出的位置（不是 BeanFactory 的解析，是后处理器在灌字段）：");
        showFrames(rootCause(e));
    }

    // ------------------------------------------------------------------ B

    /**
     * B：字段名换成 {@code wechatChannel} —— 一个 {@code @Qualifier} 都不写，照样注入成功。
     *
     * <p>今天第一个反直觉点：{@code @Autowired} 的「按类型」失败之后，还有一级
     * 「按字段名」的回退。教科书一般只讲前一半。
     */
    private static void sectionB() {
        System.out.println();
        System.out.println("=== B. 什么注解都不加，只把字段名改成 wechatChannel ===");

        AnnotationConfigApplicationContext ctx = ctxOf(
                AlipayChannel.class, WechatChannel.class, UnionPayChannel.class, NameMatch.class);

        NameMatch b = ctx.getBean(NameMatch.class);
        System.out.printf("  字段名 wechatChannel → 注入到 %s（三个候选里挑的，没写 @Qualifier）%n",
                b.wechatChannel.tag());
        System.out.println("  说明：类型匹配到 3 个之后，还有一级「字段名 == bean 名」的回退。");
        ctx.close();
    }

    // ------------------------------------------------------------------ C

    /** C：{@code @Qualifier} 按名字点名；点名不存在时是 NoSuchBean（不是 NoUnique）。 */
    private static void sectionC() {
        System.out.println();
        System.out.println("=== C. @Qualifier 点名 ===");

        AnnotationConfigApplicationContext ctx = ctxOf(
                AlipayChannel.class, WechatChannel.class, UnionPayChannel.class, ByQualifier.class);
        System.out.printf("  @Qualifier(\"alipayChannel\") → %s%n", ctx.getBean(ByQualifier.class).channel.tag());
        ctx.close();

        System.out.println("  @Qualifier(\"noSuchChannel\") 后果：");
        Throwable e = failOf(AlipayChannel.class, WechatChannel.class, UnionPayChannel.class, BadQualifier.class);
        if (e != null) {
            showChain(e);
        }
    }

    // ------------------------------------------------------------------ D

    /**
     * D：{@code @Primary} 与「字段名回退」谁赢？
     *
     * <p>字段名叫 {@code wechatChannel}（真的有个同名 bean），同时另有一个 {@code @Primary}。
     * 判定链里 Primary 在 name match 之前 —— 所以结果是 primary 那个。
     */
    private static void sectionD() {
        System.out.println();
        System.out.println("=== D. @Primary 压过「字段名回退」 ===");

        AnnotationConfigApplicationContext ctx = ctxOf(
                PrimaryConfig.class, AlipayChannel.class, WechatChannel.class, UnionPayChannel.class,
                NameMatch.class);
        NameMatch d = ctx.getBean(NameMatch.class);
        System.out.printf("  字段名依旧是 wechatChannel，但容器里有 @Primary → 注入到 %s%n", d.wechatChannel.tag());
        System.out.println("  判定顺序：Qualifier → Primary → Priority → 名字匹配，Primary 在前。");
        ctx.close();
    }

    // ------------------------------------------------------------------ E

    /** E：{@code @Resource} 按名字点名 —— 认的是字段名或 name 属性。 */
    private static void sectionE() {
        System.out.println();
        System.out.println("=== E. @Resource 按名字 ===");

        AnnotationConfigApplicationContext ctx = ctxOf(
                AlipayChannel.class, WechatChannel.class, UnionPayChannel.class,
                ByResourceName.class, ByResourceAttr.class);
        System.out.printf("  @Resource + 字段名 unionPayChannel → %s%n",
                ctx.getBean(ByResourceName.class).unionPayChannel.tag());
        System.out.printf("  @Resource(name=\"alipayChannel\") 字段名叫 anything → %s（字段名被忽略）%n",
                ctx.getBean(ByResourceAttr.class).anything.tag());
        ctx.close();
    }

    // ------------------------------------------------------------------ F

    /**
     * F：名字对不上时，两种注解的报错<b>完全不同</b> —— 这是面试的落点。
     *
     * <p>{@code @Resource} 明确点名后就直接按名字取，名字不存在 → {@code NoSuchBeanDefinitionException}
     * （它<b>没有</b>回头按类型找过，虽然类型明明能匹配上 3 个）。而默认名查不到时，它会退回按类型，
     * 于是又踩到 A 段那个 NoUnique。
     */
    private static void sectionF() {
        System.out.println();
        System.out.println("=== F. 名字对不上，两种注解的报错原文 ===");

        System.out.println("  F1. @Resource(name = \"channel\")，容器里有 3 个 PayChannel 但没有叫 channel 的：");
        Throwable e1 = failOf(
                AlipayChannel.class, WechatChannel.class, UnionPayChannel.class, ResourceWrongName.class);
        if (e1 != null) {
            showChain(e1);
        }

        System.out.println();
        System.out.println("  F2. @Resource 不给 name，字段名叫 channel（同样对不上）：");
        Throwable e2 = failOf(
                AlipayChannel.class, WechatChannel.class, UnionPayChannel.class, ResourceDefaultName.class);
        if (e2 != null) {
            showChain(e2);
            System.out.println("      注意它报的是 NoUnique 而不是 NoSuchBean —— 默认名查不到时，"
                    + "@Resource 会退回按类型，");
            System.out.println("      于是踩的是 F1 那条路之外的另一条：跟 @Autowired 一模一样的多候选爆炸。");
        }
    }

    // ------------------------------------------------------------------ 工具

    private static AnnotationConfigApplicationContext ctxOf(Class<?>... classes) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(classes);
        ctx.refresh();
        return ctx;
    }

    /**
     * 造一个注定失败的容器。
     *
     * <p>踩过的坑：注入失败<b>不是</b>在 {@code getBean} 时抛的 —— 被注入的那个 bean 如果不是
     * lazy、也没人依赖它，它自己在 {@code preInstantiateSingletons()} 的循环里就被造了，
     * 于是 {@code refresh()} 直接抛 {@code UnsatisfiedDependencyException}。
     */
    private static Throwable failOf(Class<?>... classes) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(classes);
        try {
            ctx.refresh();
            return null;
        } catch (Throwable t) {
            return t;
        } finally {
            try {
                ctx.close();
            } catch (Throwable ignore) {
                // refresh 失败的容器 close 时可能说 BeanFactory not initialized，这里不关心
            }
        }
    }

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }

    /** 打异常链：类名 + message 每一行。Spring 的 message 经常是多行带候选清单的，别截断。 */
    private static void showChain(Throwable e) {
        int i = 0;
        for (Throwable t = e; t != null && i < 4; t = t.getCause(), i++) {
            System.out.printf("      [%d] %s%n", i, t.getClass().getSimpleName());
            for (String line : String.valueOf(t.getMessage()).split("\n")) {
                if (!line.isBlank()) {
                    System.out.println("          " + line.trim());
                }
            }
            if (t.getCause() == t) {
                break;
            }
        }
    }

    private static void showFrames(Throwable t) {
        int n = 0;
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            if (c.startsWith("org.springframework") || c.startsWith("com.newt.java48")) {
                System.out.printf("      at %s.%s(%s:%d)%n", c, e.getMethodName(), e.getFileName(), e.getLineNumber());
                if (++n >= 12) {
                    break;
                }
            }
        }
    }
}
