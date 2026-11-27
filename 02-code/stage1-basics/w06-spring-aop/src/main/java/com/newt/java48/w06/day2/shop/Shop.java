package com.newt.java48.w06.day2.shop;

/**
 * 切点的靶子。方法签名故意做成参差不齐，好让各种切点表达式区分出差别来。
 *
 * <p>注意 {@link #buy(Object)} 与 {@link #buy(String)} 是重载：
 * 传 {@code "apple"} 会选中 {@code buy(String)}，想调 Object 版必须写 {@code buy((Object) "apple")}。
 */
public class Shop implements Buyer {

    @Override
    public String buy(String item) {
        return "buy:" + item;
    }

    @Override
    public String buy(Object payload) {
        return "buyObject:" + payload;
    }

    @Override
    public String buy(String item, int qty) {
        return "buy:" + item + "x" + qty;
    }

    @Override
    public int count() {
        return 42;
    }

    @Override
    @Traced
    public String tagged() {
        return "tagged";
    }

    /** 只有注解、不带参数的普通方法，用来验证 `@annotation` 是不是只认注解。 */
    public String plain() {
        return "plain";
    }
}
