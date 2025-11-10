package top.principlecreativity.lifestream.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
// [注意] 移除了未使用的 WebMvcConfigurer 导入
import top.principlecreativity.lifestream.security.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true
)
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider tokenProvider;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          JwtTokenProvider tokenProvider,
                          JwtAuthenticationEntryPoint unauthorizedHandler) {
        this.customUserDetailsService = customUserDetailsService;
        this.tokenProvider = tokenProvider;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider, customUserDetailsService);
    }

    @Bean
    public JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter() {
        return new JwtCookieAuthenticationFilter(tokenProvider, customUserDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/");
        successHandler.setAlwaysUseDefaultTargetUrl(true);
        return successHandler;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(List.of("x-auth-token"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // --- [修复 1: 新增] ---
    // 创建一个最高优先级的过滤器链，专门用于静态资源
    @Bean
    @Order(0)
    public SecurityFilterChain staticResourceFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 只匹配所有 Spring Boot 的标准静态资源路径
                .securityMatcher(PathRequest.toStaticResources().atCommonLocations())
                .authorizeHttpRequests(auth -> auth
                        // 2. 允许所有匹配的请求
                        .anyRequest().permitAll()
                )
                // 3. 静态资源不需要 Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 4. 禁用 CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // 5. [关键] 这个链条 *不* 添加任何 JWT 过滤器
                .exceptionHandling(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // --- [保持不变] ---
    // profileFilterChain 现在是 Order 1
    @Bean
    @Order(1)
    public SecurityFilterChain profileFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/profile/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // [修复 3: 语法更新] 修复“已过时”警告
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            System.out.println("Profile访问 - 路径: " + request.getRequestURI() +
                                    ", 认证状态: " + (request.getUserPrincipal() != null));
                            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            } else {
                                String returnUrl = request.getRequestURI();
                                response.sendRedirect("/login?returnUrl=" +
                                        URLEncoder.encode(returnUrl, StandardCharsets.UTF_8));
                            }
                        }));

        http.addFilterBefore(jwtCookieAuthenticationFilter(), BasicAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    // --- [保持不变] ---
    // apiFilterChain 现在是 Order 2
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // [修复 3: 语法更新] 修复“已过时”警告
                .csrf(AbstractHttpConfigurer::disable)

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // API公开端点
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/posts/**").permitAll()
                        .requestMatchers("/api/tags/**").permitAll()
                        .requestMatchers("/api/albums/**").permitAll()
                        .requestMatchers("/api/comments/**").permitAll()
                        .requestMatchers("/api/user/checkUsernameAvailability").permitAll()
                        .requestMatchers("/api/user/checkEmailAvailability").permitAll()
                        .requestMatchers("/api/images/download/**").permitAll()
                        // 需要认证的API
                        .requestMatchers("/api/images/upload").authenticated()
                        .requestMatchers("/api/user/me", "/api/user/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/**").authenticated()
                        .anyRequest().authenticated())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpStatus.OK.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":true,\"message\":\"Logout successful\"}");
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        http.addFilterBefore(jwtCookieAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // --- [修复 2: 已移除静态资源] ---
    // defaultFilterChain 现在是 Order 3
    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // [修复 3: 语法更新] 修复“已过时”警告
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // 公开页面
                        .requestMatchers("/", "/index", "/index.html", "/error").permitAll()
                        .requestMatchers("/login", "/register", "/signin", "/signup").permitAll()
                        .requestMatchers("/forgot-password", "/terms", "/privacy").permitAll()
                        // 文章相关页面和功能
                        .requestMatchers("/posts", "/posts/**").permitAll()
                        .requestMatchers("/albums", "/albums/**").permitAll()
                        .requestMatchers("/tags", "/tags/**").permitAll()
                        .requestMatchers("/search").permitAll()
                        .requestMatchers("/archives", "/archives/**").permitAll()
                        // 错误处理
                        .requestMatchers("/error/**").permitAll()

                        // [修复 2: 已移除]
                        // 所有静态资源规则 (如 /css/**, /js/**) 已被移除,
                        // 因为它们现在由 @Order(0) 的 staticResourceFilterChain 处理

                        // 需要认证的功能
                        .requestMatchers("/dashboard/**").authenticated()
                        .requestMatchers("/posts/new", "/posts/*/edit").authenticated()
                        .requestMatchers("/albums/new", "/albums/*/edit").authenticated()

                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            System.out.println("Web访问 - 路径: " + request.getRequestURI() +
                                    ", 认证状态: " + (request.getUserPrincipal() != null));
                            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            } else {
                                String returnUrl = request.getRequestURI();
                                response.sendRedirect("/login?returnUrl=" +
                                        URLEncoder.encode(returnUrl, StandardCharsets.UTF_8));
                            }
                        })
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .successHandler(authenticationSuccessHandler())
                        .permitAll())
                .rememberMe(remember -> remember
                        .key("lifeStreamSecretKey")
                        .tokenValiditySeconds(2592000)
                        .rememberMeParameter("remember-me")
                        .userDetailsService(customUserDetailsService)
                        .useSecureCookie(false));

        // [保持不变] JWT 过滤器仍然需要在这里, 用于 "rememberMe" 和已登录的会话
        http.addFilterBefore(jwtCookieAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}