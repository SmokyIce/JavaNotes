package com.newt.java48.w05.day1.app;

import com.newt.java48.w05.support.Trace;
import org.springframework.stereotype.Component;

/** 入口一：Web 层。它只认识 OrderService，不认识仓储。 */
@Component
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
        Trace.constructed("OrderController", null);
    }

    public String handle(long id) {
        return "HTTP 200 ← " + service.place(id);
    }
}
