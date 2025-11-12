package top.principlecreativity.lifestream.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.principlecreativity.lifestream.entity.Album;
import top.principlecreativity.lifestream.entity.Image;
import top.principlecreativity.lifestream.entity.Post;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.security.CurrentUser;
import top.principlecreativity.lifestream.security.UserPrincipal;
import top.principlecreativity.lifestream.service.AlbumService;
import top.principlecreativity.lifestream.service.FileStorageService;
import top.principlecreativity.lifestream.service.PostService;
import top.principlecreativity.lifestream.service.UserService;
import top.principlecreativity.lifestream.util.AppConstants;
import top.principlecreativity.lifestream.util.ImageResizeUtil; // [新增] 导入工具类

import java.io.ByteArrayInputStream; // [新增]
import java.io.InputStream; // [新增]
import java.io.IOException; // [新增]

@Controller
@RequestMapping("/profile")
public class WebProfileController {

    // --- 依赖注入 ---
    @Autowired private UserService userService;
    @Autowired private PostService postService;
    @Autowired private AlbumService albumService;
    @Autowired private FileStorageService fileStorageService;

    // --- GET Mappings (页面渲染) ---

    /**
     * [修复] 根路径 /profile 重定向
     * 自动跳转到用户的设置页面。
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String rootProfileRedirect() {
        return "redirect:/profile/settings";
    }

    /**
     * [核心] 私有设置中心
     * 路径: GET /profile/settings
     * 模板: "profile/settings.html"
     */
    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public String settingsPage(@CurrentUser UserPrincipal currentUser, Model model) {
        User user = userService.getUserById(currentUser.getId());
        model.addAttribute("user", user);
        return "profile/settings";
    }

    /**
     * [核心] 公开展示页
     * 路径: GET /profile/{username}
     * 模板: "profile/view.html"
     */
    @GetMapping("/{username}")
    public String viewProfile(
            @PathVariable String username,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @CurrentUser UserPrincipal currentUser,
            Model model) {

        User user = userService.getUserByUsername(username);
        model.addAttribute("user", user);

        // 检查是否为用户自己的主页
        boolean isOwnProfile = currentUser != null && username.equals(currentUser.getUsername());
        model.addAttribute("isOwnProfile", isOwnProfile);

        // 加载文章
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts = isOwnProfile ?
                postService.getPostsByAuthor(user, pageable) :
                postService.getVisiblePosts(user, currentUser, pageable);

        model.addAttribute("posts", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());

        // 加载相册和统计数据
        model.addAttribute("albums", albumService.getAlbumsByUser(user, PageRequest.of(0, 4, Sort.Direction.DESC, "createdAt")).getContent());
        model.addAttribute("albumCount", albumService.countAlbumsByUser(user));
        model.addAttribute("postCount", postService.countPostsByUser(user));
        model.addAttribute("imageCount", fileStorageService.countImagesByUser(user)); // 假设你有这个方法

        return "profile/view";
    }

    // --- POST Mappings (表单处理) ---

    /**
     * [重构] 处理个人资料更新
     * 路径: POST /profile/edit
     * [新增] 现在会调用 ImageResizeUtil 来处理头像
     */
    @PostMapping("/edit")
    @PreAuthorize("isAuthenticated()")
    public String updateProfile(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam("username") String username,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            RedirectAttributes redirectAttributes) {

        try {
            User user = userService.getUserById(currentUser.getId());

            // 1. 验证用户名
            if (!user.getUsername().equals(username)) {
                if (userService.existsByUsername(username)) {
                    redirectAttributes.addFlashAttribute("error", "用户名已被占用");
                    return "redirect:/profile/settings#nav-profile-pane";
                }
                user.setUsername(username);
            }
            user.setBio(bio);

            // 2. [核心] 处理头像 (剪裁 + 缩放)
            if (avatarFile != null && !avatarFile.isEmpty()) {
                // [注意] 你的 FileStorageService.storeAvatar 现在应该
                // 已经在内部调用了 ImageResizeUtil.resizeImage
                // 所以我们这里只需要直接调用它即可

                Image avatar = fileStorageService.storeAvatar(avatarFile, user);

                // [待办] 确保你的 Image 实体有 getUrl() 或 getId() 方法
                // 这里的URL格式 /api/images/download/{id} 是一个示例
                user.setAvatarUrl("/api/images/download/" + avatar.getId());

            }

            // 3. 保存用户
            userService.updateUser(user.getId(), user);

            // 4. 刷新Spring Security的认证信息
            UserPrincipal updatedPrincipal = UserPrincipal.create(user);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(updatedPrincipal, null, updatedPrincipal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            redirectAttributes.addFlashAttribute("success", "个人资料已成功更新！");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "更新资料失败: " + e.getMessage());
        }

        return "redirect:/profile/settings#nav-profile-pane";
    }

    /**
     * [重构] 处理密码更改
     * 路径: POST /profile/change-password
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        // 验证新密码
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", "新密码与确认密码不匹配");
            return "redirect:/profile/settings#nav-password-pane";
        }

        try {
            userService.changePassword(currentUser.getId(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("passwordSuccess", "密码已成功更改");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }

        return "redirect:/profile/settings#nav-password-pane";
    }
}