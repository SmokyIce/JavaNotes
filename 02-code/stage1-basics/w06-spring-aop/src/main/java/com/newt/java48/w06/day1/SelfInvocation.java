package com.newt.java48.w06.day1;

import org.springframework.aop.framework.AopContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 为什么 this.xxx() 不走代理 —— 以及三种修法。
 *
 * <p>结论一句话：代理只活在「别人拿到的那根引用」上。类内部用 this 调用，
 * 编译出来是 invokevirtual GreetingImpl.hi，跟代理对象没有任何关系。
 *
 * <p>五段实验：
 * <pre>
 *   S1  outer()            -> this.hi()                 before = 1（只有外层被拦）
 *   S2  outerViaSelf()     -> self.hi()，self 是注入的  before = 2
 *   S3  outerViaAopContext -> AopContext.currentProxy() before = 2（需 exposeProxy=true）
 *   S4  self 到底是什么类   -> 双层代理：@Lazy 解析代理 + AOP 代理
 *   S5  没有 exposeProxy 的容器调 currentProxy() -> IllegalStateException 原文
 * </pre>
 */
public class SelfInvocation {

    @Configuration
    @EnableAspectJAutoProxy(exposeProxy = true)
    static class ExposeConfig {
        @Bean
        public Greeting greeting() {
            return new GreetingImpl();
        }

        @Bean
        public TraceAspect traceAspect() {
            return new TraceAspect();
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class PlainConfig {
        @Bean
        public Greeting greeting() {
            return new GreetingImpl();
        }

        @Bean
        public TraceAspect traceAspect() {
            return new TraceAspect();
        }
    }

    static void sec(String s) {
        System.out.println();
        System.out.println("== " + s + " ==");
    }

    static void show(String label, String result) {
        System.out.println("  " + label + " 返回 = " + result);
        System.out.println("     " + TraceAspect.counts() + "   被拦方法 = " + TraceAspect.intercepted());
    }

    public static void main(String[] args) throws Exception {
        System.out.println("SelfInvocation · this.xxx() 为什么不走代理");

        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(ExposeConfig.class);
        Greeting bean = ctx.getBean(Greeting.class);
        System.out.println("容器里的 greeting 类名 = " + bean.getClass().getName());
        System.out.println("isJdkDynamicProxy = " + AopUtils.isJdkDynamicProxy(bean));

        sec("S1 outer() 内部用 this.hi()");
        TraceAspect.reset();
        show("outer(", bean.outer("S1"));
        System.out.println("  -> 业务确实调了 hi，但 before 只有 1：");
        System.out.println("     hi 这次是 invokevirtual GreetingImpl.hi，没经过 handler，也就没有切面");

        sec("S2 outerViaSelf() 内部用 self.hi()（self 由容器注入）");
        TraceAspect.reset();
        show("outerViaSelf(", bean.outerViaSelf("S2"));
        System.out.println("  -> before 变 2：self 指向的是代理，不是 this");

        sec("S3 outerViaAopContext() 用 AopContext.currentProxy()");
        TraceAspect.reset();
        show("outerViaAopContext(", bean.outerViaAopContext("S3"));
        System.out.println("  -> 同样 2 次；代价是要开 exposeProxy，且必须同一个线程（ThreadLocal）");

        sec("S4 注入进来的 self 到底是什么");
        Greeting self = ((GreetingImpl) ((org.springframework.aop.framework.Advised) bean)
                .getTargetSource().getTarget()).selfRef();
        System.out.println("  self 类名 = " + (self == null ? "null" : self.getClass().getName()));
        System.out.println("  self 与容器 bean 同类 = " + (self != null && self.getClass() == bean.getClass()));
        System.out.println("  self == 容器 bean      = " + (self == bean));
        System.out.println("  -> 同类不算证据：JDK 代理类按「接口组合」缓存，两个不同的代理完全可以共用同一个 $Proxy。");
        org.springframework.aop.framework.Advised selfAdvised =
                (org.springframework.aop.framework.Advised) self;
        System.out.println("  self 的 TargetSource 类 = " + selfAdvised.getTargetSource().getClass().getName());
        Object selfTarget = selfAdvised.getTargetSource().getTarget();
        System.out.println("  self 的 target 类       = "
                + (selfTarget == null ? "null" : selfTarget.getClass().getName()));
        System.out.println("  self 的 target == bean  = " + (selfTarget == bean));
        System.out.println("  -> TargetSource 这一层才分得清：");
        System.out.println("     self 的 TargetSource 是 @Lazy 的匿名解析器（每次 getTarget 回容器 resolve 一次），");
        System.out.println("     它 resolve 出来的正是容器里那个 AOP 代理 —— 所以真实链路是");
        System.out.println("     @Lazy 解析代理 → AOP 代理 → GreetingImpl 真身，三层。");
        System.out.println("     不加 @Lazy 就是「自己注入自己」的循环依赖，构造器注入会直接 BeanCurrentlyInCreationException");

        sec("S5 没开 exposeProxy 的容器里调 currentProxy()");
        AnnotationConfigApplicationContext plainCtx =
                new AnnotationConfigApplicationContext(PlainConfig.class);
        Greeting plain = plainCtx.getBean(Greeting.class);
        try {
            plain.outerViaAopContext("S5");
            System.out.println("  没抛异常（不该发生）");
        } catch (IllegalStateException e) {
            System.out.println("  IllegalStateException: " + e.getMessage());
        }
        System.out.println("  -> 这是「配了没生效」的典型：@EnableAspectJAutoProxy 少了 exposeProxy=true，");
        System.out.println("     编译期不报、启动不报，第一次调到这里才炸");
        plainCtx.close();

        sec("S6 三行代码的数字对照");
        System.out.println("  this.hi()                       before=1  （外层 1 次，内层 0 次）");
        System.out.println("  self.hi()   （@Lazy 注入自己）  before=2");
        System.out.println("  currentProxy().hi()             before=2");
        System.out.println("  -> 三种写法业务结果一模一样，差别只在「有没有回到代理」。");
        System.out.println("     这就是为什么 @Transactional 在同类方法调用里静默失效：事务也是靠代理开闭的。");

        System.out.println();
        System.out.println("一句话：代理不会跟着 this 走 —— 谁拿着代理引用调方法，谁才有切面。");
        ctx.close();
    }
}
