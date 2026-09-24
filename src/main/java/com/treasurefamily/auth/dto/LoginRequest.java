package com.treasurefamily.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "不能为空")
        @Size(max = 32)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "格式不正确")
        String username,
        @NotBlank(message = "不能为空") @Size(min = 8, max = 72) String password) {
}
