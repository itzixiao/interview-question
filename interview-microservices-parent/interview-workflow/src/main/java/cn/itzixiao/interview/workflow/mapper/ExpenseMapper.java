package cn.itzixiao.interview.workflow.mapper;

import cn.itzixiao.interview.workflow.entity.Expense;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报销 Mapper
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Mapper
public interface ExpenseMapper extends BaseMapper<Expense> {
}
