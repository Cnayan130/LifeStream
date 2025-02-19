package top.principlecreativity.lifestream.security;


import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * 用于注入当前认证用户的自定义注解
 * 这是对Spring Security的@AuthenticationPrincipal的封装
 */
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
