package cn.itzixiao.interview.springmvc;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.AbstractView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

/**
 * Spring MVC 内部机制深度解析
 * <p>
 * 本示例深入讲解：
 * 1. HttpMessageConverter - 消息转换器
 * 2. Formatter/Converter - 类型转换
 * 3. HandlerExceptionResolver - 异常解析器
 * 4. View/ViewResolver - 视图与视图解析器
 * 5. RequestMappingHandlerMapping 工作原理
 * <p>
 * 文档参考：docs/05-Spring框架/03-Spring-MVC运行机制.md
 */
@RestController
@RequestMapping("/mvc/internal")
public class SpringMvcInternalsDemo {

    // ==================== 1. HttpMessageConverter 示例 ====================

    /**
     * 自定义 HttpMessageConverter
     * 作用：处理特定格式的请求/响应
     * 例如：处理自定义协议、加密传输等
     */
    @Component
    public static class CustomMessageConverter extends AbstractHttpMessageConverter<Object> {

        public CustomMessageConverter() {
            // 支持自定义媒体类型
            super(new MediaType("application", "x-custom", StandardCharsets.UTF_8));
            System.out.println("【HttpMessageConverter】CustomMessageConverter 初始化");
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            // 判断是否支持该类型
            return Map.class.isAssignableFrom(clazz);
        }

        @Override
        protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
                throws IOException, HttpMessageNotReadableException {
            System.out.println("【HttpMessageConverter】readInternal - 读取自定义格式数据");
            // 自定义反序列化逻辑
            StringBuilder sb = new StringBuilder();
            try (Reader reader = new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8)) {
                char[] buffer = new char[1024];
                int len;
                while ((len = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, len);
                }
            }
            // 模拟自定义格式解析: key1=value1;key2=value2
            Map<String, String> result = new HashMap<>();
            String[] pairs = sb.toString().split(";");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    result.put(kv[0].trim(), kv[1].trim());
                }
            }
            return result;
        }

        @Override
        protected void writeInternal(Object obj, HttpOutputMessage outputMessage)
                throws IOException, HttpMessageNotWritableException {
            System.out.println("【HttpMessageConverter】writeInternal - 写入自定义格式数据");
            // 自定义序列化逻辑
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("CUSTOM_FORMAT[\n");
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append("  ").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            sb.append("]");

            try (Writer writer = new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8)) {
                writer.write(sb.toString());
            }
        }
    }

    /**
     * 测试自定义 MessageConverter
     */
    @PostMapping(value = "/custom", consumes = "application/x-custom", produces = "application/x-custom")
    public Map<String, Object> customFormat(@RequestBody Map<String, String> data) {
        System.out.println("【Controller】接收到自定义格式数据: " + data);
        Map<String, Object> result = new HashMap<>();
        result.put("received", data);
        result.put("processed", true);
        return result;
    }

    // ==================== 2. Converter 和 Formatter 示例 ====================

    /**
     * 自定义 Converter
     * 作用：类型之间的转换（String -> Object）
     */
    @Component
    public static class StringToUserConverter implements Converter<String, UserInfo> {
        @Override
        public UserInfo convert(String source) {
            System.out.println("【Converter】StringToUserConverter.convert: " + source);
            // 格式: id:name:age
            String[] parts = source.split(":");
            UserInfo user = new UserInfo();
            if (parts.length >= 1) user.setId(Long.parseLong(parts[0]));
            if (parts.length >= 2) user.setName(parts[1]);
            if (parts.length >= 3) user.setAge(Integer.parseInt(parts[2]));
            return user;
        }
    }

    /**
     * 自定义 Formatter
     * 作用：格式化（Locale 相关的转换）
     */
    @Component
    public static class MoneyFormatter implements Formatter<Money> {
        @Override
        public Money parse(String text, Locale locale) throws ParseException {
            System.out.println("【Formatter】MoneyFormatter.parse: " + text + ", locale: " + locale);
            // 解析货币格式: $100.50 或 ￥100.50
            Money money = new Money();
            if (text.startsWith("$")) {
                money.setCurrency("USD");
                money.setAmount(Double.parseDouble(text.substring(1)));
            } else if (text.startsWith("￥")) {
                money.setCurrency("CNY");
                money.setAmount(Double.parseDouble(text.substring(1)));
            } else {
                money.setAmount(Double.parseDouble(text));
            }
            return money;
        }

        @Override
        public String print(Money object, Locale locale) {
            System.out.println("【Formatter】MoneyFormatter.print: " + object + ", locale: " + locale);
            if ("USD".equals(object.getCurrency())) {
                return String.format("$%.2f", object.getAmount());
            } else if ("CNY".equals(object.getCurrency())) {
                return String.format("￥%.2f", object.getAmount());
            }
            return String.format("%.2f", object.getAmount());
        }
    }

    /**
     * 测试 Converter
     */
    @GetMapping("/converter")
    public String testConverter(@RequestParam("user") UserInfo user) {
        System.out.println("【Controller】转换后的 User: " + user);
        return "Converted: " + user;
    }

    /**
     * 测试 Formatter
     */
    @GetMapping("/formatter")
    public String testFormatter(@RequestParam("money") Money money) {
        System.out.println("【Controller】格式化后的 Money: " + money);
        return "Formatted: " + money.getCurrency() + " " + money.getAmount();
    }

    // ==================== 3. 异常处理机制 ====================

    /**
     * 自定义 HandlerExceptionResolver
     * 作用：处理异常并返回 ModelAndView
     */
    @Component
    public static class CustomExceptionResolver implements HandlerExceptionResolver {
        @Override
        public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                             Object handler, Exception ex) {
            System.out.println("【HandlerExceptionResolver】处理异常: " + ex.getClass().getSimpleName());

            // 特定异常处理
            if (ex instanceof IllegalArgumentException) {
                ModelAndView mav = new ModelAndView();
                mav.addObject("errorCode", 400);
                mav.addObject("errorMessage", "参数错误: " + ex.getMessage());
                mav.setViewName("error/400");
                return mav;
            }
            // 返回 null 表示不处理，交给下一个 Resolver
            return null;
        }
    }

    /**
     * 触发异常测试
     */
    @GetMapping("/exception/{type}")
    public String triggerException(@PathVariable String type) {
        System.out.println("【Controller】触发异常: " + type);
        switch (type) {
            case "illegal":
                throw new IllegalArgumentException("非法参数");
            case "null":
                throw new NullPointerException("空指针");
            case "runtime":
                throw new RuntimeException("运行时异常");
            default:
                return "No exception";
        }
    }

    // ==================== 4. 视图解析机制 ====================

    /**
     * 自定义 View
     * 作用：自定义视图渲染
     */
    public static class JsonView extends AbstractView {
        public JsonView() {
            setContentType("application/json");
        }

        @Override
        protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {
            System.out.println("【View】JsonView.renderMergedOutputModel");
            response.setContentType(getContentType());

            // 自定义 JSON 渲染
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : model.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("}");

            response.getWriter().write(json.toString());
        }
    }

    /**
     * 自定义 ViewResolver
     * 作用：根据视图名称解析 View 对象
     */
    @Component
    public static class CustomViewResolver implements org.springframework.web.servlet.ViewResolver {
        @Override
        public View resolveViewName(String viewName, Locale locale) throws Exception {
            System.out.println("【ViewResolver】CustomViewResolver.resolveViewName: " + viewName);
            if ("customJson".equals(viewName)) {
                return new JsonView();
            }
            // 返回 null 表示不处理，交给下一个 Resolver
            return null;
        }
    }

    // ==================== 5. 请求映射机制 ====================

    /**
     * RequestMappingHandlerMapping 工作原理：
     *
     * 1. 初始化时扫描所有 @Controller 类
     * 2. 解析 @RequestMapping 注解，创建 RequestMappingInfo
     * 3. 将 URL 映射到 HandlerMethod
     * 4. 请求到达时，根据 URL 匹配 HandlerMethod
     */

    /**
     * 标准 REST API 示例
     */
    @GetMapping("/users")
    public List<Map<String, Object>> listUsers() {
        System.out.println("【HandlerMapping】匹配到 GET /mvc/internal/users");
        return Arrays.asList(
                createUser(1L, "张三", "zhangsan@example.com"),
                createUser(2L, "李四", "lisi@example.com")
        );
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> getUserById(@PathVariable Long id) {
        System.out.println("【HandlerMapping】匹配到 GET /mvc/internal/users/{id}, id=" + id);
        return createUser(id, "User" + id, "user" + id + "@example.com");
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody Map<String, Object> user) {
        System.out.println("【HandlerMapping】匹配到 POST /mvc/internal/users");
        user.put("id", System.currentTimeMillis());
        return user;
    }

    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> user) {
        System.out.println("【HandlerMapping】匹配到 PUT /mvc/internal/users/{id}, id=" + id);
        user.put("id", id);
        return user;
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        System.out.println("【HandlerMapping】匹配到 DELETE /mvc/internal/users/{id}, id=" + id);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("deleted", true);
        return result;
    }

    /**
     * 复杂 URL 映射示例
     */
    @GetMapping({"/articles", "/posts"})
    public String multiplePaths() {
        System.out.println("【HandlerMapping】匹配到多个路径: /articles 或 /posts");
        return "Multiple paths matched";
    }

    @GetMapping(value = "/content", params = "type=article")
    public String paramCondition(@RequestParam String type) {
        System.out.println("【HandlerMapping】Param 条件匹配: type=" + type);
        return "Article content";
    }

    @GetMapping(value = "/content", params = "type=video")
    public String paramConditionVideo(@RequestParam String type) {
        System.out.println("【HandlerMapping】Param 条件匹配: type=" + type);
        return "Video content";
    }

    @GetMapping(value = "/headers", headers = "X-API-Version=2")
    public String headerCondition() {
        System.out.println("【HandlerMapping】Header 条件匹配: X-API-Version=2");
        return "API Version 2";
    }

    @GetMapping(value = "/produces", produces = "application/json")
    public Map<String, Object> producesCondition() {
        System.out.println("【HandlerMapping】Produces 条件匹配: application/json");
        Map<String, Object> result = new HashMap<>();
        result.put("format", "json");
        return result;
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> createUser(Long id, String name, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("name", name);
        user.put("email", email);
        return user;
    }

    // ==================== 实体类 ====================

    public static class UserInfo {
        private Long id;
        private String name;
        private Integer age;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "UserInfo{id=" + id + ", name='" + name + "', age=" + age + "}";
        }
    }

    public static class Money {
        private String currency;
        private double amount;

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        @Override
        public String toString() {
            return currency + " " + amount;
        }
    }
}

/**
 * 配置类：注册自定义组件
 */
@Component
class MvcInternalsConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 添加自定义 MessageConverter
        // converters.add(new SpringMvcInternalsDemo.CustomMessageConverter());
        System.out.println("【配置】注册 HttpMessageConverters, 数量: " + converters.size());
    }

    @Override
    public void addFormatters(org.springframework.format.FormatterRegistry registry) {
        // 注册自定义 Converter 和 Formatter
        registry.addConverter(new SpringMvcInternalsDemo.StringToUserConverter());
        registry.addFormatter(new SpringMvcInternalsDemo.MoneyFormatter());
        System.out.println("【配置】注册 Formatters 和 Converters");
    }
}

/**
 * ============================================
 * 视图渲染流程模拟
 * ============================================
 * 当 Controller 返回视图名称时，Spring MVC 会进行视图渲染
 * <p>
 * 完整流程：
 * 1. processDispatchResult() - 处理分发结果
 * 2. render() - 渲染视图
 * 3. resolveViewName() - ViewResolver 解析视图名称
 * 4. View.render() - 视图渲染
 * <p>
 * 源码参考：
 * protected void render(ModelAndView mv, HttpServletRequest request,
 *         HttpServletResponse response) throws Exception {
 *
 *     // 1. 确定 Locale
 *     Locale locale = (this.localeResolver != null ?
 *         this.localeResolver.resolveLocale(request) : request.getLocale());
 *     response.setLocale(locale);
 *
 *     View view;
 *     String viewName = mv.getViewName();
 *
 *     // 2. 解析视图
 *     if (viewName != null) {
 *         view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
 *     } else {
 *         view = mv.getView();
 *     }
 *
 *     // 3. 渲染视图
 *     view.render(mv.getModelInternal(), request, response);
 * }
 */
class ViewRenderingSimulator {

    /**
     * 模拟视图解析器
     * 类似 InternalResourceViewResolver
     */
    static class ViewResolverSimulator {
        private String prefix = "/WEB-INF/views/";
        private String suffix = ".jsp";

        /**
         * 解析视图名称
         * <p>
         * 源码参考 InternalResourceViewResolver：
         * protected View createView(String viewName, Locale locale) throws Exception {
         *     // 处理 redirect:
         *     if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
         *         String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
         *         return new RedirectView(redirectUrl);
         *     }
         *
         *     // 处理 forward:
         *     if (viewName.startsWith(FORWARD_URL_PREFIX)) {
         *         String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length());
         *         return new InternalResourceView(forwardUrl);
         *     }
         *
         *     // 默认：拼接前缀后缀
         *     return super.createView(viewName, locale);
         * }
         */
        public ViewSimulator resolveViewName(String viewName, Locale locale) {
            System.out.println("\n========== ViewResolver 解析视图 ==========");
            System.out.println("【输入】视图名称: " + viewName);

            // 处理 redirect:
            if (viewName.startsWith("redirect:")) {
                String redirectUrl = viewName.substring("redirect:".length());
                System.out.println("【类型】重定向视图");
                System.out.println("【目标URL】" + redirectUrl);
                return new RedirectViewSimulator(redirectUrl);
            }

            // 处理 forward:
            if (viewName.startsWith("forward:")) {
                String forwardUrl = viewName.substring("forward:".length());
                System.out.println("【类型】转发视图");
                System.out.println("【目标URL】" + forwardUrl);
                return new ForwardViewSimulator(forwardUrl);
            }

            // 默认：拼接前缀后缀
            String url = prefix + viewName + suffix;
            System.out.println("【类型】JSP 视图");
            System.out.println("【前缀】" + prefix);
            System.out.println("【后缀】" + suffix);
            System.out.println("【完整路径】" + url);

            System.out.println("========== ViewResolver 解析完成 ==========\n");
            return new JspViewSimulator(url);
        }
    }

    /**
     * 视图接口模拟
     */
    interface ViewSimulator {
        void render(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;
    }

    /**
     * JSP 视图模拟
     * <p>
     * 源码参考 InternalResourceView.render()：
     * protected void renderMergedOutputModel(Map<String, Object> model,
     *         HttpServletRequest request, HttpServletResponse response) throws Exception {
     *
     *     // 1. 合并 Model 数据到请求属性
     *     exposeModelAsRequestAttributes(model, request);
     *
     *     // 2. 暴露 Spring Bean 到请求属性
     *     exposeHelpers(request);
     *
     *     // 3. 转发到 JSP
     *     RequestDispatcher rd = request.getRequestDispatcher(getUrl());
     *     rd.forward(request, response);
     * }
     */
    static class JspViewSimulator implements ViewSimulator {
        private final String url;

        public JspViewSimulator(String url) {
            this.url = url;
        }

        @Override
        public void render(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
            System.out.println("\n========== JSP 视图渲染 ==========");
            System.out.println("【视图路径】" + url);

            // 步骤 1：合并 Model 数据到请求属性
            System.out.println("【步骤 1】合并 Model 数据到请求属性");
            for (Map.Entry<String, Object> entry : model.entrySet()) {
                System.out.println("  request.setAttribute(\"" + entry.getKey() + "\", " + entry.getValue() + ")");
                // 实际代码：request.setAttribute(entry.getKey(), entry.getValue());
            }

            // 步骤 2：转发到 JSP
            System.out.println("【步骤 2】转发到 JSP");
            System.out.println("  RequestDispatcher dispatcher = request.getRequestDispatcher(\"" + url + "\")");
            System.out.println("  dispatcher.forward(request, response)");

            // 步骤 3：JSP 渲染
            System.out.println("【步骤 3】JSP 渲染");
            System.out.println("  JSP 模板执行，输出 HTML");

            System.out.println("========== JSP 视图渲染完成 ==========\n");
        }
    }

    /**
     * 重定向视图模拟
     */
    static class RedirectViewSimulator implements ViewSimulator {
        private final String redirectUrl;

        public RedirectViewSimulator(String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }

        @Override
        public void render(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
            System.out.println("\n========== 重定向视图 ==========");
            System.out.println("【目标URL】" + redirectUrl);
            System.out.println("【HTTP 状态】302 Found");
            System.out.println("【Location 头】" + redirectUrl);
            System.out.println("========== 重定向完成 ==========\n");
        }
    }

    /**
     * 转发视图模拟
     */
    static class ForwardViewSimulator implements ViewSimulator {
        private final String forwardUrl;

        public ForwardViewSimulator(String forwardUrl) {
            this.forwardUrl = forwardUrl;
        }

        @Override
        public void render(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
            System.out.println("\n========== 转发视图 ==========");
            System.out.println("【目标URL】" + forwardUrl);
            System.out.println("【方式】服务器端转发（RequestDispatcher.forward）");
            System.out.println("========== 转发完成 ==========\n");
        }
    }

    /**
     * 模拟完整的视图渲染流程
     */
    public void simulateViewRendering(String viewName, Map<String, Object> model,
                                       HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("\n=============== 视图渲染完整流程 ===============");

        // 步骤 1：创建 ModelAndView
        System.out.println("\n【步骤 1】创建 ModelAndView");
        ModelAndView mav = new ModelAndView(viewName);
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            mav.addObject(entry.getKey(), entry.getValue());
        }
        System.out.println("  视图名称: " + mav.getViewName());
        System.out.println("  模型数据: " + mav.getModel());

        // 步骤 2：确定 Locale
        System.out.println("\n【步骤 2】确定 Locale");
        Locale locale = request.getLocale();
        System.out.println("  Locale: " + locale);

        // 步骤 3：解析视图
        System.out.println("\n【步骤 3】解析视图");
        ViewResolverSimulator viewResolver = new ViewResolverSimulator();
        ViewSimulator view = viewResolver.resolveViewName(viewName, locale);

        // 步骤 4：渲染视图
        System.out.println("\n【步骤 4】渲染视图");
        view.render(model, request, response);

        System.out.println("\n=============== 视图渲染流程结束 ===============\n");
    }
}

/**
 * ============================================
 * 响应返回流程模拟
 * ============================================
 * 当请求处理完成后，响应会经过以下流程返回给客户端
 * <p>
 * 完整流程：
 * 1. 设置响应状态码
 * 2. 设置响应头
 * 3. 写入响应体
 * 4. 刷新缓冲区
 * 5. Tomcat 处理响应
 * 6. 浏览器接收响应
 */
class ResponseReturnSimulator {

    /**
     * 模拟响应构建过程
     */
    public void simulateResponseBuilding(HttpServletResponse response, Object responseBody) {
        System.out.println("\n=============== 响应构建流程 ===============");

        // 步骤 1：设置响应状态码
        System.out.println("\n【步骤 1】设置响应状态码");
        System.out.println("  response.setStatus(200)");
        System.out.println("  HTTP/1.1 200 OK");

        // 步骤 2：设置响应头
        System.out.println("\n【步骤 2】设置响应头");
        System.out.println("  Content-Type: application/json; charset=UTF-8");
        System.out.println("  Content-Length: " + calculateContentLength(responseBody));
        System.out.println("  Cache-Control: no-cache");

        // 步骤 3：写入响应体
        System.out.println("\n【步骤 3】写入响应体");
        String json = convertToJson(responseBody);
        System.out.println("  响应体: " + json);
        System.out.println("  response.getOutputStream().write(bytes)");

        // 步骤 4：刷新缓冲区
        System.out.println("\n【步骤 4】刷新缓冲区");
        System.out.println("  response.flushBuffer()");

        System.out.println("\n=============== 响应构建完成 ===============\n");
    }

    /**
     * 模拟 Tomcat 处理响应
     */
    public void simulateTomcatResponseHandling() {
        System.out.println("\n=============== Tomcat 响应处理 ===============");

        System.out.println("\n【步骤 1】CoyoteAdapter.afterService()");
        System.out.println("  提交响应");
        System.out.println("  更新统计信息");

        System.out.println("\n【步骤 2】Http11Processor.process()");
        System.out.println("  构建 HTTP 响应报文");
        System.out.println("  HTTP/1.1 200 OK");
        System.out.println("  Content-Type: application/json");
        System.out.println("  Content-Length: 28");
        System.out.println("  ");
        System.out.println("  {\"id\":1,\"name\":\"张三\"}");

        System.out.println("\n【步骤 3】NioEndpoint 发送数据");
        System.out.println("  写入 Socket 缓冲区");
        System.out.println("  TCP 发送给客户端");

        System.out.println("\n=============== Tomcat 响应处理完成 ===============\n");
    }

    /**
     * 模拟浏览器接收响应
     */
    public void simulateBrowserReceiveResponse() {
        System.out.println("\n=============== 浏览器接收响应 ===============");

        System.out.println("\n【步骤 1】解析 HTTP 响应");
        System.out.println("  解析状态行: HTTP/1.1 200 OK");
        System.out.println("  解析响应头");

        System.out.println("\n【步骤 2】解析响应体");
        System.out.println("  Content-Type: application/json");
        System.out.println("  解析 JSON 数据");

        System.out.println("\n【步骤 3】渲染或回调");
        System.out.println("  如果是页面请求: 渲染 HTML");
        System.out.println("  如果是 AJAX 请求: 执行回调函数");

        System.out.println("\n=============== 浏览器处理完成 ===============\n");
    }

    private int calculateContentLength(Object body) {
        return convertToJson(body).length();
    }

    private String convertToJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            Map<?, ?> map = (Map<?, ?>) obj;
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                if (entry.getValue() instanceof String) {
                    sb.append("\"").append(entry.getValue()).append("\"");
                } else {
                    sb.append(entry.getValue());
                }
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        return "{\"data\":\"" + obj + "\"}";
    }
}

/**
 * ============================================
 * 完整请求处理流程演示
 * ============================================
 * 演示从请求到达到响应返回的完整流程
 */
class CompleteRequestFlowDemo {

    /**
     * 演示完整请求处理流程
     */
    public void demonstrateCompleteFlow() {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("                    Spring MVC 完整请求处理流程演示");
        System.out.println(repeat("=", 80));

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        1. 请求到达阶段                                        │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  浏览器 → DNS解析 → TCP连接 → HTTP请求 → Tomcat → Servlet容器               │");
        System.out.println("│  GET /users/123 HTTP/1.1                                                    │");
        System.out.println("│  Host: localhost:8080                                                       │");
        System.out.println("│  Content-Type: application/json                                             │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        2. DispatcherServlet 处理                             │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  doDispatch() {                                                             │");
        System.out.println("│    // 1. checkMultipart() - 检查文件上传                                      │");
        System.out.println("│    // 2. getHandler() - 获取处理器执行链                                       │");
        System.out.println("│    // 3. getHandlerAdapter() - 获取适配器                                     │");
        System.out.println("│    // 4. applyPreHandle() - 执行拦截器前置                                     │");
        System.out.println("│    // 5. ha.handle() - 调用处理器方法                                          │");
        System.out.println("│    // 6. applyPostHandle() - 执行拦截器后置                                    │");
        System.out.println("│    // 7. processDispatchResult() - 处理结果                                   │");
        System.out.println("│  }                                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        3. HandlerMapping 匹配                                │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  RequestMappingHandlerMapping.getHandler() {                                │");
        System.out.println("│    // 遍历所有注册的映射                                                       │");
        System.out.println("│    // /users/{id} → UserController.getById()                                │");
        System.out.println("│    // 提取路径变量: id = 123                                                  │");
        System.out.println("│    // 返回 HandlerExecutionChain                                             │");
        System.out.println("│  }                                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        4. HandlerAdapter 调用                                │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  RequestMappingHandlerAdapter.handle() {                                    │");
        System.out.println("│    // 1. invokeHandlerMethod()                                              │");
        System.out.println("│    //    - 参数解析 (ArgumentResolver)                                        │");
        System.out.println("│    //    - 执行 Controller 方法                                               │");
        System.out.println("│    //    - 返回值处理 (ReturnValueHandler)                                    │");
        System.out.println("│    // 2. 返回 ModelAndView                                                   │");
        System.out.println("│  }                                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        5. 参数解析流程                                        │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  getMethodArgumentValues() {                                                │");
        System.out.println("│    // 参数 1: @PathVariable Long id                                          │");
        System.out.println("│    //   → PathVariableMethodArgumentResolver                                │");
        System.out.println("│    //   → 从 URI 变量获取: id = 123                                          │");
        System.out.println("│    //   → 类型转换: String \"123\" → Long 123                                  │");
        System.out.println("│  }                                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        6. Controller 执行                                    │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  @GetMapping(\"/users/{id}\")                                                 │");
        System.out.println("│  public User getById(@PathVariable Long id) {                               │");
        System.out.println("│      return userService.findById(id);  // 返回 User 对象                      │");
        System.out.println("│  }                                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        7. 返回值处理流程                                      │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  handleReturnValue() {                                                      │");
        System.out.println("│    // 选择处理器: RequestResponseBodyMethodProcessor                         │");
        System.out.println("│    // 选择转换器: MappingJackson2HttpMessageConverter                        │");
        System.out.println("│    // 序列化: User → JSON                                                    │");
        System.out.println("│    // 写入: response.getOutputStream()                                       │");
        System.out.println("│  }                                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        8. 响应返回流程                                        │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  HTTP/1.1 200 OK                                                            │");
        System.out.println("│  Content-Type: application/json                                             │");
        System.out.println("│  Content-Length: 28                                                         │");
        System.out.println("│                                                                              │");
        System.out.println("│  {\"id\":123,\"name\":\"张三\"}                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n" + repeat("=", 80));
        System.out.println("                              流程演示结束");
        System.out.println(repeat("=", 80) + "\n");
    }

    /**
     * Java 8 兼容的字符串重复方法
     */
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
