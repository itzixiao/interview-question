package cn.itzixiao.interview.transaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 *
 * @author itzixiao
 * @since 2026-03-13
 */
@Data
@TableName("t_operation_log")
public class OperationLog {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 操作详情
     */
    private String detail;

    /**
     * 状态：SUCCESS-成功，FAIL-失败
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
