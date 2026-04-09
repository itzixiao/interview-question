package cn.itzixiao.interview.warmflow.config;

import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Warm-Flow 办理人权限处理器
 * 
 * @author itzixiao
 * @since 2026-04-09
 */
@Slf4j
@Component
public class CustomPermissionHandler implements PermissionHandler {

    // 使用 ThreadLocal 存储当前审批人ID
    private static final ThreadLocal<String> currentHandler = new ThreadLocal<>();

    /**
     * 设置当前审批人ID（由业务代码调用）
     */
    public static void setCurrentHandler(String handler) {
        currentHandler.set(handler);
        log.debug("设置当前审批人: {}", handler);
    }

    /**
     * 清除当前审批人ID
     */
    public static void clearCurrentHandler() {
        currentHandler.remove();
    }

    /**
     * 获取当前操作用户的所有权限标识
     * 返回的权限列表会用于与节点的 permission_flag 进行匹配
     */
    @Override
    public List<String> permissions() {
        String handler = currentHandler.get();
        if (handler != null) {
            // 返回包含当前handler的权限列表
            List<String> permissions = new ArrayList<>();
            permissions.add(handler);
            log.debug("返回权限列表: {}", permissions);
            return permissions;
        }
        return Collections.emptyList();
    }

    /**
     * 获取当前办理人（唯一标识）
     * 通常返回用户ID，用于记录流程实例的创建人、办理人等
     */
    @Override
    public String getHandler() {
        String handler = currentHandler.get();
        log.debug("获取当前办理人: {}", handler);
        return handler;
    }

    /**
     * 转换办理人
     * 比如设计器中预设了能办理的人包含角色或部门ID，可以通过此接口转换成具体的用户ID
     */
    @Override
    public List<String> convertPermissions(List<String> permissions) {
        log.debug("转换前的权限: {}", permissions);
        // 默认直接返回，不做转换
        return permissions;
    }
}
