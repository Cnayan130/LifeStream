package top.principlecreativity.lifestream.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import top.principlecreativity.lifestream.entity.Post;
import top.principlecreativity.lifestream.entity.Tag;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.security.CurrentUser;
import top.principlecreativity.lifestream.security.UserPrincipal;
import top.principlecreativity.lifestream.service.PostService;
import top.principlecreativity.lifestream.service.TagService;
import top.principlecreativity.lifestream.service.UserService;
import top.principlecreativity.lifestream.util.AppConstants;

import java.util.List;

@Controller
public class WebPostController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private TagService tagService;

    @GetMapping("/posts")
    public String getAllPosts(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts = postService.getAllPublishedPosts(pageable);

        model.addAttribute("postPage", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("paginationUrl", "/posts");
        model.addAttribute("pageTitle", "全部文章");
        model.addAttribute("pageDescription", "浏览博客中的全部文章内容，按时间倒序排列");

        // 添加侧边栏数据
        addSidebarAttributes(model);

        return "posts/list";
    }

    @GetMapping("/posts/{id}")
    public String getPostById(@PathVariable Long id, Model model, @CurrentUser UserPrincipal currentUser) {
        Post post = postService.getPostById(id);

        model.addAttribute("post", post);

        // 检查当前用户是否是作者
        boolean isAuthor = false;
        if (currentUser != null && post.getAuthor() != null) {
            isAuthor = post.getAuthor().getId().equals(currentUser.getId());
        }
        model.addAttribute("isAuthor", isAuthor);

        // 获取上一篇和下一篇文章
        Post previousPost = postService.getPreviousPost(post);
        Post nextPost = postService.getNextPost(post);
        model.addAttribute("previousPost", previousPost);
        model.addAttribute("nextPost", nextPost);

        // 获取相关文章
        List<Post> relatedPosts = postService.getRelatedPosts(post, 5);
        model.addAttribute("relatedPosts", relatedPosts);

        // 添加侧边栏数据
        addSidebarAttributes(model);

        return "posts/detail";
    }

    @GetMapping("/posts/tag/{tag}")
    public String getPostsByTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts = postService.getPostsByTag(tag, pageable);

        model.addAttribute("postPage", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("paginationUrl", "/posts/tag/" + tag);
        model.addAttribute("pageTitle", "标签: " + tag);
        model.addAttribute("pageDescription", "查看标签 \"" + tag + "\" 下的所有文章");
        model.addAttribute("searchKeyword", null);

        // 添加侧边栏数据
        addSidebarAttributes(model);

        return "posts/list";
    }

    @GetMapping("/posts/user/{username}")
    public String getPostsByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            Model model) {

        User user = userService.getUserByUsername(username);
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts = postService.getPostsByAuthor(user, pageable);

        model.addAttribute("postPage", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("paginationUrl", "/posts/user/" + username);
        model.addAttribute("pageTitle", user.getUsername() + " 的文章");
        model.addAttribute("pageDescription", "查看用户 " + user.getUsername() + " 发布的所有文章");
        model.addAttribute("author", user);

        // 添加侧边栏数据
        addSidebarAttributes(model);

        return "posts/list";
    }

    @GetMapping("/posts/archives/{year}/{month}")
    public String getPostsByYearAndMonth(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts = postService.getPostsByYearAndMonth(year, month, pageable);

        model.addAttribute("postPage", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("paginationUrl", "/posts/archives/" + year + "/" + month);

        // 获取月份名称
        String[] months = {"一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"};
        String monthName = months[month - 1];

        model.addAttribute("pageTitle", year + "年" + monthName + "的文章");
        model.addAttribute("pageDescription", "查看" + year + "年" + monthName + "发布的所有文章");

        // 添加侧边栏数据
        addSidebarAttributes(model);

        return "posts/list";
    }

    @GetMapping("/posts/search")
    public String searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Post> posts = postService.searchPosts(keyword, pageable);

        model.addAttribute("postPage", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("paginationUrl", "/posts/search?keyword=" + keyword);
        model.addAttribute("pageTitle", "搜索结果: " + keyword);
        model.addAttribute("pageDescription", "关键词 \"" + keyword + "\" 的搜索结果");
        model.addAttribute("searchKeyword", keyword);

        // 添加侧边栏数据
        addSidebarAttributes(model);

        return "posts/list";
    }

    @GetMapping("/posts/new")
    public String newPostForm(Model model) {
        model.addAttribute("post", null);
        model.addAttribute("isEdit", false);
        return "posts/form";
    }

    @GetMapping("/posts/{id}/edit")
    public String editPostForm(@PathVariable Long id, Model model, @CurrentUser UserPrincipal currentUser) {
        Post post = postService.getPostById(id);

        // 检查当前用户是否是作者
        if (currentUser == null || !post.getAuthor().getId().equals(currentUser.getId())) {
            return "redirect:/error/403";
        }

        model.addAttribute("post", post);
        model.addAttribute("isEdit", true);
        return "posts/form";
    }

    // 辅助方法: 添加侧边栏所需的属性
    private void addSidebarAttributes(Model model) {
        // 最近文章
        List<Post> recentPosts = postService.getRecentPosts(5);
        model.addAttribute("recentPosts", recentPosts);

        // 热门标签
        List<Tag> popularTags = tagService.getPopularTags(10);
        model.addAttribute("popularTags", popularTags);

        // 归档年份
        List<Integer> archiveYears = postService.getAllPublishedYears();
        model.addAttribute("archiveYears", archiveYears);
    }
}