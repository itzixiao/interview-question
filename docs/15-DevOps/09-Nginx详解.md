# Nginx 详解

## 一、Nginx 架构概述

### 1.1 Nginx 是什么

Nginx 是一个高性能的 HTTP 和反向代理服务器，也是一个 IMAP/POP3/SMTP 代理服务器。由俄罗斯程序员 Igor Sysoev 开发，以其高性能、高并发、低内存占用而闻名。

**核心特点：**

1. **高性能** - 单机可支持 10万+ 并发连接
2. **低资源消耗** - 10,000 个非活动 HTTP 保持连接仅占用 2.5MB 内存
3. **事件驱动** - 使用 epoll/kqueue 异步非阻塞 IO
4. **模块化设计** - 丰富的模块生态系统
5. **热部署** - 支持配置热加载，不中断服务

### 1.2 Nginx 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    Nginx 架构图                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                    ┌─────────────┐                              │
│                    │  Master     │  管理进程                    │
│                    │  Process    │  读取配置、管理 Worker        │
│                    └──────┬──────┘                              │
│                           │                                      │
│         ┌─────────────────┼─────────────────┐                  │
│         ▼                 ▼                 ▼                   │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐          │
│  │   Worker    │   │   Worker    │   │   Worker    │          │
│  │   Process   │   │   Process   │   │   Process   │          │
│  │     (1)     │   │     (2)     │   │     (N)     │          │
│  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘          │
│         │                 │                 │                   │
│         └─────────────────┼─────────────────┘                  │
│                           │                                      │
│                           ▼                                      │
│                    ┌─────────────┐                              │
│                    │   Events    │  事件驱动                    │
│                    │   Module    │  epoll/kqueue                │
│                    └─────────────┘                              │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    核心模块                              │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │   │
│  │  │   HTTP  │  │  Mail   │  │ Stream  │  │  SSL    │    │   │
│  │  │ Module  │  │ Module  │  │ Module  │  │ Module  │    │   │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 Nginx 进程模型

```
┌─────────────────────────────────────────────────────────────────┐
│                    Nginx 进程通信模型                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Master Process                        │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  - 读取配置文件                                  │    │   │
│  │  │  - 监听端口绑定                                  │    │   │
│  │  │  - 管理 Worker 进程（启动/停止/重启）            │    │   │
│  │  │  - 接收信号处理（reload/reopen/stop）            │    │   │
│  │  │  - 维护共享内存                                  │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                      │
│              信号通道（Channel）                                 │
│                           │                                      │
│         ┌─────────────────┼─────────────────┐                  │
│         ▼                 ▼                 ▼                   │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐          │
│  │   Worker    │   │   Worker    │   │   Worker    │          │
│  │   Process   │   │   Process   │   │   Process   │          │
│  │             │   │             │   │             │          │
│  │  单线程：   │   │  单线程：   │   │  单线程：   │          │
│  │  - 事件循环 │   │  - 事件循环 │   │  - 事件循环 │          │
│  │  - 非阻塞IO │   │  - 非阻塞IO │   │  - 非阻塞IO │          │
│  │  - 连接处理 │   │  - 连接处理 │   │  - 连接处理 │          │
│  └─────────────┘   └─────────────┘   └─────────────┘          │
│                                                                  │
│  进程数建议：Worker 数 = CPU 核心数                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、Nginx 配置详解

### 2.1 核心配置文件结构

```nginx
# nginx.conf 核心配置结构

# ==================== 全局配置 ====================
user nginx;                    # 运行用户
worker_processes auto;         # Worker 进程数（auto = CPU 核心数）
worker_cpu_affinity auto;      # CPU 亲和性
worker_rlimit_nofile 65535;    # 每个 Worker 最大打开文件数
error_log /var/log/nginx/error.log warn;  # 错误日志
pid /var/run/nginx.pid;        # PID 文件

# ==================== 事件配置 ====================
events {
    worker_connections 65535;  # 每个 Worker 最大连接数
    use epoll;                 # 事件模型（Linux 使用 epoll）
    multi_accept on;           # 一次接受多个连接
    accept_mutex off;          # 关闭互斥锁（高并发场景）
}

# ==================== HTTP 配置 ====================
http {
    include mime.types;        # MIME 类型配置
    default_type application/octet-stream;
    
    # 日志格式
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';
    
    access_log /var/log/nginx/access.log main;
    
    # 性能优化
    sendfile on;               # 零拷贝
    tcp_nopush on;             # 优化数据包发送
    tcp_nodelay on;            # 禁用 Nagle 算法
    keepalive_timeout 65;      # 长连接超时
    types_hash_max_size 2048;
    
    # Gzip 压缩
    gzip on;
    gzip_min_length 1k;
    gzip_comp_level 6;
    gzip_types text/plain text/css application/json application/javascript;
    
    # 包含其他配置
    include /etc/nginx/conf.d/*.conf;
    include /etc/nginx/sites-enabled/*;
}
```

### 2.2 负载均衡配置

```nginx
# 负载均衡配置示例

# 定义后端服务器组
upstream tomcat_cluster {
    # 负载均衡策略：轮询（默认）
    # server 192.168.1.101:8080;
    # server 192.168.1.102:8080;
    # server 192.168.1.103:8080;
    
    # 负载均衡策略：权重
    server 192.168.1.101:8080 weight=3;
    server 192.168.1.102:8080 weight=2;
    server 192.168.1.103:8080 weight=1;
    
    # 负载均衡策略：IP Hash（会话保持）
    # ip_hash;
    
    # 负载均衡策略：最少连接
    # least_conn;
    
    # 健康检查
    server 192.168.1.104:8080 backup;  # 备用服务器
    server 192.168.1.105:8080 down;    # 下线服务器
    
    # 长连接配置
    keepalive 32;  # 保持的空闲连接数
    keepalive_timeout 60s;  # 长连接超时时间
}

server {
    listen 80;
    server_name www.example.com;
    
    location / {
        proxy_pass http://tomcat_cluster;
        
        # 代理头设置
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 代理超时设置
        proxy_connect_timeout 30s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 代理缓冲
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 32k;
        proxy_busy_buffers_size 64k;
    }
}
```

### 2.3 反向代理配置

```nginx
# 反向代理到 Tomcat

server {
    listen 80;
    server_name api.example.com;
    
    # 访问日志
    access_log /var/log/nginx/api.access.log main;
    error_log /var/log/nginx/api.error.log;
    
    # 静态资源直接由 Nginx 处理
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2)$ {
        root /var/www/static;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
    
    # API 请求转发到 Tomcat
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        
        # 请求头透传
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 超时配置
        proxy_connect_timeout 10s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # 健康检查端点
    location /health {
        access_log off;
        return 200 "OK";
        add_header Content-Type text/plain;
    }
}
```

### 2.4 HTTPS 配置

```nginx
# HTTPS 配置（SSL/TLS）

server {
    listen 443 ssl http2;
    server_name www.example.com;
    
    # SSL 证书
    ssl_certificate /etc/nginx/ssl/example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/example.com.key;
    
    # SSL 协议和加密套件
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers on;
    
    # SSL 会话缓存
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;
    
    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    
    location / {
        proxy_pass http://tomcat_cluster;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name www.example.com;
    return 301 https://$server_name$request_uri;
}
```

---

## 三、Nginx 与 Tomcat 配合部署

### 3.1 部署架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    Nginx + Tomcat 部署架构                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                    ┌─────────────────┐                          │
│                    │   客户端请求    │                          │
│                    └────────┬────────┘                          │
│                             │                                    │
│                             ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Nginx (80/443)                       │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  职责：                                          │    │   │
│  │  │  - SSL 终止（HTTPS 解密）                        │    │   │
│  │  │  - 静态资源服务                                  │    │   │
│  │  │  - 负载均衡                                      │    │   │
│  │  │  - 请求路由                                      │    │   │
│  │  │  - Gzip 压缩                                     │    │   │
│  │  │  - 访问控制                                      │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                             │                                    │
│         ┌───────────────────┼───────────────────┐              │
│         ▼                   ▼                   ▼               │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐      │
│  │  Tomcat 1   │     │  Tomcat 2   │     │  Tomcat N   │      │
│  │   :8080     │     │   :8080     │     │   :8080     │      │
│  │             │     │             │     │             │      │
│  │  职责：     │     │  职责：     │     │  职责：     │      │
│  │  - 业务逻辑 │     │  - 业务逻辑 │     │  - 业务逻辑 │      │
│  │  - 动态内容 │     │  - 动态内容 │     │  - 动态内容 │      │
│  └─────────────┘     └─────────────┘     └─────────────┘      │
│         │                   │                   │               │
│         └───────────────────┼───────────────────┘               │
│                             │                                    │
│                             ▼                                    │
│                    ┌─────────────────┐                          │
│                    │    数据库层     │                          │
│                    │  MySQL/Redis   │                          │
│                    └─────────────────┘                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 完整配置示例

**Nginx 配置：**

```nginx
# /etc/nginx/conf.d/app.conf

# 定义 Tomcat 集群
upstream tomcat_backend {
    # 负载均衡策略：最少连接
    least_conn;
    
    server 192.168.1.101:8080 weight=3 max_fails=3 fail_timeout=30s;
    server 192.168.1.102:8080 weight=2 max_fails=3 fail_timeout=30s;
    server 192.168.1.103:8080 weight=1 max_fails=3 fail_timeout=30s;
    
    # 长连接
    keepalive 32;
}

server {
    listen 80;
    server_name api.example.com;
    
    # 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.example.com;
    
    # SSL 配置
    ssl_certificate /etc/nginx/ssl/api.example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/api.example.com.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    
    # 静态资源（由 Nginx 直接处理）
    location ~* \.(css|js|jpg|jpeg|png|gif|ico|woff|woff2|ttf|eot)$ {
        root /var/www/app/static;
        expires 30d;
        add_header Cache-Control "public, immutable";
        access_log off;
    }
    
    # API 请求转发到 Tomcat
    location / {
        proxy_pass http://tomcat_backend;
        
        # 请求头
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        
        # HTTP 版本和长连接
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        
        # 超时
        proxy_connect_timeout 10s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 缓冲
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 32k;
        proxy_busy_buffers_size 64k;
    }
    
    # 健康检查
    location /nginx-health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
    
    # Tomcat 健康检查代理
    location /actuator/health {
        proxy_pass http://tomcat_backend/actuator/health;
        proxy_set_header Host $host;
        access_log off;
    }
}
```

**Tomcat server.xml 配置：**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Server port="8005" shutdown="SHUTDOWN">
    
    <!-- 共享线程池 -->
    <Executor name="tomcatThreadPool" 
              namePrefix="http-exec-"
              maxThreads="500"
              minSpareThreads="50"
              maxIdleTime="60000"/>
    
    <Service name="Catalina">
        
        <!-- HTTP 连接器（NIO） -->
        <Connector executor="tomcatThreadPool"
                   port="8080"
                   protocol="org.apache.coyote.http11.Http11NioProtocol"
                   connectionTimeout="20000"
                   redirectPort="8443"
                   maxConnections="10000"
                   acceptCount="200"
                   compression="on"
                   compressionMinSize="2048"
                   compressableMimeType="text/html,text/xml,text/css,application/json"
                   URIEncoding="UTF-8"
                   enableLookups="false"/>
        
        <!-- AJP 连接器（可选，用于与 Apache HTTPD 集成） -->
        <Connector port="8009"
                   protocol="AJP/1.3"
                   redirectPort="8443"
                   secretRequired="false"/>
        
        <Engine name="Catalina" defaultHost="localhost">
            
            <!-- 访问日志 -->
            <Valve className="org.apache.catalina.valves.AccessLogValve"
                   directory="logs"
                   prefix="access_log"
                   suffix=".txt"
                   pattern="%h %l %u %t &quot;%r&quot; %s %b %D"
                   resolveHosts="false"/>
            
            <!-- 远程 IP 阀门（处理 Nginx 代理头） -->
            <Valve className="org.apache.catalina.valves.RemoteIpValve"
                   remoteIpHeader="X-Forwarded-For"
                   protocolHeader="X-Forwarded-Proto"
                   protocolHeaderHttpsValue="https"/>
            
            <Host name="localhost" appBase="webapps"
                  unpackWARs="true" autoDeploy="true">
                
                <!-- 错误报告 -->
                <Valve className="org.apache.catalina.valves.ErrorReportValve"
                       showReport="false"
                       showServerInfo="false"/>
                
            </Host>
        </Engine>
    </Service>
</Server>
```

---

## 四、高频面试题

**问题 1：Nginx 和 Tomcat 有什么区别？如何配合使用？**

**答：**

| 对比项 | Nginx | Tomcat |
|--------|-------|--------|
| 定位 | HTTP 服务器/反向代理 | Servlet 容器 |
| 处理内容 | 静态资源、负载均衡 | 动态内容（JSP/Servlet） |
| 并发模型 | 事件驱动、异步非阻塞 | 线程池模型 |
| SSL 处理 | 高效（支持硬件加速） | 相对较慢 |
| 适用场景 | 高并发静态资源、负载均衡 | Java Web 应用 |

**配合使用：**
1. Nginx 作为前端服务器，处理 SSL、静态资源、负载均衡
2. Tomcat 作为后端应用服务器，处理业务逻辑
3. Nginx 通过 proxy_pass 将动态请求转发到 Tomcat

**问题 2：Nginx 的负载均衡策略有哪些？**

**答：**

1. **轮询（默认）**：按顺序分配请求
2. **权重**：按权重比例分配
3. **IP Hash**：根据客户端 IP 分配，实现会话保持
4. **最少连接**：分配给连接数最少的服务器
5. **一致性 Hash**：根据请求 URL 分配，适用于缓存场景

```nginx
# 权重配置
upstream backend {
    server 192.168.1.101:8080 weight=3;
    server 192.168.1.102:8080 weight=2;
}

# 最少连接
upstream backend {
    least_conn;
    server 192.168.1.101:8080;
    server 192.168.1.102:8080;
}

# IP Hash
upstream backend {
    ip_hash;
    server 192.168.1.101:8080;
    server 192.168.1.102:8080;
}
```

**问题 3：Nginx 如何实现高可用？**

**答：**

**方案一：Keepalived + Nginx**

```
┌─────────────────────────────────────────────────────────────────┐
│                    Keepalived 高可用架构                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                    ┌─────────────┐                              │
│                    │  VIP        │  虚拟 IP（对外服务）          │
│                    │ 192.168.1.100│                              │
│                    └──────┬──────┘                              │
│                           │                                      │
│         ┌─────────────────┴─────────────────┐                  │
│         ▼                                   ▼                   │
│  ┌─────────────┐                    ┌─────────────┐            │
│  │  Nginx-Master                     │  Nginx-Backup           │
│  │  Keepalived │                    │  Keepalived │            │
│  │  Priority:100                    │  Priority:90            │
│  │  192.168.1.101                   │  192.168.1.102          │
│  └─────────────┘                    └─────────────┘            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Keepalived 配置：**

```bash
# /etc/keepalived/keepalived.conf

vrrp_script check_nginx {
    script "/etc/keepalived/check_nginx.sh"
    interval 2
    weight -20
}

vrrp_instance VI_1 {
    state MASTER
    interface eth0
    virtual_router_id 51
    priority 100
    advert_int 1
    
    authentication {
        auth_type PASS
        auth_pass 1234
    }
    
    virtual_ipaddress {
        192.168.1.100
    }
    
    track_script {
        check_nginx
    }
}
```

**问题 4：Nginx 为什么性能高？**

**答：**

1. **事件驱动模型**：使用 epoll/kqueue，单线程可处理数万连接
2. **异步非阻塞 IO**：Worker 进程不会阻塞等待 IO
3. **Master-Worker 架构**：Worker 进程独立处理请求，互不影响
4. **零拷贝技术**：sendfile 系统调用减少数据拷贝
5. **内存池管理**：减少内存分配和回收开销
6. **模块化设计**：按需加载模块，减少资源消耗

**问题 5：Nginx 如何处理静态资源和动态资源？**

**答：**

```nginx
server {
    listen 80;
    server_name example.com;
    
    # 静态资源 - 直接由 Nginx 处理
    location ~* \.(jpg|jpeg|png|gif|css|js)$ {
        root /var/www/static;
        expires 30d;
        add_header Cache-Control "public";
    }
    
    # 动态资源 - 转发到后端应用服务器
    location /api/ {
        proxy_pass http://backend_server;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**优势：**
- 静态资源直接由 Nginx 处理，性能极高
- 动态请求转发到 Tomcat 等应用服务器
- 实现动静分离，提升整体性能

---

## 五、最佳实践

### 5.1 生产环境配置清单

```
□ Nginx 配置
  ├─ Worker 数 = CPU 核心数
  ├─ 连接数：worker_connections = 65535
  ├─ 启用 sendfile、tcp_nopush
  ├─ 配置 Gzip 压缩
  ├─ 配置负载均衡健康检查
  └─ 配置 SSL/TLS

□ 安全加固
  ├─ 隐藏 Nginx 版本号
  ├─ 配置安全响应头
  ├─ 限制请求频率（防 DDoS）
  ├─ 配置访问控制
  └─ 启用 HTTPS

□ 监控告警
  ├─ Nginx 状态监控（stub_status）
  ├─ 访问日志分析
  ├─ 错误日志监控
  └─ 配置告警阈值
```

### 5.2 性能调优公式

```
Nginx Worker 数：
  Worker 数 = CPU 核心数
  
Nginx 最大连接数：
  最大连接数 = Worker 数 × worker_connections
  
  示例：
  - 8 核 CPU
  - worker_connections = 65535
  - 最大连接数 = 8 × 65535 = 524,280
```

### 5.3 常用命令

```bash
# 测试配置语法
nginx -t

# 重新加载配置
nginx -s reload

# 停止 Nginx
nginx -s stop

# 查看 Nginx 状态
curl http://localhost/nginx_status

# 查看 Worker 进程
ps aux | grep nginx
```

---

**维护者：** itzixiao  
**最后更新：** 2026-03-21  
**问题反馈：** 欢迎提 Issue 或 PR
