package cn.itzixiao.interview.security.protection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * XSS（跨站脚本攻击）防护工具类
 * 
 * 攻击原理：
 * - 攻击者在网页中注入恶意脚本
 * - 其他用户浏览页面时恶意脚本被执行
 * - 窃取 Cookie、会话令牌等敏感信息
 * 
 * 防护方案：
 * 1. 输入过滤：过滤特殊字符和危险标签
 * 2. 输出编码：HTML 实体编码
 * 3. 使用 HttpOnly Cookie
 * 4. 配置 CSP（内容安全策略）
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class XssFilter {
    
    /**
     * 常见 XSS 攻击模式
     */
    private static final Pattern SCRIPT_PATTERN = 
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    
    private static final Pattern EVENT_PATTERN = 
            Pattern.compile("\\s+on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern JAVASCRIPT_PATTERN = 
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    
    /**
     * 清理 XSS 攻击内容
     */
    public String sanitize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String sanitized = input;
        
        // 移除<script>标签
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // 移除事件处理器（onclick, onmouseover 等）
        sanitized = EVENT_PATTERN.matcher(sanitized).replaceAll("");
        
        // 移除 javascript:协议
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // HTML 实体编码
        sanitized = htmlEncode(sanitized);
        
        log.debug("XSS filter: {} -> {}", input, sanitized);
        
        return sanitized;
    }
    
    /**
     * HTML 实体编码
     */
    private String htmlEncode(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#x27;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
