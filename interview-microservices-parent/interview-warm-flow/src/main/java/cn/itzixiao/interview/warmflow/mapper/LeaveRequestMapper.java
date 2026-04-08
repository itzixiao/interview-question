package cn.itzixiao.interview.warmflow.mapper;

import cn.itzixiao.interview.warmflow.entity.LeaveRequest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 请假申请 Mapper
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {
}
