# W02 · 泛型 / 异常 / Optional / Stream · 实验工程

> 目标：**不是会用 `List<String>`，而是知道编译器替你做了什么、又在哪偷偷擦掉了。**
>
> 本周笔记：[`../../../01-notes/stage1-basics/W02-泛型-异常-Stream.md`](../../../01-notes/stage1-basics/W02-泛型-异常-Stream.md)

---

## 怎么跑

纯 JDK，无第三方依赖。本工程按 **JDK 17** 编译验证（本机另装 8 / 23 / 25，别用 8 跑，`var`、`List.of` 会挂）。

```bat
rem Windows
run.bat                  rem 默认跑 day1.ErasureProof
run.bat PecsDemo         rem 短名 -> day1 包
run.bat day2.XxxDemo     rem 带点号 -> 完整类名
```

```bash
# Git Bash / macOS / Linux
./run.sh
./run.sh PecsDemo
./run.sh day2.XxxDemo
```

> `run.bat` 规则和 W01 一致：**不带点号**当短名，默认去 `day1` 找；**带点号**当完整类名，
> 自动补前缀 `com.newt.java48.w02.`。

手动编译：

```bash
javac -encoding UTF-8 -d out @sources.txt
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w02.day1.ErasureProof
```

## 目录

```text
w02-generics-stream/
├── run.bat / run.sh                        一键编译 + 运行
├── day1-result.txt                         Day 1 实测输出（含 javac 报错原文）
├── day2-result.txt                         Day 2 实测输出（资源关闭 + 异常包装 + 异常开销）
├── day3-result.txt                         Day 3 实测输出（Optional 反模式 + 链式正例）
├── compile-errors/
│   ├── GenericLimits.java                  故意编译不过：擦除的四条硬限制
│   └── GenericLimits2.java                 故意编译不过：通配符的六种拒绝
└── src/main/java/com/newt/java48/w02/
    ├── day1/
    │   ├── ErasureProof.java               六个实验：类同一性 / 反射签名 / CCE 位置 / 泛型数组 / 桥方法 / 硬限制
    │   ├── PecsDemo.java                   五个实验：不变性 / ? extends / ? super / copy / Stack pushAll
    │   └── WildcardCapture.java            四个实验：捕获报错原文 / helper 捕获正例 / JDK 的 raw 选择 / raw 的代价
    ├── day2/
    │   ├── TryWithResourcesProbe.java      五个实验：逆序关闭 / suppressed / finally 里 return / finally 里 throw / 受检 close
    │   ├── ExceptionChainProbe.java        六个实验：三种包装 / 日志写法 / cause vs suppressed / 受检边界 / sneakyThrow / 何时包装
    │   └── ExceptionCost.java              异常开销：fillInStackTrace 的账单（跑两遍，只改一个 JVM 参数）
    └── day3/
        ├── OptionalAntiPatterns.java       五段：isPresent+get / orElse 白算 / 字段参数集合 / 五个陷阱 / 判定表
        └── OptionalChain.java              六段：null 检查对比 / map vs flatMap / filter / orElseGet vs orElseThrow / 消费侧 / 优惠券算价
```

> `compile-errors/` 不在 `src/main/java` 下，不会被 `run.bat` 编译 —— 否则整个工程编不过。
> 要单独复现报错：
>
> ```bash
> cd compile-errors
> javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors GenericLimits.java
> javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors GenericLimits2.java
> ```
>
> 加 `-J-Duser.language=en` 是因为中文 locale 下 `name clash: ... same erasure` 会被译成
> 「具有相同擦除」，搜不到任何资料 —— 报错原文比译文有用得多。
> `GenericLimits2` 的注释刻意写成英文：javac 会把出错那行源码原样回显，
> 中文注释在那里会变成 GBK 乱码，污染快照。

---

## Day 1（类型擦除 + PECS）实验清单

| #  | 要验证的结论 | 命令 | 实测 |
| :- | :--- | :--- | :--- |
| 1 | 泛型实参运行时不存在的直接证据 | `run.bat` | `ArrayList<String>` 与 `ArrayList<Integer>` 的 `getClass()` **同一个对象**，`==` 为 `true` |
| 2 | 连接口的实参也没了 | 同上 | `strings.getClass() == List.class` 是 **false**，只剩实现类 `java.util.ArrayList` |
| 3 | 反射看到的 `add` 是 `add(Object)` | 同上 | `getMethod("add", String.class)` → **NoSuchMethodException**；`getMethod("add", Object.class)` 拿到 `List.add(E)` |
| 4 | 擦的是实参，不是声明 | 同上 | `Box.class.getTypeParameters()[0]` 仍是 `T`；`Box.get` 的泛型返回类型是 `T` |
| 5 | 实参藏在 Signature 属性里 | 同上 | `StringBox.getGenericSuperclass()` = `Box<java.lang.String>`，能取出 `String` |
| 6 | 塞错类型不报在写，报在读 | 同上 | `raw.add(42)` 无声通过；`String s = list.get(1)` 抛 **CCE** |
| 7 | `(T[]) new Object[n]` 是骗局 | 同上 | 真实类型 `[Ljava.lang.Object;`；赋给 `String[]` 立刻 CCE |
| 8 | 要真数组必须传 `Class<T>` | 同上 | `Array.newInstance(String.class, 3)` → `[Ljava.lang.String;` |
| 9 | 编译器会补桥方法 | 同上 | `StringCmp` 反射出 **2 个** `compare`：`(String,String)` 与 `(Object,Object) bridge=true synthetic=true` |
| 10 | 擦除写出四条编译期硬限制 | `compile-errors/GenericLimits.java` | javac 一次报 **6 个错**，见 `day1-result.txt` 第 3 段 |
| 11 | 泛型是不变的 | `run.bat PecsDemo` | `List<Object> objs = strs;` 编译不过；数组却协变，只能在运行时抛 `ArrayStoreException` |
| 12 | `? extends` 只能读 | 同上 | `sum()` 一个方法吃 `Integer` / `Double` / `Long`；`wild.add(4)` 编译错，`add(null)` 可以 |
| 13 | `? super` 只能写 | 同上 | `fill()` 能填 `List<Object>` / `List<Number>` / `List<Integer>`；读出来静态类型只有 `Object` |
| 14 | `copy` 的两个通配符各管一端 | 同上 | `<T> void copy(List<? super T> dest, List<? extends T> src)` 能接受 `(List<Object>, List<String>)` |
| 15 | PECS 的真实用法 | 同上 | `Stack<Number>.pushAll(List<Integer>)` + `popAll(Collection<Object>)` 同时成立 |

**本节最值钱的三个点**

1. **CCE 的位置是"按类型读出来"那一行，不是 `add` 那一行。**
   编译器在每个 `get` 后面插 `checkcast`，所以错误现场和肇事现场隔了好几层 ——
   线上看到 `Integer cannot be cast to String` 时，先怀疑"谁用原始类型/反射塞进去的"，
   而不是去改那一行 `get`。
2. **反射有两个视图：`getParameterTypes()` 是擦除后的，`toGenericString()` 会还原成 `add(E)`。**
   同一个方法两套签名，框架里混用就会出 bug。
3. **想复原类型只有一个办法：继承一个写了实参的类，再读 `getGenericSuperclass()`。**
   `StringBox extends Box<String>` 能拿到 `String`，但 `Box.class.getDeclaredMethod("set", Object.class)`
   的泛型参数类型还是 `T`（方法自己的签名里没有 String），`StringBox` 自己声明的方法数是 **0**。
   Spring 的 `ParameterizedTypeReference`、Jackson 的 `TypeReference` 全靠这一招 ——
   匿名子类 + 泛型父类，本质是"用继承把实参写进 class 文件"。

   > ⚠️ 这里原先写的是 `StringBox.class.getMethod("set", Object.class)`，**跑起来直接抛
   > `NoSuchMethodException`**，把 Day 1 的演示程序截断在 B2 段（`day1-result.txt` 第一版末尾
   > 就留着那个异常栈）。原因是 `Box.set` 是包私有方法，而 `getMethod()` 只找 **public**。
   > 换成 `Box.class.getDeclaredMethod(...)` 才读得到。这条坑已修，快照 2026-10-29 重跑。

## 读代码的顺序

1. 先跑 `run.bat`，只看 A 段三行。`getClass() ==` 为 `true` 这一行，比读十遍"泛型是编译期的"都值。
2. 再读 `sectionC()`：注意 `String s = list.get(1)` 和 `List raw = list` 两个断点，
   理解"类型检查被搬到编译期"到底意味着什么。
3. 然后读 `sectionD()`：`(T[]) new Object[n]` 与 `Array.newInstance` 的对比，
   是"运行时需要组件类型"的唯一证据。
4. 最后跑 `PecsDemo`，按 `invariance → extends → super → copy → Stack` 的顺序读，
   每读一个就猜一下"这行会不会编译过"，再跑。
5. 补跑 `WildcardCapture`：先看 A 段的 `required: int,CAP#1 / found: int,CAP#2`，
   再看 B 段 helper 怎么一次捕获搞定。

---

## Day 2（异常体系）实验清单

| #  | 要验证的结论 | 命令 | 实测 |
| :- | :--- | :--- | :--- |
| 1 | TWR 关闭顺序是逆序 | `run.bat day2.TryWithResourcesProbe` | `open A → open B → use A → use B → close B → close A` |
| 2 | `close()` 抛异常时主异常不被覆盖 | 同上 | 主异常 `RuntimeException: 业务代码里炸的`，`getSuppressed().length = 1` |
| 3 | `finally` 里 `return` 静默吞异常 | 同上 | `swallow()` 返回 42，异常**无影无踪**；`overrideReturn()` 返回 2 |
| 4 | `finally` 里 `throw` 覆盖主异常，连 suppressed 都没有 | 同上 | 只 catch 到 close 的异常，`getSuppressed().length = 0`、`getCause() = null` |
| 5 | `close()` 的受检异常会被提升到 TWR 语句上 | 同上 | `catch(IOException)` 必须写，否则编译不过 |
| 6 | body 和 close 都抛时谁是主异常 | 同上 | 主异常是 body 的，close 的进 suppressed |
| 7 | `new RuntimeException(e)` **不丢栈**，是把栈切成两段 | `run.bat day2.ExceptionChainProbe` | 包装栈深度 3、栈顶在包装行；cause 栈深度 5、栈顶在 `deepest` |
| 8 | 只传 message 才是真丢栈 | 同上 | `getCause() = null`，异常类型也没了 |
| 9 | 日志写法决定还剩多少现场 | 同上 | `getMessage()` 11 字 1 行；`+e` 39 字 1 行；`printStackTrace` 634 字 9 行含 `Caused by` |
| 10 | `cause` 与 `suppressed` 分工不同 | 同上 | cause 一对一（`addSuppressed(main)` 抛 `Self-suppression not permitted`）；suppressed 一对多 |
| 11 | 受检的边界是「有没有可能抛出」 | 同上 | 无受检来源却 `catch (IOException)` → `exception IOException is never thrown in body of corresponding try statement` |
| 12 | 擦除能绕过受检检查（sneaky throw） | 同上 | `void looksInnocent()` 无 throws，运行期真抛 `java.io.IOException`，`getCause() = null` |

**本节最值钱的两个点**

1. **「`RuntimeException(e)` 丢栈」这个说法只对一半。**
   实测：包装异常的栈深度 3（栈顶是包装那一行），cause 的栈深度 5（栈顶是真正出事的 `deepest`）。
   栈没丢，是断成两截。所以「日志里带不带 cause」决定了你能不能看到后半截 ——
   真正丢栈的是 `new RuntimeException(e.getMessage())`，`getCause()` 直接是 `null`。
2. **`finally` 里的 `return` 才是真的把异常变没。**
   `swallow()` 里 `throw` 了一句 `IllegalStateException`，方法却返回 42 给调用方，
   没有异常、没有日志、没有 suppressed。javac 一句话都不说。
   TWR 存在的理由不是「少写 close」，而是保证主异常活下来、close 的异常降级成 suppressed。

### Day 2 遗留补完：`ExceptionCost`（2026-10-30）

同一个类跑两遍，只改一个 JVM 参数：

```bash
java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w02.day2.ExceptionCost
java -Dfile.encoding=UTF-8 -XX:-StackTraceInThrowable -cp out com.newt.java48.w02.day2.ExceptionCost
```

| #  | 要验证的结论 | 实测（JDK 17.0.12，5 轮 × 10 万次取最小） |
| :- | :--- | :--- |
| 1 | 贵的是抓栈，不是抛 | `new` 418 ns / `throw+catch` 447 ns，差 29 ns |
| 2 | 关掉栈采集后断崖式下降 | 447 → **31 ns**，约 15 倍 |
| 3 | 栈深是线性成本 | 多 5 层栈多 63 ns |
| 4 | 缓存异常被 JIT 优化掉 | ≈0 ns，且栈永远停在第一次创建处 |

> ⚠️ 微基准必须多轮取最小值。第一版每用例只测一轮，同一段代码两次读出 44 ns 和 295 ns
> （JIT 还停在 C1 / 解释执行）。「一次跑出来的耗时」不可信。

---

## Day 3（Optional）实验清单

| #  | 要验证的结论 | 命令 | 实测 |
| :- | :--- | :--- | :--- |
| 1 | `isPresent()+get()` 只是换了壳的 null 检查 | `run.bat day3.OptionalAntiPatterns` | 三层嵌套 16 行 vs 一条链 4 行，结果相同 |
| 2 | `get()` 的报错信息比 NPE 还差 | 同上 | `NoSuchElementException: No value present`，无变量名无上下文；对照 NPE 有 `because "..." is null` |
| 3 | `orElse(compute())` 会白算一次 | 同上 | 有值时远程调用 **1 次 / 36.2 ms**；`orElseGet` **0 次 / 0.3 ms** |
| 4 | Optional 当字段会让对象不可序列化 | 同上 | `NotSerializableException: java.util.Optional`；改成可空字段后 133 字节正常 |
| 5 | Optional 当参数，签名是骗人的 | 同上 | `sendMail(null)` 照样 NPE，栈顶就在 `sendMail` |
| 6 | `List<Optional<T>>` 让所有人多判一次 | 同上 | 3 个元素 2 个有效；`flatMap(Optional::stream)` 展平成 `[a, c]` |
| 7 | `of(null)` / `map→null` / `flatMap→null` 三者行为不同 | 同上 | NPE / **静默 empty** / NPE |
| 8 | `orElse(null)` 破坏契约 | 同上 | 返回 `null`，Optional 形同虚设 |
| 9 | 一条链与 null 检查等价但只写一次兜底 | `run.bat day3.OptionalChain` | 三种缺失场景（无地址 / 空串 / 正常）同一段代码 |
| 10 | `map` 调「返回 Optional 的方法」会套两层 | 同上 | `Optional<Optional<Address>>`，要 `.get().get()` |
| 11 | `filter` 是链式写法里不能省的一环 | 同上 | 券 500 > 订单 200：不 filter 算出 **−300**，filter 后 200 |
| 12 | `orElseThrow` 唯一值钱的地方是补上下文 | 同上 | `用户地址缺失，userId=10086` vs `No value present` |

**本节最值钱的两个点**

1. **Optional 让报错信息退化，不是进化。** `empty.get()` 只给一句 `No value present`，
   而 JDK 14+ 的 NPE 至少会说清「谁」是 null。所以 Optional 的正确姿势不是「包起来再拆」，
   而是「沿链传递、链尾处理一次」——`orElse` 出现多次就是信号：你在用它做 null 检查。
2. **`orElse` 参数是 eager 的，且 `Optional` 只管「有没有」不管「合不合法」。**
   前者让「命中缓存」的路径白打一次远程（36 ms vs 0.3 ms，结果完全正确）；
   后者让券减 500 的订单算出 −300。两个都是「结果看起来正常」的 bug，只能靠 `orElseGet` + `filter` 挡。
