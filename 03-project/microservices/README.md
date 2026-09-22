# microservices · 微服务版

第 25 周起，把 `task-core` 按边界拆开。

| 模块 | 职责 |
| :--- | :--- |
| `gateway/` | 统一入口：路由、鉴权、限流 |
| `task-service/` | 任务领域 |
| `user-service/` | 用户领域 |
| `common/` | 公共依赖：DTO、工具、异常 |
