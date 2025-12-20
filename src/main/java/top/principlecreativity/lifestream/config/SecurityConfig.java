package top.principlecreativity.lifestream.config;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import jakarta.servlet.http.Cookie;
import top.principlecreativity.lifestream.security.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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

    // ... (AuthenticationSuccessHandler, PasswordEncoder 等 Bean 保持不变，此处省略以节省篇幅) ...
    // 请保留你原有的 authenticationSuccessHandler, jwtAuthenticationFilter, passwordEncoder 等 Bean 定义

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String token = tokenProvider.generateToken(authentication);
            Cookie cookie = new Cookie("auth_token", token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(86400 * 30);
            response.addCookie(cookie);
            response.sendRedirect("/");
        };
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
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService); // 无参构造
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


    // [链条 1]: 静态资源 (保持不变)
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

    // [链条 2]: API 接口
    // 修正：API 如果被浏览器访问（AJAX），且使用 Cookie 鉴权，必须开启 CSRF
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                // 【重要修正】开启 CSRF，并使用 Cookie 存储 Token，允许 JS 读取
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                // API 依然可以是无状态的，因为我们不依赖 Session，而是依赖 JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // 登录接口不需要 CSRF/Auth
                        .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/albums/**", "/api/users/**", "/api/tags/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/images/download/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // [链条 3]: Web 网页
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                // 【重要修正】开启 CSRF，否则 Thymeleaf 无法渲染 _csrf 标签
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                // 【重要修正】Web 页面建议使用 IF_REQUIRED，完全无状态会影响 Thymeleaf 错误提示等功能
                // 虽然我们主要靠 JWT Cookie，但为了兼容性，不要强制 STATELESS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index", "/login", "/register", "/search", "/about", "/terms").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/api/images/download/**").permitAll() // 确保 Web 页面也能加载图片

                        // 需要登录的页面
                        .requestMatchers("/posts/new", "/posts/*/edit").authenticated()
                        .requestMatchers("/albums/new", "/albums/*/edit").authenticated()
                        .requestMatchers("/profile/**", "/dashboard/**").authenticated()

                        // 公开的 GET 页面
                        .requestMatchers(HttpMethod.GET, "/posts/**", "/albums/**").permitAll()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .deleteCookies("auth_token", "JSESSIONID", "XSRF-TOKEN") // 删除 CSRF Cookie
                        .logoutSuccessUrl("/")
                )
                // 确保 JWT Cookie 过滤器在 UsernamePasswordAuthenticationFilter 之前执行
                .addFilterBefore(jwtCookieAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}