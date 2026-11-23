package com.newt.java48.w05.day2;

import javax.annotation.PostConstruct;

/**
 * 一个被后处理器 {@code @Autowired} 进去的普通 Bean。
 *
 * <p>它会在 {@code registerBeanPostProcessors} 期间就被创建 ——
 * 那时后处理器还没注册完，所以它<b>一次后处理都拿不到</b>（表里 0/0），
 * Spring 还会打一条 {@code is not eligible for getting processed by all BeanPostProcessors} 的告警。
 *
 * <p>面试里问到「为什么我的 {@code @Transactional} 在这个类上不生效」，
 * 有一类答案就在这里：这个类被某个后处理器 / 监听器 / 拦截器提前注入了，代理没机会套上去。
 */
public class EarlyDependent {

    public EarlyDependent() {
        LifecycleOrder.log("EarlyDependent 构造", "被后处理器依赖 → 被迫提前创建");
    }

    @PostConstruct
    public void init() {
        LifecycleOrder.log("EarlyDependent @PostConstruct", "初始化照样跑，但没有后处理器看得见它");
    }

    public String hello() {
        return "early";
    }
}
