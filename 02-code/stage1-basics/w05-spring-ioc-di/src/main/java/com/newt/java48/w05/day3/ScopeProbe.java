package com.newt.java48.w05.day3;

import org.springframework.beans.factory.DisposableBean;

import javax.annotation.PreDestroy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * W05 Day 3 · 量作用域用的「探针 Bean」。
 *
 * <p>作用域这件事只有三个判据，全部都能在同一个类上量出来：
 * <ol>
 *   <li>连续 {@code getBean} 两次，拿到的是不是同一个对象（身份 {@code ==}）；</li>
 *   <li>它进不进单例池（{@code beanFactory.containsSingleton(name)}）；</li>
 *   <li>容器 {@code close()} 时销毁回调会不会被调（{@code @PreDestroy} 与
 *       {@code DisposableBean.destroy()} 各记一次，方便确认「两个都没跑」而不是「跑了一个」）。</li>
 * </ol>
 *
 * <p>计数器全是 {@code static}，按 {@code role}（= 注入时给的名字）分开记 ——
 * 因为同一个类会被挂上不同作用域，混在一起就分不清是谁的账。
 */
public class ScopeProbe implements DisposableBean {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final Map<String, Integer> CREATED = new LinkedHashMap<>();
    private static final Map<String, Integer> PRE_DESTROYED = new LinkedHashMap<>();
    private static final Map<String, Integer> DESTROYED = new LinkedHashMap<>();

    private String role = "proxy?";
    private int id;

    /** CGLIB 生成作用域代理时需要能构造父类（实际实例由 Objenesis 造，不会进这里的计数）。 */
    public ScopeProbe() {
        this("no-arg");
    }

    public ScopeProbe(String role) {
        this.role = role;
        this.id = SEQ.incrementAndGet();
        bump(CREATED, role);
    }

    public int id() {
        return id;
    }

    public String role() {
        return role;
    }

    public String describe() {
        return role + "#" + id;
    }

    @PreDestroy
    public void preDestroy() {
        bump(PRE_DESTROYED, role);
    }

    @Override
    public void destroy() {
        bump(DESTROYED, role);
    }

    // ------------------------------------------------------------------ 计数

    private static void bump(Map<String, Integer> map, String key) {
        map.merge(key, 1, Integer::sum);
    }

    public static int created(String role) {
        return CREATED.getOrDefault(role, 0);
    }

    public static int preDestroyed(String role) {
        return PRE_DESTROYED.getOrDefault(role, 0);
    }

    public static int destroyed(String role) {
        return DESTROYED.getOrDefault(role, 0);
    }

    public static void reset() {
        CREATED.clear();
        PRE_DESTROYED.clear();
        DESTROYED.clear();
        SEQ.set(0);
    }
}
