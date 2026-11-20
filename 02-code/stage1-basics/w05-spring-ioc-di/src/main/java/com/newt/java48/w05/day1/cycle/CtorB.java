package com.newt.java48.w05.day1.cycle;

import com.newt.java48.w05.support.Trace;
import org.springframework.stereotype.Component;

/** 循环依赖的另一半：构造器注入，和 {@link CtorA} 互相要求对方先存在。 */
@Component
public class CtorB {

    private final CtorA a;

    public CtorB(CtorA a) {
        this.a = a;
        Trace.constructed("CtorB", "a=" + a);
    }

    public CtorA a() {
        return a;
    }
}
