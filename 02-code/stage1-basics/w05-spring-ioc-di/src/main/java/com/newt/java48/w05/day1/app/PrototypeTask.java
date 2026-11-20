package com.newt.java48.w05.day1.app;

import com.newt.java48.w05.support.Trace;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/** 原型作用域：每次 getBean 都造一个新的。和单例的差别用一个计数器 + 哈希值证明。 */
@Component
@Scope("prototype")
public class PrototypeTask {

    private static final AtomicInteger INSTANCES = new AtomicInteger();

    public PrototypeTask() {
        INSTANCES.incrementAndGet();
        Trace.constructed("PrototypeTask", "第 " + INSTANCES.get() + " 个实例");
    }

    public static int instances() {
        return INSTANCES.get();
    }
}
