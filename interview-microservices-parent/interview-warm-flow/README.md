# Warm-Flow 轻量级审批工作流引擎

## 📖 项目简介

基于 **Warm-Flow 1.8.5** 实现的轻量级审批工作流系统，提供请假、报销等常见审批流程的完整实现。

Warm-Flow 是一款国产的轻量级工作流引擎，相比 Flowable/Activiti 更加简单易用，适合中小型项目的审批流程需求。

**✨ 核心特性：**
- 🎨 **官方设计器集成** - 内置可视化流程设计器，无需额外开发
- 🚀 **开箱即用** - 简单配置即可使用
- 📦 **轻量级** - 依赖少，启动快
- 🔧 **易扩展** - 支持自定义办理人、监听器等

## 🎯 核心特性

- ✅ **轻量级设计** - 核心 jar 包仅 200KB+，启动速度快
- ✅ **易于集成** - Spring Boot Starter 方式集成，开箱即用
- ✅ **功能完善** - 支持会签、或签、条件分支、动态审批人等
- ✅ **国产开源** - 中文文档完善，社区活跃
- ✅ **高性能** - 基于 MyBatis-Plus，数据库操作高效

## 📁 项目结构

```
interview-warm-flow/
├── src/main/java/cn/itzixiao/interview/warmflow/
│   ├── WarmFlowApplication.java          # 启动类
│   ├── config/                           # 配置类
│   │   ├── SecurityConfig.java          # Spring Security 配置
│   │   └── Knife4jConfig.java           # API 文档配置
│   ├── controller/                       # 控制器层
│   │   ├── LeaveRequestController.java  # 请假审批接口
│   │   └── ReimbursementRequestController.java  # 报销审批接口
│   ├── service/                          # 服务层
│   │   ├── LeaveRequestService.java     # 请假服务接口
│   │   ├── ReimbursementRequestService.java  # 报销服务接口
│   │   └── impl/                        # 服务实现
│   ├── entity/                           # 实体类
│   │   ├── LeaveRequest.java            # 请假申请实体
│   │   └── ReimbursementRequest.java    # 报销申请实体
│   └── mapper/                           # 数据访问层
│       ├── LeaveRequestMapper.java
│       └── ReimbursementRequestMapper.java
└── src/main/resources/
    ├── application.yml                   # 应用配置
    └── sql/
        └── init.sql                      # 数据库初始化脚本
```

## 🚀 快速开始

### 后端启动

#### 1. 环境准备

- JDK 17+
- MySQL 8.0+
- Maven 3.6+

#### 2. 数据库初始化

```bash
mysql -u root -p < src/main/resources/sql/init.sql
```

#### 3. 修改配置

编辑 `application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/interview_warm_flow
    username: root
    password: your_password
```

#### 4. 启动项目

```bash
mvn spring-boot:run
```

后端服务将在 `http://localhost:8095` 启动

### 前端启动

#### 1. 进入前端目录

```bash
cd ui
```

#### 2. 安装依赖

```bash
npm install
# 或使用 pnpm
pnpm install
```

#### 3. 启动开发服务器

```bash
npm run dev
```

前端服务将在 `http://localhost:5174` 启动

#### 4. 访问系统

打开浏览器访问：http://localhost:5174

## 💡 功能特性

### 前端功能

- ✅ **主页导航** - 美观的卡片式导航界面
- ✅ **请假审批测试** - 完整的请假申请、审批、撤销、查询功能
- ✅ **报销审批测试** - 完整的报销申请、审批、撤销、查询功能
- ✅ **Warm-Flow 官方设计器** - 集成官方可视化流程设计器
  - 访问地址：http://localhost:8095/warm-flow-ui/index.html
  - 可视化绘制流程图
  - 节点属性配置
  - 办理人设置
  - 流程发布管理

### 后端 API

- ✅ 请假审批 RESTful API
- ✅ 报销审批 RESTful API
- ✅ Knife4j API 文档（http://localhost:8095/doc.html）

## 💡 前端页面说明

### 1. 主页 (http://localhost:5174/)

美观的卡片式导航界面，可快速访问各个功能模块。

### 2. 请假审批 (http://localhost:5174/leave)

**功能：**
- 提交请假申请（支持多种请假类型）
- 审批/驳回请假申请
- 撤销请假申请
- 查询请假申请详情

**使用流程：**
1. 填写请假表单，点击"提交申请"
2. 系统自动生成流程实例ID
3. 在审批操作区域输入流程实例ID
4. 选择审批结果（通过/驳回），填写审批意见
5. 点击"提交审批"完成审批
6. 可在查询区域输入流程实例ID查看详情

### 3. 报销审批 (http://localhost:5174/reimbursement)

**功能：**
- 提交报销申请（支持多种报销类型）
- 审批/驳回报销申请
- 撤销报销申请
- 查询报销申请详情

**使用流程：** 同请假审批

### 4. 流程设计器 (http://localhost:8095/warm-flow-ui/index.html)

**功能：**
- 🎨 **可视化设计** - 拖拽方式绘制流程图
- ⚙️ **节点配置** - 配置节点属性、条件分支
- 👥 **办理人设置** - 设置任务办理人、候选人
- 🚀 **流程发布** - 发布/停用流程定义
- 📋 **流程管理** - 查看、编辑、删除流程定义

**使用说明：**
1. 启动后端服务后，直接访问设计器地址
2. 点击"新增"创建新的流程定义
3. 使用设计器绘制流程图
4. 配置各节点的属性和办理人
5. 保存并发布流程

**默认流程示例：**
```
开始 → 部门经理审批 → HR审批 → 结束
```

## 💡 API 接口说明

### 请假审批

#### 提交请假申请

```http
POST /api/workflow/leave/submit
Content-Type: application/json

{
  "userId": 1001,
  "userName": "张三",
  "leaveType": 3,
  "startTime": "2026-04-10 09:00:00",
  "endTime": "2026-04-12 18:00:00",
  "days": 3.0,
  "reason": "年假休息"
}
```

#### 审批请假申请

```http
POST /api/workflow/leave/approve?flowInstanceId=xxx&approved=true&comment=同意&approverId=2001
```

#### 撤销请假申请

```http
POST /api/workflow/leave/cancel?flowInstanceId=xxx&userId=1001
```

#### 查询请假详情

```http
GET /api/workflow/leave/detail?flowInstanceId=xxx
```

### 报销审批

接口路径改为 `/api/workflow/reimbursement/*`，参数类似。

## 🔧 技术栈

### 后端技术栈

- **Spring Boot 3.x** - 应用框架
- **Warm-Flow 1.8.5** - 工作流引擎（最新版本）
- **warm-flow-plugin-ui-sb-web** - 官方设计器插件
- **MyBatis-Plus** - ORM 框架
- **MySQL 8.0** - 数据库
- **Knife4j** - API 文档
- **Spring Security** - 安全框架

### 前端技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Element Plus** - Vue 3 组件库
- **Vue Router** - 官方路由管理器
- **Pinia** - 官方状态管理库
- **Axios** - HTTP 客户端
- **dayjs** - 轻量级日期处理库

## 📊 Warm-Flow vs Flowable 对比

| 特性 | Warm-Flow | Flowable |
|------|-----------|----------|
| 核心 Jar 大小 | ~200KB | ~10MB+ |
| 学习曲线 | 低（1-2天） | 高（1-2周） |
| 数据库表数量 | 7 张 | 20+ 张 |
| 适用场景 | 中小型审批流程 | 复杂业务流程 |
| 性能 | 高 | 中 |
| 社区活跃度 | 中（国产） | 高（国际） |
| 文档完善度 | 中文完善 | 英文为主 |

## 🎓 学习资源

- **官方文档：** https://warm-flow.my-doc.net/
- **GitHub 地址：** https://gitee.com/dromara/warm-flow
- **配套文档：** `docs/11-中间件/17-Warm-Flow审批工作流详解.md`

## 📝 开发计划

- [x] 添加流程设计器（可视化拖拽）- ✅ 已集成 Warm-Flow 官方设计器
- [ ] 支持会签/或签
- [ ] 支持条件分支
- [ ] 支持动态审批人
- [ ] 添加流程监控面板
- [ ] 支持流程版本管理

## 👨‍💻 作者

- **itzixiao**
- 有问题欢迎提 Issue 或 PR

## 📄 许可证

MIT License
