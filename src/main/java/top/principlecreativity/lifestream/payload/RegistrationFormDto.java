package top.principlecreativity.lifestream.payload;

// RegistrationFormDto.java
// 这个 DTO 专门用于接收 register.html 提交的表单数据

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

public class RegistrationFormDto {

    @NotEmpty(message = "用户名不能为空")
    @Size(min = 3, max = 40, message = "用户名长度必须在3到40个字符之间")
    private String username;

    @NotEmpty(message = "电子邮箱不能为空")
    @Email(message = "请输入有效的电子邮箱地址")
    private String email;

    @NotEmpty(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6到20个字符之间")
    private String password;

    @NotEmpty(message = "请确认密码")
    private String confirmPassword;

    @AssertTrue(message = "您必须同意服务条款和隐私政策")
    private boolean agreeTerms;

    // --- 在此添加所有字段的 Getters 和 Setters ---
    // (getter 和 setter 是 Spring Boot 绑定数据所必需的)
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public boolean isAgreeTerms() { return agreeTerms; }
    public void setAgreeTerms(boolean agreeTerms) { this.agreeTerms = agreeTerms; }
}