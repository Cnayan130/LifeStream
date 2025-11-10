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
    private UserSummary userData;

    public JwtAuthenticationResponse(String accessToken, UserSummary userData) {
        this.accessToken = accessToken;
        this.userData = userData;
    }

}