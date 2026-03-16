# MyBatis / MyBatis-Plus 核心原理与面试题详解

## 概述

本文档涵盖 MyBatis 和 MyBatis-Plus 的核心知识点：

- MyBatis 核心架构与执行流程
- 动态 SQL 原理与最佳实践
- MyBatis-Plus 增强功能
- SQL 注入防护
- 高频面试题

---

## 第一部分：MyBatis 核心架构

### 1.1 核心组件

```
┌────────────────────────────────────────────────────────────────────────┐
│                         MyBatis 核心架构                               │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│    ┌───────────────┐    ┌───────────────┐    ┌───────────────┐        │
│    │ SqlSession    │────│ Executor      │────│ StatementHandler │      │
│    │ 会话接口      │    │ 执行器        │    │ 语句处理器    │        │
│    └───────────────┘    └───────────────┘    └───────────────┘        │
│           │                    │                    │                  │
│           ▼                    ▼                    ▼                  │
│    ┌───────────────┐    ┌───────────────┐    ┌───────────────┐        │
│    │ MapperProxy   │    │ Cache         │    │ ParameterHandler │     │
│    │ Mapper代理    │    │ 缓存          │    │ 参数处理器    │        │
│    └───────────────┘    └───────────────┘    └───────────────┘        │
│                                                     │                  │
│                                                     ▼                  │
│                                              ┌───────────────┐        │
│                                              │ ResultSetHandler │     │
│                                              │ 结果集处理器  │        │
│                                              └───────────────┘        │
└────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心组件说明

| 组件                    | 说明                     |
|-----------------------|------------------------|
| **SqlSessionFactory** | 创建 SqlSession 的工厂，全局单例 |
| **SqlSession**        | 会话接口，执行 SQL、获取 Mapper  |
| **Executor**          | 执行器，负责 SQL 执行和缓存管理     |
| **StatementHandler**  | 语句处理器，创建 Statement 对象  |
| **ParameterHandler**  | 参数处理器，设置预编译参数          |
| **ResultSetHandler**  | 结果集处理器，封装结果集到对象        |

### 1.3 执行流程

```
1. 加载配置文件（mybatis-config.xml、Mapper.xml）
        ↓
2. SqlSessionFactoryBuilder 构建 SqlSessionFactory
        ↓
3. SqlSessionFactory 创建 SqlSession
        ↓
4. SqlSession 获取 Mapper 代理对象（MapperProxy）
        ↓
5. 调用 Mapper 方法，MapperProxy 拦截
        ↓
6. 解析 MappedStatement，获取 SQL
        ↓
7. Executor 执行 SQL
        ↓
8. StatementHandler 创建 Statement
        ↓
9. ParameterHandler 设置参数
        ↓
10. 执行 SQL，ResultSetHandler 处理结果
        ↓
11. 返回结果
```

---

## 第二部分：动态 SQL

### 2.1 动态 SQL 标签

| 标签          | 作用       | 示例场景        |
|-------------|----------|-------------|
| `<if>`      | 条件判断     | 可选查询条件      |
| `<choose>`  | 多选一      | 类似 switch   |
| `<where>`   | 智能 WHERE | 自动去除 AND/OR |
| `<set>`     | 智能 SET   | 自动去除逗号      |
| `<foreach>` | 遍历集合     | IN 查询、批量插入  |
| `<trim>`    | 自定义修剪    | 自定义前后缀      |
| `<bind>`    | 变量绑定     | 模糊查询拼接      |

### 2.2 动态查询示例

```xml
<select id="findByCondition" resultType="User">
    SELECT * FROM user
    <where>
        <if test="username != null">
            AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="status != null">
            AND status = #{status}
        </if>
        <if test="startTime != null and endTime != null">
            AND create_time BETWEEN #{startTime} AND #{endTime}
        </if>
    </where>
</select>
```

### 2.3 动态更新示例

```xml
<update id="updateUser">
    UPDATE user
    <set>
        <if test="username != null">username = #{username},</if>
        <if test="email != null">email = #{email},</if>
        <if test="status != null">status = #{status},</if>
        update_time = NOW()
    </set>
    WHERE id = #{id}
</update>
```

### 2.4 批量插入示例

```xml
<insert id="batchInsert">
    INSERT INTO user (username, email, status) VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.username}, #{item.email}, #{item.status})
    </foreach>
</insert>
```

---

## 第三部分：#{} vs ${}

### 3.1 核心区别

| 特性       | #{}         | ${}        |
|----------|-------------|------------|
| **处理方式** | 预编译，参数绑定    | 直接字符串替换    |
| **安全性**  | 安全，防 SQL 注入 | 有 SQL 注入风险 |
| **使用场景** | 参数值         | 表名、列名、排序字段 |
| **性能**   | 可缓存执行计划     | 每次生成新 SQL  |

### 3.2 SQL 注入示例

```java
// 恶意输入
String username = "admin' OR '1'='1";

// 危险代码（使用 ${}）
@Select("SELECT * FROM user WHERE username = '${username}'")
// 实际执行的 SQL：
// SELECT * FROM user WHERE username = 'admin' OR '1'='1'
// 结果：查询到所有用户！

// 安全代码（使用 #{}）
@Select("SELECT * FROM user WHERE username = #{username}")
// 实际执行的 SQL：
// SELECT * FROM user WHERE username = ?
// 参数被正确转义
```

### 3.3 必须使用 ${} 的场景

```java
// 动态排序（需白名单校验）
@Select("SELECT * FROM user ORDER BY ${orderBy} ${orderDirection}")
List<User> findAllWithOrder(@Param("orderBy") String orderBy,
                            @Param("orderDirection") String orderDirection);

// 白名单校验
private String validateOrderBy(String orderBy) {
    String[] allowedFields = {"id", "username", "create_time"};
    for (String field : allowedFields) {
        if (field.equals(orderBy)) return field;
    }
    return "id";  // 默认值
}
```

---

## 第四部分：MyBatis-Plus

### 4.1 核心特性

| 特性      | 说明                  |
|---------|---------------------|
| 通用 CRUD | BaseMapper 提供基础增删改查 |
| 条件构造器   | Wrapper 链式编程        |
| 分页插件    | 物理分页                |
| 乐观锁     | @Version 注解         |
| 逻辑删除    | @TableLogic 注解      |
| 自动填充    | @TableField(fill)   |

### 4.2 常用注解

| 注解            | 用途         |
|---------------|------------|
| `@TableName`  | 指定表名       |
| `@TableId`    | 主键（支持多种策略） |
| `@TableField` | 字段映射、填充策略  |
| `@TableLogic` | 逻辑删除       |
| `@Version`    | 乐观锁版本号     |

### 4.3 条件构造器

```java
// LambdaQueryWrapper（推荐，类型安全）
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getStatus, 1)
       .like(User::getUsername, "admin")
       .ge(User::getAge, 18)
       .orderByDesc(User::getCreateTime);
List<User> list = userMapper.selectList(wrapper);

// 复杂条件
wrapper.eq(User::getStatus, 1)
       .and(w -> w.like(User::getUsername, "admin")
                  .or()
                  .like(User::getEmail, "@gmail.com"))
       .between(User::getAge, 18, 60);
```

### 4.4 分页查询

```java
// 配置分页插件
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}

// 使用分页
Page<User> page = new Page<>(1, 10);
IPage<User> result = userMapper.selectPage(page, wrapper);
```

---

## 第五部分：缓存机制

### 5.1 一级缓存

- **作用域**：SqlSession 级别
- **默认开启**：是
- **生命周期**：SqlSession 关闭或执行增删改时清除
- **特点**：同一 SqlSession 中相同查询只执行一次 SQL

### 5.2 二级缓存

- **作用域**：Mapper（namespace）级别
- **默认开启**：否
- **开启方式**：`<cache/>` 或 `@CacheNamespace`
- **注意事项**：
    - 实体类需要实现 Serializable
    - 多表关联查询不建议使用
    - 建议使用 Redis 等分布式缓存替代

### 5.3 缓存配置

```xml
<!-- Mapper.xml 中开启二级缓存 -->
<cache
    eviction="LRU"
    flushInterval="60000"
    size="512"
    readOnly="true"/>
```

---

## 高频面试题

**问题 1:MyBatis的 #{}和 ${}有什么区别？**

**答**：
| 特性 | #{} | ${} |
|------|-----|-----|
| 处理方式 | 预编译，参数绑定 | 直接字符串替换 |
| 安全性 | 安全，防 SQL 注入 | 有 SQL 注入风险 |
| 使用场景 | 参数值 | 表名、列名、排序 |
| 性能 | 可缓存执行计划 | 每次生成新 SQL |

---

**问题 2:MyBatis如何防止 SQL 注入？**

**答**：

1. **使用 #{}**：参数预编译，自动转义特殊字符
2. **使用 ${}时进行白名单校验**：限制允许的值
3. **使用 CONCAT 进行模糊查询**：`LIKE CONCAT('%', #{keyword}, '%')`
4. **遵循最小权限原则**：数据库账号权限最小化

---

**问题 3:MyBatis的一级缓存和二级缓存有什么区别？**

**答**：
| 特性 | 一级缓存 | 二级缓存 |
|------|----------|----------|
| 作用域 | SqlSession | Mapper（namespace） |
| 默认开启 | 是 | 否 |
| 生命周期 | SqlSession 关闭或 DML 后清除 | 应用级别 |
| 适用场景 | 单次会话内重复查询 | 跨会话共享 |

---

**问题 4:MyBatis的执行流程是怎样的？**

**答**：

1. 加载配置文件，构建 SqlSessionFactory
2. SqlSessionFactory 创建 SqlSession
3. SqlSession 获取 Mapper 代理对象
4. 调用 Mapper 方法，MapperProxy 拦截
5. 解析 MappedStatement，获取 SQL
6. Executor 执行 SQL（可能走缓存）
7. StatementHandler 创建 Statement
8. ParameterHandler 设置参数
9. 执行 SQL，ResultSetHandler 处理结果

---

**问题 5:MyBatis-Plus的 LambdaQueryWrapper有什么优势？**

**答**：

1. **类型安全**：使用方法引用，编译期检查
2. **避免硬编码**：不需要写字段名字符串
3. **重构友好**：字段重命名时自动更新
4. **IDE 支持**：代码提示、自动补全

---

**问题 6:MyBatis如何实现批量插入？**

**答**：

```xml
<!-- XML 方式 -->
<insert id="batchInsert">
    INSERT INTO user (name, age) VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.name}, #{item.age})
    </foreach>
</insert>
```

```java
// MyBatis-Plus 方式
userService.saveBatch(userList, 1000);  // 每批1000条
```

---

**问题 7:MyBatis的动态 SQL 标签有哪些？**

**答**：

- `<if>`：条件判断
- `<choose>/<when>/<otherwise>`：多选一
- `<where>`：智能 WHERE（自动去除 AND/OR）
- `<set>`：智能 SET（自动去除逗号）
- `<foreach>`：遍历集合
- `<trim>`：自定义前后缀
- `<bind>`：变量绑定

---

**问题 8:MyBatis的 Mapper 接口是如何工作的？**

**答**：
MyBatis 使用 JDK 动态代理：

1. 扫描 Mapper 接口
2. 为每个接口创建 MapperProxy 代理对象
3. 调用方法时，MapperProxy 拦截
4. 根据方法签名找到对应的 MappedStatement
5. 执行 SQL 并返回结果

---

**问题 9:MyBatis-Plus的乐观锁是如何实现的？**

**答**：

1. 实体类字段加 `@Version` 注解
2. 配置乐观锁插件 `OptimisticLockerInnerInterceptor`
3. 更新时自动带上版本条件：
   ```sql
   UPDATE user SET name=?, version=version+1
   WHERE id=? AND version=?
   ```
4. 如果 version 不匹配，更新失败（影响行数为0）

---

**问题 10:MyBatis的延迟加载是怎么实现的？**

**答**：

1. 配置 `lazyLoadingEnabled=true`
2. 查询时不立即执行关联查询
3. 访问关联属性时才执行
4. 实现原理：CGLIB 动态代理，拦截 getter 方法

---

## 相关代码示例

- [MyBatisDynamicSqlDemo.java](../../interview-microservices-parent/interview-service/src/main/java/cn/itzixiao/interview/mybatis/MyBatisDynamicSqlDemo.java) -
  动态 SQL 示例
- [MyBatisPlusDemo.java](../../interview-microservices-parent/interview-service/src/main/java/cn/itzixiao/interview/mybatis/MyBatisPlusDemo.java) -
  MyBatis-Plus 示例
- [SqlInjectionPreventionDemo.java](../../interview-microservices-parent/interview-service/src/main/java/cn/itzixiao/interview/mybatis/SqlInjectionPreventionDemo.java) -
  SQL 注入防护
