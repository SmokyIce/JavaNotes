package com.newt.java48.w02.day4;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Day 4 · Lambda（二）：方法引用、变量捕获、以及编译器到底生成了什么
 *
 * <p>「lambda 里不能改外部局部变量」几乎所有教程都只说「规定如此」。
 * 这一节把它落到实处：**捕获的是值拷贝**，字节码里那个合成方法的形参就是证据。
 * 这跟 Day 1 的擦除、Day 2 的 sneakyThrow 是同一类问题 —— 编译器骗得过类型系统，
 * 骗不过运行时的内存布局。
 *
 * <pre>
 * A  方法引用五种写法（静态 / 未绑定实例 / 绑定实例 / 构造 / 数组构造）
 * B  捕获 = 值拷贝：合成方法的形参里有它；字段为什么能改
 * C  循环变量陷阱：for 不行、for-each 行、数组绕过会踩坑
 * D  lambda 里的 this 是外部类，匿名内部类里的 this 是自己
 * E  非捕获 lambda 被缓存成同一个实例（捕获的每次新建）—— 别拿它当 Map key
 * F  字节码：一次 invokedynamic + 隐藏类，磁盘上不多一个 .class
 * </pre>
 *
 * <p>运行：{@code run.bat day4.LambdaCapture}
 */
public class LambdaCapture {

    /** 故意做成字段：用来对比「局部变量捕获」和「字段读取」。 */
    private int delta = 1;

    public static void main(String[] args) {
        new LambdaCapture().run();
    }

    private void run() {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
        sectionF();
        System.out.println("========== 收尾 ==========");
        System.out.println("lambda 捕获的是**值拷贝**，不是变量本身 —— 这就是 effectively final 这条规定的真正原因。");
        System.out.println("它不是语法洁癖，而是「拷贝之后两个副本会分叉」这个物理事实的必然结果。");
    }

    // ================================================================== A
    private static void sectionA() {
        System.out.println("========== A · 方法引用五种写法 ==========");

        // A1 静态方法：参数列表完全对应
        Function<String, Integer> parse = Integer::parseInt;
        System.out.println("A1 静态    Integer::parseInt            -> " + parse.apply("42"));

        // A2 未绑定实例：第一个参数当「接收者」，所以形参比方法本身多一个
        ToIntFunction<String> length = String::length;
        System.out.println("A2 未绑定  String::length (1 个参数)     -> " + length.applyAsInt("lambda"));

        // A3 绑定实例：接收者已经定死，参数列表和方法本身一致
        String fixed = "captured";
        IntSupplier boundLength = fixed::length;
        System.out.println("A3 绑定    str::length    (0 个参数)     -> " + boundLength.getAsInt()
                + "   ← 同一个 length()，绑不绑定差一个参数");

        // A4 构造器
        Supplier<List<String>> newList = ArrayList::new;
        System.out.println("A4 构造    ArrayList::new               -> " + newList.get().getClass().getSimpleName());

        // A5 数组构造：很少人提，但 Function/Stream.toArray 里天天用
        IntFunction<int[]> newArray = int[]::new;
        System.out.println("A5 数组    int[]::new                   -> " + newArray.apply(5).length + " 长度的 int[]");

        // A6 方法引用也会按目标类型挑重载：valueOf 有 9 个重载，这里挑的是 (Object)
        Function<Object, String> str1 = String::valueOf;
        Function<Integer, String> str2 = String::valueOf;
        System.out.println("A6 String::valueOf 两个目标类型 -> " + str1.apply(3.14f) + " / " + str2.apply(99));
        System.out.println("A6 所以方法引用不是「缩写」，它同样靠目标类型做重载决议 —— "
                + "目标类型写错不会报错，会选中另一个重载");
        System.out.println();
    }

    // ================================================================== B
    private void sectionB() {
        System.out.println("========== B · 捕获的是值拷贝 ==========");

        // B1 局部变量：被捕获后必须 effectively final，改一下就编译不过
        int base = 10;
        IntUnaryOperator plusBase = x -> x + base;
        // base = 20;   // ← 取消注释立刻编译错：local variables referenced from a lambda
        //              //   expression must be final or effectively final
        System.out.println("B1 x -> x + base（base=10）-> " + plusBase.applyAsInt(5));

        // B2 铁证：javac 为每个 lambda 生成了一个合成方法，捕获的变量变成它的形参
        System.out.println("B2 本类全部合成方法：");
        Arrays.stream(LambdaCapture.class.getDeclaredMethods())
                .filter(Method::isSynthetic)
                .sorted(Comparator.comparing(Method::getName))
                .forEach(m -> System.out.println("     " + m.getName()
                        + Arrays.toString(m.getParameterTypes())
                        + "  static=" + java.lang.reflect.Modifier.isStatic(m.getModifiers())));

        Method captured = Arrays.stream(LambdaCapture.class.getDeclaredMethods())
                .filter(Method::isSynthetic)
                .filter(m -> m.getName().startsWith("lambda$sectionB"))
                .filter(m -> Arrays.equals(m.getParameterTypes(), new Class<?>[]{int.class, int.class}))
                .findFirst().orElseThrow();
        System.out.println("B2 上面那句 x -> x + base 对应 " + captured.getName()
                + Arrays.toString(captured.getParameterTypes())
                + "：第一个 int 是 x，**第二个 int 就是 base** —— 捕获 = 把它当实参传进去，所以拿到的是副本");
        System.out.println("B2 再看两个规律：捕获局部变量 -> 合成方法是 static + 多一个形参；"
                + "捕获 this（读写字段）-> 合成方法不是 static，this 隐式传入");
        System.out.println("B2 唯一的 lambda$sectionA$0(int) 是个例外：它来自 A 段的 int[]::new —— "
                + "数组没有构造器，方法引用也无从链接，只能生成一个合成方法");
        System.out.println("B2 既然方法体里用的是副本，外面再改 base 就会分叉，编译器干脆禁止改 —— "
                + "effectively final 的来源就是这个");

        // B3 字段为什么能改：捕获的不是字段，是 this 引用；字段值在堆上，两边看到的是同一份
        IntUnaryOperator readField = x -> x + this.delta;
        int before = readField.applyAsInt(5);
        this.delta = 100;
        int after = readField.applyAsInt(5);
        System.out.println("B3 lambda 读字段 delta：改之前 = " + before + "，把 delta 改成 100 之后 = " + after
                + "   ← 同一个 lambda，结果变了");

        // B4 想「改」局部变量，老办法是包一层数组：拷的是引用，内容在堆上共享
        int[] box = {10};
        IntUnaryOperator readBox = x -> x + box[0];
        int b1 = readBox.applyAsInt(5);
        box[0] = 100;
        int b2 = readBox.applyAsInt(5);
        System.out.println("B4 用 int[] 绕过：改之前 = " + b1 + "，box[0]=100 之后 = " + b2
                + "   ← 拷的是**引用**，引用指向的数组是共享的");
        System.out.println("B4 所以「不能改局部变量」准确说是「不能让引用指向别处」，"
                + "引用指向的对象内部照样能改 —— 这也是这段代码能编译的原因");
        System.out.println();
    }

    // ================================================================== C
    private static void sectionC() {
        System.out.println("========== C · 循环变量陷阱 ==========");

        // C1 for 循环的 i 是**整个循环共用的一个变量**，还在变，所以不能捕获
        // for (int i = 0; i < 3; i++) {
        //     Supplier<Integer> s = () -> i;    // 编译错：must be final or effectively final
        // }
        System.out.println("C1 for (int i = 0; ...) 里捕获 i -> 编译错");
        System.out.println("C1 原因不是「循环有毒」，而是 i 从头到尾只有一个变量，还在被 i++ 改");

        // C2 for-each 的循环变量在**每次迭代都是新变量**，所以每个 lambda 捕获到各自的值
        List<Supplier<Integer>> eachLoop = new ArrayList<>();
        for (int v : List.of(1, 2, 3)) {
            eachLoop.add(() -> v);
        }
        System.out.println("C2 for-each 捕获 -> " + eachLoop.stream().map(Supplier::get).toList()
                + "   ← 三个 lambda 各自拿到自己的副本");

        // C3 用数组绕过：所有 lambda 拿到的是同一个盒子，所以全是最后一次的值
        List<Supplier<Integer>> boxed = new ArrayList<>();
        int[] idx = {0};
        for (int i = 0; i < 3; i++) {
            boxed.add(() -> idx[0]);
            idx[0]++;
        }
        System.out.println("C3 用 int[] 绕过 -> " + boxed.stream().map(Supplier::get).toList()
                + "   ← 全是 3，因为它们共享同一个盒子");
        System.out.println("C3 这个坑在「循环里注册回调 / 监听器」时最容易踩："
                + "你以为注册了 3 个不同值的回调，实际注册了 3 个读同一个变量的");
        System.out.println("C4 正解排序：① 用 for-each（最干净）② 循环里 `int snapshot = i;` 再捕获 snapshot "
                + "③ 实在不行才用数组，并且注释写清为什么");
        System.out.println();
    }

    // ================================================================== D
    private void sectionD() {
        System.out.println("========== D · this 到底是谁 ==========");

        // D1 匿名内部类：this 是它自己
        Runnable anon = new Runnable() {
            @Override
            public void run() {
                System.out.println("D1 匿名内部类里的 this -> " + this.getClass().getName());
            }
        };
        anon.run();

        // D2 lambda：this 是外部类实例（LambdaCapture 自己）
        Runnable lambda = () -> System.out.println("D2 lambda 里的 this       -> " + this.getClass().getName());
        lambda.run();

        System.out.println("D3 实测两者不是同一个东西：lambda 没有自己的 this，它词法上就长在 run() 里，"
                + "所以 this 直接继承外面的");
        System.out.println("D4 这条的实际影响：lambda 里写 this.delta 指的是 LambdaCapture.delta，"
                + "不是「lambda 的字段」—— lambda 本身没有字段");
        System.out.println("D5 想在 lambda 里调外部类的方法不用写 Outer.this.xxx()，直接写就行；"
                + "匿名内部类反而经常需要 Outer.this 来区分");
        System.out.println();
    }

    // ================================================================== E
    private static IntUnaryOperator nonCapturing() {
        return x -> x + 1;                       // 不引用任何外部变量
    }

    private static IntUnaryOperator capturing(int base) {
        return x -> x + base;                    // 捕获了 base
    }

    private static void sectionE() {
        System.out.println("========== E · 同一行 lambda 是同一个对象吗 ==========");

        // E1 非捕获：同一个调用点返回同一个实例（invokedynamic 只链接一次）
        IntUnaryOperator a = nonCapturing();
        IntUnaryOperator b = nonCapturing();
        System.out.println("E1 非捕获 nonCapturing() == nonCapturing() -> " + (a == b)
                + "   ← 同一个单例，热路径上不产生对象");

        // E2 捕获：每次都要把 base 塞进去，所以每次新建
        IntUnaryOperator c = capturing(1);
        IntUnaryOperator d = capturing(1);
        System.out.println("E2 捕获   capturing(1) == capturing(1)       -> " + (c == d)
                + "   ← 每次调用都新建一个实例");

        // E3 后果：lambda 没有 equals/hashCode，所以拿它当 Map key 一定失效
        List<IntUnaryOperator> keys = List.of(c, d);
        System.out.println("E3 c.equals(d) = " + c.equals(d) + "，HashMap 里会当成两个不同的 key");
        System.out.println("E3 别拿 lambda 做 Map key / Set 元素 / 缓存键 —— 它既没有 equals，"
                + "对象身份也不稳定");

        // E4 另一面：非捕获 lambda 是单例，所以「lambda 一定比匿名类多创建对象」这句话是错的
        System.out.println("E4 反过来也值得记：非捕获 lambda 比匿名内部类**更轻**（单例 + 无 .class 文件），"
                + "只有捕获时才多一次对象分配");
        System.out.println();
    }

    // ================================================================== F
    private static void sectionF() {
        System.out.println("========== F · 字节码：invokedynamic + 隐藏类 ==========");

        IntUnaryOperator lam = x -> x + 1;
        Object anon = new Supplier<String>() {
            @Override
            public String get() {
                return "anon";
            }
        };

        System.out.println("F1 lambda 的运行时类名 -> " + lam.getClass().getName());
        System.out.println("F1 匿名类的运行时类名   -> " + anon.getClass().getName());
        System.out.println("F2 lambda 类是否隐藏（JDK 15+）-> " + lam.getClass().isHidden()
                + "；匿名类 -> " + anon.getClass().isHidden());
        System.out.println("F3 lambda 的接口实现 -> " + Arrays.toString(lam.getClass().getInterfaces()));

        // F4 磁盘上加了多少 .class：lambda 一个都没有
        try {
            Path dir = Paths.get(LambdaCapture.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).resolve("com/newt/java48/w02/day4");
            if (Files.exists(dir)) {
                try (var files = Files.list(dir)) {
                    List<String> classes = files
                            .map(p -> p.getFileName().toString())
                            .filter(n -> n.startsWith("LambdaCapture"))
                            .sorted()
                            .toList();
                    System.out.println("F4 day4 目录下 LambdaCapture 相关的 .class 文件：" + classes);
                }
            }
        } catch (Exception e) {
            System.out.println("F4 读 class 目录失败：" + e);
        }
        System.out.println("F4 本类里写了十几处 lambda，磁盘上却找不到一个 $$Lambda 文件 —— "
                + "lambda 的类在运行时由 LambdaMetafactory 动态生成，而且是隐藏类，不落盘");
        System.out.println("F4 上面列出的 $$Lambda$24 里的数字也会变（取决于运行时链接顺序），"
                + "所以任何时候都不要拿 lambda 的类名做判断");
        System.out.println("F5 匿名类相反：编译期就生成 LambdaCapture$1.class，数量随写法线性增长");
        System.out.println("F6 javap -c 能看到的是 `invokedynamic #xx // InvokeDynamic #0:apply:()...IntUnaryOperator;`，"
                + "不是 new + 匿名类（完整反汇编见 day4-bytecode.txt）");
        System.out.println();
    }
}
