# Elasticsearch 进阶详解

> 📚 本文档深入讲解 Elasticsearch 底层原理与高级应用，包括倒排索引、分词器、DSL 查询、聚合分析、性能优化等核心知识点。

## 目录

- [一、倒排索引原理](#一倒排索引原理)
- [二、分词器工作机制](#二分词器工作机制)
- [三、查询 DSL 高级用法](#三查询-dsl-高级用法)
- [四、聚合分析实战](#四聚合分析实战)
- [五、性能优化](#五性能优化)
- [六、集群管理与扩容](#六集群管理与扩容)
- [七、ELK日志分析实战](#七ELK日志分析实战)

---

## 一、倒排索引原理

### 1.1 什么是倒排索引？

**倒排索引（Inverted Index）**是 Elasticsearch 的核心数据结构，用于实现高效的全文检索。

**正排索引 vs 倒排索引：**

| 类型 | 结构 | 特点 | 适用场景 |
|------|------|------|----------|
| 正排索引 | 文档 ID → 内容 | 适合根据 ID 查内容 | 关系型数据库主键索引 |
| 倒排索引 | 单词 → 文档 ID 列表 | 适合根据内容找文档 | 搜索引擎全文检索 |

### 1.2 倒排索引结构

```
倒排索引 = Term Dictionary（词典） + Posting List（倒排表）

Term Dictionary: 按字母顺序排列的所有词项
Posting List: 包含该词项的文档 ID 列表
```

**示例：**

```
文档 1: Elasticsearch 是一个分布式搜索引擎
文档 2: Elasticsearch 支持全文检索
文档 3: Hadoop 是大数据处理框架

倒排索引：
Elasticsearch → [文档 1, 文档 2]
分布式     → [文档 1]
搜索引擎    → [文档 1]
支持       → [文档 2]
全文检索    → [文档 2]
Hadoop     → [文档 3]
大数据      → [文档 3]
处理       → [文档 3]
框架       → [文档 3]
```

### 1.3 FST（Finite State Transducers）

Elasticsearch 使用 **FST（有限状态转换器）** 来压缩存储 Term Dictionary，实现：
- ✅ 极高的空间效率
- ✅ 快速的查找性能
- ✅ 支持前缀搜索

### 1.4 代码示例：查看索引结构

```java
// 参见：ElasticsearchAdvancedController.java
// 接口：GET /api/es/advanced/inverted-index-demo
```

---

## 二、分词器工作机制

### 2.1 分词器组成

分词器（Analyzer）由三部分组成：

1. **Character Filters（字符过滤器）**
   - 处理原始文本（如去除 HTML 标签、转换字符）
   
2. **Tokenizer（分词器）**
   - 将文本拆分为词项
   
3. **Token Filters（词项过滤器）**
   - 对词项进行处理（如转小写、去除停用词、同义词）

### 2.2 内置分词器

| 分词器 | 说明 | 适用场景 |
|--------|------|----------|
| standard | 默认分词器，按词切分 | 英文 |
| ik_smart | IK 分词器，最少切分 | 中文粗粒度 |
| ik_max_word | IK 分词器，最细粒度 | 中文细粒度 |
| whitespace | 按空格切分 | 特殊格式 |
| keyword | 不分词 | 精确值 |

### 2.3 代码示例：测试分词效果

```java
// 参见：ElasticsearchAdvancedController.java
// 接口：POST /api/es/advanced/analyze
```

### 2.4 自定义分词器

```json
PUT /my_index
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_custom_analyzer": {
          "type": "custom",
          "tokenizer": "ik_max_word",
          "filter": ["lowercase", "stop"]
        }
      }
    }
  }
}
```

---

## 三、查询 DSL 高级用法

### 3.1 Query Context vs Filter Context

| 特性 | Query Context | Filter Context |
|------|---------------|----------------|
| 相关性评分 | ✅ 计算 | ❌ 不计算 |
| 缓存 | ❌ 不缓存 | ✅ 缓存 |
| 性能 | 较慢 | 更快 |
| 适用场景 | 全文检索 | 条件过滤 |

### 3.2 常用查询类型

#### 3.2.1 Match Query（全文检索）

```json
GET /logs/_search
{
  "query": {
    "match": {
      "message": {
        "query": "Elasticsearch 分布式",
        "operator": "and"
      }
    }
  }
}
```

#### 3.2.2 Bool Query（复合查询）

```json
GET /logs/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "message": "error" }}
      ],
      "filter": [
        { "term": { "level": "ERROR" }},
        { "range": { "timestamp": { "gte": "now-1h" }}}
      ],
      "should": [
        { "match": { "tags": "critical" }}
      ],
      "must_not": [
        { "term": { "env": "test" }}
      ]
    }
  }
}
```

#### 3.2.3 Multi Match Query（多字段查询）

```json
GET /logs/_search
{
  "query": {
    "multi_match": {
      "query": "Elasticsearch 性能优化",
      "fields": ["title^2", "content", "tags"],
      "type": "best_fields"
    }
  }
}
```

#### 3.2.4 Function Score Query（自定义评分）

```json
GET /logs/_search
{
  "query": {
    "function_score": {
      "query": { "match": { "message": "error" }},
      "functions": [
        {
          "filter": { "range": { "timestamp": { "gte": "now-1h" }}},
          "weight": 2
        },
        {
          "field_value_factor": {
            "field": "view_count",
            "factor": 1.2,
            "modifier": "log1p"
          }
        }
      ],
      "score_mode": "sum",
      "boost_mode": "multiply"
    }
  }
}
```

### 3.3 代码示例：高级查询

```java
// 参见：ElasticsearchAdvancedController.java
// 接口：
// - POST /api/es/advanced/search/bool-query
// - POST /api/es/advanced/search/multi-match
// - POST /api/es/advanced/search/function-score
```

---

## 四、聚合分析实战

### 4.1 聚合类型

| 类型 | 说明 | 常见操作 |
|------|------|----------|
| Bucket Aggregation | 分组聚合 | term, range, date_histogram |
| Metric Aggregation | 指标聚合 | avg, sum, min, max, cardinality |
| Pipeline Aggregation | 管道聚合 | derivative, moving_avg |

### 4.2 桶聚合示例

#### 4.2.1 Terms Aggregation（按字段分组）

```json
GET /logs/_search
{
  "size": 0,
  "aggs": {
    "by_level": {
      "terms": {
        "field": "level.keyword"
      }
    }
  }
}
```

#### 4.2.2 Date Histogram（时间直方图）

```json
GET /logs/_search
{
  "size": 0,
  "aggs": {
    "logs_per_hour": {
      "date_histogram": {
        "field": "timestamp",
        "calendar_interval": "hour"
      }
    }
  }
}
```

### 4.3 指标聚合示例

```json
GET /logs/_search
{
  "size": 0,
  "aggs": {
    "response_time_stats": {
      "extended_stats": {
        "field": "response_time"
      }
    },
    "unique_users": {
      "cardinality": {
        "field": "user_id"
      }
    }
  }
}
```

### 4.4 嵌套聚合示例

```json
GET /logs/_search
{
  "size": 0,
  "aggs": {
    "by_level": {
      "terms": { "field": "level.keyword" },
      "aggs": {
        "avg_response_time": {
          "avg": { "field": "response_time" }
        },
        "logs_over_time": {
          "date_histogram": {
            "field": "timestamp",
            "calendar_interval": "hour"
          }
        }
      }
    }
  }
}
```

### 4.5 代码示例：聚合分析

```java
// 参见：ElasticsearchAdvancedController.java
// 接口：
// - GET /api/es/advanced/aggregation/terms
// - GET /api/es/advanced/aggregation/histogram
// - GET /api/es/advanced/aggregation/nested
```

---

## 五、性能优化

### 5.1 写入性能优化

#### 5.1.1 批量写入

```java
// 使用 BulkRequest 批量写入
BulkRequest bulkRequest = new BulkRequest();
for (int i = 0; i < 1000; i++) {
    IndexRequest indexRequest = new IndexRequest("logs");
    indexRequest.source(jsonBuilder);
    bulkRequest.add(indexRequest);
}
BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
```

#### 5.1.2 调整刷新间隔

```json
PUT /logs/_settings
{
  "refresh_interval": "30s"  // 默认 1s，增大可减少 I/O
}
```

#### 5.1.3 使用副本策略

```json
PUT /logs/_settings
{
  "number_of_replicas": 0  // 批量导入时关闭副本，完成后开启
}
```

#### 5.1.4 增加批处理大小

```json
PUT /_cluster/settings
{
  "persistent": {
    "indices.memory.index_buffer_size": "20%"  // 默认 10%
  }
}
```

### 5.2 查询性能优化

#### 5.2.1 使用 Filter Context

```json
// ✅ 推荐：使用 filter（可缓存）
{
  "query": {
    "bool": {
      "filter": { "term": { "status": "active" }}
    }
  }
}

// ❌ 不推荐：使用 query（不可缓存）
{
  "query": {
    "bool": {
      "must": { "term": { "status": "active" }}
    }
  }
}
```

#### 5.2.2 限制返回字段

```json
GET /logs/_search
{
  "_source": ["title", "timestamp"],
  "query": { "match": { "message": "error" }}
}
```

#### 5.2.3 使用路由优化

```json
// 指定路由值，将查询定位到特定分片
GET /logs/_search?routing=user_123
{
  "query": {
    "term": { "user_id": "user_123" }
  }
}
```

#### 5.2.4 避免深度分页

```json
// ❌ 不推荐：深度分页性能差
{
  "from": 10000,
  "size": 10
}

// ✅ 推荐：使用 search_after
{
  "size": 10,
  "query": { "match_all": {}},
  "sort": [{ "timestamp": "desc" }],
  "search_after": [1609459200000]
}
```

### 5.3 索引设计优化

#### 5.3.1 合理设置分片数

```json
PUT /logs
{
  "settings": {
    "number_of_shards": 3,   // 根据数据量预估
    "number_of_replicas": 1
  }
}
```

**分片大小建议：**
- 单个分片大小：10GB ~ 50GB
- 分片数量 = 数据总量 / 40GB

#### 5.3.2 使用 ILM（索引生命周期管理）

```json
PUT /_ilm/policy/logs_policy
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "7d"
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": { "number_of_shards": 1 }
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

### 5.4 代码示例：性能优化

```java
// 参见：ElasticsearchAdvancedController.java
// 接口：
// - POST /api/es/advanced/optimize/bulk-write
// - GET /api/es/advanced/optimize/query-cache
// - POST /api/es/advanced/optimize/index-settings
```

---

## 六、集群管理与扩容

### 6.1 集群健康状态

```bash
# 查看集群健康状态
GET /_cluster/health

# 查看详细状态
GET /_cluster/health?pretty&wait_for_status=yellow&timeout=60s
```

**状态说明：**
- 🟢 **green**：所有主分片和副本分片正常
- 🟡 **yellow**：所有主分片正常，部分副本分片异常
- 🔴 **red**：有主分片不可用

### 6.2 节点管理

```bash
# 查看所有节点信息
GET /_cat/nodes?v&h=name,ip,port,node.role,master,ram,heap

# 查看分片分布
GET /_cat/shards?v&h=index,shard,prirep,node,docs,store

# 查看索引信息
GET /_cat/indices?v&h=index,health,status,pri,rep,docs.count,store.size
```

### 6.3 动态扩容

#### 6.3.1 添加新节点

```yaml
# elasticsearch.yml
cluster.name: my-cluster
node.name: node-4
node.roles: [ data, ingest ]  # 数据节点 + 预处理节点
network.host: 192.168.1.104
discovery.seed_hosts: 
  - 192.168.1.101
  - 192.168.1.102
  - 192.168.1.103
```

#### 6.3.2 分片重新平衡

```json
# 手动迁移分片
POST /_cluster/reroute
{
  "commands": [
    {
      "move": {
        "index": "logs",
        "shard": 0,
        "from_node": "node-1",
        "to_node": "node-4"
      }
    }
  ]
}

# 启用分片分配
PUT /_cluster/settings
{
  "transient": {
    "cluster.routing.allocation.enable": "all"
  }
}
```

### 6.4 分片策略

#### 6.4.1 基于时间的 rollover

```json
PUT /logs-write/_alias/logs-read
{
  "aliases": {
    "logs-read": {
      "is_write_index": true
    }
  }
}

POST /logs-write/_rollover
{
  "conditions": {
    "max_age": "7d",
    "max_docs": 10000000,
    "max_size": "50gb"
  }
}
```

#### 6.4.2 冷热架构

```json
// 热节点（Hot Node）- SSD，处理最近 7 天数据
// 温节点（Warm Node）- HDD，处理 7-30 天数据
// 冷节点（Cold Node）- 大容量 HDD，处理历史数据

PUT /_template/logs_template
{
  "index_patterns": ["logs-*"],
  "settings": {
    "index.routing.allocation.require": {
      "data": "hot"  // 新索引分配到热节点
    }
  }
}
```

### 6.5 代码示例：集群管理

```java
// 参见：ElasticsearchAdvancedController.java
// 接口：
// - GET /api/es/advanced/cluster/health
// - GET /api/es/advanced/cluster/nodes
// - GET /api/es/advanced/cluster/shards
```

---

## 七、ELK日志分析实战

### 7.1 ELK 技术栈

- **E**lasticsearch：存储和检索日志
- **L**ogstash：收集、过滤、转换日志
- **K**ibana：可视化展示

### 7.2 Filebeat 采集配置

```yaml
# filebeat.yml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/application/*.log
    fields:
      app: my-application
      env: production
    multiline.pattern: '^\d{4}-\d{2}-\d{2}'
    multiline.negate: true
    multiline.match: after

processors:
  - add_host_metadata: ~
  - add_cloud_metadata: ~

output.elasticsearch:
  hosts: ["http://localhost:19200"]
  username: "elastic"
  password: "changeme"
  indices:
    - index: "filebeat-myapp-%{+yyyy.MM.dd}"
      when.equals:
        fields.app: "my-application"

setup.kibana:
  host: "http://localhost:5601"

setup.template.settings:
  index.number_of_shards: 3
  index.number_of_replicas: 1
```

### 7.3 Logstash 管道配置

```ruby
# logstash.conf
input {
  beats {
    port => 5044
  }
  
  kafka {
    bootstrap_servers => "kafka:9092"
    topics => ["app-logs"]
    group_id => "logstash"
    consumer_threads => 3
  }
}

filter {
  grok {
    match => { 
      "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} \[%{DATA:thread}\] %{DATA:class} - %{GREEDYDATA:log_message}"
    }
  }
  
  date {
    match => [ "timestamp", "ISO8601" ]
    target => "@timestamp"
  }
  
  if [level] == "ERROR" {
    mutate {
      add_field => { "alert" => "true" }
    }
  }
  
  drop {
    when => { [level] == "DEBUG" }
  }
}

output {
  elasticsearch {
    hosts => ["http://localhost:19200"]
    index => "logs-%{+yyyy.MM.dd}"
    template_name => "logs"
  }
  
  kafka {
    bootstrap_servers => "kafka:9092"
    topic_id => "alert-logs"
    when => "[alert] == 'true'"
  }
}
```

### 7.4 Kibana 可视化

#### 7.4.1 创建索引模式

```
Management → Stack Management → Index Patterns
创建：logs-*
时间字段：@timestamp
```

#### 7.4.2 创建 Dashboard

1. **错误日志趋势图**
   - Visualization → Create new → Line
   - 指标：Count
   - X 轴：@timestamp（日期直方图）
   - 过滤器：level: ERROR

2. **日志级别分布饼图**
   - Visualization → Pie
   - 切片：level.keyword
   - 指标：Count

3. **Top 10 错误类来源**
   - Visualization → Data Table
   - 行：class.keyword
   - 指标：Count
   - 排序：Count 降序
   - 过滤器：level: ERROR

### 7.5 日志查询示例

```kql
// KQL（Kibana Query Language）

// 查询最近 1 小时的 ERROR 日志
level: ERROR and @timestamp > now-1h

// 查询特定类的日志
class: "com.example.service.UserService"

// 查询包含特定消息的日志
log_message: "NullPointerException"

// 组合查询
level: ERROR and app: my-application and @timestamp > now-24h
```

### 7.6 告警配置

```json
// Watcher 告警规则
PUT _watcher/watch/error_rate_alert
{
  "trigger": {
    "schedule": {
      "interval": "5m"
    }
  },
  "input": {
    "search": {
      "request": {
        "indices": ["logs-*"],
        "body": {
          "size": 0,
          "query": {
            "bool": {
              "must": [{ "match": { "level": "ERROR" }}],
              "filter": [{ "range": { "@timestamp": { "gte": "now-5m" }}}]
            }
          },
          "aggs": {
            "error_count": {
              "value_count": { "field": "_id" }
            }
          }
        }
      }
    }
  },
  "condition": {
    "compare": {
      "ctx.payload.aggregations.error_count.value": {
        "gt": 100
      }
    }
  },
  "actions": {
    "send_email": {
      "email": {
        "to": ["admin@example.com"],
        "subject": "【告警】错误日志数量超过阈值",
        "body": "过去 5 分钟内错误日志数量：{{ctx.payload.aggregations.error_count.value}}"
      }
    }
  }
}
```

### 7.7 代码示例：ELK 集成

```java
// 参见：ElasticsearchAdvancedController.java
// 接口：
// - POST /api/es/advanced/elk/ingest-log
// - GET /api/es/advanced/elk/query
```

---

## 八、实战演练

### 8.1 完整示例：设备操作日志检索系统

参考现有代码：
- Entity: `DeviceOperationLogES.java`
- Repository: `DeviceOperationLogESRepository.java`
- Service: `DeviceOperationLogESService.java`
- Controller: `DeviceOperationLogESController.java`

### 8.2 新增进阶功能

以下高级功能将在后续版本中补充：
- [ ] 自定义评分函数
- [ ] 地理位置搜索
- [ ] 自动补全建议
- [ ] 高亮显示优化
- [ ] 拼写纠错
- [ ] 同义词扩展

---

## 九、常见问题

### Q1: 如何排查慢查询？

```bash
# 开启慢查询日志
PUT /logs/_settings
{
  "index.search.slowlog.threshold.query.warn": "10s",
  "index.search.slowlog.threshold.query.info": "5s",
  "index.search.slowlog.threshold.fetch.warn": "1s"
}

# 查看慢查询 Profile
GET /logs/_search?profile
{
  "query": { ... }
}
```

### Q2: 如何处理大字段？

```json
// 使用 _source filtering 或 doc_values
PUT /logs/_mapping
{
  "properties": {
    "large_field": {
      "type": "text",
      "doc_values": false,  // 不用于排序/聚合时关闭
      "norms": false        // 不需要评分时关闭
    }
  }
}
```

### Q3: 如何实现数据同步？

**方案 1：** Canal + Kafka + Logstash

**方案 2：** Logstash JDBC Input Plugin

**方案 3：** Elasticsearch Rollup API

---

## 十、参考资料

- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Elasticsearch 权威指南](https://www.elastic.co/guide/cn/elasticsearch/guide/current/index.html)
- [IK 分词器](https://github.com/medcl/elasticsearch-analysis-ik)
- [Elasticsearch 源码解析](https://github.com/elastic/elasticsearch)

---

**文档版本**: v1.0  
**最后更新**: 2026-03-15  
**作者**: itzixiao
