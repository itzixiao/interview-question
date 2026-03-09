package cn.itzixiao.interview.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

/**
 * 自定义 Actuator 端点
 * 
 * 作用：提供自定义的监控和管理接口
 * 访问路径：/actuator/demo
 * 使用场景：
 * 1. 自定义业务监控指标
 * 2. 动态配置管理
 * 3. 特殊运维操作
 */
@Component
@Endpoint(id = "demo")
public class DemoEndpoint {
    
    /**
     * 读操作 - GET /actuator/demo
     * 
     * @return 当前状态信息
     */
    @ReadOperation
    public String getStatus() {
        return "Demo endpoint is running! Current time: " + System.currentTimeMillis();
    }
    
    /**
     * 写操作 - POST /actuator/demo
     * 
     * @param message 传入的消息
     * @return 操作结果
     */
    @WriteOperation
    public String updateStatus(String message) {
        return "Demo endpoint updated with message: " + message;
    }
}
