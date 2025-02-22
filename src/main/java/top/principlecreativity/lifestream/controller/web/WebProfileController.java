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

@Controller
@RequestMapping("/profile")
public class WebProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String viewOwnProfile(@CurrentUser UserPrincipal currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        return "redirect:/profile/" + currentUser.getUsername();
    }

    @GetMapping("/{username}")
    public String viewProfile(
            @PathVariable String username,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @CurrentUser UserPrincipal currentUser,
            Model model) {

        User user = userService.getUserByUsername(username);
        model.addAttribute("user", user);

        // Check if the profile belongs to the current user
        boolean isOwnProfile = currentUser != null && username.equals(currentUser.getUsername());
        model.addAttribute("isOwnProfile", isOwnProfile);

        // Get posts
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts;

        if (isOwnProfile) {
            // Show all posts (including drafts) to the owner
            posts = postService.getPostsByAuthor(user, pageable);
        } else {
            // Show only published posts to visitors
            posts = postService.getVisiblePosts(user, currentUser, pageable);
        }

        model.addAttribute("posts", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());

        // Get recent albums
        Page<Album> albums = albumService.getAlbumsByUser(user, PageRequest.of(0, 4, Sort.Direction.DESC, "createdAt"));
        model.addAttribute("albums", albums.getContent());
        model.addAttribute("albumCount", albumService.countAlbumsByUser(user));

        // Get statistics
        model.addAttribute("postCount", postService.countPostsByUser(user));
        model.addAttribute("imageCount", fileStorageService.countImagesByUser(user));

        return "profile/view";
    }

    @GetMapping("/edit")
    @PreAuthorize("isAuthenticated()")
    public String editProfileForm(@CurrentUser UserPrincipal currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        User user = userService.getUserById(currentUser.getId());
        model.addAttribute("user", user);

        return "profile/edit.html";
    }

    @PostMapping("/edit")
    @PreAuthorize("isAuthenticated()")
    public String updateProfile(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam("username") String username,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            Model model) {

        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            User user = userService.getUserById(currentUser.getId());

            // Check if username is changed and available
            if (!user.getUsername().equals(username)) {
                if (userService.existsByUsername(username)) {
                    model.addAttribute("error", "用户名已被占用");
                    model.addAttribute("user", user);
                    return "profile/edit.html";
                }
                user.setUsername(username);
            }

            // Update bio
            user.setBio(bio);

            // Handle avatar upload
            if (avatarFile != null && !avatarFile.isEmpty()) {
                Image avatar = fileStorageService.storeAvatar(avatarFile, user);
                user.setAvatarUrl("/api/images/download/" + avatar.getId());

                // 保存用户后刷新认证会话
                userService.updateUser(user.getId(), user);

                // 刷新当前认证信息
                UserPrincipal updatedPrincipal = UserPrincipal.create(user);
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        updatedPrincipal,
                        null,
                        updatedPrincipal.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(newAuth);

                return "redirect:/profile/" + user.getUsername();
            }

            userService.updateUser(user.getId(), user);
            return "redirect:/profile/" + user.getUsername();

        } catch (Exception e) {
            model.addAttribute("error", "更新资料失败: " + e.getMessage());
            model.addAttribute("user", userService.getUserById(currentUser.getId()));
            return "profile/edit.html";
        }
    }

    @GetMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePasswordForm(@CurrentUser UserPrincipal currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        return "profile/change-password";
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "新密码与确认密码不匹配");
            return "profile/change-password";
        }

        try {
            userService.changePassword(currentUser.getId(), oldPassword, newPassword);
            model.addAttribute("success", "密码已成功更改");
            return "profile/change-password";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "profile/change-password";
        }
    }
}
