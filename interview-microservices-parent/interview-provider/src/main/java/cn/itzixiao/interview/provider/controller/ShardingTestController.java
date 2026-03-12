package cn.itzixiao.interview.provider.controller;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.provider.entity.DeviceOperationLog;
import cn.itzixiao.interview.provider.mapper.DeviceOperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ShardingSphere 分片测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/sharding/test")
public class ShardingTestController {
    
    @Resource
    private DeviceOperationLogMapper deviceOperationLogMapper;
    
    /**
     * 测试精确查询 - 根据单个时间点查询
     * 应该只路由到单个月份表
     * 
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
}
