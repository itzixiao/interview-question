package cn.itzixiao.interview.workflow.listener;

import cn.itzixiao.interview.workflow.entity.User;
import cn.itzixiao.interview.workflow.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;

/**
 * 报销流程启动监听器
 * <p>
 * 触发时机：报销流程实例启动时（对应 BPMN 中 startEvent 的 executionListener）
 * <p>
 * 核心职责：根据报销金额设置不同层级的审批人
 * <ul>
 *   <li>金额 < 1000：部门经理审批</li>
 *   <li>1000 <= 金额 <= 5000：财务经理审批</li>
 *   <li>金额 > 5000：总经理审批</li>
 * </ul>
 * <p>
 * 设置的流程变量：
 * <ul>
 *   <li>deptManagerUsername - 部门经理用户名</li>
 *   <li>financeManagerUsername - 财务经理用户名</li>
 *   <li>gmUsername - 总经理用户名</li>
 * </ul>
 *
 * @author itzixiao
 * @date 2026-03-17
 * @see cn.itzixiao.interview.workflow.mapper.UserMapper#selectDeptManagerByDeptId
 * @see cn.itzixiao.interview.workflow.mapper.UserMapper#selectFinanceManager
 * @see cn.itzixiao.interview.workflow.mapper.UserMapper#selectGeneralManager
 */
@Slf4j
@Component("expenseStartListener")
public class ExpenseStartListener implements ExecutionListener {

    private static final long serialVersionUID = 5846506116342687843L;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 静态 Mapper 引用 - 解决 Flowable 监听器无法直接使用 @Autowired 的问题
     */
    private static UserMapper staticUserMapper;

    /**
     * Spring 容器启动时初始化静态 Mapper
     */
    @PostConstruct
    public void init() {
        staticUserMapper = applicationContext.getBean(UserMapper.class);
    }

    /**
     * 流程启动时执行 - 设置各级审批人变量
     *
     * @param execution Flowable 执行上下文，包含流程变量
     */
    @Override
    public void notify(DelegateExecution execution) {
        // 从流程变量中获取申请信息
        Long deptId = (Long) execution.getVariable("deptId");
        String applicantUsername = (String) execution.getVariable("applicantUsername");
        BigDecimal amount = (BigDecimal) execution.getVariable("amount");

        log.info("=== 报销流程启动 ===");
        log.info("申请人: {}, 部门ID: {}, 金额: {}", applicantUsername, deptId, amount);

        // Mapper 未初始化时的降级处理
        if (staticUserMapper == null) {
            log.error("UserMapper 未初始化，使用默认审批人 admin");
            execution.setVariable("deptManagerUsername", "admin");
            execution.setVariable("financeManagerUsername", "admin");
            execution.setVariable("gmUsername", "admin");
            return;
        }

        // 设置部门经理（根据 sys_dept.manager_id 查找）
        User deptManager = staticUserMapper.selectDeptManagerByDeptId(deptId);
        if (deptManager != null) {
            execution.setVariable("deptManagerUsername", deptManager.getUsername());
            log.info("部门经理: {} ({})", deptManager.getRealName(), deptManager.getUsername());
        } else {
            execution.setVariable("deptManagerUsername", "admin");
        }

        // 设置财务经理（根据角色编码 FINANCE_MANAGER 查找）
        User financeManager = staticUserMapper.selectFinanceManager();
        if (financeManager != null) {
            execution.setVariable("financeManagerUsername", financeManager.getUsername());
            log.info("财务经理: {} ({})", financeManager.getRealName(), financeManager.getUsername());
        } else {
            execution.setVariable("financeManagerUsername", "admin");
        }

        // 设置总经理（根据角色编码 GENERAL_MANAGER 查找）
        User gm = staticUserMapper.selectGeneralManager();
        if (gm != null) {
            execution.setVariable("gmUsername", gm.getUsername());
            log.info("总经理: {} ({})", gm.getRealName(), gm.getUsername());
        } else {
            execution.setVariable("gmUsername", "admin");
        }

        log.info("当前报销金额: {}, 将走对应审批路径", amount);
    }
}
