package com.newt.java48.w05.day1.cycle;

import com.newt.java48.w05.support.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 循环依赖的一半：字段注入。
 *
 * <p>构造的时候 {@code b} 一定是 null —— 字段注入发生在对象造好之后，
 * 这正是「容器可以先给出一个半成品引用、再往里灌字段」的前提。
 */
@Component
public class FieldA {

    @Autowired
    private FieldB b;

    public FieldA() {
        Trace.constructed("FieldA", "构造时 b=" + b);
    }

    public boolean linked() {
        return b != null;
    }

    public FieldB b() {
        return b;
    }
}
