package top.devildyw.hmdp.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * @author Devil
 * @since 2023-01-20-15:27
 */
@Data
public class LoginFormDTO {

    @Pattern(regexp = "top.devildyw.hmdp.RegexPatterns.PHONE_REGEX")
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;
    private String password;
}
