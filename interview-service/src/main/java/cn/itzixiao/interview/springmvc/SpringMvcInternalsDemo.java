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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Spring MVC 内部机制深度解析
 *
 * 本示例深入讲解：
 * 1. HttpMessageConverter - 消息转换器
 * 2. Formatter/Converter - 类型转换
 * 3. HandlerExceptionResolver - 异常解析器
 * 4. View/ViewResolver - 视图与视图解析器
 * 5. RequestMappingHandlerMapping 工作原理
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

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }

        @Override
        public String toString() {
            return "UserInfo{id=" + id + ", name='" + name + "', age=" + age + "}";
        }
    }

    public static class Money {
        private String currency;
        private double amount;

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

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
