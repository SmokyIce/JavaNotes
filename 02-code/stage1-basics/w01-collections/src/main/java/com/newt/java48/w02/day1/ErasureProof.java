package com.newt.java48.w02.day1;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Day 1 · 泛型（一）：类型擦除到底擦掉了什么
 *
 * <p>六个实验，每个都要求「先预测输出，再运行」：
 *
 * <pre>
 * A  两个不同泛型实参的 List，运行时是同一个 Class 对象
 * B  反射看到的 add / set 参数类型是 Object —— 擦除擦的是「使用处的实参」
 * C  运行时能往 List&lt;String&gt; 里塞 Integer，异常发生在取值处而不是存值处
 * D  擦除之后 new T[n] 不成立，于是 JDK 用 Array.newInstance(Class&lt;T&gt;, n) 兜底
 * E  桥方法（bridge）是编译器为了让擦除后的签名对得上而补的转发方法
 * F  擦除的四条硬限制（编译期就被挡住，见 ../compile-errors/）
 * </pre>
 *
 * <p>运行：{@code run.bat ErasureProof}（JDK 17）
 */
public class ErasureProof {

    /** 声明处带类型变量：擦除擦不掉类声明上的 T，只擦使用处的实参。 */
    static class Box<T> {
        private T value;

        void set(T v) {
            this.value = v;
        }

        T get() {
            return value;
        }
    }

    /** 继承时写了实参 String —— 这是泛型信息唯一被保留下来的地方（class 文件 Signature 属性）。 */
    static class StringBox extends Box<String> {
    }

    /** 实现 Comparable 风格的桥方法现场：接口方法擦除后是 compare(Object, Object)。 */
    static class StringCmp implements Comparator<String> {
        @Override
        public int compare(String a, String b) {
            return a.length() - b.length();
        }
    }

    public static void main(String[] args) throws Exception {
        sectionA();
        sectionB();
        sectionC();
        sectionD();
        sectionE();
        sectionF();
        // Box<String> box = new Box<String>();
        // Box b2 = box;
        // box.set("1");
        // System.out.println(box.get());
        // b2.set(2);
        // System.out.println(b2.get());
        // String s = box.get();
        // b2.set(new Object());
        // System.out.println(b2.get());
    }

    // ------------------------------------------------------------------ A
    private static void sectionA() {
        System.out.println("========== A · 运行时还有没有泛型？ ==========");
        List<String> strings = new ArrayList<>();
        List<Integer> ints = new ArrayList<>();

        System.out.println("strings.getClass() = " + strings.getClass().getName());
        System.out.println("ints.getClass()    = " + ints.getClass().getName());
        System.out.println("是同一个 Class 对象？ " + (strings.getClass() == ints.getClass()));
        System.out.println("new Box<String>() 和 new Box<Integer>() 同一个类？ "
                + (new Box<String>().getClass() == new Box<Integer>().getClass()));

        // 接口上的类型实参同样擦掉：List<String> 的运行时类型就是 ArrayList，连 List 都不是
        System.out.println("strings instanceof List ？ " + (strings instanceof List));
        System.out.println("strings.getClass() == List.class ？ " + (strings.getClass() == List.class)
                + "   <- 泛型接口的实参也没了，只剩实现类");
        System.out.println("结论：泛型只活在编译器眼里，运行时的对象身上一个字节都没有。");
        System.out.println();
    }

    // ------------------------------------------------------------------ B
    private static void sectionB() throws Exception {
        System.out.println("========== B · 反射看到的签名长什么样 ==========");
        System.out.println("List 接口里所有 add 方法的擦除后签名：");
        for (Method m : List.class.getDeclaredMethods()) {
            if (!m.getName().equals("add")) {
                continue;
            }
            StringBuilder sb = new StringBuilder("  " + m.getName() + "(");
            Class<?>[] ps = m.getParameterTypes();
            for (int i = 0; i < ps.length; i++) {
                sb.append(i == 0 ? "" : ", ").append(ps[i].getSimpleName());
            }
            System.out.println(sb.append(")"));
        }

        // 你以为存在的 add(String) 其实不存在
        try {
            List.class.getMethod("add", String.class);
            System.out.println("  getMethod(\"add\", String.class) 居然成功了？");
        } catch (NoSuchMethodException e) {
            System.out.println("  getMethod(\"add\", String.class) -> NoSuchMethodException（没有这个方法）");
        }
        System.out.println("  getMethod(\"add\", Object.class)  -> "
                + List.class.getMethod("add", Object.class).toGenericString());

        // 声明处的 T 还在：类上的 TypeVariable、方法上的泛型返回类型都能读出来
        TypeVariable<Class<Box>>[] tps = Box.class.getTypeParameters();
        System.out.println("Box 类声明上的类型变量：" + tps[0].getName()
                + "（边界 " + tps[0].getBounds()[0].getTypeName() + "）");
        System.out.println("Box.get 的泛型返回类型：" + Box.class.getDeclaredMethod("get").getGenericReturnType());
        System.out.println("Box.set 的参数类型（反射 getParameterTypes）："
                + Box.class.getDeclaredMethod("set", Object.class).getParameterTypes()[0].getSimpleName());
        System.out.println("结论：擦掉的是「使用处填的实参」，声明处的类型变量 T 以元数据形式留在 class 文件里。");

        // B2：唯一能把实参捞回来的地方 —— 子类继承时写下的那段签名
        System.out.println();
        System.out.println("B2 · 实参藏在哪：子类的 Signature 属性");
        System.out.println("  StringBox.class.getGenericSuperclass() = "
                + StringBox.class.getGenericSuperclass());
        java.lang.reflect.ParameterizedType pt =
                (java.lang.reflect.ParameterizedType) StringBox.class.getGenericSuperclass();
        System.out.println("  从中拿到的实参 = " + pt.getActualTypeArguments()[0]);

        // 注意：这里必须用 getDeclaredMethod + 父类。Box.set 是包私有，getMethod 只认 public，
        // 直接 StringBox.class.getMethod("set", Object.class) 会抛 NoSuchMethodException（踩过）。
        Method setter = Box.class.getDeclaredMethod("set", Object.class);
        System.out.println("  但 Box.set 的泛型参数类型 = " + setter.getGenericParameterTypes()[0]
                + "   <- 方法自己的签名里只有 T，没有 String");
        System.out.println("  StringBox 自己声明的方法数 = " + StringBox.class.getDeclaredMethods().length
                + "   <- 一个都没重写，实参只写在了类声明那一行");
        System.out.println("  => 想复原类型，只能「继承一个带实参的类」再读 getGenericSuperclass()：");
        System.out.println("     Spring 的 ParameterizedTypeReference、Jackson 的 TypeReference 都是这一招。");
        System.out.println();
        System.out.println();
    }

    // ------------------------------------------------------------------ C
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void sectionC() throws Exception {
        System.out.println("========== C · 往 List<String> 里塞 Integer ==========");
        List<String> list = new ArrayList<>();
        list.add("hello");

        List raw = list;          // 原始类型：编译器不再检查
        raw.add(42);              // 运行时只要求是 Object
        System.out.println("塞完之后 size = " + list.size() + "，没有人在存的时候拦你");

        Object o = list.get(1);   // 用 Object 接，也不炸
        System.out.println("用 Object 接住： " + o + "，类型是 " + o.getClass().getName());

        try {
            String s = list.get(1);   // 编译器在这里插了 checkcast java/lang/String
            System.out.println("居然拿到了 " + s);
        } catch (ClassCastException e) {
            StackTraceElement at = e.getStackTrace()[0];
            System.out.println("ClassCastException 抛在 " + at.getFileName() + ":" + at.getLineNumber()
                    + "  —— 就是 String s = list.get(1) 这一行");
            System.out.println("  " + e.getMessage());
        }

        // 反射同样绕过编译期检查
        List<String> list2 = new ArrayList<>();
        Method add = List.class.getMethod("add", Object.class);
        add.invoke(list2, 123);
        // 注意：这里必须用 List<?> 接出来。写成 list2.get(0).getClass() 会再吃一次 CCE ——
        // 因为编译器在 list2.get(0) 后面同样插了 checkcast String。
        Object reflected = ((List<?>) list2).get(0);
        System.out.println("反射 add 之后 list2 = " + list2 + "，元素运行时类型 "
                + reflected.getClass().getName());

        System.out.println("结论：擦除把类型检查从「运行时每个元素」搬到了「编译期每个表达式」，");
        System.out.println("      所以错误不会在写入时报，而是在「按 String 读出来」的那一刻才报 —— 报错位置和出错原因隔了好几层。");
        System.out.println();
    }

    // ------------------------------------------------------------------ D
    @SuppressWarnings("unchecked")
    private static <T> T[] newArrayByCast(int n) {
        // 擦除后这行就是 (Object[]) new Object[n]，T 在这里没有任何信息可用
        return (T[]) new Object[n];
    }

    /** JDK 自己的兜底方案：把 Class<T> 当参数传进来，运行时就真能造出 T[]。 */
    @SuppressWarnings("unchecked")
    private static <T> T[] newArrayByClass(Class<T> type, int n) {
        return (T[]) Array.newInstance(type, n);
    }

    private static void sectionD() {
        System.out.println("========== D · 为什么不能 new T[] ==========");

        Object[] asObject = newArrayByCast(3);
        asObject[0] = "ok";
        asObject[1] = 42;         // 这个数组运行时是 Object[]，什么都能放
        System.out.println("(T[]) new Object[3] 的真实类型 = " + asObject.getClass().getName()
                + "，放 Integer 也没事");

        try {
            String[] asString = newArrayByCast(3);   // 调用处把 (T[]) 还原成 (String[])
            asString[0] = "boom";
            System.out.println("居然成功了？");
        } catch (ClassCastException e) {
            System.out.println("赋给 String[] 时炸了： " + e.getMessage());
            System.out.println("  说明 (T[]) new Object[n] 只是骗过了编译器，数组的真实组件类型还是 Object");
        }

        String[] real = newArrayByClass(String.class, 3);
        real[0] = "a";
        real[1] = "b";
        System.out.println("Array.newInstance(String.class, 3) 的真实类型 = " + real.getClass().getName());
        try {
            Object[] sneaky = real;
            sneaky[2] = 99;      // 数组协变 + 运行时组件类型检查
        } catch (ArrayStoreException e) {
            System.out.println("往 String[] 里塞 Integer -> ArrayStoreException（数组的元素类型检查是运行时的）");
        }
        System.out.println("结论：数组在运行时必须知道自己的组件类型，而擦除后的 T 什么都不剩，");
        System.out.println("      所以 new T[n] 编译不过；要造真数组，只能把 Class<T> 显式传进来。");
        System.out.println();
    }

    // ------------------------------------------------------------------ E
    private static void sectionE() {
        System.out.println("========== E · 桥方法（bridge method）==========");
        System.out.println("StringCmp 声明的全部方法：");
        for (Method m : StringCmp.class.getDeclaredMethods()) {
            System.out.printf("  %-45s bridge=%-5s synthetic=%-5s%n",
                    m.getName() + "(" + params(m) + ")", m.isBridge(), m.isSynthetic());
        }
        System.out.println("原因：Comparator 接口擦除后是 compare(Object, Object)，");
        System.out.println("      实现类写的是 compare(String, String)，两个签名对不上；");
        System.out.println("      编译器就补一个 compare(Object,Object) 做 checkcast 再转调 —— 这就是桥方法。");
        System.out.println("      所以反射数方法时会多出一个「你没写过」的方法，框架里遍历 method 要留意 isBridge()。");
        System.out.println();
    }

    private static String params(Method m) {
        StringBuilder sb = new StringBuilder();
        Class<?>[] ps = m.getParameterTypes();
        for (int i = 0; i < ps.length; i++) {
            sb.append(i == 0 ? "" : ", ").append(ps[i].getSimpleName());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ F
    private static void sectionF() {
        System.out.println("========== F · 擦除的四条硬限制（编译期就挡） ==========");
        System.out.println("  ① new T[n] / new List<String>[n]   -> generic array creation");
        System.out.println("  ② catch (T e) / new T() / T.class  -> 运行时不知道 T 是什么");
        System.out.println("  ③ void f(List<String>) + void f(List<Integer>) -> name clash: same erasure");
        System.out.println("  ④ static 成员不能用类的类型参数      -> 静态上下文里没有 this，也就没有实参");
        System.out.println("  真报错原文见 day1-result.txt 第 3/4 段（GenericLimits + GenericLimits2）");
        System.out.println();
        System.out.println("========== 收尾 ==========");
        System.out.println("一句话：擦除擦掉的是「实参」，留下的是「声明」，代价是运行时无法复原类型。");
    }
}
