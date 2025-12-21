package top.principlecreativity.lifestream.controller.api;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.payload.*;
import top.principlecreativity.lifestream.security.CurrentUser;
import top.principlecreativity.lifestream.security.JwtTokenProvider;
import top.principlecreativity.lifestream.security.UserPrincipal;
import top.principlecreativity.lifestream.service.UserService;

import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        // 1. 执行认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. 生成 JWT
        String jwt = tokenProvider.generateToken(authentication);

        ResponseCookie cookie = ResponseCookie.from("auth_token", jwt)
                .httpOnly(true)
                .secure(true) // 生产环境必开
                .path("/")
                .maxAge(Duration.ofDays(30))
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 4. 返回 JSON (给 App/前端 JS 逻辑用)
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt, userService.convertToUserSummary(userPrincipal)));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        if (userService.existsByUsername(signUpRequest.getUsername())) {
            return new ResponseEntity<>(new ApiResponse(false, "用户名已被占用"), HttpStatus.BAD_REQUEST);
        }

        if (userService.existsByEmail(signUpRequest.getEmail())) {
            return new ResponseEntity<>(new ApiResponse(false, "邮箱地址已被注册"), HttpStatus.BAD_REQUEST);
        }

        // 创建用户
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(signUpRequest.getPassword()); // 确保 UserService 内部进行了加密！

        User result = userService.createUser(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/users/{username}")
                .buildAndExpand(result.getUsername()).toUri();

        return ResponseEntity.created(location).body(new ApiResponse(true, "用户注册成功"));
    }

    // 【路由修正】去掉重复的 /api/auth 前缀，现在访问路径是 GET /api/auth/validate
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@CurrentUser UserPrincipal currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse(false, "无效的 Token 或用户未登录"));
        }

        // 这是一个很好的实践：从数据库刷新用户状态，而不是只依赖 Token 里的旧数据
        User user = userService.getUserById(currentUser.getId());
        UserSummary userSummary = new UserSummary(user.getId(), user.getUsername(), user.getEmail(), user.getAvatarUrl());

        return ResponseEntity.ok(userSummary);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // 1. 显式清理 Security 上下文（2025 健壮性实践）
        SecurityContextHolder.clearContext();

        // 2. 使用 ResponseCookie 构建“清除令牌”（匹配 signin 中的所有安全属性）
        ResponseCookie deleteCookie = ResponseCookie.from("auth_token", "")
                .path("/")
                .httpOnly(true)
                .secure(true)        // 2025 年生产环境强制开启
                .sameSite("Strict")  // 防止 CSRF 的关键
                .maxAge(0)           // 立即过期
                .build();

        // 3. 通过 ResponseEntity 返回，不直接操作 HttpServletResponse
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(new ApiResponse(true, "注销成功"));
    }
}