package cn.itzixiao.interview.workflow.config;

import cn.itzixiao.interview.workflow.util.Result;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.StringJoiner;

/**
 * 全局异常处理器
 * - 统一封装 4xx / 5xx 错误响应为 Result 格式
 * - 针对 Flowable 引擎异常单独处理
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 参数校验失败（@Valid/@Validated）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidException(MethodArgumentNotValidException ex) {
        StringJoiner joiner = new StringJoiner("；");
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            joiner.add(error.getField() + " " + error.getDefaultMessage());
        }
        String msg = joiner.toString();
        if (msg.isEmpty()) {
            msg = "参数校验失败，请检查请求数据格式";
        }
        log.warn("参数校验失败: {}", msg);
        log.warn("异常详情: {}", ex.getMessage());
        return Result.fail(400, msg);
    }

    /**
     * 参数格式错误（JSON解析失败、类型不匹配等）
     */
    @ExceptionHandler(InvalidFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleInvalidFormat(InvalidFormatException ex) {
        log.warn("参数格式错误: {}", ex.getMessage());
        String msg = "参数格式错误: " + ex.getValue() + " 不是有效的 " + ex.getTargetType().getSimpleName();
        return Result.fail(400, msg);
    }

    /**
     * 缺少必要参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("缺少参数: {}", ex.getMessage());
        return Result.fail(400, "缺少必要参数: " + ex.getParameterName());
    }

    /**
     * 业务逻辑异常（主动抛出的 RuntimeException）
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleRuntimeException(RuntimeException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return Result.fail(400, ex.getMessage());
    }

    /**
     * Flowable 对象不存在（任务/流程实例找不到）
     */
    @ExceptionHandler(FlowableObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleFlowableNotFound(FlowableObjectNotFoundException ex) {
        log.warn("Flowable 资源不存在: {}", ex.getMessage());
        return Result.fail(404, "流程对象不存在: " + ex.getMessage());
    }

    /**
     * Flowable 引擎通用异常
     */
    @ExceptionHandler(FlowableException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleFlowableException(FlowableException ex) {
        log.error("Flowable 引擎异常: {}", ex.getMessage(), ex);
        return Result.fail(500, "工作流引擎异常: " + ex.getMessage());
    }

    /**
     * 认证失败（Spring Security）
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthException(AuthenticationException ex) {
        log.warn("认证失败: {}", ex.getMessage());
        return Result.fail(401, "认证失败，请重新登录");
    }

    /**
     * 权限不足（Spring Security）
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException ex) {
        log.warn("权限不足: {}", ex.getMessage());
        return Result.fail(403, "权限不足，无法访问该资源");
    }

    /**
     * 未知系统异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        log.error("系统异常: {}", ex.getMessage(), ex);
        return Result.fail(500, "系统内部错误，请联系管理员");
    }
}
