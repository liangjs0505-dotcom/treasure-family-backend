package com.treasurefamily.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordRequest(
        @NotBlank(message = "不能为空") @Size(max = 72) String password) {
}
