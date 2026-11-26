package com.newt.java48.w06.day1;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * 业务实现。
 *
 * <p>三个 outer* 方法只差一行，但决定了 hi() 上的切面跑不跑：
 * <pre>
 *   this.hi(name)                       -> 直接字节码调用，不经过代理
 *   this.self.hi(name)                  -> self 是容器注入的代理引用，经过代理
 *   AopContext.currentProxy().hi(name)   -> 从 ThreadLocal 取当前代理（需 exposeProxy=true）
 * </pre>
 *
 * <p>注意 self 上的 {@code @Lazy}：不 lazy 就是「自己注入自己」的循环依赖，
 * 加了 lazy 注入的是一个延迟解析的代理，第一次调用时才回容器要真正的 bean。
 */
public class GreetingImpl implements Greeting {

    @Autowired
    @Lazy
    private Greeting self;

    @Override
    public String hi(String name) {
        return "hi " + name;
    }

    @Override
    public String bye(String name) {
        return "bye " + name;
    }

    @Override
    public String outer(String name) {
        // 编译成 invokevirtual GreetingImpl.hi —— 与代理对象无关
        return "outer->" + this.hi(name);
    }

    @Override
    public String outerViaSelf(String name) {
        return "outerViaSelf->" + this.self.hi(name);
    }

    @Override
    public String outerViaAopContext(String name) {
        return "outerViaAopContext->" + ((Greeting) AopContext.currentProxy()).hi(name);
    }

    /** 给 ProxyIdentity 用：打印注入进来的 self 到底是什么类。 */
    public Greeting selfRef() {
        return self;
    }
}
