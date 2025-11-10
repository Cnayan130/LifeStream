package top.principlecreativity.lifestream.controller.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import top.principlecreativity.lifestream.service.LogService;
import top.principlecreativity.lifestream.service.UserService;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private LogService logService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private final Logger logger = LogManager.getLogger(AuthController.class);

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);

        // 将令牌设置到Cookie中
        Cookie authCookie = new Cookie("auth_token", jwt);
        authCookie.setPath("/");
        authCookie.setHttpOnly(false); // 允许JavaScript访问，因为前端需要使用
        authCookie.setMaxAge(86400); // 24小时有效期，与JWT一致
        // 在生产环境应设置secure=true
        // authCookie.setSecure(true);

        response.addCookie(authCookie);

        // 添加用户数据到响应
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt, userService.convertToUserSummary(userPrincipal)));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        if (userService.existsByUsername(signUpRequest.getUsername())) {
            return new ResponseEntity<>(new ApiResponse(false, "Username is already taken!"),
                    HttpStatus.BAD_REQUEST);
        }

        if (userService.existsByEmail(signUpRequest.getEmail())) {
            return new ResponseEntity<>(new ApiResponse(false, "Email Address already in use!"),
                    HttpStatus.BAD_REQUEST);
        }

        // Creating user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(signUpRequest.getPassword());

        User result = userService.createUser(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/users/{username}")
                .buildAndExpand(result.getUsername()).toUri();

        return ResponseEntity.created(location).body(new ApiResponse(true, "User registered successfully"));
    }

    @GetMapping("/api/auth/validate")
    public ResponseEntity<?> validateToken(@CurrentUser UserPrincipal currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse(false, "Invalid token"));
        }

        User user = userService.getUserById(currentUser.getId());
        // 返回用户信息但不包含敏感数据
        UserSummary userSummary = new UserSummary(user.getId(), user.getUsername(), user.getEmail(), user.getAvatarUrl());
        return ResponseEntity.ok(userSummary);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletResponse response) {
        // 清除Cookie
        Cookie authCookie = new Cookie("auth_token", null);
        authCookie.setPath("/");
        authCookie.setMaxAge(0);
        response.addCookie(authCookie);

        return ResponseEntity.ok().build();
    }
}