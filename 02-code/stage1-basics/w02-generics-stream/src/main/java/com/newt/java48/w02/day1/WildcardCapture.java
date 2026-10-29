package com.newt.java48.w02.day1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Day 1 遗留补充 · 通配符捕获（capture of ?）
 *
 * <p>Day 1 只碰到了「capture of ?」这个报错文案，没写正例。这一段补上正例，
 * 并且对照 JDK 17 自己的选择 —— {@code Collections.swap} 没走捕获，走的是原始类型。
 *
 * <pre>
 * A  直接对 List&lt;?&gt; 做 swap 为什么编译不过（两层 set 的捕获不是同一个 CAP#1）
 * B  正例：helper 方法把 ? 捕获成 T，swap / reverse 都能写
 * C  JDK 17 的 Collections.swap 用 raw type 绕过，源码里那句注释放这儿
 * D  raw type 的代价：塞错类型编译器不管，错误推迟到「按类型读出来」
 * </pre>
 *
 * <p>运行：{@code run.bat WildcardCapture}
 */
public class WildcardCapture {

    public static void main(String[] args) {
        directCaptureFails();
        captureHelper();
        jdkChoice();
        rawTypeCost();
        System.out.println("========== 收尾 ==========");
        System.out.println("通配符捕获 = 编译器给这个 ? 起个临时名字（CAP#1）。但捕获发生在「每次方法调用」上：");
        System.out.println("同一个 List<?> 在同一句里调 get 和 set，各捕各的（CAP#1 / CAP#2），");
        System.out.println("所以「get 出来的那个」塞不回「要求 CAP#1 的 set」。把签名换成 <T> 之后，");
        System.out.println("T 在方法体里只推断一次，全程同一个类型，才写得下去。");
    }

    // ------------------------------------------------------------------ A
    private static void directCaptureFails() {
        System.out.println("========== A · 直接对 List<?> 交换，编译不过 ==========");
        System.out.println("  能想到的最自然写法：");
        System.out.println("    void swapBad(List<?> xs, int i, int j) {");
        System.out.println("        Object tmp = xs.get(i);");
        System.out.println("        xs.set(i, xs.get(j));   // <- 编译错误");
        System.out.println("        xs.set(j, tmp);         // <- 编译错误");
        System.out.println("    }");
        System.out.println("  javac -Xdiags:verbose 的原话（实测，不是想象）：");
        System.out.println("    error: method set in interface List<E> cannot be applied to given types;");
        System.out.println("      required: int,CAP#1");
        System.out.println("      found:    int,CAP#2");
        System.out.println("      reason: argument mismatch; Object cannot be converted to CAP#1");
        System.out.println("      where CAP#1,CAP#2 are fresh type-variables:");
        System.out.println("        CAP#1 extends Object from capture of ?");
        System.out.println("        CAP#2 extends Object from capture of ?");
        System.out.println("  关键就在 required CAP#1 / found CAP#2：同一句里的两个 ? 各自独立捕获，");
        System.out.println("  编译器没有依据认为 CAP#1 和 CAP#2 是同一个类型，而 set 要求「和 get 出来的那个一致」。");
        System.out.println("  第二行 xs.set(j, tmp) 的报错是 found: int,Object —— tmp 的静态类型是 Object，");
        System.out.println("  同样不是 CAP#1。所以两个 ? 的容器，连「读出来再放回去」都做不到。");
        System.out.println();
    }

    // ------------------------------------------------------------------ B
    private static void captureHelper() {
        System.out.println("========== B · 正例：helper 方法把 ? 捕获成 T ==========");
        List<Integer> ints = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        swapWild(ints, 0, 4);
        System.out.println("swapWild(List<Integer>, 0, 4) -> " + ints);
        System.out.println("  入口签名仍是 List<?>，里面转调 swap(List<T>)：");
        System.out.println("  T 被推断成「那个 ? 捕获出来的 CAP#1」，整个方法体内 T 是同一个类型，set 就合法了。");

        List<String> strs = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        reverseWild(strs);
        System.out.println("reverseWild(List<String>) -> " + strs);

        // 元素类型还是原来的：捕获不改变容器类型，只改变「编译器愿意让你写什么」
        ints.get(0).intValue();
        System.out.println("  交换之后 ints.get(0) 仍然是 Integer -> " + ints.get(0).getClass().getSimpleName());
        System.out.println();
    }

    /** 对外只暴露 List<?>：调用方不必关心元素类型。 */
    private static void swapWild(List<?> xs, int i, int j) {
        swap(xs, i, j);          // 捕获发生在这里：? -> CAP#1，T := CAP#1
    }

    /** 真正的实现：T 让 i / j 两个位置是同一个类型，set 才通得过。 */
    private static <T> void swap(List<T> xs, int i, int j) {
        T tmp = xs.get(i);
        xs.set(i, xs.get(j));
        xs.set(j, tmp);
    }

    /** 和 JDK 的 Collections.reverse(List<?>) 同一个套路。 */
    private static void reverseWild(List<?> xs) {
        for (int i = 0, j = xs.size() - 1; i < j; i++, j--) {
            swapWild(xs, i, j);
        }
    }

    // ------------------------------------------------------------------ C
    private static void jdkChoice() {
        System.out.println("========== C · JDK 17 自己怎么写的 ==========");
        System.out.println("  java.util.Collections:495-502（JDK 17 src.zip 原文）：");
        System.out.println("    @SuppressWarnings({\"rawtypes\", \"unchecked\"})");
        System.out.println("    public static void swap(List<?> list, int i, int j) {");
        System.out.println("        // instead of using a raw type here, it's possible to capture");
        System.out.println("        // the wildcard but it will require a call to a supplementary");
        System.out.println("        // private method");
        System.out.println("        final List l = list;");
        System.out.println("        l.set(i, l.set(j, l.get(i)));");
        System.out.println("    }");
        System.out.println("  两个结论：");
        System.out.println("  ① 捕获不是唯一解，JDK 明确知道「捕获要多一个私有方法」，选了 raw type + 压制警告；");
        System.out.println("  ② 它把 raw 的作用范围压到 3 行内（final List l = list 之后就再没碰过 list），");
        System.out.println("     这是 raw type 唯一可以接受的用法 —— 局部、短、有注释、有 @SuppressWarnings。");

        List<Integer> ints = new ArrayList<>(Arrays.asList(7, 8, 9));
        Collections.swap(ints, 0, 2);
        System.out.println("  Collections.swap(List<Integer>, 0, 2) -> " + ints + "（行为与 B 段一致）");
        System.out.println();
    }

    // ------------------------------------------------------------------ D
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void rawTypeCost() {
        System.out.println("========== D · raw type 的代价：错误被推迟到读出来 ==========");
        List<Integer> ints = new ArrayList<>(Arrays.asList(1, 2, 3));
        List raw = ints;
        raw.set(0, "我不是 Integer");      // 编译通过，只有 unchecked 警告
        System.out.println("  raw.set(0, \"我不是 Integer\") 编译通过，容器里现在混了一个 String");
        System.out.println("  容器内容 = " + raw);
        try {
            Integer first = ints.get(0);   // 编译器在这里插 checkcast Integer
            System.out.println("  居然拿到了 " + first);
        } catch (ClassCastException e) {
            System.out.println("  按 Integer 读出来 -> ClassCastException: " + e.getMessage());
        }
        System.out.println("  和 Day 1 C 段同一个病：错误现场（get）离肇事现场（raw.set）隔了好几层。");
        System.out.println("  所以 helper 捕获不只是「少个警告」，它把 checkcast 之外的类型约束也保住了。");

        // 顺带一个捕获的边界：Map<?, ?> 也是同理，只能读不能写
        Map<String, Integer> m = new HashMap<>();
        m.put("a", 1);
        Map<?, ?> wild = m;
        System.out.println("  Map<?, ?> 读 entrySet -> " + wild.entrySet()
                + "；但 wild.put(\"b\", 2) 编译不过（key 和 value 都是捕获类型）");
        System.out.println();
    }
}
