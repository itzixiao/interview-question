-- =======================================================
-- 工作流审批系统数据库初始化脚本
-- 数据库：workflow_db
-- =======================================================

CREATE DATABASE IF NOT EXISTS workflow_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE workflow_db;

-- =======================================================
-- RBAC 系统表
-- =======================================================

-- 部门表
CREATE TABLE IF NOT EXISTS sys_dept
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    dept_name   VARCHAR(64) NOT NULL COMMENT '部门名称',
    parent_id   BIGINT   DEFAULT 0 COMMENT '父部门ID，0表示顶级',
    manager_id  BIGINT   DEFAULT NULL COMMENT '部门经理用户ID',
    sort        INT      DEFAULT 0 COMMENT '排序',
    status      TINYINT  DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT  DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '部门表';

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt 加密）',
    real_name   VARCHAR(64)  NOT NULL COMMENT '真实姓名',
    email       VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    dept_id     BIGINT       DEFAULT NULL COMMENT '部门ID',
    status      TINYINT      DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    INDEX idx_username (username),
    INDEX idx_dept_id (dept_id)
) ENGINE = InnoDB COMMENT = '用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_code   VARCHAR(64) NOT NULL UNIQUE COMMENT '角色编码',
    role_name   VARCHAR(64) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    status      TINYINT      DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    permission_code VARCHAR(128) NOT NULL UNIQUE COMMENT '权限编码',
    permission_name VARCHAR(64)  NOT NULL COMMENT '权限名称',
    type            TINYINT  DEFAULT 1 COMMENT '类型：1-菜单，2-按钮，3-接口',
    parent_id       BIGINT   DEFAULT 0 COMMENT '父权限ID',
    sort            INT      DEFAULT 0 COMMENT '排序',
    status          TINYINT  DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT  DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '权限表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role
(
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB COMMENT = '用户角色关联表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission
(
    id            BIGINT NOT NULL AUTO_INCREMENT,
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE = InnoDB COMMENT = '角色权限关联表';

-- =======================================================
-- 业务表
-- =======================================================

-- 请假申请表
CREATE TABLE IF NOT EXISTS biz_leave
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    apply_no            VARCHAR(32)  NOT NULL UNIQUE COMMENT '申请编号',
    applicant_id        BIGINT       NOT NULL COMMENT '申请人ID',
    applicant_name      VARCHAR(64)  NOT NULL COMMENT '申请人姓名',
    dept_id             BIGINT       NOT NULL COMMENT '部门ID',
    dept_name           VARCHAR(64) DEFAULT NULL COMMENT '部门名称',
    leave_type          TINYINT      NOT NULL COMMENT '假期类型：1-年假，2-事假，3-病假，4-调休',
    start_date          DATE         NOT NULL COMMENT '开始日期',
    end_date            DATE         NOT NULL COMMENT '结束日期',
    leave_days          INT          NOT NULL COMMENT '请假天数',
    reason              VARCHAR(512) NOT NULL COMMENT '请假原因',
    process_instance_id VARCHAR(64) DEFAULT NULL COMMENT 'Flowable 流程实例ID',
    status              TINYINT     DEFAULT 0 COMMENT '审批状态：0-草稿，1-审批中，2-已通过，3-已拒绝，4-已撤回',
    current_node        VARCHAR(64) DEFAULT NULL COMMENT '当前审批节点',
    apply_time          DATETIME    DEFAULT NULL COMMENT '申请时间',
    create_time         DATETIME    DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT     DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_process_instance_id (process_instance_id)
) ENGINE = InnoDB COMMENT = '请假申请表';

-- 报销申请表
CREATE TABLE IF NOT EXISTS biz_expense
(
    id                  BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    apply_no            VARCHAR(32)    NOT NULL UNIQUE COMMENT '申请编号',
    applicant_id        BIGINT         NOT NULL COMMENT '申请人ID',
    applicant_name      VARCHAR(64)    NOT NULL COMMENT '申请人姓名',
    dept_id             BIGINT         NOT NULL COMMENT '部门ID',
    dept_name           VARCHAR(64) DEFAULT NULL COMMENT '部门名称',
    expense_type        TINYINT        NOT NULL COMMENT '报销类型：1-差旅，2-招待，3-办公，4-培训，5-其他',
    amount              DECIMAL(12, 2) NOT NULL COMMENT '报销金额',
    description         VARCHAR(512)   NOT NULL COMMENT '报销说明',
    attachments         TEXT        DEFAULT NULL COMMENT '附件URL（逗号分隔）',
    process_instance_id VARCHAR(64) DEFAULT NULL COMMENT 'Flowable 流程实例ID',
    status              TINYINT     DEFAULT 0 COMMENT '审批状态：0-草稿，1-审批中，2-已通过，3-已拒绝，4-已撤回',
    current_node        VARCHAR(64) DEFAULT NULL COMMENT '当前审批节点',
    apply_time          DATETIME    DEFAULT NULL COMMENT '申请时间',
    create_time         DATETIME    DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT     DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_process_instance_id (process_instance_id)
) ENGINE = InnoDB COMMENT = '报销申请表';

-- =======================================================
-- 初始化数据
-- =======================================================

-- 角色数据
INSERT INTO sys_role (role_code, role_name, description)
VALUES ('ADMIN', '系统管理员', '拥有所有权限'),
       ('EMPLOYEE', '普通员工', '可提交请假和报销申请'),
       ('DEPT_MANAGER', '部门经理', '可审批本部门员工的申请'),
       ('FINANCE_MANAGER', '财务经理', '可审批1000-5000元的报销申请'),
       ('GENERAL_MANAGER', '总经理', '可审批超过3天的请假和超过5000元的报销申请');

-- 部门数据
INSERT INTO sys_dept (dept_name, parent_id, sort)
VALUES ('总公司', 0, 1),
       ('技术部', 1, 1),
       ('财务部', 1, 2),
       ('人事部', 1, 3),
       ('销售部', 1, 4);

-- 用户数据（密码均为 123456，BCrypt 加密）
-- BCrypt of '123456': $2a$10$BiACCxsTlnbVij5lB9.l1erFvxFI9AMtUBjiTqRVUw6UGI0FWIc8i
INSERT INTO sys_user (username, password, real_name, email, dept_id, status)
VALUES ('admin', '$2a$10$BiACCxsTlnbVij5lB9.l1erFvxFI9AMtUBjiTqRVUw6UGI0FWIc8i', '系统管理员', 'admin@company.com', 1,
        1),
       ('gm_zhang', '$2a$10$BiACCxsTlnbVij5lB9.l1erFvxFI9AMtUBjiTqRVUw6UGI0FWIc8i', '张总', 'zhang@company.com', 1, 1),
       ('tech_manager_li', '$2a$10$BiACCxsTlnbVij5lB9.l1erFvxFI9AMtUBjiTqRVUw6UGI0FWIc8i', '李经理', 'li@company.com',
        2, 1),
       ('finance_manager_wang', '$2a$10$BiACCxsTlnbVij5lB9.l1erFvxFI9AMtUBjiTqRVUw6UGI0FWIc8i', '王财务',
        'wang@company.com', 3, 1),
       ('emp_chen', '$2a$10$BiACCxsTlnbVij5lB9.l1erFvxFI9AMtUBjiTqRVUw6UGI0FWIc8i', '陈员工', 'chen@company.com', 2, 1),
       ('emp_liu', '$2a$10$BiACCxsTlnbVij5lB9.l1erFvxFI9AMtUBjiTqRVUw6UGI0FWIc8i', '刘员工', 'liu@company.com', 4, 1);

-- 更新部门经理（注意：需要在用户数据插入后执行）
UPDATE sys_dept
SET manager_id = (SELECT id FROM sys_user WHERE username = 'admin')
WHERE dept_name = '总公司';
UPDATE sys_dept
SET manager_id = (SELECT id FROM sys_user WHERE username = 'tech_manager_li')
WHERE dept_name = '技术部';
UPDATE sys_dept
SET manager_id = (SELECT id FROM sys_user WHERE username = 'finance_manager_wang')
WHERE dept_name = '财务部';
UPDATE sys_dept
SET manager_id = (SELECT id FROM sys_user WHERE username = 'admin')
WHERE dept_name = '人事部';
UPDATE sys_dept
SET manager_id = (SELECT id FROM sys_user WHERE username = 'gm_zhang')
WHERE dept_name = '销售部';

-- 用户角色绑定
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u,
     sys_role r
WHERE u.username = 'admin'
  AND r.role_code = 'ADMIN';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u,
     sys_role r
WHERE u.username = 'gm_zhang'
  AND r.role_code = 'GENERAL_MANAGER';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u,
     sys_role r
WHERE u.username = 'tech_manager_li'
  AND r.role_code = 'DEPT_MANAGER';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u,
     sys_role r
WHERE u.username = 'finance_manager_wang'
  AND r.role_code = 'FINANCE_MANAGER';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u,
     sys_role r
WHERE u.username = 'emp_chen'
  AND r.role_code = 'EMPLOYEE';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u,
     sys_role r
WHERE u.username = 'emp_liu'
  AND r.role_code = 'EMPLOYEE';
