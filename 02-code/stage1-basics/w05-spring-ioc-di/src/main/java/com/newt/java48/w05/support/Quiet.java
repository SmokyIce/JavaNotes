package com.newt.java48.w05.support;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 脚手架：把 Spring 在 {@code refresh()} 失败时打的那条 JUL 警告关掉。
 *
 * <p>Day 4 的实验大量是「故意让容器启动失败」，每条失败都会打一遍完整异常，和下面自己打印的
 * 异常链完全重复。Day 2 / Day 3 的 JUL WARN 是证据所以留着，这里不是。
 *
 * <p>踩过的坑：一开始按父类名 {@code AbstractApplicationContext} 写成 logger 名，<b>压不掉</b> ——
 * 因为 {@code AbstractApplicationContext} 里的字段是 {@code LogFactory.getLog(getClass())}，
 * 拿到的 logger 名是<b>子类</b> {@code org.springframework.context.annotation.AnnotationConfigApplicationContext}；
 * 面板上显示的父类名只是 {@code SimpleFormatter} 打 sourceClass 的结果。
 * 这个名字是枚举 {@code LogManager.getLoggerNames()} 查出来的，不是猜的。
 */
public final class Quiet {

    private Quiet() {
    }

    /** 关掉「容器 refresh 失败」这一条日志，不动别的 logger。 */
    public static void springRefreshWarning() {
        Logger.getLogger("org.springframework.context.annotation.AnnotationConfigApplicationContext")
                .setLevel(Level.OFF);
    }
}
