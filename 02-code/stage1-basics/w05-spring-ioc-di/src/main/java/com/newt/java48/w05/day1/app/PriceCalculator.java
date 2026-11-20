package com.newt.java48.w05.day1.app;

import com.newt.java48.w05.support.Trace;
import org.springframework.stereotype.Component;

/** 一个无关紧要的协作对象，作用是让 OrderService 有「第二个依赖」。 */
@Component
public class PriceCalculator {

    public PriceCalculator() {
        Trace.constructed("PriceCalculator", null);
    }

    public int price(String goods) {
        return goods.length() * 100;
    }
}
