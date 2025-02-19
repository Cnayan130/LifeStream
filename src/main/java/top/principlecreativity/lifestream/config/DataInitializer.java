package top.principlecreativity.lifestream.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.principlecreativity.lifestream.entity.Post;
import top.principlecreativity.lifestream.entity.Role;
import top.principlecreativity.lifestream.entity.Tag;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.repository.PostRepository;
import top.principlecreativity.lifestream.repository.RoleRepository;
import top.principlecreativity.lifestream.repository.TagRepository;
import top.principlecreativity.lifestream.repository.UserRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Configuration
@Profile("dev") // 仅在开发环境中启用
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // 初始化角色
            initRoles();

            // 初始化用户
            User admin = initAdmin();
            User normalUser = initNormalUser();

            // 初始化标签
            Set<Tag> tags = initTags();

            // 初始化文章
            initPosts(admin, normalUser, tags);

            System.out.println("测试数据初始化完成");
        };
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            Role adminRole = new Role();
            adminRole.setName(Role.ERole.ROLE_ADMIN);
            roleRepository.save(adminRole);

            Role moderatorRole = new Role();
            moderatorRole.setName(Role.ERole.ROLE_MODERATOR);
            roleRepository.save(moderatorRole);

            Role userRole = new Role();
            userRole.setName(Role.ERole.ROLE_USER);
            roleRepository.save(userRole);

            System.out.println("角色初始化完成");
        }
    }

    private User initAdmin() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setBio("系统管理员，负责博客管理和内容审核。");
            admin.setAvatarUrl("https://secure.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y");

            Role adminRole = roleRepository.findByName(Role.ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("角色未找到"));
            admin.setRoles(Collections.singleton(adminRole));

            return userRepository.save(admin);
        } else {
            return userRepository.findByUsername("admin").get();
        }
    }

    private User initNormalUser() {
        if (userRepository.findByUsername("user").isEmpty()) {
            User user = new User();
            user.setUsername("user");
            user.setEmail("user@example.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setBio("我是一个普通用户，喜欢分享生活和技术心得。");
            user.setAvatarUrl("https://secure.gravatar.com/avatar/11111111111111111111111111111111?d=mp&f=y");

            Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("角色未找到"));
            user.setRoles(Collections.singleton(userRole));

            return userRepository.save(user);
        } else {
            return userRepository.findByUsername("user").get();
        }
    }

    private Set<Tag> initTags() {
        Set<Tag> tags = new HashSet<>();

        if (tagRepository.count() == 0) {
            String[] tagNames = {"Java", "Spring Boot", "前端", "后端", "数据库", "教程", "笔记", "心得", "生活", "摄影"};

            for (String name : tagNames) {
                Tag tag = new Tag();
                tag.setName(name);
                Tag savedTag = tagRepository.save(tag);
                tags.add(savedTag);
            }

            System.out.println("标签初始化完成");
        } else {
            tagRepository.findAll().forEach(tags::add);
        }

        return tags;
    }

    private void initPosts(User admin, User normalUser, Set<Tag> allTags) {
        if (postRepository.count() == 0) {
            // 管理员的文章
            createPost(
                    admin,
                    "Spring Boot 入门教程",
                    "这是一篇 Spring Boot 入门教程，介绍了 Spring Boot 的基本概念和使用方法。",
                    "<h2>什么是 Spring Boot？</h2><p>Spring Boot 是一个基于 Spring 框架的快速开发工具，它提供了一种更简单的方式来配置和运行应用程序。</p>" +
                            "<h2>为什么使用 Spring Boot？</h2><p>Spring Boot 可以帮助您更快速地创建生产级别的应用程序，减少配置的复杂性，提高开发效率。</p>" +
                            "<h2>Spring Boot 的核心特性</h2><ul><li>自动配置</li><li>起步依赖</li><li>Actuator</li><li>外部化配置</li></ul>" +
                            "<h2>如何开始？</h2><p>您可以使用 Spring Initializr 创建一个新的 Spring Boot 项目，或者使用 Maven/Gradle 手动配置。</p>" +
                            "<h3>示例代码</h3><pre><code class=\"language-java\">@SpringBootApplication\npublic class MyApplication {\n    public static void main(String[] args) {\n        SpringApplication.run(MyApplication.class, args);\n    }\n}</code></pre>",
                    "https://spring.io/images/spring-logo-9146a4d3298760c2e7e49595184e1975.svg",
                    true,
                    extractTags(allTags, "Java", "Spring Boot", "后端", "教程")
            );

            createPost(
                    admin,
                    "MySQL 性能优化实践",
                    "分享一些 MySQL 数据库性能优化的经验和技巧，帮助您提高应用程序的响应速度。",
                    "<h2>MySQL 性能优化的重要性</h2><p>随着数据量的增长，数据库性能优化变得越来越重要。良好的数据库性能可以提高应用程序的响应速度，改善用户体验。</p>" +
                            "<h2>索引优化</h2><p>合理使用索引是提高查询性能的关键。以下是一些索引优化的建议：</p><ul><li>为常用查询条件创建索引</li><li>避免过度索引</li><li>定期分析和优化索引</li></ul>" +
                            "<h2>查询优化</h2><p>优化 SQL 查询可以显著提升性能：</p><ul><li>避免使用 SELECT *</li><li>使用 EXPLAIN 分析查询计划</li><li>优化 JOIN 操作</li></ul>" +
                            "<h3>示例查询优化</h3><pre><code class=\"language-sql\">-- 优化前\nSELECT * FROM users WHERE status = 'active';\n\n-- 优化后\nSELECT id, username, email FROM users WHERE status = 'active';</code></pre>" +
                            "<h2>服务器配置优化</h2><p>调整 MySQL 服务器参数可以根据您的硬件资源和工作负载特点提高性能。</p>",
                    "https://www.mysql.com/common/logos/logo-mysql-170x115.png",
                    true,
                    extractTags(allTags, "数据库", "后端", "教程")
            );

            // 普通用户的文章
            createPost(
                    normalUser,
                    "我的摄影之旅 - 秋日风景",
                    "分享一些秋天拍摄的风景照片，以及摄影技巧和心得。",
                    "<h2>秋天的魅力</h2><p>秋天是摄影的黄金季节，金黄的落叶、清澈的蓝天，构成了一幅幅美丽的画卷。</p>" +
                            "<h2>拍摄技巧</h2><p>以下是一些拍摄秋季风景的技巧：</p><ul><li>选择黄金时段（早晨或傍晚）拍摄</li><li>利用偏振镜增强色彩饱和度</li><li>尝试不同的构图方式</li></ul>" +
                            "<h2>我的作品</h2><p>以下是我最近拍摄的一些秋季风景照片：</p>" +
                            "<div class=\"image-gallery\"><img src=\"https://images.unsplash.com/photo-1506202687253-52e1b29d3527\" alt=\"秋季森林\" /><img src=\"https://images.unsplash.com/photo-1507783548227-544c3b8fc065\" alt=\"秋季湖泊\" /></div>" +
                            "<h2>后期处理</h2><p>适当的后期处理可以提升照片的视觉效果，但要注意保持自然，不要过度处理。</p>",
                    "https://images.unsplash.com/photo-1507783548227-544c3b8fc065?ixlib=rb-1.2.1&auto=format&fit=crop&w=1050&q=80",
                    true,
                    extractTags(allTags, "摄影", "生活")
            );

            createPost(
                    normalUser,
                    "前端开发学习笔记 - CSS Grid 布局",
                    "记录学习 CSS Grid 布局的笔记和实践经验，帮助初学者快速掌握这一强大的布局工具。",
                    "<h2>CSS Grid 布局简介</h2><p>CSS Grid 是一种二维布局系统，它可以同时处理行和列，非常适合创建复杂的网页布局。</p>" +
                            "<h2>基本概念</h2><ul><li>Grid Container（网格容器）</li><li>Grid Item（网格项）</li><li>Grid Line（网格线）</li><li>Grid Track（网格轨道）</li><li>Grid Cell（网格单元格）</li><li>Grid Area（网格区域）</li></ul>" +
                            "<h2>创建网格容器</h2><pre><code class=\"language-css\">.container {\n  display: grid;\n  grid-template-columns: repeat(3, 1fr);\n  grid-template-rows: 100px 200px;\n  gap: 10px;\n}</code></pre>" +
                            "<h2>定位网格项</h2><pre><code class=\"language-css\">.item1 {\n  grid-column: 1 / 3;\n  grid-row: 1 / 2;\n}</code></pre>" +
                            "<h2>实例展示</h2><p>下面是一个使用 CSS Grid 创建的响应式图片画廊：</p><pre><code class=\"language-css\">.gallery {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));\n  grid-gap: 15px;\n}</code></pre>" +
                            "<h2>浏览器兼容性</h2><p>现代浏览器对 CSS Grid 布局的支持已经相当好，但在使用时仍需注意一些旧版浏览器的兼容性问题。</p>",
                    null,
                    true,
                    extractTags(allTags, "前端", "教程", "笔记")
            );

            // 创建更多文章...

            System.out.println("文章初始化完成");
        }
    }

    private void createPost(User author, String title, String summary, String content, String coverImage, boolean published, Set<Tag> tags) {
        Post post = new Post();
        post.setTitle(title);
        post.setSummary(summary);
        post.setContent(content);
        post.setCoverImage(coverImage);
        post.setPublished(published);
        post.setAuthor(author);
        post.setTags(tags);

        postRepository.save(post);
    }

    private Set<Tag> extractTags(Set<Tag> allTags, String... tagNames) {
        Set<Tag> extractedTags = new HashSet<>();
        for (String tagName : tagNames) {
            allTags.stream()
                    .filter(tag -> tag.getName().equals(tagName))
                    .findFirst()
                    .ifPresent(extractedTags::add);
        }
        return extractedTags;
    }
}