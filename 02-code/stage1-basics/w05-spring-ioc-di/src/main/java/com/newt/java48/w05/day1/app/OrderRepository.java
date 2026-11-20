package com.newt.java48.w05.day1.app;

/**
 * 「同一套类」里的接口 —— 手写版和 Spring 版都要用。
 *
 * <p>注意：接口本身没有注解。Spring 在注入时找的是「类型」，不是「名字」，
 * 所以下面两个实现谁被选中，决定了 {@code impl()} 打印什么。
 */
public interface OrderRepository {

    /** 假装查库。 */
    String findById(long id);

    /** 打印实现名，用来证明「换实现」真的换掉了。 */
    String impl();
}
