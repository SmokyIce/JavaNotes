package com.newt.java48.w01.day2;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Day 2：HashMap 的 put 全流程现场观测。
 *
 * ============================================================
 * 这个类到底在干什么？
 * ============================================================
 * 面试问 "HashMap 底层原理"，很多人能背出 "数组 + 链表 + 红黑树"，
 * 但被追问 "一个 key 到底怎么落到某个下标" 就卡住了。
 *
 * 卡住的原因是：这些过程全在 JDK 源码内部，你看不见。
 * 所以这个类的唯一目的，就是**把看不见的过程打印出来**：
 *
 *   1. key 怎么变成下标 —— hashCode → 扰动 → (n-1) & hash
 *   2. 为什么源码不直接用 hashCode()，而要 h ^ (h >>> 16)
 *   3. 冲突怎么处理 —— 同桶挂链
 *   4. 什么时候扩容 —— threshold = capacity * 0.75
 *   5. 扩容后元素去哪 —— 新位置只有两种可能（原下标 / 原下标 + oldCap）
 *   6. put 覆盖同一个 key 时返回什么
 *
 * ============================================================
 * 阅读顺序建议
 * ============================================================
 * main() → 实验 A → B → C → D → E → F。
 * 每个实验都是自包含的，先看注释里的「为什么」，再看代码里的「怎么做」。
 *
 * 运行：run.bat day2.HashMapProbe
 *      （必须带 --add-opens 参数，原因见 probeReflect() 的注释）
 */
public class HashMapProbe {

    // ==================================================================
    // 第一部分：观测工具
    //
    // 为什么要反射？因为 table（桶数组）和 threshold（扩容阈值）
    // 在 JDK 里是 private transient 字段，正常代码碰不到。
    // 我们想"看见"内部状态，就只能撬开它。
    //
    // 注意：生产代码里【绝对不要】这么写 —— 这是教学手段，不是工程手段。
    // ==================================================================

    /**
     * 反射能不能用，程序启动时探测一次。
     *
     * 【为什么需要这个探测？】
     * JDK 9 引入了模块系统（JPMS）。java.base 模块默认**不对外 opens** `java.util` 包，
     * 于是 `setAccessible(true)` 会被拒绝，抛：
     *
     *   InaccessibleObjectException:
     *     module java.base does not "opens java.util" to unnamed module
     *
     * 这不是 Bug，是**故意的封装保护** —— JDK 不希望你的代码依赖它的内部实现，
     * 因为 JDK 升级时这些字段随时可能改名或消失。
     *
     * 【怎么解决？】两种，我们都做：
     *   ① 启动参数显式开一个口子（已写进 run.bat / run.sh）：
     *        java --add-opens java.base/java.util=ALL-UNNAMED -cp out ...
     *      其中 ALL-UNNAMED 表示"对未命名模块（也就是 classpath 上的代码）开放"。
     *
     *   ② 代码里先探测一次（就是这个方法）。万一忘了加参数，
     *      后续就退回"按 size 推算"的降级模式，保证程序不崩、实验照样能看。
     *
     * 【顺带区分两个容易混的参数】
     *   --add-opens   ：允许**反射**访问（包括 setAccessible）
     *   --add-exports ：只允许**编译期/运行期直接访问**类，不允许反射改可见性
     *   要反射读内部字段，必须用 --add-opens。
     */
    static final boolean REFLECT_OK = probeReflect();

    static boolean probeReflect() {
        try {
            // 拿到 HashMap 的 table 字段（类型是 Node[]）
            Field f = HashMap.class.getDeclaredField("table");
            // setAccessible(true) = 绕过 private 检查。这一步就是被模块系统拦截的地方
            f.setAccessible(true);
            // 随便建一个空 map 试读一下，读得到说明参数加对了
            f.get(new HashMap<>());
            return true;
        } catch (Throwable t) {
            // 探不到不抛异常，安静返回 false，走降级路径
            return false;
        }
    }

    /**
     * 用反射读出 HashMap 内部的 table 数组。
     *
     * 【table 是什么？】
     * 就是"桶数组"，JDK 里声明为 `transient Node<K,V>[] table`。
     * table.length 就是容量（capacity）—— 注意是数组长度，不是元素个数。
     *
     * 【table 什么时候是 null？】
     * 刚 new 出来还没 put 过时是 null（懒加载）。JDK 源码里第一次 put 才走 resize() 分配 16 个槽位。
     */
    @SuppressWarnings("unchecked")
    static Object[] tableOf(HashMap<?, ?> map) {
        try {
            Field f = HashMap.class.getDeclaredField("table");
            f.setAccessible(true);
            return (Object[]) f.get(map);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 反射读出 threshold（扩容阈值）。
     *
     * 【threshold 的意义】threshold = capacity * loadFactor，
     * 当 size 超过它时触发扩容。默认 loadFactor = 0.75。
     * 所以初始容量 16 → 阈值 12；扩容到 32 → 阈值 24；以此类推。
     */
    static int thresholdOf(HashMap<?, ?> map) {
        try {
            Field f = HashMap.class.getDeclaredField("threshold");
            f.setAccessible(true);
            return (int) f.get(map);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 统一的容量获取入口：能反射就读真实值，读不到就推算。
     *
     * 【为什么要包一层？】把"有没有权限"这个细节挡在实验代码外面，
     * 六个实验只管调用，不用各自判断 REFLECT_OK。
     * 这是很常见的一种写法：把分支收敛到一个地方。
     */
    static int capacityOf(HashMap<?, ?> map) {
        if (REFLECT_OK) {
            Object[] t = tableOf(map);
            // table 可能还没分配（懒加载），此时容量记 0
            return (t == null) ? 0 : t.length;
        }
        return inferCapacity(map.size());
    }

    /**
     * 降级方案：根据 size 反推当前容量。
     *
     * 【推算依据】扩容是"容量翻倍、阈值为容量的 0.75"，
     * 所以当前容量是满足 `size <= capacity * 0.75` 的最小 2 的幂（从 16 起步）。
     *
     *   容量 16 → 阈值 12，能装 1~12 个
     *   容量 32 → 阈值 24，能装 13~24 个
     *   容量 64 → 阈值 48，能装 25~48 个
     *
     * 注意这是**推算**，不是真实值。好处是即使反射不可用，实验 D 的
     * "第 13/25/49 个触发扩容" 这条规律依然能观察出来。
     */
    static int inferCapacity(int size) {
        int cap = 0;
        while (size > (int) (cap * 0.75f)) {
            cap = (cap == 0) ? 16 : cap * 2;
        }
        return cap;
    }

    /**
     * 打一行状态：size / capacity / threshold。
     * 六个实验里凡是需要"看内部状态"的地方都调它，保证输出格式统一。
     */
    static void printState(String label, HashMap<?, ?> map) {
        int capacity = capacityOf(map);
        // 阈值直接用 capacity * 0.75 算，避免再反射一次
        int threshold = (int) (capacity * 0.75f);
        String note = REFLECT_OK ? "" : "   (反射被模块系统拦住，容量为推算值)";
        System.out.printf("  %-22s size=%-3d capacity=%-4d threshold=%-4d%s%n",
                label, map.size(), capacity, threshold, note);
    }

    // ==================================================================
    // 第二部分：复刻 JDK 8 的 hash 扰动函数
    //
    // 这两行是整场面试的核心。JDK 源码里它们散落在 putVal / getNode 里，
    // 我们抽出来单独写成方法，方便对照和调用。
    // ==================================================================

    /**
     * 复刻 JDK 8 的扰动函数。源码原文（就在 HashMap.putVal 的第一行）：
     *
     *     static final int hash(Object key) {
     *         int h;
     *         return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
     *     }
     *
     * 【三个细节】
     *   1. key 为 null 时返回 0 —— 所以 HashMap 允许一个 null key，它永远落在下标 0。
     *   2. `h >>> 16` 是无符号右移 16 位：把一个 int 的**高 16 位挪到低 16 位**。
     *      为什么不用 `>>`？因为 `>>` 是带符号右移，负数会补 1，结果会变得很难看且不随机。
     *   3. 异或 `^` 把高位的随机性"掺"进低位 —— 因为定位桶时只用低位（见 bucketIndex）。
     *
     * 【为什么要扰动？】看实验 B，有具体的反例。
     */
    static int jdkHash(Object key) {
        int h;
        // 这里刻意保留 `(h = key.hashCode())` 这种"赋值再使用"的写法，
        // 是为了和 JDK 源码逐字对齐，方便你去翻源码时一眼认出。
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * 复刻定位桶的那一行。源码是 `tab[i = (n - 1) & hash]`。
     *
     * 【为什么是 (n-1) & hash 而不是 hash % n？】
     * 因为 n 是 2 的幂时，`hash % n` 和 `hash & (n-1)` 结果完全相同，
     * 但位运算比取模快得多（取模要跑除法指令，几个时钟周期起步）。
     *
     * 【为什么 n 必须是 2 的幂？】
     * 只有 n 是 2 的幂时，n-1 的二进制才是"低位全 1"（如 16-1 = 0b1111），
     * 与运算才能恰好"取出 hash 的低 k 位"。如果 n = 15，n-1 = 0b1110，
     * 最低位永远是 0 → 下标永远取不到奇数 → 一半的桶永远用不上。
     *
     * 这也就解释了：为什么 `new HashMap<>(17)` 实际容量是 32 ——
     * 构造时会用 tableSizeFor() 把 17 向上取整到最近的 2 的幂。
     */
    static int bucketIndex(int hash, int capacity) {
        return (n1(capacity)) & hash;
    }

    /** (n - 1)。抽出来只是为了让上面那行读起来和源码一模一样，没有别的用途 */
    private static int n1(int n) {
        return n - 1;
    }

    // ==================================================================
    // 主流程：依次跑六个实验
    // ==================================================================

    public static void main(String[] args) {
        System.out.println("=== Day 2 Lab : HashMap put 全流程 ===");
        System.out.println("java.version = " + System.getProperty("java.version"));
        System.out.println();

        experimentA_hashToBucket();        // key 怎么变成下标
        experimentB_perturbationMatters(); // 不做扰动会怎样
        experimentC_collision();           // 哈希冲突现场
        experimentD_resizeTiming();        // 什么时候扩容
        experimentE_resizeRehash();        // 扩容后元素去哪
        experimentF_putReturnValue();      // put 的返回值
    }

    // ==================================================================
    // 实验 A：一个 key 怎么变成下标
    //
    // 目标：把 "hello" 这个 key 走到某个桶的全过程，一步不漏地打出来。
    // ==================================================================

    static void experimentA_hashToBucket() {
        System.out.println("========== 实验 A：key → hashCode → 扰动 → 下标 ==========");

        HashMap<String, Integer> map = new HashMap<>();
        // 这一行 put 会触发懒加载：table 从 null 变成长度 16 的数组。
        // 注意是"第一次 put 才分配"，new HashMap<>() 时并没分配 —— 这是省内存的设计。
        map.put("hello", 1);
        printState("put 第一个元素后", map);

        String key = "hello";
        int h = key.hashCode();          // 第一步：拿到 hashCode（32 位 int）
        int perturbed = jdkHash(key);    // 第二步：扰动（高 16 位异或进低 16 位）
        int capacity = capacityOf(map);  // 当前容量，应该是 16

        System.out.println("  key            = \"" + key + "\"");
        System.out.println("  hashCode()     = " + h + "   (0x" + Integer.toHexString(h) + ")");
        System.out.println("  hash()扰动后   = " + perturbed + "   (0x" + Integer.toHexString(perturbed) + ")");
        System.out.println("  capacity       = " + capacity);
        // 第三步：(n-1) & hash 得到最终下标
        System.out.println("  (n-1) & hash   = " + bucketIndex(perturbed, capacity) + "   <-- 最终落在哪个桶");
        System.out.println();
        System.out.println("  注意：(n-1) & hash 能代替 % 的唯一前提是 n 是 2 的幂。");
        System.out.println("        16-1 = 0b1111，与运算只取 hash 的低 4 位 —— 这就是为什么容量必须是 2 的幂。");
        System.out.println();
    }

    // ==================================================================
    // 实验 B：为什么必须做扰动
    //
    // 目标：造一个反例，证明"不扰动就会全挤一个桶"。
    //
    // 反例怎么造？让一批 hash 的高位各不相同、低位完全相同。
    // 容量 16 时只看低 4 位，于是不做扰动它们全落在同一个下标。
    // ==================================================================

    static void experimentB_perturbationMatters() {
        System.out.println("========== 实验 B：不扰动会怎样 ==========");

        // 假设容量 16，n-1 = 0b1111，所以只有 hash 的最低 4 位决定下标。
        System.out.println("  假设 capacity = 16，即 n-1 = 0b1111，只看 hash 的低 4 位。");
        System.out.println();

        // 这 4 个数的共同点：低 16 位都是 0x0001（低 4 位 = 1），
        // 只有高 16 位不同（0x1111 / 0x2222 / 0x3333 / 0x4444）。
        //
        // 现实里什么时候会遇到这种分布？
        //   - 用 Float 的 hashCode（JDK 早期版本的实现有问题，低位很容易相同）
        //   - 自己写的 key 类，hashCode 算法设计得不好（比如只用了不参与运算的字段）
        int[] rawHashes = {
                makeHash(0x1111_0001),
                makeHash(0x2222_0001),
                makeHash(0x3333_0001),
                makeHash(0x4444_0001),
        };
        String[] labels = {"key1", "key2", "key3", "key4"};

        // 表头：对比"扰动前"和"扰动后"的下标
        System.out.printf("  %-6s %-14s %-14s %8s %10s%n",
                "key", "rawHash(hex)", "raw低4位", "不扰动下标", "扰动后下标");
        for (int i = 0; i < rawHashes.length; i++) {
            int raw = rawHashes[i];
            // 这里手动做一次扰动，和 jdkHash() 里的公式完全一样，只是省掉了 hashCode() 调用
            int pert = raw ^ (raw >>> 16);
            System.out.printf("  %-6s 0x%08X     0x%X        %8d %10d%n",
                    labels[i], raw, raw & 0xF,
                    // 不扰动：直接 (n-1) & raw，n-1 = 15 = 0xF
                    (raw & 0xF),
                    // 扰动后：先扰动再取低位，(n-1) & perturbed，n-1 还是 0xF
                    (pert & 0xF));
        }
        System.out.println();
        System.out.println("  看出问题了：这 4 个 key 的低 4 位全是 1，");
        System.out.println("  如果不做扰动，它们会全部挤进同一个桶，链表退化成 O(n)。");
        System.out.println("  h ^ (h >>> 16) 把高 16 位掺进低 16 位，让低位也带上高位的随机性。");
        System.out.println();
    }

    /**
     * 造一个指定的 int。
     *
     * 【为什么要有这个方法？】直接写 `int[] rawHashes = {0x11110001, ...}` 也能跑，
     * 但那样"造数据"的意图不明显。包一层 makeHash() 是为了在代码里留下痕迹：
     * 这些数字不是随便写的，是**刻意构造**出来模拟 hashCode 分布的。
     *
     * （真实场景里，你没法控制 hashCode 的值，但可以构造实现了 hashCode 的测试类。）
     */
    static int makeHash(int value) {
        return value;
    }

    // ==================================================================
    // 实验 C：哈希冲突长什么样
    //
    // 目标：用一对真实的 hashCode 碰撞，演示"同桶 ≠ 同一个 key"。
    //
    // 为什么选 "Aa" 和 "BB"？因为它们的 hashCode 恰好都是 2112。
    // 这是 Java 里最有名的一对哈希冲突字符串。
    // ==================================================================

    static void experimentC_collision() {
        System.out.println("========== 实验 C：哈希冲突长什么样 ==========");

        HashMap<String, Integer> map = new HashMap<>();
        String a = "Aa";
        String b = "BB";
        System.out.println("  经典冲突对：\"Aa\" 和 \"BB\"");
        System.out.println("  \"Aa\".hashCode() = " + a.hashCode());
        System.out.println("  \"BB\".hashCode() = " + b.hashCode());
        System.out.println("  两者 hashCode 相同 → 落在同一个桶 → 桶内挂链表区分");

        // 两个 key 都放进去。注意：HashMap 此时必须用 equals() 来区分它们。
        // 如果只用 hashCode 判断，第二个 put 就会把第一个覆盖掉 —— 那就错了。
        map.put(a, 1);
        map.put(b, 2);
        int cap = capacityOf(map);
        int idxA = bucketIndex(jdkHash(a), cap);
        int idxB = bucketIndex(jdkHash(b), cap);
        System.out.println("  \"Aa\" 下标 = " + idxA + "   \"BB\" 下标 = " + idxB
                + "   " + (idxA == idxB ? "→ 确实同桶（桶内是链表）" : "→ 不同桶"));
        System.out.println("  两者 equals 为 " + a.equals(b) + " → 所以是两个不同的 key，都能存进去");
        System.out.println();
        System.out.println("  记住三句话：");
        System.out.println("    ① hashCode 相同，equals 不一定相同  → 允许，叫哈希冲突");
        System.out.println("    ② equals 相同，hashCode 必须相同    → 这是契约，违反会导致 get 不到");
        System.out.println("    ③ 所以自己写 key 类时，这两个方法要么都重写，要么都不重写");
        System.out.println();
    }

    // ==================================================================
    // 实验 D：什么时候扩容
    //
    // 目标：观察"第几个元素触发扩容"这个边界，验证阈值公式。
    //
    // 关键细节：扩容条件是 `++size > threshold`，是**严格大于**。
    // 所以容量 16、阈值 12 时，第 12 个元素不触发，第 13 个才触发。
    // 这个边界值面试经常问。
    // ==================================================================

    static void experimentD_resizeTiming() {
        System.out.println("========== 实验 D：什么时候扩容 ==========");
        System.out.println("  规则：size > threshold 时扩容，threshold = capacity * loadFactor(0.75)");
        System.out.println();

        HashMap<Integer, Integer> map = new HashMap<>();
        // 只在这些"关键个数"上打印状态，避免刷屏
        int[] watchPoints = {1, 12, 13, 24, 25, 48, 49};

        // 逐个 put，并在关键点观察。
        // 用 Integer 做 key 是因为它的 hashCode 就是 int 值本身，最容易预测。
        for (int i = 1; i <= 49; i++) {
            map.put(i, i);
            // 命中间观测点就打印一行
            for (int w : watchPoints) {
                if (i == w) {
                    printState("put 第 " + i + " 个后", map);
                }
            }
        }
        System.out.println();
        System.out.println("  规律：");
        System.out.println("    容量 16 → 阈值 12 → 第 13 个元素触发扩容到 32");
        System.out.println("    容量 32 → 阈值 24 → 第 25 个元素触发扩容到 64");
        System.out.println("    容量 64 → 阈值 48 → 第 49 个元素触发扩容到 128");
        System.out.println("  扩容是翻倍，所以容量永远是 2 的幂，这又保证了 (n-1)&hash 成立。");
        System.out.println();
    }

    // ==================================================================
    // 实验 E：扩容后，元素的新下标只有两种可能
    //
    // 目标：证明 JDK 8 的扩容优化 —— 不用重算 hash，只拆两条链。
    //
    // 原理（这个实验最重要的一段推理）：
    //   旧容量 16 → n-1 = 0b01111（低 5 位）
    //   新容量 32 → n-1 = 0b11111（低 6 位）
    //   两者只差最高位那一位（从 0b01111 到 0b11111，多了第 5 位）
    //   所以 (新n-1) & hash 相比 (旧n-1) & hash：
    //     · 多了那一位是 0 → 结果和原来一样（留在原下标，lo 链）
    //     · 多了那一位是 1 → 结果 = 原下标 + 16（移到原下标+oldCap，hi 链）
    // ==================================================================

    static void experimentE_resizeRehash() {
        System.out.println("========== 实验 E：扩容后，元素的新下标只有两种可能 ==========");
        System.out.println("  旧容量 = 16，新容量 = 32。对每个元素：");
        System.out.println("    新下标 = (32-1) & hash");
        System.out.println("    而 (32-1) 比 (16-1) 只多了最高那一位（第 5 位）。");
        System.out.println("    所以结果要么不动，要么加上 16（旧容量）。");
        System.out.println();

        int oldCap = 16;
        int newCap = 32;
        System.out.printf("  %-14s %12s %10s %10s %10s%n",
                "key", "hash(hex)", "旧下标", "新下标", "变化");
        // 挑几个不同分布的 key，让"不动"和"+16"两种结果都出现
        String[] keys = {"Aa", "BB", "hello", "world", "java", "map", "put", "get"};
        for (String k : keys) {
            int h = jdkHash(k);
            int oldIdx = bucketIndex(h, oldCap);
            int newIdx = bucketIndex(h, newCap);
            // 差值只会是 0（不动）或 16（旧容量），不可能出现第三种情况
            String delta = (newIdx == oldIdx) ? "不动" : ("+ " + (newIdx - oldIdx));
            System.out.printf("  %-14s 0x%08X %10d %10d %10s%n",
                    "\"" + k + "\"", h, oldIdx, newIdx, delta);
        }
        System.out.println();
        System.out.println("  JDK 8 的优化：不重新算 hash，只看 hash 里新增的那一位是 0 还是 1。");
        System.out.println("    是 0 → 留在原下标（lo 链）");
        System.out.println("    是 1 → 移到 原下标 + oldCap（hi 链）");
        System.out.println("  所以扩容时只是把一条链拆成两条，不用重新计算每个元素的 hash。");
        System.out.println();

        // 补充：这也解释了 JDK 7 的问题
        System.out.println("  【对比 JDK 7】JDK 7 扩容时要对每个元素重新计算下标，而且用的是**头插法**。");
        System.out.println("  头插在并发扩容时会让链表成环，进而导致 get() 死循环、CPU 打满 ——");
        System.out.println("  这就是著名的 HashMap 并发死循环问题。JDK 8 改成尾插 + 拆链，规避了这个问题。");
        System.out.println("  （但要记住：HashMap 本来就不支持并发，要并发请用 ConcurrentHashMap。）");
        System.out.println();
    }

    // ==================================================================
    // 实验 F：put 的返回值
    //
    // 目标：验证 put 返回"被覆盖的旧值"，顺便说清一个隐蔽的坑。
    //
    // 坑在哪：返回值是 null 时，你无法区分"首次插入"和"插入了一个 null value"。
    //        这两种情况 put 都返回 null。
    // ==================================================================

    static void experimentF_putReturnValue() {
        System.out.println("========== 实验 F：put 到底返回什么 ==========");

        HashMap<String, Integer> map = new HashMap<>();
        // 首次插入：没有旧值可返回，所以是 null
        Integer r1 = map.put("k", 100);
        System.out.println("  第一次 put(\"k\", 100) → 返回 " + r1 + "   (返回被覆盖的旧值；首次插入没有旧值所以是 null)");
        System.out.println("      size = " + map.size());

        // 覆盖：返回旧值 100，size 不变
        Integer r2 = map.put("k", 200);
        System.out.println("  第二次 put(\"k\", 200) → 返回 " + r2 + "   (旧值 100，被覆盖)");
        System.out.println("      size = " + map.size() + "   (没有变！覆盖不算新增)");

        // value 允许为 null，get 出来也是 null —— 这时返回值和"新增"就混在一起了
        Integer r3 = map.put("k", null);
        System.out.println("  第三次 put(\"k\", null)  → 返回 " + r3 + "   (旧值 200)");
        System.out.println("      get(\"k\") = " + map.get("k") + "   (value 可以是 null)");

        System.out.println();
        System.out.println("  三个结论：");
        System.out.println("    ① put 返回被覆盖的旧值，首次插入返回 null");
        System.out.println("    ② 覆盖已有 key，size 不变 —— 所以 map.size() 不等于 put 过的次数");
        System.out.println("    ③ 单看返回值无法区分首次插入和插入 null —— 面试常问的坑");
        System.out.println("       要区分得用 containsKey，或者 computeIfAbsent");
        System.out.println();

        // 补充演示：怎么正确区分"首次插入"
        System.out.println("  【正确写法】如果业务要求「仅当不存在时才写入」，用 putIfAbsent 或 computeIfAbsent：");
        HashMap<String, Integer> m2 = new HashMap<>();
        m2.put("a", 1);
        Integer old = m2.putIfAbsent("a", 99);   // 存在则不动，返回旧值 1
        System.out.println("    putIfAbsent(\"a\", 99) 返回 " + old + "，结果 a = " + m2.get("a") + "（没被覆盖）");
        Integer created = m2.computeIfAbsent("b", key -> 100);   // 不存在才计算并放入
        System.out.println("    computeIfAbsent(\"b\", k -> 100) 返回 " + created + "，结果 b = " + m2.get("b"));
        System.out.println();
    }
}
