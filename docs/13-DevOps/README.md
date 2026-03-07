# DevOps 知识点详解

## 📚 文档列表

#### 1. [01-DevOps核心知识点详解.md](./01-DevOps%E6%A0%B8%E5%BF%83%E7%9F%A5%E8%AF%86%E7%82%B9%E8%AF%A6%E8%A7%A3.md)
- **内容：** Linux、Docker、Jenkins、Nginx 等运维知识
- **面试题：** 20+ 道
- **重要程度：** ⭐⭐⭐⭐

---

## 📊 统计信息

- **文档数：** 1 个
- **面试题总数：** 20+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/devops/` 目录（~500 行配置）

---

## 🎯 学习建议

### Linux 基础（2 天）
1. **常用命令**
   - ps、top、netstat、lsof
   - grep、awk、sed
   - chmod、chown

2. **性能排查**
   - CPU 使用率分析
   - 内存占用查看
   - 磁盘 IO 监控

### Docker 容器化（2 天）
1. **镜像管理**
   - Dockerfile 编写
   - 镜像构建优化

2. **容器操作**
   - 启动、停止、删除
   - 端口映射、数据卷挂载

### Nginx 配置（1 天）
1. **反向代理**
   - upstream 配置
   - 负载均衡策略

2. **静态资源服务**
   - gzip 压缩
   - 缓存配置

### CI/CD（1 天）
1. **Jenkins 流水线**
   - 构建、测试、部署
   - 自动化发布

---

## 🔗 跨模块关联

### 前置知识
- ✅ **[SpringBoot](../05-SpringBoot 与自动装配/README.md)** - 应用打包部署

### 后续进阶
- 📚 **[Gateway](../06-SpringCloud 微服务/README.md)** - Nginx vs Gateway
- 📚 **[分布式系统](../12-分布式系统/README.md)** - 容器化部署

### 知识点对应
| DevOps | 应用场景 |
|--------|---------|
| Docker | 应用容器化部署 |
| Nginx | 反向代理、负载均衡 |
| Jenkins | CI/CD 自动化 |
| Linux | 生产环境运维 |

---

## 💡 高频面试题 Top 15

1. **Docker 的优势是什么？与虚拟机的区别？**
2. **Dockerfile 的常用指令有哪些？**
3. **如何优化 Docker 镜像大小？**
4. **Nginx 的负载均衡策略有哪些？**
5. **Nginx 如何实现动静分离？**
6. **Linux 如何查看 CPU 使用率？**
7. **Linux 如何查看端口占用？**
8. **如何查看日志文件的最后 100 行？**
9. **Jenkins 流水线的基本结构？**
10. **CI/CD 的流程是怎样的？**
11. **如何进行灰度发布？**
12. **如何回滚部署？**
13. **Docker 网络模式有哪些？**
14. **Kubernetes 的核心概念？**
15. **如何进行服务健康检查？**

---

## 🛠️ 实战技巧

### Dockerfile 示例
```dockerfile
FROM openjdk:8-jdk-alpine
WORKDIR /app
COPY target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Nginx 反向代理配置
```nginx
upstream backend {
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
}

server {
    listen 80;
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 📖 推荐学习顺序

```
Linux 基础命令
   ↓
Docker 入门
   ↓
Dockerfile 编写
   ↓
Nginx 配置
   ↓
Jenkins 流水线
   ↓
CI/CD 实战
   ↓
Kubernetes 入门
```

---

## 📈 更新日志

### v2.0 - 2026-03-08
- ✅ 新增跨模块关联章节
- ✅ 补充 20+ 道高频面试题
- ✅ 添加学习建议和实战技巧
- ✅ 完善推荐学习顺序

### v1.0 - 早期版本
- ✅ 基础 DevOps 文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-08  
**问题反馈：** 欢迎提 Issue 或 PR
