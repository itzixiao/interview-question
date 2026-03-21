# MongoDB 详解

## 一、MongoDB 概述

### 1.1 什么是 MongoDB

MongoDB 是一个基于分布式文件存储的开源数据库系统，属于 NoSQL 数据库类别。它将数据存储为灵活的、类似 JSON 的文档，这意味着字段可能因文档而异，数据结构可能随时间变化。

**核心特点：**

1. **文档模型** - 数据以 BSON 格式存储，支持嵌套结构
2. **高性能** - 内存映射文件、索引优化
3. **高可用** - 副本集自动故障转移
4. **水平扩展** - 分片集群支持海量数据
5. **丰富的查询语言** - 支持聚合管道、全文搜索

### 1.2 MongoDB vs 关系型数据库

```
┌─────────────────────────────────────────────────────────────────┐
│                    数据模型对比                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  关系型数据库（MySQL）                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  users 表                                                │   │
│  │  ┌─────┬──────────┬──────────────────────────────────┐  │   │
│  │  │ id  │ name     │ email                            │  │   │
│  │  ├─────┼──────────┼──────────────────────────────────┤  │   │
│  │  │ 1   │ 张三     │ zhangsan@example.com             │  │   │
│  │  │ 2   │ 李四     │ lisi@example.com                 │  │   │
│  │  └─────┴──────────┴──────────────────────────────────┘  │   │
│  │                                                          │   │
│  │  orders 表                                               │   │
│  │  ┌─────┬─────────┬──────────┬───────────┐              │   │
│  │  │ id  │ user_id │ product  │ amount    │              │   │
│  │  ├─────┼─────────┼──────────┼───────────┤              │   │
│  │  │ 1   │ 1       │ iPhone   │ 6999.00   │              │   │
│  │  │ 2   │ 1       │ MacBook  │ 12999.00  │              │   │
│  │  └─────┴─────────┴──────────┴───────────┘              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
│  文档数据库（MongoDB）                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  users 集合                                              │   │
│  │  {                                                       │   │
│  │    "_id": ObjectId("..."),                               │   │
│  │    "name": "张三",                                        │   │
│  │    "email": "zhangsan@example.com",                      │   │
│  │    "orders": [                                           │   │
│  │      { "product": "iPhone", "amount": 6999.00 },        │   │
│  │      { "product": "MacBook", "amount": 12999.00 }       │   │
│  │    ]                                                     │   │
│  │  }                                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 核心概念对比

| MongoDB | 关系型数据库 | 说明 |
|---------|-------------|------|
| Database | Database | 数据库 |
| Collection | Table | 集合/表 |
| Document | Row | 文档/行 |
| Field | Column | 字段/列 |
| Index | Index | 索引 |
| Embedded Document | JOIN | 嵌入式文档/连接 |
| Shard | Partition | 分片/分区 |

---

## 二、数据模型详解

### 2.1 BSON 数据格式

BSON（Binary JSON）是 MongoDB 的二进制编码格式：

```
┌─────────────────────────────────────────────────────────────────┐
│                    BSON 文档结构                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  {                                                               │
│    "_id": ObjectId("507f1f77bcf86cd799439011"),                 │
│    "name": "张三",                                               │
│    "age": 28,                                                    │
│    "email": "zhangsan@example.com",                             │
│    "createdAt": ISODate("2024-01-01T00:00:00Z"),                │
│    "tags": ["developer", "java", "mongodb"],                    │
│    "address": {                                                  │
│      "province": "北京",                                         │
│      "city": "北京市",                                           │
│      "street": "朝阳区xxx路"                                     │
│    },                                                            │
│    "scores": [85, 90, 78, 92],                                  │
│    "profile": BinData(0, "base64encoded..."),                   │
│    "active": true                                                │
│  }                                                               │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                    BSON 支持的数据类型                           │
├─────────────────────────────────────────────────────────────────┤
│  类型            │  说明                    │  示例              │
│  ─────────────────────────────────────────────────────────────  │
│  String         │  UTF-8 字符串           │  "hello"           │
│  Int32/Int64    │  32/64 位整数           │  42                │
│  Double         │  64 位浮点数            │  3.14159           │
│  Boolean        │  布尔值                 │  true/false        │
│  ObjectId       │  12 字节唯一标识        │  ObjectId("...")   │
│  DateTime       │  日期时间               │  ISODate("...")    │
│  Array          │  数组                   │  [1, 2, 3]         │
│  Object         │  嵌入式文档             │  {"a": 1}          │
│  Null           │  空值                   │  null              │
│  Binary         │  二进制数据             │  BinData(...)      │
│  Decimal128     │  高精度小数             │  NumberDecimal("3.14") │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 文档设计模式

**1. 嵌入式文档（Embedded Documents）**

```javascript
// 一对多关系：将订单嵌入用户文档
{
  "_id": ObjectId("..."),
  "name": "张三",
  "orders": [
    {
      "orderId": "ORD001",
      "product": "iPhone",
      "amount": 6999,
      "status": "completed"
    },
    {
      "orderId": "ORD002", 
      "product": "MacBook",
      "amount": 12999,
      "status": "pending"
    }
  ]
}

// 优点：单次读取获取所有数据，无需 JOIN
// 缺点：文档大小限制 16MB，更新时整体更新
```

**2. 引用式文档（References）**

```javascript
// 用户集合
{
  "_id": ObjectId("user001"),
  "name": "张三"
}

// 订单集合（引用用户）
{
  "_id": ObjectId("order001"),
  "userId": ObjectId("user001"),  // 引用用户 ID
  "product": "iPhone",
  "amount": 6999
}

// 优点：文档独立，避免数据冗余
// 缺点：需要额外查询或 $lookup 聚合
```

**3. 混合模式**

```javascript
// 订单集合：嵌入用户基本信息，引用用户完整信息
{
  "_id": ObjectId("order001"),
  "userId": ObjectId("user001"),
  "userInfo": {           // 嵌入常用信息（冗余）
    "name": "张三",
    "phone": "13800138000"
  },
  "product": "iPhone",
  "amount": 6999
}

// 优点：查询订单时无需 JOIN 即可显示用户基本信息
// 缺点：用户信息变更时需要同步更新
```

---

## 三、索引机制详解

### 3.1 索引类型

```
┌─────────────────────────────────────────────────────────────────┐
│                    MongoDB 索引类型                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. 单字段索引（Single Field Index）                            │
│     db.users.createIndex({ "name": 1 })                         │
│     ┌───────────────────────────────────────────────────────┐  │
│     │  name 索引（升序）                                     │  │
│     │  ├─ "张三" → 位置1                                     │  │
│     │  ├─ "李四" → 位置2                                     │  │
│     │  └─ "王五" → 位置3                                     │  │
│     └───────────────────────────────────────────────────────┘  │
│                                                                  │
│  2. 复合索引（Compound Index）                                  │
│     db.orders.createIndex({ "userId": 1, "createdAt": -1 })     │
│     ┌───────────────────────────────────────────────────────┐  │
│     │  userId + createdAt 复合索引                           │  │
│     │  ├─ user1, 2024-01-02                                  │  │
│     │  ├─ user1, 2024-01-01                                  │  │
│     │  ├─ user2, 2024-01-03                                  │  │
│     │  └─ user2, 2024-01-02                                  │  │
│     └───────────────────────────────────────────────────────┘  │
│                                                                  │
│  3. 多键索引（Multikey Index）- 数组字段                        │
│     db.products.createIndex({ "tags": 1 })                      │
│     ┌───────────────────────────────────────────────────────┐  │
│     │  tags 数组索引                                         │  │
│     │  ├─ "electronics" → [doc1, doc3]                       │  │
│     │  ├─ "phone" → [doc1, doc2]                             │  │
│     │  └─ "laptop" → [doc3]                                  │  │
│     └───────────────────────────────────────────────────────┘  │
│                                                                  │
│  4. 文本索引（Text Index）- 全文搜索                            │
│     db.articles.createIndex({ "content": "text" })              │
│                                                                  │
│  5. 地理空间索引（Geospatial Index）                            │
│     db.places.createIndex({ "location": "2dsphere" })           │
│                                                                  │
│  6. 哈希索引（Hashed Index）- 分片键                            │
│     db.users.createIndex({ "userId": "hashed" })                │
│                                                                  │
│  7. 通配符索引（Wildcard Index）                                │
│     db.products.createIndex({ "attributes.$**": 1 })            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 索引优化策略

**索引设计原则：**

1. **ESR 原则**（Equality-Sort-Range）
   - 等值查询字段放最前
   - 排序字段放中间
   - 范围查询字段放最后

```javascript
// 查询：status = 'active' AND createdAt > '2024-01-01' ORDER BY name
// 最优索引：{ status: 1, name: 1, createdAt: 1 }
db.orders.createIndex({ 
  "status": 1,      // Equality（等值查询）
  "name": 1,        // Sort（排序）
  "createdAt": 1    // Range（范围查询）
});
```

2. **覆盖索引查询**

```javascript
// 创建复合索引
db.users.createIndex({ "name": 1, "email": 1 });

// 覆盖索引查询：查询字段都在索引中，无需回表
db.users.find(
  { "name": "张三" },           // 查询条件使用索引
  { "_id": 0, "name": 1, "email": 1 }  // 只返回索引字段
);

// explain() 验证
db.users.find(...).explain("executionStats");
// 查看 "totalDocsExamined" 是否为 0
```

3. **索引交集**

```javascript
// 创建两个单字段索引
db.orders.createIndex({ "userId": 1 });
db.orders.createIndex({ "status": 1 });

// MongoDB 可以同时使用两个索引
db.orders.find({ 
  "userId": ObjectId("..."), 
  "status": "completed" 
});
```

### 3.3 索引监控

```javascript
// 查看集合索引
db.users.getIndexes();

// 查看索引大小
db.users.stats().indexSizes;

// 查看索引使用情况
db.users.aggregate([
  { $indexStats: {} }
]);

// 查看慢查询
db.system.profile.find({
  "millis": { $gt: 100 }
}).sort({ "ts": -1 }).limit(10);

// 开启慢查询日志
db.setProfilingLevel(1, 50);  // 记录超过 50ms 的查询
```

---

## 四、聚合管道详解

### 4.1 聚合管道阶段

```
┌─────────────────────────────────────────────────────────────────┐
│                    MongoDB 聚合管道                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  输入文档流                                                      │
│       │                                                          │
│       ▼                                                          │
│  ┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐   │
│  │ $match  │ ──► │ $group  │ ──► │ $sort   │ ──► │ $limit  │   │
│  │ 过滤    │     │ 分组    │     │ 排序    │     │ 限制    │   │
│  └─────────┘     └─────────┘     └─────────┘     └─────────┘   │
│       │                                                          │
│       ▼                                                          │
│  输出结果                                                        │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                    常用管道阶段                                  │
├─────────────────────────────────────────────────────────────────┤
│  阶段          │  功能                    │  SQL 对应           │
│  ─────────────────────────────────────────────────────────────  │
│  $match        │  过滤文档                │  WHERE              │
│  $group        │  分组聚合                │  GROUP BY           │
│  $sort         │  排序                    │  ORDER BY           │
│  $limit        │  限制结果数量            │  LIMIT              │
│  $skip         │  跳过指定数量            │  OFFSET             │
│  $project      │  选择/重命名字段         │  SELECT             │
│  $lookup       │  左连接                  │  LEFT OUTER JOIN    │
│  $unwind       │  展开数组                │  CROSS JOIN         │
│  $addFields    │  添加新字段              │  计算字段           │
│  $count        │  统计文档数量            │  COUNT              │
│  $facet        │  并行多个聚合            │  子查询             │
│  $bucket       │  分桶聚合                │  范围分组           │
│  $redact       │  字段级安全控制          │  行级安全           │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 聚合管道示例

**示例 1：订单统计分析**

```javascript
// 统计每个用户的订单数量和总金额
db.orders.aggregate([
  // 阶段1：过滤已完成的订单
  {
    $match: {
      status: "completed",
      createdAt: {
        $gte: ISODate("2024-01-01"),
        $lt: ISODate("2024-02-01")
      }
    }
  },
  
  // 阶段2：按用户分组，计算订单数和总金额
  {
    $group: {
      _id: "$userId",
      orderCount: { $sum: 1 },
      totalAmount: { $sum: "$amount" },
      avgAmount: { $avg: "$amount" },
      maxAmount: { $max: "$amount" },
      minAmount: { $min: "$amount" }
    }
  },
  
  // 阶段3：关联用户信息
  {
    $lookup: {
      from: "users",
      localField: "_id",
      foreignField: "_id",
      as: "userInfo"
    }
  },
  
  // 阶段4：展开用户信息数组
  {
    $unwind: "$userInfo"
  },
  
  // 阶段5：选择输出字段
  {
    $project: {
      _id: 0,
      userId: "$_id",
      userName: "$userInfo.name",
      orderCount: 1,
      totalAmount: 1,
      avgAmount: { $round: ["$avgAmount", 2] }
    }
  },
  
  // 阶段6：按总金额降序排序
  {
    $sort: { totalAmount: -1 }
  },
  
  // 阶段7：取前10名
  {
    $limit: 10
  }
]);
```

**示例 2：多维度分析（$facet）**

```javascript
// 一次性获取多个维度的统计数据
db.orders.aggregate([
  {
    $facet: {
      // 按状态统计
      byStatus: [
        { $group: { _id: "$status", count: { $sum: 1 } } }
      ],
      // 按日期统计
      byDate: [
        { 
          $group: { 
            _id: { $dateToString: { format: "%Y-%m-%d", date: "$createdAt" } },
            count: { $sum: 1 },
            totalAmount: { $sum: "$amount" }
          } 
        },
        { $sort: { _id: 1 } },
        { $limit: 30 }
      ],
      // 总体统计
      overall: [
        {
          $group: {
            _id: null,
            totalOrders: { $sum: 1 },
            totalAmount: { $sum: "$amount" },
            avgAmount: { $avg: "$amount" }
          }
        }
      ]
    }
  }
]);
```

**示例 3：数组展开与分组**

```javascript
// 统计商品标签的销售情况
db.orders.aggregate([
  // 展开商品数组
  { $unwind: "$items" },
  
  // 展开标签数组
  { $unwind: "$items.tags" },
  
  // 按标签分组统计
  {
    $group: {
      _id: "$items.tags",
      totalSales: { $sum: "$items.quantity" },
      totalRevenue: { 
        $sum: { $multiply: ["$items.price", "$items.quantity"] }
      }
    }
  },
  
  // 排序
  { $sort: { totalRevenue: -1 } }
]);
```

---

## 五、副本集与高可用

### 5.1 副本集架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    MongoDB 副本集架构                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                    ┌─────────────────┐                          │
│                    │   客户端应用    │                          │
│                    └────────┬────────┘                          │
│                             │                                    │
│                             ▼                                    │
│              ┌──────────────────────────────┐                   │
│              │      连接字符串配置           │                   │
│              │  mongodb://host1,host2,host3 │                   │
│              │  /db?replicaSet=rs0          │                   │
│              └──────────────────────────────┘                   │
│                             │                                    │
│         ┌───────────────────┼───────────────────┐               │
│         ▼                   ▼                   ▼               │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐       │
│  │   Primary   │     │  Secondary  │     │  Secondary  │       │
│  │   (主节点)   │────►│   (从节点)   │────►│   (从节点)   │       │
│  │             │     │             │     │             │       │
│  │  读写操作   │     │  只读操作   │     │  只读操作   │       │
│  │  优先级高   │     │  优先级中   │     │  优先级低   │       │
│  └─────────────┘     └─────────────┘     └─────────────┘       │
│         │                   │                   │               │
│         └───────────────────┼───────────────────┘               │
│                             │                                    │
│                             ▼                                    │
│              ┌──────────────────────────────┐                   │
│              │     心跳检测 + 数据同步       │                   │
│              │     OpLog 复制机制           │                   │
│              └──────────────────────────────┘                   │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                    故障转移流程                                  │
├─────────────────────────────────────────────────────────────────┤
│  1. 主节点故障                                                  │
│     └─► 心跳检测失败（默认2秒间隔）                             │
│                                                                  │
│  2. 选举新主节点                                                │
│     └─► 从节点发起选举                                          │
│     └─► 获得多数票的从节点成为新主节点                          │
│     └─► 选举超时：10-30秒                                       │
│                                                                  │
│  3. 客户端重连                                                  │
│     └─► 自动发现新主节点                                        │
│     └─► 重新建立连接                                            │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 副本集配置

```javascript
// 初始化副本集配置
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "mongo1:27017", priority: 2 },
    { _id: 1, host: "mongo2:27017", priority: 1 },
    { _id: 2, host: "mongo3:27017", priority: 1, arbiterOnly: true }
  ],
  settings: {
    heartbeatIntervalMillis: 2000,    // 心跳间隔
    electionTimeoutMillis: 10000,     // 选举超时
    catchUpTimeoutMillis: 60000       // 追赶超时
  }
});

// 查看副本集状态
rs.status();

// 查看副本集配置
rs.conf();

// 添加仲裁节点（不存储数据，只参与投票）
rs.addArb("mongo4:27017");

// 添加从节点
rs.add("mongo5:27017");

// 移除节点
rs.remove("mongo5:27017");
```

### 5.3 读写分离配置

```javascript
// 连接字符串指定读偏好
// primary: 只从主节点读（默认）
// primaryPreferred: 优先主节点，不可用时读从节点
// secondary: 只从从节点读
// secondaryPreferred: 优先从节点，不可用时读主节点
// nearest: 读网络延迟最低的节点

// Java 客户端配置
MongoClientSettings settings = MongoClientSettings.builder()
    .applyConnectionString(new ConnectionString(
        "mongodb://mongo1,mongo2,mongo3/?replicaSet=rs0"
    ))
    .readPreference(ReadPreference.secondaryPreferred())
    .writeConcern(WriteConcern.MAJORITY)
    .build();
```

---

## 六、分片集群详解

### 6.1 分片架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    MongoDB 分片集群架构                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                    ┌─────────────────┐                          │
│                    │   mongos 路由   │                          │
│                    │   (可部署多个)   │                          │
│                    └────────┬────────┘                          │
│                             │                                    │
│         ┌───────────────────┼───────────────────┐               │
│         ▼                   ▼                   ▼               │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐       │
│  │ Config      │     │ Config      │     │ Config      │       │
│  │ Server 1    │     │ Server 2    │     │ Server 3    │       │
│  │ (配置服务)   │     │ (配置服务)   │     │ (配置服务)   │       │
│  └─────────────┘     └─────────────┘     └─────────────┘       │
│         │                   │                   │               │
│         └───────────────────┼───────────────────┘               │
│                             │                                    │
│                    存储集群元数据                                │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                    分片存储层                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      Shard 1                              │   │
│  │  ┌─────────────────────────────────────────────────────┐ │   │
│  │  │  副本集: rs1                                         │ │   │
│  │  │  Primary ──► Secondary ──► Secondary                 │ │   │
│  │  │  分片键范围: [minKey, 100)                           │ │   │
│  │  └─────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      Shard 2                              │   │
│  │  ┌─────────────────────────────────────────────────────┐ │   │
│  │  │  副本集: rs2                                         │ │   │
│  │  │  Primary ──► Secondary ──► Secondary                 │ │   │
│  │  │  分片键范围: [100, 200)                              │ │   │
│  │  └─────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      Shard 3                              │   │
│  │  ┌─────────────────────────────────────────────────────┐ │   │
│  │  │  副本集: rs3                                         │ │   │
│  │  │  Primary ──► Secondary ──► Secondary                 │ │   │
│  │  │  分片键范围: [200, maxKey)                           │ │   │
│  │  └─────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 分片键选择

**分片键类型：**

| 分片键类型 | 特点 | 适用场景 |
|-----------|------|---------|
| 范围分片 | 按范围划分，支持范围查询 | 时间序列、有序数据 |
| 哈希分片 | 均匀分布，不支持范围查询 | 随机访问、高写入 |
| 组合分片 | 多字段组合 | 复杂查询场景 |

```javascript
// 启用分片
sh.enableSharding("mydb");

// 范围分片
sh.shardCollection("mydb.orders", { "userId": 1 });

// 哈希分片
sh.shardCollection("mydb.users", { "userId": "hashed" });

// 组合分片
sh.shardCollection("mydb.logs", { "appId": 1, "timestamp": 1 });

// 查看分片状态
sh.status();

// 手动迁移块
sh.moveChunk("mydb.orders", { userId: 100 }, "shard2");
```

---

## 七、Java 客户端实战

### 7.1 Maven 依赖

```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>4.11.0</version>
</dependency>

<!-- Spring Data MongoDB -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### 7.2 Spring Data MongoDB 配置

```java
package cn.itzixiao.interview.mongodb.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB 配置类
 * 
 * <p>配置说明：</p>
 * <ul>
 *   <li>uri: MongoDB 连接字符串，支持副本集和分片集群</li>
 *   <li>database: 默认数据库</li>
 * </ul>
 * 
 * <p>连接字符串格式：</p>
 * <pre>
 * # 单节点
 * mongodb://localhost:27017/mydb
 * 
 * # 副本集
 * mongodb://host1:27017,host2:27017,host3:27017/mydb?replicaSet=rs0
 * 
 * # 分片集群
 * mongodb://mongos1:27017,mongos2:27017/mydb
 * 
 * # 带认证
 * mongodb://user:password@localhost:27017/mydb?authSource=admin
 * </pre>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Configuration
@EnableMongoRepositories(basePackages = "cn.itzixiao.interview.mongodb.repository")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/interview_db}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:interview_db}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        // 创建 MongoDB 客户端
        // MongoClient 是线程安全的，可以全局共享
        return MongoClients.create(mongoUri);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }
}
```

### 7.3 实体类定义

```java
package cn.itzixiao.interview.mongodb.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体类
 * 
 * <p>@Document 注解指定集合名称</p>
 * <p>@CompoundIndex 定义复合索引</p>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Data
@Document(collection = "users")
@CompoundIndexes({
    @CompoundIndex(name = "idx_name_email", def = "{'name': 1, 'email': 1}"),
    @CompoundIndex(name = "idx_status_created", def = "{'status': 1, 'createdAt': -1}")
})
public class User {

    /**
     * 主键 ID
     * MongoDB 自动生成 ObjectId
     */
    @Id
    private String id;

    /**
     * 用户名（单字段索引）
     */
    @Indexed(name = "idx_name")
    private String name;

    /**
     * 邮箱（唯一索引）
     */
    @Indexed(name = "idx_email", unique = true)
    private String email;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 用户状态
     */
    private UserStatus status;

    /**
     * 用户地址（嵌入式文档）
     */
    private Address address;

    /**
     * 用户标签（数组）
     */
    private List<String> tags;

    /**
     * 用户余额（高精度小数）
     */
    private BigDecimal balance;

    /**
     * 创建时间
     */
    @Indexed(name = "idx_created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        ACTIVE,      // 活跃
        INACTIVE,    // 未激活
        SUSPENDED    // 已封禁
    }

    /**
     * 地址嵌入式文档
     */
    @Data
    public static class Address {
        private String province;
        private String city;
        private String street;
        private String zipCode;
    }
}
```

### 7.4 Repository 接口

```java
package cn.itzixiao.interview.mongodb.repository;

import cn.itzixiao.interview.mongodb.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户 Repository 接口
 * 
 * <p>Spring Data MongoDB 自动实现基本 CRUD 操作</p>
 * <p>支持方法命名查询和 @Query 注解查询</p>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // ==================== 方法命名查询 ====================

    /**
     * 按名称查找
     * 等价于: { "name": ? }
     */
    Optional<User> findByName(String name);

    /**
     * 按邮箱查找
     */
    Optional<User> findByEmail(String email);

    /**
     * 按状态查找
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * 按年龄范围查找
     * 等价于: { "age": { $gt: min, $lt: max } }
     */
    List<User> findByAgeBetween(Integer min, Integer max);

    /**
     * 按标签查找（数组元素匹配）
     * 等价于: { "tags": tag }
     */
    List<User> findByTags(String tag);

    /**
     * 按状态和创建时间查找
     * 等价于: { "status": status, "createdAt": { $gt: since } }
     */
    List<User> findByStatusAndCreatedAtAfter(
        User.UserStatus status, 
        LocalDateTime since
    );

    /**
     * 分页查询
     */
    Page<User> findByStatus(User.UserStatus status, Pageable pageable);

    /**
     * 按名称模糊查询
     * 等价于: { "name": { $regex: name, $options: 'i' } }
     */
    List<User> findByNameContainingIgnoreCase(String name);

    /**
     * 统计某状态的用户数量
     */
    long countByStatus(User.UserStatus status);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    // ==================== @Query 注解查询 ====================

    /**
     * 自定义查询：按城市查找用户
     */
    @Query("{ 'address.city': ?0 }")
    List<User> findByCity(String city);

    /**
     * 投影查询：只返回指定字段
     */
    @Query(value = "{ 'status': ?0 }", fields = "{ 'name': 1, 'email': 1 }")
    List<User> findNameAndEmailByStatus(User.UserStatus status);

    /**
     * 复杂条件查询
     */
    @Query("{ $and: [ " +
           "  { 'age': { $gte: ?0 } }, " +
           "  { 'status': ?1 }, " +
           "  { 'tags': { $in: ?2 } } " +
           "] }")
    List<User> findComplex(Integer minAge, User.UserStatus status, List<String> tags);
}
```

### 7.5 聚合查询服务

```java
package cn.itzixiao.interview.mongodb.service;

import cn.itzixiao.interview.mongodb.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MongoDB 聚合查询服务
 * 
 * <p>展示 MongoDB 聚合管道的各种用法</p>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MongoAggregationService {

    private final MongoTemplate mongoTemplate;

    /**
     * 按状态分组统计用户数量
     * 
     * <p>SQL 等价：</p>
     * <pre>SELECT status, COUNT(*) as count FROM users GROUP BY status</pre>
     */
    public List<Map<String, Object>> countByStatus() {
        // 构建聚合管道
        Aggregation aggregation = Aggregation.newAggregation(
            // 阶段1：按状态分组，计算数量
            Aggregation.group("status")
                .count().as("count"),
            // 阶段2：投影输出字段
            Aggregation.project("count")
                .and("_id").as("status"),
            // 阶段3：按数量降序排序
            Aggregation.sort(Sort.Direction.DESC, "count")
        );

        // 执行聚合
        AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, 
            User.class, 
            Map.class
        );

        return results.getMappedResults();
    }

    /**
     * 按年龄段分组统计
     */
    public List<Map<String, Object>> countByAgeRange() {
        // 使用 $bucket 进行分桶聚合
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.bucket("age")
                .withBoundaries(0, 18, 30, 45, 60, 100)
                .withDefaultBucket("100+")
                .andOutputCount().as("count")
                .andOutput("name").push().as("names")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, User.class, Map.class
        );

        return results.getMappedResults();
    }

    /**
     * 多维度分析（使用 $facet）
     */
    public Map<String, Object> multiDimensionAnalysis() {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.facet()
                // 按状态统计
                .and(
                    Aggregation.group("status").count().as("count"),
                    Aggregation.project("count").and("_id").as("status")
                ).as("byStatus")
                // 按年龄段统计
                .and(
                    Aggregation.bucket("age")
                        .withBoundaries(0, 18, 30, 45, 60, 100)
                        .withDefaultBucket("other")
                        .andOutputCount().as("count")
                ).as("byAgeRange")
                // 总体统计
                .and(
                    Aggregation.group()
                        .count().as("totalUsers")
                        .avg("age").as("avgAge")
                        .max("balance").as("maxBalance")
                        .min("balance").as("minBalance")
                ).as("overall")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, User.class, Map.class
        );

        return results.getUniqueMappedResult();
    }

    /**
     * 查找每个城市最活跃的用户
     */
    public List<Map<String, Object>> findTopUserByCity() {
        Aggregation aggregation = Aggregation.newAggregation(
            // 阶段1：匹配有效用户
            Aggregation.match(
                Criteria.where("status").is(User.UserStatus.ACTIVE)
            ),
            // 阶段2：按城市分组，取余额最高的用户
            Aggregation.sort(Sort.Direction.DESC, "balance"),
            Aggregation.group("address.city")
                .first("$$ROOT").as("topUser"),
            // 阶段3：展开用户信息
            Aggregation.replaceRoot("topUser"),
            // 阶段4：投影输出
            Aggregation.project("name", "email", "balance")
                .and("address.city").as("city")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, User.class, Map.class
        );

        return results.getMappedResults();
    }

    /**
     * 全文搜索示例
     */
    public List<User> fullTextSearch(String keyword) {
        // 需要先创建文本索引
        // db.users.createIndex({ "name": "text", "email": "text" })
        
        TextCriteria criteria = TextCriteria.forDefaultLanguage()
            .matchingAny(keyword.split(" "));
        
        Query query = TextQuery.queryText(criteria)
            .sortByScore()
            .limit(10);
        
        return mongoTemplate.find(query, User.class);
    }

    /**
     * 批量更新
     */
    public void batchUpdateStatus(List<String> userIds, User.UserStatus newStatus) {
        Query query = Query.query(
            Criteria.where("_id").in(userIds)
        );
        
        Update update = new Update()
            .set("status", newStatus)
            .set("updatedAt", LocalDateTime.now());
        
        mongoTemplate.updateMulti(query, update, User.class);
    }

    /**
     * 原子更新（使用 $inc）
     */
    public boolean incrementBalance(String userId, double amount) {
        Query query = Query.query(
            Criteria.where("_id").is(userId)
        );
        
        Update update = new Update()
            .inc("balance", amount)
            .set("updatedAt", LocalDateTime.now());
        
        var result = mongoTemplate.updateFirst(query, update, User.class);
        return result.getModifiedCount() > 0;
    }
}
```

---

## 八、事务支持

### 8.1 多文档事务

MongoDB 4.0+ 支持多文档事务（副本集），4.2+ 支持分片集群事务：

```java
package cn.itzixiao.interview.mongodb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MongoDB 事务服务
 * 
 * <p>MongoDB 4.0+ 支持多文档事务</p>
 * <p>注意事项：</p>
 * <ul>
 *   <li>事务只能在副本集上使用（单节点不支持）</li>
 *   <li>事务会增加延迟，应尽量简短</li>
 *   <li>避免在事务中执行长时间操作</li>
 * </ul>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MongoTransactionService {

    private final MongoTemplate mongoTemplate;

    /**
     * 转账事务示例
     * 
     * <p>演示多文档事务的使用</p>
     * <p>注意：需要开启 @EnableTransactionManagement</p>
     */
    @Transactional
    public boolean transfer(String fromUserId, String toUserId, double amount) {
        log.info("开始转账事务: from={}, to={}, amount={}", 
            fromUserId, toUserId, amount);

        // 1. 扣减转出方余额
        Query fromQuery = Query.query(Criteria.where("_id").is(fromUserId));
        Update fromUpdate = new Update()
            .inc("balance", -amount)
            .set("updatedAt", java.time.LocalDateTime.now());
        
        var fromResult = mongoTemplate.updateFirst(fromQuery, fromUpdate, "users");
        if (fromResult.getModifiedCount() == 0) {
            throw new RuntimeException("转出方不存在或余额不足");
        }

        // 2. 增加转入方余额
        Query toQuery = Query.query(Criteria.where("_id").is(toUserId));
        Update toUpdate = new Update()
            .inc("balance", amount)
            .set("updatedAt", java.time.LocalDateTime.now());
        
        var toResult = mongoTemplate.updateFirst(toQuery, toUpdate, "users");
        if (toResult.getModifiedCount() == 0) {
            throw new RuntimeException("转入方不存在");
        }

        // 3. 记录转账日志
        var transferLog = new java.util.HashMap<String, Object>();
        transferLog.put("fromUserId", fromUserId);
        transferLog.put("toUserId", toUserId);
        transferLog.put("amount", amount);
        transferLog.put("createdAt", java.time.LocalDateTime.now());
        mongoTemplate.insert(transferLog, "transfer_logs");

        log.info("转账事务完成");
        return true;
    }

    /**
     * 编程式事务
     */
    public boolean transferWithProgrammatic(String fromUserId, String toUserId, 
            double amount) {
        
        return mongoTemplate.execute(session -> {
            try {
                // 在 session 中执行所有操作
                // 扣减
                Query fromQuery = Query.query(Criteria.where("_id").is(fromUserId));
                mongoTemplate.updateFirst(
                    fromQuery, 
                    Update.update("balance", -amount), 
                    "users"
                );
                
                // 增加
                Query toQuery = Query.query(Criteria.where("_id").is(toUserId));
                mongoTemplate.updateFirst(
                    toQuery, 
                    Update.update("balance", amount), 
                    "users"
                );
                
                return true;
            } catch (Exception e) {
                // 事务会自动回滚
                log.error("转账失败，事务回滚", e);
                throw e;
            }
        });
    }
}
```

---

## 九、高频面试题

**问题 1：MongoDB 和 MySQL 有什么区别？如何选择？**

**答：**

| 对比维度 | MongoDB | MySQL |
|----------|---------|-------|
| 数据模型 | 文档模型（BSON） | 关系模型（表） |
| Schema | 灵活，无固定结构 | 固定，需预定义 |
| 事务 | 4.0+ 支持（有限） | 完整 ACID |
| JOIN | $lookup（性能较低） | 高效 JOIN |
| 扩展性 | 原生支持分片 | 需要中间件 |
| 适用场景 | 非结构化数据、高写入 | 结构化数据、复杂查询 |

**选择建议：**
- **选 MongoDB**：内容管理、日志分析、物联网、实时分析、快速迭代的产品
- **选 MySQL**：金融交易、ERP/CRM、复杂业务逻辑、强一致性要求

**问题 2：MongoDB 的索引是如何工作的？有哪些类型？**

**答：**

**索引类型：**

1. **单字段索引**：对单个字段建立索引
2. **复合索引**：多个字段组合索引，遵循 ESR 原则
3. **多键索引**：对数组字段建立索引
4. **文本索引**：支持全文搜索
5. **地理空间索引**：支持地理位置查询（2d、2dsphere）
6. **哈希索引**：用于分片键，均匀分布数据
7. **通配符索引**：对未知字段建立索引

**索引优化：**
- 使用 `explain()` 分析查询计划
- 创建覆盖索引避免回表
- 避免创建过多索引（影响写入性能）
- 定期监控索引使用情况

**问题 3：MongoDB 如何实现高可用？副本集的工作原理是什么？**

**答：**

**副本集架构：**
1. **Primary（主节点）**：处理所有写操作
2. **Secondary（从节点）**：复制主节点数据，可处理读请求
3. **Arbiter（仲裁节点）**：只参与选举，不存储数据

**数据同步：**
- 主节点将写操作记录到 OpLog
- 从节点异步拉取 OpLog 并重放
- 支持全量同步和增量同步

**故障转移：**
1. 从节点检测到主节点心跳失败
2. 发起选举，获得多数票的从节点成为新主节点
3. 客户端自动重连到新主节点

**问题 4：什么是 MongoDB 的分片？如何选择分片键？**

**答：**

**分片概念：**
将数据分散存储在多个服务器上，实现水平扩展。

**分片组件：**
- **mongos**：路由服务器，转发请求
- **Config Server**：存储元数据和配置
- **Shard**：实际存储数据的分片

**分片键选择原则：**
1. **高基数**：字段值多样性高，避免热点
2. **均匀分布**：数据均匀分布到各分片
3. **查询友好**：分片键应能支持常见查询
4. **不可变**：分片键值不能修改

**分片键类型：**
- **范围分片**：适合范围查询，可能产生热点
- **哈希分片**：均匀分布，不支持范围查询

**问题 5：MongoDB 的聚合管道是什么？有哪些常用阶段？**

**答：**

**聚合管道**：将多个处理阶段串联，对文档进行转换和计算。

**常用阶段：**

| 阶段 | 功能 | SQL 对应 |
|------|------|---------|
| $match | 过滤文档 | WHERE |
| $group | 分组聚合 | GROUP BY |
| $sort | 排序 | ORDER BY |
| $project | 选择字段 | SELECT |
| $lookup | 左连接 | LEFT JOIN |
| $unwind | 展开数组 | - |
| $limit/$skip | 分页 | LIMIT/OFFSET |
| $facet | 并行聚合 | 子查询 |

**问题 6：MongoDB 如何保证数据一致性？**

**答：**

**写关注（Write Concern）：**

```javascript
// w: "majority" - 确保大多数节点写入成功
db.orders.insertOne(
  { ... },
  { writeConcern: { w: "majority", j: true, wtimeout: 5000 } }
);
```

**读关注（Read Concern）：**

- `local`：读取本地最新数据
- `available`：读取可用数据（可能回滚）
- `majority`：读取大多数节点确认的数据
- `linearizable`：线性一致性读

**因果一致性：**
- 使用 `afterClusterTime` 确保读操作在写操作之后

**问题 7：MongoDB 的文档大小限制是多少？如何处理大文件？**

**答：**

**文档大小限制：**
- 单个文档最大 **16MB**
- 嵌套层级最多 **100 层**

**处理大文件方案：**

1. **GridFS**：MongoDB 内置的大文件存储方案
   - 将文件分割为 255KB 的块
   - 存储在 fs.chunks 和 fs.files 集合

2. **引用外部存储**：
   - 文档中存储文件 URL
   - 实际文件存储在 OSS/S3

3. **数据拆分**：
   - 将大文档拆分为多个关联文档
   - 使用 $lookup 关联查询

---

## 十、最佳实践

### 10.1 文档设计建议

| 场景 | 推荐模式 | 原因 |
|------|---------|------|
| 一对少（1:N，N<100） | 嵌入式 | 单次查询获取所有数据 |
| 一对多（1:N，N>100） | 引用式 | 避免文档过大 |
| 一对极多（1:N，N>1000） | 引用式 + 索引 | 分页查询效率高 |
| 频繁更新 | 引用式 | 避免整体更新 |
| 读多写少 | 嵌入式 | 减少查询次数 |

### 10.2 性能优化清单

```
□ 索引优化
  ├─ 为常用查询字段创建索引
  ├─ 使用复合索引优化多字段查询
  ├─ 使用覆盖索引避免回表
  └─ 定期检查未使用的索引

□ 查询优化
  ├─ 使用 explain() 分析查询计划
  ├─ 限制返回字段（投影）
  ├─ 避免使用 $where 和正则表达式开头匹配
  └─ 使用批量操作减少网络往返

□ 写入优化
  ├─ 使用批量插入（bulkWrite）
  ├─ 合理设置写关注级别
  ├─ 避免频繁更新大文档
  └─ 使用 TTL 索引自动清理过期数据

□ 架构优化
  ├─ 合理设计分片键
  ├─ 使用副本集实现读写分离
  ├─ 配置合适的硬件资源
  └─ 监控关键指标（慢查询、连接数等）
```

---

**维护者：** itzixiao  
**最后更新：** 2026-03-21  
**问题反馈：** 欢迎提 Issue 或 PR
