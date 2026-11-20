package com.newt.java48.w05.support;

/**
 * 脚手架：把「谁 new 的、什么时候 new 的」打出来。
 *
 * <p>它只做两件事：
 * <ul>
 *   <li>{@link #mark} —— 打一条带相对时间的记号，用来量「容器启动 / getBean 各发生在第几毫秒」；</li>
 *   <li>{@link #constructed} —— 在构造函数里调用，打印调用者 + Spring 内部关键帧。</li>
 * </ul>
 *
 * <p>之所以要单独一个类，是因为「手写版」和「Spring 版」要用同一把尺子量，
 * 否则两边的数字和现象没法放在一张表里比（这是 11-11 踩过的坑）。
 */
public final class Trace {

    private static final long T0 = System.nanoTime();

    /** Spring 里能证明「是它 new 的」的三帧。手写版这几帧一定命中不了。 */
    private static final String[] SPRING_MARKS = {
            "createBeanInstance",       // AbstractAutowireCapableBeanFactory：反射造对象（1222 构造器解析 / 1232 无参）
            "preInstantiateSingletons", // DefaultListableBeanFactory：refresh 里预实例化单例（955 = getBean）
            "getSingleton",             // DefaultSingletonBeanRegistry：234 行 = singletonFactory.getObject()，
                                        //   正是它把 doCreateBean 叫起来的（不是三级缓存的查询，那个是另一个重载）
    };

    private Trace() {
    }

    public static long ms() {
        return (System.nanoTime() - T0) / 1_000_000L;
    }

    public static void mark(String msg) {
        System.out.printf("[%4d ms] %s%n", ms(), msg);
    }

    /**
     * 在构造函数第一行调用。
     *
     * @param bean 类名（短名）
     * @param extra 想额外打的信息，null 表示不打
     */
    public static void constructed(String bean, String extra) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        String thread = Thread.currentThread().getName();

        System.out.printf("  [构造] %-20s thread=%-14s t=+%d ms%s%n",
                bean, thread, ms(), extra == null ? "" : "  " + extra);

        int idx = indexOfMethod(st, "constructed");
        String caller = null;
        // idx 是本方法，idx+1 是调用它的构造函数本身，所以从 idx+2 开始往上找
        for (int i = idx + 2; i < st.length; i++) {
            String c = st[i].getClassName();
            if (c.equals(Trace.class.getName())) continue;
            if (c.startsWith("java.") || c.startsWith("jdk.") || c.startsWith("sun.")) continue;
            caller = c + "." + st[i].getMethodName() + ":" + st[i].getLineNumber();
            break;
        }
        System.out.println("          ↑ 直接调用者 = " + (caller == null ? "?" : caller));

        for (String key : SPRING_MARKS) {
            int hit = indexOfMethod(st, key);
            if (hit > 0) {
                System.out.println("          ↑ Spring 帧命中 = "
                        + st[hit].getClassName() + "." + key + ":" + st[hit].getLineNumber());
            }
        }
    }

    private static int indexOfMethod(StackTraceElement[] st, String method) {
        for (int i = 0; i < st.length; i++) {
            if (st[i].getMethodName().equals(method)) {
                return i;
            }
        }
        return -1;
    }
}
