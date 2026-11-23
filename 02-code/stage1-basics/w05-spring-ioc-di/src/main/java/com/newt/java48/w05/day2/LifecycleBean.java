package com.newt.java48.w05.day2;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 一个 Bean 上同时挂 <b>三种初始化</b> + <b>三种销毁</b>，每个回调打一条时间戳。
 *
 * <table border="1">
 *   <caption>六种回调 + 两个 BPP 钩子</caption>
 *   <tr><th>#</th><th>时机</th><th>谁写的</th></tr>
 *   <tr><td>①</td><td>构造器</td><td>我</td></tr>
 *   <tr><td>②</td><td>{@code BeanPostProcessor.postProcessBeforeInitialization}</td><td>容器（钩子）</td></tr>
 *   <tr><td>③</td><td>{@code @PostConstruct}</td><td>我（注解）</td></tr>
 *   <tr><td>④</td><td>{@code InitializingBean.afterPropertiesSet}</td><td>我（接口）</td></tr>
 *   <tr><td>⑤</td><td>{@code initMethod}</td><td>我（配置里点名）</td></tr>
 *   <tr><td>⑥</td><td>{@code BeanPostProcessor.postProcessAfterInitialization}</td><td>容器（钩子）</td></tr>
 *   <tr><td>⑦</td><td>{@code @PreDestroy}</td><td>我（注解）</td></tr>
 *   <tr><td>⑧</td><td>{@code DisposableBean.destroy}</td><td>我（接口）</td></tr>
 *   <tr><td>⑨</td><td>{@code destroyMethod}</td><td>我（配置里点名）</td></tr>
 * </table>
 *
 * <p>三种初始化写了三遍 = 三种写法都能生效，但<b>顺序是固定的</b>：
 * 注解 → 接口 → 配置点名的那个。这个顺序不是约定，是
 * {@code AbstractAutowireCapableBeanFactory.initializeBean} 里几行代码决定的。
 */
public class LifecycleBean implements InitializingBean, DisposableBean {

    /** 字段注入点。构造器里读一定是 null —— 这就是「属性注入晚于构造」的实证。 */
    @Autowired
    private LifecycleRepo repo;

    private String state = "NEW";

    public LifecycleBean() {
        LifecycleOrder.log("① 构造器", "repo=" + (repo == null ? "null" : "有值")
                + "   state=" + state + "   ← 此刻 @Autowired 还没发生");
    }

    @PostConstruct
    public void postConstruct() {
        state = "POST_CONSTRUCT";
        LifecycleOrder.log("③ @PostConstruct", "repo=" + (repo == null ? "null" : repo.impl())
                + "   ← 属性注入已经完成，所以这里必须不是 null");
    }

    @Override
    public void afterPropertiesSet() {
        state = "AFTER_PROPERTIES_SET";
        LifecycleOrder.log("④ afterPropertiesSet", "接口方式：类一旦 implements 就必然被调，去都去不掉");
    }

    /** 由 {@code @Bean(initMethod = "customInit")} 点名。 */
    public void customInit() {
        state = "READY";
        LifecycleOrder.log("⑤ initMethod=customInit", "配置方式：同一个类可以换一个方法，不用改类");
    }

    public String describe() {
        return "LifecycleBean{state=" + state + ", repo=" + repo.impl() + "}";
    }

    @PreDestroy
    public void preDestroy() {
        LifecycleOrder.log("⑦ @PreDestroy", "容器 close() 时，第一个被调的销毁回调");
    }

    @Override
    public void destroy() {
        LifecycleOrder.log("⑧ DisposableBean.destroy", "接口方式：销毁时同理，必然被调");
    }

    /** 由 {@code @Bean(destroyMethod = "customDestroy")} 点名。 */
    public void customDestroy() {
        LifecycleOrder.log("⑨ destroyMethod=customDestroy", "三个销毁回调到这里结束");
    }
}
