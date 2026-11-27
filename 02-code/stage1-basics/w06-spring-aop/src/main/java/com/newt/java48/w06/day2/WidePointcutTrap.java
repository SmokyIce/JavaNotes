package com.newt.java48.w06.day2;

import com.newt.java48.w06.day2.shop.Shop;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 反面教材：切点写宽到 {@code com.newt.java48.w06.day2..*.*(..)}，容器**起不来**。
 *
 * <p>过程的因果链只有四步，但四步都不在明面上：
 * <ol>
 *   <li>{@code @Configuration} 类本身也是一个 Bean，而且默认是 CGLIB 增强的（为了 {@code @Bean} 方法返回单例）；</li>
 *   <li>{@code ..} 在 AspectJ 里「零个或多个子包」，所以 {@code day2..*} 连 {@code day2} 包里的
 *       配置类 {@code WidePointcutTrap$BrokenCfg} 一起匹配；</li>
 *   <li>于是配置类的 {@code shop()} 工厂方法被切面拦下 —— 那一枪是在「造 shop 这个 Bean」的过程中打出去的；</li>
 *   <li>通知要执行，就得先拿到切面 Bean；而切面 Bean 正在被造（{@code refresh} 还没造到它），
 *       于是 {@code beforeSingletonCreation} 判定「正在创建中」，抛 {@code BeanCurrentlyInCreationException}。</li>
 * </ol>
 *
 * <p>结论：这不是「循环依赖」，是两个 Bean 的**创建顺序被一个不该命中的切点绑在了一起**。
 * 报错信息里那句 {@code Is there an unresolvable circular reference?} 会把人的注意力引向依赖注入，
 * 而真正的线索是栈里那行 {@code BrokenCfg$$EnhancerBySpringCGLIB$$....shop(<generated>)}。
 */
public class WidePointcutTrap {

    /**
     * 把 refresh 自己打的那条 WARN 压掉 —— 我们要的是**被捕获的**异常链，不是 JUL 的转述。
     *
     * <p>注意压的是 {@code org.springframework} 这个**父** logger：W05 Day 4 踩过的坑是
     * 「JUL 的 logger 名是 {@code LogFactory.getLog(getClass())} 里的子类名」，
     * 按 {@code AbstractApplicationContext} 去设 {@code Level.OFF} 压不掉，
     * 因为真正的名字是 {@code …context.annotation.AnnotationConfigApplicationContext}。
     * 设父级一劳永逸：子 logger 没有自己显式 level 时会继承。
     */
    static void quietSpring() {
        Logger.getLogger("org.springframework").setLevel(Level.OFF);
    }

    static void sec(String s) {
        System.out.println();
        System.out.println("== " + s + " ==");
    }

    // ---------------------------------------------------------------- 靶子

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class BrokenCfg {
        @Bean
        public Shop shop() {
            return new Shop();
        }

        @Bean
        public WideAspect wideAspect() {
            return new WideAspect();
        }
    }

    @Aspect
    static class WideAspect {
        static final List<String> HITS = Collections.synchronizedList(new ArrayList<>());

        /** 致命的那一行：{@code ..} 把配置类也收了进来。 */
        @Before("execution(* com.newt.java48.w06.day2..*.*(..))")
        public void before(JoinPoint jp) {
            HITS.add(jp.getTarget().getClass().getSimpleName()
                    + "#" + jp.getSignature().getName());
        }
    }

    // ---------------------------------------------------------------- 修法

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class FixedCfg {
        @Bean
        public Shop shop() {
            return new Shop();
        }

        @Bean
        public ShopOnlyAspect shopOnlyAspect() {
            return new ShopOnlyAspect();
        }
    }

    /** 修法一：把范围收到业务包 —— 配置类在 {@code day2} 而不在 {@code day2.shop}，天然不在里面。 */
    @Aspect
    static class ShopOnlyAspect {
        static final List<String> HITS = Collections.synchronizedList(new ArrayList<>());

        @Before("execution(* com.newt.java48.w06.day2.shop..*.*(..))")
        public void before(JoinPoint jp) {
            HITS.add(jp.getTarget().getClass().getSimpleName()
                    + "#" + jp.getSignature().getName());
        }
    }

    public static void main(String[] args) {
        quietSpring();
        System.out.println("WidePointcutTrap · 切点写宽到把容器自己拦了");

        sec("S1 宽切点启动容器");
        try {
            new AnnotationConfigApplicationContext(BrokenCfg.class).close();
            System.out.println("  起来了（不该发生）");
        } catch (BeansException e) {
            System.out.println("  抛 " + e.getClass().getName());
            printChain(e);
            System.out.println();
            System.out.println("  栈里那条真正的线索：");
            printGeneratedFrames(e);
            System.out.println();
            System.out.println("  -> 读法：BrokenCfg 自己被 CGLIB 增强了（为了让 @Bean 返回单例），");
            System.out.println("     所以 container 调 shop() 走的是代理 → 命中宽切点 → 通知要 wideAspect 这个 Bean");
            System.out.println("     → wideAspect 正在创建中 → BeanCurrentlyInCreationException。");
            System.out.println("     报错文案说的是 circular reference，但其实依赖关系一点都没绕，绕的是「创建顺序」");
        }

        sec("S2 修法：把 execution 的范围收到业务包（day2.shop..*）");
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(FixedCfg.class)) {
            Shop shop = ctx.getBean(Shop.class);
            shop.buy("apple");
            System.out.println("  容器起来，切点命中 = " + ShopOnlyAspect.HITS);
            System.out.println("  -> 配置类在 day2 包、业务类在 day2.shop 包，包路径一分，事故就没了");
        }

        System.out.println();
        System.out.println("一句话：切点表达式里的 .. 是「零个或多个子包」，不是「只在子包里」；"
                + "而 @Configuration / @Aspect 自己也是 Bean，会一起被匹配 —— 写宽了不是「多记几条日志」，是容器起不来。");
    }

    static void printChain(Throwable e) {
        Throwable t = e;
        int depth = 0;
        while (t != null && depth < 8) {
            String msg = t.getMessage() == null ? "" : t.getMessage();
            if (msg.length() > 150) {
                msg = msg.substring(0, 150) + "…";
            }
            System.out.println("    " + "  ".repeat(depth) + "↳ " + t.getClass().getSimpleName()
                    + ": " + msg);
            t = t.getCause();
            depth++;
        }
    }

    /** 打印 CGLIB 生成的那一帧（去重）—— 它出现的位置就是「谁被拦了」。 */
    static void printGeneratedFrames(Throwable e) {
        List<String> seen = new ArrayList<>();
        Throwable t = e;
        while (t != null) {
            for (StackTraceElement f : t.getStackTrace()) {
                if (f.getClassName().contains("EnhancerBySpringCGLIB")) {
                    // 类名里的 $$<hash> 每次运行都会变，抹掉再比，才谈得上去重。
                    // 注意替换串里的 $ 要 quoteReplacement —— 否则 "$" 被当成组引用，直接 IllegalArgumentException
                    String cls = f.getClassName().replaceAll(
                            "\\$\\$EnhancerBySpringCGLIB\\$\\$.+$",
                            java.util.regex.Matcher.quoteReplacement("$$EnhancerBySpringCGLIB$$<hash>"));
                    String line = cls + "." + f.getMethodName() + "(<generated>)";
                    if (!seen.contains(line)) {
                        seen.add(line);
                        System.out.println("    at " + line);
                    }
                }
            }
            t = t.getCause();
        }
    }
}
