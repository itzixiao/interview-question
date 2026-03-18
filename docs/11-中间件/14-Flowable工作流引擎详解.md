# Flowable 工作流引擎详解

## 一、系统架构

### 1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend                                │
│                     ┌──────────────┐                            │
│                     │  Vue3 前端   │                            │
│                     └──────┬───────┘                            │
└────────────────────────────┼────────────────────────────────────┘
                             │ HTTP
┌────────────────────────────┼────────────────────────────────────┐
│                      Backend│                                   │
│  ┌──────────────┐    ┌─────┴──────┐    ┌──────────────┐         │
│  │  Controller  │───▶│  Service   │───▶│   Mapper     │         │
│  │     层       │    │    层      │    │    层        │         │
│  └──────────────┘    └─────┬──────┘    └──────┬───────┘         │
│                            │                   │                │
│                            ▼                   ▼                │
│                   ┌─────────────────┐    ┌──────────────┐       │
│                   │   Flowable      │    │   业务表     │       │
│                   │  ┌───────────┐  │    └──────────────┘       │
│                   │  │ Runtime   │  │                           │
│                   │  │  Service  │──┼──▶ StartListener         │
│                   │  ├───────────┤  │                           │
│                   │  │   Task    │──┼──▶ TaskListener          │
│                   │  │  Service  │  │                           │
│                   │  ├───────────┤  │                           │
│                   │  │  History  │  │    ┌──────────────┐       │
│                   │  │  Service  │  │    │ Flowable表   │       │
│                   │  ├───────────┤  │    └──────────────┘       │
│                   │  │ Repository│  │                           │
│                   │  │  Service  │──┼──▶ EndListener           │
│                   │  └───────────┘  │                           │
│                   └─────────────────┘                           │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 核心组件说明

| 组件 | 职责 |
|------|------|
| **RuntimeService** | 启动流程实例、查询运行中流程、设置流程变量 |
| **TaskService** | 查询待办任务、完成任务、签收/委派任务 |
| **HistoryService** | 查询历史流程实例、历史任务、审批轨迹 |
| **RepositoryService** | 部署流程定义、查询流程模型 |

---

## 二、请假审批流程

### 2.1 流程图

```
                              ┌──────────────┐
                              │     开始     │
                              └──────┬───────┘
                                     │
                                     ▼
                         ┌───────────────────────┐
                         │      请假天数判断      │
                         └───────────┬───────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                                 │
                 ≤3天                              >3天
                    │                                 │
                    ▼                                 ▼
         ┌─────────────────────┐           ┌─────────────────────┐
         │    部门经理审批     │           │    部门经理审批     │
         └──────────┬──────────┘           └──────────┬──────────┘
                    │                                 │
                    ▼                                 ▼
         ┌─────────────────────┐           ┌─────────────────────┐
         │      审批结果       │           │      审批结果       │
         └──────────┬──────────┘           └──────────┬──────────┘
                    │                                 │
        ┌───────────┴───────────┐                     │
        │                       │                     │
      通过                    驳回                  驳回
        │                       │                     │
        ▼                       ▼                     ▼
┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
│  是否需要总经理审批  │ │    申请人修改       │ │    申请人修改       │
└──────────┬──────────┘ └──────────┬──────────┘ └──────────┬──────────┘
           │                       │                     │
    ┌──────┴──────┐                │                     │
    │             │                │                     │
  ≤3天         >3天                │                     │
    │             │                │                     │
    ▼             ▼                ▼                     ▼
┌────────┐ ┌──────────────┐ ┌─────────────────────┐ ┌──────────────┐
│ 已通过 │ │  总经理审批  │ │      操作选择       │ │    已撤回    │
└────────┘ └──────┬───────┘ └──────────┬──────────┘ └──────────────┘
                  │                    │
         ┌────────┴────────┐  ┌────────┴────────┐
         │                 │  │                 │
       通过              驳回               重新提交
         │                 │  │                 │
         ▼                 │  ▼                 │
    ┌────────┐             │  │                 │
    │ 已通过 │             │  │                 │
    └────────┘             └──┴─────────────────┘
```

### 2.2 流程规则

| 条件 | 审批路径 |
|------|---------|
| 请假 ≤ 3 天 | 部门经理审批 → 通过 |
| 请假 > 3 天 | 部门经理审批 → 总经理审批 → 通过 |
| 被驳回 | 可重新提交或撤回 |

---

## 三、报销审批流程

### 3.1 流程图

```
                              ┌──────────────┐
                              │     开始     │
                              └──────┬───────┘
                                     │
                                     ▼
                         ┌───────────────────────┐
                         │      报销金额判断      │
                         └───────────┬───────────┘
                                     │
           ┌─────────────────────────┼─────────────────────────┐
           │                         │                         │
        <1000元              1000-5000元                 >5000元
           │                         │                         │
           ▼                         ▼                         ▼
┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
│    部门经理审批     │ │    财务经理审批     │ │    总经理审批       │
└──────────┬──────────┘ └──────────┬──────────┘ └──────────┬──────────┘
           │                       │                       │
           ▼                       ▼                       ▼
┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
│      审批结果       │ │      审批结果       │ │      审批结果       │
└──────────┬──────────┘ └──────────┬──────────┘ └──────────┬──────────┘
           │                       │                       │
    ┌──────┴──────┐         ┌──────┴──────┐         ┌──────┴──────┐
    │             │         │             │         │             │
  通过          驳回      通过          驳回      通过          驳回
    │             │         │             │         │             │
    ▼             │         ▼             │         ▼             │
┌────────┐        │   ┌────────┐          │   ┌────────┐          │
│ 已通过 │        │   │ 已通过 │          │   │ 已通过 │          │
└────────┘        │   └────────┘          │   └────────┘          │
                  │                       │                       │
                  └───────────────────────┼───────────────────────┘
                                          │
                                          ▼
                              ┌─────────────────────┐
                              │    申请人修改       │
                              └──────────┬──────────┘
                                         │
                                         ▼
                              ┌─────────────────────┐
                              │      操作选择       │
                              └──────────┬──────────┘
                                         │
                              ┌──────────┴──────────┐
                              │                     │
                           重新提交               撤回
                              │                     │
                              ▼                     ▼
                              │              ┌──────────────┐
                              │              │    已撤回    │
                              │              └──────────────┘
                              │
                    (返回金额判断)
```

### 3.2 流程规则

| 金额区间 | 审批人 |
|---------|--------|
| < 1000 元 | 部门经理 |
| 1000-5000 元 | 财务经理 |
| > 5000 元 | 总经理 |

---

## 四、核心代码实现

### 4.1 流程定义 (BPMN 2.0)

```xml
<!-- leave-process.bpmn20.xml -->
<process id="leave_process" name="请假审批流程" isExecutable="true">
    
    <!-- 开始节点 - 绑定启动监听器 -->
    <startEvent id="startEvent" name="申请开始">
        <extensionElements>
            <flowable:executionListener event="start"
                class="cn.itzixiao.interview.workflow.listener.LeaveStartListener"/>
        </extensionElements>
    </startEvent>

    <!-- 用户任务 - 动态分配审批人 -->
    <userTask id="deptManagerApproval" name="部门经理审批"
              flowable:assignee="${deptManagerUsername}"
              flowable:candidateGroups="DEPT_MANAGER">
        <extensionElements>
            <flowable:taskListener event="create"
                class="cn.itzixiao.interview.workflow.listener.DeptManagerTaskListener"/>
        </extensionElements>
    </userTask>

    <!-- 排他网关 - 条件分支 -->
    <exclusiveGateway id="daysGateway" name="请假天数判断"/>
    
    <!-- 条件连线 -->
    <sequenceFlow id="flow_days_to_dept" sourceRef="daysGateway" targetRef="deptManagerApproval">
        <conditionExpression xsi:type="tFormalExpression">
            <![CDATA[${leaveDays <= 3}]]>
        </conditionExpression>
    </sequenceFlow>

    <!-- 结束节点 - 绑定结束监听器 -->
    <endEvent id="approvedEnd" name="审批通过">
        <extensionElements>
            <flowable:executionListener event="end"
                class="cn.itzixiao.interview.workflow.listener.ApprovalEndListener"/>
        </extensionElements>
    </endEvent>
</process>
```

### 4.2 启动流程实例

```java
@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private RuntimeService runtimeService;

    @Transactional
    public Long apply(LeaveApplyDTO dto) {
        // 1. 保存业务数据
        Leave leave = new Leave();
        leave.setApplicantId(currentUser.getId());
        leave.setLeaveDays(leaveDays);
        leave.setStatus(1); // 审批中
        leaveMapper.insert(leave);

        // 2. 构建流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicantUsername", currentUser.getUsername());
        variables.put("deptId", currentUser.getDeptId());
        variables.put("leaveDays", leaveDays);

        // 3. 启动流程实例
        // businessKey 格式: "LEAVE:业务ID" - 关联业务数据
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            "leave_process",        // 流程定义Key
            "LEAVE:" + leave.getId(), // businessKey
            variables               // 流程变量
        );

        // 4. 回填流程实例ID
        leave.setProcessInstanceId(instance.getId());
        leaveMapper.updateById(leave);
        
        return leave.getId();
    }
}
```

### 4.3 审批任务处理

```java
@Transactional
public void approve(ApprovalDTO dto) {
    String username = getCurrentUsername();
    
    // 1. 查询待办任务
    Task task = taskService.createTaskQuery()
            .taskId(dto.getTaskId())
            .taskAssignee(username)  // 确保是当前用户的任务
            .singleResult();

    if (task == null) {
        throw new RuntimeException("任务不存在或无权限");
    }

    // 2. 添加审批意见
    taskService.addComment(dto.getTaskId(), task.getProcessInstanceId(), dto.getComment());

    // 3. 设置审批变量（用于网关判断）
    Map<String, Object> variables = new HashMap<>();
    if ("部门经理审批".equals(task.getName())) {
        variables.put("deptApproved", dto.getApproved());
    } else if ("总经理审批".equals(task.getName())) {
        variables.put("gmApproved", dto.getApproved());
    }

    // 4. 完成任务 - 流程自动流转到下一节点
    taskService.complete(dto.getTaskId(), variables);
}
```

### 4.4 任务监听器 - 动态分配审批人

```java
@Component
public class DeptManagerTaskListener implements TaskListener {

    private static UserMapper staticUserMapper;
    private static DeptMapper staticDeptMapper;

    @PostConstruct
    public void init() {
        staticUserMapper = applicationContext.getBean(UserMapper.class);
        staticDeptMapper = applicationContext.getBean(DeptMapper.class);
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        // 1. 从流程变量获取部门ID
        Long deptId = (Long) delegateTask.getVariable("deptId");
        
        // 2. 查询部门经理
        Dept dept = staticDeptMapper.selectById(deptId);
        User manager = staticUserMapper.selectById(dept.getManagerId());
        
        // 3. 动态设置任务审批人
        delegateTask.setAssignee(manager.getUsername());
        
        log.info("部门经理审批任务已分配给: {}", manager.getUsername());
    }
}
```

### 4.5 结束监听器 - 更新业务状态

```java
@Component("approvalEndListener")
public class ApprovalEndListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        String businessKey = execution.getProcessInstanceBusinessKey();
        String processDefinitionKey = execution.getProcessDefinitionId().split(":")[0];

        if ("leave_process".equals(processDefinitionKey)) {
            // 解析 businessKey: "LEAVE:123" -> 123
            Long leaveId = parseBusinessKey(businessKey, "LEAVE:");
            
            Leave leave = leaveMapper.selectById(leaveId);
            leave.setStatus(2);  // 已通过
            leave.setCurrentNode("已通过");
            leaveMapper.updateById(leave);
        }
    }

    private Long parseBusinessKey(String businessKey, String prefix) {
        if (businessKey.startsWith(prefix)) {
            return Long.parseLong(businessKey.substring(prefix.length()));
        }
        return Long.parseLong(businessKey); // 兼容纯数字格式
    }
}
```

### 4.6 查询审批历史

```java
public List<ProcessHistoryVO> getApprovalHistory(String processInstanceId) {
    // 查询历史任务（按时间正序）
    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
            .processInstanceId(processInstanceId)
            .orderByHistoricTaskInstanceEndTime().asc()
            .list();

    return tasks.stream().map(task -> {
        ProcessHistoryVO vo = new ProcessHistoryVO();
        vo.setTaskName(task.getName());
        vo.setAssignee(task.getAssignee());
        vo.setStartTime(task.getStartTime());
        vo.setEndTime(task.getEndTime());
        
        // 获取审批意见
        List<Comment> comments = taskService.getTaskComments(task.getId());
        if (!comments.isEmpty()) {
            vo.setComment(comments.get(0).getFullMessage());
        }
        return vo;
    }).collect(Collectors.toList());
}
```

---

## 五、Flowable 核心表说明

### 5.1 运行时表 (ACT_RU_*)

| 表名 | 说明 |
|------|------|
| ACT_RU_EXECUTION | 流程执行实例表 |
| ACT_RU_TASK | 运行时任务表（待办） |
| ACT_RU_VARIABLE | 运行时变量表 |
| ACT_RU_IDENTITYLINK | 任务参与者关系表 |

### 5.2 历史表 (ACT_HI_*)

| 表名 | 说明 |
|------|------|
| ACT_HI_PROCINST | 历史流程实例表 |
| ACT_HI_TASKINST | 历史任务表 |
| ACT_HI_ACTINST | 历史活动（节点）表 |
| ACT_HI_VARINST | 历史变量表 |
| ACT_HI_COMMENT | 审批意见表 |

### 5.3 通用表 (ACT_GE_*)

| 表名 | 说明 |
|------|------|
| ACT_GE_BYTEARRAY | 二进制资源（流程图、表单等） |
| ACT_GE_PROPERTY | 引擎属性配置 |

### 5.4 流程定义表 (ACT_RE_*)

| 表名 | 说明 |
|------|------|
| ACT_RE_DEPLOYMENT | 部署信息表 |
| ACT_RE_PROCDEF | 流程定义表 |
| ACT_RE_MODEL | 流程模型表 |

---

## 六、Flowable 高频面试题

### 6.1 基础概念

**Q1: Flowable 和 Activiti 有什么区别？**

| 对比项 | Flowable | Activiti |
|--------|----------|----------|
| 起源 | Activiti 核心团队分离后创建 | Alfresco 公司开发 |
| 版本 | 活跃更新，支持 Java 17 | 7.x 后闭源（Alfresco） |
| 性能 | 优化更好，支持异步执行 | 相对较慢 |
| CMMN/DMN | 完整支持 | 部分支持 |
| 推荐 | **新项目首选** | 老项目维护 |

**Q2: 什么是 businessKey？有什么作用？**

```java
// businessKey 是流程实例与业务数据的关联标识
ProcessInstance instance = runtimeService.startProcessInstanceByKey(
    "leave_process",
    "LEAVE:123"  // businessKey = 业务类型:业务ID
);

// 通过 businessKey 反查流程实例
ProcessInstance pi = runtimeService.createProcessInstanceQuery()
    .processInstanceBusinessKey("LEAVE:123")
    .singleResult();
```

**作用：**
1. 关联业务表与流程实例
2. 在监听器中获取业务ID更新状态
3. 避免在流程变量中存储大量业务数据

**Q3: 流程变量 (Variables) 的作用域有哪些？**

| 作用域 | 说明 | 使用场景 |
|--------|------|---------|
| 全局变量 | 整个流程实例共享 | 申请人、审批天数 |
| 本地变量 | 仅当前任务可见 | 临时计算结果 |

```java
// 全局变量
runtimeService.setVariable(executionId, "leaveDays", 5);

// 本地变量
taskService.setVariableLocal(taskId, "tempResult", "xxx");
```

---

### 6.2 流程设计

**Q4: 排他网关与并行网关的区别？**

| 类型 | 符号 | 行为 |
|------|------|------|
| 排他网关 (Exclusive) | `X` | 只走满足条件的**一条**分支 |
| 并行网关 (Parallel) | `+` | **同时**走所有分支，汇聚时等待所有分支完成 |
| 包容网关 (Inclusive) | `○` | 走所有满足条件的分支 |

```xml
<!-- 排他网关示例 -->
<exclusiveGateway id="daysGateway"/>
<sequenceFlow sourceRef="daysGateway" targetRef="deptApproval">
    <conditionExpression>${leaveDays <= 3}</conditionExpression>
</sequenceFlow>
<sequenceFlow sourceRef="daysGateway" targetRef="gmApproval">
    <conditionExpression>${leaveDays > 3}</conditionExpression>
</sequenceFlow>
```

**Q5: 如何实现会签（多人审批）？**

```xml
<!-- 多实例任务 - 会签 -->
<userTask id="multiApproval" name="部门会签">
    <multiInstanceLoopCharacteristics 
        isSequential="false"
        flowable:collection="${approverList}"
        flowable:elementVariable="approver">
        <!-- 完成条件：所有人都同意才通过 -->
        <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>
    </multiInstanceLoopCharacteristics>
</userTask>
```

| 属性 | 说明 |
|------|------|
| isSequential="false" | 并行会签（同时审批） |
| isSequential="true" | 串行会签（依次审批） |
| nrOfInstances | 总实例数 |
| nrOfCompletedInstances | 已完成实例数 |

---

### 6.3 监听器

**Q6: ExecutionListener 和 TaskListener 的区别？**

| 类型 | 监听对象 | 事件类型 | 使用场景 |
|------|---------|---------|---------|
| ExecutionListener | 流程执行 | start/end/take | 流程开始/结束、连线经过 |
| TaskListener | 用户任务 | create/assignment/complete/delete | 任务创建、分配、完成 |

```java
// ExecutionListener - 流程结束时更新业务状态
@Override
public void notify(DelegateExecution execution) {
    String businessKey = execution.getProcessInstanceBusinessKey();
    // 更新业务表状态
}

// TaskListener - 任务创建时动态分配审批人
@Override
public void notify(DelegateTask task) {
    Long deptId = (Long) task.getVariable("deptId");
    task.setAssignee(findDeptManager(deptId));
}
```

**Q7: 监听器中如何注入 Spring Bean？**

Flowable 监听器由引擎实例化，不受 Spring 管理，直接 `@Autowired` 无效。

**解决方案：静态字段 + @PostConstruct**

```java
@Component
public class ApprovalEndListener implements ExecutionListener {

    @Autowired
    private ApplicationContext applicationContext;

    // 静态字段存储 Bean
    private static LeaveMapper staticLeaveMapper;

    @PostConstruct
    public void init() {
        // 应用启动时注入
        staticLeaveMapper = applicationContext.getBean(LeaveMapper.class);
    }

    @Override
    public void notify(DelegateExecution execution) {
        // 使用静态字段访问 Mapper
        staticLeaveMapper.updateById(leave);
    }
}
```

---

### 6.4 性能与优化

**Q8: 流程实例数据量大时如何优化？**

1. **历史数据清理**
```yaml
flowable:
  history-level: audit  # 只记录关键数据
  async-executor-activate: true  # 异步执行器
```

2. **定期归档历史表**
```sql
-- 归档已完成流程
INSERT INTO act_hi_procinst_archive 
SELECT * FROM act_hi_procinst WHERE end_time_ < DATE_SUB(NOW(), INTERVAL 6 MONTH);
DELETE FROM act_hi_procinst WHERE end_time_ < DATE_SUB(NOW(), INTERVAL 6 MONTH);
```

3. **异步执行**
```xml
<serviceTask id="asyncTask" flowable:async="true" />
```

**Q9: 如何实现流程的版本管理？**

```java
// 部署新版本流程
repositoryService.createDeployment()
    .addClasspathResource("processes/leave-v2.bpmn20.xml")
    .deploy();

// 查询最新版本
ProcessDefinition latest = repositoryService.createProcessDefinitionQuery()
    .processDefinitionKey("leave_process")
    .latestVersion()
    .singleResult();

// 迁移运行中实例到新版本
runtimeService.createChangeActivityStateBuilder()
    .processInstanceId(instanceId)
    .moveActivityIdTo("oldTask", "newTask")
    .changeState();
```

---

### 6.5 实战问题

**Q10: 如何实现流程回退（驳回到指定节点）？**

```java
public void rejectToNode(String taskId, String targetNodeId) {
    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
    
    // 使用 ChangeActivityStateBuilder 回退
    runtimeService.createChangeActivityStateBuilder()
        .processInstanceId(task.getProcessInstanceId())
        .moveActivityIdTo(task.getTaskDefinitionKey(), targetNodeId)
        .changeState();
}
```

**Q11: 如何获取流程图高亮当前节点？**

```java
public byte[] getProcessDiagram(String processInstanceId) {
    // 获取流程实例
    ProcessInstance instance = runtimeService.createProcessInstanceQuery()
        .processInstanceId(processInstanceId).singleResult();
    
    // 获取当前活动节点
    List<String> activeIds = runtimeService.getActiveActivityIds(processInstanceId);
    
    // 获取流程定义
    BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
    
    // 生成高亮流程图
    ProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
    return generator.generateDiagram(
        bpmnModel, 
        activeIds,           // 高亮节点
        Collections.emptyList(),
        "宋体", "宋体", "宋体"
    ).readAllBytes();
}
```

**Q12: Flowable 与业务系统如何解耦？**

1. **使用 businessKey 而非流程变量传递业务数据**
2. **监听器只负责状态同步，业务逻辑在 Service 层**
3. **使用消息事件实现异步通知**

```java
// 好的做法：businessKey 关联
runtimeService.startProcessInstanceByKey("leave", "LEAVE:123");

// 不推荐：大量业务数据放入流程变量
Map<String, Object> vars = new HashMap<>();
vars.put("leaveEntity", leave);  // 对象序列化影响性能
```

---

## 七、项目实践总结

### 7.1 最佳实践

1. **businessKey 命名规范**：`业务类型:业务ID`，如 `LEAVE:123`、`EXPENSE:456`
2. **流程变量精简**：只存储流程流转必需的数据
3. **监听器职责单一**：启动监听器初始化变量，任务监听器分配审批人，结束监听器更新状态
4. **统一异常处理**：监听器内捕获异常避免流程中断

### 7.2 常见坑点

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 监听器 Bean 注入为 null | Flowable 自行实例化 | 静态字段 + @PostConstruct |
| businessKey 解析失败 | 格式不统一（纯数字 vs 前缀:ID） | parseBusinessKey 方法兼容两种格式 |
| 流程结束但状态未更新 | 没有配置结束监听器 | 在 endEvent 上绑定 ExecutionListener |
| 网关条件不生效 | 变量名拼写错误 | 检查 BPMN 中的 `${}` 表达式 |

---

## 八、参考资料

- [Flowable 官方文档](https://www.flowable.com/open-source/docs/)
- [BPMN 2.0 规范](https://www.omg.org/spec/BPMN/2.0/)
- [Flowable GitHub](https://github.com/flowable/flowable-engine)
