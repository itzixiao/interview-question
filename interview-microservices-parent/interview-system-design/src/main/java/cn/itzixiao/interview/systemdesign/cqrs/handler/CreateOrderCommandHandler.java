package cn.itzixiao.interview.systemdesign.cqrs.handler;

import cn.itzixiao.interview.systemdesign.cqrs.command.CommandResult;
import cn.itzixiao.interview.systemdesign.cqrs.command.CreateOrderCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 命令处理器 - 处理创建订单命令
 * <p>
 * 写模型职责：
 * 1. 业务规则验证
 * 2. 数据一致性保证
 * 3. 领域逻辑执行
 * 4. 发布领域事件
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class CreateOrderCommandHandler {

    /**
     * 处理创建订单命令
     */
    public CommandResult handle(CreateOrderCommand command) {
        log.info("处理创建订单命令：userId={}", command.getUserId());

        try {
            // 1. 参数验证
            validateCommand(command);

            // 2. 业务规则验证
            validateBusinessRules(command);

            // 3. 执行创建操作
            String orderId = createOrderInDatabase(command);

            // 4. 返回结果
            return CommandResult.success(orderId);

        } catch (IllegalArgumentException e) {
            log.error("参数验证失败", e);
            return CommandResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return CommandResult.error("系统繁忙，请稍后重试");
        }
    }

    /**
     * 验证命令参数
     */
    private void validateCommand(CreateOrderCommand command) {
        if (command.getUserId() == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (command.getProductIds() == null || command.getProductIds().trim().isEmpty()) {
            throw new IllegalArgumentException("商品不能为空");
        }
        if (command.getAddress() == null || command.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("收货地址不能为空");
        }
    }

    /**
     * 验证业务规则
     */
    private void validateBusinessRules(CreateOrderCommand command) {
        // 模拟业务规则验证
        log.info("验证业务规则：检查库存、用户权限等");
    }

    /**
     * 在数据库中创建订单
     */
    private String createOrderInDatabase(CreateOrderCommand command) {
        // 模拟数据库操作
        String orderId = "ORD" + System.currentTimeMillis();
        log.info("创建订单成功：orderId={}, userId={}", orderId, command.getUserId());
        return orderId;
    }
}
