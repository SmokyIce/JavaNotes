package com.newt.java48.w02.day6;

import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Day 6 · 分组统计（C 段）—— 业务里最能把一大坨 for 循环删掉的一块
 *
 * <pre>
 * C1 groupingBy 一参：key -> List
 * C2 两参 + 下游收集器：counting / summingInt / averagingInt / mapping / maxBy / toSet
 * C3 三参：mapFactory 决定 key 的顺序（HashMap / LinkedHashMap / TreeMap）
 * C4 多级分组：部门 -> 城市 -> 人数
 * C5 partitioningBy：空组会不会消失（和 groupingBy 的关键差别）
 * C6 summarizingInt：count/sum/min/max/avg 一把抓
 * </pre>
 *
 * <p>运行：{@code run.bat day6.GroupStats}
 */
public class GroupStats {

    /**
     * 工作集：先把 dept 为 null 的那条摘掉。
     * 不是「绕过问题」，是因为 groupingBy 的 classifier 一旦返回 null 就直接 NPE，
     * 这条陷阱单独在 {@link CollectorTraps} 的 D3 段演示。
     */
    private final List<User> staff = UserData.users().stream()
            .filter(u -> u.dept() != null)
            .toList();

    public static void main(String[] args) {
        new GroupStats().run();
    }

    private void run() {
        sectionC1();
        sectionC2();
        sectionC3();
        sectionC4();
        sectionC5();
        sectionC6();
        System.out.println("========== C 收尾 ==========");
        System.out.println("groupingBy 的三层重载其实是三个问题：怎么分组 / 用什么 Map / 每组算出什么。");
        System.out.println("业务里 90% 的「按 X 统计 Y」都能落在这三格的组合里。");
        System.out.println();
    }

    // ---------------------------------------------------------------- C1
    private void sectionC1() {
        System.out.println("========== C1 · groupingBy 一参：key -> List ==========");
        Map<String, List<User>> byDept = staff.stream()
                .collect(Collectors.groupingBy(User::dept));

        System.out.println("C1 分组结果：" + byDept.size() + " 个部门");
        byDept.forEach((dept, list) ->
                System.out.println("   " + dept + " (" + list.size() + " 人) -> " + brief(list)));
        System.out.println("C1 keySet 顺序 = " + byDept.keySet());
        System.out.println("C1 这个顺序来自 HashMap 的桶分布，**不是插入顺序、也不是字母序**；"
                + "换个 JDK 版本或数据量变化就可能变，别在业务里依赖它");
        System.out.println();
    }

    // ---------------------------------------------------------------- C2
    private void sectionC2() {
        System.out.println("========== C2 · 两参：classifier + 下游收集器 ==========");

        // C2a 计数
        Map<String, Long> countByDept = staff.stream()
                .collect(Collectors.groupingBy(User::dept, Collectors.counting()));
        System.out.println("C2a counting() -> " + countByDept);

        // C2b 求和
        Map<String, Integer> sumByDept = staff.stream()
                .collect(Collectors.groupingBy(User::dept, Collectors.summingInt(User::salary)));
        System.out.println("C2b summingInt(salary) -> " + sumByDept);

        // C2c 平均年龄：直接 averagingInt(User::age) 会炸
        System.out.println("C2c averagingInt(User::age) 直接上：");
        try {
            Map<String, Double> avg = staff.stream()
                    .collect(Collectors.groupingBy(User::dept, Collectors.averagingInt(User::age)));
            System.out.println("C2c   -> " + avg + "（不该走到这里）");
        } catch (NullPointerException e) {
            System.out.println("C2c   -> " + e.getClass().getName()
                    + "，message = " + (e.getMessage() == null ? "<空>" : e.getMessage()));
            System.out.println("C2c   Integer 是包装类型，ToIntFunction 要拆箱，null 拆不了");
            System.out.println("C2c   连 message 都没有，是因为 User::age 是**方法引用**："
                    + "拆箱发生在 lambda metafactory 生成的 MethodHandle 适配层，没有字节码位置可指");
        }
        // 对照：一模一样的拆箱，写成 lambda 就有 message
        try {
            staff.stream().collect(Collectors.groupingBy(User::dept,
                    Collectors.averagingInt(u -> u.age())));
        } catch (NullPointerException e) {
            System.out.println("C2c   对照：写成 lambda averagingInt(u -> u.age()) -> message = " + e.getMessage());
            System.out.println("C2c   同样是拆箱 NPE，lambda 版有 javac 插入的 intValue() 调用点，"
                    + "HotSpot 能甩出 helpful message；方法引用版什么都没有");
        }
        // 错误修法：把 null 当 0
        Map<String, Double> avgWrongFix = staff.stream()
                .collect(Collectors.groupingBy(User::dept,
                        Collectors.averagingInt(u -> u.age() == null ? 0 : u.age())));
        // 正确修法：先过滤掉没有 age 的人，再分组
        Map<String, Double> avgRightFix = staff.stream()
                .filter(u -> u.age() != null)
                .collect(Collectors.groupingBy(User::dept,
                        Collectors.averagingInt(User::age)));
        System.out.println("C2c 错误修法（null 当 0）-> " + avgWrongFix);
        System.out.println("C2c 正确修法（先滤掉 null 再算）-> " + avgRightFix);
        System.out.println("C2c 差别：研发组 4 个人有年龄，把 null 当 0 之后分母变 5，均值被拉低 "
                + String.format("%.1f", Math.abs(avgWrongFix.get("研发") - avgRightFix.get("研发")))
                + " 岁（25.6 vs 32.0）—— 「补个默认值」在统计场景里是错的，它改变了分母");

        // C2d mapping + joining：每组拼成一行
        Map<String, String> namesByDept = staff.stream()
                .collect(Collectors.groupingBy(User::dept,
                        Collectors.mapping(User::name, Collectors.joining("/"))));
        System.out.println("C2d mapping(name, joining(\"/\")) -> " + namesByDept);
        System.out.println("C2d 测试组里那个 null name 变成了字面量 \"null\"：joining 的 add(null) 不抛错，"
                + "它会原样拼进去 —— 要干净就得自己 map 成 \"\" 或提前过滤");

        // C2e maxBy：每组薪资最高的人
        Map<String, Optional<User>> topByDept = staff.stream()
                .collect(Collectors.groupingBy(User::dept,
                        Collectors.maxBy(Comparator.comparingInt(User::salary))));
        System.out.println("C2e maxBy(comparingInt(salary)) -> ");
        topByDept.forEach((dept, u) -> System.out.println("   " + dept + " -> "
                + u.map(User::brief).orElse("-")));
        System.out.println("C2e 值是 Optional<User>：分组一定非空（空组压根不会出现在 Map 里），"
                + "所以这里的 Optional 理论上永远有值；嫌它烦就套 collectingAndThen 拆掉");

        Map<String, String> topNameByDept = staff.stream()
                .collect(Collectors.groupingBy(User::dept,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(User::salary)),
                                o -> o.map(User::brief).orElse("-"))));
        System.out.println("C2e collectingAndThen 拆掉 Optional -> " + topNameByDept);

        // C2f mapping + toSet：每组覆盖了哪些城市
        Map<String, Set<String>> citiesByDept = staff.stream()
                .collect(Collectors.groupingBy(User::dept,
                        Collectors.mapping(User::city, Collectors.toSet())));
        System.out.println("C2f mapping(city, toSet()) -> " + citiesByDept);
        System.out.println("C2f 同一个下游思路换 toSet / toMap / joining，就是「每组去重」「每组转字典」「每组拼字符串」");
        System.out.println();
    }

    // ---------------------------------------------------------------- C3
    private void sectionC3() {
        System.out.println("========== C3 · 三参：mapFactory 决定 key 的顺序 ==========");

        Function<User, String> key = User::dept;
        Map<String, Long> hashMap = staff.stream()
                .collect(Collectors.groupingBy(key, Collectors.counting()));
        Map<String, Long> linked = staff.stream()
                .collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> tree = staff.stream()
                .collect(Collectors.groupingBy(key, TreeMap::new, Collectors.counting()));

        System.out.println("C3 HashMap（默认）      -> " + hashMap.keySet());
        System.out.println("C3 LinkedHashMap        -> " + linked.keySet() + "（按首次出现顺序）");
        System.out.println("C3 TreeMap              -> " + tree.keySet() + "（按 key 自然序）");
        System.out.println("C3 结论：要给前端「部门顺序稳定」的列表，就把第三个参数写成 TreeMap::new 或 LinkedHashMap::new；"
                + "默认的 HashMap 顺序不可依赖");
        System.out.println("C3 注意三参的签名是 (classifier, mapFactory, downstream) —— "
                + "mapFactory 夹在中间，这是最容易记错位置的一个重载");
        System.out.println();
    }

    // ---------------------------------------------------------------- C4
    private void sectionC4() {
        System.out.println("========== C4 · 多级分组：部门 -> 城市 -> 人数 ==========");
        Map<String, Map<String, Long>> nested = staff.stream()
                .collect(Collectors.groupingBy(User::dept,
                        Collectors.groupingBy(User::city, Collectors.counting())));
        nested.forEach((dept, m) -> System.out.println("   " + dept + " -> " + m));
        System.out.println("C4 多级就是下游再放一个 groupingBy，Map 的嵌套层数 = groupingBy 的嵌套层数；"
                + "超过三层基本该考虑直接写 SQL 的 group by 了");
        System.out.println();
    }

    // ---------------------------------------------------------------- C5
    private void sectionC5() {
        System.out.println("========== C5 · partitioningBy：空组不会消失 ==========");

        // 产品组 3 个人全在职 —— 正好用来造「false 组为空」的场景
        List<User> product = staff.stream().filter(u -> "产品".equals(u.dept())).toList();
        Map<Boolean, List<User>> part = product.stream()
                .collect(Collectors.partitioningBy(User::active));
        Map<Boolean, List<User>> grouped = product.stream()
                .collect(Collectors.groupingBy(User::active));

        System.out.println("C5 产品组 " + product.size() + " 人，全部在职");
        System.out.println("C5 partitioningBy(active) -> keySet=" + part.keySet()
                + "，false 组 = " + brief(part.get(false)));
        System.out.println("C5 groupingBy(active)     -> keySet=" + grouped.keySet() + "，false 这个 key 根本不存在");
        System.out.println("C5 所以取 false 组时：partitioningBy 给你空 List（可以直接 size()），"
                + "groupingBy 给你 null（NPE 预备役）");
        System.out.println("C5 判断标准：条件是布尔语义、且「不满足」的那组也要出现在报表里 -> 用 partitioningBy");

        Map<Boolean, Long> partCount = staff.stream()
                .collect(Collectors.partitioningBy(User::active, Collectors.counting()));
        System.out.println("C5 partitioningBy 也支持下游 -> " + partCount + "（两个 key 一定都在）");
        System.out.println();
    }

    // ---------------------------------------------------------------- C6
    private void sectionC6() {
        System.out.println("========== C6 · summarizingInt：五个指标一次遍历 ==========");
        Map<String, IntSummaryStatistics> stats = staff.stream()
                .collect(Collectors.groupingBy(User::dept, Collectors.summarizingInt(User::salary)));
        stats.forEach((dept, s) -> System.out.println("   " + dept + " -> " + s));
        System.out.println("C6 toString 的顺序是 count / sum / min / average / max，"
                + "一个收集器顶 count + sum + min + max + average 五个");
        System.out.println("C6 它内部只遍历一遍：如果分成五次 count()/sum()/max()，就是五倍的工作量");

        IntSummaryStatistics all = staff.stream().collect(Collectors.summarizingInt(User::salary));
        System.out.println("C6 全公司 -> " + all);
        System.out.println("C6 average 是「总和 / count」，返回 double；要四舍五入得自己 format，"
                + "或者套 collectingAndThen(s -> Math.round(s.getAverage()), ...)");
        System.out.println();
    }

    // ---------------------------------------------------------------- 工具
    private static String brief(List<User> list) {
        return list.stream().map(User::brief).collect(Collectors.joining(", "));
    }
}
