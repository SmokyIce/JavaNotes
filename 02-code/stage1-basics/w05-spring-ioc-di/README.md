# W05 · Spring IOC / DI

> M2 · Spring 内核的第一周，回答一个最基础也最容易被跳过的问题：
> **Spring 到底在帮你管理什么？**
>
> 不是"帮你 new 对象"这么简单 —— 是**把「谁依赖谁」从代码里搬到配置里**，
> 于是换实现不动业务、加依赖不动入口。

## 一、怎么跑（本机没有 Maven）

依赖是手工放在 `lib/` 的 6 个 jar，版本 **5.3.39**：

| jar | 大小 | 作用 |
| :--- | ---: | :--- |
| `spring-core` | 1,490,485 B | 基础设施 + 自带 CGLIB（`@Configuration` 要被它代理） |
| `spring-beans` | 706,684 B | `BeanFactory` / 三级缓存 / `doCreateBean` |
| `spring-context` | 1,276,093 B | `AnnotationConfigApplicationContext` / `@ComponentScan` |
| `spring-expression` | 294,249 B | SpEL（本次没直接用到，但 context 依赖它） |
| `spring-aop` | 385,322 B | context 的编译期依赖 |
| `spring-jcl` | 25,161 B | Spring 5 自带的日志桥（**不是** commons-logging） |

```bash
./run.sh                      # 默认跑 SpringWiring
./run.sh ManualWiring         # 短名 → day1.ManualWiring
./run.sh day1.ContainerQuestions
```

`run.bat` 同款。`run.sh` 里 classpath 是 `out;lib/*`（Windows 分隔符是 `;`，Git Bash 下也别改成 `:`）。

> `pom.xml` 留给 W05 收尾时补（装了 Maven 再加），现在补了也验证不了 —— 不写没验证过的东西。

## 二、目录

```text
w05-spring-ioc-di/
├── lib/                         6 个 jar，见上表
├── run.sh / run.bat             javac -cp "lib/*" + java -cp "out;lib/*"
├── day1-result.txt              三段实验的完整实测（355 行）
├── day1-spring-source.txt       Spring 源码取证（行号能和栈对上）
└── src/main/java/com/newt/java48/w05/
    ├── support/
    │   ├── Trace.java           「谁 new 的、什么时候 new 的」打点
    │   └── Src.java             数源码里 new Xxx( 出现几次
    └── day1/
        ├── ManualWiring.java    A 三处各自 new / B 集中装配 / C 量化改动点
        ├── SpringWiring.java    A 谁+何时 / B 单例 / C 装配行数 / D 换实现 / E @Lazy / F prototype
        ├── ContainerQuestions.java  Q1 不写注解 / Q2 注解对 new 无效 / Q3 循环依赖 / Q4 单例池
        ├── AppConfig.java       @Configuration + @ComponentScan
        ├── MemoryConfig.java    「换实现」版配置：排除 jdbc，@Bean 出 memory
        ├── app/                 同一套业务类（Controller / Job / Test / Service / Repo / Calc）
        └── cycle/               FieldA⇄FieldB（字段注入）、CtorA⇄CtorB（构造器注入）
```

## 三、三段实验各自证明了什么

### 1. `ManualWiring` —— 没有 Spring 时，"装配"这笔账记在谁头上

| 段 | 现象 | 数字 |
| :--- | :--- | :--- |
| A 三个入口各自 `new` | 功能全对，但造出**三份仓储** | 仓储实例 = **3**，哈希三个都不同 |
| B 装配收进 `main` | 实例共享了 | 仓储实例 = **1**，哈希三个相同 |
| C 数源码 | 换实现 = A 段 3 处 / B 段 1 处；加依赖 = A 段 3 处 / B 段 3 处 | 见 `day1-result.txt` |

关键不是"3 处代码改动很麻烦"，是 **3 个连接池**：每处 `new` 都带走一份缓存、一份连接、一份计数。
A 段那三个不同哈希就是证据。

### 2. `SpringWiring` —— 同一套类，装配代码 0 行

| 段 | 结论 | 证据 |
| :--- | :--- | :--- |
| A 谁 / 何时 new | `refresh()` 期间就 new 完了 | 构造行 `t=+312~338 ms`，全在「容器创建完成 `+354 ms`」之前 |
| A 调用者 | 不是我们，是 `BeanUtils.instantiateClass` | 栈上同时命中 `createBeanInstance` / `preInstantiateSingletons` / `getSingleton` |
| B 单例 | 两次 `getBean` 同一对象 | `s1 == s2 : true`，哈希相同 |
| C 装配行数 | 入口代码里 `new X(` = **0** | 数源码；`MemoryConfig` 里唯一那 1 个 `new` 是"选哪个实现" |
| D 换实现 | 三个入口一行没改 | 仓储从 `jdbc` 变 `memory`，`OrderService` 构造器打 `repo=memory` |
| E `@Lazy` | 推迟到第一次 `getBean` | `containsSingleton("lazyBean")` false → true |
| F `prototype` | 每次给新的，且不进单例池 | `p1 == p2 : false`，累计实例 2，`containsSingleton("prototypeTask") = false` |

C 段那张表是全周最该记住的东西：

```text
SpringWiring.java（三个入口）   0 / 0 / 0      ← 入口一个 new 都没有
AppConfig.java（扫包）          0 / 0 / 0
MemoryConfig.java              0 / 0 / 0      ← 唯一那行 new 是 new MemoryOrderRepository()
```

### 3. `ContainerQuestions` —— 四个不试一次就说不准的问题

| 问题 | 实测 |
| :--- | :--- |
| Q1 不写 `@Component` | 直接要：`NoSuchBeanDefinitionException: No qualifying bean of type '...$MissingDep' available`；被别人依赖：`UnsatisfiedDependencyException` → 内层同一个 `NoSuchBeanDefinitionException`（带 `expected at least 1 bean which qualifies as autowire candidate`） |
| Q2 注解对 `new` 出来的对象 | `new NotManaged()` 后 `@Autowired` 字段 = **null**；`ctx.register(NotManaged.class)` 后 = **jdbc** |
| Q3a 循环依赖（字段注入） | **能过**。构造时两边都是 null，之后 `b.a == a` |
| Q3b 循环依赖（构造器注入） | **过不去**。`UnsatisfiedDependencyException(ctorA)` → `(ctorB)` → `BeanCurrentlyInCreationException: Requested bean is currently in creation: Is there an unresolvable circular reference?` |
| Q4 单例池 | 定义 13 / 单例 19；「有定义没实例」= `lazyBean`、`prototypeTask`；「有实例没定义」= 8 个容器自己的东西（`environment` / `systemProperties` / `messageSource` …）。取过 prototype + lazy 后：定义仍 13、单例 20（只多了 lazyBean） |

Q4 那个 **19 → 20** 是原型作用域最干净的证据：原型造完连账都不记。

## 四、四个坑

1. **`Src.count` 把自己数进去了。** 第一版把针脚 `"new JdbcOrderRepository("` 写在统计代码里当字符串常量，
   于是 `SpringWiring`（明明 0 个 new）数出 3、`AppConfig` 也数出 3。改成 `countNew(src, "JdbcOrderRepository")`
   在 `Src` 内部拼针脚才干净 —— **自指统计必须把针脚藏到被数文件之外**。
2. **`GenericApplicationContext` 只能 `refresh()` 一次。** 先 `new AnnotationConfigApplicationContext(AppConfig.class)`
   再 `register(...)` + `refresh()` → `IllegalStateException: GenericApplicationContext does not support multiple refresh attempts`。
   正确姿势：`new AnnotationConfigApplicationContext()` → `register(配置类, 额外类)` → `refresh()`。
3. **`getBeanDefinitionNames()` + `getBean(name)` 会把 `@Lazy` 和 prototype 提前造出来。**
   第一版 SpringWiring 的 A 段就是这么把 LazyBean 在枚举名单时造掉的，直接导致 E 段"refresh 时没造"这个结论不成立。
   改成读 `getBeanDefinition(name).getBeanClassName()` —— 读注册表不触发构造。
4. **栈上的 `getSingleton:234` 不是查三级缓存那一支。** 它是 `getSingleton(beanName, ObjectFactory)` 里的
   `singletonFactory.getObject()`，即"真正开始造对象"的那句。查三级缓存的是另一个重载（180 行起）。
   同名两个重载，行号差 50 多行，看栈时别对错。

## 五、待办（W05 剩下的六天）

- [ ] Bean 生命周期（`BeanPostProcessor` / `@PostConstruct` / `InitializingBean` 三者的顺序）
- [ ] Bean 作用域全量（singleton / prototype 已做，补 request / session 的存在意义）
- [ ] `@Autowired` vs `@Resource` vs 构造器注入的取舍
- [ ] 补 `pom.xml`（装了 Maven 之后）
