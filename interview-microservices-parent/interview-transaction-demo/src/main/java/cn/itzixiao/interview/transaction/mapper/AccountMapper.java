package cn.itzixiao.interview.transaction.mapper;

import cn.itzixiao.interview.transaction.entity.Account;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户 Mapper 接口
 *
 * @author itzixiao
 * @since 2026-03-13
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
