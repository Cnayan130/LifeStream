package top.principlecreativity.lifestream.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import top.principlecreativity.lifestream.entity.Post;
import top.principlecreativity.lifestream.entity.Role;
import top.principlecreativity.lifestream.entity.Tag;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.repository.PostRepository;
import top.principlecreativity.lifestream.repository.RoleRepository;
import top.principlecreativity.lifestream.repository.TagRepository;
import top.principlecreativity.lifestream.repository.UserRepository;

import jakarta.annotation.PostConstruct; // [新增] 导入 PostConstruct
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * [已合并]
 * 负责在应用程序启动时初始化数据。
 * 1. initRoles() 会在所有环境中运行，确保角色始终存在 (来自 RoleInitializer)。
 * 2. initDevData() 仅在 'dev' 配置文件中运行，用于填充测试数据。
 */
@Configuration
// [已修复] 移除了类级别的 @Profile("dev")，以便 initRoles() 可以全局运行
public class DataInitializer {

    // --- 角色初始化 (来自 RoleInitializer) ---
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private RoleRepository roleRepository;

    /**
     * [来自 RoleInitializer]
     * 在所有环境中都会运行，确保基础角色存在。
     * &#064;PostConstruct  确保它在 CommandLineRunner 之前运行。
     */
    @PostConstruct
    @Transactional
    public void initRoles() {
        try {
            if (roleRepository.count() == 0) {
                logger.info("初始化系统角色...");

                Role adminRole = new Role();
                adminRole.setName(Role.ERole.ROLE_ADMIN);
                roleRepository.save(adminRole);
                logger.info("创建角色: ROLE_ADMIN");

                Role moderatorRole = new Role();
                moderatorRole.setName(Role.ERole.ROLE_MODERATOR);
                roleRepository.save(moderatorRole);
                logger.info("创建角色: ROLE_MODERATOR");

                Role userRole = new Role();
                userRole.setName(Role.ERole.ROLE_USER);
                roleRepository.save(userRole);
                logger.info("创建角色: ROLE_USER");

                logger.info("系统角色初始化完成");
            } else {
                logger.info("系统角色已存在，跳过初始化");
            }
        } catch (Exception e) {
            logger.error("初始化系统角色失败", e);
            throw new RuntimeException("初始化系统角色失败: " + e.getMessage());
        }
    }


    // --- 开发环境测试数据 (来自旧的 DataInitializer) ---
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * [来自 DataInitializer]
     * 仅在 'dev' 配置文件中运行，用于填充测试数据。
     */
    @Transactional
    @Bean
    @Profile("dev") // [已修复] @Profile 应该放在这里，而不是整个类上
    public CommandLineRunner initDevData() {
        // [已修复] 使用 @Transactional 确保数据一致性
        return args -> {

            // [已修复] 不再调用 initRoles()，因为它已经在 @PostConstruct 中运行过了

            logger.info("开始初始化开发环境测试数据...");

            // 初始化用户
            User admin = initAdmin();
            User normalUser = initNormalUser();

            // 初始化标签
            Set<Tag> tags = initTags();

            // 初始化文章
            initPosts(admin, normalUser, tags);

            logger.info("开发环境测试数据初始化完成");
        };
    }

    // [已修复] 删除了多余的 private void initRoles() 方法

    private User initAdmin() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setBio("系统管理员，负责博客管理和内容审核。");
            admin.setAvatarUrl("https://secure.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y");

            Role adminRole = roleRepository.findByName(Role.ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("角色未找到 (initAdmin)"));
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
                    .orElseThrow(() -> new RuntimeException("角色未找到 (initNormalUser)"));
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
            logger.info("标签初始化完成");
        } else {
            tags.addAll(tagRepository.findAll());
        }
        return tags;
    }

    private void initPosts(User admin, User normalUser, Set<Tag> allTags) {
        if (postRepository.count() == 0) {
            // ... (你所有的 createPost(...) 调用都保留在这里)

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

            // ... (你其他的 createPost 调用) ...

            logger.info("文章初始化完成");
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