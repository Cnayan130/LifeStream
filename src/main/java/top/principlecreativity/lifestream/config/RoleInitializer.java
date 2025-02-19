package top.principlecreativity.lifestream.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import top.principlecreativity.lifestream.entity.Role;
import top.principlecreativity.lifestream.repository.RoleRepository;

@Configuration
public class RoleInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RoleInitializer.class);

    @Autowired
    private RoleRepository roleRepository;

    @PostConstruct
    @Transactional
    public void initRoles() {
        try {
            // 检查角色是否已存在
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
}