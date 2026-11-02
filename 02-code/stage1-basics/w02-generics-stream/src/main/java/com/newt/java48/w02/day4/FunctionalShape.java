package com.newt.java48.w02.day4;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Day 4 · Lambda（一）：函数式接口的四种形态
 *
 * <p>Lambda 的语法五分钟就能背完，真正要记的是**形状**：一个函数式接口由
 * 「进几个参数 / 出不出值」唯一决定，JDK 的 40 多个函数式接口全是从这四格派生的。
 * 认出形状，就不用背名字。
 *
 * <pre>
 * A  四种形态：Function / BiFunction / Consumer / Supplier / Predicate 各配一个业务例子
 * B  Predicate 为什么值得单独存在：只有它能布尔组合，而且组合是短路的
 * C  andThen 与 compose 的顺序是反的 —— 线上写错不会报错，只会算错
 * D  装箱代价：Function&lt;Integer,Integer&gt; vs IntUnaryOperator，实测差几倍
 * E  什么时候必须自己写函数式接口：业务名 + default 组合方法（以及 Object 方法不算抽象方法）
 * F  一个坑：lambda 没有「目标类型」就存不下来（var 也不行）
 * </pre>
 *
 * <p>运行：{@code run.bat day4.FunctionalShape}
 */
public class FunctionalShape {

    public static void main(String[] args) {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
        sectionF();
        System.out.println("========== 收尾 ==========");
        System.out.println("函数式接口不用背名字，先问两件事：进几个？出不出？");
        System.out.println("Predicate 之所以独立，是因为只有 boolean 能 and / or / negate —— 这是「可组合」的前提。");
    }

    // ---------------------------------------------------------------- 演示模型
    private record Order(String id, int amount, boolean vip, String channel) { }

    private static final List<Order> ORDERS = List.of(
            new Order("A001", 200, true, "app"),
            new Order("A002", 80, false, "app"),
            new Order("A003", 500, false, "h5"),
            new Order("A004", 0, false, "h5"));

    // ================================================================== A
    private static void sectionA() {
        System.out.println("========== A · 四种形态 ==========");

        // A1 进 1 出 1：Function<T,R>
        Function<Order, Integer> discounted = o -> o.amount() * 80 / 100;

        // A2 进 2 出 1：BiFunction<T,U,R>
        BiFunction<Order, Integer, Integer> withCoupon = (o, off) -> o.amount() - off;

        // A3 进 1 出 0：Consumer<T>，只能产生副作用（打印、入库、发消息）
        Consumer<Order> logIt = o -> System.out.printf("   [log] %s 金额 %d%n", o.id(), o.amount());

        // A4 进 0 出 1：Supplier<T>，延迟生产 —— 这是它存在的唯一理由
        Supplier<Order> mock = () -> new Order("MOCK-1", 999, false, "test");

        // A5 进 1 出 boolean：Predicate<T>，出的不是值而是判断
        Predicate<Order> isVip = Order::vip;

        Order first = ORDERS.get(0);
        System.out.println("A1 Function<Order,Integer>      打八折      -> " + discounted.apply(first));
        System.out.println("A2 BiFunction<Order,Integer,..> 再减 30     -> " + withCoupon.apply(first, 30));
        logIt.accept(first);
        System.out.println("A4 Supplier<Order>              用时才创建  -> " + mock.get().id());
        System.out.println("A5 Predicate<Order>             是否 VIP    -> " + isVip.test(first));

        // A6 派生接口：只是把类型参数固定住，形状没变
        UnaryOperator<Integer> twice = x -> x * 2;                    // = Function<T,T>
        BinaryOperator<Integer> max = Integer::max;                    // = BiFunction<T,T,T>
        IntUnaryOperator inc = x -> x + 1;                             // 基本类型版，不装箱
        System.out.println("A6 UnaryOperator/BinaryOperator/IntUnaryOperator = "
                + twice.apply(21) + " / " + max.apply(3, 9) + " / " + inc.applyAsInt(41));

        System.out.println("A7 判定口诀：进 1 出 1 -> Function；进 2 出 1 -> BiFunction；"
                + "进 1 出 0 -> Consumer；进 0 出 1 -> Supplier；出 boolean -> Predicate");
        System.out.println("A7 同一形状换个类型参数就是新接口，所以 JDK 有 43 个函数式接口，真正不同的只有 5 种形状");
        System.out.println();
    }

    // ================================================================== B
    private static void sectionB() {
        System.out.println("========== B · Predicate：唯一能组合的形态 ==========");

        // 每个校验都打一行，用来观察「哪个真的被执行了」
        Predicate<Order> paid = o -> {
            System.out.println("   [check1] 金额 > 0");
            return o.amount() > 0;
        };
        Predicate<Order> stocked = o -> {
            System.out.println("   [check2] 有库存（这里故意返回 false）");
            return false;
        };
        Predicate<Order> notBlacklisted = o -> {
            System.out.println("   [check3] 不在黑名单");
            return true;
        };

        Predicate<Order> allPass = paid.and(stocked).and(notBlacklisted);
        System.out.println("B1 三层 and 组合，对 A001 求值：");
        System.out.println("B1 结果 = " + allPass.test(ORDERS.get(0)));
        System.out.println("B1 注意 [check3] 没打印 —— and 是**短路**的，check2 为 false 就不再往下走");

        Predicate<Order> bad = paid.negate();
        System.out.println("B2 negate()：金额<=0 的订单 -> "
                + ORDERS.stream().filter(bad).map(Order::id).toList());

        System.out.println("B3 or() 同样短路；这跟 && / || 的行为完全一致 —— "
                + "Predicate 的 and/or 就是给「条件」用的 && ||");
        System.out.println("B4 所以 Predicate 必须单独存在：Function<Order,Boolean> 也能返回布尔，但它没有 and/or，"
                + "而且语义是「算出一个值」，不是「判断一件事」");
        System.out.println();
    }

    // ================================================================== C
    private static void sectionC() {
        System.out.println("========== C · andThen 与 compose 的顺序是反的 ==========");

        Function<Integer, Integer> addOne = x -> x + 1;
        Function<Integer, Integer> timesTen = x -> x * 10;

        // C1 f.andThen(g) = g(f(x))：先自己，再参数
        System.out.println("C1 addOne.andThen(timesTen).apply(2) = " + addOne.andThen(timesTen).apply(2)
                + "   （(2+1)*10）");
        // C2 f.compose(g) = f(g(x))：先参数，再自己
        System.out.println("C2 addOne.compose(timesTen).apply(2) = " + addOne.compose(timesTen).apply(2)
                + "   （2*10+1）");

        // C3 链式：Comparator 里同名方法是 thenComparing，语义等同于 andThen
        List<Order> sorted = new ArrayList<>(ORDERS);
        sorted.sort(Comparator.comparing(Order::vip).reversed()
                .thenComparing(Order::amount, Comparator.reverseOrder()));
        System.out.println("C3 Comparator 链（VIP 优先、金额从高到低）= "
                + sorted.stream().map(o -> o.id() + ":" + o.amount()).toList());
        System.out.println("C3 thenComparing 就是 andThen 的专用版：前一个比较器相等时才用后一个");

        // C4 Function.identity() 的用处：需要一个「什么都不做」的占位
        Map<String, Function<Order, ?>> columns = new LinkedHashMap<>();
        columns.put("id", Order::id);
        columns.put("amount", Order::amount);
        columns.put("raw", Function.<Order>identity());   // 原样返回，用于「整行导出」
        System.out.println("C4 identity() 在导出 / 分组里的作用是「这一列不做转换」，"
                + "避免为了凑签名再写一个 o -> o");
        System.out.println("C4 注意它必须写成 Function.<Order>identity()："
                + "identity() 的 T 从参数推不出来，只能显式给（跟 lambda 缺目标类型是同一类问题）");
        System.out.println("C4 记忆：andThen 从左往右读，compose 从右往左读；业务代码 95% 用 andThen");
        System.out.println();
    }

    // ================================================================== D
    private static void sectionD() {
        System.out.println("========== D · 装箱代价：Function<Integer,Integer> vs IntUnaryOperator ==========");

        final int n = 20_000_000;
        Function<Integer, Integer> boxed = x -> x + 1;
        IntUnaryOperator primitive = x -> x + 1;

        // D1 结果立刻被拆箱求和 —— Integer 对象理论上「不逃逸」
        long tBoxed = bestOf(7, () -> {
            long sum = 0;
            for (int i = 0; i < n; i++) {
                sum += boxed.apply(i);      // int -> Integer -> apply -> Integer -> long
            }
            return sum;
        });
        long tPrim = bestOf(7, () -> {
            long sum = 0;
            for (int i = 0; i < n; i++) {
                sum += primitive.applyAsInt(i);   // 全程 int
            }
            return sum;
        });
        long tPlain = bestOf(7, () -> {
            long sum = 0;
            for (int i = 0; i < n; i++) {
                sum += i + 1;
            }
            return sum;
        });

        System.out.printf("D1 不逃逸：装箱版 Function<Integer,Integer> %6.1f ms%n", tBoxed / 1e6);
        System.out.printf("D1 不逃逸：基本类型版 IntUnaryOperator      %6.1f ms%n", tPrim / 1e6);
        System.out.printf("D1 不逃逸：裸循环（没有 lambda 调用）      %6.1f ms%n", tPlain / 1e6);
        System.out.printf("D1 装箱 / 基本类型 = %.1f 倍 —— 比想象中低，因为 Integer 没逃逸，%n",
                (double) tBoxed / tPrim);
        System.out.println("D1   标量替换（Escape Analysis）把每次 new Integer 都消掉了，"
                + "这就是「别背结论，要量」的原因");

        // D2 让结果逃逸到堆数组：这时装箱是真的要分配
        final int m = 2_000_000;
        Integer[] backBoxed = new Integer[m];
        int[] backPrim = new int[m];
        long tEscBoxed = bestOf(5, () -> {
            for (int i = 0; i < m; i++) {
                backBoxed[i] = boxed.apply(i);      // 每轮真的 new 一个 Integer，进堆
            }
            return (long) backBoxed[m - 1];
        });
        long tEscPrim = bestOf(5, () -> {
            for (int i = 0; i < m; i++) {
                backPrim[i] = primitive.applyAsInt(i);
            }
            return (long) backPrim[m - 1];
        });
        System.out.printf("D2 逃逸：  装箱版 %6.1f ms / 基本类型版 %6.1f ms%n",
                tEscBoxed / 1e6, tEscPrim / 1e6);
        System.out.printf("D2 逃逸时装箱 / 基本类型 = %.1f 倍 —— 这才是装箱的真实账单（%d 个 Integer 对象）%n",
                (double) tEscBoxed / tEscPrim, m);
        System.out.println("D3 结论：装箱贵不贵，取决于**逃没逃逸**，不取决于「有没有装箱」。"
                + "不逃逸时 JIT 直接消掉，逃逸时每个都是一次分配 + 一次 GC 压力");
        System.out.println("D4 业务里真正逃逸的场景：结果进 List/Map、当 key、被 lambda 捕获到别的线程、"
                + "返回给上层 —— 这些地方才是该换 Int/Long/Double 特化版的地方");
        System.out.println("D5 但也别急着全换：一次 lambda 调用在几十纳秒级，接口一次 IO 是毫秒级；"
                + "只有热路径（循环内、流里百万级元素）才值得换");
        System.out.println();
    }

    /** 跑 rounds 轮，取最小值 —— 单轮数字会被 JIT 分层编译骗（见 Day 2 §10）。 */
    private static long bestOf(int rounds, Supplier<Long> task) {
        long best = Long.MAX_VALUE;
        for (int r = 0; r < rounds; r++) {
            long t0 = System.nanoTime();
            long sink = task.get();
            long cost = System.nanoTime() - t0;
            if (sink == Long.MIN_VALUE) {
                System.out.print("");        // 防优化掉，实际不会发生
            }
            best = Math.min(best, cost);
        }
        return best;
    }

    // ================================================================== E
    /**
     * 自定义函数式接口。
     *
     * <p>为什么不用现成的 {@code Predicate<Order>}：① 名字带业务语义（读代码时不用猜 OrderRule 判什么）；
     * ② 能加自己的 default 组合方法，把「订单规则」这一层词汇收在接口里。
     *
     * <p>关键细节：{@code equals} / {@code hashCode} / {@code toString} 是 Object 的 public 方法，
     * 在接口里重新声明**不算抽象方法**；default / static 方法也不算。
     * 所以一个函数式接口可以有任意多个 default 方法，抽象方法只能有一个。
     */
    @FunctionalInterface
    private interface OrderRule {
        boolean test(Order o);

        default OrderRule and(OrderRule other) {
            return o -> this.test(o) && other.test(o);
        }

        static OrderRule alwaysTrue() {
            return o -> true;
        }
    }

    private static void sectionE() {
        System.out.println("========== E · 什么时候必须自己写函数式接口 ==========");

        OrderRule bigAmount = o -> o.amount() >= 200;
        OrderRule appChannel = o -> "app".equals(o.channel());
        OrderRule both = bigAmount.and(appChannel);

        System.out.println("E1 自定义 OrderRule 组合后，命中订单 = "
                + ORDERS.stream().filter(both::test).map(Order::id).toList());
        System.out.println("E2 抽象方法计数（反射数一下）：OrderRule 的抽象方法 = "
                + countAbstract(OrderRule.class) + " 个，default 方法 = " + countDefault(OrderRule.class) + " 个");
        System.out.println("E3 这就是 Comparator 在 Java 8 里能一口气加 reversed/thenComparing 的原因 —— "
                + "加的是 default 方法，已编译的 lambda 和实现类一个都不用改");
        System.out.println("E4 反过来说：给函数式接口加第二个抽象方法 = 破坏性变更，"
                + "所有 lambda 写法立刻编译不过（见 compile-errors/LambdaLimits.java 第 1 条）");
        System.out.println();
    }

    private static long countAbstract(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isAbstract(m.getModifiers()))
                .count();
    }

    private static long countDefault(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredMethods())
                .filter(m -> m.isDefault())
                .count();
    }

    // ================================================================== F
    private static void sectionF() {
        System.out.println("========== F · lambda 没有目标类型就存不下来 ==========");

        // F1 正确写法：左边必须是函数式接口
        IntUnaryOperator ok = x -> x * 3;
        System.out.println("F1 IntUnaryOperator ok = x -> x * 3;   -> ok.applyAsInt(7) = " + ok.applyAsInt(7));

        // F2 var 不给目标类型，所以推不出来（编译错，见 compile-errors/LambdaLimits.java 第 3 条）
        //     var bad = x -> x * 3;
        System.out.println("F2 var bad = x -> x * 3;  编译不过 —— var 只是「省略类型」，不提供目标类型");

        // F3 Object 不是函数式接口，所以也不能赋
        //     Object o = x -> x * 3;
        System.out.println("F3 Object o = x -> x * 3;  也编译不过 —— Object 没有唯一抽象方法，构不成 lambda 的目标类型");

        // F4 同理，lambda 不能直接当方法参数传给 Object 形参
        printAny((Object) (IntUnaryOperator) x -> x);       // 必须显式转成函数式接口再往上转
        System.out.println("F4 要传 Object 形参，得先转成函数式接口再向上转型（上面这行就是这么写的）");
        System.out.println("F5 结论：lambda 不是「一个值」，是「一个实现某个接口的写法」。"
                + "没有接口就没有 lambda —— 它靠目标类型确定签名和方法体该长什么样");
        System.out.println();
    }

    private static void printAny(Object o) {
        System.out.println("F4 收到了 " + o.getClass().getName());
    }
}
