package com.newt.java48.w05.day1.app;

import com.newt.java48.w05.support.Trace;
import org.springframework.stereotype.Service;

/**
 * 业务类。两个依赖都走<b>构造器注入</b> —— 手写版才能照样 {@code new} 出来。
 *
 * <p>注意构造器里打印了「拿到的 impl 是什么」：Spring 版会打 jdbc / memory 两次不同的值，
 * 而 OrderService 本身一个字符没改 —— 这就是「换实现不动业务代码」的实证。
 */
@Service
public class OrderService {

    private final OrderRepository repository;
    private final PriceCalculator calculator;

    public OrderService(OrderRepository repository, PriceCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
        Trace.constructed("OrderService", "repo=" + repository.impl());
    }

    public String place(long id) {
        String goods = repository.findById(id);
        return "订单#" + id + " 商品=" + goods + " 金额=" + calculator.price(goods)
                + " 仓储=" + repository.impl();
    }

    public OrderRepository repository() {
        return repository;
    }
}
