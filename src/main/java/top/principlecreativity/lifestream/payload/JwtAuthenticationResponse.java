package top.principlecreativity.lifestream.payload;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class JwtAuthenticationResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private UserDTO userData;

    public JwtAuthenticationResponse(String accessToken, UserDTO userData) {
        this.accessToken = accessToken;
        this.userData = userData;
    }

}