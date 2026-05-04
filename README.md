# LinkFlow - 短链营销平台

> 多服务微服务架构，短链生成 + 审批流程管理

## 项目定位

LinkFlow 是一个营销活动管理平台，核心能力：
- **活动创建与管理**：营销人员创建促销活动
- **审批流程**：两级审批流程（Flowable 工作流引擎）
- **短链集成**：审批通过后自动生成短链接

**注意**：ShortLink 是独立的基础服务，LinkFlow 通过 API 调用获取短链能力。

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **网关** | Spring Cloud Gateway | - |
| **RPC** | Apache Dubbo | 3.2.0 |
| **注册中心** | Nacos | 2.3.0 |
| **工作流** | Flowable | 7.0.1 |
| **ORM** | MyBatis | 3.0.3 |
| **数据库** | MySQL | 8.x |
| **Java** | JDK | 17 |
| **Spring Boot** | - | 3.4.2 |

---

## 项目结构

```
link-flow/
├── linkflow-api          # Dubbo 接口契约 + DTO（无实现）
├── linkflow-common       # 公共工具类、常量、异常
├── linkflow-mbg          # MyBatis Generator 代码生成器
├── linkflow-gateway      # API 网关 (端口 8080)
├── linkflow-user         # 用户服务 (端口 8084)
├── linkflow-campaign     # 活动服务 (端口 8081)
├── linkflow-workflow     # 工作流服务 (端口 8082)
├── tables-sql.sql        # 数据库初始化脚本
├── linkflow_flowable_init.sql  # Flowable 初始化
└── pom.xml               # 父 POM
```

---

## 架构说明

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   外部用户                                       │
│                         (营销人员 / 审批人 / 管理员)                              │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            linkflow-gateway (8080)                              │
│                        Spring Cloud Gateway + Nacos                             │
│                                                                                 │
│    /api/user/**        /api/campaign/**        /api/workflow/**                 │
│    用户管理             活动管理                 审批操作                          │
└─────────────────────────────────────────────────────────────────────────────────┘
                    │                      │                      │
                    ▼                      ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           LinkFlow 微服务集群                                    │
│                         (Dubbo RPC + Nacos 注册)                                │
│                                                                                 │
│  ┌────────────────┐    ┌────────────────────┐    ┌────────────────────┐        │
│  │  linkflow-user │    │  linkflow-campaign │    │  linkflow-workflow │        │
│  │     (8084)     │    │      (8081)        │    │      (8082)        │        │
│  │                │    │                    │    │                    │        │
│  │ 用户注册        │    │ 活动创建            │    │ Flowable 引擎      │        │
│  │ 审批人配置      │    │ 状态管理            │    │ BPMN 流程执行      │        │
│  │ Dubbo Provider │    │ Dubbo Provider     │    │ TaskListener       │        │
│  │                │    │ Dubbo Consumer:    │    │ Delegate           │        │
│  │                │    │  → WorkflowApi     │    │ Dubbo Provider     │        │
│  │                │    │  → ShortLinkApi    │    │                    │        │
│  └────────────────┘    └────────────────────┘    └────────────────────┘        │
│                                    │                                            │
│                                    │ ShortLinkApi.createShortLink()             │
│                                    ▼                                            │
│                    ┌────────────────────────────────┐                          │
│                    │     ShortLink 集成适配层        │                          │
│                    │  HTTP/Dubbo 调用 ShortLink 服务 │                          │
│                    └────────────────────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        ShortLink 短链基础服务 (独立部署)                          │
│                        studywithus.dpdns.org (域名)                             │
│                                                                                 │
│         ┌───────────────────────────────────────────────────────────┐          │
│         │                    Nginx (80)                             │          │
│         │              负载均衡 → 3 实例                            │          │
│         └───────────────────────────────────────────────────────────┘          │
│                              │              │              │                   │
│                              ▼              ▼              ▼                   │
│         ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                       │
│         │ shortlink1  │ │ shortlink2  │ │ shortlink3  │                       │
│         │   (8080)    │ │   (8081)    │ │   (8082)    │                       │
│         └─────────────┘ └─────────────┘ └─────────────┘                       │
│                                                                                 │
│  功能：                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐      │
│  │ POST /create          创建短链接                                     │      │
│  │                       输入: longUrl                                  │      │
│  │                       输出: shortCode (如 zhIdWLGHg0)                │      │
│  │                                                                      │      │
│  │ GET /protect/{code}   短链跳转 (布隆过滤器保护)                       │      │
│  │                       输入: shortCode                                │      │
│  │                       输出: 302 重定向到 longUrl                     │      │
│  └─────────────────────────────────────────────────────────────────────┘      │
│                                                                                 │
│  技术栈：ShardingSphere 分表 │ Redis 缓存 │ Kafka 点击量 │ 布隆过滤器            │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              基础设施层                                          │
│                                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │    Nacos     │  │    MySQL     │  │    Redis     │  │    Kafka     │       │
│  │   (8848)     │  │   (3306)     │  │   (6379)     │  │   (9092)     │       │
│  │  Dubbo 注册  │  │ short_link   │  │  短链缓存    │  │ 点击量消息   │       │
│  │              │  │ linkflow_db  │  │              │  │              │       │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 模块职责

| 模块 | 端口 | 职责 |
|------|------|------|
| **linkflow-api** | - | Dubbo 接口契约 + DTO，无实现 |
| **linkflow-common** | - | 公共工具类、常量、异常处理 |
| **linkflow-mbg** | - | MyBatis Generator 代码生成器 |
| **linkflow-gateway** | 8080 | API 网关，路由转发 |
| **linkflow-user** | 8084 | 用户管理 + 审批人配置 |
| **linkflow-campaign** | 8081 | 活动管理 + 状态机 |
| **linkflow-workflow** | 8082 | Flowable 工作流引擎 |

### 数据库表

| 表名 | 所属模块 | 说明 |
|------|----------|------|
| `user` | User | 用户信息 |
| `approver_config` | User | 审批人配置（按活动类型 + 审批级别） |
| `campaign` | Campaign | 活动主表 |
| `workflow_instance` | Workflow | 工作流实例（业务层） |
| `approval_record` | Workflow | 审批记录 |
| `ACT_*` | Flowable | Flowable 引擎表（自动创建） |

---

## 核心业务流程

### 审批流程（BPMN）

```
创建活动 → 提交审批 → 一级审批(或签) → 二级审批(或签) → 审批通过/拒绝 → 绑定短链 → 活动上线
```

**BPMN 流程定义**：`linkflow-workflow/src/main/resources/processes/CampaignApproval.bpmn20.xml`

```
┌──────┐    ┌─────────────┐    ┌────────┐    ┌─────────────┐    ┌────────┐
│ 开始 │───▶│  一级审批   │───▶│ 网关1  │───▶│  二级审批   │───▶│ 网关2  │
└──────┘    │ (多实例或签)│    └────────┘    │ (多实例或签)│    └────────┘
            └─────────────┘                   └─────────────┘        │
                    │                              │                │
                    │ level1Approved               │ level2Approved │
                    ▼                              ▼                ▼
            ┌────────────────────────────────────────────────────────────┐
            │                                                          │
            │   通过 ──────────────────────▶ callbackApproved ──▶ 结束  │
            │                                                          │
            │   拒绝 ──────────────────────▶ callbackRejected ──▶ 结束  │
            │                                                          │
            └────────────────────────────────────────────────────────────┘
```

### 关键类

| 类 | 作用 |
|-----|------|
| `WorkflowApiImpl` | 工作流 Dubbo 服务实现，启动流程、审批、查询状态 |
| `CampaignServiceImpl` | 活动 Dubbo 服务实现，CRUD + 状态管理 |
| `Level1ApprovalListener` | 一级审批完成监听器，设置 `level1Approved` 变量 |
| `Level2ApprovalListener` | 二级审批完成监听器，设置 `level2Approved` 变量 |
| `CampaignApprovedDelegate` | 审批通过回调，调用 `Campaign.updateCampaignStatus(APPROVED)` |
| `CampaignRejectedDelegate` | 审批拒绝回调，调用 `Campaign.updateCampaignStatus(REJECTED)` |

---

## ShortLink 与 LinkFlow 的关系

### ShortLink 服务（独立部署）

**GitHub**: https://github.com/LUK-jiao/short-link

**域名**: `studywithus.dpdns.org`

**部署架构**:
```
Nginx(80) → 3实例(8080/8081/8082) → MySQL(分表) + Redis + Kafka
```

**核心接口**:
- `POST /create` - 创建短链接，返回短码
- `GET /protect/{code}` - 短链跳转（布隆过滤器 + 缓存）

**技术亮点**:
- ShardingSphere 分表
- 雪花算法分布式 ID
- 布隆过滤器防止穿透
- Redis 缓存热点短链
- Kafka 异步写入点击量

### 两者的边界

| 职责 | ShortLink | LinkFlow |
|------|-----------|----------|
| 短链生成 | ✅ 自己实现 | ❌ 调用 ShortLink API |
| 短链跳转 | ✅ 自己实现 | ❌ 不参与 |
| 点击统计 | ✅ 自己统计 | ❌ 可通过 API 查询 |
| 活动管理 | ❌ 不关心 | ✅ 自己实现 |
| 审批流程 | ❌ 不关心 | ✅ Flowable 实现 |
| 审批人配置 | ❌ 不关心 | ✅ User 模块实现 |

**核心设计原则**：ShortLink 服务必须「纯」——不加审批逻辑，不被 LinkFlow 污染。

---

## 当前开发进度

### 已完成模块 ✅

| 模块 | 状态 | 说明 |
|------|------|------|
| `linkflow-api` | ✅ 完成 | Dubbo 接口契约 + DTO |
| `linkflow-mbg` | ✅ 完成 | MyBatis Generator |
| `linkflow-user` | ✅ 完成 | 用户创建 + 审批人配置 |
| `linkflow-gateway` | ✅ 完成 | 网关路由配置 |
| `linkflow-campaign` | 🟡 90% | CRUD + 状态机，`bindShortCode` 空实现 |
| `linkflow-workflow` | 🟡 85% | Flowable 集成 + BPMN，审批记录持久化待验证 |

### 最新提交记录

```
0cbfd4f - WorkflowApiImpl 完整实现 (审批通过/拒绝/查询状态)
495c16c - BPMN 流程定义完成
d451108 - Gateway 模块完整实现
7caa44f - Flowable 7.0 API 兼容性修复
741a851 - Campaign + User Dubbo Provider 实现
```

### 发现的问题 ⚠️

1. **Campaign.bindShortCode()** 空实现，需要与 ShortLink 服务对接
2. **分页查询** 只是 TODO，未实现
3. **Campaign 表** 缺少 `short_code` 字段（需确认）
4. **审批记录表** 需验证是否正确记录审批历史

---

## 后续开发规划

### Phase 2: 完善核心功能（预计 3-5 天）

**优先级 P0 - 必须完成**

| 任务 | 说明 | 模块 |
|------|------|------|
| 数据库表结构检查 | 确认 `campaign.short_code` 字段、索引设计 | DB |
| 实现 bindShortCode | HTTP 调用 ShortLink `/create` 接口，绑定短码 | Campaign |
| 分页查询实现 | 使用 PageHelper 或 MyBatis-Plus 分页 | Campaign |
| ShortLink 集成适配 | 创建 `ShortLinkApi` 实现，HTTP 调用已部署服务 | 新模块或 Campaign |
| 审批回调验证 | 确保 Delegate 正确调用 Campaign，审批记录正确写入 | Workflow |

**优先级 P1 - 应该完成**

| 任务 | 说明 |
|------|------|
| 幂等处理 | 重复提交审批的防护 |
| 异常边界 | 审批人不存在、流程超时/挂起处理 |
| 单元测试 | Workflow 核心流程测试、Campaign 状态流转测试 |

### Phase 3: 接口层完善（预计 2-3 天）

| 任务 | 说明 |
|------|------|
| Gateway REST API | 对外暴露的 REST 接口路由 |
| 认证鉴权 | 如需要，集成 JWT/OAuth |
| Swagger 文档 | API 文档集成 |

### Phase 4: 集成测试与部署（预计 2-3 天）

| 任务 | 说明 |
|------|------|
| 服务启动验证 | Nacos → MySQL → User → Workflow → Campaign → Gateway |
| 端到端流程测试 | 创建活动 → 提交审批 → 一级 → 二级 → 短链绑定 → 完成 |
| Docker 部署 | 各服务 Dockerfile + docker-compose 编排 |
| 与 ShortLink 打通 | LinkFlow 调用 `studywithus.dpdns.org` 生成短链 |

---

## 开发者接手指南

### 环境准备

1. **JDK 17** - 必须
2. **Maven 3.8+**
3. **MySQL 8.x** - 创建 `linkflow_db` 数据库
4. **Nacos 2.3** - Dubbo 注册中心
5. **IDEA** (推荐)

### 启动顺序

```
1. Nacos (8848)
2. MySQL (执行 tables-sql.sql 初始化)
3. linkflow-user (8084)
4. linkflow-workflow (8082)
5. linkflow-campaign (8081)
6. linkflow-gateway (8080)
```

### 关键配置文件

| 文件 | 说明 |
|------|------|
| `pom.xml` | 父 POM，版本管理 |
| `linkflow-gateway/src/main/resources/application.yml` | 网关路由配置 |
| `linkflow-workflow/src/main/resources/application.yml` | Flowable 配置 |
| `tables-sql.sql` | 数据库初始化脚本 |

### 核心设计原则

1. **ShortLink 服务必须纯** - 不加审批逻辑，不被 LinkFlow 污染
2. **流程引擎不直接操作业务数据** - Flowable 只管理流程，通过 Delegate 回调 Campaign
3. **回调必须幂等** - Delegate 调用 Campaign 时需考虑重复调用场景

### 代码提交规则

- 改完代码**不自动提交**，等用户审阅后再提交
- 提交信息格式：`feat/fix/refactor: 简短描述`

---

## 快速开始

```bash
# 克隆项目
git clone https://github.com/LUK-jiao/link-flow.git
cd link-flow

# 编译
mvn clean package -DskipTests

# 初始化数据库
mysql -u root -p < tables-sql.sql

# 启动 Nacos (需要预先安装)
# 访问 http://localhost:8848/nacos

# 启动各服务（按顺序）
java -jar linkflow-user/target/linkflow-user-1.0.0-SNAPSHOT.jar
java -jar linkflow-workflow/target/linkflow-workflow-1.0.0-SNAPSHOT.jar
java -jar linkflow-campaign/target/linkflow-campaign-1.0.0-SNAPSHOT.jar
java -jar linkflow-gateway/target/linkflow-gateway-1.0.0-SNAPSHOT.jar
```

---

## 相关项目

- **ShortLink 短链基础服务**: https://github.com/LUK-jiao/short-link
- **部署域名**: `studywithus.dpdns.org`

---

## 作者

- GitHub: https://github.com/LUK-jiao
- Email: jiao_zeming@163.com

---

_最后更新: 2026-05-04_