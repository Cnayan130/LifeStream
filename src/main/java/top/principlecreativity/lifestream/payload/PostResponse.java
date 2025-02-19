package top.principlecreativity.lifestream.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String coverImage;
    private boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserProfile author;
    private Set<String> tags;
}