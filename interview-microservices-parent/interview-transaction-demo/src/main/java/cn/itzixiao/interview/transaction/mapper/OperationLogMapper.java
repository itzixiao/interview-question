package cn.itzixiao.interview.transaction.mapper;

import cn.itzixiao.interview.transaction.entity.OperationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper 接口
 * 
 * @author itzixiao
 * @since 2026-03-13
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
