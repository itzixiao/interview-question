# Spring Boot 与自动装配知识点详解

## 📚 文档列表

#### 1. [01-Spring-Boot 自动装配.md](./01-Spring-Boot%E8%87%AA%E5%8A%A8%E8%A3%85%E9%85%8D.md)
- **内容：** @EnableAutoConfiguration、spring.factories、条件注解、自动配置原理
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **难度：** 中级 ~ 高级

#### 2. [02-自定义线程池Starter详解.md](./02-自定义线程池Starter详解.md) ⭐新增
- **内容：** 完整实现自定义线程池Starter，包括自动配置类、属性绑定、spring.factories
- **面试题：** 5+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **特色：** 
  - 从零开发完整的 Starter 项目
  - 图解自动装配流程
  - 配套可运行的示例代码
  - 实战高频面试题解析

---

## 📊 统计信息

- **文档数：** 2 个
- **面试题总数：** 15+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/springboot/` 目录（6 个文件）和 `interview-threadpool-starter/` 目录（4 个核心文件）

---

## 🎯 学习建议

### 重点掌握
1. **@EnableAutoConfiguration 注解原理**
   - 如何扫描并加载自动配置类
   - @Import 和 ImportSelector 的作用

2. **spring.factories 机制**
   - SPI（Service Provider Interface）扩展机制
   - 如何自定义 starter

3. **条件注解**
   - @ConditionalOnClass
   - @ConditionalOnMissingBean
   - @ConditionalOnProperty

4. **自定义 Starter 开发**
   - 命名规范：`xxx-spring-boot-starter`
   - 三步走：Properties + AutoConfiguration + spring.factories
   - 实战案例：自定义线程池Starter（interview-threadpool-starter）

---

## 🔗 相关链接

- [项目总览](../../README.md)
- [代码索引](../../../interview-service/src/main/java/cn/itzixiao/interview/代码索引.md)
- [Spring Boot 示例代码](../../../interview-service/src/main/java/cn/itzixiao/interview/springboot/)

---

## 📖 推荐学习顺序

```
1. Spring Boot 快速入门
   ↓
2. 理解自动装配原理
   ↓
3. 学习条件注解使用
   ↓
4. 掌握 spring.factories 机制
   ↓
5. 动手开发自定义 Starter
```

---

## 💡 高频面试题 Top 5

1. **Spring Boot 自动装配原理是什么？**
2. **@SpringBootApplication 注解包含哪些核心注解？**
3. **如何自定义一个 Spring Boot Starter？**
4. **spring.factories 文件的作用是什么？**
5. **Spring Boot 条件注解有哪些？如何使用？**

---

**维护者：** itzixiao  
**最后更新：** 2026-03-08  
**问题反馈：** 欢迎提 Issue 或 PR
