# W05 · Spring IOC / DI

> M2 · Spring 内核的第一周，回答一个最基础也最容易被跳过的问题：
> **Spring 到底在帮你管理什么？**
>
> 不是"帮你 new 对象"这么简单 —— 是**把「谁依赖谁」从代码里搬到配置里**，
> 于是换实现不动业务、加依赖不动入口。

## 一、怎么跑（本机没有 Maven）

依赖是手工放在 `lib/` 的 7 个 jar，Spring 版本 **5.3.39**：

| jar | 大小 | 作用 |
| :--- | ---: | :--- |
| `spring-core` | 1,490,485 B | 基础设施 + 自带 CGLIB（`@Configuration` 要被它代理） |
| `spring-beans` | 706,684 B | `BeanFactory` / 三级缓存 / `doCreateBean` / `initializeBean` |
| `spring-context` | 1,276,093 B | `AnnotationConfigApplicationContext` / `@ComponentScan` / `CommonAnnotationBeanPostProcessor` |
| `spring-expression` | 294,249 B | SpEL（本次没直接用到，但 context 依赖它） |
| `spring-aop` | 385,322 B | context 的编译期依赖 |
| `spring-jcl` | 25,161 B | Spring 5 自带的日志桥（**不是** commons-logging） |
| `javax.annotation-api` | 26,586 B | Day 2 新增：`@PostConstruct` / `@PreDestroy`（JDK 11+ 已移出 JDK） |

```bash
./run.sh                      # 默认跑 SpringWiring
./run.sh ManualWiring         # 短名 → day1.ManualWiring
./run.sh day1.ContainerQuestions
./run.sh day2.LifecycleOrder  # 带包名 → day2 的入口必须写全
./run.sh day2.PostProcessorTrace
```

`run.bat` 同款。`run.sh` 里 classpath 是 `out;lib/*`（Windows 分隔符是 `;`，Git Bash 下也别改成 `:`）。

> `pom.xml` 留给 W05 收尾时补（装了 Maven 再加），现在补了也验证不了 —— 不写没验证过的东西。

## 二、目录

```text
w05-spring-ioc-di/
├── lib/                         7 个 jar，见上表
├── run.sh / run.bat             javac -cp "lib/*" + java -cp "out;lib/*"
├── day1-result.txt              三段实验的完整实测（355 行）
├── day1-spring-source.txt       Spring 源码取证（行号能和栈对上）
├── day2-result.txt              Day 2 两段实验的实测（140 行）
├── day2-spring-source.txt       Day 2 源码取证（7 段：initializeBean / 销毁登记 / BPP 注册…）
└── src/main/java/com/newt/java48/w05/
    ├── support/
    │   ├── Trace.java           「谁 new 的、什么时候 new 的」打点
    │   ├── Src.java             数源码里 new Xxx( 出现几次
    │   └── Stacks.java          「这个回调是谁调的」——从栈上读
    ├── day1/
    │   ├── ManualWiring.java    A 三处各自 new / B 集中装配 / C 量化改动点
    │   ├── SpringWiring.java    A 谁+何时 / B 单例 / C 装配行数 / D 换实现 / E @Lazy / F prototype
    │   ├── ContainerQuestions.java  Q1 不写注解 / Q2 注解对 new 无效 / Q3 循环依赖 / Q4 单例池
    │   ├── AppConfig.java       @Configuration + @ComponentScan
    │   ├── MemoryConfig.java    「换实现」版配置：排除 jdbc，@Bean 出 memory
    │   ├── app/                 同一套业务类（Controller / Job / Test / Service / Repo / Calc）
    │   └── cycle/               FieldA⇄FieldB（字段注入）、CtorA⇄CtorB（构造器注入）
    └── day2/
        ├── LifecycleOrder.java  A 六回调时间线 / B 逆序销毁 / C prototype / D 推断 close()
        ├── LifecycleBean.java   ①构造 ③@PostConstruct ④afterPropertiesSet ⑤⑥⑦⑧⑨ 全挂在身上
        ├── LifecycleBpp.java    插进 ② 与 ⑥ 两个钩子
        ├── LifecycleConfig.java @Bean(initMethod, destroyMethod) + static 声明后处理器
        ├── LifecycleRepo.java   属性注入的观测点（构造时 null / 初始化时有值）
        ├── Alpha.java / Beta.java            依赖与被依赖 → 销毁顺序
        ├── PrototypeBean.java   prototype 的销毁回调计数（恒 0）
        ├── AutoCloseBean.java   有 close() 但没声明 destroyMethod → 被推断调用
        ├── ManualOffBean.java   destroyMethod = "" → 关掉推断
        ├── PostProcessorTrace.java A 计数表 / B @PostConstruct 栈 / C 三追问 / D Ordered
        ├── TraceBpp.java        会计数的后处理器（自己带 @PostConstruct 做对照）
        ├── EarlyDependent.java  被后处理器 @Autowired → 提前创建 → 拿不到代理
        ├── PlainBean.java       普通 Bean，2/2 的样本
        └── StackDumpBean.java   在 @PostConstruct 里打印栈
```

## 三、五段实验各自证明了什么

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

### 4. `LifecycleOrder` —— 六个回调的顺序，打出来而不是背出来

| 段 | 结论 | 证据（实测） |
| :--- | :--- | :--- |
| A 六回调 | ①构造 → ②BPP.before → ③`@PostConstruct` → ④`afterPropertiesSet` → ⑤`initMethod` → ⑥BPP.after | `+375.7 / 386.7 / 389.0 / 389.5 / 390.2 / 390.5 ms`，`refresh()` 在 `+408.7 ms` 才返回 |
| A 属性注入 | 没有回调，只能用两个点夹住 | 构造器里 `repo=null`，`@PostConstruct` 里 `repo=jdbc` |
| B 销毁 | 逆序 + **依赖方先死** | `Beta 构造 → Alpha 构造`；`close()` 时 `Alpha @PreDestroy → Beta @PreDestroy` |
| C prototype | 销毁回调被调 **0 次** | 取两个实例（`p1 == p2 : false`），`close()` 后计数仍是 0 |
| D 推断 | `@Bean` 默认推断 `close()` | `AutoCloseBean.close()` 1 次 vs `ManualOffBean.close()` 0 次（`destroyMethod = ""`） |

### 5. `PostProcessorTrace` —— 后处理器对谁生效、对谁不生效

| 段 | 结论 | 证据（实测） |
| :--- | :--- | :--- |
| A 计数表 | 正常 Bean **2/2**，后处理器自己与提前创建的 Bean **0/0** | `plainBean` / `lifecycleRepo` 2/2；`bppFirst` / `bppSecond` / `earlyDependent` 0/0 |
| A 同一批 | 同批后处理器**互相看不见** | `PostProcessorRegistrationDelegate:257~265`：先全部 `getBean` 收集，再统一注册 |
| B 栈 | `@PostConstruct` 是后处理器调的 | `InitDestroyAnnotationBeanPostProcessor:157` ← `applyBeanPostProcessorsBeforeInitialization:440` ← `initializeBean:1796` |
| C 提前创建 | 拿不到代理，启动日志有官方告警 | `is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)` |
| D 排序 | `Ordered` 决定调用顺序 | `plainBean` 上的序列 `[first.before, second.before, first.after, second.after]` |

反过来也验证了：后处理器**自己**的 `@PostConstruct` 照样执行（两行都打出来了）——
因为 `CommonAnnotationBeanPostProcessor` 是 `PriorityOrdered`，比我们早注册。

## 四、七个坑

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
5. **`bppSecond` 的计数是 0/0，不是 1/1。** 我第一版文案写的是「至少能被 `bppFirst` 处理一次」，
   实测打脸。原因：同一批后处理器是「先全部 `getBean` 收集到局部 List，再统一注册」，
   收集的时候谁都没进列表。**「顺序」在 BPP 上要分「阶段」和「批内」两层看。**
6. **`@PostConstruct` 编译不过**（`程序包 javax.annotation 不存在`）。JDK 11+ 已把它移出 JDK，
   `lib/` 必须补 `javax.annotation-api`。注意 Spring 5 用 `javax.*`，Spring 6 才换 `jakarta.*`；
   下错包不会报错，只会「注解不执行」——静默失效最难查。
7. **栈帧行号指向调用点，不是方法声明行。** `postProcessBeforeInitialization` 声明在 154，
   栈上是 157（`metadata.invokeInitMethods` 那句）。**引行号前先跑一次**，别照抄博客。

## 五、待办（W05 剩下的六天）

- [x] Bean 生命周期（Day 2 完成：六回调 + 后处理器 + 三个追问）
- [ ] Bean 作用域全量（singleton / prototype 已做，补 request / session 的存在意义）
- [ ] `@Autowired` vs `@Resource` vs 构造器注入的取舍
- [ ] 补 `pom.xml`（装了 Maven 之后）
