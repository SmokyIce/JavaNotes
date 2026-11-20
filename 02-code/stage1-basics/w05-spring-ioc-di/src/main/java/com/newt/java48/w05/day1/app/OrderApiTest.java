package com.newt.java48.w05.day1.app;

import com.newt.java48.w05.support.Trace;
import org.springframework.stereotype.Component;

/**
 * 入口三：冒烟测试。
 *
 * <p>现实里第三个入口常常是「另一个批处理」或者「测试用例」，它也得自己拼一遍对象图 ——
 * 手写版「换一个实现要改三处」的第三处就在这里。
 */
@Component
public class OrderApiTest {

    private final OrderService service;

    public OrderApiTest(OrderService service) {
        this.service = service;
        Trace.constructed("OrderApiTest", null);
    }

    public String run() {
        return "TEST 断言通过 ← " + service.place(7L);
    }
}
