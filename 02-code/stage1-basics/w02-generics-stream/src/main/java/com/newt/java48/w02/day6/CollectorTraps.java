package com.newt.java48.w02.day6;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Day 6 · 收集器的六个陷阱（D 段）—— 每条都现场撞一次异常，不靠背
 *
 * <pre>
 * D1 toMap 的重复 key：IllegalStateException，附「合并函数」两种修法
 * D2 toMap 的 value 为 null：NPE，而且 HashMap 本身是允许 null value 的
 * D3 groupingBy 的 key 为 null：NPE，附原文
 * D4 teeing：一次遍历算两个聚合（求平均不用跑两遍流）
 * D5 Collectors.toList() / Stream.toList() / toUnmodifiableList 的可变性
 * D6 一对多别用 toMap 硬扛，groupingBy 才是正解
 * </pre>
 *
 * <p>运行：{@code run.bat day6.CollectorTraps}
 */
public class CollectorTraps {

    private final List<User> users = UserData.users();

    public static void main(String[] args) {
        new CollectorTraps().run();
    }

    private void run() {
        d1();
        d2();
        d3();
        d4();
        d5();
        d6();
        System.out.println("========== D 收尾 ==========");
        System.out.println("这六条的共同点：**编译期一句都不报，全靠运行时**。");
        System.out.println("toMap 用之前先问自己两句：key 会不会重复？value 会不会是 null？");
        System.out.println();
    }

    // ---------------------------------------------------------------- D1
    private void d1() {
        System.out.println("========== D1 · toMap 的重复 key ==========");

        // 先来一个正常的：id 唯一
        Map<String, String> idToName = users.stream()
                .collect(Collectors.toMap(User::id, u -> u.name() == null ? "<null>" : u.name()));
        System.out.println("D1 id 唯一时正常 -> " + idToName.size() + " 条，取 U01 = " + idToName.get("U01"));

        // 换成 dept 做 key：5 个研发挤同一个 key
        System.out.println("D1 换成以部门为 key（研发 5 人）：");
        try {
            Map<String, Integer> map = users.stream()
                    .filter(u -> u.dept() != null)
                    .collect(Collectors.toMap(User::dept, User::salary));
            System.out.println("D1   -> " + map + "（不该走到这里）");
        } catch (IllegalStateException e) {
            System.out.println("D1   -> " + e.getClass().getName() + ": " + e.getMessage());
            System.out.println("D1   注意它报在**收流那一刻**，栈里看不出是哪两条数据撞的 —— "
                    + "这是 toMap 最难排查的地方");
        }

        // 修法一：保留先出现的
        Map<String, Integer> keepFirst = users.stream()
                .filter(u -> u.dept() != null)
                .collect(Collectors.toMap(User::dept, User::salary, (a, b) -> a));
        System.out.println("D1 修法一 (a, b) -> a 保留先出现的 -> " + keepFirst);

        // 修法二：按业务规则合并（取最高薪资）
        Map<String, Integer> keepMax = users.stream()
                .filter(u -> u.dept() != null)
                .collect(Collectors.toMap(User::dept, User::salary, Math::max));
        System.out.println("D1 修法二 Math::max 取最高 -> " + keepMax);
        System.out.println("D1 两个修法的区别不是「哪个对」，而是「业务要哪个」—— "
                + "写 (a,b)->a 之前一定要能说出「为什么丢掉 b 是可以的」");
        System.out.println();
    }

    // ---------------------------------------------------------------- D2
    private void d2() {
        System.out.println("========== D2 · toMap 的 value 为 null ==========");

        // 先证明容器本身允许 null value
        Map<String, Integer> plain = new HashMap<>();
        plain.put("何静", null);
        System.out.println("D2 HashMap.put 一个 null value -> " + plain + "，没抛异常，size=" + plain.size());
        System.out.println("D2 所以「HashMap 不能存 null」是错的 —— 它允许一个 null key + 任意多个 null value");

        System.out.println("D2 但 toMap 的 value 为 null（何静 / 徐丽 的 age 是 null）：");
        try {
            Map<String, Integer> m = users.stream()
                    .collect(Collectors.toMap(User::id, User::age));
            System.out.println("D2   -> " + m + "（不该走到这里）");
        } catch (NullPointerException e) {
            System.out.println("D2   -> " + e.getClass().getName()
                    + "，message = " + (e.getMessage() == null ? "<空>" : e.getMessage()));
            System.out.println("D2   连一句解释都没有，栈顶还落在 Collectors 内部，只能靠「我传了 null value」自己想到");
        }

        System.out.println("D2 原因：toMap 内部走的是 map.merge(key, value, mergeFunction)，"
                + "而 HashMap.merge 第一句就是 if (value == null || remappingFunction == null) throw new NullPointerException()");
        System.out.println("D2 换句话说：toMap 用的是 merge，不是 put；put 的宽松规则在 merge 这儿被收紧了");

        Map<String, String> fixed = users.stream()
                .collect(Collectors.toMap(User::id, u -> String.valueOf(u.age())));
        System.out.println("D2 修法一：value 侧换成 String.valueOf(age)（null 变字面量 \"null\"）-> "
                + fixed.size() + " 条");
        System.out.println("D2 修法二：提前 filter(u -> u.age() != null)，代价是「没年龄的人」整条消失");
        System.out.println("D2 选哪个取决于语义：是「这个人不存在」还是「这个人存在但年龄未知」");
        System.out.println();
    }

    // ---------------------------------------------------------------- D3
    private void d3() {
        System.out.println("========== D3 · groupingBy 的 key 为 null ==========");
        System.out.println("D3 全量 15 条里，U99 的 dept 是 null：");
        try {
            Map<String, List<User>> m = users.stream()
                    .collect(Collectors.groupingBy(User::dept));
            System.out.println("D3   -> " + m.size() + " 组（不该走到这里）");
        } catch (NullPointerException e) {
            System.out.println("D3   -> " + e.getClass().getName() + ": " + e.getMessage());
        }
        System.out.println("D3 groupingBy 内部对 classifier 的返回值做了 Objects.requireNonNull，"
                + "所以它连一个「null 组」都不给你");
        System.out.println("D3 三种应对：① filter 掉；② classifier 里映射成 \"未分配\"；"
                + "③ 换成 groupingBy(u -> Optional.ofNullable(u.dept()), ...) —— 但这会把 Optional 塞进 Map 的 key 里，"
                + "而 Optional 的 equals 是值语义，能当 key，只是可读性很差，不推荐");
        System.out.println("D3 对比 D2：同一个「按 key 聚合」，两者的 null 政策居然不一样 —— "
                + "先看 toMap 拿 dept 当 key（含那条 null）：");
        try {
            Map<String, Integer> withNullKey = users.stream()
                    .filter(u -> "钱多".equals(u.name()))
                    .collect(Collectors.toMap(User::dept, User::salary));
            System.out.println("D3   toMap -> " + withNullKey + "，没抛异常，null 成了一个正经的 key");
        } catch (NullPointerException e) {
            System.out.println("D3   toMap -> " + e.getClass().getName() + ": " + e.getMessage());
        }
        System.out.println("D3 因为 toMap 走 HashMap.merge，而 HashMap 允许一个 null key；"
                + "groupingBy 则用 Objects.requireNonNull 把 null 挡在门外");
        System.out.println();
    }

    // ---------------------------------------------------------------- D4
    private void d4() {
        System.out.println("========== D4 · teeing：一次遍历算两个聚合 ==========");

        // 老办法：跑两遍流
        long totalSalary = users.stream().mapToLong(User::salary).sum();
        long count = users.stream().count();
        System.out.println("D4 两遍流 -> 总额 " + totalSalary + "，人数 " + count
                + "，人均 " + String.format("%.1f", totalSalary * 1.0 / count));

        // teeing：一遍流拿到两个结果再合并
        String avgOnePass = users.stream()
                .collect(Collectors.teeing(
                        Collectors.summingInt(User::salary),
                        Collectors.counting(),
                        (sum, n) -> String.format("%.1f", sum * 1.0 / n)));
        System.out.println("D4 teeing 一遍流 -> 人均 " + avgOnePass);
        System.out.println("D4 teeing(downstream1, downstream2, merger)：两个下游并行喂同一个元素，"
                + "最后 merger 把两个结果合成一个值");

        // 更有用的形态：一次算出「总额 + 人数 + 最高薪」打包成对象
        record Payroll(long total, long headcount, int max) {
        }
        Payroll payroll = users.stream()
                .collect(Collectors.teeing(
                        Collectors.summingInt(User::salary),
                        Collectors.teeing(
                                Collectors.counting(),
                                Collectors.mapping(User::salary, Collectors.maxBy(Integer::compareTo)),
                                (n, max) -> new long[]{n, max.orElse(0)}),
                        (sum, arr) -> new Payroll(sum, arr[0], (int) arr[1])));
        System.out.println("D4 嵌套 teeing 一次拿三个指标 -> 总额 " + payroll.total()
                + "，人数 " + payroll.headcount() + "，最高 " + payroll.max());

        // 按部门求人均：分组 + teeing 组合
        Map<String, String> avgByDept = users.stream()
                .filter(u -> u.dept() != null)
                .collect(Collectors.groupingBy(User::dept,
                        Collectors.teeing(
                                Collectors.summingInt(User::salary),
                                Collectors.counting(),
                                (sum, n) -> String.format("%.0f", sum * 1.0 / n))));
        System.out.println("D4 groupingBy + teeing（每组人均薪资）-> " + avgByDept);
        System.out.println("D4 teeing 是 JDK 12 新增，JDK 8 项目里只能用 collectingAndThen 套一个自定义累加器，"
                + "或者老老实实跑两遍 —— 面试问「怎么一遍流求平均」就从这儿答");
        System.out.println();
    }

    // ---------------------------------------------------------------- D5
    private void d5() {
        System.out.println("========== D5 · 三种 toList 的可变性 ==========");

        List<String> fromCollector = users.stream().map(User::id).collect(Collectors.toList());
        List<String> fromStream = users.stream().map(User::id).toList();
        List<String> unmodifiable = users.stream().map(User::id).collect(Collectors.toUnmodifiableList());

        System.out.println("D5 Collectors.toList() -> " + fromCollector.getClass().getName());
        System.out.println("D5 Stream.toList()（JDK 16+）-> " + fromStream.getClass().getName());
        System.out.println("D5 Collectors.toUnmodifiableList() -> " + unmodifiable.getClass().getName());

        System.out.println("D5 依次尝试 add(\"X\")：");
        System.out.println("D5   Collectors.toList()        -> " + tryAdd(fromCollector) + "，size=" + fromCollector.size());
        System.out.println("D5   Stream.toList()            -> " + tryAdd(fromStream));
        System.out.println("D5   toUnmodifiableList()       -> " + tryAdd(unmodifiable));

        System.out.println("D5 还有第三个差异：允许 null 元素吗（用一条含 null 的流去撞，而不是往结果里 add）");
        System.out.println("D5   Collectors.toList()        -> " + collectWithNull(false));
        System.out.println("D5   Stream.toList()            -> " + streamToListWithNull());
        System.out.println("D5   Collectors.toUnmodifiableList() -> " + collectWithNull(true));
        System.out.println("D5   注意这里三选二不是一致的：Stream.toList() 和 toUnmodifiableList() "
                + "返回同一族不可变实现（ImmutableCollections$ListN），但前者**放行 null**、后者**拒绝 null**");
        System.out.println("D5   原因在实现路径：Stream.toList() 走 listFromTrustedArrayNullsAllowed，"
                + "toUnmodifiableList() 走 listFromTrustedArray —— 后者逐个 Objects.requireNonNull");
        System.out.println("D5   而 List.of(\"a\", null) 是 NPE：那是「多个固定实参」的重载，跟单传一个数组的行为不一样");

        System.out.println("D5 结论：Collectors.toList() 只承诺「是个 List」，实现类和可变性都没承诺；"
                + "JDK 16 的 Stream.toList() 明确不可变，但**不拒绝 null 元素**（这条最容易记反）");
        System.out.println("D5 项目里混用过的典型事故：方法返回 Stream.toList() 的结果，"
                + "调用方一句 .add() / .sort() 就 UnsupportedOperationException —— 而且**只在线上数据走到那行时**才炸");
        System.out.println();
    }

    // ---------------------------------------------------------------- D6
    private void d6() {
        System.out.println("========== D6 · 一对多：toMap 硬扛 vs groupingBy ==========");

        // 用 toMap + merging 把同部门的人攒成 List
        Map<String, List<String>> viaToMap = users.stream()
                .filter(u -> u.dept() != null)
                .collect(Collectors.toMap(
                        User::dept,
                        u -> new ArrayList<>(List.of(u.brief())),
                        (a, b) -> {
                            a.addAll(b);
                            return a;
                        }));
        System.out.println("D6 toMap + 合并函数 -> " + viaToMap);

        Map<String, List<String>> viaGrouping = users.stream()
                .filter(u -> u.dept() != null)
                .collect(Collectors.groupingBy(User::dept,
                        Collectors.mapping(User::brief, Collectors.toList())));
        System.out.println("D6 groupingBy + mapping -> " + viaGrouping);
        System.out.println("D6 结果一样，但 toMap 那版你得自己 new ArrayList、自己 addAll、"
                + "还得担心合并顺序；groupingBy 一行就说完「按部门把 brief 收成 List」");
        System.out.println("D6 判断口诀：key 唯一 -> toMap；key 会重复 -> groupingBy。"
                + "用 toMap 处理重复 key，等于用手动合并函数重新实现一遍 groupingBy");
        System.out.println();
    }

    // ---------------------------------------------------------------- 工具
    private static String tryAdd(List<String> list) {
        try {
            list.add("X");
            return "成功（可变）";
        } catch (UnsupportedOperationException e) {
            return "UnsupportedOperationException";
        }
    }

    /** 含 null 的流交给 Collectors.toList() / toUnmodifiableList()：一个容忍、一个拒绝。 */
    private static String collectWithNull(boolean unmodifiable) {
        try {
            List<String> list = unmodifiable
                    ? java.util.stream.Stream.of("a", null).collect(Collectors.toUnmodifiableList())
                    : java.util.stream.Stream.of("a", null).collect(Collectors.toList());
            return "允许（size=" + list.size() + "，实现 " + list.getClass().getSimpleName() + "）";
        } catch (NullPointerException e) {
            return "NullPointerException（拒绝 null 元素）";
        }
    }

    /** 含 null 的流交给 Stream.toList()：直接 NPE。 */
    private static String streamToListWithNull() {
        try {
            List<String> list = java.util.stream.Stream.of("a", null).toList();
            return "允许（size=" + list.size() + "，实现 " + list.getClass().getSimpleName() + "）";
        } catch (NullPointerException e) {
            return "NullPointerException（" + e.getMessage() + "）";
        }
    }
}
