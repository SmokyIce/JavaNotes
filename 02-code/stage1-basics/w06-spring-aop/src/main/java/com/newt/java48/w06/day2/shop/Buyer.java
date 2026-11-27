package com.newt.java48.w06.day2.shop;

/**
 * 业务接口。存在它的唯一理由是：证明 {@code within(接口)} 是个**写窄了**的切点。
 */
public interface Buyer {

    String buy(String item);

    /**
     * 参数声明成 {@code Object}，但实际传 {@code String}。
     *
     * <p>这是 {@code args()} 与 {@code execution()} 分歧最小白的一处：
     * 前者看**运行时实参**，后者看**声明类型**。
     */
    String buy(Object payload);

    String buy(String item, int qty);

    int count();

    @Traced
    String tagged();
}
