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
├── compile-errors/
│   └── GenericLimits.java                  故意编译不过：擦除的四条硬限制
└── src/main/java/com/newt/java48/w02/day1/
    ├── ErasureProof.java                   六个实验：类同一性 / 反射签名 / CCE 位置 / 泛型数组 / 桥方法 / 硬限制
    └── PecsDemo.java                       五个实验：不变性 / ? extends / ? super / copy / Stack pushAll
```

> `compile-errors/` 不在 `src/main/java` 下，不会被 `run.bat` 编译 —— 否则整个工程编不过。
> 要单独复现报错：
>
> ```bash
> cd compile-errors
> javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors GenericLimits.java
> ```
>
> 加 `-J-Duser.language=en` 是因为中文 locale 下 `name clash: ... same erasure` 会被译成
> 「具有相同擦除」，搜不到任何资料 —— 报错原文比译文有用得多。

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
   `StringBox extends Box<String>` 能拿到 `String`，但 `StringBox.class.getMethod("set")`
   的泛型参数类型还是 `T`（方法自己的签名里没有 String）。
   Spring 的 `ParameterizedTypeReference`、Jackson 的 `TypeReference` 全靠这一招 ——
   匿名子类 + 泛型父类，本质是"用继承把实参写进 class 文件"。

## 读代码的顺序

1. 先跑 `run.bat`，只看 A 段三行。`getClass() ==` 为 `true` 这一行，比读十遍"泛型是编译期的"都值。
2. 再读 `sectionC()`：注意 `String s = list.get(1)` 和 `List raw = list` 两个断点，
   理解"类型检查被搬到编译期"到底意味着什么。
3. 然后读 `sectionD()`：`(T[]) new Object[n]` 与 `Array.newInstance` 的对比，
   是"运行时需要组件类型"的唯一证据。
4. 最后跑 `PecsDemo`，按 `invariance → extends → super → copy → Stack` 的顺序读，
   每读一个就猜一下"这行会不会编译过"，再跑。
