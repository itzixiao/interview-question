package cn.itzixiao.interview.provider.config.sharding;

import com.google.common.collect.Range;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 设备操作日志按月分片算法
 * 
 * 根据 operationTime 字段，将数据路由到对应的月份表
 * 例如：2026-03-15 -> device_operation_log_202603
 */
@Slf4j
public class DeviceOperationLogMonthShardingAlgorithm implements PreciseShardingAlgorithm<Comparable<?>>, RangeShardingAlgorithm<Comparable<?>> {
    
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    
    /**
     * 精确分片（用于 =、IN 查询）
     * 
     * @param availableTargetNames 所有可用的表名集合
     * @param shardingValue 分片值（包含分片列名和实际值）
     * @return 目标表名
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Comparable<?>> shardingValue) {
        LocalDateTime operationTime = (LocalDateTime) shardingValue.getValue();
        String targetTableName = getTargetTableName(operationTime);
        
        log.info("精确分片：operationTime={}, targetTable={}", operationTime, targetTableName);
        
        // 验证目标表是否存在于可用表集合中
        if (availableTargetNames.contains(targetTableName)) {
            return targetTableName;
        }
        
        throw new IllegalArgumentException(
            String.format("未找到对应的分表：%s, 可用表：%s", targetTableName, availableTargetNames)
        );
    }
    
    /**
     * 范围分片（用于 BETWEEN、>、< 等范围查询）
     * 
     * @param availableTargetNames 所有可用的表名集合
     * @param shardingValue 分片值（包含上下界）
     * @return 匹配的所有表名集合
     */
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Comparable<?>> shardingValue) {
        Range<Comparable<?>> valueRange = shardingValue.getValueRange();
        
        // 处理无界范围的情况
        LocalDateTime lowerBound = null;
        LocalDateTime upperBound = null;
        
        if (valueRange.hasLowerBound()) {
            lowerBound = (LocalDateTime) valueRange.lowerEndpoint();
        }
        
        if (valueRange.hasUpperBound()) {
            upperBound = (LocalDateTime) valueRange.upperEndpoint();
        }
        
        log.info("范围分片：startTime={}, endTime={}", lowerBound, upperBound);
        
        // 计算范围内涉及的所有月份表
        return calculateRangeTables(availableTargetNames, lowerBound, upperBound);
    }
    
    /**
     * 初始化方法
     */
    public void init(Properties props) {
        log.info("DeviceOperationLogMonthShardingAlgorithm 初始化完成，props={}", props);
    }
    
    /**
     * 获取目标表名
     * 
     * @param operationTime 操作时间
     * @return 表名（如：device_operation_log_202603）
     */
    private String getTargetTableName(LocalDateTime operationTime) {
        String monthSuffix = operationTime.format(MONTH_FORMATTER);
        return "device_operation_log_" + monthSuffix;
    }
    
    /**
     * 计算范围查询涉及的所有表
     * 
     * @param availableTargetNames 所有可用的表名
     * @param startTime 开始时间（null 表示无下界）
     * @param endTime 结束时间（null 表示无上界）
     * @return 匹配的表名集合
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
                    // 提取表名中的月份
                    String monthStr = tableName.substring("device_operation_log_".length());
                    try {
                        LocalDateTime tableTime = LocalDateTime.parse(monthStr + "01000000", 
                                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                        
                        // 检查下界（startTime 为 null 表示无下界限制）
                        boolean meetsLowerBound = (startTime == null) || 
                            !tableTime.isBefore(startTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
                        
                        // 检查上界（endTime 为 null 表示无上界限制）
                        boolean meetsUpperBound = (endTime == null) || 
                            !tableTime.isAfter(endTime.withDayOfMonth(
                                    endTime.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59));
                        
                        // 判断该月是否在查询范围内
                        return meetsLowerBound && meetsUpperBound;
                    } catch (Exception e) {
                        log.warn("解析表名失败：{}, error={}", tableName, e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }
}
