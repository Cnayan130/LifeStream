package top.principlecreativity.lifestream.payload;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long userId;
    private String username;
    private String userAvatar;

    private Long postId;
    private String postTitle;

    private Long parentId;
}