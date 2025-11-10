package top.principlecreativity.lifestream.controller.web;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.payload.RegistrationFormDto;
import top.principlecreativity.lifestream.service.UserService;

@Controller
public class WebAuthController {

    private static final Logger logger = LoggerFactory.getLogger(WebAuthController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login"; // 模板: resources/templates/auth/login.html
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        // 关键修复 1:
        // 网页表单 (th:object) 必须绑定到 RegistrationFormDto,
        // 并且绑定的名字 "userDto" 必须和 @ModelAttribute 的名字一致
        if (!model.containsAttribute("userDto")) {
            model.addAttribute("userDto", new RegistrationFormDto());
        }
        return "auth/register"; // 模板: resources/templates/auth/register.html
    }

    @PostMapping("/register")
    public String registerUser(
            // 关键修复 2: 接收的对象改为 RegistrationFormDto
            @Valid @ModelAttribute("userDto") RegistrationFormDto userDto,
            BindingResult bindingResult, // 移除 Model, Spring 会自动把 bindingResult 放回
            RedirectAttributes redirectAttributes) {

        logger.info("处理网页注册请求: {}", userDto.getUsername());

        // 1. 检查 @Valid 基础验证 (如 @NotEmpty, @Email)
        if (bindingResult.hasErrors()) {
            logger.warn("表单验证失败: {}", bindingResult.getAllErrors());
            // 不需要 model.addAttribute, Thymeleaf 会自动从 bindingResult 中获取错误
            return "auth/register";
        }

        // 2. 检查密码是否匹配
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            // (这是我之前建议的逻辑，比 model.addAttribute 更好)
            bindingResult.rejectValue("confirmPassword", "Mismatch.confirmPassword", "两次输入的密码不一致");
        }

        // 3. 检查用户名是否已被占用
        if (userService.existsByUsername(userDto.getUsername())) {
            bindingResult.rejectValue("username", "Exists.username", "该用户名已被占用");
        }

        // 4. 检查邮箱是否已被注册
        if (userService.existsByEmail(userDto.getEmail())) {
            bindingResult.rejectValue("email", "Exists.email", "该邮箱已被注册");
        }

        // 5. 再次检查是否有任何(业务)错误
        if (bindingResult.hasErrors()) {
            logger.warn("业务逻辑验证失败");
            return "auth/register"; // 返回页面，Thymeleaf 会自动显示所有错误
        }

        // 6. 所有验证通过，创建用户
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(userDto.getPassword()); // UserService 会负责加密

        try {
            logger.info("开始创建用户: {}", userDto.getUsername());
            userService.createUser(user);
            logger.info("用户创建成功: {}", userDto.getUsername());
            redirectAttributes.addFlashAttribute("success", "注册成功！您现在可以登录了。");
            return "redirect:/login"; // 重定向到登录页
        } catch (Exception e) {
            logger.error("用户注册失败: {}", e.getMessage(), e);
            // 关键修复 3: 使用 bindingResult.reject() 来添加全局错误
            bindingResult.reject("Global.register.error", "注册时发生未知错误，请稍后再试。");
            return "auth/register";
        }
    }

    // --- (以下方法保持不变，它们很好) ---

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @GetMapping("/terms")
    public String showTerms() {
        return "terms";
    }

    @GetMapping("/privacy")
    public String showPrivacy() {
        return "privacy";
    }
}