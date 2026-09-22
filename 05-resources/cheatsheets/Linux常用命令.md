# Linux 常用命令速查（W22）

| 分类 | 命令 |
| :--- | :--- |
| 进程 / 负载 | `top` `top -Hp <pid>` `ps -ef | grep java` `uptime` |
| 内存 | `free -h` |
| 磁盘 | `df -h` `du -sh *` |
| 日志 | `tail -f app.log` `grep -n "ERROR" app.log` `less` |
| 文件 | `find / -name "*.log" -mtime +7` |
| 网络 | `ss -lntp` `netstat -anp` `curl -v` `ping` |
| 杀进程 | `kill -15 <pid>` `kill -9 <pid>` |
| 端口占用 | `lsof -i:8080` |

## 排查套路

```bash
# 1. 看整体负载
top
# 2. 定位进程
ps -ef | grep java
# 3. 看线程
top -Hp <pid>
# 4. 看端口与连接
ss -lntp
# 5. 看日志
tail -f logs/app.log | grep ERROR
```
