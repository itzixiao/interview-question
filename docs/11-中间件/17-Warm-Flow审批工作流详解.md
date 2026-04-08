# 17-Warm-Flow审批工作流详解

## 📖 概述

Warm-Flow 是一款国产的轻量级工作流引擎,由 Dromara 开源组织开发。相比 Flowable/Activiti 等传统工作流引擎,Warm-Flow 更加简单易用、轻量高效,非常适合中小型项目的审批流程需求。

**当前使用版本:1.8.5(最新版本,集成官方设计器)**

### 核心优势

| 优势 | 说明 |
|------|------|
| **官方设计器** | 内置可视化流程设计器,开箱即用 |
| **轻量级** | 核心 jar 包仅 200KB+,启动速度快,内存占用小 |
| **易集成** | Spring Boot Starter 方式,几行配置即可使用 |
| **低学习成本** | API 简洁直观,1-2 天即可上手 |
| **功能完善** | 支持会签、或签、条件分支、动态审批人等 |
| **高性能** | 基于 MyBatis-Plus,数据库操作高效 |
| **国产开源** | 中文文档完善,社区活跃,响应及时 |

---

## 🚀 快速开始

### 1. 添加依赖

```xml
<!-- Warm-Flow 核心依赖 -->
<dependency>
    <groupId>org.dromara.warm</groupId>
    <artifactId>warm-flow-mybatis-plus-sb3-starter</artifactId>
    <version>1.8.5</version>
</dependency>

<!-- Warm-Flow 官方设计器插件 -->
<dependency>
    <groupId>org.dromara.warm</groupId>
    <artifactId>warm-flow-plugin-ui-sb-web</artifactId>
    <version>1.8.5</version>
</dependency>
```

### 2. 配置文件

```yaml
# Warm-Flow 工作流配置
warm-flow:
  enabled: true           # 是否开启工作流
  db-type: mysql         # 数据库类型
  print-sql: true        # 是否打印SQL日志
  table-prefix: flow_    # 表前缀
  show-ui: true          # 是否开启设计器UI
  token-name: Authorization  # 共享业务系统权限的token名称
```

### 3. 启动流程

```java
@Autowired
private InsService insService;

// 启动流程实例 - 使用 FlowParams 构建参数
FlowParams flowParams = FlowParams.build()
        .flowCode("leave_approval")
        .variable("userId", 1001)
        .variable("days", 3);

Instance instance = insService.start("business_key_001", flowParams);
System.out.println("流程实例ID: " + instance.getId());
```

### 4. 访问官方设计器

启动项目后,直接访问:
```
http://localhost:8085/warm-flow-ui/index.html
```

设计器提供以下功能:
- 🎨 可视化绘制流程图
- ⚙️ 配置节点属性和条件分支
- 👥 设置任务办理人和候选人
- 🚀 发布和管理流程定义

---

## 💡 核心概念

### 1. 流程实例服务 (InsService)

1.8.5版本使用 `InsService` 替代了旧版本的 `InstanceService`

```java
@Autowired
private InsService insService;

// 启动流程
Instance instance = insService.start(businessKey, flowParams);

// 查询流程实例
Instance instance = insService.getById(instanceId);
```

### 2. 任务服务 (TaskService)

1.8.5版本使用 `TaskService` 替代了旧版本的 `InsTaskService`

```java
@Autowired
private TaskService taskService;

// 查询待办任务
List<Task> tasks = taskService.listByInstanceId(instanceId);

// 审批通过
FlowParams flowParams = FlowParams.build()
        .variable("approved", true)
        .variable("comment", "同意");
taskService.skip(taskId, flowParams);

// 审批驳回
taskService.terminate(taskId, flowParams);
```

### 3. FlowParams 参数对象

1.8.5版本使用 `FlowParams` 对象传递参数,替代原来的 `Map<String, Object>`

```java
// 构建流程参数
FlowParams flowParams = FlowParams.build()
        .flowCode("leave_approval")          // 流程定义编码
        .variable("userId", 1001)            // 流程变量
        .variable("days", 3)
        .variable("reason", "年假");

// 启动流程
Instance instance = insService.start(businessKey, flowParams);

// 审批时传递变量
FlowParams approveParams = FlowParams.build()
        .variable("approved", true)
        .variable("comment", "同意");
taskService.skip(taskId, approveParams);
```

---

## 🎯 实战案例:请假审批流程

### 1. 提交请假申请

```java
@Override
@Transactional(rollbackFor = Exception.class)
public String submitLeaveRequest(LeaveRequest leaveRequest) {
    // 保存请假申请
    leaveRequest.setStatus(1); // 审批中
    this.save(leaveRequest);

    // 启动工作流 - 使用 FlowParams 构建参数
    FlowParams flowParams = FlowParams.build()
            .flowCode("leave_approval")
            .variable("userId", leaveRequest.getUserId())
            .variable("userName", leaveRequest.getUserName())
            .variable("leaveType", leaveRequest.getLeaveType())
            .variable("days", leaveRequest.getDays());

    // 启动流程实例
    Instance instance = insService.start(leaveRequest.getId().toString(), flowParams);
    
    // 更新流程实例ID
    leaveRequest.setFlowInstanceId(instance.getId());
    this.updateById(leaveRequest);

    return instance.getId();
}
```

### 2. 审批请假申请

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void approveLeaveRequest(String flowInstanceId, Boolean approved, String comment) {
    // 查询当前待办任务
    List<Task> taskList = taskService.listByInstanceId(flowInstanceId);
    
    if (taskList == null || taskList.isEmpty()) {
        throw new RuntimeException("未找到待办任务");
    }

    Task task = taskList.get(0);

    // 构建审批参数
    FlowParams flowParams = FlowParams.build()
            .variable("approved", approved)
            .variable("comment", comment);

    if (approved) {
        // 审批通过
        taskService.skip(task.getId(), flowParams);
        
        // 检查流程是否结束
        Instance instance = insService.getById(flowInstanceId);
        if (instance.getFlowStatus().equals(FlowStatus.FINISHED.getValue())) {
            // 流程结束,更新状态为已通过
            // ... 更新业务数据
        }
    } else {
        // 审批驳回
        taskService.terminate(task.getId(), flowParams);
        // ... 更新业务数据
    }
}
```

---

## 📊 版本对比

### 1.7.2 vs 1.8.5 API 对比

| 功能 | 1.7.2 版本 | 1.8.5 版本 |
|------|-----------|-----------|
| 流程实例服务 | `InstanceService` | `InsService` |
| 任务服务 | `InsTaskService` | `TaskService` |
| 启动流程 | `start(flowCode, businessKey, Map)` | `start(businessKey, FlowParams)` |
| 审批任务 | `skip(taskId, Map)` | `skip(taskId, FlowParams)` |
| 设计器 | ❌ 不支持 | ✅ 官方设计器插件 |

### 为什么选择 1.8.5?

1. ✅ **官方设计器** - 内置可视化流程设计器,无需集成第三方库
2. ✅ **更好的API** - FlowParams 对象提供更清晰的参数传递方式
3. ✅ **完整功能** - 支持撤销、驳回、拿回等新功能
4. ✅ **持续维护** - 最新版本,持续更新和优化

---

## 🎨 前端集成

### 方式一：使用官方内置设计器（推荐）

Warm-Flow 1.8.5 已内置流程设计器 UI，直接访问：

```
http://localhost:8092/warm-flow-ui
```

**设计器功能：**
- 🎨 可视化绘制流程图（基于 bpmn-js）
- ⚙️ 配置节点属性和条件分支
- 👥 设置任务办理人和候选人
- 🚀 发布和管理流程定义
- 📊 查看流程实例和审批记录

### 方式二：自定义前端集成

项目提供了完整的前端集成示例代码，位于 `interview-ui/warm-flow/` 目录：

```
interview-ui/warm-flow/
├── README.md               # 集成文档
├── WorkflowDesigner.vue    # 流程设计器组件（Vue 3）
├── ProcessInstanceList.vue # 流程实例管理
├── TodoTaskList.vue        # 待办任务列表
├── router.js               # 路由配置示例
└── package.json            # 依赖配置
```

#### 1. 安装依赖

```bash
cd interview-ui/warm-flow
npm install
```

#### 2. 主要组件

**流程设计器（WorkflowDesigner.vue）：**
- 基于 bpmn-js 实现
- 支持流程绘制、保存、部署
- 支持导入/导出 BPMN XML
- 节点属性配置面板

**流程实例管理（ProcessInstanceList.vue）：**
- 流程实例列表查询
- 发起新流程
- 审批流程（通过/驳回）
- 撤销流程
- 查看流程图

**待办任务（TodoTaskList.vue）：**
- 我的待办任务列表
- 快捷审批操作
- 查看流程进度

#### 3. 路由配置

```javascript
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/workflow/designer',
    name: 'WorkflowDesigner',
    component: () => import('./warm-flow/WorkflowDesigner.vue')
  },
  {
    path: '/workflow/instances',
    name: 'ProcessInstanceList',
    component: () => import('./warm-flow/ProcessInstanceList.vue')
  },
  {
    path: '/workflow/tasks',
    name: 'TodoTaskList',
    component: () => import('./warm-flow/TodoTaskList.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

#### 4. API 接口调用

```javascript
import axios from 'axios'

// 保存流程定义
async function saveProcess(xml) {
  const res = await axios.post('/warm-flow/def/save', {
    name: '请假审批流程',
    processJson: xml,
    category: 'leave'
  })
  return res.data
}

// 部署流程
async function deployProcess(definitionId) {
  await axios.post(`/warm-flow/def/deploy/${definitionId}`)
}

// 启动流程实例
async function startProcess(businessKey, flowParams) {
  const res = await axios.post('/warm-flow/ins/start', {
    businessKey,
    flowParams
  })
  return res.data
}

// 审批任务
async function approveTask(instanceId, approved, comment) {
  await axios.post(`/warm-flow/task/approve/${instanceId}`, {
    approved,
    comment
  })
}

// 获取待办任务
async function getTodoTasks(pageNum = 1, pageSize = 10) {
  const res = await axios.get('/warm-flow/task/todo', {
    params: { pageNum, pageSize }
  })
  return res.data
}
```

### 完整示例

查看前端集成详细文档：[interview-ui/warm-flow/README.md](../../interview-ui/warm-flow/README.md)

---

## 🎓 学习资源

- **官方文档**: https://warm-flow.com/
- **设计器文档**: https://warm-flow.com/master/primary/designerIntroduced.html
- **GitHub**: https://gitee.com/dromara/warm-flow
- **演示项目**: http://www.hhzai.top (账号:admin/admin123)
- **前端集成示例**: [interview-ui/warm-flow](../../interview-ui/warm-flow/)

---

**文档版本**: 1.0  
**更新日期**: 2026-04-08  
**Warm-Flow版本**: 1.8.5

