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
 * 天气查询 Function
 * 
 * <p>演示 AI Function Calling 功能，AI 可以调用此函数获取实时天气信息</p>
 * 
 * @author itzixiao
 * @since 2024-01-01
 */
@Slf4j
@Configuration
public class WeatherFunction {

    /**
     * 天气查询请求
     */
    @JsonClassDescription("天气查询请求参数")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            @JsonProperty(required = true, value = "city")
            @JsonPropertyDescription("城市名称，如：北京、上海、广州")
            String city,

            @JsonProperty(value = "date")
            @JsonPropertyDescription("日期，格式：yyyy-MM-dd，默认为今天")
            String date
    ) {
    }

    /**
     * 天气查询响应
     */
    @JsonClassDescription("天气查询响应结果")
    public record Response(
            @JsonProperty(value = "city")
            @JsonPropertyDescription("城市名称")
            String city,

            @JsonProperty(value = "temperature")
            @JsonPropertyDescription("温度")
            String temperature,

            @JsonProperty(value = "weather")
            @JsonPropertyDescription("天气状况")
            String weather,

            @JsonProperty(value = "humidity")
            @JsonPropertyDescription("湿度")
            String humidity,

            @JsonProperty(value = "wind")
            @JsonPropertyDescription("风力")
            String wind
    ) {
    }

    /**
     * 天气查询函数 Bean
     * 
     * <p>AI 在需要获取天气信息时会自动调用此函数</p>
     */
    @Bean
    @Description("获取指定城市的天气信息，包括温度、天气状况、湿度和风力")
    public Function<Request, Response> getWeather() {
        return request -> {
            log.info("获取天气信息: city={}, date={}", request.city(), request.date());
            
            // 模拟天气数据（实际应用中可调用天气 API）
            return switch (request.city()) {
                case "北京" -> new Response("北京", "25°C", "晴", "45%", "3级");
                case "上海" -> new Response("上海", "28°C", "多云", "60%", "2级");
                case "广州" -> new Response("广州", "32°C", "雷阵雨", "75%", "4级");
                case "深圳" -> new Response("深圳", "31°C", "多云", "70%", "3级");
                case "杭州" -> new Response("杭州", "26°C", "小雨", "65%", "2级");
                default -> new Response(request.city(), "24°C", "晴", "50%", "2级");
            };
        };
    }
}
