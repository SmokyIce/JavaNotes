package com.newt.java48.w05.support;

/**
 * 脚手架：把「这个回调是谁调的」从栈上读出来。
 *
 * <p>只留下 {@code org.springframework.*} 和 {@code com.newt.*} 的帧 —— JVM 内部帧
 * （反射、代理）会把真正有信息量的那几帧挤到几十行之外。
 *
 * <p>用法上有个硬要求：<b>必须真的跑一遍再抄行号</b>。行号随版本变，Spring 5.3.39 的
 * 1783 / 1796 / 1800 / 1805 是这一版的事，换个版本就对不上。
 */
public final class Stacks {

    private Stacks() {
    }

    /**
     * 打印当前栈里「有信息量」的帧。
     *
     * @param title 标题，会带两格缩进打出来
     * @param max   最多打几帧（防止刷屏）
     */
    public static void springFrames(String title, int max) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        System.out.println("  " + title);
        int printed = 0;
        for (StackTraceElement e : st) {
            String c = e.getClassName();
            if (c.equals(Stacks.class.getName())) {
                continue;
            }
            boolean keep = c.startsWith("org.springframework") || c.startsWith("com.newt");
            if (!keep) {
                continue;
            }
            System.out.printf("      at %s.%s(%s:%d)%n", c, e.getMethodName(), e.getFileName(), e.getLineNumber());
            if (++printed >= max) {
                break;
            }
        }
        if (printed == 0) {
            System.out.println("      （没有 spring / com.newt 的帧 —— 说明这次调用不经过容器）");
        }
    }
}
