package com.newt.java48.w05.day4.pay;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 候选三。bean 名 = {@code unionPayChannel}，{@code @Order(3)}。
 *
 * <p>三个实现、三个不同的 order，是为了让「集合注入的顺序」有唯一正确答案 ——
 * 只有两个的话，注册顺序和 order 顺序很容易碰巧一致，看不出差别。
 */
@Component
@Order(3)
public class UnionPayChannel implements PayChannel {

    @Override
    public String tag() {
        return "unionpay";
    }
}
