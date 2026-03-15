# 规则引擎 Drools

## 一、Drools 规则基础

### 1.1 规则结构

```drools
rule "规则名称"
when
    // 条件部分（LHS）
    $fact: FactType(condition)
then
    // 结果部分（RHS）
    // 执行动作
end
```

---

## 二、规则示例 - 订单折扣

### 2.1 规则文件

```drools
// 规则文件：discount.drl
package com.example.rules;

import com.example.Order;

rule "VIP 客户 9 折"
when
    $order: Order(customerType == "VIP", amount > 1000)
then
    $order.setDiscount(0.9);
    System.out.println("VIP 客户享受 9 折优惠");
end

rule "普通客户满 1000 减 100"
when
    $order: Order(customerType == "NORMAL", amount > 1000)
then
    $order.setDiscountAmount(100.0);
    System.out.println("普通客户满 1000 减 100");
end

rule "新用户首单 8 折"
when
    $order: Order(isFirstOrder == true, amount > 0)
then
    $order.setDiscount(0.8);
    System.out.println("新用户首单 8 折优惠");
end
```

---

## 三、Drools 集成

### 3.1 Spring Boot集成

```java
@Service
public class RuleEngineService {
    
    @Autowired
    private KieContainer kieContainer;
    
    /**
     * 初始化加载规则
     */
    @PostConstruct
    public void init() {
        // 从 classpath 加载规则
        KieServices kieServices = KieServices.Factory.get();
        kieContainer = kieServices.getKieClasspathContainer();
    }
    
    /**
     * 执行规则
     */
    public void executeRules(Object fact) {
        KieSession kieSession = kieContainer.newKieSession();
        
        try {
            // 插入事实
            kieSession.insert(fact);
            
            // 触发规则
            int firedRules = kieSession.fireAllRules();
            log.info("触发了 {} 条规则", firedRules);
        } finally {
            kieSession.dispose();
        }
    }
    
    /**
     * 执行规则并返回结果
     */
    public <T> T executeRulesWithResult(Object fact, Class<T> resultClass) {
        KieSession kieSession = kieContainer.newKieSession();
        
        try {
            kieSession.insert(fact);
            kieSession.fireAllRules();
            
            // 获取结果
            Collection<?> results = kieSession.getFactHandles()
                .stream()
                .map(fh -> fh.getObject())
                .filter(obj -> resultClass.isInstance(obj))
                .collect(Collectors.toList());
            
            return results.isEmpty() ? null : resultClass.cast(results.iterator().next());
        } finally {
            kieSession.dispose();
        }
    }
}
```

---

## 四、动态加载规则

### 4.1 从数据库加载

```java
@Service
public class DynamicRuleService {
    
    @Autowired
    private RuleMapper ruleMapper; // 从数据库读取规则
    
    /**
     * 动态加载规则
     */
    public KieContainer reloadRules() {
        // 1. 从数据库读取规则
        List<RuleEntity> rules = ruleMapper.selectAll();
        
        // 2. 构建 KieModule
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kfs = kieServices.newKieFileSystem();
        
        // 3. 写入规则
        for (RuleEntity rule : rules) {
            String drlContent = rule.getDrlContent();
            kfs.write("src/main/resources/rules/" + rule.getRuleName() + ".drl", drlContent);
        }
        
        // 4. 构建并返回 KieContainer
        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }
}
```

---

## 五、规则冲突解决

### 5.1 优先级设置

```drools
rule "高优先级规则"
    salience 100  // 优先级数值，越大越先执行
when
    $order: Order(amount > 10000)
then
    $order.setPriority(true);
end

rule "低优先级规则"
    salience 10
when
    $order: Order(amount > 5000)
then
    $order.setVip(true);
end
```

### 5.2 激活组

```drools
rule "规则 A"
    activation-group "group1"  // 同组只能执行一个
when
    condition1
then
    action1
end

rule "规则 B"
    activation-group "group1"
when
    condition2
then
    action2
end
```

---

## 六、规则引擎面试题

### 1. Drools 的工作原理？

**参考答案：**

**工作流程：**
1. **解析规则**：将 DRL 文件解析为内部表示
2. **编译规则**：生成 Java 字节码
3. **加载规则**：加载到 KieContainer
4. **创建会话**：新建 KieSession
5. **插入事实**：将数据插入工作内存
6. **模式匹配**：Rete 算法匹配条件和事实
7. **触发规则**：执行满足条件的规则动作

**核心算法：Rete OO**
- 增量式匹配
- 避免重复计算
- 高效处理大量规则

### 2. 规则文件的语法结构？

**参考答案：**

**基本结构：**
```drools
rule "规则名"
    attributes  // 规则属性
when
    LHS  // 条件部分
then
    RHS  // 结果部分
end
```

**常用属性：**
- `salience`: 优先级
- `activation-group`: 激活组
- `agenda-group`: 议程组
- `enabled`: 是否启用
- `no-loop`: 防止死循环

**LHS 条件：**
- 字段约束：`$order: Order(amount > 1000)`
- 绑定变量：`$amount: amount`
- 复合条件：`and`, `or`, `not`, `exists`

**RHS 动作：**
- 修改事实：`modify($order) { setAmount(2000) }`
- 插入新事实：`insert(new Fact())`
- 删除事实：`delete($order)`

### 3. 如何动态加载规则？

**参考答案：**

**方案一：从数据库加载**
1. 将规则存储在数据库
2. 运行时读取规则内容
3. 使用 KieFileSystem 动态写入
4. 重新构建 KieContainer

**方案二：从文件系统加载**
1. 监控规则文件变化
2. 自动重新编译加载

**方案三：热部署**
1. 使用 KieScanner 监听 Maven 仓库
2. 自动更新规则版本

**注意事项：**
- 规则验证（语法检查）
- 版本管理
- 回滚机制

### 4. 规则冲突如何解决？

**参考答案：**

**冲突场景：** 多条规则同时满足条件

**解决策略：**
1. **优先级（Salience）**：数值大的先执行
2. **激活组（Activation Group）**：同组只执行一个
3. **议程组（Agenda Group）**：分组控制执行顺序
4. **LIFO/FIFO**：后进先出/先进先出

**选择建议：**
- 业务优先级明确：用 Salience
- 互斥规则：用 Activation Group
- 复杂场景：组合使用

### 5. 规则引擎的适用场景？

**参考答案：**

**适合场景：**
✅ 业务规则频繁变化  
✅ 规则数量多且复杂  
✅ 需要业务人员理解规则  
✅ 决策逻辑标准化  
✅ 需要规则版本管理

**典型应用：**
- 信贷审批系统
- 保险理赔
- 电商促销
- 风控系统
- 智能推荐

**不适合场景：**
❌ 规则固定不变  
❌ 简单 if-else 就能解决  
❌ 性能要求极高（规则引擎有开销）

---

## 七、API 接口测试

```bash
# 执行规则引擎
curl -X POST http://localhost:8084/api/rules/execute \
  -H "Content-Type: application/json" \
  -d '{"customerType": "VIP", "amount": 1500}'

# 动态加载规则
curl -X POST http://localhost:8084/api/rules/reload

# 查询已加载规则
curl http://localhost:8084/api/rules/list
```

---

**作者**：itzixiao  
**创建时间**：2026-03-15  
**更新时间**：2026-03-15
