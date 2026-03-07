package cn.itzixiao.interview.openfeign.fallback;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.openfeign.client.UserClient;
import cn.itzixiao.interview.openfeign.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * UserClient 降级工厂
 * <p>
 * 使用 FallbackFactory 可以获取触发降级的异常信息，便于日志记录和问题排查。
 *
 * <pre>
 * fallbackFactory vs fallback 区别：
 *
 * fallback:
 *   - 直接实现 FeignClient 接口
 *   - 无法获取异常信息
 *   - 适合简单场景
 *
 * fallbackFactory（推荐）:
 *   - 实现 FallbackFactory<T> 接口
 *   - create(Throwable cause) 方法可获取异常信息
 *   - 便于记录日志、区分异常类型
 * </pre>
 *
 * @author itzixiao
 * @since 1.0
 */
@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        // 记录异常日志
        log.error("UserClient 调用失败，触发降级。异常信息: {}", cause.getMessage(), cause);

        return new UserClient() {
            @Override
            public Result<UserDTO> getById(Long id) {
                log.warn("getById 降级处理, id={}", id);
                return Result.error(503, "用户服务暂不可用");
            }

            @Override
            public Result<UserDTO> getByUsername(String username) {
                log.warn("getByUsername 降级处理, username={}", username);
                return Result.error(503, "用户服务暂不可用");
            }

            @Override
            public Result<List<UserDTO>> list(Integer page, Integer size, Integer status) {
                log.warn("list 降级处理, page={}, size={}, status={}", page, size, status);
                // 返回空列表而不是错误，保证页面正常显示
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<UserDTO> create(UserDTO user) {
                log.warn("create 降级处理, user={}", user);
                return Result.error(503, "用户服务暂不可用，请稍后重试");
            }

            @Override
            public Result<UserDTO> update(Long id, UserDTO user) {
                log.warn("update 降级处理, id={}, user={}", id, user);
                return Result.error(503, "用户服务暂不可用，请稍后重试");
            }

            @Override
            public Result<Void> delete(Long id) {
                log.warn("delete 降级处理, id={}", id);
                return Result.error(503, "用户服务暂不可用，请稍后重试");
            }

            @Override
            public Result<UserDTO> getByIdWithAuth(Long id, String token) {
                log.warn("getByIdWithAuth 降级处理, id={}", id);
                return Result.error(503, "用户服务暂不可用");
            }
        };
    }
}
