# lab · 复现实验

把「工作中的 Bug」变成可复现的最小 Demo。**每个实验都要能一句话说清：现象 → 原因 → 原理 → 面试怎么答。**

| 实验 | 对应周 | 说明 |
| :--- | :--- | :--- |
| `01-transaction-self-invocation/` | W07 | this.xxx() 自调用导致 @Transactional 失效 |
| `02-transaction-propagation/` | W07 | REQUIRED / REQUIRES_NEW / NESTED 行为对比 |
| `03-async-transaction-boundary/` | W08 | @Async 与事务边界：为什么事务不跨线程继承 |
| `04-slow-sql-explain/` | W10 | 制造慢 SQL → EXPLAIN → 建索引 → 对比 |
| `05-jvm-gc-troubleshooting/` | W15 | CPU 高 / 内存高 / GC 卡顿排查复现 |
| `06-redis-distributed-lock/` | W17 | 普通 Redis 锁为什么会出问题 + Redisson 对比 |
| `07-mq-reliability/` | W19 | 消息丢失 / 重复消费 / 死信队列复现 |

> 规则：修完 Bug 别关 IDE，额外花 20–30 分钟追问「为什么、底层原理、还有哪些情况会失效、面试怎么问」。
