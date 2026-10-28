# W02 · Java 泛型 · 异常 · Optional · Stream

> **第一阶段 · 打基础（W01–W12）** ｜ 预计投入 ≈ 7 小时（每天 60 分钟）
> 状态：🟨 进行中（Day 1 完成）　｜　开始日期：2026-10-28　｜　完成日期：____-__-__

> 💡 周六练习：给一个 List<User>，完成 过滤 → 排序 → 分组 → 统计。

---

## 一、本周知识点

- [x] 泛型
- [ ] 异常体系
- [ ] Optional
- [ ] Lambda
- [ ] Stream
- [ ] 过滤 / 排序 / 分组 / 统计

## 二、核心问题（能用自己的话脱稿讲出来，才算学会）

1. 「泛型只在编译期存在」这句话，运行时具体丢了什么、又留下了什么？用两个实验证明。
2. 为什么可以写 `new ArrayList[10]`，却不能写 `new ArrayList<String>[10]`？
3. `List<? extends Number>` 和 `List<? super Integer>` 各自能做什么、不能做什么？为什么 `Collections.copy` 要用两个不同的通配符？

> 自己先答一遍，再翻文末「参考要点」，别倒着来。

## 三、代码 / 实验

- 代码目录：`02-code/stage1-basics/w02-generics-stream/`
- 本周动手内容：

```text
Day 1（已完成 10-28）  类型擦除：六个实验 + PECS 五个实验 + 四类编译期报错原文
Day 2（待做）          异常体系：try-with-resources / 受检 vs 非受检 / 异常转换丢失栈
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

#### 7. 遗留

- `PecsDemo` 里两处「编译不过」的写法只能靠注释说明（`// List<Object> objs = strs;`），
  还没有像 `compile-errors/` 那样真正抓一遍报错原文 —— 下次补 `compile-errors/GenericLimits2.java`。
- 通配符捕获（`capture of ?`）只碰到了编译器报错文案，没做「用 helper 方法捕获」的正例。

---

## 四、踩坑与 Bug 复盘

| 现象 | 原因 | 排查过程 | 解决 | 底层原理 |
| :--- | :--- | :--- | :--- | :--- |
| 反射往 `List<String>` 塞了 `123` 之后，打印 `list2.get(0).getClass()` 直接抛 CCE，把一个「演示程序」跑挂了 | `list2.get(0)` 的静态类型是 `String`，编译器在这儿同样插了 `checkcast String` | 看栈顶行号，正好是自己写的那行；再想「谁插的 cast」 | 改成 `Object reflected = ((List<?>) list2).get(0);` | 每个 `get` 后面都有 checkcast；把静态类型换成 `Object` / `?` 就没有 cast 了 |
| `wild.add(7)` 之后 `wild.get(0)` 打印出 `0`，看起来像「add 没生效」 | `get(0)` 拿的是最早 `fill()` 填进去的那个元素，不是刚 add 的 | 把整个 list 打出来才对上 | 改成 `get(size - 1)` | 容器语义没变，是自己在看错位置 |
| javac 报「具有相同擦除」，搜不到任何资料 | 中文 locale 的译文不是通行术语 | 换成英文 locale 重跑 | `javac -J-Duser.language=en` | 报错原文是 `name clash: ... same erasure` |

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

> 对应的口述版答案正在往 `04-interview/question-bank/Java基础.md` 里写（M1 要求 +3 道）。

## 六、一句话总结

>

---

## 每日打卡（每天 60 分钟：复习 10 + 核心 35 + 输出 15）

| 日期 | 星期 | 学习内容 | 用时 | 产出物 | ✅ |
| :--- | :--- | :--- | ---: | :--- | :--: |
| 2026-10-28 | 三 | 类型擦除六个实验 + PECS 五个实验 + 四类编译期报错原文 | 60 min | `ErasureProof` / `PecsDemo` / `GenericLimits` / `day1-result.txt` | ✅ |
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
