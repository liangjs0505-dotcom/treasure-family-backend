package com.treasurefamily.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "不能为空")
        @Size(min = 3, max = 32, message = "长度为 3 到 32 位")
        @Pattern(regexp = "^[\\p{IsHan}A-Za-z0-9_]+$", message = "只能用中文、字母、数字和下划线")
        String username,
        @NotBlank(message = "不能为空")
        @Size(min = 8, max = 72, message = "密码至少 8 位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "须同时包含字母和数字")
        String password) {
}
