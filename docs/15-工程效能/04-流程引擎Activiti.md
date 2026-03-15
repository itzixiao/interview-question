# 流程引擎 Activiti/Camunda

## 一、Activiti 架构

### 1.1 核心组件

- **ProcessEngine**：流程引擎核心
- **RepositoryService**：流程定义管理
- **RuntimeService**：流程实例管理
- **TaskService**：任务管理
- **HistoryService**：历史记录管理

---

## 二、BPMN 流程定义

### 2.1 请假审批流程示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<process id="leaveApproval" name="请假审批流程">
  
  <!-- 开始节点 -->
  <startEvent id="start" name="开始"/>
  
  <!-- 提交申请 -->
  <userTask id="submitTask" name="提交申请"
            activiti:assignee="${applicant}"/>
  
  <!-- 部门经理审批 -->
  <userTask id="managerApproval" name="部门经理审批"
            activiti:assignee="${manager}"/>
  
  <!-- HR 审批 -->
  <userTask id="hrApproval" name="HR 审批"
            activiti:assignee="${hr}"/>
  
  <!-- 结束节点 -->
  <endEvent id="end" name="结束"/>
  
  <!-- 流程连线 -->
  <sequenceFlow id="flow1" sourceRef="start" targetRef="submitTask"/>
  <sequenceFlow id="flow2" sourceRef="submitTask" targetRef="managerApproval"/>
  
  <!-- 条件判断 -->
  <exclusiveGateway id="gateway1"/>
  <sequenceFlow id="flow3" sourceRef="managerApproval" targetRef="gateway1"/>
  
  <!-- 同意 -->
  <sequenceFlow id="flow4" sourceRef="gateway1" targetRef="hrApproval">
    <conditionExpression>${approved == true}</conditionExpression>
  </sequenceFlow>
  
  <!-- 拒绝 -->
  <sequenceFlow id="flow5" sourceRef="gateway1" targetRef="end">
    <conditionExpression>${approved == false}</conditionExpression>
  </sequenceFlow>
  
  <sequenceFlow id="flow6" sourceRef="hrApproval" targetRef="end"/>
  
</process>
```

### 2.2 BPMN 元素说明

| 元素 | 说明 | 用途 |
|------|------|------|
| **startEvent** | 开始事件 | 流程起点 |
| **endEvent** | 结束事件 | 流程终点 |
| **userTask** | 用户任务 | 需要人工处理 |
| **serviceTask** | 服务任务 | 自动执行代码 |
| **sequenceFlow** | 顺序流 | 连接节点 |
| **exclusiveGateway** | 排他网关 | 条件分支 |
| **parallelGateway** | 并行网关 | 并发执行 |

---

## 三、流程引擎使用

### 3.1 Spring Boot集成

```java
@Service
public class WorkflowService {
    
    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private RepositoryService repositoryService;
    
    /**
     * 启动流程
     */
    public void startProcess(String processKey, Map<String, Object> variables) {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            processKey, variables);
        log.info("启动流程：{}, 实例 ID: {}", processKey, instance.getId());
    }
    
    /**
     * 完成任务
     */
    public void completeTask(String taskId, Map<String, Object> variables) {
        taskService.complete(taskId, variables);
        log.info("完成任务：{}", taskId);
    }
    
    /**
     * 查询待办任务
     */
    public List<Task> getTasks(String assignee) {
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();
    }
    
    /**
     * 部署流程
     */
    public void deployProcess(String bpmnFile) {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource(bpmnFile)
                .deploy();
        log.info("部署流程：{}, 部署 ID: {}", deployment.getName(), deployment.getId());
    }
}
```

---

## 四、会签或签实现

### 4.1 会签（多人同时审批）

```xml
<userTask id="multiInstanceTask" name="会签任务">
  <multiInstanceLoopCharacteristics isSequential="false">
    <loopCardinality>${approverList.size()}</loopCardinality>
    <loopDataInputRef>approverList</loopDataInputRef>
    <inputDataItem name="approver"/>
    <completionCondition>${nrOfCompletedInstances/nrOfInstances >= 1}</completionCondition>
  </multiInstanceLoopCharacteristics>
</userTask>
```

### 4.2 或签（一人审批即可）

```xml
<userTask id="orSignTask" name="或签任务"
          activiti:candidateUsers="${approverList}">
</userTask>
```

---

## 五、流程驳回与撤回

### 5.1 流程驳回

```java
/**
 * 驳回到指定节点
 */
public void rejectToTask(String processInstanceId, String targetTaskId) {
    // 获取当前活动节点
    List<ActivityInstance> currentActivities = runtimeService
        .createActivityInstanceQuery()
        .processInstanceId(processInstanceId)
        .list();
    
    // 删除当前活动
    for (ActivityInstance activity : currentActivities) {
        runtimeService.deleteProcessInstance(processInstanceId, "驳回");
    }
    
    // 跳转到目标节点
    runtimeService.createChangeActivityStateBuilder()
        .processInstanceId(processInstanceId)
        .moveSingleExecutionToSingleExecution(targetTaskId, targetTaskId)
        .changeState();
}
```

### 5.2 流程撤回

```java
/**
 * 撤回流程
 */
public void withdrawProcess(String processInstanceId, String userId) {
    // 查询第一个任务（发起人提交后的任务）
    Task task = taskService.createTaskQuery()
        .processInstanceId(processInstanceId)
        .singleResult();
    
    if (task != null) {
        // 删除流程实例
        runtimeService.deleteProcessInstance(processInstanceId, "撤回");
        log.info("流程已撤回：{}", processInstanceId);
    }
}
```

---

## 六、Activiti vs Camunda

| 特性 | Activiti | Camunda |
|------|---------|---------|
| 起源 | Alfresco 衍生 | 基于 Activiti  fork |
| 社区活跃度 | 一般 | 活跃 |
| 功能特性 | 基础完善 | 更丰富 |
| 文档质量 | 一般 | 优秀 |
| 外部任务模式 | 不支持 | 支持 |
| DMN 决策表 | 基础 | 强大 |

---

## 七、流程引擎面试题

### 1. Activiti 的核心组件？

**参考答案：**

**7 大核心服务：**
1. **RepositoryService**：流程定义管理（部署、查询）
2. **RuntimeService**：流程实例管理（启动、变量）
3. **TaskService**：任务管理（待办、签收、完成）
4. **HistoryService**：历史记录管理（历史数据查询）
5. **IdentityService**：身份认证管理（用户、组）
6. **FormService**：表单管理（表单定义、渲染）
7. **ManagementService**：引擎管理服务（定时任务、数据库）

### 2. BPMN 2.0 规范了解吗？

**参考答案：**

**BPMN 2.0（Business Process Model and Notation）** 是业务流程建模的标准规范。

**核心元素：**
- **事件（Events）**：开始、结束、中间事件
- **活动（Activities）**：任务、子流程
- **网关（Gateways）**：排他、并行、包容、事件网关
- **连线（Flows）**：顺序流、消息流、关联

**优势：**
- 统一标准，厂商中立
- 图形化，易理解
- 可执行，直接驱动引擎

### 3. 如何实现流程的会签、或签？

**参考答案：**

**会签（多人同时审批）：**
- 使用 `multiInstanceLoopCharacteristics`
- `isSequential="false"` 表示并行
- 设置 `completionCondition` 完成条件

**或签（一人审批即可）：**
- 使用 `activiti:candidateUsers` 指定候选人列表
- 任意一人完成任务即可

**应用场景：**
- 会签：多部门联合会审
- 或签：领导出差时授权他人代批

### 4. 流程变量如何使用？

**参考答案：**

**变量作用域：**
1. **全局变量**：整个流程实例
2. **局部变量**：单个任务或执行树

**API 使用：**
```java
// 设置全局变量
runtimeService.setVariable(processInstanceId, "key", value);

// 设置局部变量
taskService.setVariableLocal(taskId, "key", value);

// 获取变量
Object value = runtimeService.getVariable(processInstanceId, "key");
```

**使用场景：**
- 传递业务数据
- 控制流程走向（网关条件）
- 动态分配审批人

### 5. 如何处理流程驳回、撤回？

**参考答案：**

**驳回实现：**
1. 删除当前活动节点
2. 使用 `ChangeActivityStateBuilder` 跳转到目标节点
3. 重新创建任务

**撤回实现：**
1. 检查是否还有待办任务（未审批）
2. 删除流程实例
3. 记录撤回日志

**注意事项：**
- 需要记录操作日志
- 考虑数据一致性
- 通知相关人员

### 6. Activiti 和 Camunda 的区别？

**参考答案：**

**主要区别：**
1. **外部任务模式**：Camunda 支持，Activiti 不支持
2. **DMN 决策表**：Camunda 更强大
3. **用户体验**：Camunda 的 Cockpit 更好用
4. **社区生态**：Camunda 更活跃
5. **版本更新**：Camunda 迭代更快

**选择建议：**
- 传统企业：Activiti 稳定
- 互联网项目：Camunda 灵活

---

## 八、API 接口测试

```bash
# 部署流程定义
curl -X POST http://localhost:8084/api/workflow/deploy \
  -F "file=@leave.bpmn"

# 启动流程实例
curl -X POST http://localhost:8084/api/workflow/start \
  -H "Content-Type: application/json" \
  -d '{"processKey": "leaveApproval", "variables": {...}}'

# 查询待办任务
curl http://localhost:8084/api/workflow/tasks?assignee=zhangsan

# 完成任务
curl -X POST http://localhost:8084/api/workflow/complete \
  -H "Content-Type: application/json" \
  -d '{"taskId": "xxx", "variables": {"approved": true}}'
```

---

**作者**：itzixiao  
**创建时间**：2026-03-15  
**更新时间**：2026-03-15
