# Docker 常用命令速查（W23 / W24）

```bash
# 镜像
docker images
docker build -t task-core:1.0 .
docker rmi <image>

# 容器
docker ps -a
docker run -d --name task-core -p 8080:8080 task-core:1.0
docker logs -f task-core
docker exec -it task-core sh / bash
docker stop/start/rm <container>

# 数据与网络
docker volume ls
docker network ls

# 编排（项目部署就用这个）
docker compose up -d
docker compose ps
docker compose logs -f
docker compose down
```

## W24 目标状态

```text
docker compose up -d
  ├── mysql
  ├── redis
  ├── rabbitmq
  └── task-core
```

**一条命令把整套环境跑起来**，并能从日志定位问题。
