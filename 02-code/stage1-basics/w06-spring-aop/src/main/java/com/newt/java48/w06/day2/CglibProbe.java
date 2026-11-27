package com.newt.java48.w06.day2;

// 注意包名：Spring 5 把 CGLIB 重新打包进了 spring-core，所以是 org.springframework.cglib，
// 不是网上到处抄的 net.sf.cglib —— lib/ 里也没有独立的 cglib jar，只靠 spring-core 就够
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CGLIB 到底生成了什么 —— 补 W06 第三格知识点（Day 1 只证明了「什么时候选它」）。
 *
 * <pre>
 *   A  手写 Enhancer：子类长什么样、instanceof 谁、构造器被调几次
 *   B  final 类：报错原文
 *   C  final 方法与 private 方法：谁能被覆写、谁压根看不见
 *   D  MethodProxy / 反射 / 直接调用：三种调用路径的耗时
 *   E  Spring 用 CGLIB 时构造器调几次（Objenesis 为什么存在）
 * </pre>
 *
 * <p>为什么这值得单独一天：Spring Boot 2.x 起 {@code proxyTargetClass} 默认 true，
 * 项目里绝大多数 Bean 都是 CGLIB 代理 —— 「final 方法上注解不生效」「private 方法没被拦」
 * 这类问题会一直出现，而它们全都**不报错**。
 */
public class CglibProbe {

    static void sec(String s) {
        System.out.println();
        System.out.println("== " + s + " ==");
    }

    // ---------------------------------------------------------------- 靶子

    public static class Calc {

        public static final AtomicInteger CONSTRUCTED = new AtomicInteger();

        public Calc() {
            CONSTRUCTED.incrementAndGet();
        }

        public int add(int a, int b) {
            return a + b;
        }

        /** final：CGLIB 生成的子类覆写不了 → 拦不到。 */
        public final int sealedAdd(int a, int b) {
            return a + b;
        }

        /** private：子类看不见这个方法，但**外面调 callSecret 时拦的是 callSecret**。 */
        private int secretAdd(int a, int b) {
            return a + b;
        }

        public int callSecret(int a, int b) {
            return secretAdd(a, b);
        }
    }

    // ---------------------------------------------------------------- 拦截器

    static class Counting implements MethodInterceptor {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy)
                throws Throwable {
            calls.incrementAndGet();
            return proxy.invokeSuper(obj, args);
        }
    }

    static Calc enhance(Counting cb) {
        Enhancer e = new Enhancer();
        e.setSuperclass(Calc.class);
        e.setCallback(cb);
        return (Calc) e.create();
    }

    // ---------------------------------------------------------------- Spring 侧

    @Aspect
    public static class TouchAspect {
        static final AtomicInteger BEFORE = new AtomicInteger();

        @Before("within(com.newt.java48.w06.day2.CglibProbe.Calc)")
        public void before() {
            BEFORE.incrementAndGet();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class Cfg {
        @Bean
        public Calc calc() {
            return new Calc();
        }

        @Bean
        public TouchAspect touchAspect() {
            return new TouchAspect();
        }
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("CglibProbe · CGLIB 生成的到底是什么");
        System.out.println("Spring = " + org.springframework.core.SpringVersion.getVersion());

        sec("A 手写 Enhancer");
        Calc.CONSTRUCTED.set(0);
        Counting cb = new Counting();
        Calc proxy = enhance(cb);
        System.out.println("  代理类名     = " + proxy.getClass().getName());
        System.out.println("  父类         = " + proxy.getClass().getSuperclass().getName());
        System.out.println("  实现的接口    = " + java.util.Arrays.toString(proxy.getClass().getInterfaces()));
        System.out.println("  proxy instanceof Calc = " + (proxy instanceof Calc));
        System.out.println("  创建代理时 Calc 构造器被调次数 = " + Calc.CONSTRUCTED.get());
        System.out.println("  -> 没有接口、也没实现 InvocationHandler，就是「继承 + 覆写」；");
        System.out.println("     所以它**是** Calc（父类就是 Calc），这和 JDK 代理「是兄弟」正好相反");

        sec("A2 方法调用：谁进了拦截器");
        cb.calls.set(0);
        System.out.println("  add(1,2)        = " + proxy.add(1, 2) + "   calls=" + cb.calls.get());
        cb.calls.set(0);
        System.out.println("  sealedAdd(1,2)  = " + proxy.sealedAdd(1, 2) + "   calls=" + cb.calls.get());
        cb.calls.set(0);
        System.out.println("  callSecret(1,2) = " + proxy.callSecret(1, 2) + "   calls=" + cb.calls.get());
        System.out.println("  -> add 进 1 次；sealedAdd（final）进 0 次；callSecret 只进 1 次，里面的 secretAdd 不算");
        System.out.println("     final 覆写不了 = 拦截不到；private 看不见 = 拦截不到；");
        System.out.println("     「内部调用不经过代理」在 CGLIB 上同样成立 —— 和 Day 1 的 this.xxx() 是同一件事");

        sec("B final 类");
        try {
            Enhancer e = new Enhancer();
            e.setSuperclass(String.class);
            // 必须给 callback —— 不给的话 create() 会先撞上「callbacks is null」的 NPE，掩盖真正的原因
            e.setCallback(org.springframework.cglib.proxy.NoOp.INSTANCE);
            e.create();
        } catch (Throwable t) {
            System.out.println("  " + t.getClass().getName());
            System.out.println("  " + t.getMessage());
        }
        System.out.println("  -> 没有「办法」绕过：要生成子类，就必须能被继承");

        sec("C Spring 里同一件事的表现");
        Calc.CONSTRUCTED.set(0);
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(Cfg.class)) {
            Calc bean = ctx.getBean(Calc.class);
            System.out.println("  类名 = " + bean.getClass().getName());
            System.out.println("  isCglibProxy = " + AopUtils.isCglibProxy(bean));
            TouchAspect.BEFORE.set(0);
            bean.add(1, 2);
            System.out.println("  add() 之后 before = " + TouchAspect.BEFORE.get());
            int before = TouchAspect.BEFORE.get();
            bean.sealedAdd(1, 2);
            System.out.println("  sealedAdd() 之后 before 增量 = " + (TouchAspect.BEFORE.get() - before));

            sec("E Spring 用 CGLIB 时，Calc 构造器被调了几次");
            System.out.println("  容器里只有一个 Calc Bean，构造器计数 = " + Calc.CONSTRUCTED.get());
            System.out.println("  -> 1 次。直接用 Enhancer.create() 时也是 1 次（A 段），");
            System.out.println("     因为那个代理实例本身要 new 出来；Spring 有两条路可选：");
            System.out.println("     ① 用 Objenesis 直接分配对象、不跑构造器（CglibAopProxy 默认走这条）；");
            System.out.println("     ② 退化成跑一次构造器 —— 那样字段会初始化两次、副作用执行两遍");
        }

        sec("D 三种调用路径耗时（500 万次，3 轮取最小）");
        Calc direct = new Calc();
        long sink = 0;

        // warmup：三个路径都先跑热，否则第一轮测的是解释执行
        for (int i = 0; i < 300_000; i++) {
            sink += direct.add(i, i);
        }
        Method reflect = Calc.class.getMethod("add", int.class, int.class);

        // 从拦截器里把 MethodProxy 捞出来 —— 它只能由 CGLIB 生成，没有公开构造器
        final MethodProxy[] holder = new MethodProxy[1];
        Enhancer e2 = new Enhancer();
        e2.setSuperclass(Calc.class);
        e2.setCallback((MethodInterceptor) (o, m, a, p) -> {
            holder[0] = p;
            return p.invokeSuper(o, a);
        });
        Calc probe = (Calc) e2.create();
        probe.add(1, 2);
        MethodProxy mp = holder[0];

        long bestDirect = Long.MAX_VALUE;
        long bestReflect = Long.MAX_VALUE;
        long bestMpInvoke = Long.MAX_VALUE;
        long bestMpSuper = Long.MAX_VALUE;
        long bestProxyCall = Long.MAX_VALUE;
        final int n = 5_000_000;
        // 复用同一个数组，把「每轮 new Object[]」这笔账从 MethodProxy 头上摘掉
        Object[] boxed = new Object[2];

        for (int round = 0; round < 3; round++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                sink += direct.add(i, 1);
            }
            bestDirect = Math.min(bestDirect, System.nanoTime() - t0);

            t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                sink += (Integer) reflect.invoke(direct, i, 1);
            }
            bestReflect = Math.min(bestReflect, System.nanoTime() - t0);

            t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                boxed[0] = i;
                boxed[1] = 1;
                sink += (Integer) mp.invoke(direct, boxed);
            }
            bestMpInvoke = Math.min(bestMpInvoke, System.nanoTime() - t0);

            t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                boxed[0] = i;
                boxed[1] = 1;
                sink += (Integer) mp.invokeSuper(probe, boxed);
            }
            bestMpSuper = Math.min(bestMpSuper, System.nanoTime() - t0);

            Counting fast = new Counting();
            Calc fastProxy = enhance(fast);
            t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                sink += fastProxy.add(i, 1);
            }
            bestProxyCall = Math.min(bestProxyCall, System.nanoTime() - t0);
        }

        System.out.printf("  直接调用          %6.1f ns/次%n", bestDirect / (double) n);
        System.out.printf("  Method.invoke     %6.1f ns/次  (%.1fx)%n",
                bestReflect / (double) n, bestReflect / (double) bestDirect);
        System.out.printf("  MethodProxy.invoke %5.1f ns/次  (%.1fx)%n",
                bestMpInvoke / (double) n, bestMpInvoke / (double) bestDirect);
        System.out.printf("  MethodProxy.invokeSuper %3.1f ns/次  (%.1fx)  ← 拦截器里的那条%n",
                bestMpSuper / (double) n, bestMpSuper / (double) bestDirect);
        System.out.printf("  走一遍代理对象     %6.1f ns/次  (%.1fx)%n",
                bestProxyCall / (double) n, bestProxyCall / (double) bestDirect);
        System.out.println("  （sink=" + sink + "，用来防止 JIT 把整个循环消掉）");
        System.out.println("  -> MethodProxy 生成的是「直接调父类方法的字节码」，绕开反射的类型检查与参数装箱，");
        System.out.println("     所以比 Method.invoke 快；但再快也快不过直接调用 —— 这是 CGLIB 的固定开销");
        System.out.println("  -> 读数字的规矩：直接调用 0.4 ns 已经贴着 JIT 下限（方法体只有一次加法、循环被展开内联），");
        System.out.println("     「倍」是相对这个下限算的；真到业务里方法体一变长，这些倍数会被一起摊薄");
        System.out.println("     （W03 Day 4 手写 AQS 时踩过同一条：临界区 1 ns 慢 5.8 倍，拉到 0.5 µs 只慢 1.13 倍）");

        System.out.println();
        System.out.println("一句话：CGLIB = 运行时生成子类并覆写方法，所以「能继承」是硬前提；"
                + "final 类不能代、final 方法拦不到、private 方法它看不见，三种情况都不报错。");
    }
}
