package cn.itzixiao.interview.transaction.service;

import cn.itzixiao.interview.transaction.entity.Account;
import cn.itzixiao.interview.transaction.entity.Order;
import cn.itzixiao.interview.transaction.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单服务 - 演示完整的事务传播场景
 *
 * @author itzixiao
 * @since 2026-03-13
 */
@Slf4j
@Service
public class OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private AccountService accountService;

    @Resource
    private LogService logService;

    /**
     * 创建订单 - 使用 REQUIRED（默认）
     * 演示：外部方法调用内部 REQUIRED 方法，共享同一事务
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrderWithRequired(Long userId, BigDecimal amount) {
        log.info("========== [REQUIRED] 开始创建订单 ==========");
        log.info("当前事务名称：{}", TransactionSynchronizationManager.getCurrentTransactionName());

        // 1. 创建订单
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus(1); // 已支付
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);
        log.info("创建订单：orderNo={}", order.getOrderNo());

        // 2. 扣减账户余额（REQUIRED - 加入同一事务）
        accountService.depositWithRequired(userId, amount.negate());

        // 3. 记录日志（REQUIRED - 加入同一事务）
        logService.logWithRequired("CREATE_ORDER", "订单创建成功：" + order.getOrderNo());

        // 模拟异常测试回滚
        if (amount.compareTo(new BigDecimal("1000")) > 0) {
            log.error("金额过大，抛出异常");
            throw new RuntimeException("金额超过限制");
        }

        log.info("========== [REQUIRED] 订单创建完成 ==========");
        return order;
    }

    /**
     * 创建订单并记录独立日志 - 演示 REQUIRES_NEW
     * 日志记录不受订单事务影响
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrderWithIndependentLog(Long userId, BigDecimal amount) {
        log.info("========== [REQUIRES_NEW] 开始创建订单（独立日志） ==========");

        // 1. 创建订单
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus(1);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);
        log.info("创建订单：orderNo={}", order.getOrderNo());

        // 2. 扣减账户余额
        accountService.depositWithRequired(userId, amount.negate());

        // 3. 记录日志（REQUIRES_NEW - 独立事务）
        logService.logSuccess("CREATE_ORDER", "订单创建成功：" + order.getOrderNo());

        // 模拟异常
        if ("TEST_ROLLBACK".equals(order.getOrderNo())) {
            throw new RuntimeException("测试回滚");
        }

        log.info("========== [REQUIRES_NEW] 订单创建完成 ==========");
        return order;
    }

    /**
     * 批量创建订单 - 演示 NESTED
     * 单个订单失败不影响其他订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateOrders(Long userId) {
        log.info("========== [NESTED] 开始批量创建订单 ==========");

        for (int i = 1; i <= 3; i++) {
            try {
                createSingleOrderWithNested(userId, new BigDecimal("100"));
            } catch (Exception e) {
                log.error("第{}个订单创建失败：{}", i, e.getMessage());
                // 继续处理下一个
            }
        }

        log.info("========== [NESTED] 批量创建完成 ==========");
    }

    /**
     * 创建单个订单 - NESTED 传播行为
     */
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public Order createSingleOrderWithNested(Long userId, BigDecimal amount) {
        log.info("[NESTED] 创建单个订单：userId={}, amount={}", userId, amount);

        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus(1);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 扣减余额
        accountService.depositWithNested(userId, amount.negate());

        // 第三个订单模拟失败
        if (amount.compareTo(new BigDecimal("50")) == 0) {
            throw new RuntimeException("模拟订单失败");
        }

        return order;
    }

    /**
     * 查询账户并演示 SUPPORTS
     */
    @Transactional(readOnly = true)
    public Account queryAccountWithSupports(Long userId) {
        log.info("========== 查询账户（SUPPORTS） ==========");
        return accountService.getAccountWithSupports(userId);
    }

    /**
     * 复杂业务场景演示
     * 结合多种传播行为
     */
    @Transactional(rollbackFor = Exception.class)
    public Order complexBusinessScenario(Long userId, BigDecimal amount) {
        log.info("========== [复杂场景] 开始业务处理 ==========");

        // 1. 查询账户（SUPPORTS - 有事务就加入）
        Account account = accountService.getAccountWithSupports(userId);
        if (account == null) {
            throw new RuntimeException("账户不存在");
        }

        // 2. 创建订单（REQUIRED - 加入当前事务）
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus(1);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 3. 扣减余额（REQUIRED - 加入当前事务）
        accountService.depositWithRequired(userId, amount.negate());

        // 4. 记录操作日志（REQUIRES_NEW - 独立提交）
        logService.logSuccess("COMPLEX_BIZ", "复杂业务处理成功");

        // 5. 记录审计日志（REQUIRED - 加入当前事务）
        logService.logWithRequired("AUDIT", "审计日志：" + order.getOrderNo());

        log.info("========== [复杂场景] 业务处理完成 ==========");
        return order;
    }
}
