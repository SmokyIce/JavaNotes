package com.newt.java48.w05.day1.cycle;

import com.newt.java48.w05.support.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 循环依赖的另一半：字段注入，和 {@link FieldA} 互相持有。 */
@Component
public class FieldB {

    @Autowired
    private FieldA a;

    public FieldB() {
        Trace.constructed("FieldB", "构造时 a=" + a);
    }

    public boolean linked() {
        return a != null;
    }

    public FieldA a() {
        return a;
    }
}
