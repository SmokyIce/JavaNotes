package com.newt.java48.w02.day5;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Day 5 · Stream（一）：惰性求值、短路、以及「中间操作只是记账」
 *
 * <p>「中间操作返回新流、终止操作才执行」这句话人人会背，但线上排查时真正有用的是
 * 另一件事：**哪一行在什么时候跑**。这一节用打印顺序把它钉死。
 *
 * <pre>
 * A  惰性求值：不接终止操作一次都不跑；接上之后是「一个元素走完全链」
 * B  短路：anyMatch / findFirst / limit 在无限流上也能停；count 停不下来
 * C  屏障：sorted / distinct 吃掉全部元素作废短路 —— 但 rangeClosed 自带 SORTED 特征，sorted() 会穿透
 * D  peek 的两个坑：count() 会把它优化掉；parallel 下顺序不保证
 * </pre>
 *
 * <p>运行：{@code run.bat day5.StreamLazy}
 */
public class StreamLazy {

    /** 记录「链路里到底什么时候执行了哪一步」，比行号可靠。 */
    private static final List<String> LOG = new ArrayList<>();

    /** 记录源里被真正取出的元素个数。 */
    private static long probes;

    public static void main(String[] args) {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        System.out.println("========== 收尾 ==========");
        System.out.println("Stream 调试只有一个抓手：判断这一步是**中间**还是**终止**。");
        System.out.println("中间操作没执行 -> 说明链尾缺终止操作，或者上游有屏障/短路提前收工。");
        System.out.println("反过来，看到「明明写了 filter 却没生效」，先数一数链尾到底有没有终止操作。");
    }

    // ================================================================== A
    private static void sectionA() {
        System.out.println("========== A · 中间操作只记账，终止操作才真跑 ==========");

        LOG.clear();
        Stream<String> pipeline = Stream.of("a", "b", "c")
                .filter(x -> {
                    LOG.add("filter " + x);
                    return true;
                })
                .map(x -> {
                    LOG.add("map " + x);
                    return x.toUpperCase();
                });

        System.out.println("A1 只构造链路、没接终止操作 -> 日志条数 = " + LOG.size() + "，内容 = " + LOG);
        System.out.println("A1 filter / map 那一行一次都没执行，它们只是往流水线上挂了个节点");

        List<String> out = pipeline.collect(Collectors.toList());
        System.out.println("A2 接上 collect 之后 -> 结果 " + out);
        System.out.println("A2 日志顺序 = " + LOG);
        System.out.println("A2 是 filter a → map a → filter b → map b，**一个元素走完全链才轮到下一个**");
        System.out.println("A2 不是「先全部 filter 再全部 map」—— 这是 Stream 与「两遍 for 循环」最本质的差别");

        LOG.clear();
        List<String> sorted = Stream.of("c", "a", "b")
                .filter(x -> {
                    LOG.add("filter " + x);
                    return true;
                })
                .sorted()
                .map(x -> {
                    LOG.add("map " + x);
                    return x.toUpperCase();
                })
                .collect(Collectors.toList());
        System.out.println("A3 中间插一个 sorted 之后 -> 结果 " + sorted);
        System.out.println("A3 日志顺序 = " + LOG);
        System.out.println("A3 变成 filter 全跑完才开始 map —— sorted 是**屏障（barrier）**，"
                + "它必须看到全部元素才能排序");
        System.out.println("A3 所以「Stream 一定是流式的」也不对：有状态操作会把管道切成几段，段内才是流式");
        System.out.println();
    }

    // ================================================================== B
    private static void sectionB() {
        System.out.println("========== B · 短路：无限流也能停下来 ==========");

        probes = 0;
        boolean hit = IntStream.rangeClosed(1, 1_000_000)
                .peek(i -> probes++)
                .anyMatch(i -> i == 3);
        System.out.println("B1 100 万个数里 anyMatch(i == 3) -> " + hit + "，实际只探测了 " + probes + " 个");

        probes = 0;
        OptionalInt first = IntStream.iterate(1, i -> i + 1)
                .peek(i -> probes++)
                .filter(i -> i % 7 == 0)
                .findFirst();
        System.out.println("B2 **无限流** iterate(1, i -> i + 1).filter(i % 7 == 0).findFirst() -> "
                + first.getAsInt() + "，只探测了 " + probes + " 个");
        System.out.println("B2 无限流能停下来，靠的就是 findFirst 是**短路**终止操作；"
                + "换成 count() 会跑到内存耗尽");

        probes = 0;
        List<Integer> top3 = IntStream.iterate(1, i -> i + 1)
                .peek(i -> probes++)
                .limit(3)
                .boxed()
                .collect(Collectors.toList());
        System.out.println("B3 iterate(...).limit(3) -> " + top3 + "，探测了 " + probes + " 个");
        System.out.println("B3 limit 是**短路中间操作**：它不参与求值，只是往上游说「够了」");

        probes = 0;
        long cnt = IntStream.rangeClosed(1, 1_000_000)
                .peek(i -> probes++)
                .filter(i -> i % 7 == 0)
                .count();
        System.out.println("B4 反例：同样 100 万个数，count() -> " + cnt + "，探测了 " + probes + " 个");
        System.out.println("B4 count 不短路 —— 它要的是总数。短路的前提是「这个终止操作只要一个答案就够了」");
        System.out.println("B4 短路家族：anyMatch / allMatch / noneMatch / findFirst / findAny / limit");
        System.out.println("B4 非短路：count / sum / average / max / min / collect / forEach");
        System.out.println();
    }

    // ================================================================== C
    private static void sectionC() {
        System.out.println("========== C · 屏障：sorted / distinct 会把短路全部作废 ==========");

        probes = 0;
        OptionalInt f1 = IntStream.rangeClosed(1, 5)
                .peek(i -> probes++)
                .findFirst();
        System.out.println("C1 rangeClosed(1,5).findFirst() -> " + f1.getAsInt() + "，探测 " + probes + " 个");

        probes = 0;
        OptionalInt f2 = IntStream.rangeClosed(1, 5)
                .peek(i -> probes++)
                .sorted()
                .findFirst();
        System.out.println("C2 中间加一个 sorted() 之后 -> " + f2.getAsInt() + "，探测 " + probes + " 个");
        System.out.println("C2 只探测了 1 个！sorted 居然没吃掉全部 —— 因为 rangeClosed 的 spliterator "
                + "**自带 SORTED 特征**（Streams.java:119），SortedOps 看到上游已有序就直接穿透（SortedOps.java:136）");
        System.out.println("C2 换句话说：rangeClosed / of(已排序) 上的 sorted() 是**空操作**，不是屏障");

        probes = 0;
        OptionalInt f3 = IntStream.of(5, 3, 1, 4, 2)
                .peek(i -> probes++)
                .sorted()
                .findFirst();
        System.out.println("C3 换成真的没排序的源 of(5,3,1,4,2).sorted().findFirst() -> " + f3.getAsInt()
                + "，探测 " + probes + " 个");
        System.out.println("C3 这次吃满了 5 个 —— findFirst 本来只要 1 个，屏障毁掉一切短路");
        System.out.println("C3 所以判断「sorted 贵不贵」要先看上游有没有 SORTED 特征，凭印象猜会差 5 倍");

        probes = 0;
        long distinct = IntStream.of(1, 2, 2, 3, 3, 3)
                .peek(i -> probes++)
                .distinct()
                .count();
        System.out.println("C4 distinct() 同理，去重后 " + distinct + " 个，探测 " + probes + " 个"
                + "（去重必须记住见过的所有值）");

        int n = 1_000_000;
        long sortThenLimit = bestOf(3, () -> IntStream.rangeClosed(1, n).boxed()
                .sorted(Comparator.reverseOrder())
                .limit(3)
                .collect(Collectors.toList())
                .size());
        long limitThenSort = bestOf(3, () -> IntStream.rangeClosed(1, n).boxed()
                .limit(3)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList())
                .size());
        System.out.println("C5 100 万条 Integer，sorted→limit(3) = " + sortThenLimit + " ms；"
                + "limit(3)→sorted = " + limitThenSort + " ms（不到 1 ms）");
        System.out.println("C5 差了一个数量级以上，但**两者语义不同**：前者是「全局最大的 3 个」，"
                + "后者是「前 3 个里最大的」，不能随便换位置");
        System.out.println("C5 想要全局 top-K 又不想全排，正解是拿一个大小为 K 的 PriorityQueue 手动收，"
                + "或者先 collect 再 destructure");
        System.out.println();
    }

    // ================================================================== D
    private static void sectionD() {
        System.out.println("========== D · peek 的两个坑 ==========");

        LOG.clear();
        long c1 = Stream.of("a", "b", "c")
                .peek(x -> LOG.add("peek " + x))
                .count();
        System.out.println("D1 Stream.of(a,b,c).peek(log).count() = " + c1
                + "，但 peek 日志条数 = " + LOG.size() + "  内容 = " + LOG);

        LOG.clear();
        long c2 = Stream.of("a", "b", "c")
                .filter(x -> true)
                .peek(x -> LOG.add("peek " + x))
                .count();
        System.out.println("D2 前面加一个 filter 之后，同样 count() -> 日志条数 = " + LOG.size() + " 内容 = " + LOG);

        System.out.println("D1/D2 对比说明：JDK 9+ 的 count() 在「元素个数能直接算出来」时会走快速路径，"
                + "**整条管道包括 peek 一起被跳过**");
        System.out.println("D3 所以 peek 只能用来调试，绝不能放业务逻辑 —— 它会静默不执行，连日志都不留");
        System.out.println("D4 想看真实执行情况，用 forEach 或 collect 这类必须逐元素的终止操作兜底");
        System.out.println();
    }

    // ---------------------------------------------------------------- 工具
    @FunctionalInterface
    private interface Task {
        long run();
    }

    /** 多轮取最小值：单轮测量会被 JIT 分层编译坑（Day 2 实测差过 6 倍）。 */
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
}
