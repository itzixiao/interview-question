# MyBatis-Plus 快速入门

## 什么是 MyBatis-Plus

MyBatis-Plus 是 MyBatis 的增强工具，在 MyBatis 基础上只做增强不做改变，为简化开发、提高效率而生。

## 核心特性

| 特性                     | 说明                                                             |
|------------------------|----------------------------------------------------------------|
| **无侵入**                | 只做增强不做改变，引入它不会对现有工程产生影响                                        |
| **损耗小**                | 启动即会自动注入基本 CRUD，性能基本无损耗                                        |
| **强大的 CRUD 操作**        | 内置通用 Mapper、通用 Service，少量配置即可实现单表大部分 CRUD 操作                   |
| **支持 Lambda 形式调用**     | 方便编写各类查询条件，无需担心字段写错                                            |
| **支持主键自动生成**           | 支持多达 4 种主键策略                                                   |
| **支持 ActiveRecord 模式** | 实体类只需继承 Model 类即可进行强大的 CRUD 操作                                 |
| **内置分页插件**             | 基于 MyBatis 物理分页，开发者无需关心具体操作                                    |
| **分页插件支持多种数据库**        | 支持 MySQL、Oracle、DB2、H2、HSQL、SQLite、PostgreSQL、SQLServer 等多种数据库 |

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.3.1</version>
</dependency>
```

### 2. 配置数据源

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mydb?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 3. 创建实体类

```java
@TableName("sys_user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String username;
    private String email;
    private Integer age;
    
    @Version
    private Integer version;
    
    @TableLogic
    private Integer deleted;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

### 4. 创建 Mapper 接口

```java
public interface UserMapper extends BaseMapper<User> {
    // 自定义方法
    List<User> selectByAgeRange(@Param("minAge") Integer minAge, 
                                 @Param("maxAge") Integer maxAge);
}
```

### 5. 使用 Service 层

```java
public interface UserService extends IService<User> {
    boolean updateStatus(Long userId, Integer status);
}

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> 
        implements UserService {
    @Override
    public boolean updateStatus(Long userId, Integer status) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId).set(User::getStatus, status);
        return update(wrapper);
    }
}
```

## 常用注解

| 注解            | 用途                |
|---------------|-------------------|
| `@TableName`  | 指定表名              |
| `@TableId`    | 主键注解，支持多种生成策略     |
| `@TableField` | 字段注解，可指定字段名、填充策略等 |
| `@TableLogic` | 逻辑删除              |
| `@Version`    | 乐观锁版本号            |
| `@EnumValue`  | 枚举映射              |

## 条件构造器

### QueryWrapper（传统方式）

```java
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("status", 1)
       .like("username", "admin")
       .ge("age", 18)
       .orderByDesc("create_time");
List<User> list = userMapper.selectList(wrapper);
```

### LambdaQueryWrapper（推荐）

```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getStatus, 1)
       .like(User::getUsername, "admin")
       .ge(User::getAge, 18)
       .orderByDesc(User::getCreateTime);
List<User> list = userMapper.selectList(wrapper);
```

## 分页查询

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
Page<User> result = userMapper.selectPage(page, wrapper);
```

## 代码生成器

MyBatis-Plus 提供代码生成器，可以快速生成 Entity、Mapper、Service、Controller 代码。

```java
FastAutoGenerator.create("jdbc:mysql://localhost:3306/mydb","root","root")
    .

globalConfig(builder ->{
        builder.

author("itzixiao")
               .

outputDir(System.getProperty("user.dir") +"/src/main/java");
        })
        .

packageConfig(builder ->{
        builder.

parent("cn.itzixiao.interview");
    })
            .

strategyConfig(builder ->{
        builder.

addInclude("sys_user","sys_role");
    })
            .

execute();
```

## 总结

MyBatis-Plus 极大地简化了 MyBatis 的使用，提供了丰富的功能和便捷的操作方式，是 Java 开发中不可或缺的利器。
