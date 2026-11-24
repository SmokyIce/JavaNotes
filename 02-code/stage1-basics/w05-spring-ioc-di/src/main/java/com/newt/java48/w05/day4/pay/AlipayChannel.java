package com.newt.java48.w05.day4.pay;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 候选一。bean 名 = {@code alipayChannel}（类名首字母小写），{@code @Order(2)}。
 */
@Component
@Order(2)
public class AlipayChannel implements PayChannel {

    @Override
    public String tag() {
        return "alipay";
    }
}
