package com.newt.java48.w05.day2;

/**
 * 一个再简单不过的依赖，只为了证明一件事：
 * <b>字段上的 {@code @Autowired} 是在「构造之后、初始化回调之前」被灌进去的</b>。
 *
 * <p>所以 {@link LifecycleBean} 的构造器里读它一定是 {@code null}，
 * 到了 {@code @PostConstruct} 里再读就必须有值 —— 这两个观测点夹住了属性注入的时机。
 */
public class LifecycleRepo {

    public String impl() {
        return "jdbc";
    }
}
