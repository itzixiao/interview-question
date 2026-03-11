package cn.itzixiao.interview.provider.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备运行日志实体类
 * 
 * 用于 Excel 导出测试
 */
@Data
@TableName("device_operation_log")
public class DeviceOperationLog {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @ExcelProperty("主键 ID")
    private Long id;

    /**
     * 设备编号（索引字段，模拟查询条件）
     */
    @ExcelProperty("设备编号")
    private String deviceCode;

    /**
     * 设备名称
     */
    @ExcelProperty("设备名称")
    private String deviceName;

    /**
     * 操作类型：1-开机 2-关机 3-故障 4-维护
     */
    @ExcelProperty("操作类型")
    private Integer operationType;

    /**
     * 操作关联数值（如温度、电压）
     */
    @ExcelProperty("操作关联数值")
    private BigDecimal operationValue;

    /**
     * 操作时间
     */
    @ExcelProperty("操作时间")
    private LocalDateTime operationTime;

    /**
     * 操作人
     */
    @ExcelProperty("操作人")
    private String operator;

    /**
     * 备注
     */
    @ExcelProperty("备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
