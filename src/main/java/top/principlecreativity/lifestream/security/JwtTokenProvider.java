package top.principlecreativity.lifestream.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private int jwtExpirationInMs;

    /**
     * 生成签名密钥
     * 注意：对于 HS512 算法，密钥字符串的字节长度必须至少为 64 字节（512位）。
     * 如果你的 app.jwtSecret 很短，建议在 application.properties 中换成一个很长的随机字符串。
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    public String generateToken(Authentication authentication) {
        // 1. 防御性编程：检查入参
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("无法生成 Token: 未通过认证的 Authentication 对象");
        }

        // 2. 类型检查与转换 (Java 16+ 的 instanceof 模式匹配)
        // 如果 principal 是 UserPrincipal 类型，直接转为 userPrincipal 变量使用
        if (!(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new IllegalArgumentException("无法生成 Token: Principal 类型不匹配，预期 UserPrincipal");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        // 3. 正常生成逻辑
        return Jwts.builder()
                .subject(Long.toString(userPrincipal.getId()))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public Long getUserIdFromJWT(String token) {
        // JDK 21: 使用 var 简化类型声明
        var claims = Jwts.parser() // API 更新: parserBuilder -> parser
                .verifyWith(getSigningKey()) // API 更新: setSigningKey -> verifyWith
                .build()
                .parseSignedClaims(token) // API 更新: parseClaimsJws -> parseSignedClaims
                .getPayload(); // API 更新: getBody -> getPayload

        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }
}