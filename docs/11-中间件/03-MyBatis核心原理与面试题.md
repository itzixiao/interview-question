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

### 5.1 一级缓存（SqlSession 级别）

#### 5.1.1 基本概念

- **作用域**：SqlSession 级别（会话级别）
- **默认开启**：是，无法关闭
- **存储位置**：内存（HashMap）
- **生命周期**：与 SqlSession 相同

#### 5.1.2 缓存失效场景

一级缓存在以下情况会失效：

1. **SqlSession 关闭**：`sqlSession.close()`
2. **执行增删改操作**：INSERT、UPDATE、DELETE
3. **手动清空缓存**：`sqlSession.clearCache()`
4. **不同的 SqlSession**：每个 SqlSession 有独立的缓存

#### 5.1.3 执行流程

```
第一次查询：
    SqlSession → 数据库 → 结果 → 存入一级缓存 → 返回

第二次查询（相同 SQL）：
    SqlSession → 一级缓存命中 → 直接返回（不访问数据库）

执行 UPDATE 后：
    SqlSession → 清空一级缓存 → 访问数据库 → 存入新缓存
```

#### 5.1.4 代码示例

```java
// 同一个 SqlSession 内，一级缓存生效
SqlSession sqlSession = sqlSessionFactory.openSession();

try {
    UserMapper mapper = sqlSession.getMapper(UserMapper.class);
    
    // 第一次查询：访问数据库
    User user1 = mapper.selectById(1L);
    System.out.println("第一次查询：" + user1);
    
    // 第二次查询：直接从一级缓存获取（不访问数据库）
    User user2 = mapper.selectById(1L);
    System.out.println("第二次查询：" + user2);
    
    // user1 == user2，是同一个对象
    System.out.println("是否相同对象：" + (user1 == user2)); // true
    
    // 执行更新操作，一级缓存被清空
    mapper.updateById(new User(1L, "newName"));
    sqlSession.commit();
    
    // 第三次查询：缓存已清空，重新访问数据库
    User user3 = mapper.selectById(1L);
    System.out.println("第三次查询：" + user3);
    
} finally {
    sqlSession.close();
}
```

#### 5.1.5 注意事项

```java
// ❌ 错误：不同 SqlSession 无法共享一级缓存
SqlSession session1 = sqlSessionFactory.openSession();
SqlSession session2 = sqlSessionFactory.openSession();

User user1 = session1.getMapper(UserMapper.class).selectById(1L); // 访问数据库
User user2 = session2.getMapper(UserMapper.class).selectById(1L); // 再次访问数据库

// ✅ 正确：使用同一个 SqlSession
SqlSession session = sqlSessionFactory.openSession();
UserMapper mapper = session.getMapper(UserMapper.class);
User user3 = mapper.selectById(1L); // 访问数据库
User user4 = mapper.selectById(1L); // 从一级缓存获取
```

---

### 5.2 二级缓存（Mapper 级别）

#### 5.2.1 基本概念

- **作用域**：Mapper（namespace）级别
- **默认开启**：否，需要手动配置
- **存储位置**：可配置（内存、Redis、Ehcache 等）
- **生命周期**：应用级别，多个 SqlSession 共享

#### 5.2.2 开启方式

**方式一：XML 配置**

```xml
<!-- Mapper.xml 中开启 -->
<mapper namespace="cn.itzixiao.interview.mapper.UserMapper">
    <!-- 开启二级缓存 -->
    <cache
        eviction="LRU"
        flushInterval="60000"
        size="512"
        readOnly="true"
        blocking="false"/>
</mapper>
```

**方式二：注解配置**

```java
@CacheNamespace(
    eviction = CacheNamespace.Eviction.LRU,
    flushInterval = 60000,
    size = 512,
    readOnly = true
)
public interface UserMapper {
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(Long id);
}
```

**方式三：全局配置（mybatis-config.xml）**

```xml
<configuration>
    <settings>
        <!-- 开启二级缓存总开关 -->
        <setting name="cacheEnabled" value="true"/>
    </settings>
</configuration>
```

#### 5.2.3 缓存策略（eviction）

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| **LRU** | 最近最少使用 | 默认策略，大多数场景适用 |
| **FIFO** | 先进先出 | 按时间顺序淘汰 |
| **SOFT** | 软引用 | 内存不足时回收 |
| **WEAK** | 弱引用 | 更激进的回收策略 |

#### 5.2.4 执行流程

```
SqlSession1 第一次查询：
    数据库 → 结果 → 存入一级缓存 → 存入二级缓存 → 返回

SqlSession1 关闭：
    一级缓存数据刷新到二级缓存

SqlSession2 查询相同数据：
    二级缓存命中 → 返回（不访问数据库）
```

#### 5.2.5 代码示例

```java
// 二级缓存需要实体类实现 Serializable
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    // ...
}

// 测试二级缓存
public void testSecondLevelCache() {
    SqlSession session1 = sqlSessionFactory.openSession();
    SqlSession session2 = sqlSessionFactory.openSession();
    
    try {
        // Session1 查询
        UserMapper mapper1 = session1.getMapper(UserMapper.class);
        User user1 = mapper1.selectById(1L);
        System.out.println("Session1 查询：" + user1);
        session1.close(); // 关闭后，数据刷新到二级缓存
        
        // Session2 查询相同数据
        UserMapper mapper2 = session2.getMapper(UserMapper.class);
        User user2 = mapper2.selectById(1L);
        System.out.println("Session2 查询：" + user2);
        // 此时不会访问数据库，直接从二级缓存获取
        
    } finally {
        session2.close();
    }
}
```

#### 5.2.6 二级缓存失效场景

1. **执行增删改操作**：任何 namespace 下的 INSERT/UPDATE/DELETE
2. **手动清空**：`sqlSession.clearCache()` 不影响二级缓存
3. **超过 flushInterval**：配置的刷新间隔到期
4. **缓存溢出**：超过 size 限制，按策略淘汰

---

### 5.3 一级缓存 vs 二级缓存 对比表

| 特性 | 一级缓存 | 二级缓存 |
|------|----------|----------|
| **作用域** | SqlSession 级别 | Mapper（namespace）级别 |
| **默认状态** | 开启 | 关闭（需手动配置） |
| **存储位置** | 内存（HashMap） | 可配置（内存/Redis/Ehcache） |
| **数据共享** | 不共享（SqlSession 隔离） | 共享（多个 SqlSession） |
| **序列化要求** | 不需要 | 需要（实体类实现 Serializable） |
| **生命周期** | 随 SqlSession 创建/销毁 | 应用级别，长期存在 |
| **清空时机** | 增删改、close、clearCache | 增删改、超时、溢出 |
| **命中率** | 低（单次会话） | 高（跨会话共享） |
| **适用场景** | 单次会话内重复查询 | 热点数据、配置数据 |

---

### 5.4 缓存使用建议

#### 5.4.1 推荐使用一级缓存的场景

```java
// ✅ 适合：单次事务内的重复查询
@Transactional
public void processOrder(Long orderId) {
    // 同一个 SqlSession 内多次查询
    Order order = orderMapper.selectById(orderId);
    User user = userMapper.selectById(order.getUserId());
    // 后续可能再次查询 order
    Order order2 = orderMapper.selectById(orderId); // 走一级缓存
}
```

#### 5.4.2 推荐使用二级缓存的场景

```java
// ✅ 适合：热点数据、不常变化的数据
// 如：系统配置、字典数据、商品分类

@CacheNamespace(
    eviction = CacheNamespace.Eviction.LRU,
    flushInterval = 3600000,  // 1小时刷新
    size = 1000
)
public interface SysConfigMapper {
    @Select("SELECT * FROM sys_config WHERE config_key = #{key}")
    SysConfig selectByKey(String key);
}
```

#### 5.4.3 不建议使用缓存的场景

```java
// ❌ 不适合缓存：实时性要求高的数据
// 如：库存、余额、订单状态

// ❌ 不适合缓存：数据量大的查询
// 如：分页查询、复杂条件查询

// ❌ 不适合缓存：频繁更新的数据
// 缓存命中率极低，反而增加开销
```

---

### 5.5 缓存相关问题排查

#### 5.5.1 缓存不生效的常见原因

```java
// 1. 二级缓存未开启
// 检查：Mapper.xml 中是否有 <cache/> 标签

// 2. 实体类未实现 Serializable
public class User implements Serializable {  // 必须实现
    private static final long serialVersionUID = 1L;
}

// 3. 在同一个 SqlSession 内测试二级缓存
// 二级缓存需要 SqlSession 关闭后才刷新
sqlSession.close();  // 必须关闭才能刷新到二级缓存

// 4. 配置了 readOnly="true" 但修改了对象
// readOnly=true 时返回的是缓存对象的引用
// 修改会影响缓存中的数据
```

#### 5.5.2 缓存穿透、击穿、雪崩

```java
// 缓存穿透：查询不存在的数据
// 解决：布隆过滤器或缓存空值

// 缓存击穿：热点数据过期，大量请求同时访问数据库
// 解决：互斥锁或逻辑过期

// 缓存雪崩：大量缓存同时过期
// 解决：随机过期时间、多级缓存
```

---

### 5.6 生产环境建议

```java
/**
 * 生产环境缓存使用建议：
 * 
 * 1. 一级缓存：保持默认开启，无需额外配置
 *    - 适用于单次会话内的重复查询
 *    - 自动管理，无需干预
 * 
 * 2. 二级缓存：谨慎使用，推荐替代方案
 *    - 简单场景：使用 MyBatis 自带二级缓存
 *    - 复杂场景：使用 Redis 等分布式缓存
 *    - 避免多表关联查询使用二级缓存
 * 
 * 3. 更好的选择：Spring Cache + Redis
 *    @Cacheable(value = "user", key = "#id")
 *    public User getUser(Long id) { ... }
 */
```

---

## 高频面试题

**问题 1:MyBatis的 #{}和 ${}有什么区别？**

**答**：

| 特性   | #{}         | ${}        |
|------|-------------|------------|
| 处理方式 | 预编译，参数绑定    | 直接字符串替换    |
| 安全性  | 安全，防 SQL 注入 | 有 SQL 注入风险 |
| 使用场景 | 参数值         | 表名、列名、排序   |
| 性能   | 可缓存执行计划     | 每次生成新 SQL  |

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

| 特性   | 一级缓存                   | 二级缓存              |
|------|------------------------|-------------------|
| 作用域  | SqlSession             | Mapper（namespace） |
| 默认开启 | 是                      | 否                 |
| 生命周期 | SqlSession 关闭或 DML 后清除 | 应用级别              |
| 适用场景 | 单次会话内重复查询              | 跨会话共享             |

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
