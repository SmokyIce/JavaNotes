# MySQL EXPLAIN 字段速查（W10）

| 字段 | 关注点 |
| :--- | :--- |
| `id` | 执行顺序，越大越先执行 |
| `select_type` | SIMPLE / PRIMARY / SUBQUERY / DERIVED |
| `type` | 访问类型，性能：`system > const > eq_ref > ref > range > index > ALL`，**出现 ALL 要警觉** |
| `possible_keys` | 可能用到的索引 |
| `key` | 实际用到的索引，为 NULL 说明没走索引 |
| `key_len` | 索引使用长度，判断联合索引用了几列 |
| `rows` | 预估扫描行数，越大越慢 |
| `filtered` | 过滤后剩余比例 |
| `Extra` | `Using index`（覆盖索引，好）、`Using where`、`Using filesort`（排序没走索引）、`Using temporary`（用了临时表） |

## 排错顺序

```text
type 是不是 ALL？ → key 是不是 NULL？ → rows 是否过大？
→ Extra 有没有 filesort / temporary？ → 建索引后再对一次
```

## 案例记录

| 序号 | SQL | 改前 type/key/rows | 改后 | 优化手段 |
| ---: | :--- | :--- | :--- | :--- |
| 1 |  |  |  |  |
| 2 |  |  |  |  |
| 3 |  |  |  |  |
| 4 |  |  |  |  |
| 5 |  |  |  |  |
| 6 |  |  |  |  |
| 7 |  |  |  |  |
| 8 |  |  |  |  |
| 9 |  |  |  |  |
| 10 |  |  |  |  |

> 第 3 个月要交出「MySQL 索引实验 + 10 条 EXPLAIN 案例」，这张表就是交付物。
