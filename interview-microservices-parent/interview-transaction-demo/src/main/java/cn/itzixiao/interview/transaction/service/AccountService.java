package cn.itzixiao.interview.transaction.service;

import cn.itzixiao.interview.transaction.entity.Account;
import cn.itzixiao.interview.transaction.mapper.AccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 账户服务 - 用于演示各种事务传播行为
 *
 * @author itzixiao
 * @since 2026-03-13
 */
@Slf4j
@Service
public class AccountService {

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private LogService logService;

    /**
     * 测试 REQUIRED 传播行为（默认）
     * 当前有事务则加入，无则新建
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void depositWithRequired(Long userId, BigDecimal amount) {
        log.info("[REQUIRED] 存款操作：userId={}, amount={}", userId, amount);

        Account account = accountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
        );

        if (account == null) {
            throw new RuntimeException("账户不存在");
        }

        account.setBalance(account.getBalance().add(amount));
        accountMapper.updateById(account);

        log.info("[REQUIRED] 存款成功：userId={}, 新余额={}", userId, account.getBalance());
    }

    /**
     * 测试 REQUIRES_NEW 传播行为
     * 挂起当前事务，创建新事务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void depositWithRequiresNew(Long userId, BigDecimal amount) {
        log.info("[REQUIRES_NEW] 存款操作：userId={}, amount={}", userId, amount);

        Account account = accountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
        );

        if (account == null) {
            throw new RuntimeException("账户不存在");
        }

        account.setBalance(account.getBalance().add(amount));
        accountMapper.updateById(account);

        log.info("[REQUIRES_NEW] 存款成功：userId={}, 新余额={}", userId, account.getBalance());
    }

    /**
     * 测试 NESTED 传播行为
     * 在当前事务中创建嵌套事务（savepoint）
     */
    @Transactional(propagation = Propagation.NESTED)
    public void depositWithNested(Long userId, BigDecimal amount) {
        log.info("[NESTED] 存款操作：userId={}, amount={}", userId, amount);

        Account account = accountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
        );

        if (account == null) {
            throw new RuntimeException("账户不存在");
        }

        account.setBalance(account.getBalance().add(amount));
        accountMapper.updateById(account);

        log.info("[NESTED] 存款成功：userId={}, 新余额={}", userId, account.getBalance());
    }

    /**
     * 测试 SUPPORTS 传播行为
     * 当前有事务则加入，无则以非事务执行
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Account getAccountWithSupports(Long userId) {
        log.info("[SUPPORTS] 查询账户：userId={}", userId);

        Account account = accountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
        );

        log.info("[SUPPORTS] 查询结果：{}", account);
        return account;
    }

    /**
     * 测试 NOT_SUPPORTED 传播行为
     * 挂起当前事务，以非事务执行
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Account getAccountWithNotSupported(Long userId) {
        log.info("[NOT_SUPPORTED] 查询账户（非事务）：userId={}", userId);

        Account account = accountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
        );

        log.info("[NOT_SUPPORTED] 查询结果：{}", account);
        return account;
    }

    /**
     * 测试 MANDATORY 传播行为
     * 必须在事务中执行，否则抛异常
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void mandatoryOperation(Long userId, BigDecimal amount) {
        log.info("[MANDATORY] 必须在事务中执行的操作：userId={}, amount={}", userId, amount);

        Account account = accountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
        );

        if (account == null) {
            throw new RuntimeException("账户不存在");
        }

        account.setBalance(account.getBalance().add(amount));
        accountMapper.updateById(account);

        log.info("[MANDATORY] 操作成功");
    }

    /**
     * 测试 NEVER 传播行为
     * 必须在非事务环境中执行
     */
    @Transactional(propagation = Propagation.NEVER)
    public void neverOperation(Long userId) {
        log.info("[NEVER] 必须在非事务中执行的操作：userId={}", userId);

        Account account = accountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
        );

        log.info("[NEVER] 查询结果：{}", account);
    }
}
