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
-- 请假审批流程定义初始化数据
-- ========================================
-- 注意：以下数据需要在框架启动并自动建表后执行
-- 或者通过官方设计器UI创建（推荐方式）

-- 1. 插入流程定义 (leave_approval)
INSERT INTO `flow_definition` (`id`, `flow_code`, `flow_name`, `model_value`, `category`, `version`, `is_publish`, `form_custom`, `form_path`, `activity_status`, `listener_type`, `listener_path`, `ext`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`) 
VALUES 
(1001, 'leave_approval', '请假审批流程', 'CLASSICS', 'business', '1', 1, 'N', NULL, 1, NULL, NULL, NULL, NOW(), 'system', NOW(), 'system', '0', NULL);

-- 2. 插入流程节点
-- 开始节点（不需要node_ratio）
INSERT INTO `flow_node` (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `handler_type`, `handler_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`) 
VALUES 
(1001, 0, 1001, 'start', '开始', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'N', NULL, '1', NOW(), 'system', NOW(), 'system', NULL, '0', NULL);

-- 一级审批节点（部门经理）- 审批人ID: 2001，签署比例100%
INSERT INTO `flow_node` (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `handler_type`, `handler_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`) 
VALUES 
(2001, 1, 1001, 'dept_manager', '部门经理审批', '2001', 100.000, NULL, NULL, NULL, NULL, NULL, NULL, 'N', NULL, '1', NOW(), 'system', NOW(), 'system', NULL, '0', NULL);

-- 二级审批节点（HR）- 审批人ID: 3001，签署比例100%
INSERT INTO `flow_node` (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `handler_type`, `handler_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`) 
VALUES 
(3001, 1, 1001, 'hr_approval', 'HR审批', '3001', 100.000, NULL, NULL, NULL, NULL, NULL, NULL, 'N', NULL, '1', NOW(), 'system', NOW(), 'system', NULL, '0', NULL);

-- 结束节点（不需要node_ratio）
INSERT INTO `flow_node` (`id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`, `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `handler_type`, `handler_path`, `form_custom`, `form_path`, `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`) 
VALUES 
(4001, 2, 1001, 'end', '结束', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'N', NULL, '1', NOW(), 'system', NOW(), 'system', NULL, '0', NULL);

-- 3. 插入流程流转关系
-- 开始 -> 一级审批（部门经理）
INSERT INTO `flow_skip` (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`) 
VALUES 
(1001, 1001, 'start', 0, 'dept_manager', 1, '提交申请', NULL, NULL, NULL, NOW(), 'system', NOW(), 'system', '0', NULL);

-- 一级审批 -> 二级审批（HR）
INSERT INTO `flow_skip` (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`) 
VALUES 
(2001, 1001, 'dept_manager', 1, 'hr_approval', 1, '经理通过', 'PASS', NULL, NULL, NOW(), 'system', NOW(), 'system', '0', NULL);

-- 二级审批 -> 结束
INSERT INTO `flow_skip` (`id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`, `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`) 
VALUES 
(3001, 1001, 'hr_approval', 1, 'end', 2, 'HR通过', 'PASS', NULL, NULL, NOW(), 'system', NOW(), 'system', '0', NULL);
