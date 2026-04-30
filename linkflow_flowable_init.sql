-- ===========================================
-- Flowable 独立数据库初始化
-- 创建时间：2026-04-30
-- ===========================================

-- 创建 Flowable 独立数据库
CREATE DATABASE IF NOT EXISTS linkflow_flowable DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Flowable 表由引擎自动创建，无需手动创建
-- 启动 Workflow 服务时会自动创建以下表：
-- ACT_RE_* (流程定义仓库)
-- ACT_RU_* (运行时数据)
-- ACT_HI_* (历史数据)
-- ACT_ID_* (身份信息)

-- 如需手动初始化，可执行 Flowable 官方 SQL 脚本
-- 位置：flowable-engine.jar 中的 org/flowable/db/目录