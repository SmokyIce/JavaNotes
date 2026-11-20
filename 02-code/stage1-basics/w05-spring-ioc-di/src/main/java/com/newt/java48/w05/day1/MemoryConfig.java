package com.newt.java48.w05.day1;

import com.newt.java48.w05.day1.app.JdbcOrderRepository;
import com.newt.java48.w05.day1.app.MemoryOrderRepository;
import com.newt.java48.w05.day1.app.OrderRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * 「换实现」的 Spring 版：多加一个 {@code @Bean}，把扫进来的 jdbc 版本排掉。
 *
 * <p>关键在<b>改哪儿</b>：三个入口（Controller / Job / Test）一个字符都没改 ——
 * 变的是「装配配置」，不是「调用点」。手写版恰恰相反：三个入口都得改。
 */
@Configuration
@ComponentScan(
        basePackages = "com.newt.java48.w05.day1.app",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JdbcOrderRepository.class))
public class MemoryConfig {

    @Bean
    public OrderRepository orderRepository() {
        return new MemoryOrderRepository();
    }
}
