package com.newt.java48.w05.day2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A 段的装配：一个依赖、一个身兼三种初始化与三种销毁的 Bean、一个后处理器。
 *
 * <p>注意 {@code initMethod} / {@code destroyMethod} 是写在 {@code @Bean} 上的 ——
 * 这正是「配置方式」和「接口方式」的区别：接口是类自己扛，配置是外部点名。
 * 同一个类放两个容器里，可以挂不同的 init 方法，类本身一行不改。
 */
@Configuration
public class LifecycleConfig {

    @Bean
    public LifecycleRepo lifecycleRepo() {
        return new LifecycleRepo();
    }

    @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
    public LifecycleBean lifecycleBean() {
        return new LifecycleBean();
    }

    /**
     * 后处理器自己也是一个 Bean，必须用 {@code static} 方法声明 ——
     * 非 static 会逼容器提前实例化 {@code LifecycleConfig} 本身，
     * 从而产生 "is not eligible for getting processed by all BeanPostProcessors" 的告警。
     * 这条坑在 {@link PostProcessorTrace} 的 C 段有实证。
     */
    @Bean
    public static LifecycleBpp lifecycleBpp() {
        return new LifecycleBpp();
    }
}
