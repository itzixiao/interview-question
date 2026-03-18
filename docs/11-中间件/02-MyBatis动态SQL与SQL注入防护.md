# MyBatis 动态 SQL 与 SQL 注入防护

## 动态 SQL 概述

MyBatis 的动态 SQL 功能可以基于不同条件动态生成 SQL 语句，避免了繁琐的字符串拼接。

## 动态 SQL 标签

| 标签            | 作用                     |
|---------------|------------------------|
| `<if>`        | 条件判断                   |
| `<choose>`    | 多选一（类似 Java switch）    |
| `<when>`      | choose 的分支             |
| `<otherwise>` | choose 的默认分支           |
| `<where>`     | 智能处理 WHERE 关键字和 AND/OR |
| `<set>`       | 智能处理 SET 关键字和逗号        |
| `<trim>`      | 自定义前缀/后缀处理             |
| `<foreach>`   | 遍历集合（用于 IN 语句）         |
| `<bind>`      | 创建变量绑定                 |

## XML 方式示例

### 动态查询

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

### 动态更新

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

### 批量插入

```xml
<insert id="batchInsert">
    INSERT INTO user (username, email, status) VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.username}, #{item.email}, #{item.status})
    </foreach>
</insert>
```

## 注解方式示例

### @SelectProvider

```java
@SelectProvider(type = UserSqlProvider.class, method = "findByConditionSql")
List<User> findByCondition(@Param("username") String username,
                           @Param("status") Integer status);

class UserSqlProvider {
    public String findByConditionSql(@Param("username") String username,
                                     @Param("status") Integer status) {
        return new SQL() {{
            SELECT("*");
            FROM("user");
            if (username != null) {
                WHERE("username LIKE CONCAT('%', #{username}, '%')");
            }
            if (status != null) {
                WHERE("status = #{status}");
            }
        }}.toString();
    }
}
```

## SQL 注入防护

### 什么是 SQL 注入

SQL 注入是最常见的 Web 安全漏洞，攻击者通过构造特殊输入，改变 SQL 语句结构，执行非授权操作。

### 攻击示例

```java
// 恶意输入
String username = "admin' OR '1'='1";

// 危险代码（使用 ${}）
@Select("SELECT * FROM user WHERE username = '${username}'")
// 实际执行的 SQL：
// SELECT * FROM user WHERE username = 'admin' OR '1'='1'
// 结果：查询到所有用户！
```

### 防护方案

#### 1. 使用 #{}（推荐）

```java
@Select("SELECT * FROM user WHERE username = #{username}")
User findByUsername(@Param("username") String username);
```

`#{}` 使用预编译语句，参数会被转义，防止 SQL 注入。

#### 2. 必须使用 ${} 时的白名单校验

某些场景必须使用 `${}`，如动态排序、表名：

```java
public List<User> findAllWithOrder(String orderBy, String orderDirection) {
    // 白名单校验
    String safeOrderBy = validateOrderBy(orderBy);
    String safeOrderDirection = validateOrderDirection(orderDirection);
    
    return mapper.findAllWithOrderInternal(safeOrderBy, safeOrderDirection);
}

private String validateOrderBy(String orderBy) {
    String[] allowedFields = {"id", "username", "create_time"};
    for (String field : allowedFields) {
        if (field.equals(orderBy)) {
            return field;
        }
    }
    return "id"; // 默认排序字段
}
```

#### 3. 模糊查询的安全写法

```java
// 安全
@Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%')")

// 危险（如果 keyword 包含 % 或 _ 可能导致全表扫描）
@Select("SELECT * FROM user WHERE username LIKE '%${keyword}%'")
```

### #{} vs ${}

| 特性   | #{}      | ${}        |
|------|----------|------------|
| 处理方式 | 预编译，参数绑定 | 直接字符串替换    |
| 安全性  | 安全，自动转义  | 危险，需白名单校验  |
| 使用场景 | 参数值      | 表名、列名、排序字段 |
| 性能   | 可缓存执行计划  | 每次生成不同 SQL |

## 最佳实践

1. **优先使用 #{}**，除非必须使用 ${}
2. **使用 ${} 时必须进行白名单校验**
3. **排序字段、表名使用白名单限制**
4. **数据库账号遵循最小权限原则**
5. **启用 MyBatis 日志，监控执行的 SQL**
6. **使用 SQL 注入检测工具进行安全扫描**

## 总结

MyBatis 的动态 SQL 功能强大，但需要注意 SQL 注入风险。始终优先使用 `#{}`，在必须使用 `${}` 的场景下，务必进行白名单校验，确保应用安全。
