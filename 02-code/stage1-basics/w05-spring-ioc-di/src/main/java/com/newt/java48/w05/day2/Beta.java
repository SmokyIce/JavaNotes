package com.newt.java48.w05.day2;

import javax.annotation.PreDestroy;

/**
 * 被 {@link Alpha} 依赖的 Bean，用来和 Alpha 对比销毁顺序。
 */
public class Beta {

    public Beta() {
        LifecycleOrder.log("Beta 构造", "它先被造出来，但会后被销毁");
    }

    public String name() {
        return "beta";
    }

    @PreDestroy
    public void bye() {
        LifecycleOrder.log("Beta @PreDestroy", "被依赖方后死 —— 逆序的实证");
    }
}
