package cn.itzixiao.interview.warmflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j OpenAPI 配置
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Warm-Flow 工作流引擎 API 文档")
                .version("1.0.0")
                .description("基于 Warm-Flow 实现的轻量级审批工作流系统")
                .contact(new Contact()
                    .name("itzixiao")
                    .email("itzixiao@example.com")));
    }
}
