package top.principlecreativity.lifestream.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    @Order(1) // 确保这个过滤器链在主过滤器链之前处理
    public SecurityFilterChain webFormLoginFilterChain(HttpSecurity http) throws Exception {
        http
                // 只处理静态资源，让主SecurityConfig处理身份验证逻辑
                .securityMatcher("/css/**", "/js/**", "/images/**", "/webjars/**", "/static/**", "/favicon.ico")
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/static/**", "/favicon.ico").permitAll()
                );

        return http.build();
    }
}