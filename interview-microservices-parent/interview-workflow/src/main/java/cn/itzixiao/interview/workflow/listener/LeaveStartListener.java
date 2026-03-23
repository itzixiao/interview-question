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

/**
 * 请假流程启动监听器
 * <p>
 * 触发时机：请假流程实例启动时（对应 BPMN 中 startEvent 的 executionListener）
 * <p>
 * 核心职责：
 * <ul>
 *   <li>根据申请人部门ID动态查找部门经理，设置流程变量 deptManagerUsername</li>
 *   <li>查找总经理用户，设置流程变量 gmUsername</li>
 *   <li>以上变量用于 BPMN 中 userTask 的 flowable:assignee 动态指定审批人</li>
 * </ul>
 * <p>
 * 注意：Flowable 通过反射实例化监听器，绕过 Spring 容器，
 * 因此使用 {@code staticUserMapper} + {@code @PostConstruct} 模式确保 Mapper 可用
 *
 * @author itzixiao
 * @date 2026-03-17
 * @see cn.itzixiao.interview.workflow.mapper.UserMapper#selectDeptManagerByDeptId
 * @see cn.itzixiao.interview.workflow.mapper.UserMapper#selectGeneralManager
 */
@Slf4j
@Component("leaveStartListener")
public class LeaveStartListener implements ExecutionListener {

    private static final long serialVersionUID = 9128332390095018053L;

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
     * 流程启动时执行 - 设置审批人变量
     *
     * @param execution Flowable 执行上下文，包含流程变量
     */
    @Override
    public void notify(DelegateExecution execution) {
        // 从流程变量中获取申请信息
        Long deptId = (Long) execution.getVariable("deptId");
        String applicantUsername = (String) execution.getVariable("applicantUsername");
        Integer leaveDays = (Integer) execution.getVariable("leaveDays");

        log.info("=== 请假流程启动 ===");
        log.info("申请人: {}, 部门ID: {}, 请假天数: {}", applicantUsername, deptId, leaveDays);

        // Mapper 未初始化时的降级处理
        if (staticUserMapper == null) {
            log.error("UserMapper 未初始化，使用默认审批人 admin");
            execution.setVariable("deptManagerUsername", "admin");
            execution.setVariable("gmUsername", "admin");
            return;
        }

        // 动态获取部门经理（根据 sys_dept.manager_id 查找）
        User deptManager = staticUserMapper.selectDeptManagerByDeptId(deptId);
        if (deptManager != null) {
            execution.setVariable("deptManagerUsername", deptManager.getUsername());
            log.info("部门经理: {} ({})", deptManager.getRealName(), deptManager.getUsername());
        } else {
            log.warn("部门[{}]未找到对应经理，将使用 admin 作为默认审批人", deptId);
            execution.setVariable("deptManagerUsername", "admin");
        }

        // 动态获取总经理（根据角色编码 GENERAL_MANAGER 查找）
        User gm = staticUserMapper.selectGeneralManager();
        if (gm != null) {
            execution.setVariable("gmUsername", gm.getUsername());
            log.info("总经理: {} ({})", gm.getRealName(), gm.getUsername());
        } else {
            execution.setVariable("gmUsername", "admin");
        }
    }
}
