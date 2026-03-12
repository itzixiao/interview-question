# ShardingSphere-JDBC 整合实战指南

## 1. 项目背景

### 1.1 需求场景
- **数据表按月分片**：`device_operation_log` 表按月份拆分为 12 个子表（`device_operation_log_202601` ~ `device_operation_log_202612`）
- **时间范围路由**：根据 `operation_time` 字段自动路由到对应的月份表
- **精确查询优化**：单月查询只访问单张表，避免全表扫描
- **范围查询支持**：多个月份查询自动路由到所有相关表

### 1.2 技术选型
- **ShardingSphere 版本**：4.1.1
- **Spring Boot 版本**：2.7.18
- **MyBatis 版本**：3.5.10
- **数据库**：MySQL 8.0

---

## 2. 环境准备

### 2.1 Maven 依赖

```xml
<!-- interview-provider/pom.xml -->
<dependencies>
    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- ShardingSphere-JDBC -->
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>sharding-jdbc-spring-boot-starter</artifactId>
        <version>4.1.1</version>
    </dependency>

    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 2.2 数据库初始化

```sql
-- 创建逻辑表（ShardingSphere 会自动映射到实际表）
CREATE TABLE IF NOT EXISTS `device_operation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `device_code` varchar(50) NOT NULL COMMENT '设备编号',
  `device_name` varchar(100) DEFAULT NULL COMMENT '设备名称',
  `operation_type` int(11) DEFAULT NULL COMMENT '操作类型',
  `operation_value` decimal(10,2) DEFAULT NULL COMMENT '操作值',
  `operation_time` datetime NOT NULL COMMENT '操作时间',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作员',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备运行日志';

-- 创建 12 个实际分表（手动创建或通过脚本）
CREATE TABLE `device_operation_log_202601` LIKE `device_operation_log`;
CREATE TABLE `device_operation_log_202602` LIKE `device_operation_log`;
-- ... 依此类推至 202612
```

---

## 3. 核心配置

### 3.1 数据源与分片规则配置

```yaml
# application-dev.yml
spring:
  shardingsphere:
    # 开启 SQL 显示（便于调试）
    props:
      sql:
        show: true
    
    # 数据源配置
    datasource:
      names: ds0
      ds0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:3306/interview?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: ${MYSQL_PASSWORD:}
        hikari:
          minimum-idle: 5
          maximum-pool-size: 20
          connection-timeout: 30000
    
    # 分库分表规则
    sharding:
      # 默认数据库策略（不分片）
      default-database-strategy:
        none:
      
      # 表分片规则
      tables:
        device_operation_log:
          # 实际数据节点（必须带前导零）
          actual-data-nodes: ds0.device_operation_log_202601,ds0.device_operation_log_202602,ds0.device_operation_log_202603,ds0.device_operation_log_202604,ds0.device_operation_log_202605,ds0.device_operation_log_202606,ds0.device_operation_log_202607,ds0.device_operation_log_202608,ds0.device_operation_log_202609,ds0.device_operation_log_202610,ds0.device_operation_log_202611,ds0.device_operation_log_202612
          
          # 表分片策略
          table-strategy:
            standard:
              # 分片列（必须是数据库字段名）
              sharding-column: operation_time
              
              # 精确分片算法（用于 =、IN 查询）
              precise-algorithm-class-name: cn.itzixiao.interview.provider.config.sharding.DeviceOperationLogMonthShardingAlgorithm
              
              # 范围分片算法（用于 BETWEEN、>、< 查询）
              range-algorithm-class-name: cn.itzixiao.interview.provider.config.sharding.DeviceOperationLogMonthShardingAlgorithm
```

**⚠️ 关键注意事项**：
1. `actual-data-nodes` 必须显式列出所有带前导零的表名（`202601` 而不是 `20261`）
2. ShardingSphere 4.1.1 不支持 Groovy 表达式的复杂格式化
3. 分片列 `operation_time` 必须是数据库中的实际字段名

---

## 4. 自定义分片算法实现

### 4.1 精确分片 + 范围分片算法

```java
// DeviceOperationLogMonthShardingAlgorithm.java
package cn.itzixiao.interview.provider.config.sharding;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 设备运行日志 - 按月份分片算法
 * 
 * 精确分片：用于 =、IN 查询，路由到单个月份表
 * 范围分片：用于 BETWEEN、>、< 查询，路由到多个月份表
 */
@Slf4j
public class DeviceOperationLogMonthShardingAlgorithm implements PreciseShardingAlgorithm<Comparable<?>>, RangeShardingAlgorithm<Comparable<?>> {
    
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    
    /**
     * 精确分片（用于 =、IN 查询）
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Comparable<?>> shardingValue) {
        LocalDateTime operationTime = (LocalDateTime) shardingValue.getValue();
        String targetTableName = getTargetTableName(operationTime);
        
        log.info("精确分片：operationTime={}, targetTable={}", operationTime, targetTableName);
        
        if (availableTargetNames.contains(targetTableName)) {
            return targetTableName;
        }
        
        throw new IllegalArgumentException(
            String.format("未找到对应的分表：%s, 可用表：%s", targetTableName, availableTargetNames)
        );
    }
    
    /**
     * 范围分片（用于 BETWEEN、>、< 等范围查询）
     */
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Comparable<?>> shardingValue) {
        Range<Comparable<?>> valueRange = shardingValue.getValueRange();
        
        LocalDateTime lowerBound = valueRange.hasLowerBound() ? (LocalDateTime) valueRange.lowerEndpoint() : null;
        LocalDateTime upperBound = valueRange.hasUpperBound() ? (LocalDateTime) valueRange.upperEndpoint() : null;
        
        log.info("范围分片：startTime={}, endTime={}", lowerBound, upperBound);
        
        return calculateRangeTables(availableTargetNames, lowerBound, upperBound);
    }
    
    /**
     * 获取目标表名
     */
    private String getTargetTableName(LocalDateTime operationTime) {
        String monthSuffix = operationTime.format(MONTH_FORMATTER);
        return "device_operation_log_" + monthSuffix;
    }
    
    /**
     * 计算范围查询涉及的所有表
     */
    private Collection<String> calculateRangeTables(
            Collection<String> availableTargetNames, 
            LocalDateTime startTime, 
            LocalDateTime endTime) {
        
        // 生成所有可能的月份表（2026 年 1-12 月）
        Collection<String> targetTables = IntStream.rangeClosed(1, 12)
                .mapToObj(i -> {
                    String month = String.format("%02d", i);
                    return "device_operation_log_2026" + month;
                })
                .collect(Collectors.toList());
        
        // 过滤出在范围内的表
        return targetTables.stream()
                .filter(availableTargetNames::contains)
                .filter(tableName -> {
                    String monthStr = tableName.substring("device_operation_log_".length());
                    try {
                        LocalDateTime tableTime = LocalDateTime.parse(monthStr + "01000000", 
                                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                        
                        // 检查下界
                        boolean meetsLowerBound = (startTime == null) || 
                            !tableTime.isBefore(startTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
                        
                        // 检查上界
                        boolean meetsUpperBound = (endTime == null) || 
                            !tableTime.isAfter(endTime.withDayOfMonth(
                                    endTime.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59));
                        
                        return meetsLowerBound && meetsUpperBound;
                    } catch (Exception e) {
                        log.warn("解析表名失败：{}, error={}", tableName, e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }
}
```

**🔍 关键点解析**：
1. 实现 `PreciseShardingAlgorithm` 和 `RangeShardingAlgorithm` 两个接口
2. 精确分片返回单个表名，范围分片返回表名集合
3. 范围计算时需要考虑月初和月末的边界情况
4. 添加详细日志便于调试路由过程

---

## 5. MyBatis 集成

### 5.1 Mapper 接口定义

```java
// DeviceOperationLogMapper.java
package cn.itzixiao.interview.provider.mapper;

import cn.itzixiao.interview.provider.entity.DeviceOperationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DeviceOperationLogMapper extends BaseMapper<DeviceOperationLog> {

    /**
     * 按时间范围查询（测试分片路由）
     * 注意：使用 XML 映射文件，移除 @Select 注解避免参数传递问题
     */
    List<DeviceOperationLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 按设备编号和时间范围查询
     */
    List<DeviceOperationLog> selectByDeviceAndTimeRange(@Param("deviceCode") String deviceCode,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);
}
```

### 5.2 MyBatis XML 映射文件

```xml
<!-- DeviceOperationLogMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="cn.itzixiao.interview.provider.mapper.DeviceOperationLogMapper">

    <!-- 按时间范围查询 -->
    <select id="selectByTimeRange" resultType="cn.itzixiao.interview.provider.entity.DeviceOperationLog">
        SELECT * FROM device_operation_log
        WHERE operation_time >= #{startTime,typeHandler=cn.itzixiao.interview.provider.config.handler.CustomLocalDateTimeTypeHandler}
          AND operation_time &lt;= #{endTime,typeHandler=cn.itzixiao.interview.provider.config.handler.CustomLocalDateTimeTypeHandler}
        ORDER BY operation_time ASC
    </select>

    <!-- 按设备编号和时间范围查询 -->
    <select id="selectByDeviceAndTimeRange" resultType="cn.itzixiao.interview.provider.entity.DeviceOperationLog">
        SELECT * FROM device_operation_log
        WHERE device_code = #{deviceCode}
          AND operation_time >= #{startTime,typeHandler=cn.itzixiao.interview.provider.config.handler.CustomLocalDateTimeTypeHandler}
          AND operation_time &lt;= #{endTime,typeHandler=cn.itzixiao.interview.provider.config.handler.CustomLocalDateTimeTypeHandler}
        ORDER BY operation_time ASC
    </select>

</mapper>
```

**⚠️ 重要说明**：
- 使用 XML 映射而非 `@Select` 注解，确保多参数正确传递给 ShardingSphere
- 使用自定义 `CustomLocalDateTimeTypeHandler` 处理 LocalDateTime 类型转换

---

## 6. 测试验证

### 6.1 测试控制器

```java
// ShardingTestController.java
@RestController
@RequestMapping("/sharding/test")
public class ShardingTestController {
    
    @Resource
    private DeviceOperationLogMapper deviceOperationLogMapper;
    
    /**
     * 精确查询 - 应该只路由到单个月份表
     */
    @GetMapping("/precise")
    public Result<List<DeviceOperationLog>> testPreciseQuery(
            @RequestParam String time) {
        LocalDateTime operationTime = LocalDateTime.parse(time);
        List<DeviceOperationLog> result = deviceOperationLogMapper.selectByTimeRange(
            operationTime, 
            operationTime.plusHours(23).plusMinutes(59).plusSeconds(59)
        );
        return Result.success(result);
    }
    
    /**
     * 范围查询 - 应该路由到多个月份表
     */
    @GetMapping("/range")
    public Result<List<DeviceOperationLog>> testRangeQuery(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.parse(endTime);
        List<DeviceOperationLog> result = deviceOperationLogMapper.selectByTimeRange(start, end);
        return Result.success(result);
    }
    
    /**
     * 全年查询 - 应该路由到所有 12 张表
     */
    @GetMapping("/all-year")
    public Result<List<DeviceOperationLog>> testAllYearData() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
        List<DeviceOperationLog> result = deviceOperationLogMapper.selectByTimeRange(start, end);
        return Result.success(result);
    }
}
```

### 6.2 测试命令

```bash
# 1. 精确查询（只访问 3 月份的表）
curl "http://localhost:8082/sharding/test/precise?time=2026-03-15T10:30"

# 2. 范围查询（访问 1-6 月的 6 张表）
curl "http://localhost:8082/sharding/test/range?startTime=2026-01-01T00:00&endTime=2026-06-30T23:59"

# 3. 全年查询（访问所有 12 张表）
curl "http://localhost:8082/sharding/test/all-year"
```

### 6.3 预期日志输出

#### 精确查询示例
```
INFO  c.i.i.p.c.ShardingTestController : 精确查询测试，operationTime=2026-03-15T10:30
INFO  DeviceOperationLogMonthShardingAlgorithm : 范围分片：startTime=2026-03-15T10:30, endTime=2026-03-15T23:59:59
INFO  [DEBUG] availableTargetNames: [device_operation_log_202601, ..., device_operation_log_202612]
INFO  [DEBUG] startTime: 2026-03-15T10:30, endTime: 2026-03-15T23:59:59
INFO  [DEBUG] generated targetTables: [device_operation_log_202601, ..., device_operation_log_202612]
INFO  [DEBUG] table: device_operation_log_202603, tableTime: 2026-03-01T00:00, meetsLowerBound: true, meetsUpperBound: true
INFO  [DEBUG] final result: [device_operation_log_202603]
INFO  ShardingSphere-SQL : Logic SQL: SELECT * FROM device_operation_log WHERE operation_time >= ? AND operation_time <= ? ORDER BY operation_time ASC
INFO  ShardingSphere-SQL : Actual SQL: ds0 ::: SELECT * FROM device_operation_log_202603 WHERE operation_time >= ? AND operation_time <= ? ORDER BY operation_time ASC
```

#### 范围查询示例
```
INFO  DeviceOperationLogMonthShardingAlgorithm : 范围分片：startTime=2026-01-01T00:00, endTime=2026-06-30T23:59:59
INFO  [DEBUG] final result: [device_operation_log_202601, device_operation_log_202602, device_operation_log_202603, device_operation_log_202604, device_operation_log_202605, device_operation_log_202606]
INFO  ShardingSphere-SQL : Actual SQL: ds0 ::: SELECT * FROM device_operation_log_202601 WHERE ...
INFO  ShardingSphere-SQL : Actual SQL: ds0 ::: SELECT * FROM device_operation_log_202602 WHERE ...
...
INFO  ShardingSphere-SQL : Actual SQL: ds0 ::: SELECT * FROM device_operation_log_202606 WHERE ...
```

---

## 7. 常见问题与解决方案

### 7.1 `no table route info` 错误

**问题现象**：
```
Caused by: java.lang.IllegalStateException: no table route info
```

**根本原因**：
- `actual-data-nodes` 生成的表名格式与分片算法期望的格式不匹配
- 例如：生成了 `device_operation_log_20261`（无前导零），但算法期望 `device_operation_log_202601`

**解决方案**：
```yaml
# ✅ 正确：显式列出所有带前导零的表名
actual-data-nodes: ds0.device_operation_log_202601,ds0.device_operation_log_202602,...,ds0.device_operation_log_202612

# ❌ 错误：ShardingSphere 4.1.1 不支持复杂的 Groovy 格式化
actual-data-nodes: ds0.device_operation_log_2026$->{1..12}.collect{String.format('%02d',it)}
```

### 7.2 参数传递丢失问题

**问题现象**：
- Controller 传递了 `startTime` 和 `endTime`，但分片算法只接收到 `startTime`

**解决方案**：
- 使用 MyBatis XML 映射文件代替 `@Select` 注解
- 确保使用 `@Param` 注解明确参数名称

### 7.3 LocalDateTime 类型转换异常

**问题现象**：
```
Error setting parameters on PreparedStatement
```

**解决方案**：
- 实现自定义 `TypeHandler` 处理 LocalDateTime 转换
- 在 XML 映射中指定 `typeHandler`

---

## 8. 性能优化建议

### 8.1 索引优化
```sql
-- 为分片列创建索引
ALTER TABLE device_operation_log_202601 ADD INDEX idx_operation_time (operation_time);
-- ... 为所有分表添加相同索引
```

### 8.2 连接池配置
```yaml
hikari:
  minimum-idle: 5          # 最小空闲连接
  maximum-pool-size: 20    # 最大连接数（根据并发量调整）
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

### 8.3 SQL 监控
```yaml
spring:
  shardingsphere:
    props:
      sql:
        show: true  # 开发环境开启，生产环境关闭
```

---

## 9. 最佳实践总结

### 9.1 配置规范
1. ✅ `actual-data-nodes` 必须显式列出所有表名（带前导零）
2. ✅ 分片列必须是数据库中的实际字段名
3. ✅ 精确分片和范围分片算法类可以复用同一个类
4. ✅ 开发环境开启 SQL 显示便于调试

### 9.2 代码规范
1. ✅ 使用 MyBatis XML 映射文件管理复杂 SQL
2. ✅ 分片算法中添加详细日志便于问题排查
3. ✅ 范围计算时严格处理边界条件（月初、月末）
4. ✅ Controller 层统一进行参数校验和异常处理

### 9.3 测试规范
1. ✅ 精确查询测试：验证单表路由
2. ✅ 范围查询测试：验证多表路由
3. ✅ 边界值测试：验证月初、月末、跨年份查询
4. ✅ 性能测试：验证大数据量下的查询性能

---

## 10. 参考资料

- [ShardingSphere 官方文档](https://shardingsphere.apache.org/document/4.1.1/cn/)
- [ShardingSphere-JDBC 用户手册](https://shardingsphere.apache.org/document/4.1.1/cn/manual/sharding-jdbc/)
- [MyBatis 官方文档](https://mybatis.org/mybatis-3/zh/index.html)
- [Spring Boot 与 ShardingSphere 整合最佳实践](https://spring.io/projects/spring-boot)

---

## 附录：完整项目结构

```
interview-provider/
├── src/main/java/
│   └── cn.itzixiao.interview.provider/
│       ├── config/
│       │   └── sharding/
│       │       └── DeviceOperationLogMonthShardingAlgorithm.java  # 分片算法
│       ├── controller/
│       │   └── ShardingTestController.java                        # 测试接口
│       ├── entity/
│       │   └── DeviceOperationLog.java                            # 实体类
│       └── mapper/
│           └── DeviceOperationLogMapper.java                      # Mapper 接口
├── src/main/resources/
│   ├── application-dev.yml                                        # 分片配置
│   └── mapper/
│       └── DeviceOperationLogMapper.xml                           # MyBatis 映射
└── pom.xml                                                        # Maven 依赖
```

---

**文档版本**：v1.0  
**最后更新**：2026-03-12  
**维护者**：interview-team
