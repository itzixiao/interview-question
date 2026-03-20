# Spring MVC 运行机制详解

## 概述

Spring MVC 是基于 Servlet API 构建的 Web 框架，采用前端控制器模式（Front Controller Pattern），核心思想是将请求处理流程解耦为多个独立的组件。

## 目录

- [核心架构图](#核心架构图)
- [请求处理流程](#请求处理流程)
  - [一、请求到达阶段](#一请求到达阶段从浏览器到-dispatcherservlet)
  - [二、doDispatch() 核心处理流程](#二doDispatch-核心处理流程)
  - [三、HandlerMapping 匹配过程详解](#三handlermapping-匹配过程详解)
  - [四、HandlerAdapter 调用过程详解](#四handleradapter-调用过程详解)
  - [五、参数解析详细流程](#五参数解析详细流程)
  - [六、返回值处理详细流程](#六返回值处理详细流程)
  - [七、视图渲染流程](#七视图渲染流程传统-mvc-模式)
  - [八、响应返回流程](#八响应返回流程)
  - [九、完整请求时序图](#九完整请求时序图)
  - [十、关键断点调试位置](#十关键断点调试位置)
- [核心组件详解](#核心组件详解)
- [数据绑定机制](#数据绑定机制)
- [常用注解](#常用注解)
- [最佳实践](#最佳实践)
- [调试技巧](#调试技巧)
- [高频面试题](#-高频面试题)

## 核心架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端请求                                │
└───────────────────────────┬─────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                    DispatcherServlet                            │
│                   （前端控制器/中央调度器）                         │
└───────────────────────────┬─────────────────────────────────────┘
                            ↓
        ┌───────────────────┼───────────────────┐
        ↓                   ↓                   ↓
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ HandlerMapping│   │ HandlerAdapter│   │ ViewResolver  │
│  （处理器映射）  │   │  （处理器适配） │   │  （视图解析）   │
└───────┬───────┘   └───────┬───────┘   └───────┬───────┘
        │                   │                   │
        ↓                   ↓                   ↓
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  Controller   │   │  Interceptor  │   │     View      │
│   （处理器）    │   │   （拦截器）    │   │   （视图）     │
└───────────────┘   └───────────────┘   └───────────────┘
```

## 请求处理流程

### 一、请求到达阶段（从浏览器到 DispatcherServlet）

#### 1.1 完整请求链路

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   浏览器     │ ─▶ │  Web服务器   │ ─▶ │ Servlet容器  │ ─▶ │ Dispatcher  │
│  发起请求    │    │ (Tomcat)    │    │   Engine    │    │  Servlet    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                  │                  │                  │
       │                  │                  │                  │
       ▼                  ▼                  ▼                  ▼
   HTTP请求          监听端口           路由到Servlet        处理请求
   GET /users/1      8080端口          匹配URL模式          doDispatch()
```

#### 1.2 详细步骤

```
步骤 1：浏览器发起 HTTP 请求
─────────────────────────────
- 构建 HTTP 请求报文
- DNS 解析获取服务器 IP
- 建立 TCP 连接（三次握手）
- 发送 HTTP 请求：
  GET /users/123 HTTP/1.1
  Host: localhost:8080
  Content-Type: application/json
  Authorization: Bearer xxx

步骤 2：Tomcat 接收请求
─────────────────────────────
- NIO/APR 线程模型监听端口
- Acceptor 线程接收连接
- Poller 线程轮询 I/O 事件
- 从线程池获取 Worker 线程处理

步骤 3：Servlet 容器路由
─────────────────────────────
- Engine：虚拟主机匹配
- Host：域名匹配（localhost）
- Context：应用上下文匹配（/myapp）
- Wrapper：Servlet 匹配（DispatcherServlet）

步骤 4：调用 DispatcherServlet.service()
─────────────────────────────
- HttpServlet.service() 判断请求方法
- 调用 doGet()/doPost() 等方法
- 最终调用 FrameworkServlet.processRequest()
- 核心：调用 doDispatch() 方法
```

### 二、doDispatch() 核心处理流程

#### 2.1 完整源码分析

```java
// DispatcherServlet.doDispatch() 完整流程
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {
    
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    boolean multipartRequestParsed = false;
    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

    try {
        ModelAndView mv = null;
        Exception dispatchException = null;

        try {
            // ==================== 步骤 1：处理文件上传 ====================
            processedRequest = checkMultipart(request);
            multipartRequestParsed = (processedRequest != request);

            // ==================== 步骤 2：获取 Handler ====================
            // 调用所有 HandlerMapping，找到能处理当前请求的 Handler
            mappedHandler = getHandler(processedRequest);
            if (mappedHandler == null) {
                noHandlerFound(processedRequest, response);
                return;  // 404 错误
            }

            // ==================== 步骤 3：获取 HandlerAdapter ====================
            // 根据 Handler 类型找到对应的适配器
            HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

            // ==================== 步骤 4：处理 Last-Modified ====================
            String method = request.getMethod();
            boolean isGet = "GET".equals(method);
            if (isGet) {
                long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
                if ((new ServletWebRequest(request, response)).checkNotModified(lastModified)) {
                    return;  // 304 未修改，直接返回
                }
            }

            // ==================== 步骤 5：执行拦截器 preHandle ====================
            // 按顺序执行所有拦截器的 preHandle 方法
            // 如果任一拦截器返回 false，请求处理终止
            if (!mappedHandler.applyPreHandle(processedRequest, response)) {
                return;
            }

            // ==================== 步骤 6：调用 Handler（核心） ====================
            // HandlerAdapter 调用 Controller 方法
            mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

            // ==================== 步骤 7：处理异步请求 ====================
            if (asyncManager.isConcurrentHandlingStarted()) {
                return;  // 异步处理，直接返回
            }

            // ==================== 步骤 8：处理默认视图名称 ====================
            applyDefaultViewName(processedRequest, mv);

            // ==================== 步骤 9：执行拦截器 postHandle ====================
            // 按逆序执行所有拦截器的 postHandle 方法
            mappedHandler.applyPostHandle(processedRequest, response, mv);
        }
        catch (Exception ex) {
            dispatchException = ex;
        }
        catch (Throwable err) {
            dispatchException = new NestedServletException("Handler dispatch failed", err);
        }

        // ==================== 步骤 10：处理结果 ====================
        // 渲染视图或写入响应，处理异常
        processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
    }
    finally {
        // ==================== 步骤 11：异步处理完成 ====================
        if (asyncManager.isConcurrentHandlingStarted()) {
            mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
            return;
        }

        // ==================== 步骤 12：清理资源 ====================
        if (multipartRequestParsed) {
            cleanupMultipart(processedRequest);
        }
    }
}
```

#### 2.2 流程时序图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           doDispatch() 完整流程                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  请求到达                                                                     │
│     │                                                                        │
│     ▼                                                                        │
│  ┌──────────────────┐                                                        │
│  │ checkMultipart() │  检查是否为文件上传请求                                   │
│  └────────┬─────────┘                                                        │
│           │                                                                  │
│           ▼                                                                  │
│  ┌──────────────────┐     ┌─────────────────────────────────┐               │
│  │  getHandler()    │────▶│ HandlerMapping1.getHandler()    │               │
│  └────────┬─────────┘     │ HandlerMapping2.getHandler()    │               │
│           │               │ ...                              │               │
│           │               │ 返回 HandlerExecutionChain       │               │
│           │               └─────────────────────────────────┘               │
│           ▼                                                                  │
│  ┌──────────────────┐     ┌─────────────────────────────────┐               │
│  │getHandlerAdapter │────▶│ 遍历所有 HandlerAdapter          │               │
│  └────────┬─────────┘     │ 找到 supports(handler) == true  │               │
│           │               └─────────────────────────────────┘               │
│           ▼                                                                  │
│  ┌──────────────────┐     ┌─────────────────────────────────┐               │
│  │ applyPreHandle() │────▶│ Interceptor1.preHandle()        │               │
│  └────────┬─────────┘     │ Interceptor2.preHandle()        │               │
│           │               │ 任一返回 false 则终止             │               │
│           │               └─────────────────────────────────┘               │
│           ▼                                                                  │
│  ┌──────────────────┐     ┌─────────────────────────────────┐               │
│  │   ha.handle()    │────▶│ 参数解析（ArgumentResolver）     │               │
│  └────────┬─────────┘     │ 执行 Controller 方法             │               │
│           │               │ 返回值处理（ReturnValueHandler） │               │
│           │               └─────────────────────────────────┘               │
│           ▼                                                                  │
│  ┌──────────────────┐     ┌─────────────────────────────────┐               │
│  │applyPostHandle() │────▶│ Interceptor2.postHandle()       │               │
│  └────────┬─────────┘     │ Interceptor1.postHandle()       │               │
│           │               │ （逆序执行）                      │               │
│           │               └─────────────────────────────────┘               │
│           ▼                                                                  │
│  ┌──────────────────┐     ┌─────────────────────────────────┐               │
│  │processDispatch   │────▶│ 渲染视图（ViewResolver）         │               │
│  │     Result()     │     │ 或写入响应体                     │               │
│  └────────┬─────────┘     └─────────────────────────────────┘               │
│           │                                                                  │
│           ▼                                                                  │
│  ┌──────────────────┐     ┌─────────────────────────────────┐               │
│  │afterCompletion() │────▶│ Interceptor2.afterCompletion()  │               │
│  └────────┬─────────┘     │ Interceptor1.afterCompletion()  │               │
│           │               │ （无论是否异常都执行）            │               │
│           │               └─────────────────────────────────┘               │
│           ▼                                                                  │
│       返回响应                                                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 三、HandlerMapping 匹配过程详解

#### 3.1 getHandler() 源码分析

```java
// DispatcherServlet.getHandler()
@Nullable
protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
    // 遍历所有 HandlerMapping
    if (this.handlerMappings != null) {
        for (HandlerMapping mapping : this.handlerMappings) {
            // 调用每个 HandlerMapping 的 getHandler 方法
            HandlerExecutionChain handler = mapping.getHandler(request);
            if (handler != null) {
                return handler;  // 找到就返回
            }
        }
    }
    return null;  // 没找到，返回 404
}
```

#### 3.2 RequestMappingHandlerMapping 匹配过程

```java
// RequestMappingHandlerMapping.getHandler()
public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
    // 1. 查找 HandlerMethod（Controller 方法）
    Object handler = getHandlerInternal(request);
    if (handler == null) {
        handler = getDefaultHandler();
    }
    if (handler == null) {
        return null;
    }
    
    // 2. 如果是 String，从容器获取 Bean
    if (handler instanceof String) {
        String handlerName = (String) handler;
        handler = obtainApplicationContext().getBean(handlerName);
    }
    
    // 3. 构建 HandlerExecutionChain（包含 Handler 和拦截器）
    HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);
    
    // 4. 处理 CORS
    if (hasCorsConfigurationSource(handler)) {
        CorsConfiguration corsConfig = getCorsConfiguration(handler, request);
        executionChain = getCorsHandlerExecutionChain(request, executionChain, corsConfig);
    }
    
    return executionChain;
}

// 查找 HandlerMethod
protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
    // 1. 获取请求路径
    String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
    
    // 2. 加读锁（保证并发安全）
    this.readWriteLock.readLock().lock();
    try {
        // 3. 从映射注册表查找
        HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
        return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
    }
    finally {
        this.readWriteLock.readLock().unlock();
    }
}
```

#### 3.3 URL 匹配过程

```
请求：GET /users/123

步骤 1：提取请求路径
─────────────────────────────
lookupPath = "/users/123"

步骤 2：遍历所有注册的映射
─────────────────────────────
已注册的映射：
- /users          → UserController.list()
- /users/{id}     → UserController.getById()
- /users/{id}/orders → OrderController.listByUser()

步骤 3：匹配过程
─────────────────────────────
1. AntPathMatcher 匹配路径模式
   - /users/{id} 匹配 /users/123 ✓
   - 提取路径变量：id = 123

2. 选择最佳匹配（BestMatch）
   - 如果多个模式匹配，选择最具体的
   - 比较规则：路径段数、通配符数量

步骤 4：返回 HandlerMethod
─────────────────────────────
HandlerMethod:
- bean: UserController 实例
- method: getById(Long id)
- parameters: [Long id]
```

### 四、HandlerAdapter 调用过程详解

#### 4.1 getHandlerAdapter() 源码

```java
protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
    if (this.handlerAdapters != null) {
        for (HandlerAdapter adapter : this.handlerAdapters) {
            // 检查适配器是否支持当前 Handler
            if (adapter.supports(handler)) {
                return adapter;
            }
        }
    }
    throw new ServletException("No adapter for handler [" + handler + "]");
}
```

#### 4.2 RequestMappingHandlerAdapter.handle() 源码

```java
public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, 
                           Object handler) throws Exception {
    return handleInternal(request, response, (HandlerMethod) handler);
}

protected ModelAndView handleInternal(HttpServletRequest request, 
        HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
    
    // 1. 检查是否支持 Session 访问
    if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
        checkAndPrepare(request, response, this.cacheSecondsForSessionAttributeHandlers);
    } else {
        checkAndPrepare(request, response, true);
    }

    // 2. 是否需要同步执行（保证线程安全）
    if (this.synchronizeOnSession) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object mutex = WebUtils.getSessionMutex(session);
            synchronized (mutex) {
                return invokeHandlerMethod(request, response, handlerMethod);
            }
        }
    }

    // 3. 调用 Handler 方法
    return invokeHandlerMethod(request, response, handlerMethod);
}
```

#### 4.3 invokeHandlerMethod() 核心流程

```java
protected ModelAndView invokeHandlerMethod(HttpServletRequest request,
        HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

    // 1. 创建 ServletWebRequest
    ServletWebRequest webRequest = new ServletWebRequest(request, response);

    try {
        // 2. 创建 WebDataBinderFactory（数据绑定）
        WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
        
        // 3. 创建 ModelFactory（模型管理）
        ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);

        // 4. 创建 ServletInvocableHandlerMethod（可调用的 Handler 方法）
        ServletInvocableHandlerMethod invocableMethod = 
            createInvocableHandlerMethod(handlerMethod);
        
        // 5. 设置参数解析器
        if (this.argumentResolvers != null) {
            invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
        }
        
        // 6. 设置返回值处理器
        if (this.returnValueHandlers != null) {
            invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
        }
        
        // 7. 设置参数名称发现器
        invocableMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
        
        // 8. 设置数据绑定工厂
        invocableMethod.setDataBinderFactory(binderFactory);

        // 9. 创建 ModelAndViewContainer
        ModelAndViewContainer mavContainer = new ModelAndViewContainer();
        mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
        modelFactory.initModel(webRequest, mavContainer, invocableMethod);
        mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);

        // 10. 执行 Controller 方法
        invocableMethod.invokeAndHandle(webRequest, mavContainer);

        // 11. 获取 ModelAndView
        return getModelAndView(mavContainer, modelFactory, webRequest);
    }
    finally {
        webRequest.requestCompleted();
    }
}
```

### 五、参数解析详细流程

#### 5.1 invokeAndHandle() 源码

```java
public void invokeAndHandle(ServletWebRequest webRequest, 
        ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {

    // 1. 执行方法，获取返回值
    Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
    
    // 2. 设置响应状态
    setResponseStatus(webRequest);

    // 3. 处理返回值
    if (returnValue == null) {
        if (isRequestNotModified(webRequest) || getResponseStatus() != null 
            || mavContainer.isRequestHandled()) {
            disableContentCachingIfNecessary(webRequest);
            mavContainer.setRequestHandled(true);
            return;
        }
    }
    else if (StringUtils.hasText(getResponseStatusReason())) {
        mavContainer.setRequestHandled(true);
        return;
    }

    mavContainer.setRequestHandled(false);
    
    // 4. 使用返回值处理器处理返回值
    this.returnValueHandlers.handleReturnValue(
        returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
}
```

#### 5.2 参数解析流程

```java
public Object invokeForRequest(NativeWebRequest request, ModelAndViewContainer mavContainer,
        Object... providedArgs) throws Exception {

    // 1. 获取方法参数值数组
    Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
    
    // 2. 反射调用方法
    return doInvoke(args);
}

protected Object[] getMethodArgumentValues(NativeWebRequest request, 
        ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {

    // 1. 获取方法参数信息
    MethodParameter[] parameters = getMethodParameters();
    Object[] args = new Object[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
        MethodParameter parameter = parameters[i];
        parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
        
        // 2. 检查是否提供了参数值
        args[i] = findProvidedArgument(parameter, providedArgs);
        if (args[i] != null) {
            continue;
        }

        // 3. 检查是否有支持的参数解析器
        if (!this.resolvers.supportsParameter(parameter)) {
            throw new IllegalStateException(
                formatArgumentError(parameter, "No suitable resolver"));
        }
        
        try {
            // 4. 使用解析器解析参数值
            args[i] = this.resolvers.resolveArgument(
                parameter, mavContainer, request, this.dataBinderFactory);
        }
        catch (Exception ex) {
            throw ex;
        }
    }
    return args;
}
```

#### 5.3 参数解析器执行流程

```
Controller 方法：
@GetMapping("/users/{id}")
public User getUser(
    @PathVariable("id") Long id,           // PathVariableMethodArgumentResolver
    @RequestParam("name") String name,     // RequestParamMethodArgumentResolver
    @RequestHeader("Token") String token,  // RequestHeaderMethodArgumentResolver
    @RequestBody UserQuery query,          // RequestResponseBodyMethodProcessor
    HttpSession session                    // SessionObjectMethodArgumentResolver
) { ... }

参数解析过程：
┌─────────────────────────────────────────────────────────────────────┐
│ 参数 1: @PathVariable("id") Long id                                  │
├─────────────────────────────────────────────────────────────────────┤
│ 1. PathVariableMethodArgumentResolver.supportsParameter() → true    │
│ 2. 从 URI 模板变量中获取：uriTemplateVariables.get("id") = "123"     │
│ 3. 类型转换：String "123" → Long 123                                 │
│ 4. 返回：Long.valueOf(123)                                           │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 参数 2: @RequestParam("name") String name                            │
├─────────────────────────────────────────────────────────────────────┤
│ 1. RequestParamMethodArgumentResolver.supportsParameter() → true    │
│ 2. 从查询参数中获取：request.getParameter("name") = "张三"           │
│ 3. 无需类型转换（已经是 String）                                      │
│ 4. 返回："张三"                                                       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ 参数 4: @RequestBody UserQuery query                                 │
├─────────────────────────────────────────────────────────────────────┤
│ 1. RequestResponseBodyMethodProcessor.supportsParameter() → true    │
│ 2. 读取请求体：request.getInputStream()                              │
│ 3. 选择 HttpMessageConverter：                                       │
│    - Content-Type: application/json                                  │
│    - 选择 MappingJackson2HttpMessageConverter                        │
│ 4. 反序列化：JSON → UserQuery 对象                                   │
│ 5. 返回：UserQuery{name="张三", age=18}                              │
└─────────────────────────────────────────────────────────────────────┘
```

### 六、返回值处理详细流程

#### 6.1 返回值处理器执行

```java
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
        ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

    // 1. 选择返回值处理器
    HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
    if (handler == null) {
        throw new IllegalArgumentException("Unknown return value type: " + returnType);
    }
    
    // 2. 处理返回值
    handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
}

private HandlerMethodReturnValueHandler selectHandler(@Nullable Object value, 
        MethodParameter returnType) {
    boolean isAsyncValue = isAsyncReturnValue(value, returnType);
    for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
        if (isAsyncValue && !(handler instanceof AsyncHandlerMethodReturnValueHandler)) {
            continue;
        }
        if (handler.supportsReturnType(returnType)) {
            return handler;
        }
    }
    return null;
}
```

#### 6.2 RequestResponseBodyMethodProcessor 处理流程

```java
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
        ModelAndViewContainer mavContainer, NativeWebRequest webRequest) 
        throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

    // 1. 标记请求已处理
    mavContainer.setRequestHandled(true);

    // 2. 获取原始请求和响应
    ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
    ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

    // 3. 使用消息转换器写入响应
    writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
}

protected <T> void writeWithMessageConverters(@Nullable T value, MethodParameter returnType,
        ServletServerHttpRequest inputMessage, ServletServerHttpResponse outputMessage)
        throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

    // 1. 获取响应体类型
    Type targetType = getGenericType(returnType);
    Class<?> valueType = value.getClass();

    // 2. 获取 Content-Type
    MediaType selectedMediaType = null;
    MediaType contentType = outputMessage.getHeaders().getContentType();
    if (contentType != null && contentType.isConcrete()) {
        selectedMediaType = contentType;
    }
    else {
        // 根据 Accept 头选择
        List<MediaType> acceptableTypes = getAcceptableMediaTypes(request);
        List<MediaType> producibleTypes = getProducibleMediaTypes(request, valueType, targetType);
        // 协商最佳 MediaType
        selectedMediaType = negotiateMediaType(acceptableTypes, producibleTypes);
    }

    // 3. 选择合适的消息转换器
    if (selectedMediaType != null) {
        for (HttpMessageConverter<?> converter : this.messageConverters) {
            GenericHttpMessageConverter genericConverter = 
                (converter instanceof GenericHttpMessageConverter ? 
                 (GenericHttpMessageConverter<?>) converter : null);
            
            if (genericConverter != null) {
                if (genericConverter.canWrite(targetType, valueType, selectedMediaType)) {
                    // 4. 执行转换并写入响应
                    genericConverter.write(value, targetType, selectedMediaType, outputMessage);
                    return;
                }
            }
            else if (converter.canWrite(valueType, selectedMediaType)) {
                ((HttpMessageConverter) converter).write(value, selectedMediaType, outputMessage);
                return;
            }
        }
    }
}
```

#### 6.3 返回值处理流程图

```
Controller 返回值：User 对象
           │
           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    选择返回值处理器                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  遍历所有 ReturnValueHandler：                                        │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ ViewNameMethodReturnValueHandler                            │   │
│  │ supportsReturnType: 返回值是 String？                        │   │
│  │ User 不是 String → false                                    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ ModelAndViewMethodReturnValueHandler                        │   │
│  │ supportsReturnType: 返回值是 ModelAndView？                  │   │
│  │ User 不是 ModelAndView → false                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ RequestResponseBodyMethodProcessor                          │   │
│  │ supportsReturnType: 方法有 @ResponseBody 注解？              │   │
│  │ 或者类有 @RestController 注解？                               │   │
│  │ → true ✓                                                    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────────┐
│              RequestResponseBodyMethodProcessor.handleReturnValue()  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 标记请求已处理                                                    │
│     mavContainer.setRequestHandled(true)                             │
│                                                                      │
│  2. 获取 Content-Type                                                │
│     Accept: application/json → selectedMediaType = application/json  │
│                                                                      │
│  3. 选择消息转换器                                                    │
│     ┌─────────────────────────────────────────────────────────────┐ │
│     │ MappingJackson2HttpMessageConverter                         │ │
│     │ canWrite(User.class, application/json) → true ✓            │ │
│     └─────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  4. 序列化并写入响应                                                  │
│     User{id=1, name="张三"}                                          │
│           ↓                                                          │
│     ObjectMapper.writeValueAsBytes()                                 │
│           ↓                                                          │
│     {"id":1,"name":"张三"}                                           │
│           ↓                                                          │
│     response.getOutputStream().write(bytes)                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
           │
           ▼
      HTTP 响应返回
```

### 七、视图渲染流程（传统 MVC 模式）

#### 7.1 processDispatchResult() 源码

```java
private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
        @Nullable HandlerExecutionChain mappedHandler, @Nullable ModelAndView mv,
        @Nullable Exception exception) throws Exception {

    boolean errorView = false;

    // 1. 处理异常
    if (exception != null) {
        if (exception instanceof ModelAndViewDefiningException) {
            mv = ((ModelAndViewDefiningException) exception).getModelAndView();
        }
        else {
            Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
            mv = processHandlerException(request, response, handler, exception);
            errorView = (mv != null);
        }
    }

    // 2. 渲染视图
    if (mv != null && !mv.wasCleared()) {
        render(mv, request, response);
        if (errorView) {
            WebUtils.clearErrorRequestAttributes(request);
        }
    }

    // 3. 执行拦截器 afterCompletion
    if (mappedHandler != null) {
        mappedHandler.triggerAfterCompletion(request, response, null);
    }
}
```

#### 7.2 render() 视图渲染

```java
protected void render(ModelAndView mv, HttpServletRequest request, 
        HttpServletResponse response) throws Exception {

    // 1. 确定 Locale
    Locale locale = (this.localeResolver != null ? 
        this.localeResolver.resolveLocale(request) : request.getLocale());
    response.setLocale(locale);

    View view;
    String viewName = mv.getViewName();
    
    // 2. 解析视图
    if (viewName != null) {
        // 使用 ViewResolver 解析视图名称
        view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
        if (view == null) {
            throw new ServletException("Could not resolve view with name '" + mv.getViewName() + "'");
        }
    }
    else {
        // 直接使用 View 对象
        view = mv.getView();
        if (view == null) {
            throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a View object");
        }
    }

    // 3. 渲染视图
    view.render(mv.getModelInternal(), request, response);
}
```

#### 7.3 视图渲染流程图

```
Controller 返回：return "user/detail";

           │
           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      ModelAndView                                    │
│  viewName = "user/detail"                                            │
│  model = {user: User{id=1, name="张三"}}                             │
└─────────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    ViewResolver 解析视图                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  InternalResourceViewResolver:                                       │
│  - prefix = "/WEB-INF/views/"                                        │
│  - suffix = ".jsp"                                                   │
│                                                                      │
│  解析结果：                                                           │
│  prefix + viewName + suffix                                          │
│  = "/WEB-INF/views/" + "user/detail" + ".jsp"                        │
│  = "/WEB-INF/views/user/detail.jsp"                                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      View.render()                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 合并 Model 数据到请求属性                                          │
│     request.setAttribute("user", user)                               │
│                                                                      │
│  2. 转发到 JSP                                                        │
│     RequestDispatcher dispatcher = request.getRequestDispatcher(url) │
│     dispatcher.forward(request, response)                            │
│                                                                      │
│  3. JSP 渲染                                                          │
│     <html>                                                           │
│       <body>                                                         │
│         <h1>${user.name}</h1>  →  张三                                │
│       </body>                                                        │
│     </html>                                                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
           │
           ▼
      HTTP 响应返回
```

### 八、响应返回流程

#### 8.1 响应构建过程

```
┌─────────────────────────────────────────────────────────────────────┐
│                        响应构建流程                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 设置响应状态码                                                    │
│     response.setStatus(200)                                          │
│                                                                      │
│  2. 设置响应头                                                        │
│     Content-Type: application/json; charset=UTF-8                    │
│     Content-Length: 28                                               │
│     Cache-Control: no-cache                                          │
│                                                                      │
│  3. 写入响应体                                                        │
│     {"id":1,"name":"张三","age":18}                                   │
│                                                                      │
│  4. 刷新缓冲区                                                        │
│     response.flushBuffer()                                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Tomcat 处理响应                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. CoyoteAdapter.afterService()                                     │
│     - 提交响应                                                        │
│     - 更新统计信息                                                    │
│                                                                      │
│  2. Http11Processor.process()                                        │
│     - 构建 HTTP 响应报文                                              │
│     - HTTP/1.1 200 OK                                                │
│       Content-Type: application/json                                 │
│       Content-Length: 28                                             │
│                                                                      │
│       {"id":1,"name":"张三","age":18}                                 │
│                                                                      │
│  3. NioEndpoint 发送数据                                              │
│     - 写入 Socket 缓冲区                                              │
│     - TCP 发送给客户端                                                │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       浏览器接收响应                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 解析 HTTP 响应                                                    │
│  2. 解析 JSON 数据                                                    │
│  3. 渲染页面或执行回调                                                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 九、完整请求时序图

```
┌───────┐    ┌────────┐    ┌────────────────┐    ┌─────────────────┐
│Browser│    │ Tomcat │    │DispatcherServlet│   │HandlerMapping   │
└───┬───┘    └───┬────┘    └───────┬────────┘    └────────┬────────┘
    │            │                 │                      │
    │ HTTP Request│                │                      │
    │───────────▶│                 │                      │
    │            │ service()       │                      │
    │            │────────────────▶│                      │
    │            │                 │                      │
    │            │                 │ getHandler()         │
    │            │                 │─────────────────────▶│
    │            │                 │                      │
    │            │                 │ HandlerExecutionChain│
    │            │                 │◀─────────────────────│
    │            │                 │                      │
    │            │                 │                      │
    │            │    ┌────────────┴────────────┐        │
    │            │    │                         │        │
    │            │    ▼                         ▼        │
    │            │ HandlerAdapter          Interceptor  │
    │            │    │                         │        │
    │            │    │ preHandle()             │        │
    │            │    │◀────────────────────────│        │
    │            │    │                         │        │
    │            │    │ handle()                │        │
    │            │    │                         │        │
    │            │    │  ┌─────────────────────┐│        │
    │            │    │  │ArgumentResolver     ││        │
    │            │    │  │解析参数              ││        │
    │            │    │  └──────────┬──────────┘│        │
    │            │    │             │           │        │
    │            │    │  ┌──────────▼──────────┐│        │
    │            │    │  │Controller.method()  ││        │
    │            │    │  │执行业务逻辑          ││        │
    │            │    │  └──────────┬──────────┘│        │
    │            │    │             │           │        │
    │            │    │  ┌──────────▼──────────┐│        │
    │            │    │  │ReturnValueHandler   ││        │
    │            │    │  │处理返回值            ││        │
    │            │    │  └──────────┬──────────┘│        │
    │            │    │             │           │        │
    │            │    │ postHandle()            │        │
    │            │    │◀────────────────────────│        │
    │            │    │                         │        │
    │            │◀───┤ ModelAndView            │        │
    │            │    │                         │        │
    │            │    │ render()                │        │
    │            │    │                         │        │
    │            │    │ afterCompletion()       │        │
    │            │    │◀────────────────────────│        │
    │            │    │                         │        │
    │            │◀───┤ Response                │        │
    │            │    │                         │        │
    │ HTTP Response│   │                         │        │
    │◀───────────│    │                         │        │
    │            │    │                         │        │
```

### 十、关键断点调试位置

```java
// 1. 请求入口
DispatcherServlet.doDispatch()              // 行 1057

// 2. Handler 查找
DispatcherServlet.getHandler()              // 行 1233
RequestMappingHandlerMapping.getHandler()   // 行 397
AbstractHandlerMethodMapping.lookupHandlerMethod()  // 行 418

// 3. 拦截器执行
HandlerExecutionChain.applyPreHandle()     // 行 148
HandlerExecutionChain.applyPostHandle()    // 行 177

// 4. Handler 调用
RequestMappingHandlerAdapter.handleInternal()  // 行 697
ServletInvocableHandlerMethod.invokeAndHandle()  // 行 116

// 5. 参数解析
InvocableHandlerMethod.getMethodArgumentValues()  // 行 167
HandlerMethodArgumentResolverComposite.resolveArgument()  // 行 121

// 6. 返回值处理
HandlerMethodReturnValueHandlerComposite.handleReturnValue()  // 行 94
RequestResponseBodyMethodProcessor.handleReturnValue()  // 行 275

// 7. 视图渲染
DispatcherServlet.render()                 // 行 1373
View.render()                              // 行 302

// 8. 异常处理
DispatcherServlet.processHandlerException()  // 行 1277
ExceptionHandlerExceptionResolver.doResolveHandlerMethodException()  // 行 392
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

| 注解                  | 作用                          |
|---------------------|-----------------------------|
| `@Controller`       | 标记控制器类                      |
| `@RestController`   | @Controller + @ResponseBody |
| `@RequestMapping`   | 映射请求路径                      |
| `@GetMapping`       | GET 请求映射                    |
| `@PostMapping`      | POST 请求映射                   |
| `@PutMapping`       | PUT 请求映射                    |
| `@DeleteMapping`    | DELETE 请求映射                 |
| `@PathVariable`     | 路径变量                        |
| `@RequestParam`     | 请求参数                        |
| `@RequestBody`      | 请求体（JSON）                   |
| `@ResponseBody`     | 响应体（JSON）                   |
| `@ModelAttribute`   | 模型属性                        |
| `@InitBinder`       | 初始化数据绑定器                    |
| `@ExceptionHandler` | 异常处理                        |
| `@ControllerAdvice` | 全局控制器增强                     |

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

| 特性   | HandlerMapping        | HandlerAdapter |
|------|-----------------------|----------------|
| 作用   | 找到处理器                 | 调用处理器          |
| 输入   | HTTP 请求               | Handler 对象     |
| 输出   | HandlerExecutionChain | ModelAndView   |
| 核心方法 | getHandler()          | handle()       |

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

| 特性   | 过滤器（Filter）                       | 拦截器（Interceptor）     |
|------|-----------------------------------|----------------------|
| 所属规范 | Servlet 规范                        | Spring MVC 提供        |
| 运行容器 | Servlet 容器                        | Spring 容器            |
| 触发时机 | 请求到达时最早执行                         | DispatcherServlet 之后 |
| 作用范围 | 所有请求（包括静态资源）                      | 仅 Controller 请求      |
| 访问权限 | 无法访问 Spring Bean                  | 可以访问 Spring Bean     |
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

| 特性       | @Controller                | @RestController             |
|----------|----------------------------|-----------------------------|
| 组成       | 基础注解                       | @Controller + @ResponseBody |
| 返回值      | ModelAndView 或 String（视图名） | 直接返回数据（JSON/XML）            |
| 适用场景     | 模板引擎（JSP、Thymeleaf）        | RESTful API                 |
| 是否需要视图解析 | 是                          | 否                           |

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
