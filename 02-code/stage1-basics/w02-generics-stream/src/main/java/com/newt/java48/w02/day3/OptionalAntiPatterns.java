package com.newt.java48.w02.day3;

import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Day 3 · Optional（一）：三个反模式 + 五个陷阱
 *
 * <p>Optional 的价值不在 API，而在「谁必须处理空」这件事的归属。
 * 这一节专门收集「看起来用了 Optional、其实比 null 还差」的写法：
 *
 * <pre>
 * A  isPresent() + get() 当 null 检查：换了个壳的 != null，还顺手弄丢了报错信息
 * B  orElse(compute())：参数是 eager 求值，有值也白算一次
 * C  Optional 当字段 / 参数 / 集合元素：序列化直接炸、签名骗人、逼调用方包装
 * D  五个静默陷阱：of(null) / map 返回 null / flatMap 返回 null / get() / orElse(null)
 * E  一张「该不该用 Optional」的判定表
 * </pre>
 *
 * <p>运行：{@code run.bat day3.OptionalAntiPatterns}
 */
public class OptionalAntiPatterns {

    public static void main(String[] args) {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
    }

    // ================================================================== A
    /** 模拟一次「按 id 查昵称」，没查到返回 empty 而不是 null。 */
    private static Optional<String> nickname(long id) {
        return id == 1L ? Optional.of("newt") : Optional.empty();
    }

    private static void sectionA() {
        System.out.println("========== A · isPresent() + get() 当 null 检查 ==========");

        // A1 反模式：本质就是 != null，只是把判断对象从引用换成了容器
        Optional<String> opt = nickname(1L);
        String v1;
        if (opt.isPresent()) {
            v1 = opt.get();
        } else {
            v1 = "匿名";
        }

        // A1 正例：orElse 一行，语义是「没有就用默认值」，不是「先判空再取值」
        String v2 = nickname(1L).orElse("匿名");
        System.out.println("A1 两种写法结果相同：" + v1 + " / " + v2);
        System.out.println("A1 但写法 1 多了 5 行，且 get() 在 JDK 10 起被标注为「优先用 orElseThrow」");

        // A2 反模式：三层嵌套的 isPresent，缩进比 null 检查还深
        Optional<Order> order = findOrder("A1001");
        String city1;
        if (order.isPresent()) {
            Optional<Address> addr = order.get().address();
            if (addr.isPresent()) {
                Optional<String> c = addr.get().city();
                if (c.isPresent()) {
                    city1 = c.get();
                } else {
                    city1 = "未知";
                }
            } else {
                city1 = "未知";
            }
        } else {
            city1 = "未知";
        }

        // A2 正例：一条链，null 的处理被推到链尾，只写一次
        String city2 = findOrder("A1001")
                .flatMap(Order::address)
                .flatMap(Address::city)
                .orElse("未知");
        System.out.println("A2 三层嵌套 = " + city1 + "；一条链 = " + city2 + "（结果同，行数 16 vs 4）");

        // A3 反直觉：get() 的报错信息比 NPE 还差
        try {
            nickname(2L).get();
        } catch (NoSuchElementException e) {
            System.out.println("A3 empty.get() -> " + e.getClass().getName() + ": " + e.getMessage());
            System.out.println("A3 这一句里没有任何变量名、没有任何业务上下文，比 NPE 的 helpful message 更难查");
        }
        try {
            lengthOf(null);
        } catch (NullPointerException e) {
            System.out.println("A3 对照 NPE  -> " + e.getClass().getName() + ": " + e.getMessage());
        }
        System.out.println();
    }

    private static int lengthOf(String s) {
        return s.length();
    }

    // ================================================================== B
    private static int remoteCalls = 0;

    /** 模拟一次有真实成本的调用：20ms 的远程查询。 */
    private static String remote(String key) {
        remoteCalls++;
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "远程值(" + key + ")";
    }

    private static void sectionB() {
        System.out.println("========== B · orElse(compute()) 白算一次 ==========");

        Optional<String> cacheHit = Optional.of("本地缓存");

        // B1 反模式：orElse 的参数在调用 orElse 之前就求值了
        remoteCalls = 0;
        long t0 = System.nanoTime();
        String r1 = cacheHit.orElse(remote("user:1"));
        long t1 = System.nanoTime();
        System.out.printf("B1 orElse(remote())    有值时远程调用次数 = %d，耗时 %.1f ms，结果 %s%n",
                remoteCalls, (t1 - t0) / 1_000_000.0, r1);

        // B1 正例：orElseGet 收 Supplier，只有真的为空才求值
        remoteCalls = 0;
        long t2 = System.nanoTime();
        String r2 = cacheHit.orElseGet(() -> remote("user:1"));
        long t3 = System.nanoTime();
        System.out.printf("B1 orElseGet(() -> ...) 有值时远程调用次数 = %d，耗时 %.1f ms，结果 %s%n",
                remoteCalls, (t3 - t2) / 1_000_000.0, r2);

        // B2 空的时候两者都一样：这时 orElse 也不亏，亏的只在「有值」那条路径上
        remoteCalls = 0;
        Optional.<String>empty().orElse(remote("user:2"));
        int eagerWhenEmpty = remoteCalls;
        remoteCalls = 0;
        Optional.<String>empty().orElseGet(() -> remote("user:2"));
        int lazyWhenEmpty = remoteCalls;
        System.out.println("B2 空值时调用次数：orElse = " + eagerWhenEmpty + "，orElseGet = " + lazyWhenEmpty + "（一样）");

        // B3 什么时候不必纠结：参数是廉价的小对象，白算一次没有可观察代价
        List<String> cheap = cacheHit.map(s -> new ArrayList<String>()).orElse(new ArrayList<>());
        System.out.println("B3 orElse(new ArrayList<>()) 白构造一次，但对象没被使用，逃逸分析后基本无成本，"
                + "size=" + cheap.size());
        System.out.println("B3 判断标准：参数里有没有 IO / 远程调用 / 加锁 / 大对象 —— 有就必须 orElseGet");
        System.out.println();
    }

    // ================================================================== C
    /** C1 反模式：Optional 当字段。 */
    private static class Settings implements Serializable {
        private static final long serialVersionUID = 1L;
        String host = "127.0.0.1";
        Optional<String> port = Optional.of("8080");   // 就这一行让整个对象不可序列化
    }

    /** C1 正例：字段用可空类型，出口处再包一层。 */
    private static class SettingsFixed implements Serializable {
        private static final long serialVersionUID = 1L;
        String host = "127.0.0.1";
        String port;                                    // 允许为 null

        Optional<String> portOpt() {                     // 返回值才用 Optional
            return Optional.ofNullable(port);
        }
    }

    private static void sectionC() {
        System.out.println("========== C · Optional 当字段 / 参数 / 集合元素 ==========");

        // C1 序列化：Optional 没实现 Serializable
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(new Settings());
            System.out.println("C1 序列化成功，" + bos.size() + " 字节");
        } catch (NotSerializableException e) {
            System.out.println("C1 序列化失败 -> NotSerializableException: " + e.getMessage());
        } catch (java.io.IOException e) {
            System.out.println("C1 其它 IO 异常 -> " + e);
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(new SettingsFixed());
            System.out.println("C1 改成可空字段后序列化成功，" + bos.size() + " 字节");
        } catch (Exception e) {
            System.out.println("C1 仍然失败 -> " + e);
        }

        // C2 参数：签名写着 Optional，实际照样能传 null
        try {
            sendMail(null);
        } catch (NullPointerException e) {
            System.out.println("C2 sendMail(null) -> NullPointerException，栈顶 " + e.getStackTrace()[0].getMethodName());
        }
        System.out.println("C2 参数用 Optional 的两个代价：① 签名给人的安全感是假的；"
                + "② 每个调用点都要 Optional.ofNullable(x) 包一遍");
        System.out.println("C2 调用点噪声示例：" + sendMailSignatureCount() + " 个调用点 = "
                + sendMailSignatureCount() + " 次包装");

        // C3 集合元素
        List<Optional<String>> list = new ArrayList<>();
        list.add(Optional.of("a"));
        list.add(Optional.empty());
        list.add(Optional.of("c"));
        long present = list.stream().filter(Optional::isPresent).count();
        System.out.println("C3 List<Optional<String>> 长度 " + list.size() + "，有效 " + present
                + "；遍历时每个元素还要再判一次");
        List<String> flat = list.stream().flatMap(Optional::stream).toList();   // JDK 9+
        System.out.println("C3 正解：源头上就不放空值 -> " + flat + "（Optional.stream() 展平）");
        System.out.println();
    }

    private static int sendMailSignatureCount() {
        return 3;   // 演示用：方法有 3 个调用点
    }

    /** C2 反模式：Optional 当参数。 */
    private static void sendMail(Optional<String> to) {
        String addr = to.orElseThrow(() -> new IllegalArgumentException("收件人必填"));
        System.out.println("C2 发送到 " + addr);
    }

    // ================================================================== D
    private static void sectionD() {
        System.out.println("========== D · 五个静默陷阱 ==========");

        // D1 Optional.of(null)：立刻 NPE，和 ofNullable 差一个词
        try {
            Optional.of(null);
        } catch (NullPointerException e) {
            System.out.println("D1 Optional.of(null)      -> NPE（of 不允许 null，要用 ofNullable）");
        }

        // D2 map 里返回 null：整个 Optional 变 empty，不报错、不提示
        Optional<String> mapped = Optional.of("newt").map(s -> (String) null);
        System.out.println("D2 map(s -> null)        -> " + mapped + "（静默变空，可能掩盖真实 bug）");

        // D3 flatMap 里返回 null：NPE，因为 flatMap 要拿返回值当 Optional 用
        try {
            Optional.of("newt").flatMap(s -> null);
        } catch (NullPointerException e) {
            System.out.println("D3 flatMap(s -> null)    -> NPE（flatMap 要求返回 Optional，不能是 null）");
        }

        // D4 orElse(null)：Optional 的契约被就地破坏
        String leaked = Optional.<String>empty().orElse(null);
        System.out.println("D4 orElse(null)          -> 返回 " + leaked + "，等于把 Optional 又变回 null，"
                + "下游照样 NPE");

        // D5 orElse 与 orElseGet 混用：返回类型都是 T，编译期看不出区别
        System.out.println("D5 orElse / orElseGet 编译期完全同签名，只能靠 code review 或 Sonar 规则拦"
                + "（java:S5411 / java:S3553）");
        System.out.println();
    }

    // ================================================================== E
    private static void sectionE() {
        System.out.println("========== E · 判定表 ==========");
        System.out.println("用法                      | 结论   | 理由");
        System.out.println("--------------------------|--------|--------------------------------------");
        System.out.println("方法返回值                 | 该用   | 显式告诉调用方「可能没有」");
        System.out.println("字段                       | 别用   | 不可序列化；字段天然可空，包一层没收益");
        System.out.println("方法参数                   | 别用   | 逼调用方包装；照样能传 null");
        System.out.println("集合元素                   | 别用   | 空元素应该在源头过滤掉");
        System.out.println("Optional<Optional<T>>      | 绝不用 | 一定是用错了 map，应该 flatMap");
        System.out.println("OptionalInt / OptionalLong | 看情况 | 基本类型特化，避免装箱；没有 OptionalFloat");
        System.out.println();
        System.out.println("一句话：Optional 是「返回值的类型」，不是「所有可空位置的容器」。");
    }

    // ---------------------------------------------------------------- 演示模型
    private record Address(Optional<String> city) { }

    private record Order(Optional<Address> address) { }

    private static Optional<Order> findOrder(String id) {
        // A1001 有完整地址；A1002 的地址缺城市；其他查不到
        return switch (id) {
            case "A1001" -> Optional.of(new Order(Optional.of(new Address(Optional.of("杭州")))));
            case "A1002" -> Optional.of(new Order(Optional.of(new Address(Optional.empty()))));
            default -> Optional.empty();
        };
    }
}
