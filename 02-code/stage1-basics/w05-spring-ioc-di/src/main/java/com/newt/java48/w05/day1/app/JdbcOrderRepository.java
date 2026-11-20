package com.newt.java48.w05.day1.app;

import com.newt.java48.w05.support.Trace;
import org.springframework.stereotype.Repository;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认实现。构造一次就等于开一个「连接池」，所以实例数是有成本的。
 *
 * <p>计数器是给「手写版三个入口 = 三份仓储」那段用的证据：
 * 不看哈希值，直接数实例个数。
 */
@Repository
public class JdbcOrderRepository implements OrderRepository {

    private static final AtomicInteger INSTANCES = new AtomicInteger();

    private final String url = "jdbc:mysql://localhost:3306/shop";

    public JdbcOrderRepository() {
        INSTANCES.incrementAndGet();
        Trace.constructed("JdbcOrderRepository", "url=" + url);
    }

    public static int instances() {
        return INSTANCES.get();
    }

    @Override
    public String findById(long id) {
        return "goods-" + id;
    }

    @Override
    public String impl() {
        return "jdbc";
    }
}
