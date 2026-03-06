package cn.itzixiao.interview.springmvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring MVC 运行机制详解
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                        Spring MVC 请求处理流程                                │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │                                                                             │
 * │   1. 请求到达 DispatcherServlet（前端控制器）                                  │
 * │          ↓                                                                  │
 * │   2. HandlerMapping 查找处理器（Controller）                                  │
 * │          ↓                                                                  │
 * │   3. HandlerAdapter 调用处理器方法                                           │
 * │          ↓                                                                  │
 * │   4. 参数解析（HandlerMethodArgumentResolver）                               │
 * │          ↓                                                                  │
 * │   5. 执行 Controller 方法                                                   │
 * │          ↓                                                                  │
 * │   6. 返回值处理（HandlerMethodReturnValueHandler）                            │
 * │          ↓                                                                  │
 * │   7. 视图解析或消息转换                                                       │
 * │          ↓                                                                  │
 * │   8. 返回响应                                                                │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 核心组件：
 * - DispatcherServlet: 前端控制器，统一接收和分发请求
 * - HandlerMapping: 映射请求到处理器
 * - HandlerAdapter: 适配不同形式的处理器
 * - HandlerInterceptor: 拦截器，预处理和后处理
 * - ViewResolver: 视图解析器
 * - HandlerExceptionResolver: 异常处理器
 */
@SpringBootApplication
public class SpringMvcMechanismDemo {

    public static void main(String[] args) {
        SpringApplication.run(SpringMvcMechanismDemo.class, args);
        System.out.println("\n========================================");
        System.out.println("Spring MVC 运行机制示例已启动");
        System.out.println("访问: http://localhost:8080/mvc/");
        System.out.println("========================================\n");
    }

    /**
     * 配置拦截器和参数解析器
     */
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                // 注册拦截器
                registry.addInterceptor(new LoggingInterceptor())
                        .addPathPatterns("/mvc/**")
                        .excludePathPatterns("/mvc/login");

                registry.addInterceptor(new AuthInterceptor())
                        .addPathPatterns("/mvc/admin/**");
            }

            @Override
            public void addArgumentResolvers(java.util.List<HandlerMethodArgumentResolver> resolvers) {
                // 注册自定义参数解析器
                resolvers.add(new CurrentUserArgumentResolver());
            }
        };
    }
}

/**
 * ============================================
 * 1. 拦截器 (HandlerInterceptor)
 * ============================================
 * 作用：在请求处理前后进行拦截处理
 * - preHandle: 处理器执行前（返回 false 则中断）
 * - postHandle: 处理器执行后，视图渲染前
 * - afterCompletion: 视图渲染完成后
 */
@Component
class LoggingInterceptor extends HandlerInterceptorAdapter {

    private static final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startTime.set(System.currentTimeMillis());
        System.out.println("\n【拦截器】LoggingInterceptor.preHandle");
        System.out.println("  请求路径: " + request.getRequestURI());
        System.out.println("  请求方法: " + request.getMethod());
        System.out.println("  处理器: " + handler.getClass().getSimpleName());
        return true; // 返回 true 继续执行，false 中断
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        System.out.println("【拦截器】LoggingInterceptor.postHandle");
        if (modelAndView != null) {
            System.out.println("  视图名称: " + modelAndView.getViewName());
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long cost = System.currentTimeMillis() - startTime.get();
        System.out.println("【拦截器】LoggingInterceptor.afterCompletion");
        System.out.println("  请求耗时: " + cost + " ms");
        if (ex != null) {
            System.out.println("  异常信息: " + ex.getMessage());
        }
        startTime.remove();
    }
}

/**
 * 认证拦截器
 */
@Component
class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("【拦截器】AuthInterceptor.preHandle - 检查用户权限");
        // 模拟权限检查
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            System.out.println("  未登录，拒绝访问");
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"Unauthorized\"}");
            return false;
        }
        System.out.println("  用户已认证，允许访问");
        return true;
    }
}

/**
 * ============================================
 * 2. 自定义参数解析器 (HandlerMethodArgumentResolver)
 * ============================================
 * 作用：自定义方法参数的解析逻辑
 * 例如：从请求头、Session 或 Token 中解析当前用户
 */
class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 判断是否需要使用此解析器
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        System.out.println("【参数解析器】CurrentUserArgumentResolver.resolveArgument");
        // 模拟从 Token 或 Session 中解析用户信息
        User user = new User();
        user.setId(1L);
        user.setUsername("zhangsan");
        user.setRole("ADMIN");
        System.out.println("  解析到用户: " + user.getUsername());
        return user;
    }
}

/**
 * 自定义注解：标记当前用户参数
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface CurrentUser {
}

/**
 * ============================================
 * 3. 全局响应处理 (ResponseBodyAdvice)
 * ============================================
 * 作用：统一处理 Controller 的返回值
 * 例如：统一包装响应格式、加密、签名等
 */
@ControllerAdvice(basePackages = "cn.itzixiao.interview.springmvc")
class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 判断是否需要进行处理
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        System.out.println("【响应处理】GlobalResponseAdvice.beforeBodyWrite");
        // 统一包装响应
        if (body instanceof ApiResponse) {
            return body;
        }
        // 已经是 String 类型特殊处理
        if (body instanceof String) {
            return body;
        }
        return ApiResponse.success(body);
    }
}

/**
 * ============================================
 * 4. 全局异常处理 (@ControllerAdvice + @ExceptionHandler)
 * ============================================
 * 作用：统一处理异常
 */
@ControllerAdvice
class MvcDemoGlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        System.out.println("【异常处理】BusinessException: " + e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ApiResponse<Void> handleException(Exception e) {
        System.out.println("【异常处理】Exception: " + e.getMessage());
        return ApiResponse.error(500, "系统内部错误");
    }
}

/**
 * ============================================
 * 5. Controller 示例
 * ============================================
 */
@Controller
@RequestMapping("/mvc")
class MvcDemoController {

    /**
     * 基本请求处理
     */
    @GetMapping("/hello")
    @ResponseBody
    public String hello() {
        System.out.println("【Controller】执行 hello() 方法");
        return "Hello Spring MVC!";
    }

    /**
     * 路径变量
     */
    @GetMapping("/user/{id}")
    @ResponseBody
    public Map<String, Object> getUser(@PathVariable Long id) {
        System.out.println("【Controller】执行 getUser() 方法，id=" + id);
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("name", "User" + id);
        return user;
    }

    /**
     * 请求参数绑定
     */
    @GetMapping("/search")
    @ResponseBody
    public Map<String, Object> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        System.out.println("【Controller】执行 search() 方法");
        System.out.println("  参数: keyword=" + keyword + ", page=" + page + ", size=" + size);

        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 请求体绑定（JSON）
     */
    @PostMapping("/user")
    @ResponseBody
    public Map<String, Object> createUser(@RequestBody User user) {
        System.out.println("【Controller】执行 createUser() 方法");
        System.out.println("  请求体: " + user);

        Map<String, Object> result = new HashMap<>();
        result.put("id", System.currentTimeMillis());
        result.put("user", user);
        return result;
    }

    /**
     * 自定义参数解析器使用
     */
    @GetMapping("/profile")
    @ResponseBody
    public Map<String, Object> getProfile(@CurrentUser User user) {
        System.out.println("【Controller】执行 getProfile() 方法");
        Map<String, Object> result = new HashMap<>();
        result.put("currentUser", user);
        return result;
    }

    /**
     * 数据绑定（表单提交）
     */
    @PostMapping("/bind")
    @ResponseBody
    public Map<String, Object> dataBind(User user) {
        System.out.println("【Controller】执行 dataBind() 方法");
        System.out.println("  绑定结果: " + user);

        Map<String, Object> result = new HashMap<>();
        result.put("boundUser", user);
        return result;
    }

    /**
     * 视图渲染（Thymeleaf/JSP）
     */
    @GetMapping("/view")
    public String viewPage(Model model) {
        System.out.println("【Controller】执行 viewPage() 方法");
        model.addAttribute("title", "Spring MVC Demo");
        model.addAttribute("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        return "demo"; // 视图名称，由 ViewResolver 解析
    }

    /**
     * 异常触发
     */
    @GetMapping("/error")
    @ResponseBody
    public String triggerError(@RequestParam(required = false) String type) {
        System.out.println("【Controller】执行 triggerError() 方法");
        if ("business".equals(type)) {
            throw new BusinessException(4001, "业务异常：参数错误");
        }
        throw new RuntimeException("系统运行时异常");
    }

    /**
     * 需要权限的接口
     */
    @GetMapping("/admin/info")
    @ResponseBody
    public Map<String, Object> adminInfo() {
        System.out.println("【Controller】执行 adminInfo() 方法");
        Map<String, Object> result = new HashMap<>();
        result.put("message", "管理员信息");
        result.put("secret", "top-secret-data");
        return result;
    }

    /**
     * 初始化数据绑定器
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        System.out.println("【数据绑定】@InitBinder 初始化绑定器");
        // 可以在这里配置自定义的编辑器
        // binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));
    }
}

/**
 * ============================================
 * 6. 实体类
 * ============================================
 */
class User {
    private Long id;
    private String username;
    private String role;
    private Integer age;
    private String email;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role='" + role + "', age=" + age + "}";
    }
}

/**
 * 统一响应包装类
 */
class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    // Getters and Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

/**
 * 业务异常
 */
class BusinessException extends RuntimeException {
    private int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }
}
