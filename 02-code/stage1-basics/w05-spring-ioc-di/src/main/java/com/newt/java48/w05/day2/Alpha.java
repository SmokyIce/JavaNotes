package com.newt.java48.w05.day2;

import javax.annotation.PreDestroy;

/**
 * 依赖 {@link Beta} 的 Bean —— 用来观察<b>「谁先被销毁」</b>。
 *
 * <p>直觉是「谁先造谁先销毁」，实际是反的：容器按<b>注册顺序的逆序</b>销毁单例，
 * 而且有依赖关系时<b>先销毁依赖方</b>（否则被依赖的对象已经没了）。
 * 所以 Alpha（后造、依赖别人）一定先死，Beta 后死。
 */
public class Alpha {

    private final Beta beta;

    public Alpha(Beta beta) {
        this.beta = beta;
        LifecycleOrder.log("Alpha 构造", "依赖 beta=" + beta.name());
    }

    public String name() {
        return "alpha";
    }

    @PreDestroy
    public void bye() {
        LifecycleOrder.log("Alpha @PreDestroy", "依赖方先死 —— 此刻 beta 还活着");
    }
}
