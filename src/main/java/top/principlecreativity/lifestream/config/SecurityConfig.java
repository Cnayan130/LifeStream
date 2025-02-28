package top.principlecreativity.lifestream.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
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

    // 添加新的过滤器链来专门处理profile页面
    @Bean
    @Order(1) // 最高优先级，确保优先处理profile请求
    public SecurityFilterChain profileFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/profile/**") // 只匹配个人资料路径
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()) // 先允许所有请求通过，由后续过滤器处理认证
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 记录访问日志
                            System.out.println("Profile访问 - 路径: " + request.getRequestURI() +
                                    ", 认证状态: " + (request.getUserPrincipal() != null));

                            // 对于AJAX请求返回401
                            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            } else {
                                // 否则重定向到登录页面
                                String returnUrl = request.getRequestURI();
                                response.sendRedirect("/login?returnUrl=" +
                                        URLEncoder.encode(returnUrl, StandardCharsets.UTF_8));
                            }
                        }));

        // 首先添加Cookie认证过滤器，然后是JWT认证过滤器
        http.addFilterBefore(jwtCookieAuthenticationFilter(), BasicAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2) // API过滤器链
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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

                        // 其他所有API请求需要认证
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

        // 先添加Cookie认证过滤器，然后是JWT认证过滤器
        http.addFilterBefore(jwtCookieAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(3) // 所有其他请求的过滤器链
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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

                        // 静态资源
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/static/**").permitAll()
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers("/css/**").permitAll()
                        .requestMatchers("/js/**").permitAll()
                        .requestMatchers("/assets/**").permitAll()
                        .requestMatchers("/fonts/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()

                        // 错误处理
                        .requestMatchers("/error/**").permitAll()

                        // 确保profile页面是公开的，由专门的过滤器链处理认证
                        .requestMatchers("/profile/**").permitAll()

                        // 需要认证的功能
                        .requestMatchers("/dashboard/**").authenticated()
                        //.requestMatchers("/profile/**").authenticated() // 注释掉，由专用过滤器链处理
                        .requestMatchers("/posts/new", "/posts/*/edit").authenticated()
                        .requestMatchers("/albums/new", "/albums/*/edit").authenticated()

                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 记录访问日志
                            System.out.println("Web访问 - 路径: " + request.getRequestURI() +
                                    ", 认证状态: " + (request.getUserPrincipal() != null));

                            // 如果是AJAX请求返回401
                            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            } else {
                                // 否则重定向到登录页面
                                String returnUrl = request.getRequestURI();
                                response.sendRedirect("/login?returnUrl=" +
                                        URLEncoder.encode(returnUrl, StandardCharsets.UTF_8));
                            }
                        })
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true) // 强制始终重定向到首页
                        .successHandler(authenticationSuccessHandler()) // 自定义成功处理器
                        .permitAll())
                .rememberMe(remember -> remember
                        .key("lifeStreamSecretKey")
                        .tokenValiditySeconds(2592000)  // 30天有效期
                        .rememberMeParameter("remember-me")
                        .userDetailsService(customUserDetailsService)
                        .useSecureCookie(false));

        // 先添加Cookie认证过滤器，然后是JWT认证过滤器
        http.addFilterBefore(jwtCookieAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 静态资源处理
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
                registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
                registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
                registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
                registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/favicon.ico");
            }
        };
    }
}