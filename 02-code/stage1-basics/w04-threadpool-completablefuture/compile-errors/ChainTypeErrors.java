package com.newt.java48.w04.day2;

import java.util.concurrent.CompletableFuture;

/**
 * Deliberately broken file. NOT compiled with the project.
 *
 * Purpose: prove that thenApply vs thenCompose is a STATIC TYPE issue,
 * caught by javac -- not something you discover at runtime.
 *
 * Compile alone:
 *   javac -encoding UTF-8 -d /tmp/ce compile-errors/ChainTypeErrors.java
 *
 * Comments are in English on purpose: javac echoes the offending source line,
 * and Chinese text turns into GBK garbage in a Git Bash console.
 */
public class ChainTypeErrors {

    static CompletableFuture<String> orderOf(String user) {
        return CompletableFuture.completedFuture(user + "-order");
    }

    // ERROR 1: thenApply returns a CF, so the value is nested one level too deep.
    // error: incompatible types: inference variable U has incompatible bounds
    //   equality constraints: String
    //   lower bounds: CompletableFuture<String>
    // (note: javac says "inference variable" rather than "cannot be converted" --
    //  the target type String fixes U, the lambda body demands CF<String>)
    static CompletableFuture<String> wrong1() {
        return CompletableFuture
                .supplyAsync(() -> "user")
                .thenApply(u -> orderOf(u));
    }

    // ERROR 2: after the nesting, 'join()' yields a CompletableFuture, not a String,
    // so there is no length() to call.
    // cannot find symbol: method length()
    static int wrong2() {
        return CompletableFuture
                .supplyAsync(() -> "user")
                .thenApply(u -> orderOf(u))
                .join()
                .length();
    }

    // ERROR 3: the mirror image -- thenCompose expects a CompletionStage,
    // but the lambda returns a plain String.
    // incompatible types: bad return type in lambda expression
    static CompletableFuture<String> wrong3() {
        return CompletableFuture
                .supplyAsync(() -> "user")
                .thenCompose(u -> u + "-order");
    }

    // Correct version: thenCompose flattens it, one join() gives the value.
    static CompletableFuture<String> right() {
        return CompletableFuture
                .supplyAsync(() -> "user")
                .thenCompose(u -> orderOf(u));
    }
}
