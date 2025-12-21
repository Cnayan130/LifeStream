package top.principlecreativity.lifestream.config;

import jakarta.servlet.http.Cookie;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import top.principlecreativity.lifestream.security.CustomUserDetailsService;
import top.principlecreativity.lifestream.security.JwtAuthenticationEntryPoint;
import top.principlecreativity.lifestream.security.JwtAuthenticationFilter;
import top.principlecreativity.lifestream.security.JwtTokenProvider;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider tokenProvider;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final PasswordEncoder passwordEncoder; // 注入 AppConfig 中定义的 Bean

    // 构造注入：Spring 会自动从 AppConfig 找到 PasswordEncoder 注入进来
    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          JwtTokenProvider tokenProvider,
                          JwtAuthenticationEntryPoint unauthorizedHandler,
                          PasswordEncoder passwordEncoder) {
        this.customUserDetailsService = customUserDetailsService;
        this.tokenProvider = tokenProvider;
        this.unauthorizedHandler = unauthorizedHandler;
        this.passwordEncoder = passwordEncoder;
    }

    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * 【登录成功处理器】
     * 负责生成 JWT 并写入 HttpOnly Cookie
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String token = tokenProvider.generateToken(authentication);

            // 创建 JWT Cookie
            Cookie cookie = new Cookie("auth_token", token);
            cookie.setPath("/");
            cookie.setHttpOnly(true); // 防 XSS 关键
            cookie.setMaxAge(86400 * 30); // 30天过期

            response.addCookie(cookie);

            // 重定向回首页
            response.sendRedirect("/");
        };
    }

    /**
     * 【认证提供者】
     * 关联 UserDetailsService 和 PasswordEncoder
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * 【认证管理器】
     * 登录接口需要用到它来手动触发认证
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // =========================================================
    //                      安全过滤链配置
    // =========================================================

    // [链条 1]: 静态资源 (无需鉴权，完全放行)
    @Bean
    @Order(0)
    public SecurityFilterChain staticResourceFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(PathRequest.toStaticResources().atCommonLocations())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    // [链条 2]: API 接口 (给 AJAX 或 移动端使用)
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                // 策略：如果你的 API 会被浏览器 AJAX 调用（且依赖 Cookie），则必须开启 CSRF。
                // 客户端 JS 需要从 Cookie 读取 XSRF-TOKEN 并放到 Header 中。
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                // API 保持无状态 (不创建 HttpSession)，认证完全依赖 JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // 登录注册接口
                        .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/albums/**", "/api/users/**", "/api/tags/**").permitAll() // 公开读接口
                        .requestMatchers(HttpMethod.GET, "/api/images/download/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 插入统一的 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // [链条 3]: Web 网页 (Thymeleaf 服务端渲染)
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                )
                // Web 页面建议 IF_REQUIRED，保证 CSRF Token 和 错误消息 FlashMap 能正常工作
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index", "/login", "/register", "/search", "/about", "/terms").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/api/images/download/**").permitAll()

                        // 需要权限的页面
                        .requestMatchers("/posts/new", "/posts/*/edit").authenticated()
                        .requestMatchers("/albums/new", "/albums/*/edit").authenticated()
                        .requestMatchers("/profile/**", "/dashboard/**").authenticated()

                        // 公开的详情页
                        .requestMatchers(HttpMethod.GET, "/posts/**", "/albums/**").permitAll()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login") // 指向你的 Controller @GetMapping("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler()) // 登录成功写 Cookie
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .deleteCookies("auth_token", "JSESSIONID", "XSRF-TOKEN")
                        .invalidateHttpSession(true) // 确保清理 Session
                        .clearAuthentication(true)
                        .logoutSuccessUrl("/")
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}