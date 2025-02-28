package top.principlecreativity.lifestream.payload;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * 用户信息摘要DTO，用于API响应中返回用户基本信息
 * 不包含敏感字段如密码等
 */

@Getter
@Setter
public class UserSummary {
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String bio;

    // 默认构造函数
    public UserSummary() {
    }

    // 带参数的构造函数
    public UserSummary(Long id, String username, String email, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    // 包含bio的构造函数
    public UserSummary(Long id, String username, String email, String avatarUrl, String bio) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSummary that = (UserSummary) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(username, that.username) &&
                Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email);
    }

    @Override
    public String toString() {
        return "UserSummary{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                '}';
    }
}