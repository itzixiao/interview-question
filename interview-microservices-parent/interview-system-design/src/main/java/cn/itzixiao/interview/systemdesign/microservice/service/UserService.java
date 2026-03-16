package cn.itzixiao.interview.systemdesign.microservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户微服务示例
 * <p>
 * 限界上下文：用户管理上下文
 * - 负责用户的注册、登录、信息管理
 * - 通过领域事件与其他服务解耦
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Service
public class UserService {

    /**
     * 用户注册
     */
    public void registerUser(Long userId, String username, String email) {
        log.info("用户注册：userId={}, username={}, email={}", userId, username, email);
        // 业务逻辑实现
    }

    /**
     * 更新用户信息
     */
    public void updateUserProfile(Long userId, String profileData) {
        log.info("更新用户信息：userId={}, profileData={}", userId, profileData);
        // 业务逻辑实现
    }

    /**
     * 获取用户信息
     */
    public String getUserInfo(Long userId) {
        log.info("获取用户信息：userId={}", userId);
        return "UserInfo-" + userId;
    }
}
