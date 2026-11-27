package com.newt.java48.w06.day2;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 两个切面套在同一个方法上，谁在外层？—— 只看 {@code @Order} 的数字，数字小的在外。
 *
 * <pre>
 *   S1  alpha @Order(1) + beta @Order(2) —— 注册顺序也是 alpha 先
 *   S2  把两个数字对调：现在 beta 在外层（注册顺序没变）
 *   S3  都不写 @Order：同 order 时按**切面 Bean 的名字**排，跟注册顺序无关
 *   S4  顺序错了会怎样：@Before 的执行顺序与 @After 正好相反
 * </pre>
 *
 * <p>口径说明：{@code @Order} 是由 {@code AspectJAwareAdvisorAutoProxyCreator} 在排序顾问链时用的，
 * 数小的**先进入、后退出** —— 也就是在洋葱的外层。
 */
public class AspectOrder {

    public static class Biz {
        public String doWork(String x) {
            return "done:" + x;
        }
    }

    /** 通知写在基类里，子类只负责带自己的标签和 {@code @Order}。 */
    public abstract static class BaseAspect {

        public static final List<String> SEQ = Collections.synchronizedList(new ArrayList<>());

        static final String PC =
                "execution(String com.newt.java48.w06.day2.AspectOrder.Biz.doWork(String))";

        abstract String tag();

        @Around(PC)
        public Object around(ProceedingJoinPoint pjp) throws Throwable {
            SEQ.add(tag() + ".around-in");
            try {
                return pjp.proceed();
            } finally {
                SEQ.add(tag() + ".around-out");
            }
        }

        @Before(PC)
        public void before(JoinPoint jp) {
            SEQ.add(tag() + ".before");
        }

        @After(PC)
        public void after(JoinPoint jp) {
            SEQ.add(tag() + ".after");
        }
    }

    @Aspect
    @Order(1)
    public static class AlphaFirst extends BaseAspect {
        @Override
        String tag() {
            return "alpha";
        }
    }

    @Aspect
    @Order(2)
    public static class BetaSecond extends BaseAspect {
        @Override
        String tag() {
            return "beta";
        }
    }

    @Aspect
    @Order(2)
    public static class AlphaSecond extends BaseAspect {
        @Override
        String tag() {
            return "alpha";
        }
    }

    @Aspect
    @Order(1)
    public static class BetaFirst extends BaseAspect {
        @Override
        String tag() {
            return "beta";
        }
    }

    /** 没有任何 @Order。名字故意一个靠前一个靠后。 */
    @Aspect
    public static class AaaAspect extends BaseAspect {
        @Override
        String tag() {
            return "aaa";
        }
    }

    @Aspect
    public static class ZzzAspect extends BaseAspect {
        @Override
        String tag() {
            return "zzz";
        }
    }

    // ---------------------------------------------------------------- 装配

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AscCfg {
        @Bean
        public Biz biz() {
            return new Biz();
        }

        @Bean
        public AlphaFirst alpha() {
            return new AlphaFirst();
        }

        @Bean
        public BetaSecond beta() {
            return new BetaSecond();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class SwappedCfg {
        @Bean
        public Biz biz() {
            return new Biz();
        }

        /** 注册顺序仍然是 alpha → beta，但 @Order 对调了。 */
        @Bean
        public AlphaSecond alpha() {
            return new AlphaSecond();
        }

        @Bean
        public BetaFirst beta() {
            return new BetaFirst();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class NoOrderCfg {
        @Bean
        public Biz biz() {
            return new Biz();
        }

        /** 故意先注册 zzz、后注册 aaa —— 如果结果还是 zzz 在前，就排除了「按名字升序」。 */
        @Bean
        public ZzzAspect zzz() {
            return new ZzzAspect();
        }

        @Bean
        public AaaAspect aaa() {
            return new AaaAspect();
        }
    }

    /** 与上一组正好反过来：先注册 aaa。用来区分「注册顺序」和「字母序」。 */
    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class NoOrderReversedCfg {
        @Bean
        public Biz biz() {
            return new Biz();
        }

        @Bean
        public AaaAspect aaa() {
            return new AaaAspect();
        }

        @Bean
        public ZzzAspect zzz() {
            return new ZzzAspect();
        }
    }

    static void sec(String s) {
        System.out.println();
        System.out.println("== " + s + " ==");
    }

    static List<String> run(Class<?> cfg) {
        BaseAspect.SEQ.clear();
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(cfg)) {
            ctx.getBean(Biz.class).doWork("x");
        }
        return new ArrayList<>(BaseAspect.SEQ);
    }

    static void dump(List<String> seq) {
        for (String s : seq) {
            System.out.println("    " + s);
        }
    }

    public static void main(String[] args) {
        System.out.println("AspectOrder · 两个切面谁在外层");
        System.out.println("Spring = " + org.springframework.core.SpringVersion.getVersion());

        sec("S1 alpha @Order(1) + beta @Order(2)，注册顺序 alpha → beta");
        List<String> s1 = run(AscCfg.class);
        dump(s1);

        sec("S2 只把 @Order 数字对调（注册顺序没变，还是 alpha → beta）");
        List<String> s2 = run(SwappedCfg.class);
        dump(s2);

        sec("S3 都不写 @Order，且故意先注册 zzz 再注册 aaa");
        List<String> s3 = run(NoOrderCfg.class);
        dump(s3);

        sec("S3b 换回来：先注册 aaa，再注册 zzz");
        List<String> s3b = run(NoOrderReversedCfg.class);
        dump(s3b);

        sec("S4 读顺序：谁先进入、谁先退出");
        System.out.println("  S1 的进入顺序 = " + enters(s1));
        System.out.println("  S1 的退出顺序 = " + exits(s1));
        System.out.println("  S2 的进入顺序 = " + enters(s2));
        System.out.println("  S2 的退出顺序 = " + exits(s2));
        System.out.println("  S3 的进入顺序 = " + enters(s3) + "   注册顺序 zzz → aaa");
        System.out.println("  S3b 的进入顺序 = " + enters(s3b) + "   注册顺序 aaa → zzz");
        System.out.println("  -> 进入按 order 升序、退出按 order 降序：数字小的在洋葱**外层**；");
        System.out.println("     对调数字后进出顺序整体翻过来，说明决定权只在 @Order，跟注册顺序无关；");
        System.out.println("     都不写时（同为 LOWEST_PRECEDENCE）顺序跟着**注册顺序**走 ——");
        System.out.println("     S3 与 S3b 的进出顺序随注册顺序翻转，而字母序 aaa < zzz 只解释了其中一组，所以不是按名字排的；");
        System.out.println("     排序是稳定的，并列就保留原序，而原序取决于 Bean 定义的扫描顺序 —— 换个组件扫描方式就可能变");
        System.out.println("  -> 记住这一条就够用：@Before 与 @After 的顺序**天生相反**，");
        System.out.println("     想让「日志切面」包住「事务切面」，就把日志的 order 写小");

        System.out.println();
        System.out.println("一句话：@Order 数字小的在外层（先进后出）；不写就是同一档 LOWEST_PRECEDENCE，"
                + "顺序退化成「切面 Bean 的注册顺序」—— 能用纯属侥幸，要确定就显式写 @Order。");
    }

    static List<String> enters(List<String> seq) {
        List<String> r = new ArrayList<>();
        for (String s : seq) {
            if (s.endsWith(".around-in")) {
                r.add(s.substring(0, s.indexOf('.')));
            }
        }
        return r;
    }

    static List<String> exits(List<String> seq) {
        List<String> r = new ArrayList<>();
        for (String s : seq) {
            if (s.endsWith(".around-out")) {
                r.add(s.substring(0, s.indexOf('.')));
            }
        }
        return r;
    }
}
