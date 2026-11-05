# JVM 排查命令速查（W15 / W38）

| 命令 | 用途 | 常用写法 |
| :--- | :--- | :--- |
| `jps -l` | 找 Java 进程 PID | `jps -lvm` |
| `jstat -gcutil <pid> 1000` | 观察 GC 频率与各代使用率 | 每秒一次 |
| `jstack <pid>` | 导出线程栈 | `jstack <pid> > stack.txt` |
| `jmap -heap <pid>` | 堆内存概览 |  |
| `jmap -dump:live,format=b,file=a.hprof <pid>` | 堆转储 | 配合 MAT 分析 |
| `jinfo -flags <pid>` | 查看 JVM 参数 |  |

## 典型场景

### CPU 100%

```bash
top -Hp <pid>              # 找到最耗 CPU 的线程
printf '%x\n' <tid>        # 线程 id 转 16 进制
jstack <pid> | grep -A 30 <16进制tid>
```

### 内存暴涨 / OOM

```bash
jstat -gcutil <pid> 1000   # 老年代是否持续增长
jmap -dump:live,format=b,file=a.hprof <pid>
```

### 线程卡死

```bash
jstack <pid> | grep -A 20 "BLOCKED"
```

### 死锁（2026-11-05 补，W03 Day 2 实测）

先看有没有，再看是谁：

```bash
jstack <pid> > stack.txt
grep -n "Found one Java-level deadlock" -A 25 stack.txt
```

`jstack` 的死锁段是**成对**的，读法就一句：A 的 `waiting to lock` 等于 B 的 `locked`，反之亦然。

```text
Found one Java-level deadlock:
"D1-A-then-B":
  waiting to lock monitor 0x...b620 (object 0x...f060, a java.lang.Object),
  which is held by "D1-B-then-A"
"D1-B-then-A":
  waiting to lock monitor 0x...bfc0 (object 0x...f050, a java.lang.Object),
  which is held by "D1-A-then-B"
...
	at com.xxx.Service.method(Service.java:65)
	- waiting to lock <0x...f060> (a java.lang.Object)
	- locked <0x...f050> (a java.lang.Object)
Found 1 deadlock.
```

看三样：**谁等谁** → **业务代码第几行** → **末尾有没有 `Found N deadlock`**
（没有这行就别按死锁查，看是 `waiting on condition` 还是 IO）。

程序化检测（可接监控报警，比人眼靠谱）：

```java
ThreadMXBean mx = ManagementFactory.getThreadMXBean();
long[] ids = mx.findDeadlockedThreads();                  // null = 没检出
ThreadInfo[] infos = mx.getThreadInfo(ids, true, true);   // 后两个 true 才带锁信息
```

**三个易错点**

| 易错 | 真相 |
| :--- | :--- |
| `isAlive()==true` 就是在干活 | BLOCKED 的线程一直活着，但不推进 |
| `blockedCount` 是等了多久 | 是「进过几次阻塞」，不是毫秒 |
| 一把锁也能死锁 | 至少要两把 + 反向获取（循环等待） |

**jstack 抓不到的两个原因**

1. 抓晚了 —— 进程已经退出，报 `<pid>: no such process`；
2. **Windows / Git Bash 里 `$!` 拿到的是 MSYS 的 pid，不是 Windows 原生 pid**，
   必须用程序自己打印的 `ProcessHandle.current().pid()`（或 `jps -l`）。

**破解优先级**：全局定序（实测 2000 轮零重试）> `tryLock` + 随机退避（不加退避会活锁，
实测 5 次里 4 次跑不完）> 缩小粒度 / 不在锁里做 IO。
