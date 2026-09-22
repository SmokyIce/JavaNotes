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
