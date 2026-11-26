package com.newt.java48.w06.day1;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 纯 JDK 动态代理 —— 不引入 Spring 的任何东西。
 *
 * <p>目的：在看 {@code @Aspect} 之前，先把「拦截」这件事的最小机制摸清。
 * Spring AOP 的 JDK 路线就是这一句话：<b>生成一个实现同样接口的类，把所有方法转发给
 * {@link InvocationHandler}</b>。
 *
 * <p>八段实验回答八个问题：
 * <pre>
 *   A 不代理，计数是多少                      -> 0
 *   B 代理一次调用，handler 进几次            -> 1
 *   C 代理内部再调自己的另一个方法，进几次    -> 还是 1（嵌套调用不经过 handler）
 *   D 代理套代理，进几次                      -> 2（一层一次）
 *   E 同样的接口再 newProxyInstance，类复用吗 -> 复用；接口变了才换类
 *   F Object 的三个方法（toString/hashCode/equals）走 handler 吗 -> 走
 *   G 代理对象能强转成实现类吗                -> 不能，ClassCastException
 *   H 接口不是 public 时，代理类放哪个包      -> 放进接口所在的包，不再是 jdk.proxy1
 * </pre>
 */
public class JdkProxy {

    // ---------- 被代理的东西 ----------

    public interface Shop {
        String place(String sku);

        String cancel(String no);

        /** 内部再调 place / cancel —— 用来测「嵌套调用进不进 handler」。 */
        String placeThenCancel(String sku);
    }

    public static class ShopImpl implements Shop {        static final AtomicInteger PLACE = new AtomicInteger();
        static final AtomicInteger CANCEL = new AtomicInteger();

        @Override
        public String place(String sku) {
            PLACE.incrementAndGet();
            return "ORDER-" + sku;
        }

        @Override
        public String cancel(String no) {
            CANCEL.incrementAndGet();
            return "CANCELLED-" + no;
        }

        @Override
        public String placeThenCancel(String sku) {
            return this.cancel(this.place(sku));
        }

        @Override
        public String toString() {
            return "ShopImpl";
        }
    }

    /**
     * package-private 接口。用它生成的代理类会被放进**接口所在的包**，
     * 类名就不再是 {@code jdk.proxy1.$ProxyN}，而是 {@code com.newt.java48.w06.day1.$ProxyN}。
     */
    interface Hidden {
        String ping();
    }

    public static class HiddenImpl implements Hidden {
        @Override
        public String ping() {
            return "pong";
        }
    }

    // ---------- 计数器与 handler ----------

    static final AtomicInteger HITS = new AtomicInteger();
    static final List<String> TRACE = Collections.synchronizedList(new ArrayList<>());

    static class CountingHandler implements InvocationHandler {
        private final Object target;

        CountingHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            HITS.incrementAndGet();
            TRACE.add(method.getDeclaringClass().getSimpleName() + "." + method.getName());
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                // 反射会把业务异常包一层，这里必须拆掉，否则调用方看到的是 ITE
                throw e.getCause();
            }
        }
    }

    static Shop wrap(Shop target) {
        return (Shop) Proxy.newProxyInstance(
                Shop.class.getClassLoader(),
                new Class<?>[]{Shop.class},
                new CountingHandler(target));
    }

    static void reset() {
        HITS.set(0);
        TRACE.clear();
        ShopImpl.PLACE.set(0);
        ShopImpl.CANCEL.set(0);
    }

    static void sec(String s) {
        System.out.println();
        System.out.println("== " + s + " ==");
    }

    // ---------- 主流程 ----------

    public static void main(String[] args) {
        System.out.println("JdkProxy · 手写 JDK 动态代理：handler 到底进几次");
        System.out.println("JDK = " + System.getProperty("java.version"));

        ShopImpl impl = new ShopImpl();

        sec("A 不代理，直接调 impl.place");
        reset();
        String a = impl.place("A1");
        System.out.println("  返回 = " + a);
        System.out.println("  handler 进入次数 = " + HITS.get() + "   业务里 place 执行次数 = " + ShopImpl.PLACE.get());
        System.out.println("  -> 没有代理，就没有拦截；业务照跑，计数是 0");

        sec("B 代理一次调用");
        reset();
        Shop proxy = wrap(impl);
        System.out.println("  代理类名 = " + proxy.getClass().getName());
        System.out.println("  proxy instanceof Shop            = " + (proxy instanceof Shop));
        System.out.println("  proxy.getClass() == ShopImpl.class = " + (proxy.getClass() == ShopImpl.class));
        String b = proxy.place("B1");
        System.out.println("  返回 = " + b);
        System.out.println("  handler 进入次数 = " + HITS.get() + "   业务里 place 执行次数 = " + ShopImpl.PLACE.get());
        System.out.println("  -> 一次调用 = 一次 handler；方法体是靠 method.invoke(target) 反射调进去的");

        sec("C 代理内部再调自己的另一个方法（嵌套调用）");
        reset();
        String c = proxy.placeThenCancel("C1");
        System.out.println("  返回 = " + c);
        System.out.println("  handler 进入次数 = " + HITS.get());
        System.out.println("  handler 看到的调用链 = " + TRACE);
        System.out.println("  业务里 place 执行次数 = " + ShopImpl.PLACE.get()
                + "   cancel 执行次数 = " + ShopImpl.CANCEL.get());
        System.out.println("  -> 业务真跑了 3 个方法，handler 只进 1 次：");
        System.out.println("     嵌套的那两个是 invokevirtual 直连 impl，跟代理没关系 —— 这就是 this.xxx() 的物理原因");

        sec("D 代理套代理");
        reset();
        Shop outer = wrap(proxy);
        System.out.println("  内层类 = " + proxy.getClass().getSimpleName()
                + "   外层类 = " + outer.getClass().getSimpleName());
        System.out.println("  两个代理是同一个类吗 = " + (outer.getClass() == proxy.getClass()));
        String d = outer.place("D1");
        System.out.println("  返回 = " + d);
        System.out.println("  handler 进入次数 = " + HITS.get() + "   调用链 = " + TRACE);
        System.out.println("  -> 一层代理一次，2 次；每层都反射转一次，所以代理链越深越慢");

        sec("E 同样的接口再生成一个代理，类复用吗");
        Shop p1 = wrap(impl);
        Shop p2 = wrap(impl);
        Object p3 = Proxy.newProxyInstance(
                Shop.class.getClassLoader(),
                new Class<?>[]{Shop.class, java.io.Serializable.class},
                new CountingHandler(impl));
        System.out.println("  p1 类 = " + p1.getClass().getName());
        System.out.println("  p2 类 = " + p2.getClass().getName() + "   p1 与 p2 同类 = " + (p1.getClass() == p2.getClass()));
        System.out.println("  p3 类 = " + p3.getClass().getName() + "   p1 与 p3 同类 = " + (p1.getClass() == p3.getClass()));
        System.out.println("  -> 类是按「接口组合」缓存的：接口不变就复用同一个 $Proxy，每次 newProxyInstance 只是换 handler");
        System.out.println("     所以「动态代理」动态的是「生成时机」，不是「每次都生成新类」");

        sec("F Object 的三个方法走 handler 吗");
        reset();
        proxy.hashCode();
        int afterHash = HITS.get();
        proxy.toString();
        int afterToString = HITS.get();
        proxy.equals(proxy);
        int afterEquals = HITS.get();
        System.out.println("  hashCode() 后计数 = " + afterHash
                + "   toString() 后 = " + afterToString
                + "   equals() 后 = " + afterEquals);
        System.out.println("  调用链 = " + TRACE);
        System.out.println("  -> 三个都进了 handler（toString 被转发到 impl，所以打印出 " + proxy + " 而不是类名）");
        System.out.println("     副作用：debug 时随手一个 toString 也会触发切面 / 甚至触发懒加载");

        sec("G 代理对象能强转成实现类吗");
        try {
            ShopImpl cast = (ShopImpl) proxy;
            System.out.println("  强转成功（不该发生）：" + cast);
        } catch (ClassCastException e) {
            System.out.println("  ClassCastException: " + e.getMessage());
        }
        System.out.println("  -> 代理和 impl 是兄弟不是父子：只共享接口，没有继承关系");
        System.out.println("     这也是「按实现类类型注入会失败」的根源（见 ProxyIdentity 的 S2b）");

        sec("H 接口不是 public 时，代理类放到哪个包");
        Hidden hidden = (Hidden) Proxy.newProxyInstance(
                Hidden.class.getClassLoader(),
                new Class<?>[]{Hidden.class},
                new CountingHandler(new HiddenImpl()));
        System.out.println("  代理类名 = " + hidden.getClass().getName());
        System.out.println("  调用 ping = " + hidden.ping());
        System.out.println("  包名     = " + hidden.getClass().getPackageName());
        System.out.println("  -> public 接口的代理在 jdk.proxy1（动态模块）里，");
        System.out.println("     package-private 接口的代理被塞进接口所在的包 —— 同包才访问得到那个接口。");
        System.out.println("     所以「代理类在哪个包」由接口的可见性决定，不是固定值（老 JDK 上还叫 com.sun.proxy.$Proxy0）");

        System.out.println();
        System.out.println("一句话：动态代理 = 运行时生成一个实现同样接口的类，"
                + "每个方法都变成 handler.invoke(proxy, method, args)。");
    }
}
