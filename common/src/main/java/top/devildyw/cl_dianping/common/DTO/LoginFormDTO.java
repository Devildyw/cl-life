package top.devildyw.cl_dianping.common.DTO;

import lombok.Data;
import top.devildyw.cl_dianping.common.constants.RegexPatterns;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * @author Devil
 * @since 2023-01-20-15:27
 */
@Data
public class LoginFormDTO {


    //todo:修改正则
    @Pattern(regexp = RegexPatterns.PHONE_REGEX, message = "手机号格式错误！")
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @Pattern(regexp = RegexPatterns.PASSWORD_REGEX, message = "验证码格式错误！")
    @NotBlank(message = "验证码不能为空！")
    private String code;
    private String password;
}
