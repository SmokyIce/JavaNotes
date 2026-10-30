package com.newt.java48.w02.day3;

import java.util.List;
import java.util.Optional;

/**
 * Day 3 · Optional（二）：一条链怎么写
 *
 * <p>上一节是「别这么写」，这一节是「该怎么写」。核心差别不是 API，而是**控制流方向**：
 * null 检查把「缺失」摊在每个 if 里，Optional 链把「缺失」收集到链尾，只处理一次。
 *
 * <pre>
 * A  null 检查 vs 一条链：同一件事，6 行 vs 1 行
 * B  map 与 flatMap 的差别：为什么会有 Optional&lt;Optional&lt;T&gt;&gt; 这种怪物
 * C  filter：连「有值但无效」一起挡掉
 * D  orElseGet / orElseThrow：缺省值 vs 带上下文的异常
 * E  ifPresentOrElse / Optional.stream()：消费侧的两条路
 * F  业务样例：优惠券算价，filter 顺手挡掉「券比订单还大」的负价
 * </pre>
 *
 * <p>运行：{@code run.bat day3.OptionalChain}
 */
public class OptionalChain {

    public static void main(String[] args) {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
        sectionF();
        System.out.println("========== 收尾 ==========");
        System.out.println("Optional 不是「消除 null」，而是「把 null 的处理推迟到链尾，只写一次」。");
        System.out.println("链上每一步都是「有就继续、没有就短路」，直到 orElse / orElseThrow 落地。");
    }

    // ---------------------------------------------------------------- 演示模型
    private record Address(String city, String zip) { }

    private record User(String name, Address address) { }

    private static final User FULL = new User("newt", new Address("杭州", "310000"));
    private static final User NO_ADDR = new User("newt", null);
    private static final User BLANK_CITY = new User("newt", new Address("  ", null));

    // ================================================================== A
    private static void sectionA() {
        System.out.println("========== A · null 检查 vs 一条链 ==========");

        // A1 传统写法：每一层都要判，且「默认值」要写三次
        String oldWay;
        if (FULL.address() != null && FULL.address().city() != null
                && !FULL.address().city().isBlank()) {
            oldWay = FULL.address().city();
        } else {
            oldWay = "未知";
        }

        // A2 一条链：只有链尾出现一次「缺省」
        String newWay = Optional.ofNullable(FULL)
                .map(User::address)
                .map(Address::city)
                .filter(c -> !c.isBlank())
                .orElse("未知");

        System.out.println("A1 传统写法 = " + oldWay + "；链式写法 = " + newWay);
        System.out.println("A2 三种缺失场景（无地址 / 城市为空白 / 城市正常），链式写法都是同一段代码：");

        for (User u : List.of(FULL, NO_ADDR, BLANK_CITY)) {
            String city = Optional.ofNullable(u)
                    .map(User::address)
                    .map(Address::city)
                    .filter(c -> !c.isBlank())
                    .orElse("未知");
            System.out.printf("   %-12s -> %s%n", describe(u), city);
        }
        System.out.println("A2 关键点：map 遇到 null 会直接短路成 empty，所以链上不需要任何 if");
        System.out.println();
    }

    private static String describe(User u) {
        if (u.address() == null) {
            return "无地址";
        }
        return u.address().city() == null ? "城市 null" : "城市[" + u.address().city() + "]";
    }

    // ================================================================== B
    /** 仓库风格的方法：它自己就返回 Optional。 */
    private static Optional<Address> addressOf(User u) {
        return Optional.ofNullable(u == null ? null : u.address());
    }

    private static void sectionB() {
        System.out.println("========== B · map 与 flatMap 的差别 ==========");

        // B1 map：函数是 T -> R，所以「R 是 Optional<Address>」时就套成两层
        Optional<Optional<Address>> nested = Optional.of(FULL).map(u -> addressOf(u));
        System.out.println("B1 map(u -> addressOf(u))     -> " + nested.getClass().getSimpleName()
                + "，要 .get().get() 才能拿到城市：" + nested.get().get().city());

        // B2 flatMap：函数是 T -> Optional<R>，直接把外层压平
        Optional<String> flat = Optional.of(FULL)
                .flatMap(u -> addressOf(u))
                .map(Address::city);
        System.out.println("B2 flatMap(u -> addressOf(u)) -> 一层 Optional，城市 = " + flat.orElse("未知"));

        // B3 map 用对的地方：函数返回的是普通值
        Optional<String> name = Optional.of(FULL).map(User::name);
        System.out.println("B3 map(User::name)            -> 普通值，用 map 就对了：" + name.orElse("-"));

        System.out.println("B4 判定规则只有一条：**被调的方法自己返回 Optional 就用 flatMap，返回普通值就用 map**");
        System.out.println("B4 写成 map 的后果不是编译错，而是多一层 Optional —— 会在链尾 orElse 时露馅");
        System.out.println();
    }

    // ================================================================== C
    private static void sectionC() {
        System.out.println("========== C · filter：连「有值但无效」一起挡掉 ==========");

        for (String raw : List.of("杭州", "  ", "")) {
            Optional<String> city = Optional.ofNullable(raw).filter(c -> !c.isBlank());
            System.out.printf("C1 输入 [%s] -> %s%n", raw, city.map(c -> "有效:" + c).orElse("被 filter 挡住"));
        }
        System.out.println("C1 filter 与 map 的关系：filter 只保留「让谓词为 true」的值，"
                + "结果仍是 Optional，所以可以继续接 map");
        System.out.println("C1 它的价值是把「空」和「脏」统一成同一个出口 —— empty");
        System.out.println();
    }

    // ================================================================== D
    private static void sectionD() {
        System.out.println("========== D · orElseGet / orElseThrow ==========");

        // D1 orElseGet：默认值本身有成本时用（详见 OptionalAntiPatterns 的 B 段）
        String city = Optional.of(NO_ADDR).map(User::address).map(Address::city)
                .orElseGet(() -> computeDefaultCity());
        System.out.println("D1 orElseGet(computeDefaultCity) = " + city);

        // D2 orElseThrow：把异常信息补全，这是它比 get() 唯一值钱的地方
        try {
            Optional.of(NO_ADDR).map(User::address).map(Address::city)
                    .orElseThrow(() -> new IllegalStateException("用户地址缺失，userId=10086"));
        } catch (IllegalStateException e) {
            System.out.println("D2 orElseThrow(带上下文) -> " + e.getMessage());
        }
        try {
            Optional.<String>empty().get();
        } catch (Exception e) {
            System.out.println("D2 同样场景 get()       -> " + e.getMessage() + "（没有 userId，不知道该查谁）");
        }

        System.out.println("D3 orElse 还有第三个陷阱：类型是 T，可以直接写 orElse(null)，"
                + "等于把 Optional 就地变回 null");
        System.out.println();
    }

    private static String computeDefaultCity() {
        System.out.println("   (computeDefaultCity 被调用了 —— 只在真的为空时才走这里)");
        return "北京";
    }

    // ================================================================== E
    private static void sectionE() {
        System.out.println("========== E · 消费侧：ifPresentOrElse 与 stream() ==========");

        // E1 ifPresentOrElse（JDK 9）：有值做什么、没值做什么，一次写清，不用先 isPresent
        for (User u : List.of(FULL, NO_ADDR)) {
            Optional.ofNullable(u).map(User::address).map(Address::city)
                    .ifPresentOrElse(
                            c -> System.out.println("E1 有城市：" + c),
                            () -> System.out.println("E1 没城市：走降级逻辑（用 IP 定位）"));
        }

        // E2 Optional.stream()（JDK 9）：把 List<Optional<T>> 里的空值直接过滤掉
        List<String> zips = List.of(FULL, NO_ADDR, BLANK_CITY).stream()
                .map(u -> Optional.ofNullable(u.address()).map(Address::zip))
                .flatMap(Optional::stream)
                .toList();
        System.out.println("E2 三个用户里有效的 zip = " + zips + "（空的被 flatMap(Optional::stream) 吃掉）");
        System.out.println("E2 注意 JDK 8 没有 Optional.stream()，等价写法是 filter(Optional::isPresent).map(Optional::get)");
        System.out.println();
    }

    // ================================================================== F
    private record Coupon(String code, int off) { }

    private static Optional<Coupon> findCoupon(String code) {
        return switch (code) {
            case "SAVE30" -> Optional.of(new Coupon("SAVE30", 30));
            case "SAVE500" -> Optional.of(new Coupon("SAVE500", 500));   // 券比订单还大
            case "EXPIRED" -> Optional.of(new Coupon("EXPIRED", 0));     // 优惠额 0 = 已失效
            default -> Optional.empty();
        };
    }

    private static void sectionF() {
        System.out.println("========== F · 业务样例：优惠券算价 ==========");
        int amount = 200;
        System.out.println("订单金额 = " + amount);

        for (String code : List.of("SAVE30", "SAVE500", "EXPIRED", "NOT_EXIST")) {
            int pay = findCoupon(code)
                    .filter(c -> c.off() > 0)              // 挡掉失效券
                    .filter(c -> c.off() <= amount)        // 挡掉「券比订单大」的负价
                    .map(Coupon::off)
                    .map(off -> amount - off)
                    .orElse(amount);
            System.out.printf("F1 券 [%-9s] -> 实付 %d%n", code, pay);
        }
        System.out.println("F1 两个 filter 都在链上，所以「没有券 / 券失效 / 券超额」三种情况共用同一个 orElse；");
        System.out.println("F1 如果写成 coupon.map(Coupon::off).orElse(0) 再算减法，SAVE500 会算出 -300。");
        System.out.println();
    }
}
