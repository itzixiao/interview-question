package cn.itzixiao.interview.provider.mapper;

import cn.itzixiao.interview.provider.entity.DeviceOperationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备运行日志 Mapper
 */
@Mapper
public interface DeviceOperationLogMapper extends BaseMapper<DeviceOperationLog> {

    /**
     * 查询全部数据（用于测试 OOM）
     * 
     * @return 所有设备日志
     */
    @Select("SELECT * FROM device_operation_log")
    List<DeviceOperationLog> selectAll();

    /**
     * 按时间范围查询（测试索引是否生效）
     * 
     * @param startTime 开始时间
     * @return 指定时间后的日志记录
     */
    @Select("SELECT * FROM device_operation_log WHERE operation_time > #{startTime,typeHandler=cn.itzixiao.interview.provider.config.handler.CustomLocalDateTimeTypeHandler} ORDER BY operation_time ASC")
    List<DeviceOperationLog> selectByTimeRange(LocalDateTime startTime);

    /**
     * 分页查询（对比内存占用）
     * 
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 分页数据
     */
    @Select("SELECT * FROM device_operation_log LIMIT #{offset}, #{limit}")
    List<DeviceOperationLog> selectPage(@org.apache.ibatis.annotations.Param("offset") int offset, 
                                        @org.apache.ibatis.annotations.Param("limit") int limit);

    /**
     * 按时间范围查询（测试分片路由）
     * ShardingSphere 会根据时间范围自动路由到对应的月份表
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指定时间范围内的日志记录
     */
    List<DeviceOperationLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 按设备编号和时间范围查询（测试多条件分片）
     * 
     * @param deviceCode 设备编号
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指定设备和时间范围内的日志记录
     */
    List<DeviceOperationLog> selectByDeviceAndTimeRange(@Param("deviceCode") String deviceCode,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);
}
