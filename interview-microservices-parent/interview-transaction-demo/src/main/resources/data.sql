-- Spring事务传播机制演示 - 初始化数据
-- 作者：itzixiao
-- 创建时间：2026-03-13

USE `interview`;

-- 清空现有数据（可选，用于重置测试环境）
-- DELETE FROM t_operation_log;
-- DELETE FROM t_order;
-- DELETE FROM t_inventory;
-- DELETE FROM t_account;

-- 插入测试账户数据
INSERT INTO `t_account` (`user_id`, `balance`) VALUES 
(1, 1000.00),
(2, 2000.00),
(3, 3000.00)
ON DUPLICATE KEY UPDATE 
    `balance` = VALUES(`balance`);

-- 插入测试库存数据
INSERT INTO `t_inventory` (`product_id`, `stock`) VALUES 
(1, 100),
(2, 200),
(3, 300)
ON DUPLICATE KEY UPDATE 
    `stock` = VALUES(`stock`);

-- 查看插入的数据
SELECT '=== 账户初始数据 ===' AS info;
SELECT * FROM t_account;

SELECT '=== 库存初始数据 ===' AS info;
SELECT * FROM t_inventory;

SELECT '=== 当前订单数量 ===' AS info;
SELECT COUNT(*) as order_count FROM t_order;

SELECT '=== 当前日志数量 ===' AS info;
SELECT COUNT(*) as log_count FROM t_operation_log;
