package com.newt.java48.w05.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 脚手架：数源码里某个字符串出现几次。
 *
 * <p>为什么费劲数源码？因为「换一个实现要动三处」是个可以被证伪的断言 ——
 * 与其在笔记里写「大概三处」，不如让程序当场把数字打出来。
 * 文件路径相对工程根目录（run.sh 会 cd 到根目录）。
 */
public final class Src {

    private Src() {
    }

    public static String text(String relative) {
        try {
            return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    public static String between(String src, String begin, String end) {
        int i = src.indexOf(begin);
        int j = src.indexOf(end);
        if (i < 0 || j < 0 || j <= i) {
            return "";
        }
        return src.substring(i, j);
    }

    public static int count(String src, String needle) {
        int n = 0;
        int i = 0;
        while ((i = src.indexOf(needle, i)) >= 0) {
            n++;
            i += needle.length();
        }
        return n;
    }

    /**
     * 数「new Xxx(」出现几次。
     *
     * <p>针脚在这里拼出来而不是由调用方传字面量：否则被数的文件里一旦出现
     * {@code "new Xxx("} 这个字符串常量（比如统计代码自己），就会把自己数进去。
     * 第一版就是这么错的 —— SpringWiring 明明一个 new 都没有，却数出 3。
     */
    public static int countNew(String src, String simpleClassName) {
        return count(src, "new " + simpleClassName + "(");
    }
}
