import java.util.ArrayList;
import java.util.List;

/**
 * 故意编译不过的文件 —— 用来抄 javac 的原话。
 *
 * <p>它不在 src/main/java 下，不会被 run.bat 编译。单独跑：
 *
 * <pre>
 * javac -encoding UTF-8 -d ../out-errors GenericLimits.java
 * </pre>
 *
 * <p>六处错误，一一对应 W02 笔记里的「擦除的四条硬限制」。
 */
public class GenericLimits {

    static class Box<T> {
        // ① 静态上下文里没有类型实参
        static T shared;

        // ② 擦除后 T 没有组件类型信息，创建不出来
        T[] arr = new T[10];

        void handle() {
            // ③ 运行时不知道 T 是哪个异常类，catch 不了
            try {
                System.out.println("x");
            } catch (T e) {
                System.out.println(e);
            }
        }
    }

    // ④ 擦除后两个方法签名完全一样：f(List)
    void f(List<String> xs) {
    }

    void f(List<Integer> xs) {
    }

    // ⑤ 泛型数组创建（不是 new T[]，是 new 具体参数化类型的数组）
    List<String>[] gs = new ArrayList<String>[10];

    // ⑥ instanceof 后面不允许跟参数化类型
    boolean isStringList(Object o) {
        return o instanceof List<String>;
    }
}
