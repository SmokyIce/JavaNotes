package com.newt.java48.w06.day2.stock;

/**
 * 故意放在**另一个子包**里、也没有接口的类。
 *
 * <p>两个用途：
 * <ol>
 *   <li>证明 {@code execution(* com.newt.java48.w06.day2..*.*(..))} 的 {@code ..}
 *       会跨包命中 —— 这就是「切点写宽了」的实物证据；</li>
 *   <li>没有接口 → 自动走 CGLIB，正好给 Day 2 的 CGLIB 实验当靶子。</li>
 * </ol>
 */
public class Warehouse {

    public static int CONSTRUCTED;

    public Warehouse() {
        CONSTRUCTED++;
    }

    public String stock(String sku) {
        return "stock:" + sku;
    }

    /** final：CGLIB 覆写不了 —— Day 1 已验证，这里用来对照 private 方法。 */
    public final String sealed() {
        return "sealed";
    }

    private String secret() {
        return "secret";
    }

    /** private 方法只能从类内部调，代理永远看不到这次调用。 */
    public String callSecret() {
        return secret();
    }
}
