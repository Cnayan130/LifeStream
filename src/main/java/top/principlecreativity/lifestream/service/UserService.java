package top.principlecreativity.lifestream.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.principlecreativity.lifestream.entity.Role;
import top.principlecreativity.lifestream.entity.User;
import top.principlecreativity.lifestream.exception.ResourceNotFoundException;
import top.principlecreativity.lifestream.payload.UserSummary;
import top.principlecreativity.lifestream.repository.RoleRepository;
import top.principlecreativity.lifestream.repository.UserRepository;
import top.principlecreativity.lifestream.security.UserPrincipal;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(User user) {
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 设置默认角色
        Role userRole;
        Optional<Role> existingRole = roleRepository.findByName(Role.ERole.ROLE_USER);

        if (existingRole.isPresent()) {
            userRole = existingRole.get();
            logger.info("找到已存在的ROLE_USER角色");
        } else {
            // 如果找不到角色就创建一个
            logger.warn("未找到ROLE_USER角色，正在创建新角色");
            userRole = new Role();
            userRole.setName(Role.ERole.ROLE_USER);
            userRole = roleRepository.save(userRole);
        }

        user.setRoles(Collections.singleton(userRole));
        logger.info("为用户 {} 分配ROLE_USER角色", user.getUsername());

        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);

        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setBio(userDetails.getBio());
        user.setAvatarUrl(userDetails.getAvatarUrl());

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = getUserById(id);

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public UserSummary convertToUserSummary(UserPrincipal userPrincipal) {
        return new UserSummary(
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                userPrincipal.getAvatarUrl()
        );
    }
}