package com.newt.java48.w01.day1;

import java.util.Arrays;

/**
 * 手写 ArrayList（Day 1 版）—— 目的不是替代 JDK，而是把"扩容现场"打出来。
 *
 * 跑一次你会亲眼看到 JDK 里的三件事：
 *   1. 构造时不分配数组（懒加载）。JDK 里 elementData 先指向
 *      DEFAULTCAPACITY_EMPTY_ELEMENTDATA，第一次 add 才真正分配 10 个槽位。
 *   2. 扩容公式：newCapacity = oldCapacity + (oldCapacity >> 1)，也就是 1.5 倍。
 *      所以 10 -> 15 -> 22 -> 33 -> 49 -> 73 -> 109 ...
 *   3. 扩容 = 申请新数组 + Arrays.copyOf 全量拷贝，是 O(n)。
 *      所以如果你能预知元素个数，new ArrayList<>(n) 直接省掉几次拷贝。
 */
public class MyArrayList<E> {

    /** JDK 里叫 DEFAULT_CAPACITY */
    private static final int DEFAULT_CAPACITY = 10;

    /** 真正存数据的地方。注意：它和 size 是两回事 */
    private Object[] elementData;

    /** 元素个数，不是数组长度 */
    private int size;

    /** 只用来统计"一共扩容了几次" */
    static int growCount = 0;

    public MyArrayList() {
        // 故意不立刻分配 10 个位置 —— JDK 也是这么干的，叫"懒加载"
        this.elementData = new Object[0];
        System.out.println("[new MyArrayList()]  elementData.length = " + elementData.length
                + "   <-- 注意：还没分配 10 个位置");
    }

    public MyArrayList(int initialCapacity) {
        this.elementData = new Object[Math.max(initialCapacity, 0)];
        System.out.println("[new MyArrayList(" + initialCapacity + ")]  elementData.length = "
                + elementData.length + "   <-- 一步到位，后面不用扩容");
    }

    public boolean add(E e) {
        ensureCapacity(size + 1);
        elementData[size++] = e;
        return true;
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        checkIndex(index);
        return (E) elementData[index];
    }

    /** 中间删除：要 System.arraycopy 把后面的元素整体左移，O(n) */
    @SuppressWarnings("unchecked")
    public E remove(int index) {
        checkIndex(index);
        E old = (E) elementData[index];
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(elementData, index + 1, elementData, index, moved);
        }
        elementData[--size] = null;   // 置 null，别让数组一直引用着已经被删的元素（否则 GC 回收不掉）
        return old;
    }

    public int size() {
        return size;
    }

    /** 当前容量。size 和 capacity 的区别就是这一周的面试高频点之一 */
    public int capacity() {
        return elementData.length;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > elementData.length) {
            grow(minCapacity);
        }
    }

    private void grow(int minCapacity) {
        int oldCapacity = elementData.length;
        int newCapacity;
        if (oldCapacity == 0) {
            // 第一次 add：JDK 会跳到一个"最小可用容量" DEFAULT_CAPACITY = 10
            newCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        } else {
            // 关键一行：1.5 倍，不是 2 倍
            newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
        }
        growCount++;
        System.out.printf("  [grow #%d]  %d -> %-4d (size=%d, need=%d)%n",
                growCount, oldCapacity, newCapacity, size, minCapacity);
        elementData = Arrays.copyOf(elementData, newCapacity);
    }

    public static void main(String[] args) {
        System.out.println("========== 实验 A：不预设容量，add 100 个元素 ==========");
        MyArrayList<Integer> list = new MyArrayList<>();
        for (int i = 1; i <= 100; i++) {
            list.add(i);
        }
        System.out.println();
        System.out.printf("add 100 个之后:  size=%d, capacity=%d, 扩容次数=%d%n",
                list.size(), list.capacity(), growCount);
        System.out.println("对照 JDK: Arrays.copyOf 后容量是 109，扩容 7 次");
        System.out.println("(10 -> 15 -> 22 -> 33 -> 49 -> 73 -> 109)");
        System.out.println("结论：100 个元素，多申请了 9 个空位，累计多拷了 202 次引用");

        System.out.println();
        System.out.println("========== 实验 B：删除后不缩容 ==========");
        int before = list.capacity();
        for (int i = 0; i < 95; i++) {
            list.remove(0);
        }
        System.out.printf("remove 95 个之后:  size=%d, capacity=%d (删前 %d)   <-- 不缩容%n",
                list.size(), list.capacity(), before);
        System.out.println("为什么不缩容？因为缩容又是一次 O(n) 拷贝，而删完元素往往会接着加回来。");

        System.out.println();
        System.out.println("========== 实验 C：预设容量，同样 add 100 个 ==========");
        growCount = 0;
        MyArrayList<Integer> preset = new MyArrayList<>(100);
        for (int i = 1; i <= 100; i++) {
            preset.add(i);
        }
        System.out.printf("size=%d, capacity=%d, 扩容次数=%d   <-- 一次都没扩%n",
                preset.size(), preset.capacity(), growCount);
        System.out.println("这就是「知道大概多少个元素，就顺手写下初始容量」的价值。");

        System.out.println();
        System.out.println("========== 实验 D：1.5 倍 vs 2 倍 ==========");
        System.out.println("改成 2 倍时：10 -> 20 -> 40 -> 80 -> 160，4 次扩容就超过 100 个元素");
        System.out.println("那为什么 JDK 选 1.5 倍？");
        System.out.println("  2 倍时：8 -> 16 -> 32，每次释放的旧数组（8、16）永远拼不出下一次要的 32，(1+2+4+8) 始终差 1");
        System.out.println("  1.5 倍时：10 -> 15 -> 22 -> 33，前几次释放的空间有机会凑成后面要的大小，内存复用率更高");
        System.out.println("  另外 1.5 倍摊还下来的单次 add 成本更平稳，不会出现特别长的停顿");
    }

    /*
     * ===== 自己动手（可选，10~20 分钟，写完这一周你就真的懂了）=====
     *
     * 1. 补一个 add(int index, E e)：
     *    先 ensureCapacity(size+1)，再 System.arraycopy(elementData, index, elementData, index+1, size-index)。
     *    重点想清楚 index == size 的边界情况（等于追加）。
     *
     * 2. 把 grow() 里的 1.5 倍改成 2 倍，跑一遍实验 A，
     *    数一数 100 个元素各扩容几次、总共拷贝多少次引用。然后自己用一句话解释 JDK 选 1.5 的理由。
     *
     * 3. 为什么 JDK 不用 int[] 存数据？
     *    提示：泛型擦除后 E 会被当成 Object，基本类型不能作为类型参数；
     *    而且如果为了 int 再单独写一个 IntArrayList，就要为 8 种基本类型各写一份。
     *    （JDK 后来确实为此单独提供了 int[]、long[]、double[] 三个特化实现，就在 java.util 里，
     *      名字叫 IntStream、LongStream、DoubleStream 的底层数组 —— 自己去翻一下。）
     */
}
