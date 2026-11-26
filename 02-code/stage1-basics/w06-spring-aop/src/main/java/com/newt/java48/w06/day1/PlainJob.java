package com.newt.java48.w06.day1;

/**
 * 没有实现任何接口的类。
 *
 * <p>用途：证明「没接口 → Spring 自动退回 CGLIB」，以及
 * <b>final 方法静默不被增强</b>（CGLIB 靠生成子类覆写方法，final 覆写不了）。
 */
public class PlainJob {

    public String run() {
        return "plain ran";
    }

    /** final 方法：CGLIB 生成子类也覆写不了 → 切面在这里静默失效，没有任何警告。 */
    public final String sealed() {
        return "sealed ran";
    }
}
