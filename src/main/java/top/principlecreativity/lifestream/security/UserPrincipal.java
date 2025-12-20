package top.principlecreativity.lifestream.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import top.principlecreativity.lifestream.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// 建议保留 OAuth2User 接口以备将来支持社交登录
public class UserPrincipal implements UserDetails {

    @Getter
    private final Long id;
    private final String username;

    @JsonIgnore
    private final String password;

    @Getter
    private final String email;
    @Getter
    private final String avatarUrl;

    private final Collection<? extends GrantedAuthority> authorities;

    // 构造函数
    public UserPrincipal(Long id, String username, String email, String password, String avatarUrl, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.avatarUrl = avatarUrl;
        this.authorities = authorities;
    }

    // 静态工厂方法：从 User 实体创建 UserPrincipal
    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getAvatarUrl(), // [关键] 确保 User.java 里有 getAvatarUrl()
                authorities
        );
    }

    @Override
    @NullMarked
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // --- Equals & HashCode ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}