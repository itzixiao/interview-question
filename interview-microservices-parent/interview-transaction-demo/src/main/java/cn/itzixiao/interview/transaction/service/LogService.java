package cn.itzixiao.interview.transaction.service;

import cn.itzixiao.interview.transaction.entity.OperationLog;
import cn.itzixiao.interview.transaction.mapper.OperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 日志服务 - 用于演示 REQUIRES_NEW 传播行为
 *
 * @author itzixiao
 * @since 2026-03-13
 */
@Slf4j
@Service
public class LogService {

    @Resource
    private OperationLogMapper operationLogMapper;

    /**
     * 记录成功日志 - 使用 REQUIRES_NEW 确保独立提交
     * 无论外部事务是否回滚，日志都会保存
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(String operation, String detail) {
        log.info("[REQUIRES_NEW] 记录成功日志：operation={}, detail={}", operation, detail);

        OperationLog logEntity = new OperationLog();
        logEntity.setOperation(operation);
        logEntity.setDetail(detail);
        logEntity.setStatus("SUCCESS");
        logEntity.setCreateTime(LocalDateTime.now());

        operationLogMapper.insert(logEntity);

        // 模拟可能的异常
        if ("FAIL".equals(detail)) {
            throw new RuntimeException("日志记录失败");
        }
    }

    /**
     * 记录失败日志 - 使用 REQUIRES_NEW
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(String operation, String detail, String errorMsg) {
        log.info("[REQUIRES_NEW] 记录失败日志：operation={}, detail={}, errorMsg={}",
                operation, detail, errorMsg);

        OperationLog logEntity = new OperationLog();
        logEntity.setOperation(operation);
        logEntity.setDetail(detail + " - " + errorMsg);
        logEntity.setStatus("FAIL");
        logEntity.setCreateTime(LocalDateTime.now());

        operationLogMapper.insert(logEntity);
    }

    /**
     * 记录日志 - 使用 REQUIRED（默认）
     * 会加入外部事务，外部回滚则日志也回滚
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void logWithRequired(String operation, String detail) {
        log.info("[REQUIRED] 记录日志：operation={}, detail={}", operation, detail);

        OperationLog logEntity = new OperationLog();
        logEntity.setOperation(operation);
        logEntity.setDetail(detail);
        logEntity.setStatus("SUCCESS");
        logEntity.setCreateTime(LocalDateTime.now());

        operationLogMapper.insert(logEntity);
    }
}
