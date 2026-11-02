import java.util.function.IntUnaryOperator;

/**
 * Deliberately does not compile -- only used to capture javac's original messages.
 * Split off from LambdaLimits.java on purpose: the "effectively final" check lives in
 * javac's FLOW analysis, which never runs if attribution already failed. Keeping it in
 * the same file made the error silently disappear next to five attribution errors.
 *
 * <pre>
 * Reproduce:
 *   javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors LambdaLimits2.java
 * </pre>
 *
 * <p>Three cases, all flow-analysis / lambda-body rejections:
 * <ol>
 *   <li>a captured local variable that is reassigned later</li>
 *   <li>{@code break} inside a lambda that tries to leave the outer loop</li>
 *   <li>{@code return} with a value inside a lambda whose target type is void</li>
 * </ol>
 *
 * <p>Comments are kept ASCII on purpose (javac echoes the offending source line).
 */
public class LambdaLimits2 {

    // ------------------------------------------------------------------ 1
    // expected: local variables referenced from a lambda expression must be final
    //           or effectively final
    static IntUnaryOperator notEffectivelyFinal() {
        int base = 10;
        IntUnaryOperator f = x -> x + base;
        base = 20;                        // second assignment: base is no longer effectively final
        return f;
    }

    // ------------------------------------------------------------------ 2
    // expected: break outside switch or loop
    static void breakOuterLoop() {
        for (int i = 0; i < 3; i++) {
            Runnable r = () -> {
                break;                    // a lambda body is not a loop: cannot break the outer for
            };
            r.run();
        }
    }

    // ------------------------------------------------------------------ 3
    // expected: incompatible types: unexpected return value
    static void valueReturnInVoidLambda() {
        Runnable r = () -> {
            return 42;                    // Runnable's run() is void
        };
        r.run();
    }
}
