package cn.itzixiao.interview.security.oauth2;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证授权处理器 - OAuth2.0 + JWT
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class AuthenticationManager {

    @Autowired
    private JwtTokenProvider tokenProvider;

    /**
     * 从请求中获取 Token 并设置认证信息
     */
    public void setAuthentication(HttpServletRequest request) {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            String username = tokenProvider.getUsernameFromToken(token);

            // 从 Token 中获取权限列表
            Claims claims = tokenProvider.getClaimsFromToken(token);
            List<String> authorities = (List<String>) claims.get("authorities");

            // 设置 Spring Security 上下文
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    authorities != null ? authorities.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList()) : java.util.Collections.emptyList()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Set authentication for user: {}", username);
        }
    }

    /**
     * 从 Header 中解析 Token
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
