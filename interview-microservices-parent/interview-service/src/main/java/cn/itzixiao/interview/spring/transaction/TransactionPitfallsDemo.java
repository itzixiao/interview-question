package cn.itzixiao.interview.spring.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Spring 事务失效场景详解
 *
 * 事务失效的常见原因：
 * 1. 非 public 方法
 * 2. 同类内部调用
 * 3. 异常被捕获未抛出
 * 4. 异常类型不匹配
 * 5. 异步方法
 * 6. 数据库引擎不支持事务
 */
@SpringBootApplication
public class TransactionPitfallsDemo implements CommandLineRunner {

    @Autowired
    private PitfallUserService userService;

    @Autowired
    private ApplicationContext applicationContext;

    public static void main(String[] args) {
        SpringApplication.run(TransactionPitfallsDemo.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("\n========================================");
        System.out.println("Spring 事务失效场景测试");
        System.out.println("========================================\n");

        initData();

        // 测试各种事务失效场景
        testNonPublicMethod();
        testSelfInvocation();
        testExceptionSwallowed();
        testWrongExceptionType();
        testRollbackForUnchecked();
        testCorrectUsage();

        System.out.println("\n========================================");
        System.out.println("测试完成");
        System.out.println("========================================\n");
    }

    private void initData() {
        System.out.println("【初始化】清空测试数据");
        userService.initTable();
    }

    /**
     * ============================================
     * 场景1：非 public 方法导致事务失效
     * ============================================
     *
     * 原因：@Transactional 只能用于 public 方法
     * Spring AOP 基于代理，非 public 方法无法被代理
     */
    private void testNonPublicMethod() {
        System.out.println("\n========== 场景1：非 public 方法 ==========");
        System.out.println("预期：事务失效，数据不回滚\n");

        try {
            userService.createUserWithPrivateMethodWrapper("PrivateUser");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }

        printResult("非 public 方法");
    }

    /**
     * ============================================
     * 场景2：同类内部调用导致事务失效
     * ============================================
     *
     * 原因：this 调用不走代理，@Transactional 不生效
     * 解决方案：
     * 1. 注入自身代理对象
     * 2. 使用 AopContext.currentProxy()
     * 3. 拆分到另一个 Service
     */
    private void testSelfInvocation() {
        System.out.println("\n========== 场景2：同类内部调用 ==========");
        System.out.println("子场景2.1：直接 this 调用（事务失效）");

        try {
            userService.outerMethodWithThisCall("SelfInvocationUser1");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }
        printResult("this 调用");

        System.out.println("\n子场景2.2：使用代理对象调用（事务生效）");
        initData();

        try {
            userService.outerMethodWithProxyCall("SelfInvocationUser2");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }
        printResult("代理调用");
    }

    /**
     * ============================================
     * 场景3：异常被捕获未抛出导致事务失效
     * ============================================
     *
     * 原因：事务回滚依赖于异常抛出，捕获后未抛出不触发回滚
     */
    private void testExceptionSwallowed() {
        System.out.println("\n========== 场景3：异常被捕获未抛出 ==========");
        System.out.println("预期：事务不回滚\n");

        initData();
        userService.createUserWithSwallowedException("SwallowedUser");

        printResult("异常被捕获");
    }

    /**
     * ============================================
     * 场景4：异常类型不匹配导致事务失效
     * ============================================
     *
     * 原因：默认只回滚 RuntimeException 和 Error
     * checked Exception 不会触发回滚
     */
    private void testWrongExceptionType() {
        System.out.println("\n========== 场景4：异常类型不匹配 ==========");
        System.out.println("子场景4.1：抛出 checked Exception（默认不回滚）");

        initData();
        try {
            userService.createUserWithCheckedException("CheckedUser");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getClass().getSimpleName());
        }
        printResult("checked 异常（默认）");

        System.out.println("\n子场景4.2：配置 rollbackFor（回滚生效）");
        initData();
        try {
            userService.createUserWithRollbackFor("RollbackForUser");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getClass().getSimpleName());
        }
        printResult("配置 rollbackFor");
    }

    /**
     * ============================================
     * 场景5：unchecked 异常正确回滚
     * ============================================
     */
    private void testRollbackForUnchecked() {
        System.out.println("\n========== 场景5：unchecked 异常 ==========");
        System.out.println("预期：事务正常回滚\n");

        initData();
        try {
            userService.createUserWithRuntimeException("RuntimeUser");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getClass().getSimpleName());
        }
        printResult("RuntimeException");
    }

    /**
     * ============================================
     * 场景6：正确使用事务
     * ============================================
     */
    private void testCorrectUsage() {
        System.out.println("\n========== 场景6：正确使用事务 ==========");
        System.out.println("预期：事务正常回滚\n");

        initData();
        try {
            // 获取代理对象调用
            PitfallUserService proxy = applicationContext.getBean(PitfallUserService.class);
            proxy.correctTransactionalMethod("CorrectUser");
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }
        printResult("正确使用");
    }

    private void printResult(String scenario) {
        List<Map<String, Object>> users = userService.listUsers();
        System.out.println("\n【" + scenario + "】结果:");
        System.out.println("  用户表数据: " + users);
        System.out.println("  结论: " + (users.isEmpty() ? "✓ 事务回滚成功" : "✗ 事务失效，数据未回滚"));
    }
}

/**
 * ============================================
 * 事务失效示例 Service
 * ============================================
 */
@Service
class PitfallUserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    public void initTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS pitfall_user");
        jdbcTemplate.execute("CREATE TABLE pitfall_user (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(50))");
    }

    public List<Map<String, Object>> listUsers() {
        return jdbcTemplate.queryForList("SELECT * FROM pitfall_user");
    }

    private void insertUser(String name) {
        jdbcTemplate.update("INSERT INTO pitfall_user (name) VALUES (?)", name);
        System.out.println("  插入用户: " + name);
    }

    // ========== 场景1：非 public 方法 ==========

    /**
     * ❌ 错误：private 方法上 @Transactional 不生效
     */
    @Transactional
    private void createUserWithPrivateMethod(String name) {
        System.out.println("【错误示例】private 方法事务");
        insertUser(name);
        throw new RuntimeException("模拟异常");
    }

    public void createUserWithPrivateMethodWrapper(String name) {
        createUserWithPrivateMethod(name);
    }

    // ========== 场景2：同类内部调用 ==========

    /**
     * ❌ 错误：this 调用不走代理
     */
    public void outerMethodWithThisCall(String name) {
        System.out.println("【错误示例】this 调用内部事务方法");
        // this 调用，不经过代理，@Transactional 不生效
        this.innerTransactionalMethod(name);
    }

    /**
     * ✓ 正确：使用代理对象调用
     */
    public void outerMethodWithProxyCall(String name) {
        System.out.println("【正确示例】代理对象调用内部事务方法");
        // 获取代理对象调用
        PitfallUserService proxy = applicationContext.getBean(PitfallUserService.class);
        proxy.innerTransactionalMethod(name);
    }

    @Transactional
    public void innerTransactionalMethod(String name) {
        System.out.println("  内部事务方法执行");
        insertUser(name);
        throw new RuntimeException("模拟异常");
    }

    // ========== 场景3：异常被捕获 ==========

    /**
     * ❌ 错误：捕获异常未抛出
     */
    @Transactional
    public void createUserWithSwallowedException(String name) {
        System.out.println("【错误示例】捕获异常未抛出");
        insertUser(name);
        try {
            // 一些业务逻辑
            throw new RuntimeException("业务异常");
        } catch (Exception e) {
            System.out.println("  捕获异常但不再抛出: " + e.getMessage());
            // 异常被吞掉，事务不会回滚
        }
    }

    // ========== 场景4：异常类型不匹配 ==========

    /**
     * ❌ 错误：checked Exception 默认不回滚
     */
    @Transactional
    public void createUserWithCheckedException(String name) throws IOException {
        System.out.println("【错误示例】checked Exception 默认不回滚");
        insertUser(name);
        throw new IOException("IO 异常");
    }

    /**
     * ✓ 正确：配置 rollbackFor 指定回滚异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void createUserWithRollbackFor(String name) throws IOException {
        System.out.println("【正确示例】配置 rollbackFor");
        insertUser(name);
        throw new IOException("IO 异常");
    }

    // ========== 场景5：unchecked 异常 ==========

    /**
     * ✓ 正确：RuntimeException 默认回滚
     */
    @Transactional
    public void createUserWithRuntimeException(String name) {
        System.out.println("【正确示例】RuntimeException 默认回滚");
        insertUser(name);
        throw new RuntimeException("运行时异常");
    }

    // ========== 场景6：正确使用 ==========

    /**
     * ✓ 正确使用事务
     */
    @Transactional(rollbackFor = Exception.class)
    public void correctTransactionalMethod(String name) throws Exception {
        System.out.println("【正确使用】public 方法 + 代理调用 + 正确异常处理");
        insertUser(name);
        throw new Exception("模拟异常");
    }
}

/**
 * ============================================
 * 事务最佳实践 Service
 * ============================================
 */
@Service
class BestPracticeUserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 最佳实践1：声明式事务（推荐）
     */
    @Transactional(
            rollbackFor = Exception.class,      // 指定回滚异常类型
            timeout = 30,                        // 超时时间（秒）
            readOnly = false                     // 是否只读
    )
    public void createUserWithDeclarativeTx(String name) throws Exception {
        jdbcTemplate.update("INSERT INTO user (name) VALUES (?)", name);

        // 其他业务操作
        sendNotification(name);

        // 可能抛出异常
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
    }

    /**
     * 最佳实践2：编程式事务（复杂场景）
     */
    public void createUserWithProgrammaticTx(String name) {
        transactionTemplate.execute(status -> {
            try {
                jdbcTemplate.update("INSERT INTO user (name) VALUES (?)", name);
                sendNotification(name);
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                System.out.println("事务回滚: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * 最佳实践3：只读事务（查询优化）
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUsers() {
        // readOnly = true 时：
        // 1. Spring 不会刷新持久化上下文
        // 2. 某些数据库可以优化查询（如 MySQL 不加锁）
        return jdbcTemplate.queryForList("SELECT * FROM user");
    }

    /**
     * 最佳实践4：事务拆分（避免大事务）
     */
    public void complexBusinessProcess(String name) {
        // 步骤1：非事务操作（如参数校验）
        validateName(name);

        // 步骤2：事务操作1
        createUser(name);

        // 步骤3：非事务操作（如发送消息）
        sendMessage(name);

        // 步骤4：事务操作2
        updateStatistics(name);
    }

    @Transactional
    public void createUser(String name) {
        jdbcTemplate.update("INSERT INTO user (name) VALUES (?)", name);
    }

    @Transactional
    public void updateStatistics(String name) {
        jdbcTemplate.update("UPDATE statistics SET count = count + 1 WHERE type = 'user'");
    }

    private void validateName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
    }

    private void sendNotification(String name) {
        System.out.println("发送通知给: " + name);
    }

    private void sendMessage(String name) {
        System.out.println("发送消息给: " + name);
    }
}

/**
 * ============================================
 * 事务传播最佳实践
 * ============================================
 *
 * 场景1：主业务 + 日志记录
 * - 主业务：REQUIRED（默认）
 * - 日志：REQUIRES_NEW（无论主业务是否成功，日志都要记录）
 *
 * 场景2：订单创建 + 库存扣减
 * - 订单：REQUIRED
 * - 库存：REQUIRED（同成功或同失败）
 *
 * 场景3：批量处理
 * - 批量方法：REQUIRED
 * - 单条处理：REQUIRES_NEW（单条失败不影响其他）
 *
 * 场景4：嵌套业务逻辑
 * - 外层：REQUIRED
 * - 内层：NESTED（内层失败可回滚，外层继续）
 */
@Service
class TransactionPropagationBestPractice {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy
    private TransactionPropagationBestPractice self;

    /**
     * 场景1：订单创建 + 日志记录
     * 日志必须记录，即使订单失败
     */
    @Transactional
    public void createOrderWithLog(String orderNo) {
        // 插入订单（同事务）
        jdbcTemplate.update("INSERT INTO orders (order_no) VALUES (?)", orderNo);

        try {
            // 业务逻辑
            processOrder(orderNo);
        } catch (Exception e) {
            // 订单处理失败，记录失败日志
            self.logOrderFailure(orderNo, e.getMessage());
            throw e; // 继续抛出，让订单事务回滚
        }

        // 记录成功日志（新事务，独立提交）
        self.logOrderSuccess(orderNo);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void logOrderSuccess(String orderNo) {
        jdbcTemplate.update("INSERT INTO order_log (order_no, status) VALUES (?, 'SUCCESS')", orderNo);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void logOrderFailure(String orderNo, String reason) {
        jdbcTemplate.update("INSERT INTO order_log (order_no, status, reason) VALUES (?, 'FAILED', ?)",
                orderNo, reason);
    }

    /**
     * 场景2：批量处理，单条失败不影响其他
     */
    public void batchProcess(List<String> items) {
        for (String item : items) {
            try {
                // 每条独立事务
                self.processSingleItem(item);
            } catch (Exception e) {
                System.out.println("处理失败: " + item + ", 原因: " + e.getMessage());
                // 继续处理下一条
            }
        }
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processSingleItem(String item) {
        jdbcTemplate.update("INSERT INTO processed_items (item_name) VALUES (?)", item);
        // 可能抛出异常
        if ("error".equals(item)) {
            throw new RuntimeException("模拟处理失败");
        }
    }

    private void processOrder(String orderNo) {
        System.out.println("处理订单: " + orderNo);
    }
}
