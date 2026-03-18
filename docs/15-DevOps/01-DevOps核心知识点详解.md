# DevOps 核心知识点详解 - Linux、Shell、Docker、Jenkins CI/CD、Nginx

## 一、Linux 核心知识

### 1.1 文件与目录操作

#### 基本命令

```bash
ls -la          # 列出所有文件（含隐藏文件），显示详细信息
cd /path        # 切换目录
pwd             # 显示当前目录
mkdir -p a/b/c  # 递归创建目录
rm -rf dir      # 强制递归删除目录（危险！）
cp -r src dst   # 递归复制目录
mv old new      # 移动/重命名
find / -name "*.log"  # 查找文件
```

### 1.2 文件权限（重要！）

#### 权限表示

```
-rwxr-xr-x  1  user  group  size  date  filename
│└┬┘└┬┘└┬┘
│ │  │  └── 其他用户权限 (r-x = 5)
│ │  └───── 所属组权限   (r-x = 5)
│ └──────── 所有者权限   (rwx = 7)
└────────── 文件类型 (-=普通文件，d=目录，l=链接)
```

#### 权限计算

- r(读) = 4
- w(写) = 2
- x(执行) = 1

**示例：**

```bash
chmod 755 file    # 设置权限为 rwxr-xr-x
chmod +x file     # 添加执行权限
chown user:group file  # 修改文件所有者
```

### 1.3 进程管理

#### 常用命令

```bash
ps -ef           # 查看所有进程
ps aux           # BSD 风格，显示更多信息（CPU、内存占用）
ps -ef | grep java  # 查找 Java 进程
top              # 实时查看进程（按 q 退出）
htop             # 更友好的 top（需安装）
kill PID         # 发送 SIGTERM 信号（优雅终止）
kill -9 PID      # 发送 SIGKILL 信号（强制终止）
nohup cmd &      # 后台运行，忽略挂断信号
jobs             # 查看后台任务
fg %1            # 将后台任务 1 调到前台
```

### 1.4 网络命令

```bash
netstat -tlnp    # 查看监听端口（t=TCP, l=监听，n=数字，p=进程）
ss -tlnp         # 更快的 netstat 替代
lsof -i:8080     # 查看占用 8080 端口的进程
curl http://...  # 发送 HTTP 请求
wget http://...  # 下载文件
ping host        # 测试网络连通性
traceroute host  # 追踪路由
ifconfig         # 查看网卡信息
ip addr          # 更现代的 ifconfig 替代
```

### 1.5 日志查看

```bash
cat file         # 查看整个文件
head -n 100 file # 查看前 100 行
tail -n 100 file # 查看后 100 行
tail -f file     # 实时追踪文件（查看日志常用）
less file        # 分页查看（支持上下翻页）
grep "ERROR" file  # 搜索关键字
grep -C 5 "ERROR" file  # 显示匹配行的前后 5 行
zcat file.gz | grep ...  # 查看压缩日志
```

### 1.6 系统信息

```bash
df -h            # 磁盘使用情况（-h 人性化显示）
du -sh *         # 当前目录各文件大小
free -h          # 内存使用情况
uptime           # 系统运行时间、负载
uname -a         # 系统内核信息
cat /etc/os-release  # 操作系统版本
whoami           # 当前用户
w                # 查看登录用户
```

---

## 二、Shell 脚本

### 2.1 Shell 脚本基础

#### 脚本开头

```bash
#!/bin/bash      # 指定解释器（Shebang）
set -e           # 遇到错误立即退出
set -x           # 打印执行的命令（调试用）
```

### 2.2 变量

#### 变量定义

```bash
NAME="value"
echo $NAME       # 使用变量
echo ${NAME}     # 推荐写法，避免歧义
echo "${NAME}"   # 双引号内可解析变量
echo '${NAME}'   # 单引号内不解析变量
```

#### 特殊变量

| 变量            | 含义              |
|---------------|-----------------|
| `$0`          | 脚本名称            |
| `$1, $2, ...` | 位置参数            |
| `$#`          | 参数个数            |
| `$@`          | 所有参数（作为独立参数）    |
| `$*`          | 所有参数（作为一个字符串）   |
| `$?`          | 上一条命令的返回值（0=成功） |
| `$$`          | 当前 Shell 的 PID  |

### 2.3 条件判断

#### if 语句

```bash
if [ -f "file.txt" ]; then
    echo "文件存在"
elif [ -d "dir" ]; then
    echo "目录存在"
else
    echo "不存在"
fi
```

#### 常用判断条件

```bash
[ -f file ]      # 文件存在且是普通文件
[ -d dir ]       # 目录存在
[ -e path ]      # 路径存在（文件或目录）
[ -z "$var" ]    # 变量为空
[ -n "$var" ]    # 变量非空
[ "$a" = "$b" ]  # 字符串相等（注意空格！）
[ $a -eq $b ]    # 数字相等
[ $a -gt $b ]    # 大于
[ $a -lt $b ]    # 小于
```

### 2.4 循环

#### for 循环

```bash
for i in 1 2 3 4 5; do
    echo $i
done

for file in *.txt; do
    echo "处理：$file"
done
```

#### while 循环

```bash
while [ $count -lt 10 ]; do
    echo $count
    count=$((count + 1))
done

# 逐行读取文件
while read line; do
    echo $line
done < file.txt
```

### 2.5 函数

```bash
function deploy() {
    local env=$1    # 局部变量
    echo "部署到 $env 环境"
    return 0        # 返回状态码
}

deploy "production"
result=$?           # 获取返回值
```

### 2.6 实用脚本：Java 应用部署脚本

```bash
#!/bin/bash
set -e

APP_NAME="my-app"
JAR_FILE="${APP_NAME}.jar"
PID_FILE="${APP_NAME}.pid"

start() {
    if [ -f "$PID_FILE" ]; then
        echo "应用已在运行"
        return 1
    fi
    nohup java -jar $JAR_FILE > app.log 2>&1 &
    echo $! > $PID_FILE
    echo "启动成功，PID: $(cat $PID_FILE)"
}

stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "应用未运行"
        return 1
    fi
    kill $(cat $PID_FILE) && rm -f $PID_FILE
    echo "停止成功"
}

case "$1" in
    start)  start ;;
    stop)   stop ;;
    restart) stop; start ;;
    *) echo "Usage: $0 {start|stop|restart}" ;;
esac
```

---

## 三、Docker 容器化

### 3.1 Docker 核心概念

#### 镜像（Image）

- 只读模板，包含运行应用所需的一切
- 由多个层（Layer）组成，共享相同层

#### 容器（Container）

- 镜像的运行实例
- 可读写层 + 镜像层
- 生命周期：创建 → 运行 → 暂停 → 停止 → 删除

#### 仓库（Registry）

- 存储和分发镜像的服务
- Docker Hub（公共）、Harbor（私有）

### 3.2 Docker 常用命令

#### 镜像操作

```bash
docker images            # 列出本地镜像
docker pull nginx:latest # 拉取镜像
docker build -t myapp:v1 .  # 构建镜像
docker push myapp:v1     # 推送镜像
docker rmi image_id      # 删除镜像
docker tag old:v1 new:v1 # 给镜像打标签
docker save -o file.tar image  # 导出镜像
docker load -i file.tar  # 导入镜像
```

#### 容器操作

```bash
docker run -d -p 8080:80 --name web nginx  # 后台运行容器
  -d: 后台运行
  -p: 端口映射（主机：容器）
  --name: 容器名称
  -v: 挂载卷（主机目录：容器目录）
  -e: 环境变量
  --restart=always: 自动重启

docker ps                # 查看运行中的容器
docker ps -a             # 查看所有容器
docker start/stop/restart container  # 启停容器
docker rm container      # 删除容器
docker exec -it container bash  # 进入容器
docker logs -f container # 查看容器日志
docker inspect container # 查看容器详情
docker cp file container:/path  # 复制文件到容器
```

### 3.3 Dockerfile 编写

#### Spring Boot 应用 Dockerfile 示例

```dockerfile
# 基础镜像
FROM openjdk:17-jdk-slim

# 维护者信息
LABEL maintainer="itzixiao@example.com"

# 设置工作目录
WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime

# 复制 JAR 包（利用 Docker 层缓存）
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx512m"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 3.4 Docker Compose

#### docker-compose.yml 示例

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
    depends_on:
      - mysql
      - redis
    networks:
      - app-network

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - app-network

  redis:
    image: redis:7
    networks:
      - app-network

volumes:
  mysql-data:

networks:
  app-network:
```

#### Docker Compose 命令

```bash
docker-compose up -d      # 启动所有服务
docker-compose down       # 停止并删除容器
docker-compose ps         # 查看服务状态
docker-compose logs -f    # 查看日志
docker-compose exec app bash  # 进入容器
docker-compose restart app    # 重启服务
```

### 3.5 Docker 网络

#### 网络模式

- **bridge（默认）**：容器通过虚拟网桥通信
- **host**：容器使用主机网络（性能最好，无隔离）
- **none**：无网络
- **container**：共享其他容器的网络

```bash
docker network ls                     # 列出网络
docker network create mynet           # 创建网络
docker run --network=mynet ...        # 指定网络
```

---

## 四、Jenkins CI/CD

### 4.1 CI/CD 概念

#### CI（持续集成）

- 频繁合并代码到主干
- 自动构建、自动测试
- 快速发现问题

#### CD（持续交付/部署）

- **持续交付**：自动化发布到测试/预发环境，手动发布到生产
- **持续部署**：全自动发布到生产环境

### 4.2 Jenkins Pipeline

#### Jenkinsfile（声明式Pipeline）

```groovy
pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'registry.example.com'
        IMAGE_NAME = 'my-app'
    }
    
    stages {
        stage('拉取代码') {
            steps {
                git branch: 'main', 
                    url: 'https://github.com/xxx/xxx.git'
            }
        }
        
        stage('编译') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('单元测试') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('构建镜像') {
            steps {
                sh """
                    docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER} .
                    docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}
                """
            }
        }
        
        stage('部署到测试环境') {
            steps {
                sh """
                    ssh user@test-server "docker pull ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}"
                    ssh user@test-server "docker-compose up -d"
                """
            }
        }
        
        stage('部署到生产环境') {
            when {
                branch 'main'
            }
            input {
                message '是否部署到生产环境？'
            }
            steps {
                sh 'deploy-to-production.sh'
            }
        }
    }
    
    post {
        success {
            echo '构建成功！'
        }
        failure {
            echo '构建失败！'
            // 发送通知
        }
    }
}
```

### 4.3 Jenkins 触发方式

1. **手动触发**：在 Jenkins 界面点击 Build
2. **定时触发（Cron 表达式）**：
   ```groovy
   triggers {
       cron('H 2 * * *')  // 每天凌晨 2 点
   }
   ```
3. **Git Webhook 触发**：代码推送时自动触发构建
4. **轮询 SCM**：
   ```groovy
   triggers {
       pollSCM('H/5 * * * *')  // 每 5 分钟检查一次
   }
   ```

---

## 五、Nginx 反向代理与负载均衡

### 5.1 Nginx 核心概念

#### 正向代理 vs 反向代理

**正向代理**：代理客户端，服务端不知道真实客户端

```
客户端 → [代理] → 服务端
例如：VPN、翻墙
```

**反向代理**：代理服务端，客户端不知道真实服务端

```
客户端 → [Nginx] → 后端服务器集群
例如：负载均衡、隐藏后端服务器
```

### 5.2 Nginx 常用命令

```bash
nginx                    # 启动
nginx -s stop            # 快速停止
nginx -s quit            # 优雅停止
nginx -s reload          # 重新加载配置（热更新）
nginx -t                 # 测试配置文件语法
nginx -v                 # 查看版本
nginx -V                 # 查看版本和编译参数
```

### 5.3 Nginx 配置结构

```nginx
# 全局块
worker_processes auto;     # 工作进程数（建议设为 CPU 核心数）

# events 块
events {
    worker_connections 1024;  # 单个 worker 最大连接数
    use epoll;                # 使用 epoll 模型（Linux）
}

# http 块
http {
    include mime.types;
    default_type application/octet-stream;
    
    # 日志格式
    log_format main '$remote_addr - $request - $status';
    access_log /var/log/nginx/access.log main;
    
    # server 块（虚拟主机）
    server {
        listen 80;
        server_name example.com;
        
        # location 块（路由规则）
        location / {
            root /usr/share/nginx/html;
            index index.html;
        }
    }
}
```

### 5.4 反向代理配置

```nginx
server {
    listen 80;
    server_name api.example.com;
    
    location / {
        proxy_pass http://127.0.0.1:8080;  # 后端服务地址
        
        # 传递请求头
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 30s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

### 5.5 负载均衡配置

```nginx
# 定义后端服务器组
upstream backend {
    # 负载均衡策略
    # 默认：轮询（round-robin）
    # ip_hash;           # IP 哈希（会话保持）
    # least_conn;        # 最少连接
    # random;            # 随机
    
    server 192.168.1.101:8080 weight=3;  # 权重
    server 192.168.1.102:8080 weight=2;
    server 192.168.1.103:8080 weight=1 backup;  # 备份服务器
    server 192.168.1.104:8080 down;      # 标记为不可用
}

server {
    listen 80;
    server_name api.example.com;
    
    location / {
        proxy_pass http://backend;  # 使用 upstream
    }
}
```

### 5.6 负载均衡策略对比

| 策略         | 说明                            |
|------------|-------------------------------|
| 轮询（默认）     | 依次分配请求，适合服务器性能相近              |
| weight     | 按权重分配，权重大的分配更多请求              |
| ip_hash    | 按 IP 哈希，同一 IP 访问同一服务器（会话保持）   |
| least_conn | 分配给连接数最少的服务器                  |
| url_hash   | 按 URL 哈希，同一 URL 访问同一服务器（缓存友好） |

### 5.7 location 匹配规则

优先级从高到低：

```nginx
location = /api { }        # 精确匹配，优先级最高
location ^~ /static/ { }   # 前缀匹配，匹配后停止搜索正则
location ~ \.php$ { }      # 正则匹配（区分大小写）
location ~* \.(jpg|png)$ { }  # 正则匹配（不区分大小写）
location /api { }          # 前缀匹配
location / { }             # 默认匹配
```

---

## 六、高频面试题汇总

### Linux 面试题

#### **问题 1：如何查看某个端口被哪个进程占用？**

**答：**

1. `netstat -tlnp | grep 8080`
2. `ss -tlnp | grep 8080`
3. `lsof -i:8080`

#### **问题 2:chmod 755 是什么意思？**

**答：**
755 = rwxr-xr-x

- 7(rwx): 所有者可读、可写、可执行
- 5(r-x): 所属组可读、可执行
- 5(r-x): 其他用户可读、可执行

#### **问题 3：如何实时查看日志文件？**

**答：**

```bash
tail -f /var/log/app.log
# 或查看最后 100 行并持续追踪
tail -n 100 -f /var/log/app.log
```

#### **问题 4:kill 和 kill -9 的区别？**

**答：**

- `kill PID`: 发送 SIGTERM 信号，进程可以捕获并优雅退出
- `kill -9 PID`: 发送 SIGKILL 信号，强制终止，无法被捕获

#### **问题 5：如何让程序后台运行，且关闭终端后继续运行？**

**答：**

```bash
nohup java -jar app.jar > app.log 2>&1 &
# nohup: 忽略挂断信号
# > app.log: 输出重定向
# 2>&1: 错误输出重定向到标准输出
# &: 后台运行
```

### Shell 面试题

#### **问题 6:$?表示什么？**

**答：**
上一条命令的返回值（退出状态码）

- 0: 成功
- 非 0: 失败

#### **问题 7：单引号和双引号的区别？**

**答：**

- 单引号：原样输出，不解析变量
  ```bash
  echo '$HOME' → 输出 $HOME
  ```
- 双引号：解析变量
  ```bash
  echo "$HOME" → 输出 /home/user
  ```

#### **问题 8：如何在 Shell 中判断文件是否存在？**

**答：**

```bash
if [ -f "/path/to/file" ]; then
    echo "文件存在"
fi
# -f: 是否是普通文件
# -d: 是否是目录
# -e: 路径是否存在
```

### Docker 面试题

#### **问题 9:Docker 镜像和容器的区别？**

**答：**

- 镜像：只读模板，包含运行应用所需的文件系统和配置
- 容器：镜像的运行实例，有独立的可写层
- 类比：镜像是类，容器是对象

#### **问题 10:COPY 和 ADD 的区别？**

**答：**

- COPY: 只复制本地文件到镜像
- ADD: 除了复制，还支持：
    1. 自动解压 tar 包
    2. 从 URL 下载文件
- 推荐使用 COPY，更明确

#### **问题 11:CMD 和 ENTRYPOINT 的区别？**

**答：**

- CMD: 容器启动时的默认命令，可被 docker run 参数覆盖
- ENTRYPOINT: 固定的执行程序，docker run 参数作为参数传递
- 最佳实践：ENTRYPOINT 定义可执行程序，CMD 定义默认参数

#### **问题 12：如何减小 Docker 镜像体积？**

**答：**

1. 使用更小的基础镜像（alpine）
2. 多阶段构建（multi-stage build）
3. 合并 RUN 命令，减少层数
4. 清理不必要的文件（apt clean）
5. 使用.dockerignore 排除不需要的文件

#### **问题 13:Docker 网络模式有哪些？**

**答：**

- bridge（默认）：虚拟网桥，容器通过 NAT 访问外网
- host：使用主机网络，性能最好但无隔离
- none：无网络
- container：共享其他容器的网络

### Jenkins 面试题

#### **问题 14：什么是 CI/CD？**

**答：**

- CI（持续集成）：频繁合并代码，自动构建、测试，快速发现问题
- CD（持续交付）：自动化发布到测试/预发环境
- CD（持续部署）：自动化发布到生产环境

#### **问题 15:Jenkins Pipeline 有哪两种语法？**

**答：**

1. 声明式 Pipeline（推荐）：结构化语法，更易读
   ```groovy
   pipeline { stages { stage { steps { } } } }
   ```
2. 脚本式 Pipeline：更灵活，用 Groovy 语法
   ```groovy
   node { stage { } }
   ```

#### **问题 16:Jenkins 如何触发构建？**

**答：**

1. 手动触发
2. 定时触发（Cron 表达式）
3. Git Webhook（代码推送时）
4. 轮询 SCM
5. 其他 Job 触发

### Nginx 面试题

#### **问题 17：正向代理和反向代理的区别？**

**答：**

- 正向代理：代理客户端，服务端不知道真实客户端（如 VPN）
- 反向代理：代理服务端，客户端不知道真实服务端（如 Nginx 负载均衡）

#### **问题 18:Nginx负载均衡有哪些策略？**

**答：**

1. 轮询（默认）：依次分配
2. weight：按权重分配
3. ip_hash：按 IP 哈希，实现会话保持
4. least_conn：最少连接数
5. url_hash：按 URL 哈希

#### **问题 19:Nginx location匹配优先级？**

**答（从高到低）：**

1. = 精确匹配
2. ^~ 前缀匹配，匹配后不再搜索正则
3. ~ 正则匹配（区分大小写）
4. ~* 正则匹配（不区分大小写）
5. 普通前缀匹配
6. / 默认匹配

#### **问题 20：如何实现 Nginx 热更新配置？**

**答：**

```bash
nginx -t          # 先测试配置语法
nginx -s reload   # 平滑重载配置
# 不会中断正在处理的请求
```

---

## 总结

本文详细介绍了 DevOps 核心知识点：

1. **Linux**：文件权限、进程管理、网络命令、日志查看
2. **Shell**：变量、条件判断、循环、函数、实战脚本
3. **Docker**：镜像、容器、Dockerfile、Docker Compose、网络
4. **Jenkins**：CI/CD 概念、Pipeline 语法、触发方式
5. **Nginx**：反向代理、负载均衡、location 匹配规则

每个部分都配有高频面试题及参考答案，帮助理解和应对面试。
