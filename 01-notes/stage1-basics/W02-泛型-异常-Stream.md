# W02 · Java 泛型 · 异常 · Optional · Stream

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟨 进行中（Day 1 / Day 2 完成）　｜　开始日期：2026-10-28　｜　完成日期：____-__-__

> 💡 周六练习：给一个 List<User>，完成 过滤 → 排序 → 分组 → 统计。

---

## 一、本周知识点

- [x] 泛型
- [x] 异常体系
- [ ] Optional
- [ ] Lambda
- [ ] Stream
- [ ] 过滤 / 排序 / 分组 / 统计

## 二、核心问题（能用自己的话脱稿讲出来，才算学会）

1. 「泛型只在编译期存在」这句话，运行时具体丢了什么、又留下了什么？用两个实验证明。
2. 为什么可以写 `new ArrayList[10]`，却不能写 `new ArrayList<String>[10]`？
3. `List<? extends Number>` 和 `List<? super Integer>` 各自能做什么、不能做什么？为什么 `Collections.copy` 要用两个不同的通配符？
4. `finally` 里的 `return` 和 `throw` 分别会怎样处理 `try` 里正在冒泡的异常？为什么 `try-with-resources` 要专门发明 `addSuppressed`？
5. 说 `throw new RuntimeException(e)` 会「丢栈」，这句话哪里不准确？什么样的写法才是真的丢栈？

> 自己先答一遍，再翻文末「参考要点」，别倒着来。

## 三、代码 / 实验

- 代码目录：`02-code/stage1-basics/w02-generics-stream/`
- 本周动手内容：

```text
Day 1（已完成 10-28）  类型擦除：六个实验 + PECS 五个实验 + 四类编译期报错原文
Day 1 补（已完成 10-29）通配符捕获：capture 报错原文 + helper 正例 + JDK 的 raw 选择
Day 2（已完成 10-29）  异常体系：TWR 关闭与 suppressed / finally 吞异常 / 包装丢栈真相 / sneakyThrow
Day 3（待做）          Optional：正确用法与三个反模式
Day 4（待做）          Lambda：函数式接口 + 方法引用 + 变量捕获（ effectively final ）
Day 5（待做）          Stream：中间/终止操作、惰性求值、短路
Day 6（待做）          List<User> 过滤 → 排序 → 分组 → 统计
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

- `Error`（`OutOfMemoryError` / `StackOverflowError`）只提了一句，没做实验；
  「为什么 `catch (Throwable)` 危险」还没展开 —— 等 W13 JVM 内存那块一起补更顺。
- 异常的性能成本只说了「`fillInStackTrace` 贵」，没量。可以用 `-XX:-StackTraceInThrowable`
  跑一个 100 万次的循环对比 —— 顺手能当 JVM 参数的第一个实验。

---

## 四、踩坑与 Bug 复盘

| 现象 | 原因 | 排查过程 | 解决 | 底层原理 |
| :--- | :--- | :--- | :--- | :--- |
| 反射往 `List<String>` 塞了 `123` 之后，打印 `list2.get(0).getClass()` 直接抛 CCE，把一个「演示程序」跑挂了 | `list2.get(0)` 的静态类型是 `String`，编译器在这儿同样插了 `checkcast String` | 看栈顶行号，正好是自己写的那行；再想「谁插的 cast」 | 改成 `Object reflected = ((List<?>) list2).get(0);` | 每个 `get` 后面都有 checkcast；把静态类型换成 `Object` / `?` 就没有 cast 了 |
| `wild.add(7)` 之后 `wild.get(0)` 打印出 `0`，看起来像「add 没生效」 | `get(0)` 拿的是最早 `fill()` 填进去的那个元素，不是刚 add 的 | 把整个 list 打出来才对上 | 改成 `get(size - 1)` | 容器语义没变，是自己在看错位置 |
| javac 报「具有相同擦除」，搜不到任何资料 | 中文 locale 的译文不是通行术语 | 换成英文 locale 重跑 | `javac -J-Duser.language=en` | 报错原文是 `name clash: ... same erasure` |
| `ErasureProof` 跑完 B 段就抛 `NoSuchMethodException: ...StringBox.set(java.lang.Object)`，后面 C~F 四段全没跑 | `Box.set` 是包私有，而 `Class.getMethod()` 只找 **public** 方法 | 异常栈顶落在 `ErasureProof.java:126`，正是那句 `StringBox.class.getMethod("set", Object.class)` | 改成 `Box.class.getDeclaredMethod("set", Object.class)`，并补打「StringBox 自己声明的方法数 = 0」 | `getMethod` = public + 含继承；`getDeclaredMethod` = 本类全部（含私有），但**不含继承**，所以只能在 `Box` 上取 |
| 以为是「两个 `?` 捕获成 CAP#1 和 CAP#2，所以不能互转」，先写进了笔记 | 想当然，没实测 | 编译一个最小用例，用 `-Xdiags:verbose` 抓原文 | 原文是 `required: int,CAP#1 / found: int,CAP#2 / reason: argument mismatch` | 捕获发生在**每次方法调用**上，同一句里的 `get` 和 `set` 各捕各的，不能当成「一个 ? 捕一次」 |

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

> 对应的口述版答案在 `04-interview/question-bank/Java基础.md`（M1 要求 +3 道，现已 17 道）。

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 2026-10-28 | 三 | 类型擦除六个实验 + PECS 五个实验 + 四类编译期报错原文 | 60 min | `ErasureProof` / `PecsDemo` / `GenericLimits` / `day1-result.txt` | ✅ |
| 2026-10-29 | 四 | 补 Day 1 遗留（GenericLimits2 + WildcardCapture / B2 崩溃修复）+ Day 2 异常体系十段实验 | 60 min | `WildcardCapture` / `TryWithResourcesProbe` / `ExceptionChainProbe` / `day2-result.txt` | ✅ |
|  | 一 |  |  |  |  |
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
