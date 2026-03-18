package cn.itzixiao.interview.workflow.service;

import cn.itzixiao.interview.workflow.dto.ApprovalDTO;
import cn.itzixiao.interview.workflow.dto.ExpenseApplyDTO;
import cn.itzixiao.interview.workflow.entity.Expense;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 报销审批服务接口
 *
 * @author itzixiao
 * @date 2026-03-17
 */
public interface ExpenseService {

    Long apply(ExpenseApplyDTO dto);

    void approve(ApprovalDTO dto);

    void resubmit(Long expenseId, ExpenseApplyDTO dto);

    void withdraw(Long expenseId);

    IPage<Expense> myList(Page<Expense> page, Integer status);

    IPage<Expense> pendingList(Page<Expense> page);

    Expense detail(Long id);
}
