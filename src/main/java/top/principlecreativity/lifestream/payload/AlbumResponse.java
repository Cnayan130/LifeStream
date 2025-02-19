package top.principlecreativity.lifestream.payload;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlbumResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long creatorId;
    private String creatorName;

    private int imageCount;
}