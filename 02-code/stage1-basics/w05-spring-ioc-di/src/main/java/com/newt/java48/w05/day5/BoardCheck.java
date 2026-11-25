package com.newt.java48.w05.day5;

import com.newt.java48.w05.support.Quiet;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * W05 Day 5 · 封板自检：把四天口述过的结论，逐条变成「跑一遍能对上的断言」。
 *
 * <p>前几天每个论点都是一段独立 Demo，靠眼睛看输出；今天反过来 —— 先写期望值，再让程序告诉你对不上。
 * 封板的标准不是「笔记写完了」，是「换个 JDK / 换个 Spring 小版本，这条断言还站得住」。
 *
 * <p>四段对应笔记里的「Spring 到底在帮你管理什么」三段答案 + 一句边界：
 * <ol>
 *   <li><b>S1 对象图</b> —— 3 个调用点 → 1 个装配点（Day 1）；</li>
 *   <li><b>S2 生命周期</b> —— 九步顺序 + prototype 不管埋（Day 2）；</li>
 *   <li><b>S3 作用域</b> —— 造几个 + 谁管埋（Day 3）；</li>
 *   <li><b>S4 边界</b> —— 判定链四级 + 两种注解的报错（Day 4）；</li>
 *   <li><b>S5 封板表</b> —— 任一条 FAIL 就 {@code System.exit(1)}。</li>
 * </ol>
 *
 * <p>今天新增的观测点（Day 2 只能靠两个点夹住的那件事）：给 {@code LifeProbe} 加一个
 * {@code @Autowired} 的 <b>setter</b>，在 setter 里打点。setter 是我自己写的方法、不是容器回调，
 * 它落在哪个位置，就把「属性注入」这件事从「夹住」变成「钉死」。
 */
public class BoardCheck {

    private static final List<String> RESULTS = new ArrayList<>();
    private static int failed = 0;

    public static void main(String[] args) {
        Quiet.springRefreshWarning();
        System.out.println("W05 封板自检 · Spring 5.3.39 / JDK " + System.getProperty("java.version"));
        System.out.println();

        section1ObjectGraph();
        section2Lifecycle();
        section3Scope();
        section4PickOne();
        section5Board();

        if (failed > 0) {
            System.out.println();
            System.out.println("封板失败：" + failed + " 条断言不成立，先别勾第四格。");
            System.exit(1);
        }
    }

    // ================================================================== S1 对象图

    interface Repo {
        String name();
    }

    /** 每 new 一个就 +1 —— 「谁多开了一个连接池」靠它说话。 */
    static class JdbcRepo implements Repo {
        static final AtomicInteger BUILT = new AtomicInteger();
        private final int id = BUILT.incrementAndGet();

        @Override
        public String name() {
            return "jdbc#" + id;
        }
    }

    static class OrderService {
        final Repo repo;

        OrderService(Repo repo) {
            this.repo = repo;
        }
    }

    // 手写版：三个入口，各自拼对象（这就是 Day 1 A 段那三个连接池）
    static OrderService webEntry() {
        return new OrderService(new JdbcRepo());
    }

    static OrderService jobEntry() {
        return new OrderService(new JdbcRepo());
    }

    static OrderService testEntry() {
        return new OrderService(new JdbcRepo());
    }

    @Configuration
    static class GraphConfig {
        @Bean
        Repo jdbcRepo() {
            return new JdbcRepo();
        }

        @Bean
        OrderService webOrderService(Repo repo) {
            return new OrderService(repo);
        }

        @Bean
        OrderService jobOrderService(Repo repo) {
            return new OrderService(repo);
        }

        @Bean
        OrderService apiTestOrderService(Repo repo) {
            return new OrderService(repo);
        }
    }

    private static void section1ObjectGraph() {
        System.out.println("=== S1. 对象图：3 个调用点 → 1 个装配点 ===");

        JdbcRepo.BUILT.set(0);
        OrderService a = webEntry();
        OrderService b = jobEntry();
        OrderService c = testEntry();
        System.out.printf("  手写版：三个入口 → %d 份仓储（%s / %s / %s）%n",
                JdbcRepo.BUILT.get(), a.repo.name(), b.repo.name(), c.repo.name());
        check("手写装配造出 3 份仓储", JdbcRepo.BUILT.get() == 3, "实际 " + JdbcRepo.BUILT.get());

        JdbcRepo.BUILT.set(0);
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(GraphConfig.class);
        OrderService w = ctx.getBean("webOrderService", OrderService.class);
        OrderService j = ctx.getBean("jobOrderService", OrderService.class);
        OrderService t = ctx.getBean("apiTestOrderService", OrderService.class);
        System.out.printf("  容器版：同样三个入口 → %d 份仓储（%s）%n", JdbcRepo.BUILT.get(), w.repo.name());
        check("容器只造 1 份仓储", JdbcRepo.BUILT.get() == 1, "实际 " + JdbcRepo.BUILT.get());
        check("三个入口拿到同一个引用", w.repo == j.repo && j.repo == t.repo,
                "w==j " + (w.repo == j.repo) + " / j==t " + (j.repo == t.repo));
        ctx.close();
        System.out.println();
    }

    // ================================================================== S2 生命周期

    private static final List<String> LOG = new ArrayList<>();

    static class LifeProbe implements InitializingBean, DisposableBean {
        private Repo repo;

        LifeProbe() {
            LOG.add("ctor");
        }

        /** 注入点：setter 是我写的方法，不是容器回调 —— 它的位置就是 populateBean 的位置。 */
        @Autowired
        void setRepo(Repo repo) {
            this.repo = repo;
            LOG.add("inject(setter)");
        }

        @PostConstruct
        void postConstruct() {
            LOG.add("@PostConstruct");
            System.out.println("    @PostConstruct 里看到的 repo = " + repo.name()
                    + "（构造时还是 null，注入确实夹在中间）");
        }

        @Override
        public void afterPropertiesSet() {
            LOG.add("afterPropertiesSet");
        }

        void customInit() {
            LOG.add("initMethod");
        }

        @PreDestroy
        void preDestroy() {
            LOG.add("@PreDestroy");
        }

        @Override
        public void destroy() {
            LOG.add("destroy");
        }

        void customDestroy() {
            LOG.add("destroyMethod");
        }
    }

    static class LifeBpp implements BeanPostProcessor {
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            if (bean instanceof LifeProbe) {
                LOG.add("bpp.before");
            }
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof LifeProbe) {
                LOG.add("bpp.after");
            }
            return bean;
        }
    }

    /** 带计数器的通用 Bean：作用域判据和 prototype 销毁都用它，避免两套脚手架数字打架。 */
    static class Counted implements DisposableBean {
        private static final Map<String, AtomicInteger> BUILT = new HashMap<>();
        private static final Map<String, AtomicInteger> PRE = new HashMap<>();
        private static final Map<String, AtomicInteger> DEST = new HashMap<>();

        private final String kind;
        private final int id;

        Counted(String kind) {
            this.kind = kind;
            this.id = inc(BUILT, kind);
        }

        static void reset() {
            BUILT.clear();
            PRE.clear();
            DEST.clear();
        }

        static int built(String kind) {
            return get(BUILT, kind);
        }

        static int preDestroyed(String kind) {
            return get(PRE, kind);
        }

        static int destroyed(String kind) {
            return get(DEST, kind);
        }

        private static int inc(Map<String, AtomicInteger> m, String k) {
            return m.computeIfAbsent(k, x -> new AtomicInteger()).incrementAndGet();
        }

        private static int get(Map<String, AtomicInteger> m, String k) {
            AtomicInteger a = m.get(k);
            return a == null ? 0 : a.get();
        }

        String describe() {
            return kind + "#" + id;
        }

        @PreDestroy
        void pre() {
            inc(PRE, kind);
        }

        @Override
        public void destroy() {
            inc(DEST, kind);
        }
    }

    @Configuration
    static class LifeConfig {
        /** static：让它在配置类实例化之前就被识别为后处理器，别制造 early-instantiation 噪音。 */
        @Bean
        static LifeBpp lifeBpp() {
            return new LifeBpp();
        }

        @Bean
        Repo jdbcRepo() {
            return new JdbcRepo();
        }

        @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
        LifeProbe lifeProbe() {
            return new LifeProbe();
        }

        @Bean
        @Scope("prototype")
        Counted protoCounted() {
            return new Counted("protoCounted");
        }
    }

    private static void section2Lifecycle() {
        System.out.println("=== S2. 生命周期：顺序不是背的，是打出来的 ===");
        LOG.clear();
        Counted.reset();
        JdbcRepo.BUILT.set(0);

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(LifeConfig.class);

        System.out.println("  初始化阶段实际顺序：");
        for (int i = 0; i < LOG.size(); i++) {
            System.out.printf("    %d. %s%n", i + 1, LOG.get(i));
        }
        List<String> expectInit = List.of("ctor", "inject(setter)", "bpp.before",
                "@PostConstruct", "afterPropertiesSet", "initMethod", "bpp.after");
        check("初始化七步顺序与源码一致", LOG.equals(expectInit), "实际 " + LOG);

        // prototype 取两个，close 后销毁回调应当 0 次
        ctx.getBean("protoCounted", Counted.class);
        ctx.getBean("protoCounted", Counted.class);
        check("prototype 取两次造两个", Counted.built("protoCounted") == 2,
                "实际 " + Counted.built("protoCounted"));

        LOG.clear();
        ctx.close();

        System.out.println("  销毁阶段实际顺序：");
        for (int i = 0; i < LOG.size(); i++) {
            System.out.printf("    %d. %s%n", i + 1, LOG.get(i));
        }
        check("销毁三件套顺序", LOG.equals(List.of("@PreDestroy", "destroy", "destroyMethod")), "实际 " + LOG);
        check("prototype 销毁 0 次（登记阶段就被 !isPrototype() 短路）",
                Counted.preDestroyed("protoCounted") == 0 && Counted.destroyed("protoCounted") == 0,
                "@PreDestroy " + Counted.preDestroyed("protoCounted")
                        + " / destroy " + Counted.destroyed("protoCounted"));
        System.out.println();
    }

    // ================================================================== S3 作用域

    @Configuration
    static class ScopeConfig {
        @Bean
        Counted singleCounted() {
            return new Counted("singleCounted");
        }

        @Bean
        @Scope("prototype")
        Counted protoCounted2() {
            return new Counted("protoCounted2");
        }
    }

    private static void section3Scope() {
        System.out.println("=== S3. 作用域：造几个 + 谁管埋 ===");
        Counted.reset();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ScopeConfig.class);
        check("refresh 后单例已构造 1 次（预实例化）", Counted.built("singleCounted") == 1,
                "实际 " + Counted.built("singleCounted"));
        check("refresh 后原型构造 0 次", Counted.built("protoCounted2") == 0,
                "实际 " + Counted.built("protoCounted2"));

        Counted s1 = ctx.getBean("singleCounted", Counted.class);
        Counted s2 = ctx.getBean("singleCounted", Counted.class);
        Counted p1 = ctx.getBean("protoCounted2", Counted.class);
        Counted p2 = ctx.getBean("protoCounted2", Counted.class);
        check("单例两次 getBean 同一个", s1 == s2, s1.describe() + " / " + s2.describe());
        check("原型两次 getBean 不同", p1 != p2, p1.describe() + " / " + p2.describe());
        check("单例进池 / 原型不进池",
                ctx.getBeanFactory().containsSingleton("singleCounted")
                        && !ctx.getBeanFactory().containsSingleton("protoCounted2"),
                "true / false");

        ctx.close();
        check("close() 销毁单例（@PreDestroy 1 + destroy 1）",
                Counted.preDestroyed("singleCounted") == 1 && Counted.destroyed("singleCounted") == 1,
                Counted.preDestroyed("singleCounted") + " + " + Counted.destroyed("singleCounted"));
        check("close() 不碰原型", Counted.destroyed("protoCounted2") == 0,
                "destroy " + Counted.destroyed("protoCounted2"));
        System.out.println();
    }

    // ================================================================== S4 边界：挑哪一个

    interface Pay {
        String tag();
    }

    @Configuration
    static class PayConfig {
        @Bean
        Pay alipayChannel() {
            return () -> "alipay";
        }

        @Bean
        Pay wechatChannel() {
            return () -> "wechat";
        }

        @Bean
        Pay unionPayChannel() {
            return () -> "unionpay";
        }
    }

    @Configuration
    static class PrimaryPayConfig {
        @Bean
        @Primary
        Pay preferredChannel() {
            return () -> "preferred";
        }
    }

    static class NeedsOne {
        @Autowired
        Pay channel;
    }

    static class NameMatch {
        @Autowired
        Pay wechatChannel;
    }

    static class ByQualifier {
        @Autowired
        @Qualifier("alipayChannel")
        Pay channel;
    }

    static class ResourceWrongName {
        @Resource(name = "channel")
        Pay channel;
    }

    static class ResourceDefaultName {
        @Resource
        Pay channel;
    }

    private static void section4PickOne() {
        System.out.println("=== S4. 边界：三个实现挑一个，挑不出来报什么 ===");

        Throwable noUnique = failOf(PayConfig.class, NeedsOne.class);
        String msg = noUnique == null ? "" : String.valueOf(chainMessage(noUnique));
        System.out.println("  字段名 channel 对不上任何 bean 名 → " + firstLine(noUnique));
        check("多候选抛 NoUniqueBeanDefinitionException",
                noUnique != null && msg.contains("No qualifying bean of type")
                        && msg.contains("expected single matching bean but found 3"),
                firstLine(noUnique));
        check("报错里列出三个候选名",
                msg.contains("alipayChannel") && msg.contains("wechatChannel") && msg.contains("unionPayChannel"),
                "含三个名字");

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(PayConfig.class, NameMatch.class);
        ctx.refresh();
        String fallback = ctx.getBean(NameMatch.class).wechatChannel.tag();
        System.out.println("  字段名 wechatChannel + 零注解 → " + fallback);
        check("第 3 级「按字段名回退」", "wechat".equals(fallback), fallback);
        ctx.close();

        ctx = new AnnotationConfigApplicationContext();
        ctx.register(PayConfig.class, PrimaryPayConfig.class, NameMatch.class);
        ctx.refresh();
        String primary = ctx.getBean(NameMatch.class).wechatChannel.tag();
        System.out.println("  字段名依旧是 wechatChannel + 容器里有 @Primary → " + primary);
        check("@Primary 压过字段名（Primary 在名字之前）", "preferred".equals(primary), primary);
        ctx.close();

        ctx = new AnnotationConfigApplicationContext();
        ctx.register(PayConfig.class, ByQualifier.class);
        ctx.refresh();
        String qualified = ctx.getBean(ByQualifier.class).channel.tag();
        System.out.println("  @Qualifier(\"alipayChannel\") → " + qualified);
        check("@Qualifier 在筛候选时生效", "alipay".equals(qualified), qualified);
        ctx.close();

        Throwable explicit = failOf(PayConfig.class, ResourceWrongName.class);
        System.out.println("  @Resource(name=\"channel\") → " + firstLine(explicit));
        check("@Resource 显式点名对不上 → No bean named（不查类型）",
                explicit != null && String.valueOf(chainMessage(explicit)).contains("No bean named 'channel'"),
                firstLine(explicit));

        Throwable implicit = failOf(PayConfig.class, ResourceDefaultName.class);
        System.out.println("  @Resource 默认名对不上 → " + firstLine(implicit));
        check("@Resource 默认名对不上 → 退回按类型 → 同一个 NoUnique",
                implicit != null
                        && String.valueOf(chainMessage(implicit)).contains("expected single matching bean"),
                firstLine(implicit));

        // 今天新抓到的一条：内层异常相同，外层包装不同。
        // 因为 NoUniqueBeanDefinitionException 是 NoSuchBeanDefinitionException 的子类，
        // AutowiredFieldElement.inject 的 catch (NoSuchBeanDefinitionException) 会把它一起改写。
        System.out.println();
        System.out.println("  三条失败链的完整形状（今天才注意到的差别）：");
        showChain("@Autowired 多候选", noUnique);
        showChain("@Resource 显式名错", explicit);
        showChain("@Resource 默认名错", implicit);
        check("@Autowired 失败外层是 UnsatisfiedDependencyException",
                noUnique != null && noUnique.getClass().getSimpleName().equals("UnsatisfiedDependencyException"),
                noUnique == null ? "null" : noUnique.getClass().getSimpleName());
        check("@Resource 失败外层是 BeanCreationException（不是 UnsatisfiedDependency）",
                explicit != null && implicit != null
                        && explicit.getClass().getSimpleName().equals("BeanCreationException")
                        && implicit.getClass().getSimpleName().equals("BeanCreationException"),
                (explicit == null ? "null" : explicit.getClass().getSimpleName()) + " / "
                        + (implicit == null ? "null" : implicit.getClass().getSimpleName()));
        System.out.println();
    }

    private static void showChain(String label, Throwable t) {
        StringBuilder sb = new StringBuilder("    " + label + "：");
        int i = 0;
        for (Throwable x = t; x != null && i < 4; x = x.getCause(), i++) {
            sb.append(i == 0 ? "" : " ← ").append(x.getClass().getSimpleName());
            if (x.getCause() == x) {
                break;
            }
        }
        System.out.println(sb);
    }

    // ================================================================== S5 封板表

    private static void section5Board() {
        System.out.println("=== S5. 封板表 ===");
        for (String r : RESULTS) {
            System.out.println("  " + r);
        }
        System.out.println();
        System.out.println("  " + (RESULTS.size() - failed) + " / " + RESULTS.size() + " 条通过");
    }

    // ================================================================== 工具

    private static void check(String name, boolean ok, String detail) {
        RESULTS.add((ok ? "[PASS] " : "[FAIL] ") + name + (detail.isEmpty() ? "" : "   (" + detail + ")"));
        if (!ok) {
            failed++;
        }
        System.out.println("    " + (ok ? "✓ " : "✗ ") + name + (detail.isEmpty() ? "" : "   " + detail));
    }

    private static Throwable failOf(Class<?>... classes) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(classes);
        try {
            ctx.refresh();
            return null;
        } catch (Throwable t) {
            return t;
        } finally {
            try {
                ctx.close();
            } catch (Throwable ignore) {
                // refresh 失败时 close 可能报 BeanFactory not initialized，这里不关心
            }
        }
    }

    /** 把整条异常链的 message 拼起来 —— Spring 往往把「因果」放在中间某层。 */
    private static String chainMessage(Throwable t) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Throwable x = t; x != null && i < 4; x = x.getCause(), i++) {
            sb.append(x.getMessage()).append('\n');
            if (x.getCause() == x) {
                break;
            }
        }
        return sb.toString();
    }

    /** 打「最有信息量的那一行」：最外层类名 + 内层根因类名。 */
    private static String firstLine(Throwable t) {
        if (t == null) {
            return "(没有异常 —— 说明假设错了)";
        }
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return t.getClass().getSimpleName() + " ← " + root.getClass().getSimpleName();
    }
}
