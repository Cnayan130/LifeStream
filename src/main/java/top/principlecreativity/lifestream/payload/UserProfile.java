package top.principlecreativity.lifestream.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime joinedAt;
    private String bio;
    private String avatarUrl;
}