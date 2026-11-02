/**
 * Deliberately does not compile -- only used to capture javac's original messages.
 * Not under src/main/java, so run.bat never compiles it.
 *
 * <p>Day 4 leftover: "lambda needs a target type" was only a comment in the notes.
 * This file collects five lambda-related **attribution** errors in one go.
 * The "effectively final" case lives in LambdaLimits2.java -- see the note at the bottom.
 *
 * <pre>
 * Reproduce (English locale, otherwise the messages are translated):
 *   javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors LambdaLimits.java
 * </pre>
 *
 * <p>Five cases:
 * <ol>
 *   <li>{@code @FunctionalInterface} with two abstract methods -- this is how Java 8
 *       avoided breaking every lambda: new methods must be default</li>
 *   <li>{@code var} does not supply a target type, so a lambda cannot be inferred</li>
 *   <li>{@code Object} is not a functional interface, so a lambda cannot be assigned to it</li>
 *   <li>an interface whose only method is a redeclaration of {@code Object.equals}
 *       has zero abstract methods -- so no lambda can implement it</li>
 *   <li>overload resolution with two functional-interface parameters is ambiguous
 *       for an explicit lambda (an anonymous class would not be ambiguous)</li>
 * </ol>
 *
 * <p>Comments are kept ASCII on purpose: javac echoes the offending source line into
 * the output, and a Chinese comment there turns into GBK garbage in the snapshot.
 *
 * <p>Why the capture error is NOT here: javac compiles in phases, and the
 * "effectively final" check is part of the later FLOW analysis. Once attribution
 * fails on any of the five cases above, flow analysis is skipped and the capture
 * error never gets reported -- it silently disappears. Six errors in one file
 * therefore look like five. Keep such cases in separate files.
 */
public class LambdaLimits {

    // ------------------------------------------------------------------ 1
    // expected: Unexpected @FunctionalInterface annotation
    //             BadRule is not a functional interface
    //             multiple non-overriding abstract methods found in interface BadRule
    @FunctionalInterface
    interface BadRule {
        boolean test(int amount);

        boolean testVip(int amount);      // second abstract method: annotation now fails
    }

    // ------------------------------------------------------------------ 2
    // expected: cannot infer type for local variable f
    //             (lambda expression needs an explicit target-type)
    static void varWithLambda() {
        var f = x -> x + 1;
    }

    // ------------------------------------------------------------------ 3
    // expected: incompatible types: Object is not a functional interface
    static void assignToObject() {
        Object o = () -> System.out.println("hi");
    }

    // ------------------------------------------------------------------ 4
    // expected: incompatible types: BadEq is not a functional interface
    //             no abstract method found in interface BadEq
    interface BadEq {
        boolean equals(Object other);     // already a public method of Object: counts as zero
    }

    static void interfaceWithNoAbstractMethod() {
        BadEq eq = x -> true;
    }

    // ------------------------------------------------------------------ 5
    // expected: reference to f is ambiguous
    //             both method f(A) and method f(B) match
    interface A {
        int apply(int x);
    }

    interface B {
        int apply(int x);
    }

    static void f(A a) {
    }

    static void f(B b) {
    }

    static void ambiguousOverload() {
        f(x -> x + 1);                    // A or B? javac refuses to guess
    }
}
