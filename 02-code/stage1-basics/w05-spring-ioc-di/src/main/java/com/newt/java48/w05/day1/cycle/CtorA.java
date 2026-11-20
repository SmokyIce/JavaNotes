package com.newt.java48.w05.day1.cycle;

import com.newt.java48.w05.support.Trace;
import org.springframework.stereotype.Component;

/**
 * 循环依赖的一半：构造器注入。
 *
 * <p>这时候容器<b>没得选</b>：要造 A 必须先有一个 B，要造 B 必须先有一个 A ——
 * 而对象都还没造出来，连「提前暴露一个半成品引用」的机会都没有。
 */
@Component
public class CtorA {

    private final CtorB b;

    public CtorA(CtorB b) {
        this.b = b;
        Trace.constructed("CtorA", "b=" + b);
    }

    public CtorB b() {
        return b;
    }
}
