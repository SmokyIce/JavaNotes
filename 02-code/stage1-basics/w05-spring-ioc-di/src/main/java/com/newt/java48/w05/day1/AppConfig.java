package com.newt.java48.w05.day1;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 最朴素的 Spring 配置：一个注解扫一个包。
 *
 * <p>它自己不干活 —— 没有一行 {@code new}。所有对象图的拼装信息来自
 * 「类上的注解」+「构造器的签名」，这两样合起来就是「声明式装配」的全部输入。
 */
@Configuration
@ComponentScan(basePackages = "com.newt.java48.w05.day1.app")
public class AppConfig {
}
