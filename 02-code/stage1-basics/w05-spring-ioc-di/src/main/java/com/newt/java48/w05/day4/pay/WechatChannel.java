package com.newt.java48.w05.day4.pay;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 候选二。bean 名 = {@code wechatChannel}，{@code @Order(1)}。
 *
 * <p>这个名字今天会被用两次：一次当 {@code @Autowired} 字段名回退的靶子，
 * 一次当 {@code @Resource} 按名字的靶子。
 */
@Component
@Order(1)
public class WechatChannel implements PayChannel {

    @Override
    public String tag() {
        return "wechat";
    }
}
