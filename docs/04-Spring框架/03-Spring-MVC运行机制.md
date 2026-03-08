# Spring MVC 运行机制详解

## 概述

Spring MVC 是基于 Servlet API 构建的 Web 框架，采用前端控制器模式（Front Controller Pattern），核心思想是将请求处理流程解耦为多个独立的组件。

## 核心架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端请求                               │
└───────────────────────────┬─────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                    DispatcherServlet                            │
│                   （前端控制器/中央调度器）                        │
└───────────────────────────┬─────────────────────────────────────┘
                            ↓
        ┌───────────────────┼───────────────────┐
        ↓                   ↓                   ↓
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ HandlerMapping│   │ HandlerAdapter│   │ ViewResolver  │
│  （处理器映射） │   │  （处理器适配） │   │  （视图解析）  │
└───────┬───────┘   └───────┬───────┘   └───────┬───────┘
        │                   │                   │
        ↓                   ↓                   ↓
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  Controller   │   │  Interceptor  │   │     View      │
│   （处理器）   │   │   （拦截器）   │   │   （视图）    │
└───────────────┘   └───────────────┘   └───────────────┘
```

## 请求处理流程

### 完整流程图

```
1. 请求到达
   ↓
2. DispatcherServlet.doDispatch()
   ↓
3. 调用 HandlerMapping 获取 HandlerExecutionChain
   ↓
4. 获取 HandlerAdapter
   ↓
5. 执行拦截器 preHandle()
   ↓
6. HandlerAdapter 调用处理器方法
   ├─ 6.1 参数解析（ArgumentResolver）
   ├─ 6.2 执行 Controller 方法
   └─ 6.3 返回值处理（ReturnValueHandler）
   ↓
7. 执行拦截器 postHandle()
   ↓
8. 处理异常（如果有）
   ↓
9. 渲染视图或处理响应
   ↓
10. 执行拦截器 afterCompletion()
   ↓
11. 返回响应
```

## 核心组件详解

### 1. DispatcherServlet（前端控制器）

**作用**：统一接收所有请求，协调各组件完成请求处理

**核心方法**：`doDispatch()`

```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
    // 1. 获取处理器执行链
    HandlerExecutionChain mappedHandler = getHandler(request);
    
    // 2. 获取处理器适配器
    HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
    
    // 3. 执行拦截器 preHandle
    if (!mappedHandler.applyPreHandle(request, response)) {
        return;
    }
    
    // 4. 调用处理器方法
    ModelAndView mv = ha.handle(request, response, mappedHandler.getHandler());
    
    // 5. 执行拦截器 postHandle
    mappedHandler.applyPostHandle(request, response, mv);
    
    // 6. 处理结果（渲染视图或写入响应）
    processDispatchResult(request, response, mappedHandler, mv, exception);
    
    // 7. 执行拦截器 afterCompletion
    mappedHandler.triggerAfterCompletion(request, response, null);
}
```

### 2. HandlerMapping（处理器映射）

**作用**：根据请求 URL 找到对应的处理器（Controller 方法）

**主要实现类**：
- `RequestMappingHandlerMapping`：处理 @RequestMapping 注解
- `BeanNameUrlHandlerMapping`：根据 Bean 名称映射
- `SimpleUrlHandlerMapping`：简单 URL 映射

**映射过程**：
```java
// 初始化时扫描 @RequestMapping
@RequestMapping("/users/{id}")
public User getUser(@PathVariable Long id) { ... }

// 请求到达时匹配
GET /users/123  →  匹配到 getUser 方法，id=123
```

### 3. HandlerAdapter（处理器适配器）

**作用**：适配不同形式的处理器，统一调用方式

**主要实现类**：
- `RequestMappingHandlerAdapter`：处理 @RequestMapping 方法
- `HttpRequestHandlerAdapter`：处理 HttpRequestHandler
- `SimpleControllerHandlerAdapter`：处理 Controller 接口

**核心方法**：
```java
ModelAndView handle(HttpServletRequest request, 
                    HttpServletResponse response, 
                    Object handler) throws Exception;
```

### 4. HandlerInterceptor（拦截器）

**作用**：在请求处理前后进行拦截处理

**三个回调方法**：
```java
public interface HandlerInterceptor {
    // 处理器执行前，返回 false 则中断处理
    boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                      Object handler);
    
    // 处理器执行后，视图渲染前
    void postHandle(HttpServletRequest request, HttpServletResponse response,
                    Object handler, ModelAndView modelAndView);
    
    // 视图渲染完成后（无论是否异常）
    void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                         Object handler, Exception ex);
}
```

**执行顺序**：
```
Interceptor1.preHandle()
    Interceptor2.preHandle()
        Controller.handle()
    Interceptor2.postHandle()
Interceptor1.postHandle()
渲染视图
Interceptor2.afterCompletion()
Interceptor1.afterCompletion()
```

### 5. HandlerMethodArgumentResolver（参数解析器）

**作用**：解析方法参数值

**内置解析器**：
- `@RequestParam` → RequestParamMethodArgumentResolver
- `@PathVariable` → PathVariableMethodArgumentResolver
- `@RequestBody` → RequestResponseBodyMethodProcessor
- `@ModelAttribute` → ServletModelAttributeMethodProcessor

**自定义解析器示例**：
```java
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }
    
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // 从 Token/Session 解析当前用户
        return userService.getCurrentUser();
    }
}
```

### 6. HttpMessageConverter（消息转换器）

**作用**：处理 HTTP 请求体和响应体的转换

**内置转换器**：
| 转换器 | 作用 |
|--------|------|
| MappingJackson2HttpMessageConverter | JSON 转换 |
| StringHttpMessageConverter | 字符串转换 |
| ByteArrayHttpMessageConverter | 字节数组转换 |
| FormHttpMessageConverter | 表单数据转换 |

**执行时机**：
- 请求：`@RequestBody` → 使用转换器将请求体转为 Java 对象
- 响应：`@ResponseBody` → 使用转换器将 Java 对象转为响应体

### 7. HandlerExceptionResolver（异常解析器）

**作用**：处理异常并返回 ModelAndView 或写入响应

**主要实现类**：
- `ExceptionHandlerExceptionResolver`：处理 @ExceptionHandler
- `DefaultHandlerExceptionResolver`：处理标准 Spring 异常
- `SimpleMappingExceptionResolver`：简单异常映射

**使用方式**：
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
```

### 8. ViewResolver（视图解析器）

**作用**：根据视图名称解析 View 对象

**主要实现类**：
- `InternalResourceViewResolver`：JSP 视图
- `ThymeleafViewResolver`：Thymeleaf 模板
- `FreeMarkerViewResolver`：FreeMarker 模板

**解析过程**：
```java
// 控制器返回视图名称
return "user/detail";

// ViewResolver 解析
prefix + viewName + suffix = /WEB-INF/views/user/detail.jsp
```

## 数据绑定机制

### 类型转换

**Converter**：简单类型转换
```java
public interface Converter<S, T> {
    T convert(S source);
}

// 示例：String → Date
@Component
public class StringToDateConverter implements Converter<String, Date> {
    @Override
    public Date convert(String source) {
        return new SimpleDateFormat("yyyy-MM-dd").parse(source);
    }
}
```

**Formatter**：带 Locale 的格式化
```java
public interface Formatter<T> extends Printer<T>, Parser<T> {
}

// 示例：货币格式化
@Component
public class MoneyFormatter implements Formatter<Money> {
    @Override
    public Money parse(String text, Locale locale) {
        // 解析货币
    }
    
    @Override
    public String print(Money object, Locale locale) {
        // 格式化货币
    }
}
```

### 数据绑定流程

```
请求参数
    ↓
DataBinder
    ├─ 1. 类型转换（Converter/Formatter）
    ├─ 2. 数据校验（Validator）
    └─ 3. 绑定到对象
    ↓
Controller 方法参数
```

## 常用注解

| 注解 | 作用 |
|------|------|
| `@Controller` | 标记控制器类 |
| `@RestController` | @Controller + @ResponseBody |
| `@RequestMapping` | 映射请求路径 |
| `@GetMapping` | GET 请求映射 |
| `@PostMapping` | POST 请求映射 |
| `@PutMapping` | PUT 请求映射 |
| `@DeleteMapping` | DELETE 请求映射 |
| `@PathVariable` | 路径变量 |
| `@RequestParam` | 请求参数 |
| `@RequestBody` | 请求体（JSON） |
| `@ResponseBody` | 响应体（JSON） |
| `@ModelAttribute` | 模型属性 |
| `@InitBinder` | 初始化数据绑定器 |
| `@ExceptionHandler` | 异常处理 |
| `@ControllerAdvice` | 全局控制器增强 |

## 最佳实践

### 1. 统一响应格式

```java
@ControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse) {
            return body;
        }
        return ApiResponse.success(body);
    }
}
```

### 2. 统一异常处理

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ApiResponse<Void> handleException(Exception e) {
        return ApiResponse.error(500, "系统内部错误");
    }
}
```

### 3. 拦截器使用

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                             Object handler) {
        String token = request.getHeader("Authorization");
        if (token == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
```

### 4. 性能优化

- **启用缓存**：配置 `WebDataBinder` 的自动增长
- **减少拦截器**：不必要的路径排除拦截
- **异步处理**：使用 `Callable` 或 `DeferredResult`
- **压缩响应**：启用 Gzip 压缩

## 调试技巧

### 1. 开启 DEBUG 日志

```yaml
logging:
  level:
    org.springframework.web: DEBUG
    org.springframework.web.servlet.DispatcherServlet: DEBUG
```

### 2. 查看 HandlerMapping 注册信息

```java
@Autowired
private RequestMappingHandlerMapping handlerMapping;

@PostConstruct
public void init() {
    handlerMapping.getHandlerMethods().forEach((key, value) -> {
        System.out.println(key + " -> " + value);
    });
}
```

### 3. 断点调试

关键断点位置：
- `DispatcherServlet.doDispatch()`
- `RequestMappingHandlerMapping.getHandler()`
- `RequestMappingHandlerAdapter.invokeHandlerMethod()`
- `HandlerMethodArgumentResolver.resolveArgument()`

---

## 💡 高频面试题

**问题 1：Spring MVC 的工作流程是什么？**

答案：
Spring MVC 采用前端控制器模式，核心流程如下：

```
1. 客户端发送请求 → DispatcherServlet（前端控制器）
2. DispatcherServlet → HandlerMapping（处理器映射）查找 Handler
3. HandlerMapping → 返回 HandlerExecutionChain（包含 Controller 和拦截器）
4. DispatcherServlet → HandlerAdapter（处理器适配器）执行 Handler
5. HandlerAdapter → 调用 Controller 方法
   - 参数解析（ArgumentResolver）
   - 执行业务逻辑
   - 返回值处理（ReturnValueHandler）
6. Controller 返回 ModelAndView 或数据
7. HandlerAdapter → 返回 ModelAndView 给 DispatcherServlet
8. DispatcherServlet → ViewResolver（视图解析器）解析视图
9. ViewResolver → 返回 View 对象
10. DispatcherServlet → 渲染视图并返回响应
```

**核心组件作用：**
| 组件 | 作用 |
|------|------|
| DispatcherServlet | 中央调度器，统一处理所有请求 |
| HandlerMapping | 根据 URL 找到对应的 Controller 方法 |
| HandlerAdapter | 适配不同类型的处理器，统一调用方式 |
| Controller | 实际的业务处理器 |
| ViewResolver | 将视图名称解析为 View 对象 |
| HandlerInterceptor | 拦截器，在请求前后执行自定义逻辑 |

**问题 2：DispatcherServlet 的作用是什么？**

答案：
**DispatcherServlet** 是 Spring MVC 的核心，作为前端控制器（Front Controller），承担以下职责：

**核心作用：**
1. **统一入口**：接收所有 HTTP 请求
2. **协调组件**：调度 HandlerMapping、HandlerAdapter、ViewResolver 等组件
3. **流程控制**：控制整个请求处理的流程
4. **异常处理**：统一处理异常

**主要工作：**
```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
    // 1. 获取 Handler（处理器）
    HandlerExecutionChain mappedHandler = getHandler(request);
    
    // 2. 获取 HandlerAdapter（适配器）
    HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
    
    // 3. 执行拦截器的 preHandle
    if (!mappedHandler.applyPreHandle(request, response)) return;
    
    // 4. 调用处理器方法
    ModelAndView mv = ha.handle(request, response, mappedHandler.getHandler());
    
    // 5. 执行拦截器的 postHandle
    mappedHandler.applyPostHandle(request, response, mv);
    
    // 6. 处理结果（渲染视图或写入响应）
    processDispatchResult(request, response, mappedHandler, mv, null);
    
    // 7. 执行拦截器的 afterCompletion
    mappedHandler.triggerAfterCompletion(request, response, null);
}
```

**问题 3：HandlerMapping 和 HandlerAdapter 的区别？**

答案：

| 特性 | HandlerMapping | HandlerAdapter |
|------|---------------|----------------|
| 作用 | 找到处理器 | 调用处理器 |
| 输入 | HTTP 请求 | Handler 对象 |
| 输出 | HandlerExecutionChain | ModelAndView |
| 核心方法 | getHandler() | handle() |

**HandlerMapping（找谁处理）：**
```java
// 根据 URL 找到对应的 Controller 方法
GET /users/123 → @GetMapping("/users/{id}") getUser(@PathVariable Long id)
```

**HandlerAdapter（怎么调用）：**
```java
// 统一调用方式，无论什么类型的 Handler
ModelAndView mv = adapter.handle(request, response, handler);
```

**为什么需要 Adapter？**
因为 Handler 有多种形式：
- `@Controller` 注解方法
- `HttpRequestHandler` 接口实现
- `Controller` 接口实现

通过 Adapter 统一调用方式，解耦 DispatcherServlet 与具体 Handler 类型。

**问题 4：拦截器和过滤器的区别？**

答案：

| 特性 | 过滤器（Filter） | 拦截器（Interceptor） |
|------|----------------|---------------------|
| 所属规范 | Servlet 规范 | Spring MVC 提供 |
| 运行容器 | Servlet 容器 | Spring 容器 |
| 触发时机 | 请求到达时最早执行 | DispatcherServlet 之后 |
| 作用范围 | 所有请求（包括静态资源） | 仅 Controller 请求 |
| 访问权限 | 无法访问 Spring Bean | 可以访问 Spring Bean |
| 执行顺序 | Filter → Interceptor → Controller |

**过滤器示例：**
```java
@Component
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) req;
        String token = request.getHeader("Authorization");
        // 验证 Token
        chain.doFilter(req, res); // 放行
    }
}
```

**拦截器示例：**
```java
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (token == null) {
            response.setStatus(401);
            return false; // 中断请求
        }
        return true; // 继续执行
    }
}
```

**使用建议：**
- 通用处理（编码、CORS）→ 用 Filter
- 业务相关（权限、日志）→ 用 Interceptor

**问题 5：@RestController 和@Controller 的区别？**

答案：

| 特性 | @Controller | @RestController |
|------|------------|----------------|
| 组成 | 基础注解 | @Controller + @ResponseBody |
| 返回值 | ModelAndView 或 String（视图名） | 直接返回数据（JSON/XML） |
| 适用场景 | 模板引擎（JSP、Thymeleaf） | RESTful API |
| 是否需要视图解析 | 是 | 否 |

**@Controller（返回视图）：**
```java
@Controller
public class UserController {
    @GetMapping("/user/{id}")
    public String getUser(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        return "user/detail"; // 返回视图名称 → JSP/Thymeleaf
    }
}
```

**@RestController（返回数据）：**
```java
@RestController
public class UserRestController {
    @GetMapping("/api/user/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id); // 直接返回对象 → JSON
    }
}
```

**@RestController = @Controller + @ResponseBody：**
```java
// 等价于
@RestController
public class UserRestController {}

// 等同于
@Controller
@ResponseBody
public class UserRestController {}
```

**问题 6：Spring MVC 如何处理异常？**

答案：
Spring MVC 提供三种异常处理方式：

**1. @ExceptionHandler（局部）**
```java
@Controller
public class UserController {
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseBody
    public ApiResponse handleUserNotFound(UserNotFoundException e) {
        return ApiResponse.error(404, e.getMessage());
    }
}
```

**2. @ControllerAdvice（全局）**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ApiResponse handleBusinessException(BusinessException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ApiResponse handleException(Exception e) {
        return ApiResponse.error(500, "系统内部错误");
    }
}
```

**3. SimpleMappingExceptionResolver（配置式）**
```java
@Bean
public SimpleMappingExceptionResolver exceptionResolver() {
    SimpleMappingExceptionResolver resolver = new SimpleMappingExceptionResolver();
    Properties mappings = new Properties();
    mappings.setProperty("UserNotFoundException", "error/404");
    mappings.setProperty("Exception", "error/500");
    resolver.setExceptionMappings(mappings);
    return resolver;
}
```

**执行优先级：**
```
@ControllerAdvice（全局） > @ExceptionHandler（局部） > ExceptionResolver（配置）
```

**问题 7：@RequestBody 和@ResponseBody 的作用？**

答案：

**@RequestBody（请求体 → Java 对象）：**
```java
@PostMapping("/users")
public User createUser(@RequestBody UserCreateRequest request) {
    // HTTP Body: {"name":"张三","age":18}
    // Spring 自动将 JSON 转为 Java 对象
    return userService.create(request);
}
```

**工作原理：**
1. 读取 HTTP 请求体
2. HttpMessageConverter 根据 Content-Type 选择转换器
3. MappingJackson2HttpMessageConverter 将 JSON 转为 Java 对象

**@ResponseBody（Java 对象 → 响应体）：**
```java
@GetMapping("/users/{id}")
@ResponseBody
public User getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return user; // Spring 自动将 Java 对象转为 JSON
}
```

**工作原理：**
1. 检查返回值类型
2. HttpMessageConverter 根据 Accept 头选择转换器
3. MappingJackson2HttpMessageConverter 将 Java 对象转为 JSON

**常用组合：**
```java
@RestController // = @Controller + @ResponseBody
public class UserRestController {
    @PostMapping("/api/users")
    public User create(@RequestBody UserCreateRequest request) {
        // 自动完成 JSON ↔ Java 对象的转换
    }
}
```

**问题 8：参数解析器和返回值处理器分别是什么？**

答案：

**HandlerMethodArgumentResolver（参数解析器）：**
负责解析 Controller 方法的参数值。

**常见解析器：**
| 注解 | 解析器 | 作用 |
|------|--------|------|
| @RequestParam | RequestParamMethodArgumentResolver | 解析请求参数 |
| @PathVariable | PathVariableMethodArgumentResolver | 解析路径变量 |
| @RequestBody | RequestResponseBodyMethodProcessor | 解析请求体 |
| @ModelAttribute | ServletModelAttributeMethodProcessor | 解析模型属性 |

**示例：**
```java
@GetMapping("/users/{id}")
public User getUser(
    @PathVariable Long id,              // PathVariableMethodArgumentResolver
    @RequestParam String name,          // RequestParamMethodArgumentResolver
    @RequestBody UserCreateRequest req  // RequestResponseBodyMethodProcessor
) {
    // 参数都已解析完成
}
```

**HandlerMethodReturnValueHandler（返回值处理器）：**
负责处理 Controller 方法的返回值。

**常见处理器：**
| 注解 | 处理器 | 作用 |
|------|--------|------|
| @ResponseBody | RequestResponseBodyMethodProcessor | 返回 JSON/XML |
| ModelAndView | ModelAndViewMethodReturnValueHandler | 返回视图 |
| String | ViewNameMethodReturnValueHandler | 返回视图名称 |

**问题 9：消息转换器（HttpMessageConverter）的作用？**

答案：
**HttpMessageConverter** 负责 HTTP 请求体和响应体与 Java 对象之间的转换。

**内置转换器：**
| 转换器 | 作用 |
|--------|------|
| MappingJackson2HttpMessageConverter | JSON ↔ Java 对象（最常用） |
| StringHttpMessageConverter | String ↔ 文本 |
| ByteArrayHttpMessageConverter | byte[] ↔ 二进制 |
| FormHttpMessageConverter | MultiValueMap ↔ 表单数据 |
| SourceHttpMessageConverter | XML ↔ Java 对象 |

**工作流程：**
```
请求：HTTP Body → HttpMessageConverter → Java 对象
响应：Java 对象 → HttpMessageConverter → HTTP Body
```

**配置示例：**
```yaml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    serialization:
      write-dates-as-timestamps: false
```

**自定义转换器：**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 添加自定义转换器
        converters.add(new FastJsonHttpMessageConverter());
    }
}
```

**问题 10：如何实现统一响应格式？**

答案：

**方式一：ResponseBodyAdvice（推荐）**
```java
@ControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true; // 对所有响应生效
    }
    
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse) {
            return body; // 已经是统一格式，不重复包装
        }
        return ApiResponse.success(body); // 包装为统一格式
    }
}
```

**方式二：手动包装**
```java
@RestController
public class UserController {
    @GetMapping("/users/{id}")
    public ApiResponse<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ApiResponse.success(user);
    }
}
```

**统一响应类：**
```java
@Data
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(200);
        resp.setMessage("success");
        resp.setData(data);
        return resp;
    }
}
```

**效果：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "张三"
  }
}
```
