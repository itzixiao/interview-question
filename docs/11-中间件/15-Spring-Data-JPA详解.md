# Spring Data JPA 详解

## 一、概述

### 1.1 什么是 JPA

**JPA（Java Persistence API）** 是 Java EE 规范中定义的 ORM（对象关系映射）标准接口，由 JSR-338 规范定义。

```
┌──────────────────────────────────────────────────────────┐
│                      JPA 技术栈                            │
├──────────────────────────────────────────────────────────┤
│  应用层                                                    │
│   └── Spring Data JPA（Repository 接口、查询方法派生）        │
│         ↓                                                 │
│  JPA 规范层                                                │
│   └── javax.persistence / jakarta.persistence API        │
│         ↓                                                 │
│  ORM 实现层                                                │
│   └── Hibernate（默认）/ EclipseLink / OpenJPA             │
│         ↓                                                 │
│  JDBC 层                                                  │
│   └── 数据库（MySQL / PostgreSQL / Oracle）                 │
└──────────────────────────────────────────────────────────┘
```

### 1.2 核心概念对比

| 概念                   | 说明                  | 类比 MyBatis     |
|----------------------|---------------------|----------------|
| `EntityManager`      | JPA 核心 API，管理实体生命周期 | SqlSession     |
| `Entity`             | 映射数据库表的 Java 对象     | POJO + XML     |
| `Repository`         | Spring Data 数据访问接口  | Mapper 接口      |
| `JPQL`               | 面向对象的查询语言           | HQL / SQL      |
| `Criteria API`       | 类型安全的动态查询           | MyBatis 动态 SQL |
| `PersistenceContext` | 一级缓存，管理实体状态         | SqlSession 缓存  |

### 1.3 与 MyBatis 对比

| 维度     | Spring Data JPA    | MyBatis       |
|--------|--------------------|---------------|
| 学习曲线   | 较陡（需理解实体状态）        | 较平（SQL 直观）    |
| 代码量    | 少（方法名派生查询）         | 多（需写 SQL/XML） |
| SQL 控制 | 弱（复杂 SQL 需 @Query） | 强（完全自定义）      |
| 性能优化   | 较难精确控制             | 精确控制          |
| 数据库移植性 | 好（方言自动切换）          | 差（SQL 绑定数据库）  |
| 适用场景   | CRUD 为主、快速开发       | 复杂 SQL、报表查询   |

---

## 二、快速开始

### 2.1 添加依赖

```xml
<!-- Spring Boot JPA Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

        <!-- MySQL 驱动 -->
<dependency>
<groupId>com.mysql</groupId>
<artifactId>mysql-connector-j</artifactId>
<scope>runtime</scope>
</dependency>
```

### 2.2 配置文件

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/interview?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update        # create / create-drop / update / validate / none
    show-sql: true            # 打印 SQL（生产环境关闭）
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true      # 格式化 SQL 输出
        use_sql_comments: true # SQL 注释
    open-in-view: false       # 关闭 OSIV（Open Session In View），推荐生产环境关闭
```

**`ddl-auto` 选项说明**：

| 值             | 说明             | 适用环境       |
|---------------|----------------|------------|
| `create`      | 启动时删旧表建新表      | 仅测试        |
| `create-drop` | 启动建表，关闭删表      | 仅测试        |
| `update`      | 自动增量更新表结构      | 开发环境       |
| `validate`    | 校验实体与表是否匹配，不修改 | 生产环境       |
| `none`        | 不做任何操作         | 生产环境（手动建表） |

---

## 三、实体映射

### 3.1 基础注解

```java
import jakarta.persistence.*;  // Spring Boot 3.x（Jakarta EE）
// import javax.persistence.*;  // Spring Boot 2.x（Java EE）

@Data
@Entity
@Table(name = "user", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_email", columnList = "email", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 自增主键
    private Long id;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "age")
    private Integer age;

    /**
     * 枚举映射：推荐 STRING，存储枚举名称（可读性好）
     * 不推荐 ORDINAL，枚举新增时会错位
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private UserStatus status;

    /** 大文本字段 */
    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 瞬态字段，不映射到数据库 */
    @Transient
    private String fullInfo;

    /** 版本号字段，用于乐观锁 */
    @Version
    private Long version;

    /** 自动填充创建时间 */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /** 自动填充更新时间 */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
```

### 3.2 主键生成策略

```java
// 1. 数据库自增（最常用，MySQL）
@GeneratedValue(strategy = GenerationType.IDENTITY)

// 2. 序列（Oracle、PostgreSQL）
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
@SequenceGenerator(name = "user_seq", sequenceName = "user_sequence", allocationSize = 1)

// 3. 表生成器（数据库无关，但性能差）
@GeneratedValue(strategy = GenerationType.TABLE)

// 4. 自动选择（默认，根据数据库类型选择）
@GeneratedValue(strategy = GenerationType.AUTO)

// 5. UUID（Spring Boot 3.x + Hibernate 6）
@Id
@UuidGenerator
private UUID id;
```

### 3.3 关联关系映射

#### 多对一 / 一对多

```java
// 订单（多）→ 用户（一）
@Entity
@Table(name = "order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 多对一：多个订单属于一个用户 */
    @ManyToOne(fetch = FetchType.LAZY)  // 推荐 LAZY，避免 N+1 问题
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private BigDecimal amount;
}

// 用户（一）→ 订单（多）
@Entity
public class User {

    // ...

    /** 一对多：一个用户有多个订单，mappedBy 指向 Order.user 字段 */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();
}
```

#### 多对多

```java

@Entity
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** 多对多：学生选课 */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "student_course",           // 中间表名
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}
```

#### 一对一

```java

@Entity
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 一对一：共享主键（profile 的 id = user 的 id） */
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String bio;
    private String avatar;
}
```

### 3.4 继承映射策略

```java
// 策略一：单表继承（推荐，性能最好）
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal amount;
}

@Entity
@DiscriminatorValue("ALIPAY")
public class AlipayPayment extends Payment {
    private String alipayOrderId;
}

@Entity
@DiscriminatorValue("WECHAT")
public class WechatPayment extends Payment {
    private String transactionId;
}
```

---

## 四、Repository 接口

### 4.1 接口层级

```
Repository（标记接口）
    └── CrudRepository<T, ID>（CRUD 基础操作）
            └── PagingAndSortingRepository<T, ID>（分页排序）
                    └── JpaRepository<T, ID>（JPA 扩展，最常用）
                            └── JpaSpecificationExecutor<T>（Criteria 动态查询）
```

### 4.2 JpaRepository 内置方法

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // 内置方法（无需实现）：
    // save(T entity)            保存或更新（根据 id 判断）
    // saveAll(Iterable<T>)      批量保存
    // findById(ID id)           按 id 查询，返回 Optional<T>
    // findAll()                 查所有
    // findAll(Sort sort)        带排序查所有
    // findAll(Pageable page)    分页查询
    // deleteById(ID id)         按 id 删除
    // deleteAll()               删所有
    // count()                   统计总数
    // existsById(ID id)         是否存在
    // flush()                   刷新到数据库
    // saveAndFlush(T entity)    保存并立即刷新
}
```

### 4.3 方法名派生查询

Spring Data JPA 根据**方法名**自动生成 SQL，无需手写任何代码：

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // findBy + 字段名
    Optional<User> findByUsername(String username);

    // findBy + 多字段（And / Or）
    List<User> findByUsernameAndStatus(String username, UserStatus status);

    List<User> findByUsernameOrEmail(String username, String email);

    // 比较操作（Between / LessThan / GreaterThan / After / Before）
    List<User> findByAgeBetween(int min, int max);

    List<User> findByAgeGreaterThan(int age);

    List<User> findByCreateTimeAfter(LocalDateTime time);

    // 模糊查询（Like / Containing / StartingWith / EndingWith）
    List<User> findByUsernameLike(String pattern);          // 需手动加 %

    List<User> findByUsernameContaining(String keyword);    // 自动加 %keyword%

    List<User> findByUsernameStartingWith(String prefix);   // prefix%

    // 空值判断（IsNull / IsNotNull）
    List<User> findByEmailIsNull();

    List<User> findByEmailIsNotNull();

    // 集合包含（In / NotIn）
    List<User> findByStatusIn(List<UserStatus> statuses);

    List<User> findByIdNotIn(List<Long> ids);

    // 布尔判断（True / False）
    List<User> findByActiveTrue();

    // 排序（OrderBy + Asc / Desc）
    List<User> findByStatusOrderByCreateTimeDesc(UserStatus status);

    // 统计 / 存在
    long countByStatus(UserStatus status);

    boolean existsByEmail(String email);

    // 删除
    void deleteByStatus(UserStatus status);

    long deleteByCreateTimeBefore(LocalDateTime time);

    // 限制结果（Top / First）
    User findFirstByOrderByCreateTimeDesc();

    List<User> findTop10ByStatusOrderByCreateTimeDesc(UserStatus status);
}
```

**关键字汇总**：

| 关键字             | 生成 SQL                    | 示例                                  |
|-----------------|---------------------------|-------------------------------------|
| `And`           | `WHERE a = ? AND b = ?`   | `findByUsernameAndAge`              |
| `Or`            | `WHERE a = ? OR b = ?`    | `findByUsernameOrEmail`             |
| `Between`       | `WHERE a BETWEEN ? AND ?` | `findByAgeBetween`                  |
| `LessThan`      | `WHERE a < ?`             | `findByAgeLessThan`                 |
| `GreaterThan`   | `WHERE a > ?`             | `findByAgeGreaterThan`              |
| `Like`          | `WHERE a LIKE ?`          | `findByNameLike`                    |
| `Containing`    | `WHERE a LIKE %?%`        | `findByNameContaining`              |
| `StartingWith`  | `WHERE a LIKE ?%`         | `findByNameStartingWith`            |
| `In`            | `WHERE a IN (...)`        | `findByStatusIn`                    |
| `NotIn`         | `WHERE a NOT IN (...)`    | `findByIdNotIn`                     |
| `IsNull`        | `WHERE a IS NULL`         | `findByEmailIsNull`                 |
| `IsNotNull`     | `WHERE a IS NOT NULL`     | `findByEmailIsNotNull`              |
| `OrderBy`       | `ORDER BY a ASC/DESC`     | `findByStatusOrderByCreateTimeDesc` |
| `Top` / `First` | `LIMIT n`                 | `findTop10By...`                    |

---

## 五、自定义查询

### 5.1 @Query 注解（JPQL）

```java
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * JPQL 查询：面向实体对象，不是表名和列名
     * User 是实体类名，u.username 是字段名
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.status = :status")
    Optional<User> findByUsernameAndStatus(
            @Param("username") String username,
            @Param("status") UserStatus status);

    /** 返回部分字段：使用构造函数表达式 */
    @Query("SELECT new cn.itzixiao.dto.UserDTO(u.id, u.username, u.email) FROM User u WHERE u.status = :status")
    List<UserDTO> findUserDTOByStatus(@Param("status") UserStatus status);

    /** 模糊查询 */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
    List<User> searchByKeyword(@Param("keyword") String keyword);

    /** 聚合查询 */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") UserStatus status);

    @Query("SELECT COALESCE(SUM(u.age), 0) FROM User u")
    Long sumAge();

    /** 分页查询（配合 Pageable） */
    @Query("SELECT u FROM User u WHERE u.status = :status ORDER BY u.createTime DESC")
    Page<User> findActiveUsers(@Param("status") UserStatus status, Pageable pageable);
}
```

### 5.2 @Query 注解（原生 SQL）

```java
/**
 * nativeQuery = true 使用原生 SQL
 * 适用于复杂 SQL、数据库特定函数
 */
@Query(
        value = "SELECT u.id, u.username, COUNT(o.id) AS order_count " +
                "FROM user u LEFT JOIN `order` o ON u.id = o.user_id " +
                "GROUP BY u.id, u.username " +
                "ORDER BY order_count DESC LIMIT :limit",
        nativeQuery = true
)
List<Object[]> findUserOrderCounts(@Param("limit") int limit);

/** 原生 SQL 分页（需指定 countQuery） */
@Query(
        value = "SELECT * FROM user WHERE status = :status",
        countQuery = "SELECT COUNT(*) FROM user WHERE status = :status",
        nativeQuery = true
)
Page<User> findByStatusNative(@Param("status") String status, Pageable pageable);
```

### 5.3 @Modifying 更新/删除

```java
/**
 * @Modifying：标识该查询为更新操作（INSERT/UPDATE/DELETE）
 * @Transactional：更新操作必须在事务中
 * clearAutomatically = true：执行后清除一级缓存（防止脏读）
 */
@Modifying(clearAutomatically = true)
@Transactional
@Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
int updateStatus(@Param("id") Long id, @Param("status") UserStatus status);

@Modifying
@Transactional
@Query("DELETE FROM User u WHERE u.createTime < :time AND u.status = 'INACTIVE'")
int deleteInactiveUsers(@Param("time") LocalDateTime time);

/** 批量更新 */
@Modifying
@Transactional
@Query("UPDATE User u SET u.lastLoginTime = :time WHERE u.id IN :ids")
int batchUpdateLastLoginTime(@Param("ids") List<Long> ids, @Param("time") LocalDateTime time);
```

### 5.4 Specification 动态查询

适用于多条件动态过滤（相当于 MyBatis 的 `<if>` 标签）：

```java
// Repository 需继承 JpaSpecificationExecutor
public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {
}

// 构建 Specification
public class UserSpec {

    public static Specification<User> usernameContains(String keyword) {
        return (root, query, cb) ->
                keyword == null ? null :
                        cb.like(root.get("username"), "%" + keyword + "%");
    }

    public static Specification<User> statusEquals(UserStatus status) {
        return (root, query, cb) ->
                status == null ? null :
                        cb.equal(root.get("status"), status);
    }

    public static Specification<User> ageBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("age"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("age"), min);
            return cb.between(root.get("age"), min, max);
        };
    }

    public static Specification<User> createTimeBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start == null) return cb.lessThanOrEqualTo(root.get("createTime"), end);
            if (end == null) return cb.greaterThanOrEqualTo(root.get("createTime"), start);
            return cb.between(root.get("createTime"), start, end);
        };
    }
}

// Service 层调用（组合多个条件）
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Page<User> search(UserSearchRequest req, Pageable pageable) {
        Specification<User> spec = Specification
                .where(UserSpec.usernameContains(req.getKeyword()))
                .and(UserSpec.statusEquals(req.getStatus()))
                .and(UserSpec.ageBetween(req.getMinAge(), req.getMaxAge()))
                .and(UserSpec.createTimeBetween(req.getStartTime(), req.getEndTime()));

        return userRepository.findAll(spec, pageable);
    }
}
```

---

## 六、分页与排序

### 6.1 Pageable 分页

```java
// Controller 接收分页参数
@GetMapping("/users")
public Page<User> getUsers(
        @RequestParam(defaultValue = "0") int page,     // 页码从 0 开始
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createTime") String sort,
        @RequestParam(defaultValue = "DESC") String direction) {

    Sort.Direction dir = Sort.Direction.fromString(direction);
    Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));

    return userRepository.findAll(pageable);
}

// 多字段排序
Pageable pageable = PageRequest.of(0, 10,
        Sort.by(Sort.Order.desc("createTime"),
                Sort.Order.asc("username")));
```

### 6.2 自定义分页返回

```java
// Page 对象包含：content（数据列表）、totalElements、totalPages、number、size
Page<User> page = userRepository.findAll(pageable);

// 转换为前端友好的 Map 格式
Map<String, Object> result = new HashMap<>();
result.

put("list",page.getContent());
        result.

put("total",page.getTotalElements());
        result.

put("totalPages",page.getTotalPages());
        result.

put("currentPage",page.getNumber());
        result.

put("pageSize",page.getSize());
        result.

put("hasNext",page.hasNext());
        result.

put("hasPrevious",page.hasPrevious());
```

### 6.3 Slice vs Page

```java
// Page：执行 count 查询（总数），适合需要展示总页数的场景
Page<User> page = userRepository.findAll(pageable);

// Slice：不执行 count 查询（性能更好），适合"加载更多"场景
Slice<User> slice = userRepository.findByStatus(status, pageable);
boolean hasMore = slice.hasNext();
```

---

## 七、实体状态与生命周期

### 7.1 四种实体状态

```
New（瞬态）─── persist() ───▶ Managed（持久态）─── detach() ───▶ Detached（游离态）
     ▲                              │                                    │
     │                              │ flush/commit                       │ merge()
     │                              ▼                                    │
     └────────────────────── Database ◀──────────────────────────────────┘
                                    │
                              remove()
                                    │
                                    ▼
                             Removed（删除态）
```

| 状态            | 说明                          | 是否受 EntityManager 管理 |
|---------------|-----------------------------|----------------------|
| New（瞬态）       | `new` 创建的对象，未与数据库关联         | 否                    |
| Managed（持久态）  | 通过 `save()`/`find()` 获取，受管理 | 是                    |
| Detached（游离态） | 脱离 EntityManager（事务结束后）     | 否                    |
| Removed（删除态）  | 调用 `delete()` 后，等待提交        | 是（标记删除）              |

### 7.2 重要行为说明

```java

@Transactional
public void example(Long userId) {
    // 1. 查询 → Managed 状态
    User user = userRepository.findById(userId).orElseThrow();

    // 2. 修改 → Hibernate 自动脏检查（Dirty Checking）
    //    事务提交时自动 UPDATE，无需手动调用 save()
    user.setUsername("newName");

    // 3. 事务结束 → user 变为 Detached 状态
}

@Transactional
public void mergeExample(User detachedUser) {
    // merge() 将游离态变为持久态（如果 id 存在则 UPDATE，否则 INSERT）
    userRepository.save(detachedUser);
}
```

**自动脏检查（Dirty Checking）**：

- 在事务中查询的实体处于 Managed 状态
- 事务提交前，Hibernate 会比较快照与当前状态
- 有变化则自动执行 UPDATE，**无需手动调用 save()**
- 这也是新手常见困惑点：为什么没有 save() 却更新了数据库

### 7.3 生命周期回调

```java

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    /** persist() 前触发 */
    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
        log.info("准备插入用户: {}", username);
    }

    /** persist() 后触发 */
    @PostPersist
    public void postPersist() {
        log.info("用户已插入，id: {}", id);
    }

    /** update 前触发 */
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }

    /** remove() 前触发 */
    @PreRemove
    public void preRemove() {
        log.info("准备删除用户: {}", id);
    }
}
```

---

## 八、事务与锁

### 8.1 @Transactional 使用

```java

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 读操作：readOnly = true 提升性能
     * - 关闭 Dirty Checking（不检查实体变更）
     * - Hibernate 刷新模式设为 NEVER
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 写操作：默认 readOnly = false
     * rollbackFor：指定异常类型回滚（默认只有 RuntimeException）
     */
    @Transactional(rollbackFor = Exception.class)
    public User createUser(UserCreateRequest req) {
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        return userRepository.save(user);
    }

    /**
     * 传播行为：REQUIRES_NEW
     * 新开事务，与外部事务隔离（适用于日志记录等不能回滚的操作）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOperationLog(String operation) {
        // 即使外部事务回滚，此日志也会保存
    }
}
```

### 8.2 乐观锁

```java

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer stock;

    /** @Version 字段自动实现乐观锁 */
    @Version
    private Long version;
}

// 并发扣减库存
@Transactional
public void reduceStock(Long productId, int quantity) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("商品不存在"));

    if (product.getStock() < quantity) {
        throw new RuntimeException("库存不足");
    }

    product.setStock(product.getStock() - quantity);
    // Hibernate 自动在 UPDATE 语句中加 WHERE version = ?
    // 若 version 不匹配，抛出 OptimisticLockException
    productRepository.save(product);
}
```

### 8.3 悲观锁

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** 悲观写锁：SELECT ... FOR UPDATE */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    /** 悲观读锁：SELECT ... LOCK IN SHARE MODE */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForRead(@Param("id") Long id);
}
```

---

## 九、N+1 问题与性能优化

### 9.1 N+1 问题

```java
// ❌ 触发 N+1：查询所有用户后，每个用户再查一次订单
List<User> users = userRepository.findAll();
users.

forEach(u ->{
int orderCount = u.getOrders().size();  // 每次访问触发一条 SQL
});
// 生成 SQL：1 + N 条

// ✅ 解决方案一：JOIN FETCH（JPQL）
@Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.orders WHERE u.status = :status")
List<User> findWithOrders(@Param("status") UserStatus status);

// ✅ 解决方案二：@EntityGraph（不修改 Repository 方法）
@EntityGraph(attributePaths = {"orders", "profile"})
@Query("SELECT u FROM User u WHERE u.status = :status")
List<User> findWithOrdersAndProfile(@Param("status") UserStatus status);

// ✅ 解决方案三：@BatchSize（批量加载，减少 SQL 数量）
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
@BatchSize(size = 100)   // 一次加载 100 个关联对象
private List<Order> orders;
```

### 9.2 查询投影（减少数据传输）

```java
// 方式一：接口投影（Spring Data 自动实现）
public interface UserSummary {
    Long getId();

    String getUsername();

    String getEmail();
}

List<UserSummary> findByStatus(UserStatus status, Class<UserSummary> type);

// 方式二：DTO 类投影（JPQL 构造函数表达式）
@Query("SELECT new cn.itzixiao.dto.UserDTO(u.id, u.username) FROM User u")
List<UserDTO> findAllUserDTO();

// 方式三：原生 SQL + Tuple
@Query(value = "SELECT id, username FROM user WHERE status = ?1", nativeQuery = true)
List<Object[]> findIdAndUsername(String status);
```

### 9.3 批量操作优化

```java
// ❌ 逐条 save（每条生成一个 SQL）
users.forEach(userRepository::save);

// ✅ saveAll（Hibernate 可批量刷新）
userRepository.

saveAll(users);

// ✅ 配置批量大小（application.yml）
// spring.jpa.properties.hibernate.jdbc.batch_size: 50
// spring.jpa.properties.hibernate.order_inserts: true
// spring.jpa.properties.hibernate.order_updates: true

// ✅ 原生 SQL 批量插入（最高效）
@Modifying
@Transactional
@Query(value = "INSERT INTO user (username, email, status) VALUES ?1", nativeQuery = true)
void batchInsert(List<Object[]> data);
```

---

## 十、审计功能（Spring Data Auditing）

```java
// 1. 开启审计
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // 返回当前操作用户（从 Security Context 获取）
        return () -> Optional.ofNullable(
                SecurityContextHolder.getContext().getAuthentication()
        ).map(Authentication::getName);
    }
}

// 2. 审计基类（所有实体继承）
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @CreatedBy
    @Column(name = "create_by", updatable = false, length = 50)
    private String createBy;

    @LastModifiedBy
    @Column(name = "update_by", length = 50)
    private String updateBy;
}

// 3. 实体继承基类
@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    // ...
}
```

---

## 十一、高频面试题

**问题 1：JPA 和 Hibernate 是什么关系？**

**参考答案：**

- **JPA** 是 Java 官方定义的 ORM 规范（接口），位于 `jakarta.persistence` 包
- **Hibernate** 是 JPA 规范的具体实现（最流行的一种），另外还有 EclipseLink 等
- **Spring Data JPA** 是在 JPA 之上的封装，提供 Repository 接口、方法名派生查询等便捷功能
- 三者关系：Spring Data JPA → JPA 规范 → Hibernate 实现 → JDBC → 数据库

---

**问题 2：`@OneToMany` 的 `fetch` 默认值是什么？为什么推荐 LAZY？**

**参考答案：**

- `@OneToMany`、`@ManyToMany` 默认 `EAGER` 或 `LAZY`？答：`LAZY`
- `@ManyToOne`、`@OneToOne` 默认：`EAGER`
- **推荐 LAZY 的原因**：
    - EAGER 会在加载主实体时立即 JOIN 加载关联实体，即使不需要
    - 多个关联关系同时 EAGER 会导致笛卡尔积查询，性能急剧下降
    - LAZY 按需加载，需要时再触发 SQL，性能更可控

---

**问题 3：什么是 N+1 问题？如何解决？**

**参考答案：**

**N+1 问题**：查询 N 条记录时，关联集合懒加载导致额外触发 N 条 SQL（共 N+1 条）。

**解决方案**：

1. `JOIN FETCH`：JPQL 中联表加载 `LEFT JOIN FETCH u.orders`
2. `@EntityGraph`：声明式指定要加载的属性路径，不修改 SQL
3. `@BatchSize`：批量加载关联集合，N 条变为 N/batchSize 条
4. `@Query` + 原生 SQL：完全手控 JOIN
5. 查询投影（DTO/Interface）：只查必要字段，避免加载关联

---

**问题 4：JPA 的一级缓存和二级缓存分别是什么？**

**参考答案：**

| 缓存   | 范围                           | 生命周期    | 默认是否开启               |
|------|------------------------------|---------|----------------------|
| 一级缓存 | EntityManager（Session）级别     | 事务生命周期内 | 是                    |
| 二级缓存 | SessionFactory 级别（跨 Session） | 应用生命周期  | 否（需配置 EHCache/Redis） |

- **一级缓存**：同一事务内第二次查询同一 id 直接走缓存，不发 SQL
- **二级缓存**：跨事务、跨 Session 共享，需注意数据一致性
- `@Modifying(clearAutomatically = true)` 会清除一级缓存，防止 UPDATE 后读到旧数据

---

**问题 5：save() 与 saveAndFlush() 的区别？**

**参考答案：**

- `save()`：保存到一级缓存（Persistence Context），等待事务提交时才 flush 到数据库
- `saveAndFlush()`：立即 flush 到数据库（执行 SQL），但事务未提交，不可见
- **使用场景**：需要在同一事务中获取数据库生成的 id 或触发器结果时使用 `saveAndFlush()`

---

**问题 6：@Transactional(readOnly = true) 有什么效果？**

**参考答案：**

1. **关闭 Dirty Checking**：不创建实体快照，节省内存，提升性能
2. **设置 FlushMode 为 NEVER**：不向数据库 flush 变更
3. **数据库级别**：部分数据库驱动/连接池会将只读事务路由到读库（主从分离）
4. **注意**：只读事务中调用 `save()`/`delete()` 不会立即报错，但 flush 时会报错

---

**问题 7：乐观锁（@Version）和悲观锁（@Lock）如何选择？**

**参考答案：**

| 维度   | 乐观锁                                   | 悲观锁               |
|------|---------------------------------------|-------------------|
| 实现方式 | `@Version` + UPDATE WHERE version = ? | SELECT FOR UPDATE |
| 冲突假设 | 冲突少                                   | 冲突多               |
| 性能   | 高（无锁等待）                               | 低（加锁等待）           |
| 冲突处理 | 抛出 `OptimisticLockException`，需重试      | 自动等待，成功获取         |
| 适用场景 | 库存扣减（高并发 + 冲突少）                       | 转账（强一致性要求）        |
| 死锁风险 | 无                                     | 有                 |

---

**问题 8：Hibernate 的脏检查（Dirty Checking）是如何实现的？**

**参考答案：**

1. 实体进入 Managed 状态时，Hibernate 会保存一份**快照（Snapshot）**
2. 事务提交前（`flush` 时），将当前实体状态与快照逐字段对比
3. 有差异的字段生成 `UPDATE` SQL 执行
4. **实现原理**：Hibernate 使用字节码增强（Bytecode Enhancement）或 `equals()` 比较
5. **性能影响**：实体较多时脏检查开销大，`readOnly = true` 可跳过此过程

---

**问题 9：Spring Data JPA 如何实现分页？PageRequest 怎么用？**

**参考答案：**

```java
// 创建分页对象（页码从 0 开始）
Pageable pageable = PageRequest.of(
                pageNum - 1,           // 第几页（前端传 1，这里要减 1）
                pageSize,              // 每页条数
                Sort.by(Sort.Direction.DESC, "createTime")  // 排序
        );

// 调用 Repository
Page<User> page = userRepository.findAll(pageable);

// Page 对象信息
page.

getContent();         // 当前页数据
page.

getTotalElements();   // 总条数（执行了 COUNT 查询）
page.

getTotalPages();      // 总页数
page.

getNumber();          // 当前页码（从 0 开始）
page.

hasNext();            // 是否有下一页
```

---

**问题 10：`open-in-view` 是什么？为什么推荐关闭？**

**参考答案：**

- **Open Session In View（OSIV）**：Spring Boot 默认开启，将 EntityManager 的生命周期延伸到 View 层（HTTP 请求结束才关闭）
- **开启的问题**：
    - EntityManager 与数据库连接长时间占用（从请求开始到响应结束）
    - 在 View/Controller 层触发懒加载的 SQL，难以控制和优化
    - 高并发时数据库连接池耗尽
- **推荐关闭**：`spring.jpa.open-in-view: false`，在 Service 层事务内完成所有数据加载，传 DTO 到 Controller

---

## 十二、本项目中的 JPA 使用示例

本项目 `interview-spring-ai` 模块中使用了 Spring Data JPA 管理知识库文档：

| 文件                                            | 说明                                            |
|-----------------------------------------------|-----------------------------------------------|
| `entity/KnowledgeDocument.java`               | 实体类，含 `@CreationTimestamp`、`@UpdateTimestamp` |
| `repository/KnowledgeDocumentRepository.java` | Repository，含方法名派生查询 + `@Query` 聚合查询           |
| `service/RagService.java`                     | 业务层，含去重逻辑、批量持久化                               |
| `controller/KnowledgeBaseController.java`     | Controller，含统计聚合优化（2 次 SQL 替代 N+2 次）          |

**聚合查询避免 N+2 问题（见 `KnowledgeDocumentRepository`）**：

```java
// ❌ 错误做法：3 次 SQL（2 次全表扫描）
long documentCount = documentRepository.count();
long totalChunks = documentRepository.findAll().stream()
        .mapToLong(KnowledgeDocument::getChunkCount).sum();
long totalSize = documentRepository.findAll().stream()
        .mapToLong(KnowledgeDocument::getCharCount).sum();

// ✅ 正确做法：2 次 SQL（0 次全表遍历）
long documentCount = documentRepository.count();
List<Object[]> sums = documentRepository.sumChunksAndChars();

// sumChunksAndChars() 定义
@Query("SELECT COALESCE(SUM(d.chunkCount), 0), COALESCE(SUM(d.charCount), 0) FROM KnowledgeDocument d")
List<Object[]> sumChunksAndChars();
```

---

**维护者：** itzixiao  
**最后更新：** 2026-03-24  
**问题反馈：** 欢迎提 Issue 或 PR
