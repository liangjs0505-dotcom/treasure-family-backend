package com.treasurefamily.sale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutLineRequest(
        @NotBlank(message = "不能为空") String goodsId,
        @NotNull(message = "不能为空") @Min(value = 1, message = "至少 1 件") Integer qty) {
}
