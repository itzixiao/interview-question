# 中间件知识点详解

## 📚 文档列表

#### 1. [01-MyBatis-Plus快速入门.md](./01-MyBatis-Plus%E5%BF%AB%E9%80%9F%E5%85%A5%E9%97%A8.md)
- **内容：** 通用 mapper、Service、分页插件
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐

#### 2. [02-MyBatis动态 SQL 与 SQL 注入防护.md](./02-MyBatis%E5%8A%A8%E6%80%81SQL%E4%B8%8ESQL%E6%B3%A8%E5%85%A5%E9%98%B2%E6%8A%A4.md)
- **内容：** #{}vs${}、SQL 注入原理、防护方案
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 3. [03-MyBatis核心原理与面试题.md](./03-MyBatis%E6%A0%B8%E5%BF%83%E5%8E%9F%E7%90%86%E4%B8%8E%E9%9D%A2%E8%AF%95%E9%A2%98.md)
- **内容：** SQL 映射、插件机制、缓存原理
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐

#### 4. [04-Nacos 核心知识点详解.md](./04-Nacos%E6%A0%B8%E5%BF%83%E7%9F%A5%E8%AF%86%E7%82%B9%E8%AF%A6%E8%A7%A3.md)
- **内容：** 服务发现、配置管理、服务注册与发现
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 5. [05-Sentinel限流熔断详解.md](./05-Sentinel%E9%99%90%E6%B5%81%E7%86%94%E6%96%AD%E8%AF%A6%E8%A7%A3.md)
- **内容：** 流量控制、熔断降级、系统自适应保护
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐

---

## 📊 统计信息

- **文档数：** 5 个
- **面试题总数：** 51+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/mybatis/` 等目录（~2,000 行代码）

---

## 🎯 学习建议

### MyBatis（2-3 天）
1. **核心概念**
   - SqlSessionFactory、SqlSession
   - Mapper 接口绑定
   - 动态 SQL 标签

2. **缓存机制**
   - 一级缓存（SqlSession 级别）
   - 二级缓存（Namespace 级别）
   - 缓存失效场景

3. **插件开发**
   - Interceptor 接口
   - 拦截 Executor、StatementHandler

### Nacos（2-3 天）
1. **服务发现**
   - 服务注册与注销
   - 心跳机制
   - 健康检查

2. **配置中心**
   - 配置动态刷新
   - 多环境隔离
   - 配置版本管理

### Sentinel（2 天）
1. **限流规则**
   - QPS 模式、线程模式
   - 热点参数限流

2. **熔断降级**
   - 异常比例、慢调用比例
   - 半开状态恢复

---

## 🔗 跨模块关联

### 前置知识
- ✅ **[Spring框架](../04-Spring框架/README.md)** - IOC、AOP
- ✅ **[MySQL](../07-MySQL 数据库/README.md)** - SQL 语法、索引优化

### 后续进阶
- 📚 **[SpringBoot](../05-SpringBoot 与自动装配/README.md)** - Starter 集成
- 📚 **[分布式系统](../12-分布式系统/README.md)** - 配置中心实战

### 知识点对应
| 中间件 | 应用场景 |
|--------|---------|
| MyBatis | ORM 映射、动态 SQL |
| Nacos | 微服务注册发现、配置管理 |
| Sentinel | 高并发限流、熔断降级 |
| MyBatis-Plus | 快速开发、通用 CRUD |

---

## 💡 高频面试题 Top 15

1. **MyBatis 的一级缓存和二级缓存区别？**
2. **MyBatis 的插件原理是什么？**
3. **#{}和${}的区别？如何防止 SQL 注入？**
4. **Nacos 如何实现服务注册与发现？**
5. **Nacos 配置中心是如何实现动态刷新的？**
6. **Sentinel 的限流算法有哪些？**
7. **Sentinel 如何实现熔断降级？**
8. **MyBatis-Plus 相比 MyBatis 有什么优势？**
9. **MyBatis 的动态 SQL 有哪些常用标签？**
10. **Nacos 和 Eureka 的区别？**
11. **Sentinel 和 Hystrix 的区别？**
12. **MyBatis 中 ResultMap 的作用？**
13. **MyBatis 的分页插件原理？**
14. **Nacos 支持哪些配置格式？**
15. **Sentinel 的规则如何持久化？**

---

## 🛠️ 实战技巧

### MyBatis 动态 SQL 示例
```xml
<select id="findUsers" resultType="User">
  SELECT * FROM users
  <where>
    <if test="name != null">
      AND name LIKE #{name}
    </if>
    <if test="age != null">
      AND age > #{age}
    </if>
  </where>
</select>
```

### Sentinel 限流配置
```java
@SentinelResource(value = "getUser", 
    blockHandler = "handleBlock",
    fallback = "handleFallback")
public User getUser(Long id) {
    return userService.getById(id);
}

public User handleBlock(Long id, BlockException ex) {
    return new User("默认用户");
}
```

---

## 📖 推荐学习顺序

```
MyBatis 基础
   ↓
动态 SQL
   ↓
缓存机制
   ↓
插件开发
   ↓
Nacos 服务发现
   ↓
Nacos 配置中心
   ↓
Sentinel 限流
   ↓
综合实战
```

---

## 📈 更新日志

### v2.0 - 2026-03-08
- ✅ 新增跨模块关联章节
- ✅ 补充 51+ 道高频面试题
- ✅ 添加学习建议和实战技巧
- ✅ 完善推荐学习顺序

### v1.0 - 早期版本
- ✅ 基础中间件文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-08  
**问题反馈：** 欢迎提 Issue 或 PR
