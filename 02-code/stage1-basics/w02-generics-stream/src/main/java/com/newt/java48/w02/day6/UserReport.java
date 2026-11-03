package com.newt.java48.w02.day6;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Day 6 · List&lt;User&gt; 过滤 → 排序（A / B 段）
 *
 * <pre>
 * A  过滤：把 where 从句翻译成 Predicate
 *    A1 单条件       A2 Predicate 拼装（and/or/negate 的优先级）
 *    A3 null 字段的 NPE   A4 短路终止操作
 * B  排序：comparing 只是开头
 *    B1 单级         B2 多级（含 nullsLast）
 *    B3 reversed() 放的位置决定反转几级
 *    B4 age 不做 null 处理会 NPE
 *    B5 稳定性：两次 sort 叠加时的隐藏契约
 * </pre>
 *
 * <p>运行：{@code run.bat day6.UserReport}
 */
public class UserReport {

    private final List<User> users = UserData.users();

    public static void main(String[] args) {
        new UserReport().run();
    }

    private void run() {
        sectionA();
        sectionB();
        System.out.println("========== A/B 收尾 ==========");
        System.out.println("过滤回答「哪些行」，排序回答「什么顺序」。");
        System.out.println("这两件事的难点都不在 API，而在 null：15 条数据里 4 条带 null 字段，"
                + "A/B/C/D 四段各炸一次。");
        System.out.println();
    }

    // ================================================================== A
    private void sectionA() {
        System.out.println("========== A · 过滤 ==========");

        // A1 单条件
        List<User> richActive = users.stream()
                .filter(u -> u.active())
                .filter(u -> u.salary() >= 20000)
                .collect(Collectors.toList());
        System.out.println("A1 在职 且 薪资≥20000 -> " + richActive.size() + " 人：");
        printBrief(richActive);
        System.out.println("A1 两个 filter 串联 == 一个 && ；拆开写的好处是每一步能单独注释、单独复用");

        // A2 Predicate 拼装：业务里「查询条件」经常是运行时拼的
        Predicate<User> isDev = u -> "研发".equals(u.dept());
        Predicate<User> isTest = u -> "测试".equals(u.dept());
        Predicate<User> highPay = u -> u.salary() >= 20000;
        Predicate<User> active = User::active;

        System.out.println("A2 研发 且 高薪 -> " + users.stream().filter(isDev.and(highPay)).count() + " 人");

        long chain = users.stream().filter(isDev.or(isTest).and(active)).count();
        long nested = users.stream().filter(isDev.or(isTest.and(active))).count();
        System.out.println("A2 研发 或 测试，且在职 -> isDev.or(isTest).and(active) = " + chain + " 人");
        System.out.println("A2 同样想表达「研发 或 (测试且在职)」，写 isDev.or(isTest.and(active)) = " + nested + " 人");
        System.out.println("A2   差 " + (nested - chain) + " 人。方法链是**左结合**：先算出 (isDev||isTest) 这个新 Predicate，"
                + "再整体 && active，所以是 (a||b)&&c，不是按布尔优先级想的 a||(b&&c)");
        System.out.println("A2   少的那个是离职的研发王强：按「研发就留下」他该在，按「先并集再筛在职」他不在 —— "
                + "按直觉写就会少人，而且不报错");

        System.out.println("A2 非研发 -> " + users.stream().filter(isDev.negate()).count() + " 人"
                + "（negate 把 null dept 也算进「非研发」，判空逻辑要自己保证）");
        System.out.println("A2 复用价值：这四个 Predicate 可以按前端传参自由组合，"
                + "不用为每种查询写一个方法 —— 但组合顺序必须照着实测结果写，别凭优先级猜");

        // A3 null 字段的 NPE：filter 只过滤元素，过滤不了字段
        System.out.println("A3 想找名字以「张」开头的人，直接写 u.name().startsWith(\"张\")：");
        try {
            List<User> zhang = users.stream()
                    .filter(u -> u.name().startsWith("张"))
                    .collect(Collectors.toList());
            System.out.println("A3   -> " + zhang.size() + " 人（不该走到这里）");
        } catch (NullPointerException e) {
            System.out.println("A3   -> " + e.getClass().getName() + ": " + e.getMessage());
            System.out.println("A3   栈顶会指在 filter 的 lambda 里，但真凶是 U14 的 name=null");
        }

        long nonNullElements = users.stream().filter(Objects::nonNull).count();
        System.out.println("A3 常见误解：加一句 filter(Objects::nonNull) 就能防住 -> 实际过滤后还是 "
                + nonNullElements + " 条（一个都没去掉）");
        System.out.println("A3   因为 Objects::nonNull 判的是**元素本身**为不为 null，判不了元素里的字段");

        List<User> zhangFixed = users.stream()
                .filter(u -> u.name() != null && u.name().startsWith("张"))
                .collect(Collectors.toList());
        System.out.println("A3 修法一（前置判空，最直接）-> " + zhangFixed.size() + " 人：" + brief(zhangFixed));

        List<User> zhangFixed2 = users.stream()
                .filter(u -> Objects.toString(u.name(), "").startsWith("张"))
                .collect(Collectors.toList());
        System.out.println("A3 修法二（Objects.toString 兜底成空串）-> " + zhangFixed2.size() + " 人");
        System.out.println("A3 别用 u.name().equals(\"张\") 这种写法来「常量在前」防 NPE —— "
                + "equals 能这么写，startsWith / contains / length 都不能，字段本身为 null 时一样炸");

        // A4 短路终止操作：只要一个答案时别 collect 完再 get(0)
        Optional<User> firstDev = users.stream()
                .filter(isDev)
                .findFirst();
        System.out.println("A4 findFirst 第一个研发 -> " + firstDev.map(User::brief).orElse("-"));

        boolean anyOver40 = users.stream().anyMatch(u -> u.age() != null && u.age() > 40);
        boolean allHaveDept = users.stream().allMatch(u -> u.dept() != null);
        boolean noneNegative = users.stream().noneMatch(u -> u.salary() < 0);
        System.out.println("A4 anyMatch(有 40 岁以上) -> " + anyOver40
                + "；allMatch(都有部门) -> " + allHaveDept
                + "；noneMatch(没有负薪资) -> " + noneNegative);
        System.out.println("A4 三个 match + findFirst 都是短路的：拿到答案就停，不会遍历完 15 条；"
                + "collect(toList) 则必须走完 —— 「只想知道有没有」时别用 collect");
        System.out.println();
    }

    // ================================================================== B
    private void sectionB() {
        System.out.println("========== B · 排序 ==========");

        // B1 单级：薪资降序。salary 是 int，不会 null，所以这级最安全
        List<User> bySalaryDesc = new ArrayList<>(users);
        bySalaryDesc.sort(Comparator.comparingInt(User::salary).reversed());
        System.out.println("B1 薪资降序 -> " + brief(bySalaryDesc));

        // B2 多级：部门升序（null 排最后）→ 薪资降序 → 年龄升序（null 排最后）
        Comparator<User> byDeptAsc = Comparator.comparing(User::dept, Comparator.nullsLast(String::compareTo));
        Comparator<User> bySalaryDescCmp = Comparator.comparingInt(User::salary).reversed();
        Comparator<User> byAgeAscNullsLast = Comparator.comparing(User::age, Comparator.nullsLast(Integer::compareTo));

        List<User> multi = new ArrayList<>(users);
        multi.sort(byDeptAsc.thenComparing(bySalaryDescCmp).thenComparing(byAgeAscNullsLast));
        System.out.println("B2 部门↑ → 薪资↓ → 年龄↑(null 最后)：");
        multi.forEach(u -> System.out.println("   " + u));
        System.out.println("B2 thenComparing 是「前一键相等时才比下一键」，"
                + "所以只有同部门的人才会走到薪资比较，同薪资的才走到年龄");

        // B3 reversed() 的位置：这是最容易被 review 漏掉的一行
        List<User> wrong = new ArrayList<>(users);
        wrong.sort(Comparator.comparing(User::dept, Comparator.nullsLast(String::compareTo))
                .thenComparing(User::salary)
                .reversed());
        List<User> right = new ArrayList<>(users);
        right.sort(Comparator.comparing(User::dept, Comparator.nullsLast(String::compareTo))
                .thenComparing(User::salary, Comparator.reverseOrder()));

        System.out.println("B3 写法一 comparing(dept).thenComparing(salary).reversed() -> "
                + brief(wrong.subList(0, 5)) + " ...");
        System.out.println("B3 写法二 comparing(dept).thenComparing(salary, reverseOrder()) -> "
                + brief(right.subList(0, 5)) + " ...");
        System.out.println("B3 写法一把**整条链**反转了：部门也变成降序、null 从最后跑到最前；"
                + "写法二只反转薪资那一级");
        System.out.println("B3 规律：reversed() 作用在「它左边整个 Comparator」上，不是最近那一级");
        System.out.println("B3 而且 reversed() 之后 nullsLast 也一起被反转成 nullsFirst —— "
                + "这两条叠在一起，是线上「排序结果和需求反了」的常见来源");

        // B4 age 不做 null 处理
        System.out.println("B4 直接 comparing(User::age)，不管 null：");
        try {
            List<User> bad = new ArrayList<>(users);
            bad.sort(Comparator.comparing(User::age));
            System.out.println("B4   -> 排完了（不该走到这里）");
        } catch (NullPointerException e) {
            System.out.println("B4   -> " + e.getClass().getName() + ": " + e.getMessage());
            System.out.println("B4   comparing 的 keyExtractor 返回 null，紧接着就调 compareTo —— 拆箱前先炸");
        }
        List<User> ageNullsLast = new ArrayList<>(users);
        ageNullsLast.sort(byAgeAscNullsLast);
        System.out.println("B4 修法：comparing(User::age, nullsLast(Integer::compareTo)) -> "
                + brief(ageNullsLast.subList(0, 6)) + " ... " + brief(ageNullsLast.subList(13, 15))
                + "（末尾两位 age=null）");
        System.out.println("B4 注意 nullsLast 要包在「内层 Comparator」上："
                + "写成 comparing(User::age).nullsLast(...) 是编译不过的，nullsLast 是 Comparator 的静态方法");

        // B5 稳定性：先按薪资排、再按部门排，同部门内薪资顺序应当保留
        List<User> devOnly = users.stream().filter(isDev()).collect(Collectors.toList());
        List<User> twoPass = new ArrayList<>(devOnly);
        twoPass.sort(Comparator.comparingInt(User::salary));                 // 第一趟：薪资升序
        twoPass.sort(Comparator.comparing(User::dept));                      // 第二趟：只有部门
        System.out.println("B5 研发组先按薪资升序、再按部门排 -> " + brief(twoPass));
        System.out.println("B5 List.sort 用的是 TimSort（稳定），第二趟不会打乱第一趟的成果 —— "
                + "所以「多级排序」其实可以拆成多次单键排序，只要键的顺序从次要到主要倒着来");
        System.out.println("B5 但这条契约只对**对象数组 / List** 成立：Arrays.sort 对基本类型数组用的是双轴快排，"
                + "不稳定，别拿 int[] 做这事");
        System.out.println();
    }

    private Predicate<User> isDev() {
        return u -> "研发".equals(u.dept());
    }

    // ---------------------------------------------------------------- 工具
    private static void printBrief(List<User> list) {
        System.out.println("   " + brief(list));
    }

    private static String brief(List<User> list) {
        return list.stream().map(User::brief).collect(Collectors.joining(", "));
    }
}
