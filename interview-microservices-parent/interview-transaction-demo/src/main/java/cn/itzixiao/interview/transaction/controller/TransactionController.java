package cn.itzixiao.interview.transaction.controller;

import cn.itzixiao.interview.transaction.entity.Account;
import cn.itzixiao.interview.transaction.entity.Order;
import cn.itzixiao.interview.transaction.service.AccountService;
import cn.itzixiao.interview.transaction.service.LogService;
import cn.itzixiao.interview.transaction.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 事务传播机制演示控制器
 *
 * @author itzixiao
 * @since 2026-03-13
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction")
public class TransactionController {

    @Resource
    private OrderService orderService;

    @Resource
    private AccountService accountService;

    @Resource
    private LogService logService;

    /**
     * 测试 REQUIRED 传播行为
     * GET /api/transaction/required?userId=1&amount=100
     */
    @GetMapping("/required")
    public Order testRequired(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "100") BigDecimal amount) {
        log.info("===== 测试 REQUIRED 传播行为 =====");
        return orderService.createOrderWithRequired(userId, amount);
    }

    /**
     * 测试 REQUIRES_NEW 传播行为
     * GET /api/transaction/requires-new?userId=1&amount=50
     */
    @GetMapping("/requires-new")
    public Order testRequiresNew(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "50") BigDecimal amount) {
        log.info("===== 测试 REQUIRES_NEW 传播行为 =====");
        return orderService.createOrderWithIndependentLog(userId, amount);
    }

    /**
     * 测试 NESTED 传播行为
     * GET /api/transaction/nested?userId=1
     */
    @GetMapping("/nested")
    public void testNested(@RequestParam(defaultValue = "1") Long userId) {
        log.info("===== 测试 NESTED 传播行为 =====");
        orderService.batchCreateOrders(userId);
    }

    /**
     * 测试 SUPPORTS 传播行为
     * GET /api/transaction/supports?userId=1
     */
    @GetMapping("/supports")
    public Account testSupports(@RequestParam(defaultValue = "1") Long userId) {
        log.info("===== 测试 SUPPORTS 传播行为 =====");
        return orderService.queryAccountWithSupports(userId);
    }

    /**
     * 测试 MANDATORY 传播行为（需要在事务中调用）
     * GET /api/transaction/mandatory?userId=1&amount=50
     */
    @GetMapping("/mandatory")
    public void testMandatory(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "50") BigDecimal amount) {
        log.info("===== 测试 MANDATORY 传播行为 =====");
        accountService.mandatoryOperation(userId, amount);
    }

    /**
     * 测试 NEVER 传播行为（不能在事务中调用）
     * GET /api/transaction/never?userId=1
     */
    @GetMapping("/never")
    public void testNever(@RequestParam(defaultValue = "1") Long userId) {
        log.info("===== 测试 NEVER 传播行为 =====");
        accountService.neverOperation(userId);
    }

    /**
     * 测试 NOT_SUPPORTED 传播行为
     * GET /api/transaction/not-supported?userId=1
     */
    @GetMapping("/not-supported")
    public Account testNotSupported(@RequestParam(defaultValue = "1") Long userId) {
        log.info("===== 测试 NOT_SUPPORTED 传播行为 =====");
        return accountService.getAccountWithNotSupported(userId);
    }

    /**
     * 复杂业务场景演示
     * GET /api/transaction/complex?userId=1&amount=200
     */
    @GetMapping("/complex")
    public Order testComplexScenario(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "200") BigDecimal amount) {
        log.info("===== 复杂业务场景演示 =====");
        return orderService.complexBusinessScenario(userId, amount);
    }

    /**
     * 直接测试日志服务（REQUIRES_NEW）
     * GET /api/transaction/log/success?operation=TEST&detail=SUCCESS
     */
    @GetMapping("/log/success")
    public void testLogSuccess(
            @RequestParam(defaultValue = "TEST") String operation,
            @RequestParam(defaultValue = "SUCCESS") String detail) {
        log.info("===== 测试日志服务（REQUIRES_NEW） =====");
        logService.logSuccess(operation, detail);
    }

    /**
     * 查询账户余额
     * GET /api/transaction/account?userId=1
     */
    @GetMapping("/account")
    public Account getAccount(@RequestParam(defaultValue = "1") Long userId) {
        log.info("===== 查询账户 =====");
        return accountService.getAccountWithSupports(userId);
    }
}
