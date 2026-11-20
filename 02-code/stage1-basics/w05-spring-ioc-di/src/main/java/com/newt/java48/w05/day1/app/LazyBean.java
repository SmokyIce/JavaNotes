package com.newt.java48.w05.day1.app;

import com.newt.java48.w05.support.Trace;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 一个「懒」的 Bean：{@code @Lazy} 让它在 refresh 之后才被创建。
 *
 * <p>它存在的唯一作用是给「什么时候 new 的」这个问题一个反证 ——
 * 默认单例是在 refresh 里被 eagerly 造出来的，而 {@code @Lazy} 把这行代码推到了第一次 getBean。
 */
@Lazy
@Component
public class LazyBean {

    public LazyBean() {
        Trace.constructed("LazyBean", "@Lazy：refresh 时不造");
    }

    public String hello() {
        return "LazyBean 终于出生了";
    }
}
