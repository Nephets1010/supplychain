package io.github.nephets.supplychain.config;

import io.github.nephets.supplychain.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    /** 允许携带凭据的来源白名单，逗号分隔；配置来自 app.cors.allowed-origins，默认 "*"（历史行为）。 */
    private final String allowedOrigins;

    public SecurityConfig(
            JwtFilter jwtFilter, @Value("${app.cors.allowed-origins:*}") String allowedOrigins) {
        this.jwtFilter = jwtFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/api/auth/login",
                                                "/api/settle/preview",
                                                "/actuator/health",
                                                "/h2-console/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**")
                                        .permitAll()
                                        .requestMatchers("/api/merchant/**")
                                        .hasAuthority("ROLE_MERCHANT")
                                        .requestMatchers("/api/shop/**")
                                        .hasAuthority("ROLE_SHOPKEEPER")
                                        .requestMatchers("/api/admin/**")
                                        .hasAuthority("ROLE_ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(h -> h.frameOptions(f -> f.disable()))
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (req, res, ex) -> writeJson(res, 401, "未认证或令牌已失效"))
                                        .accessDeniedHandler(
                                                (req, res, ex) -> writeJson(res, 403, "无权限访问该资源")));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(parseAllowedOrigins(allowedOrigins));
        c.setAllowedMethods(List.of("*"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

    /**
     * 解析逗号分隔的来源白名单。空值 / 空串一律回退为 "*"，确保未配置时行为与历史版本一致。
     *
     * @param raw 原始配置值，可为 null
     * @return 来源模式列表，永不为空
     */
    private static List<String> parseAllowedOrigins(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of("*");
        }
        List<String> origins =
                Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return origins.isEmpty() ? List.of("*") : origins;
    }

    private void writeJson(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\":\"" + status + "\",\"message\":\"" + message + "\"}");
    }
}
