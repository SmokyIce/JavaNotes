package com.newt.java48.w01.day1;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Day 1：比较动态数组和双向链表的操作耗时、近似内存占用。
 *
 * 阅读顺序：main → compare 开头的实验 → 被测操作 → 计时与内存工具。
 * 运行方式见本实验工程根目录的 README.md。
 *
 * 这是观察数量级的教学实验。两种集合的部分测试规模不同，
 * 单次计时会受 JIT、GC 和运行环境影响，不能把倍率当成固定结论。
 */
public class ArrayListVsLinkedList {

    // 顺序操作用较大规模；涉及反复移动或查找的操作缩小规模，避免等待太久。
    static final int SEQUENTIAL_ELEMENT_COUNT = 1_000_000;
    static final int INSERT_REMOVE_COUNT = 20_000;
    static final int INDEXED_ELEMENT_COUNT = 20_000;

    // 保存求和结果，降低被 JIT 当作无用计算消除的风险。
    static volatile long consumedSum = 0;

    public static void main(String[] args) {
        warmUp();

        System.out.println("=== Day 1 Lab : ArrayList vs LinkedList ===");
        System.out.println("java.version = " + System.getProperty("java.version")
                + "   maxHeap = " + (Runtime.getRuntime().maxMemory() >> 20) + " MB");
        System.out.println("N_SEQ = " + SEQUENTIAL_ELEMENT_COUNT
                + "   N_SHIFT = " + INSERT_REMOVE_COUNT + "   N_GET = " + INDEXED_ELEMENT_COUNT);
        System.out.println();
        System.out.printf("%-28s %14s %12s %12s %9s%n",
                "operation", "scale (AL/LL)", "ArrayList", "LinkedList", "LL/AL");
        System.out.printf("%-28s %14s %12s %12s %9s%n", "", "", "ns/op", "ns/op", "x");
        printSeparator();
        compareAppend();
        compareScatteredAccess();
        compareIndexedTraversal();
        compareIteratorTraversal();
        compareHeadInsertion();
        compareMiddleInsertion();
        compareHeadRemoval();

        System.out.println();
        compareMemoryUsage();
        System.out.println();
        System.out.println("读一遍上面的数字，然后回答：为什么 LinkedList 连 append 都赢不了？");
        System.out.println("hint: new Node() 的成本 + 内存不连续 + CPU cache line (64B) 的命中率");
    }

    private static void compareAppend() {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();
        compareOperations("add(E)   append", formatScale(SEQUENTIAL_ELEMENT_COUNT, SEQUENTIAL_ELEMENT_COUNT),
                () -> append(arrayList, SEQUENTIAL_ELEMENT_COUNT), SEQUENTIAL_ELEMENT_COUNT,
                () -> append(linkedList, SEQUENTIAL_ELEMENT_COUNT), SEQUENTIAL_ELEMENT_COUNT);
    }

    private static void compareScatteredAccess() {
        List<Integer> arrayList = new ArrayList<>(createSequentialValues(SEQUENTIAL_ELEMENT_COUNT));
        List<Integer> linkedList = new LinkedList<>(createSequentialValues(INDEXED_ELEMENT_COUNT));
        compareOperations("get(i)   random access", formatScale(SEQUENTIAL_ELEMENT_COUNT, INDEXED_ELEMENT_COUNT),
                () -> sumByScatteredIndex(arrayList, SEQUENTIAL_ELEMENT_COUNT), SEQUENTIAL_ELEMENT_COUNT,
                () -> sumByScatteredIndex(linkedList, INDEXED_ELEMENT_COUNT), INDEXED_ELEMENT_COUNT);
    }

    private static void compareIndexedTraversal() {
        List<Integer> arrayList = new ArrayList<>(createSequentialValues(SEQUENTIAL_ELEMENT_COUNT));
        List<Integer> linkedList = new LinkedList<>(createSequentialValues(INDEXED_ELEMENT_COUNT));
        compareOperations("for-i    indexed walk", formatScale(SEQUENTIAL_ELEMENT_COUNT, INDEXED_ELEMENT_COUNT),
                () -> sumByIndex(arrayList, SEQUENTIAL_ELEMENT_COUNT), SEQUENTIAL_ELEMENT_COUNT,
                () -> sumByIndex(linkedList, INDEXED_ELEMENT_COUNT), INDEXED_ELEMENT_COUNT);
    }

    private static void compareIteratorTraversal() {
        List<Integer> arrayList = new ArrayList<>(createSequentialValues(SEQUENTIAL_ELEMENT_COUNT));
        List<Integer> linkedList = new LinkedList<>(createSequentialValues(SEQUENTIAL_ELEMENT_COUNT));
        compareOperations("iterator walk + sum", formatScale(SEQUENTIAL_ELEMENT_COUNT, SEQUENTIAL_ELEMENT_COUNT),
                () -> sumByIterator(arrayList), SEQUENTIAL_ELEMENT_COUNT,
                () -> sumByIterator(linkedList), SEQUENTIAL_ELEMENT_COUNT);
    }

    private static void compareHeadInsertion() {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();
        compareOperations("add(0,E) insert head", formatScale(INSERT_REMOVE_COUNT, INSERT_REMOVE_COUNT),
                () -> insertHead(arrayList, INSERT_REMOVE_COUNT), INSERT_REMOVE_COUNT,
                () -> insertHead(linkedList, INSERT_REMOVE_COUNT), INSERT_REMOVE_COUNT);
    }

    private static void compareMiddleInsertion() {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();
        compareOperations("add(mid,E) insert middle", formatScale(INSERT_REMOVE_COUNT, INSERT_REMOVE_COUNT),
                () -> insertMiddle(arrayList, INSERT_REMOVE_COUNT), INSERT_REMOVE_COUNT,
                () -> insertMiddle(linkedList, INSERT_REMOVE_COUNT), INSERT_REMOVE_COUNT);
    }

    private static void compareHeadRemoval() {
        List<Integer> arrayList = new ArrayList<>(createSequentialValues(INSERT_REMOVE_COUNT));
        List<Integer> linkedList = new LinkedList<>(createSequentialValues(INSERT_REMOVE_COUNT));
        compareOperations("remove(0) remove head", formatScale(INSERT_REMOVE_COUNT, INSERT_REMOVE_COUNT),
                () -> removeHead(arrayList, INSERT_REMOVE_COUNT), INSERT_REMOVE_COUNT,
                () -> removeHead(linkedList, INSERT_REMOVE_COUNT), INSERT_REMOVE_COUNT);
    }

    // ================= 被测操作 =================

    static void append(List<Integer> list, int elementCount) {
        for (int i = 0; i < elementCount; i++) {
            list.add(i);
        }
    }

    static void sumByScatteredIndex(List<Integer> list, int elementCount) {
        long sum = 0;
        for (int i = 0; i < elementCount; i++) {
            // 固定公式生成分散的下标，让每次运行的访问顺序一致。
            int index = (int) (((i * 2654435761L) >>> 3) % elementCount);
            sum += list.get(index);
        }
        consumedSum = sum;
    }

    static void sumByIndex(List<Integer> list, int elementCount) {
        long sum = 0;
        for (int i = 0; i < elementCount; i++) {
            sum += list.get(i);
        }
        consumedSum = sum;
    }

    static void sumByIterator(List<Integer> list) {
        long sum = 0;
        for (Integer value : list) {
            sum += value;
        }
        consumedSum = sum;
    }

    static void insertHead(List<Integer> list, int elementCount) {
        for (int i = 0; i < elementCount; i++) {
            list.add(0, i);
        }
    }

    static void insertMiddle(List<Integer> list, int elementCount) {
        for (int i = 0; i < elementCount; i++) {
            list.add(list.size() / 2, i);
        }
    }

    static void removeHead(List<Integer> list, int elementCount) {
        for (int i = 0; i < elementCount; i++) {
            list.remove(0);
        }
    }

    // ================= 工具 =================

    static void compareOperations(String name, String scaleLabel,
                                  Runnable arrayListTask, long arrayListOperationCount,
                                  Runnable linkedListTask, long linkedListOperationCount) {
        long arrayListNanos = measureNanos(arrayListTask);
        long linkedListNanos = measureNanos(linkedListTask);
        double arrayListNanosPerOperation = (double) arrayListNanos / arrayListOperationCount;
        double linkedListNanosPerOperation = (double) linkedListNanos / linkedListOperationCount;
        System.out.printf("%-28s %14s %12.2f %12.2f %8.2fx%n", name, scaleLabel,
                arrayListNanosPerOperation, linkedListNanosPerOperation,
                linkedListNanosPerOperation / arrayListNanosPerOperation);
    }

    static long measureNanos(Runnable task) {
        long startNanos = System.nanoTime();
        task.run();
        return System.nanoTime() - startNanos;
    }

    static void warmUp() {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();
        for (int round = 0; round < 3; round++) {
            append(arrayList, 20_000);
            append(linkedList, 20_000);
            sumByScatteredIndex(arrayList, 20_000);
            sumByScatteredIndex(linkedList, 2_000);
            sumByIndex(arrayList, 20_000);
            sumByIterator(linkedList);
            insertHead(arrayList, 2_000);
            insertHead(linkedList, 2_000);
            removeHead(arrayList, 2_000);
            removeHead(linkedList, 2_000);
            arrayList.clear();
            linkedList.clear();
        }
    }

    static List<Integer> createSequentialValues(int elementCount) {
        List<Integer> list = new ArrayList<>(elementCount);
        for (int i = 0; i < elementCount; i++) {
            list.add(i);
        }
        return list;
    }

    static String formatScale(int arrayListCount, int linkedListCount) {
        return formatCount(arrayListCount) + " / " + formatCount(linkedListCount);
    }

    static String formatCount(int value) {
        if (value >= 1_000_000) {
            return (value / 1_000_000) + "M";
        }
        if (value >= 1_000) {
            return (value / 1_000) + "k";
        }
        return String.valueOf(value);
    }

    static void printSeparator() {
        for (int i = 0; i < 80; i++) {
            System.out.print('-');
        }
        System.out.println();
    }

    // ================= 内存占用 =================

    // 两种集合共享同一批 Integer，尽量排除元素本身占用的内存。
    static void compareMemoryUsage() {
        int elementCount = 1_000_000;
        Integer[] sharedValues = new Integer[elementCount];
        for (int i = 0; i < elementCount; i++) {
            sharedValues[i] = i;
        }

        requestGarbageCollection();
        long heapBeforeArrayList = usedHeapBytes();
        List<Integer> arrayList = new ArrayList<>(elementCount);
        for (Integer value : sharedValues) {
            arrayList.add(value);
        }
        requestGarbageCollection();
        long arrayListExtraBytes = usedHeapBytes() - heapBeforeArrayList;

        requestGarbageCollection();
        long heapBeforeLinkedList = usedHeapBytes();
        List<Integer> linkedList = new LinkedList<>();
        for (Integer value : sharedValues) {
            linkedList.add(value);
        }
        requestGarbageCollection();
        long linkedListExtraBytes = usedHeapBytes() - heapBeforeLinkedList;

        System.out.println("--- memory footprint (1,000,000 same Integer objects, approx) ---");
        System.out.printf("ArrayList   extra heap: %7.2f MB   (1 x Object[] with 1M slots)%n",
                arrayListExtraBytes / 1024.0 / 1024.0);
        System.out.printf("LinkedList  extra heap: %7.2f MB   (1M x Node objects, each {prev,item,next})%n",
                linkedListExtraBytes / 1024.0 / 1024.0);
        System.out.printf("LinkedList costs about %.1fx more memory%n",
                linkedListExtraBytes == 0 ? 0.0 : (double) linkedListExtraBytes / Math.max(arrayListExtraBytes, 1));
        System.out.println("(keep alive to avoid dead-code elimination: " + (arrayList.size() + linkedList.size()) + ")");
    }

    static long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    static void requestGarbageCollection() {
        for (int i = 0; i < 2; i++) {
            System.gc();
            try {
                Thread.sleep(120);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
