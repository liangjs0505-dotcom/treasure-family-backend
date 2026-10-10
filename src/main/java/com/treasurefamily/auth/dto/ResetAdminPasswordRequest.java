package com.treasurefamily.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetAdminPasswordRequest(
        @NotBlank(message = "不能为空") @Size(max = 72) String currentPassword,
        @NotBlank(message = "不能为空") @Size(min = 5, max = 8, message = "须为 5 到 8 位") String newPassword) {
}
