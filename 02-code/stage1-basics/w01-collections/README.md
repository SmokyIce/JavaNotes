# W01 · Java 集合 · 实验工程

> 目标：**不只是会用 HashMap，要能解释它。**
>
> 本周笔记：[`../../../01-notes/stage1-basics/W01-Java集合.md`](../../../01-notes/stage1-basics/W01-Java集合.md)

---

## 怎么跑

本周不需要任何第三方依赖，纯 JDK 就能跑（本机已装 JDK 8 / 17 / 23 / 25，本工程按 **JDK 17** 编译验证）。

```bat
rem Windows：双击，或在 cmd 里执行
run.bat
run.bat MyArrayList
```

```bash
# Git Bash / macOS / Linux
./run.sh
./run.sh MyArrayList
```

如果只想手动来：

```bash
javac -encoding UTF-8 -d out src/main/java/com/newt/java48/w01/day1/*.java
java -cp out com.newt.java48.w01.day1.ArrayListVsLinkedList
```

> 目录已经按 **Maven 标准布局**（`src/main/java`）建好，等 W05 开始写 Spring 时，
> 直接补一个 `pom.xml` 就能升成 Maven 工程，不用挪文件。

## 目录

```text
w01-collections/
├── run.bat / run.sh                                一键编译 + 运行
├── day1-result.txt                                 Day 1 实测输出（本机快照）
└── src/main/java/com/newt/java48/w01/day1/
    ├── ArrayListVsLinkedList.java                  7 项操作的真实耗时对比 + 内存占用
    └── MyArrayList.java                            手写动态数组，打印扩容现场
```

---

## Day 1（ArrayList / LinkedList）实验清单

| # | 要验证的结论 | 命令 | 预期 |
| :-- | :--- | :--- | :--- |
| 1 | 按下标遍历链表是性能事故 | `run.bat` | `for-i indexed walk` 一行，LinkedList 慢 **上千倍** |
| 2 | 随机访问链表慢 | `run.bat` | `get(i) random access`，LinkedList 慢 **上百倍** |
| 3 | 尾部追加链表居然也不快 | `run.bat` | `add(E) append`，LinkedList 慢 **2~4 倍** |
| 4 | 用迭代器遍历时两者差不多 | `run.bat` | `iterator walk + sum`，比值接近 **1x** |
| 5 | 头部插入删除链表才真正赢 | `run.bat` | `add(0,E)` / `remove(0)` 两行，LinkedList 快 **十几~几十倍** |
| 6 | 中间插入链表又输了 | `run.bat` | `add(mid,E)`，因为**先要 O(n) 找到那个位置** |
| 7 | 同样 100 万元素，链表更费内存 | `run.bat` | 约 **5.7 倍**（Node 对象头 + prev/next 两个指针） |
| 8 | 扩容是 1.5 倍，不是 2 倍 | `run.bat MyArrayList` | `10 -> 15 -> 22 -> 33 -> 49 -> 73 -> 109` |

## 读代码的顺序

1. 先跑 `run.bat MyArrayList`，看扩容现场，读 `MyArrayList.grow()` —— 只有十来行，把这十行看懂，
   JDK `ArrayList` 的扩容你就懂了 80%。
2. 再跑 `run.bat`，对着结果表读 `ArrayListVsLinkedList.java` 里每个被测方法。
   注意 `warmUp()` 和 `sink`：一个是为了让 JIT 先编译好，一个是为了防止 JIT 把循环优化掉。
   **不写这两样，测出来的数字没有意义。**

---

## 本机 Day 1 实测快照（JDK 17.0.12）

```text
operation                     scale (AL/LL)    ArrayList   LinkedList     LL/AL
                                                   ns/op        ns/op         x
--------------------------------------------------------------------------------
add(E)   append                     1M / 1M        44.02       161.24     3.66x
get(i)   random access             1M / 20k        42.87      6990.08   163.04x
for-i    indexed walk              1M / 20k         2.97      6710.78  2262.57x
iterator walk + sum                 1M / 1M        10.60         9.83     0.93x
add(0,E) insert head              20k / 20k       995.54        56.28     0.06x
add(mid,E) insert middle          20k / 20k       487.29      7634.38    15.67x
remove(0) remove head             20k / 20k       894.96        24.89     0.03x

ArrayList   extra heap:    4.00 MB   (1 x Object[] with 1M slots)
LinkedList  extra heap:   22.91 MB   (1M x Node objects, each {prev,item,next})
```

> 每次跑数字都会有浮动（GC、JIT、CPU 频率），**要看的是数量级，不是小数点**。
> 自己在自己机器上再跑一遍，对比一下——如果某项和上表差了一个数量级，那是值得追下去的问题。
