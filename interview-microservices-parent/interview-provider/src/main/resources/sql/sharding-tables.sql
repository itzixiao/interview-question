-- =============================================
-- ShardingSphere 按月分表 - 表结构初始化脚本
-- 说明：创建 2026 年 1-12 月的设备日志分表
-- =============================================

-- 原始表（逻辑表，实际不使用）
CREATE TABLE IF NOT EXISTS `device_operation_log`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表（逻辑表）';

-- 2026 年 1 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202601`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 1 月';

-- 2026 年 2 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202602`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 2 月';

-- 2026 年 3 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202603`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 3 月';

-- 2026 年 4 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202604`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 4 月';

-- 2026 年 5 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202605`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 5 月';

-- 2026 年 6 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202606`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 6 月';

-- 2026 年 7 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202607`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 7 月';

-- 2026 年 8 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202608`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 8 月';

-- 2026 年 9 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202609`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 9 月';

-- 2026 年 10 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202610`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 10 月';

-- 2026 年 11 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202611`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 11 月';

-- 2026 年 12 月表
CREATE TABLE IF NOT EXISTS `device_operation_log_202612`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `device_code`     VARCHAR(50) NOT NULL COMMENT '设备编号',
    `device_name`     VARCHAR(100)   DEFAULT NULL COMMENT '设备名称',
    `operation_type`  TINYINT     NOT NULL COMMENT '操作类型：1-开机 2-关机 3-故障 4-维护',
    `operation_value` DECIMAL(10, 2) DEFAULT NULL COMMENT '操作关联数值（如温度、电压）',
    `operation_time`  DATETIME    NOT NULL COMMENT '操作时间',
    `operator`        VARCHAR(50)    DEFAULT NULL COMMENT '操作人',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_time` (`operation_time`) USING BTREE,
    INDEX `idx_device_code` (`device_code`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='设备运行日志表 - 2026 年 12 月';
