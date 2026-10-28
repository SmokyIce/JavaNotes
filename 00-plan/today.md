---
title: 今天做什么
date: 2026-10-28
weekday: 周三
milestone: M1 · Java 地基
task: W02 Day 1 — 类型擦除的证据 + PECS；顺带补齐 W01 遗留
status: 已完成
---

# 今天 · 2026-10-28（周三）· 已完成

> M1 的第一天。开工前先做一次「遗留清扫」，再进今天的正题 —— 昨天的封板不算完，
> 有两处漏的（W02 工程还是空目录、W01 Day 1 的验收清单没勾）。

## 一、先补的遗留

| 遗留 | 状态 | 处理 |
| :--- | :--- | :--- |
| `w02-generics-stream/` 只有一个 `.gitkeep` | ✅ 已补 | 按 Maven 布局建 `src/main/java`，补 `run.bat` / `run.sh` / `README.md`，和 W01 一套脚本风格 |
| `W01-Java集合.md` Day 1 验收清单 5 项没勾 | ✅ 已补 | 重跑 `MyArrayList`（JDK 17.0.12）确认 `10→15→22→33→49→73→109`、删 95 个后不缩容，5 项补勾并记录实测 |
| `W02` 笔记还是空模板（状态 ⬜、日期空白） | ✅ 已补 | 状态改 🟨 进行中、开始日期 2026-10-28、Day 1 整节写完、打卡首行补上 |
| 题库 M1 要求「泛型 +3」 | ✅ 已补 | `Java基础.md` 从 12 题扩到 15 题（擦除 / 泛型数组与 TypeReference / PECS） |

## 二、今天的正题：类型擦除 + PECS

代码在 `02-code/stage1-basics/w02-generics-stream/src/main/java/com/newt/java48/w02/day1/`：

| 文件 | 内容 |
| :--- | :--- |
| `ErasureProof.java` | 6 段：类同一性 / 反射签名（含 B2 实参藏在哪）/ CCE 位置 / 泛型数组 / 桥方法 / 四条硬限制 |
| `PecsDemo.java` | 5 段：不变性 / `? extends` / `? super` / `copy` 双通配符 / `Stack.pushAll`+`popAll` |
| `compile-errors/GenericLimits.java` | 故意编不过：javac 一次报 6 个错，原文收进 `day1-result.txt` |
| `day1-result.txt` | 三段实测快照（JDK 17.0.12） |

## 三、今天挖到的三个点

1. **擦掉的是「实参」，留下的是「声明」。** `List<String>` 和 `List<Integer>` 的 `getClass()`
   `==` 为 `true`，但 `Box.class.getTypeParameters()` 还能读出 `T`。
   实参唯一能捞回来的地方是**子类的 Signature 属性**：`StringBox.class.getGenericSuperclass()`
   拿到 `Box<java.lang.String>`。Spring 的 `ParameterizedTypeReference`、Jackson 的 `TypeReference`
   全靠这一招 —— 匿名子类的作用就是「把实参写进 class 文件」。

2. **CCE 抛在 `get` 那一行，不是 `add` 那一行。** 原始类型 `raw.add(42)` 无声通过，
   直到 `String s = list.get(1)` 才炸（`ErasureProof.java:134`），因为编译器在每个 `get`
   后面插 `checkcast`。写 demo 时自己也踩了一次：反射塞完 `123` 之后直接
   `list2.get(0).getClass()`，整个演示程序被这个 CCE 打断 —— 正好证明了这一点。

3. **`(T[]) new Object[n]` 是骗局，`Array.newInstance(Class<T>, n)` 才是真数组。**
   前者真实类型还是 `[Ljava.lang.Object;`，赋给 `String[]` 立刻 CCE；
   后者拿到的是 `[Ljava.lang.String;`，往里塞 `Integer` 会抛 `ArrayStoreException`。
   这条链的终点是：**擦除把类型变成了必须由调用方显式告知的参数。**

## 四、环境备忘

- 编译：`find src/main/java -name "*.java" > sources.txt && javac -encoding UTF-8 -d out @sources.txt`
- 跑 Demo：`java -Dfile.encoding=UTF-8 -cp out com.newt.java48.w02.day1.ErasureProof`
- 抓 javac 报错原文：`javac -J-Duser.language=en -encoding UTF-8 -d ../out-errors GenericLimits.java`
  —— 中文 locale 会把 `same erasure` 译成「具有相同擦除」，搜不到资料。

---

# 下一个时间盒（60 分钟）

**W02 Day 2：异常体系**，重点不是背「受检 / 非受检」，而是三件能写出 bug 的事：

| 时间 | 做什么 | 产出 |
| -: | :--- | :--- |
| 10 分钟 | 复述今天：擦除擦的是实参 / 实参藏在 Signature / 为什么不能 `new T[]` | 口头过，卡住回看笔记 Day 1 |
| 20 分钟 | `try-with-resources` 与 `finally` 里 `return` 吞异常（写一个 `close()` 抛异常的例子） | `day2/TryWithResourcesProbe.java` |
| 20 分钟 | 受检/非受检的真实分界线：`throw new RuntimeException(e)` 为什么会丢原始栈（`addSuppressed` vs 覆盖栈） | `day2/ExceptionChainProbe.java` |
| 10 分钟 | 输出：一句话讲清「什么时候该包装异常，什么时候不该」 | 笔记「核心问题」 |

> 优先做第二个 20 分钟。`finally` 里 `return` 会**静默吞掉** `try` 里抛的异常，
> 这个坑在写资源清理代码时最容易踩，而且只有跑出来才信。
