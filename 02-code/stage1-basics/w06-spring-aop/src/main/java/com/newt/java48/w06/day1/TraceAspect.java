package com.newt.java48.w06.day1;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 唯一的切面。三段实验共用它，所以每段开工必须先 {@link #reset()}。
 *
 * <p>三个计数器分别对应三种通知，用来回答「一次调用被拦了几次」；
 * {@link #EVENTS} 记的是「谁被拦了」，顺序就是实际执行顺序。
 *
 * <p>{@link #LAST_THIS} / {@link #LAST_TARGET} 是这次实验的核心证据：
 * {@code getThis()} 是代理对象，{@code getTarget()} 是被包着的真身。
 */
@Aspect
public class TraceAspect {

    public static final AtomicInteger BEFORE = new AtomicInteger();
    public static final AtomicInteger AROUND = new AtomicInteger();
    public static final AtomicInteger AFTER = new AtomicInteger();

    public static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

    public static Object LAST_THIS;
    public static Object LAST_TARGET;

    @Pointcut("within(com.newt.java48.w06.day1.GreetingImpl) "
            + "|| within(com.newt.java48.w06.day1.PlainJob)")
    public void targets() {
    }

    @Before("targets()")
    public void before(JoinPoint jp) {
        BEFORE.incrementAndGet();
        LAST_THIS = jp.getThis();
        LAST_TARGET = jp.getTarget();
        EVENTS.add("@Before " + jp.getSignature().toShortString());
    }

    @Around("targets()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        AROUND.incrementAndGet();
        EVENTS.add("@Around 进入 " + pjp.getSignature().getName());
        Object ret = pjp.proceed();
        EVENTS.add("@Around 返回 " + pjp.getSignature().getName());
        return ret;
    }

    @After("targets()")
    public void after(JoinPoint jp) {
        AFTER.incrementAndGet();
        EVENTS.add("@After " + jp.getSignature().getName());
    }

    /** 每段实验开工先清零 —— 静态状态不归零，后一段就替前一段付账。 */
    public static void reset() {
        BEFORE.set(0);
        AROUND.set(0);
        AFTER.set(0);
        EVENTS.clear();
        LAST_THIS = null;
        LAST_TARGET = null;
    }

    public static String counts() {
        return "before=" + BEFORE.get() + " around=" + AROUND.get() + " after=" + AFTER.get();
    }

    /** 「谁被拦了」的紧凑视图：@Before 一次调用只打一次，正好当清单用。 */
    public static List<String> intercepted() {
        List<String> names = new ArrayList<>();
        synchronized (EVENTS) {
            for (String e : EVENTS) {
                if (e.startsWith("@Before ")) {
                    String s = e.substring("@Before ".length());
                    int p = s.indexOf('(');
                    names.add(p > 0 ? s.substring(0, p) : s);
                }
            }
        }
        return names;
    }
}
