package com.newt.java48.w05.day3;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.SimpleThreadScope;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * W05 Day 3 · 第一段：{@code singleton} 与 {@code prototype} 的实测对照。
 *
 * <p>三小节：
 * <ul>
 *   <li>A —— 两条作用域模型跑一遍三条判据：两次 {@code getBean} 是不是同一个 / 进不进单例池 /
 *       销毁回调会不会被调；</li>
 *   <li>B —— {@code thread} 作用域（Spring 自带 {@link SimpleThreadScope}）：证明作用域不是硬编码的
 *       枚举，而是容器里的一个 {@code Map<String, Scope>}；</li>
 *   <li>C —— 四种作用域的判据矩阵（{@code request} / {@code session} 的结论来自
 *       {@code WebScopeTrap}，在非 Web 容器里根本取不到）。</li>
 * </ul>
 */
public class ScopeMatrix {

    public static void main(String[] args) {
        sectionA();
        sectionB();
        sectionC();
    }

    // ------------------------------------------------------------------ A

    private static void sectionA() {
        System.out.println("=== A. singleton vs prototype：三条判据 ===");
        ScopeProbe.reset();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(BasicConfig.class);

        System.out.println("  refresh() 刚返回（此刻还没手工 getBean 过）：");
        System.out.printf("    singletonProbe  构造 %d 次  ← 单例在 refresh 里被预实例化了%n",
                ScopeProbe.created("singletonProbe"));
        System.out.printf("    prototypeProbe  构造 %d 次  ← 原型不预造，等着被要%n",
                ScopeProbe.created("prototypeProbe"));

        ScopeProbe s1 = ctx.getBean("singletonProbe", ScopeProbe.class);
        ScopeProbe s2 = ctx.getBean("singletonProbe", ScopeProbe.class);
        ScopeProbe p1 = ctx.getBean("prototypeProbe", ScopeProbe.class);
        ScopeProbe p2 = ctx.getBean("prototypeProbe", ScopeProbe.class);

        System.out.println();
        System.out.printf("  singleton  两次 getBean：%s / %s → 是同一个？ %s%n",
                s1.describe(), s2.describe(), s1 == s2);
        System.out.printf("  prototype  两次 getBean：%s / %s → 是同一个？ %s%n",
                p1.describe(), p2.describe(), p1 == p2);

        System.out.println();
        System.out.printf("  containsSingleton(singletonProbe) = %s%n",
                ctx.getBeanFactory().containsSingleton("singletonProbe"));
        System.out.printf("  containsSingleton(prototypeProbe) = %s%n",
                ctx.getBeanFactory().containsSingleton("prototypeProbe"));
        System.out.printf("  取过两次之后构造计数：singleton %d / prototype %d%n",
                ScopeProbe.created("singletonProbe"), ScopeProbe.created("prototypeProbe"));

        ctx.close();

        System.out.println();
        System.out.printf("  close() 之后：singletonProbe @PreDestroy %d 次 / destroy %d 次%n",
                ScopeProbe.preDestroyed("singletonProbe"), ScopeProbe.destroyed("singletonProbe"));
        System.out.printf("  close() 之后：prototypeProbe @PreDestroy %d 次 / destroy %d 次%n",
                ScopeProbe.preDestroyed("prototypeProbe"), ScopeProbe.destroyed("prototypeProbe"));
        System.out.println();
        System.out.println("  读法：作用域是两个问题合成的一个答案 ——「造几个」和「谁管埋」。");
        System.out.println("        singleton 造 1 个且容器负责销毁；prototype 每次一个且**谁取谁负责**，");
        System.out.println("        因为 AbstractBeanFactory:1944 的 !mbd.isPrototype() 在登记阶段就短路了。");
    }

    @Configuration
    static class BasicConfig {

        @Bean
        public ScopeProbe singletonProbe() {
            return new ScopeProbe("singletonProbe");
        }

        @Bean
        @Scope("prototype")
        public ScopeProbe prototypeProbe() {
            return new ScopeProbe("prototypeProbe");
        }
    }

    // ------------------------------------------------------------------ B

    private static void sectionB() {
        System.out.println();
        System.out.println("=== B. thread 作用域：作用域是「容器里的一本登记册」，可插拔 ===");
        ScopeProbe.reset();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

        System.out.println("  先试试能不能把 singleton 也塞进登记册：");
        try {
            ctx.getBeanFactory().registerScope("singleton", new SimpleThreadScope());
            System.out.println("  ！居然成功了 —— 如果看到这行说明结论错了");
        } catch (Throwable e) {
            System.out.println("    " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("    ↑ AbstractBeanFactory.registerScope:1041，抛在 1045 —— 这两个名字不许被替换，");
            System.out.println("      因为它们的逻辑写在 doGetBean 的 if 分支里，不在 scopes 这张表里。");
        }

        System.out.println("  refresh() 前 registerScope(\"thread\", new SimpleThreadScope())");
        ctx.getBeanFactory().registerScope("thread", new SimpleThreadScope());
        ctx.register(ThreadScopeConfig.class);
        ctx.refresh();

        System.out.println("  容器里登记过的作用域 = "
                + Arrays.toString(ctx.getBeanFactory().getRegisteredScopeNames())
                + "   ← 只有我们手工登记的这一个");
        System.out.println("  （singleton / prototype 不在这个名单里：它们是写死在 doGetBean 里的两个 if 分支）");

        ScopeProbe m1 = ctx.getBean("threadProbe", ScopeProbe.class);
        ScopeProbe m2 = ctx.getBean("threadProbe", ScopeProbe.class);
        System.out.println("  上面那条 WARN 是 SimpleThreadScope 自己打的：它的 registerDestructionCallback"
                + " 只写日志、不登记 ——");
        System.out.println("  也就是说 Spring 自带的 thread 作用域连「管埋」的接口都不实现（又一个只管造不管埋）。");
        System.out.printf("  main 线程两次 getBean：%s / %s → 同一个？ %s%n",
                m1.describe(), m2.describe(), m1 == m2);

        AtomicReference<ScopeProbe> other = new AtomicReference<>();
        Thread t = new Thread(() -> other.set(ctx.getBean("threadProbe", ScopeProbe.class)), "worker-A");
        t.start();
        joinQuietly(t);
        System.out.printf("  worker-A 线程 getBean：%s → 与 main 的是同一个？ %s%n",
                other.get().describe(), other.get() == m1);
        System.out.printf("  累计构造 %d 个实例（2 个线程各一个）%n", ScopeProbe.created("threadProbe"));
        System.out.println("  SimpleThreadScope 内部就是一个 ThreadLocal —— 所以「作用域」= 「一份按某个 key 找实例的存储」；");
        System.out.println("  换 key 就是换作用域：thread / request / session 全都只是 key 不同。");
        ctx.close();
        System.out.printf("  close() 之后 threadProbe 的销毁计数 = %d —— 容器连调的地方都没有%n",
                ScopeProbe.destroyed("threadProbe"));
    }

    @Configuration
    static class ThreadScopeConfig {

        @Bean
        @Scope("thread")
        public ScopeProbe threadProbe() {
            return new ScopeProbe("threadProbe");
        }
    }

    private static void joinQuietly(Thread t) {
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------ C

    private static void sectionC() {
        System.out.println();
        System.out.println("=== C. 四种作用域的判据矩阵（本次全部实测） ===");
        System.out.println();
        System.out.println("  作用域      | 两次 getBean 同一个？ | 进单例池？ | close() 触发销毁回调？ | 非 Web 容器里");
        System.out.println("  ----------- | --------------------- | ---------- | ---------------------- | ------------");
        System.out.println("  singleton   | 是                    | 是         | 是（@PreDestroy + destroy 各 1 次） | 正常");
        System.out.println("  prototype   | 否                    | 否         | 否（登记阶段就被短路）             | 正常");
        System.out.println("  request     | 取不到                | 否         | 不会发生                           | 抛 ISE（见 WebScopeTrap A）");
        System.out.println("  session     | 取不到                | 否         | 不会发生                           | 抛 ISE（见 WebScopeTrap B）");
        System.out.println();
        System.out.println("  注意最后两行的措辞：request / session 不是「行为不同」，是**这个容器压根没有这个作用域**。");
        System.out.println("  它们由 spring-web 的 RequestScope / SessionScope 提供，只在 WebApplicationContext 里注册。");
    }
}
