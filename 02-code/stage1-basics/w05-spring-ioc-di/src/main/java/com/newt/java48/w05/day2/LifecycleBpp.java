package com.newt.java48.w05.day2;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 只有两行逻辑的后处理器：在初始化前后各打一条时间戳。
 *
 * <p>它存在的意义是把 {@code ②} 和 {@code ⑥} 这两个「容器钩子」插进六个回调的序列里，
 * 从而证明：<b>{@code @PostConstruct} 不是容器直接调的，而是在
 * {@code postProcessBeforeInitialization} 的遍历过程中被另一个后处理器调起来的</b>。
 *
 * <p>只对 {@code lifecycleBean} 打印 —— 这个后处理器本身对容器里<b>每一个</b> Bean 都会跑一遍，
 * 全打出来会把 A 段刷爆（这件事放到 {@link PostProcessorTrace} 里单独演示）。
 */
public class LifecycleBpp implements BeanPostProcessor {

    private static final String WATCH = "lifecycleBean";

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (WATCH.equals(beanName)) {
            LifecycleOrder.log("② BPP.postProcessBeforeInitialization", "初始化回调还没开始跑");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (WATCH.equals(beanName)) {
            LifecycleOrder.log("⑥ BPP.postProcessAfterInitialization", "三种初始化都跑完了 —— AOP 代理就在这一步把对象换掉");
        }
        return bean;
    }
}
