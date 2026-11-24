package com.newt.java48.w05.day4;

import com.newt.java48.w05.day4.pay.AlipayChannel;
import com.newt.java48.w05.day4.pay.PayChannel;
import com.newt.java48.w05.day4.pay.UnionPayChannel;
import com.newt.java48.w05.day4.pay.WechatChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.newt.java48.w05.support.Quiet;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * W05 Day 4 · 第二段：多个候选不是只能「挑一个」—— 也可以全收。
 *
 * <p>{@code List} / {@code Map} / 数组三种集合形状，回答三个问题：
 * <ol>
 *   <li>顺序谁定？{@code @Order} 管不管 {@code Map}？（实测：只管 List / 数组，Map 按注册顺序）</li>
 *   <li>{@code Map} 的 key 是什么？（bean 名，不是类名、不是 {@code tag()}）</li>
 *   <li>零候选呢？<b>「集合注入给空集合」是讹传</b> —— 实测 {@code required=true} 照样炸，
 *       {@code required=false} 给的是 {@code null} 不是空集合。</li>
 * </ol>
 */
public class CollectionInject {

    public static void main(String[] args) {
        Quiet.springRefreshWarning();
        sectionA();
        sectionB();
        sectionC();
        sectionD();
    }

    // ------------------------------------------------------------------ 被测 bean

    /** 三个候选全收，看顺序。 */
    @Component
    static class AllChannels {
        @Autowired
        List<PayChannel> list;

        @Autowired
        Map<String, PayChannel> map;

        @Autowired
        PayChannel[] array;
    }

    /** 给集合加 @Qualifier，看能不能过滤候选集。 */
    @Component
    static class FilteredList {
        @Autowired
        @Qualifier("alipayChannel")
        List<PayChannel> list;
    }

    // ------------------------------------------------------------------ A

    private static void sectionA() {
        System.out.println("=== A. List<PayChannel>：顺序由 @Order 决定 ===");

        AllChannels bean = ctxOf(AlipayChannel.class, WechatChannel.class, UnionPayChannel.class,
                AllChannels.class).getBean(AllChannels.class);

        System.out.print("  注册顺序 = alipay, wechat, unionpay；@Order = wechat(1) < alipay(2) < unionpay(3)");
        System.out.println();
        System.out.print("  List 实际顺序 = ");
        bean.list.forEach(c -> System.out.print(c.tag() + " "));
        System.out.println();
        System.out.printf("  结论：List 按 @Order 排（%s），不是注册顺序%n", tags(bean.list));

        System.out.print("  PayChannel[] 实际顺序 = ");
        Arrays.stream(bean.array).forEach(c -> System.out.print(c.tag() + " "));
        System.out.println("  ← 数组和 List 同源，排序也一致");

        System.out.print("  Map 实际顺序 = ");
        bean.map.forEach((k, v) -> System.out.print(k + "=" + v.tag() + " "));
        System.out.println();
        System.out.println("  ↑ 注意 Map 的顺序跟 List 不一样 —— 这一格是这一段的重点，见 B。");
    }

    // ------------------------------------------------------------------ B

    /**
     * B：{@code Map} 的两个事实 —— key 是 bean 名；<b>{@code @Order} 对它无效</b>。
     *
     * <p>Map 的迭代顺序 = 候选表 {@code findAutowireCandidates} 的插入顺序 = bean 定义注册顺序。
     * 这一点和 List 的处理分支不同（List 里有 {@code AnnotationAwareOrderComparator.sort}，Map 没有）。
     */
    private static void sectionB() {
        System.out.println();
        System.out.println("=== B. Map<String, PayChannel>：key 是谁，顺序听谁的 ===");

        AllChannels bean = ctxOf(AlipayChannel.class, WechatChannel.class, UnionPayChannel.class,
                AllChannels.class).getBean(AllChannels.class);

        System.out.println("  key 集合 = " + bean.map.keySet());
        System.out.println("  key 是 bean 名（类名首字母小写），不是类全名、也不是 tag() 的返回值。");
        System.out.println("  顺序 = " + bean.map.keySet() + " ← 注册顺序，@Order 在这里没生效");
        System.out.println("  而同一个容器里的 List 是 " + tags(bean.list) + " ← @Order 生效了");
        System.out.println("  所以「按顺序执行一串策略」必须用 List；Map 只适合按名字精确取。");
    }

    /** 三段对照的被测 bean：required=true / required=false / 单值。 */
    static class RequiredList {
        @Autowired
        List<PayChannel> list;
    }

    static class OptionalList {
        @Autowired(required = false)
        List<PayChannel> list;

        @Autowired(required = false)
        Map<String, PayChannel> map;
    }

    static class SingleNeed {
        @Autowired
        PayChannel channel;
    }

    // ------------------------------------------------------------------ C

    /**
     * C：零候选的三段对照 —— 这一段的结论和我一开始写的不一样。
     *
     * <p>「集合注入在没有候选时会给你一个空集合」—— 这句话在 {@code required=true}（默认）下<b>是错的</b>：
     * {@code resolveMultipleBeans} 发现候选表是空的就返回 {@code null}，于是流程落回单值分支，
     * 然后 {@code raiseNoMatchingBeanFound} 直接抛。只有 {@code required=false} 才放行，而放行给的是
     * {@code null}，不是 {@code Collections.emptyList()}。
     */
    private static void sectionC() {
        System.out.println();
        System.out.println("=== C. 容器里一个 PayChannel 都没有 ===");

        System.out.println("  C1. @Autowired（默认 required=true）List<PayChannel>：");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(RequiredList.class);
        try {
            ctx.refresh();
            System.out.println("      ✗ 没炸 —— 与「空集合」的说法一致，但与我的预期不符");
        } catch (Throwable t) {
            Throwable root = rootOf(t);
            System.out.println("      " + root.getClass().getSimpleName() + ":");
            for (String line : String.valueOf(root.getMessage()).split("\n")) {
                if (!line.isBlank()) {
                    System.out.println("        " + line.trim());
                }
            }
        }
        ctx.close();
        System.out.println("      ↑ 注意类型写的是 List<PayChannel>，报的却是「至少要有 1 个」—— 集合没有特权。");

        System.out.println();
        System.out.println("  C2. @Autowired(required=false) List / Map：");
        AnnotationConfigApplicationContext ctx2 = new AnnotationConfigApplicationContext();
        ctx2.register(OptionalList.class);
        ctx2.refresh();
        OptionalList ok = ctx2.getBean(OptionalList.class);
        System.out.printf("      List → %s%n", ok.list == null ? "null（不是空集合！）" : "size=" + ok.list.size());
        System.out.printf("      Map  → %s%n", ok.map == null ? "null（不是空 Map！）" : "size=" + ok.map.size());
        System.out.println("      所以「收成集合就天然为空」是假的：要么 required=false 后再判 null，"
                + "要么写 `orElse(emptyList)`。");
        ctx2.close();

        System.out.println();
        System.out.println("  C3. 同样空容器里注入单值 PayChannel：");
        AnnotationConfigApplicationContext ctx3 = new AnnotationConfigApplicationContext();
        ctx3.register(SingleNeed.class);
        try {
            ctx3.refresh();
        } catch (Throwable t) {
            Throwable root = rootOf(t);
            System.out.println("      " + root.getClass().getSimpleName() + ":");
            for (String line : String.valueOf(root.getMessage()).split("\n")) {
                if (!line.isBlank()) {
                    System.out.println("        " + line.trim());
                }
            }
        }
        ctx3.close();
        System.out.println("      跟 C1 是同一个异常类型 —— 集合和单值在「没有候选」这一点上没有区别。");
    }

    // ------------------------------------------------------------------ D

    /** D：{@code @Qualifier} 能不能把集合缩小？ */
    private static void sectionD() {
        System.out.println();
        System.out.println("=== D. 集合 + @Qualifier：缩范围而不是挑一个 ===");

        FilteredList bean = ctxOf(AlipayChannel.class, WechatChannel.class, UnionPayChannel.class,
                FilteredList.class).getBean(FilteredList.class);
        System.out.println("  @Qualifier(\"alipayChannel\") List<PayChannel> → " + tags(bean.list));
        System.out.println("  候选按 qualifier 先缩到 1 个，再「收全部」—— 所以只剩 alipay，不会报 NoUnique。");
    }

    // ------------------------------------------------------------------ 工具

    private static AnnotationConfigApplicationContext ctxOf(Class<?>... classes) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(classes);
        ctx.refresh();
        return ctx;
    }

    private static Throwable rootOf(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }

    private static String tags(List<PayChannel> list) {
        StringBuilder sb = new StringBuilder();
        for (PayChannel c : list) {
            if (sb.length() > 0) {
                sb.append(" → ");
            }
            sb.append(c.tag());
        }
        return sb.toString();
    }
}
