package com.newt.java48.w05.day2;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * W05 Day 2 · 第一段：<b>Bean 生命周期</b> —— 把顺序打出来，而不是背下来。
 *
 * <p>四段：
 * <ul>
 *   <li>A —— 一个 Bean 挂六种回调，加两个后处理器钩子，看完整序列（①~⑨）；</li>
 *   <li>B —— 多个 Bean 的销毁是<b>逆序</b>，且依赖方先死；</li>
 *   <li>C —— prototype 的销毁回调<b>一次都不会被调</b>（源头是登记那一步就短路了）；</li>
 *   <li>D —— {@code @Bean} 的 {@code destroyMethod} 默认会<b>推断</b> {@code close()}；
 *       写 {@code destroyMethod = ""} 才关掉。</li>
 * </ul>
 *
 * <p>时间戳精确到 0.001 ms —— 六个回调经常落在同一毫秒里，只打毫秒会看不出先后。
 */
public class LifecycleOrder {

    private static final long T0 = System.nanoTime();

    public static void main(String[] args) {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
    }

    /** 全类共用的一把尺子：容器里的回调都从这里打时间戳。 */
    public static void log(String step, String detail) {
        double ms = (System.nanoTime() - T0) / 1_000_000.0;
        System.out.printf("  %8.3f ms  %-40s %s%n", ms, step, detail == null ? "" : detail);
    }

    // ------------------------------------------------------------------ A

    private static void sectionA() {
        System.out.println();
        System.out.println("=== A. 六种回调 + 两个后处理器钩子，真实顺序 ===");
        System.out.println("  （① 构造 → ② BPP.before → ③ @PostConstruct → ④ afterPropertiesSet");
        System.out.println("    → ⑤ initMethod → ⑥ BPP.after；销毁是 ⑦ → ⑧ → ⑨）");
        System.out.println();

        log("T0 容器创建开始", "new AnnotationConfigApplicationContext(...)");
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(LifecycleConfig.class);
        log("refresh() 返回", "此时 Bean 已经全部初始化完");

        System.out.println();
        log("业务调用", ctx.getBean(LifecycleBean.class).describe());
        System.out.println();

        log("ctx.close()", "开始销毁单例");
        ctx.close();
        log("close() 返回", "销毁回调全部跑完");

        System.out.println();
        System.out.println("  读法：② 在 ③ 之前 —— 说明 @PostConstruct 不是容器直接调的，");
        System.out.println("        而是在 postProcessBeforeInitialization 的遍历里被「另一个后处理器」调起来的；");
        System.out.println("  ④ 一定在 ⑤ 之前 —— afterPropertiesSet 是接口，initMethod 是配置，接口优先；");
        System.out.println("  ⑥ 在所有初始化之后 —— AOP 代理就在这一步把原对象换成代理对象。");
    }

    // ------------------------------------------------------------------ B

    private static void sectionB() {
        System.out.println();
        System.out.println("=== B. 多个 Bean 的销毁顺序：逆序 + 依赖方先死 ===");

        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(DestroyOrderConfig.class);
        System.out.println("  容器就绪，现在 close()：");
        ctx.close();

        System.out.println("  Alpha 后造、依赖 Beta → 先被销毁；Beta 先造、被依赖 → 后销毁。");
        System.out.println("  原因不是「逆序」这一条：DefaultSingletonBeanRegistry 会先销毁 dependentBeanMap 里的下游。");
    }

    @Configuration
    static class DestroyOrderConfig {

        @Bean
        public Beta beta() {
            return new Beta();
        }

        @Bean
        public Alpha alpha(Beta beta) {
            return new Alpha(beta);
        }
    }

    // ------------------------------------------------------------------ C

    private static void sectionC() {
        System.out.println();
        System.out.println("=== C. prototype 的销毁回调：一次都不会被调 ===");

        PrototypeBean.reset();
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(PrototypeScopeConfig.class);

        PrototypeBean p1 = ctx.getBean(PrototypeBean.class);
        PrototypeBean p2 = ctx.getBean(PrototypeBean.class);
        System.out.printf("  取了两次，p1 == p2 : %s（累计实例 %d）%n", p1 == p2, PrototypeBean.instances());

        ctx.close();
        System.out.printf("  close() 之后：销毁回调被调 %d 次（期望 0）%n", PrototypeBean.destroys());
        System.out.println("  不是「忘了调」，是根本没登记：");
        System.out.println("  AbstractBeanFactory.registerDisposableBeanIfNecessary:1942");
        System.out.println("    if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) { ... }");
        System.out.println("  prototype 在进 if 之前就被短路了，销毁阶段自然也找不到它 —— 谁取谁负责关。");
    }

    @Configuration
    static class PrototypeScopeConfig {

        @Bean
        @Scope("prototype")
        public PrototypeBean prototypeBean() {
            return new PrototypeBean();
        }
    }

    // ------------------------------------------------------------------ D

    private static void sectionD() {
        System.out.println();
        System.out.println("=== D. @Bean 默认会「推断」销毁方法：close() / shutdown() ===");

        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(InferDestroyConfig.class);
        ctx.getBean(AutoCloseBean.class);
        ctx.getBean(ManualOffBean.class);
        ctx.close();

        System.out.printf("  AutoCloseBean.close()  被调 %d 次（@Bean 没写 destroyMethod）%n", AutoCloseBean.closes());
        System.out.printf("  ManualOffBean.close()  被调 %d 次（@Bean(destroyMethod = \"\")）%n", ManualOffBean.closes());
        System.out.println("  默认值是 AbstractBeanDefinition.INFER_METHOD（字符串 \"(inferred)\"）：");
        System.out.println("  容器自己去找 close() / shutdown() 并调掉 —— 所以别随便给自己类起这两个名字。");
    }

    @Configuration
    static class InferDestroyConfig {

        @Bean
        public AutoCloseBean autoCloseBean() {
            return new AutoCloseBean();
        }

        @Bean(destroyMethod = "")
        public ManualOffBean manualOffBean() {
            return new ManualOffBean();
        }
    }
}
