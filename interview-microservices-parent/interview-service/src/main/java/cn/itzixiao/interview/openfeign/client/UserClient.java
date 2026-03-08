package cn.itzixiao.interview.openfeign.client;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.openfeign.dto.UserDTO;
import cn.itzixiao.interview.openfeign.fallback.UserClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务 Feign 客户端
 * <p>
 * 演示 OpenFeign 远程调用的标准用法
 *
 * <pre>
 * 关键注解说明：
 * - name/value: 服务名称（对应注册中心的服务名）
 * - url: 直接指定服务地址（用于测试或无注册中心场景）
 * - path: 接口统一前缀
 * - configuration: 指定配置类
 * - fallback: 降级实现类
 * - fallbackFactory: 降级工厂（推荐，可获取异常信息）
 * </pre>
 *
 * @author itzixiao
 * @since 1.0
 */
@FeignClient(
        name = "interview-provider",           // 服务名称
        path = "/api/users",                   // 接口前缀
        fallbackFactory = UserClientFallbackFactory.class  // 降级工厂
)
public interface UserClient {

    /**
     * 根据ID查询用户
     * <p>
     * 演示 @PathVariable 参数传递
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    Result<UserDTO> getById(@PathVariable("id") Long id);

    /**
     * 根据用户名查询用户
     * <p>
     * 演示 @RequestParam 参数传递
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/username")
    Result<UserDTO> getByUsername(@RequestParam("username") String username);

    /**
     * 查询用户列表
     * <p>
     * 演示分页查询和多参数传递
     *
     * @param page   页码
     * @param size   每页大小
     * @param status 状态筛选（可选）
     * @return 用户列表
     */
    @GetMapping("/list")
    Result<List<UserDTO>> list(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "status", required = false) Integer status
    );

    /**
     * 创建用户
     * <p>
     * 演示 @RequestBody 参数传递（POST JSON）
     *
     * @param user 用户信息
     * @return 创建结果
     */
    @PostMapping
    Result<UserDTO> create(@RequestBody UserDTO user);

    /**
     * 更新用户
     * <p>
     * 演示 PUT 请求 + @PathVariable + @RequestBody 组合
     *
     * @param id   用户ID
     * @param user 用户信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    Result<UserDTO> update(@PathVariable("id") Long id, @RequestBody UserDTO user);

    /**
     * 删除用户
     * <p>
     * 演示 DELETE 请求
     *
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    Result<Void> delete(@PathVariable("id") Long id);

    /**
     * 带请求头的查询
     * <p>
     * 演示 @RequestHeader 参数传递
     *
     * @param id    用户ID
     * @param token 认证令牌
     * @return 用户信息
     */
    @GetMapping("/auth/{id}")
    Result<UserDTO> getByIdWithAuth(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String token
    );
}
