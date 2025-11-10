package top.principlecreativity.lifestream.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.principlecreativity.lifestream.service.UserService;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/user") // 匹配 JS 请求的 /api/user 路径
public class UserCheckController {

    @Autowired
    private UserService userService; // 复用你的 UserService

    // 对应 JS: /api/user/checkUsernameAvailability?username=...
    @GetMapping("/checkUsernameAvailability")
    public ResponseEntity<?> checkUsernameAvailability(@RequestParam("username") String username) {
        boolean isAvailable = !userService.existsByUsername(username);

        // JS 期望的响应格式 { available: true/false }
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", isAvailable);

        // （可选）可以返回更详细的
        // Map<String, Object> response = new HashMap<>();
        // response.put("available", false);
        // response.put("message", "该用户名已被占用");

        return ResponseEntity.ok(response);
    }

    // 对应 JS: /api/user/checkEmailAvailability?email=...
    @GetMapping("/checkEmailAvailability")
    public ResponseEntity<?> checkEmailAvailability(@RequestParam("email") String email) {
        boolean isAvailable = !userService.existsByEmail(email);

        Map<String, Boolean> response = new HashMap<>();
        response.put("available", isAvailable);

        return ResponseEntity.ok(response);
    }
}
