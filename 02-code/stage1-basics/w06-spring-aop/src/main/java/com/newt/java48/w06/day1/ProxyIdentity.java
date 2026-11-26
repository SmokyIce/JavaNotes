package com.newt.java48.w06.day1;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Arrays;

/**
 * 同一套类交给 Spring：{@code @Aspect} 之后，容器里那个对象还是原来那个吗？
 *
 * <p>七段实验：
 * <pre>
 *   S1  手工 new 的实例，切面完全不认识它            -> 计数 0
 *   S2  容器 getBean(Greeting.class)                 -> 是代理，isJdkDynamicProxy = true
 *   S2b 容器 getBean(GreetingImpl.class)             -> BeanNotOfRequiredTypeException（按实现类取不到）
 *   S3  调一次 hi()：三个通知各几次 + this/target 是谁
 *   S4  proxyTargetClass=true                        -> 换成 CGLIB 子类
 *   S5  没有接口的类                                  -> 自动 CGLIB；final 方法静默不拦
 *   S6  切面 Bean 自己被代理了吗                      -> 没有（它的切点不匹配自己）
 * </pre>
 */
public class ProxyIdentity {

    /** 默认：proxyTargetClass = false，有接口就走 JDK 代理。 */
    @Configuration
    @EnableAspectJAutoProxy
    static class JdkConfig {
        @Bean
        public Greeting greeting() {
            return new GreetingImpl();
        }

        @Bean
        public PlainJob plainJob() {
            return new PlainJob();
        }

        @Bean
        public TraceAspect traceAspect() {
            return new TraceAspect();
        }
    }

    /** 强制 CGLIB：proxyTargetClass = true。Spring Boot 2.x 起这是默认值。 */
    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class CglibConfig {
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

    public static void main(String[] args) throws Exception {
        System.out.println("ProxyIdentity · 容器里那个对象到底是谁");
        System.out.println("Spring = " + org.springframework.core.SpringVersion.getVersion());

        sec("S1 手工 new 出来的实例，切面根本不知道它的存在");
        TraceAspect.reset();
        Greeting manual = new GreetingImpl();
        System.out.println("  类名 = " + manual.getClass().getName());
        System.out.println("  调用 hi = " + manual.hi("S1"));
        System.out.println("  " + TraceAspect.counts());
        System.out.println("  -> @Aspect 只作用于「容器造出来的 Bean」，new 出来的对象连注册都没注册过");

        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(JdkConfig.class);

        sec("S2 容器里的 Greeting 是什么类");
        Greeting bean = ctx.getBean(Greeting.class);
        System.out.println("  类名 = " + bean.getClass().getName());
        System.out.println("  isAopProxy         = " + AopUtils.isAopProxy(bean));
        System.out.println("  isJdkDynamicProxy  = " + AopUtils.isJdkDynamicProxy(bean));
        System.out.println("  isCglibProxy       = " + AopUtils.isCglibProxy(bean));
        System.out.println("  实现的接口          = " + Arrays.toString(bean.getClass().getInterfaces()));
        System.out.println("  bean == 手工 new 的那个 = " + (bean == manual));
        System.out.println("  -> 默认 proxyTargetClass=false + 目标有接口 ⇒ JDK 代理；");
        System.out.println("     容器里放的是代理，真身被 Advised 包在里面");

        sec("S2b 按实现类取这个 Bean（两种取法，两个不同的错）");
        try {
            GreetingImpl wrong = ctx.getBean(GreetingImpl.class);
            System.out.println("  按类型取到了（不该发生）：" + wrong.getClass().getName());
        } catch (NoSuchBeanDefinitionException e) {
            System.out.println("  getBean(GreetingImpl.class) -> NoSuchBeanDefinitionException: " + e.getMessage());
        }
        try {
            GreetingImpl wrong = ctx.getBean("greeting", GreetingImpl.class);
            System.out.println("  按名字+类型取到了（不该发生）：" + wrong.getClass().getName());
        } catch (BeanNotOfRequiredTypeException e) {
            System.out.println("  getBean(\"greeting\", GreetingImpl.class) -> BeanNotOfRequiredTypeException: " + e.getMessage());
        }
        System.out.println("  -> 两个错要分清：");
        System.out.println("     按类型找，连「候选」都算不上（Bean 定义声明的类型是接口 Greeting），所以 NoSuchBean；");
        System.out.println("     按名字找，名字先命中，再拿实例对类型 —— 代理只实现接口、不是 GreetingImpl 的子类，所以 BeanNotOfRequiredType。");
        System.out.println("     Spring Boot 2.x 默认 proxyTargetClass=true，就是为了让「按实现类注入」还能用");

        sec("S3 调一次 hi()：三个通知各几次，this 与 target 分别是谁");
        TraceAspect.reset();
        String r = bean.hi("S3");
        System.out.println("  返回 = " + r);
        System.out.println("  " + TraceAspect.counts());
        System.out.println("  @Before 收到的 this   = " + TraceAspect.LAST_THIS.getClass().getName());
        System.out.println("  @Before 收到的 target = " + TraceAspect.LAST_TARGET.getClass().getName());
        System.out.println("  this == target        = " + (TraceAspect.LAST_THIS == TraceAspect.LAST_TARGET));
        System.out.println("  this == 容器里那个 bean = " + (TraceAspect.LAST_THIS == bean));
        Object realTarget = ((Advised) bean).getTargetSource().getTarget();
        System.out.println("  Advised 里包着的真身    = " + realTarget.getClass().getName());
        System.out.println("  -> 一次调用 = before 1 + around 1 + after 1；");
        System.out.println("     getThis() 给你代理，getTarget() 给你真身 —— 这俩永远不是同一个对象");

        sec("S4 换成 proxyTargetClass=true");
        AnnotationConfigApplicationContext cglibCtx =
                new AnnotationConfigApplicationContext(CglibConfig.class);
        Greeting cglibBean = cglibCtx.getBean(Greeting.class);
        System.out.println("  类名 = " + cglibBean.getClass().getName());
        System.out.println("  isJdkDynamicProxy = " + AopUtils.isJdkDynamicProxy(cglibBean)
                + "   isCglibProxy = " + AopUtils.isCglibProxy(cglibBean));
        System.out.println("  父类 = " + cglibBean.getClass().getSuperclass().getName());
        TraceAspect.reset();
        System.out.println("  调用 hi = " + cglibBean.hi("S4") + "   " + TraceAspect.counts());
        System.out.println("  -> CGLIB = 生成子类覆写方法，所以它「是」GreetingImpl（父类就是它），");
        System.out.println("     代价是①不能代理 final 类 ②不能代理 final 方法 ③要能被继承（private 构造器不行）");
        cglibCtx.close();

        sec("S5 没有接口的类：Spring 怎么选");
        PlainJob job = ctx.getBean(PlainJob.class);
        TraceAspect.reset();
        System.out.println("  类名 = " + job.getClass().getName());
        System.out.println("  isCglibProxy = " + AopUtils.isCglibProxy(job)
                + "   isJdkDynamicProxy = " + AopUtils.isJdkDynamicProxy(job));
        System.out.println("  调用 run()    = " + job.run() + "   " + TraceAspect.counts());
        int afterRun = TraceAspect.BEFORE.get();
        System.out.println("  调用 sealed() = " + job.sealed() + "   " + TraceAspect.counts());
        System.out.println("  sealed 这前后 before 增量 = " + (TraceAspect.BEFORE.get() - afterRun));
        System.out.println("  -> 没接口时自动退回 CGLIB（不需要 proxyTargetClass）；");
        System.out.println("     而 final 方法覆写不了 ⇒ 切面静默不执行，启动日志一个字都不提");

        sec("S6 切面 Bean 自己被代理了吗");
        TraceAspect aspect = ctx.getBean(TraceAspect.class);
        System.out.println("  类名 = " + aspect.getClass().getName());
        System.out.println("  isAopProxy = " + AopUtils.isAopProxy(aspect));
        System.out.println("  -> 没有：它的切点只匹配 GreetingImpl / PlainJob，自己不在里面。");
        System.out.println("     （但如果有别的切面覆盖到它，切面之间也会互相增强 —— 那才是最难查的一类问题）");

        sec("S7 一次调用的通知顺序");
        TraceAspect.reset();
        bean.hi("S7");
        TraceAspect.EVENTS.forEach(e -> System.out.println("  " + e));
        System.out.println("  " + TraceAspect.counts());
        System.out.println("  -> 实测顺序：@Around 进入 → @Before → 目标方法 → @After → @Around 返回。");
        System.out.println("     只有 @Around 能包住别人（它有 proceed()），其余通知都落在 proceed() 前后；");
        System.out.println("     @After 在 Around 的 proceed() 返回之前就跑完了 —— 它就是那段 try/finally 里的 finally");

        System.out.println();
        System.out.println("一句话：@Aspect 不改你的类，它让容器在注册期把「真身」换成代理；"
                + "拿到代理的人都走切面，拿到真身的人（this、new 出来的、被提前创建的 Bean）都不走。");
        ctx.close();
    }
}
