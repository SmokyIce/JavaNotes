# Redis 常用命令速查（W12 / W16 / W17）

```bash
# 通用
SET k v EX 60 / GET k / DEL k / EXISTS k / TTL k / PERSIST k
KEYS *            # 生产慎用，用 SCAN
SCAN 0 MATCH task:* COUNT 100

# 数据结构
HSET h f v / HGETALL h / HINCRBY h f 1
LPUSH / LRANGE / RPOP
SADD / SMEMBERS / SISMEMBER
ZADD / ZRANGE / ZINCRBY          # 排行榜 / 延时队列

# 分布式锁
SET lock:task:1 <uuid> NX PX 30000
# 解锁必须用 Lua 保证原子：比较 value 后再 DEL

# 监控
INFO memory / INFO stats / SLOWLOG GET 10 / MEMORY USAGE k
```

## 缓存三兄弟

| 问题 | 现象 | 方案 |
| :--- | :--- | :--- |
| 穿透 | 查不存在的数据，每次都打到 DB | 空值缓存 / 布隆过滤器 |
| 击穿 | 热点 key 过期瞬间大量请求打到 DB | 互斥锁 / 逻辑过期 |
| 雪崩 | 大量 key 同时过期 | 过期时间加随机值 / 多级缓存 |
