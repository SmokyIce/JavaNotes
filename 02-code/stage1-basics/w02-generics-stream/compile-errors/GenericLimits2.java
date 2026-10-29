import java.util.ArrayList;
import java.util.List;

/**
 * Deliberately does not compile -- only used to capture javac's original messages.
 * Not under src/main/java, so run.bat never compiles it.
 *
 * <p>Leftover from Day 1: the PECS cases were only documented with a comment
 * ({@code // wild.add(4);}) and never had real compiler output. This file collects
 * six wildcard-related compile-time rejections in one go.
 *
 * <pre>
 * Reproduce (must pass -J-Duser.language=en, or "same erasure" is translated
 * into Chinese and becomes unsearchable):
 *   javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors GenericLimits2.java
 * </pre>
 *
 * <p>Six cases:
 * <ol>
 *   <li>invariance: {@code List<Object> = List<String>} is rejected</li>
 *   <li>producer side: {@code ? extends} is read-only</li>
 *   <li>consumer side: {@code ? super} is write-only, read-back is just Object</li>
 *   <li>unbounded {@code List<?>}: cannot even add</li>
 *   <li>overloading on wildcards: both erase to {@code f(List)}, name clash</li>
 *   <li>wildcards are not allowed in {@code new}</li>
 * </ol>
 *
 * <p>Comments are kept ASCII on purpose: javac echoes the offending source line into
 * the output, and a Chinese comment there turns into GBK garbage in the snapshot.
 */
public class GenericLimits2 {

    // ------------------------------------------------------------------ 1
    // expected: incompatible types: List<String> cannot be converted to List<Object>
    void invariance(List<String> strs) {
        List<Object> objs = strs;
    }

    // ------------------------------------------------------------------ 2
    // expected: incompatible types: int cannot be converted to CAP#1
    //           where CAP#1 is a fresh type-variable:
    //             CAP#1 extends Number from capture of ? extends Number
    // add(null) in the same block is legal on purpose -- the control group.
    void producerOnly(List<? extends Number> src) {
        Number first = src.get(0);   // read: fine, upper bound is Number
        src.add(null);               // write null: fine, the only value valid for every subtype
        src.add(4);                  // write a real value: rejected
    }

    // ------------------------------------------------------------------ 3
    // expected: incompatible types: CAP#1 cannot be converted to Integer
    void consumerOnly(List<? super Integer> dst) {
        dst.add(4);                  // write Integer: fine
        Integer got = dst.get(0);    // read back as Integer: rejected, only Object is safe
    }

    // ------------------------------------------------------------------ 4
    // expected: incompatible types: String cannot be converted to CAP#1
    //           where CAP#1 is a fresh type-variable:
    //             CAP#1 extends Object from capture of ?
    void unbounded(List<?> any) {
        Object o = any.get(0);       // read: fine
        any.add("x");                // write: rejected, nobody knows what the list holds
    }

    // ------------------------------------------------------------------ 5
    // expected: name clash: f(List<? super Number>) and f(List<? extends Number>)
    //           have the same erasure, yet neither overrides the other
    void f(List<? extends Number> a) {
    }

    void f(List<? super Number> b) {
    }

    // ------------------------------------------------------------------ 6
    // expected: unexpected type / required: class or interface without bounds / found: ?
    List<?> wildcardNew() {
        return new ArrayList<?>();
    }
}
