package com.newt.java48.w05.day1;

import com.newt.java48.w05.day1.app.JdbcOrderRepository;
import com.newt.java48.w05.day1.app.OrderRepository;
import com.newt.java48.w05.day1.app.OrderService;
import com.newt.java48.w05.day1.app.PriceCalculator;
import com.newt.java48.w05.support.Src;
import com.newt.java48.w05.support.Trace;

/**
 * 第一段实验：<b>没有 Spring 的版本</b>。
 *
 * <p>三个入口（Web / 定时任务 / 冒烟测试）都用到 OrderService，
 * 而 OrderService 需要仓储和计价器。谁来 new 这两个东西？答案是：<b>每个入口自己</b>。
 *
 * <p>跑三段：
 * <ol>
 *   <li>A —— 三处各自 new：功能一模一样，但造出三份仓储（= 三个连接池）；</li>
 *   <li>B —— 把装配收进 main、入口只收参数：实例共享了，可「什么时候 new / 只 new 一次 / 线程安全」全靠人；</li>
 *   <li>C —— 数一下「换实现」和「加依赖」分别要动几处，和 Spring 版对照。</li>
 * </ol>
 */
public class ManualWiring {

    // ---------- A 段：三个入口各自拼装 开始 ----------

    static OrderService webEntry() {
        return new OrderService(new JdbcOrderRepository(), new PriceCalculator());
    }

    static OrderService jobEntry() {
        return new OrderService(new JdbcOrderRepository(), new PriceCalculator());
    }

    static OrderService testEntry() {
        return new OrderService(new JdbcOrderRepository(), new PriceCalculator());
    }

    // ---------- A 段：三个入口各自拼装 结束 ----------

    // ---------- B 段：装配集中到 main 开始 ----------

    static OrderService webEntryShared(OrderRepository repo, PriceCalculator calc) {
        return new OrderService(repo, calc);
    }

    static OrderService jobEntryShared(OrderRepository repo, PriceCalculator calc) {
        return new OrderService(repo, calc);
    }

    static OrderService testEntryShared(OrderRepository repo, PriceCalculator calc) {
        return new OrderService(repo, calc);
    }

    private static void sectionB() {
        System.out.println();
        System.out.println("=== B. 把装配收进 main：实例共享了，但责任也全落到人身上 ===");

        int before = JdbcOrderRepository.instances();
        OrderRepository repo = new JdbcOrderRepository();
        PriceCalculator calc = new PriceCalculator();
        OrderService web = webEntryShared(repo, calc);
        OrderService job = jobEntryShared(repo, calc);
        OrderService test = testEntryShared(repo, calc);
        int made = JdbcOrderRepository.instances() - before;

        System.out.println("  入口一（Web）  : " + web.place(2L));
        System.out.println("  入口二（Job）  : " + job.place(2L));
        System.out.println("  入口三（Test） : " + test.place(2L));

        System.out.println("  ── 统计 ──");
        System.out.println("  这一段造出的仓储实例数 = " + made);
        System.out.printf("  三个入口拿到的仓储哈希   : %d / %d / %d（同一个）%n",
                System.identityHashCode(web.repository()),
                System.identityHashCode(job.repository()),
                System.identityHashCode(test.repository()));
        System.out.println("  代价：main 成了唯一的装配点 —— 谁先 new、只 new 一次、并发下安全吗，全靠人记着。");
        System.out.println("        再加第四个入口，回到这里改；再加一个依赖，三个入口全要改。");
    }

    // ---------- B 段：装配集中到 main 结束 ----------

    public static void main(String[] args) {
        Trace.mark("ManualWiring 启动（没有 Spring，没有容器）");
        sectionA();
        sectionB();
        sectionC();
    }

    private static void sectionA() {
        System.out.println();
        System.out.println("=== A. 三个入口各自 new：三份仓储 = 三个连接池 ===");

        int before = JdbcOrderRepository.instances();
        OrderService web = webEntry();
        OrderService job = jobEntry();
        OrderService test = testEntry();
        int made = JdbcOrderRepository.instances() - before;

        System.out.println("  入口一（Web）  : " + web.place(1L));
        System.out.println("  入口二（Job）  : " + job.place(1L));
        System.out.println("  入口三（Test） : " + test.place(1L));

        System.out.println("  ── 统计 ──");
        System.out.println("  这一段造出的仓储实例数 = " + made);
        System.out.printf("  三个入口拿到的仓储哈希   : %d / %d / %d%n",
                System.identityHashCode(web.repository()),
                System.identityHashCode(job.repository()),
                System.identityHashCode(test.repository()));
        System.out.println("  结论：功能正确，但每个入口都开了一个『连接池』；缓存、计数、连接全部各存一份。");
    }

    private static void sectionC() {
        System.out.println();
        System.out.println("=== C. 量化：换实现 / 加依赖分别要动几处（数本文件源码）===");

        String self = Src.text("src/main/java/com/newt/java48/w05/day1/ManualWiring.java");
        String aSection = Src.between(self, "A 段：三个入口各自拼装 开始", "A 段：三个入口各自拼装 结束");
        String bSection = Src.between(self, "B 段：装配集中到 main 开始", "B 段：装配集中到 main 结束");

        String[] needles = {"JdbcOrderRepository", "PriceCalculator", "OrderService"};
        System.out.printf("  %-30s %-24s %-21s %s%n",
                "待改的东西", "new JdbcOrderRepository", "new PriceCalculator", "new OrderService");
        for (String[] row : new String[][]{
                {"A 段（每入口各来一份）", aSection},
                {"B 段（装配集中到 main）", bSection}}) {
            System.out.printf("  %-30s %-24d %-21d %d%n", row[0],
                    Src.countNew(row[1], needles[0]),
                    Src.countNew(row[1], needles[1]),
                    Src.countNew(row[1], needles[2]));
        }
        System.out.println();
        System.out.println("  「换实现」（jdbc → memory）改的是 new JdbcOrderRepository( 那一行：A 段 3 处，B 段 1 处。");
        System.out.println("  「加依赖」（给 OrderService 再塞一个参数）改的是 new OrderService( 那一行：A 段 3 处，B 段 3 处。");
        System.out.println("  注意 B 段已经把「造仓储」收到 1 处了，但「造 OrderService」还是 3 处 ——");
        System.out.println("  因为每个入口都要拿到装配好的 Service，这一步没人能替你省。");
        System.out.println("  Spring 版的对照：入口代码里这两行出现 0 次，见 SpringWiring 的 C 段。");
    }
}
