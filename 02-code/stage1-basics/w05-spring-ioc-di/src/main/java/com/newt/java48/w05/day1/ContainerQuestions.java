package com.newt.java48.w05.day1;

import com.newt.java48.w05.day1.app.OrderRepository;
import com.newt.java48.w05.day1.cycle.CtorA;
import com.newt.java48.w05.day1.cycle.CtorB;
import com.newt.java48.w05.day1.cycle.FieldA;
import com.newt.java48.w05.day1.cycle.FieldB;
import com.newt.java48.w05.support.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 第三段实验：四个「不试一次就说不准」的问题。
 *
 * <ol>
 *   <li>不写 {@code @Component} 会怎样（两种：直接要、被别人依赖）；</li>
 *   <li>注解会不会在 {@code new} 出来的对象上生效；</li>
 *   <li>循环依赖到底报不报错（字段注入 vs 构造器注入）；</li>
 *   <li>单例池里有几个东西、原型和懒加载的在哪。</li>
 * </ol>
 */
public class ContainerQuestions {

    public static void main(String[] args) {
        // Q1b / Q3b 是「故意让容器启动失败」，Spring 会打一条 WARN 到 JUL；
        // 消息和我们下面打印的异常链重复，这里关掉，输出干净点。
        Logger.getLogger("org.springframework.context.support.AbstractApplicationContext")
                .setLevel(Level.OFF);

        Trace.mark("ContainerQuestions 启动");
        q1();
        q2();
        q3();
        q4();
    }

    // ---------------- Q1：不写注解 ----------------

    private static void q1() {
        System.out.println();
        System.out.println("=== Q1. 不写 @Component 会怎样 ===");

        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {
            System.out.println("  [1a] 直接 getBean 一个没注册的类：");
            ctx.getBean(MissingDep.class);
        } catch (Exception e) {
            System.out.println("       " + e.getClass().getName());
            System.out.println("       " + firstLine(e.getMessage()));
        }

        System.out.println("  [1b] 被别人依赖时才炸（真实项目里通常是这种）：");
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(BrokenService.class);
            ctx.refresh();
        } catch (Exception e) {
            printChain(e);
        }
        System.out.println("  共同点：异常信息里都点名了「哪个类找不到 / 哪个构造器参数没东西可注」。");
    }

    // ---------------- Q2：注解在 new 出来的对象上 ----------------

    private static void q2() {
        System.out.println();
        System.out.println("=== Q2. 注解只对「容器造的对象」生效 ===");

        NotManaged byHand = new NotManaged();
        System.out.println("  new NotManaged() 之后，@Autowired 字段 = " + byHand.repo);

        // 注意：不能先 new 一个带 AppConfig 的容器再 register + refresh
        // —— GenericApplicationContext 只允许 refresh 一次（下面第一版就是这么炸的）
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(AppConfig.class, NotManaged.class);
            ctx.refresh();
            NotManaged bySpring = ctx.getBean(NotManaged.class);
            System.out.println("  交给容器注册后，@Autowired 字段 = "
                    + (bySpring.repo == null ? "null" : bySpring.repo.impl()));
        }
        System.out.println("  同一个类，同样的注解，差别只在「谁 new 的、以及有没有人处理注解」。");
    }

    // ---------------- Q3：循环依赖 ----------------

    private static void q3() {
        System.out.println();
        System.out.println("=== Q3. 循环依赖：字段注入能过，构造器注入过不去 ===");

        System.out.println("  [3a] 字段注入 A ⇄ B：");
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(FieldA.class, FieldB.class);
            ctx.refresh();
            FieldA a = ctx.getBean(FieldA.class);
            FieldB b = ctx.getBean(FieldB.class);
            System.out.println("       a.b 非空 = " + a.linked() + "，b.a 非空 = " + b.linked()
                    + "，b.a 是不是同一个 a = " + (b.a() == a));
            System.out.println("       做法：先 new 出 A 的空壳暴露出去（早期引用），造完 B 再回头把 a 的字段灌满。");
        }

        System.out.println("  [3b] 构造器注入 A ⇄ B：");
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(CtorA.class, CtorB.class);
            ctx.refresh();
            System.out.println("       居然成功了？" + ctx.getBean(CtorA.class));
        } catch (Exception e) {
            printChain(e);
            System.out.println("       没有空壳可暴露 —— 参数必须在 new 之前就位，于是无解。");
            System.out.println("       这也是 Spring 官方推荐构造器注入、却要求「别写出环」的原因。");
        }
    }

    // ---------------- Q4：单例池 ----------------

    private static void q4() {
        System.out.println();
        System.out.println("=== Q4. 单例池里有什么 ===");

        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {
            ConfigurableListableBeanFactory factory = ctx.getBeanFactory();
            Set<String> defs = new LinkedHashSet<>(Arrays.asList(ctx.getBeanDefinitionNames()));
            Set<String> singles = new TreeSet<>(Arrays.asList(factory.getSingletonNames()));

            System.out.printf("  bean 定义数 = %d，单例池实例数 = %d%n", defs.size(), singles.size());

            List<String> defOnly = new ArrayList<>(defs);
            defOnly.removeAll(singles);
            List<String> singleOnly = new ArrayList<>(singles);
            singleOnly.removeAll(defs);

            System.out.println("  ── 有定义、没实例（= 还没轮到造）──");
            defOnly.forEach(n -> System.out.println("     " + n));
            System.out.println("  ── 有实例、没定义（= 容器自己手动塞进去的）──");
            singleOnly.forEach(n -> System.out.println("     " + n));

            ctx.getBean(com.newt.java48.w05.day1.app.PrototypeTask.class);
            ctx.getBean(com.newt.java48.w05.day1.app.LazyBean.class);
            System.out.printf("  取过 prototype 与 lazyBean 之后：定义数 %d，单例数 %d%n",
                    ctx.getBeanDefinitionCount(), factory.getSingletonCount());
            System.out.println("  → 单例池是一张 Map<beanName, instance>；原型造完就撒手，容器连账都不记。");
        }
    }

    // ---------------- 小工具 ----------------

    private static String firstLine(String s) {
        if (s == null) {
            return "（无 message）";
        }
        int nl = s.indexOf('\n');
        return nl < 0 ? s : s.substring(0, nl) + " …";
    }

    private static void printChain(Throwable e) {
        Throwable cur = e;
        int depth = 0;
        while (cur != null && depth < 6) {
            System.out.println("       " + "  ".repeat(depth) + "- " + cur.getClass().getSimpleName()
                    + ": " + firstLine(cur.getMessage()));
            cur = cur.getCause();
            depth++;
        }
    }

    // ---------------- 实验用的两个「残缺」类 ----------------

    /** 完全没有注解 —— 容器不知道它的存在。 */
    static class MissingDep {
    }

    /** 有注解，但依赖要不到 —— 真实项目里最常见的启动失败。 */
    @Component
    static class BrokenService {
        BrokenService(MissingDep dep) {
            Trace.constructed("BrokenService", "dep=" + dep);
        }
    }

    /** 有 {@code @Autowired} 字段，但类本身没注册 —— 用来证明「注解不会自己生效」。 */
    static class NotManaged {
        @Autowired
        OrderRepository repo;
    }
}
