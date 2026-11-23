package com.newt.java48.w05.day3;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.util.HashMap;
import java.util.Map;

/**
 * W05 Day 3 · 第二段：<b>作用域的边界</b> —— 非 Web 容器里的 {@code request} / {@code session}，
 * 以及 {@code proxyMode} 到底解决什么问题。
 *
 * <p>四小节：
 * <ul>
 *   <li>A —— {@code @Scope("request")} 在 {@code AnnotationConfigApplicationContext} 里，
 *       <b>refresh 不报错、getBean 才报错</b>，报错原文与栈；</li>
 *   <li>B —— 同一个错在 {@code session} 上重演，确认这不是 request 特有的；</li>
 *   <li>C —— {@code @Scope(proxyMode)} 的两个用途：<b>原型注入单例</b>（每次调用换新实例）、
 *       <b>Web 作用域注入单例</b>（把解析推迟到调用时 —— 报错时机后移，问题没变）；</li>
 *   <li>D —— 自己实现 {@code org.springframework.beans.factory.config.Scope}：
 *       每 2 次 {@code getBean} 换一个实例的怪作用域，
 *       顺带证明自定义作用域的销毁回调是**交给 scope 自己**登记的。</li>
 * </ul>
 */
public class WebScopeTrap {

    public static void main(String[] args) {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
    }

    // ------------------------------------------------------------------ A

    private static void sectionA() {
        System.out.println("=== A. @Scope(\"request\") 在非 Web 容器里 ===");
        ScopeProbe.reset();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(WebScopeConfig.class);
        System.out.println("  new AnnotationConfigApplicationContext(WebScopeConfig.class) 成功了 —— refresh 没有报错。");
        System.out.println("  为什么？因为它不是单例：preInstantiateSingletons 的循环压根不会碰它。");
        System.out.printf("  定义上的 scope = \"%s\"%n",
                ctx.getBeanDefinition("requestProbe").getScope());
        System.out.println("  容器登记过的作用域 = "
                + java.util.Arrays.toString(ctx.getBeanFactory().getRegisteredScopeNames())
                + "   ← 空的，没有任何自定义作用域");

        try {
            ctx.getBean("requestProbe", ScopeProbe.class);
            System.out.println("  ！没抛异常 —— 如果看到这行说明结论错了");
        } catch (Throwable e) {
            printError("getBean(\"requestProbe\")", e, 5);
        }
        System.out.println();
        System.out.println("  源码位置：AbstractBeanFactory.doGetBean（本次 5.3.39 的行号）");
        System.out.println("    363  String scopeName = mbd.getScope();");
        System.out.println("    365  if (!StringUtils.hasLength(scopeName)) throw new IllegalStateException(\"No scope name defined ...\");");
        System.out.println("    367  Scope scope = this.scopes.get(scopeName);");
        System.out.println("    369  if (scope == null) throw new IllegalStateException(\"No Scope registered for scope name '\" + scopeName + \"'\");");
        System.out.println("  这句话是「查表查不到」，不是「这个作用域不能这么用」—— 表里没有就是没有。");
        ctx.close();
    }

    // ------------------------------------------------------------------ B

    private static void sectionB() {
        System.out.println();
        System.out.println("=== B. 换成 session：同一个错，换个名字 ===");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SessionScopeConfig.class);
        try {
            ctx.getBean("sessionProbe", ScopeProbe.class);
        } catch (Throwable e) {
            printError("getBean(\"sessionProbe\")", e, 3);
        }
        System.out.println("  结论：request / session 不是「行为不同」，是**这个容器没有这两个作用域**。");
        System.out.println("  spring-web 的 RequestScope / SessionScope 才把它们注册进 WebApplicationContext；");
        System.out.println("  所以同一段代码在 Spring Boot Web 里跑得好好的，挪到单测里就炸 —— 单测里没有 Web 上下文。");
        ctx.close();
    }

    @Configuration
    static class WebScopeConfig {

        @Bean
        @Scope("request")
        public ScopeProbe requestProbe() {
            return new ScopeProbe("requestProbe");
        }
    }

    @Configuration
    static class SessionScopeConfig {

        @Bean
        @Scope("session")
        public ScopeProbe sessionProbe() {
            return new ScopeProbe("sessionProbe");
        }
    }

    // ------------------------------------------------------------------ C

    private static void sectionC() {
        System.out.println();
        System.out.println("=== C. proxyMode：为什么需要「作用域代理」 ===");
        System.out.println("  场景：一个 singleton 里注入一个 prototype。它拿到的是「当时那一个」，不会自己变。");
        ScopeProbe.reset();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ProxyConfig.class);

        PlainHolder plain = ctx.getBean(PlainHolder.class);
        ProxyHolder proxied = ctx.getBean(ProxyHolder.class);

        System.out.println();
        System.out.println("  C1 没有代理（@Scope(\"prototype\") 直接注入）：");
        System.out.printf("     holder 里的字段是 %s%n", plain.describeHeld());
        for (int i = 0; i < 3; i++) {
            System.out.printf("     第 %d 次调用 probe.id() → %d%n", i + 1, plain.heldId());
        }
        System.out.printf("     原型累计构造 %d 个 —— 注入那一刻的那一个，之后再也不问容器%n",
                ScopeProbe.created("plainProto"));

        System.out.println();
        System.out.println("  C2 加 @Scope(value=\"prototype\", proxyMode=TARGET_CLASS)：");
        System.out.printf("     holder 里的字段类型是 %s（代理，不是 ScopeProbe 本身）%n",
                proxied.heldType());
        for (int i = 0; i < 3; i++) {
            System.out.printf("     第 %d 次调用 probe.id() → %d%n", i + 1, proxied.heldId());
        }
        System.out.printf("     原型累计构造 %d 个 —— 每次方法调用都重新解析目标%n",
                ScopeProbe.created("proxiedProto"));
        System.out.println("  机制：代理类每个方法先调 TargetSource.getTarget() → beanFactory.getBean(name)，");
        System.out.println("        所以「什么时候真的去容器里要」被推迟到了方法调用那一刻。");

        System.out.println();
        System.out.println("  C2b 代理是怎么实现的？注册阶段把一个定义拆成了两个：");
        for (String n : ctx.getBeanDefinitionNames()) {
            if (!n.contains("requestProxy")) {
                continue;
            }
            org.springframework.beans.factory.config.BeanDefinition bd = ctx.getBeanDefinition(n);
            System.out.printf("     %-26s scope=%-10s class=%-46s factoryMethod=%s%n",
                    n,
                    (bd.getScope() == null || bd.getScope().isEmpty()) ? "(singleton)" : bd.getScope(),
                    bd.getBeanClassName() == null ? "-" : bd.getBeanClassName(),
                    bd.getFactoryMethodName() == null ? "-" : bd.getFactoryMethodName());
        }
        System.out.println("     外层 requestProxy 是个单例的 FactoryBean（它的 class 就是 ScopedProxyFactoryBean，产代理），");
        System.out.println("     真身挪到了 scopedTarget.requestProxy，还带着 scope=request 和原来的 factoryMethod；");
        System.out.println("     真身那个定义 class 是空的也正常 —— @Bean 方法产出的定义只有 factoryMethod，没有 class。");
        System.out.println("     所以「代理」不是魔法，是容器在注册期做的一次定义替换。");

        System.out.println();
        System.out.println("  C3 同一个机理用在 Web 作用域上：getBean 不报错了，第一次调用才报");
        try {
            ScopeProbe webProxy = ctx.getBean("requestProxy", ScopeProbe.class);
            System.out.printf("     getBean(\"requestProxy\") 拿到 %s（代理），没抛异常%n", mask(webProxy.getClass().getName()));
            System.out.println("     现在调它的方法：");
            webProxy.id();
            System.out.println("     ！没抛异常 —— 如果看到这行说明结论错了");
        } catch (Throwable e) {
            printError("requestProxy.id()", e, 4);
        }
        System.out.println("    这就是 proxyMode 的真正价值：**把「什么时候解析目标实例」从注入时推迟到使用时**。");
        System.out.println("    Web 里它让单例能安全持有 request 作用域的 Bean；代价是错误从启动期挪到了运行期。");
        ctx.close();
    }

    @Configuration
    static class ProxyConfig {

        @Bean
        @Scope("prototype")
        public ScopeProbe plainProto() {
            return new ScopeProbe("plainProto");
        }

        @Bean
        @Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
        public ScopeProbe proxiedProto() {
            return new ScopeProbe("proxiedProto");
        }

        @Bean
        @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
        public ScopeProbe requestProxy() {
            return new ScopeProbe("requestProxy");
        }

        @Bean
        public PlainHolder plainHolder(@org.springframework.beans.factory.annotation.Qualifier("plainProto")
                                               ScopeProbe plainProto) {
            return new PlainHolder(plainProto);
        }

        @Bean
        public ProxyHolder proxyHolder(@org.springframework.beans.factory.annotation.Qualifier("proxiedProto")
                                               ScopeProbe proxiedProto) {
            return new ProxyHolder(proxiedProto);
        }
    }

    /** 单例里抱着一个普通 prototype 引用。 */
    static class PlainHolder {

        private final ScopeProbe held;

        PlainHolder(ScopeProbe held) {
            this.held = held;
        }

        String describeHeld() {
            return held.describe();
        }

        int heldId() {
            return held.id();
        }
    }

    /** 单例里抱着一个作用域代理。 */
    static class ProxyHolder {

        private final ScopeProbe held;

        ProxyHolder(ScopeProbe held) {
            this.held = held;
        }

        String heldType() {
            return mask(held.getClass().getName());
        }

        int heldId() {
            return held.id();
        }
    }

    // ------------------------------------------------------------------ D

    private static void sectionD() {
        System.out.println();
        System.out.println("=== D. 自定义 Scope：每 2 次 getBean 换一个实例 ===");
        ScopeProbe.reset();

        EveryTwoScope scope = new EveryTwoScope();
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getBeanFactory().registerScope("every-two", scope);
        ctx.register(CustomScopeConfig.class);
        ctx.refresh();

        System.out.println("  registerScope(\"every-two\", scope) + @Scope(\"every-two\") 就装上了 —— 不用改容器源码。");
        for (int i = 1; i <= 4; i++) {
            System.out.printf("  第 %d 次 getBean → %s%n", i, ctx.getBean("everyTwoProbe", ScopeProbe.class).describe());
        }
        System.out.printf("  构造 %d 个 / scope.get() 被调 %d 次 / 轮换时触发销毁回调 %d 次%n",
                ScopeProbe.created("everyTwoProbe"), scope.gets(), ScopeProbe.destroyed("everyTwoProbe"));

        ctx.close();
        System.out.printf("  ctx.close() 之后销毁回调累计 %d 次 —— **没有变化**%n",
                ScopeProbe.destroyed("everyTwoProbe"));
        System.out.println();
        System.out.println("  两条容易忽略的：");
        System.out.println("  ① 自定义作用域的 Bean，销毁回调不是容器调的，是登记时交给 scope 的 ——");
        System.out.println("     AbstractBeanFactory:1952~1960 那个 else 分支：");
        System.out.println("       Scope scope = this.scopes.get(mbd.getScope());");
        System.out.println("       scope.registerDestructionCallback(beanName, new DisposableBeanAdapter(...));");
        System.out.println("     所以「什么时候销毁」由 scope 决定（我们是轮换时调 cb.run()）。");
        System.out.println("  ② ctx.close() 只销毁单例（DefaultSingletonBeanRegistry.destroySingletons），");
        System.out.println("     自定义作用域里的最后一个实例容器不管 —— 真实现里要在 scope 里自己收尾（或实现 remove/clear）。");
    }

    @Configuration
    static class CustomScopeConfig {

        @Bean
        @Scope("every-two")
        public ScopeProbe everyTwoProbe() {
            return new ScopeProbe("everyTwoProbe");
        }
    }

    /**
     * 一个「怪」作用域：第 1、3、5… 次 {@code get()} 换新实例，第 2、4、6… 次复用。
     *
     * <p>只为了证明一件事：{@code Scope} 的五个方法里，容器真正依赖的只有
     * {@code get} / {@code registerDestructionCallback} / {@code remove} 三个，
     * 另外两个（{@code resolveContextualObject} / {@code getConversationId}）是给扩展用的。
     */
    static final class EveryTwoScope implements org.springframework.beans.factory.config.Scope {

        private final Map<String, Object> current = new HashMap<>();
        private final Map<String, Runnable> callbacks = new HashMap<>();
        private int gets;

        @Override
        public Object get(String name, ObjectFactory<?> objectFactory) {
            gets++;
            if (gets % 2 == 1) {                 // 第 1、3、5… 次：换一个新的
                Runnable cb = callbacks.remove(name);
                if (cb != null) {
                    cb.run();                    // 这就是 DisposableBeanAdapter：@PreDestroy + destroy 一起跑
                }
                current.put(name, objectFactory.getObject());
            }
            return current.get(name);
        }

        @Override
        public Object remove(String name) {
            callbacks.remove(name);
            return current.remove(name);
        }

        @Override
        public void registerDestructionCallback(String name, Runnable callback) {
            callbacks.put(name, callback);
        }

        @Override
        public Object resolveContextualObject(String key) {
            return null;
        }

        @Override
        public String getConversationId() {
            return "every-two";
        }

        int gets() {
            return gets;
        }
    }

    // ------------------------------------------------------------------ 工具

    /**
     * CGLIB 类名末尾带一段哈希（{@code $$EnhancerBySpringCGLIB$$xxxxxxxx}），每次运行可能不同，
     * 不能直接抄进笔记当证据 —— 统一替换成 {@code $$<hash>}。
     */
    static String mask(String className) {
        return className.replaceAll("\\$\\$EnhancerBySpringCGLIB\\$\\$[0-9a-f]+",
                "\\$\\$EnhancerBySpringCGLIB\\$\\$<hash>");
    }

    private static void printError(String what, Throwable e, int maxFrames) {
        System.out.println("  " + what + " 抛出：");
        System.out.println("    " + e.getClass().getName());
        System.out.println("    message = " + e.getMessage());
        if (e.getCause() != null) {
            System.out.println("    cause   = " + e.getCause().getClass().getName() + " : " + e.getCause().getMessage());
        }
        System.out.println("    栈（只留 spring / com.newt 的帧）：");
        int printed = 0;
        for (StackTraceElement f : e.getStackTrace()) {
            String c = f.getClassName();
            if (!c.startsWith("org.springframework") && !c.startsWith("com.newt")) {
                continue;
            }
            System.out.printf("      at %s.%s(%s:%d)%n", c, f.getMethodName(), f.getFileName(), f.getLineNumber());
            if (++printed >= maxFrames) {
                break;
            }
        }
    }
}
