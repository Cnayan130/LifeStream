package top.principlecreativity.lifestream.payload;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImageResponse {
    private Long id;
    private String filename;
    private String contentType;
    private Long size;
    private String description;
    private LocalDateTime uploadedAt;

    private Long uploaderId;
    private String uploaderName;

    private Long albumId;
    private String albumName;

    private String downloadUrl;
}