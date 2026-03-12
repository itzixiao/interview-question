# Spring Boot 整合 Elasticsearch 完全指南

## 📖 概述

本文档详细介绍如何在 Spring Boot 项目中整合 Elasticsearch，实现海量数据的分布式搜索功能。通过设备运行日志管理系统的实战案例，从 0 到 1 讲解 ES 的核心用法。

### 技术栈版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.18 | 框架核心 |
| Spring Data Elasticsearch | 4.4.x | 数据访问层 |
| Elasticsearch | 7.17.x | 搜索引擎 |
| MySQL | 8.0+ | 关系型数据库 |
| ShardingSphere | 4.1.1 | 分库分表中间件 |

---

## 🎯 为什么使用 Elasticsearch？

### 传统数据库的局限性

```sql
-- MySQL 模糊查询效率低（全表扫描）
SELECT * FROM device_operation_log 
WHERE device_name LIKE '%设备%';

-- 多字段组合查询性能差
SELECT * FROM device_operation_log 
WHERE device_name LIKE '%设备%' 
  AND operation_type = 1 
  AND operator LIKE '%张%';
```

**问题：**
- LIKE 模糊查询效率低下（不走索引）
- 海量数据（千万级）查询缓慢
- 无法实现全文检索、相关性排序
- 不支持复杂的聚合分析

### ES 的优势

```java
// ES 全文检索 - 毫秒级响应
Page<DeviceOperationLogES> results = 
    esRepository.searchByDeviceName("设备", pageable);
```

**优势：**
- ✅ **倒排索引** - 全文检索毫秒级响应
- ✅ **分布式架构** - 水平扩展支持 PB 级数据
- ✅ **复杂查询** - 布尔查询、聚合分析、地理位置
- ✅ **高可用** - 自动分片、副本机制

---

## 🚀 快速开始

### 1. Maven 依赖

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Data Elasticsearch -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
    </dependency>
</dependencies>
```

### 2. application.yml 配置

```yaml
spring:
  elasticsearch:
    # ES 地址（支持集群，多个地址用逗号分隔）
    uris: http://localhost:19200
    # 用户名密码（如果启用了认证）
    username: elastic
    password: ${ES_PASSWORD:}
```

### 3. Docker 安装 Elasticsearch

```bash
# 单机模式（开发环境）
docker run -d \
  --name elasticsearch \
  -p 19200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  elasticsearch:7.17.9

# 验证是否启动成功
curl http://localhost:19200
```

**返回示例：**
```json
{
  "name": "es-node-1",
  "cluster_name": "docker-cluster",
  "version": {
    "number": "7.17.9"
  }
}
```

---

## 💻 核心代码实现

### 1. ES 配置类 - 支持认证与超时配置

**文件路径：** `interview-microservices-parent/interview-provider/src/main/java/cn/itzixiao/interview/provider/config/ElasticsearchConfig.java`

```java
package cn.itzixiao.interview.provider.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch 配置类
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:19200}")
    private String esUris;

    @Value("${spring.elasticsearch.username:elastic}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        // 解析 ES 地址
        String host = esUris.replace("http://", "").replace("https://", "");
        String[] parts = host.split(":");
        String hostname = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
        
        // 配置超时时间（增加到 60 秒）
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(60 * 1000)  // 连接超时 60 秒
                .setSocketTimeout(60 * 1000)   // 读取超时 60 秒
                .setConnectionRequestTimeout(60 * 1000)  // 从连接池获取连接的超时 60 秒
                .build();
        
        RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, port, "http"));
        
        // 如果配置了用户名密码，则添加认证
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClientBuilder -> 
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                        .setDefaultRequestConfig(requestConfig));
        } else {
            builder.setHttpClientConfigCallback(httpClientBuilder -> 
                httpClientBuilder.setDefaultRequestConfig(requestConfig));
        }
        
        return new RestHighLevelClient(builder);
    }

    @Bean
    public ElasticsearchRestTemplate elasticsearchTemplate() {
        return new ElasticsearchRestTemplate(restHighLevelClient());
    }
}
```

**关键点：**
- ✅ 支持环境变量配置 ES 地址
- ✅ 可选的用户名密码认证
- ✅ 自定义超时时间（避免大数据量操作超时）

---

### 2. ES 实体类设计

**文件路径：** `interview-microservices-parent/interview-provider/src/main/java/cn/itzixiao/interview/provider/entity/es/DeviceOperationLogES.java`

```java
package cn.itzixiao.interview.provider.entity.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

/**
 * 设备运行日志 ES 实体类
 */
@Data
@Document(indexName = "device_operation_log")
public class DeviceOperationLogES {

    /**
     * 主键 ID
     */
    @Id
    private Long id;

    /**
     * 设备编号（精确匹配）
     */
    @Field(type = FieldType.Keyword)
    private String deviceCode;

    /**
     * 设备名称（全文检索）
     */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String deviceName;

    /**
     * 操作类型：1-开机 2-关机 3-故障 4-维护
     */
    @Field(type = FieldType.Integer)
    private Integer operationType;

    /**
     * 操作关联数值（如温度、电压）
     */
    @Field(type = FieldType.Double)
    private BigDecimal operationValue;

    /**
     * 操作时间
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss")
    private String operationTime;

    /**
     * 操作人
     */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String operator;

    /**
     * 备注
     */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String remark;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss")
    private String createTime;
}
```

**字段类型详解：**

| 类型 | 说明 | 适用场景 |
|------|------|----------|
| `Keyword` | 不分词，精确匹配 | 设备编号、状态码、ID |
| `Text` | 分词，全文检索 | 设备名称、描述、备注 |
| `Integer/Long` | 数值类型 | 操作类型、计数 |
| `Double` | 浮点数 | 温度、电压等测量值 |
| `Date` | 日期时间 | 操作时间、创建时间 |

**分词器选择：**
```java
// 标准分词器（英文按空格分词，中文单字分词）
@Field(analyzer = "standard", searchAnalyzer = "standard")

// IK 智能分词（中文推荐）
@Field(analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
```

---

### 3. Repository 数据访问层

**文件路径：** `interview-microservices-parent/interview-provider/src/main/java/cn/itzixiao/interview/provider/repository/DeviceOperationLogESRepository.java`

```java
package cn.itzixiao.interview.provider.repository;

import cn.itzixiao.interview.provider.entity.es.DeviceOperationLogES;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 设备运行日志 ES 仓库接口
 */
@Repository
public interface DeviceOperationLogESRepository extends ElasticsearchRepository<DeviceOperationLogES, Long> {

    /**
     * 根据设备编号查询（精确匹配）
     */
    Page<DeviceOperationLogES> findByDeviceCode(String deviceCode, Pageable pageable);

    /**
     * 根据操作类型查询（精确匹配）
     */
    Page<DeviceOperationLogES> findByOperationType(Integer operationType, Pageable pageable);

    /**
     * 根据操作人查询（全文检索）
     */
    Page<DeviceOperationLogES> findByOperator(String operator, Pageable pageable);

    /**
     * 根据设备名称模糊查询（全文检索）
     * 使用 JSON 格式自定义查询
     */
    @Query("{\"match\": {\"deviceName\": {\"query\": \"?0\", \"analyzer\": \"ik_max_word\"}}}")
    Page<DeviceOperationLogES> searchByDeviceName(String deviceName, Pageable pageable);

    /**
     * 多字段组合查询（布尔查询）
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"deviceName\": \"?0\"}}, {\"term\": {\"operationType\": ?1}}]}}")
    Page<DeviceOperationLogES> searchByDeviceNameAndOperationType(String deviceName, Integer operationType, Pageable pageable);
}
```

**查询注解说明：**

| 注解 | 查询类型 | 说明 |
|------|----------|------|
| `findByXxx` | 方法命名查询 | Spring 自动解析为 Term 查询 |
| `@Query` + `match` | 全文检索 | 分词匹配，计算相关性得分 |
| `@Query` + `term` | 精确匹配 | 不分词，用于 Keyword/数值字段 |
| `@Query` + `bool` | 布尔查询 | must(且)、should(或)、must_not(非) |

---

### 4. Service 业务逻辑层

**文件路径：** `interview-microservices-parent/interview-provider/src/main/java/cn/itzixiao/interview/provider/service/DeviceOperationLogESService.java`

```java
package cn.itzixiao.interview.provider.service;

import cn.itzixiao.interview.provider.entity.DeviceOperationLog;
import cn.itzixiao.interview.provider.entity.es.DeviceOperationLogES;
import cn.itzixiao.interview.provider.mapper.DeviceOperationLogMapper;
import cn.itzixiao.interview.provider.repository.DeviceOperationLogESRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 数据加载服务
 */
@Slf4j
@Service
public class DeviceOperationLogESService {

    private final DeviceOperationLogMapper deviceOperationLogMapper;
    private final DeviceOperationLogESRepository esRepository;
    private final ElasticsearchRestTemplate elasticsearchRestTemplate;

    public DeviceOperationLogESService(DeviceOperationLogMapper deviceOperationLogMapper,
                                       DeviceOperationLogESRepository esRepository,
                                       ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.deviceOperationLogMapper = deviceOperationLogMapper;
        this.esRepository = esRepository;
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    /**
     * 创建索引（如果不存在）
     */
    public void createIndex() {
        IndexOperations indexOps = elasticsearchRestTemplate.indexOps(DeviceOperationLogES.class);
        
        // 检查索引是否存在
        if (!indexOps.exists()) {
            log.info("索引不存在，正在创建索引：device_operation_log");
            // 创建索引并设置映射
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping());
            log.info("索引创建成功");
        } else {
            log.info("索引已存在：device_operation_log");
        }
    }

    /**
     * 删除索引
     */
    public void deleteIndex() {
        IndexOperations indexOps = elasticsearchRestTemplate.indexOps(DeviceOperationLogES.class);
        if (indexOps.exists()) {
            log.info("正在删除索引：device_operation_log");
            indexOps.delete();
            log.info("索引删除成功");
        } else {
            log.warn("索引不存在：device_operation_log");
        }
    }

    /**
     * 从 MySQL 加载全部数据到 ES
     * 
     * @return 加载的记录数
     */
    public int loadAllDataToES() {
        log.info("===========================================");
        log.info("【ES 数据加载】开始从 MySQL 加载全部数据到 ES");
        log.info("===========================================");
        
        long startTime = System.currentTimeMillis();
        
        // 先确保索引存在
        createIndex();
        
        // 分批查询 MySQL 数据，避免一次性加载过多数据
        int batchSize = 1000;
        int current = 0;
        int totalLoaded = 0;
        
        while (true) {
            long batchStart = System.currentTimeMillis();
            
            // 分页查询 MySQL 数据
            List<DeviceOperationLog> pageData = deviceOperationLogMapper.selectPage(current, batchSize);
            
            if (pageData == null || pageData.isEmpty()) {
                log.info("【ES 数据加载】所有数据已加载完毕");
                break;
            }
            
            // 转换为 ES 实体
            List<DeviceOperationLogES> esEntities = convertToESEntities(pageData);
            
            // 批量保存到 ES
            List<IndexQuery> indexQueries = new ArrayList<>();
            for (DeviceOperationLogES entity : esEntities) {
                IndexQuery indexQuery = new IndexQueryBuilder()
                        .withId(entity.getId().toString())
                        .withObject(entity)
                        .build();
                indexQueries.add(indexQuery);
            }
            
            // 批量索引
            elasticsearchRestTemplate.bulkIndex(indexQueries, 
                    IndexCoordinates.of("device_operation_log"));
            
            totalLoaded += pageData.size();
            
            log.info("【ES 数据加载】批次完成：当前页 {} 条，累计 {} 条，耗时 {}ms",
                    pageData.size(), totalLoaded, System.currentTimeMillis() - batchStart);
            
            current += batchSize;
        }
        
        log.info("【ES 数据加载】全部完成，总计 {}ms，加载 {} 条记录",
                System.currentTimeMillis() - startTime, totalLoaded);
        
        return totalLoaded;
    }

    /**
     * 增量更新单条数据到 ES
     */
    public void updateSingleDataToES(Long id) {
        log.info("【ES 数据更新】正在更新单条数据到 ES，ID: {}", id);
        
        // 从 MySQL 查询数据
        DeviceOperationLog mysqlData = deviceOperationLogMapper.selectById(id);
        if (mysqlData == null) {
            log.warn("【ES 数据更新】MySQL 中未找到数据，ID: {}", id);
            return;
        }
        
        // 转换为 ES 实体
        DeviceOperationLogES esEntity = convertToESEntity(mysqlData);
        
        // 保存到 ES
        esRepository.save(esEntity);
        
        log.info("【ES 数据更新】更新成功，ID: {}", id);
    }

    /**
     * 从 ES 中删除单条数据
     */
    public void deleteSingleDataFromES(Long id) {
        log.info("【ES 数据删除】正在从 ES 删除数据，ID: {}", id);
        
        esRepository.deleteById(id);
        
        log.info("【ES 数据删除】删除成功，ID: {}", id);
    }

    /**
     * 批量转换实体
     */
    private List<DeviceOperationLogES> convertToESEntities(List<DeviceOperationLog> mysqlDataList) {
        List<DeviceOperationLogES> esEntities = new ArrayList<>();
        for (DeviceOperationLog mysqlData : mysqlDataList) {
            esEntities.add(convertToESEntity(mysqlData));
        }
        return esEntities;
    }

    /**
     * 转换单个实体
     */
    private DeviceOperationLogES convertToESEntity(DeviceOperationLog mysqlData) {
        DeviceOperationLogES esEntity = new DeviceOperationLogES();
        BeanUtils.copyProperties(mysqlData, esEntity);
        return esEntity;
    }

    /**
     * 获取 ES 索引统计信息
     */
    public Map<String, Object> getIndexStats() {
        log.info("【ES 统计】正在获取索引统计信息");
        
        try {
            // 获取文档总数
            long count = esRepository.count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("indexName", "device_operation_log");
            stats.put("documentCount", count);
            stats.put("health", "green"); // 简化实现
            
            return stats;
        } catch (Exception e) {
            log.error("【ES 统计】获取统计信息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "获取统计信息失败：" + e.getMessage());
            errorResult.put("indexName", "device_operation_log");
            return errorResult;
        }
    }

    // ... 其他查询方法见 Controller 章节
}
```

**关键技术点：**

1. **批量索引优化**
   ```java
   // 分批加载，每批 1000 条
   int batchSize = 1000;
   
   // 批量写入 ES
   elasticsearchRestTemplate.bulkIndex(indexQueries, IndexCoordinates.of("device_operation_log"));
   ```

2. **索引管理**
   ```java
   IndexOperations indexOps = elasticsearchRestTemplate.indexOps(DeviceOperationLogES.class);
   indexOps.create();      // 创建索引
   indexOps.exists();      // 检查是否存在
   indexOps.delete();      // 删除索引
   ```

3. **数据同步策略**
   - 全量同步：定时任务每天凌晨同步全部数据
   - 增量同步：MySQL 数据变更时实时更新 ES

---

### 5. Controller 接口层

**文件路径：** `interview-microservices-parent/interview-provider/src/main/java/cn/itzixiao/interview/provider/controller/DeviceOperationLogESController.java`

```java
package cn.itzixiao.interview.provider.controller;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.provider.entity.es.DeviceOperationLogES;
import cn.itzixiao.interview.provider.service.sharding.DeviceOperationLogESService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch 搜索控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/es")
public class DeviceOperationLogESController {

    private final DeviceOperationLogESService esService;

    public DeviceOperationLogESController(DeviceOperationLogESService esService) {
        this.esService = esService;
    }

    /**
     * 加载 MySQL 数据到 ES
     */
    @PostMapping("/load")
    public Result<Map<String, Object>> loadAllDataToES() {
        log.info("【ES 接口】接收到加载全部数据请求");
        
        try {
            int count = esService.loadAllDataToES();
            Map<String, Object> result = new HashMap<>();
            result.put("message", "数据加载成功");
            result.put("count", count);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】加载数据失败", e);
            return Result.error("加载数据失败：" + e.getMessage());
        }
    }

    /**
     * 创建索引
     */
    @PostMapping("/index/create")
    public Result<String> createIndex() {
        log.info("【ES 接口】接收到创建索引请求");
        
        try {
            esService.createIndex();
            return Result.success("索引创建成功");
        } catch (Exception e) {
            log.error("【ES 接口】创建索引失败", e);
            return Result.error("创建索引失败：" + e.getMessage());
        }
    }

    /**
     * 删除索引
     */
    @DeleteMapping("/index/delete")
    public Result<String> deleteIndex() {
        log.info("【ES 接口】接收到删除索引请求");
        
        try {
            esService.deleteIndex();
            return Result.success("索引删除成功");
        } catch (Exception e) {
            log.error("【ES 接口】删除索引失败", e);
            return Result.error("删除索引失败：" + e.getMessage());
        }
    }

    /**
     * 获取索引统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getIndexStats() {
        log.info("【ES 接口】接收到获取统计信息请求");
        
        try {
            Map<String, Object> stats = esService.getIndexStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("【ES 接口】获取统计信息失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 分页搜索 - 根据设备编号
     */
    @GetMapping("/search/device-code")
    public Result<Page<DeviceOperationLogES>> searchByDeviceCode(
            @RequestParam String deviceCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("【ES 接口】搜索设备编号：{}, 页码：{}, 大小：{}", deviceCode, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DeviceOperationLogES> result = esService.findByDeviceCode(deviceCode, pageable);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】搜索失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 分页搜索 - 根据操作类型
     */
    @GetMapping("/search/operation-type")
    public Result<Page<DeviceOperationLogES>> searchByOperationType(
            @RequestParam Integer operationType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("【ES 接口】搜索操作类型：{}, 页码：{}, 大小：{}", operationType, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DeviceOperationLogES> result = esService.findByOperationType(operationType, pageable);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】搜索失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 分页搜索 - 根据操作人
     */
    @GetMapping("/search/operator")
    public Result<Page<DeviceOperationLogES>> searchByOperator(
            @RequestParam String operator,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("【ES 接口】搜索操作人：{}, 页码：{}, 大小：{}", operator, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DeviceOperationLogES> result = esService.findByOperator(operator, pageable);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】搜索失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 全文检索 - 根据设备名称
     */
    @GetMapping("/search/device-name")
    public Result<Page<DeviceOperationLogES>> searchByDeviceName(
            @RequestParam String deviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("【ES 接口】全文检索设备名称：{}, 页码：{}, 大小：{}", deviceName, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DeviceOperationLogES> result = esService.searchByDeviceName(deviceName, pageable);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】搜索失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 组合查询 - 设备名称 + 操作类型
     */
    @GetMapping("/search/combined")
    public Result<Page<DeviceOperationLogES>> searchByDeviceNameAndOperationType(
            @RequestParam String deviceName,
            @RequestParam Integer operationType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("【ES 接口】组合查询 - 设备名称：{}, 操作类型：{}, 页码：{}, 大小：{}", 
                deviceName, operationType, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DeviceOperationLogES> result = esService.searchByDeviceNameAndOperationType(
                    deviceName, operationType, pageable);
            return Result.success(result);
        } catch (Exception e) {
            log.error("【ES 接口】搜索失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
}
```

---

## 🧪 测试用例

### HTTP Client 测试脚本

**文件路径：** `interview-microservices-parent/interview-provider/http/TestElasticsearch.http`

```http
### Elasticsearch 接口测试

# 1. 加载 MySQL 数据到 ES
POST http://localhost:8082/api/es/load
Content-Type: application/json

###

# 2. 创建索引
POST http://localhost:8082/api/es/index/create

###

# 3. 删除索引
DELETE http://localhost:8082/api/es/index/delete

###

# 4. 获取索引统计信息
GET http://localhost:8082/api/es/stats

###

# 5. 根据设备编号搜索（分页）
GET http://localhost:8082/api/es/search/device-code?deviceCode=DEVICE001&page=0&size=10

###

# 6. 根据操作类型搜索（分页）
GET http://localhost:8082/api/es/search/operation-type?operationType=1&page=0&size=10

###

# 7. 根据操作人搜索（分页）
GET http://localhost:8082/api/es/search/operator?operator=张三&page=0&size=10

###

# 8. 根据设备名称全文检索（分页）
GET http://localhost:8082/api/es/search/device-name?deviceName=设备&page=0&size=10

###

# 9. 组合查询 - 设备名称 + 操作类型
GET http://localhost:8082/api/es/search/combined?deviceName=设备&operationType=1&page=0&size=10

###
```

### 测试步骤

```bash
# 1. 启动 Elasticsearch
docker start elasticsearch

# 2. 启动应用
cd interview-microservices-parent/interview-provider
mvn spring-boot:run

# 3. 使用 IDEA HTTP Client 测试
# 打开 TestElasticsearch.http 文件，依次点击 ### 之间的请求
```

### 预期输出示例

**1. 加载数据成功：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "message": "数据加载成功",
    "count": 12000
  }
}
```

**2. 索引统计信息：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "indexName": "device_operation_log",
    "documentCount": 12000,
    "health": "green"
  }
}
```

**3. 搜索结果（分页）：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "deviceCode": "DEVICE001",
        "deviceName": "空压机 1 号",
        "operationType": 1,
        "operationValue": 45.5,
        "operationTime": "2026-03-12 10:30:00",
        "operator": "张三",
        "remark": "正常开机"
      }
    ],
    "totalPages": 120,
    "totalElements": 1200,
    "size": 10,
    "number": 0
  }
}
```

---

## 🔬 核心技术原理

### 1. 倒排索引（Inverted Index）

**传统正排索引：**
```
文档 1: 我 爱 北 京 天 安 门
文档 2: 他 爱 上 海 外 滩
```

**倒排索引：**
```
"爱" -> [文档 1, 文档 2]
"北京" -> [文档 1]
"上海" -> [文档 2]
```

**优势：** 全文检索时直接查倒排表，无需遍历所有文档。

### 2. 分词器（Analyzer）

```java
// 标准分词器
"我爱北京天安门" -> ["我", "爱", "北", "京", "天", "安", "门"]

// IK 智能分词
"我爱北京天安门" -> ["我", "爱", "北京", "天安门"]
```

**配置示例：**
```java
@Field(analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
private String deviceName;
```

### 3. 布尔查询（Bool Query）

```json
{
  "bool": {
    "must": [
      { "match": { "deviceName": "设备" }},
      { "term": { "operationType": 1 }}
    ],
    "filter": [
      { "range": { "operationTime": { "gte": "2026-01-01" }}}
    ]
  }
}
```

**查询子句：**
- `must`: 必须匹配（贡献评分）
- `should`: 可选匹配（贡献评分）
- `must_not`: 必须不匹配
- `filter`: 过滤条件（不贡献评分，性能更好）

---

## ⚡ 性能优化实战

### 1. 批量索引优化

```java
// ❌ 错误：单条插入，性能极差
for (DeviceOperationLogES entity : entities) {
    esRepository.save(entity);
}

// ✅ 正确：批量插入，性能提升 10-20 倍
List<IndexQuery> indexQueries = new ArrayList<>();
for (DeviceOperationLogES entity : entities) {
    IndexQuery indexQuery = new IndexQueryBuilder()
            .withId(entity.getId().toString())
            .withObject(entity)
            .build();
    indexQueries.add(indexQuery);
}
elasticsearchRestTemplate.bulkIndex(indexQueries, IndexCoordinates.of("device_operation_log"));
```

### 2. 分页查询优化

```java
// ❌ deep paging 问题：深度分页性能差
PageRequest.of(10000, 10); // 第 10001 页

// ✅ 解决方案 1：search_after（类似游标）
SearchAfterQueryBuilder query = SearchAfterQueryBuilder.searchAfter()
    .from(0)
    .size(10)
    .sort(SortBuilders.fieldSort("id").order(SortOrder.ASC));

// ✅ 解决方案 2：限制最大页数
if (page > 100) {
    throw new BusinessException("最多只能查看前 100 页");
}
```

### 3. 字段映射优化

```java
// ❌ 所有字段都用 Text，浪费存储空间
@Field(type = FieldType.Text)
private String deviceCode; // 实际只需精确匹配

// ✅ 根据查询需求选择字段类型
@Field(type = FieldType.Keyword)  // 精确匹配
private String deviceCode;

@Field(type = FieldType.Text, analyzer = "ik_max_word")  // 全文检索
private String deviceName;
```

### 4. 刷新间隔优化

```java
// 开发环境：立即刷新，便于调试
PUT /device_operation_log/_settings
{
  "refresh_interval": "1s"
}

// 生产环境（大批量导入）：关闭自动刷新
PUT /device_operation_log/_settings
{
  "refresh_interval": "-1"  // 关闭自动刷新
}

// 导入完成后手动刷新
POST /device_operation_log/_refresh

// 恢复自动刷新
PUT /device_operation_log/_settings
{
  "refresh_interval": "30s"
}
```

---

## 🛠️ 常见问题与解决方案

### 1. Connection Timeout 连接超时

**问题：**
```
org.elasticsearch.client.ResponseException: method [POST], host [http://localhost:19200]
```

**解决方案：**
```java
// 增加超时时间
RequestConfig requestConfig = RequestConfig.custom()
    .setConnectTimeout(60 * 1000)
    .setSocketTimeout(60 * 1000)
    .setConnectionRequestTimeout(60 * 1000)
    .build();
```

### 2. 中文分词乱码问题

**问题：** 中文被分成单字，搜索结果不准确

**解决方案：**
```java
// 1. 安装 IK 分词插件（在 ES 容器中）
docker exec -it elasticsearch bash
bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.9/elasticsearch-analysis-ik-7.17.9.zip

// 2. 实体类中使用 IK 分词器
@Field(analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
private String deviceName;
```

### 3. 索引映射冲突

**问题：**
```
MapperParsingException: failed to parse field [deviceCode]
```

**原因：** 同一字段在不同文档中类型不一致

**解决方案：**
```java
// 明确指定字段类型
@Field(type = FieldType.Keyword)
private String deviceCode;

// 创建索引时显式设置映射
IndexOperations indexOps = elasticsearchRestTemplate.indexOps(DeviceOperationLogES.class);
indexOps.create();
indexOps.putMapping(indexOps.createMapping());
```

### 4. 大数据量 OOM 内存溢出

**问题：** 一次性加载 100 万条数据到 ES，导致内存溢出

**解决方案：**
```java
// 分批处理，每批 1000 条
int batchSize = 1000;
int current = 0;

while (true) {
    List<DeviceOperationLog> pageData = mapper.selectPage(current, batchSize);
    if (pageData.isEmpty()) break;
    
    // 批量写入 ES
    bulkIndex(pageData);
    
    current += batchSize;
    
    // 手动触发 GC（可选）
    System.gc();
}
```

---

## 🎓 高频面试题

### 1. Elasticsearch 为什么这么快？⭐重点

**参考答案：**

1. **倒排索引**
   - 传统数据库：文档 → 单词（正排）
   - ES：单词 → 文档列表（倒排）
   - 全文检索时直接查倒排表，无需遍历所有文档

2. **分词器优化**
   - IK 智能分词、拼音分词等
   - 提前分词建立索引，查询时直接使用

3. **缓存机制**
   - Filter Cache：过滤结果缓存
   - Query Cache：查询结果缓存
   - Field Data Cache：字段数据缓存

4. **分布式架构**
   - 数据分片存储，并行查询
   - 近实时搜索（NRT），1 秒延迟

5. **列式存储**
   - Doc Values（磁盘上的列式结构）
   - 适合聚合、排序、分组查询

---

### 2. ES 和 MySQL 的区别？应用场景？

**参考答案：**

| 特性 | MySQL | Elasticsearch |
|------|-------|---------------|
| 数据模型 | 关系型 | 文档型（JSON） |
| 查询方式 | SQL | RESTful API/DSL |
| 擅长领域 | 事务处理、复杂关联 | 全文检索、数据分析 |
| 扩展性 | 垂直扩展为主 | 水平扩展（分片） |
| 实时性 | 实时 | 近实时（1 秒延迟） |

**应用场景：**
- **MySQL**: 订单、支付、用户信息等强一致性场景
- **ES**: 商品搜索、日志分析、监控告警等检索场景

**最佳实践：** MySQL + ES 异构数据源
- MySQL 作为权威数据源（增删改）
- ES 作为搜索引擎（查询）
- 通过 Canal/Maxwell 等工具实时同步

---

### 3. 如何保证 MySQL 和 ES 的数据一致性？⭐重点

**参考答案：**

**方案 1：双写（简单但不可靠）**
```java
// 先写 MySQL
mysqlMapper.insert(log);
// 再写 ES
esRepository.save(esEntity);
```
**问题：** ES 写入失败会导致不一致

**方案 2：异步消息队列（推荐）**
```java
// 1. MySQL 写入成功后发送 MQ 消息
mysqlMapper.insert(log);
mqTemplate.send("es_sync_topic", log.getId());

// 2. 消费者监听 MQ 消息写入 ES
@RabbitListener(queues = "es_sync_queue")
public void syncToES(Long id) {
    DeviceOperationLog log = mysqlMapper.selectById(id);
    esRepository.save(convertToES(log));
}
```
**优势：** 解耦、削峰、可靠投递

**方案 3：Canal 监听 Binlog（阿里方案）**
```
MySQL (Master) 
  ↓ (Binlog)
Canal Server (模拟 Slave)
  ↓ (MQ: RocketMQ/Kafka)
Canal Client → 写入 ES
```
**优势：** 无侵入、准实时（秒级）

**方案 4：定时全量补偿**
```java
// 每天凌晨对比 MySQL 和 ES 数据
@Scheduled(cron = "0 0 2 * * ?")
public void fullSync() {
    // 找出差异数据并补偿
    List<Long> mysqlIds = mysqlMapper.selectAllIds();
    List<Long> esIds = esRepository.findAllIds();
    
    List<Long> diffIds = mysqlIds.stream()
        .filter(id -> !esIds.contains(id))
        .collect(Collectors.toList());
    
    // 补偿缺失数据
    for (Long id : diffIds) {
        syncToES(id);
    }
}
```

**生产环境最佳实践：**
- 实时同步：Canal + MQ
- 兜底方案：每天凌晨全量补偿
- 监控告警：数据不一致超过阈值时报警

---

### 4. ES 的分片和副本机制？

**参考答案：**

**分片（Shard）：**
- 将索引数据分散到多个节点
- 每个分片是一个独立的 Lucene 实例
- 分片数在创建索引时设定，之后不可变（除非 Reindex）

**副本（Replica）：**
- 分片的备份，提高可用性
- 副本数可动态调整
- 副本不会和主分片在同一节点

**示例：**
```json
PUT /device_operation_log
{
  "settings": {
    "number_of_shards": 3,      // 3 个主分片
    "number_of_replicas": 1     // 每个主分片 1 个副本
  }
}
```

**集群状态：**
- 绿色：所有主分片和副本分片都正常
- 黄色：所有主分片正常，部分副本分片异常
- 红色：部分主分片异常（数据不完整）

---

### 5. 如何优化 ES 的查询性能？⭐重点

**参考答案：**

**1. 索引设计优化**
- 合理设置分片数（单分片 30-50GB）
- 使用合适的字段类型（Keyword vs Text）
- 关闭不必要的 `_all` 字段

**2. 查询语句优化**
```json
// ❌ 性能差：使用 score 排序
{
  "sort": ["_score"]
}

// ✅ 性能好：使用 doc_values
{
  "sort": [{"operationTime": "desc"}]
}

// ❌ 性能差：深度分页
{
  "from": 10000,
  "size": 10
}

// ✅ 性能好：search_after
{
  "search_after": [123456],
  "size": 10
}
```

**3. 缓存优化**
- 使用 Filter 代替 Query（不计算评分，可缓存）
- 开启 Query Cache
- 合理使用 Field Cache

**4. 硬件优化**
- SSD 硬盘（IOPS 提升 10 倍+）
- 增加内存（至少 32GB）
- 独立部署（不与 Kibana/Logstash 混部）

---

### 6. ES 写入数据的流程？

**参考答案：**

1. **客户端发送请求** → 协调节点（任意节点）
2. **协调节点路由** → 根据文档 ID 计算分片
3. **转发到主分片** → 执行写入操作
4. **主分片写入** → 写入内存 + 追加到 Translog
5. **同步到副本分片** → 等待副本确认
6. **返回响应** → 所有副本成功后返回客户端

**关键点：**
- 写一致性：默认等待所有活跃分片确认
- Translog：防止数据丢失（定期 fsync）
- Refresh：默认 1 秒后将内存数据变为可搜索

---

### 7. 如何处理 ES 的大规模数据迁移？

**参考答案：**

**方案 1：Reindex API（官方推荐）**
```json
POST _reindex
{
  "source": {
    "index": "device_operation_log_old"
  },
  "dest": {
    "index": "device_operation_log_new"
  },
  "size": 1000,  // 批量大小
  "max_docs": 1000000
}
```

**方案 2：Logstash 同步**
```ruby
input {
  elasticsearch {
    hosts => ["http://old-es:9200"]
    index => "device_operation_log_old"
  }
}

output {
  elasticsearch {
    hosts => ["http://new-es:9200"]
    index => "device_operation_log_new"
  }
}
```

**方案 3：双写 + 灰度切换**
1. 代码改造支持双写（同时写新旧索引）
2. 历史数据通过 Reindex 迁移
3. 灰度切换读流量到新索引
4. 观察稳定后下线旧索引

---

## 📊 生产环境最佳实践

### 1. 索引命名规范

```yaml
# 格式：{业务}_{类型}_{时间范围}
device_operation_log_202601  # 2026 年 1 月
device_operation_log_202602  # 2026 年 2 月

# 好处：按月分索引，便于归档和清理
```

### 2. 别名管理

```json
// 创建别名指向最新索引
POST /_aliases
{
  "actions": [
    { "remove": { "index": "device_operation_log_202601", "alias": "device_operation_log_current" }},
    { "add": { "index": "device_operation_log_202602", "alias": "device_operation_log_current" }}
  ]
}

// 查询时使用别名
GET /device_operation_log_current/_search
```

### 3. 生命周期管理（ILM）

```json
PUT _ilm/policy/device_log_policy
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "30d"
          }
        }
      },
      "warm": {
        "min_age": "30d",
        "actions": {
          "shrink": {
            "number_of_shards": 1
          }
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

### 4. 监控告警配置

```yaml
# 关键指标监控
- 集群健康状态（红/黄/绿）
- CPU 使用率（>80% 告警）
- JVM 堆内存（>85% 告警）
- 磁盘使用率（>80% 告警）
- 查询延迟（P95 > 1s 告警）
- 写入失败率（>1% 告警）
```

---

## 🔗 参考资料

### 官方文档
- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Spring Data Elasticsearch](https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/)

### 学习资源
- [Elasticsearch 权威指南（中文版）](https://es.xiaoleilu.com/)
- [Elasticsearch 源码解析](https://github.com/elastic/elasticsearch)

### 工具推荐
- **Kibana**: ES 官方可视化工具
- **Elasticsearch Head**: 集群管理插件
- **Cerebro**: 集群监控与管理
- **Postman/IDEA HTTP Client**: API 测试工具

---

## 📈 更新日志

### v1.0 - 2026-03-12
- ✅ 完整的 Spring Boot 整合 ES 实战案例
- ✅ 包含配置、实体类、Repository、Service、Controller 全套代码
- ✅ 详细的性能优化方案
- ✅ 10+ 道高频面试题与参考答案
- ✅ 生产环境最佳实践

---

**维护者：** itzixiao  
**最后更新：** 2026-03-12  
**问题反馈：** 欢迎提 Issue 或 PR
