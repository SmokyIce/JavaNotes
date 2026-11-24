package com.newt.java48.w05.day4;

import com.newt.java48.w05.day4.pay.AlipayChannel;
import com.newt.java48.w05.day4.pay.PayChannel;
import com.newt.java48.w05.day4.pay.UnionPayChannel;
import com.newt.java48.w05.day4.pay.WechatChannel;
import com.newt.java48.w05.support.Quiet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * W05 Day 4 · 第三段：构造器注入的两个细节 + {@code ObjectProvider}。
 *
 * <p>四小节：
 * <ul>
 *   <li>A —— 单构造器<b>不用写</b> {@code @Autowired}（4.3 起的行为，很多人还在写）；</li>
 *   <li>B —— 多构造器一个都不标：有默认构造器就悄悄用默认的，没有就直接炸；</li>
 *   <li>C —— 多构造器标一个：选中的是标了的那个；</li>
 *   <li>D —— {@code ObjectProvider}：把「取对象」从启动期推迟到调用期（原型 / 可选 / 多候选都在这里收场）。</li>
 * </ul>
 *
 * <p>还有一条负结果放在 B 段尾巴：{@code @Autowired} 的「按名字回退」对<b>字段</b>有效，
 * 对<b>构造器参数</b>要额外条件（参数名得真的能被反射读到）。
 */
public class CtorInject {

    public static void main(String[] args) {
        Quiet.springRefreshWarning();
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
    }

    // ------------------------------------------------------------------ A

    /** 单候选依赖，用来看「单构造器免注解」。 */
    interface Notifier {
        String kind();
    }

    @Component
    static class LogNotifier implements Notifier {
        @Override
        public String kind() {
            return "log";
        }
    }

    /** 只有一个构造器，<b>没有</b> {@code @Autowired}。 */
    @Component
    static class SingleCtor {
        final Notifier notifier;

        SingleCtor(Notifier notifier) {
            this.notifier = notifier;
        }
    }

    private static void sectionA() {
        System.out.println("=== A. 单构造器：不写 @Autowired 也能注入 ===");

        AnnotationConfigApplicationContext ctx = ctxOf(LogNotifier.class, SingleCtor.class);
        SingleCtor bean = ctx.getBean(SingleCtor.class);
        System.out.printf("  构造器上没有任何注解 → notifier = %s%n", bean.notifier.kind());
        System.out.println("  Spring 4.3 起：类里只有一个构造器时它就是唯一的候选，不用标。");
        System.out.println("  这也是「构造器注入」被推荐之后，代码里 @Autowired 越来越少的原因。");
        ctx.close();
    }

    // ------------------------------------------------------------------ B

    /** 两个构造器，一个都不标，且<b>没有</b>无参构造器。 */
    @Component
    static class TwoNoMark {
        final String which;

        TwoNoMark(Notifier notifier) {
            this.which = "ctor(Notifier)";
        }

        TwoNoMark(Notifier notifier, String tag) {
            this.which = "ctor(Notifier, String)";
        }
    }

    /** 两个构造器 + 一个无参构造器，一个都不标 —— 悄悄挑无参的那个。 */
    @Component
    static class TwoWithDefault {
        final String which;

        TwoWithDefault() {
            this.which = "无参构造器";
        }

        TwoWithDefault(Notifier notifier) {
            this.which = "ctor(Notifier)";
        }
    }

    private static void sectionB() {
        System.out.println();
        System.out.println("=== B. 多构造器，一个都不标 ===");

        System.out.println("  B1. 没有无参构造器：");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(LogNotifier.class, TwoNoMark.class);
        try {
            ctx.refresh();
            System.out.println("      ✗ 没炸，与预期不符");
        } catch (Throwable t) {
            Throwable root = rootOf(t);
            System.out.println("      " + t.getClass().getSimpleName() + ": "
                    + String.valueOf(t.getMessage()).split("\n")[0]);
            System.out.println("      ↑ 根因是 " + root.getClass().getSimpleName() + ": "
                    + String.valueOf(root.getMessage()).split("\n")[0]);
        }
        ctx.close();

        System.out.println();
        System.out.println("  B2. 有无参构造器（同样一个都不标）：");
        AnnotationConfigApplicationContext ctx2 = ctxOf(LogNotifier.class, TwoWithDefault.class);
        System.out.printf("      实际走的是「%s」—— 不报错，参数版被静默忽略%n",
                ctx2.getBean(TwoWithDefault.class).which);
        System.out.println("      这个比报错危险：依赖是 null（或字段没赋上），运行时才 NPE。");
        ctx2.close();
    }

    // ------------------------------------------------------------------ C

    /** 两个构造器，标一个。 */
    @Component
    static class TwoMarked {
        final String which;

        TwoMarked(Notifier notifier) {
            this.which = "ctor(Notifier)";
        }

        @Autowired
        TwoMarked(Notifier notifier, String tag) {
            this.which = "ctor(Notifier, String)  ← 被 @Autowired 选中";
        }
    }

    private static void sectionC() {
        System.out.println();
        System.out.println("=== C. 多构造器 + 标一个 @Autowired ===");

        AnnotationConfigApplicationContext ctx = ctxOf(LogNotifier.class, StringBeanConfig.class,
                TwoMarked.class);
        System.out.printf("  实际走的是「%s」%n", ctx.getBean(TwoMarked.class).which);
        System.out.println("  规则：多构造器时，标了 @Autowired 的那个赢；标了多个会报错。");
        ctx.close();
    }

    /** 给 String 参数提供一个 bean（Spring 不会自动给 String 造值）。 */
    @Configuration
    static class StringBeanConfig {
        @Bean
        String tag() {
            return "tag";
        }
    }

    // ------------------------------------------------------------------ D

    /** 原型 bean 的计数器：每次造新对象 tag 就变。 */
    @Configuration
    static class PrototypeConfig {
        private final AtomicInteger seq = new AtomicInteger();

        @Bean
        @Scope("prototype")
        PayChannel tempChannel() {
            return () -> "temp-" + seq.incrementAndGet();
        }
    }

    /** 用 {@code ObjectProvider} 收一切：没有就是 null，多个也是 null。 */
    @Component
    static class ProviderUser {
        @Autowired
        ObjectProvider<PayChannel> provider;
    }

    private static void sectionD() {
        System.out.println();
        System.out.println("=== D. ObjectProvider：把「取」推迟到调用时 ===");

        System.out.println("  D1. 容器里只有一个 prototype 的 PayChannel：");
        AnnotationConfigApplicationContext ctx = ctxOf(PrototypeConfig.class, ProviderUser.class);
        ProviderUser user = ctx.getBean(ProviderUser.class);
        PayChannel a = user.provider.getObject();
        PayChannel b = user.provider.getObject();
        System.out.printf("      provider.getObject() 两次 → %s / %s%n", a.tag(), b.tag());
        System.out.printf("      是同一个对象吗：%s（原型，所以每次都是新的）%n", a == b);
        System.out.println("      关键：注入的是「取的对象」不是「值」—— 容器此时并没有解析 PayChannel。");
        ctx.close();

        System.out.println();
        System.out.println("  D2. 容器里一个 PayChannel 都没有：");
        AnnotationConfigApplicationContext ctx2 = ctxOf(ProviderUser.class);
        ProviderUser u2 = ctx2.getBean(ProviderUser.class);
        System.out.printf("      getIfAvailable() → %s%n", u2.provider.getIfAvailable());
        System.out.printf("      getIfUnique()    → %s%n", u2.provider.getIfUnique());
        try {
            u2.provider.getObject();
        } catch (Throwable t) {
            System.out.printf("      getObject()      → %s: %s%n", t.getClass().getSimpleName(),
                    String.valueOf(t.getMessage()).split("\n")[0]);
        }
        ctx2.close();

        System.out.println();
        System.out.println("  D3. 容器里有 3 个 PayChannel（就是 A 段让 @Autowired 爆炸的那种局面）：");
        AnnotationConfigApplicationContext ctx3 = ctxOf(AlipayChannel.class, WechatChannel.class,
                UnionPayChannel.class, ProviderUser.class);
        ProviderUser u3 = ctx3.getBean(ProviderUser.class);
        System.out.printf("      getIfUnique()  → %s（多个候选 → null，不抛）%n", u3.provider.getIfUnique());
        try {
            u3.provider.getIfAvailable();
        } catch (Throwable t) {
            Throwable root = rootOf(t);
            System.out.printf("      getIfAvailable() → %s（注意：它没兜住多候选）%n", root.getClass().getSimpleName());
        }
        ctx3.close();
        System.out.println("      两种方法只差一个 if：Unique 会把 NoUnique 吞成 null，Available 不会。");
    }

    // ------------------------------------------------------------------ E

    /**
     * 构造器参数名叫 {@code wechatChannel}，没写 {@code @Qualifier} —— 能不能像字段那样靠名字回退？
     *
     * <p>实测答案：<b>不能</b>。字段名一定写在 class 文件里，参数名不一定 —— 它要么来自
     * {@code -parameters}（反射直接读），要么来自 {@code -g} 生成的 {@code LocalVariableTable}。
     * javac 默认只写 {@code LineNumberTable}，于是反射看到的参数名是 {@code arg0}，
     * 名字回退无从下手，直接退化成 A 段那个多候选爆炸。
     */
    @Component
    static class CtorParamName {
        final PayChannel c;

        CtorParamName(PayChannel wechatChannel) {
            this.c = wechatChannel;
        }
    }

    private static void sectionE() {
        System.out.println();
        System.out.println("=== E. 同一个「按名字回退」，换成构造器参数就不灵了 ===");

        java.lang.reflect.Parameter p = CtorParamName.class.getDeclaredConstructors()[0].getParameters()[0];
        System.out.printf("  反射读到的参数名 = %s（isNamePresent=%s）%n", p.getName(), p.isNamePresent());
        System.out.println("  javac 没加 -parameters/-g，参数名只剩 arg0 —— 拿它去和 bean 名对，当然对不上。");

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(AlipayChannel.class, WechatChannel.class, UnionPayChannel.class, CtorParamName.class);
        try {
            ctx.refresh();
            System.out.println("  ✗ 注入成功了 —— 说明参数名其实读得到（可能加了 -parameters）");
        } catch (Throwable t) {
            Throwable root = rootOf(t);
            System.out.println("  结果：" + root.getClass().getSimpleName() + ": "
                    + String.valueOf(root.getMessage()).split("\n")[0]);
        }
        ctx.close();
        System.out.println("  对照 InjectionChoice 的 B 段：字段名 wechatChannel 什么都不写就能注入。");
        System.out.println("  差别不在「按不按名字」，在「名字拿不拿得到」。");
    }

    // ------------------------------------------------------------------ 工具

    private static AnnotationConfigApplicationContext ctxOf(Class<?>... classes) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(classes);
        ctx.refresh();
        return ctx;
    }

    private static Throwable rootOf(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }
}
