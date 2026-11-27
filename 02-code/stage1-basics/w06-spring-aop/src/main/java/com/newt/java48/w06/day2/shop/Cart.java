package com.newt.java48.w06.day2.shop;

/**
 * 和 {@link Shop} 同包、不实现 {@link Buyer} 的第二个业务类。
 *
 * <p>存在理由：证明「用包路径写切点」和「用类名写切点」的差别 ——
 * {@code within(com.newt.java48.w06.day2.shop..*)} 会把整个包收进来，
 * 而 {@code within(Shop)} 只收那一个类。这是「写宽了」最常见的一种写法。
 */
public class Cart {

    public String add(String sku) {
        return "add:" + sku;
    }

    public int size() {
        return 0;
    }
}
