package com.newt.java48.w05.day1.app;

import com.newt.java48.w05.support.Trace;

/**
 * 第二个实现，<b>故意不写任何 Spring 注解</b>。
 *
 * <p>它是「换实现」那一节的另一半：写不写 {@code @Repository} 决定了容器认不认它 ——
 * 这是「注解不是元数据，是注册指令」最直接的证据。
 */
public class MemoryOrderRepository implements OrderRepository {

    public MemoryOrderRepository() {
        Trace.constructed("MemoryOrderRepository", "无注解版本");
    }

    @Override
    public String findById(long id) {
        return "memory-goods-" + id;
    }

    @Override
    public String impl() {
        return "memory";
    }
}
