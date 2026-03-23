package cn.itzixiao.interview.provider.controller.sharding;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.provider.entity.DeviceOperationLog;
import cn.itzixiao.interview.provider.entity.es.DeviceOperationLogES;
import cn.itzixiao.interview.provider.mapper.DeviceOperationLogMapper;
import cn.itzixiao.interview.provider.repository.DeviceOperationLogESRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ShardingSphere 分片测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/sharding/test")
public class ShardingTestController {

    @Resource
    private DeviceOperationLogMapper deviceOperationLogMapper;

    @Resource
    private DeviceOperationLogESRepository esRepository;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 测试精确查询 - 根据单个时间点查询
     * 应该只路由到单个月份表
     * <p>
     * 注意：ShardingSphere 4.1.1 不支持 ResultSet.getObject() with type，
     * 因此需要在 Controller 层进行结果转换
     */
    @GetMapping("/precise")
    public Result<List<DeviceOperationLog>> testPreciseQuery(
            @RequestParam String time) {
        LocalDateTime operationTime = LocalDateTime.parse(time);
        log.info("精确查询测试，operationTime={}", operationTime);

        // 查询指定时间范围的数据（ShardingSphere 会根据 operation_time 路由到对应的月份表）
        List<DeviceOperationLog> result = deviceOperationLogMapper.selectByTimeRange(operationTime, operationTime.plusHours(23).plusMinutes(59).plusSeconds(59));
        return Result.success(result);
    }

    /**
     * 测试范围查询 - 根据时间范围查询
     * 应该路由到多个月份表
     */
    @GetMapping("/range")
    public Result<List<DeviceOperationLog>> testRangeQuery(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.parse(endTime);
        log.info("范围查询测试，startTime={}, endTime={}", start, end);

        List<DeviceOperationLog> result = deviceOperationLogMapper.selectByTimeRange(start, end);
        return Result.success(result);
    }

    /**
     * 测试按设备 + 时间范围查询
     * 测试多条件组合查询
     */
    @GetMapping("/device-range")
    public Result<List<DeviceOperationLog>> testDeviceRangeQuery(
            @RequestParam String deviceCode,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.parse(endTime);
        log.info("设备 + 时间范围查询，deviceCode={}, startTime={}, endTime={}", deviceCode, start, end);

        List<DeviceOperationLog> result = deviceOperationLogMapper.selectByDeviceAndTimeRange(deviceCode, start, end);
        return Result.success(result);
    }

    /**
     * 测试全年数据查询
     * 应该路由到所有 12 张表
     */
    @GetMapping("/all-year")
    public Result<List<DeviceOperationLog>> testAllYearData() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
        log.info("全年数据查询，startTime={}, endTime={}", start, end);

        List<DeviceOperationLog> result = deviceOperationLogMapper.selectByTimeRange(start, end);
        return Result.success(result);
    }

    /**
     * 测试新增数据
     * ShardingSphere 会根据 operation_time 自动路由到对应的月份表
     */
    @PostMapping("/insert")
    public Result<Map<String, Object>> testInsert(
            @RequestParam String deviceCode,
            @RequestParam String deviceName,
            @RequestParam Integer operationType,
            @RequestParam String operationTime,
            @RequestParam(required = false) Double operationValue,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String remark) {

        log.info("【分片测试】接收到新增请求：deviceCode={}, deviceName={}, operationType={}, operationTime={}",
                deviceCode, deviceName, operationType, operationTime);

        try {
            // 从 Redis 获取全局唯一 ID
            String redisKey = "global:id:device_operation_log";
            long uniqueId = redisTemplate.opsForValue().increment(redisKey);
            log.info("【分片测试】从 Redis 获取唯一 ID: {}", uniqueId);

            // 创建实体对象
            DeviceOperationLog logEntity = new DeviceOperationLog();
            logEntity.setId(uniqueId);  // 设置全局唯一 ID
            logEntity.setDeviceCode(deviceCode);
            logEntity.setDeviceName(deviceName);
            logEntity.setOperationType(operationType);
            // 将 ISO 8601 字符串转换为 LocalDateTime
            logEntity.setOperationTime(LocalDateTime.parse(operationTime));
            if (operationValue != null) {
                logEntity.setOperationValue(java.math.BigDecimal.valueOf(operationValue));
            }
            logEntity.setOperator(operator);
            logEntity.setRemark(remark);

            // 插入数据库（ShardingSphere 会自动路由）
            int rows = deviceOperationLogMapper.insert(logEntity);

            log.info("【分片测试】插入 MySQL 成功，影响行数：{}, 生成的 ID: {}", rows, logEntity.getId());

            // 同步到 Elasticsearch
            try {
                DeviceOperationLogES esEntity = new DeviceOperationLogES();
                BeanUtils.copyProperties(logEntity, esEntity);
                // ES 也使用相同的全局唯一 ID
                esEntity.setId(uniqueId);
                esRepository.save(esEntity);
                log.info("【分片测试】同步到 ES 成功，ID: {}", uniqueId);
            } catch (Exception e) {
                log.error("【分片测试】同步到 ES 失败", e);
                // ES 失败不影响主流程，仅记录日志
            }

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("message", "插入成功");
            result.put("id", logEntity.getId());
            result.put("targetTable", getTargetTableName(logEntity.getOperationTime()));
            result.put("rows", rows);

            return Result.success(result);
        } catch (Exception e) {
            log.error("【分片测试】插入失败", e);
            return Result.error("插入失败：" + e.getMessage());
        }
    }

    /**
     * 根据操作时间计算目标表名
     */
    private String getTargetTableName(LocalDateTime operationTime) {
        if (operationTime == null) {
            return "unknown";
        }
        int year = operationTime.getYear();
        int month = operationTime.getMonthValue();
        return String.format("device_operation_log_%04d%02d", year, month);
    }
}
