package cn.itzixiao.interview.transaction.seata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Seata AT 模式原理演示
 * <p>
 * AT 模式（Automatic Transaction）- 自动补偿事务
 * <p>
 * 执行流程：
 * 1. 一阶段：
 * - 解析 SQL，找出更新前后的数据
 * - 保存 before image 和 after image 到 undo_log
 * - 提交本地事务
 * - 返回成功给 TC
 * <p>
 * 2. 二阶段（提交）：
 * - 异步批量删除 undo_log
 * - 性能影响小
 * <p>
 * 3. 二阶段（回滚）：
 * - 根据 undo_log 生成反向 SQL
 * - 恢复数据到 before image 状态
 * - 提交本地事务
 * <p>
 * 优点：
 * - 无侵入，业务代码无需修改
 * - 支持大多数 SQL 操作
 * - 性能较好
 * <p>
 * 缺点：
 * - 需要全局锁（可能产生死锁）
 * - 隔离性依赖数据库
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Service
public class SeataAtDemo {

    /**
     * AT 模式示例 - 订单扣减库存
     * <p>
     * 场景：用户下单，需要扣减库存、创建订单、扣减余额
     */
    public void atModeExample() {
        log.info("========== Seata AT 模式示例 ==========");

        // 1. 开启全局事务（@GlobalTransactional）
        log.info("1. TM 开启全局事务，XID: 123456");

        // 2. 一阶段执行 - 各微服务本地事务
        log.info("\n2. 一阶段执行：");
        log.info("   - 订单服务：创建订单，生成 undo_log");
        log.info("   - 库存服务：扣减库存，生成 undo_log");
        log.info("   - 账户服务：扣减余额，生成 undo_log");

        // 3. 一阶段提交
        log.info("\n3. 一阶段提交：所有分支事务提交，释放本地锁");

        // 4. 二阶段提交（成功时）
        log.info("\n4. 二阶段提交（成功场景）：");
        log.info("   - TC 收集所有分支成功");
        log.info("   - 发送提交指令");
        log.info("   - 异步删除 undo_log");

        // 5. 二阶段回滚（失败时）
        log.info("\n5. 二阶段回滚（失败场景）：");
        log.info("   - TC 检测到某个分支失败");
        log.info("   - 发送回滚指令");
        log.info("   - 根据 undo_log 生成反向 SQL");
        log.info("   - 恢复数据：库存 +1，余额 +100");

        log.info("\n========== AT 模式特点 ==========");
        log.info("✅ 优点：无侵入、性能好、支持 SQL 多");
        log.info("❌ 缺点：有全局锁、隔离性依赖 DB");
        log.info("==============================\n");
    }

    /**
     * undo_log 表结构说明
     */
    public void showUndoLogStructure() {
        log.info("===== undo_log 表结构 =====");
        log.info("CREATE TABLE `undo_log` (");
        log.info("  `id` BIGINT NOT NULL AUTO_INCREMENT,");
        log.info("  `branch_id` BIGINT NOT NULL,");
        log.info("  `xid` VARCHAR(100) NOT NULL,");
        log.info("  `context` VARCHAR(128) NOT NULL,");
        log.info("  `rollback_info` LONGBLOB NOT NULL,  -- before/after image");
        log.info("  `log_status` INT NOT NULL,");
        log.info("  `log_created` DATETIME NOT NULL,");
        log.info("  `log_modified` DATETIME NOT NULL,");
        log.info("  PRIMARY KEY (`id`),");
        log.info("  UNIQUE KEY `ux_id_branch_xid` (`branch_id`, `xid`)");
        log.info(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        log.info("\nrollback_info JSON 结构：");
        log.info("{");
        log.info("    \"beforeImage\": {");
        log.info("        \"rows\": [{\"id\": 1, \"stock\": 100}]");
        log.info("    },");
        log.info("    \"afterImage\": {");
        log.info("        \"rows\": [{\"id\": 1, \"stock\": 99}]");
        log.info("    }");
        log.info("}");
    }
}
