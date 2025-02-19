package top.principlecreativity.lifestream.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class PostRequest {
    @NotBlank
    @Size(min = 3, max = 200)
    private String title;

    @NotBlank
    private String content;

    @Size(max = 500)
    private String summary;

    private String coverImage;

    private boolean published;

    private Set<String> tags;
}