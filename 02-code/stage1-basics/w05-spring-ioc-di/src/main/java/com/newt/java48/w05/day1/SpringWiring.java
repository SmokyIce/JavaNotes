package com.newt.java48.w05.day1;

import com.newt.java48.w05.day1.app.LazyBean;
import com.newt.java48.w05.day1.app.OrderApiTest;
import com.newt.java48.w05.day1.app.OrderController;
import com.newt.java48.w05.day1.app.OrderRepository;
import com.newt.java48.w05.day1.app.OrderService;
import com.newt.java48.w05.day1.app.PrototypeTask;
import com.newt.java48.w05.day1.app.ReportJob;
import com.newt.java48.w05.support.Src;
import com.newt.java48.w05.support.Trace;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 第二段实验：<b>同一套类交给 Spring</b>。
 *
 * <p>三个入口、四个组件、零行 {@code new} —— 唯一的输入是 {@link AppConfig} 上的
 * {@code @ComponentScan} 和各个类上的构造器签名。
 *
 * <p>五段：A 谁在什么时候 new 的 / B 是不是同一个 / C 装配代码还剩几行 /
 * D 换实现 / E {@code @Lazy} / F 原型作用域。
 */
public class SpringWiring {

    public static void main(String[] args) {
        Trace.mark("SpringWiring 启动（此时一个 Bean 都还没有）");

        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(AppConfig.class);

        Trace.mark("容器创建完成：refresh() 返回，单例已经在池子里了");

        sectionA(ctx);
        sectionB(ctx);
        sectionC();
        sectionD();
        sectionE(ctx);
        sectionF(ctx);

        Trace.mark("close()");
        ctx.close();
    }

    private static void sectionA(AnnotationConfigApplicationContext ctx) {
        System.out.println();
        System.out.println("=== A. 谁 new 的？什么时候 new 的？ ===");
        System.out.println("  上面每条 [构造] 都是 refresh() 期间打的 —— 注意它们的时间戳都早于「容器创建完成」那行。");
        System.out.println("  直接调用者不是我们，是 spring 的 BeanUtils.instantiateClass；");
        System.out.println("  再往上命中 createBeanInstance（反射造对象）和 preInstantiateSingletons（预实例化单例）。");
        System.out.println("  换句话说：new 发生在我们拿到容器之前，而不是第一次 getBean 的时候。");

        System.out.println("  ── 容器里注册了多少个 bean 定义 ──");
        System.out.println("  getBeanDefinitionCount() = " + ctx.getBeanDefinitionCount());
        System.out.println("  下面这份名单来自「bean 定义注册表」，读它不会触发构造 —— 所以 @Lazy / prototype 也列在这里。");
        for (String name : ctx.getBeanDefinitionNames()) {
            if (name.startsWith("org.springframework")) {
                continue; // 容器自己的基础设施（6 个左右），先不看
            }
            String cn = ctx.getBeanFactory().getBeanDefinition(name).getBeanClassName();
            String simple = cn == null ? "?" : cn.substring(cn.lastIndexOf('.') + 1);
            System.out.printf("    %-22s -> %s%n", name, simple);
        }
    }

    private static void sectionB(AnnotationConfigApplicationContext ctx) {
        System.out.println();
        System.out.println("=== B. getBean 两次，是不是同一个对象 ===");

        OrderService s1 = ctx.getBean(OrderService.class);
        OrderService s2 = ctx.getBean(OrderService.class);
        System.out.printf("  s1 == s2 : %s   hash: %d / %d%n",
                s1 == s2, System.identityHashCode(s1), System.identityHashCode(s2));

        OrderRepository r1 = ctx.getBean(OrderRepository.class);
        OrderRepository r2 = ctx.getBean(OrderRepository.class);
        System.out.printf("  仓储 r1 == r2 : %s   impl=%s%n", r1 == r2, r1.impl());
        System.out.println("  默认作用域是 singleton：容器就是一张「bean 名 → 唯一实例」的缓存表。");

        System.out.println();
        System.out.println("  ── 三个入口跑一遍（它们自己没 new 任何东西）──");
        System.out.println("  " + ctx.getBean(OrderController.class).handle(11L));
        System.out.println("  " + ctx.getBean(ReportJob.class).run());
        System.out.println("  " + ctx.getBean(OrderApiTest.class).run());
    }

    private static void sectionC() {
        System.out.println();
        System.out.println("=== C. 装配代码还剩几行（数源码）===");

        String self = Src.text("src/main/java/com/newt/java48/w05/day1/SpringWiring.java");
        String memCfg = Src.text("src/main/java/com/newt/java48/w05/day1/MemoryConfig.java");
        String appCfg = Src.text("src/main/java/com/newt/java48/w05/day1/AppConfig.java");

        System.out.printf("  %-34s %-26s %-22s %s%n", "文件", "new JdbcOrderRepository", "new PriceCalculator", "new OrderService");
        System.out.printf("  %-34s %-26d %-22d %d%n", "SpringWiring.java（三个入口）",
                Src.countNew(self, "JdbcOrderRepository"),
                Src.countNew(self, "PriceCalculator"),
                Src.countNew(self, "OrderService"));
        System.out.printf("  %-34s %-26d %-22d %d%n", "AppConfig.java（扫包）",
                Src.countNew(appCfg, "JdbcOrderRepository"),
                Src.countNew(appCfg, "PriceCalculator"),
                Src.countNew(appCfg, "OrderService"));
        System.out.printf("  %-34s %-26d %-22d %d%n", "MemoryConfig.java（换实现时才写 new）",
                Src.countNew(memCfg, "JdbcOrderRepository"),
                Src.countNew(memCfg, "PriceCalculator"),
                Src.countNew(memCfg, "OrderService"));
        System.out.println();
        System.out.println("  MemoryConfig 里唯一的那行 new = " + Src.countNew(memCfg, "MemoryOrderRepository")
                + " 次（决定「配哪个实现」，不是「每个入口各配一次」）");
        System.out.println("  同样的三行 new，手写版各出现 3 次，Spring 版在入口代码里出现 0 次。");
    }

    private static void sectionD() {
        System.out.println();
        System.out.println("=== D. 换实现：只换装配配置，三个入口一个字没改 ===");

        AnnotationConfigApplicationContext mem =
                new AnnotationConfigApplicationContext(MemoryConfig.class);
        Trace.mark("内存版容器就绪");

        System.out.println("  仓储实现 = " + mem.getBean(OrderRepository.class).impl());
        System.out.println("  " + mem.getBean(OrderController.class).handle(22L));
        System.out.println("  " + mem.getBean(ReportJob.class).run());
        System.out.println("  " + mem.getBean(OrderApiTest.class).run());
        System.out.println("  注意 OrderService 构造器里打的 repo=memory —— 业务类零改动，只是注进来的东西换了。");

        mem.close();
    }

    private static void sectionE(AnnotationConfigApplicationContext ctx) {
        System.out.println();
        System.out.println("=== E. @Lazy：把「什么时候 new」推到第一次 getBean ===");

        boolean before = ctx.getBeanFactory().containsSingleton("lazyBean");
        Trace.mark("getBean(LazyBean) 之前，单例池里有 lazyBean 吗 = " + before);
        ctx.getBean(LazyBean.class);
        Trace.mark("getBean(LazyBean) 之后 = " + ctx.getBeanFactory().containsSingleton("lazyBean"));
        System.out.println("  refresh 阶段它没被造（时间戳比「容器创建完成」晚），@Lazy 只是把构造推迟，不是取消。");
    }

    private static void sectionF(AnnotationConfigApplicationContext ctx) {
        System.out.println();
        System.out.println("=== F. 原型作用域：每次 getBean 都给新的 ===");

        PrototypeTask p1 = ctx.getBean(PrototypeTask.class);
        PrototypeTask p2 = ctx.getBean(PrototypeTask.class);
        System.out.printf("  p1 == p2 : %s   hash: %d / %d   累计实例 %d%n",
                p1 == p2, System.identityHashCode(p1), System.identityHashCode(p2),
                PrototypeTask.instances());
        System.out.println("  单例池里有 prototypeTask 吗 = "
                + ctx.getBeanFactory().containsSingleton("prototypeTask")
                + "（原型不进单例池，容器不负责销毁，只管造）");
    }
}
