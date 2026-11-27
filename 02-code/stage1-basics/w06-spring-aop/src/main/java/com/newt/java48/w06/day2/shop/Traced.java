package com.newt.java48.w06.day2.shop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义方法注解 —— 只为了给 {@code @annotation(...)} 这种切点当靶子。
 *
 * <p>{@code RUNTIME} 是硬要求：切点匹配发生在运行期，{@code CLASS} / {@code SOURCE}
 * 的注解在字节码里根本留不到那一刻。这一条是「注解式切点不生效」最常见的死因之一。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Traced {
}
