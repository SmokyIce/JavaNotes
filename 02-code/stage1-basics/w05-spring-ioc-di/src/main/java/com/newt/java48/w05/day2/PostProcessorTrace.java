package com.newt.java48.w05.day2;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * W05 Day 2 · 第二段：<b>BeanPostProcessor</b>。
 *
 * <p>四段：
 * <ul>
 *   <li>A —— 两个后处理器对每个 Bean 计数，打出「谁被处理了几次」的表格；</li>
 *   <li>B —— 在 {@code @PostConstruct} 里打印栈，回答「这个回调是谁调的」；</li>
 *   <li>C —— 三个追问的结论（含后处理器自己会不会被后处理）；</li>
 *   <li>D —— 多个后处理器之间按 {@code Ordered} 排序的实测顺序。</li>
 * </ul>
 *
 * <p>A 段跑起来会看到两条 {@code INFO} 级告警：{@code 'xxx' ... is not eligible for getting
 * processed by all BeanPostProcessors}。它<b>不是错误</b>，而是「这个 Bean 在后处理器注册完成前
 * 就被创建了、拿不到全部后处理（包括 AOP 代理）」的官方提示 —— 事务 / 异步失效的一类病根。
 */
public class PostProcessorTrace {

    public static void main(String[] args) {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
    }

    // ------------------------------------------------------------------ A

    private static void sectionA() {
        System.out.println();
        System.out.println("=== A. 两个后处理器，对每个 Bean 各跑一遍 ===");
        System.out.println("  （下面那几条 INFO 告警是故意触发的，见文件头注释）");
        System.out.println();

        TraceBpp.reset();
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(BppConfig.class);

        System.out.println();
        System.out.printf("  共 %d 个 Bean 走过 before，%d 个走过 after%n",
                TraceBpp.beforeTotal(), TraceBpp.afterTotal());
        System.out.println();
        printTable();

        System.out.println();
        System.out.println("  读法：");
        System.out.println("    正常的 Bean（plainBean / lifecycleRepo）        2/2 —— 两个后处理器各处理一次；");
        System.out.println("    bppFirst、bppSecond                            0/0 —— 同一批后处理器是「先全部");
        System.out.println("        getBean 收集，再统一注册」（PostProcessorRegistrationDelegate:257~265），");
        System.out.println("        互相都看不见；它们的 @PostConstruct 却被更早注册的 CommonAnnotationBPP 执行了；");
        System.out.println("    earlyDependent                                 0/0 —— 被 bppFirst @Autowired，");
        System.out.println("        比后处理器还早出生，连代理都套不上（就是那条 is not eligible 告警）。");

        ctx.close();
    }

    private static void printTable() {
        Map<String, int[]> c = TraceBpp.counts();
        List<String> names = new ArrayList<>(c.keySet());
        // 0/0 的 Bean 根本不会进 map，得手工补上才能看见「它被漏掉了」
        for (String must : new String[]{"earlyDependent", "bppFirst", "bppSecond", "plainBean", "lifecycleRepo"}) {
            if (!c.containsKey(must)) {
                names.add(must);
            }
        }
        Collections.sort(names);

        int internalBefore = 0;
        int internalAfter = 0;
        int internalRows = 0;

        System.out.printf("  %-48s %7s %7s%n", "beanName", "before", "after");
        System.out.println("  " + "-".repeat(64));
        for (String name : names) {
            int[] v = c.getOrDefault(name, new int[2]);
            if (name.startsWith("org.springframework")) {
                internalBefore += v[0];
                internalAfter += v[1];
                internalRows++;
                continue;
            }
            System.out.printf("  %-48s %7d %7d%n", name, v[0], v[1]);
        }
        System.out.println("  " + "-".repeat(64));
        if (internalRows == 0) {
            System.out.println("  （本次容器很干净，没有 org.springframework.* 的 bean 被计数）");
        } else {
            System.out.printf("  %-48s %7d %7d%n",
                    "容器内部 bean（org.springframework.*，共 " + internalRows + " 个）", internalBefore, internalAfter);
        }
    }

    @Configuration
    public static class BppConfig {

        /** 后处理器用 static 方法声明 —— 否则会逼容器提前实例化本配置类，多一条无谓的告警。 */
        @Bean
        public static TraceBpp bppFirst() {
            return new TraceBpp("first", 1);
        }

        @Bean
        public static TraceBpp bppSecond() {
            return new TraceBpp("second", 2);
        }

        @Bean
        public EarlyDependent earlyDependent() {
            return new EarlyDependent();
        }

        @Bean
        public PlainBean plainBean() {
            return new PlainBean();
        }

        /** 再加一个普通 Bean，让「2/2」不止一个样本。 */
        @Bean
        public LifecycleRepo lifecycleRepo() {
            return new LifecycleRepo();
        }
    }

    // ------------------------------------------------------------------ B

    private static void sectionB() {
        System.out.println();
        System.out.println("=== B. @PostConstruct 到底是谁调的：读栈 ===");

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(StackConfig.class);
        ctx.close();

        System.out.println();
        System.out.println("  栈上这几帧就是答案（行号对应 Spring 5.3.39）：");
        System.out.println("    InitDestroyAnnotationBeanPostProcessor$LifecycleElement.invoke:389 —— 真正反射调用我那个方法");
        System.out.println("      ↑ LifecycleMetadata.invokeInitMethods:333（按 @PostConstruct 的元数据逐个调）");
        System.out.println("      ↑ InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization:157");
        System.out.println("        （方法体在 154 行声明；157 行就是那句 metadata.invokeInitMethods）");
        System.out.println("      ↑ applyBeanPostProcessorsBeforeInitialization:440（一次遍历）");
        System.out.println("      ↑ initializeBean:1796（同一个方法里 1800 行才轮到 invokeInitMethods）");
        System.out.println("      ↑ doCreateBean:620 → createBean:542");
        System.out.println("  所以 @PostConstruct 的执行者是后处理器，而它的「载体」是 CommonAnnotationBeanPostProcessor：");
        System.out.println("    该类第 205 行 setInitAnnotationType(PostConstruct.class)。");
    }

    @Configuration
    static class StackConfig {

        @Bean
        public StackDumpBean stackDumpBean() {
            return new StackDumpBean();
        }
    }

    // ------------------------------------------------------------------ C

    private static void sectionC() {
        System.out.println();
        System.out.println("=== C. 三个追问的结论 ===");
        System.out.println("  ① @PostConstruct 是谁调的？");
        System.out.println("     CommonAnnotationBeanPostProcessor（继承 InitDestroyAnnotationBeanPostProcessor）。");
        System.out.println("     证据在 B 段的栈上，不是记忆 —— 而且这个后处理器是 AnnotationConfigApplicationContext");
        System.out.println("     自动注册的：classpath 上有 javax.annotation-api 才会有（JDK 11+ 已经把它移出 JDK）。");
        System.out.println("  ② prototype 的销毁回调会不会被调？");
        System.out.println("     不会，一次都不会 —— 见 LifecycleOrder 的 C 段，原因是 AbstractBeanFactory:1942");
        System.out.println("     用 !mbd.isPrototype() 在登记阶段就短路了。");
        System.out.println("  ③ BeanPostProcessor 自己会不会被后处理？");
        System.out.println("     被「更早注册」的处理，被「同批与后来的」漏掉：");
        System.out.println("       · 它的 @PostConstruct 照样执行（CommonAnnotationBPP 是 PriorityOrdered，253 行就注册完了）；");
        System.out.println("       · 但它自己进不了任何后处理器的计数器（A 段 bppFirst / bppSecond 都是 0/0）。");
        System.out.println("     所以「给后处理器加 @Transactional / @Async」基本等于白写 —— 它自己也拿不到代理。");
    }

    // ------------------------------------------------------------------ D

    private static void sectionD() {
        System.out.println();
        System.out.println("=== D. 多个后处理器的顺序由 Ordered 决定 ===");
        System.out.println("  plainBean 上的调用序列：" + TraceBpp.seq());
        System.out.println("  first(order=1) 永远在 second(order=2) 前面，before 与 after 各自按同一个顺序遍历。");
        System.out.println("  Spring 自己的顺序是 PriorityOrdered → Ordered → 剩下的（无序的按注册顺序）。");
        System.out.println("  @Order / Ordered 只影响「谁先被调用」，不影响 Bean 的创建顺序。");
    }
}
