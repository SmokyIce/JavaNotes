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
├── day4-result.txt                         Day 4 实测输出（函数式接口 + 捕获 + 8 条报错原文）
├── day4-bytecode.txt                       Day 4 字节码证据（javap -p / -c / -v）
├── day5-result.txt                         Day 5 实测输出（惰性 + 短路 + 屏障 + parallel）
├── day6-result.txt                         Day 6 实测输出（过滤 + 排序 + 分组 + 六个收集器陷阱）
├── compile-errors/
│   ├── GenericLimits.java                  故意编译不过：擦除的四条硬限制
│   ├── GenericLimits2.java                 故意编译不过：通配符的六种拒绝
│   ├── LambdaLimits.java                   故意编译不过：五个 lambda 目标类型 / 函数式接口错误
│   ├── LambdaLimits2.java                  故意编译不过：lambda 里 break、void 里 return 值
│   └── LambdaLimits3.java                  故意编译不过：effectively final（必须单独一个文件）
└── src/main/java/com/newt/java48/w02/
    ├── day1/
    │   ├── ErasureProof.java               六个实验：类同一性 / 反射签名 / CCE 位置 / 泛型数组 / 桥方法 / 硬限制
    │   ├── PecsDemo.java                   五个实验：不变性 / ? extends / ? super / copy / Stack pushAll
    │   └── WildcardCapture.java            四个实验：捕获报错原文 / helper 捕获正例 / JDK 的 raw 选择 / raw 的代价
    ├── day2/
    │   ├── TryWithResourcesProbe.java      五个实验：逆序关闭 / suppressed / finally 里 return / finally 里 throw / 受检 close
    │   ├── ExceptionChainProbe.java        六个实验：三种包装 / 日志写法 / cause vs suppressed / 受检边界 / sneakyThrow / 何时包装
    │   └── ExceptionCost.java              异常开销：fillInStackTrace 的账单（跑两遍，只改一个 JVM 参数）
    ├── day3/
    │   ├── OptionalAntiPatterns.java       五段：isPresent+get / orElse 白算 / 字段参数集合 / 五个陷阱 / 判定表
    │   └── OptionalChain.java              六段：null 检查对比 / map vs flatMap / filter / orElseGet vs orElseThrow / 消费侧 / 优惠券算价
    ├── day4/
    │   ├── FunctionalShape.java            六段：五种形态 / Predicate 短路 / andThen vs compose / 装箱逃逸 / 自定义接口 / 目标类型
    │   └── LambdaCapture.java              六段：方法引用五连 / 值捕获 / 循环变量 / this 归属 / 实例复用 / invokedynamic
    ├── day5/
    │   ├── StreamLazy.java                 四段：惰性求值 / 短路 / 屏障 / peek 与 count
    │   ├── StreamOnce.java                 四段：流只能用一次 / map vs flatMap / parallel 代价 / 何时不用
    │   └── FuncInterfaceRule.java          四段：抽象方法怎么数（补 Day 4 遗留）
    └── day6/
        ├── User.java                       15 条统一模型（故意留 name/age/dept 三个 null）
        ├── UserData.java                   共用数据源
        ├── UserReport.java                 A 过滤（Predicate 左结合 / null NPE / 短路）+ B 排序（多级 / reversed 陷阱 / 稳定性）
        ├── GroupStats.java                 C 分组：groupingBy 一参/两参/三参 + 七个下游收集器 + 多级 + partitioningBy
        └── CollectorTraps.java             D 六个陷阱：toMap 两种炸法 / groupingBy null key / teeing / 三种 toList
```

> `compile-errors/` 不在 `src/main/java` 下，不会被 `run.bat` 编译 —— 否则整个工程编不过。
> 要单独复现报错：
>
> ```bash
> cd compile-errors
> javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors GenericLimits.java
> javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors GenericLimits2.java
> javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors LambdaLimits.java
> javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors LambdaLimits2.java
> javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors LambdaLimits3.java
> ```
>
> 加 `-J-Duser.language=en` 是因为中文 locale 下 `name clash: ... same erasure` 会被译成
> 「具有相同擦除」，搜不到任何资料 —— 报错原文比译文有用得多。
> `GenericLimits2` / `LambdaLimits*` 的注释刻意写成英文：javac 会把出错那行源码原样回显，
> 中文注释在那里会变成 GBK 乱码，污染快照。
>
> **`LambdaLimits` 为什么拆成三个文件**：javac 先做 attribution（类型 / 重载 / 目标类型），
> 再做 FLOW 分析，`effectively final` 属于后者。只要文件里有任何一个 attribution 错误，
> flow 分析就整个被跳过 —— 六条错误一起放会只报五条，`effectively final` 那条**凭空消失**。

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

---

## Day 4（Lambda）实验清单

| #  | 要验证的结论 | 命令 | 实测 |
| :- | :--- | :--- | :--- |
| 1 | 五种形状覆盖所有函数式接口 | `run.bat day4.FunctionalShape` | `Function` / `BiFunction` / `Consumer` / `Supplier` / `Predicate` 各一个业务例子 |
| 2 | `Predicate.and` 是短路的 | 同上 | 三层 `and`，第二个返回 false，**第三个没执行**（日志缺 check3） |
| 3 | `andThen` 与 `compose` 顺序相反 | 同上 | `(2+1)*10 = 30` vs `2*10+1 = 21` |
| 4 | 装箱贵不贵取决于逃逸 | 同上 | 不逃逸 **2.0 倍**（10.0 vs 4.9 ms）；逃逸到 `Integer[]` **36 倍**（23.7 vs 0.7 ms） |
| 5 | 函数式接口可以有多个 default 方法 | 同上 | `OrderRule` 抽象方法 1 个、default 1 个，`@FunctionalInterface` 仍然合法 |
| 6 | `var` / `Object` 都不是 lambda 的目标类型 | 同上 + `LambdaLimits` | 两条都是编译错，原文在 `day4-result.txt` 第 3 段 |
| 7 | 方法引用绑定与未绑定差一个参数 | `run.bat day4.LambdaCapture` | `String::length` 1 个参数 vs `str::length` 0 个参数 |
| 8 | 捕获是值拷贝，合成方法的形参为证 | 同上 | `lambda$sectionB$1(int, int)`：第二个 int 就是 `base` |
| 9 | 字段能改、数组能绕过 | 同上 | 改 `delta` 1→100，同一 lambda 结果 `6`→`105`；`box[0]` 同理 |
| 10 | `for` 编译错、`for-each` 各自拷贝、数组共享 | 同上 | `[1,2,3]` vs `[3,3,3]` |
| 11 | lambda 的 `this` 是外部类 | 同上 | 匿名类 `LambdaCapture$1` vs lambda `LambdaCapture` |
| 12 | 非捕获 lambda 复用单例 | 同上 | `nonCapturing()==nonCapturing()` **true**；`capturing(1)==capturing(1)` **false** |
| 13 | lambda 是 `invokedynamic` + 隐藏类 | `day4-bytecode.txt` | 磁盘无 `$$Lambda`；`isHidden()`=true；`BootstrapMethods` 指向 `LambdaMetafactory` |

**本节最值钱的两个点**

1. **`effectively final` 不是语法洁癖，是值拷贝的必然结果。** `javap -p` 能直接看到
   `lambda$sectionB$1(int, int)` —— 捕获的变量成了合成方法的**形参**。于是「字段能改」
   （捕获的是 `this`，合成方法非 static）、「数组能绕过」（拷的是引用）都有了同一个解释。
2. **「装箱很贵」只有一半对，贵的是逃逸的装箱。** 不逃逸时标量替换把 `new Integer` 消掉，只差 2 倍；
   一逃逸立刻 36 倍。所以优化判断要落在「会不会进集合/跨线程」，不是「有没有装箱」。

---

## Day 5（Stream）实验清单

| #  | 要验证的结论 | 命令 | 实测 |
| :- | :--- | :--- | :--- |
| 1 | 中间操作只记账，不接终止操作一行都不跑 | `run.bat day5.StreamLazy` | 构造完链路日志 **0 条**；接 `collect` 后才有 |
| 2 | 一个元素走完全链才轮到下一个 | 同上 | 日志 `filter a, map a, filter b, map b, filter c, map c` |
| 3 | `sorted` 是屏障，把管道切成两段 | 同上 | 日志变成 `filter c, filter a, filter b` 才轮到 `map` |
| 4 | `sorted` 在上游已有序时是**空操作** | 同上 | `rangeClosed(1,5).sorted().findFirst()` 只探测 **1** 个；换 `of(5,3,1,4,2)` 变 **5** 个 |
| 5 | `distinct` 一定全量 | 同上 | 6 个元素探测 6 个 |
| 6 | 短路终止操作：100 万个数里找 3 | 同上 | `anyMatch` 只探测 **3** 个 |
| 7 | 无限流也能停 | 同上 | `iterate(1, i+1).filter(i%7==0).findFirst()` → 7，探测 **7** 个 |
| 8 | `count` 不短路 | 同上 | 100 万个数探测 **1 000 000** 个 |
| 9 | `limit` 是短路中间操作 | 同上 | `iterate(...).limit(3)` → `[1,2,3]`，探测 3 个 |
| 10 | `sorted` 前后的成本差一个数量级 | 同上 | 100 万条：`sorted→limit(3)` **25 ms** vs `limit(3)→sorted` **< 1 ms**（语义不同） |
| 11 | `count()` 会把 `peek` 整条跳过 | 同上 | `peek(log).count()` 日志 **0** 条；前面加 `filter(x->true)` 变 **3** 条 |
| 12 | 一个流只能用一次 | `run.bat day5.StreamOnce` | 第二次 `count()` → `IllegalStateException: stream has already been operated upon or closed` |
| 13 | 字段缓存流 = API 设计事故 | 同上 | 调用两次第二次必挂；方法返回新流两次都正常 |
| 14 | `Supplier<Stream>` 才是正解 | 同上 | 两次 `get()` 分别得到 `2` / `[A001, A003]` |
| 15 | `map` 出嵌套，`flatMap` 出平铺 | 同上 | 3 个订单：`map` 得 3 个 `List`，`flatMap` 得 **6** 个 `Item` |
| 16 | `Optional::stream` 展平集合 | 同上 | `[Optional(of a), empty, of c]` → `[a@x.com, c@x.com]` |
| 17 | `parallel().forEach` 不保序 | 同上 | 实测 `[7,8,6,10,11,9,3,1,5,4,2,0]`（每次不同）；`forEachOrdered` 才 `[0..11]` |
| 18 | 大数据量并行有收益，小数据量反而慢 | 同上 | 2000 万求和 20 → 6 ms；1000 元素 × 30 轮 21 → 49 µs（**更慢**）；200 万 × 30 轮 34 → 5 ms |
| 19 | 函数式接口的抽象方法要减掉 Object 方法 | `run.bat day5.FuncInterfaceRule` | 反射数出 **4** 个，编译器只算 **1** 个 |
| 20 | `Comparator` 只剩一个真抽象方法 | 同上 | `[compare]`（`equals` 是重声明，`thenComparing` 全是 default） |

**本节最值钱的三个点**

1. **「中间操作 / 终止操作」不是知识点，是排查工具。** 线上看到「filter 写了却没生效」，
   按 ① 链尾缺终止操作 → ② 被 `sorted`/`distinct` 屏障挡住 → ③ `count()` 走快速路径跳过整条管道
   三步走，比读十遍 API 文档快。
2. **`sorted` 是不是屏障，取决于上游 spliterator 有没有 `SORTED` 特征。**
   `IntStream.rangeClosed` 自带这个特征，`SortedOps.opWrapSink` 直接 `return sink` 把节点摘掉；
   换成真无序的源才吃满。所以「`sorted` 一定全量」是错的 —— 结论要 fact-check，凭印象会差 5 倍。
3. **`parallel` 不是「更快」，是「用核数换单次耗时」。** 实测量级：2000 万求和 3.3 倍收益，
   1000 元素 × 30 轮 **反向慢 2~5 倍**。四条标准（量 ≥ 1 万 / 计算够重 / 无共享可变状态 / 不依赖顺序）
   缺一条就别开。

---

## Day 6（`List<User>` 报表）实验清单

数据：`UserData` 15 条，**故意留三个 null** —— `name` 1 条（U14）、`age` 2 条（何静 / 徐丽）、`dept` 1 条（U99）。
所以每一段都会撞一次 NPE，这正是清单要留的证据。

| #  | 要验证的结论 | 命令 | 实测 |
| :- | :--- | :--- | :--- |
| 1 | 两个 `filter` 串联等价于 `&&` | `run.bat day6.UserReport` | 在职且薪资≥20000 → **9 人** |
| 2 | `isDev.or(isTest).and(active)` 是**左结合** | 同上 | `(a\|\|b)&&c` → **6 人**；`a\|\|(b&&c)` → **7 人**，差的是离职研发 |
| 3 | `filter(Objects::nonNull)` 防不了字段 null | 同上 | 15 条过滤后**还是 15 条**，后面照样 NPE |
| 4 | 名字判断撞 null 的报错原文 | 同上 | `Cannot invoke "String.startsWith(String)" because the return value of "...User.name()" is null` |
| 5 | `findFirst` / `*Match` 短路 | 同上 | 只拿到第一个研发就停；`allMatch(都有部门)` → **false** |
| 6 | 多级排序：部门↑ → 薪资↓ → 年龄↑ | 同上 | 研发组 45000/31000/22000/22000/18000；同薪的 22000 两人按年龄排，null 在后 |
| 7 | **`reversed()` 反转的是整条链** | 同上 | 写法一 dept 降序且 **null 跑到最前**；写法二 dept 仍升序 |
| 8 | `comparing(User::age)` 遇 null 直接 NPE | 同上 | `Cannot invoke "Comparable.compareTo(Object)" ... is null` |
| 9 | `nullsLast` 必须包在内层 | 同上 | `comparing(User::age, nullsLast(Integer::compareTo))` 排完，末两位是 null |
| 10 | `List.sort` 稳定（TimSort） | 同上 | 先按薪资升序、再按部门排，同部门内薪资顺序完整保留 |
| 11 | `groupingBy` 一参 | `run.bat day6.GroupStats` | 4 个部门：产品 3 / 研发 5 / 运营 3 / 测试 3 |
| 12 | 默认 `HashMap` 的 key 顺序不可依赖 | 同上 | `[产品, 研发, 运营, 测试]` —— 既非插入序也非字母序 |
| 13 | `summingInt` 分组求和 | 同上 | `{产品=83000, 研发=138000, 运营=55000, 测试=48500}` |
| 14 | `averagingInt(User::age)` 遇 null 拆箱 NPE，**message 为空** | 同上 | `NullPointerException，message = <空>` |
| 14b | 同一拆箱换成 lambda 就**有** message | 同上 | `averagingInt(u -> u.age())` → `Cannot invoke "Integer.intValue()" because ... "User.age()" is null` |
| 15 | 「补默认值 0」会改分母 | 同上 | 研发 25.6（null 当 0）vs **32.0**（先滤 null），**差 6.4 岁** |
| 16 | `joining` 把 null 拼成字面量 | 同上 | 测试组 → `吴磊/徐丽/null` |
| 17 | `maxBy` 的值是 `Optional` | 同上 | 研发 → 周涛(45000)；套 `collectingAndThen` 可拆掉 Optional |
| 18 | 三参重载的 `mapFactory` 决定 key 顺序 | 同上 | HashMap `[产品,研发,运营,测试]` / LinkedHashMap `[研发,产品,运营,测试]` / TreeMap `[产品,测试,研发,运营]` |
| 19 | 多级分组 | 同上 | 研发 → `{上海=1, 深圳=1, 北京=3}` |
| 20 | `partitioningBy` 空组不消失 | 同上 | 产品组全在职：`partitioningBy` 的 `false` 组 = `[]`；`groupingBy` 压根没这个 key |
| 21 | `summarizingInt` 一次拿五个指标 | 同上 | 研发 `count=5, sum=138000, min=18000, avg=27600, max=45000` |
| 22 | `toMap` 重复 key | `run.bat day6.CollectorTraps` | `IllegalStateException: Duplicate key 研发 (attempted merging values 22000 and 31000)` |
| 23 | `toMap` 的合并函数决定业务语义 | 同上 | `(a,b)->a` → `研发=22000`；`Math::max` → `研发=45000` |
| 24 | **`HashMap.put(k, null)` 允许，`toMap` 的 null value 却 NPE** | 同上 | `put` 得到 `{何静=null}`；`toMap` 抛**无 message** 的 NPE（内部走 `merge`） |
| 25 | `groupingBy` 拒绝 null key | 同上 | `element cannot be mapped to a null key` |
| 26 | 同一份数据 `toMap` 却接受 null key | 同上 | `{null=50000}` —— 因为它走 `HashMap.merge` |
| 27 | `teeing` 一次遍历算平均 | 同上 | 一遍与两遍结果一致：总额 374500 / 15 人 / 人均 24966.7 |
| 28 | 三种 `toList` 的可变性 | 同上 | `ArrayList` 可 add；`ListN` 两个都抛 `UnsupportedOperationException` |
| 29 | **三种 `toList` 的 null 政策不一致** | 同上 | `toList()` 允许 / `Stream.toList()` **允许** / `toUnmodifiableList()` **NPE** |
| 30 | `toMap` 硬扛一对多是自找麻烦 | 同上 | `toMap + 合并函数` 与 `groupingBy + mapping` 结果相同，后者一行搞定 |

**本节最值钱的三个点**

1. **`Predicate` 的方法链是左结合，不是布尔优先级。** `isDev.or(isTest).and(active)` 是
   `(a||b)&&c` 而不是 `a||(b&&c)`。这类错**不报编译错、不抛异常，只少几行数据** ——
   条件拼装完必须拿两条边界数据验人数。
2. **`reversed()` 作用于它左边整条链，连 `nullsLast` 也会一起反转成 `nullsFirst`。**
   要只反转某一级，就把 `reverseOrder()` 传给那一级的 `comparing`，别在链尾按 `.reversed()`。
3. **API 的 null 政策只能靠「它内部调了什么」推，不能凭名字猜。**
   `toMap` 走 `HashMap.merge`（value 不许 null、key 可以 null）；
   `groupingBy` 走 `Objects.requireNonNull`（key 不许 null）；
   `Stream.toList()` 走 `listFromTrustedArrayNullsAllowed`（放行 null），
   而 `toUnmodifiableList()` 走 `listFromTrustedArray`（拒绝 null）——
   同一族 `ListN` 实现，行为却不同。
