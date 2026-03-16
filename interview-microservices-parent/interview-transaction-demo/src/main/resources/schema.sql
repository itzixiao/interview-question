-- Spring事务传播机制演示 - 数据库表结构
-- 作者：itzixiao
-- 创建时间：2026-03-13

CREATE DATABASE IF NOT EXISTS `interview` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `interview`;

-- 账户表
DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account`
(
    `id`          BIGINT(20)     NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `user_id`     BIGINT(20)     NOT NULL COMMENT '用户 ID',
    `balance`     DECIMAL(10, 2) NOT NULL DEFAULT '0.00' COMMENT '余额',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='账户表';

-- 订单表
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order`
(
    `id`          BIGINT(20)     NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `order_no`    VARCHAR(64)    NOT NULL COMMENT '订单号',
    `user_id`     BIGINT(20)     NOT NULL COMMENT '用户 ID',
    `amount`      DECIMAL(10, 2) NOT NULL DEFAULT '0.00' COMMENT '订单金额',
    `status`      TINYINT(4)     NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已取消',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';

-- 库存表
DROP TABLE IF EXISTS `t_inventory`;
CREATE TABLE `t_inventory`
(
    `id`          BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `product_id`  BIGINT(20) NOT NULL COMMENT '商品 ID',
    `stock`       INT(11)    NOT NULL DEFAULT '0' COMMENT '库存数量',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_id` (`product_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='库存表';

-- 操作日志表（用于演示 REQUIRES_NEW）
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log`
(
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `operation`   VARCHAR(100) NOT NULL COMMENT '操作类型',
    `detail`      VARCHAR(500) NOT NULL COMMENT '操作详情',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS-成功，FAIL-失败',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志表';

-- 初始化测试数据
INSERT INTO `t_account` (`user_id`, `balance`)
VALUES (1, 1000.00),
       (2, 2000.00),
       (3, 3000.00);

INSERT INTO `t_inventory` (`product_id`, `stock`)
VALUES (1, 100),
       (2, 200),
       (3, 300);

-- 查看初始数据
SELECT '=== 账户初始数据 ===' AS info;
SELECT *
FROM t_account;

SELECT '=== 库存初始数据 ===' AS info;
SELECT *
FROM t_inventory;
