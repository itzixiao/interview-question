package cn.itzixiao.interview.transaction.mapper;

import cn.itzixiao.interview.transaction.entity.Inventory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存 Mapper 接口
 *
 * @author itzixiao
 * @since 2026-03-13
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {
}
