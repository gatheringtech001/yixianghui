package com.ruoyi.system.domain.vo;

import lombok.Data;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** 自主加入仅接受联系人与条款确认，不接受审核状态、身份绑定或管理权限。 */
@Data
public class ConsultantApplicationRequest {
    public static final String TERMS_VERSION = "2026-09-v1";

    @NotBlank(message = "请填写姓名")
    @Size(max = 50, message = "姓名不能超过50个字")
    private String consultantName;

    @NotBlank(message = "请填写手机号")
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "请输入正确的11位手机号")
    private String mobile;

    @NotNull(message = "请同意达人加入条款")
    @AssertTrue(message = "请同意达人加入条款")
    private Boolean acceptedTerms;

    @NotBlank(message = "请阅读最新达人加入条款")
    @Pattern(regexp = TERMS_VERSION, message = "请阅读最新达人加入条款")
    private String termsVersion;
}
