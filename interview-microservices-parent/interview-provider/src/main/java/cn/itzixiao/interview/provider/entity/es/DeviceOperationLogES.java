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
     * 设备编号（索引字段，模拟查询条件）
     */
    @Field(type = FieldType.Keyword)
    private String deviceCode;

    /**
     * 设备名称
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
