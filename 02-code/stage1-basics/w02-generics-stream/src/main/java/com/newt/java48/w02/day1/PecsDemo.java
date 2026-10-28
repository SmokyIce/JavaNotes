package com.newt.java48.w02.day1;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

/**
 * Day 1 · 泛型（二）：PECS 与通配符
 *
 * <p>Producers Extends, Consumers Super —— 从哪儿读用 extends，往哪儿写用 super。
 * 五个实验：
 *
 * <pre>
 * A  泛型不变性：List&lt;String&gt; 不是 List&lt;Object&gt;，编译期就不让过
 * B  ? extends Number：能读成 Number，几乎不能写（只能写 null）
 * C  ? super Integer：能写 Integer，读出来只是 Object
 * D  自己实现 Collections.copy 的签名，看清两个通配符各自负责哪一端
 * E  Effective Java 的 Stack.pushAll / popAll：PECS 的真实用法
 * </pre>
 */
public class PecsDemo {

    public static void main(String[] args) {
        invariance();
        extendsWildcard();
        superWildcard();
        copySignature();
        stackPushPopAll();
        System.out.println("========== 收尾 ==========");
        System.out.println("PECS：参数是「生产者」用 extends（只读），是「消费者」用 super（只写）。");
        System.out.println("没有通配符的 <T> 只能保证「进和出是同一种类型」，一旦要跨类型传，就得放开一端。");
    }

    // ------------------------------------------------------------------ A
    private static void invariance() {
        System.out.println("========== A · 泛型是不变的（invariant） ==========");
        List<String> strs = new ArrayList<>(Arrays.asList("a", "b"));

        // 下面这行编译不过，正是泛型存在的理由：
        //   List<Object> objs = strs;
        //   objs.add(42);            <- 如果允许，strs 里就混进了 Integer
        //   String s = strs.get(0);  <- 这行就该 CCE 了
        System.out.println("List<Object> objs = strs;  编译错误：incompatible types");
        System.out.println("  （不是设计缺陷，是故意的：数组协变留下的坑，泛型用不变性补上）");

        // 数组是协变的，所以能在运行时报错 —— 对比着看就明白泛型为什么要「不变」
        Object[] arr = new String[2];
        try {
            arr[0] = 42;
        } catch (ArrayStoreException e) {
            System.out.println("对比：Object[] arr = new String[2]; arr[0] = 42; -> ArrayStoreException（运行时才发现）");
        }

        List<Object> copied = new ArrayList<>(strs);   // 只能复制一份，不能直接赋引用
        copied.add(42);
        System.out.println("复制一份之后 copied = " + copied + "，strs 仍然是 " + strs);
        System.out.println();
    }

    // ------------------------------------------------------------------ B
    private static void extendsWildcard() {
        System.out.println("========== B · ? extends Number：只读 ==========");
        List<Integer> ints = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Double> doubles = new ArrayList<>(Arrays.asList(1.5, 2.5));
        List<Long> longs = new ArrayList<>(Arrays.asList(10L, 20L));

        System.out.println("sum(List<Integer>) = " + sum(ints));
        System.out.println("sum(List<Double>)  = " + sum(doubles));
        System.out.println("sum(List<Long>)    = " + sum(longs));
        System.out.println("  签名是 sum(List<? extends Number>)，一个方法吃掉三种 List");

        // 写成 List<Number> 就不行了：List<Integer> 不是 List<Number>
        //   static double sum2(List<Number> xs)   -> sum2(ints) 编译错误

        List<? extends Number> wild = ints;
        Number n = wild.get(0);          // 读：至少是 Number，安全
        System.out.println("读出来 " + n + "，静态类型 Number，实际 " + n.getClass().getSimpleName());
        wild.add(null);                  // 唯一能写进去的值
        System.out.println("wild.add(null) 允许（null 是唯一对所有子类型都合法的值），size = " + wild.size());
        // wild.add(4);                  // 编译错误：capture of ? extends Number
        System.out.println("wild.add(4) 编译错误：? extends Number 可能是 Double，编译器不敢让你写");
        System.out.println("结论：extends 通配符 = 生产者，只能读；读出来的上界就是那个上界类型。");
        System.out.println();
    }

    private static double sum(List<? extends Number> xs) {
        double s = 0;
        for (Number x : xs) {
            s += x.doubleValue();       // 统一走 Number 的方法，不需要知道具体子类型
        }
        return s;
    }

    // ------------------------------------------------------------------ C
    private static void superWildcard() {
        System.out.println("========== C · ? super Integer：只写 ==========");
        List<Object> objs = new ArrayList<>();
        List<Number> nums = new ArrayList<>();
        List<Integer> ints = new ArrayList<>();

        fill(objs, 3);
        fill(nums, 3);
        fill(ints, 3);
        System.out.println("fill 到 List<Object>  -> " + objs + "  元素实际类型 "
                + objs.get(0).getClass().getSimpleName());
        System.out.println("fill 到 List<Number>  -> " + nums);
        System.out.println("fill 到 List<Integer> -> " + ints);
        System.out.println("  签名是 fill(List<? super Integer>, int)，三种容器都能收");

        List<? super Integer> wild = nums;
        wild.add(7);                     // 写：Integer 一定能塞进 Integer 的任一父类型容器
        Object got = wild.get(wild.size() - 1);   // 读：编译器只知道是 Object
        System.out.println("wild.add(7) 可以，nums 变成 " + nums + "；读出来静态类型只能是 Object -> " + got
                + "（运行时是 " + got.getClass().getSimpleName() + "）");
        // Integer bad = wild.get(0);    // 编译错误：容器可能是 List<Object>
        System.out.println("Integer bad = wild.get(0) 编译错误：容器可能是 List<Object>，里面不一定装 Integer");
        System.out.println("结论：super 通配符 = 消费者，只能写；读出来只能当 Object。");
        System.out.println();
    }

    private static void fill(List<? super Integer> dst, int count) {
        for (int i = 0; i < count; i++) {
            dst.add(i);
        }
    }

    // ------------------------------------------------------------------ D
    private static void copySignature() {
        System.out.println("========== D · Collections.copy 的签名为什么长那样 ==========");
        List<Object> dest = new ArrayList<>(Arrays.asList("x", "y", "z"));
        List<String> src = Arrays.asList("a", "b", "c");
        copy(dest, src);
        System.out.println("copy(List<Object> dest, List<String> src) 之后 dest = " + dest);
        System.out.println("  签名：<T> void copy(List<? super T> dest, List<? extends T> src)");
        System.out.println("  dest 是消费者 -> super；src 是生产者 -> extends；T 由两边共同推断出来");
        // 反过来就不行：copy(src, dest) 会让 String 容器接收 Object，编译期挡住
        System.out.println();
    }

    private static <T> void copy(List<? super T> dest, List<? extends T> src) {
        for (int i = 0; i < src.size(); i++) {
            dest.set(i, src.get(i));
        }
    }

    // ------------------------------------------------------------------ E
    private static void stackPushPopAll() {
        System.out.println("========== E · Stack 的 pushAll / popAll ==========");
        Stack<Number> stack = new Stack<>();
        List<Integer> ints = Arrays.asList(1, 2, 3);
        List<Double> doubles = Arrays.asList(1.5, 2.5);

        stack.pushAll(ints);        // Iterable<? extends E>
        stack.pushAll(doubles);
        System.out.println("pushAll(List<Integer>) + pushAll(List<Double>) 之后 size = " + stack.size());

        Collection<Object> sink = new ArrayList<>();
        stack.popAll(sink);         // Collection<? super E>
        System.out.println("popAll 到 Collection<Object> -> " + sink);

        // 这两行是 PECS 的反面教材，编译不过：
        //   stack.pushAll(Collection<Object>)   -> Object 不是 Number 的子类型
        //   stack.popAll(Collection<Integer>)   -> Integer 装不下 Double
        System.out.println("换成 Collection<Integer> 接 popAll 会编译错：栈里可能有 Double");
        System.out.println();
    }

    /** 迷你 Stack，签名照抄 Effective Java 第 31 条。 */
    static class Stack<E> {
        private final Deque<E> items = new ArrayDeque<>();

        void push(E e) {
            items.push(e);
        }

        E pop() {
            return items.pop();
        }

        int size() {
            return items.size();
        }

        /** E 的生产者：读 src 写自己，所以 src 用 extends。 */
        void pushAll(Iterable<? extends E> src) {
            for (E e : src) {
                push(e);
            }
        }

        /** E 的消费者：读自己写 dst，所以 dst 用 super。 */
        void popAll(Collection<? super E> dst) {
            while (!items.isEmpty()) {
                dst.add(pop());
            }
        }
    }
}
