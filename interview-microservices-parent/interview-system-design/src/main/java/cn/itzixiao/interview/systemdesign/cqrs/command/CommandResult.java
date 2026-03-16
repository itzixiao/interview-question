package cn.itzixiao.interview.systemdesign.cqrs.command;

import lombok.Data;

/**
 * 命令处理结果
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Data
public class CommandResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果数据（如生成的订单 ID）
     */
    private Object data;

    /**
     * 错误信息
     */
    private String errorMessage;

    public static CommandResult success(Object data) {
        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static CommandResult error(String message) {
        CommandResult result = new CommandResult();
        result.setSuccess(false);
        result.setErrorMessage(message);
        return result;
    }
}
