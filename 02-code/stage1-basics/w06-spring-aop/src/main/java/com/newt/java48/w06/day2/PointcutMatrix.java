package com.newt.java48.w06.day2;

import com.newt.java48.w06.day2.shop.Traced;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 八种切点写法，各配一个 {@code @Before} —— 调一圈方法之后，
 * {@link #HITS} 就是一张「谁命中谁没命中」的矩阵。
 *
 * <p>{@link #recording} 是一个手动开关：容器 refresh、以及打印日志时都可能碰到
 * 目标对象的方法（尤其 {@code toString()}），不关掉就会混进无关记录。
 * 只在「我要观测的那一次调用」前后打开它。
 */
@Aspect
public class PointcutMatrix {

    /** key = 切点名，value = 命中的 {@code 类#方法(声明参数类型)} 列表（按调用顺序）。 */
    public static final Map<String, List<String>> HITS = new LinkedHashMap<>();

    /** 单线程实验，用普通 boolean 就够；只在主流程里成对置 true/false。 */
    public static boolean recording;

    static {
        for (String k : new String[]{
                "execution buy(String)",
                "execution buy(Object)",
                "within(Shop) 单类",
                "within(shop..*) 整包",
                "within(Buyer) 接口",
                "args(String)",
                "args(String,..)",
                "@annotation(Traced)",
        }) {
            HITS.put(k, new ArrayList<>());
        }
    }

    // ---------------------------------------------------------------- 切点

    /** 精确到参数声明类型：只吃 buy(String)，不吃 buy(Object)，也不吃 buy(String,int)。 */
    @Pointcut("execution(* com.newt.java48.w06.day2.shop.Shop.buy(String))")
    public void execBuyString() {
    }

    /** 与上一个只差一个参数类型 —— 用来证明 execution 认的是**声明**类型。 */
    @Pointcut("execution(* com.newt.java48.w06.day2.shop.Shop.buy(Object))")
    public void execBuyObject() {
    }

    /** 按「方法声明在哪个类」匹配：Shop 自己的方法全中，Cart / Warehouse 一个都不中。 */
    @Pointcut("within(com.newt.java48.w06.day2.shop.Shop)")
    public void withinShop() {
    }

    /** 同一个 within 换成包路径：整个 shop 包都进来了，包括根本没想拦的 Cart。 */
    @Pointcut("within(com.newt.java48.w06.day2.shop..*)")
    public void withinShopPackage() {
    }

    /** 把 within 写成接口 —— 这是最典型的「写窄了」，命中数会是 0。 */
    @Pointcut("within(com.newt.java48.w06.day2.shop.Buyer)")
    public void withinBuyer() {
    }

    /** 运行时实参：只要「恰好一个参数，且它是 String」就中，跟声明类型无关。 */
    @Pointcut("args(String)")
    public void argsOneString() {
    }

    /** 运行时实参：第一个是 String，后面几个不管。 */
    @Pointcut("args(String,..)")
    public void argsStringFirst() {
    }

    /** 方法上有没有那个注解。 */
    @Pointcut("@annotation(com.newt.java48.w06.day2.shop.Traced)")
    public void annotated() {
    }

    // ---------------------------------------------------------------- 通知

    @Before("execBuyString()")
    public void a1(JoinPoint jp) {
        record("execution buy(String)", jp);
    }

    @Before("execBuyObject()")
    public void a2(JoinPoint jp) {
        record("execution buy(Object)", jp);
    }

    @Before("withinShop()")
    public void a3(JoinPoint jp) {
        record("within(Shop) 单类", jp);
    }

    @Before("withinShopPackage()")
    public void a4(JoinPoint jp) {
        record("within(shop..*) 整包", jp);
    }

    @Before("withinBuyer()")
    public void a5(JoinPoint jp) {
        record("within(Buyer) 接口", jp);
    }

    @Before("argsOneString()")
    public void a6(JoinPoint jp) {
        record("args(String)", jp);
    }

    @Before("argsStringFirst()")
    public void a7(JoinPoint jp) {
        record("args(String,..)", jp);
    }

    @Before("annotated()")
    public void a8(JoinPoint jp) {
        record("@annotation(Traced)", jp);
    }

    // ---------------------------------------------------------------- 工具

    private static void record(String key, JoinPoint jp) {
        if (!recording) {
            return;
        }
        HITS.get(key).add(label(jp));
    }

    /** {@code Shop#buy(String)} —— 用**声明**参数类型，才能区分重载。 */
    static String label(JoinPoint jp) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        StringBuilder sb = new StringBuilder();
        sb.append(jp.getTarget().getClass().getSimpleName())
                .append('#')
                .append(sig.getName())
                .append('(');
        Class<?>[] pTypes = sig.getParameterTypes();
        for (int i = 0; i < pTypes.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(pTypes[i].getSimpleName());
        }
        return sb.append(')').toString();
    }

    public static void clear() {
        HITS.values().forEach(List::clear);
    }
}
