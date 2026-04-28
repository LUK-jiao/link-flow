-- ===========================================
-- LinkFlow 数据库初始化脚本
-- 创建时间：2026-04-28
-- ===========================================

CREATE DATABASE IF NOT EXISTS linkflow_db DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE linkflow_db;

-- ===========================================
-- 1. 用户模块（已存在，保留）
-- ===========================================

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER/APPROVER/ADMIN',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 审批人配置表
CREATE TABLE IF NOT EXISTS approver_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    campaign_type VARCHAR(50) NOT NULL COMMENT '活动类型',
    approver_user_id BIGINT NOT NULL COMMENT '审批人用户ID',
    approver_level INT NOT NULL DEFAULT 1 COMMENT '审批级别：1-一级审批，2-二级审批',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_campaign_type (campaign_type),
    INDEX idx_approver_user_id (approver_user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批人配置表';

-- ===========================================
-- 2. Campaign 模块
-- ===========================================

-- 活动表
CREATE TABLE IF NOT EXISTS campaign (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '活动ID',
    name VARCHAR(100) NOT NULL COMMENT '活动名称',
    description VARCHAR(500) COMMENT '活动描述',
    campaign_type VARCHAR(50) NOT NULL COMMENT '活动类型：MARKETING/PROMOTION/EVENT',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PENDING_APPROVAL/APPROVED/REJECTED/ACTIVE/PAUSED/COMPLETED/CANCELLED',
    creator_user_id BIGINT NOT NULL COMMENT '创建人用户ID',
    start_time DATETIME COMMENT '活动开始时间',
    end_time DATETIME COMMENT '活动结束时间',
    budget DECIMAL(12,2) COMMENT '预算',
    reject_reason VARCHAR(500) COMMENT '拒绝原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_creator_user_id (creator_user_id),
    INDEX idx_status (status),
    INDEX idx_campaign_type (campaign_type),
    INDEX idx_create_time (create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';


-- ===========================================
-- 3. Workflow 模块
-- ===========================================

-- 工作流实例表（业务层，Flowable表由引擎自动创建）
CREATE TABLE IF NOT EXISTS workflow_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实例ID',
    business_key VARCHAR(100) NOT NULL COMMENT '业务键（如campaign_id）',
    business_type VARCHAR(50) NOT NULL COMMENT '业务类型：CAMPAIGN_APPROVAL/OTHER',
    process_instance_id VARCHAR(64) COMMENT 'Flowable流程实例ID',
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING/COMPLETED/CANCELLED/TERMINATED',
    initiator_id BIGINT NOT NULL COMMENT '发起人ID',
    current_assignee VARCHAR(100) COMMENT '当前处理人',
    start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_business_key (business_key, business_type),
    INDEX idx_process_instance_id (process_instance_id),
    INDEX idx_status (status),
    INDEX idx_initiator_id (initiator_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流实例表';

-- 审批记录表
CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    campaign_id BIGINT NOT NULL COMMENT '活动ID',
    workflow_instance_id BIGINT NOT NULL COMMENT '工作流实例ID',
    task_id VARCHAR(64) COMMENT 'Flowable任务ID',
    task_name VARCHAR(100) COMMENT '任务名称',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approver_name VARCHAR(50) COMMENT '审批人姓名',
    action VARCHAR(20) NOT NULL COMMENT '动作：APPROVE/REJECT/CANCEL/DELEGATE',
    comment VARCHAR(500) COMMENT '审批意见',
    approve_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_workflow_instance_id (workflow_instance_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_approve_time (approve_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- ===========================================
-- 4. Flowable 引擎表（由 Flowable 自动创建）
-- ===========================================
-- 如需手动初始化，可在 application.yml 配置：
-- spring.flowable.database-schema-update: true
-- 或执行 Flowable 官方 SQL 脚本

-- ===========================================
-- 5. 初始化数据
-- ===========================================

-- 插入默认管理员
INSERT INTO user (username, password, email, role, status) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6V.Sa', 'admin@linkflow.com', 'ADMIN', 1)
    ON DUPLICATE KEY UPDATE username = username;

-- 插入默认审批人配置
INSERT INTO approver_config (campaign_type, approver_id, approver_level) VALUES
                                                                                     ('MARKETING', 1, 1),
                                                                                     ('PROMOTION', 1, 1),
                                                                                     ('EVENT', 1, 1)
    ON DUPLICATE KEY UPDATE id = id;