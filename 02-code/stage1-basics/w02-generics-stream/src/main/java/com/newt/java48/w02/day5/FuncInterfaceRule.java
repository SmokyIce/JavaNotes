package com.newt.java48.w02.day5;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Day 4 遗留补完 · 函数式接口的「抽象方法」到底怎么数
 *
 * <p>Day 4 只在 `compile-errors/LambdaLimits.java` 第 4 条里留了一个**反例**
 * （只声明 `equals` 的接口 → `no abstract method found`）。这里补正向的最小复现：
 * 同一个接口，反射数出 **4 个**抽象方法，编译器却只算 1 个 —— 而且 lambda 能赋值。
 *
 * <pre>
 * A  反射视角的抽象方法数 vs 编译器视角（JLS 9.8 排除了 Object 的 public 方法）
 * B  只要还剩 1 个真抽象方法，lambda 就成立（equals/toString/hashCode 随便写）
 * C  default / static 方法既不抢名额，也不影响 @FunctionalInterface
 * D  为什么会这样：Object 的方法本来就有实现，接口再声明也不算「待实现」
 * </pre>
 *
 * <p>运行：{@code run.bat day5.FuncInterfaceRule}
 */
public class FuncInterfaceRule {

    /** 反例：只有 equals —— 抽象方法数为 0，不是函数式接口。 */
    interface OnlyEquals {
        boolean equals(Object other);
    }

    /** 正例：一个真抽象方法 + 三个 Object 方法重声明 + 一个 default + 一个 static。 */
    interface Named {
        String name();

        boolean equals(Object other);

        String toString();

        int hashCode();

        default String upper() {
            return name().toUpperCase();
        }

        static String kind() {
            return "Named";
        }
    }

    public static void main(String[] args) {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        System.out.println("========== 收尾 ==========");
        System.out.println("数抽象方法时要**先减掉 Object 的 public 方法**（equals / hashCode / toString），"
                + "再减掉 default 和 static。");
        System.out.println("反射只告诉你「声明了什么」，函数式接口的判定规则是编译器另外一套 —— "
                + "跟 Day 1「同一个方法两套签名」是同一类双视角问题。");
    }

    // ================================================================== A
    private static void sectionA() {
        System.out.println("========== A · 两个视角，两个数字 ==========");

        Method[] declared = Named.class.getDeclaredMethods();
        long rawAbstract = Arrays.stream(declared)
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .count();
        long effective = Arrays.stream(declared)
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .filter(m -> !isObjectMethod(m))
                .count();

        System.out.println("A1 Named 声明的全部方法：" + Arrays.stream(declared)
                .map(m -> m.getName() + "()")
                .sorted()
                .collect(Collectors.joining(", ")));
        System.out.println("A2 反射数出的 abstract 方法 = " + rawAbstract);
        System.out.println("A3 减掉 Object 方法后 = " + effective + "   ← 编译器用的就是这个数");
        System.out.println("A4 所以「反射看不出来它是函数式接口」是真的："
                + "反射只说声明，判定规则在 JLS 9.8 里");

        long onlyEqualsRaw = Arrays.stream(OnlyEquals.class.getDeclaredMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .count();
        long onlyEqualsEffective = Arrays.stream(OnlyEquals.class.getDeclaredMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .filter(m -> !isObjectMethod(m))
                .count();
        System.out.println("A5 OnlyEquals：反射数出 " + onlyEqualsRaw + " 个，实际算 " + onlyEqualsEffective
                + " 个 -> 0 个抽象方法，它不是函数式接口（反例原文在 compile-errors/LambdaLimits.java 第 4 条）");
        System.out.println();
    }

    // ================================================================== B
    private static void sectionB() {
        System.out.println("========== B · 只剩 1 个真抽象方法，lambda 就成立 ==========");

        Named n = () -> "order-service";
        System.out.println("B1 Named n = () -> \"order-service\"  -> n.name() = " + n.name());
        System.out.println("B2 equals / toString / hashCode 都可以被重声明，但不占名额："
                + "n.equals(n) = " + n.equals(n) + "，n.toString() = " + n);
        System.out.println("B3 注意 lambda 没重写 equals/toString —— 它用的是 Object 的实现，"
                + "所以 lambda 的 toString 是一串 $$Lambda 地址，不能用来做业务标识");
        System.out.println("B4 default 方法照常可用：n.upper() = " + n.upper()
                + "；static 方法走接口名：Named.kind() = " + Named.kind());
        System.out.println();
    }

    // ================================================================== C
    private static void sectionC() {
        System.out.println("========== C · default / static 不抢名额 ==========");

        @FunctionalInterface
        interface Rule {
            boolean test(int amount);

            default Rule and(Rule other) {
                return v -> this.test(v) && other.test(v);
            }

            static Rule alwaysTrue() {
                return v -> true;
            }
        }

        Rule positive = v -> v > 0;
        Rule big = v -> v >= 100;
        Rule both = positive.and(big);
        System.out.println("C1 1 个抽象 + 1 个 default + 1 个 static，@FunctionalInterface 依然合法");
        System.out.println("C2 positive.and(big)：100 -> " + both.test(100) + "，50 -> " + both.test(50)
                + "，-1 -> " + both.test(-1));
        System.out.println("C3 这就是 Predicate / Comparator / Function 都能链式的原因："
                + "它们靠 default 方法扩展，而不是靠加抽象方法");
        System.out.println("C3 JDK 8 加函数式接口时正是这条规则救了全世界的实现类 —— "
                + "抽象方法一加，所有实现立刻编译失败");
        System.out.println();
    }

    // ================================================================== D
    private static void sectionD() {
        System.out.println("========== D · 顺带看两个 JDK 自带的痕迹 ==========");

        List<String> raw = Arrays.stream(Comparator.class.getDeclaredMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .filter(m -> !isObjectMethod(m))
                .map(Method::getName)
                .collect(Collectors.toList());
        System.out.println("D1 Comparator 的真抽象方法：" + raw
                + "（equals 是重声明，减掉了；thenComparing 等全是 default）");

        List<String> fu = new ArrayList<>();
        UnaryOperator<String> trim = String::trim;
        Function<String, String> same = Function.identity();
        Predicate<String> notEmpty = s -> !s.isEmpty();
        fu.add(trim.apply("  a  "));
        fu.add(same.apply("b"));
        fu.add(String.valueOf(notEmpty.test("c")));
        System.out.println("D2 三个内置接口的方法引用 / lambda 可以混着塞进同一个 List<String>：" + fu);
        System.out.println("D3 它们的抽象方法都是一个：apply / apply / test —— 形状相同就能互相替代，"
                + "这也是 Day 4「形状比名字重要」的由来");
        System.out.println();
    }

    // ---------------------------------------------------------------- 工具
    /** JLS 9.8：与 Object 的 public 方法签名相同的接口方法，不算抽象方法。 */
    private static boolean isObjectMethod(Method m) {
        try {
            Object.class.getMethod(m.getName(), m.getParameterTypes());
            return Modifier.isPublic(m.getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
