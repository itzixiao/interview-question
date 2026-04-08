package cn.itzixiao.interview.warmflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 安全配置
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 放行 Warm-Flow 设计器相关路径
                .requestMatchers("/warm-flow-ui/**", "/warm-flow/**").permitAll()
                // 放行 API 文档
                .requestMatchers("/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()
                // 放行工作流 API
                .requestMatchers("/api/workflow/**").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
