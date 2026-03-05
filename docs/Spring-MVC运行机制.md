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
