package com.newt.java48.w06.day2;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 五种通知里最容易被想当然的两种：{@code @AfterReturning} 与 {@code @AfterThrowing}。
 *
 * <p>它们都没有返回值 —— 这不是写法习惯，是**物理上不可能**：
 * 一个 {@code void} 方法拿什么去替换 {@code proceed()} 的返回值，又拿什么去「取消」一个已经抛出的异常？
 *
 * <pre>
 *   S1  @AfterReturning 能不能改返回值：重赋值 vs 改对象内部
 *   S2  returning 的类型对不上时，通知还执行吗（静默）
 *   S3  @AfterThrowing 能不能吞异常
 *   S4  throwing 声明窄了：别的异常它接不到
 *   S5  在 @AfterThrowing 里抛新异常：原始异常还在吗
 *   S6  唯一能吞的是 @Around（try/catch 包住 proceed）
 *   S7  四种通知在一个方法上的完整顺序
 * </pre>
 */
public class AdviceTypes {

    static void sec(String s) {
        System.out.println();
        System.out.println("== " + s + " ==");
    }

    /** 问「某条通知这次执行了没有」—— 别去数整个 LOG，里面本来就混着别的通知。 */
    static boolean has(String key) {
        return ThrowAdvice.LOG.stream().anyMatch(s -> s.contains(key));
    }

    static String describe(Throwable t) {
        return t == null ? "null（原异常已彻底丢失）"
                : t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    // ---------------------------------------------------------------- 业务

    public static class Pay {

        public StringBuilder sb() {
            return new StringBuilder("hello");
        }

        public int total() {
            return 100;
        }

        public String charge(String order) {
            return "charged:" + order;
        }

        public void boom() {
            throw new IllegalArgumentException("boom-arg");
        }

        public void fail() {
            throw new IllegalStateException("fail-state");
        }

        public void crash() {
            throw new IllegalStateException("crash-state");
        }

        public void crash2() {
            throw new IllegalStateException("crash2-state");
        }
    }

    // ---------------------------------------------------------------- 切面

    @Aspect
    public static class RetAdvice {

        static final List<String> LOG = Collections.synchronizedList(new ArrayList<>());

        /**
         * 两件事一起做：把参数 {@code ret} 换掉（不可能生效），把对象内部改掉（生效）。
         */
        @AfterReturning(pointcut = "execution(java.lang.StringBuilder com.newt.java48.w06.day2.AdviceTypes.Pay.sb())",
                returning = "ret")
        public void afterSb(JoinPoint jp, StringBuilder ret) {
            ret.append("-touched");              // 改对象内部
            ret = new StringBuilder("REPLACED"); // 换引用：出了这个方法就没了
            LOG.add("@AfterReturning sb 执行了");
        }

        /**
         * {@code returning} 声明成 String，但目标方法返回 int。
         *
         * <p>这一条是「静默失效」的样本：既不在启动时报错，也不在调用的时报错，
         * 只是**永远不执行**。理由见输出。
         */
        @AfterReturning(pointcut = "execution(int com.newt.java48.w06.day2.AdviceTypes.Pay.total())",
                returning = "ret")
        public void afterTotal(JoinPoint jp, String ret) {
            LOG.add("@AfterReturning total 执行了（不该出现）");
        }
    }

    @Aspect
    public static class ThrowAdvice {

        static final List<String> LOG = Collections.synchronizedList(new ArrayList<>());

        /** 想「吞掉」异常：只能看着它，然后什么也做不了。 */
        @AfterThrowing(pointcut = "execution(void com.newt.java48.w06.day2.AdviceTypes.Pay.boom())",
                throwing = "ex")
        public void afterBoom(JoinPoint jp, Throwable ex) {
            LOG.add("@AfterThrowing boom 收到 " + ex.getClass().getSimpleName() + ":" + ex.getMessage());
        }

        /** {@code throwing} 声明成 IllegalStateException，于是 IllegalArgumentException 它接不到。 */
        @AfterThrowing(pointcut = "execution(void com.newt.java48.w06.day2.AdviceTypes.Pay.*())",
                throwing = "ex")
        public void afterState(JoinPoint jp, IllegalStateException ex) {
            LOG.add("@AfterThrowing State 收到 " + jp.getSignature().getName()
                    + " " + ex.getMessage());
        }

        /** 在通知里抛新异常：原异常会被顶掉还是被挂住？ */
        @AfterThrowing(pointcut = "execution(void com.newt.java48.w06.day2.AdviceTypes.Pay.crash())",
                throwing = "ex")
        public void afterCrash(JoinPoint jp, IllegalStateException ex) {
            throw new RuntimeException("advice-thrown", ex);
        }

        /** 对照组：抛新异常时**不**把原异常传进去 —— 看框架会不会自己帮你挂。 */
        @AfterThrowing(pointcut = "execution(void com.newt.java48.w06.day2.AdviceTypes.Pay.crash2())",
                throwing = "ex")
        public void afterCrash2(JoinPoint jp, IllegalStateException ex) {
            throw new RuntimeException("advice-bare");
        }
    }

    @Aspect
    public static class OrderProbe {

        static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

        @Around("execution(String com.newt.java48.w06.day2.AdviceTypes.Pay.charge(String))")
        public Object around(ProceedingJoinPoint pjp) throws Throwable {
            EVENTS.add("@Around 进入");
            try {
                Object r = pjp.proceed();
                EVENTS.add("@Around 拿到返回值");
                return r;
            } finally {
                EVENTS.add("@Around finally");
            }
        }

        @Before("execution(String com.newt.java48.w06.day2.AdviceTypes.Pay.charge(String))")
        public void before(JoinPoint jp) {
            EVENTS.add("@Before");
        }

        @AfterReturning(pointcut = "execution(String com.newt.java48.w06.day2.AdviceTypes.Pay.charge(String))",
                returning = "ret")
        public void afterReturning(JoinPoint jp, Object ret) {
            EVENTS.add("@AfterReturning 拿到 " + ret);
        }

        @After("execution(String com.newt.java48.w06.day2.AdviceTypes.Pay.charge(String))")
        public void after(JoinPoint jp) {
            EVENTS.add("@After");
        }
    }

    /** 唯一能吞异常的通知。 */
    @Aspect
    public static class SwallowProbe {

        static final List<String> LOG = Collections.synchronizedList(new ArrayList<>());

        @Around("execution(void com.newt.java48.w06.day2.AdviceTypes.Pay.crash())")
        public Object swallow(ProceedingJoinPoint pjp) throws Throwable {
            try {
                return pjp.proceed();
            } catch (IllegalStateException e) {
                LOG.add("@Around 抓到了 " + e.getMessage() + "，吞掉并正常返回");
                return null;
            }
        }
    }

    // ---------------------------------------------------------------- 装配

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class Cfg {
        @Bean
        public Pay pay() {
            return new Pay();
        }

        @Bean
        public RetAdvice retAdvice() {
            return new RetAdvice();
        }

        @Bean
        public ThrowAdvice throwAdvice() {
            return new ThrowAdvice();
        }

        @Bean
        public OrderProbe orderProbe() {
            return new OrderProbe();
        }
    }

    /** 只装「能吞异常」的那个切面，否则 S6 会被 ThrowAdvice 干扰。 */
    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class SwallowCfg {
        @Bean
        public Pay pay() {
            return new Pay();
        }

        @Bean
        public SwallowProbe swallowProbe() {
            return new SwallowProbe();
        }
    }

    public static void main(String[] args) {
        System.out.println("AdviceTypes · @AfterReturning 改得动吗，@AfterThrowing 吞得掉吗");
        System.out.println("Spring = " + org.springframework.core.SpringVersion.getVersion());

        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(Cfg.class);
        Pay pay = ctx.getBean(Pay.class);

        sec("S1 @AfterReturning 能不能改返回值");
        StringBuilder ret = pay.sb();
        System.out.println("  调用方拿到的 = " + ret);
        System.out.println("  切面日志     = " + RetAdvice.LOG);
        System.out.println("  -> 「-touched」出现了、「REPLACED」没出现：");
        System.out.println("     ret 是个局部参数，重新赋值只改了那个引用，调用方的引用没变；");
        System.out.println("     但 ret 指向的**对象**是共用的，改内部就会生效 —— 所以可变对象「看起来能改返回值」，其实只是副作用");

        sec("S2 returning 的类型对不上：通知静默不执行");
        int total = pay.total();
        System.out.println("  pay.total() = " + total);
        System.out.println("  RetAdvice.LOG = " + RetAdvice.LOG);
        System.out.println("  -> LOG 里只有 sb 那条，total 那条**从来没执行**，启动不报错、调用不报错。");
        System.out.println("     原因：Advice 的 returning 参数类型要能接住返回值，接不住就跳过；");
        System.out.println("     这一条比「切点写错」更难查 —— 切点明明对的，注解明明在，就是不跑");

        sec("S3 @AfterThrowing 能不能吞掉异常");
        try {
            pay.boom();
            System.out.println("  没抛（不该发生）");
        } catch (IllegalArgumentException e) {
            System.out.println("  调用方仍然收到 " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        System.out.println("  ThrowAdvice.LOG = " + ThrowAdvice.LOG);
        System.out.println("  -> 通知收到了异常，异常照样往上跑。@AfterThrowing 没有返回值、也没有「取消」这条路，");
        System.out.println("     它和 catch 块**不是**一回事：它是「事后围观」，不是「接住」");

        sec("S4 throwing 声明窄了，别的异常接不到");
        ThrowAdvice.LOG.clear();
        try {
            pay.boom();
        } catch (IllegalArgumentException ignored) {
            // 只看通知有没有被调
        }
        System.out.println("  boom() 抛 IllegalArgumentException：");
        System.out.println("    afterBoom（切点=boom，throwing=Throwable）      被调 = " + has("boom"));
        System.out.println("    afterState（切点=Pay.*()，throwing=IllegalState） 被调 = " + has("State") + "  ← 切点命中、类型接不住");
        ThrowAdvice.LOG.clear();
        try {
            pay.fail();
        } catch (IllegalStateException ignored) {
            // 同上
        }
        System.out.println("  fail() 抛 IllegalStateException：");
        System.out.println("    afterBoom（切点=boom，压根不匹配 fail）          被调 = " + has("boom"));
        System.out.println("    afterState（切点=Pay.*()，类型对得上）            被调 = " + has("State")
                + "  " + ThrowAdvice.LOG);
        System.out.println("  -> 和 S2 同一个机制：类型接不住就跳过，安静地跳过");
        System.out.println("     所以「异常日志有时有有时没有」，先看 throwing 声明的类型够不够宽（写 Throwable 最省事）");

        sec("S5 在 @AfterThrowing 里抛新异常");
        try {
            pay.crash();
        } catch (Exception e) {
            System.out.println("  crash()  通知 throw new RuntimeException(\"advice-thrown\", ex)：");
            System.out.println("    调用方收到 " + e.getClass().getName() + ": " + e.getMessage());
            System.out.println("    cause = " + describe(e.getCause()));
        }
        try {
            pay.crash2();
        } catch (Exception e) {
            System.out.println("  crash2() 通知 throw new RuntimeException(\"advice-bare\")，不传 ex：");
            System.out.println("    调用方收到 " + e.getClass().getName() + ": " + e.getMessage());
            System.out.println("    cause = " + describe(e.getCause()));
            System.out.println("    suppressed 个数 = " + e.getSuppressed().length);
        }
        System.out.println("  -> 新异常顶掉了原异常，而且框架**不会**帮你把原异常挂到哪儿去：");
        System.out.println("     第一组还能看到 cause，是我在代码里自己传的；第二组什么都不传，原异常就彻底没了。");
        System.out.println("     对比 W04 的 whenComplete：那里框架至少塞进了 suppressed，这里连 suppressed 都没有");

        sec("S6 唯一能吞异常的是 @Around");
        AnnotationConfigApplicationContext swallowCtx =
                new AnnotationConfigApplicationContext(SwallowCfg.class);
        Pay swallowPay = swallowCtx.getBean(Pay.class);
        try {
            swallowPay.crash();
            System.out.println("  crash() 正常返回了，调用方不知道出过事");
        } catch (RuntimeException e) {
            System.out.println("  仍然抛了：" + e.getClass().getSimpleName());
        }
        System.out.println("  SwallowProbe.LOG = " + SwallowProbe.LOG);
        System.out.println("  -> 只有 @Around 能吞，因为它有 proceed()，可以在外面套 try/catch；");
        System.out.println("     但吞掉业务异常通常是坏主意：事务、监控、调用方全都看不到失败");

        sec("S7 四种通知在同一个方法上的顺序");
        OrderProbe.EVENTS.clear();
        String charged = pay.charge("A-1");
        System.out.println("  返回 = " + charged);
        for (String e : OrderProbe.EVENTS) {
            System.out.println("    " + e);
        }
        System.out.println("  -> @AfterReturning 在 @After 之前、在 @Around 拿到返回值之前；");
        System.out.println("     @After 是 proceed() 外面那层 finally 里的，所以它在「返回值已确定、还没交出去」之间执行");

        System.out.println();
        System.out.println("一句话：只有 @Around 有 proceed()，所以只有它能改返回值、能吞异常；"
                + "其余四种通知都是「路过看一眼」，参数类型接不住就静默跳过。");

        swallowCtx.close();
        ctx.close();
    }
}
