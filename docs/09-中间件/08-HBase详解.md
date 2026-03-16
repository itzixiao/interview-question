# HBase 详解

## 📚 目录

1. [HBase 概述](#hbase-概述)
2. [数据模型](#数据模型)
3. [架构原理](#架构原理)
4. [核心操作](#核心操作)
5. [高级特性](#高级特性)
6. [性能优化](#性能优化)
7. [高频面试题](#高频面试题)

---

## HBase 概述

### 什么是 HBase？

**HBase**（Hadoop Database）是一个分布式、可扩展的 NoSQL 数据库，基于 Google BigTable 论文设计，运行在 HDFS 之上。

### 核心特点

| 特性       | 说明              |
|----------|-----------------|
| **面向列**  | 数据按列族存储，适合稀疏数据  |
| **高扩展性** | 可轻松扩展到数千台服务器    |
| **强一致性** | 单行读写具有强一致性      |
| **自动分区** | 数据自动按 Region 分布 |
| **多版本**  | 每个单元格可保存多个时间戳版本 |

### 适用场景

✅ **适合的场景：**

- 海量数据存储（TB/PB 级别）
- 高并发随机读写
- 半结构化或稀疏数据
- 历史数据查询（如订单历史、日志）

❌ **不适合的场景：**

- 复杂的事务处理
- 多表关联查询（Join）
- 实时统计分析
- 小量数据存储

### HBase vs MySQL

| 维度       | HBase | MySQL    |
|----------|-------|----------|
| **数据模型** | 面向列   | 关系型      |
| **扩展方式** | 水平扩展  | 垂直扩展     |
| **事务支持** | 单行事务  | 跨行事务     |
| **查询能力** | 简单查询  | 复杂 SQL   |
| **数据规模** | PB 级别 | GB/TB 级别 |
| **延迟**   | 毫秒级   | 毫秒 - 秒级  |

---

## 数据模型

### 逻辑视图

```
┌─────────────────────────────────────────────────────────┐
│ Row Key    │ Column Family: basic   │ education         │
├────────────┼────────────────────────┼───────────────────┤
│ user001    │ name:张三              │ university:北大   │
│            │ age:25                 │ degree:本科       │
│            │ email:zs@example.com   │                   │
├────────────┼────────────────────────┼───────────────────┤
│ user002    │ name:李四              │ university:清华   │
│            │ age:28                 │                   │
└────────────┴────────────────────────┴───────────────────┘
```

### 核心概念

#### 1. Table（表）

- HBase 中的表由行和列组成
- 表名通常是字符串（如 `user_info`）

#### 2. Row Key（行键）

- **定义**: 行的唯一标识符
- **特点**:
    - 按字典序排序
    - 设计决定查询模式
    - 长度可变（建议定长）
- **设计原则**:
  ```java
  // ✅ 好的设计：散列均匀
  userId + timestamp  // 避免热点
  
  // ❌ 坏的设计：容易热点
  timestamp           // 新数据都在同一 Region
  ```

#### 3. Column Family（列族）

- **定义**: 列的集合，是权限控制和存储的基本单位
- **特点**:
    - 创建表时定义
    - 物理存储在一起
    - 可独立设置属性（TTL、压缩等）
- **命名规范**: 通常使用小写字母（如 `basic`, `education`）

#### 4. Column Qualifier（列限定符）

- **定义**: 具体的列名
- **特点**:
    - 动态添加，无需预定义
    - 属于某个列族
    - 格式：`family:qualifier`

#### 5. Cell（单元格）

- **定义**: 行和列的交点
- **组成**: `(RowKey, Family, Qualifier, Timestamp, Value)`

#### 6. Time Stamp（时间戳）

- **作用**: 标识数据版本
- **默认**: 系统自动生成（long 类型）
- **版本控制**: 可配置保留的版本数

---

## 架构原理

### 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    Client                                │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                   ZooKeeper                              │
│  - Meta 表位置                                          │
│  - RegionServer 状态                                    │
│  - DDL 操作协调                                          │
└─────────────────────────────────────────────────────────┘
                          │
          ┌───────────────┴───────────────┐
          ▼                               ▼
┌─────────────────┐             ┌─────────────────┐
│  RegionServer1  │             │  RegionServer2  │
│  ┌───────────┐  │             │  ┌───────────┐  │
│  │ Region A  │  │             │  │ Region C  │  │
│  │ Region B  │  │             │  │ Region D  │  │
│  └───────────┘  │             │  └───────────┘  │
│  ┌───────────┐  │             │  ┌───────────┐  │
│  │  MemStore │  │             │  │  MemStore │  │
│  │  WAL      │  │             │  │  WAL      │  │
│  └───────────┘  │             │  └───────────┘  │
└─────────────────┘             └─────────────────┘
          │                               │
          ▼                               ▼
┌─────────────────────────────────────────────────────────┐
│                      HDFS                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │ StoreFile│  │ StoreFile│  │ StoreFile│              │
│  └──────────┘  └──────────┘  └──────────┘              │
└─────────────────────────────────────────────────────────┘
```

### 核心组件

#### 1. HMaster

- **职责**:
    - 管理元数据（表结构、Region 分配）
    - 负载均衡
    - DDL 操作（建表、删表）
- **特点**: 可部署多个，通过 ZooKeeper 选举主节点

#### 2. RegionServer

- **职责**:
    - 处理数据读写请求
    - 管理 Region
    - 刷新 MemStore 到磁盘
- **组成**:
    - **WAL (Write-Ahead Log)**: 预写日志，数据恢复用
    - **MemStore**: 内存缓存，写缓冲
    - **BlockCache**: 读缓存

#### 3. Region

- **定义**: 表的水平分片
- **组成**: 一个或多个 Store（对应列族）
- **分裂**: Region 过大时自动分裂

#### 4. Store

- **定义**: 每个列族对应一个 Store
- **组成**:
    - MemStore（内存）
    - StoreFiles（HFile，磁盘）

### 写入流程

```
1. Client → RegionServer
2. 写入 WAL（追加日志）
3. 写入 MemStore（内存）
4. 返回成功
5. MemStore 满 → Flush 到磁盘（StoreFile）
```

### 读取流程

```
1. Client → ZooKeeper 查找 Meta 表位置
2. 访问 Meta 表找到 Region 位置
3. 访问对应 RegionServer
4. 先查 BlockCache，再查 MemStore，最后查 StoreFile
5. 合并结果返回
```

---

## 核心操作

### Java API 示例

#### 1. 创建表

```java
public void createTable(String tableName, String... columnFamilies) {
    Admin admin = connection.getAdmin();
    
    if (admin.tableExists(TableName.valueOf(tableName))) {
        System.out.println("表已存在");
        return;
    }
    
    TableDescriptorBuilder tableDescriptor = 
        TableDescriptorBuilder.newBuilder(TableName.valueOf(tableName));
    
    for (String family : columnFamilies) {
        ColumnFamilyDescriptorBuilder familyDescriptor = 
            ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(family));
        familyDescriptor.setMaxVersions(3);  // 最多 3 个版本
        tableDescriptor.setColumnFamily(familyDescriptor.build());
    }
    
    admin.createTable(tableDescriptor.build());
}
```

#### 2. 插入数据

```java
// 单条插入
Put put = new Put(Bytes.toBytes(rowKey));
put.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes.toBytes(value));
table.put(put);

// 批量插入
List<Put> puts = new ArrayList<>();
// ... 添加多个 Put
table.put(puts);
```

#### 3. 查询数据

```java
// 单行查询
Get get = new Get(Bytes.toBytes(rowKey));
Result result = table.get(get);

// 指定列查询
get.addColumn(Bytes.toBytes("basic"), Bytes.toBytes("name"));

// 多版本查询
get.setMaxVersions(3);
```

#### 4. 扫描数据

```java
// 全表扫描
Scan scan = new Scan();
ResultScanner scanner = table.getScanner(scan);

// 范围扫描
scan.setStartRow(Bytes.toBytes("user001"));
scan.setStopRow(Bytes.toBytes("user999"));

// 带过滤器扫描
SingleColumnValueFilter filter = new SingleColumnValueFilter(
    Bytes.toBytes("basic"),
    Bytes.toBytes("age"),
    CompareOperator.GREATER,
    Bytes.toBytes(25)
);
scan.setFilter(filter);
```

#### 5. 删除数据

```java
// 删除行
Delete delete = new Delete(Bytes.toBytes(rowKey));
table.delete(delete);

// 删除列
delete.addColumn(Bytes.toBytes("basic"), Bytes.toBytes("age"));

// 删除表
admin.disableTable(tableName);  // 先禁用
admin.deleteTable(tableName);   // 再删除
```

---

## 高级特性

### 1. 过滤器（Filter）

#### 常用过滤器

| 过滤器                         | 作用   | 示例                          |
|-----------------------------|------|-----------------------------|
| **SingleColumnValueFilter** | 列值过滤 | `age > 25`                  |
| **RowFilter**               | 行键过滤 | `rowkey LIKE 'user%'`       |
| **PrefixFilter**            | 前缀过滤 | `rowkey STARTS WITH 'user'` |
| **PageFilter**              | 分页   | `LIMIT 100`                 |
| **ColumnPaginationFilter**  | 列分页  | 每页 10 列                     |

#### 使用示例

```java
// 组合过滤器（AND）
FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
filterList.addFilter(new RowFilter(CompareOperator.EQUAL, 
    new BinaryPrefixComparator(Bytes.toBytes("user"))));
filterList.addFilter(new SingleColumnValueFilter(
    Bytes.toBytes("basic"),
    Bytes.toBytes("age"),
    CompareOperator.GREATER,
    Bytes.toBytes(25)
));

scan.setFilter(filterList);
```

### 2. Coprocessor（协处理器）

类似数据库的触发器和存储过程

```java
// 示例：全局计数器
public class CounterEndpoint extends BaseEndpoint 
    implements CounterProtocol {
    
    @Override
    public long increment(byte[] family, byte[] qualifier, long amount) {
        // 原子递增逻辑
    }
}
```

### 3. Bloom Filter（布隆过滤器）

- **作用**: 快速判断数据是否存在
- **优势**: 减少磁盘 IO
- **配置**:
  ```java
  familyDescriptor.setBloomFilterType(BloomType.ROW);
  ```

### 4. TTL（Time To Live）

```java
// 设置数据生存周期
familyDescriptor.setTimeToLive(2592000);  // 30 天（秒）
```

---

## 性能优化

### 1. Row Key 设计优化

#### 热点问题解决方案

**方案一：加盐（Salting）**

```java
// 在 rowkey 前添加随机前缀
int salt = Math.abs(rowkey.hashCode()) % 10;
String saltedRowkey = salt + "_" + rowkey;
```

**方案二：哈希**

```java
// 对原始 key 哈希
String hashedKey = MD5(originalKey).substring(0, 8) + originalKey;
```

**方案三：反转**

```java
// 反转手机号等
String reversedPhone = new StringBuilder(phone).reverse().toString();
```

### 2. 预分区

```java
// 创建预分区表
byte[][] splits = new byte[4][];
splits[0] = Bytes.toBytes("1000");
splits[1] = Bytes.toBytes("5000");
splits[2] = Bytes.toBytes("9000");

admin.createTable(tableDescriptor, splits);
```

### 3. 写入优化

| 优化项        | 配置                          | 说明       |
|------------|-----------------------------|----------|
| **关闭 WAL** | `write.setWal(false)`       | 批量导入时使用  |
| **增加批次**   | `setAutoFlush(false)`       | 累积一定量再提交 |
| **调整参数**   | `hbase.client.write.buffer` | 写缓冲区大小   |

### 4. 读取优化

| 优化项              | 配置                       | 说明     |
|------------------|--------------------------|--------|
| **BlockCache**   | `hfile.block.cache.size` | 读缓存比例  |
| **Bloom Filter** | `ROW` / `ROWCOL`         | 减少磁盘查找 |
| **压缩**           | `GZIP` / `SNAPPY`        | 减少存储空间 |

### 5. Compaction 优化

```properties
# Minor Compaction
hbase.hstore.compaction.threshold=3  # 文件数阈值
hbase.hstore.compaction.max.size=1GB  # 最大文件大小

# Major Compaction（手动触发）
major_compact 'table_name'
```

---

## 💡 高频面试题

### 问题 1：HBase 的 Row Key 设计原则？

**答案：**

**设计原则：**

1. **唯一性原则**
    - Row Key 必须唯一标识一行
    - 类似关系数据库的主键

2. **散列均匀原则**（最重要）
    - 避免数据集中在某些 Region
    - 防止热点问题

3. **长度适中原则**
    - 建议 64 字节以内
    - 过短影响查询，过长浪费存储

4. **业务相关原则**
    - 结合查询模式设计
    - 常用查询条件放前面

**设计方案：**

```java
// 1. 时间序列数据 - 反转时间戳
String rowkey = Long.MAX_VALUE - timestamp + "_" + userId;

// 2. 用户数据 - 哈希散列
String rowkey = MD5(userId).substring(0, 8) + userId;

// 3. 订单数据 - 加盐
int salt = orderId % 10;
String rowkey = salt + "_" + orderId;

// 4. 手机号数据 - 反转
String rowkey = new StringBuilder(phone).reverse().toString();
```

---

### 问题 2：HBase 的写入流程是什么？

**答案：**

**完整流程：**

```
1. Client 获取 Meta 表位置（从 ZooKeeper）
2. Client 找到对应 RegionServer
3. RegionServer 执行写入：
   a. 写入 WAL（Write-Ahead Log）
      - 顺序追加，保证数据安全
   b. 写入 MemStore（内存）
      - 无序存储，快速写入
   c. 返回成功给 Client
4. 异步过程：
   - MemStore 满（默认 128MB）→ Flush
   - 生成 StoreFile（HFile）
   - 多个 StoreFile → Minor Compaction
   - 定期 → Major Compaction
```

**关键特点：**

- **追加写**: 比随机写快
- **批量写**: 多次写入合并为一次磁盘 IO
- **异步刷盘**: 不影响写入性能

---

### 问题 3：HBase 的读取流程是什么？

**答案：**

**完整流程：**

```
1. Client 访问 ZooKeeper
   - 获取 hbase:meta 表位置
   
2. 访问 Meta 表
   - 根据 Row Key 找到对应 Region
   - 获取 Region 所在 RegionServer
   
3. 访问 RegionServer
   - 读取 BlockCache（读缓存）
   - 读取 MemStore（内存写缓冲）
   - 读取 StoreFile（磁盘文件）
   
4. 合并结果
   - 按时间戳排序
   - 返回指定版本数据
```

**性能优化点：**

- **BlockCache**: 热点数据缓存
- **Bloom Filter**: 快速排除不存在的数据
- **并行读取**: 多个 StoreFile 并行扫描

---

### 问题 4：HBase 如何处理数据更新和删除？

**答案：**

**更新机制：**

- **逻辑**: 不是原地更新，而是插入新版本
- **过程**:
    1. 写入新的 Cell（相同 RowKey + Column，不同 Timestamp）
    2. 读取时返回最新版本
    3. 旧版本在 Compaction 时清理

**删除机制：**

- **逻辑**: 标记删除，不是立即删除
- **过程**:
    1. 写入 DELETE Marker（特殊的时间戳）
    2. 读取时遇到 DELETE Marker 停止
    3. Major Compaction 时物理删除

**示例：**

```
时间线：
T1: 写入 (name=张三@1000)
T2: 更新 (name=张三@2000)
T3: 删除 (DELETE_MARKER@3000)

读取结果：
- T1-T2: 读到 name=张三@1000
- T2-T3: 读到 name=张三@2000
- T3 后：读不到数据（遇到 DELETE_MARKER）
- Major Compaction 后：所有版本物理删除
```

---

### 问题 5：HBase 的 Compaction 机制？

**答案：**

**Compaction 类型：**

| 类型        | 触发条件             | 作用     | 特点         |
|-----------|------------------|--------|------------|
| **Minor** | StoreFile 数量达到阈值 | 合并小文件  | 不处理过期/删除数据 |
| **Major** | 手动触发或定时          | 合并所有文件 | 清理过期/删除数据  |

**Minor Compaction:**

```
触发条件：
- hbase.hstore.compaction.threshold=3（默认）
- StoreFile 数量 >= 3

过程：
1. 选择要合并的文件（小的优先）
2. 合并为一个大文件
3. 删除原文件

优点：减少文件数量，提高读取性能
缺点：不清理垃圾数据
```

**Major Compaction:**

```
触发方式：
- 手动：major_compact 'table_name'
- 自动：hbase.majorcompaction.interval（默认 7 天）

过程：
1. 合并所有 StoreFile
2. 清理过期数据（超过 TTL）
3. 清理被删除数据（DELETE_MARKER）
4. 清理多余版本

优点：释放存储空间
缺点：IO 开销大，影响性能
```

---

### 问题 6：HBase 如何保证高可用？

**答案：**

**高可用机制：**

1. **HMaster HA**
    - 部署多个 HMaster
    - 通过 ZooKeeper 选举
    - 主节点故障，备用自动接管

2. **RegionServer HA**
    - Region 自动重新分配
    - WAL 用于数据恢复
    - 监控心跳，快速故障检测

3. **数据冗余**
    - HDFS 多副本（默认 3 副本）
    - 数据块分布在多个节点
    - 机架感知策略

4. **WAL 机制**
    - 写入前先写日志
    - RegionServer 故障后回放 WAL
    - 保证数据不丢失

**故障恢复流程：**

```
RegionServer 宕机：
1. ZooKeeper 检测到节点消失
2. HMaster 将故障节点的 Region 标记为待分配
3. 其他 RegionServer 接管这些 Region
4. 回放 WAL 恢复数据
5. 服务恢复正常
```

---

### 问题 7：HBase 与 Hive 的区别？

**答案：**

| 维度       | HBase          | Hive          |
|----------|----------------|---------------|
| **定位**   | NoSQL 数据库      | 数据仓库工具        |
| **查询**   | 简单查询（Get/Scan） | SQL 查询（类 SQL） |
| **延迟**   | 毫秒级            | 分钟级           |
| **适用场景** | 实时查询           | 离线分析          |
| **底层存储** | HDFS           | HDFS          |
| **更新**   | 支持             | 不支持（仅追加）      |
| **索引**   | Row Key        | 分区/桶          |

**典型用法：**

- **HBase**: 查询用户最新订单（实时）
- **Hive**: 统计过去一年销售趋势（离线）

**联合使用：**

```sql
-- Hive 创建 HBase 外部表
CREATE EXTERNAL TABLE hbase_table(
  key STRING,
  name STRING
)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES ("hbase.columns.mapping" = ":key,basic:name")
TBLPROPERTIES ("hbase.table.name" = "user_info");

-- Hive 分析 HBase 数据
SELECT COUNT(*) FROM hbase_table WHERE name LIKE '张%';
```

---

## 🔗 跨模块关联

### 前置知识

- ✅ **[Java基础](../01-Java基础/README.md)** - IO、集合框架
- ✅ **[MySQL](../07-MySQL数据库/README.md)** - 数据库基础
- ✅ **[Hadoop](../09-中间件/README.md)** - HDFS 原理

### 后续应用

- 📚 **[Spark](../09-中间件/README.md)** - Spark 读写 HBase
- 📚 **[Phoenix](../09-中间件/README.md)** - SQL on HBase
- 📚 **[实时计算](../12-分布式系统/README.md)** - 实时数据入库

---

## 📖 学习建议

### 第一阶段：理解概念（2-3 天）

1. HBase 数据模型
2. 架构组件
3. 读写流程

### 第二阶段：动手实践（3-4 天）

1. 搭建单机环境
2. Shell 命令练习
3. Java API 编程

### 第三阶段：深入原理（3-4 天）

1. LSM 树原理
2. Compaction 机制
3. 性能调优

### 第四阶段：面试冲刺（1-2 天）

1. 熟记 7 道面试题
2. 理解答题要点
3. 能够手写代码

---

## 📈 更新日志

### v1.0 - 2026-03-15

- ✅ 新增 HBase 详解文档
- ✅ 补充 7 道高频面试题
- ✅ 添加完整代码示例
- ✅ 配套代码在 `interview-provider/hbase/`

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15  
**问题反馈：** 欢迎提 Issue 或 PR
