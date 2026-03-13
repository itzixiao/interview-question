package cn.itzixiao.interview.transaction.controller;

import cn.itzixiao.interview.transaction.entity.Account;
import cn.itzixiao.interview.transaction.entity.Order;
import cn.itzixiao.interview.transaction.service.AccountService;
import cn.itzixiao.interview.transaction.service.LogService;
import cn.itzixiao.interview.transaction.service.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
@Api(tags = "Spring事务传播机制演示 API", value = "事务传播行为测试接口")
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
    @ApiOperation(value = "01-REQUIRED 传播行为测试", notes = "当前有事务则加入，无则新建（默认传播行为）")
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
    @ApiOperation(value = "02-REQUIRES_NEW 传播行为测试", notes = "挂起当前事务，新建独立事务（适用于日志记录）")
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
    @ApiOperation(value = "03-NESTED 传播行为测试", notes = "在当前事务中创建嵌套事务（适用于批量处理部分回滚）")
    public void testNested(@RequestParam(defaultValue = "1") Long userId) {
        log.info("===== 测试 NESTED 传播行为 =====");
        orderService.batchCreateOrders(userId);
    }
    
    /**
     * 测试 SUPPORTS 传播行为
     * GET /api/transaction/supports?userId=1
     */
    @GetMapping("/supports")
    @ApiOperation(value = "04-SUPPORTS 传播行为测试", notes = "当前有事务则加入，无则以非事务执行（适用于查询）")
    public Account testSupports(@RequestParam(defaultValue = "1") Long userId) {
        log.info("===== 测试 SUPPORTS 传播行为 =====");
        return orderService.queryAccountWithSupports(userId);
    }
    
    /**
     * 测试 MANDATORY 传播行为（需要在事务中调用）
     * GET /api/transaction/mandatory?userId=1&amount=50
     */
    @GetMapping("/mandatory")
    @ApiOperation(value = "05-MANDATORY 传播行为测试", notes = "当前必须有事务，否则抛出异常")
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
    @ApiOperation(value = "06-NEVER 传播行为测试", notes = "当前必须无事务，否则抛出异常")
    public void testNever(@RequestParam(defaultValue = "1") Long userId) {
        log.info("===== 测试 NEVER 传播行为 =====");
        accountService.neverOperation(userId);
    }
    
    /**
     * 测试 NOT_SUPPORTED 传播行为
     * GET /api/transaction/not-supported?userId=1
     */
    @GetMapping("/not-supported")
    @ApiOperation(value = "07-NOT_SUPPORTED 传播行为测试", notes = "挂起当前事务，以非事务执行")
    public Account testNotSupported(@RequestParam(defaultValue = "1") Long userId) {
        log.info("===== 测试 NOT_SUPPORTED 传播行为 =====");
        return accountService.getAccountWithNotSupported(userId);
    }
    
    /**
     * 复杂业务场景演示
     * GET /api/transaction/complex?userId=1&amount=200
     */
    @GetMapping("/complex")
    @ApiOperation(value = "08-复杂业务场景演示", notes = "结合多种传播行为的完整业务流程")
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
    @ApiOperation(value = "09-日志服务测试（REQUIRES_NEW）", notes = "独立事务记录日志，不受外部事务影响")
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
    @ApiOperation(value = "10-查询账户余额", notes = "查询指定用户的账户余额信息")
    public Account getAccount(@RequestParam(defaultValue = "1") Long userId) {
        log.info("===== 查询账户 =====");
        return accountService.getAccountWithSupports(userId);
    }
}
