package top.principlecreativity.lifestream.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.principlecreativity.lifestream.security.CustomUserDetailsService;
import top.principlecreativity.lifestream.security.JwtAuthenticationEntryPoint;
import top.principlecreativity.lifestream.security.JwtAuthenticationFilter;
import top.principlecreativity.lifestream.security.JwtTokenProvider;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true
)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
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
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080")); // 添加本地前端地址
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(List.of("x-auth-token"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Order(2) // 确保这个过滤器链在web表单过滤器链之后处理
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

                        // 需要认证的API
                        .requestMatchers("/api/images/upload").authenticated()
                        .requestMatchers("/api/user/**").authenticated()

                        // 其他所有API请求需要认证
                        .anyRequest().authenticated());

        // JWT认证过滤器
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(3) // 低优先级过滤器链，处理所有其他请求
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
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

                        // 需要认证的功能
                        .requestMatchers("/dashboard/**").authenticated()
                        .requestMatchers("/profile/**").authenticated()
                        .requestMatchers("/posts/new", "/posts/*/edit").authenticated()
                        .requestMatchers("/albums/new", "/albums/*/edit").authenticated()

                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true) // 强制始终重定向到首页
                        .successHandler(authenticationSuccessHandler()) // 自定义成功处理器
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/"))
                .rememberMe(remember -> remember
                        .key("lifeStreamSecretKey")  // 使用强随机密钥，生产环境建议使用更复杂的值
                        .tokenValiditySeconds(2592000)  // 30天有效期
                        .rememberMeParameter("remember-me")  // 表单参数名称
                        .userDetailsService(customUserDetailsService)  // 自定义用户服务
                        .useSecureCookie(false));  // 开发环境设为false，生产环境设为true

        // JWT认证过滤器
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 如果你使用WebMvcConfigurer自定义静态资源映射，请确保这些路径一致
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