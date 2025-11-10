package top.principlecreativity.lifestream.payload;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ImageUpdateRequest {
    // Getters and Setters
    private String description;
    private Long albumId;

}