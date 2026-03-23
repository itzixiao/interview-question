package cn.itzixiao.interview.springai.function;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * 计算器 Function
 * 
 * <p>演示 AI Function Calling 功能，AI 可以调用此函数进行数学计算</p>
 * 
 * @author itzixiao
 * @since 2024-01-01
 */
@Slf4j
@Configuration
public class CalculatorFunction {

    /**
     * 计算请求
     */
    @JsonClassDescription("数学计算请求参数")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            @JsonProperty(required = true, value = "expression")
            @JsonPropertyDescription("数学表达式，如：1 + 2, 3.14 * 2, sqrt(16)")
            String expression
    ) {
    }

    /**
     * 计算响应
     */
    @JsonClassDescription("数学计算响应结果")
    public record Response(
            @JsonProperty(value = "expression")
            @JsonPropertyDescription("原始表达式")
            String expression,

            @JsonProperty(value = "result")
            @JsonPropertyDescription("计算结果")
            String result,

            @JsonProperty(value = "success")
            @JsonPropertyDescription("是否计算成功")
            boolean success,

            @JsonProperty(value = "error")
            @JsonPropertyDescription("错误信息（如果计算失败）")
            String error
    ) {
    }

    /**
     * 计算器函数 Bean
     * 
     * <p>AI 在需要进行数学计算时会自动调用此函数</p>
     */
    @Bean
    @Description("执行数学计算，支持基本运算（加减乘除）、幂运算、开方等")
    public Function<Request, Response> calculate() {
        return request -> {
            log.info("执行计算: expression={}", request.expression());
            
            try {
                // 简单的表达式计算（实际应用中可以使用更强大的表达式引擎）
                String expr = request.expression().replaceAll("\\s+", "");
                double result = evaluate(expr);
                
                return new Response(
                        request.expression(),
                        String.valueOf(result),
                        true,
                        null
                );
            } catch (Exception e) {
                log.error("计算错误: {}", e.getMessage());
                return new Response(
                        request.expression(),
                        null,
                        false,
                        "计算错误: " + e.getMessage()
                );
            }
        };
    }

    /**
     * 简单的表达式求值
     */
    private double evaluate(String expression) {
        // 处理 sqrt 函数
        if (expression.startsWith("sqrt(") && expression.endsWith(")")) {
            double value = Double.parseDouble(expression.substring(5, expression.length() - 1));
            return Math.sqrt(value);
        }
        
        // 处理幂运算
        if (expression.contains("^")) {
            String[] parts = expression.split("\\^");
            double base = Double.parseDouble(parts[0]);
            double exponent = Double.parseDouble(parts[1]);
            return Math.pow(base, exponent);
        }
        
        // 基本运算
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+");
            return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
        }
        if (expression.contains("-")) {
            String[] parts = expression.split("-");
            return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
        }
        if (expression.contains("*")) {
            String[] parts = expression.split("\\*");
            return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
        }
        if (expression.contains("/")) {
            String[] parts = expression.split("/");
            return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        }
        
        // 纯数字
        return Double.parseDouble(expression);
    }
}
