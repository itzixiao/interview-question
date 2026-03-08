package cn.itzixiao.interview.spring.transaction;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Spring 事务完全指南 - 失效场景 + 传播机制 + 隔离级别 + 最佳实践
 * 
 * 本示例包含：
 * 1. 8 种事务失效场景详解
 * 2. 7 种事务传播行为演示
 * 3. 4 种事务隔离级别说明
 * 4. 编程式事务 vs 声明式事务
 * 5. 事务最佳实践与避坑指南
 */
@SpringBootApplication
public class TransactionCompleteGuide implements CommandLineRunner {
    
    @Autowired
    private TransactionFailureService failureService;
    
    @Autowired
    private TransactionPropagationService propagationService;
    
    @Autowired
    private IsolationLevelService isolationService;
    
    @Autowired
    private BestPracticeService bestPracticeService;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    public static void main(String[] args) {
        SpringApplication.run(TransactionCompleteGuide.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║      Spring 事务完全指南 - 失效 + 传播 + 隔离          ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        
        // 初始化数据
        initDatabase();
        
        // Part 1: 事务失效场景测试
        testTransactionFailures();
        
        // Part 2: 事务传播行为测试
        testPropagationBehaviors();
        
        // Part 3: 事务隔离级别测试
        testIsolationLevels();
        
        // Part 4: 最佳实践演示
        testBestPractices();
        
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║              所有测试完成！                          ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");
    }
    
    private void initDatabase() {
        System.out.println("\n【准备】初始化数据库表结构...\n");
        failureService.initTables();
    }
    
    // ==================== Part 1: 事务失效场景 ====================
    
    private void testTransactionFailures() {
        System.out.println("\n");
        System.out.println("┌──────────────────────────────────────────────────┐");
        System.out.println("│  Part 1: Spring 事务失效 8 大场景详解                  │");
        System.out.println("└──────────────────────────────────────────────────┘");
        
        // 场景 1: 非 public 方法
        System.out.println("\n【场景 1】非 public 方法上的@Transactional 失效");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        failureService.resetData();
        try {
            failureService.testNonPublicMethod();
        } catch (Exception e) {
            System.out.println("  ⚠ 捕获异常：" + e.getMessage());
        }
        printUsers("非 public 方法");
        
        // 场景 2: 自调用问题
        System.out.println("\n【场景 2】同类自调用导致事务失效");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        failureService.resetData();
        try {
            failureService.testSelfInvocation();
        } catch (Exception e) {
            System.out.println("  ⚠ 捕获异常：" + e.getMessage());
        }
        printUsers("自调用");
        
        // 场景 3: 异常被吞掉
        System.out.println("\n【场景 3】异常被捕获未抛出，事务不回滚");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        failureService.resetData();
        failureService.testSwallowedException();
        printUsers("异常被吞");
        
        // 场景 4: checked Exception
        System.out.println("\n【场景 4】checked Exception 默认不回滚");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        failureService.resetData();
        try {
            failureService.testCheckedException();
        } catch (Exception e) {
            System.out.println("  ⚠ 捕获异常：" + e.getClass().getSimpleName());
        }
        printUsers("checked 异常（默认）");
        
        // 场景 5: rollbackFor 配置
        System.out.println("\n【场景 5】配置 rollbackFor 后 checked Exception 回滚");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        failureService.resetData();
        try {
            failureService.testRollbackForChecked();
        } catch (Exception e) {
            System.out.println("  ⚠ 捕获异常：" + e.getClass().getSimpleName());
        }
        printUsers("rollbackFor");
        
        // 场景 6: 代理对象问题
        System.out.println("\n【场景 6】非代理对象调用导致事务失效");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        failureService.resetData();
        try {
            failureService.testDirectCall();
        } catch (Exception e) {
            System.out.println("  ⚠ 捕获异常：" + e.getMessage());
        }
        printUsers("直接调用");
        
        // 场景 7: 异步方法
        System.out.println("\n【场景 7】异步方法中的事务失效");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        failureService.resetData();
        failureService.testAsyncMethod();
        try {
            Thread.sleep(1000); // 等待异步执行
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        printUsers("异步方法");
        
        // 场景 8: 数据库引擎不支持
        System.out.println("\n【场景 8】数据库引擎不支持事务（如 MyISAM）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  ℹ 提示：MySQL 使用 InnoDB 才支持事务");
        System.out.println("  ✓ 本示例使用 H2 内存数据库，默认支持事务");
    }
    
    private void printUsers(String scenario) {
        List<Map<String, Object>> users = failureService.listUsers();
        System.out.println("\n  📊 【" + scenario + "】结果:");
        System.out.println("     用户数据：" + users);
        System.out.println("     结论：" + (users.isEmpty() ? "✓ 事务已回滚" : "✗ 事务失效"));
    }
    
    // ==================== Part 2: 事务传播行为 ====================
    
    private void testPropagationBehaviors() {
        System.out.println("\n\n");
        System.out.println("┌──────────────────────────────────────────────────┐");
        System.out.println("│  Part 2: Spring 事务传播行为详解                      │");
        System.out.println("└──────────────────────────────────────────────────┘");
        
        // REQUIRED
        System.out.println("\n【传播行为 1】REQUIRED（默认）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  规则：当前有事务则加入，无则新建");
        propagationService.testRequired();
        
        // REQUIRES_NEW
        System.out.println("\n【传播行为 2】REQUIRES_NEW");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  规则：挂起当前事务，新建独立事务");
        propagationService.testRequiresNew();
        
        // NESTED
        System.out.println("\n【传播行为 3】NESTED");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  规则：在当前事务中创建嵌套事务（savepoint）");
        propagationService.testNested();
        
        // SUPPORTS
        System.out.println("\n【传播行为 4】SUPPORTS");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  规则：当前有事务则加入，无则以非事务执行");
        propagationService.testSupports();
        
        // NOT_SUPPORTED
        System.out.println("\n【传播行为 5】NOT_SUPPORTED");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  规则：挂起当前事务，以非事务执行");
        propagationService.testNotSupported();
        
        // MANDATORY
        System.out.println("\n【传播行为 6】MANDATORY");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  规则：当前必须有事务，否则抛出异常");
        propagationService.testMandatory();
        
        // NEVER
        System.out.println("\n【传播行为 7】NEVER");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  规则：当前必须无事务，否则抛出异常");
        propagationService.testNever();
    }
    
    // ==================== Part 3: 事务隔离级别 ====================
    
    private void testIsolationLevels() {
        System.out.println("\n\n");
        System.out.println("┌──────────────────────────────────────────────────┐");
        System.out.println("│  Part 3: 事务隔离级别详解                             │");
        System.out.println("└──────────────────────────────────────────────────┘");
        
        System.out.println("\n【隔离级别对比】");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  READ_UNCOMMITTED   - 读未提交（可能脏读）");
        System.out.println("  READ_COMMITTED     - 读已提交（避免脏读）");
        System.out.println("  REPEATABLE_READ    - 可重复读（避免不可重复读）");
        System.out.println("  SERIALIZABLE       - 串行化（避免幻读）");
        
        System.out.println("\n【MySQL 默认隔离级别】REPEATABLE_READ");
        System.out.println("【Oracle 默认隔离级别】READ_COMMITTED");
        
        // 演示不同隔离级别的读取行为
        System.out.println("\n【演示】不同隔离级别下的查询");
        isolationService.demoIsolationLevels();
    }
    
    // ==================== Part 4: 最佳实践 ====================
    
    private void testBestPractices() {
        System.out.println("\n\n");
        System.out.println("┌──────────────────────────────────────────────────┐");
        System.out.println("│  Part 4: Spring 事务最佳实践                           │");
        System.out.println("└──────────────────────────────────────────────────┘");
        
        System.out.println("\n【最佳实践 1】声明式事务（推荐）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        bestPracticeService.declarativeTransaction();
        
        System.out.println("\n【最佳实践 2】编程式事务（复杂场景）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        bestPracticeService.programmaticTransaction();
        
        System.out.println("\n【最佳实践 3】只读事务优化");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        bestPracticeService.readOnlyTransaction();
        
        System.out.println("\n【最佳实践 4】事务拆分（避免大事务）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        bestPracticeService.splitTransactions();
        
        System.out.println("\n【最佳实践 5】日志记录使用 REQUIRES_NEW");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        bestPracticeService.logWithRequiresNew();
    }
}

// ==================== Part 1: 事务失效场景 Service ====================

@Service
class TransactionFailureService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    public void initTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS tx_user");
        jdbcTemplate.execute("CREATE TABLE tx_user (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(50))");
    }
    
    public void resetData() {
        jdbcTemplate.execute("DELETE FROM tx_user");
    }
    
    public List<Map<String, Object>> listUsers() {
        return jdbcTemplate.queryForList("SELECT * FROM tx_user");
    }
    
    private void insertUser(String name) {
        jdbcTemplate.update("INSERT INTO tx_user (name) VALUES (?)", name);
        System.out.println("  ✓ 插入用户：" + name);
    }
    
    // ========== 场景 1: 非 public 方法 ==========
    
    /**
     * ❌ 错误示范：private 方法上的@Transactional 不生效
     * 原因：Spring AOP 基于动态代理，只能代理 public 方法
     */
    @Transactional
    private void privateMethodWithTx(String name) {
        System.out.println("  【private 方法】执行事务逻辑");
        insertUser(name);
        throw new RuntimeException("模拟异常");
    }
    
    public void testNonPublicMethod() {
        System.out.println("  → 调用 private 方法上的@Transactional");
        privateMethodWithTx("PrivateUser");
    }
    
    // ========== 场景 2: 自调用问题 ==========
    
    /**
     * ❌ 错误示范：同类内部调用不走代理
     */
    public void testSelfInvocation() {
        System.out.println("  → this 调用内部事务方法");
        this.internalTxMethod("SelfCallUser"); // this 调用，不经过代理
    }
    
    @Transactional
    public void internalTxMethod(String name) {
        System.out.println("  【内部方法】执行事务逻辑");
        insertUser(name);
        throw new RuntimeException("模拟异常");
    }
    
    // ========== 场景 3: 异常被吞掉 ==========
    
    /**
     * ❌ 错误示范：捕获异常未抛出
     */
    @Transactional
    public void testSwallowedException() {
        System.out.println("  → 捕获异常但未抛出");
        insertUser("SwallowUser");
        try {
            throw new RuntimeException("业务异常");
        } catch (Exception e) {
            System.out.println("  ⚠ 异常被吞掉：" + e.getMessage());
            // 未继续抛出，事务不会回滚
        }
    }
    
    // ========== 场景 4: checked Exception ==========
    
    /**
     * ❌ 错误示范：checked Exception 默认不回滚
     */
    @Transactional
    public void testCheckedException() throws IOException {
        System.out.println("  → 抛出 checked Exception（默认不回滚）");
        insertUser("CheckedUser");
        throw new IOException("IO 异常");
    }
    
    // ========== 场景 5: rollbackFor ==========
    
    /**
     * ✓ 正确示范：配置 rollbackFor
     */
    @Transactional(rollbackFor = Exception.class)
    public void testRollbackForChecked() throws IOException {
        System.out.println("  → 配置 rollbackFor = Exception.class");
        insertUser("RollbackForUser");
        throw new IOException("IO 异常");
    }
    
    // ========== 场景 6: 非代理对象调用 ==========
    
    /**
     * ❌ 错误示范：直接 new 对象调用
     */
    public void testDirectCall() {
        System.out.println("  → 直接 new 对象调用（不走 Spring 容器）");
        TransactionFailureService directService = new TransactionFailureService();
        // 绕过 Spring 容器，@Transactional 不生效
        directService.insertUser("DirectCallUser");
        throw new RuntimeException("模拟异常");
    }
    
    // ========== 场景 7: 异步方法 ==========
    
    /**
     * ⚠ 注意：异步方法中的事务可能失效
     */
    public void testAsyncMethod() {
        System.out.println("  → 异步方法中的事务（需要额外配置）");
        new Thread(() -> {
            // 新线程不在 Spring 事务管理中
            insertUser("AsyncUser");
            throw new RuntimeException("异步异常");
        }).start();
    }
}

// ==================== Part 2: 事务传播行为 Service ====================

@Service
class TransactionPropagationService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    @Lazy
    private TransactionPropagationService self;
    
    private void printTxInfo(String operation) {
        boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
        String txName = TransactionSynchronizationManager.getCurrentTransactionName();
        System.out.println("  事务状态：" + (txActive ? "✓ 活跃" : "✗ 无事务") + 
                          (txActive ? ", 名称：" + txName : ""));
    }
    
    // ========== REQUIRED ==========
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void testRequired() {
        System.out.println("  → 外部方法（REQUIRED）");
        printTxInfo("external");
        
        try {
            self.innerRequired();
        } catch (Exception e) {
            System.out.println("  ⚠ 异常：" + e.getMessage());
        }
        
        System.out.println("  结果：外部和内层同回滚");
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void innerRequired() {
        System.out.println("    → 内部方法（REQUIRED）");
        printTxInfo("inner");
        insertData("required_data");
        throw new RuntimeException("模拟异常");
    }
    
    // ========== REQUIRES_NEW ==========
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void testRequiresNew() {
        System.out.println("  → 外部方法（REQUIRED）");
        printTxInfo("external");
        
        insertData("requires_new_outer");
        
        try {
            self.innerRequiresNew();
        } catch (Exception e) {
            System.out.println("  ⚠ 异常：" + e.getMessage());
        }
        
        System.out.println("  结果：内层已提交，外层回滚");
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void innerRequiresNew() {
        System.out.println("    → 内部方法（REQUIRES_NEW）");
        printTxInfo("inner");
        insertData("requires_new_inner");
        throw new RuntimeException("内层异常");
    }
    
    // ========== NESTED ==========
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void testNested() {
        System.out.println("  → 外部方法（REQUIRED）");
        printTxInfo("external");
        
        insertData("nested_outer");
        
        try {
            self.innerNested();
        } catch (Exception e) {
            System.out.println("  ⚠ 嵌套事务异常：" + e.getMessage());
        }
        
        System.out.println("  结果：外层继续执行并提交");
        insertData("nested_continue");
    }
    
    @Transactional(propagation = Propagation.NESTED)
    public void innerNested() {
        System.out.println("    → 内部方法（NESTED）");
        printTxInfo("inner");
        insertData("nested_inner");
        throw new RuntimeException("嵌套事务异常");
    }
    
    // ========== SUPPORTS ==========
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void testSupports() {
        System.out.println("  → 外部有事务");
        printTxInfo("external");
        
        self.innerSupports();
        System.out.println("  结果：SUPPORTS 加入当前事务");
    }
    
    public void testSupportsNoTx() {
        System.out.println("  → 外部无事务");
        printTxInfo("external");
        
        self.innerSupports();
        System.out.println("  结果：SUPPORTS 以非事务执行");
    }
    
    @Transactional(propagation = Propagation.SUPPORTS)
    public void innerSupports() {
        System.out.println("    → 内部方法（SUPPORTS）");
        printTxInfo("inner");
        insertData("supports_data");
    }
    
    // ========== NOT_SUPPORTED ==========
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void testNotSupported() {
        System.out.println("  → 外部方法（REQUIRED）");
        printTxInfo("external");
        
        self.innerNotSupported();
        System.out.println("  结果：内层以非事务执行");
    }
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void innerNotSupported() {
        System.out.println("    → 内部方法（NOT_SUPPORTED）");
        printTxInfo("inner");
        insertData("not_supported_data");
        System.out.println("    ℹ 以非事务方式执行");
    }
    
    // ========== MANDATORY ==========
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void testMandatory() {
        System.out.println("  → 外部有事务");
        printTxInfo("external");
        
        try {
            self.innerMandatory();
            System.out.println("  ✓ MANDATORY 成功加入事务");
        } catch (Exception e) {
            System.out.println("  ⚠ 异常：" + e.getClass().getSimpleName());
        }
    }
    
    public void testMandatoryNoTx() {
        System.out.println("  → 外部无事务");
        printTxInfo("external");
        
        try {
            self.innerMandatory();
        } catch (Exception e) {
            System.out.println("  ⚠ 异常：" + e.getClass().getSimpleName() + 
                              " - " + e.getMessage());
        }
    }
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void innerMandatory() {
        System.out.println("    → 内部方法（MANDATORY）");
        printTxInfo("inner");
        insertData("mandatory_data");
    }
    
    // ========== NEVER ==========
    
    public void testNever() {
        System.out.println("  → 外部无事务");
        printTxInfo("external");
        
        self.innerNever();
        System.out.println("  ✓ NEVER 正常执行");
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void testNeverWithTx() {
        System.out.println("  → 外部有事务");
        printTxInfo("external");
        
        try {
            self.innerNever();
        } catch (Exception e) {
            System.out.println("  ⚠ 异常：" + e.getClass().getSimpleName() + 
                              " - " + e.getMessage());
        }
    }
    
    @Transactional(propagation = Propagation.NEVER)
    public void innerNever() {
        System.out.println("    → 内部方法（NEVER）");
        printTxInfo("inner");
        insertData("never_data");
    }
    
    private void insertData(String name) {
        jdbcTemplate.update("INSERT INTO tx_user (name) VALUES (?)", name);
        System.out.println("    ✓ 插入数据：" + name);
    }
}

// ==================== Part 3: 隔离级别 Service ====================

@Service
class IsolationLevelService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public void demoIsolationLevels() {
        System.out.println("\n  【READ_UNCOMMITTED】可能读到未提交的数据（脏读）");
        System.out.println("  【READ_COMMITTED】只能读到已提交的数据（避免脏读）");
        System.out.println("  【REPEATABLE_READ】同一事务内多次读取结果一致");
        System.out.println("  【SERIALIZABLE】强制排序，避免幻读（性能最低）");
        
        // 实际演示需要在多事务环境下进行
        System.out.println("\n  ℹ 完整演示需要开启多个并发事务");
        System.out.println("  ✓ 生产环境建议：使用数据库默认隔离级别");
    }
}

// ==================== Part 4: 最佳实践 Service ====================

@Service
class BestPracticeService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    @Autowired
    @Lazy
    private BestPracticeService self;
    
    // ========== 最佳实践 1: 声明式事务 ==========
    
    /**
     * ✓ 推荐：声明式事务配置
     */
    @Transactional(
        rollbackFor = Exception.class,  // 指定回滚异常
        timeout = 30,                    // 超时时间（秒）
        readOnly = false,                // 是否只读
        isolation = Isolation.DEFAULT    // 隔离级别
    )
    public void declarativeTransaction() {
        System.out.println("  ✓ 使用注解配置事务属性");
        System.out.println("  ✓ 代码简洁，易于维护");
    }
    
    // ========== 最佳实践 2: 编程式事务 ==========
    
    /**
     * ✓ 复杂场景：编程式事务
     */
    public void programmaticTransaction() {
        System.out.println("  ✓ 使用 TransactionTemplate");
        
        transactionTemplate.execute(status -> {
            try {
                jdbcTemplate.update("INSERT INTO tx_user (name) VALUES (?)", "ProgrammaticUser");
                System.out.println("    ✓ 插入数据");
                
                // 复杂业务逻辑
                if (true) {
                    throw new RuntimeException("模拟异常");
                }
                
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                System.out.println("    ⚠ 事务回滚：" + e.getMessage());
                return false;
            }
        });
    }
    
    // ========== 最佳实践 3: 只读事务 ==========
    
    /**
     * ✓ 查询优化：只读事务
     */
    @Transactional(readOnly = true)
    public void readOnlyTransaction() {
        System.out.println("  ✓ readOnly = true 优化查询性能");
        System.out.println("  ✓ Spring 不会刷新持久化上下文");
        
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT * FROM tx_user");
        System.out.println("  ✓ 查询用户数：" + users.size());
    }
    
    // ========== 最佳实践 4: 事务拆分 ==========
    
    /**
     * ✓ 避免大事务：拆分事务
     */
    public void splitTransactions() {
        System.out.println("  ✓ 将大事务拆分为小事务");
        System.out.println("  ✓ 减少锁竞争，提高并发性能");
        
        // 步骤 1: 非事务操作
        validateInput("test");
        
        // 步骤 2: 事务操作 1
        createEntity("Entity1");
        
        // 步骤 3: 非事务操作
        sendNotification("Entity1");
        
        // 步骤 4: 事务操作 2
        updateStatistics("Entity1");
    }
    
    @Transactional
    public void createEntity(String name) {
        System.out.println("    ✓ 事务 1: 创建实体");
    }
    
    @Transactional
    public void updateStatistics(String name) {
        System.out.println("    ✓ 事务 2: 更新统计");
    }
    
    private void validateInput(String input) {
        System.out.println("    ✓ 参数校验（非事务）");
    }
    
    private void sendNotification(String name) {
        System.out.println("    ✓ 发送通知（非事务）");
    }
    
    // ========== 最佳实践 5: 日志记录 ==========
    
    /**
     * ✓ 日志记录：使用 REQUIRES_NEW
     */
    @Transactional
    public void logWithRequiresNew() {
        System.out.println("  ✓ 业务操作（REQUIRED）");
        
        try {
            jdbcTemplate.update("INSERT INTO tx_user (name) VALUES (?)", "BusinessUser");
            System.out.println("    ✓ 业务数据插入");
            
            // 业务异常
            throw new RuntimeException("业务失败");
        } catch (Exception e) {
            // 记录失败日志（独立事务）
            self.logFailure("业务失败：" + e.getMessage());
            throw e;
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(String message) {
        System.out.println("    ✓ 记录失败日志（独立事务）: " + message);
        // 即使业务回滚，日志也会保留
    }
}

/**
 * ========================================
 *           Spring 事务高频面试题目
 * ========================================
 * 
 * 【问题 1】Spring 事务失效的常见场景有哪些？
 * 
 * 【答案】
 * 
 * 1. 非 public 方法
 *    - @Transactional 只能用于 public 方法
 *    - Spring AOP 基于动态代理，非 public 方法无法被代理
 *    - 解决：改为 public 方法
 * 
 * 2. 自调用问题（最常见）
 *    - 同类内部调用不走代理，this 调用绕过事务切面
 *    - 解决：
 *      a. 注入自身代理对象：@Autowired @Lazy SelfService self;
 *      b. 使用 AopContext.currentProxy()
 *      c. 拆分到另一个 Service
 * 
 * 3. 异常被捕获未抛出
 *    - 事务回滚依赖于异常抛出
 *    - 解决：捕获后继续抛出或手动设置 rollbackOnly
 * 
 * 4. 异常类型不匹配
 *    - 默认只回滚 RuntimeException 和 Error
 *    - checked Exception 不会触发回滚
 *    - 解决：@Transactional(rollbackFor = Exception.class)
 * 
 * 5. 数据库引擎不支持
 *    - MySQL MyISAM 引擎不支持事务
 *    - 解决：使用 InnoDB 引擎
 * 
 * 6. 异步方法
 *    - 新线程不在 Spring 事务管理范围内
 *    - 解决：使用 @Async 时配置事务管理器
 * 
 * 7. 传播行为配置错误
 *    - NOT_SUPPORTED 会挂起事务
 *    - 解决：理解各种传播行为的含义
 * 
 * 8. 多个数据源
 *    - 未指定正确的事务管理器
 *    - 解决：@Transactional(transactionManager = "xxx")
 * 
 * 
 * 【问题 2】Spring 事务传播行为有哪些？分别适用于什么场景？
 * 
 * 【答案】
 * 
 * 1. REQUIRED（默认）
 *    - 规则：当前有事务则加入，无则新建
 *    - 场景：大多数业务场景，保证原子性
 *    - 示例：订单创建 + 库存扣减
 * 
 * 2. REQUIRES_NEW
 *    - 规则：挂起当前事务，新建独立事务
 *    - 场景：日志记录、审计操作
 *    - 示例：无论业务成功失败，都要记录日志
 * 
 * 3. NESTED
 *    - 规则：在当前事务中创建嵌套事务（savepoint）
 *    - 场景：部分回滚需求
 *    - 示例：批量处理，单条失败不影响其他
 *    - 注意：仅 DataSourceTransactionManager 支持
 * 
 * 4. SUPPORTS
 *    - 规则：当前有事务则加入，无则以非事务执行
 *    - 场景：查询操作，可有可无的事务
 *    - 示例：根据 ID 查询用户信息
 * 
 * 5. NOT_SUPPORTED
 *    - 规则：挂起当前事务，以非事务执行
 *    - 场景：不需要事务的操作（如文件 IO）
 *    - 示例：导出 Excel 文件
 * 
 * 6. MANDATORY
 *    - 规则：当前必须有事务，否则抛出异常
 *    - 场景：强制要求事务的方法
 *    - 示例：核心业务逻辑，必须在事务中执行
 * 
 * 7. NEVER
 *    - 规则：当前必须无事务，否则抛出异常
 *    - 场景：绝对不能有事务的操作
 *    - 示例：某些特定的批处理任务
 * 
 * 
 * 【问题 3】REQUIRED 和 REQUIRES_NEW 有什么区别？
 * 
 * 【答案】
 * 
 * | 对比维度    | REQUIRED                      | REQUIRES_NEW                |
 * |------------|-------------------------------|-----------------------------|
 * | 事务关系    | 加入当前事务                   | 创建新事务，独立于当前事务     |
 * | 回滚影响    | 同成功或同失败                 | 内层回滚不影响外层             |
 * | 锁持有时间  | 整个事务期间                   | 仅内层事务期间                 |
 * | 性能        | 较好（单个事务）               | 较差（多个事务）               |
 * | 使用场景    | 大多数业务场景                 | 日志、审计等独立操作           |
 * 
 * 示例对比：
 * 
 * ```java
 * // REQUIRED 场景
 * @Transactional
 * public void createOrder() {
 *     orderMapper.insert(order);      // 同事务
 *     inventoryMapper.reduce();       // 同事务
 *     // 一个失败，全部回滚
 * }
 * 
 * // REQUIRES_NEW 场景
 * @Transactional
 * public void createOrder() {
 *     orderMapper.insert(order);
 *     try {
 *         logService.log();  // REQUIRES_NEW，独立提交
 *     } catch (Exception e) {
 *         // 日志失败不影响订单
 *     }
 * }
 * ```
 * 
 * 
 * 【问题 4】NESTED 和 REQUIRES_NEW 有什么区别？
 * 
 * 【答案】
 * 
 * 1. 事务关系不同
 *    - NESTED: 嵌套事务，是外层事务的一部分（savepoint）
 *    - REQUIRES_NEW: 全新事务，完全独立于外层
 * 
 * 2. 回滚影响不同
 *    - NESTED: 外层回滚，内层也回滚
 *    - REQUIRES_NEW: 外层回滚，内层已提交的不受影响
 * 
 * 3. 实现机制不同
 *    - NESTED: 基于 JDBC savepoint 实现
 *    - REQUIRES_NEW: 挂起当前事务，创建新事务
 * 
 * 4. 支持程度不同
 *    - NESTED: 仅 DataSourceTransactionManager 支持
 *    - REQUIRES_NEW: 所有事务管理器都支持
 * 
 * 示意图：
 * 
 * ```
 * NESTED:
 * ┌─────────────────────┐
 * │  Outer Transaction  │
 * │  ┌───────────────┐  │
 * │  │ Inner (Save)  │  │
 * │  └───────────────┘  │
 * │                     │
 * │  Outer 回滚 → Inner 也回滚
 * └─────────────────────┘
 * 
 * REQUIRES_NEW:
 * ┌─────────────────────┐
 * │  Outer Transaction  │ (暂停)
 * └─────────────────────┘
 * ┌─────────────────────┐
 * │  New Transaction    │ (独立)
 * └─────────────────────┘
 * Outer 回滚 ≠ New 回滚
 * ```
 * 
 * 
 * 【问题 5】如何正确地在事务中记录日志？
 * 
 * 【答案】
 * 
 * 错误做法：
 * ```java
 * @Transactional
 * public void business() {
 *     logMapper.insert("业务开始");  // ❌ 同事务，回滚时日志也消失
 *     // 业务逻辑
 * }
 * ```
 * 
 * 正确做法：
 * ```java
 * @Service
 * public class BusinessService {
 *     
 *     @Autowired
 *     private LogService logService;
 *     
 *     @Transactional
 *     public void business() {
 *         try {
 *             // 业务逻辑
 *             logService.logSuccess("业务成功");  // ✓ REQUIRES_NEW
 *         } catch (Exception e) {
 *             logService.logError("业务失败");   // ✓ REQUIRES_NEW
 *             throw e;
 *         }
 *     }
 * }
 * 
 * @Service
 * public class LogService {
 *     @Transactional(propagation = Propagation.REQUIRES_NEW)
 *     public void logSuccess(String msg) {
 *         // 独立事务，不受业务回滚影响
 *     }
 *     
 *     @Transactional(propagation = Propagation.REQUIRES_NEW)
 *     public void logError(String msg) {
 *         // 独立事务，不受业务回滚影响
 *     }
 * }
 * ```
 * 
 * 关键点：
 * 1. 日志服务使用 REQUIRES_NEW 传播行为
 * 2. 日志操作在 try-catch 块之外调用
 * 3. 确保日志服务是独立的 Bean（通过代理调用）
 * 
 * 
 * 【问题 6】事务隔离级别有哪些？MySQL 默认是什么？
 * 
 * 【答案】
 * 
 * SQL 标准定义的隔离级别（从低到高）：
 * 
 * 1. READ_UNCOMMITTED（读未提交）
 *    - 问题：脏读、不可重复读、幻读
 *    - 性能：最好
 *    - 适用：对数据一致性要求不高的统计场景
 * 
 * 2. READ_COMMITTED（读已提交）
 *    - 避免：脏读
 *    - 问题：不可重复读、幻读
 *    - 性能：较好
 *    - 适用：Oracle 默认，大多数场景够用
 * 
 * 3. REPEATABLE_READ（可重复读）
 *    - 避免：脏读、不可重复读
 *    - 问题：幻读（InnoDB 通过 MVCC+Next-Key Lock 基本解决）
 *    - 性能：一般
 *    - 适用：MySQL 默认，推荐选择
 * 
 * 4. SERIALIZABLE（串行化）
 *    - 避免：脏读、不可重复读、幻读
 *    - 实现：强制事务串行执行
 *    - 性能：最差
 *    - 适用：对数据一致性要求极高的金融场景
 * 
 * MySQL InnoDB 的特殊优化：
 * - 在 REPEATABLE_READ 隔离级别下，通过 Next-Key Lock 基本解决了幻读问题
 * - 因此 MySQL 默认使用 REPEATABLE_READ 而非 READ_COMMITTED
 * 
 * 选择建议：
 * - 互联网应用：REPEATABLE_READ（MySQL 默认）
 * - 金融系统：考虑 SERIALIZABLE 或应用层加锁
 * - 数据分析：READ_UNCOMMITTED（允许脏读）
 * 
 * 
 * 【问题 7】@Transactional 的原理是什么？
 * 
 * 【答案】
 * 
 * 核心原理：AOP（面向切面编程）+ 动态代理
 * 
 * 1. 扫描阶段
 *    - Spring 扫描带有@Transactional 的 Bean
 *    - 创建 TransactionInterceptor（事务拦截器）
 * 
 * 2. 代理创建
 *    - JDK 动态代理（接口）或 CGLIB（类）
 *    - 生成代理对象，包裹目标对象
 * 
 * 3. 方法调用流程
 *    ```
 *    客户端调用
 *      ↓
 *    代理对象
 *      ↓
 *    TransactionInterceptor.invoke()
 *      ↓
 *    PlatformTransactionManager.getTransaction()
 *      ↓
 *    执行目标方法
 *      ↓
 *    成功：commit()
 *    异常：rollback()
 *    ```
 * 
 * 关键源码：
 * ```java
 * // TransactionInterceptor.java
 * @Override
 * public Object invoke(MethodInvocation invocation) throws Throwable {
 *     // 1. 获取事务属性
 *     TransactionAttributeSource tas = getTransactionAttributeSource();
 *     TransactionAttribute txAttr = tas.getTransactionAttribute(method, targetClass);
 *     
 *     // 2. 获取事务管理器
 *     PlatformTransactionManager tm = determineTransactionManager(txAttr);
 *     
 *     // 3. 创建事务
 *     TransactionStatus status = tm.getTransaction(txAttr);
 *     
 *     try {
 *         // 4. 执行目标方法
 *         Object result = invocation.proceed();
 *         
 *         // 5. 提交事务
 *         tm.commit(status);
 *         return result;
 *     } catch (Throwable ex) {
 *         // 6. 回滚事务
 *         completeTransactionAfterThrowing(status, ex);
 *         throw ex;
 *     }
 * }
 * ```
 * 
 * 注意事项：
 * 1. 只有外部调用代理对象才生效
 * 2. 自调用（this 调用）不经过代理
 * 3. 必须是 public 方法
 * 4. 异常必须抛出才能触发回滚
 * 
 * 
 * 【问题 8】如何手动回滚事务？
 * 
 * 【答案】
 * 
 * 方法 1: 抛出异常（推荐）
 * ```java
 * @Transactional
 * public void method() {
 *     if (error) {
 *         throw new RuntimeException("业务失败"); // ✓ 自动回滚
 *     }
 * }
 * ```
 * 
 * 方法 2: 手动设置 rollbackOnly
 * ```java
 * @Transactional
 * public void method() {
 *     try {
 *         // 业务逻辑
 *     } catch (Exception e) {
 *         TransactionAspectSupport.currentTransactionStatus()
 *             .setRollbackOnly(); // ✓ 手动回滚
 *         throw e;
 *     }
 * }
 * ```
 * 
 * 方法 3: 编程式事务
 * ```java
 * transactionTemplate.execute(status -> {
 *     try {
 *         // 业务逻辑
 *         return result;
 *     } catch (Exception e) {
 *         status.setRollbackOnly(); // ✓ 标记回滚
 *         throw e;
 *     }
 * });
 * ```
 * 
 * 
 * 【问题 9】事务超时如何配置？有什么作用？
 * 
 * 【答案】
 * 
 * 配置方式：
 * ```java
 * @Transactional(timeout = 30)  // 超时时间 30 秒
 * public void longRunningMethod() {
 *     // 超过 30 秒未完成，自动回滚
 * }
 * ```
 * 
 * 作用：
 * 1. 防止长事务占用数据库资源
 * 2. 避免死锁长时间不释放
 * 3. 快速失败，提高系统可用性
 * 
 * 底层实现：
 * - Spring 注册 TransactionSynchronization
 * - 到达超时时间后抛出 TimeoutException
 * - 触发事务回滚
 * 
 * 使用建议：
 * - 查询操作：timeout = 5-10 秒
 * - 简单业务：timeout = 30 秒
 * - 复杂业务：timeout = 60 秒
 * - 批处理：根据数据量合理设置
 * 
 * 
 * 【问题 10】什么是大事务？有什么危害？如何优化？
 * 
 * 【答案】
 * 
 * 大事务定义：
 * - 执行时间长
 * - 操作数据量大
 * - 持有锁时间久
 * 
 * 危害：
 * 1. 锁竞争加剧，并发性能下降
 * 2. 可能导致死锁
 * 3. 数据库连接占用时间长
 * 4. 回滚时间长，影响系统可用性
 * 
 * 优化方案：
 * 
 * 1. 事务拆分
 * ```java
 * // ❌ 大事务
 * @Transactional
 * public void process() {
 *     validate();      // 参数校验（非事务）
 *     saveOrder();     // 保存订单（事务）
 *     sendEmail();     // 发送邮件（非事务）
 *     saveLog();       // 保存日志（事务）
 * }
 * 
 * // ✓ 拆分事务
 * public void process() {
 *     validate();          // 非事务
 *     saveOrder();         // @Transactional
 *     sendEmail();         // 非事务
 *     saveLog();           // @Transactional
 * }
 * ```
 * 
 * 2. 异步处理
 * ```java
 * @Transactional
 * public void createOrder() {
 *     orderMapper.insert(order);
 *     asyncService.sendEmail(order); // @Async
 * }
 * ```
 * 
 * 3. 批量操作分批提交
 * ```java
 * @Transactional
 * public void batchInsert(List<Data> list) {
 *     int batchSize = 100;
 *     for (int i = 0; i < list.size(); i += batchSize) {
 *         List<Data> batch = list.subList(i, Math.min(i + batchSize, list.size()));
 *         batchMapper.insert(batch);
 *         // 每批提交一次，减少锁持有时间
 *     }
 * }
 * ```
 * 
 * 4. 只读事务优化
 * ```java
 * @Transactional(readOnly = true)
 * public List<Data> query() {
 *     // readOnly=true，Spring 优化处理
 * }
 * ```
 * 
 * 
 * ========================================
 */
