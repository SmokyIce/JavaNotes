package com.newt.java48.w05.day2;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会计数的后处理器：对<b>每一个</b> Bean 的初始化前后各记一笔。
 *
 * <p>三件事靠它证明：
 * <ol>
 *   <li><b>它对注册之后创建的每个 Bean 都跑一遍</b> —— 普通 Bean 那几行都是 2/2（两个后处理器各一次）；</li>
 *   <li><b>它看不见「注册完成之前」就出生的 Bean</b> —— {@code earlyDependent} 是 0/0；
 *       而且<b>同一批的后处理器互相也看不见</b>：{@code bppFirst} / {@code bppSecond} 都是 0/0。
 *       原因是 {@code PostProcessorRegistrationDelegate:257~265} 先把这一批全部 {@code getBean}
 *       收集到局部 List，<b>然后才</b>统一注册 —— 收集的时候谁也还没进列表；</li>
 *   <li><b>多个后处理器之间按 {@link Ordered} 排序</b> —— {@code SEQ} 里 first 永远在 second 前面。</li>
 * </ol>
 *
 * <p>反过来，<b>更早阶段注册的</b>后处理器是看得见它的：本类故意带一个 {@code @PostConstruct}，
 * 它一定会被执行 —— 因为 {@code CommonAnnotationBeanPostProcessor} 属于 PriorityOrdered，
 * 在 253 行就已经注册完了，比我们早。
 *
 * <p>第 2 条是 AOP 失效的一大来源：如果一个 Bean 被提前创建（比如被某个后处理器
 * {@code @Autowired} 进去），它就<b>拿不到代理</b>，事务、异步注解全部白写。
 */
public class TraceBpp implements BeanPostProcessor, Ordered {

    private static final Map<String, int[]> COUNTS = new LinkedHashMap<>();
    private static final List<String> SEQ = new ArrayList<>();
    private static int beforeTotal;
    private static int afterTotal;

    private final String tag;
    private final int order;

    /**
     * 故意让第一个后处理器依赖一个普通 Bean —— 这个 Bean 会被迫在
     * 「后处理器还没注册齐」的时候提前创建，于是拿到
     * {@code is not eligible for getting processed by all BeanPostProcessors} 的告警。
     */
    @Autowired
    private EarlyDependent earlyDependent;

    public TraceBpp(String tag, int order) {
        this.tag = tag;
        this.order = order;
    }

    /**
     * 这个回调不是给我们自己看的，是给「更早注册的后处理器」看的：
     * 它被执行 = 说明 PriorityOrdered 阶段的 CommonAnnotationBeanPostProcessor 已经在列表里了。
     * 同时它不会出现在 {@link #COUNTS} 里 —— 因为那时我们还没注册。
     */
    @javax.annotation.PostConstruct
    public void init() {
        System.out.printf("  [后处理器自己的 @PostConstruct] %-6s 被执行 → 早注册的 CommonAnnotationBPP 看得见我%n", tag);
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        row(beanName)[0]++;
        beforeTotal++;
        if ("plainBean".equals(beanName)) {
            SEQ.add(tag + ".before");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        row(beanName)[1]++;
        afterTotal++;
        if ("plainBean".equals(beanName)) {
            SEQ.add(tag + ".after");
        }
        return bean;
    }

    private static int[] row(String beanName) {
        return COUNTS.computeIfAbsent(beanName, k -> new int[2]);
    }

    static Map<String, int[]> counts() {
        return COUNTS;
    }

    static List<String> seq() {
        return SEQ;
    }

    static int beforeTotal() {
        return beforeTotal;
    }

    static int afterTotal() {
        return afterTotal;
    }

    static int ownCount() {
        return counts().size();
    }

    static void reset() {
        COUNTS.clear();
        SEQ.clear();
        beforeTotal = 0;
        afterTotal = 0;
    }
}
