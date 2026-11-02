---
title: 今天做什么
date: 2026-11-02
weekday: 周一
milestone: M1 · Java 地基
task: W02 Day 4 — Lambda（函数式接口五形态 + 方法引用 + 变量捕获）
status: 已完成
---

# 今天 · 2026-11-02（周一）· 已完成

> 距上次（10-30）隔了两天，按里程碑制直接接着走，不补课。
> 开工第一步还是**回归编译 + 跑全部入口**（7 个），全通过，没有中断的代码 ——
> 唯一算「遗留」的是笔记里 Day 4 那三个待做项，也就是今天正题。

## 一、先查的遗留

| 检查项 | 结果 |
| :--- | :--- |
| 全量重编 + 跑 7 个入口（day1×3 / day2×3 / day3×2，其中 `ExceptionCost` 要特殊参数单独跑） | ✅ 全部 exit=0，无回归 |
| `01-notes` 里的未勾项 | 只剩 W02 的 Lambda / Stream / 练习，以及 W03 之后尚未开始的里程碑，无孤儿待办 |
| `02-code` 下的 `.gitkeep` 空目录 | 均为 W03–W48 尚未解锁的里程碑，不动 |

## 二、今天的正题：Lambda

代码在 `02-code/stage1-basics/w02-generics-stream/src/main/java/com/newt/java48/w02/day4/`：

| 文件 | 内容 |
| :--- | :--- |
| `FunctionalShape.java` | 六段：五种形状 / `Predicate` 短路 / `andThen` vs `compose` / 装箱逃逸 / 自定义接口 / 目标类型 |
| `LambdaCapture.java` | 六段：方法引用五连 / 值捕获 / 循环变量 / `this` 归属 / 实例复用 / `invokedynamic` |
| `compile-errors/LambdaLimits{,2,3}.java` | 8 条编译期报错原文（分三个文件，原因见第四节） |
| `day4-result.txt` / `day4-bytecode.txt` | 运行快照 + `javap -p/-c/-v` 三段字节码证据 |

## 三、今天挖到的四个点

1. **「捕获」在字节码里就是「当实参传进去」。**
   `int base = 10; f = x -> x + base;` 编译后 `javap -p` 能看到 `lambda$sectionB$1(int, int)` ——
   第一个 `int` 是 `x`，**第二个 `int` 就是 `base`**。所以 `effectively final` 不是语法洁癖：
   副本会分叉，分叉之后语义没法解释，干脆禁止第二次赋值。
   顺着这一条，另外两个反常现象也一起解释了 —— 字段能改（捕获的是 `this`，合成方法非 static）、
   `int[] box` 能绕过（拷的是引用，指向的数组在堆上共享）。

2. **`for` 不行、`for-each` 行，用数组绕过反而全错。**
   `for (int i...)` 的 `i` 是整个循环共用的一个变量还在被 `i++` 改，编译不过；
   `for-each` 的循环变量每次迭代是新变量，实测拿到 `[1, 2, 3]`；
   而 `int[] idx` 老写法实测是 `[3, 3, 3]` —— 三个 lambda 共享一个盒子。
   这个坑在「循环里注册回调 / 监听器」时是真实事故。

3. **「装箱很贵」只有一半对：贵的是逃逸的装箱。**
   同一个 `x + 1`，不逃逸（结果立刻拆箱求和）装箱版只慢 **2.0 倍**（10.0 vs 4.9 ms），
   因为标量替换把 `new Integer` 消掉了；一旦结果写进 `Integer[]` 逃逸，立刻变 **36 倍**（23.7 vs 0.7 ms）。
   所以判断标准是「会不会进集合 / 跨线程」，不是「有没有装箱」。

4. **lambda 不是匿名类的语法糖，两者的内存与类文件行为完全不同。**
   磁盘上 `day4/` 只有 6 个 `.class`，没有一个 `$$Lambda`；
   运行时 `getClass()` 是 `LambdaCapture$$Lambda$24/0x...`，`isHidden()` 为 **true**；
   `BootstrapMethods` 里引导方法是 `LambdaMetafactory.metafactory`，第三段参数指向那个 `lambda$xxx` 私有方法。
   顺带量到一个反直觉结论：**非捕获 lambda 复用单例**（`nonCapturing()==nonCapturing()` 为 true），
   捕获的每次新建（`capturing(1)==capturing(1)` 为 false）——
   所以「lambda 一定比匿名类多创建对象」是错的，但「别拿 lambda 当 Map key」是对的。

## 四、今天踩的坑

- **一个文件放 6 条 lambda 编译错误，javac 只报 5 条。** `effectively final` 那条凭空消失。
  原因是 javac 分阶段：attribution（类型 / 重载 / 目标类型）先跑，FLOW 分析后跑，
  `effectively final` 属于后者，attribution 一失败 flow 就整个跳过。
  拆成 `LambdaLimits`（5 条 attr）/ `LambdaLimits2`（break、return 值）/ `LambdaLimits3`（单独一条）才凑齐 8 条原文。
  这条经验能用一辈子：**「补上一个错又冒出来三个」不是编译器抽风，是阶段掩盖。**
- `Function.identity()` 放进 `Map<String, Function<Order, Object>>` 推不出类型 ——
  它没有入参，`T` 只能落到 `Object`。要写 `Function.<Order>identity()`。
- `sectionB` / `sectionD` 里用 `this.delta` 却写成 `static` 方法 → 报「无法从静态上下文引用非静态变量 this」。
  正好印证第 3 点：lambda 的 `this` 就是它所在方法的 `this`，方法静态则 lambda 也拿不到。

## 五、环境备忘

- 跑 day4：`run.bat day4.FunctionalShape` / `run.bat day4.LambdaCapture`
- 想看字节码：`javap -p -c -cp out com.newt.java48.w02.day4.LambdaCapture`
  （`javap` 不受 `-Dfile.encoding` 影响，常量池里的中文会是 GBK 乱码，不影响读方法句柄）
- 手动编译仍然要先 `find src/main/java -name "*.java" > sources.txt`

---

# 下一个时间盒（60 分钟）

**W02 Day 5：Stream** —— 重点不是 API 列表，而是「谁在什么时候执行」：

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述今天：捕获为什么是值拷贝 / 非捕获 lambda 为什么是单例 | 口头过，卡住回看笔记 Day 4 |
| 25 分钟 | 惰性求值：中间操作只是「记账」，终止操作才真跑；用打印顺序证明 | `day5/StreamLazy.java` |
| 15 分钟 | 短路：`findFirst` / `anyMatch` / `limit` 在无限流上能停下来；对照 `sorted` 必须消费全部 | 同上 |
| 10 分钟 | 一个流只能用一次（`IllegalStateException: stream has already been operated upon or closed`）+ `flatMap` vs `map` | 同上 + 笔记 Day 5 小节 |

> 优先做第二个 25 分钟。「中间操作返回新流、终止操作才执行」这句话人人会背，
> 但**能指着打印顺序说清哪一行在什么时候跑**的人不多 —— 这是 Stream 调试的唯一抓手：
> 线上 `stream()` 链路里某一步没执行，先判断它是不是中间操作。
