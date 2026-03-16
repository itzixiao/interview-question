package cn.itzixiao.interview.spring.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Spring 事务传播行为详解
 * <p>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                        事务传播行为（Propagation）                            │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │                                                                             │
 * │  REQUIRED      (默认)  当前有事务则加入，无则新建                              │
 * │  REQUIRES_NEW         挂起当前事务，新建独立事务                               │
 * │  NESTED               在当前事务中创建嵌套事务（savepoint）                     │
 * │  SUPPORTS             当前有事务则加入，无则以非事务执行                        │
 * │  NOT_SUPPORTED        挂起当前事务，以非事务执行                               │
 * │  MANDATORY            当前必须有事务，否则抛出异常                             │
 * │  NEVER                当前必须无事务，否则抛出异常                             │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * <p>
 * 事务隔离级别（Isolation）：
 * - DEFAULT: 使用数据库默认隔离级别
 * - READ_UNCOMMITTED: 读未提交
 * - READ_COMMITTED: 读已提交（Oracle默认）
 * - REPEATABLE_READ: 可重复读（MySQL默认）
 * - SERIALIZABLE: 串行化
 */
@SpringBootApplication
public class TransactionPropagationDemo implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private LogService logService;

    public static void main(String[] args) {
        SpringApplication.run(TransactionPropagationDemo.class, args);
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Override
    public void run(String... args) {
        System.out.println("\n========================================");
        System.out.println("Spring 事务传播行为测试开始");
        System.out.println("========================================\n");

        // 初始化数据
        initData();

        // 测试各种传播行为
        testRequired();
        testRequiresNew();
        testNested();
        testSupports();
        testNotSupported();
        testMandatory();
        testNever();

        System.out.println("\n========================================");
        System.out.println("所有测试完成");
        System.out.println("========================================\n");
    }

    private void initData() {
        System.out.println("【初始化】清空测试数据");
        userService.initTable();
    }

    /**
     * ============================================
     * 1. REQUIRED（默认）- 当前有事务则加入，无则新建
     * ============================================
     * <p>
     * 场景：方法 A 调用方法 B，两者都是 REQUIRED
     * - 如果 A 有事务，B 加入 A 的事务
     * - 如果 A 无事务，B 新建一个事务
     */
    private void testRequired() {
        System.out.println("\n========== 测试 REQUIRED ==========");
        System.out.println("场景：外部有事务，内部 REQUIRED");
        System.out.println("预期：内部加入外部事务，同回滚\n");

        try {
            userService.createUserWithRequired("张三", "order-001");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }

        // 验证结果
        printData("REQUIRED 测试后");
    }

    /**
     * ============================================
     * 2. REQUIRES_NEW - 挂起当前事务，新建独立事务
     * ============================================
     * <p>
     * 场景：方法 A 调用方法 B，B 是 REQUIRES_NEW
     * - 挂起 A 的事务
     * - B 新建独立事务，独立提交/回滚
     * - B 完成后，恢复 A 的事务
     */
    private void testRequiresNew() {
        System.out.println("\n========== 测试 REQUIRES_NEW ==========");
        System.out.println("场景：外部有事务，内部 REQUIRES_NEW");
        System.out.println("预期：内部独立事务，外部回滚不影响内部\n");

        try {
            userService.createUserWithRequiresNew("李四", "order-002");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }

        printData("REQUIRES_NEW 测试后");
    }

    /**
     * ============================================
     * 3. NESTED - 在当前事务中创建嵌套事务（savepoint）
     * ============================================
     * <p>
     * 场景：方法 A 调用方法 B，B 是 NESTED
     * - B 在 A 的事务中创建一个 savepoint
     * - B 回滚只回滚到 savepoint
     * - A 回滚则整个事务回滚
     * <p>
     * 注意：NESTED 只对 DataSourceTransactionManager 有效
     */
    private void testNested() {
        System.out.println("\n========== 测试 NESTED ==========");
        System.out.println("场景：外部有事务，内部 NESTED");
        System.out.println("预期：内部回滚到 savepoint，外部可继续\n");

        try {
            userService.createUserWithNested("王五", "order-003");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }

        printData("NESTED 测试后");
    }

    /**
     * ============================================
     * 4. SUPPORTS - 当前有事务则加入，无则以非事务执行
     * ============================================
     * <p>
     * 场景：方法 A 调用方法 B，B 是 SUPPORTS
     * - 如果 A 有事务，B 加入
     * - 如果 A 无事务，B 以非事务方式执行
     */
    private void testSupports() {
        System.out.println("\n========== 测试 SUPPORTS ==========");
        System.out.println("场景1：外部有事务，内部 SUPPORTS");

        try {
            userService.createUserWithSupportsTx("赵六", "order-004");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }

        System.out.println("\n场景2：外部无事务，内部 SUPPORTS");
        try {
            userService.createUserWithSupportsNoTx("孙七", "order-005");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }

        printData("SUPPORTS 测试后");
    }

    /**
     * ============================================
     * 5. NOT_SUPPORTED - 挂起当前事务，以非事务执行
     * ============================================
     * <p>
     * 场景：方法 A 调用方法 B，B 是 NOT_SUPPORTED
     * - 挂起 A 的事务
     * - B 以非事务方式执行
     * - B 完成后，恢复 A 的事务
     */
    private void testNotSupported() {
        System.out.println("\n========== 测试 NOT_SUPPORTED ==========");
        System.out.println("场景：外部有事务，内部 NOT_SUPPORTED");
        System.out.println("预期：内部以非事务执行，不受外部回滚影响\n");

        try {
            userService.createUserWithNotSupported("周八", "order-006");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }

        printData("NOT_SUPPORTED 测试后");
    }

    /**
     * ============================================
     * 6. MANDATORY - 当前必须有事务，否则抛出异常
     * ============================================
     * <p>
     * 场景：方法 A 调用方法 B，B 是 MANDATORY
     * - 如果 A 有事务，B 加入
     * - 如果 A 无事务，抛出 IllegalTransactionStateException
     */
    private void testMandatory() {
        System.out.println("\n========== 测试 MANDATORY ==========");
        System.out.println("场景1：外部有事务，内部 MANDATORY");

        try {
            userService.createUserWithMandatoryTx("吴九", "order-007");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        System.out.println("\n场景2：外部无事务，内部 MANDATORY");
        try {
            userService.createUserWithMandatoryNoTx("郑十", "order-008");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        printData("MANDATORY 测试后");
    }

    /**
     * ============================================
     * 7. NEVER - 当前必须无事务，否则抛出异常
     * ============================================
     * <p>
     * 场景：方法 A 调用方法 B，B 是 NEVER
     * - 如果 A 无事务，B 以非事务执行
     * - 如果 A 有事务，抛出 IllegalTransactionStateException
     */
    private void testNever() {
        System.out.println("\n========== 测试 NEVER ==========");
        System.out.println("场景1：外部无事务，内部 NEVER");

        try {
            userService.createUserWithNeverNoTx("钱十一", "order-009");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        System.out.println("\n场景2：外部有事务，内部 NEVER");
        try {
            userService.createUserWithNeverTx("冯十二", "order-010");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        printData("NEVER 测试后");
    }

    private void printData(String title) {
        System.out.println("\n--- " + title + " ---");
        System.out.println("Users: " + userService.listUsers());
        System.out.println("Orders: " + orderService.listOrders());
        System.out.println("Logs: " + logService.listLogs());
    }
}

/**
 * ============================================
 * UserService - 主业务服务
 * ============================================
 */
@Service("txUserService")
class UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private LogService logService;

    public void initTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS tx_user");
        jdbcTemplate.execute("DROP TABLE IF EXISTS tx_order");
        jdbcTemplate.execute("DROP TABLE IF EXISTS tx_log");

        jdbcTemplate.execute("CREATE TABLE tx_user (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(50))");
        jdbcTemplate.execute("CREATE TABLE tx_order (id INT PRIMARY KEY AUTO_INCREMENT, user_id INT, order_no VARCHAR(50))");
        jdbcTemplate.execute("CREATE TABLE tx_log (id INT PRIMARY KEY AUTO_INCREMENT, message VARCHAR(200))");

        System.out.println("表初始化完成\n");
    }

    public List<Map<String, Object>> listUsers() {
        return jdbcTemplate.queryForList("SELECT * FROM tx_user");
    }

    private void insertUser(String name) {
        jdbcTemplate.update("INSERT INTO tx_user (name) VALUES (?)", name);
        System.out.println("  插入用户: " + name);
    }

    // ========== REQUIRED 测试 ==========

    @Transactional(propagation = Propagation.REQUIRED)
    public void createUserWithRequired(String name, String orderNo) {
        System.out.println("【UserService】createUserWithRequired 开始");
        printTransactionInfo();

        insertUser(name);
        orderService.createOrder(orderNo); // REQUIRED，加入当前事务

        // 模拟异常，整个事务回滚
        throw new RuntimeException("模拟业务异常");
    }

    // ========== REQUIRES_NEW 测试 ==========

    @Transactional(propagation = Propagation.REQUIRED)
    public void createUserWithRequiresNew(String name, String orderNo) {
        System.out.println("【UserService】createUserWithRequiresNew 开始");
        printTransactionInfo();

        insertUser(name);
        orderService.createOrderRequiresNew(orderNo); // REQUIRES_NEW，独立事务

        // 异常只影响当前事务，不影响 REQUIRES_NEW 的事务
        throw new RuntimeException("模拟业务异常");
    }

    // ========== NESTED 测试 ==========

    @Transactional(propagation = Propagation.REQUIRED)
    public void createUserWithNested(String name, String orderNo) {
        System.out.println("【UserService】createUserWithNested 开始");
        printTransactionInfo();

        insertUser(name);

        try {
            orderService.createOrderNested(orderNo); // NESTED，嵌套事务
        } catch (Exception e) {
            System.out.println("  捕获嵌套事务异常: " + e.getMessage());
            // 嵌套事务回滚到 savepoint，外部事务继续
        }

        // 外部事务正常提交
        System.out.println("【UserService】外部事务继续执行并提交");
    }

    // ========== SUPPORTS 测试 ==========

    @Transactional(propagation = Propagation.REQUIRED)
    public void createUserWithSupportsTx(String name, String orderNo) {
        System.out.println("【UserService】createUserWithSupportsTx 开始（外部有事务）");
        printTransactionInfo();

        insertUser(name);
        orderService.createOrderSupports(orderNo); // SUPPORTS，加入事务

        throw new RuntimeException("模拟异常，一起回滚");
    }

    public void createUserWithSupportsNoTx(String name, String orderNo) {
        System.out.println("【UserService】createUserWithSupportsNoTx 开始（外部无事务）");
        printTransactionInfo();

        insertUser(name);
        orderService.createOrderSupports(orderNo); // SUPPORTS，无事务执行

        throw new RuntimeException("模拟异常");
    }

    // ========== NOT_SUPPORTED 测试 ==========

    @Transactional(propagation = Propagation.REQUIRED)
    public void createUserWithNotSupported(String name, String orderNo) {
        System.out.println("【UserService】createUserWithNotSupported 开始");
        printTransactionInfo();

        insertUser(name);
        orderService.createOrderNotSupported(orderNo); // NOT_SUPPORTED，挂起事务

        throw new RuntimeException("模拟异常，外部回滚，内部已提交");
    }

    // ========== MANDATORY 测试 ==========

    @Transactional(propagation = Propagation.REQUIRED)
    public void createUserWithMandatoryTx(String name, String orderNo) {
        System.out.println("【UserService】createUserWithMandatoryTx 开始（外部有事务）");
        printTransactionInfo();

        insertUser(name);
        orderService.createOrderMandatory(orderNo); // MANDATORY，加入事务
    }

    public void createUserWithMandatoryNoTx(String name, String orderNo) {
        System.out.println("【UserService】createUserWithMandatoryNoTx 开始（外部无事务）");
        printTransactionInfo();

        insertUser(name);
        orderService.createOrderMandatory(orderNo); // MANDATORY，抛出异常
    }

    // ========== NEVER 测试 ==========

    public void createUserWithNeverNoTx(String name, String orderNo) {
        System.out.println("【UserService】createUserWithNeverNoTx 开始（外部无事务）");
        printTransactionInfo();

        insertUser(name);
        orderService.createOrderNever(orderNo); // NEVER，无事务执行
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void createUserWithNeverTx(String name, String orderNo) {
        System.out.println("【UserService】createUserWithNeverTx 开始（外部有事务）");
        printTransactionInfo();

        insertUser(name);
        orderService.createOrderNever(orderNo); // NEVER，抛出异常
    }

    private void printTransactionInfo() {
        boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
        String txName = TransactionSynchronizationManager.getCurrentTransactionName();
        System.out.println("  当前事务状态: " + (isActive ? "活跃" : "无事务") +
                (isActive ? ", 事务名: " + txName : ""));
    }
}

/**
 * ============================================
 * OrderService - 订单服务
 * ============================================
 */
@Service
class OrderService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> listOrders() {
        return jdbcTemplate.queryForList("SELECT * FROM tx_order");
    }

    private void insertOrder(String orderNo) {
        jdbcTemplate.update("INSERT INTO tx_order (order_no) VALUES (?)", orderNo);
        System.out.println("  插入订单: " + orderNo);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void createOrder(String orderNo) {
        System.out.println("【OrderService】createOrder (REQUIRED)");
        printTransactionInfo();
        insertOrder(orderNo);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createOrderRequiresNew(String orderNo) {
        System.out.println("【OrderService】createOrderRequiresNew (REQUIRES_NEW)");
        printTransactionInfo();
        insertOrder(orderNo);
        System.out.println("  REQUIRES_NEW 事务独立提交");
    }

    @Transactional(propagation = Propagation.NESTED)
    public void createOrderNested(String orderNo) {
        System.out.println("【OrderService】createOrderNested (NESTED)");
        printTransactionInfo();
        insertOrder(orderNo);
        throw new RuntimeException("NESTED 事务异常，回滚到 savepoint");
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void createOrderSupports(String orderNo) {
        System.out.println("【OrderService】createOrderSupports (SUPPORTS)");
        printTransactionInfo();
        insertOrder(orderNo);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createOrderNotSupported(String orderNo) {
        System.out.println("【OrderService】createOrderNotSupported (NOT_SUPPORTED)");
        printTransactionInfo();
        insertOrder(orderNo);
        System.out.println("  NOT_SUPPORTED 以非事务方式执行");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createOrderMandatory(String orderNo) {
        System.out.println("【OrderService】createOrderMandatory (MANDATORY)");
        printTransactionInfo();
        insertOrder(orderNo);
    }

    @Transactional(propagation = Propagation.NEVER)
    public void createOrderNever(String orderNo) {
        System.out.println("【OrderService】createOrderNever (NEVER)");
        printTransactionInfo();
        insertOrder(orderNo);
    }

    private void printTransactionInfo() {
        boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
        System.out.println("  事务状态: " + (isActive ? "活跃" : "无事务"));
    }
}

/**
 * ============================================
 * LogService - 日志服务（REQUIRES_NEW 示例）
 * ============================================
 * 日志记录应该独立于业务事务，即使业务回滚，日志也应该保留
 */
@Service
class LogService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> listLogs() {
        return jdbcTemplate.queryForList("SELECT * FROM tx_log");
    }

    /**
     * 记录日志 - 使用 REQUIRES_NEW 确保日志独立提交
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String message) {
        jdbcTemplate.update("INSERT INTO tx_log (message) VALUES (?)", message);
        System.out.println("  记录日志: " + message);
    }
}

/**
 * ============================================
 * AccountService - 账户服务（事务隔离级别示例）
 * ============================================
 */
@Service
class AccountService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 转账 - 演示事务隔离级别
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void transfer(String fromAccount, String toAccount, double amount) {
        System.out.println("【AccountService】转账开始");
        System.out.println("  隔离级别: READ_COMMITTED");

        // 扣款
        jdbcTemplate.update("UPDATE account SET balance = balance - ? WHERE account_no = ?",
                amount, fromAccount);
        System.out.println("  从 " + fromAccount + " 扣除 " + amount);

        // 收款
        jdbcTemplate.update("UPDATE account SET balance = balance + ? WHERE account_no = ?",
                amount, toAccount);
        System.out.println("  向 " + toAccount + " 转入 " + amount);

        System.out.println("【AccountService】转账完成");
    }

    /**
     * 查询余额 - 演示不同隔离级别的读行为
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public double getBalanceReadUncommitted(String accountNo) {
        // 可能读到未提交的数据（脏读）
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE account_no = ?",
                Double.class, accountNo);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public double getBalanceReadCommitted(String accountNo) {
        // 只能读到已提交的数据
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE account_no = ?",
                Double.class, accountNo);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public double getBalanceRepeatableRead(String accountNo) {
        // 可重复读，同一事务内多次读取结果一致
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE account_no = ?",
                Double.class, accountNo);
    }
}
