package top.principlecreativity.lifestream.payload;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [已重构]
 * 用户信息摘要DTO (基础类)
 * 包含所有API响应中通用的用户字段
 */
@Data // [修复] 统一使用 @Data (包含 Getter, Setter, EqualsAndHashCode, ToString)
@NoArgsConstructor // [修复] 添加无参构造
public class UserSummary {
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String bio;

    // 构造函数 (用于替代旧的 UserDto)
    public UserSummary(Long id, String username, String email, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    // 构造函数 (包含 bio)
    public UserSummary(Long id, String username, String email, String avatarUrl, String bio) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }
}