package com.newt.java48.w05.day2;

/**
 * 有一个「看起来无害」的 {@code close()} 方法，<b>没有</b>在任何地方声明它是销毁方法。
 *
 * <p>但 {@code @Bean} 的 {@code destroyMethod} 默认值是 {@code AbstractBeanDefinition.INFER_METHOD}
 * （字符串 {@code "(inferred)"}）：容器会自动去找 {@code close()} 或 {@code shutdown()} 并调用。
 * 这是 @Bean 与 @Component 的一个大差别 —— 很多人不知道自己的 {@code close()} 已经在容器关闭时被调过了。
 *
 * <p>要关掉它：{@code @Bean(destroyMethod = "")}，见 {@link ManualOffBean}。
 */
public class AutoCloseBean {

    private static int closes;

    public AutoCloseBean() {
        LifecycleOrder.log("AutoCloseBean 构造", "close() 不在任何注解里");
    }

    public void close() {
        closes++;
        LifecycleOrder.log("AutoCloseBean.close()", "@Bean 默认推断出来的销毁方法，被调了");
    }

    public static int closes() {
        return closes;
    }
}
