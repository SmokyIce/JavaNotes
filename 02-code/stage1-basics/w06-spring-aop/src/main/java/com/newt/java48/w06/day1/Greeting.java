package com.newt.java48.w06.day1;

/**
 * 被代理的业务接口。
 *
 * <p>故意拆成「接口 + 实现」两个文件：JDK 动态代理只能代理接口，
 * 而 Spring AOP 默认也走 JDK 代理 —— 前提正是「目标类实现了接口」。
 *
 * <p>后三个方法（outer*）是为 SelfInvocation 准备的：同一个类内部的调用链，
 * 区别只在「用 this 还是用代理引用」。
 */
public interface Greeting {

    String hi(String name);

    String bye(String name);

    /** 内部用 this.hi() —— 自调用，不经过代理。 */
    String outer(String name);

    /** 内部用注入进来的 self.hi() —— 绕过 this，回到代理。 */
    String outerViaSelf(String name);

    /** 内部用 AopContext.currentProxy().hi() —— 从 ThreadLocal 里取当前代理。 */
    String outerViaAopContext(String name);
}
