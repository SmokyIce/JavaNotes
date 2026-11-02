package com.newt.java48.w02.day5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Day 5 · Stream（二）：流只能用一次、map vs flatMap、parallel 的代价
 *
 * <pre>
 * A  一个流只能用一次：三种案发现场 + Supplier&lt;Stream&gt; 正解
 * B  map 与 flatMap：一个是「换个形状」，一个是「拆开再铺平」
 * C  parallel：forEach 不保序、forEachOrdered 保序；小数据量下 parallel 反而更慢
 * D  什么时候不该用 Stream（这条比 API 列表有用）
 * </pre>
 *
 * <p>运行：{@code run.bat day5.StreamOnce}
 */
public class StreamOnce {

    private final List<Order> orders = List.of(
            new Order("A001", "张三", true, List.of(new Item("书", 59), new Item("笔", 8))),
            new Order("A002", "李四", false, List.of(new Item("杯子", 39))),
            new Order("A003", "王五", true, List.of(new Item("键盘", 299), new Item("鼠标", 129), new Item("垫子", 49))));

    /** 故意做成字段：这就是线上最常见的那种「流被缓存」事故。 */
    private Stream<Order> cached;

    public static void main(String[] args) {
        new StreamOnce().run();
    }

    private void run() {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        System.out.println("========== 收尾 ==========");
        System.out.println("Stream 是一个「一次性的游标」，不是集合。");
        System.out.println("要复用就复用**数据**（List）或**工厂**（Supplier<Stream>），绝不复用 Stream 本身。");
    }

    // ================================================================== A
    private void sectionA() {
        System.out.println("========== A · 一个流只能用一次 ==========");

        // A1 拿着变量用两次
        Stream<Order> s = orders.stream();
        long n1 = s.count();
        System.out.println("A1 第一次 count() -> " + n1);
        try {
            long n2 = s.count();
            System.out.println("A1 第二次 count() -> " + n2 + "（不该走到这里）");
        } catch (IllegalStateException e) {
            System.out.println("A1 第二次 count() -> " + e.getClass().getName() + ": " + e.getMessage());
        }

        // A2 缓存成字段：方法调用方完全看不出问题
        cached = orders.stream().filter(o -> o.amount() >= 100);
        System.out.println("A2 字段缓存流，第一次用 -> " + cached.count());
        try {
            System.out.println("A2 第二次用 -> " + cached.count());
        } catch (IllegalStateException e) {
            System.out.println("A2 第二次用 -> " + e.getClass().getName() + ": " + e.getMessage());
            System.out.println("A2 所以「把 Stream 当返回值 / 当字段」是 API 设计事故："
                    + "调用方看到的是 Stream，却不知道它已经烧掉了");
        }

        // A3 方法每次返回新流：安全
        long c1 = vipOrders().count();
        long c2 = vipOrders().count();
        System.out.println("A3 方法返回 Stream（每次都 new 一个）-> " + c1 + " / " + c2 + "，两次都正常");
        System.out.println("A3 区别在于：方法体里的 orders.stream() 每次调用都重新创建一个游标");

        // A4 正解：Supplier<Stream>
        Supplier<Stream<Order>> factory = () -> orders.stream().filter(Order::vip);
        System.out.println("A4 Supplier<Stream> 两次 -> " + factory.get().count() + " / "
                + factory.get().map(Order::id).collect(Collectors.toList()));

        // A5 真实事故：先 count 看看有几个，再 collect
        Stream<Order> once = orders.stream();
        System.out.println("A5 「先看看几条」-> " + once.count() + " 条");
        try {
            once.map(Order::id).collect(Collectors.toList());
        } catch (IllegalStateException e) {
            System.out.println("A5 再 collect -> " + e.getMessage());
            System.out.println("A5 这是最像人话、也最容易写出来的一种：日志里加了句 count，业务就挂了");
        }
        System.out.println();
    }

    // ================================================================== B
    private void sectionB() {
        System.out.println("========== B · map 换形状，flatMap 拆开铺平 ==========");

        List<List<Item>> nested = orders.stream()
                .map(Order::items)
                .collect(Collectors.toList());
        System.out.println("B1 map(Order::items) -> " + nested.size() + " 个元素，每个还是一个 List：");
        nested.forEach(list -> System.out.println("   " + list));

        List<Item> flat = orders.stream()
                .flatMap(o -> o.items().stream())
                .collect(Collectors.toList());
        System.out.println("B2 flatMap(o -> o.items().stream()) -> " + flat.size() + " 个元素：" + flat);
        System.out.println("B2 map 是 1 个订单换 1 个 List；flatMap 是 1 个订单换 N 个元素再**合并成一条流**");

        long total = orders.stream()
                .flatMap(o -> o.items().stream())
                .mapToInt(Item::price)
                .sum();
        System.out.println("B3 订单总额（flatMap + mapToInt + sum）= " + total
                + "，这里必须用 flatMap：直接 map 拿到的是 List，sum 无从下手");

        String letters = Stream.of("ab", "cd")
                .flatMap(w -> w.chars().mapToObj(ch -> (char) ch))
                .map(String::valueOf)
                .collect(Collectors.joining("-"));
        System.out.println("B4 拆字符串：Stream.of(\"ab\",\"cd\").flatMap(w -> w.chars()...) -> " + letters);
        System.out.println("B4 flatMap 的入参必须返回 Stream：chars() 返回 IntStream，"
                + "要 mapToObj 装箱成 Character 才能拼" );

        List<Optional<String>> maybeEmails = List.of(
                Optional.of("a@x.com"), Optional.empty(), Optional.of("c@x.com"));
        List<String> emails = maybeEmails.stream()
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        System.out.println("B5 List<Optional<String>>.flatMap(Optional::stream) -> " + emails);
        System.out.println("B5 这是 Day 3「Optional 不要塞进集合」那条的补救手段："
                + "真拿到了，flatMap(Optional::stream) 一步展平，比 filter(isPresent).map(get) 干净");
        System.out.println();
    }

    // ================================================================== C
    private void sectionC() {
        System.out.println("========== C · parallel：省了代码，别顺手把顺序也省了 ==========");

        List<Integer> seq = IntStream.range(0, 12).boxed().collect(Collectors.toList());
        List<String> a = Collections.synchronizedList(new ArrayList<>());
        List<String> b = Collections.synchronizedList(new ArrayList<>());
        List<String> c = Collections.synchronizedList(new ArrayList<>());

        seq.stream().forEach(i -> a.add(String.valueOf(i)));
        seq.parallelStream().forEach(i -> b.add(String.valueOf(i)));
        seq.parallelStream().forEachOrdered(i -> c.add(String.valueOf(i)));

        System.out.println("C1 串行 forEach        -> " + a);
        System.out.println("C2 parallel forEach    -> " + b);
        System.out.println("C3 parallel forEachOrdered -> " + c);
        System.out.println("C2 顺序不保证（多线程抢同一个 list，还得自己加同步）；C3 保序，但保序本身要付出汇合等待");

        int n = 20_000_000;
        long[] data = new long[n];
        for (int i = 0; i < n; i++) {
            data[i] = i;
        }
        long seqMs = bestOf(3, () -> java.util.Arrays.stream(data).sum());
        long parMs = bestOf(3, () -> java.util.Arrays.stream(data).parallel().sum());
        System.out.println("C4 2000 万个数求和（每元素几乎不做事）：串行 " + seqMs + " ms；parallel " + parMs + " ms"
                + "，本机 " + Runtime.getRuntime().availableProcessors() + " 核");

        long[] small = new long[1_000];
        for (int i = 0; i < small.length; i++) {
            small[i] = i;
        }
        long smallSeq = bestOfMicros(200, () -> java.util.Arrays.stream(small).map(StreamOnce::heavy).sum());
        long smallPar = bestOfMicros(200, () -> java.util.Arrays.stream(small).parallel()
                .map(StreamOnce::heavy).sum());
        System.out.println("C5 1000 个元素、每元素 30 轮计算：串行 " + smallSeq + " µs；parallel " + smallPar + " µs");
        System.out.println("C5 小数据量下 parallel **反而慢 " + String.format("%.1f", smallPar * 1.0 / Math.max(smallSeq, 1))
                + " 倍** —— 拆任务 + 合并结果的开销比干活本身还大");

        long[] big = new long[2_000_000];
        for (int i = 0; i < big.length; i++) {
            big[i] = i;
        }
        long bigSeq = bestOf(3, () -> java.util.Arrays.stream(big).map(StreamOnce::heavy).sum());
        long bigPar = bestOf(3, () -> java.util.Arrays.stream(big).parallel().map(StreamOnce::heavy).sum());
        System.out.println("C6 200 万个元素 × 每元素 30 轮计算：串行 " + bigSeq + " ms；parallel " + bigPar + " ms"
                + "（同样的活，前者单核跑满，后者摊到 12 核）");
        System.out.println("C6 所以 parallel 不是「更快」，是「用核数换单次耗时」，代价是 CPU 占用和可读性");
        System.out.println("C6 判断标准：数据量 ≥ 1 万、每元素计算够重、无共享可变状态、不依赖顺序 —— "
                + "四条都满足才考虑 parallel；任一条不满足，for 循环更划算");
        System.out.println();
    }

    // ================================================================== D
    private static void sectionD() {
        System.out.println("========== D · 什么时候不该用 Stream ==========");
        System.out.println("D1 需要 break / continue / return 中途退出 -> 用 for。Stream 没有 break，"
                + "硬写 takeWhile 只在有序且可判定的场景成立");
        System.out.println("D2 要修改外层局部变量 -> 用 for。lambda 只能读 effectively final 的副本（Day 4 结论）");
        System.out.println("D3 需要下标 / 相邻元素比较（i 与 i-1）-> 用 for，Stream 没有索引概念");
        System.out.println("D4 链超过 5 步、还要 try-catch -> 拆成几个有名字的方法，别写成 20 行不换行");
        System.out.println("D5 反过来，下面这些用 Stream 明显更好：过滤+映射+收集、分组统计、"
                + "「同一个集合连着做三件事」这类一次性遍历");
        System.out.println();
    }

    // ---------------------------------------------------------------- 工具
    private Stream<Order> vipOrders() {
        return orders.stream().filter(Order::vip);
    }

    @FunctionalInterface
    private interface Task {
        long run();
    }

    private static long bestOf(int rounds, Task task) {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < rounds; i++) {
            long start = System.nanoTime();
            task.run();
            long cost = (System.nanoTime() - start) / 1_000_000;
            best = Math.min(best, cost);
        }
        return best;
    }

    private static long bestOfMicros(int rounds, Task task) {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < rounds; i++) {
            long start = System.nanoTime();
            task.run();
            long cost = (System.nanoTime() - start) / 1_000;
            best = Math.min(best, cost);
        }
        return best;
    }

    /** 故意做成「不能一眼算出答案」的重活，否则 JIT 会把整个循环消掉。 */
    private static long heavy(long v) {
        long acc = v;
        for (int i = 0; i < 30; i++) {
            acc = acc * 31 + (acc >>> 3);
        }
        return acc;
    }

    // ---------------------------------------------------------------- 模型
    record Item(String name, int price) {
        @Override
        public String toString() {
            return name + "(" + price + ")";
        }
    }

    record Order(String id, String customer, boolean vip, List<Item> items) {
        int amount() {
            return items.stream().mapToInt(Item::price).sum();
        }
    }
}
