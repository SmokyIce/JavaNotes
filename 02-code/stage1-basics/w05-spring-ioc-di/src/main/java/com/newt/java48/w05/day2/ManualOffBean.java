package com.newt.java48.w05.day2;

/**
 * 和 {@link AutoCloseBean} 一模一样的 {@code close()}，唯一区别是
 * 声明处写了 {@code @Bean(destroyMethod = "")} —— 空字符串表示<b>显式关闭推断</b>。
 *
 * <p>两个一起跑，一个计数 1 一个计数 0，「默认会推断」这件事就不用背了。
 */
public class ManualOffBean {

    private static int closes;

    public ManualOffBean() {
        LifecycleOrder.log("ManualOffBean 构造", "同样有 close()，但 destroyMethod 被置空");
    }

    public void close() {
        closes++;
        LifecycleOrder.log("ManualOffBean.close()", "如果这行打出来了，说明推断没有被关掉");
    }

    public static int closes() {
        return closes;
    }
}
