import java.util.function.IntUnaryOperator;

/**
 * Deliberately does not compile -- only used to capture javac's original message.
 *
 * <p>This file exists because of a genuine surprise: the "effectively final" check is
 * the ONLY thing in it, and it still needed its own file.
 *
 * <p>javac runs attribution first, then FLOW analysis. The effectively-final rule is
 * computed during flow analysis, so as soon as a file contains any attribution error
 * (a lambda with no target type, a break inside a lambda, an ambiguous overload...),
 * flow analysis is skipped and this error never gets reported. Put this case together
 * with any of the errors in LambdaLimits.java / LambdaLimits2.java and it silently
 * vanishes -- six intended errors show up as five.
 *
 * <pre>
 * Reproduce:
 *   javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors LambdaLimits3.java
 * </pre>
 *
 * <p>Expected: local variables referenced from a lambda expression must be final
 * or effectively final
 *
 * <p>Comments are kept ASCII on purpose (javac echoes the offending source line).
 */
public class LambdaLimits3 {

    static IntUnaryOperator notEffectivelyFinal() {
        int base = 10;
        IntUnaryOperator f = x -> x + base;
        base = 20;                        // second assignment: base is no longer effectively final
        return f;
    }
}
