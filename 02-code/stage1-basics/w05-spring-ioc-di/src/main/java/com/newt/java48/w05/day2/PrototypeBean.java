package com.newt.java48.w05.day2;

import org.springframework.beans.factory.DisposableBean;

import javax.annotation.PreDestroy;

/**
 * 原型作用域的 Bean：容器<b>管造不管埋</b>。
 *
 * <p>不是「忘了调」，而是<b>根本没登记</b> ——
 * {@code AbstractBeanFactory.registerDisposableBeanIfNecessary:1942} 第一行就是
 * {@code if (!mbd.isPrototype() && requiresDestruction(bean, mbd))}，
 * prototype 在登记这一步就被短路了，销毁阶段自然找不到它。
 *
 * <p>所以「原型 Bean 里的连接池没人关」是真实的内存泄漏源，
 * 只能自己 {@code getBean} 之后记得关。
 */
public class PrototypeBean implements DisposableBean {

    private static int instances;
    private static int destroys;

    public PrototypeBean() {
        LifecycleOrder.log("PrototypeBean 构造", "第 " + (++instances) + " 个实例");
    }

    @PreDestroy
    public void preDestroy() {
        destroys++;
        LifecycleOrder.log("PrototypeBean @PreDestroy", "如果这行打出来了，说明我错了");
    }

    @Override
    public void destroy() {
        destroys++;
        LifecycleOrder.log("PrototypeBean destroy()", "同上");
    }

    public static int destroys() {
        return destroys;
    }

    public static int instances() {
        return instances;
    }

    public static void reset() {
        destroys = 0;
        instances = 0;
    }
}
