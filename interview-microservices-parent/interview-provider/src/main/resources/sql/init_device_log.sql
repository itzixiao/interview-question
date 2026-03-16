-- =============================================
-- 设备运行日志表初始化脚本
-- 用于 Excel 导出测试（大数据量场景）
-- =============================================

-- 1. 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `interview_demo` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `interview_demo`;

-- 2. 创建表结构
DROP TABLE IF EXISTS `device_operation_log`;

CREATE TABLE `device_operation_log`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(32)     NOT NULL COMMENT '设备编号（索引字段，模拟查询条件）',
    `device_name`     VARCHAR(64)     NOT NULL COMMENT '设备名称',
    `operation_type`  TINYINT         NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME        NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(32)    DEFAULT 'system' COMMENT '操作人',
    `remark`          VARCHAR(255)   DEFAULT '' COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    -- 加索引模拟实际查询场景，导出时更贴近真实业务
    INDEX `idx_device_code` (`device_code`),
    INDEX `idx_operation_time` (`operation_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志（大数据量导出测试表）';

-- 3. 插入测试数据（生成 1000 条数据用于测试）
-- 说明：可以根据需要调整数据量，测试不同场景

DELIMITER $$

-- 创建存储过程批量插入数据
CREATE PROCEDURE insert_test_data(IN num_records INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_device_code VARCHAR(32);
    DECLARE v_device_name VARCHAR(64);
    DECLARE v_operation_type TINYINT;
    DECLARE v_operation_value DECIMAL(10, 2);
    DECLARE v_operation_time DATETIME;
    DECLARE v_operator VARCHAR(32);

    WHILE i <= num_records
        DO
            -- 生成设备编号（DEVICE-001 到 DEVICE-050）
            SET v_device_code = CONCAT('DEVICE-', LPAD(MOD(i, 50) + 1, 3, '0'));

            -- 生成设备名称
            SET v_device_name = CONCAT(v_device_code, '-测试设备');

            -- 随机操作类型（1-开机，2-关机，3-故障，4-维护）
            SET v_operation_type = MOD(i, 4) + 1;

            -- 随机操作数值（20.00 到 100.00）
            SET v_operation_value = ROUND(20 + RAND() * 80, 2);

            -- 生成操作时间（过去 30 天内）
            SET v_operation_time = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY);
            SET v_operation_time = DATE_ADD(v_operation_time, INTERVAL FLOOR(RAND() * 24) HOUR);
            SET v_operation_time = DATE_ADD(v_operation_time, INTERVAL FLOOR(RAND() * 60) MINUTE);

            -- 随机操作人
            CASE MOD(i, 5)
                WHEN 0 THEN SET v_operator = '张三';
                WHEN 1 THEN SET v_operator = '李四';
                WHEN 2 THEN SET v_operator = '王五';
                WHEN 3 THEN SET v_operator = '赵六';
                ELSE SET v_operator = 'system';
                END CASE;

            -- 插入数据
            INSERT INTO device_operation_log
            (device_code, device_name, operation_type, operation_value, operation_time, operator, remark)
            VALUES (v_device_code, v_device_name, v_operation_type, v_operation_value, v_operation_time, v_operator,
                    CONCAT('测试数据-', i));

            SET i = i + 1;
        END WHILE;
END$$

DELIMITER ;

-- 调用存储过程插入 1000 条测试数据
CALL insert_test_data(1000);

-- 4. 验证数据
SELECT COUNT(*) AS total_records
FROM device_operation_log;

-- 查看前 10 条数据
SELECT *
FROM device_operation_log
LIMIT 10;

-- 查看不同操作类型的分布
SELECT operation_type, COUNT(*) AS count
FROM device_operation_log
GROUP BY operation_type;

-- 查看时间范围
SELECT MIN(operation_time) AS earliest_time,
       MAX(operation_time) AS latest_time,
       COUNT(*)            AS total_count
FROM device_operation_log;

-- 5. 测试索引是否生效（使用 EXPLAIN 查看执行计划）
-- 测试全表扫描
EXPLAIN
SELECT *
FROM device_operation_log;

-- 测试时间范围查询（应该走 idx_operation_time 索引）
EXPLAIN
SELECT *
FROM device_operation_log
WHERE operation_time > DATE_SUB(NOW(), INTERVAL 7 DAY);

-- 测试设备编号查询（应该走 idx_device_code 索引）
EXPLAIN
SELECT *
FROM device_operation_log
WHERE device_code = 'DEVICE-001';

-- 6. 清理存储过程（可选）
DROP PROCEDURE IF EXISTS insert_test_data;

-- 完成提示
SELECT '数据初始化完成！可以开始测试 Excel 导出功能。' AS status;
