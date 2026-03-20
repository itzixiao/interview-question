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
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring MVC 运行机制详解
 * <p>
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
 * <p>
 * 核心组件：
 * - DispatcherServlet: 前端控制器，统一接收和分发请求
 * - HandlerMapping: 映射请求到处理器
 * - HandlerAdapter: 适配不同形式的处理器
 * - HandlerInterceptor: 拦截器，预处理和后处理
 * - ViewResolver: 视图解析器
 * - HandlerExceptionResolver: 异常处理器
 * <p>
 * 文档参考：docs/05-Spring框架/03-Spring-MVC运行机制.md
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
 * doDispatch() 核心流程模拟
 * ============================================
 * 这是 DispatcherServlet.doDispatch() 方法的模拟实现
 * 用于理解 Spring MVC 请求处理的核心流程
 * <p>
 * 完整流程：
 * 1. checkMultipart() - 检查文件上传
 * 2. getHandler() - 获取处理器执行链
 * 3. getHandlerAdapter() - 获取处理器适配器
 * 4. applyPreHandle() - 执行拦截器前置处理
 * 5. ha.handle() - 调用处理器方法
 * 6. applyPostHandle() - 执行拦截器后置处理
 * 7. processDispatchResult() - 处理结果
 * 8. triggerAfterCompletion() - 执行拦截器完成处理
 */
class DoDispatchSimulator {

    /**
     * 模拟 DispatcherServlet.doDispatch() 核心流程
     * <p>
     * 源码参考：
     * protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
     *     HttpServletRequest processedRequest = request;
     *     HandlerExecutionChain mappedHandler = null;
     *     boolean multipartRequestParsed = false;
     *
     *     try {
     *         ModelAndView mv = null;
     *         Exception dispatchException = null;
     *
     *         try {
     *             // 步骤 1：处理文件上传
     *             processedRequest = checkMultipart(request);
     *             multipartRequestParsed = (processedRequest != request);
     *
     *             // 步骤 2：获取 Handler
     *             mappedHandler = getHandler(processedRequest);
     *             if (mappedHandler == null) {
     *                 noHandlerFound(processedRequest, response);
     *                 return;
     *             }
     *
     *             // 步骤 3：获取 HandlerAdapter
     *             HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
     *
     *             // 步骤 4：处理 Last-Modified
     *             // ...
     *
     *             // 步骤 5：执行拦截器 preHandle
     *             if (!mappedHandler.applyPreHandle(processedRequest, response)) {
     *                 return;
     *             }
     *
     *             // 步骤 6：调用 Handler
     *             mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
     *
     *             // 步骤 7：处理异步请求
     *             // ...
     *
     *             // 步骤 8：处理默认视图名称
     *             applyDefaultViewName(processedRequest, mv);
     *
     *             // 步骤 9：执行拦截器 postHandle
     *             mappedHandler.applyPostHandle(processedRequest, response, mv);
     *         }
     *         catch (Exception ex) {
     *             dispatchException = ex;
     *         }
     *
     *         // 步骤 10：处理结果
     *         processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
     *     }
     *     finally {
     *         // 步骤 11-12：清理资源
     *         if (multipartRequestParsed) {
     *             cleanupMultipart(processedRequest);
     *         }
     *     }
     * }
     */
    public void simulateDoDispatch(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("\n========== doDispatch() 核心流程模拟 ==========");

        Object handler = null;
        boolean multipartRequestParsed = false;

        try {
            ModelAndView mv = null;
            Exception dispatchException = null;

            try {
                // ==================== 步骤 1：处理文件上传 ====================
                System.out.println("\n【步骤 1】checkMultipart() - 检查是否为文件上传请求");
                HttpServletRequest processedRequest = checkMultipart(request);
                multipartRequestParsed = (processedRequest != request);
                System.out.println("  结果: " + (multipartRequestParsed ? "是文件上传请求" : "普通请求"));

                // ==================== 步骤 2：获取 Handler ====================
                System.out.println("\n【步骤 2】getHandler() - 获取处理器执行链");
                // 调用所有 HandlerMapping，找到能处理当前请求的 Handler
                HandlerExecutionChainSimulator mappedHandler = getHandler(processedRequest);
                if (mappedHandler == null) {
                    System.out.println("  结果: 404 - 未找到处理器");
                    return;
                }
                handler = mappedHandler.getHandler();
                System.out.println("  结果: 找到处理器 " + handler.getClass().getSimpleName());

                // ==================== 步骤 3：获取 HandlerAdapter ====================
                System.out.println("\n【步骤 3】getHandlerAdapter() - 获取处理器适配器");
                HandlerAdapterSimulator ha = getHandlerAdapter(handler);
                System.out.println("  结果: " + ha.getClass().getSimpleName());

                // ==================== 步骤 4：处理 Last-Modified ====================
                System.out.println("\n【步骤 4】处理 Last-Modified 缓存检查");
                String method = request.getMethod();
                boolean isGet = "GET".equals(method);
                if (isGet) {
                    System.out.println("  GET 请求，检查 Last-Modified");
                    // long lastModified = ha.getLastModified(request, handler);
                    // 如果资源未修改，返回 304
                }

                // ==================== 步骤 5：执行拦截器 preHandle ====================
                System.out.println("\n【步骤 5】applyPreHandle() - 执行拦截器前置处理");
                // 按顺序执行所有拦截器的 preHandle 方法
                // 如果任一拦截器返回 false，请求处理终止
                boolean continueProcessing = applyPreHandle(mappedHandler, processedRequest, response);
                if (!continueProcessing) {
                    System.out.println("  结果: 拦截器中断请求");
                    return;
                }
                System.out.println("  结果: 所有拦截器 preHandle 返回 true");

                // ==================== 步骤 6：调用 Handler（核心） ====================
                System.out.println("\n【步骤 6】ha.handle() - 调用处理器方法");
                // HandlerAdapter 调用 Controller 方法
                // 内部流程：参数解析 → 执行方法 → 返回值处理
                mv = ha.handle(processedRequest, response, handler);
                System.out.println("  结果: ModelAndView = " + (mv != null ? mv.getViewName() : "null (REST响应)"));

                // ==================== 步骤 7：处理异步请求 ====================
                System.out.println("\n【步骤 7】检查异步请求处理");
                // if (asyncManager.isConcurrentHandlingStarted()) { return; }

                // ==================== 步骤 8：处理默认视图名称 ====================
                System.out.println("\n【步骤 8】applyDefaultViewName() - 处理默认视图名称");
                // applyDefaultViewName(processedRequest, mv);

                // ==================== 步骤 9：执行拦截器 postHandle ====================
                System.out.println("\n【步骤 9】applyPostHandle() - 执行拦截器后置处理");
                // 按逆序执行所有拦截器的 postHandle 方法
                applyPostHandle(mappedHandler, processedRequest, response, mv);
            } catch (Exception ex) {
                dispatchException = ex;
                System.out.println("\n【异常】请求处理异常: " + ex.getMessage());
            }

            // ==================== 步骤 10：处理结果 ====================
            System.out.println("\n【步骤 10】processDispatchResult() - 处理结果");
            // 渲染视图或写入响应，处理异常
            processDispatchResult(request, response, handler, mv, dispatchException);

        } finally {
            // ==================== 步骤 11-12：清理资源 ====================
            if (multipartRequestParsed) {
                System.out.println("\n【清理】cleanupMultipart() - 清理文件上传资源");
            }
        }

        System.out.println("\n========== doDispatch() 流程结束 ==========\n");
    }

    // 以下是模拟方法
    private HttpServletRequest checkMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("multipart/")) {
            return request; // 实际会包装为 MultipartHttpServletRequest
        }
        return request;
    }

    private HandlerExecutionChainSimulator getHandler(HttpServletRequest request) {
        // 模拟：遍历所有 HandlerMapping 查找 Handler
        return new HandlerExecutionChainSimulator(new Object(), new ArrayList<>());
    }

    private HandlerAdapterSimulator getHandlerAdapter(Object handler) {
        return new HandlerAdapterSimulator();
    }

    private boolean applyPreHandle(HandlerExecutionChainSimulator chain, HttpServletRequest request, HttpServletResponse response) {
        return true; // 模拟所有拦截器都通过
    }

    private void applyPostHandle(HandlerExecutionChainSimulator chain, HttpServletRequest request, HttpServletResponse response, ModelAndView mv) {
        // 模拟执行 postHandle
    }

    private void processDispatchResult(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mv, Exception ex) {
        if (ex != null) {
            System.out.println("  处理异常: " + ex.getMessage());
        } else if (mv != null) {
            System.out.println("  渲染视图: " + mv.getViewName());
        } else {
            System.out.println("  REST响应已写入");
        }
    }

    // 模拟类
    static class HandlerExecutionChainSimulator {
        private final Object handler;
        private final List<HandlerInterceptor> interceptors;

        public HandlerExecutionChainSimulator(Object handler, List<HandlerInterceptor> interceptors) {
            this.handler = handler;
            this.interceptors = interceptors;
        }

        public Object getHandler() {
            return handler;
        }
    }

    static class HandlerAdapterSimulator {
        public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            return null; // 模拟 REST 响应
        }
    }
}

/**
 * ============================================
 * HandlerMapping 匹配过程详解
 * ============================================
 * HandlerMapping 负责根据请求 URL 找到对应的处理器（Controller 方法）
 * <p>
 * 主要实现类：
 * - RequestMappingHandlerMapping：处理 @RequestMapping 注解
 * - BeanNameUrlHandlerMapping：根据 Bean 名称映射
 * - SimpleUrlHandlerMapping：简单 URL 映射
 * <p>
 * 匹配过程：
 * 1. 获取请求路径 lookupPath
 * 2. 加读锁保证并发安全
 * 3. 从映射注册表查找 HandlerMethod
 * 4. 构建 HandlerExecutionChain（包含拦截器）
 */
class HandlerMappingSimulator {

    /**
     * 模拟 URL 映射注册表
     * Key: URL 模式, Value: Handler 方法信息
     */
    private final Map<String, HandlerMethodInfo> urlHandlerMap = new ConcurrentHashMap<>();

    public HandlerMappingSimulator() {
        // 初始化：扫描 @RequestMapping 注册映射
        registerHandlerMethod("/users", "list", "GET");
        registerHandlerMethod("/users/{id}", "getById", "GET");
        registerHandlerMethod("/users", "create", "POST");
        registerHandlerMethod("/users/{id}", "update", "PUT");
        registerHandlerMethod("/users/{id}", "delete", "DELETE");
    }

    /**
     * 注册 Handler 方法
     */
    private void registerHandlerMethod(String pattern, String methodName, String httpMethod) {
        HandlerMethodInfo info = new HandlerMethodInfo(pattern, methodName, httpMethod);
        urlHandlerMap.put(pattern, info);
        System.out.println("【HandlerMapping】注册映射: " + httpMethod + " " + pattern + " -> " + methodName + "()");
    }

    /**
     * 模拟 getHandler() 方法
     * <p>
     * 源码参考：
     * public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
     *     // 1. 查找 HandlerMethod
     *     Object handler = getHandlerInternal(request);
     *     if (handler == null) {
     *         handler = getDefaultHandler();
     *     }
     *     if (handler == null) {
     *         return null;
     *     }
     *
     *     // 2. 如果是 String，从容器获取 Bean
     *     if (handler instanceof String) {
     *         String handlerName = (String) handler;
     *         handler = obtainApplicationContext().getBean(handlerName);
     *     }
     *
     *     // 3. 构建 HandlerExecutionChain
     *     HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);
     *
     *     // 4. 处理 CORS
     *     if (hasCorsConfigurationSource(handler)) {
     *         CorsConfiguration corsConfig = getCorsConfiguration(handler, request);
     *         executionChain = getCorsHandlerExecutionChain(request, executionChain, corsConfig);
     *     }
     *
     *     return executionChain;
     * }
     */
    public HandlerExecutionChainSimulator getHandler(HttpServletRequest request) {
        System.out.println("\n========== HandlerMapping.getHandler() 流程 ==========");

        // 步骤 1：获取请求路径
        String lookupPath = request.getRequestURI();
        System.out.println("【步骤 1】获取请求路径: " + lookupPath);

        // 步骤 2：查找 HandlerMethod
        HandlerMethodInfo handlerMethod = lookupHandlerMethod(lookupPath, request.getMethod());
        if (handlerMethod == null) {
            System.out.println("【结果】404 - 未找到匹配的处理器");
            return null;
        }
        System.out.println("【步骤 2】找到处理器方法: " + handlerMethod.methodName + "()");

        // 步骤 3：提取路径变量
        Map<String, String> uriVariables = extractUriVariables(handlerMethod.pattern, lookupPath);
        if (!uriVariables.isEmpty()) {
            System.out.println("【步骤 3】提取路径变量: " + uriVariables);
        }

        // 步骤 4：构建 HandlerExecutionChain（包含拦截器）
        HandlerExecutionChainSimulator chain = new HandlerExecutionChainSimulator(handlerMethod, new ArrayList<>());
        System.out.println("【步骤 4】构建 HandlerExecutionChain");

        System.out.println("========== HandlerMapping.getHandler() 结束 ==========\n");
        return chain;
    }

    /**
     * 查找 HandlerMethod
     * <p>
     * 源码参考：
     * protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
     *     // 1. 获取请求路径
     *     String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
     *
     *     // 2. 加读锁
     *     this.readWriteLock.readLock().lock();
     *     try {
     *         // 3. 从映射注册表查找
     *         HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
     *         return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
     *     }
     *     finally {
     *         this.readWriteLock.readLock().unlock();
     *     }
     * }
     */
    private HandlerMethodInfo lookupHandlerMethod(String lookupPath, String httpMethod) {
        System.out.println("【lookupHandlerMethod】查找路径: " + lookupPath + ", 方法: " + httpMethod);

        // 精确匹配
        if (urlHandlerMap.containsKey(lookupPath)) {
            HandlerMethodInfo info = urlHandlerMap.get(lookupPath);
            if (info.httpMethod.equals(httpMethod)) {
                return info;
            }
        }

        // 模式匹配（如 /users/{id}）
        for (Map.Entry<String, HandlerMethodInfo> entry : urlHandlerMap.entrySet()) {
            String pattern = entry.getKey();
            if (isPatternMatch(pattern, lookupPath, httpMethod)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * 模式匹配
     * 使用 AntPathMatcher 匹配路径模式
     */
    private boolean isPatternMatch(String pattern, String lookupPath, String httpMethod) {
        HandlerMethodInfo info = urlHandlerMap.get(pattern);
        if (info == null || !info.httpMethod.equals(httpMethod)) {
            return false;
        }

        // 简化匹配逻辑：检查路径变量模式
        // 实际使用 AntPathMatcher.match()
        if (pattern.contains("{") && pattern.contains("}")) {
            String regex = pattern.replaceAll("\\{[^}]+\\}", "[^/]+");
            return lookupPath.matches(regex);
        }

        return false;
    }

    /**
     * 提取路径变量
     * 例如：pattern="/users/{id}", lookupPath="/users/123"
     * 结果：{id: "123"}
     */
    private Map<String, String> extractUriVariables(String pattern, String lookupPath) {
        Map<String, String> variables = new LinkedHashMap<>();

        if (!pattern.contains("{")) {
            return variables;
        }

        String[] patternParts = pattern.split("/");
        String[] pathParts = lookupPath.split("/");

        for (int i = 0; i < patternParts.length && i < pathParts.length; i++) {
            String patternPart = patternParts[i];
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                String variableName = patternPart.substring(1, patternPart.length() - 1);
                variables.put(variableName, pathParts[i]);
            }
        }

        return variables;
    }

    /**
     * Handler 方法信息
     */
    static class HandlerMethodInfo {
        String pattern;
        String methodName;
        String httpMethod;

        public HandlerMethodInfo(String pattern, String methodName, String httpMethod) {
            this.pattern = pattern;
            this.methodName = methodName;
            this.httpMethod = httpMethod;
        }
    }

    static class HandlerExecutionChainSimulator {
        private final Object handler;
        private final List<HandlerInterceptor> interceptors;

        public HandlerExecutionChainSimulator(Object handler, List<HandlerInterceptor> interceptors) {
            this.handler = handler;
            this.interceptors = interceptors;
        }

        public Object getHandler() {
            return handler;
        }
    }
}

/**
 * ============================================
 * 参数解析详细流程模拟
 * ============================================
 * HandlerMethodArgumentResolver 负责解析 Controller 方法的参数值
 * <p>
 * 常见参数解析器：
 * - @RequestParam → RequestParamMethodArgumentResolver
 * - @PathVariable → PathVariableMethodArgumentResolver
 * - @RequestBody → RequestResponseBodyMethodProcessor
 * - @RequestHeader → RequestHeaderMethodArgumentResolver
 * - @CookieValue → ServletCookieValueMethodArgumentResolver
 * - HttpSession → SessionObjectMethodArgumentResolver
 * <p>
 * 解析流程：
 * 1. getMethodArgumentValues() - 获取所有参数值
 * 2. 遍历每个参数，查找支持的解析器
 * 3. 调用解析器的 resolveArgument() 方法
 * 4. 类型转换（如需要）
 */
class ArgumentResolverSimulator {

    /**
     * 模拟参数解析过程
     * <p>
     * 源码参考：
     * protected Object[] getMethodArgumentValues(NativeWebRequest request,
     *         ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {
     *
     *     // 1. 获取方法参数信息
     *     MethodParameter[] parameters = getMethodParameters();
     *     Object[] args = new Object[parameters.length];
     *
     *     for (int i = 0; i < parameters.length; i++) {
     *         MethodParameter parameter = parameters[i];
     *         parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
     *
     *         // 2. 检查是否提供了参数值
     *         args[i] = findProvidedArgument(parameter, providedArgs);
     *         if (args[i] != null) {
     *             continue;
     *         }
     *
     *         // 3. 检查是否有支持的参数解析器
     *         if (!this.resolvers.supportsParameter(parameter)) {
     *             throw new IllegalStateException("No suitable resolver");
     *         }
     *
     *         // 4. 使用解析器解析参数值
     *         args[i] = this.resolvers.resolveArgument(
     *             parameter, mavContainer, request, this.dataBinderFactory);
     *     }
     *     return args;
     * }
     */
    public Object[] resolveMethodArguments(Method method, HttpServletRequest request, Map<String, String> uriVariables) {
        System.out.println("\n========== 参数解析流程 ==========");
        System.out.println("【目标方法】" + method.getName() + "()");

        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            java.lang.reflect.Parameter parameter = parameters[i];
            System.out.println("\n【参数 " + (i + 1) + "】" + parameter.getName() + ": " + parameter.getType().getSimpleName());

            // 检查注解，选择对应的解析器
            if (parameter.isAnnotationPresent(PathVariable.class)) {
                args[i] = resolvePathVariable(parameter, uriVariables);
            } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                args[i] = resolveRequestParam(parameter, request);
            } else if (parameter.isAnnotationPresent(RequestBody.class)) {
                args[i] = resolveRequestBody(parameter, request);
            } else if (parameter.isAnnotationPresent(RequestHeader.class)) {
                args[i] = resolveRequestHeader(parameter, request);
            } else {
                // 默认解析器
                args[i] = resolveDefault(parameter, request);
            }

            System.out.println("  解析结果: " + args[i]);
        }

        System.out.println("\n========== 参数解析完成 ==========");
        return args;
    }

    /**
     * 解析 @PathVariable 参数
     * <p>
     * 源码参考 PathVariableMethodArgumentResolver：
     * 1. 从 URI 模板变量中获取值
     * 2. 类型转换（String → 目标类型）
     */
    private Object resolvePathVariable(java.lang.reflect.Parameter parameter, Map<String, String> uriVariables) {
        System.out.println("  解析器: PathVariableMethodArgumentResolver");

        PathVariable annotation = parameter.getAnnotation(PathVariable.class);
        String name = annotation.value().isEmpty() ? parameter.getName() : annotation.value();

        String value = uriVariables.get(name);
        System.out.println("  从 URI 变量获取: " + name + " = " + value);

        // 类型转换
        return convertType(value, parameter.getType());
    }

    /**
     * 解析 @RequestParam 参数
     * <p>
     * 源码参考 RequestParamMethodArgumentResolver：
     * 1. 从请求参数中获取值
     * 2. 处理 required 和 defaultValue
     * 3. 类型转换
     */
    private Object resolveRequestParam(java.lang.reflect.Parameter parameter, HttpServletRequest request) {
        System.out.println("  解析器: RequestParamMethodArgumentResolver");

        RequestParam annotation = parameter.getAnnotation(RequestParam.class);
        String name = annotation.value().isEmpty() ? parameter.getName() : annotation.value();

        String value = request.getParameter(name);
        if (value == null && !annotation.defaultValue().isEmpty()) {
            value = annotation.defaultValue();
        }
        System.out.println("  从请求参数获取: " + name + " = " + value);

        return convertType(value, parameter.getType());
    }

    /**
     * 解析 @RequestBody 参数
     * <p>
     * 源码参考 RequestResponseBodyMethodProcessor：
     * 1. 读取请求体
     * 2. 根据 Content-Type 选择 HttpMessageConverter
     * 3. 反序列化为 Java 对象
     */
    private Object resolveRequestBody(java.lang.reflect.Parameter parameter, HttpServletRequest request) {
        System.out.println("  解析器: RequestResponseBodyMethodProcessor");
        System.out.println("  Content-Type: " + request.getContentType());

        // 模拟：读取请求体并反序列化
        System.out.println("  选择消息转换器: MappingJackson2HttpMessageConverter");
        System.out.println("  反序列化 JSON → " + parameter.getType().getSimpleName());

        return null; // 实际会返回反序列化后的对象
    }

    /**
     * 解析 @RequestHeader 参数
     */
    private Object resolveRequestHeader(java.lang.reflect.Parameter parameter, HttpServletRequest request) {
        System.out.println("  解析器: RequestHeaderMethodArgumentResolver");

        RequestHeader annotation = parameter.getAnnotation(RequestHeader.class);
        String name = annotation.value().isEmpty() ? parameter.getName() : annotation.value();

        String value = request.getHeader(name);
        System.out.println("  从请求头获取: " + name + " = " + value);

        return convertType(value, parameter.getType());
    }

    /**
     * 默认解析器
     */
    private Object resolveDefault(java.lang.reflect.Parameter parameter, HttpServletRequest request) {
        System.out.println("  解析器: 默认解析器（ServletModelAttributeMethodProcessor）");
        return null;
    }

    /**
     * 类型转换
     */
    private Object convertType(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType == String.class) {
            return value;
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value);
        }

        return value;
    }
}

/**
 * ============================================
 * 返回值处理详细流程模拟
 * ============================================
 * HandlerMethodReturnValueHandler 负责处理 Controller 方法的返回值
 * <p>
 * 常见返回值处理器：
 * - @ResponseBody → RequestResponseBodyMethodProcessor
 * - ModelAndView → ModelAndViewMethodReturnValueHandler
 * - String → ViewNameMethodReturnValueHandler
 * - void → VoidReturnValueHandler
 * <p>
 * 处理流程：
 * 1. 选择返回值处理器
 * 2. 调用 handleReturnValue() 方法
 * 3. 如果是 REST 响应，使用 HttpMessageConverter 序列化
 * 4. 如果是视图，使用 ViewResolver 解析
 */
class ReturnValueHandlerSimulator {

    /**
     * 模拟返回值处理过程
     * <p>
     * 源码参考：
     * public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
     *         ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
     *
     *     // 1. 选择返回值处理器
     *     HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
     *     if (handler == null) {
     *         throw new IllegalArgumentException("Unknown return value type");
     *     }
     *
     *     // 2. 处理返回值
     *     handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
     * }
     */
    public void handleReturnValue(Object returnValue, Method method, HttpServletRequest request, HttpServletResponse response) {
        System.out.println("\n========== 返回值处理流程 ==========");
        System.out.println("【返回值类型】" + (returnValue != null ? returnValue.getClass().getSimpleName() : "null"));
        System.out.println("【返回值】" + returnValue);

        // 步骤 1：选择返回值处理器
        ReturnValueHandler handler = selectHandler(returnValue, method);
        System.out.println("【选择的处理器】" + handler.getClass().getSimpleName());

        // 步骤 2：处理返回值
        handler.handleReturnValue(returnValue, method, request, response);

        System.out.println("========== 返回值处理完成 ==========\n");
    }

    /**
     * 选择返回值处理器
     */
    private ReturnValueHandler selectHandler(Object returnValue, Method method) {
        // 检查是否有 @ResponseBody 注解
        if (method.isAnnotationPresent(ResponseBody.class) ||
            method.getDeclaringClass().isAnnotationPresent(RestController.class)) {
            return new RequestResponseBodyProcessor();
        }

        // 检查返回值类型
        if (returnValue instanceof ModelAndView) {
            return new ModelAndViewReturnValueHandler();
        }

        if (returnValue instanceof String) {
            return new ViewNameReturnValueHandler();
        }

        if (returnValue == null || returnValue instanceof Void) {
            return new VoidReturnValueHandler();
        }

        // 默认使用 REST 处理器
        return new RequestResponseBodyProcessor();
    }

    /**
     * 返回值处理器接口
     */
    interface ReturnValueHandler {
        boolean supportsReturnType(Method method);
        void handleReturnValue(Object returnValue, Method method, HttpServletRequest request, HttpServletResponse response);
    }

    /**
     * RequestResponseBodyMethodProcessor 模拟
     * <p>
     * 源码参考：
     * public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
     *         ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
     *
     *     // 1. 标记请求已处理
     *     mavContainer.setRequestHandled(true);
     *
     *     // 2. 获取原始请求和响应
     *     ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
     *     ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);
     *
     *     // 3. 使用消息转换器写入响应
     *     writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
     * }
     */
    static class RequestResponseBodyProcessor implements ReturnValueHandler {
        @Override
        public boolean supportsReturnType(Method method) {
            return method.isAnnotationPresent(ResponseBody.class) ||
                   method.getDeclaringClass().isAnnotationPresent(RestController.class);
        }

        @Override
        public void handleReturnValue(Object returnValue, Method method, HttpServletRequest request, HttpServletResponse response) {
            System.out.println("  【RequestResponseBodyProcessor】处理 REST 响应");

            // 步骤 1：标记请求已处理
            System.out.println("  步骤 1: mavContainer.setRequestHandled(true)");

            // 步骤 2：获取 Content-Type
            String accept = request.getHeader("Accept");
            String contentType = accept != null ? accept : "application/json";
            System.out.println("  步骤 2: Content-Type = " + contentType);

            // 步骤 3：选择消息转换器
            System.out.println("  步骤 3: 选择 MappingJackson2HttpMessageConverter");

            // 步骤 4：序列化并写入响应
            System.out.println("  步骤 4: 序列化对象 → JSON");
            String json = simulateJsonSerialization(returnValue);
            System.out.println("  JSON: " + json);

            // 步骤 5：写入响应
            System.out.println("  步骤 5: 写入 response.getOutputStream()");
        }

        private String simulateJsonSerialization(Object obj) {
            if (obj == null) {
                return "null";
            }
            if (obj instanceof Map) {
                return obj.toString().replace("=", ":");
            }
            return "{\"data\":\"" + obj + "\"}";
        }
    }

    /**
     * ModelAndViewMethodReturnValueHandler 模拟
     */
    static class ModelAndViewReturnValueHandler implements ReturnValueHandler {
        @Override
        public boolean supportsReturnType(Method method) {
            return ModelAndView.class.isAssignableFrom(method.getReturnType());
        }

        @Override
        public void handleReturnValue(Object returnValue, Method method, HttpServletRequest request, HttpServletResponse response) {
            System.out.println("  【ModelAndViewReturnValueHandler】处理视图响应");
            ModelAndView mav = (ModelAndView) returnValue;
            System.out.println("  视图名称: " + mav.getViewName());
            System.out.println("  模型数据: " + mav.getModel());
        }
    }

    /**
     * ViewNameMethodReturnValueHandler 模拟
     */
    static class ViewNameReturnValueHandler implements ReturnValueHandler {
        @Override
        public boolean supportsReturnType(Method method) {
            return String.class.isAssignableFrom(method.getReturnType());
        }

        @Override
        public void handleReturnValue(Object returnValue, Method method, HttpServletRequest request, HttpServletResponse response) {
            System.out.println("  【ViewNameReturnValueHandler】处理视图名称");
            String viewName = (String) returnValue;
            System.out.println("  视图名称: " + viewName);
            System.out.println("  将由 ViewResolver 解析");
        }
    }

    /**
     * VoidReturnValueHandler 模拟
     */
    static class VoidReturnValueHandler implements ReturnValueHandler {
        @Override
        public boolean supportsReturnType(Method method) {
            return void.class.isAssignableFrom(method.getReturnType()) ||
                   Void.class.isAssignableFrom(method.getReturnType());
        }

        @Override
        public void handleReturnValue(Object returnValue, Method method, HttpServletRequest request, HttpServletResponse response) {
            System.out.println("  【VoidReturnValueHandler】无返回值");
            System.out.println("  响应已由方法内部处理");
        }
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
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

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
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
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

    public int getCode() {
        return code;
    }
}
