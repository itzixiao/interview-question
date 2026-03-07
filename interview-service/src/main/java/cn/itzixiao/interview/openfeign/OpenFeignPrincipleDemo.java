package cn.itzixiao.interview.openfeign;

/**
 * OpenFeign 核心原理详解 - 教学型示例
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                           OpenFeign 整体架构图                                       │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │                                                                                      │
 * │   ┌──────────────┐     ┌──────────────────┐     ┌─────────────────┐                │
 * │   │  @FeignClient │────▶│  JDK动态代理      │────▶│  ReflectiveFeign │               │
 * │   │  接口定义      │     │  生成代理对象     │     │  方法处理器      │                │
 * │   └──────────────┘     └──────────────────┘     └────────┬────────┘                │
 * │                                                          │                          │
 * │                              ┌───────────────────────────┴───────────────────┐      │
 * │                              ▼                                               ▼      │
 * │                 ┌────────────────────────┐                    ┌────────────────────┐│
 * │                 │    SynchronousMethod   │                    │   RequestTemplate  ││
 * │                 │    Handler             │───────────────────▶│   请求模板构建     ││
 * │                 └────────────────────────┘                    └─────────┬──────────┘│
 * │                                                                         │           │
 * │                 ┌───────────────────────────────────────────────────────┘           │
 * │                 ▼                                                                    │
 * │   ┌─────────────────────────────────────────────────────────────────────────────┐  │
 * │   │                           核心组件处理链                                      │  │
 * │   │  ┌──────────┐  ┌──────────┐  ┌───────────────┐  ┌──────────────────────────┐ │  │
 * │   │  │ Contract │─▶│ Encoder  │─▶│ Interceptors  │─▶│ Client (HTTP客户端)       │ │  │
 * │   │  │ 契约解析  │  │ 请求编码  │  │ 请求拦截器    │  │ ├─ ApacheHttpClient      │ │  │
 * │   │  └──────────┘  └──────────┘  └───────────────┘  │ ├─ OkHttpClient          │ │  │
 * │   │                                                  │ └─ LoadBalancerClient   │ │  │
 * │   │                                                  └──────────────────────────┘ │  │
 * │   └─────────────────────────────────────────────────────────────────────────────┘  │
 * │                                                                         │           │
 * │                 ┌───────────────────────────────────────────────────────┘           │
 * │                 ▼                                                                    │
 * │   ┌─────────────────────────────────────────────────────────────────────────────┐  │
 * │   │                           响应处理链                                          │  │
 * │   │  ┌──────────────────┐  ┌──────────────┐  ┌────────────────────────────────┐ │  │
 * │   │  │  HTTP Response   │─▶│   Decoder    │─▶│  返回结果给调用方               │ │  │
 * │   │  │  HTTP响应         │  │  响应解码     │  │  (方法返回值)                  │ │  │
 * │   │  └──────────────────┘  └──────────────┘  └────────────────────────────────┘ │  │
 * │   └─────────────────────────────────────────────────────────────────────────────┘  │
 * │                                                                                      │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>一、OpenFeign 是什么？</h2>
 * <pre>
 * OpenFeign 是一个声明式的 HTTP 客户端，它使得编写 HTTP 客户端变得更加简单。
 * 开发者只需要定义一个接口并添加注解，OpenFeign 就会自动生成实现类来完成 HTTP 请求。
 *
 * 核心特点：
 * 1. 声明式定义：通过接口 + 注解的方式定义远程调用
 * 2. 集成负载均衡：与 Spring Cloud LoadBalancer 无缝集成
 * 3. 可插拔设计：支持自定义编码器、解码器、日志、拦截器等
 * 4. 熔断降级：支持与 Sentinel/Resilience4j 集成
 * </pre>
 *
 * <h2>二、OpenFeign vs RestTemplate vs WebClient</h2>
 * <pre>
 * ┌───────────────────┬────────────────────────┬────────────────────────┬────────────────────────┐
 * │       特性        │      RestTemplate      │       OpenFeign        │       WebClient        │
 * ├───────────────────┼────────────────────────┼────────────────────────┼────────────────────────┤
 * │    编程方式       │       命令式           │        声明式          │      响应式            │
 * │    代码量         │       较多             │        最少            │      中等              │
 * │    可读性         │       一般             │        最好            │      一般              │
 * │    负载均衡       │   需手动集成           │     自动集成           │   需手动集成           │
 * │    熔断集成       │   需手动集成           │     方便集成           │   需手动集成           │
 * │    性能           │       一般             │        一般            │      最好              │
 * │    维护状态       │   Spring 5.0后弃用     │     活跃维护           │   推荐使用             │
 * └───────────────────┴────────────────────────┴────────────────────────┴────────────────────────┘
 * </pre>
 *
 * @author itzixiao
 * @since 1.0
 */
public class OpenFeignPrincipleDemo {

    // ==================== 第一部分：OpenFeign 核心组件详解 ====================

    /**
     * <h2>1. Contract - 契约解析器</h2>
     * <pre>
     * Contract 负责解析 Feign 接口上的注解，将其转换为 MethodMetadata。
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                        Contract 解析流程                                │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │                                                                          │
     * │   @FeignClient(name = "user-service")                                   │
     * │   public interface UserClient {                                         │
     * │       @GetMapping("/users/{id}")                                        │
     * │       User getById(@PathVariable("id") Long id);                        │
     * │   }                                                                      │
     * │                         │                                               │
     * │                         ▼                                               │
     * │   ┌─────────────────────────────────────────────────────────────────┐  │
     * │   │  SpringMvcContract.parseAndValidateMetadata()                   │  │
     * │   │  解析 Spring MVC 注解 (@GetMapping, @PostMapping, 等)           │  │
     * │   └─────────────────────────────────────────────────────────────────┘  │
     * │                         │                                               │
     * │                         ▼                                               │
     * │   ┌─────────────────────────────────────────────────────────────────┐  │
     * │   │  MethodMetadata:                                                │  │
     * │   │  - template: GET /users/{id}                                    │  │
     * │   │  - returnType: User.class                                       │  │
     * │   │  - parameterIndexToName: {0: "id"}                              │  │
     * │   └─────────────────────────────────────────────────────────────────┘  │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * Contract 实现类：
     * - feign.Contract.Default: 原生 Feign 注解支持
     * - SpringMvcContract: Spring MVC 注解支持 (默认)
     * - JAXRSContract: JAX-RS 注解支持
     * </pre>
     */
    public void contractDemo() {
        /*
         * SpringMvcContract 源码核心逻辑：
         *
         * public class SpringMvcContract extends Contract.BaseContract {
         *
         *     @Override
         *     protected void processAnnotationOnClass(MethodMetadata data, Class<?> clz) {
         *         // 解析类级别的 @RequestMapping
         *         RequestMapping classAnnotation = findMergedAnnotation(clz, RequestMapping.class);
         *         if (classAnnotation != null) {
         *             // 设置类级别的路径前缀
         *             data.template().uri(classAnnotation.value()[0]);
         *         }
         *     }
         *
         *     @Override
         *     protected void processAnnotationOnMethod(MethodMetadata data, Annotation annotation, Method method) {
         *         // 解析方法级别的 @GetMapping, @PostMapping 等
         *         if (annotation instanceof GetMapping) {
         *             // 设置 HTTP 方法为 GET
         *             data.template().method(HttpMethod.GET);
         *             // 设置请求路径
         *             data.template().uri(((GetMapping) annotation).value()[0]);
         *         }
         *         // ... 其他 HTTP 方法处理
         *     }
         *
         *     @Override
         *     protected boolean processAnnotationsOnParameter(MethodMetadata data, Annotation[] annotations, int paramIndex) {
         *         // 解析参数注解: @PathVariable, @RequestParam, @RequestBody, @RequestHeader
         *         for (Annotation annotation : annotations) {
         *             if (annotation instanceof PathVariable) {
         *                 // 记录路径变量
         *                 String name = ((PathVariable) annotation).value();
         *                 data.indexToName().put(paramIndex, Collections.singleton(name));
         *             } else if (annotation instanceof RequestParam) {
         *                 // 记录请求参数
         *                 // ...
         *             } else if (annotation instanceof RequestBody) {
         *                 // 标记请求体参数
         *                 data.bodyIndex(paramIndex);
         *             }
         *         }
         *         return true;
         *     }
         * }
         */
    }

    /**
     * <h2>2. Encoder - 请求编码器</h2>
     * <pre>
     * Encoder 负责将方法参数编码为 HTTP 请求体。
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                        Encoder 编码流程                                 │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │                                                                          │
     * │   Java 对象 (User)          Encoder.encode()          HTTP 请求体       │
     * │   ┌─────────────┐    ────────────────────────▶    ┌─────────────────┐  │
     * │   │ id: 1       │                                  │ {               │  │
     * │   │ name: "Tom" │    SpringEncoder                │   "id": 1,      │  │
     * │   │ age: 20     │    (Jackson/Gson)               │   "name": "Tom",│  │
     * │   └─────────────┘                                  │   "age": 20     │  │
     * │                                                    │ }               │  │
     * │                                                    └─────────────────┘  │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * 常用 Encoder 实现：
     * - SpringEncoder: 使用 Spring 的 HttpMessageConverter (默认)
     * - JacksonEncoder: 使用 Jackson 进行 JSON 序列化
     * - GsonEncoder: 使用 Gson 进行 JSON 序列化
     * - FormEncoder: 表单数据编码
     * </pre>
     */
    public void encoderDemo() {
        /*
         * SpringEncoder 源码核心逻辑：
         *
         * public class SpringEncoder implements Encoder {
         *
         *     private final ObjectFactory<HttpMessageConverters> messageConverters;
         *
         *     @Override
         *     public void encode(Object object, Type bodyType, RequestTemplate template) {
         *         if (object == null) {
         *             return;
         *         }
         *
         *         // 获取 Content-Type
         *         MediaType requestContentType = getContentType(template);
         *
         *         // 遍历所有的 HttpMessageConverter，找到能处理该类型的转换器
         *         for (HttpMessageConverter<?> messageConverter : messageConverters.getObject().getConverters()) {
         *             if (messageConverter.canWrite(object.getClass(), requestContentType)) {
         *
         *                 // 使用 ByteArrayOutputStream 捕获序列化结果
         *                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         *                 HttpOutputMessage outputMessage = new HttpOutputMessageWrapper(outputStream);
         *
         *                 // 执行序列化
         *                 ((HttpMessageConverter<Object>) messageConverter)
         *                     .write(object, requestContentType, outputMessage);
         *
         *                 // 将序列化结果设置到请求模板
         *                 template.body(outputStream.toByteArray(), StandardCharsets.UTF_8);
         *                 return;
         *             }
         *         }
         *
         *         throw new EncodeException("Could not encode body: " + object);
         *     }
         * }
         */
    }

    /**
     * <h2>3. Decoder - 响应解码器</h2>
     * <pre>
     * Decoder 负责将 HTTP 响应体解码为 Java 对象。
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                        Decoder 解码流程                                 │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │                                                                          │
     * │   HTTP 响应体              Decoder.decode()           Java 对象         │
     * │   ┌─────────────────┐    ────────────────────▶    ┌─────────────┐       │
     * │   │ {               │                              │ id: 1       │       │
     * │   │   "id": 1,      │    ResponseEntityDecoder    │ name: "Tom" │       │
     * │   │   "name": "Tom",│    (OptionalDecoder)        │ age: 20     │       │
     * │   │   "age": 20     │    (SpringDecoder)          └─────────────┘       │
     * │   │ }               │                                                    │
     * │   └─────────────────┘                                                    │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * 解码器装饰链：
     * ResponseEntityDecoder → OptionalDecoder → SpringDecoder
     *
     * ResponseEntityDecoder: 处理 ResponseEntity 包装类型
     * OptionalDecoder: 处理 Optional 包装类型
     * SpringDecoder: 使用 HttpMessageConverter 进行反序列化
     * </pre>
     */
    public void decoderDemo() {
        /*
         * SpringDecoder 源码核心逻辑：
         *
         * public class SpringDecoder implements Decoder {
         *
         *     @Override
         *     public Object decode(Response response, Type type) throws IOException {
         *         if (response.status() == 404 || response.status() == 204) {
         *             return null;  // 404/204 返回 null
         *         }
         *
         *         // 获取响应的 Content-Type
         *         MediaType contentType = getContentType(response);
         *
         *         // 遍历所有的 HttpMessageConverter
         *         for (HttpMessageConverter<?> messageConverter : messageConverters.getObject().getConverters()) {
         *             if (messageConverter.canRead(toClass(type), contentType)) {
         *
         *                 // 构建 HttpInputMessage
         *                 HttpInputMessage inputMessage = new HttpInputMessageWrapper(response);
         *
         *                 // 执行反序列化
         *                 return messageConverter.read(toClass(type), inputMessage);
         *             }
         *         }
         *
         *         throw new DecodeException(response.status(), "Could not decode response");
         *     }
         * }
         */
    }

    /**
     * <h2>4. Client - HTTP 客户端</h2>
     * <pre>
     * Client 负责发送 HTTP 请求并接收响应，是 Feign 的网络层。
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                        Client 层次结构                                   │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │                                                                          │
     * │                        ┌─────────────────┐                               │
     * │                        │  feign.Client   │  (接口)                       │
     * │                        └────────┬────────┘                               │
     * │                                 │                                        │
     * │       ┌───────────────┬─────────┼─────────┬────────────────┐            │
     * │       ▼               ▼         ▼         ▼                ▼            │
     * │  ┌─────────┐   ┌───────────┐ ┌─────────┐ ┌─────────────┐ ┌─────────────┐│
     * │  │ Default │   │ApacheHttp │ │ OkHttp  │ │ LoadBalancer│ │ Fallback    ││
     * │  │ (JDK)   │   │ Client    │ │ Client  │ │ Client      │ │ Factory     ││
     * │  └─────────┘   └───────────┘ └─────────┘ └─────────────┘ └─────────────┘│
     * │       │               │           │             │                        │
     * │       ▼               ▼           ▼             ▼                        │
     * │  HttpURLConn     Apache       OkHttp3     服务发现         熔断降级       │
     * │  ection          HttpClient               负载均衡                       │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * Client 选择建议：
     * ┌────────────────────┬────────────────────────────────────────────────────┐
     * │      Client        │                    适用场景                         │
     * ├────────────────────┼────────────────────────────────────────────────────┤
     * │  Default (JDK)     │  简单场景，无连接池                                 │
     * │  ApacheHttpClient  │  需要连接池、更好的性能                             │
     * │  OkHttpClient      │  现代 HTTP 客户端，支持 HTTP/2                      │
     * │  LoadBalancer      │  微服务场景，需要负载均衡（默认）                   │
     * └────────────────────┴────────────────────────────────────────────────────┘
     * </pre>
     */
    public void clientDemo() {
        /*
         * FeignBlockingLoadBalancerClient 源码核心逻辑（负载均衡客户端）：
         *
         * public class FeignBlockingLoadBalancerClient implements Client {
         *
         *     private final Client delegate;  // 实际的 HTTP 客户端
         *     private final LoadBalancerClient loadBalancerClient;  // 负载均衡器
         *
         *     @Override
         *     public Response execute(Request request, Request.Options options) throws IOException {
         *
         *         // 1. 从请求 URL 中提取服务名
         *         URI originalUri = URI.create(request.url());
         *         String serviceId = originalUri.getHost();  // 如: user-service
         *
         *         // 2. 通过 LoadBalancer 选择服务实例
         *         ServiceInstance instance = loadBalancerClient.choose(serviceId);
         *         // 假设选中: 192.168.1.100:8080
         *
         *         if (instance == null) {
         *             throw new IllegalStateException("No instances available for " + serviceId);
         *         }
         *
         *         // 3. 重构请求 URL (将服务名替换为实际地址)
         *         // http://user-service/users/1 → http://192.168.1.100:8080/users/1
         *         String reconstructedUrl = loadBalancerClient.reconstructURI(instance, originalUri).toString();
         *
         *         // 4. 创建新的请求
         *         Request newRequest = Request.create(
         *             request.httpMethod(),
         *             reconstructedUrl,
         *             request.headers(),
         *             request.body(),
         *             request.charset()
         *         );
         *
         *         // 5. 委托给实际的 HTTP 客户端执行请求
         *         return delegate.execute(newRequest, options);
         *     }
         * }
         */
    }

    // ==================== 第二部分：OpenFeign 动态代理机制 ====================

    /**
     * <h2>动态代理核心原理</h2>
     * <pre>
     * OpenFeign 使用 JDK 动态代理为 @FeignClient 接口生成代理对象。
     *
     * ┌─────────────────────────────────────────────────────────────────────────────────┐
     * │                        动态代理生成流程                                          │
     * ├─────────────────────────────────────────────────────────────────────────────────┤
     * │                                                                                  │
     * │   1. 应用启动                                                                    │
     * │      │                                                                           │
     * │      ▼                                                                           │
     * │   ┌──────────────────────────────────────────────────────────────────────────┐  │
     * │   │  @EnableFeignClients                                                     │  │
     * │   │  触发 FeignClientsRegistrar.registerBeanDefinitions()                    │  │
     * │   └────────────────────────────────────┬─────────────────────────────────────┘  │
     * │                                        │                                         │
     * │      ┌─────────────────────────────────┘                                         │
     * │      ▼                                                                           │
     * │   2. 扫描 @FeignClient 接口                                                      │
     * │      │                                                                           │
     * │      ▼                                                                           │
     * │   ┌──────────────────────────────────────────────────────────────────────────┐  │
     * │   │  ClassPathScanningCandidateComponentProvider                             │  │
     * │   │  扫描所有带 @FeignClient 注解的接口                                       │  │
     * │   │  如: UserClient, OrderClient, ProductClient                              │  │
     * │   └────────────────────────────────────┬─────────────────────────────────────┘  │
     * │                                        │                                         │
     * │      ┌─────────────────────────────────┘                                         │
     * │      ▼                                                                           │
     * │   3. 注册 FeignClientFactoryBean                                                │
     * │      │                                                                           │
     * │      ▼                                                                           │
     * │   ┌──────────────────────────────────────────────────────────────────────────┐  │
     * │   │  为每个 @FeignClient 接口注册一个 FeignClientFactoryBean                  │  │
     * │   │  BeanDefinition:                                                          │  │
     * │   │  - beanClass: FeignClientFactoryBean                                      │  │
     * │   │  - type: UserClient.class                                                 │  │
     * │   │  - name: "user-service"                                                   │  │
     * │   │  - url: ""                                                                │  │
     * │   └────────────────────────────────────┬─────────────────────────────────────┘  │
     * │                                        │                                         │
     * │      ┌─────────────────────────────────┘                                         │
     * │      ▼                                                                           │
     * │   4. 创建代理对象（懒加载，首次使用时）                                          │
     * │      │                                                                           │
     * │      ▼                                                                           │
     * │   ┌──────────────────────────────────────────────────────────────────────────┐  │
     * │   │  FeignClientFactoryBean.getObject()                                       │  │
     * │   │      │                                                                     │  │
     * │   │      ▼                                                                     │  │
     * │   │  Feign.Builder                                                            │  │
     * │   │      .encoder(encoder)                                                    │  │
     * │   │      .decoder(decoder)                                                    │  │
     * │   │      .contract(contract)                                                  │  │
     * │   │      .client(client)                                                      │  │
     * │   │      .target(UserClient.class, "http://user-service");                    │  │
     * │   │      │                                                                     │  │
     * │   │      ▼                                                                     │  │
     * │   │  ReflectiveFeign.newInstance()                                            │  │
     * │   │      │                                                                     │  │
     * │   │      ▼                                                                     │  │
     * │   │  ┌────────────────────────────────────────────────────────────────────┐   │  │
     * │   │  │  JDK Proxy.newProxyInstance(                                       │   │  │
     * │   │  │      classLoader,                                                  │   │  │
     * │   │  │      new Class<?>[] { UserClient.class },                          │   │  │
     * │   │  │      new FeignInvocationHandler(target, dispatch)                  │   │  │
     * │   │  │  )                                                                 │   │  │
     * │   │  │                                                                    │   │  │
     * │   │  │  dispatch = Map<Method, MethodHandler>                             │   │  │
     * │   │  │  每个接口方法对应一个 SynchronousMethodHandler                     │   │  │
     * │   │  └────────────────────────────────────────────────────────────────────┘   │  │
     * │   └──────────────────────────────────────────────────────────────────────────┘  │
     * │                                                                                  │
     * └─────────────────────────────────────────────────────────────────────────────────┘
     * </pre>
     */
    public void dynamicProxyDemo() {
        /*
         * FeignClientsRegistrar 源码核心逻辑：
         *
         * class FeignClientsRegistrar implements ImportBeanDefinitionRegistrar {
         *
         *     @Override
         *     public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
         *
         *         // 1. 注册默认配置
         *         registerDefaultConfiguration(metadata, registry);
         *
         *         // 2. 扫描并注册 FeignClient
         *         registerFeignClients(metadata, registry);
         *     }
         *
         *     private void registerFeignClients(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
         *         // 获取 @EnableFeignClients 的属性
         *         Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableFeignClients.class.getName());
         *
         *         // 创建扫描器
         *         ClassPathScanningCandidateComponentProvider scanner = getScanner();
         *         scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class));
         *
         *         // 获取扫描包路径
         *         Set<String> basePackages = getBasePackages(metadata);
         *
         *         // 扫描并注册每个 FeignClient
         *         for (String basePackage : basePackages) {
         *             Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
         *             for (BeanDefinition candidateComponent : candidateComponents) {
         *                 // 注册 FeignClientFactoryBean
         *                 registerFeignClient(registry, annotationMetadata, attributes);
         *             }
         *         }
         *     }
         *
         *     private void registerFeignClient(BeanDefinitionRegistry registry, ...) {
         *         BeanDefinitionBuilder definition = BeanDefinitionBuilder
         *             .genericBeanDefinition(FeignClientFactoryBean.class);
         *
         *         // 设置属性
         *         definition.addPropertyValue("type", className);
         *         definition.addPropertyValue("name", name);
         *         definition.addPropertyValue("url", url);
         *         definition.addPropertyValue("path", path);
         *         definition.addPropertyValue("fallback", fallback);
         *         definition.addPropertyValue("fallbackFactory", fallbackFactory);
         *
         *         // 注册 BeanDefinition
         *         registry.registerBeanDefinition(name, definition.getBeanDefinition());
         *     }
         * }
         */
    }

    /**
     * <h2>方法调用执行流程</h2>
     * <pre>
     * 当调用 Feign 接口方法时，实际执行的是 FeignInvocationHandler.invoke()
     *
     * ┌─────────────────────────────────────────────────────────────────────────────────┐
     * │                        方法调用执行流程                                          │
     * ├─────────────────────────────────────────────────────────────────────────────────┤
     * │                                                                                  │
     * │   userClient.getById(1L);                                                       │
     * │         │                                                                        │
     * │         ▼                                                                        │
     * │   ┌──────────────────────────────────────────────────────────────────────────┐  │
     * │   │  FeignInvocationHandler.invoke(proxy, method, args)                       │  │
     * │   │      │                                                                     │  │
     * │   │      ▼                                                                     │  │
     * │   │  // 从 dispatch 映射中获取对应的 MethodHandler                            │  │
     * │   │  MethodHandler handler = dispatch.get(method);                            │  │
     * │   │  return handler.invoke(args);                                             │  │
     * │   └────────────────────────────────────┬─────────────────────────────────────┘  │
     * │                                        │                                         │
     * │                                        ▼                                         │
     * │   ┌──────────────────────────────────────────────────────────────────────────┐  │
     * │   │  SynchronousMethodHandler.invoke(args)                                    │  │
     * │   │      │                                                                     │  │
     * │   │      ▼                                                                     │  │
     * │   │  // 1. 构建 RequestTemplate                                               │  │
     * │   │  RequestTemplate template = buildTemplateFromArgs.create(args);           │  │
     * │   │  // template: GET http://user-service/users/1                             │  │
     * │   │      │                                                                     │  │
     * │   │      ▼                                                                     │  │
     * │   │  // 2. 应用请求拦截器                                                      │  │
     * │   │  for (RequestInterceptor interceptor : requestInterceptors) {             │  │
     * │   │      interceptor.apply(template);                                         │  │
     * │   │  }                                                                         │  │
     * │   │      │                                                                     │  │
     * │   │      ▼                                                                     │  │
     * │   │  // 3. 执行 HTTP 请求（通过 Client）                                       │  │
     * │   │  Response response = client.execute(request, options);                    │  │
     * │   │      │                                                                     │  │
     * │   │      ▼                                                                     │  │
     * │   │  // 4. 解码响应                                                            │  │
     * │   │  return decoder.decode(response, metadata.returnType());                  │  │
     * │   └──────────────────────────────────────────────────────────────────────────┘  │
     * │                                                                                  │
     * └─────────────────────────────────────────────────────────────────────────────────┘
     * </pre>
     */
    public void methodInvokeDemo() {
        /*
         * SynchronousMethodHandler 源码核心逻辑：
         *
         * final class SynchronousMethodHandler implements MethodHandler {
         *
         *     private final MethodMetadata metadata;
         *     private final Target<?> target;
         *     private final Client client;
         *     private final Encoder encoder;
         *     private final Decoder decoder;
         *     private final List<RequestInterceptor> requestInterceptors;
         *
         *     @Override
         *     public Object invoke(Object[] argv) throws Throwable {
         *
         *         // 1. 根据方法参数构建 RequestTemplate
         *         RequestTemplate template = buildTemplateFromArgs.create(argv);
         *
         *         // 2. 获取请求配置（超时等）
         *         Options options = findOptions(argv);
         *
         *         // 3. 重试机制
         *         Retryer retryer = this.retryer.clone();
         *         while (true) {
         *             try {
         *                 return executeAndDecode(template, options);
         *             } catch (RetryableException e) {
         *                 // 判断是否需要重试
         *                 retryer.continueOrPropagate(e);
         *                 continue;
         *             }
         *         }
         *     }
         *
         *     Object executeAndDecode(RequestTemplate template, Options options) throws Throwable {
         *
         *         // 1. 应用请求拦截器
         *         for (RequestInterceptor interceptor : requestInterceptors) {
         *             interceptor.apply(template);
         *         }
         *
         *         // 2. 构建 Request 对象
         *         Request request = targetRequest(template);
         *
         *         // 3. 执行 HTTP 请求
         *         Response response = client.execute(request, options);
         *
         *         // 4. 解码响应
         *         if (response.status() >= 200 && response.status() < 300) {
         *             // 成功响应
         *             if (void.class == metadata.returnType()) {
         *                 return null;
         *             }
         *             return decode(response);
         *         } else if (decode404 && response.status() == 404 && void.class != metadata.returnType()) {
         *             // 404 处理
         *             return decode(response);
         *         } else {
         *             // 错误响应
         *             throw errorDecoder.decode(metadata.configKey(), response);
         *         }
         *     }
         * }
         */
    }

    // ==================== 第三部分：OpenFeign 高级特性 ====================

    /**
     * <h2>1. 请求拦截器 (RequestInterceptor)</h2>
     * <pre>
     * 请求拦截器用于在发送请求前对请求进行统一处理。
     *
     * 常见使用场景：
     * - 添加认证信息（Token、签名）
     * - 添加公共请求头
     * - 请求日志记录
     * - 请求参数加密
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                        请求拦截器链                                      │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │                                                                          │
     * │   Request   →   Interceptor1   →   Interceptor2   →   Client           │
     * │   (原始请求)     (添加Token)         (添加TraceId)     (发送请求)        │
     * │                                                                          │
     * │   示例：添加 JWT Token                                                   │
     * │   ┌────────────────────────────────────────────────────────────────┐    │
     * │   │  @Component                                                     │    │
     * │   │  public class AuthRequestInterceptor implements RequestInterc. {│    │
     * │   │      @Override                                                  │    │
     * │   │      public void apply(RequestTemplate template) {              │    │
     * │   │          // 从 SecurityContext 获取 Token                       │    │
     * │   │          String token = SecurityContextHolder.getContext()      │    │
     * │   │              .getAuthentication().getCredentials().toString();  │    │
     * │   │          // 添加到请求头                                         │    │
     * │   │          template.header("Authorization", "Bearer " + token);   │    │
     * │   │      }                                                          │    │
     * │   │  }                                                              │    │
     * │   └────────────────────────────────────────────────────────────────┘    │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     * </pre>
     */
    public void requestInterceptorDemo() {
    }

    /**
     * <h2>2. 日志配置</h2>
     * <pre>
     * OpenFeign 提供了灵活的日志配置，用于调试和监控。
     *
     * 日志级别：
     * ┌────────────────┬────────────────────────────────────────────────────────┐
     * │    Level       │                      描述                               │
     * ├────────────────┼────────────────────────────────────────────────────────┤
     * │    NONE        │  不记录任何日志（默认）                                 │
     * │    BASIC       │  仅记录请求方法和URL以及响应状态码和执行时间            │
     * │    HEADERS     │  记录请求和响应的头信息                                 │
     * │    FULL        │  记录请求和响应的头、体及元数据                         │
     * └────────────────┴────────────────────────────────────────────────────────┘
     *
     * 配置方式：
     * ┌────────────────────────────────────────────────────────────────────────┐
     * │  // 方式1: 全局配置                                                     │
     * │  @Configuration                                                        │
     * │  public class FeignConfig {                                            │
     * │      @Bean                                                             │
     * │      Logger.Level feignLoggerLevel() {                                 │
     * │          return Logger.Level.FULL;                                     │
     * │      }                                                                 │
     * │  }                                                                     │
     * │                                                                        │
     * │  // 方式2: 配置文件                                                     │
     * │  feign:                                                                │
     * │    client:                                                             │
     * │      config:                                                           │
     * │        default:                        # 全局配置                       │
     * │          loggerLevel: FULL                                             │
     * │        user-service:                   # 针对特定服务                   │
     * │          loggerLevel: BASIC                                            │
     * └────────────────────────────────────────────────────────────────────────┘
     *
     * 注意：还需要配置 Spring 日志级别
     * logging:
     *   level:
     *     cn.itzixiao.interview.openfeign: DEBUG
     * </pre>
     */
    public void loggingConfigDemo() {
    }

    /**
     * <h2>3. 超时配置</h2>
     * <pre>
     * OpenFeign 的超时配置非常重要，需要根据业务场景合理设置。
     *
     * 超时类型：
     * ┌───────────────────┬────────────────────────────────────────────────────┐
     * │    超时类型        │                      描述                          │
     * ├───────────────────┼────────────────────────────────────────────────────┤
     * │  connectTimeout   │  建立连接的超时时间（默认10秒）                     │
     * │  readTimeout      │  等待响应的超时时间（默认60秒）                     │
     * └───────────────────┴────────────────────────────────────────────────────┘
     *
     * 配置方式：
     * ┌────────────────────────────────────────────────────────────────────────┐
     * │  feign:                                                                │
     * │    client:                                                             │
     * │      config:                                                           │
     * │        default:                        # 全局默认配置                   │
     * │          connectTimeout: 5000          # 5秒                           │
     * │          readTimeout: 10000            # 10秒                          │
     * │        user-service:                   # 针对特定服务                   │
     * │          connectTimeout: 3000          # 3秒                           │
     * │          readTimeout: 5000             # 5秒                           │
     * └────────────────────────────────────────────────────────────────────────┘
     *
     * 超时配置优先级：
     * 1. FeignClient 注解中的 configuration 类配置
     * 2. 配置文件中针对特定服务的配置
     * 3. 配置文件中的 default 配置
     * 4. Feign 默认配置
     * </pre>
     */
    public void timeoutConfigDemo() {
    }

    /**
     * <h2>4. 重试机制</h2>
     * <pre>
     * OpenFeign 支持请求失败后的自动重试。
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                        重试机制流程                                      │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │                                                                          │
     * │   请求失败                                                               │
     * │       │                                                                  │
     * │       ▼                                                                  │
     * │   ┌─────────────────────────────┐                                       │
     * │   │  Retryer.continueOrPropagate│                                       │
     * │   │  判断是否重试                │                                       │
     * │   └──────────────┬──────────────┘                                       │
     * │                  │                                                       │
     * │        ┌─────────┴─────────┐                                            │
     * │        ▼                   ▼                                            │
     * │   ┌──────────┐      ┌──────────────┐                                    │
     * │   │ 可以重试  │      │ 不可重试/达到 │                                   │
     * │   │ 等待后重试│      │ 最大次数     │                                   │
     * │   └────┬─────┘      └──────┬───────┘                                    │
     * │        │                   │                                            │
     * │        ▼                   ▼                                            │
     * │   再次执行请求          抛出异常                                         │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * 默认重试策略 (Retryer.Default)：
     * - maxAttempts: 5 次
     * - period: 100ms 初始间隔
     * - maxPeriod: 1s 最大间隔
     * - 使用指数退避算法
     *
     * 配置方式：
     * ┌────────────────────────────────────────────────────────────────────────┐
     * │  @Bean                                                                 │
     * │  public Retryer feignRetryer() {                                       │
     * │      // 最大重试3次，初始间隔200ms，最大间隔2s                           │
     * │      return new Retryer.Default(200, 2000, 3);                         │
     * │  }                                                                     │
     * │                                                                        │
     * │  // 禁用重试                                                            │
     * │  @Bean                                                                 │
     * │  public Retryer feignRetryer() {                                       │
     * │      return Retryer.NEVER_RETRY;                                       │
     * │  }                                                                     │
     * └────────────────────────────────────────────────────────────────────────┘
     *
     * 注意：与熔断器配合使用时，建议禁用 Feign 重试，由熔断器统一管理
     * </pre>
     */
    public void retryConfigDemo() {
    }

    /**
     * <h2>5. 熔断降级</h2>
     * <pre>
     * OpenFeign 支持与 Sentinel 或 Resilience4j 集成实现熔断降级。
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                        熔断降级机制                                      │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │                                                                          │
     * │   正常请求流程：                                                         │
     * │   Client → Feign → 远程服务 → 响应                                      │
     * │                                                                          │
     * │   熔断触发后流程：                                                       │
     * │   Client → Feign → Fallback/FallbackFactory → 降级响应                  │
     * │                                                                          │
     * │   配置方式：                                                             │
     * │   ┌──────────────────────────────────────────────────────────────────┐  │
     * │   │  // 1. 启用熔断                                                   │  │
     * │   │  feign:                                                          │  │
     * │   │    sentinel:                                                     │  │
     * │   │      enabled: true   # 或使用 circuitbreaker.enabled: true       │  │
     * │   │                                                                  │  │
     * │   │  // 2. 定义 FeignClient                                          │  │
     * │   │  @FeignClient(name = "user-service",                            │  │
     * │   │      fallback = UserClientFallback.class)                       │  │
     * │   │  // 或使用 fallbackFactory (推荐，可获取异常信息)                  │  │
     * │   │  @FeignClient(name = "user-service",                            │  │
     * │   │      fallbackFactory = UserClientFallbackFactory.class)         │  │
     * │   └──────────────────────────────────────────────────────────────────┘  │
     * │                                                                          │
     * │   fallback vs fallbackFactory：                                         │
     * │   ┌───────────────────┬────────────────────────────────────────────┐    │
     * │   │    fallback       │  简单降级，无法获取异常信息                  │    │
     * │   ├───────────────────┼────────────────────────────────────────────┤    │
     * │   │  fallbackFactory  │  可获取异常信息，便于日志记录和问题排查     │    │
     * │   └───────────────────┴────────────────────────────────────────────┘    │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     * </pre>
     */
    public void circuitBreakerDemo() {
    }

    /**
     * <h2>6. 请求压缩</h2>
     * <pre>
     * OpenFeign 支持对请求和响应进行 GZIP 压缩，减少网络传输数据量。
     *
     * ┌────────────────────────────────────────────────────────────────────────┐
     * │  feign:                                                                │
     * │    compression:                                                        │
     * │      request:                                                          │
     * │        enabled: true                   # 启用请求压缩                   │
     * │        mime-types: text/xml,application/xml,application/json           │
     * │        min-request-size: 2048          # 最小压缩阈值（字节）           │
     * │      response:                                                         │
     * │        enabled: true                   # 启用响应解压                   │
     * └────────────────────────────────────────────────────────────────────────┘
     *
     * 适用场景：
     * - 大数据量传输
     * - 网络带宽有限
     * - 数据传输成本敏感
     * </pre>
     */
    public void compressionDemo() {
    }

    // ==================== 第四部分：OpenFeign 最佳实践 ====================

    /**
     * <h2>最佳实践总结</h2>
     * <pre>
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                        OpenFeign 最佳实践                                │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │                                                                          │
     * │  1. 接口定义规范                                                         │
     * │     ├─ 将 FeignClient 接口定义在独立的 API 模块                          │
     * │     ├─ 服务提供者和消费者共享同一接口定义                                │
     * │     └─ 避免接口定义分散，确保契约一致性                                  │
     * │                                                                          │
     * │  2. 超时配置                                                             │
     * │     ├─ 根据业务场景合理设置超时时间                                      │
     * │     ├─ 核心接口设置较短超时，防止级联故障                                │
     * │     └─ 批量/异步接口可设置较长超时                                       │
     * │                                                                          │
     * │  3. 熔断降级                                                             │
     * │     ├─ 所有 FeignClient 都应配置 fallback                               │
     * │     ├─ 推荐使用 fallbackFactory 便于排查问题                            │
     * │     └─ 降级逻辑要简单可靠，避免依赖外部服务                             │
     * │                                                                          │
     * │  4. 日志配置                                                             │
     * │     ├─ 开发环境使用 FULL 级别便于调试                                   │
     * │     ├─ 生产环境使用 BASIC 或 NONE 级别                                  │
     * │     └─ 配合链路追踪系统使用                                              │
     * │                                                                          │
     * │  5. 连接池配置                                                           │
     * │     ├─ 推荐使用 OkHttp 或 Apache HttpClient                             │
     * │     ├─ 合理配置连接池大小                                                │
     * │     └─ 开启 HTTP/2 支持（如果服务端支持）                                │
     * │                                                                          │
     * │  6. 重试策略                                                             │
     * │     ├─ 与熔断器配合使用时禁用 Feign 重试                                 │
     * │     ├─ 仅对幂等接口启用重试                                              │
     * │     └─ 使用指数退避算法避免服务雪崩                                      │
     * │                                                                          │
     * │  7. 异常处理                                                             │
     * │     ├─ 自定义 ErrorDecoder 处理业务异常                                 │
     * │     ├─ 区分可重试异常和不可重试异常                                     │
     * │     └─ 保留原始异常堆栈便于排查                                          │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     * </pre>
     */
    public void bestPracticesDemo() {
    }

    // ==================== 第五部分：高频面试题 ====================

    /**
     * <h2>OpenFeign 高频面试题</h2>
     * <pre>
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                           面试题 1                                       │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │  Q: OpenFeign 的核心原理是什么？                                         │
     * │                                                                          │
     * │  A: OpenFeign 基于 JDK 动态代理实现：                                    │
     * │                                                                          │
     * │     1. 启动阶段：                                                        │
     * │        - @EnableFeignClients 触发 FeignClientsRegistrar                 │
     * │        - 扫描所有 @FeignClient 接口                                      │
     * │        - 为每个接口注册 FeignClientFactoryBean                          │
     * │                                                                          │
     * │     2. 代理生成：                                                        │
     * │        - FeignClientFactoryBean.getObject() 创建代理                    │
     * │        - Feign.Builder 配置各组件（Contract、Encoder、Decoder、Client） │
     * │        - ReflectiveFeign.newInstance() 使用 JDK Proxy 生成代理对象      │
     * │                                                                          │
     * │     3. 方法调用：                                                        │
     * │        - FeignInvocationHandler.invoke() 拦截方法调用                   │
     * │        - SynchronousMethodHandler 处理请求：                            │
     * │          · 构建 RequestTemplate                                         │
     * │          · 应用 RequestInterceptor                                      │
     * │          · Client 发送 HTTP 请求                                        │
     * │          · Decoder 解码响应                                             │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                           面试题 2                                       │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │  Q: OpenFeign 与 RestTemplate 有什么区别？                               │
     * │                                                                          │
     * │  A: 主要区别：                                                           │
     * │                                                                          │
     * │     ┌───────────────┬─────────────────────────┬─────────────────────┐   │
     * │     │     维度       │      RestTemplate       │     OpenFeign       │   │
     * │     ├───────────────┼─────────────────────────┼─────────────────────┤   │
     * │     │   编程范式     │  命令式（显式调用）     │  声明式（接口定义）  │   │
     * │     │   代码量       │  较多                   │  最少               │   │
     * │     │   可维护性     │  一般                   │  较好               │   │
     * │     │   负载均衡     │  需手动集成 @LoadBalan. │  自动集成           │   │
     * │     │   熔断降级     │  需手动实现             │  声明式配置         │   │
     * │     │   契约一致性   │  无法保证               │  接口即契约         │   │
     * │     │   维护状态     │  Spring 5.0 后弃用      │  活跃维护           │   │
     * │     └───────────────┴─────────────────────────┴─────────────────────┘   │
     * │                                                                          │
     * │     推荐在微服务架构中使用 OpenFeign，单体应用可考虑 WebClient           │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                           面试题 3                                       │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │  Q: OpenFeign 如何实现负载均衡？                                         │
     * │                                                                          │
     * │  A: OpenFeign 通过 Spring Cloud LoadBalancer 实现负载均衡：              │
     * │                                                                          │
     * │     1. Client 层集成：                                                   │
     * │        - FeignBlockingLoadBalancerClient 包装实际的 HTTP Client         │
     * │        - 请求发送前，从 URL 提取服务名                                   │
     * │                                                                          │
     * │     2. 服务发现：                                                        │
     * │        - LoadBalancerClient.choose(serviceId) 选择服务实例               │
     * │        - 从 Nacos/Eureka 等注册中心获取实例列表                          │
     * │                                                                          │
     * │     3. 负载均衡策略：                                                    │
     * │        - 默认：轮询 (RoundRobinLoadBalancer)                            │
     * │        - 可配置：随机、权重、最少活跃等                                  │
     * │                                                                          │
     * │     4. URL 重写：                                                        │
     * │        - http://user-service/users → http://192.168.1.100:8080/users    │
     * │        - reconstructURI() 将服务名替换为实际地址                         │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                           面试题 4                                       │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │  Q: OpenFeign 的超时时间如何配置？优先级是怎样的？                        │
     * │                                                                          │
     * │  A: OpenFeign 超时配置：                                                 │
     * │                                                                          │
     * │     配置方式：                                                           │
     * │     ┌──────────────────────────────────────────────────────────────┐    │
     * │     │  feign:                                                      │    │
     * │     │    client:                                                   │    │
     * │     │      config:                                                 │    │
     * │     │        default:                  # 全局配置                  │    │
     * │     │          connectTimeout: 5000    # 连接超时 5秒              │    │
     * │     │          readTimeout: 10000      # 读取超时 10秒             │    │
     * │     │        user-service:             # 特定服务配置              │    │
     * │     │          connectTimeout: 3000                                │    │
     * │     │          readTimeout: 5000                                   │    │
     * │     └──────────────────────────────────────────────────────────────┘    │
     * │                                                                          │
     * │     优先级（从高到低）：                                                 │
     * │     1. @FeignClient(configuration=XxxConfig.class) 中的配置              │
     * │     2. feign.client.config.{service-name} 特定服务配置                  │
     * │     3. feign.client.config.default 全局配置                             │
     * │     4. Feign 默认值（connect: 10s, read: 60s）                          │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                           面试题 5                                       │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │  Q: OpenFeign 如何实现熔断降级？fallback 和 fallbackFactory 的区别？     │
     * │                                                                          │
     * │  A: 熔断降级配置：                                                       │
     * │                                                                          │
     * │     1. 启用熔断（Sentinel）：                                            │
     * │        feign.sentinel.enabled: true                                     │
     * │                                                                          │
     * │     2. 定义降级实现：                                                    │
     * │        @FeignClient(name = "user-service",                              │
     * │            fallback = UserClientFallback.class,      // 方式1          │
     * │            fallbackFactory = UserClientFallbackFactory.class) // 方式2 │
     * │                                                                          │
     * │     区别：                                                               │
     * │     ┌───────────────────┬───────────────────────────────────────────┐   │
     * │     │    fallback       │  - 直接实现 FeignClient 接口              │   │
     * │     │                   │  - 无法获取触发降级的异常信息             │   │
     * │     │                   │  - 适合简单场景                           │   │
     * │     ├───────────────────┼───────────────────────────────────────────┤   │
     * │     │  fallbackFactory  │  - 实现 FallbackFactory<T> 接口          │   │
     * │     │                   │  - create(Throwable cause) 可获取异常    │   │
     * │     │                   │  - 推荐使用，便于日志记录和问题排查       │   │
     * │     └───────────────────┴───────────────────────────────────────────┘   │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                           面试题 6                                       │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │  Q: OpenFeign 如何传递请求头？如何解决 Feign 调用丢失 Header 的问题？     │
     * │                                                                          │
     * │  A: Header 传递方案：                                                    │
     * │                                                                          │
     * │     1. 方法参数传递（单个接口）：                                        │
     * │        @GetMapping("/users/{id}")                                       │
     * │        User getById(@PathVariable Long id,                              │
     * │                     @RequestHeader("Authorization") String token);      │
     * │                                                                          │
     * │     2. RequestInterceptor（全局拦截）：                                  │
     * │        @Component                                                       │
     * │        public class AuthInterceptor implements RequestInterceptor {     │
     * │            @Override                                                    │
     * │            public void apply(RequestTemplate template) {                │
     * │                // 从当前请求上下文获取 Header                            │
     * │                RequestAttributes attrs = RequestContextHolder           │
     * │                    .getRequestAttributes();                             │
     * │                if (attrs instanceof ServletRequestAttributes) {         │
     * │                    HttpServletRequest request = ((ServletRequestAttr..) │
     * │                        attrs).getRequest();                             │
     * │                    String token = request.getHeader("Authorization");   │
     * │                    if (token != null) {                                 │
     * │                        template.header("Authorization", token);         │
     * │                    }                                                    │
     * │                }                                                        │
     * │            }                                                            │
     * │        }                                                                │
     * │                                                                          │
     * │     注意：异步调用时 RequestContextHolder 可能失效，需使用 InheritableT. │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                           面试题 7                                       │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │  Q: OpenFeign 的日志级别有哪些？生产环境应该如何配置？                    │
     * │                                                                          │
     * │  A: Feign 日志级别：                                                     │
     * │                                                                          │
     * │     ┌────────────┬──────────────────────────────────────────────────┐   │
     * │     │   Level    │                      描述                         │   │
     * │     ├────────────┼──────────────────────────────────────────────────┤   │
     * │     │   NONE     │  不记录任何日志（默认，生产推荐）                 │   │
     * │     │   BASIC    │  仅记录请求方法、URL、响应状态码和执行时间        │   │
     * │     │   HEADERS  │  BASIC + 请求和响应的头信息                       │   │
     * │     │   FULL     │  HEADERS + 请求和响应的 Body（开发调试用）        │   │
     * │     └────────────┴──────────────────────────────────────────────────┘   │
     * │                                                                          │
     * │     生产环境建议：                                                       │
     * │     - 默认使用 NONE 或 BASIC                                            │
     * │     - 配合链路追踪（Sleuth/Zipkin）使用                                 │
     * │     - 异常情况可动态调整日志级别                                        │
     * │                                                                          │
     * │     配置示例：                                                           │
     * │     feign:                                                               │
     * │       client:                                                           │
     * │         config:                                                         │
     * │           default:                                                      │
     * │             loggerLevel: BASIC    # 生产环境                            │
     * │     logging:                                                            │
     * │       level:                                                            │
     * │         com.example.client: DEBUG # 需要配合 Spring 日志级别            │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                           面试题 8                                       │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │  Q: OpenFeign 和 Dubbo 的区别？各适用什么场景？                          │
     * │                                                                          │
     * │  A: 主要区别：                                                           │
     * │                                                                          │
     * │     ┌───────────────┬─────────────────────────┬──────────────────────┐  │
     * │     │     维度       │       OpenFeign         │        Dubbo         │  │
     * │     ├───────────────┼─────────────────────────┼──────────────────────┤  │
     * │     │   通信协议     │  HTTP/REST              │  Dubbo协议/多协议    │  │
     * │     │   性能         │  一般（HTTP开销）       │  较高（二进制传输）  │  │
     * │     │   序列化       │  JSON                   │  Hessian/Protobuf    │  │
     * │     │   跨语言       │  天然支持               │  需要适配            │  │
     * │     │   学习成本     │  较低                   │  较高                │  │
     * │     │   功能丰富度   │  基础功能               │  功能完善            │  │
     * │     │   生态         │  Spring Cloud          │  Apache/Alibaba      │  │
     * │     └───────────────┴─────────────────────────┴──────────────────────┘  │
     * │                                                                          │
     * │     适用场景：                                                           │
     * │     - OpenFeign: 异构系统集成、对外API、跨语言调用                       │
     * │     - Dubbo: 高性能内部服务调用、复杂治理需求                            │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                           面试题 9                                       │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │  Q: OpenFeign 调用出现 "No instances available" 错误如何排查？           │
     * │                                                                          │
     * │  A: 排查步骤：                                                           │
     * │                                                                          │
     * │     1. 检查服务注册：                                                    │
     * │        - 确认服务提供者已启动并注册到 Nacos/Eureka                       │
     * │        - 检查注册中心控制台，确认服务列表存在                            │
     * │                                                                          │
     * │     2. 检查服务名：                                                      │
     * │        - @FeignClient(name="xxx") 中的服务名是否正确                    │
     * │        - 服务名大小写是否匹配                                           │
     * │                                                                          │
     * │     3. 检查网络连通性：                                                  │
     * │        - 消费者能否访问注册中心                                          │
     * │        - 消费者能否访问服务提供者                                        │
     * │                                                                          │
     * │     4. 检查 LoadBalancer 依赖：                                          │
     * │        - 确认引入 spring-cloud-starter-loadbalancer                     │
     * │        - 检查是否存在依赖冲突                                           │
     * │                                                                          │
     * │     5. 检查命名空间和分组：                                              │
     * │        - 消费者和提供者是否在同一 namespace/group                        │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │                           面试题 10                                      │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │  Q: 如何优化 OpenFeign 的性能？                                          │
     * │                                                                          │
     * │  A: 性能优化方案：                                                       │
     * │                                                                          │
     * │     1. 使用连接池：                                                      │
     * │        - 引入 feign-okhttp 或 feign-httpclient                          │
     * │        - 配置合理的连接池大小                                           │
     * │        ┌────────────────────────────────────────────────────────────┐  │
     * │        │  feign:                                                     │  │
     * │        │    okhttp:                                                  │  │
     * │        │      enabled: true                                          │  │
     * │        │    httpclient:                                              │  │
     * │        │      enabled: true                                          │  │
     * │        │      max-connections: 200                                   │  │
     * │        │      max-connections-per-route: 50                          │  │
     * │        └────────────────────────────────────────────────────────────┘  │
     * │                                                                          │
     * │     2. 启用响应压缩：                                                    │
     * │        feign.compression.response.enabled: true                         │
     * │                                                                          │
     * │     3. 合理设置超时：                                                    │
     * │        - 避免超时设置过长导致线程阻塞                                    │
     * │        - 根据接口特性差异化配置                                          │
     * │                                                                          │
     * │     4. 禁用重试（配合熔断器）：                                          │
     * │        - 避免重复请求加重下游压力                                        │
     * │                                                                          │
     * │     5. 关闭不必要的日志：                                                │
     * │        - 生产环境使用 NONE 或 BASIC 级别                                │
     * │                                                                          │
     * │     6. 异步调用（特定场景）：                                            │
     * │        - 非关键路径可考虑异步化                                          │
     * │                                                                          │
     * └─────────────────────────────────────────────────────────────────────────┘
     *
     * </pre>
     */
    public void interviewQuestionsDemo() {
    }
}
