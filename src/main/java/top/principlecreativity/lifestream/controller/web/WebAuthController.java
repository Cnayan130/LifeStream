package top.principlecreativity.lifestream.controller.web;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.payload.SignUpRequest;
import top.principlecreativity.lifestream.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class WebAuthController {

    private static final Logger logger = LoggerFactory.getLogger(WebAuthController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new SignUpRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") SignUpRequest signUpRequest,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        logger.info("处理用户注册请求: {}", signUpRequest.getUsername());

        // 验证检查
        if (bindingResult.hasErrors()) {
            logger.warn("表单验证失败: {}", bindingResult.getAllErrors());
            return "auth/register";
        }

        // 检查用户名是否已被占用
        if (userService.existsByUsername(signUpRequest.getUsername())) {
            logger.warn("用户名已被占用: {}", signUpRequest.getUsername());
            model.addAttribute("error", "用户名已被占用！");
            return "auth/register";
        }

        // 检查邮箱是否已被注册
        if (userService.existsByEmail(signUpRequest.getEmail())) {
            logger.warn("邮箱已被注册: {}", signUpRequest.getEmail());
            model.addAttribute("error", "该邮箱已被注册！");
            return "auth/register";
        }

        // 创建用户
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(signUpRequest.getPassword());

        try {
            logger.info("开始创建用户: {}", signUpRequest.getUsername());
            userService.createUser(user);
            logger.info("用户创建成功: {}", signUpRequest.getUsername());
            redirectAttributes.addFlashAttribute("success", "注册成功！您现在可以登录了。");
            return "redirect:/login";
        } catch (Exception e) {
            logger.error("用户注册失败: {}", e.getMessage(), e);
            model.addAttribute("error", "注册失败：" + e.getMessage());
            model.addAttribute("user", signUpRequest);
            return "auth/register";
        }
    }

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