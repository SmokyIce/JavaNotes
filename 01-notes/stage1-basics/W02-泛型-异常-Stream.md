# W02 · Java 泛型 · 异常 · Optional · Stream

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟩 已完成（Day 1 / Day 2 / Day 3 / Day 4 / Day 5 / Day 6 全部完成）　｜　开始日期：2026-10-28　｜　完成日期：2026-11-03

> 💡 周六练习：给一个 List<User>，完成 过滤 → 排序 → 分组 → 统计。（Day 6 已完成）

---

## 一、本周知识点

- [x] 泛型
- [x] 异常体系
- [x] Optional
- [x] Lambda
- [x] Stream
- [x] 过滤 / 排序 / 分组 / 统计

## 二、核心问题（能用自己的话脱稿讲出来，才算学会）

1. 「泛型只在编译期存在」这句话，运行时具体丢了什么、又留下了什么？用两个实验证明。
2. 为什么可以写 `new ArrayList[10]`，却不能写 `new ArrayList<String>[10]`？
3. `List<? extends Number>` 和 `List<? super Integer>` 各自能做什么、不能做什么？为什么 `Collections.copy` 要用两个不同的通配符？
4. `finally` 里的 `return` 和 `throw` 分别会怎样处理 `try` 里正在冒泡的异常？为什么 `try-with-resources` 要专门发明 `addSuppressed`？
5. 说 `throw new RuntimeException(e)` 会「丢栈」，这句话哪里不准确？什么样的写法才是真的丢栈？
6. `isPresent() + get()` 和 `orElse` 都是「有值取值、没值兜底」，为什么前者是反模式？`orElse` 又在什么情况下会白算一次、甚至算出负数？
7. 「lambda 里不能改外部局部变量」这条规定，真实的物理原因是什么？怎么用字节码证明它捕获的是值拷贝？
8. `Function<Integer,Integer>` 一定比 `IntUnaryOperator` 慢吗？为什么「装箱很贵」这个结论有时量出来只有 2 倍，有时是 36 倍？
9. `stream().filter(...).map(...)` 写完不接终止操作会怎样？为什么加了 `sorted()` 之后打印顺序会变？怎么判断一行代码是中间操作还是终止操作？
10. 一个 `Stream` 为什么只能用一次？`parallel()` 在什么情况下反而更慢？
11. `groupingBy` 的三参数重载各解决什么问题？`toMap` 在什么情况下会抛异常？为什么它对 null 的容忍度和 `groupingBy` 不一样？

> 自己先答一遍，再翻文末「参考要点」，别倒着来。

## 三、代码 / 实验

- 代码目录：`02-code/stage1-basics/w02-generics-stream/`
- 本周动手内容：

```text
Day 1（已完成 10-28）  类型擦除：六个实验 + PECS 五个实验 + 四类编译期报错原文
Day 1 补（已完成 10-29）通配符捕获：capture 报错原文 + helper 正例 + JDK 的 raw 选择
Day 2（已完成 10-29）  异常体系：TWR 关闭与 suppressed / finally 吞异常 / 包装丢栈真相 / sneakyThrow
Day 2 补（已完成 10-30）异常开销：fillInStackTrace 才是账单，-XX:-StackTraceInThrowable 对照实测
Day 3（已完成 10-30）  Optional：三个反模式 + 五个陷阱 + 一条 map/flatMap/filter 链
Day 4（已完成 11-02）  Lambda：函数式接口五形态 + 方法引用五连 + 变量捕获（值拷贝）+ invokedynamic
Day 5（已完成 11-02）  Stream：惰性求值 / 短路 / 屏障 / peek + 流只能用一次 + map vs flatMap + parallel
Day 6（已完成 11-03）  List<User> 报表：过滤（Predicate 组合）+ 多级排序（nullsLast）+ groupingBy 三层重载 + 收集器六个陷阱
```

### Day 1 · 2026-10-28（周三）· 类型擦除 + PECS

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 W01：HashMap put 四步 + 扩容为什么只拆两条链 | 口头过，卡住回看 W01 Day 4 |
| 25 分钟 | 擦除到底擦掉了什么：`List<String>` 和 `List<Integer>` 运行时是同一个类 | `ErasureProof` 的 A / B / C 段 |
| 15 分钟 | 把「擦除」推到它的必然结果：泛型数组、桥方法、四条硬限制 | `ErasureProof` D / E / F + `compile-errors/` |
| 10 分钟 | PECS：不变性 → extends → super → copy → Stack | `PecsDemo` 五段 |

实测快照：`w02-generics-stream/day1-result.txt`（JDK 17.0.12，含 javac 报错原文）

#### 1. 擦除擦的是「实参」，不是「声明」

`List<String>` 和 `List<Integer>` 的 `getClass()` 是**同一个对象**，`==` 为 `true`，
名字都是 `java.util.ArrayList` —— 连 `List` 这个接口都不剩，只剩实现类。

但 `Box.class.getTypeParameters()[0]` 还能读出 `T`，`Box.get` 的泛型返回类型也是 `T`。
所以准确的表述是：**擦掉的是使用处填的实参，声明处的类型变量以元数据留在 class 文件里。**

> 🔍 补充：实参并没有完全消失，它藏在子类的 `Signature` 属性里。
> `StringBox extends Box<String>` 之后，`StringBox.class.getGenericSuperclass()` 能拿到
> `Box<java.lang.String>`，实参 `String` 就在里面。
> 这是运行时**唯一**能复原泛型的地方 —— Spring 的 `ParameterizedTypeReference`、
> Jackson 的 `TypeReference` 全是「匿名子类 + 泛型父类」这一招。
> 反过来说，`StringBox.class.getMethod("set", Object.class).getGenericParameterTypes()[0]`
> 拿到的还是 `T` 而不是 `String`，因为方法自己的签名里本来就没写 String。

#### 2. 类型检查被搬到了「按类型读出来」那一刻

原始类型塞进去不报错：

```java
List<String> list = new ArrayList<>();
list.add("hello");
List raw = list;
raw.add(42);            // 无声通过
Object o = list.get(1); // 也不报
String s = list.get(1);// 这行抛 CCE
```

实测 CCE 抛在 **`String s = list.get(1)` 那一行**（`ErasureProof.java:134`），
不是 `add` 那一行。因为编译器在每个 `get` 后面都插了 `checkcast java/lang/String`。

> 🔍 补充：这解释了线上一类很迷惑的报错 —— `Integer cannot be cast to String` 出现在
> 某个看起来人畜无害的 `get` 上。真正的原因是**之前某个地方**用原始类型 / 反射 /
> JSON 反序列化塞了错类型。改那行 `get` 没用，要往上追。

#### 3. 为什么不能 `new T[]`

数组在**运行时**必须知道自己的组件类型（`ArrayStoreException` 就是靠它检查出来的），
而擦除后的 `T` 什么都不剩，所以 `new T[10]` 编译不过，javac 的原话是 `generic array creation`。

`(T[]) new Object[n]` 只是骗过编译器：真实类型还是 `[Ljava.lang.Object;`，
一旦赋值给 `String[]` 立刻 CCE。JDK 自己的兜底方案是把 `Class<T>` 显式传进来：

```java
static <T> T[] newArray(Class<T> type, int n) {
    return (T[]) Array.newInstance(type, n);   // 真实类型 [Ljava.lang.String;
}
```

这正是「擦除 → 为什么不能 `new T[]` → 为什么框架都要 `Class<T>` 参数」这条链的终点：
`TypeReference` / `Class<T>` / `Array.newInstance` 三种写法，解决的是同一个问题。

#### 4. 桥方法：编译器替你补的转发

`class StringCmp implements Comparator<String>` 只写了一个 `compare(String,String)`，
但反射能看到**两个**：

```text
compare(String, String)    bridge=false  synthetic=false
compare(Object, Object)    bridge=true   synthetic=true
```

接口擦除后是 `compare(Object,Object)`，实现类写的是 `compare(String,String)`，两个签名对不上，
编译器就补一个 `(Object,Object)` 版本做 `checkcast` 再转调。

> 🔍 补充：这是「泛型重写」能工作的底层机制，也是反射框架的坑 ——
> 遍历 `getDeclaredMethods()` 时一定要跳 `isBridge()`，否则同一个方法会被处理两次。
> Spring 的 `ResolvableType`、MyBatis 的 Mapper 方法解析都在这上面做过特殊处理。

#### 5. 擦除写下的四条硬限制（javac 原话）

`compile-errors/GenericLimits.java` 一次编出 6 个错，摘四条：

| 写法 | javac 原话 |
| :--- | :--- |
| `static T shared;` | `non-static type variable T cannot be referenced from a static context` |
| `T[] arr = new T[10];` | `generic array creation` |
| `catch (T e)` | `unexpected type / required: class, found: type parameter T` |
| `f(List<String>)` + `f(List<Integer>)` | `name clash: ... have the same erasure` |
| `new ArrayList<String>[10]` | `generic array creation` |
| `o instanceof List<String>` | `Object cannot be safely cast to List<String>` |

> 🔍 补充：中文 locale 下 javac 把 `same erasure` 译成「具有相同擦除」，这个说法
> 在搜索引擎里几乎搜不到东西。看报错时加 `-J-Duser.language=en` 拿原文，效率差好几倍。

#### 6. PECS

| 场景 | 写法 | 能读成 | 能写 | 谁用 |
| :--- | :--- | :--- | :--- | :--- |
| 不变 | `List<T>` | T | T | 读写同类型 |
| 生产者 | `List<? extends Number>` | `Number` | 只有 `null` | `sum()` / 遍历汇总 |
| 消费者 | `List<? super Integer>` | `Object` | `Integer` 及其子类 | `fill()` / `Collections.copy` 的 dest |
| 两端都放开 | `<T> copy(List<? super T> dest, List<? extends T> src)` | — | — | 跨类型搬运 |

一句话：**从里面读，用 extends；往里面写，用 super。**

> 🔍 补充：`List<? extends Number>` 允许 `add(null)`，这不是 bug ——
> `null` 是唯一对所有子类型都成立的值，所以编译器放行。这个细节面试里提一句很加分。

#### 7. 遗留（2026-10-29 已补完）

- ✅ `compile-errors/GenericLimits2.java` 补上了，一次编出 **6 个错**，覆盖通配符的五种拒绝 + `new` 里不能写 `?`。
  原文进 `day1-result.txt` 第 4/4 段。注释刻意写成英文：javac 会把出错那行源码原样回显，中文在那里会变 GBK 乱码。
- ✅ 通配符捕获的正例补成 `day1/WildcardCapture.java`，顺手挖到一条比正例更值钱的东西 ——
  **JDK 17 的 `Collections.swap` 没走捕获，走的是原始类型**（见 Day 2 之前的「补 · 通配符捕获」一节）。

#### 8. 补 · 通配符捕获（2026-10-29）

Day 1 原计划没有这一节，补它的起因是「`capture of ?` 只见过报错文案，没见过正例」。

**先看不能写什么。** 最自然的交换写法编译不过，javac 原文（`-Xdiags:verbose`）：

```text
error: method set in interface List<E> cannot be applied to given types;
  required: int,CAP#1
  found:    int,CAP#2
  reason: argument mismatch; Object cannot be converted to CAP#1
  where CAP#1,CAP#2 are fresh type-variables:
    CAP#1 extends Object from capture of ?
    CAP#2 extends Object from capture of ?
```

关键在 `required CAP#1` 和 `found CAP#2`：**捕获发生在「每次方法调用」上**，
同一句里的 `xs.get(j)` 和 `xs.set(...)` 各捕各的，编译器没有依据认为两者同类型。
所以 `List<?>` 连「读出来再放回去」都做不到。

**正例是加一层 helper，把 `?` 变成一个具名类型变量：**

```java
private static void swapWild(List<?> xs, int i, int j) { swap(xs, i, j); }   // ? -> CAP#1
private static <T> void swap(List<T> xs, int i, int j) {                     // T := CAP#1
    T tmp = xs.get(i);
    xs.set(i, xs.get(j));
    xs.set(j, tmp);
}
```

`T` 在方法体内只推断一次，全程同一个类型，`set` 就通了。对外仍是 `List<?>`，调用方不用关心元素类型。

> 🔍 最有意思的一条：**JDK 自己没这么写。** JDK 17 `java.util.Collections:495-502`：

```java
@SuppressWarnings({"rawtypes", "unchecked"})
public static void swap(List<?> list, int i, int j) {
    // instead of using a raw type here, it's possible to capture
    // the wildcard but it will require a call to a supplementary
    // private method
    final List l = list;
    l.set(i, l.set(j, l.get(i)));
}
```

源码注释说得很直白：**捕获能做到，但要多一个私有方法**，所以它选了 raw type。
代价是把类型检查全关掉 —— 实测 `raw.set(0, "我不是 Integer")` 编译通过（只有 unchecked 警告），
容器里混进一个 `String`，直到 `ints.get(0)` 按 `Integer` 读才抛 CCE，和 Day 1 C 段同一个病。
所以 raw type 能接受的前提是「作用域极短」：`final List l = list` 之后 3 行内用完就再也不碰。

---

### Day 2 · 2026-10-29（周四）· 异常体系

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 1：擦除擦的是实参 / 实参藏在 Signature / 为什么不能 `new T[]` | 口头过，卡住回看 Day 1 |
| 20 分钟 | `try-with-resources` 与 `finally` 里 `return` 吞异常 | `day2/TryWithResourcesProbe.java` 五段 |
| 20 分钟 | 「包装丢栈」到底丢的是什么：cause / suppressed / sneakyThrow | `day2/ExceptionChainProbe.java` 六段 |
| 10 分钟 | 输出：什么时候该包装、什么时候不该 | 本节第 8 小节 + 核心问题 Q4 / Q5 |

实测快照：`w02-generics-stream/day2-result.txt`（JDK 17.0.12）

#### 1. TWR：逆序关闭，且主异常一定活下来

关闭顺序是**后开的先关**（`open A → open B → use A → use B → close B → close A`），栈语义。
理由不是「对称好看」：B 常常依赖 A（B 是 A 上的事务 / A 是 B 的连接池），先关 A 会让 B 的 `close` 拿到坏连接。

真正值钱的是异常处理。三件事同时发生时，TWR 的排序是固定的：

| 情况 | 主异常 | close 的异常 |
| :--- | :--- | :--- |
| body 抛、close 也抛 | **body 的** | `suppressed` |
| body 正常、只有 close 抛 | **close 的** | 无（没有东西可压制） |
| 两个资源都抛 | 最外层 body 的 | 多个 suppressed |

实测：body 抛 `RuntimeException: 业务代码里炸的`，close 抛 `IllegalStateException: close 阶段自己炸了`，
catch 到的主异常是前者，`getSuppressed().length = 1`。两个信息都在。

> 🔍 顺带一个细节：`close()` 声明了受检异常时，它会被「提升」到 TWR 语句上，
> 写这段代码的人必须 `catch` 或 `throws`，编译器不兜。这在 `CheckedRes` 那段实测过。

#### 2. `finally` 里 `return`：异常被「正常返回」吃掉

```java
static int swallow() {
    try {
        throw new IllegalStateException("try 里抛的，本应往上冒泡");
    } finally {
        return 42;      // 这一行把上面的异常直接抹掉
    }
}
```

实测返回 `42`：**没有异常、没有日志、没有 suppressed、没有 cause**，调用方完全不知道出过事。
机制上不是「被 catch 了」，而是 `finally` 里的 `return` 让方法走「正常返回」路径，
正在冒泡的异常帧被直接丢弃。

同一个位置写 `return 2` 还会盖掉 `try` 里的 `return 1`（实测 `overrideReturn()` 返回 2）。
**javac 对这个写法一句话都不说**，只有 IDE 会标 `return inside finally block`。

#### 3. `finally` 里 `throw`：主异常被覆盖，连 suppressed 都没有

Java 7 之前手写资源关闭就是这个结构：

```java
BadClose bad = new BadClose();
try {
    throw new RuntimeException("业务异常 A");
} finally {
    bad.close();      // 这里抛异常，直接把 A 顶掉
}
```

实测 catch 到的是 `IllegalStateException: close 阶段自己炸了`，
`getSuppressed().length = 0`、`getCause() = null` —— **业务异常 A 彻底消失**。

线上表现：日志里只有「close 炸了」，业务异常查不到，于是去查连接池，查半天没结果。
这就是 TWR 被设计出来的**直接原因**：把「关资源」从 `finally` 挪走，由编译器生成
`try/finally + addSuppressed`，保证主异常不被覆盖。

> 一句话记法：**`finally` 里只允许做「不会抛、不会 return」的收尾**。要关资源就用 TWR。

#### 4. 包装异常：不丢栈，是把栈切成两段

「`throw new RuntimeException(e)` 丢栈」这句话只对一半。实测三组：

| 写法 | 新异常栈深度 / 栈顶 | cause | 结论 |
| :--- | :--- | :--- | :--- |
| `new RuntimeException("…", e)` | 3 / `wrapWithCause:50`（包装那一行） | `IllegalStateException`，深度 5，栈顶 `deepest` | 栈**分两段**，信息最全 |
| `new RuntimeException("…" + e.getMessage())` | 3 / 同上 | **null** | 真丢栈，连异常类型都没了 |
| 不包装，直接往上抛 | 4 / `deepest:38` | 无 | 一整段，最好排查 |

所以准确的表述是：**包装会把「栈」拆成「新异常的上半段 + cause 的下半段」**。
真正的出错点在下半段 —— 只打新异常的 message，等于只看半截。

#### 5. 日志写法决定还剩多少现场

同一个异常，三种打印方式的体积差 **57 倍**：

| 写法 | 字符数 | 行数 | 有没有位置 |
| :--- | ---: | ---: | :--- |
| `log.error("查询失败：" + e.getMessage())` | 11 | 1 | ❌ 连类名都没有 |
| `log.error("查询失败：" + e)` | 39 | 1 | ❌ 只有类名 + message |
| `log.error("查询失败", e)` / `printStackTrace` | 634 | 9 | ✅ 含 `Caused by` 两段栈 |

slf4j 的规则：`{}` 占位符最后一个参数是 `Throwable` 时自动打全栈，**不需要** `e.toString()`；
反过来写成 `log.error("... {}", e.getMessage())` 就只剩一句话。

#### 6. `cause` 与 `suppressed` 的分工

| | 语义 | 数量 | 可变性 |
| :--- | :--- | :--- | :--- |
| `cause` | 「因为什么才失败」 | 一对一 | `initCause` 只能补一次，之后不可改 |
| `suppressed` | 「失败路上还顺手炸了别的」 | 一对多 | 可 add 多个 |

实测两个坑：`addSuppressed(null)` → NPE；`addSuppressed(自己)` → `IllegalArgumentException: Self-suppression not permitted`。
TWR 关资源走的是 suppressed，正因为 close 的异常不是「失败原因」，只是「失败时的附带损害」。

#### 7. 受检 / 非受检：编译器只问一句话

**「这里有没有可能抛出受检异常，而你没交代？」** 仅此而已。

- try 里不可能抛 `IOException` 却写 `catch (IOException e)` → 编译错：
  `exception IOException is never thrown in body of corresponding try statement`
- `catch (RuntimeException e)` / `catch (Exception e)` 永远合法，编译器不管非受检异常。
- 所以「受检」约束的是**调用方必须做决定**，不是「这个错误更严重」。`Error` 和 `RuntimeException` 之外的
  `Throwable` 子类才受检。

Day 1 的擦除在这儿有一个意外的用处 —— **sneaky throw**：

```java
private static <T extends Throwable> void sneakyThrow(Throwable t) throws T { throw (T) t; }

private static void looksInnocent() {                       // 签名上一个 throws 都没有
    ExceptionChainProbe.<RuntimeException>sneakyThrow(new java.io.IOException("我是受检异常"));
}
```

实测：`looksInnocent()` 运行时真的抛出 `java.io.IOException`，`getCause() = null`（不是包装，是原样抛）。
调用方按签名以为不用处理，实际会收到没声明过的受检异常。这就是 Lombok `@SneakyThrows` 的原理，
生产代码慎用。

#### 8. 什么时候包装，什么时候不包装

**该包装（并且一定带 cause）**：跨层（DAO 的 `SQLException` 不该漏到 Controller）、
换异常体系（受检 → 非受检，比如 lambda 里没法写 `throws`）、补上下文（把 orderId 塞进 message）。

**不该包装**：同层同语义（只多一层 `Caused by`，纯噪音）、只想加日志（打完直接 `throw e`）、
用 message 替代 cause。

判断标准一句话：**包装是为了「给上层一个能处理的抽象」，不是为了「显得统一」。**

#### 9. 遗留

- ✅ 异常的性能成本 —— 2026-10-30 补完，`day2/ExceptionCost.java`，见文末「补 · 异常开销」一节。
- ⬜ `Error`（`OutOfMemoryError` / `StackOverflowError`）只提了一句，没做实验；
  「为什么 `catch (Throwable)` 危险」还没展开 —— 等 W13 JVM 内存那块一起补更顺（不补课，到那儿再说）。

#### 10. 补 · 异常开销（2026-10-30）

Day 2 说「`fillInStackTrace` 贵」但没量。补法是把同一个类跑两遍，只改一个 JVM 参数。
每用例 5 轮 × 10 万次**取最小值**（原因见下面第 3 条），JDK 17.0.12：

| 用例 | 默认 | `-XX:-StackTraceInThrowable` |
| :--- | ---: | ---: |
| A `new` 但不抛（栈深 1） | 418 ns | 37 ns |
| B `throw` + `catch`（栈深 1） | 447 ns | 31 ns |
| C `throw` + `catch`（栈深 6） | 510 ns | 31 ns |
| D 反复抛同一个缓存异常 | ≈0 ns | ≈0 ns |

三条结论，一条比一条反直觉：

1. **贵的是抓栈，不是抛。** B − A = +29 ns —— 「抛 + 捕」这个动作本身几乎免费；
   把采集关掉，B 直接掉到 31 ns，差 15 倍。整笔账单在 `Throwable` 构造函数里的 `fillInStackTrace`。
2. **栈深是线性成本。** C − B = +63 ns 就是多走 5 层栈的代价。线上真实栈深 30~50 层，
   一次异常几百纳秒到微秒级 —— 这解释了「循环里用异常控流程」为什么会把接口拖慢一个数量级。
3. **微基准不能只测一轮。** 第一版代码每用例只测一轮，同一段代码两次跑出 **44 ns 和 295 ns**
   （JIT 还停在 C1 甚至解释执行）。改成多轮取最小值后才稳定。这条比异常本身更值得记：
   **任何「一次跑出来的耗时」都不可信。**

> `-XX:-StackTraceInThrowable` 只用来做实验，生产绝对不能开 —— 排障全靠那份栈，
> 关掉之后日志里 `at xxx` 一行都没有。缓存异常（D 那行）同理：栈是第一次创建时的，
> 出事的时候只能看到一个陈年现场。

---

### Day 3 · 2026-10-30（周五）· Optional

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 2：`finally` 里 return/throw 各自怎么吞异常 / 包装是切栈不是丢栈 | 口头过，卡住回看 Day 2 |
| 25 分钟 | 三个反模式：`isPresent()+get()`、`orElse(compute())`、Optional 当字段/参数/集合元素 | `day3/OptionalAntiPatterns.java` 五段 |
| 15 分钟 | 正例：`map/flatMap/filter/orElseGet/orElseThrow` 串成一条链 | `day3/OptionalChain.java` 六段 |
| 10 分钟 | 输出：一句话讲清「Optional 是给返回值用的」 | 本节第 6 小节 + 核心问题 Q6 |

实测快照：`w02-generics-stream/day3-result.txt`（JDK 17.0.12）

#### 1. 反模式一：`isPresent() + get()` 只是换了个壳的 `!= null`

实测同一件事的两种写法：三层嵌套 `isPresent` **16 行**，`flatMap` 一条链 **4 行**，结果一样。
但这只是「丑」，还不是要害。要害在 `get()` 的报错信息：

```text
empty.get()   -> java.util.NoSuchElementException: No value present
lengthOf(null) -> java.lang.NullPointerException: Cannot invoke "String.length()" because "<parameter1>" is null
```

`No value present` **没有变量名、没有业务上下文、没有是哪一层的 Optional**。
JDK 14 给 NPE 加的 helpful message 反而把「谁是 null」说清楚了 ——
**用 Optional 把 null 包起来，报错信息是退化的，不是进化的。** 这条很少有人说。

> 🔍 补充：上面 NPE 里显示 `<parameter1>` 而不是参数名，是因为 javac 默认只带
> `-g:source,lines`，没有局部变量表。加 `-g` 才显示 `s`。想看变量名就得付 10%~20% 的 class 体积。

#### 2. 反模式二：`orElse(compute())` 白算一次

`orElse` 的参数是**方法调用**，在进 `orElse` 之前就求值完了。实测（模拟一次 20ms 远程查询）：

| 写法 | 有值时远程调用次数 | 耗时 |
| :--- | ---: | ---: |
| `cacheHit.orElse(remote("user:1"))` | **1 次** | 36.2 ms |
| `cacheHit.orElseGet(() -> remote("user:1"))` | **0 次** | 0.3 ms |

空值时两者完全一样（都是 1 次）。所以这个 bug 的特征很刁钻：
**命中缓存的路径上多打一次数据库，业务结果完全正确，监控上看只像「偶尔慢一点」。**

但也不必谈 `orElse` 色变 —— `orElse(new ArrayList<>())` 这种廉价对象，白构造一次基本无害
（对象没被使用，逃逸分析后直接消掉）。判断标准就一条：

> **参数里有没有 IO / 远程调用 / 加锁 / 大对象？有就必须 `orElseGet`。**
> 所以 Sonar 规则 `java:S5411` 才直接一刀切禁掉 `orElse` 的调用形式。

#### 3. 反模式三：Optional 当字段 / 参数 / 集合元素

三个代价，一个比一个实际：

| 位置 | 实测现象 | 为什么 |
| :--- | :--- | :--- |
| 字段 | `NotSerializableException: java.util.Optional` | `Optional` **没实现 `Serializable`**。任何走 Java 序列化的地方（Session、RPC、本地缓存、`ObjectOutputStream`）直接炸 |
| 方法参数 | `sendMail(null)` 照样 NPE，栈顶就在 `sendMail` | 签名给人的安全感是假的；而且每个调用点都要 `Optional.ofNullable(x)` 包一遍（实测 3 个调用点 = 3 次包装） |
| 集合元素 | `List<Optional<String>>` 3 个元素、2 个有效，遍历还要再判一次 | 空元素应该在**源头**过滤掉，而不是塞进集合让所有人处理 |

`Settings` 那个例子只加了一行 `Optional<String> port`，整个对象就从「能序列化」变成「不能」。
改成 `String port` + `Optional<String> portOpt()` 之后，序列化 133 字节正常 —
**这就是 JDK 官方那句「Optional 是为返回值设计的」的真实含义，不是风格洁癖。**

#### 4. 五个静默陷阱

| 写法 | 结果 | 危害 |
| :--- | :--- | :--- |
| `Optional.of(null)` | NPE | `of` 不接受 null，要用 `ofNullable`；差一个词，运行时才炸 |
| `opt.map(s -> null)` | **静默变成 `Optional.empty()`** | 不报错不提示，把一个 bug 伪装成「没有值」 |
| `opt.flatMap(s -> null)` | NPE | `flatMap` 的契约是「你必须还我一个 Optional」 |
| `empty.get()` | `NoSuchElementException: No value present` | 见第 1 小节，信息量比 NPE 还少 |
| `opt.orElse(null)` | 返回 `null` | 当场把 Optional 的契约破坏掉，下游照样 NPE |

最有意思的是第 2、3 行：**`map` 和 `flatMap` 对 null 的态度完全相反，而两者都「合理」**。
`map` 的契约是「函数可能返回 null，那我短路」，`flatMap` 的契约是「函数必须返回 Optional」。
同一个 `s -> null` 传进去，一个静默、一个爆炸 —— 这类差异只能靠实测记住，看文档看不出来。

#### 5. 正例：一条链 + `filter` 才是完整形态

`map` / `flatMap` 的判定只有一条：

> **被调的方法自己返回 `Optional` → 用 `flatMap`；返回普通值 → 用 `map`。**

实测写成 `map` 的后果不是编译错，而是得到 `Optional<Optional<Address>>`，
要 `.get().get()` 才能拿出城市 —— 然后在链尾 `orElse` 的时候才露馅。

`filter` 的作用常被低估：它把「空」和「脏」统一成同一个出口 `empty`。业务样例（订单 200 元）：

| 券 | 优惠额 | 不走 filter | 走 filter |
| :--- | ---: | ---: | ---: |
| `SAVE30` | 30 | 170 | 170 |
| `SAVE500` | 500 | **−300** | 200 |
| `EXPIRED` | 0 | 200 | 200 |
| `NOT_EXIST` | — | 200 | 200 |

```java
int pay = findCoupon(code)
        .filter(c -> c.off() > 0)          // 挡掉失效券
        .filter(c -> c.off() <= amount)    // 挡掉「券比订单大」
        .map(Coupon::off)
        .map(off -> amount - off)
        .orElse(amount);
```

两个 `filter` 让「没券 / 券失效 / 券超额」三种情况共用同一个 `orElse`。
少了第二个 `filter` 就会**算出一个负数的实付金额** —— 这是链式写法里最容易漏的一环：
`Optional` 只管「有没有」，**不管「值合不合法」，合法性得自己 `filter`。**

消费侧还有两个 JDK 9 之后才有的写法：`ifPresentOrElse(consumer, runnable)`
一次写清「有值做什么、没值做什么」，不用先 `isPresent()`；
`flatMap(Optional::stream)` 能把 `List<Optional<T>>` 里的空值直接吃掉（JDK 8 得写
`filter(Optional::isPresent).map(Optional::get)`）。

#### 6. 一句话边界

> **Optional 是「返回值的类型」，不是「所有可空位置的容器」。**

它的价值不是消除 null，而是**把「缺失」的处理收敛到链尾，只写一次**：
链上每步都是「有就继续、没有就短路」，到 `orElse` / `orElseGet` / `orElseThrow` 才落地。
所以判断一个 Optional 用得对不对，最简单的办法是看 `orElse` 出现了几次 ——
**出现多次说明你在用它做 null 检查，不是在做链式取值。**

---

### Day 4 · 2026-11-02（周一）· Lambda

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 3：`orElse` 为什么白算 / Optional 为什么不能做字段 | 口头过，卡住回看 Day 3 |
| 20 分钟 | 函数式接口五种形状（`Function` / `BiFunction` / `Consumer` / `Supplier` / `Predicate`）+ 自定义接口与 default 方法 | `day4/FunctionalShape` A / B / C / E 段 |
| 20 分钟 | 方法引用五种写法 + 变量捕获与 effectively final：为什么只能读不能改 | `day4/LambdaCapture` A / B / C / D 段 |
| 10 分钟 | 输出：`lambda` 在字节码里是一次 `invokedynamic`，不是匿名内部类 | `day4-bytecode.txt` + 本节第 7 小节 |

实测快照：`w02-generics-stream/day4-result.txt`（JDK 17.0.12，含 8 条报错原文）
字节码证据：`w02-generics-stream/day4-bytecode.txt`（javap -p / -c / -v 三段）

#### 1. 形状比名字重要：五种形态决定一切

同一份「订单」数据，五种接口各配一个业务例子：

| 形状 | 接口 | 业务里长什么样 | 关键点 |
| :--- | :--- | :--- | :--- |
| 进 1 出 1 | `Function<T,R>` | `o -> o.amount() * 80 / 100` | 变换 |
| 进 2 出 1 | `BiFunction<T,U,R>` | `(o, off) -> o.amount() - off` | 二元变换；三元就没有标准接口了，得自己写 |
| 进 1 出 0 | `Consumer<T>` | `o -> log.info(...)` | 只有副作用，所以链尾不能再用它取结果 |
| 进 0 出 1 | `Supplier<T>` | `() -> new Order("MOCK-1", ...)` | **唯一的价值是延迟**：调用时才创建 |
| 进 1 出 boolean | `Predicate<T>` | `Order::vip` | 出的不是值，是判断 —— 所以能布尔组合 |

`UnaryOperator` / `BinaryOperator` 只是把类型参数捏成同一个（`Function<T,T>` / `BiFunction<T,T,T>`），
`IntUnaryOperator` 这类是把类型写死成基本类型以省掉装箱。**形状没变，只是特化。**

> `java.util.function` 包里 43 个接口，真正不同的形状只有 5 种。所以不用背名字，
> 遇到不认识的接口先看它有几个参数、返不返回值，答案就出来了。

#### 2. 为什么 `Predicate` 必须单独存在

`Function<Order, Boolean>` 也能返回布尔，但它没有 `and` / `or` / `negate`。
实测三层 `and` 组合，第二个条件返回 `false` 时**第三个条件根本没执行** —— `and` 是短路的，
和 `&&` 语义完全一致。这就是「校验链」能写成一行、且不打无效远程调用的原因：

```java
Predicate<Order> allPass = paid.and(stocked).and(notBlacklisted);
```

所以两者的区别不是「返回值类型不同」这么表面，而是：`Function<T,Boolean>` 的语义是
**算出一个值**（这个值恰好是布尔），`Predicate<T>` 的语义是**判断一件事**，而判断可以组合。

#### 3. 方法引用五种写法：绑定与未绑定差一个参数

| 写法 | 例 | 形参个数 |
| :--- | :--- | :--- |
| 静态 | `Integer::parseInt` | 与目标方法一致 |
| 未绑定实例 | `String::length` | 目标方法 + 1（第一个参数当接收者） |
| 绑定实例 | `str::length` | 与目标方法一致（接收者已定死） |
| 构造器 | `ArrayList::new` | 与构造器一致 |
| 数组构造 | `int[]::new` | 取长度，返回数组 |

实测同一个 `length()`：`String::length` 当 `ToIntFunction<String>` 用（1 个参数），
`fixed::length` 当 `IntSupplier` 用（0 个参数）。**「未绑定」三个字说的就是接收者还没定。**

两条容易忽略的：

- 方法引用**同样靠目标类型做重载决议**。`String::valueOf` 有 9 个重载，
  写成 `Function<Object,String>` 和 `Function<Integer,String>` 会选中不同的那个。
  选错不报错，只是行为不同。
- 只有数组构造引用 `int[]::new` 会被编译成一个合成方法（`lambda$sectionA$0(int)`）——
  数组没有构造器，方法引用无从链接，只能生成。

#### 4. 捕获是值拷贝：合成方法的形参就是铁证

```java
int base = 10;
IntUnaryOperator f = x -> x + base;
// base = 20;   // 编译错：must be final or effectively final
```

`javap -p` 能看到 javac 生成了 `lambda$sectionB$1(int, int)`：第一个 `int` 是 `x`，
**第二个 `int` 就是 `base`**。也就是说「捕获」在字节码层面的真实动作是
**把变量当实参传进一个静态方法**，方法体里拿到的是副本。

副本一旦和原变量分叉，语义就说不清了，所以编译器直接禁止第二次赋值 ——
`effectively final` 不是语法洁癖，是这个物理事实的必然结果。

再看两条规律，正好解释了为什么「字段能改、局部变量不能改」：

| 捕获的东西 | 合成方法 | 为什么 |
| :--- | :--- | :--- |
| 局部变量 | `static`，多一个形参 | 值拷贝进来 |
| `this`（读字段） | **非** static，`this` 隐式传入 | 拿到的是引用，字段值在堆上，两边看同一份 |

实测把字段 `delta` 从 1 改成 100，同一个 lambda 的返回值从 `6` 变成 `105`；
而局部变量靠着 `int[] box = {10}` 也能绕过限制（`box[0] = 100` 之后结果跟着变），
因为拷的是**引用**，引用指向的数组是共享的。

> 所以准确的说法不是「lambda 里不能改局部变量」，而是
> **「不能让被捕获的引用指向别处」**。指向的对象内部随便改。

#### 5. 循环变量：`for` 不行、`for-each` 行、数组会踩坑

| 写法 | 结果 | 原因 |
| :--- | :--- | :--- |
| `for (int i = 0; ...) { s = () -> i; }` | **编译错** | `i` 从头到尾只有一个变量，还在被 `i++` 改 |
| `for (int v : list) { s = () -> v; }` | `[1, 2, 3]` 各取各的 | `for-each` 的循环变量**每次迭代是新变量** |
| `int[] idx = {0}` 绕过 | `[3, 3, 3]` 全是最后的值 | 三个 lambda 共享同一个盒子 |

第三行是最容易出线上事故的：**循环里注册回调 / 监听器**时，你以为注册了三个不同值的回调，
实际注册了三个读同一个变量的。正解排序：`for-each` → 循环内 `int snapshot = i` 再捕获 → 数组（并写注释）。

#### 6. `this` 与实例：非捕获 lambda 是单例

- `this` 的归属：匿名内部类里 `this.getClass()` 是 `LambdaCapture$1`，
  lambda 里是 `LambdaCapture` —— **lambda 没有自己的 this**，它词法上就长在外层方法里。
- 实例的复用：`nonCapturing() == nonCapturing()` 实测 **true**（`invokedynamic` 只链接一次，
  非捕获 lambda 复用同一个实例），`capturing(1) == capturing(1)` 实测 **false**（每次新建）。

两个推论，方向相反但都对：

1. 「lambda 一定比匿名类多创建对象」是**错的** —— 非捕获时它比匿名类更轻（单例 + 不落盘的类）。
2. **别拿 lambda 做 Map key / Set 元素 / 缓存键** —— 它没有 `equals`，对象身份还不稳定，
   `capturing(1).equals(capturing(1))` 是 `false`。

#### 7. 字节码：一次 `invokedynamic` + 一个隐藏类

`day4-bytecode.txt` 里三段证据连起来看：

1. `day4/` 目录下只有 6 个 `.class`，**没有一个是 `$$Lambda`** —— lambda 的类不落盘。
2. `javap -c` 里创建 lambda 的指令是 `invokedynamic #161 // InvokeDynamic #13:applyAsInt:(I)...`，
   不是 `new` + `invokespecial <init>`。匿名类在同样的位置会是后者。
3. `javap -v` 的 `BootstrapMethods` 表里，引导方法写着
   `LambdaMetafactory.metafactory`，第三个参数正是第 2 节那个 `lambda$xxx` 私有方法的方法句柄。

运行时 `getClass()` 拿到的是 `LambdaCapture$$Lambda$24/0x0000028c...`，
`isHidden()` 返回 **true**（JDK 15+ 的隐藏类），`getInterfaces()` 只有
`[interface java.util.function.IntUnaryOperator]`。

> 顺带看清了方法引用和 lambda 的关系：`Integer::parseInt` 的 `BootstrapMethods` 条目
> 直接指向 `REF_invokeStatic java/lang/Integer.parseInt`，**没有中间那层合成方法**；
> 而 `str::length` 是 `REF_invokeVirtual java/lang/String.length` + 一个捕获的接收者。
> 所以方法引用不是「lambda 的简写」，它是更直接的链接方式。

#### 8. 装箱：2 倍还是 36 倍，取决于逃没逃逸

`Function<Integer,Integer>` 与 `IntUnaryOperator` 做同一件事（`x + 1`），每用例 7 轮取最小值：

| 场景 | 装箱版 | 基本类型版 | 倍数 |
| :--- | ---: | ---: | ---: |
| 不逃逸（结果立刻拆箱求和） | 10.0 ms | 4.9 ms | **2.0 倍** |
| 逃逸（结果写进 `Integer[]`） | 23.7 ms | 0.7 ms | **36 倍** |
| 裸循环（没有 lambda 调用） | — | 5.0 ms | — |

结论有点反直觉：**「装箱很贵」这个说法只有一半对，贵的是「逃逸的装箱」。**
不逃逸时标量替换（Escape Analysis）把每次 `new Integer` 都消掉了，差距只有 2 倍；
一旦结果要进集合、进 Map、跨线程传出去，每个 `Integer` 都是一次真实分配 + 一份 GC 压力，立刻变 36 倍。

> 这正是 Day 2 §10 那条规矩的再次应用：**别背结论，要量**。
> 而且这类倍数完全依赖 JIT 优化，换台机器、换个 `-Xmx` 都可能不一样。

#### 9. 遗留

- ✅ `@FunctionalInterface` 与 `Object` 方法的关系只做了反射计数（抽象方法 1 个 / default 1 个），
  没做「接口里声明 `equals` 算不算抽象方法」的正向最小复现 —— **2026-11-02 已补**，
  见 Day 5 的 `day5/FuncInterfaceRule`（反射数出 4 个 abstract，编译器只算 1 个）。
- ⬜ `MethodHandle` / `VarHandle` 没碰，属于 W13 之后的内容（反射与动态调用那条线）。

---

### Day 5 · 2026-11-02（周一）· Stream

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 4：捕获为什么是值拷贝 / 非捕获 lambda 为什么是单例 | 口头过，卡住回看 Day 4 |
| 25 分钟 | 惰性求值：中间操作只「记账」，终止操作才真跑；用打印顺序证明 | `day5/StreamLazy` A 段 |
| 15 分钟 | 短路：`findFirst` / `anyMatch` / `limit` 在无限流上能停；对照 `sorted` / `count` | `day5/StreamLazy` B / C 段 |
| 10 分钟 | 一个流只能用一次 + `flatMap` vs `map` + parallel 的代价 | `day5/StreamOnce` + 本节第 5~7 小节 |

实测快照：`w02-generics-stream/day5-result.txt`（三个入口的完整输出）
顺带补完 Day 4 遗留：`day5/FuncInterfaceRule`（函数式接口的抽象方法到底怎么数）

#### 1. 中间操作只记账，终止操作才真跑

同一段链路，先在没接终止操作时看日志：

```java
Stream<String> pipeline = Stream.of("a", "b", "c")
        .filter(x -> { LOG.add("filter " + x); return true; })
        .map(x -> { LOG.add("map " + x); return x.toUpperCase(); });
// 此时 LOG.size() == 0 —— filter / map 那一行一次都没跑
List<String> out = pipeline.collect(Collectors.toList());
// 之后 LOG = [filter a, map a, filter b, map b, filter c, map c]
```

两个结论，第二个比第一个值钱：

1. **中间操作只是往流水线上挂节点**，不接终止操作就什么都不发生（连一行日志都没有）。
2. 真跑起来之后是 **一个元素走完全链才轮到下一个**，不是「先全部 filter 再全部 map」。
   这就是 Stream 和「两遍 for 循环」最本质的差别 —— 后者要遍历两次，前者只遍历一次。

判断一行是中间还是终止，不用背 API 表：**看返回值**。返回 `Stream` 的是中间操作，
返回 `void` / `long` / `Optional` / 集合 / 数组的是终止操作。中间操作可以有 0 到 N 个，
终止操作必须有且只有 1 个。

#### 2. 屏障：`sorted()` 会把「提前收工」作废 —— 但 `rangeClosed` 是例外

前面说过只有终止操作才真跑，但终止操作不一定要把所有元素跑完（下一节讲短路）。
`sorted` 是有状态中间操作，它必须看到全部元素才能排序，于是把「流式」切成两段、
也让所有提前收工失效。实测（`peek` 里数探测个数）：

| 链路 | 探测个数 | 说明 |
| :--- | ---: | :--- |
| `rangeClosed(1,5).findFirst()` | 1 | 正常短路 |
| `rangeClosed(1,5).sorted().findFirst()` | **1** | 居然没变多！ |
| `of(5,3,1,4,2).sorted().findFirst()` | 5 | 这次吃满了 |
| `of(1,2,2,3,3,3).distinct().count()` | 6 | `distinct` 必须记住见过的所有值 |

第二行反直觉：`sorted()` 竟然没吃掉全部 5 个。翻 JDK 源码才明白 ——
`IntStream.rangeClosed` 的 spliterator **本身就带 `SORTED` 特征**（`Streams.java:119`），
而 `SortedOps` 的 `opWrapSink` 第一件事就是：

> `// If the input is already naturally sorted and this operation also naturally sorted then this is a no-op`
> `if (StreamOpFlag.SORTED.isKnown(flags) && isNaturalSort) return sink;`（`SortedOps.java:136`）

也就是说 **`rangeClosed(...).sorted()` 是空操作**，编译器直接把你挂的节点摘了。
换成真没排序的源（`of(5,3,1,4,2)`）立刻就吃满 —— 那时它才是屏障。

> 这条值得单独记：**「`sorted` 一定全量」和「`sorted` 一定便宜」都是错的**，
> 结论取决于上游 spliterator 有没有 `SORTED` 特征。凭印象猜，这一项会差 5 倍。

再看真实成本（100 万条 `Integer`，3 轮取最小值）：

| 写法 | 耗时 | 语义 |
| :--- | ---: | :--- |
| `sorted(reverseOrder()).limit(3)` | 25 ms | 全局最大的 3 个 |
| `limit(3).sorted(reverseOrder())` | < 1 ms | 前 3 个里最大的 |

差一个数量级以上，但**两者语义不同，不能随手换位置**。要全局 top-K 又不想全排，
正解是自己维护一个大小为 K 的堆（`PriorityQueue`），而不是把 `limit` 挪到前面假装优化。

#### 3. 短路：终止操作要的答案越少，越能提前收工

| 链路 | 探测个数 | 结论 |
| :--- | ---: | :--- |
| 100 万个数 `anyMatch(i == 3)` | 3 | 命中即停 |
| **无限流** `iterate(1, i+1).filter(i % 7 == 0).findFirst()` | 7 | 无限流也能停 |
| `iterate(1, i+1).limit(3)` | 3 | `limit` 是短路中间操作 |
| 100 万个数 `filter(...).count()` | 1 000 000 | `count` 不短路 |

短路家族：`anyMatch` / `allMatch` / `noneMatch` / `findFirst` / `findAny` / `limit`。
非短路：`count` / `sum` / `average` / `max` / `min` / `collect` / `forEach`。

判断标准只有一句：**这个终止操作拿到的答案，是不是「一个元素就够」**。
`anyMatch` 是（找到了就 true），`count` 不是（必须数完）。

> 无限流只能配短路终止操作。`Stream.iterate(...).count()` 不是「慢」，是**永远跑不完**。
> 另外，只要链路里挡着一个真屏障（`sorted` / `distinct`），下面这些「只探测 3 个」就全部不成立。

#### 4. `peek` 的两个坑：`count()` 会让它静默消失

```java
Stream.of("a","b","c").peek(x -> LOG.add("peek " + x)).count();          // count=3，LOG 里 0 条
Stream.of("a","b","c").filter(x -> true).peek(...).count();              // count=3，LOG 里 3 条
```

同一段代码，加了个 `filter` 就从「一条不打印」变成「三条都打印」。原因：JDK 9+ 的 `count()`
在**元素个数能直接算出来**（源是 `SIZED`、中间没有会改变数量的操作）时会走快速路径，
**整条管道连同 `peek` 一起被跳过**。

推论：`peek` 只能用来调试，绝不能放业务逻辑 —— 它会静默不执行，连日志都不留。
要保证逐元素执行，用 `forEach` / `collect` 这类必须真消费的终止操作兜底。

#### 5. 一个流只能用一次

```text
java.lang.IllegalStateException: stream has already been operated upon or closed
```

四种场景实测：

| 场景 | 结果 | 说明 |
| :--- | :--- | :--- |
| 局部变量 `s.count()` 两次 | 第二次 ISE | 流是一次性游标 |
| **字段**缓存流，调用两次 | 第二次 ISE | API 设计事故：调用方看不出它已经烧掉 |
| 方法返回 `Stream`（每次 new） | 正常 | 每次调用都是一个新游标 |
| `Supplier<Stream>` 取两次 | 正常 | 复用「工厂」而不是「流」 |

最容易写出来的一种是「先看看几条，再 collect」：

```java
long n = once.count();                 // 日志里打印「共 n 条」
List<Order> list = once.collect(...);  // 挂了：ISE
```

**Stream 是游标，不是集合。要复用就复用数据（`List`）或工厂（`Supplier<Stream>`）。**

#### 6. `map` 换形状，`flatMap` 拆开铺平

3 个订单，共 6 件商品：

| 写法 | 结果 |
| :--- | :--- |
| `map(Order::items)` | 3 个元素，每个还是 `List<Item>` |
| `flatMap(o -> o.items().stream())` | 6 个元素，`Item` 平铺 |

一句话记法：`map` 是 `T → R`，`flatMap` 是 `T → Stream<R>` 再把所有流**拍平成一条**。
签名上就写着：`flatMap` 的入参必须返回 `Stream`，所以 `chars()`（`IntStream`）要先
`mapToObj` 装箱才能拼字符串。

一个顺手的用法，接的是 Day 3 那条「Optional 别塞进集合」：
`List<Optional<String>>` 拿 `flatMap(Optional::stream)` 一步展平，比
`filter(Optional::isPresent).map(Optional::get)` 干净得多。

#### 7. `parallel` 不是「更快」，是「用核数换单次耗时」

顺序问题先看清楚的：

| 写法 | 结果 |
| :--- | :--- |
| 串行 `forEach` | `[0..11]` 严格有序 |
| `parallel().forEach` | `[7, 8, 6, 10, 11, 9, 3, 1, 5, 4, 2, 0]` 乱序（每次不同） |
| `parallel().forEachOrdered` | `[0..11]` 保序（代价是汇合等待） |

性能（本机 12 核，3 轮取最小值）：

| 场景 | 串行 | parallel | 结论 |
| :--- | ---: | ---: | :--- |
| 2000 万个数求和（每元素几乎不做事） | 20 ms | 6 ms | 3.3 倍，但被内存带宽卡住，拿不到 12 倍 |
| 1000 个元素 × 30 轮计算 | 21 µs | 49 µs | **反向慢 2~5 倍** |
| 200 万个元素 × 30 轮计算 | 34 ms | 5 ms | 6~7 倍 |

（数字取自 `day5-result.txt`；并行计时同机重跑浮动能到 2 倍，只看量级别记绝对值。）

所以「小数据量开 parallel」是真的会变慢：拆任务 + 合并结果的开销比干活本身还大。
四条都满足才考虑 parallel：**数据量 ≥ 1 万 / 每元素计算够重 / 无共享可变状态 / 不依赖顺序**。
任一条不满足，老老实实写 for。

#### 8. 遗留

- ✅ **Day 6**（2026-11-03 已完成）：`List<User>` 过滤 → 排序 → 分组 → 统计，见下一节。
- ✅ `Collectors.toList()` 与 JDK 16 的 `Stream.toList()` 的差异已实测（Day 6 §7），
  结论比预想的多一条：**两者都是不可变，但 null 政策不一致**。
- ⬜ `Files.lines()` / `BufferedReader.lines()` 返回的流**持有文件句柄**，必须 TWR 包住；
  这条和 Day 2 的 TWR 是同一件事，等 W08 读文件 / 读大 CSV 时再实测。
- ⬜ `parallel()` 默认共用 `ForkJoinPool.commonPool()`，容器里 `availableProcessors()` 可能只报 1，
  属于 W04 线程池那条线，先记一笔。

---

### Day 6 · 2026-11-03（周二）· `List<User>` 报表：过滤 → 排序 → 分组 → 统计

> W02 的收尾，也是这一周里**最能直接搬进业务代码**的一次。
> 代码在 `w02-generics-stream/src/main/java/.../w02/day6/`，实测快照 `day6-result.txt`。

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述 Day 5：中间/终止怎么分 / 屏障什么时候不成立 / 流为什么只能用一次 | 口头过 |
| 15 分钟 | 过滤 + 多级排序（`comparing().thenComparing()`，含 null 排最后） | `UserReport` A / B 段 |
| 20 分钟 | 分组：`groupingBy` 一参 / 两参 / 三参，`counting` / `summingInt` / `averagingInt` / `mapping` | `GroupStats` C 段 |
| 10 分钟 | `toMap` 重复 key 撞一次、`teeing` 一次算两个聚合 | `CollectorTraps` D 段 |
| 5 分钟 | 输出快照 + 笔记 + 勾掉「过滤 / 排序 / 分组 / 统计」 | `day6-result.txt` |

数据是 15 条 `User`，**故意留了三个 null**：`name` 1 条、`age` 2 条、`dept` 1 条。
它们分别对应 A / B / D 段各炸一次 —— 这不是凑数，是为了让「每个 API 的 null 政策」都留下实测证据。

#### 1. 过滤：`Predicate` 的组合是**左结合**，不是布尔优先级

写业务查询时常把条件拆成几个 `Predicate` 复用，拼的时候很容易按布尔运算的直觉来：

```java
Predicate<User> q1 = isDev.or(isTest).and(active);      // 实测 6 人
Predicate<User> q2 = isDev.or(isTest.and(active));      // 实测 7 人
```

差的那 1 人是**离职的研发王强**。原因不是「`and` 优先级更高」，而是方法链**左结合**：

- `isDev.or(isTest)` 先返回一个**新** Predicate（表示并集），
- 再 `.and(active)` 是对这个并集整体做与 → `(a || b) && c`。

想表达 `a || (b && c)` 必须自己加括号：`isDev.or(isTest.and(active))`。
**这类错误不报编译错、不抛异常，只是少了几行数据**，是报表类需求最难发现的一种 bug。
结论：条件拼装完一定拿两条「边界数据」验一遍人数。

另一个常见误解是 `filter(Objects::nonNull)` 能防住 `name` 为 null ——
它对 15 条数据过滤完**还是 15 条**，因为它判的是**元素本身**，判不了元素里的字段。
真正的修法只有两种：`u.name() != null && u.name().startsWith(...)`，
或者 `Objects.toString(u.name(), "").startsWith(...)`。

> 顺带一条：`"常量".equals(x)` 这个防 NPE 的老技巧只对 `equals` 成立。
> `startsWith` / `contains` / `length` 都是「接收者方法」，字段为 null 时照样炸。

#### 2. 排序：`nullsLast` 和 `reversed()` 会互相拆台

多级排序本身没难度，写清楚「部门↑ → 薪资↓ → 年龄↑」就行：

```java
Comparator.comparing(User::dept, Comparator.nullsLast(String::compareTo))
          .thenComparing(Comparator.comparingInt(User::salary).reversed())
          .thenComparing(User::age, Comparator.nullsLast(Integer::compareTo))
```

真正要记的是两个陷阱，都实测过：

**① `reversed()` 作用在「它左边整条链」上，不是最近那一级。**

```java
// 写法一：整条链反转
comparing(User::dept, nullsLast(...)).thenComparing(User::salary).reversed()
// 结果：钱多(null dept 排到了最前), 孙浩, 赵敏, 杨帆, 周涛 ...

// 写法二：只反转薪资这一级
comparing(User::dept, nullsLast(...)).thenComparing(User::salary, Comparator.reverseOrder())
// 结果：郑凯, 刘洋, 陈静, 徐丽, 吴磊 ...（部门仍是升序）
```

写法一有两个连带后果：**部门一起降序**，而且 **`nullsLast` 被反转成了 `nullsFirst`**。
这两条叠起来就是线上「排序方向和要求反了」的常见来源。

**② 键是包装类型且可能为 null 时，`comparing` 直接 NPE。**

```
java.lang.NullPointerException: Cannot invoke "java.lang.Comparable.compareTo(Object)"
because the return value of "java.util.function.Function.apply(Object)" is null
```

`keyExtractor` 返回 null，下一句就调 `compareTo` —— 拆箱/调方法之前先炸。
修法是把 `nullsLast` 包在**内层**比较器上：`comparing(User::age, nullsLast(Integer::compareTo))`。
写成 `comparing(User::age).nullsLast(...)` 编译不过，因为 `nullsLast` 是 `Comparator` 的静态方法，
不是实例方法。

**③ 稳定性：`List.sort` 是 TimSort，稳定。** 所以「多级排序」可以拆成多次单键排序，
只要键的顺序从次要到主要倒着来。实测：研发组先按薪资升序、再按部门排，
同部门内的薪资顺序完整保留。但这条契约只对**对象数组 / List** 成立 ——
`Arrays.sort` 对基本类型数组用双轴快排，**不稳定**，别拿 `int[]` 干这事。

#### 3. `groupingBy` 的三层重载，其实是三个不同的问题

| 重载 | 回答的问题 | 典型写法 |
| :--- | :--- | :--- |
| `groupingBy(classifier)` | 怎么分组 | `groupingBy(User::dept)` → `Map<String, List<User>>` |
| `groupingBy(classifier, downstream)` | 每组算出什么 | `groupingBy(User::dept, counting())` |
| `groupingBy(classifier, mapFactory, downstream)` | 用什么 Map 装 | `groupingBy(User::dept, TreeMap::new, counting())` |

第三个重载的 `mapFactory` **夹在中间**，是最容易记错位置的一个。它解决的是「key 顺序」：

```
HashMap（默认）   -> [产品, 研发, 运营, 测试]   ← 桶分布，不可依赖
LinkedHashMap    -> [研发, 产品, 运营, 测试]   ← 首次出现顺序
TreeMap          -> [产品, 测试, 研发, 运营]   ← key 自然序
```

要给前端「部门顺序稳定」的列表，就把 `mapFactory` 写成 `TreeMap::new` 或 `LinkedHashMap::new`。
默认 HashMap 的顺序换个 JDK 版本就可能变，**不能作为业务约定**。

常用下游收集器（都实测过）：

| 下游 | 作用 | 研发组结果 |
| :--- | :--- | :--- |
| `counting()` | 计数 | 5 |
| `summingInt(User::salary)` | 求和 | 138000 |
| `averagingInt(User::age)` | 平均 | 32.0（**必须先滤 null**） |
| `mapping(User::name, joining("/"))` | 每组拼一行 | 张伟/李娜/王强/周涛/何静 |
| `mapping(User::city, toSet())` | 每组去重 | [上海, 北京, 深圳] |
| `maxBy(comparingInt(salary))` | 每组最大 | 周涛(45000)，值是 `Optional` |
| `summarizingInt(User::salary)` | 五个指标一把抓 | count/sum/min/avg/max |

两条踩出来的：

- **`averagingInt` 遇 null 直接 NPE**（`ToIntFunction` 要拆箱），而且用 `User::age` 这个方法引用时
  **报错 message 是空的**，栈顶还在 `Collectors` 内部，看不出是哪个字段。
  同一个拆箱写成 lambda 就有 message，这是今天最值得记的一条对照：

  | 写法 | message |
  | :--- | :--- |
  | `averagingInt(User::age)` | `<空>` |
  | `averagingInt(u -> u.age())` | `Cannot invoke "Integer.intValue()" because the return value of "User.age()" is null` |

  原因是**方法引用的拆箱发生在 lambda metafactory 生成的 MethodHandle 适配层**，
  那里没有字节码位置可指；lambda 版有 javac 插入的 `intValue()` 调用点，HotSpot 才能甩出 helpful message。
  所以「一个 NPE 什么都没说」不代表信息丢了，可能只是你用了方法引用。

  修法不是「null 当 0」—— 实测研发组 null 当 0 得 25.6，先滤 null 得 32.0，**差 6.4 岁**，
  因为分母从 4 变成了 5。统计场景里「补个默认值」等于改了分母，是错的。
- **`joining` 会把 null 拼成字面量 `"null"`**（`add(null)` 不抛错）。
  测试组那行变成 `吴磊/徐丽/null`，要干净就得自己 map 成 `""` 或提前过滤。

#### 4. `partitioningBy`：空组不会消失

这是它和 `groupingBy` 唯一但关键的差别。产品组 3 人全在职：

```
partitioningBy(active) -> keySet=[false, true]，false 组 = []      ← 空 List，能直接 size()
groupingBy(active)     -> keySet=[true]，false 这个 key 不存在     ← 取到 null，NPE 预备役
```

判断标准：**条件是布尔语义、且「不满足」的那组也要出现在报表里**（比如「在职 / 离职」两栏都要有数），
就用 `partitioningBy`；否则 `groupingBy`。

#### 5. `toMap` 的两种爆炸方式

```
① 重复 key  -> IllegalStateException: Duplicate key 研发 (attempted merging values 22000 and 31000)
② value 为 null -> NullPointerException，message 是空的
```

**①** 报在**收流那一刻**，栈里看不出是哪两条数据撞的，这是它最难排查的地方。
两种修法对应两种业务语义：`(a, b) -> a` 保留先出现的、`Math::max` 取最高薪资。
写 `(a,b)->a` 之前要能说出「为什么丢掉 b 是可以的」。

**②** 更反直觉：`HashMap` 本身**允许** null value（`put` 没问题），但 `toMap` 内部走的是
`map.merge(key, value, fn)`，而 `HashMap.merge` 第一句就是
`if (value == null || remappingFunction == null) throw new NullPointerException()`。
**换句话说：toMap 用的是 merge 不是 put，put 的宽松规则在 merge 这儿被收紧了。**

同一个「按 key 聚合」，两者对 null 的政策还不一样：

```
groupingBy(User::dept)  -> NPE: element cannot be mapped to a null key
toMap(User::dept, ...)  -> {null=50000}，没抛异常，null 成了一个正经的 key
```

因为 `groupingBy` 对 classifier 的返回值做了 `Objects.requireNonNull`，而 `toMap` 走 `HashMap.merge`
（HashMap 允许一个 null key）。**记这条的正确方式不是背，是想清楚「它内部用的是什么容器方法」。**

#### 6. `teeing`：一次遍历算两个聚合

```java
users.stream().collect(Collectors.teeing(
        Collectors.summingInt(User::salary),
        Collectors.counting(),
        (sum, n) -> String.format("%.1f", sum * 1.0 / n)));   // 24966.7
```

两个下游并行喂同一个元素，最后 merger 把结果合成一个值。实测与「跑两遍流」结果一致
（总额 374500 / 15 人 / 人均 24966.7），但只遍历一次。
可以和 `groupingBy` 组合算「每组人均」，也能嵌套拿三个指标。
`teeing` 是 **JDK 12** 新增，JDK 8 项目里只能 `collectingAndThen` 套自定义累加器或老实跑两遍 ——
面试问「怎么一遍流求平均」就从这儿答。

#### 7. 三种 toList：可变性 + null 政策，两条都不一样

| 写法 | 实现类 | `add()` | 含 null 元素 |
| :--- | :--- | :--- | :--- |
| `Collectors.toList()` | `ArrayList` | ✅ 成功 | ✅ 允许 |
| `Stream.toList()`（JDK 16+） | `ImmutableCollections$ListN` | ❌ UOE | ✅ **允许** |
| `Collectors.toUnmodifiableList()` | `ImmutableCollections$ListN` | ❌ UOE | ❌ **NPE** |

第三列是这次实测里最反直觉的一条，我原本以为「不可变 = 也不许 null」，写进代码后跑出来才发现错了：
**同一个 `ListN` 实现，`Stream.toList()` 放行 null、`toUnmodifiableList()` 拒绝 null。**

原因在实现路径：`Stream.toList()` 走 `listFromTrustedArrayNullsAllowed`，
`toUnmodifiableList()` 走 `listFromTrustedArray`（逐个 `Objects.requireNonNull`）。
而 `List.of("a", null)` 抛 NPE 又是另一回事 —— 那是「多个固定实参」的重载，跟单传一个数组的行为不一样。

线上事故形态：方法返回 `Stream.toList()` 的结果，调用方一句 `.add()` / `.sort()` 就
`UnsupportedOperationException`，而且**只在数据走到那行时**才炸。

#### 8. 遗留

- ⬜ `Collectors.toMap` 的 `mapFactory` 参数（四参重载）今天没碰，需要「保持顺序的 Map」时再用。
- ⬜ `groupingBy` 的 `concurrentGroupingBy`（并行流版）留到 W04 讲线程池时对照。
- ⬜ `Files.lines()` 的流持句柄那条仍在排队，等 W08。
- ✅ W02 全部 6 天完成，可以封板 —— 进入 W03 并发。

## 四、踩坑与 Bug 复盘

| 现象 | 原因 | 排查过程 | 解决 | 底层原理 |
| :--- | :--- | :--- | :--- | :--- |
| 反射往 `List<String>` 塞了 `123` 之后，打印 `list2.get(0).getClass()` 直接抛 CCE，把一个「演示程序」跑挂了 | `list2.get(0)` 的静态类型是 `String`，编译器在这儿同样插了 `checkcast String` | 看栈顶行号，正好是自己写的那行；再想「谁插的 cast」 | 改成 `Object reflected = ((List<?>) list2).get(0);` | 每个 `get` 后面都有 checkcast；把静态类型换成 `Object` / `?` 就没有 cast 了 |
| `wild.add(7)` 之后 `wild.get(0)` 打印出 `0`，看起来像「add 没生效」 | `get(0)` 拿的是最早 `fill()` 填进去的那个元素，不是刚 add 的 | 把整个 list 打出来才对上 | 改成 `get(size - 1)` | 容器语义没变，是自己在看错位置 |
| javac 报「具有相同擦除」，搜不到任何资料 | 中文 locale 的译文不是通行术语 | 换成英文 locale 重跑 | `javac -J-Duser.language=en` | 报错原文是 `name clash: ... same erasure` |
| `ErasureProof` 跑完 B 段就抛 `NoSuchMethodException: ...StringBox.set(java.lang.Object)`，后面 C~F 四段全没跑 | `Box.set` 是包私有，而 `Class.getMethod()` 只找 **public** 方法 | 异常栈顶落在 `ErasureProof.java:126`，正是那句 `StringBox.class.getMethod("set", Object.class)` | 改成 `Box.class.getDeclaredMethod("set", Object.class)`，并补打「StringBox 自己声明的方法数 = 0」 | `getMethod` = public + 含继承；`getDeclaredMethod` = 本类全部（含私有），但**不含继承**，所以只能在 `Box` 上取 |
| 以为是「两个 `?` 捕获成 CAP#1 和 CAP#2，所以不能互转」，先写进了笔记 | 想当然，没实测 | 编译一个最小用例，用 `-Xdiags:verbose` 抓原文 | 原文是 `required: int,CAP#1 / found: int,CAP#2 / reason: argument mismatch` | 捕获发生在**每次方法调用**上，同一句里的 `get` 和 `set` 各捕各的，不能当成「一个 ? 捕一次」 |
| `try (ByteArrayOutputStream bos = ...; ObjectOutputStream oos = ...) { ... } catch (NotSerializableException e) { ... }` 编译不过，报「未报告的异常错误 `IOException`」 | TWR 里资源的 `close()` 声明了 `IOException`，它会被**提升**到整个语句上；`NotSerializableException` 是它的子类，只 catch 子类不算交代 | 报错点在 `try (` 那一行的资源变量上，正是 Day 2 §7「受检 close 提升」的原话 | 补一个 `catch (java.io.IOException e)` 兜底 | TWR 的受检异常是「body 的 + 所有资源 close 的」合集，catch 必须覆盖全集 |
| 新建 `ExceptionCost.java` 后 `java -cp out ...ExceptionCost` 报 `ClassNotFoundException` | `javac @sources.txt` 只编清单里列出的文件，清单是之前 `find` 生成的 | 看 `sources.txt` 里根本没有新文件 | 重新 `find src/main/java -name "*.java" > sources.txt` 再编 | `run.bat` 每次都会重新 `dir /s /b`，手动编译时容易漏这一步 |
| 同一段异常代码，两次测量读出 `44 ns` 和 `295 ns`，差了 6 倍，差点当成「JVM 参数没生效」 | 只测了一轮，且当时 JIT 还停在 C1 / 解释执行；`-XX:-StackTraceInThrowable` 本身是生效的（栈帧数确实是 0） | 把「栈深 1 抓到的帧数」一起打出来，确认参数生效，怀疑对象转向测量方法 | 改成每用例 5 轮 × 10 万次**取最小值**，数字立刻稳定（418 / 447 / 510 ns） | JIT 分层编译让「一次测量」毫无意义；微基准要么取最小值，要么用 JMH |
| 想看 NPE 的 helpful message 里显示变量名，结果打出 `because "<parameter1>" is null` | javac 默认只生成 `-g:source,lines`，**不含局部变量表** | 对照 `javap -l` 看 LocalVariableTable 是空的 | 知道即可（加 `-g` 能显示 `s`，但 class 体积涨 10%~20%） | helpful message 靠局部变量表还原名字，没有就退化成 `<parameter1>` |
| `Function.identity()` 放进 `Map<String, Function<Order, Object>>` 报「不兼容的类型：推断类型不符合等式约束」 | `identity()` 的 `T` **没有入参可推**，只能落到 `Object`，于是推出 `Function<Object,Object>` | 报错里写着「T 扩展自 Object」，说明它没从上下文拿到 `Order` | 显式写成 `Function.<Order>identity()`，Map 的值类型改成 `Function<Order, ?>` | 泛型方法的类型参数只从**入参**推断；`identity()` 无入参，必须显式给 —— 跟 lambda 缺目标类型是同一类问题 |
| 把 `sectionB()` 写成 `static` 却在里面用 `this.delta`，报「无法从静态上下文引用非静态变量 this」 | 要演示「字段能改」就必须有实例，静态方法里没有 `this` | 三处报错都指向 `this` | 把 `sectionB` / `sectionD` 改成实例方法，`main` 里 `new LambdaCapture().run()` 串起来 | 这正是第 6 小节要讲的：lambda 里的 `this` 就是它所在方法的 `this`，方法静态则 lambda 也拿不到 `this` |
| 一个文件里放了 6 个 lambda 编译错误，javac 只报了 **5 个**，`effectively final` 那条凭空消失 | javac 先做 attribution（类型/重载/目标类型），再做 FLOW 分析；`effectively final` 属于后者，attribution 一失败就**根本不会跑** flow | 对着行号数，发现少的正好是 `base = 20` 那条；把它单独放一个文件就报出来了 | 拆成 `LambdaLimits`（5 条 attr）/ `LambdaLimits2`（break、return 值）/ `LambdaLimits3`（effectively final，单独一条） | 编译器是分阶段的，前面的阶段报错会掩盖后面阶段的错误 —— 这解释了「为什么补上一个错，又冒出来三个」 |
| 想证明「`sorted` 是屏障、会把短路作废」，结果 `rangeClosed(1,5).sorted().findFirst()` 只探测了 **1 个**，跟不加 `sorted` 一样 | `IntStream.rangeClosed` 的 spliterator 自带 `SORTED` 特征，`SortedOps` 看到上游已有序就把节点直接摘掉（空操作） | 用 `peek` 数探测个数 → 只有 1；换 `IntStream.of(5,3,1,4,2)` 立刻变 5 | 结论改写成「屏障成立的前提是上游真的无序」，并补第 3 个实验做对照 | `Streams.java:119` 的 `characteristics()` 带 `Spliterator.SORTED`；`SortedOps.java:136` 的 `if (SORTED.isKnown(flags) && isNaturalSort) return sink;` |
| 测 parallel 收益，500 万个数平方和读出「串行 1 ms / parallel 0 ms」，数字完全不能用 | 活太轻，JIT 把整个 `sum` 向量化甚至常量折叠了，测到的是「循环被消掉」的时间 | 两个数都接近 0，明显是测量问题，不是 JVM 太强 | 换成「算不出答案」的重活（每元素 30 轮乘加）+ 2000 万 / 200 万两档数据量，再补一个小数据量对照（1k 元素反而慢 2~5 倍） | 微基准老规矩（Day 2 已踩过一次）：**数字接近 0 或两档差距离谱时，先怀疑 JIT 把活消掉了** |
| `peek` 里加的日志一条都没打印，以为流没执行（甚至怀疑代码没编译进去） | JDK 9+ 的 `count()` 在源是 `SIZED`、中间没有改数量的操作时走快速路径，**整条管道包括 `peek` 一起跳过** | 在 `peek` 前加一个 `filter(x -> true)`，日志立刻出现 3 条，而 `count()` 结果不变 | 调试一律用 `forEach` / `collect` 兜底；业务逻辑绝不写进 `peek` | 「终止操作」不等于「逐元素执行」；`count` 只要个数，`SIZED` 流能直接算出来 |
| 用反射数 `Named` 接口的 abstract 方法得到 **4 个**，可编译器认为它只有 1 个 | JLS 9.8：与 `Object` 的 public 方法签名相同的接口方法（`equals` / `hashCode` / `toString`）**不计入**抽象方法 | 手写 `isObjectMethod()` 过滤后数量从 4 变 1，同时 lambda 能正常赋值 | 计数逻辑抽成工具方法，快照里同时打印「反射数」和「编译器数」 | 反射只反映「声明了什么」，函数式接口判定是编译器另一套规则 —— 又一个双视角问题（同 Day 1 的 `getParameterTypes` vs `toGenericString`） |
| 笔记和代码注释里我先写了「`a.or(b).and(c)` 等价于 `a || (b && c)`」，跑出来人数对不上才发现写反了 | `Predicate` 的 `or` / `and` 是**方法链左结合**：`isDev.or(isTest)` 先返回一个表示并集的新 Predicate，再整体 `.and(active)` | 单独写一个最小用例，用 `"x"`（短、命中 x）当输入：`x.or(y).and(z)` 给 `false`，`x.or(y.and(z))` 给 `true` | 结论改成「等价于 `(a\|\|b)&&c`」，并在 `UserReport` A2 段把两种写法并列打印（6 人 vs 7 人） | 方法链没有运算符优先级，只有求值顺序；按布尔直觉读链式 Predicate 必错，而且**不报错、只少数据** |
| 以为「`Stream.toList()` 不可变，所以也不许 null」，代码注释直接这么写了 | 想当然，把「不可变」和「null-hostile」当成同一件事 | 用 `Stream.of("a", null)` 分别喂给三种收集器，实测：`Stream.toList()` 允许（size=2），`toUnmodifiableList()` 抛 NPE | 结论改写成「可变性一个样、null 政策两个样」，并补上实现路径差异（`listFromTrustedArrayNullsAllowed` vs `listFromTrustedArray`） | 同样是 `ImmutableCollections$ListN`，但入口方法不同，校验强度就不同；**两个都是不可变 ≠ 两个行为一致** |
| `groupingBy` 平均年龄直接 `averagingInt(User::age)` 抛 NPE，message 是空的，栈顶还在 `Collectors` 内部 | 两层原因：① `ToIntFunction` 要对 `Integer` 拆箱，null 拆不了；② **`User::age` 是方法引用**，拆箱发生在 lambda metafactory 生成的 MethodHandle 适配层，没有字节码位置可指，所以 HotSpot 甩不出 helpful message | 把 `age` 打出来定位到何静 / 徐丽两条 null；顺手把同一句改成 `averagingInt(u -> u.age())` 对照，message 立刻出现 | 先 `filter(u -> u.age() != null)` 再分组 | 同一个拆箱，**lambda 版有 message、方法引用版没有** —— 排查时「NPE 什么都没说」可能只是写法问题，不一定是信息丢了 |
| 「修 NPE 就补个默认值」，把 null age 当 0 算平均 | 0 会进分母，`count` 从 4 变 5 | 两种写法并列打印：研发 25.6 vs 32.0，差 6.4 岁 | 改成先过滤再算 | 平均值 = 总和 / **有效样本数**；补默认值改变的是分母，不是「看起来更好看」的小事 |
| 对全量 15 条直接 `groupingBy(User::dept)` 抛 NPE，一开始以为是数据里混进了空字符串 | `groupingBy` 对 classifier 返回值做 `Objects.requireNonNull`，null key 一律拒绝 | 报错原文就是 `element cannot be mapped to a null key`，很明确；再用 `toMap` 对同一份数据做对照，`{null=50000}` 竟然成功 | 业务侧决定「未分配」还是过滤掉，代码里显式写 | `groupingBy` 走 `requireNonNull`、`toMap` 走 `HashMap.merge`（允许一个 null key）—— 两个「按 key 聚合」的 null 政策不一致，只能靠记内部实现 |
| `toMap(User::id, User::age)` 抛 NPE，可 `HashMap.put(id, null)` 明明没问题 | `toMap` 内部用的是 `map.merge(...)`，`HashMap.merge` 第一句就 `if (value == null) throw new NullPointerException()` | 先单测 `put` 证明容器允许，再看 `toMap` 无参重载的实现是 `toMap(k, v, throwingMerger(), HashMap::new)` | value 侧 `String.valueOf(age)` 兜底，或提前 filter | **toMap 用的是 merge 不是 put**；`put` 的宽松规则在 `merge` 这儿被收紧了，且这个 NPE 连 message 都没有 |

---

## 五、面试会怎么问

- 「泛型是编译期的」—— 追问：运行时还剩什么？（Signature 属性 + 桥方法）
- 类型擦除的证据 —— 要求现场写一行代码证明（`getClass() ==`）
- 为什么不能 `new T[]`？那框架怎么创建泛型数组？（`Array.newInstance(Class<T>)`）
- `TypeReference` / `ParameterizedTypeReference` 是怎么拿到泛型的？（匿名子类 + `getGenericSuperclass`）
- PECS 是什么？`Collections.copy` 的签名为什么两个通配符？
- 桥方法是什么？为什么反射会多出一个方法？
- 泛型重载为什么不允许？（`same erasure`）
- `List<? extends Number>` 为什么能 `add(null)`？
- `try-with-resources` 是怎么保证 close 抛异常时不覆盖主异常？（`addSuppressed`）
- 手写 `finally` 关资源会有什么问题？（`finally` 里抛异常覆盖主异常 / 里 `return` 直接吞掉）
- `finally` 里写 `return` 会怎样？（异常被丢弃，方法正常返回；javac 不报错）
- `throw new RuntimeException(e)` 会丢栈吗？（不丢，切两段；只传 message 才丢）
- `cause` 和 `suppressed` 有什么区别？谁会往里塞？（TWR 关资源塞 suppressed）
- 受检异常的边界在哪？能不能绕过？（`sneakyThrow` / 泛型擦除，Lombok @SneakyThrows）
- 什么时候该包装异常、什么时候不该？（跨层/换体系/补上下文 vs 同层纯噪音）
- 异常到底慢在哪？量过吗？（`fillInStackTrace` 走栈，实测 447 ns vs 关掉采集后 31 ns）
- `Optional` 能彻底消灭 NPE 吗？（不能：`get()` / `orElse(null)` / 当字段都还能漏）
- `orElse` 和 `orElseGet` 有什么区别？实际线上会出什么问题？（eager 求值，命中缓存时白打一次远程）
- 为什么 `Optional` 不该做字段？（没实现 `Serializable`，序列化直接炸；官方立场是「给返回值用」）
- `Optional.map` 里返回 `null` 会怎样？`flatMap` 呢？（前者静默变 empty，后者 NPE）
- 为什么 lambda 里改不了外部局部变量？（捕获值拷贝，合成方法的形参为证；字段能改是因为捕获的是 `this`）
- lambda 和匿名内部类有什么区别？（`this` 归属 / 是否生成 `.class` / 实例是否复用 / 能否序列化）
- lambda 每次都 new 一个对象吗？（非捕获复用单例，`==` 为 true；捕获的每次新建）
- `Predicate<T>` 和 `Function<T,Boolean>` 有什么不一样？（只有前者能 `and/or/negate`，且短路）
- `andThen` 和 `compose` 的顺序？（前者 g(f(x))，后者 f(g(x))；`thenComparing` 是 `andThen` 的专用版）
- `String::length` 和 `str::length` 为什么参数个数差一个？（未绑定 vs 绑定接收者）
- `Function<Integer,Integer>` 一定比 `IntUnaryOperator` 慢吗？（不逃逸 2 倍，逃逸 36 倍；差别在标量替换）
- 中间操作和终止操作怎么区分？（看返回值是不是 `Stream`；不接终止操作一行都不执行）
- 「`filter` 写了没生效」怎么排查？（先数链尾有没有终止操作；再怀疑 `sorted`/`distinct` 屏障、或者 `count()` 走快速路径跳过整条管道）
- `sorted()` 一定很贵吗？（不一定：上游 spliterator 带 `SORTED` 特征时它是空操作，实测 `rangeClosed(...).sorted()` 探测数没变）
- 一个 Stream 为什么只能用一次？（一次性游标，第二次终止操作抛 `stream has already been operated upon or closed`；要复用给 `Supplier<Stream>`）
- 无限流怎么停下来？（只能配短路终止操作：`findFirst` / `anyMatch` / `limit`；`count()` 永远跑不完）
- `map` 和 `flatMap` 有什么区别？（`T→R` vs `T→Stream<R>` 再拍平；`Optional::stream` 是展平 `List<Optional<T>>` 的顺手写法）
- `parallel()` 一定更快吗？（小数据量反而慢 2~5 倍；四条标准：量 ≥ 1 万 / 计算够重 / 无共享可变状态 / 不依赖顺序）
- `groupingBy` 的三个重载分别解决什么问题？（怎么分组 / 每组算什么 / 用什么 Map 装；`mapFactory` 夹在中间最容易记错）
- `groupingBy` 返回的 Map 顺序能依赖吗？（默认 HashMap 按桶分布；要稳定就传 `TreeMap::new` / `LinkedHashMap::new`）
- `toMap` 什么时候会抛异常？（重复 key 抛 `IllegalStateException`；value 为 null 抛**无 message** 的 NPE）
- 为什么 `HashMap.put(k, null)` 没事，`Collectors.toMap(k, v)` 就 NPE？（toMap 走 `HashMap.merge`，merge 第一句就拒绝 null value）
- `groupingBy` 和 `toMap` 对 null key 的态度为什么不一样？（前者 `Objects.requireNonNull`，后者走 `HashMap.merge` 允许一个 null key）
- `partitioningBy` 和 `groupingBy` 有什么区别？（前者一定返回 `true`/`false` 两个 key，空组给空 List；后者空组直接不存在）
- `Stream.toList()` 和 `Collectors.toList()` 有什么区别？（可变性 + null 政策都不同：前者不可变但**放行 null**，后者可变；`toUnmodifiableList()` 才拒绝 null）
- 一遍流怎么同时求总和和个数？（`Collectors.teeing`，JDK 12+；JDK 8 只能 `collectingAndThen` 或跑两遍）
- `Comparator.comparing(...).reversed()` 之后再加 `thenComparing`，谁被反转了？（`reversed()` 作用于它左边**整条链**，`nullsLast` 也会一起变成 `nullsFirst`）
- 多级排序能不能拆成多次单键排序？（能，`List.sort` 是 TimSort 稳定排序；但 `Arrays.sort` 对基本类型数组不稳定）

> 对应的口述版答案在 `04-interview/question-bank/Java基础.md`（M1 要求 +3 道，现已 24 道）。

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 2026-10-28 | 三 | 类型擦除六个实验 + PECS 五个实验 + 四类编译期报错原文 | 60 min | `ErasureProof` / `PecsDemo` / `GenericLimits` / `day1-result.txt` | ✅ |
| 2026-10-29 | 四 | 补 Day 1 遗留（GenericLimits2 + WildcardCapture / B2 崩溃修复）+ Day 2 异常体系十段实验 | 60 min | `WildcardCapture` / `TryWithResourcesProbe` / `ExceptionChainProbe` / `day2-result.txt` | ✅ |
| 2026-10-30 | 五 | Day 3 Optional（三反模式 + 五陷阱 + 一条链）+ 补 Day 2 遗留「异常开销」 | 60 min | `OptionalAntiPatterns` / `OptionalChain` / `ExceptionCost` / `day3-result.txt` | ✅ |
| 2026-11-02 | 一 | Day 4 Lambda（五形态 + 方法引用五连 + 值捕获 + invokedynamic + 装箱逃逸） | 60 min | `FunctionalShape` / `LambdaCapture` / `LambdaLimits`×3 / `day4-result.txt` / `day4-bytecode.txt` | ✅ |
| 2026-11-02 | 一 | Day 5 Stream（惰性求值 + 短路 + 屏障 + peek 坑 + 流只能用一次 + map/flatMap + parallel）+ 补 Day 4 遗留 | 60 min | `StreamLazy` / `StreamOnce` / `FuncInterfaceRule` / `day5-result.txt` | ✅ |
| 2026-11-03 | 二 | Day 6 报表：Predicate 左结合 + 多级排序（nullsLast / reversed 陷阱）+ groupingBy 三层重载 + 收集器六个陷阱 + teeing + 三种 toList 的 null 政策 | 60 min | `UserReport` / `GroupStats` / `CollectorTraps` / `User` / `UserData` / `day6-result.txt` | ✅ |
|  | 二 |  |  |  |  |
|  | 三 |  |  |  |  |
|  | 四 |  |  |  |  |
|  | 五 |  |  |  |  |
|  | 六 |  |  |  |  |
|  | 日 |  |  |  |  |

---

## 参考要点（先自己答，再对照）

**Q1 · 擦除丢了什么、留下什么**

丢的是运行时对象上的类型实参：`new ArrayList<String>()` 与 `new ArrayList<Integer>()`
的 `getClass()` 是同一个对象，连 `List` 都不剩。留的有三样：
① 声明处的类型变量 `T`（`getTypeParameters()` 能读出）；
② 子类继承时写下的实参，藏在 class 文件的 `Signature` 属性里，`getGenericSuperclass()` 能取回；
③ 编译器为对齐擦除后签名而生成的桥方法。
证据就是 `ErasureProof` 的 A 段（`==` 为 true）和 B2 段（`Box<java.lang.String>`）。

**Q2 · 为什么不能 `new ArrayList<String>[10]`**

数组在运行时带组件类型，写入时要按组件类型做检查（`ArrayStoreException`）；
泛型数组一旦允许，`List<String>[]` 就可以被当成 `List<Integer>[]` 的兄弟（数组协变），
于是能绕过编译器把 `Integer` 塞进 `List<String>`，泛型的类型安全就整个破掉了。
而擦除后运行时又无法校验「这个数组装的是 `List<String>`」，所以只能直接禁止。
要造真数组，只能把组件类型显式传进来：`Array.newInstance(type, n)`。

**Q3 · extends / super**

`? extends Number` 是生产者：读出来至少是 `Number`，可以安全当 `Number` 用；
写不进去（只能 `add(null)`），因为编译器不知道容器到底是 `List<Integer>` 还是 `List<Double>`。
`? super Integer` 是消费者：写 `Integer` 及其子类都安全，因为容器至少能装 `Integer`；
读出来只能当 `Object`，因为容器可能是 `List<Object>`。
`Collections.copy(List<? super T> dest, List<? extends T> src)` 正好一个写一个读 ——
`dest` 是消费者用 `super`，`src` 是生产者用 `extends`，`T` 由两边共同推断。

**Q4 · `finally` 里的 return / throw**

`return`：把方法引到「正常返回」路径，正在冒泡的异常被直接丢弃 —— 不 catch、不打日志、
不进 suppressed，调用方看到的是一个正常返回值。实测 `swallow()` 里 throw 了
`IllegalStateException`，方法返回 `42`，什么痕迹都没有。`finally` 里的 `return` 还会盖掉
`try` 里的 `return` 值。javac 完全不报错。

`throw`：`finally` 里抛出的异常成为最终抛出的那个，`try` 里原本的异常被**覆盖**，
既不是 cause 也不是 suppressed，彻底消失。实测 `getSuppressed().length = 0`、`getCause() = null`。

所以要 `addSuppressed`：Java 7 引入 TWR 时，编译器要生成「关资源 + 保留主异常」的代码，
而「保留」不能靠 cause（那会把因果说反：close 失败不是业务失败的原因），
也不能靠重新抛（只能抛一个）。`suppressed` 是为此新增的语义：
**主异常照常抛，附带损害挂在它身上，两个都能打到日志里。**

**Q5 · 「包装丢栈」哪里不准确**

不准确在于：`new RuntimeException(e)` 的栈**没丢**，是断成了两段。
实测包装异常栈深度 3、栈顶是包装那一行；cause 的栈深度 5、栈顶是真正出事的 `deepest()`。
原始调用链完整保存在 cause 里，只要日志打了 cause（`log.error("...", e)`），
两段栈都能看到，还多了一句业务语义。

真正丢栈的是这两类写法：
① `new RuntimeException(e.getMessage())` —— cause 直接 `null`，异常类型、栈、嵌套关系全没了；
② `log.error("... " + e.getMessage())` —— 异常对象根本没进日志，只剩一句话。

所以判断标准不是「要不要包装」，而是「包装时有没有把 cause 传下去、打日志时有没有把异常对象传下去」。

**Q6 · `isPresent()+get()` 为什么是反模式，`orElse` 又什么时候会咬人**

先纠一个常见误解：`isPresent()+get()` 的问题**不是「写法更长」**，是它把 Optional 退化成
「一个必须拆开的盒子」，而拆开的代价是报错信息变差 —— 实测 `empty.get()` 只给
`NoSuchElementException: No value present`，没有变量名、没有业务上下文、没说是哪一层；
同一场景的 NPE 反而有 helpful message（`because "<parameter1>" is null`）。
也就是说，包了一层 Optional，**错误现场是退化的**。

真正的用法是把「缺失」沿链传递、在链尾处理一次：`map` / `flatMap` / `filter` 串起来，
最后用 `orElse` / `orElseGet` / `orElseThrow` 落地。判断写得对不对，看 `orElse` 出现了几次 ——
出现多次就是在用它做 null 检查。

`orElse` 咬人的地方是**参数 eager 求值**：`orElse(remote("user:1"))` 在进 `orElse` 之前就执行了，
所以「命中缓存」这条路径上也白打一次远程（实测有值时调用 1 次、36 ms；`orElseGet` 0 次、0.3 ms）。
它不报错、结果还对，只表现为「偶尔慢一点」。判断标准：参数里有 IO / 远程 / 加锁 / 大对象就必须 `orElseGet`；
廉价小对象（`new ArrayList<>()`）则可以不管，逃逸分析会消掉。

还有一个更隐蔽的坑：`orElse` 返回类型是 `T`，所以能写 `orElse(null)` —— 当场把 Optional 的契约破坏掉；
以及 `filter` 不能省：订单 200 元的券减 500，不 `filter(c -> c.off() <= amount)` 会算出 **−300**。
**Optional 只管「有没有」，不管「值合不合法」。**

**Q7 · lambda 为什么改不了外部局部变量**

不是「规定如此」，而是**捕获的是值拷贝**，两个副本一旦分叉语义就不成立，所以干脆禁止第二次赋值。
证据在字节码层面：`int base = 10; IntUnaryOperator f = x -> x + base;` 编译后
`javap -p` 能看到 javac 生成的合成方法 `lambda$sectionB$1(int, int)` —— **第一个 `int` 是 `x`，
第二个 `int` 就是 `base`**。所谓「捕获」，真实动作是把它当实参传进一个静态方法。

由此能推出三条现象，全部实测过：

1. **字段为什么能改**：字段不是被捕获的，被捕获的是 `this`；那种 lambda 的合成方法**不是 static**，
   `this` 隐式传入。所以把 `delta` 从 1 改成 100，同一个 lambda 的返回值从 `6` 变成 `105`。
2. **`int[] box` 为什么能绕过**：拷的是引用，引用指向的数组在堆上，两边共享。
   所以准确说法是「不能让被捕获的引用指向别处」，不是「不能改局部变量」。
3. **`for` 与 `for-each` 的差别**：`for (int i...)` 里 `i` 是**整个循环共用的一个变量**，还在被改，
   所以编译错；`for-each` 的循环变量**每次迭代都是新变量**，每个 lambda 捕获到各自的副本，
   实测得到 `[1, 2, 3]`。用 `int[] idx` 绕过的写法反而会全变 `[3, 3, 3]` —— 三个 lambda 共享一个盒子。

最后一条边界：`javac` 的分阶段编译会让这条错误「消失」—— 同一个文件里只要有任何 attribution 错误
（比如 lambda 缺目标类型、`break` 出现在 lambda 里），FLOW 分析就被跳过，
`effectively final` 这条**根本不会报**。这就是「补上一个错又冒出来三个」的原因。

**Q8 · 装箱一定慢吗**

不一定，取决于**逃没逃逸**。同一个 `x + 1`，`Function<Integer,Integer>` vs `IntUnaryOperator`，
每用例 7 轮取最小值（JDK 17.0.12）：

| 场景 | 装箱版 | 基本类型版 | 倍数 |
| :--- | ---: | ---: | ---: |
| 不逃逸（结果立刻拆箱求和） | 10.0 ms | 4.9 ms | 2.0 倍 |
| 逃逸（结果写进 `Integer[]`） | 23.7 ms | 0.7 ms | 36 倍 |

原因是不逃逸时 JIT 的**标量替换**（Escape Analysis 的一部分）把每次 `new Integer` 直接消掉了 ——
对象根本没被创建，自然没有分配成本。一旦结果要进 `List`/`Map`、当 key、跨线程传出去，
每个 `Integer` 都是真实分配，再加上 GC 压力，差距立刻拉到几十倍。

所以工程上的判断标准不是「有没有装箱」，而是「这个装箱会不会逃逸」：
循环内求和、聚合统计基本可以不管；要放进集合或用 `Stream` 收集成 `List<Integer>`，才值得换
`IntStream` / `IntUnaryOperator` 这类特化版本。而且在一次 IO 是毫秒级的接口里，
省几十纳秒没有意义 —— **只有热路径才需要这一层优化**。

**Q9 · Stream 什么时候才真正执行，`filter` 为什么「没生效」**

先给结论：**中间操作只是记账，终止操作才让整条链跑起来**。区分方法看返回值 ——
返回 `Stream` 的是中间操作，返回 `void` / `long` / `Optional` / 集合的是终止操作。

证据是打印顺序，不是概念。`Stream.of("a","b","c").filter(打印).map(打印)` 这种链，
不接 `collect` 时日志条数是 **0**；接上之后日志是
`filter a, map a, filter b, map b, filter c, map c` —— **一个元素走完全链才轮到下一个**，
不是「先全部 filter 再全部 map」。这也是 Stream 和「两遍 for 循环」的本质差别：只遍历一次。

那「明明写了 filter 却没生效」有三种可能，按排查顺序：

1. **链尾没终止操作** —— 最常见，日志一条都没有。
2. **被屏障挡住** —— `sorted` / `distinct` 必须吃满全部元素，会把你以为的「提前收工」作废。
3. **终止操作走了快速路径** —— `count()` 在源是 `SIZED` 时能直接算出个数，整条管道连 `peek` 一起跳过。
   实测 `Stream.of("a","b","c").peek(log).count()` 里 `peek` 执行了 **0 次**，前面加一个
   `filter(x -> true)` 才变成 3 次。

最后一个坑值得单独记：**「有终止操作」不等于「每个元素都执行」**。所以 `peek` 只能调试，
不能放业务逻辑 —— 它会静默不执行，连日志都不留。

顺带一个反直觉的：`sorted()` 不一定是屏障。`rangeClosed(1,5).sorted().findFirst()` 实测只探测
**1 个**元素，跟不加 `sorted` 一样。因为 `IntStream.rangeClosed` 的 spliterator 自带 `SORTED` 特征，
`SortedOps` 里有一句 `if (SORTED.isKnown(flags) && isNaturalSort) return sink;` —— 节点被直接摘掉。
换成 `IntStream.of(5,3,1,4,2)` 立刻吃满 5 个。

**Q10 · 一个流为什么只能用一次，`parallel` 什么时候更慢**

`Stream` 不是集合，是**一次性游标**：它内部只有一个「已经消费到哪」的位置，第二次接终止操作就抛
`IllegalStateException: stream has already been operated upon or closed`。
三种案发现场，后两种最容易上线：

1. 局部变量 `s.count()` 两次 —— 一眼能看出来。
2. **把流缓存成字段 / 当返回值** —— 调用方看到的是 `Stream`，不知道它已经烧掉了。
3. **「先看看几条，再 collect」** —— 日志里加一句 `count()`，业务就挂了。

正解是复用**数据**或**工厂**：要么每次 `list.stream()`，要么拿 `Supplier<Stream<T>>` 每次 `get()`。
方法返回 `Stream`（方法体内每次 new 一个）是安全的，缓存成字段是不安全的 ——
区别只在「游标是不是每次新建」。

`parallel()` 方面，先纠正一个说法：它不是「更快」，是**用核数换单次耗时**，代价是 CPU 占用和可读性。
实测（12 核，3 轮取最小值）：

| 场景 | 串行 | parallel |
| :--- | ---: | ---: |
| 2000 万个数求和（每元素几乎不做事） | 20 ms | 6 ms |
| 1000 个元素 × 30 轮计算 | 21 µs | 49 µs（**更慢**） |
| 200 万个元素 × 30 轮计算 | 34 ms | 5 ms |

小数据量下拆任务 + 合并结果的开销比干活本身还大，所以慢了 2~5 倍。
四条都满足才考虑并行：**数据量 ≥ 1 万 / 每元素计算够重 / 无共享可变状态 / 不依赖顺序**。
顺序那条还要注意两个坑：`forEach` 在 parallel 下**不保序**（实测 `[7,8,6,10,11,...]`），
要顺序必须 `forEachOrdered`；而且往 `ArrayList` 里并发 `add` 本身就不安全。

**Q11 · `groupingBy` 的三层重载 / `toMap` 什么时候炸 / 两者为什么对 null 态度不同**

三层重载其实是三个独立问题，不是「同一个方法的三种写法」：

1. `groupingBy(classifier)` —— **怎么分组**，返回 `Map<K, List<T>>`。
2. `groupingBy(classifier, downstream)` —— **每组算出什么**。下游换成 `counting` / `summingInt` /
   `averagingInt` / `mapping` / `maxBy` / `summarizingInt`，就是「每组计数 / 求和 / 平均 / 拼串 / 取最大 / 五个指标」。
3. `groupingBy(classifier, mapFactory, downstream)` —— **用什么 Map 装**。注意 `mapFactory` 夹在中间，
   这个位置最容易记错。默认 `HashMap` 的 key 顺序来自桶分布，**不可依赖**；
   要稳定就传 `TreeMap::new`（自然序）或 `LinkedHashMap::new`（首次出现顺序）。

`toMap` 的两条爆炸路径：

- **重复 key** → `IllegalStateException: Duplicate key 研发 (attempted merging values 22000 and 31000)`。
  报在收流那一刻，栈里看不出是哪两条撞的。修法是补合并函数 `(a,b) -> a` 或 `Math::max` ——
  选哪个取决于业务语义，不是技术问题。
- **value 为 null** → NPE，而且 **message 是空的**。这条最容易误判成「HashMap 不能存 null」，
  但 `put(k, null)` 完全没问题。原因是 `toMap` 内部走 `map.merge(key, value, fn)`，
  而 `HashMap.merge` 第一句就是 `if (value == null) throw new NullPointerException()`。
  —— **toMap 用的是 merge，不是 put。**

两者对 null key 的政策也不一样，这点最能说明「要理解 API 就得知道它内部调了什么」：

| 写法 | null key | 原因 |
| :--- | :--- | :--- |
| `groupingBy(User::dept)` | NPE: `element cannot be mapped to a null key` | classifier 返回值过 `Objects.requireNonNull` |
| `toMap(User::dept, ...)` | 允许，`{null=50000}` | 走 `HashMap.merge`，HashMap 允许一个 null key |

另外两个配套结论：`partitioningBy` 一定返回 `true`/`false` 两个 key（空组给空 List，能直接 `size()`），
`groupingBy` 的空组压根不存在（取到 null）；三种 toList 里 **可变性是「可变 / 不可变 / 不可变」，
但 null 政策是「允许 / 允许 / 拒绝」** —— `Stream.toList()` 和 `toUnmodifiableList()` 是同一个
`ImmutableCollections$ListN`，行为却不同，别把「不可变」和「不许 null」当成一回事。
