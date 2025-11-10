package top.principlecreativity.lifestream.payload;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [已重构]
 * 用户个人资料DTO (扩展类)
 * 包含 UserSummary 的所有字段，并添加了个人资料页特定的字段
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true) // [修复] 当继承 @Data 类时，必须加这个
public class UserProfile extends UserSummary { // [修复] 继承 UserSummary

    private LocalDateTime joinedAt;

    // 完整的构造函数
    public UserProfile(Long id, String username, String email, String avatarUrl, String bio, LocalDateTime joinedAt) {
        // [修复] 调用父类的构造函数
        super(id, username, email, avatarUrl, bio);
        this.joinedAt = joinedAt;
    }
}