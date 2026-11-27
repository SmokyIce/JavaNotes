package com.newt.java48.w06.day2;

import com.newt.java48.w06.day2.shop.Buyer;
import com.newt.java48.w06.day2.shop.Cart;
import com.newt.java48.w06.day2.shop.Shop;
import com.newt.java48.w06.day2.stock.Warehouse;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 八种切点写法，同一批方法各打一次 —— 看谁命中、谁不命中。
 *
 * <pre>
 *   S1  一张命中矩阵（CGLIB 统一代理，10 次调用）
 *   S2  「写窄了」：within(接口) 命中 0 次
 *   S3  「写宽了」：within 用包路径，同包另一个类也被收
 *   S4  args 看运行时实参，execution 看声明类型 —— 差在 buy(Object) 那一行
 *   S5  切点匹配的是「目标类」不是「代理类」：JDK 代理下 within(Shop) 照样命中
 *   S6  但 JDK 代理下「接口里没有的方法」根本不进切面
 * </pre>
 *
 * <p>配置类用 {@code proxyBeanMethods = false}：不加这句，宽切点会把配置类自己的
 * {@code @Bean} 工厂方法也拦掉，容器直接起不来 —— 那一幕单独放在
 * {@code WidePointcutTrap} 里当反面教材。
 */
public class PointcutForms {

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class CglibConfig {
        @Bean
        public Shop shop() {
            return new Shop();
        }

        @Bean
        public Cart cart() {
            return new Cart();
        }

        @Bean
        public Warehouse warehouse() {
            return new Warehouse();
        }

        @Bean
        public PointcutMatrix pointcutMatrix() {
            return new PointcutMatrix();
        }
    }

    /** 默认配置：Shop 有接口 → JDK 代理；Warehouse 没接口 → 自动 CGLIB。 */
    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class JdkConfig {
        @Bean
        public Shop shop() {
            return new Shop();
        }

        @Bean
        public Cart cart() {
            return new Cart();
        }

        @Bean
        public Warehouse warehouse() {
            return new Warehouse();
        }

        @Bean
        public PointcutMatrix pointcutMatrix() {
            return new PointcutMatrix();
        }
    }

    static void sec(String s) {
        System.out.println();
        System.out.println("== " + s + " ==");
    }

    /** 跑一遍固定的调用脚本，全程开着 recording。 */
    static void sweep(Shop shop, Cart cart, Warehouse wh) {
        PointcutMatrix.clear();
        PointcutMatrix.recording = true;
        shop.buy("apple");                 // buy(String)
        shop.buy((Object) "apple");        // buy(Object)，但运行时实参是 String
        shop.buy("apple", 3);              // buy(String,int)
        shop.count();                      // int count()
        shop.tagged();                     // 带 @Traced
        shop.plain();                      // 普通方法，不在接口里
        cart.add("pear");                  // 同包，但另一个类
        cart.size();
        wh.stock("sku-1");                 // 另一个子包、另一个类
        shop.toString();                   // Object 继承来的方法
        PointcutMatrix.recording = false;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("PointcutForms · 八种切点各拦到什么");
        System.out.println("Spring = " + org.springframework.core.SpringVersion.getVersion());

        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(CglibConfig.class);
        Shop shop = ctx.getBean(Shop.class);
        Cart cart = ctx.getBean(Cart.class);
        Warehouse wh = ctx.getBean(Warehouse.class);

        System.out.println();
        System.out.println("三个目标对象：");
        System.out.println("  shop      = " + shop.getClass().getName()
                + "   isCglib=" + AopUtils.isCglibProxy(shop));
        System.out.println("  cart      = " + cart.getClass().getName());
        System.out.println("  warehouse = " + wh.getClass().getName()
                + "   isCglib=" + AopUtils.isCglibProxy(wh));

        sec("S1 命中矩阵（每格列出被拦的「类#方法(声明参数类型)」）");
        sweep(shop, cart, wh);
        for (Map.Entry<String, List<String>> e : PointcutMatrix.HITS.entrySet()) {
            List<String> v = e.getValue();
            System.out.printf("  %-26s 命中 %d 个  %s%n",
                    e.getKey(), v.size(), v.isEmpty() ? "—" : v.toString());
        }
        System.out.println("  调用脚本共 10 次：buy(String) / buy(Object) / buy(String,int) / count()"
                + " / tagged() / plain() / Cart#add / Cart#size / Warehouse#stock(String) / toString()");

        sec("S2 「写窄了」：within 写成接口，命中 0");
        System.out.println("  within(Shop)         => " + PointcutMatrix.HITS.get("within(Shop) 单类").size() + " 个");
        System.out.println("  within(Buyer 接口)   => " + PointcutMatrix.HITS.get("within(Buyer) 接口").size() + " 个");
        System.out.println("  -> within 问的是「这个方法声明在哪个类里」，不是「这个对象实现了谁」；");
        System.out.println("     接口只是对外的写法清单，不是代码定义的位置 —— 写接口名 = 一条都拦不到，而且不报错、不告警");

        sec("S3 「写宽了」：within 用包路径，同包的 Cart 一起进来");
        List<String> wholePackage = PointcutMatrix.HITS.get("within(shop..*) 整包");
        List<String> singleClass = PointcutMatrix.HITS.get("within(Shop) 单类");
        System.out.println("  within(shop..*) => " + wholePackage.size() + " 个 : " + wholePackage);
        System.out.println("  within(Shop)    => " + singleClass.size() + " 个 : " + singleClass);
        System.out.println("  差集（多拦的）   = " + diff(wholePackage, singleClass));
        System.out.println("  -> 你只想给 Shop 开事务，结果 Cart 也进了；一旦切面里有写操作 / 审计日志，这就是脏数据");

        sec("S4 args 看运行时实参，execution 看声明类型");
        System.out.println("  buy(String)       被 execution buy(String) 拦 = "
                + PointcutMatrix.HITS.get("execution buy(String)").contains("Shop#buy(String)"));
        System.out.println("  buy((Object)str)  被 execution buy(Object) 拦 = "
                + PointcutMatrix.HITS.get("execution buy(Object)").contains("Shop#buy(Object)"));
        System.out.println("  buy((Object)str)  被 args(String) 拦          = "
                + PointcutMatrix.HITS.get("args(String)").contains("Shop#buy(Object)"));
        System.out.println("  buy(Object) 的声明参数类型 = " + declaredFirstParam(shop, "buy"));
        System.out.println("  args(String)    命中 = " + PointcutMatrix.HITS.get("args(String)"));
        System.out.println("  args(String,..) 命中 = " + PointcutMatrix.HITS.get("args(String,..)"));
        System.out.println("  -> execution 用「方法签名上的类型」，args 用「这一枪打出去的实参类型」；");
        System.out.println("     所以 args(String) 会命中声明成 Object 的方法，也会命中别的类 —— 它只认参数，不认方法属于谁；");
        System.out.println("     反过来 buy(String,int) 明明是 String 开头，args(String) 却不命中：参数个数也要对上");

        sec("S5 切点匹配的是「目标类」不是「代理类」：换回默认 JDK 代理");
        AnnotationConfigApplicationContext jdkCtx =
                new AnnotationConfigApplicationContext(JdkConfig.class);
        Buyer jdkShop = jdkCtx.getBean(Buyer.class);
        System.out.println("  shop = " + jdkShop.getClass().getName()
                + "   isJdk=" + AopUtils.isJdkDynamicProxy(jdkShop));
        try {
            Shop byImpl = jdkCtx.getBean(Shop.class);
            System.out.println("  按实现类取到了（不该发生）：" + byImpl.getClass().getName());
        } catch (NoSuchBeanDefinitionException e) {
            System.out.println("  getBean(Shop.class) -> NoSuchBeanDefinitionException（Day 1 S2b 的同一条）");
        }
        PointcutMatrix.clear();
        PointcutMatrix.recording = true;
        jdkShop.buy("apple");
        PointcutMatrix.recording = false;
        System.out.println("  within(Shop) 命中 = " + PointcutMatrix.HITS.get("within(Shop) 单类"));
        System.out.println("  -> 代理类里根本没有 Shop 这个名字，切点照样命中：");
        System.out.println("     匹配是在「目标类的方法」上算的，算完才决定包不包代理 —— 所以表达式与代理方式无关");

        sec("S6 但 JDK 代理下「接口里没有的方法」连调用都发不出去");
        try {
            ((Shop) jdkShop).plain();
        } catch (ClassCastException e) {
            System.out.println("  强转 (Shop) proxy -> " + e.getMessage());
        }
        Object real = ((Advised) jdkShop).getTargetSource().getTarget();
        System.out.println("  从 Advised 里掏出真身 = " + real.getClass().getName());
        PointcutMatrix.clear();
        PointcutMatrix.recording = true;
        String plainRet = ((Shop) real).plain();
        PointcutMatrix.recording = false;
        System.out.println("  真身调 plain() 返回 = " + plainRet);
        System.out.println("  within(Shop) 命中 = " + PointcutMatrix.HITS.get("within(Shop) 单类") + "（空）");
        System.out.println("  -> plain() 不在 Buyer 接口里，JDK 代理生成的类压根没这个方法；");
        System.out.println("     要调它只能强转（CCE）或绕过代理拿真身 —— 两条路都不经过切面。");
        System.out.println("     物理原因和 this.xxx() 完全一样：代理只活在「别人手里那根接口引用」上。");
        System.out.println("     这是「加了 @Transactional 却没事务」的第二条路 —— 方法没写在接口里");

        System.out.println();
        System.out.println("一句话：切点由两个轴组成 —— 在哪（within / execution 的类路径）和 什么方法（名字、参数、注解）；"
                + "写窄了安静地不生效，写宽了安静地多拦一堆，两种都不报错。");

        System.out.println();
        System.out.println("（全宽的 execution(* *(..)) 不在这个矩阵里 —— 它会把配置类自己的 @Bean 方法也拦掉，"
                + "启动就炸，见 WidePointcutTrap）");

        jdkCtx.close();
        ctx.close();
    }

    static List<String> diff(List<String> a, List<String> b) {
        List<String> r = new java.util.ArrayList<>(a);
        r.removeAll(b);
        return r;
    }

    /** 反射读一下 buy(Object) 重载的声明参数类型，用来对比 args 的运行时视角。 */
    static String declaredFirstParam(Object target, String name) {
        Class<?> c = AopUtils.getTargetClass(target);
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 1
                    && m.getParameterTypes()[0] != String.class) {
                return m.getParameterTypes()[0].getName();
            }
        }
        return "?";
    }
}
