-- ========================================
-- Warm-Flow 工作流引擎数据库初始化脚本
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS interview_warm_flow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE interview_warm_flow;

-- ========================================
-- 业务表
-- ========================================

-- 请假申请表
CREATE TABLE IF NOT EXISTS `leave_request` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `user_name` VARCHAR(100) NOT NULL COMMENT '申请人姓名',
    `leave_type` INT NOT NULL COMMENT '请假类型：1-事假 2-病假 3-年假 4-婚假 5-产假',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `days` DOUBLE NOT NULL COMMENT '请假天数',
    `reason` VARCHAR(1000) COMMENT '请假原因',
    `flow_instance_id` VARCHAR(100) COMMENT '流程实例ID',
    `status` INT NOT NULL DEFAULT 0 COMMENT '审批状态：0-草稿 1-审批中 2-已通过 3-已驳回 4-已撤销',
    `current_node` VARCHAR(100) COMMENT '当前审批节点',
    `approval_comment` VARCHAR(1000) COMMENT '审批意见',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_flow_instance_id` (`flow_instance_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请表';

-- 报销申请表
CREATE TABLE IF NOT EXISTS `reimbursement_request` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `user_name` VARCHAR(100) NOT NULL COMMENT '申请人姓名',
    `reimbursement_type` INT NOT NULL COMMENT '报销类型：1-差旅费 2-交通费 3-餐饮费 4-办公用品 5-其他',
    `amount` DECIMAL(10, 2) NOT NULL COMMENT '报销金额',
    `reason` VARCHAR(1000) COMMENT '报销事由',
    `attachment_urls` TEXT COMMENT '发票附件URL',
    `flow_instance_id` VARCHAR(100) COMMENT '流程实例ID',
    `status` INT NOT NULL DEFAULT 0 COMMENT '审批状态：0-草稿 1-审批中 2-已通过 3-已驳回 4-已撤销',
    `current_node` VARCHAR(100) COMMENT '当前审批节点',
    `approval_comment` VARCHAR(1000) COMMENT '审批意见',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_flow_instance_id` (`flow_instance_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报销申请表';

-- ========================================
-- Warm-Flow 工作流引擎表（由框架自动创建，这里仅作为参考）
-- ========================================

-- 流程定义表
-- flow_definition

-- 流程节点表
-- flow_node

-- 流程流转表
-- flow_skip

-- 流程条件表达式表
-- flow_condition

-- 流程实例表
-- flow_instance

-- 流程任务表
-- flow_task

-- 流程变量表
-- flow_variable

-- ========================================
-- 测试数据
-- ========================================

-- 插入测试请假申请数据
INSERT INTO `leave_request` (`user_id`, `user_name`, `leave_type`, `start_time`, `end_time`, `days`, `reason`, `status`) 
VALUES 
(1001, '张三', 3, '2026-04-10 09:00:00', '2026-04-12 18:00:00', 3.0, '年假休息', 0),
(1002, '李四', 1, '2026-04-15 09:00:00', '2026-04-15 18:00:00', 1.0, '处理个人事务', 0);

-- 插入测试报销申请数据
INSERT INTO `reimbursement_request` (`user_id`, `user_name`, `reimbursement_type`, `amount`, `reason`, `status`) 
VALUES 
(1001, '张三', 1, 2500.00, '北京出差差旅费', 0),
(1003, '王五', 3, 350.00, '客户招待餐费', 0);
