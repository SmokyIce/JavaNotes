package com.newt.java48.w05.day4.pay;

/**
 * Day 4 的被注入目标：一个只有三个实现的支付渠道接口。
 *
 * <p>故意做成只有**一个**抽象方法 —— 这样 {@code @Configuration} 里可以写成 lambda，
 * 省掉一堆只为实验存在的实现类。
 */
public interface PayChannel {

    /** 渠道代号，注入结果靠它区分。 */
    String tag();
}
