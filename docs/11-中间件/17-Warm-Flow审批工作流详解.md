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

### 2. 审批请假申请（含权限处理）

```java
@Autowired
private CustomPermissionHandler permissionHandler;

@Override
@Transactional(rollbackFor = Exception.class)
public void approveLeaveRequest(String flowInstanceId, Boolean approved, String comment, Long approverId) {
    // 设置当前审批人到 ThreadLocal
    String handler = String.valueOf(approverId);
    CustomPermissionHandler.setCurrentHandler(handler);
    
    try {
        // 构建审批参数
        Map<String, Object> variable = new HashMap<>();
        variable.put("approved", approved);
        variable.put("comment", comment);

        Long instanceId = Long.valueOf(flowInstanceId);

        if (approved) {
            // 审批通过
            FlowParams passParams = FlowParams.build()
                    .flowCode("leave_approval")
                    .variable(variable)
                    .skipType("PASS")
                    .handler(handler);
            
            taskService.skipByInsId(instanceId, passParams);
            
            // 重新查询流程实例，检查流程是否结束
            Instance updatedInstance = insService.getById(instanceId);
            
            // 判断流程是否已完成
            if (FlowStatus.FINISHED.getKey().equals(updatedInstance.getFlowStatus()) || "end".equals(updatedInstance.getNodeCode())) {
                // 流程结束，更新业务表状态为已通过
                LeaveRequest leaveRequest = this.lambdaQuery()
                        .eq(LeaveRequest::getFlowInstanceId, flowInstanceId)
                        .one();
                if (leaveRequest != null) {
                    leaveRequest.setStatus(2); // 已通过
                    leaveRequest.setApprovalComment(comment);
                    this.updateById(leaveRequest);
                }
            }
        } else {
            // 审批驳回
            FlowParams rejectParams = FlowParams.build()
                    .flowCode("leave_approval")
                    .variable(variable)
                    .skipType("REJECT")
                    .handler(handler);
            taskService.terminationByInsId(instanceId, rejectParams);
            
            // 更新业务表状态为已驳回
            LeaveRequest leaveRequest = this.lambdaQuery()
                    .eq(LeaveRequest::getFlowInstanceId, flowInstanceId)
                    .one();
            if (leaveRequest != null) {
                leaveRequest.setStatus(3); // 已驳回
                leaveRequest.setApprovalComment(comment);
                this.updateById(leaveRequest);
            }
        }
    } finally {
        // 清除 ThreadLocal，防止内存泄漏
        CustomPermissionHandler.clearCurrentHandler();
    }
}
```

**关键点说明：**
- ✅ **ThreadLocal 管理**：使用 `CustomPermissionHandler` 传递当前审批人信息
- ✅ **权限校验**：Warm-Flow 会自动调用 `PermissionHandler.permissions()` 进行权限校验
- ✅ **流程结束判断**：使用 `FlowStatus.FINISHED.getKey()` 或 `node_code == "end"` 判断流程是否完成
- ✅ **业务表同步**：流程结束后及时更新业务表状态
- ✅ **资源清理**：在 finally 块中清除 ThreadLocal，防止内存泄漏

### 3. 自定义权限处理器（CustomPermissionHandler）

```java
@Slf4j
@Component
public class CustomPermissionHandler implements PermissionHandler {
    // 使用 ThreadLocal 存储当前审批人ID
    private static final ThreadLocal<String> currentHandler = new ThreadLocal<>();

    /**
     * 设置当前审批人
     */
    public static void setCurrentHandler(String handler) {
        currentHandler.set(handler);
        log.debug("设置当前审批人: {}", handler);
    }

    /**
     * 清除当前审批人
     */
    public static void clearCurrentHandler() {
        currentHandler.remove();
    }

    /**
     * 返回当前用户的权限列表
     * Warm-Flow 会调用此方法进行权限校验
     */
    @Override
    public List<String> permissions() {
        String handler = currentHandler.get();
        if (handler != null) {
            List<String> permissions = new ArrayList<>();
            permissions.add(handler);
            return permissions;
        }
        return Collections.emptyList();
    }

    /**
     * 返回当前办理人
     */
    @Override
    public String getHandler() {
        String handler = currentHandler.get();
        log.debug("获取当前办理人: {}", handler);
        return handler;
    }
}
```

**工作原理：**
1. 业务代码调用 `CustomPermissionHandler.setCurrentHandler("2001")`
2. Warm-Flow 执行时调用 `PermissionHandler.permissions()` 获取权限列表 `["2001"]`
3. Warm-Flow 校验权限列表是否包含节点的 `permission_flag`（如 `"2001"`）
4. 校验通过则执行任务跳转
5. 最后在 finally 块中调用 `clearCurrentHandler()` 清理资源

### 4. 撤销流程（忽略权限校验）

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void cancelLeaveRequest(String flowInstanceId, Long userId) {
    // 设置当前操作人到 ThreadLocal
    String handler = String.valueOf(userId);
    CustomPermissionHandler.setCurrentHandler(handler);
    
    try {
        Long instanceId = Long.valueOf(flowInstanceId);
        
        // 查询流程实例
        Instance instance = insService.getById(instanceId);
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        // 终止流程 - 需要传入handler参数，并忽略权限校验
        FlowParams cancelParams = FlowParams.build()
                .flowCode("leave_approval")
                .handler(handler)
                .ignore(true);  // 忽略权限校验（申请人可以撤销自己的申请）
        taskService.terminationByInsId(instanceId, cancelParams);

        // 更新业务表状态为已撤销
        LeaveRequest leaveRequest = this.lambdaQuery()
                .eq(LeaveRequest::getFlowInstanceId, flowInstanceId)
                .one();
        if (leaveRequest != null) {
            leaveRequest.setStatus(4); // 已撤销
            this.updateById(leaveRequest);
        }
    } finally {
        // 清除 ThreadLocal，防止内存泄漏
        CustomPermissionHandler.clearCurrentHandler();
    }
}
```

**关键点说明：**
- ✅ **`.ignore(true)`**：忽略权限校验，允许申请人撤销自己提交的申请
- ✅ **即使当前节点不是申请人**：例如部门经理审批中，申请人仍可撤销
- ✅ **必须设置 handler**：即使忽略权限校验，也需要传入 handler 用于记录

---

## ⚠️ 常见问题

### 1. 权限校验失败

**错误信息：** `无法跳转到该节点,请检查当前用户是否有权限!`

**原因：**
- 未设置 `handler` 参数
- 未实现 `PermissionHandler` 接口
- 节点的 `permission_flag` 与传入的 `handler` 不匹配

**解决方案：**
```java
// 1. 实现 PermissionHandler 接口（见上文 CustomPermissionHandler）
// 2. 在业务代码中设置 handler
CustomPermissionHandler.setCurrentHandler(String.valueOf(approverId));
try {
    FlowParams params = FlowParams.build()
            .flowCode("leave_approval")
            .handler(String.valueOf(approverId))  // 必须设置
            .skipType("PASS");
    taskService.skipByInsId(instanceId, params);
} finally {
    CustomPermissionHandler.clearCurrentHandler();
}
```

### 2. NullPointerException: Cannot invoke "String.toCharArray()"

**错误信息：** `java.lang.NullPointerException: Cannot invoke "String.toCharArray()" because "val" is null`

**原因：** 节点的 `node_ratio` 字段为 NULL

**解决方案：**
```sql
-- 为审批节点设置 node_ratio = 100.000
UPDATE flow_node SET node_ratio = 100.000 WHERE node_type = 1;
```

### 3. 流程结束后业务表状态未更新

**原因：** 判断流程结束的条件不正确

**解决方案：**
```java
// 正确的方式：同时判断 flow_status 和 node_code
Instance instance = insService.getById(instanceId);
if (FlowStatus.FINISHED.getKey().equals(instance.getFlowStatus()) || "end".equals(instance.getNodeCode())) {
    // 流程已结束，更新业务表
}
```

### 4. ThreadLocal 内存泄漏

**原因：** 未在 finally 块中清除 ThreadLocal

**解决方案：**
```java
CustomPermissionHandler.setCurrentHandler(handler);
try {
    // 业务逻辑
} finally {
    CustomPermissionHandler.clearCurrentHandler();  // 必须清理
}
```

---

## 🎯 最佳实践

### 1. 流程节点配置

| 节点类型 | permission_flag | node_ratio | 说明 |
|---------|----------------|------------|------|
| 开始节点 | NULL | NULL | 不需要权限校验 |
| 审批节点 | 审批人ID（如 "2001"） | 100.000 | 单人审批 |
| 会签节点 | 多个审批人ID | 100.000 | 所有人都需同意 |
| 或签节点 | 多个审批人ID | 50.000 | 任意一人同意即可 |
| 结束节点 | NULL | NULL | 不需要权限校验 |

### 2. 数据库初始化

```sql
-- 清理旧数据
DELETE FROM flow_skip WHERE definition_id = 1001;
DELETE FROM flow_node WHERE definition_id = 1001;
DELETE FROM flow_definition WHERE id = 1001;

-- 插入流程定义
INSERT INTO flow_definition (...) VALUES (...);

-- 插入节点（注意设置 node_ratio）
INSERT INTO flow_node (id, node_type, permission_flag, node_ratio, ...) 
VALUES (2001, 1, '2001', 100.000, ...);

-- 插入流转关系
INSERT INTO flow_skip (...) VALUES (...);
```

### 3. 代码结构建议

```
src/main/java/
├── config/
│   └── CustomPermissionHandler.java    # 权限处理器
├── service/
│   ├── LeaveRequestService.java        # 业务接口
│   └── impl/
│       └── LeaveRequestServiceImpl.java # 业务实现
└── controller/
    └── LeaveRequestController.java     # 控制器
```

### 4. 日志记录

```java
log.info("审批操作，approverId: {}, handler: {}", approverId, handler);
log.info("流程状态: {}, 当前节点: {}", instance.getFlowStatus(), instance.getNodeCode());
log.info("业务表状态已更新为已通过");
```

### 5. 事务管理

```java
@Transactional(rollbackFor = Exception.class)  // 所有异常都回滚
public void approveLeaveRequest(...) {
    // Warm-Flow 操作和业务表更新在同一事务中
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

